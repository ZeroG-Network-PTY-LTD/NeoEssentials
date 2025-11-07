
package com.zerog.neoessentials.config;


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
        // Load from ConfigManager - use static methods
        // Set values with defaults using ConfigManager
        this.startingBalance = BigDecimal.valueOf(ConfigManager.getEconomyStartingBalance());
        this.currencySymbol = ConfigManager.getCurrencySymbol();
        this.maxBalance = BigDecimal.valueOf(ConfigManager.getMaxBalance());
        this.taxPercentage = ConfigManager.getTaxPercentage();
        this.allowNegativeBalances = ConfigManager.allowNegativeBalances();
        this.cleanupInactiveAccounts = ConfigManager.isCleanupInactiveAccountsEnabled();
        this.inactiveAccountCleanupDays = ConfigManager.getInactiveAccountCleanupDays();
        this.maxTransferAmount = BigDecimal.valueOf(ConfigManager.getMaxTransferAmount());
        this.paytoggleDefault = ConfigManager.getPayToggleDefault();
        this.cacheMaximumSize = ConfigManager.getCacheMaximumSize();
        this.cacheExpireAfterAccessMinutes = ConfigManager.getCacheExpireAfterAccessMinutes();
    }

    public static EconomyConfig load(File configFile) {
        // Delegate to ConfigManager - it handles loading automatically
        ConfigManager.loadAll();
        return new EconomyConfig();
    }
}
