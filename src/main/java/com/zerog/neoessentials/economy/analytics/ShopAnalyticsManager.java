package com.zerog.neoessentials.economy.analytics;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.economy.shop.ShopItem;
import com.zerog.neoessentials.economy.shop.ShopManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Advanced analytics system for player shops providing detailed insights and statistics
 */
public class ShopAnalyticsManager {
    
    private final Map<UUID, PlayerShopAnalytics> playerAnalytics = new ConcurrentHashMap<>();
    private final Map<String, ItemMarketData> itemMarketData = new ConcurrentHashMap<>();
    private final List<SaleTransaction> recentTransactions = Collections.synchronizedList(new ArrayList<>());
    
    private static final int MAX_RECENT_TRANSACTIONS = 1000;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Records a shop transaction for analytics
     */
    public void recordTransaction(UUID buyerId, UUID sellerId, ShopItem item, int quantity, BigDecimal totalPrice) {
        try {
            SaleTransaction transaction = new SaleTransaction(
                buyerId, sellerId, item.getItemStack().copy(), quantity, totalPrice, LocalDateTime.now()
            );
            
            // Add to recent transactions (with limit)
            synchronized (recentTransactions) {
                recentTransactions.add(transaction);
                if (recentTransactions.size() > MAX_RECENT_TRANSACTIONS) {
                    recentTransactions.remove(0);
                }
            }
            
            // Update player analytics
            updatePlayerAnalytics(sellerId, item, quantity, totalPrice, true);
            updatePlayerAnalytics(buyerId, item, quantity, totalPrice, false);
            
            // Update item market data
            updateItemMarketData(item.getItemStack(), quantity, totalPrice);
            
            NeoEssentials.LOGGER.debug("Recorded shop transaction: {} bought {}x {} from {} for {}",
                buyerId, quantity, item.getItemStack().getHoverName().getString(), sellerId, totalPrice);
                
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to record shop transaction", e);
        }
    }
    
    /**
     * Gets comprehensive analytics for a player's shop performance
     */
    public PlayerShopAnalytics getPlayerAnalytics(UUID playerId) {
        return playerAnalytics.computeIfAbsent(playerId, k -> new PlayerShopAnalytics(playerId));
    }
    
    /**
     * Gets market data for a specific item type
     */
    public ItemMarketData getItemMarketData(ItemStack item) {
        String itemKey = getItemKey(item);
        return itemMarketData.computeIfAbsent(itemKey, k -> new ItemMarketData(item.copy()));
    }
    
