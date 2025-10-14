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
            
            // Save state to persist across restarts
            saveDashboardState(true);
            
            LOGGER.info(MessageUtil.localize("neoessentials.dashboard.server.started_header"));
            LOGGER.info(MessageUtil.localize("neoessentials.dashboard.server.started_title"));
            LOGGER.info(MessageUtil.localize("neoessentials.dashboard.server.started_access", port));
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
        // Player stats endpoint
        server.createContext("/api/players", new com.zerog.neoessentials.webdashboard.handlers.PlayersHandler());
        
        // Server stats endpoint
        server.createContext("/api/server", new com.zerog.neoessentials.webdashboard.handlers.ServerStatsHandler());
        
        // Logs endpoint
        server.createContext("/api/logs", new com.zerog.neoessentials.webdashboard.handlers.LogsHandler());
        
        // Config endpoints
        server.createContext("/api/config", new com.zerog.neoessentials.webdashboard.handlers.ConfigHandler());
        
        LOGGER.info(MessageUtil.localize("neoessentials.dashboard.server.endpoints_registered"));
    }
    
    public boolean isRunning() {
        return running;
    }
    
    public int getPort() {
        return port;
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
            
            // Default to index.html
            if (path.equals("/") || path.isEmpty()) {
                path = "/index.html";
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
