package com.neoessentials.api.home.data;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Data class representing a player's home location
 * Similar to EssentialsX home storage format
 */
public class HomeData {
    private final String name;
    private final ResourceKey<Level> dimension;
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;
    private final long createdAt;
    
    public HomeData(String name, ResourceKey<Level> dimension, double x, double y, double z, 
                   float yaw, float pitch, long createdAt) {
        this.name = name;
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.createdAt = createdAt;
    }
    
    public HomeData(String name, ResourceKey<Level> dimension, BlockPos pos, float yaw, float pitch) {
        this(name, dimension, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, pitch, 
             System.currentTimeMillis());
    }
    
    // Getters
    public String getName() { return name; }
    public ResourceKey<Level> getDimension() { return dimension; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
    public long getCreatedAt() { return createdAt; }
    
    public BlockPos getBlockPos() {
        return new BlockPos((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
    }
    
    @Override
    public String toString() {
        return String.format("HomeData{name='%s', dimension=%s, x=%.2f, y=%.2f, z=%.2f, yaw=%.2f, pitch=%.2f}",
                name, dimension.location(), x, y, z, yaw, pitch);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        HomeData homeData = (HomeData) obj;
        return Double.compare(homeData.x, x) == 0 &&
               Double.compare(homeData.y, y) == 0 &&
               Double.compare(homeData.z, z) == 0 &&
               Float.compare(homeData.yaw, yaw) == 0 &&
               Float.compare(homeData.pitch, pitch) == 0 &&
               name.equals(homeData.name) &&
               dimension.equals(homeData.dimension);
    }
    
    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + dimension.hashCode();
        result = 31 * result + Double.hashCode(x);
        result = 31 * result + Double.hashCode(y);
        result = 31 * result + Double.hashCode(z);
        result = 31 * result + Float.hashCode(yaw);
        result = 31 * result + Float.hashCode(pitch);
        return result;
    }
}
