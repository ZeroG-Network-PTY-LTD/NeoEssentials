package com.zerog.neoessentials.economy.bank;

import com.zerog.neoessentials.economy.EconomyManager;
import com.zerog.neoessentials.economy.TransactionType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Advanced Banking System
 * Features:
 * - Multiple account types (Savings, Checking, Business)
 * - Interest calculation and compound interest
 * - Loan system with credit scoring
 * - Investment accounts
 * - Bank transfers and wire transfers
 * - ATM and mobile banking simulation
 */
public class BankManager {
    private final EconomyManager economyManager;
    private final Map<String, BankAccount> accounts;
    private final Map<UUID, List<String>> playerAccounts;
    private final Map<String, Loan> loans;
    private final Map<UUID, CreditScore> creditScores;
    
    private final InterestCalculator interestCalculator;
    private final LoanProcessor loanProcessor;
    private final CreditScoreCalculator creditCalculator;
    
    private LocalDateTime lastInterestCalculation;
    private boolean bankingEnabled;
    
    public BankManager(EconomyManager economyManager) {
        this.economyManager = economyManager;
        this.accounts = new ConcurrentHashMap<>();
        this.playerAccounts = new ConcurrentHashMap<>();
        this.loans = new ConcurrentHashMap<>();
        this.creditScores = new ConcurrentHashMap<>();
        
        this.interestCalculator = new InterestCalculator();
        this.loanProcessor = new LoanProcessor(this);
        this.creditCalculator = new CreditScoreCalculator();
        
        this.lastInterestCalculation = LocalDateTime.now();
        this.bankingEnabled = true;
    }
    
    public void initialize() {
        try {
            // Load existing accounts and loans
            loadBankingData();
            
            System.out.println("Banking System initialized successfully");
        } catch (Exception e) {
            System.err.println("Failed to initialize Banking System: " + e.getMessage());
            bankingEnabled = false;
        }
    }
    
    /**
     * Create a new bank account
     */
    public BankAccountResult createAccount(UUID playerId, String accountName, 
                                         AccountType accountType, String currency) {
        if (!bankingEnabled) {
            return new BankAccountResult(false, "Banking system is disabled", null);
        }
        
        try {
            // Generate unique account ID
            String accountId = generateAccountId();
            
            // Create account
            BankAccount account = new BankAccount(accountId, playerId, accountName, 
                                                accountType, currency);
            
            // Set account-specific properties
            configureAccountType(account, accountType);
            
            // Save account
            accounts.put(accountId, account);
            playerAccounts.computeIfAbsent(playerId, k -> new ArrayList<>()).add(accountId);
            
            return new BankAccountResult(true, "Account created successfully", account);
            
        } catch (Exception e) {
            return new BankAccountResult(false, "Failed to create account: " + e.getMessage(), null);
        }
    }
    
    /**
     * Deposit money to bank account
     */
    public boolean deposit(String accountId, BigDecimal amount, String description) {
        BankAccount account = accounts.get(accountId);
        if (account == null || !account.isActive()) {
            return false;
        }
        
        // Check if player has enough money
        BigDecimal playerBalance = economyManager.getBalance(account.getOwnerId(), account.getCurrency());
        if (playerBalance.compareTo(amount) < 0) {
            return false;
        }
        
        // Process deposit
        boolean success = economyManager.removeBalance(account.getOwnerId(), account.getCurrency(), amount);
        if (success) {
            account.deposit(amount);
            
            // Record transaction
            economyManager.getTransactionManager().recordTransaction(
                account.getOwnerId(), null, account.getCurrency(), amount.negate(),
                TransactionType.BANK_DEPOSIT, description != null ? description : "Bank deposit"
            );
            
            saveAccount(account);
            updateCreditScore(account.getOwnerId(), amount, true);
            return true;
        }
        
        return false;
    }
    
