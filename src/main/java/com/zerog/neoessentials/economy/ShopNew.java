package com.zerog.neoessentials.economy;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

/**
 * Represents a shop in the NeoEssentials economy system.
 * Redesigned for better functionality and maintainability.
 */
public class ShopNew {
    private final UUID shopId;
    private final UUID ownerId;
    private String shopName;
    private ShopLocation shopLocation;
    private String locationName;
    private String category;
    private final ShopType shopType;
    private final long createdTime;
    private boolean isActive;
    private final Currency currency;
    
    // Shop inventory and pricing
    private final Map<String, ShopItem> inventory; // Item ID -> Shop Item
    private final Map<String, ItemPricing> itemPricing; // Item ID -> Pricing info
    
    // Shop statistics
    private final List<Sale> salesHistory;
    private double totalRevenue;
    private int totalSales;
    private final Set<UUID> customers;
    
    // Shop settings
    private String description;
    private boolean allowsHaggling;
    private double discountRate;
    private int bulkThreshold;
    private double taxRate;
    
    // Employee management (simplified)
    private final Set<UUID> employees;
    private final Map<UUID, ShopPermission> employeePermissions;
    
    public enum ShopType {
        PLAYER("Player Shop", true, true, -1),
        SERVER("Server Shop", false, false, -1),
        AUCTION("Auction House", true, false, 7);
        
        private final String displayName;
        private final boolean playerManaged;
        private final boolean allowsPriceChanges;
        private final int maxDuration; // -1 means unlimited
        
        ShopType(String displayName, boolean playerManaged, boolean allowsPriceChanges, int maxDuration) {
            this.displayName = displayName;
            this.playerManaged = playerManaged;
            this.allowsPriceChanges = allowsPriceChanges;
            this.maxDuration = maxDuration;
        }
        
        public String getDisplayName() { return displayName; }
        public boolean isPlayerManaged() { return playerManaged; }
        public boolean allowsPriceChanges() { return allowsPriceChanges; }
        public int getMaxDuration() { return maxDuration; }
    }
    
    public enum ShopPermission {
        STOCK_ITEMS,
        SET_PRICES,
        MANAGE_EMPLOYEES,
        VIEW_SALES,
        WITHDRAW_FUNDS
    }
    
    /**
     * Create a new shop
     */
    public ShopNew(UUID ownerId, String shopName, String locationName, String category, ShopType shopType) {
        this.shopId = UUID.randomUUID();
        this.ownerId = ownerId;
        this.shopName = shopName;
        this.shopLocation = null;
        this.locationName = locationName;
        this.category = category;
        this.shopType = shopType;
        this.createdTime = System.currentTimeMillis();
        this.isActive = true;
        this.currency = CurrencyManager.getInstance().getDefaultCurrency();
        
        this.inventory = new ConcurrentHashMap<>();
        this.itemPricing = new ConcurrentHashMap<>();
        this.salesHistory = new ArrayList<>();
        this.totalRevenue = 0.0;
        this.totalSales = 0;
        this.customers = new HashSet<>();
        
        this.description = "";
        this.allowsHaggling = false;
        this.discountRate = 0.1; // 10% bulk discount
        this.bulkThreshold = 10;
        this.taxRate = 0.05; // 5% tax
        
        this.employees = new HashSet<>();
        this.employeePermissions = new ConcurrentHashMap<>();
    }
    
    /**
     * Add or update an item in the shop
     */
    public boolean addItem(String itemId, int quantity, double buyPrice, double sellPrice) {
        if (!ItemHandler.isValidItem(itemId)) {
            return false;
        }
        
        ShopItem item = inventory.computeIfAbsent(itemId, id -> new ShopItem(id, 0));
        item.addStock(quantity);
        
        ItemPricing pricing = new ItemPricing(buyPrice, sellPrice);
        itemPricing.put(itemId, pricing);
        
        return true;
    }
    
