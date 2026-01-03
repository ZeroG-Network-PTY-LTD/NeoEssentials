package com.zerog.neoessentials.webdashboard;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.webdashboard.api.endpoints.AdminEndpoint;
import com.zerog.neoessentials.webdashboard.api.endpoints.GameEndpoint;
import com.zerog.neoessentials.webdashboard.api.endpoints.LoggingEndpoint;
import com.zerog.neoessentials.webdashboard.api.endpoints.PlayerEndpoint;
import com.zerog.neoessentials.webdashboard.api.endpoints.ServerEndpoint;
import com.zerog.neoessentials.webdashboard.handlers.AuthHandler;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

/**
 * Main entry point for the NeoEssentials Dashboard API
 * API-First architecture that provides:
 * - RESTful API endpoints for data access
 * - Authentication & Authorization system
 * - Real-time data collection and processing
 * - WebSocket support for live updates
 * Design Philosophy:
 * - Separation of concerns: API layer separate from UI
 * - Security-first: All endpoints require authentication
 * - Performance: Efficient data collection and caching
 * - Extensibility: Easy to add new endpoints and features
 */
@SuppressWarnings("ConstantConditions") // Intentional null checks for safety
public class DashboardAPI {
    private static final Logger LOGGER = LoggerFactory.getLogger(DashboardAPI.class);
    private static DashboardAPI INSTANCE;
    
    private HttpServer apiServer;
    private boolean running = false;
    private final int port;
    private final String bindAddress;
    private MinecraftServer server;
    
    private DashboardAPI(int port, String bindAddress) {
        this.port = port;
        this.bindAddress = bindAddress;
    }
    
    /**
     * Get singleton instance of the Dashboard API
     */
    public static DashboardAPI getInstance() {
        if (INSTANCE == null) {
            ConfigManager config = ConfigManager.getInstance();
            int port = config.getWebDashboardPort();
            String bindAddress = config.getWebDashboardBindAddress();
            INSTANCE = new DashboardAPI(port, bindAddress);
        }
        return INSTANCE;
    }
    
    /**
     * Set the MinecraftServer instance
     * Called by DashboardLifecycleManager on server start
     */
    public void setServer(MinecraftServer server) {
        this.server = server;
    }
    
    /**
     * Start the Dashboard API server
     */
    public void start() {
        if (running) {
            LOGGER.warn("Dashboard API is already running");
            return;
        }
        
        if (server == null) {
            LOGGER.error("Cannot start Dashboard API: MinecraftServer not set");
            return;
        }
        
        try {
            // Create HTTP server
            InetSocketAddress address = new InetSocketAddress(bindAddress, port);
            apiServer = HttpServer.create(address, 0);
            
            // Set up thread pool
            apiServer.setExecutor(Executors.newFixedThreadPool(10));
            
            // Register API endpoints
            registerEndpoints();
            
            // Start server
            apiServer.start();
            running = true;
            
            // Get the friendly URL from config
            ConfigManager config = ConfigManager.getInstance();
            String dashboardUrl = config.getWebDashboardUrl();

            LOGGER.info("Dashboard API started successfully on {}:{}", bindAddress, port);
            LOGGER.info("Access the dashboard at: {}", dashboardUrl);
            LOGGER.info("API Endpoints available at: {}/api/", dashboardUrl);

        } catch (IOException e) {
            LOGGER.error("Failed to start Dashboard API server", e);
            running = false;
        }
    }
    
    /**
     * Stop the Dashboard API server
     */
    public void stop() {
        if (!running || apiServer == null) {
            return;
        }
        
        try {
            apiServer.stop(2);
            running = false;
            LOGGER.info("Dashboard API stopped successfully");
        } catch (Exception e) {
            LOGGER.error("Error stopping Dashboard API", e);
        }
    }
    
