package com.zerog.neoessentials.systems.security;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;

/**
 * Session management for security system
 */
public class SessionManager {
    private final Map<String, UserSession> activeSessions = new ConcurrentHashMap<>();
    
    public String createSession(String userId) {
        String sessionId = "session-" + System.currentTimeMillis() + "-" + userId;
        UserSession session = new UserSession(sessionId, userId, LocalDateTime.now());
        activeSessions.put(sessionId, session);
        return sessionId;
    }
    
    public boolean validateSession(String sessionId) {
        UserSession session = activeSessions.get(sessionId);
        return session != null && session.isValid();
    }
    
    public void invalidateSession(String sessionId) {
        activeSessions.remove(sessionId);
    }
    
    public UserSession getSession(String sessionId) {
        return activeSessions.get(sessionId);
    }
    
    public int getActiveSessionCount() {
        return activeSessions.size();
    }
    
    /**
     * Initialize the session manager
     */
    public void initialize() {
        // Clear any existing sessions on initialization
        activeSessions.clear();
    }
    
    /**
     * Clean up expired sessions
     */
    public void cleanupExpiredSessions() {
        LocalDateTime now = LocalDateTime.now();
        activeSessions.entrySet().removeIf(entry -> {
            UserSession session = entry.getValue();
            // Consider sessions older than 1 hour expired
            return now.isAfter(session.getLastActivity().plusHours(1)) || !session.isValid();
        });
    }
    
    public static class UserSession {
        private final String sessionId;
        private final String userId;
        private final LocalDateTime createdAt;
        private LocalDateTime lastActivity;
        private boolean valid = true;
        
        public UserSession(String sessionId, String userId, LocalDateTime createdAt) {
            this.sessionId = sessionId;
            this.userId = userId;
            this.createdAt = createdAt;
            this.lastActivity = createdAt;
        }
        
        public String getSessionId() { return sessionId; }
        public String getUserId() { return userId; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getLastActivity() { return lastActivity; }
        public boolean isValid() { return valid; }
        
        public void updateActivity() {
            this.lastActivity = LocalDateTime.now();
        }
        
        public void invalidate() {
            this.valid = false;
        }
    }
}
