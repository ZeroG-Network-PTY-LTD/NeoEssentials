package com.zerog.neoessentials.economy.shop;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.economy.Currency;
import com.zerog.neoessentials.economy.EconomyManager;
import com.zerog.neoessentials.economy.analytics.ShopAnalyticsManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages shop operations and item listings
 */
public class ShopManager {
    
    private final EconomyManager economyManager;
    private final Map<UUID, ShopItem> shopItems = new ConcurrentHashMap<>();
    private final Map<String, Set<UUID>> itemIndex = new ConcurrentHashMap<>(); // Item name -> Set of item IDs
    private final ShopAnalyticsManager analytics;
    
    public ShopManager(EconomyManager economyManager) {
        this.economyManager = Objects.requireNonNull(economyManager, "Economy manager cannot be null");
        this.analytics = new ShopAnalyticsManager();
    }
    
    /**
     * Gets the analytics manager
     */
    public ShopAnalyticsManager getAnalytics() {
        return analytics;
    }
    
    /**
     * Adds an item to the shop
     */
    public boolean addShopItem(ShopItem item) {
        if (!validateShopItem(item)) {
            return false;
        }
        
        try {
            shopItems.put(item.getId(), item);
            indexItem(item);
            NeoEssentials.LOGGER.info("Added shop item: {} for {} {}", 
                item.getItemStack().getHoverName().getString(),
                item.getCurrency().format(item.getBuyPrice()),
                item.getStock() > 0 ? "(" + item.getStock() + " in stock)" : "");
            return true;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to add shop item", e);
            return false;
        }
    }
    
    /**
     * Removes an item from the shop
     */
    public boolean removeShopItem(UUID itemId) {
        ShopItem removed = shopItems.remove(itemId);
        if (removed != null) {
            removeFromIndex(removed);
            NeoEssentials.LOGGER.info("Removed shop item: {}", removed);
            return true;
        }
        return false;
    }
    
    /**
     * Gets a shop item by ID
     */
    public Optional<ShopItem> getShopItem(UUID itemId) {
        return Optional.ofNullable(shopItems.get(itemId));
    }
    
    /**
     * Searches for shop items by item name or type
     */
    public List<ShopItem> searchItems(String query, ShopItem.Type type) {
        return shopItems.values().stream()
                .filter(item -> type == null || item.getType() == type || item.getType() == ShopItem.Type.BOTH)
                .filter(item -> query == null || 
                    item.getItemStack().getDisplayName().getString().toLowerCase().contains(query.toLowerCase()) ||
                    item.getDescription().toLowerCase().contains(query.toLowerCase()))
                .sorted(Comparator.comparing(item -> item.getItemStack().getDisplayName().getString()))
                .collect(Collectors.toList());
    }
    
    /**
     * Gets all buyable items
     */
    public List<ShopItem> getBuyableItems() {
        return shopItems.values().stream()
                .filter(ShopItem::canBuy)
                .filter(ShopItem::hasStock)
                .sorted(Comparator.comparing(item -> item.getItemStack().getDisplayName().getString()))
                .collect(Collectors.toList());
    }
    
    /**
     * Gets all sellable items
     */
    public List<ShopItem> getSellableItems() {
        return shopItems.values().stream()
                .filter(ShopItem::canSell)
                .sorted(Comparator.comparing(item -> item.getItemStack().getDisplayName().getString()))
                .collect(Collectors.toList());
    }
    
    /**
     * Gets all shop items (including out of stock items)
     */
    public List<ShopItem> getAllItems() {
        return new ArrayList<>(shopItems.values());
    }
    
    /**
     * Gets available shop items (in stock)
     */
    public List<ShopItem> getAvailableItems() {
        return shopItems.values().stream()
                .filter(item -> item.hasStock()) // Use hasStock() which properly handles infinite stock
                .collect(Collectors.toList());
    }
    
