package com.zerog.neoessentials.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.lang.ref.SoftReference;

/**
 * Memory-optimized storage manager for NeoEssentials
 * Handles file-based storage with JSON format
 * Supports async operations and efficient caching
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class StorageManager {
    
    // Singleton instance with thread-safe lazy initialization
    private static volatile StorageManager instance;
    
    // Optimized Gson instance with better memory usage
    private final Gson gson;
    private final Path dataDirectory;
    
    // Memory-efficient cache with soft references and size limits
    private final Map<String, SoftReference<Object>> cache;
    private final ReentrantReadWriteLock cacheLock;
    private static final int MAX_CACHE_SIZE = 500;
    
    // Thread pool for async operations - shared resource
    private static final ForkJoinPool ASYNC_EXECUTOR = ForkJoinPool.commonPool();
    
    // Reusable objects to reduce allocation pressure
    private final ThreadLocal<StringBuilder> stringBuilder = 
        ThreadLocal.withInitial(() -> new StringBuilder(256));
    
    private StorageManager() {
        // Initialize Gson with memory-efficient settings
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .disableHtmlEscaping() // Reduces string processing overhead
            .create();
        
        this.dataDirectory = Paths.get("neoessentials");
        this.cache = new ConcurrentHashMap<>(64, 0.75f); // Optimized initial capacity
        this.cacheLock = new ReentrantReadWriteLock();
        
        // Ensure data directory exists
        createDirectoryIfNotExists(dataDirectory);
    }
    
    /**
     * Thread-safe singleton accessor with double-checked locking
     */
    public static StorageManager getInstance() {
        if (instance == null) {
            synchronized (StorageManager.class) {
                if (instance == null) {
                    instance = new StorageManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * Save player data with memory optimization
     */
    public CompletableFuture<Boolean> savePlayerData(UUID playerUuid, Map<String, Object> playerData) {
        return CompletableFuture.supplyAsync(() -> {
            String cacheKey = "player_" + playerUuid.toString();
            Path filePath = dataDirectory.resolve("players").resolve(playerUuid.toString() + ".json");
            
            try {
                createDirectoryIfNotExists(filePath.getParent());
                
                // Use streaming JSON writer for better memory efficiency
                try (JsonWriter writer = new JsonWriter(Files.newBufferedWriter(filePath, 
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
                    gson.toJson(playerData, Map.class, writer);
                }
                
                // Cache with soft reference for memory efficiency
                cachePut(cacheKey, playerData);
                return true;
                
            } catch (IOException e) {
                handleError("savePlayerData", e);
                return false;
            }
        }, ASYNC_EXECUTOR);
    }
    
    /**
     * Load player data with memory optimization
     */
    @SuppressWarnings("unchecked")
    public CompletableFuture<Map<String, Object>> loadPlayerData(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String cacheKey = "player_" + playerUuid.toString();
            
            // Check cache first
            Map<String, Object> cached = cacheGet(cacheKey, Map.class);
            if (cached != null) {
                return cached;
            }
            
            Path filePath = dataDirectory.resolve("players").resolve(playerUuid.toString() + ".json");
            
            try {
                if (Files.exists(filePath)) {
                    // Use streaming JSON reader for better memory efficiency
                    try (JsonReader reader = new JsonReader(Files.newBufferedReader(filePath))) {
                        Map<String, Object> data = gson.fromJson(reader, Map.class);
                        if (data != null) {
                            cachePut(cacheKey, data);
                            return data;
                        }
                    }
                }
            } catch (IOException e) {
                handleError("loadPlayerData", e);
            }
            
            // Return empty map instead of null to avoid NullPointerExceptions
            return new HashMap<>();
        }, ASYNC_EXECUTOR);
    }
    
    /**
     * Save generic data with memory optimization
     */
    public <T> CompletableFuture<Boolean> saveDataAsync(String category, String filename, T data) {
        return CompletableFuture.supplyAsync(() -> {
            String cacheKey = category + "/" + filename;
            Path filePath = dataDirectory.resolve(category).resolve(filename + ".json");
            Path tempPath = dataDirectory.resolve(category).resolve(filename + ".json.tmp");
            
            try {
                createDirectoryIfNotExists(filePath.getParent());
                
                // Write to temporary file first for atomic saves
                try (JsonWriter writer = new JsonWriter(Files.newBufferedWriter(tempPath, 
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
                    gson.toJson(data, data.getClass(), writer);
                }
                
                // Atomic move from temp to final location
                try {
                    Files.move(tempPath, filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING, 
                              java.nio.file.StandardCopyOption.ATOMIC_MOVE);
                } catch (UnsupportedOperationException e) {
                    // Fallback for filesystems that don't support atomic moves
                    Files.move(tempPath, filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
                
                cachePut(cacheKey, data);
                return true;
                
            } catch (IOException e) {
                handleError("saveDataAsync", e);
                // Clean up temp file if it exists
                try {
                    Files.deleteIfExists(tempPath);
                } catch (IOException cleanupError) {
                    // Ignore cleanup errors
                }
                return false;
            }
        }, ASYNC_EXECUTOR);
    }
    
    /**
     * Load generic data with memory optimization
     */
    public <T> CompletableFuture<T> loadDataAsync(String category, String filename, Class<T> clazz) {
        return CompletableFuture.supplyAsync(() -> {
            String cacheKey = category + "/" + filename;
            
            // Check cache first
            T cached = cacheGet(cacheKey, clazz);
            if (cached != null) {
                return cached;
            }
            
            Path filePath = dataDirectory.resolve(category).resolve(filename + ".json");
            
            try {
                if (Files.exists(filePath)) {
                    // Use streaming JSON reader for better memory efficiency
                    try (JsonReader reader = new JsonReader(Files.newBufferedReader(filePath))) {
                        T data = gson.fromJson(reader, clazz);
                        if (data != null) {
                            cachePut(cacheKey, data);
                            return data;
                        }
                    }
                }
            } catch (IOException e) {
                handleError("loadDataAsync", e);
            }
            
            return null;
        }, ASYNC_EXECUTOR);
    }
    
    /**
     * Delete file with cache cleanup
     */
    public CompletableFuture<Boolean> deleteFile(String category, String filename) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Path filePath = dataDirectory.resolve(category).resolve(filename + ".json");
                boolean deleted = Files.deleteIfExists(filePath);
                
                // Remove from cache
                String cacheKey = category + "/" + filename;
                cacheRemove(cacheKey);
                
                return deleted;
            } catch (IOException e) {
                handleError("deleteFile", e);
                return false;
            }
        }, ASYNC_EXECUTOR);
    }
    
    /**
     * Memory-efficient cache operations with soft references
     */
    private void cachePut(String key, Object value) {
        cacheLock.writeLock().lock();
        try {
            // Limit cache size to prevent memory issues
            if (cache.size() >= MAX_CACHE_SIZE) {
                // Remove expired soft references and oldest entries
                cleanupCache();
            }
            cache.put(key, new SoftReference<>(value));
        } finally {
            cacheLock.writeLock().unlock();
        }
    }
    
    @SuppressWarnings("unchecked")
    private <T> T cacheGet(String key, Class<T> clazz) {
        cacheLock.readLock().lock();
        try {
            SoftReference<Object> ref = cache.get(key);
            if (ref != null) {
                Object value = ref.get();
                if (value != null && clazz.isInstance(value)) {
                    return (T) value;
                } else if (value == null) {
                    // Soft reference was cleared, remove the entry
                    cache.remove(key);
                }
            }
            return null;
        } finally {
            cacheLock.readLock().unlock();
        }
    }
    
    private void cacheRemove(String key) {
        cacheLock.writeLock().lock();
        try {
            cache.remove(key);
        } finally {
            cacheLock.writeLock().unlock();
        }
    }
    
    /**
     * Cleanup expired cache entries and limit size
     */
    private void cleanupCache() {
        // Remove entries with cleared soft references
        cache.entrySet().removeIf(entry -> entry.getValue().get() == null);
        
        // If still too large, remove oldest entries (approximate LRU)
        if (cache.size() >= MAX_CACHE_SIZE) {
            int toRemove = cache.size() - MAX_CACHE_SIZE + 10; // Remove extra for buffer
            Iterator<String> iterator = cache.keySet().iterator();
            for (int i = 0; i < toRemove && iterator.hasNext(); i++) {
                iterator.next();
                iterator.remove();
            }
        }
    }
    
    /**
     * Get cache statistics for monitoring
     */
    public Map<String, Object> getCacheStats() {
        cacheLock.readLock().lock();
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("cacheSize", cache.size());
            stats.put("maxCacheSize", MAX_CACHE_SIZE);
            
            // Count active references
            long activeRefs = cache.values().stream()
                .mapToLong(ref -> ref.get() != null ? 1 : 0)
                .sum();
            stats.put("activeReferences", activeRefs);
            
            return stats;
        } finally {
            cacheLock.readLock().unlock();
        }
    }
    
    /**
     * Efficient directory creation
     */
    private void createDirectoryIfNotExists(Path directory) {
        if (!Files.exists(directory)) {
            try {
                Files.createDirectories(directory);
            } catch (IOException e) {
                handleError("createDirectoryIfNotExists", e);
            }
        }
    }
    
    /**
     * Centralized error handling
     */
    private void handleError(String operation, Exception e) {
        // Use StringBuilder for efficient error message building
        StringBuilder sb = stringBuilder.get();
        sb.setLength(0);
        sb.append("StorageManager error in ").append(operation).append(": ").append(e.getMessage());
        
        // Handle error using existing error handler if available
        try {
            com.zerog.neoessentials.util.ErrorHandler.handleError(
                com.zerog.neoessentials.util.ErrorHandler.ErrorCategory.DATA_STORAGE,
                com.zerog.neoessentials.util.ErrorHandler.ErrorSeverity.HIGH,
                operation, e);
        } catch (Exception ex) {
            // Fallback to system error if error handler is not available
            System.err.println(sb.toString());
            e.printStackTrace();
        }
    }
    
    /**
     * Legacy compatibility methods
     */
    public CompletableFuture<Boolean> savePlayerEconomy(UUID playerUuid, Map<String, Object> economyData) {
        return savePlayerData(playerUuid, economyData);
    }
    
    public CompletableFuture<Map<String, Object>> loadPlayerEconomy(UUID playerUuid) {
        return loadPlayerData(playerUuid);
    }
    
    /**
     * Cleanup resources on shutdown
     */
    public void shutdown() {
        // Clear cache to help GC
        cacheLock.writeLock().lock();
        try {
            cache.clear();
        } finally {
            cacheLock.writeLock().unlock();
        }
    }
}
