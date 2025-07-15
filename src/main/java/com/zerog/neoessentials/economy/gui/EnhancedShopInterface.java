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

import java.math.BigDecimal;
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
            NeoEssentials.LOGGER.info("Opening shop GUI for player {} in mode {} on page {}", 
                player.getName().getString(), mode, page);
            
            // Comprehensive validation
            if (economyManager == null) {
                player.sendSystemMessage(Component.literal("§cEconomy manager is not available"));
                NeoEssentials.LOGGER.error("Economy manager is null when opening shop for player {}", player.getName().getString());
                return;
            }
            
            if (!economyManager.isInitialized()) {
                player.sendSystemMessage(Component.literal("§cEconomy system is not initialized"));
                NeoEssentials.LOGGER.error("Economy system not initialized when opening shop for player {}", player.getName().getString());
                return;
            }
            
            if (!economyManager.isEnabled()) {
                player.sendSystemMessage(Component.literal("§cEconomy system is disabled"));
                player.sendSystemMessage(Component.literal("§7Use '/economy enable' to enable the economy system"));
                NeoEssentials.LOGGER.warn("Economy system is disabled when player {} tried to open shop", player.getName().getString());
                return;
            }
            
            ShopManager shopManager = economyManager.getShopManager();
            if (shopManager == null) {
                player.sendSystemMessage(Component.literal("§cShop system is not available"));
                NeoEssentials.LOGGER.error("Shop manager is null when opening shop for player {}", player.getName().getString());
                return;
            }
            
            // Validate player account
            if (!economyManager.validatePlayerAccount(player.getUUID())) {
                // Try to create account
                String playerName = player.getName().getString();
                var account = economyManager.getOrCreateAccount(player.getUUID(), playerName);
                if (account == null) {
                    player.sendSystemMessage(Component.literal("§cFailed to access your economy account"));
                    NeoEssentials.LOGGER.error("Failed to create account for player {}", playerName);
                    return;
                }
                NeoEssentials.LOGGER.info("Created new account for player {} opening shop", playerName);
            }
            
            // Get shop items with comprehensive logging
            List<ShopItem> shopItems = getShopItems(shopManager, mode, targetPlayer, player);
            NeoEssentials.LOGGER.info("Found {} shop items for player {} in mode {} (total in shop: {})", 
                shopItems.size(), player.getName().getString(), mode, shopManager.getAllItems().size());
            
            // Detailed diagnostics if shop is empty
            if (shopItems.isEmpty()) {
                List<ShopItem> allItems = shopManager.getAllItems();
                NeoEssentials.LOGGER.info("Shop diagnostics for empty result:");
                NeoEssentials.LOGGER.info("  - All items in shop: {}", allItems.size());
                NeoEssentials.LOGGER.info("  - Mode: {}", mode);
                NeoEssentials.LOGGER.info("  - Target player: {}", targetPlayer);
                NeoEssentials.LOGGER.info("  - Viewer: {}", player.getUUID());
                
                for (ShopItem item : allItems.stream().limit(10).toList()) {
                    NeoEssentials.LOGGER.info("  - Item: {} | Stock: {} | CanBuy: {} | Type: {} | Admin: {} | Creator: {}", 
                        item.getItemStack().getHoverName().getString(), 
                        item.getStock(), item.canBuy(), item.getType(), item.isAdminItem(),
                        item.getCreatedBy());
                }
                
                // Add helpful message to player
                switch (mode) {
                    case GLOBAL -> {
                        player.sendSystemMessage(Component.literal("§eThe global shop appears to be empty"));
                        player.sendSystemMessage(Component.literal("§7This might be due to:"));
                        player.sendSystemMessage(Component.literal("§7- No default items were added"));
                        player.sendSystemMessage(Component.literal("§7- All items are out of stock"));
                        player.sendSystemMessage(Component.literal("§7- Economy system issues"));
                    }
                    case PERSONAL -> {
                        player.sendSystemMessage(Component.literal("§eYou haven't created any shop listings yet"));
                        player.sendSystemMessage(Component.literal("§7Hold an item and click 'Create Item' to add to your shop"));
                    }
                    case BROWSE -> {
                        player.sendSystemMessage(Component.literal("§eThis player has no shop items"));
                    }
                }
            }
            
            // Create container
            SimpleContainer container = new SimpleContainer(CONTAINER_SIZE);
            
            // Calculate pagination
            int totalPages = Math.max(1, (int) Math.ceil((double) shopItems.size() / ITEMS_PER_PAGE));
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
                    NeoEssentials.LOGGER.debug("Added item {} to slot {}", 
                        shopItem.getItemStack().getHoverName().getString(), slotIndex);
                }
            }
            
            // Setup navigation and controls
            setupNavigationItems(container, mode, targetPlayer, player, page, totalPages, shopItems.size());
            
            // Show player balance
            try {
                BigDecimal balance = economyManager.getBalance(player.getUUID());
                player.sendSystemMessage(Component.literal("§7Your balance: " + economyManager.formatCurrency(balance)));
            } catch (Exception e) {
                NeoEssentials.LOGGER.warn("Failed to get player balance for shop interface", e);
            }
            
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
            NeoEssentials.LOGGER.info("Successfully opened {} shop GUI for player {} (page {}/{}, {} items)", 
                                    mode, player.getName().getString(), page + 1, totalPages, shopItems.size());
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to open shop interface for player " + player.getName().getString(), e);
            player.sendSystemMessage(Component.literal("§cFailed to open shop interface: " + e.getMessage()));
            player.sendSystemMessage(Component.literal("§7Please report this error to an administrator"));
        }
    }
    
    private static List<ShopItem> getShopItems(ShopManager shopManager, ShopMode mode, UUID targetPlayer, ServerPlayer viewer) {
        try {
            List<ShopItem> items;
            
            switch (mode) {
                case GLOBAL:
                    // Get all available items (admin + player items)
                    items = shopManager.getAllItems().stream()
                        .filter(item -> item.hasStock() || item.getStock() == -1) // Include infinite stock items
                        .filter(item -> item.canBuy()) // Only buyable items
                        .filter(item -> item.isValid()) // Valid items only
                        .collect(Collectors.toList());
                    NeoEssentials.LOGGER.debug("Global shop: {} items after filtering", items.size());
                    break;
                    
                case PERSONAL:
                    // Get only viewer's own items
                    items = shopManager.getAllItems().stream()
                        .filter(item -> viewer.getUUID().equals(item.getCreatedBy()))
                        .filter(item -> item.isValid())
                        .collect(Collectors.toList());
                    NeoEssentials.LOGGER.debug("Personal shop for {}: {} items", viewer.getName().getString(), items.size());
                    break;
                    
                case BROWSE:
                    if (targetPlayer == null) {
                        NeoEssentials.LOGGER.warn("Browse mode requested but target player is null");
                        return List.of();
                    }
                    // Get specific player's items
                    items = shopManager.getAllItems().stream()
                        .filter(item -> targetPlayer.equals(item.getCreatedBy()))
                        .filter(item -> item.hasStock() || item.getStock() == -1)
                        .filter(item -> item.canBuy())
                        .filter(item -> item.isValid())
                        .collect(Collectors.toList());
                    NeoEssentials.LOGGER.debug("Browse shop for {}: {} items", targetPlayer, items.size());
                    break;
                    
                default:
                    NeoEssentials.LOGGER.warn("Unknown shop mode: {}", mode);
                    return List.of();
            }
            
            // Sort items by name for consistent display
            items.sort((a, b) -> a.getItemStack().getHoverName().getString()
                .compareToIgnoreCase(b.getItemStack().getHoverName().getString()));
            
            return items;
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error getting shop items for mode " + mode, e);
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
                
                // Add pricing information using proper currency formatting
                if (shopItem.canBuy() && shopItem.getBuyPrice() != null) {
                    nameBuilder.append(" §7- §a").append(shopItem.getCurrency().format(shopItem.getBuyPrice()));
                }
                
                if (shopItem.canSell() && shopItem.getSellPrice() != null) {
                    nameBuilder.append(" §7(Sell: §c").append(shopItem.getCurrency().format(shopItem.getSellPrice())).append("§7)");
                }
                
                // Add stock information
                if (shopItem.getStock() < 0) {
                    nameBuilder.append(" §7[§aInfinite Stock§7]");
                } else if (shopItem.getStock() > 0) {
                    nameBuilder.append(" §7[§e").append(shopItem.getStock()).append("§7 in stock]");
                } else {
                    nameBuilder.append(" §7[§cOut of stock§7]");
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
        
        // Instructions item
        ItemStack instructions = new ItemStack(Items.WRITTEN_BOOK);
        instructions.set(DataComponents.CUSTOM_NAME, Component.literal("§eInstructions"));
        java.util.List<Component> lore = new java.util.ArrayList<>();
        lore.add(Component.literal("§7Left click: Buy 1 item"));
        lore.add(Component.literal("§7Shift+Left click: Buy 64 items"));
        lore.add(Component.literal("§7Right click: Sell 1 item to shop"));
        lore.add(Component.literal("§7(Only works if shop buys the item)"));
        instructions.set(DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(lore));
        container.setItem(43, instructions);
    }
    
    private static String getShopTitle(ShopMode mode, UUID targetPlayer, ServerPlayer viewer) {
        return switch (mode) {
            case GLOBAL -> "§6Global Shop";
            case PERSONAL -> "§b" + viewer.getName().getString() + "'s Shop";
            case BROWSE -> "§ePlayer Shop"; // Could lookup player name here
        };
    }
}
