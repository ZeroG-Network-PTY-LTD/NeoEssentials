package com.zerog.neoessentials.commands.essentials;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
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
            .requires(source -> source.hasPermission(3))
            .then(Commands.argument("player", GameProfileArgument.gameProfile())
                .executes(ctx -> banPlayer(ctx, "Banned by an operator"))
                .then(Commands.argument("reason", StringArgumentType.greedyString())
                    .executes(ctx -> banPlayer(ctx, StringArgumentType.getString(ctx, "reason")))
                )
            )
        );
        
        // /unban <player> - Unban a player
        dispatcher.register(Commands.literal("unban")
            .requires(source -> source.hasPermission(3))
            .then(Commands.argument("player", GameProfileArgument.gameProfile())
                .executes(ctx -> unbanPlayer(ctx))
            )
        );
        
        // /pardon - Alias for unban
        dispatcher.register(Commands.literal("pardon")
            .requires(source -> source.hasPermission(3))
            .then(Commands.argument("player", GameProfileArgument.gameProfile())
                .executes(ctx -> unbanPlayer(ctx))
            )
        );
    }
    
    private static int banPlayer(CommandContext<CommandSourceStack> context, String reason) throws CommandSyntaxException {
        var gameProfiles = GameProfileArgument.getGameProfiles(context, "player");
        
        if (gameProfiles.isEmpty()) {
            context.getSource().sendFailure(Component.literal("§cPlayer not found!"));
            return 0;
        }
        
        var gameProfile = gameProfiles.iterator().next();
        String playerName = gameProfile.getName();
        
        // Check if the executor is trying to ban themselves
        try {
            ServerPlayer executor = context.getSource().getPlayerOrException();
            if (executor.getGameProfile().getId().equals(gameProfile.getId())) {
                context.getSource().sendFailure(Component.literal("§cYou cannot ban yourself!"));
                return 0;
            }
        } catch (CommandSyntaxException e) {
            // Command executed from console, which is fine
        }
        
        // Check if player is already banned
        if (context.getSource().getServer().getPlayerList().getBans().isBanned(gameProfile)) {
            context.getSource().sendFailure(Component.literal("§c" + playerName + " is already banned!"));
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
            onlinePlayer.connection.disconnect(Component.literal("§cYou have been banned from this server\n§7Reason: §f" + reason));
        }
        
        // Broadcast to server
        context.getSource().getServer().getPlayerList().broadcastSystemMessage(
            Component.literal("§c" + playerName + " was banned from the server"), 
            false
        );
        
        // Send confirmation to executor
        context.getSource().sendSuccess(() -> Component.literal("§aBanned " + playerName + " from the server"), true);
        context.getSource().sendSuccess(() -> Component.literal("§7Reason: §f" + reason), false);
        
        return 1;
    }
    
    private static int unbanPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var gameProfiles = GameProfileArgument.getGameProfiles(context, "player");
        
        if (gameProfiles.isEmpty()) {
            context.getSource().sendFailure(Component.literal("§cPlayer not found!"));
            return 0;
        }
        
        var gameProfile = gameProfiles.iterator().next();
        String playerName = gameProfile.getName();
        
        // Check if player is banned
        if (!context.getSource().getServer().getPlayerList().getBans().isBanned(gameProfile)) {
            context.getSource().sendFailure(Component.literal("§c" + playerName + " is not banned!"));
            return 0;
        }
        
        // Remove from ban list
        context.getSource().getServer().getPlayerList().getBans().remove(gameProfile);
        
        // Send confirmation to executor
        context.getSource().sendSuccess(() -> Component.literal("§aUnbanned " + playerName), true);
        
        return 1;
    }
}
