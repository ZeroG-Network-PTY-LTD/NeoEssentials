package com.zerog.neoessentials.economy;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Central economy manager that coordinates all economic activities in NeoEssentials.
 * Handles player balances, transactions, banking, shops, and economic analytics.
 */
public class EconomyManager {
    private static EconomyManager instance;
    
    // Core managers
    private final CurrencyManager currencyManager;
    private final BankManager bankManager;
    private final TransactionManager transactionManager;
    private final ShopManager shopManager;
    private final EconomicAnalytics analytics;
    
    // Player data
    private final Map<UUID, PlayerEconomyData> playerData;
    
    // Economy settings
    private boolean economyEnabled;
    private double startingBalance;
    private boolean negativeBalancesAllowed;
    private double maxBalance;
    private double inflationRate;
    
    // Scheduled tasks
    private final ScheduledExecutorService scheduler;
    
    private EconomyManager() {
        this.currencyManager = CurrencyManager.getInstance();
        this.bankManager = BankManager.getInstance();
        this.transactionManager = new TransactionManager();
        this.shopManager = new ShopManager();
        this.analytics = new EconomicAnalytics();
        this.playerData = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(2);
        
        initializeDefaults();
        startScheduledTasks();
    }
    
    public static EconomyManager getInstance() {
        if (instance == null) {
            instance = new EconomyManager();
        }
        return instance;
    }
    
    /**
     * Initialize default economy settings
     */
    private void initializeDefaults() {
        this.economyEnabled = true;
        this.startingBalance = 100.0;
        this.negativeBalancesAllowed = false;
        this.maxBalance = 1000000.0;
        this.inflationRate = 0.02; // 2% annual inflation
    }
    
    /**
     * Start scheduled economy tasks
     */
    private void startScheduledTasks() {
        // Daily interest calculation
        scheduler.scheduleAtFixedRate(
            () -> bankManager.calculateInterestForAllAccounts(),
            1, 24, TimeUnit.HOURS
        );
        
        // Economic analytics update
        scheduler.scheduleAtFixedRate(
            () -> analytics.updateEconomicMetrics(),
            0, 1, TimeUnit.HOURS
        );
    }
    
    /**
     * Get or create player economy data
     * 
     * @param playerId The player's UUID
     * @return The player's economy data
     */
    public PlayerEconomyData getPlayerData(UUID playerId) {
        return playerData.computeIfAbsent(playerId, id -> {
            PlayerEconomyData data = new PlayerEconomyData(id);
            
            // Set starting balance in default currency
            Currency defaultCurrency = currencyManager.getDefaultCurrency();
            if (defaultCurrency != null) {
                data.setBalance(defaultCurrency, startingBalance);
            }
            
            // Create primary bank account
            bankManager.getPrimaryAccount(playerId);
            
            return data;
        });
    }
    
    /**
     * Get player balance in a specific currency
     * 
     * @param playerId The player's UUID
     * @param currency The currency
     * @return The player's balance
     */
    public double getBalance(UUID playerId, Currency currency) {
        return getPlayerData(playerId).getBalance(currency);
    }
    
    /**
     * Set player balance in a specific currency
     * 
     * @param playerId The player's UUID
     * @param currency The currency
     * @param amount The new balance
     * @return true if successful
     */
    public boolean setBalance(UUID playerId, Currency currency, double amount) {
        if (!economyEnabled) return false;
        if (!negativeBalancesAllowed && amount < 0) return false;
        if (amount > maxBalance) return false;
        
        PlayerEconomyData data = getPlayerData(playerId);
        double oldBalance = data.getBalance(currency);
        data.setBalance(currency, amount);
        
        // Record transaction
        Transaction transaction = new Transaction(
            UUID.randomUUID(),
            playerId,
            null,
            amount - oldBalance,
            currency,
            "Balance set by admin",
            Transaction.TransactionType.ADMIN,
            System.currentTimeMillis()
        );
        transactionManager.recordTransaction(transaction);
        
        return true;
    }
    
    /**
     * Add money to a player's balance
     * 
     * @param playerId The player's UUID
     * @param currency The currency
     * @param amount The amount to add
     * @param reason Reason for the addition
     * @return true if successful
     */
    public boolean addBalance(UUID playerId, Currency currency, double amount, String reason) {
        if (!economyEnabled || amount <= 0) return false;
        
        PlayerEconomyData data = getPlayerData(playerId);
        double currentBalance = data.getBalance(currency);
        double newBalance = currentBalance + amount;
        
        if (newBalance > maxBalance) return false;
        
        data.setBalance(currency, newBalance);
        
        // Record transaction
        Transaction transaction = new Transaction(
            UUID.randomUUID(),
            playerId,
            null,
            amount,
            currency,
            reason,
            Transaction.TransactionType.DEPOSIT,
            System.currentTimeMillis()
        );
        transactionManager.recordTransaction(transaction);
        
        return true;
    }
    
    /**
     * Remove money from a player's balance
     * 
     * @param playerId The player's UUID
     * @param currency The currency
     * @param amount The amount to remove
     * @param reason Reason for the removal
     * @return true if successful
     */
    public boolean removeBalance(UUID playerId, Currency currency, double amount, String reason) {
        if (!economyEnabled || amount <= 0) return false;
        
        PlayerEconomyData data = getPlayerData(playerId);
        double currentBalance = data.getBalance(currency);
        double newBalance = currentBalance - amount;
        
        if (!negativeBalancesAllowed && newBalance < 0) return false;
        
        data.setBalance(currency, newBalance);
        
        // Record transaction
        Transaction transaction = new Transaction(
            UUID.randomUUID(),
            playerId,
            null,
            -amount,
            currency,
            reason,
            Transaction.TransactionType.WITHDRAWAL,
            System.currentTimeMillis()
        );
        transactionManager.recordTransaction(transaction);
        
        return true;
    }
    
