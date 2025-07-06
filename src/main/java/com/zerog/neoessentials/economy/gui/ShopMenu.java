package com.zerog.neoessentials.economy.gui;

import com.zerog.neoessentials.economy.EconomyManager;
import com.zerog.neoessentials.economy.shop.ShopItem;
import com.zerog.neoessentials.economy.shop.ShopManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * GUI menu for browsing and purchasing shop items
 */
public class ShopMenu extends BaseEconomyMenu {
    
    private final EconomyManager economyManager;
    private final ShopManager shopManager;
    private List<ShopItem> currentItems;
    private int currentPage = 0;
    private static final int ITEMS_PER_PAGE = 45; // 5 rows for items, 1 row for navigation
    
    public ShopMenu(int containerId, Inventory playerInventory, EconomyManager economyManager) {
        super(MenuType.GENERIC_9x6, containerId, playerInventory, 6);
        this.economyManager = economyManager;
        this.shopManager = economyManager.getShopManager();
        
        setupClickHandler();
        refreshItems();
        updateDisplay();
    }
    
    private void setupClickHandler() {
        this.clickHandler = (slot, clickType) -> {
            if (slot < 0 || slot >= container.getContainerSize()) return;
            
            ItemStack clickedItem = container.getItem(slot);
            if (clickedItem.isEmpty()) return;
            
            // Navigation items (bottom row)
            if (slot >= 45) {
                handleNavigationClick(slot, clickType);
                return;
            }
            
            // Shop items
            int itemIndex = currentPage * ITEMS_PER_PAGE + slot;
            if (itemIndex >= 0 && itemIndex < currentItems.size()) {
                ShopItem shopItem = currentItems.get(itemIndex);
                handleShopItemClick(shopItem, clickType);
            }
        };
    }
    
    private void handleNavigationClick(int slot, ClickType clickType) {
        switch (slot) {
            case 45: // Previous page
                if (currentPage > 0) {
                    currentPage--;
                    updateDisplay();
                }
                break;
            case 49: // Refresh/Categories
                refreshItems();
                updateDisplay();
                break;
            case 53: // Next page
                int maxPages = (currentItems.size() - 1) / ITEMS_PER_PAGE;
                if (currentPage < maxPages) {
                    currentPage++;
                    updateDisplay();
                }
                break;
        }
    }
    
    private void handleShopItemClick(ShopItem shopItem, ClickType clickType) {
        if (clickType == ClickType.PICKUP) {
            // Left click - buy 1
            buyItem(shopItem, 1);
        } else if (clickType == ClickType.PICKUP_ALL) {
            // Right click - buy stack
            buyItem(shopItem, shopItem.getItemStack().getMaxStackSize());
        } else if (clickType == ClickType.QUICK_MOVE) {
            // Shift click - show info
            showItemInfo(shopItem);
        }
    }
    
    private void buyItem(ShopItem shopItem, int quantity) {
        ShopManager.BuyResult result = shopManager.buyItem(player, shopItem.getId(), quantity);
        
        if (result.isSuccess()) {
            player.sendSystemMessage(Component.literal("§aPurchased " + quantity + "x " + 
                shopItem.getItemStack().getHoverName().getString() + " successfully!"));
        } else {
            player.sendSystemMessage(Component.literal("§cPurchase failed: " + result.getMessage()));
        }
    }
    
    private void showItemInfo(ShopItem shopItem) {
        player.sendSystemMessage(Component.literal("§6=== Shop Item Info ==="));
        player.sendSystemMessage(Component.literal("§7Item: §b" + shopItem.getItemStack().getHoverName().getString()));
        player.sendSystemMessage(Component.literal("§7Price: §a" + economyManager.formatCurrency(shopItem.getPrice())));
        player.sendSystemMessage(Component.literal("§7Stock: §e" + (shopItem.getStock() == -1 ? "Unlimited" : shopItem.getStock())));
        if (shopItem.getDescription() != null && !shopItem.getDescription().isEmpty()) {
            player.sendSystemMessage(Component.literal("§7Description: §f" + shopItem.getDescription()));
        }
    }
    
    private void refreshItems() {
        currentItems = shopManager.getAllItems();
        currentPage = 0;
    }
    
    private void updateDisplay() {
        // Clear container
        for (int i = 0; i < container.getContainerSize(); i++) {
            container.setItem(i, ItemStack.EMPTY);
        }
        
        // Add shop items
        int startIndex = currentPage * ITEMS_PER_PAGE;
        for (int i = 0; i < ITEMS_PER_PAGE && (startIndex + i) < currentItems.size(); i++) {
            ShopItem shopItem = currentItems.get(startIndex + i);
            ItemStack displayItem = shopItem.getItemStack().copy();
            
            // Add lore with price information            // Note: Setting NBT directly on displayed items for price info
            // In newer MC versions, use hover text through lore instead
            
            container.setItem(i, displayItem);
        }
        
        // Add navigation items
        if (currentPage > 0) {
            ItemStack prevPage = new ItemStack(Items.ARROW);
            // prevPage.setHoverName(Component.literal("§ePrevious Page"));
            // Note: setHoverName may not be available in this MC version
            container.setItem(45, prevPage);
        }
        
        ItemStack refresh = new ItemStack(Items.COMPASS);
        // refresh.setHoverName(Component.literal("§eRefresh Shop"));
        // Note: setHoverName may not be available in this MC version
        container.setItem(49, refresh);
        
        int maxPages = (currentItems.size() - 1) / ITEMS_PER_PAGE;
        if (currentPage < maxPages) {
            ItemStack nextPage = new ItemStack(Items.ARROW);
            // nextPage.setHoverName(Component.literal("§eNext Page"));
            // Note: setHoverName may not be available in this MC version
            container.setItem(53, nextPage);
        }
        
        // Add page info
        ItemStack pageInfo = new ItemStack(Items.BOOK);
        // pageInfo.setHoverName(Component.literal("§ePage " + (currentPage + 1) + "/" + (maxPages + 1)));
        // Note: setHoverName may not be available in this MC version
        container.setItem(48, pageInfo);
    }
    
    @Override
    public boolean clickMenuButton(@javax.annotation.Nonnull net.minecraft.world.entity.player.Player player, int id) {
        if (clickHandler != null && player instanceof ServerPlayer) {
            clickHandler.accept(id, ClickType.PICKUP);
            return true;
        }
        return false;
    }
    
    @Override
    public void refresh() {
        refreshItems();
        updateDisplay();
    }
    
    @Override
    public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player, int index) {
        // Don't allow items to be moved
        return ItemStack.EMPTY;
    }
}
