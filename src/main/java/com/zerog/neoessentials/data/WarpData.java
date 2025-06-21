package com.zerog.neoessentials.data;

import net.minecraft.core.BlockPos;

/**
 * Represents a server warp location
 */
public class WarpData {
    private String name;
    private String dimension;
    private BlockPos position;
    private float pitch;
    private float yaw;
    private String permission;
    
    public WarpData(String name, String dimension, BlockPos position, float pitch, float yaw, String permission) {
        this.name = name;
        this.dimension = dimension;
        this.position = position;
        this.pitch = pitch;
        this.yaw = yaw;
        this.permission = permission;
    }
    
    /**
     * Gets the name of the warp
     * 
     * @return The name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Gets the dimension of the warp
     * 
     * @return The dimension
     */
    public String getDimension() {
        return dimension;
    }
    
    /**
     * Gets the position of the warp
     * 
     * @return The BlockPos
     */
    public BlockPos getPosition() {
        return position;
    }
    
    /**
     * Gets the pitch (vertical looking direction) of the warp
     * 
     * @return The pitch
     */
    public float getPitch() {
        return pitch;
    }
    
    /**
     * Gets the yaw (horizontal looking direction) of the warp
     * 
     * @return The yaw
     */
    public float getYaw() {
        return yaw;
    }
    
    /**
     * Gets the permission required to use this warp
     * 
     * @return The permission node, or null if no permission is required
     */
    public String getPermission() {
        return permission;
    }
    
    /**
     * Sets the permission required to use this warp
     * 
     * @param permission The permission node, or null if no permission is required
     */
    public void setPermission(String permission) {
        this.permission = permission;
    }
}
