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
        if (config.has("startingBalance")) {
            return config.get("startingBalance").getAsBigDecimal();
        }
        return new BigDecimal("100.0");
    }
    
    /**
     * Get currency symbol
     */
    public String getCurrencySymbol() {
        JsonObject config = getConfig(ECONOMY_CONFIG);
        if (config.has("currencySymbol")) {
            return config.get("currencySymbol").getAsString();
        }
        return "$";
    }
    
    /**
     * Get economy tax percentage for payments
     */
    public double getEconomyTaxPercentage() {
        JsonObject config = getConfig(ECONOMY_CONFIG);
        if (config.has("taxPercentage")) {
            return config.get("taxPercentage").getAsDouble();
        }
        return 0.0; // Default to no tax
    }
    

    
    /**
     * Get maximum balance
     */
    public BigDecimal getMaxBalance() {
        JsonObject config = getConfig(ECONOMY_CONFIG);
        if (config.has("maxBalance")) {
            return config.get("maxBalance").getAsBigDecimal();
        }
        return new BigDecimal("100000.0");
    }
    
    /**
     * Get tax percentage
     */
    public double getTaxPercentage() {
        JsonObject config = getConfig(ECONOMY_CONFIG);
        if (config.has("taxPercentage")) {
            return config.get("taxPercentage").getAsDouble();
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
     * Get the maximum level allowed for unsafe enchantments
     */
    public int getMaxUnsafeEnchantmentLevel() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("items")) {
            JsonObject items = config.getAsJsonObject("items");
            if (items.has("max-unsafe-enchantment-level")) {
                return items.get("max-unsafe-enchantment-level").getAsInt();
            }
        }
        return 32767; // Default maximum level for unsafe enchantments
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

    /**
     * Get maximum command length from security settings
     */
    public int getMaxCommandLength() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("security")) {
            JsonObject security = config.getAsJsonObject("security");
            if (security.has("maxCommandLength")) {
                return security.get("maxCommandLength").getAsInt();
            }
        }
        return 256; // Default fallback
    }

    /**
     * Get maximum reason length from security settings
     */
    public int getMaxReasonLength() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("security")) {
            JsonObject security = config.getAsJsonObject("security");
            if (security.has("maxReasonLength")) {
                return security.get("maxReasonLength").getAsInt();
            }
        }
        return 500; // Default fallback
    }

    /**
     * Get maximum economy amount from security/economy settings
     */
    public BigDecimal getMaxEconomyAmount() {
        JsonObject config = getConfig(ECONOMY_CONFIG);
        if (config.has("economySettings")) {
            JsonObject settings = config.getAsJsonObject("economySettings");
            if (settings.has("maxBalance")) {
                return new BigDecimal(settings.get("maxBalance").getAsString());
            }
        }
        return new BigDecimal("999999999.99"); // Default fallback
    }

    /**
     * Get minimum economy amount from economy settings
     */
    public BigDecimal getMinEconomyAmount() {
        JsonObject config = getConfig(ECONOMY_CONFIG);
        if (config.has("economySettings")) {
            JsonObject settings = config.getAsJsonObject("economySettings");
            if (settings.has("minTransferAmount")) {
                return new BigDecimal(settings.get("minTransferAmount").getAsString());
            }
        }
        return new BigDecimal("0.01"); // Default fallback
    }

    /**
     * Get pay cooldown seconds from payment settings
     */
    public int getPayCooldownSeconds() {
        JsonObject config = getConfig(ECONOMY_CONFIG);
        if (config.has("payCooldownSeconds")) {
            return config.get("payCooldownSeconds").getAsInt();
        }
        return 3; // Default fallback
    }

    /**
     * Get default permission group from permissions settings
     */
    public String getDefaultGroup() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("permissions")) {
            JsonObject permissions = config.getAsJsonObject("permissions");
            if (permissions.has("defaultGroup")) {
                return permissions.get("defaultGroup").getAsString();
            }
        }
        return "default"; // Default fallback
    }

    // === ECONOMY CONFIGURATION METHODS ===

    /**
     * Check if negative balances are allowed
     */
    public boolean allowNegativeBalances() {
        JsonObject config = getConfig(ECONOMY_CONFIG);
        if (config.has("allowNegativeBalances")) {
            return config.get("allowNegativeBalances").getAsBoolean();
        }
        return false; // Default to not allowing negative balances
    }

    /**
     * Check if inactive account cleanup is enabled
     */
    public boolean isCleanupInactiveAccountsEnabled() {
        JsonObject config = getConfig(ECONOMY_CONFIG);
        if (config.has("cleanupInactiveAccounts")) {
            return config.get("cleanupInactiveAccounts").getAsBoolean();
        }
        return true; // Default to enabled
    }

    /**
     * Get inactive account cleanup days
     */
    public int getInactiveAccountCleanupDays() {
        JsonObject config = getConfig(ECONOMY_CONFIG);
        if (config.has("inactiveAccountCleanupDays")) {
            return config.get("inactiveAccountCleanupDays").getAsInt();
        }
        return 30; // Default to 30 days
    }

    /**
     * Get maximum transfer amount
     */
    public BigDecimal getMaxTransferAmount() {
        JsonObject config = getConfig(ECONOMY_CONFIG);
        if (config.has("maxTransferAmount")) {
            return config.get("maxTransferAmount").getAsBigDecimal();
        }
        return new BigDecimal("10000.0"); // Default max transfer
    }

    /**
     * Get default pay toggle setting
     */
    public boolean getPayToggleDefault() {
        JsonObject config = getConfig(ECONOMY_CONFIG);
        if (config.has("paytoggleDefault")) {
            return config.get("paytoggleDefault").getAsBoolean();
        }
        return true; // Default to allowing payments
    }

    /**
     * Get cache maximum size
     */
    public int getCacheMaximumSize() {
        JsonObject config = getConfig(ECONOMY_CONFIG);
        if (config.has("cacheMaximumSize")) {
            return config.get("cacheMaximumSize").getAsInt();
        }
        return 10000; // Default cache size
    }

    /**
     * Get cache expire after access minutes
     */
    public int getCacheExpireAfterAccessMinutes() {
        JsonObject config = getConfig(ECONOMY_CONFIG);
        if (config.has("cacheExpireAfterAccessMinutes")) {
            return config.get("cacheExpireAfterAccessMinutes").getAsInt();
        }
        return 60; // Default to 60 minutes
    }
}