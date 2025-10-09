package com.zerog.neoessentials.chat;

import net.minecraft.server.level.ServerPlayer;
import java.util.Set;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe manager for player ignore lists.
 * Handles ignoring/unignoring players and message filtering.
 */
public class IgnoreManager {
    // Thread-safe storage for ignore relationships
    private static final Map<String, Set<String>> ignoreMap = new ConcurrentHashMap<>();

    public static void ignore(ServerPlayer player, String targetName) {
        String playerName = player.getName().getString().toLowerCase();
        ignoreMap.computeIfAbsent(playerName, k -> ConcurrentHashMap.newKeySet()).add(targetName.toLowerCase());
    }

    public static void unignore(ServerPlayer player, String targetName) {
        String playerName = player.getName().getString().toLowerCase();
        Set<String> ignored = ignoreMap.get(playerName);
        if (ignored != null) {
            ignored.remove(targetName.toLowerCase());
            if (ignored.isEmpty()) {
                ignoreMap.remove(playerName);
            }
        }
    }

    public static boolean isIgnoring(ServerPlayer player, ServerPlayer target) {
        String playerName = player.getName().getString().toLowerCase();
        String targetName = target.getName().getString().toLowerCase();
        Set<String> ignored = ignoreMap.get(playerName);
        return ignored != null && ignored.contains(targetName);
    }
    
    /**
     * Check if a player is ignoring another player by name
     */
    public static boolean isIgnoring(ServerPlayer player, String targetName) {
        String playerName = player.getName().getString().toLowerCase();
        Set<String> ignored = ignoreMap.get(playerName);
        return ignored != null && ignored.contains(targetName.toLowerCase());
    }
    
    /**
     * Get the ignore list for a player
     */
    public static Set<String> getIgnoreList(ServerPlayer player) {
        String playerName = player.getName().getString().toLowerCase();
        Set<String> ignored = ignoreMap.get(playerName);
        return ignored != null ? Set.copyOf(ignored) : Set.of();
    }
    
    /**
     * Clean up ignore data when a player disconnects
     */
    public static void cleanupPlayer(ServerPlayer player) {
        String playerName = player.getName().getString().toLowerCase();
        
        // Remove this player's ignore list
        ignoreMap.remove(playerName);
        
        // Remove this player from all other players' ignore lists
        ignoreMap.values().forEach(ignoreSet -> ignoreSet.remove(playerName));
        
        // Clean up empty ignore lists
        ignoreMap.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }
}
