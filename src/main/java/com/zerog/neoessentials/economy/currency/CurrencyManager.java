package com.zerog.neoessentials.economy.currency;

import com.zerog.neoessentials.economy.EconomyManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Multi-Currency Management System
 * Supports multiple currencies with:
 * - Dynamic exchange rates
 * - Currency conversion
 * - Regional currency support
 * - Cryptocurrency simulation
 * - Inflation/deflation modeling
 */
public class CurrencyManager {
    private final EconomyManager economyManager;
    private final Map<String, Currency> currencies;
    private final Map<String, Map<String, BigDecimal>> exchangeRates;
    private final ExchangeRateUpdater rateUpdater;
    
    private String primaryCurrency;
    private LocalDateTime lastRateUpdate;
    
    public CurrencyManager(EconomyManager economyManager) {
        this.economyManager = economyManager;
        this.currencies = new ConcurrentHashMap<>();
        this.exchangeRates = new ConcurrentHashMap<>();
        this.rateUpdater = new ExchangeRateUpdater();
        this.primaryCurrency = "coins";
        this.lastRateUpdate = LocalDateTime.now();
    }
    
    public void initialize() {
        // Create default currencies
        createDefaultCurrencies();
        
        // Initialize exchange rates
        initializeExchangeRates();
        
        // Start rate update task
        startRateUpdateTask();
    }
    
    private void createDefaultCurrencies() {
        // Primary currency - Coins
        addCurrency(new Currency("coins", "Coins", "¢", true, true, 
                   CurrencyType.STANDARD, new BigDecimal("1000000")));
        
        // Secondary currency - Gems
        addCurrency(new Currency("gems", "Gems", "♦", true, true, 
                   CurrencyType.PREMIUM, new BigDecimal("100000")));
        
        // Bank currency - Bank Notes
        addCurrency(new Currency("banknotes", "Bank Notes", "§", true, false, 
                   CurrencyType.BANKING, new BigDecimal("500000")));
        
        // Regional currency - Gold
        addCurrency(new Currency("gold", "Gold", "Au", true, true, 
                   CurrencyType.COMMODITY, new BigDecimal("50000")));
        
        // Crypto currency - BitCoins
        addCurrency(new Currency("bitcoins", "BitCoins", "₿", true, true, 
                   CurrencyType.CRYPTOCURRENCY, new BigDecimal("1000")));
    }
    
    private void initializeExchangeRates() {
        // Set base exchange rates (all relative to primary currency "coins")
        setExchangeRate("coins", "gems", new BigDecimal("0.1")); // 10 coins = 1 gem
        setExchangeRate("coins", "banknotes", new BigDecimal("1.0")); // 1:1 conversion
        setExchangeRate("coins", "gold", new BigDecimal("0.01")); // 100 coins = 1 gold
        setExchangeRate("coins", "bitcoins", new BigDecimal("0.001")); // 1000 coins = 1 bitcoin
        
        // Set reverse rates
        setExchangeRate("gems", "coins", new BigDecimal("10"));
        setExchangeRate("banknotes", "coins", new BigDecimal("1.0"));
        setExchangeRate("gold", "coins", new BigDecimal("100"));
        setExchangeRate("bitcoins", "coins", new BigDecimal("1000"));
    }
    
    public void addCurrency(Currency currency) {
        currencies.put(currency.getCode(), currency);
        exchangeRates.putIfAbsent(currency.getCode(), new ConcurrentHashMap<>());
    }
    
    public Currency getCurrency(String code) {
        return currencies.get(code);
    }
    
    public List<Currency> getAllCurrencies() {
        return new ArrayList<>(currencies.values());
    }
    
