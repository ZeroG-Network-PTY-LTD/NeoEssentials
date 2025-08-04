package com.zerog.neoessentials.economy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Player economy data container
 * Stores all economic information for a player
 */
public class PlayerEconomyData {
    private final UUID playerId;
    private final Map<String, BigDecimal> balances;
    private final List<String> bankAccounts;
    private final Map<String, Object> economySettings;
    private LocalDateTime lastActivity;
    private BigDecimal totalEarned;
    private BigDecimal totalSpent;
    private int transactionCount;
    
    public PlayerEconomyData(UUID playerId) {
        this.playerId = playerId;
        this.balances = new ConcurrentHashMap<>();
        this.bankAccounts = new ArrayList<>();
        this.economySettings = new ConcurrentHashMap<>();
        this.lastActivity = LocalDateTime.now();
        this.totalEarned = BigDecimal.ZERO;
        this.totalSpent = BigDecimal.ZERO;
        this.transactionCount = 0;
    }
    
    public UUID getPlayerId() { return playerId; }
    
    public BigDecimal getBalance(String currency) {
        return balances.getOrDefault(currency, BigDecimal.ZERO);
    }
    
    public void setBalance(String currency, BigDecimal amount) {
        balances.put(currency, amount);
        updateActivity();
    }
    
    public Map<String, BigDecimal> getAllBalances() {
        return new HashMap<>(balances);
    }
    
    public List<String> getBankAccounts() {
        return new ArrayList<>(bankAccounts);
    }
    
    public void addBankAccount(String accountId) {
        if (!bankAccounts.contains(accountId)) {
            bankAccounts.add(accountId);
        }
    }
    
    public void removeBankAccount(String accountId) {
        bankAccounts.remove(accountId);
    }
    
    public Object getEconomySetting(String key) {
        return economySettings.get(key);
    }
    
    public void setEconomySetting(String key, Object value) {
        economySettings.put(key, value);
    }
    
    public LocalDateTime getLastActivity() { return lastActivity; }
    
    public void updateActivity() {
        this.lastActivity = LocalDateTime.now();
    }
    
    public BigDecimal getTotalEarned() { return totalEarned; }
    public BigDecimal getTotalSpent() { return totalSpent; }
    public int getTransactionCount() { return transactionCount; }
    
    public void addEarned(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            totalEarned = totalEarned.add(amount);
            transactionCount++;
        }
    }
    
    public void addSpent(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            totalSpent = totalSpent.add(amount);
            transactionCount++;
        }
    }
}
