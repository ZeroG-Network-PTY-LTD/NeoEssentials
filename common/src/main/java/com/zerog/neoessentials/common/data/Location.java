package com.zerog.neoessentials.common.data;

/**
 * Simple location class that is version-independent.
 * This is used to store location data without requiring Minecraft-specific classes.
 */
public class Location {
    private String worldName;
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;
    
    /**
     * Default constructor required for deserialization
     */
    public Location() {
    }
    
    /**
     * Create a new location with the given parameters
     * 
     * @param worldName The name of the world
     * @param x The x coordinate
     * @param y The y coordinate
     * @param z The z coordinate
     * @param yaw The yaw angle
     * @param pitch The pitch angle
     */
    public Location(String worldName, double x, double y, double z, float yaw, float pitch) {
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }
    
    /**
     * Create a new location with the given parameters (default yaw and pitch)
     * 
     * @param worldName The name of the world
     * @param x The x coordinate
     * @param y The y coordinate
     * @param z The z coordinate
     */
    public Location(String worldName, double x, double y, double z) {
        this(worldName, x, y, z, 0, 0);
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
     * Get the distance squared to another location
     * 
     * @param other The other location
     * @return The distance squared
     */
    public double distanceSquared(Location other) {
        if (!worldName.equals(other.worldName)) {
            return Double.MAX_VALUE;
        }
        
        double dx = x - other.x;
        double dy = y - other.y;
        double dz = z - other.z;
        return dx * dx + dy * dy + dz * dz;
    }
    
    /**
     * Get the distance to another location
     * 
     * @param other The other location
     * @return The distance
     */
    public double distance(Location other) {
        return Math.sqrt(distanceSquared(other));
    }
    
    @Override
    public String toString() {
        return String.format("Location{world=%s, x=%.2f, y=%.2f, z=%.2f, yaw=%.2f, pitch=%.2f}", 
                worldName, x, y, z, yaw, pitch);
    }
    
    /**
     * Get a formatted string representation of the location
     * 
     * @return A formatted string
     */
    public String toFormattedString() {
        return String.format("World: %s, X: %.2f, Y: %.2f, Z: %.2f", worldName, x, y, z);
    }
    
    /**
     * Get a formatted string representation of the location with block coordinates
     * 
     * @return A formatted string
     */
    public String toBlockString() {
        return String.format("World: %s, X: %d, Y: %d, Z: %d", 
                worldName, (int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
    }
}
