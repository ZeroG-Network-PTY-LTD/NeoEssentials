package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.data.BookmarkManager;
import com.zerog.neoessentials.ui.TeleportHistoryGUI;
import com.zerog.neoessentials.util.LanguageUtil;
import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.utils.TeleportUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

/**
 * Enhanced teleportation commands with bookmarks and history management.
 * Provides advanced teleportation features beyond basic teleport commands.
 */
public class TeleportBookmarkCommands {

    /**
     * Registers all enhanced teleportation commands.
     *
     * @param dispatcher The command dispatcher to register commands with
     */
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /tphistory - Show teleport history with subcommands
        dispatcher.register(
            Commands.literal("tphistory")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.tphistory"))
                .executes(this::showTeleportHistory)
                .then(
                    // /tphistory gui - Show GUI version
                    Commands.literal("gui")
                        .executes(this::showTeleportHistoryGUI)
                )
                .then(
                    // /tphistory teleport <index> - Teleport to specific history entry
                    Commands.literal("teleport")
                        .then(
                            Commands.argument("index", IntegerArgumentType.integer(0))
                                .executes(this::teleportToHistoryEntry)
                        )
                )
                .then(
                    // /tphistory clear [confirm] - Clear history
                    Commands.literal("clear")
                        .executes(this::clearHistoryPrompt)
                        .then(
                            Commands.literal("confirm")
                                .executes(this::clearHistoryConfirm)
                        )
                )
                .then(
                    // /tphistory export - Export history to text
                    Commands.literal("export")
                        .executes(this::exportHistory)
                )
        );
        
