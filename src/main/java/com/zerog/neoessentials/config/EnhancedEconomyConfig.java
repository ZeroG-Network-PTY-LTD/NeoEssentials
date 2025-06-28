package com.zerog.neoessentials.config;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.HashMap;

/**
 * Enhanced economy configuration manager that loads from YAML files.
 * Provides comprehensive configuration for the v1.0.2 economy system.
 */
public class EnhancedEconomyConfig {
    
    private Map<String, Object> config;
    private static EnhancedEconomyConfig instance;
    
    // Economy settings
    private boolean economyEnabled;
    private double startingBalance;
    private double maxBalance;
    private boolean allowNegativeBalances;
    private double inflationRate;
    
    // Banking settings
    private boolean bankingEnabled;
    private double accountCreationFee;
    private int maxAccountsPerPlayer;
    private boolean autoCreateChecking;
    
    // Loan settings
    private boolean loansEnabled;
    private double startingCreditScore;
    private double minCreditScore;
    private double maxCreditScore;
    private int creditUpdateInterval;
    
    // Shop settings
    private boolean shopsEnabled;
    private double shopCreationFee;
    private int maxShopsPerPlayer;
    private double dailyRentalFee;
    private double salesTaxRate;
    
    // Auction settings
    private boolean auctionsEnabled;
    private int minAuctionDuration;
    private int maxAuctionDuration;
    private int defaultAuctionDuration;
    private double minBidIncrement;
    private double listingFeeRate;
    private double successFeeRate;
    
    // Transaction settings
    private int historyRetention;
    private double maxTransactionAmount;
    private double dailyLimit;
    private int transactionCooldown;
    
    // Analytics settings
    private boolean analyticsEnabled;
    private int updateInterval;
    private int retentionPeriod;
    private boolean dailyReports;
    private boolean weeklyReports;
    private boolean monthlyReports;
    
    // Performance settings
    private int threadPoolSize;
    private int queueSize;
    private int playerDataCacheSize;
    private int cacheExpiry;
    private int connectionPoolSize;
    private int queryTimeout;
    
    private EnhancedEconomyConfig() {
        loadDefaults();
    }
    
    public static EnhancedEconomyConfig getInstance() {
        if (instance == null) {
            instance = new EnhancedEconomyConfig();
        }
        return instance;
    }
    
    /**
     * Load configuration from YAML file
     */
    public void loadFromFile(String filePath) {
        try {
            Yaml yaml = new Yaml(new Constructor(Map.class));
            try (InputStream inputStream = new FileInputStream(filePath)) {
                config = yaml.load(inputStream);
                parseConfig();
            }
        } catch (IOException e) {
            System.err.println("Failed to load economy config from " + filePath + ": " + e.getMessage());
            loadDefaults();
        }
    }
    
    /**
     * Load default configuration values
     */
    private void loadDefaults() {
        // Economy defaults
        economyEnabled = true;
        startingBalance = 100.0;
        maxBalance = 1000000.0;
        allowNegativeBalances = false;
        inflationRate = 0.02;
        
        // Banking defaults
        bankingEnabled = true;
        accountCreationFee = 100.0;
        maxAccountsPerPlayer = 5;
        autoCreateChecking = true;
        
        // Loan defaults
        loansEnabled = true;
        startingCreditScore = 750.0;
        minCreditScore = 300.0;
        maxCreditScore = 850.0;
        creditUpdateInterval = 24;
        
        // Shop defaults
        shopsEnabled = true;
        shopCreationFee = 500.0;
        maxShopsPerPlayer = 5;
        dailyRentalFee = 50.0;
        salesTaxRate = 0.05;
        
        // Auction defaults
        auctionsEnabled = true;
        minAuctionDuration = 1;
        maxAuctionDuration = 168;
        defaultAuctionDuration = 24;
        minBidIncrement = 0.05;
        listingFeeRate = 0.02;
        successFeeRate = 0.05;
        
        // Transaction defaults
        historyRetention = 365;
        maxTransactionAmount = 100000.0;
        dailyLimit = 500000.0;
        transactionCooldown = 1;
        
        // Analytics defaults
        analyticsEnabled = true;
        updateInterval = 1;
        retentionPeriod = 365;
        dailyReports = true;
        weeklyReports = true;
        monthlyReports = true;
        
        // Performance defaults
        threadPoolSize = 2;
        queueSize = 1000;
        playerDataCacheSize = 1000;
        cacheExpiry = 30;
        connectionPoolSize = 10;
        queryTimeout = 30;
    }
    
