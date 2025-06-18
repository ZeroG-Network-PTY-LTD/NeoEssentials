<<<<<<< HEAD
public class LocationAdapter {
    
=======
package com.zerog.neoessentials.adapter;

import com.zerog.neoessentials.common.adapter.ILocationAdapter;
import com.zerog.neoessentials.common.data.Location;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * NeoForge 1.20.1 implementation of the location adapter
 */
public class LocationAdapter implements ILocationAdapter {
    /**
     * Converts a common Location object to a Minecraft world and position
     * 
     * @param location The location to convert
     * @param playerRef A reference player for dimension lookup if world name doesn't match
     * @return An array with [0] = ServerLevel, [1] = BlockPos, or null if conversion failed
     */
    @Override
    public Object[] fromCommonLocation(Location location, Object playerRef) {
        if (location == null) return null;
        ServerPlayer player = playerRef instanceof ServerPlayer ? (ServerPlayer)playerRef : null;
        
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
     * @param worldRef The Minecraft world
     * @param posRef The position in the world
     * @param playerRef Optional player for rotation data
     * @return A common Location object
     */
    @Override
    public Location toCommonLocation(Object worldRef, Object posRef, Object playerRef) {
        if (!(worldRef instanceof Level) || !(posRef instanceof BlockPos)) return null;
        
        Level level = (Level)worldRef;
        BlockPos pos = (BlockPos)posRef;
        ServerPlayer player = playerRef instanceof ServerPlayer ? (ServerPlayer)playerRef : null;
        
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
     * @param playerRef The player
     * @return A common Location object
     */
    @Override
    public Location fromPlayer(Object playerRef) {
        if (!(playerRef instanceof ServerPlayer)) return null;
        
        ServerPlayer player = (ServerPlayer)playerRef;
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
>>>>>>> 7ac3350 (feat: Implement NeoEssentials for NeoForge 1.20.1 and 1.20.5)
}
