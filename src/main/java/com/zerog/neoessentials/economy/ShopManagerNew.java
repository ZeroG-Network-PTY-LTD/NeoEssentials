package com.zerog.neoessentials.economy;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages all shops in the NeoEssentials economy system.
 * Redesigned for better functionality and maintainability.
 */
public class ShopManagerNew {
    private static ShopManagerNew instance;
    private final Map<UUID, ShopNew> shops; // Shop ID -> Shop
    private final Map<UUID, List<UUID>> playerShops; // Player ID -> List of Shop IDs
    private final Map<String, List<UUID>> shopsByCategory; // Category -> List of Shop IDs
    private final Map<String, List<UUID>> shopsByLocation; // Location -> List of Shop IDs
    
    // Shop settings
    private final int maxShopsPerPlayer;
    private final double shopCreationFee;
    private final double shopMaintenanceFee; // Per day
    private final double globalTaxRate;
    
    // Categories
    private final Set<String> validCategories;
    
    private ShopManagerNew() {
        this.shops = new ConcurrentHashMap<>();
        this.playerShops = new ConcurrentHashMap<>();
        this.shopsByCategory = new ConcurrentHashMap<>();
        this.shopsByLocation = new ConcurrentHashMap<>();
        
        this.maxShopsPerPlayer = 5;
        this.shopCreationFee = 1000.0;
        this.shopMaintenanceFee = 100.0;
        this.globalTaxRate = 0.05; // 5% tax on all sales
        
        this.validCategories = new HashSet<>();
        initializeCategories();
    }
    
    public static ShopManagerNew getInstance() {
        if (instance == null) {
            instance = new ShopManagerNew();
        }
        return instance;
    }
    
    /**
     * Initialize shop categories
     */
    private void initializeCategories() {
        validCategories.add("general");
        validCategories.add("armor");
        validCategories.add("weapons");
        validCategories.add("tools");
        validCategories.add("blocks");
        validCategories.add("food");
        validCategories.add("potions");
        validCategories.add("enchanted");
        validCategories.add("rare");
        validCategories.add("building");
        validCategories.add("decoration");
        validCategories.add("redstone");
        validCategories.add("transportation");
        validCategories.add("farming");
        validCategories.add("mining");
    }
    
    /**
     * Create a new shop
     */
    public ShopCreationResult createShop(UUID ownerId, String shopName, String category, ShopNew.ShopType shopType) {
        // Validate inputs
        if (shopName == null || shopName.trim().isEmpty()) {
            return new ShopCreationResult(false, "Shop name cannot be empty", null);
        }
        
        if (!validCategories.contains(category.toLowerCase())) {
            return new ShopCreationResult(false, "Invalid category. Available: " + String.join(", ", validCategories), null);
        }
        
        // Check player shop limit
        List<UUID> playerShopIds = playerShops.getOrDefault(ownerId, new ArrayList<>());
        if (playerShopIds.size() >= maxShopsPerPlayer) {
            return new ShopCreationResult(false, "Maximum shops per player reached (" + maxShopsPerPlayer + ")", null);
        }
        
        // Check and charge creation fee
        WalletManager walletManager = WalletManager.getInstance();
        if (!walletManager.hasEnoughMoney(ownerId, shopCreationFee)) {
            return new ShopCreationResult(false, "Insufficient funds for shop creation fee: " + 
                CurrencyManager.getInstance().getDefaultCurrency().format(shopCreationFee), null);
        }
        
        // Create the shop
        ShopNew shop = new ShopNew(ownerId, shopName, "Default Location", category, shopType);
        
        // Charge creation fee
        walletManager.subtractMoney(ownerId, shopCreationFee);
        
        // Register the shop
        shops.put(shop.getShopId(), shop);
        playerShops.computeIfAbsent(ownerId, k -> new ArrayList<>()).add(shop.getShopId());
        shopsByCategory.computeIfAbsent(category.toLowerCase(), k -> new ArrayList<>()).add(shop.getShopId());
        shopsByLocation.computeIfAbsent("default", k -> new ArrayList<>()).add(shop.getShopId());
        
        return new ShopCreationResult(true, "Shop created successfully", shop);
    }
    
    /**
     * Delete a shop
     */
    public boolean deleteShop(UUID shopId, UUID requesterId) {
        ShopNew shop = shops.get(shopId);
        if (shop == null) {
            return false;
        }
        
        // Check permissions
        if (!shop.getOwnerId().equals(requesterId)) {
            return false; // Only owner can delete
        }
        
        // Remove from all indexes
        shops.remove(shopId);
        playerShops.get(shop.getOwnerId()).remove(shopId);
        shopsByCategory.get(shop.getCategory()).remove(shopId);
        shopsByLocation.values().forEach(list -> list.remove(shopId));
        
        return true;
    }
    
    /**
     * Get shop by ID
     */
    public ShopNew getShop(UUID shopId) {
        return shops.get(shopId);
    }
    
