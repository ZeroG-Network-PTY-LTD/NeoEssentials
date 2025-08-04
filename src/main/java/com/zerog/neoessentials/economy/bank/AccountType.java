package com.zerog.neoessentials.economy.bank;

/**
 * Types of bank accounts
 */
public enum AccountType {
    SAVINGS("Savings Account", "Earns interest, limited withdrawals"),
    CHECKING("Checking Account", "Regular transactions, lower interest"),
    BUSINESS("Business Account", "For business transactions"),
    INVESTMENT("Investment Account", "High interest, limited access");
    
    private final String displayName;
    private final String description;
    
    AccountType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}
