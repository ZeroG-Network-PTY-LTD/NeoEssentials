package com.zerog.neoessentials.webdashboard.websocket;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages real-time data streaming to WebSocket clients
 * Periodically collects and broadcasts server metrics
 */
public class DataStreamManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataStreamManager.class);
    private static DataStreamManager INSTANCE;
    
    private final ScheduledExecutorService scheduler;
    private final DashboardWebSocketServer webSocketServer;
    
    // Stream intervals (in seconds)
    private static final int SERVER_STATS_INTERVAL = 1;
    private static final int PLAYER_UPDATE_INTERVAL = 2;
    private static final int PERFORMANCE_METRICS_INTERVAL = 1;
    private static final int MEMORY_UPDATE_INTERVAL = 5;
    private static final int ENTITY_COUNT_INTERVAL = 10;
    
    // TPS tracking
    private double lastTps = 20.0;
    private long lastTickTime = 0;
    
    private DataStreamManager(DashboardWebSocketServer webSocketServer) {
        this.webSocketServer = webSocketServer;
        this.scheduler = Executors.newScheduledThreadPool(3);
    }
    
    public static DataStreamManager getInstance(DashboardWebSocketServer webSocketServer) {
        if (INSTANCE == null) {
            INSTANCE = new DataStreamManager(webSocketServer);
        }
        return INSTANCE;
    }
    
    public static DataStreamManager getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("DataStreamManager not initialized");
        }
        return INSTANCE;
    }
    
    /**
     * Start all data streaming tasks
     */
    public void startStreaming() {
        LOGGER.info("Starting WebSocket data streaming...");
        
        // Server stats stream
        scheduler.scheduleAtFixedRate(this::streamServerStats, 
            0, SERVER_STATS_INTERVAL, TimeUnit.SECONDS);
        
        // Player updates stream
        scheduler.scheduleAtFixedRate(this::streamPlayerUpdates, 
            0, PLAYER_UPDATE_INTERVAL, TimeUnit.SECONDS);
        
        // Performance metrics stream
        scheduler.scheduleAtFixedRate(this::streamPerformanceMetrics, 
            0, PERFORMANCE_METRICS_INTERVAL, TimeUnit.SECONDS);
        
        // Memory updates stream
        scheduler.scheduleAtFixedRate(this::streamMemoryUpdates, 
            0, MEMORY_UPDATE_INTERVAL, TimeUnit.SECONDS);
        
        // Entity count stream
        scheduler.scheduleAtFixedRate(this::streamEntityCounts, 
            0, ENTITY_COUNT_INTERVAL, TimeUnit.SECONDS);
        
        LOGGER.info("WebSocket data streaming started successfully");
    }
    
    /**
     * Stop all data streaming tasks
     */
    public void stopStreaming() {
        LOGGER.info("Stopping WebSocket data streaming...");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        LOGGER.info("WebSocket data streaming stopped");
    }
    
    /**
     * Stream server statistics
     */
    private void streamServerStats() {
        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) return;
            
            JsonObject data = new JsonObject();
            data.addProperty("type", "server_stats");
            data.addProperty("tps", Math.min(lastTps, 20.0));
            data.addProperty("online", server.getPlayerList().getPlayerCount());
            data.addProperty("maxPlayers", server.getPlayerList().getMaxPlayers());
            data.addProperty("tickTime", lastTickTime);
            
            webSocketServer.broadcast("server_stats", data);
        } catch (Exception e) {
            LOGGER.error("Error streaming server stats", e);
        }
    }
    
    /**
     * Stream player updates
     */
    private void streamPlayerUpdates() {
        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) return;
            
            JsonObject data = new JsonObject();
            data.addProperty("type", "player_update");
            
            JsonArray players = new JsonArray();
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                JsonObject playerData = new JsonObject();
                playerData.addProperty("uuid", player.getUUID().toString());
                playerData.addProperty("name", player.getName().getString());
                playerData.addProperty("health", player.getHealth());
                playerData.addProperty("maxHealth", player.getMaxHealth());
                playerData.addProperty("hunger", player.getFoodData().getFoodLevel());
                playerData.addProperty("level", player.experienceLevel);
                playerData.addProperty("ping", player.connection.latency());
                
                // Position
                JsonObject position = new JsonObject();
                position.addProperty("x", (int)player.getX());
                position.addProperty("y", (int)player.getY());
                position.addProperty("z", (int)player.getZ());
                position.addProperty("dimension", player.level().dimension().location().toString());
                playerData.add("position", position);
                
                players.add(playerData);
            }
            
            data.add("players", players);
            webSocketServer.broadcast("player_update", data);
        } catch (Exception e) {
            LOGGER.error("Error streaming player updates", e);
        }
    }
    
    /**
     * Stream performance metrics
     */
    private void streamPerformanceMetrics() {
        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) return;
            
            JsonObject data = new JsonObject();
            data.addProperty("type", "performance_metrics");
            
            // TPS
            data.addProperty("tps", Math.min(lastTps, 20.0));
            data.addProperty("tickTime", lastTickTime);
            
            // Memory
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
            long usedMemoryMB = heapUsage.getUsed() / (1024 * 1024);
            long maxMemoryMB = heapUsage.getMax() / (1024 * 1024);
            data.addProperty("memoryUsed", usedMemoryMB);
            data.addProperty("memoryMax", maxMemoryMB);
            data.addProperty("memoryPercent", (int)((double)usedMemoryMB / maxMemoryMB * 100));
            
            // Thread count
            ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
            data.addProperty("threadCount", threadBean.getThreadCount());
            data.addProperty("peakThreadCount", threadBean.getPeakThreadCount());
            
            // Chunks loaded (across all dimensions)
            int chunksLoaded = 0;
            for (var level : server.getAllLevels()) {
                chunksLoaded += level.getChunkSource().getLoadedChunksCount();
            }
            data.addProperty("chunksLoaded", chunksLoaded);
            
            webSocketServer.broadcast("performance_metrics", data);
        } catch (Exception e) {
            LOGGER.error("Error streaming performance metrics", e);
        }
    }
    
    /**
     * Stream memory updates
     */
    private void streamMemoryUpdates() {
        try {
            JsonObject data = new JsonObject();
            data.addProperty("type", "memory_update");
            
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
            MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
            
            // Heap memory
            JsonObject heap = new JsonObject();
            heap.addProperty("used", heapUsage.getUsed() / (1024 * 1024));
            heap.addProperty("max", heapUsage.getMax() / (1024 * 1024));
            heap.addProperty("committed", heapUsage.getCommitted() / (1024 * 1024));
            data.add("heap", heap);
            
            // Non-heap memory
            JsonObject nonHeap = new JsonObject();
            nonHeap.addProperty("used", nonHeapUsage.getUsed() / (1024 * 1024));
            nonHeap.addProperty("max", nonHeapUsage.getMax() / (1024 * 1024));
            nonHeap.addProperty("committed", nonHeapUsage.getCommitted() / (1024 * 1024));
            data.add("nonHeap", nonHeap);
            
            webSocketServer.broadcast("memory_update", data);
        } catch (Exception e) {
            LOGGER.error("Error streaming memory updates", e);
        }
    }
    
    /**
     * Stream entity counts
     */
    private void streamEntityCounts() {
        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) return;
            
            JsonObject data = new JsonObject();
            data.addProperty("type", "entity_count");
            
            // Count entities across all dimensions
            int totalEntities = 0;
            JsonObject perDimension = new JsonObject();
            for (var level : server.getAllLevels()) {
                int entityCount = 0;
                for (@SuppressWarnings("unused") var entity : level.getAllEntities()) {
                    entityCount++;
                }
                totalEntities += entityCount;
                String dimensionName = level.dimension().location().toString();
                perDimension.addProperty(dimensionName, entityCount);
            }
            
            data.addProperty("total", totalEntities);
            data.add("byDimension", perDimension);
            
            webSocketServer.broadcast("entity_count", data);
        } catch (Exception e) {
            LOGGER.error("Error streaming entity counts", e);
        }
    }
    
    /**
     * Update TPS (called from tick event)
     */
    public void updateTps(double tps, long tickTime) {
        this.lastTps = tps;
        this.lastTickTime = tickTime;
    }
    
    /**
     * Broadcast alert to all clients
     */
    public void broadcastAlert(String severity, String category, String title, String message) {
        JsonObject data = new JsonObject();
        data.addProperty("type", "alert");
        data.addProperty("severity", severity);
        data.addProperty("category", category);
        data.addProperty("title", title);
        data.addProperty("message", message);
        
        webSocketServer.broadcast("alerts", data);
    }
    
    /**
     * Broadcast chat message to all clients
     */
    public void broadcastChatMessage(String player, String message, long timestamp) {
        JsonObject data = new JsonObject();
        data.addProperty("type", "chat_message");
        data.addProperty("player", player);
        data.addProperty("message", message);
        data.addProperty("timestamp", timestamp);
        
        webSocketServer.broadcast("chat", data);
    }
    
    /**
     * Broadcast player join event
     */
    public void broadcastPlayerJoin(String playerName, String uuid) {
        JsonObject data = new JsonObject();
        data.addProperty("type", "player_join");
        data.addProperty("name", playerName);
        data.addProperty("uuid", uuid);
        
        webSocketServer.broadcastToAll(data);
    }
    
    /**
     * Broadcast player leave event
     */
    public void broadcastPlayerLeave(String playerName, String uuid) {
        JsonObject data = new JsonObject();
        data.addProperty("type", "player_leave");
        data.addProperty("name", playerName);
        data.addProperty("uuid", uuid);
        
        webSocketServer.broadcastToAll(data);
    }
}
