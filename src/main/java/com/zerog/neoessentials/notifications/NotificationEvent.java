package com.zerog.neoessentials.notifications;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a notification event that can be sent through various channels
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class NotificationEvent {
    
    public enum Type {
        PLAYER_JOIN,
        PLAYER_LEAVE,
        PLAYER_DEATH,
        SERVER_START,
        SERVER_STOP,
        SECURITY_ALERT,
        COMMAND_EXECUTION,
        PERFORMANCE_ALERT,
        ERROR_ALERT,
        CUSTOM
    }
    
    public enum Severity {
        INFO,
        WARNING,
        CRITICAL
    }
    
    private final Type type;
    private final String title;
    private final String message;
    private final String playerName;
    private final long timestamp;
    private final Severity severity;
    private final Map<String, String> metadata;
    
    private NotificationEvent(Builder builder) {
        this.type = builder.type;
        this.title = builder.title;
        this.message = builder.message;
        this.playerName = builder.playerName;
        this.timestamp = builder.timestamp;
        this.severity = builder.severity;
        this.metadata = new HashMap<>(builder.metadata);
    }
    
    public Type getType() {
        return type;
    }
    
    public String getTitle() {
        return title;
    }
    
    public String getMessage() {
        return message;
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public Severity getSeverity() {
        return severity;
    }
    
    public Map<String, String> getMetadata() {
        return new HashMap<>(metadata);
    }
    
    public String getMetadata(String key) {
        return metadata.get(key);
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private Type type;
        private String title;
        private String message;
        private String playerName;
        private long timestamp = System.currentTimeMillis();
        private Severity severity = Severity.INFO;
        private Map<String, String> metadata = new HashMap<>();
        
        public Builder type(Type type) {
            this.type = type;
            return this;
        }
        
        public Builder title(String title) {
            this.title = title;
            return this;
        }
        
        public Builder message(String message) {
            this.message = message;
            return this;
        }
        
        public Builder playerName(String playerName) {
            this.playerName = playerName;
            return this;
        }
        
        public Builder timestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public Builder severity(Severity severity) {
            this.severity = severity;
            return this;
        }
        
        public Builder metadata(String key, String value) {
            this.metadata.put(key, value);
            return this;
        }
        
        public Builder metadata(Map<String, String> metadata) {
            this.metadata.putAll(metadata);
            return this;
        }
        
        public NotificationEvent build() {
            if (type == null) {
                throw new IllegalArgumentException("Type cannot be null");
            }
            if (title == null || title.isEmpty()) {
                throw new IllegalArgumentException("Title cannot be null or empty");
            }
            if (message == null || message.isEmpty()) {
                throw new IllegalArgumentException("Message cannot be null or empty");
            }
            
            return new NotificationEvent(this);
        }
    }
    
    @Override
    public String toString() {
        return String.format("NotificationEvent{type=%s, title='%s', message='%s', playerName='%s', timestamp=%d, severity=%s}",
                type, title, message, playerName, timestamp, severity);
    }
}