    public List<Currency> getTradableCurrencies() {
        return currencies.values().stream()
            .filter(Currency::isTradable)
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    public String getPrimaryCurrency() {
        return primaryCurrency;
    }
    
    public void setPrimaryCurrency(String currencyCode) {
        if (currencies.containsKey(currencyCode)) {
            this.primaryCurrency = currencyCode;
        }
    }
    
    public BigDecimal getExchangeRate(String fromCurrency, String toCurrency) {
        if (fromCurrency.equals(toCurrency)) {
            return BigDecimal.ONE;
        }
        
        Map<String, BigDecimal> rates = exchangeRates.get(fromCurrency);
        if (rates != null && rates.containsKey(toCurrency)) {
            return rates.get(toCurrency);
        }
        
        // Try indirect conversion through primary currency
        if (!fromCurrency.equals(primaryCurrency) && !toCurrency.equals(primaryCurrency)) {
            BigDecimal toPrimary = getExchangeRate(fromCurrency, primaryCurrency);
            BigDecimal fromPrimary = getExchangeRate(primaryCurrency, toCurrency);
            
            if (toPrimary != null && fromPrimary != null) {
                return toPrimary.multiply(fromPrimary);
            }
        }
        
        return null; // No conversion available
    }
    
    public void setExchangeRate(String fromCurrency, String toCurrency, BigDecimal rate) {
        exchangeRates.computeIfAbsent(fromCurrency, k -> new ConcurrentHashMap<>())
            .put(toCurrency, rate);
    }
    
    public BigDecimal convertCurrency(BigDecimal amount, String fromCurrency, String toCurrency) {
        if (fromCurrency.equals(toCurrency)) {
            return amount;
        }
        
        BigDecimal rate = getExchangeRate(fromCurrency, toCurrency);
        if (rate == null) {
            return null; // Conversion not possible
        }
        
        return amount.multiply(rate);
    }
    
    public boolean canConvert(String fromCurrency, String toCurrency) {
        return getExchangeRate(fromCurrency, toCurrency) != null;
    }
    
    public CurrencyExchangeResult exchangeCurrency(UUID playerId, String fromCurrency, 
                                                  String toCurrency, BigDecimal amount) {
        
        // Check if conversion is possible
        if (!canConvert(fromCurrency, toCurrency)) {
            return new CurrencyExchangeResult(false, "Currency conversion not available", 
                                            BigDecimal.ZERO, BigDecimal.ZERO);
        }
        
        // Check player balance
        BigDecimal playerBalance = economyManager.getBalance(playerId, fromCurrency);
        if (playerBalance.compareTo(amount) < 0) {
            return new CurrencyExchangeResult(false, "Insufficient balance", 
                                            BigDecimal.ZERO, BigDecimal.ZERO);
        }
        
        // Calculate conversion
        BigDecimal convertedAmount = convertCurrency(amount, fromCurrency, toCurrency);
        if (convertedAmount == null) {
            return new CurrencyExchangeResult(false, "Conversion calculation failed", 
                                            BigDecimal.ZERO, BigDecimal.ZERO);
        }
        
        // Calculate fees
        BigDecimal fee = calculateExchangeFee(amount, fromCurrency, toCurrency);
        BigDecimal totalDeduction = amount.add(fee);
        
        if (playerBalance.compareTo(totalDeduction) < 0) {
            return new CurrencyExchangeResult(false, "Insufficient balance including fees", 
                                            BigDecimal.ZERO, fee);
        }
        
        // Perform exchange
        boolean success = economyManager.removeBalance(playerId, fromCurrency, totalDeduction) &&
                         economyManager.addBalance(playerId, toCurrency, convertedAmount);
        
        if (success) {
            // Record exchange transaction
            economyManager.getTransactionManager().recordTransaction(
                playerId, null, fromCurrency, amount.negate(),
                com.zerog.neoessentials.economy.TransactionType.CURRENCY_EXCHANGE,
                String.format("Exchange %s to %s", fromCurrency, toCurrency)
            );
            
            return new CurrencyExchangeResult(true, "Exchange successful", 
                                            convertedAmount, fee);
        } else {
            return new CurrencyExchangeResult(false, "Exchange transaction failed", 
                                            BigDecimal.ZERO, fee);
        }
    }
    
    private BigDecimal calculateExchangeFee(BigDecimal amount, String fromCurrency, String toCurrency) {
        Currency from = getCurrency(fromCurrency);
        Currency to = getCurrency(toCurrency);
        
        // Base fee rate
        BigDecimal feeRate = new BigDecimal("0.02"); // 2% base fee
        
        // Adjust fee based on currency types
        if (from != null && to != null) {
            if (from.getType() == CurrencyType.CRYPTOCURRENCY || 
                to.getType() == CurrencyType.CRYPTOCURRENCY) {
                feeRate = feeRate.multiply(new BigDecimal("2")); // Higher fee for crypto
            }
            
            if (from.getType() == CurrencyType.PREMIUM || 
                to.getType() == CurrencyType.PREMIUM) {
                feeRate = feeRate.multiply(new BigDecimal("1.5")); // Higher fee for premium
            }
        }
        
        BigDecimal fee = amount.multiply(feeRate);
        BigDecimal maxFee = new BigDecimal("1000"); // Maximum fee cap
        
        return fee.min(maxFee);
    }
    
    private void startRateUpdateTask() {
        // Start background task to update exchange rates
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            while (true) {
                try {
                    Thread.sleep(300000); // Update every 5 minutes
                    rateUpdater.updateRates(exchangeRates, currencies);
                    lastRateUpdate = LocalDateTime.now();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    System.err.println("Error updating exchange rates: " + e.getMessage());
                }
            }
        });
    }
    
    public LocalDateTime getLastRateUpdate() {
        return lastRateUpdate;
    }
    
    public Map<String, BigDecimal> getAllExchangeRates(String baseCurrency) {
        Map<String, BigDecimal> rates = new HashMap<>();
        
        for (String currency : currencies.keySet()) {
            if (!currency.equals(baseCurrency)) {
                BigDecimal rate = getExchangeRate(baseCurrency, currency);
                if (rate != null) {
                    rates.put(currency, rate);
                }
            }
        }
        
        return rates;
    }
    
    public static class CurrencyExchangeResult {
        private final boolean successful;
        private final String message;
        private final BigDecimal convertedAmount;
        private final BigDecimal fee;
        
        public CurrencyExchangeResult(boolean successful, String message, 
                                    BigDecimal convertedAmount, BigDecimal fee) {
            this.successful = successful;
            this.message = message;
            this.convertedAmount = convertedAmount;
            this.fee = fee;
        }
        
        public boolean isSuccessful() { return successful; }
        public String getMessage() { return message; }
        public BigDecimal getConvertedAmount() { return convertedAmount; }
        public BigDecimal getFee() { return fee; }
    }
}
