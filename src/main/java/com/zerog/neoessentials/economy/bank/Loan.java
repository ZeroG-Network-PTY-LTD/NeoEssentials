package com.zerog.neoessentials.economy.bank;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a loan
 */
public class Loan {
    private final String loanId;
    private final UUID borrowerId;
    private final BigDecimal originalAmount;
    private final String currency;
    private final BigDecimal interestRate;
    private final int termMonths;
    private final LoanType loanType;
    private final LocalDateTime createdDate;
    
    private BigDecimal remainingBalance;
    private BigDecimal totalPaid;
    private int paymentsMade;
    private LocalDateTime lastPaymentDate;
    private LoanStatus status;
    private List<LoanPayment> paymentHistory;
    
    public Loan(String loanId, UUID borrowerId, BigDecimal originalAmount, 
               String currency, BigDecimal interestRate, int termMonths, LoanType loanType) {
        this.loanId = loanId;
        this.borrowerId = borrowerId;
        this.originalAmount = originalAmount;
        this.currency = currency;
        this.interestRate = interestRate;
        this.termMonths = termMonths;
        this.loanType = loanType;
        this.createdDate = LocalDateTime.now();
        
        this.remainingBalance = originalAmount;
        this.totalPaid = BigDecimal.ZERO;
        this.paymentsMade = 0;
        this.lastPaymentDate = null;
        this.status = LoanStatus.ACTIVE;
        this.paymentHistory = new ArrayList<>();
    }
    
    public void makePayment(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) > 0 && status == LoanStatus.ACTIVE) {
            // Calculate interest and principal portions
            BigDecimal interestPortion = calculateInterestPortion(amount);
            BigDecimal principalPortion = amount.subtract(interestPortion);
            
            // Ensure we don't pay more than the remaining balance
            if (principalPortion.compareTo(remainingBalance) > 0) {
                principalPortion = remainingBalance;
                amount = principalPortion.add(interestPortion);
            }
            
            // Update loan
            remainingBalance = remainingBalance.subtract(principalPortion);
            totalPaid = totalPaid.add(amount);
            paymentsMade++;
            lastPaymentDate = LocalDateTime.now();
            
            // Record payment
            LoanPayment payment = new LoanPayment(amount, principalPortion, 
                                                interestPortion, LocalDateTime.now());
            paymentHistory.add(payment);
            
            // Check if loan is paid off
            if (remainingBalance.compareTo(BigDecimal.ZERO) <= 0) {
                status = LoanStatus.PAID_OFF;
            }
        }
    }
    
    private BigDecimal calculateInterestPortion(BigDecimal paymentAmount) {
        // Simple interest calculation for this payment
        BigDecimal monthlyRate = interestRate.divide(BigDecimal.valueOf(12), 6, java.math.RoundingMode.HALF_UP);
        BigDecimal interestOwed = remainingBalance.multiply(monthlyRate);
        
        // Interest portion is the minimum of payment amount and interest owed
        return paymentAmount.min(interestOwed);
    }
    
    public BigDecimal calculateMonthlyPayment() {
        if (termMonths <= 0 || interestRate.compareTo(BigDecimal.ZERO) <= 0) {
            return originalAmount.divide(BigDecimal.valueOf(termMonths), 2, java.math.RoundingMode.HALF_UP);
        }
        
        // Monthly payment calculation using loan formula
        BigDecimal monthlyRate = interestRate.divide(BigDecimal.valueOf(12), 6, java.math.RoundingMode.HALF_UP);
        BigDecimal onePlusRate = BigDecimal.ONE.add(monthlyRate);
        BigDecimal powerTerm = onePlusRate.pow(termMonths);
        
        BigDecimal numerator = originalAmount.multiply(monthlyRate).multiply(powerTerm);
        BigDecimal denominator = powerTerm.subtract(BigDecimal.ONE);
        
        return numerator.divide(denominator, 2, java.math.RoundingMode.HALF_UP);
    }
    
    // Getters
    public String getLoanId() { return loanId; }
    public UUID getBorrowerId() { return borrowerId; }
    public BigDecimal getOriginalAmount() { return originalAmount; }
    public String getCurrency() { return currency; }
    public BigDecimal getInterestRate() { return interestRate; }
    public int getTermMonths() { return termMonths; }
    public LoanType getLoanType() { return loanType; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public BigDecimal getRemainingBalance() { return remainingBalance; }
    public BigDecimal getTotalPaid() { return totalPaid; }
    public int getPaymentsMade() { return paymentsMade; }
    public LocalDateTime getLastPaymentDate() { return lastPaymentDate; }
    public LoanStatus getStatus() { return status; }
    public List<LoanPayment> getPaymentHistory() { return new ArrayList<>(paymentHistory); }
    
    // Setters
    public void setStatus(LoanStatus status) { this.status = status; }
    
    @Override
    public String toString() {
        return String.format("Loan[%s] %s - %s %s remaining (%s)", 
                           loanId, loanType.getDisplayName(), 
                           remainingBalance, currency, status.getDisplayName());
    }
    
    /**
     * Represents a loan payment
     */
    public static class LoanPayment {
        private final BigDecimal totalAmount;
        private final BigDecimal principalAmount;
        private final BigDecimal interestAmount;
        private final LocalDateTime paymentDate;
        
        public LoanPayment(BigDecimal totalAmount, BigDecimal principalAmount, 
                          BigDecimal interestAmount, LocalDateTime paymentDate) {
            this.totalAmount = totalAmount;
            this.principalAmount = principalAmount;
            this.interestAmount = interestAmount;
            this.paymentDate = paymentDate;
        }
        
        public BigDecimal getTotalAmount() { return totalAmount; }
        public BigDecimal getPrincipalAmount() { return principalAmount; }
        public BigDecimal getInterestAmount() { return interestAmount; }
        public LocalDateTime getPaymentDate() { return paymentDate; }
    }
}
