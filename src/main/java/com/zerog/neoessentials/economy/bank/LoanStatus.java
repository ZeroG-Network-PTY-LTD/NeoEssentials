package com.zerog.neoessentials.economy.bank;

/**
 * Loan status enumeration
 */
public enum LoanStatus {
    PENDING("Pending", "Loan application under review"),
    APPROVED("Approved", "Loan approved but not disbursed"),
    ACTIVE("Active", "Loan is active and payments are due"),
    PAID_OFF("Paid Off", "Loan has been fully paid"),
    DEFAULTED("Defaulted", "Loan is in default"),
    CANCELLED("Cancelled", "Loan application was cancelled");
    
    private final String displayName;
    private final String description;
    
    LoanStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}
