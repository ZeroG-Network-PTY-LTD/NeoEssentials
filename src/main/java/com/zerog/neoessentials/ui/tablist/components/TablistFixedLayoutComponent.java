package com.zerog.neoessentials.ui.tablist.components;

import net.minecraft.server.level.ServerPlayer;

/**
 * Component for displaying a fixed layout in the tablist
 */
public class TablistFixedLayoutComponent implements TablistComponent {
    private final String id = "fixed_layout";
    private final String displayName = "Fixed Layout";
    
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
        // Will be implemented with fixed layout logic
    }
}
