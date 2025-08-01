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
        // Basic safety checks
        if (location.y < -64 || location.y > 320) {
            return false; // Outside world limits (1.18+ world height)
        }
        
        // Check for dangerous Y levels
        if (location.y <= 0 && location.world.contains("nether")) {
            return false; // Below bedrock in nether
        }
        
        if (location.y < 5 && location.world.contains("overworld")) {
            return false; // Near bedrock/void in overworld
        }
        
        // Additional safety checks would require world access:
        // - Check for solid blocks to stand on
        // - Check for air blocks above (no suffocation)
        // - Check for lava/fire/dangerous blocks
        // - Check for protected regions
        
        return true;
    }
    
    /**
     * Find a safe location near the given location
     */
    public static Location findSafeLocation(Location location) {
        // Try the original location first
        if (isSafeLocation(location)) {
            return location;
        }
        
        // Try locations in a spiral pattern around the original
        for (int radius = 1; radius <= 10; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) == radius || Math.abs(dz) == radius) {
                        Location testLocation = new Location(
                            location.world,
                            location.x + dx,
                            location.y,
                            location.z + dz,
                            location.yaw,
                            location.pitch
                        );
                        
                        if (isSafeLocation(testLocation)) {
                            return testLocation;
                        }
                    }
                }
            }
        }
        
        // If no safe location found, return original with adjusted Y
        double safeY = Math.max(64, Math.min(200, location.y));
        return new Location(location.world, location.x, safeY, location.z, location.yaw, location.pitch);
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
    
    /**
     * Format location with detailed information
     */
    public static String formatDetailedLocation(Location location) {
        return String.format("World: %s, X: %.2f, Y: %.2f, Z: %.2f, Yaw: %.1f°, Pitch: %.1f°",
            location.world, location.x, location.y, location.z, location.yaw, location.pitch);
    }
    
    /**
     * Get block coordinates (rounded down)
     */
    public static String formatBlockCoordinates(Location location) {
        return String.format("%d, %d, %d", 
            (int) Math.floor(location.x), 
            (int) Math.floor(location.y), 
            (int) Math.floor(location.z));
    }
    
    /**
     * Calculate 2D distance (ignoring Y coordinate)
     */
    public static double distance2D(Location loc1, Location loc2) {
        if (!loc1.sameWorld(loc2)) {
            return Double.MAX_VALUE;
        }
        
        double dx = loc1.x - loc2.x;
        double dz = loc1.z - loc2.z;
        return Math.sqrt(dx * dx + dz * dz);
    }
    
    /**
     * Check if location is within a certain distance of another
     */
    public static boolean isWithinDistance(Location loc1, Location loc2, double maxDistance) {
        return loc1.distance(loc2) <= maxDistance;
    }
    
    /**
     * Create location from ServerPlayer
     */
    public static Location fromServerPlayer(net.minecraft.server.level.ServerPlayer player) {
        return new Location(
            player.level().dimension().location().getPath(),
            player.getX(),
            player.getY(),
            player.getZ(),
            player.getYRot(),
            player.getXRot()
        );
    }
    
    /**
     * Create location from coordinates with current timestamp
     */
    public static Location fromCoordinates(String world, double x, double y, double z) {
        return new Location(world, x, y, z, 0.0f, 0.0f);
    }
    
    /**
     * Create location from coordinates with rotation
     */
    public static Location fromCoordinates(String world, double x, double y, double z, float yaw, float pitch) {
        return new Location(world, x, y, z, yaw, pitch);
    }
    
    /**
     * Check if a location is in a dangerous area
     */
    public static boolean isDangerousLocation(Location location) {
        // Check for void
        if (location.y < 0) {
            return true;
        }
        
        // Check for very high locations (above build limit)
        if (location.y > 319) {
            return true;
        }
        
        // Check for nether ceiling
        if (location.world.contains("nether") && location.y > 126) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Get a spawn-safe location (for when players join)
     */
    public static Location getSpawnSafeLocation(Location baseLocation) {
        Location safeLocation = findSafeLocation(baseLocation);
        
        // Make sure spawn location is not too high or low
        if (safeLocation.y < 64) {
            safeLocation = new Location(safeLocation.world, safeLocation.x, 64, safeLocation.z, 
                                      safeLocation.yaw, safeLocation.pitch);
        }
        
        if (safeLocation.y > 200) {
            safeLocation = new Location(safeLocation.world, safeLocation.x, 200, safeLocation.z, 
                                      safeLocation.yaw, safeLocation.pitch);
        }
        
        return safeLocation;
    }
}
