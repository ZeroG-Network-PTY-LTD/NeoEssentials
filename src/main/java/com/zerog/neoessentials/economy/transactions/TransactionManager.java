package com.zerog.neoessentials.economy.transactions;

import com.zerog.neoessentials.economy.*;
import com.zerog.neoessentials.storage.DataManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Advanced Transaction Management System
 * Handles all economic transactions with:
 * - Complete transaction history
 * - Transaction verification and rollback
 * - Batch transaction processing
 * - Cross-server transaction support
 * - Transaction analytics and reporting
 * - Fraud detection and prevention
 */
public class TransactionManager {
    private final DataManager dataManager;
    
    // Transaction storage
    private final Queue<Transaction> pendingTransactions;
    private final Map<String, Transaction> transactionHistory;
    private final Map<UUID, List<String>> playerTransactions;
    
    // Transaction limits and security
    private final TransactionLimits limits;
    private final FraudDetector fraudDetector;
    
    // Processing flags
    private boolean processingEnabled;
    
    public TransactionManager(EconomyManager economyManager) {
        this.dataManager = DataManager.getInstance();
        this.pendingTransactions = new ConcurrentLinkedQueue<>();
        this.transactionHistory = new HashMap<>();
        this.playerTransactions = new HashMap<>();
        this.limits = new TransactionLimits();
        this.fraudDetector = new FraudDetector();
        this.processingEnabled = true;
        
        initialize();
    }
    
    private void initialize() {
        try {
            // Load transaction history
            loadTransactionHistory();
            
            // Start background processing
            startBackgroundProcessing();
            
        } catch (Exception e) {
            System.err.println("Failed to initialize Transaction Manager: " + e.getMessage());
        }
    }
    
