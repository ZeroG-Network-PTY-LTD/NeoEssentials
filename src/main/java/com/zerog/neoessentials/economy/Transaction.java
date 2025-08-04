package com.zerog.neoessentials.economy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a financial transaction in the economy system
 */
public class Transaction {
    private final String transactionId;
    private final UUID fromPlayer;
    private final UUID toPlayer;
    private final String currency;
    private final BigDecimal amount;
    private final TransactionType type;
    private final String description;
    private final LocalDateTime timestamp;
    private final String serverSource;
    private final boolean processed;
    
    public Transaction(UUID fromPlayer, UUID toPlayer, String currency, 
                      BigDecimal amount, TransactionType type, String description) {
        this.transactionId = UUID.randomUUID().toString();
        this.fromPlayer = fromPlayer;
        this.toPlayer = toPlayer;
        this.currency = currency;
        this.amount = amount;
        this.type = type;
        this.description = description;
        this.timestamp = LocalDateTime.now();
        this.serverSource = "local";
        this.processed = true;
    }
    
    public Transaction(String transactionId, UUID fromPlayer, UUID toPlayer, 
                      String currency, BigDecimal amount, TransactionType type, 
                      String description, LocalDateTime timestamp, String serverSource, 
                      boolean processed) {
        this.transactionId = transactionId;
        this.fromPlayer = fromPlayer;
        this.toPlayer = toPlayer;
        this.currency = currency;
        this.amount = amount;
        this.type = type;
        this.description = description;
        this.timestamp = timestamp;
        this.serverSource = serverSource;
        this.processed = processed;
    }
    
    // Getters
    public String getTransactionId() { return transactionId; }
    public UUID getFromPlayer() { return fromPlayer; }
    public UUID getToPlayer() { return toPlayer; }
    public String getCurrency() { return currency; }
    public BigDecimal getAmount() { return amount; }
    public TransactionType getType() { return type; }
    public String getDescription() { return description; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getServerSource() { return serverSource; }
    public boolean isProcessed() { return processed; }
    
    @Override
    public String toString() {
        return String.format("Transaction{id=%s, type=%s, amount=%s %s, from=%s, to=%s, time=%s}", 
            transactionId, type, amount, currency, fromPlayer, toPlayer, timestamp);
    }
}
