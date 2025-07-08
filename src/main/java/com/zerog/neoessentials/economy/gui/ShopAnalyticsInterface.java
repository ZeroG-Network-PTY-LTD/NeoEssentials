package com.zerog.neoessentials.economy.gui;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.economy.EconomyManager;
import com.zerog.neoessentials.economy.analytics.ItemMarketData;
import com.zerog.neoessentials.economy.analytics.PlayerShopAnalytics;
import com.zerog.neoessentials.economy.analytics.ShopAnalyticsManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Advanced analytics interface for shop statistics and market insights
 */
public class ShopAnalyticsInterface {
    
    private static final int CONTAINER_SIZE = 54; // 6x9 slots
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    
    public enum AnalyticsView {
        PLAYER_OVERVIEW,
        MARKET_TRENDS,
        TOP_PERFORMERS,
        ITEM_DETAILS
    }
    
    /**
     * Opens the analytics interface for a player
     */
    public static void openAnalytics(ServerPlayer player, EconomyManager economyManager) {
        openAnalytics(player, economyManager, AnalyticsView.PLAYER_OVERVIEW, null);
    }
    
    /**
     * Opens a specific analytics view
     */
    public static void openAnalytics(ServerPlayer player, EconomyManager economyManager, 
                                   AnalyticsView view, Object data) {
        try {
            SimpleContainer container = new SimpleContainer(CONTAINER_SIZE);
            setupAnalyticsInterface(container, player, economyManager, view, data);
            
            String title = switch (view) {
                case PLAYER_OVERVIEW -> "§6Your Shop Analytics";
                case MARKET_TRENDS -> "§bMarket Trends";
                case TOP_PERFORMERS -> "§aTop Performers";
                case ITEM_DETAILS -> "§eItem Market Details";
            };
            
            SimpleMenuProvider menuProvider = new SimpleMenuProvider(
                (containerId, inventory, menuPlayer) -> {
                    return new ShopAnalyticsMenu(containerId, inventory, container, 
                                               player, economyManager, view, data);
                },
                Component.literal(title)
            );
            
            player.openMenu(menuProvider);
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to open analytics interface", e);
            player.sendSystemMessage(Component.literal("§cFailed to open analytics interface"));
        }
    }
    
    /**
     * Sets up the analytics interface based on the view type
     */
    private static void setupAnalyticsInterface(SimpleContainer container, ServerPlayer player, 
                                              EconomyManager economyManager, AnalyticsView view, Object data) {
        container.clearContent();
        
        ShopAnalyticsManager analytics = economyManager.getShopManager().getAnalytics();
        
        switch (view) {
            case PLAYER_OVERVIEW -> setupPlayerOverview(container, player, analytics);
            case MARKET_TRENDS -> setupMarketTrends(container, analytics);
            case TOP_PERFORMERS -> setupTopPerformers(container, analytics);
            case ITEM_DETAILS -> setupItemDetails(container, data, analytics);
        }
        
        // Add navigation buttons
        setupNavigationButtons(container, view);
        
        // Fill empty slots
        fillEmptySlots(container);
    }
    
