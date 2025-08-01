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
    private final Map<String, Object> metadata;
    private final LocalDateTime timestamp;
    
    public Notification(NotificationType type, NotificationPriority priority, 
                       String title, String message, Map<String, Object> metadata) {
        this.id = UUID.randomUUID().toString();
        this.type = type;
        this.priority = priority;
        this.title = title;
        this.message = message;
        this.metadata = metadata;
        this.timestamp = LocalDateTime.now();
    }
    
    // Getters
    public String getId() { return id; }
    public NotificationType getType() { return type; }
    public NotificationPriority getPriority() { return priority; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public Map<String, Object> getMetadata() { return metadata; }
    public LocalDateTime getTimestamp() { return timestamp; }
}

/**
 * Types of notifications
 */
public enum NotificationType {
    SYSTEM_INFO,
    SYSTEM_WARNING,
    SYSTEM_ALERT,
    PERFORMANCE_ALERT,
    SECURITY_ALERT,
    USER_ACTION,
    BACKUP_STATUS,
    UPDATE_AVAILABLE,
    MAINTENANCE_REQUIRED
}

/**
 * Notification priority levels
 */
public enum NotificationPriority {
    LOW(1),
    NORMAL(2),
    HIGH(3),
    CRITICAL(4);
    
    private final int level;
    
    NotificationPriority(int level) {
        this.level = level;
    }
    
    public int getLevel() { return level; }
    
    public boolean isHigherThan(NotificationPriority other) {
        return this.level > other.level;
    }
}

/**
 * Available notification channels
 */
public enum NotificationChannel {
    CONSOLE,
    FILE,
    DISCORD,
    EMAIL,
    WEBHOOK
}

/**
 * Notification statistics
 */
public class NotificationStats {
    private final boolean systemActive;
    private final int queueSize;
    private final int historySize;
    private final Map<NotificationType, Integer> typeCounts;
    private final int enabledChannels;
    private final long uptime;
    private final LocalDateTime lastUpdate;
    
    public NotificationStats(boolean systemActive, int queueSize, int historySize,
                           Map<NotificationType, Integer> typeCounts, int enabledChannels,
                           long uptime, LocalDateTime lastUpdate) {
        this.systemActive = systemActive;
        this.queueSize = queueSize;
        this.historySize = historySize;
        this.typeCounts = typeCounts;
        this.enabledChannels = enabledChannels;
        this.uptime = uptime;
        this.lastUpdate = lastUpdate;
    }
    
    // Getters
    public boolean isSystemActive() { return systemActive; }
    public int getQueueSize() { return queueSize; }
    public int getHistorySize() { return historySize; }
    public Map<NotificationType, Integer> getTypeCounts() { return typeCounts; }
    public int getEnabledChannels() { return enabledChannels; }
    public long getUptime() { return uptime; }
    public LocalDateTime getLastUpdate() { return lastUpdate; }
}

/**
 * Notification configuration
 */
public class NotificationConfig {
    private boolean enabled = true;
    private boolean fileLoggingEnabled = true;
    private boolean discordWebhookEnabled = false;
    private String discordWebhookUrl;
    private int historyRetentionHours = 24;
    private int maxQueueSize = 1000;
    private boolean enableRealTimeAlerts = true;
    
    // Getters and setters
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public boolean isFileLoggingEnabled() { return fileLoggingEnabled; }
    public void setFileLoggingEnabled(boolean fileLoggingEnabled) { this.fileLoggingEnabled = fileLoggingEnabled; }
    
    public boolean isDiscordWebhookEnabled() { return discordWebhookEnabled; }
    public void setDiscordWebhookEnabled(boolean discordWebhookEnabled) { this.discordWebhookEnabled = discordWebhookEnabled; }
    
    public String getDiscordWebhookUrl() { return discordWebhookUrl; }
    public void setDiscordWebhookUrl(String discordWebhookUrl) { this.discordWebhookUrl = discordWebhookUrl; }
    
    public int getHistoryRetentionHours() { return historyRetentionHours; }
    public void setHistoryRetentionHours(int historyRetentionHours) { this.historyRetentionHours = historyRetentionHours; }
    
    public int getMaxQueueSize() { return maxQueueSize; }
    public void setMaxQueueSize(int maxQueueSize) { this.maxQueueSize = maxQueueSize; }
    
    public boolean isEnableRealTimeAlerts() { return enableRealTimeAlerts; }
    public void setEnableRealTimeAlerts(boolean enableRealTimeAlerts) { this.enableRealTimeAlerts = enableRealTimeAlerts; }
}
