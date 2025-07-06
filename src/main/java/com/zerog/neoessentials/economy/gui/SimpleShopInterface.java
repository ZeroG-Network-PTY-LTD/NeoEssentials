package com.zerog.neoessentials.economy.gui;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.economy.EconomyManager;
import com.zerog.neoessentials.economy.shop.ShopItem;
import com.zerog.neoessentials.economy.shop.ShopManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

/**
 * Simplified shop interface using chest menu
 */
public class SimpleShopInterface {
    
    private static final int ITEMS_PER_PAGE = 45; // 5 rows for items
    
    public static void openShop(ServerPlayer player, EconomyManager economyManager) {
        try {
            ShopManager shopManager = economyManager.getShopManager();
            List<ShopItem> availableItems = shopManager.getAvailableItems();
            
            // Create a simple container for the shop
            SimpleContainer container = new SimpleContainer(54); // 6 rows
            
            // Fill with shop items
            int itemCount = Math.min(availableItems.size(), ITEMS_PER_PAGE);
            for (int i = 0; i < itemCount; i++) {
                ShopItem shopItem = availableItems.get(i);
                ItemStack displayItem = createShopDisplayItem(shopItem, economyManager);
                container.setItem(i, displayItem);
            }
            
            // Add navigation/info items in bottom row
            setupNavigationItems(container, availableItems.size(), economyManager);
            
            // Open the menu using the built-in chest menu type
            ChestMenu menu = new ChestMenu(MenuType.GENERIC_9x6, 0, player.getInventory(), container, 6) {
                @Override
                public boolean stillValid(net.minecraft.world.entity.player.Player menuPlayer) {
                    return menuPlayer == player && menuPlayer.isAlive();
                }
                
                @Override
                public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player menuPlayer, int index) {
                    // Prevent shift-clicking items out
                    return ItemStack.EMPTY;
                }
                
                @Override
                public boolean canTakeItemForPickAll(ItemStack stack, net.minecraft.world.inventory.Slot slot) {
                    // Prevent taking items
                    return false;
                }
            };
            
            // Open the container
            player.openMenu(new net.minecraft.world.SimpleMenuProvider(
                (containerId, inventory, menuPlayer) -> menu,
                Component.literal("Shop")
            ));
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to open shop interface", e);
            player.sendSystemMessage(Component.literal("§cFailed to open shop interface"));
        }
    }
    
    private static ItemStack createShopDisplayItem(ShopItem shopItem, EconomyManager economyManager) {
        ItemStack displayItem = shopItem.getItemStack().copy();
        
        // For now, just return the item as-is
        // In a full implementation, you would add lore with pricing information
        // using the appropriate MC version methods
        
        return displayItem;
    }
    
    private static void setupNavigationItems(SimpleContainer container, int totalItems, EconomyManager economyManager) {
        // Refresh button
        ItemStack refresh = new ItemStack(Items.EMERALD);
        container.setItem(49, refresh); // Center of bottom row
        
        // Close button  
        ItemStack close = new ItemStack(Items.BARRIER);
        container.setItem(53, close); // Right side of bottom row
        
        // Info item
        ItemStack info = new ItemStack(Items.BOOK);
        container.setItem(45, info); // Left side of bottom row
        
        // Fill empty slots in bottom row with glass panes
        for (int i = 46; i < 54; i++) {
            if (i != 49 && i != 53 && i != 45) {
                container.setItem(i, new ItemStack(Items.LIGHT_GRAY_STAINED_GLASS_PANE));
            }
        }
    }
}
