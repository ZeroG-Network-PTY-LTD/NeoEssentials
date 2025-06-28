package com.zerog.neoessentials.economy;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a bank account in the NeoEssentials banking system.
 * Supports different account types with varying features and limitations.
 */
public class BankAccount {
    private final UUID accountId;
    private final UUID ownerId;
    private final AccountType type;
    private final String accountNumber;
    private final Map<Currency, Double> balances;
    private final long createdTime;
    private boolean isActive;
    private double creditLimit;
    private double interestRate;
    private long lastInterestCalculation;
    private final Map<UUID, Permission> sharedUsers; // For joint accounts
    
    public enum AccountType {
        CHECKING("Checking Account", 0.01, 0.0, -1, 0.005),     // Low interest, no withdrawal limit, small transaction fee
        SAVINGS("Savings Account", 0.05, 500.0, 6, 0.0),       // Higher interest, withdrawal limits, no transaction fee
        BUSINESS("Business Account", 0.02, 0.0, -1, 0.01),     // Medium interest, no withdrawal limit, higher transaction fee
        JOINT("Joint Account", 0.01, 0.0, -1, 0.005),          // Shared account, checking account features
        INVESTMENT("Investment Account", 0.08, 1000.0, 12, 0.02); // Highest interest, strict withdrawal limits, high fees
        
        private final String displayName;
        private final double baseInterestRate;
        private final double monthlyWithdrawalLimit; // -1 means unlimited
        private final int minimumMonthsBeforeWithdrawal;
        private final double transactionFeeRate;
        
        AccountType(String displayName, double baseInterestRate, double monthlyWithdrawalLimit, 
                   int minimumMonthsBeforeWithdrawal, double transactionFeeRate) {
            this.displayName = displayName;
            this.baseInterestRate = baseInterestRate;
            this.monthlyWithdrawalLimit = monthlyWithdrawalLimit;
            this.minimumMonthsBeforeWithdrawal = minimumMonthsBeforeWithdrawal;
            this.transactionFeeRate = transactionFeeRate;
        }
        
        public String getDisplayName() { return displayName; }
        public double getBaseInterestRate() { return baseInterestRate; }
        public double getMonthlyWithdrawalLimit() { return monthlyWithdrawalLimit; }
        public int getMinimumMonthsBeforeWithdrawal() { return minimumMonthsBeforeWithdrawal; }
        public double getTransactionFeeRate() { return transactionFeeRate; }
    }
    
    public enum Permission {
        VIEW,           // Can view account balance and transactions
        DEPOSIT,        // Can deposit money
        WITHDRAW,       // Can withdraw money
        FULL_ACCESS     // Can manage account settings
    }
    
    /**
     * Create a new bank account
     * 
     * @param ownerId The UUID of the account owner
     * @param type The type of account
     * @param accountNumber The unique account number
     */
    public BankAccount(UUID ownerId, AccountType type, String accountNumber) {
        this.accountId = UUID.randomUUID();
        this.ownerId = ownerId;
        this.type = type;
        this.accountNumber = accountNumber;
        this.balances = new HashMap<>();
        this.createdTime = System.currentTimeMillis();
        this.isActive = true;
        this.creditLimit = 0.0;
        this.interestRate = type.getBaseInterestRate();
        this.lastInterestCalculation = System.currentTimeMillis();
        this.sharedUsers = new HashMap<>();
    }
    
    /**
     * Get the balance for a specific currency
     * 
     * @param currency The currency to check
     * @return The balance in that currency
     */
    public double getBalance(Currency currency) {
        return balances.getOrDefault(currency, 0.0);
    }
    
    /**
     * Set the balance for a specific currency
     * 
     * @param currency The currency
     * @param amount The new balance
     */
    public void setBalance(Currency currency, double amount) {
        if (amount < 0 && Math.abs(amount) > creditLimit) {
            throw new IllegalArgumentException("Insufficient funds and credit limit exceeded");
        }
        balances.put(currency, amount);
    }
    
    /**
     * Deposit money into the account
     * 
     * @param currency The currency to deposit
     * @param amount The amount to deposit
     * @return True if successful
     */
    public boolean deposit(Currency currency, double amount) {
        if (amount <= 0 || !isActive) {
            return false;
        }
        
        double currentBalance = getBalance(currency);
        setBalance(currency, currentBalance + amount);
        return true;
    }
    
    /**
     * Deposit money with a transaction description
     * 
     * @param currency The currency to deposit
     * @param amount The amount to deposit
     * @param description Description of the transaction
     * @return True if successful
     */
    public boolean deposit(Currency currency, double amount, String description) {
        // For now, just call the basic deposit method
        // In a full implementation, this would record the transaction with description
        return deposit(currency, amount);
    }
    
    /**
     * Withdraw money from the account
     * 
     * @param currency The currency to withdraw
     * @param amount The amount to withdraw
     * @return True if successful
     */
    public boolean withdraw(Currency currency, double amount) {
        if (amount <= 0 || !isActive) {
            return false;
        }
        
        double currentBalance = getBalance(currency);
        double availableBalance = currentBalance + creditLimit;
        
        if (amount > availableBalance) {
            return false;
        }
        
        // Check withdrawal limits for savings accounts
        if (type == AccountType.SAVINGS || type == AccountType.INVESTMENT) {
            if (!canWithdraw(amount)) {
                return false;
            }
        }
        
        setBalance(currency, currentBalance - amount);
        return true;
    }
    
