package com.zerog.neoessentials.features.homes;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.config.HomeConfig;
import com.zerog.neoessentials.storage.StorageManager;

/**
 * Home manager for NeoEssentials
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class HomeManager {
    
    private final HomeConfig config;
    private final StorageManager storageManager;
    
    public HomeManager(HomeConfig config, StorageManager storageManager) {
        this.config = config;
        this.storageManager = storageManager;
        
        NeoEssentials.LOGGER.info("Home manager initialized");
    }
}
