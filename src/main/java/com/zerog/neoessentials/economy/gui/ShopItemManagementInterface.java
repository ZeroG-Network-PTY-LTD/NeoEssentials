package com.zerog.neoessentials.economy.gui;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.economy.EconomyManager;
import com.zerog.neoessentials.economy.shop.ShopItem;
import com.zerog.neoessentials.economy.shop.ShopManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.UUID;

/**
 * Shop item management interface for editing and managing player's shop listings
 */
public class ShopItemManagementInterface {
    
    private static final int CONTAINER_SIZE = 45; // 5x9 slots
    
    public enum ManagementAction {
        EDIT_PRICE,
        EDIT_STOCK,
        REMOVE_LISTING,
        RETRIEVE_ITEMS,
        BACK_TO_SHOP
    }
    
    /**
     * Opens the shop item management interface for a specific item
     */
    public static void openItemManagement(ServerPlayer player, EconomyManager economyManager, 
                                        ShopItem shopItem) {
        try {
            // Verify player owns this item
            if (!player.getUUID().equals(shopItem.getCreatedBy())) {
                player.sendSystemMessage(Component.literal("§cYou can only manage your own shop items"));
                return;
            }
            
            SimpleContainer container = new SimpleContainer(CONTAINER_SIZE);
            setupManagementInterface(container, shopItem, player, economyManager);
            
            String title = "§6Manage: " + shopItem.getItemStack().getHoverName().getString();
            
            SimpleMenuProvider menuProvider = new SimpleMenuProvider(
                (containerId, inventory, menuPlayer) -> {
                    return new ShopItemManagementMenu(containerId, inventory, container, 
                                                    player, economyManager, shopItem);
                },
                Component.literal(title)
            );
            
            player.openMenu(menuProvider);
            NeoEssentials.LOGGER.info("Opened shop item management for player {} - item: {}", 
                                    player.getName().getString(), shopItem.getItemStack().getHoverName().getString());
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to open shop item management for player " + player.getName().getString(), e);
            player.sendSystemMessage(Component.literal("§cFailed to open management interface"));
        }
    }
    
    /**
     * Sets up the management interface layout
     */
    private static void setupManagementInterface(SimpleContainer container, ShopItem shopItem, 
                                               ServerPlayer player, EconomyManager economyManager) {
        try {
            // Clear container
            container.clearContent();
            
            // Show the item being managed (center top)
            ItemStack displayItem = shopItem.getItemStack().copy();
            String itemInfo = String.format("§f%s\n§7Stock: §e%d\n§7Price: §6%.2f coins\n§7Created: §a%s", 
                displayItem.getHoverName().getString(),
                shopItem.getStock(),
                shopItem.getBuyPrice().doubleValue(),
                shopItem.getCreatedAt().toLocalDate().toString());
            displayItem.set(DataComponents.CUSTOM_NAME, Component.literal(itemInfo));
            container.setItem(13, displayItem); // Center of top section
            
            // Management action buttons
            setupActionButtons(container, shopItem);
            
            // Current statistics
            setupStatisticsDisplay(container, shopItem);
            
            // Navigation buttons
            setupNavigationButtons(container);
            
            // Fill empty slots with glass panes
            fillEmptySlots(container);
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.warn("Failed to setup management interface", e);
        }
    }
    
