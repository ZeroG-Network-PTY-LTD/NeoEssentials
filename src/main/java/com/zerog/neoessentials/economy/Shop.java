package com.zerog.neoessentials.economy;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

/**
 * Represents a shop in the NeoEssentials economy system.
 * Supports different shop types with varying features and restrictions.
 */
public class Shop {
    private final UUID shopId;
    private final UUID ownerId;
    private String shopName;
    private ShopLocation shopLocation; // Physical location for teleportation
    private String locationName; // Display name for the location
    private String category; // Shop category (armor, blocks, food, etc.)
    private final ShopType shopType;
    private final long createdTime;
    private boolean isActive;
    private final Currency currency;
    
    // Shop inventory and pricing
    private final Map<String, ShopItem> inventory; // Item ID -> Shop Item
    private final Map<String, Double> itemPrices; // Item ID -> Price
    
    // Shop statistics
    private final List<Sale> salesHistory;
    private double totalRevenue;
    private int totalSales;
    private final Set<UUID> customers; // Unique customers
    
    // Shop settings
    private String description;
    private boolean allowsHaggling;
    private double discountRate; // For bulk purchases
    private int bulkThreshold; // Minimum quantity for bulk discount
    
    // Employee management
    private final ShopEmployeeManager employeeManager;
    
    public enum ShopType {
        PLAYER("Player Shop", true, true, -1),
        PLAYER_OWNED("Player Owned", true, true, -1),
        PLAYER_RENTAL("Player Rental", true, true, 30), // 30 day max rental
        SERVER_SHOP("Server Shop", false, false, -1),
        ADMIN("Admin Shop", false, false, -1),
        AUCTION_HOUSE("Auction House", true, false, 7), // 7 day max auctions
        DYNAMIC_SHOP("Dynamic Shop", false, true, -1); // Prices change based on supply/demand
        
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
    
    /**
     * Create a new shop
     * 
     * @param ownerId The owner's UUID
     * @param shopName The shop name
     * @param locationName The shop location name
     * @param shopType The type of shop
     */
    public Shop(UUID ownerId, String shopName, String locationName, String category, ShopType shopType) {
        this.shopId = UUID.randomUUID();
        this.ownerId = ownerId;
        this.shopName = shopName;
        this.shopLocation = null; // Will be set when player uses /shop setlocation
        this.locationName = locationName;
        this.category = category;
        this.shopType = shopType;
        this.createdTime = System.currentTimeMillis();
        this.isActive = true;
        this.currency = CurrencyManager.getInstance().getDefaultCurrency();
        
        this.inventory = new ConcurrentHashMap<>();
        this.itemPrices = new ConcurrentHashMap<>();
        this.salesHistory = new ArrayList<>();
        this.totalRevenue = 0.0;
        this.totalSales = 0;
        this.customers = new HashSet<>();
        
        this.description = "";
        this.allowsHaggling = false;
        this.discountRate = 0.1; // 10% bulk discount
        this.bulkThreshold = 10;
        
        // Initialize employee manager and add owner
        this.employeeManager = new ShopEmployeeManager(this.shopId);
        this.employeeManager.addEmployee(ownerId, "Owner", ShopEmployeeManager.EmployeeRole.OWNER, ownerId);
    }
    
    /**
     * Add an item to the shop inventory
     * 
     * @param itemId The item identifier
     * @param quantity The quantity to add
     * @param price The price per item
     * @param itemName Display name of the item
     * @return true if item was added successfully
     */
    public boolean addItem(String itemId, int quantity, double price, String itemName) {
        if (!isActive || quantity <= 0 || price < 0) {
            return false;
        }
        
        // Validate item exists in Minecraft
        if (!ItemHandler.isValidItem(itemId)) {
            return false;
        }
        
        // Generate display name if not provided
        if (itemName == null || itemName.trim().isEmpty()) {
            itemName = ItemHandler.formatItemName(itemId);
        }
        
        // Check if shop type allows price changes
        if (!shopType.allowsPriceChanges() && itemPrices.containsKey(itemId)) {
            // Use existing price for fixed-price shops
            price = itemPrices.get(itemId);
        }
        
        ShopItem existingItem = inventory.get(itemId);
        if (existingItem != null) {
            // Update existing item
            existingItem.addQuantity(quantity);
        } else {
            // Add new item
            ShopItem shopItem = new ShopItem(itemId, itemName, quantity);
            inventory.put(itemId, shopItem);
        }
        
        itemPrices.put(itemId, price);
        
        // Save shop changes to persistence
        saveToStorage();
        
        return true;
    }
    
