package com.zerog.neoessentials.webdashboard.auth;

/**
 * Represents an authenticated user session
 */
public class AuthSession {
    private final String sessionId;
    private final User user;
    private final String token;
    private final long createdAt;
    private final long expiresAt;
    private final String ipAddress;
    
    public AuthSession(String sessionId, User user, String token, long expiresAt, String ipAddress) {
        this.sessionId = sessionId;
        this.user = user;
        this.token = token;
        this.createdAt = System.currentTimeMillis();
        this.expiresAt = expiresAt;
        this.ipAddress = ipAddress;
    }
    
    public boolean isExpired(long currentTime) {
        return currentTime > expiresAt;
    }
    
    public boolean hasPermission(String permission) {
        return user.hasPermission(permission);
    }
    
    // Getters
    public String getSessionId() { return sessionId; }
    public User getUser() { return user; }
    public String getToken() { return token; }
    public long getCreatedAt() { return createdAt; }
    public long getExpiresAt() { return expiresAt; }
    public String getIpAddress() { return ipAddress; }
}
