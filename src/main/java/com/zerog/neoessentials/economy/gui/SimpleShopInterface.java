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
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.util.List;

/**
 * Simplified shop interface using chest menu
 */
public class SimpleShopInterface {
    
    private static final int ITEMS_PER_PAGE = 36; // 4 rows for items, 1 row for navigation
    
    public static void openShop(ServerPlayer player, EconomyManager economyManager) {
        try {
            // First check if shop manager exists
            ShopManager shopManager = economyManager.getShopManager();
            if (shopManager == null) {
                player.sendSystemMessage(Component.literal("§cShop system is not available"));
                return;
            }
            
            // Get available items safely
            List<ShopItem> availableItems;
            try {
                availableItems = shopManager.getAvailableItems();
            } catch (Exception e) {
                NeoEssentials.LOGGER.error("Failed to get shop items", e);
                // Create empty list as fallback
                availableItems = List.of();
            }
            
            // Create a simple container for the shop (5 rows instead of 6 to be safe)
            SimpleContainer container = new SimpleContainer(45); // 5 rows only
            
            // If no items available, show a helpful message
            if (availableItems.isEmpty()) {
                ItemStack noItemsInfo = new ItemStack(Items.PAPER);
                noItemsInfo.set(DataComponents.CUSTOM_NAME, Component.literal("§eNo items available"));
                container.setItem(22, noItemsInfo); // Center slot of 5-row container
                
                NeoEssentials.LOGGER.info("Shop is empty for player {}", player.getName().getString());
            } else {
                // Fill with shop items
                int itemCount = Math.min(availableItems.size(), ITEMS_PER_PAGE);
                NeoEssentials.LOGGER.info("Loading {} shop items for player {}", itemCount, player.getName().getString());
                
                for (int i = 0; i < itemCount; i++) {
                    try {
                        ShopItem shopItem = availableItems.get(i);
                        ItemStack displayItem = createShopDisplayItem(shopItem, economyManager);
                        container.setItem(i, displayItem);
                    } catch (Exception e) {
                        NeoEssentials.LOGGER.warn("Failed to create display item for slot " + i, e);
                        // Put a placeholder item if something goes wrong
                        ItemStack errorItem = new ItemStack(Items.BARRIER);
                        errorItem.set(DataComponents.CUSTOM_NAME, Component.literal("§cError loading item"));
                        container.setItem(i, errorItem);
                    }
                }
            }
            
            // Add navigation/info items in bottom row
            setupNavigationItems(container, availableItems.size(), economyManager);
            
            // Create a simple menu provider
            SimpleMenuProvider menuProvider = new SimpleMenuProvider(
                (containerId, inventory, menuPlayer) -> {
                    // Create ChestMenu with 5 rows instead of 6
                    ChestMenu menu = new ChestMenu(MenuType.GENERIC_9x5, containerId, inventory, container, 5) {
                        @Override
                        public boolean stillValid(@Nonnull net.minecraft.world.entity.player.Player menuPlayer) {
                            return menuPlayer == player && menuPlayer.isAlive() && !menuPlayer.isRemoved();
                        }
                        
                        @Override
                        @Nonnull
                        public ItemStack quickMoveStack(@Nonnull net.minecraft.world.entity.player.Player menuPlayer, int index) {
                            // Prevent shift-clicking items out
                            return ItemStack.EMPTY;
                        }
                        
                        @Override
                        public boolean canTakeItemForPickAll(@Nonnull ItemStack stack, @Nonnull net.minecraft.world.inventory.Slot slot) {
                            // Prevent taking items
                            return false;
                        }
                    };
                    return menu;
                },
                Component.literal("§6Shop")
            );
            
            // Open the container safely
            player.openMenu(menuProvider);
            NeoEssentials.LOGGER.info("Opened shop GUI for player {}", player.getName().getString());
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to open shop interface for player " + player.getName().getString(), e);
            player.sendSystemMessage(Component.literal("§cFailed to open shop interface. Please try again or contact an administrator."));
        }
    }
    
    private static ItemStack createShopDisplayItem(ShopItem shopItem, EconomyManager economyManager) {
        try {
            ItemStack displayItem = shopItem.getItemStack().copy();
            
            // Create basic display item with price information
            if (displayItem != null && !displayItem.isEmpty()) {
                // Add price info to the display name
                String itemName = displayItem.getHoverName().getString();
                BigDecimal price = shopItem.getPrice();
                String priceText = String.format("%.2f", price.doubleValue());
                
                // Create a new display name with price
                Component newName = Component.literal("§f" + itemName + " §7- §6" + priceText + " coins");
                displayItem.set(DataComponents.CUSTOM_NAME, newName);
                
                return displayItem;
            }
        } catch (Exception e) {
            NeoEssentials.LOGGER.warn("Failed to create shop display item", e);
        }
        
        // Fallback to a simple item if something goes wrong
        ItemStack fallback = new ItemStack(Items.STONE);
        fallback.set(DataComponents.CUSTOM_NAME, Component.literal("§cError loading item"));
        return fallback;
    }
    
    private static void setupNavigationItems(SimpleContainer container, int totalItems, EconomyManager economyManager) {
        try {
            // Refresh button - bottom row center
            ItemStack refresh = new ItemStack(Items.EMERALD);
            refresh.set(DataComponents.CUSTOM_NAME, Component.literal("§aRefresh Shop"));
            container.setItem(40, refresh); // Center of bottom row (5th row)
            
            // Close button - bottom row right  
            ItemStack close = new ItemStack(Items.BARRIER);
            close.set(DataComponents.CUSTOM_NAME, Component.literal("§cClose Shop"));
            container.setItem(44, close); // Right side of bottom row
            
            // Info item - bottom row left
            ItemStack info = new ItemStack(Items.BOOK);
            info.set(DataComponents.CUSTOM_NAME, Component.literal("§eShop Info"));
            container.setItem(36, info); // Left side of bottom row
            
            // Fill empty slots in bottom row with glass panes
            for (int i = 37; i < 44; i++) {
                if (i != 40 && i != 41) { // Skip refresh and item count slots
                    ItemStack glassPane = new ItemStack(Items.LIGHT_GRAY_STAINED_GLASS_PANE);
                    glassPane.set(DataComponents.CUSTOM_NAME, Component.literal(""));
                    container.setItem(i, glassPane);
                }
            }
            
            // Add item count info
            ItemStack itemCountInfo = new ItemStack(Items.PAPER);
            itemCountInfo.set(DataComponents.CUSTOM_NAME, Component.literal("§7Items available: §f" + totalItems));
            container.setItem(41, itemCountInfo);
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.warn("Failed to setup navigation items", e);
        }
    }
}
