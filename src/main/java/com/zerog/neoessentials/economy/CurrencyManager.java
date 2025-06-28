package com.zerog.neoessentials.economy;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central manager for all currencies in the NeoEssentials economy system.
 * Handles currency registration, exchange rates, and conversions.
 */
public class CurrencyManager {
    private static CurrencyManager instance;
    private final Map<String, Currency> currencies;
    private final Map<String, Double> exchangeRates;
    private Currency defaultCurrency;
    private final Map<String, Double> historicalRates; // For tracking exchange rate history
    
    private CurrencyManager() {
        this.currencies = new ConcurrentHashMap<>();
        this.exchangeRates = new ConcurrentHashMap<>();
        this.historicalRates = new ConcurrentHashMap<>();
        initializeDefaultCurrencies();
    }
    
    public static CurrencyManager getInstance() {
        if (instance == null) {
            instance = new CurrencyManager();
        }
        return instance;
    }
    
    /**
     * Initialize default currencies for the server
     */
    private void initializeDefaultCurrencies() {
        // Default server currency
        Currency coins = new Currency(
            "coins", 
            "Coin", 
            "$", 
            "Coins", 
            true, 
            false, 
            1.0, 
            Currency.CurrencyType.STANDARD
        );
        registerCurrency(coins);
        setDefaultCurrency(coins);
        
        // Resource-backed currencies
        Currency goldIngots = new Currency(
            "gold_ingots",
            "Gold Ingot",
            "⚆",
            "Gold Ingots",
            false,
            true,
            10.0,
            Currency.CurrencyType.RESOURCE
        );
        registerCurrency(goldIngots);
        
        Currency diamonds = new Currency(
            "diamonds",
            "Diamond",
            "♦",
            "Diamonds",
            false,
            true,
            50.0,
            Currency.CurrencyType.RESOURCE
        );
        registerCurrency(diamonds);
        
        // Event tokens
        Currency eventTokens = new Currency(
            "event_tokens",
            "Event Token",
            "✦",
            "Event Tokens",
            false,
            false,
            5.0,
            Currency.CurrencyType.TOKEN
        );
        registerCurrency(eventTokens);
    }
    
    /**
     * Register a new currency
     * 
     * @param currency The currency to register
     * @return true if successfully registered, false if currency ID already exists
     */
    public boolean registerCurrency(Currency currency) {
        if (currencies.containsKey(currency.getId())) {
            return false;
        }
        
        currencies.put(currency.getId(), currency);
        exchangeRates.put(currency.getId(), currency.getExchangeRate());
        return true;
    }
    
    /**
     * Get a currency by ID
     * 
     * @param currencyId The currency ID
     * @return The currency, or null if not found
     */
    public Currency getCurrency(String currencyId) {
        return currencies.get(currencyId);
    }
    
    /**
     * Get all registered currencies
     * 
     * @return Collection of all currencies
     */
    public Collection<Currency> getAllCurrencies() {
        return currencies.values();
    }
    
    /**
     * Get all currencies of a specific type
     * 
     * @param type The currency type to filter by
     * @return List of currencies of the specified type
     */
    public List<Currency> getCurrenciesByType(Currency.CurrencyType type) {
        return currencies.values().stream()
                .filter(currency -> currency.getType() == type)
                .toList();
    }
    
    /**
     * Set the default currency for the server
     * 
     * @param currency The currency to set as default
     */
    public void setDefaultCurrency(Currency currency) {
        if (currency != null && currencies.containsKey(currency.getId())) {
            this.defaultCurrency = currency;
        }
    }
    
    /**
     * Get the default currency
     * 
     * @return The default currency
     */
    public Currency getDefaultCurrency() {
        return defaultCurrency;
    }
    
    /**
     * Update exchange rate for a currency
     * 
     * @param currencyId The currency ID
     * @param newRate The new exchange rate
     */
    public void updateExchangeRate(String currencyId, double newRate) {
        if (currencies.containsKey(currencyId)) {
            // Store historical rate
            String historyKey = currencyId + "_" + System.currentTimeMillis();
            historicalRates.put(historyKey, exchangeRates.get(currencyId));
            
            // Update current rate
            exchangeRates.put(currencyId, newRate);
        }
    }
    
    /**
     * Get current exchange rate for a currency
     * 
     * @param currencyId The currency ID
     * @return The current exchange rate, or 1.0 if currency not found
     */
    public double getExchangeRate(String currencyId) {
        return exchangeRates.getOrDefault(currencyId, 1.0);
    }
    
    /**
     * Convert an amount from one currency to another
     * 
     * @param amount The amount to convert
     * @param fromCurrency Source currency
     * @param toCurrency Target currency
     * @return The converted amount
     */
    public double convertCurrency(double amount, Currency fromCurrency, Currency toCurrency) {
        if (fromCurrency.equals(toCurrency)) {
            return amount;
        }
        
        // Convert to default currency first
        double inDefaultCurrency = amount * getExchangeRate(fromCurrency.getId());
        
        // Convert from default currency to target currency
        return inDefaultCurrency / getExchangeRate(toCurrency.getId());
    }
    
    /**
     * Calculate conversion fee for currency exchange
     * 
     * @param amount The amount being converted
     * @param fromCurrency Source currency
     * @param toCurrency Target currency
     * @return The conversion fee in the target currency
     */
    public double calculateConversionFee(double amount, Currency fromCurrency, Currency toCurrency) {
        if (fromCurrency.equals(toCurrency)) {
            return 0.0;
        }
        
        // Base conversion fee of 2%
        double feeRate = 0.02;
        
        // Different types have different fees
        if (fromCurrency.getType() != toCurrency.getType()) {
            feeRate += 0.01; // Additional 1% for cross-type conversions
        }
        
        double convertedAmount = convertCurrency(amount, fromCurrency, toCurrency);
        return convertedAmount * feeRate;
    }
    
    /**
     * Get exchange rate history for a currency
     * 
     * @param currencyId The currency ID
     * @param days Number of days of history to retrieve
     * @return Map of timestamp to exchange rate
     */
    public Map<Long, Double> getExchangeRateHistory(String currencyId, int days) {
        Map<Long, Double> history = new HashMap<>();
        long cutoffTime = System.currentTimeMillis() - (days * 24L * 60L * 60L * 1000L);
        
        for (Map.Entry<String, Double> entry : historicalRates.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(currencyId + "_")) {
                try {
                    long timestamp = Long.parseLong(key.substring(currencyId.length() + 1));
                    if (timestamp >= cutoffTime) {
                        history.put(timestamp, entry.getValue());
                    }
                } catch (NumberFormatException e) {
                    // Skip invalid entries
                }
            }
        }
        
        return history;
    }
    
    /**
     * Check if a currency exists
     * 
     * @param currencyId The currency ID to check
     * @return true if the currency exists
     */
    public boolean currencyExists(String currencyId) {
        return currencies.containsKey(currencyId);
    }
    
    /**
     * Remove a currency (admin only)
     * 
     * @param currencyId The currency ID to remove
     * @return true if successfully removed
     */
    public boolean removeCurrency(String currencyId) {
        if (defaultCurrency != null && defaultCurrency.getId().equals(currencyId)) {
            return false; // Cannot remove default currency
        }
        
        currencies.remove(currencyId);
        exchangeRates.remove(currencyId);
        return true;
    }
    
    /**
     * Reload configuration for currency manager
     */
    public void reloadConfiguration() {
        // TODO: Implement configuration reload when needed
        // This is a placeholder to fix compilation errors
    }
}