    /**
     * Parse loaded YAML configuration
     */
    @SuppressWarnings("unchecked")
    private void parseConfig() {
        if (config == null) {
            loadDefaults();
            return;
        }
        
        // Parse economy settings
        Map<String, Object> economy = (Map<String, Object>) config.get("economy");
        if (economy != null) {
            economyEnabled = getBoolean(economy, "enabled", true);
            startingBalance = getDouble(economy, "starting_balance", 100.0);
            maxBalance = getDouble(economy, "max_balance", 1000000.0);
            allowNegativeBalances = getBoolean(economy, "allow_negative_balances", false);
            inflationRate = getDouble(economy, "inflation_rate", 0.02);
        }
        
        // Parse banking settings
        Map<String, Object> banking = (Map<String, Object>) config.get("banking");
        if (banking != null) {
            bankingEnabled = getBoolean(banking, "enabled", true);
            
            Map<String, Object> accountCreation = (Map<String, Object>) banking.get("account_creation");
            if (accountCreation != null) {
                accountCreationFee = getDouble(accountCreation, "creation_fee", 100.0);
                maxAccountsPerPlayer = getInt(accountCreation, "max_accounts_per_player", 5);
                autoCreateChecking = getBoolean(accountCreation, "auto_create_checking", true);
            }
        }
        
        // Parse loan settings
        Map<String, Object> loans = (Map<String, Object>) config.get("loans");
        if (loans != null) {
            loansEnabled = getBoolean(loans, "enabled", true);
            
            Map<String, Object> creditScoring = (Map<String, Object>) loans.get("credit_scoring");
            if (creditScoring != null) {
                startingCreditScore = getDouble(creditScoring, "starting_score", 750.0);
                minCreditScore = getDouble(creditScoring, "min_score", 300.0);
                maxCreditScore = getDouble(creditScoring, "max_score", 850.0);
                creditUpdateInterval = getInt(creditScoring, "update_interval", 24);
            }
        }
        
        // Parse shop settings
        Map<String, Object> shops = (Map<String, Object>) config.get("shops");
        if (shops != null) {
            shopsEnabled = getBoolean(shops, "enabled", true);
            
            Map<String, Object> creation = (Map<String, Object>) shops.get("creation");
            if (creation != null) {
                shopCreationFee = getDouble(creation, "creation_fee", 500.0);
                maxShopsPerPlayer = getInt(creation, "max_shops_per_player", 5);
                dailyRentalFee = getDouble(creation, "rental_fee", 50.0);
            }
            
            Map<String, Object> taxation = (Map<String, Object>) shops.get("taxation");
            if (taxation != null) {
                salesTaxRate = getDouble(taxation, "sales_tax_rate", 0.05);
            }
        }
        
        // Parse auction settings
        Map<String, Object> auctions = (Map<String, Object>) config.get("auctions");
        if (auctions != null) {
            auctionsEnabled = getBoolean(auctions, "enabled", true);
            
            Map<String, Object> settings = (Map<String, Object>) auctions.get("settings");
            if (settings != null) {
                minAuctionDuration = getInt(settings, "min_duration", 1);
                maxAuctionDuration = getInt(settings, "max_duration", 168);
                defaultAuctionDuration = getInt(settings, "default_duration", 24);
                minBidIncrement = getDouble(settings, "min_bid_increment", 0.05);
            }
            
            Map<String, Object> fees = (Map<String, Object>) auctions.get("fees");
            if (fees != null) {
                listingFeeRate = getDouble(fees, "listing_fee_rate", 0.02);
                successFeeRate = getDouble(fees, "success_fee_rate", 0.05);
            }
        }
        
        // Parse transaction settings
        Map<String, Object> transactions = (Map<String, Object>) config.get("transactions");
        if (transactions != null) {
            historyRetention = getInt(transactions, "history_retention", 365);
            
            Map<String, Object> limits = (Map<String, Object>) transactions.get("limits");
            if (limits != null) {
                maxTransactionAmount = getDouble(limits, "max_transaction_amount", 100000.0);
                dailyLimit = getDouble(limits, "daily_limit", 500000.0);
                transactionCooldown = getInt(limits, "cooldown", 1);
            }
        }
        
        // Parse analytics settings
        Map<String, Object> analytics = (Map<String, Object>) config.get("analytics");
        if (analytics != null) {
            analyticsEnabled = getBoolean(analytics, "enabled", true);
            
            Map<String, Object> collection = (Map<String, Object>) analytics.get("collection");
            if (collection != null) {
                updateInterval = getInt(collection, "update_interval", 1);
                retentionPeriod = getInt(collection, "retention_period", 365);
            }
            
            Map<String, Object> reporting = (Map<String, Object>) analytics.get("reporting");
            if (reporting != null) {
                dailyReports = getBoolean(reporting, "daily_reports", true);
                weeklyReports = getBoolean(reporting, "weekly_reports", true);
                monthlyReports = getBoolean(reporting, "monthly_reports", true);
            }
        }
        
        // Parse performance settings
        Map<String, Object> performance = (Map<String, Object>) config.get("performance");
        if (performance != null) {
            Map<String, Object> backgroundTasks = (Map<String, Object>) performance.get("background_tasks");
            if (backgroundTasks != null) {
                threadPoolSize = getInt(backgroundTasks, "thread_pool_size", 2);
                queueSize = getInt(backgroundTasks, "queue_size", 1000);
            }
            
            Map<String, Object> caching = (Map<String, Object>) performance.get("caching");
            if (caching != null) {
                playerDataCacheSize = getInt(caching, "player_data_cache_size", 1000);
                cacheExpiry = getInt(caching, "cache_expiry", 30);
            }
            
            Map<String, Object> database = (Map<String, Object>) performance.get("database");
            if (database != null) {
                connectionPoolSize = getInt(database, "connection_pool_size", 10);
                queryTimeout = getInt(database, "query_timeout", 30);
            }
        }
    }
    
