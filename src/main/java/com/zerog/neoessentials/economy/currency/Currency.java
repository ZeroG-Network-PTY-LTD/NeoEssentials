package com.zerog.neoessentials.economy.currency;

import java.math.BigDecimal;

/**
 * Represents a currency in the economy system
 */
public class Currency {
    private final String code;
    private final String name;
    private final String symbol;
    private final boolean tradable;
    private final boolean transferable;
    private final CurrencyType type;
    private final BigDecimal maxSupply;
    
    private BigDecimal currentSupply;
    private boolean active;
    
    public Currency(String code, String name, String symbol, boolean tradable, 
                   boolean transferable, CurrencyType type, BigDecimal maxSupply) {
        this.code = code;
        this.name = name;
        this.symbol = symbol;
        this.tradable = tradable;
        this.transferable = transferable;
        this.type = type;
        this.maxSupply = maxSupply;
        this.currentSupply = BigDecimal.ZERO;
        this.active = true;
    }
    
    // Getters
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getSymbol() { return symbol; }
    public boolean isTradable() { return tradable; }
    public boolean isTransferable() { return transferable; }
    public CurrencyType getType() { return type; }
    public BigDecimal getMaxSupply() { return maxSupply; }
    public BigDecimal getCurrentSupply() { return currentSupply; }
    public boolean isActive() { return active; }
    
    // Setters
    public void setCurrentSupply(BigDecimal currentSupply) {
        this.currentSupply = currentSupply;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    public String formatAmount(BigDecimal amount) {
        return symbol + amount.toString();
    }
    
    @Override
    public String toString() {
        return String.format("%s (%s) - %s", name, code, symbol);
    }
}
