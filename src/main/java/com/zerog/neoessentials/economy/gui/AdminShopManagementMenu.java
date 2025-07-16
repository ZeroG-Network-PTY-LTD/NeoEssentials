package com.zerog.neoessentials.economy.gui;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.economy.EconomyManager;
import com.zerog.neoessentials.economy.shop.ShopItem;
import com.zerog.neoessentials.economy.shop.ShopManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
<<<<<<< HEAD
import java.math.BigDecimal;
=======
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Menu for admin shop management with click handling
 */
public class AdminShopManagementMenu extends ChestMenu {
    
    private final ServerPlayer player;
    private final EconomyManager economyManager;
    private final int currentPage;
    private final List<ShopItem> adminItems;
    private final Container adminContainer;
    
    public AdminShopManagementMenu(int containerId, Inventory playerInventory, Container container,
                                  ServerPlayer player, EconomyManager economyManager, 
                                  int currentPage, List<ShopItem> adminItems) {
        super(MenuType.GENERIC_9x6, containerId, playerInventory, container, 6);
        this.player = player;
        this.economyManager = economyManager;
        this.currentPage = currentPage;
        this.adminItems = adminItems;
        this.adminContainer = container;
    }
    
    @Override
    public boolean stillValid(@Nonnull net.minecraft.world.entity.player.Player menuPlayer) {
        return menuPlayer == player && menuPlayer.isAlive() && !menuPlayer.isRemoved();
    }
    
    @Override
    public void clicked(int slotIndex, int dragType, @Nonnull ClickType clickType, @Nonnull net.minecraft.world.entity.player.Player menuPlayer) {
        if (menuPlayer != player) return;
        
        // Prevent taking items from admin container
        if (slotIndex < 54) {
            handleClick(slotIndex, clickType);
            return;
        }
        
        // Allow normal inventory interactions
        super.clicked(slotIndex, dragType, clickType, menuPlayer);
    }
    
    private void handleClick(int slotIndex, ClickType clickType) {
        try {
            ItemStack clickedItem = this.getSlot(slotIndex).getItem();
            
            if (clickedItem.isEmpty()) return;
            
            // Handle control buttons (bottom row)
            if (slotIndex >= 45) {
                handleControlClick(slotIndex, clickType, clickedItem);
                return;
            }
            
            // Handle admin item clicks
            if (slotIndex < 45) {
                handleAdminItemClick(slotIndex, clickType, clickedItem);
                return;
            }
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error handling admin shop click", e);
            player.sendSystemMessage(Component.literal("§cAn error occurred while processing your click"));
        }
    }
    
    private void handleControlClick(int slotIndex, ClickType clickType, ItemStack clickedItem) {
        String itemName = clickedItem.getHoverName().getString();
        
        switch (slotIndex) {
            case 45: // Previous page
                if (itemName.contains("Previous")) {
                    AdminShopManagementInterface.openAdminShopManagement(player, economyManager, currentPage - 1);
                }
                break;
                
            case 46: // Add item
                if (itemName.contains("Add Item")) {
                    handleAddItem();
                }
                break;
                
            case 47: // Clear all
                if (itemName.contains("Clear All")) {
                    handleClearAll();
                }
                break;
                
            case 48: // Reload defaults
                if (itemName.contains("Reload Default")) {
                    handleReloadDefaults();
                }
                break;
                
            case 49: // Back to shop
                if (itemName.contains("Back to Shop")) {
                    player.closeContainer();
                    try {
                        var server = player.getServer();
                        if (server != null) {
                            server.execute(() -> {
                                EnhancedShopInterface.openShop(player, economyManager);
                            });
                        } else {
                            EnhancedShopInterface.openShop(player, economyManager);
                        }
                    } catch (Exception e) {
                        NeoEssentials.LOGGER.error("Error opening shop", e);
                    }
                }
                break;
                
            case 51: // Player shops
                if (itemName.contains("Player Shops")) {
                    handleViewPlayerShops();
                }
                break;
                
            case 52: // Debug button
                if (itemName.contains("Debug")) {
                    handleDebugShop();
                }
                break;
                
            case 53: // Next page or Close
                if (itemName.contains("Next")) {
                    AdminShopManagementInterface.openAdminShopManagement(player, economyManager, currentPage + 1);
                } else if (itemName.contains("Close")) {
                    player.closeContainer();
                }
                break;
        }
    }
    
