package com.zerog.neoessentials.managers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.lang.ref.SoftReference;
import java.util.Map;
import java.util.UUID;
import java.util.LinkedList;
import java.util.List;
import java.util.Collection;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.text.DecimalFormat;

import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.config.MainConfig;
import com.zerog.neoessentials.storage.PlayerDataManager;
import com.zerog.neoessentials.storage.StorageManager;

/**
 * Memory-optimized economy management system for NeoEssentials
 * Vault-compatible economy with banking, shops, and transactions
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class EconomyManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(EconomyManager.class);
    private static volatile EconomyManager instance;
    
    private final ConfigManager configManager;
    private final PlayerDataManager playerDataManager;
    
    // Memory-optimized cache with size limits
    private final Map<UUID, SoftReference<BigDecimal>> balanceCache;
    private final Map<String, BankAccount> bankAccounts;
    private final ReentrantReadWriteLock cacheLock;
    
    // Limited transaction history to prevent memory bloat
    private final LinkedList<Transaction> transactionHistory;
    private static final int MAX_TRANSACTION_HISTORY = 1000;
    private final ReentrantReadWriteLock historyLock;
    
    // Cache cleanup
    private final ScheduledExecutorService cleanupExecutor;
    private static final long CLEANUP_INTERVAL = 120000; // 2 minutes
    
    // Reusable objects
    private final ThreadLocal<DecimalFormat> formatter = 
        ThreadLocal.withInitial(() -> new DecimalFormat("#,##0.00"));
    
    private EconomyManager() {
        this.configManager = ConfigManager.getInstance();
        this.playerDataManager = PlayerDataManager.getInstance();
        
        // Initialize optimized data structures
        this.balanceCache = new ConcurrentHashMap<>();
        this.bankAccounts = new ConcurrentHashMap<>();
        this.cacheLock = new ReentrantReadWriteLock();
        
        // Initialize limited transaction history
        this.transactionHistory = new LinkedList<>();
        this.historyLock = new ReentrantReadWriteLock();
        
        // Initialize cleanup executor
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
        
        // Start cache cleanup
        startCacheCleanup();
        
        // Initialize ShopManager with this economy manager instance
        com.zerog.neoessentials.economy.shops.ShopManager.createInstance(this);
        
        LOGGER.info("Economy Manager initialized with memory optimizations");
    }
    
    /**
     * Thread-safe singleton accessor
     */
    public static EconomyManager getInstance() {
        if (instance == null) {
            synchronized (EconomyManager.class) {
                if (instance == null) {
                    instance = new EconomyManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * Start cache cleanup task
     */
    private void startCacheCleanup() {
        cleanupExecutor.scheduleAtFixedRate(
            this::cleanupCache,
            CLEANUP_INTERVAL,
            CLEANUP_INTERVAL,
            TimeUnit.MILLISECONDS
        );
    }
    
    /**
     * Cleanup expired cache entries and limit transaction history
     */
    private void cleanupCache() {
        // Clean balance cache
        cacheLock.writeLock().lock();
        try {
            balanceCache.entrySet().removeIf(entry -> entry.getValue().get() == null);
        } finally {
            cacheLock.writeLock().unlock();
        }
        
        // Limit transaction history size
        historyLock.writeLock().lock();
        try {
            while (transactionHistory.size() > MAX_TRANSACTION_HISTORY) {
                transactionHistory.removeFirst();
            }
        } finally {
            historyLock.writeLock().unlock();
        }
    }
    
    /**
     * Check if economy system is enabled
     */
    public boolean isEnabled() {
        return configManager.getMainConfig().modules.economy;
    }
    
    /**
     * Helper method to get balance from cache
     */
    private BigDecimal getCachedBalance(UUID playerUUID) {
        cacheLock.readLock().lock();
        try {
            SoftReference<BigDecimal> ref = balanceCache.get(playerUUID);
            if (ref != null) {
                BigDecimal balance = ref.get();
                if (balance != null) {
                    return balance;
                } else {
                    // Reference was cleared, remove it
                    balanceCache.remove(playerUUID);
                }
            }
            return null;
        } finally {
            cacheLock.readLock().unlock();
        }
    }
    
    /**
     * Helper method to cache balance
     */
    private void cacheBalance(UUID playerUUID, BigDecimal balance) {
        cacheLock.writeLock().lock();
        try {
            balanceCache.put(playerUUID, new SoftReference<>(balance));
        } finally {
            cacheLock.writeLock().unlock();
        }
    }
    
    /**
     * Helper method to add transaction to history with size limit
     */
    private void addTransactionToHistory(Transaction transaction) {
        historyLock.writeLock().lock();
        try {
            transactionHistory.addLast(transaction);
            // Remove oldest if exceeded limit
            if (transactionHistory.size() > MAX_TRANSACTION_HISTORY) {
                transactionHistory.removeFirst();
            }
        } finally {
            historyLock.writeLock().unlock();
        }
    }
    
    /**
     * Get player's balance with memory optimization
     */
    public BigDecimal getBalance(UUID playerUUID) {
        if (!isEnabled()) {
            return BigDecimal.ZERO;
        }
        
        // Check cache first
        BigDecimal cachedBalance = getCachedBalance(playerUUID);
        if (cachedBalance != null) {
            return cachedBalance;
        }
        
        // Ensure player data is loaded (only if not already in cache)
        if (!playerDataManager.isPlayerDataLoaded(playerUUID)) {
            playerDataManager.loadPlayerDataSync(playerUUID);
        }
        
        // Load from storage
        BigDecimal balance = playerDataManager.getBalance(playerUUID);
        
        LOGGER.info("Loading balance for player {}: {}", playerUUID, balance != null ? formatCurrency(balance) : "null");
        
        // Check if player needs starting balance initialization (only for truly new players)
        if ((balance == null || balance.equals(BigDecimal.ZERO)) && !hasBeenInitialized(playerUUID)) {
            // New player - set starting balance from config and mark as initialized
            initializePlayerBalance(playerUUID);
            balance = BigDecimal.valueOf(configManager.getMainConfig().economySettings.startingBalance);
        } else if (balance == null) {
            // Player exists but balance is null (corrupted data) - set to zero, don't reset to starting balance
            balance = BigDecimal.ZERO;
            playerDataManager.setBalance(playerUUID, balance);
            LOGGER.warn("Player {} had null balance, set to zero", playerUUID);
        }
        
        cacheBalance(playerUUID, balance);
        return balance;
    }
    
    /**
     * Set player's balance
     */
    public void setBalance(UUID playerUUID, double amount) {
        setBalance(playerUUID, BigDecimal.valueOf(amount));
    }
    
    /**
     * Set player's balance with BigDecimal precision
     */
    public void setBalance(UUID playerUUID, BigDecimal amount) {
        if (!isEnabled()) {
            return;
        }
        
        MainConfig.EconomySettings config = configManager.getMainConfig().economySettings;
        
        // Validate amount
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            amount = BigDecimal.ZERO;
        }
        if (amount.compareTo(BigDecimal.valueOf(config.maxBalance)) > 0) {
            amount = BigDecimal.valueOf(config.maxBalance);
        }
        
        // Update cache and storage
        cacheBalance(playerUUID, amount);
        playerDataManager.setBalance(playerUUID, amount);
        
        LOGGER.info("Set balance for player {} to {} - saved to storage", playerUUID, formatCurrency(amount));
    }
    
    /**
     * Add money to player's account
     */
    public boolean depositBalance(UUID playerUUID, double amount, String reason) {
        return depositBalance(playerUUID, BigDecimal.valueOf(amount), reason);
    }
    
    /**
     * Add money to player's account with BigDecimal precision
     */
    public boolean depositBalance(UUID playerUUID, BigDecimal amount, String reason) {
        if (!isEnabled() || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        
        BigDecimal currentBalance = getBalance(playerUUID);
        BigDecimal newBalance = currentBalance.add(amount);
        
        MainConfig.EconomySettings config = configManager.getMainConfig().economySettings;
        if (newBalance.compareTo(BigDecimal.valueOf(config.maxBalance)) > 0) {
            newBalance = BigDecimal.valueOf(config.maxBalance);
        }
        
        setBalance(playerUUID, newBalance);
        recordTransaction(playerUUID, TransactionType.DEPOSIT, amount, reason);
        
        LOGGER.info("Deposited {} to player {} ({}) - new balance: {}", 
            formatCurrency(amount), playerUUID, reason, formatCurrency(newBalance));
        
        return true;
    }
    
    /**
     * Remove money from player's account
     */
    public boolean withdrawBalance(UUID playerUUID, double amount, String reason) {
        return withdrawBalance(playerUUID, BigDecimal.valueOf(amount), reason);
    }
    
    /**
     * Remove money from player's account with BigDecimal precision
     */
    public boolean withdrawBalance(UUID playerUUID, BigDecimal amount, String reason) {
        if (!isEnabled() || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        
        BigDecimal currentBalance = getBalance(playerUUID);
        if (currentBalance.compareTo(amount) < 0) {
            LOGGER.warn("Player {} attempted to withdraw {} but only has {} (reason: {})", 
                playerUUID, formatCurrency(amount), formatCurrency(currentBalance), reason);
            return false;
        }
        
        BigDecimal newBalance = currentBalance.subtract(amount);
        setBalance(playerUUID, newBalance);
        recordTransaction(playerUUID, TransactionType.WITHDRAWAL, amount, reason);
        
        LOGGER.info("Withdrew {} from player {} ({}) - new balance: {}", 
            formatCurrency(amount), playerUUID, reason, formatCurrency(newBalance));
        
        return true;
    }
    
    /**
     * Check if player has sufficient balance
     */
    public boolean hasBalance(UUID playerUUID, double amount) {
        return hasBalance(playerUUID, BigDecimal.valueOf(amount));
    }
    
    /**
     * Check if player has sufficient balance with BigDecimal precision
     */
    public boolean hasBalance(UUID playerUUID, BigDecimal amount) {
        BigDecimal currentBalance = getBalance(playerUUID);
        return currentBalance.compareTo(amount) >= 0;
    }
    
    /**
     * Format currency amount for display
     */
    public String formatCurrency(double amount) {
        return formatCurrency(BigDecimal.valueOf(amount));
    }
    
    /**
     * Format currency amount for display with BigDecimal precision
     */
    public String formatCurrency(BigDecimal amount) {
        DecimalFormat format = formatter.get();
        MainConfig.EconomySettings config = configManager.getMainConfig().economySettings;
        return config.currencySymbol + format.format(amount);
    }
    
    /**
     * Record a transaction for history tracking
     */
    private void recordTransaction(UUID playerUUID, TransactionType type, BigDecimal amount, String reason) {
        Transaction transaction = new Transaction(
            UUID.randomUUID(),
            playerUUID,
            type,
            amount,
            reason,
            System.currentTimeMillis()
        );
        addTransactionToHistory(transaction);
    }
    
    /**
     * Initialize player balance for truly new players
     */
    private void initializePlayerBalance(UUID playerUUID) {
        MainConfig.EconomySettings config = configManager.getMainConfig().economySettings;
        BigDecimal startingBalance = BigDecimal.valueOf(config.startingBalance);
        
        playerDataManager.setBalance(playerUUID, startingBalance);
        playerDataManager.setSetting(playerUUID, "economy_initialized", true);
        
        LOGGER.info("Initialized new player {} with starting balance: {}", 
            playerUUID, formatCurrency(startingBalance));
    }
    
    /**
     * Check if player has been previously initialized
     */
    private boolean hasBeenInitialized(UUID playerUUID) {
        return playerDataManager.getSettingBoolean(playerUUID, "economy_initialized", false);
    }
    
    /**
     * Get transaction history for a player
     */
    public List<Transaction> getTransactionHistory(UUID playerUUID) {
        historyLock.readLock().lock();
        try {
            return transactionHistory.stream()
                .filter(transaction -> transaction.playerUUID.equals(playerUUID))
                .collect(Collectors.toList());
        } finally {
            historyLock.readLock().unlock();
        }
    }
    
    /**
     * Get transaction history for a player with limit
     */
    public List<Transaction> getTransactionHistory(UUID playerUUID, int limit) {
        historyLock.readLock().lock();
        try {
            return transactionHistory.stream()
                .filter(transaction -> transaction.playerUUID.equals(playerUUID))
                .limit(limit)
                .collect(Collectors.toList());
        } finally {
            historyLock.readLock().unlock();
        }
    }
    
    /**
     * Clean up inactive accounts
     */
    public void cleanup() {
        MainConfig.EconomySettings config = configManager.getMainConfig().economySettings;
        
        // Remove small balances if configured
        if (config.cleanupInactiveAccounts) {
            balanceCache.entrySet().removeIf(entry -> {
                BigDecimal balance = entry.getValue().get();
                return balance != null && balance.compareTo(BigDecimal.valueOf(0.01)) < 0;
            });
        }
        
        // Cleanup old transactions
        long cutoffTime = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000); // 30 days
        transactionHistory.removeIf(transaction -> transaction.timestamp < cutoffTime);
    }
    
    /**
     * Get current total money in circulation
     */
    public BigDecimal getTotalMoney() {
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<UUID, SoftReference<BigDecimal>> entry : balanceCache.entrySet()) {
            BigDecimal balance = entry.getValue().get();
            if (balance != null) {
                total = total.add(balance);
            }
        }
        return total;
    }
    
    /**
     * Get number of active accounts in cache
     */
    public int getActiveAccountCount() {
        return (int) balanceCache.entrySet().stream()
            .map(entry -> entry.getValue().get())
            .filter(Objects::nonNull)
            .count();
    }
    
    /**
     * Get bank account for player
     */
    public BankAccount getBankAccount(UUID playerUUID) {
        return bankAccounts.get(playerUUID.toString());
    }
    
    /**
     * Create bank account for player
     */
    public BankAccount createBankAccount(UUID playerUUID, String accountName) {
        BankAccount account = new BankAccount(accountName, playerUUID);
        bankAccounts.put(account.getAccountId(), account);
        return account;
    }
    
    /**
     * Get all bank accounts
     */
    public Collection<BankAccount> getAllBankAccounts() {
        return bankAccounts.values();
    }
    
    /**
     * Transfer money between players
     */
    public boolean transferMoney(UUID fromPlayer, UUID toPlayer, BigDecimal amount, String reason) {
        if (!isEnabled() || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        
        if (!hasBalance(fromPlayer, amount)) {
            return false;
        }
        
        if (withdrawBalance(fromPlayer, amount, "Transfer to " + toPlayer + ": " + reason)) {
            return depositBalance(toPlayer, amount, "Transfer from " + fromPlayer + ": " + reason);
        }
        
        return false;
    }
    
    /**
     * Save individual player data to persistent storage
     */
    private void savePlayerData(UUID playerUUID) {
        try {
            SoftReference<BigDecimal> balanceRef = balanceCache.get(playerUUID);
            if (balanceRef != null) {
                BigDecimal balance = balanceRef.get();
                if (balance != null) {
                    // Create a simple data structure for storage
                    Map<String, Object> playerEconomyData = new HashMap<>();
                    playerEconomyData.put("balance", balance.doubleValue());
                    playerEconomyData.put("lastSaved", System.currentTimeMillis());
                    
                    // Save using storage manager
                    StorageManager.getInstance().savePlayerEconomy(playerUUID, playerEconomyData);
                    
                    LOGGER.debug("Saved economy data for player {} - balance: {}", playerUUID, formatCurrency(balance));
                } else {
                    LOGGER.warn("Balance reference was cleared for player {}, skipping save", playerUUID);
                }
            } else {
                LOGGER.debug("No cached data for player {}, skipping save", playerUUID);
            }
        } catch (Exception e) {
            LOGGER.error("Error saving player data for {}", playerUUID, e);
        }
    }
    
    /**
     * Shutdown the economy manager and save all data
     */
    public void shutdown() {
        try {
            LOGGER.info("Shutting down Economy Manager - saving all player data");
            
            // Save all player data through the storage manager
            for (UUID playerUUID : balanceCache.keySet()) {
                savePlayerData(playerUUID);
            }
            
            LOGGER.info("Economy Manager shutdown completed - saved {} player records", balanceCache.size());
        } catch (Exception e) {
            LOGGER.error("Error during economy manager shutdown", e);
        }
    }
    
    /**
     * Get top balances for leaderboard display
     */
    public List<Map.Entry<UUID, BigDecimal>> getTopBalances(int limit) {
        return balanceCache.entrySet().stream()
            .map(entry -> {
                BigDecimal balance = entry.getValue().get();
                return balance != null ? Map.entry(entry.getKey(), balance) : null;
            })
            .filter(Objects::nonNull)
            .sorted(Map.Entry.<UUID, BigDecimal>comparingByValue().reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    /**
     * Show balance to command source
     */
    public int showBalance(net.minecraft.commands.CommandSourceStack source, net.minecraft.server.level.ServerPlayer player) {
        try {
            BigDecimal balance = getBalance(player.getUUID());
            source.sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                "Balance for " + player.getName().getString() + ": " + formatCurrency(balance)
            ), false);
            return 1;
        } catch (Exception e) {
            source.sendFailure(net.minecraft.network.chat.Component.literal("Error retrieving balance: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Pay money between players
     */
    public int pay(net.minecraft.commands.CommandSourceStack source, net.minecraft.server.level.ServerPlayer fromPlayer, 
                   net.minecraft.server.level.ServerPlayer toPlayer, double amount) {
        try {
            if (transferMoney(fromPlayer.getUUID(), toPlayer.getUUID(), BigDecimal.valueOf(amount), "Player payment")) {
                source.sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                    "Transferred " + formatCurrency(amount) + " from " + fromPlayer.getName().getString() + 
                    " to " + toPlayer.getName().getString()
                ), false);
                return 1;
            } else {
                source.sendFailure(net.minecraft.network.chat.Component.literal("Transfer failed - insufficient funds"));
                return 0;
            }
        } catch (Exception e) {
            source.sendFailure(net.minecraft.network.chat.Component.literal("Error processing payment: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Give money to player (admin command)
     */
    public int give(net.minecraft.commands.CommandSourceStack source, net.minecraft.server.level.ServerPlayer player, double amount) {
        try {
            if (depositBalance(player.getUUID(), amount, "Admin give")) {
                source.sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                    "Gave " + formatCurrency(amount) + " to " + player.getName().getString()
                ), false);
                return 1;
            } else {
                source.sendFailure(net.minecraft.network.chat.Component.literal("Failed to give money"));
                return 0;
            }
        } catch (Exception e) {
            source.sendFailure(net.minecraft.network.chat.Component.literal("Error giving money: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Take money from player (admin command)
     */
    public int take(net.minecraft.commands.CommandSourceStack source, net.minecraft.server.level.ServerPlayer player, double amount) {
        try {
            if (withdrawBalance(player.getUUID(), amount, "Admin take")) {
                source.sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                    "Took " + formatCurrency(amount) + " from " + player.getName().getString()
                ), false);
                return 1;
            } else {
                source.sendFailure(net.minecraft.network.chat.Component.literal("Failed to take money - insufficient funds"));
                return 0;
            }
        } catch (Exception e) {
            source.sendFailure(net.minecraft.network.chat.Component.literal("Error taking money: " + e.getMessage()));
            return 0;
        }
    }
    
    // Static inner classes for data structures
    /**
     * Transaction record for history tracking
     */
    public static class Transaction {
        public final UUID transactionId;
        public final UUID playerUUID;
        public final TransactionType type;
        public final BigDecimal amount;
        public final String reason;
        public final long timestamp;
        
        public Transaction(UUID transactionId, UUID playerUUID, TransactionType type, 
                         BigDecimal amount, String reason, long timestamp) {
            this.transactionId = transactionId;
            this.playerUUID = playerUUID;
            this.type = type;
            this.amount = amount;
            this.reason = reason;
            this.timestamp = timestamp;
        }
    }
    
    /**
     * Transaction types for tracking
     */
    public enum TransactionType {
        DEPOSIT,
        WITHDRAWAL,
        TRANSFER_IN,
        TRANSFER_OUT,
        TRANSFER_SENT,
        TRANSFER_RECEIVED,
        PURCHASE,
        SALE,
        SHOP_PURCHASE,
        SHOP_SALE,
        ADMIN_GIVE,
        ADMIN_TAKE,
        FEE,
        COMMAND_COST
    }
    
    /**
     * Bank account for advanced banking features
     */
    public static class BankAccount {
        private final String accountId;
        private final String accountName;
        private final UUID owner;
        private BigDecimal balance;
        private final long createdTime;
        private boolean active;
        
        public BankAccount(String accountName, UUID owner) {
            this.accountId = UUID.randomUUID().toString();
            this.accountName = accountName;
            this.owner = owner;
            this.balance = BigDecimal.ZERO;
            this.createdTime = System.currentTimeMillis();
            this.active = true;
        }
        
        // Getters and setters
        public String getAccountId() { return accountId; }
        public String getAccountName() { return accountName; }
        public UUID getOwner() { return owner; }
        public BigDecimal getBalance() { return balance; }
        public void setBalance(BigDecimal balance) { this.balance = balance; }
        public long getCreatedTime() { return createdTime; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
    }
    
    /**
     * Bank member for shared accounts
     */
    public static class BankMember {
        private final UUID playerUUID;
        private final String permission;
        private final long joinedTime;
        
        public BankMember(UUID playerUUID, String permission) {
            this.playerUUID = playerUUID;
            this.permission = permission;
            this.joinedTime = System.currentTimeMillis();
        }
        
        public UUID getPlayerUUID() { return playerUUID; }
        public String getPermission() { return permission; }
        public long getJoinedTime() { return joinedTime; }
    }
}
