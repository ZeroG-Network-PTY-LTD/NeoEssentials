package com.zerog.neoessentials.chat.handlers;

import com.zerog.neoessentials.chat.AfkManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks player movement for AFK detection.
 *
 * <p>This handler monitors player position and rotation changes to detect actual movement.
 * Small movements (e.g., from client-side micro-adjustments) are ignored using configurable thresholds.</p>
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Position tracking with distance threshold (default: 0.1 blocks)</li>
 *   <li>Rotation tracking with angle threshold (default: 5 degrees)</li>
 *   <li>Prevents false positives from lag/jitter</li>
 *   <li>Configurable sensitivity</li>
 * </ul>
 *
 * @author NeoEssentials
 * @version 1.0.0
 */
@EventBusSubscriber(modid = "neoessentials")
public class AfkMovementHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(AfkMovementHandler.class);

    // Store last known position and rotation for each player
    private static final Map<UUID, PlayerPosition> lastPositions = new ConcurrentHashMap<>();

    // Movement detection thresholds (configurable)
    private static final double POSITION_THRESHOLD = 0.1; // Minimum distance to consider movement (blocks)
    private static final double ROTATION_THRESHOLD = 5.0; // Minimum rotation to consider movement (degrees)

    // Tick throttling - check every N ticks to reduce CPU usage
    private static final int CHECK_INTERVAL_TICKS = 10; // Check every 10 ticks (~0.5 seconds)
    private static final Map<UUID, Integer> tickCounters = new ConcurrentHashMap<>();

    /**
     * Player position data container
     */
    private static class PlayerPosition {
        final Vec3 position;
        final float yaw;
        final float pitch;
        final long timestamp;

        PlayerPosition(Vec3 position, float yaw, float pitch) {
            this.position = position;
            // Validate and sanitize rotation values to prevent NaN propagation
            this.yaw = Float.isNaN(yaw) || Float.isInfinite(yaw) ? 0.0f : yaw;
            this.pitch = Float.isNaN(pitch) || Float.isInfinite(pitch) ? 0.0f : pitch;
            this.timestamp = System.currentTimeMillis();
        }

        /**
         * Calculate distance moved from this position
         */
        double distanceTo(Vec3 newPosition) {
            return position.distanceTo(newPosition);
        }

        /**
         * Calculate rotation difference in degrees
         */
        double rotationDifference(float newYaw, float newPitch) {
            // Normalize angles to -180 to 180
            float yawDiff = normalizeAngle(newYaw - yaw);
            float pitchDiff = normalizeAngle(newPitch - pitch);

            // Calculate total rotation magnitude
            return Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);
        }

        /**
         * Normalize angle to -180 to 180 range
         */
        private float normalizeAngle(float angle) {
            while (angle > 180) angle -= 360;
            while (angle < -180) angle += 360;
            return angle;
        }
    }

    /**
     * Track player movement on every player tick
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        UUID uuid = player.getUUID();

        // Throttle checks to reduce CPU usage
        int tickCount = tickCounters.getOrDefault(uuid, 0) + 1;
        if (tickCount < CHECK_INTERVAL_TICKS) {
            tickCounters.put(uuid, tickCount);
            return;
        }
        tickCounters.put(uuid, 0); // Reset counter

        // Get current position and rotation
        Vec3 currentPosition = player.position();
        float currentYaw = player.getYRot();
        float currentPitch = player.getXRot();

        // Validate rotation values - skip this tick if invalid (prevents NaN errors)
        if (Float.isNaN(currentYaw) || Float.isInfinite(currentYaw) ||
            Float.isNaN(currentPitch) || Float.isInfinite(currentPitch)) {
            LOGGER.debug("Skipping movement check for {} due to invalid rotation (NaN/Infinite)",
                player.getName().getString());
            return;
        }

        // Get last known position
        PlayerPosition lastPosition = lastPositions.get(uuid);

        if (lastPosition == null) {
            // First time tracking this player - store position but don't trigger activity
            lastPositions.put(uuid, new PlayerPosition(currentPosition, currentYaw, currentPitch));
            return;
        }

        // Calculate movement distance and rotation change
        double distanceMoved = lastPosition.distanceTo(currentPosition);
        double rotationDiff = lastPosition.rotationDifference(currentYaw, currentPitch);

        // Check if movement exceeds thresholds
        boolean positionChanged = distanceMoved > POSITION_THRESHOLD;
        boolean rotationChanged = rotationDiff > ROTATION_THRESHOLD;

        if (positionChanged || rotationChanged) {
            // Update AFK status
            AfkManager.getInstance().updateActivity(uuid);

            // Update stored position
            lastPositions.put(uuid, new PlayerPosition(currentPosition, currentYaw, currentPitch));
        }
        // else: Movement too small - likely lag/jitter, ignore it
    }

    /**
     * Clean up tracking data when player logs out
     */
    @SubscribeEvent
    public static void onPlayerLogout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UUID uuid = player.getUUID();
            lastPositions.remove(uuid);
            tickCounters.remove(uuid);
            LOGGER.debug("Movement tracking cleanup for: {}", player.getName().getString());
        }
    }

    /**
     * Initialize tracking when player logs in
     */
    @SubscribeEvent
    public static void onPlayerLogin(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UUID uuid = player.getUUID();
            Vec3 position = player.position();
            float yaw = player.getYRot();
            float pitch = player.getXRot();

            lastPositions.put(uuid, new PlayerPosition(position, yaw, pitch));
            tickCounters.put(uuid, 0);
            LOGGER.debug("Movement tracking initialized for: {}", player.getName().getString());
        }
    }

    /**
     * Get the position threshold (for external configuration)
     */
    public static double getPositionThreshold() {
        return POSITION_THRESHOLD;
    }

    /**
     * Get the rotation threshold (for external configuration)
     */
    public static double getRotationThreshold() {
        return ROTATION_THRESHOLD;
    }
}

