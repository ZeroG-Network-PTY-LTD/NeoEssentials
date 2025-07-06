package com.zerog.neoessentials.economy.gui;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.economy.EconomyManager;
import com.zerog.neoessentials.economy.shop.ShopItem;
import com.zerog.neoessentials.economy.shop.ShopManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI for the shop system
 */
public class ShopMenu extends BaseEconomyMenu {
    
    public static final MenuType<ShopMenu> TYPE = new MenuType<>(ShopMenu::new, FeatureFlagSet.of());
    
    private final ShopManager shopManager;
    private final List<ShopItem> currentItems;
    private int currentPage = 0;
    private static final int ITEMS_PER_PAGE = 45; // 5 rows of 9 items
    
    public ShopMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, NeoEssentials.getInstance().getEconomyManager().getShopManager());
    }
    
    public ShopMenu(int containerId, Inventory playerInventory, ShopManager shopManager) {
        super(TYPE, containerId, playerInventory, 6); // 6 rows for shop interface
        this.shopManager = shopManager;
        this.currentItems = new ArrayList<>();
        
        setupClickHandler();
        loadShopItems();
        updateDisplay();
    }
    
    private void setupClickHandler() {
        this.clickHandler = (slot, clickType) -> {
            if (slot < 0 || slot >= container.getContainerSize()) return;
            
            // Navigation buttons
            if (slot == 53) { // Next page button (bottom right)
                nextPage();
                return;
            } else if (slot == 45) { // Previous page button (bottom left)
                previousPage();
                return;
            } else if (slot == 49) { // Close button (bottom center)
                player.closeContainer();
                return;
            }
            
            // Shop item slots (0-44)
            if (slot >= 0 && slot < ITEMS_PER_PAGE) {
                int itemIndex = currentPage * ITEMS_PER_PAGE + slot;
                if (itemIndex < currentItems.size()) {
                    ShopItem shopItem = currentItems.get(itemIndex);
                    handleShopItemClick(shopItem, clickType);
                }
            }
        };
    }
    
    private void loadShopItems() {
        currentItems.clear();
        currentItems.addAll(shopManager.getAllItems());
    }
    
    private void updateDisplay() {
        // Clear container
        for (int i = 0; i < container.getContainerSize(); i++) {
            container.setItem(i, ItemStack.EMPTY);
        }
        
        // Add shop items for current page
        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, currentItems.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            ShopItem shopItem = currentItems.get(i);
            ItemStack displayItem = createShopItemDisplay(shopItem);
            container.setItem(i - startIndex, displayItem);
        }
        
        // Add navigation buttons
        addNavigationButtons();
        
        // Refresh player's view
        broadcastChanges();
    }
    
    private ItemStack createShopItemDisplay(ShopItem shopItem) {
        ItemStack display = shopItem.getItemStack().copy();
        
        // Create lore with pricing information
        List<Component> lore = new ArrayList<>();
        EconomyManager economyManager = NeoEssentials.getInstance().getEconomyManager();
        
        if (shopItem.canBuy()) {
            lore.add(Component.literal("§aBuy Price: §f" + economyManager.formatCurrency(shopItem.getBuyPrice())));
        }
        if (shopItem.canSell()) {
            lore.add(Component.literal("§cSell Price: §f" + economyManager.formatCurrency(shopItem.getSellPrice())));
        }
        
        lore.add(Component.literal(""));
        lore.add(Component.literal("§7Left Click: §aBuy §71"));
        lore.add(Component.literal("§7Right Click: §aSell §71"));
        lore.add(Component.literal("§7Shift + Left Click: §aBuy §764"));
        lore.add(Component.literal("§7Shift + Right Click: §aSell §7All"));
        
        // Set the lore
        display.getOrCreateTagElement("display").putString("Lore", lore.toString());
        
        return display;
    }
    
    private void addNavigationButtons() {
        // Previous page button (slot 45)
        if (currentPage > 0) {
            ItemStack prevButton = new ItemStack(Items.ARROW);
            prevButton.setHoverName(Component.literal("§aPrevious Page"));
            container.setItem(45, prevButton);
        }
        
        // Page info (slot 49)
        ItemStack pageInfo = new ItemStack(Items.BOOK);
        int totalPages = (int) Math.ceil((double) currentItems.size() / ITEMS_PER_PAGE);
        pageInfo.setHoverName(Component.literal("§ePage " + (currentPage + 1) + " of " + totalPages));
        container.setItem(49, pageInfo);
        
        // Next page button (slot 53)
        if ((currentPage + 1) * ITEMS_PER_PAGE < currentItems.size()) {
            ItemStack nextButton = new ItemStack(Items.ARROW);
            nextButton.setHoverName(Component.literal("§aNext Page"));
            container.setItem(53, nextButton);
        }
        
        // Close button (slot 4 in bottom row for visibility)
        ItemStack closeButton = new ItemStack(Items.BARRIER);
        closeButton.setHoverName(Component.literal("§cClose"));
        container.setItem(4, closeButton);
    }
    
    private void handleShopItemClick(ShopItem shopItem, ClickType clickType) {
        int quantity = 1;
        boolean isBuy = true;
        
        switch (clickType) {
            case LEFT:
                // Buy 1
                quantity = 1;
                isBuy = true;
                break;
            case RIGHT:
                // Sell 1
                quantity = 1;
                isBuy = false;
                break;
            case SHIFT_LEFT:
                // Buy 64
                quantity = 64;
                isBuy = true;
                break;
            case SHIFT_RIGHT:
                // Sell all
                quantity = -1; // Special value for "all"
                isBuy = false;
                break;
            default:
                return;
        }
        
        // Execute the transaction
        if (isBuy) {
            if (shopItem.canBuy()) {
                shopManager.buyItem(player, shopItem.getId(), quantity);
            }
        } else {
            if (shopItem.canSell()) {
                if (quantity == -1) {
                    // Sell all - count items in inventory
                    quantity = countItemsInInventory(shopItem.getItemStack());
                }
                if (quantity > 0) {
                    shopManager.sellItem(player, shopItem.getId(), quantity);
                }
            }
        }
        
        // Refresh display
        updateDisplay();
    }
    
    private int countItemsInInventory(ItemStack targetItem) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (ItemStack.isSameItemSameTags(stack, targetItem)) {
                count += stack.getCount();
            }
        }
        return count;
    }
    
    private void nextPage() {
        int totalPages = (int) Math.ceil((double) currentItems.size() / ITEMS_PER_PAGE);
        if (currentPage < totalPages - 1) {
            currentPage++;
            updateDisplay();
        }
    }
    
    private void previousPage() {
        if (currentPage > 0) {
            currentPage--;
            updateDisplay();
        }
    }
    
    @Override
    public boolean stillValid(Player player) {
        return true; // Virtual container is always valid
    }
    
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // Disable shift-clicking to move items
        return ItemStack.EMPTY;
    }
    
    public static class Provider implements MenuProvider {
        private final ShopManager shopManager;
        
        public Provider(ShopManager shopManager) {
            this.shopManager = shopManager;
        }
        
        @Override
        public Component getDisplayName() {
            return Component.literal("Shop");
        }
        
        @Override
        public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
            return new ShopMenu(containerId, playerInventory, shopManager);
        }
    }
    
    /**
     * Opens the shop GUI for a player
     */
    public static void openFor(ServerPlayer player) {
        EconomyManager economyManager = NeoEssentials.getInstance().getEconomyManager();
        if (economyManager != null && economyManager.isEnabled()) {
            ShopManager shopManager = economyManager.getShopManager();
            player.openMenu(new Provider(shopManager));
        } else {
            player.sendSystemMessage(Component.literal("§cShop system is not available"));
        }
    }
}
