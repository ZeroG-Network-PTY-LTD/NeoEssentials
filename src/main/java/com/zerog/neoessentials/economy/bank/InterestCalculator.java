package com.zerog.neoessentials.economy.bank;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Simple interest calculator for bank accounts
 */
public class InterestCalculator {
    
    public BigDecimal calculateInterest(BankAccount account, LocalDateTime fromDate, LocalDateTime toDate) {
        if (!account.hasInterest() || account.getInterestRate().compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        // Calculate days between dates
        long daysBetween = ChronoUnit.DAYS.between(fromDate, toDate);
        if (daysBetween <= 0) {
            return BigDecimal.ZERO;
        }
        
        // Calculate daily interest rate
        BigDecimal annualRate = account.getInterestRate();
        BigDecimal dailyRate = annualRate.divide(BigDecimal.valueOf(365), 8, java.math.RoundingMode.HALF_UP);
        
        // Calculate interest
        BigDecimal interest = account.getBalance()
            .multiply(dailyRate)
            .multiply(BigDecimal.valueOf(daysBetween));
        
        return interest.setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