    /**
     * Withdraw money from bank account
     */
    public boolean withdraw(String accountId, BigDecimal amount, String description) {
        BankAccount account = accounts.get(accountId);
        if (account == null || !account.isActive()) {
            return false;
        }
        
        // Check if account has enough money
        if (account.getBalance().compareTo(amount) < 0) {
            return false;
        }
        
        // Check withdrawal limits
        if (!checkWithdrawalLimits(account, amount)) {
            return false;
        }
        
        // Process withdrawal
        account.withdraw(amount);
        economyManager.addBalance(account.getOwnerId(), account.getCurrency(), amount);
        
        // Record transaction
        economyManager.getTransactionManager().recordTransaction(
            null, account.getOwnerId(), account.getCurrency(), amount,
            TransactionType.BANK_WITHDRAW, description != null ? description : "Bank withdrawal"
        );
        
        saveAccount(account);
        return true;
    }
    
    /**
     * Transfer money between bank accounts
     */
    public boolean transfer(String fromAccountId, String toAccountId, BigDecimal amount, String description) {
        BankAccount fromAccount = accounts.get(fromAccountId);
        BankAccount toAccount = accounts.get(toAccountId);
        
        if (fromAccount == null || toAccount == null || 
            !fromAccount.isActive() || !toAccount.isActive()) {
            return false;
        }
        
        // Check if accounts use same currency
        if (!fromAccount.getCurrency().equals(toAccount.getCurrency())) {
            return false; // Currency conversion would be needed
        }
        
        // Check balance
        if (fromAccount.getBalance().compareTo(amount) < 0) {
            return false;
        }
        
        // Calculate transfer fee
        BigDecimal fee = calculateTransferFee(fromAccount, toAccount, amount);
        BigDecimal totalDeduction = amount.add(fee);
        
        if (fromAccount.getBalance().compareTo(totalDeduction) < 0) {
            return false;
        }
        
        // Process transfer
        fromAccount.withdraw(totalDeduction);
        toAccount.deposit(amount);
        
        // Record transactions
        economyManager.getTransactionManager().recordTransaction(
            fromAccount.getOwnerId(), toAccount.getOwnerId(), fromAccount.getCurrency(), amount.negate(),
            TransactionType.TRANSFER_SEND, description != null ? description : "Bank transfer"
        );
        
        economyManager.getTransactionManager().recordTransaction(
            toAccount.getOwnerId(), fromAccount.getOwnerId(), toAccount.getCurrency(), amount,
            TransactionType.TRANSFER_RECEIVE, description != null ? description : "Bank transfer received"
        );
        
        if (fee.compareTo(BigDecimal.ZERO) > 0) {
            economyManager.getTransactionManager().recordTransaction(
                fromAccount.getOwnerId(), null, fromAccount.getCurrency(), fee.negate(),
                TransactionType.FEE, "Bank transfer fee"
            );
        }
        
        saveAccount(fromAccount);
        saveAccount(toAccount);
        return true;
    }
    
    /**
     * Apply for a loan
     */
    public LoanResult applyForLoan(UUID playerId, BigDecimal amount, String currency, 
                                  int termMonths, LoanType loanType) {
        
        // Check credit score
        CreditScore creditScore = getCreditScore(playerId);
        if (creditScore.getScore() < getMinimumCreditScore(loanType)) {
            return new LoanResult(false, "Credit score too low", null);
        }
        
        // Calculate loan terms
        LoanTerms terms = calculateLoanTerms(amount, termMonths, loanType, creditScore);
        
        // Create loan
        String loanId = UUID.randomUUID().toString();
        Loan loan = new Loan(loanId, playerId, amount, currency, terms.getInterestRate(), 
                           termMonths, loanType);
        
        // Disburse loan amount
        boolean success = economyManager.addBalance(playerId, currency, amount);
        if (success) {
            loans.put(loanId, loan);
            
            // Record loan transaction
            economyManager.getTransactionManager().recordTransaction(
                null, playerId, currency, amount,
                TransactionType.BANK_LOAN, "Loan disbursement"
            );
            
            updateCreditScore(playerId, amount, false);
            return new LoanResult(true, "Loan approved", loan);
        }
        
        return new LoanResult(false, "Failed to disburse loan", null);
    }
    
