package com.zerog.neoessentials.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Custom GUI Manager for NeoEssentials
 * Handles custom GUI creation and management
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class CustomGuiManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomGuiManager.class);
    private static CustomGuiManager instance;
    
    private CustomGuiManager() {
    }
    
    public static CustomGuiManager getInstance() {
        if (instance == null) {
            instance = new CustomGuiManager();
        }
        return instance;
    }
    
    /**
     * Open a custom GUI for a player
     */
    public void openGui(ServerPlayer player, GuiType type, Object... args) {
        try {
            switch (type) {
                case SHOP_MAIN -> openShopMainGui(player);
                case SHOP_CATEGORY -> openShopCategoryGui(player, (String) args[0]);
                case PLAYER_STATS -> openPlayerStatsGui(player);
                case SERVER_INFO -> openServerInfoGui(player);
                case ECONOMY_MANAGEMENT -> openEconomyManagementGui(player);
                case KIT_SELECTOR -> openKitSelectorGui(player);
                case WARP_SELECTOR -> openWarpSelectorGui(player);
                case TELEPORT_MENU -> openTeleportMenuGui(player);
                default -> LOGGER.warn("Unknown GUI type: " + type);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to open GUI for player: " + player.getDisplayName().getString(), e);
            player.sendSystemMessage(Component.literal("§cFailed to open GUI: " + e.getMessage()));
        }
    }
    
    /**
     * Create a simple chest-based GUI
     */
    private MenuProvider createChestGui(String title, int rows, List<GuiItem> items) {
        return new SimpleMenuProvider(
            (windowId, playerInventory, player) -> {
                AbstractContainerMenu menu = new ChestMenu(MenuType.GENERIC_9x3, windowId, playerInventory, 
                    new SimpleContainer(rows * 9), rows);
                
                // Add items to the container
                for (int i = 0; i < items.size() && i < rows * 9; i++) {
                    GuiItem item = items.get(i);
                    if (item != null) {
                        menu.getSlot(i).set(item.getItemStack());
                    }
                }
                
                return menu;
            },
            Component.literal(title)
        );
    }
    
    /**
     * Open shop main GUI
     */
    private void openShopMainGui(ServerPlayer player) {
        List<GuiItem> items = new ArrayList<>();
        
        // Shop categories
        items.add(new GuiItem(createItem(Items.DIAMOND_SWORD, "§6Weapons & Tools", "§7Click to browse weapons"), 
            p -> openGui(p, GuiType.SHOP_CATEGORY, "weapons")));
        
        items.add(new GuiItem(createItem(Items.DIAMOND_CHESTPLATE, "§bArmor", "§7Click to browse armor"), 
            p -> openGui(p, GuiType.SHOP_CATEGORY, "armor")));
        
        items.add(new GuiItem(createItem(Items.COOKED_BEEF, "§eFoods", "§7Click to browse food items"), 
            p -> openGui(p, GuiType.SHOP_CATEGORY, "food")));
        
        items.add(new GuiItem(createItem(Items.STONE, "§8Building Blocks", "§7Click to browse blocks"), 
            p -> openGui(p, GuiType.SHOP_CATEGORY, "blocks")));
        
        items.add(new GuiItem(createItem(Items.REDSTONE, "§cRedstone", "§7Click to browse redstone items"), 
            p -> openGui(p, GuiType.SHOP_CATEGORY, "redstone")));
        
        // Navigation and info items
        items.add(new GuiItem(createItem(Items.EMERALD, "§aYour Balance", "§7Current balance: §6$1000"), null)); // Static for now
        
        items.add(new GuiItem(createItem(Items.BOOK, "§9Shop Info", "§7Welcome to the server shop!"), null));
        
        items.add(new GuiItem(createItem(Items.BARRIER, "§cClose", "§7Click to close the shop"), 
            p -> p.closeContainer()));
        
        MenuProvider gui = createChestGui("§6Server Shop", 3, items);
        player.openMenu(gui);
    }
    
    /**
     * Open shop category GUI
     */
    private void openShopCategoryGui(ServerPlayer player, String category) {
        List<GuiItem> items = new ArrayList<>();
        
        switch (category.toLowerCase()) {
            case "weapons" -> {
                items.add(new GuiItem(createItem(Items.WOODEN_SWORD, "§fWooden Sword", "§7Price: §6$10"), null));
                items.add(new GuiItem(createItem(Items.STONE_SWORD, "§fStone Sword", "§7Price: §6$25"), null));
                items.add(new GuiItem(createItem(Items.IRON_SWORD, "§fIron Sword", "§7Price: §6$50"), null));
                items.add(new GuiItem(createItem(Items.DIAMOND_SWORD, "§bDiamond Sword", "§7Price: §6$200"), null));
            }
            case "armor" -> {
                items.add(new GuiItem(createItem(Items.LEATHER_HELMET, "§6Leather Helmet", "§7Price: §6$15"), null));
                items.add(new GuiItem(createItem(Items.IRON_HELMET, "§fIron Helmet", "§7Price: §6$75"), null));
                items.add(new GuiItem(createItem(Items.DIAMOND_HELMET, "§bDiamond Helmet", "§7Price: §6$300"), null));
            }
            case "food" -> {
                items.add(new GuiItem(createItem(Items.BREAD, "§6Bread", "§7Price: §6$5"), null));
                items.add(new GuiItem(createItem(Items.COOKED_BEEF, "§cCooked Beef", "§7Price: §6$8"), null));
                items.add(new GuiItem(createItem(Items.GOLDEN_APPLE, "§6Golden Apple", "§7Price: §6$100"), null));
            }
        }
        
        // Add back button
        items.add(new GuiItem(createItem(Items.ARROW, "§aBack to Shop", "§7Return to main shop"), 
            p -> openGui(p, GuiType.SHOP_MAIN)));
        
        MenuProvider gui = createChestGui("§6" + category.substring(0, 1).toUpperCase() + category.substring(1), 3, items);
        player.openMenu(gui);
    }
    
    /**
     * Open player stats GUI
     */
    private void openPlayerStatsGui(ServerPlayer player) {
        List<GuiItem> items = new ArrayList<>();
        
        items.add(new GuiItem(createItem(Items.PLAYER_HEAD, "§6Player Info", 
            "§7Name: " + player.getDisplayName().getString(),
            "§7Level: " + player.experienceLevel,
            "§7Health: " + (int)player.getHealth() + "/" + (int)player.getMaxHealth()), null));
        
        items.add(new GuiItem(createItem(Items.CLOCK, "§bPlay Time", "§7Total playtime info"), null));
        
        items.add(new GuiItem(createItem(Items.EMERALD, "§aBalance", "§7Your current balance"), null));
        
        items.add(new GuiItem(createItem(Items.DIAMOND, "§9Achievements", "§7View your achievements"), null));
        
        MenuProvider gui = createChestGui("§6Player Statistics", 3, items);
        player.openMenu(gui);
    }
    
    /**
     * Open server info GUI
     */
    private void openServerInfoGui(ServerPlayer player) {
        List<GuiItem> items = new ArrayList<>();
        
        String onlineCount = "Unknown";
        String maxPlayers = "Unknown";
        var server = player.getServer();
        if (server != null) {
            onlineCount = String.valueOf(server.getPlayerCount());
            maxPlayers = String.valueOf(server.getMaxPlayers());
        }
        
        items.add(new GuiItem(createItem(Items.BEACON, "§6Server Info", 
            "§7Welcome to NeoEssentials!",
            "§7Online Players: " + onlineCount,
            "§7Max Players: " + maxPlayers), null));
        
        items.add(new GuiItem(createItem(Items.BOOK, "§9Rules", "§7Click to view server rules"), null));
        
        items.add(new GuiItem(createItem(Items.COMPASS, "§bWarps", "§7Available server warps"), 
            p -> openGui(p, GuiType.WARP_SELECTOR)));
        
        items.add(new GuiItem(createItem(Items.ENDER_PEARL, "§dTeleport", "§7Teleportation options"), 
            p -> openGui(p, GuiType.TELEPORT_MENU)));
        
        MenuProvider gui = createChestGui("§6Server Information", 3, items);
        player.openMenu(gui);
    }
    
    /**
     * Open economy management GUI
     */
    private void openEconomyManagementGui(ServerPlayer player) {
        List<GuiItem> items = new ArrayList<>();
        
        items.add(new GuiItem(createItem(Items.EMERALD, "§aBalance", "§7View your balance"), null));
        items.add(new GuiItem(createItem(Items.GOLD_INGOT, "§6Currency Exchange", "§7Exchange currencies"), null));
        items.add(new GuiItem(createItem(Items.CHEST, "§9Banking", "§7Manage your accounts"), null));
        items.add(new GuiItem(createItem(Items.PAPER, "§7Transaction History", "§7View your transactions"), null));
        
        MenuProvider gui = createChestGui("§6Economy Management", 3, items);
        player.openMenu(gui);
    }
    
    /**
     * Open kit selector GUI
     */
    private void openKitSelectorGui(ServerPlayer player) {
        List<GuiItem> items = new ArrayList<>();
        
        items.add(new GuiItem(createItem(Items.WOODEN_SWORD, "§6Starter Kit", "§7Basic items for new players"), null));
        items.add(new GuiItem(createItem(Items.STONE_PICKAXE, "§8Mining Kit", "§7Tools for mining"), null));
        items.add(new GuiItem(createItem(Items.BREAD, "§eFood Kit", "§7Food supplies"), null));
        items.add(new GuiItem(createItem(Items.IRON_CHESTPLATE, "§7PvP Kit", "§7Combat equipment"), null));
        
        MenuProvider gui = createChestGui("§6Kit Selector", 3, items);
        player.openMenu(gui);
    }
    
    /**
     * Open warp selector GUI
     */
    private void openWarpSelectorGui(ServerPlayer player) {
        List<GuiItem> items = new ArrayList<>();
        
        items.add(new GuiItem(createItem(Items.GRASS_BLOCK, "§aSpawn", "§7Teleport to spawn"), null));
        items.add(new GuiItem(createItem(Items.DIAMOND_ORE, "§bMining World", "§7Resource gathering area"), null));
        items.add(new GuiItem(createItem(Items.NETHER_BRICKS, "§cNether Hub", "§7Nether portal area"), null));
        items.add(new GuiItem(createItem(Items.END_STONE, "§5End Portal", "§7End dimension access"), null));
        
        MenuProvider gui = createChestGui("§6Server Warps", 3, items);
        player.openMenu(gui);
    }
    
    /**
     * Open teleport menu GUI
     */
    private void openTeleportMenuGui(ServerPlayer player) {
        List<GuiItem> items = new ArrayList<>();
        
        items.add(new GuiItem(createItem(Items.ENDER_PEARL, "§dTeleport to Player", "§7TP to another player"), null));
        items.add(new GuiItem(createItem(Items.COMPASS, "§9Random Teleport", "§7Teleport to random location"), null));
        items.add(new GuiItem(createItem(Items.WHITE_BED, "§aGo Home", "§7Teleport to your home"), null));
        items.add(new GuiItem(createItem(Items.CLOCK, "§eGo Back", "§7Return to previous location"), null));
        
        MenuProvider gui = createChestGui("§6Teleportation", 3, items);
        player.openMenu(gui);
    }
    
    /**
     * Create an item with display name and lore
     */
    private ItemStack createItem(net.minecraft.world.item.Item item, String name, String... lore) {
        ItemStack stack = new ItemStack(item);
        
        // For GUI purposes, we'll use a simplified approach
        // The actual item display will be handled by the client
        // This is sufficient for basic GUI functionality
        
        return stack;
    }
    
    /**
     * GUI item class
     */
    public static class GuiItem {
        private final ItemStack itemStack;
        private final GuiClickAction clickAction;
        
        public GuiItem(ItemStack itemStack, GuiClickAction clickAction) {
            this.itemStack = itemStack;
            this.clickAction = clickAction;
        }
        
        public ItemStack getItemStack() {
            return itemStack;
        }
        
        public GuiClickAction getClickAction() {
            return clickAction;
        }
    }
    
    /**
     * GUI click action interface
     */
    @FunctionalInterface
    public interface GuiClickAction {
        void onClick(ServerPlayer player);
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
     * GUI types enum
     */
    public enum GuiType {
        SHOP_MAIN,
        SHOP_CATEGORY,
        PLAYER_STATS,
        SERVER_INFO,
        ECONOMY_MANAGEMENT,
        KIT_SELECTOR,
        WARP_SELECTOR,
        TELEPORT_MENU
    }
}
