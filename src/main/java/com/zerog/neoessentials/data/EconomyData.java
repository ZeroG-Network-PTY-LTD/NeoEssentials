package com.zerog.neoessentials.data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents economy data for a player
 */
public class EconomyData {
    private BigDecimal balance;
    private List<Transaction> transactions;
    
    public EconomyData() {
        this.balance = BigDecimal.ZERO;
        this.transactions = new ArrayList<>();
    }
    
    public EconomyData(BigDecimal balance) {
        this.balance = balance;
        this.transactions = new ArrayList<>();
    }
    
    /**
     * Gets the player's balance
     * 
     * @return The balance
     */
    public BigDecimal getBalance() {
        return balance;
    }
    
    /**
     * Sets the player's balance
     * 
     * @param balance The new balance
     */
    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
    
    /**
     * Adds to the player's balance
     * 
     * @param amount The amount to add
     */
    public void addToBalance(BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }
    
    /**
     * Subtracts from the player's balance
     * 
     * @param amount The amount to subtract
     */
    public void subtractFromBalance(BigDecimal amount) {
        this.balance = this.balance.subtract(amount);
    }
    
    /**
     * Gets the transaction history
     * 
     * @return The transactions
     */
    public List<Transaction> getTransactions() {
        return transactions;
    }
    
    /**
     * Adds a transaction to the history
     * 
     * @param transaction The transaction to add
     */
    public void addTransaction(Transaction transaction) {
        this.transactions.add(transaction);
        
        // Limit transaction history size
        if (this.transactions.size() > 100) {
            this.transactions.remove(0);
        }
    }
    
    /**
     * Represents a transaction in the economy system
     */
    public static class Transaction {
        private String description;
        private BigDecimal amount;
        private long timestamp;
        
        public Transaction(String description, BigDecimal amount) {
            this.description = description;
            this.amount = amount;
            this.timestamp = System.currentTimeMillis();
        }
        
        public Transaction(String description, BigDecimal amount, long timestamp) {
            this.description = description;
            this.amount = amount;
            this.timestamp = timestamp;
        }
        
        /**
         * Gets the description of the transaction
         * 
         * @return The description
         */
        public String getDescription() {
            return description;
        }
        
        /**
         * Gets the amount of the transaction
         * 
         * @return The amount
         */
        public BigDecimal getAmount() {
            return amount;
        }
        
        /**
         * Gets the timestamp of the transaction
         * 
         * @return The timestamp in milliseconds
         */
        public long getTimestamp() {
            return timestamp;
        }
    }
}
