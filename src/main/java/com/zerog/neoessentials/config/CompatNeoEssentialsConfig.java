package com.zerog.neoessentials.config;

<<<<<<< HEAD
import com.zerog.neoessentials.NeoEssentials;
import java.util.HashMap;
import java.util.Map;
import com.zerog.neoessentials.NeoEssentials;
=======
import java.util.HashMap;
import java.util.Map;
>>>>>>> 2c0e119 (feat: Add compatibility layer for legacy config structure and enhance DataManager with scheduler integration)

/**
 * Compatibility layer that adapts our TOML configs to the old config structure.
 * This allows existing code to continue working with the new config system.
<<<<<<< HEAD
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
=======
 */
public class CompatNeoEssentialsConfig {
    // General settings
    private boolean debug = false;
    private String defaultLanguage = "en_us";
    
    // Economy settings
    private boolean economyEnabled = true;
    private String currencyNameSingular = "Dollar";
    private String currencyNamePlural = "Dollars";
    private String currencySymbol = "$";
    private double startingBalance = 100.0;
    
    // Teleportation settings
    private boolean teleportEnabled = true;
    private int teleportCooldown = 30;  // seconds
    private int teleportWarmup = 3;     // seconds
    private int maxHomes = 3;
    
    // Warp settings
    private boolean warpsEnabled = true;
    private Map<String, Integer> warpCosts = new HashMap<>();
    
    // Chat settings
    private boolean chatFormattingEnabled = true;
    private String chatFormat = "{DISPLAYNAME} &7: &f{MESSAGE}";
    
    // Command settings
    private Map<String, Boolean> commandsEnabled = new HashMap<>();
    
    // Permission settings
    private Map<String, Boolean> defaultPermissions = new HashMap<>();    /**
     * Constructor - initializes default values but doesn't load from config
     */
    public CompatNeoEssentialsConfig() {
        // Initialize with default values
        // Actual values will be loaded from configs when requested
    }
    
    /**
     * Initialize config values from the TOML configs
     * This should be called after configs are loaded
     */
    public void initialize() {
        // Initialize with values from TOML configs
        this.debug = GeneralConfig.DEBUG_MODE.get();
        this.economyEnabled = GeneralConfig.ENABLE_ECONOMY.get();
        
        // Economy settings
        if (this.economyEnabled) {
            this.currencyNameSingular = EconomyConfig.CURRENCY_NAME_SINGULAR.get();
            this.currencyNamePlural = EconomyConfig.CURRENCY_NAME_PLURAL.get();
            this.currencySymbol = EconomyConfig.CURRENCY_SYMBOL.get();
            this.startingBalance = EconomyConfig.STARTING_BALANCE.get();
        }
        
        // Teleportation settings
        this.teleportEnabled = GeneralConfig.ENABLE_TELEPORTATION.get();
        if (this.teleportEnabled) {
            this.teleportCooldown = HomeConfig.COOLDOWN_SECONDS.get();
            this.teleportWarmup = HomeConfig.WARMUP_SECONDS.get();
            this.maxHomes = HomeConfig.DEFAULT_MAX_HOMES.get();
        }
        
        // Warp settings
        this.warpsEnabled = GeneralConfig.ENABLE_WARPS.get();
        
        // Command settings
        commandsEnabled.put("home", GeneralConfig.ENABLE_HOMES.get());
        commandsEnabled.put("warp", GeneralConfig.ENABLE_WARPS.get());
        commandsEnabled.put("tpa", GeneralConfig.ENABLE_TELEPORTATION.get());
        commandsEnabled.put("back", GeneralConfig.ENABLE_TELEPORTATION.get());
        commandsEnabled.put("kit", GeneralConfig.ENABLE_KITS.get());
    }
    
    /**
>>>>>>> 2c0e119 (feat: Add compatibility layer for legacy config structure and enhance DataManager with scheduler integration)
     * Gets whether debug mode is enabled
     * @return True if debug mode is enabled
     */
    public boolean isDebug() {
<<<<<<< HEAD
        return ConfigUtil.getConfigSafe(GeneralConfig.DEBUG_MODE, false);
=======
        return debug;
>>>>>>> 2c0e119 (feat: Add compatibility layer for legacy config structure and enhance DataManager with scheduler integration)
    }
    
    /**
     * Gets the default language code
     * @return The language code (e.g. "en_us")
     */
    public String getDefaultLanguage() {
<<<<<<< HEAD
        return "en_us";
    }
      /**
=======
        return defaultLanguage;
    }
    
    /**
>>>>>>> 2c0e119 (feat: Add compatibility layer for legacy config structure and enhance DataManager with scheduler integration)
     * Gets whether economy is enabled
     * @return True if economy is enabled
     */
    public boolean isEconomyEnabled() {
<<<<<<< HEAD
        return ConfigUtil.getConfigSafe(GeneralConfig.ENABLE_ECONOMY, true);
    }
      /**
     * Gets the singular name of the currency
     * @return The currency name (e.g. "Dollar")
     */
    public String getCurrencyNameSingular() {
        if (!isEconomyEnabled()) return "Dollar";
        return ConfigUtil.getConfigSafe(EconomyConfig.CURRENCY_NAME_SINGULAR, "Dollar");
=======
        return economyEnabled;
    }
    
