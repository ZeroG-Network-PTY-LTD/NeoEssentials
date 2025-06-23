package com.zerog.neoessentials.ui.tablist.layouts;

/**
 * Implementation of a dynamic tablist layout that changes as players join
 */
public class TablistDynamicLayout implements TablistLayout {
    /**
     * Gets the name of this layout
     */
    @Override
    public String getName() {
        return "Dynamic Player List";
    }
    
    /**
     * Gets the type of this layout
     */
    @Override
    public LayoutType getType() {
        return LayoutType.DYNAMIC;
    }
}
