package com.zerog.neoessentials.economy.storage;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.config.EconomyConfig;

import java.nio.file.Path;

/**
 * Factory for creating economy storage instances based on configuration
 */
public class EconomyStorageFactory {
    
    /**
     * Creates a storage instance based on the configuration
     * @param config the economy configuration
     * @param dataDirectory the data directory for file-based storage
     * @return the storage instance
     */
    public static EconomyStorage createStorage(EconomyConfig config, Path dataDirectory) {
        EconomyConfig.StorageType type = config.getStorageType();
        
        switch (type) {
            case JSON:
                return new JsonEconomyStorage(dataDirectory.resolve("economy"));
                
            case SQLITE:
                return new SqliteEconomyStorage(dataDirectory.resolve("economy"));
                
            case MYSQL:
                return new MySqlEconomyStorage(config.getDatabaseConfig());
                
            default:
                NeoEssentials.LOGGER.warn("Unknown storage type: {}, defaulting to JSON", type);
                return new JsonEconomyStorage(dataDirectory.resolve("economy"));
        }
    }
    
    /**
     * Creates a storage instance based on string type
     * @param typeString the storage type as string
     * @param dataDirectory the data directory for file-based storage
     * @return the storage instance
     */
    public static EconomyStorage createStorage(String typeString, Path dataDirectory) {
        try {
            EconomyConfig.StorageType type = EconomyConfig.StorageType.valueOf(typeString.toUpperCase());
            
            switch (type) {
                case JSON:
                    return new JsonEconomyStorage(dataDirectory.resolve("economy"));
                    
                case SQLITE:
                    return new SqliteEconomyStorage(dataDirectory.resolve("economy"));
                    
                default:
                    NeoEssentials.LOGGER.warn("Storage type {} not supported without database config, defaulting to JSON", type);
                    return new JsonEconomyStorage(dataDirectory.resolve("economy"));
            }
        } catch (IllegalArgumentException e) {
            NeoEssentials.LOGGER.warn("Invalid storage type: {}, defaulting to JSON", typeString);
            return new JsonEconomyStorage(dataDirectory.resolve("economy"));
        }
    }
}
