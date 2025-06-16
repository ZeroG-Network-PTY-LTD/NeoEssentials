package com.zerog.neoessentials.config;

import com.zerog.neoessentials.NeoEssentials;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Handles configuration for the database settings
 */
public class DatabaseConfig {
    private final ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
    private final ModConfigSpec spec;
    
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
    public final ModConfigSpec.BooleanValue mysqlUseSSL;
    public final ModConfigSpec.ConfigValue<String> mysqlTablePrefix;
=======
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
    public final ModConfigSpec.BooleanValue mysqlUseSSL;
    public final ModConfigSpec.ConfigValue<String> mysqlTablePrefix;
>>>>>>> 73a32aa (Implement SQLite storage handler and associated factory and manager classes)
    
    // SQLite settings
    public final ModConfigSpec.ConfigValue<String> sqliteFilename;
    
    public DatabaseConfig() {
        // Define the config
        builder.comment("NeoEssentials Database Configuration");
        
        // Storage type
        storageType = builder
                .comment("Storage type: JSON, SQLITE, MYSQL")
                .defineEnum("storage_type", StorageType.JSON);
        
        // MySQL settings category
        builder.push("mysql");
        
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
=======
>>>>>>> 73a32aa (Implement SQLite storage handler and associated factory and manager classes)
                
        mysqlUseSSL = builder
                .comment("Whether to use SSL for MySQL connections")
                .define("use_ssl", false);
                
        mysqlTablePrefix = builder
                .comment("Prefix for MySQL tables")
                .define("table_prefix", "ne_");
<<<<<<< HEAD
=======
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
>>>>>>> 73a32aa (Implement SQLite storage handler and associated factory and manager classes)
        
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
=======
>>>>>>> 73a32aa (Implement SQLite storage handler and associated factory and manager classes)
    
    /**
     * Get the MySQL host
     * 
     * @return MySQL host
     */
    public String getHost() {
        return mysqlHost.get();
    }
    
    /**
     * Get the MySQL port
     * 
     * @return MySQL port
     */
    public int getPort() {
        return mysqlPort.get();
    }
    
    /**
     * Get the MySQL database name
     * 
     * @return MySQL database name
     */
    public String getDatabase() {
        return mysqlDatabase.get();
    }
    
    /**
     * Get the MySQL username
     * 
     * @return MySQL username
     */
    public String getUsername() {
        return mysqlUsername.get();
    }
    
    /**
     * Get the MySQL password
     * 
     * @return MySQL password
     */
    public String getPassword() {
        return mysqlPassword.get();
    }
    
    /**
     * Check if MySQL should use SSL
     * 
     * @return True if MySQL should use SSL
     */
    public boolean isUseSsl() {
        return mysqlUseSSL.get();
    }
    
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
=======
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
>>>>>>> 73a32aa (Implement SQLite storage handler and associated factory and manager classes)
}
