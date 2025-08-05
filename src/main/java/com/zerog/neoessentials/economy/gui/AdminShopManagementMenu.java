package com.zerog.neoessentials.economy.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemLore;
import com.zerog.neoessentials.managers.EconomyManager;
import com.zerog.neoessentials.gui.GuiClickHandler;
import com.zerog.neoessentials.gui.CustomGuiManager;
import com.zerog.neoessentials.util.MessageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.util.*;

/**
 * Admin Shop Management Menu
 * Provides comprehensive shop administration tools for server operators
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class AdminShopManagementMenu {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminShopManagementMenu.class);
    private static AdminShopManagementMenu instance;
    
    private final EconomyManager economyManager;
    
    // Shop management data
    private static final Map<String, Double> globalPriceMultipliers = new HashMap<>();
    private static final Map<String, Boolean> categoryEnabled = new HashMap<>();
    private static final List<String> recentTransactions = new ArrayList<>();
    
    static {
        // Initialize default settings
        globalPriceMultipliers.put("weapons", 1.0);
        globalPriceMultipliers.put("armor", 1.0);
        globalPriceMultipliers.put("food", 1.0);
        globalPriceMultipliers.put("blocks", 1.0);
        globalPriceMultipliers.put("redstone", 1.0);
        globalPriceMultipliers.put("rare", 1.0);
        
        categoryEnabled.put("weapons", true);
        categoryEnabled.put("armor", true);
        categoryEnabled.put("food", true);
        categoryEnabled.put("blocks", true);
        categoryEnabled.put("redstone", true);
        categoryEnabled.put("rare", true);
    }
    
    private AdminShopManagementMenu() {
        this.economyManager = EconomyManager.getInstance();
    }
    
    public static AdminShopManagementMenu getInstance() {
        if (instance == null) {
            instance = new AdminShopManagementMenu();
        }
        return instance;
    }
    
    /**
     * Open the main admin shop management menu
     */
    public void openMainMenu(ServerPlayer player) {
        if (!player.hasPermissions(3)) {
            MessageUtil.sendMessage(player, "&cYou don't have permission to access shop management!");
            return;
        }
        
        List<GuiItem> items = new ArrayList<>();
        
        // Shop Statistics (top row)
        items.add(createInfoItem(Items.EMERALD, "§a💰 Shop Statistics", 
            "§7Total transactions today: §e" + getTotalTransactions(),
            "§7Total revenue today: §2" + getTotalRevenue(),
            "§7Active shoppers: §b" + getActiveShoppers(),
            "§7Most purchased: §6" + getMostPurchasedItem()));
        
        items.add(createInfoItem(Items.BOOK, "§9📊 Sales Report", 
            "§7Weekly sales summary:",
            "§7• Weapons: §e" + getCategorySales("weapons") + " sales",
            "§7• Armor: §e" + getCategorySales("armor") + " sales",
            "§7• Food: §e" + getCategorySales("food") + " sales",
            "§7• Blocks: §e" + getCategorySales("blocks") + " sales"));
        
        items.add(createInfoItem(Items.GOLD_INGOT, "§6💸 Economy Health", 
            "§7Server money supply: §2" + getServerMoneySupply(),
            "§7Average player balance: §2" + getAveragePlayerBalance(),
            "§7Inflation rate: §e" + getInflationRate() + "%",
            "§7Economy status: §a" + getEconomyStatus()));
        
        // Price Management (row 2)
        items.add(createActionItem(Items.COMPARATOR, "§c⚙ Price Controls", 
            "§7Manage global price multipliers",
            "§7Current multipliers:",
            "§8• Weapons: §e" + String.format("%.2f", globalPriceMultipliers.get("weapons")) + "x",
            "§8• Armor: §e" + String.format("%.2f", globalPriceMultipliers.get("armor")) + "x",
            "§8• Food: §e" + String.format("%.2f", globalPriceMultipliers.get("food")) + "x",
            "§eClick to modify prices"));
        
        items.add(createActionItem(Items.REDSTONE, "§c🔧 Category Toggle", 
            "§7Enable/disable shop categories",
            "§7Current status:",
            "§8• Weapons: " + (categoryEnabled.get("weapons") ? "§aEnabled" : "§cDisabled"),
            "§8• Armor: " + (categoryEnabled.get("armor") ? "§aEnabled" : "§cDisabled"),
            "§8• Food: " + (categoryEnabled.get("food") ? "§aEnabled" : "§cDisabled"),
            "§8• Blocks: " + (categoryEnabled.get("blocks") ? "§aEnabled" : "§cDisabled"),
            "§eClick to toggle categories"));
        
        items.add(createActionItem(Items.WRITABLE_BOOK, "§d📝 Custom Items", 
            "§7Add custom items to shop",
            "§7Current custom items: §e" + getCustomItemCount(),
            "§7Recent additions: §e" + getRecentAdditions(),
            "",
            "§eClick to manage custom items"));
        
        // Player Management (row 3)
        items.add(createActionItem(Items.PLAYER_HEAD, "§b👥 Player Management", 
            "§7Manage player shop access",
            "§7• View player purchase history",
            "§7• Set individual discounts",
            "§7• Manage shop permissions",
            "§7• Reset player data",
            "§eClick to manage players",
            p -> openPlayerManagementMenu(p)));
        
        items.add(createActionItem(Items.CLOCK, "§e⏰ Shop Hours", 
            "§7Configure shop operating hours",
            "§7Current status: §a" + (isShopOpen() ? "Open 24/7" : "Scheduled"),
            "§7Open time: §e" + getShopOpenTime(),
            "§7Close time: §e" + getShopCloseTime(),
            "§eClick to configure schedule",
            p -> openShopScheduleMenu(p)));
        
        items.add(createActionItem(Items.CHEST, "§6📦 Inventory Manager", 
            "§7Manage shop stock levels",
            "§7• Set item stock limits",
            "§7• Configure auto-restock",
            "§7• View low stock alerts",
            "§7Current alerts: §c" + getLowStockAlerts(),
            "§eClick to manage inventory",
            p -> openInventoryManagementMenu(p)));
        
        // Logs and Reports (row 4)
        items.add(createActionItem(Items.MAP, "§9📋 Transaction Logs", 
            "§7View detailed transaction history",
            "§7• All player purchases",
            "§7• Admin modifications",
            "§7• Price changes",
            "§7Recent entries: §e" + recentTransactions.size(),
            "§eClick to view logs",
            p -> openTransactionLogsMenu(p)));
        
        items.add(createActionItem(Items.PAPER, "§a📈 Analytics", 
            "§7Advanced shop analytics",
            "§7• Sales trends",
            "§7• Popular items report",
            "§7• Revenue forecasting",
            "§7• Player behavior analysis",
            "§eClick to view analytics",
            p -> openAnalyticsMenu(p)));
        
        items.add(createActionItem(Items.COMMAND_BLOCK, "§c⚡ Quick Actions", 
            "§7Emergency shop controls",
            "§7• Emergency shop closure",
            "§7• Mass price reset",
            "§7• Clear all transactions",
            "§7• Backup shop data",
            "§cUse with caution!",
            p -> openQuickActionsMenu(p)));
        
        // Navigation (bottom row)
        items.add(createActionItem(Items.BARRIER, "§c❌ Close Admin Panel", 
            "§7Exit shop management", 
            p -> p.closeContainer()));
        
        items.add(createActionItem(Items.NETHER_STAR, "§5⭐ Super Admin", 
            "§7Advanced administrative tools",
            "§7• Database management",
            "§7• Configuration editor",
            "§7• System diagnostics",
            "§cRequires super admin access",
            p -> openSuperAdminMenu(p)));
        
        MenuProvider gui = createAdminChestGui("§c§l🛠 Shop Administration Panel §c§l🛠", 6, items);
        player.openMenu(gui);
        
        LOGGER.info("Admin {} opened shop management panel", player.getName().getString());
    }
    
    /**
     * Open price control menu
     */
    private void openPriceControlMenu(ServerPlayer player) {
        List<GuiItem> items = new ArrayList<>();
        
        // Price multiplier controls for each category
        for (String category : Arrays.asList("weapons", "armor", "food", "blocks", "redstone", "rare")) {
            net.minecraft.world.item.Item icon = getCategoryIcon(category);
            double currentMultiplier = globalPriceMultipliers.get(category);
            
            items.add(createActionItem(icon, "§6" + category.toUpperCase() + " Prices", 
                "§7Current multiplier: §e" + String.format("%.2f", currentMultiplier) + "x",
                "§7Example: $10 → $" + String.format("%.2f", 10.0 * currentMultiplier),
                "",
                "§aLeft-click: +0.1x",
                "§cRight-click: -0.1x",
                "§eShift-click: Reset to 1.0x",
                p -> adjustPriceMultiplier(p, category, 0.1)));
        }
        
        // Global actions
        items.add(createActionItem(Items.GOLD_BLOCK, "§6📊 Global Price Adjustment", 
            "§7Apply changes to all categories",
            "§7• Increase all by 10%",
            "§7• Decrease all by 10%",
            "§7• Apply inflation adjustment",
            "§eClick for global controls",
            p -> openGlobalPriceMenu(p)));
        
        items.add(createActionItem(Items.ANVIL, "§c⚒ Price Calculator", 
            "§7Calculate optimal pricing",
            "§7• Supply/demand analysis",
            "§7• Competitor pricing",
            "§7• Profit margin calculator",
            "§eClick to open calculator",
            p -> openPriceCalculatorMenu(p)));
        
        items.add(createActionItem(Items.ARROW, "§a⬅ Back to Admin Panel", 
            "§7Return to main admin menu", 
            p -> openMainMenu(p)));
        
        MenuProvider gui = createAdminChestGui("§c§l💰 Price Control Center", 6, items);
        player.openMenu(gui);
    }
    
    /**
     * Open category toggle menu
     */
    private void openCategoryToggleMenu(ServerPlayer player) {
        List<GuiItem> items = new ArrayList<>();
        
        for (String category : Arrays.asList("weapons", "armor", "food", "blocks", "redstone", "rare")) {
            boolean enabled = categoryEnabled.get(category);
            net.minecraft.world.item.Item icon = enabled ? getCategoryIcon(category) : Items.BARRIER;
            
            items.add(createActionItem(icon, 
                (enabled ? "§a✓ " : "§c✗ ") + category.toUpperCase() + " Category", 
                "§7Status: " + (enabled ? "§aEnabled" : "§cDisabled"),
                "§7Items in category: §e" + getCategoryItemCount(category),
                "§7Recent sales: §e" + getCategorySales(category),
                "",
                "§eClick to " + (enabled ? "disable" : "enable"),
                p -> toggleCategory(p, category)));
        }
        
        items.add(createActionItem(Items.REDSTONE_BLOCK, "§c🔴 Emergency Disable All", 
            "§7Disable all shop categories",
            "§7Use in emergency situations",
            "§cThis will close the entire shop!",
            p -> emergencyDisableAll(p)));
        
        items.add(createActionItem(Items.EMERALD_BLOCK, "§a🟢 Enable All Categories", 
            "§7Enable all shop categories",
            "§7Restore normal shop operations",
            p -> enableAllCategories(p)));
        
        items.add(createActionItem(Items.ARROW, "§a⬅ Back to Admin Panel", 
            "§7Return to main admin menu", 
            p -> openMainMenu(p)));
        
        MenuProvider gui = createAdminChestGui("§c§l🔧 Category Management", 6, items);
        player.openMenu(gui);
    }
    
    // Helper methods for shop management
    private int getTotalTransactions() {
        return 1247; // Placeholder - would track real data
    }
    
    private String getTotalRevenue() {
        return economyManager.formatCurrency(BigDecimal.valueOf(45230.75));
    }
    
    private int getActiveShoppers() {
        return 23; // Placeholder - would track active users
    }
    
    private String getMostPurchasedItem() {
        return "Diamond Sword"; // Placeholder - would track real data
    }
    
    private int getCategorySales(String category) {
        return switch (category) {
            case "weapons" -> 342;
            case "armor" -> 298;
            case "food" -> 567;
            case "blocks" -> 834;
            case "redstone" -> 123;
            case "rare" -> 45;
            default -> 0;
        };
    }
    
    private String getServerMoneySupply() {
        return economyManager.formatCurrency(BigDecimal.valueOf(2340567.89));
    }
    
    private String getAveragePlayerBalance() {
        return economyManager.formatCurrency(BigDecimal.valueOf(1250.45));
    }
    
    private String getInflationRate() {
        return "2.3"; // Placeholder
    }
    
    private String getEconomyStatus() {
        return "Healthy"; // Placeholder - would calculate based on metrics
    }
    
    private int getCustomItemCount() {
        return 15; // Placeholder
    }
    
    private String getRecentAdditions() {
        return "Magic Sword, Healing Potion"; // Placeholder
    }
    
    private boolean isShopOpen() {
        return true; // Placeholder - would check schedule
    }
    
    private String getShopOpenTime() {
        return "Always Open"; // Placeholder
    }
    
    private String getShopCloseTime() {
        return "Never"; // Placeholder
    }
    
    private int getLowStockAlerts() {
        return 3; // Placeholder
    }
    
    private net.minecraft.world.item.Item getCategoryIcon(String category) {
        return switch (category) {
            case "weapons" -> Items.DIAMOND_SWORD;
            case "armor" -> Items.DIAMOND_CHESTPLATE;
            case "food" -> Items.COOKED_BEEF;
            case "blocks" -> Items.STONE_BRICKS;
            case "redstone" -> Items.REDSTONE;
            case "rare" -> Items.NETHER_STAR;
            default -> Items.CHEST;
        };
    }
    
    private int getCategoryItemCount(String category) {
        return switch (category) {
            case "weapons" -> 15;
            case "armor" -> 12;
            case "food" -> 8;
            case "blocks" -> 20;
            case "redstone" -> 10;
            case "rare" -> 5;
            default -> 0;
        };
    }
    
    private void adjustPriceMultiplier(ServerPlayer player, String category, double adjustment) {
        double current = globalPriceMultipliers.get(category);
        double newValue = Math.max(0.1, Math.min(10.0, current + adjustment));
        globalPriceMultipliers.put(category, newValue);
        
        MessageUtil.sendMessage(player, "&aAdjusted " + category + " price multiplier to " + 
            String.format("%.2f", newValue) + "x");
        
        LOGGER.info("Admin {} adjusted {} price multiplier to {}x", 
            player.getName().getString(), category, newValue);
        
        // Refresh the menu
        openPriceControlMenu(player);
    }
    
    private void toggleCategory(ServerPlayer player, String category) {
        boolean current = categoryEnabled.get(category);
        categoryEnabled.put(category, !current);
        
        MessageUtil.sendMessage(player, "&a" + category + " category " + 
            (!current ? "enabled" : "disabled"));
        
        LOGGER.info("Admin {} {} {} category", 
            player.getName().getString(), (!current ? "enabled" : "disabled"), category);
        
        // Refresh the menu
        openCategoryToggleMenu(player);
    }
    
    private void emergencyDisableAll(ServerPlayer player) {
        for (String category : categoryEnabled.keySet()) {
            categoryEnabled.put(category, false);
        }
        
        MessageUtil.sendMessage(player, "&cEmergency shutdown: All shop categories disabled!");
        LOGGER.warn("EMERGENCY: Admin {} disabled all shop categories", player.getName().getString());
        
        openCategoryToggleMenu(player);
    }
    
    private void enableAllCategories(ServerPlayer player) {
        for (String category : categoryEnabled.keySet()) {
            categoryEnabled.put(category, true);
        }
        
        MessageUtil.sendMessage(player, "&aAll shop categories enabled!");
        LOGGER.info("Admin {} enabled all shop categories", player.getName().getString());
        
        openCategoryToggleMenu(player);
    }
    
    // Placeholder methods for unimplemented menus
    private void openCustomItemMenu(ServerPlayer player) {
        MessageUtil.sendMessage(player, "&eCustom item management coming soon!");
    }
    
    private void openPlayerManagementMenu(ServerPlayer player) {
        MessageUtil.sendMessage(player, "&ePlayer management coming soon!");
    }
    
    private void openShopScheduleMenu(ServerPlayer player) {
        MessageUtil.sendMessage(player, "&eShop schedule management coming soon!");
    }
    
    private void openInventoryManagementMenu(ServerPlayer player) {
        MessageUtil.sendMessage(player, "&eInventory management coming soon!");
    }
    
    private void openTransactionLogsMenu(ServerPlayer player) {
        MessageUtil.sendMessage(player, "&eTransaction logs coming soon!");
    }
    
    private void openAnalyticsMenu(ServerPlayer player) {
        MessageUtil.sendMessage(player, "&eAnalytics dashboard coming soon!");
    }
    
    private void openQuickActionsMenu(ServerPlayer player) {
        MessageUtil.sendMessage(player, "&eQuick actions menu coming soon!");
    }
    
    private void openSuperAdminMenu(ServerPlayer player) {
        if (!player.hasPermissions(4)) {
            MessageUtil.sendMessage(player, "&cSuper admin access required!");
            return;
        }
        MessageUtil.sendMessage(player, "&eSuper admin panel coming soon!");
    }
    
    private void openGlobalPriceMenu(ServerPlayer player) {
        MessageUtil.sendMessage(player, "&eGlobal price controls coming soon!");
    }
    
    private void openPriceCalculatorMenu(ServerPlayer player) {
        MessageUtil.sendMessage(player, "&ePrice calculator coming soon!");
    }
    
    // Helper methods for creating GUI items
    private GuiItem createInfoItem(net.minecraft.world.item.Item icon, String name, String... lore) {
        ItemStack item = new ItemStack(icon);
        item.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        
        List<Component> loreComponents = new ArrayList<>();
        for (String line : lore) {
            loreComponents.add(Component.literal(line));
        }
        
        item.set(DataComponents.LORE, new ItemLore(loreComponents));
        
        return new GuiItem(item, null);
    }
    
    private GuiItem createActionItem(net.minecraft.world.item.Item icon, String name, String... lore) {
        ItemStack item = new ItemStack(icon);
        item.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        
        List<Component> loreComponents = new ArrayList<>();
        for (String line : lore) {
            loreComponents.add(Component.literal(line));
        }
        
        item.set(DataComponents.LORE, new ItemLore(loreComponents));
        
        return new GuiItem(item, null); // Actions would be implemented separately
    }
    
    private GuiItem createMultiLineActionItem(net.minecraft.world.item.Item icon, String name, 
                                            CustomGuiManager.GuiClickAction action, String... lore) {
        ItemStack item = new ItemStack(icon);
        item.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        
        List<Component> loreComponents = new ArrayList<>();
        for (String line : lore) {
            loreComponents.add(Component.literal(line));
        }
        
        item.set(DataComponents.LORE, new ItemLore(loreComponents));
        
        return new GuiItem(item, action);
    }
    
    /**
     * Create admin chest GUI with click handling
     */
    private MenuProvider createAdminChestGui(String title, int rows, List<GuiItem> items) {
        return new SimpleMenuProvider(
            (windowId, playerInventory, player) -> {
                SimpleContainer container = new SimpleContainer(rows * 9);
                AbstractContainerMenu menu = new ChestMenu(MenuType.GENERIC_9x6, windowId, playerInventory, container, rows);
                
                // Prepare click actions for registration
                Map<Integer, CustomGuiManager.GuiClickAction> clickActions = new HashMap<>();
                
                // Add items to the container and register click actions
                for (int i = 0; i < items.size() && i < rows * 9; i++) {
                    GuiItem item = items.get(i);
                    if (item != null) {
                        container.setItem(i, item.getItemStack());
                        if (item.getClickAction() != null) {
                            clickActions.put(i, item.getClickAction());
                        }
                    }
                }
                
                // Register GUI session for click handling
                if (player instanceof ServerPlayer serverPlayer) {
                    GuiClickHandler.registerSession(serverPlayer, CustomGuiManager.GuiType.ECONOMY_MANAGEMENT, clickActions);
                }
                
                return menu;
            },
            Component.literal(title)
        );
    }
    
    /**
     * Simple container implementation
     */
    private static class SimpleContainer implements net.minecraft.world.Container {
        private final ItemStack[] items;
        
        public SimpleContainer(int size) {
            this.items = new ItemStack[size];
            for (int i = 0; i < size; i++) {
                items[i] = ItemStack.EMPTY;
            }
        }
        
        @Override
        public int getContainerSize() {
            return items.length;
        }
        
        @Override
        public boolean isEmpty() {
            for (ItemStack item : items) {
                if (!item.isEmpty()) {
                    return false;
                }
            }
            return true;
        }
        
        @Override
        public ItemStack getItem(int index) {
            return index >= 0 && index < items.length ? items[index] : ItemStack.EMPTY;
        }
        
        @Override
        public ItemStack removeItem(int index, int count) {
            return ItemStack.EMPTY;
        }
        
        @Override
        public ItemStack removeItemNoUpdate(int index) {
            return ItemStack.EMPTY;
        }
        
        @Override
        public void setItem(int index, @Nonnull ItemStack stack) {
            if (index >= 0 && index < items.length) {
                items[index] = stack;
            }
        }
        
        @Override
        public void setChanged() {
            // No-op for GUI
        }
        
        @Override
        public boolean stillValid(@Nonnull Player player) {
            return true;
        }
        
        @Override
        public void clearContent() {
            for (int i = 0; i < items.length; i++) {
                items[i] = ItemStack.EMPTY;
            }
        }
    }
    
    /**
     * GUI item wrapper
     */
    public static class GuiItem {
        private final ItemStack itemStack;
        private final CustomGuiManager.GuiClickAction clickAction;
        
        public GuiItem(ItemStack itemStack, CustomGuiManager.GuiClickAction clickAction) {
            this.itemStack = itemStack;
            this.clickAction = clickAction;
        }
        
        public ItemStack getItemStack() {
            return itemStack;
        }
        
        public CustomGuiManager.GuiClickAction getClickAction() {
            return clickAction;
        }
    }
}
