package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.ui.MenuSystem;
import com.zerog.neoessentials.util.LanguageUtil;
import com.zerog.neoessentials.util.PerformanceMonitor;
import com.zerog.neoessentials.util.PermissionUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Advanced player statistics and monitoring commands.
 * Provides comprehensive player information, activity tracking, and performance metrics.
 */
public class PlayerStatsCommands {
    
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,##0.00");
    
    /**
     * Registers all player statistics commands.
     *
     * @param dispatcher The command dispatcher to register commands with
     */
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /playerstats - Show own statistics
        dispatcher.register(
            Commands.literal("playerstats")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.playerstats"))
                .executes(this::showOwnStats)
                .then(
                    Commands.argument("player", EntityArgument.player())
                        .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.playerstats.other"))
                        .executes(this::showPlayerStats)
                )
        );
        
        // /playerinfo - Detailed player information
        dispatcher.register(
            Commands.literal("playerinfo")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.playerinfo"))
                .executes(this::showOwnInfo)
                .then(
                    Commands.argument("player", EntityArgument.player())
                        .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.playerinfo.other"))
                        .executes(this::showPlayerInfo)
                )
        );
        
        // /playerperformance - Show player performance metrics
        dispatcher.register(
            Commands.literal("playerperf")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.playerperf"))
                .executes(this::showOwnPerformance)
                .then(
                    Commands.argument("player", EntityArgument.player())
                        .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.playerperf.other"))
                        .executes(this::showPlayerPerformance)
                )
        );
        
