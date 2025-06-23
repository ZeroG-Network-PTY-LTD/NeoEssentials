package com.zerog.neoessentials.ui.tablist.components;

import net.minecraft.server.level.ServerPlayer;

/**
 * Base interface for all tablist components
 */
public interface TablistComponent {
    /**
     * Gets the unique ID of this component
     * @return The component ID
     */
    String getId();
    
    /**
     * Gets the display name of this component
     * @return The component display name
     */
    String getDisplayName();
    
    /**
     * Called when the tablist is being updated
     * @param player The player the tablist is being updated for
     */
    void update(ServerPlayer player);
}
