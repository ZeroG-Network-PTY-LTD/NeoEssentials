package com.zerog.neoessentials.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zerog.neoessentials.NeoEssentials;

import java.io.*;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuration class for the econ    // Storage getters/setters
    public StorageType getStorageType() { return storageType; }
    public void setStorageType(StorageType storageType) { this.storageType = storageType; }
    
    public String getStorageTypeString() { return storageType.name().toLowerCase(); }
    public void setStorageTypeString(String storageType) { 
        try {
            this.storageType = StorageType.valueOf(storageType.toUpperCase());
        } catch (IllegalArgumentException e) {
            this.storageType = StorageType.SQLITE; // Default fallback
        }
    }
    
    public DatabaseConfig getDatabaseConfig() { return databaseConfig; }
    public void setDatabaseConfig(DatabaseConfig databaseConfig) { this.databaseConfig = databaseConfig; }
    
    public int getBackupIntervalHours() { return backupIntervalHours; }
    public void setBackupIntervalHours(int backupIntervalHours) { 
        this.backupIntervalHours = Math.max(1, backupIntervalHours); 
    }
    
    public boolean isEnableBackups() { return enableBackups; }
    public void setEnableBackups(boolean enableBackups) { this.enableBackups = enableBackups; }rovides all necessary settings for economy functionality.
 */
public class EconomyConfig {
    
    public enum StorageType {
        JSON, SQLITE, MYSQL
    }
    
    // Economy system settings
    private boolean enabled = true;
    private boolean autoDisableOnExternal = true;
    
    // Currency settings
    private String currencyName = "Coin";
    private String currencyPluralName = "Coins";
    private String currencySymbol = "⛃";
    private int decimalPlaces = 2;
    
    // Balance settings
    private BigDecimal startingBalance = BigDecimal.valueOf(1000);
    private BigDecimal maxBalance = BigDecimal.valueOf(1000000);
    private BigDecimal minBalance = BigDecimal.ZERO;
    
    // Storage settings
    private StorageType storageType = StorageType.SQLITE; // sqlite, mysql, json
    private int backupIntervalHours = 24;
    private boolean enableBackups = true;
    private DatabaseConfig databaseConfig;
    
    // GUI settings
    private String guiTheme = "default";
    private boolean enableAnimations = true;
    private boolean enableSounds = true;
    private int pageSize = 45; // Items per page in GUIs
    
    // Shop settings
    private int maxShopsPerPlayer = 5;
    private BigDecimal shopCreationCost = BigDecimal.valueOf(1000);
    private int maxItemsPerShop = 27;
    private boolean enablePlayerShops = true;
    private boolean enableAdminShops = true;
    
    // Auction settings
    private boolean enableAuctions = true;
    private int maxAuctionsPerPlayer = 3;
    private int maxAuctionDurationHours = 168; // 1 week
    private BigDecimal listingFeePercentage = BigDecimal.valueOf(2.5);
    private int minAuctionDurationHours = 1;
    
    // Banking settings (optional features)
    private boolean enableBanking = false;
    private boolean enableLoans = false;
    private BigDecimal interestRate = BigDecimal.valueOf(0.01); // 1% daily
    private BigDecimal loanInterestRate = BigDecimal.valueOf(0.05); // 5% daily
    
    // Transaction settings
    private boolean enableTransactionLogging = true;
    private int maxTransactionHistoryDays = 30;
    private boolean enableTransactionBroadcasts = false;
    private BigDecimal broadcastThreshold = BigDecimal.valueOf(10000);
    
    // Detection settings
    private int detectionCheckIntervalMinutes = 5;
    private boolean verboseDetectionLogging = false;
    
    // Performance settings
    private int cacheSize = 1000; // Number of accounts to cache
    private int cacheExpirationMinutes = 30;
    private boolean asyncTransactions = true;
    
    /**
     * Default configuration file path
     */
    private static final String CONFIG_FILE_NAME = "economy.json";
    
