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
import com.zerog.neoessentials.gui.GuiClickHandler;
import com.zerog.neoessentials.gui.CustomGuiManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * Shop Interface
 * Advanced shop browsing and interaction system
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class ShopInterface {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShopInterface.class);
    private static ShopInterface instance;
    
    // Shop data storage
    private static final Map<String, ShopData> availableShops = new HashMap<>();
    private static final Map<String, ShopFilter> userFilters = new HashMap<>();
    
    static {
        initializeShopData();
    }
    
    private ShopInterface() {
    }
    
    public static ShopInterface getInstance() {
        if (instance == null) {
            instance = new ShopInterface();
        }
        return instance;
    }
    
    /**
     * Initialize sample shop data
     */
    private static void initializeShopData() {
        // Player shops
        availableShops.put("steve_general", new ShopData("Steve's General Store", "Steve",
                ShopType.PLAYER, "general", "spawn", 4.8, 156, true, 45.0));
        
        availableShops.put("alex_weapons", new ShopData("Alex's Armory", "Alex",
                ShopType.PLAYER, "weapons", "market", 4.6, 89, true, 150.0));
        
        availableShops.put("herobrine_food", new ShopData("Herobrine's Kitchen", "Herobrine",
                ShopType.PLAYER, "food", "spawn", 4.9, 234, false, 25.0));
        
        // Admin shops
        availableShops.put("server_economy", new ShopData("Server Economy Hub", "Server",
                ShopType.ADMIN, "all", "spawn", 5.0, 9999, true, 1000.0));
        
        availableShops.put("rare_items", new ShopData("Rare Items Emporium", "Server",
                ShopType.ADMIN, "rare", "market", 4.7, 445, true, 5000.0));
    }
    
    /**
     * Open the main shop browser
     */
    public void openShopBrowser(ServerPlayer player) {
        List<GuiItem> items = new ArrayList<>();
        
        // Filter controls
        items.add(createActionItem(Items.COMPASS, "§6🔍 Search & Filter",
            "§7Filter shops by:",
            "§7• Category",
            "§7• Distance", 
            "§7• Rating",
            "§7• Online status",
            "§eClick to set filters"));
        
        items.add(createActionItem(Items.WRITABLE_BOOK, "§a📝 Quick Search",
            "§7Search shops by name",
            "§7Type shop name or owner",
            "§eClick to search"));
        
        // Category filters
        items.add(createCategoryFilter(Items.EMERALD_BLOCK, "§2💎 All Shops",
                "all", player, availableShops.size()));
        
        items.add(createCategoryFilter(Items.CHEST, "§6📦 General Stores",
                "general", player, getShopCountByCategory("general")));
        
        items.add(createCategoryFilter(Items.DIAMOND_SWORD, "§c⚔ Weapon Shops",
                "weapons", player, getShopCountByCategory("weapons")));
        
        items.add(createCategoryFilter(Items.COOKED_BEEF, "§e🍖 Food Markets",
                "food", player, getShopCountByCategory("food")));
        
        items.add(createCategoryFilter(Items.NETHER_STAR, "§5✦ Rare Items",
                "rare", player, getShopCountByCategory("rare")));
        
        // Current filter status
        ShopFilter filter = userFilters.getOrDefault(player.getStringUUID(), new ShopFilter());
        items.add(createInfoItem(Items.PAPER, "§9📋 Current Filters",
            "§7Active filters:",
            "§7• Category: §e" + filter.getCategory(),
            "§7• Max distance: §e" + filter.getMaxDistance() + " blocks",
            "§7• Min rating: §e" + filter.getMinRating() + "★",
            "§7• Online only: " + (filter.isOnlineOnly() ? "§aYes" : "§cNo"),
            "§7• Results: §e" + getFilteredShopCount(filter)));
        
        // Sort options
        items.add(createActionItem(Items.CLOCK, "§d📊 Sort Options",
            "§7Sort shops by:",
            "§7• Distance (closest first)",
            "§7• Rating (highest first)",
            "§7• Activity (most active)",
            "§7• Price (cheapest first)",
            "§eClick to change sorting"));
        
        // Shop recommendations
        items.add(createInfoItem(Items.BEACON, "§6⭐ Recommended",
            "§7Top recommended shops:",
            "§7• §a" + getTopShop("rating").getName() + " §7(5.0★)",
            "§7• §a" + getTopShop("activity").getName() + " §7(Active)",
            "§7• §a" + getTopShop("distance").getName() + " §7(Nearby)"));
        
        MenuProvider gui = createChestGui("§6§l🏪 Shop Browser §6§l🏪", 6, items);
        player.openMenu(gui);
        
        LOGGER.info("Player {} opened shop browser", player.getName().getString());
    }
    
    /**
     * Open filtered shop list
     */
    public void openShopList(ServerPlayer player, String category) {
        ShopFilter filter = userFilters.getOrDefault(player.getStringUUID(), new ShopFilter());
        filter.setCategory(category);
        
        List<ShopData> filteredShops = getFilteredShops(filter);
        List<GuiItem> items = new ArrayList<>();
        
        // Add shop entries
        for (ShopData shop : filteredShops) {
            items.add(createShopItem(shop, player));
        }
        
        // Fill remaining slots with controls
        while (items.size() < 45) { // Leave room for controls
            items.add(createInfoItem(Items.GRAY_STAINED_GLASS_PANE, "§7", ""));
        }
        
        // Control buttons
        items.add(createActionItem(Items.ARROW, "§a⬅ Back to Browser",
            "§7Return to shop browser"));
        
        items.add(createActionItem(Items.REDSTONE, "§c🔄 Refresh List",
            "§7Refresh shop listings",
            "§7Last updated: §ejust now"));
        
        items.add(createActionItem(Items.MAP, "§9🗺 View on Map",
            "§7Show shops on world map",
            "§7Visual shop locations"));
        
        String title = "§6§l🏪 " + getCategoryDisplayName(category) + " Shops §6§l🏪";
        MenuProvider gui = createChestGui(title, 6, items);
        player.openMenu(gui);
    }
    
    // Helper methods
    private int getShopCountByCategory(String category) {
        return (int) availableShops.values().stream()
                .filter(shop -> category.equals("all") || shop.getCategory().equals(category))
                .count();
    }
    
    private int getFilteredShopCount(ShopFilter filter) {
        return getFilteredShops(filter).size();
    }
    
    private List<ShopData> getFilteredShops(ShopFilter filter) {
        return availableShops.values().stream()
                .filter(shop -> filter.matches(shop))
                .sorted((a, b) -> Double.compare(b.getRating(), a.getRating()))
                .toList();
    }
    
    private ShopData getTopShop(String criteria) {
        return switch (criteria) {
            case "rating" -> availableShops.values().stream()
                    .max(Comparator.comparing(ShopData::getRating))
                    .orElse(availableShops.values().iterator().next());
            case "activity" -> availableShops.values().stream()
                    .max(Comparator.comparing(ShopData::getTransactions))
                    .orElse(availableShops.values().iterator().next());
            default -> availableShops.values().iterator().next();
        };
    }
    
    private String getCategoryDisplayName(String category) {
        return switch (category) {
            case "all" -> "All";
            case "general" -> "General";
            case "weapons" -> "Weapons";
            case "food" -> "Food";
            case "rare" -> "Rare Items";
            default -> category;
        };
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
        
        return new GuiItem(item, null);
    }
    
    private GuiItem createCategoryFilter(net.minecraft.world.item.Item icon, String name, String category, ServerPlayer player, int count) {
        return createActionItem(icon, name,
            "§7Category: §e" + category,
            "§7Available shops: §a" + count,
            "§eClick to filter");
    }
    
    private GuiItem createShopItem(ShopData shop, ServerPlayer player) {
        net.minecraft.world.item.Item icon = switch (shop.getCategory()) {
            case "weapons" -> Items.DIAMOND_SWORD;
            case "food" -> Items.COOKED_BEEF;
            case "rare" -> Items.NETHER_STAR;
            case "general" -> Items.CHEST;
            default -> Items.EMERALD_BLOCK;
        };
        
        return createActionItem(icon, "§6" + shop.getName(),
            "§7Owner: §e" + shop.getOwner(),
            "§7Rating: §e" + shop.getRating() + "★ §7(" + shop.getTransactions() + " sales)",
            "§7Location: §e" + shop.getLocation(),
            "§7Status: " + (shop.isOnline() ? "§aOnline" : "§cOffline"),
            "§7Avg price: §2$" + String.format("%.2f", shop.getAveragePrice()),
            "§eClick to visit");
    }
    
    /**
     * Create chest GUI
     */
    private MenuProvider createChestGui(String title, int rows, List<GuiItem> items) {
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
     * Shop data class
     */
    public static class ShopData {
        private final String name;
        private final String owner;
        private final ShopType type;
        private final String category;
        private final String location;
        private final double rating;
        private final int transactions;
        private final boolean online;
        private final double averagePrice;
        
        public ShopData(String name, String owner, ShopType type, String category, 
                       String location, double rating, int transactions, boolean online, double averagePrice) {
            this.name = name;
            this.owner = owner;
            this.type = type;
            this.category = category;
            this.location = location;
            this.rating = rating;
            this.transactions = transactions;
            this.online = online;
            this.averagePrice = averagePrice;
        }
        
        // Getters
        public String getName() { return name; }
        public String getOwner() { return owner; }
        public ShopType getType() { return type; }
        public String getCategory() { return category; }
        public String getLocation() { return location; }
        public double getRating() { return rating; }
        public int getTransactions() { return transactions; }
        public boolean isOnline() { return online; }
        public double getAveragePrice() { return averagePrice; }
    }
    
    /**
     * Shop filter class
     */
    public static class ShopFilter {
        private String category = "all";
        private double maxDistance = 1000.0;
        private double minRating = 0.0;
        private boolean onlineOnly = false;
        
        public boolean matches(ShopData shop) {
            if (!category.equals("all") && !shop.getCategory().equals(category)) {
                return false;
            }
            if (shop.getRating() < minRating) {
                return false;
            }
            if (onlineOnly && !shop.isOnline()) {
                return false;
            }
            return true;
        }
        
        // Getters and setters
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public double getMaxDistance() { return maxDistance; }
        public double getMinRating() { return minRating; }
        public boolean isOnlineOnly() { return onlineOnly; }
    }
    
    /**
     * Shop types
     */
    public enum ShopType {
        PLAYER,
        ADMIN,
        AUCTION
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
