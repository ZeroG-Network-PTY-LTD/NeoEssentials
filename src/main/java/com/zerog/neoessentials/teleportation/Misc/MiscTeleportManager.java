package com.zerog.neoessentials.teleportation.Misc;

import com.zerog.neoessentials.teleportation.TeleportLocation;
import com.zerog.neoessentials.teleportation.TeleportUtil;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager for miscellaneous teleportation utilities (/back, death locations, etc.)
 */
public class MiscTeleportManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(MiscTeleportManager.class);
    
    // Singleton pattern
    private static class SingletonHolder {
        private static final MiscTeleportManager INSTANCE = new MiscTeleportManager();
    }
    
    public static MiscTeleportManager getInstance() {
        return SingletonHolder.INSTANCE;
    }
    
    // Storage for back locations
    private final Map<UUID, TeleportLocation> backLocations = new ConcurrentHashMap<>();
    private final Map<UUID, TeleportLocation> deathLocations = new ConcurrentHashMap<>();
    
    // Configuration
    private int maxBackHistory = 5; // Number of back locations to remember
    private int teleportDelay = 3; // 3 second delay for back teleports
    private boolean enableDeathBack = true; // Allow /back after death
    private boolean enableTeleportBack = true; // Save location before teleporting
    
    private MiscTeleportManager() {
        // Private constructor for singleton
    }
    
    /**
     * Save a player's current location as their back location
     */
    public void saveBackLocation(ServerPlayer player) {
        if (!enableTeleportBack) {
            return;
        }
        
        UUID playerId = player.getUUID();
        TeleportLocation backLocation = new TeleportLocation(player);
        
        backLocations.put(playerId, backLocation);
        
        LOGGER.debug("Saved back location for {}: {}", 
                    player.getName().getString(), backLocation);
    }
    
    /**
     * Save a player's death location
     */
    public void saveDeathLocation(ServerPlayer player) {
        if (!enableDeathBack) {
            return;
        }
        
        UUID playerId = player.getUUID();
        TeleportLocation deathLocation = new TeleportLocation(player);
        
        deathLocations.put(playerId, deathLocation);
        
        player.sendSystemMessage(MessageUtil.info("commands.neoessentials.teleport.misc.death_location_saved"));
        
        LOGGER.info("Saved death location for {}: {}", 
                   player.getName().getString(), deathLocation);
    }
    
    /**
     * Teleport player back to their previous location or death location (prioritizing death location, but do not remove death location until player dies again)
     */
    public boolean teleportBack(ServerPlayer player) {
        UUID playerId = player.getUUID();
        TeleportLocation deathLocation = deathLocations.get(playerId);
        TeleportLocation backLocation = backLocations.get(playerId);
        TeleportLocation targetLocation = null;
        final boolean usedDeath;

        // If death location exists and player is not already at that location, prioritize it
        if (deathLocation != null && !isPlayerAtLocation(player, deathLocation)) {
            targetLocation = deathLocation;
            usedDeath = true;
        } else if (backLocation != null) {
            targetLocation = backLocation;
            usedDeath = false;
        } else {
            usedDeath = false;
        }

        if (targetLocation == null) {
            player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.misc.no_back_location"));
            return false;
        }

        // Save current location before teleporting back
        TeleportLocation currentLocation = new TeleportLocation(player);
        final TeleportLocation finalTargetLocation = targetLocation;
        int delayTicks = teleportDelay * 20;
        TeleportUtil.teleportPlayer(player, finalTargetLocation, delayTicks, true).thenAccept(result -> {
            if (result.isSuccess()) {
                // Update back location to where they just came from
                backLocations.put(playerId, currentLocation);
                if (usedDeath) {
                    player.sendSystemMessage(MessageUtil.success("commands.neoessentials.teleport.misc.death_teleport_success"));
                } else {
                    player.sendSystemMessage(MessageUtil.success("commands.neoessentials.teleport.misc.back_success"));
                }
                LOGGER.info("Player {} teleported back to {}", player.getName().getString(), finalTargetLocation);
            } else {
                player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.misc.back_failed", result.getMessage()));
                LOGGER.warn("Failed back teleport for {}: {}", player.getName().getString(), result.getMessage());
            }
        });
        return true;
    }

    /**
     * Helper to check if player is already at a given location (within a small threshold)
     */
    private boolean isPlayerAtLocation(ServerPlayer player, TeleportLocation location) {
        double dx = player.getX() - location.getX();
        double dy = player.getY() - location.getY();
        double dz = player.getZ() - location.getZ();
        return Math.abs(dx) < 0.5 && Math.abs(dy) < 1.0 && Math.abs(dz) < 0.5 &&
                player.level().dimension().location().toString().equals(location.getWorldName());
    }

    /**
     * Teleport player to their death location
     */
    public boolean teleportToDeathLocation(ServerPlayer player) {
        UUID playerId = player.getUUID();
        
        TeleportLocation deathLocation = deathLocations.get(playerId);
        if (deathLocation == null) {
            player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.misc.no_death_location"));
            return false;
        }
        
        // Save current location as back location
        saveBackLocation(player);
        
        // Perform the teleport
        int delayTicks = teleportDelay * 20; // Convert seconds to ticks
        TeleportUtil.teleportPlayer(player, deathLocation, delayTicks, true).thenAccept(result -> {
            if (result.isSuccess()) {
                player.sendSystemMessage(MessageUtil.success("commands.neoessentials.teleport.misc.death_teleport_success"));
                
                // Clear death location after successful teleport
                deathLocations.remove(playerId);
                
                LOGGER.info("Player {} teleported to death location: {}", 
                           player.getName().getString(), deathLocation);
            } else {
                player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.misc.death_teleport_failed", result.getMessage()));
                
                LOGGER.warn("Failed death teleport for {}: {}", 
                           player.getName().getString(), result.getMessage());
            }
        });
        
        return true;
    }
    
    /**
     * Clear a player's back location
     */
    public void clearBackLocation(ServerPlayer player) {
        UUID playerId = player.getUUID();
        backLocations.remove(playerId);
        deathLocations.remove(playerId);
        
        LOGGER.debug("Cleared back locations for {}", player.getName().getString());
    }
    
    /**
     * Check if player has a back location
     */
    public boolean hasBackLocation(ServerPlayer player) {
        UUID playerId = player.getUUID();
        return backLocations.containsKey(playerId) || deathLocations.containsKey(playerId);
    }
    
    /**
     * Get back location info for a player
     */
    public String getBackLocationInfo(ServerPlayer player) {
        UUID playerId = player.getUUID();
        
        TeleportLocation backLocation = backLocations.get(playerId);
        if (backLocation != null) {
            return MessageUtil.localize("commands.neoessentials.teleport.misc.back_info", 
                                       backLocation.getWorldName(), 
                                       String.format("%.1f %.1f %.1f", backLocation.getX(), backLocation.getY(), backLocation.getZ()));
        }
        
        TeleportLocation deathLocation = deathLocations.get(playerId);
        if (deathLocation != null) {
            return MessageUtil.localize("commands.neoessentials.teleport.misc.death_info", 
                                       deathLocation.getWorldName(), 
                                       String.format("%.1f %.1f %.1f", deathLocation.getX(), deathLocation.getY(), deathLocation.getZ()));
        }
        
        return MessageUtil.localize("commands.neoessentials.teleport.misc.no_back_location");
    }
    
    /**
     * Handle player disconnect - clean up data
     */
    public void onPlayerDisconnect(ServerPlayer player) {
        // Keep back locations for reconnection, but remove from memory after some time
        // For now, we'll keep them until server restart
        
        LOGGER.debug("Player {} disconnected, keeping back location data", player.getName().getString());
    }
    
    /**
     * Handle player death - save death location
     */
    public void onPlayerDeath(ServerPlayer player) {
        saveDeathLocation(player);
    }
    
    // Configuration getters/setters
    public int getMaxBackHistory() {
        return maxBackHistory;
    }
    
    public void setMaxBackHistory(int max) {
        this.maxBackHistory = Math.max(1, max);
    }
    
    public int getTeleportDelay() {
        return teleportDelay;
    }
    
    public void setTeleportDelay(int delay) {
        this.teleportDelay = Math.max(0, delay);
    }
    
    public boolean isEnableDeathBack() {
        return enableDeathBack;
    }
    
    public void setEnableDeathBack(boolean enable) {
        this.enableDeathBack = enable;
    }
    
    public boolean isEnableTeleportBack() {
        return enableTeleportBack;
    }
    
    public void setEnableTeleportBack(boolean enable) {
        this.enableTeleportBack = enable;
    }
    
    /**
     * Get statistics
     */
    public String getStatistics() {
        return String.format("MiscTeleport Statistics: %d back locations, %d death locations, delay=%ds", 
                           backLocations.size(), deathLocations.size(), teleportDelay);
    }
    
    /**
     * Teleport player to the highest solid block at their current position
     */
    public boolean teleportToTop(ServerPlayer player) {
        // Save current location as back location
        saveBackLocation(player);
        
        int currentX = (int) player.getX();
        int currentZ = (int) player.getZ();
        int maxY = player.level().getMaxBuildHeight() - 1;
        
        // Find the highest solid block
        for (int y = maxY; y >= player.level().getMinBuildHeight(); y--) {
            if (!player.level().getBlockState(new net.minecraft.core.BlockPos(currentX, y, currentZ)).isAir()) {
                // Found solid block, teleport to one block above it
                final int targetY = y + 1; // Make final for lambda
                TeleportLocation topLocation = new TeleportLocation(
                    player.level().dimension().location().toString(),
                    currentX + 0.5, targetY, currentZ + 0.5,
                    player.getYRot(), player.getXRot(), "system"
                );
                
                int delayTicks = teleportDelay * 20;
                TeleportUtil.teleportPlayer(player, topLocation, delayTicks, true).thenAccept(result -> {
                    if (result.isSuccess()) {
                        player.sendSystemMessage(MessageUtil.success("commands.neoessentials.teleport.misc.top_success"));
                        LOGGER.info("Player {} teleported to top at Y={}", player.getName().getString(), targetY);
                    } else {
                        player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.misc.top_failed", result.getMessage()));
                    }
                });
                return true;
            }
        }
        
        player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.misc.no_solid_block"));
        return false;
    }
    
    /**
     * Teleport player through walls to the next open space
     */
    public boolean teleportJump(ServerPlayer player) {
        // Save current location as back location
        saveBackLocation(player);
        
        net.minecraft.world.phys.Vec3 lookDirection = player.getLookAngle();
        net.minecraft.world.phys.Vec3 currentPos = player.position();
        
        // Search for next open space in look direction
        for (int distance = 1; distance <= 20; distance++) {
            double newX = currentPos.x + lookDirection.x * distance;
            double newY = currentPos.y + lookDirection.y * distance;
            double newZ = currentPos.z + lookDirection.z * distance;
            
            net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos((int) newX, (int) newY, (int) newZ);
            net.minecraft.core.BlockPos posAbove = pos.above();
            
            // Check if there's enough space for player (2 blocks high)
            if (player.level().getBlockState(pos).isAir() && 
                player.level().getBlockState(posAbove).isAir()) {
                
                final int finalDistance = distance; // Make final for lambda
                TeleportLocation jumpLocation = new TeleportLocation(
                    player.level().dimension().location().toString(),
                    newX, newY, newZ,
                    player.getYRot(), player.getXRot(), "system"
                );
                
                int delayTicks = teleportDelay * 20;
                TeleportUtil.teleportPlayer(player, jumpLocation, delayTicks, true).thenAccept(result -> {
                    if (result.isSuccess()) {
                        player.sendSystemMessage(MessageUtil.success("commands.neoessentials.teleport.misc.jump_success"));
                        LOGGER.info("Player {} jumped through walls to distance {}", player.getName().getString(), finalDistance);
                    } else {
                        player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.misc.jump_failed", result.getMessage()));
                    }
                });
                return true;
            }
        }
        
        player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.misc.no_open_space"));
        return false;
    }
    
    /**
     * Teleport player to the block they are looking at
     */
    public boolean teleportToLookingAt(ServerPlayer player) {
        // Save current location as back location
        saveBackLocation(player);
        
        // Perform raycast to find what player is looking at
        net.minecraft.world.phys.HitResult hitResult = player.pick(100.0D, 1.0F, false);
        
        if (hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            net.minecraft.world.phys.BlockHitResult blockHitResult = (net.minecraft.world.phys.BlockHitResult) hitResult;
            net.minecraft.core.BlockPos targetPos = blockHitResult.getBlockPos();
            
            // Teleport to one block above the target block
            TeleportLocation jumpToLocation = new TeleportLocation(
                player.level().dimension().location().toString(),
                targetPos.getX() + 0.5, targetPos.getY() + 1.0, targetPos.getZ() + 0.5,
                player.getYRot(), player.getXRot(), "system"
            );
            
            int delayTicks = teleportDelay * 20;
            TeleportUtil.teleportPlayer(player, jumpToLocation, delayTicks, true).thenAccept(result -> {
                if (result.isSuccess()) {
                    player.sendSystemMessage(MessageUtil.success("commands.neoessentials.teleport.misc.jumpto_success"));
                    LOGGER.info("Player {} teleported to looking at: {}", player.getName().getString(), targetPos);
                } else {
                    player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.misc.jumpto_failed", result.getMessage()));
                }
            });
            return true;
        }
        
        player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.misc.no_block_in_sight"));
        return false;
    }
    
    /**
     * Random teleportation within configured bounds
     */
    public boolean randomTeleport(ServerPlayer player) {
        // Save current location as back location
        saveBackLocation(player);
        
        // Configuration for random teleport bounds
        int maxDistance = 1000; // Maximum distance from spawn/current location
        int attempts = 10; // Number of attempts to find safe location
        
        java.util.Random random = new java.util.Random();
        
        for (int attempt = 0; attempt < attempts; attempt++) {
            // Generate random coordinates
            int randomX = (int) player.getX() + random.nextInt(maxDistance * 2) - maxDistance;
            int randomZ = (int) player.getZ() + random.nextInt(maxDistance * 2) - maxDistance;
            
            // Find safe Y coordinate (highest solid block + 1)
            int maxY = player.level().getMaxBuildHeight() - 1;
            for (int y = maxY; y >= player.level().getMinBuildHeight(); y--) {
                net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(randomX, y, randomZ);
                net.minecraft.core.BlockPos posAbove = pos.above();
                net.minecraft.core.BlockPos posAbove2 = posAbove.above();
                
                // Check if location is safe (solid ground, air above)
                if (!player.level().getBlockState(pos).isAir() && 
                    player.level().getBlockState(posAbove).isAir() && 
                    player.level().getBlockState(posAbove2).isAir()) {
                    
                    final int safeY = y + 1; // Make final for lambda
                    TeleportLocation randomLocation = new TeleportLocation(
                        player.level().dimension().location().toString(),
                        randomX + 0.5, safeY, randomZ + 0.5,
                        player.getYRot(), player.getXRot(), "system"
                    );
                    
                    int delayTicks = teleportDelay * 20;
                    TeleportUtil.teleportPlayer(player, randomLocation, delayTicks, true).thenAccept(result -> {
                        if (result.isSuccess()) {
                            player.sendSystemMessage(MessageUtil.success("commands.neoessentials.teleport.misc.tpr_success", randomX, safeY, randomZ));
                            LOGGER.info("Player {} randomly teleported to: {} {} {}", player.getName().getString(), randomX, safeY, randomZ);
                        } else {
                            player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.misc.tpr_failed", result.getMessage()));
                        }
                    });
                    return true;
                }
            }
        }
        
        player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.misc.tpr_no_safe_location"));
        return false;
    }
    
    /**
     * Clear all data (for server shutdown)
     */
    public void clearAllData() {
        backLocations.clear();
        deathLocations.clear();
        LOGGER.info("Cleared all misc teleport data");
    }
}





