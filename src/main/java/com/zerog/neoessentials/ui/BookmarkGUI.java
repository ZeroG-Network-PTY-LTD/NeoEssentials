package com.zerog.neoessentials.ui;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.data.BookmarkManager;
import com.zerog.neoessentials.util.LanguageUtil;
import com.zerog.neoessentials.utils.TeleportUtil;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * GUI system for bookmark management with interactive text components.
 * Provides clickable interfaces for bookmark operations.
 */
public class BookmarkGUI {
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM dd, yyyy HH:mm");
    
    /**
     * Shows the main bookmark menu with interactive options.
     * 
     * @param player The player to show the menu to
     */
    public static void showBookmarkMenu(ServerPlayer player) {
        BookmarkManager bookmarkManager = NeoEssentials.getInstance().getDataManager().getBookmarkManager();
        Map<String, BookmarkManager.BookmarkData> bookmarks = bookmarkManager.getPlayerBookmarks(player.getUUID());
        
        // Header
        LanguageUtil.sendMessage(player, "§6§l=== Bookmark Manager ===");
        LanguageUtil.sendMessage(player, "§7Click on options to interact");
        LanguageUtil.sendMessage(player, "");
        
        // Add bookmark option
        MutableComponent addBookmark = Component.literal("§a[+ Add Bookmark]")
            .withStyle(style -> style
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tpbookmark add "))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                    Component.literal("§7Click to add a bookmark at your current location")))
            );
        player.sendSystemMessage(addBookmark);
        
        if (bookmarks.isEmpty()) {
            LanguageUtil.sendMessage(player, "§7No bookmarks found. Add one using the button above!");
            return;
        }
        
        LanguageUtil.sendMessage(player, "");
        LanguageUtil.sendMessage(player, "§e§lYour Bookmarks:");
        
        // List bookmarks with interactive options
        bookmarks.entrySet().stream()
            .sorted(Map.Entry.<String, BookmarkManager.BookmarkData>comparingByValue(
                (b1, b2) -> Long.compare(b2.timestamp, b1.timestamp)
            ))
            .forEach(entry -> {
                String name = entry.getKey();
                BookmarkManager.BookmarkData bookmark = entry.getValue();
                
                showBookmarkEntry(player, name, bookmark);
            });
        
        // Footer with stats
        LanguageUtil.sendMessage(player, "");
        LanguageUtil.sendMessage(player, "§7Total bookmarks: §e" + bookmarks.size() + 
            "§7/§e" + bookmarkManager.getMaxBookmarks());
    }
    
    /**
     * Shows a single bookmark entry with interactive buttons.
     * 
     * @param player The player viewing the bookmark
     * @param name The bookmark name
     * @param bookmark The bookmark data
     */
    private static void showBookmarkEntry(ServerPlayer player, String name, BookmarkManager.BookmarkData bookmark) {
        String coords = String.format("%.0f, %.0f, %.0f", bookmark.x, bookmark.y, bookmark.z);
        String dimensionName = bookmark.dimension.substring(bookmark.dimension.lastIndexOf(':') + 1);
        String dateCreated = DATE_FORMAT.format(new Date(bookmark.timestamp));
        
        // Bookmark name and coordinates
        LanguageUtil.sendMessage(player, "§6▶ §e" + name + " §7(" + dimensionName + ")");
        LanguageUtil.sendMessage(player, "  §7Location: §f" + coords);
        LanguageUtil.sendMessage(player, "  §7Created: §f" + dateCreated);
        
        // Interactive buttons
        MutableComponent teleportButton = Component.literal("§a[Teleport]")
            .withStyle(style -> style
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpbookmark tp " + name))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    Component.literal("§7Click to teleport to this bookmark")))
            );
        
        MutableComponent removeButton = Component.literal("§c[Remove]")
            .withStyle(style -> style
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tpbookmark remove " + name))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    Component.literal("§7Click to remove this bookmark")))
            );
        
        MutableComponent infoButton = Component.literal("§b[Info]")
            .withStyle(style -> style
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bookmarkinfo " + name))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    Component.literal("§7Click for detailed information")))
            );
        
        // Combine buttons in one line
        MutableComponent buttonRow = Component.literal("  ")
            .append(teleportButton)
            .append(Component.literal(" "))
            .append(removeButton)
            .append(Component.literal(" "))
            .append(infoButton);
        
        player.sendSystemMessage(buttonRow);
        LanguageUtil.sendMessage(player, "");
    }
    