    /**
     * Player buys an item from the shop
     */
    public BuyResult buyItem(ServerPlayer player, UUID itemId, int quantity) {
        if (!economyManager.isEnabled()) {
            return new BuyResult(false, "Economy system is disabled");
        }
        
        // Validate the purchase request
        if (!validatePurchaseRequest(itemId, player, quantity)) {
            return new BuyResult(false, "Invalid purchase request");
        }
        
        ShopItem item = shopItems.get(itemId);
        if (item == null) {
            return new BuyResult(false, "Item not found in shop");
        }
        
        if (!item.canBuy()) {
            return new BuyResult(false, "This item is not for sale");
        }
        
        // Check stock (admin items with stock = -1 have infinite stock)
        if (item.getStock() >= 0 && item.getStock() < quantity) {
            return new BuyResult(false, "Insufficient stock (available: " + item.getStock() + ")");
        }
        
        // Use the item's currency for the transaction
        Currency currency = item.getCurrency();
        BigDecimal totalCost = item.getBuyPrice().multiply(BigDecimal.valueOf(quantity));
        
        // Check if player has enough money using the proper currency
        if (!economyManager.hasBalance(player.getUUID(), totalCost)) {
            return new BuyResult(false, "Insufficient funds. Required: " + currency.format(totalCost) + 
                                      ", Available: " + currency.format(economyManager.getBalance(player.getUUID())));
        }
        
        // Check if player has inventory space
        ItemStack itemToGive = item.getItemStack().copy();
        itemToGive.setCount(quantity);
        
        if (!hasInventorySpace(player, itemToGive)) {
            return new BuyResult(false, "Insufficient inventory space");
        }
        
        // Process the transaction
        try {
            // Deduct money using the default currency (the economy manager will handle conversion if needed)
            if (!economyManager.subtractMoney(player.getUUID(), totalCost, economyManager.getDefaultCurrency(), 
                    "Shop purchase: " + quantity + "x " + item.getItemStack().getHoverName().getString())) {
                return new BuyResult(false, "Failed to process payment");
            }
            
            // Give items to player
            if (!player.getInventory().add(itemToGive)) {
                // Rollback payment if item giving fails
                economyManager.addMoney(player.getUUID(), totalCost, economyManager.getDefaultCurrency(), 
                        "Shop purchase rollback");
                return new BuyResult(false, "Failed to add items to inventory");
            }
            
            // Update shop stock (only if not admin item - admin items have unlimited stock)
            if (!item.isAdminItem() && item.getStock() > 0) {
                ShopItem updatedItem = item.withStock(item.getStock() - quantity);
                shopItems.put(itemId, updatedItem);
            }
            
            // If this is a player shop, pay the seller
            if (item.getCreatedBy() != null) {
                economyManager.addMoney(item.getCreatedBy(), totalCost, economyManager.getDefaultCurrency(), 
                        "Shop sale: " + quantity + "x " + item.getItemStack().getHoverName().getString());
            }
            
            // Record analytics
            analytics.recordTransaction(player.getUUID(), item.getCreatedBy(), item, quantity, totalCost);
            
            // Log the shop transaction
            NeoEssentials.getInstance().getEconomyManager().getTransactionLogger()
                .logShopTransaction("PURCHASE", player.getUUID(), player.getName().getString(),
                                  item.getItemStack().getHoverName().getString(), quantity, totalCost,
                                  currency, item.getCreatedBy() != null ? "player" : "admin");
            
            String message = String.format("Successfully purchased %dx %s for %s", 
                    quantity, 
                    item.getItemStack().getHoverName().getString(),
                    currency.format(totalCost));
            
            return new BuyResult(true, message);
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error processing shop purchase", e);
            return new BuyResult(false, "An error occurred while processing your purchase");
        }
    }
    
