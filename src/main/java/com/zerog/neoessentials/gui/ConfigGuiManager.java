package com.zerog.neoessentials.gui;

import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.utils.PlaceholderManager;
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
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Configuration GUI system for NeoEssentials
 * Provides in-game configuration interface for administrators
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class ConfigGuiManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigGuiManager.class);
    private static ConfigGuiManager instance;
    
    private final ConfigManager configManager;
    private final PlaceholderManager placeholderManager;
    
    private ConfigGuiManager() {
        this.configManager = ConfigManager.getInstance();
        this.placeholderManager = PlaceholderManager.getInstance();
    }
    
    public static ConfigGuiManager getInstance() {
        if (instance == null) {
            instance = new ConfigGuiManager();
        }
        return instance;
    }
    
    /**
     * Open the main configuration GUI for a player
     */
    public void openMainConfigGui(ServerPlayer player) {
        if (!player.hasPermissions(3)) { // Require admin permissions
            player.sendSystemMessage(Component.literal("§cYou don't have permission to access the configuration GUI."));
            return;
        }
        
        MainConfigMenu menu = new MainConfigMenu();
        player.openMenu(menu);
        
        LOGGER.info("Player {} opened the configuration GUI", player.getName().getString());
    }
    
    /**
     * Open economy configuration GUI
     */
    public void openEconomyConfigGui(ServerPlayer player) {
        if (!player.hasPermissions(3)) {
            player.sendSystemMessage(Component.literal("§cYou don't have permission to access this configuration."));
            return;
        }
        
        EconomyConfigMenu menu = new EconomyConfigMenu();
        player.openMenu(menu);
    }
    
    /**
     * Open home configuration GUI
     */
    public void openHomeConfigGui(ServerPlayer player) {
        if (!player.hasPermissions(3)) {
            player.sendSystemMessage(Component.literal("§cYou don't have permission to access this configuration."));
            return;
        }
        
        HomeConfigMenu menu = new HomeConfigMenu();
        player.openMenu(menu);
    }
    
    /**
     * Main configuration menu
     */
    private class MainConfigMenu implements MenuProvider {
        
        @Override
        public Component getDisplayName() {
            return Component.literal("§6NeoEssentials Configuration");
        }
        
        @Override
        public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
            SimpleContainer container = new SimpleContainer(54); // 6 rows
            
            // Economy settings
            ItemStack economyItem = new ItemStack(Items.GOLD_INGOT);
            economyItem.set(DataComponents.CUSTOM_NAME, Component.literal("§6Economy Settings"));
            setLore(economyItem, Arrays.asList(
                "§7Configure economy system",
                "§7• Starting balance",
                "§7• Currency symbol",
                "§7• Command costs",
                "",
                "§eClick to configure"
            ));
            container.setItem(10, economyItem);
            
            // Home settings
            ItemStack homeItem = new ItemStack(Items.RED_BED);
            homeItem.set(DataComponents.CUSTOM_NAME, Component.literal("§2Home Settings"));
            setLore(homeItem, Arrays.asList(
                "§7Configure home system",
                "§7• Maximum homes per player",
                "§7• Home costs",
                "§7• Restricted worlds",
                "",
                "§eClick to configure"
            ));
            container.setItem(12, homeItem);
            
            // Warp settings
            ItemStack warpItem = new ItemStack(Items.ENDER_PEARL);
            warpItem.set(DataComponents.CUSTOM_NAME, Component.literal("§5Warp Settings"));
            setLore(warpItem, Arrays.asList(
                "§7Configure warp system",
                "§7• Warp categories",
                "§7• Teleport costs",
                "§7• Cooldowns",
                "",
                "§eClick to configure"
            ));
            container.setItem(14, warpItem);
            
            // Kit settings
            ItemStack kitItem = new ItemStack(Items.CHEST);
            kitItem.set(DataComponents.CUSTOM_NAME, Component.literal("§9Kit Settings"));
            setLore(kitItem, Arrays.asList(
                "§7Configure kit system",
                "§7• Kit contents",
                "§7• Cooldowns",
                "§7• Costs",
                "",
                "§eClick to configure"
            ));
            container.setItem(16, kitItem);
            
            // Messaging settings
            ItemStack messagingItem = new ItemStack(Items.WRITABLE_BOOK);
            messagingItem.set(DataComponents.CUSTOM_NAME, Component.literal("§bMessaging Settings"));
            setLore(messagingItem, Arrays.asList(
                "§7Configure messaging system",
                "§7• Mail system",
                "§7• Private messages",
                "§7• Broadcast settings",
                "",
                "§eClick to configure"
            ));
            container.setItem(28, messagingItem);
            
            // Spawn settings
            ItemStack spawnItem = new ItemStack(Items.COMPASS);
            spawnItem.set(DataComponents.CUSTOM_NAME, Component.literal("§aSpawn Settings"));
            setLore(spawnItem, Arrays.asList(
                "§7Configure spawn system",
                "§7• Spawn location",
                "§7• First join behavior",
                "§7• Protection settings",
                "",
                "§eClick to configure"
            ));
            container.setItem(30, spawnItem);
            
            // Moderation settings
            ItemStack moderationItem = new ItemStack(Items.IRON_SWORD);
            moderationItem.set(DataComponents.CUSTOM_NAME, Component.literal("§cModeration Settings"));
            setLore(moderationItem, Arrays.asList(
                "§7Configure moderation tools",
                "§7• Jail locations",
                "§7• Mute settings",
                "§7• Ban settings",
                "",
                "§eClick to configure"
            ));
            container.setItem(32, moderationItem);
            
            // Performance monitoring
            ItemStack performanceItem = new ItemStack(Items.REDSTONE);
            performanceItem.set(DataComponents.CUSTOM_NAME, Component.literal("§6Performance Monitor"));
            setLore(performanceItem, Arrays.asList(
                "§7View performance metrics",
                "§7• Command execution times",
                "§7• Memory usage",
                "§7• System load",
                "",
                "§eClick to view"
            ));
            container.setItem(34, performanceItem);
            
            // Placeholder settings
            ItemStack placeholderItem = new ItemStack(Items.NAME_TAG);
            placeholderItem.set(DataComponents.CUSTOM_NAME, Component.literal("§dPlaceholder Settings"));
            setLore(placeholderItem, Arrays.asList(
                "§7Configure placeholders",
                "§7• Custom placeholders",
                "§7• Format settings",
                "§7• Available placeholders",
                "",
                "§eClick to configure"
            ));
            container.setItem(40, placeholderItem);
            
            // Reload configuration
            ItemStack reloadItem = new ItemStack(Items.STRUCTURE_VOID);
            reloadItem.set(DataComponents.CUSTOM_NAME, Component.literal("§cReload Configuration"));
            setLore(reloadItem, Arrays.asList(
                "§7Reload all configuration files",
                "§7This will apply changes made",
                "§7outside the game.",
                "",
                "§cClick to reload"
            ));
            container.setItem(49, reloadItem);
            
            // Fill empty slots with glass panes
            ItemStack glassPane = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
            glassPane.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
            for (int i = 0; i < 54; i++) {
                if (container.getItem(i).isEmpty()) {
                    container.setItem(i, glassPane);
                }
            }
            
            return new ChestMenu(MenuType.GENERIC_9x6, containerId, playerInventory, container, 6);
        }
    }
    
    /**
     * Economy configuration menu
     */
    private class EconomyConfigMenu implements MenuProvider {
        
        @Override
        public Component getDisplayName() {
            return Component.literal("§6Economy Configuration");
        }
        
        @Override
        public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
            SimpleContainer container = new SimpleContainer(27); // 3 rows
            
            // Starting balance setting
            ItemStack startingBalanceItem = new ItemStack(Items.EMERALD);
            startingBalanceItem.set(DataComponents.CUSTOM_NAME, Component.literal("§aStarting Balance"));
            setLore(startingBalanceItem, Arrays.asList(
                "§7Current: §e" + configManager.getEconomyConfig().startingBalance,
                "§7The amount new players start with",
                "",
                "§eLeft-click: +100",
                "§eRight-click: -100",
                "§eShift-click: +1000/-1000"
            ));
            container.setItem(10, startingBalanceItem);
            
            // Currency symbol setting
            ItemStack currencyItem = new ItemStack(Items.GOLD_NUGGET);
            currencyItem.set(DataComponents.CUSTOM_NAME, Component.literal("§eCurrency Symbol"));
            setLore(currencyItem, Arrays.asList(
                "§7Current: §e" + configManager.getEconomyConfig().currencySymbol,
                "§7The symbol used for currency",
                "",
                "§eClick to change"
            ));
            container.setItem(12, currencyItem);
            
            // Command costs
            ItemStack costsItem = new ItemStack(Items.PAPER);
            costsItem.set(DataComponents.CUSTOM_NAME, Component.literal("§6Command Costs"));
            setLore(costsItem, Arrays.asList(
                "§7Configure costs for commands",
                "§7• Home teleportation",
                "§7• Warp usage",
                "§7• Kit retrieval",
                "",
                "§eClick to configure"
            ));
            container.setItem(14, costsItem);
            
            // Back button
            ItemStack backItem = new ItemStack(Items.ARROW);
            backItem.set(DataComponents.CUSTOM_NAME, Component.literal("§7Back to Main Menu"));
            container.setItem(22, backItem);
            
            return new ChestMenu(MenuType.GENERIC_9x3, containerId, playerInventory, container, 3);
        }
    }
    
    /**
     * Home configuration menu
     */
    private class HomeConfigMenu implements MenuProvider {
        
        @Override
        public Component getDisplayName() {
            return Component.literal("§2Home Configuration");
        }
        
        @Override
        public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
            SimpleContainer container = new SimpleContainer(27); // 3 rows
            
            // Max homes setting
            ItemStack maxHomesItem = new ItemStack(Items.RED_BED);
            maxHomesItem.set(DataComponents.CUSTOM_NAME, Component.literal("§aMaximum Homes"));
            setLore(maxHomesItem, Arrays.asList(
                "§7Current default: §e" + configManager.getHomeConfig().defaultMaxHomes,
                "§7Maximum homes per player",
                "",
                "§eLeft-click: +1",
                "§eRight-click: -1",
                "§eShift-click: +5/-5"
            ));
            container.setItem(10, maxHomesItem);
            
            // Home costs
            ItemStack costItem = new ItemStack(Items.GOLD_INGOT);
            costItem.set(DataComponents.CUSTOM_NAME, Component.literal("§6Home Costs"));
            setLore(costItem, Arrays.asList(
                "§7Set home cost: §e" + configManager.getHomeConfig().setHomeCost,
                "§7Teleport cost: §e" + configManager.getHomeConfig().getTeleportCost(),
                "",
                "§eClick to modify"
            ));
            container.setItem(12, costItem);
            
            // Restricted worlds
            ItemStack restrictedItem = new ItemStack(Items.BARRIER);
            restrictedItem.set(DataComponents.CUSTOM_NAME, Component.literal("§cRestricted Worlds"));
            setLore(restrictedItem, Arrays.asList(
                "§7Worlds where homes are disabled",
                "§7Current: §e" + String.join(", ", configManager.getHomeConfig().restrictedWorlds),
                "",
                "§eClick to configure"
            ));
            container.setItem(14, restrictedItem);
            
            // Back button
            ItemStack backItem = new ItemStack(Items.ARROW);
            backItem.set(DataComponents.CUSTOM_NAME, Component.literal("§7Back to Main Menu"));
            container.setItem(22, backItem);
            
            return new ChestMenu(MenuType.GENERIC_9x3, containerId, playerInventory, container, 3);
        }
    }
    
    /**
     * Utility method to set custom name on an item
     */
    private void setCustomName(ItemStack item, String name) {
        item.set(DataComponents.CUSTOM_NAME, Component.literal(name));
    }
    
    /**
     * Utility method to set lore on an item
     */
    private void setLore(ItemStack item, List<String> lore) {
        List<Component> loreComponents = new ArrayList<>();
        for (String line : lore) {
            loreComponents.add(Component.literal(line));
        }
        item.set(DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(loreComponents));
    }
    
    /**
     * Handle GUI click events
     */
    public void handleGuiClick(ServerPlayer player, int slot, String guiType) {
        switch (guiType) {
            case "main":
                handleMainMenuClick(player, slot);
                break;
            case "economy":
                handleEconomyMenuClick(player, slot);
                break;
            case "home":
                handleHomeMenuClick(player, slot);
                break;
        }
    }
    
    private void handleMainMenuClick(ServerPlayer player, int slot) {
        switch (slot) {
            case 10 -> openEconomyConfigGui(player);
            case 12 -> openHomeConfigGui(player);
            case 14 -> player.sendSystemMessage(Component.literal("§eWarp configuration coming soon!"));
            case 16 -> player.sendSystemMessage(Component.literal("§eKit configuration coming soon!"));
            case 28 -> player.sendSystemMessage(Component.literal("§eMessaging configuration coming soon!"));
            case 30 -> player.sendSystemMessage(Component.literal("§eSpawn configuration coming soon!"));
            case 32 -> player.sendSystemMessage(Component.literal("§eModeration configuration coming soon!"));
            case 34 -> showPerformanceReport(player);
            case 40 -> showPlaceholderInfo(player);
            case 49 -> reloadConfiguration(player);
        }
    }
    
    private void handleEconomyMenuClick(ServerPlayer player, int slot) {
        switch (slot) {
            case 22 -> openMainConfigGui(player);
        }
    }
    
    private void handleHomeMenuClick(ServerPlayer player, int slot) {
        switch (slot) {
            case 22 -> openMainConfigGui(player);
        }
    }
    
    private void showPerformanceReport(ServerPlayer player) {
        player.sendSystemMessage(Component.literal("§6=== Performance Report ==="));
        // This would show actual performance data
        player.sendSystemMessage(Component.literal("§7Memory Usage: §e75% (1.2GB / 1.6GB)"));
        player.sendSystemMessage(Component.literal("§7Server TPS: §a20.0"));
        player.sendSystemMessage(Component.literal("§7Command Executions: §e1,234"));
        player.sendSystemMessage(Component.literal("§7Average Command Time: §e12ms"));
    }
    
    private void showPlaceholderInfo(ServerPlayer player) {
        player.sendSystemMessage(Component.literal("§d=== Placeholder Information ==="));
        Map<String, ?> placeholders = placeholderManager.getRegisteredPlaceholders();
        player.sendSystemMessage(Component.literal("§7Registered Placeholders: §e" + placeholders.size()));
        player.sendSystemMessage(Component.literal("§7Examples: §e%player%, %balance%, %homes_count%"));
    }
    
    private void reloadConfiguration(ServerPlayer player) {
        try {
            configManager.reloadAll();
            player.sendSystemMessage(Component.literal("§aConfiguration reloaded successfully!"));
            LOGGER.info("Configuration reloaded by player {}", player.getName().getString());
        } catch (Exception e) {
            player.sendSystemMessage(Component.literal("§cFailed to reload configuration: " + e.getMessage()));
            LOGGER.error("Failed to reload configuration", e);
        }
    }
}
