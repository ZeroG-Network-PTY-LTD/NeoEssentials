package com.zerog.neoessentials.webdashboard.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Represents an active user session in the dashboard
 */
public class Session {
    private final String sessionId;
    private final String userId;
    private final String username;
    private final User.Role role;
    private final String ipAddress;
    private final String userAgent;
    private final long createdAt;
    private long lastAccessTime;
    private boolean requiresPasswordChange;
    private boolean valid;

    private static final long SESSION_TIMEOUT_MS = 30 * 60 * 1000; // 30 minutes

    public Session(String userId, String username, User.Role role, String ipAddress, String userAgent) {
        this.sessionId = UUID.randomUUID().toString();
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.createdAt = System.currentTimeMillis();
        this.lastAccessTime = this.createdAt;
        this.requiresPasswordChange = false;
        this.valid = true;
    }

    public boolean isValid() {
        if (!valid) {
            return false;
        }
        // Check if session has timed out
        long now = System.currentTimeMillis();
        if (now - lastAccessTime > SESSION_TIMEOUT_MS) {
            valid = false;
            return false;
        }
        return true;
    }
    
    public boolean isActive() {
        return isValid();
    }

    public void updateAccessTime() {
        this.lastAccessTime = System.currentTimeMillis();
    }

    public void invalidate() {
        this.valid = false;
    }
    
    public com.google.gson.JsonObject toJson() {
        com.google.gson.JsonObject json = new com.google.gson.JsonObject();
        json.addProperty("sessionId", sessionId);
        json.addProperty("userId", userId);
        json.addProperty("username", username);
        json.addProperty("role", role.name());
        json.addProperty("ipAddress", ipAddress);
        json.addProperty("userAgent", userAgent);
        json.addProperty("createdAt", createdAt);
        json.addProperty("lastAccessTime", lastAccessTime);
        json.addProperty("requiresPasswordChange", requiresPasswordChange);
        json.addProperty("valid", valid);
        return json;
    }

    /**
     * A non-secret, deterministic reference derived from the real session id — safe to show
     * in an admin-facing sessions listing and to use for revoke-by-reference, unlike
     * {@link #getSessionId()} itself, which IS the live bearer credential (accepted directly
     * via the Authorization header / sessionId cookie) and must never be echoed back to
     * anyone but the session's own owner.
     */
    public String getSessionRef() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(sessionId.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash, 0, 8);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed available on every JVM; this is unreachable in practice.
            throw new IllegalStateException(e);
        }
    }

    // Getters
    public String getSessionId() { return sessionId; }
    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public User.Role getRole() { return role; }
    public String getIpAddress() { return ipAddress; }
    public String getUserAgent() { return userAgent; }
    public long getCreatedAt() { return createdAt; }
    public long getLastAccessTime() { return lastAccessTime; }
    public boolean requiresPasswordChange() { return requiresPasswordChange; }

    // Setters
    public void setRequiresPasswordChange(boolean requiresPasswordChange) {
        this.requiresPasswordChange = requiresPasswordChange;
    }
}
