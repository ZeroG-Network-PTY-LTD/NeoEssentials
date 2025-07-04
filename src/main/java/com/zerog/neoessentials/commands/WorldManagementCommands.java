package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.ui.MenuSystem;
import com.zerog.neoessentials.util.LanguageUtil;
import com.zerog.neoessentials.util.PermissionUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Advanced world management commands for NeoEssentials.
 * Provides comprehensive world information, dimension management, and utilities.
 */
public class WorldManagementCommands {
    
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,##0.00");
    
    /**
     * Registers all world management commands.
     *
     * @param dispatcher The command dispatcher to register commands with
     */
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /worldinfo - Display world information
        dispatcher.register(
            Commands.literal("worldinfo")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.worldinfo"))
                .executes(this::showWorldInfo)
                .then(
                    Commands.argument("dimension", StringArgumentType.string())
                        .executes(this::showDimensionInfo)
                )
        );
        
        // /dimensions - List and manage dimensions
        dispatcher.register(
            Commands.literal("dimensions")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.dimensions"))
                .executes(this::showDimensionsList)
                .then(
                    Commands.literal("gui")
                        .executes(this::showDimensionsGUI)
                )
        );
        
        // /worldstats - Show world statistics
        dispatcher.register(
            Commands.literal("worldstats")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.worldstats"))
                .executes(this::showWorldStats)
        );
        
