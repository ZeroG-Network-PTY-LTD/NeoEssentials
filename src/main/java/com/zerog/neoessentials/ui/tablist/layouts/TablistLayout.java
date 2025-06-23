package com.zerog.neoessentials.ui.tablist.layouts;

/**
 * Interface for tablist layouts
 */
public interface TablistLayout {
    /**
     * Gets the name of this layout
     */
    String getName();
    
    /**
     * Gets the type of this layout
     */
    LayoutType getType();
    
    /**
     * Layout type enumeration
     */
    enum LayoutType {
        DYNAMIC,    // Dynamic player list that adjusts as players join/leave
        FIXED       // Fixed slots with assigned content
    }
}
