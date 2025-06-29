package com.zerog.neoessentials.ui.shop;

import com.zerog.neoessentials.economy.Shop;
import com.zerog.neoessentials.economy.ShopManager;
import com.zerog.neoessentials.economy.EconomyManager;
import com.zerog.neoessentials.utils.MessageUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.SimpleContainer;

import java.util.List;
import java.util.UUID;

/**
 * Shop Management GUI system that provides intuitive interfaces for shop owners
 * to manage their shops, inventory, prices, and employees.
 */
public class ShopManagementGUI {

    private final EconomyManager economyManager;
    private final ShopManager shopManager;

    public ShopManagementGUI(EconomyManager economyManager) {
        this.economyManager = economyManager;
        this.shopManager = economyManager.getShopManager();
    }

    /**
     * Opens the main shop management interface for a player.
     * Shows a list of shops they own/manage.
     */
    public void openShopListGUI(ServerPlayer player) {
        try {
            List<Shop> playerShops = shopManager.getPlayerShops(player.getUUID());
            
            if (playerShops.isEmpty()) {
                MessageUtil.sendMessage(player, "§cYou don't own any shops. Use §e/shop create <name> §cto create one.");
                return;
            }

            SimpleContainer container = new SimpleContainer(54); // Double chest size
            setupShopListItems(container, playerShops, player);

            MenuProvider menuProvider = new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.literal("§6§lYour Shops §7(" + playerShops.size() + ")");
                }

                @Override
                public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
                    return new ChestMenu(MenuType.GENERIC_9x6, containerId, playerInventory, container, 6);
                }
            };

            player.openMenu(menuProvider);
            
        } catch (Exception e) {
            MessageUtil.sendErrorMessage(player, "Failed to open shop management interface: " + e.getMessage());
        }
    }

    /**
     * Opens the detailed management interface for a specific shop.
     */
    public void openShopManagementGUI(ServerPlayer player, String shopName) {
        try {
            Shop shop = shopManager.getShopByName(shopName);
            
            if (shop == null) {
                MessageUtil.sendErrorMessage(player, "Shop not found: " + shopName);
                return;
            }

            // Check if player has permission to manage this shop
            if (!shop.getOwnerId().equals(player.getUUID()) && 
                !shop.hasPermission(player.getUUID(), 
                    com.zerog.neoessentials.economy.ShopEmployeeManager.ShopPermission.MANAGE_INVENTORY)) {
                MessageUtil.sendErrorMessage(player, "You don't have permission to manage this shop.");
                return;
            }

            SimpleContainer container = new SimpleContainer(54);
            setupShopManagementItems(container, shop, player);

            MenuProvider menuProvider = new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.literal("§6§lManage: §e" + shop.getName());
                }

                @Override
                public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
                    return new ShopManagementMenu(containerId, playerInventory, container, shop);
                }
            };

            player.openMenu(menuProvider);
            
        } catch (Exception e) {
            MessageUtil.sendErrorMessage(player, "Failed to open shop management: " + e.getMessage());
        }
    }

    /**
     * Opens the shop inventory management interface.
     */
    public void openShopInventoryGUI(ServerPlayer player, String shopName) {
        try {
            Shop shop = shopManager.getShopByName(shopName);
            
            if (shop == null) {
                MessageUtil.sendErrorMessage(player, "Shop not found: " + shopName);
                return;
            }

            // Check permissions
            if (!shop.getOwnerId().equals(player.getUUID()) && 
                !shop.hasPermission(player.getUUID(), 
                    com.zerog.neoessentials.economy.ShopEmployeeManager.ShopPermission.MANAGE_INVENTORY)) {
                MessageUtil.sendErrorMessage(player, "You don't have permission to manage this shop's inventory.");
                return;
            }

            SimpleContainer container = new SimpleContainer(54);
            setupShopInventoryItems(container, shop);

            MenuProvider menuProvider = new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.literal("§e" + shop.getName() + " §7- §6Inventory");
                }

                @Override
                public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
                    return new ShopInventoryMenu(containerId, playerInventory, container, shop);
                }
            };

            player.openMenu(menuProvider);
            
        } catch (Exception e) {
            MessageUtil.sendErrorMessage(player, "Failed to open shop inventory: " + e.getMessage());
        }
    }

    /**
     * Sets up the items for the shop list GUI.
     */
    private void setupShopListItems(SimpleContainer container, List<Shop> shops, ServerPlayer player) {
        int slot = 0;
        
        // Add shop items
        for (Shop shop : shops) {
            if (slot >= 45) break; // Save space for controls
            
            ItemStack shopItem = createShopDisplayItem(shop);
            container.setItem(slot, shopItem);
            slot++;
        }
        
        // Add control items at the bottom
        setupControlItems(container);
    }

    /**
     * Sets up the items for the shop management GUI.
     */
    private void setupShopManagementItems(SimpleContainer container, Shop shop, ServerPlayer player) {
        // Row 1: Shop information and quick stats
        container.setItem(4, createShopInfoItem(shop));
        
        // Row 2: Management options
        container.setItem(9, createInventoryManagementItem());
        container.setItem(10, createPricingManagementItem());
        container.setItem(11, createEmployeeManagementItem());
        container.setItem(12, createPermissionManagementItem());
        container.setItem(13, createShopSettingsItem());
        container.setItem(14, createReportsItem());
        container.setItem(15, createTransactionHistoryItem());
        container.setItem(16, createShopAnalyticsItem());
        
        // Row 3: Quick actions
        container.setItem(18, createToggleShopItem(shop));
        container.setItem(19, createRestockAllItem());
        container.setItem(20, createClearSoldOutItem());
        container.setItem(21, createBulkPricingItem());
        container.setItem(22, createBackupShopItem());
        container.setItem(23, createImportExportItem());
        
        // Bottom row: Navigation
        container.setItem(45, createBackItem());
        container.setItem(49, createHelpItem());
        container.setItem(53, createCloseItem());
    }

    /**
     * Sets up the items for the shop inventory GUI.
     */
    private void setupShopInventoryItems(SimpleContainer container, Shop shop) {
        int slot = 0;
        
        // Display current shop inventory
        for (var entry : shop.getInventory().entrySet()) {
            if (slot >= 45) break;
            
            var shopItem = entry.getValue();
            ItemStack displayItem = shopItem.toItemStack();
            
            // Add custom lore with pricing and stock info
            var meta = displayItem.getOrCreateTag();
            meta.putBoolean("IsShopItem", true);
            meta.putString("ShopItemId", entry.getKey());
            
            container.setItem(slot, displayItem);
            slot++;
        }
        
        // Add management controls at the bottom
        setupInventoryControlItems(container);
    }

    /**
     * Creates a display item for a shop in the shop list.
     */
    private ItemStack createShopDisplayItem(Shop shop) {
        ItemStack item = new ItemStack(Items.CHEST);
        var tag = item.getOrCreateTag();
        
        tag.putString("display", "{\n" +
            "\"Name\": \"{\\\"text\\\":\\\"§6" + shop.getName() + "\\\",\\\"italic\\\":false}\",\n" +
            "\"Lore\": [\n" +
            "\"{\\\"text\\\":\\\"§7Type: §e" + shop.getCategory() + "\\\",\\\"italic\\\":false}\",\n" +
            "\"{\\\"text\\\":\\\"§7Items: §e" + shop.getInventory().size() + "\\\",\\\"italic\\\":false}\",\n" +
            "\"{\\\"text\\\":\\\"§7Status: " + (shop.isOpen() ? "§aOpen" : "§cClosed") + "\\\",\\\"italic\\\":false}\",\n" +
            "\"{\\\"text\\\":\\\"\\\",\\\"italic\\\":false}\",\n" +
            "\"{\\\"text\\\":\\\"§eClick to manage this shop\\\",\\\"italic\\\":false}\"\n" +
            "]\n" +
            "}");
        
        tag.putString("ShopName", shop.getName());
        tag.putBoolean("IsShopItem", true);
        
        return item;
    }

    /**
     * Creates the shop information display item.
     */
    private ItemStack createShopInfoItem(Shop shop) {
        ItemStack item = new ItemStack(Items.NAME_TAG);
        var tag = item.getOrCreateTag();
        
        tag.putString("display", "{\n" +
            "\"Name\": \"{\\\"text\\\":\\\"§6§l" + shop.getName() + "\\\",\\\"italic\\\":false}\",\n" +
            "\"Lore\": [\n" +
            "\"{\\\"text\\\":\\\"§7Category: §e" + shop.getCategory() + "\\\",\\\"italic\\\":false}\",\n" +
            "\"{\\\"text\\\":\\\"§7Description: §f" + shop.getDescription() + "\\\",\\\"italic\\\":false}\",\n" +
            "\"{\\\"text\\\":\\\"§7Status: " + (shop.isOpen() ? "§aOpen" : "§cClosed") + "\\\",\\\"italic\\\":false}\",\n" +
            "\"{\\\"text\\\":\\\"§7Items in stock: §e" + shop.getInventory().size() + "\\\",\\\"italic\\\":false}\",\n" +
            "\"{\\\"text\\\":\\\"\\\",\\\"italic\\\":false}\",\n" +
            "\"{\\\"text\\\":\\\"§7Shop Information\\\",\\\"italic\\\":false}\"\n" +
            "]\n" +
            "}");
        
        return item;
    }

    // Helper methods to create management items
    private ItemStack createInventoryManagementItem() {
        ItemStack item = new ItemStack(Items.BARREL);
        var tag = item.getOrCreateTag();
        tag.putString("display", "{\n" +
            "\"Name\": \"{\\\"text\\\":\\\"§6Inventory Management\\\",\\\"italic\\\":false}\",\n" +
            "\"Lore\": [\n" +
            "\"{\\\"text\\\":\\\"§7Manage shop stock\\\",\\\"italic\\\":false}\",\n" +
            "\"{\\\"text\\\":\\\"§7Add, remove, and organize items\\\",\\\"italic\\\":false}\",\n" +
            "\"{\\\"text\\\":\\\"\\\",\\\"italic\\\":false}\",\n" +
            "\"{\\\"text\\\":\\\"§eClick to open inventory management\\\",\\\"italic\\\":false}\"\n" +
            "]\n" +
            "}");
        tag.putString("Action", "inventory_management");
        return item;
    }

    private ItemStack createPricingManagementItem() {
        ItemStack item = new ItemStack(Items.GOLD_INGOT);
        var tag = item.getOrCreateTag();
        tag.putString("display", "{\n" +
            "\"Name\": \"{\\\"text\\\":\\\"§6Pricing Management\\\",\\\"italic\\\":false}\",\n" +
            "\"Lore\": [\n" +
            "\"{\\\"text\\\":\\\"§7Set buy and sell prices\\\",\\\"italic\\\":false}\",\n" +
            "\"{\\\"text\\\":\\\"§7Bulk pricing updates\\\",\\\"italic\\\":false}\",\n" +
            "\"{\\\"text\\\":\\\"\\\",\\\"italic\\\":false}\",\n" +
            "\"{\\\"text\\\":\\\"§eClick to manage pricing\\\",\\\"italic\\\":false}\"\n" +
            "]\n" +
            "}");
        tag.putString("Action", "pricing_management");
        return item;
    }

    private ItemStack createEmployeeManagementItem() {
        ItemStack item = new ItemStack(Items.PLAYER_HEAD);
        var tag = item.getOrCreateTag();
        tag.putString("display", "{\n" +
            "\"Name\": \"{\\\"text\\\":\\\"§6Employee Management\\\",\\\"italic\\\":false}\",\n" +
            "\"Lore\": [\n" +
            "\"{\\\"text\\\":\\\"§7Hire and manage employees\\\",\\\"italic\\\":false}\",\n" +
            "\"{\\\"text\\\":\\\"§7Set roles and permissions\\\",\\\"italic\\\":false}\",\n" +
            "\"{\\\"text\\\":\\\"\\\",\\\"italic\\\":false}\",\n" +
            "\"{\\\"text\\\":\\\"§eClick to manage employees\\\",\\\"italic\\\":false}\"\n" +
            "]\n" +
            "}");
        tag.putString("Action", "employee_management");
        return item;
    }

    private ItemStack createToggleShopItem(Shop shop) {
        ItemStack item = new ItemStack(shop.isOpen() ? Items.GREEN_WOOL : Items.RED_WOOL);
        var tag = item.getOrCreateTag();
        String status = shop.isOpen() ? "Open" : "Closed";
        String action = shop.isOpen() ? "Close" : "Open";
        
        tag.putString("display", "{\n" +
            "\"Name\": \"{\\\"text\\\":\\\"§6" + action + " Shop\\\",\\\"italic\\\":false}\",\n" +
            "\"Lore\": [\n" +
            "\"{\\\"text\\\":\\\"§7Current status: " + (shop.isOpen() ? "§aOpen" : "§cClosed") + "\\\",\\\"italic\\\":false}\",\n" +
            "\"{\\\"text\\\":\\\"\\\",\\\"italic\\\":false}\",\n" +
            "\"{\\\"text\\\":\\\"§eClick to " + action.toLowerCase() + " this shop\\\",\\\"italic\\\":false}\"\n" +
            "]\n" +
            "}");
        tag.putString("Action", "toggle_shop");
        return item;
    }

    // More helper methods for creating UI items
    private ItemStack createBackItem() {
        ItemStack item = new ItemStack(Items.ARROW);
        var tag = item.getOrCreateTag();
        tag.putString("display", "{\n" +
            "\"Name\": \"{\\\"text\\\":\\\"§cBack\\\",\\\"italic\\\":false}\",\n" +
            "\"Lore\": [\n" +
            "\"{\\\"text\\\":\\\"§7Return to previous menu\\\",\\\"italic\\\":false}\"\n" +
            "]\n" +
            "}");
        tag.putString("Action", "back");
        return item;
    }

    private ItemStack createCloseItem() {
        ItemStack item = new ItemStack(Items.BARRIER);
        var tag = item.getOrCreateTag();
        tag.putString("display", "{\n" +
            "\"Name\": \"{\\\"text\\\":\\\"§cClose\\\",\\\"italic\\\":false}\",\n" +
            "\"Lore\": [\n" +
            "\"{\\\"text\\\":\\\"§7Close this menu\\\",\\\"italic\\\":false}\"\n" +
            "]\n" +
            "}");
        tag.putString("Action", "close");
        return item;
    }

    private void setupControlItems(SimpleContainer container) {
        // Add navigation and utility items at the bottom
        container.setItem(45, createBackItem());
        container.setItem(49, createNewShopItem());
        container.setItem(53, createCloseItem());
    }

    private void setupInventoryControlItems(SimpleContainer container) {
        // Add inventory management controls
        container.setItem(45, createBackItem());
        container.setItem(47, createAddItemButton());
        container.setItem(49, createRestockAllItem());
        container.setItem(51, createClearSoldOutItem());
        container.setItem(53, createCloseItem());
    }

    // Additional helper methods for UI items
    private ItemStack createNewShopItem() {
        ItemStack item = new ItemStack(Items.EMERALD);
        var tag = item.getOrCreateTag();
        tag.putString("display", "{\n" +
            "\"Name\": \"{\\\"text\\\":\\\"§aCreate New Shop\\\",\\\"italic\\\":false}\",\n" +
            "\"Lore\": [\n" +
            "\"{\\\"text\\\":\\\"§7Start a new business\\\",\\\"italic\\\":false}\",\n" +
            "\"{\\\"text\\\":\\\"\\\",\\\"italic\\\":false}\",\n" +
            "\"{\\\"text\\\":\\\"§eClick to create a new shop\\\",\\\"italic\\\":false}\"\n" +
            "]\n" +
            "}");
        tag.putString("Action", "create_shop");
        return item;
    }

    private ItemStack createAddItemButton() {
        ItemStack item = new ItemStack(Items.PLUS_SIGN);
        var tag = item.getOrCreateTag();
        tag.putString("display", "{\n" +
            "\"Name\": \"{\\\"text\\\":\\\"§aAdd Item\\\",\\\"italic\\\":false}\",\n" +
            "\"Lore\": [\n" +
            "\"{\\\"text\\\":\\\"§7Add items from your inventory\\\",\\\"italic\\\":false}\",\n" +
            "\"{\\\"text\\\":\\\"\\\",\\\"italic\\\":false}\",\n" +
            "\"{\\\"text\\\":\\\"§eClick to add items to shop\\\",\\\"italic\\\":false}\"\n" +
            "]\n" +
            "}");
        tag.putString("Action", "add_item");
        return item;
    }

    // More UI helper methods...
    private ItemStack createRestockAllItem() {
        ItemStack item = new ItemStack(Items.CHEST_MINECART);
        var tag = item.getOrCreateTag();
        tag.putString("display", "{\n" +
            "\"Name\": \"{\\\"text\\\":\\\"§6Restock All\\\",\\\"italic\\\":false}\",\n" +
            "\"Lore\": [\n" +
            "\"{\\\"text\\\":\\\"§7Automatically restock all items\\\",\\\"italic\\\":false}\",\n" +
            "\"{\\\"text\\\":\\\"§7from your inventory\\\",\\\"italic\\\":false}\",\n" +
            "\"{\\\"text\\\":\\\"\\\",\\\"italic\\\":false}\",\n" +
            "\"{\\\"text\\\":\\\"§eClick to restock shop\\\",\\\"italic\\\":false}\"\n" +
            "]\n" +
            "}");
        tag.putString("Action", "restock_all");
        return item;
    }

    private ItemStack createClearSoldOutItem() {
        ItemStack item = new ItemStack(Items.TNT);
        var tag = item.getOrCreateTag();
        tag.putString("display", "{\n" +
            "\"Name\": \"{\\\"text\\\":\\\"§cClear Sold Out\\\",\\\"italic\\\":false}\",\n" +
            "\"Lore\": [\n" +
            "\"{\\\"text\\\":\\\"§7Remove all sold out items\\\",\\\"italic\\\":false}\",\n" +
            "\"{\\\"text\\\":\\\"§7from the shop\\\",\\\"italic\\\":false}\",\n" +
            "\"{\\\"text\\\":\\\"\\\",\\\"italic\\\":false}\",\n" +
            "\"{\\\"text\\\":\\\"§eClick to clear sold out items\\\",\\\"italic\\\":false}\"\n" +
            "]\n" +
            "}");
        tag.putString("Action", "clear_sold_out");
        return item;
    }

    // Additional placeholder methods for complete functionality
    private ItemStack createPermissionManagementItem() {
        return new ItemStack(Items.PAPER); // Placeholder
    }

    private ItemStack createShopSettingsItem() {
        return new ItemStack(Items.REDSTONE); // Placeholder
    }

    private ItemStack createReportsItem() {
        return new ItemStack(Items.BOOK); // Placeholder
    }

    private ItemStack createTransactionHistoryItem() {
        return new ItemStack(Items.WRITABLE_BOOK); // Placeholder
    }

    private ItemStack createShopAnalyticsItem() {
        return new ItemStack(Items.COMPASS); // Placeholder
    }

    private ItemStack createBulkPricingItem() {
        return new ItemStack(Items.EMERALD_BLOCK); // Placeholder
    }

    private ItemStack createBackupShopItem() {
        return new ItemStack(Items.ENDER_CHEST); // Placeholder
    }

    private ItemStack createImportExportItem() {
        return new ItemStack(Items.SHULKER_BOX); // Placeholder
    }

    private ItemStack createHelpItem() {
        return new ItemStack(Items.ENCHANTED_BOOK); // Placeholder
    }
}
