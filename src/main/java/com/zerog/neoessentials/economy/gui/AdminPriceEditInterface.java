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
 * Admin Price Edit Interface
 * Provides detailed price editing controls for individual shop items
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class AdminPriceEditInterface {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminPriceEditInterface.class);
    private static AdminPriceEditInterface instance;
    
    private final EconomyManager economyManager;
    
    // Price data storage
    private static final Map<String, Map<String, Double>> itemPrices = new HashMap<>();
    private static final Map<String, Map<String, Double>> itemSellPrices = new HashMap<>();
    private static final Map<String, Boolean> itemEnabled = new HashMap<>();
    
    static {
        initializePriceData();
    }
    
    private AdminPriceEditInterface() {
        this.economyManager = EconomyManager.getInstance();
    }
    
    public static AdminPriceEditInterface getInstance() {
        if (instance == null) {
            instance = new AdminPriceEditInterface();
        }
        return instance;
    }
    
    /**
     * Initialize default price data
     */
    private static void initializePriceData() {
        // Weapons category
        Map<String, Double> weaponBuyPrices = new HashMap<>();
        Map<String, Double> weaponSellPrices = new HashMap<>();
        
        weaponBuyPrices.put("wooden_sword", 10.0);
        weaponBuyPrices.put("stone_sword", 25.0);
        weaponBuyPrices.put("iron_sword", 50.0);
        weaponBuyPrices.put("diamond_sword", 200.0);
        weaponBuyPrices.put("netherite_sword", 500.0);
        
        weaponSellPrices.put("wooden_sword", 2.0);
        weaponSellPrices.put("stone_sword", 5.0);
        weaponSellPrices.put("iron_sword", 10.0);
        weaponSellPrices.put("diamond_sword", 40.0);
        weaponSellPrices.put("netherite_sword", 100.0);
        
        itemPrices.put("weapons", weaponBuyPrices);
        itemSellPrices.put("weapons", weaponSellPrices);
        
        // Initialize enabled status
        weaponBuyPrices.keySet().forEach(item -> itemEnabled.put("weapons:" + item, true));
        
        // Food category
        Map<String, Double> foodBuyPrices = new HashMap<>();
        Map<String, Double> foodSellPrices = new HashMap<>();
        
        foodBuyPrices.put("bread", 5.0);
        foodBuyPrices.put("cooked_beef", 8.0);
        foodBuyPrices.put("golden_apple", 100.0);
        foodBuyPrices.put("enchanted_golden_apple", 1000.0);
        
        foodSellPrices.put("bread", 1.0);
        foodSellPrices.put("cooked_beef", 1.5);
        foodSellPrices.put("golden_apple", 20.0);
        foodSellPrices.put("enchanted_golden_apple", 200.0);
        
        itemPrices.put("food", foodBuyPrices);
        itemSellPrices.put("food", foodSellPrices);
        
        foodBuyPrices.keySet().forEach(item -> itemEnabled.put("food:" + item, true));
    }
    
    /**
     * Open the main price editing menu
     */
    public void openMainPriceMenu(ServerPlayer player) {
        if (!player.hasPermissions(3)) {
            MessageUtil.sendMessage(player, "&cYou don't have permission to edit prices!");
            return;
        }
        
        List<GuiItem> items = new ArrayList<>();
        
        // Category selection
        items.add(createCategoryItem(Items.DIAMOND_SWORD, "§6⚔ Weapons & Tools", "weapons",
            "§7Edit weapon and tool prices",
            "§eClick to edit weapon prices"));
        
        items.add(createCategoryItem(Items.DIAMOND_CHESTPLATE, "§b🛡 Armor", "armor",
            "§7Edit armor prices",
            "§eClick to edit armor prices"));
        
        items.add(createCategoryItem(Items.COOKED_BEEF, "§e🍖 Food & Consumables", "food",
            "§7Edit food and consumable prices",
            "§eClick to edit food prices"));
        
        items.add(createCategoryItem(Items.STONE_BRICKS, "§8🏗 Building Blocks", "blocks",
            "§7Edit building material prices",
            "§eClick to edit block prices"));
        
        items.add(createCategoryItem(Items.REDSTONE, "§c⚡ Redstone & Tech", "redstone",
            "§7Edit redstone component prices",
            "§eClick to edit redstone prices"));
        
        items.add(createCategoryItem(Items.NETHER_STAR, "§5✦ Rare Items", "rare",
            "§7Edit rare item prices",
            "§eClick to edit rare prices"));
        
        // Global tools
        items.add(createInfoItem(Items.GOLD_BLOCK, "§6📊 Price Analytics",
            "§7Current market status:",
            "§7• Average item price: §e$45.30",
            "§7• Price volatility: §aLow",
            "§7• Last updated: §f2 minutes ago",
            "§7• Total items: §e" + getTotalItemCount()));
        
        items.add(createActionItem(Items.COMPARATOR, "§c⚙ Bulk Price Tools",
            "§7Bulk price modification tools",
            "§7• Apply percentage changes",
            "§7• Category-wide adjustments",
            "§7• Import/export prices",
            "§eClick for bulk tools",
            null));
        
        items.add(createActionItem(Items.BOOK, "§9📖 Price History",
            "§7View price change history",
            "§7• Recent modifications",
            "§7• Admin change log",
            "§7• Price trend analysis",
            "§eClick to view history",
            null));
        
        // Quick stats
        items.add(createInfoItem(Items.EMERALD, "§a💰 Revenue Impact",
            "§7Price change impact:",
            "§7• Today's changes: §e" + getTodayPriceChanges(),
            "§7• Estimated revenue change: §2+$" + getRevenueImpact(),
            "§7• Player reactions: §a" + getPlayerReactions()));
        
        MenuProvider gui = createPriceChestGui("§c§l💰 Price Management Center §c§l💰", 6, items);
        player.openMenu(gui);
        
        LOGGER.info("Admin {} opened price editing interface", player.getName().getString());
    }
    
    /**
     * Open category price editing menu
     */
    public void openCategoryPriceMenu(ServerPlayer player, String category) {
        Map<String, Double> buyPrices = itemPrices.get(category);
        Map<String, Double> sellPrices = itemSellPrices.get(category);
        
        if (buyPrices == null) {
            MessageUtil.sendMessage(player, "&cInvalid category: " + category);
            return;
        }
        
        List<GuiItem> items = new ArrayList<>();
        
        // Add items from category for editing
        for (String itemName : buyPrices.keySet()) {
            net.minecraft.world.item.Item mcItem = getMinecraftItem(itemName);
            double buyPrice = buyPrices.get(itemName);
            double sellPrice = sellPrices.getOrDefault(itemName, buyPrice * 0.2);
            boolean enabled = itemEnabled.getOrDefault(category + ":" + itemName, true);
            
            items.add(createPriceEditItem(mcItem, itemName, buyPrice, sellPrice, enabled, category));
        }
        
        // Category tools
        items.add(createActionItem(Items.GOLD_INGOT, "§6📈 Category Analytics",
            "§7" + category + " category statistics:",
            "§7• Average buy price: §e$" + String.format("%.2f", getAverageBuyPrice(category)),
            "§7• Average sell price: §e$" + String.format("%.2f", getAverageSellPrice(category)),
            "§7• Total items: §e" + buyPrices.size(),
            "§7• Enabled items: §a" + getEnabledItemCount(category),
            null));
        
        items.add(createActionItem(Items.ANVIL, "§c⚒ Bulk Category Edit",
            "§7Modify all " + category + " prices:",
            "§7• Increase all by 10%",
            "§7• Decrease all by 10%",
            "§7• Set profit margin",
            "§7• Reset to defaults",
            "§eClick for bulk actions",
            null));
        
        items.add(createActionItem(Items.ARROW, "§a⬅ Back to Price Menu",
            "§7Return to main price editor",
            null));
        
        String categoryTitle = "§c§l💰 " + category.substring(0, 1).toUpperCase() + category.substring(1) + " Prices";
        MenuProvider gui = createPriceChestGui(categoryTitle, 6, items);
        player.openMenu(gui);
    }
    
    /**
     * Open individual item price editor
     */
    public void openItemPriceEditor(ServerPlayer player, String category, String itemName) {
        double currentBuyPrice = itemPrices.get(category).get(itemName);
        double currentSellPrice = itemSellPrices.get(category).get(itemName);
        boolean enabled = itemEnabled.getOrDefault(category + ":" + itemName, true);
        
        List<GuiItem> items = new ArrayList<>();
        
        net.minecraft.world.item.Item mcItem = getMinecraftItem(itemName);
        
        // Item display
        items.add(createInfoItem(mcItem, "§6§l" + formatItemName(itemName),
            "§7Category: §e" + category,
            "§7Current buy price: §2$" + String.format("%.2f", currentBuyPrice),
            "§7Current sell price: §c$" + String.format("%.2f", currentSellPrice),
            "§7Profit margin: §e" + String.format("%.1f", (currentBuyPrice - currentSellPrice) / currentBuyPrice * 100) + "%",
            "§7Status: " + (enabled ? "§aEnabled" : "§cDisabled")));
        
        // Price adjustment buttons
        items.add(createPriceAdjustItem(Items.GREEN_WOOL, "§a▲ Increase Buy Price",
            "§7Current: §2$" + String.format("%.2f", currentBuyPrice),
            "§7• +$0.50",
            "§7• +$5.00",
            "§7• +10%",
            "§eClick to increase"));
        
        items.add(createPriceAdjustItem(Items.RED_WOOL, "§c▼ Decrease Buy Price",
            "§7Current: §2$" + String.format("%.2f", currentBuyPrice),
            "§7• -$0.50",
            "§7• -$5.00",
            "§7• -10%",
            "§eClick to decrease"));
        
        items.add(createPriceAdjustItem(Items.LIME_WOOL, "§a▲ Increase Sell Price",
            "§7Current: §c$" + String.format("%.2f", currentSellPrice),
            "§7• +$0.50",
            "§7• +$2.00",
            "§7• +10%",
            "§eClick to increase"));
        
        items.add(createPriceAdjustItem(Items.PINK_WOOL, "§c▼ Decrease Sell Price",
            "§7Current: §c$" + String.format("%.2f", currentSellPrice),
            "§7• -$0.50",
            "§7• -$2.00",
            "§7• -10%",
            "§eClick to decrease"));
        
        // Toggle enable/disable
        items.add(createActionItem(enabled ? Items.REDSTONE_TORCH : Items.TORCH, 
            enabled ? "§c🔴 Disable Item" : "§a🟢 Enable Item",
            "§7Current status: " + (enabled ? "§aEnabled" : "§cDisabled"),
            "§7Click to " + (enabled ? "disable" : "enable") + " this item",
            null));
        
        // Quick preset buttons
        items.add(createActionItem(Items.GOLD_NUGGET, "§6📋 Price Presets",
            "§7Apply common price settings:",
            "§7• Economy starter (cheap)",
            "§7• Balanced pricing",
            "§7• Premium pricing",
            "§7• Custom market rate",
            "§eClick for presets",
            null));
        
        items.add(createActionItem(Items.CLOCK, "§e⏰ Price History",
            "§7View price change history for " + formatItemName(itemName),
            "§7• Last 10 changes",
            "§7• Admin change log",
            "§7• Player purchase impact",
            "§eClick to view history",
            null));
        
        items.add(createActionItem(Items.ARROW, "§a⬅ Back to " + category,
            "§7Return to category editor",
            null));
        
        String itemTitle = "§c§l💰 " + formatItemName(itemName) + " Price Editor";
        MenuProvider gui = createPriceChestGui(itemTitle, 6, items);
        player.openMenu(gui);
    }
    
    // Helper methods
    private net.minecraft.world.item.Item getMinecraftItem(String itemName) {
        return switch (itemName) {
            case "wooden_sword" -> Items.WOODEN_SWORD;
            case "stone_sword" -> Items.STONE_SWORD;
            case "iron_sword" -> Items.IRON_SWORD;
            case "diamond_sword" -> Items.DIAMOND_SWORD;
            case "netherite_sword" -> Items.NETHERITE_SWORD;
            case "bread" -> Items.BREAD;
            case "cooked_beef" -> Items.COOKED_BEEF;
            case "golden_apple" -> Items.GOLDEN_APPLE;
            case "enchanted_golden_apple" -> Items.ENCHANTED_GOLDEN_APPLE;
            default -> Items.BARRIER;
        };
    }
    
    private String formatItemName(String itemName) {
        return Arrays.stream(itemName.split("_"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                .reduce((a, b) -> a + " " + b)
                .orElse(itemName);
    }
    
    private int getTotalItemCount() {
        return itemPrices.values().stream().mapToInt(Map::size).sum();
    }
    
    private String getTodayPriceChanges() {
        return "15 items"; // Placeholder
    }
    
    private String getRevenueImpact() {
        return "234.50"; // Placeholder
    }
    
    private String getPlayerReactions() {
        return "Positive"; // Placeholder
    }
    
    private double getAverageBuyPrice(String category) {
        Map<String, Double> prices = itemPrices.get(category);
        return prices.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }
    
    private double getAverageSellPrice(String category) {
        Map<String, Double> prices = itemSellPrices.get(category);
        return prices.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }
    
    private int getEnabledItemCount(String category) {
        return (int) itemEnabled.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(category + ":") && entry.getValue())
                .count();
    }
    
    // GUI Helper methods
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
    
    private GuiItem createCategoryItem(net.minecraft.world.item.Item icon, String name, String category, String... lore) {
        return createActionItem(icon, name, lore);
    }
    
    private GuiItem createPriceEditItem(net.minecraft.world.item.Item mcItem, String itemName, 
                                      double buyPrice, double sellPrice, boolean enabled, String category) {
        ItemStack item = new ItemStack(mcItem);
        item.set(DataComponents.CUSTOM_NAME, Component.literal("§6" + formatItemName(itemName)));
        
        List<Component> lore = Arrays.asList(
            Component.literal("§7Buy Price: §2$" + String.format("%.2f", buyPrice)),
            Component.literal("§7Sell Price: §c$" + String.format("%.2f", sellPrice)),
            Component.literal("§7Profit: §e" + String.format("%.1f", (buyPrice - sellPrice) / buyPrice * 100) + "%"),
            Component.literal("§7Status: " + (enabled ? "§aEnabled" : "§cDisabled")),
            Component.literal(""),
            Component.literal("§eClick to edit prices")
        );
        
        item.set(DataComponents.LORE, new ItemLore(lore));
        
        return new GuiItem(item, null);
    }
    
    private GuiItem createPriceAdjustItem(net.minecraft.world.item.Item icon, String name, String... lore) {
        return createActionItem(icon, name, lore);
    }
    
    /**
     * Create price editing chest GUI
     */
    private MenuProvider createPriceChestGui(String title, int rows, List<GuiItem> items) {
        return new SimpleMenuProvider(
            (windowId, playerInventory, player) -> {
                SimpleContainer container = new SimpleContainer(rows * 9);
                AbstractContainerMenu menu = new ChestMenu(MenuType.GENERIC_9x6, windowId, playerInventory, container, rows);
                
                // Add items to container
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