    // Helper methods for safe value extraction
    private boolean getBoolean(Map<String, Object> map, String key, boolean defaultValue) {
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }
    
    private double getDouble(Map<String, Object> map, String key, double defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }
    
    private int getInt(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }
    
    // Getters for all configuration values
    public boolean isEconomyEnabled() { return economyEnabled; }
    public double getStartingBalance() { return startingBalance; }
    public double getMaxBalance() { return maxBalance; }
    public boolean allowNegativeBalances() { return allowNegativeBalances; }
    public double getInflationRate() { return inflationRate; }
    
    public boolean isBankingEnabled() { return bankingEnabled; }
    public double getAccountCreationFee() { return accountCreationFee; }
    public int getMaxAccountsPerPlayer() { return maxAccountsPerPlayer; }
    public boolean isAutoCreateChecking() { return autoCreateChecking; }
    
    public boolean isLoansEnabled() { return loansEnabled; }
    public double getStartingCreditScore() { return startingCreditScore; }
    public double getMinCreditScore() { return minCreditScore; }
    public double getMaxCreditScore() { return maxCreditScore; }
    public int getCreditUpdateInterval() { return creditUpdateInterval; }
    
    public boolean isShopsEnabled() { return shopsEnabled; }
    public double getShopCreationFee() { return shopCreationFee; }
    public int getMaxShopsPerPlayer() { return maxShopsPerPlayer; }
    public double getDailyRentalFee() { return dailyRentalFee; }
    public double getSalesTaxRate() { return salesTaxRate; }
    
    public boolean isAuctionsEnabled() { return auctionsEnabled; }
    public int getMinAuctionDuration() { return minAuctionDuration; }
    public int getMaxAuctionDuration() { return maxAuctionDuration; }
    public int getDefaultAuctionDuration() { return defaultAuctionDuration; }
    public double getMinBidIncrement() { return minBidIncrement; }
    public double getListingFeeRate() { return listingFeeRate; }
    public double getSuccessFeeRate() { return successFeeRate; }
    
    public int getHistoryRetention() { return historyRetention; }
    public double getMaxTransactionAmount() { return maxTransactionAmount; }
    public double getDailyLimit() { return dailyLimit; }
    public int getTransactionCooldown() { return transactionCooldown; }
    
    public boolean isAnalyticsEnabled() { return analyticsEnabled; }
    public int getUpdateInterval() { return updateInterval; }
    public int getRetentionPeriod() { return retentionPeriod; }
    public boolean isDailyReports() { return dailyReports; }
    public boolean isWeeklyReports() { return weeklyReports; }
    public boolean isMonthlyReports() { return monthlyReports; }
    
    public int getThreadPoolSize() { return threadPoolSize; }
    public int getQueueSize() { return queueSize; }
    public int getPlayerDataCacheSize() { return playerDataCacheSize; }
    public int getCacheExpiry() { return cacheExpiry; }
    public int getConnectionPoolSize() { return connectionPoolSize; }
    public int getQueryTimeout() { return queryTimeout; }
    
    /**
     * Get loan type configuration from YAML
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getLoanTypeConfig(String loanType) {
        if (config == null) return new HashMap<>();
        
        Map<String, Object> loans = (Map<String, Object>) config.get("loans");
        if (loans == null) return new HashMap<>();
        
        Map<String, Object> loanTypes = (Map<String, Object>) loans.get("loan_types");
        if (loanTypes == null) return new HashMap<>();
        
        Map<String, Object> typeConfig = (Map<String, Object>) loanTypes.get(loanType);
        return typeConfig != null ? typeConfig : new HashMap<>();
    }
    
    /**
     * Get account type configuration from YAML
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getAccountTypeConfig(String accountType) {
        if (config == null) return new HashMap<>();
        
        Map<String, Object> banking = (Map<String, Object>) config.get("banking");
        if (banking == null) return new HashMap<>();
        
        Map<String, Object> accountTypes = (Map<String, Object>) banking.get("account_types");
        if (accountTypes == null) return new HashMap<>();
        
        Map<String, Object> typeConfig = (Map<String, Object>) accountTypes.get(accountType);
        return typeConfig != null ? typeConfig : new HashMap<>();
    }
    
    /**
     * Get currency configuration from YAML
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getCurrencyConfig(String currencyName) {
        if (config == null) return new HashMap<>();
        
        Map<String, Object> currencies = (Map<String, Object>) config.get("currencies");
        if (currencies == null) return new HashMap<>();
        
        Map<String, Object> currencyConfig = (Map<String, Object>) currencies.get(currencyName);
        return currencyConfig != null ? currencyConfig : new HashMap<>();
    }
}
