package com.zerog.neoessentials.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Comprehensive storage manager for NeoEssentials
 * Handles file-based storage with JSON format
 * Supports async operations and caching
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class StorageManager {
    /**
     * Legacy-compatible: Save player economy data
     */
    public CompletableFuture<Boolean> savePlayerEconomy(UUID playerUuid, Map<String, Object> economyData) {
        // Delegate to unified savePlayerData
        return savePlayerData(playerUuid, economyData);
    }

    /**
     * Legacy-compatible: Load player economy data
     */
    public CompletableFuture<Map<String, Object>> loadPlayerEconomy(UUID playerUuid) {
        // Delegate to unified loadPlayerData
        return loadPlayerData(playerUuid);
    }

    /**
     * Legacy-compatible: Save generic data async
     */
    public <T> CompletableFuture<Boolean> saveDataAsync(String category, String filename, T data) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Path filePath = dataDirectory.resolve(category).resolve(filename + ".json");
                Files.createDirectories(filePath.getParent());
                try (Writer writer = Files.newBufferedWriter(filePath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                    gson.toJson(data, writer);
                }
                cache.put(category + "/" + filename, data);
                return true;
            } catch (IOException e) {
                com.zerog.neoessentials.util.DebugUtil.errorLog("Failed to save data to JSON: " + category + "/" + filename + ", error: " + e.getMessage());
                return false;
            }
        });
    }

    /**
     * Legacy-compatible: Load generic data async
     */
    public <T> CompletableFuture<T> loadDataAsync(String category, String filename, Class<T> clazz) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Path filePath = dataDirectory.resolve(category).resolve(filename + ".json");
                if (Files.exists(filePath)) {
                    try (Reader reader = Files.newBufferedReader(filePath)) {
                        T data = gson.fromJson(reader, clazz);
                        cache.put(category + "/" + filename, data);
                        return data;
                    }
                }
            } catch (IOException e) {
                com.zerog.neoessentials.util.DebugUtil.errorLog("Failed to load data from JSON: " + category + "/" + filename + ", error: " + e.getMessage());
            }
            return null;
        });
    }
    /**
     * Singleton accessor for StorageManager
     */
    public static StorageManager getInstance() {
        if (instance == null) {
            instance = new StorageManager();
        }
        return instance;
    }
    /**
     * Save player data (economy, homes, etc.)
    * Save player data to JSON file only.
     */
    public CompletableFuture<Boolean> savePlayerData(UUID playerUuid, Map<String, Object> playerData) {
        // Only JSON file storage is supported
        return CompletableFuture.supplyAsync(() -> {
            try {
                Path playerFile = dataDirectory.resolve("players").resolve(playerUuid + ".json");
                try (Writer writer = Files.newBufferedWriter(playerFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                    gson.toJson(playerData, writer);
                }
                cache.put("players/" + playerUuid, playerData);
                return true;
            } catch (IOException e) {
                com.zerog.neoessentials.util.DebugUtil.errorLog("Failed to save player data to JSON: " + e.getMessage());
                return false;
            }
        });
    }

    /**
     * Load player data (economy, homes, etc.)
    * Load player data from JSON file only.
     */
    public CompletableFuture<Map<String, Object>> loadPlayerData(UUID playerUuid) {
        // Only JSON file storage is supported
        return CompletableFuture.supplyAsync(() -> {
            try {
                Path playerFile = dataDirectory.resolve("players").resolve(playerUuid + ".json");
                if (Files.exists(playerFile)) {
                    try (Reader reader = Files.newBufferedReader(playerFile)) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> playerData = (Map<String, Object>) gson.fromJson(reader, Map.class);
                        cache.put("players/" + playerUuid, playerData);
                        return playerData;
                    }
                }
            } catch (IOException e) {
                com.zerog.neoessentials.util.DebugUtil.errorLog("Failed to load player data from JSON: " + e.getMessage());
            }
            return new HashMap<>();
        });
    }
    // Only JSON file storage is supported.
    
    // LOGGER removed; now using DebugUtil for all logging
    // Singleton instance (not used, but kept for future expansion)
    private static StorageManager instance;
    
    private final Gson gson;
    private final Path dataDirectory;
    private final Map<String, Object> cache;
    
    private StorageManager() {
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();
        
        this.dataDirectory = Paths.get("neoessentials");
        this.cache = new ConcurrentHashMap<>();
        // Storage initialization logic can be added here if needed
        // End of constructor
    
    /**
     * Save player economy data
     */
    
    /**
     * Load player economy data
     */
    
    /**
     * Save warp data
     */
    
    /**
     * Load warp data
     */
    
    /**
     * Save player mail
     */
    
    /**
     * Load player mail
     */
        // All per-player data is now stored in players/{uuid}.json. Legacy per-player folder methods and fragments removed. Only initialization, global/server-wide data, and backup logic remain.
    }
    
    /**
     * Delete a file
     */
    public CompletableFuture<Boolean> deleteFile(String category, String filename) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Path filePath = dataDirectory.resolve(category).resolve(filename + ".json");
                boolean deleted = Files.deleteIfExists(filePath);
                
                // Remove from cache
                cache.remove(category + "/" + filename);
                
                com.zerog.neoessentials.util.DebugUtil.debugLog("Deleted file: " + filePath);
                return deleted;
                
            } catch (IOException e) {
                com.zerog.neoessentials.util.DebugUtil.errorLog("Failed to delete file " + category + "/" + filename + ", error: " + e.getMessage());
                return false;
            }
        });
    }
    
    /**
     * Create backup of all data
     */
    public CompletableFuture<Boolean> createBackup() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String timestamp = String.valueOf(System.currentTimeMillis());
                Path backupPath = dataDirectory.resolve("backups").resolve("backup_" + timestamp);
                Files.createDirectories(backupPath);
                
                // Copy all data directories
                copyDirectory(dataDirectory.resolve("players"), backupPath.resolve("players"));
                copyDirectory(dataDirectory.resolve("homes"), backupPath.resolve("homes"));
                copyDirectory(dataDirectory.resolve("warps"), backupPath.resolve("warps"));
                copyDirectory(dataDirectory.resolve("economy"), backupPath.resolve("economy"));
                copyDirectory(dataDirectory.resolve("kits"), backupPath.resolve("kits"));
                copyDirectory(dataDirectory.resolve("mail"), backupPath.resolve("mail"));
                
                com.zerog.neoessentials.util.DebugUtil.infoLog("Created backup at: " + backupPath);
                return true;
                
            } catch (IOException e) {
                com.zerog.neoessentials.util.DebugUtil.errorLog("Failed to create backup: " + e.getMessage());
                return false;
            }
        });
    }
    
    /**
     * Helper method to copy directories recursively
     */
    private void copyDirectory(Path source, Path target) throws IOException {
        if (!Files.exists(source)) {
            return;
        }
        
        Files.createDirectories(target);
        
        Files.walk(source)
            .forEach(sourcePath -> {
                try {
                    Path targetPath = target.resolve(source.relativize(sourcePath));
                    if (Files.isDirectory(sourcePath)) {
                        Files.createDirectories(targetPath);
                    } else {
                        Files.copy(sourcePath, targetPath);
                    }
                } catch (IOException e) {
                    com.zerog.neoessentials.util.DebugUtil.errorLog("Failed to copy file: " + sourcePath + ", error: " + e.getMessage());
                }
            });
    }
    
    /**
     * Clear cache for performance
     */
    public void clearCache() {
    cache.clear();
    com.zerog.neoessentials.util.DebugUtil.debugLog("Storage cache cleared");
    }
    
    /**
     * Get cache size
     */
    public int getCacheSize() {
        return cache.size();
    }
    
    /**
     * Shutdown storage manager
     */
    public void shutdown() {
    // Save any pending cache data
    com.zerog.neoessentials.util.DebugUtil.infoLog("Storage manager shutting down, cache size: " + cache.size());
    clearCache();
    }
}
