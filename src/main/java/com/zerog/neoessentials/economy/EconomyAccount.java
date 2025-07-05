package com.zerog.neoessentials.economy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a player's economic account.
 * Thread-safe implementation with balance tracking for multiple currencies.
 */
public class EconomyAccount {
    
    public enum Status {
        ACTIVE,     // Account is active and can be used
        FROZEN,     // Account is frozen, no transactions allowed
        SUSPENDED,  // Account is temporarily suspended
        CLOSED      // Account is permanently closed
    }
    
    private final UUID playerId;
    private final String playerName;
    private final LocalDateTime createdAt;
    private volatile Status status;
    private volatile LocalDateTime lastActivity;
    
    // Thread-safe balance storage for multiple currencies
    private final Map<Currency, BigDecimal> balances;
    
    // Account metadata
    private final Map<String, String> metadata;
    
    public EconomyAccount(UUID playerId, String playerName) {
        this.playerId = Objects.requireNonNull(playerId, "Player ID cannot be null");
        this.playerName = Objects.requireNonNull(playerName, "Player name cannot be null");
        this.createdAt = LocalDateTime.now();
        this.status = Status.ACTIVE;
        this.lastActivity = LocalDateTime.now();
        this.balances = new ConcurrentHashMap<>();
        this.metadata = new ConcurrentHashMap<>();
    }
    
    /**
     * Gets the balance for a specific currency
     */
    public BigDecimal getBalance(Currency currency) {
        Objects.requireNonNull(currency, "Currency cannot be null");
        return balances.getOrDefault(currency, BigDecimal.ZERO);
    }
    
    /**
     * Sets the balance for a specific currency
     */
    public synchronized void setBalance(Currency currency, BigDecimal amount) {
        Objects.requireNonNull(currency, "Currency cannot be null");
        Objects.requireNonNull(amount, "Amount cannot be null");
        
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Balance cannot be negative");
        }
        
        balances.put(currency, amount.setScale(currency.getDecimalPlaces(), BigDecimal.ROUND_HALF_UP));
        updateLastActivity();
    }
    
    /**
     * Adds to the balance for a specific currency
     */
    public synchronized void addBalance(Currency currency, BigDecimal amount) {
        Objects.requireNonNull(currency, "Currency cannot be null");
        Objects.requireNonNull(amount, "Amount cannot be null");
        
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Cannot add negative amount");
        }
        
        BigDecimal currentBalance = getBalance(currency);
        setBalance(currency, currentBalance.add(amount));
    }
    
    /**
     * Subtracts from the balance for a specific currency
     * @return true if successful, false if insufficient funds
     */
    public synchronized boolean subtractBalance(Currency currency, BigDecimal amount) {
        Objects.requireNonNull(currency, "Currency cannot be null");
        Objects.requireNonNull(amount, "Amount cannot be null");
        
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Cannot subtract negative amount");
        }
        
        BigDecimal currentBalance = getBalance(currency);
        if (currentBalance.compareTo(amount) < 0) {
            return false; // Insufficient funds
        }
        
        setBalance(currency, currentBalance.subtract(amount));
        return true;
    }
    
    /**
     * Checks if the account has sufficient balance
     */
    public boolean hasBalance(Currency currency, BigDecimal amount) {
        Objects.requireNonNull(currency, "Currency cannot be null");
        Objects.requireNonNull(amount, "Amount cannot be null");
        
        return getBalance(currency).compareTo(amount) >= 0;
    }
    
    /**
     * Gets all currency balances
     */
    public Map<Currency, BigDecimal> getAllBalances() {
        return new HashMap<>(balances);
    }
    
    /**
     * Checks if the account can perform transactions
     */
    public boolean isActive() {
        return status == Status.ACTIVE;
    }
    
    /**
     * Updates the last activity timestamp
     */
    public void updateLastActivity() {
        this.lastActivity = LocalDateTime.now();
    }
    
    /**
     * Sets account metadata
     */
    public void setMetadata(String key, String value) {
        Objects.requireNonNull(key, "Metadata key cannot be null");
        if (value == null) {
            metadata.remove(key);
        } else {
            metadata.put(key, value);
        }
    }
    
    /**
     * Gets account metadata
     */
    public String getMetadata(String key) {
        return metadata.get(key);
    }
    
    /**
     * Gets all metadata
     */
    public Map<String, String> getAllMetadata() {
        return new HashMap<>(metadata);
    }
    
    // Getters and Setters
    public UUID getPlayerId() { return playerId; }
    public String getPlayerName() { return playerName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = Objects.requireNonNull(status); }
    public LocalDateTime getLastActivity() { return lastActivity; }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        EconomyAccount that = (EconomyAccount) obj;
        return Objects.equals(playerId, that.playerId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(playerId);
    }
    
    @Override
    public String toString() {
        return String.format("EconomyAccount{player=%s, status=%s, currencies=%d}", 
                playerName, status, balances.size());
    }
}
