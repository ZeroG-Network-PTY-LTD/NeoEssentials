package com.zerog.neoessentials.config;

import com.google.gson.JsonObject;
import java.io.File;
import java.util.List;

/**
 * Global configuration access.
 * Now delegates to the centralized ConfigManager for consistency.
 * 
 * @deprecated Use ConfigManager.getInstance() directly for new code
 */
@Deprecated
public class GlobalConfig {
    
    public static void load(File configFile) {
        // Delegate to ConfigManager - it handles loading automatically
        ConfigManager.getInstance().loadAll();
    }

    public static boolean isEconomyEnabled() {
        return ConfigManager.getInstance().isEconomyEnabled();
    }

    public static JsonObject getItemsConfig() {
        JsonObject config = ConfigManager.getInstance().getConfig(ConfigManager.MAIN_CONFIG);
        return config.has("items") ? config.getAsJsonObject("items") : new JsonObject();
    }

    // Helper methods for common item config options
    public static boolean isPermissionBasedItemSpawn() {
        JsonObject items = getItemsConfig();
        return items.has("permission-based-item-spawn") && items.get("permission-based-item-spawn").getAsBoolean();
    }
    
    public static boolean isDropItemsIfFull() {
        JsonObject items = getItemsConfig();
        return items.has("drop-items-if-full") && items.get("drop-items-if-full").getAsBoolean();
    }
    
    public static int getOversizedStackSize() {
        JsonObject items = getItemsConfig();
        return items.has("oversized-stacksize") ? items.get("oversized-stacksize").getAsInt() : 64;
    }
    
    public static int getDefaultStackSize() {
        JsonObject items = getItemsConfig();
        return items.has("default-stack-size") ? items.get("default-stack-size").getAsInt() : -1;
    }
    
    public static boolean isUnsafeEnchantmentsAllowed() {
        return ConfigManager.getInstance().isUnsafeEnchantsAllowed();
    }
    
    public static List<String> getItemSpawnBlacklist() {
        return ConfigManager.getInstance().getItemSpawnBlacklist();
    }
}