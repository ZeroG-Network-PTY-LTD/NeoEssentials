package com.zerog.neoessentials.economy.gui;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.economy.EconomyManager;
import com.zerog.neoessentials.economy.shop.ShopItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Interface for editing shop item stock through GUI
 */
public class ShopStockEditorInterface {
    
    private static final int CONTAINER_SIZE = 27; // 3x9 slots
    
    /**
     * Opens the stock editor for a shop item
     */
    public static void openStockEditor(ServerPlayer player, EconomyManager economyManager, ShopItem shopItem) {
        try {
            SimpleContainer container = new SimpleContainer(CONTAINER_SIZE);
            setupStockEditorInterface(container, shopItem, player);
            
            SimpleMenuProvider menuProvider = new SimpleMenuProvider(
                (containerId, inventory, menuPlayer) -> {
                    return new ShopStockEditorMenu(containerId, inventory, container, 
                                                 player, economyManager, shopItem);
                },
                Component.literal("§eEdit Stock: " + shopItem.getItemStack().getHoverName().getString())
            );
            
            player.openMenu(menuProvider);
            
            // Send instructions
            player.sendSystemMessage(Component.literal("§e=== Stock Editor ==="));
            player.sendSystemMessage(Component.literal("§7Current stock: §e" + shopItem.getStock()));
            player.sendSystemMessage(Component.literal("§7Place matching items in the slots to add to stock"));
            player.sendSystemMessage(Component.literal("§7Click the green wool to confirm additions"));
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to open stock editor", e);
            player.sendSystemMessage(Component.literal("§cFailed to open stock editor"));
        }
    }
    
    /**
     * Sets up the stock editor interface layout
     */
    private static void setupStockEditorInterface(SimpleContainer container, ShopItem shopItem, ServerPlayer player) {
        // Clear container
        container.clearContent();
        
        // Show current item and stock info
        ItemStack displayItem = shopItem.getItemStack().copy();
        displayItem.set(DataComponents.CUSTOM_NAME, 
            Component.literal("§6Current Item\n§7Stock: §e" + shopItem.getStock()));
        container.setItem(4, displayItem); // Top center
        
        // Add stock confirmation button
        ItemStack confirmButton = new ItemStack(Items.LIME_WOOL);
        confirmButton.set(DataComponents.CUSTOM_NAME, 
            Component.literal("§aConfirm Stock Addition"));
        container.setItem(22, confirmButton); // Bottom center
        
        // Cancel button
        ItemStack cancelButton = new ItemStack(Items.RED_WOOL);
        cancelButton.set(DataComponents.CUSTOM_NAME, 
            Component.literal("§cCancel"));
        container.setItem(18, cancelButton); // Bottom left
        
        // Instructions
        ItemStack instructions = new ItemStack(Items.PAPER);
        instructions.set(DataComponents.CUSTOM_NAME, 
            Component.literal("§eInstructions\n§7Place matching items in empty slots\n§7Click green wool to add to stock"));
        container.setItem(0, instructions); // Top left
        
        // Fill remaining slots with glass panes (except item slots)
        ItemStack glassPane = new ItemStack(Items.LIGHT_GRAY_STAINED_GLASS_PANE);
        glassPane.set(DataComponents.CUSTOM_NAME, Component.literal(""));
        
        int[] glassPaneSlots = {1, 2, 3, 5, 6, 7, 8, 19, 20, 21, 23, 24, 25, 26};
        for (int slot : glassPaneSlots) {
            container.setItem(slot, glassPane.copy());
        }
    }
    
    /**
     * Custom menu for stock editing
     */
    public static class ShopStockEditorMenu extends net.minecraft.world.inventory.ChestMenu {
        private final ServerPlayer player;
        private final EconomyManager economyManager;
        private final ShopItem shopItem;
        private final net.minecraft.world.Container stockContainer;
        
        public ShopStockEditorMenu(int containerId, net.minecraft.world.entity.player.Inventory playerInventory, 
                                  net.minecraft.world.Container container, ServerPlayer player, 
                                  EconomyManager economyManager, ShopItem shopItem) {
            super(net.minecraft.world.inventory.MenuType.GENERIC_9x3, containerId, playerInventory, container, 3);
            this.player = player;
            this.economyManager = economyManager;
            this.shopItem = shopItem;
            this.stockContainer = container;
        }
        
        @Override
        public boolean stillValid(net.minecraft.world.entity.player.Player menuPlayer) {
            return menuPlayer == player && menuPlayer.isAlive();
        }
        
