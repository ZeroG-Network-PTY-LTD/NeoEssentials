package com.zerog.neoessentials.chat.handlers;

import com.zerog.neoessentials.chat.AfkManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Simple movement detection using periodic position checks.
 * Uses a separate thread to periodically check player positions.
 */
public class AfkMovementDetector {
    private static final Logger LOGGER = LoggerFactory.getLogger(AfkMovementDetector.class);
    
    // Minimum distance to consider as movement (in blocks)
    private static final double MOVEMENT_THRESHOLD = 0.1;
    
    // Store last known positions
    private static final Map<UUID, PlayerPosition> lastPositions = new HashMap<>();
    
    // Scheduled executor for position checks
    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "AFK-MovementDetector");
        t.setDaemon(true);
        return t;
    });
    
    // Start the movement detection task
    static {
        executor.scheduleAtFixedRate(AfkMovementDetector::checkAllPlayersMovement, 5, 5, TimeUnit.SECONDS);
    }
    
    /**
     * Check movement for all online players
     */
    private static void checkAllPlayersMovement() {
        try {
            net.minecraft.server.MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server == null) return;
            
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                checkPlayerMovement(player);
            }
        } catch (Exception e) {
            LOGGER.error("Error checking player movement", e);
        }
    }
    
    /**
     * Check if a player has moved significantly
     */
    private static void checkPlayerMovement(ServerPlayer player) {
        UUID playerId = player.getUUID();

        // Get rotation values
        float yaw = player.getYRot();
        float pitch = player.getXRot();

        // Skip this check if rotation is invalid (prevents NaN errors)
        if (Float.isNaN(yaw) || Float.isInfinite(yaw) ||
            Float.isNaN(pitch) || Float.isInfinite(pitch)) {
            LOGGER.debug("Skipping movement check for {} due to invalid rotation (NaN/Infinite)",
                player.getName().getString());
            return;
        }

        PlayerPosition currentPos = new PlayerPosition(
            player.getX(), 
            player.getY(), 
            player.getZ(),
            yaw,
            pitch
        );
        
        PlayerPosition lastPos = lastPositions.get(playerId);
        
        if (lastPos != null) {
            // Calculate distance moved
            double distanceMoved = currentPos.distanceTo(lastPos);
            double rotationChanged = currentPos.rotationDifference(lastPos);

            // Use configurable rotation threshold from AfkManager
            double rotationThreshold = AfkManager.getInstance().getRotationThreshold();
            // If player moved significantly or rotated significantly
            if (distanceMoved > MOVEMENT_THRESHOLD || rotationChanged > rotationThreshold) {
                AfkManager.getInstance().updateActivity(playerId);
                LOGGER.debug("Movement activity tracked for {}: distance={}, rotation={} (threshold={})",
                    player.getName().getString(), distanceMoved, rotationChanged, rotationThreshold);
            }
        }
        
        // Update stored position
        lastPositions.put(playerId, currentPos);
    }
    
    /**
     * Clean up position data when player logs out
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            lastPositions.remove(player.getUUID());
            LOGGER.debug("Cleaned up movement tracking for player: {}", player.getName().getString());
        }
    }
    
    /**
     * Initialize position tracking when player logs in
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerPosition currentPos = new PlayerPosition(
                player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot()
            );
            lastPositions.put(player.getUUID(), currentPos);
            LOGGER.debug("Initialized movement tracking for player: {}", player.getName().getString());
        }
    }
    
    /**
     * Shutdown the movement detector
     */
    public static void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
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
            // Validate and sanitize rotation values to prevent NaN propagation
            this.yaw = Float.isNaN(yaw) || Float.isInfinite(yaw) ? 0.0f : yaw;
            this.pitch = Float.isNaN(pitch) || Float.isInfinite(pitch) ? 0.0f : pitch;
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