    /**
     * Remove an item from the shop inventory
     * 
     * @param itemId The item identifier
     * @param quantity The quantity to remove
     * @return true if item was removed successfully
     */
    public boolean removeItem(String itemId, int quantity) {
        ShopItem item = inventory.get(itemId);
        if (item == null || item.getQuantity() < quantity) {
            return false;
        }
        
        item.removeQuantity(quantity);
        
        // Remove item completely if quantity reaches 0
        if (item.getQuantity() <= 0) {
            inventory.remove(itemId);
            itemPrices.remove(itemId);
        }
        
        // Save shop changes to persistence
        saveToStorage();
        
        return true;
    }
    
    /**
     * Check if shop has an item in stock
     * 
     * @param itemId The item identifier
     * @param quantity The required quantity
     * @return true if item is available in sufficient quantity
     */
    public boolean hasItemInStock(String itemId, int quantity) {
        ShopItem item = inventory.get(itemId);
        return item != null && item.getQuantity() >= quantity;
    }
    
    /**
     * Check if shop has any item for sale (by name)
     * 
     * @param searchTerm The search term
     * @return true if shop has item matching the search term
     */
    public boolean hasItemForSale(String searchTerm) {
        String lowerSearch = searchTerm.toLowerCase();
        return inventory.values().stream()
                .anyMatch(item -> item.getItemName().toLowerCase().contains(lowerSearch));
    }
    
    /**
     * Get the price of an item
     * 
     * @param itemId The item identifier
     * @return The price per item, or 0.0 if item not found
     */
    public double getItemPrice(String itemId) {
        return itemPrices.getOrDefault(itemId, 0.0);
    }
    
    /**
     * Set the price of an item
     * 
     * @param itemId The item identifier
     * @param price The new price
     * @return true if price was set successfully
     */
    public boolean setItemPrice(String itemId, double price) {
        if (!shopType.allowsPriceChanges() || price < 0) {
            return false;
        }
        
        // Set the price regardless of whether item is in inventory
        itemPrices.put(itemId, price);
        
        // If item isn't in inventory yet, add it with 0 stock
        if (!inventory.containsKey(itemId)) {
            ShopItem newItem = new ShopItem(itemId, 0, price);
            inventory.put(itemId, newItem);
        }
        
        return true;
    }
    
    /**
     * Calculate the total price for a purchase including discounts
     * 
     * @param itemId The item identifier
     * @param quantity The quantity to purchase
     * @return The total price after any applicable discounts
     */
    public double calculateTotalPrice(String itemId, int quantity) {
        double basePrice = getItemPrice(itemId) * quantity;
        
        // Apply bulk discount if applicable
        if (quantity >= bulkThreshold) {
            basePrice *= (1.0 - discountRate);
        }
        
        return basePrice;
    }
    
    /**
     * Record a sale
     * 
     * @param buyerId The buyer's UUID
     * @param itemId The item sold
     * @param quantity The quantity sold
     * @param totalPrice The total sale price
     * @param timestamp The sale timestamp
     */
    public void recordSale(UUID buyerId, String itemId, int quantity, double totalPrice, long timestamp) {
        Sale sale = new Sale(buyerId, itemId, quantity, totalPrice, timestamp);
        salesHistory.add(sale);
        
        totalRevenue += totalPrice;
        totalSales++;
        customers.add(buyerId);
        
        // Save shop changes to persistence
        saveToStorage();
    }
    
    /**
     * Get all items currently for sale
     * 
     * @return Map of item ID to shop item
     */
    public Map<String, ShopItem> getInventory() {
        return new HashMap<>(inventory);
    }
    
    /**
     * Get available items (items with quantity > 0)
     * 
     * @return Collection of available shop items
     */
    public Collection<ShopItem> getAvailableItems() {
        return inventory.values().stream()
                .filter(item -> item.getQuantity() > 0)
                .toList();
    }
    
