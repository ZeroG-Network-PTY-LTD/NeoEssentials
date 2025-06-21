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
     */
    public static <T> T getConfigSafe(ModConfigSpec.ConfigValue<T> configValue, T defaultValue) {
        try {
            return configValue.get();
        } catch (IllegalStateException e) {
            // Config is not loaded yet
            return defaultValue;
        }
    }
}