    /**
     * Sets up the action buttons for management operations
     */
    private static void setupActionButtons(SimpleContainer container, ShopItem shopItem) {
        // Edit Price button
        ItemStack editPrice = new ItemStack(Items.GOLD_INGOT);
        editPrice.set(DataComponents.CUSTOM_NAME, Component.literal("§6Edit Price"));
        // Add lore with current price and instructions
        String priceInfo = String.format("§7Current: §6%.2f coins\n§eClick to open price editor", 
            shopItem.getBuyPrice().doubleValue());
        container.setItem(19, editPrice); // Left side of middle row
        
        // Edit Stock button
        ItemStack editStock = new ItemStack(Items.CHEST);
        editStock.set(DataComponents.CUSTOM_NAME, Component.literal("§eEdit Stock"));
        // Add lore with current stock and instructions
        String stockInfo = String.format("§7Current: §e%d items\n§eClick to open stock editor", 
            shopItem.getStock());
        container.setItem(21, editStock); // Middle of middle row
        
        // Remove Listing button
        ItemStack removeListing = new ItemStack(Items.RED_WOOL);
        removeListing.set(DataComponents.CUSTOM_NAME, Component.literal("§cRemove Listing"));
        // Add lore with warning about removing
        String removeInfo = "§7Permanently removes this listing\n§7Returns remaining stock to inventory\n§cClick to confirm removal";
        container.setItem(23, removeListing); // Right side of middle row
        
        // Retrieve Items button (if stock > 0)
        if (shopItem.getStock() > 0) {
            ItemStack retrieveItems = new ItemStack(Items.HOPPER);
            retrieveItems.set(DataComponents.CUSTOM_NAME, Component.literal("§bRetrieve Items"));
            String retrieveInfo = String.format("§7Take back %d items\n§7This will remove them from sale\n§bClick to retrieve", 
                shopItem.getStock());
            container.setItem(25, retrieveItems); // Far right of middle row
        }
    }
    
    /**
     * Sets up statistics display
     */
    private static void setupStatisticsDisplay(SimpleContainer container, ShopItem shopItem) {
        // Item details
        ItemStack details = new ItemStack(Items.PAPER);
        String detailsText = String.format("§eItem Details\n§7ID: §f%s\n§7Type: §f%s\n§7Admin Item: §f%s",
            shopItem.getId().toString().substring(0, 8) + "...",
            shopItem.getType().toString(),
            shopItem.isAdminItem() ? "Yes" : "No");
        details.set(DataComponents.CUSTOM_NAME, Component.literal(detailsText));
        container.setItem(10, details); // Left side info
        
        // Sales statistics (placeholder for future enhancement)
        ItemStack stats = new ItemStack(Items.BOOK);
        stats.set(DataComponents.CUSTOM_NAME, Component.literal("§aSales Statistics\n§7Total Sold: §fComing Soon\n§7Revenue: §fComing Soon"));
        container.setItem(16, stats); // Right side info
    }
    
    /**
     * Sets up navigation buttons
     */
    private static void setupNavigationButtons(SimpleContainer container) {
        // Back to shop button
        ItemStack backToShop = new ItemStack(Items.ARROW);
        backToShop.set(DataComponents.CUSTOM_NAME, Component.literal("§eBack to My Shop"));
        container.setItem(36, backToShop); // Bottom left
        
        // Close button
        ItemStack close = new ItemStack(Items.BARRIER);
        close.set(DataComponents.CUSTOM_NAME, Component.literal("§cClose"));
        container.setItem(44, close); // Bottom right
    }
    
    /**
     * Fills empty slots with decorative glass panes
     */
    private static void fillEmptySlots(SimpleContainer container) {
        ItemStack glassPane = new ItemStack(Items.LIGHT_GRAY_STAINED_GLASS_PANE);
        glassPane.set(DataComponents.CUSTOM_NAME, Component.literal(""));
        
        // Fill specific slots that should be glass panes
        int[] glassPaneSlots = {0, 1, 2, 3, 4, 5, 6, 7, 8,    // Top row
                               9, 11, 12, 14, 15, 17,           // Around displays
                               18, 20, 22, 24, 26,              // Around buttons
                               27, 28, 29, 30, 31, 32, 33, 34, 35, // Empty row
                               37, 38, 39, 40, 41, 42, 43};     // Bottom row except corners
        
        for (int slot : glassPaneSlots) {
            if (container.getItem(slot).isEmpty()) {
                container.setItem(slot, glassPane.copy());
            }
        }
    }
}
