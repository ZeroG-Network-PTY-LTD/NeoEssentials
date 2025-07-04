package com.zerog.neoessentials.ui;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.data.BookmarkManager;
import com.zerog.neoessentials.util.LanguageUtil;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Enhanced bookmark management GUI using the MenuSystem for interactive bookmark operations.
 * Provides a modern, user-friendly interface for managing teleport bookmarks.
 */
public class BookmarkGUI {
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM dd, HH:mm");
    private static final int ITEMS_PER_PAGE = 6;
    
    /**
     * Shows the main bookmark management menu.
     * 
     * @param player The player to show the menu to
     * @param page The page number to show (1-based)
     */
    public static void showBookmarkMenu(ServerPlayer player, int page) {
        BookmarkManager bookmarkManager = NeoEssentials.getInstance().getDataManager().getBookmarkManager();
        Map<String, BookmarkManager.BookmarkData> bookmarks = bookmarkManager.getPlayerBookmarks(player.getUUID());
        
        if (bookmarks.isEmpty()) {
            showEmptyBookmarkMenu(player);
            return;
        }
        
        // Convert bookmarks to menu items
        List<MenuSystem.MenuItem> items = createBookmarkMenuItems(bookmarks, player);
        
        // Add management options at the top
        items.add(0, new MenuSystem.MenuItem(
            "&a➕ Add New Bookmark",
            "/tpbookmark add ",
            "&7Add a bookmark at your current location\n&eClick to enter bookmark name"
        ));
        
        // Show the menu with pagination
        showCustomBookmarkMenu(player, page, items, bookmarks.size(), bookmarkManager.getMaxBookmarks());
    }
    
    /**
     * Shows the bookmark menu when no bookmarks exist.
     * 
     * @param player The player to show the empty menu to
     */
    private static void showEmptyBookmarkMenu(ServerPlayer player) {
        BookmarkManager bookmarkManager = NeoEssentials.getInstance().getDataManager().getBookmarkManager();
        
        List<MenuSystem.MenuItem> items = new ArrayList<>();
        
        items.add(new MenuSystem.MenuItem(
            "&a➕ Add Your First Bookmark",
            "/tpbookmark add ",
            "&7Add a bookmark at your current location\n&eClick to enter bookmark name"
        ));
        
        items.add(new MenuSystem.MenuItem(
            "&e📖 Bookmark Help",
            "/help bookmarks",
            "&7Learn how to use the bookmark system"
        ));
        
        // Show simple menu for empty state
        showSimpleBookmarkMenu(player, items, 0, bookmarkManager.getMaxBookmarks());
    }
    
    /**
     * Creates menu items from bookmark data.
     * 
     * @param bookmarks The bookmark map
     * @param player The player viewing the bookmarks
     * @return List of menu items
     */
    private static List<MenuSystem.MenuItem> createBookmarkMenuItems(
            Map<String, BookmarkManager.BookmarkData> bookmarks, ServerPlayer player) {
        
        List<MenuSystem.MenuItem> items = new ArrayList<>();
        
        // Sort bookmarks by creation date (newest first)
        List<Map.Entry<String, BookmarkManager.BookmarkData>> sortedBookmarks = 
            bookmarks.entrySet().stream()
                .sorted(Map.Entry.<String, BookmarkManager.BookmarkData>comparingByValue(
                    (b1, b2) -> Long.compare(b2.timestamp, b1.timestamp)
                ))
                .toList();
        
        for (Map.Entry<String, BookmarkManager.BookmarkData> entry : sortedBookmarks) {
            String name = entry.getKey();
            BookmarkManager.BookmarkData bookmark = entry.getValue();
            
            // Format the display text
            String displayText = formatBookmarkEntry(name, bookmark);
            
            // Command to teleport to this bookmark
            String command = "/tpbookmark tp " + name;
            
            // Hover text with more details
            String hoverText = formatBookmarkHoverText(name, bookmark);
            
            items.add(new MenuSystem.MenuItem(displayText, command, hoverText));
        }
        
        return items;
    }
    
    /**
     * Formats a bookmark entry for display in the menu.
     * 
     * @param name The bookmark name
     * @param bookmark The bookmark data
     * @return Formatted display text
     */
    private static String formatBookmarkEntry(String name, BookmarkManager.BookmarkData bookmark) {
        // Format the timestamp
        String timeStr = DATE_FORMAT.format(new Date(bookmark.timestamp));
        
        // Extract dimension name from full dimension path
        String dimensionName = getDimensionDisplayName(bookmark.dimension);
        
        // Create the clickable entry
        return String.format("&e🔖 %s &7%s &8[&b%s&8]", name, timeStr, dimensionName);
    }
    
