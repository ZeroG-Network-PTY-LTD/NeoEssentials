package com.zerog.neoessentials.economy.gui;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.economy.EconomyManager;
import com.zerog.neoessentials.economy.shop.ShopItem;
import com.zerog.neoessentials.economy.shop.ShopManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import javax.annotation.Nonnull;
import java.math.BigDecimal;

/**
 * Interface for admins to edit shop item prices via GUI
 */
public class AdminPriceEditInterface {
    
    private static final int CONTAINER_SIZE = 27; // 3x9 slots
    
    /**
     * Opens the price editing interface for an admin shop item
     */
    public static void openPriceEdit(ServerPlayer player, EconomyManager economyManager, ShopItem adminItem) {
        try {
            NeoEssentials.LOGGER.info("Opening price edit interface for {}", adminItem.getItemStack().getHoverName().getString());
            
            // Create container
            SimpleContainer container = new SimpleContainer(CONTAINER_SIZE);
            
            // Setup the interface
            setupPriceEditInterface(container, adminItem, economyManager);
            
            // Create menu
            String title = "§4Edit Price: " + adminItem.getItemStack().getHoverName().getString();
            SimpleMenuProvider menuProvider = new SimpleMenuProvider(
                (containerId, inventory, menuPlayer) -> {
                    return new AdminPriceEditMenu(containerId, inventory, container, player, 
                                                economyManager, adminItem);
                },
                Component.literal(title)
            );
            
            player.openMenu(menuProvider);
            
            // Send instructions
            player.sendSystemMessage(Component.literal("§6=== Price Edit Interface ==="));
            player.sendSystemMessage(Component.literal("§7Click on price options to set new prices"));
            player.sendSystemMessage(Component.literal("§7Current Buy Price: " + 
                (adminItem.getBuyPrice() != null ? economyManager.getDefaultCurrency().format(adminItem.getBuyPrice()) : "None")));
            player.sendSystemMessage(Component.literal("§7Current Sell Price: " + 
                (adminItem.getSellPrice() != null ? economyManager.getDefaultCurrency().format(adminItem.getSellPrice()) : "None")));
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error opening price edit interface", e);
            player.sendSystemMessage(Component.literal("§cError opening price edit interface"));
        }
    }
    
    private static void setupPriceEditInterface(SimpleContainer container, ShopItem adminItem, EconomyManager economyManager) {
        // Item display
        ItemStack displayItem = adminItem.getItemStack().copy();
        displayItem.set(DataComponents.CUSTOM_NAME, Component.literal("§4[ADMIN] " + adminItem.getItemStack().getHoverName().getString()));
        container.setItem(4, displayItem);
        
        // Price preset buttons
        double[] presetPrices = {1.0, 5.0, 10.0, 25.0, 50.0, 100.0, 250.0, 500.0, 1000.0};
        
        for (int i = 0; i < presetPrices.length && i < 9; i++) {
            ItemStack priceButton = new ItemStack(Items.GOLD_NUGGET);
            priceButton.set(DataComponents.CUSTOM_NAME, Component.literal("§6Set Price: " + 
                economyManager.getDefaultCurrency().format(BigDecimal.valueOf(presetPrices[i]))));
            container.setItem(i + 9, priceButton);
        }
        
        // Custom price input
        ItemStack customPrice = new ItemStack(Items.ANVIL);
        customPrice.set(DataComponents.CUSTOM_NAME, Component.literal("§eCustom Price (Type in chat)"));
        container.setItem(18, customPrice);
        
        // Type toggles
        ItemStack buyToggle = new ItemStack(Items.EMERALD);
        buyToggle.set(DataComponents.CUSTOM_NAME, Component.literal("§aBuy Mode: " + 
            (adminItem.canBuy() ? "§aEnabled" : "§cDisabled")));
        container.setItem(19, buyToggle);
        
        ItemStack sellToggle = new ItemStack(Items.DIAMOND);
        sellToggle.set(DataComponents.CUSTOM_NAME, Component.literal("§bSell Mode: " + 
            (adminItem.canSell() ? "§aEnabled" : "§cDisabled")));
        container.setItem(20, sellToggle);
        
        // Action buttons
        ItemStack saveButton = new ItemStack(Items.LIME_CONCRETE);
        saveButton.set(DataComponents.CUSTOM_NAME, Component.literal("§aSave Changes"));
        container.setItem(21, saveButton);
        
        ItemStack cancelButton = new ItemStack(Items.RED_CONCRETE);
        cancelButton.set(DataComponents.CUSTOM_NAME, Component.literal("§cCancel"));
        container.setItem(22, cancelButton);
        
        // Back button
        ItemStack backButton = new ItemStack(Items.ARROW);
        backButton.set(DataComponents.CUSTOM_NAME, Component.literal("§7Back to Admin Shop"));
        container.setItem(26, backButton);
        
        // Fill empty slots with glass panes
        for (int i = 0; i < CONTAINER_SIZE; i++) {
            if (container.getItem(i).isEmpty()) {
                ItemStack glassPane = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
                glassPane.set(DataComponents.CUSTOM_NAME, Component.literal(""));
                container.setItem(i, glassPane);
            }
        }
    }
    
