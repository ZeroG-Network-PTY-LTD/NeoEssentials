package com.zerog.neoessentials.economy;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a player's personal wallet/cash system, separate from bank accounts.
 * This is the "cash on hand" that players use for daily transactions.
 */
public class PlayerWallet {
    private final UUID playerId;
    private final Map<Currency, Double> cashBalances;
    private long lastUpdated;
    
    public PlayerWallet(UUID playerId) {
        this.playerId = playerId;
        this.cashBalances = new ConcurrentHashMap<>();
        this.lastUpdated = System.currentTimeMillis();
        
        // Initialize with starting balance in default currency
        Currency defaultCurrency = CurrencyManager.getInstance().getDefaultCurrency();
        if (defaultCurrency != null) {
            // Get starting balance from economy config
            double startingBalance = getStartingBalance();
            cashBalances.put(defaultCurrency, startingBalance);
        }
    }
    
    /**
     * Get cash balance for a specific currency
     * 
     * @param currency The currency
     * @return The cash balance, or 0.0 if no balance exists
     */
    public double getCashBalance(Currency currency) {
        return cashBalances.getOrDefault(currency, 0.0);
    }
    
    /**
     * Set cash balance for a specific currency
     * 
     * @param currency The currency
     * @param amount The new balance
     * @return true if successful
     */
    public boolean setCashBalance(Currency currency, double amount) {
        if (amount < 0 && !allowNegativeBalances()) {
            return false;
        }
        
        double maxBalance = getMaxBalance();
        if (amount > maxBalance) {
            return false;
        }
        
        cashBalances.put(currency, amount);
        lastUpdated = System.currentTimeMillis();
        return true;
    }
    
    /**
     * Add to cash balance for a specific currency
     * 
     * @param currency The currency
     * @param amount The amount to add (can be negative to subtract)
     * @return true if successful
     */
    public boolean addCash(Currency currency, double amount) {
        double currentBalance = getCashBalance(currency);
        double newBalance = currentBalance + amount;
        
        return setCashBalance(currency, newBalance);
    }
    
    /**
     * Subtract from cash balance for a specific currency
     * 
     * @param currency The currency
     * @param amount The amount to subtract
     * @return true if successful
     */
    public boolean subtractCash(Currency currency, double amount) {
        if (amount <= 0) return false;
        
        double currentBalance = getCashBalance(currency);
        if (currentBalance < amount && !allowNegativeBalances()) {
            return false; // Insufficient funds
        }
        
        return addCash(currency, -amount);
    }
    
    /**
     * Check if player has sufficient cash
     * 
     * @param currency The currency to check
     * @param amount The amount needed
     * @return true if player has sufficient cash
     */
    public boolean hasCash(Currency currency, double amount) {
        return getCashBalance(currency) >= amount;
    }
    
    /**
     * Transfer cash from this wallet to another wallet
     * 
     * @param targetWallet The target wallet
     * @param currency The currency to transfer
     * @param amount The amount to transfer
     * @return true if successful
     */
    public boolean transferCash(PlayerWallet targetWallet, Currency currency, double amount) {
        if (amount <= 0) return false;
        if (!hasCash(currency, amount)) return false;
        
        // Check if target can receive this amount
        double targetCurrentBalance = targetWallet.getCashBalance(currency);
        double targetMaxBalance = targetWallet.getMaxBalance();
        if (targetCurrentBalance + amount > targetMaxBalance) {
            return false; // Target would exceed maximum balance
        }
        
        // Perform the transfer
        if (subtractCash(currency, amount)) {
            if (targetWallet.addCash(currency, amount)) {
                return true;
            } else {
                // Rollback if target addition failed
                addCash(currency, amount);
                return false;
            }
        }
        
        return false;
    }
    
    /**
     * Get all cash balances for this wallet
     * 
     * @return Map of currency to balance
     */
    public Map<Currency, Double> getAllCashBalances() {
        return new HashMap<>(cashBalances);
    }
    
    /**
     * Get total cash worth in default currency
     * 
     * @return Total value of all cash balances converted to default currency
     */
    public double getTotalCashWorth() {
        Currency defaultCurrency = CurrencyManager.getInstance().getDefaultCurrency();
        if (defaultCurrency == null) return 0.0;
        
        double totalWorth = 0.0;
        CurrencyManager currencyManager = CurrencyManager.getInstance();
        
        for (Map.Entry<Currency, Double> entry : cashBalances.entrySet()) {
            Currency currency = entry.getKey();
            double amount = entry.getValue();
            
            if (currency.equals(defaultCurrency)) {
                totalWorth += amount;
            } else {
                // Convert to default currency
                double convertedAmount = currencyManager.convertCurrency(
                    currency, defaultCurrency, amount
                );
                totalWorth += convertedAmount;
            }
        }
        
        return totalWorth;
    }
    
    /**
     * Get the player ID for this wallet
     * 
     * @return The player UUID
     */
    public UUID getPlayerId() {
        return playerId;
    }
    
    /**
     * Get when this wallet was last updated
     * 
     * @return Last update timestamp
     */
    public long getLastUpdated() {
        return lastUpdated;
    }
    
    /**
     * Get starting balance from economy configuration
     * 
     * @return Starting balance amount
     */
    private double getStartingBalance() {
        try {
            // This would normally come from economy config
            // For now, return a default value
            return 100.0;
        } catch (Exception e) {
            return 100.0; // Default fallback
        }
    }
    
    /**
     * Get maximum balance from economy configuration
     * 
     * @return Maximum balance amount
     */
    private double getMaxBalance() {
        try {
            // This would normally come from economy config
            // For now, return a default value
            return 1000000.0;
        } catch (Exception e) {
            return 1000000.0; // Default fallback
        }
    }
    
    /**
     * Check if negative balances are allowed from economy configuration
     * 
     * @return true if negative balances are allowed
     */
    private boolean allowNegativeBalances() {
        try {
            // This would normally come from economy config
            // For now, return false for safety
            return false;
        } catch (Exception e) {
            return false; // Default fallback
        }
    }
}
