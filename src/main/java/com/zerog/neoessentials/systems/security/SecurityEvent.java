package com.zerog.neoessentials.systems.security;

/**
 * Security event data class
 */
public class SecurityEvent {
    private final String eventType;
    private final String details;
    private final SecurityLevel level;
    private final long timestamp;
    
    public SecurityEvent(String eventType, String details, SecurityLevel level, long timestamp) {
        this.eventType = eventType;
        this.details = details;
        this.level = level;
        this.timestamp = timestamp;
    }
    
    public String getEventType() {
        return eventType;
    }
    
    public String getDetails() {
        return details;
    }
    
    public SecurityLevel getLevel() {
        return level;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    @Override
    public String toString() {
        return String.format("[%s] %s: %s", level, eventType, details);
    }
}
