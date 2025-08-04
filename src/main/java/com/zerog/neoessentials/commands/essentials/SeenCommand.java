package com.zerog.neoessentials.commands.essentials;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.time.Duration;

/**
 * Seen command implementation for NeoEssentials
 * Shows when a player was last seen online
 * 
 * Commands:
 * - /seen <player> - Check when a player was last online
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class SeenCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("seen")
            .requires(source -> source.hasPermission(1))
            .then(Commands.argument("player", StringArgumentType.word())
                .suggests((context, builder) -> {
                    // Suggest all players (online and offline from user cache)
                    return SharedSuggestionProvider.suggest(
                        context.getSource().getServer().getPlayerList().getPlayers()
                            .stream().map(p -> p.getName().getString()), 
                        builder
                    );
                })
                .executes(SeenCommand::checkPlayerSeen)));
    }
    
    /**
     * Execute /seen <player> command to check when player was last online
     */
    private static int checkPlayerSeen(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String playerName = StringArgumentType.getString(context, "player");
        
        // Check if player is currently online
        ServerPlayer onlinePlayer = context.getSource().getServer().getPlayerList().getPlayerByName(playerName);
        
        if (onlinePlayer != null) {
            // Player is online
            context.getSource().sendSuccess(() -> Component.literal("§a" + playerName + " is currently online!"), false);
            context.getSource().sendSuccess(() -> Component.literal("§7Location: §b" + onlinePlayer.level().dimension().location()), false);
            context.getSource().sendSuccess(() -> Component.literal("§7Game Mode: §6" + onlinePlayer.gameMode.getGameModeForPlayer().getName()), false);
            
            return 1;
        }
        
        // Player is not online - try to get info from user cache
        // This is a simplified implementation. In a production environment,
        // you'd want to store last seen times in a database or persistent storage
        
        var userCache = context.getSource().getServer().getProfileCache();
        if (userCache != null) {
            var gameProfile = userCache.get(playerName);
            
            if (gameProfile.isPresent()) {
                context.getSource().sendSuccess(() -> Component.literal("§c" + playerName + " is currently offline."), false);
                context.getSource().sendSuccess(() -> Component.literal("§7UUID: §f" + gameProfile.get().getId()), false);
                context.getSource().sendSuccess(() -> Component.literal("§7Last seen data not available (requires database storage)."), false);
                context.getSource().sendSuccess(() -> Component.literal("§7This player has been on the server before."), false);
            } else {
                context.getSource().sendFailure(Component.literal("§cPlayer '" + playerName + "' has never been on this server!"));
                return 0;
            }
        } else {
            context.getSource().sendFailure(Component.literal("§cUnable to check player data - user cache not available."));
            return 0;
        }
        
        return 1;
    }
    
    /**
     * Format duration in a human-readable way
     */
    private static String formatDuration(long millis) {
        if (millis < 0) millis = 0;
        
        Duration duration = Duration.ofMillis(millis);
        
        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;
        
        StringBuilder result = new StringBuilder();
        
        if (days > 0) {
            result.append(days).append("d ");
        }
        if (hours > 0) {
            result.append(hours).append("h ");
        }
        if (minutes > 0) {
            result.append(minutes).append("m ");
        }
        if (seconds > 0 || result.length() == 0) {
            result.append(seconds).append("s");
        }
        
        return result.toString().trim();
    }
}
