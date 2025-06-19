package com.zerog.neoessentials.config;

import java.util.HashMap;
import java.util.Map;

/**
 * Compatibility layer that adapts our TOML configs to the old config structure.
 * This allows existing code to continue working with the new config system.
 * 
 * This class uses lazy loading for config values - they are fetched directly
 * from the TOML configs when needed rather than being initialized at startup.
 */
public class CompatNeoEssentialsConfig {
    // Command settings - cache for performance
    private final Map<String, Boolean> commandsEnabled = new HashMap<>();
    
    // Permission settings - cache for performance
    private final Map<String, Boolean> defaultPermissions = new HashMap<>();
    
    /**
     * Constructor - doesn't load any config values
     */
    public CompatNeoEssentialsConfig() {
        // Empty constructor - no initialization of config values
    }
    
    /**
     * Gets whether debug mode is enabled
     * @return True if debug mode is enabled
     */
    public boolean isDebug() {
        try {
            return GeneralConfig.DEBUG_MODE.get();
        } catch (IllegalStateException e) {
            // Config not loaded yet, return default
            return false;
        }
    }
    
    /**
     * Gets the default language code
     * @return The language code (e.g. "en_us")
     */
    public String getDefaultLanguage() {
        return "en_us";
    }
    
    /**
     * Gets whether economy is enabled
     * @return True if economy is enabled
     */
    public boolean isEconomyEnabled() {
        try {
            return GeneralConfig.ENABLE_ECONOMY.get();
        } catch (IllegalStateException e) {
            // Config not loaded yet, return default
            return true;
        }
    }
    
    /**
     * Gets the singular name of the currency
     * @return The currency name (e.g. "Dollar")
     */
    public String getCurrencyNameSingular() {
        if (!isEconomyEnabled()) return "Dollar";
        
        try {
            return EconomyConfig.CURRENCY_NAME_SINGULAR.get();
        } catch (IllegalStateException e) {
            return "Dollar";
        }
    }
    
    /**
     * Gets the plural name of the currency
     * @return The currency name (e.g. "Dollars")
     */
    public String getCurrencyNamePlural() {
        if (!isEconomyEnabled()) return "Dollars";
        
        try {
            return EconomyConfig.CURRENCY_NAME_PLURAL.get();
        } catch (IllegalStateException e) {
            return "Dollars";
        }
    }
    
    /**
     * Gets the currency symbol
     * @return The currency symbol (e.g. "$")
     */
    public String getCurrencySymbol() {
        if (!isEconomyEnabled()) return "$";
        
        try {
            return EconomyConfig.CURRENCY_SYMBOL.get();
        } catch (IllegalStateException e) {
            return "$";
        }
    }
    
    /**
     * Gets the starting balance for new players
     * @return The starting balance
     */
    public double getStartingBalance() {
        if (!isEconomyEnabled()) return 100.0;
        
        try {
            return EconomyConfig.STARTING_BALANCE.get();
        } catch (IllegalStateException e) {
            return 100.0;
        }
    }
    
    /**
     * Gets whether teleportation is enabled
     * @return True if teleportation is enabled
     */
    public boolean isTeleportEnabled() {
        try {
            return GeneralConfig.ENABLE_TELEPORTATION.get();
        } catch (IllegalStateException e) {
            return true;
        }
    }
    
    /**
     * Gets the teleport cooldown in seconds
     * @return The cooldown in seconds
     */
    public int getTeleportCooldown() {
        if (!isTeleportEnabled()) return 30;
        
        try {
            return HomeConfig.COOLDOWN_SECONDS.get();
        } catch (IllegalStateException e) {
            return 30;
        }
    }
    
    /**
     * Gets the teleport warmup in seconds
     * @return The warmup in seconds
     */
    public int getTeleportWarmup() {
        if (!isTeleportEnabled()) return 3;
        
        try {
            return HomeConfig.WARMUP_SECONDS.get();
        } catch (IllegalStateException e) {
            return 3;
        }
    }
    
    /**
     * Gets the maximum number of homes a player can have
     * @return The max homes
     */
    public int getMaxHomes() {
        if (!isTeleportEnabled()) return 3;
        
        try {
            return HomeConfig.DEFAULT_MAX_HOMES.get();
        } catch (IllegalStateException e) {
            return 3;
        }
    }
    
    /**
     * Gets whether warps are enabled
     * @return True if warps are enabled
     */
    public boolean isWarpsEnabled() {
        try {
            return GeneralConfig.ENABLE_WARPS.get();
        } catch (IllegalStateException e) {
            return true;
        }
    }
    
    /**
     * Gets whether a command is enabled
     * @param command The command name
     * @return True if the command is enabled
     */
    public boolean isCommandEnabled(String command) {
        if (commandsEnabled.containsKey(command)) {
            return commandsEnabled.get(command);
        }
        
        try {
            boolean enabled = true;
            switch (command.toLowerCase()) {
                case "home":
                    enabled = GeneralConfig.ENABLE_HOMES.get();
                    break;
                case "warp":
                    enabled = GeneralConfig.ENABLE_WARPS.get();
                    break;
                case "tpa":
                case "back":
                    enabled = GeneralConfig.ENABLE_TELEPORTATION.get();
                    break;
                case "kit":
                    enabled = GeneralConfig.ENABLE_KITS.get();
                    break;
                default:
                    enabled = true;
            }
            
            // Cache the result
            commandsEnabled.put(command, enabled);
            return enabled;
        } catch (IllegalStateException e) {
            // Default to enabled if config not loaded
            return true;
        }
    }
    
    /**
     * Gets the map of default permissions
     * @return Map of permission names to boolean values
     */
    public Map<String, Boolean> defaultPermissions() {
        return defaultPermissions;
    }
}
