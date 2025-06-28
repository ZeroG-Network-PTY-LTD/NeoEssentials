package com.zerog.neoessentials.economy;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Stores all economy-related data for a single player.
 * Includes balances in multiple currencies, transaction history, and economic stats.
 */
public class PlayerEconomyData {
    private final UUID playerId;
    private final Map<Currency, Double> balances;
    private final long joinTime;
    private double totalEarned;
    private double totalSpent;
    private int transactionCount;
    private double creditScore;
    private long lastActivity;
    
    public PlayerEconomyData(UUID playerId) {
        this.playerId = playerId;
        this.balances = new HashMap<>();
        this.joinTime = System.currentTimeMillis();
        this.totalEarned = 0.0;
        this.totalSpent = 0.0;
        this.transactionCount = 0;
        this.creditScore = 750.0; // Start with good credit score
        this.lastActivity = System.currentTimeMillis();
    }
    
    /**
     * Get balance for a specific currency
     * 
     * @param currency The currency
     * @return The balance, or 0.0 if no balance exists
     */
    public double getBalance(Currency currency) {
        return balances.getOrDefault(currency, 0.0);
    }
    
    /**
     * Set balance for a specific currency
     * 
     * @param currency The currency
     * @param amount The new balance
     */
    public void setBalance(Currency currency, double amount) {
        balances.put(currency, amount);
        lastActivity = System.currentTimeMillis();
    }
    
    /**
     * Add to balance for a specific currency
     * 
     * @param currency The currency
     * @param amount The amount to add
     */
    public void addBalance(Currency currency, double amount) {
        double current = getBalance(currency);
        setBalance(currency, current + amount);
        
        if (amount > 0) {
            totalEarned += amount;
        } else {
            totalSpent += Math.abs(amount);
        }
        transactionCount++;
    }
    
    /**
     * Check if player has sufficient balance
     * 
     * @param currency The currency to check
     * @param amount The amount needed
     * @return true if player has sufficient balance
     */
    public boolean hasBalance(Currency currency, double amount) {
        return getBalance(currency) >= amount;
    }
    
    /**
     * Get all balances for this player
     * 
     * @return Map of currency to balance
     */
    public Map<Currency, Double> getAllBalances() {
        return new HashMap<>(balances);
    }
    
    /**
     * Get total net worth in default currency
     * 
     * @return Total value of all balances converted to default currency
     */
    public double getNetWorth() {
        Currency defaultCurrency = CurrencyManager.getInstance().getDefaultCurrency();
        if (defaultCurrency == null) return 0.0;
        
        double totalWorth = 0.0;
        CurrencyManager currencyManager = CurrencyManager.getInstance();
        
        for (Map.Entry<Currency, Double> entry : balances.entrySet()) {
            Currency currency = entry.getKey();
            double amount = entry.getValue();
            
            if (currency.equals(defaultCurrency)) {
                totalWorth += amount;
            } else {
                totalWorth += currencyManager.convertCurrency(amount, currency, defaultCurrency);
            }
        }
        
        return totalWorth;
    }
    
    /**
     * Update credit score based on transaction history and account age
     */
    public void updateCreditScore() {
        double baseScore = 600.0;
        
        // Account age bonus (up to 100 points for accounts older than 1 year)
        long accountAge = System.currentTimeMillis() - joinTime;
        long oneYear = 365L * 24L * 60L * 60L * 1000L;
        double ageBonus = Math.min(100.0, (double) accountAge / oneYear * 100.0);
        
        // Transaction history bonus (up to 50 points)
        double transactionBonus = Math.min(50.0, transactionCount * 0.5);
        
        // Income to spending ratio (up to 100 points)
        double ratioBonus = 0.0;
        if (totalSpent > 0) {
            double ratio = totalEarned / totalSpent;
            ratioBonus = Math.min(100.0, ratio * 50.0);
        } else if (totalEarned > 0) {
            ratioBonus = 50.0; // Bonus for earning without spending
        }
        
        // Activity bonus (lose points for inactivity)
        long timeSinceActivity = System.currentTimeMillis() - lastActivity;
        long thirtyDays = 30L * 24L * 60L * 60L * 1000L;
        double activityPenalty = 0.0;
        if (timeSinceActivity > thirtyDays) {
            activityPenalty = Math.min(50.0, (double) timeSinceActivity / thirtyDays * 10.0);
        }
        
        creditScore = Math.max(300.0, Math.min(850.0, 
            baseScore + ageBonus + transactionBonus + ratioBonus - activityPenalty));
    }
    
    // Getters
    public UUID getPlayerId() { return playerId; }
    public long getJoinTime() { return joinTime; }
    public double getTotalEarned() { return totalEarned; }
    public double getTotalSpent() { return totalSpent; }
    public int getTransactionCount() { return transactionCount; }
    public double getCreditScore() { return creditScore; }
    public long getLastActivity() { return lastActivity; }
    
    // Setters for admin purposes
    public void setCreditScore(double creditScore) { 
        this.creditScore = Math.max(300.0, Math.min(850.0, creditScore)); 
    }
    
    @Override
    public String toString() {
        return "PlayerEconomyData{" +
                "playerId=" + playerId +
                ", netWorth=" + getNetWorth() +
                ", creditScore=" + creditScore +
                ", transactionCount=" + transactionCount +
                '}';
    }
}
