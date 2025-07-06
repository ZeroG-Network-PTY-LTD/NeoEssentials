package com.zerog.neoessentials.economy;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.config.EconomyConfig;
import com.zerog.neoessentials.economy.auction.AuctionManager;
import com.zerog.neoessentials.economy.external.ExternalEconomyDetector;
import com.zerog.neoessentials.economy.external.EconomyDetectionReport;
import com.zerog.neoessentials.economy.storage.EconomyStorage;
import com.zerog.neoessentials.economy.storage.EconomyStorageFactory;
import com.zerog.neoessentials.economy.shop.ShopManager;
import net.minecraft.server.MinecraftServer;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Main manager for the economy system.
 * Handles all economic operations and coordinates between components.
 */
public class EconomyManager {
    
    private final EconomyConfig config;
    private final EconomyStorage storage;
    private final ExternalEconomyDetector externalDetector;
    private final Currency defaultCurrency;
    private final ScheduledExecutorService executor;
    private final ShopManager shopManager;
    private final AuctionManager auctionManager;
    
    // Account cache for performance
    private final Map<UUID, EconomyAccount> accountCache = new ConcurrentHashMap<>();
    private final long cacheExpirationMs;
    
    private boolean enabled = false;
    private boolean initialized = false;
    
    public EconomyManager(EconomyConfig config, Path dataDirectory) {
        this.config = Objects.requireNonNull(config, "Economy config cannot be null");
        this.storage = EconomyStorageFactory.createStorage(config, dataDirectory);
        this.externalDetector = new ExternalEconomyDetector();
        this.executor = Executors.newScheduledThreadPool(2);
        this.cacheExpirationMs = config.getCacheExpirationMinutes() * 60 * 1000L;
        
        // Create default currency from config
        this.defaultCurrency = Currency.createBasic(
            "default",
            config.getCurrencyName(),
            config.getCurrencySymbol(),
            config.getCurrencyPluralName()
        );
        this.shopManager = new ShopManager(this);
        this.auctionManager = new AuctionManager(this);
    }
    
    /**
     * Initializes the economy system
     */
    public boolean initialize() {
        try {
            NeoEssentials.LOGGER.info("Initializing Economy System...");
            
            // Check for external economy mods
            EconomyDetectionReport detectionReport = externalDetector.getDetectionReport();
            
            if (config.isAutoDisableOnExternal() && detectionReport.hasExternalEconomy()) {
                NeoEssentials.LOGGER.info("External economy detected: {}. Disabling internal economy.", 
                        detectionReport.getDetectedMods());
                enabled = false;
                initialized = true;
                return true;
            }
            
            // Initialize storage
            if (!storage.initialize()) {
                NeoEssentials.LOGGER.error("Failed to initialize economy storage");
                return false;
            }
            
            // Start background tasks
            startBackgroundTasks();
            
            enabled = config.isEnabled();
            initialized = true;
            
            NeoEssentials.LOGGER.info("Economy System initialized successfully. Enabled: {}", enabled);
            return true;
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to initialize Economy System", e);
            return false;
        }
    }
    
    /**
     * Shuts down the economy system
     */
    public void shutdown() {
        if (!initialized) return;
        
        try {
            NeoEssentials.LOGGER.info("Shutting down Economy System...");
            
            // Shutdown executor
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            
            // Save all cached accounts
            saveAllCachedAccounts();
            
            // Close storage
            storage.close();
            
            initialized = false;
            enabled = false;
            
            NeoEssentials.LOGGER.info("Economy System shut down successfully");
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error during Economy System shutdown", e);
        }
    }
    
    /**
     * Gets or creates an account for a player
     */
    public EconomyAccount getOrCreateAccount(UUID playerId, String playerName) {
        Objects.requireNonNull(playerId, "Player ID cannot be null");
        Objects.requireNonNull(playerName, "Player name cannot be null");
        
        if (!isEnabled()) {
            return null;
        }
        
        // Check cache first
        EconomyAccount cached = accountCache.get(playerId);
        if (cached != null) {
            return cached;
        }
        
        // Try to load from storage
        Optional<EconomyAccount> loaded = storage.loadAccount(playerId);
        if (loaded.isPresent()) {
            EconomyAccount account = loaded.get();
            cacheAccount(account);
            return account;
        }
        
        // Create new account
        EconomyAccount newAccount = new EconomyAccount(playerId, playerName);
        newAccount.setBalance(defaultCurrency, config.getStartingBalance());
        
        // Save to storage
        storage.saveAccount(newAccount);
        
        // Cache the account
        cacheAccount(newAccount);
        
        NeoEssentials.LOGGER.debug("Created new economy account for player: {}", playerName);
        return newAccount;
    }
    
