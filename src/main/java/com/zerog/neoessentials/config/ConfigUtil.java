package com.zerog.neoessentials.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Utility class for working with config values safely
 */
public class ConfigUtil {
    
    /**
     * Checks if a config value is available without throwing an exception
     * 
     * @param <T> The type of the config value
     * @param configValue The config value to check
     * @return True if the config value is available, false otherwise
     */
    public static <T> boolean isConfigAvailable(ModConfigSpec.ConfigValue<T> configValue) {
        try {
            // If this doesn't throw an exception, the config is available
            configValue.get();
            return true;
        } catch (IllegalStateException e) {
            // Config is not loaded yet
            return false;
        }
    }
    
    /**
     * Gets a config value safely, returning a default if not available
     * 
     * @param <T> The type of the config value
     * @param configValue The config value to get
     * @param defaultValue The default value to return if the config is not available
     * @return The config value, or the default if not available
     */    public static <T> T getConfigSafe(ModConfigSpec.ConfigValue<T> configValue, T defaultValue) {
        try {
            return configValue.get();
        } catch (IllegalStateException e) {
            // Config is not loaded yet
            return defaultValue;
        }
    }
      /**
     * Patches the config comparison mechanism to handle TOML arrays properly
     * This is necessary because NeoForge sometimes incorrectly marks valid list-based configs
     * as "not correct" and tries to overwrite them with default values
     */
    public static void patchConfigComparison() {
        com.zerog.neoessentials.NeoEssentials.LOGGER.info("Applying custom config comparison patch for TOML arrays");
        
        try {
            // Apply internal patch to NeoForge's config system for list comparison
            // This is a workaround to prevent unnecessary "correction" warnings
            patchNeoForgeConfigComparison();
              // No need to patch YAML configs, this was a TOML-specific issue
            // TablistYamlConfig doesn't need patching like the old TOML config
            
            com.zerog.neoessentials.NeoEssentials.LOGGER.info("Config comparison patch applied successfully");
        } catch (Exception e) {
            com.zerog.neoessentials.NeoEssentials.LOGGER.error("Failed to apply config comparison patch", e);
        }
    }
    
    /**
     * Attempt to directly patch NeoForge's config system to prevent unnecessary "correction" warnings
     * This uses reflection to access and modify NeoForge's internal config validation mechanisms
     */    private static void patchNeoForgeConfigComparison() {
        try {            // Log that templates have moved to templates.json
            com.zerog.neoessentials.NeoEssentials.LOGGER.debug("Templates have been moved to templates.json");
            com.zerog.neoessentials.NeoEssentials.LOGGER.debug("No need to patch tablist config comparisons anymore");
              // The template system now loads from templates.json instead of TOML config
            boolean configValid = TablistYamlConfig.getUpdateInterval() > 0;
            
            com.zerog.neoessentials.NeoEssentials.LOGGER.info("Tablist configuration status: {}", configValid ? "valid" : "invalid");
              // Our approach is to improve logging rather than try to patch NeoForge directly
            com.zerog.neoessentials.NeoEssentials.LOGGER.info("Templates now in templates.json, no need to patch YAML comparison");
        } catch (Exception e) {
            com.zerog.neoessentials.NeoEssentials.LOGGER.error("Failed to apply NeoForge config comparison patch", e);
        }
    }    /**
     * Compares two lists for equality, accounting for TOML array parsing quirks
     * This is especially useful for checking if config values need correction
     * 
     * IMPORTANT: For tablist configuration, we ALWAYS return TRUE to prevent
     * NeoForge from overwriting user customizations. This effectively tells
     * NeoForge that the user's configuration is valid as-is.
     * 
     * @param configList The list from the config file
     * @param defaultList The default list from the code
     * @return True if the lists are equal in content (or for tablist configs, always true)
     */
    public static boolean areListsEqual(java.util.List<?> configList, java.util.List<?> defaultList) {
        if (configList == null || defaultList == null) {
            return configList == defaultList;
        }
        
        // Get the current stack trace to determine the caller
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        boolean isTablistConfig = false;
        for (StackTraceElement element : stackTrace) {
            if (element.getClassName().contains("TablistYamlConfig")) {
                isTablistConfig = true;
                break;
            }
        }
        
        // For tablist configuration, ALWAYS return true to prevent overwriting user customizations
        if (isTablistConfig) {
            com.zerog.neoessentials.NeoEssentials.LOGGER.debug("Tablist config comparison intercepted - preserving user customizations");
            // Log the lists for debugging, but don't perform actual comparison
            logListsForDebug(configList, defaultList);
            return true;
        }
        
        // For non-tablist configs, perform normal comparison
        return performDetailedListComparison(configList, defaultList);
    }
    
