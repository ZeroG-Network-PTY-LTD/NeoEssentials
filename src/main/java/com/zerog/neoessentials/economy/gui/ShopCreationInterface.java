package com.zerog.neoessentials.economy.gui;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.economy.Currency;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Interface for creating new shop items through GUI
 */
public class ShopCreationInterface {
    
    /**
     * Opens the shop creation interface for a player
     */
    public static void openShopCreation(ServerPlayer player, EconomyManager economyManager) {
        try {
            // Check if player is holding an item
            ItemStack heldItem = player.getMainHandItem();
            
            if (heldItem.isEmpty()) {
                player.sendSystemMessage(Component.literal("§cYou must be holding an item to create a shop listing"));
                return;
            }
            
            // Create menu provider for anvil-like interface
            SimpleMenuProvider menuProvider = new SimpleMenuProvider(
                (containerId, inventory, menuPlayer) -> {
                    return new ShopCreationMenu(containerId, inventory, player, economyManager, heldItem.copy());
                },
                Component.literal("§6Create Shop Listing: " + heldItem.getHoverName().getString())
            );
            
            player.openMenu(menuProvider);
            
            // Send instructions to player
            player.sendSystemMessage(Component.literal("§e=== Create Shop Listing ==="));
            player.sendSystemMessage(Component.literal("§7Item: " + heldItem.getHoverName().getString()));
            player.sendSystemMessage(Component.literal("§7Available: " + heldItem.getCount()));
            player.sendSystemMessage(Component.literal("§7Enter price per item (e.g., '10.50')"));
            player.sendSystemMessage(Component.literal("§7Place a paper in the first slot and rename it to set the price"));
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to open shop creation interface", e);
            player.sendSystemMessage(Component.literal("§cFailed to open shop creation interface"));
        }
    }
    
    /**
     * Custom anvil menu for shop creation
     */
    public static class ShopCreationMenu extends AnvilMenu {
        private final ServerPlayer player;
        private final EconomyManager economyManager;
        private final ItemStack itemToSell;
        
        public ShopCreationMenu(int containerId, Inventory playerInventory, ServerPlayer player, 
                               EconomyManager economyManager, ItemStack itemToSell) {
            super(containerId, playerInventory, ContainerLevelAccess.NULL);
            this.player = player;
            this.economyManager = economyManager;
            this.itemToSell = itemToSell;
            
            // Setup initial items
            setupInitialItems();
        }
        
        private void setupInitialItems() {
            // Place a paper in the first slot for price input
            ItemStack priceItem = new ItemStack(Items.PAPER);
            priceItem.set(DataComponents.CUSTOM_NAME, Component.literal("0.00"));
            this.inputSlots.setItem(0, priceItem);
            
            // Show the item to be sold in the second slot
            this.inputSlots.setItem(1, itemToSell.copy());
        }
        
        @Override
        public void createResult() {
            ItemStack priceItem = this.inputSlots.getItem(0);
            ItemStack itemSlot = this.inputSlots.getItem(1);
            
            if (!priceItem.isEmpty() && !itemSlot.isEmpty()) {
                String priceText = priceItem.getHoverName().getString();
                
                try {
                    double price = Double.parseDouble(priceText);
                    if (price > 0) {
                        // Create result item showing the shop listing
                        ItemStack resultItem = new ItemStack(Items.EMERALD);
                        resultItem.set(DataComponents.CUSTOM_NAME, 
                            Component.literal("§aCreate Listing: " + itemToSell.getHoverName().getString() + 
                                           "\n§7Price: §6" + price + " coins each" +
                                           "\n§7Stock: §e" + itemToSell.getCount() + " items"));
                        this.resultSlots.setItem(0, resultItem);
                        return;
                    }
                } catch (NumberFormatException e) {
                    // Invalid price format
                }
            }
            
            // Clear result if invalid
            this.resultSlots.setItem(0, ItemStack.EMPTY);
        }
        
        @Override
        public boolean stillValid(net.minecraft.world.entity.player.Player menuPlayer) {
            return menuPlayer == player && menuPlayer.isAlive();
        }
        
        @Override
        public void removed(net.minecraft.world.entity.player.Player menuPlayer) {
            super.removed(menuPlayer);
            
            // Check if result was taken (shop item was created)
            if (this.resultSlots.getItem(0).isEmpty() && !this.inputSlots.getItem(0).isEmpty()) {
                String priceText = this.inputSlots.getItem(0).getHoverName().getString();
                try {
                    double price = Double.parseDouble(priceText);
                    if (price > 0) {
                        createShopListing(price);
                        return;
                    }
                } catch (NumberFormatException e) {
                    // Invalid price
                }
            }
            
            // Return to personal shop if no listing was created
            player.getServer().execute(() -> {
                EnhancedShopInterface.openPersonalShop(player, economyManager);
            });
        }
        
        private void createShopListing(double price) {
            try {
                // Validate player still has the item
                ItemStack currentHeldItem = player.getMainHandItem();
                if (currentHeldItem.isEmpty() || !ItemStack.isSameItem(currentHeldItem, itemToSell)) {
                    player.sendSystemMessage(Component.literal("§cYou no longer have the required item"));
                    return;
                }
                
                int stock = Math.min(itemToSell.getCount(), currentHeldItem.getCount());
                
                // Create shop item
                ShopItem shopItem = new ShopItem.Builder()
                    .id(UUID.randomUUID())
                    .itemStack(itemToSell.copy())
                    .type(ShopItem.Type.BUY)
                    .buyPrice(BigDecimal.valueOf(price))
                    .currency(Currency.createBasic("coins", "Coin", "§6", "Coins"))
                    .stock(stock)
                    .maxStock(stock)
                    .createdBy(player.getUUID())
                    .createdAt(LocalDateTime.now())
                    .description("Player shop item")
                    .adminItem(false)
                    .build();
                
                ShopManager shopManager = economyManager.getShopManager();
                if (shopManager.addShopItem(shopItem)) {
                    // Remove items from player's inventory
                    currentHeldItem.shrink(stock);
                    
                    player.sendSystemMessage(Component.literal("§aSuccessfully created shop listing!"));
                    player.sendSystemMessage(Component.literal("§7Item: " + itemToSell.getHoverName().getString()));
                    player.sendSystemMessage(Component.literal("§7Stock: §e" + stock));
                    player.sendSystemMessage(Component.literal("§7Price: §6" + price + " coins each"));
                    
                    NeoEssentials.LOGGER.info("Player {} created shop item: {}x {} for {} coins each", 
                        player.getName().getString(), stock, itemToSell.getHoverName().getString(), price);
                    
                    // Return to personal shop with updated listings
                    player.getServer().execute(() -> {
                        EnhancedShopInterface.openPersonalShop(player, economyManager);
                    });
                } else {
                    player.sendSystemMessage(Component.literal("§cFailed to create shop listing"));
                    player.getServer().execute(() -> {
                        EnhancedShopInterface.openPersonalShop(player, economyManager);
                    });
                }
            } catch (Exception e) {
                NeoEssentials.LOGGER.error("Failed to create shop listing", e);
                player.sendSystemMessage(Component.literal("§cFailed to create shop listing"));
            }
        }
    }
}
