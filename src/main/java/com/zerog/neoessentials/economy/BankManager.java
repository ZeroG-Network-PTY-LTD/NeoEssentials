package com.zerog.neoessentials.economy;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all bank accounts and banking operations in the NeoEssentials economy system.
 * Handles account creation, transactions, interest calculation, and loan management.
 */
public class BankManager {
    private static BankManager instance;
    final Map<UUID, List<BankAccount>> playerAccounts; // Player UUID -> List of accounts (package-private for EconomyManager)
    private final Map<String, BankAccount> accountsByNumber; // Account number -> Account
    private final Map<UUID, Loan> activeLoans; // Loan ID -> Loan
    private final InterestCalculator interestCalculator;
    private final LoanManager loanManager;
    
    private BankManager() {
        this.playerAccounts = new ConcurrentHashMap<>();
        this.accountsByNumber = new ConcurrentHashMap<>();
        this.activeLoans = new ConcurrentHashMap<>();
        this.interestCalculator = new InterestCalculator();
        this.loanManager = new LoanManager();
    }
    
    public static BankManager getInstance() {
        if (instance == null) {
            instance = new BankManager();
        }
        return instance;
    }
    
    /**
     * Create a new bank account for a player
     * 
     * @param playerId The player's UUID
     * @param accountType The type of account to create
     * @return The created bank account, or null if creation failed
     */
    public BankAccount createAccount(UUID playerId, BankAccount.AccountType accountType) {
        String accountNumber = generateAccountNumber();
        BankAccount account = new BankAccount(playerId, accountType, accountNumber);
        
        // Add to player's accounts
        playerAccounts.computeIfAbsent(playerId, k -> new ArrayList<>()).add(account);
        
        // Add to account lookup
        accountsByNumber.put(accountNumber, account);
        
        return account;
    }
    
    /**
     * Get all accounts for a player
     * 
     * @param playerId The player's UUID
     * @return List of the player's bank accounts
     */
    public List<BankAccount> getPlayerAccounts(UUID playerId) {
        return playerAccounts.getOrDefault(playerId, new ArrayList<>());
    }
    
    /**
     * Get a bank account by account number
     * 
     * @param accountNumber The account number
     * @return The bank account, or null if not found
     */
    public BankAccount getAccountByNumber(String accountNumber) {
        return accountsByNumber.get(accountNumber);
    }
    
    /**
     * Get the primary checking account for a player (creates one if none exists)
     * 
     * @param playerId The player's UUID
     * @return The player's primary checking account
     */
    public BankAccount getPrimaryAccount(UUID playerId) {
        List<BankAccount> accounts = getPlayerAccounts(playerId);
        
        // Look for existing checking account
        for (BankAccount account : accounts) {
            if (account.getType() == BankAccount.AccountType.CHECKING && account.isActive()) {
                return account;
            }
        }
        
        // Create new checking account if none exists
        return createAccount(playerId, BankAccount.AccountType.CHECKING);
    }
    
