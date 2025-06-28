package com.zerog.neoessentials.economy;

import java.util.*;

/**
 * Represents a loan in the NeoEssentials banking system.
 * Supports different types of loans with varying terms and conditions.
 */
public class Loan {
    private final UUID loanId;
    private final UUID borrowerId;
    private final double principalAmount;
    private final Currency currency;
    private final LoanType loanType;
    private final int termMonths;
    private final double interestRate; // Annual rate
    private final long createdTime;
    private final double monthlyPayment;
    private final List<LoanPayment> payments;
    
    private double remainingBalance;
    private LoanStatus status;
    private long nextPaymentDue;
    private int paymentsRemaining;
    private double totalInterestPaid;
    
    public enum LoanType {
        PERSONAL("Personal Loan", 500, 50000, 60, true),
        MORTGAGE("Mortgage", 10000, 1000000, 360, false),
        BUSINESS("Business Loan", 1000, 500000, 120, true);
        
        private final String displayName;
        private final double minAmount;
        private final double maxAmount;
        private final int maxTermMonths;
        private final boolean requiresCollateral;
        
        LoanType(String displayName, double minAmount, double maxAmount, 
                int maxTermMonths, boolean requiresCollateral) {
            this.displayName = displayName;
            this.minAmount = minAmount;
            this.maxAmount = maxAmount;
            this.maxTermMonths = maxTermMonths;
            this.requiresCollateral = requiresCollateral;
        }
        
        public String getDisplayName() { return displayName; }
        public double getMinAmount() { return minAmount; }
        public double getMaxAmount() { return maxAmount; }
        public int getMaxTermMonths() { return maxTermMonths; }
        public boolean requiresCollateral() { return requiresCollateral; }
    }
    
    public enum LoanStatus {
        PENDING,     // Application pending approval
        APPROVED,    // Loan approved, funds disbursed
        CURRENT,     // Loan in good standing
        LATE,        // Payment overdue
        DEFAULT,     // Loan in default
        PAID_OFF,    // Loan fully paid
        FORECLOSED   // Loan foreclosed
    }
    
    /**
     * Create a new loan
     * 
     * @param borrowerId The borrower's UUID
     * @param principalAmount The loan amount
     * @param currency The loan currency
     * @param loanType The type of loan
     * @param termMonths Loan term in months
     * @param interestRate Annual interest rate
     */
    public Loan(UUID borrowerId, double principalAmount, Currency currency, 
               LoanType loanType, int termMonths, double interestRate) {
        this.loanId = UUID.randomUUID();
        this.borrowerId = borrowerId;
        this.principalAmount = principalAmount;
        this.currency = currency;
        this.loanType = loanType;
        this.termMonths = termMonths;
        this.interestRate = interestRate;
        this.createdTime = System.currentTimeMillis();
        this.payments = new ArrayList<>();
        this.remainingBalance = principalAmount;
        this.status = LoanStatus.PENDING;
        this.paymentsRemaining = termMonths;
        this.totalInterestPaid = 0.0;
        
        // Calculate monthly payment using standard loan formula
        this.monthlyPayment = calculateMonthlyPayment();
        
        // Set first payment due date (30 days from creation)
        this.nextPaymentDue = createdTime + (30L * 24L * 60L * 60L * 1000L);
    }
    
    /**
     * Create a loan for persistence loading (preserves original UUID)
     * 
     * @param loanId The original loan UUID from database
     * @param borrowerId The borrower's UUID
     * @param principalAmount The loan amount
     * @param currency The loan currency
     * @param loanType The type of loan
     * @param termMonths Loan term in months
     * @param interestRate Annual interest rate
     */
    public Loan(UUID loanId, UUID borrowerId, double principalAmount, Currency currency, 
               LoanType loanType, int termMonths, double interestRate) {
        this.loanId = loanId; // Use provided UUID instead of generating new one
        this.borrowerId = borrowerId;
        this.principalAmount = principalAmount;
        this.currency = currency;
        this.loanType = loanType;
        this.termMonths = termMonths;
        this.interestRate = interestRate;
        this.createdTime = System.currentTimeMillis();
        this.payments = new ArrayList<>();
        this.remainingBalance = principalAmount;
        this.status = LoanStatus.PENDING;
        this.paymentsRemaining = termMonths;
        this.totalInterestPaid = 0.0;
        
        // Calculate monthly payment using standard loan formula
        this.monthlyPayment = calculateMonthlyPayment();
        
        // Set first payment due date (30 days from creation)
        this.nextPaymentDue = createdTime + (30L * 24L * 60L * 60L * 1000L);
    }
    
    /**
     * Calculate monthly payment using standard amortization formula
     * 
     * @return Monthly payment amount
     */
    private double calculateMonthlyPayment() {
        if (interestRate == 0) {
            return principalAmount / termMonths;
        }
        
        double monthlyRate = interestRate / 12.0;
        double factor = Math.pow(1 + monthlyRate, termMonths);
        return principalAmount * (monthlyRate * factor) / (factor - 1);
    }
    
