package com.zerog.neoessentials.economy.storage;

import com.zerog.neoessentials.economy.EconomyAccount;
import com.zerog.neoessentials.economy.Transaction;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Interface for economy data storage.
 * Provides methods for persisting economy data across different storage backends.
 */
public interface EconomyStorage {
    
    /**
     * Initializes the storage system
     * @return true if initialization was successful
     */
    boolean initialize();
    
    /**
     * Closes the storage system and cleans up resources
     */
    void close();
    
    /**
     * Saves an economy account
     * @param account the account to save
     * @return true if save was successful
     */
    boolean saveAccount(EconomyAccount account);
    
    /**
     * Loads an economy account
     * @param playerId the player's UUID
     * @return the account if found, empty if not found
     */
    Optional<EconomyAccount> loadAccount(UUID playerId);
    
    /**
     * Deletes an economy account
     * @param playerId the player's UUID
     * @return true if deletion was successful
     */
    boolean deleteAccount(UUID playerId);
    
    /**
     * Checks if an account exists
     * @param playerId the player's UUID
     * @return true if account exists
     */
    boolean accountExists(UUID playerId);
    
    /**
     * Gets all accounts (for admin purposes)
     * @return list of all accounts
     */
    List<EconomyAccount> getAllAccounts();
    
    /**
     * Logs a transaction
     * @param transaction the transaction to log
     * @return true if logging was successful
     */
    boolean logTransaction(Transaction transaction);
    
    /**
     * Gets transaction history for a player
     * @param playerId the player's UUID
     * @param limit maximum number of transactions to return
     * @return list of transactions
     */
    List<Transaction> getTransactionHistory(UUID playerId, int limit);
    
    /**
     * Gets transaction history for a player within a date range
     * @param playerId the player's UUID
     * @param fromTimestamp start timestamp (epoch millis)
     * @param toTimestamp end timestamp (epoch millis)
     * @param limit maximum number of transactions to return
     * @return list of transactions
     */
    List<Transaction> getTransactionHistory(UUID playerId, long fromTimestamp, long toTimestamp, int limit);
    
    /**
     * Gets a specific transaction by ID
     * @param transactionId the transaction ID
     * @return the transaction if found, empty if not found
     */
    Optional<Transaction> getTransaction(UUID transactionId);
    
    /**
     * Gets all transactions (for admin purposes)
     * @param limit maximum number of transactions to return
     * @return list of transactions
     */
    List<Transaction> getAllTransactions(int limit);
    
    /**
     * Performs a backup of the economy data
     * @return true if backup was successful
     */
    boolean backup();
    
    /**
     * Restores from a backup
     * @param backupName the name of the backup to restore
     * @return true if restore was successful
     */
    boolean restore(String backupName);
    
    /**
     * Gets available backup names
     * @return list of available backup names
     */
    List<String> getAvailableBackups();
    
    /**
     * Performs database maintenance operations
     * @return true if maintenance was successful
     */
    boolean performMaintenance();
    
    /**
     * Gets storage statistics
     * @return storage statistics
     */
    StorageStatistics getStatistics();
    
    /**
     * Validates the integrity of stored data
     * @return validation report
     */
    StorageValidationReport validateData();
}
