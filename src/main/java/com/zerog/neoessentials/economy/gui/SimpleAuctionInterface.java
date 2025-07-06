package com.zerog.neoessentials.economy.gui;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.economy.EconomyManager;
import com.zerog.neoessentials.economy.auction.AuctionItem;
import com.zerog.neoessentials.economy.auction.AuctionManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * Simplified auction interface using chest menu
 */
public class SimpleAuctionInterface {
    
    private static final int ITEMS_PER_PAGE = 45; // 5 rows for items
    
    public static void openAuctionHouse(ServerPlayer player, EconomyManager economyManager) {
        try {
            AuctionManager auctionManager = economyManager.getAuctionManager();
            List<AuctionItem> activeAuctions = auctionManager.getActiveAuctions();
            
            // Create a simple container for the auction house
            SimpleContainer container = new SimpleContainer(54); // 6 rows
            
            // Fill with auction items
            int itemCount = Math.min(activeAuctions.size(), ITEMS_PER_PAGE);
            for (int i = 0; i < itemCount; i++) {
                AuctionItem auctionItem = activeAuctions.get(i);
                ItemStack displayItem = createAuctionDisplayItem(auctionItem, economyManager);
                container.setItem(i, displayItem);
            }
            
            // Add navigation/info items in bottom row
            setupNavigationItems(container, activeAuctions.size(), economyManager);
            
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
                Component.literal("Auction House")
            ));
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to open auction interface", e);
            player.sendSystemMessage(Component.literal("§cFailed to open auction interface"));
        }
    }
    
    private static ItemStack createAuctionDisplayItem(AuctionItem auctionItem, EconomyManager economyManager) {
        ItemStack displayItem = auctionItem.getItemStack().copy();
        
        // For now, just return the item as-is
        // In a full implementation, you would add lore with auction information
        // using the appropriate MC version methods
        
        return displayItem;
    }
    
    private static void setupNavigationItems(SimpleContainer container, int totalItems, EconomyManager economyManager) {
        // Create auction button (if player is holding an item)
        ItemStack createAuction = new ItemStack(Items.GOLD_INGOT);
        container.setItem(45, createAuction); // Left side of bottom row
        
        // Refresh button
        ItemStack refresh = new ItemStack(Items.EMERALD);
        container.setItem(49, refresh); // Center of bottom row
        
        // My auctions button
        ItemStack myAuctions = new ItemStack(Items.ENDER_CHEST);
        container.setItem(47, myAuctions);
        
        // My bids button
        ItemStack myBids = new ItemStack(Items.DIAMOND);
        container.setItem(51, myBids);
        
        // Close button  
        ItemStack close = new ItemStack(Items.BARRIER);
        container.setItem(53, close); // Right side of bottom row
        
        // Fill empty slots in bottom row with glass panes
        for (int i = 46; i < 54; i++) {
            if (container.getItem(i).isEmpty()) {
                container.setItem(i, new ItemStack(Items.LIGHT_GRAY_STAINED_GLASS_PANE));
            }
        }
    }
}
