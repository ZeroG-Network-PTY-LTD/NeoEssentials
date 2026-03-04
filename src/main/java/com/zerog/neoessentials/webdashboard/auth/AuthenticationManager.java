package com.zerog.neoessentials.webdashboard.auth;

import com.zerog.neoessentials.webdashboard.security.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Authentication manager for the Dashboard API.
 * Delegates all real work to the fully-implemented
 * {@link com.zerog.neoessentials.webdashboard.security.AuthenticationManager}.
 */
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
        LOGGER.debug("Authentication attempt for user: {}", username);
        try {
            Session session = real().authenticate(username, password, "dashboard", "DashboardAPI");
            if (session == null) {
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
            return AuthResult.success(session.getSessionId(), session.getSessionId(), authSession);
        } catch (Exception e) {
            LOGGER.error("Error during authentication for {}: {}", username, e.getMessage(), e);
            return AuthResult.failure("Authentication error");
        }
    }

    /**
     * Validate an authentication token.
     */
    public AuthSession validateToken(String token) {
        try {
            Session session = real().validateSession(token);
            if (session == null) return null;
            com.zerog.neoessentials.webdashboard.auth.User legacyUser = new com.zerog.neoessentials.webdashboard.auth.User(
                java.util.UUID.randomUUID(), session.getUsername());
            legacyUser.addPermission("dashboard.*");
            return new AuthSession(
                session.getSessionId(), legacyUser, session.getSessionId(),
                System.currentTimeMillis() + 24 * 60 * 60 * 1000L, "dashboard"
            );
        } catch (Exception e) {
            LOGGER.error("Error validating token: {}", e.getMessage(), e);
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
            return session != null ? session.getSessionId() : null;
        } catch (Exception e) {
            LOGGER.error("Error refreshing token: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Logout and invalidate session.
     */
    public void logout(String token) {
        LOGGER.debug("Logout request for token");
        try {
            real().logout(token);
        } catch (Exception e) {
            LOGGER.error("Error during logout: {}", e.getMessage(), e);
        }
    }

    /**
     * Check if session has the required permission.
     */
    public boolean hasPermission(AuthSession session, String permission) {
        if (session == null) return false;
        try {
            return real().hasPermission(session.getSessionId(), permission);
        } catch (Exception e) {
            LOGGER.error("Error checking permission: {}", e.getMessage(), e);
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
