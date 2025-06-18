package com.zerog.neoessentials.common.data;

/**
 * Data class for warp locations that is version-independent.
 * This contains just the basic data without any Minecraft-specific types.
 */
public class WarpData {
    private String name;
    private String worldName;
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;
    private String permission;
    private boolean hidden;
    
    /**
     * Default constructor required for deserialization
     */
    public WarpData() {
    }
    
    /**
     * Create a new warp with the given parameters
     * 
     * @param name The name of the warp
     * @param worldName The name of the world the warp is in
     * @param x The x coordinate of the warp
     * @param y The y coordinate of the warp
     * @param z The z coordinate of the warp
     * @param yaw The yaw angle of the warp
     * @param pitch The pitch angle of the warp
     * @param permission The permission required to use the warp
     * @param hidden Whether the warp is hidden from warp list
     */
    public WarpData(String name, String worldName, double x, double y, double z, float yaw, float pitch, String permission, boolean hidden) {
        this.name = name;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.permission = permission;
        this.hidden = hidden;
    }
    
    /**
     * Get the name of the warp
     * 
     * @return The name of the warp
     */
    public String getName() {
        return name;
    }
    
    /**
     * Set the name of the warp
     * 
     * @param name The name of the warp
     */
    public void setName(String name) {
        this.name = name;
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
    
    /**
     * Get the permission required to use the warp
     * 
     * @return The permission required to use the warp
     */
    public String getPermission() {
        return permission;
    }
    
    /**
     * Set the permission required to use the warp
     * 
     * @param permission The permission required to use the warp
     */
    public void setPermission(String permission) {
        this.permission = permission;
    }
    
    /**
     * Get whether the warp is hidden from warp list
     * 
     * @return Whether the warp is hidden from warp list
     */
    public boolean isHidden() {
        return hidden;
    }
    
    /**
     * Set whether the warp is hidden from warp list
     * 
     * @param hidden Whether the warp is hidden from warp list
     */
    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }
    
    /**
     * Convert this warp to a Location object
     * 
     * @return A Location object representing this warp
     */
    public Location toLocation() {
        return new Location(worldName, x, y, z, yaw, pitch);
    }
}
