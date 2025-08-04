package com.zerog.neoessentials.economy.currency;

/**
 * Types of currencies in the economy system
 */
public enum CurrencyType {
    STANDARD("Standard", "Regular server currency"),
    PREMIUM("Premium", "Premium currency from donations or achievements"),
    BANKING("Banking", "Bank-issued currency"),
    COMMODITY("Commodity", "Commodity-backed currency"),
    CRYPTOCURRENCY("Cryptocurrency", "Digital cryptocurrency"),
    REGIONAL("Regional", "Region-specific currency"),
    EVENT("Event", "Special event currency"),
    GUILD("Guild", "Guild-specific currency");
    
    private final String displayName;
    private final String description;
    
    CurrencyType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}
