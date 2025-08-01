package com.zerog.neoessentials.systems.notifications;

import com.zerog.neoessentials.systems.status.SystemStatusMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enterprise Notification System for NeoEssentials
 * Provides real-time alerts and notifications based on system status
 * 
 * Features:
 * - Health-based alert generation
 * - Console notifications
 * - File logging of critical events
 * - Integration with SystemStatusMonitor
 * - Configurable alert thresholds
 * 
 * @author ZeroG
 * @since 2.1.0
 */
public class AlertNotificationSystem {
    private static final Logger LOGGER = LoggerFactory.getLogger(AlertNotificationSystem.class);
    
    // Singleton instance
    private static AlertNotificationSystem instance;
    
    // Notification system components
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final ConcurrentLinkedQueue<StatusAlert> alertQueue = new ConcurrentLinkedQueue<>();
    private final Map<String, StatusAlert> activeAlerts = new ConcurrentHashMap<>();
    private final List<NotificationChannel> channels = new ArrayList<>();
    
    // System integration
    private final SystemStatusMonitor statusMonitor = SystemStatusMonitor.getInstance();
    
    // Configuration
    private boolean isRunning = false;
    private double healthThreshold = 70.0; // Alert when health drops below 70%
    private double criticalThreshold = 50.0; // Critical alert when health drops below 50%
    private long monitoringInterval = 30000; // 30 seconds
    
    private AlertNotificationSystem() {
        initializeNotificationChannels();
    }
    
    public static AlertNotificationSystem getInstance() {
        if (instance == null) {
            synchronized (AlertNotificationSystem.class) {
                if (instance == null) {
                    instance = new AlertNotificationSystem();
                }
            }
        }
        return instance;
    }
    
    /**
     * Initialize the notification system
     */
    public void initialize() {
        if (isRunning) {
            LOGGER.warn("AlertNotificationSystem is already running");
            return;
        }
        
        try {
            // Start background monitoring
            startBackgroundMonitoring();
            
            // Start notification processor
            startNotificationProcessor();
            
            isRunning = true;
            LOGGER.info("AlertNotificationSystem initialized successfully");
            
            // Send initialization notification
            sendAlert(createSystemAlert(AlertLevel.INFO, "Notification System", 
                "Alert notification system has been initialized and is monitoring system health"));
            
        } catch (Exception e) {
            LOGGER.error("Failed to initialize AlertNotificationSystem", e);
        }
    }
    
    /**
     * Shutdown the notification system
     */
    public void shutdown() {
        if (!isRunning) {
            return;
        }
        
        try {
            isRunning = false;
            scheduler.shutdown();
            
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            
            LOGGER.info("AlertNotificationSystem shutdown completed");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            scheduler.shutdownNow();
            LOGGER.error("AlertNotificationSystem shutdown interrupted", e);
        }
    }
    
