package com.zerog.neoessentials.features.kits;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.config.KitConfig;
import com.zerog.neoessentials.storage.StorageManager;

/**
 * Kit manager for NeoEssentials
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class KitManager {
    
    private final KitConfig config;
    private final StorageManager storageManager;
    
    public KitManager(KitConfig config, StorageManager storageManager) {
        this.config = config;
        this.storageManager = storageManager;
        
        NeoEssentials.LOGGER.info("Kit manager initialized");
    }
}
