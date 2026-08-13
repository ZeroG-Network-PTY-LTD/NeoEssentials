package com.zerog.neoessentials.webdashboard.auth;

import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import com.zerog.neoessentials.webdashboard.security.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Authentication manager for the Dashboard API.
 * Delegates all real work to the fully-implemented
 * {@link com.zerog.neoessentials.webdashboard.security.AuthenticationManager}.
 * Kept as a bridge for any external callers that reference the old auth package.
 */
@SuppressWarnings("unused") // Bridge class — may be referenced by external callers or future code
public class AuthenticationManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationManager.class);
    private static AuthenticationManager INSTANCE;

    // Keep maps for legacy callers but drive state through the real manager
    @SuppressWarnings("unused")
    private final Map<String, AuthSession> activeSessions = new ConcurrentHashMap<>();
    @SuppressWarnings("unused")
    private final Map<String, User> users = new ConcurrentHashMap<>();

    private AuthenticationManager() {}

    public static AuthenticationManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new AuthenticationManager();
        }
        return INSTANCE;
    }

    private com.zerog.neoessentials.webdashboard.security.AuthenticationManager real() {
        return com.zerog.neoessentials.webdashboard.security.AuthenticationManager.getInstance();
    }

    /**
     * Authenticate user with username/password.
     * Delegates to security.AuthenticationManager which handles hashing,
     * lockouts, session creation, and audit logging.
     */
    public AuthResult authenticate(String username, String password) {
        NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Authentication attempt for user: {}", username);
        try {
            Session session = real().authenticate(username, password, "dashboard", "DashboardAPI");
            if (session == null) {
                NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Authentication failed for user: {} (invalid credentials or account locked)", username);
                return AuthResult.failure("Invalid credentials or account locked");
            }
            // Wrap into legacy AuthResult
            com.zerog.neoessentials.webdashboard.auth.User legacyUser = new com.zerog.neoessentials.webdashboard.auth.User(
                java.util.UUID.randomUUID(), session.getUsername());
            legacyUser.addPermission("dashboard.*");
            AuthSession authSession = new AuthSession(
                session.getSessionId(), legacyUser, session.getSessionId(),
                System.currentTimeMillis() + 24 * 60 * 60 * 1000L, "dashboard"
            );
            NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Authentication succeeded for user: {}", username);
            return AuthResult.success(session.getSessionId(), session.getSessionId(), authSession);
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "Error during authentication for " + username, e);
            return AuthResult.failure("Authentication error");
        }
    }

    /**
     * Validate an authentication token.
     */
    public AuthSession validateToken(String token) {
        try {
            Session session = real().validateSession(token);
            if (session == null) {
                NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Token validation failed: session not found or expired");
                return null;
            }
            com.zerog.neoessentials.webdashboard.auth.User legacyUser = new com.zerog.neoessentials.webdashboard.auth.User(
                java.util.UUID.randomUUID(), session.getUsername());
            legacyUser.addPermission("dashboard.*");
            NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Token validated for user: {}", session.getUsername());
            return new AuthSession(
                session.getSessionId(), legacyUser, session.getSessionId(),
                System.currentTimeMillis() + 24 * 60 * 60 * 1000L, "dashboard"
            );
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "Error validating token", e);
            return null;
        }
    }

    /**
     * Refresh an authentication token.
     * Re-validates the existing session — the real manager extends access time on every validate call.
     */
    public String refreshToken(String refreshToken) {
        try {
            Session session = real().validateSession(refreshToken);
            NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Token refresh {}", session != null ? "succeeded" : "failed");
            return session != null ? session.getSessionId() : null;
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "Error refreshing token", e);
            return null;
        }
    }

    /**
     * Logout and invalidate session.
     */
    public void logout(String token) {
        NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Logout request for token");
        try {
            real().logout(token);
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "Error during logout", e);
        }
    }

    /**
     * Check if session has the required permission.
     */
    public boolean hasPermission(AuthSession session, String permission) {
        if (session == null) return false;
        try {
            boolean allowed = real().hasPermission(session.getSessionId(), permission);
            NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Permission check '{}' for user {} -> {}", permission, session.getUser() != null ? session.getUser().getUsername() : "unknown", allowed);
            return allowed;
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "Error checking permission", e);
            return false;
        }
    }

    /**
     * Clean up expired sessions — delegated to the real manager's background task.
     */
    public void cleanupExpiredSessions() {
        // The real manager runs its own scheduled cleanup every minute.
        // Nothing additional needed here.
    }
}
