package com.zerog.neoessentials.ui.tablist.components;

import net.minecraft.server.level.ServerPlayer;

/**
 * Component for creating table-style layouts in the tablist
 */
public class TablistTableComponent implements TablistComponent {
    private final String id = "table";
    private final String displayName = "Table Component";
    
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
        // Will be implemented with table layout logic
    }
}
