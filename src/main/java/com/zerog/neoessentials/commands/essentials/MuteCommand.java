package com.zerog.neoessentials.commands.essentials;

import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mute command implementation for NeoEssentials
 * Provides player muting functionality with duration and reason support
 * 
 * Commands:
 * - /mute <player> [duration] [reason] - Mute a player
 * - /unmute <player> - Unmute a player
 * - /mutelist - List all muted players
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class MuteCommand {
    
    // In-memory storage for muted players
    // In production, this should be persisted to database or config
    private static final Map<UUID, MuteData> mutedPlayers = new ConcurrentHashMap<>();
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /mute <player> [duration] [reason] - Mute a player
        dispatcher.register(Commands.literal("mute")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.MODERATION_BASIC))
            .then(Commands.argument("player", EntityArgument.player())
                .executes(ctx -> mutePlayer(ctx, EntityArgument.getPlayer(ctx, "player"), 0, "Muted by an operator"))
                .then(Commands.argument("duration", StringArgumentType.word())
                    .executes(ctx -> mutePlayerWithDuration(ctx, EntityArgument.getPlayer(ctx, "player"), StringArgumentType.getString(ctx, "duration"), "Muted by an operator"))
                    .then(Commands.argument("reason", StringArgumentType.greedyString())
                        .executes(ctx -> mutePlayerWithDuration(ctx, EntityArgument.getPlayer(ctx, "player"), StringArgumentType.getString(ctx, "duration"), StringArgumentType.getString(ctx, "reason")))
                    )
                )
                .then(Commands.argument("reason", StringArgumentType.greedyString())
                    .executes(ctx -> mutePlayer(ctx, EntityArgument.getPlayer(ctx, "player"), 0, StringArgumentType.getString(ctx, "reason")))
                )
            )
        );
        
        // /unmute <player> - Unmute a player
        dispatcher.register(Commands.literal("unmute")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.MODERATION_BASIC))
            .then(Commands.argument("player", EntityArgument.player())
                .executes(ctx -> unmutePlayer(ctx, EntityArgument.getPlayer(ctx, "player")))
            )
        );
        
        // /mutelist - List all muted players
        dispatcher.register(Commands.literal("mutelist")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.MODERATION_BASIC))
            .executes(ctx -> listMutedPlayers(ctx))
        );
    }
    
    /**
     * Mute a player permanently
     */
    private static int mutePlayer(CommandContext<CommandSourceStack> context, ServerPlayer targetPlayer, long durationMinutes, String reason) throws CommandSyntaxException {
        if (targetPlayer == null) {
            context.getSource().sendFailure(Component.literal("§cPlayer not found!"));
            return 0;
        }
        
        // Check if the executor is trying to mute themselves
        try {
            ServerPlayer executor = context.getSource().getPlayerOrException();
            if (executor.getUUID().equals(targetPlayer.getUUID())) {
                context.getSource().sendFailure(Component.literal("§cYou cannot mute yourself!"));
                return 0;
            }
        } catch (CommandSyntaxException e) {
            // Command executed from console, which is fine
        }
        
        UUID playerId = targetPlayer.getUUID();
        String playerName = targetPlayer.getName().getString();
        
        // Check if player is already muted
        if (mutedPlayers.containsKey(playerId) && !isExpired(mutedPlayers.get(playerId))) {
            context.getSource().sendFailure(Component.literal("§c" + playerName + " is already muted!"));
            return 0;
        }
        
        // Calculate expiration time (0 = permanent)
        long expirationTime = durationMinutes > 0 ? System.currentTimeMillis() + (durationMinutes * 60 * 1000) : 0;
        
        // Create mute data
        MuteData muteData = new MuteData(
            reason,
            context.getSource().getTextName(),
            System.currentTimeMillis(),
            expirationTime
        );
        
        // Add to muted players
        mutedPlayers.put(playerId, muteData);
        
        // Notify the muted player
        String durationText = durationMinutes > 0 ? 
            " for " + MessageUtil.formatTime(durationMinutes * 60 * 1000) : " permanently";
        MessageUtil.sendMessage(targetPlayer, "&cYou have been muted" + durationText);
        MessageUtil.sendMessage(targetPlayer, "&7Reason: &f" + reason);
        
        // Send confirmation to executor
        context.getSource().sendSuccess(() -> Component.literal("§aMuted " + playerName + durationText), true);
        context.getSource().sendSuccess(() -> Component.literal("§7Reason: §f" + reason), false);
        
        return 1;
    }
    
    /**
     * Mute a player with duration parsing
     */
    private static int mutePlayerWithDuration(CommandContext<CommandSourceStack> context, ServerPlayer targetPlayer, String durationStr, String reason) throws CommandSyntaxException {
        long durationMinutes = parseDuration(durationStr);
        if (durationMinutes < 0) {
            context.getSource().sendFailure(Component.literal("§cInvalid duration format! Use: 5m, 1h, 2d, etc."));
            return 0;
        }
        
        return mutePlayer(context, targetPlayer, durationMinutes, reason);
    }
    
    /**
     * Unmute a player
     */
    private static int unmutePlayer(CommandContext<CommandSourceStack> context, ServerPlayer targetPlayer) throws CommandSyntaxException {
        if (targetPlayer == null) {
            context.getSource().sendFailure(Component.literal("§cPlayer not found!"));
            return 0;
        }
        
        UUID playerId = targetPlayer.getUUID();
        String playerName = targetPlayer.getName().getString();
        
        // Check if player is muted
        if (!mutedPlayers.containsKey(playerId) || isExpired(mutedPlayers.get(playerId))) {
            context.getSource().sendFailure(Component.literal("§c" + playerName + " is not muted!"));
            return 0;
        }
        
        // Remove from muted players
        mutedPlayers.remove(playerId);
        
        // Notify the unmuted player
        MessageUtil.sendMessage(targetPlayer, "&aYou have been unmuted!");
        
        // Send confirmation to executor
        context.getSource().sendSuccess(() -> Component.literal("§aUnmuted " + playerName), true);
        
        return 1;
    }
    
    /**
     * List all muted players
     */
    private static int listMutedPlayers(CommandContext<CommandSourceStack> context) {
        // Clean expired mutes first
        cleanupExpiredMutes();
        
        if (mutedPlayers.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("§aNo players are currently muted."), false);
            return 1;
        }
        
        context.getSource().sendSuccess(() -> Component.literal("§6=== Muted Players ==="), false);
        
        mutedPlayers.forEach((playerId, muteData) -> {
            // Get player name safely
            ServerPlayer onlinePlayer = context.getSource().getServer().getPlayerList().getPlayer(playerId);
            String playerName = onlinePlayer != null ? 
                onlinePlayer.getName().getString() : 
                "Unknown Player";
            
            String timeInfo = muteData.expirationTime > 0 ?
                "Expires in " + MessageUtil.formatTime(muteData.expirationTime - System.currentTimeMillis()) :
                "Permanent";
            
            context.getSource().sendSuccess(() -> Component.literal(
                "§7- §c" + playerName + " §7(" + timeInfo + ") - §f" + muteData.reason
            ), false);
        });
        
        return 1;
    }
    
    /**
     * Check if a player is currently muted
     */
    public static boolean isPlayerMuted(UUID playerId) {
        MuteData muteData = mutedPlayers.get(playerId);
        return muteData != null && !isExpired(muteData);
    }
    
    /**
     * Parse duration string (5m, 1h, 2d) into minutes
     */
    private static long parseDuration(String durationStr) {
        if (durationStr == null || durationStr.isEmpty()) {
            return 0; // Permanent
        }
        
        durationStr = durationStr.toLowerCase();
        
        try {
            if (durationStr.endsWith("m")) {
                return Long.parseLong(durationStr.substring(0, durationStr.length() - 1));
            } else if (durationStr.endsWith("h")) {
                return Long.parseLong(durationStr.substring(0, durationStr.length() - 1)) * 60;
            } else if (durationStr.endsWith("d")) {
                return Long.parseLong(durationStr.substring(0, durationStr.length() - 1)) * 60 * 24;
            } else {
                // Try to parse as plain minutes
                return Long.parseLong(durationStr);
            }
        } catch (NumberFormatException e) {
            return -1; // Invalid format
        }
    }
    
    /**
     * Check if a mute has expired
     */
    private static boolean isExpired(MuteData muteData) {
        return muteData.expirationTime > 0 && System.currentTimeMillis() > muteData.expirationTime;
    }
    
    /**
     * Clean up expired mutes
     */
    public static void cleanupExpiredMutes() {
        mutedPlayers.entrySet().removeIf(entry -> isExpired(entry.getValue()));
    }
    
    /**
     * Data class for mute information
     */
    private static class MuteData {
        final String reason;
        final long expirationTime; // 0 = permanent
        
        MuteData(String reason, String mutedBy, long muteTime, long expirationTime) {
            this.reason = reason;
            this.expirationTime = expirationTime;
            // mutedBy and muteTime are received but not stored for this simple implementation
        }
    }
}
