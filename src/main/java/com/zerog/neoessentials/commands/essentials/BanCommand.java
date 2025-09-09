package com.zerog.neoessentials.commands.essentials;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.UserBanListEntry;

import java.util.Date;

/**
 * Ban command implementation - /ban
 * Bans players from the server with optional reason
 */
public class BanCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /ban <player> [reason] - Ban a player from the server
        dispatcher.register(Commands.literal("ban")
            .requires(source -> PermissionUtil.hasPermission(source, PermissionNodes.BAN))
            .then(Commands.argument("player", GameProfileArgument.gameProfile())
                .executes(ctx -> banPlayer(ctx, "Banned by an operator"))
                .then(Commands.argument("reason", StringArgumentType.greedyString())
                    .executes(ctx -> banPlayer(ctx, StringArgumentType.getString(ctx, "reason")))
                )
            )
        );
        
        // /unban <player> - Unban a player
        dispatcher.register(Commands.literal("unban")
            .requires(source -> PermissionUtil.hasPermission(source, PermissionNodes.UNBAN))
            .then(Commands.argument("player", GameProfileArgument.gameProfile())
                .executes(ctx -> unbanPlayer(ctx))
            )
        );
        
        // /pardon - Alias for unban
        dispatcher.register(Commands.literal("pardon")
            .requires(source -> PermissionUtil.hasPermission(source, PermissionNodes.UNBAN))
            .then(Commands.argument("player", GameProfileArgument.gameProfile())
                .executes(ctx -> unbanPlayer(ctx))
            )
        );
    }
    
    private static int banPlayer(CommandContext<CommandSourceStack> context, String reason) throws CommandSyntaxException {
        var gameProfiles = GameProfileArgument.getGameProfiles(context, "player");
        
        if (gameProfiles.isEmpty()) {
            context.getSource().sendFailure(MessageUtil.translatable("neoessentials.player.not_found"));
            return 0;
        }
        
        var gameProfile = gameProfiles.iterator().next();
        String playerName = gameProfile.getName();
        
        // Check if the executor is trying to ban themselves
        try {
            ServerPlayer executor = context.getSource().getPlayerOrException();
            if (executor.getGameProfile().getId().equals(gameProfile.getId())) {
                context.getSource().sendFailure(MessageUtil.translatable("neoessentials.ban.cannot_self"));
                return 0;
            }
        } catch (CommandSyntaxException e) {
            // Command executed from console, which is fine
        }
        
        // Check if player is already banned
        if (context.getSource().getServer().getPlayerList().getBans().isBanned(gameProfile)) {
            context.getSource().sendFailure(MessageUtil.translatable("neoessentials.ban.already_banned", playerName));
            return 0;
        }
        
        // Create ban entry
        UserBanListEntry banEntry = new UserBanListEntry(
            gameProfile,
            new Date(),
            context.getSource().getTextName(),
            null, // No expiration (permanent ban)
            reason
        );
        
        // Add to ban list
        context.getSource().getServer().getPlayerList().getBans().add(banEntry);
        
        // Kick player if online
        ServerPlayer onlinePlayer = context.getSource().getServer().getPlayerList().getPlayer(gameProfile.getId());
        if (onlinePlayer != null) {
            onlinePlayer.connection.disconnect(MessageUtil.translatable(onlinePlayer, "neoessentials.ban.disconnect_message", reason));
        }
        
        // Broadcast to server
        context.getSource().getServer().getPlayerList().broadcastSystemMessage(
            MessageUtil.translatable("neoessentials.ban.broadcast", playerName), 
            false
        );
        
        // Send confirmation to executor
        context.getSource().sendSuccess(() -> MessageUtil.translatable("neoessentials.ban.success", playerName), true);
        context.getSource().sendSuccess(() -> MessageUtil.translatable("neoessentials.ban.reason", reason), false);
        
        return 1;
    }
    
    private static int unbanPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var gameProfiles = GameProfileArgument.getGameProfiles(context, "player");
        
        if (gameProfiles.isEmpty()) {
            context.getSource().sendFailure(MessageUtil.translatable("neoessentials.player.not_found"));
            return 0;
        }
        
        var gameProfile = gameProfiles.iterator().next();
        String playerName = gameProfile.getName();
        
        // Check if player is banned
        if (!context.getSource().getServer().getPlayerList().getBans().isBanned(gameProfile)) {
            context.getSource().sendFailure(MessageUtil.translatable("neoessentials.ban.not_banned", playerName));
            return 0;
        }
        
        // Remove from ban list
        context.getSource().getServer().getPlayerList().getBans().remove(gameProfile);
        
        // Send confirmation to executor
        context.getSource().sendSuccess(() -> MessageUtil.translatable("neoessentials.ban.unban_success", playerName), true);
        
        return 1;
    }
}
