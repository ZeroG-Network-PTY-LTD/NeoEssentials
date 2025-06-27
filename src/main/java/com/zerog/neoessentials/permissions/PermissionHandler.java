package com.zerog.neoessentials.permissions;

import net.minecraft.server.level.ServerPlayer;

/**
 * Interface for permission system handlers.
 * Implementations will handle permission checks for different backends (LuckPerms, etc.)
 */
public interface PermissionHandler {
    
    /**
     * Check if a player has a specific permission
     * 
     * @param player The player to check
     * @param permission The permission node to check
     * @return true if the player has the permission, false otherwise
     */
    boolean hasPermission(ServerPlayer player, String permission);
    
    /**
     * Check if this permission handler is available
     * 
     * @return true if the permission system is available
     */
    boolean isAvailable();
    
    /**
     * Get the name of the permission handler
     * 
     * @return The name of the permission handler
     */
    String getName();
}