    /**
     * Gets the singular name of the currency
     * @return The currency name
     */
    public String getCurrencyNameSingular() {
        return currencyNameSingular;
>>>>>>> 2c0e119 (feat: Add compatibility layer for legacy config structure and enhance DataManager with scheduler integration)
    }
    
    /**
     * Gets the plural name of the currency
<<<<<<< HEAD
     * @return The currency name (e.g. "Dollars")
     */
    public String getCurrencyNamePlural() {
        if (!isEconomyEnabled()) return "Dollars";
        return ConfigUtil.getConfigSafe(EconomyConfig.CURRENCY_NAME_PLURAL, "Dollars");
=======
     * @return The plural currency name
     */
    public String getCurrencyNamePlural() {
        return currencyNamePlural;
>>>>>>> 2c0e119 (feat: Add compatibility layer for legacy config structure and enhance DataManager with scheduler integration)
    }
    
    /**
     * Gets the currency symbol
<<<<<<< HEAD
     * @return The currency symbol (e.g. "$")
     */
    public String getCurrencySymbol() {
        if (!isEconomyEnabled()) return "$";
        return ConfigUtil.getConfigSafe(EconomyConfig.CURRENCY_SYMBOL, "$");
    }
      /**
=======
     * @return The currency symbol
     */
    public String getCurrencySymbol() {
        return currencySymbol;
    }
    
    /**
>>>>>>> 2c0e119 (feat: Add compatibility layer for legacy config structure and enhance DataManager with scheduler integration)
     * Gets the starting balance for new players
     * @return The starting balance
     */
    public double getStartingBalance() {
<<<<<<< HEAD
        if (!isEconomyEnabled()) return 100.0;
        return ConfigUtil.getConfigSafe(EconomyConfig.STARTING_BALANCE, 100.0);
=======
        return startingBalance;
>>>>>>> 2c0e119 (feat: Add compatibility layer for legacy config structure and enhance DataManager with scheduler integration)
    }
    
    /**
     * Gets whether teleportation is enabled
     * @return True if teleportation is enabled
     */
    public boolean isTeleportEnabled() {
<<<<<<< HEAD
        return ConfigUtil.getConfigSafe(GeneralConfig.ENABLE_TELEPORTATION, true);
=======
        return teleportEnabled;
>>>>>>> 2c0e119 (feat: Add compatibility layer for legacy config structure and enhance DataManager with scheduler integration)
    }
    
    /**
     * Gets the teleport cooldown in seconds
     * @return The cooldown in seconds
     */
    public int getTeleportCooldown() {
<<<<<<< HEAD
        if (!isTeleportEnabled()) return 30;
        return ConfigUtil.getConfigSafe(HomeConfig.COOLDOWN_SECONDS, 30);
=======
        return teleportCooldown;
>>>>>>> 2c0e119 (feat: Add compatibility layer for legacy config structure and enhance DataManager with scheduler integration)
    }
    
    /**
     * Gets the teleport warmup in seconds
     * @return The warmup in seconds
     */
    public int getTeleportWarmup() {
<<<<<<< HEAD
        if (!isTeleportEnabled()) return 3;
        return ConfigUtil.getConfigSafe(HomeConfig.WARMUP_SECONDS, 3);
=======
        return teleportWarmup;
>>>>>>> 2c0e119 (feat: Add compatibility layer for legacy config structure and enhance DataManager with scheduler integration)
    }
    
    /**
     * Gets the maximum number of homes a player can have
     * @return The max homes
     */
    public int getMaxHomes() {
<<<<<<< HEAD
        if (!isTeleportEnabled()) return 3;
        return ConfigUtil.getConfigSafe(HomeConfig.DEFAULT_MAX_HOMES, 3);
=======
        return maxHomes;
>>>>>>> 2c0e119 (feat: Add compatibility layer for legacy config structure and enhance DataManager with scheduler integration)
    }
    
    /**
     * Gets whether warps are enabled
     * @return True if warps are enabled
     */
    public boolean isWarpsEnabled() {
<<<<<<< HEAD
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
=======
        return warpsEnabled;
    }
    
    /**
     * Gets the cost of a particular warp
     * @param warpName The name of the warp
     * @return The cost, or 0 if not specified
     */
    public int getWarpCost(String warpName) {
        return warpCosts.getOrDefault(warpName, 0);
    }
    
    /**
     * Gets whether a particular command is enabled
     * @param commandName The command name
     * @return True if enabled
     */
    public boolean isCommandEnabled(String commandName) {
        return commandsEnabled.getOrDefault(commandName, true);
>>>>>>> 2c0e119 (feat: Add compatibility layer for legacy config structure and enhance DataManager with scheduler integration)
    }
    
    /**
     * Gets the map of default permissions
     * @return Map of permission names to boolean values
     */
    public Map<String, Boolean> defaultPermissions() {
        return defaultPermissions;
    }
}
