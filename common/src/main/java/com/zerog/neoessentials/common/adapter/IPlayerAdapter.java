package com.zerog.neoessentials.common.adapter;

/**
 * Interface for player adapters that handle player operations
 * across different Minecraft versions
 */
public interface IPlayerAdapter {
    /**
     * Get a player's username
     * @param playerRef The version-specific player reference
     * @return The player's username
     */
    String getUsername(Object playerRef);
    
    /**
     * Get a player's UUID as a string
     * @param playerRef The version-specific player reference
     * @return The player's UUID as a string
     */
    String getUniqueId(Object playerRef);
    
    /**
     * Send a message to a player
     * @param playerRef The version-specific player reference
     * @param message The message to send
     */
    void sendMessage(Object playerRef, String message);
    
    /**
     * Teleport a player to a location
     * @param playerRef The version-specific player reference
     * @param worldRef The version-specific world reference
     * @param x The x coordinate
     * @param y The y coordinate
     * @param z The z coordinate
     * @param yaw The yaw rotation
     * @param pitch The pitch rotation
     * @return true if the teleport was successful, false otherwise
     */
    boolean teleport(Object playerRef, Object worldRef, double x, double y, double z, float yaw, float pitch);
}