    /**
     * Menu for price editing interface
     */
    public static class AdminPriceEditMenu extends ChestMenu {
        
        private final ServerPlayer player;
        private final EconomyManager economyManager;
        private final ShopItem originalItem;
        private final Container editContainer;
        private double currentBuyPrice;
        private double currentSellPrice;
        private ShopItem.Type currentType;
        
        public AdminPriceEditMenu(int containerId, Inventory playerInventory, Container container,
                                 ServerPlayer player, EconomyManager economyManager, ShopItem originalItem) {
            super(MenuType.GENERIC_9x3, containerId, playerInventory, container, 3);
            this.player = player;
            this.economyManager = economyManager;
            this.originalItem = originalItem;
            this.editContainer = container;
            
            // Initialize current values
            this.currentBuyPrice = originalItem.getBuyPrice() != null ? originalItem.getBuyPrice().doubleValue() : 0.0;
            this.currentSellPrice = originalItem.getSellPrice() != null ? originalItem.getSellPrice().doubleValue() : 0.0;
            this.currentType = originalItem.getType();
        }
        
        @Override
        public boolean stillValid(@Nonnull net.minecraft.world.entity.player.Player menuPlayer) {
            return menuPlayer == player && menuPlayer.isAlive() && !menuPlayer.isRemoved();
        }
        
        @Override
        public void clicked(int slotIndex, int dragType, @Nonnull ClickType clickType, @Nonnull net.minecraft.world.entity.player.Player menuPlayer) {
            if (menuPlayer != player) return;
            
            // Prevent taking items from edit container
            if (slotIndex < 27) {
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
                
                String itemName = clickedItem.getHoverName().getString();
                
                // Handle preset price buttons (slots 9-17)
                if (slotIndex >= 9 && slotIndex <= 17) {
                    handlePresetPrice(itemName);
                    return;
                }
                
                // Handle specific buttons
                switch (slotIndex) {
                    case 18: // Custom price
                        handleCustomPrice();
                        break;
                        
                    case 19: // Buy toggle
                        handleBuyToggle();
                        break;
                        
                    case 20: // Sell toggle
                        handleSellToggle();
                        break;
                        
                    case 21: // Save changes
                        handleSaveChanges();
                        break;
                        
                    case 22: // Cancel
                        handleCancel();
                        break;
                        
                    case 26: // Back
                        handleBack();
                        break;
                }
                
            } catch (Exception e) {
                NeoEssentials.LOGGER.error("Error handling price edit click", e);
                player.sendSystemMessage(Component.literal("§cAn error occurred while processing your click"));
            }
        }
        
