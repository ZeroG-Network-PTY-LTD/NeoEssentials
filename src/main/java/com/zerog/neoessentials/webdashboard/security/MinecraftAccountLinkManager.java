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
 * Lets an EXISTING dashboard account (this mod's own, or the paired external Laravel app's)
 * prove ownership of a Minecraft account from the browser — the opposite direction of
 * {@link DashboardRegistrationManager} (which creates a brand-new MOD dashboard account
 * starting from an in-game player, not the reverse).
 *
 * <p>Deliberately keyed by a plain {@code ownerKey} string, NOT a mod {@link User} id — the
 * external dashboard has its own separate user accounts (a Laravel {@code User} model) that may
 * have no corresponding mod-side dashboard account at all, so this can't assume one exists.
 * {@code ownerKey} is always a dashboard username: for the mod's own bundled dashboard, the
 * session's real {@link User#getUsername()}; for the external app (which has no mod-side
 * session of its own), whatever username it passes explicitly (its {@code mod_username}).
 *
 * <p>Flow: the dashboard's Settings/Profile page calls {@code POST /api/auth/link-minecraft/start}
 * while logged in, gets back a short code, and the player types {@code /linkaccount <code>}
 * in-game. If {@code ownerKey} happens to match a REAL mod dashboard user (true for the mod's
 * own SPA; only sometimes true for the external app), that user's own {@code mcUuid}/
 * {@code mcUsername} are updated too, keeping both dashboards' views of the same account in
 * sync — but the completed-link result is always retrievable via {@code ownerKey} regardless,
 * so the external app can persist it onto its own {@code User} row even with no matching mod
 * account.
 */
