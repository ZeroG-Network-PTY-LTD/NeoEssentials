package com.zerog.neoessentials.economy;

import com.zerog.neoessentials.NeoEssentialsMod;
import com.zerog.neoessentials.economy.bank.BankManager;
import com.zerog.neoessentials.economy.currency.CurrencyManager;
import com.zerog.neoessentials.economy.transactions.TransactionManager;
import com.zerog.neoessentials.economy.market.MarketManager;
// import com.zerog.neoessentials.economy.shops.ShopManager; // Now uses managers.EconomyManager
import com.zerog.neoessentials.economy.auction.AuctionManager;
import com.zerog.neoessentials.config.EconomyConfig;
import com.zerog.neoessentials.storage.StorageManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    private static EconomyManager instance;
    
    // Core managers
    private final CurrencyManager currencyManager;
    private final BankManager bankManager;
    private final TransactionManager transactionManager;
    private final MarketManager marketManager;
    // private final ShopManager shopManager; // Now uses managers.EconomyManager
    private final AuctionManager auctionManager;
    
    // Configuration and data
    private final EconomyConfig config;
    private final StorageManager storageManager;
    
    // Player balances and economy data
    private final Map<UUID, PlayerEconomyData> playerData;
    private final Map<String, EconomyServer> serverConnections;
    
    // Economy statistics and analytics
    private EconomyAnalytics analytics;
    private LocalDateTime lastUpdate;
    private boolean economyEnabled;
    
    public EconomyManager() {
        this.config = new EconomyConfig();
        this.storageManager = StorageManager.getInstance();
        this.playerData = new ConcurrentHashMap<>();
        this.serverConnections = new ConcurrentHashMap<>();
        
        // Initialize managers
        this.currencyManager = new CurrencyManager(this);
        this.bankManager = new BankManager(this);
        this.transactionManager = new TransactionManager(this);
        this.marketManager = new MarketManager(this);
        // ShopManager now uses managers.EconomyManager - removed initialization
        this.auctionManager = new AuctionManager(this);
        
        this.analytics = new EconomyAnalytics();
        this.lastUpdate = LocalDateTime.now();
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
            NeoEssentialsMod.LOGGER.info("Initializing Advanced Economy System...");
            
            // Load configuration
            config.load();
            
            // Initialize currency system
            currencyManager.initialize();
            
            // Initialize banking system
            bankManager.initialize();
            
            // Load player data
            loadPlayerData();
            
            // Initialize market systems
            marketManager.initialize();
            // shopManager.initialize(); // Now uses managers.EconomyManager
            auctionManager.initialize();
            
            // Start background tasks
            startBackgroundTasks();
            
            NeoEssentialsMod.LOGGER.info("Economy System initialized successfully");
            
        } catch (Exception e) {
            NeoEssentialsMod.LOGGER.error("Failed to initialize Economy System", e);
            economyEnabled = false;
        }
    }
    
    private void startBackgroundTasks() {
        // Start interest calculation task
        CompletableFuture.runAsync(() -> {
            while (economyEnabled) {
                try {
                    Thread.sleep(config.getInterestUpdateInterval() * 1000);
                    bankManager.processInterest();
                    updateEconomyAnalytics();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    NeoEssentialsMod.LOGGER.error("Error in economy background task", e);
                }
            }
        });
        
        // Start market update task
        CompletableFuture.runAsync(() -> {
            while (economyEnabled) {
                try {
                    Thread.sleep(config.getMarketUpdateInterval() * 1000);
                    marketManager.updatePrices();
                    auctionManager.processExpiredAuctions();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    NeoEssentialsMod.LOGGER.error("Error in market update task", e);
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
        BigDecimal feeRate = config.getTransferFeeRate();
        BigDecimal fee = amount.multiply(feeRate);
        BigDecimal maxFee = config.getMaxTransferFee();
        
        return fee.min(maxFee);
    }
    
    // Player Data Management
    private PlayerEconomyData getPlayerData(UUID playerId) {
        return playerData.computeIfAbsent(playerId, id -> {
            PlayerEconomyData data = loadPlayerDataFromStorage(id);
            if (data == null) {
                data = new PlayerEconomyData(id);
                // Set default balance for primary currency
                data.setBalance(currencyManager.getPrimaryCurrency(), config.getStartingBalance());
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
            NeoEssentialsMod.LOGGER.error("Failed to load player economy data for " + playerId, e);
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
                    NeoEssentialsMod.LOGGER.debug("Saved economy data for player " + playerId);
                }).exceptionally(throwable -> {
                    NeoEssentialsMod.LOGGER.error("Failed to save player economy data for " + playerId, throwable);
                    return null;
                });
                
            } catch (Exception e) {
                NeoEssentialsMod.LOGGER.error("Failed to save player economy data for " + playerId, e);
            }
        }
    }
    
    private void loadPlayerData() {
        try {
            // For now, we'll load data on-demand as players join
            // The StorageManager doesn't have a "load all" method 
            // so we'll rely on individual player data loading
            NeoEssentialsMod.LOGGER.info("Economy data will be loaded on-demand as players join the server");
        } catch (Exception e) {
            NeoEssentialsMod.LOGGER.error("Error initializing economy data loading", e);
        }
    }
    
    private void updateEconomyAnalytics() {
        try {
            analytics.update(playerData, transactionManager.getRecentTransactions());
        } catch (Exception e) {
            NeoEssentialsMod.LOGGER.error("Failed to update economy analytics", e);
        }
    }
    
    // Public getters for managers
    public CurrencyManager getCurrencyManager() { return currencyManager; }
    public BankManager getBankManager() { return bankManager; }
    public TransactionManager getTransactionManager() { return transactionManager; }
    public MarketManager getMarketManager() { return marketManager; }
    // public ShopManager getShopManager() { return shopManager; } // Now uses managers.EconomyManager
    public AuctionManager getAuctionManager() { return auctionManager; }
    public EconomyAnalytics getAnalytics() { return analytics; }
    public EconomyConfig getConfig() { return config; }
    
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
            bankManager.shutdown();
            marketManager.shutdown();
            auctionManager.shutdown();
            
            NeoEssentialsMod.LOGGER.info("Economy System shutdown completed");
        } catch (Exception e) {
            NeoEssentialsMod.LOGGER.error("Error during economy shutdown", e);
        }
    }
}
