package com.zerog.neoessentials.teleportation;

import com.zerog.neoessentials.util.MessageUtil;
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
        
        // Schedule the teleport
        player.getServer().tell(new net.minecraft.server.TickTask(delayTicks, () -> {
            // Check if player moved (cancel if they did)
            if (player.position().distanceTo(originalPos) > 1.0) {
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
            
            // Perform the teleport
            if (player.level() != targetLevel) {
                // Cross-dimension teleport
                player.teleportTo(targetLevel, location.getX(), location.getY(), location.getZ(), 
                                location.getYaw(), location.getPitch());
            } else {
                // Same dimension teleport
                player.teleportTo(location.getX(), location.getY(), location.getZ());
                player.setYRot(location.getYaw());
                player.setXRot(location.getPitch());
            }
            
            LOGGER.debug("Teleported {} to {}", player.getName().getString(), location.getLocationString());
            future.complete(TeleportResult.success("Teleported to " + location.getLocationString()));
            
        } catch (Exception e) {
            LOGGER.error("Failed to teleport player {}: {}", player.getName().getString(), e.getMessage(), e);
            future.complete(TeleportResult.failure("Teleport failed: " + e.getMessage()));
        }
    }
    
    /**
     * Get the highest safe Y coordinate at the given X,Z in the world
     */
    public static int getHighestSafeY(ServerLevel level, int x, int z) {
        // Search downward for a safe spot
        for (int y = level.getMaxBuildHeight() - 1; y >= level.getMinBuildHeight(); y--) {
            BlockPos testPos = new BlockPos(x, y, z);
            BlockPos above = testPos.above();
            
            // Check if this position is safe (solid ground, air above)
            if (!level.getBlockState(testPos).isAir() && level.getBlockState(testPos).canOcclude() && 
                level.getBlockState(above).isAir() && 
                level.getBlockState(above.above()).isAir()) {
                return y + 1; // Return the air block above the solid ground
            }
        }
        
        return level.getSeaLevel(); // Fallback to sea level
    }
    
    /**
     * Find the nearest safe location to a position
     */
    public static BlockPos findNearestSafeLocation(ServerLevel level, BlockPos center, int maxRadius) {
        // Try the center first
        if (isSafeLocation(level, center)) {
            return center;
        }
        
        // Search in expanding spiral
        for (int radius = 1; radius <= maxRadius; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) != radius && Math.abs(dz) != radius) continue;
                    
                    BlockPos testPos = center.offset(dx, 0, dz);
                    int safeY = getHighestSafeY(level, testPos.getX(), testPos.getZ());
                    BlockPos safePos = new BlockPos(testPos.getX(), safeY, testPos.getZ());
                    
                    if (isSafeLocation(level, safePos)) {
                        return safePos;
                    }
                }
            }
        }
        
        return null; // No safe location found
    }
    
    /**
     * Check if a location is safe for teleportation
     */
    public static boolean isSafeLocation(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos)) return false;
        
        BlockPos ground = pos.below();
        BlockPos feet = pos;
        BlockPos head = pos.above();
        
        // Need solid ground and air for feet/head
        boolean solidGround = !level.getBlockState(ground).isAir() && level.getBlockState(ground).canOcclude();
        boolean feetFree = level.getBlockState(feet).isAir();
        boolean headFree = level.getBlockState(head).isAir();
        
        return solidGround && feetFree && headFree;
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