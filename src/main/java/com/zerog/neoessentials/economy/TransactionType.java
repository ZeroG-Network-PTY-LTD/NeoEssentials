package com.zerog.neoessentials.economy;

/**
 * Transaction types for the economy system
 */
public enum TransactionType {
    // Basic transactions
    ADMIN_GIVE("Admin Give", "Money given by administrator"),
    ADMIN_TAKE("Admin Take", "Money taken by administrator"),
    ADMIN_SET("Admin Set", "Balance set by administrator"),
    
    // Player transactions
    TRANSFER_SEND("Transfer Send", "Money sent to another player"),
    TRANSFER_RECEIVE("Transfer Receive", "Money received from another player"),
    
    // Banking transactions
    BANK_DEPOSIT("Bank Deposit", "Money deposited to bank account"),
    BANK_WITHDRAW("Bank Withdraw", "Money withdrawn from bank account"),
    BANK_INTEREST("Bank Interest", "Interest earned on bank account"),
    BANK_LOAN("Bank Loan", "Loan received from bank"),
    BANK_LOAN_PAYMENT("Loan Payment", "Payment made on bank loan"),
    
    // Commerce transactions
    SHOP_PURCHASE("Shop Purchase", "Item purchased from shop"),
    SHOP_SALE("Shop Sale", "Item sold to shop"),
    AUCTION_BID("Auction Bid", "Bid placed on auction"),
    AUCTION_WIN("Auction Win", "Item won at auction"),
    AUCTION_REFUND("Auction Refund", "Refund for failed auction"),
    
    // Market transactions
    MARKET_BUY("Market Buy", "Item purchased from market"),
    MARKET_SELL("Market Sell", "Item sold to market"),
    
    // System transactions
    FEE("Fee", "Transaction or service fee"),
    TAX("Tax", "Government tax payment"),
    SALARY("Salary", "Job salary payment"),
    REWARD("Reward", "Quest or achievement reward"),
    FINE("Fine", "Penalty or fine payment"),
    
    // Special transactions
    CURRENCY_EXCHANGE("Currency Exchange", "Currency converted to another type"),
    CROSS_SERVER("Cross Server", "Transaction from another server"),
    INSURANCE_CLAIM("Insurance Claim", "Insurance claim payout"),
    INVESTMENT("Investment", "Investment purchase or sale");
    
    private final String displayName;
    private final String description;
    
    TransactionType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    
    public boolean isPositive() {
        return this == ADMIN_GIVE || this == TRANSFER_RECEIVE || this == BANK_INTEREST ||
               this == BANK_LOAN || this == SHOP_SALE || this == AUCTION_REFUND ||
               this == MARKET_SELL || this == SALARY || this == REWARD ||
               this == INSURANCE_CLAIM;
    }
    
    public boolean isNegative() {
        return this == ADMIN_TAKE || this == TRANSFER_SEND || this == BANK_DEPOSIT ||
               this == BANK_WITHDRAW || this == BANK_LOAN_PAYMENT || this == SHOP_PURCHASE ||
               this == AUCTION_BID || this == AUCTION_WIN || this == MARKET_BUY ||
               this == FEE || this == TAX || this == FINE;
    }
}
