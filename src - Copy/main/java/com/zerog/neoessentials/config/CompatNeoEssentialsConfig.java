package com.zerog.neoessentials.config;

import com.zerog.neoessentials.NeoEssentials;
import java.util.HashMap;
import java.util.Map;
import com.zerog.neoessentials.NeoEssentials;

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
    }    /**
     * Initializes default values from configs after configs are loaded.
     * This should only be called by ModConfigManager after all configs are loaded.
     */
    public void initialize() {
        NeoEssentials.LOGGER.info("Initializing compatibility config layer");
        
        // Initialize default permissions if needed
        // We don't need to pre-populate other values since we use lazy loading now
        try {
            // Pre-cache some frequently used command states using our safe utility
            if (ConfigUtil.isConfigAvailable(GeneralConfig.ENABLE_HOMES)) {
                commandsEnabled.put("home", GeneralConfig.ENABLE_HOMES.get());
                NeoEssentials.LOGGER.debug("Pre-cached home command state: " + commandsEnabled.get("home"));
            }
            
            if (ConfigUtil.isConfigAvailable(GeneralConfig.ENABLE_WARPS)) {
                commandsEnabled.put("warp", GeneralConfig.ENABLE_WARPS.get());
                NeoEssentials.LOGGER.debug("Pre-cached warp command state: " + commandsEnabled.get("warp"));
            }
            
            if (ConfigUtil.isConfigAvailable(GeneralConfig.ENABLE_TELEPORTATION)) {
                boolean teleportEnabled = GeneralConfig.ENABLE_TELEPORTATION.get();
                commandsEnabled.put("tpa", teleportEnabled);
                commandsEnabled.put("back", teleportEnabled);
                NeoEssentials.LOGGER.debug("Pre-cached teleport commands state: " + teleportEnabled);
            }
              if (ConfigUtil.isConfigAvailable(GeneralConfig.ENABLE_KITS)) {
                commandsEnabled.put("kit", GeneralConfig.ENABLE_KITS.get());
                NeoEssentials.LOGGER.debug("Pre-cached kit command state: " + commandsEnabled.get("kit"));
            }
        } catch (Exception e) {
            NeoEssentials.LOGGER.warn("Error during compatibility config initialization", e);
            // If we get here, configs are still not loaded or another issue occurred
            // This is fine - we'll just use lazy loading for everything
        }
        
        // Initialize default permissions map for commonly used permissions
        try {
            defaultPermissions.put("neoessentials.command.home", true);
            defaultPermissions.put("neoessentials.command.warp", true);
            defaultPermissions.put("neoessentials.command.tpa", true);
            defaultPermissions.put("neoessentials.command.back", true);
            defaultPermissions.put("neoessentials.command.spawn", true);
            defaultPermissions.put("neoessentials.command.kit", true);
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error initializing default permissions", e);
        }
    }
      /**
     * Gets whether debug mode is enabled
     * @return True if debug mode is enabled
     */
    public boolean isDebug() {
        return ConfigUtil.getConfigSafe(GeneralConfig.DEBUG_MODE, false);
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
        return ConfigUtil.getConfigSafe(GeneralConfig.ENABLE_ECONOMY, true);
    }
      /**
     * Gets the singular name of the currency
     * @return The currency name (e.g. "Dollar")
     */
    public String getCurrencyNameSingular() {
        if (!isEconomyEnabled()) return "Dollar";
        return ConfigUtil.getConfigSafe(EconomyConfig.CURRENCY_NAME_SINGULAR, "Dollar");
    }
    
    /**
     * Gets the plural name of the currency
     * @return The currency name (e.g. "Dollars")
     */
    public String getCurrencyNamePlural() {
        if (!isEconomyEnabled()) return "Dollars";
        return ConfigUtil.getConfigSafe(EconomyConfig.CURRENCY_NAME_PLURAL, "Dollars");
    }
    
    /**
     * Gets the currency symbol
     * @return The currency symbol (e.g. "$")
     */
    public String getCurrencySymbol() {
        if (!isEconomyEnabled()) return "$";
        return ConfigUtil.getConfigSafe(EconomyConfig.CURRENCY_SYMBOL, "$");
    }
      /**
     * Gets the starting balance for new players
     * @return The starting balance
     */
    public double getStartingBalance() {
        if (!isEconomyEnabled()) return 100.0;
        return ConfigUtil.getConfigSafe(EconomyConfig.STARTING_BALANCE, 100.0);
    }
    
    /**
     * Gets whether teleportation is enabled
     * @return True if teleportation is enabled
     */
    public boolean isTeleportEnabled() {
        return ConfigUtil.getConfigSafe(GeneralConfig.ENABLE_TELEPORTATION, true);
    }
    
    /**
     * Gets the teleport cooldown in seconds
     * @return The cooldown in seconds
     */
    public int getTeleportCooldown() {
        if (!isTeleportEnabled()) return 30;
        return ConfigUtil.getConfigSafe(HomeConfig.COOLDOWN_SECONDS, 30);
    }
    
    /**
     * Gets the teleport warmup in seconds
     * @return The warmup in seconds
     */
    public int getTeleportWarmup() {
        if (!isTeleportEnabled()) return 3;
        return ConfigUtil.getConfigSafe(HomeConfig.WARMUP_SECONDS, 3);
    }
    
    /**
     * Gets the maximum number of homes a player can have
     * @return The max homes
     */
    public int getMaxHomes() {
        if (!isTeleportEnabled()) return 3;
        return ConfigUtil.getConfigSafe(HomeConfig.DEFAULT_MAX_HOMES, 3);
    }
    
    /**
     * Gets whether warps are enabled
     * @return True if warps are enabled
     */
    public boolean isWarpsEnabled() {
        return ConfigUtil.getConfigSafe(GeneralConfig.ENABLE_WARPS, true);
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
        
        boolean enabled = true;
        String commandLower = command.toLowerCase();
        
        switch (commandLower) {
            case "home":
                enabled = ConfigUtil.getConfigSafe(GeneralConfig.ENABLE_HOMES, true);
                break;
            case "warp":
                enabled = ConfigUtil.getConfigSafe(GeneralConfig.ENABLE_WARPS, true);
                break;
            case "tpa":
            case "back":
                enabled = ConfigUtil.getConfigSafe(GeneralConfig.ENABLE_TELEPORTATION, true);
                break;
            case "kit":
                enabled = ConfigUtil.getConfigSafe(GeneralConfig.ENABLE_KITS, true);
                break;
            default:
                enabled = true;
        }
        
        // Cache the result
        commandsEnabled.put(commandLower, enabled);
        return enabled;
    }
    
    /**
     * Gets the map of default permissions
     * @return Map of permission names to boolean values
     */
    public Map<String, Boolean> defaultPermissions() {
        return defaultPermissions;
    }
}