    /**
     * Transfer money between accounts
     * 
     * @param fromAccount Source account
     * @param toAccount Destination account
     * @param amount Amount to transfer
     * @param currency Currency to transfer
     * @param reason Reason for transfer
     * @return true if transfer was successful
     */
    public boolean transferBetweenAccounts(BankAccount fromAccount, BankAccount toAccount, 
                                         double amount, Currency currency, String reason) {
        if (!fromAccount.isActive() || !toAccount.isActive()) {
            return false;
        }
        
        // Check if source account has sufficient funds
        if (fromAccount.getBalance(currency) < amount) {
            return false;
        }
        
        // Calculate transaction fee
        double fee = amount * fromAccount.getType().getTransactionFeeRate();
        double totalDeduction = amount + fee;
        
        if (fromAccount.getBalance(currency) < totalDeduction) {
            return false;
        }
        
        // Perform transfer
        try {
            fromAccount.withdraw(currency, totalDeduction, "Transfer: " + reason + " (including fee: " + fee + ")");
            toAccount.deposit(currency, amount, "Transfer from " + fromAccount.getAccountNumber() + ": " + reason);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Calculate and apply interest to all eligible accounts
     */
    public void calculateInterestForAllAccounts() {
        for (List<BankAccount> accounts : playerAccounts.values()) {
            for (BankAccount account : accounts) {
                if (account.isActive()) {
                    interestCalculator.calculateAndApplyInterest(account);
                }
            }
        }
    }
    
    /**
     * Close a bank account
     * 
     * @param accountNumber The account number to close
     * @param reason Reason for closure
     * @return true if account was successfully closed
     */
    public boolean closeAccount(String accountNumber, String reason) {
        BankAccount account = getAccountByNumber(accountNumber);
        if (account == null || !account.isActive()) {
            return false;
        }
        
        // Check if account has any balances
        for (Currency currency : CurrencyManager.getInstance().getAllCurrencies()) {
            if (account.getBalance(currency) > 0) {
                return false; // Cannot close account with remaining balance
            }
        }
        
        // Close the account
        account.setActive(false);
        return true;
    }
    
    /**
     * Get total balance across all accounts for a player in a specific currency
     * 
     * @param playerId The player's UUID
     * @param currency The currency to check
     * @return Total balance across all accounts
     */
    public double getTotalPlayerBalance(UUID playerId, Currency currency) {
        List<BankAccount> accounts = getPlayerAccounts(playerId);
        return accounts.stream()
                .filter(BankAccount::isActive)
                .mapToDouble(account -> account.getBalance(currency))
                .sum();
    }
    
    /**
     * Generate a unique account number
     * 
     * @return A unique 10-digit account number
     */
    private String generateAccountNumber() {
        String accountNumber;
        Random random = new Random();
        
        do {
            // Generate 10-digit account number
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 10; i++) {
                sb.append(random.nextInt(10));
            }
            accountNumber = sb.toString();
        } while (accountsByNumber.containsKey(accountNumber));
        
        return accountNumber;
    }
    
    /**
     * Inner class for calculating interest
     */
    private static class InterestCalculator {
        private static final long INTEREST_INTERVAL = 24 * 60 * 60 * 1000; // 24 hours
        
        public void calculateAndApplyInterest(BankAccount account) {
            long currentTime = System.currentTimeMillis();
            long lastCalculation = account.getLastInterestCalculation();
            
            // Check if 24 hours have passed
            if (currentTime - lastCalculation < INTEREST_INTERVAL) {
                return;
            }
            
            Currency defaultCurrency = CurrencyManager.getInstance().getDefaultCurrency();
            double balance = account.getBalance(defaultCurrency);
            
            if (balance > 0) {
                double interestRate = account.getInterestRate();
                double dailyRate = interestRate / 365.0; // Convert annual rate to daily
                double interest = balance * dailyRate;
                
                // Apply interest
                account.deposit(defaultCurrency, interest, "Daily interest payment");
                account.setLastInterestCalculation(currentTime);
            }
        }
    }
    
    /**
     * Inner class for loan management
     */
    private static class LoanManager {
        
        /**
         * Apply for a loan
         * 
         * @param playerId Player applying for loan
         * @param amount Loan amount
         * @param currency Loan currency
         * @param loanType Type of loan
         * @param termMonths Loan term in months
         * @return The created loan, or null if application denied
         */
        public Loan applyForLoan(UUID playerId, double amount, Currency currency, 
                                Loan.LoanType loanType, int termMonths) {
            // Check credit score and loan eligibility
            if (!isEligibleForLoan(playerId, amount, loanType)) {
                return null;
            }
            
            double interestRate = getLoanInterestRate(loanType, termMonths);
            Loan loan = new Loan(playerId, amount, currency, loanType, termMonths, interestRate);
            
            return loan;
        }
        
        private boolean isEligibleForLoan(UUID playerId, double amount, Loan.LoanType loanType) {
            // Basic eligibility checks
            BankAccount primaryAccount = BankManager.getInstance().getPrimaryAccount(playerId);
            if (primaryAccount == null) {
                return false;
            }
            
            // Check if player has existing loans
            // Check credit score
            // Check income/balance ratios
            // etc.
            
            return true; // Simplified for now
        }
        
        private double getLoanInterestRate(Loan.LoanType loanType, int termMonths) {
            return switch (loanType) {
                case PERSONAL -> 0.08 + (termMonths > 12 ? 0.02 : 0.0);
                case MORTGAGE -> 0.05 + (termMonths > 120 ? 0.01 : 0.0);
                case BUSINESS -> 0.06 + (termMonths > 24 ? 0.015 : 0.0);
            };
        }
    }
}