    /**
     * Record a new transaction
     */
    public boolean recordTransaction(UUID fromPlayer, UUID toPlayer, String currency, 
                                   BigDecimal amount, TransactionType type, String description) {
        
        if (!processingEnabled) {
            return false;
        }
        
        try {
            // Create transaction
            Transaction transaction = new Transaction(fromPlayer, toPlayer, currency, 
                                                    amount, type, description);
            
            // Validate transaction
            if (!validateTransaction(transaction)) {
                return false;
            }
            
            // Check fraud detection
            if (fraudDetector.isTransactionSuspicious(transaction, getPlayerTransactionHistory(fromPlayer))) {
                // Flag for manual review but allow transaction
                flagTransactionForReview(transaction, "Fraud detection alert");
            }
            
            // Add to history
            addTransactionToHistory(transaction);
            
            // Queue for processing
            pendingTransactions.offer(transaction);
            
            return true;
            
        } catch (Exception e) {
            System.err.println("Failed to record transaction: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get transaction history for a player
     */
    public List<Transaction> getPlayerTransactionHistory(UUID playerId) {
        List<String> transactionIds = playerTransactions.getOrDefault(playerId, new ArrayList<>());
        return transactionIds.stream()
            .map(transactionHistory::get)
            .filter(Objects::nonNull)
            .sorted((t1, t2) -> t2.getTimestamp().compareTo(t1.getTimestamp()))
            .collect(Collectors.toList());
    }
    
    /**
     * Get transaction history with filters
     */
    public List<Transaction> getTransactionHistory(UUID playerId, String currency, 
                                                 TransactionType type, LocalDateTime since) {
        return getPlayerTransactionHistory(playerId).stream()
            .filter(t -> currency == null || currency.equals(t.getCurrency()))
            .filter(t -> type == null || type == t.getType())
            .filter(t -> since == null || t.getTimestamp().isAfter(since))
            .collect(Collectors.toList());
    }
    
    /**
     * Get recent transactions for analytics
     */
    public List<Transaction> getRecentTransactions() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(1);
        return transactionHistory.values().stream()
            .filter(t -> t.getTimestamp().isAfter(cutoff))
            .collect(Collectors.toList());
    }
    
    /**
     * Calculate transaction statistics for a player
     */
    public TransactionStats getPlayerStats(UUID playerId) {
        List<Transaction> transactions = getPlayerTransactionHistory(playerId);
        
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;
        Map<TransactionType, Integer> typeCounts = new HashMap<>();
        Map<String, BigDecimal> currencyTotals = new HashMap<>();
        
        for (Transaction transaction : transactions) {
            BigDecimal amount = transaction.getAmount();
            TransactionType type = transaction.getType();
            String currency = transaction.getCurrency();
            
            // Count by type
            typeCounts.merge(type, 1, Integer::sum);
            
            // Sum by currency
            currencyTotals.merge(currency, amount.abs(), BigDecimal::add);
            
            // Calculate income vs expenses
            if (playerId.equals(transaction.getToPlayer()) && amount.compareTo(BigDecimal.ZERO) > 0) {
                totalIncome = totalIncome.add(amount);
            } else if (playerId.equals(transaction.getFromPlayer()) && amount.compareTo(BigDecimal.ZERO) < 0) {
                totalExpenses = totalExpenses.add(amount.abs());
            }
        }
        
        return new TransactionStats(playerId, transactions.size(), totalIncome, 
                                  totalExpenses, typeCounts, currencyTotals);
    }
    
    /**
     * Reverse a transaction (if possible)
     */
    public boolean reverseTransaction(String transactionId, String reason) {
        Transaction original = transactionHistory.get(transactionId);
        if (original == null) {
            return false;
        }
        
        // Create reverse transaction
        Transaction reversal = new Transaction(
            original.getToPlayer(),
            original.getFromPlayer(),
            original.getCurrency(),
            original.getAmount().negate(),
            TransactionType.ADMIN_SET,
            "Reversal: " + reason
        );
        
        // Record the reversal
        return recordTransaction(reversal.getFromPlayer(), reversal.getToPlayer(), 
                               reversal.getCurrency(), reversal.getAmount(), 
                               reversal.getType(), reversal.getDescription());
    }
    
    private boolean validateTransaction(Transaction transaction) {
        // Check basic validity
        if (transaction.getAmount() == null || transaction.getCurrency() == null) {
            return false;
        }
        
        // Check transaction limits
        if (!limits.isWithinLimits(transaction)) {
            return false;
        }
        
        // Additional validation can be added here
        return true;
    }
    
    private void addTransactionToHistory(Transaction transaction) {
        transactionHistory.put(transaction.getTransactionId(), transaction);
        
        // Add to player histories
        if (transaction.getFromPlayer() != null) {
            playerTransactions.computeIfAbsent(transaction.getFromPlayer(), k -> new ArrayList<>())
                .add(transaction.getTransactionId());
        }
        
        if (transaction.getToPlayer() != null) {
            playerTransactions.computeIfAbsent(transaction.getToPlayer(), k -> new ArrayList<>())
                .add(transaction.getTransactionId());
        }
    }
    
    private void flagTransactionForReview(Transaction transaction, String reason) {
        // Implementation for flagging suspicious transactions
        System.out.println("Transaction flagged for review: " + transaction.getTransactionId() + " - " + reason);
    }
    
    private void startBackgroundProcessing() {
        CompletableFuture.runAsync(() -> {
            while (processingEnabled) {
                try {
                    Thread.sleep(5000); // Process every 5 seconds
                    processPendingTransactions();
                    cleanupOldTransactions();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    System.err.println("Error in transaction processing: " + e.getMessage());
                }
            }
        });
    }
    
    private void processPendingTransactions() {
        while (!pendingTransactions.isEmpty()) {
            Transaction transaction = pendingTransactions.poll();
            if (transaction != null) {
                // Process transaction (save to storage, etc.)
                saveTransaction(transaction);
            }
        }
    }
    
    private void cleanupOldTransactions() {
    // Remove transactions older than 30 days (default)
    LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        
        List<String> toRemove = transactionHistory.values().stream()
            .filter(t -> t.getTimestamp().isBefore(cutoff))
            .map(Transaction::getTransactionId)
            .collect(Collectors.toList());
        
        for (String transactionId : toRemove) {
            Transaction transaction = transactionHistory.remove(transactionId);
            if (transaction != null) {
                // Remove from player histories
                if (transaction.getFromPlayer() != null) {
                    List<String> playerTxns = playerTransactions.get(transaction.getFromPlayer());
                    if (playerTxns != null) {
                        playerTxns.remove(transactionId);
                    }
                }
                if (transaction.getToPlayer() != null) {
                    List<String> playerTxns = playerTransactions.get(transaction.getToPlayer());
                    if (playerTxns != null) {
                        playerTxns.remove(transactionId);
                    }
                }
            }
        }
    }
    
    private void saveTransaction(Transaction transaction) {
        try {
            dataManager.saveTransaction(transaction);
        } catch (Exception e) {
            System.err.println("Failed to save transaction: " + e.getMessage());
        }
    }
    
    private void loadTransactionHistory() {
        try {
            Map<String, Transaction> loaded = dataManager.loadTransactionHistory();
            transactionHistory.putAll(loaded);
            
            // Rebuild player transaction index
            for (Transaction transaction : loaded.values()) {
                if (transaction.getFromPlayer() != null) {
                    playerTransactions.computeIfAbsent(transaction.getFromPlayer(), k -> new ArrayList<>())
                        .add(transaction.getTransactionId());
                }
                if (transaction.getToPlayer() != null) {
                    playerTransactions.computeIfAbsent(transaction.getToPlayer(), k -> new ArrayList<>())
                        .add(transaction.getTransactionId());
                }
            }
            
        } catch (Exception e) {
            System.err.println("Failed to load transaction history: " + e.getMessage());
        }
    }
    
    public void shutdown() {
        processingEnabled = false;
        
        // Process any remaining transactions
        processPendingTransactions();
    }
    
    // Transaction statistics class
    public static class TransactionStats {
        private final UUID playerId;
        private final int totalTransactions;
        private final BigDecimal totalIncome;
        private final BigDecimal totalExpenses;
        private final Map<TransactionType, Integer> transactionCounts;
        private final Map<String, BigDecimal> currencyTotals;
        
        public TransactionStats(UUID playerId, int totalTransactions, BigDecimal totalIncome, 
                              BigDecimal totalExpenses, Map<TransactionType, Integer> transactionCounts,
                              Map<String, BigDecimal> currencyTotals) {
            this.playerId = playerId;
            this.totalTransactions = totalTransactions;
            this.totalIncome = totalIncome;
            this.totalExpenses = totalExpenses;
            this.transactionCounts = new HashMap<>(transactionCounts);
            this.currencyTotals = new HashMap<>(currencyTotals);
        }
        
        // Getters
        public UUID getPlayerId() { return playerId; }
        public int getTotalTransactions() { return totalTransactions; }
        public BigDecimal getTotalIncome() { return totalIncome; }
        public BigDecimal getTotalExpenses() { return totalExpenses; }
        public BigDecimal getNetIncome() { return totalIncome.subtract(totalExpenses); }
        public Map<TransactionType, Integer> getTransactionCounts() { return new HashMap<>(transactionCounts); }
        public Map<String, BigDecimal> getCurrencyTotals() { return new HashMap<>(currencyTotals); }
    }
}