public class MinecraftAccountLinkManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(MinecraftAccountLinkManager.class);
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no O/0/I/1 — easy to misread
    private static final int CODE_LENGTH = 8;
    private static final long CODE_TTL_MS = 5 * 60 * 1000;

    private static MinecraftAccountLinkManager INSTANCE;

    private final Map<String, PendingLink> pendingByCode = new ConcurrentHashMap<>();
    // Completed links, keyed by ownerKey (username) — deliberately never expired: an external
    // caller that's slow to poll (or misses the window) should still be able to read the
    // outcome, same "ephemeral but not eagerly discarded" reasoning as pendingRegistrations
    // elsewhere in this package. Cleared only by an explicit unlink.
    private final Map<String, CompletedLink> completedByOwnerKey = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    private MinecraftAccountLinkManager() {}

    public static synchronized MinecraftAccountLinkManager getInstance() {
        if (INSTANCE == null) INSTANCE = new MinecraftAccountLinkManager();
        return INSTANCE;
    }

    /**
     * @return the generated code, or null if this owner already has a linked Minecraft account
     *         (must unlink first — no accidental relinking mid-flow).
     */
    public String startLink(String ownerKey) {
        if (getLinkedUuid(ownerKey) != null) {
            NeoLog.warn(LOGGER, LogCategory.WEB_DASHBOARD, "'{}' already has a linked Minecraft account", ownerKey);
            return null;
        }

        // Clear any earlier pending code for this same owner so only the most recent one works.
        pendingByCode.values().removeIf(p -> p.ownerKey.equals(ownerKey));

        String code = generateCode();
        pendingByCode.put(code, new PendingLink(ownerKey, System.currentTimeMillis() + CODE_TTL_MS));
        NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Started Minecraft account link flow for owner '{}'", ownerKey);
        return code;
    }

    public long peekExpiry(String code) {
        PendingLink pending = pendingByCode.get(code);
        return pending != null ? pending.expiresAt : 0;
    }

    /** Currently-linked MC UUID for a given owner, checking both a real mod User (if any) and the completed-link cache. */
    public String getLinkedUuid(String ownerKey) {
        User modUser = AuthenticationManager.getInstance().getUserByUsername(ownerKey);
        if (modUser != null && modUser.getMcUuid() != null) return modUser.getMcUuid();

        CompletedLink completed = completedByOwnerKey.get(ownerKey);
        return completed != null ? completed.mcUuid : null;
    }

    public String getLinkedUsername(String ownerKey) {
        User modUser = AuthenticationManager.getInstance().getUserByUsername(ownerKey);
        if (modUser != null && modUser.getMcUsername() != null) return modUser.getMcUsername();

        CompletedLink completed = completedByOwnerKey.get(ownerKey);
        return completed != null ? completed.mcUsername : null;
    }

    /**
     * Called from {@code /linkaccount <code>} in-game. Returns a human-readable result rather
     * than a bare boolean so the command can give the player a specific reason on failure.
     */
    public LinkResult completeLink(String code, UUID minecraftUuid, String minecraftUsername) {
        PendingLink pending = pendingByCode.get(code);
        if (pending == null) {
            NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Minecraft account link failed for {}: unknown or already-used code", minecraftUsername);
            return LinkResult.failure("That code isn't valid. Generate a new one from the dashboard's Settings page.");
        }
        if (System.currentTimeMillis() > pending.expiresAt) {
            pendingByCode.remove(code);
            NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Minecraft account link failed for {}: code expired", minecraftUsername);
            return LinkResult.failure("That code has expired. Generate a new one from the dashboard's Settings page.");
        }

        // Reject if this MC account is already linked to a DIFFERENT real mod dashboard user —
        // no silent takeover. (Can't fully dedupe against the external app's own DB from here;
        // it enforces its own uniqueness on mc_uuid independently.)
        User existingLink = AuthenticationManager.getInstance().getUserByMcUuid(minecraftUuid.toString());
        if (existingLink != null && !existingLink.getUsername().equals(pending.ownerKey)) {
            NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Minecraft account link failed for {}: already linked to a different dashboard account", minecraftUsername);
            return LinkResult.failure("This Minecraft account is already linked to a different dashboard account.");
        }

        completedByOwnerKey.put(pending.ownerKey, new CompletedLink(minecraftUuid.toString(), minecraftUsername));
        NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "Minecraft account '{}' linked to dashboard owner '{}'", minecraftUsername, pending.ownerKey);

        // If ownerKey also happens to be a real mod dashboard user, keep it in sync too (this is
        // always true for the mod's own bundled SPA, and gets the audit log + sync webhook for
        // free via AuthenticationManager).
        User modUser = AuthenticationManager.getInstance().getUserByUsername(pending.ownerKey);
        if (modUser != null) {
            AuthenticationManager.getInstance().setMinecraftLink(modUser.getId(), minecraftUuid.toString(), minecraftUsername);
        }

        pendingByCode.remove(code);
        return LinkResult.success();
    }

    /** Self-service unlink — clears both the mod User (if any) and the completed-link cache. */
    public void unlink(String ownerKey) {
        completedByOwnerKey.remove(ownerKey);
        User modUser = AuthenticationManager.getInstance().getUserByUsername(ownerKey);
        if (modUser != null) {
            AuthenticationManager.getInstance().clearMinecraftLink(modUser.getId());
        }
        NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Minecraft account link cleared for owner '{}'", ownerKey);
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
        }
        return sb.toString();
    }

    private static class PendingLink {
        final String ownerKey;
        final long expiresAt;

        PendingLink(String ownerKey, long expiresAt) {
            this.ownerKey = ownerKey;
            this.expiresAt = expiresAt;
        }
    }

    private static class CompletedLink {
        final String mcUuid;
        final String mcUsername;

        CompletedLink(String mcUuid, String mcUsername) {
            this.mcUuid = mcUuid;
            this.mcUsername = mcUsername;
        }
    }

    public static class LinkResult {
        public final boolean success;
        public final String message;

        private LinkResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        static LinkResult success() { return new LinkResult(true, null); }
        static LinkResult failure(String message) { return new LinkResult(false, message); }
    }
}
