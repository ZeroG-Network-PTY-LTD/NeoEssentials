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

import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin interface for managing shop items
 */
public class AdminShopManagementInterface {
    
    private static final int CONTAINER_SIZE = 54; // 6x9 slots
    private static final int ITEMS_PER_PAGE = 45;
    
    /**
     * Opens the admin shop management interface
     */
    public static void openAdminShopManagement(ServerPlayer player, EconomyManager economyManager) {
        openAdminShopManagement(player, economyManager, 0);
    }
    
    public static void openAdminShopManagement(ServerPlayer player, EconomyManager economyManager, int page) {
        try {
            NeoEssentials.LOGGER.info("Opening admin shop management for player {}", player.getName().getString());
            
            if (economyManager == null) {
                player.sendSystemMessage(Component.literal("§cEconomy manager is not available"));
                return;
            }
            
            ShopManager shopManager = economyManager.getShopManager();
            if (shopManager == null) {
                player.sendSystemMessage(Component.literal("§cShop manager is not available"));
                return;
            }
            
            // Get all admin items
            List<ShopItem> adminItems = shopManager.getAllItems().stream()
                .filter(ShopItem::isAdminItem)
                .collect(Collectors.toList());
            
            // Create container
            SimpleContainer container = new SimpleContainer(CONTAINER_SIZE);
            
            // Setup admin items
            setupAdminItems(container, adminItems, page);
            
            // Setup control buttons
            setupControlButtons(container, player, economyManager, page, adminItems.size());
            
            // Create menu
            String title = "§4Admin Shop Management (Page " + (page + 1) + ")";
            SimpleMenuProvider menuProvider = new SimpleMenuProvider(
                (containerId, inventory, menuPlayer) -> {
                    return new AdminShopManagementMenu(containerId, inventory, container, player, 
                                                     economyManager, page, adminItems);
                },
                Component.literal(title)
            );
            
            player.openMenu(menuProvider);
            NeoEssentials.LOGGER.info("Opened admin shop management for player {} with {} items", 
                                    player.getName().getString(), adminItems.size());
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to open admin shop management", e);
            player.sendSystemMessage(Component.literal("§cFailed to open admin shop management: " + e.getMessage()));
        }
    }
    
    private static void setupAdminItems(SimpleContainer container, List<ShopItem> adminItems, int page) {
        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, adminItems.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            int slotIndex = i - startIndex;
            if (slotIndex < 45) { // Only fill the first 45 slots
                ShopItem item = adminItems.get(i);
                ItemStack displayItem = createAdminDisplayItem(item);
                container.setItem(slotIndex, displayItem);
            }
        }
    }
    
    private static ItemStack createAdminDisplayItem(ShopItem shopItem) {
        ItemStack displayItem = shopItem.getItemStack().copy();
        
        // Enhanced display name with admin info
        String itemName = displayItem.getHoverName().getString();
        String displayName = String.format("§c[ADMIN] §f%s", itemName);
        
        // Add price info
        if (shopItem.getBuyPrice() != null) {
            displayName += " §7- §a" + shopItem.getCurrency().format(shopItem.getBuyPrice());
        }
        
        // Add stock info
        if (shopItem.getStock() < 0) {
            displayName += " §7[§aInfinite§7]";
        } else {
            displayName += " §7[§e" + shopItem.getStock() + "§7]";
        }
        
        displayItem.set(DataComponents.CUSTOM_NAME, Component.literal(displayName));
        
        return displayItem;
    }
    
    private static void setupControlButtons(SimpleContainer container, ServerPlayer player, 
                                          EconomyManager economyManager, int page, int totalItems) {
        int totalPages = (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE);
        
        // Navigation buttons
        if (page > 0) {
            ItemStack prevPage = new ItemStack(Items.ARROW);
            prevPage.set(DataComponents.CUSTOM_NAME, Component.literal("§ePrevious Page"));
            container.setItem(45, prevPage);
        }
        
        if (page < totalPages - 1) {
            ItemStack nextPage = new ItemStack(Items.ARROW);
            nextPage.set(DataComponents.CUSTOM_NAME, Component.literal("§eNext Page"));
            container.setItem(53, nextPage);
        } else {
            // Close button if no next page
            ItemStack close = new ItemStack(Items.BARRIER);
            close.set(DataComponents.CUSTOM_NAME, Component.literal("§cClose"));
            container.setItem(53, close);
        }
        
        // Add item button
        ItemStack addItem = new ItemStack(Items.EMERALD);
        addItem.set(DataComponents.CUSTOM_NAME, Component.literal("§aAdd Item to Admin Shop"));
        container.setItem(46, addItem);
        
        // Remove all button
        ItemStack removeAll = new ItemStack(Items.TNT);
        removeAll.set(DataComponents.CUSTOM_NAME, Component.literal("§cClear All Admin Items"));
        container.setItem(47, removeAll);
        
        // Reload defaults button
        ItemStack reloadDefaults = new ItemStack(Items.BOOK);
        reloadDefaults.set(DataComponents.CUSTOM_NAME, Component.literal("§6Reload Default Items"));
        container.setItem(48, reloadDefaults);
        
        // Back to shop button
        ItemStack backToShop = new ItemStack(Items.CHEST);
        backToShop.set(DataComponents.CUSTOM_NAME, Component.literal("§bBack to Shop"));
        container.setItem(49, backToShop);
        
        // Page info
        ItemStack pageInfo = new ItemStack(Items.PAPER);
        String pageText = totalPages > 1 ? 
            String.format("§7Page %d of %d", page + 1, totalPages) :
            String.format("§7%d admin items", totalItems);
        pageInfo.set(DataComponents.CUSTOM_NAME, Component.literal(pageText));
        container.setItem(50, pageInfo);
        
        // Player shop button
        ItemStack playerShop = new ItemStack(Items.ENDER_CHEST);
        playerShop.set(DataComponents.CUSTOM_NAME, Component.literal("§bView Player Shops"));
        container.setItem(51, playerShop);
        
        // Debug button
        ItemStack debugButton = new ItemStack(Items.REDSTONE);
        debugButton.set(DataComponents.CUSTOM_NAME, Component.literal("§cDebug Shop"));
        container.setItem(52, debugButton);
        
        // Fill empty spots with glass panes
        for (int i = 45; i < 54; i++) {
            if (container.getItem(i).isEmpty()) {
                ItemStack glassPane = new ItemStack(Items.BLACK_STAINED_GLASS_PANE);
                glassPane.set(DataComponents.CUSTOM_NAME, Component.literal(""));
                container.setItem(i, glassPane);
            }
        }
    }
}