    private void handleAdminItemClick(int slotIndex, ClickType clickType, ItemStack clickedItem) {
        // Calculate which admin item this corresponds to
        int itemIndex = (currentPage * 45) + slotIndex;
        
        if (itemIndex >= adminItems.size()) {
            return; // Invalid item index
        }
        
        ShopItem adminItem = adminItems.get(itemIndex);
        
        if (adminItem == null) {
            player.sendSystemMessage(Component.literal("§cAdmin item not found"));
            return;
        }
        
        // Handle different click types
        switch (clickType) {
            case PICKUP: // Left click - Edit price
                handleEditPrice(adminItem);
                break;
                
            case QUICK_MOVE: // Shift + left click - Remove item
                handleRemoveItem(adminItem);
                break;
                
            case PICKUP_ALL: // Double click - Duplicate item
                handleDuplicateItem(adminItem);
                break;
                
            case QUICK_CRAFT: // Right click - Toggle buy/sell
                handleToggleBuySell(adminItem);
                break;
                
            default:
                // Default action - show item info
                showItemInfo(adminItem);
                break;
        }
    }
    
    private void handleAddItem() {
        try {
            ItemStack heldItem = player.getMainHandItem();
            
            if (heldItem.isEmpty()) {
                player.sendSystemMessage(Component.literal("§cYou must be holding an item to add to the admin shop"));
                return;
            }
            
            player.closeContainer();
            
            // Open admin item creation interface
            AdminItemCreationInterface.openAdminItemCreation(player, economyManager, heldItem);
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error adding admin item", e);
            player.sendSystemMessage(Component.literal("§cError adding admin item"));
        }
    }
    
    private void handleClearAll() {
        try {
            ShopManager shopManager = economyManager.getShopManager();
            List<ShopItem> allAdminItems = shopManager.getAllItems().stream()
                .filter(ShopItem::isAdminItem)
                .toList();
            
            int removedCount = 0;
            for (ShopItem item : allAdminItems) {
                if (shopManager.removeShopItem(item.getId())) {
                    removedCount++;
                }
            }
            
            player.sendSystemMessage(Component.literal("§aRemoved " + removedCount + " admin items"));
            
            // Refresh the GUI
            AdminShopManagementInterface.openAdminShopManagement(player, economyManager, 0);
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error clearing admin items", e);
            player.sendSystemMessage(Component.literal("§cError clearing admin items"));
        }
    }
    
    private void handleReloadDefaults() {
        try {
            // Clear existing admin items
            ShopManager shopManager = economyManager.getShopManager();
            List<ShopItem> allAdminItems = shopManager.getAllItems().stream()
                .filter(ShopItem::isAdminItem)
                .toList();
            
            for (ShopItem item : allAdminItems) {
                shopManager.removeShopItem(item.getId());
            }
            
            // Add default items
            com.zerog.neoessentials.economy.shop.ShopUtils.addDefaultShopItems(economyManager);
            
            player.sendSystemMessage(Component.literal("§aReloaded default admin shop items"));
            
            // Refresh the GUI
            AdminShopManagementInterface.openAdminShopManagement(player, economyManager, 0);
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error reloading defaults", e);
            player.sendSystemMessage(Component.literal("§cError reloading defaults"));
        }
    }
    
    private void handleViewPlayerShops() {
        // TODO: Implement player shop browsing for admins
        player.sendSystemMessage(Component.literal("§ePlayer shop browsing not yet implemented"));
    }
    
    private void handleEditPrice(ShopItem adminItem) {
        try {
            player.closeContainer();
            
            // Open price editing interface
            AdminPriceEditInterface.openPriceEdit(player, economyManager, adminItem);
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error opening price edit interface", e);
            player.sendSystemMessage(Component.literal("§cError opening price edit interface"));
        }
    }
    
    private void handleRemoveItem(ShopItem adminItem) {
        try {
            ShopManager shopManager = economyManager.getShopManager();
            if (shopManager.removeShopItem(adminItem.getId())) {
                player.sendSystemMessage(Component.literal("§aRemoved " + 
                    adminItem.getItemStack().getHoverName().getString() + " from admin shop"));
                
                // Refresh the GUI
                AdminShopManagementInterface.openAdminShopManagement(player, economyManager, currentPage);
            } else {
                player.sendSystemMessage(Component.literal("§cFailed to remove item"));
            }
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error removing admin item", e);
            player.sendSystemMessage(Component.literal("§cError removing item"));
        }
    }
    