    /**
     * Gets top selling items across all shops
     */
    public List<ItemMarketData> getTopSellingItems(int limit) {
        return itemMarketData.values().stream()
            .sorted((a, b) -> Integer.compare(b.getTotalQuantitySold(), a.getTotalQuantitySold()))
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    /**
     * Gets top earning players from shop sales
     */
    public List<PlayerShopAnalytics> getTopEarningPlayers(int limit) {
        return playerAnalytics.values().stream()
            .sorted((a, b) -> b.getTotalRevenue().compareTo(a.getTotalRevenue()))
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    /**
     * Gets recent transactions for analysis
     */
    public List<SaleTransaction> getRecentTransactions(int limit) {
        synchronized (recentTransactions) {
            return recentTransactions.stream()
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .limit(limit)
                .collect(Collectors.toList());
        }
    }
    
    /**
     * Generates a market report for server administrators
     */
    public MarketReport generateMarketReport() {
        try {
            MarketReport report = new MarketReport();
            
            // Basic statistics
            report.totalActiveShops = playerAnalytics.size();
            report.totalTransactions = recentTransactions.size();
            report.totalItemTypes = itemMarketData.size();
            
            // Revenue statistics
            BigDecimal totalRevenue = playerAnalytics.values().stream()
                .map(PlayerShopAnalytics::getTotalRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            report.totalMarketRevenue = totalRevenue;
            
            // Average prices
            Map<String, BigDecimal> avgPrices = new HashMap<>();
            for (ItemMarketData data : itemMarketData.values()) {
                if (data.getTotalQuantitySold() > 0) {
                    BigDecimal avgPrice = data.getTotalRevenue()
                        .divide(BigDecimal.valueOf(data.getTotalQuantitySold()), 2, BigDecimal.ROUND_HALF_UP);
                    avgPrices.put(data.getItemName(), avgPrice);
                }
            }
            report.averageItemPrices = avgPrices;
            
            // Top performers
            report.topSellingItems = getTopSellingItems(10);
            report.topEarningPlayers = getTopEarningPlayers(10);
            
            report.generatedAt = LocalDateTime.now();
            
            return report;
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to generate market report", e);
            return new MarketReport(); // Return empty report on error
        }
    }
    
    private void updatePlayerAnalytics(UUID playerId, ShopItem item, int quantity, BigDecimal totalPrice, boolean isSeller) {
        PlayerShopAnalytics analytics = getPlayerAnalytics(playerId);
        
        if (isSeller) {
            analytics.addSale(item.getItemStack().copy(), quantity, totalPrice);
        } else {
            analytics.addPurchase(item.getItemStack().copy(), quantity, totalPrice);
        }
    }
    
    private void updateItemMarketData(ItemStack item, int quantity, BigDecimal totalPrice) {
        String itemKey = getItemKey(item);
        ItemMarketData data = getItemMarketData(item);
        data.addSale(quantity, totalPrice);
    }
    
    private String getItemKey(ItemStack item) {
        return item.getItem().toString() + "_" + item.getDamageValue();
    }
    
    /**
     * Clears old analytics data to prevent memory issues
     */
    public void cleanupOldData(int daysToKeep) {
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
            
            // Clean up recent transactions
            synchronized (recentTransactions) {
                recentTransactions.removeIf(transaction -> transaction.getTimestamp().isBefore(cutoffDate));
            }
            
            // Reset player analytics periodically (could be enhanced to only reset old data)
            if (daysToKeep <= 30) {
                playerAnalytics.values().forEach(PlayerShopAnalytics::resetOldData);
            }
            
            NeoEssentials.LOGGER.info("Cleaned up shop analytics data older than {} days", daysToKeep);
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to cleanup old analytics data", e);
        }
    }
    
    /**
     * Data class representing a single sale transaction
     */
    public static class SaleTransaction {
        private final UUID buyerId;
        private final UUID sellerId;
        private final ItemStack item;
        private final int quantity;
        private final BigDecimal totalPrice;
        private final LocalDateTime timestamp;
        
        public SaleTransaction(UUID buyerId, UUID sellerId, ItemStack item, int quantity, BigDecimal totalPrice, LocalDateTime timestamp) {
            this.buyerId = buyerId;
            this.sellerId = sellerId;
            this.item = item;
            this.quantity = quantity;
            this.totalPrice = totalPrice;
            this.timestamp = timestamp;
        }
        
        // Getters
        public UUID getBuyerId() { return buyerId; }
        public UUID getSellerId() { return sellerId; }
        public ItemStack getItem() { return item; }
        public int getQuantity() { return quantity; }
        public BigDecimal getTotalPrice() { return totalPrice; }
        public LocalDateTime getTimestamp() { return timestamp; }
        
        public BigDecimal getUnitPrice() {
            return totalPrice.divide(BigDecimal.valueOf(quantity), 2, BigDecimal.ROUND_HALF_UP);
        }
    }
    
    /**
     * Comprehensive market report for administrators
     */
    public static class MarketReport {
        public int totalActiveShops = 0;
        public int totalTransactions = 0;
        public int totalItemTypes = 0;
        public BigDecimal totalMarketRevenue = BigDecimal.ZERO;
        public Map<String, BigDecimal> averageItemPrices = new HashMap<>();
        public List<ItemMarketData> topSellingItems = new ArrayList<>();
        public List<PlayerShopAnalytics> topEarningPlayers = new ArrayList<>();
        public LocalDateTime generatedAt = LocalDateTime.now();
        
        public String toFormattedString() {
            StringBuilder sb = new StringBuilder();
            sb.append("§6=== Market Report ===\n");
            sb.append("§7Generated: §f").append(generatedAt.format(DATE_FORMATTER)).append("\n\n");
            sb.append("§e📊 Basic Statistics:\n");
            sb.append("§7Active Shops: §f").append(totalActiveShops).append("\n");
            sb.append("§7Total Transactions: §f").append(totalTransactions).append("\n");
            sb.append("§7Item Types Traded: §f").append(totalItemTypes).append("\n");
            sb.append("§7Total Market Revenue: §6").append(totalMarketRevenue).append(" coins\n\n");
            
            sb.append("§e🏆 Top Selling Items:\n");
            for (int i = 0; i < Math.min(5, topSellingItems.size()); i++) {
                ItemMarketData item = topSellingItems.get(i);
                sb.append("§7").append(i + 1).append(". §f").append(item.getItemName())
                  .append(" §7(§e").append(item.getTotalQuantitySold()).append(" sold§7)\n");
            }
            
            return sb.toString();
        }
    }
}
