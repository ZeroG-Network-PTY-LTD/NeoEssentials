package com.zerog.neoessentials.economy;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all shops in the NeoEssentials economy system.
 * Supports player-owned shops, server shops, auctions, and dynamic pricing.
 */
public class ShopManager {
    private static ShopManager instance;
    private final Map<UUID, Shop> shops; // Shop ID -> Shop
    private final Map<UUID, List<UUID>> playerShops; // Player ID -> List of Shop IDs
    private final Map<String, List<UUID>> shopsByLocation; // Location -> List of Shop IDs
    private final AuctionHouse auctionHouse;
    private final DynamicPricingEngine pricingEngine;
    
    // Shop settings
    private final int maxShopsPerPlayer;
    private final double shopCreationFee;
    private final double shopRentalFee; // Per day
    private final double shopTaxRate; // Percentage of sales
    
    private ShopManager() {
        this.shops = new ConcurrentHashMap<>();
        this.playerShops = new ConcurrentHashMap<>();
        this.shopsByLocation = new ConcurrentHashMap<>();
        this.auctionHouse = new AuctionHouse();
        this.pricingEngine = new DynamicPricingEngine();
        this.maxShopsPerPlayer = 5;
        this.shopCreationFee = 500.0;
        this.shopRentalFee = 50.0;
        this.shopTaxRate = 0.05; // 5% tax on sales
    }
    
    public static ShopManager getInstance() {
        if (instance == null) {
            instance = new ShopManager();
        }
        return instance;
    }
    
    /**
     * Create a new shop
     * 
     * @param ownerId The shop owner's UUID
     * @param shopName The shop name
     * @param location The shop location
     * @param shopType The type of shop
     * @return The created shop, or null if creation failed
     */
    public Shop createShop(UUID ownerId, String shopName, String location, Shop.ShopType shopType) {
        // Check if player can create more shops
        List<UUID> playerShopIds = playerShops.getOrDefault(ownerId, new ArrayList<>());
        if (playerShopIds.size() >= maxShopsPerPlayer) {
            return null;
        }
        
        // Check if player has enough money for creation fee
        EconomyManager economyManager = EconomyManager.getInstance();
        Currency defaultCurrency = CurrencyManager.getInstance().getDefaultCurrency();
        if (!economyManager.removeBalance(ownerId, defaultCurrency, shopCreationFee, "Shop creation fee")) {
            return null;
        }
        
        // Create the shop
        Shop shop = new Shop(ownerId, shopName, location, "general", shopType);
        shops.put(shop.getShopId(), shop);
        
        // Add to indexes
        playerShops.computeIfAbsent(ownerId, k -> new ArrayList<>()).add(shop.getShopId());
        shopsByLocation.computeIfAbsent(location, k -> new ArrayList<>()).add(shop.getShopId());
        
        return shop;
    }
    
    /**
     * Create a new shop (simplified signature for tests)
     * 
     * @param ownerId The shop owner's UUID
     * @param shopName The shop name
     * @param shopType The type of shop
     * @return The created shop, or null if creation failed
     */
    public Shop createShop(UUID ownerId, String shopName, Shop.ShopType shopType) {
        // Use default location and category for test compatibility
        return createShop(ownerId, shopName, "Default Location", "general", shopType);
    }

    /**
     * Create a new shop with category
     * 
     * @param ownerId The shop owner's UUID
     * @param shopName The shop name
     * @param location The shop location
     * @param category The shop category
     * @param shopType The type of shop
     * @return The created shop, or null if creation failed
     */
    public Shop createShop(UUID ownerId, String shopName, String location, String category, Shop.ShopType shopType) {
        // Check if player can create more shops
        List<UUID> playerShopIds = playerShops.getOrDefault(ownerId, new ArrayList<>());
        if (playerShopIds.size() >= maxShopsPerPlayer) {
            return null;
        }
        
        // Check if player has enough money for creation fee
        EconomyManager economyManager = EconomyManager.getInstance();
        Currency defaultCurrency = CurrencyManager.getInstance().getDefaultCurrency();
        if (!economyManager.removeBalance(ownerId, defaultCurrency, shopCreationFee, "Shop creation fee")) {
            return null;
        }
        
        // Create the shop
        Shop shop = new Shop(ownerId, shopName, location, category, shopType);
        shops.put(shop.getShopId(), shop);
        
        // Add to indexes
        playerShops.computeIfAbsent(ownerId, k -> new ArrayList<>()).add(shop.getShopId());
        shopsByLocation.computeIfAbsent(location, k -> new ArrayList<>()).add(shop.getShopId());
        
        return shop;
    }