    // Default constructor
    public EconomyConfig() {
        // Configuration loaded from files will override these defaults
    }
    
    // Economy system getters/setters
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public boolean isAutoDisableOnExternal() { return autoDisableOnExternal; }
    public void setAutoDisableOnExternal(boolean autoDisableOnExternal) { this.autoDisableOnExternal = autoDisableOnExternal; }
    
    // Currency getters/setters
    public String getCurrencyName() { return currencyName; }
    public void setCurrencyName(String currencyName) { this.currencyName = currencyName; }
    
    public String getCurrencyPluralName() { return currencyPluralName; }
    public void setCurrencyPluralName(String currencyPluralName) { this.currencyPluralName = currencyPluralName; }
    
    public String getCurrencySymbol() { return currencySymbol; }
    public void setCurrencySymbol(String currencySymbol) { this.currencySymbol = currencySymbol; }
    
    public int getDecimalPlaces() { return decimalPlaces; }
    public void setDecimalPlaces(int decimalPlaces) { this.decimalPlaces = Math.max(0, decimalPlaces); }
    
    // Balance getters/setters
    public BigDecimal getStartingBalance() { return startingBalance; }
    public void setStartingBalance(BigDecimal startingBalance) { 
        this.startingBalance = startingBalance.max(BigDecimal.ZERO); 
    }
    
    public BigDecimal getMaxBalance() { return maxBalance; }
    public void setMaxBalance(BigDecimal maxBalance) { 
        this.maxBalance = maxBalance.max(BigDecimal.ZERO); 
    }
    
    public BigDecimal getMinBalance() { return minBalance; }
    public void setMinBalance(BigDecimal minBalance) { this.minBalance = minBalance; }
    
    // Storage getters/setters
    public StorageType getStorageType() { return storageType; }
    public void setStorageType(StorageType storageType) { this.storageType = storageType; }
    
    public String getStorageTypeString() { return storageType.name().toLowerCase(); }
    public void setStorageTypeString(String storageType) { 
        try {
            this.storageType = StorageType.valueOf(storageType.toUpperCase());
        } catch (IllegalArgumentException e) {
            this.storageType = StorageType.SQLITE; // Default fallback
        }
    }
    
    public DatabaseConfig getDatabaseConfig() { return databaseConfig; }
    public void setDatabaseConfig(DatabaseConfig databaseConfig) { this.databaseConfig = databaseConfig; }
    
    public int getBackupIntervalHours() { return backupIntervalHours; }
    public void setBackupIntervalHours(int backupIntervalHours) { 
        this.backupIntervalHours = Math.max(1, backupIntervalHours); 
    }
    
    public boolean isEnableBackups() { return enableBackups; }
    public void setEnableBackups(boolean enableBackups) { this.enableBackups = enableBackups; }
    
    // GUI getters/setters
    public String getGuiTheme() { return guiTheme; }
    public void setGuiTheme(String guiTheme) { this.guiTheme = guiTheme; }
    
    public boolean isEnableAnimations() { return enableAnimations; }
    public void setEnableAnimations(boolean enableAnimations) { this.enableAnimations = enableAnimations; }
    
