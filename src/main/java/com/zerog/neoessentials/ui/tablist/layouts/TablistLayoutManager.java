package com.zerog.neoessentials.ui.tablist.layouts;

import com.zerog.neoessentials.NeoEssentials;

/**
 * Manager class for tablist layouts
 */
public class TablistLayoutManager {
    private TablistLayout activeLayout;
    
    /**
     * Creates a new TablistLayoutManager
     */
    public TablistLayoutManager() {
        // Default to dynamic layout
        activeLayout = new TablistDynamicLayout();
    }
    
    /**
     * Sets the active layout
     * @param layout The layout to set as active
     */
    public void setActiveLayout(TablistLayout layout) {
        if (layout != null) {
            this.activeLayout = layout;
            NeoEssentials.LOGGER.info("Set active tablist layout to: {}", layout.getName());
        }
    }
    
    /**
     * Gets the active layout
     * @return The active layout
     */
    public TablistLayout getActiveLayout() {
        return activeLayout;
    }
}