    /**
     * Remove an item from the shop
     */
    public boolean removeItem(String itemId) {
        inventory.remove(itemId);
        itemPricing.remove(itemId);
        return true;
    }
    
    /**
     * Update item pricing
     */
    public boolean updateItemPricing(String itemId, double buyPrice, double sellPrice) {
        if (!inventory.containsKey(itemId)) {
            return false;
        }
        
        ItemPricing pricing = new ItemPricing(buyPrice, sellPrice);
        itemPricing.put(itemId, pricing);
        return true;
    }
    
    /**
     * Check if item is available for purchase
     */
    public boolean isItemAvailable(String itemId, int quantity) {
        ShopItem item = inventory.get(itemId);
        return item != null && item.getQuantity() >= quantity;
    }
    
    /**
     * Get item buy price
     */
    public double getItemBuyPrice(String itemId) {
        ItemPricing pricing = itemPricing.get(itemId);
        return pricing != null ? pricing.getBuyPrice() : 0.0;
    }
    
    /**
     * Get item sell price
     */
    public double getItemSellPrice(String itemId) {
        ItemPricing pricing = itemPricing.get(itemId);
        return pricing != null ? pricing.getSellPrice() : 0.0;
    }
    
    /**
     * Purchase items from the shop
     */
    public PurchaseResult purchaseItem(UUID buyerId, String itemId, int quantity) {
        if (!isItemAvailable(itemId, quantity)) {
            return new PurchaseResult(false, "Item not available in requested quantity");
        }
        
        double totalCost = calculateTotalCost(itemId, quantity);
        
        // Check if buyer has sufficient funds
        WalletManager walletManager = WalletManager.getInstance();
        if (!walletManager.hasEnoughMoney(buyerId, totalCost)) {
            return new PurchaseResult(false, "Insufficient funds");
        }
        
        // Process the purchase
        walletManager.subtractMoney(buyerId, totalCost);
        
        // Update inventory
        ShopItem item = inventory.get(itemId);
        item.removeStock(quantity);
        
        // Record the sale
        Sale sale = new Sale(buyerId, itemId, quantity, totalCost, System.currentTimeMillis());
        salesHistory.add(sale);
        totalRevenue += totalCost;
        totalSales++;
        customers.add(buyerId);
        
        // Pay the shop owner (minus tax)
        double ownerPayment = totalCost * (1.0 - taxRate);
        walletManager.addMoney(ownerId, ownerPayment);
        
        return new PurchaseResult(true, "Purchase successful");
    }
    
    /**
     * Sell items to the shop
     */
    public SaleResult sellItem(UUID sellerId, String itemId, int quantity) {
        ItemPricing pricing = itemPricing.get(itemId);
        if (pricing == null || pricing.getSellPrice() <= 0) {
            return new SaleResult(false, "Shop does not buy this item");
        }
        
        double totalPayment = pricing.getSellPrice() * quantity;
        
        // Check if shop owner has enough funds
        WalletManager walletManager = WalletManager.getInstance();
        if (!walletManager.hasEnoughMoney(ownerId, totalPayment)) {
            return new SaleResult(false, "Shop owner has insufficient funds");
        }
        
        // Process the sale
        walletManager.subtractMoney(ownerId, totalPayment);
        walletManager.addMoney(sellerId, totalPayment);
        
        // Update inventory
        ShopItem item = inventory.computeIfAbsent(itemId, id -> new ShopItem(id, 0));
        item.addStock(quantity);
        
        return new SaleResult(true, "Sale successful");
    }
    
    /**
     * Calculate total cost including discounts
     */
    private double calculateTotalCost(String itemId, int quantity) {
        double basePrice = getItemBuyPrice(itemId);
        double totalCost = basePrice * quantity;
        
        // Apply bulk discount if applicable
        if (quantity >= bulkThreshold) {
            totalCost *= (1.0 - discountRate);
        }
        
        return totalCost;
    }
    
    /**
     * Get all available items
     */
    public List<ShopItem> getAvailableItems() {
        return inventory.values().stream()
                .filter(item -> item.getQuantity() > 0)
                .collect(ArrayList::new, (list, item) -> list.add(item), List::addAll);
    }
    