    /**
     * Start background system monitoring
     */
    private void startBackgroundMonitoring() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                checkSystemHealth();
                checkResourceLimits();
                checkComponentStatus();
            } catch (Exception e) {
                LOGGER.error("Error in background monitoring", e);
            }
        }, 10, monitoringInterval / 1000, TimeUnit.SECONDS);
        
        LOGGER.info("Background system monitoring started (interval: {}ms)", monitoringInterval);
    }
    
    /**
     * Start notification processor
     */
    private void startNotificationProcessor() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                processNotificationQueue();
            } catch (Exception e) {
                LOGGER.error("Error processing notifications", e);
            }
        }, 5, 5, TimeUnit.SECONDS);
        
        LOGGER.info("Notification processor started");
    }
    
    /**
     * Check overall system health and generate alerts
     */
    private void checkSystemHealth() {
        try {
            var systemStatus = statusMonitor.getSystemStatus();
            double currentHealth = systemStatus.getHealthScore();
            
            String alertKey = "system_health";
            
            if (currentHealth < criticalThreshold) {
                // Critical health alert
                if (!activeAlerts.containsKey(alertKey + "_critical")) {
                    StatusAlert alert = createSystemAlert(AlertLevel.CRITICAL, "System Health Critical",
                        String.format("System health has dropped to %.1f%% (Critical threshold: %.1f%%)", 
                        currentHealth, criticalThreshold));
                    sendAlert(alert);
                    activeAlerts.put(alertKey + "_critical", alert);
                }
            } else if (currentHealth < healthThreshold) {
                // Warning health alert
                if (!activeAlerts.containsKey(alertKey + "_warning")) {
                    StatusAlert alert = createSystemAlert(AlertLevel.WARNING, "System Health Warning",
                        String.format("System health has dropped to %.1f%% (Warning threshold: %.1f%%)", 
                        currentHealth, healthThreshold));
                    sendAlert(alert);
                    activeAlerts.put(alertKey + "_warning", alert);
                }
            } else {
                // Clear any existing health alerts
                StatusAlert cleared = activeAlerts.remove(alertKey + "_critical");
                if (cleared != null) {
                    sendAlert(createSystemAlert(AlertLevel.INFO, "System Health Recovered",
                        String.format("System health has recovered to %.1f%%", currentHealth)));
                }
                cleared = activeAlerts.remove(alertKey + "_warning");
                if (cleared != null) {
                    sendAlert(createSystemAlert(AlertLevel.INFO, "System Health Improved",
                        String.format("System health has improved to %.1f%%", currentHealth)));
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error checking system health", e);
        }
    }
    
    /**
     * Check resource usage limits
     */
    private void checkResourceLimits() {
        try {
            var resourceStatus = statusMonitor.getSystemResourceStatus();
            
            // Check memory usage
            checkResourceLimit("memory", resourceStatus.getMemoryUsagePercent(), 
                "Memory usage", "%.1f%% memory utilization");
            
            // Check heap usage
            checkResourceLimit("heap", resourceStatus.getHeapUsagePercent(), 
                "Heap memory usage", "%.1f%% heap utilization");
            
            // Check thread count
            if (resourceStatus.getThreadCount() > 200) {
                String alertKey = "thread_count";
                if (!activeAlerts.containsKey(alertKey)) {
                    StatusAlert alert = createSystemAlert(AlertLevel.WARNING, "High Thread Count",
                        String.format("Thread count is high: %d active threads", resourceStatus.getThreadCount()));
                    sendAlert(alert);
                    activeAlerts.put(alertKey, alert);
                }
            } else {
                activeAlerts.remove("thread_count");
            }
            
        } catch (Exception e) {
            LOGGER.error("Error checking resource limits", e);
        }
    }
    
    /**
     * Check resource limit with configurable thresholds
     */
    private void checkResourceLimit(String resourceName, double currentUsage, String displayName, String messageFormat) {
        String alertKey = resourceName + "_usage";
        
        if (currentUsage > 95.0) {
            // Critical usage
            if (!activeAlerts.containsKey(alertKey + "_critical")) {
                StatusAlert alert = createSystemAlert(AlertLevel.CRITICAL, displayName + " Critical",
                    String.format(messageFormat + " (Critical: >95%%)", currentUsage));
                sendAlert(alert);
                activeAlerts.put(alertKey + "_critical", alert);
            }
        } else if (currentUsage > 85.0) {
            // Warning usage
            if (!activeAlerts.containsKey(alertKey + "_warning")) {
                StatusAlert alert = createSystemAlert(AlertLevel.WARNING, displayName + " High",
                    String.format(messageFormat + " (Warning: >85%%)", currentUsage));
                sendAlert(alert);
                activeAlerts.put(alertKey + "_warning", alert);
            }
        } else {
            // Clear alerts
            activeAlerts.remove(alertKey + "_critical");
            activeAlerts.remove(alertKey + "_warning");
        }
    }
    
    /**
     * Check enterprise component status
     */
    private void checkComponentStatus() {
        try {
            var componentStatus = statusMonitor.getEnterpriseComponentStatus();
            
            for (Map.Entry<String, SystemStatusMonitor.ComponentStatus> entry : componentStatus.entrySet()) {
                String componentName = entry.getKey();
                var status = entry.getValue();
                String alertKey = "component_" + componentName.toLowerCase().replace(" ", "_");
                
                if (status.getState() == SystemStatusMonitor.ComponentState.ERROR) {
                    if (!activeAlerts.containsKey(alertKey)) {
                        StatusAlert alert = createSystemAlert(AlertLevel.ERROR, "Component Error",
                            String.format("Enterprise component '%s' is in ERROR state: %s", 
                            componentName, status.getMessage()));
                        sendAlert(alert);
                        activeAlerts.put(alertKey, alert);
                    }
                } else if (status.getState() == SystemStatusMonitor.ComponentState.WARNING) {
                    if (!activeAlerts.containsKey(alertKey)) {
                        StatusAlert alert = createSystemAlert(AlertLevel.WARNING, "Component Warning",
                            String.format("Enterprise component '%s' is in WARNING state: %s", 
                            componentName, status.getMessage()));
                        sendAlert(alert);
                        activeAlerts.put(alertKey, alert);
                    }
                } else if (status.getState() == SystemStatusMonitor.ComponentState.ACTIVE) {
                    // Clear any existing alerts for this component
                    StatusAlert cleared = activeAlerts.remove(alertKey);
                    if (cleared != null) {
                        sendAlert(createSystemAlert(AlertLevel.INFO, "Component Recovered",
                            String.format("Enterprise component '%s' has recovered and is now ACTIVE", componentName)));
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error checking component status", e);
        }
    }
    
    /**
     * Create a system alert
     */
    private StatusAlert createSystemAlert(AlertLevel level, String title, String message) {
        return new StatusAlert(
            level,
            title,
            message,
            "SystemMonitor",
            LocalDateTime.now()
        );
    }
    
    /**
     * Send an alert through all notification channels
     */
    public void sendAlert(StatusAlert alert) {
        alertQueue.offer(alert);
        LOGGER.debug("Alert queued: {} - {}", alert.getLevel(), alert.getTitle());
    }
    
    /**
     * Process the notification queue
     */
    private void processNotificationQueue() {
        StatusAlert alert;
        while ((alert = alertQueue.poll()) != null) {
            for (NotificationChannel channel : channels) {
                try {
                    channel.sendNotification(alert);
                } catch (Exception e) {
                    LOGGER.error("Error sending notification through channel: {}", channel.getClass().getSimpleName(), e);
                }
            }
        }
    }
    
    /**
     * Initialize notification channels
     */
    private void initializeNotificationChannels() {
        // Console notification channel
        channels.add(new ConsoleNotificationChannel());
        
        // File logging channel
        channels.add(new FileNotificationChannel());
        
        LOGGER.info("Initialized {} notification channels", channels.size());
    }
    
    /**
     * Get current alert statistics
     */
    public Map<String, Object> getAlertStatistics() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("active_alerts", activeAlerts.size());
        stats.put("queue_size", alertQueue.size());
        stats.put("channels", channels.size());
        stats.put("is_running", isRunning);
        stats.put("health_threshold", healthThreshold);
        stats.put("critical_threshold", criticalThreshold);
        stats.put("monitoring_interval_ms", monitoringInterval);
        return stats;
    }
    
    // Getters and setters
    public boolean isRunning() { return isRunning; }
    public double getHealthThreshold() { return healthThreshold; }
    public void setHealthThreshold(double threshold) { this.healthThreshold = threshold; }
    public double getCriticalThreshold() { return criticalThreshold; }
    public void setCriticalThreshold(double threshold) { this.criticalThreshold = threshold; }
    public long getMonitoringInterval() { return monitoringInterval; }
    public void setMonitoringInterval(long interval) { this.monitoringInterval = interval; }
    
    // Inner classes for alerts and channels
    
    /**
     * Alert level enumeration
     */
    public enum AlertLevel {
        INFO("INFO", "§a"),
        WARNING("WARNING", "§e"),
        ERROR("ERROR", "§c"),
        CRITICAL("CRITICAL", "§4");
        
        private final String name;
        private final String colorCode;
        
        AlertLevel(String name, String colorCode) {
            this.name = name;
            this.colorCode = colorCode;
        }
        
        public String getName() { return name; }
        public String getColorCode() { return colorCode; }
    }
    
    /**
     * Status alert data class
     */
    public static class StatusAlert {
        private final AlertLevel level;
        private final String title;
        private final String message;
        private final String source;
        private final LocalDateTime timestamp;
        
        public StatusAlert(AlertLevel level, String title, String message, String source, LocalDateTime timestamp) {
            this.level = level;
            this.title = title;
            this.message = message;
            this.source = source;
            this.timestamp = timestamp;
        }
        
        public AlertLevel getLevel() { return level; }
        public String getTitle() { return title; }
        public String getMessage() { return message; }
        public String getSource() { return source; }
        public LocalDateTime getTimestamp() { return timestamp; }
        
        @Override
        public String toString() {
            return String.format("[%s] %s: %s - %s", 
                level.getName(), 
                timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss")), 
                title, 
                message);
        }
    }
    
    /**
     * Notification channel interface
     */
    public interface NotificationChannel {
        void sendNotification(StatusAlert alert);
        String getChannelName();
    }
    
    /**
     * Console notification channel
     */
    private static class ConsoleNotificationChannel implements NotificationChannel {
        private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleNotificationChannel.class);
        
        @Override
        public void sendNotification(StatusAlert alert) {
            // Log to console based on alert level
            switch (alert.getLevel()) {
                case CRITICAL:
                case ERROR:
                    LOGGER.error("[ALERT] {}: {}", alert.getTitle(), alert.getMessage());
                    break;
                case WARNING:
                    LOGGER.warn("[ALERT] {}: {}", alert.getTitle(), alert.getMessage());
                    break;
                case INFO:
                default:
                    LOGGER.info("[ALERT] {}: {}", alert.getTitle(), alert.getMessage());
                    break;
            }
        }
        
        @Override
        public String getChannelName() {
            return "Console";
        }
    }
    
    /**
     * File notification channel
     */
    private static class FileNotificationChannel implements NotificationChannel {
        private static final Logger LOGGER = LoggerFactory.getLogger(FileNotificationChannel.class);
        private static final String LOG_FILE = "neoessentials/alerts.log";
        
        @Override
        public void sendNotification(StatusAlert alert) {
            try {
                String logEntry = String.format("%s [%s] %s: %s%n",
                    alert.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    alert.getLevel().getName(),
                    alert.getTitle(),
                    alert.getMessage());
                
                // Append to log file
                try (FileWriter writer = new FileWriter(LOG_FILE, true)) {
                    writer.write(logEntry);
                    writer.flush();
                }
            } catch (IOException e) {
                LOGGER.error("Failed to write alert to log file", e);
            }
        }
        
        @Override
        public String getChannelName() {
            return "File";
        }
    }
}
