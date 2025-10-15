package com.zerog.neoessentials.logs;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API handler for log viewing and searching.
 * Provides endpoints for tailing, searching, and downloading logs.
 */
public class LogHandler implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(LogHandler.class);
    private final Gson gson = new Gson();
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        
        try {
            // Remove /api/logs prefix
            String endpoint = path.replace("/api/logs", "");
            
            switch (endpoint) {
                case "/tail":
                    if ("GET".equals(method)) {
                        handleTail(exchange);
                    } else {
                        sendMethodNotAllowed(exchange);
                    }
                    break;
                    
                case "/search":
                    if ("GET".equals(method)) {
                        handleSearch(exchange);
                    } else {
                        sendMethodNotAllowed(exchange);
                    }
                    break;
                    
                case "/files":
                    if ("GET".equals(method)) {
                        handleListFiles(exchange);
                    } else {
                        sendMethodNotAllowed(exchange);
                    }
                    break;
                    
                case "/download":
                    if ("GET".equals(method)) {
                        handleDownload(exchange);
                    } else {
                        sendMethodNotAllowed(exchange);
                    }
                    break;
                    
                case "/stats":
                    if ("GET".equals(method)) {
                        handleStats(exchange);
                    } else {
                        sendMethodNotAllowed(exchange);
                    }
                    break;
                    
                default:
                    sendNotFound(exchange);
                    break;
            }
        } catch (Exception e) {
            LOGGER.error("Error handling log request", e);
            sendError(exchange, "Internal server error: " + e.getMessage());
        }
    }
    
    /**
     * GET /api/logs/tail?lines=100
     * Get the most recent log lines
     */
    private void handleTail(HttpExchange exchange) throws IOException {
        Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
        int lineCount = Integer.parseInt(params.getOrDefault("lines", "100"));
        
        // Limit to reasonable range
        lineCount = Math.max(1, Math.min(lineCount, 10000));
        
        List<LogManager.LogEntry> entries = LogManager.getInstance().tailLog(lineCount);
        
        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.addProperty("lineCount", entries.size());
        response.addProperty("timestamp", Instant.now().toString());
        
        JsonArray logsArray = new JsonArray();
        for (LogManager.LogEntry entry : entries) {
            logsArray.add(logEntryToJson(entry));
        }
        response.add("logs", logsArray);
        
        sendJsonResponse(exchange, 200, response);
    }
    
    /**
     * GET /api/logs/search?query=error&level=ERROR&regex=false&caseSensitive=false&maxResults=500
     * Search logs with filters
     */
    private void handleSearch(HttpExchange exchange) throws IOException {
        Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
        
        String query = params.get("query");
        String level = params.get("level");
        boolean useRegex = Boolean.parseBoolean(params.getOrDefault("regex", "false"));
        boolean caseSensitive = Boolean.parseBoolean(params.getOrDefault("caseSensitive", "false"));
        int maxResults = Integer.parseInt(params.getOrDefault("maxResults", "500"));
        
        // Limit max results
        maxResults = Math.max(1, Math.min(maxResults, 5000));
        
        List<LogManager.LogEntry> results = LogManager.getInstance().searchLogs(
            query, level, useRegex, caseSensitive, maxResults
        );
        
        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.addProperty("resultCount", results.size());
        response.addProperty("query", query);
        response.addProperty("level", level);
        response.addProperty("useRegex", useRegex);
        response.addProperty("caseSensitive", caseSensitive);
        response.addProperty("timestamp", Instant.now().toString());
        
        JsonArray resultsArray = new JsonArray();
        for (LogManager.LogEntry entry : results) {
            resultsArray.add(logEntryToJson(entry));
        }
        response.add("results", resultsArray);
        
        sendJsonResponse(exchange, 200, response);
    }
    
    /**
     * GET /api/logs/files
     * List available log files
     */
    private void handleListFiles(HttpExchange exchange) throws IOException {
        List<LogManager.LogFileInfo> files = LogManager.getInstance().getLogFiles();
        
        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.addProperty("fileCount", files.size());
        
        JsonArray filesArray = new JsonArray();
        for (LogManager.LogFileInfo file : files) {
            JsonObject fileObj = new JsonObject();
            fileObj.addProperty("name", file.getName());
            fileObj.addProperty("size", file.getSize());
            fileObj.addProperty("sizeFormatted", formatFileSize(file.getSize()));
            fileObj.addProperty("modified", file.getModified().toString());
            fileObj.addProperty("compressed", file.isCompressed());
            fileObj.addProperty("latest", file.isLatest());
            filesArray.add(fileObj);
        }
        response.add("files", filesArray);
        
        sendJsonResponse(exchange, 200, response);
    }
    
    /**
     * GET /api/logs/download?file=latest.log
     * Download a log file
     */
    private void handleDownload(HttpExchange exchange) throws IOException {
        Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
        String fileName = params.get("file");
        
        if (fileName == null || fileName.isEmpty()) {
            sendBadRequest(exchange, "Missing 'file' parameter");
            return;
        }
        
        try {
            byte[] content = LogManager.getInstance().getLogFileContent(fileName);
            
            // Set headers for file download
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            exchange.getResponseHeaders().set("Content-Disposition", 
                "attachment; filename=\"" + fileName + "\"");
            
            exchange.sendResponseHeaders(200, content.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(content);
            }
            
        } catch (SecurityException e) {
            sendBadRequest(exchange, "Invalid file path");
        } catch (IOException e) {
            sendNotFound(exchange);
        }
    }
    
    /**
     * GET /api/logs/stats
     * Get log file statistics
     */
    private void handleStats(HttpExchange exchange) throws IOException {
        LogManager.LogStats stats = LogManager.getInstance().getLogStats();
        
        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.addProperty("fileSize", stats.getFileSize());
        response.addProperty("fileSizeFormatted", formatFileSize(stats.getFileSize()));
        response.addProperty("lineCount", stats.getLineCount());
        
        JsonObject levelCounts = new JsonObject();
        for (Map.Entry<String, Long> entry : stats.getLevelCounts().entrySet()) {
            levelCounts.addProperty(entry.getKey(), entry.getValue());
        }
        response.add("levelCounts", levelCounts);
        
        sendJsonResponse(exchange, 200, response);
    }
    
    /**
     * Convert LogEntry to JSON
     */
    private JsonObject logEntryToJson(LogManager.LogEntry entry) {
        JsonObject obj = new JsonObject();
        obj.addProperty("timestamp", entry.getTimestamp());
        obj.addProperty("level", entry.getLevel());
        obj.addProperty("thread", entry.getThread());
        obj.addProperty("logger", entry.getLogger());
        obj.addProperty("message", entry.getMessage());
        obj.addProperty("lineNumber", entry.getLineNumber());
        return obj;
    }
    
    /**
     * Format file size in human-readable format
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "B";
        return String.format("%.2f %s", bytes / Math.pow(1024, exp), pre);
    }
    
    /**
     * Parse query parameters from URL
     */
    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isEmpty()) {
            return params;
        }
        
        for (String param : query.split("&")) {
            String[] pair = param.split("=", 2);
            if (pair.length == 2) {
                try {
                    String key = URLDecoder.decode(pair[0], StandardCharsets.UTF_8);
                    String value = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                    params.put(key, value);
                } catch (Exception e) {
                    LOGGER.warn("Failed to decode parameter: {}", param);
                }
            }
        }
        
        return params;
    }
    
    private void sendJsonResponse(HttpExchange exchange, int statusCode, JsonObject response) throws IOException {
        String jsonResponse = gson.toJson(response);
        byte[] bytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
        
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
    
    private void sendMethodNotAllowed(HttpExchange exchange) throws IOException {
        JsonObject response = new JsonObject();
        response.addProperty("success", false);
        response.addProperty("error", "Method not allowed");
        response.addProperty("timestamp", Instant.now().toString());
        sendJsonResponse(exchange, 405, response);
    }
    
    private void sendNotFound(HttpExchange exchange) throws IOException {
        JsonObject response = new JsonObject();
        response.addProperty("success", false);
        response.addProperty("error", "Resource not found");
        response.addProperty("timestamp", Instant.now().toString());
        sendJsonResponse(exchange, 404, response);
    }
    
    private void sendBadRequest(HttpExchange exchange, String message) throws IOException {
        JsonObject response = new JsonObject();
        response.addProperty("success", false);
        response.addProperty("error", message);
        response.addProperty("timestamp", Instant.now().toString());
        sendJsonResponse(exchange, 400, response);
    }
    
    private void sendError(HttpExchange exchange, String message) throws IOException {
        JsonObject response = new JsonObject();
        response.addProperty("success", false);
        response.addProperty("error", message);
        response.addProperty("timestamp", Instant.now().toString());
        sendJsonResponse(exchange, 500, response);
    }
}