    /**
     * Search items by name
     */
    public List<ShopItem> searchItems(String searchTerm) {
        return inventory.values().stream()
                .filter(item -> item.getItemName().toLowerCase().contains(searchTerm.toLowerCase()))
                .collect(ArrayList::new, (list, item) -> list.add(item), List::addAll);
    }
    
    // Getters and setters
    public UUID getShopId() { return shopId; }
    public UUID getOwnerId() { return ownerId; }
    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }
    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public ShopType getShopType() { return shopType; }
    public long getCreatedTime() { return createdTime; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public Currency getCurrency() { return currency; }
    public double getTotalRevenue() { return totalRevenue; }
    public int getTotalSales() { return totalSales; }
    public Set<UUID> getCustomers() { return new HashSet<>(customers); }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isAllowsHaggling() { return allowsHaggling; }
    public void setAllowsHaggling(boolean allowsHaggling) { this.allowsHaggling = allowsHaggling; }
    public double getDiscountRate() { return discountRate; }
    public void setDiscountRate(double discountRate) { this.discountRate = discountRate; }
    public int getBulkThreshold() { return bulkThreshold; }
    public void setBulkThreshold(int bulkThreshold) { this.bulkThreshold = bulkThreshold; }
    
    /**
     * ShopItem class
     */
    public static class ShopItem {
        private final String itemId;
        private String itemName;
        private int quantity;
        private long lastRestocked;
        
        public ShopItem(String itemId, int quantity) {
            this.itemId = itemId;
            this.itemName = ItemHandler.getItemDisplayName(itemId);
            this.quantity = quantity;
            this.lastRestocked = System.currentTimeMillis();
        }
        
        public void addStock(int amount) {
            this.quantity += amount;
            this.lastRestocked = System.currentTimeMillis();
        }
        
        public void removeStock(int amount) {
            this.quantity = Math.max(0, this.quantity - amount);
        }
        
        public String getItemId() { return itemId; }
        public String getItemName() { return itemName; }
        public int getQuantity() { return quantity; }
        public long getLastRestocked() { return lastRestocked; }
    }
    
    /**
     * ItemPricing class
     */
    public static class ItemPricing {
        private final double buyPrice;
        private final double sellPrice;
        
        public ItemPricing(double buyPrice, double sellPrice) {
            this.buyPrice = buyPrice;
            this.sellPrice = sellPrice;
        }
        
        public double getBuyPrice() { return buyPrice; }
        public double getSellPrice() { return sellPrice; }
    }
    
    /**
     * Sale record
     */
    public static class Sale {
        private final UUID buyerId;
        private final String itemId;
        private final int quantity;
        private final double totalCost;
        private final long timestamp;
        
        public Sale(UUID buyerId, String itemId, int quantity, double totalCost, long timestamp) {
            this.buyerId = buyerId;
            this.itemId = itemId;
            this.quantity = quantity;
            this.totalCost = totalCost;
            this.timestamp = timestamp;
        }
        
        public UUID getBuyerId() { return buyerId; }
        public String getItemId() { return itemId; }
        public int getQuantity() { return quantity; }
        public double getTotalCost() { return totalCost; }
        public long getTimestamp() { return timestamp; }
    }
    
    /**
     * Purchase result
     */
    public static class PurchaseResult {
        private final boolean success;
        private final String message;
        
        public PurchaseResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }
    
    /**
     * Sale result
     */
    public static class SaleResult {
        private final boolean success;
        private final String message;
        
        public SaleResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }
    
    /**
     * Shop location
     */
    public static class ShopLocation {
        private final String world;
        private final double x;
        private final double y;
        private final double z;
        
        public ShopLocation(String world, double x, double y, double z) {
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
        }
        
        public String getWorld() { return world; }
        public double getX() { return x; }
        public double getY() { return y; }
        public double getZ() { return z; }
    }
}
