package com.zerog.neoessentials.ui.shop;

import com.zerog.neoessentials.economy.Shop;
import com.zerog.neoessentials.economy.ShopManager;
import com.zerog.neoessentials.util.LanguageUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Container menu for shop inventory management interface
 */
public class ShopInventoryMenu extends AbstractContainerMenu {
    
    private final Container container;
    private final Shop shop;
    private final ShopManager shopManager;
    
    public ShopInventoryMenu(int containerId, Inventory playerInventory, Container container, 
                           Shop shop, ShopManager shopManager) {
        super(MenuType.GENERIC_9x6, containerId);
        this.container = container;
        this.shop = shop;
        this.shopManager = shopManager;
        
        // Add container slots
        for (int row = 0; row < 6; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(container, col + row * 9, 8 + col * 18, 18 + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false; // GUI items cannot be moved
                    }
                });
            }
        }
        
        // Add player inventory slots
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18 + 36));
            }
        }
        
        // Add player hotbar slots
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142 + 36));
        }
    }
    
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY; // Disable shift-clicking for now
    }
    
    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }
    
    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < container.getContainerSize()) {
            ItemStack clickedItem = container.getItem(slotId);
            
            // Check if the item has an action
            if (clickedItem.has(DataComponents.CUSTOM_DATA)) {
                CustomData customData = clickedItem.get(DataComponents.CUSTOM_DATA);
                if (customData != null) {
                    CompoundTag tag = customData.copyTag();
                    if (tag.contains("Action")) {
                        handleActionClick(tag.getString("Action"), tag, (ServerPlayer) player, clickType);
                        return;
                    }
                }
            }
        }
        
        // For other slots (like player inventory), allow normal behavior
        if (slotId >= container.getContainerSize()) {
            super.clicked(slotId, button, clickType, player);
        }
    }
    
    /**
     * Handles action clicks from GUI items
     */
    private void handleActionClick(String action, CompoundTag actionData, ServerPlayer player, ClickType clickType) {
        switch (action) {
            case "back_to_main":
                new ShopManagementGUI(shop, shopManager).openMainMenu(player);
                break;
                
            case "add_items":
                openAddItemsInterface(player);
                break;
                
            case "remove_all_items":
                if (shop.getOwnerId().equals(player.getUUID())) {
                    removeAllItems(player);
                } else {
                    LanguageUtil.sendMessage(player, "§cOnly the shop owner can remove all items!");
                }
                break;
                
            case "modify_item":
                String itemId = actionData.getString("ItemId");
                modifyShopItem(player, itemId, clickType);
                break;
                
            default:
                LanguageUtil.sendMessage(player, "§cUnknown action: " + action);
                break;
        }
    }
    
    /**
     * Opens interface for adding items from player inventory
     */
    private void openAddItemsInterface(ServerPlayer player) {
        // For now, we'll use a simple message. Later we can implement a more sophisticated interface
        LanguageUtil.sendMessage(player, "§ePlace items in your hotbar, then use §6/shop additem <shopname> <amount> <buy-price> <sell-price>");
        LanguageUtil.sendMessage(player, "§7Example: §f/shop additem " + shop.getShopName() + " 64 10.0 8.0");
        player.closeContainer();
    }
    
    /**
     * Removes all items from the shop and returns them to player
     */
    private void removeAllItems(ServerPlayer player) {
        try {
            int itemsRemoved = 0;
            var shopItems = shop.getInventory();
            
            for (String itemId : shopItems.keySet()) {
                Shop.ShopItem shopItem = shopItems.get(itemId);
                if (shopItem != null && shopItem.getQuantity() > 0) {
                    // Create item stack for the full stock
                    net.minecraft.world.item.Item item = com.zerog.neoessentials.economy.ItemHandler.getItemFromId(shopItem.getItemId());
                    if (item != null) {
                        ItemStack itemStack = new ItemStack(item, shopItem.getQuantity());
                        
                        // Try to add to player inventory
                        if (player.getInventory().add(itemStack)) {
                            // Successfully added, remove from shop
                            shop.removeItem(itemId, shopItem.getQuantity());
                            itemsRemoved++;
                        } else {
                            // Inventory full, drop items
                            player.drop(itemStack, false);
                            shop.removeItem(itemId, shopItem.getQuantity());
                            itemsRemoved++;
                        }
                    }
                }
            }
            
            if (itemsRemoved > 0) {
                MessageUtil.sendSuccessMessage(player, "Removed " + itemsRemoved + " item types from shop inventory.");
                
                // Save shop changes
                if (shopManager != null) {
                    shopManager.saveShop(shop);
                }
                
                // Refresh the GUI
                new ShopInventoryGUI(shop, shopManager).openInventoryMenu(player);
            } else {
                LanguageUtil.sendMessage(player, "§7No items to remove from shop.");
            }
            
        } catch (Exception e) {
            MessageUtil.sendErrorMessage(player, "Failed to remove items: " + e.getMessage());
        }
    }
    
    /**
     * Modifies a shop item based on click type
     */
    private void modifyShopItem(ServerPlayer player, String itemId, ClickType clickType) {
        try {
            Shop.ShopItem shopItem = shop.getInventory().get(itemId);
            if (shopItem == null) {
                LanguageUtil.sendMessage(player, "§cItem not found in shop!");
                return;
            }
            
            int removeAmount = 1; // Default
            
            switch (clickType) {
                case PICKUP: // Left click
                    removeAmount = 1;
                    break;
                case QUICK_MOVE: // Shift click
                    removeAmount = shopItem.getQuantity(); // Remove all
                    break;
                case PICKUP_ALL: // Right click
                    removeAmount = Math.min(shopItem.getQuantity(), 64); // Remove stack
                    break;
                default:
                    removeAmount = 1;
                    break;
            }
            
            if (removeAmount > shopItem.getQuantity()) {
                removeAmount = shopItem.getQuantity();
            }
            
            if (removeAmount <= 0) {
                LanguageUtil.sendMessage(player, "§7No stock to remove.");
                return;
            }
            
            // Create item stack to return to player
            net.minecraft.world.item.Item item = com.zerog.neoessentials.economy.ItemHandler.getItemFromId(shopItem.getItemId());
            if (item != null) {
                ItemStack returnStack = new ItemStack(item, removeAmount);
                
                // Try to add to player inventory
                if (player.getInventory().add(returnStack)) {
                    // Successfully added, remove from shop
                    shop.removeItem(itemId, removeAmount);
                    LanguageUtil.sendMessage(player, "§aRemoved §f" + removeAmount + "x " + 
                        com.zerog.neoessentials.economy.ItemHandler.formatItemName(itemId) + " §afrom shop.");
                } else {
                    // Inventory full, drop items
                    player.drop(returnStack, false);
                    shop.removeItem(itemId, removeAmount);
                    LanguageUtil.sendMessage(player, "§eInventory full! Dropped §f" + removeAmount + "x " + 
                        com.zerog.neoessentials.economy.ItemHandler.formatItemName(itemId) + " §eon the ground.");
                }
                
                // Save shop changes
                if (shopManager != null) {
                    shopManager.saveShop(shop);
                }
                
                // Refresh the GUI
                new ShopInventoryGUI(shop, shopManager).openInventoryMenu(player);
            }
            
        } catch (Exception e) {
            MessageUtil.sendErrorMessage(player, "Failed to modify item: " + e.getMessage());
        }
    }
}
