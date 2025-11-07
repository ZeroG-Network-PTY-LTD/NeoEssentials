package com.zerog.neoessentials.webdashboard.api.endpoints;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.zerog.neoessentials.webdashboard.data.GameDataCollector;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Handles all game-related API endpoints
 * All Minecraft server calls are executed on the server thread for thread safety
 */
public class GameEndpoint implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GameEndpoint.class);
    private final MinecraftServer server;
    private final GameDataCollector gameCollector;
    
    public GameEndpoint(MinecraftServer server) {
        this.server = server;
        this.gameCollector = new GameDataCollector(server);
    }
    
    @Override
    public void handle(HttpExchange exchange) {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        
        LOGGER.info("GameEndpoint handling request: {} {}", method, path);
        
        try {
            // Only allow GET requests
            if (!"GET".equals(method)) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }
            
            // Execute data collection on server thread for thread safety
            CompletableFuture<JsonObject> future = CompletableFuture.supplyAsync(() -> {
                try {
                    LOGGER.info("Collecting game data for endpoint: {}", path);
                    // Parse path to determine which endpoint
                    if (path.equals("/api/game/statistics")) {
                        return gameCollector.getGameStatistics();
                    } else if (path.equals("/api/game/events")) {
                        return gameCollector.getGameEvents(100);
                    } else if (path.equals("/api/game/activity")) {
                        return gameCollector.getGameActivity();
                    } else if (path.equals("/api/game/blocks")) {
                        return gameCollector.getTopBlocks();
                    } else {
                        JsonObject error = new JsonObject();
                        error.addProperty("error", "Endpoint not found");
                        return error;
                    }
                } catch (Exception e) {
                    LOGGER.error("Error collecting game data for path: {}", path, e);
                    JsonObject error = new JsonObject();
                    error.addProperty("error", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
                    return error;
                }
            }, server);
            
            // Wait for result with timeout
            JsonObject response;
            try {
                response = future.get(10, TimeUnit.SECONDS);
                LOGGER.info("Game data collected successfully for: {}", path);
            } catch (java.util.concurrent.TimeoutException e) {
                LOGGER.error("Timeout waiting for game data collection: {}", path);
                response = new JsonObject();
                response.addProperty("error", "Request timeout - server may be overloaded");
            } catch (java.util.concurrent.ExecutionException e) {
                LOGGER.error("Execution error during game data collection: {}", path, e);
                response = new JsonObject();
                response.addProperty("error", "Internal server error: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
            }
            
            if (response.has("error")) {
                sendResponse(exchange, response.get("error").getAsString().equals("Endpoint not found") ? 404 : 500, response.toString());
            } else {
                sendResponse(exchange, 200, response.toString());
            }
            
        } catch (IOException e) {
            LOGGER.error("IOException handling request: {} {}", method, path, e);
            try {
                String errorResponse = String.format("{\"error\":\"IO Error: %s\"}", e.getMessage());
                sendResponse(exchange, 500, errorResponse);
            } catch (IOException e2) {
                LOGGER.error("Failed to send error response", e2);
            }
        } catch (Exception e) {
            LOGGER.error("Unexpected error handling request: {} {}", method, path, e);
            try {
                String errorResponse = String.format("{\"error\":\"%s\"}", e.getMessage() != null ? e.getMessage().replace("\"", "\\\"") : "Unknown error");
                sendResponse(exchange, 500, errorResponse);
            } catch (IOException e2) {
                LOGGER.error("Failed to send error response", e2);
            }
        } finally {
            exchange.close();
        }
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
