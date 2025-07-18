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
    
<<<<<<< HEAD
    public enum StorageType {
        SQLITE("sqlite"),
        MYSQL("mysql"),
        JSON("json");
=======
    // Storage type
    public final ModConfigSpec.EnumValue<StorageType> storageType;
    
    // MySQL settings
    public final ModConfigSpec.ConfigValue<String> mysqlHost;
    public final ModConfigSpec.IntValue mysqlPort;
    public final ModConfigSpec.ConfigValue<String> mysqlDatabase;
    public final ModConfigSpec.ConfigValue<String> mysqlUsername;
    public final ModConfigSpec.ConfigValue<String> mysqlPassword;
<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
    public final ModConfigSpec.BooleanValue mysqlUseSSL;
    public final ModConfigSpec.ConfigValue<String> mysqlTablePrefix;
=======
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
    public final ModConfigSpec.BooleanValue mysqlUseSSL;
    public final ModConfigSpec.ConfigValue<String> mysqlTablePrefix;
>>>>>>> 73a32aa (Implement SQLite storage handler and associated factory and manager classes)
=======
    public final ModConfigSpec.BooleanValue mysqlUseSSL;
    public final ModConfigSpec.ConfigValue<String> mysqlTablePrefix;
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
    
    // SQLite settings
    public final ModConfigSpec.ConfigValue<String> sqliteFilename;
    
    public DatabaseConfig() {
        // Define the config
        builder.comment("NeoEssentials Database Configuration");
>>>>>>> c39b21b14130226fa8d7bc29cee63dcb88440b58
        
        private final String name;
        
        StorageType(String name) {
            this.name = name;
        }
        
<<<<<<< HEAD
        public String getName() {
            return name;
        }
    }
    
    // Storage type
    public StorageType storageType = StorageType.SQLITE;
=======
        mysqlHost = builder
                .comment("MySQL Server host")
                .define("host", "localhost");
        
        mysqlPort = builder
                .comment("MySQL Server port")
                .defineInRange("port", 3306, 1, 65535);
        
        mysqlDatabase = builder
                .comment("MySQL database name")
                .define("database", "neoessentials");
        
        mysqlUsername = builder
                .comment("MySQL username")
                .define("username", "root");
        
        mysqlPassword = builder
                .comment("MySQL password")
                .define("password", "");
<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> 73a32aa (Implement SQLite storage handler and associated factory and manager classes)
=======
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
                
        mysqlUseSSL = builder
                .comment("Whether to use SSL for MySQL connections")
                .define("use_ssl", false);
                
        mysqlTablePrefix = builder
                .comment("Prefix for MySQL tables")
                .define("table_prefix", "ne_");
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
>>>>>>> 73a32aa (Implement SQLite storage handler and associated factory and manager classes)
=======
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
        
        builder.pop();
        
        // SQLite settings category
        builder.push("sqlite");
        
        sqliteFilename = builder
                .comment("SQLite database filename (without extension)")
                .define("filename", "neoessentials");
        
        builder.pop();
        
        // Build the config
        spec = builder.build();
    }
      /**
     * Initialize the config directory
     */
    public void initialize() {
        // Create config directory if it doesn't exist
        Path configDir = FMLPaths.CONFIGDIR.get().resolve(NeoEssentials.MODID);
        try {
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
                NeoEssentials.LOGGER.info("Created config directory for NeoEssentials");
            }
        } catch (IOException e) {
            NeoEssentials.LOGGER.error("Failed to create config directory for NeoEssentials", e);
        }
    }
    
    /**
     * Get the ModConfigSpec
     * 
     * @return The ModConfigSpec
     */
    public ModConfigSpec getSpec() {
        return spec;
    }
<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> 73a32aa (Implement SQLite storage handler and associated factory and manager classes)
=======
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
>>>>>>> c39b21b14130226fa8d7bc29cee63dcb88440b58
    
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
<<<<<<< HEAD
=======
    
    /**
     * Get the MySQL table prefix
     * 
     * @return MySQL table prefix
     */
    public String getTablePrefix() {
        return mysqlTablePrefix.get();
    }
    
    /**
     * Get the SQLite filename
     * 
     * @return SQLite filename
     */
    public String getSqliteFilename() {
        return sqliteFilename.get();
    }
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
>>>>>>> 73a32aa (Implement SQLite storage handler and associated factory and manager classes)
=======
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
>>>>>>> c39b21b14130226fa8d7bc29cee63dcb88440b58
}