    /**
     * Make loan payment
     */
    public boolean makeLoanPayment(String loanId, BigDecimal amount) {
        Loan loan = loans.get(loanId);
        if (loan == null || loan.getStatus() != LoanStatus.ACTIVE) {
            return false;
        }
        
        // Check if player has enough money
        BigDecimal playerBalance = economyManager.getBalance(loan.getBorrowerId(), loan.getCurrency());
        if (playerBalance.compareTo(amount) < 0) {
            return false;
        }
        
        // Process payment
        boolean success = economyManager.removeBalance(loan.getBorrowerId(), loan.getCurrency(), amount);
        if (success) {
            loan.makePayment(amount);
            
            // Record transaction
            economyManager.getTransactionManager().recordTransaction(
                loan.getBorrowerId(), null, loan.getCurrency(), amount.negate(),
                TransactionType.BANK_LOAN_PAYMENT, "Loan payment"
            );
            
            updateCreditScore(loan.getBorrowerId(), amount, true);
            saveLoan(loan);
            return true;
        }
        
        return false;
    }
    
    /**
     * Process interest for all accounts
     */
    public void processInterest() {
        if (!bankingEnabled) {
            return;
        }
        
        LocalDateTime now = LocalDateTime.now();
        for (BankAccount account : accounts.values()) {
            if (account.isActive() && account.hasInterest()) {
                BigDecimal interest = interestCalculator.calculateInterest(account, lastInterestCalculation, now);
                if (interest.compareTo(BigDecimal.ZERO) > 0) {
                    account.deposit(interest);
                    
                    // Record interest transaction
                    economyManager.getTransactionManager().recordTransaction(
                        null, account.getOwnerId(), account.getCurrency(), interest,
                        TransactionType.BANK_INTEREST, "Interest earned"
                    );
                    
                    saveAccount(account);
                }
            }
        }
        
        lastInterestCalculation = now;
    }
    
    // Utility methods
    private void configureAccountType(BankAccount account, AccountType accountType) {
        switch (accountType) {
            case SAVINGS:
                account.setInterestRate(new BigDecimal("0.025")); // 2.5% annual
                account.setWithdrawalLimit(new BigDecimal("5000"));
                account.setHasInterest(true);
                break;
            case CHECKING:
                account.setInterestRate(new BigDecimal("0.005")); // 0.5% annual
                account.setWithdrawalLimit(new BigDecimal("10000"));
                account.setHasInterest(true);
                break;
            case BUSINESS:
                account.setInterestRate(new BigDecimal("0.015")); // 1.5% annual
                account.setWithdrawalLimit(new BigDecimal("50000"));
                account.setHasInterest(true);
                break;
            case INVESTMENT:
                account.setInterestRate(new BigDecimal("0.060")); // 6% annual
                account.setWithdrawalLimit(new BigDecimal("1000"));
                account.setHasInterest(true);
                break;
        }
    }
    
    private boolean checkWithdrawalLimits(BankAccount account, BigDecimal amount) {
        return account.getWithdrawalLimit() == null || 
               amount.compareTo(account.getWithdrawalLimit()) <= 0;
    }
    
    private BigDecimal calculateTransferFee(BankAccount fromAccount, BankAccount toAccount, BigDecimal amount) {
        BigDecimal feeRate = new BigDecimal("0.001"); // 0.1% base fee
        
        // Different bank fee
        if (!fromAccount.getOwnerId().equals(toAccount.getOwnerId())) {
            feeRate = feeRate.multiply(new BigDecimal("2"));
        }
        
        BigDecimal fee = amount.multiply(feeRate);
        BigDecimal maxFee = new BigDecimal("100");
        
        return fee.min(maxFee);
    }
    
    private CreditScore getCreditScore(UUID playerId) {
        return creditScores.computeIfAbsent(playerId, id -> new CreditScore(id));
    }
    