    /**
     * Create a new shop without deducting creation fee (payment already handled)
     * 
     * @param ownerId The shop owner's UUID
     * @param shopName The shop name
     * @param location The shop location
     * @param category The shop category
     * @param shopType The type of shop
     * @param skipPayment If true, skip the payment deduction (payment already handled externally)
     * @return The created shop, or null if creation failed
     */
    public Shop createShop(UUID ownerId, String shopName, String location, String category, Shop.ShopType shopType, boolean skipPayment) {
        // Check if player can create more shops
        List<UUID> playerShopIds = playerShops.getOrDefault(ownerId, new ArrayList<>());
        if (playerShopIds.size() >= maxShopsPerPlayer) {
            return null;
        }
        
        // Only check/deduct payment if not skipping
        if (!skipPayment) {
            EconomyManager economyManager = EconomyManager.getInstance();
            Currency defaultCurrency = CurrencyManager.getInstance().getDefaultCurrency();
            if (!economyManager.removeBalance(ownerId, defaultCurrency, shopCreationFee, "Shop creation fee")) {
                return null;
            }
        }
        
        // Create the shop
        Shop shop = new Shop(ownerId, shopName, location, category, shopType);
        shops.put(shop.getShopId(), shop);
        
        // Add to indexes
        playerShops.computeIfAbsent(ownerId, k -> new ArrayList<>()).add(shop.getShopId());
        shopsByLocation.computeIfAbsent(location, k -> new ArrayList<>()).add(shop.getShopId());
        
        return shop;
    }

    /**
     * Get a shop by ID
     * 
     * @param shopId The shop ID
     * @return The shop, or null if not found
     */
    public Shop getShop(UUID shopId) {
        return shops.get(shopId);
    }
    
    /**
     * Get a shop by its name
     * 
     * @param shopName The name of the shop
     * @return The shop if found, null otherwise
     */
    public Shop getShopByName(String shopName) {
        return shops.values().stream()
                .filter(shop -> shop.getShopName().equalsIgnoreCase(shopName))
                .findFirst()
                .orElse(null);
    }

    /**
     * Get all shops owned by a player
     * 
     * @param playerId The player's UUID
     * @return List of shops owned by the player
     */
    public List<Shop> getPlayerShops(UUID playerId) {
        List<UUID> shopIds = playerShops.getOrDefault(playerId, new ArrayList<>());
        return shopIds.stream()
                .map(shops::get)
                .filter(Objects::nonNull)
                .toList();
    }
    
    /**
     * Get all shops at a location
     * 
     * @param location The location
     * @return List of shops at the location
     */
    public List<Shop> getShopsAtLocation(String location) {
        List<UUID> shopIds = shopsByLocation.getOrDefault(location, new ArrayList<>());
        return shopIds.stream()
                .map(shops::get)
                .filter(Objects::nonNull)
                .filter(Shop::isActive)
                .toList();
    }
    
    /**
     * Search for shops by name, category, or item
     * 
     * @param searchTerm The search term
     * @param limit Maximum number of results
     * @return List of matching shops
     */
    public List<Shop> searchShops(String searchTerm, int limit) {
        String lowerSearch = searchTerm.toLowerCase();
        
        return shops.values().stream()
                .filter(Shop::isActive)
                .filter(shop -> shop.getShopName().toLowerCase().contains(lowerSearch) ||
                               shop.getCategory().toLowerCase().contains(lowerSearch) ||
                               shop.hasItemForSale(searchTerm))
                .limit(limit)
                .toList();
    }
    
    /**
     * Get shops by category
     * 
     * @param category The category to search for
     * @return List of shops in the specified category
     */
    public List<Shop> getShopsByCategory(String category) {
        String lowerCategory = category.toLowerCase();
        
        return shops.values().stream()
                .filter(Shop::isActive)
                .filter(shop -> shop.getCategory().toLowerCase().equals(lowerCategory))
                .toList();
    }

