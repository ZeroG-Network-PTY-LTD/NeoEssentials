package com.zerog.neoessentials.config;

import java.util.HashMap;
import java.util.Map;

/**
 * Compatibility layer that adapts our TOML configs to the old config structure.
 * This allows existing code to continue working with the new config system.
 */
public class NeoEssentialsConfig {
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
    private Map<String, Boolean> defaultPermissions = new HashMap<>();
    
    /**
     * Constructor that loads values from the TOML configs
     */
    public NeoEssentialsConfig() {
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
     * Gets whether debug mode is enabled
     * @return True if debug mode is enabled
     */
    public boolean isDebug() {
        return debug;
    }
    
    /**
     * Gets the default language code
     * @return The language code (e.g. "en_us")
     */
    public String getDefaultLanguage() {
        return defaultLanguage;
    }
    
    /**
     * Gets whether economy is enabled
     * @return True if economy is enabled
     */
    public boolean isEconomyEnabled() {
        return economyEnabled;
    }
    
    /**
     * Gets the singular name of the currency
     * @return The currency name
     */
    public String getCurrencyNameSingular() {
        return currencyNameSingular;
    }
    
    /**
     * Gets the plural name of the currency
     * @return The plural currency name
     */
    public String getCurrencyNamePlural() {
        return currencyNamePlural;
    }
    
    /**
     * Gets the currency symbol
     * @return The currency symbol
     */
    public String getCurrencySymbol() {
        return currencySymbol;
    }
    
    /**
     * Gets the starting balance for new players
     * @return The starting balance
     */
    public double getStartingBalance() {
        return startingBalance;
    }
    
    /**
     * Gets whether teleportation is enabled
     * @return True if teleportation is enabled
     */
    public boolean isTeleportEnabled() {
        return teleportEnabled;
    }
    
    /**
     * Gets the teleport cooldown in seconds
     * @return The cooldown in seconds
     */
    public int getTeleportCooldown() {
        return teleportCooldown;
    }
    
    /**
     * Gets the teleport warmup in seconds
     * @return The warmup in seconds
     */
    public int getTeleportWarmup() {
        return teleportWarmup;
    }
    
    /**
     * Gets the maximum number of homes a player can have
     * @return The max homes
     */
    public int getMaxHomes() {
        return maxHomes;
    }
    
    /**
     * Gets whether warps are enabled
     * @return True if warps are enabled
     */
    public boolean isWarpsEnabled() {
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
    }
}
