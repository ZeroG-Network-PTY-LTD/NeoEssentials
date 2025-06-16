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
     */
    public static StorageHandler createStorageHandler(DatabaseConfig config) {
        StorageType storageType = config.storageType.get();
        
        NeoEssentials.LOGGER.info("Creating {} storage handler", storageType);
        
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