    public boolean isEnableSounds() { return enableSounds; }
    public void setEnableSounds(boolean enableSounds) { this.enableSounds = enableSounds; }
    
    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = Math.max(9, Math.min(54, pageSize)); }
    
    // Shop getters/setters
    public int getMaxShopsPerPlayer() { return maxShopsPerPlayer; }
    public void setMaxShopsPerPlayer(int maxShopsPerPlayer) { 
        this.maxShopsPerPlayer = Math.max(0, maxShopsPerPlayer); 
    }
    
    public BigDecimal getShopCreationCost() { return shopCreationCost; }
    public void setShopCreationCost(BigDecimal shopCreationCost) { 
        this.shopCreationCost = shopCreationCost.max(BigDecimal.ZERO); 
    }
    
    public int getMaxItemsPerShop() { return maxItemsPerShop; }
    public void setMaxItemsPerShop(int maxItemsPerShop) { 
        this.maxItemsPerShop = Math.max(1, Math.min(54, maxItemsPerShop)); 
    }
    
    public boolean isEnablePlayerShops() { return enablePlayerShops; }
    public void setEnablePlayerShops(boolean enablePlayerShops) { this.enablePlayerShops = enablePlayerShops; }
    
    public boolean isEnableAdminShops() { return enableAdminShops; }
    public void setEnableAdminShops(boolean enableAdminShops) { this.enableAdminShops = enableAdminShops; }
    
    // Auction getters/setters
    public boolean isEnableAuctions() { return enableAuctions; }
    public void setEnableAuctions(boolean enableAuctions) { this.enableAuctions = enableAuctions; }
    
    public int getMaxAuctionsPerPlayer() { return maxAuctionsPerPlayer; }
    public void setMaxAuctionsPerPlayer(int maxAuctionsPerPlayer) { 
        this.maxAuctionsPerPlayer = Math.max(0, maxAuctionsPerPlayer); 
    }
    
    public int getMaxAuctionDurationHours() { return maxAuctionDurationHours; }
    public void setMaxAuctionDurationHours(int maxAuctionDurationHours) { 
        this.maxAuctionDurationHours = Math.max(1, maxAuctionDurationHours); 
    }
    
    public BigDecimal getListingFeePercentage() { return listingFeePercentage; }
    public void setListingFeePercentage(BigDecimal listingFeePercentage) { 
        this.listingFeePercentage = listingFeePercentage.max(BigDecimal.ZERO); 
    }
    
    public int getMinAuctionDurationHours() { return minAuctionDurationHours; }
    public void setMinAuctionDurationHours(int minAuctionDurationHours) { 
        this.minAuctionDurationHours = Math.max(1, minAuctionDurationHours); 
    }
    
    // Banking getters/setters
    public boolean isEnableBanking() { return enableBanking; }
    public void setEnableBanking(boolean enableBanking) { this.enableBanking = enableBanking; }
    
    public boolean isEnableLoans() { return enableLoans; }
    public void setEnableLoans(boolean enableLoans) { this.enableLoans = enableLoans; }
    
    public BigDecimal getInterestRate() { return interestRate; }
    public void setInterestRate(BigDecimal interestRate) { 
        this.interestRate = interestRate.max(BigDecimal.ZERO); 
    }
    
    public BigDecimal getLoanInterestRate() { return loanInterestRate; }
    public void setLoanInterestRate(BigDecimal loanInterestRate) { 
        this.loanInterestRate = loanInterestRate.max(BigDecimal.ZERO); 
    }
    
    // Transaction getters/setters
    public boolean isEnableTransactionLogging() { return enableTransactionLogging; }
    public void setEnableTransactionLogging(boolean enableTransactionLogging) { 
        this.enableTransactionLogging = enableTransactionLogging; 
    }
    
    public int getMaxTransactionHistoryDays() { return maxTransactionHistoryDays; }
    public void setMaxTransactionHistoryDays(int maxTransactionHistoryDays) { 
        this.maxTransactionHistoryDays = Math.max(1, maxTransactionHistoryDays); 
    }
    
    public boolean isEnableTransactionBroadcasts() { return enableTransactionBroadcasts; }
    public void setEnableTransactionBroadcasts(boolean enableTransactionBroadcasts) { 
        this.enableTransactionBroadcasts = enableTransactionBroadcasts; 
    }
    
    public BigDecimal getBroadcastThreshold() { return broadcastThreshold; }
    public void setBroadcastThreshold(BigDecimal broadcastThreshold) { 
        this.broadcastThreshold = broadcastThreshold.max(BigDecimal.ZERO); 
    }
    
    // Detection getters/setters
    public int getDetectionCheckIntervalMinutes() { return detectionCheckIntervalMinutes; }
    public void setDetectionCheckIntervalMinutes(int detectionCheckIntervalMinutes) { 
        this.detectionCheckIntervalMinutes = Math.max(1, detectionCheckIntervalMinutes); 
    }
    
    public boolean isVerboseDetectionLogging() { return verboseDetectionLogging; }
    public void setVerboseDetectionLogging(boolean verboseDetectionLogging) { 
        this.verboseDetectionLogging = verboseDetectionLogging; 
    }
    
    // Performance getters/setters
    public int getCacheSize() { return cacheSize; }
    public void setCacheSize(int cacheSize) { this.cacheSize = Math.max(10, cacheSize); }
    
    public int getCacheExpirationMinutes() { return cacheExpirationMinutes; }
    public void setCacheExpirationMinutes(int cacheExpirationMinutes) { 
        this.cacheExpirationMinutes = Math.max(1, cacheExpirationMinutes); 
    }
    
    public boolean isAsyncTransactions() { return asyncTransactions; }
    public void setAsyncTransactions(boolean asyncTransactions) { this.asyncTransactions = asyncTransactions; }
    
    /**
     * Validates the configuration and returns any issues found
     */
    public String validateConfiguration() {
        StringBuilder issues = new StringBuilder();
        
        if (maxBalance.compareTo(startingBalance) < 0) {
            issues.append("Max balance cannot be less than starting balance. ");
        }
        
        if (minBalance.compareTo(BigDecimal.ZERO) < 0) {
            issues.append("Min balance cannot be negative. ");
        }
        
        if (maxAuctionDurationHours < minAuctionDurationHours) {
            issues.append("Max auction duration cannot be less than min auction duration. ");
        }
        
        if (listingFeePercentage.compareTo(BigDecimal.valueOf(100)) > 0) {
            issues.append("Listing fee percentage cannot exceed 100%. ");
        }
        
        return issues.toString().trim();
    }
    
    /**
     * Loads configuration from file or creates default if not exists
     */
    public static EconomyConfig loadFromFile(Path configDirectory) {
        Path configFile = configDirectory.resolve(CONFIG_FILE_NAME);
        
        if (!Files.exists(configFile)) {
            NeoEssentials.LOGGER.info("Economy config file not found, creating default: " + configFile);
            EconomyConfig defaultConfig = new EconomyConfig();
            defaultConfig.saveToFile(configDirectory);
            return defaultConfig;
        }
        
        try {
            String content = Files.readString(configFile);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            EconomyConfig config = gson.fromJson(content, EconomyConfig.class);
            
            if (config == null) {
                NeoEssentials.LOGGER.warn("Failed to parse economy config, using defaults");
                return new EconomyConfig();
            }
            
            String validation = config.validateConfiguration();
            if (!validation.isEmpty()) {
                NeoEssentials.LOGGER.warn("Economy config validation issues: " + validation);
            }
            
            NeoEssentials.LOGGER.info("Loaded economy configuration from: " + configFile);
            return config;
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to load economy config from: " + configFile, e);
            return new EconomyConfig();
        }
    }
    
    /**
     * Saves configuration to file
     */
    public boolean saveToFile(Path configDirectory) {
        try {
            Files.createDirectories(configDirectory);
            Path configFile = configDirectory.resolve(CONFIG_FILE_NAME);
            
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(this);
            
            Files.writeString(configFile, json);
            NeoEssentials.LOGGER.info("Saved economy configuration to: " + configFile);
            return true;
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to save economy config", e);
            return false;
        }
    }
    
    /**
     * Reloads configuration from file
     */
    public static EconomyConfig reloadFromFile(Path configDirectory) {
        NeoEssentials.LOGGER.info("Reloading economy configuration");
        return loadFromFile(configDirectory);
    }

    @Override
    public String toString() {
        return String.format("EconomyConfig{enabled=%s, currency=%s, startingBalance=%s}", 
                enabled, currencyName, startingBalance);
    }
}
