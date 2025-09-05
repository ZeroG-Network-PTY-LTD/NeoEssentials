package com.zerog.neoessentials.economy;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.zerog.neoessentials.economy.currency.CurrencyManager;
import com.zerog.neoessentials.economy.transactions.TransactionManager;
// import com.zerog.neoessentials.economy.shops.ShopManager; // Now uses managers.EconomyManager
import com.zerog.neoessentials.storage.StorageManager;

import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.math.BigDecimal;
// ...existing code...

/**
 * Advanced Economy Management System
 * Provides comprehensive economic features including:
 * - Multi-currency support with exchange rates
 * - Banking system with accounts, loans, and interest
 * - Transaction history and analytics
 * - Dynamic market system with supply/demand
 * - Player shops and auction house
 * - Tax system and government economics
 * - Cross-server economy synchronization
 */
public class EconomyManager {
    private static final Logger LOGGER = LogManager.getLogger(EconomyManager.class);
    private static EconomyManager instance;
    
    // Core managers
    private final CurrencyManager currencyManager;
    private final TransactionManager transactionManager;
    // private final ShopManager shopManager; // Now uses managers.EconomyManager
    // Auction manager removed as per feature cleanup
    
    // Configuration and data
    private final StorageManager storageManager;
    
    // Player balances and economy data
    private final Map<UUID, PlayerEconomyData> playerData;
    
    // Economy statistics and analytics
    private EconomyAnalytics analytics;
    private boolean economyEnabled;
    
    public EconomyManager() {
        this.storageManager = StorageManager.getInstance();
        this.playerData = new ConcurrentHashMap<>();
        
        // Initialize managers
        this.currencyManager = new CurrencyManager(this);
        this.transactionManager = new TransactionManager(this);
        // ShopManager now uses managers.EconomyManager - removed initialization
        // Auction manager removed as per feature cleanup
        
        this.analytics = new EconomyAnalytics();
        this.economyEnabled = true;
        
        instance = this;
        initialize();
    }
    
    public static EconomyManager getInstance() {
        if (instance == null) {
            instance = new EconomyManager();
        }
        return instance;
    }
    
    private void initialize() {
        try {
            LOGGER.info("Initializing Advanced Economy System...");

            // Initialize currency system
            currencyManager.initialize();

            // Initialize banking system

            // Load player data
            loadPlayerData();

            // Initialize market systems
            // shopManager.initialize(); // Now uses managers.EconomyManager
            // Auction system removed as per feature cleanup

            // Start background tasks
            startBackgroundTasks();

            LOGGER.info("Economy System initialized successfully");

        } catch (Exception e) {
            LOGGER.error("Failed to initialize Economy System", e);
            economyEnabled = false;
        }
    }
    
    private void startBackgroundTasks() {
        // Start interest calculation task
        CompletableFuture.runAsync(() -> {
            while (economyEnabled) {
                try {
                    Thread.sleep(60 * 1000); // Default: 60 seconds
                    updateEconomyAnalytics();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    LOGGER.error("Error in economy background task", e);
                }
            }
        });

        // Start market update task
        CompletableFuture.runAsync(() -> {
            while (economyEnabled) {
                try {
                    Thread.sleep(60 * 1000); // Default: 60 seconds
                    // Auction processing removed as per feature cleanup
                    // Process other economy tasks here if needed
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    LOGGER.error("Error in market update task", e);
                }
            }
        });
    }
    
    // Balance Management
    public BigDecimal getBalance(UUID playerId, String currency) {
        PlayerEconomyData data = getPlayerData(playerId);
        return data.getBalance(currency);
    }
    
    public boolean setBalance(UUID playerId, String currency, BigDecimal amount) {
        if (!economyEnabled || amount.compareTo(BigDecimal.ZERO) < 0) {
            return false;
        }
        
        PlayerEconomyData data = getPlayerData(playerId);
        BigDecimal oldBalance = data.getBalance(currency);
        data.setBalance(currency, amount);
        
        // Record transaction
        transactionManager.recordTransaction(
            playerId, null, currency, amount.subtract(oldBalance),
            TransactionType.ADMIN_SET, "Balance set by admin"
        );
        
        savePlayerData(playerId);
        return true;
    }
    
