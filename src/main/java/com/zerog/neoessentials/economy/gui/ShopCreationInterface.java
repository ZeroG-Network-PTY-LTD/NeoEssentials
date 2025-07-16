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
<<<<<<< HEAD
            // Check if economy system is enabled
            if (!economyManager.isEnabled()) {
                player.sendSystemMessage(Component.literal("§cEconomy system is disabled. Cannot create shop listings."));
                return;
            }
            
=======
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
            // Check if player is holding an item
            ItemStack heldItem = player.getMainHandItem();
            
            if (heldItem.isEmpty()) {
                player.sendSystemMessage(Component.literal("§cYou must be holding an item to create a shop listing"));
                return;
            }
            
<<<<<<< HEAD
            // Validate item can be sold
            if (!isValidShopItem(heldItem)) {
                player.sendSystemMessage(Component.literal("§cThis item cannot be sold in the shop"));
                return;
            }
            
            // Check if player already has too many shop listings
            ShopManager shopManager = economyManager.getShopManager();
            long playerItems = shopManager.getAllItems().stream()
                .filter(item -> item.getCreatedBy() != null && item.getCreatedBy().equals(player.getUUID()))
                .count();
            
            if (playerItems >= 50) { // Max 50 listings per player
                player.sendSystemMessage(Component.literal("§cYou have too many shop listings. Maximum: 50"));
                return;
            }
            
=======
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
            // Send clear instructions to the player
            player.sendSystemMessage(Component.literal("§6=== Create Shop Item ==="));
            player.sendSystemMessage(Component.literal("§eItem: " + heldItem.getHoverName().getString()));
            player.sendSystemMessage(Component.literal("§eQuantity: " + heldItem.getCount()));
<<<<<<< HEAD
            player.sendSystemMessage(Component.literal("§7Current balance: " + economyManager.getDefaultCurrency().format(economyManager.getBalance(player.getUUID()))));
