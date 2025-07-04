package com.zerog.neoessentials.ui;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.data.TeleportHistoryManager;
import com.zerog.neoessentials.util.LanguageUtil;
import net.minecraft.server.level.ServerPlayer;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Interactive teleport history GUI using the MenuSystem for chat-based interaction.
 * Provides a paginated, clickable interface for browsing and teleporting to previous locations.
 */
public class TeleportHistoryGUI {
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM dd, HH:mm:ss");
    private static final int ITEMS_PER_PAGE = 8;
    
    /**
     * Shows the teleport history GUI to a player.
     * 
     * @param player The player to show the GUI to
     * @param page The page number to show (1-based)
     */
    public static void show(ServerPlayer player, int page) {
        UUID playerUuid = player.getUUID();
        
        // Get the teleport history manager
        TeleportHistoryManager historyManager = NeoEssentials.getInstance()
            .getDataManager()
            .getTeleportHistoryManager();
        
        // Get the player's history
        var history = historyManager.getPlayerHistory(playerUuid);
        
        if (history == null || history.isEmpty()) {
            LanguageUtil.sendMessage(player, "commands.tphistory.no_history");
            return;
        }
        
        // Convert history to menu items
        List<MenuSystem.MenuItem> items = createHistoryMenuItems(history, player);
        
        // Create and show the menu
        MenuSystem.builder()
            .title("Teleport History")
            .items(items)
            .itemsPerPage(ITEMS_PER_PAGE)
            .showPageNumbers(true)
            .back("/tphistory", "&8[&7Back to Commands&8]", "&7Click to return to teleport commands")
            .show(player, page);
    }
    
    /**
     * Creates menu items from teleport history entries.
     * 
     * @param history The teleport history list
     * @param player The player viewing the history
     * @return List of menu items
     */
    private static List<MenuSystem.MenuItem> createHistoryMenuItems(
            List<TeleportHistoryManager.TeleportLocation> history,
            ServerPlayer player) {
        
        List<MenuSystem.MenuItem> items = new ArrayList<>();
        
        // Use the history list directly
        List<TeleportHistoryManager.TeleportLocation> historyList = history;
        
        for (int i = 0; i < historyList.size(); i++) {
            TeleportHistoryManager.TeleportLocation location = historyList.get(i);
            
            // Format the display text
            String displayText = formatHistoryEntry(location, i + 1);
            
            // Command to teleport to this location
            String command = "/tphistory teleport " + i;
            
            // Hover text with more details
            String hoverText = formatHoverText(location, i + 1);
            
            items.add(new MenuSystem.MenuItem(displayText, command, hoverText));
        }
        
        return items;
    }
    
    /**
     * Formats a history entry for display in the menu.
     * 
     * @param location The teleport location
     * @param index The 1-based index of the entry
     * @return Formatted display text
     */
    private static String formatHistoryEntry(TeleportHistoryManager.TeleportLocation location, int index) {
        // Format the timestamp
        String timeStr = DATE_FORMAT.format(new Date(location.getTimestamp()));
        
        // Format coordinates
        String coords = String.format("%.1f, %.1f, %.1f", 
            location.getX(), location.getY(), location.getZ());
        
        // Extract dimension name from full dimension path
        String dimensionName = getDimensionDisplayName(location.getDimension());
        
        // Create the clickable entry
        return String.format("&a%d. &7%s &8[&b%s&8] &7%s", 
            index, timeStr, dimensionName, coords);
    }
    
    /**
     * Formats detailed hover text for a history entry.
     * 
     * @param location The teleport location
     * @param index The 1-based index of the entry
     * @return Formatted hover text
     */
    private static String formatHoverText(TeleportHistoryManager.TeleportLocation location, int index) {
        String timeStr = DATE_FORMAT.format(new Date(location.getTimestamp()));
        String dimensionName = getDimensionDisplayName(location.getDimension());
        
        return String.format(
            "&6Entry #%d&r\n" +
            "&7Time: &f%s\n" +
            "&7Dimension: &f%s\n" +
            "&7Coordinates: &f%.2f, %.2f, %.2f\n" +
            "&7Rotation: &f%.1f°, %.1f°\n" +
            "\n" +
            "&aClick to teleport here!",
            index, timeStr, dimensionName,
            location.getX(), location.getY(), location.getZ(),
            location.getYaw(), location.getPitch()
        );
    }
    
    /**
     * Converts a full dimension resource location to a user-friendly display name.
     * 
     * @param dimension The full dimension resource location
     * @return User-friendly dimension name
     */
    private static String getDimensionDisplayName(String dimension) {
        if (dimension == null) return "Unknown";
        
        // Extract the dimension name from resource location
        if (dimension.contains(":")) {
            String[] parts = dimension.split(":");
            if (parts.length >= 2) {
                String dimName = parts[1];
                
                // Convert common dimension names to friendly names
                switch (dimName) {
                    case "overworld":
                        return "Overworld";
                    case "the_nether":
                        return "Nether";
                    case "the_end":
                        return "End";
                    default:
                        // Capitalize first letter and replace underscores
                        return dimName.substring(0, 1).toUpperCase() + 
                               dimName.substring(1).replace("_", " ");
                }
            }
        }
        
        return dimension;
    }
    
    /**
     * Shows a specific page of the teleport history GUI.
     * This method can be called from commands to navigate pages.
     * 
     * @param player The player to show the GUI to
     * @param page The page number (1-based)
     */
    public static void showPage(ServerPlayer player, int page) {
        show(player, page);
    }
    
    /**
     * Shows the first page of the teleport history GUI.
     * 
     * @param player The player to show the GUI to
     */
    public static void showFirstPage(ServerPlayer player) {
        show(player, 1);
    }
}