    /**
     * Sets up the player overview analytics view
     */
    private static void setupPlayerOverview(SimpleContainer container, ServerPlayer player, 
                                          ShopAnalyticsManager analytics) {
        PlayerShopAnalytics playerStats = analytics.getPlayerAnalytics(player.getUUID());
        
        // Revenue summary
        ItemStack revenueItem = new ItemStack(Items.GOLD_INGOT);
        revenueItem.set(DataComponents.CUSTOM_NAME, Component.literal("§6Total Revenue"));
        String revenueLore = String.format("§7Total Earned: §6%s coins\n§7Total Spent: §c%s coins\n§7Net Profit: %s%s coins\n§7Items Sold: §e%d\n§7Items Purchased: §b%d",
            playerStats.getTotalRevenue(),
            playerStats.getTotalSpent(),
            playerStats.getProfitMargin().compareTo(BigDecimal.ZERO) >= 0 ? "§a+" : "§c",
            playerStats.getProfitMargin(),
            playerStats.getTotalItemsSold(),
            playerStats.getTotalItemsPurchased());
        // Note: In a real implementation, you'd add this as lore using DataComponents
        container.setItem(4, revenueItem);
        
        // Top selling items
        List<PlayerShopAnalytics.ItemSalesData> topItems = playerStats.getTopSellingItems(7);
        for (int i = 0; i < Math.min(topItems.size(), 7); i++) {
            PlayerShopAnalytics.ItemSalesData itemData = topItems.get(i);
            ItemStack displayItem = itemData.getItem().copy();
            displayItem.set(DataComponents.CUSTOM_NAME, 
                Component.literal("§e" + itemData.getItem().getHoverName().getString()));
            String itemLore = String.format("§7Sold: §e%d items\n§7Revenue: §6%s coins\n§7Avg Price: §a%s coins\n§7Sales: §b%d transactions",
                itemData.getTotalQuantitySold(),
                itemData.getTotalRevenue(),
                itemData.getAverageUnitPrice(),
                itemData.getNumberOfSales());
            container.setItem(10 + i, displayItem);
        }
        
        // Daily performance (last 7 days)
        List<PlayerShopAnalytics.DailySalesData> dailyData = playerStats.getRecentDailySales(7);
        for (int i = 0; i < Math.min(dailyData.size(), 7); i++) {
            PlayerShopAnalytics.DailySalesData dayData = dailyData.get(i);
            ItemStack dayItem = new ItemStack(Items.CLOCK);
            dayItem.set(DataComponents.CUSTOM_NAME, 
                Component.literal("§b" + dayData.getDate().format(DATE_FORMATTER)));
            String dayLore = String.format("§7Revenue: §6%s coins\n§7Spent: §c%s coins\n§7Net: %s%s coins\n§7Items Sold: §e%d",
                dayData.getTotalRevenue(),
                dayData.getTotalSpent(),
                dayData.getNetProfit().compareTo(BigDecimal.ZERO) >= 0 ? "§a+" : "§c",
                dayData.getNetProfit(),
                dayData.getItemsSold());
            container.setItem(28 + i, dayItem);
        }
        
        // Performance metrics
        ItemStack metricsItem = new ItemStack(Items.PAPER);
        metricsItem.set(DataComponents.CUSTOM_NAME, Component.literal("§aPerformance Metrics"));
        BigDecimal avgDaily = playerStats.getAverageDailyRevenue(30);
        String metricsLore = String.format("§7Avg Daily Revenue: §6%s coins\n§7First Sale: §b%s\n§7Last Activity: §e%s\n§7Total Days Active: §a%d",
            avgDaily,
            playerStats.getFirstSaleDate() != null ? playerStats.getFirstSaleDate().format(DATE_FORMATTER) : "Never",
            playerStats.getLastActivityDate().format(DATE_FORMATTER),
            playerStats.getFirstSaleDate() != null ? 
                java.time.temporal.ChronoUnit.DAYS.between(playerStats.getFirstSaleDate(), java.time.LocalDateTime.now()) : 0);
        container.setItem(22, metricsItem);
    }
    
    /**
     * Sets up the market trends view
     */
    private static void setupMarketTrends(SimpleContainer container, ShopAnalyticsManager analytics) {
        ShopAnalyticsManager.MarketReport report = analytics.generateMarketReport();
        
        // Market overview
        ItemStack overviewItem = new ItemStack(Items.FILLED_MAP);
        overviewItem.set(DataComponents.CUSTOM_NAME, Component.literal("§6Market Overview"));
        String overviewLore = String.format("§7Active Shops: §e%d\n§7Total Transactions: §b%d\n§7Item Types: §a%d\n§7Total Revenue: §6%s coins",
            report.totalActiveShops,
            report.totalTransactions,
            report.totalItemTypes,
            report.totalMarketRevenue);
        container.setItem(4, overviewItem);
        
        // Top selling items in market
        List<ItemMarketData> topItems = report.topSellingItems;
        for (int i = 0; i < Math.min(topItems.size(), 14); i++) {
            ItemMarketData itemData = topItems.get(i);
            ItemStack displayItem = itemData.getItem().copy();
            displayItem.set(DataComponents.CUSTOM_NAME, 
                Component.literal("§e" + itemData.getItemName()));
            
            String trend = itemData.getPriceTrend().getDisplayName();
            String activity = itemData.getMarketActivity().getDisplayName();
            
            String itemLore = String.format("§7Total Sold: §e%d\n§7Avg Price: §6%s coins\n§7Price Range: §a%s§7-§c%s\n§7Trend: %s\n§7Activity: %s",
                itemData.getTotalQuantitySold(),
                itemData.getAveragePrice(),
                itemData.getLowestPrice(),
                itemData.getHighestPrice(),
                trend,
                activity);
            
            int row = i < 7 ? 0 : 1;
            int col = i % 7;
            container.setItem(10 + row * 9 + col, displayItem);
        }
    }
    
