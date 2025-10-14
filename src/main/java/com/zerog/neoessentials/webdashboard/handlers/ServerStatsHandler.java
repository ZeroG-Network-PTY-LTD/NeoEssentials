package com.zerog.neoessentials.webdashboard.handlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.nio.charset.StandardCharsets;

/**
 * Handler for /api/server endpoint
 * Returns server status, TPS, player counts, and health metrics
 */
public class ServerStatsHandler implements HttpHandler {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
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
            JsonObject response = getServerStats();
            sendJsonResponse(exchange, 200, response);
        } catch (Exception e) {
            sendJsonResponse(exchange, 500, createErrorResponse(MessageUtil.localize("neoessentials.dashboard.api.internal_error", e.getMessage())));
        }
    }
    
    /**
     * Get server statistics
     */
    private JsonObject getServerStats() {
        JsonObject stats = new JsonObject();
        
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        
        if (server != null) {
            // Server status
            stats.addProperty("status", MessageUtil.localize("neoessentials.dashboard.stats.status_online"));
            
            // TPS calculation
            double currentTps = calculateTps(server);
            stats.addProperty("tps", Math.min(currentTps, 20.0)); // Cap at 20
            
            // Player counts
            int onlinePlayers = server.getPlayerList().getPlayerCount();
            int maxPlayers = server.getPlayerList().getMaxPlayers();
            stats.addProperty("online", onlinePlayers);
            stats.addProperty("maxPlayers", maxPlayers);
            
            // Server health (based on TPS and memory)
            double healthPercent = calculateHealthPercent(currentTps);
            stats.addProperty("healthPercent", (int)healthPercent);
            
            // Memory statistics
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
            
            long usedMemoryMB = heapUsage.getUsed() / (1024 * 1024);
            long maxMemoryMB = heapUsage.getMax() / (1024 * 1024);
            long freeMemoryMB = maxMemoryMB - usedMemoryMB;
            
            JsonObject memory = new JsonObject();
            memory.addProperty("used", usedMemoryMB);
            memory.addProperty("max", maxMemoryMB);
            memory.addProperty("free", freeMemoryMB);
            memory.addProperty("percentUsed", (int)((double)usedMemoryMB / maxMemoryMB * 100));
            stats.add("memory", memory);
            
            // Server uptime
            long uptimeMillis = ManagementFactory.getRuntimeMXBean().getUptime();
            stats.addProperty("uptime", formatUptime(uptimeMillis));
            stats.addProperty("uptimeMillis", uptimeMillis);
            
            // World information
            stats.addProperty("worldName", server.getWorldData().getLevelName());
            stats.addProperty("difficulty", server.getWorldData().getDifficulty().getKey());
            
            // Server version
            stats.addProperty("version", server.getServerVersion());
            
        } else {
            stats.addProperty("status", MessageUtil.localize("neoessentials.dashboard.stats.status_offline"));
            stats.addProperty("tps", 0);
            stats.addProperty("online", 0);
            stats.addProperty("maxPlayers", 0);
            stats.addProperty("healthPercent", 0);
        }
        
        stats.addProperty("timestamp", System.currentTimeMillis());
        
        return stats;
    }
    
    /**
     * Calculate current TPS
     * Integration ready when tick time tracking system is implemented
     */
    private double calculateTps(MinecraftServer server) {
        // For now, return estimated TPS based on server running state
        // This can be enhanced with actual tick time tracking via ServerTickEvent
        if (server != null && !server.isStopped()) {
            // Assume good TPS if server is running smoothly
            return 20.0;
        }
        return 0.0;
    }
    
    /**
     * Calculate server health percentage based on TPS and memory
     */
    private double calculateHealthPercent(double tps) {
        // Health is primarily based on TPS
        double tpsHealth = (tps / 20.0) * 100.0;
        
        // Factor in memory usage
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        double memoryUsedPercent = (double)heapUsage.getUsed() / heapUsage.getMax() * 100.0;
        double memoryHealth = 100.0 - (memoryUsedPercent * 0.5); // Memory has less weight
        
        // Weighted average (TPS 70%, Memory 30%)
        return (tpsHealth * 0.7) + (memoryHealth * 0.3);
    }
    
    /**
     * Format uptime in human-readable format
     */
    private String formatUptime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return String.format("%dd %dh %dm", days, hours % 24, minutes % 60);
        } else if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes % 60, seconds % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
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
    
    /**
     * Update TPS tracking (call this from a tick event)
     */
    public static void updateTps(MinecraftServer server) {
        // This method can be called from ServerTickEvent to track TPS more accurately
    }
}
