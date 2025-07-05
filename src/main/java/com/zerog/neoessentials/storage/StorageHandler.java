package com.zerog.neoessentials.storage;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.zerog.neoessentials.data.HomeData;
import com.zerog.neoessentials.data.KitManager.Kit;
import com.zerog.neoessentials.data.WarpData;

/**
 * Interface for storage handlers
 */
public interface StorageHandler {
    
    /**
     * Initializes the storage handler
     */
    void initialize();
    
    /**
     * Shuts down the storage handler
     */
    void shutdown();
    
    /**
     * Saves player home data
     * 
     * @param uuid The player UUID
     * @param homes The home data
     * @return True if successful, false otherwise
     */
    boolean saveHomeData(UUID uuid, Map<String, HomeData> homes);
    
    /**
     * Loads player home data
     * 
     * @param uuid The player UUID
     * @return The home data, or null if an error occurs
     */
    Map<String, HomeData> loadHomeData(UUID uuid);
    
    /**
     * Saves all warps
     * 
     * @param warps The warps to save
     * @return True if successful, false otherwise
     */
    boolean saveWarps(Map<String, WarpData> warps);
    
    /**
     * Loads all warps
     * 
     * @return The warps, or null if an error occurs
     */
    Map<String, WarpData> loadWarps();
    
    /**
     * Saves all kits
     * 
     * @param kits The kits to save
     * @param cooldowns The kit cooldowns
     * @return True if successful, false otherwise
     */
    boolean saveKits(Map<String, Kit> kits, Map<UUID, Map<String, Long>> cooldowns);
    
    /**
     * Loads all kits
     * 
     * @return A list containing the kits and cooldowns, or null if an error occurs
     */
    List<Object> loadKits();
    
    /**
     * Saves all spawn data
     * 
     * @param spawn The spawn data
     * @return True if successful, false otherwise
     */
    boolean saveSpawnData(Map<String, Object> spawn);
    
    /**
     * Loads all spawn data
     * 
     * @return The spawn data, or null if an error occurs
     */
    Map<String, Object> loadSpawnData();
}
