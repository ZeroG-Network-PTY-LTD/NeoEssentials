package com.zerog.neoessentials.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.data.DataManager;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Manages all configuration for the NeoEssentials mod.
 * Responsible for loading, saving, and providing access to configuration settings.
 */
public class ConfigManager {
    private static final String CONFIG_DIR = "config/neoessentials";    private static final String MAIN_CONFIG_FILE = "config.json";
    
    private NeoEssentialsConfig config;
    private DatabaseConfig databaseConfig;
<<<<<<< HEAD
<<<<<<< HEAD
    private TablistConfig tablistConfig;
=======
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
    private TablistConfig tablistConfig;
>>>>>>> 7058369 (feat: Update migration tasks and enhance tablist documentation; refactor permission checks in AdminPanelCommand and CommandManager)
    private final Gson gson;
      public ConfigManager() {
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();
                
        this.config = new NeoEssentialsConfig();
        this.databaseConfig = new DatabaseConfig();
    }
      /**
     * Initialize the configuration system.
     * Creates necessary directories and loads or creates config files.
     */
    public void initialize() {
        try {
            // Ensure config directory exists
            Path configDir = Paths.get(CONFIG_DIR);
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
                NeoEssentials.LOGGER.info("Created config directory: {}", configDir);
            }
            
            // Load or create main config
            File configFile = new File(CONFIG_DIR, MAIN_CONFIG_FILE);
            if (configFile.exists()) {
                try (FileReader reader = new FileReader(configFile)) {
                    this.config = gson.fromJson(reader, NeoEssentialsConfig.class);
                    NeoEssentials.LOGGER.info("Loaded configuration file");
                }
            } else {
                // Default config doesn't exist, create it
                saveConfig();
                NeoEssentials.LOGGER.info("Created default configuration file");
            }
              // Initialize database config
            databaseConfig.initialize();
<<<<<<< HEAD
<<<<<<< HEAD
              // Database config is now handled by StorageManager in NeoEssentials.java
            // No need to set anything here as the DatabaseConfig will be passed directly
            // to the StorageManager when it's created
=======
            
            // Apply database config to DataManager if it's available
            if (NeoEssentials.getInstance() != null && 
                NeoEssentials.getInstance().getDataManager() != null) {
                
                DataManager dataManager = NeoEssentials.getInstance().getDataManager();
                
                dataManager.setDatabaseConfig(
                    databaseConfig.storageType.get(),
                    databaseConfig.mysqlHost.get(),
                    databaseConfig.mysqlPort.get(),
                    databaseConfig.mysqlDatabase.get(),
                    databaseConfig.mysqlUsername.get(),
                    databaseConfig.mysqlPassword.get()
                );
            }
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
              // Database config is now handled by StorageManager in NeoEssentials.java
            // No need to set anything here as the DatabaseConfig will be passed directly
            // to the StorageManager when it's created
>>>>>>> ff27982 (refactor: Remove database config handling from ConfigManager; delegate to StorageManager)
        } catch (IOException e) {
            NeoEssentials.LOGGER.error("Failed to initialize config system", e);
        }
    }
    
    /**
     * Save the current configuration to file.
     */
    public void saveConfig() {
        try {
            File configFile = new File(CONFIG_DIR, MAIN_CONFIG_FILE);
            try (FileWriter writer = new FileWriter(configFile)) {
                gson.toJson(config, writer);
                NeoEssentials.LOGGER.info("Saved configuration file");
            }
        } catch (IOException e) {
            NeoEssentials.LOGGER.error("Failed to save configuration", e);
        }
    }
      /**
     * Get the current configuration.
     * 
     * @return The current configuration object
     */
    public NeoEssentialsConfig getConfig() {
        return config;
    }
    
    /**
     * Get the database configuration.
     * 
     * @return The database configuration object
     */
    public DatabaseConfig getDatabaseConfig() {
        return databaseConfig;
    }
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> 7058369 (feat: Update migration tasks and enhance tablist documentation; refactor permission checks in AdminPanelCommand and CommandManager)
    
    /**
     * Get the tablist configuration.
     * 
     * @return The tablist configuration object
     */
    public TablistConfig getTablistConfig() {
        if (tablistConfig == null) {
            tablistConfig = new TablistConfig();
            tablistConfig.load();
        }
        return tablistConfig;
    }
<<<<<<< HEAD
=======
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
>>>>>>> 7058369 (feat: Update migration tasks and enhance tablist documentation; refactor permission checks in AdminPanelCommand and CommandManager)
}