        @Override
        public void clicked(int slotIndex, int dragType, net.minecraft.world.inventory.ClickType clickType, 
                           net.minecraft.world.entity.player.Player menuPlayer) {
            if (menuPlayer != player) return;
            
            if (slotIndex < 27) { // Stock container slots
                handleStockClick(slotIndex, clickType);
                return;
            }
            
            // Allow normal inventory interactions
            super.clicked(slotIndex, dragType, clickType, menuPlayer);
        }
        
        private void handleStockClick(int slotIndex, net.minecraft.world.inventory.ClickType clickType) {
            try {
                ItemStack clickedItem = this.getSlot(slotIndex).getItem();
                
                if (slotIndex == 22) { // Confirm button
                    if (clickedItem.getItem() == Items.LIME_WOOL) {
                        confirmStockAddition();
                    }
                } else if (slotIndex == 18) { // Cancel button
                    if (clickedItem.getItem() == Items.RED_WOOL) {
                        cancelStockEditing();
                    }
                } else if (isItemSlot(slotIndex)) {
                    // Allow item placement in designated slots
                    super.clicked(slotIndex, 0, clickType, player);
                }
            } catch (Exception e) {
                NeoEssentials.LOGGER.error("Error handling stock click", e);
            }
        }
        
        private boolean isItemSlot(int slotIndex) {
            // Item slots are 9-17 (middle row)
            return slotIndex >= 9 && slotIndex <= 17;
        }
        
        private void confirmStockAddition() {
            try {
                int totalItemsToAdd = 0;
                
                // Count matching items in item slots
                for (int i = 9; i <= 17; i++) {
                    ItemStack slotItem = stockContainer.getItem(i);
                    if (!slotItem.isEmpty() && ItemStack.isSameItem(slotItem, shopItem.getItemStack())) {
                        totalItemsToAdd += slotItem.getCount();
                    }
                }
                
                if (totalItemsToAdd == 0) {
                    player.sendSystemMessage(Component.literal("§cNo matching items found to add to stock"));
                    return;
                }
                
                // Update shop item with new stock
                ShopItem updatedItem = shopItem.withStock(shopItem.getStock() + totalItemsToAdd);
                
                if (economyManager.getShopManager().addShopItem(updatedItem)) {
                    // Clear the items from container (they've been added to stock)
                    for (int i = 9; i <= 17; i++) {
                        ItemStack slotItem = stockContainer.getItem(i);
                        if (!slotItem.isEmpty() && ItemStack.isSameItem(slotItem, shopItem.getItemStack())) {
                            stockContainer.setItem(i, ItemStack.EMPTY);
                        }
                    }
                    
                    player.sendSystemMessage(Component.literal("§aAdded " + totalItemsToAdd + 
                        " items to stock. New stock: " + (shopItem.getStock() + totalItemsToAdd)));
                    
                    NeoEssentials.LOGGER.info("Player {} added {} items to shop stock", 
                        player.getName().getString(), totalItemsToAdd);
                    
                    // Close and return to management
                    player.closeContainer();
                    player.getServer().execute(() -> {
                        ShopItemManagementInterface.openItemManagement(player, economyManager, updatedItem);
                    });
                } else {
                    player.sendSystemMessage(Component.literal("§cFailed to update stock"));
                }
            } catch (Exception e) {
                NeoEssentials.LOGGER.error("Error confirming stock addition", e);
                player.sendSystemMessage(Component.literal("§cFailed to add stock"));
            }
        }
        
        private void cancelStockEditing() {
            player.closeContainer();
            player.getServer().execute(() -> {
                ShopItemManagementInterface.openItemManagement(player, economyManager, shopItem);
            });
        }
        
        @Override
        public net.minecraft.world.item.ItemStack quickMoveStack(net.minecraft.world.entity.player.Player menuPlayer, int index) {
            if (menuPlayer != player) return ItemStack.EMPTY;
            
            // Handle shift-click for moving items
            if (index >= 27) { // From player inventory
                ItemStack stackToMove = this.getSlot(index).getItem();
                if (!stackToMove.isEmpty() && ItemStack.isSameItem(stackToMove, shopItem.getItemStack())) {
                    // Try to move to item slots
                    for (int i = 9; i <= 17; i++) {
                        if (stockContainer.getItem(i).isEmpty()) {
                            stockContainer.setItem(i, stackToMove.copy());
                            this.getSlot(index).set(ItemStack.EMPTY);
                            return ItemStack.EMPTY;
                        }
                    }
                }
            }
            
            return ItemStack.EMPTY;
        }
    }
}
