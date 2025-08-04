package com.zerog.neoessentials.commands.essentials;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;

import java.util.List;

/**
 * List command implementation for NeoEssentials
 * Shows online players with additional information
 * 
 * Commands:
 * - /list - Show online players
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class ListCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("list")
            .executes(ListCommand::listPlayers));
    }
    
    /**
     * Execute /list command to show online players
     */
    private static int listPlayers(CommandContext<CommandSourceStack> context) {
        PlayerList playerList = context.getSource().getServer().getPlayerList();
        List<ServerPlayer> players = playerList.getPlayers();
        
        int playerCount = players.size();
        int maxPlayers = playerList.getMaxPlayers();
        
        // Send header with player count
        Component header = Component.literal(String.format("§6=== Online Players (%d/%d) ===", playerCount, maxPlayers));
        context.getSource().sendSuccess(() -> header, false);
        
        if (playerCount == 0) {
            context.getSource().sendSuccess(() -> Component.literal("§7No players are currently online."), false);
            return 1;
        }
        
        // Group players by permission level for better display
        StringBuilder adminList = new StringBuilder();
        StringBuilder modList = new StringBuilder();
        StringBuilder playersList = new StringBuilder();
        
        for (ServerPlayer player : players) {
            String displayName = getPlayerDisplayInfo(player);
            
            if (player.hasPermissions(4)) {
                // Admin level
                if (adminList.length() > 0) adminList.append("§7, ");
                adminList.append("§c").append(displayName);
            } else if (player.hasPermissions(2)) {
                // Moderator level
                if (modList.length() > 0) modList.append("§7, ");
                modList.append("§6").append(displayName);
            } else {
                // Regular player
                if (playersList.length() > 0) playersList.append("§7, ");
                playersList.append("§a").append(displayName);
            }
        }
        
        // Display players by group
        if (adminList.length() > 0) {
            context.getSource().sendSuccess(() -> Component.literal("§cAdmins: " + adminList.toString()), false);
        }
        
        if (modList.length() > 0) {
            context.getSource().sendSuccess(() -> Component.literal("§6Moderators: " + modList.toString()), false);
        }
        
        if (playersList.length() > 0) {
            context.getSource().sendSuccess(() -> Component.literal("§aPlayers: " + playersList.toString()), false);
        }
        
        return 1;
    }
    
    /**
     * Get display information for a player
     */
    private static String getPlayerDisplayInfo(ServerPlayer player) {
        StringBuilder info = new StringBuilder();
        info.append(player.getName().getString());
        
        // Add additional info indicators
        if (player.isCreative()) {
            info.append(" §7[C]"); // Creative mode
        } else if (player.isSpectator()) {
            info.append(" §7[S]"); // Spectator mode
        }
        
        if (player.getAbilities().invulnerable) {
            info.append(" §7[G]"); // God mode
        }
        
        if (player.isInvisible()) {
            info.append(" §7[V]"); // Vanished
        }
        
        return info.toString();
    }
}