    /**
     * Get shop by name
     */
    public ShopNew getShopByName(String shopName) {
        return shops.values().stream()
                .filter(shop -> shop.getShopName().equalsIgnoreCase(shopName))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Get all shops owned by a player
     */
    public List<ShopNew> getPlayerShops(UUID playerId) {
        return playerShops.getOrDefault(playerId, new ArrayList<>())
                .stream()
                .map(shops::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    
    /**
     * Get all shops in a category
     */
    public List<ShopNew> getShopsByCategory(String category) {
        return shopsByCategory.getOrDefault(category.toLowerCase(), new ArrayList<>())
                .stream()
                .map(shops::get)
                .filter(Objects::nonNull)
                .filter(ShopNew::isActive)
                .collect(Collectors.toList());
    }
    
    /**
     * Get all active shops
     */
    public List<ShopNew> getAllActiveShops() {
        return shops.values().stream()
                .filter(ShopNew::isActive)
                .collect(Collectors.toList());
    }
    
    /**
     * Search shops by name
     */
    public List<ShopNew> searchShops(String searchTerm) {
        return shops.values().stream()
                .filter(ShopNew::isActive)
                .filter(shop -> shop.getShopName().toLowerCase().contains(searchTerm.toLowerCase()))
                .collect(Collectors.toList());
    }
    
    /**
     * Search items across all shops
     */
    public List<ShopItemSearchResult> searchItems(String itemName) {
        List<ShopItemSearchResult> results = new ArrayList<>();
        
        for (ShopNew shop : getAllActiveShops()) {
            List<ShopNew.ShopItem> items = shop.searchItems(itemName);
            for (ShopNew.ShopItem item : items) {
                results.add(new ShopItemSearchResult(shop, item));
            }
        }
        
        return results;
    }
    
    /**
     * Get statistics for all shops
     */
    public ShopStatistics getGlobalStatistics() {
        int totalShops = shops.size();
        int activeShops = (int) shops.values().stream().filter(ShopNew::isActive).count();
        double totalRevenue = shops.values().stream().mapToDouble(ShopNew::getTotalRevenue).sum();
        int totalSales = shops.values().stream().mapToInt(ShopNew::getTotalSales).sum();
        
        return new ShopStatistics(totalShops, activeShops, totalRevenue, totalSales);
    }
    
    /**
     * Get valid categories
     */
    public Set<String> getValidCategories() {
        return new HashSet<>(validCategories);
    }
    
    /**
     * Check if a player can afford shop creation
     */
    public boolean canAffordShopCreation(UUID playerId) {
        return WalletManager.getInstance().hasEnoughMoney(playerId, shopCreationFee);
    }
    
    /**
     * Get shop creation fee
     */
    public double getShopCreationFee() {
        return shopCreationFee;
    }
    
    /**
     * Get max shops per player
     */
    public int getMaxShopsPerPlayer() {
        return maxShopsPerPlayer;
    }
    
    /**
     * Process daily maintenance for all shops
     */
    public void processDailyMaintenance() {
        for (ShopNew shop : getAllActiveShops()) {
            if (shop.getShopType() == ShopNew.ShopType.PLAYER) {
                // Charge maintenance fee
                WalletManager walletManager = WalletManager.getInstance();
                if (walletManager.hasEnoughMoney(shop.getOwnerId(), shopMaintenanceFee)) {
                    walletManager.subtractMoney(shop.getOwnerId(), shopMaintenanceFee);
                } else {
                    // Mark shop as inactive if can't pay maintenance
                    shop.setActive(false);
                }
            }
        }
    }
    
    /**
     * Shop creation result
     */
    public static class ShopCreationResult {
        private final boolean success;
        private final String message;
        private final ShopNew shop;
        
        public ShopCreationResult(boolean success, String message, ShopNew shop) {
            this.success = success;
            this.message = message;
            this.shop = shop;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public ShopNew getShop() { return shop; }
    }
    
    /**
     * Shop item search result
     */
    public static class ShopItemSearchResult {
        private final ShopNew shop;
        private final ShopNew.ShopItem item;
        
        public ShopItemSearchResult(ShopNew shop, ShopNew.ShopItem item) {
            this.shop = shop;
            this.item = item;
        }
        
        public ShopNew getShop() { return shop; }
        public ShopNew.ShopItem getItem() { return item; }
    }
    
    /**
     * Shop statistics
     */
    public static class ShopStatistics {
        private final int totalShops;
        private final int activeShops;
        private final double totalRevenue;
        private final int totalSales;
        
        public ShopStatistics(int totalShops, int activeShops, double totalRevenue, int totalSales) {
            this.totalShops = totalShops;
            this.activeShops = activeShops;
            this.totalRevenue = totalRevenue;
            this.totalSales = totalSales;
        }
        
        public int getTotalShops() { return totalShops; }
        public int getActiveShops() { return activeShops; }
        public double getTotalRevenue() { return totalRevenue; }
        public int getTotalSales() { return totalSales; }
    }
}
