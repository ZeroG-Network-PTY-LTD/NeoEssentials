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
import java.util.*;

/**
 * Admin Shop Management Menu
 * Comprehensive administration interface for economy shop management
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class AdminShopManagementMenu {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminShopManagementMenu.class);
    private static AdminShopManagementMenu instance;
    
    // Price management data
    private static final Map<String, Double> globalPriceMultipliers = new HashMap<>();
    private static final Map<String, Boolean> categoryEnabled = new HashMap<>();
    
    static {
        initializePriceData();
    }
    
    private AdminShopManagementMenu() {
    }
    
    public static AdminShopManagementMenu getInstance() {
        if (instance == null) {
            instance = new AdminShopManagementMenu();
        }
        return instance;
    }
    
    /**
     * Initialize default price multipliers and category states
     */
    private static void initializePriceData() {
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
    
    /**
     * Open the main admin shop management interface
     */
    public void openAdminShopMenu(ServerPlayer player) {
        if (!player.hasPermissions(3)) {
            MessageUtil.sendMessage(player, "&cYou need admin permissions to access shop management!");
            return;
        }
        
        List<GuiItem> items = new ArrayList<>();
        
        // Economy overview (row 1)
        items.add(createInfoItem(Items.EMERALD_BLOCK, "§2§l💎 Economy Overview",
            "§7Server economy statistics:",
            "§7• Total transactions today: §e" + getTodayTransactions(),
            "§7• Daily revenue: §2$" + getDailyRevenue(),
            "§7• Active players: §a" + getActivePlayers(),
            "§7• Shop status: §a" + getShopStatus(),
            "§7• Economy health: §a" + getEconomyHealth()));
        
        items.add(createInfoItem(Items.GOLD_BLOCK, "§6📊 Market Analytics",
            "§7Current market statistics:",
            "§7• Most popular item: §e" + getMostPopularItem(),
            "§7• Average transaction: §2$" + getAverageTransaction(),
            "§7• Inflation rate: §e" + getInflationRate() + "%",
            "§7• Economy status: §a" + getEconomyStatus()));
        
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
        
        // Management Tools (row 3)
        items.add(createActionItem(Items.PLAYER_HEAD, "§b👥 Player Management", 
            "§7Manage player shop access",
            "§7• View player purchase history",
            "§7• Set individual discounts",
            "§7• Manage shop permissions",
            "§7• Reset player data",
            "§eClick to manage players"));
        
        items.add(createActionItem(Items.CLOCK, "§e⏰ Shop Hours", 
            "§7Configure shop operating hours",
            "§7Current status: §a" + (isShopOpen() ? "Open 24/7" : "Scheduled"),
            "§7Open time: §e" + getShopOpenTime(),
            "§7Close time: §e" + getShopCloseTime(),
            "§eClick to configure schedule"));
        
        items.add(createActionItem(Items.CHEST, "§6📦 Inventory Manager", 
            "§7Manage shop stock levels",
            "§7• Current stock: §a" + getTotalStockLevel() + " items",
            "§7• Low stock alerts: §e" + getLowStockCount() + " items",
            "§7• Auto-restock: " + (isAutoRestockEnabled() ? "§aEnabled" : "§cDisabled"),
            "§7• Stock value: §2$" + String.format("%.2f", getTotalStockValue()),
            "§eClick to manage inventory"));
        
        // Analytics and Logs (row 4)
        items.add(createActionItem(Items.MAP, "§9📋 Transaction Logs",
            "§7View detailed transaction history",
            "§7• Recent purchases: §e" + getRecentTransactions(),
            "§7• Daily revenue: §2$" + getDailyRevenue(),
            "§7• Top customers",
            "§7• Failed transactions",
            "§eClick to view logs"));
        
        items.add(createActionItem(Items.PAPER, "§a📈 Analytics",
            "§7Advanced shop analytics",
            "§7• Sales trends and patterns",
            "§7• Popular items analysis",
            "§7• Revenue forecasting",
            "§7• Customer behavior insights",
            "§eClick for analytics"));
        
        items.add(createActionItem(Items.COMMAND_BLOCK, "§c⚡ Quick Actions",
            "§7Common administrative actions",
            "§7• Emergency shop closure",
            "§7• Bulk price reset",
            "§7• Clear transaction history",
            "§7• Restart shop services",
            "§eClick for quick actions"));
        
        // Controls (row 5)
        items.add(createActionItem(Items.BARRIER, "§c❌ Close Admin Panel",
            "§7Close the admin panel",
            "§eClick to close"));
        
        items.add(createActionItem(Items.NETHER_STAR, "§5⭐ Super Admin",
            "§7Advanced server controls",
            "§7• Global economy settings",
            "§7• Server-wide price controls",
            "§7• Economic policy management",
            "§eClick for super admin tools"));
        
        MenuProvider gui = createAdminChestGui("§c§l⚙ Shop Management Panel ⚙", 6, items);
        player.openMenu(gui);
        
        LOGGER.info("Admin {} opened shop management interface", player.getName().getString());
    }
    
    // Placeholder methods for analytics data
    private String getTodayTransactions() { return "247"; }
    private String getDailyRevenue() { return "1,234.56"; }
    private String getActivePlayers() { return "18"; }
    private String getShopStatus() { return "Online"; }
    private String getEconomyHealth() { return "Excellent"; }
    private String getMostPopularItem() { return "Diamond Sword"; }
    private String getAverageTransaction() { return "45.30"; }
    private String getInflationRate() { return "2.1"; }
    private String getEconomyStatus() { return "Stable"; }
    private int getCustomItemCount() { return 12; }
    private String getRecentAdditions() { return "3 today"; }
    private boolean isShopOpen() { return true; }
    private String getShopOpenTime() { return "00:00"; }
    private String getShopCloseTime() { return "23:59"; }
    private int getTotalStockLevel() { return 1456; }
    private int getLowStockCount() { return 8; }
    private boolean isAutoRestockEnabled() { return true; }
    private double getTotalStockValue() { return 12456.78; }
    private String getRecentTransactions() { return "67"; }
    
    // Helper methods
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
        
        return new GuiItem(item, null);
    }
    
    /**
     * Create admin chest GUI with click handling
     */
    private MenuProvider createAdminChestGui(String title, int rows, List<GuiItem> items) {
        return new SimpleMenuProvider(
            (windowId, playerInventory, player) -> {
                SimpleContainer container = new SimpleContainer(rows * 9);
                AbstractContainerMenu menu = new ChestMenu(MenuType.GENERIC_9x6, windowId, playerInventory, container, rows);
                
                // Add items to the container
                for (int i = 0; i < items.size() && i < rows * 9; i++) {
                    GuiItem item = items.get(i);
                    if (item != null) {
                        container.setItem(i, item.getItemStack());
                    }
                }
                
                // Register GUI session
                if (player instanceof ServerPlayer serverPlayer) {
                    GuiClickHandler.registerSession(serverPlayer, CustomGuiManager.GuiType.ECONOMY_MANAGEMENT, new HashMap<>());
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
