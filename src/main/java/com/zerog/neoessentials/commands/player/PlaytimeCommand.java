package com.zerog.neoessentials.commands.player;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.player.PlayerData;
import com.zerog.neoessentials.player.PlayerDataManager;
import com.zerog.neoessentials.player.PlaytimeTracker;
import com.zerog.neoessentials.util.MessageUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

/**
 * Command for viewing playtime statistics
 * Supports viewing own playtime or other players' playtime (with permission)
 */
public class PlaytimeCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlaytimeCommand.class);
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("playtime")
            .executes(PlaytimeCommand::getOwnPlaytime)
            .then(Commands.argument("player", EntityArgument.player())
                .requires(source -> source.hasPermission(2))
                .executes(PlaytimeCommand::getPlayerPlaytime))
            .then(Commands.literal("top")
                .requires(source -> source.hasPermission(1))
                .executes(PlaytimeCommand::getTopPlaytime))
            .then(Commands.literal("session")
                .executes(PlaytimeCommand::getSessionPlaytime))
        );
    }
    
    private static int getOwnPlaytime(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        return showPlaytime(context.getSource(), player);
    }
    
    private static int getPlayerPlaytime(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
        return showPlaytime(context.getSource(), targetPlayer);
    }
    
    private static int getSessionPlaytime(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        PlayerData playerData = PlayerDataManager.getInstance().getPlayerData(player.getUUID());
        long sessionTime = PlaytimeTracker.getInstance().getCurrentSessionTime(player.getUUID());
        
        Component message = MessageUtils.format("&6Your current session: &e" + 
            PlaytimeTracker.formatTime(sessionTime));
        
        context.getSource().sendSuccess(() -> message, false);
        return 1;
    }
    
    private static int getTopPlaytime(CommandContext<CommandSourceStack> context) {
        try {
            List<PlayerData> topPlayers = PlayerDataManager.getInstance().getTopPlayersByPlaytime(10);
            
            if (topPlayers.isEmpty()) {
                Component message = MessageUtils.format("&cNo playtime data available.");
                context.getSource().sendSuccess(() -> message, false);
                return 1;
            }
            
            Component header = MessageUtils.format("&6&l=== Top 10 Playtime ===");
            context.getSource().sendSuccess(() -> header, false);
            
            for (int i = 0; i < topPlayers.size(); i++) {
                PlayerData data = topPlayers.get(i);
                String rank = String.valueOf(i + 1);
                String playerName = data.getLastKnownName() != null ? data.getLastKnownName() : "Unknown";
                String playtime = PlaytimeTracker.formatTime(data.getTotalPlaytime());
                
                Component rankMessage = MessageUtils.format(
                    "&e" + rank + ". &f" + playerName + " &7- &a" + playtime
                );
                context.getSource().sendSuccess(() -> rankMessage, false);
            }
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error getting top playtime", e);
            Component error = MessageUtils.format("&cError retrieving playtime data.");
            context.getSource().sendSuccess(() -> error, false);
            return 0;
        }
    }
    
    private static int showPlaytime(CommandSourceStack source, ServerPlayer targetPlayer) {
        try {
            PlayerData playerData = PlayerDataManager.getInstance().getPlayerData(targetPlayer.getUUID());
            long sessionTime = PlaytimeTracker.getInstance().getCurrentSessionTime(targetPlayer.getUUID());
            
            boolean isOwnPlaytime = source.getEntity() instanceof ServerPlayer player && 
                player.getUUID().equals(targetPlayer.getUUID());
            
            String targetName = isOwnPlaytime ? "Your" : targetPlayer.getName().getString() + "'s";
            
            Component header = MessageUtils.format("&6&l=== " + targetName + " Playtime ===");
            source.sendSuccess(() -> header, false);
            
            Component totalTime = MessageUtils.format("&eTotal Playtime: &a" + 
                PlaytimeTracker.formatTime(playerData.getTotalPlaytime()));
            source.sendSuccess(() -> totalTime, false);
            
            Component currentSession = MessageUtils.format("&eCurrent Session: &a" + 
                PlaytimeTracker.formatTime(sessionTime));
            source.sendSuccess(() -> currentSession, false);
            
            if (playerData.getFirstJoin() > 0) {
                Component firstJoin = MessageUtils.format("&eFirst Join: &7" + 
                    MessageUtils.formatTimestamp(playerData.getFirstJoin()));
                source.sendSuccess(() -> firstJoin, false);
            }
            
            if (playerData.getLastSeen() > 0) {
                Component lastSeen = MessageUtils.format("&eLast Seen: &7" + 
                    MessageUtils.formatTimestamp(playerData.getLastSeen()));
                source.sendSuccess(() -> lastSeen, false);
            }
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error showing playtime for player " + targetPlayer.getName().getString(), e);
            Component error = MessageUtils.format("&cError retrieving playtime data.");
            source.sendSuccess(() -> error, false);
            return 0;
        }
    }
}
