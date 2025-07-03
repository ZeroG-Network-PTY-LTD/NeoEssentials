package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.util.LanguageUtil;
import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.utils.TeleportHistory;
import com.zerog.neoessentials.utils.TeleportUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

/**
 * Enhanced teleportation commands with bookmarks and history management.
 * Provides advanced teleportation features beyond basic teleport commands.
 */
public class TeleportBookmarkCommands {

    // Storage for player bookmarks
    private static final Map<UUID, Map<String, BookmarkLocation>> playerBookmarks = new HashMap<>();
    
    // Maximum bookmarks per player
    private static final int MAX_BOOKMARKS = 20;

    /**
     * Registers all enhanced teleportation commands.
     *
     * @param dispatcher The command dispatcher to register commands with
     */
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /tphistory - Show teleport history
        dispatcher.register(
            Commands.literal("tphistory")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.tphistory"))
                .executes(this::showTeleportHistory)
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
            
            // This is a simplified version - in a full implementation you'd want to 
            // extend TeleportHistory to provide a method to get history for display
            LanguageUtil.sendMessage(player, "commands.tphistory.header");
            LanguageUtil.sendMessage(player, "§7Use /back to return to your previous location.");
            
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
            
            UUID playerId = player.getUUID();
            Map<String, BookmarkLocation> bookmarks = playerBookmarks.computeIfAbsent(playerId, k -> new HashMap<>());
            
            // Check if player has too many bookmarks
            if (bookmarks.size() >= MAX_BOOKMARKS && !bookmarks.containsKey(bookmarkName)) {
                LanguageUtil.sendErrorMessage(player, "commands.tpbookmark.too_many", String.valueOf(MAX_BOOKMARKS));
                return 0;
            }
            
            // Create the bookmark
            BookmarkLocation bookmark = new BookmarkLocation(
                player.serverLevel().dimension().location().toString(),
                player.getX(),
                player.getY(),
                player.getZ(),
                player.getYRot(),
                player.getXRot(),
                System.currentTimeMillis()
            );
            
            bookmarks.put(bookmarkName, bookmark);
            
            LanguageUtil.sendMessage(player, "commands.tpbookmark.added", bookmarkName);
            
            return 1;
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
            
            UUID playerId = player.getUUID();
            Map<String, BookmarkLocation> bookmarks = playerBookmarks.get(playerId);
            
            if (bookmarks == null || !bookmarks.containsKey(bookmarkName)) {
                LanguageUtil.sendErrorMessage(player, "commands.tpbookmark.not_found", bookmarkName);
                return 0;
            }
            
            bookmarks.remove(bookmarkName);
            
            LanguageUtil.sendMessage(player, "commands.tpbookmark.removed", bookmarkName);
            
            return 1;
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
            
            UUID playerId = player.getUUID();
            Map<String, BookmarkLocation> bookmarks = playerBookmarks.get(playerId);
            
            if (bookmarks == null || bookmarks.isEmpty()) {
                LanguageUtil.sendMessage(player, "commands.tpbookmark.no_bookmarks");
                return 0;
            }
            
            LanguageUtil.sendMessage(player, "commands.tpbookmark.list.header");
            
            // Sort bookmarks by creation time (newest first)
            bookmarks.entrySet().stream()
                .sorted(Map.Entry.<String, BookmarkLocation>comparingByValue(
                    (b1, b2) -> Long.compare(b2.timestamp, b1.timestamp)
                ))
                .forEach(entry -> {
                    String name = entry.getKey();
                    BookmarkLocation bookmark = entry.getValue();
                    
                    String coords = String.format("%.1f, %.1f, %.1f", 
                        bookmark.x, bookmark.y, bookmark.z);
                    
                    LanguageUtil.sendMessage(player, "§e%s §7- %s §8(%s)", 
                        name, coords, bookmark.dimension);
                });
            
            LanguageUtil.sendMessage(player, "commands.tpbookmark.list.footer", 
                String.valueOf(bookmarks.size()), String.valueOf(MAX_BOOKMARKS));
            
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
            
            UUID playerId = player.getUUID();
            Map<String, BookmarkLocation> bookmarks = playerBookmarks.get(playerId);
            
            if (bookmarks == null || !bookmarks.containsKey(bookmarkName)) {
                LanguageUtil.sendErrorMessage(player, "commands.tpbookmark.not_found", bookmarkName);
                return 0;
            }
            
            BookmarkLocation bookmark = bookmarks.get(bookmarkName);
            
            // Find the target dimension
            net.minecraft.server.level.ServerLevel targetLevel = null;
            for (net.minecraft.server.level.ServerLevel level : player.getServer().getAllLevels()) {
                if (level.dimension().location().toString().equals(bookmark.dimension)) {
                    targetLevel = level;
                    break;
                }
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
     * Class to store bookmark location data.
     */
    private static class BookmarkLocation {
        public final String dimension;
        public final double x, y, z;
        public final float yaw, pitch;
        public final long timestamp;

        public BookmarkLocation(String dimension, double x, double y, double z, float yaw, float pitch, long timestamp) {
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
            this.timestamp = timestamp;
        }
    }
}
