package com.zerog.neoessentials.economy;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages all transactions in the NeoEssentials economy system.
 * Provides transaction recording, querying, and analytics capabilities.
 */
public class TransactionManager {
    private final Map<UUID, Transaction> allTransactions;
    private final Map<UUID, List<UUID>> playerTransactions; // Player ID -> List of Transaction IDs
    private final Map<Transaction.TransactionType, List<UUID>> transactionsByType;
    private final Map<String, List<UUID>> transactionsByCurrency; // Currency ID -> List of Transaction IDs
    
    // Transaction limits and settings
    private final int maxTransactionHistoryDays;
    private final long cleanupInterval;
    private long lastCleanup;
    
    public TransactionManager() {
        this.allTransactions = new ConcurrentHashMap<>();
        this.playerTransactions = new ConcurrentHashMap<>();
        this.transactionsByType = new ConcurrentHashMap<>();
        this.transactionsByCurrency = new ConcurrentHashMap<>();
        this.maxTransactionHistoryDays = 365; // Keep 1 year of history
        this.cleanupInterval = 24 * 60 * 60 * 1000; // Daily cleanup
        this.lastCleanup = System.currentTimeMillis();
    }
    
    /**
     * Record a new transaction
     * 
     * @param transaction The transaction to record
     */
    public void recordTransaction(Transaction transaction) {
        if (transaction == null) return;
        
        // Store the transaction
        allTransactions.put(transaction.getTransactionId(), transaction);
        
        // Index by players
        if (transaction.getFromPlayer() != null) {
            playerTransactions.computeIfAbsent(transaction.getFromPlayer(), k -> new ArrayList<>())
                             .add(transaction.getTransactionId());
        }
        if (transaction.getToPlayer() != null) {
            playerTransactions.computeIfAbsent(transaction.getToPlayer(), k -> new ArrayList<>())
                             .add(transaction.getTransactionId());
        }
        
        // Index by type
        transactionsByType.computeIfAbsent(transaction.getType(), k -> new ArrayList<>())
                         .add(transaction.getTransactionId());
        
        // Index by currency
        String currencyId = transaction.getCurrency().getId();
        transactionsByCurrency.computeIfAbsent(currencyId, k -> new ArrayList<>())
                             .add(transaction.getTransactionId());
        
        // Periodic cleanup
        performPeriodicCleanup();
    }
    
    /**
     * Get a transaction by ID
     * 
     * @param transactionId The transaction ID
     * @return The transaction, or null if not found
     */
    public Transaction getTransaction(UUID transactionId) {
        return allTransactions.get(transactionId);
    }
    
    /**
     * Get all transactions for a player within a time period
     * 
     * @param playerId The player's UUID
     * @param days Number of days to look back
     * @return List of transactions
     */
    public List<Transaction> getPlayerTransactions(UUID playerId, int days) {
        List<UUID> transactionIds = playerTransactions.getOrDefault(playerId, new ArrayList<>());
        long cutoffTime = System.currentTimeMillis() - (days * 24L * 60L * 60L * 1000L);
        
        return transactionIds.stream()
                .map(allTransactions::get)
                .filter(Objects::nonNull)
                .filter(transaction -> transaction.getTimestamp() >= cutoffTime)
                .sorted((t1, t2) -> Long.compare(t2.getTimestamp(), t1.getTimestamp()))
                .collect(Collectors.toList());
    }
    
    /**
     * Get transactions by type within a time period
     * 
     * @param type The transaction type
     * @param days Number of days to look back
     * @return List of transactions
     */
    public List<Transaction> getTransactionsByType(Transaction.TransactionType type, int days) {
        List<UUID> transactionIds = transactionsByType.getOrDefault(type, new ArrayList<>());
        long cutoffTime = System.currentTimeMillis() - (days * 24L * 60L * 60L * 1000L);
        
        return transactionIds.stream()
                .map(allTransactions::get)
                .filter(Objects::nonNull)
                .filter(transaction -> transaction.getTimestamp() >= cutoffTime)
                .sorted((t1, t2) -> Long.compare(t2.getTimestamp(), t1.getTimestamp()))
                .collect(Collectors.toList());
    }
    
