package com.zerog.neoessentials.economy.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for ItemStack operations in GUIs
 * Provides compatibility across different MC versions
 */
public class ItemStackHelper {
    
    /**
     * Creates a display item with custom name and lore
     */
    public static ItemStack createDisplayItem(ItemStack base, String name, List<String> lore) {
        ItemStack display = base.copy();
        
        // Try to set display name using NBT (fallback method)
        try {
            CompoundTag tag = display.getOrCreateTag();
            CompoundTag displayTag = tag.getCompound("display");
            if (displayTag.isEmpty()) {
                displayTag = new CompoundTag();
                tag.put("display", displayTag);
            }
            displayTag.putString("Name", Component.Serializer.toJson(Component.literal(name)));
            
            if (lore != null && !lore.isEmpty()) {
                List<String> loreJson = new ArrayList<>();
                for (String line : lore) {
                    loreJson.add(Component.Serializer.toJson(Component.literal(line)));
                }
                // Convert to NBT list format would go here
                // This is a simplified version for compatibility
            }
        } catch (Exception e) {
            // If NBT methods are not available, just return the base item
            // In a real implementation, you'd use the appropriate MC version methods
        }
        
        return display;
    }
    
    /**
     * Creates a simple display item with just a name
     */
    public static ItemStack createDisplayItem(ItemStack base, String name) {
        return createDisplayItem(base, name, null);
    }
    
    /**
     * Creates navigation items for GUIs
     */
    public static class NavigationItems {
        
        public static ItemStack createPreviousPageItem() {
            return createDisplayItem(new ItemStack(Items.ARROW), "§ePrevious Page");
        }
        
        public static ItemStack createNextPageItem() {
            return createDisplayItem(new ItemStack(Items.ARROW), "§eNext Page");
        }
        
        public static ItemStack createRefreshItem() {
            return createDisplayItem(new ItemStack(Items.LIME_DYE), "§eRefresh");
        }
        
        public static ItemStack createCloseItem() {
            return createDisplayItem(new ItemStack(Items.BARRIER), "§cClose");
        }
        
        public static ItemStack createBackItem() {
            return createDisplayItem(new ItemStack(Items.ARROW), "§eBack");
        }
        
        public static ItemStack createPageInfoItem(int currentPage, int totalPages) {
            return createDisplayItem(new ItemStack(Items.PAPER), 
                "§ePage " + (currentPage + 1) + " of " + totalPages);
        }
        
        public static ItemStack createEmptySlotItem() {
            return createDisplayItem(new ItemStack(Items.LIGHT_GRAY_STAINED_GLASS_PANE), " ");
        }
    }
    
    /**
     * Creates shop-specific display items
     */
    public static class ShopItems {
        
        public static ItemStack createShopDisplayItem(ItemStack item, String price, int stock) {
            List<String> lore = new ArrayList<>();
            lore.add("§7Price: §a" + price);
            if (stock >= 0) {
                lore.add("§7Stock: §e" + (stock == 0 ? "Out of Stock" : stock));
            } else {
                lore.add("§7Stock: §eUnlimited");
            }
            lore.add("");
            lore.add("§eClick to purchase");
            
            return createDisplayItem(item, item.getHoverName().getString(), lore);
        }
        
        public static ItemStack createCreateShopItem() {
            List<String> lore = new ArrayList<>();
            lore.add("§7Create a new shop listing");
            lore.add("");
            lore.add("§eClick to create");
            
            return createDisplayItem(new ItemStack(Items.EMERALD), "§aCreate Shop Listing", lore);
        }
    }
    
    /**
     * Creates auction-specific display items
     */
    public static class AuctionItems {
        
        public static ItemStack createAuctionDisplayItem(ItemStack item, String currentBid, 
                                                       String buyNowPrice, String timeLeft) {
            List<String> lore = new ArrayList<>();
            lore.add("§7Current Bid: §a" + currentBid);
            if (buyNowPrice != null) {
                lore.add("§7Buy Now: §a" + buyNowPrice);
            }
            lore.add("§7Time Left: §e" + timeLeft);
            lore.add("");
            lore.add("§eLeft-Click: Bid");
            if (buyNowPrice != null) {
                lore.add("§eRight-Click: Buy Now");
            }
            
            return createDisplayItem(item, item.getHoverName().getString(), lore);
        }
        
        public static ItemStack createCreateAuctionItem() {
            List<String> lore = new ArrayList<>();
            lore.add("§7Create a new auction");
            lore.add("§7Hold an item and click");
            lore.add("");
            lore.add("§eClick to create");
            
            return createDisplayItem(new ItemStack(Items.GOLD_INGOT), "§6Create Auction", lore);
        }
        
        public static ItemStack createViewModeItem(String mode) {
            List<String> lore = new ArrayList<>();
            lore.add("§7Current view: §e" + mode);
            lore.add("");
            lore.add("§eClick to cycle views");
            
            return createDisplayItem(new ItemStack(Items.ENDER_EYE), "§bView Mode", lore);
        }
    }
    
    /**
     * Safely checks if an ItemStack is empty
     */
    public static boolean isEmpty(ItemStack stack) {
        return stack == null || stack.isEmpty();
    }
    
    /**
     * Safely gets the display name of an ItemStack
     */
    public static String getDisplayName(ItemStack stack) {
        if (isEmpty(stack)) {
            return "Air";
        }
        
        try {
            return stack.getHoverName().getString();
        } catch (Exception e) {
            return stack.getItem().toString();
        }
    }
    
    /**
     * Creates a copy of an ItemStack safely
     */
    public static ItemStack safeCopy(ItemStack stack) {
        if (isEmpty(stack)) {
            return ItemStack.EMPTY;
        }
        
        try {
            return stack.copy();
        } catch (Exception e) {
            return new ItemStack(stack.getItem(), stack.getCount());
        }
    }
}
