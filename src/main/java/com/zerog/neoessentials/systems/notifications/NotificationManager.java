package com.zerog.neoessentials.systems.notifications;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.Map;

/**
 * Enterprise Notification Manager for NeoEssentials
 * Provides comprehensive notification system for server administrators
 * 
 * Features:
 * - Real-time system notifications
 * - Priority-based message routing
 * - Multiple notification channels (console, file, discord webhook)
 * - Notification templates and automation
 * - Performance metrics and alerts
 * - Audit trail for all notifications
 * 
 * @author ZeroG
 * @since 2.1.0
 */
public class NotificationManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationManager.class);
    
    // Singleton instance
    private static NotificationManager instance;
    
    // Configuration
    private final Path notificationDir;
    private final Path configFile;
    private final Path logFile;
    private NotificationConfig config;
    
    // Runtime data
    private final Queue<Notification> notificationQueue = new ConcurrentLinkedQueue<>();
    private final List<Notification> notificationHistory = new CopyOnWriteArrayList<>();
    private final Map<NotificationType, Integer> notificationCounts = new ConcurrentHashMap<>();
    private final Set<NotificationChannel> enabledChannels = ConcurrentHashMap.newKeySet();
    
    // Background services
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private boolean isRunning = false;
    private long startTime;
    
    // JSON serialization
    private final Gson gson = new GsonBuilder()
        .setPrettyPrinting()
        .setDateFormat("yyyy-MM-dd HH:mm:ss")
        .create();
    
    public NotificationManager() {
        this.notificationDir = Paths.get("config", "neoessentials", "notifications");
        this.configFile = notificationDir.resolve("notification-config.json");
        this.logFile = notificationDir.resolve("notifications.log");
        
        // Create directories
        try {
            Files.createDirectories(notificationDir);
        } catch (IOException e) {
            LOGGER.error("Failed to create notification directory", e);
        }
        
        loadConfiguration();
    }
    
    public static NotificationManager getInstance() {
        if (instance == null) {
            instance = new NotificationManager();
        }
        return instance;
    }
    
    /**
     * Initialize the notification manager
     */
    public void initialize() {
        if (isRunning) {
            LOGGER.warn("Notification Manager is already running");
            return;
        }
        
        LOGGER.info("Initializing Enterprise Notification Manager...");
        
        startTime = System.currentTimeMillis();
        
        // Initialize enabled channels
        initializeChannels();
        
        // Start background services
        startBackgroundServices();
        
        isRunning = true;
        
        // Send initialization notification
        sendNotification(new Notification(
            NotificationType.SYSTEM_INFO,
            NotificationPriority.HIGH,
            "Enterprise Notification Manager",
            "Notification system initialized successfully",
            "SYSTEM",
            Map.of(
                "start_time", LocalDateTime.now().toString(),
                "enabled_channels", enabledChannels.toString(),
                "version", "2.1.0"
            )
        ));
        
        LOGGER.info("Enterprise Notification Manager initialized successfully");
    }
    
    /**
     * Shutdown the notification manager
     */
    public void shutdown() {
        if (!isRunning) {
            return;
        }
        
        LOGGER.info("Shutting down Notification Manager...");
        
        // Send shutdown notification
        sendNotification(new Notification(
            NotificationType.SYSTEM_INFO,
            NotificationPriority.HIGH,
            "Enterprise Notification Manager",
            "Notification system shutting down",
            "SYSTEM",
            Map.of("shutdown_time", LocalDateTime.now().toString())
        ));
        
        // Process remaining notifications
        processRemainingNotifications();
        
        // Shutdown background services
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // Save final state
        saveNotificationHistory();
        
        isRunning = false;
        LOGGER.info("Notification Manager shutdown complete");
    }
    
    /**
     * Send a notification through all enabled channels
     */
    public void sendNotification(Notification notification) {
        if (!isRunning) {
            LOGGER.debug("Notification Manager not running, queuing notification");
            notificationQueue.offer(notification);
            return;
        }
        
        try {
            // Add to history
            notificationHistory.add(notification);
            
            // Update counts
            notificationCounts.merge(notification.getType(), 1, Integer::sum);
            
            // Process through enabled channels
            for (NotificationChannel channel : enabledChannels) {
                processNotificationChannel(notification, channel);
            }
            
            // Log to file
            logNotificationToFile(notification);
            
            // Keep history size manageable
            if (notificationHistory.size() > 1000) {
                notificationHistory.subList(0, notificationHistory.size() - 1000).clear();
            }
            
        } catch (Exception e) {
            LOGGER.error("Error sending notification", e);
        }
    }
    
    /**
     * Send a simple info notification
     */
    public void sendInfo(String title, String message) {
        sendNotification(new Notification(
            NotificationType.SYSTEM_INFO,
            NotificationPriority.NORMAL,
            title,
            message,
            "SYSTEM",
            Map.of()
        ));
    }
    
    /**
     * Send a warning notification
     */
    public void sendWarning(String title, String message) {
        sendNotification(new Notification(
            NotificationType.SYSTEM_WARNING,
            NotificationPriority.HIGH,
            title,
            message,
            "SYSTEM",
            Map.of()
        ));
    }
    
    /**
     * Send a critical alert notification
     */
    public void sendCritical(String title, String message) {
        sendNotification(new Notification(
            NotificationType.SYSTEM_ALERT,
            NotificationPriority.CRITICAL,
            title,
            message,
            "SYSTEM",
            Map.of()
        ));
    }
    
    /**
     * Send performance alert
     */
    public void sendPerformanceAlert(String metric, double value, double threshold) {
        sendNotification(new Notification(
            NotificationType.PERFORMANCE_ALERT,
            NotificationPriority.HIGH,
            "Performance Alert",
            String.format("Metric '%s' exceeded threshold: %.2f > %.2f", metric, value, threshold),
            "SYSTEM",
            Map.of(
                "metric", metric,
                "value", value,
                "threshold", threshold,
                "severity", value > threshold * 1.5 ? "CRITICAL" : "WARNING"
            )
        ));
    }
    
    /**
     * Get notification statistics
     */
    public NotificationStats getNotificationStats() {
        return new NotificationStats(
            isRunning,
            notificationQueue.size(),
            notificationHistory.size(),
            new HashMap<>(notificationCounts),
            enabledChannels.size(),
            getUptimeMillis(),
            LocalDateTime.now()
        );
    }
    
    /**
     * Get recent notifications
     */
    public List<Notification> getRecentNotifications(int limit) {
        return notificationHistory.stream()
            .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
            .limit(limit)
            .toList();
    }
    
    /**
     * Get notifications by type
     */
    public List<Notification> getNotificationsByType(NotificationType type, int limit) {
        return notificationHistory.stream()
            .filter(n -> n.getType() == type)
            .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
            .limit(limit)
            .toList();
    }
    
    /**
     * Get uptime in milliseconds
     */
    public long getUptimeMillis() {
        return isRunning ? System.currentTimeMillis() - startTime : 0;
    }
    
    /**
     * Initialize notification channels
     */
    private void initializeChannels() {
        // Always enable console logging
        enabledChannels.add(NotificationChannel.CONSOLE);
        
        // Enable file logging if configured
        if (config.isFileLoggingEnabled()) {
            enabledChannels.add(NotificationChannel.FILE);
        }
        
        // Enable Discord webhook if configured
        if (config.isDiscordWebhookEnabled() && config.getDiscordWebhookUrl() != null) {
            enabledChannels.add(NotificationChannel.DISCORD);
        }
        
        LOGGER.info("Initialized notification channels: {}", enabledChannels);
    }
    
    /**
     * Start background notification services
     */
    private void startBackgroundServices() {
        // Notification processing task (every 5 seconds)
        scheduler.scheduleAtFixedRate(this::processNotificationQueue, 0, 5, TimeUnit.SECONDS);
        
        // Statistics logging task (every minute)
        scheduler.scheduleAtFixedRate(this::logNotificationStats, 1, 1, TimeUnit.MINUTES);
        
        // History cleanup task (every hour)
        scheduler.scheduleAtFixedRate(this::cleanupNotificationHistory, 1, 1, TimeUnit.HOURS);
    }
    
    /**
     * Process queued notifications
     */
    private void processNotificationQueue() {
        try {
            Notification notification;
            while ((notification = notificationQueue.poll()) != null) {
                sendNotification(notification);
            }
        } catch (Exception e) {
            LOGGER.error("Error processing notification queue", e);
        }
    }
    
    /**
     * Process notification through specific channel
     */
    private void processNotificationChannel(Notification notification, NotificationChannel channel) {
        try {
            switch (channel) {
                case CONSOLE:
                    processConsoleNotification(notification);
                    break;
                case FILE:
                    // File logging is handled separately
                    break;
                case DISCORD:
                    processDiscordNotification(notification);
                    break;
                default:
                    LOGGER.warn("Unknown notification channel: {}", channel);
            }
        } catch (Exception e) {
            LOGGER.error("Error processing notification through channel {}", channel, e);
        }
    }
    
    /**
     * Process console notification
     */
    private void processConsoleNotification(Notification notification) {
        String logLevel = notification.getPriority() == NotificationPriority.CRITICAL ? "ERROR" :
                         notification.getPriority() == NotificationPriority.HIGH ? "WARN" : "INFO";
        
        String message = String.format("[%s] [%s] %s: %s", 
            notification.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
            notification.getType(),
            notification.getTitle(),
            notification.getMessage()
        );
        
        switch (logLevel) {
            case "ERROR" -> LOGGER.error(message);
            case "WARN" -> LOGGER.warn(message);
            default -> LOGGER.info(message);
        }
    }
    
    /**
     * Process Discord webhook notification (placeholder)
     */
    private void processDiscordNotification(Notification notification) {
        // TODO: Implement Discord webhook integration
        LOGGER.debug("Discord notification: {} - {}", notification.getTitle(), notification.getMessage());
    }
    
    /**
     * Log notification to file
     */
    private void logNotificationToFile(Notification notification) {
        if (!config.isFileLoggingEnabled()) {
            return;
        }
        
        try {
            String logEntry = String.format("[%s] [%s] [%s] %s: %s%n",
                notification.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                notification.getType(),
                notification.getPriority(),
                notification.getTitle(),
                notification.getMessage()
            );
            
            Files.writeString(logFile, logEntry, StandardCharsets.UTF_8, 
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                
        } catch (IOException e) {
            LOGGER.error("Failed to log notification to file", e);
        }
    }
    
    /**
     * Log notification statistics
     */
    private void logNotificationStats() {
        try {
            NotificationStats stats = getNotificationStats();
            LOGGER.debug("Notification Stats - Queue: {}, History: {}, Uptime: {}ms", 
                stats.getQueueSize(), stats.getHistorySize(), stats.getUptime());
        } catch (Exception e) {
            LOGGER.error("Error logging notification stats", e);
        }
    }
    
    /**
     * Cleanup old notification history
     */
    private void cleanupNotificationHistory() {
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusHours(config.getHistoryRetentionHours());
            int sizeBefore = notificationHistory.size();
            
            notificationHistory.removeIf(notification -> notification.getTimestamp().isBefore(cutoff));
            
            int sizeAfter = notificationHistory.size();
            if (sizeBefore != sizeAfter) {
                LOGGER.debug("Cleaned up {} old notifications", sizeBefore - sizeAfter);
            }
        } catch (Exception e) {
            LOGGER.error("Error cleaning up notification history", e);
        }
    }
    
    /**
     * Process remaining notifications on shutdown
     */
    private void processRemainingNotifications() {
        int processed = 0;
        Notification notification;
        while ((notification = notificationQueue.poll()) != null && processed < 100) {
            try {
                logNotificationToFile(notification);
                processed++;
            } catch (Exception e) {
                LOGGER.error("Error processing remaining notification", e);
            }
        }
        
        if (processed > 0) {
            LOGGER.info("Processed {} remaining notifications on shutdown", processed);
        }
    }
    
    /**
     * Save notification history to file
     */
    private void saveNotificationHistory() {
        try {
            Path historyFile = notificationDir.resolve("notification-history.json");
            Map<String, Object> data = new HashMap<>();
            data.put("timestamp", LocalDateTime.now());
            data.put("total_notifications", notificationHistory.size());
            data.put("notification_counts", notificationCounts);
            data.put("uptime_ms", getUptimeMillis());
            data.put("recent_notifications", getRecentNotifications(50));
            
            String json = gson.toJson(data);
            Files.writeString(historyFile, json, StandardCharsets.UTF_8);
            
        } catch (Exception e) {
            LOGGER.error("Error saving notification history", e);
        }
    }
    
    /**
     * Load notification configuration
     */
    private void loadConfiguration() {
        try {
            if (Files.exists(configFile)) {
                String json = Files.readString(configFile, StandardCharsets.UTF_8);
                config = gson.fromJson(json, NotificationConfig.class);
            } else {
                config = createDefaultConfiguration();
                saveConfiguration();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load notification configuration, using defaults", e);
            config = createDefaultConfiguration();
        }
    }
    
    /**
     * Save notification configuration
     */
    private void saveConfiguration() {
        try {
            String json = gson.toJson(config);
            Files.writeString(configFile, json, StandardCharsets.UTF_8, 
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception e) {
            LOGGER.error("Failed to save notification configuration", e);
        }
    }
    
    /**
     * Create default configuration
     */
    private NotificationConfig createDefaultConfiguration() {
        NotificationConfig defaultConfig = new NotificationConfig();
        defaultConfig.setEnabled(true);
        defaultConfig.setFileLoggingEnabled(true);
        defaultConfig.setDiscordWebhookEnabled(false);
        defaultConfig.setHistoryRetentionHours(24);
        defaultConfig.setMaxQueueSize(1000);
        return defaultConfig;
    }
    
    // Getters
    public boolean isRunning() { return isRunning; }
    public NotificationConfig getConfig() { return config; }
    public Set<NotificationChannel> getEnabledChannels() { return new HashSet<>(enabledChannels); }
}
