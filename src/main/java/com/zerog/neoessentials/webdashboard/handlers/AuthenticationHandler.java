package com.zerog.neoessentials.webdashboard.handlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import com.zerog.neoessentials.webdashboard.security.AuthenticationManager;
import com.zerog.neoessentials.webdashboard.security.DashboardLoginRequestManager;
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
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

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
            } else if ("GET".equals(method) && path.endsWith("/discord/status")) {
                handleDiscordStatus(exchange);
            } else if ("POST".equals(method) && path.endsWith("/login-request")) {
                handleLoginRequestStart(exchange);
            } else if ("GET".equals(method) && path.endsWith("/login-request/status")) {
                handleLoginRequestStatus(exchange);
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
            } else if ("POST".equals(method) && path.endsWith("/link-minecraft/start")) {
                handleLinkMinecraftStart(exchange);
            } else if ("GET".equals(method) && path.endsWith("/link-minecraft/status")) {
                handleLinkMinecraftStatus(exchange);
            } else if ("POST".equals(method) && path.endsWith("/unlink-minecraft")) {
                handleUnlinkMinecraft(exchange);
            } else if ("GET".equals(method) && path.endsWith("/discord-status")) {
                handleAccountDiscordStatus(exchange);
            } else {
                sendJsonResponse(exchange, 400, createErrorResponse("Invalid endpoint"));
            }
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "Error handling authentication request", e);
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
        // Verify old password (via verifyPassword, not a raw hash comparison — hashPassword()
        // salts with a fresh random salt every call, so comparing its output directly would
        // never match the stored hash even for the correct password)
        if (!authManager.verifyPassword(oldPassword, user.getPasswordHash())) {
            sendJsonResponse(exchange, 403, createErrorResponse("Old password is incorrect"));
            return;
        }
        try {
            authManager.updatePassword(userId, newPassword);
            user.setRequiresPasswordChange(false);
            user.setTempPassword(false);
            authManager.saveUsers();
            // Debug logging for user and session state after password change
            NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Password changed for user '{}': requiresPasswordChange={}, isTempPassword={}",
                user.getUsername(), user.requiresPasswordChange(), user.isTempPassword());
            Session sessionObj = authManager.validateSession(sessionId);
            NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Session state after password change for '{}': active={}, requiresPasswordChange={}",
                user.getUsername(), sessionObj != null ? sessionObj.isActive() : "null", sessionObj != null ? sessionObj.requiresPasswordChange() : "null");
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
     * Resolves the dashboard-username "owner key" these four Minecraft-link/Discord-status
     * endpoints act on — deliberately a plain username string, not a resolved mod {@link User},
     * since the external Laravel dashboard has its own separate user accounts that may have no
     * corresponding mod-side dashboard account at all (see
     * {@link com.zerog.neoessentials.webdashboard.security.MinecraftAccountLinkManager}'s class
     * doc). Two distinct callers use this:
     * <ul>
     *   <li>The bundled internal dashboard (same-origin SPA): resolves via the session cookie,
     *       exactly like {@link #handleChangePassword} — always acts on the calling user, key
     *       is that user's own username.</li>
     *   <li>The external Laravel dashboard (server-to-server): authenticates with its paired
     *       API key (Bearer {@code neo_...}, see {@link com.zerog.neoessentials.webdashboard.security.ApiKeyManager})
     *       and must pass the target username explicitly ({@code usernameParam}) — its own
     *       Laravel user's {@code mod_username}, which may or may not match a real mod
     *       dashboard account.</li>
     * </ul>
     * Writes the error response itself and returns null on any failure.
     */
    private String resolveOwnerKey(HttpExchange exchange, String usernameParam) throws IOException {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        String bearer = (authHeader != null && authHeader.startsWith("Bearer ")) ? authHeader.substring(7) : null;

        if (bearer != null && com.zerog.neoessentials.webdashboard.security.ApiKeyManager.getInstance().validate(bearer) != null) {
            if (usernameParam == null || usernameParam.isBlank()) {
                sendJsonResponse(exchange, 400, createErrorResponse("Missing username — required when authenticating with an API key."));
                return null;
            }
            return usernameParam;
        }

        String sessionId = getSessionIdFromCookie(exchange);
        if (sessionId == null) {
            sendJsonResponse(exchange, 401, createErrorResponse("No active session"));
            return null;
        }
        AuthenticationManager authManager = AuthenticationManager.getInstance();
        Session session = authManager.validateSession(sessionId);
        if (session == null) {
            sendJsonResponse(exchange, 401, createErrorResponse("Invalid or expired session"));
            return null;
        }
        User user = authManager.getUser(session.getUserId());
        if (user == null) {
            sendJsonResponse(exchange, 404, createErrorResponse("User not found"));
            return null;
        }
        return user.getUsername();
    }

    private String queryParam(HttpExchange exchange, String name) {
        String query = exchange.getRequestURI().getQuery();
        if (query == null) return null;
        for (String param : query.split("&")) {
            if (param.startsWith(name + "=")) {
                return java.net.URLDecoder.decode(param.substring(name.length() + 1), StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    /** POST /api/auth/link-minecraft/start — generates a code for /linkaccount <code> in-game. */
    private void handleLinkMinecraftStart(HttpExchange exchange) throws IOException {
        String requestBody = readRequestBody(exchange);
        JsonObject request = requestBody != null && !requestBody.isBlank() ? GSON.fromJson(requestBody, JsonObject.class) : new JsonObject();
        String usernameParam = request.has("username") ? request.get("username").getAsString() : null;

        String ownerKey = resolveOwnerKey(exchange, usernameParam);
        if (ownerKey == null) return;

        com.zerog.neoessentials.webdashboard.security.MinecraftAccountLinkManager linkManager =
            com.zerog.neoessentials.webdashboard.security.MinecraftAccountLinkManager.getInstance();
        String code = linkManager.startLink(ownerKey);
        if (code == null) {
            sendJsonResponse(exchange, 400, createErrorResponse("This account already has a linked Minecraft account — unlink it first."));
            return;
        }

        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.addProperty("code", code);
        response.addProperty("expiresAt", linkManager.peekExpiry(code));
        sendJsonResponse(exchange, 200, response);
    }

    /** GET /api/auth/link-minecraft/status — polled by the Settings page while a code is showing. */
    private void handleLinkMinecraftStatus(HttpExchange exchange) throws IOException {
        String ownerKey = resolveOwnerKey(exchange, queryParam(exchange, "username"));
        if (ownerKey == null) return;

        com.zerog.neoessentials.webdashboard.security.MinecraftAccountLinkManager linkManager =
            com.zerog.neoessentials.webdashboard.security.MinecraftAccountLinkManager.getInstance();
        String mcUuid = linkManager.getLinkedUuid(ownerKey);

        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.addProperty("linked", mcUuid != null);
        response.addProperty("mcUuid", mcUuid);
        response.addProperty("mcUsername", linkManager.getLinkedUsername(ownerKey));
        sendJsonResponse(exchange, 200, response);
    }

    /** POST /api/auth/unlink-minecraft — self-service, no code needed. */
    private void handleUnlinkMinecraft(HttpExchange exchange) throws IOException {
        String requestBody = readRequestBody(exchange);
        JsonObject request = requestBody != null && !requestBody.isBlank() ? GSON.fromJson(requestBody, JsonObject.class) : new JsonObject();
        String usernameParam = request.has("username") ? request.get("username").getAsString() : null;

        String ownerKey = resolveOwnerKey(exchange, usernameParam);
        if (ownerKey == null) return;

        com.zerog.neoessentials.webdashboard.security.MinecraftAccountLinkManager.getInstance().unlink(ownerKey);

        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        sendJsonResponse(exchange, 200, response);
    }

    /**
     * GET /api/auth/discord-status — is the resolved owner's linked Minecraft account (if any)
     * also linked to Discord via whichever companion bot is installed? Purely informational —
     * this mod never performs Discord OAuth2 itself (see DiscordAuthProvider).
     */
    private void handleAccountDiscordStatus(HttpExchange exchange) throws IOException {
        String ownerKey = resolveOwnerKey(exchange, queryParam(exchange, "username"));
        if (ownerKey == null) return;

        String mcUuid = com.zerog.neoessentials.webdashboard.security.MinecraftAccountLinkManager.getInstance().getLinkedUuid(ownerKey);

        JsonObject response = new JsonObject();
        response.addProperty("success", true);

        if (mcUuid == null) {
            response.addProperty("linked", false);
            sendJsonResponse(exchange, 200, response);
            return;
        }

        DiscordUser discordUser = DiscordAuthProvider.getInstance().getLinkedAccountByUuid(UUID.fromString(mcUuid));
        boolean linked = discordUser != null && discordUser.isLinked();
        response.addProperty("linked", linked);
        if (linked) {
            response.addProperty("discordUsername", discordUser.getDiscordUsername());
        }
        sendJsonResponse(exchange, 200, response);
    }

    /**
     * POST /api/auth/login
     * Body:
     * - {"username": "admin", "password": "password"} - Standard password auth
     * - {"username": "minecraft_name", "type": "minecraft"} - Minecraft permission auth (DEPRECATED - requires online)
     *
     * Supports: password-based, registration-based, and legacy Minecraft auth.
     * Discord-linked login is a separate flow — see handleDiscordAuth (GET /api/auth/discord).
     */
    private void handleLogin(HttpExchange exchange) throws IOException {
        String requestBody = readRequestBody(exchange);
        JsonObject request = GSON.fromJson(requestBody, JsonObject.class);

        String ipAddress = exchange.getRemoteAddress().getAddress().getHostAddress();
        String userAgent = exchange.getRequestHeaders().getFirst("User-Agent");
        AuthenticationManager authManager = AuthenticationManager.getInstance();

        // Validate username
        if (!request.has("username")) {
            sendJsonResponse(exchange, 400, createErrorResponse("Missing username"));
            return;
        }
        
        String username = request.get("username").getAsString();

        // The old "type":"minecraft" branch minted a session straight from this unauthenticated
        // username string with no proof the caller owned the account — see
        // DashboardLoginRequestManager for the safe replacement (POST /api/auth/login-request).

        // Standard password-based authentication
        if (!request.has("password")) {
            sendJsonResponse(exchange, 400, createErrorResponse("Missing password"));
            return;
        }

        String password = request.get("password").getAsString();

        Session session = authManager.authenticate(username, password, ipAddress, userAgent);
        if (session != null) {
            NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Session created for user '{}': requiresPasswordChange={}",
                username, session.requiresPasswordChange());
        } else {
            NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Login failed for user '{}': no session created", username);
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
                NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "Cannot authenticate: Server instance not available");
                return null;
            }

            UUID playerUuid = null;

            // Try to get player UUID from various sources
            // 1. Try server's profile cache (for players who have logged in)
            net.minecraft.server.players.NameAndId profile = server.services().nameToIdCache().get(minecraftUsername).orElse(null);
            if (profile != null) {
                playerUuid = profile.id();
                NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Found player UUID from server profile cache: {}", playerUuid);
            }

            // 2. Try to get from internal permission system (might have offline data)
            if (playerUuid == null) {
                com.zerog.neoessentials.permissions.PermissionManager permManager =
                    com.zerog.neoessentials.api.permissions.PermissionAPI.getManager();

                if (permManager != null) {
                    // Check if we have a user with this username in our system
                    for (com.zerog.neoessentials.permissions.PermissionUser permUser : permManager.getUsers()) {
                        // Try to get username from cache
                        var cachedProfile = server.services().nameToIdCache().get(permUser.getUuid()).orElse(null);
                        if (cachedProfile != null && cachedProfile.name().equalsIgnoreCase(minecraftUsername)) {
                            playerUuid = permUser.getUuid();
                            NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Found player UUID from permission system: {}", playerUuid);
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
                            NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Found player UUID from LuckPerms: {}", playerUuid);
                        }
                    }
                } catch (Exception e) {
                    NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Could not get UUID from LuckPerms: {}", e.getMessage());
                }
            }

            // 4. Try Mojang API as last resort (requires internet connection)
            if (playerUuid == null) {
                try {
                    NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "Attempting to fetch UUID from Mojang API for username: {}", minecraftUsername);
                    playerUuid = fetchUuidFromMojangAPI(minecraftUsername);
                    if (playerUuid != null) {
                        NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "Retrieved UUID from Mojang API: {}", playerUuid);
                    }
                } catch (Exception e) {
                    NeoLog.warn(LOGGER, LogCategory.WEB_DASHBOARD, "Failed to fetch UUID from Mojang API: {}", e.getMessage());
                }
            }

            if (playerUuid == null) {
                NeoLog.warn(LOGGER, LogCategory.WEB_DASHBOARD, "Could not find UUID for player: {} - They may have never joined the server and are not in any permission system", minecraftUsername);
                return null;
            }

            // Check if player has dashboard access permission (works offline via permission systems)
            boolean hasAccess = com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(
                playerUuid, "neoessentials.dashboard.access");

            if (!hasAccess) {
                NeoLog.warn(LOGGER, LogCategory.WEB_DASHBOARD, "Player {} (UUID: {}) does not have dashboard access permission", minecraftUsername, playerUuid);
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
                NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "Auto-created dashboard user for Minecraft player: {} (UUID: {}, Role: {})", minecraftUsername, playerUuid, role);
            } else {
                // Update existing user's role if permissions changed
                if (user.getRole() != role) {
                    user.setRole(role);
                    authManager.saveUsers();
                    NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "Updated dashboard role for {} (UUID: {}): {}", minecraftUsername, playerUuid, role);
                }
            }

            // Create session
            Session session = authManager.createSession(user.getId(), ipAddress, userAgent);
            NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "Minecraft player {} (UUID: {}) authenticated to dashboard with role: {} (Offline-capable)", minecraftUsername, playerUuid, role);

            return session;

        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "Error during Minecraft authentication for " + minecraftUsername, e);
            return null;
        }
    }

    /**
     * GET /api/auth/discord/status
     * Lightweight, no-auth endpoint. Used by the login page to decide whether to show
     * the Discord-linked login option.
     * Response: {
     *   "enabled": true,               // discord_auth.json "enabled" flag
     *   "linkAdapterAvailable": false, // a Discord companion mod (SDLink/Mc2Discord) is loaded and ready
     *   "requiresLinkedAccount": true,
     *   "allowAutoRegistration": true
     * }
     */
    private void handleDiscordStatus(HttpExchange exchange) throws IOException {
        DiscordAuthConfig config = DiscordAuthConfig.load();
        DiscordAuthProvider provider = DiscordAuthProvider.getInstance();

        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.addProperty("enabled", config.isEnabled());
        response.addProperty("linkAdapterAvailable", provider.isAvailable());
        response.addProperty("requiresLinkedAccount", config.requiresLinkedAccount());
        response.addProperty("allowAutoRegistration", config.allowsAutoRegistration());
        sendJsonResponse(exchange, 200, response);
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
                NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Player '{}' not found in Mojang database", username);
                return null;
            } else {
                NeoLog.warn(LOGGER, LogCategory.WEB_DASHBOARD, "Mojang API returned unexpected status code: {}", responseCode);
                return null;
            }
        } catch (Exception e) {
            NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Error fetching UUID from Mojang API: {}", e.getMessage());
            return null;
        }
    }

    /**
     * POST /api/auth/login-request
     * Body: {"username": "<claimed>", "method": "discord"|"minecraft"}
     * Starts a dashboard login attempt for a CLAIMED username — mints no session yet. The
     * browser shows the returned code to the user, who must run /dashboardlogin <code>
     * in-game to prove they actually control that account, then poll login-request/status.
     * Replaces the old GET /api/auth/discord and POST /api/auth/login {"type":"minecraft"}
     * flows, which minted a real session straight from this same unauthenticated username
     * string with no proof of ownership — a full account-impersonation hole.
     */
    private void handleLoginRequestStart(HttpExchange exchange) throws IOException {
        String requestBody = readRequestBody(exchange);
        JsonObject request = GSON.fromJson(requestBody, JsonObject.class);

        if (!request.has("username") || request.get("username").getAsString().isEmpty()) {
            sendJsonResponse(exchange, 400, createErrorResponse("Missing username"));
            return;
        }
        String claimedUsername = request.get("username").getAsString();
        String authMethod = request.has("method") ? request.get("method").getAsString() : "minecraft";
        if (!"discord".equals(authMethod) && !"minecraft".equals(authMethod)) {
            sendJsonResponse(exchange, 400, createErrorResponse("Invalid method"));
            return;
        }

        DashboardLoginRequestManager.StartedRequest started =
            DashboardLoginRequestManager.getInstance().startLoginRequest(claimedUsername, authMethod);

        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.addProperty("code", started.code);
        response.addProperty("requestId", started.requestId);
        response.addProperty("expiresAt", started.expiresAt);
        sendJsonResponse(exchange, 200, response);
    }

    /**
     * GET /api/auth/login-request/status?requestId=<id>
     * Polled by the browser after showing the code from login-request. Only mints and returns
     * a real session once /dashboardlogin <code> has been redeemed in-game for THIS requestId —
     * at which point the identity used below is the in-game-VERIFIED username, never the
     * originally-claimed one from the start call.
     */
    private void handleLoginRequestStatus(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        String requestId = null;
        if (query != null) {
            for (String param : query.split("&")) {
                if (param.startsWith("requestId=")) {
                    requestId = param.substring("requestId=".length());
                    break;
                }
            }
        }
        if (requestId == null || requestId.isEmpty()) {
            sendJsonResponse(exchange, 400, createErrorResponse("Missing requestId"));
            return;
        }

        DashboardLoginRequestManager.PollResult poll =
            DashboardLoginRequestManager.getInstance().pollLoginRequest(requestId);

        if (poll.status == DashboardLoginRequestManager.PollResult.Status.PENDING) {
            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("status", "pending");
            sendJsonResponse(exchange, 200, response);
            return;
        }
        if (poll.status == DashboardLoginRequestManager.PollResult.Status.EXPIRED) {
            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("status", "expired");
            sendJsonResponse(exchange, 200, response);
            return;
        }

        // COMPLETED — mint the session using the verified identity, not anything from the browser.
        String ipAddress = exchange.getRemoteAddress().getAddress().getHostAddress();
        String userAgent = exchange.getRequestHeaders().getFirst("User-Agent");

        VerifiedLoginOutcome outcome = "discord".equals(poll.method)
            ? completeDiscordLogin(poll.minecraftUsername, ipAddress, "Discord-" + poll.minecraftUsername)
            : completeMinecraftLogin(poll.minecraftUsername, ipAddress, userAgent);

        if (outcome.session == null) {
            sendJsonResponse(exchange, outcome.httpStatus, createErrorResponse(outcome.errorMessage));
            return;
        }

        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.addProperty("status", "completed");
        response.addProperty("sessionId", outcome.session.getSessionId());
        response.add("session", outcome.session.toJson());
        response.add("user", outcome.userJson);

        exchange.getResponseHeaders().add("Set-Cookie",
            "sessionId=" + outcome.session.getSessionId() + "; Path=/; HttpOnly; SameSite=Lax; Max-Age=86400");
        sendJsonResponse(exchange, 200, response);
    }

    /** Result of minting a session from an in-game-verified identity. */
    private static class VerifiedLoginOutcome {
        final Session session;
        final JsonObject userJson;
        final int httpStatus;
        final String errorMessage;

        private VerifiedLoginOutcome(Session session, JsonObject userJson, int httpStatus, String errorMessage) {
            this.session = session;
            this.userJson = userJson;
            this.httpStatus = httpStatus;
            this.errorMessage = errorMessage;
        }

        static VerifiedLoginOutcome success(Session session, JsonObject userJson) {
            return new VerifiedLoginOutcome(session, userJson, 200, null);
        }

        static VerifiedLoginOutcome failure(int httpStatus, String errorMessage) {
            return new VerifiedLoginOutcome(null, null, httpStatus, errorMessage);
        }
    }

    /**
     * Authenticate using a Discord account linked via Simple Discord Link — now only ever
     * called with a {@code minecraftUsername} already verified in-game via /dashboardlogin,
     * never a raw unauthenticated query/body param.
     */
    private VerifiedLoginOutcome completeDiscordLogin(String minecraftUsername, String ipAddress, String userAgent) {
        DiscordAuthConfig discordConfig = DiscordAuthConfig.load();
        if (!discordConfig.isEnabled()) {
            return VerifiedLoginOutcome.failure(403, "Discord authentication is disabled");
        }

        DiscordAuthProvider discordProvider = DiscordAuthProvider.getInstance();
        if (!discordProvider.isAvailable()) {
            return VerifiedLoginOutcome.failure(503,
                "Discord authentication unavailable. Install Simple Discord Link, Mc2Discord, or DCIntegration and link your account in-game.");
        }

        DiscordUser discordUser = discordProvider.getLinkedAccount(minecraftUsername);
        if (discordUser == null || !discordUser.isLinked()) {
            return VerifiedLoginOutcome.failure(404,
                "No Discord account linked. Please link your account using /discord link in-game.");
        }

        if (discordConfig.isBlacklisted(discordUser.getDiscordId())) {
            NeoLog.warn(LOGGER, LogCategory.WEB_DASHBOARD, "Blacklisted Discord user attempted login: {} ({})",
                discordUser.getDiscordUsername(), discordUser.getDiscordId());
            return VerifiedLoginOutcome.failure(403, "Access denied");
        }

        if (!discordConfig.passesWhitelist(discordUser.getDiscordRoles())) {
            NeoLog.warn(LOGGER, LogCategory.WEB_DASHBOARD, "Discord user without required role attempted login: {} (roles: {})",
                discordUser.getDiscordUsername(), discordUser.getDiscordRoles());
            return VerifiedLoginOutcome.failure(403, "You do not have the required Discord role to access the dashboard");
        }

        User.Role dashboardRole = discordConfig.getHighestRole(discordUser.getDiscordRoles());

        AuthenticationManager authManager = AuthenticationManager.getInstance();
        User user = authManager.getUserByUsername(minecraftUsername);

        if (user == null) {
            if (!discordConfig.allowsAutoRegistration()) {
                return VerifiedLoginOutcome.failure(403, "Account not found and auto-registration is disabled");
            }
            String email = discordUser.getDiscordId() + "@discord.link"; // Placeholder email
            user = authManager.createUser(minecraftUsername,
                UUID.randomUUID().toString(), // Random password (won't be used)
                email, dashboardRole);
            NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "Auto-created dashboard user from Discord: {} with role {}",
                minecraftUsername, dashboardRole);
        } else if (dashboardRole.ordinal() != user.getRole().ordinal()) {
            user.setRole(dashboardRole);
            NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "Updated user {} role to {} based on Discord roles",
                minecraftUsername, dashboardRole);
        }

        Session session = authManager.createSession(user.getId(), ipAddress, userAgent);

        JsonObject userJson = user.toJson();
        userJson.addProperty("discordId", discordUser.getDiscordId());
        userJson.addProperty("discordUsername", discordUser.getDiscordUsername());
        JsonArray discordRolesArray = new JsonArray();
        discordUser.getDiscordRoles().forEach(discordRolesArray::add);
        userJson.add("discordRoles", discordRolesArray);

        NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "Discord authentication successful: {} (Discord: {}, Role: {})",
            minecraftUsername, discordUser.getDiscordUsername(), dashboardRole);

        return VerifiedLoginOutcome.success(session, userJson);
    }

    /**
     * Legacy Minecraft-permission-based authentication — now only ever called with a
     * {@code minecraftUsername} already verified in-game via /dashboardlogin, never a raw
     * unauthenticated body param.
     */
    private VerifiedLoginOutcome completeMinecraftLogin(String minecraftUsername, String ipAddress, String userAgent) {
        Session session = handleMinecraftAuth(minecraftUsername, ipAddress, userAgent);
        if (session == null) {
            return VerifiedLoginOutcome.failure(403, "You don't have permission to access the dashboard");
        }
        AuthenticationManager authManager = AuthenticationManager.getInstance();
        User user = authManager.getUser(session.getUserId());
        return VerifiedLoginOutcome.success(session, user != null ? user.toJson() : new JsonObject());
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
        response.addProperty("success", true);
        response.addProperty("valid", true);
        // Top-level fields for backwards-compatibility with dashboard.js checkAuthentication()
        response.addProperty("username", session.getUsername());
        response.addProperty("isAdmin", session.getRole() == User.Role.ADMIN);
        response.addProperty("authType", "password");
        response.add("session", session.toJson());

        // Get user details
        User user = authManager.getUser(session.getUserId());
        if (user != null) {
            response.add("user", user.toJson());
            // Only meaningful for User.Role.CUSTOM (see webDashboard.groupPermissions) — empty
            // for the 4 fixed roles, which both frontends still gate purely on isAdmin/role name
            // as before. Lets a CUSTOM-role frontend know which tabs to show without guessing
            // from the role name, using the exact same per-tab check DashboardAPI.withAuth itself
            // enforces server-side (User.hasTabPermission), so this list can never drift from
            // what's actually allowed.
            response.add("tabs", buildCustomTabPermissions(user));
        }

        sendJsonResponse(exchange, 200, response);
    }

    private JsonArray buildCustomTabPermissions(User user) {
        JsonArray tabsArray = new JsonArray();
        if (user.getRole() != User.Role.CUSTOM) return tabsArray;
        for (String tabId : com.zerog.neoessentials.webdashboard.security.DashboardTab.allTabIds()) {
            boolean canRead = user.hasTabPermission(tabId, false);
            boolean canWrite = user.hasTabPermission(tabId, true);
            if (!canRead && !canWrite) continue;
            JsonObject tabObj = new JsonObject();
            tabObj.addProperty("tab", tabId);
            tabObj.addProperty("canRead", canRead);
            tabObj.addProperty("canWrite", canWrite);
            tabsArray.add(tabObj);
        }
        return tabsArray;
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
        String cookieHeader = exchange.getRequestHeaders().getFirst("Cookie");
        NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "handleGetCurrentSession: Cookie header present: {}", cookieHeader != null);
        String sessionId = getSessionIdFromCookie(exchange);
        NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "handleGetCurrentSession: sessionId present: {}", sessionId != null);

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
