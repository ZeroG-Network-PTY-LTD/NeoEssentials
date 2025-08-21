package com.zerog.neoessentials.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
                LOGGER.error("Failed to save data to JSON: {}/{}", category, filename, e);
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
                LOGGER.error("Failed to load data from JSON: {}/{}", category, filename, e);
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
     * If SQL is enabled, save to database. Otherwise, save to JSON file.
     */
    public CompletableFuture<Boolean> savePlayerData(UUID playerUuid, Map<String, Object> playerData) {
        if (sqlEnabled && sqlConnection != null) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    // Example: Save economy balance to SQL (expand as needed)
                    Object balanceObj = playerData.get("balance");
                    if (balanceObj != null) {
                        double balance = Double.parseDouble(balanceObj.toString());
                        String sql = "INSERT INTO player_economy (uuid, balance) VALUES (?, ?) ON DUPLICATE KEY UPDATE balance=?";
                        try (java.sql.PreparedStatement stmt = sqlConnection.prepareStatement(sql)) {
                            stmt.setString(1, playerUuid.toString());
                            stmt.setDouble(2, balance);
                            stmt.setDouble(3, balance);
                            stmt.executeUpdate();
                        }
                    }
                    // Add more fields/tables as needed
                    return true;
                } catch (Exception e) {
                    LOGGER.error("Failed to save player data to SQL", e);
                    return false;
                }
            });
        } else {
            // Fallback: Save to JSON file in players/{uuid}.json
            return CompletableFuture.supplyAsync(() -> {
                try {
                    Path playerFile = dataDirectory.resolve("players").resolve(playerUuid + ".json");
                    try (Writer writer = Files.newBufferedWriter(playerFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                        gson.toJson(playerData, writer);
                    }
                    cache.put("players/" + playerUuid, playerData);
                    return true;
                } catch (IOException e) {
                    LOGGER.error("Failed to save player data to JSON", e);
                    return false;
                }
            });
        }
    }

    /**
     * Load player data (economy, homes, etc.)
     * If SQL is enabled, load from database. Otherwise, load from JSON file.
     */
    public CompletableFuture<Map<String, Object>> loadPlayerData(UUID playerUuid) {
        if (sqlEnabled && sqlConnection != null) {
            return CompletableFuture.supplyAsync(() -> {
                Map<String, Object> playerData = new HashMap<>();
                try {
                    // Example: Load economy balance from SQL (expand as needed)
                    String sql = "SELECT balance FROM player_economy WHERE uuid=?";
                    try (java.sql.PreparedStatement stmt = sqlConnection.prepareStatement(sql)) {
                        stmt.setString(1, playerUuid.toString());
                        try (java.sql.ResultSet rs = stmt.executeQuery()) {
                            if (rs.next()) {
                                playerData.put("balance", rs.getDouble("balance"));
                            }
                        }
                    }
                    // Add more fields/tables as needed
                } catch (Exception e) {
                    LOGGER.error("Failed to load player data from SQL", e);
                }
                return playerData;
            });
        } else {
            // Fallback: Load from JSON file in players/{uuid}.json
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
                    LOGGER.error("Failed to load player data from JSON", e);
                }
                return new HashMap<>();
            });
        }
    }
    // SQL database connection fields
    private java.sql.Connection sqlConnection;
    private String sqlType;
    private boolean sqlEnabled;

    private void initializeSqlStorage() {
        // Load config from MainConfig.Database
        com.zerog.neoessentials.config.MainConfig.Database dbConfig = com.zerog.neoessentials.config.ConfigManager.getInstance().getMainConfig().database;
        sqlType = dbConfig.type;
        sqlEnabled = !"flatfile".equalsIgnoreCase(sqlType);
        if (!sqlEnabled) {
            LOGGER.info("SQL storage disabled, using flatfile JSON storage.");
            return;
        }
        try {
            if ("mysql".equalsIgnoreCase(sqlType)) {
                Class.forName("com.mysql.cj.jdbc.Driver");
                String url = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC", dbConfig.host, dbConfig.port, dbConfig.database);
                sqlConnection = java.sql.DriverManager.getConnection(url, dbConfig.username, dbConfig.password);
                LOGGER.info("Connected to MySQL database: {}", url);
            } else if ("sqlite".equalsIgnoreCase(sqlType)) {
                Class.forName("org.sqlite.JDBC");
                String url = "jdbc:sqlite:" + dbConfig.database + ".db";
                sqlConnection = java.sql.DriverManager.getConnection(url);
                LOGGER.info("Connected to SQLite database: {}", url);
            } else {
                LOGGER.warn("Unknown SQL type: {}. Defaulting to flatfile storage.", sqlType);
                sqlEnabled = false;
            }
        } catch (Exception e) {
            LOGGER.error("Failed to initialize SQL storage", e);
            sqlEnabled = false;
        }
    }
    
    private static final Logger LOGGER = LoggerFactory.getLogger(StorageManager.class);
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
    initializeSqlStorage();
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
