package com.zerog.neoessentials.economy.transactions;

import com.zerog.neoessentials.economy.Transaction;
import com.zerog.neoessentials.economy.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Fraud detection system for transactions
 */
public class FraudDetector {
    private final Map<String, AlertLevel> alertRules;
    private final Map<UUID, FraudProfile> playerProfiles;
    
    public FraudDetector() {
        this.alertRules = new HashMap<>();
        this.playerProfiles = new HashMap<>();
        initializeRules();
    }
    
    private void initializeRules() {
        // High-frequency transaction rule
        alertRules.put("high_frequency", AlertLevel.MEDIUM);
        
        // Large amount rule
        alertRules.put("large_amount", AlertLevel.HIGH);
        
        // Unusual time rule
        alertRules.put("unusual_time", AlertLevel.LOW);
        
        // Rapid succession rule
        alertRules.put("rapid_succession", AlertLevel.MEDIUM);
    }
    
    public boolean isTransactionSuspicious(Transaction transaction, List<Transaction> playerHistory) {
        UUID playerId = transaction.getFromPlayer();
        if (playerId == null) {
            return false;
        }
        
        FraudProfile profile = getOrCreateProfile(playerId);
        
        int suspicionScore = 0;
        
        // Check for high frequency
        if (isHighFrequency(transaction, playerHistory)) {
            suspicionScore += 3;
        }
        
        // Check for large amount
        if (isLargeAmount(transaction, profile)) {
            suspicionScore += 5;
        }
        
        // Check for unusual time
        if (isUnusualTime(transaction, profile)) {
            suspicionScore += 2;
        }
        
        // Check for rapid succession
        if (isRapidSuccession(transaction, playerHistory)) {
            suspicionScore += 4;
        }
        
        // Update profile
        profile.addTransaction(transaction);
        
        // Return true if suspicion score exceeds threshold
        return suspicionScore >= 7;
    }
    
    private boolean isHighFrequency(Transaction transaction, List<Transaction> history) {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long recentCount = history.stream()
            .filter(t -> t.getTimestamp().isAfter(oneHourAgo))
            .count();
        
        return recentCount > 20; // More than 20 transactions in an hour
    }
    
    private boolean isLargeAmount(Transaction transaction, FraudProfile profile) {
        BigDecimal amount = transaction.getAmount().abs();
        BigDecimal averageAmount = profile.getAverageTransactionAmount();
        
        if (averageAmount.compareTo(BigDecimal.ZERO) == 0) {
            return amount.compareTo(new BigDecimal("10000")) > 0; // Default large amount
        }
        
        // Amount is 10x larger than usual
        return amount.compareTo(averageAmount.multiply(BigDecimal.TEN)) > 0;
    }
    
    private boolean isUnusualTime(Transaction transaction, FraudProfile profile) {
        int hour = transaction.getTimestamp().getHour();
        Set<Integer> usualHours = profile.getUsualTransactionHours();
        
        if (usualHours.isEmpty()) {
            return false; // No pattern established yet
        }
        
        return !usualHours.contains(hour);
    }
    
    private boolean isRapidSuccession(Transaction transaction, List<Transaction> history) {
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        long recentCount = history.stream()
            .filter(t -> t.getTimestamp().isAfter(fiveMinutesAgo))
            .count();
        
        return recentCount > 5; // More than 5 transactions in 5 minutes
    }
    
    private FraudProfile getOrCreateProfile(UUID playerId) {
        return playerProfiles.computeIfAbsent(playerId, FraudProfile::new);
    }
    
    public enum AlertLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }
    
    private static class FraudProfile {
        private final List<BigDecimal> transactionAmounts;
        private final Set<Integer> transactionHours;
        private final Map<TransactionType, Integer> typeFrequency;
        
        public FraudProfile(UUID playerId) {
            this.transactionAmounts = new ArrayList<>();
            this.transactionHours = new HashSet<>();
            this.typeFrequency = new HashMap<>();
        }
        
        public void addTransaction(Transaction transaction) {
            transactionAmounts.add(transaction.getAmount().abs());
            transactionHours.add(transaction.getTimestamp().getHour());
            typeFrequency.merge(transaction.getType(), 1, Integer::sum);
            
            // Keep only last 100 transactions for analysis
            if (transactionAmounts.size() > 100) {
                transactionAmounts.remove(0);
            }
        }
        
        public BigDecimal getAverageTransactionAmount() {
            if (transactionAmounts.isEmpty()) {
                return BigDecimal.ZERO;
            }
            
            BigDecimal sum = transactionAmounts.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            return sum.divide(BigDecimal.valueOf(transactionAmounts.size()), 
                            2, java.math.RoundingMode.HALF_UP);
        }
        
        public Set<Integer> getUsualTransactionHours() {
            return new HashSet<>(transactionHours);
        }
    }
}