=======
            player.sendSystemMessage(Component.literal("§eType the price for this item in chat"));
            player.sendSystemMessage(Component.literal("§7Example: 10.50"));
            player.sendSystemMessage(Component.literal("§7This will create a shop listing with " + heldItem.getCount() + " items in stock"));
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
            
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
<<<<<<< HEAD
     * Validates if an item can be sold in the shop
     */
    private static boolean isValidShopItem(ItemStack item) {
        if (item.isEmpty()) return false;
        
        // Blacklist certain items
        String itemId = item.getItem().toString();
        if (itemId.contains("spawn_egg") || 
            itemId.contains("command_block") || 
            itemId.contains("barrier") ||
            itemId.contains("structure_block") ||
            itemId.contains("debug_stick") ||
            itemId.contains("knowledge_book")) {
            return false;
        }
        
        // Check if item has components that make it unsuitable for shop
        if (item.has(DataComponents.WRITTEN_BOOK_CONTENT) || 
            item.has(DataComponents.WRITABLE_BOOK_CONTENT)) {
            return false;
        }
        
        return true;
    }
    
    /**
=======
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
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
<<<<<<< HEAD
                // Validate economy system is still enabled
                if (!economyManager.isEnabled()) {
                    player.sendSystemMessage(Component.literal("§cEconomy system is disabled"));
                    return;
                }
                
                // Validate price
                if (price <= 0) {
                    player.sendSystemMessage(Component.literal("§cPrice must be greater than 0"));
                    return;
                }
                
                if (price > 1000000) {
                    player.sendSystemMessage(Component.literal("§cPrice cannot exceed 1,000,000"));
                    return;
                }
                
                // Validate player still has the item
                ItemStack currentHeldItem = player.getMainHandItem();
                if (currentHeldItem.isEmpty()) {
                    player.sendSystemMessage(Component.literal("§cYou are no longer holding an item"));
                    return;
                }
                
                if (!ItemStack.isSameItem(currentHeldItem, itemToSell)) {
                    player.sendSystemMessage(Component.literal("§cYou are no longer holding the correct item"));
                    return;
                }
                
                int availableStock = currentHeldItem.getCount();
                int requestedStock = Math.min(itemToSell.getCount(), availableStock);
                
                if (requestedStock <= 0) {
                    player.sendSystemMessage(Component.literal("§cNo items available to list"));
                    return;
                }
                
                // Check for existing similar listings
                ShopManager shopManager = economyManager.getShopManager();
                boolean hasSimilarListing = shopManager.getAllItems().stream()
                    .anyMatch(item -> item.getCreatedBy() != null && 
                             item.getCreatedBy().equals(player.getUUID()) &&
                             ItemStack.isSameItem(item.getItemStack(), itemToSell) &&
                             item.getBuyPrice() != null &&
                             item.getBuyPrice().equals(BigDecimal.valueOf(price)));
                
                if (hasSimilarListing) {
                    player.sendSystemMessage(Component.literal("§cYou already have a similar listing with the same price"));
                    return;
                }
                
                // Create the item with validation
                ItemStack shopItemStack = itemToSell.copy();
                shopItemStack.setCount(1); // Shop listings are per-item
                
                // Create shop item using the shop manager's builder
                ShopItem shopItem = shopManager.createShopItemBuilder()
                    .id(UUID.randomUUID())
                    .itemStack(shopItemStack)
                    .type(ShopItem.Type.BUY)
                    .buyPrice(BigDecimal.valueOf(price))
                    .sellPrice(null) // Players can't set sell prices initially
                    .stock(requestedStock)
                    .maxStock(requestedStock)
                    .createdBy(player.getUUID())
                    .createdAt(LocalDateTime.now())
                    .description("Player shop item - " + player.getName().getString())
                    .adminItem(false)
                    .build();
                
                // Add the item to shop with validation
                if (shopManager.safeAddShopItem(shopItem)) {
                    // Remove items from player's inventory
                    currentHeldItem.shrink(requestedStock);
                    
                    // Send success message
                    player.sendSystemMessage(Component.literal("§aSuccessfully created shop listing!"));
                    player.sendSystemMessage(Component.literal("§7Item: " + shopItemStack.getHoverName().getString()));
                    player.sendSystemMessage(Component.literal("§7Stock: §e" + requestedStock));
                    player.sendSystemMessage(Component.literal("§7Price: " + economyManager.getDefaultCurrency().format(BigDecimal.valueOf(price)) + " each"));
                    
                    // Log the creation
                    NeoEssentials.LOGGER.info("Player {} created shop item: {}x {} for {} each", 
                        player.getName().getString(), requestedStock, shopItemStack.getHoverName().getString(), 
=======
                // Validate player still has the item
                ItemStack currentHeldItem = player.getMainHandItem();
                if (currentHeldItem.isEmpty() || !ItemStack.isSameItem(currentHeldItem, itemToSell)) {
                    player.sendSystemMessage(Component.literal("§cYou no longer have the required item"));
                    return;
                }
                
                int stock = Math.min(itemToSell.getCount(), currentHeldItem.getCount());
                
                // Create shop item using the shop manager's helper method
                ShopManager shopManager = economyManager.getShopManager();
                ShopItem shopItem = shopManager.createShopItemBuilder()
                    .id(UUID.randomUUID())
                    .itemStack(itemToSell.copy())
                    .type(ShopItem.Type.BUY)
                    .buyPrice(BigDecimal.valueOf(price))
                    .stock(stock)
                    .maxStock(stock)
                    .createdBy(player.getUUID())
                    .createdAt(LocalDateTime.now())
                    .description("Player shop item")
                    .adminItem(false)
                    .build();
                
                if (shopManager.addShopItem(shopItem)) {
                    // Remove items from player's inventory
                    currentHeldItem.shrink(stock);
                    
                    player.sendSystemMessage(Component.literal("§aSuccessfully created shop listing!"));
                    player.sendSystemMessage(Component.literal("§7Item: " + itemToSell.getHoverName().getString()));
                    player.sendSystemMessage(Component.literal("§7Stock: §e" + stock));
                    player.sendSystemMessage(Component.literal("§7Price: " + economyManager.getDefaultCurrency().format(BigDecimal.valueOf(price)) + " each"));
                    
                    NeoEssentials.LOGGER.info("Player {} created shop item: {}x {} for {} each", 
                        player.getName().getString(), stock, itemToSell.getHoverName().getString(), 
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
                        economyManager.getDefaultCurrency().format(BigDecimal.valueOf(price)));
                    
                    // Return to personal shop with updated listings
                    player.getServer().execute(() -> {
<<<<<<< HEAD
                        try {
                            EnhancedShopInterface.openPersonalShop(player, economyManager);
                        } catch (Exception e) {
                            NeoEssentials.LOGGER.error("Error opening personal shop after creation", e);
                        }
                    });
                } else {
                    player.sendSystemMessage(Component.literal("§cFailed to create shop listing. Please try again."));
                    // Return to personal shop
                    player.getServer().execute(() -> {
                        try {
                            EnhancedShopInterface.openPersonalShop(player, economyManager);
                        } catch (Exception e) {
                            NeoEssentials.LOGGER.error("Error opening personal shop after failed creation", e);
                        }
                    });
                }
                
            } catch (NumberFormatException e) {
                player.sendSystemMessage(Component.literal("§cInvalid price format"));
                NeoEssentials.LOGGER.warn("Player {} provided invalid price format", player.getName().getString());
            } catch (Exception e) {
                NeoEssentials.LOGGER.error("Failed to create shop listing for player {}", player.getName().getString(), e);
                player.sendSystemMessage(Component.literal("§cFailed to create shop listing: " + e.getMessage()));
=======
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
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
            }
        }
    }
}