        private void handlePresetPrice(String itemName) {
            try {
                // Extract price from item name
                String priceStr = itemName.replace("Set Price: ", "").replace("$", "").replace(",", "");
                double price = Double.parseDouble(priceStr);
                
<<<<<<< HEAD
                if (price <= 0) {
                    player.sendSystemMessage(Component.literal("§cPrice must be greater than 0"));
                    return;
                }
                
                if (price > 1000000) {
                    player.sendSystemMessage(Component.literal("§cPrice cannot exceed 1,000,000"));
                    return;
                }
                
                // Set price based on current type
                boolean priceSet = false;
                if (currentType == ShopItem.Type.BUY || currentType == ShopItem.Type.BOTH) {
                    currentBuyPrice = price;
                    priceSet = true;
                }
                if (currentType == ShopItem.Type.SELL || currentType == ShopItem.Type.BOTH) {
                    currentSellPrice = price;
                    priceSet = true;
                }
                
                if (priceSet) {
                    player.sendSystemMessage(Component.literal("§aSet price to " + 
                        economyManager.getDefaultCurrency().format(BigDecimal.valueOf(price))));
                    
                    // Update the interface display
                    updatePriceDisplay();
                } else {
                    player.sendSystemMessage(Component.literal("§cNo valid price type selected"));
                }
                
            } catch (NumberFormatException e) {
                player.sendSystemMessage(Component.literal("§cInvalid price format"));
                NeoEssentials.LOGGER.warn("Admin {} provided invalid price format: {}", 
                    player.getName().getString(), itemName);
            } catch (Exception e) {
                NeoEssentials.LOGGER.error("Error handling preset price", e);
                player.sendSystemMessage(Component.literal("§cError setting price"));
=======
                // Set price based on current type
                if (currentType == ShopItem.Type.BUY || currentType == ShopItem.Type.BOTH) {
                    currentBuyPrice = price;
                }
                if (currentType == ShopItem.Type.SELL || currentType == ShopItem.Type.BOTH) {
                    currentSellPrice = price;
                }
                
                player.sendSystemMessage(Component.literal("§aSet price to " + 
                    economyManager.getDefaultCurrency().format(BigDecimal.valueOf(price))));
                
            } catch (NumberFormatException e) {
                player.sendSystemMessage(Component.literal("§cInvalid price format"));
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
            }
        }
        
        private void handleCustomPrice() {
            player.sendSystemMessage(Component.literal("§eType the custom price in chat"));
            player.sendSystemMessage(Component.literal("§7Example: 123.45"));
            // Note: This would need a chat handler system to be fully implemented
        }
        
        private void handleBuyToggle() {
            currentType = switch (currentType) {
                case BUY -> ShopItem.Type.SELL;
                case SELL -> ShopItem.Type.BOTH;
                case BOTH -> ShopItem.Type.SELL;
            };
            
            player.sendSystemMessage(Component.literal("§aToggled to " + currentType));
            updateInterface();
        }
        
        private void handleSellToggle() {
            currentType = switch (currentType) {
                case BUY -> ShopItem.Type.BOTH;
                case SELL -> ShopItem.Type.BUY;
                case BOTH -> ShopItem.Type.BUY;
            };
            
            player.sendSystemMessage(Component.literal("§aToggled to " + currentType));
            updateInterface();
        }
        
