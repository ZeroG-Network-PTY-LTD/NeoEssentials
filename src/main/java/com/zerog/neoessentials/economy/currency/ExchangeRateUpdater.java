package com.zerog.neoessentials.economy.currency;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Random;

/**
 * Updates exchange rates dynamically
 */
public class ExchangeRateUpdater {
    private final Random random;
    private final BigDecimal maxFluctuation;
    
    public ExchangeRateUpdater() {
        this.random = new Random();
        this.maxFluctuation = new BigDecimal("0.05"); // 5% max fluctuation
    }
    
    public void updateRates(Map<String, Map<String, BigDecimal>> exchangeRates, 
                           Map<String, Currency> currencies) {
        
        for (Map.Entry<String, Map<String, BigDecimal>> entry : exchangeRates.entrySet()) {
            String fromCurrency = entry.getKey();
            Map<String, BigDecimal> rates = entry.getValue();
            
            Currency fromCurr = currencies.get(fromCurrency);
            if (fromCurr == null || !fromCurr.isActive()) {
                continue;
            }
            
            for (Map.Entry<String, BigDecimal> rateEntry : rates.entrySet()) {
                String toCurrency = rateEntry.getKey();
                BigDecimal currentRate = rateEntry.getValue();
                
                Currency toCurr = currencies.get(toCurrency);
                if (toCurr == null || !toCurr.isActive()) {
                    continue;
                }
                
                // Calculate fluctuation based on currency types
                BigDecimal fluctuation = calculateFluctuation(fromCurr, toCurr);
                
                // Apply fluctuation
                BigDecimal change = currentRate.multiply(fluctuation);
                BigDecimal newRate = currentRate.add(change);
                
                // Ensure rate doesn't go negative or too extreme
                if (newRate.compareTo(BigDecimal.ZERO) > 0 && 
                    newRate.compareTo(currentRate.multiply(new BigDecimal("2"))) < 0) {
                    rates.put(toCurrency, newRate.setScale(6, RoundingMode.HALF_UP));
                }
            }
        }
    }
    
    private BigDecimal calculateFluctuation(Currency fromCurrency, Currency toCurrency) {
        BigDecimal volatility = getVolatility(fromCurrency).add(getVolatility(toCurrency))
            .divide(new BigDecimal("2"), 6, RoundingMode.HALF_UP);
        
        // Generate random fluctuation between -volatility and +volatility
        double randomValue = (random.nextDouble() - 0.5) * 2; // -1 to 1
        BigDecimal fluctuation = volatility.multiply(BigDecimal.valueOf(randomValue));
        
        // Cap fluctuation at maximum
        if (fluctuation.abs().compareTo(maxFluctuation) > 0) {
            fluctuation = fluctuation.signum() > 0 ? maxFluctuation : maxFluctuation.negate();
        }
        
        return fluctuation;
    }
    
    private BigDecimal getVolatility(Currency currency) {
        switch (currency.getType()) {
            case CRYPTOCURRENCY:
                return new BigDecimal("0.10"); // 10% volatility
            case PREMIUM:
                return new BigDecimal("0.02"); // 2% volatility
            case COMMODITY:
                return new BigDecimal("0.05"); // 5% volatility
            case STANDARD:
            case BANKING:
                return new BigDecimal("0.01"); // 1% volatility
            default:
                return new BigDecimal("0.03"); // 3% default volatility
        }
    }
}
