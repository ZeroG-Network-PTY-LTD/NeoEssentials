package com.zerog.neoessentials.webdashboard.handlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Handler for /api/config endpoint
 * GET: Returns list of editable config files with their options
 * POST: Updates config file settings
 */
public class ConfigHandler implements HttpHandler {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = Paths.get("config", "neoessentials");
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Add CORS headers
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        
        // Handle OPTIONS preflight
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }
        
        try {
            if ("GET".equals(exchange.getRequestMethod())) {
                handleGetConfig(exchange);
            } else if ("POST".equals(exchange.getRequestMethod())) {
                handlePostConfig(exchange);
            } else {
                sendJsonResponse(exchange, 405, createErrorResponse("Method not allowed"));
            }
        } catch (Exception e) {
            sendJsonResponse(exchange, 500, createErrorResponse("Internal server error: " + e.getMessage()));
        }
    }
    
    /**
     * Handle GET request - list all config files
     */
    private void handleGetConfig(HttpExchange exchange) throws IOException {
        JsonObject response = new JsonObject();
        JsonArray configsArray = new JsonArray();
        
        try {
            if (Files.exists(CONFIG_DIR)) {
                List<Path> configFiles = listConfigFiles(CONFIG_DIR);
                
                for (Path configFile : configFiles) {
                    JsonObject configObj = new JsonObject();
                    configObj.addProperty("name", configFile.getFileName().toString());
                    configObj.addProperty("path", CONFIG_DIR.relativize(configFile).toString());
                    
                    // Read and parse config file
                    try {
                        String content = Files.readString(configFile, StandardCharsets.UTF_8);
                        JsonObject configData = JsonParser.parseString(content).getAsJsonObject();
                        
                        // Create options array from config data
                        JsonArray optionsArray = new JsonArray();
                        configData.entrySet().forEach(entry -> {
                            JsonObject option = new JsonObject();
                            option.addProperty("key", entry.getKey());
                            option.addProperty("label", formatLabel(entry.getKey()));
                            
                            // Determine type and value
                            if (entry.getValue().isJsonPrimitive()) {
                                if (entry.getValue().getAsJsonPrimitive().isBoolean()) {
                                    option.addProperty("type", "toggle");
                                    option.addProperty("value", entry.getValue().getAsBoolean());
                                } else if (entry.getValue().getAsJsonPrimitive().isNumber()) {
                                    option.addProperty("type", "number");
                                    option.addProperty("value", entry.getValue().getAsNumber());
                                } else {
                                    option.addProperty("type", "text");
                                    option.addProperty("value", entry.getValue().getAsString());
                                }
                            } else if (entry.getValue().isJsonArray()) {
                                option.addProperty("type", "array");
                                option.add("value", entry.getValue());
                            } else {
                                option.addProperty("type", "object");
                                option.add("value", entry.getValue());
                            }
                            
                            optionsArray.add(option);
                        });
                        
                        configObj.add("options", optionsArray);
                        configsArray.add(configObj);
                        
                    } catch (Exception e) {
                        // If parsing fails, skip this file
                        configObj.addProperty("error", "Failed to parse config file");
                        configObj.add("options", new JsonArray());
                        configsArray.add(configObj);
                    }
                }
                
                response.addProperty("success", true);
            } else {
                response.addProperty("success", false);
                response.addProperty("message", "Config directory not found");
            }
        } catch (Exception e) {
            response.addProperty("success", false);
            response.addProperty("error", e.getMessage());
        }
        
        response.add("configs", configsArray);
        response.addProperty("count", configsArray.size());
        response.addProperty("timestamp", System.currentTimeMillis());
        
        sendJsonResponse(exchange, 200, response);
    }
    
    /**
     * Handle POST request - update config file
     */
    private void handlePostConfig(HttpExchange exchange) throws IOException {
        // Read request body
        String requestBody;
        try (InputStream is = exchange.getRequestBody()) {
            requestBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
        
        JsonObject response = new JsonObject();
        
        try {
            JsonObject requestData = JsonParser.parseString(requestBody).getAsJsonObject();
            
            if (!requestData.has("file") || !requestData.has("config")) {
                sendJsonResponse(exchange, 400, createErrorResponse("Missing required fields: file, config"));
                return;
            }
            
            String fileName = requestData.get("file").getAsString();
            JsonObject newConfig = requestData.getAsJsonObject("config");
            
            // Validate file name (security check)
            if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
                sendJsonResponse(exchange, 400, createErrorResponse("Invalid file name"));
                return;
            }
            
            Path configFile = CONFIG_DIR.resolve(fileName);
            
            if (!Files.exists(configFile)) {
                sendJsonResponse(exchange, 404, createErrorResponse("Config file not found"));
                return;
            }
            
            // Write updated config
            String jsonContent = GSON.toJson(newConfig);
            Files.writeString(configFile, jsonContent, StandardCharsets.UTF_8);
            
            response.addProperty("success", true);
            response.addProperty("message", "Config updated successfully");
            response.addProperty("file", fileName);
            
            sendJsonResponse(exchange, 200, response);
            
        } catch (Exception e) {
            sendJsonResponse(exchange, 500, createErrorResponse("Failed to update config: " + e.getMessage()));
        }
    }
    
    /**
     * List all JSON config files in directory
     */
    private List<Path> listConfigFiles(Path directory) throws IOException {
        try (Stream<Path> paths = Files.walk(directory)) {
            return paths
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".json"))
                .collect(Collectors.toList());
        }
    }
    
    /**
     * Format config key as human-readable label
     */
    private String formatLabel(String key) {
        // Convert camelCase or snake_case to Title Case
        String formatted = key.replaceAll("([A-Z])", " $1")
                              .replaceAll("_", " ")
                              .trim();
        
        // Capitalize first letter of each word
        String[] words = formatted.split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0) {
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1).toLowerCase())
                      .append(" ");
            }
        }
        
        return result.toString().trim();
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
