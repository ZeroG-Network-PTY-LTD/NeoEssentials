package com.zerog.neoessentials.common.adapter;

/**
 * Interface for adapter factories that create version-specific implementations
 * This allows the common module to define abstract adapter classes
 * while version-specific modules provide the implementations
 */
public interface AdapterFactory {
    /**
     * Get the location adapter for the current version
     * @return A location adapter implementation
     */
    ILocationAdapter getLocationAdapter();
    
    /**
     * Get the permissions adapter for the current version
     * @return A permissions adapter implementation
     */
    IPermissionAdapter getPermissionAdapter();
    
    /**
     * Get the player adapter for the current version
     * @return A player adapter implementation
     */
    IPlayerAdapter getPlayerAdapter();
    
    /**
     * Get the command adapter for the current version
     * @return A command adapter implementation
     */
    ICommandAdapter getCommandAdapter();
}
