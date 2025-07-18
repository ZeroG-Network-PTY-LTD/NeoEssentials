package com.zerog.neoessentials.features.tablist;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.config.TabListConfig;
import com.zerog.neoessentials.placeholders.PlaceholderManager;

/**
 * Tab list manager for NeoEssentials
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class TabListManager {
    
    private final TabListConfig config;
    private final PlaceholderManager placeholderManager;
    
    public TabListManager(TabListConfig config, PlaceholderManager placeholderManager) {
        this.config = config;
        this.placeholderManager = placeholderManager;
        
        NeoEssentials.LOGGER.info("Tab list manager initialized");
    }
}
