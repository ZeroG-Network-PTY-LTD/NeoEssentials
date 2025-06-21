package com.zerog.neoessentials.common.adapter;

import com.zerog.neoessentials.common.data.Location;

/**
 * Interface for location adapters that convert between common Location objects
 * and version-specific Minecraft world/position objects
 */
public interface ILocationAdapter {
    /**
     * Convert a common Location to version-specific objects
     * @param location The common Location object
     * @param playerRef An optional player reference for context (e.g., dimension resolution)
     * @return Implementation-specific array of objects representing the location
     */
    Object[] fromCommonLocation(Location location, Object playerRef);
    
    /**
     * Convert version-specific world and position to a common Location
     * @param worldRef The version-specific world/level/dimension reference
     * @param posRef The version-specific position reference
     * @param playerRef An optional player reference for additional data like rotation
     * @return A common Location object
     */
    Location toCommonLocation(Object worldRef, Object posRef, Object playerRef);
    
    /**
     * Create a common Location from a player's current position
     * @param playerRef The version-specific player reference
     * @return A common Location object
     */
    Location fromPlayer(Object playerRef);
}
