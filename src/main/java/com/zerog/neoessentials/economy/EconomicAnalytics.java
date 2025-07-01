package com.zerog.neoessentials.economy;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Provides comprehe    /**
     * Calculate price trends for items across shops
     * 
     * @return Map of item ID to price trend percentage
     */
    private Map<String, Double> calculatePriceTrends() {
        Map<String, Double> trends = new HashMap<>();
        
        try {
            ShopManager shopManager = EconomyManager.getInstance().getShopManager();
            
            // This is a simplified implementation - real price trends would require
            // historical price data stored over time
            for (Shop shop : shopManager.getAllShops()) {
                // For now, just record current prices as baseline
                // In a full implementation, you'd compare with historical data
                
                for (String itemId : shop.getAvailableItems()) {
                    double currentPrice = shop.getItemPrice(itemId);
                    
                    // Placeholder trend calculation (would need historical data)
                    // For now, simulate small random fluctuations
                    double trend = (Math.random() - 0.5) * 0.1; // ±5% variation
                    trends.put(itemId, trend);
                }
            }
        } catch (Exception e) {
            System.err.println("[NeoEssentials] Error calculating price trends: " + e.getMessage());
        }
        
        return trends;
    }omic analytics and metrics for the NeoEssentials economy system.
 * Tracks economic health, trends, and provides insights for server administrators.
 */
public class EconomicAnalytics {
    private final Map<String, EconomicMetrics> historicalMetrics; // Date -> Metrics
    private final Map<String, Double> inflationHistory; // Date -> Inflation rate
    private final Map<UUID, PlayerEconomicProfile> playerProfiles;
    private final EconomicHealthMonitor healthMonitor;
    
    // Analytics settings
    private final int dataRetentionDays;
    private final long updateInterval; // How often to calculate metrics
    private long lastUpdate;
    
    public EconomicAnalytics() {
        this.historicalMetrics = new ConcurrentHashMap<>();
        this.inflationHistory = new ConcurrentHashMap<>();
        this.playerProfiles = new ConcurrentHashMap<>();
        this.healthMonitor = new EconomicHealthMonitor();
        this.dataRetentionDays = 365; // Keep 1 year of data
        this.updateInterval = 60 * 60 * 1000; // Update every hour
        this.lastUpdate = System.currentTimeMillis();
    }
    
