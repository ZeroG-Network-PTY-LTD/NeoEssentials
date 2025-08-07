package com.zerog.neoessentials.managers;

import com.zerog.neoessentials.config.ConfigurationUnifier;
import com.zerog.neoessentials.config.EconomyConfig;
import com.zerog.neoessentials.storage.PlayerDataManager;
import com.zerog.neoessentials.storage.StorageManager;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Economy management system for NeoEssentials
 * Vault-compatible economy with banking, shops, and transactions
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class EconomyManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(EconomyManager.class);
    private static EconomyManager instance;
    
    private final ConfigurationUnifier configUnifier;
    private final PlayerDataManager playerDataManager;
    private final Map<UUID, BigDecimal> balanceCache;
    private final Map<UUID, BankAccount> bankAccounts;
    private final List<Transaction> transactionHistory;
    
    private EconomyManager() {
        this.configUnifier = ConfigurationUnifier.getInstance();
        this.playerDataManager = PlayerDataManager.getInstance();
        this.balanceCache = new ConcurrentHashMap<>();
        this.bankAccounts = new ConcurrentHashMap<>();
        this.transactionHistory = Collections.synchronizedList(new ArrayList<>());
        
        // Initialize ShopManager with this economy manager instance
        com.zerog.neoessentials.economy.shops.ShopManager.createInstance(this);
        
        // Initialize the shop manager to load shops from storage
        com.zerog.neoessentials.economy.shops.ShopManager shopManager = 
            com.zerog.neoessentials.economy.shops.ShopManager.getInstance();
        if (shopManager != null) {
            shopManager.initialize();
        }
    }
    
    public static EconomyManager getInstance() {
        if (instance == null) {
            instance = new EconomyManager();
        }
        return instance;
    }
    
    /**
     * Check if economy system is enabled
     */
    public boolean isEnabled() {
        return configUnifier.getConfigManager().getEconomyConfig().enabled;
    }
    
    /**
     * Get player's balance
     */
    public BigDecimal getBalance(UUID playerUUID) {
        if (!isEnabled()) {
            return BigDecimal.ZERO;
        }
        
        // Check cache first
        BigDecimal cachedBalance = balanceCache.get(playerUUID);
        if (cachedBalance != null) {
            return cachedBalance;
        }
        
        // Load from storage
        BigDecimal balance = playerDataManager.getBalance(playerUUID);
        
        LOGGER.info("Loading balance for player {}: {}", playerUUID, balance != null ? formatCurrency(balance) : "null");
        
        // Check if player needs starting balance initialization (only for truly new players)
        if (balance == null && !hasBeenInitialized(playerUUID)) {
            // New player - set starting balance from config
            EconomyConfig config = configUnifier.getConfigManager().getEconomyConfig();
            balance = BigDecimal.valueOf(config.startingBalance);
            playerDataManager.setBalance(playerUUID, balance);
            markAsInitialized(playerUUID);
            LOGGER.info("Set starting balance of {} for new player {}", formatCurrency(balance), playerUUID);
        } else if (balance == null) {
            // Player exists but balance is null (corrupted data) - set to zero, don't reset to starting balance
            balance = BigDecimal.ZERO;
            playerDataManager.setBalance(playerUUID, balance);
            LOGGER.warn("Player {} had null balance, set to zero", playerUUID);
        }
        
        balanceCache.put(playerUUID, balance);
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
        
        EconomyConfig config = configUnifier.getConfigManager().getEconomyConfig();
        
        // Validate amount
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            amount = BigDecimal.ZERO;
        }
        if (amount.compareTo(BigDecimal.valueOf(config.maxBalance)) > 0) {
            amount = BigDecimal.valueOf(config.maxBalance);
        }
        
        // Update cache and storage
        balanceCache.put(playerUUID, amount);
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
        
        EconomyConfig config = configUnifier.getConfigManager().getEconomyConfig();
        if (newBalance.compareTo(BigDecimal.valueOf(config.maxBalance)) > 0) {
            newBalance = BigDecimal.valueOf(config.maxBalance);
        }
        
        setBalance(playerUUID, newBalance);
        recordTransaction(playerUUID, TransactionType.DEPOSIT, amount, reason);
        
        LOGGER.info("Deposited {} to player {} (reason: {}). New balance: {}", 
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
            return false; // Insufficient funds
        }
        
        BigDecimal newBalance = currentBalance.subtract(amount);
        setBalance(playerUUID, newBalance);
        recordTransaction(playerUUID, TransactionType.WITHDRAWAL, amount, reason);
        
        LOGGER.info("Withdrew {} from player {} (reason: {}). New balance: {}", 
            formatCurrency(amount), playerUUID, reason, formatCurrency(newBalance));
        
        return true;
    }
    
    /**
     * Check if player has enough money
     */
    public boolean hasBalance(UUID playerUUID, double amount) {
        return hasBalance(playerUUID, BigDecimal.valueOf(amount));
    }
    
    /**
     * Check if player has enough money with BigDecimal precision
     */
    public boolean hasBalance(UUID playerUUID, BigDecimal amount) {
        if (!isEnabled()) {
            return true;
        }
        
        BigDecimal balance = getBalance(playerUUID);
        return balance.compareTo(amount) >= 0;
    }
    
    /**
     * Transfer money between players
     */
    public boolean transferMoney(UUID fromPlayer, UUID toPlayer, BigDecimal amount, String reason) {
        if (!isEnabled() || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        
        // Check if sender has enough money
        if (!hasBalance(fromPlayer, amount)) {
            return false;
        }
        
        // Apply transfer fee if configured
        EconomyConfig config = configUnifier.getConfigManager().getEconomyConfig();
        BigDecimal fee = BigDecimal.ZERO;
        if (config.transferFeePercent > 0) {
            fee = amount.multiply(BigDecimal.valueOf(config.transferFeePercent / 100.0))
                       .setScale(2, RoundingMode.HALF_UP);
        }
        
        BigDecimal totalDeduction = amount.add(fee);
        
        // Check if sender can afford amount + fee
        if (!hasBalance(fromPlayer, totalDeduction)) {
            return false;
        }
        
        // Perform transfer
        if (withdrawBalance(fromPlayer, totalDeduction, "Transfer to " + toPlayer + " (fee: " + formatCurrency(fee) + ")")) {
            depositBalance(toPlayer, amount, "Transfer from " + fromPlayer);
            
            // Record fee transaction if applicable
            if (fee.compareTo(BigDecimal.ZERO) > 0) {
                recordTransaction(fromPlayer, TransactionType.FEE, fee, "Transfer fee");
            }
            
            return true;
        }
        
        return false;
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
        EconomyConfig config = configUnifier.getConfigManager().getEconomyConfig();
        
        DecimalFormat formatter = new DecimalFormat(config.currencyFormat);
        formatter.setRoundingMode(RoundingMode.HALF_UP);
        
        String formattedAmount = formatter.format(amount);
        
        return config.currencySymbol + formattedAmount;
    }
    
    /**
     * Get command cost from configuration
     */
    public double getCommandCost(String command) {
        EconomyConfig config = configUnifier.getConfigManager().getEconomyConfig();
        return config.commandCosts.getOrDefault(command.toLowerCase(), BigDecimal.ZERO).doubleValue();
    }
    
    /**
     * Charge player for command usage
     */
    public boolean chargeCommand(ServerPlayer player, String command) {
        double cost = getCommandCost(command);
        if (cost <= 0) {
            return true; // No cost, allow command
        }
        
        UUID playerUUID = player.getUUID();
        if (!hasBalance(playerUUID, cost)) {
            EconomyConfig config = configUnifier.getConfigManager().getEconomyConfig();
            MessageUtil.sendMessage(player, config.messages.insufficientFunds,
                formatCurrency(cost), formatCurrency(getBalance(playerUUID)));
            return false;
        }
        
        withdrawBalance(playerUUID, cost, "Command usage: " + command);
        EconomyConfig config = configUnifier.getConfigManager().getEconomyConfig();
        MessageUtil.sendMessage(player, config.messages.commandCostCharged,
            formatCurrency(cost), command);
        
        return true;
    }
    
    /**
     * Bank account management
     */
    public BankAccount getBankAccount(UUID playerUUID) {
        return bankAccounts.computeIfAbsent(playerUUID, uuid -> new BankAccount(uuid));
    }
    
    /**
     * Calculate and apply interest to all accounts
     */
    public void processInterest() {
        EconomyConfig config = configUnifier.getConfigManager().getEconomyConfig();
        if (!config.bank.enabled || config.bank.interestRate <= 0) {
            return;
        }
        
        // Process interest for all accounts
        double interestRate = config.bank.interestRate / 100.0;
        
        for (UUID playerUUID : balanceCache.keySet()) {
            BigDecimal balance = getBalance(playerUUID);
            
            if (balance.compareTo(BigDecimal.valueOf(config.bank.minimumBalance)) >= 0) {
                BigDecimal interest = balance.multiply(BigDecimal.valueOf(interestRate))
                                           .setScale(2, RoundingMode.HALF_UP);
                
                if (interest.compareTo(BigDecimal.valueOf(config.bank.maxInterestPayout)) > 0) {
                    interest = BigDecimal.valueOf(config.bank.maxInterestPayout);
                }
                
                if (interest.compareTo(BigDecimal.ZERO) > 0) {
                    depositBalance(playerUUID, interest, "Bank interest");
                }
            }
        }
        
        LOGGER.info("Processed interest for {} accounts", balanceCache.size());
    }
    
    /**
     * Get transaction history for player
     */
    public List<Transaction> getTransactionHistory(UUID playerUUID, int limit) {
        return transactionHistory.stream()
            .filter(transaction -> transaction.playerUUID.equals(playerUUID))
            .sorted((t1, t2) -> Long.compare(t2.timestamp, t1.timestamp))
            .limit(limit)
            .toList();
    }
    
    /**
     * Record a transaction
     */
    private void recordTransaction(UUID playerUUID, TransactionType type, BigDecimal amount, String reason) {
        Transaction transaction = new Transaction(playerUUID, type, amount, reason, System.currentTimeMillis());
        transactionHistory.add(transaction);
        
        // Limit history size
        if (transactionHistory.size() > 10000) {
            transactionHistory.subList(0, 5000).clear();
        }
    }
    
    /**
     * Get top balances for leaderboard
     */
    public List<Map.Entry<UUID, BigDecimal>> getTopBalances(int limit) {
        return balanceCache.entrySet().stream()
            .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
            .limit(limit)
            .toList();
    }
    
    /**
     * Clean up inactive accounts
     */
    public void cleanup() {
        EconomyConfig config = configUnifier.getConfigManager().getEconomyConfig();
        
        // Remove small balances if configured
        if (config.cleanupInactiveAccounts) {
            balanceCache.entrySet().removeIf(entry -> 
                entry.getValue().compareTo(BigDecimal.valueOf(0.01)) < 0);
        }
        
        // Cleanup old transactions
        long cutoffTime = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000); // 30 days
        transactionHistory.removeIf(transaction -> transaction.timestamp < cutoffTime);
    }
    
    /**
     * Initialize starting balance for a new player
     * This method ensures players get their configured starting balance
     */
    public void initializePlayerBalance(UUID playerUUID) {
        if (!isEnabled()) {
            return;
        }
        
        EconomyConfig config = configUnifier.getConfigManager().getEconomyConfig();
        BigDecimal startingBalance = BigDecimal.valueOf(config.startingBalance);
        
        // Set the starting balance and mark as initialized
        setBalance(playerUUID, startingBalance);
        markAsInitialized(playerUUID);
        LOGGER.info("Initialized starting balance of {} for player {}", formatCurrency(startingBalance), playerUUID);
    }
    
    /**
     * Check if player has been initialized with starting balance
     */
    private boolean hasBeenInitialized(UUID playerUUID) {
        Object initialized = playerDataManager.getSetting(playerUUID, "economy.initialized");
        return initialized != null && Boolean.parseBoolean(initialized.toString());
    }
    
    /**
     * Mark player as initialized
     */
    private void markAsInitialized(UUID playerUUID) {
        playerDataManager.setSetting(playerUUID, "economy.initialized", "true");
    }
    
    /**
     * Transaction types
     */
    public enum TransactionType {
        DEPOSIT,
        WITHDRAWAL,
        TRANSFER_SENT,
        TRANSFER_RECEIVED,
        FEE,
        COMMAND_COST,
        SHOP_PURCHASE,
        SHOP_SALE
    }
    
    /**
     * Transaction record
     */
    public static class Transaction {
        public final UUID playerUUID;
        public final TransactionType type;
        public final BigDecimal amount;
        public final String reason;
        public final long timestamp;
        
        public Transaction(UUID playerUUID, TransactionType type, BigDecimal amount, String reason, long timestamp) {
            this.playerUUID = playerUUID;
            this.type = type;
            this.amount = amount;
            this.reason = reason;
            this.timestamp = timestamp;
        }
    }
    
    /**
     * Bank account with additional features
     */
    public static class BankAccount {
        public final UUID playerUUID;
        private BigDecimal savingsBalance;
        private long lastInterestCalculation;
        private boolean frozen;
        
        public BankAccount(UUID playerUUID) {
            this.playerUUID = playerUUID;
            this.savingsBalance = BigDecimal.ZERO;
            this.lastInterestCalculation = System.currentTimeMillis();
            this.frozen = false;
        }
        
        public BigDecimal getSavingsBalance() {
            return savingsBalance;
        }
        
        public void setSavingsBalance(BigDecimal balance) {
            this.savingsBalance = balance;
        }
        
        public boolean isFrozen() {
            return frozen;
        }
        
        public void setFrozen(boolean frozen) {
            this.frozen = frozen;
        }
        
        public long getLastInterestCalculation() {
            return lastInterestCalculation;
        }
        
        public void setLastInterestCalculation(long timestamp) {
            this.lastInterestCalculation = timestamp;
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
     * Save individual player data to persistent storage
     */
    private void savePlayerData(UUID playerUUID) {
        try {
            BigDecimal balance = balanceCache.get(playerUUID);
            if (balance != null) {
                // Create a simple data structure for storage
                Map<String, Object> playerEconomyData = new HashMap<>();
                playerEconomyData.put("balance", balance.doubleValue());
                playerEconomyData.put("lastSaved", System.currentTimeMillis());
                
                // Save using storage manager
                StorageManager.getInstance().savePlayerEconomy(playerUUID, playerEconomyData);
                LOGGER.debug("Saved economy data for player {}: ${}", playerUUID, balance);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to save economy data for player " + playerUUID, e);
        }
    }
}
