package com.zerog.neoessentials.webdashboard.handlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.zerog.neoessentials.util.MessageUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handler for /api/logs endpoint
 * Returns recent server log lines
 */
public class LogsHandler implements HttpHandler {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int DEFAULT_LOG_LINES = 100;
    private static final int MAX_LOG_LINES = 1000;
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Add CORS headers
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        
        // Handle OPTIONS preflight
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }
        
        // Only allow GET
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendJsonResponse(exchange, 405, createErrorResponse(MessageUtil.localize("neoessentials.dashboard.api.method_not_allowed")));
            return;
        }
        
        try {
            // Parse query parameters
            int lines = DEFAULT_LOG_LINES;
            String query = exchange.getRequestURI().getQuery();
            if (query != null && query.contains("lines=")) {
                try {
                    String linesParam = query.split("lines=")[1].split("&")[0];
                    lines = Integer.parseInt(linesParam);
                    lines = Math.min(lines, MAX_LOG_LINES); // Cap at max
                } catch (Exception e) {
                    // Use default if parsing fails
                }
            }
            
            JsonObject response = getLogData(lines);
            sendJsonResponse(exchange, 200, response);
        } catch (Exception e) {
            sendJsonResponse(exchange, 500, createErrorResponse(MessageUtil.localize("neoessentials.dashboard.api.internal_error", e.getMessage())));
        }
    }
    
    /**
     * Get log data from the server log file
     */
    private JsonObject getLogData(int lineCount) {
        JsonObject response = new JsonObject();
        JsonArray logsArray = new JsonArray();
        
        try {
            // Find the latest log file
            Path logFile = findLatestLogFile();
            
            if (logFile != null && Files.exists(logFile)) {
                List<String> lines = readLastLines(logFile, lineCount);
                
                // Add lines to JSON array
                for (String line : lines) {
                    // Filter out sensitive information
                    String filteredLine = filterSensitiveInfo(line);
                    logsArray.add(filteredLine);
                }
                
                response.addProperty("file", logFile.getFileName().toString());
                response.addProperty("success", true);
            } else {
                response.addProperty("success", false);
                response.addProperty("message", MessageUtil.localize("neoessentials.dashboard.api.log_not_found"));
            }
        } catch (Exception e) {
            response.addProperty("success", false);
            response.addProperty("error", e.getMessage());
        }
        
        response.add("logs", logsArray);
        response.addProperty("count", logsArray.size());
        response.addProperty("timestamp", System.currentTimeMillis());
        
        return response;
    }
    
    /**
     * Find the latest log file
     */
    private Path findLatestLogFile() {
        // Common log file locations
        Path[] possiblePaths = {
            Paths.get("logs", "latest.log"),
            Paths.get("logs", "debug.log"),
            Paths.get(".", "latest.log")
        };
        
        for (Path path : possiblePaths) {
            if (Files.exists(path)) {
                return path;
            }
        }
        
        return null;
    }
    
    /**
     * Read last N lines from a file
     */
    private List<String> readLastLines(Path file, int lineCount) throws IOException {
        List<String> allLines = new ArrayList<>();
        
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                allLines.add(line);
                
                // Keep only the last N lines to prevent memory issues
                if (allLines.size() > lineCount * 2) {
                    allLines = allLines.stream()
                        .skip(allLines.size() - lineCount)
                        .collect(Collectors.toList());
                }
            }
        }
        
        // Return last N lines
        if (allLines.size() > lineCount) {
            return allLines.subList(allLines.size() - lineCount, allLines.size());
        }
        
        return allLines;
    }
    
    /**
     * Filter sensitive information from log lines
     */
    private String filterSensitiveInfo(String line) {
        // Remove IP addresses (basic pattern)
        line = line.replaceAll("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b", "***.***.***.**");
        
        // Remove UUIDs (basic pattern)
        line = line.replaceAll("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}", "********-****-****-****-************");
        
        // Remove potential passwords or tokens
        if (line.toLowerCase().contains("password") || line.toLowerCase().contains("token")) {
            line = line.replaceAll("(?i)(password|token)[=:\\s]+[^\\s,;]+", "$1=***");
        }
        
        return line;
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
