package com.zerog.neoessentials.webdashboard.api.endpoints;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
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
        
        NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "GameEndpoint handling request: {} {}", method, path);

        try {
            // Only allow GET requests
            if (!"GET".equals(method)) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }
            
            // Execute data collection on server thread for thread safety
            CompletableFuture<JsonObject> future = CompletableFuture.supplyAsync(() -> {
                try {
                    NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Collecting game data for endpoint: {}", path);
                    // Parse path to determine which endpoint
                    return switch (path) {
                        case "/api/game/statistics" -> gameCollector.getGameStatistics();
                        case "/api/game/events" -> gameCollector.getGameEvents(100);
                        case "/api/game/activity" -> gameCollector.getGameActivity();
                        case "/api/game/blocks" -> gameCollector.getTopBlocks();
                        default -> {
                            JsonObject error = new JsonObject();
                            error.addProperty("error", "Endpoint not found");
                            yield error;
                        }
                    };
                } catch (Exception e) {
                    NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "Error collecting game data for path: {}", path, e);
                    JsonObject error = new JsonObject();
                    error.addProperty("error", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
                    return error;
                }
            }, server);
            
            // Wait for result with timeout
            JsonObject response;
            try {
                response = future.get(10, TimeUnit.SECONDS);
                NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Game data collected successfully for: {}", path);
            } catch (java.util.concurrent.TimeoutException e) {
                NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "Timeout waiting for game data collection: {}", path);
                response = new JsonObject();
                response.addProperty("error", "Request timeout - server may be overloaded");
            } catch (java.util.concurrent.ExecutionException e) {
                NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "Execution error during game data collection: {}", path, e);
                response = new JsonObject();
                response.addProperty("error", "Internal server error: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
            }
            
            if (response.has("error")) {
                sendResponse(exchange, response.get("error").getAsString().equals("Endpoint not found") ? 404 : 500, response.toString());
            } else {
                sendResponse(exchange, 200, response.toString());
            }
            
        } catch (IOException e) {
            // IOException often means client disconnected - don't try to send error response
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            if (errorMsg.contains("stream is closed") || errorMsg.contains("Broken pipe") || errorMsg.contains("Connection reset")) {
                NeoLog.warn(LOGGER, LogCategory.WEB_DASHBOARD, "Client disconnected during request: {} {} - {}", method, path, errorMsg);
            } else {
                NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "IOException handling request: {} {}", method, path, e);
                // Only try to send error response if stream is likely still open
                try {
                    String errorResponse = String.format("{\"error\":\"IO Error: %s\"}", errorMsg);
                    sendResponse(exchange, 500, errorResponse);
                } catch (IOException e2) {
                    NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Could not send error response (client likely disconnected): {}", e2.getMessage());
                }
            }
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "Unexpected error handling request: {} {}", method, path, e);
            try {
                String errorMsg = e.getMessage() != null ? e.getMessage().replace("\"", "\\\"") : "Unknown error";
                String errorResponse = String.format("{\"error\":\"%s\"}", errorMsg);
                sendResponse(exchange, 500, errorResponse);
            } catch (IOException e2) {
                NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Could not send error response (client likely disconnected): {}", e2.getMessage());
            }
        } finally {
            // Safely close exchange - don't log error if already closed
            try {
                exchange.close();
            } catch (Exception e) {
                NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Exchange already closed: {}", e.getMessage());
            }
        }
    }
    
    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        // Check if response has already been sent or stream is closed
        try {
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(statusCode, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
                os.flush(); // Ensure data is sent
            }
        } catch (IOException e) {
            // Re-throw but with context
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            if (msg.contains("stream is closed") || msg.contains("Broken pipe") || msg.contains("Connection reset")) {
                // Client disconnected - this is expected during heavy operations or restarts
                throw new IOException("Client disconnected: " + msg, e);
            }
            throw e;
        }
    }
}