    /**
     * Get shop performance metrics
     * 
     * @param days Number of days to analyze
     * @return Map of performance metrics
     */
    public Map<String, Object> getPerformanceMetrics(int days) {
        long cutoffTime = System.currentTimeMillis() - (days * 24L * 60L * 60L * 1000L);
        
        List<Sale> recentSales = salesHistory.stream()
                .filter(sale -> sale.getTimestamp() >= cutoffTime)
                .toList();
        
        double recentRevenue = recentSales.stream()
                .mapToDouble(Sale::getTotalPrice)
                .sum();
        
        int recentSalesCount = recentSales.size();
        
        Set<UUID> recentCustomers = recentSales.stream()
                .map(Sale::getBuyerId)
                .collect(HashSet::new, HashSet::add, HashSet::addAll);
        
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("revenue", recentRevenue);
        metrics.put("sales", recentSalesCount);
        metrics.put("customers", recentCustomers.size());
        metrics.put("averageSale", recentSalesCount > 0 ? recentRevenue / recentSalesCount : 0.0);
        metrics.put("itemsInStock", inventory.values().stream().mapToInt(ShopItem::getQuantity).sum());
        metrics.put("uniqueItems", inventory.size());
        
        return metrics;
    }
    
    /**
     * Get sales history within a time period
     * 
     * @param days Number of days to look back
     * @return List of sales
     */
    public List<Sale> getSalesHistory(int days) {
        long cutoffTime = System.currentTimeMillis() - (days * 24L * 60L * 60L * 1000L);
        
        return salesHistory.stream()
                .filter(sale -> sale.getTimestamp() >= cutoffTime)
                .sorted((s1, s2) -> Long.compare(s2.getTimestamp(), s1.getTimestamp()))
                .toList();
    }
    