    /**
     * Sets up the top performers view
     */
    private static void setupTopPerformers(SimpleContainer container, ShopAnalyticsManager analytics) {
        List<PlayerShopAnalytics> topPlayers = analytics.getTopEarningPlayers(21);
        
        // Top earning players
        for (int i = 0; i < Math.min(topPlayers.size(), 21); i++) {
            PlayerShopAnalytics playerData = topPlayers.get(i);
            ItemStack playerItem = new ItemStack(Items.PLAYER_HEAD);
            playerItem.set(DataComponents.CUSTOM_NAME, 
                Component.literal("§e#" + (i + 1) + " Top Earner"));
            
            String playerLore = String.format("§7Revenue: §6%s coins\n§7Items Sold: §e%d\n§7Profit Margin: %s%s coins\n§7Avg Daily: §a%s coins",
                playerData.getTotalRevenue(),
                playerData.getTotalItemsSold(),
                playerData.getProfitMargin().compareTo(BigDecimal.ZERO) >= 0 ? "§a+" : "§c",
                playerData.getProfitMargin(),
                playerData.getAverageDailyRevenue(30));
            
            int row = i / 7;
            int col = i % 7;
            container.setItem(10 + row * 9 + col, playerItem);
        }
    }
    
    /**
     * Sets up item details view for a specific item
     */
    private static void setupItemDetails(SimpleContainer container, Object data, ShopAnalyticsManager analytics) {
        if (!(data instanceof ItemStack item)) return;
        
        ItemMarketData itemData = analytics.getItemMarketData(item);
        
        // Main item display
        ItemStack displayItem = itemData.getItem().copy();
        displayItem.set(DataComponents.CUSTOM_NAME, 
            Component.literal("§6" + itemData.getItemName() + " Market Data"));
        container.setItem(4, displayItem);
        
        // Market statistics
        ItemStack statsItem = new ItemStack(Items.BOOK);
        statsItem.set(DataComponents.CUSTOM_NAME, Component.literal("§bMarket Statistics"));
        String statsLore = String.format("§7Total Sold: §e%d\n§7Total Revenue: §6%s coins\n§7Transactions: §b%d\n§7Avg Price: §a%s coins\n§7Price Range: §a%s§7-§c%s",
            itemData.getTotalQuantitySold(),
            itemData.getTotalRevenue(),
            itemData.getNumberOfTransactions(),
            itemData.getAveragePrice(),
            itemData.getLowestPrice(),
            itemData.getHighestPrice());
        container.setItem(20, statsItem);
        
        // Price trend
        ItemStack trendItem = new ItemStack(Items.COMPASS);
        trendItem.set(DataComponents.CUSTOM_NAME, Component.literal("§ePrice Trend"));
        String trendLore = String.format("§7Current Trend: %s\n§7Market Activity: %s\n§7Volatility: §6%s\n§7First Sale: §b%s\n§7Last Sale: §e%s",
            itemData.getPriceTrend().getDisplayName(),
            itemData.getMarketActivity().getDisplayName(),
            itemData.getPriceVolatility(),
            itemData.getFirstSaleDate() != null ? itemData.getFirstSaleDate().format(DATE_FORMATTER) : "Never",
            itemData.getLastSaleDate() != null ? itemData.getLastSaleDate().format(DATE_FORMATTER) : "Never");
        container.setItem(24, trendItem);
        
        // Recent price history
        List<ItemMarketData.PriceDataPoint> priceHistory = itemData.getRecentPriceHistory(14);
        for (int i = 0; i < Math.min(priceHistory.size(), 14); i++) {
            ItemMarketData.PriceDataPoint point = priceHistory.get(i);
            ItemStack priceItem = new ItemStack(Items.GOLD_NUGGET);
            priceItem.set(DataComponents.CUSTOM_NAME, 
                Component.literal("§6" + point.getPrice() + " coins"));
            String priceLore = String.format("§7Quantity: §e%d\n§7Date: §b%s",
                point.getQuantity(),
                point.getTimestamp().format(DATE_FORMATTER));
            
            int row = i < 7 ? 0 : 1;
            int col = i % 7;
            container.setItem(28 + row * 9 + col, priceItem);
        }
    }
    
    /**
     * Sets up navigation buttons
     */
    private static void setupNavigationButtons(SimpleContainer container, AnalyticsView currentView) {
        // View selection buttons
        if (currentView != AnalyticsView.PLAYER_OVERVIEW) {
            ItemStack playerView = new ItemStack(Items.PLAYER_HEAD);
            playerView.set(DataComponents.CUSTOM_NAME, Component.literal("§bYour Analytics"));
            container.setItem(45, playerView);
        }
        
        if (currentView != AnalyticsView.MARKET_TRENDS) {
            ItemStack marketView = new ItemStack(Items.FILLED_MAP);
            marketView.set(DataComponents.CUSTOM_NAME, Component.literal("§aMarket Trends"));
            container.setItem(46, marketView);
        }
        
        if (currentView != AnalyticsView.TOP_PERFORMERS) {
            ItemStack topView = new ItemStack(Items.GOLD_BLOCK);
            topView.set(DataComponents.CUSTOM_NAME, Component.literal("§6Top Performers"));
            container.setItem(47, topView);
        }
        
        // Refresh button
        ItemStack refresh = new ItemStack(Items.LIME_DYE);
        refresh.set(DataComponents.CUSTOM_NAME, Component.literal("§aRefresh Data"));
        container.setItem(52, refresh);
        
        // Close button
        ItemStack close = new ItemStack(Items.BARRIER);
        close.set(DataComponents.CUSTOM_NAME, Component.literal("§cClose"));
        container.setItem(53, close);
    }
    
