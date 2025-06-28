package com.zerog.neoessentials.economy;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Handles automated loan processing including status updates, interest accrual,
 * and overdue loan management for the NeoEssentials banking system.
 */
public class LoanProcessor {
    private static LoanProcessor instance;
    private final ScheduledExecutorService scheduler;
    private final BankManager bankManager;
    private final EconomyManager economyManager;
    private boolean isRunning = false;
    
    private LoanProcessor() {
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.economyManager = EconomyManager.getInstance();
        this.bankManager = economyManager.getBankManager();
    }
    
    public static LoanProcessor getInstance() {
        if (instance == null) {
            instance = new LoanProcessor();
        }
        return instance;
    }
    
    /**
     * Start the loan processing scheduler
     */
    public void start() {
        if (isRunning) {
            return;
        }
        
        // Schedule daily loan processing (every 24 hours)
        scheduler.scheduleAtFixedRate(this::processDailyLoans, 0, 24, TimeUnit.HOURS);
        
        // Schedule hourly overdue checks (every hour)
        scheduler.scheduleAtFixedRate(this::checkOverdueLoans, 0, 1, TimeUnit.HOURS);
        
        isRunning = true;
        System.out.println("[NeoEssentials] Loan processor started - daily processing and hourly overdue checks.");
    }
    
    /**
     * Stop the loan processing scheduler
     */
    public void stop() {
        if (!isRunning) {
            return;
        }
        
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        isRunning = false;
        System.out.println("[NeoEssentials] Loan processor stopped.");
    }
    
