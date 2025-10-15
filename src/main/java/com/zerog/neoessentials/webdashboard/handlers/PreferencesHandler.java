package com.zerog.neoessentials.webdashboard.handlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.stream.Collectors;

/**
 * Handler for /api/preferences endpoint
 * Manages user dashboard layouts, widget configurations, and preferences
 */
public class PreferencesHandler implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(PreferencesHandler.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PREFERENCES_DIR = Paths.get("neoessentials", "preferences");
    
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
            // Ensure preferences directory exists
            if (!Files.exists(PREFERENCES_DIR)) {
                Files.createDirectories(PREFERENCES_DIR);
            }
            
            if ("GET".equals(method) && path.endsWith("/layout")) {
                handleGetLayout(exchange);
            } else if ("POST".equals(method) && path.endsWith("/layout")) {
                handleSaveLayout(exchange);
            } else if ("GET".equals(method) && path.endsWith("/preferences")) {
                handleGetPreferences(exchange);
            } else if ("POST".equals(method) && path.endsWith("/preferences")) {
                handleSavePreferences(exchange);
            } else if ("GET".equals(method) && path.endsWith("/presets")) {
                handleGetPresets(exchange);
            } else if ("POST".equals(method) && path.endsWith("/preset")) {
                handleApplyPreset(exchange);
            } else {
                sendJsonResponse(exchange, 400, createErrorResponse("Invalid endpoint"));
            }
        } catch (Exception e) {
            LOGGER.error("Error handling preferences request", e);
            sendJsonResponse(exchange, 500, createErrorResponse("Internal server error: " + e.getMessage()));
        }
    }
    
    /**
     * GET /api/preferences/layout
     * Retrieve user's saved dashboard layout
     */
    private void handleGetLayout(HttpExchange exchange) throws IOException {
        String username = getUsernameFromSession(exchange);
        if (username == null) {
            sendJsonResponse(exchange, 401, createErrorResponse("Authentication required"));
            return;
        }
        
        Path layoutFile = PREFERENCES_DIR.resolve(username + "_layout.json");
        
        if (!Files.exists(layoutFile)) {
            // Return default layout
            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.add("layout", getDefaultLayout());
            sendJsonResponse(exchange, 200, response);
            return;
        }
        
        try {
            String layoutJson = Files.readString(layoutFile, StandardCharsets.UTF_8);
            JsonObject layout = JsonParser.parseString(layoutJson).getAsJsonObject();
            
            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.add("layout", layout);
            
            sendJsonResponse(exchange, 200, response);
        } catch (Exception e) {
            LOGGER.error("Error reading layout file", e);
            sendJsonResponse(exchange, 500, createErrorResponse("Failed to read layout"));
        }
    }
    
    /**
     * POST /api/preferences/layout
     * Save user's dashboard layout
     * Body: {"widgets": [...], "gridConfig": {...}}
     */
    private void handleSaveLayout(HttpExchange exchange) throws IOException {
        String username = getUsernameFromSession(exchange);
        if (username == null) {
            sendJsonResponse(exchange, 401, createErrorResponse("Authentication required"));
            return;
        }
        
        String requestBody = readRequestBody(exchange);
        
        try {
            JsonObject layout = JsonParser.parseString(requestBody).getAsJsonObject();
            
            // Add metadata
            layout.addProperty("username", username);
            layout.addProperty("savedAt", System.currentTimeMillis());
            
            // Save to file
            Path layoutFile = PREFERENCES_DIR.resolve(username + "_layout.json");
            Files.writeString(layoutFile, GSON.toJson(layout), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            
            LOGGER.info("Saved dashboard layout for user: {}", username);
            
            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("message", "Layout saved successfully");
            
            sendJsonResponse(exchange, 200, response);
        } catch (Exception e) {
            LOGGER.error("Error saving layout", e);
            sendJsonResponse(exchange, 500, createErrorResponse("Failed to save layout"));
        }
    }
    
    /**
     * GET /api/preferences/preferences
     * Retrieve user's dashboard preferences (theme, settings, etc.)
     */
    private void handleGetPreferences(HttpExchange exchange) throws IOException {
        String username = getUsernameFromSession(exchange);
        if (username == null) {
            sendJsonResponse(exchange, 401, createErrorResponse("Authentication required"));
            return;
        }
        
        Path prefsFile = PREFERENCES_DIR.resolve(username + "_preferences.json");
        
        if (!Files.exists(prefsFile)) {
            // Return default preferences
            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.add("preferences", getDefaultPreferences());
            sendJsonResponse(exchange, 200, response);
            return;
        }
        
        try {
            String prefsJson = Files.readString(prefsFile, StandardCharsets.UTF_8);
            JsonObject prefs = JsonParser.parseString(prefsJson).getAsJsonObject();
            
            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.add("preferences", prefs);
            
            sendJsonResponse(exchange, 200, response);
        } catch (Exception e) {
            LOGGER.error("Error reading preferences file", e);
            sendJsonResponse(exchange, 500, createErrorResponse("Failed to read preferences"));
        }
    }
    
    /**
     * POST /api/preferences/preferences
     * Save user's dashboard preferences
     * Body: {"theme": "...", "autoRefresh": true, ...}
     */
    private void handleSavePreferences(HttpExchange exchange) throws IOException {
        String username = getUsernameFromSession(exchange);
        if (username == null) {
            sendJsonResponse(exchange, 401, createErrorResponse("Authentication required"));
            return;
        }
        
        String requestBody = readRequestBody(exchange);
        
        try {
            JsonObject prefs = JsonParser.parseString(requestBody).getAsJsonObject();
            
            // Add metadata
            prefs.addProperty("username", username);
            prefs.addProperty("updatedAt", System.currentTimeMillis());
            
            // Save to file
            Path prefsFile = PREFERENCES_DIR.resolve(username + "_preferences.json");
            Files.writeString(prefsFile, GSON.toJson(prefs), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            
            LOGGER.info("Saved preferences for user: {}", username);
            
            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("message", "Preferences saved successfully");
            
            sendJsonResponse(exchange, 200, response);
        } catch (Exception e) {
            LOGGER.error("Error saving preferences", e);
            sendJsonResponse(exchange, 500, createErrorResponse("Failed to save preferences"));
        }
    }
    
    /**
     * GET /api/preferences/presets
     * Get available layout presets
     */
    private void handleGetPresets(HttpExchange exchange) throws IOException {
        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.add("presets", getLayoutPresets());
        sendJsonResponse(exchange, 200, response);
    }
    
    /**
     * POST /api/preferences/preset
     * Apply a layout preset
     * Body: {"preset": "minimal"}
     */
    private void handleApplyPreset(HttpExchange exchange) throws IOException {
        String username = getUsernameFromSession(exchange);
        if (username == null) {
            sendJsonResponse(exchange, 401, createErrorResponse("Authentication required"));
            return;
        }
        
        String requestBody = readRequestBody(exchange);
        JsonObject request = JsonParser.parseString(requestBody).getAsJsonObject();
        
        if (!request.has("preset")) {
            sendJsonResponse(exchange, 400, createErrorResponse("Missing preset name"));
            return;
        }
        
        String presetName = request.get("preset").getAsString();
        JsonObject layout = getPresetLayout(presetName);
        
        if (layout == null) {
            sendJsonResponse(exchange, 404, createErrorResponse("Preset not found"));
            return;
        }
        
        // Save preset as user's layout
        layout.addProperty("username", username);
        layout.addProperty("savedAt", System.currentTimeMillis());
        
        Path layoutFile = PREFERENCES_DIR.resolve(username + "_layout.json");
        Files.writeString(layoutFile, GSON.toJson(layout), StandardCharsets.UTF_8,
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        
        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.addProperty("message", "Preset applied successfully");
        response.add("layout", layout);
        
        sendJsonResponse(exchange, 200, response);
    }
    
    /**
     * Extract username from session
     */
    private String getUsernameFromSession(HttpExchange exchange) {
        Object usernameAttr = exchange.getAttribute("username");
        return usernameAttr != null ? usernameAttr.toString() : null;
    }
    
    /**
     * Get default dashboard layout
     */
    private JsonObject getDefaultLayout() {
        JsonObject layout = new JsonObject();
        layout.addProperty("preset", "default");
        layout.addProperty("columns", 12);
        layout.addProperty("rowHeight", 80);
        
        // Default widgets will be defined in frontend
        layout.addProperty("widgets", "[]");
        
        return layout;
    }
    
    /**
     * Get default user preferences
     */
    private JsonObject getDefaultPreferences() {
        JsonObject prefs = new JsonObject();
        prefs.addProperty("theme", "space");
        prefs.addProperty("autoRefresh", true);
        prefs.addProperty("refreshInterval", 5000);
        prefs.addProperty("notifications", true);
        prefs.addProperty("soundEffects", false);
        prefs.addProperty("compactMode", false);
        prefs.addProperty("animationsEnabled", true);
        return prefs;
    }
    
    /**
     * Get available layout presets
     */
    private JsonObject getLayoutPresets() {
        JsonObject presets = new JsonObject();
        
        // Default preset
        JsonObject defaultPreset = new JsonObject();
        defaultPreset.addProperty("name", "Default");
        defaultPreset.addProperty("description", "Balanced layout with all widgets visible");
        defaultPreset.addProperty("id", "default");
        
        // Minimal preset
        JsonObject minimalPreset = new JsonObject();
        minimalPreset.addProperty("name", "Minimal");
        minimalPreset.addProperty("description", "Clean layout with essential widgets only");
        minimalPreset.addProperty("id", "minimal");
        
        // Advanced preset
        JsonObject advancedPreset = new JsonObject();
        advancedPreset.addProperty("name", "Advanced");
        advancedPreset.addProperty("description", "Power user layout with detailed metrics");
        advancedPreset.addProperty("id", "advanced");
        
        // Monitoring preset
        JsonObject monitoringPreset = new JsonObject();
        monitoringPreset.addProperty("name", "Monitoring");
        monitoringPreset.addProperty("description", "Focus on server performance and metrics");
        monitoringPreset.addProperty("id", "monitoring");
        
        presets.add("default", defaultPreset);
        presets.add("minimal", minimalPreset);
        presets.add("advanced", advancedPreset);
        presets.add("monitoring", monitoringPreset);
        
        return presets;
    }
    
    /**
     * Get preset layout configuration
     */
    private JsonObject getPresetLayout(String presetName) {
        JsonObject layout = new JsonObject();
        layout.addProperty("preset", presetName);
        layout.addProperty("columns", 12);
        layout.addProperty("rowHeight", 80);
        
        // Preset-specific configurations will be handled by frontend
        // This just returns the base structure
        
        switch (presetName) {
            case "default":
                layout.addProperty("description", "Default balanced layout");
                break;
            case "minimal":
                layout.addProperty("description", "Minimal layout with essential widgets");
                break;
            case "advanced":
                layout.addProperty("description", "Advanced layout for power users");
                break;
            case "monitoring":
                layout.addProperty("description", "Performance monitoring focused layout");
                break;
            default:
                return null;
        }
        
        return layout;
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
