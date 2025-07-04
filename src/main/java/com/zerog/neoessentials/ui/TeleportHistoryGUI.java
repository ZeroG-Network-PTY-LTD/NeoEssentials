package com.zerog.neoessentials.ui;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.data.TeleportHistoryManager;
import com.zerog.neoessentials.util.LanguageUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * GUI interface for viewing and managing teleport history.
 * Provides an interactive, user-friendly way to browse and use teleport history.
 */
public class TeleportHistoryGUI {
    
    private static final int GUI_SIZE = 54; // 6 rows
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd HH:mm");
    
    /**
     * Opens the teleport history GUI for a player
     * 
     * @param player The player to open the GUI for
     */
    public static void openHistoryGUI(ServerPlayer player) {
        TeleportHistoryManager historyManager = NeoEssentials.getInstance().getDataManager().getTeleportHistoryManager();
        List<TeleportHistoryManager.TeleportLocation> history = historyManager.getPlayerHistory(player.getUUID());
        
        if (history.isEmpty()) {
            LanguageUtil.sendMessage(player, "§cNo teleport history found.");
            return;
        }
        
        SimpleContainer container = new SimpleContainer(GUI_SIZE);
        setupHistoryInterface(container, player, history, 0);
        
        player.openMenu(new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.literal("§8[§3Teleport History§8] §7" + history.size() + " locations");
            }
            
            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
                return new TeleportHistoryMenu(containerId, playerInventory, container, history);
            }
        });
    }
    
    /**
     * Sets up the history interface with teleport locations
     * 
     * @param container The container to set up
     * @param player The player viewing the history
     * @param history The teleport history
     * @param page The current page number
     */
    private static void setupHistoryInterface(SimpleContainer container, ServerPlayer player, 
                                             List<TeleportHistoryManager.TeleportLocation> history, int page) {
        // Clear container
        container.clearContent();
        
        // Calculate pagination
        int entriesPerPage = 45; // 9x5 grid for entries
        int totalPages = (history.size() + entriesPerPage - 1) / entriesPerPage;
        int startIndex = page * entriesPerPage;
        int endIndex = Math.min(startIndex + entriesPerPage, history.size());
        
        // Add history entries
        for (int i = startIndex; i < endIndex; i++) {
            TeleportHistoryManager.TeleportLocation location = history.get(i);
            ItemStack item = createHistoryItem(location, i + 1);
            container.setItem(i - startIndex, item);
        }
        
        // Add navigation items
        setupNavigationItems(container, page, totalPages, history.size());
    }
    
    /**
     * Creates an item stack representing a teleport history entry
     * 
     * @param location The teleport location
     * @param index The history index
     * @return The item stack
     */
    private static ItemStack createHistoryItem(TeleportHistoryManager.TeleportLocation location, int index) {
        // Choose item based on dimension
        ItemStack item;
        String dimensionName = getDimensionDisplayName(location.getDimension());
        
        switch (location.getDimension()) {
            case "minecraft:the_nether":
                item = new ItemStack(Items.NETHERRACK);
                break;
            case "minecraft:the_end":
                item = new ItemStack(Items.END_STONE);
                break;
            default:
                item = new ItemStack(Items.GRASS_BLOCK);
                break;
        }
        
        // Set display name
        item.setHoverName(Component.literal("§3Location #" + index));
        
        return item;
    }
    
    /**
     * Sets up navigation items at the bottom of the GUI
     * 
     * @param container The container
     * @param currentPage The current page number
     * @param totalPages The total number of pages
     * @param totalEntries The total number of history entries
     */
    private static void setupNavigationItems(SimpleContainer container, int currentPage, int totalPages, int totalEntries) {
        // Previous page button
        if (currentPage > 0) {
            ItemStack prevPage = new ItemStack(Items.ARROW);
            prevPage.setHoverName(Component.literal(TextUtil.colorize("&aPrevious Page")));
            container.setItem(45, prevPage);
        }
        
        // Info item
        ItemStack info = new ItemStack(Items.BOOK);
        info.setHoverName(Component.literal(TextUtil.colorize("&6Teleport History")));
        var lore = info.getOrCreateTagElement("display").getList("Lore", 8);
        lore.add(Component.Serializer.toJsonTree(Component.literal(
            TextUtil.colorize("&7Total Locations: &f" + totalEntries))));
        lore.add(Component.Serializer.toJsonTree(Component.literal(
            TextUtil.colorize("&7Page: &f" + (currentPage + 1) + "/" + totalPages))));
        lore.add(Component.Serializer.toJsonTree(Component.literal("")));
        lore.add(Component.Serializer.toJsonTree(Component.literal(
            TextUtil.colorize("&7Click entries to teleport"))));
        lore.add(Component.Serializer.toJsonTree(Component.literal(
            TextUtil.colorize("&7Shift+Click for coordinates"))));
        container.setItem(49, info);
        
        // Next page button
        if (currentPage < totalPages - 1) {
            ItemStack nextPage = new ItemStack(Items.ARROW);
            nextPage.setHoverName(Component.literal(TextUtil.colorize("&aNext Page")));
            container.setItem(53, nextPage);
        }
        
        // Clear history button
        ItemStack clearHistory = new ItemStack(Items.BARRIER);
        clearHistory.setHoverName(Component.literal(TextUtil.colorize("&cClear History")));
        var clearLore = clearHistory.getOrCreateTagElement("display").getList("Lore", 8);
        clearLore.add(Component.Serializer.toJsonTree(Component.literal(
            TextUtil.colorize("&7This will clear all teleport history"))));
        clearLore.add(Component.Serializer.toJsonTree(Component.literal(
            TextUtil.colorize("&cWarning: This cannot be undone!"))));
        container.setItem(46, clearHistory);
        
        // Close button
        ItemStack close = new ItemStack(Items.RED_STAINED_GLASS_PANE);
        close.setHoverName(Component.literal(TextUtil.colorize("&cClose")));
        container.setItem(52, close);
    }
    
    /**
     * Gets a user-friendly display name for a dimension
     * 
     * @param dimension The dimension resource location
     * @return The display name
     */
    private static String getDimensionDisplayName(String dimension) {
        switch (dimension) {
            case "minecraft:overworld":
                return "Overworld";
            case "minecraft:the_nether":
                return "The Nether";
            case "minecraft:the_end":
                return "The End";
            default:
                // Handle modded dimensions
                String[] parts = dimension.split(":");
                if (parts.length == 2) {
                    return parts[1].replace("_", " ");
                }
                return dimension;
        }
    }
    
    /**
     * Menu handler for the teleport history GUI
     */
    public static class TeleportHistoryMenu extends ChestMenu {
        private final List<TeleportHistoryManager.TeleportLocation> history;
        
        public TeleportHistoryMenu(int containerId, Inventory playerInventory, SimpleContainer container,
                                  List<TeleportHistoryManager.TeleportLocation> history) {
            super(MenuType.GENERIC_9x6, containerId, playerInventory, container, 6);
            this.history = history;
        }
        
        @Override
        public boolean clickMenuButton(Player player, int slotId) {
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return false;
            }
            
            TeleportHistoryManager historyManager = NeoEssentials.getInstance().getDataManager().getTeleportHistoryManager();
            
            // Handle navigation buttons
            if (slotId == 45) { // Previous page
                // TODO: Implement pagination
                return true;
            } else if (slotId == 53) { // Next page
                // TODO: Implement pagination
                return true;
            } else if (slotId == 46) { // Clear history
                historyManager.clearPlayerHistory(serverPlayer.getUUID());
                serverPlayer.closeContainer();
                LanguageUtil.sendSuccessMessage(serverPlayer, "Teleport history cleared.");
                return true;
            } else if (slotId == 52) { // Close
                serverPlayer.closeContainer();
                return true;
            } else if (slotId >= 0 && slotId < 45 && slotId < history.size()) {
                // Teleport to selected location
                TeleportHistoryManager.TeleportLocation location = history.get(slotId);
                
                // Find the target level
                var targetLevel = serverPlayer.getServer().getAllLevels().stream()
                    .filter(level -> level.dimension().location().toString().equals(location.getDimension()))
                    .findFirst()
                    .orElse(null);
                
                if (targetLevel != null) {
                    // Record current position before teleporting
                    historyManager.recordPosition(serverPlayer);
                    
                    // Teleport player
                    serverPlayer.teleportTo(targetLevel, location.getX(), location.getY(), location.getZ(), 
                                          location.getYaw(), location.getPitch());
                    
                    serverPlayer.closeContainer();
                    LanguageUtil.sendSuccessMessage(serverPlayer, "Teleported to history location!");
                } else {
                    LanguageUtil.sendErrorMessage(serverPlayer, "Could not find target dimension.");
                }
                return true;
            }
            
            return super.clickMenuButton(player, slotId);
        }
    }
}
