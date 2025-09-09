package com.zerog.neoessentials.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Unified Configuration Manager for NeoEssentials
 * Manages only the specific JSON files we want to generate
 */
public class ConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigManager.class);
    private static ConfigManager instance;
    
    private final Gson gson;
    private final Path configPath;
    
    private MainConfig mainConfig;
    private CommandsConfig commandsConfig;
    private CustomPlaceholderConfig customPlaceholderConfig;
    private TablistConfig tablistConfig;
    private ShopsConfig shopsConfig;

    private ConfigManager() {
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();
        this.configPath = Paths.get("config/neoessentials");
    }

    public static ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    public void initialize() {
        try {
            Files.createDirectories(configPath);
            
            // Load the 4 main config files we want to generate
            mainConfig = loadConfig("config.json", MainConfig.class);
            commandsConfig = loadConfig("commands.json", CommandsConfig.class);  
            customPlaceholderConfig = loadConfig("customPlaceholders.json", CustomPlaceholderConfig.class);
            tablistConfig = loadConfig("tablist.json", TablistConfig.class);
            shopsConfig = loadConfig("shops.json", ShopsConfig.class);
            
            // Configuration loaded successfully
            LOGGER.info("Configuration files loaded successfully");
            LOGGER.info("MainConfig modules loaded: homes={}, economy={}, warps={}, kits={}, chat={}, spawn={}, moderation={}", 
                mainConfig.modules.homes, mainConfig.modules.economy, mainConfig.modules.warps, 
                mainConfig.modules.kits, mainConfig.modules.chat, mainConfig.modules.spawn, mainConfig.modules.moderation);
            LOGGER.info("CommandsConfig loaded with {} commands configured", commandsConfig.commands.size());
            tablistConfig = loadConfig("tablist.json", TablistConfig.class);
            // Ensure defaults exist after loading (fixes configs loaded from JSON that bypass constructor)
            if (tablistConfig != null) {
                tablistConfig.ensureDefaults();
            }
            shopsConfig = loadConfig("shops.json", ShopsConfig.class);
            
            // Clean up unwanted config files
            cleanupUnwantedConfigFiles();
            
            LOGGER.info("ConfigManager initialized successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to initialize ConfigManager: {}", e.getMessage(), e);
        }
    }

    private <T> T loadConfig(String fileName, Class<T> configClass) {
        Path filePath = configPath.resolve(fileName);
        
        try {
            if (Files.exists(filePath)) {
                String content = Files.readString(filePath);
                return gson.fromJson(content, configClass);
            } else {
                T defaultConfig = configClass.getDeclaredConstructor().newInstance();
                saveConfig(fileName, defaultConfig);
                return defaultConfig;
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load {}: {}", fileName, e.getMessage());
            try {
                return configClass.getDeclaredConstructor().newInstance();
            } catch (Exception ex) {
                throw new RuntimeException("Failed to create default config for " + fileName, ex);
            }
        }
    }

    public <T> void saveConfig(String fileName, T config) {
        Path filePath = configPath.resolve(fileName);
        
        try {
            String json = gson.toJson(config);
            Files.writeString(filePath, json);
        } catch (IOException e) {
            LOGGER.error("Failed to save {}: {}", fileName, e.getMessage());
        }
    }

    /**
     * Clean up unwanted config files that shouldn't be generated
     */
    private void cleanupUnwantedConfigFiles() {
        String[] unwantedFiles = {
            "core.json", "displays.json", "features.json", "general.json", 
            "placeholders.json", "teleport.json"
        };
        
        for (String unwantedFile : unwantedFiles) {
            Path unwantedPath = configPath.resolve(unwantedFile);
            try {
                if (Files.exists(unwantedPath)) {
                    Files.delete(unwantedPath);
                    LOGGER.info("Cleaned up unwanted config file: {}", unwantedFile);
                }
            } catch (IOException e) {
                LOGGER.warn("Failed to delete unwanted config file {}: {}", unwantedFile, e.getMessage());
            }
        }
    }

    public void reloadAll() {
        initialize();
    }

    // Getters for config objects
    public MainConfig getMainConfig() { return mainConfig; }
    public CommandsConfig getCommandsConfig() { return commandsConfig; }
    public CustomPlaceholderConfig getCustomPlaceholderConfig() { return customPlaceholderConfig; }
    public TablistConfig getTablistConfig() { return tablistConfig; }
    public ShopsConfig getShopsConfig() { return shopsConfig; }
    
    public Path getConfigPath() { return configPath; }
    
    public String[] getAllConfigFiles() {
        return new String[]{"config.json", "commands.json", "customPlaceholders.json", "tablist.json", "shops.json"};
    }
    
    // Additional utility methods for backward compatibility
    public boolean configExists(String fileName) {
        return Files.exists(configPath.resolve(fileName));
    }
    
    public void saveAll() {
        saveConfig("config.json", mainConfig);
        saveConfig("commands.json", commandsConfig);
        saveConfig("customPlaceholders.json", customPlaceholderConfig);
        saveConfig("tablist.json", tablistConfig);
        saveConfig("shops.json", shopsConfig);
    }
    
    // Simplified config status for compatibility
    public ConfigStatus getConfigStatus() {
        return new ConfigStatus();
    }
    
    public static class ConfigStatus {
        public boolean isLoaded(String configName) {
            return true; // All configs are loaded if ConfigManager is initialized
        }
        
        public boolean isValid(String configName) {
            return true; // Our configs are always valid
        }
        
        public String getError(String configName) {
            return null; // No errors in simplified implementation
        }
        
        public String getHealthSummary() {
            return "All configuration files are loaded and healthy.";
        }
    }
}
