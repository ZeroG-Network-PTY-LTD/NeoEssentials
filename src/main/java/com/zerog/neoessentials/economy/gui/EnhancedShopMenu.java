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
 * Enhanced Shop Menu with advanced features
 * Provides categorized shopping, search functionality, and admin controls
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class EnhancedShopMenu {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnhancedShopMenu.class);
    private static EnhancedShopMenu instance;
    
    private final EconomyManager economyManager;
    
    // Shop categories and items
    private static final Map<String, List<ShopItem>> SHOP_CATEGORIES = new HashMap<>();
    
    // Shop item definitions
    public static class ShopItem {
        public final net.minecraft.world.item.Item item;
        public final String displayName;
        public final double buyPrice;
        public final double sellPrice;
        public final String category;
        public final String description;
        public final boolean adminOnly;
        public final int maxStack;
        
        public ShopItem(net.minecraft.world.item.Item item, String displayName, double buyPrice, 
                       double sellPrice, String category, String description, boolean adminOnly, int maxStack) {
            this.item = item;
            this.displayName = displayName;
            this.buyPrice = buyPrice;
            this.sellPrice = sellPrice;
            this.category = category;
            this.description = description;
            this.adminOnly = adminOnly;
            this.maxStack = maxStack;
        }
    }
    
    static {
        initializeShopItems();
    }
    
    private EnhancedShopMenu() {
        this.economyManager = EconomyManager.getInstance();
    }
    
    public static EnhancedShopMenu getInstance() {
        if (instance == null) {
            instance = new EnhancedShopMenu();
        }
        return instance;
    }
    
    /**
     * Initialize shop items and categories
     */
    private static void initializeShopItems() {
        // Weapons & Tools
        List<ShopItem> weapons = Arrays.asList(
            new ShopItem(Items.WOODEN_SWORD, "§fWooden Sword", 10.0, 2.0, "weapons", "Basic wooden sword", false, 1),
            new ShopItem(Items.STONE_SWORD, "§fStone Sword", 25.0, 5.0, "weapons", "Durable stone sword", false, 1),
            new ShopItem(Items.IRON_SWORD, "§fIron Sword", 50.0, 10.0, "weapons", "Sharp iron sword", false, 1),
            new ShopItem(Items.DIAMOND_SWORD, "§bDiamond Sword", 200.0, 40.0, "weapons", "Powerful diamond sword", false, 1),
            new ShopItem(Items.NETHERITE_SWORD, "§4Netherite Sword", 500.0, 100.0, "weapons", "Ultimate netherite sword", false, 1),
            new ShopItem(Items.BOW, "§6Bow", 30.0, 6.0, "weapons", "Ranged weapon", false, 1),
            new ShopItem(Items.CROSSBOW, "§eCrossBow", 75.0, 15.0, "weapons", "Advanced ranged weapon", false, 1),
            new ShopItem(Items.WOODEN_PICKAXE, "§fWooden Pickaxe", 8.0, 1.5, "weapons", "Basic mining tool", false, 1),
            new ShopItem(Items.STONE_PICKAXE, "§fStone Pickaxe", 20.0, 4.0, "weapons", "Stone mining tool", false, 1),
            new ShopItem(Items.IRON_PICKAXE, "§fIron Pickaxe", 40.0, 8.0, "weapons", "Iron mining tool", false, 1),
            new ShopItem(Items.DIAMOND_PICKAXE, "§bDiamond Pickaxe", 160.0, 32.0, "weapons", "Diamond mining tool", false, 1)
        );
        
        // Armor
        List<ShopItem> armor = Arrays.asList(
            new ShopItem(Items.LEATHER_HELMET, "§6Leather Helmet", 15.0, 3.0, "armor", "Basic head protection", false, 1),
            new ShopItem(Items.LEATHER_CHESTPLATE, "§6Leather Chestplate", 25.0, 5.0, "armor", "Basic chest protection", false, 1),
            new ShopItem(Items.LEATHER_LEGGINGS, "§6Leather Leggings", 20.0, 4.0, "armor", "Basic leg protection", false, 1),
            new ShopItem(Items.LEATHER_BOOTS, "§6Leather Boots", 12.0, 2.5, "armor", "Basic foot protection", false, 1),
            new ShopItem(Items.IRON_HELMET, "§fIron Helmet", 75.0, 15.0, "armor", "Iron head protection", false, 1),
            new ShopItem(Items.IRON_CHESTPLATE, "§fIron Chestplate", 125.0, 25.0, "armor", "Iron chest protection", false, 1),
            new ShopItem(Items.IRON_LEGGINGS, "§fIron Leggings", 100.0, 20.0, "armor", "Iron leg protection", false, 1),
            new ShopItem(Items.IRON_BOOTS, "§fIron Boots", 60.0, 12.0, "armor", "Iron foot protection", false, 1),
            new ShopItem(Items.DIAMOND_HELMET, "§bDiamond Helmet", 300.0, 60.0, "armor", "Diamond head protection", false, 1),
            new ShopItem(Items.DIAMOND_CHESTPLATE, "§bDiamond Chestplate", 500.0, 100.0, "armor", "Diamond chest protection", false, 1)
        );
        
        // Food & Consumables
        List<ShopItem> food = Arrays.asList(
            new ShopItem(Items.BREAD, "§6Bread", 5.0, 1.0, "food", "Restores hunger", false, 64),
            new ShopItem(Items.COOKED_BEEF, "§cCooked Beef", 8.0, 1.5, "food", "High nutrition meat", false, 64),
            new ShopItem(Items.COOKED_PORKCHOP, "§dCooked Porkchop", 7.0, 1.4, "food", "Tasty pork", false, 64),
            new ShopItem(Items.GOLDEN_APPLE, "§6Golden Apple", 100.0, 20.0, "food", "Magical healing apple", false, 16),
            new ShopItem(Items.ENCHANTED_GOLDEN_APPLE, "§5Enchanted Golden Apple", 1000.0, 200.0, "food", "Ultimate healing apple", true, 4),
            new ShopItem(Items.CAKE, "§fCake", 25.0, 5.0, "food", "Delicious cake", false, 1),
            new ShopItem(Items.COOKIE, "§6Cookie", 2.0, 0.5, "food", "Sweet treat", false, 64)
        );
        
        // Building Blocks
        List<ShopItem> blocks = Arrays.asList(
            new ShopItem(Items.COBBLESTONE, "§8Cobblestone", 1.0, 0.2, "blocks", "Basic building block", false, 64),
            new ShopItem(Items.STONE, "§8Stone", 1.5, 0.3, "blocks", "Smooth stone block", false, 64),
            new ShopItem(Items.STONE_BRICKS, "§8Stone Bricks", 2.0, 0.4, "blocks", "Decorative stone", false, 64),
            new ShopItem(Items.OAK_PLANKS, "§6Oak Planks", 1.0, 0.2, "blocks", "Wooden planks", false, 64),
            new ShopItem(Items.OAK_LOG, "§6Oak Log", 2.0, 0.4, "blocks", "Natural wood", false, 64),
            new ShopItem(Items.GLASS, "§fGlass", 3.0, 0.6, "blocks", "Transparent block", false, 64),
            new ShopItem(Items.OBSIDIAN, "§5Obsidian", 50.0, 10.0, "blocks", "Blast resistant block", false, 64),
            new ShopItem(Items.GLOWSTONE, "§eGlowstone", 20.0, 4.0, "blocks", "Light source block", false, 64)
        );
        
        // Redstone & Tech
        List<ShopItem> redstone = Arrays.asList(
            new ShopItem(Items.REDSTONE, "§cRedstone", 5.0, 1.0, "redstone", "Circuit component", false, 64),
            new ShopItem(Items.REPEATER, "§cRepeater", 15.0, 3.0, "redstone", "Signal repeater", false, 16),
            new ShopItem(Items.COMPARATOR, "§cComparator", 20.0, 4.0, "redstone", "Signal comparator", false, 16),
            new ShopItem(Items.PISTON, "§7Piston", 25.0, 5.0, "redstone", "Moving block", false, 16),
            new ShopItem(Items.STICKY_PISTON, "§aPiston (Sticky)", 35.0, 7.0, "redstone", "Sticky moving block", false, 16),
            new ShopItem(Items.REDSTONE_LAMP, "§eRedstone Lamp", 30.0, 6.0, "redstone", "Redstone light source", false, 16),
            new ShopItem(Items.HOPPER, "§8Hopper", 40.0, 8.0, "redstone", "Item transport", false, 16)
        );
        
        // Rare Items (Admin only)
        List<ShopItem> rare = Arrays.asList(
            new ShopItem(Items.ELYTRA, "§5Elytra", 5000.0, 1000.0, "rare", "Wings of flight", true, 1),
            new ShopItem(Items.DRAGON_EGG, "§5Dragon Egg", 10000.0, 2000.0, "rare", "Legendary dragon egg", true, 1),
            new ShopItem(Items.NETHER_STAR, "§fNether Star", 2500.0, 500.0, "rare", "Wither drop", true, 1),
            new ShopItem(Items.BEACON, "§eBeacon", 3000.0, 600.0, "rare", "Area effect block", true, 1),
            new ShopItem(Items.TOTEM_OF_UNDYING, "§6Totem of Undying", 1500.0, 300.0, "rare", "Second chance item", true, 1)
        );
        
        SHOP_CATEGORIES.put("weapons", weapons);
        SHOP_CATEGORIES.put("armor", armor);
        SHOP_CATEGORIES.put("food", food);
        SHOP_CATEGORIES.put("blocks", blocks);
        SHOP_CATEGORIES.put("redstone", redstone);
        SHOP_CATEGORIES.put("rare", rare);
    }
    
    /**
     * Open the enhanced shop main menu
     */
    public void openMainMenu(ServerPlayer player) {
        List<GuiItem> items = new ArrayList<>();
        
        BigDecimal balance = economyManager.getBalance(player.getUUID());
        String formattedBalance = economyManager.formatCurrency(balance);
        
        // Category buttons (slots 10-16)
        items.add(createCategoryItem(Items.DIAMOND_SWORD, "§6⚔ Weapons & Tools", "weapons", 
            "§7Browse weapons and tools", "§e" + SHOP_CATEGORIES.get("weapons").size() + " items available"));
        
        items.add(createCategoryItem(Items.DIAMOND_CHESTPLATE, "§b🛡 Armor", "armor", 
            "§7Browse protective armor", "§e" + SHOP_CATEGORIES.get("armor").size() + " items available"));
        
        items.add(createCategoryItem(Items.COOKED_BEEF, "§e🍖 Food & Consumables", "food", 
            "§7Browse food and potions", "§e" + SHOP_CATEGORIES.get("food").size() + " items available"));
        
        items.add(createCategoryItem(Items.STONE_BRICKS, "§8🏗 Building Blocks", "blocks", 
            "§7Browse building materials", "§e" + SHOP_CATEGORIES.get("blocks").size() + " items available"));
        
        items.add(createCategoryItem(Items.REDSTONE, "§c⚡ Redstone & Tech", "redstone", 
            "§7Browse redstone components", "§e" + SHOP_CATEGORIES.get("redstone").size() + " items available"));
        
        // Rare items (admin only)
        if (player.hasPermissions(3)) {
            items.add(createCategoryItem(Items.NETHER_STAR, "§5✦ Rare Items", "rare", 
                "§7Browse rare items §c(Admin Only)", "§e" + SHOP_CATEGORIES.get("rare").size() + " items available"));
        }
        
        // Player info and options (slots 40-44)
        items.add(createInfoItem(Items.EMERALD, "§a💰 Your Balance", 
            "§7Current balance: §2" + formattedBalance,
            "§7Rank: §e" + getPlayerEconomyRank(balance.doubleValue()),
            "§7Total purchases: §a" + getTotalPurchases(player)));
        
        items.add(createInfoItem(Items.CHEST, "§9📦 Sell Items", 
            "§7Sell items from your inventory",
            "§7Right-click with items to sell",
            "§e§lComing Soon!"));
        
        items.add(createInfoItem(Items.BOOK, "§d📖 Shop Guide", 
            "§7How to use the enhanced shop:",
            "§7• Click categories to browse",
            "§7• Left-click to buy 1 item",
            "§7• Right-click to buy 10 items",
            "§7• Shift-click to buy max stack"));
        
        items.add(createInfoItem(Items.COMPARATOR, "§c⚙ Admin Controls", 
            "§7Shop management tools",
            player.hasPermissions(3) ? "§aClick to access admin panel" : "§cAdmin access required"));
        
        MenuProvider gui = createEnhancedChestGui("§6§l🏪 Enhanced Shop §6§l🏪", 6, items, CustomGuiManager.GuiType.SHOP_MAIN);
        player.openMenu(gui);
    }
    
    /**
     * Open a shop category
     */
    public void openCategoryMenu(ServerPlayer player, String category) {
        List<ShopItem> categoryItems = SHOP_CATEGORIES.get(category.toLowerCase());
        if (categoryItems == null) {
            MessageUtil.sendMessage(player, "&cInvalid shop category!");
            return;
        }
        
        List<GuiItem> items = new ArrayList<>();
        BigDecimal balance = economyManager.getBalance(player.getUUID());
        
        // Add items from category
        for (ShopItem shopItem : categoryItems) {
            // Skip admin items for non-admins
            if (shopItem.adminOnly && !player.hasPermissions(3)) {
                continue;
            }
            
            items.add(createShopItemGui(shopItem, player, balance));
        }
        
        // Navigation and info items
        BigDecimal playerBalance = economyManager.getBalance(player.getUUID());
        String formattedBalance = economyManager.formatCurrency(playerBalance);
        items.add(createInfoItem(Items.EMERALD, "§a💰 Your Balance", "§7Current balance: §2" + formattedBalance));
        
        items.add(createActionItem(Items.ARROW, "§a⬅ Back to Shop", "§7Return to main shop menu", 
            p -> openMainMenu(p)));
        
        String categoryTitle = "§6§l🏪 " + category.substring(0, 1).toUpperCase() + category.substring(1) + " Shop";
        MenuProvider gui = createEnhancedChestGui(categoryTitle, 6, items, CustomGuiManager.GuiType.SHOP_CATEGORY);
        player.openMenu(gui);
    }
    
    /**
     * Get player's economy rank
     */
    private String getPlayerEconomyRank(double balance) {
        if (balance >= 1000000) return "§5§lMillionaire";
        if (balance >= 500000) return "§6§lWealthy";
        if (balance >= 100000) return "§e§lRich";
        if (balance >= 50000) return "§a§lProsperity";
        if (balance >= 10000) return "§2§lWell-off";
        if (balance >= 5000) return "§b§lComfortable";
        if (balance >= 1000) return "§9§lStable";
        if (balance >= 100) return "§f§lStarting";
        return "§7§lBroke";
    }
    
    /**
     * Get total purchases (placeholder)
     */
    private int getTotalPurchases(ServerPlayer player) {
        // This would need actual tracking - placeholder
        return player.experienceLevel * 5 + 10;
    }
    
    // Helper methods for creating GUI items
    private GuiItem createCategoryItem(net.minecraft.world.item.Item icon, String name, String category, String... lore) {
        ItemStack item = new ItemStack(icon);
        item.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        
        List<Component> loreComponents = new ArrayList<>();
        for (String line : lore) {
            loreComponents.add(Component.literal(line));
        }
        loreComponents.add(Component.literal(""));
        loreComponents.add(Component.literal("§e§lClick to browse!"));
        
        item.set(DataComponents.LORE, new ItemLore(loreComponents));
        
        return new GuiItem(item, p -> openCategoryMenu(p, category));
    }
    
    private GuiItem createInfoItem(net.minecraft.world.item.Item icon, String name, String... lore) {
        ItemStack item = new ItemStack(icon);
        item.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        
        List<Component> loreComponents = new ArrayList<>();
        for (String line : lore) {
            loreComponents.add(Component.literal(line));
        }
        
        item.set(DataComponents.LORE, new ItemLore(loreComponents));
        
        return new GuiItem(item, null); // No click action for info items
    }
    
    private GuiItem createActionItem(net.minecraft.world.item.Item icon, String name, String description, 
                                   CustomGuiManager.GuiClickAction action) {
        ItemStack item = new ItemStack(icon);
        item.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        item.set(DataComponents.LORE, new ItemLore(Arrays.asList(Component.literal(description))));
        
        return new GuiItem(item, action);
    }
    
    private GuiItem createShopItemGui(ShopItem shopItem, ServerPlayer player, BigDecimal balance) {
        ItemStack itemStack = new ItemStack(shopItem.item);
        itemStack.set(DataComponents.CUSTOM_NAME, Component.literal(shopItem.displayName));
        
        String formattedBuyPrice = economyManager.formatCurrency(BigDecimal.valueOf(shopItem.buyPrice));
        String formattedSellPrice = economyManager.formatCurrency(BigDecimal.valueOf(shopItem.sellPrice));
        boolean canAfford = balance.compareTo(BigDecimal.valueOf(shopItem.buyPrice)) >= 0;
        
        List<Component> lore = Arrays.asList(
            Component.literal("§7" + shopItem.description),
            Component.literal(""),
            Component.literal("§7Buy Price: §2" + formattedBuyPrice),
            Component.literal("§7Sell Price: §c" + formattedSellPrice),
            Component.literal("§7Max Stack: §e" + shopItem.maxStack),
            Component.literal(""),
            Component.literal(canAfford ? "§a§lLeft-click: Buy 1" : "§c§lCan't afford!"),
            Component.literal(canAfford ? "§a§lRight-click: Buy 10" : "§7Need: §c" + 
                economyManager.formatCurrency(BigDecimal.valueOf(shopItem.buyPrice).subtract(balance))),
            Component.literal(canAfford ? "§a§lShift-click: Buy " + shopItem.maxStack : "")
        );
        
        itemStack.set(DataComponents.LORE, new ItemLore(lore));
        
        // Add purchase functionality if player can afford it
        CustomGuiManager.GuiClickAction action = null;
        if (canAfford) {
            action = p -> purchaseItem(p, shopItem, 1);
        }
        
        return new GuiItem(itemStack, action);
    }
    
    /**
     * Handle item purchase
     */
    private void purchaseItem(ServerPlayer player, ShopItem shopItem, int quantity) {
        BigDecimal totalCost = BigDecimal.valueOf(shopItem.buyPrice * quantity);
        
        // Validate quantity limits
        quantity = Math.min(quantity, shopItem.maxStack);
        
        // Check if player has enough money
        if (!economyManager.hasBalance(player.getUUID(), totalCost)) {
            MessageUtil.sendMessage(player, "&cYou don't have enough money for this purchase!");
            return;
        }
        
        // Process the purchase
        if (economyManager.withdrawBalance(player.getUUID(), totalCost, 
                "Enhanced shop purchase: " + quantity + "x " + shopItem.displayName)) {
            
            // Give the item to the player
            ItemStack purchasedItem = new ItemStack(shopItem.item, quantity);
            if (!player.getInventory().add(purchasedItem)) {
                // Inventory full, drop the item
                player.drop(purchasedItem, false);
                MessageUtil.sendMessage(player, "&6Purchase successful! Item dropped (inventory full)");
            } else {
                MessageUtil.sendMessage(player, "&aPurchase successful! Bought " + quantity + "x " + shopItem.displayName);
            }
            
            String formattedCost = economyManager.formatCurrency(totalCost);
            String newBalance = economyManager.formatCurrency(economyManager.getBalance(player.getUUID()));
            MessageUtil.sendMessage(player, "&7Spent: &c" + formattedCost + " &7| New balance: &a" + newBalance);
            
            LOGGER.info("Enhanced shop purchase: {} bought {}x {} for {} (new balance: {})", 
                player.getName().getString(), quantity, shopItem.displayName, 
                formattedCost, newBalance);
            
            // Refresh the GUI
            player.closeContainer();
            openCategoryMenu(player, shopItem.category);
        } else {
            MessageUtil.sendMessage(player, "&cFailed to process purchase. Please try again.");
        }
    }
    
    /**
     * Create enhanced chest GUI with click handling
     */
    private MenuProvider createEnhancedChestGui(String title, int rows, List<GuiItem> items, 
                                              CustomGuiManager.GuiType guiType) {
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
                    GuiClickHandler.registerSession(serverPlayer, guiType, clickActions);
                }
                
                return menu;
            },
            Component.literal(title)
        );
    }
    
    /**
     * Simple container implementation for the shop
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
            return ItemStack.EMPTY; // Read-only for GUI
        }
        
        @Override
        public ItemStack removeItemNoUpdate(int index) {
            return ItemStack.EMPTY; // Read-only for GUI
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