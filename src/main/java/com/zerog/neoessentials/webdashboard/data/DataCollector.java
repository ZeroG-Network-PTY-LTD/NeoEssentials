package com.zerog.neoessentials.webdashboard.data;

import com.google.gson.JsonObject;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Central data collection and management system for the Dashboard API
 * Coordinates all specialized data collectors and provides unified access
 * 
 * Design:
 * - Real-time data collection from Minecraft server
 * - Specialized collectors for different data domains
 * - Efficient caching to reduce server load
 * - Event-driven updates for live data
 */
public class DataCollector {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataCollector.class);
    private static DataCollector INSTANCE;
    
    private final Map<String, CachedData> dataCache;
    
    // Specialized collectors
    private PlayerDataCollector playerCollector;
    private ServerDataCollector serverCollector;
    private GameDataCollector gameCollector;
    private LoggingDataCollector loggingCollector;
    
    private DataCollector() {
        this.dataCache = new ConcurrentHashMap<>();
    }
    
    public static DataCollector getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new DataCollector();
        }
        return INSTANCE;
    }
    
    /**
     * Initialize data collector with server instance
     */
    public void initialize(MinecraftServer server) {
        // Initialize specialized collectors
        this.playerCollector = new PlayerDataCollector(server);
        this.serverCollector = new ServerDataCollector(server);
        this.gameCollector = new GameDataCollector(server);
        this.loggingCollector = new LoggingDataCollector();

        NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "Data Collector initialized with all specialized collectors");
    }
    
    /**
     * Stop data collection
     */
    public void shutdown() {
        this.dataCache.clear();
        NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "Data Collector stopped");
    }
    
    // ===== PLAYER DATA METHODS =====
    
    public JsonObject getPlayerProfile(UUID playerUuid) {
        return playerCollector.getPlayerProfile(playerUuid);
    }
    
    public JsonObject getPlayerStatistics(UUID playerUuid) {
        return playerCollector.getPlayerStatistics(playerUuid);
    }
    
    public JsonObject getPlayerAchievements(UUID playerUuid) {
        return playerCollector.getPlayerAchievements(playerUuid);
    }
    
    public JsonObject getPlayerInventory(UUID playerUuid) {
        return playerCollector.getPlayerInventory(playerUuid);
    }
    
    public JsonObject getPlayerStatus(UUID playerUuid) {
        return playerCollector.getPlayerStatus(playerUuid);
    }
    
    public JsonObject getPlayerHealth(UUID playerUuid) {
        return playerCollector.getPlayerHealth(playerUuid);
    }
    
    public JsonObject getPlayerXP(UUID playerUuid) {
        return playerCollector.getPlayerXP(playerUuid);
    }
    
    public JsonObject getPlayerLocation(UUID playerUuid) {
        return playerCollector.getPlayerLocation(playerUuid);
    }
    
    public JsonObject getPlayerHomes(String username) {
        return playerCollector.getPlayerHomes(username);
    }
    
    public JsonObject getOnlinePlayers() {
        return playerCollector.getOnlinePlayers();
    }
    
    // ===== SERVER DATA METHODS =====
    
    public JsonObject getServerProfile() {
        return serverCollector.getServerProfile();
    }
    
    public JsonObject getServerStatistics() {
        return serverCollector.getServerStatistics();
    }
    
    /**
     * Collect server status data (cached)
     */
    public JsonObject getServerStatus() {
        return getCachedOrCompute("server_status", 
            () -> serverCollector.getServerStatus(), 1000);
    }
    
    public JsonObject getServerHealth() {
        return serverCollector.getServerHealth();
    }
    
    public JsonObject getServerWorlds() {
        return serverCollector.getServerWorlds();
    }
    
    public JsonObject getServerConfig() {
        return serverCollector.getServerConfig();
    }
    
    public JsonObject getServerPerformance() {
        return serverCollector.getServerPerformance();
    }
    
    /**
     * Collect server info data (cached)
     */
    public JsonObject getServerInfo() {
        return getCachedOrCompute("server_info", 
            () -> serverCollector.getServerProfile(), 60000);
    }
    
    /**
     * Collect memory usage data (cached)
     */
    public JsonObject getMemoryInfo() {
        return getCachedOrCompute("memory_info", () -> {
            JsonObject stats = serverCollector.getServerStatistics();
            return stats.getAsJsonObject("memory");
        }, 2000);
    }
    
    // ===== GAME DATA METHODS =====
    
    public JsonObject getGameEvents(int limit) {
        return gameCollector.getGameEvents(limit);
    }
    
    public JsonObject getGameStatistics() {
        return gameCollector.getGameStatistics();
    }
    
    public JsonObject getGameActivity() {
        return gameCollector.getGameActivity();
    }
    
    public JsonObject getTopBlocks() {
        return gameCollector.getTopBlocks();
    }
    
    public void clearGameEvents() {
        gameCollector.clearEvents();
    }
    
    // ===== LOGGING & ANALYTICS METHODS =====
    
    public JsonObject getRequestLogs(int limit) {
        return loggingCollector.getRequestLogs(limit);
    }
    
    public JsonObject getErrorLogs(int limit, String severity) {
        return loggingCollector.getErrorLogs(limit, severity);
    }
    
    public JsonObject getPerformanceMetrics() {
        return loggingCollector.getPerformanceMetrics();
    }
    
    public JsonObject getUserActivity() {
        return loggingCollector.getUserActivity();
    }
    
    public JsonObject getServerLogs(int lines) {
        return loggingCollector.getServerLogs(lines);
    }
    
    public JsonObject getErrorStatistics() {
        return loggingCollector.getErrorStatistics();
    }
    
    public void clearLogs(String logType) {
        loggingCollector.clearLogs(logType);
    }
    
    // Static logging methods for API access
    public static void logRequest(String endpoint, String method, int statusCode, 
                                   long responseTimeMs, String username) {
        LoggingDataCollector.logRequest(endpoint, method, statusCode, responseTimeMs, username);
    }
    
    public static void logError(String message, String severity, Exception exception) {
        LoggingDataCollector.logError(message, severity, exception);
    }
    
    // ===== LEGACY/COMPATIBILITY METHODS =====
    
    /**
     * Collect world data (delegated to server collector)
     */
    public JsonObject getWorldData() {
        return serverCollector.getServerWorlds();
    }
    
    /**
     * Collect economy data
     * FUTURE: Integrate with EconomyAPI for player balances and transactions
     */
    public JsonObject getEconomyData() {
        // Placeholder for future economy integration
        return new JsonObject();
    }
    
    /**
     * Get or compute cached data
     */
    private JsonObject getCachedOrCompute(String key, DataSupplier supplier, long cacheDuration) {
        CachedData cached = dataCache.get(key);
        long now = System.currentTimeMillis();
        
        if (cached != null && (now - cached.timestamp) < cacheDuration) {
            return cached.data;
        }
        
        JsonObject data = supplier.get();
        dataCache.put(key, new CachedData(data, now));
        return data;
    }
    
    /**
     * Clear all cached data
     */
    public void clearCache() {
        dataCache.clear();
        NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "All cached data cleared");
    }
    
    /**
     * Clear specific cached data
     */
    public void clearCache(String key) {
        dataCache.remove(key);
        NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Cache cleared for key: {}", key);
    }
    
    /**
     * Cached data holder
     */
    private static class CachedData {
        final JsonObject data;
        final long timestamp;
        
        CachedData(JsonObject data, long timestamp) {
            this.data = data;
            this.timestamp = timestamp;
        }
    }
    
    /**
     * Functional interface for data suppliers
     */
    @FunctionalInterface
    private interface DataSupplier {
        JsonObject get();
    }
}
