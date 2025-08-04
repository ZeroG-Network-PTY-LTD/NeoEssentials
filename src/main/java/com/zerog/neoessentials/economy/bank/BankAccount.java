package com.zerog.neoessentials.economy.bank;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a bank account
 */
public class BankAccount {
    private final String accountId;
    private final UUID ownerId;
    private final String accountName;
    private final AccountType accountType;
    private final String currency;
    private final LocalDateTime createdDate;
    
    private BigDecimal balance;
    private BigDecimal interestRate;
    private BigDecimal withdrawalLimit;
    private boolean hasInterest;
    private boolean active;
    private LocalDateTime lastActivity;
    private List<String> transactionHistory;
    
    public BankAccount(String accountId, UUID ownerId, String accountName, 
                      AccountType accountType, String currency) {
        this.accountId = accountId;
        this.ownerId = ownerId;
        this.accountName = accountName;
        this.accountType = accountType;
        this.currency = currency;
        this.createdDate = LocalDateTime.now();
        
        this.balance = BigDecimal.ZERO;
        this.interestRate = BigDecimal.ZERO;
        this.withdrawalLimit = null;
        this.hasInterest = false;
        this.active = true;
        this.lastActivity = LocalDateTime.now();
        this.transactionHistory = new ArrayList<>();
    }
    
    public void deposit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            balance = balance.add(amount);
            updateActivity();
        }
    }
    
    public boolean withdraw(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) > 0 && balance.compareTo(amount) >= 0) {
            balance = balance.subtract(amount);
            updateActivity();
            return true;
        }
        return false;
    }
    
    private void updateActivity() {
        this.lastActivity = LocalDateTime.now();
    }
    
    // Getters
    public String getAccountId() { return accountId; }
    public UUID getOwnerId() { return ownerId; }
    public String getAccountName() { return accountName; }
    public AccountType getAccountType() { return accountType; }
    public String getCurrency() { return currency; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public BigDecimal getBalance() { return balance; }
    public BigDecimal getInterestRate() { return interestRate; }
    public BigDecimal getWithdrawalLimit() { return withdrawalLimit; }
    public boolean hasInterest() { return hasInterest; }
    public boolean isActive() { return active; }
    public LocalDateTime getLastActivity() { return lastActivity; }
    public List<String> getTransactionHistory() { return new ArrayList<>(transactionHistory); }
    
    // Setters
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }
    public void setWithdrawalLimit(BigDecimal withdrawalLimit) { this.withdrawalLimit = withdrawalLimit; }
    public void setHasInterest(boolean hasInterest) { this.hasInterest = hasInterest; }
    public void setActive(boolean active) { this.active = active; }
    
    @Override
    public String toString() {
        return String.format("Account[%s] %s (%s) - Balance: %s %s", 
                           accountId, accountName, accountType.getDisplayName(), 
                           balance, currency);
    }
}
