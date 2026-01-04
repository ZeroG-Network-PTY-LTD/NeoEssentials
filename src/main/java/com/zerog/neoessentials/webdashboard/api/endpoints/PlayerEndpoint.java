package com.zerog.neoessentials.webdashboard.api.endpoints;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.zerog.neoessentials.webdashboard.data.PlayerDataCollector;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
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
     * Convert username to UUID (must be called from server thread)
     */
    private UUID usernameToUuid(String username) {
        ServerPlayer player = server.getPlayerList().getPlayerByName(username);
        return player != null ? player.getUUID() : null;
    }
    
    @Override
    public void handle(HttpExchange exchange) {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        
        LOGGER.info("PlayerEndpoint handling request: {} {}", method, path);
        
        try {
            // Only allow GET requests
            if (!"GET".equals(method)) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }
            
            // Execute data collection on server thread for thread safety
            CompletableFuture<JsonObject> future = CompletableFuture.supplyAsync(() -> {
                try {
                    LOGGER.info("Collecting player data for endpoint: {}", path);
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
                LOGGER.info("Player data collected successfully for: {}", path);
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
