package com.zerog.neoessentials.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zerog.neoessentials.localization.LanguageManager;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Enhanced Configuration Manager for NeoEssentials
 * Phase 4: Complete Configuration System Overhaul
 * 
 * Features:
 * - User-friendly JSON configuration files
 * - Hot-reload capability
 * - Configuration validation
 * - Default value management
 * - Per-module configuration sections
 * - Easy customization interface
 * - Configuration templates and backup system
 * 
 * @author ZeroG
 * @since 2.0.0 (Phase 4 Enhanced)
 */
public class EnhancedConfigManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(EnhancedConfigManager.class);
    private static EnhancedConfigManager instance;
    private final Gson gson;
    private final Path configPath;
    private final Path userConfigPath;
    private final Path templatesPath;
    private final Path backupPath;
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
    
    // Phase 4: Enhanced features
    private boolean hotReloadEnabled = true;
    private boolean autoBackupEnabled = true;
    private ScheduledExecutorService hotReloadExecutor;
    private WatchService configWatcher;
    
    private EnhancedConfigManager() {
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .serializeNulls()
            .create();
        this.configPath = FMLPaths.CONFIGDIR.get().resolve("neoessentials");
        this.userConfigPath = configPath.resolve("user");
        this.templatesPath = configPath.resolve("templates");
        this.backupPath = configPath.resolve("backup");
        
        // Create config directories
        createConfigDirectories();
        
        // Initialize language manager first
        LanguageManager.getInstance(configPath).initialize();
    }
    
    public static EnhancedConfigManager getInstance() {
        if (instance == null) {
            instance = new EnhancedConfigManager();
        }
        return instance;
    }
    
    /**
     * Phase 4: Initialize all configurations with enhanced features
     */
    public void initialize() {
        LOGGER.info("Initializing NeoEssentials Enhanced Configuration System (Phase 4)...");
        
        try {
            // Create default configurations if they don't exist
            createDefaultConfigurations();
            
            // Load all configuration files
            loadAllConfigurations();
            
            // Validate configurations
            validateAllConfigurations();
            
            // Setup hot-reload if enabled
            if (hotReloadEnabled) {
                setupHotReload();
            }
            
            LOGGER.info("Enhanced Configuration system initialized successfully!");
            
        } catch (Exception e) {
            LOGGER.error("Failed to initialize enhanced configuration system", e);
        }
    }
    
    /**
     * Create config directories with proper structure
     */
    private void createConfigDirectories() {
        try {
            Files.createDirectories(configPath);
            Files.createDirectories(userConfigPath);
            Files.createDirectories(templatesPath);
            Files.createDirectories(backupPath);
            
            LOGGER.info("Created NeoEssentials config directories");
        } catch (Exception e) {
            LOGGER.error("Failed to create config directories", e);
        }
    }
    
    /**
     * Create default configuration files
     */
    private void createDefaultConfigurations() {
        LOGGER.info("Creating default configuration files...");
        
        // Create main configuration template
        createConfigurationTemplate("main.json", new MainConfig());
        createConfigurationTemplate("economy.json", new EconomyConfig());
        createConfigurationTemplate("homes.json", new HomeConfig());
        createConfigurationTemplate("kits.json", new KitConfig());
        createConfigurationTemplate("warps.json", new WarpConfig());
        createConfigurationTemplate("moderation.json", new ModerationConfig());
        createConfigurationTemplate("messaging.json", new MessagingConfig());
        createConfigurationTemplate("discord.json", new DiscordConfig());
        createConfigurationTemplate("tablist.json", new TablistConfig());
        createConfigurationTemplate("spawn.json", new SpawnConfig());
        
        // Create README file for users
        createConfigurationReadme();
    }
    
    /**
     * Create a configuration template file
     */
    private <T> void createConfigurationTemplate(String fileName, T defaultConfig) {
        Path configFile = configPath.resolve(fileName);
        Path templateFile = templatesPath.resolve(fileName);
        
        try {
            // Create template file (always overwrite templates)
            try (FileWriter writer = new FileWriter(templateFile.toFile())) {
                gson.toJson(defaultConfig, writer);
            }
            
            // Create user config file if it doesn't exist
            if (!Files.exists(configFile)) {
                try (FileWriter writer = new FileWriter(configFile.toFile())) {
                    gson.toJson(defaultConfig, writer);
                }
                LOGGER.info("Created default configuration: {}", fileName);
            }
            
            // Update modification time tracking
            configModificationTimes.put(fileName, configFile.toFile().lastModified());
            
        } catch (IOException e) {
            LOGGER.error("Failed to create configuration template: {}", fileName, e);
        }
    }
    
    /**
     * Create a README file explaining configuration customization
     */
    private void createConfigurationReadme() {
        Path readmePath = configPath.resolve("README.md");
        
        try (FileWriter writer = new FileWriter(readmePath.toFile())) {
            writer.write("# NeoEssentials Configuration Guide\n\n");
            writer.write("Welcome to NeoEssentials! This folder contains all configuration files for the mod.\n\n");
            writer.write("## Configuration Files\n\n");
            writer.write("- `main.json` - Main mod settings and general configuration\n");
            writer.write("- `economy.json` - Economy system settings\n");
            writer.write("- `homes.json` - Home system configuration\n");
            writer.write("- `kits.json` - Kit system settings\n");
            writer.write("- `warps.json` - Warp system configuration\n");
            writer.write("- `moderation.json` - Moderation tools settings\n");
            writer.write("- `messaging.json` - Chat and messaging configuration\n");
            writer.write("- `discord.json` - Discord integration settings\n");
            writer.write("- `tablist.json` - Tab list customization\n");
            writer.write("- `spawn.json` - Spawn system configuration\n\n");
            writer.write("## Folders\n\n");
            writer.write("- `templates/` - Default configuration templates (do not edit)\n");
            writer.write("- `backup/` - Automatic configuration backups\n");
            writer.write("- `user/` - User-specific configurations\n");
            writer.write("- `languages/` - Language files for localization\n\n");
            writer.write("## Customization\n\n");
            writer.write("1. Edit the JSON files directly for your server's needs\n");
            writer.write("2. Use `/config reload` in-game to apply changes without restart\n");
            writer.write("3. Use `/config status` to check configuration health\n");
            writer.write("4. Backups are created automatically when changes are detected\n\n");
            writer.write("## Support\n\n");
            writer.write("If you encounter issues, check the server logs or use `/config validate` for diagnostics.\n");
            
        } catch (IOException e) {
            LOGGER.error("Failed to create configuration README", e);
        }
    }
    
    /**
     * Load all configurations
     */
    private void loadAllConfigurations() {
        LOGGER.info("Loading all configuration files...");
        
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
    }
    
    /**
     * Setup hot-reload monitoring
     */
    private void setupHotReload() {
        if (!hotReloadEnabled) {
            LOGGER.info("Hot-reload is disabled");
            return;
        }
        
        try {
            // Initialize file system watcher
            configWatcher = FileSystems.getDefault().newWatchService();
            
            // Watch the config directory for modifications
            configPath.register(configWatcher, 
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_CREATE);
            
            // Start background thread for file watching
            hotReloadExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "NeoEssentials-HotReload");
                t.setDaemon(true);
                return t;
            });
            
            // Check for file changes every 2 seconds
            hotReloadExecutor.scheduleAtFixedRate(this::checkForConfigChanges, 2, 2, TimeUnit.SECONDS);
            
            LOGGER.info("Hot-reload monitoring enabled - watching directory: {}", configPath);
            
        } catch (IOException e) {
            LOGGER.error("Failed to setup hot-reload monitoring", e);
            hotReloadEnabled = false;
        }
    }
    
    /**
     * Check for configuration file changes and reload them
     */
    private void checkForConfigChanges() {
        if (configWatcher == null) return;
        
        try {
            WatchKey key = configWatcher.poll();
            if (key != null) {
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    
                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }
                    
                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                    Path filename = pathEvent.context();
                    
                    if (filename.toString().endsWith(".json")) {
                        String fileName = filename.toString();
                        
                        // Add small delay to ensure file write is complete
                        Thread.sleep(100);
                        
                        if (hotReloadIfChanged(fileName)) {
                            LOGGER.info("Auto-reloaded configuration file: {}", fileName);
                        }
                    }
                }
                
                boolean valid = key.reset();
                if (!valid) {
                    LOGGER.warn("Config directory watch key became invalid");
                    return; // Exit method if key becomes invalid
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("Error during hot-reload file watching", e);
        }
    }
    
    /**
     * Shutdown hot-reload monitoring
     */
    public void shutdownHotReload() {
        if (hotReloadExecutor != null) {
            hotReloadExecutor.shutdown();
            try {
                if (!hotReloadExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    hotReloadExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                hotReloadExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        if (configWatcher != null) {
            try {
                configWatcher.close();
            } catch (IOException e) {
                LOGGER.error("Error closing config watcher", e);
            }
        }
        
        LOGGER.info("Hot-reload monitoring shutdown");
    }
    
    /**
     * Validate all configurations
     */
    private boolean validateAllConfigurations() {
        boolean allValid = true;
        
        LOGGER.info("Validating all configurations...");
        
        String[] configFiles = {
            "main.json", "economy.json", "homes.json", "kits.json", "warps.json",
            "moderation.json", "messaging.json", "discord.json", "tablist.json", "spawn.json"
        };
        
        for (String fileName : configFiles) {
            String configName = fileName.replace(".json", "");
            File configFile = configPath.resolve(fileName).toFile();
            
            ConfigValidator.ValidationResult result = ConfigValidator.validateConfigFile(configFile, configName);
            
            if (!result.isValid()) {
                allValid = false;
                configStatus.markValid(configName, false, String.join("; ", result.getErrors()));
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
        
        if (allValid) {
            LOGGER.info("All configurations validated successfully!");
        } else {
            LOGGER.warn("Some configurations have validation errors - check logs for details");
        }
        
        return allValid;
    }
    
    // Load methods (same as before)
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
    
    /**
     * Generic configuration loading with enhanced error handling
     */
    private <T> T loadConfig(String fileName, Class<T> configClass) {
        File configFile = configPath.resolve(fileName).toFile();
        String configName = fileName.replace(".json", "");
        
        // Update modification time
        long currentModTime = configFile.lastModified();
        configModificationTimes.put(fileName, currentModTime);
        
        if (!configFile.exists()) {
            LOGGER.info("Configuration file not found, creating default: {}", fileName);
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
            
            // Cache the configuration
            configCache.put(configName, config);
            
            return config;
        } catch (IOException e) {
            LOGGER.error("Failed to load configuration: {}", fileName, e);
            configStatus.markLoaded(configName, false);
            configStatus.markValid(configName, false, e.getMessage());
            return createDefaultConfig(configClass);
        }
    }
    
    /**
     * Generic configuration saving with backup
     */
    private <T> void saveConfig(String fileName, T config) {
        if (config == null) {
            LOGGER.warn("Attempted to save null configuration: {}", fileName);
            return;
        }
        
        File configFile = configPath.resolve(fileName).toFile();
        
        // Create backup if auto-backup is enabled
        if (autoBackupEnabled && configFile.exists()) {
            createConfigBackup(fileName);
        }
        
        try (FileWriter writer = new FileWriter(configFile)) {
            gson.toJson(config, writer);
            LOGGER.debug("Saved configuration: {}", fileName);
            
            // Update cache and modification time
            String configName = fileName.replace(".json", "");
            configCache.put(configName, config);
            configModificationTimes.put(fileName, configFile.lastModified());
            
        } catch (IOException e) {
            LOGGER.error("Failed to save configuration: {}", fileName, e);
        }
    }
    
    /**
     * Create a backup of a configuration file
     */
    private void createConfigBackup(String fileName) {
        try {
            File sourceFile = configPath.resolve(fileName).toFile();
            String timestamp = String.valueOf(System.currentTimeMillis());
            String backupFileName = fileName.replace(".json", "_" + timestamp + ".json");
            File backupFile = backupPath.resolve(backupFileName).toFile();
            
            Files.copy(sourceFile.toPath(), backupFile.toPath());
            LOGGER.debug("Created backup for {}: {}", fileName, backupFileName);
            
        } catch (IOException e) {
            LOGGER.error("Failed to create backup for {}", fileName, e);
        }
    }
    
    /**
     * Create default configuration instance
     */
    private <T> T createDefaultConfig(Class<T> configClass) {
        try {
            return configClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            LOGGER.error("Failed to create default configuration for: {}", configClass.getSimpleName(), e);
            return null;
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
        
        // Clear cache
        configCache.clear();
        
        // Reload all configurations
        loadAllConfigurations();
        
        // Validate after reload
        validateAllConfigurations();
        
        LOGGER.info("Configuration reload completed!");
    }
    
    // Save methods
    private void saveMainConfig() { saveConfig("main.json", mainConfig); }
    private void saveEconomyConfig() { saveConfig("economy.json", economyConfig); }
    private void saveHomeConfig() { saveConfig("homes.json", homeConfig); }
    private void saveKitConfig() { saveConfig("kits.json", kitConfig); }
    private void saveWarpConfig() { saveConfig("warps.json", warpConfig); }
    private void saveModerationConfig() { saveConfig("moderation.json", moderationConfig); }
    private void saveMessagingConfig() { saveConfig("messaging.json", messagingConfig); }
    private void saveDiscordConfig() { saveConfig("discord.json", discordConfig); }
    private void saveTablistConfig() { saveConfig("tablist.json", tablistConfig); }
    private void saveSpawnConfig() { saveConfig("spawn.json", spawnConfig); }
    
    // Configuration getters with null safety
    public MainConfig getMainConfig() { return mainConfig != null ? mainConfig : new MainConfig(); }
    public EconomyConfig getEconomyConfig() { return economyConfig != null ? economyConfig : new EconomyConfig(); }
    public HomeConfig getHomeConfig() { return homeConfig != null ? homeConfig : new HomeConfig(); }
    public KitConfig getKitConfig() { return kitConfig != null ? kitConfig : new KitConfig(); }
    public WarpConfig getWarpConfig() { return warpConfig != null ? warpConfig : new WarpConfig(); }
    public ModerationConfig getModerationConfig() { return moderationConfig != null ? moderationConfig : new ModerationConfig(); }
    public MessagingConfig getMessagingConfig() { return messagingConfig != null ? messagingConfig : new MessagingConfig(); }
    public DiscordConfig getDiscordConfig() { return discordConfig != null ? discordConfig : new DiscordConfig(); }
    public TablistConfig getTablistConfig() { return tablistConfig != null ? tablistConfig : new TablistConfig(); }
    public SpawnConfig getSpawnConfig() { return spawnConfig != null ? spawnConfig : new SpawnConfig(); }
    
    // Utility methods
    public Path getConfigPath() { return configPath; }
    public ConfigStatus getConfigStatus() { return configStatus; }
    public boolean configExists(String fileName) { return configPath.resolve(fileName).toFile().exists(); }
    public File getConfigFile(String fileName) { return configPath.resolve(fileName).toFile(); }
    
    public String[] getAllConfigFiles() {
        return new String[] {
            "main.json", "economy.json", "homes.json", "kits.json", "warps.json",
            "moderation.json", "messaging.json", "discord.json", "tablist.json", "spawn.json"
        };
    }
    
    /**
     * Check if a configuration needs reloading based on file modification time
     */
    public boolean needsReload(String fileName) {
        File configFile = configPath.resolve(fileName).toFile();
        if (!configFile.exists()) return false;
        
        Long cachedTime = configModificationTimes.get(fileName);
        if (cachedTime == null) return true;
        
        return configFile.lastModified() > cachedTime;
    }
    
    /**
     * Hot-reload a specific configuration if it has changed
     */
    public boolean hotReloadIfChanged(String fileName) {
        if (!hotReloadEnabled || !needsReload(fileName)) {
            return false;
        }
        
        LOGGER.info("Hot-reloading configuration: {}", fileName);
        
        try {
            switch (fileName) {
                case "main.json": loadMainConfig(); break;
                case "economy.json": loadEconomyConfig(); break;
                case "homes.json": loadHomeConfig(); break;
                case "kits.json": loadKitConfig(); break;
                case "warps.json": loadWarpConfig(); break;
                case "moderation.json": loadModerationConfig(); break;
                case "messaging.json": loadMessagingConfig(); break;
                case "discord.json": loadDiscordConfig(); break;
                case "tablist.json": loadTablistConfig(); break;
                case "spawn.json": loadSpawnConfig(); break;
                default: return false;
            }
            
            LOGGER.info("Successfully hot-reloaded: {}", fileName);
            return true;
            
        } catch (Exception e) {
            LOGGER.error("Failed to hot-reload configuration: {}", fileName, e);
            return false;
        }
    }
    
    /**
     * Get configuration value from cache or file
     */
    @SuppressWarnings("unchecked")
    public <T> T getConfigValue(String configName, String key, T defaultValue) {
        Object cachedConfig = configCache.get(configName);
        if (cachedConfig != null) {
            // TODO: Implement deep property access for nested configuration values
            return defaultValue; // Placeholder for now
        }
        return defaultValue;
    }
    
    /**
     * Set hot-reload enabled/disabled
     */
    public void setHotReloadEnabled(boolean enabled) {
        this.hotReloadEnabled = enabled;
        LOGGER.info("Hot-reload {}", enabled ? "enabled" : "disabled");
    }
    
    /**
     * Set auto-backup enabled/disabled
     */
    public void setAutoBackupEnabled(boolean enabled) {
        this.autoBackupEnabled = enabled;
        LOGGER.info("Auto-backup {}", enabled ? "enabled" : "disabled");
    }
    
    /**
     * Cleanup resources when shutting down
     */
    public void shutdown() {
        shutdownHotReload();
        LOGGER.info("Enhanced Configuration Manager shutdown complete");
    }
}
