package com.zerog.neoessentials.economy.bank;

/**
 * Types of loans
 */
public enum LoanType {
    PERSONAL("Personal Loan", "General purpose personal loan"),
    BUSINESS("Business Loan", "For business expansion"),
    MORTGAGE("Mortgage", "For property purchase"),
    AUTO("Auto Loan", "For vehicle purchase"),
    EDUCATION("Education Loan", "For educational expenses");
    
    private final String displayName;
    private final String description;
    
    LoanType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}
