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
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Interface for admins to create new admin shop items
 */
public class AdminItemCreationInterface {
    
    /**
     * Opens the admin item creation interface
     */
    public static void openAdminItemCreation(ServerPlayer player, EconomyManager economyManager, ItemStack sourceItem) {
        try {
            // Create menu provider for anvil-like interface
            SimpleMenuProvider menuProvider = new SimpleMenuProvider(
                (containerId, inventory, menuPlayer) -> {
                    return new AdminItemCreationMenu(containerId, inventory, player, economyManager, sourceItem.copy());
                },
                Component.literal("§4Create Admin Shop Item: " + sourceItem.getHoverName().getString())
            );
            
            player.openMenu(menuProvider);
            
            // Send instructions
            player.sendSystemMessage(Component.literal("§6=== Admin Item Creation ==="));
            player.sendSystemMessage(Component.literal("§eType the price for this item in chat"));
            player.sendSystemMessage(Component.literal("§7Example: 100.50"));
            player.sendSystemMessage(Component.literal("§7This item will have infinite stock"));
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error opening admin item creation interface", e);
            player.sendSystemMessage(Component.literal("§cError opening admin item creation interface"));
        }
    }
    
    /**
     * Menu for creating admin shop items
     */
    public static class AdminItemCreationMenu extends AnvilMenu {
        
        private final ServerPlayer player;
        private final EconomyManager economyManager;
        private final ItemStack sourceItem;
        private final Container inputContainer;
        
        public AdminItemCreationMenu(int containerId, Inventory playerInventory, ServerPlayer player, 
                                   EconomyManager economyManager, ItemStack sourceItem) {
            super(containerId, playerInventory, ContainerLevelAccess.NULL);
            this.player = player;
            this.economyManager = economyManager;
            this.sourceItem = sourceItem;
            this.inputContainer = new SimpleContainer(3);
            
            // Setup the anvil-like interface
            setupInterface();
        }
        
        private void setupInterface() {
            // Place the source item in the first slot
            inputContainer.setItem(0, sourceItem.copy());
            
            // Create instruction item in the second slot
            ItemStack instructionItem = new ItemStack(Items.GOLD_INGOT);
            instructionItem.set(DataComponents.CUSTOM_NAME, Component.literal("§6Type price in chat"));
            inputContainer.setItem(1, instructionItem);
            
            // Create preview item in the result slot
            ItemStack previewItem = sourceItem.copy();
            previewItem.set(DataComponents.CUSTOM_NAME, Component.literal("§4[ADMIN] " + sourceItem.getHoverName().getString()));
            inputContainer.setItem(2, previewItem);
        }
        
        @Override
        public boolean stillValid(@Nonnull net.minecraft.world.entity.player.Player menuPlayer) {
            return menuPlayer == player && menuPlayer.isAlive() && !menuPlayer.isRemoved();
        }
        
        @Override
        public void removed(@Nonnull net.minecraft.world.entity.player.Player menuPlayer) {
            super.removed(menuPlayer);
            
            // Don't return the item to inventory for admin creation
            // Admins can create items without losing them
        }
        
        /**
         * Called when the admin types in chat to set the price
         */
        public void setPriceFromChat(String priceText) {
            try {
                double price = Double.parseDouble(priceText);
                
                if (price <= 0) {
                    player.sendSystemMessage(Component.literal("§cPrice must be greater than 0"));
                    return;
                }
                
                // Create the admin shop item
                createAdminShopItem(price);
                
            } catch (NumberFormatException e) {
                player.sendSystemMessage(Component.literal("§cInvalid price format. Please enter a number."));
            }
        }
        
        private void createAdminShopItem(double price) {
            try {
                ShopManager shopManager = economyManager.getShopManager();
                
                if (shopManager == null) {
                    player.sendSystemMessage(Component.literal("§cShop manager is not available"));
                    return;
                }
                
                // Create the admin shop item
                ShopItem adminItem = new ShopItem.Builder()
                    .id(UUID.randomUUID())
                    .itemStack(sourceItem.copy())
                    .type(ShopItem.Type.BUY)
                    .buyPrice(BigDecimal.valueOf(price))
                    .currency(economyManager.getDefaultCurrency())
                    .stock(-1) // Infinite stock
                    .maxStock(-1)
                    .createdBy(null) // Admin item
                    .createdAt(LocalDateTime.now())
                    .description("Admin shop item")
                    .adminItem(true)
                    .build();
                
                // Add to shop
                if (shopManager.addShopItem(adminItem)) {
                    player.sendSystemMessage(Component.literal("§aSuccessfully created admin shop item: " + 
                        sourceItem.getHoverName().getString() + " for " + 
                        economyManager.getDefaultCurrency().format(BigDecimal.valueOf(price))));
                    
                    // Close the menu
                    player.closeContainer();
                    
                    // Reopen admin shop management
                    try {
                        var server = player.getServer();
                        if (server != null) {
                            server.execute(() -> {
                                AdminShopManagementInterface.openAdminShopManagement(player, economyManager);
                            });
                        } else {
                            AdminShopManagementInterface.openAdminShopManagement(player, economyManager);
                        }
                    } catch (Exception e) {
                        NeoEssentials.LOGGER.error("Error reopening admin shop management", e);
                    }
                    
                } else {
                    player.sendSystemMessage(Component.literal("§cFailed to create admin shop item"));
                }
                
            } catch (Exception e) {
                NeoEssentials.LOGGER.error("Error creating admin shop item", e);
                player.sendSystemMessage(Component.literal("§cError creating admin shop item: " + e.getMessage()));
            }
        }
    }
}
