package com.zerog.neoessentials.features.warps;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.config.WarpConfig;
import com.zerog.neoessentials.storage.StorageManager;

/**
 * Warp manager for NeoEssentials
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class WarpManager {
    
    private final WarpConfig config;
    private final StorageManager storageManager;
    
    public WarpManager(WarpConfig config, StorageManager storageManager) {
        this.config = config;
        this.storageManager = storageManager;
        
        NeoEssentials.LOGGER.info("Warp manager initialized");
    }
}
