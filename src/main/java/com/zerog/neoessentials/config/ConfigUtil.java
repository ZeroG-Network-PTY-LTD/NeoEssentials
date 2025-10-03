package com.zerog.neoessentials.config;

/**
 * Unified config access for command enabling/disabling.
 * Now delegates to the centralized ConfigManager for consistency.
 * 
 * @deprecated Use ConfigManager.getInstance() directly for new code
 */
@Deprecated
public class ConfigUtil {
    
    /**
     * Check if a command is enabled in the config.
     * Defaults to true if config is missing or command not specified.
     */
    public static boolean isCommandEnabled(String command) {
        return ConfigManager.getInstance().isCommandEnabled(command);
    }
}