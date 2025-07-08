package com.zerog.neoessentials.economy.analytics;

import net.minecraft.world.item.ItemStack;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Analytics data for individual player shop performance
 */
public class PlayerShopAnalytics {
    
    private final UUID playerId;
    private final Map<String, ItemSalesData> itemSales = new ConcurrentHashMap<>();
    private final Map<String, ItemPurchaseData> itemPurchases = new ConcurrentHashMap<>();
    private final List<DailySalesData> dailySales = Collections.synchronizedList(new ArrayList<>());
    
    private BigDecimal totalRevenue = BigDecimal.ZERO;
    private BigDecimal totalSpent = BigDecimal.ZERO;
    private int totalItemsSold = 0;
    private int totalItemsPurchased = 0;
    private LocalDateTime firstSaleDate;
    private LocalDateTime lastActivityDate;
    
    public PlayerShopAnalytics(UUID playerId) {
        this.playerId = playerId;
        this.lastActivityDate = LocalDateTime.now();
    }
    
    /**
     * Records a sale made by this player
     */
    public void addSale(ItemStack item, int quantity, BigDecimal totalPrice) {
        try {
            String itemKey = getItemKey(item);
            
            // Update item-specific sales data
            ItemSalesData salesData = itemSales.computeIfAbsent(itemKey, k -> new ItemSalesData(item.copy()));
            salesData.addSale(quantity, totalPrice);
            
            // Update totals
            totalRevenue = totalRevenue.add(totalPrice);
            totalItemsSold += quantity;
            lastActivityDate = LocalDateTime.now();
            
            if (firstSaleDate == null) {
                firstSaleDate = LocalDateTime.now();
            }
            
            // Update daily sales tracking
            updateDailySales(totalPrice, quantity, true);
            
        } catch (Exception e) {
            // Log error but don't throw to avoid disrupting shop operations
            System.err.println("Error adding sale to player analytics: " + e.getMessage());
        }
    }
    
    /**
     * Records a purchase made by this player
     */
    public void addPurchase(ItemStack item, int quantity, BigDecimal totalPrice) {
        try {
            String itemKey = getItemKey(item);
            
            // Update item-specific purchase data
            ItemPurchaseData purchaseData = itemPurchases.computeIfAbsent(itemKey, k -> new ItemPurchaseData(item.copy()));
            purchaseData.addPurchase(quantity, totalPrice);
            
            // Update totals
            totalSpent = totalSpent.add(totalPrice);
            totalItemsPurchased += quantity;
            lastActivityDate = LocalDateTime.now();
            
            // Update daily sales tracking
            updateDailySales(totalPrice, quantity, false);
            
        } catch (Exception e) {
            System.err.println("Error adding purchase to player analytics: " + e.getMessage());
        }
    }
    
    /**
     * Gets the most profitable items sold by this player
     */
    public List<ItemSalesData> getTopSellingItems(int limit) {
        return itemSales.values().stream()
            .sorted((a, b) -> b.getTotalRevenue().compareTo(a.getTotalRevenue()))
            .limit(limit)
            .collect(ArrayList::new, (list, item) -> list.add(item), (list1, list2) -> list1.addAll(list2));
    }
    
    /**
     * Gets the most purchased items by this player
     */
    public List<ItemPurchaseData> getTopPurchasedItems(int limit) {
        return itemPurchases.values().stream()
            .sorted((a, b) -> Integer.compare(b.getTotalQuantity(), a.getTotalQuantity()))
            .limit(limit)
            .collect(ArrayList::new, (list, item) -> list.add(item), (list1, list2) -> list1.addAll(list2));
    }
    
