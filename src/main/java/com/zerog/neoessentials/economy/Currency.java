package com.zerog.neoessentials.economy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Represents a currency type in the economy system.
 * Supports multiple currencies with proper decimal handling.
 */
public class Currency {
    private final String id;
    private final String name;
    private final String symbol;
    private final String pluralName;
    private final int decimalPlaces;
    private final BigDecimal exchangeRate; // Rate to base currency
    
    public Currency(String id, String name, String symbol, String pluralName, int decimalPlaces, BigDecimal exchangeRate) {
        this.id = Objects.requireNonNull(id, "Currency ID cannot be null");
        this.name = Objects.requireNonNull(name, "Currency name cannot be null");
        this.symbol = Objects.requireNonNull(symbol, "Currency symbol cannot be null");
        this.pluralName = Objects.requireNonNull(pluralName, "Currency plural name cannot be null");
        this.decimalPlaces = Math.max(0, decimalPlaces);
        this.exchangeRate = Objects.requireNonNull(exchangeRate, "Exchange rate cannot be null");
        
        if (exchangeRate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Exchange rate must be positive");
        }
    }
    
    /**
     * Creates a basic currency with default settings
     */
    public static Currency createBasic(String id, String name, String symbol, String pluralName) {
        return new Currency(id, name, symbol, pluralName, 2, BigDecimal.ONE);
    }
    
    /**
     * Formats an amount in this currency
     */
    public String format(BigDecimal amount) {
        BigDecimal rounded = amount.setScale(decimalPlaces, RoundingMode.HALF_UP);
        String amountStr = rounded.toString();
        
        if (rounded.compareTo(BigDecimal.ONE) == 0) {
            return symbol + amountStr + " " + name;
        } else {
            return symbol + amountStr + " " + pluralName;
        }
    }
    
    /**
     * Converts an amount from this currency to another currency
     */
    public BigDecimal convertTo(BigDecimal amount, Currency targetCurrency) {
        if (this.equals(targetCurrency)) {
            return amount;
        }
        
        // Convert to base currency first, then to target
        BigDecimal baseAmount = amount.divide(this.exchangeRate, 10, RoundingMode.HALF_UP);
        return baseAmount.multiply(targetCurrency.exchangeRate)
                        .setScale(targetCurrency.decimalPlaces, RoundingMode.HALF_UP);
    }
    
    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getSymbol() { return symbol; }
    public String getPluralName() { return pluralName; }
    public int getDecimalPlaces() { return decimalPlaces; }
    public BigDecimal getExchangeRate() { return exchangeRate; }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Currency currency = (Currency) obj;
        return Objects.equals(id, currency.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return name + " (" + symbol + ")";
    }
}
