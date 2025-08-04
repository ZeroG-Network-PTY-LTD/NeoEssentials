package com.zerog.neoessentials.economy.bank;

import java.math.BigDecimal;

/**
 * Simple credit score calculator
 */
public class CreditScoreCalculator {
    
    public void updateScore(CreditScore creditScore, BigDecimal amount, boolean positive) {
        int change = calculateScoreChange(amount, positive);
        creditScore.updateScore(change);
    }
    
    private int calculateScoreChange(BigDecimal amount, boolean positive) {
        // Simple score calculation based on transaction amount
        int baseChange = 1;
        
        if (amount.compareTo(new BigDecimal("1000")) > 0) {
            baseChange = 3;
        } else if (amount.compareTo(new BigDecimal("100")) > 0) {
            baseChange = 2;
        }
        
        return positive ? baseChange : -baseChange;
    }
}
