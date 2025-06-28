package com.zerog.neoessentials.economy;

import java.util.UUID;

/**
 * Represents a currency in the NeoEssentials economy system.
 * Supports multiple currencies with different properties and behaviors.
 */
public class Currency {
    private final String id;
    private final String displayName;
    private final String symbol;
    private final String pluralName;
    private final boolean isDefault;
    private final boolean isPhysical; // Can be stored as items
    private final double exchangeRate; // Rate compared to default currency
    private final CurrencyType type;
    
    public enum CurrencyType {
        STANDARD,    // Regular server currency
        REGIONAL,    // Area-specific currency
        RESOURCE,    // Backed by physical items (gold, diamonds, etc.)
        TOKEN,       // Event/achievement tokens
        CRYPTO       // Digital-only currency with fluctuating value
    }
    
    /**
     * Create a new currency
     * 
     * @param id Unique identifier for the currency
     * @param displayName Display name of the currency
     * @param symbol Symbol used when displaying amounts
     * @param pluralName Plural form of the currency name
     * @param isDefault Whether this is the server's default currency
     * @param isPhysical Whether this currency can be stored as physical items
     * @param exchangeRate Exchange rate compared to default currency
     * @param type The type of currency
     */
    public Currency(String id, String displayName, String symbol, String pluralName,
                   boolean isDefault, boolean isPhysical, double exchangeRate, CurrencyType type) {
        this.id = id;
        this.displayName = displayName;
        this.symbol = symbol;
        this.pluralName = pluralName;
        this.isDefault = isDefault;
        this.isPhysical = isPhysical;
        this.exchangeRate = exchangeRate;
        this.type = type;
    }
    
    // Getters
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getSymbol() { return symbol; }
    public String getPluralName() { return pluralName; }
    public boolean isDefault() { return isDefault; }
    public boolean isPhysical() { return isPhysical; }
    public double getExchangeRate() { return exchangeRate; }
    public CurrencyType getType() { return type; }
    
    /**
     * Format an amount in this currency
     * 
     * @param amount The amount to format
     * @return Formatted currency string
     */
    public String format(double amount) {
        String formattedAmount = String.format("%.2f", amount);
        String currencyName = Math.abs(amount - 1.0) < 0.009 ? displayName : pluralName;
        return symbol + formattedAmount + " " + currencyName;
    }
    
    /**
     * Convert an amount from this currency to another currency
     * 
     * @param amount Amount in this currency
     * @param targetCurrency Target currency to convert to
     * @return Amount in target currency
     */
    public double convertTo(double amount, Currency targetCurrency) {
        if (this.equals(targetCurrency)) {
            return amount;
        }
        
        // Convert to default currency first, then to target
        double inDefaultCurrency = amount * this.exchangeRate;
        return inDefaultCurrency / targetCurrency.exchangeRate;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Currency currency = (Currency) obj;
        return id.equals(currency.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
    
    @Override
    public String toString() {
        return "Currency{" +
                "id='" + id + '\'' +
                ", displayName='" + displayName + '\'' +
                ", symbol='" + symbol + '\'' +
                ", type=" + type +
                '}';
    }
}
