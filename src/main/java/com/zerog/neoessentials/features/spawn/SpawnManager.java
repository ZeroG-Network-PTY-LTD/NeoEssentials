package com.zerog.neoessentials.features.spawn;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.config.SpawnConfig;
import com.zerog.neoessentials.storage.StorageManager;

/**
 * Spawn manager for NeoEssentials
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class SpawnManager {
    
    private final SpawnConfig config;
    private final StorageManager storageManager;
    
    public SpawnManager(SpawnConfig config, StorageManager storageManager) {
        this.config = config;
        this.storageManager = storageManager;
        
        NeoEssentials.LOGGER.info("Spawn manager initialized");
    }
}