    private void handleDuplicateItem(ShopItem adminItem) {
        try {
            ShopItem duplicatedItem = new ShopItem.Builder()
                .id(UUID.randomUUID())
                .itemStack(adminItem.getItemStack().copy())
                .type(adminItem.getType())
                .buyPrice(adminItem.getBuyPrice())
                .sellPrice(adminItem.getSellPrice())
                .currency(adminItem.getCurrency())
                .stock(-1) // Infinite stock
                .maxStock(-1)
                .createdBy(null) // Admin item
                .createdAt(LocalDateTime.now())
                .description("Duplicated admin item")
                .adminItem(true)
                .build();
            
            ShopManager shopManager = economyManager.getShopManager();
            if (shopManager.addShopItem(duplicatedItem)) {
                player.sendSystemMessage(Component.literal("§aDuplicated " + 
                    adminItem.getItemStack().getHoverName().getString()));
                
                // Refresh the GUI
                AdminShopManagementInterface.openAdminShopManagement(player, economyManager, currentPage);
            } else {
                player.sendSystemMessage(Component.literal("§cFailed to duplicate item"));
            }
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error duplicating admin item", e);
            player.sendSystemMessage(Component.literal("§cError duplicating item"));
        }
    }
    
    private void handleToggleBuySell(ShopItem adminItem) {
        try {
            ShopManager shopManager = economyManager.getShopManager();
            
            // Determine new type
            ShopItem.Type newType = switch (adminItem.getType()) {
                case BUY -> ShopItem.Type.SELL;
                case SELL -> ShopItem.Type.BOTH;
                case BOTH -> ShopItem.Type.BUY;
            };
            
            // Create updated item
            ShopItem updatedItem = new ShopItem.Builder()
                .id(adminItem.getId())
                .itemStack(adminItem.getItemStack())
                .type(newType)
                .buyPrice(adminItem.getBuyPrice())
                .sellPrice(adminItem.getSellPrice())
                .currency(adminItem.getCurrency())
                .stock(adminItem.getStock())
                .maxStock(adminItem.getMaxStock())
                .createdBy(adminItem.getCreatedBy())
                .createdAt(adminItem.getCreatedAt())
                .description(adminItem.getDescription())
                .adminItem(adminItem.isAdminItem())
                .build();
            
            // Update in shop
            if (shopManager.removeShopItem(adminItem.getId()) && shopManager.addShopItem(updatedItem)) {
                player.sendSystemMessage(Component.literal("§aToggled " + 
                    adminItem.getItemStack().getHoverName().getString() + " to " + newType));
                
                // Refresh the GUI
                AdminShopManagementInterface.openAdminShopManagement(player, economyManager, currentPage);
            } else {
                player.sendSystemMessage(Component.literal("§cFailed to toggle item type"));
            }
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error toggling buy/sell", e);
            player.sendSystemMessage(Component.literal("§cError toggling item type"));
        }
    }
    
    private void showItemInfo(ShopItem adminItem) {
        player.sendSystemMessage(Component.literal("§6=== Admin Item Info ==="));
        player.sendSystemMessage(Component.literal("§eItem: " + adminItem.getItemStack().getHoverName().getString()));
        player.sendSystemMessage(Component.literal("§eType: " + adminItem.getType()));
        
        if (adminItem.getBuyPrice() != null) {
            player.sendSystemMessage(Component.literal("§eBuy Price: " + adminItem.getCurrency().format(adminItem.getBuyPrice())));
        }
        if (adminItem.getSellPrice() != null) {
            player.sendSystemMessage(Component.literal("§eSell Price: " + adminItem.getCurrency().format(adminItem.getSellPrice())));
        }
        
        player.sendSystemMessage(Component.literal("§eStock: " + (adminItem.getStock() < 0 ? "Infinite" : adminItem.getStock())));
        player.sendSystemMessage(Component.literal("§eDescription: " + adminItem.getDescription()));
        player.sendSystemMessage(Component.literal("§eCreated: " + adminItem.getCreatedAt().toString()));
        
        player.sendSystemMessage(Component.literal("§7=== Controls ==="));
        player.sendSystemMessage(Component.literal("§7Left-click: Edit price"));
        player.sendSystemMessage(Component.literal("§7Right-click: Toggle buy/sell mode"));
        player.sendSystemMessage(Component.literal("§7Shift-click: Remove item"));
        player.sendSystemMessage(Component.literal("§7Double-click: Duplicate item"));
    }
    
