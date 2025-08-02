package com.zerog.neoessentials.systems.notifications;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a notification in the system
 */
public class Notification {
    private final String id;
    private final NotificationType type;
    private final NotificationPriority priority;
    private final String title;
    private final String message;
    private final String recipient;
    private final LocalDateTime timestamp;
    private final Map<String, Object> metadata;
    private boolean read = false;
    
    public Notification(NotificationType type, NotificationPriority priority, 
                       String title, String message, String recipient) {
        this(type, priority, title, message, recipient, null);
    }
    
    public Notification(NotificationType type, NotificationPriority priority, 
                       String title, String message, String recipient, 
                       Map<String, Object> metadata) {
        this.id = UUID.randomUUID().toString();
        this.type = type;
        this.priority = priority;
        this.title = title;
        this.message = message;
        this.recipient = recipient;
        this.timestamp = LocalDateTime.now();
        this.metadata = metadata;
    }
    
    public String getId() { return id; }
    public NotificationType getType() { return type; }
    public NotificationPriority getPriority() { return priority; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getRecipient() { return recipient; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public Map<String, Object> getMetadata() { return metadata; }
    public boolean isRead() { return read; }
    
    public void markAsRead() { this.read = true; }
    
    @Override
    public String toString() {
        return String.format("[%s] %s: %s", priority, type, title);
    }
}
