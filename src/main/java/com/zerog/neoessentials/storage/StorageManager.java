package com.zerog.neoessentials.storage;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.config.DatabaseConfig;

/**
 * Storage manager for NeoEssentials
 * 
 * Handles database connections and storage operations
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class StorageManager {
    
    private final DatabaseConfig config;
    
    public StorageManager(DatabaseConfig config) {
        this.config = config;
        
        NeoEssentials.LOGGER.info("Initialized storage manager with {} storage", 
            config.storageType.getName());
    }
    
    public void shutdown() {
        NeoEssentials.LOGGER.info("Storage manager shutdown");
    }
}