    /**
     * Get transactions by currency within a time period
     * 
     * @param currency The currency
     * @param days Number of days to look back
     * @return List of transactions
     */
    public List<Transaction> getTransactionsByCurrency(Currency currency, int days) {
        List<UUID> transactionIds = transactionsByCurrency.getOrDefault(currency.getId(), new ArrayList<>());
        long cutoffTime = System.currentTimeMillis() - (days * 24L * 60L * 60L * 1000L);
        
        return transactionIds.stream()
                .map(allTransactions::get)
                .filter(Objects::nonNull)
                .filter(transaction -> transaction.getTimestamp() >= cutoffTime)
                .sorted((t1, t2) -> Long.compare(t2.getTimestamp(), t1.getTimestamp()))
                .collect(Collectors.toList());
    }
    
    /**
     * Calculate total transaction volume for a player
     * 
     * @param playerId The player's UUID
     * @param currency The currency to check
     * @param days Number of days to look back
     * @return Total transaction volume
     */
    public double getPlayerTransactionVolume(UUID playerId, Currency currency, int days) {
        return getPlayerTransactions(playerId, days).stream()
                .filter(transaction -> transaction.getCurrency().equals(currency))
                .mapToDouble(transaction -> Math.abs(transaction.getAmount()))
                .sum();
    }
    
    /**
     * Calculate net income for a player
     * 
     * @param playerId The player's UUID
     * @param currency The currency to check
     * @param days Number of days to look back
     * @return Net income (positive = gained money, negative = lost money)
     */
    public double getPlayerNetIncome(UUID playerId, Currency currency, int days) {
        return getPlayerTransactions(playerId, days).stream()
                .filter(transaction -> transaction.getCurrency().equals(currency))
                .mapToDouble(transaction -> transaction.getEffectiveAmount(playerId))
                .sum();
    }
    
    /**
     * Get spending breakdown by transaction type for a player
     * 
     * @param playerId The player's UUID
     * @param currency The currency to check
     * @param days Number of days to look back
     * @return Map of transaction type to total amount
     */
    public Map<Transaction.TransactionType, Double> getPlayerSpendingBreakdown(UUID playerId, Currency currency, int days) {
        Map<Transaction.TransactionType, Double> breakdown = new HashMap<>();
        
        getPlayerTransactions(playerId, days).stream()
                .filter(transaction -> transaction.getCurrency().equals(currency))
                .forEach(transaction -> {
                    double effectiveAmount = transaction.getEffectiveAmount(playerId);
                    if (effectiveAmount < 0) { // Only count expenses
                        breakdown.merge(transaction.getType(), Math.abs(effectiveAmount), Double::sum);
                    }
                });
        
        return breakdown;
    }
    
    /**
     * Get total economic activity (all transactions) within a time period
     * 
     * @param currency The currency to check
     * @param days Number of days to look back
     * @return Total transaction volume
     */
    public double getTotalEconomicActivity(Currency currency, int days) {
        long cutoffTime = System.currentTimeMillis() - (days * 24L * 60L * 60L * 1000L);
        
        return allTransactions.values().stream()
                .filter(transaction -> transaction.getTimestamp() >= cutoffTime)
                .filter(transaction -> transaction.getCurrency().equals(currency))
                .mapToDouble(transaction -> Math.abs(transaction.getAmount()))
                .sum();
    }
    
    /**
     * Get the most active players by transaction count
     * 
     * @param days Number of days to look back
     * @param limit Number of players to return
     * @return Ordered list of player UUIDs and transaction counts
     */
    public List<Map.Entry<UUID, Integer>> getMostActiveTraders(int days, int limit) {
        long cutoffTime = System.currentTimeMillis() - (days * 24L * 60L * 60L * 1000L);
        Map<UUID, Integer> playerTransactionCounts = new HashMap<>();
        
        allTransactions.values().stream()
                .filter(transaction -> transaction.getTimestamp() >= cutoffTime)
                .forEach(transaction -> {
                    if (transaction.getFromPlayer() != null) {
                        playerTransactionCounts.merge(transaction.getFromPlayer(), 1, Integer::sum);
                    }
                    if (transaction.getToPlayer() != null) {
                        playerTransactionCounts.merge(transaction.getToPlayer(), 1, Integer::sum);
                    }
                });
        
        return playerTransactionCounts.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    /**
     * Search transactions by description
     * 
     * @param searchTerm The search term
     * @param days Number of days to look back
     * @param limit Maximum number of results
     * @return List of matching transactions
     */
    public List<Transaction> searchTransactions(String searchTerm, int days, int limit) {
        long cutoffTime = System.currentTimeMillis() - (days * 24L * 60L * 60L * 1000L);
        String lowerSearchTerm = searchTerm.toLowerCase();
        
        return allTransactions.values().stream()
                .filter(transaction -> transaction.getTimestamp() >= cutoffTime)
                .filter(transaction -> transaction.getDescription().toLowerCase().contains(lowerSearchTerm))
                .sorted((t1, t2) -> Long.compare(t2.getTimestamp(), t1.getTimestamp()))
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    /**
     * Perform periodic cleanup of old transactions
     */
    private void performPeriodicCleanup() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCleanup < cleanupInterval) {
            return;
        }
        
        long cutoffTime = currentTime - (maxTransactionHistoryDays * 24L * 60L * 60L * 1000L);
        List<UUID> toRemove = new ArrayList<>();
        
        // Find old transactions
        for (Map.Entry<UUID, Transaction> entry : allTransactions.entrySet()) {
            if (entry.getValue().getTimestamp() < cutoffTime) {
                toRemove.add(entry.getKey());
            }
        }
        
        // Remove old transactions
        for (UUID transactionId : toRemove) {
            Transaction transaction = allTransactions.remove(transactionId);
            if (transaction != null) {
                removeFromIndexes(transaction);
            }
        }
        
        lastCleanup = currentTime;
    }
    
