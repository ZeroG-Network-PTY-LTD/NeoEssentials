package com.zerog.neoessentials.webdashboard.api.endpoints;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.zerog.neoessentials.webdashboard.data.LoggingDataCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Handles all logging-related API endpoints
 */
public class LoggingEndpoint implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingEndpoint.class);
    private final LoggingDataCollector loggingCollector;
    
    public LoggingEndpoint() {
        this.loggingCollector = new LoggingDataCollector();
    }
    
    @Override
    public void handle(HttpExchange exchange) {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        
        LOGGER.info("LoggingEndpoint handling request: {} {}", method, path);
        
        try {
            // Only allow GET requests
            if (!"GET".equals(method)) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }
            
            JsonObject response;
            
            // Parse path to determine which endpoint
            if (path.equals("/api/logging/requests")) {
                response = loggingCollector.getRequestLogs(100);
            } else if (path.equals("/api/logging/errors")) {
                response = loggingCollector.getErrorLogs(100, "ALL");
            } else if (path.equals("/api/logging/performance")) {
                response = loggingCollector.getPerformanceMetrics();
            } else {
                sendResponse(exchange, 404, "{\"error\":\"Endpoint not found\"}");
                return;
            }
            
            sendResponse(exchange, 200, response.toString());
            
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