    private void updateCreditScore(UUID playerId, BigDecimal amount, boolean positive) {
        CreditScore score = getCreditScore(playerId);
        creditCalculator.updateScore(score, amount, positive);
    }
    
    private int getMinimumCreditScore(LoanType loanType) {
        switch (loanType) {
            case PERSONAL: return 600;
            case BUSINESS: return 650;
            case MORTGAGE: return 700;
            case AUTO: return 580;
            default: return 600;
        }
    }
    
    private LoanTerms calculateLoanTerms(BigDecimal amount, int termMonths, 
                                       LoanType loanType, CreditScore creditScore) {
        BigDecimal baseRate = new BigDecimal("0.05"); // 5% base rate
        
        // Adjust rate based on loan type
        switch (loanType) {
            case MORTGAGE:
                baseRate = new BigDecimal("0.035");
                break;
            case AUTO:
                baseRate = new BigDecimal("0.045");
                break;
            case BUSINESS:
                baseRate = new BigDecimal("0.065");
                break;
        }
        
        // Adjust rate based on credit score
        int score = creditScore.getScore();
        if (score >= 800) {
            baseRate = baseRate.multiply(new BigDecimal("0.9"));
        } else if (score >= 700) {
            baseRate = baseRate.multiply(new BigDecimal("0.95"));
        } else if (score < 600) {
            baseRate = baseRate.multiply(new BigDecimal("1.2"));
        }
        
        return new LoanTerms(baseRate, termMonths);
    }
    
    private String generateAccountId() {
        return "ACC" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    private void saveAccount(BankAccount account) {
        // Save account to storage
        // Implementation would depend on storage system
    }
    
    private void saveLoan(Loan loan) {
        // Save loan to storage
        // Implementation would depend on storage system
    }
    
    private void loadBankingData() {
        // Load accounts and loans from storage
        // Implementation would depend on storage system
    }
    
    // Getters
    public List<BankAccount> getPlayerAccounts(UUID playerId) {
        List<String> accountIds = playerAccounts.getOrDefault(playerId, new ArrayList<>());
        return accountIds.stream()
            .map(accounts::get)
            .filter(Objects::nonNull)
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    public BankAccount getAccount(String accountId) {
        return accounts.get(accountId);
    }
    
    public List<Loan> getPlayerLoans(UUID playerId) {
        return loans.values().stream()
            .filter(loan -> loan.getBorrowerId().equals(playerId))
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    public Loan getLoan(String loanId) {
        return loans.get(loanId);
    }
    
    public void shutdown() {
        bankingEnabled = false;
        
        // Save all accounts and loans
        for (BankAccount account : accounts.values()) {
            saveAccount(account);
        }
        
        for (Loan loan : loans.values()) {
            saveLoan(loan);
        }
    }
    
    // Result classes
    public static class BankAccountResult {
        private final boolean successful;
        private final String message;
        private final BankAccount account;
        
        public BankAccountResult(boolean successful, String message, BankAccount account) {
            this.successful = successful;
            this.message = message;
            this.account = account;
        }
        
        public boolean isSuccessful() { return successful; }
        public String getMessage() { return message; }
        public BankAccount getAccount() { return account; }
    }
    
    public static class LoanResult {
        private final boolean successful;
        private final String message;
        private final Loan loan;
        
        public LoanResult(boolean successful, String message, Loan loan) {
            this.successful = successful;
            this.message = message;
            this.loan = loan;
        }
        
        public boolean isSuccessful() { return successful; }
        public String getMessage() { return message; }
        public Loan getLoan() { return loan; }
    }
    
    private static class LoanTerms {
        private final BigDecimal interestRate;
        private final int termMonths;
        
        public LoanTerms(BigDecimal interestRate, int termMonths) {
            this.interestRate = interestRate;
            this.termMonths = termMonths;
        }
        
        public BigDecimal getInterestRate() { return interestRate; }
        public int getTermMonths() { return termMonths; }
    }
}