        private void handleSaveChanges() {
            try {
<<<<<<< HEAD
                // Validate economy system is still enabled
                if (!economyManager.isEnabled()) {
                    player.sendSystemMessage(Component.literal("§cEconomy system is disabled"));
                    return;
                }
                
                // Validate prices
                if (currentType == ShopItem.Type.BUY || currentType == ShopItem.Type.BOTH) {
                    if (currentBuyPrice <= 0) {
                        player.sendSystemMessage(Component.literal("§cBuy price must be greater than 0"));
                        return;
                    }
                    if (currentBuyPrice > 1000000) {
                        player.sendSystemMessage(Component.literal("§cBuy price cannot exceed 1,000,000"));
                        return;
                    }
                }
                
                if (currentType == ShopItem.Type.SELL || currentType == ShopItem.Type.BOTH) {
                    if (currentSellPrice <= 0) {
                        player.sendSystemMessage(Component.literal("§cSell price must be greater than 0"));
                        return;
                    }
                    if (currentSellPrice > 1000000) {
                        player.sendSystemMessage(Component.literal("§cSell price cannot exceed 1,000,000"));
                        return;
                    }
                }
                
                // Validate buy price isn't lower than sell price for admin items
                if (currentType == ShopItem.Type.BOTH) {
                    if (currentBuyPrice < currentSellPrice) {
                        player.sendSystemMessage(Component.literal("§cBuy price should be higher than sell price"));
                        player.sendSystemMessage(Component.literal("§7Buy: " + currentBuyPrice + ", Sell: " + currentSellPrice));
                        return;
                    }
                }
                
                ShopManager shopManager = economyManager.getShopManager();
                
                // Create updated item with validation
                ShopItem.Builder builder = new ShopItem.Builder()
                    .id(originalItem.getId())
                    .itemStack(originalItem.getItemStack())
                    .type(currentType)
=======
                ShopManager shopManager = economyManager.getShopManager();
                
                // Create updated item
                ShopItem updatedItem = new ShopItem.Builder()
                    .id(originalItem.getId())
                    .itemStack(originalItem.getItemStack())
                    .type(currentType)
                    .buyPrice(currentType == ShopItem.Type.BUY || currentType == ShopItem.Type.BOTH ? 
                        BigDecimal.valueOf(currentBuyPrice) : null)
                    .sellPrice((currentType == ShopItem.Type.SELL || currentType == ShopItem.Type.BOTH) && currentSellPrice > 0 ? 
                        BigDecimal.valueOf(currentSellPrice) : null)
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
                    .currency(originalItem.getCurrency())
                    .stock(originalItem.getStock())
                    .maxStock(originalItem.getMaxStock())
                    .createdBy(originalItem.getCreatedBy())
                    .createdAt(originalItem.getCreatedAt())
                    .description(originalItem.getDescription())
<<<<<<< HEAD
                    .adminItem(originalItem.isAdminItem());
                
                // Set prices based on type
                if (currentType == ShopItem.Type.BUY || currentType == ShopItem.Type.BOTH) {
                    builder.buyPrice(BigDecimal.valueOf(currentBuyPrice));
                }
                if (currentType == ShopItem.Type.SELL || currentType == ShopItem.Type.BOTH) {
                    builder.sellPrice(BigDecimal.valueOf(currentSellPrice));
                }
                
                ShopItem updatedItem = builder.build();
                
                // Validate the created item
                if (!validateUpdatedItem(updatedItem)) {
                    player.sendSystemMessage(Component.literal("§cItem validation failed"));
                    return;
                }
                
                // Update in shop atomically
                if (shopManager.removeShopItem(originalItem.getId())) {
                    if (shopManager.safeAddShopItem(updatedItem)) {
                        player.sendSystemMessage(Component.literal("§aSuccessfully updated " + 
                            originalItem.getItemStack().getHoverName().getString()));
                        
                        // Log the change
                        NeoEssentials.LOGGER.info("Admin {} updated item {} - Type: {}, Buy: {}, Sell: {}", 
                            player.getName().getString(), 
                            originalItem.getItemStack().getHoverName().getString(),
                            currentType,
                            currentType == ShopItem.Type.BUY || currentType == ShopItem.Type.BOTH ? 
                                economyManager.getDefaultCurrency().format(BigDecimal.valueOf(currentBuyPrice)) : "N/A",
                            currentType == ShopItem.Type.SELL || currentType == ShopItem.Type.BOTH ? 
                                economyManager.getDefaultCurrency().format(BigDecimal.valueOf(currentSellPrice)) : "N/A");
                        
                        // Close and return to admin shop
                        player.closeContainer();
                        player.getServer().execute(() -> {
                            try {
                                AdminShopManagementInterface.openAdminShopManagement(player, economyManager);
                            } catch (Exception e) {
                                NeoEssentials.LOGGER.error("Error opening admin shop management", e);
                            }
                        });
                    } else {
                        // Failed to add updated item, try to restore original
                        shopManager.safeAddShopItem(originalItem);
                        player.sendSystemMessage(Component.literal("§cFailed to update item - changes reverted"));
                    }
                } else {
                    player.sendSystemMessage(Component.literal("§cFailed to update item - original item not found"));
                }
                
            } catch (Exception e) {
                NeoEssentials.LOGGER.error("Error saving changes for admin item", e);
                player.sendSystemMessage(Component.literal("§cError saving changes: " + e.getMessage()));
            }
        }
        
