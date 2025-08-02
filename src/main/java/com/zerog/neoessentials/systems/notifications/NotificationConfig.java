package com.zerog.neoessentials.systems.notifications;

import java.util.Set;
import java.util.HashSet;

/**
 * Configuration for the notification system
 */
public class NotificationConfig {
    private boolean enabled = true;
    private int maxNotifications = 1000;
    private int retentionDays = 30;
    private boolean discordEnabled = false;
    private String discordWebhook = "";
    private boolean emailEnabled = false;
    private String emailHost = "";
    private int emailPort = 587;
    private String emailUsername = "";
    private String emailPassword = "";
    private Set<NotificationChannel> enabledChannels = new HashSet<>();
    private Set<NotificationType> mutedTypes = new HashSet<>();
    private boolean fileLoggingEnabled = true;
    private String discordWebhookUrl = "";
    private int historyRetentionHours = 24;
    private int maxQueueSize = 1000;
    
    public NotificationConfig() {
        // Default enabled channels
        enabledChannels.add(NotificationChannel.CONSOLE);
        enabledChannels.add(NotificationChannel.FILE_LOG);
    }
    
    // Getters and setters
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public int getMaxNotifications() { return maxNotifications; }
    public void setMaxNotifications(int maxNotifications) { this.maxNotifications = maxNotifications; }
    
    public int getRetentionDays() { return retentionDays; }
    public void setRetentionDays(int retentionDays) { this.retentionDays = retentionDays; }
    
    public boolean isDiscordEnabled() { return discordEnabled; }
    public void setDiscordEnabled(boolean discordEnabled) { this.discordEnabled = discordEnabled; }
    
    public String getDiscordWebhook() { return discordWebhook; }
    public void setDiscordWebhook(String discordWebhook) { this.discordWebhook = discordWebhook; }
    
    public boolean isEmailEnabled() { return emailEnabled; }
    public void setEmailEnabled(boolean emailEnabled) { this.emailEnabled = emailEnabled; }
    
    public String getEmailHost() { return emailHost; }
    public void setEmailHost(String emailHost) { this.emailHost = emailHost; }
    
    public int getEmailPort() { return emailPort; }
    public void setEmailPort(int emailPort) { this.emailPort = emailPort; }
    
    public String getEmailUsername() { return emailUsername; }
    public void setEmailUsername(String emailUsername) { this.emailUsername = emailUsername; }
    
    public String getEmailPassword() { return emailPassword; }
    public void setEmailPassword(String emailPassword) { this.emailPassword = emailPassword; }
    
    public Set<NotificationChannel> getEnabledChannels() { return enabledChannels; }
    public void setEnabledChannels(Set<NotificationChannel> enabledChannels) { this.enabledChannels = enabledChannels; }
    
    public Set<NotificationType> getMutedTypes() { return mutedTypes; }
    public void setMutedTypes(Set<NotificationType> mutedTypes) { this.mutedTypes = mutedTypes; }
    
    public boolean isChannelEnabled(NotificationChannel channel) {
        return enabledChannels.contains(channel);
    }
    
    public boolean isTypeMuted(NotificationType type) {
        return mutedTypes.contains(type);
    }
    
    // Additional methods for compatibility
    public boolean isFileLoggingEnabled() { return fileLoggingEnabled; }
    public void setFileLoggingEnabled(boolean fileLoggingEnabled) { this.fileLoggingEnabled = fileLoggingEnabled; }
    
    public boolean isDiscordWebhookEnabled() { return discordEnabled; }
    public void setDiscordWebhookEnabled(boolean discordWebhookEnabled) { this.discordEnabled = discordWebhookEnabled; }
    
    public String getDiscordWebhookUrl() { return discordWebhookUrl; }
    public void setDiscordWebhookUrl(String discordWebhookUrl) { this.discordWebhookUrl = discordWebhookUrl; }
    
    public int getHistoryRetentionHours() { return historyRetentionHours; }
    public void setHistoryRetentionHours(int historyRetentionHours) { this.historyRetentionHours = historyRetentionHours; }
    
    public int getMaxQueueSize() { return maxQueueSize; }
    public void setMaxQueueSize(int maxQueueSize) { this.maxQueueSize = maxQueueSize; }
}
