package com.zerog.neoessentials.commands.debug;

import com.mojang.brigadier.CommandDispatcher;
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
 * Debug command for testing the professional scoreboard system
 * 
 * Commands:
 * /scoreboardtest reload - Reload and test scoreboard configuration
 * /scoreboardtest status - Show detailed scoreboard system status
 * /scoreboardtest player <player> - Show player-specific scoreboard info
 * /scoreboardtest toggle <player> - Toggle scoreboard for testing
 * /scoreboardtest info - Show system information
 */
public class ScoreboardTestCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("scoreboardtest")
            .requires(source -> source.hasPermission(2) || 
                     (source.getEntity() instanceof ServerPlayer player && 
                      hasPermission(player, PermissionNodes.ADMIN_SCOREBOARD_TEST)))
            
            // /scoreboardtest reload
            .then(Commands.literal("reload")
                .executes(ScoreboardTestCommand::testReload))
            
            // /scoreboardtest status
            .then(Commands.literal("status")
                .executes(ScoreboardTestCommand::showDetailedStatus))
            
            // /scoreboardtest player <player>
            .then(Commands.literal("player")
                .then(Commands.argument("target", EntityArgument.player())
                    .executes(ScoreboardTestCommand::showPlayerInfo)))
            
            // /scoreboardtest toggle <player>
            .then(Commands.literal("toggle")
                .then(Commands.argument("target", EntityArgument.player())
                    .executes(ScoreboardTestCommand::toggleTest)))
            
            // /scoreboardtest info
            .then(Commands.literal("info")
                .executes(ScoreboardTestCommand::showSystemInfo))
        );
    }
    
    /**
     * Test scoreboard reload functionality
     */
    private static int testReload(CommandContext<CommandSourceStack> context) {
        try {
            ScoreboardManager scoreboardMgr = ScoreboardManager.getInstance();
            if (scoreboardMgr == null) {
                context.getSource().sendFailure(Component.literal("§c[ScoreboardTest] Scoreboard system not initialized"));
                return 0;
            }
            
            context.getSource().sendSuccess(() -> Component.literal("§e[ScoreboardTest] Testing scoreboard reload..."), false);
            
            long startTime = System.currentTimeMillis();
            scoreboardMgr.reloadConfig();
            long endTime = System.currentTimeMillis();
            
            context.getSource().sendSuccess(() -> Component.literal("§a[ScoreboardTest] Reload completed in " + (endTime - startTime) + "ms"), false);
            context.getSource().sendSuccess(() -> Component.literal("§7[ScoreboardTest] Status: " + scoreboardMgr.getStatus()), false);
            
            DebugUtil.debugLog("[ScoreboardTest] Reload test completed by " + context.getSource().getTextName());
            return 1;
            
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§c[ScoreboardTest] Error during reload test: " + e.getMessage()));
            DebugUtil.errorLog("[ScoreboardTest] Error in reload test: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Show detailed scoreboard system status
     */
    private static int showDetailedStatus(CommandContext<CommandSourceStack> context) {
        try {
            ScoreboardManager scoreboardMgr = ScoreboardManager.getInstance();
            if (scoreboardMgr == null) {
                context.getSource().sendFailure(Component.literal("§c[ScoreboardTest] Scoreboard system not initialized"));
                return 0;
            }
            
            context.getSource().sendSuccess(() -> Component.literal("§6[ScoreboardTest] Detailed Scoreboard Status"), false);
            context.getSource().sendSuccess(() -> Component.literal("§7━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"), false);
            
            String status = scoreboardMgr.getStatus();
            context.getSource().sendSuccess(() -> Component.literal("§7System: " + status), false);
            
            // Show config file status
            java.io.File configFile = new java.io.File("config/neoessentials/scoreboard.json");
            context.getSource().sendSuccess(() -> Component.literal("§7Config File: " + 
                (configFile.exists() ? "§aFound" : "§cMissing") + " §7(" + configFile.getAbsolutePath() + ")"), false);
            
            if (configFile.exists()) {
                context.getSource().sendSuccess(() -> Component.literal("§7Config Size: " + configFile.length() + " bytes"), false);
                context.getSource().sendSuccess(() -> Component.literal("§7Last Modified: " + 
                    new java.util.Date(configFile.lastModified())), false);
            }
            
            // Show active players with scoreboards
            net.minecraft.server.MinecraftServer server = context.getSource().getServer();
            final int playersWithScoreboards = server.getPlayerList().getPlayers().size();
            
            context.getSource().sendSuccess(() -> Component.literal("§7Active Players: §e" + playersWithScoreboards), false);
            context.getSource().sendSuccess(() -> Component.literal("§7━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"), false);
            
            return 1;
            
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§c[ScoreboardTest] Error getting detailed status: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Show player-specific scoreboard information
     */
    private static int showPlayerInfo(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "target");
            ScoreboardManager scoreboardMgr = ScoreboardManager.getInstance();
            
            if (scoreboardMgr == null) {
                context.getSource().sendFailure(Component.literal("§c[ScoreboardTest] Scoreboard system not initialized"));
                return 0;
            }
            
            String playerName = targetPlayer.getName().getString();
            context.getSource().sendSuccess(() -> Component.literal("§6[ScoreboardTest] Player Info: " + playerName), false);
            context.getSource().sendSuccess(() -> Component.literal("§7━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"), false);
            
            // Show player stats
            context.getSource().sendSuccess(() -> Component.literal("§7Health: §c" + (int)targetPlayer.getHealth() + 
                "§7/§c" + (int)targetPlayer.getMaxHealth()), false);
            context.getSource().sendSuccess(() -> Component.literal("§7Food: §6" + targetPlayer.getFoodData().getFoodLevel()), false);
            context.getSource().sendSuccess(() -> Component.literal("§7Level: §a" + targetPlayer.experienceLevel), false);
            context.getSource().sendSuccess(() -> Component.literal("§7Location: §e" + 
                (int)targetPlayer.getX() + ", " + (int)targetPlayer.getY() + ", " + (int)targetPlayer.getZ()), false);
            context.getSource().sendSuccess(() -> Component.literal("§7World: §e" + 
                targetPlayer.level().dimension().location().getPath()), false);
            context.getSource().sendSuccess(() -> Component.literal("§7Ping: §e" + targetPlayer.connection.latency() + "ms"), false);
            
            // Test permission checking
            try {
                com.zerog.neoessentials.permissions.CustomPermissionsManager permMgr = 
                    com.zerog.neoessentials.permissions.CustomPermissionsManager.getInstance();
                    
                boolean hasOwnerPerm = permMgr.hasPermission(targetPlayer, "neoessentials.scoreboard.owner");
                boolean hasAdminPerm = permMgr.hasPermission(targetPlayer, "neoessentials.scoreboard.admin");
                boolean hasStaffPerm = permMgr.hasPermission(targetPlayer, "neoessentials.scoreboard.staff");
                
                context.getSource().sendSuccess(() -> Component.literal("§7Permissions:"), false);
                context.getSource().sendSuccess(() -> Component.literal("§7  Owner: " + (hasOwnerPerm ? "§aYes" : "§cNo")), false);
                context.getSource().sendSuccess(() -> Component.literal("§7  Admin: " + (hasAdminPerm ? "§aYes" : "§cNo")), false);
                context.getSource().sendSuccess(() -> Component.literal("§7  Staff: " + (hasStaffPerm ? "§aYes" : "§cNo")), false);
            } catch (Exception e) {
                context.getSource().sendSuccess(() -> Component.literal("§7Permissions: §cError checking"), false);
            }
            
            context.getSource().sendSuccess(() -> Component.literal("§7━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"), false);
            
            return 1;
            
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§c[ScoreboardTest] Error getting player info: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Toggle scoreboard for testing
     */
    private static int toggleTest(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "target");
            ScoreboardManager scoreboardMgr = ScoreboardManager.getInstance();
            
            if (scoreboardMgr == null) {
                context.getSource().sendFailure(Component.literal("§c[ScoreboardTest] Scoreboard system not initialized"));
                return 0;
            }
            
            String playerName = targetPlayer.getName().getString();
            context.getSource().sendSuccess(() -> Component.literal("§e[ScoreboardTest] Toggling scoreboard for " + playerName + "..."), false);
            
            scoreboardMgr.toggleScoreboard(targetPlayer);
            
            context.getSource().sendSuccess(() -> Component.literal("§a[ScoreboardTest] Scoreboard toggled for " + playerName), false);
            context.getSource().sendSuccess(() -> Component.literal("§7[ScoreboardTest] Player should see change immediately"), false);
            
            DebugUtil.debugLog("[ScoreboardTest] Toggled scoreboard for " + playerName + " by " + context.getSource().getTextName());
            return 1;
            
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§c[ScoreboardTest] Error toggling scoreboard: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Show system information
     */
    private static int showSystemInfo(CommandContext<CommandSourceStack> context) {
        try {
            context.getSource().sendSuccess(() -> Component.literal("§6[ScoreboardTest] NeoEssentials Professional Scoreboard System"), false);
            context.getSource().sendSuccess(() -> Component.literal("§7━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"), false);
            
            context.getSource().sendSuccess(() -> Component.literal("§7System Type: §aProfessional Minecraft Scoreboard"), false);
            context.getSource().sendSuccess(() -> Component.literal("§7Features: §ePermission-based layouts, animations, placeholders"), false);
            context.getSource().sendSuccess(() -> Component.literal("§7Config File: §econfig/neoessentials/scoreboard.json"), false);
            context.getSource().sendSuccess(() -> Component.literal("§7Compatibility: §eLike FeatherBoard, ScoreBoard-r, TAB plugin"), false);
            
            context.getSource().sendSuccess(() -> Component.literal("§7Available Commands:"), false);
            context.getSource().sendSuccess(() -> Component.literal("§7  /scoreboard reload §8- Reload configuration"), false);
            context.getSource().sendSuccess(() -> Component.literal("§7  /scoreboard toggle [player] §8- Toggle scoreboard"), false);
            context.getSource().sendSuccess(() -> Component.literal("§7  /scoreboard status §8- Show system status"), false);
            context.getSource().sendSuccess(() -> Component.literal("§7  /scoreboard update [player] §8- Force update"), false);
            context.getSource().sendSuccess(() -> Component.literal("§7  /scoreboardtest §8- Advanced testing commands"), false);
            
            context.getSource().sendSuccess(() -> Component.literal("§7━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"), false);
            
            return 1;
            
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§c[ScoreboardTest] Error showing system info: " + e.getMessage()));
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
            DebugUtil.debugLog("[ScoreboardTest] Error checking permission: " + e.getMessage());
            return false;
        }
    }
}