    /**
     * Fills empty slots with glass panes
     */
    private static void fillEmptySlots(SimpleContainer container) {
        ItemStack glassPane = new ItemStack(Items.LIGHT_GRAY_STAINED_GLASS_PANE);
        glassPane.set(DataComponents.CUSTOM_NAME, Component.literal(""));
        
        for (int i = 0; i < CONTAINER_SIZE; i++) {
            if (container.getItem(i).isEmpty()) {
                container.setItem(i, glassPane.copy());
            }
        }
    }
    
    /**
     * Custom menu for analytics interface
     */
    public static class ShopAnalyticsMenu extends ChestMenu {
        private final ServerPlayer player;
        private final EconomyManager economyManager;
        private final AnalyticsView currentView;
        private final Object viewData;
        private final Container analyticsContainer;
        
        public ShopAnalyticsMenu(int containerId, Inventory playerInventory, Container container,
                               ServerPlayer player, EconomyManager economyManager, 
                               AnalyticsView view, Object data) {
            super(MenuType.GENERIC_9x6, containerId, playerInventory, container, 6);
            this.player = player;
            this.economyManager = economyManager;
            this.currentView = view;
            this.viewData = data;
            this.analyticsContainer = container;
        }
        
        @Override
        public boolean stillValid(@Nonnull net.minecraft.world.entity.player.Player menuPlayer) {
            return menuPlayer == player && menuPlayer.isAlive();
        }
        
        @Override
        public void clicked(int slotIndex, int dragType, @Nonnull ClickType clickType, 
                           @Nonnull net.minecraft.world.entity.player.Player menuPlayer) {
            if (menuPlayer != player) return;
            
            if (slotIndex < 54) {
                handleAnalyticsClick(slotIndex, clickType);
                return;
            }
            
            super.clicked(slotIndex, dragType, clickType, menuPlayer);
        }
        
        private void handleAnalyticsClick(int slotIndex, ClickType clickType) {
            ItemStack clickedItem = this.getSlot(slotIndex).getItem();
            if (clickedItem.isEmpty()) return;
            
            String itemName = clickedItem.getHoverName().getString();
            
            // Handle navigation clicks
            switch (slotIndex) {
                case 45: // Player Analytics
                    if (itemName.contains("Your Analytics")) {
                        player.closeContainer();
                        player.getServer().execute(() -> 
                            openAnalytics(player, economyManager, AnalyticsView.PLAYER_OVERVIEW, null));
                    }
                    break;
                case 46: // Market Trends
                    if (itemName.contains("Market Trends")) {
                        player.closeContainer();
                        player.getServer().execute(() -> 
                            openAnalytics(player, economyManager, AnalyticsView.MARKET_TRENDS, null));
                    }
                    break;
                case 47: // Top Performers
                    if (itemName.contains("Top Performers")) {
                        player.closeContainer();
                        player.getServer().execute(() -> 
                            openAnalytics(player, economyManager, AnalyticsView.TOP_PERFORMERS, null));
                    }
                    break;
                case 52: // Refresh
                    if (itemName.contains("Refresh")) {
                        player.closeContainer();
                        player.getServer().execute(() -> 
                            openAnalytics(player, economyManager, currentView, viewData));
                    }
                    break;
                case 53: // Close
                    if (itemName.contains("Close")) {
                        player.closeContainer();
                    }
                    break;
                default:
                    // Handle item detail views for market trends
                    if (currentView == AnalyticsView.MARKET_TRENDS && 
                        slotIndex >= 10 && slotIndex <= 25 && !itemName.contains("Market Overview")) {
                        player.closeContainer();
                        player.getServer().execute(() -> 
                            openAnalytics(player, economyManager, AnalyticsView.ITEM_DETAILS, clickedItem));
                    }
                    break;
            }
        }
        
        @Override
        @Nonnull
        public ItemStack quickMoveStack(@Nonnull net.minecraft.world.entity.player.Player menuPlayer, int index) {
            // Prevent shift-clicking items out of analytics container
            return ItemStack.EMPTY;
        }
    }
}
