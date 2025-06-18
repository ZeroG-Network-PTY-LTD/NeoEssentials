package com.zerog.neoessentials.adapter;

import com.zerog.neoessentials.common.data.Location;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * Adapter class for NeoForge 1.21.1 that bridges between common module data and Minecraft specific classes
 */
public class LocationAdapter {
    /**
     * Converts a common Location object to a Minecraft world and position
     * 
     * @param location The location to convert
     * @param player A reference player for dimension lookup if world name doesn't match
     * @return An array with [0] = ServerLevel, [1] = BlockPos, or null if conversion failed
     */
    public static Object[] fromCommonLocation(Location location, ServerPlayer player) {
        if (location == null) return null;
        
        // Find the world by name
        ServerLevel level = null;
        for (ServerLevel serverLevel : ServerLifecycleHooks.getCurrentServer().getAllLevels()) {
            if (serverLevel.dimension().location().toString().equals(location.getWorldName()) ||
                serverLevel.dimension().location().getPath().equals(location.getWorldName())) {
                level = serverLevel;
                break;
            }
        }
        
        // If we couldn't find the world, use the player's current world as fallback
        if (level == null && player != null) {
            level = player.getLevel();
        }
        
        if (level == null) return null;
        
        // Create BlockPos
        BlockPos pos = new BlockPos((int)location.getX(), (int)location.getY(), (int)location.getZ());
        
        return new Object[] { level, pos };
    }
    
    /**
     * Converts Minecraft world and position to a common Location object
     * 
     * @param level The Minecraft world
     * @param pos The position in the world
     * @param player Optional player for rotation data
     * @return A common Location object
     */
    public static Location toCommonLocation(Level level, BlockPos pos, ServerPlayer player) {
        if (level == null || pos == null) return null;
        
        String worldName = level.dimension().location().toString();
        double x = pos.getX() + 0.5;
        double y = pos.getY();
        double z = pos.getZ() + 0.5;
        float yaw = player != null ? player.getYRot() : 0;
        float pitch = player != null ? player.getXRot() : 0;
        
        return new Location(worldName, x, y, z, yaw, pitch);
    }
    
    /**
     * Creates a common Location from a player's position
     * 
     * @param player The player
     * @return A common Location object
     */
    public static Location fromPlayer(ServerPlayer player) {
        if (player == null) return null;
        
        String worldName = player.getLevel().dimension().location().toString();
        return new Location(
            worldName,
            player.getX(),
            player.getY(),
            player.getZ(),
            player.getYRot(),
            player.getXRot()
        );
    }
}
