package com.zerog.neoessentials.webdashboard.security;

import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gates dashboard login-by-claimed-identity (Discord-linked or legacy Minecraft-username auth)
 * behind proof that the requester actually controls the named Minecraft account — the same
 * problem {@link MinecraftAccountLinkManager} already solves for linking, applied here to login
 * itself. Previously {@code AuthenticationHandler} minted a real session straight from an
 * unauthenticated username string (query param or request body), letting anyone impersonate any
 * staff member by name alone. Flow: the browser calls {@code POST /api/auth/login-request} with
 * the username it's claiming to be, gets back a short code, the actual player redeems it
 * in-game via {@code /dashboardlogin <code>} (proving live possession of that account), and only
 * then does {@code GET /api/auth/login-request/status} report success — at which point
 * {@code AuthenticationHandler} mints the session using the in-game-VERIFIED username, never the
 * originally-claimed one.
 */
public class DashboardLoginRequestManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(DashboardLoginRequestManager.class);
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no O/0/I/1 — easy to misread
    private static final int CODE_LENGTH = 8;
    private static final long CODE_TTL_MS = 5 * 60 * 1000;

    private static DashboardLoginRequestManager INSTANCE;

    private final Map<String, PendingRequest> pendingByCode = new ConcurrentHashMap<>();
    private final Map<String, VerifiedResult> resultsByRequestId = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    private DashboardLoginRequestManager() {}

    public static synchronized DashboardLoginRequestManager getInstance() {
        if (INSTANCE == null) INSTANCE = new DashboardLoginRequestManager();
        return INSTANCE;
    }

    /** @return the generated code + requestId the browser should poll with. */
    public StartedRequest startLoginRequest(String claimedUsername, String method) {
        String code = generateCode();
        String requestId = UUID.randomUUID().toString();
        long expiresAt = System.currentTimeMillis() + CODE_TTL_MS;
        pendingByCode.put(code, new PendingRequest(claimedUsername, method, requestId, expiresAt));
        NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Started dashboard login request for claimed username '{}' (method={})", claimedUsername, method);
        return new StartedRequest(code, requestId, expiresAt);
    }

    /**
     * Called from {@code /dashboardlogin <code>} in-game. Only marks the request verified if the
     * redeeming player's real username matches what the browser originally claimed — this is
     * the actual security boundary; everything else is just plumbing.
     */
    public LinkResultLike completeLoginRequest(String code, UUID minecraftUuid, String minecraftUsername) {
        PendingRequest pending = pendingByCode.get(code);
        if (pending == null) {
            NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Dashboard login request failed for {}: unknown or already-used code", minecraftUsername);
            return LinkResultLike.failure("That code isn't valid. Start a new login attempt from the dashboard.");
        }
        if (System.currentTimeMillis() > pending.expiresAt) {
            pendingByCode.remove(code);
            NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Dashboard login request failed for {}: code expired", minecraftUsername);
            return LinkResultLike.failure("That code has expired. Start a new login attempt from the dashboard.");
        }
        if (!minecraftUsername.equalsIgnoreCase(pending.claimedUsername)) {
            NeoLog.warn(LOGGER, LogCategory.WEB_DASHBOARD, "Dashboard login request mismatch: code was issued for '{}' but redeemed by '{}'", pending.claimedUsername, minecraftUsername);
            return LinkResultLike.failure("This code was requested for a different account.");
        }

        resultsByRequestId.put(pending.requestId, new VerifiedResult(minecraftUuid, minecraftUsername, pending.method, System.currentTimeMillis() + CODE_TTL_MS));
        pendingByCode.remove(code);
        NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "Dashboard login verified in-game for '{}'", minecraftUsername);
        return LinkResultLike.success();
    }

    /**
     * Polled by the browser. Consumes the result on first successful read (single-use, like the
     * code itself) so a stolen/replayed requestId can't re-trigger a session mint.
     */
    public PollResult pollLoginRequest(String requestId) {
        VerifiedResult result = resultsByRequestId.remove(requestId);
        if (result != null) {
            if (System.currentTimeMillis() > result.expiresAt) {
                return PollResult.expired();
            }
            return PollResult.completed(result.minecraftUuid, result.minecraftUsername, result.method);
        }
        boolean stillPending = pendingByCode.values().stream().anyMatch(p -> p.requestId.equals(requestId));
        return stillPending ? PollResult.pending() : PollResult.expired();
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
        }
        return sb.toString();
    }

    private static class PendingRequest {
        final String claimedUsername;
        final String method;
        final String requestId;
        final long expiresAt;

        PendingRequest(String claimedUsername, String method, String requestId, long expiresAt) {
            this.claimedUsername = claimedUsername;
            this.method = method;
            this.requestId = requestId;
            this.expiresAt = expiresAt;
        }
    }

    private static class VerifiedResult {
        final UUID minecraftUuid;
        final String minecraftUsername;
        final String method;
        final long expiresAt;

        VerifiedResult(UUID minecraftUuid, String minecraftUsername, String method, long expiresAt) {
            this.minecraftUuid = minecraftUuid;
            this.minecraftUsername = minecraftUsername;
            this.method = method;
            this.expiresAt = expiresAt;
        }
    }

    public static class StartedRequest {
        public final String code;
        public final String requestId;
        public final long expiresAt;

        StartedRequest(String code, String requestId, long expiresAt) {
            this.code = code;
            this.requestId = requestId;
            this.expiresAt = expiresAt;
        }
    }

    public static class PollResult {
        public enum Status { PENDING, COMPLETED, EXPIRED }
        public final Status status;
        public final UUID minecraftUuid;
        public final String minecraftUsername;
        public final String method;

        private PollResult(Status status, UUID minecraftUuid, String minecraftUsername, String method) {
            this.status = status;
            this.minecraftUuid = minecraftUuid;
            this.minecraftUsername = minecraftUsername;
            this.method = method;
        }

        static PollResult pending() { return new PollResult(Status.PENDING, null, null, null); }
        static PollResult expired() { return new PollResult(Status.EXPIRED, null, null, null); }
        static PollResult completed(UUID uuid, String username, String method) { return new PollResult(Status.COMPLETED, uuid, username, method); }
    }

    public static class LinkResultLike {
        public final boolean success;
        public final String message;

        private LinkResultLike(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        static LinkResultLike success() { return new LinkResultLike(true, null); }
        static LinkResultLike failure(String message) { return new LinkResultLike(false, message); }
    }
}
