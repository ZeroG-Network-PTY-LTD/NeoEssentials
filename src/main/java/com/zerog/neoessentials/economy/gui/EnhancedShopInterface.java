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
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Enhanced shop interface with full functionality
 */
public class EnhancedShopInterface {
    
    public enum ShopMode {
        GLOBAL,     // Global shop (all items)
        PERSONAL,   // Player's own shops
        BROWSE      // Browse other players' shops
    }
    
    private static final int ITEMS_PER_PAGE = 36;
    private static final int CONTAINER_SIZE = 45;
    
    public static void openShop(ServerPlayer player, EconomyManager economyManager) {
        openShop(player, economyManager, ShopMode.GLOBAL, null, 0);
    }
    
    public static void openPersonalShop(ServerPlayer player, EconomyManager economyManager) {
        openShop(player, economyManager, ShopMode.PERSONAL, player.getUUID(), 0);
    }
    
    public static void openPlayerShop(ServerPlayer player, EconomyManager economyManager, UUID shopOwner) {
        openShop(player, economyManager, ShopMode.BROWSE, shopOwner, 0);
    }
    
    public static void openShop(ServerPlayer player, EconomyManager economyManager, ShopMode mode, UUID targetPlayer, int page) {
        try {
            ShopManager shopManager = economyManager.getShopManager();
            if (shopManager == null) {
                player.sendSystemMessage(Component.literal("§cShop system is not available"));
                return;
            }
            
            List<ShopItem> shopItems = getShopItems(shopManager, mode, targetPlayer, player);
            
            // Create container
            SimpleContainer container = new SimpleContainer(CONTAINER_SIZE);
            
            // Calculate pagination
            int totalPages = (int) Math.ceil((double) shopItems.size() / ITEMS_PER_PAGE);
            int startIndex = page * ITEMS_PER_PAGE;
            int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, shopItems.size());
            
            // Fill with items
            if (shopItems.isEmpty()) {
                setupEmptyShop(container, mode, targetPlayer, player);
            } else {
                for (int i = startIndex; i < endIndex; i++) {
                    int slotIndex = i - startIndex;
                    ShopItem shopItem = shopItems.get(i);
                    ItemStack displayItem = createEnhancedDisplayItem(shopItem, player, mode);
                    container.setItem(slotIndex, displayItem);
                }
            }
            
            // Setup navigation and controls
            setupNavigationItems(container, mode, targetPlayer, player, page, totalPages, shopItems.size());
            
            // Create menu with click handling
            String title = getShopTitle(mode, targetPlayer, player);
            SimpleMenuProvider menuProvider = new SimpleMenuProvider(
                (containerId, inventory, menuPlayer) -> {
                    return new EnhancedShopMenu(containerId, inventory, container, player, 
                                               economyManager, mode, targetPlayer, page, shopItems);
                },
                Component.literal(title)
            );
            
            player.openMenu(menuProvider);
            NeoEssentials.LOGGER.info("Opened {} shop GUI for player {} (page {})", 
                                    mode, player.getName().getString(), page);
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to open shop interface for player " + player.getName().getString(), e);
            player.sendSystemMessage(Component.literal("§cFailed to open shop interface. Please try again."));
        }
    }
    
    private static List<ShopItem> getShopItems(ShopManager shopManager, ShopMode mode, UUID targetPlayer, ServerPlayer viewer) {
        switch (mode) {
            case GLOBAL:
                return shopManager.getAvailableItems();
            case PERSONAL:
                return shopManager.getAvailableItems().stream()
                    .filter(item -> viewer.getUUID().equals(item.getCreatedBy()))
                    .collect(Collectors.toList());
            case BROWSE:
                if (targetPlayer == null) return List.of();
                return shopManager.getAvailableItems().stream()
                    .filter(item -> targetPlayer.equals(item.getCreatedBy()))
                    .collect(Collectors.toList());
            default:
                return List.of();
        }
    }
    
    private static void setupEmptyShop(SimpleContainer container, ShopMode mode, UUID targetPlayer, ServerPlayer player) {
        ItemStack emptyInfo = new ItemStack(Items.PAPER);
        String message = switch (mode) {
            case GLOBAL -> "§eNo items available in the global shop";
            case PERSONAL -> "§eYou don't have any items for sale\n§7Hold an item and click 'Create Shop Item' to start selling";
            case BROWSE -> "§eThis player doesn't have any items for sale";
        };
        emptyInfo.set(DataComponents.CUSTOM_NAME, Component.literal(message));
        container.setItem(22, emptyInfo);
    }
    
    private static ItemStack createEnhancedDisplayItem(ShopItem shopItem, ServerPlayer viewer, ShopMode mode) {
        try {
            ItemStack displayItem = shopItem.getItemStack().copy();
            
            if (displayItem != null && !displayItem.isEmpty()) {
                String itemName = displayItem.getHoverName().getString();
                StringBuilder nameBuilder = new StringBuilder("§f").append(itemName);
                
                // Add pricing information
                if (shopItem.canBuy() && shopItem.getBuyPrice() != null) {
                    nameBuilder.append(" §7- §6").append(String.format("%.2f", shopItem.getBuyPrice().doubleValue())).append(" coins");
                }
                
                // Add stock information
                if (shopItem.getStock() > 0) {
                    nameBuilder.append(" §7(").append(shopItem.getStock()).append(" in stock)");
                } else if (shopItem.getStock() == 0) {
                    nameBuilder.append(" §c(Out of stock)");
                }
                
                // Add owner information if browsing
                if (mode == ShopMode.BROWSE || mode == ShopMode.GLOBAL) {
                    // Could add seller name here if we have player name lookup
                    if (!shopItem.isAdminItem()) {
                        nameBuilder.append(" §7[Player Shop]");
                    } else {
                        nameBuilder.append(" §7[Admin Shop]");
                    }
                }
                
                displayItem.set(DataComponents.CUSTOM_NAME, Component.literal(nameBuilder.toString()));
                return displayItem;
            }
        } catch (Exception e) {
            NeoEssentials.LOGGER.warn("Failed to create enhanced display item", e);
        }
        
        // Fallback
        ItemStack fallback = new ItemStack(Items.STONE);
        fallback.set(DataComponents.CUSTOM_NAME, Component.literal("§cError loading item"));
        return fallback;
    }
    
    private static void setupNavigationItems(SimpleContainer container, ShopMode mode, UUID targetPlayer, 
                                           ServerPlayer player, int currentPage, int totalPages, int totalItems) {
        try {
            // Navigation buttons
            if (currentPage > 0) {
                ItemStack prevPage = new ItemStack(Items.ARROW);
                prevPage.set(DataComponents.CUSTOM_NAME, Component.literal("§ePrevious Page"));
                container.setItem(36, prevPage);
            }
            
            if (currentPage < totalPages - 1) {
                ItemStack nextPage = new ItemStack(Items.ARROW);
                nextPage.set(DataComponents.CUSTOM_NAME, Component.literal("§eNext Page"));
                container.setItem(38, nextPage);
            }
            
            // Mode switching buttons
            setupModeButtons(container, mode, player);
            
            // Action buttons
            setupActionButtons(container, mode, player);
            
            // Info items
            ItemStack pageInfo = new ItemStack(Items.PAPER);
            String pageText = totalPages > 1 ? 
                String.format("§7Page %d of %d", currentPage + 1, totalPages) :
                String.format("§7%d items", totalItems);
            pageInfo.set(DataComponents.CUSTOM_NAME, Component.literal(pageText));
            container.setItem(40, pageInfo);
            
            // Close button
            ItemStack close = new ItemStack(Items.BARRIER);
            close.set(DataComponents.CUSTOM_NAME, Component.literal("§cClose Shop"));
            container.setItem(44, close);
            
            // Fill empty spots with glass panes
            for (int i = 37; i < 44; i++) {
                if (container.getItem(i).isEmpty() && i != 40) {
                    ItemStack glassPane = new ItemStack(Items.LIGHT_GRAY_STAINED_GLASS_PANE);
                    glassPane.set(DataComponents.CUSTOM_NAME, Component.literal(""));
                    container.setItem(i, glassPane);
                }
            }
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.warn("Failed to setup navigation items", e);
        }
    }
    
    private static void setupModeButtons(SimpleContainer container, ShopMode currentMode, ServerPlayer player) {
        // Global shop button
        if (currentMode != ShopMode.GLOBAL) {
            ItemStack globalShop = new ItemStack(Items.EMERALD_BLOCK);
            globalShop.set(DataComponents.CUSTOM_NAME, Component.literal("§aGlobal Shop"));
            container.setItem(37, globalShop);
        }
        
        // Personal shop button
        if (currentMode != ShopMode.PERSONAL) {
            ItemStack personalShop = new ItemStack(Items.ENDER_CHEST);
            personalShop.set(DataComponents.CUSTOM_NAME, Component.literal("§bMy Shop"));
            container.setItem(39, personalShop);
        }
    }
    
    private static void setupActionButtons(SimpleContainer container, ShopMode mode, ServerPlayer player) {
        if (mode == ShopMode.PERSONAL) {
            // Create new shop item button
            ItemStack createItem = new ItemStack(Items.NETHER_STAR);
            createItem.set(DataComponents.CUSTOM_NAME, Component.literal("§6Create Shop Item"));
            container.setItem(41, createItem);
        }
        
        // Refresh button
        ItemStack refresh = new ItemStack(Items.LIME_DYE);
        refresh.set(DataComponents.CUSTOM_NAME, Component.literal("§aRefresh"));
        container.setItem(42, refresh);
    }
    
    private static String getShopTitle(ShopMode mode, UUID targetPlayer, ServerPlayer viewer) {
        return switch (mode) {
            case GLOBAL -> "§6Global Shop";
            case PERSONAL -> "§b" + viewer.getName().getString() + "'s Shop";
            case BROWSE -> "§ePlayer Shop"; // Could lookup player name here
        };
    }
}
