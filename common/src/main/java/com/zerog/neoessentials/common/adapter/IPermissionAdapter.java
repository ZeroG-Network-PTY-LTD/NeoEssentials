package com.zerog.neoessentials.common.adapter;

/**
 * Interface for permission adapters that handle permission checking
 * across different Minecraft versions and permission systems
 */
public interface IPermissionAdapter {
    /**
     * Check if a player has a specific permission
     * @param playerRef The version-specific player reference
     * @param permission The permission node to check
     * @return true if the player has the permission, false otherwise
     */
    boolean hasPermission(Object playerRef, String permission);
    
    /**
     * Check if a player has a specific permission or is server operator
     * @param playerRef The version-specific player reference
     * @param permission The permission node to check
     * @return true if the player has the permission or is an operator, false otherwise
     */
    boolean hasPermissionOrOp(Object playerRef, String permission);
    
    /**
     * Check if a player is a server operator
     * @param playerRef The version-specific player reference
     * @return true if the player is an operator, false otherwise
     */
    boolean isOp(Object playerRef);
    
    /**
     * Initialize the permission adapter
     * This may involve setting up hooks to permission systems like LuckPerms
     */
    void initialize();
}