    /**
     * Formats detailed hover text for a bookmark entry.
     * 
     * @param name The bookmark name
     * @param bookmark The bookmark data
     * @return Formatted hover text
     */
    private static String formatBookmarkHoverText(String name, BookmarkManager.BookmarkData bookmark) {
        String timeStr = DATE_FORMAT.format(new Date(bookmark.timestamp));
        String dimensionName = getDimensionDisplayName(bookmark.dimension);
        
        return String.format(
            "&6📍 %s&r\n" +
            "&7Created: &f%s\n" +
            "&7Dimension: &f%s\n" +
            "&7Coordinates: &f%.1f, %.1f, %.1f\n" +
            "&7Rotation: &f%.1f°, %.1f°\n" +
            "\n" +
            "&a🚀 Click to teleport!\n" +
            "&7Right-click for options",
            name, timeStr, dimensionName,
            bookmark.x, bookmark.y, bookmark.z,
            bookmark.yaw, bookmark.pitch
        );
    }
    
    /**
     * Gets a user-friendly dimension display name.
     * 
     * @param dimension The full dimension identifier
     * @return A short, user-friendly dimension name
     */
    private static String getDimensionDisplayName(String dimension) {
        if (dimension == null) return "Unknown";
        
        // Handle common dimensions
        if (dimension.equals("minecraft:overworld")) return "Overworld";
        if (dimension.equals("minecraft:the_nether")) return "Nether";
        if (dimension.equals("minecraft:the_end")) return "End";
        
        // For modded dimensions, extract just the name part
        int colonIndex = dimension.lastIndexOf(':');
        if (colonIndex != -1 && colonIndex < dimension.length() - 1) {
            String name = dimension.substring(colonIndex + 1);
            // Capitalize first letter and replace underscores
            return name.substring(0, 1).toUpperCase() + 
                   name.substring(1).replace("_", " ");
        }
        
        return dimension;
    }
    
    /**
     * Shows detailed information about a specific bookmark.
     * 
     * @param player The player to show the details to
     * @param bookmarkName The name of the bookmark to show
     */
    public static void showBookmarkDetails(ServerPlayer player, String bookmarkName) {
        BookmarkManager bookmarkManager = NeoEssentials.getInstance().getDataManager().getBookmarkManager();
        BookmarkManager.BookmarkData bookmark = bookmarkManager.getBookmark(player.getUUID(), bookmarkName);
        
        if (bookmark == null) {
            LanguageUtil.sendErrorMessage(player, "commands.tpbookmark.not_found", bookmarkName);
            return;
        }
        
        // Create menu items for bookmark actions
        List<MenuSystem.MenuItem> items = new ArrayList<>();
        
        // Teleport action
        items.add(new MenuSystem.MenuItem(
            "&a🚀 Teleport to Bookmark",
            "/tpbookmark tp " + bookmarkName,
            "&7Click to teleport to this bookmark"
        ));
        
        // Edit bookmark
        items.add(new MenuSystem.MenuItem(
            "&e✏️ Edit Bookmark",
            "/tpbookmark edit " + bookmarkName,
            "&7Modify this bookmark's properties"
        ));
        
        // Remove bookmark
        items.add(new MenuSystem.MenuItem(
            "&c�️ Remove Bookmark",
            "/tpbookmark remove " + bookmarkName,
            "&cPermanently delete this bookmark"
        ));
        
        // Back to list
        items.add(new MenuSystem.MenuItem(
            "&7↩ Back to List",
            "/tpbookmark gui",
            "&7Return to bookmark list"
        ));
        
        // Format bookmark info
        String dimensionName = getDimensionDisplayName(bookmark.dimension);
        String timeStr = DATE_FORMAT.format(new Date(bookmark.timestamp));
        
        String bookmarkInfo = String.format(
            "&7Dimension: &f%s\n" +
            "&7Coordinates: &f%.1f, %.1f, %.1f\n" +
            "&7Created: &f%s",
            dimensionName, bookmark.x, bookmark.y, bookmark.z, timeStr
        );
        
        showSimpleMenu(
            player,
            "Bookmark: " + bookmarkName,
            items,
            bookmarkInfo,
            "&7Choose an action"
        );
    }
    
    /**
     * Shows a confirmation dialog for bookmark removal.
     * 
     * @param player The player requesting the removal
     * @param bookmarkName The name of the bookmark to remove
     */
    public static void showRemoveConfirmation(ServerPlayer player, String bookmarkName) {
        BookmarkManager bookmarkManager = NeoEssentials.getInstance().getDataManager().getBookmarkManager();
        BookmarkManager.BookmarkData bookmark = bookmarkManager.getBookmark(player.getUUID(), bookmarkName);
        
        if (bookmark == null) {
            LanguageUtil.sendErrorMessage(player, "commands.tpbookmark.not_found", bookmarkName);
            return;
        }
        
        List<MenuSystem.MenuItem> items = new ArrayList<>();
        
        // Confirm removal
        items.add(new MenuSystem.MenuItem(
            "&c✓ Yes, Remove It",
            "/tpbookmark confirm-remove " + bookmarkName,
            "&cPermanently delete this bookmark"
        ));
        
        // Cancel
        items.add(new MenuSystem.MenuItem(
            "&a✗ No, Keep It", 
            "/tpbookmark gui",
            "&7Cancel and return to bookmark list"
        ));
        
        String warningText = String.format(
            "&cAre you sure you want to remove:\n" +
            "&e%s &7(%.0f, %.0f, %.0f)\n" +
            "&7This action cannot be undone!",
            bookmarkName, bookmark.x, bookmark.y, bookmark.z
        );
        
        showSimpleMenu(
            player,
            "Remove Bookmark",
            items,
            warningText,
            "&cChoose carefully"
        );
    }
    
