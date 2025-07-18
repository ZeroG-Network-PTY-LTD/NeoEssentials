package com.zerog.neoessentials.economy.gui;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.economy.EconomyManager;
import com.zerog.neoessentials.economy.shop.ShopItem;
import com.zerog.neoessentials.economy.shop.ShopManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.UUID;

/**
 * Enhanced shop menu with click handling functionality
 */
public class EnhancedShopMenu extends ChestMenu {
    
    private final ServerPlayer player;
    private final EconomyManager economyManager;
    private final EnhancedShopInterface.ShopMode mode;
    private final UUID targetPlayer;
    private final int currentPage;
    private final List<ShopItem> shopItems;
    
    private final Container shopContainer;
    
    public EnhancedShopMenu(int containerId, Inventory playerInventory, Container container, 
                           ServerPlayer player, EconomyManager economyManager, 
                           EnhancedShopInterface.ShopMode mode, UUID targetPlayer, 
                           int currentPage, List<ShopItem> shopItems) {
        super(MenuType.GENERIC_9x5, containerId, playerInventory, container, 5);
        this.player = player;
        this.economyManager = economyManager;
        this.mode = mode;
        this.targetPlayer = targetPlayer;
        this.currentPage = currentPage;
        this.shopItems = shopItems;
        this.shopContainer = container;
    }
    
    @Override
    public boolean stillValid(@Nonnull net.minecraft.world.entity.player.Player menuPlayer) {
        return menuPlayer == player && menuPlayer.isAlive() && !menuPlayer.isRemoved();
    }
    
    @Override
    public boolean clickMenuButton(@Nonnull net.minecraft.world.entity.player.Player menuPlayer, int buttonId) {
        if (menuPlayer != player) return false;
        
        handleClick(buttonId, ClickType.PICKUP);
        return true;
    }
    
    @Override
    @Nonnull
    public ItemStack quickMoveStack(@Nonnull net.minecraft.world.entity.player.Player menuPlayer, int index) {
        if (menuPlayer != player) return ItemStack.EMPTY;
        
        // Handle shift-click
        handleClick(index, ClickType.QUICK_MOVE);
        return ItemStack.EMPTY;
    }
    
    @Override
    public void clicked(int slotIndex, int dragType, @Nonnull ClickType clickType, @Nonnull net.minecraft.world.entity.player.Player menuPlayer) {
        if (menuPlayer != player) return;
        
        // Prevent taking items from shop slots
        if (slotIndex < 45) { // Shop container slots
            handleClick(slotIndex, clickType);
            return;
        }
        
        // Allow normal inventory interactions
        super.clicked(slotIndex, dragType, clickType, menuPlayer);
    }
    
    private void handleClick(int slotIndex, ClickType clickType) {
        try {
            ItemStack clickedItem = this.getSlot(slotIndex).getItem();
            
            if (clickedItem.isEmpty()) return;
            
            // Handle navigation clicks (bottom row)
            if (slotIndex >= 36 && slotIndex <= 44) {
                handleNavigationClick(slotIndex, clickType, clickedItem);
                return;
            }
            
            // Handle shop item clicks
            if (slotIndex < 36) {
                handleShopItemClick(slotIndex, clickType, clickedItem);
                return;
            }
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error handling shop click", e);
            player.sendSystemMessage(Component.literal("§cAn error occurred while processing your click"));
        }
    }
    
