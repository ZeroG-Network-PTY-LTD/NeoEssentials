package com.zerog.neoessentials.webdashboard;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.webdashboard.api.endpoints.AdminEndpoint;
import com.zerog.neoessentials.webdashboard.api.endpoints.GameEndpoint;
import com.zerog.neoessentials.webdashboard.api.endpoints.LoggingEndpoint;
import com.zerog.neoessentials.webdashboard.api.endpoints.PlayerEndpoint;
import com.zerog.neoessentials.webdashboard.api.endpoints.ServerEndpoint;
import com.zerog.neoessentials.webdashboard.endpoints.BackupEndpoint;
import com.zerog.neoessentials.webdashboard.endpoints.CloudStorageEndpoint;
import com.zerog.neoessentials.webdashboard.endpoints.DiscordEndpoint;
import com.zerog.neoessentials.webdashboard.endpoints.KitsEndpoint;
import com.zerog.neoessentials.webdashboard.endpoints.HologramEndpoint;
import com.zerog.neoessentials.webdashboard.endpoints.ModerationEndpoint;
import com.zerog.neoessentials.webdashboard.endpoints.MotdEndpoint;
import com.zerog.neoessentials.webdashboard.endpoints.PermissionEndpoint;
import com.zerog.neoessentials.webdashboard.endpoints.PlaceholderEndpoint;
import com.zerog.neoessentials.webdashboard.endpoints.StatsEndpoint;
import com.zerog.neoessentials.webdashboard.endpoints.TeleportEndpoint;
import com.zerog.neoessentials.webdashboard.endpoints.UserManagementEndpoint;
import com.zerog.neoessentials.webdashboard.handlers.AuthHandler;
import com.zerog.neoessentials.webdashboard.handlers.AuthenticationHandler;
import com.zerog.neoessentials.webdashboard.handlers.FileManagementHandler;
import com.zerog.neoessentials.docs.DocumentationHandler;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
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
    /** Held so the TPS-sampler thread can be shut down cleanly. */
    private StatsEndpoint statsEndpoint;

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
            NeoLog.warn(LOGGER, LogCategory.WEB_DASHBOARD, "Dashboard API is already running");
            return;
        }
        
        if (server == null) {
            NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "Cannot start Dashboard API: MinecraftServer not set");
            return;
        }
        
        try {
            // Read port and bind address from config (allows dynamic updates)
            int port = getPort();
            String bindAddress = getBindAddress();

            NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "Starting Dashboard API on {}:{}", bindAddress, port);

            // Create HTTP server with automatic fallback for bind address issues
            InetSocketAddress address;
            try {
                address = new InetSocketAddress(bindAddress, port);
                apiServer = HttpServer.create(address, 0);
            } catch (java.net.BindException e) {
                // If binding to the configured address fails, try fallback
                NeoLog.warn(LOGGER, LogCategory.WEB_DASHBOARD, "Cannot bind to {}:{}. Error: {}", bindAddress, port, e.getMessage());

                if (!"0.0.0.0".equals(bindAddress)) {
                    NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "Attempting fallback to 0.0.0.0:{} (all interfaces)...", port);
                    try {
                        address = new InetSocketAddress("0.0.0.0", port);
                        apiServer = HttpServer.create(address, 0);
                        NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "Successfully bound to fallback address 0.0.0.0:{}", port);
                        bindAddress = "0.0.0.0"; // Update for logging below
                    } catch (java.net.BindException e2) {
                        NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "Fallback also failed! Port {} may be in use or system doesn't support network binding.", port);
                        NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "Possible solutions:");
                        NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "  1. Change the port in config/neoessentials/config.json → webDashboard.port");
                        NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "  2. Check if another application is using port {}", port);
                        NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "  3. Verify your server's network configuration");
                        throw e2;
                    }
                } else {
                    NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "Cannot bind to any interface on port {}!", port);
                    NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "Possible solutions:");
                    NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "  1. Change the port in config/neoessentials/config.json → webDashboard.port");
                    NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "  2. Check if another application is using port {}", port);
                    NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "  3. Verify your server's firewall and network settings");
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

            NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "Dashboard API started successfully on {}:{}", bindAddress, port);
            NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "Access the dashboard at: {}", dashboardUrl);
            NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "API Endpoints available at: {}/api/", dashboardUrl);

        } catch (IOException e) {
            NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "Failed to start Dashboard API server", e);
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
            NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "Stopping Dashboard API server...");

            // Shutdown stats sampler thread
            if (statsEndpoint != null) {
                statsEndpoint.shutdown();
                statsEndpoint = null;
            }

            // Stop accepting new requests and wait up to 2 seconds for existing requests to complete
            apiServer.stop(2);

            // CRITICAL: Properly shutdown the executor service to prevent thread hang
            if (executor != null) {
                executor.shutdown();
                try {
                    if (!executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                        NeoLog.warn(LOGGER, LogCategory.WEB_DASHBOARD, "Dashboard executor did not terminate gracefully, forcing shutdown...");
                        executor.shutdownNow();
                        // Wait a bit more for tasks to respond to being cancelled
                        if (!executor.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS)) {
                            NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "Dashboard executor did not terminate after forced shutdown");
                        }
                    }
                } catch (InterruptedException e) {
                    NeoLog.warn(LOGGER, LogCategory.WEB_DASHBOARD, "Interrupted while waiting for Dashboard executor shutdown");
                    executor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }

            running = false;
            NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "Dashboard API stopped successfully");
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "Error stopping Dashboard API", e);
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
        return withAuth(handler, true);
    }

    /**
     * @param requireAuth if false, still applies CORS/rate-limiting but skips the Bearer-token
     *                    check entirely — for genuinely public routes (e.g. the public
     *                    moderation lookup), not a bypass of the auth check itself.
     */
    private HttpHandler withAuth(HttpHandler handler, boolean requireAuth) {
        return exchange -> {
            try {
                NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Dispatching request: {} {}",
                    exchange.getRequestMethod(), exchange.getRequestURI().getPath());

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
                        NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Rate limit exceeded for IP {} ({}/{})", ip, reqCount, maxReq);
                        return;
                    }
                }

                // ── Authentication ─────────────────────────────────────────────
                if (requireAuth && cfg.isDashboardAuthRequired()) {
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
                            NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "AuthenticationManager session check failed: {}", e.getMessage());
                        }

                        // Fallback: try legacy AuthHandler token (for existing Minecraft auth sessions)
                        if (!authenticated && AuthHandler.validateToken(token)) {
                            authenticated = true;
                            authUsername = AuthHandler.getUsername(token);
                            authAdmin = AuthHandler.isAdmin(token);
                        }

                        // Fallback: long-lived API key (server-to-server — the external dashboard's
                        // own backend, not a human session). Distinct credential space from the
                        // above; see ApiKeyManager for why.
                        if (!authenticated) {
                            com.zerog.neoessentials.webdashboard.security.ApiKeyManager.ApiKeyRecord keyRecord =
                                com.zerog.neoessentials.webdashboard.security.ApiKeyManager.getInstance().validate(token);
                            if (keyRecord != null) {
                                authenticated = true;
                                authUsername = "apikey:" + keyRecord.label;
                                authAdmin = keyRecord.role == com.zerog.neoessentials.webdashboard.security.User.Role.ADMIN;
                                exchange.setAttribute("auth-type", "apikey");
                                exchange.setAttribute("auth-role", keyRecord.role.name());
                            }
                        }
                    }

                    if (!authenticated) {
                        NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Unauthorized API request to {} - token: {}", exchange.getRequestURI(), token == null ? "null" : "invalid");
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
                    NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Authenticated API request to {} by {}", exchange.getRequestURI(), authUsername);
                }

                handler.handle(exchange);

            } catch (Exception e) {
                NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "Error in auth middleware for {}", exchange.getRequestURI(), e);
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
                    NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "Failed to send error response", ex);
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
        apiServer.createContext("/api/scoreboard", withAuth(new com.zerog.neoessentials.webdashboard.endpoints.ScoreboardEndpoint(server)));
        apiServer.createContext("/api/leaderboard", withAuth(new com.zerog.neoessentials.webdashboard.endpoints.LeaderboardEndpoint(server)));
        apiServer.createContext("/api/rules", withAuth(new com.zerog.neoessentials.webdashboard.endpoints.RulesEndpoint()));
        apiServer.createContext("/api/teleport", withAuth(new TeleportEndpoint(server)));
        apiServer.createContext("/api/placeholders", withAuth(new PlaceholderEndpoint(server)));
        apiServer.createContext("/api/shops", withAuth(new com.zerog.neoessentials.shop.dashboard.ShopEndpoint()));
        apiServer.createContext("/api/apikeys",     withAuth(new com.zerog.neoessentials.webdashboard.endpoints.ApiKeyEndpoint()));
        apiServer.createContext("/api/backup",      withAuth(new BackupEndpoint()));
        apiServer.createContext("/api/discord",     withAuth(new DiscordEndpoint()));
        apiServer.createContext("/api/cloud",       withAuth(new CloudStorageEndpoint()));
        apiServer.createContext("/api/users",       withAuth(new UserManagementEndpoint()));
        apiServer.createContext("/api/moderation",  withAuth(new ModerationEndpoint(server)));
        // Public, no-login player lookup (bans/mutes/kicks/warns by name + recent activity feed) —
        // still CORS/rate-limited via withAuth(handler, false), just skips the Bearer-token check.
        apiServer.createContext("/api/public/moderation",
            withAuth(new com.zerog.neoessentials.webdashboard.endpoints.PublicModerationEndpoint(server), false));
        apiServer.createContext("/api/kits",        withAuth(new KitsEndpoint()));
        apiServer.createContext("/api/holograms",   withAuth(new HologramEndpoint()));
        apiServer.createContext("/api/warps",       withAuth(new com.zerog.neoessentials.webdashboard.endpoints.WarpsEndpoint(server)));
        apiServer.createContext("/api/commands",    withAuth(new com.zerog.neoessentials.webdashboard.handlers.CommandExecutionHandler()));
        apiServer.createContext("/api/economy",     withAuth(new com.zerog.neoessentials.webdashboard.endpoints.EconomyEndpoint(server)));
        statsEndpoint = new StatsEndpoint(server);
        apiServer.createContext("/api/stats", withAuth(statsEndpoint));
        apiServer.createContext("/api/docs", new DocumentationHandler());
        // Unauthenticated, no-op reachability check — lets an external dashboard app (or
        // curl) confirm "can I even reach this port" independently of whether login/auth
        // works, which is the single biggest cause of "the connection isn't establishing"
        // confusion between a separately-hosted dashboard and this mod.
        apiServer.createContext("/api/ping", exchange -> {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, OPTIONS");
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            String body = "{\"success\":true,\"mod\":\"neoessentials\",\"mode\":\""
                + (ConfigManager.isDashboardInternalUiEnabled() ? "internal" : "external") + "\"}";
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
        });

        NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "API endpoints registered:");
        NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "  - /api/auth/* (login, logout, validate, discord)");
        NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "  - /api/textures/* (item, block textures from server resource packs)");
        NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "  - /api/player/* (profile, stats, achievements, inventory, status, health, xp, location, homes, online, fly, god, feed, extinguish, speed, nickname, give, burn, kill, effect, lightning, spawnmob, sudo, clearinventory, ptime, pweather) [AUTH REQUIRED]");
        NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "  - /api/server/* (profile, performance, worlds, players, entities, memory, history, assets) [AUTH REQUIRED]");
        NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "  - /api/game/* (statistics, events, activity, blocks) [AUTH REQUIRED]");
        NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "  - /api/logging/* (requests, errors, performance) [AUTH REQUIRED]");
        NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "  - /api/admin/* (restart, stop, reload, save, broadcast) [AUTH REQUIRED - ADMIN ONLY]");
        NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "  - /api/files/* (browse, read, write, create, upload, delete, backup, restore, cloud) [AUTH REQUIRED]");
        NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "  - /api/permissions/* (overview, groups, users, manage) [AUTH REQUIRED - ADMIN ONLY]");
        NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "  - /api/motd/* (overview, profiles, active, rotation, broadcast) [AUTH REQUIRED]");
        NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "  - /api/rules/* (list, add, edit, delete, reload) [AUTH REQUIRED]");
        NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "  - /api/teleport/* (settings GET/PUT) [AUTH REQUIRED - ADMIN ONLY]");
        NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "  - /api/placeholders/* (list, resolve, stats) [AUTH REQUIRED]");
        NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "  - /api/shops/* (list, stats, npc, csv/export, csv/import, price) [AUTH REQUIRED]");
        NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "  - /api/backup/*       (status, list, create, restore, download, delete) [ADMIN]");
        NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "  - /api/discord/*      (status, events, test) [AUTH]");
        NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "  - /api/cloud/*        (status, config, test, files, upload, delete) [ADMIN]");
        NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "  - /api/users/*        (list, sessions, create, role, password, enable, disable, delete) [ADMIN]");
        NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "  - /api/moderation/*   (overview, bans, warns, ban, unban) [AUTH/ADMIN]");
        NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "  - /api/kits/*         (list, stats, {name}) [AUTH]");
        NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "  - /api/holograms/*    (list, stats, create, get, update, delete, spawn, despawn, visible) [ADMIN]");
        NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "  - /api/stats/* (overview, economy, activity, performance) [AUTH REQUIRED]");
        NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "  - /api/docs/* (sections, api, tutorials, faq, videos, search) [PUBLIC]");

        if (!ConfigManager.isDashboardInternalUiEnabled()) {
            // webDashboard.mode: "external" — only the REST API above is served. Register a
            // minimal "/" so a browser hitting the bare host:port gets a helpful message
            // instead of a raw connection reset, but skip the bundled-UI resource check and
            // static-file catch-all entirely (no point warning about missing UI resources
            // when this server intentionally never serves them).
            NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "webDashboard.mode is 'external' — REST API only, bundled dashboard UI not served at \"/\".");
            apiServer.createContext("/", exchange -> {
                String body = "{\"success\":true,\"mode\":\"external\",\"message\":"
                    + "\"NeoEssentials dashboard API — this server does not serve a UI. "
                    + "Point your external dashboard app's API URL here and use /api/*.\"}";
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
            });
            return;
        }

        // Check if dashboard resources are available
        try (java.io.InputStream testStream = getClass().getResourceAsStream("/webdashboard/index.html")) {
            if (testStream != null) {
                NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "Dashboard resources verified - index.html found");
            } else {
                NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "Dashboard resources NOT found - /webdashboard/index.html is null!");
            }
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "Error checking dashboard resources", e);
        }

        // Serve static frontend files (catch-all, must be registered last)
        apiServer.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            
            NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Serving static file: {}", path);
            
            // Default to index.html
            if (path.equals("/") || path.equals("/index.html")) {
                path = "/index.html";
            }
            
            // Serve file from resources
            try (java.io.InputStream in = getClass().getResourceAsStream("/webdashboard" + path)) {
                if (in != null) {
                    serveStaticBytes(exchange, path, in.readAllBytes());
                    return;
                }

                // No exact resource match. `getResourceAsStream` returns null rather than
                // throwing, so without this branch a missing file silently produced no
                // response at all (the connection just hung until the client gave up) —
                // neither a 404 nor the SPA fallback below ever fired. A path with a file
                // extension (".js", ".css", an image, ...) is a genuine missing asset; anything
                // else is a client-side route (react-router) being deep-linked/refreshed, which
                // needs index.html so the SPA can boot and render that route itself.
                boolean looksLikeAssetRequest = path.lastIndexOf('.') > path.lastIndexOf('/');
                if (looksLikeAssetRequest) {
                    sendStatic404(exchange, path);
                    return;
                }

                try (java.io.InputStream fallback = getClass().getResourceAsStream("/webdashboard/index.html")) {
                    if (fallback == null) {
                        sendStatic404(exchange, path);
                        return;
                    }
                    NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "SPA fallback: serving index.html for client-side route {}", path);
                    serveStaticBytes(exchange, "/index.html", fallback.readAllBytes());
                }
            } catch (Exception e) {
                NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "Error serving file: {}", path, e);
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
        
        NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "Static file serving enabled for frontend");
    }

    /**
     * Writes a static-file response with ETag/cache-control headers, handling the
     * If-None-Match 304 short-circuit. Shared by the direct-hit and SPA-fallback
     * (index.html) paths in the "/" catch-all above so both get identical caching behavior.
     */
    private void serveStaticBytes(HttpExchange exchange, String servedPath, byte[] bytes) throws IOException {
        String etag = "\"" + Integer.toHexString(java.util.Arrays.hashCode(bytes)) + "\"";

        String ifNoneMatch = exchange.getRequestHeaders().getFirst("If-None-Match");
        if (etag.equals(ifNoneMatch)) {
            exchange.sendResponseHeaders(304, -1);
            NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Served 304 Not Modified for: {} (ETag: {})", servedPath, etag);
            return;
        }

        exchange.getResponseHeaders().set("Content-Type", getContentType(servedPath));
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate, max-age=0");
        exchange.getResponseHeaders().set("Pragma", "no-cache");
        exchange.getResponseHeaders().set("Expires", "0");
        exchange.getResponseHeaders().set("ETag", etag);
        exchange.getResponseHeaders().set("Last-Modified", "Fri, 03 Jan 2026 00:00:00 GMT");
        exchange.getResponseHeaders().set("X-NeoEssentials-Version", getBuildNumber());

        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }

        NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Successfully served: {} ({} bytes, ETag: {})", servedPath, bytes.length, etag);
    }

    /** Writes a genuine 404 for a missing static asset (not a client-side SPA route). */
    private void sendStatic404(HttpExchange exchange, String path) throws IOException {
        NeoLog.warn(LOGGER, LogCategory.WEB_DASHBOARD, "File not found: {}", path);
        String response = "404 Not Found: " + path;
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain");
        exchange.sendResponseHeaders(404, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    /**
     * Get content type based on file extension
     */
    private String getContentType(String path) {
        if (path.endsWith(".html")) return "text/html";
        if (path.endsWith(".css")) return "text/css";
        if (path.endsWith(".js") || path.endsWith(".mjs")) return "application/javascript";
        if (path.endsWith(".json")) return "application/json";
        if (path.endsWith(".png")) return "image/png";
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
        if (path.endsWith(".svg")) return "image/svg+xml";
        if (path.endsWith(".ico")) return "image/x-icon";
        if (path.endsWith(".woff2")) return "font/woff2";
        if (path.endsWith(".woff")) return "font/woff";
        if (path.endsWith(".ttf")) return "font/ttf";
        if (path.endsWith(".map")) return "application/json";
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
            NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Could not read build number: {}", e.getMessage());
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