    /**
     * Process a shop purchase
     * 
     * @param shopId The shop ID
     * @param buyerId The buyer's UUID
     * @param itemId The item being purchased
     * @param quantity The quantity to purchase
     * @return true if purchase was successful
     */
    public boolean processPurchase(UUID shopId, UUID buyerId, String itemId, int quantity) {
        Shop shop = getShop(shopId);
        if (shop == null || !shop.isActive()) {
            return false;
        }
        
        // Check if item is available
        if (!shop.hasItemInStock(itemId, quantity)) {
            return false;
        }
        
        double totalPrice = shop.getItemPrice(itemId) * quantity;
        Currency currency = shop.getCurrency();
        
        // Check if buyer has enough money
        EconomyManager economyManager = EconomyManager.getInstance();
        if (economyManager.getBalance(buyerId, currency) < totalPrice) {
            return false;
        }
        
        // Calculate tax
        double tax = totalPrice * shopTaxRate;
        double sellerReceives = totalPrice - tax;
        
        // Process the transaction
        if (!economyManager.removeBalance(buyerId, currency, totalPrice, "Shop purchase: " + itemId)) {
            return false;
        }
        
        if (!economyManager.addBalance(shop.getOwnerId(), currency, sellerReceives, "Shop sale: " + itemId)) {
            // Refund buyer if seller payment fails
            economyManager.addBalance(buyerId, currency, totalPrice, "Refund: Shop purchase failed");
            return false;
        }
        
        // Remove item from shop inventory
        shop.removeItem(itemId, quantity);
        
        // Record the sale
        shop.recordSale(buyerId, itemId, quantity, totalPrice, System.currentTimeMillis());
        
        // Update dynamic pricing
        pricingEngine.recordSale(itemId, totalPrice, quantity);
        
        return true;
    }
    
    /**
     * Calculate daily rental fees for all shops
     */
    public void processShopRentals() {
        Currency defaultCurrency = CurrencyManager.getInstance().getDefaultCurrency();
        EconomyManager economyManager = EconomyManager.getInstance();
        
        for (Shop shop : shops.values()) {
            if (shop.isActive() && shop.getShopType() == Shop.ShopType.PLAYER_RENTAL) {
                UUID ownerId = shop.getOwnerId();
                
                if (!economyManager.removeBalance(ownerId, defaultCurrency, shopRentalFee, "Shop rental fee")) {
                    // Close shop if owner can't pay rent
                    shop.setActive(false);
                }
            }
        }
    }
    
    /**
     * Get shop performance statistics
     * 
     * @param shopId The shop ID
     * @param days Number of days to analyze
     * @return Map of performance metrics
     */
    public Map<String, Object> getShopPerformance(UUID shopId, int days) {
        Shop shop = getShop(shopId);
        if (shop == null) {
            return new HashMap<>();
        }
        
        return shop.getPerformanceMetrics(days);
    }
    
    /**
     * Get top performing shops
     * 
     * @param metric The metric to sort by ("sales", "revenue", "customers")
     * @param days Number of days to analyze
     * @param limit Number of shops to return
     * @return List of top shops
     */
    public List<Shop> getTopShops(String metric, int days, int limit) {
        return shops.values().stream()
                .filter(Shop::isActive)
                .sorted((s1, s2) -> {
                    Map<String, Object> m1 = s1.getPerformanceMetrics(days);
                    Map<String, Object> m2 = s2.getPerformanceMetrics(days);
                    
                    Double v1 = (Double) m1.getOrDefault(metric, 0.0);
                    Double v2 = (Double) m2.getOrDefault(metric, 0.0);
                    
                    return Double.compare(v2, v1); // Descending order
                })
                .limit(limit)
                .toList();
    }
    
    /**
     * Reload configuration for shop manager
     */
    public void reloadConfiguration() {
        // TODO: Implement configuration reload when needed
        // This is a placeholder to fix compilation errors
    }
    
    /**
     * Get total shops count for statistics
     */
    public int getTotalShopsCount() {
        return shops.size();
    }
    
    /**
     * Get all active shops
     * 
     * @return List of all active shops
     */
    public List<Shop> getAllShops() {
        return shops.values().stream()
                .filter(Shop::isActive)
                .toList();
    }
    
    /**
     * Delete a shop
     * 
     * @param shopId The ID of the shop to delete
     * @return true if the shop was successfully deleted, false otherwise
     */
    public boolean deleteShop(UUID shopId) {
        Shop shop = shops.get(shopId);
        if (shop == null) {
            return false;
        }
        
        try {
            // Remove from main shops map
            shops.remove(shopId);
            
            // Remove from player shops mapping
            if (shop.getOwnerId() != null) {
                List<UUID> playerShopList = playerShops.get(shop.getOwnerId());
                if (playerShopList != null) {
                    playerShopList.remove(shopId);
                    if (playerShopList.isEmpty()) {
                        playerShops.remove(shop.getOwnerId());
                    }
                }
            }
            
            // Remove from location mapping
            String location = shop.getLocation();
            if (location != null) {
                List<UUID> locationShops = shopsByLocation.get(location);
                if (locationShops != null) {
                    locationShops.remove(shopId);
                    if (locationShops.isEmpty()) {
                        shopsByLocation.remove(location);
                    }
                }
            }
            
            // TODO: Handle shop inventory - could return items to owner or void them
            // TODO: Handle pending transactions/orders
            // TODO: Persist deletion to database/file
            
            return true;
        } catch (Exception e) {
            // Re-add shop if deletion failed
            shops.put(shopId, shop);
            return false;
        }
    }

