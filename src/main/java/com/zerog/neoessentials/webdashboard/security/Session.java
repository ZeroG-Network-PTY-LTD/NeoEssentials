package com.zerog.neoessentials.webdashboard.security;

import com.google.gson.JsonObject;

import java.util.UUID;

/**
 * User session for dashboard authentication
 */
public class Session {
    private final String sessionId;
    private final String userId;
    private final String username;
    private final User.Role role;
    private final long createdAt;
    private long lastAccessedAt;
    private final String ipAddress;
    private final String userAgent;
    private boolean active;
    private boolean requiresPasswordChange;
    
    // Session timeout: 24 hours
    private static final long SESSION_TIMEOUT_MS = 24 * 60 * 60 * 1000;
    
    public Session(String userId, String username, User.Role role, String ipAddress, String userAgent) {
        this.sessionId = UUID.randomUUID().toString();
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.createdAt = System.currentTimeMillis();
        this.lastAccessedAt = createdAt;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.active = true;
        this.requiresPasswordChange = false;
    }
    
    // Getters
    public String getSessionId() { return sessionId; }
    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public User.Role getRole() { return role; }
    public long getCreatedAt() { return createdAt; }
    public long getLastAccessedAt() { return lastAccessedAt; }
    public String getIpAddress() { return ipAddress; }
    public String getUserAgent() { return userAgent; }
    public boolean isActive() { return active; }
    public boolean requiresPasswordChange() { return requiresPasswordChange; }
    public void setRequiresPasswordChange(boolean requiresPasswordChange) { this.requiresPasswordChange = requiresPasswordChange; }
    
    /**
     * Update last accessed time
     */
    public void updateAccessTime() {
        this.lastAccessedAt = System.currentTimeMillis();
    }
    
    /**
     * Invalidate session
     */
    public void invalidate() {
        this.active = false;
    }
    
    /**
     * Check if session is expired
     */
    public boolean isExpired() {
        return System.currentTimeMillis() - lastAccessedAt > SESSION_TIMEOUT_MS;
    }
    
    /**
     * Check if session is valid (active and not expired)
     */
    public boolean isValid() {
        return active && !isExpired();
    }
    
    /**
     * Get session age in milliseconds
     */
    public long getAgeMs() {
        return System.currentTimeMillis() - createdAt;
    }
    
    /**
     * Get time since last access in milliseconds
     */
    public long getIdleTimeMs() {
        return System.currentTimeMillis() - lastAccessedAt;
    }
    
    /**
     * Convert session to JSON
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("sessionId", sessionId);
        json.addProperty("userId", userId);
        json.addProperty("username", username);
        json.addProperty("role", role.name());
        json.addProperty("createdAt", createdAt);
        json.addProperty("lastAccessedAt", lastAccessedAt);
        json.addProperty("ipAddress", ipAddress);
        json.addProperty("userAgent", userAgent);
        json.addProperty("active", active);
        json.addProperty("expired", isExpired());
        json.addProperty("ageMs", getAgeMs());
        json.addProperty("idleTimeMs", getIdleTimeMs());
        json.addProperty("requiresPasswordChange", requiresPasswordChange);
        return json;
    }
}