    /**
     * Player sells an item to the shop
     */
    public SellResult sellItem(ServerPlayer player, UUID itemId, int quantity) {
        if (!economyManager.isEnabled()) {
            return new SellResult(false, "Economy system is disabled");
        }
        
        ShopItem item = shopItems.get(itemId);
        if (item == null) {
            return new SellResult(false, "Item not found in shop");
        }
        
        if (!item.canSell()) {
            return new SellResult(false, "This item cannot be sold to the shop");
        }
        
        if (quantity <= 0) {
            return new SellResult(false, "Invalid quantity");
        }
        
        // Check if shop can accept more stock
        if (!item.isAdminItem() && item.getStock() + quantity > item.getMaxStock()) {
            int maxAcceptable = item.getMaxStock() - item.getStock();
            return new SellResult(false, "Shop cannot accept that many items (max acceptable: " + maxAcceptable + ")");
        }
        
        // Check if player has the items
        ItemStack requiredItem = item.getItemStack().copy();
        requiredItem.setCount(quantity);
        
        if (!hasItems(player, requiredItem)) {
            return new SellResult(false, "You don't have enough of this item");
        }
        
        Currency currency = item.getCurrency();
        BigDecimal totalPayment = item.getSellPrice().multiply(BigDecimal.valueOf(quantity));
        
        try {
            // Remove items from player
            if (!removeItems(player, requiredItem)) {
                return new SellResult(false, "Failed to remove items from inventory");
            }
            
            // Pay player using the default currency
            if (!economyManager.addMoney(player.getUUID(), totalPayment, economyManager.getDefaultCurrency(), 
                    "Shop sale: " + quantity + "x " + item.getItemStack().getHoverName().getString())) {
                // Try to give items back if payment fails
                player.getInventory().add(requiredItem);
                return new SellResult(false, "Failed to process payment");
            }
            
            // Update shop stock (only if not admin item - admin items have unlimited capacity)
            if (!item.isAdminItem()) {
                ShopItem updatedItem = item.withStock(item.getStock() + quantity);
                shopItems.put(itemId, updatedItem);
            }
            
            // Record analytics (note: for sell transactions, seller is the admin/shop owner, buyer is the player)
            analytics.recordTransaction(item.getCreatedBy(), player.getUUID(), item, quantity, totalPayment);
            
            // Log the shop transaction
            NeoEssentials.getInstance().getEconomyManager().getTransactionLogger()
                .logShopTransaction("SALE", player.getUUID(), player.getName().getString(),
                                  item.getItemStack().getHoverName().getString(), quantity, totalPayment,
                                  currency, item.getCreatedBy() != null ? "player" : "admin");
            
            String message = String.format("Successfully sold %dx %s for %s", 
                    quantity, 
                    item.getItemStack().getHoverName().getString(),
                    currency.format(totalPayment));
            
            return new SellResult(true, message);
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error processing shop sale", e);
            return new SellResult(false, "An error occurred while processing your sale");
        }
    }
    
    /**
     * Gets shop statistics
     */
    public ShopStatistics getStatistics() {
        int totalItems = shopItems.size();
        int buyableItems = (int) shopItems.values().stream().filter(ShopItem::canBuy).count();
        int sellableItems = (int) shopItems.values().stream().filter(ShopItem::canSell).count();
        int adminItems = (int) shopItems.values().stream().filter(ShopItem::isAdminItem).count();
        
        return new ShopStatistics(totalItems, buyableItems, sellableItems, adminItems);
    }
    
    /**
     * Enhanced debug method to validate shop integrity and diagnose issues
     */
    public void validateShopIntegrity() {
        NeoEssentials.LOGGER.info("=== COMPREHENSIVE SHOP INTEGRITY CHECK ===");
        NeoEssentials.LOGGER.info("Economy system enabled: {}", economyManager.isEnabled());
        NeoEssentials.LOGGER.info("Total items in shop: {}", shopItems.size());
        
        int adminItems = 0;
        int playerItems = 0;
        int infiniteStockItems = 0;
        int buyableItems = 0;
        int sellableItems = 0;
        int bothTypeItems = 0;
        int invalidItems = 0;
        
        for (ShopItem item : shopItems.values()) {
            try {
                // Basic validation
                if (!validateShopItem(item)) {
                    invalidItems++;
                    NeoEssentials.LOGGER.warn("Invalid shop item found: {}", item.getItemStack().getHoverName().getString());
                    continue;
                }
                
                if (item.isAdminItem()) {
                    adminItems++;
                } else {
                    playerItems++;
                }
                
                if (item.getStock() < 0) {
                    infiniteStockItems++;
                }
                
                if (item.canBuy()) {
                    buyableItems++;
                }
                
                if (item.canSell()) {
                    sellableItems++;
                }
                
                if (item.getType() == ShopItem.Type.BOTH) {
                    bothTypeItems++;
                }
                
                NeoEssentials.LOGGER.info("Item: {} | Stock: {} | Type: {} | Admin: {} | Buy: {} | Sell: {} | ID: {}", 
                    item.getItemStack().getHoverName().getString(),
                    item.getStock() < 0 ? "INFINITE" : item.getStock(),
                    item.getType(),
                    item.isAdminItem(),
                    item.canBuy() ? item.getCurrency().format(item.getBuyPrice()) : "N/A",
                    item.canSell() ? item.getCurrency().format(item.getSellPrice()) : "N/A",
                    item.getId().toString().substring(0, 8)
                );
                
            } catch (Exception e) {
                NeoEssentials.LOGGER.error("Error validating shop item", e);
                invalidItems++;
            }
        }
        
        NeoEssentials.LOGGER.info("=== SHOP STATISTICS ===");
        NeoEssentials.LOGGER.info("Admin items: {}", adminItems);
        NeoEssentials.LOGGER.info("Player items: {}", playerItems);
        NeoEssentials.LOGGER.info("Infinite stock items: {}", infiniteStockItems);
        NeoEssentials.LOGGER.info("Buyable items: {}", buyableItems);
        NeoEssentials.LOGGER.info("Sellable items: {}", sellableItems);
        NeoEssentials.LOGGER.info("Both type items: {}", bothTypeItems);
        NeoEssentials.LOGGER.info("Invalid items: {}", invalidItems);
        NeoEssentials.LOGGER.info("Available items (filtered): {}", getAvailableItems().size());
        NeoEssentials.LOGGER.info("Index entries: {}", itemIndex.size());
        
        // Check for potential issues
        if (invalidItems > 0) {
            NeoEssentials.LOGGER.warn("Found {} invalid items in shop!", invalidItems);
        }
        
        if (adminItems == 0) {
            NeoEssentials.LOGGER.warn("No admin items found in shop!");
        }
        
        if (buyableItems == 0) {
            NeoEssentials.LOGGER.warn("No buyable items found in shop!");
        }
        
        NeoEssentials.LOGGER.info("=== End Comprehensive Integrity Check ===");
    }
    