    public boolean addBalance(UUID playerId, String currency, BigDecimal amount) {
        if (!economyEnabled || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        
        PlayerEconomyData data = getPlayerData(playerId);
        BigDecimal newBalance = data.getBalance(currency).add(amount);
        data.setBalance(currency, newBalance);
        
        // Record transaction
        transactionManager.recordTransaction(
            playerId, null, currency, amount,
            TransactionType.ADMIN_GIVE, "Balance added by admin"
        );
        
        savePlayerData(playerId);
        return true;
    }
    
    public boolean removeBalance(UUID playerId, String currency, BigDecimal amount) {
        if (!economyEnabled || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        
        PlayerEconomyData data = getPlayerData(playerId);
        BigDecimal currentBalance = data.getBalance(currency);
        
        if (currentBalance.compareTo(amount) < 0) {
            return false; // Insufficient funds
        }
        
        BigDecimal newBalance = currentBalance.subtract(amount);
        data.setBalance(currency, newBalance);
        
        // Record transaction
        transactionManager.recordTransaction(
            playerId, null, currency, amount.negate(),
            TransactionType.ADMIN_TAKE, "Balance removed by admin"
        );
        
        savePlayerData(playerId);
        return true;
    }
    
    public boolean transferMoney(UUID fromPlayer, UUID toPlayer, String currency, BigDecimal amount) {
        if (!economyEnabled || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        
        PlayerEconomyData fromData = getPlayerData(fromPlayer);
        PlayerEconomyData toData = getPlayerData(toPlayer);
        
        BigDecimal fromBalance = fromData.getBalance(currency);
        if (fromBalance.compareTo(amount) < 0) {
            return false; // Insufficient funds
        }
        
        // Calculate transfer fee
        BigDecimal fee = calculateTransferFee(amount);
        BigDecimal totalDeduction = amount.add(fee);
        
        if (fromBalance.compareTo(totalDeduction) < 0) {
            return false; // Insufficient funds including fee
        }
        
        // Process transfer
        fromData.setBalance(currency, fromBalance.subtract(totalDeduction));
        toData.setBalance(currency, toData.getBalance(currency).add(amount));
        
        // Record transactions
        transactionManager.recordTransaction(
            fromPlayer, toPlayer, currency, amount.negate(),
            TransactionType.TRANSFER_SEND, "Money transfer to player"
        );
        
        transactionManager.recordTransaction(
            toPlayer, fromPlayer, currency, amount,
            TransactionType.TRANSFER_RECEIVE, "Money transfer from player"
        );
        
        if (fee.compareTo(BigDecimal.ZERO) > 0) {
            transactionManager.recordTransaction(
                fromPlayer, null, currency, fee.negate(),
                TransactionType.FEE, "Transfer fee"
            );
        }
        
        savePlayerData(fromPlayer);
        savePlayerData(toPlayer);
        return true;
    }
    
    private BigDecimal calculateTransferFee(BigDecimal amount) {
        // Get fee rate from config
        com.zerog.neoessentials.config.MainConfig.EconomySettings config = 
            com.zerog.neoessentials.config.ConfigManager.getInstance().getMainConfig().economySettings;
        
        BigDecimal feeRate = BigDecimal.valueOf(config.transactionFeePercent / 100.0);
        BigDecimal fee = amount.multiply(feeRate);
        BigDecimal maxFee = BigDecimal.valueOf(config.maxTransferAmount * 0.1); // 10% of max transfer as max fee

        return fee.min(maxFee);
    }
    
    // Player Data Management
    private PlayerEconomyData getPlayerData(UUID playerId) {
        return playerData.computeIfAbsent(playerId, id -> {
            PlayerEconomyData data = loadPlayerDataFromStorage(id);
            if (data == null) {
                data = new PlayerEconomyData(id);
                // Set starting balance from config for primary currency
                com.zerog.neoessentials.config.MainConfig.EconomySettings config = 
                    com.zerog.neoessentials.config.ConfigManager.getInstance().getMainConfig().economySettings;
                data.setBalance(currencyManager.getPrimaryCurrency(), BigDecimal.valueOf(config.startingBalance));
            }
            return data;
        });
    }
    
    private PlayerEconomyData loadPlayerDataFromStorage(UUID playerId) {
        try {
            // Load from StorageManager (returns Map<String, Object>)
            Map<String, Object> rawData = storageManager.loadPlayerEconomy(playerId).get();
            
            if (rawData != null && !rawData.isEmpty()) {
                // Convert raw data to PlayerEconomyData
                PlayerEconomyData playerData = new PlayerEconomyData(playerId);
                
                // Extract balance data
                if (rawData.containsKey("balances")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> balances = (Map<String, Object>) rawData.get("balances");
                    for (Map.Entry<String, Object> entry : balances.entrySet()) {
                        String currency = entry.getKey();
                        double balance = ((Number) entry.getValue()).doubleValue();
                        playerData.setBalance(currency, BigDecimal.valueOf(balance));
                    }
                }
                
                return playerData;
            }
            
            return null;
        } catch (Exception e) {
            LOGGER.error("Failed to load player economy data for " + playerId, e);
            return null;
        }
    }
    
    private void savePlayerData(UUID playerId) {
        PlayerEconomyData data = playerData.get(playerId);
        if (data != null) {
            try {
                // Convert PlayerEconomyData to Map<String, Object> for StorageManager
                Map<String, Object> rawData = new HashMap<>();
                Map<String, Object> balances = new HashMap<>();
                
                // Convert all balances to the raw format
                Map<String, BigDecimal> allBalances = data.getAllBalances();
                for (Map.Entry<String, BigDecimal> entry : allBalances.entrySet()) {
                    balances.put(entry.getKey(), entry.getValue().doubleValue());
                }
                
                rawData.put("balances", balances);
                rawData.put("lastSaved", System.currentTimeMillis());
                
                // Save asynchronously
                storageManager.savePlayerEconomy(playerId, rawData).thenRun(() -> {
                    LOGGER.debug("Saved economy data for player " + playerId);
                }).exceptionally(throwable -> {
                    LOGGER.error("Failed to save player economy data for " + playerId, throwable);
                    return null;
                });
                
            } catch (Exception e) {
                LOGGER.error("Failed to save player economy data for " + playerId, e);
            }
        }
    }
    
    private void loadPlayerData() {
        try {
            // For now, we'll load data on-demand as players join
            // The StorageManager doesn't have a "load all" method 
            // so we'll rely on individual player data loading
            LOGGER.info("Economy data will be loaded on-demand as players join the server");
        } catch (Exception e) {
            LOGGER.error("Error initializing economy data loading", e);
        }
    }
    
    private void updateEconomyAnalytics() {
        try {
            analytics.update(playerData, transactionManager.getRecentTransactions());
        } catch (Exception e) {
            LOGGER.error("Failed to update economy analytics", e);
        }
    }
    
    // Public getters for managers
    public CurrencyManager getCurrencyManager() { return currencyManager; }
    public TransactionManager getTransactionManager() { return transactionManager; }
    // public ShopManager getShopManager() { return shopManager; } // Now uses managers.EconomyManager
    // Auction manager removed as per feature cleanup
    public EconomyAnalytics getAnalytics() { return analytics; }
    
    // Compatibility methods for GUI classes (using default currency)
    public BigDecimal getBalance(UUID playerId) {
        return getBalance(playerId, currencyManager.getPrimaryCurrency());
    }
    
    public boolean hasBalance(UUID playerId, BigDecimal amount) {
        BigDecimal balance = getBalance(playerId);
        return balance.compareTo(amount) >= 0;
    }
    
    public boolean hasBalance(UUID playerId, double amount) {
        return hasBalance(playerId, BigDecimal.valueOf(amount));
    }
    
    public boolean withdrawBalance(UUID playerId, BigDecimal amount, String reason) {
        return removeBalance(playerId, currencyManager.getPrimaryCurrency(), amount);
    }
    
    public boolean withdrawBalance(UUID playerId, double amount, String reason) {
        return withdrawBalance(playerId, BigDecimal.valueOf(amount), reason);
    }
    
    public String formatCurrency(BigDecimal amount) {
        com.zerog.neoessentials.economy.currency.Currency primaryCurrency = 
            currencyManager.getCurrency(currencyManager.getPrimaryCurrency());
        return primaryCurrency != null ? primaryCurrency.formatAmount(amount) : amount.toString();
    }
    
    public String formatCurrency(double amount) {
        return formatCurrency(BigDecimal.valueOf(amount));
    }
    
    public boolean isEnabled() { return economyEnabled; }
    
    public void shutdown() {
        try {
            economyEnabled = false;
            
            // Save all player data
            for (UUID playerId : playerData.keySet()) {
                savePlayerData(playerId);
            }
            
            // Shutdown managers
            // Auction manager removed as per feature cleanup
            
            LOGGER.info("Economy System shutdown completed");
        } catch (Exception e) {
            LOGGER.error("Error during economy shutdown", e);
        }
    }
}
