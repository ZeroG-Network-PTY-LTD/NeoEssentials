package com.zerog.neoessentials.economy.analytics;

import net.minecraft.world.item.ItemStack;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Market data for a specific item type across all player shops
 */
public class ItemMarketData {
    
    private final ItemStack item;
    private final String itemName;
    
    private int totalQuantitySold = 0;
    private BigDecimal totalRevenue = BigDecimal.ZERO;
    private int numberOfTransactions = 0;
    
    private BigDecimal lowestPrice = null;
    private BigDecimal highestPrice = null;
    private final List<PriceDataPoint> priceHistory = Collections.synchronizedList(new ArrayList<>());
    
    private LocalDateTime firstSaleDate;
    private LocalDateTime lastSaleDate;
    
    private static final int MAX_PRICE_HISTORY = 100; // Keep last 100 price points
    
    public ItemMarketData(ItemStack item) {
        this.item = item;
        this.itemName = item.getHoverName().getString();
    }
    
    /**
     * Records a sale of this item type
     */
    public void addSale(int quantity, BigDecimal totalPrice) {
        try {
            totalQuantitySold += quantity;
            totalRevenue = totalRevenue.add(totalPrice);
            numberOfTransactions++;
            lastSaleDate = LocalDateTime.now();
            
            if (firstSaleDate == null) {
                firstSaleDate = LocalDateTime.now();
            }
            
            // Calculate unit price for this transaction
            BigDecimal unitPrice = totalPrice.divide(BigDecimal.valueOf(quantity), 2, BigDecimal.ROUND_HALF_UP);
            
            // Update price range
            if (lowestPrice == null || unitPrice.compareTo(lowestPrice) < 0) {
                lowestPrice = unitPrice;
            }
            if (highestPrice == null || unitPrice.compareTo(highestPrice) > 0) {
                highestPrice = unitPrice;
            }
            
            // Add to price history
            synchronized (priceHistory) {
                priceHistory.add(new PriceDataPoint(unitPrice, quantity, LocalDateTime.now()));
                
                // Keep only the most recent price points
                if (priceHistory.size() > MAX_PRICE_HISTORY) {
                    priceHistory.remove(0);
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error adding sale to item market data: " + e.getMessage());
        }
    }
    
    /**
     * Gets the average unit price across all sales
     */
    public BigDecimal getAveragePrice() {
        if (totalQuantitySold == 0) return BigDecimal.ZERO;
        return totalRevenue.divide(BigDecimal.valueOf(totalQuantitySold), 2, BigDecimal.ROUND_HALF_UP);
    }
    
    /**
     * Gets the current market price trend based on recent sales
     */
    public PriceTrend getPriceTrend() {
        synchronized (priceHistory) {
            if (priceHistory.size() < 2) return PriceTrend.STABLE;
            
            // Compare recent prices to determine trend
            int recentSamples = Math.min(10, priceHistory.size());
            List<PriceDataPoint> recent = priceHistory.subList(priceHistory.size() - recentSamples, priceHistory.size());
            
            BigDecimal oldAvg = calculateAveragePrice(recent.subList(0, recentSamples / 2));
            BigDecimal newAvg = calculateAveragePrice(recent.subList(recentSamples / 2, recentSamples));
            
            double changePercent = newAvg.subtract(oldAvg)
                .divide(oldAvg.max(BigDecimal.valueOf(0.01)), 4, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
            
            if (changePercent > 5.0) return PriceTrend.RISING;
            if (changePercent < -5.0) return PriceTrend.FALLING;
            return PriceTrend.STABLE;
        }
    }
    
    /**
     * Gets the price volatility (standard deviation of prices)
     */
    public BigDecimal getPriceVolatility() {
        synchronized (priceHistory) {
            if (priceHistory.size() < 2) return BigDecimal.ZERO;
            
            BigDecimal mean = getAveragePrice();
            BigDecimal sumSquaredDiffs = BigDecimal.ZERO;
            
            for (PriceDataPoint point : priceHistory) {
                BigDecimal diff = point.getPrice().subtract(mean);
                sumSquaredDiffs = sumSquaredDiffs.add(diff.multiply(diff));
            }
            
            BigDecimal variance = sumSquaredDiffs.divide(BigDecimal.valueOf(priceHistory.size()), 4, BigDecimal.ROUND_HALF_UP);
            return BigDecimal.valueOf(Math.sqrt(variance.doubleValue()));
        }
    }
    
    /**
     * Gets recent price history for analysis
     */
    public List<PriceDataPoint> getRecentPriceHistory(int limit) {
        synchronized (priceHistory) {
            int size = priceHistory.size();
            int start = Math.max(0, size - limit);
            return new ArrayList<>(priceHistory.subList(start, size));
        }
    }
    
    /**
     * Gets market activity level based on recent transactions
     */
    public MarketActivity getMarketActivity() {
        if (lastSaleDate == null) return MarketActivity.INACTIVE;
        
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusDays(7);
        
        if (lastSaleDate.isAfter(oneDayAgo)) {
            return numberOfTransactions > 5 ? MarketActivity.HIGH : MarketActivity.MODERATE;
        } else if (lastSaleDate.isAfter(oneWeekAgo)) {
            return MarketActivity.LOW;
        } else {
            return MarketActivity.INACTIVE;
        }
    }
    
    private BigDecimal calculateAveragePrice(List<PriceDataPoint> points) {
        if (points.isEmpty()) return BigDecimal.ZERO;
        
        BigDecimal sum = points.stream()
            .map(PriceDataPoint::getPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
            
        return sum.divide(BigDecimal.valueOf(points.size()), 2, BigDecimal.ROUND_HALF_UP);
    }
    
    // Getters
    public ItemStack getItem() { return item; }
    public String getItemName() { return itemName; }
    public int getTotalQuantitySold() { return totalQuantitySold; }
    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public int getNumberOfTransactions() { return numberOfTransactions; }
    public BigDecimal getLowestPrice() { return lowestPrice; }
    public BigDecimal getHighestPrice() { return highestPrice; }
    public LocalDateTime getFirstSaleDate() { return firstSaleDate; }
    public LocalDateTime getLastSaleDate() { return lastSaleDate; }
    
    /**
     * Data point representing a single price observation
     */
    public static class PriceDataPoint {
        private final BigDecimal price;
        private final int quantity;
        private final LocalDateTime timestamp;
        
        public PriceDataPoint(BigDecimal price, int quantity, LocalDateTime timestamp) {
            this.price = price;
            this.quantity = quantity;
            this.timestamp = timestamp;
        }
        
        public BigDecimal getPrice() { return price; }
        public int getQuantity() { return quantity; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
    
    /**
     * Enum representing price trend directions
     */
    public enum PriceTrend {
        RISING("§a↗ Rising"),
        FALLING("§c↘ Falling"),
        STABLE("§e→ Stable");
        
        private final String displayName;
        
        PriceTrend(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() { return displayName; }
        
        @Override
        public String toString() { return displayName; }
    }
    
    /**
     * Enum representing market activity levels
     */
    public enum MarketActivity {
        INACTIVE("§7Inactive"),
        LOW("§elow"),
        MODERATE("§aModerate"),
        HIGH("§6High");
        
        private final String displayName;
        
        MarketActivity(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() { return displayName; }
        
        @Override
        public String toString() { return displayName; }
    }
}
