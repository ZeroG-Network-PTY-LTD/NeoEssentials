package com.zerog.neoessentials.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
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
    public static final String KITS_CONFIG = "kits.json";
    
    // Config version tracking - increment when structure changes
    private static final String CONFIG_VERSION_KEY = "_configVersion";
    private static final int CURRENT_CONFIG_VERSION = 8;
    
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
            loadConfig(KITS_CONFIG);
            
            loaded = true;
            LOGGER.info("Configuration loading completed");
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Load a specific configuration file with smart updating
     */
    private void loadConfig(String configName) {
        File configFile = ResourceUtil.getConfigFile(configName);
        
        // Extract default config if it doesn't exist
        if (!configFile.exists()) {
            extractDefaultConfig(configName, configFile);
        } else {
            // Check if config needs updating
            if (shouldUpdateConfig(configName, configFile)) {
                LOGGER.info("Config structure outdated for {}, updating...", configName);
                updateConfigWithBackup(configName, configFile);
            }
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
        
        // Add version to all minimal configs
        minimalConfig.addProperty(CONFIG_VERSION_KEY, CURRENT_CONFIG_VERSION);
        minimalConfig.addProperty(CONFIG_VERSION_KEY + "_comment", 
            "DO NOT MODIFY: This field is used by NeoEssentials for automatic config updates. Changing this may cause config corruption.");
        
        switch (configName) {
            case MAIN_CONFIG:
                JsonObject modules = new JsonObject();
                modules.addProperty("economyEnabled", true);
                modules.addProperty("permissionsEnabled", true);
                modules.addProperty("webDashboardEnabled", true);
                minimalConfig.add("modules", modules);
                
                JsonObject commands = new JsonObject();
                minimalConfig.add("commands", commands);
                
                JsonObject webDashboard = new JsonObject();
                webDashboard.addProperty("enabled", true);
                webDashboard.addProperty("autoStart", false);
                webDashboard.addProperty("port", 8080);
                minimalConfig.add("webDashboard", webDashboard);
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
                
            case KITS_CONFIG:
                minimalConfig.add("kits", new JsonArray());
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
     * Check if a config file needs updating by comparing structure with JAR template
     */
    private boolean shouldUpdateConfig(String configName, File configFile) {
        try {
            // Load existing config
            JsonObject existingConfig;
            try (FileReader reader = new FileReader(configFile)) {
                existingConfig = JsonParser.parseReader(reader).getAsJsonObject();
            }
            
            // Load template config from JAR
            JsonObject templateConfig = loadJarConfig(configName);
            if (templateConfig == null) {
                return false; // No template to compare against
            }
            
            // Check version first
            int existingVersion = existingConfig.has(CONFIG_VERSION_KEY) ? 
                existingConfig.get(CONFIG_VERSION_KEY).getAsInt() : 0;
            int templateVersion = templateConfig.has(CONFIG_VERSION_KEY) ? 
                templateConfig.get(CONFIG_VERSION_KEY).getAsInt() : CURRENT_CONFIG_VERSION;
                
            if (existingVersion < templateVersion) {
                LOGGER.info("Config version outdated: existing={}, template={}", existingVersion, templateVersion);
                return true;
            }
            
            // Compare structure (keys/sections)
            return !configStructuresMatch(existingConfig, templateConfig);
            
        } catch (Exception e) {
            LOGGER.warn("Error checking config update status for {}: {}", configName, e.getMessage());
            return false;
        }
    }
    
    /**
     * Load config template from JAR resources
     */
    private JsonObject loadJarConfig(String configName) {
        InputStream in = null;
        try {
            // Handle special cases for files in /data/ not /data/config/neoessentials/
            if (PERMISSIONS_CONFIG.equals(configName)) {
                in = ResourceUtil.class.getResourceAsStream("/data/permissions.json");
            } else if (ECONOMY_CONFIG.equals(configName)) {
                in = ResourceUtil.class.getResourceAsStream("/data/economy.json");
            } else {
                in = ResourceUtil.getJarConfigResource(configName);
            }
            
            if (in != null) {
                String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                return JsonParser.parseString(content).getAsJsonObject();
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to load JAR config template {}: {}", configName, e.getMessage());
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    LOGGER.warn("Failed to close JAR config stream for {}", configName);
                }
            }
        }
        return null;
    }
    
    /**
     * Compare config structures to see if they have the same keys/sections
     */
    private boolean configStructuresMatch(JsonObject existing, JsonObject template) {
        // Compare top-level keys (excluding version fields and comments)
        Set<String> existingKeys = existing.keySet().stream()
            .filter(key -> !key.equals(CONFIG_VERSION_KEY) && !key.endsWith("_comment"))
            .collect(Collectors.toSet());
        Set<String> templateKeys = template.keySet().stream()
            .filter(key -> !key.equals(CONFIG_VERSION_KEY) && !key.endsWith("_comment"))
            .collect(Collectors.toSet());
            
        if (!existingKeys.equals(templateKeys)) {
            LOGGER.debug("Top-level keys differ: existing={}, template={}", existingKeys, templateKeys);
            return false;
        }
        
        // Compare nested object structures
        for (String key : templateKeys) {
            JsonElement existingElement = existing.get(key);
            JsonElement templateElement = template.get(key);
            
            if (existingElement.isJsonObject() && templateElement.isJsonObject()) {
                if (!nestedKeysMatch(existingElement.getAsJsonObject(), templateElement.getAsJsonObject())) {
                    LOGGER.debug("Nested keys differ in section: {}", key);
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * Compare nested object keys recursively
     */
    private boolean nestedKeysMatch(JsonObject existing, JsonObject template) {
        Set<String> existingKeys = existing.keySet();
        Set<String> templateKeys = template.keySet();
        
        if (!existingKeys.containsAll(templateKeys)) {
            return false; // Template has new keys
        }
        
        // Check nested objects
        for (String key : templateKeys) {
            JsonElement existingElement = existing.get(key);
            JsonElement templateElement = template.get(key);
            
            if (existingElement != null && existingElement.isJsonObject() && templateElement.isJsonObject()) {
                if (!nestedKeysMatch(existingElement.getAsJsonObject(), templateElement.getAsJsonObject())) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * Update config file with backup of old version
     */
    private void updateConfigWithBackup(String configName, File configFile) {
        try {
            // Create backup with incremental numbering
            File backupFile = createBackupFile(configFile);
            Files.copy(configFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            LOGGER.info("Created backup: {}", backupFile.getName());
            
            // Load old config values
            JsonObject oldConfig;
            try (FileReader reader = new FileReader(configFile)) {
                oldConfig = JsonParser.parseReader(reader).getAsJsonObject();
            }
            
            // Load new template
            JsonObject newTemplate = loadJarConfig(configName);
            if (newTemplate == null) {
                LOGGER.error("Could not load new template for {}", configName);
                return;
            }
            
            // Merge old values into new structure
            JsonObject mergedConfig = mergeConfigs(oldConfig, newTemplate);
            
            // Add version marker and comment
            mergedConfig.addProperty(CONFIG_VERSION_KEY, CURRENT_CONFIG_VERSION);
            mergedConfig.addProperty(CONFIG_VERSION_KEY + "_comment", 
                "DO NOT MODIFY: This field is used by NeoEssentials for automatic config updates. Changing this may cause config corruption.");
            
            // Write updated config
            try (FileWriter writer = new FileWriter(configFile)) {
                GSON.toJson(mergedConfig, writer);
                LOGGER.info("Updated configuration: {} (backup saved as {})", configName, backupFile.getName());
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to update config {}: {}", configName, e.getMessage());
        }
    }
    
    /**
     * Create backup file with incremental numbering (.bak1, .bak2, etc.)
     */
    private File createBackupFile(File originalFile) {
        File parent = originalFile.getParentFile();
        String baseName = originalFile.getName();
        
        for (int i = 1; i <= 999; i++) {
            File backupFile = new File(parent, baseName + ".bak" + i);
            if (!backupFile.exists()) {
                return backupFile;
            }
        }
        
        // Fallback if we somehow have 999 backups
        return new File(parent, baseName + ".bak999");
    }
    
    /**
     * Merge old config values into new template structure
     */
    private JsonObject mergeConfigs(JsonObject oldConfig, JsonObject newTemplate) {
        JsonObject merged = newTemplate.deepCopy();
        
        // Recursively merge old values
        mergeJsonObjects(merged, oldConfig);
        
        return merged;
    }
    
    /**
     * Recursively merge old values into new structure, preserving user customizations
     */
    private void mergeJsonObjects(JsonObject target, JsonObject source) {
        for (Map.Entry<String, JsonElement> entry : source.entrySet()) {
            String key = entry.getKey();
            JsonElement sourceValue = entry.getValue();
            
            // Skip version keys and comment fields
            if (key.equals(CONFIG_VERSION_KEY) || key.endsWith("_comment")) {
                continue;
            }
            
            // If target has this key, merge values
            if (target.has(key)) {
                JsonElement targetValue = target.get(key);
                
                if (sourceValue.isJsonObject() && targetValue.isJsonObject()) {
                    // Recursively merge objects
                    mergeJsonObjects(targetValue.getAsJsonObject(), sourceValue.getAsJsonObject());
                } else if (!sourceValue.isJsonObject()) {
                    // Use old value for primitives (preserve user settings)
                    target.add(key, sourceValue);
                }
            }
            // Don't add keys that don't exist in new template
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
     * Check if moderation system is enabled
     */
    public boolean isModerationEnabled() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("modules")) {
            JsonObject modules = config.getAsJsonObject("modules");
            if (modules.has("moderationEnabled")) {
                return modules.get("moderationEnabled").getAsBoolean();
            }
        }
        return true; // Default to enabled
    }
    
    /**
     * Check if chat system is enabled
     */
    public boolean isChatEnabled() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("modules")) {
            JsonObject modules = config.getAsJsonObject("modules");
            if (modules.has("chatEnabled")) {
                return modules.get("chatEnabled").getAsBoolean();
            }
        }
        return true; // Default to enabled
    }
    
    /**
     * Check if permissions system is enabled
     */
    public boolean isPermissionsEnabled() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("modules")) {
            JsonObject modules = config.getAsJsonObject("modules");
            if (modules.has("permissionsEnabled")) {
                return modules.get("permissionsEnabled").getAsBoolean();
            }
        }
        return true; // Default to enabled
    }
    
    /**
     * Check if web dashboard is enabled
     */
    public boolean isWebDashboardEnabled() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("modules")) {
            JsonObject modules = config.getAsJsonObject("modules");
            if (modules.has("webDashboardEnabled")) {
                return modules.get("webDashboardEnabled").getAsBoolean();
            }
        }
        return true; // Default to enabled
    }
    
    /**
     * Check if web dashboard should auto-start on server launch
     */
    public boolean isWebDashboardAutoStartEnabled() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("webDashboard")) {
            JsonObject webDashboard = config.getAsJsonObject("webDashboard");
            if (webDashboard.has("autoStart")) {
                return webDashboard.get("autoStart").getAsBoolean();
            }
        }
        return false; // Default to manual start
    }
    
    /**
     * Get web dashboard port
     */
    public int getWebDashboardPort() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("webDashboard")) {
            JsonObject webDashboard = config.getAsJsonObject("webDashboard");
            if (webDashboard.has("port")) {
                return webDashboard.get("port").getAsInt();
            }
        }
        return 8080; // Default port
    }
    
    /**
     * Get web dashboard bind address
     */
    public String getWebDashboardBindAddress() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("webDashboard")) {
            JsonObject webDashboard = config.getAsJsonObject("webDashboard");
            if (webDashboard.has("bindAddress")) {
                return webDashboard.get("bindAddress").getAsString();
            }
        }
        return "127.0.0.1"; // Default to localhost only
    }
    
    /**
     * Check if CORS is enabled for web dashboard
     */
    public boolean isWebDashboardCORSEnabled() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("webDashboard")) {
            JsonObject webDashboard = config.getAsJsonObject("webDashboard");
            if (webDashboard.has("enableCORS")) {
                return webDashboard.get("enableCORS").getAsBoolean();
            }
        }
        return true; // Default to enabled
    }
    
    /**
     * Get web dashboard max threads
     */
    public int getWebDashboardMaxThreads() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("webDashboard")) {
            JsonObject webDashboard = config.getAsJsonObject("webDashboard");
            if (webDashboard.has("maxThreads")) {
                return webDashboard.get("maxThreads").getAsInt();
            }
        }
        return 4; // Default thread count
    }
    
    /**
     * Check if config editing is allowed via web dashboard
     */
    public boolean isWebDashboardConfigEditingAllowed() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("webDashboard")) {
            JsonObject webDashboard = config.getAsJsonObject("webDashboard");
            if (webDashboard.has("securitySettings")) {
                JsonObject security = webDashboard.getAsJsonObject("securitySettings");
                if (security.has("allowConfigEditing")) {
                    return security.get("allowConfigEditing").getAsBoolean();
                }
            }
        }
        return true; // Default to allowed
    }
    
    /**
     * Get max log lines for web dashboard
     */
    public int getWebDashboardMaxLogLines() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("webDashboard")) {
            JsonObject webDashboard = config.getAsJsonObject("webDashboard");
            if (webDashboard.has("apiSettings")) {
                JsonObject apiSettings = webDashboard.getAsJsonObject("apiSettings");
                if (apiSettings.has("maxLogLines")) {
                    return apiSettings.get("maxLogLines").getAsInt();
                }
            }
        }
        return 1000; // Default max lines
    }
    
    /**
     * Get default log lines for web dashboard
     */
    public int getWebDashboardDefaultLogLines() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("webDashboard")) {
            JsonObject webDashboard = config.getAsJsonObject("webDashboard");
            if (webDashboard.has("apiSettings")) {
                JsonObject apiSettings = webDashboard.getAsJsonObject("apiSettings");
                if (apiSettings.has("defaultLogLines")) {
                    return apiSettings.get("defaultLogLines").getAsInt();
                }
            }
        }
        return 100; // Default lines
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
     * Check if unsafe commands are allowed from security settings
     */
    public boolean isUnsafeCommandsAllowed() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("security")) {
            JsonObject security = config.getAsJsonObject("security");
            if (security.has("allowUnsafeCommands")) {
                return security.get("allowUnsafeCommands").getAsBoolean();
            }
        }
        return false; // Default to safe mode
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
    
    /**
     * Check if debug logging is enabled
     */
    public boolean isDebugLoggingEnabled() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("logging")) {
            JsonObject logging = config.getAsJsonObject("logging");
            if (logging.has("enableDebugLogging")) {
                return logging.get("enableDebugLogging").getAsBoolean();
            }
        }
        return false; // Default to disabled
    }

    // === KIT MODULE CONFIGURATION METHODS ===
    
    /**
     * Check if the kit module is enabled
     */
    public boolean isKitModuleEnabled() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("modules")) {
            JsonObject modules = config.getAsJsonObject("modules");
            if (modules.has("kitsEnabled")) {
                return modules.get("kitsEnabled").getAsBoolean();
            }
        }
        return true; // Default to enabled for backwards compatibility
    }
    
    /**
     * Check if kit system is enabled (also checks module level)
     */
    public boolean isKitSystemEnabled() {
        if (!isKitModuleEnabled()) return false;
        
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("kits")) {
            JsonObject kits = config.getAsJsonObject("kits");
            if (kits.has("enabled")) {
                return kits.get("enabled").getAsBoolean();
            }
        }
        return true; // Default to enabled
    }
    
    /**
     * Check if one-time kits should be skipped from kit list when used
     */
    public boolean shouldSkipUsedOneTimeKitsFromList() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("kits")) {
            JsonObject kits = config.getAsJsonObject("kits");
            if (kits.has("skipUsedOneTimeKitsFromKitList")) {
                return kits.get("skipUsedOneTimeKitsFromKitList").getAsBoolean();
            }
        }
        return false; // Default to false
    }
    
    /**
     * Check if kit auto-equip is enabled
     */
    public boolean isKitAutoEquipEnabled() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("kits")) {
            JsonObject kits = config.getAsJsonObject("kits");
            if (kits.has("kitAutoEquip")) {
                return kits.get("kitAutoEquip").getAsBoolean();
            }
        }
        return false; // Default to false
    }
    
    /**
     * Check if createkit should use pastebin instead of direct file creation
     */
    public boolean isPastebinCreatekitEnabled() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("kits")) {
            JsonObject kits = config.getAsJsonObject("kits");
            if (kits.has("pastebinCreatekit")) {
                return kits.get("pastebinCreatekit").getAsBoolean();
            }
        }
        return false; // Default to false
    }
    
    /**
     * Check if NBT serialization should be used in createkit
     */
    public boolean isNbtSerializationInCreatekitEnabled() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("kits")) {
            JsonObject kits = config.getAsJsonObject("kits");
            if (kits.has("useNbtSerializationInCreatekit")) {
                return kits.get("useNbtSerializationInCreatekit").getAsBoolean();
            }
        }
        return false; // Default to false
    }
    
    /**
     * Check if unsafe enchantments are allowed in kits
     */
    public boolean areUnsafeEnchantsAllowedInKits() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("kits")) {
            JsonObject kits = config.getAsJsonObject("kits");
            if (kits.has("unsafeEnchantments")) {
                return kits.get("unsafeEnchantments").getAsBoolean();
            }
        }
        return false; // Default to false
    }
    
    /**
     * Get maximum kits per player
     */
    public int getMaxKitsPerPlayer() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("kits")) {
            JsonObject kits = config.getAsJsonObject("kits");
            if (kits.has("maxKitsPerPlayer")) {
                return kits.get("maxKitsPerPlayer").getAsInt();
            }
        }
        return 10; // Default to 10
    }
    
    /**
     * Get default kit cooldown in seconds
     */
    public long getDefaultKitCooldown() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("kits")) {
            JsonObject kits = config.getAsJsonObject("kits");
            if (kits.has("defaultCooldown")) {
                return kits.get("defaultCooldown").getAsLong();
            }
        }
        return 86400; // Default to 24 hours
    }
    
    /**
     * Check if kit override is allowed for privileged players
     */
    public boolean isKitOverrideAllowed() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("kits")) {
            JsonObject kits = config.getAsJsonObject("kits");
            if (kits.has("allowKitOverride")) {
                return kits.get("allowKitOverride").getAsBoolean();
            }
        }
        return true; // Default to true
    }
    
    /**
     * Check if kit preview is enabled
     */
    public boolean isKitPreviewEnabled() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("kits")) {
            JsonObject kits = config.getAsJsonObject("kits");
            if (kits.has("enableKitPreview")) {
                return kits.get("enableKitPreview").getAsBoolean();
            }
        }
        return true; // Default to true
    }
    
    /**
     * Check if kit usage should be broadcast
     */
    public boolean shouldBroadcastKitUsage() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("kits")) {
            JsonObject kits = config.getAsJsonObject("kits");
            if (kits.has("broadcastKitUsage")) {
                return kits.get("broadcastKitUsage").getAsBoolean();
            }
        }
        return false; // Default to false
    }
    
    /**
     * Check if kit usage should be logged
     */
    public boolean shouldLogKitUsage() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("kits")) {
            JsonObject kits = config.getAsJsonObject("kits");
            if (kits.has("logKitUsage")) {
                return kits.get("logKitUsage").getAsBoolean();
            }
        }
        return true; // Default to true
    }
    
    /**
     * Get cost for a specific kit command
     */
    public int getKitCommandCost(String command) {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("kits")) {
            JsonObject kits = config.getAsJsonObject("kits");
            if (kits.has("commandCosts")) {
                JsonObject costs = kits.getAsJsonObject("commandCosts");
                if (costs.has(command)) {
                    return costs.get(command).getAsInt();
                }
            }
        }
        return 0; // Default to free
    }
    
    /**
     * Check if new player kit is enabled
     */
    public boolean isNewPlayerKitEnabled() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("kits")) {
            JsonObject kits = config.getAsJsonObject("kits");
            if (kits.has("newPlayerKit")) {
                JsonObject newPlayerKit = kits.getAsJsonObject("newPlayerKit");
                if (newPlayerKit.has("enabled")) {
                    return newPlayerKit.get("enabled").getAsBoolean();
                }
            }
        }
        return false; // Default to false
    }
    
    /**
     * Get new player kit name
     */
    public String getNewPlayerKitName() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("kits")) {
            JsonObject kits = config.getAsJsonObject("kits");
            if (kits.has("newPlayerKit")) {
                JsonObject newPlayerKit = kits.getAsJsonObject("newPlayerKit");
                if (newPlayerKit.has("kitName")) {
                    String kitName = newPlayerKit.get("kitName").getAsString();
                    return kitName.isEmpty() ? null : kitName;
                }
            }
        }
        return null; // Default to null (disabled)
    }
    
    // ===============================
    // TELEPORTATION CONFIGURATION METHODS
    // ===============================
    
    /**
     * Check if teleportation system is enabled
     */
    public boolean isTeleportationEnabled() {
        return getBooleanConfig("modules", "teleportationEnabled", true);
    }
    
    // --- Home Settings ---
    

    
    /**
     * Get maximum number of homes per player
     */
    public int getMaxHomes() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("teleportation")) {
            JsonObject teleportation = config.getAsJsonObject("teleportation");
            if (teleportation.has("homeSettings")) {
                JsonObject homeSettings = teleportation.getAsJsonObject("homeSettings");
                if (homeSettings.has("maxHomes")) {
                    return homeSettings.get("maxHomes").getAsInt();
                }
            }
        }
        return 5; // Default to 5
    }
    
    /**
     * Check if cross-dimension homes are allowed
     */
    public boolean areCrossDimensionHomesAllowed() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("teleportation")) {
            JsonObject teleportation = config.getAsJsonObject("teleportation");
            if (teleportation.has("homeSettings")) {
                JsonObject homeSettings = teleportation.getAsJsonObject("homeSettings");
                if (homeSettings.has("allowCrossDimensionHomes")) {
                    return homeSettings.get("allowCrossDimensionHomes").getAsBoolean();
                }
            }
        }
        return true; // Default to true
    }
    
    /**
     * Get home set cooldown in seconds
     */
    public int getHomeSetCooldown() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("teleportation")) {
            JsonObject teleportation = config.getAsJsonObject("teleportation");
            if (teleportation.has("homeSettings")) {
                JsonObject homeSettings = teleportation.getAsJsonObject("homeSettings");
                if (homeSettings.has("homeSetCooldown")) {
                    return homeSettings.get("homeSetCooldown").getAsInt();
                }
            }
        }
        return 30; // Default to 30 seconds
    }
    
    /**
     * Get home teleport cooldown in seconds
     */
    public int getHomeTeleportCooldown() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("teleportation")) {
            JsonObject teleportation = config.getAsJsonObject("teleportation");
            if (teleportation.has("homeSettings")) {
                JsonObject homeSettings = teleportation.getAsJsonObject("homeSettings");
                if (homeSettings.has("homeTeleportCooldown")) {
                    return homeSettings.get("homeTeleportCooldown").getAsInt();
                }
            }
        }
        return 5; // Default to 5 seconds
    }
    
    /**
     * Get home delete cooldown in seconds
     */
    public int getHomeDeleteCooldown() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("teleportation")) {
            JsonObject teleportation = config.getAsJsonObject("teleportation");
            if (teleportation.has("homeSettings")) {
                JsonObject homeSettings = teleportation.getAsJsonObject("homeSettings");
                if (homeSettings.has("homeDeleteCooldown")) {
                    return homeSettings.get("homeDeleteCooldown").getAsInt();
                }
            }
        }
        return 10; // Default to 10 seconds
    }
    
    /**
     * Check if confirmation is required for home deletion
     */
    public boolean isHomeDeleteConfirmationRequired() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("teleportation")) {
            JsonObject teleportation = config.getAsJsonObject("teleportation");
            if (teleportation.has("homeSettings")) {
                JsonObject homeSettings = teleportation.getAsJsonObject("homeSettings");
                if (homeSettings.has("requireConfirmationForDelete")) {
                    return homeSettings.get("requireConfirmationForDelete").getAsBoolean();
                }
            }
        }
        return true; // Default to true
    }
    
    /**
     * Check if home override is allowed
     */
    public boolean isHomeOverrideAllowed() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("teleportation")) {
            JsonObject teleportation = config.getAsJsonObject("teleportation");
            if (teleportation.has("homeSettings")) {
                JsonObject homeSettings = teleportation.getAsJsonObject("homeSettings");
                if (homeSettings.has("allowHomeOverride")) {
                    return homeSettings.get("allowHomeOverride").getAsBoolean();
                }
            }
        }
        return false; // Default to false
    }
    
    /**
     * Check if home teleport safety checks are enabled
     */
    public boolean isHomeTeleportSafetyEnabled() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("teleportation")) {
            JsonObject teleportation = config.getAsJsonObject("teleportation");
            if (teleportation.has("homeSettings")) {
                JsonObject homeSettings = teleportation.getAsJsonObject("homeSettings");
                if (homeSettings.has("enableHomeTeleportSafety")) {
                    return homeSettings.get("enableHomeTeleportSafety").getAsBoolean();
                }
            }
        }
        return true; // Default to true
    }
    
    /**
     * Check if home actions should be logged
     */
    public boolean shouldLogHomeActions() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("teleportation")) {
            JsonObject teleportation = config.getAsJsonObject("teleportation");
            if (teleportation.has("homeSettings")) {
                JsonObject homeSettings = teleportation.getAsJsonObject("homeSettings");
                if (homeSettings.has("logHomeActions")) {
                    return homeSettings.get("logHomeActions").getAsBoolean();
                }
            }
        }
        return true; // Default to true
    }
    
    // --- Warp Settings ---
    

    
    /**
     * Check if player warps are allowed
     */
    public boolean arePlayerWarpsAllowed() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("teleportation")) {
            JsonObject teleportation = config.getAsJsonObject("teleportation");
            if (teleportation.has("warpSettings")) {
                JsonObject warpSettings = teleportation.getAsJsonObject("warpSettings");
                if (warpSettings.has("allowPlayerWarps")) {
                    return warpSettings.get("allowPlayerWarps").getAsBoolean();
                }
            }
        }
        return true; // Default to true
    }
    
    /**
     * Get maximum player warps
     */
    public int getMaxPlayerWarps() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("teleportation")) {
            JsonObject teleportation = config.getAsJsonObject("teleportation");
            if (teleportation.has("warpSettings")) {
                JsonObject warpSettings = teleportation.getAsJsonObject("warpSettings");
                if (warpSettings.has("maxPlayerWarps")) {
                    return warpSettings.get("maxPlayerWarps").getAsInt();
                }
            }
        }
        return 3; // Default to 3
    }
    
    /**
     * Get warp cooldown in seconds
     */
    public int getWarpCooldown() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("teleportation")) {
            JsonObject teleportation = config.getAsJsonObject("teleportation");
            if (teleportation.has("warpSettings")) {
                JsonObject warpSettings = teleportation.getAsJsonObject("warpSettings");
                if (warpSettings.has("warpCooldown")) {
                    return warpSettings.get("warpCooldown").getAsInt();
                }
            }
        }
        return 10; // Default to 10 seconds
    }
    
    /**
     * Get warp set cooldown in seconds
     */
    public int getWarpSetCooldown() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("teleportation")) {
            JsonObject teleportation = config.getAsJsonObject("teleportation");
            if (teleportation.has("warpSettings")) {
                JsonObject warpSettings = teleportation.getAsJsonObject("warpSettings");
                if (warpSettings.has("warpSetCooldown")) {
                    return warpSettings.get("warpSetCooldown").getAsInt();
                }
            }
        }
        return 60; // Default to 60 seconds
    }
    
    /**
     * Check if cross-dimension warps are allowed
     */
    public boolean areCrossDimensionWarpsAllowed() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("teleportation")) {
            JsonObject teleportation = config.getAsJsonObject("teleportation");
            if (teleportation.has("warpSettings")) {
                JsonObject warpSettings = teleportation.getAsJsonObject("warpSettings");
                if (warpSettings.has("allowCrossDimensionWarps")) {
                    return warpSettings.get("allowCrossDimensionWarps").getAsBoolean();
                }
            }
        }
        return true; // Default to true
    }
    
    /**
     * Check if warp safety checks are enabled
     */
    public boolean isWarpSafetyEnabled() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("teleportation")) {
            JsonObject teleportation = config.getAsJsonObject("teleportation");
            if (teleportation.has("warpSettings")) {
                JsonObject warpSettings = teleportation.getAsJsonObject("warpSettings");
                if (warpSettings.has("enableWarpSafety")) {
                    return warpSettings.get("enableWarpSafety").getAsBoolean();
                }
            }
        }
        return true; // Default to true
    }
    
    /**
     * Check if warp creation should be broadcast
     */
    public boolean shouldBroadcastWarpCreation() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("teleportation")) {
            JsonObject teleportation = config.getAsJsonObject("teleportation");
            if (teleportation.has("warpSettings")) {
                JsonObject warpSettings = teleportation.getAsJsonObject("warpSettings");
                if (warpSettings.has("broadcastWarpCreation")) {
                    return warpSettings.get("broadcastWarpCreation").getAsBoolean();
                }
            }
        }
        return false; // Default to false
    }
    
    /**
     * Check if warp actions should be logged
     */
    public boolean shouldLogWarpActions() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("teleportation")) {
            JsonObject teleportation = config.getAsJsonObject("teleportation");
            if (teleportation.has("warpSettings")) {
                JsonObject warpSettings = teleportation.getAsJsonObject("warpSettings");
                if (warpSettings.has("logWarpActions")) {
                    return warpSettings.get("logWarpActions").getAsBoolean();
                }
            }
        }
        return true; // Default to true
    }
    
    // --- Spawn Settings ---
    

    
    /**
     * Check if players should spawn on join
     */
    public boolean shouldSpawnOnJoin() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("teleportation")) {
            JsonObject teleportation = config.getAsJsonObject("teleportation");
            if (teleportation.has("spawnSettings")) {
                JsonObject spawnSettings = teleportation.getAsJsonObject("spawnSettings");
                if (spawnSettings.has("spawnOnJoin")) {
                    return spawnSettings.get("spawnOnJoin").getAsBoolean();
                }
            }
        }
        return true; // Default to true
    }
    
    /**
     * Check if players should spawn on death
     */
    public boolean shouldSpawnOnDeath() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("teleportation")) {
            JsonObject teleportation = config.getAsJsonObject("teleportation");
            if (teleportation.has("spawnSettings")) {
                JsonObject spawnSettings = teleportation.getAsJsonObject("spawnSettings");
                if (spawnSettings.has("spawnOnDeath")) {
                    return spawnSettings.get("spawnOnDeath").getAsBoolean();
                }
            }
        }
        return false; // Default to false
    }
    
    /**
     * Get spawn cooldown in seconds
     */
    public int getSpawnCooldown() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("teleportation")) {
            JsonObject teleportation = config.getAsJsonObject("teleportation");
            if (teleportation.has("spawnSettings")) {
                JsonObject spawnSettings = teleportation.getAsJsonObject("spawnSettings");
                if (spawnSettings.has("spawnCooldown")) {
                    return spawnSettings.get("spawnCooldown").getAsInt();
                }
            }
        }
        return 5; // Default to 5 seconds
    }
    
    /**
     * Check if spawn can be set by admins
     */
    public boolean isSpawnSetAllowed() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("teleportation")) {
            JsonObject teleportation = config.getAsJsonObject("teleportation");
            if (teleportation.has("spawnSettings")) {
                JsonObject spawnSettings = teleportation.getAsJsonObject("spawnSettings");
                if (spawnSettings.has("allowSpawnSet")) {
                    return spawnSettings.get("allowSpawnSet").getAsBoolean();
                }
            }
        }
        return true; // Default to true
    }
    
    /**
     * Check if spawn safety checks are enabled
     */
    public boolean isSpawnSafetyEnabled() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("teleportation")) {
            JsonObject teleportation = config.getAsJsonObject("teleportation");
            if (teleportation.has("spawnSettings")) {
                JsonObject spawnSettings = teleportation.getAsJsonObject("spawnSettings");
                if (spawnSettings.has("enableSpawnSafety")) {
                    return spawnSettings.get("enableSpawnSafety").getAsBoolean();
                }
            }
        }
        return true; // Default to true
    }
    
    /**
     * Check if spawn actions should be logged
     */
    public boolean shouldLogSpawnActions() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("teleportation")) {
            JsonObject teleportation = config.getAsJsonObject("teleportation");
            if (teleportation.has("spawnSettings")) {
                JsonObject spawnSettings = teleportation.getAsJsonObject("spawnSettings");
                if (spawnSettings.has("logSpawnActions")) {
                    return spawnSettings.get("logSpawnActions").getAsBoolean();
                }
            }
        }
        return true; // Default to true
    }
    
    // --- Teleport Request Settings ---
    

    
    /**
     * Get teleport request timeout in seconds
     */
    public int getTeleportRequestTimeout() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("teleportation")) {
            JsonObject teleportation = config.getAsJsonObject("teleportation");
            if (teleportation.has("teleportRequestSettings")) {
                JsonObject requestSettings = teleportation.getAsJsonObject("teleportRequestSettings");
                if (requestSettings.has("requestTimeout")) {
                    return requestSettings.get("requestTimeout").getAsInt();
                }
            }
        }
        return 60; // Default to 60 seconds
    }
    
    /**
     * Get maximum pending requests per player
     */
    public int getMaxPendingRequests() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("teleportation")) {
            JsonObject teleportation = config.getAsJsonObject("teleportation");
            if (teleportation.has("teleportRequestSettings")) {
                JsonObject requestSettings = teleportation.getAsJsonObject("teleportRequestSettings");
                if (requestSettings.has("maxPendingRequests")) {
                    return requestSettings.get("maxPendingRequests").getAsInt();
                }
            }
        }
        return 5; // Default to 5
    }
    
    /**
     * Get cooldown between teleport requests in seconds
     */
    public int getTeleportRequestCooldown() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("teleportation")) {
            JsonObject teleportation = config.getAsJsonObject("teleportation");
            if (teleportation.has("teleportRequestSettings")) {
                JsonObject requestSettings = teleportation.getAsJsonObject("teleportRequestSettings");
                if (requestSettings.has("cooldownBetweenRequests")) {
                    return requestSettings.get("cooldownBetweenRequests").getAsInt();
                }
            }
        }
        return 10; // Default to 10 seconds
    }
    
    /**
     * Check if multiple requests to same player are allowed
     */
    public boolean areMultipleTeleportRequestsAllowed() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("teleportation")) {
            JsonObject teleportation = config.getAsJsonObject("teleportation");
            if (teleportation.has("teleportRequestSettings")) {
                JsonObject requestSettings = teleportation.getAsJsonObject("teleportRequestSettings");
                if (requestSettings.has("allowMultipleRequests")) {
                    return requestSettings.get("allowMultipleRequests").getAsBoolean();
                }
            }
        }
        return false; // Default to false
    }
    
    /**
     * Check if teleport request notifications are enabled
     */
    public boolean areTeleportRequestNotificationsEnabled() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("teleportation")) {
            JsonObject teleportation = config.getAsJsonObject("teleportation");
            if (teleportation.has("teleportRequestSettings")) {
                JsonObject requestSettings = teleportation.getAsJsonObject("teleportRequestSettings");
                if (requestSettings.has("enableRequestNotifications")) {
                    return requestSettings.get("enableRequestNotifications").getAsBoolean();
                }
            }
        }
        return true; // Default to true
    }
    
    /**
     * Check if auto-accept from friends is enabled
     */
    public boolean isAutoAcceptFromFriendsEnabled() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("teleportation")) {
            JsonObject teleportation = config.getAsJsonObject("teleportation");
            if (teleportation.has("teleportRequestSettings")) {
                JsonObject requestSettings = teleportation.getAsJsonObject("teleportRequestSettings");
                if (requestSettings.has("autoAcceptFromFriends")) {
                    return requestSettings.get("autoAcceptFromFriends").getAsBoolean();
                }
            }
        }
        return false; // Default to false
    }
    
    /**
     * Check if teleport safety checks are enabled for requests
     */
    public boolean isTeleportRequestSafetyEnabled() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("teleportation")) {
            JsonObject teleportation = config.getAsJsonObject("teleportation");
            if (teleportation.has("teleportRequestSettings")) {
                JsonObject requestSettings = teleportation.getAsJsonObject("teleportRequestSettings");
                if (requestSettings.has("enableTeleportSafety")) {
                    return requestSettings.get("enableTeleportSafety").getAsBoolean();
                }
            }
        }
        return true; // Default to true
    }
    
    /**
     * Check if teleport request actions should be logged
     */
    public boolean shouldLogTeleportRequests() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("teleportation")) {
            JsonObject teleportation = config.getAsJsonObject("teleportation");
            if (teleportation.has("teleportRequestSettings")) {
                JsonObject requestSettings = teleportation.getAsJsonObject("teleportRequestSettings");
                if (requestSettings.has("logTeleportRequests")) {
                    return requestSettings.get("logTeleportRequests").getAsBoolean();
                }
            }
        }
        return true; // Default to true
    }
    
    // --- General Teleportation Settings ---
    
    /**
     * Get teleport delay in seconds
     */
    public int getTeleportDelay() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("teleportation")) {
            JsonObject teleportation = config.getAsJsonObject("teleportation");
            if (teleportation.has("generalSettings")) {
                JsonObject generalSettings = teleportation.getAsJsonObject("generalSettings");
                if (generalSettings.has("teleportDelay")) {
                    return generalSettings.get("teleportDelay").getAsInt();
                }
            }
        }
        return 3; // Default to 3 seconds
    }
    
    /**
     * Check if teleportation should be canceled on movement
     */
    public boolean shouldCancelTeleportOnMovement() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("teleportation")) {
            JsonObject teleportation = config.getAsJsonObject("teleportation");
            if (teleportation.has("generalSettings")) {
                JsonObject generalSettings = teleportation.getAsJsonObject("generalSettings");
                if (generalSettings.has("cancelOnMovement")) {
                    return generalSettings.get("cancelOnMovement").getAsBoolean();
                }
            }
        }
        return true; // Default to true
    }
    
    /**
     * Check if teleportation should be canceled on damage
     */
    public boolean shouldCancelTeleportOnDamage() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("teleportation")) {
            JsonObject teleportation = config.getAsJsonObject("teleportation");
            if (teleportation.has("generalSettings")) {
                JsonObject generalSettings = teleportation.getAsJsonObject("generalSettings");
                if (generalSettings.has("cancelOnDamage")) {
                    return generalSettings.get("cancelOnDamage").getAsBoolean();
                }
            }
        }
        return false; // Default to false
    }
    
    /**
     * Check if teleport warmup is enabled
     */
    public boolean isTeleportWarmupEnabled() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("teleportation")) {
            JsonObject teleportation = config.getAsJsonObject("teleportation");
            if (teleportation.has("generalSettings")) {
                JsonObject generalSettings = teleportation.getAsJsonObject("generalSettings");
                if (generalSettings.has("enableTeleportWarmup")) {
                    return generalSettings.get("enableTeleportWarmup").getAsBoolean();
                }
            }
        }
        return true; // Default to true
    }
    
    /**
     * Check if teleportation is allowed in combat
     */
    public boolean isTeleportInCombatAllowed() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("teleportation")) {
            JsonObject teleportation = config.getAsJsonObject("teleportation");
            if (teleportation.has("generalSettings")) {
                JsonObject generalSettings = teleportation.getAsJsonObject("generalSettings");
                if (generalSettings.has("allowTeleportInCombat")) {
                    return generalSettings.get("allowTeleportInCombat").getAsBoolean();
                }
            }
        }
        return false; // Default to false
    }
    
    /**
     * Get maximum teleportation distance in blocks
     */
    public int getMaxTeleportDistance() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("teleportation")) {
            JsonObject teleportation = config.getAsJsonObject("teleportation");
            if (teleportation.has("generalSettings")) {
                JsonObject generalSettings = teleportation.getAsJsonObject("generalSettings");
                if (generalSettings.has("maxTeleportDistance")) {
                    return generalSettings.get("maxTeleportDistance").getAsInt();
                }
            }
        }
        return -1; // Default to unlimited
    }
    
    /**
     * Check if particle effects are enabled for teleportation
     */
    public boolean areTeleportParticleEffectsEnabled() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("teleportation")) {
            JsonObject teleportation = config.getAsJsonObject("teleportation");
            if (teleportation.has("generalSettings")) {
                JsonObject generalSettings = teleportation.getAsJsonObject("generalSettings");
                if (generalSettings.has("enableParticleEffects")) {
                    return generalSettings.get("enableParticleEffects").getAsBoolean();
                }
            }
        }
        return true; // Default to true
    }
    
    /**
     * Check if sound effects are enabled for teleportation
     */
    public boolean areTeleportSoundEffectsEnabled() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("teleportation")) {
            JsonObject teleportation = config.getAsJsonObject("teleportation");
            if (teleportation.has("generalSettings")) {
                JsonObject generalSettings = teleportation.getAsJsonObject("generalSettings");
                if (generalSettings.has("enableSoundEffects")) {
                    return generalSettings.get("enableSoundEffects").getAsBoolean();
                }
            }
        }
        return true; // Default to true
    }
    
    /**
     * Get list of protected areas where teleportation is restricted
     */
    public List<String> getTeleportProtectedAreas() {
        JsonObject config = getConfig(MAIN_CONFIG);
        List<String> areas = new ArrayList<>();
        if (config.has("teleportation")) {
            JsonObject teleportation = config.getAsJsonObject("teleportation");
            if (teleportation.has("generalSettings")) {
                JsonObject generalSettings = teleportation.getAsJsonObject("generalSettings");
                if (generalSettings.has("protectedAreas")) {
                    JsonArray protectedAreas = generalSettings.getAsJsonArray("protectedAreas");
                    for (JsonElement area : protectedAreas) {
                        areas.add(area.getAsString());
                    }
                }
            }
        }
        return areas;
    }
    
    /**
     * Get cost for a specific teleportation command
     */
    public int getTeleportCommandCost(String command) {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("teleportation")) {
            JsonObject teleportation = config.getAsJsonObject("teleportation");
            if (teleportation.has("commandCosts")) {
                JsonObject costs = teleportation.getAsJsonObject("commandCosts");
                if (costs.has(command)) {
                    return costs.get(command).getAsInt();
                }
            }
        }
        return 0; // Default to free
    }
    
    /**
     * Helper method to get boolean config values
     */
    private boolean getBooleanConfig(String section, String key, boolean defaultValue) {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has(section)) {
            JsonObject sectionObj = config.getAsJsonObject(section);
            if (sectionObj.has(key)) {
                return sectionObj.get(key).getAsBoolean();
            }
        }
        return defaultValue;
    }
    
}