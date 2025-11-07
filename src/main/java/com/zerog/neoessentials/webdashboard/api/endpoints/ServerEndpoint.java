package com.zerog.neoessentials.webdashboard.api.endpoints;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.zerog.neoessentials.webdashboard.data.ServerDataCollector;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Handles all server-related API endpoints
 * All Minecraft server calls are executed on the server thread for thread safety
 */
public class ServerEndpoint implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerEndpoint.class);
    private final MinecraftServer server;
    private final ServerDataCollector serverCollector;
    
    public ServerEndpoint(MinecraftServer server) {
        this.server = server;
        this.serverCollector = new ServerDataCollector(server);
    }
    
    @Override
    public void handle(HttpExchange exchange) {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        
        LOGGER.info("ServerEndpoint handling request: {} {}", method, path);
        
        try {
            // Only allow GET requests
            if (!"GET".equals(method)) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }
            
            // Execute data collection on server thread for thread safety
            CompletableFuture<JsonObject> future = CompletableFuture.supplyAsync(() -> {
                try {
                    LOGGER.info("Collecting data for endpoint: {}", path);
                    // Parse path to determine which endpoint
                    if (path.equals("/api/server/profile")) {
                        return serverCollector.getServerProfile();
                    } else if (path.equals("/api/server/performance")) {
                        return serverCollector.getServerPerformance();
                    } else if (path.equals("/api/server/statistics")) {
                        return serverCollector.getServerStatistics();
                    } else if (path.equals("/api/server/status")) {
                        return serverCollector.getServerStatus();
                    } else if (path.equals("/api/server/health")) {
                        return serverCollector.getServerHealth();
                    } else if (path.equals("/api/server/worlds")) {
                        return serverCollector.getServerWorlds();
                    } else if (path.equals("/api/server/config")) {
                        return serverCollector.getServerConfig();
                    } else {
                        JsonObject error = new JsonObject();
                        error.addProperty("error", "Endpoint not found");
                        return error;
                    }
                } catch (Exception e) {
                    LOGGER.error("Error collecting server data for path: {}", path, e);
                    JsonObject error = new JsonObject();
                    error.addProperty("error", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
                    return error;
                }
            }, server);
            
            // Wait for result with timeout
            JsonObject response;
            try {
                response = future.get(10, TimeUnit.SECONDS);
                LOGGER.info("Data collected successfully for: {}", path);
            } catch (java.util.concurrent.TimeoutException e) {
                LOGGER.error("Timeout waiting for data collection: {}", path);
                response = new JsonObject();
                response.addProperty("error", "Request timeout - server may be overloaded");
            } catch (java.util.concurrent.ExecutionException e) {
                LOGGER.error("Execution error during data collection: {}", path, e);
                response = new JsonObject();
                response.addProperty("error", "Internal server error: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
            }
            
            if (response.has("error") && !path.equals("/api/server/profile")) {
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
