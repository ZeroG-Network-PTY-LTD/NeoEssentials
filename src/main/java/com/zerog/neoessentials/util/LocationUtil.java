package com.zerog.neoessentials.util;

/**
 * Utility class for handling location data
 * Shared between different managers to avoid circular dependencies
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class LocationUtil {
    
    /**
     * General location data class
     */
    public static class Location {
        public final String world;
        public final double x;
        public final double y;
        public final double z;
        public final float yaw;
        public final float pitch;
        public final long timestamp;
        
        public Location(String world, double x, double y, double z, float yaw, float pitch, long timestamp) {
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
            this.timestamp = timestamp;
        }
        
        public Location(String world, double x, double y, double z, float yaw, float pitch) {
            this(world, x, y, z, yaw, pitch, System.currentTimeMillis());
        }
        
        /**
         * Calculate distance between two locations (ignores world)
         */
        public double distance(Location other) {
            double dx = this.x - other.x;
            double dy = this.y - other.y;
            double dz = this.z - other.z;
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
        
        /**
         * Check if two locations are in the same world
         */
        public boolean sameWorld(Location other) {
            return this.world.equals(other.world);
        }
        
        @Override
        public String toString() {
            return String.format("Location{world='%s', x=%.2f, y=%.2f, z=%.2f, yaw=%.2f, pitch=%.2f}", 
                world, x, y, z, yaw, pitch);
        }
    }
    
    /**
     * Check if a location is safe for teleportation
     */
    public static boolean isSafeLocation(Location location) {
        // Basic safety checks - would be improved with actual world access
        if (location.y < 0 || location.y > 256) {
            return false;
        }
        
        // Additional safety checks would go here:
        // - Check for solid blocks
        // - Check for lava/void
        // - Check for suffocation
        
        return true;
    }
    
    /**
     * Format location for display
     */
    public static String formatLocation(Location location) {
        return String.format("%s: %.1f, %.1f, %.1f", 
            location.world, location.x, location.y, location.z);
    }
    
    /**
     * Format coordinates only
     */
    public static String formatCoordinates(Location location) {
        return String.format("%.1f, %.1f, %.1f", location.x, location.y, location.z);
    }
}
