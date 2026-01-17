package com.zerog.neoessentials.webdashboard.handlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.zerog.neoessentials.webdashboard.security.AuthenticationManager;
import com.zerog.neoessentials.webdashboard.security.DiscordAuthConfig;
import com.zerog.neoessentials.webdashboard.security.DiscordAuthProvider;
import com.zerog.neoessentials.webdashboard.security.DiscordUser;
import com.zerog.neoessentials.webdashboard.security.Session;
import com.zerog.neoessentials.webdashboard.security.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Handler for /api/auth endpoint
 * Manages authentication, session management, and user operations
 */
public class AuthenticationHandler implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationHandler.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Add CORS headers
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
        
        // Handle OPTIONS preflight
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }
        
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        
        try {
            if ("POST".equals(method) && path.endsWith("/login")) {
                handleLogin(exchange);
            } else if ("GET".equals(method) && path.endsWith("/discord")) {
                handleDiscordAuth(exchange);
            } else if ("POST".equals(method) && path.endsWith("/logout")) {
                handleLogout(exchange);
            } else if ("GET".equals(method) && path.endsWith("/validate")) {
                handleValidate(exchange);
            } else if ("GET".equals(method) && path.endsWith("/users")) {
                handleGetUsers(exchange);
            } else if ("POST".equals(method) && path.endsWith("/users")) {
                handleCreateUser(exchange);
            } else if ("PUT".equals(method) && path.contains("/users/")) {
                handleUpdateUser(exchange);
            } else if ("DELETE".equals(method) && path.contains("/users/")) {
                handleDeleteUser(exchange);
            } else if ("GET".equals(method) && path.endsWith("/sessions")) {
                handleGetSessions(exchange);
            } else if ("GET".equals(method) && (path.endsWith("/session") || path.equals("/api/session"))) {
                handleGetCurrentSession(exchange);
            } else if ("POST".equals(method) && path.endsWith("/change-password")) {
                handleChangePassword(exchange);
            } else {
                sendJsonResponse(exchange, 400, createErrorResponse("Invalid endpoint"));
            }
        } catch (Exception e) {
            LOGGER.error("Error handling authentication request", e);
            sendJsonResponse(exchange, 500, createErrorResponse("Internal server error: " + e.getMessage()));
        }
    }

    /**
     * POST /api/auth/change-password
     * Body: {"oldPassword": "...", "newPassword": "..."}
     * Allows a logged-in user to change their own password (not just admin)
     * Clears requiresPasswordChange and isTempPassword flags after success
     */
    private void handleChangePassword(HttpExchange exchange) throws IOException {
        String sessionId = getSessionIdFromCookie(exchange);
        if (sessionId == null) {
            sendJsonResponse(exchange, 401, createErrorResponse("No active session"));
            return;
        }
        AuthenticationManager authManager = AuthenticationManager.getInstance();
        Session session = authManager.validateSession(sessionId);
        if (session == null) {
            sendJsonResponse(exchange, 401, createErrorResponse("Invalid or expired session"));
            return;
        }
        String userId = session.getUserId();
        User user = authManager.getUser(userId);
        if (user == null) {
            sendJsonResponse(exchange, 404, createErrorResponse("User not found"));
            return;
        }
        String requestBody = readRequestBody(exchange);
        JsonObject request = GSON.fromJson(requestBody, JsonObject.class);
        if (!request.has("oldPassword") || !request.has("newPassword")) {
            sendJsonResponse(exchange, 400, createErrorResponse("Missing oldPassword or newPassword"));
            return;
        }
        String oldPassword = request.get("oldPassword").getAsString();
        String newPassword = request.get("newPassword").getAsString();
        // Verify old password
        String oldHash = authManager.hashPassword(oldPassword);
        if (!oldHash.equals(user.getPasswordHash())) {
            sendJsonResponse(exchange, 403, createErrorResponse("Old password is incorrect"));
            return;
        }
        try {
            authManager.updatePassword(userId, newPassword);
            user.setRequiresPasswordChange(false);
            user.setTempPassword(false);
            authManager.saveUsers();
            // Debug logging for user and session state after password change
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Password changed for user '{}': requiresPasswordChange={}, isTempPassword={}",
                    user.getUsername(), user.requiresPasswordChange(), user.isTempPassword());
                Session sessionObj = authManager.validateSession(sessionId);
                LOGGER.debug("Session state after password change: sessionId={}, active={}, requiresPasswordChange={}",
                    sessionId, sessionObj != null ? sessionObj.isActive() : "null", sessionObj != null ? sessionObj.requiresPasswordChange() : "null");
            }
            // Invalidate the current session after password change
            authManager.logout(sessionId);
            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("message", "Password changed successfully. Please log in again.");
            sendJsonResponse(exchange, 200, response);
        } catch (IllegalArgumentException e) {
            sendJsonResponse(exchange, 400, createErrorResponse(e.getMessage()));
        }
    }
    
    /**
     * POST /api/auth/login
     * Body: {"username": "admin", "password": "password"} OR {"username": "minecraft_name", "type": "minecraft"}
     * Supports both password-based and permission-based (Minecraft) authentication
     */
    private void handleLogin(HttpExchange exchange) throws IOException {
        String requestBody = readRequestBody(exchange);
        JsonObject request = GSON.fromJson(requestBody, JsonObject.class);
        
        if (!request.has("username")) {
            sendJsonResponse(exchange, 400, createErrorResponse("Missing username"));
            return;
        }
        
        String username = request.get("username").getAsString();
        String ipAddress = exchange.getRemoteAddress().getAddress().getHostAddress();
        String userAgent = exchange.getRequestHeaders().getFirst("User-Agent");
        AuthenticationManager authManager = AuthenticationManager.getInstance();

        // Check if this is permission-based (Minecraft) authentication
        if (request.has("type") && "minecraft".equals(request.get("type").getAsString())) {
            // Permission-based authentication using Minecraft username
            Session session = handleMinecraftAuth(username, ipAddress, userAgent);

            if (session == null) {
                sendJsonResponse(exchange, 403, createErrorResponse("You don't have permission to access the dashboard"));
                return;
            }

            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("sessionId", session.getSessionId());
            response.addProperty("authType", "minecraft");
            response.add("session", session.toJson());

            // Get user details
            User user = authManager.getUser(session.getUserId());
            if (user != null) {
                response.add("user", user.toJson());
            }

            // Set session cookie
            exchange.getResponseHeaders().add("Set-Cookie",
                "sessionId=" + session.getSessionId() + "; Path=/; HttpOnly; SameSite=Lax; Max-Age=86400");

            sendJsonResponse(exchange, 200, response);
            return;
        }

        // Traditional password-based authentication
        if (!request.has("password")) {
            sendJsonResponse(exchange, 400, createErrorResponse("Missing password"));
            return;
        }

        String password = request.get("password").getAsString();

        Session session = authManager.authenticate(username, password, ipAddress, userAgent);
        if (com.zerog.neoessentials.config.ConfigManager.isDebugModeEnabled()) {
            if (session != null) {
                LOGGER.debug("Session created for user '{}': sessionId={}, requiresPasswordChange={}",
                    username, session.getSessionId(), session.requiresPasswordChange());
            } else {
                LOGGER.debug("Login failed for user '{}': no session created", username);
            }
        }
        
        if (session == null) {
            sendJsonResponse(exchange, 401, createErrorResponse("Invalid credentials or account locked"));
            return;
        }
        
        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.addProperty("sessionId", session.getSessionId());
        response.addProperty("authType", "password");
        response.add("session", session.toJson());
        
        // Get user details
        User user = authManager.getUser(session.getUserId());
        if (user != null) {
            response.add("user", user.toJson());
        }
        
        // Set session cookie
        exchange.getResponseHeaders().add("Set-Cookie", 
            "sessionId=" + session.getSessionId() + "; Path=/; HttpOnly; SameSite=Lax; Max-Age=86400");
        
        sendJsonResponse(exchange, 200, response);
    }
    
    /**
     * Handle Minecraft permission-based authentication
     * Checks if the player has the required permission to access the dashboard
     * Works for both online and offline players (uses permission system UUID lookup)
     */
    private Session handleMinecraftAuth(String minecraftUsername, String ipAddress, String userAgent) {
        try {
            // Get server instance from DashboardAPI
            net.minecraft.server.MinecraftServer server = com.zerog.neoessentials.webdashboard.DashboardAPI.getInstance().getServer();
            if (server == null) {
                LOGGER.error("Cannot authenticate: Server instance not available");
                return null;
            }

            UUID playerUuid = null;

            // Try to get player UUID from various sources
            // 1. Try server's profile cache (for players who have logged in)
            com.mojang.authlib.GameProfile profile = server.getProfileCache().get(minecraftUsername).orElse(null);
            if (profile != null) {
                playerUuid = profile.getId();
                LOGGER.debug("Found player UUID from server profile cache: {}", playerUuid);
            }

            // 2. Try to get from internal permission system (might have offline data)
            if (playerUuid == null) {
                com.zerog.neoessentials.permissions.PermissionManager permManager =
                    com.zerog.neoessentials.api.permissions.PermissionAPI.getManager();

                if (permManager != null) {
                    // Check if we have a user with this username in our system
                    for (com.zerog.neoessentials.permissions.PermissionUser permUser : permManager.getUsers()) {
                        // Try to get username from cache
                        var cachedProfile = server.getProfileCache().get(permUser.getUuid()).orElse(null);
                        if (cachedProfile != null && cachedProfile.getName().equalsIgnoreCase(minecraftUsername)) {
                            playerUuid = permUser.getUuid();
                            LOGGER.debug("Found player UUID from permission system: {}", playerUuid);
                            break;
                        }
                    }
                }
            }

            // 3. Try to get from LuckPerms if available
            if (playerUuid == null && com.zerog.neoessentials.api.permissions.PermissionAPI.isUsingExternal()) {
                try {
                    com.zerog.neoessentials.permissions.ExternalPermissionAdapter adapter =
                        com.zerog.neoessentials.api.permissions.PermissionAPI.getExternalAdapter();

                    if (adapter instanceof com.zerog.neoessentials.permissions.LuckPermsAdapter) {
                        // Try to get UUID from LuckPerms user manager
                        net.luckperms.api.LuckPerms luckPerms = net.luckperms.api.LuckPermsProvider.get();
                        net.luckperms.api.model.user.User lpUser = luckPerms.getUserManager().getUser(minecraftUsername);

                        if (lpUser != null) {
                            playerUuid = lpUser.getUniqueId();
                            LOGGER.debug("Found player UUID from LuckPerms: {}", playerUuid);
                        }
                    }
                } catch (Exception e) {
                    LOGGER.debug("Could not get UUID from LuckPerms: {}", e.getMessage());
                }
            }

            // 4. Try Mojang API as last resort (requires internet connection)
            if (playerUuid == null) {
                try {
                    LOGGER.info("Attempting to fetch UUID from Mojang API for username: {}", minecraftUsername);
                    playerUuid = fetchUuidFromMojangAPI(minecraftUsername);
                    if (playerUuid != null) {
                        LOGGER.info("Retrieved UUID from Mojang API: {}", playerUuid);
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to fetch UUID from Mojang API: {}", e.getMessage());
                }
            }

            if (playerUuid == null) {
                LOGGER.warn("Could not find UUID for player: {} - They may have never joined the server and are not in any permission system", minecraftUsername);
                return null;
            }

            // Check if player has dashboard access permission (works offline via permission systems)
            boolean hasAccess = com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(
                playerUuid, "neoessentials.dashboard.access");

            if (!hasAccess) {
                LOGGER.warn("Player {} (UUID: {}) does not have dashboard access permission", minecraftUsername, playerUuid);
                return null;
            }

            // Determine user role based on permissions (works offline)
            User.Role role = User.Role.VIEWER;
            if (com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(
                    playerUuid, "neoessentials.dashboard.admin")) {
                role = User.Role.ADMIN;
            } else if (com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(
                    playerUuid, "neoessentials.dashboard.moderator")) {
                role = User.Role.MODERATOR;
            }

            // Get or create dashboard user
            AuthenticationManager authManager = AuthenticationManager.getInstance();
            User user = authManager.getUserByUsername(minecraftUsername);

            if (user == null) {
                // Auto-create user for Minecraft authentication using createUser method
                // Use a random password since Minecraft auth doesn't use passwords
                String randomPassword = UUID.randomUUID().toString();
                user = authManager.createUser(minecraftUsername, randomPassword, playerUuid.toString() + "@minecraft", role);
                LOGGER.info("Auto-created dashboard user for Minecraft player: {} (UUID: {}, Role: {})", minecraftUsername, playerUuid, role);
            } else {
                // Update existing user's role if permissions changed
                if (user.getRole() != role) {
                    user.setRole(role);
                    authManager.saveUsers();
                    LOGGER.info("Updated dashboard role for {} (UUID: {}): {}", minecraftUsername, playerUuid, role);
                }
            }

            // Create session
            Session session = authManager.createSession(user.getId(), ipAddress, userAgent);
            LOGGER.info("Minecraft player {} (UUID: {}) authenticated to dashboard with role: {} (Offline-capable)", minecraftUsername, playerUuid, role);

            return session;

        } catch (Exception e) {
            LOGGER.error("Error during Minecraft authentication for {}: {}", minecraftUsername, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Fetch player UUID from Mojang API
     * Used as fallback when player is not in local caches
     */
    private UUID fetchUuidFromMojangAPI(String username) {
        try {
            java.net.URL url = new java.net.URL("https://api.mojang.com/users/profiles/minecraft/" + username);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                // Parse JSON response
                com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(response.toString()).getAsJsonObject();
                String uuidString = json.get("id").getAsString();

                // Convert UUID string to proper UUID format (add dashes)
                String formattedUuid = uuidString.replaceFirst(
                    "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
                    "$1-$2-$3-$4-$5"
                );

                return UUID.fromString(formattedUuid);
            } else if (responseCode == 204 || responseCode == 404) {
                LOGGER.debug("Player '{}' not found in Mojang database", username);
                return null;
            } else {
                LOGGER.warn("Mojang API returned unexpected status code: {}", responseCode);
                return null;
            }
        } catch (Exception e) {
            LOGGER.debug("Error fetching UUID from Mojang API: {}", e.getMessage());
            return null;
        }
    }

    /**
     * GET /api/auth/discord?username=<minecraftUsername>
     * Authenticate using Discord account linked via Simple Discord Link
     */
    private void handleDiscordAuth(HttpExchange exchange) throws IOException {
        // Parse query parameters
        String query = exchange.getRequestURI().getQuery();
        if (query == null || !query.contains("username=")) {
            sendJsonResponse(exchange, 400, createErrorResponse("Missing username parameter"));
            return;
        }
        
        String minecraftUsername = null;
        for (String param : query.split("&")) {
            if (param.startsWith("username=")) {
                minecraftUsername = param.substring("username=".length());
                break;
            }
        }
        
        if (minecraftUsername == null || minecraftUsername.isEmpty()) {
            sendJsonResponse(exchange, 400, createErrorResponse("Invalid username"));
            return;
        }
        
        // Load Discord auth config
        DiscordAuthConfig discordConfig = DiscordAuthConfig.load();
        
        // Check if Discord auth is enabled
        if (!discordConfig.isEnabled()) {
            sendJsonResponse(exchange, 403, createErrorResponse("Discord authentication is disabled"));
            return;
        }
        
        // Get Discord auth provider
        DiscordAuthProvider discordProvider = DiscordAuthProvider.getInstance();
        
        // Check if SDLink is available
        if (!discordProvider.isAvailable()) {
            sendJsonResponse(exchange, 503, createErrorResponse(
                "Discord authentication unavailable. Simple Discord Link mod is required."));
            return;
        }
        
        // Get linked Discord account
        DiscordUser discordUser = discordProvider.getLinkedAccount(minecraftUsername);
        
        if (discordUser == null || !discordUser.isLinked()) {
            sendJsonResponse(exchange, 404, createErrorResponse(
                "No Discord account linked. Please link your account using /discord link in-game."));
            return;
        }
        
        // Check if user is blacklisted
        if (discordConfig.isBlacklisted(discordUser.getDiscordId())) {
            sendJsonResponse(exchange, 403, createErrorResponse("Access denied"));
            LOGGER.warn("Blacklisted Discord user attempted login: {} ({})", 
                discordUser.getDiscordUsername(), discordUser.getDiscordId());
            return;
        }
        
        // Check whitelist (if configured)
        if (!discordConfig.passesWhitelist(discordUser.getDiscordRoles())) {
            sendJsonResponse(exchange, 403, createErrorResponse(
                "You do not have the required Discord role to access the dashboard"));
            LOGGER.warn("Discord user without required role attempted login: {} (roles: {})", 
                discordUser.getDiscordUsername(), discordUser.getDiscordRoles());
            return;
        }
        
        // Map Discord roles to Dashboard role
        User.Role dashboardRole = discordConfig.getHighestRole(discordUser.getDiscordRoles());
        
        // Get or create dashboard user
        AuthenticationManager authManager = AuthenticationManager.getInstance();
        User user = authManager.getUserByUsername(minecraftUsername);
        
        if (user == null) {
            // Auto-register if enabled
            if (!discordConfig.allowsAutoRegistration()) {
                sendJsonResponse(exchange, 403, createErrorResponse(
                    "Account not found and auto-registration is disabled"));
                return;
            }
            
            // Create new user with Discord role
            String email = discordUser.getDiscordId() + "@discord.link"; // Placeholder email
            user = authManager.createUser(minecraftUsername, 
                UUID.randomUUID().toString(), // Random password (won't be used)
                email, dashboardRole);
            
            LOGGER.info("Auto-created dashboard user from Discord: {} with role {}", 
                minecraftUsername, dashboardRole);
        } else {
            // Always update existing user's role to match Discord role
            if (dashboardRole.ordinal() != user.getRole().ordinal()) {
                user.setRole(dashboardRole);
                LOGGER.info("Updated user {} role to {} based on Discord roles", 
                    minecraftUsername, dashboardRole);
            }
        }
        
        // Create session
        String ipAddress = exchange.getRemoteAddress().getAddress().getHostAddress();
        String userAgent = "Discord-" + discordUser.getDiscordUsername();
        
        Session session = authManager.createSession(user.getId(), ipAddress, userAgent);
        
        // Build response
        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.addProperty("sessionId", session.getSessionId());
        response.add("session", session.toJson());
        
        // Add user details
        JsonObject userJson = user.toJson();
        userJson.addProperty("discordId", discordUser.getDiscordId());
        userJson.addProperty("discordUsername", discordUser.getDiscordUsername());
        
        JsonArray discordRolesArray = new JsonArray();
        discordUser.getDiscordRoles().forEach(discordRolesArray::add);
        userJson.add("discordRoles", discordRolesArray);
        
        response.add("user", userJson);
        
        LOGGER.info("Discord authentication successful: {} (Discord: {}, Role: {})", 
            minecraftUsername, discordUser.getDiscordUsername(), dashboardRole);
        
        // Set session cookie
        exchange.getResponseHeaders().add("Set-Cookie", 
            "sessionId=" + session.getSessionId() + "; Path=/; HttpOnly; SameSite=Lax; Max-Age=86400");
        
        sendJsonResponse(exchange, 200, response);
    }
    
    /**
     * POST /api/auth/logout
     * Headers: Authorization: Bearer <sessionId>
     */
    private void handleLogout(HttpExchange exchange) throws IOException {
        String sessionId = extractSessionId(exchange);
        if (sessionId == null) {
            sendJsonResponse(exchange, 401, createErrorResponse("Not authenticated"));
            return;
        }
        
        AuthenticationManager authManager = AuthenticationManager.getInstance();
        authManager.logout(sessionId);
        
        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.addProperty("message", "Logged out successfully");
        
        sendJsonResponse(exchange, 200, response);
    }
    
    /**
     * GET /api/auth/validate
     * Headers: Authorization: Bearer <sessionId>
     */
    private void handleValidate(HttpExchange exchange) throws IOException {
        String sessionId = extractSessionId(exchange);
        if (sessionId == null) {
            sendJsonResponse(exchange, 401, createErrorResponse("Not authenticated"));
            return;
        }
        
        AuthenticationManager authManager = AuthenticationManager.getInstance();
        Session session = authManager.validateSession(sessionId);
        
        if (session == null) {
            sendJsonResponse(exchange, 401, createErrorResponse("Invalid or expired session"));
            return;
        }
        
        JsonObject response = new JsonObject();
        response.addProperty("valid", true);
        response.add("session", session.toJson());
        
        // Get user details
        User user = authManager.getUser(session.getUserId());
        if (user != null) {
            response.add("user", user.toJson());
        }
        
        sendJsonResponse(exchange, 200, response);
    }
    
    /**
     * GET /api/auth/users
     * Requires ADMIN role
     */
    private void handleGetUsers(HttpExchange exchange) throws IOException {
        String sessionId = extractSessionId(exchange);
        if (!requireAdmin(sessionId)) {
            sendJsonResponse(exchange, 403, createErrorResponse("Admin access required"));
            return;
        }
        
        AuthenticationManager authManager = AuthenticationManager.getInstance();
        JsonObject response = new JsonObject();
        
        JsonArray usersArray = new JsonArray();
        authManager.getAllUsers().forEach(user -> usersArray.add(user.toJson()));
        
        response.add("users", usersArray);
        response.addProperty("count", usersArray.size());
        
        sendJsonResponse(exchange, 200, response);
    }
    
    /**
     * POST /api/auth/users
     * Body: {"username": "...", "password": "...", "email": "...", "role": "..."}
     * Requires ADMIN role
     */
    private void handleCreateUser(HttpExchange exchange) throws IOException {
        String sessionId = extractSessionId(exchange);
        if (!requireAdmin(sessionId)) {
            sendJsonResponse(exchange, 403, createErrorResponse("Admin access required"));
            return;
        }
        
        String requestBody = readRequestBody(exchange);
        JsonObject request = GSON.fromJson(requestBody, JsonObject.class);
        
        if (!request.has("username") || !request.has("password")) {
            sendJsonResponse(exchange, 400, createErrorResponse("Missing username or password"));
            return;
        }
        
        String username = request.get("username").getAsString();
        String password = request.get("password").getAsString();
        String email = request.has("email") ? request.get("email").getAsString() : null;
        User.Role role = request.has("role") ? 
            User.Role.valueOf(request.get("role").getAsString()) : User.Role.VIEWER;
        
        try {
            AuthenticationManager authManager = AuthenticationManager.getInstance();
            User user = authManager.createUser(username, password, email, role);
            
            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("message", "User created successfully");
            response.add("user", user.toJson());
            
            sendJsonResponse(exchange, 201, response);
        } catch (IllegalArgumentException e) {
            sendJsonResponse(exchange, 400, createErrorResponse(e.getMessage()));
        }
    }
    
    /**
     * PUT /api/auth/users/{userId}
     * Body: {"password": "...", "email": "...", "role": "...", "enabled": true}
     * Requires ADMIN role
     */
    private void handleUpdateUser(HttpExchange exchange) throws IOException {
        String sessionId = extractSessionId(exchange);
        if (!requireAdmin(sessionId)) {
            sendJsonResponse(exchange, 403, createErrorResponse("Admin access required"));
            return;
        }
        
        // Extract user ID from path
        String path = exchange.getRequestURI().getPath();
        String userId = path.substring(path.lastIndexOf('/') + 1);
        
        String requestBody = readRequestBody(exchange);
        JsonObject request = GSON.fromJson(requestBody, JsonObject.class);
        
        AuthenticationManager authManager = AuthenticationManager.getInstance();
        User user = authManager.getUser(userId);
        
        if (user == null) {
            sendJsonResponse(exchange, 404, createErrorResponse("User not found"));
            return;
        }
        
        try {
            // Update password if provided
            if (request.has("password")) {
                authManager.updatePassword(userId, request.get("password").getAsString());
            }
            
            // Update role if provided
            if (request.has("role")) {
                User.Role newRole = User.Role.valueOf(request.get("role").getAsString());
                authManager.updateUserRole(userId, newRole);
            }
            
            // Update enabled status if provided
            if (request.has("enabled")) {
                authManager.setUserEnabled(userId, request.get("enabled").getAsBoolean());
            }
            
            // Update email if provided
            if (request.has("email")) {
                user.setEmail(request.get("email").getAsString());
            }
            
            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("message", "User updated successfully");
            response.add("user", user.toJson());
            
            sendJsonResponse(exchange, 200, response);
        } catch (IllegalArgumentException e) {
            sendJsonResponse(exchange, 400, createErrorResponse(e.getMessage()));
        }
    }
    
    /**
     * DELETE /api/auth/users/{userId}
     * Requires ADMIN role
     */
    private void handleDeleteUser(HttpExchange exchange) throws IOException {
        String sessionId = extractSessionId(exchange);
        if (!requireAdmin(sessionId)) {
            sendJsonResponse(exchange, 403, createErrorResponse("Admin access required"));
            return;
        }
        
        // Extract user ID from path
        String path = exchange.getRequestURI().getPath();
        String userId = path.substring(path.lastIndexOf('/') + 1);
        
        try {
            AuthenticationManager authManager = AuthenticationManager.getInstance();
            authManager.deleteUser(userId);
            
            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("message", "User deleted successfully");
            
            sendJsonResponse(exchange, 200, response);
        } catch (IllegalArgumentException e) {
            sendJsonResponse(exchange, 404, createErrorResponse(e.getMessage()));
        }
    }
    
    /**
     * GET /api/auth/sessions
     * Requires ADMIN role
     */
    private void handleGetSessions(HttpExchange exchange) throws IOException {
        String sessionId = extractSessionId(exchange);
        if (!requireAdmin(sessionId)) {
            sendJsonResponse(exchange, 403, createErrorResponse("Admin access required"));
            return;
        }
        
        AuthenticationManager authManager = AuthenticationManager.getInstance();
        JsonObject response = new JsonObject();
        
        JsonArray sessionsArray = new JsonArray();
        authManager.getActiveSessions().forEach(session -> sessionsArray.add(session.toJson()));
        
        response.add("sessions", sessionsArray);
        response.addProperty("count", sessionsArray.size());
        
        sendJsonResponse(exchange, 200, response);
    }
    
    /**
     * GET /api/auth/session
     * Get current session information (from cookie)
     */
    private void handleGetCurrentSession(HttpExchange exchange) throws IOException {
        // Debug logging for session cookie
        if (com.zerog.neoessentials.config.ConfigManager.isDebugModeEnabled()) {
            String cookieHeader = exchange.getRequestHeaders().getFirst("Cookie");
            LOGGER.debug("handleGetCurrentSession: Raw Cookie header: {}", cookieHeader);
        }
        String sessionId = getSessionIdFromCookie(exchange);
        if (com.zerog.neoessentials.config.ConfigManager.isDebugModeEnabled()) {
            LOGGER.debug("handleGetCurrentSession: Extracted sessionId: {}", sessionId);
        }
        
        // Try to get session ID from cookie
        if (sessionId == null) {
            sendJsonResponse(exchange, 401, createErrorResponse("No active session"));
            return;
        }

        AuthenticationManager authManager = AuthenticationManager.getInstance();
        Session session = authManager.validateSession(sessionId);

        if (session == null) {
            sendJsonResponse(exchange, 401, createErrorResponse("Invalid or expired session"));
            return;
        }

        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.add("session", session.toJson());

        // Get user details
        User user = authManager.getUser(session.getUserId());
        if (user != null) {
            response.add("user", user.toJson());
        }

        sendJsonResponse(exchange, 200, response);
    }
    
    /**
     * Get session ID from cookie header
     */
    private String getSessionIdFromCookie(HttpExchange exchange) {
        String cookieHeader = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookieHeader == null) {
            return null;
        }
        
        String[] cookies = cookieHeader.split(";");
        for (String cookie : cookies) {
            String[] parts = cookie.trim().split("=", 2);
            if (parts.length == 2 && "sessionId".equals(parts[0])) {
                return parts[1];
            }
        }
        
        return null;
    }
    
    /**
     * Extract session ID from Authorization header
     */
    private String extractSessionId(HttpExchange exchange) {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
    
    /**
     * Check if session has ADMIN role
     */
    private boolean requireAdmin(String sessionId) {
        if (sessionId == null) {
            return false;
        }
        
        AuthenticationManager authManager = AuthenticationManager.getInstance();
        Session session = authManager.validateSession(sessionId);
        
        return session != null && session.getRole() == User.Role.ADMIN;
    }
    
    /**
     * Read request body
     */
    private String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }
    
    /**
     * Send JSON response
     */
    private void sendJsonResponse(HttpExchange exchange, int statusCode, JsonObject json) throws IOException {
        byte[] response = GSON.toJson(json).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }
    
    /**
     * Create error response JSON
     */
    private JsonObject createErrorResponse(String message) {
        JsonObject error = new JsonObject();
        error.addProperty("error", message);
        error.addProperty("timestamp", System.currentTimeMillis());
        return error;
    }
}
