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

import java.math.BigDecimal;

/**
 * Interface for editing shop item prices using a custom GUI
 */
public class ShopPriceEditorInterface {
    
    private static final int CONTAINER_SIZE = 27; // 3x9 slots
    
    /**
     * Opens the price editor for a shop item
     */
    public static void openPriceEditor(ServerPlayer player, EconomyManager economyManager, ShopItem shopItem) {
        try {
            SimpleContainer container = new SimpleContainer(CONTAINER_SIZE);
            setupPriceEditorInterface(container, shopItem);
            
            SimpleMenuProvider menuProvider = new SimpleMenuProvider(
                (containerId, inventory, menuPlayer) -> {
                    return new ShopPriceEditorMenu(containerId, inventory, container, player, economyManager, shopItem);
                },
                Component.literal("§6Set New Price: " + shopItem.getItemStack().getHoverName().getString())
            );
            
            player.openMenu(menuProvider);
            
            // Send instructions to player
            player.sendSystemMessage(Component.literal("§e=== Price Editor ==="));
            player.sendSystemMessage(Component.literal("§7Current price: §6" + shopItem.getBuyPrice() + " coins"));
            player.sendSystemMessage(Component.literal("§7Click on price buttons to set new price"));
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to open price editor", e);
            player.sendSystemMessage(Component.literal("§cFailed to open price editor"));
        }
    }
    
    /**
     * Sets up the price editor interface
     */
    private static void setupPriceEditorInterface(SimpleContainer container, ShopItem shopItem) {
        // Clear container
        container.clearContent();
        
        // Show current item and price
        ItemStack displayItem = shopItem.getItemStack().copy();
        displayItem.set(DataComponents.CUSTOM_NAME, 
            Component.literal("§6Current Price: " + shopItem.getBuyPrice() + " coins"));
        container.setItem(4, displayItem); // Top center
        
        // Price adjustment buttons
        setupPriceButtons(container, shopItem.getBuyPrice().doubleValue());
        
        // Confirm and cancel buttons
        ItemStack confirmButton = new ItemStack(Items.LIME_WOOL);
        confirmButton.set(DataComponents.CUSTOM_NAME, Component.literal("§aConfirm New Price"));
        container.setItem(22, confirmButton);
        
        ItemStack cancelButton = new ItemStack(Items.RED_WOOL);
        cancelButton.set(DataComponents.CUSTOM_NAME, Component.literal("§cCancel"));
        container.setItem(18, cancelButton);
        
        // Instructions
        ItemStack instructions = new ItemStack(Items.PAPER);
        instructions.set(DataComponents.CUSTOM_NAME, 
            Component.literal("§eInstructions\n§7Click buttons to adjust price\n§7Click green wool to confirm"));
        container.setItem(0, instructions);
        
        // Fill with glass panes
        ItemStack glassPane = new ItemStack(Items.LIGHT_GRAY_STAINED_GLASS_PANE);
        glassPane.set(DataComponents.CUSTOM_NAME, Component.literal(""));
        
        int[] glassPaneSlots = {1, 2, 3, 5, 6, 7, 8, 19, 20, 21, 23, 24, 25, 26};
        for (int slot : glassPaneSlots) {
            container.setItem(slot, glassPane.copy());
        }
    }
    
    /**
     * Sets up price adjustment buttons
     */
    private static void setupPriceButtons(SimpleContainer container, double currentPrice) {
        // Price adjustment buttons
        double[] adjustments = {-10.0, -1.0, -0.1, 0.1, 1.0, 10.0};
        String[] buttonTexts = {"-10", "-1", "-0.1", "+0.1", "+1", "+10"};
        int[] buttonSlots = {9, 10, 11, 15, 16, 17};
        
        for (int i = 0; i < adjustments.length; i++) {
            double newPrice = Math.max(0.01, currentPrice + adjustments[i]);
            ItemStack button = new ItemStack(adjustments[i] < 0 ? Items.RED_CONCRETE : Items.GREEN_CONCRETE);
            button.set(DataComponents.CUSTOM_NAME, 
                Component.literal("§e" + buttonTexts[i] + " coins\n§7New price: §6" + String.format("%.2f", newPrice)));
            container.setItem(buttonSlots[i], button);
        }
        
        // Current price display
        ItemStack currentPriceDisplay = new ItemStack(Items.GOLD_NUGGET);
        currentPriceDisplay.set(DataComponents.CUSTOM_NAME, 
            Component.literal("§6Current: " + String.format("%.2f", currentPrice) + " coins"));
        container.setItem(13, currentPriceDisplay);
    }
    
    /**
     * Custom menu for price editing
     */
    public static class ShopPriceEditorMenu extends net.minecraft.world.inventory.ChestMenu {
        private final ServerPlayer player;
        private final EconomyManager economyManager;
        private final ShopItem shopItem;
        private final SimpleContainer priceContainer;
        private double currentPrice;
        
        public ShopPriceEditorMenu(int containerId, net.minecraft.world.entity.player.Inventory playerInventory, 
                                 SimpleContainer container, ServerPlayer player, 
                                 EconomyManager economyManager, ShopItem shopItem) {
            super(net.minecraft.world.inventory.MenuType.GENERIC_9x3, containerId, playerInventory, container, 3);
            this.player = player;
            this.economyManager = economyManager;
            this.shopItem = shopItem;
            this.priceContainer = container;
            this.currentPrice = shopItem.getBuyPrice().doubleValue();
        }
        
