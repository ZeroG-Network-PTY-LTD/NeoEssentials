package com.zerog.neoessentials.economy;

import java.util.UUID;

/**
 * Represents a financial transaction in the NeoEssentials economy system.
 * Tracks all money movements for auditing and analytics purposes.
 */
public class Transaction {
    private final UUID transactionId;
    private final UUID fromPlayer;
    private final UUID toPlayer;
    private final double amount;
    private final Currency currency;
    private final String description;
    private final TransactionType type;
    private final long timestamp;
    private final String metadata; // Additional data (JSON format)
    
    public enum TransactionType {
        DEPOSIT,        // Money added to account
        WITHDRAWAL,     // Money removed from account
        TRANSFER_IN,    // Money received from another player
        TRANSFER_OUT,   // Money sent to another player
        PURCHASE,       // Money spent on shop purchases
        SALE,           // Money earned from shop sales
        LOAN_DISBURSEMENT, // Loan money received
        LOAN_PAYMENT,   // Loan payment made
        INTEREST,       // Interest earned or charged
        FEE,            // Service fees
        TAX,            // Taxes paid
        ADMIN,          // Administrative transactions
        SALARY,         // Regular income payments
        BONUS,          // Bonus payments
        PENALTY,        // Penalty charges
        REFUND,         // Refunds issued
        INVESTMENT,     // Investment transactions
        DIVIDEND,       // Dividend payments
        PLAYER_PAY      // Direct player-to-player payments
    }
    
    /**
     * Create a new transaction
     * 
     * @param transactionId Unique transaction identifier
     * @param fromPlayer Source player (null for system transactions)
     * @param toPlayer Destination player (null for system transactions)
     * @param amount Transaction amount (positive for credits, negative for debits)
     * @param currency Transaction currency
     * @param description Human-readable description
     * @param type Transaction type
     * @param timestamp Transaction timestamp
     */
    public Transaction(UUID transactionId, UUID fromPlayer, UUID toPlayer, double amount,
                      Currency currency, String description, TransactionType type, long timestamp) {
        this.transactionId = transactionId;
        this.fromPlayer = fromPlayer;
        this.toPlayer = toPlayer;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
        this.type = type;
        this.timestamp = timestamp;
        this.metadata = "";
    }
    
    /**
     * Create a new transaction with metadata
     * 
     * @param transactionId Unique transaction identifier
     * @param fromPlayer Source player (null for system transactions)
     * @param toPlayer Destination player (null for system transactions)
     * @param amount Transaction amount (positive for credits, negative for debits)
     * @param currency Transaction currency
     * @param description Human-readable description
     * @param type Transaction type
     * @param timestamp Transaction timestamp
     * @param metadata Additional transaction data in JSON format
     */
    public Transaction(UUID transactionId, UUID fromPlayer, UUID toPlayer, double amount,
                      Currency currency, String description, TransactionType type, 
                      long timestamp, String metadata) {
        this.transactionId = transactionId;
        this.fromPlayer = fromPlayer;
        this.toPlayer = toPlayer;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
        this.type = type;
        this.timestamp = timestamp;
        this.metadata = metadata != null ? metadata : "";
    }
    
    /**
     * Check if this transaction involves a specific player
     * 
     * @param playerId The player ID to check
     * @return true if the player is involved in this transaction
     */
    public boolean involvesPlayer(UUID playerId) {
        return (fromPlayer != null && fromPlayer.equals(playerId)) ||
               (toPlayer != null && toPlayer.equals(playerId));
    }
    
    /**
     * Get the effective amount for a specific player
     * Positive if the player received money, negative if they sent money
     * 
     * @param playerId The player ID
     * @return The effective amount for this player
     */
    public double getEffectiveAmount(UUID playerId) {
        if (toPlayer != null && toPlayer.equals(playerId)) {
            return Math.abs(amount); // Player received money
        } else if (fromPlayer != null && fromPlayer.equals(playerId)) {
            return -Math.abs(amount); // Player sent money
        }
        return 0.0; // Player not involved in transaction
    }
    
    /**
     * Check if this is a system transaction (no players involved)
     * 
     * @return true if this is a system transaction
     */
    public boolean isSystemTransaction() {
        return fromPlayer == null && toPlayer == null;
    }
    
    /**
     * Check if this is an income transaction (money coming in)
     * 
     * @return true if this adds money to the economy
     */
    public boolean isIncomeTransaction() {
        return type == TransactionType.DEPOSIT ||
               type == TransactionType.SALARY ||
               type == TransactionType.BONUS ||
               type == TransactionType.INTEREST ||
               type == TransactionType.DIVIDEND ||
               type == TransactionType.LOAN_DISBURSEMENT ||
               type == TransactionType.REFUND;
    }
    
    /**
     * Check if this is an expense transaction (money going out)
     * 
     * @return true if this removes money from the economy
     */
    public boolean isExpenseTransaction() {
        return type == TransactionType.WITHDRAWAL ||
               type == TransactionType.PURCHASE ||
               type == TransactionType.LOAN_PAYMENT ||
               type == TransactionType.FEE ||
               type == TransactionType.TAX ||
               type == TransactionType.PENALTY;
    }
    
    /**
     * Format the transaction amount with currency symbol
     * 
     * @return Formatted amount string
     */
    public String getFormattedAmount() {
        return currency.format(Math.abs(amount));
    }
    
    /**
     * Get a summary description of the transaction
     * 
     * @return Summary string
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append(type.name()).append(": ");
        sb.append(getFormattedAmount());
        
        if (fromPlayer != null && toPlayer != null) {
            sb.append(" (").append(fromPlayer.toString().substring(0, 8))
              .append(" → ").append(toPlayer.toString().substring(0, 8)).append(")");
        } else if (fromPlayer != null) {
            sb.append(" from ").append(fromPlayer.toString().substring(0, 8));
        } else if (toPlayer != null) {
            sb.append(" to ").append(toPlayer.toString().substring(0, 8));
        }
        
        return sb.toString();
    }
    
    // Getters
    public UUID getTransactionId() { return transactionId; }
    public UUID getFromPlayer() { return fromPlayer; }
    public UUID getToPlayer() { return toPlayer; }
    public double getAmount() { return amount; }
    public Currency getCurrency() { return currency; }
    public String getDescription() { return description; }
    public TransactionType getType() { return type; }
    public long getTimestamp() { return timestamp; }
    public String getMetadata() { return metadata; }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Transaction that = (Transaction) obj;
        return transactionId.equals(that.transactionId);
    }
    
    @Override
    public int hashCode() {
        return transactionId.hashCode();
    }
    
    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + transactionId +
                ", type=" + type +
                ", amount=" + getFormattedAmount() +
                ", timestamp=" + timestamp +
                '}';
    }
}
