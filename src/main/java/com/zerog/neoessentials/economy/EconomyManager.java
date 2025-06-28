package com.zerog.neoessentials.economy;

import com.zerog.neoessentials.config.EnhancedEconomyConfig;
import com.zerog.neoessentials.economy.persistence.EconomyPersistenceManager;
import com.zerog.neoessentials.NeoEssentials;

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
    
    // Configuration and persistence
    private final EnhancedEconomyConfig config;
    private final EconomyPersistenceManager persistenceManager;
    
    // Player data
    private final Map<UUID, PlayerEconomyData> playerData;
    
    // Economy settings - loaded from config
    private boolean economyEnabled;
    private double startingBalance;
    private boolean negativeBalancesAllowed;
    private double maxBalance;
    private double inflationRate;
    
    // Scheduled tasks
    private final ScheduledExecutorService scheduler;
    
    private EconomyManager() {
        this.config = EnhancedEconomyConfig.getInstance();
        this.persistenceManager = EconomyPersistenceManager.getInstance();
        
        this.currencyManager = CurrencyManager.getInstance();
        this.bankManager = BankManager.getInstance();
        this.transactionManager = new TransactionManager();
        this.shopManager = ShopManager.getInstance();
        
        // Set persistence manager for all components
        this.bankManager.setPersistenceManager(this.persistenceManager);
        this.analytics = new EconomicAnalytics();
        this.playerData = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(config.getThreadPoolSize());
        
        loadConfiguration();
        startScheduledTasks();
    }
    
    public static EconomyManager getInstance() {
        if (instance == null) {
            instance = new EconomyManager();
        }
        return instance;
    }
    
    /**
     * Load configuration from the enhanced config system
     */
    private void loadConfiguration() {
        this.economyEnabled = config.isEconomyEnabled();
        this.startingBalance = config.getStartingBalance();
        this.negativeBalancesAllowed = config.allowNegativeBalances();
        this.maxBalance = config.getMaxBalance();
        this.inflationRate = config.getInflationRate();
        
        // Load configuration file if it exists
        try {
            config.loadFromFile("config/neoessentials/economy.yml");
        } catch (Exception e) {
            System.err.println("Failed to load economy config, using defaults: " + e.getMessage());
        }
    }
    
    /**
     * Reload configuration from file
     */
    public void reloadConfiguration() {
        loadConfiguration();
        
        // Update components with new configuration
        currencyManager.reloadConfiguration();
        bankManager.reloadConfiguration();
        shopManager.reloadConfiguration();
        
        // Restart scheduled tasks with new intervals
        scheduler.shutdown();
        // New scheduler will be created with updated thread pool size
        startScheduledTasks();
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
        
        // Economic analytics update - use config interval
        scheduler.scheduleAtFixedRate(
            () -> analytics.updateEconomicMetrics(),
            0, config.getUpdateInterval(), TimeUnit.HOURS
        );
        
        // Auto-save player data every 5 minutes
        scheduler.scheduleAtFixedRate(
            this::saveAllPlayerData,
            5, 5, TimeUnit.MINUTES
        );
        
        // Economy health checks every hour
        scheduler.scheduleAtFixedRate(
            this::performHealthChecks,
            1, 1, TimeUnit.HOURS
        );
    }
    
    /**
     * Save all player data to persistence
     */
    private void saveAllPlayerData() {
        for (PlayerEconomyData data : playerData.values()) {
            persistenceManager.savePlayerData(data);
        }
    }
    
    /**
     * Perform economy health checks
     */
    private void performHealthChecks() {
        try {
            analytics.updateEconomicMetrics();
            
            // Check inflation rate
            double currentInflation = analytics.getInflationRate();
            if (currentInflation > 0.1) { // 10% warning threshold
                System.out.println("WARNING: High inflation detected: " + (currentInflation * 100) + "%");
            }
            
            // Check wealth inequality
            double giniCoefficient = analytics.getWealthDistribution().getGiniCoefficient();
            if (giniCoefficient > 0.7) {
                System.out.println("WARNING: High wealth inequality detected: " + giniCoefficient);
            }
            
            // Check economic velocity
            double velocity = analytics.getEconomicVelocity();
            if (velocity < 0.1 || velocity > 2.0) {
                System.out.println("WARNING: Unusual economic velocity: " + velocity);
            }
        } catch (Exception e) {
            System.err.println("Error during economy health check: " + e.getMessage());
        }
    }
    
    /**
     * Get or create player economy data
     * 
     * @param playerId The player's UUID
     * @return The player's economy data
     */
    public PlayerEconomyData getPlayerData(UUID playerId) {
        return playerData.computeIfAbsent(playerId, id -> {
            try {
                // Try to load from persistence first
                PlayerEconomyData data = persistenceManager.loadPlayerData(id).join();
                if (data != null) {
                    return data;
                }
            } catch (Exception e) {
                System.err.println("Failed to load player data for " + id + ": " + e.getMessage());
            }
            
            // Create new player data
            PlayerEconomyData data = new PlayerEconomyData(id);
            
            // Set starting balance in default currency
            Currency defaultCurrency = currencyManager.getDefaultCurrency();
            if (defaultCurrency != null) {
                data.setBalance(defaultCurrency, startingBalance);
            }
            
            // Create primary bank account if banking is enabled
            if (config.isBankingEnabled() && config.isAutoCreateChecking()) {
                bankManager.getPrimaryAccount(playerId);
            }
            
            // Save the new player data
            persistenceManager.savePlayerData(data);
            
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
    
    /**
     * Shutdown the economy system properly
     */
    public void shutdown() {
        try {
            com.zerog.neoessentials.NeoEssentials.LOGGER.info("Shutting down NeoEssentials Economy System...");
            
            // Stop the loan processor
            LoanProcessor.getInstance().stop();
            
            // Shutdown the main scheduler
            if (scheduler != null && !scheduler.isShutdown()) {
                scheduler.shutdown();
                try {
                    if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                        scheduler.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    scheduler.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
            
            // Close persistence manager
            if (persistenceManager != null) {
                persistenceManager.shutdown();
            }
            
            com.zerog.neoessentials.NeoEssentials.LOGGER.info("Economy system shutdown complete.");
            
        } catch (Exception e) {
            com.zerog.neoessentials.NeoEssentials.LOGGER.error("Error during economy system shutdown: " + e.getMessage(), e);
        }
    }
    
    /**
     * Initialize the economy system and load all persistent data
     */
    public void initialize() {
        try {
            com.zerog.neoessentials.NeoEssentials.LOGGER.info("Initializing NeoEssentials Economy System...");
            
            // Initialize bank accounts system
            bankManager.initializeAccounts();
            
            // Initialize loan persistence and preload active loans
            bankManager.initializeLoans();
            
            // Start the loan processor for automated loan management
            LoanProcessor.getInstance().start();
            
            // Load summary statistics
            List<Loan> allActiveLoans = bankManager.getAllActiveLoans();
            int totalLoans = allActiveLoans.size();
            
            Currency defaultCurrency = currencyManager.getDefaultCurrency();
            double totalOutstanding = allActiveLoans.stream()
                .mapToDouble(Loan::getCurrentBalance)
                .sum();
            
            com.zerog.neoessentials.NeoEssentials.LOGGER.info("Economy system initialized successfully!");
            com.zerog.neoessentials.NeoEssentials.LOGGER.info("  - Bank account persistence enabled");
            com.zerog.neoessentials.NeoEssentials.LOGGER.info("  - {} active loans loaded from database", totalLoans);
            if (totalLoans > 0) {
                com.zerog.neoessentials.NeoEssentials.LOGGER.info("  - Total outstanding loan balance: {}", 
                    defaultCurrency != null ? defaultCurrency.format(totalOutstanding) : String.format("$%.2f", totalOutstanding));
            }
            com.zerog.neoessentials.NeoEssentials.LOGGER.info("  - Automated loan processing started (daily updates, hourly overdue checks)");
            com.zerog.neoessentials.NeoEssentials.LOGGER.info("  - All economic data will persist across server restarts");
            
        } catch (Exception e) {
            com.zerog.neoessentials.NeoEssentials.LOGGER.error("Failed to initialize economy system: " + e.getMessage(), e);
        }
    }

    // Getters for managers and configuration
    public CurrencyManager getCurrencyManager() { return currencyManager; }
    public BankManager getBankManager() { return bankManager; }
    public TransactionManager getTransactionManager() { return transactionManager; }
    public ShopManager getShopManager() { return shopManager; }
    public EconomicAnalytics getAnalytics() { return analytics; }
    public EnhancedEconomyConfig getConfig() { return config; }
    public EconomyPersistenceManager getPersistenceManager() { return persistenceManager; }
    
    // Economy settings getters
    public boolean isEconomyEnabled() { return economyEnabled; }
    public double getStartingBalance() { return startingBalance; }
    public boolean areNegativeBalancesAllowed() { return negativeBalancesAllowed; }
    public double getMaxBalance() { return maxBalance; }
    public double getInflationRate() { return inflationRate; }
    
    /**
     * Get all player data (for admin purposes)
     */
    public Map<UUID, PlayerEconomyData> getAllPlayerData() {
        return new HashMap<>(playerData);
    }
    
    /**
     * Get economy statistics
     */
    public Map<String, Object> getEconomyStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalPlayers", playerData.size());
        stats.put("economyEnabled", economyEnabled);
        stats.put("totalCurrencies", currencyManager.getAllCurrencies().size());
        stats.put("totalBankAccounts", bankManager.getTotalAccountCount());
        stats.put("totalActiveLoans", bankManager.getActiveLoansCount());
        stats.put("totalShops", shopManager.getTotalShopsCount());
        stats.put("totalActiveAuctions", shopManager.getAuctionHouse().getActiveAuctionsCount());
        
        // Economic health metrics
        stats.put("inflationRate", analytics.getInflationRate());
        stats.put("economicVelocity", analytics.getEconomicVelocity());
        stats.put("giniCoefficient", analytics.getWealthDistribution().getGiniCoefficient());
        
        return stats;
    }
}
