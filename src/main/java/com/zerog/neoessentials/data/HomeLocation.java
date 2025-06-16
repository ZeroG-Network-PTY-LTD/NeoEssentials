package com.zerog.neoessentials.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Represents the location of a home
 */
public class HomeLocation {
    private String dimension;
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;
    
    /**
     * Creates a new home location
     * 
     * @param dimension The dimension ID (e.g. "minecraft:overworld")
     * @param x The X coordinate
     * @param y The Y coordinate
     * @param z The Z coordinate
     * @param yaw The yaw rotation
     * @param pitch The pitch rotation
     */
    public HomeLocation(String dimension, double x, double y, double z, float yaw, float pitch) {
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }
    
    /**
     * Gets the dimension ID
     * 
     * @return The dimension ID
     */
    public String getDimension() {
        return dimension;
    }
    
    /**
     * Gets the X coordinate
     * 
     * @return The X coordinate
     */
    public double getX() {
        return x;
    }
    
    /**
     * Gets the Y coordinate
     * 
     * @return The Y coordinate
     */
    public double getY() {
        return y;
    }
    
    /**
     * Gets the Z coordinate
     * 
     * @return The Z coordinate
     */
    public double getZ() {
        return z;
    }
    
    /**
     * Gets the yaw rotation
     * 
     * @return The yaw rotation
     */
    public float getYaw() {
        return yaw;
    }
    
    /**
     * Gets the pitch rotation
     * 
     * @return The pitch rotation
     */
    public float getPitch() {
        return pitch;
    }
    
    /**
     * Gets the server level for this home location
     * 
     * @param server The Minecraft server
     * @return The server level, or null if the dimension doesn't exist
     */
    public ServerLevel getLevel(MinecraftServer server) {
        try {
            ResourceLocation dimLocation = ResourceLocation.tryParse(dimension);
            if (dimLocation == null) {
                return server.overworld(); // Default to overworld
            }
            
            ResourceKey<Level> levelKey = ResourceKey.create(Registries.DIMENSION, dimLocation);
            return server.getLevel(levelKey);
        } catch (Exception e) {
            // Return the overworld as fallback
            return server.overworld();
        }
    }
    
    /**
     * Gets the BlockPos for this location
     * 
     * @return The BlockPos
     */
    public BlockPos getBlockPos() {
        return BlockPos.containing(x, y, z);
    }
}
