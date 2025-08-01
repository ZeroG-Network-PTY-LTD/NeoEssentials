package com.zerog.neoessentials.systems.security;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a security event in the audit trail
 */
public class SecurityEvent {
    private final String id;
    private final SecurityEventType type;
    private final String user;
    private final String action;
    private final SecurityLevel level;
    private final Map<String, Object> details;
    private final LocalDateTime timestamp;
    private final String sourceIP;
    private final String userAgent;
    
    public SecurityEvent(SecurityEventType type, String user, String action, 
                        SecurityLevel level, Map<String, Object> details) {
        this.id = UUID.randomUUID().toString();
        this.type = type;
        this.user = user;
        this.action = action;
        this.level = level;
        this.details = details;
        this.timestamp = LocalDateTime.now();
        this.sourceIP = "127.0.0.1"; // TODO: Get real IP
        this.userAgent = "NeoEssentials"; // TODO: Get real user agent
    }
    
    // Getters
    public String getId() { return id; }
    public SecurityEventType getType() { return type; }
    public String getUser() { return user; }
    public String getAction() { return action; }
    public SecurityLevel getLevel() { return level; }
    public Map<String, Object> getDetails() { return details; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getSourceIP() { return sourceIP; }
    public String getUserAgent() { return userAgent; }
}
