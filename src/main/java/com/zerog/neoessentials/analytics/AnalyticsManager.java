package com.zerog.neoessentials.analytics;

import com.zerog.neoessentials.config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Comprehensive analytics and reporting system for NeoEssentials
 * Tracks command usage, player behavior, performance metrics, and feature statistics
 */
public class AnalyticsManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnalyticsManager.class);
    private static AnalyticsManager instance;
    private final Map<String, AtomicLong> commandUsageStats = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerSession> playerSessions = new ConcurrentHashMap<>();
    private final List<AnalyticsEvent> eventHistory = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, PerformanceMetric> performanceMetrics = new ConcurrentHashMap<>();
    private final Map<String, FeatureUsageStats> featureStats = new ConcurrentHashMap<>();
    
    // Analytics configuration
    private boolean analyticsEnabled = true;
    private int maxEventHistory = 10000;
    private long sessionTimeoutMinutes = 30;
    
    private AnalyticsManager(ConfigManager configManager) {
        loadConfiguration();
        LOGGER.info("Analytics Manager initialized");
    }
    
    public static synchronized AnalyticsManager getInstance(ConfigManager configManager) {
        if (instance == null) {
            instance = new AnalyticsManager(configManager);
        }
        return instance;
    }
    
    public static AnalyticsManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("AnalyticsManager not initialized");
        }
        return instance;
    }
    
    /**
     * Track command execution with performance metrics
     */
    public void trackCommandExecution(String command, UUID playerUUID, long executionTimeMs, boolean success) {
        if (!analyticsEnabled) return;
        
        try {
            // Update command usage statistics
            commandUsageStats.computeIfAbsent(command, k -> new AtomicLong(0)).incrementAndGet();
            
            // Record analytics event
            AnalyticsEvent event = new AnalyticsEvent(
                AnalyticsEvent.EventType.COMMAND_EXECUTION,
                command,
                playerUUID,
                Map.of(
                    "executionTime", executionTimeMs,
                    "success", success,
                    "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                )
            );
            recordEvent(event);
            
            // Update performance metrics
            updatePerformanceMetric(command, executionTimeMs);
            
            LOGGER.debug("Tracked command execution: {} by {} in {}ms (success: {})", 
                command, playerUUID, executionTimeMs, success);
                
        } catch (Exception e) {
            LOGGER.error("Error tracking command execution: " + e.getMessage(), e);
        }
    }
    
    /**
     * Track player login/logout events
     */
    public void trackPlayerSession(UUID playerUUID, String eventType, String playerName) {
        if (!analyticsEnabled) return;
        
        try {
            LocalDateTime now = LocalDateTime.now();
            
            if ("login".equals(eventType)) {
                PlayerSession session = new PlayerSession(playerUUID, playerName, now);
                playerSessions.put(playerUUID, session);
                
                AnalyticsEvent event = new AnalyticsEvent(
                    AnalyticsEvent.EventType.PLAYER_LOGIN,
                    "player_login",
                    playerUUID,
                    Map.of(
                        "playerName", playerName,
                        "loginTime", now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    )
                );
                recordEvent(event);
                
            } else if ("logout".equals(eventType)) {
                PlayerSession session = playerSessions.remove(playerUUID);
                if (session != null) {
                    session.setLogoutTime(now);
                    long sessionDuration = session.getSessionDurationMinutes();
                    
                    AnalyticsEvent event = new AnalyticsEvent(
                        AnalyticsEvent.EventType.PLAYER_LOGOUT,
                        "player_logout",
                        playerUUID,
                        Map.of(
                            "playerName", playerName,
                            "logoutTime", now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                            "sessionDuration", sessionDuration
                        )
                    );
                    recordEvent(event);
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("Error tracking player session: " + e.getMessage(), e);
        }
    }
    
    /**
     * Track feature usage statistics
     */
    public void trackFeatureUsage(String featureName, String action, Map<String, Object> metadata) {
        if (!analyticsEnabled) return;
        
        try {
            FeatureUsageStats stats = featureStats.computeIfAbsent(featureName, 
                k -> new FeatureUsageStats(featureName));
            stats.recordUsage(action);
            
            AnalyticsEvent event = new AnalyticsEvent(
                AnalyticsEvent.EventType.FEATURE_USAGE,
                featureName + ":" + action,
                null,
                metadata
            );
            recordEvent(event);
            
        } catch (Exception e) {
            LOGGER.error("Error tracking feature usage: " + e.getMessage(), e);
        }
    }
    
    /**
     * Track server performance events
     */
    public void trackServerEvent(String eventType, Map<String, Object> data) {
        if (!analyticsEnabled) return;
        
        try {
            AnalyticsEvent event = new AnalyticsEvent(
                AnalyticsEvent.EventType.SERVER_EVENT,
                eventType,
                null,
                data
            );
            recordEvent(event);
            
        } catch (Exception e) {
            LOGGER.error("Error tracking server event: " + e.getMessage(), e);
        }
    }
    
    /**
     * Generate comprehensive analytics report
     */
    public AnalyticsReport generateReport(String reportType, LocalDateTime startTime, LocalDateTime endTime) {
        try {
            AnalyticsReport report = new AnalyticsReport(reportType, startTime, endTime);
            
            // Command usage statistics
            Map<String, Long> commandStats = new HashMap<>();
            commandUsageStats.forEach((cmd, count) -> commandStats.put(cmd, count.get()));
            report.setCommandUsageStats(commandStats);
            
            // Active player sessions
            report.setActivePlayerSessions(new HashMap<>(playerSessions));
            
            // Performance metrics
            report.setPerformanceMetrics(new HashMap<>(performanceMetrics));
            
            // Feature usage statistics
            report.setFeatureUsageStats(new HashMap<>(featureStats));
            
            // Event summary for time period
            List<AnalyticsEvent> periodEvents = eventHistory.stream()
                .filter(event -> {
                    LocalDateTime eventTime = event.getTimestamp();
                    return eventTime.isAfter(startTime) && eventTime.isBefore(endTime);
                })
                .toList();
            report.setEventSummary(periodEvents);
            
            LOGGER.info("Generated analytics report: {} ({} events)", reportType, periodEvents.size());
            return report;
            
        } catch (Exception e) {
            LOGGER.error("Error generating analytics report: " + e.getMessage(), e);
            return new AnalyticsReport("error", startTime, endTime);
        }
    }
    
    /**
     * Get real-time server statistics
     */
    public Map<String, Object> getRealtimeStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            stats.put("totalCommands", commandUsageStats.values().stream()
                .mapToLong(AtomicLong::get).sum());
            stats.put("activePlayers", playerSessions.size());
            stats.put("totalEvents", eventHistory.size());
            stats.put("featuresTracked", featureStats.size());
            stats.put("analyticsEnabled", analyticsEnabled);
            stats.put("lastUpdate", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            // Top 5 most used commands
            Map<String, Long> topCommands = commandUsageStats.entrySet().stream()
                .sorted(Map.Entry.<String, AtomicLong>comparingByValue(
                    (a, b) -> Long.compare(b.get(), a.get())))
                .limit(5)
                .collect(LinkedHashMap::new, 
                    (map, entry) -> map.put(entry.getKey(), entry.getValue().get()),
                    LinkedHashMap::putAll);
            stats.put("topCommands", topCommands);
            
        } catch (Exception e) {
            LOGGER.error("Error getting realtime stats: " + e.getMessage(), e);
            stats.put("error", e.getMessage());
        }
        
        return stats;
    }
    
    // Private helper methods
    
    private void recordEvent(AnalyticsEvent event) {
        eventHistory.add(event);
        
        // Maintain event history size limit
        if (eventHistory.size() > maxEventHistory) {
            eventHistory.subList(0, eventHistory.size() - maxEventHistory).clear();
        }
    }
    
    private void updatePerformanceMetric(String command, long executionTimeMs) {
        PerformanceMetric metric = performanceMetrics.computeIfAbsent(command, 
            k -> new PerformanceMetric(command));
        metric.recordExecution(executionTimeMs);
    }
    
    private void loadConfiguration() {
        try {
            // Use main config for analytics settings temporarily
            // TODO: Create dedicated AnalyticsConfig class
            analyticsEnabled = true; // Default enabled
            maxEventHistory = 10000; // Default 10k events
            sessionTimeoutMinutes = 30; // Default 30 minutes
            
            LOGGER.info("Analytics configuration loaded - enabled: {}, maxEvents: {}", 
                analyticsEnabled, maxEventHistory);
                
        } catch (Exception e) {
            LOGGER.error("Error loading analytics configuration: " + e.getMessage(), e);
        }
    }
    
    /**
     * Clean up old data and optimize memory usage
     */
    public void performMaintenance() {
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusMinutes(sessionTimeoutMinutes);
            
            // Remove expired player sessions
            playerSessions.entrySet().removeIf(entry -> {
                PlayerSession session = entry.getValue();
                return session.getLoginTime().isBefore(cutoff);
            });
            
            // Clean old events if over limit
            if (eventHistory.size() > maxEventHistory) {
                eventHistory.subList(0, eventHistory.size() - maxEventHistory).clear();
            }
            
            LOGGER.debug("Analytics maintenance completed");
            
        } catch (Exception e) {
            LOGGER.error("Error during analytics maintenance: " + e.getMessage(), e);
        }
    }
    
    // Getters for access to statistics
    public Map<String, AtomicLong> getCommandUsageStats() { return commandUsageStats; }
    public Map<UUID, PlayerSession> getPlayerSessions() { return playerSessions; }
    public List<AnalyticsEvent> getEventHistory() { return new ArrayList<>(eventHistory); }
    public Map<String, PerformanceMetric> getPerformanceMetrics() { return performanceMetrics; }
    public Map<String, FeatureUsageStats> getFeatureStats() { return featureStats; }
    
    public boolean isAnalyticsEnabled() { return analyticsEnabled; }
    public void setAnalyticsEnabled(boolean enabled) { this.analyticsEnabled = enabled; }
}
