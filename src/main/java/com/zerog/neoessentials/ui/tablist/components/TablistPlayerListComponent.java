package com.zerog.neoessentials.ui.tablist.components;

import net.minecraft.server.level.ServerPlayer;

/**
 * Component that displays a dynamic player list in the tablist
 */
public class TablistPlayerListComponent implements TablistComponent {
    private final String id = "playerlist";
    private final String displayName = "Dynamic Player List";
    
    /**
     * Gets the unique ID of this component
     */
    @Override
    public String getId() {
        return id;
    }
    
    /**
     * Gets the display name of this component
     */
    @Override
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Called when the tablist is being updated
     */
    @Override
    public void update(ServerPlayer player) {
        // Will be implemented with player list update logic
    }
}
