package com.zerog.neoessentials.notifications;

import com.zerog.neoessentials.config.MainConfig;
import com.zerog.neoessentials.notifications.channels.LogNotificationChannel;
import com.zerog.neoessentials.notifications.channels.NotificationChannel;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central notification management system for NeoEssentials
 * Supports multiple notification channels: Email, SMS, Log
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class NotificationManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationManager.class);
    private static NotificationManager instance;
    
    private final Map<String, NotificationChannel> channels = new ConcurrentHashMap<>();
    // Removed unused field config
    private final Set<NotificationEvent.Type> enabledEvents = EnumSet.allOf(NotificationEvent.Type.class);
    
    private NotificationManager(MainConfig config) {
    // ...existing code...
        initializeChannels();
        com.zerog.neoessentials.util.DebugUtil.debugLog("Notification manager initialized with " + channels.size() + " channels");
    }
    
    public static synchronized NotificationManager getInstance(MainConfig config) {
        if (instance == null) {
            instance = new NotificationManager(null);
        }
        return instance;
    }
    
    public static NotificationManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("NotificationManager not initialized");
        }
        return instance;
    }
    
    /**
     * Initialize notification channels based on configuration
     */
    private void initializeChannels() {
        // Always enable log notifications
        channels.put("log", new LogNotificationChannel());
        
        com.zerog.neoessentials.util.DebugUtil.debugLog("Notification channels initialized: " + String.join(", ", channels.keySet()));
    }
    
    /**
     * Send a notification to all enabled channels
     */
    public CompletableFuture<Void> sendNotification(NotificationEvent event) {
        if (!enabledEvents.contains(event.getType())) {
            return CompletableFuture.completedFuture(null);
        }
        
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        for (Map.Entry<String, NotificationChannel> entry : channels.entrySet()) {
            String channelName = entry.getKey();
            NotificationChannel channel = entry.getValue();
            
            if (channel.isEnabled() && channel.supportsEventType(event.getType())) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        channel.sendNotification(event);
                        LOGGER.debug("Sent notification to {} channel: {}", channelName, event.getTitle());
                    } catch (Exception e) {
                        LOGGER.error("Failed to send notification to {} channel", channelName, e);
                    }
                });
                futures.add(future);
            }
        }
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }
    
    /**
     * Send player join notification
     */
    public void notifyPlayerJoin(ServerPlayer player) {
        NotificationEvent event = NotificationEvent.builder()
            .type(NotificationEvent.Type.PLAYER_JOIN)
            .title("Player Joined")
            .message(player.getName().getString() + " joined the server")
            .playerName(player.getName().getString())
            .timestamp(System.currentTimeMillis())
            .severity(NotificationEvent.Severity.INFO)
            .build();
        
        sendNotification(event);
    }
    
    /**
     * Send player leave notification
     */
    public void notifyPlayerLeave(ServerPlayer player) {
        NotificationEvent event = NotificationEvent.builder()
            .type(NotificationEvent.Type.PLAYER_LEAVE)
            .title("Player Left")
            .message(player.getName().getString() + " left the server")
            .playerName(player.getName().getString())
            .timestamp(System.currentTimeMillis())
            .severity(NotificationEvent.Severity.INFO)
            .build();
        
        sendNotification(event);
    }
    
    /**
     * Send server start notification
     */
    public void notifyServerStart() {
        NotificationEvent event = NotificationEvent.builder()
            .type(NotificationEvent.Type.SERVER_START)
            .title("Server Started")
            .message("Minecraft server has started successfully")
            .timestamp(System.currentTimeMillis())
            .severity(NotificationEvent.Severity.INFO)
            .build();
        
        sendNotification(event);
    }
    
    /**
     * Send server stop notification
     */
    public void notifyServerStop() {
        NotificationEvent event = NotificationEvent.builder()
            .type(NotificationEvent.Type.SERVER_STOP)
            .title("Server Stopping")
            .message("Minecraft server is shutting down")
            .timestamp(System.currentTimeMillis())
            .severity(NotificationEvent.Severity.WARNING)
            .build();
        
        sendNotification(event);
    }
    
    /**
     * Send security alert notification
     */
    public void notifySecurityAlert(String alertType, String details, ServerPlayer player) {
        NotificationEvent event = NotificationEvent.builder()
            .type(NotificationEvent.Type.SECURITY_ALERT)
            .title("Security Alert: " + alertType)
            .message(details)
            .playerName(player != null ? player.getName().getString() : "Unknown")
            .timestamp(System.currentTimeMillis())
            .severity(NotificationEvent.Severity.CRITICAL)
            .build();
        
        sendNotification(event);
    }
    
    /**
     * Send command execution notification for monitoring
     */
    public void notifyCommandExecution(String command, ServerPlayer player, boolean success) {
        // Check command logging configuration in MainConfig
    // Replace with correct config access pattern
    // Example: if using a ConfigManager singleton:
    // if (!ConfigManager.getInstance().getMainConfig().notifications.logCommands) {
    //     return;
    // }
        
        NotificationEvent event = NotificationEvent.builder()
            .type(NotificationEvent.Type.COMMAND_EXECUTION)
            .title("Command Executed")
            .message(player.getName().getString() + " executed: " + command + 
                    (success ? " (Success)" : " (Failed)"))
            .playerName(player.getName().getString())
            .timestamp(System.currentTimeMillis())
            .severity(success ? NotificationEvent.Severity.INFO : NotificationEvent.Severity.WARNING)
            .metadata("command", command)
            .metadata("success", String.valueOf(success))
            .build();
        
        sendNotification(event);
    }
    
    /**
     * Send performance alert notification
     */
    public void notifyPerformanceAlert(String metric, double value, double threshold) {
        NotificationEvent event = NotificationEvent.builder()
            .type(NotificationEvent.Type.PERFORMANCE_ALERT)
            .title("Performance Alert")
            .message(String.format("%s is %.2f (threshold: %.2f)", metric, value, threshold))
            .timestamp(System.currentTimeMillis())
            .severity(NotificationEvent.Severity.WARNING)
            .metadata("metric", metric)
            .metadata("value", String.valueOf(value))
            .metadata("threshold", String.valueOf(threshold))
            .build();
        
        sendNotification(event);
    }
    
    /**
     * Enable or disable specific event types
     */
    public void setEventEnabled(NotificationEvent.Type eventType, boolean enabled) {
        if (enabled) {
            enabledEvents.add(eventType);
        } else {
            enabledEvents.remove(eventType);
        }
        LOGGER.info("Event type {} {}", eventType, enabled ? "enabled" : "disabled");
    }
    
    /**
     * Get all available notification channels
     */
    public Map<String, NotificationChannel> getChannels() {
        return Collections.unmodifiableMap(channels);
    }
    
    /**
     * Get enabled event types
     */
    public Set<NotificationEvent.Type> getEnabledEvents() {
        return Collections.unmodifiableSet(enabledEvents);
    }
    
    /**
     * Shutdown notification manager and cleanup resources
     */
    public void shutdown() {
        LOGGER.info("Shutting down notification manager...");
        
        // Send server stop notification before shutting down
        notifyServerStop();
        
        // Wait a bit for notifications to be sent
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Close all channels
        for (NotificationChannel channel : channels.values()) {
            try {
                channel.close();
            } catch (Exception e) {
                LOGGER.error("Error closing notification channel", e);
            }
        }
        
        channels.clear();
        LOGGER.info("Notification manager shutdown complete");
    }
}
