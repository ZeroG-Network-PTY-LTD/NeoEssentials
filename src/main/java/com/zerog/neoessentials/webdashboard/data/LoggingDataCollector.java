package com.zerog.neoessentials.webdashboard.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Logging and Analytics Collector
 * Collects logs and analytics data for the Dashboard API
 * 
 * Endpoints served:
 * - Request Logs (API access logs)
 * - Error Logs (server errors and warnings)
 * - Performance Metrics (API performance tracking)
 * - User Activity Tracking (player actions analytics)
 */
public class LoggingDataCollector {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingDataCollector.class);
    private static final int MAX_LOG_ENTRIES = 10000;
    
    // In-memory log storage (consider moving to database for persistence)
    private static final ConcurrentLinkedQueue<LogEntry> requestLogs = new ConcurrentLinkedQueue<>();
    private static final ConcurrentLinkedQueue<LogEntry> errorLogs = new ConcurrentLinkedQueue<>();
    private static final ConcurrentLinkedQueue<PerformanceMetric> performanceMetrics = new ConcurrentLinkedQueue<>();
    
    /**
     * Get recent request logs
     * Endpoint: GET /api/logs/requests?limit=100
     */
    public JsonObject getRequestLogs(int limit) {
        JsonObject response = new JsonObject();
        JsonArray logs = new JsonArray();
        
        List<LogEntry> recentLogs = new ArrayList<>();
        for (LogEntry log : requestLogs) {
            if (recentLogs.size() >= limit) break;
            recentLogs.add(log);
        }
        
        recentLogs.forEach(log -> logs.add(log.toJson()));
        
        response.add("logs", logs);
        response.addProperty("count", logs.size());
        response.addProperty("total", requestLogs.size());
        
        return response;
    }
    
    /**
     * Get recent error logs
     * Endpoint: GET /api/logs/errors?limit=100
     */
    public JsonObject getErrorLogs(int limit, String severity) {
        JsonObject response = new JsonObject();
        JsonArray logs = new JsonArray();
        
        List<LogEntry> recentErrors = new ArrayList<>();
        for (LogEntry log : errorLogs) {
            if (recentErrors.size() >= limit) break;
            if (severity != null && !log.severity.equalsIgnoreCase(severity)) continue;
            recentErrors.add(log);
        }
        
        recentErrors.forEach(log -> logs.add(log.toJson()));
        
        response.add("logs", logs);
        response.addProperty("count", logs.size());
        response.addProperty("total", errorLogs.size());
        
        return response;
    }
    
    /**
     * Get performance metrics
     * Endpoint: GET /api/logs/performance
     */
    public JsonObject getPerformanceMetrics() {
        JsonObject response = new JsonObject();
        JsonArray metrics = new JsonArray();
        
        // Calculate average response times per endpoint
        List<PerformanceMetric> recentMetrics = new ArrayList<>(performanceMetrics);
        
        recentMetrics.forEach(metric -> metrics.add(metric.toJson()));
        
        // Calculate statistics
        if (!recentMetrics.isEmpty()) {
            double avgResponseTime = recentMetrics.stream()
                .mapToLong(m -> m.responseTimeMs)
                .average()
                .orElse(0.0);
            
            long maxResponseTime = recentMetrics.stream()
                .mapToLong(m -> m.responseTimeMs)
                .max()
                .orElse(0);
            
            long minResponseTime = recentMetrics.stream()
                .mapToLong(m -> m.responseTimeMs)
                .min()
                .orElse(0);
            
            response.addProperty("averageResponseTime", avgResponseTime);
            response.addProperty("maxResponseTime", maxResponseTime);
            response.addProperty("minResponseTime", minResponseTime);
        }
        
        response.add("metrics", metrics);
        response.addProperty("count", metrics.size());
        
        return response;
    }
    
    /**
     * Get user activity analytics
     * Endpoint: GET /api/logs/activity
     */
    public JsonObject getUserActivity() {
        JsonObject activity = new JsonObject();
        
        // Analyze request logs for user activity
        long oneHourAgo = Instant.now().toEpochMilli() - (60 * 60 * 1000);
        
        int totalRequests = 0;
        int uniqueUsers = 0;
        int successfulRequests = 0;
        int failedRequests = 0;
        
        for (LogEntry log : requestLogs) {
            if (log.timestamp < oneHourAgo) continue;
            
            totalRequests++;
            if (log.statusCode >= 200 && log.statusCode < 300) {
                successfulRequests++;
            } else if (log.statusCode >= 400) {
                failedRequests++;
            }
        }
        
        activity.addProperty("totalRequests", totalRequests);
        activity.addProperty("uniqueUsers", uniqueUsers); // FUTURE: Track unique users via session tokens
        activity.addProperty("successfulRequests", successfulRequests);
        activity.addProperty("failedRequests", failedRequests);
        activity.addProperty("period", "last_hour");
        
        return activity;
    }
    
    /**
     * Get server log file contents
     * Endpoint: GET /api/logs/server?lines=100
     */
    public JsonObject getServerLogs(int lines) {
        JsonObject response = new JsonObject();
        JsonArray logs = new JsonArray();
        
        File logFile = new File("logs/latest.log");
        if (logFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
                List<String> allLines = new ArrayList<>();
                String line;
                while ((line = reader.readLine()) != null) {
                    allLines.add(line);
                }
                
                // Get last N lines
                int startIndex = Math.max(0, allLines.size() - lines);
                for (int i = startIndex; i < allLines.size(); i++) {
                    logs.add(allLines.get(i));
                }
                
                response.add("logs", logs);
                response.addProperty("count", logs.size());
                response.addProperty("totalLines", allLines.size());
            } catch (IOException e) {
                NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "Failed to read server log file", e);
                response.addProperty("error", "Failed to read log file: " + e.getMessage());
            }
        } else {
            response.addProperty("error", "Log file not found");
        }
        
        return response;
    }
    
    /**
     * Get error statistics
     * Endpoint: GET /api/logs/error-stats
     */
    public JsonObject getErrorStatistics() {
        JsonObject stats = new JsonObject();
        
        int criticalErrors = 0;
        int warnings = 0;
        int errors = 0;
        
        for (LogEntry log : errorLogs) {
            switch (log.severity.toLowerCase()) {
                case "critical" -> criticalErrors++;
                case "error" -> errors++;
                case "warning" -> warnings++;
            }
        }
        
        stats.addProperty("critical", criticalErrors);
        stats.addProperty("errors", errors);
        stats.addProperty("warnings", warnings);
        stats.addProperty("total", errorLogs.size());
        
        return stats;
    }
    
    // Logging methods (called by API endpoints)
    
    public static void logRequest(String endpoint, String method, int statusCode, 
                                   long responseTimeMs, String username) {
        LogEntry entry = new LogEntry(
            "REQUEST",
            endpoint + " " + method + " -> " + statusCode,
            "INFO",
            statusCode,
            username
        );
        
        requestLogs.offer(entry);
        if (requestLogs.size() > MAX_LOG_ENTRIES) {
            requestLogs.poll();
        }
        
        // Also log performance metric
        PerformanceMetric metric = new PerformanceMetric(
            endpoint,
            method,
            responseTimeMs,
            statusCode
        );
        
        performanceMetrics.offer(metric);
        if (performanceMetrics.size() > MAX_LOG_ENTRIES) {
            performanceMetrics.poll();
        }
    }
    
    public static void logError(String message, String severity, Exception exception) {
        String fullMessage = message;
        if (exception != null) {
            fullMessage += ": " + exception.getMessage();
        }
        
        LogEntry entry = new LogEntry(
            "ERROR",
            fullMessage,
            severity,
            500,
            "system"
        );
        
        errorLogs.offer(entry);
        if (errorLogs.size() > MAX_LOG_ENTRIES) {
            errorLogs.poll();
        }
        
        NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, message, exception);
    }
    
    /**
     * Clear log history (admin action)
     * Endpoint: DELETE /api/logs/clear
     */
    public void clearLogs(String logType) {
        switch (logType.toLowerCase()) {
            case "requests" -> {
                requestLogs.clear();
                NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Request logs cleared");
            }
            case "errors" -> {
                errorLogs.clear();
                NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Error logs cleared");
            }
            case "performance" -> {
                performanceMetrics.clear();
                NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Performance metrics cleared");
            }
            case "all" -> {
                requestLogs.clear();
                errorLogs.clear();
                performanceMetrics.clear();
                NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "All logs cleared");
            }
        }
    }
    
    // Inner classes
    
    public static class LogEntry {
        public final String type;
        public final String message;
        public final String severity;
        public final int statusCode;
        public final String username;
        public final long timestamp;
        
        public LogEntry(String type, String message, String severity, int statusCode, String username) {
            this.type = type;
            this.message = message;
            this.severity = severity;
            this.statusCode = statusCode;
            this.username = username;
            this.timestamp = Instant.now().toEpochMilli();
        }
        
        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("type", type);
            json.addProperty("message", message);
            json.addProperty("severity", severity);
            json.addProperty("statusCode", statusCode);
            json.addProperty("username", username);
            json.addProperty("timestamp", timestamp);
            return json;
        }
    }
    
    public static class PerformanceMetric {
        public final String endpoint;
        public final String method;
        public final long responseTimeMs;
        public final int statusCode;
        public final long timestamp;
        
        public PerformanceMetric(String endpoint, String method, long responseTimeMs, int statusCode) {
            this.endpoint = endpoint;
            this.method = method;
            this.responseTimeMs = responseTimeMs;
            this.statusCode = statusCode;
            this.timestamp = Instant.now().toEpochMilli();
        }
        
        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("endpoint", endpoint);
            json.addProperty("method", method);
            json.addProperty("responseTime", responseTimeMs);
            json.addProperty("statusCode", statusCode);
            json.addProperty("timestamp", timestamp);
            return json;
        }
    }
}