    private void handleNavigationClick(int slotIndex, ClickType clickType, ItemStack clickedItem) {
        String itemName = clickedItem.getHoverName().getString();
        
        switch (slotIndex) {
            case 36: // Previous page or Global Shop
                if (itemName.contains("Previous")) {
                    EnhancedShopInterface.openShop(player, economyManager, mode, targetPlayer, currentPage - 1);
                } else if (itemName.contains("Global")) {
                    EnhancedShopInterface.openShop(player, economyManager, EnhancedShopInterface.ShopMode.GLOBAL, null, 0);
                }
                break;
                
            case 37: // Global shop button
                if (itemName.contains("Global")) {
                    EnhancedShopInterface.openShop(player, economyManager, EnhancedShopInterface.ShopMode.GLOBAL, null, 0);
                }
                break;
                
            case 38: // Next page
                if (itemName.contains("Next")) {
                    EnhancedShopInterface.openShop(player, economyManager, mode, targetPlayer, currentPage + 1);
                }
                break;
                
            case 39: // Personal shop button
                if (itemName.contains("My Shop")) {
                    EnhancedShopInterface.openPersonalShop(player, economyManager);
                }
                break;
                
            case 41: // Create shop item or action button
                if (itemName.contains("Create") && mode == EnhancedShopInterface.ShopMode.PERSONAL) {
                    handleCreateShopItem();
                }
                break;
                
            case 42: // Refresh
                if (itemName.contains("Refresh")) {
                    EnhancedShopInterface.openShop(player, economyManager, mode, targetPlayer, currentPage);
                }
                break;
                
            case 44: // Close
                if (itemName.contains("Close")) {
                    player.closeContainer();
                }
                break;
        }
    }
    
    private void handleShopItemClick(int slotIndex, ClickType clickType, ItemStack clickedItem) {
        // Calculate which shop item this corresponds to
        int itemIndex = (currentPage * 36) + slotIndex;
        
        if (itemIndex >= shopItems.size()) {
            return; // Invalid item index
        }
        
        ShopItem shopItem = shopItems.get(itemIndex);
        
        if (shopItem == null) {
            player.sendSystemMessage(Component.literal("§cShop item not found"));
            return;
        }
        
        // Handle different click types
        switch (clickType) {
            case PICKUP: // Left click
                handleBuyItem(shopItem, 1);
                break;
                
            case QUICK_MOVE: // Shift + left click
                // For infinite stock items (stock = -1), buy 64. Otherwise buy up to available stock
                int shiftClickAmount = shopItem.getStock() < 0 ? 64 : Math.min(shopItem.getStock(), 64);
                handleBuyItem(shopItem, shiftClickAmount);
                break;
                
            case PICKUP_ALL: // Double click
                int doubleClickAmount = shopItem.getStock() < 0 ? 64 : Math.min(shopItem.getStock(), 64);
                handleBuyItem(shopItem, doubleClickAmount);
                break;
                
            case QUICK_CRAFT: // Right click
                if (mode == EnhancedShopInterface.ShopMode.PERSONAL && player.getUUID().equals(shopItem.getCreatedBy())) {
                    handleManageItem(shopItem);
                } else if (shopItem.canSell()) {
                    // Right click on items that can be sold - sell to shop
                    handleSellItem(shopItem, 1);
                } else {
                    handleBuyItem(shopItem, 1);
                }
                break;
                
            default:
                handleBuyItem(shopItem, 1);
                break;
        }
    }
    
    private void handleBuyItem(ShopItem shopItem, int quantity) {
        try {
            if (!shopItem.canBuy()) {
                player.sendSystemMessage(Component.literal("§cThis item is not for sale"));
                return;
            }
            
            if (!shopItem.hasStock()) {
                player.sendSystemMessage(Component.literal("§cThis item is out of stock"));
                return;
            }
            
            // Limit quantity to available stock (unless infinite stock)
            if (shopItem.getStock() > 0) {
                quantity = Math.min(quantity, shopItem.getStock());
            }
            
            if (quantity <= 0) {
                player.sendSystemMessage(Component.literal("§cInvalid quantity"));
                return;
            }
            
            ShopManager shopManager = economyManager.getShopManager();
            ShopManager.BuyResult result = shopManager.buyItem(player, shopItem.getId(), quantity);
            
            if (result.isSuccess()) {
                player.sendSystemMessage(Component.literal("§aSuccessfully purchased " + quantity + "x " + 
                    shopItem.getItemStack().getHoverName().getString()));
                
                // Refresh the GUI to show updated stock
                EnhancedShopInterface.openShop(player, economyManager, mode, targetPlayer, currentPage);
            } else {
                player.sendSystemMessage(Component.literal("§c" + result.getMessage()));
            }
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error buying item", e);
            player.sendSystemMessage(Component.literal("§cFailed to purchase item"));
        }
    }
    
