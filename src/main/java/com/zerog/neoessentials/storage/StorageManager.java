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
     * 
     * @return True if initialization was successful, false otherwise
     */
    public boolean initialize() {
        NeoEssentials.LOGGER.info("Initializing storage manager with {} storage", config.storageType.get());
        
        // Create the storage handler based on config
        try {
            storageHandler = StorageFactory.createStorageHandler(config);
            
            if (storageHandler == null) {
                NeoEssentials.LOGGER.error("Failed to create storage handler for type {}", config.storageType.get());
                return false;
            }
            
            // Initialize the storage handler
            storageHandler.initialize();
            
            NeoEssentials.LOGGER.info("Storage manager successfully initialized with {} storage", config.storageType.get());
            return true;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error initializing storage manager: {}", e.getMessage(), e);
            return false;
        }
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
            try {
                return storageHandler.saveHomeData(uuid, homes);
            } catch (Exception e) {
                NeoEssentials.LOGGER.error("Error saving home data for player {}: {}", uuid, e.getMessage(), e);
            }
        } else {
            NeoEssentials.LOGGER.error("Cannot save home data: Storage handler is not initialized");
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
            try {
                return storageHandler.loadHomeData(uuid);
            } catch (Exception e) {
                NeoEssentials.LOGGER.error("Error loading home data for player {}: {}", uuid, e.getMessage(), e);
            }
        } else {
            NeoEssentials.LOGGER.error("Cannot load home data: Storage handler is not initialized");
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
            try {
                NeoEssentials.LOGGER.debug("Saving {} warps via storage manager", warps.size());
                return storageHandler.saveWarps(warps);
            } catch (Exception e) {
                NeoEssentials.LOGGER.error("Error saving warps: {}", e.getMessage(), e);
            }
        } else {
            NeoEssentials.LOGGER.error("Cannot save warps: Storage handler is not initialized");
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
            try {
                NeoEssentials.LOGGER.debug("Loading warps via storage manager");
                Map<String, WarpData> warps = storageHandler.loadWarps();
                NeoEssentials.LOGGER.debug("Storage manager loaded {} warps", warps.size());
                return warps;
            } catch (Exception e) {
                NeoEssentials.LOGGER.error("Error loading warps: {}", e.getMessage(), e);
            }
        } else {
            NeoEssentials.LOGGER.error("Cannot load warps: Storage handler is not initialized");
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
