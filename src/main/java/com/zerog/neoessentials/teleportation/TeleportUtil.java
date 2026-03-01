package com.zerog.neoessentials.teleportation;

import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Utility class for teleportation operations with safety checks and async loading
 */
public class TeleportUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(TeleportUtil.class);
    
    // Teleport delays (in ticks)
    public static final int INSTANT_TELEPORT = 0;
    public static final int SHORT_DELAY = 20;   // 1 second
    public static final int MEDIUM_DELAY = 60;  // 3 seconds
    public static final int LONG_DELAY = 100;   // 5 seconds
    
    /**
     * Teleport a player to a location with safety checks
     */
    public static CompletableFuture<TeleportResult> teleportPlayer(ServerPlayer player, TeleportLocation location) {
        return teleportPlayer(player, location, INSTANT_TELEPORT, true);
    }
    
    /**
     * Teleport a player to a location with options
     */
    public static CompletableFuture<TeleportResult> teleportPlayer(ServerPlayer player, TeleportLocation location, 
                                                                  int delayTicks, boolean findSafe) {
        CompletableFuture<TeleportResult> future = new CompletableFuture<>();

        // Enforce combat check if enabled in config
        com.zerog.neoessentials.config.ConfigManager configManager = com.zerog.neoessentials.config.ConfigManager.getInstance();
        boolean allowTeleportInCombat = configManager.isAllowTeleportInCombatEnabled();
        if (!allowTeleportInCombat && com.zerog.neoessentials.teleportation.CombatTracker.isInCombat(player)) {
            int remainingTime = com.zerog.neoessentials.teleportation.CombatTracker.getRemainingCombatTime(player);
            future.complete(TeleportResult.failure("You cannot teleport while in combat! Please wait " + remainingTime + " second(s)."));
            return future;
        }

        // Enforce protectedAreas using YAWP (Yet Another World Protector)
        java.util.List<String> protectedAreas = com.zerog.neoessentials.config.ConfigManager.getProtectedAreas();
        if (protectedAreas != null && !protectedAreas.isEmpty()) {
            try {
                // YAWP API: net.yawp.api.YawpAPI
                // Check if YAWP is loaded and available
                Class<?> yawpApiClass = Class.forName("net.yawp.api.YawpAPI");
                Object yawpApi = yawpApiClass.getMethod("getInstance").invoke(null);
                // Query regions at target location
                java.util.List<?> regions = (java.util.List<?>) yawpApiClass.getMethod("getRegionsAt", ServerLevel.class, double.class, double.class, double.class)
                        .invoke(yawpApi, location.getLevel(), location.getX(), location.getY(), location.getZ());
                if (regions != null) {
                    for (Object region : regions) {
                        String regionName = (String) region.getClass().getMethod("getName").invoke(region);
                        if (protectedAreas.contains(regionName)) {
                            future.complete(TeleportResult.failure("Teleportation is blocked: target location is in a protected area (" + regionName + ")!"));
                            return future;
                        }
                    }
                }
            } catch (ClassNotFoundException e) {
                // YAWP not installed, skip region check
            } catch (Exception e) {
                LOGGER.error("Error checking YAWP protected areas: {}", e.getMessage(), e);
            }
        }

        // Enforce maxTeleportDistance if set in config
        int maxDistance = configManager.getMaxTeleportDistance();
        if (maxDistance > 0) {
            // Try to get player's current location as TeleportLocation
            TeleportLocation fromLoc = new TeleportLocation(player);
            if (fromLoc.getWorldName().equals(location.getWorldName())) {
                double dist = fromLoc.distanceTo(location);
                if (dist > maxDistance) {
                    future.complete(TeleportResult.failure("Teleport distance exceeds the maximum allowed by config (" + maxDistance + ")!"));
                    return future;
                }
            }
        }

        if (location == null) {
            future.complete(TeleportResult.failure("Invalid teleport location"));
            return future;
        }

        ServerLevel targetLevel = location.getLevel();
        if (targetLevel == null) {
            future.complete(TeleportResult.failure("Target world not found or not loaded"));
            return future;
        }

        // Find safe location if requested
        TeleportLocation finalLocation = location;
        if (findSafe && !location.isSafe()) {
            finalLocation = location.findSafeLocation();
            if (finalLocation == null) {
                future.complete(TeleportResult.failure("No safe teleport location found"));
                return future;
            }
        }

        // Load chunks if needed
        ChunkPos chunkPos = new ChunkPos(new BlockPos((int) finalLocation.getX(), 
                                                     (int) finalLocation.getY(), 
                                                     (int) finalLocation.getZ()));

        if (!targetLevel.isLoaded(chunkPos.getWorldPosition())) {
            // Force load the chunk
            targetLevel.getChunkSource().addRegionTicket(
                net.minecraft.server.level.TicketType.PORTAL,
                chunkPos,
                3,
                chunkPos.getWorldPosition()
            );
        }

        // Execute teleport (with delay if specified)
        TeleportLocation teleportTo = finalLocation;
        if (delayTicks > 0) {
            // Schedule delayed teleport
            player.getServer().execute(() -> {
                scheduleDelayedTeleport(player, teleportTo, delayTicks, future);
            });
        } else {
            // Immediate teleport
            executeTeleport(player, teleportTo, future);
        }

        return future;
    }
    
    /**
     * Schedule a delayed teleport
     */
    private static void scheduleDelayedTeleport(ServerPlayer player, TeleportLocation location, 
                                              int delayTicks, CompletableFuture<TeleportResult> future) {
        // Store original position to check for movement
        Vec3 originalPos = player.position();
        com.zerog.neoessentials.config.ConfigManager configManager = com.zerog.neoessentials.config.ConfigManager.getInstance();
        boolean cancelOnMovement = com.zerog.neoessentials.config.ConfigManager.isCancelOnMovementEnabled();
        boolean cancelOnDamage = configManager.isCancelOnDamageEnabled();

        // Define cancel action
        Runnable cancelAction = () -> {
            future.complete(TeleportResult.failure("Teleport cancelled - you moved/took damage!"));
        };
        // Register for damage cancel if enabled
        if (cancelOnDamage) {
            com.zerog.neoessentials.teleportation.TeleportDamageCancelHandler.registerPendingTeleport(player, cancelAction);
        }

        // Schedule the teleport
        player.getServer().tell(new net.minecraft.server.TickTask(delayTicks, () -> {
            // Unregister damage cancel (teleport completed or cancelled)
            if (cancelOnDamage) {
                com.zerog.neoessentials.teleportation.TeleportDamageCancelHandler.unregisterPendingTeleport(player);
            }
            // Check if player moved (cancel if they did), only if enabled in config
            // Use 1.5 block threshold to avoid false positives from network lag or small position shifts
            if (cancelOnMovement && player.position().distanceTo(originalPos) > 1.5) {
                double distance = player.position().distanceTo(originalPos);
                LOGGER.debug("Teleport cancelled for {} - moved {} blocks (threshold: 1.5)",
                    player.getName().getString(), String.format("%.2f", distance));
                future.complete(TeleportResult.failure("Teleport cancelled - you moved!"));
                return;
            }
            // Check if player is still online
            if (player.hasDisconnected()) {
                future.complete(TeleportResult.failure("Player disconnected"));
                return;
            }
            executeTeleport(player, location, future);
        }));
    }
    
    /**
     * Execute the actual teleport
     */
    private static void executeTeleport(ServerPlayer player, TeleportLocation location, 
                                      CompletableFuture<TeleportResult> future) {
        try {
            ServerLevel targetLevel = location.getLevel();
            if (targetLevel == null) {
                future.complete(TeleportResult.failure("Target world no longer available"));
                return;
            }

            // Particle effects (source)
            com.zerog.neoessentials.config.ConfigManager configManager = com.zerog.neoessentials.config.ConfigManager.getInstance();
            if (configManager.getEnableParticleEffects()) {
                // Show particle at source
                if (player.level() instanceof ServerLevel serverLevel) {
                    for (int i = 0; i < 50; i++) {
                        double dx = player.getX() + (player.getRandom().nextDouble() - 0.5) * 1.0;
                        double dy = player.getY() + 1 + player.getRandom().nextDouble();
                        double dz = player.getZ() + (player.getRandom().nextDouble() - 0.5) * 1.0;
                        serverLevel.addParticle(
                            net.minecraft.core.particles.ParticleTypes.PORTAL,
                            dx, dy, dz,
                            0, 0, 0
                        );
                    }
                }
            }

            // Sound effects (source)
            if (com.zerog.neoessentials.config.ConfigManager.getEnableSoundEffects()) {
                if (player.level() instanceof ServerLevel serverLevel) {
                    serverLevel.playSound(
                        null, // No specific player, play for all nearby
                        player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ENDERMAN_TELEPORT,
                        SoundSource.PLAYERS,
                        1.0F, 1.0F
                    );
                }
            }

            // Perform the teleport
            if (player.level() != targetLevel) {
                // Cross-dimension teleport
                // Validate rotation values before teleporting
                float yaw = location.getYaw();
                float pitch = location.getPitch();

                // Sanitize rotation to prevent NaN errors
                if (Float.isNaN(yaw) || Float.isInfinite(yaw)) {
                    yaw = 0.0f;
                    LOGGER.warn("Invalid yaw during cross-dimension teleport, using 0.0f");
                }
                if (Float.isNaN(pitch) || Float.isInfinite(pitch)) {
                    pitch = 0.0f;
                    LOGGER.warn("Invalid pitch during cross-dimension teleport, using 0.0f");
                }

                player.teleportTo(targetLevel, location.getX(), location.getY(), location.getZ(), yaw, pitch);
            } else {
                // Same dimension teleport
                player.teleportTo(location.getX(), location.getY(), location.getZ());

                // Validate rotation values before setting
                float yaw = location.getYaw();
                float pitch = location.getPitch();

                // Sanitize rotation to prevent NaN errors
                if (Float.isNaN(yaw) || Float.isInfinite(yaw)) {
                    yaw = 0.0f;
                    LOGGER.warn("Invalid yaw during same-dimension teleport, using 0.0f");
                }
                if (Float.isNaN(pitch) || Float.isInfinite(pitch)) {
                    pitch = 0.0f;
                    LOGGER.warn("Invalid pitch during same-dimension teleport, using 0.0f");
                }

                player.setYRot(yaw);
                player.setXRot(pitch);
            }

            // Particle effects (destination)
            if (configManager.getEnableParticleEffects()) {
                for (int i = 0; i < 50; i++) {
                    double dx = location.getX() + (player.getRandom().nextDouble() - 0.5) * 1.0;
                    double dy = location.getY() + 1 + player.getRandom().nextDouble();
                    double dz = location.getZ() + (player.getRandom().nextDouble() - 0.5) * 1.0;
                    targetLevel.addParticle(
                        net.minecraft.core.particles.ParticleTypes.PORTAL,
                        dx, dy, dz,
                        0, 0, 0
                    );
                }
            }

            // Sound effects (destination)
            if (com.zerog.neoessentials.config.ConfigManager.getEnableSoundEffects()) {
                targetLevel.playSound(
                    null, // No specific player, play for all nearby
                    location.getX(), location.getY(), location.getZ(),
                    SoundEvents.ENDERMAN_TELEPORT,
                    SoundSource.PLAYERS,
                    1.0F, 1.0F
                );
            }

            LOGGER.debug("Teleported {} to {}", player.getName().getString(), location.getLocationString());
            future.complete(TeleportResult.success("Teleported to " + location.getLocationString()));

        } catch (Exception e) {
            LOGGER.error("Failed to teleport player {}: {}", player.getName().getString(), e.getMessage(), e);
            future.complete(TeleportResult.failure("Teleport failed: " + e.getMessage()));
        }
    }
    
    /**
     * Get the highest safe Y coordinate at the given X,Z in the world.
     * Scans top-down for a solid, non-dangerous ground with two clear blocks above.
     */
    public static int getHighestSafeY(ServerLevel level, int x, int z) {
        for (int y = level.getMaxBuildHeight() - 2; y >= level.getMinBuildHeight() + 1; y--) {
            BlockPos testPos = new BlockPos(x, y, z);
            if (isSafeLocation(level, testPos)) {
                return y;
            }
        }
        return level.getSeaLevel();
    }

    /**
     * Find the nearest safe location to a position.
     */
    public static BlockPos findNearestSafeLocation(ServerLevel level, BlockPos center, int maxRadius) {
        if (isSafeLocation(level, center)) return center;

        for (int radius = 1; radius <= maxRadius; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) != radius && Math.abs(dz) != radius) continue;
                    int safeY = getHighestSafeY(level, center.getX() + dx, center.getZ() + dz);
                    BlockPos safePos = new BlockPos(center.getX() + dx, safeY, center.getZ() + dz);
                    if (isSafeLocation(level, safePos)) return safePos;
                }
            }
        }
        return null;
    }

    /**
     * Check if a location is safe for teleportation.
     * Uses isSolid() (not canOcclude()) and rejects dangerous block types.
     */
    public static boolean isSafeLocation(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos)) return false;

        BlockPos ground = pos.below();
        BlockPos head   = pos.above();

        net.minecraft.world.level.block.state.BlockState groundState = level.getBlockState(ground);
        net.minecraft.world.level.block.state.BlockState feetState   = level.getBlockState(pos);
        net.minecraft.world.level.block.state.BlockState headState   = level.getBlockState(head);

        // Ground must be solid: has a non-empty collision shape and is not air
        if (groundState.isAir() || groundState.getCollisionShape(level, ground).isEmpty()) return false;
        if (!feetState.getCollisionShape(level, pos).isEmpty() && !feetState.isAir()) return false;
        if (!headState.getCollisionShape(level, head).isEmpty() && !headState.isAir()) return false;
        if (isDangerousBlock(groundState)) return false;
        if (isDangerousBlock(feetState))   return false;

        return true;
    }

    /** Returns true if the block state represents a dangerous block to stand on or in. */
    private static boolean isDangerousBlock(net.minecraft.world.level.block.state.BlockState state) {
        net.minecraft.world.level.block.Block block = state.getBlock();
        return block == net.minecraft.world.level.block.Blocks.LAVA
            || block == net.minecraft.world.level.block.Blocks.WATER
            || block == net.minecraft.world.level.block.Blocks.FIRE
            || block == net.minecraft.world.level.block.Blocks.SOUL_FIRE
            || block == net.minecraft.world.level.block.Blocks.MAGMA_BLOCK
            || block == net.minecraft.world.level.block.Blocks.CACTUS
            || block == net.minecraft.world.level.block.Blocks.SWEET_BERRY_BUSH
            || block == net.minecraft.world.level.block.Blocks.WITHER_ROSE
            || block == net.minecraft.world.level.block.Blocks.NETHER_PORTAL
            || block == net.minecraft.world.level.block.Blocks.CAMPFIRE
            || block == net.minecraft.world.level.block.Blocks.SOUL_CAMPFIRE
            || block == net.minecraft.world.level.block.Blocks.POWDER_SNOW;
    }
    
    /**
     * Send teleport countdown message to player
     */
    public static void sendCountdownMessage(ServerPlayer player, int seconds) {
        if (seconds > 0) {
            player.sendSystemMessage(MessageUtil.info("commands.neoessentials.teleport.countdown", seconds));
        }
    }
    
    /**
     * Result class for teleport operations
     */
    public static class TeleportResult {
        private final boolean success;
        private final String message;
        private final TeleportLocation location;
        
        private TeleportResult(boolean success, String message, TeleportLocation location) {
            this.success = success;
            this.message = message;
            this.location = location;
        }
        
        public static TeleportResult success(String message) {
            return new TeleportResult(true, message, null);
        }
        
        public static TeleportResult success(String message, TeleportLocation location) {
            return new TeleportResult(true, message, location);
        }
        
        public static TeleportResult failure(String message) {
            return new TeleportResult(false, message, null);
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public TeleportLocation getLocation() { return location; }
    }
}