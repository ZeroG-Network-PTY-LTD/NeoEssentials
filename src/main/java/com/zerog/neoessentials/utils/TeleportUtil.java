package com.zerog.neoessentials.utils;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.util.LanguageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Utility class for handling teleportation-related functionality.
 */
public class TeleportUtil {
    // Maps player UUIDs to their last location before teleport
    private static final Map<UUID, LocationData> lastLocations = new HashMap<>();
    
    // Maps player UUIDs to teleport request data
    private static final Map<UUID, TeleportRequest> teleportRequests = new HashMap<>();
    
    // Maps player UUIDs to teleport cooldown end time (in milliseconds)
    private static final Map<UUID, Long> teleportCooldowns = new HashMap<>();    /**
     * Teleports a player to a specific location with specific rotation
     * 
     * @param player The player to teleport
     * @param level The level (dimension) to teleport to
     * @param x The x coordinate
     * @param y The y coordinate
     * @param z The z coordinate
     * @param yaw The yaw rotation
     * @param pitch The pitch rotation
     * @return True if teleportation was successful, false otherwise
<<<<<<< HEAD
<<<<<<< HEAD
     */    public static boolean teleport(ServerPlayer player, ServerLevel level, double x, double y, double z, float yaw, float pitch) {
        try {
            // Validate input parameters
            if (player == null) {
                NeoEssentials.LOGGER.error("Cannot teleport null player");
                return false;
            }
            
            if (level == null) {
                NeoEssentials.LOGGER.error("Cannot teleport player {} to null level", player.getScoreboardName());
                return false;
            }
            
            NeoEssentials.LOGGER.debug("Teleporting player {} to [{}, {}, {}] in dimension {}", 
                player.getScoreboardName(), x, y, z, level.dimension().location());
            
=======
     */
    public static boolean teleport(ServerPlayer player, ServerLevel level, double x, double y, double z, float yaw, float pitch) {
        try {
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
     */    public static boolean teleport(ServerPlayer player, ServerLevel level, double x, double y, double z, float yaw, float pitch) {
        try {
            // Validate input parameters
            if (player == null) {
                NeoEssentials.LOGGER.error("Cannot teleport null player");
                return false;
            }
            
            if (level == null) {
                NeoEssentials.LOGGER.error("Cannot teleport player {} to null level", player.getScoreboardName());
                return false;
            }
            
            NeoEssentials.LOGGER.debug("Teleporting player {} to [{}, {}, {}] in dimension {}", 
                player.getScoreboardName(), x, y, z, level.dimension().location());
            
>>>>>>> 1713a18 (Refactor warp command handling and improve MySQL storage)
            // Save the player's current location
            saveLastLocation(player);
            
            // Record position for /back command
            TeleportHistory.recordPosition(player);
            
            // Perform the teleport
            player.teleportTo(level, x, y, z, yaw, pitch);
            
            // Apply cooldown
            applyCooldown(player.getUUID());
            
<<<<<<< HEAD
<<<<<<< HEAD
            NeoEssentials.LOGGER.debug("Successfully teleported player {} to [{}, {}, {}] in dimension {}", 
                player.getScoreboardName(), x, y, z, level.dimension().location());
            
            return true;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error teleporting player: {}", e.getMessage(), e);
=======
            return true;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error teleporting player " + player.getScoreboardName(), e);
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
            NeoEssentials.LOGGER.debug("Successfully teleported player {} to [{}, {}, {}] in dimension {}", 
                player.getScoreboardName(), x, y, z, level.dimension().location());
            
            return true;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error teleporting player: {}", e.getMessage(), e);
>>>>>>> 1713a18 (Refactor warp command handling and improve MySQL storage)
            return false;
        }
    }
    
    /**
     * Teleports a player to a specific location
     * 
     * @param player The player to teleport
     * @param level The level (dimension) to teleport to
     * @param pos The position to teleport to
     * @param saveLastLocation Whether to save the player's last location
     * @return True if teleportation was successful, false otherwise
     */
    public static boolean teleportPlayer(ServerPlayer player, ServerLevel level, Vec3 pos, boolean saveLastLocation) {
        // Check if player is on cooldown
        if (isOnCooldown(player.getUUID())) {
            long remainingCooldown = getRemainingCooldown(player.getUUID()) / 1000;
            LanguageUtil.sendErrorMessage(player, "You must wait " + remainingCooldown + " seconds before teleporting again.");
            return false;
        }
        
        try {
            // Save the player's current location if requested
            if (saveLastLocation) {
                saveLastLocation(player);
            }
            
            // Perform the teleport
            player.teleportTo(level, pos.x, pos.y, pos.z, player.getYRot(), player.getXRot());
            
            // Apply cooldown
            applyCooldown(player.getUUID());
            
            return true;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error teleporting player " + player.getScoreboardName(), e);
            return false;
        }
    }
    
    /**
     * Save a player's current location
     * 
     * @param player The player whose location to save
     */
    public static void saveLastLocation(ServerPlayer player) {
        LocationData locationData = new LocationData(
                player.level().dimension().location().toString(),
                player.getX(),
                player.getY(),
                player.getZ(),
                player.getYRot(),
                player.getXRot()
        );
        
        lastLocations.put(player.getUUID(), locationData);
    }
    
    /**
     * Teleports a player back to their last location
     * 
     * @param player The player to teleport back
     * @return True if teleportation was successful, false otherwise
     */
    public static boolean teleportToLastLocation(ServerPlayer player) {
        UUID playerId = player.getUUID();
        
        if (!lastLocations.containsKey(playerId)) {
            LanguageUtil.sendErrorMessage(player, "You have no previous location to return to.");
            return false;
        }
          LocationData lastLocation = lastLocations.get(playerId);
        ServerLevel level = player.getServer().getLevel(
                ResourceLocation.tryParse(lastLocation.dimension) != null ?
                ResourceKey.create(Registries.DIMENSION, ResourceLocation.tryParse(lastLocation.dimension)) :
                Level.OVERWORLD);
        
        if (level == null) {
            LanguageUtil.sendErrorMessage(player, "Error: Could not find the dimension you were previously in.");
            return false;
        }
        
        Vec3 pos = new Vec3(lastLocation.x, lastLocation.y, lastLocation.z);
        boolean result = teleportPlayer(player, level, pos, false);
        
        if (result) {
            // Remove the last location after a successful teleport back
            lastLocations.remove(playerId);
        }
        
        return result;
    }
    
    /**
     * Creates a teleport request from one player to another
     * 
     * @param requester The player requesting the teleport
     * @param target The target player
     * @param isTeleportTo True if the requester wants to teleport to the target, false if they want the target to teleport to them
     * @return True if the request was created successfully, false otherwise
     */
    public static boolean createTeleportRequest(ServerPlayer requester, ServerPlayer target, boolean isTeleportTo) {
        UUID requesterId = requester.getUUID();
        UUID targetId = target.getUUID();
        
        // Check if either player already has a pending request with the other
        for (Map.Entry<UUID, TeleportRequest> entry : teleportRequests.entrySet()) {
            TeleportRequest request = entry.getValue();
            
            if (request.isExpired()) {
                continue;
            }
            
            if ((request.requester.equals(requesterId) && request.target.equals(targetId)) ||
                (request.requester.equals(targetId) && request.target.equals(requesterId))) {
                return false;
            }
        }
        
        // Create new request
        // Default expiration time is 60 seconds
        long expirationTime = System.currentTimeMillis() + 60 * 1000;
        TeleportRequest request = new TeleportRequest(requesterId, targetId, isTeleportTo, expirationTime);
        
        teleportRequests.put(targetId, request);
        
        return true;
    }
    
    /**
     * Accepts a pending teleport request
     * 
     * @param player The player accepting the request
     * @return True if a request was accepted, false otherwise
     */
    public static boolean acceptTeleportRequest(ServerPlayer player) {
        UUID playerId = player.getUUID();
        
        if (!teleportRequests.containsKey(playerId)) {
            return false;
        }
        
        TeleportRequest request = teleportRequests.get(playerId);
        
        if (request.isExpired()) {
            teleportRequests.remove(playerId);
            return false;
        }
          // Get the other player
        if (player.getServer() == null) {
            teleportRequests.remove(playerId);
            return false;
        }
        
        ServerPlayer otherPlayer = player.getServer().getPlayerList().getPlayer(request.requester);
        
        if (otherPlayer == null) {
            teleportRequests.remove(playerId);
            LanguageUtil.sendErrorMessage(player, "The player who sent the teleport request is no longer online.");
            return false;
        }        // Perform teleport based on the request type
        boolean success;
        if (request.isTeleportTo) {
            // Requester teleports to target
            success = teleportPlayer(otherPlayer, (ServerLevel)player.level(), 
                    new net.minecraft.world.phys.Vec3(player.getX(), player.getY(), player.getZ()), true);
            
            if (success) {
                LanguageUtil.sendMessage(otherPlayer, "Teleporting to " + player.getScoreboardName() + ".");
                LanguageUtil.sendMessage(player, otherPlayer.getScoreboardName() + " is teleporting to you.");
            }
        } else {
            // Target teleports to requester
            success = teleportPlayer(player, (ServerLevel)otherPlayer.level(),
                    new net.minecraft.world.phys.Vec3(otherPlayer.getX(), otherPlayer.getY(), otherPlayer.getZ()), true);
            
            if (success) {
                LanguageUtil.sendMessage(player, "Teleporting to " + otherPlayer.getScoreboardName() + ".");
                LanguageUtil.sendMessage(otherPlayer, player.getScoreboardName() + " is teleporting to you.");
            }
        }
        
        // Remove the request
        teleportRequests.remove(playerId);
        
        return success;
    }
      /**
     * Denies a pending teleport request
     * 
     * @param player The player denying the request
     * @return True if a request was denied, false otherwise
     */
    public static boolean denyTeleportRequest(ServerPlayer player) {
        UUID playerId = player.getUUID();
        
        if (!teleportRequests.containsKey(playerId)) {
            return false;
        }
        
        TeleportRequest request = teleportRequests.get(playerId);
        
        // Check if the request is expired
        if (request.isExpired()) {
            teleportRequests.remove(playerId);
            return false;
        }
        
        // Get the other player if the server is available
        if (player.getServer() != null) {
            ServerPlayer otherPlayer = player.getServer().getPlayerList().getPlayer(request.requester);
            
            // Notify the requester if they're online
            if (otherPlayer != null) {
                LanguageUtil.sendMessage(otherPlayer, player.getScoreboardName() + " denied your teleport request.");
            }
        }
        
        // Remove the request
        teleportRequests.remove(playerId);
        
        return true;
    }
    
    /**
     * Checks if a player is on teleport cooldown
     * 
     * @param playerId The player's UUID
     * @return True if the player is on cooldown, false otherwise
     */
    private static boolean isOnCooldown(UUID playerId) {
        if (!teleportCooldowns.containsKey(playerId)) {
            return false;
        }
        
        long cooldownEndTime = teleportCooldowns.get(playerId);
        boolean onCooldown = System.currentTimeMillis() < cooldownEndTime;
        
        // Clean up expired cooldowns
        if (!onCooldown) {
            teleportCooldowns.remove(playerId);
        }
        
        return onCooldown;
    }
    
    /**
     * Applies a teleport cooldown to a player
     * 
     * @param playerId The player's UUID
     */
    private static void applyCooldown(UUID playerId) {
        // TODO: Get cooldown time from config
        long cooldownTimeMillis = 3000; // Default: 3 seconds
        teleportCooldowns.put(playerId, System.currentTimeMillis() + cooldownTimeMillis);
    }
    
    /**
     * Gets the remaining cooldown time for a player
     * 
     * @param playerId The player's UUID
     * @return The remaining cooldown time in milliseconds
     */
    private static long getRemainingCooldown(UUID playerId) {
        if (!isOnCooldown(playerId)) {
            return 0;
        }
        
        long cooldownEndTime = teleportCooldowns.get(playerId);
        return Math.max(0, cooldownEndTime - System.currentTimeMillis());
    }
    
    /**
     * Class to store teleport request data
     */
    private static class TeleportRequest {
        final UUID requester;
        final UUID target;
        final boolean isTeleportTo;
        final long expiration;
        
        TeleportRequest(UUID requester, UUID target, boolean isTeleportTo, long expiration) {
            this.requester = requester;
            this.target = target;
            this.isTeleportTo = isTeleportTo;
            this.expiration = expiration;
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() > expiration;
        }
    }
    
    /**
     * Class to store location data
     */
    private static class LocationData {
        final String dimension;
        final double x;
        final double y;
        final double z;
        final float yaw;
        final float pitch;
        
        LocationData(String dimension, double x, double y, double z, float yaw, float pitch) {
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }
}
