package com.zerog.neoessentials.ui.shop;

import com.zerog.neoessentials.economy.Shop;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Custom menu for shop management interface that handles click events
 * and provides enhanced functionality for shop owners.
 */
public class ShopManagementMenu extends AbstractContainerMenu {
    
    private final Container container;
    private final Shop shop;
    private final int containerRows;

    public ShopManagementMenu(int containerId, Inventory playerInventory, Container container, Shop shop) {
        super(MenuType.GENERIC_9x6, containerId);
        this.container = container;
        this.shop = shop;
        this.containerRows = 6;

        // Add container slots
        for (int row = 0; row < this.containerRows; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new ShopManagementSlot(container, row * 9 + col, 8 + col * 18, 18 + row * 18));
            }
        }

        // Add player inventory slots
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 103 + row * 18 + this.containerRows * 18 - 108));
            }
        }

        // Add player hotbar slots
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 161 + this.containerRows * 18 - 108));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < this.container.getContainerSize()) {
            ItemStack clickedItem = this.container.getItem(slotId);
            
            if (clickedItem.hasTag() && clickedItem.getTag().contains("Action")) {
                handleActionClick(clickedItem.getTag().getString("Action"), player);
                return; // Don't allow item to be moved
            }
        }
        
        // Only allow normal inventory interactions for non-action items
        if (slotId >= this.container.getContainerSize()) {
            super.clicked(slotId, button, clickType, player);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        
        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            
            // Don't allow moving action items
            if (stackInSlot.hasTag() && stackInSlot.getTag().contains("Action")) {
                return ItemStack.EMPTY;
            }
            
            itemstack = stackInSlot.copy();
            
            if (index < this.containerRows * 9) {
                if (!this.moveItemStackTo(stackInSlot, this.containerRows * 9, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stackInSlot, 0, this.containerRows * 9, false)) {
                return ItemStack.EMPTY;
            }

            if (stackInSlot.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemstack;
    }

    /**
     * Handles action button clicks in the shop management interface.
     */
    private void handleActionClick(String action, Player player) {
        // Get the shop management GUI instance to handle actions
        var economyManager = com.zerog.neoessentials.economy.EconomyManager.getInstance();
        var gui = new ShopManagementGUI(economyManager);
        
        switch (action) {
            case "inventory_management":
                player.closeContainer();
                gui.openShopInventoryGUI((net.minecraft.server.level.ServerPlayer) player, shop.getName());
                break;
                
            case "pricing_management":
                player.closeContainer();
                // TODO: Open pricing management interface
                com.zerog.neoessentials.utils.MessageUtil.sendMessage(
                    (net.minecraft.server.level.ServerPlayer) player, 
                    "§6Pricing management coming soon! Use §e/shop price §6commands for now."
                );
                break;
                
            case "employee_management":
                player.closeContainer();
                // TODO: Open employee management interface
                com.zerog.neoessentials.utils.MessageUtil.sendMessage(
                    (net.minecraft.server.level.ServerPlayer) player, 
                    "§6Employee management coming soon! Use §e/shop employee §6commands for now."
                );
                break;
                
            case "toggle_shop":
                try {
                    if (shop.isOpen()) {
                        shop.closeShop();
                        com.zerog.neoessentials.utils.MessageUtil.sendMessage(
                            (net.minecraft.server.level.ServerPlayer) player, 
                            "§cShop §e" + shop.getName() + " §chas been closed."
                        );
                    } else {
                        shop.openShop();
                        com.zerog.neoessentials.utils.MessageUtil.sendMessage(
                            (net.minecraft.server.level.ServerPlayer) player, 
                            "§aShop §e" + shop.getName() + " §ais now open for business!"
                        );
                    }
                    // Refresh the GUI
                    player.closeContainer();
                    gui.openShopManagementGUI((net.minecraft.server.level.ServerPlayer) player, shop.getName());
                } catch (Exception e) {
                    com.zerog.neoessentials.utils.MessageUtil.sendErrorMessage(
                        (net.minecraft.server.level.ServerPlayer) player, 
                        "Failed to toggle shop status: " + e.getMessage()
                    );
                }
                break;
                
            case "restock_all":
                // TODO: Implement restock all functionality
                com.zerog.neoessentials.utils.MessageUtil.sendMessage(
                    (net.minecraft.server.level.ServerPlayer) player, 
                    "§6Restock functionality coming soon!"
                );
                break;
                
            case "clear_sold_out":
                // TODO: Implement clear sold out functionality
                com.zerog.neoessentials.utils.MessageUtil.sendMessage(
                    (net.minecraft.server.level.ServerPlayer) player, 
                    "§6Clear sold out functionality coming soon!"
                );
                break;
                
            case "back":
                player.closeContainer();
                gui.openShopListGUI((net.minecraft.server.level.ServerPlayer) player);
                break;
                
            case "close":
                player.closeContainer();
                break;
                
            default:
                com.zerog.neoessentials.utils.MessageUtil.sendMessage(
                    (net.minecraft.server.level.ServerPlayer) player, 
                    "§cUnknown action: " + action
                );
                break;
        }
    }

    /**
     * Custom slot class for shop management items that prevents moving action items.
     */
    private static class ShopManagementSlot extends Slot {
        public ShopManagementSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPickup(Player player) {
            ItemStack stack = this.getItem();
            // Don't allow picking up action items (UI elements)
            return !(stack.hasTag() && stack.getTag().contains("Action"));
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            // Don't allow placing items in action slots
            ItemStack currentStack = this.getItem();
            return !(currentStack.hasTag() && currentStack.getTag().contains("Action"));
        }
    }
}