    // Getters for shop settings
    public int getMaxShopsPerPlayer() { return maxShopsPerPlayer; }
    public double getShopCreationFee() { return shopCreationFee; }
    public double getShopRentalFee() { return shopRentalFee; }
    public double getShopTaxRate() { return shopTaxRate; }
    
    // Manager getters
    public AuctionHouse getAuctionHouse() { return auctionHouse; }
    public DynamicPricingEngine getPricingEngine() { return pricingEngine; }
    
    /**
     * Inner class for auction house functionality
     */
    public static class AuctionHouse {
        private final Map<UUID, Auction> auctions;
        
        public AuctionHouse() {
            this.auctions = new ConcurrentHashMap<>();
        }
        
        public Auction createAuction(UUID sellerId, String itemId, String itemName, int quantity, 
                                   double startingBid, long duration) {
            Auction auction = new Auction(sellerId, itemId, itemName, quantity, startingBid, duration);
            auctions.put(auction.getAuctionId(), auction);
            return auction;
        }
        
        public List<Auction> getActiveAuctions() {
            return auctions.values().stream()
                    .filter(Auction::isActive)
                    .toList();
        }
        
        /**
         * Get an auction by ID
         * 
         * @param auctionId The auction ID
         * @return The auction, or null if not found
         */
        public Auction getAuctionById(UUID auctionId) {
            return auctions.get(auctionId);
        }
        
        /**
         * Get auctions by seller
         * 
         * @param sellerId The seller's UUID
         * @return List of auctions by the seller
         */
        public List<Auction> getAuctionsBySeller(UUID sellerId) {
            return auctions.values().stream()
                    .filter(auction -> auction.getSellerId().equals(sellerId))
                    .toList();
        }
        
        /**
         * Search auctions by item name
         * 
         * @param itemName The item name to search for
         * @return List of matching auctions
         */
        public List<Auction> searchAuctions(String itemName) {
            return auctions.values().stream()
                    .filter(auction -> auction.isActive() && 
                            auction.getItemName().toLowerCase().contains(itemName.toLowerCase()))
                    .toList();
        }
        
        /**
         * Remove expired auctions
         * 
         * @return Number of auctions removed
         */
        public int cleanupExpiredAuctions() {
            List<UUID> expiredIds = auctions.values().stream()
                    .filter(auction -> !auction.isActive() && auction.getEndTime() < System.currentTimeMillis())
                    .map(Auction::getAuctionId)
                    .toList();
            
            expiredIds.forEach(auctions::remove);
            return expiredIds.size();
        }
        
        /**
         * Get count of active auctions for statistics
         */
        public int getActiveAuctionsCount() {
            return (int) auctions.values().stream()
                    .filter(Auction::isActive)
                    .count();
        }
        
        /**
         * Get all auctions (for admin purposes)
         */
        public Collection<Auction> getAllAuctions() {
            return auctions.values();
        }
    }
    
    /**
     * Inner class for dynamic pricing
     */
    public static class DynamicPricingEngine {
        private final Map<String, ItemPriceData> itemPrices;
        
        public DynamicPricingEngine() {
            this.itemPrices = new ConcurrentHashMap<>();
        }
        
        public void recordSale(String itemId, double price, int quantity) {
            itemPrices.computeIfAbsent(itemId, k -> new ItemPriceData())
                     .recordSale(price, quantity);
        }
        
        public double getSuggestedPrice(String itemId) {
            ItemPriceData data = itemPrices.get(itemId);
            return data != null ? data.getAveragePrice() : 0.0;
        }
        
        public boolean hasRecentSales(String itemId, long maxAgeMillis) {
            ItemPriceData data = itemPrices.get(itemId);
            if (data == null) return false;
            
            long timeSinceLastSale = System.currentTimeMillis() - data.getLastSaleTime();
            return timeSinceLastSale <= maxAgeMillis;
        }
        
        private static class ItemPriceData {
            private double totalValue;
            private int totalQuantity;
            private long lastSale;
            
            public void recordSale(double price, int quantity) {
                totalValue += price;
                totalQuantity += quantity;
                lastSale = System.currentTimeMillis();
            }
            
            public double getAveragePrice() {
                return totalQuantity > 0 ? totalValue / totalQuantity : 0.0;
            }
            
            public long getLastSaleTime() {
                return lastSale;
            }
        }
    }
}