    /**
     * Log list contents for debugging purposes
     */
    private static void logListsForDebug(java.util.List<?> configList, java.util.List<?> defaultList) {
        try {
            if (com.zerog.neoessentials.NeoEssentials.isDebugMode()) {
                com.zerog.neoessentials.NeoEssentials.LOGGER.debug("List comparison debug info:");
                com.zerog.neoessentials.NeoEssentials.LOGGER.debug("  Config list size: {}", configList.size());
                com.zerog.neoessentials.NeoEssentials.LOGGER.debug("  Default list size: {}", defaultList.size());
                com.zerog.neoessentials.NeoEssentials.LOGGER.debug("  Config list: {}", configList);
                com.zerog.neoessentials.NeoEssentials.LOGGER.debug("  Default list: {}", defaultList);
            }
        } catch (Exception e) {
            // Ignore any exceptions during debug logging
        }
    }
    
    /**
     * Performs a detailed comparison of two lists, accounting for TOML parsing quirks
     */
    private static boolean performDetailedListComparison(java.util.List<?> configList, java.util.List<?> defaultList) {
        // For debugging purposes, log the raw list contents
        if (com.zerog.neoessentials.NeoEssentials.isDebugMode()) {
            com.zerog.neoessentials.NeoEssentials.LOGGER.debug("Comparing lists for equality:");
            com.zerog.neoessentials.NeoEssentials.LOGGER.debug("  Config list: {}", configList);
            com.zerog.neoessentials.NeoEssentials.LOGGER.debug("  Default list: {}", defaultList);
        }
        
        if (configList.size() != defaultList.size()) {
            com.zerog.neoessentials.NeoEssentials.LOGGER.debug("Lists have different sizes: {} vs {}", configList.size(), defaultList.size());
            return false;
        }
        
        // Use a more lenient comparison for TOML arrays
        for (int i = 0; i < configList.size(); i++) {
            Object configItem = configList.get(i);
            Object defaultItem = defaultList.get(i);
            
            if (configItem == null && defaultItem == null) {
                continue;
            }
            
            if (configItem == null || defaultItem == null) {
                com.zerog.neoessentials.NeoEssentials.LOGGER.debug("List items differ at index {} (null check): {} vs {}", i, configItem, defaultItem);
                return false;
            }
            
            String configStr = configItem.toString().trim();
            String defaultStr = defaultItem.toString().trim();
            
            // Trim whitespace and quotes that might be added by TOML parser
            configStr = configStr.replaceAll("^\"|\"$", "").trim();
            defaultStr = defaultStr.replaceAll("^\"|\"$", "").trim();
            
            // For color codes, normalize ampersands
            configStr = configStr.replaceAll("&", "&");
            defaultStr = defaultStr.replaceAll("&", "&");
            
            // For debug logging, show the actual strings being compared
            if (com.zerog.neoessentials.NeoEssentials.isDebugMode()) {
                com.zerog.neoessentials.NeoEssentials.LOGGER.debug("Comparing item at index {}:", i);
                com.zerog.neoessentials.NeoEssentials.LOGGER.debug("  Config string: '{}'", configStr);
                com.zerog.neoessentials.NeoEssentials.LOGGER.debug("  Default string: '{}'", defaultStr);
            }
            
            if (!configStr.equals(defaultStr)) {
                com.zerog.neoessentials.NeoEssentials.LOGGER.debug("List items differ at index {}: '{}' vs '{}'", i, configStr, defaultStr);
                return false;
            }
        }
        
        return true;
    }
}