    /**
     * Remove transaction from all indexes
     * 
     * @param transaction The transaction to remove
     */
    private void removeFromIndexes(Transaction transaction) {
        UUID transactionId = transaction.getTransactionId();
        
        // Remove from player indexes
        if (transaction.getFromPlayer() != null) {
            List<UUID> playerTxns = playerTransactions.get(transaction.getFromPlayer());
            if (playerTxns != null) {
                playerTxns.remove(transactionId);
            }
        }
        if (transaction.getToPlayer() != null) {
            List<UUID> playerTxns = playerTransactions.get(transaction.getToPlayer());
            if (playerTxns != null) {
                playerTxns.remove(transactionId);
            }
        }
        
        // Remove from type index
        List<UUID> typeTxns = transactionsByType.get(transaction.getType());
        if (typeTxns != null) {
            typeTxns.remove(transactionId);
        }
        
        // Remove from currency index
        List<UUID> currencyTxns = transactionsByCurrency.get(transaction.getCurrency().getId());
        if (currencyTxns != null) {
            currencyTxns.remove(transactionId);
        }
    }
    
    /**
     * Get transaction statistics
     * 
     * @return Map of statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalTransactions", allTransactions.size());
        stats.put("totalPlayers", playerTransactions.size());
        stats.put("transactionTypes", transactionsByType.size());
        stats.put("currencies", transactionsByCurrency.size());
        return stats;
    }

    /**
     * Get total transaction count
     * 
     * @return Total number of transactions
     */
    public long getTotalTransactionCount() {
        return allTransactions.size();
    }

    /**
     * Get total transaction volume across all currencies
     * 
     * @return Total transaction volume
     */
    public double getTotalTransactionVolume() {
        return allTransactions.values().stream()
                .mapToDouble(transaction -> Math.abs(transaction.getAmount()))
                .sum();
    }

    /**
     * Get daily transaction volume (last 24 hours)
     * 
     * @return Daily transaction volume
     */
    public double getDailyTransactionVolume() {
        long cutoffTime = System.currentTimeMillis() - (24L * 60L * 60L * 1000L);
        return allTransactions.values().stream()
                .filter(transaction -> transaction.getTimestamp() >= cutoffTime)
                .mapToDouble(transaction -> Math.abs(transaction.getAmount()))
                .sum();
    }

    /**
     * Get hourly transaction count (last hour)
     * 
     * @return Hourly transaction count
     */
    public int getHourlyTransactionCount() {
        long cutoffTime = System.currentTimeMillis() - (60L * 60L * 1000L);
        return (int) allTransactions.values().stream()
                .filter(transaction -> transaction.getTimestamp() >= cutoffTime)
                .count();
    }

    /**
     * Get hourly transaction volume (last hour)
     * 
     * @return Hourly transaction volume
     */
    public double getHourlyTransactionVolume() {
        long cutoffTime = System.currentTimeMillis() - (60L * 60L * 1000L);
        return allTransactions.values().stream()
                .filter(transaction -> transaction.getTimestamp() >= cutoffTime)
                .mapToDouble(transaction -> Math.abs(transaction.getAmount()))
                .sum();
    }

    /**
     * Get recent transactions
     * 
     * @param limit Maximum number of transactions to return
     * @return List of recent transactions
     */
    public List<Transaction> getRecentTransactions(int limit) {
        return allTransactions.values().stream()
                .sorted((t1, t2) -> Long.compare(t2.getTimestamp(), t1.getTimestamp()))
                .limit(limit)
                .collect(Collectors.toList());
    }
}
