package com.zerog.neoessentials.economy.transactions;

import com.zerog.neoessentials.economy.Transaction;
import com.zerog.neoessentials.economy.TransactionType;

import java.math.BigDecimal;
// ...existing code...
import java.util.*;

/**
 * Transaction limits and validation
 */
public class TransactionLimits {
    private final Map<TransactionType, BigDecimal> maxAmounts;
    private final Map<TransactionType, BigDecimal> minAmounts;
    private final Map<TransactionType, Integer> dailyLimits;
    private final BigDecimal globalMaxAmount;
    private final BigDecimal globalMinAmount;
    
    public TransactionLimits() {
        this.maxAmounts = new HashMap<>();
        this.minAmounts = new HashMap<>();
        this.dailyLimits = new HashMap<>();
        this.globalMaxAmount = new BigDecimal("1000000"); // 1 million default max
        this.globalMinAmount = new BigDecimal("0.01"); // 1 cent default min
        
        initializeDefaults();
    }
    
    private void initializeDefaults() {
        // Set default limits for different transaction types
        maxAmounts.put(TransactionType.TRANSFER_SEND, new BigDecimal("100000"));
        maxAmounts.put(TransactionType.ADMIN_GIVE, new BigDecimal("1000000"));
        maxAmounts.put(TransactionType.BANK_LOAN, new BigDecimal("500000"));
        
        minAmounts.put(TransactionType.TRANSFER_SEND, new BigDecimal("1"));
        minAmounts.put(TransactionType.SHOP_PURCHASE, new BigDecimal("0.01"));
        
        dailyLimits.put(TransactionType.TRANSFER_SEND, 50);
        dailyLimits.put(TransactionType.BANK_WITHDRAW, 10);
    }
    
    public boolean isWithinLimits(Transaction transaction) {
        BigDecimal amount = transaction.getAmount().abs();
        TransactionType type = transaction.getType();
        
        // Check global limits
        if (amount.compareTo(globalMaxAmount) > 0 || amount.compareTo(globalMinAmount) < 0) {
            return false;
        }
        
        // Check type-specific limits
        BigDecimal maxAmount = maxAmounts.get(type);
        if (maxAmount != null && amount.compareTo(maxAmount) > 0) {
            return false;
        }
        
        BigDecimal minAmount = minAmounts.get(type);
        if (minAmount != null && amount.compareTo(minAmount) < 0) {
            return false;
        }
        
        return true;
    }
    
    public void setLimit(TransactionType type, BigDecimal maxAmount, BigDecimal minAmount, Integer dailyLimit) {
        if (maxAmount != null) {
            maxAmounts.put(type, maxAmount);
        }
        if (minAmount != null) {
            minAmounts.put(type, minAmount);
        }
        if (dailyLimit != null) {
            dailyLimits.put(type, dailyLimit);
        }
    }
    
    public BigDecimal getMaxAmount(TransactionType type) {
        return maxAmounts.getOrDefault(type, globalMaxAmount);
    }
    
    public BigDecimal getMinAmount(TransactionType type) {
        return minAmounts.getOrDefault(type, globalMinAmount);
    }
    
    public Integer getDailyLimit(TransactionType type) {
        return dailyLimits.get(type);
    }
}
