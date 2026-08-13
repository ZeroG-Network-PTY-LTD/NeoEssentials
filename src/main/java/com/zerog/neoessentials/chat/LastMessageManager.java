package com.zerog.neoessentials.chat;

import net.minecraft.server.level.ServerPlayer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks the last player who messaged each player for /reply functionality.
 * Includes cleanup functionality for offline players.
 */
public class LastMessageManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(LastMessageManager.class);
    private static final Map<String, String> lastMessagerMap = new ConcurrentHashMap<>();

    /**
     * Set the last messager for a recipient
     */
    public static void setLastMessager(ServerPlayer recipient, ServerPlayer sender) {
        if (recipient == null || sender == null) return;
        String recipientName = recipient.getName().getString().toLowerCase();
        String senderName = sender.getName().getString().toLowerCase();
        lastMessagerMap.put(recipientName, senderName);
        NeoLog.debug(LOGGER, LogCategory.CHAT, "LastMessageManager - Stored: {} -> {}, Map size: {}", recipientName, senderName, lastMessagerMap.size());
        NeoLog.debug(LOGGER, LogCategory.CHAT, "LastMessageManager - Current map: {}", lastMessagerMap);
    }

    /**
     * Get the last player who messaged the given player
     */
    public static ServerPlayer getLastMessager(ServerPlayer player) {
        if (player == null || player.getServer() == null) {
            NeoLog.debug(LOGGER, LogCategory.CHAT, "LastMessageManager - getLastMessager called with null player or server");
            return null;
        }
        
        String playerName = player.getName().getString().toLowerCase();
        String lastMessagerName = lastMessagerMap.get(playerName);
        NeoLog.debug(LOGGER, LogCategory.CHAT, "LastMessageManager - Looking up: {} -> {}", playerName, lastMessagerName);
        NeoLog.debug(LOGGER, LogCategory.CHAT, "LastMessageManager - Current map: {}", lastMessagerMap);
        
        if (lastMessagerName == null || lastMessagerName.isEmpty()) {
            NeoLog.debug(LOGGER, LogCategory.CHAT, "LastMessageManager - No last messager found for {}", playerName);
            return null;
        }
        
        // Use more efficient player lookup by name
        ServerPlayer target = player.getServer().getPlayerList().getPlayerByName(lastMessagerName);
        NeoLog.debug(LOGGER, LogCategory.CHAT, "LastMessageManager - Player lookup for {}: {}", lastMessagerName, (target != null ? "found" : "not found"));
        
        if (target != null && target.connection != null) {
            NeoLog.debug(LOGGER, LogCategory.CHAT, "LastMessageManager - Returning valid target: {}", target.getName().getString());
            return target;
        }
        
        // Player not found online or disconnected - clean up the entry
        NeoLog.debug(LOGGER, LogCategory.CHAT, "LastMessageManager - Cleaning up offline player: {}", lastMessagerName);
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
