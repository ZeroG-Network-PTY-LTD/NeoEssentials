package com.zerog.neoessentials.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zerog.neoessentials.util.LocationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
    
    private static final Logger LOGGER = LoggerFactory.getLogger(StorageManager.class);
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
        
        initializeStorage();
    }
    
    public static StorageManager getInstance() {
        if (instance == null) {
            instance = new StorageManager();
        }
        return instance;
    }
    
    /**
     * Initialize storage directories
     */
    private void initializeStorage() {
        try {
            // Create main data directory
            Files.createDirectories(dataDirectory);
            
            // Create subdirectories
            Files.createDirectories(dataDirectory.resolve("players"));
            Files.createDirectories(dataDirectory.resolve("homes"));
            Files.createDirectories(dataDirectory.resolve("warps"));
            Files.createDirectories(dataDirectory.resolve("economy"));
            Files.createDirectories(dataDirectory.resolve("kits"));
            Files.createDirectories(dataDirectory.resolve("mail"));
            Files.createDirectories(dataDirectory.resolve("shops"));
            Files.createDirectories(dataDirectory.resolve("backups"));
            
            LOGGER.info("Storage system initialized at: {}", dataDirectory.toAbsolutePath());
            
        } catch (IOException e) {
            LOGGER.error("Failed to initialize storage directories", e);
        }
    }
    
    /**
     * Save data to file asynchronously
     */
    public CompletableFuture<Boolean> saveDataAsync(String category, String filename, Object data) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Path categoryPath = dataDirectory.resolve(category);
                Files.createDirectories(categoryPath);
                
                Path filePath = categoryPath.resolve(filename + ".json");
                String jsonData = gson.toJson(data);
                
                // Try atomic approach first
                try {
                    // Write to temp file first, then rename for atomic operation
                    Path tempFile = categoryPath.resolve(filename + ".tmp");
                    
                    // Ensure temp file is created successfully
                    Files.write(tempFile, jsonData.getBytes());
                    
                    // Verify temp file exists before moving
                    if (!Files.exists(tempFile)) {
                        throw new IOException("Temp file was not created: " + tempFile);
                    }
                    
                    // Ensure target directory still exists
                    Files.createDirectories(categoryPath);
                    
                    // Move with replace existing (without atomic move to avoid filesystem issues)
                    Files.move(tempFile, filePath, StandardCopyOption.REPLACE_EXISTING);
                    
                } catch (IOException atomicError) {
                    // If atomic approach fails, try direct write as fallback
                    LOGGER.warn("Atomic save failed for {}/{}, trying direct write: {}", category, filename, atomicError.getMessage());
                    Files.write(filePath, jsonData.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
                }
                
                // Update cache
                cache.put(category + "/" + filename, data);
                
                LOGGER.debug("Saved data to {}", filePath);
                return true;
                
            } catch (IOException e) {
                LOGGER.error("Failed to save data to {}/{}", category, filename, e);
                return false;
            }
        });
    }
    
    /**
     * Load data from file asynchronously
     */
    public <T> CompletableFuture<T> loadDataAsync(String category, String filename, Class<T> clazz) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Check cache first
                String cacheKey = category + "/" + filename;
                if (cache.containsKey(cacheKey)) {
                    @SuppressWarnings("unchecked")
                    T cachedData = (T) cache.get(cacheKey);
                    return cachedData;
                }
                
                Path filePath = dataDirectory.resolve(category).resolve(filename + ".json");
                
                if (!Files.exists(filePath)) {
                    return null;
                }
                
                String jsonData = Files.readString(filePath);
                T data = gson.fromJson(jsonData, clazz);
                
                // Cache the loaded data
                cache.put(cacheKey, data);
                
                LOGGER.debug("Loaded data from {}", filePath);
                return data;
                
            } catch (IOException e) {
                LOGGER.error("Failed to load data from {}/{}", category, filename, e);
                return null;
            }
        });
    }
    
    /**
     * Save player homes data
     */
    public CompletableFuture<Boolean> savePlayerHomes(UUID playerUuid, Map<String, LocationUtil.Location> homes) {
        Map<String, Map<String, Object>> homeData = new HashMap<>();
        
        for (Map.Entry<String, LocationUtil.Location> entry : homes.entrySet()) {
            LocationUtil.Location location = entry.getValue();
            Map<String, Object> locationData = Map.of(
                "world", location.world,
                "x", location.x,
                "y", location.y,
                "z", location.z,
                "yaw", location.yaw,
                "pitch", location.pitch,
                "timestamp", location.timestamp
            );
            homeData.put(entry.getKey(), locationData);
        }
        
        return saveDataAsync("homes", playerUuid.toString(), homeData);
    }
    
    /**
     * Load player homes data
     */
    @SuppressWarnings("unchecked")
    public CompletableFuture<Map<String, LocationUtil.Location>> loadPlayerHomes(UUID playerUuid) {
        return loadDataAsync("homes", playerUuid.toString(), Map.class)
            .thenApply(data -> {
                if (data == null) {
                    return new HashMap<>();
                }
                
                Map<String, LocationUtil.Location> homes = new HashMap<>();
                Map<String, Map<String, Object>> homeData = (Map<String, Map<String, Object>>) data;
                
                for (Map.Entry<String, Map<String, Object>> entry : homeData.entrySet()) {
                    Map<String, Object> locationData = entry.getValue();
                    LocationUtil.Location location = new LocationUtil.Location(
                        (String) locationData.get("world"),
                        ((Number) locationData.get("x")).doubleValue(),
                        ((Number) locationData.get("y")).doubleValue(),
                        ((Number) locationData.get("z")).doubleValue(),
                        ((Number) locationData.get("yaw")).floatValue(),
                        ((Number) locationData.get("pitch")).floatValue(),
                        ((Number) locationData.get("timestamp")).longValue()
                    );
                    homes.put(entry.getKey(), location);
                }
                
                return homes;
            });
    }
    
    /**
     * Save player economy data
     */
    public CompletableFuture<Boolean> savePlayerEconomy(UUID playerUuid, Map<String, Object> economyData) {
        return saveDataAsync("economy", playerUuid.toString(), economyData);
    }
    
    /**
     * Load player economy data
     */
    @SuppressWarnings("unchecked")
    public CompletableFuture<Map<String, Object>> loadPlayerEconomy(UUID playerUuid) {
        return loadDataAsync("economy", playerUuid.toString(), Map.class)
            .thenApply(data -> data != null ? (Map<String, Object>) data : new HashMap<>());
    }
    
    /**
     * Save warp data
     */
    public CompletableFuture<Boolean> saveWarps(Map<String, Object> warpData) {
        return saveDataAsync("warps", "server_warps", warpData);
    }
    
    /**
     * Load warp data
     */
    @SuppressWarnings("unchecked")
    public CompletableFuture<Map<String, Object>> loadWarps() {
        return loadDataAsync("warps", "server_warps", Map.class)
            .thenApply(data -> data != null ? (Map<String, Object>) data : new HashMap<>());
    }
    
    /**
     * Save player mail
     */
    public CompletableFuture<Boolean> savePlayerMail(UUID playerUuid, List<Map<String, Object>> mailData) {
        return saveDataAsync("mail", playerUuid.toString(), mailData);
    }
    
    /**
     * Load player mail
     */
    @SuppressWarnings("unchecked")
    public CompletableFuture<List<Map<String, Object>>> loadPlayerMail(UUID playerUuid) {
        return loadDataAsync("mail", playerUuid.toString(), List.class)
            .thenApply(data -> data != null ? (List<Map<String, Object>>) data : new ArrayList<>());
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
                
                LOGGER.debug("Deleted file: {}", filePath);
                return deleted;
                
            } catch (IOException e) {
                LOGGER.error("Failed to delete file {}/{}", category, filename, e);
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
                
                LOGGER.info("Created backup at: {}", backupPath);
                return true;
                
            } catch (IOException e) {
                LOGGER.error("Failed to create backup", e);
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
                    LOGGER.error("Failed to copy file: {}", sourcePath, e);
                }
            });
    }
    
    /**
     * Clear cache for performance
     */
    public void clearCache() {
        cache.clear();
        LOGGER.debug("Storage cache cleared");
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
        LOGGER.info("Storage manager shutting down, cache size: {}", cache.size());
        clearCache();
    }
}