        /**
         * Validates the updated item before saving
         */
        private boolean validateUpdatedItem(ShopItem item) {
            if (item == null) return false;
            if (item.getItemStack() == null || item.getItemStack().isEmpty()) return false;
            if (item.getBuyPrice() != null && item.getBuyPrice().compareTo(BigDecimal.ZERO) <= 0) return false;
            if (item.getSellPrice() != null && item.getSellPrice().compareTo(BigDecimal.ZERO) <= 0) return false;
            
            return true;
        }
        
        /**
         * Updates the price display in the interface
         */
        private void updatePriceDisplay() {
            // This could be implemented to show current prices in the interface
            // For now, we'll just update the toggle buttons
            updateInterface();
        }
        
=======
                    .adminItem(originalItem.isAdminItem())
                    .build();
                
                // Update in shop
                if (shopManager.removeShopItem(originalItem.getId()) && shopManager.addShopItem(updatedItem)) {
                    player.sendSystemMessage(Component.literal("§aSuccessfully updated " + 
                        originalItem.getItemStack().getHoverName().getString()));
                    
                    // Close and return to admin shop
                    player.closeContainer();
                    AdminShopManagementInterface.openAdminShopManagement(player, economyManager);
                } else {
                    player.sendSystemMessage(Component.literal("§cFailed to update item"));
                }
                
            } catch (Exception e) {
                NeoEssentials.LOGGER.error("Error saving changes", e);
                player.sendSystemMessage(Component.literal("§cError saving changes"));
            }
        }
        
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
        private void handleCancel() {
            player.closeContainer();
            AdminShopManagementInterface.openAdminShopManagement(player, economyManager);
        }
        
        private void handleBack() {
            handleCancel();
        }
        
        private void updateInterface() {
            // Update toggle buttons
            ItemStack buyToggle = new ItemStack(Items.EMERALD);
            buyToggle.set(DataComponents.CUSTOM_NAME, Component.literal("§aBuy Mode: " + 
                (currentType == ShopItem.Type.BUY || currentType == ShopItem.Type.BOTH ? "§aEnabled" : "§cDisabled")));
            editContainer.setItem(19, buyToggle);
            
            ItemStack sellToggle = new ItemStack(Items.DIAMOND);
            sellToggle.set(DataComponents.CUSTOM_NAME, Component.literal("§bSell Mode: " + 
                (currentType == ShopItem.Type.SELL || currentType == ShopItem.Type.BOTH ? "§aEnabled" : "§cDisabled")));
            editContainer.setItem(20, sellToggle);
        }
        
        @Override
        public boolean canTakeItemForPickAll(@Nonnull ItemStack stack, @Nonnull net.minecraft.world.inventory.Slot slot) {
            // Prevent taking items from edit container
            return slot.container != this.editContainer;
        }
    }
}
