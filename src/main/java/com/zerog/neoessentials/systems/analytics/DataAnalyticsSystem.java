package com.zerog.neoessentials.systems.analytics;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.storage.PlayerDataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.lang.management.ManagementFactory;

/**
 * Advanced data analytics system for NeoEssentials
 * Provides comprehensive server analytics, player behavior tracking,
 * and automated reporting capabilities
 * 
 * @author ZeroG
 * @since 2.1.0
 */
public class DataAnalyticsSystem {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataAnalyticsSystem.class);
    
    // Singleton instance
    private static DataAnalyticsSystem instance;
    
    // Core dependencies
    private final ConfigManager configManager;
    private final PlayerDataManager playerDataManager;
    
    // Analytics data stores
    private final Map<String, CommandAnalytics> commandUsage = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerAnalytics> playerAnalytics = new ConcurrentHashMap<>();
    private final Queue<ServerEvent> eventHistory = new ConcurrentLinkedQueue<>();
    private final Map<String, FeatureUsage> featureMetrics = new ConcurrentHashMap<>();
    
    // System metrics
    private final AtomicLong totalCommandsExecuted = new AtomicLong(0);
    private final AtomicLong totalPlayersTracked = new AtomicLong(0);
    private final AtomicLong totalErrorsLogged = new AtomicLong(0);
    
    // Scheduled tasks
    private final ScheduledExecutorService analyticsScheduler = Executors.newScheduledThreadPool(2);
    private ScheduledFuture<?> metricsCollectionTask;
    private ScheduledFuture<?> reportGenerationTask;
    
    private DataAnalyticsSystem() {
        this.configManager = ConfigManager.getInstance();
        this.playerDataManager = PlayerDataManager.getInstance();
        
        // Initialize built-in feature metrics
        initializeFeatureMetrics();
        
        // Start analytics collection
        startAnalyticsCollection();
        
        LOGGER.info("Data Analytics System initialized");
    }
    
    public static DataAnalyticsSystem getInstance() {
        if (instance == null) {
            instance = new DataAnalyticsSystem();
        }
        return instance;
    }
    
    /**
     * Initialize feature metrics tracking
     */
    private void initializeFeatureMetrics() {
        String[] features = {
            "homes", "warps", "economy", "kits", "messaging", 
            "moderation", "spawns", "teleportation", "commands",
            "placeholders", "tablist", "announcements"
        };
        
        for (String feature : features) {
            featureMetrics.put(feature, new FeatureUsage(feature));
        }
    }
    
    /**
     * Start analytics collection tasks
     */
    private void startAnalyticsCollection() {
        // Collect metrics every 30 seconds
        metricsCollectionTask = analyticsScheduler.scheduleAtFixedRate(
            this::collectSystemMetrics, 30, 30, TimeUnit.SECONDS
        );
        
        // Generate reports every hour
        reportGenerationTask = analyticsScheduler.scheduleAtFixedRate(
            this::generateHourlyReport, 3600, 3600, TimeUnit.SECONDS
        );
        
        LOGGER.info("Analytics collection tasks started");
    }
    
    /**
     * Track command execution
     */
    public void trackCommandExecution(String command, UUID playerId, boolean success, long executionTime) {
        // Update command analytics
        commandUsage.computeIfAbsent(command, CommandAnalytics::new)
                   .recordExecution(success, executionTime);
        
        // Update player analytics
        playerAnalytics.computeIfAbsent(playerId, PlayerAnalytics::new)
                      .recordCommand(command, success, executionTime);
        
        // Update global counters
        totalCommandsExecuted.incrementAndGet();
        if (!success) {
            totalErrorsLogged.incrementAndGet();
        }
        
        // Log event
        recordEvent(new ServerEvent("command_execution", Map.of(
            "command", command,
            "player", playerId.toString(),
            "success", success,
            "execution_time", executionTime
        )));
        
        // Update feature metrics
        String feature = categorizeCommand(command);
        if (feature != null) {
            featureMetrics.get(feature).recordUsage();
        }
    }
    
    /**
     * Track player session
     */
    public void trackPlayerSession(UUID playerId, String playerName, SessionEvent event) {
        PlayerAnalytics analytics = playerAnalytics.computeIfAbsent(playerId, PlayerAnalytics::new);
        analytics.setPlayerName(playerName);
        
        switch (event) {
            case JOIN:
                analytics.recordJoin();
                totalPlayersTracked.set(playerAnalytics.size());
                recordEvent(new ServerEvent("player_join", Map.of(
                    "player", playerId.toString(),
                    "name", playerName
                )));
                break;
                
            case LEAVE:
                analytics.recordLeave();
                recordEvent(new ServerEvent("player_leave", Map.of(
                    "player", playerId.toString(),
                    "name", playerName,
                    "session_duration", analytics.getCurrentSessionDuration()
                )));
                break;
        }
    }
    
    /**
     * Track feature usage
     */
    public void trackFeatureUsage(String feature, String action, Map<String, Object> metadata) {
        FeatureUsage usage = featureMetrics.get(feature);
        if (usage != null) {
            usage.recordUsage();
            usage.recordAction(action, metadata);
        }
        
        recordEvent(new ServerEvent("feature_usage", Map.of(
            "feature", feature,
            "action", action,
            "metadata", metadata
        )));
    }
    
    /**
     * Track error occurrence
     */
    public void trackError(String component, String error, Map<String, Object> context) {
        totalErrorsLogged.incrementAndGet();
        
        recordEvent(new ServerEvent("error", Map.of(
            "component", component,
            "error", error,
            "context", context,
            "timestamp", System.currentTimeMillis()
        )));
        
        LOGGER.warn("Analytics tracked error in {}: {}", component, error);
    }
    
    /**
     * Get command analytics
     */
    public List<CommandAnalytics> getTopCommands(int limit) {
        return commandUsage.values().stream()
                          .sorted((a, b) -> Long.compare(b.getTotalExecutions(), a.getTotalExecutions()))
                          .limit(limit)
                          .collect(Collectors.toList());
    }
    
    /**
     * Get player analytics
     */
    public List<PlayerAnalytics> getTopPlayers(int limit) {
        return playerAnalytics.values().stream()
                             .sorted((a, b) -> Long.compare(b.getTotalCommands(), a.getTotalCommands()))
                             .limit(limit)
                             .collect(Collectors.toList());
    }
    
    /**
     * Get feature usage statistics
     */
    public Map<String, FeatureUsage> getFeatureUsageStats() {
        return new HashMap<>(featureMetrics);
    }
    
    /**
     * Generate comprehensive analytics report
     */
    public AnalyticsReport generateReport(ReportType type) {
        long now = System.currentTimeMillis();
        long timeWindow = getTimeWindow(type);
        long startTime = now - timeWindow;
        
        AnalyticsReport report = new AnalyticsReport(type, startTime, now);
        
        // System overview
        report.setTotalCommands(totalCommandsExecuted.get());
        report.setTotalPlayers(totalPlayersTracked.get());
        report.setTotalErrors(totalErrorsLogged.get());
        report.setUptime(ManagementFactory.getRuntimeMXBean().getUptime());
        
        // Top commands
        report.setTopCommands(getTopCommands(10));
        
        // Active players
        List<PlayerAnalytics> activePlayers = playerAnalytics.values().stream()
            .filter(p -> p.getLastActivity() > startTime)
            .sorted((a, b) -> Long.compare(b.getTotalCommands(), a.getTotalCommands()))
            .limit(20)
            .collect(Collectors.toList());
        report.setActivePlayers(activePlayers);
        
        // Feature usage
        report.setFeatureUsage(new HashMap<>(featureMetrics));
        
        // Recent events
        List<ServerEvent> recentEvents = eventHistory.stream()
            .filter(e -> e.getTimestamp() > startTime)
            .sorted((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()))
            .limit(100)
            .collect(Collectors.toList());
        report.setRecentEvents(recentEvents);
        
        // Performance metrics
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> performance = new HashMap<>();
        performance.put("memory_used", runtime.totalMemory() - runtime.freeMemory());
        performance.put("memory_total", runtime.totalMemory());
        performance.put("memory_max", runtime.maxMemory());
        performance.put("processors", runtime.availableProcessors());
        performance.put("threads", Thread.activeCount());
        report.setPerformanceMetrics(performance);
        
        return report;
    }
    
    /**
     * Collect system metrics
     */
    private void collectSystemMetrics() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long memoryUsed = runtime.totalMemory() - runtime.freeMemory();
            long memoryTotal = runtime.totalMemory();
            
            recordEvent(new ServerEvent("system_metrics", Map.of(
                "memory_used", memoryUsed,
                "memory_total", memoryTotal,
                "memory_percent", (memoryUsed * 100.0) / memoryTotal,
                "active_threads", Thread.activeCount(),
                "active_players", playerAnalytics.values().stream()
                    .mapToLong(p -> p.isOnline() ? 1 : 0).sum()
            )));
            
            // Clean old events (keep last 10000)
            while (eventHistory.size() > 10000) {
                eventHistory.poll();
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to collect system metrics", e);
        }
    }
    
    /**
     * Generate hourly report
     */
    private void generateHourlyReport() {
        try {
            AnalyticsReport report = generateReport(ReportType.HOURLY);
            LOGGER.info("Hourly Analytics Report Generated - Commands: {}, Players: {}, Errors: {}",
                report.getTotalCommands(), report.getTotalPlayers(), report.getTotalErrors());
            
            // Save report to file if configured
            // This would integrate with file storage system
            
        } catch (Exception e) {
            LOGGER.error("Failed to generate hourly report", e);
        }
    }
    
    /**
     * Record server event
     */
    private void recordEvent(ServerEvent event) {
        eventHistory.offer(event);
    }
    
    /**
     * Categorize command to feature
     */
    private String categorizeCommand(String command) {
        if (command.startsWith("/home") || command.startsWith("/sethome") || command.startsWith("/delhome")) {
            return "homes";
        } else if (command.startsWith("/warp") || command.startsWith("/setwarp") || command.startsWith("/delwarp")) {
            return "warps";
        } else if (command.startsWith("/balance") || command.startsWith("/pay") || command.startsWith("/eco")) {
            return "economy";
        } else if (command.startsWith("/kit")) {
            return "kits";
        } else if (command.startsWith("/msg") || command.startsWith("/mail") || command.startsWith("/reply")) {
            return "messaging";
        } else if (command.startsWith("/kick") || command.startsWith("/ban") || command.startsWith("/mute")) {
            return "moderation";
        } else if (command.startsWith("/spawn") || command.startsWith("/setspawn")) {
            return "spawns";
        } else if (command.startsWith("/tp") || command.startsWith("/tpa") || command.startsWith("/back")) {
            return "teleportation";
        }
        return "commands";
    }
    
    /**
     * Get time window for report type
     */
    private long getTimeWindow(ReportType type) {
        switch (type) {
            case HOURLY: return 3600_000L; // 1 hour
            case DAILY: return 86400_000L; // 24 hours
            case WEEKLY: return 604800_000L; // 7 days
            case MONTHLY: return 2592000_000L; // 30 days
            default: return 3600_000L;
        }
    }
    
    /**
     * Shutdown analytics system
     */
    public void shutdown() {
        if (metricsCollectionTask != null) {
            metricsCollectionTask.cancel(false);
        }
        if (reportGenerationTask != null) {
            reportGenerationTask.cancel(false);
        }
        
        analyticsScheduler.shutdown();
        
        try {
            if (!analyticsScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                analyticsScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            analyticsScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        LOGGER.info("Data Analytics System shutdown");
    }
    
    // Data classes
    
    public static class CommandAnalytics {
        private final String command;
        private final AtomicLong totalExecutions = new AtomicLong(0);
        private final AtomicLong successfulExecutions = new AtomicLong(0);
        private final AtomicLong totalExecutionTime = new AtomicLong(0);
        private final AtomicLong maxExecutionTime = new AtomicLong(0);
        private final AtomicLong minExecutionTime = new AtomicLong(Long.MAX_VALUE);
        
        public CommandAnalytics(String command) {
            this.command = command;
        }
        
        public void recordExecution(boolean success, long executionTime) {
            totalExecutions.incrementAndGet();
            if (success) {
                successfulExecutions.incrementAndGet();
            }
            
            totalExecutionTime.addAndGet(executionTime);
            maxExecutionTime.updateAndGet(current -> Math.max(current, executionTime));
            minExecutionTime.updateAndGet(current -> Math.min(current, executionTime));
        }
        
        public String getCommand() { return command; }
        public long getTotalExecutions() { return totalExecutions.get(); }
        public long getSuccessfulExecutions() { return successfulExecutions.get(); }
        public double getSuccessRate() { 
            long total = totalExecutions.get();
            return total > 0 ? (successfulExecutions.get() * 100.0) / total : 0.0;
        }
        public long getAverageExecutionTime() {
            long total = totalExecutions.get();
            return total > 0 ? totalExecutionTime.get() / total : 0;
        }
        public long getMaxExecutionTime() { return maxExecutionTime.get(); }
        public long getMinExecutionTime() { 
            long min = minExecutionTime.get();
            return min == Long.MAX_VALUE ? 0 : min;
        }
    }
    
    public static class PlayerAnalytics {
        private String playerName;
        private final AtomicLong totalCommands = new AtomicLong(0);
        private final AtomicLong successfulCommands = new AtomicLong(0);
        private final AtomicLong totalSessionTime = new AtomicLong(0);
        private final AtomicLong joinCount = new AtomicLong(0);
        private final Map<String, AtomicLong> commandCounts = new ConcurrentHashMap<>();
        private volatile long lastActivity = System.currentTimeMillis();
        private volatile long sessionStart = 0;
        private volatile boolean online = false;
        
        public PlayerAnalytics(UUID playerId) {
            // Constructor with player ID
        }
        
        public void recordCommand(String command, boolean success, long executionTime) {
            totalCommands.incrementAndGet();
            if (success) {
                successfulCommands.incrementAndGet();
            }
            
            commandCounts.computeIfAbsent(command, k -> new AtomicLong(0)).incrementAndGet();
            lastActivity = System.currentTimeMillis();
        }
        
        public void recordJoin() {
            joinCount.incrementAndGet();
            sessionStart = System.currentTimeMillis();
            online = true;
            lastActivity = System.currentTimeMillis();
        }
        
        public void recordLeave() {
            if (sessionStart > 0) {
                totalSessionTime.addAndGet(System.currentTimeMillis() - sessionStart);
            }
            online = false;
            sessionStart = 0;
        }
        
        public long getCurrentSessionDuration() {
            return sessionStart > 0 ? System.currentTimeMillis() - sessionStart : 0;
        }
        
        // Getters
        public String getPlayerName() { return playerName; }
        public void setPlayerName(String playerName) { this.playerName = playerName; }
        public long getTotalCommands() { return totalCommands.get(); }
        public long getSuccessfulCommands() { return successfulCommands.get(); }
        public double getCommandSuccessRate() {
            long total = totalCommands.get();
            return total > 0 ? (successfulCommands.get() * 100.0) / total : 0.0;
        }
        public long getTotalSessionTime() { return totalSessionTime.get(); }
        public long getJoinCount() { return joinCount.get(); }
        public long getLastActivity() { return lastActivity; }
        public boolean isOnline() { return online; }
        public Map<String, AtomicLong> getCommandCounts() { return new HashMap<>(commandCounts); }
    }
    
    public static class FeatureUsage {
        private final String feature;
        private final AtomicLong usageCount = new AtomicLong(0);
        private final Map<String, AtomicLong> actionCounts = new ConcurrentHashMap<>();
        private final Queue<Map<String, Object>> recentActions = new ConcurrentLinkedQueue<>();
        private volatile long lastUsed = 0;
        
        public FeatureUsage(String feature) {
            this.feature = feature;
        }
        
        public void recordUsage() {
            usageCount.incrementAndGet();
            lastUsed = System.currentTimeMillis();
        }
        
        public void recordAction(String action, Map<String, Object> metadata) {
            actionCounts.computeIfAbsent(action, k -> new AtomicLong(0)).incrementAndGet();
            
            Map<String, Object> actionData = new HashMap<>(metadata);
            actionData.put("action", action);
            actionData.put("timestamp", System.currentTimeMillis());
            
            recentActions.offer(actionData);
            
            // Keep only last 100 actions
            while (recentActions.size() > 100) {
                recentActions.poll();
            }
        }
        
        public String getFeature() { return feature; }
        public long getUsageCount() { return usageCount.get(); }
        public long getLastUsed() { return lastUsed; }
        public Map<String, AtomicLong> getActionCounts() { return new HashMap<>(actionCounts); }
        public List<Map<String, Object>> getRecentActions() { return new ArrayList<>(recentActions); }
    }
    
    public static class ServerEvent {
        private final String type;
        private final Map<String, Object> data;
        private final long timestamp;
        
        public ServerEvent(String type, Map<String, Object> data) {
            this.type = type;
            this.data = new HashMap<>(data);
            this.timestamp = System.currentTimeMillis();
        }
        
        public String getType() { return type; }
        public Map<String, Object> getData() { return data; }
        public long getTimestamp() { return timestamp; }
    }
    
    public enum SessionEvent {
        JOIN, LEAVE
    }
    
    public enum ReportType {
        HOURLY, DAILY, WEEKLY, MONTHLY
    }
    
    public static class AnalyticsReport {
        private final ReportType type;
        private final long startTime;
        private final long endTime;
        private final String generatedAt;
        
        private long totalCommands;
        private long totalPlayers;
        private long totalErrors;
        private long uptime;
        
        private List<CommandAnalytics> topCommands;
        private List<PlayerAnalytics> activePlayers;
        private Map<String, FeatureUsage> featureUsage;
        private List<ServerEvent> recentEvents;
        private Map<String, Object> performanceMetrics;
        
        public AnalyticsReport(ReportType type, long startTime, long endTime) {
            this.type = type;
            this.startTime = startTime;
            this.endTime = endTime;
            this.generatedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
        
        // Getters and setters
        public ReportType getType() { return type; }
        public long getStartTime() { return startTime; }
        public long getEndTime() { return endTime; }
        public String getGeneratedAt() { return generatedAt; }
        
        public long getTotalCommands() { return totalCommands; }
        public void setTotalCommands(long totalCommands) { this.totalCommands = totalCommands; }
        
        public long getTotalPlayers() { return totalPlayers; }
        public void setTotalPlayers(long totalPlayers) { this.totalPlayers = totalPlayers; }
        
        public long getTotalErrors() { return totalErrors; }
        public void setTotalErrors(long totalErrors) { this.totalErrors = totalErrors; }
        
        public long getUptime() { return uptime; }
        public void setUptime(long uptime) { this.uptime = uptime; }
        
        public List<CommandAnalytics> getTopCommands() { return topCommands; }
        public void setTopCommands(List<CommandAnalytics> topCommands) { this.topCommands = topCommands; }
        
        public List<PlayerAnalytics> getActivePlayers() { return activePlayers; }
        public void setActivePlayers(List<PlayerAnalytics> activePlayers) { this.activePlayers = activePlayers; }
        
        public Map<String, FeatureUsage> getFeatureUsage() { return featureUsage; }
        public void setFeatureUsage(Map<String, FeatureUsage> featureUsage) { this.featureUsage = featureUsage; }
        
        public List<ServerEvent> getRecentEvents() { return recentEvents; }
        public void setRecentEvents(List<ServerEvent> recentEvents) { this.recentEvents = recentEvents; }
        
        public Map<String, Object> getPerformanceMetrics() { return performanceMetrics; }
        public void setPerformanceMetrics(Map<String, Object> performanceMetrics) { this.performanceMetrics = performanceMetrics; }
    }
}
