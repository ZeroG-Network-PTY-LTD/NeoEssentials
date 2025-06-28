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
    private com.zerog.neoessentials.economy.persistence.EconomyPersistenceManager persistenceManager;
    
    private BankManager() {
        this.playerAccounts = new ConcurrentHashMap<>();
        this.accountsByNumber = new ConcurrentHashMap<>();
        this.activeLoans = new ConcurrentHashMap<>();
        this.interestCalculator = new InterestCalculator();
        this.loanManager = new LoanManager();
    }
    
    public void setPersistenceManager(com.zerog.neoessentials.economy.persistence.EconomyPersistenceManager persistenceManager) {
        this.persistenceManager = persistenceManager;
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
     * Get the loan manager for handling loan operations
     */
    public LoanManager getLoanManager() {
        return loanManager;
    }
    
    /**
     * Get all loans for a specific player
     */
    public List<Loan> getPlayerLoans(UUID playerId) {
        if (persistenceManager != null) {
            return persistenceManager.loadPlayerLoans(playerId).join();
        }
        return new ArrayList<>();
    }
    
    /**
     * Get a specific loan by ID
     */
    public Loan getLoan(UUID loanId) {
        if (persistenceManager != null) {
            return persistenceManager.loadLoan(loanId).join();
        }
        return activeLoans.get(loanId);
    }
    
    /**
     * Get all active loans (admin function)
     */
    public List<Loan> getAllActiveLoans() {
        if (persistenceManager != null) {
            return persistenceManager.loadAllActiveLoans().join();
        }
        return new ArrayList<>(activeLoans.values());
    }
    
    /**
     * Inner class for loan management
     */
    public class LoanManager {
        
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
            
            // Save the loan to persistence if available
            if (persistenceManager != null) {
                persistenceManager.saveLoan(loan);
            }
            
            // Add to local cache
            activeLoans.put(loan.getLoanId(), loan);
            
            return loan;
        }
        
        /**
         * Make a payment on a loan
         * @param playerId Player making the payment
         * @param loanId Loan to pay (null for automatic selection)
         * @param amount Payment amount
         * @return true if payment successful
         */
        public boolean makePayment(UUID playerId, UUID loanId, double amount) {
            Loan loan;
            
            if (loanId == null) {
                // Find the player's most recent active loan
                List<Loan> playerLoans = getPlayerLoans(playerId);
                loan = playerLoans.stream()
                    .filter(l -> l.getStatus() == Loan.LoanStatus.CURRENT || l.getStatus() == Loan.LoanStatus.LATE)
                    .findFirst()
                    .orElse(null);
            } else {
                loan = getLoan(loanId);
            }
            
            if (loan == null || !loan.getBorrowerId().equals(playerId)) {
                return false;
            }
            
            // Check if player has sufficient funds in their primary account
            BankAccount primaryAccount = getPrimaryAccount(playerId);
            Currency defaultCurrency = CurrencyManager.getInstance().getDefaultCurrency();
            
            if (primaryAccount == null || primaryAccount.getBalance(defaultCurrency) < amount) {
                return false;
            }
            
            // Process the payment
            boolean success = loan.makePayment(amount, primaryAccount.getAccountNumber());
            if (success) {
                // Deduct from player's account
                primaryAccount.withdraw(defaultCurrency, amount, "Loan payment");
                
                // Save the updated loan and account
                if (persistenceManager != null) {
                    persistenceManager.saveLoan(loan);
                    persistenceManager.saveBankAccount(primaryAccount);
                }
                
                // Update local cache
                activeLoans.put(loan.getLoanId(), loan);
            }
            
            return success;
        }
        
        /**
         * Calculate credit score for a player
         * @param playerId Player UUID
         * @return Credit score (300-850)
         */
        public int calculateCreditScore(UUID playerId) {
            int baseScore = 650; // Starting credit score
            
            // Get player's loan history
            List<Loan> playerLoans = getPlayerLoans(playerId);
            
            // Calculate based on payment history
            int onTimePayments = 0;
            int totalPayments = 0;
            
            for (Loan loan : playerLoans) {
                // Add logic based on payment history
                totalPayments += loan.getTermMonths() - loan.getPaymentsRemaining();
                onTimePayments += loan.getTermMonths() - loan.getPaymentsRemaining(); // Simplified
            }
            
            // Calculate payment history score (35% of total)
            if (totalPayments > 0) {
                double paymentRatio = (double) onTimePayments / totalPayments;
                baseScore += (int) ((paymentRatio - 0.5) * 200); // -100 to +100
            }
            
            // Account balance factor (30% of total)
            BankAccount primaryAccount = getPrimaryAccount(playerId);
            Currency defaultCurrency = CurrencyManager.getInstance().getDefaultCurrency();
            
            if (primaryAccount != null) {
                double balance = primaryAccount.getBalance(defaultCurrency);
                if (balance > 10000) baseScore += 50;
                else if (balance > 5000) baseScore += 25;
                else if (balance < 1000) baseScore -= 25;
            }
            
            // Number of active loans (10% of total)
            long activeLoansCount = playerLoans.stream()
                .filter(l -> l.getStatus() == Loan.LoanStatus.CURRENT || l.getStatus() == Loan.LoanStatus.LATE)
                .count();
            
            if (activeLoansCount > 3) baseScore -= 30;
            else if (activeLoansCount == 0) baseScore += 10;
            
            // Ensure score is within valid range
            return Math.max(300, Math.min(850, baseScore));
        }
        
        /**
         * Get loan eligibility for a player
         * @param playerId Player UUID
         * @param loanType Type of loan
         * @return Maximum loan amount the player is eligible for
         */
        public double getMaxLoanEligibility(UUID playerId, Loan.LoanType loanType) {
            int creditScore = calculateCreditScore(playerId);
            
            // Base eligibility based on credit score
            double baseAmount = switch (loanType) {
                case PERSONAL -> creditScore < 600 ? 1000 : creditScore < 700 ? 5000 : 15000;
                case BUSINESS -> creditScore < 600 ? 5000 : creditScore < 700 ? 25000 : 75000;
                case MORTGAGE -> creditScore < 600 ? 10000 : creditScore < 700 ? 50000 : 200000;
            };
            
            // Adjust based on existing loans
            List<Loan> existingLoans = getPlayerLoans(playerId);
            double existingDebt = existingLoans.stream()
                .filter(l -> l.getStatus() == Loan.LoanStatus.CURRENT || l.getStatus() == Loan.LoanStatus.LATE)
                .mapToDouble(Loan::getRemainingBalance)
                .sum();
            
            return Math.max(0, baseAmount - existingDebt);
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
    
    /**
     * Reload configuration for bank manager
     */
    public void reloadConfiguration() {
        // TODO: Implement configuration reload when needed
        // This is a placeholder to fix compilation errors
    }
    
    /**
     * Get total account count for statistics
     */
    public int getTotalAccountCount() {
        return accountsByNumber.size();
    }
    
    /**
     * Get active loans count for statistics
     */
    public int getActiveLoansCount() {
        return (int) activeLoans.values().stream()
            .filter(loan -> loan.getStatus() == Loan.LoanStatus.CURRENT || loan.getStatus() == Loan.LoanStatus.LATE)
            .count();
    }
    
    /**
     * Deposit money into an account
     * 
     * @param accountId The account ID
     * @param amount The amount to deposit
     * @return true if successful
     */
    public boolean deposit(UUID accountId, double amount) {
        BankAccount account = getAccountById(accountId);
        if (account == null) {
            return false;
        }
        
        Currency defaultCurrency = CurrencyManager.getInstance().getDefaultCurrency();
        return account.deposit(defaultCurrency, amount);
    }
    
    /**
     * Withdraw money from an account
     * 
     * @param accountId The account ID
     * @param amount The amount to withdraw
     * @return true if successful
     */
    public boolean withdraw(UUID accountId, double amount) {
        BankAccount account = getAccountById(accountId);
        if (account == null) {
            return false;
        }
        
        Currency defaultCurrency = CurrencyManager.getInstance().getDefaultCurrency();
        return account.withdraw(defaultCurrency, amount);
    }
    
    /**
     * Transfer money between accounts
     * 
     * @param fromAccountId Source account ID
     * @param toAccountId Target account ID
     * @param amount Amount to transfer
     * @return true if successful
     */
    public boolean transfer(UUID fromAccountId, UUID toAccountId, double amount) {
        Currency defaultCurrency = CurrencyManager.getInstance().getDefaultCurrency();
        return transfer(fromAccountId, toAccountId, defaultCurrency, amount, "Transfer");
    }
    
    /**
     * Apply for a loan
     * 
     * @param playerId Player ID
     * @param amount Loan amount
     * @param loanType Type of loan
     * @param termMonths Term in months
     * @return Loan ID as string if successful, null otherwise
     */
    public String applyForLoan(UUID playerId, double amount, Loan.LoanType loanType, int termMonths) {
        return loanManager.applyForLoan(playerId, amount, loanType, termMonths);
    }
    
    /**
     * Approve a loan
     * 
     * @param loanId Loan ID
     * @return true if successful
     */
    public boolean approveLoan(UUID loanId) {
        return loanManager.approveLoan(loanId);
    }
    
    /**
     * Make a loan payment
     * 
     * @param playerId Player ID
     * @param loanId Loan ID
     * @param amount Payment amount
     * @return true if successful
     */
    public boolean makeLoanPayment(UUID playerId, UUID loanId, double amount) {
        return loanManager.makePayment(playerId, loanId, amount);
    }
    
    /**
     * Get account by ID
     * 
     * @param accountId Account ID
     * @return BankAccount or null if not found
     */
    private BankAccount getAccountById(UUID accountId) {
        for (List<BankAccount> accounts : playerAccounts.values()) {
            for (BankAccount account : accounts) {
                if (account.getAccountId().equals(accountId)) {
                    return account;
                }
            }
        }
        return null;
    }
}