    /**
     * Shows detailed information about a specific bookmark.
     * 
     * @param player The player requesting the information
     * @param bookmarkName The name of the bookmark
     */
    public static void showBookmarkInfo(ServerPlayer player, String bookmarkName) {
        BookmarkManager bookmarkManager = NeoEssentials.getInstance().getDataManager().getBookmarkManager();
        BookmarkManager.BookmarkData bookmark = bookmarkManager.getBookmark(player.getUUID(), bookmarkName);
        
        if (bookmark == null) {
            LanguageUtil.sendErrorMessage(player, "commands.tpbookmark.not_found", bookmarkName);
            return;
        }
        
        LanguageUtil.sendMessage(player, "§6§l=== Bookmark Information ===");
        LanguageUtil.sendMessage(player, "§e§lName: §f" + bookmarkName);
        LanguageUtil.sendMessage(player, "§e§lDimension: §f" + bookmark.dimension);
        LanguageUtil.sendMessage(player, "§e§lCoordinates:");
        LanguageUtil.sendMessage(player, "  §7X: §f" + String.format("%.3f", bookmark.x));
        LanguageUtil.sendMessage(player, "  §7Y: §f" + String.format("%.3f", bookmark.y));
        LanguageUtil.sendMessage(player, "  §7Z: §f" + String.format("%.3f", bookmark.z));
        LanguageUtil.sendMessage(player, "§e§lRotation:");
        LanguageUtil.sendMessage(player, "  §7Yaw: §f" + String.format("%.1f°", bookmark.yaw));
        LanguageUtil.sendMessage(player, "  §7Pitch: §f" + String.format("%.1f°", bookmark.pitch));
        LanguageUtil.sendMessage(player, "§e§lCreated: §f" + DATE_FORMAT.format(new Date(bookmark.timestamp)));
        
        if (!bookmark.description.isEmpty()) {
            LanguageUtil.sendMessage(player, "§e§lDescription: §f" + bookmark.description);
        }
        
        LanguageUtil.sendMessage(player, "");
        
        // Quick teleport button
        MutableComponent teleportButton = Component.literal("§a§l[Teleport Now]")
            .withStyle(style -> style
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpbookmark tp " + bookmarkName))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    Component.literal("§7Click to teleport to this bookmark")))
            );
        
        player.sendSystemMessage(teleportButton);
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
        
        LanguageUtil.sendMessage(player, "§c§l=== Remove Bookmark ===");
        LanguageUtil.sendMessage(player, "§7Are you sure you want to remove the bookmark:");
        LanguageUtil.sendMessage(player, "§e" + bookmarkName + " §7(" + 
            String.format("%.0f, %.0f, %.0f", bookmark.x, bookmark.y, bookmark.z) + ")");
        LanguageUtil.sendMessage(player, "");
        
        // Confirmation buttons
        MutableComponent confirmButton = Component.literal("§c§l[YES, REMOVE]")
            .withStyle(style -> style
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpbookmark remove " + bookmarkName))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    Component.literal("§cClick to permanently remove this bookmark")))
            );
        
        MutableComponent cancelButton = Component.literal("§a§l[CANCEL]")
            .withStyle(style -> style
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bookmarks"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    Component.literal("§7Click to cancel and return to bookmark list")))
            );
        
        MutableComponent buttonRow = Component.literal("")
            .append(confirmButton)
            .append(Component.literal("  "))
            .append(cancelButton);
        
        player.sendSystemMessage(buttonRow);
    }
}
