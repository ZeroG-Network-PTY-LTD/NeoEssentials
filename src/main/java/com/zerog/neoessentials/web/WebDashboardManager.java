package com.zerog.neoessentials.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.bus.api.SubscribeEvent;

/**
 * Enhanced Web Dashboard Manager for NeoEssentials
 * Provides comprehensive web-based administration interface with real-time monitoring
 */
public class WebDashboardManager {
    private static WebDashboardManager instance;
    
    private HttpServer httpServer;
    private final Map<String, DashboardSession> activeSessions;
    private final Map<String, Object> dashboardData;
    private final Map<String, Long> performanceHistory;
    private final List<Map<String, Object>> alertHistory;
    private final Map<String, Object> widgets;
    private final ScheduledExecutorService scheduler;
    
    private boolean dashboardEnabled;
    private int port;
    private String bindAddress;
    private String theme;
    private boolean enableSSL;
    private boolean enableAuth;
    private String adminPassword;
    private int maxSessions;
    private long sessionTimeout;
    private boolean enableRealTimeUpdates;
    private int updateInterval;
    
    
    private WebDashboardManager() {
        this.activeSessions = new ConcurrentHashMap<>();
        this.dashboardData = new ConcurrentHashMap<>();
        this.widgets = new ConcurrentHashMap<>();
        this.performanceHistory = new ConcurrentHashMap<>();
        this.alertHistory = Collections.synchronizedList(new ArrayList<>());
        this.scheduler = Executors.newScheduledThreadPool(2);
        
        // Default configuration
        this.dashboardEnabled = false;
        this.port = 8080;
        this.bindAddress = "0.0.0.0";
        this.theme = "dark";
        this.enableSSL = false;
        this.enableAuth = true;
        this.adminPassword = "admin123"; // Should be changed in production
        this.maxSessions = 10;
        this.sessionTimeout = 3600000; // 1 hour
        this.enableRealTimeUpdates = true;
        this.updateInterval = 5; // 5 seconds
        
        // Register for server events
        NeoForge.EVENT_BUS.register(this);
        
        // Initialize widgets
        initializeWidgets();
        
        com.zerog.neoessentials.util.DebugUtil.infoLog("Enhanced WebDashboardManager initialized with advanced features");
    }
    
    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }
    
    public static WebDashboardManager getInstance() {
        if (instance == null) {
            instance = new WebDashboardManager();
        }
        return instance;
    }
    
    /**
     * Enhanced start method with better configuration and monitoring
     */
    public boolean start() {
        if (dashboardEnabled) {
            com.zerog.neoessentials.util.DebugUtil.warnLog("Dashboard is already running on port " + port);
            return false;
        }
        
        try {
            // Validate configuration
            if (!validateConfiguration()) {
                com.zerog.neoessentials.util.DebugUtil.errorLog("Invalid dashboard configuration");
                return false;
            }
            
            // Initialize enhanced dashboard data
            initializeEnhancedDashboardData();
            
            // Create and start HTTP server
            httpServer = HttpServer.create(new InetSocketAddress(bindAddress, port), 0);
            
            // Set up request handlers
            httpServer.createContext("/", new DashboardHandler());
            httpServer.createContext("/api/data", new ApiDataHandler());
            httpServer.createContext("/api/stats", new ApiStatsHandler());
            httpServer.createContext("/api/players", new ApiPlayersHandler());
            
            httpServer.setExecutor(Executors.newFixedThreadPool(4));
            httpServer.start();
            
            // Start performance monitoring
            startPerformanceMonitoring();
            
            // Start real-time updates if enabled
            if (enableRealTimeUpdates) {
                startRealTimeUpdates();
            }
            
            dashboardEnabled = true;
            
            // Add startup event
            addRealTimeEvent("SYSTEM", "Web Dashboard started on port " + port, "INFO");
            
            com.zerog.neoessentials.util.DebugUtil.infoLog("Enhanced web dashboard started on " + bindAddress + ":" + port);
            return true;
        } catch (Exception e) {
            com.zerog.neoessentials.util.DebugUtil.errorLog("Failed to start enhanced web dashboard: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Enhanced stop method with proper cleanup
     */
    public void stop() {
        if (!dashboardEnabled) {
            return;
        }
        
        try {
            // Stop HTTP server
            if (httpServer != null) {
                httpServer.stop(2);
                httpServer = null;
            }
            
            // Stop schedulers
            if (scheduler != null && !scheduler.isShutdown()) {
                scheduler.shutdown();
                try {
                    if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                        scheduler.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    scheduler.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
            
            // Clear sessions and data
            activeSessions.clear();
            performanceHistory.clear();
            
            dashboardEnabled = false;
            
            // Add stop event
            addRealTimeEvent("SYSTEM", "Web Dashboard stopped", "INFO");
            
            com.zerog.neoessentials.util.DebugUtil.infoLog("Enhanced web dashboard stopped and cleaned up");
        } catch (Exception e) {
            com.zerog.neoessentials.util.DebugUtil.errorLog("Error stopping dashboard: " + e.getMessage());
        }
    }
    
    /**
     * Validate dashboard configuration
     */
    private boolean validateConfiguration() {
        if (port < 1024 || port > 65535) {
            com.zerog.neoessentials.util.DebugUtil.errorLog("Invalid port number: " + port);
            return false;
        }
        
        if (maxSessions < 1 || maxSessions > 100) {
            com.zerog.neoessentials.util.DebugUtil.errorLog("Invalid max sessions: " + maxSessions);
            return false;
        }
        
        if (sessionTimeout < 60000) { // Minimum 1 minute
            com.zerog.neoessentials.util.DebugUtil.errorLog("Session timeout too short: " + sessionTimeout);
            return false;
        }
        
        return true;
    }
    
    /**
     * Initialize widgets for the dashboard
     */
    private void initializeWidgets() {
        // Server Status Widget
        Map<String, Object> serverWidget = new HashMap<>();
        serverWidget.put("type", "server_status");
        serverWidget.put("title", "Server Status");
        serverWidget.put("position", Map.of("x", 0, "y", 0, "width", 6, "height", 4));
        serverWidget.put("refreshRate", 5000);
        widgets.put("server_status", serverWidget);
        
        // Performance Widget
        Map<String, Object> perfWidget = new HashMap<>();
        perfWidget.put("type", "performance");
        perfWidget.put("title", "Performance Metrics");
        perfWidget.put("position", Map.of("x", 6, "y", 0, "width", 6, "height", 4));
        perfWidget.put("refreshRate", 2000);
        widgets.put("performance", perfWidget);
        
        // Economy Widget
        Map<String, Object> econWidget = new HashMap<>();
        econWidget.put("type", "economy");
        econWidget.put("title", "Economy Overview");
        econWidget.put("position", Map.of("x", 0, "y", 4, "width", 8, "height", 4));
        econWidget.put("refreshRate", 10000);
        widgets.put("economy", econWidget);
        
        // Recent Events Widget
        Map<String, Object> eventsWidget = new HashMap<>();
        eventsWidget.put("type", "events");
        eventsWidget.put("title", "Recent Events");
        eventsWidget.put("position", Map.of("x", 8, "y", 4, "width", 4, "height", 8));
        eventsWidget.put("refreshRate", 3000);
        widgets.put("events", eventsWidget);
        
        com.zerog.neoessentials.util.DebugUtil.infoLog("Dashboard widgets initialized");
    }
    
    /**
     * Start performance monitoring
     */
    private void startPerformanceMonitoring() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                collectPerformanceMetrics();
                checkPerformanceAlerts();
            } catch (Exception e) {
                com.zerog.neoessentials.util.DebugUtil.errorLog("Error in performance monitoring: " + e.getMessage());
            }
        }, 0, updateInterval, TimeUnit.SECONDS);
        
        com.zerog.neoessentials.util.DebugUtil.infoLog("Performance monitoring started");
    }
    
    /**
     * Start real-time updates
     */
    private void startRealTimeUpdates() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                updateRealTimeData();
                broadcastUpdates();
            } catch (Exception e) {
                com.zerog.neoessentials.util.DebugUtil.errorLog("Error in real-time updates: " + e.getMessage());
            }
        }, 0, updateInterval, TimeUnit.SECONDS);
        
        com.zerog.neoessentials.util.DebugUtil.infoLog("Real-time updates started");
    }
    
    /**
     * Initialize enhanced dashboard data
     */
    private void initializeEnhancedDashboardData() {
        // Call original initialization
        initializeDashboardData();
        
        // Add enhanced data
        dashboardData.put("dashboard_version", "2.0");
        dashboardData.put("start_time", System.currentTimeMillis());
        dashboardData.put("theme", theme);
        dashboardData.put("ssl_enabled", enableSSL);
        dashboardData.put("auth_enabled", enableAuth);
        dashboardData.put("max_sessions", maxSessions);
        dashboardData.put("session_timeout", sessionTimeout);
        dashboardData.put("real_time_enabled", enableRealTimeUpdates);
        dashboardData.put("update_interval", updateInterval);
        
        // Initialize performance history
        dashboardData.put("performance_history", new ArrayList<>());
        dashboardData.put("alert_history", new ArrayList<>());
        dashboardData.put("session_history", new ArrayList<>());
        
        // Initialize API endpoints
        dashboardData.put("api_endpoints", initializeAPIEndpoints());
        
        // Initialize security metrics
        dashboardData.put("security_events", new ArrayList<>());
        dashboardData.put("failed_logins", 0);
        dashboardData.put("successful_logins", 0);
        
        com.zerog.neoessentials.util.DebugUtil.infoLog("Enhanced dashboard data initialized");
    }
    
    /**
     * Collect performance metrics
     */
    private void collectPerformanceMetrics() {
        long currentTime = System.currentTimeMillis();
        
        // Collect system metrics
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        double memoryUsagePercent = (double) usedMemory / totalMemory * 100;
        
        // Store in performance history
        performanceHistory.put("memory_" + currentTime, (long) memoryUsagePercent);
        
        // Update dashboard data
        dashboardData.put("memory_usage_percent", memoryUsagePercent);
        dashboardData.put("memory_used_mb", usedMemory / 1024 / 1024);
        dashboardData.put("memory_total_mb", totalMemory / 1024 / 1024);
        dashboardData.put("last_performance_update", currentTime);
        
        // Clean old performance history (keep last 100 entries)
        if (performanceHistory.size() > 100) {
            performanceHistory.entrySet().removeIf(entry -> 
                currentTime - Long.parseLong(entry.getKey().split("_")[1]) > 600000); // 10 minutes
        }
    }
    
    /**
     * Check for performance alerts
     */
    private void checkPerformanceAlerts() {
        double memoryUsage = (double) dashboardData.getOrDefault("memory_usage_percent", 0.0);
        double tps = (double) dashboardData.getOrDefault("tps", 20.0);
        
        // Memory usage alert
        if (memoryUsage > 90) {
            createAlert("HIGH_MEMORY_USAGE", "Memory usage critical: " + String.format("%.1f%%", memoryUsage), "ERROR");
        } else if (memoryUsage > 75) {
            createAlert("MEDIUM_MEMORY_USAGE", "Memory usage high: " + String.format("%.1f%%", memoryUsage), "WARN");
        }
        
        // TPS alert
        if (tps < 15) {
            createAlert("LOW_TPS", "Server TPS critically low: " + String.format("%.1f", tps), "ERROR");
        } else if (tps < 18) {
            createAlert("MEDIUM_TPS", "Server TPS low: " + String.format("%.1f", tps), "WARN");
        }
    }
    
    /**
     * Update real-time data
     */
    private void updateRealTimeData() {
        long currentTime = System.currentTimeMillis();
        
        // Update uptime
        long startTime = (long) dashboardData.getOrDefault("start_time", currentTime);
        dashboardData.put("uptime_seconds", (currentTime - startTime) / 1000);
        
        // Update session count
        dashboardData.put("active_sessions", activeSessions.size());
        
        // Update timestamp
        dashboardData.put("last_update", currentTime);
        dashboardData.put("last_update_formatted", 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    }
    
    /**
     * Broadcast updates to connected sessions
     */
    private void broadcastUpdates() {
        // In a real implementation, this would send WebSocket updates
        // For now, we just log the update
        int activeSessionCount = activeSessions.size();
        if (activeSessionCount > 0) {
            com.zerog.neoessentials.util.DebugUtil.debugLog("Broadcasting updates to " + activeSessionCount + " active sessions");
        }
    }
    
    /**
     * Initialize API endpoints
     */
    private Map<String, Object> initializeAPIEndpoints() {
        Map<String, Object> endpoints = new HashMap<>();
        
        endpoints.put("/api/status", "Server status information");
        endpoints.put("/api/performance", "Performance metrics");
        endpoints.put("/api/economy", "Economy statistics");
        endpoints.put("/api/players", "Online players information");
        endpoints.put("/api/events", "Recent events");
        endpoints.put("/api/alerts", "System alerts");
        endpoints.put("/api/sessions", "Active sessions");
        endpoints.put("/api/config", "Dashboard configuration");
        endpoints.put("/api/widgets", "Dashboard widgets");
        endpoints.put("/api/security", "Security metrics");
        
        return endpoints;
    }
    
    /**
     * Create an alert
     */
    private void createAlert(String type, String message, String severity) {
        Map<String, Object> alert = new HashMap<>();
        alert.put("type", type);
        alert.put("message", message);
        alert.put("severity", severity);
        alert.put("timestamp", System.currentTimeMillis());
        alert.put("acknowledged", false);
        
        alertHistory.add(alert);
        
        // Keep only last 50 alerts
        if (alertHistory.size() > 50) {
            alertHistory.remove(0);
        }
        
        // Also add as real-time event
        addRealTimeEvent("ALERT", message, severity);
        
        com.zerog.neoessentials.util.DebugUtil.infoLog("Alert created: [" + severity + "] " + type + " - " + message);
    }
    
    /**
     * Initialize dashboard data (original method)
     */
    private void initializeDashboardData() {
        dashboardData.put("server_status", "online");
        dashboardData.put("player_count", 0);
        dashboardData.put("uptime", System.currentTimeMillis());
        dashboardData.put("version", "1.0.2");
        
        // Enhanced shop and economy data
        dashboardData.put("total_shops", 0);
        dashboardData.put("active_shops", 0);
        dashboardData.put("daily_transactions", 0);
        dashboardData.put("daily_revenue", 0.0);
        dashboardData.put("total_economy_balance", 0.0);
        dashboardData.put("active_auctions", 0);
        dashboardData.put("pending_trades", 0);
        
        // Performance metrics
        dashboardData.put("tps", 20.0);
        dashboardData.put("memory_usage", 0);
        dashboardData.put("cpu_usage", 0);
        dashboardData.put("disk_usage", 0);
        
        // Security metrics
        dashboardData.put("banned_players", 0);
        dashboardData.put("login_attempts", 0);
        dashboardData.put("suspicious_activity", 0);
        
        com.zerog.neoessentials.util.DebugUtil.infoLog("Enhanced dashboard data initialized with shop and economy metrics");
    }
    
    /**
     * Update dashboard data
     */
    public void updateData(String key, Object value) {
        dashboardData.put(key, value);
    }
    
    /**
     * Get dashboard data
     */
    public Map<String, Object> getDashboardData() {
        return new HashMap<>(dashboardData);
    }
    
    /**
     * Enhanced session creation with better security
     */
    public String createSession(String username, String ipAddress) {
        // Check session limits
        if (activeSessions.size() >= maxSessions) {
            com.zerog.neoessentials.util.DebugUtil.warnLog("Maximum sessions reached, rejecting new session for: " + username);
            return null;
        }
        
        String sessionId = UUID.randomUUID().toString();
        DashboardSession session = new DashboardSession(sessionId, username, System.currentTimeMillis(), ipAddress);
        activeSessions.put(sessionId, session);
        
        // Log successful login
        int successfulLogins = (int) dashboardData.getOrDefault("successful_logins", 0);
        dashboardData.put("successful_logins", successfulLogins + 1);
        
        // Add security event
        addSecurityEvent("LOGIN_SUCCESS", "User " + username + " logged in from " + ipAddress, "INFO");
        
        com.zerog.neoessentials.util.DebugUtil.infoLog("Created dashboard session for user: " + username + " from " + ipAddress);
        return sessionId;
    }
    
    /**
     * Enhanced session validation with configurable timeout
     */
    public boolean validateSession(String sessionId) {
        DashboardSession session = activeSessions.get(sessionId);
        if (session == null) {
            return false;
        }
        
        // Check if session is expired
        long sessionAge = System.currentTimeMillis() - session.getCreatedTime();
        if (sessionAge > sessionTimeout) {
            activeSessions.remove(sessionId);
            addSecurityEvent("SESSION_EXPIRED", "Session expired for user: " + session.getUsername(), "INFO");
            return false;
        }
        
        // Update last activity
        session.updateLastActivity();
        return true;
    }
    
    /**
     * Get all dashboard widgets
     */
    public Map<String, Object> getWidgets() {
        return new HashMap<>(widgets);
    }
    
    /**
     * Get performance history
     */
    public Map<String, Long> getPerformanceHistory() {
        return new HashMap<>(performanceHistory);
    }
    
    /**
     * Get alert history
     */
    public List<Map<String, Object>> getAlertHistory() {
        return new ArrayList<>(alertHistory);
    }
    
    /**
     * Get security events
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getSecurityEvents() {
        return (List<Map<String, Object>>) dashboardData.getOrDefault("security_events", new ArrayList<>());
    }
    
    /**
     * Add security event
     */
    private void addSecurityEvent(String type, String message, String severity) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", type);
        event.put("message", message);
        event.put("severity", severity);
        event.put("timestamp", System.currentTimeMillis());
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> securityEvents = (List<Map<String, Object>>) 
                dashboardData.computeIfAbsent("security_events", k -> new ArrayList<>());
        
        securityEvents.add(0, event);
        if (securityEvents.size() > 100) {
            securityEvents.remove(securityEvents.size() - 1);
        }
    }
    
    /**
     * Get dashboard configuration
     */
    public Map<String, Object> getConfiguration() {
        Map<String, Object> config = new HashMap<>();
        config.put("port", port);
        config.put("bind_address", bindAddress);
        config.put("theme", theme);
        config.put("ssl_enabled", enableSSL);
        config.put("auth_enabled", enableAuth);
        config.put("max_sessions", maxSessions);
        config.put("session_timeout", sessionTimeout);
        config.put("real_time_enabled", enableRealTimeUpdates);
        config.put("update_interval", updateInterval);
        return config;
    }
    
    /**
     * Update dashboard configuration
     */
    public boolean updateConfiguration(Map<String, Object> newConfig) {
        try {
            if (newConfig.containsKey("theme")) {
                theme = (String) newConfig.get("theme");
            }
            if (newConfig.containsKey("max_sessions")) {
                maxSessions = (int) newConfig.get("max_sessions");
            }
            if (newConfig.containsKey("session_timeout")) {
                sessionTimeout = (long) newConfig.get("session_timeout");
            }
            if (newConfig.containsKey("real_time_enabled")) {
                enableRealTimeUpdates = (boolean) newConfig.get("real_time_enabled");
            }
            if (newConfig.containsKey("update_interval")) {
                updateInterval = (int) newConfig.get("update_interval");
            }
            
            addRealTimeEvent("CONFIG", "Dashboard configuration updated", "INFO");
            return true;
        } catch (Exception e) {
            com.zerog.neoessentials.util.DebugUtil.errorLog("Failed to update configuration: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Authenticate user with password
     */
    public boolean authenticateUser(String username, String password) {
        if (!enableAuth) {
            return true; // Auth disabled
        }
        
        // Simple password check (in production, use proper hashing)
        boolean authenticated = "admin".equals(username) && adminPassword.equals(password);
        
        if (!authenticated) {
            int failedLogins = (int) dashboardData.getOrDefault("failed_logins", 0);
            dashboardData.put("failed_logins", failedLogins + 1);
            addSecurityEvent("LOGIN_FAILED", "Failed login attempt for user: " + username, "WARN");
        }
        
        return authenticated;
    }
    
    /**
     * Remove a dashboard session
     */
    public void removeSession(String sessionId) {
        activeSessions.remove(sessionId);
    }
    
    /**
     * Get active sessions count
     */
    public int getActiveSessionsCount() {
        return activeSessions.size();
    }
    
    /**
     * Check if dashboard is enabled
     */
    public boolean isDashboardEnabled() {
        return dashboardEnabled;
    }
    
    /**
     * Set dashboard port
     */
    public void setPort(int port) {
        if (!dashboardEnabled) {
            this.port = port;
            com.zerog.neoessentials.util.DebugUtil.infoLog("Dashboard port set to " + port);
        } else {
            com.zerog.neoessentials.util.DebugUtil.warnLog("Cannot change port while dashboard is running");
        }
    }
    
    /**
     * Get dashboard port
     */
    public int getPort() {
        return port;
    }
    
    /**
     * Get shop analytics data
     */
    public Map<String, Object> getShopAnalytics() {
        Map<String, Object> analytics = new HashMap<>();
        analytics.put("total_shops", dashboardData.getOrDefault("total_shops", 0));
        analytics.put("active_shops", dashboardData.getOrDefault("active_shops", 0));
        analytics.put("daily_transactions", dashboardData.getOrDefault("daily_transactions", 0));
        analytics.put("daily_revenue", dashboardData.getOrDefault("daily_revenue", 0.0));
        analytics.put("average_transaction", calculateAverageTransaction());
        analytics.put("top_selling_category", getTopSellingCategory());
        analytics.put("most_active_shop", getMostActiveShop());
        return analytics;
    }
    
    /**
     * Get economy health metrics
     */
    public Map<String, Object> getEconomyHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("total_balance", dashboardData.getOrDefault("total_economy_balance", 0.0));
        health.put("average_balance", calculateAveragePlayerBalance());
        health.put("inflation_rate", calculateInflationRate());
        health.put("money_velocity", calculateMoneyVelocity());
        health.put("economy_status", getEconomyStatus());
        return health;
    }
    
    /**
     * Get server performance data
     */
    public Map<String, Object> getServerPerformance() {
        Map<String, Object> performance = new HashMap<>();
        performance.put("tps", dashboardData.getOrDefault("tps", 20.0));
        performance.put("memory_usage", dashboardData.getOrDefault("memory_usage", 0));
        performance.put("cpu_usage", dashboardData.getOrDefault("cpu_usage", 0));
        performance.put("disk_usage", dashboardData.getOrDefault("disk_usage", 0));
        performance.put("network_io", getNetworkIO());
        performance.put("chunk_loading", getChunkLoadingStats());
        return performance;
    }
    
    /**
     * Update shop metrics
     */
    public void updateShopMetrics(int totalShops, int activeShops, int dailyTransactions, double dailyRevenue) {
        dashboardData.put("total_shops", totalShops);
        dashboardData.put("active_shops", activeShops);
        dashboardData.put("daily_transactions", dailyTransactions);
        dashboardData.put("daily_revenue", dailyRevenue);
    com.zerog.neoessentials.util.DebugUtil.debugLog("Updated shop metrics: " + totalShops + " total, " + activeShops + " active, " + dailyTransactions + " transactions, $" + dailyRevenue);
    }
    
    /**
     * Update performance metrics
     */
    public void updatePerformanceMetrics(double tps, int memoryUsage, int cpuUsage) {
        dashboardData.put("tps", tps);
        dashboardData.put("memory_usage", memoryUsage);
        dashboardData.put("cpu_usage", cpuUsage);
    }
    
    /**
     * Add real-time event for dashboard
     */
    public void addRealTimeEvent(String eventType, String message, String severity) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", eventType);
        event.put("message", message);
        event.put("severity", severity);
        event.put("timestamp", System.currentTimeMillis());
        
        // Store recent events (last 50)
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> events = (List<Map<String, Object>>) 
                dashboardData.computeIfAbsent("recent_events", k -> new ArrayList<>());
        
        events.add(0, event); // Add to front
        if (events.size() > 50) {
            events.remove(events.size() - 1); // Remove oldest
        }
        
    com.zerog.neoessentials.util.DebugUtil.infoLog("Dashboard event: [" + severity + "] " + eventType + " - " + message);
    }
    
    // Helper methods for analytics
    private double calculateAverageTransaction() {
        int transactions = (int) dashboardData.getOrDefault("daily_transactions", 0);
        double revenue = (double) dashboardData.getOrDefault("daily_revenue", 0.0);
        return transactions > 0 ? revenue / transactions : 0.0;
    }
    
    private String getTopSellingCategory() {
        // Placeholder - would integrate with shop system
        return "General Items";
    }
    
    private String getMostActiveShop() {
        // Placeholder - would integrate with shop system
        return "Server Economy Hub";
    }
    
    private double calculateAveragePlayerBalance() {
        // Placeholder - would integrate with economy system
        return 1500.0;
    }
    
    private double calculateInflationRate() {
        // Placeholder - would calculate based on price history
        return 2.3;
    }
    
    private double calculateMoneyVelocity() {
        // Placeholder - would calculate money circulation speed
        return 1.2;
    }
    
    private String getEconomyStatus() {
        double totalBalance = (double) dashboardData.getOrDefault("total_economy_balance", 0.0);
        if (totalBalance > 100000) return "Healthy";
        if (totalBalance > 50000) return "Stable";
        if (totalBalance > 10000) return "Growing";
        return "New";
    }
    
    private Map<String, Object> getNetworkIO() {
        Map<String, Object> network = new HashMap<>();
        network.put("bytes_sent", 0);
        network.put("bytes_received", 0);
        network.put("packets_sent", 0);
        network.put("packets_received", 0);
        return network;
    }
    
    private Map<String, Object> getChunkLoadingStats() {
        Map<String, Object> chunks = new HashMap<>();
        chunks.put("loaded_chunks", 0);
        chunks.put("generated_chunks", 0);
        chunks.put("chunk_generation_rate", 0);
        return chunks;
    }
    
    /**
     * Enhanced Dashboard session class with IP tracking and activity monitoring
     */
    public static class DashboardSession {
        private final String sessionId;
        private final String username;
        private final long createdTime;
        private final String ipAddress;
        private long lastActivity;
        private int requestCount;
        
        public DashboardSession(String sessionId, String username, long createdTime) {
            this(sessionId, username, createdTime, "unknown");
        }
        
        public DashboardSession(String sessionId, String username, long createdTime, String ipAddress) {
            this.sessionId = sessionId;
            this.username = username;
            this.createdTime = createdTime;
            this.ipAddress = ipAddress;
            this.lastActivity = createdTime;
            this.requestCount = 0;
        }
        
        public String getSessionId() {
            return sessionId;
        }
        
        public String getUsername() {
            return username;
        }
        
        public long getCreatedTime() {
            return createdTime;
        }
        
        public String getIpAddress() {
            return ipAddress;
        }
        
        public long getLastActivity() {
            return lastActivity;
        }
        
        public void updateLastActivity() {
            this.lastActivity = System.currentTimeMillis();
            this.requestCount++;
        }
        
        public int getRequestCount() {
            return requestCount;
        }
        
        public long getSessionDuration() {
            return System.currentTimeMillis() - createdTime;
        }
        
        public boolean isExpired(long timeoutMs) {
            return (System.currentTimeMillis() - lastActivity) > timeoutMs;
        }
    }
    
    /**
     * HTTP handler for the main dashboard page
     */
    private class DashboardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                String response = loadDashboardHtml();
                exchange.getResponseHeaders().set("Content-Type", "text/html");
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
                
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes(StandardCharsets.UTF_8));
                }
            } catch (Exception e) {
                com.zerog.neoessentials.util.DebugUtil.errorLog("Error serving dashboard: " + e.getMessage());
                sendErrorResponse(exchange, 500, "Internal Server Error");
            }
        }
    }
    
    /**
     * HTTP handler for API data endpoint
     */
    private class ApiDataHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                updateRealTimeData();
                
                com.google.gson.Gson gson = new com.google.gson.Gson();
                String response = gson.toJson(dashboardData);
                
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
                
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes(StandardCharsets.UTF_8));
                }
            } catch (Exception e) {
                com.zerog.neoessentials.util.DebugUtil.errorLog("Error serving API data: " + e.getMessage());
                sendErrorResponse(exchange, 500, "Internal Server Error");
            }
        }
    }
    
    /**
     * HTTP handler for API stats endpoint
     */
    private class ApiStatsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                Map<String, Object> stats = getPerformanceMetrics();
                
                com.google.gson.Gson gson = new com.google.gson.Gson();
                String response = gson.toJson(stats);
                
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
                
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes(StandardCharsets.UTF_8));
                }
            } catch (Exception e) {
                com.zerog.neoessentials.util.DebugUtil.errorLog("Error serving API stats: " + e.getMessage());
                sendErrorResponse(exchange, 500, "Internal Server Error");
            }
        }
    }
    
    /**
     * HTTP handler for API players endpoint
     */
    private class ApiPlayersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                Map<String, Object> data = new HashMap<>();
                data.put("players", dashboardData.get("players"));
                data.put("player_count", dashboardData.get("player_count"));
                
                com.google.gson.Gson gson = new com.google.gson.Gson();
                String response = gson.toJson(data);
                
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
                
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes(StandardCharsets.UTF_8));
                }
            } catch (Exception e) {
                com.zerog.neoessentials.util.DebugUtil.errorLog("Error serving API players: " + e.getMessage());
                sendErrorResponse(exchange, 500, "Internal Server Error");
            }
        }
    }
    
    /**
     * Load dashboard HTML from resources
     */
    private String loadDashboardHtml() throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream("/assets/neoessentials/web/dashboard.html")) {
            if (inputStream == null) {
                throw new IOException("Dashboard HTML file not found in resources");
            }
            
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    
    /**
     * Get performance metrics for stats API
     */
    private Map<String, Object> getPerformanceMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        // Get server instance
        net.minecraft.server.MinecraftServer server = null;
        try {
            server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        } catch (Exception e) {
            com.zerog.neoessentials.util.DebugUtil.warnLog("Could not get server instance for metrics: " + e.getMessage());
        }
        
        if (server != null) {
            // Server performance metrics (simplified for compatibility)
            metrics.put("tps", 20.0); // Default TPS since getAverageTickTime() may not be available
            metrics.put("tick_time", 50.0); // Default tick time
            
            // Memory metrics
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            metrics.put("memory_used", usedMemory / 1024 / 1024); // MB
            metrics.put("memory_max", maxMemory / 1024 / 1024); // MB
            metrics.put("memory_usage", maxMemory > 0 ? ((double) usedMemory / maxMemory) * 100 : 0);
            
            // Player metrics
            metrics.put("player_count", server.getPlayerCount());
            metrics.put("max_players", server.getMaxPlayers());
            
            // Basic uptime calculation
            metrics.put("uptime", System.currentTimeMillis() / 1000); // Simple uptime in seconds
        } else {
            // Default values when server is not available
            metrics.put("tps", 20.0);
            metrics.put("tick_time", 50.0);
            metrics.put("memory_used", 0);
            metrics.put("memory_max", 0);
            metrics.put("memory_usage", 0);
            metrics.put("player_count", 0);
            metrics.put("max_players", 20);
            metrics.put("uptime", 0);
        }
        
        return metrics;
    }
    
    /**
     * Send error response
     */
    private void sendErrorResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        byte[] response = message.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain");
        exchange.sendResponseHeaders(statusCode, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }
}
