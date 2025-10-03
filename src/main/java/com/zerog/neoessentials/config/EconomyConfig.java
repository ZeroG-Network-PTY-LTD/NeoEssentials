
package com.zerog.neoessentials.config;

import com.google.gson.JsonObject;
import java.io.File;
import java.math.BigDecimal;

/**
 * Economy configuration wrapper.
 * Now delegates to the centralized ConfigManager but maintains the same interface.
 * 
 * @deprecated Use ConfigManager.getInstance() directly for new code
 */
@Deprecated
public class EconomyConfig {
    public BigDecimal startingBalance;
    public String currencySymbol;
    public BigDecimal maxBalance;
    public boolean cleanupInactiveAccounts;
    public double taxPercentage;
    public BigDecimal maxTransferAmount;
    public boolean paytoggleDefault;
    public boolean allowNegativeBalances;
    public int inactiveAccountCleanupDays;
    // Cache configuration
    public int cacheMaximumSize;
    public int cacheExpireAfterAccessMinutes;

    public EconomyConfig() {
        // Load from ConfigManager
        ConfigManager configManager = ConfigManager.getInstance();
        JsonObject config = configManager.getConfig(ConfigManager.ECONOMY_CONFIG);
        JsonObject econ = config.has("economySettings") ? config.getAsJsonObject("economySettings") : new JsonObject();
        
        // Set values with defaults
        this.startingBalance = configManager.getEconomyStartingBalance();
        this.currencySymbol = configManager.getCurrencySymbol();
        this.maxBalance = configManager.getMaxBalance();
        this.taxPercentage = configManager.getTaxPercentage();
        
        // Other settings with defaults
        this.cleanupInactiveAccounts = econ.has("cleanupInactiveAccounts") ? econ.get("cleanupInactiveAccounts").getAsBoolean() : true;
        this.maxTransferAmount = econ.has("maxTransferAmount") ? econ.get("maxTransferAmount").getAsBigDecimal() : new BigDecimal("10000.0");
        this.paytoggleDefault = econ.has("paytoggleDefault") ? econ.get("paytoggleDefault").getAsBoolean() : true;
        this.allowNegativeBalances = econ.has("allowNegativeBalances") ? econ.get("allowNegativeBalances").getAsBoolean() : false;
        this.inactiveAccountCleanupDays = econ.has("inactiveAccountCleanupDays") ? econ.get("inactiveAccountCleanupDays").getAsInt() : 30;
        this.cacheMaximumSize = econ.has("cacheMaximumSize") ? econ.get("cacheMaximumSize").getAsInt() : 10000;
        this.cacheExpireAfterAccessMinutes = econ.has("cacheExpireAfterAccessMinutes") ? econ.get("cacheExpireAfterAccessMinutes").getAsInt() : 60;
    }

    public static EconomyConfig load(File configFile) {
        // Delegate to ConfigManager - it handles loading automatically
        ConfigManager.getInstance().loadAll();
        return new EconomyConfig();
    }
}