    /**
     * Diagnose economy system issues
     */
    public void diagnoseEconomyIssues() {
        NeoEssentials.LOGGER.info("=== ECONOMY SYSTEM DIAGNOSIS ===");
        
        try {
            NeoEssentials.LOGGER.info("Economy Manager: {}", economyManager != null ? "Present" : "NULL");
            NeoEssentials.LOGGER.info("Economy Enabled: {}", economyManager.isEnabled());
            NeoEssentials.LOGGER.info("Default Currency: {}", economyManager.getDefaultCurrency().getName());
            
            // Test basic economy functions
            UUID testUUID = UUID.randomUUID();
            try {
                BigDecimal testBalance = economyManager.getBalance(testUUID);
                NeoEssentials.LOGGER.info("Balance check test: SUCCESS (returned {})", testBalance);
            } catch (Exception e) {
                NeoEssentials.LOGGER.error("Balance check test: FAILED", e);
            }
            
            // Check shop manager state
            NeoEssentials.LOGGER.info("Shop Items Map: {}", shopItems != null ? "Present" : "NULL");
            NeoEssentials.LOGGER.info("Item Index Map: {}", itemIndex != null ? "Present" : "NULL");
            NeoEssentials.LOGGER.info("Analytics Manager: {}", analytics != null ? "Present" : "NULL");
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error during economy diagnosis", e);
        }
        
        NeoEssentials.LOGGER.info("=== End Economy Diagnosis ===");
    }
    
    // Helper methods
    private void indexItem(ShopItem item) {
        String itemName = item.getItemStack().getDescriptionId();
        itemIndex.computeIfAbsent(itemName, k -> ConcurrentHashMap.newKeySet()).add(item.getId());
    }
    
    private void removeFromIndex(ShopItem item) {
        String itemName = item.getItemStack().getDescriptionId();
        Set<UUID> itemIds = itemIndex.get(itemName);
        if (itemIds != null) {
            itemIds.remove(item.getId());
            if (itemIds.isEmpty()) {
                itemIndex.remove(itemName);
            }
        }
    }
    
    private boolean hasInventorySpace(ServerPlayer player, ItemStack itemStack) {
        return player.getInventory().getFreeSlot() != -1 || 
               canStackInExistingSlots(player, itemStack);
    }
    
