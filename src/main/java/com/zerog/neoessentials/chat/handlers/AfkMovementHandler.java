package com.zerog.neoessentials.chat.handlers;

import com.zerog.neoessentials.chat.AfkManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
// Server tick events not available - using alternative approach
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks player movement to detect activity for AFK system.
 * Uses position comparison to determine if a player has moved significantly.
 */
public class AfkMovementHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(AfkMovementHandler.class);
    
    // Minimum distance to consider as movement (in blocks)
    private static final double MOVEMENT_THRESHOLD = 0.1;
    
    // Store last known positions
    private static final Map<UUID, PlayerPosition> lastPositions = new HashMap<>();
    
    // Track movement checks per second (reduce frequency to avoid spam)
    private static int tickCounter = 0;
    private static final int CHECK_INTERVAL = 20; // Check every 20 ticks (1 second)
    
    /**
     * Movement tracking disabled - server tick events not available in this version
     * Movement will be tracked through other player interaction events
     */
    /*
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        // Movement tracking implementation when tick events become available
    }
    */
    
    /**
     * Check if a player has moved significantly
     */
    private static void checkPlayerMovement(ServerPlayer player) {
        UUID playerId = player.getUUID();
        PlayerPosition currentPos = new PlayerPosition(
            player.getX(), 
            player.getY(), 
            player.getZ(),
            player.getYRot(), // Y rotation (yaw)
            player.getXRot()  // X rotation (pitch)
        );
        
        PlayerPosition lastPos = lastPositions.get(playerId);
        
        if (lastPos != null) {
            // Calculate distance moved
            double distanceMoved = currentPos.distanceTo(lastPos);
            double rotationChanged = currentPos.rotationDifference(lastPos);
            
            // If player moved significantly or rotated significantly
            if (distanceMoved > MOVEMENT_THRESHOLD || rotationChanged > 5.0) {
                AfkManager.getInstance().updateActivity(playerId);
                LOGGER.debug("Movement activity tracked for {}: distance={}, rotation={}", 
                    player.getName().getString(), distanceMoved, rotationChanged);
            }
        }
        
        // Update stored position
        lastPositions.put(playerId, currentPos);
    }
    
    /**
     * Clean up position data when player logs out
     */
    public static void onPlayerLogout(UUID playerId) {
        lastPositions.remove(playerId);
        LOGGER.debug("Cleaned up movement tracking for player: {}", playerId);
    }
    
    /**
     * Helper class to store player position and rotation
     */
    private static class PlayerPosition {
        private final double x, y, z;
        private final float yaw, pitch;
        
        public PlayerPosition(double x, double y, double z, float yaw, float pitch) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
        }
        
        /**
         * Calculate 3D distance to another position
         */
        public double distanceTo(PlayerPosition other) {
            double dx = this.x - other.x;
            double dy = this.y - other.y;
            double dz = this.z - other.z;
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
        
        /**
         * Calculate rotation difference (combined yaw and pitch change)
         */
        public double rotationDifference(PlayerPosition other) {
            double yawDiff = Math.abs(this.yaw - other.yaw);
            double pitchDiff = Math.abs(this.pitch - other.pitch);
            
            // Handle yaw wrapping (360 degrees)
            if (yawDiff > 180) {
                yawDiff = 360 - yawDiff;
            }
            
            return Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);
        }
    }
}