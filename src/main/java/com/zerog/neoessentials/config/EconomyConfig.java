package com.zerog.neoessentials.config;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Economy configuration for NeoEssentials
 * Compatible with Vault API and EssentialsX economy
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class EconomyConfig {
    
    // Basic economy settings
    public boolean enabled = true;
    public String currencySymbol = "$";
    public String currencyName = "dollar";
    public String currencyNamePlural = "dollars";
    public String currencyFormat = "#,##0.00";
    
    // Starting balance and limits
    public double startingBalance = 100.00;
    public double minimumBalance = 0.00;
    public double maxBalance = 10000000.00;
    
    // Transaction settings
    public double minimumPayAmount = 0.01;
    public double maximumPayAmount = 10000.00;
    public boolean logTransactions = true;
    public double transferFeePercent = 0.0; // 0% transfer fee by default
    
    // Command costs
    public Map<String, BigDecimal> commandCosts = new HashMap<>();
    
    // Vault integration
    public VaultConfig vault = new VaultConfig();
    
    // Shop settings
    public ShopConfig shop = new ShopConfig();
    
    // Bank settings  
    public BankConfig bank = new BankConfig();
    
    // Cleanup settings
    public boolean cleanupInactiveAccounts = false;
    
    // Messages
    public MessagesConfig messages = new MessagesConfig();
    
    public EconomyConfig() {
        initializeDefaults();
    }

    /**
     * Load configuration from file or initialize defaults
     */
    public void load() {
        // Load configuration from file if it exists
        // For now, just ensure defaults are initialized
        initializeDefaults();
    }
    
    private void initializeDefaults() {
        // Default command costs
        commandCosts.put("heal", new BigDecimal("10.00"));
        commandCosts.put("feed", new BigDecimal("5.00"));
        commandCosts.put("fly", new BigDecimal("20.00"));
        commandCosts.put("god", new BigDecimal("50.00"));
        commandCosts.put("repair", new BigDecimal("15.00"));
        commandCosts.put("kit", new BigDecimal("25.00"));
    }
    
    public static class VaultConfig {
        public boolean enabled = true;
        public String economyName = "NeoEssentials Economy";
        public boolean requireServer = false;
        public boolean supportBanks = false;
    }
    
    public static class ShopConfig {
        public boolean enabled = true;
        public boolean allowSigns = true;
        public boolean allowChestShops = true;
        public boolean allowAdminShops = true;
        
        // Shop creation costs
        public BigDecimal signShopCost = new BigDecimal("100.00");
        public BigDecimal chestShopCost = new BigDecimal("250.00");
        
        // Transaction fees
        public double transactionFeePercent = 0.05; // 5%
        public BigDecimal minimumTransactionFee = new BigDecimal("0.10");
        public BigDecimal maximumTransactionFee = new BigDecimal("50.00");
        
        // Shop limits
        public int maxShopsPerPlayer = 10;
        public boolean enableShopTax = false;
        public double dailyShopTaxPercent = 0.01; // 1% daily
    }
    
    public static class BankConfig {
        public boolean enabled = false;
        public boolean allowLoans = false;
        public boolean allowSavings = true;
        
        // Interest rates
        public double interestRate = 0.02; // 2% daily
        public double loanInterestRate = 0.05; // 5% daily
        
        // Limits
        public double maximumLoanAmount = 10000.00;
        public double minimumBalance = 100.00;
        public double maxInterestPayout = 1000.00;
        
        // Processing
        public int interestCalculationHours = 24; // Calculate every 24 hours
    }
    
    public static class MessagesConfig {
        public String insufficientFunds = "&cYou don't have enough money! You need {0} but only have {1}.";
        public String commandCostCharged = "&aYou were charged {0} for using /{1}.";
        public String balanceUpdated = "&aYour balance has been updated to {0}.";
        public String transactionComplete = "&aTransaction complete!";
        public String economyDisabled = "&cThe economy system is disabled.";
    }
    
    /**
     * Get command cost for a specific command
     */
    public BigDecimal getCommandCost(String command) {
        return commandCosts.getOrDefault(command.toLowerCase(), BigDecimal.ZERO);
    }
    
    /**
     * Check if command has a cost
     */
    public boolean hasCommandCost(String command) {
        return commandCosts.containsKey(command.toLowerCase()) && 
               commandCosts.get(command.toLowerCase()).compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Format currency amount for display
     */
    public String formatCurrency(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ONE) == 0) {
            return currencySymbol + amount.toString() + " " + currencyName;
        } else {
            return currencySymbol + amount.toString() + " " + currencyNamePlural;
        }
    }
    
    /**
     * Check if Vault integration is enabled
     */
    public boolean isVaultEnabled() {
        return enabled && vault.enabled;
    }
    
    /**
     * Validate the economy configuration
     */
    public boolean isValid() {
        if (!enabled) return true; // Valid if disabled
        
        if (startingBalance < 0) return false;
        if (maxBalance <= 0) return false;
        if (transferFeePercent < 0 || transferFeePercent > 100) return false;
        
        // Validate currency settings
        if (currencySymbol == null || currencySymbol.isEmpty()) return false;
        if (currencyFormat == null || currencyFormat.isEmpty()) return false;
        
        return true;
    }
    
    // Additional methods for advanced economy features
    public BigDecimal getStartingBalance() {
        return BigDecimal.valueOf(startingBalance);
    }
    
    public BigDecimal getTransferFeeRate() {
        return BigDecimal.valueOf(transferFeePercent / 100.0);
    }
    
    public BigDecimal getMaxTransferFee() {
        return BigDecimal.valueOf(maxBalance * 0.1); // 10% of max balance
    }
    
    public int getInterestUpdateInterval() {
        return 3600; // 1 hour in seconds
    }
    
    public int getMarketUpdateInterval() {
        return 1800; // 30 minutes in seconds
    }
    
    public int getTransactionRetentionDays() {
        return 90; // 90 days
    }
}
