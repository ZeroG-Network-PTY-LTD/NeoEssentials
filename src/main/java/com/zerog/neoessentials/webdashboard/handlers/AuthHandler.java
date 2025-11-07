package com.zerog.neoessentials.webdashboard.handlers;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.config.ConfigManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Authentication handler for web dashboard.
 * Supports Minecraft player authentication and Discord OAuth.
 */
public class AuthHandler implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthHandler.class);
    private final MinecraftServer server;
    
    // Session management
    private static final Map<String, SessionData> sessions = new ConcurrentHashMap<>();
    private static final long SESSION_TIMEOUT = 24 * 60 * 60 * 1000; // 24 hours
    private static final SecureRandom random = new SecureRandom();
    
    // Required permissions
    private static final String DASHBOARD_VIEW_PERMISSION = "neoessentials.dashboard.view";
    private static final String DASHBOARD_ADMIN_PERMISSION = "neoessentials.dashboard.admin";
    
    public AuthHandler(MinecraftServer server) {
        this.server = server;
    }
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        
        try {
            if (path.endsWith("/auth/login") && "POST".equals(method)) {
                handleLogin(exchange);
            } else if (path.endsWith("/auth/logout") && "POST".equals(method)) {
                handleLogout(exchange);
            } else if (path.endsWith("/auth/validate") && "GET".equals(method)) {
                handleValidate(exchange);
            } else if (path.endsWith("/auth/discord") && "POST".equals(method)) {
                handleDiscordAuth(exchange);
            } else {
                sendError(exchange, 404, "Not Found");
            }
        } catch (Exception e) {
            LOGGER.error("Error handling auth request", e);
            sendError(exchange, 500, "Internal Server Error");
        }
    }
    
    /**
     * Handle Minecraft player login
     */
    private void handleLogin(HttpExchange exchange) throws IOException {
        try {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JsonObject request = com.google.gson.JsonParser.parseString(body).getAsJsonObject();
            
            String username = request.has("username") ? request.get("username").getAsString() : null;
            
            if (username == null || username.isEmpty()) {
                sendError(exchange, 400, "Username is required");
                return;
            }
            
            // Check if player is online
            ServerPlayer player = server.getPlayerList().getPlayerByName(username);
            if (player == null) {
                sendError(exchange, 401, "Player must be online on the server to authenticate");
                return;
            }
            
            // Check if player is OP (super admin)
            boolean isOp = server.getPlayerList().isOp(player.getGameProfile());
            
            // Check permissions
            boolean hasViewPermission = isOp || PermissionAPI.hasPermission(player.getUUID(), DASHBOARD_VIEW_PERMISSION);
            boolean hasAdminPermission = isOp || PermissionAPI.hasPermission(player.getUUID(), DASHBOARD_ADMIN_PERMISSION);
            
            if (!hasViewPermission) {
                sendError(exchange, 403, "You don't have permission to access the dashboard");
                return;
            }
            
            // Generate session token
            String token = generateToken();
            SessionData session = new SessionData(
                username,
                player.getStringUUID(),
                "minecraft",
                hasAdminPermission,
                System.currentTimeMillis()
            );
            
            sessions.put(token, session);
            
            // Clean up old sessions
            cleanupSessions();
            
            // Send response
            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("token", token);
            response.addProperty("username", username);
            response.addProperty("isAdmin", hasAdminPermission);
            response.addProperty("authType", "minecraft");
            
            sendJson(exchange, 200, response.toString());
            
            LOGGER.info("Player {} authenticated to dashboard (admin: {})", username, hasAdminPermission);
            
        } catch (Exception e) {
            LOGGER.error("Error during login", e);
            sendError(exchange, 500, "Authentication failed");
        }
    }
    
    /**
     * Handle Discord OAuth authentication
     */
    private void handleDiscordAuth(HttpExchange exchange) throws IOException {
        try {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JsonObject request = com.google.gson.JsonParser.parseString(body).getAsJsonObject();
            
            String discordId = request.has("discordId") ? request.get("discordId").getAsString() : null;
            String discordUsername = request.has("username") ? request.get("username").getAsString() : null;
            
            if (discordId == null || discordUsername == null) {
                sendError(exchange, 400, "Discord ID and username are required");
                return;
            }
            
            // Check Discord role permissions via config
            boolean hasPermission = checkDiscordPermission(discordId);
            
            if (!hasPermission) {
                sendError(exchange, 403, "Your Discord role doesn't have permission to access the dashboard");
                return;
            }
            
            // Generate session token
            String token = generateToken();
            SessionData session = new SessionData(
                discordUsername,
                discordId,
                "discord",
                checkDiscordAdminPermission(discordId),
                System.currentTimeMillis()
            );
            
            sessions.put(token, session);
            cleanupSessions();
            
            // Send response
            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("token", token);
            response.addProperty("username", discordUsername);
            response.addProperty("isAdmin", session.isAdmin);
            response.addProperty("authType", "discord");
            
            sendJson(exchange, 200, response.toString());
            
            LOGGER.info("Discord user {} authenticated to dashboard (admin: {})", discordUsername, session.isAdmin);
            
        } catch (Exception e) {
            LOGGER.error("Error during Discord authentication", e);
            sendError(exchange, 500, "Authentication failed");
        }
    }
    
    /**
     * Validate existing session token
     */
    private void handleValidate(HttpExchange exchange) throws IOException {
        String token = getTokenFromHeader(exchange);
        
        if (token == null) {
            sendError(exchange, 401, "No token provided");
            return;
        }
        
        SessionData session = sessions.get(token);
        
        if (session == null || isExpired(session)) {
            if (session != null) {
                sessions.remove(token);
            }
            sendError(exchange, 401, "Invalid or expired token");
            return;
        }
        
        // Return session info
        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.addProperty("username", session.username);
        response.addProperty("isAdmin", session.isAdmin);
        response.addProperty("authType", session.authType);
        
        sendJson(exchange, 200, response.toString());
    }
    
    /**
     * Logout and invalidate session
     */
    private void handleLogout(HttpExchange exchange) throws IOException {
        String token = getTokenFromHeader(exchange);
        
        if (token != null) {
            sessions.remove(token);
        }
        
        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        
        sendJson(exchange, 200, response.toString());
    }
    
    /**
     * Validate token from any API request
     */
    public static boolean validateToken(String token) {
        if (token == null) return false;
        
        SessionData session = sessions.get(token);
        if (session == null || isExpired(session)) {
            if (session != null) sessions.remove(token);
            return false;
        }
        
        return true;
    }
    
    /**
     * Check if session has admin privileges
     */
    public static boolean isAdmin(String token) {
        SessionData session = sessions.get(token);
        return session != null && !isExpired(session) && session.isAdmin;
    }
    
    /**
     * Get session username
     */
    public static String getUsername(String token) {
        SessionData session = sessions.get(token);
        return (session != null && !isExpired(session)) ? session.username : null;
    }
    
    // Helper methods
    
    private String generateToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    
    private String getTokenFromHeader(HttpExchange exchange) {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
    
    private static boolean isExpired(SessionData session) {
        return System.currentTimeMillis() - session.timestamp > SESSION_TIMEOUT;
    }
    
    private void cleanupSessions() {
        sessions.entrySet().removeIf(entry -> isExpired(entry.getValue()));
    }
    
    /**
     * Check if Discord user has permission to view dashboard
     */
    private boolean checkDiscordPermission(String discordId) {
        // Get allowed Discord roles from config
        List<String> allowedRoles = getDiscordAllowedRoles();
        
        // FUTURE IMPLEMENTATION: Actual Discord role checking via DCIntegration API
        // When DCIntegration mod provides role checking API, implement here
        LOGGER.warn("Discord permission check not yet implemented for user: {}", discordId);
        LOGGER.debug("Allowed Discord roles: {}", allowedRoles);
        return false;
    }
    
    /**
     * Check if Discord user has admin permissions
     */
    private boolean checkDiscordAdminPermission(String discordId) {
        // Get admin Discord roles from config
        List<String> adminRoles = getDiscordAdminRoles();
        
        // FUTURE IMPLEMENTATION: Actual Discord role checking via DCIntegration API
        LOGGER.warn("Discord admin check not yet implemented for user: {}", discordId);
        LOGGER.debug("Admin Discord roles: {}", adminRoles);
        return false;
    }
    
    /**
     * Get allowed Discord roles from config
     */
    private List<String> getDiscordAllowedRoles() {
        List<String> roles = new ArrayList<>();
        try {
            JsonObject config = ConfigManager.getInstance().getConfig("config.json");
            if (config.has("webDashboard") && config.getAsJsonObject("webDashboard").has("discord")) {
                JsonObject discord = config.getAsJsonObject("webDashboard").getAsJsonObject("discord");
                if (discord.has("allowedRoles") && discord.get("allowedRoles").isJsonArray()) {
                    for (var el : discord.getAsJsonArray("allowedRoles")) {
                        if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isString()) {
                            roles.add(el.getAsString());
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to read Discord allowed roles from config", e);
        }
        
        // Default roles if not configured
        if (roles.isEmpty()) {
            roles.addAll(Arrays.asList("Admin", "Moderator", "Staff"));
        }
        
        return roles;
    }
    
    /**
     * Get admin Discord roles from config
     */
    private List<String> getDiscordAdminRoles() {
        List<String> roles = new ArrayList<>();
        try {
            JsonObject config = ConfigManager.getInstance().getConfig("config.json");
            if (config.has("webDashboard") && config.getAsJsonObject("webDashboard").has("discord")) {
                JsonObject discord = config.getAsJsonObject("webDashboard").getAsJsonObject("discord");
                if (discord.has("adminRoles") && discord.get("adminRoles").isJsonArray()) {
                    for (var el : discord.getAsJsonArray("adminRoles")) {
                        if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isString()) {
                            roles.add(el.getAsString());
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to read Discord admin roles from config", e);
        }
        
        // Default admin role if not configured
        if (roles.isEmpty()) {
            roles.add("Admin");
        }
        
        return roles;
    }
    
    private void sendJson(HttpExchange exchange, int code, String json) throws IOException {
        byte[] response = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(code, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }
    
    private void sendError(HttpExchange exchange, int code, String message) throws IOException {
        JsonObject error = new JsonObject();
        error.addProperty("success", false);
        error.addProperty("error", message);
        sendJson(exchange, code, error.toString());
    }
    
    /**
     * Session data holder
     */
    private static class SessionData {
        final String username;
        @SuppressWarnings("unused") // Stored for future auditing/logging features
        final String userId; // UUID for Minecraft, Discord ID for Discord
        final String authType; // "minecraft" or "discord"
        final boolean isAdmin;
        final long timestamp;
        
        SessionData(String username, String userId, String authType, boolean isAdmin, long timestamp) {
            this.username = username;
            this.userId = userId;
            this.authType = authType;
            this.isAdmin = isAdmin;
            this.timestamp = timestamp;
        }
    }
}
