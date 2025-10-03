package com.zerog.neoessentials.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zerog.neoessentials.util.ResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Unified, thread-safe configuration manager for NeoEssentials.
 * Handles all configuration loading, caching, and access in a consistent manner.
 */
public class ConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigManager.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    // Thread-safe singleton
    private static class SingletonHolder {
        private static final ConfigManager INSTANCE = new ConfigManager();
    }
    
    public static ConfigManager getInstance() {
        return SingletonHolder.INSTANCE;
    }
    
    // Thread-safe configuration cache
    private final ConcurrentHashMap<String, JsonObject> configCache = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private volatile boolean loaded = false;
    
    // Configuration file names
    public static final String MAIN_CONFIG = "config.json";
    public static final String ECONOMY_CONFIG = "economy.json";
    public static final String PERMISSIONS_CONFIG = "permissions.json";
    
    private ConfigManager() {
        // Private constructor for singleton
    }
    
    /**
     * Load all configuration files
     */
    public void loadAll() {
        lock.writeLock().lock();
        try {
            LOGGER.info("Loading NeoEssentials configurations...");
            
            // Ensure config directory exists
            ResourceUtil.ensureConfigDirectory();
            
            // Load main config
            loadConfig(MAIN_CONFIG);
            loadConfig(ECONOMY_CONFIG);
            loadConfig(PERMISSIONS_CONFIG);
            
            loaded = true;
            LOGGER.info("Configuration loading completed");
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Load a specific configuration file
     */
    private void loadConfig(String configName) {
        File configFile = ResourceUtil.getConfigFile(configName);
        
        // Extract default config if it doesn't exist
        if (!configFile.exists()) {
            extractDefaultConfig(configName, configFile);
        }
        
        // Load the configuration
        try (FileReader reader = new FileReader(configFile)) {
            JsonObject config = JsonParser.parseReader(reader).getAsJsonObject();
            configCache.put(configName, config);
            LOGGER.debug("Loaded configuration: {}", configName);
        } catch (Exception e) {
            LOGGER.error("Failed to load configuration {}: {}", configName, e.getMessage());
            // Put empty config to prevent repeated failures
            configCache.put(configName, new JsonObject());
        }
    }
    
    /**
     * Extract default configuration from JAR resources
     */
    private void extractDefaultConfig(String configName, File configFile) {
        try (InputStream in = ResourceUtil.getJarConfigResource(configName)) {
            if (in != null) {
                configFile.getParentFile().mkdirs();
                // Use Files.copy for efficient, safe resource transfer - no need for manual buffering
                Files.copy(in, configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                LOGGER.info("Extracted default configuration: {}", configName);
            } else {
                LOGGER.warn("Default configuration not found in JAR: {}", configName);
                // Create minimal config
                createMinimalConfig(configName, configFile);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to extract default configuration {}: {}", configName, e.getMessage());
            createMinimalConfig(configName, configFile);
        }
    }
    
    /**
     * Create minimal configuration if extraction fails
     */
    private void createMinimalConfig(String configName, File configFile) {
        JsonObject minimalConfig = new JsonObject();
        
        switch (configName) {
            case MAIN_CONFIG:
                JsonObject modules = new JsonObject();
                modules.addProperty("economyEnabled", true);
                modules.addProperty("permissionsEnabled", true);
                minimalConfig.add("modules", modules);
                
                JsonObject commands = new JsonObject();
                minimalConfig.add("commands", commands);
                break;
                
            case ECONOMY_CONFIG:
                JsonObject economySettings = new JsonObject();
                economySettings.addProperty("startingBalance", 100.0);
                economySettings.addProperty("currencySymbol", "$");
                economySettings.addProperty("maxBalance", 100000.0);
                minimalConfig.add("economySettings", economySettings);
                break;
                
            case PERMISSIONS_CONFIG:
                JsonObject defaultGroup = new JsonObject();
                defaultGroup.addProperty("default", true);
                minimalConfig.add("groups", defaultGroup);
                break;
        }
        
        try (FileWriter writer = new FileWriter(configFile)) {
            GSON.toJson(minimalConfig, writer);
            LOGGER.info("Created minimal configuration: {}", configName);
        } catch (IOException e) {
            LOGGER.error("Failed to create minimal configuration {}: {}", configName, e.getMessage());
        }
    }
    
    /**
     * Get a configuration object
     */
    public JsonObject getConfig(String configName) {
        if (!loaded) {
            loadAll();
        }
        
        lock.readLock().lock();
        try {
            return configCache.getOrDefault(configName, new JsonObject());
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Reload all configurations
     */
    public void reloadAll() {
        lock.writeLock().lock();
        try {
            configCache.clear();
            loaded = false;
            loadAll();
            LOGGER.info("All configurations reloaded");
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    // === CONVENIENCE METHODS FOR COMMON CONFIG ACCESS ===
    
    /**
     * Check if a command is enabled
     */
    public boolean isCommandEnabled(String command) {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("commands")) {
            JsonObject commands = config.getAsJsonObject("commands");
            if (commands.has(command)) {
                return commands.get(command).getAsBoolean();
            }
        }
        return true; // Default to enabled
    }
    
    /**
     * Check if economy is enabled
     */
    public boolean isEconomyEnabled() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("modules")) {
            JsonObject modules = config.getAsJsonObject("modules");
            if (modules.has("economyEnabled")) {
                return modules.get("economyEnabled").getAsBoolean();
            }
        }
        return true; // Default to enabled
    }
    
    /**
     * Get economy starting balance
     */
    public BigDecimal getEconomyStartingBalance() {
        JsonObject config = getConfig(ECONOMY_CONFIG);
        if (config.has("economySettings")) {
            JsonObject settings = config.getAsJsonObject("economySettings");
            if (settings.has("startingBalance")) {
                return settings.get("startingBalance").getAsBigDecimal();
            }
        }
        return new BigDecimal("100.0");
    }
    
    /**
     * Get currency symbol
     */
    public String getCurrencySymbol() {
        JsonObject config = getConfig(ECONOMY_CONFIG);
        if (config.has("economySettings")) {
            JsonObject settings = config.getAsJsonObject("economySettings");
            if (settings.has("currencySymbol")) {
                return settings.get("currencySymbol").getAsString();
            }
        }
        return "$";
    }
    
    /**
     * Get economy tax percentage for payments
     */
    public double getEconomyTaxPercentage() {
        JsonObject config = getConfig(ECONOMY_CONFIG);
        if (config.has("economySettings")) {
            JsonObject settings = config.getAsJsonObject("economySettings");
            if (settings.has("taxPercentage")) {
                return settings.get("taxPercentage").getAsDouble();
            }
        }
        return 0.0; // Default to no tax
    }
    

    
    /**
     * Get maximum balance
     */
    public BigDecimal getMaxBalance() {
        JsonObject config = getConfig(ECONOMY_CONFIG);
        if (config.has("economySettings")) {
            JsonObject settings = config.getAsJsonObject("economySettings");
            if (settings.has("maxBalance")) {
                return settings.get("maxBalance").getAsBigDecimal();
            }
        }
        return new BigDecimal("100000.0");
    }
    
    /**
     * Get tax percentage
     */
    public double getTaxPercentage() {
        JsonObject config = getConfig(ECONOMY_CONFIG);
        if (config.has("economySettings")) {
            JsonObject settings = config.getAsJsonObject("economySettings");
            if (settings.has("taxPercentage")) {
                return settings.get("taxPercentage").getAsDouble();
            }
        }
        return 1.5;
    }
    
    /**
     * Check if unsafe enchantments are allowed
     */
    public boolean isUnsafeEnchantsAllowed() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("items")) {
            JsonObject items = config.getAsJsonObject("items");
            if (items.has("unsafe-enchantments")) {
                return items.get("unsafe-enchantments").getAsBoolean();
            }
        }
        return false;
    }
    
    /**
     * Get item spawn blacklist
     */
    public List<String> getItemSpawnBlacklist() {
        List<String> blacklist = new ArrayList<>();
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("items")) {
            JsonObject items = config.getAsJsonObject("items");
            if (items.has("item-spawn-blacklist")) {
                items.getAsJsonArray("item-spawn-blacklist").forEach(element -> 
                    blacklist.add(element.getAsString())
                );
            }
        }
        return blacklist;
    }
}