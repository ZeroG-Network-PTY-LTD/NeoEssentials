package com.zerog.neoessentials.economy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * Represents the location of a shop for teleportation purposes.
 * This class stores dimensional coordinates for shop teleportation.
 */
public class ShopLocation {
    private String dimension;
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;

    /**
     * Default constructor for serialization
     */
    public ShopLocation() {
    }

    /**
     * Create a new shop location
     * 
     * @param dimension The dimension identifier
     * @param x The x coordinate
     * @param y The y coordinate
     * @param z The z coordinate
     * @param yaw The yaw rotation
     * @param pitch The pitch rotation
     */
    public ShopLocation(String dimension, double x, double y, double z, float yaw, float pitch) {
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    /**
     * Create a shop location from a player's current position
     * 
     * @param player The player to get the location from
     */
    public ShopLocation(ServerPlayer player) {
        this.dimension = player.level().dimension().location().toString();
        this.x = player.getX();
        this.y = player.getY();
        this.z = player.getZ();
        this.yaw = player.getYRot();
        this.pitch = player.getXRot();
    }

    /**
     * Get the server level for this location
     * 
     * @param server The Minecraft server
     * @return The server level, or null if the dimension doesn't exist
     */
    public ServerLevel getLevel(MinecraftServer server) {
        if (server == null || dimension == null) {
            return null;
        }

        // Parse the dimension string
        ResourceLocation dimensionRL = ResourceLocation.tryParse(dimension);
        if (dimensionRL == null) {
            return null;
        }

        // Get the level
        ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, dimensionRL);
        return server.getLevel(dimensionKey);
    }

    /**
     * Get the BlockPos for this location
     * 
     * @return The BlockPos
     */
    public BlockPos getBlockPos() {
        return BlockPos.containing(x, y, z);
    }

    /**
     * Check if this location is valid (has required data)
     * 
     * @return True if location is valid
     */
    public boolean isValid() {
        return dimension != null && !dimension.isEmpty();
    }

    /**
     * Get a formatted string representation of this location
     * 
     * @return Formatted location string
     */
    public String getFormattedLocation() {
        if (!isValid()) {
            return "Invalid Location";
        }
        
        String dimName = dimension.contains(":") ? dimension.split(":")[1] : dimension;
        return String.format("%s (%.1f, %.1f, %.1f)", 
            dimName, x, y, z);
    }

    /**
     * Teleport a player to this shop location
     * 
     * @param player The player to teleport
     * @return True if teleportation was successful
     */
    public boolean teleportPlayer(ServerPlayer player) {
        if (!isValid()) {
            return false;
        }
        
        try {
            MinecraftServer server = player.getServer();
            if (server == null) {
                return false;
            }
            
            ServerLevel targetLevel = getLevel(server);
            if (targetLevel == null) {
                return false;
            }
            
            // Teleport the player
            player.teleportTo(targetLevel, x, y, z, yaw, pitch);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Getters and setters
    public String getDimension() {
        return dimension;
    }

    public void setDimension(String dimension) {
        this.dimension = dimension;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    @Override
    public String toString() {
        return String.format("ShopLocation{dimension='%s', x=%.2f, y=%.2f, z=%.2f, yaw=%.2f, pitch=%.2f}",
                dimension, x, y, z, yaw, pitch);
    }
}
