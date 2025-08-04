package com.zerog.neoessentials.security;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Security event class for tracking security-related activities
 */
public class SecurityEvent {
    private final SecurityEventType type;
    private final String message;
    private final SecurityLevel level;
    private final long timestamp;
    private final ThreatLevel threatLevel;
    private final String description;
    private final String source;

    public SecurityEvent(SecurityEventType type, String message, SecurityLevel level, long timestamp) {
        this.type = type;
        this.message = message;
        this.level = level;
        this.timestamp = timestamp;
        this.threatLevel = mapLevelToThreat(level);
        this.description = message;
        this.source = "System";
    }

    public SecurityEvent(SecurityEventType type, String message, SecurityLevel level, ThreatLevel threatLevel, String source, long timestamp) {
        this.type = type;
        this.message = message;
        this.level = level;
        this.timestamp = timestamp;
        this.threatLevel = threatLevel;
        this.description = message;
        this.source = source;
    }

    private ThreatLevel mapLevelToThreat(SecurityLevel level) {
        switch (level) {
            case INFO: return ThreatLevel.NONE;
            case LOW: return ThreatLevel.LOW;
            case MEDIUM: return ThreatLevel.MEDIUM;
            case WARNING: return ThreatLevel.HIGH;
            case HIGH: return ThreatLevel.HIGH;
            case CRITICAL: return ThreatLevel.CRITICAL;
            default: return ThreatLevel.NONE;
        }
    }

    public SecurityEventType getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public SecurityLevel getLevel() {
        return level;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public LocalDateTime getTimestampAsDateTime() {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
    }

    public ThreatLevel getThreatLevel() {
        return threatLevel;
    }

    public String getDescription() {
        return description;
    }

    public String getSource() {
        return source;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s: %s", level, type, message);
    }
}
