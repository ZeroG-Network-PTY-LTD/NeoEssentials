package com.zerog.neoessentials.webdashboard.analytics;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * REST API handler for player analytics endpoints
 * Provides analytics data, statistics, retention, and activity heatmaps
 */
public class AnalyticsHandler implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnalyticsHandler.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        
        try {
            if ("GET".equals(method)) {
                if (path.endsWith("/analytics/active")) {
                    handleGetActiveSessions(exchange);
                } else if (path.endsWith("/analytics/report")) {
                    handleGetAnalyticsReport(exchange);
                } else if (path.endsWith("/analytics/retention")) {
                    handleGetRetentionReport(exchange);
                } else if (path.endsWith("/analytics/daily")) {
                    handleGetDailyStats(exchange);
                } else if (path.endsWith("/analytics/heatmap")) {
                    handleGetActivityHeatmap(exchange);
                } else {
                    sendErrorResponse(exchange, 404, "Endpoint not found");
                }
            } else {
                sendErrorResponse(exchange, 405, "Method not allowed");
            }
        } catch (Exception e) {
            LOGGER.error("Error handling analytics request", e);
            sendErrorResponse(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }
    
    /**
     * GET /api/analytics/active - Get active player sessions
     */
    private void handleGetActiveSessions(HttpExchange exchange) throws IOException {
        PlayerSessionTracker tracker = PlayerSessionTracker.getInstance();
        
        JsonObject response = new JsonObject();
        response.addProperty("activePlayerCount", tracker.getActivePlayerCount());
        response.addProperty("timestamp", System.currentTimeMillis());
        
        // Convert active sessions to JSON array
        List<Map<String, Object>> sessions = new ArrayList<>();
        for (PlayerSessionTracker.PlayerSession session : tracker.getActiveSessions()) {
            Map<String, Object> sessionData = new HashMap<>();
            sessionData.put("playerId", session.playerId.toString());
            sessionData.put("playerName", session.playerName);
            sessionData.put("joinTime", session.joinTime);
            sessionData.put("duration", System.currentTimeMillis() - session.joinTime);
            sessionData.put("lastActivity", session.lastActivity);
            sessions.add(sessionData);
        }
        
        response.add("activeSessions", GSON.toJsonTree(sessions));
        
        sendJsonResponse(exchange, 200, response);
    }
    
    /**
     * GET /api/analytics/report?days=7 - Get analytics report for time range
     */
    private void handleGetAnalyticsReport(HttpExchange exchange) throws IOException {
        // Parse query parameters
        Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
        int days = Integer.parseInt(params.getOrDefault("days", "7"));
        
        long endTime = System.currentTimeMillis();
        long startTime = endTime - (days * 24L * 60 * 60 * 1000);
        
        PlayerSessionTracker tracker = PlayerSessionTracker.getInstance();
        PlayerSessionTracker.AnalyticsReport report = tracker.getAnalyticsReport(startTime, endTime);
        
        JsonObject response = new JsonObject();
        response.addProperty("startTime", report.startTime);
        response.addProperty("endTime", report.endTime);
        response.addProperty("generatedAt", report.generatedAt);
        response.addProperty("days", days);
        response.addProperty("uniquePlayers", report.uniquePlayers);
        response.addProperty("totalSessions", report.totalSessions);
        response.addProperty("totalPlaytime", report.totalPlaytime);
        response.addProperty("totalPlaytimeHours", report.totalPlaytime / (1000.0 * 60 * 60));
        response.addProperty("averageSessionDuration", report.averageSessionDuration);
        response.addProperty("averageSessionMinutes", report.averageSessionDuration / (1000.0 * 60));
        response.addProperty("peakHour", report.peakHour);
        response.add("hourlyActivity", GSON.toJsonTree(report.hourlyActivity));
        
        sendJsonResponse(exchange, 200, response);
    }
    
    /**
     * GET /api/analytics/retention?days=30 - Get retention statistics
     */
    private void handleGetRetentionReport(HttpExchange exchange) throws IOException {
        // Parse query parameters
        Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
        int days = Integer.parseInt(params.getOrDefault("days", "30"));
        
        PlayerSessionTracker tracker = PlayerSessionTracker.getInstance();
        PlayerSessionTracker.RetentionReport report = tracker.getRetentionReport(days);
        
        JsonObject response = new JsonObject();
        response.addProperty("days", report.days);
        response.addProperty("generatedAt", report.generatedAt);
        response.addProperty("totalPlayersStart", report.totalPlayersStart);
        response.addProperty("totalPlayersEnd", report.totalPlayersEnd);
        response.addProperty("retainedPlayers", report.retainedPlayers);
        response.addProperty("retentionRate", report.retentionRate);
        response.add("dailyPlayerCounts", GSON.toJsonTree(report.dailyPlayerCounts));
        
        sendJsonResponse(exchange, 200, response);
    }
    
    /**
     * GET /api/analytics/daily?days=30 - Get daily statistics
     */
    private void handleGetDailyStats(HttpExchange exchange) throws IOException {
        // Parse query parameters
        Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
        int days = Integer.parseInt(params.getOrDefault("days", "30"));
        
        PlayerSessionTracker tracker = PlayerSessionTracker.getInstance();
        Map<String, PlayerSessionTracker.DailyStats> dailyStats = tracker.getDailyStats(days);
        
        JsonObject response = new JsonObject();
        response.addProperty("days", days);
        response.addProperty("generatedAt", System.currentTimeMillis());
        
        List<Map<String, Object>> statsArray = new ArrayList<>();
        for (Map.Entry<String, PlayerSessionTracker.DailyStats> entry : dailyStats.entrySet()) {
            PlayerSessionTracker.DailyStats stats = entry.getValue();
            Map<String, Object> statsData = new HashMap<>();
            statsData.put("date", stats.date);
            statsData.put("uniquePlayers", stats.uniquePlayers.size());
            statsData.put("totalJoins", stats.totalJoins);
            statsData.put("totalSessions", stats.totalSessions);
            statsData.put("totalPlaytime", stats.totalPlaytime);
            statsData.put("totalPlaytimeHours", stats.totalPlaytime / (1000.0 * 60 * 60));
            statsData.put("averageSessionDuration", stats.averageSessionDuration);
            statsData.put("averageSessionMinutes", stats.averageSessionDuration / (1000.0 * 60));
            statsData.put("peakOnline", stats.peakOnline);
            statsArray.add(statsData);
        }
        
        response.add("dailyStats", GSON.toJsonTree(statsArray));
        
        sendJsonResponse(exchange, 200, response);
    }
    
    /**
     * GET /api/analytics/heatmap?days=7 - Get activity heatmap data
     */
    private void handleGetActivityHeatmap(HttpExchange exchange) throws IOException {
        // Parse query parameters
        Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
        int days = Integer.parseInt(params.getOrDefault("days", "7"));
        
        long endTime = System.currentTimeMillis();
        long startTime = endTime - (days * 24L * 60 * 60 * 1000);
        
        PlayerSessionTracker tracker = PlayerSessionTracker.getInstance();
        PlayerSessionTracker.AnalyticsReport report = tracker.getAnalyticsReport(startTime, endTime);
        
        // Create heatmap data structure (hour of day -> activity count)
        Map<Integer, Integer> hourlyActivity = report.hourlyActivity;
        
        // Find peak and minimum activity
        int peakActivity = hourlyActivity.values().stream().max(Integer::compare).orElse(0);
        int minActivity = hourlyActivity.values().stream().min(Integer::compare).orElse(0);
        
        JsonObject response = new JsonObject();
        response.addProperty("days", days);
        response.addProperty("startTime", startTime);
        response.addProperty("endTime", endTime);
        response.addProperty("generatedAt", System.currentTimeMillis());
        response.addProperty("peakHour", report.peakHour);
        response.addProperty("peakActivity", peakActivity);
        response.addProperty("minActivity", minActivity);
        
        // Format heatmap data for frontend
        List<Map<String, Object>> heatmapData = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            Map<String, Object> hourData = new HashMap<>();
            hourData.put("hour", hour);
            hourData.put("activity", hourlyActivity.getOrDefault(hour, 0));
            
            // Calculate intensity (0.0 - 1.0)
            double intensity = peakActivity > 0 ? 
                (double) hourlyActivity.getOrDefault(hour, 0) / peakActivity : 0.0;
            hourData.put("intensity", intensity);
            
            // Categorize activity level
            String level;
            if (intensity >= 0.75) level = "high";
            else if (intensity >= 0.5) level = "medium";
            else if (intensity >= 0.25) level = "low";
            else level = "minimal";
            hourData.put("level", level);
            
            heatmapData.add(hourData);
        }
        
        response.add("heatmap", GSON.toJsonTree(heatmapData));
        
        sendJsonResponse(exchange, 200, response);
    }
    
    /**
     * Parse query parameters from query string
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
