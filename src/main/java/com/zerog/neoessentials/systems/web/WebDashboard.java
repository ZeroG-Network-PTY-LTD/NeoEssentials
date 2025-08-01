package com.zerog.neoessentials.systems.web;

import com.zerog.neoessentials.systems.analytics.DataAnalyticsSystem;
import com.zerog.neoessentials.systems.automation.CommandScheduler;
import com.zerog.neoessentials.systems.compatibility.PluginCompatibilityManager;
import com.zerog.neoessentials.systems.status.SystemStatusMonitor;
import com.zerog.neoessentials.systems.notifications.AlertNotificationSystem;
import com.zerog.neoessentials.utils.PerformanceMonitor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

/**
 * Advanced web dashboard for NeoEssentials remote administration
 * Provides real-time monitoring, configuration management, and analytics
 * 
 * @author ZeroG
 * @since 2.1.0
 */
public class WebDashboard {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebDashboard.class);
    
    // Singleton instance
    private static WebDashboard instance;
    
    // Web server
    private HttpServer server;
    private final int port;
    private boolean isRunning = false;
    
    // Dependencies
    private final DataAnalyticsSystem analytics = DataAnalyticsSystem.getInstance();
    private final CommandScheduler scheduler = CommandScheduler.getInstance();
    private final PluginCompatibilityManager compatibility = PluginCompatibilityManager.getInstance();
    private final SystemStatusMonitor statusMonitor = SystemStatusMonitor.getInstance();
    private final AlertNotificationSystem alertSystem = AlertNotificationSystem.getInstance();
    private final PerformanceMonitor performance = PerformanceMonitor.getInstance();
    
    // JSON serialization
    private final Gson gson = new GsonBuilder()
        .setPrettyPrinting()
        .setDateFormat("yyyy-MM-dd HH:mm:ss")
        .create();
    
    // Authentication and sessions
    private final Map<String, DashboardSession> activeSessions = new ConcurrentHashMap<>();
    private final Map<String, DashboardUser> authorizedUsers = new ConcurrentHashMap<>();
    
    // Real-time data
    private final Map<String, Object> realtimeData = new ConcurrentHashMap<>();
    
    public WebDashboard() {
        this(8080); // Default port
    }
    
    public WebDashboard(int port) {
        this.port = port;
        initializeDefaultUsers();
    }
    
    public static WebDashboard getInstance() {
        if (instance == null) {
            instance = new WebDashboard();
        }
        return instance;
    }
    
    /**
     * Start the web dashboard server
     */
    public void start() {
        if (isRunning) {
            LOGGER.warn("Web dashboard is already running");
            return;
        }
        
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            
            // Register endpoints
            registerEndpoints();
            
            // Start server with thread pool
            server.setExecutor(Executors.newFixedThreadPool(10));
            server.start();
            
            isRunning = true;
            
            LOGGER.info("Web Dashboard started on port {}", port);
            LOGGER.info("Dashboard URL: http://localhost:{}/", port);
            
            // Start real-time data collection
            startRealtimeDataCollection();
            
        } catch (Exception e) {
            LOGGER.error("Failed to start web dashboard", e);
        }
    }
    
    /**
     * Stop the web dashboard server
     */
    public void stop() {
        if (!isRunning) {
            return;
        }
        
        if (server != null) {
            server.stop(5);
            server = null;
        }
        
        isRunning = false;
        LOGGER.info("Web Dashboard stopped");
    }
    
    /**
     * Register all HTTP endpoints
     */
    private void registerEndpoints() {
        // Static files
        server.createContext("/", new StaticFileHandler());
        
        // Authentication
        server.createContext("/api/auth/login", new LoginHandler());
        server.createContext("/api/auth/logout", new LogoutHandler());
        server.createContext("/api/auth/validate", new ValidateSessionHandler());
        
        // Dashboard data
        server.createContext("/api/dashboard/overview", new DashboardOverviewHandler());
        server.createContext("/api/dashboard/realtime", new RealtimeDataHandler());
        
        // Analytics
        server.createContext("/api/analytics/commands", new CommandAnalyticsHandler());
        server.createContext("/api/analytics/players", new PlayerAnalyticsHandler());
        server.createContext("/api/analytics/features", new FeatureAnalyticsHandler());
        server.createContext("/api/analytics/reports", new ReportsHandler());
        
        // Performance monitoring
        server.createContext("/api/performance/metrics", new PerformanceMetricsHandler());
        server.createContext("/api/performance/history", new PerformanceHistoryHandler());
        
        // Task management
        server.createContext("/api/tasks/list", new TaskListHandler());
        server.createContext("/api/tasks/create", new TaskCreateHandler());
        server.createContext("/api/tasks/cancel", new TaskCancelHandler());
        server.createContext("/api/tasks/templates", new TaskTemplatesHandler());
        
        // Plugin compatibility
        server.createContext("/api/plugins/status", new PluginStatusHandler());
        server.createContext("/api/plugins/compatibility", new PluginCompatibilityHandler());
        
        // System status monitoring
        server.createContext("/api/status/overview", new SystemStatusOverviewHandler());
        server.createContext("/api/status/resources", new SystemResourceStatusHandler());
        server.createContext("/api/status/components", new SystemComponentStatusHandler());
        server.createContext("/api/status/health", new SystemHealthStatusHandler());
        server.createContext("/api/status/history", new SystemStatusHistoryHandler());
        
        // Alert and notification system
        server.createContext("/api/alerts/status", new AlertStatusHandler());
        server.createContext("/api/alerts/config", new AlertConfigHandler());
        
        // Server management
        server.createContext("/api/server/info", new ServerInfoHandler());
        server.createContext("/api/server/command", new ServerCommandHandler());
        server.createContext("/api/server/config", new ConfigurationHandler());
        
        LOGGER.info("Registered {} API endpoints", 25);
    }
    
    /**
     * Initialize default dashboard users
     */
    private void initializeDefaultUsers() {
        // Default admin user
        authorizedUsers.put("admin", new DashboardUser(
            "admin", 
            "admin123", // This should be hashed in production
            DashboardRole.ADMINISTRATOR,
            "Default Administrator",
            System.currentTimeMillis()
        ));
        
        // Default viewer user
        authorizedUsers.put("viewer", new DashboardUser(
            "viewer",
            "viewer123",
            DashboardRole.VIEWER,
            "Read-only Viewer",
            System.currentTimeMillis()
        ));
        
        LOGGER.info("Initialized {} dashboard users", authorizedUsers.size());
    }
    
    /**
     * Start real-time data collection
     */
    private void startRealtimeDataCollection() {
        // Update real-time data every 5 seconds
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            try {
                updateRealtimeData();
            } catch (Exception e) {
                LOGGER.error("Error updating real-time data", e);
            }
        }, 0, 5, java.util.concurrent.TimeUnit.SECONDS);
    }
    
    /**
     * Update real-time dashboard data
     */
    private void updateRealtimeData() {
        Map<String, Object> data = new HashMap<>();
        
        // System metrics
        Runtime runtime = Runtime.getRuntime();
        data.put("memory_used", runtime.totalMemory() - runtime.freeMemory());
        data.put("memory_total", runtime.totalMemory());
        data.put("memory_max", runtime.maxMemory());
        data.put("threads", Thread.activeCount());
        data.put("uptime", java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime());
        
        // Performance metrics
        data.put("performance", performance.getSystemMetrics());
        
        // Analytics summary
        data.put("total_commands", analytics.generateReport(DataAnalyticsSystem.ReportType.HOURLY).getTotalCommands());
        data.put("active_players", analytics.generateReport(DataAnalyticsSystem.ReportType.HOURLY).getActivePlayers().size());
        
        // Task statistics
        data.put("active_tasks", scheduler.getActiveTasks().size());
        
        // Plugin status
        data.put("active_plugins", compatibility.getActiveHooks().size());
        
        data.put("timestamp", System.currentTimeMillis());
        
        realtimeData.putAll(data);
    }
    
    /**
     * Validate user session
     */
    private DashboardSession validateSession(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return null;
        }
        
        DashboardSession session = activeSessions.get(sessionId);
        if (session != null && !session.isExpired()) {
            session.updateLastActivity();
            return session;
        }
        
        // Remove expired session
        if (session != null) {
            activeSessions.remove(sessionId);
        }
        
        return null;
    }
    
    /**
     * Send JSON response
     */
    private void sendJsonResponse(HttpExchange exchange, Object data, int statusCode) throws IOException {
        String json = gson.toJson(data);
        byte[] response = json.getBytes(StandardCharsets.UTF_8);
        
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, response.length);
        
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }
    
    /**
     * Send error response
     */
    private void sendErrorResponse(HttpExchange exchange, String message, int statusCode) throws IOException {
        Map<String, Object> error = Map.of(
            "error", true,
            "message", message,
            "timestamp", System.currentTimeMillis()
        );
        sendJsonResponse(exchange, error, statusCode);
    }
    
    // HTTP Handlers
    
    private class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) {
                path = "/index.html";
            }
            
            // Serve static dashboard files
            InputStream resource = getClass().getResourceAsStream("/dashboard" + path);
            if (resource != null) {
                byte[] content = resource.readAllBytes();
                String contentType = getContentType(path);
                
                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.sendResponseHeaders(200, content.length);
                
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(content);
                }
            } else {
                exchange.sendResponseHeaders(404, 0);
            }
        }
        
        private String getContentType(String path) {
            if (path.endsWith(".html")) return "text/html";
            if (path.endsWith(".css")) return "text/css";
            if (path.endsWith(".js")) return "application/javascript";
            if (path.endsWith(".json")) return "application/json";
            return "text/plain";
        }
    }
    
    private class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendErrorResponse(exchange, "Method not allowed", 405);
                return;
            }
            
            // Parse login request
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            // Simple parsing - in production use proper JSON parsing
            
            // Validate credentials
            String username = "admin"; // Extract from request
            String password = "admin123"; // Extract from request
            
            DashboardUser user = authorizedUsers.get(username);
            if (user != null && user.getPassword().equals(password)) {
                // Create session
                String sessionId = UUID.randomUUID().toString();
                DashboardSession session = new DashboardSession(sessionId, user);
                activeSessions.put(sessionId, session);
                
                Map<String, Object> response = Map.of(
                    "success", true,
                    "sessionId", sessionId,
                    "user", Map.of(
                        "username", user.getUsername(),
                        "role", user.getRole().toString(),
                        "displayName", user.getDisplayName()
                    )
                );
                
                sendJsonResponse(exchange, response, 200);
            } else {
                sendErrorResponse(exchange, "Invalid credentials", 401);
            }
        }
    }
    
    private class LogoutHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String sessionId = exchange.getRequestHeaders().getFirst("X-Session-ID");
            if (sessionId != null) {
                activeSessions.remove(sessionId);
            }
            
            sendJsonResponse(exchange, Map.of("success", true), 200);
        }
    }
    
    private class ValidateSessionHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String sessionId = exchange.getRequestHeaders().getFirst("X-Session-ID");
            DashboardSession session = validateSession(sessionId);
            
            if (session != null) {
                Map<String, Object> response = Map.of(
                    "valid", true,
                    "user", Map.of(
                        "username", session.getUser().getUsername(),
                        "role", session.getUser().getRole().toString(),
                        "displayName", session.getUser().getDisplayName()
                    )
                );
                sendJsonResponse(exchange, response, 200);
            } else {
                sendErrorResponse(exchange, "Invalid session", 401);
            }
        }
    }
    
    private class DashboardOverviewHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            DashboardSession session = validateSession(exchange.getRequestHeaders().getFirst("X-Session-ID"));
            if (session == null) {
                sendErrorResponse(exchange, "Unauthorized", 401);
                return;
            }
            
            Map<String, Object> overview = new HashMap<>();
            
            // System overview
            overview.put("system", realtimeData);
            
            // Quick stats
            DataAnalyticsSystem.AnalyticsReport report = analytics.generateReport(DataAnalyticsSystem.ReportType.DAILY);
            overview.put("commands_today", report.getTotalCommands());
            overview.put("players_today", report.getTotalPlayers());
            overview.put("errors_today", report.getTotalErrors());
            
            // Top commands
            overview.put("top_commands", report.getTopCommands().stream().limit(5).toList());
            
            // Active tasks
            overview.put("active_tasks", scheduler.getActiveTasks().size());
            
            // Plugin status
            overview.put("plugin_integrations", compatibility.getActiveHooks().size());
            
            sendJsonResponse(exchange, overview, 200);
        }
    }
    
    private class RealtimeDataHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            DashboardSession session = validateSession(exchange.getRequestHeaders().getFirst("X-Session-ID"));
            if (session == null) {
                sendErrorResponse(exchange, "Unauthorized", 401);
                return;
            }
            
            sendJsonResponse(exchange, realtimeData, 200);
        }
    }
    
    private class CommandAnalyticsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            DashboardSession session = validateSession(exchange.getRequestHeaders().getFirst("X-Session-ID"));
            if (session == null) {
                sendErrorResponse(exchange, "Unauthorized", 401);
                return;
            }
            
            List<DataAnalyticsSystem.CommandAnalytics> topCommands = analytics.getTopCommands(20);
            sendJsonResponse(exchange, topCommands, 200);
        }
    }
    
    private class PlayerAnalyticsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            DashboardSession session = validateSession(exchange.getRequestHeaders().getFirst("X-Session-ID"));
            if (session == null) {
                sendErrorResponse(exchange, "Unauthorized", 401);
                return;
            }
            
            List<DataAnalyticsSystem.PlayerAnalytics> topPlayers = analytics.getTopPlayers(20);
            sendJsonResponse(exchange, topPlayers, 200);
        }
    }
    
    private class FeatureAnalyticsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            DashboardSession session = validateSession(exchange.getRequestHeaders().getFirst("X-Session-ID"));
            if (session == null) {
                sendErrorResponse(exchange, "Unauthorized", 401);
                return;
            }
            
            Map<String, DataAnalyticsSystem.FeatureUsage> featureUsage = analytics.getFeatureUsageStats();
            sendJsonResponse(exchange, featureUsage, 200);
        }
    }
    
    private class ReportsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            DashboardSession session = validateSession(exchange.getRequestHeaders().getFirst("X-Session-ID"));
            if (session == null) {
                sendErrorResponse(exchange, "Unauthorized", 401);
                return;
            }
            
            String reportType = exchange.getRequestURI().getQuery();
            DataAnalyticsSystem.ReportType type = DataAnalyticsSystem.ReportType.DAILY;
            
            if (reportType != null && reportType.contains("type=")) {
                String typeParam = reportType.split("type=")[1].split("&")[0];
                try {
                    type = DataAnalyticsSystem.ReportType.valueOf(typeParam.toUpperCase());
                } catch (IllegalArgumentException e) {
                    // Use default
                }
            }
            
            DataAnalyticsSystem.AnalyticsReport report = analytics.generateReport(type);
            sendJsonResponse(exchange, report, 200);
        }
    }
    
    private class PerformanceMetricsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            DashboardSession session = validateSession(exchange.getRequestHeaders().getFirst("X-Session-ID"));
            if (session == null) {
                sendErrorResponse(exchange, "Unauthorized", 401);
                return;
            }
            
            sendJsonResponse(exchange, performance.getSystemMetrics(), 200);
        }
    }
    
    private class PerformanceHistoryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            DashboardSession session = validateSession(exchange.getRequestHeaders().getFirst("X-Session-ID"));
            if (session == null) {
                sendErrorResponse(exchange, "Unauthorized", 401);
                return;
            }
            
            sendJsonResponse(exchange, performance.getAllCommandMetrics(), 200);
        }
    }
    
    private class TaskListHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            DashboardSession session = validateSession(exchange.getRequestHeaders().getFirst("X-Session-ID"));
            if (session == null) {
                sendErrorResponse(exchange, "Unauthorized", 401);
                return;
            }
            
            List<CommandScheduler.ScheduledTask> tasks = scheduler.getActiveTasks();
            sendJsonResponse(exchange, tasks, 200);
        }
    }
    
    private class TaskCreateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            DashboardSession session = validateSession(exchange.getRequestHeaders().getFirst("X-Session-ID"));
            if (session == null || !session.getUser().getRole().canManageTasks()) {
                sendErrorResponse(exchange, "Unauthorized", 401);
                return;
            }
            
            // Implementation would parse task creation request
            sendJsonResponse(exchange, Map.of("success", true, "message", "Task creation not implemented"), 200);
        }
    }
    
    private class TaskCancelHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            DashboardSession session = validateSession(exchange.getRequestHeaders().getFirst("X-Session-ID"));
            if (session == null || !session.getUser().getRole().canManageTasks()) {
                sendErrorResponse(exchange, "Unauthorized", 401);
                return;
            }
            
            // Implementation would parse task ID and cancel
            sendJsonResponse(exchange, Map.of("success", true, "message", "Task cancellation not implemented"), 200);
        }
    }
    
    private class TaskTemplatesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            DashboardSession session = validateSession(exchange.getRequestHeaders().getFirst("X-Session-ID"));
            if (session == null) {
                sendErrorResponse(exchange, "Unauthorized", 401);
                return;
            }
            
            List<CommandScheduler.TaskTemplate> templates = scheduler.getTaskTemplates();
            sendJsonResponse(exchange, templates, 200);
        }
    }
    
    private class PluginStatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            DashboardSession session = validateSession(exchange.getRequestHeaders().getFirst("X-Session-ID"));
            if (session == null) {
                sendErrorResponse(exchange, "Unauthorized", 401);
                return;
            }
            
            Map<String, PluginCompatibilityManager.PluginInfo> detectedPlugins = compatibility.getDetectedPlugins();
            sendJsonResponse(exchange, detectedPlugins, 200);
        }
    }
    
    private class PluginCompatibilityHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            DashboardSession session = validateSession(exchange.getRequestHeaders().getFirst("X-Session-ID"));
            if (session == null) {
                sendErrorResponse(exchange, "Unauthorized", 401);
                return;
            }
            
            PluginCompatibilityManager.CompatibilityReport report = compatibility.generateCompatibilityReport();
            sendJsonResponse(exchange, report, 200);
        }
    }
    
    private class ServerInfoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            DashboardSession session = validateSession(exchange.getRequestHeaders().getFirst("X-Session-ID"));
            if (session == null) {
                sendErrorResponse(exchange, "Unauthorized", 401);
                return;
            }
            
            Map<String, Object> serverInfo = new HashMap<>();
            serverInfo.put("version", "NeoEssentials 2.1.0");
            serverInfo.put("uptime", java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime());
            serverInfo.put("java_version", System.getProperty("java.version"));
            
            sendJsonResponse(exchange, serverInfo, 200);
        }
    }
    
    private class ServerCommandHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            DashboardSession session = validateSession(exchange.getRequestHeaders().getFirst("X-Session-ID"));
            if (session == null || !session.getUser().getRole().canExecuteCommands()) {
                sendErrorResponse(exchange, "Unauthorized", 401);
                return;
            }
            
            // Implementation would execute server commands
            sendJsonResponse(exchange, Map.of("success", true, "message", "Command execution not implemented"), 200);
        }
    }
    
    private class ConfigurationHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            DashboardSession session = validateSession(exchange.getRequestHeaders().getFirst("X-Session-ID"));
            if (session == null || !session.getUser().getRole().canManageConfig()) {
                sendErrorResponse(exchange, "Unauthorized", 401);
                return;
            }
            
            // Implementation would handle configuration management
            sendJsonResponse(exchange, Map.of("success", true, "message", "Configuration management not implemented"), 200);
        }
    }
    
    // System Status Monitoring Handlers
    private class SystemStatusOverviewHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            DashboardSession session = validateSession(exchange.getRequestHeaders().getFirst("X-Session-ID"));
            if (session == null) {
                sendErrorResponse(exchange, "Unauthorized", 401);
                return;
            }
            
            try {
                var systemStatus = statusMonitor.getSystemStatus();
                Map<String, Object> overview = Map.of(
                    "active", systemStatus.isSystemActive(),
                    "healthScore", systemStatus.getHealthScore(),
                    "health", systemStatus.getHealth(),
                    "uptime", systemStatus.getUptime(),
                    "lastUpdate", systemStatus.getLastUpdate().toString(),
                    "resourceStatus", systemStatus.getResourceStatus(),
                    "componentCount", systemStatus.getComponentStatuses().size(),
                    "activeComponents", systemStatus.getComponentStatuses().values().stream()
                        .mapToLong(comp -> comp.getState() == SystemStatusMonitor.ComponentState.ACTIVE ? 1 : 0)
                        .sum()
                );
                sendJsonResponse(exchange, overview, 200);
            } catch (Exception e) {
                LOGGER.error("Error in system status overview", e);
                sendErrorResponse(exchange, "Internal server error", 500);
            }
        }
    }
    
    private class SystemResourceStatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            DashboardSession session = validateSession(exchange.getRequestHeaders().getFirst("X-Session-ID"));
            if (session == null) {
                sendErrorResponse(exchange, "Unauthorized", 401);
                return;
            }
            
            try {
                var resourceStatus = statusMonitor.getSystemResourceStatus();
                sendJsonResponse(exchange, resourceStatus, 200);
            } catch (Exception e) {
                LOGGER.error("Error in resource status", e);
                sendErrorResponse(exchange, "Internal server error", 500);
            }
        }
    }
    
    private class SystemComponentStatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            DashboardSession session = validateSession(exchange.getRequestHeaders().getFirst("X-Session-ID"));
            if (session == null) {
                sendErrorResponse(exchange, "Unauthorized", 401);
                return;
            }
            
            try {
                var componentStatus = statusMonitor.getEnterpriseComponentStatus();
                sendJsonResponse(exchange, componentStatus, 200);
            } catch (Exception e) {
                LOGGER.error("Error in component status", e);
                sendErrorResponse(exchange, "Internal server error", 500);
            }
        }
    }
    
    private class SystemHealthStatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            DashboardSession session = validateSession(exchange.getRequestHeaders().getFirst("X-Session-ID"));
            if (session == null) {
                sendErrorResponse(exchange, "Unauthorized", 401);
                return;
            }
            
            try {
                var systemStatus = statusMonitor.getSystemStatus();
                Map<String, Object> healthData = Map.of(
                    "healthScore", systemStatus.getHealthScore(),
                    "health", systemStatus.getHealth(),
                    "recommendations", generateHealthRecommendations(systemStatus.getHealthScore()),
                    "factors", Map.of(
                        "memory", calculateMemoryHealthScore(systemStatus.getResourceStatus().getMemoryUsagePercent()),
                        "heap", calculateMemoryHealthScore(systemStatus.getResourceStatus().getHeapUsagePercent()),
                        "components", (double) systemStatus.getComponentStatuses().values().stream()
                            .mapToLong(comp -> comp.getState() == SystemStatusMonitor.ComponentState.ACTIVE ? 1 : 0)
                            .sum() / systemStatus.getComponentStatuses().size() * 100.0
                    )
                );
                sendJsonResponse(exchange, healthData, 200);
            } catch (Exception e) {
                LOGGER.error("Error in health status", e);
                sendErrorResponse(exchange, "Internal server error", 500);
            }
        }
        
        private double calculateMemoryHealthScore(double usagePercent) {
            if (usagePercent < 50) return 100.0;
            if (usagePercent < 70) return 90.0;
            if (usagePercent < 85) return 70.0;
            if (usagePercent < 95) return 40.0;
            return 10.0;
        }
        
        private String generateHealthRecommendations(double healthScore) {
            if (healthScore >= 95) return "System is operating at optimal levels";
            if (healthScore >= 80) return "System performance is good but could be optimized";
            if (healthScore >= 65) return "System performance needs attention";
            return "Critical system issues detected - immediate investigation recommended";
        }
    }
    
    private class SystemStatusHistoryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            DashboardSession session = validateSession(exchange.getRequestHeaders().getFirst("X-Session-ID"));
            if (session == null) {
                sendErrorResponse(exchange, "Unauthorized", 401);
                return;
            }
            
            try {
                var history = statusMonitor.getStatusHistory();
                Map<String, Object> historyData = Map.of(
                    "snapshots", history.size(),
                    "history", history.stream().limit(50).toArray(), // Limit to last 50 entries
                    "oldestSnapshot", history.isEmpty() ? null : history.get(0).getTimestamp().toString(),
                    "newestSnapshot", history.isEmpty() ? null : history.get(history.size() - 1).getTimestamp().toString()
                );
                sendJsonResponse(exchange, historyData, 200);
            } catch (Exception e) {
                LOGGER.error("Error in status history", e);
                sendErrorResponse(exchange, "Internal server error", 500);
            }
        }
    }
    
    // Data classes
    
    public static class DashboardUser {
        private final String username;
        private final String password;
        private final DashboardRole role;
        private final String displayName;
        private final long createdAt;
        
        public DashboardUser(String username, String password, DashboardRole role, String displayName, long createdAt) {
            this.username = username;
            this.password = password;
            this.role = role;
            this.displayName = displayName;
            this.createdAt = createdAt;
        }
        
        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public DashboardRole getRole() { return role; }
        public String getDisplayName() { return displayName; }
        public long getCreatedAt() { return createdAt; }
    }
    
    public static class DashboardSession {
        private final String sessionId;
        private final DashboardUser user;
        private final long createdAt;
        private long lastActivity;
        private static final long SESSION_TIMEOUT = 3600000; // 1 hour
        
        public DashboardSession(String sessionId, DashboardUser user) {
            this.sessionId = sessionId;
            this.user = user;
            this.createdAt = System.currentTimeMillis();
            this.lastActivity = createdAt;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() - lastActivity > SESSION_TIMEOUT;
        }
        
        public void updateLastActivity() {
            this.lastActivity = System.currentTimeMillis();
        }
        
        public String getSessionId() { return sessionId; }
        public DashboardUser getUser() { return user; }
        public long getCreatedAt() { return createdAt; }
        public long getLastActivity() { return lastActivity; }
    }
    
    public enum DashboardRole {
        ADMINISTRATOR(true, true, true, true),
        MODERATOR(false, true, true, false),
        VIEWER(false, false, false, false);
        
        private final boolean canManageConfig;
        private final boolean canManageTasks;
        private final boolean canExecuteCommands;
        private final boolean canManageUsers;
        
        DashboardRole(boolean canManageConfig, boolean canManageTasks, boolean canExecuteCommands, boolean canManageUsers) {
            this.canManageConfig = canManageConfig;
            this.canManageTasks = canManageTasks;
            this.canExecuteCommands = canExecuteCommands;
            this.canManageUsers = canManageUsers;
        }
        
        public boolean canManageConfig() { return canManageConfig; }
        public boolean canManageTasks() { return canManageTasks; }
        public boolean canExecuteCommands() { return canExecuteCommands; }
        public boolean canManageUsers() { return canManageUsers; }
    }
    
    // Alert System Handlers
    private class AlertStatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            DashboardSession session = validateSession(exchange.getRequestHeaders().getFirst("X-Session-ID"));
            if (session == null) {
                sendErrorResponse(exchange, "Unauthorized", 401);
                return;
            }
            
            try {
                var alertStats = alertSystem.getAlertStatistics();
                sendJsonResponse(exchange, alertStats, 200);
            } catch (Exception e) {
                LOGGER.error("Error in alert status", e);
                sendErrorResponse(exchange, "Internal server error", 500);
            }
        }
    }
    
    private class AlertConfigHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            DashboardSession session = validateSession(exchange.getRequestHeaders().getFirst("X-Session-ID"));
            if (session == null) {
                sendErrorResponse(exchange, "Unauthorized", 401);
                return;
            }
            
            try {
                Map<String, Object> config = Map.of(
                    "isRunning", alertSystem.isRunning(),
                    "healthThreshold", alertSystem.getHealthThreshold(),
                    "criticalThreshold", alertSystem.getCriticalThreshold(),
                    "monitoringInterval", alertSystem.getMonitoringInterval(),
                    "statistics", alertSystem.getAlertStatistics()
                );
                sendJsonResponse(exchange, config, 200);
            } catch (Exception e) {
                LOGGER.error("Error in alert config", e);
                sendErrorResponse(exchange, "Internal server error", 500);
            }
        }
    }
}
