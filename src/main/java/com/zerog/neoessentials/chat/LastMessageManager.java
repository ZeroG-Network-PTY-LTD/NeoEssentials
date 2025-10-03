package com.zerog.neoessentials.chat;

import net.minecraft.server.level.ServerPlayer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks the last player who messaged each player for /reply functionality.
 * Includes cleanup functionality for offline players.
 */
public class LastMessageManager {
    private static final Map<String, String> lastMessagerMap = new ConcurrentHashMap<>();

    /**
     * Set the last messager for a recipient
     */
    public static void setLastMessager(ServerPlayer recipient, ServerPlayer sender) {
        if (recipient == null || sender == null) return;
        lastMessagerMap.put(recipient.getName().getString().toLowerCase(), sender.getName().getString().toLowerCase());
    }

    /**
     * Get the last player who messaged the given player
     */
    public static ServerPlayer getLastMessager(ServerPlayer player) {
        if (player == null || player.getServer() == null) return null;
        
        String last = lastMessagerMap.get(player.getName().getString().toLowerCase());
        if (last == null) return null;
        
        // Find the player on the server
        for (ServerPlayer p : player.getServer().getPlayerList().getPlayers()) {
            if (p.getName().getString().equalsIgnoreCase(last)) {
                return p;
            }
        }
        
        // Player not found online - clean up the entry
        lastMessagerMap.remove(player.getName().getString().toLowerCase());
        return null;
    }
    
    /**
     * Remove a player from all message tracking when they leave
     */
    public static void cleanupPlayer(ServerPlayer player) {
        if (player == null) return;
        String playerName = player.getName().getString().toLowerCase();
        
        // Remove this player as a recipient
        lastMessagerMap.remove(playerName);
        
        // Remove this player as a sender from other players' records
        lastMessagerMap.entrySet().removeIf(entry -> entry.getValue().equals(playerName));
    }
    
    /**
     * Check if a player has someone to reply to
     */
    public static boolean hasReplyTarget(ServerPlayer player) {
        if (player == null) return false;
        return getLastMessager(player) != null;
    }
}
