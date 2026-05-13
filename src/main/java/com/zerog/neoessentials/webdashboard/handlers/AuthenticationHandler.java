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
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    // ── In-game Discord registration state tracking ───────────────────────────
    // Maps OAuth2 state token → pending registration (minecraft username + expiry)
    private static final java.util.concurrent.ConcurrentHashMap<String, PendingDiscordReg>
        PENDING_DISCORD_REGS = new java.util.concurrent.ConcurrentHashMap<>();

    /** Represents a pending in-game-initiated Discord OAuth2 registration. */
    private static class PendingDiscordReg {
        final String  minecraftUsername;
        final UUID    playerUuid;
        final long    expiresAt;

        PendingDiscordReg(UUID playerUuid, String minecraftUsername) {
            this.playerUuid        = playerUuid;
            this.minecraftUsername = minecraftUsername;
            this.expiresAt         = System.currentTimeMillis() + 5 * 60_000L; // 5 min
        }

        boolean isExpired() { return System.currentTimeMillis() > expiresAt; }
    }

    /**
     * Called by {@code /dashboardregister discord} to create a one-time OAuth2 state
     * that is bound to a specific Minecraft player. Returns the state token string.
     */
    public static String createPendingDiscordRegistration(UUID playerUuid, String minecraftUsername) {
        // Evict stale entries first
        PENDING_DISCORD_REGS.entrySet().removeIf(e -> e.getValue().isExpired());
        String state = UUID.randomUUID().toString().replace("-", "");
        PENDING_DISCORD_REGS.put(state, new PendingDiscordReg(playerUuid, minecraftUsername));
        return state;
    }
    
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
            } else if ("GET".equals(method) && path.endsWith("/discord")) {
                handleDiscordAuth(exchange);
            } else if ("GET".equals(method) && path.contains("/discord/callback")) {
                handleDiscordOAuthCallback(exchange);
            } else if ("GET".equals(method) && path.endsWith("/discord/authorize")) {
                handleDiscordAuthorizeRedirect(exchange);
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
     * Body:
     * - {"username": "admin", "password": "password"} - Standard password auth
     * - {"username": "minecraft_name", "type": "minecraft"} - Minecraft permission auth (DEPRECATED - requires online)
     * - {"discordCode": "oauth_code"} - Discord OAuth (if SDLink is available)
     *
     * Supports: password-based, registration-based, Discord OAuth (with SDLink), and legacy Minecraft auth
     */
    private void handleLogin(HttpExchange exchange) throws IOException {
        String requestBody = readRequestBody(exchange);
        JsonObject request = GSON.fromJson(requestBody, JsonObject.class);
        
        String ipAddress = exchange.getRemoteAddress().getAddress().getHostAddress();
        String userAgent = exchange.getRequestHeaders().getFirst("User-Agent");
        AuthenticationManager authManager = AuthenticationManager.getInstance();

        // Check if this is Discord OAuth authentication
        if (request.has("discordCode")) {
            Session session = handleDiscordOAuth(request.get("discordCode").getAsString(), ipAddress, userAgent);
            if (session == null) {
                sendJsonResponse(exchange, 401, createErrorResponse("Discord authentication failed"));
                return;
            }

            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("sessionId", session.getSessionId());
            response.addProperty("authType", "discord");
            response.add("session", session.toJson());

            User user = authManager.getUser(session.getUserId());
            if (user != null) {
                response.add("user", user.toJson());
            }

            exchange.getResponseHeaders().add("Set-Cookie",
                "sessionId=" + session.getSessionId() + "; Path=/; HttpOnly; SameSite=Lax; Max-Age=86400");

            sendJsonResponse(exchange, 200, response);
            return;
        }

        // Validate username
        if (!request.has("username")) {
            sendJsonResponse(exchange, 400, createErrorResponse("Missing username"));
            return;
        }
        
        String username = request.get("username").getAsString();

        // LEGACY: Check if this is permission-based (Minecraft) authentication (DEPRECATED)
        // This requires the player to be online, use registration-based auth instead
        if (request.has("type") && "minecraft".equals(request.get("type").getAsString())) {
            LOGGER.warn("Legacy Minecraft auth used by {}, this method is deprecated - use registration-based auth instead", username);
            Session session = handleMinecraftAuth(username, ipAddress, userAgent);

            if (session == null) {
                sendJsonResponse(exchange, 403, createErrorResponse("You don't have permission to access the dashboard or are not online"));
                return;
            }

            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("sessionId", session.getSessionId());
            response.addProperty("authType", "minecraft");
            response.add("session", session.toJson());

            User user = authManager.getUser(session.getUserId());
            if (user != null) {
                response.add("user", user.toJson());
            }

            exchange.getResponseHeaders().add("Set-Cookie",
                "sessionId=" + session.getSessionId() + "; Path=/; HttpOnly; SameSite=Lax; Max-Age=86400");

            sendJsonResponse(exchange, 200, response);
            return;
        }

        // Standard password-based authentication
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
     * Handle Discord OAuth2 flow — called when POST /api/auth/login contains {"discordCode": "..."}
     * Exchanges the authorization code for an access token, fetches the Discord user,
     * finds their linked Minecraft account via SDLink, then creates a dashboard session.
     *
     * Flow:
     *   1. POST to Discord token endpoint with code + client credentials
     *   2. GET /users/@me with the access token → Discord user ID + username
     *   3. GET /users/@me/guilds/{guildId}/member with the access token → guild roles
     *   4. Look up linked Minecraft account via DiscordAuthProvider.getLinkedAccountByDiscordId()
     *   5. Apply role mapping, get/create dashboard user, create session
     */
    private Session handleDiscordOAuth(String oauthCode, String ipAddress, String userAgent) {
        return handleDiscordOAuth(oauthCode, ipAddress, userAgent, null);
    }

    /**
     * Core Discord OAuth2 flow.
     *
     * @param forcedMinecraftUsername When non-null (set by in-game {@code /dashboardregister discord}),
     *                                the account will be linked to this Minecraft username regardless
     *                                of whether SDLink is installed.
     */
    private Session handleDiscordOAuth(String oauthCode, String ipAddress, String userAgent,
                                       String forcedMinecraftUsername) {
        DiscordAuthConfig discordConfig = DiscordAuthConfig.load();

        if (!discordConfig.isEnabled()) {
            LOGGER.warn("Discord OAuth2 authentication requested but Discord auth is disabled");
            return null;
        }

        if (!discordConfig.isOauth2Configured()) {
            LOGGER.warn("Discord OAuth2 authentication requested but clientId/clientSecret are not configured in discord_auth.json");
            return null;
        }

        try {
            // ── Step 1: Exchange authorization code for access token ──────────
            String tokenJson = exchangeDiscordCode(
                oauthCode,
                discordConfig.getOauth2ClientId(),
                discordConfig.getOauth2ClientSecret(),
                discordConfig.getOauth2RedirectUri()
            );

            if (tokenJson == null) {
                LOGGER.error("Discord token exchange failed — no response");
                return null;
            }

            com.google.gson.JsonObject tokenResponse = com.google.gson.JsonParser
                .parseString(tokenJson).getAsJsonObject();

            if (tokenResponse.has("error")) {
                LOGGER.error("Discord token exchange error: {} — {}",
                    tokenResponse.get("error").getAsString(),
                    tokenResponse.has("error_description")
                        ? tokenResponse.get("error_description").getAsString() : "");
                return null;
            }

            String accessToken = tokenResponse.get("access_token").getAsString();

            // ── Step 2: Fetch Discord user info (/users/@me) ─────────────────
            String userJson = fetchDiscordApi("https://discord.com/api/v10/users/@me", accessToken);
            if (userJson == null) {
                LOGGER.error("Failed to fetch Discord user info");
                return null;
            }

            com.google.gson.JsonObject discordUserObj = com.google.gson.JsonParser
                .parseString(userJson).getAsJsonObject();

            String discordId = discordUserObj.get("id").getAsString();
            String discordUsername = discordUserObj.has("global_name") && !discordUserObj.get("global_name").isJsonNull()
                ? discordUserObj.get("global_name").getAsString()
                : discordUserObj.get("username").getAsString();

            LOGGER.info("Discord OAuth2: authenticated Discord user {} (ID: {})", discordUsername, discordId);

            // ── Step 3: Check blacklist before going further ──────────────────
            if (discordConfig.isBlacklisted(discordId)) {
                LOGGER.warn("Blacklisted Discord user attempted OAuth2 login: {} ({})", discordUsername, discordId);
                return null;
            }

            // ── Step 4: Fetch guild roles via /users/@me/guilds/{id}/member ──
            // Roles are fetched via SDLink's cache first; OAuth fallback used when bot isn't ready
            DiscordAuthProvider discordProvider = DiscordAuthProvider.getInstance();
            java.util.List<String> discordRoles;

            if (discordProvider.isAvailable()) {
                // Preferred: SDLink has the member cached with role IDs
                discordRoles = discordProvider.getDiscordRoles(discordId);
                LOGGER.debug("Fetched {} Discord roles from SDLink cache for {}", discordRoles.size(), discordId);
            } else {
                // Fallback: parse roles from token scopes if guild member endpoint available
                discordRoles = new java.util.ArrayList<>();
                LOGGER.debug("SDLink not available; role mapping will use empty role list for {}", discordUsername);
            }

            // ── Step 5: Whitelist check ───────────────────────────────────────
            if (!discordConfig.passesWhitelist(discordRoles)) {
                LOGGER.warn("Discord user {} ({}) does not have a whitelisted role, denying OAuth2 login",
                    discordUsername, discordId);
                return null;
            }

            // ── Step 6: Determine dashboard role from Discord roles ───────────
            User.Role dashboardRole = discordConfig.getHighestRole(discordRoles);

            // ── Step 7: Find linked Minecraft account ─────────────────────────
            String minecraftUsername = null;

            if (forcedMinecraftUsername != null) {
                // In-game registration: we already know the Minecraft player
                minecraftUsername = forcedMinecraftUsername;
                LOGGER.info("Discord OAuth2: using in-game player '{}' as Minecraft account (state-bound registration)",
                    minecraftUsername);
            } else if (discordProvider.isAvailable()) {
                DiscordUser linkedAccount = discordProvider.getLinkedAccountByDiscordId(discordId);
                if (linkedAccount != null && linkedAccount.isLinked()) {
                    minecraftUsername = linkedAccount.getMinecraftUsername();
                    LOGGER.info("Discord OAuth2: found linked Minecraft account {} for Discord user {}",
                        minecraftUsername, discordUsername);
                } else if (discordConfig.requiresLinkedAccount()) {
                    LOGGER.warn("Discord OAuth2: no linked Minecraft account for {} and requireLinkedAccount=true",
                        discordUsername);
                    return null;
                }
            } else if (discordConfig.requiresLinkedAccount()) {
                LOGGER.warn("Discord OAuth2: SDLink not available and requireLinkedAccount=true — cannot verify link");
                return null;
            }

            // ── Step 8: Get or create dashboard user ──────────────────────────
            AuthenticationManager authManager = AuthenticationManager.getInstance();
            // Use Minecraft username if linked, otherwise fall back to Discord username
            String accountUsername = minecraftUsername != null ? minecraftUsername : discordUsername;

            User user = authManager.getUserByUsername(accountUsername);

            if (user == null) {
                // Allow creation if: auto-registration is on, OR this is an explicit in-game registration
                boolean allowCreate = discordConfig.allowsAutoRegistration() || forcedMinecraftUsername != null;
                if (!allowCreate) {
                    LOGGER.warn("Discord OAuth2: account not found and auto-registration is disabled for {}",
                        accountUsername);
                    return null;
                }
                // Auto-create with a random unusable password — login is via OAuth2 only
                String email = discordId + "@discord.oauth";
                user = authManager.createUser(
                    accountUsername,
                    UUID.randomUUID().toString(),
                    email,
                    dashboardRole
                );
                LOGGER.info("Discord OAuth2: auto-created dashboard user '{}' with role {} (Discord: {})",
                    accountUsername, dashboardRole, discordUsername);
            } else {
                // Sync role if changed
                if (user.getRole() != dashboardRole) {
                    user.setRole(dashboardRole);
                    authManager.saveUsers();
                    LOGGER.info("Discord OAuth2: updated role for '{}' to {} based on Discord roles",
                        accountUsername, dashboardRole);
                }
            }

            // ── Step 9: Create session ────────────────────────────────────────
            Session session = authManager.createSession(user.getId(), ipAddress,
                userAgent != null ? userAgent : "Discord-OAuth2/" + discordUsername);

            LOGGER.info("Discord OAuth2 authentication successful: {} (Discord: {}, Role: {}, IP: {})",
                accountUsername, discordUsername, dashboardRole, ipAddress);

            return session;

        } catch (Exception e) {
            LOGGER.error("Error during Discord OAuth2 flow: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Exchange a Discord authorization code for an access token.
     * POSTs to https://discord.com/api/v10/oauth2/token with application/x-www-form-urlencoded body.
     */
    private String exchangeDiscordCode(String code, String clientId, String clientSecret, String redirectUri) {
        try {
            String body = "client_id=" + encode(clientId)
                + "&client_secret=" + encode(clientSecret)
                + "&grant_type=authorization_code"
                + "&code=" + encode(code)
                + "&redirect_uri=" + encode(redirectUri);

            java.net.URL url = new java.net.URL("https://discord.com/api/v10/oauth2/token");
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("User-Agent", "NeoEssentials-Dashboard/1.0");
            conn.setDoOutput(true);
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);

            try (java.io.OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int status = conn.getResponseCode();
            java.io.InputStream is = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
            if (is == null) return null;

            try (java.io.BufferedReader reader =
                     new java.io.BufferedReader(new java.io.InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                if (status >= 400) {
                    LOGGER.error("Discord token endpoint returned HTTP {}: {}", status, sb);
                }
                return sb.toString();
            }
        } catch (Exception e) {
            LOGGER.error("Error exchanging Discord authorization code: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Fetch a Discord API endpoint with a Bearer access token.
     */
    private String fetchDiscordApi(String apiUrl, String accessToken) {
        try {
            java.net.URL url = new java.net.URL(apiUrl);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);
            conn.setRequestProperty("User-Agent", "NeoEssentials-Dashboard/1.0");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);

            int status = conn.getResponseCode();
            java.io.InputStream is = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
            if (is == null) return null;

            try (java.io.BufferedReader reader =
                     new java.io.BufferedReader(new java.io.InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                if (status >= 400) {
                    LOGGER.error("Discord API {} returned HTTP {}: {}", apiUrl, status, sb);
                    return null;
                }
                return sb.toString();
            }
        } catch (Exception e) {
            LOGGER.error("Error fetching Discord API {}: {}", apiUrl, e.getMessage(), e);
            return null;
        }
    }

    /** URL-encode a string for form-urlencoded body. */
    private static String encode(String value) {
        try {
            return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }

    /**
     * GET /api/auth/discord/status
     * Lightweight, no-auth endpoint. Returns whether Discord OAuth2 is enabled and configured.
     * Used by the login page to decide whether to show / enable the Discord login button.
     * Response: {
     *   "enabled": true,           // discord_auth.json "enabled" flag
     *   "configured": true,        // clientId + clientSecret are set
     *   "sdlinkAvailable": false,  // Simple Discord Link mod is loaded
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
        response.addProperty("configured", config.isOauth2Configured());
        response.addProperty("sdlinkAvailable", provider.isAvailable());
        response.addProperty("requiresLinkedAccount", config.requiresLinkedAccount());
        response.addProperty("allowAutoRegistration", config.allowsAutoRegistration());
        sendJsonResponse(exchange, 200, response);
    }

    /**
     * GET /api/auth/discord/authorize
     * Returns the Discord OAuth2 authorization URL so the frontend can redirect the browser.
     * Response: {"authorizeUrl": "https://discord.com/api/oauth2/authorize?..."}
     */
    private void handleDiscordAuthorizeRedirect(HttpExchange exchange) throws IOException {
        DiscordAuthConfig discordConfig = DiscordAuthConfig.load();

        if (!discordConfig.isEnabled()) {
            sendJsonResponse(exchange, 403, createErrorResponse("Discord authentication is disabled"));
            return;
        }

        if (!discordConfig.isOauth2Configured()) {
            sendJsonResponse(exchange, 503, createErrorResponse(
                "Discord OAuth2 is not configured. Set clientId and clientSecret in discord_auth.json"));
            return;
        }

        String state = UUID.randomUUID().toString().replace("-", "");
        String authorizeUrl = "https://discord.com/api/oauth2/authorize"
            + "?client_id=" + encode(discordConfig.getOauth2ClientId())
            + "&redirect_uri=" + encode(discordConfig.getOauth2RedirectUri())
            + "&response_type=code"
            + "&scope=" + encode(discordConfig.getOauth2Scopes())
            + "&state=" + state;

        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.addProperty("authorizeUrl", authorizeUrl);
        response.addProperty("state", state);
        sendJsonResponse(exchange, 200, response);
    }

    /**
     * GET /api/auth/discord/callback?code=...&state=...
     * Discord redirects the browser here after the user authorizes the app.
     * Exchanges the code for a session and redirects to the dashboard.
     */
    private void handleDiscordOAuthCallback(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        if (query == null) {
            sendHtmlRedirect(exchange, "/login.html?error=missing_code");
            return;
        }

        java.util.Map<String, String> params = new java.util.HashMap<>();
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) params.put(kv[0], java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
        }

        String code = params.get("code");
        String error = params.get("error");
        String state = params.get("state");

        if (error != null) {
            LOGGER.warn("Discord OAuth2 callback received error: {}", error);
            sendHtmlRedirect(exchange, "/login.html?error=" + encode(error));
            return;
        }

        if (code == null || code.isEmpty()) {
            sendHtmlRedirect(exchange, "/login.html?error=missing_code");
            return;
        }

        String ipAddress = exchange.getRemoteAddress().getAddress().getHostAddress();
        String userAgent = exchange.getRequestHeaders().getFirst("User-Agent");

        // Check whether this callback was initiated by an in-game /dashboardregister discord command
        String forcedMinecraftUsername = null;
        if (state != null && !state.isEmpty()) {
            PendingDiscordReg pending = PENDING_DISCORD_REGS.remove(state);
            if (pending != null && !pending.isExpired()) {
                forcedMinecraftUsername = pending.minecraftUsername;
                LOGGER.info("Discord OAuth2 callback matches in-game registration for Minecraft player '{}'",
                    forcedMinecraftUsername);
            }
        }

        Session session = handleDiscordOAuth(code, ipAddress, userAgent, forcedMinecraftUsername);

        if (session == null) {
            sendHtmlRedirect(exchange, "/login.html?error=discord_auth_failed");
            return;
        }

        // Pass sessionId as URL parameter so dashboard.js can store it in localStorage.
        // Also pass the state back so the client can verify CSRF protection.
        // HttpOnly cookies are invisible to JS, so we use a redirect URL param instead.
        String redirectUrl = "/index.html?sessionId=" + session.getSessionId() + "&auth=discord";
        if (state != null && !state.isEmpty()) {
            redirectUrl += "&state=" + encode(state);
        }
        sendHtmlRedirect(exchange, redirectUrl);
    }

    /** Send a 302 redirect response with HTML body fallback. */
    private void sendHtmlRedirect(HttpExchange exchange, String location) throws IOException {
        exchange.getResponseHeaders().add("Location", location);
        byte[] body = ("<html><body>Redirecting… <a href=\"" + location + "\">click here</a></body></html>")
            .getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(302, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
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