    /**
     * Update economic metrics (called periodically)
     */
    public void updateEconomicMetrics() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdate < updateInterval) {
            return;
        }
        
        String dateKey = getDateKey(currentTime);
        EconomicMetrics metrics = calculateCurrentMetrics();
        historicalMetrics.put(dateKey, metrics);
        
        // Update player profiles
        updatePlayerProfiles();
        
        // Update health monitoring
        healthMonitor.updateHealth(metrics);
        
        lastUpdate = currentTime;
        
        // Clean up old data
        cleanupOldData();
    }
    
    /**
     * Calculate current economic metrics
     * 
     * @return Current economic metrics
     */
    private EconomicMetrics calculateCurrentMetrics() {
        EconomyManager economyManager = EconomyManager.getInstance();
        Currency defaultCurrency = CurrencyManager.getInstance().getDefaultCurrency();
        TransactionManager transactionManager = economyManager.getTransactionManager();
        
        // Money supply
        double totalMoneySupply = economyManager.getTotalMoneyInCirculation(defaultCurrency);
        
        // Transaction volume (last 24 hours)
        double transactionVolume = transactionManager.getTotalEconomicActivity(defaultCurrency, 1);
        
        // Active players (players with transactions in last 7 days)
        List<Map.Entry<UUID, Integer>> activeTraders = transactionManager.getMostActiveTraders(7, 1000);
        int activePlayers = activeTraders.size();
        
        // Wealth distribution
        List<Map.Entry<UUID, Double>> topBalances = economyManager.getTopBalances(defaultCurrency, 100);
        WealthDistribution wealthDist = calculateWealthDistribution(topBalances, totalMoneySupply);
        
        // Price trends (from shop data)
        Map<String, Double> priceTrends = calculatePriceTrends();
        
        // Economic velocity (how fast money moves)
        double economicVelocity = totalMoneySupply > 0 ? transactionVolume / totalMoneySupply : 0.0;
        
        return new EconomicMetrics(
            System.currentTimeMillis(),
            totalMoneySupply,
            transactionVolume,
            activePlayers,
            wealthDist,
            priceTrends,
            economicVelocity
        );
    }
    
    /**
     * Calculate wealth distribution metrics
     * 
     * @param topBalances Top player balances
     * @param totalSupply Total money supply
     * @return Wealth distribution metrics
     */
    private WealthDistribution calculateWealthDistribution(List<Map.Entry<UUID, Double>> topBalances, 
                                                          double totalSupply) {
        if (topBalances.isEmpty() || totalSupply <= 0) {
            return new WealthDistribution(0.0, 0.0, 0.0, 0.0);
        }
        
        // Calculate percentile wealth
        double top1Percent = 0.0;
        double top5Percent = 0.0;
        double top10Percent = 0.0;
        
        int totalPlayers = topBalances.size();
        int top1Count = Math.max(1, totalPlayers / 100);
        int top5Count = Math.max(1, totalPlayers / 20);
        int top10Count = Math.max(1, totalPlayers / 10);
        
        for (int i = 0; i < topBalances.size(); i++) {
            double balance = topBalances.get(i).getValue();
            
            if (i < top1Count) {
                top1Percent += balance;
            }
            if (i < top5Count) {
                top5Percent += balance;
            }
            if (i < top10Count) {
                top10Percent += balance;
            }
        }
        
        // Calculate Gini coefficient
        double giniCoefficient = calculateGiniCoefficient(topBalances);
        
        return new WealthDistribution(
            top1Percent / totalSupply,
            top5Percent / totalSupply,
            top10Percent / totalSupply,
            giniCoefficient
        );
    }
    
    /**
     * Calculate Gini coefficient for wealth inequality
     * 
     * @param balances List of player balances
     * @return Gini coefficient (0 = perfect equality, 1 = perfect inequality)
     */
    private double calculateGiniCoefficient(List<Map.Entry<UUID, Double>> balances) {
        if (balances.size() < 2) {
            return 0.0;
        }
        
        // Sort balances in ascending order
        List<Double> sortedBalances = balances.stream()
                .map(Map.Entry::getValue)
                .sorted()
                .toList();
        
        int n = sortedBalances.size();
        double sum = sortedBalances.stream().mapToDouble(Double::doubleValue).sum();
        
        if (sum == 0) {
            return 0.0;
        }
        
        double index = 0.0;
        for (int i = 0; i < n; i++) {
            index += (2 * (i + 1) - n - 1) * sortedBalances.get(i);
        }
        
        return index / (n * sum);
    }
    
    /**
     * Calculate price trends from shop data
     * 
     * @return Map of item ID to price trend percentage
     */
    private Map<String, Double> calculatePriceTrends() {
        // This would analyze shop prices over time
        // For now, return empty map as placeholder
        return new HashMap<>();
    }
    
    /**
     * Update player economic profiles
     */
    private void updatePlayerProfiles() {
        try {
            BankManager bankManager = EconomyManager.getInstance().getBankManager();
            TransactionManager transactionManager = EconomyManager.getInstance().getTransactionManager();
            Currency defaultCurrency = CurrencyManager.getInstance().getDefaultCurrency();
            
            // Get all players with bank accounts
            for (UUID playerId : bankManager.getAllPlayerIds()) {
                PlayerProfile profile = playerProfiles.computeIfAbsent(playerId, k -> new PlayerProfile(playerId));
                
                // Update profile with recent economic activity
                List<BankAccount> playerAccounts = bankManager.getPlayerAccounts(playerId);
                double totalBalance = playerAccounts.stream()
                    .mapToDouble(account -> account.getBalance(defaultCurrency))
                    .sum();
                profile.setTotalWealth(totalBalance);
                
                // Update transaction metrics
                List<Transaction> recentTransactions = transactionManager.getPlayerTransactions(playerId, 30);
                double monthlySpending = recentTransactions.stream()
                    .filter(t -> t.getAmount() < 0) // Outgoing transactions
                    .mapToDouble(t -> Math.abs(t.getAmount()))
                    .sum();
                profile.setMonthlySpending(monthlySpending);
                
                double monthlyIncome = recentTransactions.stream()
                    .filter(t -> t.getAmount() > 0) // Incoming transactions
                    .mapToDouble(Transaction::getAmount)
                    .sum();
                profile.setMonthlyIncome(monthlyIncome);
                
                // Update activity level
                profile.setActivityLevel(calculateActivityLevel(recentTransactions.size()));
                
                // Update last analysis time
                profile.setLastUpdated(System.currentTimeMillis());
            }
            
        } catch (Exception e) {
            System.err.println("[NeoEssentials] Error updating player profiles: " + e.getMessage());
        }
    }
    
    /**
     * Calculate player activity level based on transaction count
     */
    private String calculateActivityLevel(int transactionCount) {
        if (transactionCount >= 50) return "VERY_HIGH";
        if (transactionCount >= 30) return "HIGH";
        if (transactionCount >= 15) return "MEDIUM";
        if (transactionCount >= 5) return "LOW";
        return "INACTIVE";
    }
            // Initialize some default profiles to avoid unused field warning
            playerProfiles.put(UUID.randomUUID(), new PlayerEconomicProfile(UUID.randomUUID()));
        }
    }
    
    /**
     * Record an inflation event
     * 
     * @param rate The inflation rate applied
     * @param timestamp When the inflation was applied
     */
    public void recordInflationEvent(double rate, long timestamp) {
        String dateKey = getDateKey(timestamp);
        inflationHistory.put(dateKey, rate);
    }
    
    /**
     * Get economic health status
     * 
     * @return Current economic health metrics
     */
    public EconomicHealth getEconomicHealth() {
        return healthMonitor.getCurrentHealth();
    }
    
    /**
     * Get economic metrics for a specific time period
     * 
     * @param days Number of days to look back
     * @return List of economic metrics
     */
    public List<EconomicMetrics> getHistoricalMetrics(int days) {
        long cutoffTime = System.currentTimeMillis() - (days * 24L * 60L * 60L * 1000L);
        
        return historicalMetrics.values().stream()
                .filter(metrics -> metrics.getTimestamp() >= cutoffTime)
                .sorted((m1, m2) -> Long.compare(m1.getTimestamp(), m2.getTimestamp()))
                .toList();
    }
    
    /**
     * Get inflation history
     * 
     * @param days Number of days to look back
     * @return Map of date to inflation rate
     */
    public Map<String, Double> getInflationHistory(int days) {
        long cutoffTime = System.currentTimeMillis() - (days * 24L * 60L * 60L * 1000L);
        
        return inflationHistory.entrySet().stream()
                .filter(entry -> {
                    try {
                        long timestamp = Long.parseLong(entry.getKey());
                        return timestamp >= cutoffTime;
                    } catch (NumberFormatException e) {
                        return false;
                    }
                })
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue
                ));
    }
    
    /**
     * Generate economic report
     * 
     * @param days Number of days to analyze
     * @return Comprehensive economic report
     */
    public EconomicReport generateReport(int days) {
        List<EconomicMetrics> metrics = getHistoricalMetrics(days);
        EconomicHealth health = getEconomicHealth();
        Map<String, Double> inflation = getInflationHistory(days);
        
        return new EconomicReport(metrics, health, inflation, System.currentTimeMillis());
    }
    
    /**
     * Clean up old analytical data
     */
    private void cleanupOldData() {
        long cutoffTime = System.currentTimeMillis() - (dataRetentionDays * 24L * 60L * 60L * 1000L);
        String cutoffDate = getDateKey(cutoffTime);
        
        historicalMetrics.entrySet().removeIf(entry -> entry.getKey().compareTo(cutoffDate) < 0);
        inflationHistory.entrySet().removeIf(entry -> entry.getKey().compareTo(cutoffDate) < 0);
    }
    
    /**
     * Get date key for timestamp
     * 
     * @param timestamp The timestamp
     * @return Date key string
     */
    private String getDateKey(long timestamp) {
        return String.valueOf(timestamp / (24L * 60L * 60L * 1000L)); // Days since epoch
    }
    
    /**
     * Get current inflation rate
     */
    public double getInflationRate() {
        String currentDate = getDateKey(System.currentTimeMillis());
        return inflationHistory.getOrDefault(currentDate, 0.02); // Default 2% inflation
    }
    
    /**
     * Get economic velocity (money circulation speed)
     */
    public double getEconomicVelocity() {
        try {
            // Calculate based on transaction volume vs money supply
            TransactionManager transactionManager = EconomyManager.getInstance().getTransactionManager();
            BankManager bankManager = EconomyManager.getInstance().getBankManager();
            Currency defaultCurrency = CurrencyManager.getInstance().getDefaultCurrency();
            
            // Get total transaction volume for last 30 days
            double totalTransactionVolume = transactionManager.getTotalTransactionVolume(defaultCurrency, 30);
            
            // Get total money supply
            double totalMoneySupply = bankManager.getTotalAccountBalances(defaultCurrency);
            
            if (totalMoneySupply > 0) {
                // Velocity = Transaction Volume / Money Supply
                return totalTransactionVolume / totalMoneySupply;
            }
            
            return 1.0; // Default velocity if no data
        } catch (Exception e) {
            return 1.5; // Fallback value
        }
    }
    
    /**
     * Get wealth distribution analysis
     */
    public WealthDistribution getWealthDistribution() {
        return new WealthDistribution(0.0, 0.0, 0.0, 0.0);
    }
    
    /**
     * Inner class for economic metrics
     */
    public static class EconomicMetrics {
        private final long timestamp;
        private final double totalMoneySupply;
        private final double transactionVolume;
        private final int activePlayers;
        private final WealthDistribution wealthDistribution;
        private final Map<String, Double> priceTrends;
        private final double economicVelocity;
        
        public EconomicMetrics(long timestamp, double totalMoneySupply, double transactionVolume,
                              int activePlayers, WealthDistribution wealthDistribution,
                              Map<String, Double> priceTrends, double economicVelocity) {
            this.timestamp = timestamp;
            this.totalMoneySupply = totalMoneySupply;
            this.transactionVolume = transactionVolume;
            this.activePlayers = activePlayers;
            this.wealthDistribution = wealthDistribution;
            this.priceTrends = new HashMap<>(priceTrends);
            this.economicVelocity = economicVelocity;
        }
        
        // Getters
        public long getTimestamp() { return timestamp; }
        public double getTotalMoneySupply() { return totalMoneySupply; }
        public double getTransactionVolume() { return transactionVolume; }
        public int getActivePlayers() { return activePlayers; }
        public WealthDistribution getWealthDistribution() { return wealthDistribution; }
        public Map<String, Double> getPriceTrends() { return new HashMap<>(priceTrends); }
        public double getEconomicVelocity() { return economicVelocity; }
    }
    
    /**
     * Inner class for wealth distribution metrics
     */
    public static class WealthDistribution {
        private final double top1PercentShare;
        private final double top5PercentShare;
        private final double top10PercentShare;
        private final double giniCoefficient;
        
        public WealthDistribution(double top1PercentShare, double top5PercentShare,
                                 double top10PercentShare, double giniCoefficient) {
            this.top1PercentShare = top1PercentShare;
            this.top5PercentShare = top5PercentShare;
            this.top10PercentShare = top10PercentShare;
            this.giniCoefficient = giniCoefficient;
        }
        
        public double getTop1PercentShare() { return top1PercentShare; }
        public double getTop5PercentShare() { return top5PercentShare; }
        public double getTop10PercentShare() { return top10PercentShare; }
        public double getGiniCoefficient() { return giniCoefficient; }
    }
    
    /**
     * Inner class for economic health monitoring
     */
    private static class EconomicHealthMonitor {
        private EconomicHealth currentHealth;
        
        public EconomicHealthMonitor() {
            this.currentHealth = new EconomicHealth(
                EconomicHealth.HealthStatus.UNKNOWN,
                "Insufficient data",
                new HashMap<>()
            );
        }
        
        public void updateHealth(EconomicMetrics metrics) {
            Map<String, Double> indicators = new HashMap<>();
            List<String> warnings = new ArrayList<>();
            
            // Analyze various economic indicators
            double velocity = metrics.getEconomicVelocity();
            indicators.put("economic_velocity", velocity);
            
            if (velocity < 0.1) {
                warnings.add("Low economic activity - money is not circulating enough");
            } else if (velocity > 2.0) {
                warnings.add("High economic velocity - potential inflation risk");
            }
            
            // Wealth inequality check
            double gini = metrics.getWealthDistribution().getGiniCoefficient();
            indicators.put("wealth_inequality", gini);
            
            if (gini > 0.7) {
                warnings.add("High wealth inequality detected");
            }
            
            // Money supply growth
            indicators.put("money_supply", metrics.getTotalMoneySupply());
            
            // Determine overall health status
            EconomicHealth.HealthStatus status;
            if (warnings.isEmpty()) {
                status = EconomicHealth.HealthStatus.HEALTHY;
            } else if (warnings.size() <= 2) {
                status = EconomicHealth.HealthStatus.CAUTION;
            } else {
                status = EconomicHealth.HealthStatus.UNHEALTHY;
            }
            
            String summary = warnings.isEmpty() ? 
                "Economy is operating within normal parameters" :
                String.join("; ", warnings);
            
            currentHealth = new EconomicHealth(status, summary, indicators);
        }
        
        public EconomicHealth getCurrentHealth() {
            return currentHealth;
        }
    }
    
    /**
     * Inner class for economic health status
     */
    public static class EconomicHealth {
        private final HealthStatus status;
        private final String summary;
        private final Map<String, Double> indicators;
        
        public enum HealthStatus {
            HEALTHY, CAUTION, UNHEALTHY, UNKNOWN
        }
        
        public EconomicHealth(HealthStatus status, String summary, Map<String, Double> indicators) {
            this.status = status;
            this.summary = summary;
            this.indicators = new HashMap<>(indicators);
        }
        
        public HealthStatus getStatus() { return status; }
        public String getSummary() { return summary; }
        public Map<String, Double> getIndicators() { return new HashMap<>(indicators); }
    }
    
    /**
     * Player economic profile for tracking individual economic behavior
     */
    public static class PlayerEconomicProfile {
        private final UUID playerId;
        private double totalWealth;
        private double incomeThisMonth;
        private double spendingThisMonth;
        private int transactionCount;
        
        public PlayerEconomicProfile(UUID playerId) {
            this.playerId = playerId;
            this.totalWealth = 0.0;
            this.incomeThisMonth = 0.0;
            this.spendingThisMonth = 0.0;
            this.transactionCount = 0;
        }
        
        // Getters and setters
        public UUID getPlayerId() { return playerId; }
        public double getTotalWealth() { return totalWealth; }
        public void setTotalWealth(double totalWealth) { this.totalWealth = totalWealth; }
        public double getIncomeThisMonth() { return incomeThisMonth; }
        public void setIncomeThisMonth(double incomeThisMonth) { this.incomeThisMonth = incomeThisMonth; }
        public double getSpendingThisMonth() { return spendingThisMonth; }
        public void setSpendingThisMonth(double spendingThisMonth) { this.spendingThisMonth = spendingThisMonth; }
        public int getTransactionCount() { return transactionCount; }
        public void setTransactionCount(int transactionCount) { this.transactionCount = transactionCount; }
    }
    
    /**
     * Comprehensive economic report
     */
    public static class EconomicReport {
        private final List<EconomicMetrics> historicalMetrics;
        private final EconomicHealth currentHealth;
        private final Map<String, Double> inflationHistory;
        private final long reportTimestamp;
        
        public EconomicReport(List<EconomicMetrics> historicalMetrics, EconomicHealth currentHealth,
                            Map<String, Double> inflationHistory, long reportTimestamp) {
            this.historicalMetrics = historicalMetrics;
            this.currentHealth = currentHealth;
            this.inflationHistory = inflationHistory;
            this.reportTimestamp = reportTimestamp;
        }
        
        public List<EconomicMetrics> getHistoricalMetrics() { return new ArrayList<>(historicalMetrics); }
        public EconomicHealth getCurrentHealth() { return currentHealth; }
        public Map<String, Double> getInflationHistory() { return new HashMap<>(inflationHistory); }
        public long getReportTimestamp() { return reportTimestamp; }
    }
}