    /**
     * Gets daily sales data for the last N days
     */
    public List<DailySalesData> getRecentDailySales(int days) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        synchronized (dailySales) {
            return dailySales.stream()
                .filter(data -> data.getDate().isAfter(cutoff))
                .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
                .collect(ArrayList::new, (list, item) -> list.add(item), (list1, list2) -> list1.addAll(list2));
        }
    }
    
    /**
     * Calculates average daily revenue over the last N days
     */
    public BigDecimal getAverageDailyRevenue(int days) {
        List<DailySalesData> recentSales = getRecentDailySales(days);
        if (recentSales.isEmpty()) return BigDecimal.ZERO;
        
        BigDecimal totalRevenue = recentSales.stream()
            .map(DailySalesData::getTotalRevenue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
            
        return totalRevenue.divide(BigDecimal.valueOf(Math.max(recentSales.size(), 1)), 2, BigDecimal.ROUND_HALF_UP);
    }
    
    /**
     * Gets profit margin (revenue - expenses)
     */
    public BigDecimal getProfitMargin() {
        return totalRevenue.subtract(totalSpent);
    }
    
    /**
     * Resets old data to prevent memory buildup
     */
    public void resetOldData() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(90); // Keep 90 days
        synchronized (dailySales) {
            dailySales.removeIf(data -> data.getDate().isBefore(cutoff));
        }
    }
    
    private void updateDailySales(BigDecimal amount, int quantity, boolean isSale) {
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        
        synchronized (dailySales) {
            DailySalesData todayData = dailySales.stream()
                .filter(data -> data.getDate().equals(today))
                .findFirst()
                .orElse(null);
                
            if (todayData == null) {
                todayData = new DailySalesData(today);
                dailySales.add(todayData);
            }
            
            if (isSale) {
                todayData.addSale(amount, quantity);
            } else {
                todayData.addPurchase(amount, quantity);
            }
        }
    }
    
    private String getItemKey(ItemStack item) {
        return item.getItem().toString() + "_" + item.getDamageValue();
    }
    
    // Getters
    public UUID getPlayerId() { return playerId; }
    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public BigDecimal getTotalSpent() { return totalSpent; }
    public int getTotalItemsSold() { return totalItemsSold; }
    public int getTotalItemsPurchased() { return totalItemsPurchased; }
    public LocalDateTime getFirstSaleDate() { return firstSaleDate; }
    public LocalDateTime getLastActivityDate() { return lastActivityDate; }
    public Map<String, ItemSalesData> getItemSales() { return new HashMap<>(itemSales); }
    public Map<String, ItemPurchaseData> getItemPurchases() { return new HashMap<>(itemPurchases); }
    
    /**
     * Data class for tracking sales of a specific item type
     */
    public static class ItemSalesData {
        private final ItemStack item;
        private int totalQuantitySold = 0;
        private BigDecimal totalRevenue = BigDecimal.ZERO;
        private int numberOfSales = 0;
        private LocalDateTime firstSaleDate;
        private LocalDateTime lastSaleDate;
        
        public ItemSalesData(ItemStack item) {
            this.item = item;
        }
        
        public void addSale(int quantity, BigDecimal revenue) {
            totalQuantitySold += quantity;
            totalRevenue = totalRevenue.add(revenue);
            numberOfSales++;
            lastSaleDate = LocalDateTime.now();
            
            if (firstSaleDate == null) {
                firstSaleDate = LocalDateTime.now();
            }
        }
        
        public BigDecimal getAverageUnitPrice() {
            if (totalQuantitySold == 0) return BigDecimal.ZERO;
            return totalRevenue.divide(BigDecimal.valueOf(totalQuantitySold), 2, BigDecimal.ROUND_HALF_UP);
        }
        
        // Getters
        public ItemStack getItem() { return item; }
        public int getTotalQuantitySold() { return totalQuantitySold; }
        public BigDecimal getTotalRevenue() { return totalRevenue; }
        public int getNumberOfSales() { return numberOfSales; }
        public LocalDateTime getFirstSaleDate() { return firstSaleDate; }
        public LocalDateTime getLastSaleDate() { return lastSaleDate; }
    }
    
    /**
     * Data class for tracking purchases of a specific item type
     */
    public static class ItemPurchaseData {
        private final ItemStack item;
        private int totalQuantity = 0;
        private BigDecimal totalSpent = BigDecimal.ZERO;
        private int numberOfPurchases = 0;
        
        public ItemPurchaseData(ItemStack item) {
            this.item = item;
        }
        
        public void addPurchase(int quantity, BigDecimal cost) {
            totalQuantity += quantity;
            totalSpent = totalSpent.add(cost);
            numberOfPurchases++;
        }
        
        public BigDecimal getAverageUnitPrice() {
            if (totalQuantity == 0) return BigDecimal.ZERO;
            return totalSpent.divide(BigDecimal.valueOf(totalQuantity), 2, BigDecimal.ROUND_HALF_UP);
        }
        
        // Getters
        public ItemStack getItem() { return item; }
        public int getTotalQuantity() { return totalQuantity; }
        public BigDecimal getTotalSpent() { return totalSpent; }
        public int getNumberOfPurchases() { return numberOfPurchases; }
    }
    
    /**
     * Data class for tracking daily sales performance
     */
    public static class DailySalesData {
        private final LocalDateTime date;
        private BigDecimal totalRevenue = BigDecimal.ZERO;
        private BigDecimal totalSpent = BigDecimal.ZERO;
        private int itemsSold = 0;
        private int itemsPurchased = 0;
        
        public DailySalesData(LocalDateTime date) {
            this.date = date;
        }
        
        public void addSale(BigDecimal revenue, int quantity) {
            totalRevenue = totalRevenue.add(revenue);
            itemsSold += quantity;
        }
        
        public void addPurchase(BigDecimal cost, int quantity) {
            totalSpent = totalSpent.add(cost);
            itemsPurchased += quantity;
        }
        
        public BigDecimal getNetProfit() {
            return totalRevenue.subtract(totalSpent);
        }
        
        // Getters
        public LocalDateTime getDate() { return date; }
        public BigDecimal getTotalRevenue() { return totalRevenue; }
        public BigDecimal getTotalSpent() { return totalSpent; }
        public int getItemsSold() { return itemsSold; }
        public int getItemsPurchased() { return itemsPurchased; }
    }
}
