package com.zerog.neoessentials.storage;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.config.DatabaseConfig;
import com.zerog.neoessentials.config.StorageType;

/**
 * Factory for creating storage handlers based on configuration
 */
public class StorageFactory {
    
    /**
     * Creates a storage handler based on the provided configuration
     * 
     * @param config The database configuration
     * @return A storage handler instance
     */    public static StorageHandler createStorageHandler(DatabaseConfig config) {
        // Safely handle config access 
        StorageType storageType;
        try {
            storageType = config.storageType.get();
            NeoEssentials.LOGGER.info("Creating {} storage handler", storageType);
        } catch (IllegalStateException e) {
            // Config not loaded yet, default to JSON
            NeoEssentials.LOGGER.warn("Config not fully loaded yet, falling back to JSON storage");
            return new JsonStorageHandler();
        }
        
        switch (storageType) {
            case JSON:
                return new JsonStorageHandler();
            case SQLITE:
                return new SQLiteStorageHandler();
            case MYSQL:
                return new MySQLStorageHandler(config);
            default:
                NeoEssentials.LOGGER.warn("Unknown storage type {}, falling back to JSON", storageType);
                return new JsonStorageHandler();
        }
    }
}