    /**
     * Gets an account by player ID
     */
    public Optional<EconomyAccount> getAccount(UUID playerId) {
        if (!isEnabled()) {
            return Optional.empty();
        }
        
        // Check cache first
        EconomyAccount cached = accountCache.get(playerId);
        if (cached != null) {
            return Optional.of(cached);
        }
        
        // Load from storage
        Optional<EconomyAccount> loaded = storage.loadAccount(playerId);
        if (loaded.isPresent()) {
            cacheAccount(loaded.get());
        }
        
        return loaded;
    }
    
    /**
     * Transfers money between accounts
     */
    public boolean transferMoney(UUID fromPlayerId, UUID toPlayerId, BigDecimal amount, Currency currency, String description) {
        if (!isEnabled() || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        
        try {
            EconomyAccount fromAccount = getAccount(fromPlayerId).orElse(null);
            EconomyAccount toAccount = getAccount(toPlayerId).orElse(null);
            
            if (fromAccount == null || toAccount == null) {
                return false;
            }
            
            if (!fromAccount.hasBalance(currency, amount)) {
                return false;
            }
            
            // Perform the transfer
            if (fromAccount.subtractBalance(currency, amount)) {
                toAccount.addBalance(currency, amount);
                
                // Log the transaction
                Transaction transaction = Transaction.builder()
                        .fromAccount(fromPlayerId)
                        .toAccount(toPlayerId)
                        .amount(amount)
                        .currency(currency)
                        .type(Transaction.Type.PAYMENT)
                        .description(description)
                        .build();
                
                storage.logTransaction(transaction);
                
                // Save accounts
                storage.saveAccount(fromAccount);
                storage.saveAccount(toAccount);
                
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error transferring money", e);
            return false;
        }
    }
    
    /**
     * Adds money to an account
     */
    public boolean addMoney(UUID playerId, BigDecimal amount, Currency currency, String description) {
        if (!isEnabled() || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        
        try {
            EconomyAccount account = getAccount(playerId).orElse(null);
            if (account == null) {
                return false;
            }
            
            account.addBalance(currency, amount);
            
            // Log the transaction
            Transaction transaction = Transaction.builder()
                    .fromAccount(UUID.fromString("00000000-0000-0000-0000-000000000000")) // System account
                    .toAccount(playerId)
                    .amount(amount)
                    .currency(currency)
                    .type(Transaction.Type.ADMIN_GIVE)
                    .description(description)
                    .build();
            
            storage.logTransaction(transaction);
            storage.saveAccount(account);
            
            return true;
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error adding money", e);
            return false;
        }
    }
    
    /**
     * Subtracts money from an account
     */
    public boolean subtractMoney(UUID playerId, BigDecimal amount, Currency currency, String description) {
        if (!isEnabled() || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        
        try {
            EconomyAccount account = getAccount(playerId).orElse(null);
            if (account == null) {
                return false;
            }
            
            if (!account.hasBalance(currency, amount)) {
                return false;
            }
            
            if (account.subtractBalance(currency, amount)) {
                // Log the transaction
                Transaction transaction = Transaction.builder()
                        .fromAccount(playerId)
                        .toAccount(UUID.fromString("00000000-0000-0000-0000-000000000000")) // System account
                        .amount(amount)
                        .currency(currency)
                        .type(Transaction.Type.ADMIN_TAKE)
                        .description(description)
                        .build();
                
                storage.logTransaction(transaction);
                storage.saveAccount(account);
                
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error subtracting money", e);
            return false;
        }
    }
    
    /**
     * Sets the balance of an account
     */
    public boolean setBalance(UUID playerId, BigDecimal amount, Currency currency, String description) {
        if (!isEnabled() || amount.compareTo(BigDecimal.ZERO) < 0) {
            return false;
        }
        
        try {
            EconomyAccount account = getAccount(playerId).orElse(null);
            if (account == null) {
                return false;
            }
            
            BigDecimal oldBalance = account.getBalance(currency);
            account.setBalance(currency, amount);
            
            // Log the transaction
            Transaction transaction = Transaction.builder()
                    .fromAccount(UUID.fromString("00000000-0000-0000-0000-000000000000")) // System account
                    .toAccount(playerId)
                    .amount(amount)
                    .currency(currency)
                    .type(Transaction.Type.ADMIN_SET)
                    .description(description + " (Old: " + currency.format(oldBalance) + ")")
                    .build();
            
            storage.logTransaction(transaction);
            storage.saveAccount(account);
            
            return true;
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error setting balance", e);
            return false;
        }
    }
    
    /**
     * Gets the top accounts by balance
     */
    public List<EconomyAccount> getTopAccounts(int limit) {
        if (!isEnabled()) {
            return new ArrayList<>();
        }
        
        try {
            List<EconomyAccount> allAccounts = storage.getAllAccounts();
            
            return allAccounts.stream()
                    .sorted((a, b) -> b.getBalance(defaultCurrency).compareTo(a.getBalance(defaultCurrency)))
                    .limit(limit)
                    .toList();
                    
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error getting top accounts", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Gets transaction history for a player
     */
    public List<Transaction> getTransactionHistory(UUID playerId, int limit) {
        if (!isEnabled()) {
            return new ArrayList<>();
        }
        
        return storage.getTransactionHistory(playerId, limit);
    }
    
    private void cacheAccount(EconomyAccount account) {
        accountCache.put(account.getPlayerId(), account);
        
        // Schedule cache expiration
        executor.schedule(() -> {
            accountCache.remove(account.getPlayerId());
        }, cacheExpirationMs, TimeUnit.MILLISECONDS);
    }
    
    private void saveAllCachedAccounts() {
        for (EconomyAccount account : accountCache.values()) {
            storage.saveAccount(account);
        }
        accountCache.clear();
    }
    
    private void startBackgroundTasks() {
        // Periodic cache cleanup
        executor.scheduleAtFixedRate(() -> {
            try {
                // Save accounts periodically
                saveAllCachedAccounts();
            } catch (Exception e) {
                NeoEssentials.LOGGER.error("Error in periodic account save", e);
            }
        }, 5, 5, TimeUnit.MINUTES);
        
        // Periodic maintenance
        if (config.isEnableBackups()) {
            executor.scheduleAtFixedRate(() -> {
                try {
                    storage.backup();
                    storage.performMaintenance();
                } catch (Exception e) {
                    NeoEssentials.LOGGER.error("Error in periodic maintenance", e);
                }
            }, config.getBackupIntervalHours(), config.getBackupIntervalHours(), TimeUnit.HOURS);
        }
        
        // External economy detection
        executor.scheduleAtFixedRate(() -> {
            try {
                if (config.isAutoDisableOnExternal()) {
                    boolean hasExternal = externalDetector.detectExternalEconomy();
                    if (hasExternal && enabled) {
                        NeoEssentials.LOGGER.info("External economy detected during runtime. Disabling internal economy.");
                        enabled = false;
                    }
                }
            } catch (Exception e) {
                NeoEssentials.LOGGER.error("Error in external economy detection", e);
            }
        }, config.getDetectionCheckIntervalMinutes(), config.getDetectionCheckIntervalMinutes(), TimeUnit.MINUTES);
    }
    
    // Getters
    public boolean isEnabled() { return enabled && initialized; }
    public boolean isInitialized() { return initialized; }
    public EconomyConfig getConfig() { return config; }
    public Currency getDefaultCurrency() { return defaultCurrency; }
    public EconomyStorage getStorage() { return storage; }
    public ExternalEconomyDetector getExternalDetector() { return externalDetector; }
    public ShopManager getShopManager() { return shopManager; }
    public AuctionManager getAuctionManager() { return auctionManager; }
    public ScheduledExecutorService getScheduler() { return executor; }
    
    /**
     * Formats a currency amount for display
     */
    public String formatCurrency(BigDecimal amount) {
        return defaultCurrency.format(amount);
    }
    
    /**
     * Gets the current server instance
     */
    public MinecraftServer getServer() {
        return NeoEssentials.getInstance().getServer();
    }
}