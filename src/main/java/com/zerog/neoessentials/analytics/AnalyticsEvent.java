package com.zerog.neoessentials.analytics;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Represents an analytics event in the system
 */
public class AnalyticsEvent {
    
    public enum EventType {
        COMMAND_EXECUTION,
        PLAYER_LOGIN,
        PLAYER_LOGOUT,
        FEATURE_USAGE,
        SERVER_EVENT,
        PERFORMANCE_ALERT,
        ERROR_EVENT
    }
    
    private final EventType eventType;
    private final String eventName;
    private final UUID playerUUID;
    private final Map<String, Object> metadata;
    private final LocalDateTime timestamp;
    
    public AnalyticsEvent(EventType eventType, String eventName, UUID playerUUID, Map<String, Object> metadata) {
        this.eventType = eventType;
        this.eventName = eventName;
        this.playerUUID = playerUUID;
        this.metadata = metadata;
        this.timestamp = LocalDateTime.now();
    }
    
    // Getters
    public EventType getEventType() { return eventType; }
    public String getEventName() { return eventName; }
    public UUID getPlayerUUID() { return playerUUID; }
    public Map<String, Object> getMetadata() { return metadata; }
    public LocalDateTime getTimestamp() { return timestamp; }
    
    @Override
    public String toString() {
        return String.format("AnalyticsEvent{type=%s, name='%s', player=%s, time=%s}", 
            eventType, eventName, playerUUID, timestamp);
    }
}
