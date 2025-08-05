package com.zerog.neoessentials.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zerog.neoessentials.localization.LanguageManager;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enhanced Configuration Manager for NeoEssentials
 * Phase 4: Complete Configuration System Overhaul
 * 
 * Features:
 * - User-friendly TOML configuration files
 * - Hot-reload capability
 * - Configuration validation
 * - Default value management
 * - Per-module configuration sections
 * - Easy customization interface
 * 
 * @author ZeroG
 * @since 2.0.0 (Phase 4 Enhanced)
 */
public class ConfigManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigManager.class);
    private static ConfigManager instance;
    private final Gson gson;
    private final Path configPath;
    private final Path userConfigPath;
    private final ConfigStatus configStatus = new ConfigStatus();
    private final Map<String, Object> configCache = new ConcurrentHashMap<>();
    private final Map<String, Long> configModificationTimes = new ConcurrentHashMap<>();
    
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
    
    // Phase 4: Hot-reload capability
    private boolean hotReloadEnabled = true;
    
    private ConfigManager() {
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .serializeNulls()
            .create();
        this.configPath = FMLPaths.CONFIGDIR.get().resolve("neoessentials");
        this.userConfigPath = configPath.resolve("user");
        
        // Create config directories
        createConfigDirectories();
        
        // Initialize language manager first
        LanguageManager.getInstance(configPath).initialize();
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
        String configName = fileName.replace(".json", "");
        
        // Update modification time
        configStatus.updateModified(configName, configFile.lastModified());
        
        if (!configFile.exists()) {
            LOGGER.info("Creating default configuration: {}", fileName);
            T defaultConfig = createDefaultConfig(configClass);
            saveConfig(fileName, defaultConfig);
            configStatus.markLoaded(configName, true);
            configStatus.markValid(configName, true, null);
            return defaultConfig;
        }
        
        try (FileReader reader = new FileReader(configFile)) {
            T config = gson.fromJson(reader, configClass);
            LOGGER.debug("Loaded configuration: {}", fileName);
            configStatus.markLoaded(configName, true);
            configStatus.markValid(configName, true, null);
            return config;
        } catch (IOException e) {
            LOGGER.error("Failed to load configuration: {}", fileName, e);
            configStatus.markLoaded(configName, false);
            configStatus.markValid(configName, false, e.getMessage());
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
        boolean allValid = true;
        
        for (String fileName : getAllConfigFiles()) {
            String configName = fileName.replace(".json", "");
            File configFile = getConfigFile(fileName);
            
            ConfigValidator.ValidationResult result = ConfigValidator.validateConfigFile(configFile, configName);
            
            if (!result.isValid()) {
                allValid = false;
                configStatus.markValid(configName, false, 
                    String.join("; ", result.getErrors()));
                LOGGER.warn("Configuration validation failed for {}: {}", 
                    configName, String.join(", ", result.getErrors()));
            } else {
                configStatus.markValid(configName, true, null);
                if (result.hasWarnings()) {
                    LOGGER.warn("Configuration warnings for {}: {}", 
                        configName, String.join(", ", result.getWarnings()));
                }
            }
        }
        
        return allValid;
    }
    
    /**
     * Validate a specific configuration
     */
    public ConfigValidator.ValidationResult validateConfiguration(String configName) {
        String fileName = configName + ".json";
        File configFile = getConfigFile(fileName);
        return ConfigValidator.validateConfigFile(configFile, configName);
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
     * Get configuration status tracker
     */
    public ConfigStatus getConfigStatus() {
        return configStatus;
    }
    
    /**
     * Check if configuration file exists
     */
    public boolean configExists(String fileName) {
        return configPath.resolve(fileName).toFile().exists();
    }
    
    /**
     * Get all configuration file names
     */
    public String[] getAllConfigFiles() {
        return new String[]{
            "main.json", "economy.json", "homes.json", "kits.json", 
            "warps.json", "moderation.json", "messaging.json", 
            "discord.json", "tablist.json", "spawn.json"
        };
    }
    
    /**
     * Force reload a specific configuration
     */
    public boolean reloadConfig(String configName) {
        try {
            switch (configName.toLowerCase()) {
                case "main" -> loadMainConfig();
                case "economy" -> loadEconomyConfig();
                case "homes" -> loadHomeConfig();
                case "kits" -> loadKitConfig();
                case "warps" -> loadWarpConfig();
                case "moderation" -> loadModerationConfig();
                case "messaging" -> loadMessagingConfig();
                case "discord" -> loadDiscordConfig();
                case "tablist" -> loadTablistConfig();
                case "spawn" -> loadSpawnConfig();
                default -> {
                    LOGGER.warn("Unknown configuration: {}", configName);
                    return false;
                }
            }
            LOGGER.info("Reloaded configuration: {}", configName);
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to reload configuration: {}", configName, e);
            return false;
        }
    }
    
    /**
     * Get configuration file modification time
     */
    public long getConfigModificationTime(String fileName) {
        File configFile = getConfigFile(fileName);
        return configFile.exists() ? configFile.lastModified() : 0;
    }
}