    // Getters and setters
    public UUID getShopId() { return shopId; }
    public UUID getOwnerId() { return ownerId; }
    public String getName() { return shopName; }
    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }
    public ShopLocation getShopLocation() { return shopLocation; }
    public void setShopLocation(ShopLocation shopLocation) { this.shopLocation = shopLocation; }
    public String getLocation() { return locationName; } // For backwards compatibility
    public String getLocationName() { return locationName; }
    public void setLocation(String locationName) { this.locationName = locationName; } // For backwards compatibility
    public void setLocationName(String locationName) { this.locationName = locationName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public ShopType getShopType() { return shopType; }
    public long getCreatedTime() { return createdTime; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { this.isActive = active; }
    public Currency getCurrency() { return currency; }
    public double getTotalRevenue() { return totalRevenue; }
    public int getTotalSales() { return totalSales; }
    public int getCustomerCount() { return customers.size(); }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean allowsHaggling() { return allowsHaggling; }
    public void setAllowsHaggling(boolean allowsHaggling) { this.allowsHaggling = allowsHaggling; }
    public double getDiscountRate() { return discountRate; }
    public void setDiscountRate(double discountRate) { this.discountRate = Math.max(0.0, Math.min(1.0, discountRate)); }
    public int getBulkThreshold() { return bulkThreshold; }
    public void setBulkThreshold(int bulkThreshold) { this.bulkThreshold = Math.max(1, bulkThreshold); }
    
    // Employee management methods
    public ShopEmployeeManager getEmployeeManager() { return employeeManager; }
    
    /**
     * Check if a player has permission to perform an action in this shop
     * 
     * @param playerId The player's UUID
     * @param permission The permission to check
     * @return true if player has permission
     */
    public boolean hasPermission(UUID playerId, ShopEmployeeManager.ShopPermission permission) {
        return employeeManager.hasPermission(playerId, permission);
    }
    
    /**
     * Add an employee to the shop
     * 
     * @param managerId The manager adding the employee
     * @param employeeId The new employee's UUID
     * @param employeeName The new employee's name
     * @param role The role to assign
     * @return true if employee was added successfully
     */
    public boolean addEmployee(UUID managerId, UUID employeeId, String employeeName, ShopEmployeeManager.EmployeeRole role) {
        if (!hasPermission(managerId, ShopEmployeeManager.ShopPermission.HIRE_EMPLOYEES)) {
            return false;
        }
        
        boolean result = employeeManager.addEmployee(employeeId, employeeName, role, managerId);
        if (result) {
            saveToStorage();
        }
        return result;
    }
    
    /**
     * Remove an employee from the shop
     * 
     * @param managerId The manager removing the employee
     * @param employeeId The employee to remove
     * @return true if employee was removed successfully
     */
    public boolean removeEmployee(UUID managerId, UUID employeeId) {
        if (!hasPermission(managerId, ShopEmployeeManager.ShopPermission.FIRE_EMPLOYEES)) {
            return false;
        }
        
        boolean result = employeeManager.removeEmployee(employeeId, managerId);
        if (result) {
            saveToStorage();
        }
        return result;
    }
    
    /**
     * Change an employee's role
     * 
     * @param managerId The manager making the change
     * @param employeeId The employee whose role to change
     * @param newRole The new role
     * @return true if role was changed successfully
     */
    public boolean changeEmployeeRole(UUID managerId, UUID employeeId, ShopEmployeeManager.EmployeeRole newRole) {
        if (!hasPermission(managerId, ShopEmployeeManager.ShopPermission.CHANGE_ROLES)) {
            return false;
        }
        
        boolean result = employeeManager.changeEmployeeRole(employeeId, newRole, managerId);
        if (result) {
            saveToStorage();
        }
        return result;
    }
    
    /**
     * Check if a player is an employee of this shop
     * 
     * @param playerId The player to check
     * @return true if player is an employee
     */
    public boolean isEmployee(UUID playerId) {
        return employeeManager.isEmployee(playerId);
    }
    
    /**
     * Get all active employees
     * 
     * @return List of active employees
     */
    public List<ShopEmployeeManager.ShopEmployee> getActiveEmployees() {
        return employeeManager.getActiveEmployees();
    }
    
    /**
     * Override the existing addItem method to include permission checking
     */
    public boolean addItem(String itemId, int quantity, double price, String itemName, UUID operatorId) {
        if (operatorId != null && !hasPermission(operatorId, ShopEmployeeManager.ShopPermission.MANAGE_INVENTORY)) {
            return false;
        }
        
        return addItem(itemId, quantity, price, itemName);
    }
    
    /**
     * Override the existing setItemPrice method to include permission checking
     */
    public boolean setItemPrice(String itemId, double price, UUID operatorId) {
        if (operatorId != null && !hasPermission(operatorId, ShopEmployeeManager.ShopPermission.SET_PRICES)) {
            return false;
        }
        
        return setItemPrice(itemId, price);
    }
    
    /**
     * Check if this shop has a teleportation location set
     * 
     * @return True if shop has a valid location for teleportation
     */
    public boolean hasLocation() {
        return shopLocation != null && shopLocation.isValid();
    }
    
    /**
     * Get formatted location string for display
     * 
     * @return Formatted location string or "No location set"
     */
    public String getFormattedLocation() {
        if (!hasLocation()) {
            return "No location set";
        }
        return shopLocation.getFormattedLocation();
    }
    
    /**
     * Set the shop's physical location from a player's current position
     * 
     * @param player The player to get location from
     * @return True if location was set successfully
     */
    public boolean setLocationFromPlayer(ServerPlayer player) {
        try {
            this.shopLocation = new ShopLocation(player);
            saveToStorage();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String toString() {
        return "Shop{" +
                "shopId=" + shopId +
                ", shopName='" + shopName + '\'' +
                ", locationName='" + locationName + '\'' +
                ", type=" + shopType +
                ", isActive=" + isActive +
                '}';
    }
    
    /**
     * Inner class representing an item in the shop
     */
    public static class ShopItem {
        private final String itemId;
        private final String itemName;
        private int quantity;
        private final long addedTime;
        
        public ShopItem(String itemId, String itemName, int quantity) {
            this.itemId = itemId;
            this.itemName = itemName;
            this.quantity = quantity;
            this.addedTime = System.currentTimeMillis();
        }
        
        public void addQuantity(int amount) {
            this.quantity += amount;
        }
        
        public void removeQuantity(int amount) {
            this.quantity = Math.max(0, this.quantity - amount);
        }
        
        public String getItemId() { return itemId; }
        public String getItemName() { return itemName; }
        public int getQuantity() { return quantity; }
        public long getAddedTime() { return addedTime; }
    }
    
    /**
     * Inner class representing a sale record
     */
    public static class Sale {
        private final UUID buyerId;
        private final String itemId;
        private final int quantity;
        private final double totalPrice;
        private final long timestamp;
        
        public Sale(UUID buyerId, String itemId, int quantity, double totalPrice, long timestamp) {
            this.buyerId = buyerId;
            this.itemId = itemId;
            this.quantity = quantity;
            this.totalPrice = totalPrice;
            this.timestamp = timestamp;
        }
        
        public UUID getBuyerId() { return buyerId; }
        public String getItemId() { return itemId; }
        public int getQuantity() { return quantity; }
        public double getTotalPrice() { return totalPrice; }
        public long getTimestamp() { return timestamp; }
    }
    
    /**
     * Save this shop to persistent storage
     */
    private void saveToStorage() {
        try {
            com.zerog.neoessentials.economy.persistence.EconomyPersistenceManager.getInstance().saveShop(this);
        } catch (Exception e) {
            // Log error but don't fail the operation
            System.err.println("Failed to save shop " + shopName + " to storage: " + e.getMessage());
        }
    }
}