    private boolean canStackInExistingSlots(ServerPlayer player, ItemStack itemStack) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack slotStack = player.getInventory().getItem(i);
            if (!slotStack.isEmpty() && ItemStack.isSameItem(slotStack, itemStack)) {
                int spaceInSlot = slotStack.getMaxStackSize() - slotStack.getCount();
                if (spaceInSlot >= itemStack.getCount()) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private boolean hasItems(ServerPlayer player, ItemStack itemStack) {
        int remainingNeeded = itemStack.getCount();
        
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack slotStack = player.getInventory().getItem(i);
            if (!slotStack.isEmpty() && ItemStack.isSameItem(slotStack, itemStack)) {
                remainingNeeded -= slotStack.getCount();
                if (remainingNeeded <= 0) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    private boolean removeItems(ServerPlayer player, ItemStack itemStack) {
        int remainingToRemove = itemStack.getCount();
        
        for (int i = 0; i < player.getInventory().getContainerSize() && remainingToRemove > 0; i++) {
            ItemStack slotStack = player.getInventory().getItem(i);
            if (!slotStack.isEmpty() && ItemStack.isSameItem(slotStack, itemStack)) {
                int toRemoveFromSlot = Math.min(remainingToRemove, slotStack.getCount());
                slotStack.shrink(toRemoveFromSlot);
                remainingToRemove -= toRemoveFromSlot;
            }
        }
        
        return remainingToRemove == 0;
    }
    
    /**
     * Creates a properly configured shop item builder
     */
    public ShopItem.Builder createShopItemBuilder() {
        return new ShopItem.Builder().currency(economyManager.getDefaultCurrency());
    }
    
    /**
     * Validates a shop item before adding it to the shop
     */
    private boolean validateShopItem(ShopItem item) {
        if (item == null) {
            NeoEssentials.LOGGER.warn("Attempted to validate null shop item");
            return false;
        }
        
        if (item.getItemStack() == null || item.getItemStack().isEmpty()) {
            NeoEssentials.LOGGER.warn("Shop item has invalid ItemStack");
            return false;
        }
        
        if (item.getBuyPrice() != null && item.getBuyPrice().compareTo(BigDecimal.ZERO) < 0) {
            NeoEssentials.LOGGER.warn("Shop item has negative buy price: {}", item.getBuyPrice());
            return false;
        }
        
        if (item.getSellPrice() != null && item.getSellPrice().compareTo(BigDecimal.ZERO) < 0) {
            NeoEssentials.LOGGER.warn("Shop item has negative sell price: {}", item.getSellPrice());
            return false;
        }
        
        if (item.getStock() < -1) {
            NeoEssentials.LOGGER.warn("Shop item has invalid stock: {}", item.getStock());
            return false;
        }
        
        return true;
    }
    
    /**
     * Safely adds a shop item with validation
     */
    public boolean safeAddShopItem(ShopItem item) {
        if (!validateShopItem(item)) {
            return false;
        }
        
        try {
            return addShopItem(item);
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to safely add shop item", e);
            return false;
        }
    }
    
    /**
     * Validates a purchase request before processing
     */
    private boolean validatePurchaseRequest(UUID itemId, ServerPlayer player, int quantity) {
        if (itemId == null) {
            NeoEssentials.LOGGER.warn("Purchase request has null item ID");
            return false;
        }
        
        if (player == null) {
            NeoEssentials.LOGGER.warn("Purchase request has null player");
            return false;
        }
        
        if (quantity <= 0) {
            NeoEssentials.LOGGER.warn("Purchase request has invalid quantity: {}", quantity);
            return false;
        }
        
        if (quantity > 2304) { // Max inventory size
            NeoEssentials.LOGGER.warn("Purchase request exceeds max inventory size: {}", quantity);
            return false;
        }
        
        return true;
    }
    
    // Result classes
    public static class BuyResult {
        private final boolean success;
        private final String message;
        
        public BuyResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }
    
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
    
    public static class SellResult {
        private final boolean success;
        private final String message;
        
        public SellResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }
    
    public static class ShopStatistics {
        private final int totalItems;
        private final int buyableItems;
        private final int sellableItems;
        private final int adminItems;
        
        public ShopStatistics(int totalItems, int buyableItems, int sellableItems, int adminItems) {
            this.totalItems = totalItems;
            this.buyableItems = buyableItems;
            this.sellableItems = sellableItems;
            this.adminItems = adminItems;
        }
        
        public int getTotalItems() { return totalItems; }
        public int getBuyableItems() { return buyableItems; }
        public int getSellableItems() { return sellableItems; }
        public int getAdminItems() { return adminItems; }
    }
}