        // /tpbookmark commands
        dispatcher.register(
            Commands.literal("tpbookmark")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.tpbookmark"))
                .then(
                    // /tpbookmark add <name> - Add current location as bookmark
                    Commands.literal("add")
                        .then(
                            Commands.argument("name", StringArgumentType.string())
                                .executes(this::addBookmark)
                        )
                )
                .then(
                    // /tpbookmark remove <name> - Remove bookmark
                    Commands.literal("remove")
                        .then(
                            Commands.argument("name", StringArgumentType.string())
                                .executes(this::removeBookmark)
                        )
                )
                .then(
                    // /tpbookmark list - List all bookmarks
                    Commands.literal("list")
                        .executes(this::listBookmarks)
                )
                .then(
                    // /tpbookmark tp <name> - Teleport to bookmark
                    Commands.literal("tp")
                        .then(
                            Commands.argument("name", StringArgumentType.string())
                                .executes(this::teleportToBookmark)
                        )
                )
        );
    }

    /**
     * Shows the player's teleport history.
     */
    private int showTeleportHistory(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            
            var historyManager = NeoEssentials.getInstance().getDataManager().getTeleportHistoryManager();
            var history = historyManager.getPlayerHistory(player.getUUID());
            
            if (history.isEmpty()) {
                LanguageUtil.sendMessage(player, "commands.tphistory.no_history");
                return 0;
            }
            
            LanguageUtil.sendMessage(player, "commands.tphistory.header");
            
            // Show up to 10 most recent locations
            int count = 0;
            for (var location : history) {
                if (count >= 10) break;
                count++;
                
                // Format the timestamp
                long timeDiff = System.currentTimeMillis() - location.getTimestamp();
                String timeAgo = formatTimeAgo(timeDiff);
                
                // Format coordinates
                String coords = String.format("%.1f, %.1f, %.1f", 
                    location.getX(), location.getY(), location.getZ());
                
                // Get dimension name (extract just the dimension name from the full path)
                String dimName = location.getDimension();
                if (dimName.contains(":")) {
                    dimName = dimName.substring(dimName.lastIndexOf(':') + 1);
                }
                
                LanguageUtil.sendMessage(player, "commands.tphistory.entry", 
                    String.valueOf(count), coords, dimName, timeAgo);
            }
            
            LanguageUtil.sendMessage(player, "commands.tphistory.footer");
            
            return 1;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error showing teleport history: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Adds a bookmark at the player's current location.
     */
    private int addBookmark(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            String bookmarkName = StringArgumentType.getString(context, "name");
            
            // Validate bookmark name
            if (bookmarkName.length() > 32) {
                LanguageUtil.sendErrorMessage(player, "commands.tpbookmark.name_too_long");
                return 0;
            }
            
            if (!bookmarkName.matches("^[a-zA-Z0-9_-]+$")) {
                LanguageUtil.sendErrorMessage(player, "commands.tpbookmark.invalid_name");
                return 0;
            }
            
            BookmarkManager bookmarkManager = NeoEssentials.getInstance().getDataManager().getBookmarkManager();
            
            // Create the bookmark
            BookmarkManager.BookmarkData bookmark = new BookmarkManager.BookmarkData(
                player.serverLevel().dimension().location().toString(),
                player.getX(),
                player.getY(),
                player.getZ(),
                player.getYRot(),
                player.getXRot(),
                System.currentTimeMillis()
            );
            
            boolean success = bookmarkManager.addBookmark(player, bookmarkName, bookmark);
            
            if (success) {
                LanguageUtil.sendMessage(player, "commands.tpbookmark.added", bookmarkName);
            } else {
                LanguageUtil.sendErrorMessage(player, "commands.tpbookmark.too_many", 
                    String.valueOf(bookmarkManager.getMaxBookmarks()));
            }
            
            return success ? 1 : 0;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error adding bookmark: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Removes a bookmark.
     */
    private int removeBookmark(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            String bookmarkName = StringArgumentType.getString(context, "name");
            
            BookmarkManager bookmarkManager = NeoEssentials.getInstance().getDataManager().getBookmarkManager();
            boolean success = bookmarkManager.removeBookmark(player.getUUID(), bookmarkName);
            
            if (success) {
                LanguageUtil.sendMessage(player, "commands.tpbookmark.removed", bookmarkName);
            } else {
                LanguageUtil.sendErrorMessage(player, "commands.tpbookmark.not_found", bookmarkName);
            }
            
            return success ? 1 : 0;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error removing bookmark: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Lists all bookmarks for the player.
     */
    private int listBookmarks(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            
            BookmarkManager bookmarkManager = NeoEssentials.getInstance().getDataManager().getBookmarkManager();
            Map<String, BookmarkManager.BookmarkData> bookmarks = bookmarkManager.getPlayerBookmarks(player.getUUID());
            
            if (bookmarks.isEmpty()) {
                LanguageUtil.sendMessage(player, "commands.tpbookmark.no_bookmarks");
                return 0;
            }
            
            LanguageUtil.sendMessage(player, "commands.tpbookmark.list.header");
            
            // Sort bookmarks by creation time (newest first)
            bookmarks.entrySet().stream()
                .sorted(Map.Entry.<String, BookmarkManager.BookmarkData>comparingByValue(
                    (b1, b2) -> Long.compare(b2.timestamp, b1.timestamp)
                ))
                .forEach(entry -> {
                    String name = entry.getKey();
                    BookmarkManager.BookmarkData bookmark = entry.getValue();
                    
                    String coords = String.format("%.1f, %.1f, %.1f", 
                        bookmark.x, bookmark.y, bookmark.z);
                    
                    LanguageUtil.sendMessage(player, "§e%s §7- %s §8(%s)", 
                        name, coords, bookmark.dimension);
                });
            
            LanguageUtil.sendMessage(player, "commands.tpbookmark.list.footer", 
                String.valueOf(bookmarks.size()), String.valueOf(bookmarkManager.getMaxBookmarks()));
            
            return 1;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error listing bookmarks: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Teleports to a bookmark.
     */
    private int teleportToBookmark(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            String bookmarkName = StringArgumentType.getString(context, "name");
            
            BookmarkManager bookmarkManager = NeoEssentials.getInstance().getDataManager().getBookmarkManager();
            BookmarkManager.BookmarkData bookmark = bookmarkManager.getBookmark(player.getUUID(), bookmarkName);
            
            if (bookmark == null) {
                LanguageUtil.sendErrorMessage(player, "commands.tpbookmark.not_found", bookmarkName);
                return 0;
            }
            
            // Find the target dimension
            net.minecraft.server.level.ServerLevel targetLevel = null;
            if (player.getServer() != null) {
                for (net.minecraft.server.level.ServerLevel level : player.getServer().getAllLevels()) {
                    if (level.dimension().location().toString().equals(bookmark.dimension)) {
                        targetLevel = level;
                        break;
                    }
                }
            } else {
                LanguageUtil.sendErrorMessage(player, "commands.tpbookmark.server_not_available");
                return 0;
            }
            
            if (targetLevel == null) {
                LanguageUtil.sendErrorMessage(player, "commands.tpbookmark.dimension_not_found", bookmark.dimension);
                return 0;
            }
            
            // Teleport the player
            boolean success = TeleportUtil.teleport(player, targetLevel, 
                bookmark.x, bookmark.y, bookmark.z, bookmark.yaw, bookmark.pitch);
            
            if (success) {
                LanguageUtil.sendMessage(player, "commands.tpbookmark.teleported", bookmarkName);
            } else {
                LanguageUtil.sendErrorMessage(player, "commands.tpbookmark.teleport_failed", bookmarkName);
            }
            
            return success ? 1 : 0;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error teleporting to bookmark: {}", e.getMessage());
            return 0;
        }
    }
    
    /**
     * Shows the teleport history GUI (fallback to text for now)
     */
    private int showTeleportHistoryGUI(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            
            // Show the MenuSystem-based GUI
            TeleportHistoryGUI.showFirstPage(player);
            
            return 1;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error showing teleport history GUI: {}", e.getMessage());
            return 0;
        }
    }
    
    /**
     * Teleports to a specific history entry by index
     */
    private int teleportToHistoryEntry(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            int index = IntegerArgumentType.getInteger(context, "index");
            
            var historyManager = NeoEssentials.getInstance().getDataManager().getTeleportHistoryManager();
            var history = historyManager.getPlayerHistory(player.getUUID());
            
            if (history.isEmpty()) {
                LanguageUtil.sendMessage(player, "commands.tphistory.no_history");
                return 0;
            }
            
            if (index >= history.size()) {
                LanguageUtil.sendErrorMessage(player, "commands.tphistory.invalid_index", 
                    String.valueOf(index), String.valueOf(history.size() - 1));
                return 0;
            }
            
            var location = history.get(index);
            
            // Find the target dimension
            net.minecraft.server.level.ServerLevel targetLevel = null;
            if (player.getServer() != null) {
                for (net.minecraft.server.level.ServerLevel level : player.getServer().getAllLevels()) {
                    if (level.dimension().location().toString().equals(location.getDimension())) {
                        targetLevel = level;
                        break;
                    }
                }
            } else {
                LanguageUtil.sendErrorMessage(player, "commands.tpbookmark.server_not_available");
                return 0;
            }
            
            if (targetLevel == null) {
                LanguageUtil.sendErrorMessage(player, "commands.tphistory.dimension_not_found", location.getDimension());
                return 0;
            }
            
            // Teleport the player
            boolean success = TeleportUtil.teleport(player, targetLevel, 
                location.getX(), location.getY(), location.getZ(), 
                location.getYaw(), location.getPitch());
            
            if (success) {
                LanguageUtil.sendMessage(player, "commands.tphistory.teleported", String.valueOf(index + 1));
            } else {
                LanguageUtil.sendErrorMessage(player, "commands.tphistory.teleport_failed", String.valueOf(index + 1));
            }
            
            return success ? 1 : 0;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error teleporting to history entry: {}", e.getMessage());
            return 0;
        }
    }
    
    /**
     * Shows clear history prompt
     */
    private int clearHistoryPrompt(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            
            var historyManager = NeoEssentials.getInstance().getDataManager().getTeleportHistoryManager();
            var history = historyManager.getPlayerHistory(player.getUUID());
            
            if (history.isEmpty()) {
                LanguageUtil.sendMessage(player, "commands.tphistory.no_history");
                return 0;
            }
            
            LanguageUtil.sendMessage(player, "commands.tphistory.clear_prompt", String.valueOf(history.size()));
            LanguageUtil.sendMessage(player, "commands.tphistory.clear_confirm_instruction");
            
            return 1;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error showing clear history prompt: {}", e.getMessage());
            return 0;
        }
    }
    
    /**
     * Confirms and clears teleport history
     */
    private int clearHistoryConfirm(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            
            var historyManager = NeoEssentials.getInstance().getDataManager().getTeleportHistoryManager();
            var history = historyManager.getPlayerHistory(player.getUUID());
            
            if (history.isEmpty()) {
                LanguageUtil.sendMessage(player, "commands.tphistory.no_history");
                return 0;
            }
            
            int clearedCount = history.size();
            historyManager.clearPlayerHistory(player.getUUID());
            
            LanguageUtil.sendMessage(player, "commands.tphistory.cleared", String.valueOf(clearedCount));
            
            return 1;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error clearing teleport history: {}", e.getMessage());
            return 0;
        }
    }
    
    /**
     * Exports teleport history to chat
     */
    private int exportHistory(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            
            var historyManager = NeoEssentials.getInstance().getDataManager().getTeleportHistoryManager();
            var history = historyManager.getPlayerHistory(player.getUUID());
            
            if (history.isEmpty()) {
                LanguageUtil.sendMessage(player, "commands.tphistory.no_history");
                return 0;
            }
            
            LanguageUtil.sendMessage(player, "commands.tphistory.export_header", String.valueOf(history.size()));
            
            for (int i = 0; i < Math.min(history.size(), 20); i++) {
                var location = history.get(i);
                String timeStr = formatTimeAgo(System.currentTimeMillis() - location.getTimestamp());
                String coords = String.format("%.1f, %.1f, %.1f", 
                    location.getX(), location.getY(), location.getZ());
                String dimName = location.getDimension();
                if (dimName.contains(":")) {
                    dimName = dimName.substring(dimName.lastIndexOf(':') + 1);
                }
                
                LanguageUtil.sendMessage(player, "§7%d. §f%s §7(%s) §8- %s", 
                    i + 1, coords, dimName, timeStr);
            }
            
            if (history.size() > 20) {
                LanguageUtil.sendMessage(player, "commands.tphistory.export_truncated", 
                    String.valueOf(history.size() - 20));
            }
            
            return 1;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error exporting teleport history: {}", e.getMessage());
            return 0;
        }
    }
    
    /**
     * Formats a time difference into a human-readable string.
     * 
     * @param timeDiff Time difference in milliseconds
     * @return Formatted time string (e.g., "5m ago", "2h ago", "3d ago")
     */
    private String formatTimeAgo(long timeDiff) {
        long seconds = timeDiff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return days + "d ago";
        } else if (hours > 0) {
            return hours + "h ago";
        } else if (minutes > 0) {
            return minutes + "m ago";
        } else {
            return seconds + "s ago";
        }
    }
}