    /**
     * Withdraw money with a transaction description
     * 
     * @param currency The currency to withdraw
     * @param amount The amount to withdraw
     * @param description Description of the transaction
     * @return True if successful
     */
    public boolean withdraw(Currency currency, double amount, String description) {
        // For now, just call the basic withdraw method
        // In a full implementation, this would record the transaction with description
        return withdraw(currency, amount);
    }
    
    /**
     * Get the last interest calculation timestamp
     * 
     * @return The timestamp of the last interest calculation
     */
    public long getLastInterestCalculation() {
        return lastInterestCalculation;
    }
    
    /**
     * Set the last interest calculation timestamp
     * 
     * @param timestamp The timestamp to set
     */
    public void setLastInterestCalculation(long timestamp) {
        this.lastInterestCalculation = timestamp;
    }

    /**
     * Check if a withdrawal amount is allowed based on account restrictions
     * 
     * @param amount The amount to withdraw
     * @return True if withdrawal is allowed
     */
    private boolean canWithdraw(double amount) {
        // Check minimum time requirements
        long accountAgeMonths = (System.currentTimeMillis() - createdTime) / (30L * 24 * 60 * 60 * 1000);
        if (accountAgeMonths < type.getMinimumMonthsBeforeWithdrawal()) {
            return false;
        }
        
        // Check monthly withdrawal limits
        if (type.getMonthlyWithdrawalLimit() > 0) {
            // TODO: Implement monthly withdrawal tracking
            // For now, assume it's allowed
        }
        
        return true;
    }
    
    /**
     * Calculate and apply interest to the account
     * 
     * @return The amount of interest earned
     */
    public double calculateInterest() {
        long currentTime = System.currentTimeMillis();
        long timeDiff = currentTime - lastInterestCalculation;
        
        // Calculate daily interest (assuming annual rate)
        double dailyRate = interestRate / 365.0;
        long daysPassed = timeDiff / (24 * 60 * 60 * 1000);
        
        if (daysPassed < 1) {
            return 0.0;
        }
        
        double totalInterest = 0.0;
        
        // Apply interest to all positive balances
        for (Map.Entry<Currency, Double> entry : balances.entrySet()) {
            double balance = entry.getValue();
            if (balance > 0) {
                double interest = balance * dailyRate * daysPassed;
                entry.setValue(balance + interest);
                totalInterest += interest;
            }
        }
        
        lastInterestCalculation = currentTime;
        return totalInterest;
    }
    
    /**
     * Transfer money to another account
     * 
     * @param targetAccount The target account
     * @param currency The currency to transfer
     * @param amount The amount to transfer
     * @return True if successful
     */
    public boolean transferTo(BankAccount targetAccount, Currency currency, double amount) {
        if (!this.withdraw(currency, amount)) {
            return false;
        }
        
        if (!targetAccount.deposit(currency, amount)) {
            // Rollback the withdrawal
            this.deposit(currency, amount);
            return false;
        }
        
        return true;
    }
    
    /**
     * Add a shared user to the account (for joint accounts)
     * 
     * @param userId The user to add
     * @param permission The permission level
     */
    public void addSharedUser(UUID userId, Permission permission) {
        if (type == AccountType.JOINT) {
            sharedUsers.put(userId, permission);
        }
    }
    
    /**
     * Remove a shared user from the account
     * 
     * @param userId The user to remove
     */
    public void removeSharedUser(UUID userId) {
        sharedUsers.remove(userId);
    }
    
    /**
     * Check if a user has specific permission on this account
     * 
     * @param userId The user to check
     * @param requiredPermission The required permission
     * @return True if user has permission
     */
    public boolean hasPermission(UUID userId, Permission requiredPermission) {
        if (userId.equals(ownerId)) {
            return true; // Owner has all permissions
        }
        
        Permission userPermission = sharedUsers.get(userId);
        if (userPermission == null) {
            return false;
        }
        
        // Check permission hierarchy
        switch (requiredPermission) {
            case VIEW:
                return true; // All shared users can view
            case DEPOSIT:
                return userPermission == Permission.DEPOSIT || 
                       userPermission == Permission.WITHDRAW || 
                       userPermission == Permission.FULL_ACCESS;
            case WITHDRAW:
                return userPermission == Permission.WITHDRAW || 
                       userPermission == Permission.FULL_ACCESS;
            case FULL_ACCESS:
                return userPermission == Permission.FULL_ACCESS;
            default:
                return false;
        }
    }
    
    // Getters and setters
    public UUID getAccountId() { return accountId; }
    public UUID getOwnerId() { return ownerId; }
    public AccountType getType() { return type; }
    public String getAccountNumber() { return accountNumber; }
    public Map<Currency, Double> getAllBalances() { return new HashMap<>(balances); }
    public long getCreatedTime() { return createdTime; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { this.isActive = active; }
    public double getCreditLimit() { return creditLimit; }
    public void setCreditLimit(double creditLimit) { this.creditLimit = creditLimit; }
    public double getInterestRate() { return interestRate; }
    public void setInterestRate(double interestRate) { this.interestRate = interestRate; }
    public Map<UUID, Permission> getSharedUsers() { return new HashMap<>(sharedUsers); }
}
