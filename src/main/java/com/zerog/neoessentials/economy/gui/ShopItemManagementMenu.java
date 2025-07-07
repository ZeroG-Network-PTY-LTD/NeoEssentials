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

/**
 * Menu class for handling shop item management interactions
 */
public class ShopItemManagementMenu extends ChestMenu {
    
    private final ServerPlayer player;
    private final EconomyManager economyManager;
    private final ShopItem shopItem;
    private final Container managementContainer;
    
    public ShopItemManagementMenu(int containerId, Inventory playerInventory, Container container, 
                                ServerPlayer player, EconomyManager economyManager, ShopItem shopItem) {
        super(MenuType.GENERIC_9x5, containerId, playerInventory, container, 5);
        this.player = player;
        this.economyManager = economyManager;
        this.shopItem = shopItem;
        this.managementContainer = container;
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
        
        // Prevent taking items from management slots
        if (slotIndex < 45) { // Management container slots
            handleClick(slotIndex, clickType);
            return;
        }
        
        // Allow normal inventory interactions
        super.clicked(slotIndex, dragType, clickType, menuPlayer);
    }
    
    /**
     * Handles clicks on management interface elements
     */
    private void handleClick(int slotIndex, ClickType clickType) {
        try {
            ItemStack clickedItem = this.getSlot(slotIndex).getItem();
            
            if (clickedItem.isEmpty()) return;
            
            String itemName = clickedItem.getHoverName().getString();
            
            switch (slotIndex) {
                case 19: // Edit Price
                    if (itemName.contains("Edit Price")) {
                        handleEditPrice();
                    }
                    break;
                    
                case 21: // Edit Stock
                    if (itemName.contains("Edit Stock")) {
                        handleEditStock();
                    }
                    break;
                    
                case 23: // Remove Listing
                    if (itemName.contains("Remove Listing")) {
                        handleRemoveListing();
                    }
                    break;
                    
                case 25: // Retrieve Items
                    if (itemName.contains("Retrieve Items")) {
                        handleRetrieveItems();
                    }
                    break;
                    
                case 36: // Back to shop
                    if (itemName.contains("Back to My Shop")) {
                        handleBackToShop();
                    }
                    break;
                    
                case 44: // Close
                    if (itemName.contains("Close")) {
                        player.closeContainer();
                    }
                    break;
                    
                default:
                    // Ignore clicks on other slots
                    break;
            }
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error handling management click", e);
            player.sendSystemMessage(Component.literal("§cAn error occurred while processing your click"));
        }
    }
    
    /**
     * Handles editing the price of the shop item
     */
    private void handleEditPrice() {
        try {
            // For now, show instructions for using commands
            player.sendSystemMessage(Component.literal("§ePrice editing via GUI coming soon!"));
            player.sendSystemMessage(Component.literal("§7Current price: §6" + shopItem.getBuyPrice() + " coins"));
            player.sendSystemMessage(Component.literal("§7Use command: §f/shop editprice " + 
                shopItem.getId().toString().substring(0, 8) + " <new_price>"));
            
            // Close the management interface
            player.closeContainer();
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error editing price", e);
            player.sendSystemMessage(Component.literal("§cFailed to edit price"));
        }
    }
    
    /**
     * Handles editing the stock of the shop item
     */
    private void handleEditStock() {
        try {
            // For now, show instructions for using commands
            player.sendSystemMessage(Component.literal("§eStock editing via GUI coming soon!"));
            player.sendSystemMessage(Component.literal("§7Current stock: §e" + shopItem.getStock()));
            player.sendSystemMessage(Component.literal("§7Use command: §f/shop addstock " + 
                shopItem.getId().toString().substring(0, 8) + " <quantity>"));
            player.sendSystemMessage(Component.literal("§7Hold items in your hand to add them to stock"));
            
            // Close the management interface
            player.closeContainer();
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error editing stock", e);
            player.sendSystemMessage(Component.literal("§cFailed to edit stock"));
        }
    }
    
