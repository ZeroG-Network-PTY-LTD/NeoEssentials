package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.features.ScoreboardManager;
import com.zerog.neoessentials.permissions.PermissionNodes;
import com.zerog.neoessentials.util.DebugUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Professional Scoreboard Command System
 * 
 * Commands:
 * /scoreboard reload - Reload scoreboard configuration
 * /scoreboard toggle [player] - Toggle scoreboard for player
 * /scoreboard status - Show scoreboard system status
 * /scoreboard update [player] - Force update scoreboard
 * /scoreboard test <player> <layout> - Test specific layout on player
 */
public class ScoreboardCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("scoreboard")
            .requires(source -> source.hasPermission(2) || 
                     (source.getEntity() instanceof ServerPlayer player && 
                      hasPermission(player, PermissionNodes.ADMIN_SCOREBOARD)))
            
            // /scoreboard reload
            .then(Commands.literal("reload")
                .requires(source -> source.hasPermission(2) || 
                         (source.getEntity() instanceof ServerPlayer player && 
                          hasPermission(player, PermissionNodes.ADMIN_SCOREBOARD_RELOAD)))
                .executes(ScoreboardCommand::reloadConfig))
            
            // /scoreboard status
            .then(Commands.literal("status")
                .executes(ScoreboardCommand::showStatus))
            
            // /scoreboard toggle [player]
            .then(Commands.literal("toggle")
                .executes(ScoreboardCommand::toggleSelf)
                .then(Commands.argument("player", EntityArgument.player())
                    .requires(source -> source.hasPermission(2) || 
                             (source.getEntity() instanceof ServerPlayer player && 
                              hasPermission(player, PermissionNodes.ADMIN_SCOREBOARD_TOGGLE)))
                    .executes(ScoreboardCommand::toggleOther)))
            
            // /scoreboard update [player]
            .then(Commands.literal("update")
                .executes(ScoreboardCommand::updateSelf)
                .then(Commands.argument("player", EntityArgument.player())
                    .requires(source -> source.hasPermission(2) || 
                             (source.getEntity() instanceof ServerPlayer player && 
                              hasPermission(player, PermissionNodes.ADMIN_SCOREBOARD_UPDATE)))
                    .executes(ScoreboardCommand::updateOther)))
            
            // /scoreboard updateall
            .then(Commands.literal("updateall")
                .requires(source -> source.hasPermission(2) || 
                         (source.getEntity() instanceof ServerPlayer player && 
                          hasPermission(player, PermissionNodes.ADMIN_SCOREBOARD_UPDATE)))
                .executes(ScoreboardCommand::updateAll))
            
            // /scoreboard test <player> <layout>
            .then(Commands.literal("test")
                .requires(source -> source.hasPermission(3) || 
                         (source.getEntity() instanceof ServerPlayer player && 
                          hasPermission(player, PermissionNodes.ADMIN_SCOREBOARD_TEST)))
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("layout", StringArgumentType.word())
                        .executes(ScoreboardCommand::testLayout))))
        );
    }
    
    /**
     * Reload scoreboard configuration
     */
    private static int reloadConfig(CommandContext<CommandSourceStack> context) {
        try {
            ScoreboardManager scoreboardMgr = ScoreboardManager.getInstance();
            if (scoreboardMgr == null) {
                context.getSource().sendFailure(Component.literal("§cScoreboard system not initialized"));
                return 0;
            }
            
            scoreboardMgr.reloadConfig();
            context.getSource().sendSuccess(() -> Component.literal("§aScoreboard configuration reloaded successfully"), true);
            
            DebugUtil.debugLog("[ScoreboardCommand] Configuration reloaded by " + context.getSource().getTextName());
            return 1;
            
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError reloading scoreboard config: " + e.getMessage()));
            DebugUtil.errorLog("[ScoreboardCommand] Error reloading config: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Show scoreboard system status
     */
    private static int showStatus(CommandContext<CommandSourceStack> context) {
        try {
            ScoreboardManager scoreboardMgr = ScoreboardManager.getInstance();
            if (scoreboardMgr == null) {
                context.getSource().sendFailure(Component.literal("§cScoreboard system not initialized"));
                return 0;
            }
            
            String status = scoreboardMgr.getStatus();
            context.getSource().sendSuccess(() -> Component.literal("§6Scoreboard Status:\n§7" + status), false);
            
            return 1;
            
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError getting scoreboard status: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Toggle scoreboard for command sender
     */
    private static int toggleSelf(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("§cThis command can only be used by players"));
            return 0;
        }
        
        return toggleScoreboard(context, player);
    }
    
    /**
     * Toggle scoreboard for another player
     */
    private static int toggleOther(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
            return toggleScoreboard(context, targetPlayer);
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError toggling scoreboard: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Toggle scoreboard for a player
     */
    private static int toggleScoreboard(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        try {
            ScoreboardManager scoreboardMgr = ScoreboardManager.getInstance();
            if (scoreboardMgr == null) {
                context.getSource().sendFailure(Component.literal("§cScoreboard system not initialized"));
                return 0;
            }
            
            scoreboardMgr.toggleScoreboard(player);
            
            String playerName = player.getName().getString();
            if (context.getSource().getEntity() == player) {
                context.getSource().sendSuccess(() -> Component.literal("§aScoreboard toggled"), false);
            } else {
                context.getSource().sendSuccess(() -> Component.literal("§aScoreboard toggled for " + playerName), true);
            }
            
            DebugUtil.debugLog("[ScoreboardCommand] Scoreboard toggled for " + playerName + " by " + context.getSource().getTextName());
            return 1;
            
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError toggling scoreboard: " + e.getMessage()));
            DebugUtil.errorLog("[ScoreboardCommand] Error toggling scoreboard: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Update scoreboard for command sender
     */
    private static int updateSelf(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("§cThis command can only be used by players"));
            return 0;
        }
        
        return updateScoreboard(context, player);
    }
    
    /**
     * Update scoreboard for another player
     */
    private static int updateOther(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
            return updateScoreboard(context, targetPlayer);
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError updating scoreboard: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Update scoreboard for a player
     */
    private static int updateScoreboard(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        try {
            ScoreboardManager scoreboardMgr = ScoreboardManager.getInstance();
            if (scoreboardMgr == null) {
                context.getSource().sendFailure(Component.literal("§cScoreboard system not initialized"));
                return 0;
            }
            
            scoreboardMgr.forceUpdateScoreboard(player);
            
            String playerName = player.getName().getString();
            if (context.getSource().getEntity() == player) {
                context.getSource().sendSuccess(() -> Component.literal("§aScoreboard updated"), false);
            } else {
                context.getSource().sendSuccess(() -> Component.literal("§aScoreboard updated for " + playerName), true);
            }
            
            DebugUtil.debugLog("[ScoreboardCommand] Scoreboard updated for " + playerName + " by " + context.getSource().getTextName());
            return 1;
            
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError updating scoreboard: " + e.getMessage()));
            DebugUtil.errorLog("[ScoreboardCommand] Error updating scoreboard: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Update all scoreboards
     */
    private static int updateAll(CommandContext<CommandSourceStack> context) {
        try {
            ScoreboardManager scoreboardMgr = ScoreboardManager.getInstance();
            if (scoreboardMgr == null) {
                context.getSource().sendFailure(Component.literal("§cScoreboard system not initialized"));
                return 0;
            }
            
            scoreboardMgr.updateAllScoreboards();
            context.getSource().sendSuccess(() -> Component.literal("§aAll scoreboards updated"), true);
            
            DebugUtil.debugLog("[ScoreboardCommand] All scoreboards updated by " + context.getSource().getTextName());
            return 1;
            
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError updating all scoreboards: " + e.getMessage()));
            DebugUtil.errorLog("[ScoreboardCommand] Error updating all scoreboards: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Test specific layout on player
     */
    private static int testLayout(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
            String layoutName = StringArgumentType.getString(context, "layout");
            
            // For now, just force update the scoreboard
            // In the future, this could temporarily apply a specific layout
            ScoreboardManager scoreboardMgr = ScoreboardManager.getInstance();
            if (scoreboardMgr == null) {
                context.getSource().sendFailure(Component.literal("§cScoreboard system not initialized"));
                return 0;
            }
            
            scoreboardMgr.forceUpdateScoreboard(targetPlayer);
            
            String playerName = targetPlayer.getName().getString();
            context.getSource().sendSuccess(() -> Component.literal("§aLayout test applied to " + playerName + 
                                                                   " (Note: Full layout testing coming soon)"), true);
            
            DebugUtil.debugLog("[ScoreboardCommand] Layout test '" + layoutName + "' applied to " + playerName + 
                              " by " + context.getSource().getTextName());
            return 1;
            
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError testing layout: " + e.getMessage()));
            DebugUtil.errorLog("[ScoreboardCommand] Error testing layout: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Check if player has permission
     */
    private static boolean hasPermission(ServerPlayer player, String permission) {
        try {
            com.zerog.neoessentials.permissions.CustomPermissionsManager permMgr = 
                com.zerog.neoessentials.permissions.CustomPermissionsManager.getInstance();
            return permMgr.hasPermission(player, permission);
        } catch (Exception e) {
            DebugUtil.debugLog("[ScoreboardCommand] Error checking permission: " + e.getMessage());
            return false;
        }
    }
}