    /**
     * Process all loans daily - update statuses, accrue interest, handle defaults
     */
    private void processDailyLoans() {
        try {
            List<Loan> allLoans = bankManager.getAllActiveLoans();
            int processedCount = 0;
            int overdueCount = 0;
            int defaultCount = 0;
            
            for (Loan loan : allLoans) {
                if (loan.getStatus() == Loan.LoanStatus.CURRENT || 
                    loan.getStatus() == Loan.LoanStatus.LATE) {
                    
                    // Check if payment is overdue
                    if (loan.isOverdue()) {
                        long daysOverdue = getDaysOverdue(loan);
                        
                        if (daysOverdue > 30) { // Default after 30 days
                            loan.setStatus(Loan.LoanStatus.DEFAULT);
                            defaultCount++;
                            
                            // Notify player about default status
                            LoanNotificationManager.getInstance().notifyLoanDefault(loan);
                            
                            System.out.println("[NeoEssentials] Loan " + loan.getLoanId().toString().substring(0, 8) + 
                                " moved to DEFAULT status (overdue " + daysOverdue + " days)");
                        } else if (loan.getStatus() != Loan.LoanStatus.LATE) {
                            loan.setStatus(Loan.LoanStatus.LATE);
                            overdueCount++;
                            
                            // Apply late fee
                            applyLateFee(loan);
                            
                            System.out.println("[NeoEssentials] Loan " + loan.getLoanId().toString().substring(0, 8) + 
                                " marked as LATE (overdue " + daysOverdue + " days)");
                        }
                    }
                    
                    // Accrue daily interest (for demonstration - normally would be monthly)
                    accrueInterest(loan);
                    
                    // Save loan changes
                    saveLoan(loan);
                    processedCount++;
                }
            }
            
            // Send payment reminders for loans due soon
            LoanNotificationManager.getInstance().sendPaymentReminders();
            
            if (processedCount > 0) {
                System.out.println("[NeoEssentials] Daily loan processing complete: " + 
                    processedCount + " loans processed, " + overdueCount + " marked late, " + 
                    defaultCount + " moved to default");
            }
        } catch (Exception e) {
            System.err.println("[NeoEssentials] Error during daily loan processing: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Check for overdue loans every hour
     */
    private void checkOverdueLoans() {
        try {
            List<Loan> activeLoans = bankManager.getAllActiveLoans().stream()
                .filter(loan -> loan.getStatus() == Loan.LoanStatus.CURRENT)
                .toList();
            
            int newOverdueCount = 0;
            
            for (Loan loan : activeLoans) {
                if (loan.isOverdue()) {
                    loan.checkAndUpdateLateStatus();
                    if (loan.getStatus() == Loan.LoanStatus.LATE) {
                        newOverdueCount++;
                        saveLoan(loan);
                        
                        // Notify player about overdue loan
                        LoanNotificationManager.getInstance().notifyLoanOverdue(loan);
                    }
                }
            }
            
            if (newOverdueCount > 0) {
                System.out.println("[NeoEssentials] Hourly overdue check: " + newOverdueCount + " loans marked as overdue");
            }
        } catch (Exception e) {
            System.err.println("[NeoEssentials] Error during overdue loan check: " + e.getMessage());
        }
    }
    
    /**
     * Apply late fee to an overdue loan
     */
    private void applyLateFee(Loan loan) {
        try {
            // Calculate late fee (e.g., 5% of monthly payment or $25, whichever is higher)
            double lateFee = Math.max(loan.getMonthlyPayment() * 0.05, 25.0);
            
            // Add late fee to remaining balance
            double newBalance = loan.getCurrentBalance() + lateFee;
            loan.setCurrentBalance(newBalance);
            
            // Notify player about late fee
            LoanNotificationManager.getInstance().notifyLateFeeApplied(loan, lateFee);
            
            System.out.println("[NeoEssentials] Applied late fee of " + 
                loan.getCurrency().format(lateFee) + " to loan " + 
                loan.getLoanId().toString().substring(0, 8));
        } catch (Exception e) {
            System.err.println("[NeoEssentials] Error applying late fee to loan " + 
                loan.getLoanId().toString().substring(0, 8) + ": " + e.getMessage());
        }
    }
    
    /**
     * Accrue interest on a loan (simplified daily accrual)
     */
    private void accrueInterest(Loan loan) {
        try {
            // Calculate daily interest (annual rate / 365)
            double dailyRate = loan.getInterestRate() / 365.0;
            double dailyInterest = loan.getCurrentBalance() * dailyRate;
            
            // Add interest to balance
            double newBalance = loan.getCurrentBalance() + dailyInterest;
            loan.setCurrentBalance(newBalance);
            
            // Note: In a real system, you'd track this separately to distinguish 
            // principal from accrued interest
        } catch (Exception e) {
            System.err.println("[NeoEssentials] Error accruing interest for loan " + 
                loan.getLoanId().toString().substring(0, 8) + ": " + e.getMessage());
        }
    }
    
    /**
     * Get the number of days a loan is overdue
     */
    private long getDaysOverdue(Loan loan) {
        long currentTime = System.currentTimeMillis();
        long overdueTime = currentTime - loan.getNextPaymentDueTime();
        return overdueTime / (24 * 60 * 60 * 1000L); // Convert milliseconds to days
    }
    
    /**
     * Save loan to persistence
     */
    private void saveLoan(Loan loan) {
        try {
            if (bankManager.getPersistenceManager() != null) {
                bankManager.getPersistenceManager().saveLoan(loan);
            }
        } catch (Exception e) {
            System.err.println("[NeoEssentials] Error saving loan " + 
                loan.getLoanId().toString().substring(0, 8) + ": " + e.getMessage());
        }
    }
    
    /**
     * Get statistics about the loan system
     */
    public LoanSystemStats getStats() {
        try {
            List<Loan> allLoans = bankManager.getAllActiveLoans();
            
            int totalLoans = allLoans.size();
            int currentLoans = 0;
            int overdueLoans = 0;
            int defaultLoans = 0;
            int paidOffLoans = 0;
            double totalOutstanding = 0.0;
            double totalOverdue = 0.0;
            
            for (Loan loan : allLoans) {
                switch (loan.getStatus()) {
                    case CURRENT -> {
                        currentLoans++;
                        totalOutstanding += loan.getCurrentBalance();
                    }
                    case LATE -> {
                        overdueLoans++;
                        totalOutstanding += loan.getCurrentBalance();
                        totalOverdue += loan.getCurrentBalance();
                    }
                    case DEFAULT, FORECLOSED -> {
                        defaultLoans++;
                        totalOutstanding += loan.getCurrentBalance();
                        totalOverdue += loan.getCurrentBalance();
                    }
                    case PAID_OFF -> paidOffLoans++;
                    case PENDING, APPROVED -> {
                        // Pending and approved loans don't count towards outstanding balances yet
                    }
                }
            }
            
            return new LoanSystemStats(totalLoans, currentLoans, overdueLoans, 
                defaultLoans, paidOffLoans, totalOutstanding, totalOverdue);
        } catch (Exception e) {
            System.err.println("[NeoEssentials] Error getting loan stats: " + e.getMessage());
            return new LoanSystemStats(0, 0, 0, 0, 0, 0.0, 0.0);
        }
    }
    
    /**
     * Simple stats class for loan system metrics
     */
    public static class LoanSystemStats {
        public final int totalLoans;
        public final int currentLoans;
        public final int overdueLoans;
        public final int defaultLoans;
        public final int paidOffLoans;
        public final double totalOutstanding;
        public final double totalOverdue;
        
        public LoanSystemStats(int totalLoans, int currentLoans, int overdueLoans, 
                              int defaultLoans, int paidOffLoans, 
                              double totalOutstanding, double totalOverdue) {
            this.totalLoans = totalLoans;
            this.currentLoans = currentLoans;
            this.overdueLoans = overdueLoans;
            this.defaultLoans = defaultLoans;
            this.paidOffLoans = paidOffLoans;
            this.totalOutstanding = totalOutstanding;
            this.totalOverdue = totalOverdue;
        }
        
        @Override
        public String toString() {
            return String.format("LoanStats{total=%d, current=%d, overdue=%d, default=%d, paidOff=%d, outstanding=%.2f, overdue=%.2f}",
                totalLoans, currentLoans, overdueLoans, defaultLoans, paidOffLoans, totalOutstanding, totalOverdue);
        }
    }
    
    /**
     * Manual processing trigger for testing or admin use
     */
    public void processAllLoans() {
        System.out.println("[NeoEssentials] Manual loan processing triggered...");
        processDailyLoans();
        checkOverdueLoans();
    }
}
