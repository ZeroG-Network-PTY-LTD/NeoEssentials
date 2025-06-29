package com.zerog.neoessentials.ui.shop;

import com.zerog.neoessentials.economy.Shop;
import com.zerog.neoessentials.utils.MessageUtil;
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
 * Container menu for shop management interface
 */
public class ShopManagementMenu extends AbstractContainerMenu {
    
    private final Container container;
    private final Shop shop;
    
    public ShopManagementMenu(int containerId, Inventory playerInventory, Container container, Shop shop) {
        super(MenuType.GENERIC_9x6, containerId);
        this.container = container;
        this.shop = shop;
        
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
        return ItemStack.EMPTY; // Disable shift-clicking
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
                        handleActionClick(tag.getString("Action"), (ServerPlayer) player);
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
    
    @Override
    public boolean clickMenuButton(Player player, int button) {
        ItemStack stackInSlot = container.getItem(button);
        
        // Check if the item has an action
        if (stackInSlot.has(DataComponents.CUSTOM_DATA)) {
            CustomData customData = stackInSlot.get(DataComponents.CUSTOM_DATA);
            if (customData != null) {
                CompoundTag tag = customData.copyTag();
                if (tag.contains("Action")) {
                    handleActionClick(tag.getString("Action"), (ServerPlayer) player);
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Handles action clicks from GUI items
     */
    private void handleActionClick(String action, ServerPlayer player) {
        switch (action) {
            case "toggle_status":
                if (shop.getOwnerId().equals(player.getUUID())) {
                    if (shop.isActive()) {
                        shop.setActive(false);
                        MessageUtil.sendMessage(player, "§cShop has been deactivated.");
                    } else {
                        shop.setActive(true);
                        MessageUtil.sendMessage(player, "§aShop has been activated.");
                    }
                    
                    // Refresh the GUI
                    var gui = new ShopManagementGUI(shop, null);
                    gui.openMainMenu(player);
                } else {
                    MessageUtil.sendMessage(player, "§cOnly the shop owner can change the shop status!");
                }
                break;
                
            case "inventory_management":
                MessageUtil.sendMessage(player, "§eInventory management interface coming soon!");
                break;
                
            case "pricing_management":
                MessageUtil.sendMessage(player, "§ePricing management interface coming soon!");
                break;
                
            case "employee_management":
                MessageUtil.sendMessage(player, "§eEmployee management interface coming soon!");
                break;
                
            case "view_statistics":
                MessageUtil.sendMessage(player, "§eStatistics interface coming soon!");
                break;
                
            default:
                MessageUtil.sendMessage(player, "§cUnknown action: " + action);
                break;
        }
    }
    
    /**
     * Custom slot that prevents placing items
     */
    private static class GUISlot extends Slot {
        public GUISlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }
        
        @Override
        public boolean mayPlace(ItemStack stack) {
            return false; // GUI items cannot be moved
        }
        
        @Override
        public boolean mayPickup(Player player) {
            return false; // GUI items cannot be picked up
        }
    }
}
