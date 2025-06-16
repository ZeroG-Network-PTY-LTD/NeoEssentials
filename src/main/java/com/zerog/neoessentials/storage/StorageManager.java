package com.zerog.neoessentials.storage;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.config.DatabaseConfig;
import com.zerog.neoessentials.config.StorageType;
import com.zerog.neoessentials.data.EconomyData;
import com.zerog.neoessentials.data.HomeData;
import com.zerog.neoessentials.data.KitManager;
import com.zerog.neoessentials.data.WarpData;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manager for storage handlers
 */
public class StorageManager {
    private StorageHandler storageHandler;
    private final DatabaseConfig config;
    
    public StorageManager(DatabaseConfig config) {
        this.config = config;
    }
    
    /**
     * Initialize the storage manager
     */
    public void initialize() {
        // Create the storage handler based on config
        storageHandler = StorageFactory.createStorageHandler(config);
        
        // Initialize the storage handler
        storageHandler.initialize();
        
        NeoEssentials.LOGGER.info("Storage manager initialized with {} storage", config.storageType.get());
    }
    
    /**
     * Shutdown the storage manager
     */
    public void shutdown() {
        if (storageHandler != null) {
            storageHandler.shutdown();
            storageHandler = null;
        }
    }
    
    /**
     * Get the current storage type
     * 
     * @return The storage type
     */
    public StorageType getStorageType() {
        return config.storageType.get();
    }
    
    /**
     * Get the current storage handler
     * 
     * @return The storage handler
     */
    public StorageHandler getStorageHandler() {
        return storageHandler;
    }
    
    /**
     * Save home data for a player
     * 
     * @param uuid The player UUID
     * @param homes The home data
     * @return True if successful, false otherwise
     */
    public boolean saveHomeData(UUID uuid, Map<String, HomeData> homes) {
        if (storageHandler != null) {
            return storageHandler.saveHomeData(uuid, homes);
        }
        return false;
    }
    
    /**
     * Load home data for a player
     * 
     * @param uuid The player UUID
     * @return The home data, or an empty map if an error occurs
     */
    public Map<String, HomeData> loadHomeData(UUID uuid) {
        if (storageHandler != null) {
            return storageHandler.loadHomeData(uuid);
        }
        return Map.of();
    }
    
    /**
     * Save all warps
     * 
     * @param warps The warps to save
     * @return True if successful, false otherwise
     */
    public boolean saveWarps(Map<String, WarpData> warps) {
        if (storageHandler != null) {
            return storageHandler.saveWarps(warps);
        }
        return false;
    }
    
    /**
     * Load all warps
     * 
     * @return The warps, or an empty map if an error occurs
     */
    public Map<String, WarpData> loadWarps() {
        if (storageHandler != null) {
            return storageHandler.loadWarps();
        }
        return Map.of();
    }
    
    /**
     * Save economy data for a player
     * 
     * @param uuid The player UUID
     * @param economyData The economy data
     * @return True if successful, false otherwise
     */
    public boolean saveEconomyData(UUID uuid, EconomyData economyData) {
        if (storageHandler != null) {
            return storageHandler.saveEconomyData(uuid, economyData);
        }
        return false;
    }
    
    /**
     * Load economy data for a player
     * 
     * @param uuid The player UUID
     * @return The economy data, or a new EconomyData instance if an error occurs
     */
    public EconomyData loadEconomyData(UUID uuid) {
        if (storageHandler != null) {
            return storageHandler.loadEconomyData(uuid);
        }
        return new EconomyData();
    }
    
    /**
     * Save all kits
     * 
     * @param kits The kits to save
     * @param cooldowns The kit cooldowns
     * @return True if successful, false otherwise
     */
    public boolean saveKits(Map<String, KitManager.Kit> kits, Map<UUID, Map<String, Long>> cooldowns) {
        if (storageHandler != null) {
            return storageHandler.saveKits(kits, cooldowns);
        }
        return false;
    }
    
    /**
     * Load all kits
     * 
     * @return A list containing the kits and cooldowns, or null if an error occurs
     */
    public List<Object> loadKits() {
        if (storageHandler != null) {
            return storageHandler.loadKits();
        }
        return null;
    }
    
    /**
     * Save all spawn data
     * 
     * @param spawn The spawn data
     * @return True if successful, false otherwise
     */
    public boolean saveSpawnData(Map<String, Object> spawn) {
        if (storageHandler != null) {
            return storageHandler.saveSpawnData(spawn);
        }
        return false;
    }
    
    /**
     * Load all spawn data
     * 
     * @return The spawn data, or an empty map if an error occurs
     */
    public Map<String, Object> loadSpawnData() {
        if (storageHandler != null) {
            return storageHandler.loadSpawnData();
        }
        return Map.of();
    }
}
