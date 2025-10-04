
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
        // Load from ConfigManager
        ConfigManager configManager = ConfigManager.getInstance();
        // Set values with defaults using ConfigManager
        this.startingBalance = configManager.getEconomyStartingBalance();
        this.currencySymbol = configManager.getCurrencySymbol();
        this.maxBalance = configManager.getMaxBalance();
        this.taxPercentage = configManager.getTaxPercentage();
        this.allowNegativeBalances = configManager.allowNegativeBalances();
        this.cleanupInactiveAccounts = configManager.isCleanupInactiveAccountsEnabled();
        this.inactiveAccountCleanupDays = configManager.getInactiveAccountCleanupDays();
        this.maxTransferAmount = configManager.getMaxTransferAmount();
        this.paytoggleDefault = configManager.getPayToggleDefault();
        this.cacheMaximumSize = configManager.getCacheMaximumSize();
        this.cacheExpireAfterAccessMinutes = configManager.getCacheExpireAfterAccessMinutes();
    }

    public static EconomyConfig load(File configFile) {
        // Delegate to ConfigManager - it handles loading automatically
        ConfigManager.getInstance().loadAll();
        return new EconomyConfig();
    }
}
