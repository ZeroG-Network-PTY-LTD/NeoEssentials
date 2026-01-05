package com.zerog.neoessentials.teleportation;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * Represents a teleportation location with world, position, and rotation data
 */
public class TeleportLocation {
    private final String worldName;
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;
    private final long timestamp;
    private final String createdBy;
    
    public TeleportLocation(String worldName, double x, double y, double z, float yaw, float pitch, String createdBy) {
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.timestamp = System.currentTimeMillis();
        this.createdBy = createdBy;
    }
    
    public TeleportLocation(ServerPlayer player) {
        this(player.level().dimension().location().toString(),
             player.getX(),
             player.getY(), 
             player.getZ(),
             player.getYRot(),
             player.getXRot(),
             player.getName().getString());
    }
    
    public TeleportLocation(ServerLevel level, BlockPos pos, float yaw, float pitch, String createdBy) {
        this(level.dimension().location().toString(),
             pos.getX() + 0.5,
             pos.getY(),
             pos.getZ() + 0.5,
             yaw,
             pitch,
             createdBy);
    }
    
    // Getters
    public String getWorldName() { return worldName; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
    public long getTimestamp() { return timestamp; }
    public String getCreatedBy() { return createdBy; }
    
    /**
     * Get the ServerLevel for this location
     */
    public ServerLevel getLevel() {
        try {
            ResourceLocation worldKey;

            // Parse ResourceLocation manually to avoid classloading issues
            if (worldName.contains(":")) {
                String[] parts = worldName.split(":", 2);
                worldKey = ResourceLocation.fromNamespaceAndPath(parts[0], parts[1]);
            } else {
                worldKey = ResourceLocation.fromNamespaceAndPath("minecraft", worldName);
            }

            return ServerLifecycleHooks.getCurrentServer().getLevel(
                net.minecraft.resources.ResourceKey.create(
                    net.minecraft.core.registries.Registries.DIMENSION, 
                    worldKey
                )
            );
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Check if this location is safe for teleportation
     */
    public boolean isSafe() {
        ServerLevel level = getLevel();
        if (level == null) return false;
        
        BlockPos pos = new BlockPos((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
        
        // Check if the location is loaded
        if (!level.isLoaded(pos)) return false;
        
        // Check if there's solid ground and air space for player
        BlockPos ground = pos.below();
        BlockPos feet = pos;
        BlockPos head = pos.above();
        
        // Need solid ground and air for feet/head
        boolean solidGround = !level.getBlockState(ground).isAir() && level.getBlockState(ground).canOcclude();
        boolean feetFree = level.getBlockState(feet).isAir();
        boolean headFree = level.getBlockState(head).isAir();
        
        return solidGround && feetFree && headFree;
    }
    
    /**
     * Find a safe location near this position
     */
    public TeleportLocation findSafeLocation() {
        ServerLevel level = getLevel();
        if (level == null) return null;
        
        BlockPos startPos = new BlockPos((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
        
        // Try the original location first
        if (isSafe()) return this;
        
        // Search in expanding radius
        for (int radius = 1; radius <= 16; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) != radius && Math.abs(dz) != radius) continue;
                    
                    BlockPos searchPos = startPos.offset(dx, 0, dz);
                    
                    // Try different Y levels
                    for (int dy = -8; dy <= 8; dy++) {
                        BlockPos testPos = searchPos.offset(0, dy, 0);
                        TeleportLocation testLoc = new TeleportLocation(level, testPos, yaw, pitch, createdBy);
                        
                        if (testLoc.isSafe()) {
                            return testLoc;
                        }
                    }
                }
            }
        }
        
        return null; // No safe location found
    }
    
    /**
     * Get formatted coordinates string
     */
    public String getCoordinatesString() {
        return String.format("%.1f, %.1f, %.1f", x, y, z);
    }
    
    /**
     * Get formatted location string with world
     */
    public String getLocationString() {
        return String.format("%s (%.1f, %.1f, %.1f)", getWorldDisplayName(), x, y, z);
    }
    
    /**
     * Get display name for world
     */
    public String getWorldDisplayName() {
        if (worldName.contains("overworld")) return "Overworld";
        if (worldName.contains("nether")) return "Nether";
        if (worldName.contains("end")) return "End";
        return worldName;
    }
    
    /**
     * Calculate distance to another location (same world only)
     */
    public double distanceTo(TeleportLocation other) {
        if (!worldName.equals(other.worldName)) return Double.MAX_VALUE;
        
        double dx = x - other.x;
        double dy = y - other.y;
        double dz = z - other.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
    
    /**
     * Serialize to JSON
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("world", worldName);
        json.addProperty("x", x);
        json.addProperty("y", y);
        json.addProperty("z", z);
        json.addProperty("yaw", yaw);
        json.addProperty("pitch", pitch);
        json.addProperty("timestamp", timestamp);
        json.addProperty("createdBy", createdBy);
        return json;
    }
    
    /**
     * Convert to storable string format (JSON) for player data
     */
    public String toLocationString() {
        return toJson().toString();
    }
    
    /**
     * Deserialize from JSON
     */
    public static TeleportLocation fromJson(JsonObject json) {
        try {
            String world = json.get("world").getAsString();
            double x = json.get("x").getAsDouble();
            double y = json.get("y").getAsDouble();
            double z = json.get("z").getAsDouble();
            float yaw = json.has("yaw") ? json.get("yaw").getAsFloat() : 0.0f;
            float pitch = json.has("pitch") ? json.get("pitch").getAsFloat() : 0.0f;
            String createdBy = json.has("createdBy") ? json.get("createdBy").getAsString() : "Unknown";
            
            TeleportLocation location = new TeleportLocation(world, x, y, z, yaw, pitch, createdBy);
            // Timestamp is set in constructor, but we can preserve the original if present
            return location;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Parse location from string format (JSON or simplified format)
     */
    public static TeleportLocation fromLocationString(String locationString) {
        if (locationString == null || locationString.trim().isEmpty()) {
            return null;
        }
        
        try {
            // Try to parse as JSON first (preferred format)
            if (locationString.trim().startsWith("{")) {
                JsonObject json = com.google.gson.JsonParser.parseString(locationString).getAsJsonObject();
                return fromJson(json);
            }
            
            // Try to parse simplified format: "world,x,y,z,yaw,pitch"
            String[] parts = locationString.split(",");
            if (parts.length >= 4) {
                String world = parts[0].trim();
                double x = Double.parseDouble(parts[1].trim());
                double y = Double.parseDouble(parts[2].trim());
                double z = Double.parseDouble(parts[3].trim());
                float yaw = parts.length > 4 ? Float.parseFloat(parts[4].trim()) : 0.0f;
                float pitch = parts.length > 5 ? Float.parseFloat(parts[5].trim()) : 0.0f;
                
                return new TeleportLocation(world, x, y, z, yaw, pitch, "System");
            }
            
        } catch (Exception e) {
            // Log error and return null for invalid format
        }
        
        return null;
    }
    
    @Override
    public String toString() {
        return String.format("TeleportLocation{world=%s, x=%.2f, y=%.2f, z=%.2f, yaw=%.1f, pitch=%.1f}", 
                           worldName, x, y, z, yaw, pitch);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof TeleportLocation)) return false;
        
        TeleportLocation other = (TeleportLocation) obj;
        return worldName.equals(other.worldName) &&
               Math.abs(x - other.x) < 0.1 &&
               Math.abs(y - other.y) < 0.1 &&
               Math.abs(z - other.z) < 0.1;
    }
    
    @Override
    public int hashCode() {
        return worldName.hashCode() ^ Double.hashCode(x) ^ Double.hashCode(y) ^ Double.hashCode(z);
    }
}