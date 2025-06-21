package com.zerog.neoessentials.utils;

import com.zerog.neoessentials.NeoEssentials;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Manages teleport history for players
 */
public class TeleportHistory {
    // Map of player UUID to their teleport history stack
    private static final Map<UUID, Deque<TeleportLocation>> teleportHistory = new HashMap<>();
    
    // Maximum number of locations to remember per player
    private static final int MAX_HISTORY_SIZE = 10;
    
    /**
     * Records a player's position before teleporting
     * 
     * @param player The player being teleported
     */
    public static void recordPosition(ServerPlayer player) {
        if (player == null) return;
        
        UUID playerUuid = player.getUUID();
        ServerLevel level = player.serverLevel();
        
        // Create a new teleport location
        TeleportLocation location = new TeleportLocation(
            level.dimension().location().toString(),
            player.getX(),
            player.getY(),
            player.getZ(),
            player.getYRot(),
            player.getXRot()
        );
        
        // Get or create the history stack for this player
        Deque<TeleportLocation> history = teleportHistory.computeIfAbsent(playerUuid, k -> new ArrayDeque<>());
        
        // Add the location to the history stack
        history.push(location);
        
        // Trim the history if it's too large
        while (history.size() > MAX_HISTORY_SIZE) {
            history.pollLast();
        }
    }
    
    /**
     * Teleports a player back to their previous location
     * 
     * @param player The player to teleport
     * @return True if teleport was successful, false otherwise
     */
    public static boolean teleportBack(ServerPlayer player) {
        if (player == null) return false;
        
        UUID playerUuid = player.getUUID();
        Deque<TeleportLocation> history = teleportHistory.get(playerUuid);
        
        // Check if the player has a history
        if (history == null || history.isEmpty()) {
            return false;
        }
        
        // Get the last location
        TeleportLocation lastLocation = history.pop();
        
        // Find the dimension
        ServerLevel targetLevel = null;
        for (ServerLevel level : player.getServer().getAllLevels()) {
            if (level.dimension().location().toString().equals(lastLocation.dimension)) {
                targetLevel = level;
                break;
            }
        }
        
        if (targetLevel == null) {
            NeoEssentials.LOGGER.error("Could not find dimension for teleport history: {}", lastLocation.dimension);
            return false;
        }
        
        // Teleport the player
        return TeleportUtil.teleport(player, targetLevel, 
            lastLocation.x, lastLocation.y, lastLocation.z, 
            lastLocation.yaw, lastLocation.pitch);
    }
    
    /**
     * Class to store teleport location data
     */
    private static class TeleportLocation {
        private final String dimension;
        private final double x;
        private final double y;
        private final double z;
        private final float yaw;
        private final float pitch;
        
        public TeleportLocation(String dimension, double x, double y, double z, float yaw, float pitch) {
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }
}
