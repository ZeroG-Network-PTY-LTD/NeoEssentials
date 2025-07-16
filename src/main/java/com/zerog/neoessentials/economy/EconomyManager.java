package com.zerog.neoessentials.economy;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.config.EconomyConfig;
import com.zerog.neoessentials.economy.auction.AuctionManager;
import com.zerog.neoessentials.economy.external.ExternalEconomyDetector;
import com.zerog.neoessentials.economy.external.EconomyDetectionReport;
import com.zerog.neoessentials.economy.storage.EconomyStorage;
import com.zerog.neoessentials.economy.storage.EconomyStorageFactory;
import com.zerog.neoessentials.economy.shop.ShopManager;
<<<<<<< HEAD
import com.zerog.neoessentials.economy.shop.ShopItem;
=======
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
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
    private final TransactionLogger transactionLogger;
    
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
        this.transactionLogger = new TransactionLogger(dataDirectory.toFile());
    }
    
    /**
     * Initializes the economy system
     */
    public boolean initialize() {
        try {
            NeoEssentials.LOGGER.info("Initializing Economy System...");
            
<<<<<<< HEAD
            // Validate configuration
            if (config == null) {
                NeoEssentials.LOGGER.error("Economy config is null - cannot initialize");
                return false;
            }
            
=======
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
            // Check for external economy mods
            EconomyDetectionReport detectionReport = externalDetector.getDetectionReport();
            
            if (config.isAutoDisableOnExternal() && detectionReport.hasExternalEconomy()) {
                NeoEssentials.LOGGER.info("External economy detected: {}. Disabling internal economy.", 
                        detectionReport.getDetectedMods());
                enabled = false;
                initialized = true;
                return true;
            }
            
<<<<<<< HEAD
            // Initialize storage with retry logic
            int maxRetries = 3;
            boolean storageInitialized = false;
            
            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                try {
                    NeoEssentials.LOGGER.info("Initializing economy storage (attempt {}/{})", attempt, maxRetries);
                    if (storage.initialize()) {
                        storageInitialized = true;
                        break;
                    }
                } catch (Exception e) {
                    NeoEssentials.LOGGER.warn("Storage initialization attempt {} failed", attempt, e);
                    if (attempt < maxRetries) {
                        // Wait before retry
                        try {
                            Thread.sleep(1000 * attempt); // Progressive delay
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }
            
            if (!storageInitialized) {
                NeoEssentials.LOGGER.error("Failed to initialize economy storage after {} attempts", maxRetries);
                return false;
            }
            
            // Validate storage functionality
            if (!validateStorageFunctionality()) {
                NeoEssentials.LOGGER.error("Storage validation failed");
                return false;
            }
            
            // Initialize currency
            if (defaultCurrency == null) {
                NeoEssentials.LOGGER.error("Default currency is null - cannot initialize economy");
                return false;
            }
            
            // Initialize shop manager
            if (shopManager == null) {
                NeoEssentials.LOGGER.error("Shop manager is null - cannot initialize economy");
=======
            // Initialize storage
            if (!storage.initialize()) {
                NeoEssentials.LOGGER.error("Failed to initialize economy storage");
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
                return false;
            }
            
            // Start background tasks
            startBackgroundTasks();
            
<<<<<<< HEAD
            // Enable economy if configured
            if (config.isEnabled()) {
                enabled = true;
                
                // Add default shop items if enabled and shop is empty
                try {
                    List<ShopItem> existingItems = shopManager.getAllItems();
                    if (existingItems.isEmpty()) {
                        NeoEssentials.LOGGER.info("Shop is empty, adding default items");
                        com.zerog.neoessentials.economy.shop.ShopUtils.addDefaultShopItems(this);
                    } else {
                        NeoEssentials.LOGGER.info("Found {} existing shop items", existingItems.size());
                    }
                } catch (Exception e) {
                    NeoEssentials.LOGGER.warn("Failed to add default shop items", e);
                }
            } else {
                NeoEssentials.LOGGER.info("Economy is disabled in configuration");
                enabled = false;
            }
            
            initialized = true;
            
            // Log system status
            NeoEssentials.LOGGER.info("Economy System initialized successfully");
            NeoEssentials.LOGGER.info("  - Enabled: {}", enabled);
            NeoEssentials.LOGGER.info("  - Default Currency: {}", defaultCurrency.getName());
            NeoEssentials.LOGGER.info("  - Storage Type: {}", storage.getClass().getSimpleName());
            NeoEssentials.LOGGER.info("  - Shop Items: {}", shopManager.getAllItems().size());
            
=======
            // Add default shop items if enabled
            if (config.isEnabled()) {
                com.zerog.neoessentials.economy.shop.ShopUtils.addDefaultShopItems(this);
            }
            
            enabled = config.isEnabled();
            initialized = true;
            
            NeoEssentials.LOGGER.info("Economy System initialized successfully. Enabled: {}", enabled);
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
            return true;
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to initialize Economy System", e);
<<<<<<< HEAD
            enabled = false;
            initialized = false;
            return false;
        }
    }
    
    /**
     * Validates that storage is working correctly
     */
    public boolean validateStorageFunctionality() {
        try {
            // Test creating a temporary account
            UUID testUUID = UUID.fromString("00000000-0000-0000-0000-000000000001");
            EconomyAccount testAccount = new EconomyAccount(testUUID, "TestAccount");
            testAccount.setBalance(defaultCurrency, BigDecimal.valueOf(100));
            
            // Test saving
            if (!storage.saveAccount(testAccount)) {
                NeoEssentials.LOGGER.error("Storage save test failed");
                return false;
            }
            
            // Test loading
            Optional<EconomyAccount> loaded = storage.loadAccount(testUUID);
            if (loaded.isEmpty()) {
                NeoEssentials.LOGGER.error("Storage load test failed - account not found");
                return false;
            }
            
            if (!loaded.get().getBalance(defaultCurrency).equals(BigDecimal.valueOf(100))) {
                NeoEssentials.LOGGER.error("Storage load test failed - balance mismatch");
                return false;
            }
            
            // Clean up test account
            try {
                storage.deleteAccount(testUUID);
            } catch (Exception e) {
                NeoEssentials.LOGGER.warn("Failed to clean up test account", e);
            }
            
            NeoEssentials.LOGGER.info("Storage validation successful");
            return true;
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Storage validation failed", e);
=======
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
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
            
            // Shutdown transaction logger
            transactionLogger.shutdown();
            
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
<<<<<<< HEAD
            NeoEssentials.LOGGER.warn("Economy system is disabled - cannot create account for player: {}", playerName);
            return null;
        }
        
        if (!initialized) {
            NeoEssentials.LOGGER.warn("Economy system is not initialized - cannot create account for player: {}", playerName);
            return null;
        }
        
        try {
            // Check cache first
            EconomyAccount cached = accountCache.get(playerId);
            if (cached != null) {
                // Update player name if it has changed
                if (!cached.getPlayerName().equals(playerName)) {
                    NeoEssentials.LOGGER.info("Updating player name from {} to {} for UUID {}", 
                        cached.getPlayerName(), playerName, playerId);
                    // Create updated account
                    EconomyAccount updated = new EconomyAccount(playerId, playerName);
                    updated.setBalance(defaultCurrency, cached.getBalance(defaultCurrency));
                    updated.setStatus(cached.getStatus());
                    // Copy metadata
                    for (Map.Entry<String, String> entry : cached.getAllMetadata().entrySet()) {
                        updated.setMetadata(entry.getKey(), entry.getValue());
                    }
                    storage.saveAccount(updated);
                    cacheAccount(updated);
                    return updated;
                }
                return cached;
            }
            
            // Try to load from storage
            Optional<EconomyAccount> loaded = storage.loadAccount(playerId);
            if (loaded.isPresent()) {
                EconomyAccount account = loaded.get();
                
                // Validate loaded account
                if (!account.isValid()) {
                    NeoEssentials.LOGGER.warn("Loaded invalid account for player: {}, creating new one", playerName);
                } else {
                    // Update player name if it has changed
                    if (!account.getPlayerName().equals(playerName)) {
                        NeoEssentials.LOGGER.info("Updating player name from {} to {} for UUID {}", 
                            account.getPlayerName(), playerName, playerId);
                        account = new EconomyAccount(playerId, playerName);
                        account.setBalance(defaultCurrency, loaded.get().getBalance(defaultCurrency));
                        account.setStatus(loaded.get().getStatus());
                        storage.saveAccount(account);
                    }
                    
                    cacheAccount(account);
                    NeoEssentials.LOGGER.debug("Loaded existing account for player: {}", playerName);
                    return account;
                }
            }
            
            // Create new account
            EconomyAccount newAccount = new EconomyAccount(playerId, playerName);
            
            // Set starting balance
            BigDecimal startingBalance = config.getStartingBalance();
            if (startingBalance.compareTo(BigDecimal.ZERO) > 0) {
                newAccount.setBalance(defaultCurrency, startingBalance);
                NeoEssentials.LOGGER.info("Created new account for player {} with starting balance: {}", 
                    playerName, formatCurrency(startingBalance));
            } else {
                newAccount.setBalance(defaultCurrency, BigDecimal.ZERO);
                NeoEssentials.LOGGER.info("Created new account for player {} with zero balance", playerName);
            }
            
            // Set creation metadata
            newAccount.setMetadata("created_version", NeoEssentials.getInstance().getVersion());
            newAccount.setMetadata("created_timestamp", String.valueOf(System.currentTimeMillis()));
            
            // Save to storage
            if (!storage.saveAccount(newAccount)) {
                NeoEssentials.LOGGER.error("Failed to save new account for player: {}", playerName);
                return null;
            }
            
            // Cache the account
            cacheAccount(newAccount);
            
            NeoEssentials.LOGGER.info("Successfully created new economy account for player: {}", playerName);
            return newAccount;
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error getting or creating account for player: " + playerName, e);
            return null;
        }
=======
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
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
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
                transactionLogger.logTransaction(transaction);
                
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
<<<<<<< HEAD
        if (!isEnabled() || !isValidTransactionAmount(amount)) {
            NeoEssentials.LOGGER.warn("Invalid add money request: enabled={}, amount={}", isEnabled(), amount);
            return false;
        }
        
        if (currency == null) {
            currency = defaultCurrency;
        }
        
        if (!isCurrencySupported(currency)) {
            NeoEssentials.LOGGER.warn("Unsupported currency: {}", currency.getName());
=======
        if (!isEnabled() || amount.compareTo(BigDecimal.ZERO) <= 0) {
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
            return false;
        }
        
        try {
<<<<<<< HEAD
            // Get account (will create if needed)
            String playerName = getPlayerName(playerId);
            EconomyAccount account = getOrCreateAccount(playerId, playerName);
            if (account == null) {
                NeoEssentials.LOGGER.error("Failed to get account for player: {}", playerId);
                return false;
            }
            
            if (!account.canTransact()) {
                NeoEssentials.LOGGER.warn("Account cannot transact: {} (status: {})", 
                    account.getPlayerName(), account.getStatus());
                return false;
            }
            
            BigDecimal oldBalance = account.getBalance(currency);
            account.addBalance(currency, amount);
            BigDecimal newBalance = account.getBalance(currency);
=======
            EconomyAccount account = getAccount(playerId).orElse(null);
            if (account == null) {
                return false;
            }
            
            account.addBalance(currency, amount);
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
            
            // Log the transaction
            Transaction transaction = Transaction.builder()
                    .fromAccount(UUID.fromString("00000000-0000-0000-0000-000000000000")) // System account
                    .toAccount(playerId)
                    .amount(amount)
                    .currency(currency)
                    .type(Transaction.Type.ADMIN_GIVE)
<<<<<<< HEAD
                    .description(description != null ? description : "Money added by system")
                    .build();
            
            // Save everything atomically
            if (storage.saveAccount(account) && storage.logTransaction(transaction)) {
                transactionLogger.logTransaction(transaction);
                
                NeoEssentials.LOGGER.info("Added {} to player {} (old: {}, new: {}): {}", 
                    currency.format(amount), account.getPlayerName(), 
                    currency.format(oldBalance), currency.format(newBalance), description);
                return true;
            } else {
                // Rollback on failure
                account.setBalance(currency, oldBalance);
                NeoEssentials.LOGGER.error("Failed to save account or transaction for add money operation");
                return false;
            }
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error adding money to player " + playerId, e);
=======
                    .description(description)
                    .build();
            
            storage.logTransaction(transaction);
            transactionLogger.logTransaction(transaction);
            storage.saveAccount(account);
            
            return true;
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error adding money", e);
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
            return false;
        }
    }
    
    /**
     * Subtracts money from an account
     */
    public boolean subtractMoney(UUID playerId, BigDecimal amount, Currency currency, String description) {
<<<<<<< HEAD
        if (!isEnabled() || !isValidTransactionAmount(amount)) {
            NeoEssentials.LOGGER.warn("Invalid subtract money request: enabled={}, amount={}", isEnabled(), amount);
            return false;
        }
        
        if (currency == null) {
            currency = defaultCurrency;
        }
        
        if (!isCurrencySupported(currency)) {
            NeoEssentials.LOGGER.warn("Unsupported currency: {}", currency.getName());
=======
        if (!isEnabled() || amount.compareTo(BigDecimal.ZERO) <= 0) {
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
            return false;
        }
        
        try {
            EconomyAccount account = getAccount(playerId).orElse(null);
            if (account == null) {
<<<<<<< HEAD
                NeoEssentials.LOGGER.warn("No account found for player: {}", playerId);
                return false;
            }
            
            if (!account.canTransact()) {
                NeoEssentials.LOGGER.warn("Account cannot transact: {} (status: {})", 
                    account.getPlayerName(), account.getStatus());
=======
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
                return false;
            }
            
            if (!account.hasBalance(currency, amount)) {
<<<<<<< HEAD
                NeoEssentials.LOGGER.debug("Insufficient balance for player {}: has {}, needs {}", 
                    account.getPlayerName(), currency.format(account.getBalance(currency)), currency.format(amount));
                return false;
            }
            
            BigDecimal oldBalance = account.getBalance(currency);
            
            if (account.subtractBalance(currency, amount)) {
                BigDecimal newBalance = account.getBalance(currency);
                
=======
                return false;
            }
            
            if (account.subtractBalance(currency, amount)) {
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
                // Log the transaction
                Transaction transaction = Transaction.builder()
                        .fromAccount(playerId)
                        .toAccount(UUID.fromString("00000000-0000-0000-0000-000000000000")) // System account
                        .amount(amount)
                        .currency(currency)
                        .type(Transaction.Type.ADMIN_TAKE)
<<<<<<< HEAD
                        .description(description != null ? description : "Money subtracted by system")
                        .build();
                
                // Save everything atomically
                if (storage.saveAccount(account) && storage.logTransaction(transaction)) {
                    transactionLogger.logTransaction(transaction);
                    
                    NeoEssentials.LOGGER.info("Subtracted {} from player {} (old: {}, new: {}): {}", 
                        currency.format(amount), account.getPlayerName(), 
                        currency.format(oldBalance), currency.format(newBalance), description);
                    return true;
                } else {
                    // Rollback on failure
                    account.setBalance(currency, oldBalance);
                    NeoEssentials.LOGGER.error("Failed to save account or transaction for subtract money operation");
                    return false;
                }
=======
                        .description(description)
                        .build();
                
                storage.logTransaction(transaction);
                transactionLogger.logTransaction(transaction);
                storage.saveAccount(account);
                
                return true;
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
            }
            
            return false;
            
        } catch (Exception e) {
<<<<<<< HEAD
            NeoEssentials.LOGGER.error("Error subtracting money from player " + playerId, e);
=======
            NeoEssentials.LOGGER.error("Error subtracting money", e);
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
            return false;
        }
    }
    
    /**
<<<<<<< HEAD
     * Gets player name from server
     */
    private String getPlayerName(UUID playerId) {
        try {
            MinecraftServer server = getServer();
            if (server != null) {
                var playerProfile = server.getProfileCache().get(playerId);
                if (playerProfile.isPresent()) {
                    return playerProfile.get().getName();
                }
            }
            return "Unknown Player";
        } catch (Exception e) {
            return "Unknown Player";
        }
    }
    
    /**
=======
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
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
            
            // Log the balance change
            transactionLogger.logBalanceChange(playerId, account.getPlayerName(), currency, 
                oldBalance, amount, description);
            
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
            transactionLogger.logTransaction(transaction);
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
    public Currency getDefaultCurrency() {
        return defaultCurrency;
    }
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
    
    /**
     * Validates that a currency is supported by this economy system
     */
    public boolean isCurrencySupported(Currency currency) {
        return currency != null && currency.equals(defaultCurrency);
    }
    
    /**
     * Creates a properly configured currency instance
     */
    public Currency createCurrency(String name, String symbol, String pluralName) {
        return Currency.createBasic(name.toLowerCase(), name, symbol, pluralName);
    }
    
    /**
     * Gets account balance in the default currency
     */
    public BigDecimal getBalance(UUID playerId) {
        EconomyAccount account = getAccount(playerId).orElse(null);
        if (account == null) {
            return BigDecimal.ZERO;
        }
        return account.getBalance(defaultCurrency);
    }
    
    /**
     * Checks if a player has enough balance for a transaction
     */
    public boolean hasBalance(UUID playerId, BigDecimal amount) {
        EconomyAccount account = getAccount(playerId).orElse(null);
        if (account == null) {
            return false;
        }
        return account.hasBalance(defaultCurrency, amount);
    }
    
    /**
     * Validates that a player account exists and is accessible
     */
    public boolean validatePlayerAccount(UUID playerId) {
        if (playerId == null) {
            NeoEssentials.LOGGER.warn("Attempted to validate null player ID");
            return false;
        }
        
        if (!enabled) {
            NeoEssentials.LOGGER.warn("Economy system is disabled - cannot validate account for player: {}", playerId);
            return false;
        }
        
        try {
            Optional<EconomyAccount> accountOpt = getAccount(playerId);
            return accountOpt.isPresent() && accountOpt.get().isValid();
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to validate player account: {}", playerId, e);
            return false;
        }
    }
    
    /**
     * Checks if a transaction amount is valid (positive and not too large)
     */
    public boolean isValidTransactionAmount(BigDecimal amount) {
        if (amount == null) {
            return false;
        }
        
        // Check if amount is positive
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        
        // Check if amount is reasonable (not more than 1 trillion)
        BigDecimal maxAmount = new BigDecimal("1000000000000");
        if (amount.compareTo(maxAmount) > 0) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Performs a safe money transfer between accounts with validation
     */
    public boolean safeTransfer(UUID fromPlayer, UUID toPlayer, BigDecimal amount, Currency currency, String description) {
        if (!enabled) {
            NeoEssentials.LOGGER.warn("Economy system is disabled - cannot perform transfer");
            return false;
        }
        
        if (!isValidTransactionAmount(amount)) {
            NeoEssentials.LOGGER.warn("Invalid transaction amount: {}", amount);
            return false;
        }
        
        if (!validatePlayerAccount(fromPlayer) || !validatePlayerAccount(toPlayer)) {
            NeoEssentials.LOGGER.warn("Invalid player accounts for transfer: {} -> {}", fromPlayer, toPlayer);
            return false;
        }
        
        try {
            // Check if sender has sufficient balance
            if (!hasBalance(fromPlayer, amount)) {
                NeoEssentials.LOGGER.debug("Insufficient balance for transfer: {} needs {}", fromPlayer, amount);
                return false;
            }
            
            // Perform the transfer atomically
            boolean deducted = subtractMoney(fromPlayer, amount, currency, description);
            if (!deducted) {
                NeoEssentials.LOGGER.warn("Failed to deduct money from sender: {}", fromPlayer);
                return false;
            }
            
            boolean added = addMoney(toPlayer, amount, currency, description);
            if (!added) {
                // Rollback deduction if adding fails
                addMoney(fromPlayer, amount, currency, "Transfer rollback: " + description);
                NeoEssentials.LOGGER.warn("Failed to add money to recipient: {}, rolled back", toPlayer);
                return false;
            }
            
            NeoEssentials.LOGGER.info("Successfully transferred {} {} from {} to {}: {}", 
                amount, currency.getSymbol(), fromPlayer, toPlayer, description);
            return true;
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error during safe transfer", e);
            return false;
        }
    }
    
    /**
     * Gets the transaction logger
     */
    public TransactionLogger getTransactionLogger() {
        return transactionLogger;
    }
<<<<<<< HEAD
    
    /**
     * Performs comprehensive economy system diagnostics
     */
    public List<String> performSystemDiagnostics() {
        List<String> diagnostics = new ArrayList<>();
        
        try {
            // Basic system status
            diagnostics.add("System Status:");
            diagnostics.add("  - Initialized: " + initialized);
            diagnostics.add("  - Enabled: " + enabled);
            diagnostics.add("  - Config Present: " + (config != null));
            diagnostics.add("  - Storage Present: " + (storage != null));
            diagnostics.add("  - Default Currency: " + (defaultCurrency != null ? defaultCurrency.getName() : "NULL"));
            
            // Storage diagnostics
            if (storage != null) {
                diagnostics.add("Storage Information:");
                diagnostics.add("  - Type: " + storage.getClass().getSimpleName());
                try {
                    List<EconomyAccount> allAccounts = storage.getAllAccounts();
                    diagnostics.add("  - Total Accounts: " + allAccounts.size());
                    
                    if (!allAccounts.isEmpty()) {
                        BigDecimal totalMoney = allAccounts.stream()
                            .map(acc -> acc.getBalance(defaultCurrency))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                        diagnostics.add("  - Total Money in Economy: " + formatCurrency(totalMoney));
                        
                        EconomyAccount richest = allAccounts.stream()
                            .max((a, b) -> a.getBalance(defaultCurrency).compareTo(b.getBalance(defaultCurrency)))
                            .orElse(null);
                        if (richest != null) {
                            diagnostics.add("  - Richest Player: " + richest.getPlayerName() + " with " + 
                                formatCurrency(richest.getBalance(defaultCurrency)));
                        }
                    }
                } catch (Exception e) {
                    diagnostics.add("  - Error accessing storage data: " + e.getMessage());
                }
            }
            
            // Cache diagnostics
            diagnostics.add("Cache Information:");
            diagnostics.add("  - Cached Accounts: " + accountCache.size());
            diagnostics.add("  - Cache Expiration: " + (cacheExpirationMs / 60000) + " minutes");
            
            // Shop diagnostics
            if (shopManager != null) {
                diagnostics.add("Shop Manager Status: Available");
            } else {
                diagnostics.add("Shop Manager Status: NULL");
            }
            
            // External economy check
            if (externalDetector != null) {
                EconomyDetectionReport report = externalDetector.getDetectionReport();
                diagnostics.add("External Economy Status:");
                diagnostics.add("  - Has External Economy: " + report.hasExternalEconomy());
                if (report.hasExternalEconomy()) {
                    diagnostics.add("  - Detected Mods: " + report.getDetectedMods());
                }
            }
            
            // Test basic functionality
            diagnostics.add("Functionality Tests:");
            diagnostics.addAll(testBasicFunctionality());
            
        } catch (Exception e) {
            diagnostics.add("Error during system diagnostics: " + e.getMessage());
        }
        
        return diagnostics;
    }
    
    /**
     * Tests basic economy functionality
     */
    private List<String> testBasicFunctionality() {
        List<String> tests = new ArrayList<>();
        try {
            // Test 1: Currency validation
            boolean currencyTest = isCurrencySupported(defaultCurrency);
            tests.add("  - Currency Support Test: " + (currencyTest ? "PASS" : "FAIL"));
            
            // Test 2: Amount validation
            boolean amountTest = isValidTransactionAmount(BigDecimal.valueOf(100));
            tests.add("  - Amount Validation Test: " + (amountTest ? "PASS" : "FAIL"));
            
            // Test 3: Storage connectivity
            boolean storageTest = storage != null && initialized;
            tests.add("  - Storage Connectivity Test: " + (storageTest ? "PASS" : "FAIL"));
            
            // Test 4: Account cache functionality
            boolean cacheTest = accountCache != null;
            tests.add("  - Cache Functionality Test: " + (cacheTest ? "PASS" : "FAIL"));
            
            // Test 5: Transaction logger
            boolean loggerTest = transactionLogger != null;
            tests.add("  - Transaction Logger Test: " + (loggerTest ? "PASS" : "FAIL"));
            
        } catch (Exception e) {
            tests.add("  - Error during functionality tests: " + e.getMessage());
        }
        return tests;
    }
    
    /**
     * Force enables the economy system (admin override)
     */
    public boolean forceEnable() {
        try {
            if (!initialized) {
                NeoEssentials.LOGGER.warn("Cannot force enable - system not initialized");
                return false;
            }
            
            enabled = true;
            NeoEssentials.LOGGER.info("Economy system force enabled by admin");
            return true;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error force enabling economy", e);
            return false;
        }
    }
    
    /**
     * Force disables the economy system (admin override)
     */
    public boolean forceDisable() {
        try {
            enabled = false;
            NeoEssentials.LOGGER.info("Economy system force disabled by admin");
            return true;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error force disabling economy", e);
            return false;
        }
    }
    
    /**
     * Reloads the economy system
     */
    public boolean reload() {
        try {
            NeoEssentials.LOGGER.info("Reloading economy system...");
            
            // Save all cached accounts
            saveAllCachedAccounts();
            
            // Clear cache
            accountCache.clear();
            
            // Re-initialize storage
            if (!storage.initialize()) {
                NeoEssentials.LOGGER.error("Failed to re-initialize storage during reload");
                return false;
            }
            
            // Validate storage
            if (!validateStorageFunctionality()) {
                NeoEssentials.LOGGER.error("Storage validation failed during reload");
                return false;
            }
            
            NeoEssentials.LOGGER.info("Economy system reloaded successfully");
            return true;
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error reloading economy system", e);
            return false;
        }
    }
    
    /**
     * Gets the total number of accounts in the system
     */
    public int getTotalAccounts() {
        try {
            if (storage == null) return 0;
            return storage.getAllAccounts().size();
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error getting total accounts", e);
            return 0;
        }
    }
    
    /**
     * Gets the storage type name
     */
    public String getStorageType() {
        return storage != null ? storage.getClass().getSimpleName() : "Unknown";
    }
=======
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
}