    /**
     * Transfer money between players
     * 
     * @param fromPlayer Source player
     * @param toPlayer Destination player
     * @param amount Amount to transfer
     * @param currency Currency to transfer
     * @param reason Reason for transfer
     * @return true if successful
     */
    public boolean transferMoney(UUID fromPlayer, UUID toPlayer, double amount, 
                               Currency currency, String reason) {
        if (!economyEnabled || amount <= 0) return false;
        if (fromPlayer.equals(toPlayer)) return false;
        
        PlayerEconomyData fromData = getPlayerData(fromPlayer);
        PlayerEconomyData toData = getPlayerData(toPlayer);
        
        double fromBalance = fromData.getBalance(currency);
        double toBalance = toData.getBalance(currency);
        
        // Check if sender has sufficient funds
        if (fromBalance < amount) return false;
        
        // Check if recipient can receive the money
        if (toBalance + amount > maxBalance) return false;
        
        // Perform transfer
        fromData.setBalance(currency, fromBalance - amount);
        toData.setBalance(currency, toBalance + amount);
        
        // Record transactions
        Transaction fromTransaction = new Transaction(
            UUID.randomUUID(),
            fromPlayer,
            toPlayer,
            -amount,
            currency,
            "Transfer to player: " + reason,
            Transaction.TransactionType.TRANSFER_OUT,
            System.currentTimeMillis()
        );
        
        Transaction toTransaction = new Transaction(
            UUID.randomUUID(),
            toPlayer,
            fromPlayer,
            amount,
            currency,
            "Transfer from player: " + reason,
            Transaction.TransactionType.TRANSFER_IN,
            System.currentTimeMillis()
        );
        
        transactionManager.recordTransaction(fromTransaction);
        transactionManager.recordTransaction(toTransaction);
        
        return true;
    }
    
    /**
     * Get the top balances for a currency
     * 
     * @param currency The currency to check
     * @param limit Number of top players to return
     * @return Ordered list of player UUIDs and balances
     */
    public List<Map.Entry<UUID, Double>> getTopBalances(Currency currency, int limit) {
        return playerData.entrySet().stream()
                .map(entry -> Map.entry(entry.getKey(), entry.getValue().getBalance(currency)))
                .filter(entry -> entry.getValue() > 0)
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .limit(limit)
                .toList();
    }
    
    /**
     * Get player transaction history
     * 
     * @param playerId The player's UUID
     * @param days Number of days of history
     * @return List of transactions
     */
    public List<Transaction> getPlayerTransactionHistory(UUID playerId, int days) {
        return transactionManager.getPlayerTransactions(playerId, days);
    }
    
    /**
     * Calculate total money in circulation for a currency
     * 
     * @param currency The currency
     * @return Total amount in circulation
     */
    public double getTotalMoneyInCirculation(Currency currency) {
        double total = 0.0;
        
        // Add all player balances
        for (PlayerEconomyData data : playerData.values()) {
            total += data.getBalance(currency);
        }
        
        // Add all bank account balances
        for (List<BankAccount> accounts : bankManager.playerAccounts.values()) {
            for (BankAccount account : accounts) {
                if (account.isActive()) {
                    total += account.getBalance(currency);
                }
            }
        }
        
        return total;
    }
    
    /**
     * Apply inflation to the economy
     * 
     * @param rate Inflation rate (e.g., 0.02 for 2%)
     */
    public void applyInflation(double rate) {
        Currency defaultCurrency = currencyManager.getDefaultCurrency();
        if (defaultCurrency == null) return;
        
        // Reduce all balances by inflation rate
        for (PlayerEconomyData data : playerData.values()) {
            double currentBalance = data.getBalance(defaultCurrency);
            double newBalance = currentBalance * (1.0 - rate);
            data.setBalance(defaultCurrency, newBalance);
        }
        
        // Record inflation event
        analytics.recordInflationEvent(rate, System.currentTimeMillis());
    }
    
    // Getters and setters for economy settings
    public boolean isEconomyEnabled() { return economyEnabled; }
    public void setEconomyEnabled(boolean enabled) { this.economyEnabled = enabled; }
    
    public double getStartingBalance() { return startingBalance; }
    public void setStartingBalance(double balance) { this.startingBalance = balance; }
    
    public boolean areNegativeBalancesAllowed() { return negativeBalancesAllowed; }
    public void setNegativeBalancesAllowed(boolean allowed) { this.negativeBalancesAllowed = allowed; }
    
    public double getMaxBalance() { return maxBalance; }
    public void setMaxBalance(double maxBalance) { this.maxBalance = maxBalance; }
    
    public double getInflationRate() { return inflationRate; }
    public void setInflationRate(double rate) { this.inflationRate = rate; }
    
    // Manager getters
    public CurrencyManager getCurrencyManager() { return currencyManager; }
    public BankManager getBankManager() { return bankManager; }
    public TransactionManager getTransactionManager() { return transactionManager; }
    public ShopManager getShopManager() { return shopManager; }
    public EconomicAnalytics getAnalytics() { return analytics; }
    
    /**
     * Shutdown the economy system
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }
}
