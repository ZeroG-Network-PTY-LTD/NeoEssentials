package com.zerog.neoessentials.player;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Core player data management system for NeoEssentials
 * Handles persistent storage and retrieval of player-specific data
 * 
 * @author ZeroG
 * @since 2.1.0
 */
public class PlayerDataManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerDataManager.class);
    private static PlayerDataManager instance;
    
    private final Map<UUID, PlayerData> playerDataCache;
    private final File dataDirectory;
    private final Gson gson;
    
    private PlayerDataManager() {
        this.playerDataCache = new ConcurrentHashMap<>();
        this.dataDirectory = new File("neoessentials", "playerdata");
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();
        
        // Ensure data directory exists
        if (!dataDirectory.exists()) {
            boolean created = dataDirectory.mkdirs();
            if (created) {
                LOGGER.info("Created player data directory: {}", dataDirectory.getAbsolutePath());
            } else {
                LOGGER.error("Failed to create player data directory: {}", dataDirectory.getAbsolutePath());
            }
        }
    }
    
    public static PlayerDataManager getInstance() {
        if (instance == null) {
            instance = new PlayerDataManager();
        }
        return instance;
    }
    
    /**
     * Load player data from storage
     */
    public PlayerData loadPlayerData(UUID playerUUID) {
        // Check cache first
        PlayerData cached = playerDataCache.get(playerUUID);
        if (cached != null) {
            return cached;
        }
        
        // Load from file
        File playerFile = new File(dataDirectory, playerUUID.toString() + ".json");
        
        if (!playerFile.exists()) {
            // Create new player data
            PlayerData newData = new PlayerData(playerUUID);
            playerDataCache.put(playerUUID, newData);
            savePlayerData(newData);
            LOGGER.debug("Created new player data for UUID: {}", playerUUID);
            return newData;
        }
        
        try (FileReader reader = new FileReader(playerFile)) {
            PlayerData data = gson.fromJson(reader, PlayerData.class);
            if (data != null) {
                playerDataCache.put(playerUUID, data);
                LOGGER.debug("Loaded player data for UUID: {}", playerUUID);
                return data;
            }
        } catch (IOException | JsonSyntaxException e) {
            com.zerog.neoessentials.util.ErrorHandler.handleError(
                com.zerog.neoessentials.util.ErrorHandler.ErrorCategory.PLAYER_MANAGEMENT,
                com.zerog.neoessentials.util.ErrorHandler.ErrorSeverity.HIGH,
                "Player Data Loading", e);
        }
        
        // Fallback to new data if loading failed
        PlayerData fallbackData = new PlayerData(playerUUID);
        playerDataCache.put(playerUUID, fallbackData);
        return fallbackData;
    }
    
    /**
     * Save player data to storage
     */
    public void savePlayerData(PlayerData data) {
        if (data == null) {
            LOGGER.warn("Attempted to save null player data");
            return;
        }
        
        File playerFile = new File(dataDirectory, data.getPlayerUUID().toString() + ".json");
        
        try (FileWriter writer = new FileWriter(playerFile)) {
            gson.toJson(data, writer);
            playerDataCache.put(data.getPlayerUUID(), data);
            LOGGER.debug("Saved player data for UUID: {}", data.getPlayerUUID());
        } catch (IOException e) {
            com.zerog.neoessentials.util.ErrorHandler.handleError(
                com.zerog.neoessentials.util.ErrorHandler.ErrorCategory.PLAYER_MANAGEMENT,
                com.zerog.neoessentials.util.ErrorHandler.ErrorSeverity.HIGH,
                "Player Data Saving", e);
        }
    }
    
    /**
     * Get player data (loads if not cached)
     */
    public PlayerData getPlayerData(UUID playerUUID) {
        return loadPlayerData(playerUUID);
    }
    
    /**
     * Get player data by ServerPlayer
     */
    public PlayerData getPlayerData(ServerPlayer player) {
        return getPlayerData(player.getUUID());
    }
    
    /**
     * Update and save player data with statistics validation
     */
    public void updatePlayerData(PlayerData data) {
        if (data != null) {
            data.setLastUpdated(System.currentTimeMillis());
            validateStatistics(data);
            savePlayerData(data);
        }
    }
    
    /**
     * Validate and fix statistics data integrity
     */
    private void validateStatistics(PlayerData data) {
        Map<String, Object> stats = data.getStatistics();
        
        // Ensure core statistics exist and are valid
        ensureStatistic(stats, "player_kills", 0);
        ensureStatistic(stats, "player_deaths", 0);
        ensureStatistic(stats, "mob_kills", 0);
        ensureStatistic(stats, "pvp_kills", 0);
        ensureStatistic(stats, "blocks_broken", 0);
        ensureStatistic(stats, "blocks_placed", 0);
        ensureStatistic(stats, "distance_traveled", 0.0);
        ensureStatistic(stats, "jumps", 0);
        ensureStatistic(stats, "items_crafted", 0);
        ensureStatistic(stats, "damage_dealt", 0.0);
        ensureStatistic(stats, "damage_taken", 0.0);
        
        // Validate that statistics are non-negative
        validateNonNegative(stats, "player_kills");
        validateNonNegative(stats, "player_deaths");
        validateNonNegative(stats, "mob_kills");
        validateNonNegative(stats, "pvp_kills");
        validateNonNegative(stats, "blocks_broken");
        validateNonNegative(stats, "blocks_placed");
        validateNonNegative(stats, "distance_traveled");
        validateNonNegative(stats, "jumps");
        validateNonNegative(stats, "items_crafted");
        validateNonNegative(stats, "damage_dealt");
        validateNonNegative(stats, "damage_taken");
    }
    
    /**
     * Ensure a statistic exists with a default value
     */
    private void ensureStatistic(Map<String, Object> stats, String key, Object defaultValue) {
        if (!stats.containsKey(key) || stats.get(key) == null) {
            stats.put(key, defaultValue);
        }
    }
    
    /**
     * Validate that a statistic is non-negative
     */
    private void validateNonNegative(Map<String, Object> stats, String key) {
        Object value = stats.get(key);
        if (value instanceof Number) {
            Number num = (Number) value;
            if (num.doubleValue() < 0) {
                if (value instanceof Integer) {
                    stats.put(key, 0);
                } else if (value instanceof Long) {
                    stats.put(key, 0L);
                } else if (value instanceof Double) {
                    stats.put(key, 0.0);
                } else if (value instanceof Float) {
                    stats.put(key, 0.0f);
                }
                LOGGER.warn("Reset negative statistic {} for player {}", key, 
                           stats.containsKey("playerUUID") ? stats.get("playerUUID") : "unknown");
            }
        }
    }
    
    /**
     * Remove player data from cache (will be reloaded when needed)
     */
    public void unloadPlayerData(UUID playerUUID) {
        PlayerData data = playerDataCache.remove(playerUUID);
        if (data != null) {
            savePlayerData(data); // Save before unloading
            LOGGER.debug("Unloaded player data for UUID: {}", playerUUID);
        }
    }
    
    /**
     * Save all cached player data
     */
    public void saveAllPlayerData() {
        com.zerog.neoessentials.util.DebugUtil.debugLog("Saving all cached player data (" + playerDataCache.size() + " players)...");
        
        int saved = 0;
        for (PlayerData data : playerDataCache.values()) {
            try {
                savePlayerData(data);
                saved++;
            } catch (Exception e) {
                LOGGER.error("Failed to save player data for UUID: {}", data.getPlayerUUID(), e);
            }
        }
        
        com.zerog.neoessentials.util.DebugUtil.debugLog("Saved " + saved + " player data files");
    }
    
    /**
     * Get the number of cached player data entries
     */
    public int getCachedPlayerCount() {
        return playerDataCache.size();
    }
    
    /**
     * Clear the player data cache
     */
    public void clearCache() {
        saveAllPlayerData(); // Save before clearing
        playerDataCache.clear();
        com.zerog.neoessentials.util.DebugUtil.debugLog("Cleared player data cache");
    }
    
    /**
     * Get data directory
     */
    public File getDataDirectory() {
        return dataDirectory;
    }
    
    /**
     * Check if player data exists in cache
     */
    public boolean isPlayerDataCached(UUID playerUUID) {
        return playerDataCache.containsKey(playerUUID);
    }
    
    /**
     * Get all cached player UUIDs
     */
    public java.util.Set<UUID> getCachedPlayerUUIDs() {
        return playerDataCache.keySet();
    }
    
    /**
     * Shutdown the manager (save all data)
     */
    public void shutdown() {
        LOGGER.info("Shutting down PlayerDataManager...");
        saveAllPlayerData();
        playerDataCache.clear();
        LOGGER.info("PlayerDataManager shutdown complete");
    }
    
    /**
     * Get top players by playtime
     */
    public List<PlayerData> getTopPlayersByPlaytime(int limit) {
        return playerDataCache.values().stream()
            .sorted((a, b) -> Long.compare(b.getTotalPlaytime(), a.getTotalPlaytime()))
            .limit(limit)
            .collect(Collectors.toList());
    }
}
