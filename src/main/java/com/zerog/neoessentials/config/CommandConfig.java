
package com.zerog.neoessentials.config;

/**
 * Command configuration access.
 * Now delegates to the centralized ConfigManager for consistency.
 * 
 * @deprecated Use ConfigManager.getInstance() directly for new code
 */
@Deprecated
public class CommandConfig {

    public static boolean isCommandEnabled(String command) {
        return ConfigManager.getInstance().isCommandEnabled(command);
    }

    public static void load() {
        // Delegate to ConfigManager - it handles loading automatically
        ConfigManager.getInstance().loadAll();
    }
}
