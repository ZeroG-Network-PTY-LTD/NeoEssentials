package com.zerog.neoessentials.economy.shop;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.economy.Currency;
import com.zerog.neoessentials.economy.EconomyAccount;
import com.zerog.neoessentials.economy.EconomyManager;
import com.zerog.neoessentials.economy.Transaction;
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
    
    public ShopManager(EconomyManager economyManager) {
        this.economyManager = Objects.requireNonNull(economyManager, "Economy manager cannot be null");
    }
    
    /**
     * Adds an item to the shop
     */
    public boolean addShopItem(ShopItem item) {
        if (item == null) return false;
        
        try {
            shopItems.put(item.getId(), item);
            indexItem(item);
            NeoEssentials.LOGGER.info("Added shop item: {}", item);
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
     * Gets all shop items
     */
    public List<ShopItem> getAllItems() {
        return new ArrayList<>(shopItems.values());
    }
    
    /**
     * Gets available shop items (in stock)
     */
    public List<ShopItem> getAvailableItems() {
        return shopItems.values().stream()
                .filter(item -> item.getStock() != 0)
                .collect(Collectors.toList());
    }
    
    /**
     * Player buys an item from the shop
     */
    public BuyResult buyItem(ServerPlayer player, UUID itemId, int quantity) {
        if (!economyManager.isEnabled()) {
            return new BuyResult(false, "Economy system is disabled");
        }
        
        ShopItem item = shopItems.get(itemId);
        if (item == null) {
            return new BuyResult(false, "Item not found in shop");
        }
        
        if (!item.canBuy()) {
            return new BuyResult(false, "This item is not for sale");
        }
        
        if (quantity <= 0) {
            return new BuyResult(false, "Invalid quantity");
        }
        
        if (item.getStock() < quantity) {
            return new BuyResult(false, "Insufficient stock (available: " + item.getStock() + ")");
        }
        
        BigDecimal totalCost = item.getBuyPrice().multiply(BigDecimal.valueOf(quantity));
        
        // Check if player has enough money
        EconomyAccount account = economyManager.getOrCreateAccount(player.getUUID(), player.getName().getString());
        if (account == null) {
            return new BuyResult(false, "Could not access your account");
        }
        
        if (!account.hasBalance(item.getCurrency(), totalCost)) {
            return new BuyResult(false, "Insufficient funds. Required: " + item.getCurrency().format(totalCost));
        }
        
        // Check if player has inventory space
        ItemStack itemToGive = item.getItemStack().copy();
        itemToGive.setCount(quantity);
        
        if (!hasInventorySpace(player, itemToGive)) {
            return new BuyResult(false, "Insufficient inventory space");
        }
        
        // Process the transaction
        try {
            // Deduct money
            if (!economyManager.subtractMoney(player.getUUID(), totalCost, item.getCurrency(), 
                    "Shop purchase: " + quantity + "x " + item.getItemStack().getDisplayName().getString())) {
                return new BuyResult(false, "Failed to process payment");
            }
            
            // Give items to player
            if (!player.getInventory().add(itemToGive)) {
                // Rollback payment if item giving fails
                economyManager.addMoney(player.getUUID(), totalCost, item.getCurrency(), 
                        "Shop purchase rollback");
                return new BuyResult(false, "Failed to add items to inventory");
            }
            
            // Update shop stock
            ShopItem updatedItem = item.withStock(item.getStock() - quantity);
            shopItems.put(itemId, updatedItem);
            
            String message = String.format("Successfully purchased %dx %s for %s", 
                    quantity, 
                    item.getItemStack().getDisplayName().getString(),
                    item.getCurrency().format(totalCost));
            
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
        
        BigDecimal totalPayment = item.getSellPrice().multiply(BigDecimal.valueOf(quantity));
        
        try {
            // Remove items from player
            if (!removeItems(player, requiredItem)) {
                return new SellResult(false, "Failed to remove items from inventory");
            }
            
            // Pay player
            if (!economyManager.addMoney(player.getUUID(), totalPayment, item.getCurrency(), 
                    "Shop sale: " + quantity + "x " + item.getItemStack().getDisplayName().getString())) {
                // Try to give items back if payment fails
                player.getInventory().add(requiredItem);
                return new SellResult(false, "Failed to process payment");
            }
            
            // Update shop stock (only if not admin item - admin items have unlimited capacity)
            if (!item.isAdminItem()) {
                ShopItem updatedItem = item.withStock(item.getStock() + quantity);
                shopItems.put(itemId, updatedItem);
            }
            
            String message = String.format("Successfully sold %dx %s for %s", 
                    quantity, 
                    item.getItemStack().getDisplayName().getString(),
                    item.getCurrency().format(totalPayment));
            
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
