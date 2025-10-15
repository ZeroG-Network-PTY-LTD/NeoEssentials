package com.zerog.neoessentials.webdashboard.map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * REST API handler for map viewer endpoints
 * Provides player locations, waypoints, world info, and map data
 */
public class MapHandler implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(MapHandler.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        
        try {
            if ("GET".equals(method)) {
                handleGet(exchange, path);
            } else if ("POST".equals(method)) {
                handlePost(exchange, path);
            } else if ("PUT".equals(method)) {
                handlePut(exchange, path);
            } else if ("DELETE".equals(method)) {
                handleDelete(exchange, path);
            } else {
                sendErrorResponse(exchange, 405, "Method not allowed");
            }
        } catch (Exception e) {
            LOGGER.error("Error handling map request", e);
            sendErrorResponse(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }
    
    /**
     * Handle GET requests
     */
    private void handleGet(HttpExchange exchange, String path) throws IOException {
        Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
        
        if (path.endsWith("/map/players")) {
            handleGetPlayers(exchange, params);
        } else if (path.endsWith("/map/waypoints")) {
            handleGetWaypoints(exchange, params);
        } else if (path.endsWith("/map/world-info")) {
            handleGetWorldInfo(exchange, params);
        } else if (path.endsWith("/map/dimensions")) {
            handleGetDimensions(exchange);
        } else if (path.endsWith("/map/spawns")) {
            handleGetSpawns(exchange);
        } else {
            sendErrorResponse(exchange, 404, "Endpoint not found");
        }
    }
    
    /**
     * Handle POST requests
     */
    private void handlePost(HttpExchange exchange, String path) throws IOException {
        if (path.endsWith("/map/waypoints")) {
            handleCreateWaypoint(exchange);
        } else {
            sendErrorResponse(exchange, 404, "Endpoint not found");
        }
    }
    
    /**
     * Handle PUT requests
     */
    private void handlePut(HttpExchange exchange, String path) throws IOException {
        if (path.contains("/map/waypoints/")) {
            handleUpdateWaypoint(exchange, path);
        } else {
            sendErrorResponse(exchange, 404, "Endpoint not found");
        }
    }
    
    /**
     * Handle DELETE requests
     */
    private void handleDelete(HttpExchange exchange, String path) throws IOException {
        if (path.contains("/map/waypoints/")) {
            handleDeleteWaypoint(exchange, path);
        } else {
            sendErrorResponse(exchange, 404, "Endpoint not found");
        }
    }
    
    /**
     * GET /api/map/players?dimension=minecraft:overworld
     */
    private void handleGetPlayers(HttpExchange exchange, Map<String, String> params) throws IOException {
        PlayerLocationTracker tracker = PlayerLocationTracker.getInstance();
        
        JsonObject response;
        if (params.containsKey("dimension")) {
            String dimension = params.get("dimension");
            response = tracker.getPlayerLocationsJson(dimension);
        } else {
            response = tracker.getPlayerLocationsJson();
        }
        
        sendJsonResponse(exchange, 200, response);
    }
    
    /**
     * GET /api/map/waypoints?dimension=minecraft:overworld
     */
    private void handleGetWaypoints(HttpExchange exchange, Map<String, String> params) throws IOException {
        WaypointManager manager = WaypointManager.getInstance();
        
        JsonObject response;
        if (params.containsKey("dimension")) {
            String dimension = params.get("dimension");
            response = manager.getWaypointsJson(dimension);
        } else {
            response = manager.getWaypointsJson();
        }
        
        sendJsonResponse(exchange, 200, response);
    }
    
    /**
     * GET /api/map/world-info?dimension=minecraft:overworld
     */
    private void handleGetWorldInfo(HttpExchange exchange, Map<String, String> params) throws IOException {
        WorldInfoCollector collector = WorldInfoCollector.getInstance();
        
        JsonObject response;
        if (params.containsKey("dimension")) {
            String dimension = params.get("dimension");
            response = collector.getDimensionInfoJson(dimension);
        } else {
            response = collector.getWorldInfoJson();
        }
        
        sendJsonResponse(exchange, 200, response);
    }
    
    /**
     * GET /api/map/dimensions
     */
    private void handleGetDimensions(HttpExchange exchange) throws IOException {
        WorldInfoCollector collector = WorldInfoCollector.getInstance();
        JsonObject response = collector.getDimensionsListJson();
        sendJsonResponse(exchange, 200, response);
    }
    
    /**
     * GET /api/map/spawns
     */
    private void handleGetSpawns(HttpExchange exchange) throws IOException {
        WorldInfoCollector collector = WorldInfoCollector.getInstance();
        JsonObject response = collector.getSpawnPointsJson();
        sendJsonResponse(exchange, 200, response);
    }
    
    /**
     * POST /api/map/waypoints - Create new waypoint
     */
    private void handleCreateWaypoint(HttpExchange exchange) throws IOException {
        String requestBody = readRequestBody(exchange);
        JsonObject data = JsonParser.parseString(requestBody).getAsJsonObject();
        
        // Extract waypoint data
        String name = data.get("name").getAsString();
        String dimension = data.get("dimension").getAsString();
        double x = data.get("x").getAsDouble();
        double y = data.get("y").getAsDouble();
        double z = data.get("z").getAsDouble();
        String color = data.has("color") ? data.get("color").getAsString() : null;
        String icon = data.has("icon") ? data.get("icon").getAsString() : null;
        String description = data.has("description") ? data.get("description").getAsString() : null;
        String createdBy = getUsernameFromSession(exchange);
        
        // Create waypoint
        WaypointManager manager = WaypointManager.getInstance();
        WaypointManager.Waypoint waypoint = manager.createWaypoint(
            name, dimension, x, y, z, color, icon, description, createdBy
        );
        
        // Build response
        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.addProperty("message", "Waypoint created successfully");
        response.addProperty("waypointId", waypoint.id);
        
        sendJsonResponse(exchange, 201, response);
    }
    
    /**
     * PUT /api/map/waypoints/{id} - Update waypoint
     */
    private void handleUpdateWaypoint(HttpExchange exchange, String path) throws IOException {
        // Extract waypoint ID from path
        String[] pathParts = path.split("/");
        String waypointId = pathParts[pathParts.length - 1];
        
        String requestBody = readRequestBody(exchange);
        JsonObject data = JsonParser.parseString(requestBody).getAsJsonObject();
        
        // Extract update fields
        String name = data.has("name") ? data.get("name").getAsString() : null;
        String dimension = data.has("dimension") ? data.get("dimension").getAsString() : null;
        Double x = data.has("x") ? data.get("x").getAsDouble() : null;
        Double y = data.has("y") ? data.get("y").getAsDouble() : null;
        Double z = data.has("z") ? data.get("z").getAsDouble() : null;
        String color = data.has("color") ? data.get("color").getAsString() : null;
        String icon = data.has("icon") ? data.get("icon").getAsString() : null;
        String description = data.has("description") ? data.get("description").getAsString() : null;
        Boolean visible = data.has("visible") ? data.get("visible").getAsBoolean() : null;
        
        // Update waypoint
        WaypointManager manager = WaypointManager.getInstance();
        boolean success = manager.updateWaypoint(waypointId, name, dimension, x, y, z, color, icon, description, visible);
        
        if (success) {
            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("message", "Waypoint updated successfully");
            sendJsonResponse(exchange, 200, response);
        } else {
            sendErrorResponse(exchange, 404, "Waypoint not found");
        }
    }
    
    /**
     * DELETE /api/map/waypoints/{id} - Delete waypoint
     */
    private void handleDeleteWaypoint(HttpExchange exchange, String path) throws IOException {
        // Extract waypoint ID from path
        String[] pathParts = path.split("/");
        String waypointId = pathParts[pathParts.length - 1];
        
        WaypointManager manager = WaypointManager.getInstance();
        boolean success = manager.deleteWaypoint(waypointId);
        
        if (success) {
            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("message", "Waypoint deleted successfully");
            sendJsonResponse(exchange, 200, response);
        } else {
            sendErrorResponse(exchange, 404, "Waypoint not found");
        }
    }
    
    /**
     * Parse query parameters
     */
    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new HashMap<>();
        if (query != null && !query.isEmpty()) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2) {
                    params.put(keyValue[0], keyValue[1]);
                }
            }
        }
        return params;
    }
    
    /**
     * Read request body
     */
    private String readRequestBody(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }
    
    /**
     * Get username from session
     */
    private String getUsernameFromSession(HttpExchange exchange) {
        Object username = exchange.getAttribute("username");
        return username != null ? username.toString() : "unknown";
    }
    
    /**
     * Send JSON response
     */
    private void sendJsonResponse(HttpExchange exchange, int statusCode, JsonObject data) throws IOException {
        String response = GSON.toJson(data);
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
    
    /**
     * Send error response
     */
    private void sendErrorResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        JsonObject error = new JsonObject();
        error.addProperty("error", message);
        error.addProperty("timestamp", System.currentTimeMillis());
        sendJsonResponse(exchange, statusCode, error);
    }
}