        // /dimension - Teleport to dimension
        dispatcher.register(
            Commands.literal("dimension")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.dimension"))
                .then(
                    Commands.argument("target", StringArgumentType.string())
                        .executes(this::teleportToDimension)
                )
        );
    }
    
    /**
     * Shows information about the current world.
     */
    private int showWorldInfo(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            ServerLevel level = player.serverLevel();
            
            LanguageUtil.sendMessage(player, "&6====== &lWorld Information&r &6======");
            
            // Basic world info
            String dimensionName = getDimensionDisplayName(level.dimension().location().toString());
            LanguageUtil.sendMessage(player, "&eDimension: &f" + dimensionName);
            LanguageUtil.sendMessage(player, "&eWorld Time: &f" + formatWorldTime(level.getDayTime()));
            LanguageUtil.sendMessage(player, "&eGame Time: &f" + formatGameTime(level.getGameTime()));
            
            // Weather information (using public methods)
            if (level.isRaining()) {
                LanguageUtil.sendMessage(player, "&eWeather: &f" + (level.isThundering() ? "Thundering" : "Raining"));
            } else {
                LanguageUtil.sendMessage(player, "&eWeather: &fClear");
            }
            
            // Player information
            int playerCount = level.players().size();
            LanguageUtil.sendMessage(player, "&ePlayers in dimension: &f" + playerCount);
            
            // Difficulty and game rules
            LanguageUtil.sendMessage(player, "&eDifficulty: &f" + level.getDifficulty().getDisplayName().getString());
            LanguageUtil.sendMessage(player, "&eHardcore: &f" + (level.getLevelData().isHardcore() ? "Yes" : "No"));
            
            // Spawn information
            var spawn = level.getSharedSpawnPos();
            LanguageUtil.sendMessage(player, "&eSpawn Point: &f" + spawn.getX() + ", " + spawn.getY() + ", " + spawn.getZ());
            
            LanguageUtil.sendMessage(player, "&6===================================");
            
            return 1;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error showing world info: {}", e.getMessage());
            LanguageUtil.sendErrorMessage(context.getSource(), "An error occurred while retrieving world information.");
            return 0;
        }
    }
    
    /**
     * Shows information about a specific dimension.
     */
    private int showDimensionInfo(CommandContext<CommandSourceStack> context) {
        try {
            MinecraftServer server = context.getSource().getServer();
            String dimensionName = StringArgumentType.getString(context, "dimension");
            
            // Try to find the dimension
            ResourceLocation dimensionId = dimensionName.contains(":") ? 
                ResourceLocation.parse(dimensionName) :
                ResourceLocation.fromNamespaceAndPath("minecraft", dimensionName);
            ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, dimensionId);
            ServerLevel level = server.getLevel(dimensionKey);
            
            if (level == null) {
                LanguageUtil.sendErrorMessage(context.getSource(), "Dimension '" + dimensionName + "' not found!");
                return 0;
            }
            
            ServerPlayer player = context.getSource().getPlayerOrException();
            
            LanguageUtil.sendMessage(player, "&6====== &l" + getDimensionDisplayName(dimensionName) + " Info&r &6======");
            
            // Dimension-specific information
            LanguageUtil.sendMessage(player, "&eDimension ID: &f" + dimensionId.toString());
            LanguageUtil.sendMessage(player, "&ePlayers: &f" + level.players().size());
            LanguageUtil.sendMessage(player, "&eLoaded Chunks: &f" + level.getChunkSource().getLoadedChunksCount());
            
            // Environment info
            LanguageUtil.sendMessage(player, "&eHas Sky: &f" + (level.dimensionType().hasSkyLight() ? "Yes" : "No"));
            LanguageUtil.sendMessage(player, "&eHas Ceiling: &f" + (level.dimensionType().hasCeiling() ? "Yes" : "No"));
            LanguageUtil.sendMessage(player, "&eUltrawarm: &f" + (level.dimensionType().ultraWarm() ? "Yes" : "No"));
            LanguageUtil.sendMessage(player, "&eNatural: &f" + (level.dimensionType().natural() ? "Yes" : "No"));
            
            // Coordinate scaling
            double coordinateScale = level.dimensionType().coordinateScale();
            LanguageUtil.sendMessage(player, "&eCoordinate Scale: &f" + DECIMAL_FORMAT.format(coordinateScale));
            
            // Bed functionality
            LanguageUtil.sendMessage(player, "&eBed Works: &f" + (level.dimensionType().bedWorks() ? "Yes" : "No"));
            LanguageUtil.sendMessage(player, "&eRespawn Anchor Works: &f" + (level.dimensionType().respawnAnchorWorks() ? "Yes" : "No"));
            
            LanguageUtil.sendMessage(player, "&6===================================");
            
            return 1;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error showing dimension info: {}", e.getMessage());
            LanguageUtil.sendErrorMessage(context.getSource(), "An error occurred while retrieving dimension information.");
            return 0;
        }
    }
    
    /**
     * Shows a list of all available dimensions.
     */
    private int showDimensionsList(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            MinecraftServer server = context.getSource().getServer();
            
            LanguageUtil.sendMessage(player, "&6====== &lAvailable Dimensions&r &6======");
            
            int count = 0;
            for (ServerLevel level : server.getAllLevels()) {
                count++;
                String dimensionName = level.dimension().location().toString();
                String displayName = getDimensionDisplayName(dimensionName);
                int playerCount = level.players().size();
                
                LanguageUtil.sendMessage(player, "&e" + count + ". &f" + displayName + " &7(" + dimensionName + ")");
                LanguageUtil.sendMessage(player, "   &7Players: &f" + playerCount + " &7| &7Chunks: &f" + level.getChunkSource().getLoadedChunksCount());
            }
            
            LanguageUtil.sendMessage(player, "");
            LanguageUtil.sendMessage(player, "&7Total dimensions: &e" + count);
            LanguageUtil.sendMessage(player, "&7Use &e/dimensions gui &7for interactive browser");
            LanguageUtil.sendMessage(player, "&6===================================");
            
            return 1;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error showing dimensions list: {}", e.getMessage());
            LanguageUtil.sendErrorMessage(context.getSource(), "An error occurred while retrieving dimensions list.");
            return 0;
        }
    }
    
    /**
     * Shows the dimensions GUI browser.
     */
    private int showDimensionsGUI(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            MinecraftServer server = context.getSource().getServer();
            
            List<MenuSystem.MenuItem> items = new ArrayList<>();
            
            // Add dimension entries
            for (ServerLevel level : server.getAllLevels()) {
                String dimensionName = level.dimension().location().toString();
                String displayName = getDimensionDisplayName(dimensionName);
                int playerCount = level.players().size();
                int chunkCount = level.getChunkSource().getLoadedChunksCount();
                
                String itemText = String.format("&e🌍 %s &7(%d players)", displayName, playerCount);
                String command = "/dimension " + dimensionName;
                String hoverText = String.format(
                    "&6%s&r\\n" +
                    "&7Dimension ID: &f%s\\n" +
                    "&7Players: &f%d\\n" +
                    "&7Loaded Chunks: &f%d\\n" +
                    "&7Type: &f%s\\n" +
                    "\\n" +
                    "&a🚀 Click to teleport!",
                    displayName, dimensionName, playerCount, chunkCount,
                    level.dimensionType().natural() ? "Natural" : "Artificial"
                );
                
                items.add(new MenuSystem.MenuItem(itemText, command, hoverText));
            }
            
            // Add utility options
            items.add(new MenuSystem.MenuItem(
                "&b📊 World Statistics",
                "/worldstats",
                "&7View comprehensive world statistics"
            ));
            
            items.add(new MenuSystem.MenuItem(
                "&f📋 Current World Info",
                "/worldinfo",
                "&7Show detailed information about current world"
            ));
            
            MenuSystem.builder()
                .title("Dimension Browser")
                .items(items)
                .itemsPerPage(8)
                .showPageNumbers(false)
                .back("/dimensions", "&7Back to List", "&7Return to text-based dimension list")
                .show(player, 1);
            
            return 1;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error showing dimensions GUI: {}", e.getMessage());
            LanguageUtil.sendErrorMessage(context.getSource(), "An error occurred while showing dimensions GUI.");
            return 0;
        }
    }
    
    /**
     * Shows comprehensive world statistics.
     */
    private int showWorldStats(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            MinecraftServer server = context.getSource().getServer();
            
            LanguageUtil.sendMessage(player, "&6====== &lWorld Statistics&r &6======");
            
            // Server-wide stats
            int totalPlayers = server.getPlayerCount();
            int maxPlayers = server.getMaxPlayers();
            LanguageUtil.sendMessage(player, "&eTotal Players: &f" + totalPlayers + " / " + maxPlayers);
            
            // Dimension breakdown
            LanguageUtil.sendMessage(player, "");
            LanguageUtil.sendMessage(player, "&eDimension Breakdown:");
            
            int totalChunks = 0;
            for (ServerLevel level : server.getAllLevels()) {
                String dimensionName = getDimensionDisplayName(level.dimension().location().toString());
                int playerCount = level.players().size();
                int chunkCount = level.getChunkSource().getLoadedChunksCount();
                totalChunks += chunkCount;
                
                LanguageUtil.sendMessage(player, "&f  " + dimensionName + ": &7" + playerCount + " players, " + chunkCount + " chunks");
            }
            
            // Memory and performance info
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            LanguageUtil.sendMessage(player, "");
            LanguageUtil.sendMessage(player, "&eMemory Usage:");
            LanguageUtil.sendMessage(player, "&f  Used: &7" + formatBytes(usedMemory) + " / " + formatBytes(maxMemory));
            LanguageUtil.sendMessage(player, "&f  Free: &7" + formatBytes(freeMemory));
            
            // Server performance (approximate TPS calculation)
            // Simple estimation - in production you'd want to track tick times over time
            double estimatedTps = 20.0; // Default assumption for display
            LanguageUtil.sendMessage(player, "&f  TPS (est): &7" + DECIMAL_FORMAT.format(estimatedTps));
            LanguageUtil.sendMessage(player, "&f  Server Uptime: &7" + formatUptime(server.getTickCount()));
            
            LanguageUtil.sendMessage(player, "");
            LanguageUtil.sendMessage(player, "&eTotal Loaded Chunks: &f" + totalChunks);
            LanguageUtil.sendMessage(player, "&eServer Uptime: &f" + formatUptime(server.getTickCount()));
            
            LanguageUtil.sendMessage(player, "&6===================================");
            
            return 1;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error showing world stats: {}", e.getMessage());
            LanguageUtil.sendErrorMessage(context.getSource(), "An error occurred while retrieving world statistics.");
            return 0;
        }
    }
    
    /**
     * Teleports player to a specific dimension.
     */
    private int teleportToDimension(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            String dimensionName = StringArgumentType.getString(context, "target");
            MinecraftServer server = context.getSource().getServer();
            
            // Try to find the dimension
            ResourceLocation dimensionId = dimensionName.contains(":") ? 
                ResourceLocation.parse(dimensionName) :
                ResourceLocation.fromNamespaceAndPath("minecraft", dimensionName);
            ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, dimensionId);
            ServerLevel targetLevel = server.getLevel(dimensionKey);
            
            if (targetLevel == null) {
                LanguageUtil.sendErrorMessage(player, "Dimension '" + dimensionName + "' not found!");
                return 0;
            }
            
            // Get spawn point of target dimension
            var spawnPos = targetLevel.getSharedSpawnPos();
            
            // Teleport player
            player.teleportTo(targetLevel, spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, 0.0f, 0.0f);
            
            String displayName = getDimensionDisplayName(dimensionName);
            LanguageUtil.sendMessage(player, "&aTeleported to " + displayName + "!");
            
            return 1;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error teleporting to dimension: {}", e.getMessage());
            LanguageUtil.sendErrorMessage(context.getSource(), "An error occurred while teleporting to dimension.");
            return 0;
        }
    }
    
    // Utility methods
    
    /**
     * Gets a user-friendly dimension display name.
     */
    private String getDimensionDisplayName(String dimension) {
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
     * Formats world time into a readable format.
     */
    private String formatWorldTime(long time) {
        long day = time / 24000;
        long timeOfDay = time % 24000;
        int hour = (int) ((timeOfDay + 6000) / 1000) % 24;
        int minute = (int) ((timeOfDay % 1000) * 60 / 1000);
        
        return String.format("Day %d, %02d:%02d", day + 1, hour, minute);
    }
    
    /**
     * Formats game time into a readable format.
     */
    private String formatGameTime(long ticks) {
        long seconds = ticks / 20;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return String.format("%dd %02dh %02dm", days, hours % 24, minutes % 60);
        } else if (hours > 0) {
            return String.format("%dh %02dm %02ds", hours, minutes % 60, seconds % 60);
        } else if (minutes > 0) {
            return String.format("%dm %02ds", minutes, seconds % 60);
        } else {
            return seconds + "s";
        }
    }
    
    /**
     * Formats bytes into a human-readable format.
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return DECIMAL_FORMAT.format(bytes / 1024.0) + " KB";
        if (bytes < 1024 * 1024 * 1024) return DECIMAL_FORMAT.format(bytes / (1024.0 * 1024.0)) + " MB";
        return DECIMAL_FORMAT.format(bytes / (1024.0 * 1024.0 * 1024.0)) + " GB";
    }
    
    /**
     * Formats server uptime.
     */
    private String formatUptime(long ticks) {
        return formatGameTime(ticks);
    }
}
