package com.zerog.neoessentials.data;

import net.minecraft.core.BlockPos;

/**
 * Represents a home location for a player
 */
public class HomeData {
    private String dimension;
    private BlockPos position;
    private float pitch;
    private float yaw;
    
    public HomeData(String dimension, BlockPos position, float pitch, float yaw) {
        this.dimension = dimension;
        this.position = position;
        this.pitch = pitch;
        this.yaw = yaw;
    }
    
    /**
     * Gets the dimension of the home
     * 
     * @return The dimension
     */
    public String getDimension() {
        return dimension;
    }
    
    /**
     * Gets the position of the home
     * 
     * @return The BlockPos
     */
    public BlockPos getPosition() {
        return position;
    }
    
    /**
     * Gets the pitch (vertical looking direction) of the player at this home
     * 
     * @return The pitch
     */
    public float getPitch() {
        return pitch;
    }
    
    /**
     * Gets the yaw (horizontal looking direction) of the player at this home
     * 
     * @return The yaw
     */
    public float getYaw() {
        return yaw;
    }
}
