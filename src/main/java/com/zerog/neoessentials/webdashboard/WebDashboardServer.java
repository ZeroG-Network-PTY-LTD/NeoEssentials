package com.zerog.neoessentials.webdashboard;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.zerog.neoessentials.util.MessageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.Executors;

/**
 * Embedded HTTP server for the NeoEssentials Web Dashboard
 * Serves static files and provides RESTful API endpoints
 */
public class WebDashboardServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebDashboardServer.class);
    private static WebDashboardServer INSTANCE;
    private static final Path STATE_FILE = Paths.get("neoessentials", "dashboard_state.txt");
    
    private HttpServer server;
    private com.zerog.neoessentials.webdashboard.websocket.DashboardWebSocketServer webSocketServer;
    private com.zerog.neoessentials.webdashboard.websocket.DataStreamManager dataStreamManager;
    private final int port;
    private final String bindAddress;
    private final Path webRoot;
    private boolean running = false;
    
    private WebDashboardServer(int port, String bindAddress) {
        this.port = port;
        this.bindAddress = bindAddress;
        // Extract to server root directory: rootserver/neoessentials/webdashboard
        this.webRoot = Paths.get("neoessentials", "webdashboard");
    }
    
    public static WebDashboardServer getInstance() {
        if (INSTANCE == null) {
            // Load settings from config
            com.zerog.neoessentials.config.ConfigManager configManager = 
                com.zerog.neoessentials.config.ConfigManager.getInstance();
            int port = configManager.getWebDashboardPort();
            String bindAddress = configManager.getWebDashboardBindAddress();
            INSTANCE = new WebDashboardServer(port, bindAddress);
            
            // Auto-start if dashboard was running before shutdown
            if (shouldAutoStart()) {
                LOGGER.info("Dashboard was active before shutdown, automatically restarting...");
                INSTANCE.start();
            }
        }
        return INSTANCE;
    }
    
    public static WebDashboardServer getInstance(int port, String bindAddress) {
        if (INSTANCE == null) {
            INSTANCE = new WebDashboardServer(port, bindAddress);
        }
        return INSTANCE;
    }
    
    /**
     * Start the web dashboard server
     */
    public void start() {
        if (running) {
            LOGGER.warn(MessageUtil.localize("neoessentials.dashboard.server.already_running"));
            return;
        }
        
        try {
            // Ensure web root directory exists and extract dashboard files from JAR
            if (!Files.exists(webRoot)) {
                Files.createDirectories(webRoot);
                LOGGER.info(MessageUtil.localize("neoessentials.dashboard.server.directory_created", webRoot.toAbsolutePath()));
            }
            
            // Extract dashboard files from resources if they don't exist
            extractDashboardFiles();
            
            // Create HTTP server with configured bind address
            com.zerog.neoessentials.config.ConfigManager configManager = 
                com.zerog.neoessentials.config.ConfigManager.getInstance();
            int maxThreads = configManager.getWebDashboardMaxThreads();
            
            server = HttpServer.create(new InetSocketAddress(bindAddress, port), 0);
            server.setExecutor(Executors.newFixedThreadPool(maxThreads));
            
            // Register API endpoints
            registerApiEndpoints();
            
            // Register static file handler (must be last)
            server.createContext("/", new StaticFileHandler(webRoot));
            
            // Start server
            server.start();
            running = true;
            
            // Start WebSocket server for real-time updates
            int wsPort = configManager.getWebDashboardWebSocketPort();
            try {
                webSocketServer = com.zerog.neoessentials.webdashboard.websocket.DashboardWebSocketServer.getInstance(wsPort);
                webSocketServer.start();
                LOGGER.info("WebSocket server started on port {}", wsPort);
                
                // Start data streaming
                dataStreamManager = com.zerog.neoessentials.webdashboard.websocket.DataStreamManager.getInstance(webSocketServer);
                dataStreamManager.startStreaming();
                LOGGER.info("WebSocket data streaming started");
            } catch (Exception e) {
                LOGGER.error("Failed to start WebSocket server on port {}", wsPort, e);
                LOGGER.error("Make sure the WebSocket port {} is available and not blocked by firewall", wsPort);
            }
            
            // Save state to persist across restarts
            saveDashboardState(true);
            
            LOGGER.info(MessageUtil.localize("neoessentials.dashboard.server.started_header"));
            LOGGER.info(MessageUtil.localize("neoessentials.dashboard.server.started_title"));
            LOGGER.info(MessageUtil.localize("neoessentials.dashboard.server.started_access", port));
            LOGGER.info("WebSocket: ws://localhost:{} (Configure 'websocketPort' in config.json if using custom ports)", wsPort);
            LOGGER.info(MessageUtil.localize("neoessentials.dashboard.server.started_footer"));
            
        } catch (IOException e) {
            LOGGER.error(MessageUtil.localize("neoessentials.dashboard.server.start_failed"), e);
        }
    }
    
    /**
     * Extract dashboard files from JAR resources to the file system
     * Extracts to server root: rootserver/neoessentials/webdashboard/
     */
    private void extractDashboardFiles() {
        // All dashboard files from src/main/resources/data/webdashboard/
        String[] dashboardFiles = {
            "index.html",
            "login.html",
            "orbitron.css",
            "space-dashboard.js",
            "space-glass.css",
            "space-theme.css"
        };
        
        for (String fileName : dashboardFiles) {
            try {
                Path targetFile = webRoot.resolve(fileName);
                
                // Only extract if file doesn't exist (don't overwrite customizations)
                if (!Files.exists(targetFile)) {
                    // Load resource from JAR: /data/webdashboard/ in resources
                    var resourceStream = getClass().getResourceAsStream("/data/webdashboard/" + fileName);
                    
                    if (resourceStream != null) {
                        Files.copy(resourceStream, targetFile);
                        LOGGER.info("Extracted dashboard file: {} -> {}", fileName, targetFile.toAbsolutePath());
                        resourceStream.close();
                    } else {
                        LOGGER.warn("Dashboard resource not found in JAR: /data/webdashboard/{}", fileName);
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Failed to extract dashboard file: {}", fileName, e);
            }
        }
    }
    
    /**
     * Stop the web dashboard server
     */
    public void stop() {
        if (!running) {
            return;
        }
        
        // Stop WebSocket data streaming
        if (dataStreamManager != null) {
            try {
                dataStreamManager.stopStreaming();
                LOGGER.info("WebSocket data streaming stopped");
            } catch (Exception e) {
                LOGGER.error("Error stopping data streaming", e);
            }
        }
        
        // Stop WebSocket server
        if (webSocketServer != null) {
            try {
                webSocketServer.stop(1000);
                LOGGER.info("WebSocket server stopped");
            } catch (Exception e) {
                LOGGER.error("Error stopping WebSocket server", e);
            }
        }
        
        if (server != null) {
            server.stop(0);
            running = false;
            
            // Save state - dashboard manually stopped
            saveDashboardState(false);
            
            LOGGER.info(MessageUtil.localize("neoessentials.dashboard.server.stopped"));
        }
    }
    
    /**
     * Save dashboard running state to persist across server restarts
     */
    private void saveDashboardState(boolean isRunning) {
        try {
            Path stateDir = STATE_FILE.getParent();
            if (stateDir != null && !Files.exists(stateDir)) {
                Files.createDirectories(stateDir);
            }
            
            Files.writeString(STATE_FILE, 
                isRunning ? "running" : "stopped", 
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
            
            LOGGER.debug("Dashboard state saved: {}", isRunning ? "running" : "stopped");
        } catch (IOException e) {
            LOGGER.error("Failed to save dashboard state", e);
        }
    }
    
    /**
     * Check if dashboard should auto-start based on saved state
     */
    private static boolean shouldAutoStart() {
        try {
            if (Files.exists(STATE_FILE)) {
                String state = Files.readString(STATE_FILE, StandardCharsets.UTF_8).trim();
                return "running".equalsIgnoreCase(state);
            }
        } catch (IOException e) {
            LOGGER.debug("Could not read dashboard state file, assuming stopped", e);
        }
        return false;
    }
    
    /**
     * Register all API endpoints
     */
    private void registerApiEndpoints() {
        // Authentication endpoint (no filter - handles its own auth)
        server.createContext("/api/auth", new com.zerog.neoessentials.webdashboard.handlers.AuthenticationHandler());
        
        // Password change endpoint (no filter - validates session internally)
        server.createContext("/api/change-password", new com.zerog.neoessentials.webdashboard.handlers.PasswordChangeHandler());
        
        // Create authentication filter for protected endpoints
        com.zerog.neoessentials.webdashboard.filters.AuthenticationFilter authFilter = 
            new com.zerog.neoessentials.webdashboard.filters.AuthenticationFilter();
        
        // Player stats endpoint (protected)
        var playersContext = server.createContext("/api/players", 
            new com.zerog.neoessentials.webdashboard.handlers.PlayersHandler());
        playersContext.getFilters().add(authFilter);
        
        // Server stats endpoint (protected)
        var serverContext = server.createContext("/api/server", 
            new com.zerog.neoessentials.webdashboard.handlers.ServerStatsHandler());
        serverContext.getFilters().add(authFilter);
        
        // Logs endpoint (protected)
        var logsContext = server.createContext("/api/logs", 
            new com.zerog.neoessentials.webdashboard.handlers.LogsHandler());
        logsContext.getFilters().add(authFilter);
        
        // Config endpoints (protected)
        var configContext = server.createContext("/api/config", 
            new com.zerog.neoessentials.webdashboard.handlers.ConfigHandler());
        configContext.getFilters().add(authFilter);
        
        // File management endpoints (protected)
        var filesContext = server.createContext("/api/files", 
            new com.zerog.neoessentials.webdashboard.handlers.FileManagementHandler());
        filesContext.getFilters().add(authFilter);
        
        // Command execution endpoints (protected)
        var commandsContext = server.createContext("/api/commands", 
            new com.zerog.neoessentials.webdashboard.handlers.CommandExecutionHandler());
        commandsContext.getFilters().add(authFilter);
        
        // Preferences endpoints (protected)
        var preferencesContext = server.createContext("/api/preferences", 
            new com.zerog.neoessentials.webdashboard.handlers.PreferencesHandler());
        preferencesContext.getFilters().add(authFilter);
        
        // Analytics endpoints (protected)
        var analyticsContext = server.createContext("/api/analytics", 
            new com.zerog.neoessentials.webdashboard.analytics.AnalyticsHandler());
        analyticsContext.getFilters().add(authFilter);
        
        // Map viewer endpoints (protected)
        var mapContext = server.createContext("/api/map", 
            new com.zerog.neoessentials.webdashboard.map.MapHandler());
        mapContext.getFilters().add(authFilter);
        
        // Scheduled tasks endpoints (protected)
        var tasksContext = server.createContext("/api/tasks", 
            new com.zerog.neoessentials.scheduler.TaskHandler());
        tasksContext.getFilters().add(authFilter);
        
        // Resource pack endpoints (protected)
        var resourcePacksContext = server.createContext("/api/resourcepacks", 
            new com.zerog.neoessentials.resourcepacks.ResourcePackHandler());
        resourcePacksContext.getFilters().add(authFilter);
        
        // Moderation endpoints (protected)
        var moderationContext = server.createContext("/api/moderation", 
            new com.zerog.neoessentials.moderation.ModerationHandler());
        moderationContext.getFilters().add(authFilter);
        
        // Log viewer endpoints (protected)
        var logViewerDetailContext = server.createContext("/api/logs/", 
            new com.zerog.neoessentials.logs.LogHandler());
        logViewerDetailContext.getFilters().add(authFilter);
        
        // Database browser endpoints (protected)
        var databaseContext = server.createContext("/api/database", 
            new com.zerog.neoessentials.database.DatabaseHandler());
        databaseContext.getFilters().add(authFilter);
        
        // Internationalization endpoints (protected)
        var i18nContext = server.createContext("/api/i18n", 
            new com.zerog.neoessentials.i18n.TranslationHandler());
        i18nContext.getFilters().add(authFilter);
        
        // Documentation endpoints (all users - help system should be accessible)
        // No authentication filter - documentation should be accessible to all users
        server.createContext("/api/docs", 
            new com.zerog.neoessentials.docs.DocumentationHandler());
        
        // WebSocket info endpoint (no auth - needed for initial connection setup)
        server.createContext("/api/websocket/info", exchange -> {
            try {
                if ("GET".equals(exchange.getRequestMethod())) {
                    com.zerog.neoessentials.config.ConfigManager configMgr = 
                        com.zerog.neoessentials.config.ConfigManager.getInstance();
                    int wsPort = configMgr.getWebDashboardWebSocketPort();
                    String response = String.format("{\"port\":%d,\"protocol\":\"%s\"}", 
                        wsPort, 
                        "ws");
                    
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                    exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
                    
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(response.getBytes(StandardCharsets.UTF_8));
                    }
                } else {
                    exchange.sendResponseHeaders(405, -1);
                }
            } catch (Exception e) {
                LOGGER.error("Error handling websocket info request", e);
                exchange.sendResponseHeaders(500, -1);
            } finally {
                exchange.close();
            }
        });
        
        LOGGER.info(MessageUtil.localize("neoessentials.dashboard.server.endpoints_registered"));
        LOGGER.info("Registered endpoints: /api/auth, /api/players, /api/server, /api/logs (console), /api/logs/ (viewer), /api/config, /api/files, /api/commands, /api/preferences, /api/analytics, /api/map, /api/tasks, /api/resourcepacks, /api/moderation, /api/database, /api/i18n, /api/docs, /api/websocket/info");
        LOGGER.info("Authentication filter applied to all protected endpoints (documentation is public)");
    }
    
    public boolean isRunning() {
        return running;
    }
    
    public int getPort() {
        return port;
    }
    
    public com.zerog.neoessentials.webdashboard.websocket.DashboardWebSocketServer getWebSocketServer() {
        return webSocketServer;
    }
    
    public com.zerog.neoessentials.webdashboard.websocket.DataStreamManager getDataStreamManager() {
        return dataStreamManager;
    }
    
    /**
     * Static file handler for serving HTML, CSS, JS files
     */
    private static class StaticFileHandler implements HttpHandler {
        private final Path webRoot;
        
        public StaticFileHandler(Path webRoot) {
            this.webRoot = webRoot;
        }
        
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            
            // Default to login page for root
            if (path.equals("/") || path.isEmpty()) {
                path = "/login.html";
            }
            
            Path filePath = webRoot.resolve(path.substring(1)); // Remove leading slash
            
            // Security check - prevent directory traversal
            if (!filePath.normalize().startsWith(webRoot.normalize())) {
                sendResponse(exchange, 403, MessageUtil.localize("neoessentials.dashboard.server.forbidden"));
                return;
            }
            
            if (Files.exists(filePath) && !Files.isDirectory(filePath)) {
                String contentType = getContentType(filePath.toString());
                byte[] content = Files.readAllBytes(filePath);
                
                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.sendResponseHeaders(200, content.length);
                
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(content);
                }
            } else {
                sendResponse(exchange, 404, MessageUtil.localize("neoessentials.dashboard.server.file_not_found", path));
            }
        }
        
        private String getContentType(String filename) {
            if (filename.endsWith(".html")) return "text/html; charset=UTF-8";
            if (filename.endsWith(".css")) return "text/css; charset=UTF-8";
            if (filename.endsWith(".js")) return "application/javascript; charset=UTF-8";
            if (filename.endsWith(".json")) return "application/json; charset=UTF-8";
            if (filename.endsWith(".png")) return "image/png";
            if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) return "image/jpeg";
            if (filename.endsWith(".svg")) return "image/svg+xml";
            return "text/plain; charset=UTF-8";
        }
        
        private void sendResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
            byte[] response = message.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusCode, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        }
    }
}
