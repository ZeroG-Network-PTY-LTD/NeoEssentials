package com.zerog.neoessentials.common.data;

import java.util.UUID;

/**
 * Data class for home locations that is version-independent.
 * This contains just the basic data without any Minecraft-specific types.
 */
public class HomeData {
    private UUID playerUuid;
    private String homeName;
    private String worldName;
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;
    
    /**
     * Default constructor required for deserialization
     */
    public HomeData() {
    }
    
    /**
     * Create a new home with the given parameters
     * 
     * @param playerUuid The UUID of the player who owns the home
     * @param homeName The name of the home
     * @param worldName The name of the world the home is in
     * @param x The x coordinate of the home
     * @param y The y coordinate of the home
     * @param z The z coordinate of the home
     * @param yaw The yaw angle of the home
     * @param pitch The pitch angle of the home
     */
    public HomeData(UUID playerUuid, String homeName, String worldName, double x, double y, double z, float yaw, float pitch) {
        this.playerUuid = playerUuid;
        this.homeName = homeName;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }
    
    /**
     * Get the player UUID
     * 
     * @return The player UUID
     */
    public UUID getPlayerUuid() {
        return playerUuid;
    }
    
    /**
     * Set the player UUID
     * 
     * @param playerUuid The player UUID
     */
    public void setPlayerUuid(UUID playerUuid) {
        this.playerUuid = playerUuid;
    }
    
    /**
     * Get the home name
     * 
     * @return The home name
     */
    public String getHomeName() {
        return homeName;
    }
    
    /**
     * Set the home name
     * 
     * @param homeName The home name
     */
    public void setHomeName(String homeName) {
        this.homeName = homeName;
    }
    
    /**
     * Get the world name
     * 
     * @return The world name
     */
    public String getWorldName() {
        return worldName;
    }
    
    /**
     * Set the world name
     * 
     * @param worldName The world name
     */
    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }
    
    /**
     * Get the x coordinate
     * 
     * @return The x coordinate
     */
    public double getX() {
        return x;
    }
    
    /**
     * Set the x coordinate
     * 
     * @param x The x coordinate
     */
    public void setX(double x) {
        this.x = x;
    }
    
    /**
     * Get the y coordinate
     * 
     * @return The y coordinate
     */
    public double getY() {
        return y;
    }
    
    /**
     * Set the y coordinate
     * 
     * @param y The y coordinate
     */
    public void setY(double y) {
        this.y = y;
    }
    
    /**
     * Get the z coordinate
     * 
     * @return The z coordinate
     */
    public double getZ() {
        return z;
    }
    
    /**
     * Set the z coordinate
     * 
     * @param z The z coordinate
     */
    public void setZ(double z) {
        this.z = z;
    }
    
    /**
     * Get the yaw angle
     * 
     * @return The yaw angle
     */
    public float getYaw() {
        return yaw;
    }
    
    /**
     * Set the yaw angle
     * 
     * @param yaw The yaw angle
     */
    public void setYaw(float yaw) {
        this.yaw = yaw;
    }
    
    /**
     * Get the pitch angle
     * 
     * @return The pitch angle
     */
    public float getPitch() {
        return pitch;
    }
    
    /**
     * Set the pitch angle
     * 
     * @param pitch The pitch angle
     */
    public void setPitch(float pitch) {
        this.pitch = pitch;
    }
}
