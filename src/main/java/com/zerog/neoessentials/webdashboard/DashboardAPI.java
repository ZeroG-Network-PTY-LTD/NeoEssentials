package com.zerog.neoessentials.webdashboard;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.webdashboard.api.endpoints.AdminEndpoint;
import com.zerog.neoessentials.webdashboard.api.endpoints.GameEndpoint;
import com.zerog.neoessentials.webdashboard.api.endpoints.LoggingEndpoint;
import com.zerog.neoessentials.webdashboard.api.endpoints.PlayerEndpoint;
import com.zerog.neoessentials.webdashboard.api.endpoints.ServerEndpoint;
import com.zerog.neoessentials.webdashboard.endpoints.MotdEndpoint;
import com.zerog.neoessentials.webdashboard.endpoints.PermissionEndpoint;
import com.zerog.neoessentials.webdashboard.endpoints.PlaceholderEndpoint;
import com.zerog.neoessentials.webdashboard.endpoints.TeleportEndpoint;
import com.zerog.neoessentials.webdashboard.handlers.AuthHandler;
import com.zerog.neoessentials.webdashboard.handlers.AuthenticationHandler;
import com.zerog.neoessentials.webdashboard.handlers.FileManagementHandler;
import com.zerog.neoessentials.docs.DocumentationHandler;
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
    private java.util.concurrent.ExecutorService executor;
    private boolean running = false;
    private MinecraftServer server;

    // Per-IP rate limiter: maps IP → deque of request timestamps
    private final java.util.Map<String, java.util.ArrayDeque<Long>> rateLimitMap =
        new java.util.concurrent.ConcurrentHashMap<>();

    private DashboardAPI() {
        // Empty constructor - port and bindAddress read from config on each start
    }
    
    /**
     * Get singleton instance of the Dashboard API
     */
    public static DashboardAPI getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new DashboardAPI();
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
     * Get the MinecraftServer instance
     */
    public MinecraftServer getServer() {
        return this.server;
    }

    /**
     * Get current port from config
     */
    public int getPort() {
        return ConfigManager.getInstance().getWebDashboardPort();
    }

    /**
     * Get current bind address from config
     */
    public String getBindAddress() {
        return ConfigManager.getInstance().getWebDashboardBindAddress();
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
            // Read port and bind address from config (allows dynamic updates)
            int port = getPort();
            String bindAddress = getBindAddress();

            LOGGER.info("Starting Dashboard API on {}:{}", bindAddress, port);

            // Create HTTP server with automatic fallback for bind address issues
            InetSocketAddress address;
            try {
                address = new InetSocketAddress(bindAddress, port);
                apiServer = HttpServer.create(address, 0);
            } catch (java.net.BindException e) {
                // If binding to the configured address fails, try fallback
                LOGGER.warn("Cannot bind to {}:{}. Error: {}", bindAddress, port, e.getMessage());

                if (!"0.0.0.0".equals(bindAddress)) {
                    LOGGER.info("Attempting fallback to 0.0.0.0:{} (all interfaces)...", port);
                    try {
                        address = new InetSocketAddress("0.0.0.0", port);
                        apiServer = HttpServer.create(address, 0);
                        LOGGER.info("Successfully bound to fallback address 0.0.0.0:{}", port);
                        bindAddress = "0.0.0.0"; // Update for logging below
                    } catch (java.net.BindException e2) {
                        LOGGER.error("Fallback also failed! Port {} may be in use or system doesn't support network binding.", port);
                        LOGGER.error("Possible solutions:");
                        LOGGER.error("  1. Change the port in config/neoessentials/config.json → webDashboard.port");
                        LOGGER.error("  2. Check if another application is using port {}", port);
                        LOGGER.error("  3. Verify your server's network configuration");
                        throw e2;
                    }
                } else {
                    LOGGER.error("Cannot bind to any interface on port {}!", port);
                    LOGGER.error("Possible solutions:");
                    LOGGER.error("  1. Change the port in config/neoessentials/config.json → webDashboard.port");
                    LOGGER.error("  2. Check if another application is using port {}", port);
                    LOGGER.error("  3. Verify your server's firewall and network settings");
                    throw e;
                }
            }

            // Set up thread pool and store reference for proper shutdown
            executor = Executors.newFixedThreadPool(10);
            apiServer.setExecutor(executor);

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
            LOGGER.info("Stopping Dashboard API server...");

            // Stop accepting new requests and wait up to 2 seconds for existing requests to complete
            apiServer.stop(2);

            // CRITICAL: Properly shutdown the executor service to prevent thread hang
            if (executor != null) {
                executor.shutdown();
                try {
                    if (!executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                        LOGGER.warn("Dashboard executor did not terminate gracefully, forcing shutdown...");
                        executor.shutdownNow();
                        // Wait a bit more for tasks to respond to being cancelled
                        if (!executor.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS)) {
                            LOGGER.error("Dashboard executor did not terminate after forced shutdown");
                        }
                    }
                } catch (InterruptedException e) {
                    LOGGER.warn("Interrupted while waiting for Dashboard executor shutdown");
                    executor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }

            running = false;
            LOGGER.info("Dashboard API stopped successfully");
        } catch (Exception e) {
            LOGGER.error("Error stopping Dashboard API", e);
        }
    }
    
    /**
     * Authentication + rate-limiting middleware wrapper.
     * - If webDashboard.security.requireAuthentication = true (default), validates Bearer token.
     * - If webDashboard.security.enableRateLimiting = true (default), enforces per-IP request cap.
     * - Supports both new AuthenticationManager sessions and legacy AuthHandler sessions.
     */
    @SuppressWarnings("ConstantConditions")
    private HttpHandler withAuth(HttpHandler handler) {
        return exchange -> {
            try {
                ConfigManager cfg = ConfigManager.getInstance();

                // ── CORS preflight bypass ─────────────────────────────────────
                if ("OPTIONS".equals(exchange.getRequestMethod())) {
                    exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                    exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
                    exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
                    exchange.sendResponseHeaders(204, -1);
                    return;
                }

                // ── Rate limiting ─────────────────────────────────────────────
                if (cfg.isDashboardRateLimitingEnabled()) {
                    String ip = exchange.getRemoteAddress().getAddress().getHostAddress();
                    int maxReq = cfg.getDashboardMaxRequestsPerMinute();
                    long now = System.currentTimeMillis();
                    long windowStart = now - 60_000L;

                    rateLimitMap.compute(ip, (k, deque) -> {
                        if (deque == null) deque = new java.util.ArrayDeque<>();
                        // Drop timestamps outside the 1-minute window
                        while (!deque.isEmpty() && deque.peekFirst() < windowStart) deque.pollFirst();
                        deque.addLast(now);
                        return deque;
                    });

                    int reqCount = rateLimitMap.get(ip).size();
                    if (reqCount > maxReq) {
                        String response = "{\"success\":false,\"error\":\"Rate limit exceeded. Max " + maxReq + " requests/min.\"}";
                        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                        exchange.getResponseHeaders().set("Content-Type", "application/json");
                        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                        exchange.getResponseHeaders().set("Retry-After", "60");
                        exchange.sendResponseHeaders(429, bytes.length);
                        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
                        LOGGER.debug("Rate limit exceeded for IP {} ({}/{})", ip, reqCount, maxReq);
                        return;
                    }
                }

                // ── Authentication ─────────────────────────────────────────────
                if (cfg.isDashboardAuthRequired()) {
                    String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
                    String token = null;
                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        token = authHeader.substring(7);
                    }

                    boolean authenticated = false;
                    String authUsername = null;
                    boolean authAdmin = false;

                    if (token != null && !token.isEmpty()) {
                        // Try new AuthenticationManager session first (password-based & Minecraft auth via AuthenticationHandler)
                        try {
                            com.zerog.neoessentials.webdashboard.security.Session newSession =
                                com.zerog.neoessentials.webdashboard.security.AuthenticationManager.getInstance().validateSession(token);
                            if (newSession != null) {
                                authenticated = true;
                                authUsername = newSession.getUsername();
                                authAdmin = newSession.getRole() == com.zerog.neoessentials.webdashboard.security.User.Role.ADMIN;
                            }
                        } catch (Exception e) {
                            LOGGER.debug("AuthenticationManager session check failed: {}", e.getMessage());
                        }

                        // Fallback: try legacy AuthHandler token (for existing Minecraft auth sessions)
                        if (!authenticated && AuthHandler.validateToken(token)) {
                            authenticated = true;
                            authUsername = AuthHandler.getUsername(token);
                            authAdmin = AuthHandler.isAdmin(token);
                        }
                    }

                    if (!authenticated) {
                        LOGGER.debug("Unauthorized API request to {} - token: {}", exchange.getRequestURI(), token == null ? "null" : "invalid");
                        String response = "{\"success\":false,\"error\":\"Unauthorized - Please login first\"}";
                        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                        exchange.getResponseHeaders().set("Content-Type", "application/json");
                        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                        exchange.sendResponseHeaders(401, bytes.length);
                        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
                        return;
                    }

                    exchange.setAttribute("auth-username", authUsername);
                    exchange.setAttribute("auth-admin", authAdmin);
                    LOGGER.debug("Authenticated API request to {} by {}", exchange.getRequestURI(), authUsername);
                }

                handler.handle(exchange);

            } catch (Exception e) {
                LOGGER.error("Error in auth middleware for {}", exchange.getRequestURI(), e);
                try {
                    if (!exchange.getResponseHeaders().containsKey("Content-Type")) {
                        String errorResponse = "{\"success\":false,\"error\":\"Authentication error: " + e.getMessage() + "\"}";
                        byte[] bytes = errorResponse.getBytes(StandardCharsets.UTF_8);
                        exchange.getResponseHeaders().set("Content-Type", "application/json");
                        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                        exchange.sendResponseHeaders(500, bytes.length);
                        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
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
        // AuthenticationHandler supports password-based, Minecraft, and Discord OAuth auth
        apiServer.createContext("/api/auth", new AuthenticationHandler());


        // Register API endpoint handlers with authentication middleware
        apiServer.createContext("/api/player", withAuth(new PlayerEndpoint(server)));
        apiServer.createContext("/api/server", withAuth(new ServerEndpoint(server)));
        apiServer.createContext("/api/game", withAuth(new GameEndpoint(server)));
        apiServer.createContext("/api/logging", withAuth(new LoggingEndpoint()));
        apiServer.createContext("/api/admin", withAuth(new AdminEndpoint(server)));
        apiServer.createContext("/api/files", withAuth(new FileManagementHandler()));
        apiServer.createContext("/api/permissions", withAuth(new PermissionEndpoint(server)));
        apiServer.createContext("/api/motd", withAuth(new MotdEndpoint(server)));
        apiServer.createContext("/api/rules", withAuth(new com.zerog.neoessentials.webdashboard.endpoints.RulesEndpoint()));
        apiServer.createContext("/api/teleport", withAuth(new TeleportEndpoint(server)));
        apiServer.createContext("/api/placeholders", withAuth(new PlaceholderEndpoint(server)));
        apiServer.createContext("/api/shops", withAuth(new com.zerog.neoessentials.shop.dashboard.ShopEndpoint()));
        apiServer.createContext("/api/docs", new DocumentationHandler());

        LOGGER.info("API endpoints registered:");
        LOGGER.info("  - /api/auth/* (login, logout, validate, discord)");
        LOGGER.info("  - /api/textures/* (item, block textures from server resource packs)");
        LOGGER.info("  - /api/player/* (profile, stats, achievements, inventory, status, health, xp, location, homes, online) [AUTH REQUIRED]");
        LOGGER.info("  - /api/server/* (profile, performance, worlds, players, entities, memory, history, assets) [AUTH REQUIRED]");
        LOGGER.info("  - /api/game/* (statistics, events, activity, blocks) [AUTH REQUIRED]");
        LOGGER.info("  - /api/logging/* (requests, errors, performance) [AUTH REQUIRED]");
        LOGGER.info("  - /api/admin/* (restart, stop, reload, save) [AUTH REQUIRED - ADMIN ONLY]");
        LOGGER.info("  - /api/files/* (browse, read, write, create, upload, delete, backup, restore, cloud) [AUTH REQUIRED]");
        LOGGER.info("  - /api/permissions/* (overview, groups, users, manage) [AUTH REQUIRED - ADMIN ONLY]");
        LOGGER.info("  - /api/motd/* (overview, profiles, active, rotation, broadcast) [AUTH REQUIRED]");
        LOGGER.info("  - /api/rules/* (list, add, edit, delete, reload) [AUTH REQUIRED]");
        LOGGER.info("  - /api/teleport/* (settings GET/PUT) [AUTH REQUIRED - ADMIN ONLY]");
        LOGGER.info("  - /api/placeholders/* (list, resolve, stats) [AUTH REQUIRED]");
        LOGGER.info("  - /api/shops/* (list, stats, npc, csv/export, csv/import, price) [AUTH REQUIRED]");
        LOGGER.info("  - /api/docs/* (sections, api, tutorials, faq, videos, search) [PUBLIC]");

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
                }
            } catch (java.io.FileNotFoundException e) {
                // 404 Not Found
                LOGGER.warn("File not found: {}", path);
                String response = "404 Not Found: " + path;
                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/plain");
                exchange.sendResponseHeaders(404, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
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
}
