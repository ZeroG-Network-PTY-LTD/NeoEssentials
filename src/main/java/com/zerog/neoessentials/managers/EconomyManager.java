package com.zerog.neoessentials.managers;

import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.config.EconomyConfig;
import com.zerog.neoessentials.storage.PlayerDataManager;
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
    
    private final ConfigManager configManager;
    private final PlayerDataManager playerDataManager;
    private final Map<UUID, BigDecimal> balanceCache;
    private final Map<UUID, BankAccount> bankAccounts;
    private final List<Transaction> transactionHistory;
    
    private EconomyManager() {
        this.configManager = ConfigManager.getInstance();
        this.playerDataManager = PlayerDataManager.getInstance();
        this.balanceCache = new ConcurrentHashMap<>();
        this.bankAccounts = new ConcurrentHashMap<>();
        this.transactionHistory = Collections.synchronizedList(new ArrayList<>());
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
        return configManager.getEconomyConfig().enabled;
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
        if (balance == null) {
            EconomyConfig config = configManager.getEconomyConfig();
            balance = BigDecimal.valueOf(config.startingBalance);
            playerDataManager.setBalance(playerUUID, balance);
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
        
        EconomyConfig config = configManager.getEconomyConfig();
        
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
        
        LOGGER.debug("Set balance for player {} to {}", playerUUID, formatCurrency(amount));
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
        
        EconomyConfig config = configManager.getEconomyConfig();
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
        EconomyConfig config = configManager.getEconomyConfig();
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
        EconomyConfig config = configManager.getEconomyConfig();
        
        DecimalFormat formatter = new DecimalFormat(config.currencyFormat);
        formatter.setRoundingMode(RoundingMode.HALF_UP);
        
        String formattedAmount = formatter.format(amount);
        
        return config.currencySymbol + formattedAmount;
    }
    
    /**
     * Get command cost from configuration
     */
    public double getCommandCost(String command) {
        EconomyConfig config = configManager.getEconomyConfig();
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
            EconomyConfig config = configManager.getEconomyConfig();
            MessageUtil.sendMessage(player, config.messages.insufficientFunds
                .replace("{COST}", formatCurrency(cost))
                .replace("{BALANCE}", formatCurrency(getBalance(playerUUID))));
            return false;
        }
        
        withdrawBalance(playerUUID, cost, "Command usage: " + command);
        EconomyConfig config = configManager.getEconomyConfig();
        MessageUtil.sendMessage(player, config.messages.commandCostCharged
            .replace("{COST}", formatCurrency(cost))
            .replace("{COMMAND}", command));
        
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
        EconomyConfig config = configManager.getEconomyConfig();
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
        EconomyConfig config = configManager.getEconomyConfig();
        
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
}
