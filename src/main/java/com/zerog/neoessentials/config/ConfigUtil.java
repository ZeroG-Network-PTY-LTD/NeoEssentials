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
        
        // Apply our custom validator for tablist headers and footers
        // This ensures that TOML arrays are compared properly
        TablistTomlConfig.patchConfigComparison();
        
        com.zerog.neoessentials.NeoEssentials.LOGGER.info("Config comparison patch applied");
    }
    
    /**
     * Compares two lists for equality, accounting for TOML array parsing quirks
     * This is especially useful for checking if config values need correction
     * 
     * @param <T> The type of elements in the lists
     * @param configList The list from the config file
     * @param defaultList The default list from the code
     * @return True if the lists are equal in content, false otherwise
     */
    public static <T> boolean areListsEqual(java.util.List<T> configList, java.util.List<T> defaultList) {
        if (configList == null || defaultList == null) {
            return configList == defaultList;
        }
        
        if (configList.size() != defaultList.size()) {
            com.zerog.neoessentials.NeoEssentials.LOGGER.debug("Lists have different sizes: {} vs {}", configList.size(), defaultList.size());
            return false;
        }
        
        // Use a more lenient comparison for TOML arrays
        for (int i = 0; i < configList.size(); i++) {
            T configItem = configList.get(i);
            T defaultItem = defaultList.get(i);
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
            configStr = configStr.replaceAll("^\"|\"$", "");
            defaultStr = defaultStr.replaceAll("^\"|\"$", "");
            
            if (!configStr.equals(defaultStr)) {
                com.zerog.neoessentials.NeoEssentials.LOGGER.debug("List items differ at index {}: '{}' vs '{}'", i, configStr, defaultStr);
                return false;
            }
        }
        
        return true;
    }
}
