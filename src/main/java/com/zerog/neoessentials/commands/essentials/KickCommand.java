package com.zerog.neoessentials.commands.essentials;

import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Kick command implementation - /kick
 * Kicks players from the server with optional reason
 */
public class KickCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /kick <player> [reason] - Kick a player from the server
        dispatcher.register(Commands.literal("kick")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.ADMIN_BASIC))
            .then(Commands.argument("player", StringArgumentType.word())
                .executes(ctx -> kickPlayerWithReason(ctx, "Kicked by an operator"))
                .then(Commands.argument("reason", StringArgumentType.greedyString())
                    .executes(ctx -> kickPlayerWithReason(ctx, StringArgumentType.getString(ctx, "reason")))
                )
            )
        );
    }
    
    private static int kickPlayerWithReason(CommandContext<CommandSourceStack> context, String reason) throws CommandSyntaxException {
        String playerName = StringArgumentType.getString(context, "player");
        ServerPlayer targetPlayer = context.getSource().getServer().getPlayerList().getPlayerByName(playerName);
        
        if (targetPlayer == null) {
            context.getSource().sendFailure(Component.literal("Player '" + playerName + "' not found or not online"));
            return 0;
        }
        
        return kickPlayer(context, targetPlayer, reason);
    }
    
    private static int kickPlayer(CommandContext<CommandSourceStack> context, ServerPlayer targetPlayer, String reason) throws CommandSyntaxException {
        if (targetPlayer == null) {
            context.getSource().sendFailure(MessageUtil.translatable("neoessentials.kick.player_not_found"));
            return 0;
        }
        
        // Check if the executor is trying to kick themselves
        try {
            ServerPlayer executor = context.getSource().getPlayerOrException();
            if (executor.getUUID().equals(targetPlayer.getUUID())) {
                context.getSource().sendFailure(MessageUtil.translatable("neoessentials.kick.cannot_self"));
                return 0;
            }
        } catch (CommandSyntaxException e) {
            // Command executed from console, which is fine
        }
        
        // Check if target has higher permissions (skip this check - let server ops handle it)
        // This is a simplified implementation - in production you'd want more sophisticated permission checking
        
        String playerName = targetPlayer.getName().getString();
        
        // Kick the player - use kick method on playerlist
        context.getSource().getServer().getPlayerList().remove(targetPlayer);
        
        // Broadcast to server
        context.getSource().getServer().getPlayerList().broadcastSystemMessage(
            MessageUtil.translatable("neoessentials.kick.broadcast", playerName), 
            false
        );
        
    // Send confirmation to executor
    context.getSource().sendSuccess(() -> MessageUtil.translatable("neoessentials.kick.success", playerName), true);
    context.getSource().sendSuccess(() -> MessageUtil.translatable("neoessentials.kick.reason", reason), false);
        
        return 1;
    }
}
