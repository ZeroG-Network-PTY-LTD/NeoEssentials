package com.zerog.neoessentials.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Central configuration manager for NeoEssentials
 * Handles loading, saving, and accessing all configuration files
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class ConfigManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigManager.class);
    private static ConfigManager instance;
    private final Gson gson;
    private final Path configPath;
    
    // Configuration instances
    private MainConfig mainConfig;
    private EconomyConfig economyConfig;
    private HomeConfig homeConfig;
    private KitConfig kitConfig;
    private WarpConfig warpConfig;
    private ModerationConfig moderationConfig;
    private MessagingConfig messagingConfig;
    private DiscordConfig discordConfig;
    private TablistConfig tablistConfig;
    private SpawnConfig spawnConfig;
    
    private ConfigManager() {
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();
        this.configPath = FMLPaths.CONFIGDIR.get().resolve("neoessentials");
        
        // Create config directory if it doesn't exist
        createConfigDirectory();
    }
    
    public static ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }
    
    /**
     * Initialize all configurations
     */
    public void initialize() {
        LOGGER.info("Initializing NeoEssentials configurations...");
        
        try {
            loadMainConfig();
            loadEconomyConfig();
            loadHomeConfig();
            loadKitConfig();
            loadWarpConfig();
            loadModerationConfig();
            loadMessagingConfig();
            loadDiscordConfig();
            loadTablistConfig();
            loadSpawnConfig();
            
            LOGGER.info("All configurations loaded successfully!");
        } catch (Exception e) {
            LOGGER.error("Failed to initialize configurations", e);
        }
    }
    
    /**
     * Save all configurations
     */
    public void saveAll() {
        LOGGER.info("Saving all NeoEssentials configurations...");
        
        try {
            saveMainConfig();
            saveEconomyConfig();
            saveHomeConfig();
            saveKitConfig();
            saveWarpConfig();
            saveModerationConfig();
            saveMessagingConfig();
            saveDiscordConfig();
            saveTablistConfig();
            saveSpawnConfig();
            
            LOGGER.info("All configurations saved successfully!");
        } catch (Exception e) {
            LOGGER.error("Failed to save configurations", e);
        }
    }
    
    /**
     * Reload all configurations
     */
    public void reloadAll() {
        LOGGER.info("Reloading all NeoEssentials configurations...");
        initialize();
    }
    
    // Configuration getters
    public MainConfig getMainConfig() {
        return mainConfig != null ? mainConfig : new MainConfig();
    }
    
    public EconomyConfig getEconomyConfig() {
        return economyConfig != null ? economyConfig : new EconomyConfig();
    }
    
    public HomeConfig getHomeConfig() {
        return homeConfig != null ? homeConfig : new HomeConfig();
    }
    
    public KitConfig getKitConfig() {
        return kitConfig != null ? kitConfig : new KitConfig();
    }
    
    public WarpConfig getWarpConfig() {
        return warpConfig != null ? warpConfig : new WarpConfig();
    }
    
    public ModerationConfig getModerationConfig() {
        return moderationConfig != null ? moderationConfig : new ModerationConfig();
    }
    
    public MessagingConfig getMessagingConfig() {
        return messagingConfig != null ? messagingConfig : new MessagingConfig();
    }
    
    public DiscordConfig getDiscordConfig() {
        return discordConfig != null ? discordConfig : new DiscordConfig();
    }
    
    public TablistConfig getTablistConfig() {
        return tablistConfig != null ? tablistConfig : new TablistConfig();
    }
    
    public SpawnConfig getSpawnConfig() {
        return spawnConfig != null ? spawnConfig : new SpawnConfig();
    }
    
    // Load methods
    private void loadMainConfig() {
        this.mainConfig = loadConfig("main.json", MainConfig.class);
    }
    
    private void loadEconomyConfig() {
        this.economyConfig = loadConfig("economy.json", EconomyConfig.class);
    }
    
    private void loadHomeConfig() {
        this.homeConfig = loadConfig("homes.json", HomeConfig.class);
    }
    
    private void loadKitConfig() {
        this.kitConfig = loadConfig("kits.json", KitConfig.class);
    }
    
    private void loadWarpConfig() {
        this.warpConfig = loadConfig("warps.json", WarpConfig.class);
    }
    
    private void loadModerationConfig() {
        this.moderationConfig = loadConfig("moderation.json", ModerationConfig.class);
    }
    
    private void loadMessagingConfig() {
        this.messagingConfig = loadConfig("messaging.json", MessagingConfig.class);
    }
    
    private void loadDiscordConfig() {
        this.discordConfig = loadConfig("discord.json", DiscordConfig.class);
    }
    
    private void loadTablistConfig() {
        this.tablistConfig = loadConfig("tablist.json", TablistConfig.class);
    }
    
    private void loadSpawnConfig() {
        this.spawnConfig = loadConfig("spawn.json", SpawnConfig.class);
    }
    
    // Save methods
    private void saveMainConfig() {
        saveConfig("main.json", mainConfig);
    }
    
    private void saveEconomyConfig() {
        saveConfig("economy.json", economyConfig);
    }
    
    private void saveHomeConfig() {
        saveConfig("homes.json", homeConfig);
    }
    
    private void saveKitConfig() {
        saveConfig("kits.json", kitConfig);
    }
    
    private void saveWarpConfig() {
        saveConfig("warps.json", warpConfig);
    }
    
    private void saveModerationConfig() {
        saveConfig("moderation.json", moderationConfig);
    }
    
    private void saveMessagingConfig() {
        saveConfig("messaging.json", messagingConfig);
    }
    
    private void saveDiscordConfig() {
        saveConfig("discord.json", discordConfig);
    }
    
    private void saveTablistConfig() {
        saveConfig("tablist.json", tablistConfig);
    }
    
    private void saveSpawnConfig() {
        saveConfig("spawn.json", spawnConfig);
    }
    
    // Generic configuration loading
    private <T> T loadConfig(String fileName, Class<T> configClass) {
        File configFile = configPath.resolve(fileName).toFile();
        
        if (!configFile.exists()) {
            LOGGER.info("Creating default configuration: {}", fileName);
            T defaultConfig = createDefaultConfig(configClass);
            saveConfig(fileName, defaultConfig);
            return defaultConfig;
        }
        
        try (FileReader reader = new FileReader(configFile)) {
            T config = gson.fromJson(reader, configClass);
            LOGGER.debug("Loaded configuration: {}", fileName);
            return config;
        } catch (IOException e) {
            LOGGER.error("Failed to load configuration: {}", fileName, e);
            return createDefaultConfig(configClass);
        }
    }
    
    // Generic configuration saving
    private <T> void saveConfig(String fileName, T config) {
        if (config == null) {
            LOGGER.warn("Attempted to save null configuration: {}", fileName);
            return;
        }
        
        File configFile = configPath.resolve(fileName).toFile();
        
        try (FileWriter writer = new FileWriter(configFile)) {
            gson.toJson(config, writer);
            LOGGER.debug("Saved configuration: {}", fileName);
        } catch (IOException e) {
            LOGGER.error("Failed to save configuration: {}", fileName, e);
        }
    }
    
    // Create default configuration instance
    private <T> T createDefaultConfig(Class<T> configClass) {
        try {
            return configClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            LOGGER.error("Failed to create default configuration for: {}", configClass.getSimpleName(), e);
            return null;
        }
    }
    
    // Create config directory
    private void createConfigDirectory() {
        File configDir = configPath.toFile();
        if (!configDir.exists()) {
            boolean created = configDir.mkdirs();
            if (created) {
                LOGGER.info("Created NeoEssentials config directory: {}", configPath);
            } else {
                LOGGER.error("Failed to create config directory: {}", configPath);
            }
        }
    }
    
    /**
     * Check if all critical configurations are valid
     */
    public boolean validateConfigurations() {
        boolean valid = true;
        
        // Validate main config
        if (mainConfig == null) {
            LOGGER.error("Main configuration is null!");
            valid = false;
        }
        
        // Validate economy config if economy is enabled
        if (mainConfig != null && mainConfig.modules.economy && economyConfig != null) {
            // Basic validation - just check if it exists
            LOGGER.debug("Economy configuration is present");
        }
        
        // Validate Discord config if Discord is enabled
        if (discordConfig != null && !discordConfig.isValid()) {
            LOGGER.error("Discord configuration is invalid!");
            valid = false;
        }
        
        return valid;
    }
    
    /**
     * Get configuration file path
     */
    public Path getConfigPath() {
        return configPath;
    }
    
    /**
     * Get configuration file for specific config
     */
    public File getConfigFile(String fileName) {
        return configPath.resolve(fileName).toFile();
    }
    
    /**
     * Check if configuration file exists
     */
    public boolean configExists(String fileName) {
        return configPath.resolve(fileName).toFile().exists();
    }
}