        // /onlineplayers - Enhanced online player list with stats
        dispatcher.register(
            Commands.literal("onlineplayers")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.onlineplayers"))
                .executes(this::showOnlinePlayersBasic)
                .then(
                    Commands.literal("detailed")
                        .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.onlineplayers.detailed"))
                        .executes(this::showOnlinePlayersDetailed)
                )
                .then(
                    Commands.literal("gui")
                        .executes(this::showOnlinePlayersGUI)
                )
        );
    }
    
    /**
     * Shows the command sender's own statistics.
     */
    private int showOwnStats(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            return showPlayerStatsInternal(context, player);
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error showing own stats: {}", e.getMessage());
            return 0;
        }
    }
    
    /**
     * Shows another player's statistics.
     */
    private int showPlayerStats(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "player");
            return showPlayerStatsInternal(context, target);
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error showing player stats: {}", e.getMessage());
            return 0;
        }
    }
    
    /**
     * Internal method to display player statistics.
     */
    private int showPlayerStatsInternal(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        try {
            CommandSourceStack source = context.getSource();
            
            LanguageUtil.sendMessage(source, "§6====== Player Statistics: " + target.getName().getString() + " ======");
            
            // Basic player info
            LanguageUtil.sendMessage(source, "§eUUID: §f" + target.getUUID().toString());
            LanguageUtil.sendMessage(source, "§eGamemode: §f" + target.gameMode.getGameModeForPlayer().getName());
            LanguageUtil.sendMessage(source, "§eExperience Level: §f" + target.experienceLevel);
            LanguageUtil.sendMessage(source, "§eHealth: §f" + DECIMAL_FORMAT.format(target.getHealth()) + "/" + 
                                           DECIMAL_FORMAT.format(target.getMaxHealth()));
            LanguageUtil.sendMessage(source, "§eFood Level: §f" + target.getFoodData().getFoodLevel() + "/20");
            
            // Location info
            LanguageUtil.sendMessage(source, "");
            LanguageUtil.sendMessage(source, "§eLocation:");
            LanguageUtil.sendMessage(source, "§f  Dimension: §7" + target.level().dimension().location());
            LanguageUtil.sendMessage(source, "§f  Position: §7" + 
                                   DECIMAL_FORMAT.format(target.getX()) + ", " +
                                   DECIMAL_FORMAT.format(target.getY()) + ", " +
                                   DECIMAL_FORMAT.format(target.getZ()));
            
            // Game statistics
            LanguageUtil.sendMessage(source, "");
            LanguageUtil.sendMessage(source, "§eGame Statistics:");
            
            // Get some basic stats
            long playTime = target.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME));
            long timeSinceRest = target.getStats().getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
            int mobKills = target.getStats().getValue(Stats.CUSTOM.get(Stats.MOB_KILLS));
            int playerKills = target.getStats().getValue(Stats.CUSTOM.get(Stats.PLAYER_KILLS));
            int deaths = target.getStats().getValue(Stats.CUSTOM.get(Stats.DEATHS));
            
            LanguageUtil.sendMessage(source, "§f  Play Time: §7" + formatGameTime(playTime));
            LanguageUtil.sendMessage(source, "§f  Time Since Rest: §7" + formatGameTime(timeSinceRest));
            LanguageUtil.sendMessage(source, "§f  Mob Kills: §7" + mobKills);
            LanguageUtil.sendMessage(source, "§f  Player Kills: §7" + playerKills);
            LanguageUtil.sendMessage(source, "§f  Deaths: §7" + deaths);
            
            LanguageUtil.sendMessage(source, "§6" + "=".repeat(50));
            
            return 1;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error displaying player statistics: {}", e.getMessage());
            return 0;
        }
    }
    
    /**
     * Shows the command sender's own detailed information.
     */
    private int showOwnInfo(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            return showPlayerInfoInternal(context, player);
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error showing own info: {}", e.getMessage());
            return 0;
        }
    }
    
    /**
     * Shows another player's detailed information.
     */
    private int showPlayerInfo(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "player");
            return showPlayerInfoInternal(context, target);
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error showing player info: {}", e.getMessage());
            return 0;
        }
    }
    
    /**
     * Internal method to display detailed player information.
     */
    private int showPlayerInfoInternal(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        try {
            CommandSourceStack source = context.getSource();
            
            LanguageUtil.sendMessage(source, "§6====== Player Information: " + target.getName().getString() + " ======");
            
            // Performance info
            PerformanceMonitor.PlayerPerformanceInfo perfInfo = PerformanceMonitor.getPlayerPerformance(target);
            
            LanguageUtil.sendMessage(source, "§eConnection Info:");
            LanguageUtil.sendMessage(source, "§f  Ping: §7" + perfInfo.ping + "ms");
            LanguageUtil.sendMessage(source, "§f  IP Address: §7" + 
                (target.getIpAddress() != null ? target.getIpAddress() : "Unknown"));
            
            LanguageUtil.sendMessage(source, "");
            LanguageUtil.sendMessage(source, "§eWorld Context:");
            LanguageUtil.sendMessage(source, "§f  Dimension: §7" + perfInfo.dimension);
            LanguageUtil.sendMessage(source, "§f  Chunk: §7" + perfInfo.chunkX + ", " + perfInfo.chunkZ);
            LanguageUtil.sendMessage(source, "§f  Nearby Entities: §7" + perfInfo.nearbyEntities);
            
            // Permission info (if available)
            LanguageUtil.sendMessage(source, "");
            LanguageUtil.sendMessage(source, "§ePermissions:");
            LanguageUtil.sendMessage(source, "§f  Operator: §7" + (target.hasPermissions(2) ? "Yes" : "No"));
            
            // NeoEssentials specific data
            if (NeoEssentials.getInstance().getDataManager() != null) {
                var settingsManager = NeoEssentials.getInstance().getDataManager().getPlayerSettingsManager();
                var settings = settingsManager.getPlayerSettings(target.getUUID());
                
                LanguageUtil.sendMessage(source, "");
                LanguageUtil.sendMessage(source, "§eNeoEssentials Data:");
                LanguageUtil.sendMessage(source, "§f  Settings Configured: §7" + (settings != null ? "Yes" : "No"));
                
                var historyManager = NeoEssentials.getInstance().getDataManager().getTeleportHistoryManager();
                var history = historyManager.getPlayerHistory(target.getUUID());
                LanguageUtil.sendMessage(source, "§f  Teleport History: §7" + history.size() + " entries");
                
                var bookmarkManager = NeoEssentials.getInstance().getDataManager().getBookmarkManager();
                var bookmarks = bookmarkManager.getPlayerBookmarks(target.getUUID());
                LanguageUtil.sendMessage(source, "§f  Bookmarks: §7" + bookmarks.size() + " saved");
            }
            
            LanguageUtil.sendMessage(source, "§6" + "=".repeat(50));
            
            return 1;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error displaying player information: {}", e.getMessage());
            return 0;
        }
    }
    
    /**
     * Shows the command sender's own performance metrics.
     */
    private int showOwnPerformance(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            return showPlayerPerformanceInternal(context, player);
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error showing own performance: {}", e.getMessage());
            return 0;
        }
    }
    
    /**
     * Shows another player's performance metrics.
     */
    private int showPlayerPerformance(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "player");
            return showPlayerPerformanceInternal(context, target);
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error showing player performance: {}", e.getMessage());
            return 0;
        }
    }
    
    /**
     * Internal method to display player performance metrics.
     */
    private int showPlayerPerformanceInternal(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        try {
            CommandSourceStack source = context.getSource();
            PerformanceMonitor.PlayerPerformanceInfo perfInfo = PerformanceMonitor.getPlayerPerformance(target);
            
            LanguageUtil.sendMessage(source, "§6====== Performance: " + target.getName().getString() + " ======");
            
            LanguageUtil.sendMessage(source, "§eConnection Performance:");
            LanguageUtil.sendMessage(source, "§f  Ping: §7" + perfInfo.ping + "ms");
            
            String pingStatus;
            if (perfInfo.ping < 50) pingStatus = "§aExcellent";
            else if (perfInfo.ping < 100) pingStatus = "§eGood";
            else if (perfInfo.ping < 200) pingStatus = "§6Fair";
            else pingStatus = "§cPoor";
            
            LanguageUtil.sendMessage(source, "§f  Connection Quality: " + pingStatus);
            
            LanguageUtil.sendMessage(source, "");
            LanguageUtil.sendMessage(source, "§eWorld Impact:");
            LanguageUtil.sendMessage(source, "§f  Current Chunk: §7[" + perfInfo.chunkX + ", " + perfInfo.chunkZ + "]");
            LanguageUtil.sendMessage(source, "§f  Nearby Entities: §7" + perfInfo.nearbyEntities);
            
            String entityImpact;
            if (perfInfo.nearbyEntities < 50) entityImpact = "§aLow";
            else if (perfInfo.nearbyEntities < 100) entityImpact = "§eModerate";
            else if (perfInfo.nearbyEntities < 200) entityImpact = "§6High";
            else entityImpact = "§cVery High";
            
            LanguageUtil.sendMessage(source, "§f  Entity Load Impact: " + entityImpact);
            
            LanguageUtil.sendMessage(source, "§6" + "=".repeat(40));
            
            return 1;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error displaying player performance: {}", e.getMessage());
            return 0;
        }
    }
    
    /**
     * Shows basic online player list.
     */
    private int showOnlinePlayersBasic(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            var server = source.getServer();
            var players = server.getPlayerList().getPlayers();
            
            LanguageUtil.sendMessage(source, "§6====== Online Players (" + players.size() + ") ======");
            
            for (ServerPlayer player : players) {
                String gamemode = player.gameMode.getGameModeForPlayer().getName();
                String dimension = getDimensionDisplayName(player.level().dimension().location().toString());
                
                LanguageUtil.sendMessage(source, "§e" + player.getName().getString() + 
                                       " §7- " + gamemode + " in " + dimension);
            }
            
            LanguageUtil.sendMessage(source, "§6" + "=".repeat(30));
            
            return 1;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error showing online players: {}", e.getMessage());
            return 0;
        }
    }
    
    /**
     * Shows detailed online player list.
     */
    private int showOnlinePlayersDetailed(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            var server = source.getServer();
            var players = server.getPlayerList().getPlayers();
            
            LanguageUtil.sendMessage(source, "§6====== Detailed Online Players (" + players.size() + ") ======");
            
            for (ServerPlayer player : players) {
                PerformanceMonitor.PlayerPerformanceInfo perfInfo = PerformanceMonitor.getPlayerPerformance(player);
                
                LanguageUtil.sendMessage(source, "§e" + player.getName().getString());
                LanguageUtil.sendMessage(source, "§f  Location: §7" + getDimensionDisplayName(perfInfo.dimension) + 
                                       " [" + perfInfo.chunkX + "," + perfInfo.chunkZ + "]");
                LanguageUtil.sendMessage(source, "§f  Ping: §7" + perfInfo.ping + "ms");
                LanguageUtil.sendMessage(source, "§f  Gamemode: §7" + player.gameMode.getGameModeForPlayer().getName());
                LanguageUtil.sendMessage(source, "");
            }
            
            LanguageUtil.sendMessage(source, "§6" + "=".repeat(40));
            
            return 1;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error showing detailed online players: {}", e.getMessage());
            return 0;
        }
    }
    
    /**
     * Shows online players in GUI format.
     */
    private int showOnlinePlayersGUI(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            var server = context.getSource().getServer();
            var players = server.getPlayerList().getPlayers();
            
            List<MenuSystem.MenuItem> playerItems = players.stream()
                .map(p -> {
                    PerformanceMonitor.PlayerPerformanceInfo perfInfo = PerformanceMonitor.getPlayerPerformance(p);
                    String description = String.format(
                        "&7%s in %s\n&7Ping: %sms | Entities: %d",
                        p.gameMode.getGameModeForPlayer().getName(),
                        getDimensionDisplayName(perfInfo.dimension),
                        Math.round(perfInfo.ping),
                        perfInfo.nearbyEntities
                    );
                    
                    return new MenuSystem.MenuItem(
                        "&e" + p.getName().getString(),
                        "/playerinfo " + p.getName().getString(),
                        description
                    );
                })
                .collect(Collectors.toList());
            
            showSimpleMenu(player, "Online Players (" + players.size() + ")", playerItems, 
                         "§7Click on a player name to view their detailed information", 
                         "§7Use §e/onlineplayers detailed §7for more information");
            
            return 1;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error showing online players GUI: {}", e.getMessage());
            return 0;
        }
    }
    
    /**
     * Formats game time in ticks to a human-readable format.
     */
    private String formatGameTime(long ticks) {
        long seconds = ticks / 20; // 20 ticks per second
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return String.format("%dd %dh %dm", days, hours % 24, minutes % 60);
        } else if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes % 60, seconds % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }
    
    /**
     * Gets a user-friendly display name for a dimension.
     */
    private String getDimensionDisplayName(String dimensionId) {
        if (dimensionId.contains(":")) {
            String[] parts = dimensionId.split(":");
            String name = parts[parts.length - 1];
            return name.substring(0, 1).toUpperCase() + name.substring(1).replace("_", " ");
        }
        return dimensionId;
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
