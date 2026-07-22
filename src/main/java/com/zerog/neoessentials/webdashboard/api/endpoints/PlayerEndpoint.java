package com.zerog.neoessentials.webdashboard.api.endpoints;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.zerog.neoessentials.webdashboard.data.PlayerDataCollector;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Handles all player-related API endpoints
 * All Minecraft server calls are executed on the server thread for thread safety
 */
public class PlayerEndpoint implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerEndpoint.class);
    private final MinecraftServer server;
    private final PlayerDataCollector playerCollector;
    
    public PlayerEndpoint(MinecraftServer server) {
        this.server = server;
        this.playerCollector = new PlayerDataCollector(server);
    }
    
    /**
     * Convert username to UUID (must be called from server thread). Falls back to the
     * profile cache for offline players — without this, profile/stats/inventory/xp/
     * location all silently 404'd for anyone not currently online, even though the
     * underlying collector methods already support reading offline player data.
     */
    private UUID usernameToUuid(String username) {
        ServerPlayer player = server.getPlayerList().getPlayerByName(username);
        if (player != null) return player.getUUID();

        var cache = server.services().nameToIdCache();
        if (cache == null) return null;
        return cache.get(username).map(net.minecraft.server.players.NameAndId::id).orElse(null);
    }
    
    @Override
    public void handle(HttpExchange exchange) {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        
        LOGGER.debug("PlayerEndpoint handling request: {} {}", method, path);

        try {
            // Route POST requests
            if ("POST".equals(method)) {
                if (path.matches("/api/player/kick/.*")) {
                    String username = path.substring("/api/player/kick/".length());
                    handleKick(exchange, username);
                } else if (path.matches("/api/player/gamemode/.*")) {
                    String username = path.substring("/api/player/gamemode/".length());
                    handleGamemode(exchange, username);
                } else if (path.matches("/api/player/teleport/.*")) {
                    String username = path.substring("/api/player/teleport/".length());
                    handleTeleport(exchange, username);
                } else if (path.matches("/api/player/heal/.*")) {
                    String username = path.substring("/api/player/heal/".length());
                    handleHeal(exchange, username);
                } else if (path.matches("/api/player/fly/.*")) {
                    String username = path.substring("/api/player/fly/".length());
                    handleFly(exchange, username);
                } else if (path.matches("/api/player/god/.*")) {
                    String username = path.substring("/api/player/god/".length());
                    handleGod(exchange, username);
                } else if (path.matches("/api/player/feed/.*")) {
                    String username = path.substring("/api/player/feed/".length());
                    handleFeed(exchange, username);
                } else if (path.matches("/api/player/extinguish/.*")) {
                    String username = path.substring("/api/player/extinguish/".length());
                    handleExtinguish(exchange, username);
                } else if (path.matches("/api/player/speed/.*")) {
                    String username = path.substring("/api/player/speed/".length());
                    handleSpeed(exchange, username);
                } else if (path.matches("/api/player/nickname/.*")) {
                    String username = path.substring("/api/player/nickname/".length());
                    handleNickname(exchange, username);
                } else if (path.matches("/api/player/give/.*")) {
                    String username = path.substring("/api/player/give/".length());
                    handleGive(exchange, username);
                } else if (path.matches("/api/player/burn/.*")) {
                    String username = path.substring("/api/player/burn/".length());
                    handleBurn(exchange, username);
                } else if (path.matches("/api/player/kill/.*")) {
                    String username = path.substring("/api/player/kill/".length());
                    handleKill(exchange, username);
                } else if (path.matches("/api/player/effect/.*")) {
                    String username = path.substring("/api/player/effect/".length());
                    handleEffect(exchange, username);
                } else if (path.matches("/api/player/lightning/.*")) {
                    String username = path.substring("/api/player/lightning/".length());
                    handleLightning(exchange, username);
                } else if (path.matches("/api/player/spawnmob/.*")) {
                    String username = path.substring("/api/player/spawnmob/".length());
                    handleSpawnMob(exchange, username);
                } else {
                    sendResponse(exchange, 404, "{\"error\":\"Endpoint not found\"}");
                }
                return;
            }

            // Only allow GET requests beyond this point
            if (!"GET".equals(method)) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }
            
            // Execute data collection on server thread for thread safety
            CompletableFuture<JsonObject> future = CompletableFuture.supplyAsync(() -> {
                try {
                    LOGGER.debug("Collecting player data for endpoint: {}", path);
                    return getResponse(path);
                } catch (Exception e) {
                    LOGGER.error("Error collecting player data for path: {}", path, e);
                    JsonObject error = new JsonObject();
                    error.addProperty("error", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
                    return error;
                }
            }, server);
            
            // Wait for result with timeout
            JsonObject response;
            try {
                response = future.get(10, TimeUnit.SECONDS);
                LOGGER.debug("Player data collected successfully for: {}", path);
            } catch (java.util.concurrent.TimeoutException e) {
                LOGGER.error("Timeout waiting for player data collection: {}", path);
                response = new JsonObject();
                response.addProperty("error", "Request timeout - server may be overloaded");
            } catch (java.util.concurrent.ExecutionException e) {
                LOGGER.error("Execution error during player data collection: {}", path, e);
                response = new JsonObject();
                response.addProperty("error", "Internal server error: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
            }
            
            if (response.has("error")) {
                String errorMsg = response.get("error").getAsString();
                if (errorMsg.equals("Player not found") || errorMsg.equals("Endpoint not found")) {
                    sendResponse(exchange, 404, response.toString());
                } else {
                    sendResponse(exchange, 500, response.toString());
                }
            } else {
                sendResponse(exchange, 200, response.toString());
            }
            
        } catch (IOException e) {
            // IOException often means client disconnected - don't try to send error response
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            if (errorMsg.contains("stream is closed") || errorMsg.contains("Broken pipe") || errorMsg.contains("Connection reset")) {
                LOGGER.warn("Client disconnected during request: {} {} - {}", method, path, errorMsg);
            } else {
                LOGGER.error("IOException handling request: {} {}", method, path, e);
                try {
                    String errorResponse = String.format("{\"error\":\"IO Error: %s\"}", errorMsg);
                    sendResponse(exchange, 500, errorResponse);
                } catch (IOException e2) {
                    LOGGER.debug("Could not send error response (client likely disconnected): {}", e2.getMessage());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Unexpected error handling request: {} {}", method, path, e);
            try {
                String errorMsg = e.getMessage() != null ? e.getMessage().replace("\"", "\\\"") : "Unknown error";
                String errorResponse = String.format("{\"error\":\"%s\"}", errorMsg);
                sendResponse(exchange, 500, errorResponse);
            } catch (IOException e2) {
                LOGGER.debug("Could not send error response (client likely disconnected): {}", e2.getMessage());
            }
        } finally {
            // Safely close exchange - don't log error if already closed
            try {
                exchange.close();
            } catch (Exception e) {
                // Ignore - exchange may already be closed
            }
        }
    }
    
    // ── POST /api/player/kick/{username} ────────────────────────────────────

    private void handleKick(HttpExchange exchange, String username) throws IOException {
        if (!isAdmin(exchange)) {
            sendResponse(exchange, 403, "{\"success\":false,\"error\":\"Admin permission required\"}");
            return;
        }
        String bodyJson = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String reason;
        try {
            JsonObject body = JsonParser.parseString(bodyJson).getAsJsonObject();
            reason = body.has("reason") ? body.get("reason").getAsString() : "Kicked by dashboard admin";
        } catch (Exception e) {
            reason = "Kicked by dashboard admin";
        }
        final String finalReason = reason;
        final String kickedBy = (String) exchange.getAttribute("auth-username");

        CompletableFuture<JsonObject> future = CompletableFuture.supplyAsync(() -> {
            JsonObject resp = new JsonObject();
            try {
                ServerPlayer player = server.getPlayerList().getPlayerByName(username);
                if (player == null) {
                    resp.addProperty("success", false);
                    resp.addProperty("error", "Player '" + username + "' is not online");
                    return resp;
                }
                player.connection.disconnect(Component.literal(finalReason));
                // Record via KickManager so dashboard-initiated kicks show up in kick
                // history alongside /kick-command kicks, instead of leaving no trace.
                com.zerog.neoessentials.moderation.KickManager.getInstance()
                    .recordKick(username, player.getUUID(), finalReason, kickedBy != null ? kickedBy : "dashboard");
                resp.addProperty("success", true);
                resp.addProperty("message", username + " was kicked: " + finalReason);
            } catch (Exception e) {
                resp.addProperty("success", false);
                resp.addProperty("error", e.getMessage());
            }
            return resp;
        }, server);

        JsonObject result;
        try {
            result = future.get(8, TimeUnit.SECONDS);
        } catch (Exception e) {
            result = new JsonObject();
            result.addProperty("success", false);
            result.addProperty("error", "Timeout or server error: " + e.getMessage());
        }
        int status = result.has("success") && result.get("success").getAsBoolean() ? 200 : 400;
        sendResponse(exchange, status, result.toString());
    }

    // ── POST /api/player/gamemode/{username} ─────────────────────────────────
    // Body: {"gamemode": "creative"}  (survival / creative / adventure / spectator)

    private void handleGamemode(HttpExchange exchange, String username) throws IOException {
        if (!isAdmin(exchange)) {
            sendResponse(exchange, 403, "{\"success\":false,\"error\":\"Admin permission required\"}");
            return;
        }
        String bodyJson = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        GameType targetMode;
        try {
            JsonObject body = JsonParser.parseString(bodyJson).getAsJsonObject();
            String gm = body.has("gamemode") ? body.get("gamemode").getAsString().toLowerCase() : "survival";
            targetMode = switch (gm) {
                case "creative"   -> GameType.CREATIVE;
                case "adventure"  -> GameType.ADVENTURE;
                case "spectator"  -> GameType.SPECTATOR;
                default           -> GameType.SURVIVAL;
            };
        } catch (Exception e) {
            sendResponse(exchange, 400, "{\"success\":false,\"error\":\"Invalid body or gamemode\"}");
            return;
        }
        final GameType finalMode = targetMode;

        CompletableFuture<JsonObject> future = CompletableFuture.supplyAsync(() -> {
            JsonObject resp = new JsonObject();
            try {
                ServerPlayer player = server.getPlayerList().getPlayerByName(username);
                if (player == null) {
                    resp.addProperty("success", false);
                    resp.addProperty("error", "Player '" + username + "' is not online");
                    return resp;
                }
                player.setGameMode(finalMode);
                resp.addProperty("success", true);
                resp.addProperty("message", username + "'s game mode is now " + finalMode.getName());
            } catch (Exception e) {
                resp.addProperty("success", false);
                resp.addProperty("error", e.getMessage());
            }
            return resp;
        }, server);

        JsonObject result;
        try {
            result = future.get(8, TimeUnit.SECONDS);
        } catch (Exception e) {
            result = new JsonObject();
            result.addProperty("success", false);
            result.addProperty("error", "Timeout or server error: " + e.getMessage());
        }
        int status = result.has("success") && result.get("success").getAsBoolean() ? 200 : 400;
        sendResponse(exchange, status, result.toString());
    }

    // ── POST /api/player/teleport/{username} ─────────────────────────────────
    // Body: {"targetUsername": "..."} OR {"x":, "y":, "z":, "world"?: "minecraft:overworld"}

    private void handleTeleport(HttpExchange exchange, String username) throws IOException {
        if (!isAdmin(exchange)) {
            sendResponse(exchange, 403, "{\"success\":false,\"error\":\"Admin permission required\"}");
            return;
        }
        String bodyJson = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        JsonObject body;
        try {
            body = JsonParser.parseString(bodyJson).getAsJsonObject();
        } catch (Exception e) {
            sendResponse(exchange, 400, "{\"success\":false,\"error\":\"Invalid JSON body\"}");
            return;
        }
        final JsonObject finalBody = body;

        CompletableFuture<JsonObject> future = CompletableFuture.supplyAsync(() -> {
            JsonObject resp = new JsonObject();
            try {
                ServerPlayer player = server.getPlayerList().getPlayerByName(username);
                if (player == null) {
                    resp.addProperty("success", false);
                    resp.addProperty("error", "Player '" + username + "' is not online");
                    return resp;
                }

                if (finalBody.has("targetUsername")) {
                    String targetName = finalBody.get("targetUsername").getAsString();
                    ServerPlayer target = server.getPlayerList().getPlayerByName(targetName);
                    if (target == null) {
                        resp.addProperty("success", false);
                        resp.addProperty("error", "Target player '" + targetName + "' is not online");
                        return resp;
                    }
                    player.teleportTo(
                com.zerog.neoessentials.util.LevelCompat.of(target),
                target.getX(),
                target.getY(),
                target.getZ(),
                java.util.Set.of(),
                player.getYRot(),
                player.getXRot(),
                true
            );
                    resp.addProperty("success", true);
                    resp.addProperty("message", username + " teleported to " + targetName);
                } else if (finalBody.has("x") && finalBody.has("y") && finalBody.has("z")) {
                    net.minecraft.server.level.ServerLevel level = com.zerog.neoessentials.util.LevelCompat.of(player);
                    if (finalBody.has("world")) {
                        String worldName = finalBody.get("world").getAsString();
                        net.minecraft.resources.Identifier worldKey = worldName.contains(":")
                            ? net.minecraft.resources.Identifier.parse(worldName)
                            : net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft", worldName);
                        net.minecraft.server.level.ServerLevel requested = server.getLevel(
                            net.minecraft.resources.ResourceKey.create(
                                net.minecraft.core.registries.Registries.DIMENSION, worldKey));
                        if (requested != null) level = requested;
                    }
                    double x = finalBody.get("x").getAsDouble();
                    double y = finalBody.get("y").getAsDouble();
                    double z = finalBody.get("z").getAsDouble();
                    player.teleportTo(
                level,
                x,
                y,
                z,
                java.util.Set.of(),
                player.getYRot(),
                player.getXRot(),
                true
            );
                    resp.addProperty("success", true);
                    resp.addProperty("message", username + " teleported to " + x + ", " + y + ", " + z);
                } else {
                    resp.addProperty("success", false);
                    resp.addProperty("error", "Body must contain 'targetUsername' or 'x'/'y'/'z'");
                }
            } catch (Exception e) {
                resp.addProperty("success", false);
                resp.addProperty("error", e.getMessage());
            }
            return resp;
        }, server);

        JsonObject result;
        try {
            result = future.get(8, TimeUnit.SECONDS);
        } catch (Exception e) {
            result = new JsonObject();
            result.addProperty("success", false);
            result.addProperty("error", "Timeout or server error: " + e.getMessage());
        }
        int status = result.has("success") && result.get("success").getAsBoolean() ? 200 : 400;
        sendResponse(exchange, status, result.toString());
    }

    // ── POST /api/player/heal/{username} ──────────────────────────────────────
    // Heals to full health and feeds to full hunger/saturation.

    private void handleHeal(HttpExchange exchange, String username) throws IOException {
        if (!isAdmin(exchange)) {
            sendResponse(exchange, 403, "{\"success\":false,\"error\":\"Admin permission required\"}");
            return;
        }

        CompletableFuture<JsonObject> future = CompletableFuture.supplyAsync(() -> {
            JsonObject resp = new JsonObject();
            try {
                ServerPlayer player = server.getPlayerList().getPlayerByName(username);
                if (player == null) {
                    resp.addProperty("success", false);
                    resp.addProperty("error", "Player '" + username + "' is not online");
                    return resp;
                }
                player.setHealth(player.getMaxHealth());
                player.getFoodData().setFoodLevel(20);
                player.getFoodData().setSaturation(20.0f);
                resp.addProperty("success", true);
                resp.addProperty("message", username + " healed and fed");
            } catch (Exception e) {
                resp.addProperty("success", false);
                resp.addProperty("error", e.getMessage());
            }
            return resp;
        }, server);

        JsonObject result;
        try {
            result = future.get(8, TimeUnit.SECONDS);
        } catch (Exception e) {
            result = new JsonObject();
            result.addProperty("success", false);
            result.addProperty("error", "Timeout or server error: " + e.getMessage());
        }
        int status = result.has("success") && result.get("success").getAsBoolean() ? 200 : 400;
        sendResponse(exchange, status, result.toString());
    }

    // ── POST /api/player/fly/{username} ───────────────────────────────────────
    // Body (optional): {"enable": true|false} — omit to toggle current state.

    private void handleFly(HttpExchange exchange, String username) throws IOException {
        if (!isAdmin(exchange)) {
            sendResponse(exchange, 403, "{\"success\":false,\"error\":\"Admin permission required\"}");
            return;
        }
        Boolean enable = readOptionalBoolean(exchange, "enable");

        CompletableFuture<JsonObject> future = CompletableFuture.supplyAsync(() -> {
            JsonObject resp = new JsonObject();
            try {
                ServerPlayer player = server.getPlayerList().getPlayerByName(username);
                if (player == null) {
                    resp.addProperty("success", false);
                    resp.addProperty("error", "Player '" + username + "' is not online");
                    return resp;
                }
                boolean newState = com.zerog.neoessentials.util.commands.PlayerStateCommands.setFly(player, enable);
                resp.addProperty("success", true);
                resp.addProperty("enabled", newState);
                resp.addProperty("message", username + "'s flight is now " + (newState ? "enabled" : "disabled"));
            } catch (Exception e) {
                resp.addProperty("success", false);
                resp.addProperty("error", e.getMessage());
            }
            return resp;
        }, server);

        sendFutureResult(exchange, future);
    }

    // ── POST /api/player/god/{username} ───────────────────────────────────────
    // Body (optional): {"enable": true|false} — omit to toggle current state.

    private void handleGod(HttpExchange exchange, String username) throws IOException {
        if (!isAdmin(exchange)) {
            sendResponse(exchange, 403, "{\"success\":false,\"error\":\"Admin permission required\"}");
            return;
        }
        Boolean enable = readOptionalBoolean(exchange, "enable");

        CompletableFuture<JsonObject> future = CompletableFuture.supplyAsync(() -> {
            JsonObject resp = new JsonObject();
            try {
                ServerPlayer player = server.getPlayerList().getPlayerByName(username);
                if (player == null) {
                    resp.addProperty("success", false);
                    resp.addProperty("error", "Player '" + username + "' is not online");
                    return resp;
                }
                boolean newState = com.zerog.neoessentials.util.commands.PlayerStateCommands.setGod(player, enable);
                resp.addProperty("success", true);
                resp.addProperty("enabled", newState);
                resp.addProperty("message", username + "'s god mode is now " + (newState ? "enabled" : "disabled"));
            } catch (Exception e) {
                resp.addProperty("success", false);
                resp.addProperty("error", e.getMessage());
            }
            return resp;
        }, server);

        sendFutureResult(exchange, future);
    }

    // ── POST /api/player/feed/{username} ──────────────────────────────────────

    private void handleFeed(HttpExchange exchange, String username) throws IOException {
        if (!isAdmin(exchange)) {
            sendResponse(exchange, 403, "{\"success\":false,\"error\":\"Admin permission required\"}");
            return;
        }

        CompletableFuture<JsonObject> future = CompletableFuture.supplyAsync(() -> {
            JsonObject resp = new JsonObject();
            try {
                ServerPlayer player = server.getPlayerList().getPlayerByName(username);
                if (player == null) {
                    resp.addProperty("success", false);
                    resp.addProperty("error", "Player '" + username + "' is not online");
                    return resp;
                }
                com.zerog.neoessentials.util.commands.PlayerStateCommands.feedPlayer(player);
                resp.addProperty("success", true);
                resp.addProperty("message", username + " fed");
            } catch (Exception e) {
                resp.addProperty("success", false);
                resp.addProperty("error", e.getMessage());
            }
            return resp;
        }, server);

        sendFutureResult(exchange, future);
    }

    // ── POST /api/player/extinguish/{username} ────────────────────────────────

    private void handleExtinguish(HttpExchange exchange, String username) throws IOException {
        if (!isAdmin(exchange)) {
            sendResponse(exchange, 403, "{\"success\":false,\"error\":\"Admin permission required\"}");
            return;
        }

        CompletableFuture<JsonObject> future = CompletableFuture.supplyAsync(() -> {
            JsonObject resp = new JsonObject();
            try {
                ServerPlayer player = server.getPlayerList().getPlayerByName(username);
                if (player == null) {
                    resp.addProperty("success", false);
                    resp.addProperty("error", "Player '" + username + "' is not online");
                    return resp;
                }
                com.zerog.neoessentials.util.commands.PlayerStateCommands.extinguishPlayer(player);
                resp.addProperty("success", true);
                resp.addProperty("message", username + " extinguished");
            } catch (Exception e) {
                resp.addProperty("success", false);
                resp.addProperty("error", e.getMessage());
            }
            return resp;
        }, server);

        sendFutureResult(exchange, future);
    }

    // ── POST /api/player/speed/{username} ─────────────────────────────────────
    // Body: {"type": "walk"|"fly", "speed": 0-10}

    private void handleSpeed(HttpExchange exchange, String username) throws IOException {
        if (!isAdmin(exchange)) {
            sendResponse(exchange, 403, "{\"success\":false,\"error\":\"Admin permission required\"}");
            return;
        }
        String bodyJson = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        boolean fly;
        float speed;
        try {
            JsonObject body = JsonParser.parseString(bodyJson).getAsJsonObject();
            fly = body.has("type") && "fly".equalsIgnoreCase(body.get("type").getAsString());
            speed = body.has("speed") ? body.get("speed").getAsFloat() : 1f;
            speed = Math.max(0f, Math.min(10f, speed));
        } catch (Exception e) {
            sendResponse(exchange, 400, "{\"success\":false,\"error\":\"Invalid body\"}");
            return;
        }
        final boolean finalFly = fly;
        final float finalSpeed = speed;

        CompletableFuture<JsonObject> future = CompletableFuture.supplyAsync(() -> {
            JsonObject resp = new JsonObject();
            try {
                ServerPlayer player = server.getPlayerList().getPlayerByName(username);
                if (player == null) {
                    resp.addProperty("success", false);
                    resp.addProperty("error", "Player '" + username + "' is not online");
                    return resp;
                }
                com.zerog.neoessentials.util.commands.PlayerStateCommands.setSpeed(player, finalFly, finalSpeed);
                resp.addProperty("success", true);
                resp.addProperty("message", username + "'s " + (finalFly ? "fly" : "walk") + " speed set to " + finalSpeed);
            } catch (Exception e) {
                resp.addProperty("success", false);
                resp.addProperty("error", e.getMessage());
            }
            return resp;
        }, server);

        sendFutureResult(exchange, future);
    }

    // ── POST /api/player/nickname/{username} ──────────────────────────────────
    // Body: {"nickname": "..."} — omit/blank/"reset"/"off" clears the nickname.

    private void handleNickname(HttpExchange exchange, String username) throws IOException {
        if (!isAdmin(exchange)) {
            sendResponse(exchange, 403, "{\"success\":false,\"error\":\"Admin permission required\"}");
            return;
        }
        String bodyJson = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String nickname = null;
        try {
            if (!bodyJson.isBlank()) {
                JsonObject body = JsonParser.parseString(bodyJson).getAsJsonObject();
                if (body.has("nickname") && !body.get("nickname").isJsonNull()) {
                    nickname = body.get("nickname").getAsString();
                }
            }
        } catch (Exception e) {
            sendResponse(exchange, 400, "{\"success\":false,\"error\":\"Invalid body\"}");
            return;
        }
        final String finalNickname = nickname;

        CompletableFuture<JsonObject> future = CompletableFuture.supplyAsync(() -> {
            JsonObject resp = new JsonObject();
            try {
                ServerPlayer player = server.getPlayerList().getPlayerByName(username);
                if (player == null) {
                    resp.addProperty("success", false);
                    resp.addProperty("error", "Player '" + username + "' is not online");
                    return resp;
                }
                String error = com.zerog.neoessentials.util.commands.NickCommand.setNicknameAdmin(player, finalNickname);
                if (error != null) {
                    resp.addProperty("success", false);
                    resp.addProperty("error", error);
                } else {
                    resp.addProperty("success", true);
                    resp.addProperty("message", "Nickname updated for " + username);
                }
            } catch (Exception e) {
                resp.addProperty("success", false);
                resp.addProperty("error", e.getMessage());
            }
            return resp;
        }, server);

        sendFutureResult(exchange, future);
    }

    // ── POST /api/player/give/{username} ──────────────────────────────────────
    // Body: {"item": "minecraft:diamond_sword", "amount": 1}

    private void handleGive(HttpExchange exchange, String username) throws IOException {
        if (!isAdmin(exchange)) {
            sendResponse(exchange, 403, "{\"success\":false,\"error\":\"Admin permission required\"}");
            return;
        }
        String bodyJson = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String itemId;
        int amount;
        try {
            JsonObject body = JsonParser.parseString(bodyJson).getAsJsonObject();
            itemId = body.has("item") ? body.get("item").getAsString() : "";
            amount = body.has("amount") ? Math.max(1, Math.min(3456, body.get("amount").getAsInt())) : 1;
        } catch (Exception e) {
            sendResponse(exchange, 400, "{\"success\":false,\"error\":\"Invalid body\"}");
            return;
        }
        if (itemId.isBlank()) {
            sendResponse(exchange, 400, "{\"success\":false,\"error\":\"Missing 'item'\"}");
            return;
        }
        final String finalItemId = itemId;
        final int finalAmount = amount;

        CompletableFuture<JsonObject> future = CompletableFuture.supplyAsync(() -> {
            JsonObject resp = new JsonObject();
            try {
                ServerPlayer player = server.getPlayerList().getPlayerByName(username);
                if (player == null) {
                    resp.addProperty("success", false);
                    resp.addProperty("error", "Player '" + username + "' is not online");
                    return resp;
                }
                net.minecraft.world.item.ItemStack stack = com.zerog.neoessentials.economy.worth.WorthManager.resolveItem(finalItemId);
                if (stack == null) {
                    resp.addProperty("success", false);
                    resp.addProperty("error", "Unknown item: " + finalItemId);
                    return resp;
                }
                int remaining = finalAmount;
                int maxStack = stack.getMaxStackSize();
                while (remaining > 0) {
                    int give = Math.min(remaining, maxStack);
                    net.minecraft.world.item.ItemStack toGive = stack.copyWithCount(give);
                    if (!player.getInventory().add(toGive)) {
                        player.drop(toGive, false);
                    }
                    remaining -= give;
                }
                resp.addProperty("success", true);
                resp.addProperty("message", "Gave " + finalAmount + "x " + finalItemId + " to " + username);
            } catch (Exception e) {
                resp.addProperty("success", false);
                resp.addProperty("error", e.getMessage());
            }
            return resp;
        }, server);

        sendFutureResult(exchange, future);
    }

    // ── POST /api/player/burn/{username} ──────────────────────────────────────
    // Body (optional): {"seconds": 10}

    private void handleBurn(HttpExchange exchange, String username) throws IOException {
        if (!isAdmin(exchange)) {
            sendResponse(exchange, 403, "{\"success\":false,\"error\":\"Admin permission required\"}");
            return;
        }
        String bodyJson = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        int seconds = 10;
        try {
            if (!bodyJson.isBlank()) {
                JsonObject body = JsonParser.parseString(bodyJson).getAsJsonObject();
                if (body.has("seconds")) seconds = Math.max(1, Math.min(600, body.get("seconds").getAsInt()));
            }
        } catch (Exception ignored) {}
        final int finalSeconds = seconds;

        CompletableFuture<JsonObject> future = CompletableFuture.supplyAsync(() -> {
            JsonObject resp = new JsonObject();
            try {
                ServerPlayer player = server.getPlayerList().getPlayerByName(username);
                if (player == null) {
                    resp.addProperty("success", false);
                    resp.addProperty("error", "Player '" + username + "' is not online");
                    return resp;
                }
                player.setRemainingFireTicks(finalSeconds * 20);
                resp.addProperty("success", true);
                resp.addProperty("message", username + " set on fire for " + finalSeconds + "s");
            } catch (Exception e) {
                resp.addProperty("success", false);
                resp.addProperty("error", e.getMessage());
            }
            return resp;
        }, server);

        sendFutureResult(exchange, future);
    }

    // ── POST /api/player/kill/{username} ──────────────────────────────────────

    private void handleKill(HttpExchange exchange, String username) throws IOException {
        if (!isAdmin(exchange)) {
            sendResponse(exchange, 403, "{\"success\":false,\"error\":\"Admin permission required\"}");
            return;
        }

        CompletableFuture<JsonObject> future = CompletableFuture.supplyAsync(() -> {
            JsonObject resp = new JsonObject();
            try {
                ServerPlayer player = server.getPlayerList().getPlayerByName(username);
                if (player == null) {
                    resp.addProperty("success", false);
                    resp.addProperty("error", "Player '" + username + "' is not online");
                    return resp;
                }
                player.hurt(player.damageSources().genericKill(), Float.MAX_VALUE);
                resp.addProperty("success", true);
                resp.addProperty("message", username + " killed");
            } catch (Exception e) {
                resp.addProperty("success", false);
                resp.addProperty("error", e.getMessage());
            }
            return resp;
        }, server);

        sendFutureResult(exchange, future);
    }

    // ── POST /api/player/effect/{username} ────────────────────────────────────
    // Body: {"clear": true} OR {"effect": "speed", "duration": 30, "amplifier": 0}

    private void handleEffect(HttpExchange exchange, String username) throws IOException {
        if (!isAdmin(exchange)) {
            sendResponse(exchange, 403, "{\"success\":false,\"error\":\"Admin permission required\"}");
            return;
        }
        String bodyJson = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        JsonObject body;
        try {
            body = JsonParser.parseString(bodyJson).getAsJsonObject();
        } catch (Exception e) {
            sendResponse(exchange, 400, "{\"success\":false,\"error\":\"Invalid body\"}");
            return;
        }
        final boolean clear = body.has("clear") && body.get("clear").getAsBoolean();
        final String effectId = body.has("effect") ? body.get("effect").getAsString() : null;
        final int duration = body.has("duration") ? Math.max(1, body.get("duration").getAsInt()) : 30;
        final int amplifier = body.has("amplifier") ? Math.max(0, Math.min(255, body.get("amplifier").getAsInt())) : 0;

        if (!clear && (effectId == null || effectId.isBlank())) {
            sendResponse(exchange, 400, "{\"success\":false,\"error\":\"Body must contain 'clear':true or an 'effect'\"}");
            return;
        }

        CompletableFuture<JsonObject> future = CompletableFuture.supplyAsync(() -> {
            JsonObject resp = new JsonObject();
            try {
                ServerPlayer player = server.getPlayerList().getPlayerByName(username);
                if (player == null) {
                    resp.addProperty("success", false);
                    resp.addProperty("error", "Player '" + username + "' is not online");
                    return resp;
                }
                if (clear) {
                    player.removeAllEffects();
                    resp.addProperty("success", true);
                    resp.addProperty("message", "Cleared all effects on " + username);
                    return resp;
                }
                String id = effectId.contains(":") ? effectId : "minecraft:" + effectId;
                net.minecraft.resources.Identifier loc = net.minecraft.resources.Identifier.tryParse(id);
                net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effectHolder = loc != null
                    ? net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.get(loc)
                        .map(h -> (net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>) h).orElse(null)
                    : null;
                if (effectHolder == null) {
                    effectHolder = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.entrySet().stream()
                        .filter(e -> e.getKey().identifier().getPath().equals(effectId.toLowerCase()))
                        .map(e -> net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(e.getValue()))
                        .findFirst().orElse(null);
                }
                if (effectHolder == null) {
                    resp.addProperty("success", false);
                    resp.addProperty("error", "Unknown effect: " + effectId);
                    return resp;
                }
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    effectHolder, duration * 20, amplifier, false, true));
                resp.addProperty("success", true);
                resp.addProperty("message", "Applied " + effectId + " (amp " + amplifier + ", " + duration + "s) to " + username);
            } catch (Exception e) {
                resp.addProperty("success", false);
                resp.addProperty("error", e.getMessage());
            }
            return resp;
        }, server);

        sendFutureResult(exchange, future);
    }

    // ── POST /api/player/lightning/{username} ─────────────────────────────────
    // Strikes lightning at the player's current position.

    private void handleLightning(HttpExchange exchange, String username) throws IOException {
        if (!isAdmin(exchange)) {
            sendResponse(exchange, 403, "{\"success\":false,\"error\":\"Admin permission required\"}");
            return;
        }

        CompletableFuture<JsonObject> future = CompletableFuture.supplyAsync(() -> {
            JsonObject resp = new JsonObject();
            try {
                ServerPlayer player = server.getPlayerList().getPlayerByName(username);
                if (player == null) {
                    resp.addProperty("success", false);
                    resp.addProperty("error", "Player '" + username + "' is not online");
                    return resp;
                }
                var level = com.zerog.neoessentials.util.LevelCompat.of(player);
                net.minecraft.world.entity.LightningBolt bolt =
                    com.zerog.neoessentials.util.EntityTypeCompat.create(net.minecraft.world.entity.EntityType.LIGHTNING_BOLT, level);
                if (bolt != null) {
                    bolt.snapTo(player.getX(), player.getY(), player.getZ());
                    level.addFreshEntity(bolt);
                }
                resp.addProperty("success", true);
                resp.addProperty("message", "Lightning struck " + username);
            } catch (Exception e) {
                resp.addProperty("success", false);
                resp.addProperty("error", e.getMessage());
            }
            return resp;
        }, server);

        sendFutureResult(exchange, future);
    }

    // ── POST /api/player/spawnmob/{username} ──────────────────────────────────
    // Body: {"mob": "zombie", "amount": 1} — spawned at the player's current location.

    @SuppressWarnings("deprecation")
    private void handleSpawnMob(HttpExchange exchange, String username) throws IOException {
        if (!isAdmin(exchange)) {
            sendResponse(exchange, 403, "{\"success\":false,\"error\":\"Admin permission required\"}");
            return;
        }
        String bodyJson = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String mobId;
        int amount;
        try {
            JsonObject body = JsonParser.parseString(bodyJson).getAsJsonObject();
            mobId = body.has("mob") ? body.get("mob").getAsString() : "";
            amount = body.has("amount") ? Math.max(1, Math.min(100, body.get("amount").getAsInt())) : 1;
        } catch (Exception e) {
            sendResponse(exchange, 400, "{\"success\":false,\"error\":\"Invalid body\"}");
            return;
        }
        if (mobId.isBlank()) {
            sendResponse(exchange, 400, "{\"success\":false,\"error\":\"Missing 'mob'\"}");
            return;
        }
        final String finalMobId = mobId;
        final int finalAmount = amount;

        CompletableFuture<JsonObject> future = CompletableFuture.supplyAsync(() -> {
            JsonObject resp = new JsonObject();
            try {
                ServerPlayer player = server.getPlayerList().getPlayerByName(username);
                if (player == null) {
                    resp.addProperty("success", false);
                    resp.addProperty("error", "Player '" + username + "' is not online");
                    return resp;
                }
                String id = finalMobId.contains(":") ? finalMobId : "minecraft:" + finalMobId;
                net.minecraft.resources.Identifier loc = net.minecraft.resources.Identifier.tryParse(id);
                var typeOpt = loc != null
                    ? net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getOptional(loc)
                    : java.util.Optional.<net.minecraft.world.entity.EntityType<?>>empty();
                if (typeOpt.isEmpty()) {
                    typeOpt = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.entrySet().stream()
                        .filter(e -> e.getKey().identifier().getPath().equals(finalMobId.toLowerCase()))
                        .<net.minecraft.world.entity.EntityType<?>>map(java.util.Map.Entry::getValue)
                        .findFirst();
                }
                if (typeOpt.isEmpty()) {
                    resp.addProperty("success", false);
                    resp.addProperty("error", "Unknown mob: " + finalMobId);
                    return resp;
                }
                var entityType = typeOpt.get();
                var level = com.zerog.neoessentials.util.LevelCompat.of(player);
                int spawned = 0;
                for (int i = 0; i < finalAmount; i++) {
                    var entity = com.zerog.neoessentials.util.EntityTypeCompat.create(entityType, level);
                    if (entity == null) break;
                    entity.snapTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0f);
                    if (entity instanceof net.minecraft.world.entity.Mob mob) {
                        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(player.blockPosition()),
                            net.minecraft.world.entity.EntitySpawnReason.COMMAND, null);
                    }
                    level.addFreshEntity(entity);
                    spawned++;
                }
                resp.addProperty("success", true);
                resp.addProperty("message", "Spawned " + spawned + "x " + finalMobId + " at " + username);
            } catch (Exception e) {
                resp.addProperty("success", false);
                resp.addProperty("error", e.getMessage());
            }
            return resp;
        }, server);

        sendFutureResult(exchange, future);
    }

    /** Reads an optional boolean field from the request body; returns null if absent/blank/invalid. */
    private Boolean readOptionalBoolean(HttpExchange exchange, String field) throws IOException {
        String bodyJson = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        if (bodyJson.isBlank()) return null;
        try {
            JsonObject body = JsonParser.parseString(bodyJson).getAsJsonObject();
            return body.has(field) && !body.get(field).isJsonNull() ? body.get(field).getAsBoolean() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /** Waits on a future built the same way every handler above builds it, and sends the result. */
    private void sendFutureResult(HttpExchange exchange, CompletableFuture<JsonObject> future) throws IOException {
        JsonObject result;
        try {
            result = future.get(8, TimeUnit.SECONDS);
        } catch (Exception e) {
            result = new JsonObject();
            result.addProperty("success", false);
            result.addProperty("error", "Timeout or server error: " + e.getMessage());
        }
        int status = result.has("success") && result.get("success").getAsBoolean() ? 200 : 400;
        sendResponse(exchange, status, result.toString());
    }

    /** Returns true if the exchange was authenticated as an admin. */
    private boolean isAdmin(HttpExchange exchange) {
        Object adminAttr = exchange.getAttribute("auth-admin");
        return Boolean.TRUE.equals(adminAttr);
    }

    private JsonObject getResponse(String path) {
        JsonObject response;
            
            // Parse path to determine which endpoint
            if (path.matches("/api/player/profile/.*")) {
                String username = path.substring("/api/player/profile/".length());
                UUID uuid = usernameToUuid(username);
                if (uuid == null) {
                    response = new JsonObject();
                    response.addProperty("error", "Player not found");
                    return response;
                }
                response = playerCollector.getPlayerProfile(uuid);
            } else if (path.matches("/api/player/stats/.*")) {
                String username = path.substring("/api/player/stats/".length());
                UUID uuid = usernameToUuid(username);
                if (uuid == null) {
                    response = new JsonObject();
                    response.addProperty("error", "Player not found");
                    return response;
                }
                response = playerCollector.getPlayerStatistics(uuid);
            } else if (path.matches("/api/player/achievements/.*")) {
                String username = path.substring("/api/player/achievements/".length());
                UUID uuid = usernameToUuid(username);
                if (uuid == null) {
                    response = new JsonObject();
                    response.addProperty("error", "Player not found");
                    return response;
                }
                response = playerCollector.getPlayerAchievements(uuid);
            } else if (path.matches("/api/player/inventory/.*")) {
                String username = path.substring("/api/player/inventory/".length());
                UUID uuid = usernameToUuid(username);
                if (uuid == null) {
                    response = new JsonObject();
                    response.addProperty("error", "Player not found");
                    return response;
                }
                response = playerCollector.getPlayerInventory(uuid);
            } else if (path.matches("/api/player/status/.*")) {
                String username = path.substring("/api/player/status/".length());
                UUID uuid = usernameToUuid(username);
                if (uuid == null) {
                    response = new JsonObject();
                    response.addProperty("error", "Player not found");
                    return response;
                }
                response = playerCollector.getPlayerStatus(uuid);
            } else if (path.matches("/api/player/health/.*")) {
                String username = path.substring("/api/player/health/".length());
                UUID uuid = usernameToUuid(username);
                if (uuid == null) {
                    response = new JsonObject();
                    response.addProperty("error", "Player not found");
                    return response;
                }
                response = playerCollector.getPlayerHealth(uuid);
            } else if (path.matches("/api/player/xp/.*")) {
                String username = path.substring("/api/player/xp/".length());
                UUID uuid = usernameToUuid(username);
                if (uuid == null) {
                    response = new JsonObject();
                    response.addProperty("error", "Player not found");
                    return response;
                }
                response = playerCollector.getPlayerXP(uuid);
            } else if (path.matches("/api/player/location/.*")) {
                String username = path.substring("/api/player/location/".length());
                UUID uuid = usernameToUuid(username);
                if (uuid == null) {
                    response = new JsonObject();
                    response.addProperty("error", "Player not found");
                    return response;
                }
                response = playerCollector.getPlayerLocation(uuid);
            } else if (path.matches("/api/player/homes/.*")) {
                String username = path.substring("/api/player/homes/".length());
                response = playerCollector.getPlayerHomes(username);
            } else if (path.equals("/api/player/online")) {
                response = playerCollector.getOnlinePlayers();
            } else if (path.matches("/api/player/lookup/.*")) {
                String username = path.substring("/api/player/lookup/".length());
                response = playerCollector.lookupPlayer(username);
            } else {
                response = new JsonObject();
                response.addProperty("error", "Endpoint not found");
                return response;
            }
            
            return response;
    }
    
    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