    private void handleDebugShop() {
        try {
            ShopManager shopManager = economyManager.getShopManager();
            
<<<<<<< HEAD
            // Run comprehensive integrity check
            shopManager.validateShopIntegrity();
            
            // Run economy diagnosis
            shopManager.diagnoseEconomyIssues();
            
            List<ShopItem> allItems = shopManager.getAllItems();
            List<ShopItem> availableItems = shopManager.getAvailableItems();
            List<ShopItem> adminItems = allItems.stream().filter(ShopItem::isAdminItem).toList();
            List<ShopItem> playerItems = allItems.stream().filter(item -> !item.isAdminItem()).toList();
            
            player.sendSystemMessage(Component.literal("§6=== SHOP DEBUG INFO ==="));
            player.sendSystemMessage(Component.literal("§7Economy enabled: " + (economyManager.isEnabled() ? "§aYES" : "§cNO")));
            player.sendSystemMessage(Component.literal("§7Total items: " + allItems.size()));
            player.sendSystemMessage(Component.literal("§7Available items: " + availableItems.size()));
            player.sendSystemMessage(Component.literal("§7Admin items: " + adminItems.size()));
            player.sendSystemMessage(Component.literal("§7Player items: " + playerItems.size()));
            
            // Show player balance
            try {
                BigDecimal balance = economyManager.getBalance(player.getUUID());
                player.sendSystemMessage(Component.literal("§7Your balance: " + economyManager.getDefaultCurrency().format(balance)));
            } catch (Exception e) {
                player.sendSystemMessage(Component.literal("§cError getting balance: " + e.getMessage()));
            }
=======
            // Run integrity check
            shopManager.validateShopIntegrity();
            
            List<ShopItem> allItems = shopManager.getAllItems();
            List<ShopItem> availableItems = shopManager.getAvailableItems();
            List<ShopItem> adminItems = allItems.stream().filter(ShopItem::isAdminItem).toList();
            
            player.sendSystemMessage(Component.literal("§6=== SHOP DEBUG INFO ==="));
            player.sendSystemMessage(Component.literal("§7Total items: " + allItems.size()));
            player.sendSystemMessage(Component.literal("§7Available items: " + availableItems.size()));
            player.sendSystemMessage(Component.literal("§7Admin items: " + adminItems.size()));
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
            
            if (!adminItems.isEmpty()) {
                player.sendSystemMessage(Component.literal("§eRecent Admin Items:"));
                for (ShopItem item : adminItems.stream().limit(5).toList()) {
                    String stockInfo = item.getStock() < 0 ? "∞" : String.valueOf(item.getStock());
<<<<<<< HEAD
                    String priceInfo = item.canBuy() ? economyManager.getDefaultCurrency().format(item.getBuyPrice()) : "N/A";
                    player.sendSystemMessage(Component.literal("§7- " + item.getItemStack().getHoverName().getString() + 
                        " | Stock: " + stockInfo + " | Type: " + item.getType() + " | Price: " + priceInfo));
=======
                    player.sendSystemMessage(Component.literal("§7- " + item.getItemStack().getHoverName().getString() + 
                        " | Stock: " + stockInfo + " | Type: " + item.getType()));
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
                }
                if (adminItems.size() > 5) {
                    player.sendSystemMessage(Component.literal("§7... and " + (adminItems.size() - 5) + " more"));
                }
            }
            
<<<<<<< HEAD
            if (!playerItems.isEmpty()) {
                player.sendSystemMessage(Component.literal("§eRecent Player Items:"));
                for (ShopItem item : playerItems.stream().limit(3).toList()) {
                    String stockInfo = item.getStock() < 0 ? "∞" : String.valueOf(item.getStock());
                    String priceInfo = item.canBuy() ? economyManager.getDefaultCurrency().format(item.getBuyPrice()) : "N/A";
                    player.sendSystemMessage(Component.literal("§7- " + item.getItemStack().getHoverName().getString() + 
                        " | Stock: " + stockInfo + " | Price: " + priceInfo));
                }
                if (playerItems.size() > 3) {
                    player.sendSystemMessage(Component.literal("§7... and " + (playerItems.size() - 3) + " more"));
                }
            }
            
            player.sendSystemMessage(Component.literal("§aDetailed debug info sent to server console"));
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error debugging shop", e);
            player.sendSystemMessage(Component.literal("§cError debugging shop: " + e.getMessage()));
=======
            player.sendSystemMessage(Component.literal("§aCheck server console for detailed debug info"));
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error debugging shop", e);
            player.sendSystemMessage(Component.literal("§cError debugging shop"));
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
        }
    }
    
    
    @Override
    public boolean canTakeItemForPickAll(@Nonnull ItemStack stack, @Nonnull net.minecraft.world.inventory.Slot slot) {
        // Prevent taking items from admin container
        return slot.container != this.adminContainer;
    }
}
