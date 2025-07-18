package com.zerog.neoessentials.config;

/**
 * Database configuration for NeoEssentials
 * 
 * Configures the storage backend for player data, economy, homes, etc.
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class DatabaseConfig {
    
    public enum StorageType {
        SQLITE("sqlite"),
        MYSQL("mysql"),
        JSON("json");
        
        private final String name;
        
        StorageType(String name) {
            this.name = name;
        }
        
        public String getName() {
            return name;
        }
    }
    
    // Storage type
    public StorageType storageType = StorageType.SQLITE;
    
    // SQLite settings
    public String sqliteFile = "neoessentials.db";
    
    // MySQL settings
    public String mysqlHost = "localhost";
    public int mysqlPort = 3306;
    public String mysqlDatabase = "neoessentials";
    public String mysqlUsername = "root";
    public String mysqlPassword = "password";
    public String mysqlTablePrefix = "ne_";
    
    // Connection pool settings
    public int connectionPoolSize = 5;
    public int connectionTimeoutSeconds = 30;
    public int idleTimeoutMinutes = 10;
    
    // Backup settings
    public boolean enableBackups = true;
    public int backupIntervalHours = 24;
    public int maxBackups = 7;
    
    // Cache settings
    public boolean enableCaching = true;
    public int cacheExpirationMinutes = 15;
    public int maxCacheSize = 1000;
    
    public static DatabaseConfig createDefault() {
        return new DatabaseConfig();
    }
}