    /**
     * Shows a custom bookmark menu with pagination.
     * 
     * @param player The player to show the menu to
     * @param page The page number to show
     * @param items The menu items to display
     * @param totalBookmarks The total number of bookmarks
     * @param maxBookmarks The maximum allowed bookmarks
     */
    private static void showCustomBookmarkMenu(ServerPlayer player, int page, List<MenuSystem.MenuItem> items, 
                                             int totalBookmarks, int maxBookmarks) {
        // Calculate total pages
        int totalPages = (int) Math.ceil((double) items.size() / ITEMS_PER_PAGE);
        
        // Ensure page is in valid range
        page = Math.max(1, Math.min(page, totalPages));
        
        // Send header
        String headerText = "&6====== &lBookmark Manager&r &6======";
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(LanguageUtil.formatText(headerText)));
        
        // Show page numbers and bookmark count
        if (totalPages > 1) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(LanguageUtil.formatText(
                    "&7Page &e" + page + "&7/&e" + totalPages)));
        }
        
        String countText = String.format("&7Bookmarks: &e%d&7/&e%d", totalBookmarks, maxBookmarks);
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(LanguageUtil.formatText(countText)));
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(""));
        
        // Send items for this page
        int startIndex = (page - 1) * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, items.size());
        
        if (items.isEmpty()) {
            // No items to display
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(LanguageUtil.formatText("&7No bookmarks to display.")));
        } else {
            // Display items for this page
            for (int i = startIndex; i < endIndex; i++) {
                MenuSystem.MenuItem item = items.get(i);
                player.sendSystemMessage(item.getFormattedComponent());
            }
        }
        
        // Add navigation if needed
        if (totalPages > 1) {
            net.minecraft.network.chat.MutableComponent navigation = net.minecraft.network.chat.Component.literal("");
            
            // Previous page button
            if (page > 1) {
                MenuSystem.MenuItem prevPageItem = new MenuSystem.MenuItem("&8[&aPrevious Page&8] ", 
                        "/tpbookmark gui page " + (page - 1), "&7Click to go to the previous page");
                navigation.append(prevPageItem.getFormattedComponent()).append(" ");
            }
            
            // Next page button
            if (page < totalPages) {
                MenuSystem.MenuItem nextPageItem = new MenuSystem.MenuItem("&8[&aNext Page&8]", 
                        "/tpbookmark gui page " + (page + 1), "&7Click to go to the next page");
                navigation.append(nextPageItem.getFormattedComponent());
            }
            
            player.sendSystemMessage(navigation);
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(""));
        }
        
        // Send footer
        String footerText = "&6===================================";
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(LanguageUtil.formatText(footerText)));
    }
    
    /**
     * Shows a simple bookmark menu without pagination.
     * 
     * @param player The player to show the menu to
     * @param items The menu items to display
     * @param totalBookmarks The total number of bookmarks
     * @param maxBookmarks The maximum allowed bookmarks
     */
    private static void showSimpleBookmarkMenu(ServerPlayer player, List<MenuSystem.MenuItem> items,
                                              int totalBookmarks, int maxBookmarks) {
        // Send header
        String headerText = "&6====== &lBookmark Manager&r &6======";
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(LanguageUtil.formatText(headerText)));
        
        // Show status message
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(LanguageUtil.formatText("&7No bookmarks found. Get started by adding one!")));
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(""));
        
        // Display items
        for (MenuSystem.MenuItem item : items) {
            player.sendSystemMessage(item.getFormattedComponent());
        }
        
        // Footer with limit info
        String footerText = String.format("&7Bookmark limit: &e%d&7/&e%d", totalBookmarks, maxBookmarks);
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(""));
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(LanguageUtil.formatText(footerText)));
        
        String footerLine = "&6===================================";
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(LanguageUtil.formatText(footerLine)));
    }
    
    /**
     * Shows a simple menu with title and status.
     * 
     * @param player The player to show the menu to
     * @param title The menu title
     * @param items The menu items to display
     * @param statusText The status text to show
     * @param footerText The footer text to show
     */
    private static void showSimpleMenu(ServerPlayer player, String title, List<MenuSystem.MenuItem> items,
                                      String statusText, String footerText) {
        // Send header
        String headerText = "&6====== &l" + title + "&r &6======";
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(LanguageUtil.formatText(headerText)));
        
        // Show status if provided
        if (statusText != null && !statusText.isEmpty()) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(LanguageUtil.formatText(statusText)));
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(""));
        }
        
        // Display items
        for (MenuSystem.MenuItem item : items) {
            player.sendSystemMessage(item.getFormattedComponent());
        }
        
        // Footer if provided
        if (footerText != null && !footerText.isEmpty()) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(""));
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(LanguageUtil.formatText(footerText)));
        }
        
        String footerLine = "&6===================================";
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(LanguageUtil.formatText(footerLine)));
    }
}