    private void handleSellItem(ShopItem shopItem, int quantity) {
        try {
            if (!shopItem.canSell()) {
                player.sendSystemMessage(Component.literal("§cThis shop does not buy this item"));
                return;
            }
            
            if (shopItem.getSellPrice() == null) {
                player.sendSystemMessage(Component.literal("§cNo sell price set for this item"));
                return;
            }
            
            // Check if player has the required items in their inventory
            ItemStack requiredItem = shopItem.getItemStack().copy();
            int availableCount = 0;
            
            // Count how many of the required item the player has
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stackInSlot = player.getInventory().getItem(i);
                if (ItemStack.isSameItemSameComponents(stackInSlot, requiredItem)) {
                    availableCount += stackInSlot.getCount();
                }
            }
            
            if (availableCount < quantity) {
                player.sendSystemMessage(Component.literal("§cYou don't have enough " + 
                    requiredItem.getHoverName().getString() + " to sell. Need: " + quantity + ", Have: " + availableCount));
                return;
            }
            
            // Sell the items
            ShopManager shopManager = economyManager.getShopManager();
            ShopManager.SellResult result = shopManager.sellItem(player, shopItem.getId(), quantity);
            
            if (result.isSuccess()) {
                player.sendSystemMessage(Component.literal("§aSuccessfully sold " + quantity + "x " + 
                    shopItem.getItemStack().getHoverName().getString() + " for " + 
                    shopItem.getCurrency().format(shopItem.getSellPrice().multiply(java.math.BigDecimal.valueOf(quantity)))));
                
                // Refresh the GUI to show updated stock
                EnhancedShopInterface.openShop(player, economyManager, mode, targetPlayer, currentPage);
            } else {
                player.sendSystemMessage(Component.literal("§c" + result.getMessage()));
            }
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error selling item", e);
            player.sendSystemMessage(Component.literal("§cFailed to sell item"));
        }
    }
    
    private void handleManageItem(ShopItem shopItem) {
        // Open management interface for the player's own shop item
        try {
            player.closeContainer();
            
            // Small delay before opening management interface
            try {
                var server = player.getServer();
                if (server != null) {
                    server.execute(() -> {
                        ShopItemManagementInterface.openItemManagement(player, economyManager, shopItem);
                    });
                } else {
                    ShopItemManagementInterface.openItemManagement(player, economyManager, shopItem);
                }
            } catch (Exception e) {
                NeoEssentials.LOGGER.error("Error opening item management interface", e);
                ShopItemManagementInterface.openItemManagement(player, economyManager, shopItem);
            }
            
            NeoEssentials.LOGGER.info("Opening shop item management for player {} - item: {}", 
                player.getName().getString(), shopItem.getItemStack().getHoverName().getString());
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error managing item", e);
            player.sendSystemMessage(Component.literal("§cFailed to manage item"));
        }
    }
    
    private void handleCreateShopItem() {
        try {
            // Check if player is holding an item
            ItemStack heldItem = player.getMainHandItem();
            
            if (heldItem.isEmpty()) {
                player.sendSystemMessage(Component.literal("§cYou must be holding an item to create a shop listing"));
                return;
            }
            
            // Close current interface and open shop creation interface
            player.closeContainer();
            
            // Small delay before opening creation interface
            try {
                var server = player.getServer();
                if (server != null) {
                    server.execute(() -> {
                        ShopCreationInterface.openShopCreation(player, economyManager);
                    });
                } else {
                    ShopCreationInterface.openShopCreation(player, economyManager);
                }
            } catch (Exception e) {
                NeoEssentials.LOGGER.error("Error opening shop creation interface", e);
                ShopCreationInterface.openShopCreation(player, economyManager);
            }
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error creating shop item", e);
            player.sendSystemMessage(Component.literal("§cFailed to create shop item"));
        }
    }
    
    @Override
    public boolean canTakeItemForPickAll(@Nonnull ItemStack stack, @Nonnull Slot slot) {
        // Prevent taking items from shop container
        return slot.container != this.shopContainer;
    }
}
