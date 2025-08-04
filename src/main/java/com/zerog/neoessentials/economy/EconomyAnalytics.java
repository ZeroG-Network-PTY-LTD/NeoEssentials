package com.zerog.neoessentials.economy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Economy analytics and statistics
 */
public class EconomyAnalytics {
    private BigDecimal totalMoney;
    private BigDecimal averageBalance;
    private BigDecimal medianBalance;
    private int totalPlayers;
    private int activePlayers;
    
    private Map<String, BigDecimal> currencyTotals;
    private Map<TransactionType, Integer> transactionCounts;
    private Map<TransactionType, BigDecimal> transactionVolumes;
    
    private LocalDateTime lastUpdate;
    private List<EconomyTrend> trends;
    
    public EconomyAnalytics() {
        this.currencyTotals = new HashMap<>();
        this.transactionCounts = new HashMap<>();
        this.transactionVolumes = new HashMap<>();
        this.trends = new ArrayList<>();
        this.lastUpdate = LocalDateTime.now();
    }
    
    public void update(Map<UUID, PlayerEconomyData> playerData, List<Transaction> recentTransactions) {
        calculatePlayerStatistics(playerData);
        calculateTransactionStatistics(recentTransactions);
        updateTrends();
        this.lastUpdate = LocalDateTime.now();
    }
    
    private void calculatePlayerStatistics(Map<UUID, PlayerEconomyData> playerData) {
        totalPlayers = playerData.size();
        activePlayers = 0;
        currencyTotals.clear();
        
        List<BigDecimal> allBalances = new ArrayList<>();
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        
        for (PlayerEconomyData data : playerData.values()) {
            if (data.getLastActivity().isAfter(cutoff)) {
                activePlayers++;
            }
            
            for (Map.Entry<String, BigDecimal> entry : data.getAllBalances().entrySet()) {
                String currency = entry.getKey();
                BigDecimal balance = entry.getValue();
                
                currencyTotals.merge(currency, balance, BigDecimal::add);
                allBalances.add(balance);
            }
        }
        
        // Calculate statistics
        totalMoney = allBalances.stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        if (!allBalances.isEmpty()) {
            averageBalance = totalMoney.divide(
                BigDecimal.valueOf(allBalances.size()), 
                2, java.math.RoundingMode.HALF_UP
            );
            
            // Calculate median
            Collections.sort(allBalances);
            int size = allBalances.size();
            if (size % 2 == 0) {
                medianBalance = allBalances.get(size / 2 - 1)
                    .add(allBalances.get(size / 2))
                    .divide(BigDecimal.valueOf(2), 2, java.math.RoundingMode.HALF_UP);
            } else {
                medianBalance = allBalances.get(size / 2);
            }
        }
    }
    
    private void calculateTransactionStatistics(List<Transaction> transactions) {
        transactionCounts.clear();
        transactionVolumes.clear();
        
        for (Transaction transaction : transactions) {
            TransactionType type = transaction.getType();
            BigDecimal amount = transaction.getAmount().abs();
            
            transactionCounts.merge(type, 1, Integer::sum);
            transactionVolumes.merge(type, amount, BigDecimal::add);
        }
    }
    
    private void updateTrends() {
        // Add current snapshot to trends
        EconomyTrend trend = new EconomyTrend(
            LocalDateTime.now(),
            totalMoney,
            averageBalance,
            totalPlayers,
            activePlayers
        );
        
        trends.add(trend);
        
        // Keep only last 30 days of trends
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        trends = trends.stream()
            .filter(t -> t.getTimestamp().isAfter(cutoff))
            .collect(Collectors.toList());
    }
    
    // Getters
    public BigDecimal getTotalMoney() { return totalMoney; }
    public BigDecimal getAverageBalance() { return averageBalance; }
    public BigDecimal getMedianBalance() { return medianBalance; }
    public int getTotalPlayers() { return totalPlayers; }
    public int getActivePlayers() { return activePlayers; }
    public Map<String, BigDecimal> getCurrencyTotals() { return new HashMap<>(currencyTotals); }
    public Map<TransactionType, Integer> getTransactionCounts() { return new HashMap<>(transactionCounts); }
    public Map<TransactionType, BigDecimal> getTransactionVolumes() { return new HashMap<>(transactionVolumes); }
    public LocalDateTime getLastUpdate() { return lastUpdate; }
    public List<EconomyTrend> getTrends() { return new ArrayList<>(trends); }
    
    public static class EconomyTrend {
        private final LocalDateTime timestamp;
        private final BigDecimal totalMoney;
        private final BigDecimal averageBalance;
        private final int totalPlayers;
        private final int activePlayers;
        
        public EconomyTrend(LocalDateTime timestamp, BigDecimal totalMoney, 
                           BigDecimal averageBalance, int totalPlayers, int activePlayers) {
            this.timestamp = timestamp;
            this.totalMoney = totalMoney;
            this.averageBalance = averageBalance;
            this.totalPlayers = totalPlayers;
            this.activePlayers = activePlayers;
        }
        
        public LocalDateTime getTimestamp() { return timestamp; }
        public BigDecimal getTotalMoney() { return totalMoney; }
        public BigDecimal getAverageBalance() { return averageBalance; }
        public int getTotalPlayers() { return totalPlayers; }
        public int getActivePlayers() { return activePlayers; }
    }
}
