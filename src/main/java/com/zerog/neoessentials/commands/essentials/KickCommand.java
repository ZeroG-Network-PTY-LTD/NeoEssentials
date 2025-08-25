package com.zerog.neoessentials.commands.essentials;

import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
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
            .then(Commands.argument("player", EntityArgument.player())
                .executes(ctx -> kickPlayer(ctx, EntityArgument.getPlayer(ctx, "player"), "Kicked by an operator"))
                .then(Commands.argument("reason", StringArgumentType.greedyString())
                    .executes(ctx -> kickPlayer(ctx, EntityArgument.getPlayer(ctx, "player"), StringArgumentType.getString(ctx, "reason")))
                )
            )
        );
    }
    
    private static int kickPlayer(CommandContext<CommandSourceStack> context, ServerPlayer targetPlayer, String reason) throws CommandSyntaxException {
        if (targetPlayer == null) {
            context.getSource().sendFailure(Component.translatable("neoessentials.kick.player_not_found"));
            return 0;
        }
        
        // Check if the executor is trying to kick themselves
        try {
            ServerPlayer executor = context.getSource().getPlayerOrException();
            if (executor.getUUID().equals(targetPlayer.getUUID())) {
                context.getSource().sendFailure(Component.translatable("neoessentials.kick.cannot_self"));
                return 0;
            }
        } catch (CommandSyntaxException e) {
            // Command executed from console, which is fine
        }
        
        // Check if target has higher permissions (skip this check - let server ops handle it)
        // This is a simplified implementation - in production you'd want more sophisticated permission checking
        
        String playerName = targetPlayer.getName().getString();
        
    // Kick the player
    targetPlayer.connection.disconnect(Component.translatable("neoessentials.kick.kicked", reason));
        
        // Broadcast to server
        context.getSource().getServer().getPlayerList().broadcastSystemMessage(
            Component.translatable("neoessentials.kick.broadcast", playerName), 
            false
        );
        
    // Send confirmation to executor
    context.getSource().sendSuccess(() -> Component.translatable("neoessentials.kick.success", playerName), true);
    context.getSource().sendSuccess(() -> Component.translatable("neoessentials.kick.reason", reason), false);
        
        return 1;
    }
}
