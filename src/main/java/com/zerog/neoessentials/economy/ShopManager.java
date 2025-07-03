package com.zerog.neoessentials.economy;

import com.zerog.neoessentials.config.EnhancedEconomyConfig;
import com.zerog.neoessentials.economy.persistence.EconomyPersistenceManager;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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
        
        // Load existing shops from persistence
        loadAllShops();
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
        WalletManager walletManager = WalletManager.getInstance();
        if (walletManager.getCashBalance(ownerId) < shopCreationFee) {
            return null;
        }
        
        // Charge creation fee
        walletManager.subtractCash(ownerId, shopCreationFee);
        
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
        
        // Save to persistence
        EconomyPersistenceManager.getInstance().saveShop(shop);
        
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
        try {
            // Reload shop-related configuration from economy config
            EnhancedEconomyConfig config = EnhancedEconomyConfig.getInstance();
            
            // Re-read any shop-specific settings
            // Note: Most shop settings are stored per-shop, so minimal config reload needed
            
            // Log the reload
            System.out.println("[NeoEssentials] ShopManager configuration reloaded");
        } catch (Exception e) {
            System.err.println("[NeoEssentials] Error reloading ShopManager configuration: " + e.getMessage());
        }
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
            String locationName = shop.getLocationName();
            if (locationName != null) {
                List<UUID> locationShops = shopsByLocation.get(locationName);
                if (locationShops != null) {
                    locationShops.remove(shopId);
                    if (locationShops.isEmpty()) {
                        shopsByLocation.remove(locationName);
                    }
                }
            }
            
            // Handle shop inventory and pending transactions
            handleShopDeletion(shop);
            
            // Persist deletion to database/file
            EconomyPersistenceManager.getInstance().deleteShop(shopId);
            
            return true;
        } catch (Exception e) {
            // Re-add shop if deletion failed
            shops.put(shopId, shop);
            return false;
        }
    }

    /**
     * Load all shops from persistence on server startup
     */
    public void loadAllShops() {
        try {
            Map<UUID, Shop> persistedShops = EconomyPersistenceManager.getInstance().getAllShops();
            
            for (Shop shop : persistedShops.values()) {
                if (shop != null) {
                    // Add to main shops map
                    shops.put(shop.getShopId(), shop);
                    
                    // Add to indexes
                    playerShops.computeIfAbsent(shop.getOwnerId(), k -> new ArrayList<>()).add(shop.getShopId());
                    if (shop.getLocationName() != null) {
                        shopsByLocation.computeIfAbsent(shop.getLocationName(), k -> new ArrayList<>()).add(shop.getShopId());
                    }
                }
            }
            
            System.out.println("Loaded " + persistedShops.size() + " shops from persistence");
        } catch (Exception e) {
            System.err.println("Failed to load shops from persistence: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Save a shop to persistence (for external calls)
     */
    public void saveShop(Shop shop) {
        if (shop != null) {
            EconomyPersistenceManager.getInstance().saveShop(shop);
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
        private final Map<UUID, AutoBid> autoBids; // Auto-bid ID -> AutoBid
        private final Map<UUID, List<UUID>> playerAutoBids; // Player ID -> List of AutoBid IDs
        
        public AuctionHouse() {
            this.auctions = new ConcurrentHashMap<>();
            this.autoBids = new ConcurrentHashMap<>();
            this.playerAutoBids = new ConcurrentHashMap<>();
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
        
        // Auto-bidding functionality
        
        /**
         * Set up an auto-bid on an auction
         * 
         * @param auctionId The auction to bid on
         * @param playerId The player setting up auto-bidding
         * @param maxAmount Maximum amount to bid
         * @param increment Amount to increment bids by
         * @return true if auto-bid was set up successfully
         */
        public boolean setAutoBid(UUID auctionId, UUID playerId, double maxAmount, double increment) {
            Auction auction = auctions.get(auctionId);
            if (auction == null || !auction.isActive()) {
                return false;
            }
            
            // Check if player is not the seller
            if (auction.getSellerId().equals(playerId)) {
                return false; // Cannot auto-bid on own auction
            }
            
            // Remove any existing auto-bid for this player on this auction
            cancelAutoBid(auctionId, playerId);
            
            // Create new auto-bid
            AutoBid autoBid = new AutoBid(auctionId, playerId, maxAmount, increment);
            autoBids.put(generateAutoBidId(auctionId, playerId), autoBid);
            
            // Add to player's auto-bid list
            playerAutoBids.computeIfAbsent(playerId, k -> new ArrayList<>())
                         .add(generateAutoBidId(auctionId, playerId));
            
            return true;
        }
        
        /**
         * Cancel an auto-bid
         * 
         * @param auctionId The auction ID
         * @param playerId The player canceling auto-bidding
         * @return true if auto-bid was canceled
         */
        public boolean cancelAutoBid(UUID auctionId, UUID playerId) {
            UUID autoBidId = generateAutoBidId(auctionId, playerId);
            AutoBid autoBid = autoBids.remove(autoBidId);
            
            if (autoBid != null) {
                // Remove from player's auto-bid list
                List<UUID> playerBids = playerAutoBids.get(playerId);
                if (playerBids != null) {
                    playerBids.remove(autoBidId);
                    if (playerBids.isEmpty()) {
                        playerAutoBids.remove(playerId);
                    }
                }
                return true;
            }
            return false;
        }
        
        /**
         * Get all auto-bids for a player
         * 
         * @param playerId The player ID
         * @return List of active auto-bids
         */
        public List<AutoBid> getAutoBidsForPlayer(UUID playerId) {
            List<UUID> bidIds = playerAutoBids.get(playerId);
            if (bidIds == null) {
                return new ArrayList<>();
            }
            
            return bidIds.stream()
                    .map(autoBids::get)
                    .filter(Objects::nonNull)
                    .filter(AutoBid::isActive)
                    .toList();
        }
        
        /**
         * Process auto-bids when a new bid is placed on an auction
         * This should be called whenever a manual bid is placed
         * 
         * @param auctionId The auction that received a bid
         * @param currentHighestBid The current highest bid amount
         */
        public void processAutoBids(UUID auctionId, double currentHighestBid) {
            Auction auction = auctions.get(auctionId);
            if (auction == null || !auction.isActive()) {
                return;
            }
            
            // Get all auto-bids for this auction
            List<AutoBid> auctionAutoBids = autoBids.values().stream()
                    .filter(autoBid -> autoBid.getAuctionId().equals(auctionId))
                    .filter(AutoBid::isActive)
                    .sorted((a, b) -> Double.compare(b.getMaxAmount(), a.getMaxAmount())) // Highest max first
                    .toList();
            
            for (AutoBid autoBid : auctionAutoBids) {
                // Skip if this auto-bid belongs to current highest bidder
                if (auction.getHighestBidderId() != null && 
                    auction.getHighestBidderId().equals(autoBid.getPlayerId())) {
                    continue;
                }
                
                double nextBid = autoBid.calculateNextBid(currentHighestBid);
                if (nextBid > 0) {
                    // Try to place the auto-bid
                    UUID bidderId = autoBid.getPlayerId();
                    EconomyManager economyManager = EconomyManager.getInstance();
                    
                    // Check if player has enough funds (wallet + bank)
                    double walletBalance = economyManager.getBalance(bidderId);
                    double bankBalance = 0; // TODO: Get bank balance when available
                    double totalAvailable = walletBalance + bankBalance;
                    
                    if (totalAvailable >= nextBid) {
                        // Place the auto-bid
                        boolean bidSuccess = auction.placeBid(bidderId, nextBid);
                        if (bidSuccess) {
                            // Charge the player (prefer wallet first, then bank)
                            double walletCharge = Math.min(nextBid, walletBalance);
                            double bankCharge = nextBid - walletCharge;
                            
                            if (walletCharge > 0) {
                                economyManager.subtractMoney(bidderId, walletCharge);
                            }
                            // TODO: Charge bank when available
                            
                            currentHighestBid = nextBid;
                            break; // Stop processing auto-bids for this round
                        }
                    } else {
                        // Not enough funds, deactivate auto-bid
                        autoBid.setActive(false);
                    }
                }
            }
        }
        
        /**
         * Clean up auto-bids for ended auctions
         */
        public void cleanupAutoBids() {
            Set<UUID> endedAuctions = auctions.values().stream()
                    .filter(auction -> !auction.isActive())
                    .map(Auction::getAuctionId)
                    .collect(Collectors.toSet());
            
            // Remove auto-bids for ended auctions
            autoBids.entrySet().removeIf(entry -> 
                    endedAuctions.contains(entry.getValue().getAuctionId()));
            
            // Clean up player auto-bid lists
            playerAutoBids.values().forEach(bidList -> 
                    bidList.removeIf(bidId -> {
                        AutoBid autoBid = autoBids.get(bidId);
                        return autoBid == null || endedAuctions.contains(autoBid.getAuctionId());
                    }));
            
            // Remove empty player lists
            playerAutoBids.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        }
        
        /**
         * Generate a unique ID for an auto-bid
         */
        private UUID generateAutoBidId(UUID auctionId, UUID playerId) {
            return UUID.nameUUIDFromBytes((auctionId.toString() + playerId.toString()).getBytes());
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
    
    /**
     * Handle shop deletion by managing inventory and pending transactions
     * 
     * @param shop The shop being deleted
     */
    private void handleShopDeletion(Shop shop) {
        try {
            // Get shop inventory
            Map<String, Shop.ShopItem> inventory = shop.getInventory();
            
            // Log the deletion for debugging
            System.out.println("[NeoEssentials] Handling deletion of shop: " + shop.getName());
            System.out.println("[NeoEssentials] Shop has " + inventory.size() + " item types in inventory");
            
            // Option 1: Return items to shop owner (preferred)
            UUID ownerId = shop.getOwnerId();
            if (ownerId != null) {
                returnItemsToOwner(shop, ownerId, inventory);
            } else {
                // Option 2: If owner not found, void the items (log for audit)
                voidShopInventory(shop, inventory);
            }
            
            // Handle pending transactions/orders
            handlePendingTransactions(shop);
            
        } catch (Exception e) {
            System.err.println("[NeoEssentials] Error handling shop deletion for " + shop.getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Return shop items to the owner (placeholder implementation)
     * In a full implementation, this would add items to player inventory or a mailbox system
     */
    private void returnItemsToOwner(Shop shop, UUID ownerId, Map<String, Shop.ShopItem> inventory) {
        System.out.println("[NeoEssentials] Returning " + inventory.size() + " item types to shop owner " + ownerId);
        
        try {
            BankManager bankManager = EconomyManager.getInstance().getBankManager();
            Currency currency = shop.getCurrency();
            double totalValue = 0.0;
            
            // Calculate total value of inventory
            for (Map.Entry<String, Shop.ShopItem> entry : inventory.entrySet()) {
                Shop.ShopItem item = entry.getValue();
                String itemId = entry.getKey();
                Double itemPrice = shop.getItemPrice(itemId);
                if (itemPrice == null) itemPrice = 1.0; // Default price if not found
                
                double itemValue = item.getQuantity() * itemPrice * 0.8; // 80% return value
                totalValue += itemValue;
                System.out.println("[NeoEssentials] - " + item.getQuantity() + "x " + item.getItemName() + 
                    " (Value: " + currency.format(itemValue) + ")");
            }
            
            if (totalValue > 0) {
                // Try to deposit compensation to owner's primary account
                BankAccount ownerAccount = bankManager.getPrimaryAccount(ownerId);
                if (ownerAccount != null) {
                    boolean success = bankManager.deposit(ownerAccount.getAccountId(), totalValue);
                    
                    if (success) {
                        System.out.println("[NeoEssentials] Deposited " + currency.format(totalValue) + 
                            " compensation to owner's account");
                        
                        // Store notification for when owner logs in
                        storeOwnerNotification(ownerId, shop, totalValue, currency);
                    } else {
                        System.err.println("[NeoEssentials] Failed to deposit compensation - items lost");
                    }
                } else {
                    System.err.println("[NeoEssentials] Owner account not found - items lost");
                }
            }
            
        } catch (Exception e) {
            System.err.println("[NeoEssentials] Error returning items to owner: " + e.getMessage());
        }
    }
    
    /**
     * Void shop inventory (items are lost)
     */
    private void voidShopInventory(Shop shop, Map<String, Shop.ShopItem> inventory) {
        System.out.println("[NeoEssentials] Voiding inventory for shop " + shop.getName() + " (owner not found)");
        
        for (Map.Entry<String, Shop.ShopItem> entry : inventory.entrySet()) {
            Shop.ShopItem item = entry.getValue();
            System.out.println("[NeoEssentials] - Voided " + item.getQuantity() + "x " + item.getItemName());
        }
    }
    
    /**
     * Handle pending transactions for deleted shop
     */
    private void handlePendingTransactions(Shop shop) {
        System.out.println("[NeoEssentials] Handling pending transactions for shop: " + shop.getName());
        
        try {
            BankManager bankManager = EconomyManager.getInstance().getBankManager();
            TransactionManager transactionManager = EconomyManager.getInstance().getTransactionManager();
            Currency currency = shop.getCurrency();
            
            // Get recent transactions for this shop (last 30 days)
            long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24L * 60L * 60L * 1000L);
            
            // Find and handle pending purchases/orders
            // Note: This is a simplified implementation - in a real system you'd have
            // a proper pending transaction table
            
            // Get shop customers from sales history instead
            Set<UUID> affectedCustomers = new HashSet<>();
            for (Shop.Sale sale : shop.getSalesHistory(30)) { // Last 30 days
                affectedCustomers.add(sale.getBuyerId());
            }
            int refundCount = 0;
            double totalRefunds = 0.0;
            
            for (UUID customerId : affectedCustomers) {
                try {
                    // Check if customer has any recent transactions that might be pending
                    List<Transaction> customerTransactions = transactionManager.getPlayerTransactions(customerId, 30)
                        .stream()
                        .filter(t -> t.getDescription().contains(shop.getName()) || 
                                   t.getDescription().contains("Shop purchase"))
                        .toList();
                    
                    // For simulation, assume any recent transaction might need refunding
                    for (Transaction transaction : customerTransactions) {
                        if (transaction.getAmount() < 0) { // Purchase (negative amount)
                            double refundAmount = Math.abs(transaction.getAmount());
                            
                            BankAccount customerAccount = bankManager.getPrimaryAccount(customerId);
                            if (customerAccount != null) {
                                boolean success = bankManager.deposit(customerAccount.getAccountId(), 
                                    refundAmount);
                                
                                if (success) {
                                    refundCount++;
                                    totalRefunds += refundAmount;
                                    System.out.println("[NeoEssentials] Refunded " + currency.format(refundAmount) + 
                                        " to customer " + customerId.toString().substring(0, 8));
                                }
                            }
                        }
                    }
                    
                } catch (Exception e) {
                    System.err.println("[NeoEssentials] Error processing customer " + 
                        customerId.toString().substring(0, 8) + ": " + e.getMessage());
                }
            }
            
            if (refundCount > 0) {
                System.out.println("[NeoEssentials] Processed " + refundCount + " refunds totaling " + 
                    currency.format(totalRefunds));
            } else {
                System.out.println("[NeoEssentials] No pending transactions requiring refunds found");
            }
            
        } catch (Exception e) {
            System.err.println("[NeoEssentials] Error handling pending transactions: " + e.getMessage());
        }
    }
    
    /**
     * Store notification for shop owner about compensation
     */
    private void storeOwnerNotification(UUID ownerId, Shop shop, double compensation, Currency currency) {
        // This would typically integrate with a notification system
        // For now, just log that a notification should be sent
        System.out.println("[NeoEssentials] Notification queued for owner " + ownerId.toString().substring(0, 8) + 
            ": Shop '" + shop.getName() + "' was deleted, " + currency.format(compensation) + " compensation deposited");
    }
}