    /**
     * Make a payment on the loan
     * 
     * @param amount Payment amount
     * @param paymentSource Source of payment (account number, etc.)
     * @return true if payment was successful
     */
    public boolean makePayment(double amount, String paymentSource) {
        if (status != LoanStatus.CURRENT && status != LoanStatus.LATE) {
            return false; // Cannot make payments on inactive loans
        }
        
        if (amount <= 0) {
            return false;
        }
        
        // Calculate interest and principal portions
        double monthlyInterestRate = interestRate / 12.0;
        double interestPortion = remainingBalance * monthlyInterestRate;
        double principalPortion = Math.min(amount - interestPortion, remainingBalance);
        
        // Ensure payment covers at least the interest
        if (amount < interestPortion) {
            return false; // Payment too small
        }
        
        // Create payment record
        LoanPayment payment = new LoanPayment(
            amount, 
            interestPortion, 
            principalPortion, 
            System.currentTimeMillis(),
            paymentSource
        );
        payments.add(payment);
        
        // Update loan state
        remainingBalance -= principalPortion;
        totalInterestPaid += interestPortion;
        paymentsRemaining--;
        
        // Update next payment due date
        nextPaymentDue += (30L * 24L * 60L * 60L * 1000L); // Add 30 days
        
        // Check if loan is paid off
        if (remainingBalance <= 0.01) { // Small tolerance for floating point
            status = LoanStatus.PAID_OFF;
            remainingBalance = 0.0;
            paymentsRemaining = 0;
        } else {
            status = LoanStatus.CURRENT;
        }
        
        return true;
    }
    
    /**
     * Check if loan payment is overdue
     * 
     * @return true if payment is overdue
     */
    public boolean isOverdue() {
        return System.currentTimeMillis() > nextPaymentDue && status == LoanStatus.CURRENT;
    }
    
    /**
     * Mark loan as late if payment is overdue
     */
    public void checkAndUpdateLateStatus() {
        if (isOverdue() && status == LoanStatus.CURRENT) {
            status = LoanStatus.LATE;
        }
    }
    
    /**
     * Calculate remaining total cost of loan
     * 
     * @return Total remaining payments
     */
    public double getRemainingTotalCost() {
        return monthlyPayment * paymentsRemaining;
    }
    
    /**
     * Get loan payment history
     * 
     * @return List of all payments made
     */
    public List<LoanPayment> getPaymentHistory() {
        return new ArrayList<>(payments);
    }
    
    /**
     * Approve the loan (admin action)
     */
    public void approve() {
        if (status == LoanStatus.PENDING) {
            status = LoanStatus.APPROVED;
        }
    }
    
    /**
     * Deny the loan (admin action)
     */
    public void deny() {
        if (status == LoanStatus.PENDING) {
            status = LoanStatus.FORECLOSED; // Use foreclosed to indicate denied
        }
    }
    
    // Getters
    public UUID getLoanId() { return loanId; }
    public UUID getBorrowerId() { return borrowerId; }
    public double getPrincipalAmount() { return principalAmount; }
    public Currency getCurrency() { return currency; }
    public LoanType getLoanType() { return loanType; }
    public int getTermMonths() { return termMonths; }
    public double getInterestRate() { return interestRate; }
    public long getCreatedTime() { return createdTime; }
    public double getMonthlyPayment() { return monthlyPayment; }
    public double getRemainingBalance() { return remainingBalance; }
    public LoanStatus getStatus() { return status; }
    public long getNextPaymentDueTime() { return nextPaymentDue; }
    public int getPaymentsRemaining() { return paymentsRemaining; }
    public double getTotalInterestPaid() { return totalInterestPaid; }
    
    // Setter methods for persistence layer
    public void setCurrentBalance(double balance) { this.remainingBalance = balance; }
    public void setRemainingPayments(int payments) { this.paymentsRemaining = payments; }
    public void setStatus(LoanStatus status) { this.status = status; }
    public void setCreatedDate(Date date) { /* Created time is final, but this is for persistence compatibility */ }
    public void setLastPaymentDate(Date date) { /* For persistence - payment history handles this */ }
    public void setNextPaymentDue(Date date) { this.nextPaymentDue = date.getTime(); }
    
    /**
     * Set the next payment due time
     */
    public void setNextPaymentDueTime(long nextPaymentDue) {
        this.nextPaymentDue = nextPaymentDue;
    }
    
    // Additional getters for persistence
    public double getCurrentBalance() { return remainingBalance; }
    public LoanType getType() { return loanType; }
    public Date getCreatedDate() { return new Date(createdTime); }
    public Date getLastPaymentDate() { 
        // Return date of last payment if available
        if (!payments.isEmpty()) {
            LoanPayment lastPayment = payments.get(payments.size() - 1);
            return new Date(lastPayment.getPaymentTime());
        }
        return null;
    }
    public Date getNextPaymentDue() { return new Date(nextPaymentDue); }
    
    // Method to get payment history (for tests and admin viewing)
    public List<LoanPayment> getPayments() { return new ArrayList<>(payments); }
    
    @Override
    public String toString() {
        return "Loan{" +
                "loanId=" + loanId +
                ", type=" + loanType +
                ", amount=" + currency.format(principalAmount) +
                ", remaining=" + currency.format(remainingBalance) +
                ", status=" + status +
                '}';
    }
    
    /**
     * Inner class representing a loan payment
     */
    public static class LoanPayment {
        private final double amount;
        private final double interestPortion;
        private final double principalPortion;
        private final long paymentTime;
        private final String source;
        
        public LoanPayment(double amount, double interestPortion, double principalPortion, 
                          long paymentTime, String source) {
            this.amount = amount;
            this.interestPortion = interestPortion;
            this.principalPortion = principalPortion;
            this.paymentTime = paymentTime;
            this.source = source;
        }
        
        public double getAmount() { return amount; }
        public double getInterestPortion() { return interestPortion; }
        public double getPrincipalPortion() { return principalPortion; }
        public long getPaymentTime() { return paymentTime; }
        public String getSource() { return source; }
    }
}
