package com.zerog.neoessentials.ui.shop;

import com.zerog.neoessentials.economy.Shop;
import com.zerog.neoessentials.economy.EconomyManager;
import com.zerog.neoessentials.economy.Currency;
import com.zerog.neoessentials.util.LanguageUtil;
import com.zerog.neoessentials.NeoEssentials;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.SimpleContainer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Menu handler for shop interface GUI
 * Handles click events and transactions
 */
public class ShopInterfaceMenu extends ChestMenu {
    
    private final Shop shop;
    private final SimpleContainer container;
    
    public ShopInterfaceMenu(int containerId, Inventory playerInventory, SimpleContainer container, Shop shop) {
        super(MenuType.GENERIC_9x6, containerId, playerInventory, container, 6);
        this.shop = shop;
        this.container = container;
    }
    
    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        return false;
    }
    
    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        
        // Prevent taking items from the GUI
        if (slotId < 0 || slotId >= container.getContainerSize()) {
            return;
        }
        
        ItemStack clickedItem = container.getItem(slotId);
        if (clickedItem.isEmpty()) {
            return;
        }
        
        // Get action data from clicked item
        CustomData customData = clickedItem.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return;
        }
        
        CompoundTag actionData = customData.copyTag();
        String action = actionData.getString("Action");
        
        switch (action) {
            case "shop_item" -> handleShopItemClick(serverPlayer, actionData, button, clickType);
            case "close" -> serverPlayer.closeContainer();
            case "refresh" -> refreshShopInterface(serverPlayer);
        }
    }
    
    /**
     * Handles clicking on shop items for buying/selling
     */
    private void handleShopItemClick(ServerPlayer player, CompoundTag actionData, int button, ClickType clickType) {
        String itemName = actionData.getString("ItemName");
        double buyPrice = actionData.getDouble("BuyPrice");
        double sellPrice = actionData.getDouble("SellPrice");
        int stock = actionData.getInt("Stock");
        
        EconomyManager economyManager = NeoEssentials.getInstance().getDataManager().getNewEconomyManager();
        
        // Determine quantity based on shift-click
        int quantity = (clickType == ClickType.QUICK_MOVE) ? 5 : 1;
        
        if (button == 0) { // Left-click = Buy
            handleBuyItem(player, economyManager, itemName, buyPrice, quantity, stock);
        } else if (button == 1) { // Right-click = Sell
            if (sellPrice > 0) {
                handleSellItem(player, economyManager, itemName, sellPrice, quantity);
            } else {
                MessageUtil.sendErrorMessage(player, "This shop doesn't buy " + itemName);
            }
        }
    }
    
    /**
     * Handles buying items from the shop
     */
    private void handleBuyItem(ServerPlayer player, EconomyManager economyManager, 
                              String itemName, double buyPrice, int quantity, int stock) {
        
        // Check stock
        if (stock < quantity) {
            MessageUtil.sendErrorMessage(player, "Not enough stock! Available: " + stock);
            return;
        }
        
        // Calculate total cost
        double totalCost = buyPrice * quantity;
        
        // Check if player can afford
        com.zerog.neoessentials.economy.Currency defaultCurrency = com.zerog.neoessentials.economy.CurrencyManager.getInstance().getDefaultCurrency();
        double availableFunds = economyManager.getTotalAvailableFunds(player.getUUID(), defaultCurrency);
        if (availableFunds < totalCost) {
            MessageUtil.sendErrorMessage(player, 
                "Not enough money! Cost: $" + String.format("%.2f", totalCost) + 
                ", Available: $" + String.format("%.2f", availableFunds));
            return;
        }
        
        // Check inventory space
        if (!hasInventorySpace(player, itemName, quantity)) {
            MessageUtil.sendErrorMessage(player, "Not enough inventory space!");
            return;
        }
        
        // Process the purchase using smart payment
        try {
            boolean success = economyManager.makeSmartPayment(player.getUUID(), totalCost, defaultCurrency, "Shop purchase");
            if (!success) {
                MessageUtil.sendErrorMessage(player, "Payment failed! Please try again.");
                return;
            }
            
            // Remove items from shop - need to check if shop has this method
            shop.removeItem(itemName, quantity);
            
            // Give items to player
            giveItemToPlayer(player, itemName, quantity);
            
            // Success message
            String displayName = itemName; // Use item name directly
            MessageUtil.sendSuccessMessage(player, 
                "Purchased " + quantity + "x " + displayName + " for $" + String.format("%.2f", totalCost));
            
            // Pay shop owner (if it's a player shop)
            if (shop.getOwnerId() != null) {
                economyManager.getWalletManager().addCash(shop.getOwnerId(), totalCost);
            }
            
            // Refresh the interface
            refreshShopInterface(player);
            
        } catch (Exception e) {
            MessageUtil.sendErrorMessage(player, "Transaction failed: " + e.getMessage());
        }
    }
    
    /**
     * Handles selling items to the shop
     */
    private void handleSellItem(ServerPlayer player, EconomyManager economyManager, 
                               String itemName, double sellPrice, int quantity) {
        
        // Check if player has enough items
        int playerItemCount = getPlayerItemCount(player, itemName);
        if (playerItemCount < quantity) {
            MessageUtil.sendErrorMessage(player, 
                "Not enough items! You have " + playerItemCount + ", need " + quantity);
            return;
        }
        
        // Calculate total payment
        double totalPayment = sellPrice * quantity;
        
        // Check if shop can afford to buy (for player shops)
        if (shop.getOwnerId() != null) {
            com.zerog.neoessentials.economy.Currency defaultCurrency = com.zerog.neoessentials.economy.CurrencyManager.getInstance().getDefaultCurrency();
            double shopOwnerBalance = economyManager.getTotalAvailableFunds(shop.getOwnerId(), defaultCurrency);
            if (shopOwnerBalance < totalPayment) {
                MessageUtil.sendErrorMessage(player, "Shop owner cannot afford to buy your items!");
                return;
            }
        }
        
        try {
            // Remove items from player
            if (!removeItemFromPlayer(player, itemName, quantity)) {
                MessageUtil.sendErrorMessage(player, "Failed to remove items from inventory!");
                return;
            }
            
            // Add items to shop
            shop.addItem(itemName, quantity, sellPrice, itemName);
            
            // Pay player
            economyManager.getWalletManager().addCash(player.getUUID(), totalPayment);
            
            // Charge shop owner (if it's a player shop)
            if (shop.getOwnerId() != null) {
                com.zerog.neoessentials.economy.Currency defaultCurrency = com.zerog.neoessentials.economy.CurrencyManager.getInstance().getDefaultCurrency();
                economyManager.makeSmartPayment(shop.getOwnerId(), totalPayment, defaultCurrency, "Buying from player");
            }
            
            // Success message
            String displayName = itemName; // Use item name directly
            MessageUtil.sendSuccessMessage(player, 
                "Sold " + quantity + "x " + displayName + " for $" + String.format("%.2f", totalPayment));
            
            // Refresh the interface
            refreshShopInterface(player);
            
        } catch (Exception e) {
            MessageUtil.sendErrorMessage(player, "Transaction failed: " + e.getMessage());
        }
    }
    
    /**
     * Refreshes the shop interface
     */
    private void refreshShopInterface(ServerPlayer player) {
        // Clear the container
        container.clearContent();
        
        // Re-setup the interface
        EconomyManager economyManager = NeoEssentials.getInstance().getDataManager().getNewEconomyManager();
        ShopInterfaceGUI gui = new ShopInterfaceGUI(shop, economyManager);
        
        // Re-open the interface
        player.closeContainer();
        gui.openShopInterface(player);
    }
    
    /**
     * Checks if player has enough inventory space
     */
    private boolean hasInventorySpace(ServerPlayer player, String itemName, int quantity) {
        ItemStack targetItem = createItemFromString(itemName);
        if (targetItem.isEmpty()) return false;
        
        int slotsNeeded = (quantity + targetItem.getMaxStackSize() - 1) / targetItem.getMaxStackSize();
        int emptySlots = 0;
        
        for (ItemStack stack : player.getInventory().items) {
            if (stack.isEmpty()) {
                emptySlots++;
            } else if (ItemStack.isSameItem(stack, targetItem)) {
                int canAdd = targetItem.getMaxStackSize() - stack.getCount();
                if (canAdd > 0) {
                    quantity -= canAdd;
                    if (quantity <= 0) return true;
                }
            }
        }
        
        return emptySlots >= slotsNeeded;
    }
    
    /**
     * Gives items to player
     */
    private void giveItemToPlayer(ServerPlayer player, String itemName, int quantity) {
        ItemStack item = createItemFromString(itemName);
        if (item.isEmpty()) return;
        
        while (quantity > 0) {
            int stackSize = Math.min(quantity, item.getMaxStackSize());
            ItemStack stack = item.copy();
            stack.setCount(stackSize);
            
            if (!player.getInventory().add(stack)) {
                // Drop items if inventory is full
                player.drop(stack, false);
            }
            
            quantity -= stackSize;
        }
    }
    
    /**
     * Removes items from player inventory
     */
    private boolean removeItemFromPlayer(ServerPlayer player, String itemName, int quantity) {
        ItemStack targetItem = createItemFromString(itemName);
        if (targetItem.isEmpty()) return false;
        
        int remaining = quantity;
        
        for (int i = 0; i < player.getInventory().items.size() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().items.get(i);
            if (ItemStack.isSameItem(stack, targetItem)) {
                int toRemove = Math.min(remaining, stack.getCount());
                stack.shrink(toRemove);
                remaining -= toRemove;
            }
        }
        
        return remaining == 0;
    }
    
    /**
     * Gets player's item count
     */
    private int getPlayerItemCount(ServerPlayer player, String itemName) {
        ItemStack targetItem = createItemFromString(itemName);
        if (targetItem.isEmpty()) return 0;
        
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (ItemStack.isSameItem(stack, targetItem)) {
                count += stack.getCount();
            }
        }
        return count;
    }
    
    /**
     * Creates ItemStack from string
     */
    private ItemStack createItemFromString(String itemName) {
        try {
            if (itemName.startsWith("minecraft:")) {
                itemName = itemName.substring(10);
            }
            
            net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                net.minecraft.resources.ResourceLocation.parse("minecraft:" + itemName));
            
            if (item != null && item != net.minecraft.world.item.Items.AIR) {
                return new ItemStack(item);
            }
        } catch (Exception e) {
            // Item not found
        }
        
        return ItemStack.EMPTY;
    }
    
    @Override
    public boolean stillValid(Player player) {
        return shop.isActive() && super.stillValid(player);
    }
}
