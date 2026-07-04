package com.zerog.neoessentials.chat;

import net.minecraft.server.level.ServerPlayer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.zerog.neoessentials.util.ChatDebugUtil;

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
        String recipientName = recipient.getName().getString().toLowerCase();
        String senderName = sender.getName().getString().toLowerCase();
        lastMessagerMap.put(recipientName, senderName);
        ChatDebugUtil.debug("LastMessageManager - Stored: %s -> %s, Map size: %d", recipientName, senderName, lastMessagerMap.size());
        ChatDebugUtil.debug("LastMessageManager - Current map: %s", lastMessagerMap);
    }

    /**
     * Get the last player who messaged the given player
     */
    public static ServerPlayer getLastMessager(ServerPlayer player) {
        if (player == null || player.level().getServer() == null) {
            ChatDebugUtil.debug("LastMessageManager - getLastMessager called with null player or server");
            return null;
        }
        
        String playerName = player.getName().getString().toLowerCase();
        String lastMessagerName = lastMessagerMap.get(playerName);
        ChatDebugUtil.debug("LastMessageManager - Looking up: %s -> %s", playerName, lastMessagerName);
        ChatDebugUtil.debug("LastMessageManager - Current map: %s", lastMessagerMap);
        
        if (lastMessagerName == null || lastMessagerName.isEmpty()) {
            ChatDebugUtil.debug("LastMessageManager - No last messager found for %s", playerName);
            return null;
        }
        
        // Use more efficient player lookup by name
        ServerPlayer target = player.level().getServer().getPlayerList().getPlayerByName(lastMessagerName);
        ChatDebugUtil.debug("LastMessageManager - Player lookup for %s: %s", lastMessagerName, (target != null ? "found" : "not found"));
        
        if (target != null && target.connection != null) {
            ChatDebugUtil.debug("LastMessageManager - Returning valid target: %s", target.getName().getString());
            return target;
        }
        
        // Player not found online or disconnected - clean up the entry
        ChatDebugUtil.debug("LastMessageManager - Cleaning up offline player: %s", lastMessagerName);
        lastMessagerMap.remove(playerName);
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
    
    /**
     * Debug method to get the current size of the message map
     */
    public static int getMessageMapSize() {
        return lastMessagerMap.size();
    }
    
    /**
     * Debug method to check if a player has an entry in the message map
     */
    public static boolean hasMessageEntry(ServerPlayer player) {
        if (player == null) return false;
        return lastMessagerMap.containsKey(player.getName().getString().toLowerCase());
    }
}
