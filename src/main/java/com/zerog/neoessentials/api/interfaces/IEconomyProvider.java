package com.zerog.neoessentials.api.interfaces;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.List;

/**
 * Economy provider interface for NeoEssentials API
 * Allows third-party plugins to implement custom economy systems
 * 
 * @author ZeroG
 * @since 2.1.0
 */
public interface IEconomyProvider {
    
    /**
     * Get the name of this economy provider
     * @return Provider name
     */
    String getProviderName();
    
    /**
     * Get the version of this economy provider
     * @return Provider version
     */
    String getProviderVersion();
    
    /**
     * Check if this provider is enabled and ready
     * @return true if provider is ready to handle transactions
     */
    boolean isEnabled();
    
    /**
     * Get the currency name (singular)
     * @return Currency name (e.g., "Dollar")
     */
    String getCurrencyNameSingular();
    
    /**
     * Get the currency name (plural)
     * @return Currency name (e.g., "Dollars")
     */
    String getCurrencyNamePlural();
    
    /**
     * Get the currency symbol
     * @return Currency symbol (e.g., "$")
     */
    String getCurrencySymbol();
    
    /**
     * Check if fractional currency is supported
     * @return true if fractional amounts are supported
     */
    boolean supportsFractionalCurrency();
    
    /**
     * Get player's current balance
     * @param playerUuid Player's UUID
     * @return Current balance, or BigDecimal.ZERO if player not found
     */
    BigDecimal getBalance(UUID playerUuid);
    
    /**
     * Check if player has at least the specified amount
     * @param playerUuid Player's UUID
     * @param amount Amount to check
     * @return true if player has sufficient funds
     */
    boolean hasBalance(UUID playerUuid, BigDecimal amount);
    
    /**
     * Withdraw money from player's account
     * @param playerUuid Player's UUID
     * @param amount Amount to withdraw
     * @param reason Reason for transaction (for logging)
     * @return true if transaction was successful
     */
    boolean withdraw(UUID playerUuid, BigDecimal amount, String reason);
    
    /**
     * Deposit money to player's account
     * @param playerUuid Player's UUID
     * @param amount Amount to deposit
     * @param reason Reason for transaction (for logging)
     * @return true if transaction was successful
     */
    boolean deposit(UUID playerUuid, BigDecimal amount, String reason);
    
    /**
     * Transfer money between players
     * @param fromUuid Source player's UUID
     * @param toUuid Target player's UUID
     * @param amount Amount to transfer
     * @param reason Reason for transaction
     * @return true if transaction was successful
     */
    boolean transfer(UUID fromUuid, UUID toUuid, BigDecimal amount, String reason);
    
    /**
     * Set player's balance
     * @param playerUuid Player's UUID
     * @param amount New balance amount
     * @param reason Reason for change
     * @return true if operation was successful
     */
    boolean setBalance(UUID playerUuid, BigDecimal amount, String reason);
    
    /**
     * Format currency amount to string
     * @param amount Amount to format
     * @return Formatted currency string
     */
    String formatCurrency(BigDecimal amount);
    
    /**
     * Get transaction history for player
     * @param playerUuid Player's UUID
     * @param limit Maximum number of transactions to return
     * @return List of transaction records
     */
    List<TransactionRecord> getTransactionHistory(UUID playerUuid, int limit);
    
    /**
     * Get top players by balance
     * @param limit Maximum number of players to return
     * @return List of balance records
     */
    List<BalanceRecord> getTopBalances(int limit);
    
    /**
     * Check if player account exists
     * @param playerUuid Player's UUID
     * @return true if account exists
     */
    boolean hasAccount(UUID playerUuid);
    
    /**
     * Create player account
     * @param playerUuid Player's UUID
     * @param playerName Player's name
     * @return true if account was created successfully
     */
    boolean createAccount(UUID playerUuid, String playerName);
    
    /**
     * Delete player account
     * @param playerUuid Player's UUID
     * @return true if account was deleted successfully
     */
    boolean deleteAccount(UUID playerUuid);
    
    /**
     * Transaction record for history tracking
     */
    record TransactionRecord(
        long timestamp,
        String type,
        BigDecimal amount,
        BigDecimal balanceBefore,
        BigDecimal balanceAfter,
        String reason,
        UUID relatedPlayer
    ) {}
    
    /**
     * Balance record for rankings
     */
    record BalanceRecord(
        UUID playerUuid,
        String playerName,
        BigDecimal balance,
        int rank
    ) {}
}