        @Override
        public boolean stillValid(net.minecraft.world.entity.player.Player menuPlayer) {
            return menuPlayer == player && menuPlayer.isAlive();
        }
        
        @Override
        public void clicked(int slotIndex, int dragType, net.minecraft.world.inventory.ClickType clickType, 
                           net.minecraft.world.entity.player.Player menuPlayer) {
            if (menuPlayer != player) return;
            
            // Handle button clicks
            if (slotIndex < 27) { // Container slots
                handleButtonClick(slotIndex);
                return;
            }
            
            // Allow normal inventory interactions
            super.clicked(slotIndex, dragType, clickType, menuPlayer);
        }
        
        private void handleButtonClick(int slotIndex) {
            try {
                ItemStack clickedItem = priceContainer.getItem(slotIndex);
                if (clickedItem.isEmpty()) return;
                
                String itemName = clickedItem.getHoverName().getString();
                
                // Handle price adjustment buttons
                double[] adjustments = {-10.0, -1.0, -0.1, 0.1, 1.0, 10.0};
                int[] buttonSlots = {9, 10, 11, 15, 16, 17};
                
                for (int i = 0; i < buttonSlots.length; i++) {
                    if (slotIndex == buttonSlots[i]) {
                        adjustPrice(adjustments[i]);
                        return;
                    }
                }
                
                // Handle confirm button
                if (slotIndex == 22 && itemName.contains("Confirm")) {
                    confirmPriceChange();
                    return;
                }
                
                // Handle cancel button
                if (slotIndex == 18 && itemName.contains("Cancel")) {
                    cancelPriceChange();
                    return;
                }
                
            } catch (Exception e) {
                NeoEssentials.LOGGER.error("Error handling price editor click", e);
            }
        }
        
        private void adjustPrice(double adjustment) {
            double newPrice = Math.max(0.01, currentPrice + adjustment);
            if (newPrice != currentPrice) {
                currentPrice = newPrice;
                updatePriceDisplay();
            }
        }
        
        private void updatePriceDisplay() {
            // Update current price display
            ItemStack currentPriceDisplay = new ItemStack(Items.GOLD_NUGGET);
            currentPriceDisplay.set(DataComponents.CUSTOM_NAME, 
                Component.literal("§6New Price: " + String.format("%.2f", currentPrice) + " coins"));
            priceContainer.setItem(13, currentPriceDisplay);
            
            // Update price adjustment buttons with new values
            double[] adjustments = {-10.0, -1.0, -0.1, 0.1, 1.0, 10.0};
            String[] buttonTexts = {"-10", "-1", "-0.1", "+0.1", "+1", "+10"};
            int[] buttonSlots = {9, 10, 11, 15, 16, 17};
            
            for (int i = 0; i < adjustments.length; i++) {
                double newPrice = Math.max(0.01, currentPrice + adjustments[i]);
                ItemStack button = new ItemStack(adjustments[i] < 0 ? Items.RED_CONCRETE : Items.GREEN_CONCRETE);
                button.set(DataComponents.CUSTOM_NAME, 
                    Component.literal("§e" + buttonTexts[i] + " coins\n§7New price: §6" + String.format("%.2f", newPrice)));
                priceContainer.setItem(buttonSlots[i], button);
            }
        }
        
        private void confirmPriceChange() {
            try {
                // Create updated shop item with new price
                ShopItem updatedItem = new ShopItem.Builder()
                    .id(shopItem.getId())
                    .itemStack(shopItem.getItemStack())
                    .type(shopItem.getType())
                    .buyPrice(BigDecimal.valueOf(currentPrice))
                    .sellPrice(shopItem.getSellPrice())
                    .currency(shopItem.getCurrency())
                    .stock(shopItem.getStock())
                    .maxStock(shopItem.getMaxStock())
                    .createdBy(shopItem.getCreatedBy())
                    .createdAt(shopItem.getCreatedAt())
                    .description(shopItem.getDescription())
                    .adminItem(shopItem.isAdminItem())
                    .build();
                
                if (economyManager.getShopManager().addShopItem(updatedItem)) {
                    player.sendSystemMessage(Component.literal("§aUpdated price of " + 
                        shopItem.getItemStack().getHoverName().getString() + " to §6" + currentPrice + " coins"));
                    
                    NeoEssentials.LOGGER.info("Player {} updated shop item price: {} to {} coins", 
                        player.getName().getString(), shopItem.getItemStack().getHoverName().getString(), currentPrice);
                    
                    // Close and return to management interface
                    player.closeContainer();
                    player.getServer().execute(() -> {
                        ShopItemManagementInterface.openItemManagement(player, economyManager, updatedItem);
                    });
                } else {
                    player.sendSystemMessage(Component.literal("§cFailed to update price"));
                }
            } catch (Exception e) {
                NeoEssentials.LOGGER.error("Failed to update shop item price", e);
                player.sendSystemMessage(Component.literal("§cFailed to update price"));
            }
        }
        
        private void cancelPriceChange() {
            player.closeContainer();
            player.getServer().execute(() -> {
                ShopItemManagementInterface.openItemManagement(player, economyManager, shopItem);
            });
        }
        
        @Override
        public boolean canTakeItemForPickAll(ItemStack stack, net.minecraft.world.inventory.Slot slot) {
            return false; // Prevent taking any items
        }
    }
}