    /**
     * Handles removing the shop listing
     */
    private void handleRemoveListing() {
        try {
            ShopManager shopManager = economyManager.getShopManager();
            
            if (shopManager.removeShopItem(shopItem.getId())) {
                // Return any remaining stock to player
                if (shopItem.getStock() > 0) {
                    ItemStack returnItems = shopItem.getItemStack().copy();
                    returnItems.setCount(shopItem.getStock());
                    
                    if (player.getInventory().add(returnItems)) {
                        player.sendSystemMessage(Component.literal("§aShop listing removed and " + 
                            shopItem.getStock() + " items returned to your inventory"));
                    } else {
                        // If inventory is full, drop items
                        player.drop(returnItems, false);
                        player.sendSystemMessage(Component.literal("§aShop listing removed and " + 
                            shopItem.getStock() + " items dropped (inventory full)"));
                    }
                } else {
                    player.sendSystemMessage(Component.literal("§aShop listing removed successfully"));
                }
                
                NeoEssentials.LOGGER.info("Player {} removed shop listing for item {}", 
                    player.getName().getString(), shopItem.getItemStack().getHoverName().getString());
                
                // Close interface and return to personal shop
                player.closeContainer();
                
                // Small delay before opening personal shop
                player.getServer().execute(() -> {
                    EnhancedShopInterface.openPersonalShop(player, economyManager);
                });
                
            } else {
                player.sendSystemMessage(Component.literal("§cFailed to remove shop listing"));
            }
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error removing listing", e);
            player.sendSystemMessage(Component.literal("§cFailed to remove listing"));
        }
    }
    
    /**
     * Handles retrieving items from the shop listing without removing it
     */
    private void handleRetrieveItems() {
        try {
            if (shopItem.getStock() <= 0) {
                player.sendSystemMessage(Component.literal("§cNo items in stock to retrieve"));
                return;
            }
            
            ShopManager shopManager = economyManager.getShopManager();
            
            // Create updated shop item with 0 stock
            ShopItem updatedItem = shopItem.withStock(0);
            
            // Update the shop item
            if (shopManager.addShopItem(updatedItem)) { // This will replace the existing item
                // Give items to player
                ItemStack retrievedItems = shopItem.getItemStack().copy();
                retrievedItems.setCount(shopItem.getStock());
                
                if (player.getInventory().add(retrievedItems)) {
                    player.sendSystemMessage(Component.literal("§aRetrieved " + shopItem.getStock() + 
                        " items from your shop listing"));
                } else {
                    // If inventory is full, drop items
                    player.drop(retrievedItems, false);
                    player.sendSystemMessage(Component.literal("§aRetrieved " + shopItem.getStock() + 
                        " items (dropped due to full inventory)"));
                }
                
                NeoEssentials.LOGGER.info("Player {} retrieved {} items from shop listing", 
                    player.getName().getString(), shopItem.getStock());
                
                // Close interface and return to personal shop  
                player.closeContainer();
                
                // Small delay before opening personal shop to show updated stock
                player.getServer().execute(() -> {
                    EnhancedShopInterface.openPersonalShop(player, economyManager);
                });
                
            } else {
                player.sendSystemMessage(Component.literal("§cFailed to retrieve items"));
            }
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error retrieving items", e);
            player.sendSystemMessage(Component.literal("§cFailed to retrieve items"));
        }
    }
    
    /**
     * Handles returning to the personal shop
     */
    private void handleBackToShop() {
        try {
            player.closeContainer();
            
            // Small delay before opening personal shop
            player.getServer().execute(() -> {
                EnhancedShopInterface.openPersonalShop(player, economyManager);
            });
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error returning to shop", e);
            player.sendSystemMessage(Component.literal("§cFailed to return to shop"));
        }
    }
    
    @Override
    public boolean canTakeItemForPickAll(@Nonnull ItemStack stack, @Nonnull Slot slot) {
        // Prevent taking items from management container
        return slot.container != this.managementContainer;
    }
}