    /**
     * Authentication middleware wrapper
     * Validates token before allowing access to protected endpoints
     */
    @SuppressWarnings("ConstantConditions") // Null check is intentional for safety
    private HttpHandler withAuth(HttpHandler handler) {
        return exchange -> {
            try {
                // Get token from Authorization header
                String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
                String token = null;
                
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    token = authHeader.substring(7);
                }
                
                // Validate token (validateToken handles null internally)
                if (!AuthHandler.validateToken(token)) {
                    // Unauthorized
                    LOGGER.info("Unauthorized API request to {} - token: {}", exchange.getRequestURI(), token == null ? "null" : "invalid");
                    String response = "{\"success\":false,\"error\":\"Unauthorized - Please login first\"}";
                    byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                    exchange.sendResponseHeaders(401, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(bytes);
                    }
                    return;
                }
                
                LOGGER.info("Authenticated API request to {} by user {}", exchange.getRequestURI(), AuthHandler.getUsername(token));
                
                // Store auth info in exchange attributes for handler to use
                exchange.setAttribute("auth-username", AuthHandler.getUsername(token));
                exchange.setAttribute("auth-admin", AuthHandler.isAdmin(token));
                
                // Call the actual handler - this will handle its own exchange closing
                handler.handle(exchange);
            } catch (Exception e) {
                LOGGER.error("Error in authentication middleware for {}", exchange.getRequestURI(), e);
                try {
                    if (!exchange.getResponseHeaders().containsKey("Content-Type")) {
                        String errorResponse = "{\"success\":false,\"error\":\"Authentication error: " + e.getMessage() + "\"}";
                        byte[] bytes = errorResponse.getBytes(StandardCharsets.UTF_8);
                        exchange.getResponseHeaders().set("Content-Type", "application/json");
                        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                        exchange.sendResponseHeaders(500, bytes.length);
                        try (OutputStream os = exchange.getResponseBody()) {
                            os.write(bytes);
                        }
                    }
                } catch (Exception ex) {
                    LOGGER.error("Failed to send error response", ex);
                }
            }
        };
    }
    
    /**
     * Register all API endpoints
     * This is where we'll add authentication, data endpoints, etc.
     */
    private void registerEndpoints() {
        // Register authentication endpoints (no auth required)
        apiServer.createContext("/api/auth", new AuthHandler(server));
        
        // Register API endpoint handlers with authentication middleware
        apiServer.createContext("/api/player", withAuth(new PlayerEndpoint(server)));
        apiServer.createContext("/api/server", withAuth(new ServerEndpoint(server)));
        apiServer.createContext("/api/game", withAuth(new GameEndpoint(server)));
        apiServer.createContext("/api/logging", withAuth(new LoggingEndpoint()));
        apiServer.createContext("/api/admin", withAuth(new AdminEndpoint(server)));

        LOGGER.info("API endpoints registered:");
        LOGGER.info("  - /api/auth/* (login, logout, validate, discord)");
        LOGGER.info("  - /api/player/* (profile, stats, achievements, inventory, status, health, xp, location, homes, online) [AUTH REQUIRED]");
        LOGGER.info("  - /api/server/* (profile, performance, worlds, players, entities, memory, history) [AUTH REQUIRED]");
        LOGGER.info("  - /api/game/* (statistics, events, activity, blocks) [AUTH REQUIRED]");
        LOGGER.info("  - /api/logging/* (requests, errors, performance) [AUTH REQUIRED]");
        LOGGER.info("  - /api/admin/* (restart, stop, reload, save) [AUTH REQUIRED - ADMIN ONLY]");

        // Check if dashboard resources are available
        try (java.io.InputStream testStream = getClass().getResourceAsStream("/webdashboard/index.html")) {
            if (testStream != null) {
                LOGGER.info("Dashboard resources verified - index.html found");
            } else {
                LOGGER.error("Dashboard resources NOT found - /webdashboard/index.html is null!");
            }
        } catch (Exception e) {
            LOGGER.error("Error checking dashboard resources", e);
        }
        
        // Serve static frontend files (catch-all, must be registered last)
        apiServer.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            
            LOGGER.debug("Serving static file: {}", path);
            
            // Default to index.html
            if (path.equals("/") || path.equals("/index.html")) {
                path = "/index.html";
            }
            
            // Serve file from resources
            try (java.io.InputStream in = getClass().getResourceAsStream("/webdashboard" + path)) {
                if (in != null) {
                    byte[] bytes = in.readAllBytes();
                    
                    // Generate ETag based on content hash for proper cache validation
                    String etag = "\"" + Integer.toHexString(java.util.Arrays.hashCode(bytes)) + "\"";

                    // Check If-None-Match header (ETag validation)
                    String ifNoneMatch = exchange.getRequestHeaders().getFirst("If-None-Match");
                    if (etag.equals(ifNoneMatch)) {
                        // Content hasn't changed - return 304 Not Modified
                        exchange.sendResponseHeaders(304, -1);
                        LOGGER.debug("Served 304 Not Modified for: {} (ETag: {})", path, etag);
                        return;
                    }

                    // Set content type and CORS headers
                    String contentType = getContentType(path);
                    exchange.getResponseHeaders().set("Content-Type", contentType);
                    exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");

                    // Strong cache-busting headers - force revalidation
                    exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate, max-age=0");
                    exchange.getResponseHeaders().set("Pragma", "no-cache");
                    exchange.getResponseHeaders().set("Expires", "0");
                    
                    // Add ETag for proper cache validation
                    exchange.getResponseHeaders().set("ETag", etag);

                    // Add Last-Modified header (use build time as baseline)
                    exchange.getResponseHeaders().set("Last-Modified", "Fri, 03 Jan 2026 00:00:00 GMT");

                    // Add custom header indicating mod version for debugging
                    exchange.getResponseHeaders().set("X-NeoEssentials-Version", getBuildNumber());

                    exchange.sendResponseHeaders(200, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(bytes);
                    }
                    
                    LOGGER.debug("Successfully served: {} ({} bytes, ETag: {})", path, bytes.length, etag);
                } else {
                    // 404 Not Found
                    LOGGER.warn("File not found: /webdashboard{}", path);
                    String response = "404 Not Found: " + path;
                    byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "text/plain");
                    exchange.sendResponseHeaders(404, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(bytes);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Error serving file: {}", path, e);
                String response = "500 Internal Server Error: " + e.getMessage();
                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/plain");
                exchange.sendResponseHeaders(500, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } finally {
                exchange.close();
            }
        });
        
        LOGGER.info("Static file serving enabled for frontend");
    }
    
    /**
     * Get content type based on file extension
     */
    private String getContentType(String path) {
        if (path.endsWith(".html")) return "text/html";
        if (path.endsWith(".css")) return "text/css";
        if (path.endsWith(".js")) return "application/javascript";
        if (path.endsWith(".json")) return "application/json";
        if (path.endsWith(".png")) return "image/png";
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
        if (path.endsWith(".svg")) return "image/svg+xml";
        return "text/plain";
    }
    
    /**
     * Get build number for cache-busting headers
     */
    private String getBuildNumber() {
        try (java.io.InputStream in = getClass().getResourceAsStream("/build_number.txt")) {
            if (in != null) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
            }
        } catch (Exception e) {
            LOGGER.debug("Could not read build number: {}", e.getMessage());
        }
        return "unknown";
    }

    /**
     * Check if API server is running
     */
    public boolean isRunning() {
        return running;
    }
    
    /**
     * Get API server port
     */
    public int getPort() {
        return port;
    }
    
    /**
     * Get API server bind address
     */
    public String getBindAddress() {
        return bindAddress;
    }
}
