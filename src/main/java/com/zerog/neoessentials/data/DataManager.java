package com.zerog.neoessentials.data;

import com.zerog.neoessentials.NeoEssentials;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;

import java.io.File;

/**
 * Main data manager class that initializes and manages all data storage components.
 */
public class DataManager {
    private UserManager userManager;
    private EconomyManager economyManager;
    private HomeManager homeManager;
    private WarpManager warpManager;
    private SpawnManager spawnManager;
    private KitManager kitManager;
    
    private final String dataFolder = "neoessentials/";
    
    public DataManager() {
        // Create the data folder if it doesn't exist
        File dataFolderFile = new File(dataFolder);
        if (!dataFolderFile.exists()) {
            dataFolderFile.mkdirs();
        }
        
        // Initialize all managers
        userManager = new UserManager();
        economyManager = new EconomyManager();
        homeManager = new HomeManager();
        warpManager = new WarpManager();
        spawnManager = new SpawnManager();
        kitManager = new KitManager();
    }
    
    /**
     * Initialize the data manager and all its components
     */
    public void initialize() {
        NeoEssentials.LOGGER.info("Initializing NeoEssentials Data Manager");
          
        // Initialize all data managers
        userManager.initialize();
        economyManager.initialize();
        homeManager.initialize();
        warpManager.initialize();
        spawnManager.initialize();
        kitManager.initialize();
        
        NeoEssentials.LOGGER.info("NeoEssentials Data Manager initialized");
    }
    
    /**
     * Reload data from storage.
     * Call this method after the storage manager has been initialized if it wasn't available during initial loading.
     */
    public void reloadFromStorage() {
        NeoEssentials.LOGGER.info("Reloading data from storage");
        
        // Check if storage manager is initialized
        if (NeoEssentials.getInstance().getStorageManager() != null) {
            // Reload warps data
            warpManager.reloadWarps();
            
            // Reload other data as needed
            // homeManager.reloadHomes();
            // economyManager.reloadEconomyData();
            // kitManager.reloadKits();
            // spawnManager.reloadSpawnData();
            
            NeoEssentials.LOGGER.info("Data successfully reloaded from storage");
        } else {
            NeoEssentials.LOGGER.warn("Cannot reload data: Storage manager is not initialized");
        }
    }
    
    /**
     * Save all data to disk
     */
    public void saveAll() {
        NeoEssentials.LOGGER.info("Saving all NeoEssentials data");
        
        userManager.saveAll();
        economyManager.saveAll();
        homeManager.saveAll();
        warpManager.saveAll();
        spawnManager.saveSpawnData();
        kitManager.saveKits();
    }
    
    /**
     * Close all database connections and save pending data
     */
    public void shutdown() {
        NeoEssentials.LOGGER.info("Shutting down NeoEssentials Data Manager");
        
        // Save all data first
        saveAll();
    }
    
    /**
     * Gets the user manager instance
     * 
     * @return The user manager
     */
    public UserManager getUserManager() {
        return userManager;
    }
    
    /**
     * Gets the economy manager instance
     * 
     * @return The economy manager
     */
    public EconomyManager getEconomyManager() {
        return economyManager;
    }
    
    /**
     * Gets the home manager instance
     * 
     * @return The home manager
     */
    public HomeManager getHomeManager() {
        return homeManager;
    }
    
    /**
     * Gets the warp manager instance
     * 
     * @return The warp manager
     */
    public WarpManager getWarpManager() {
        return warpManager;
    }
    
    /**
     * Gets the spawn manager instance
     * 
     * @return The spawn manager
     */
    public SpawnManager getSpawnManager() {
        return spawnManager;
    }
    
    /**
     * Gets the kit manager instance
     * 
     * @return The kit manager
     */
    public KitManager getKitManager() {
        return kitManager;
    }
    
    /**
     * Get the data directory
     * 
     * @return The data directory path
     */
    public String getDataDirectory() {
        return dataFolder;
    }
    
    /**
     * Converts a CompoundTag to a string
     * 
     * @param nbt The NBT tag to convert
     * @return The string representation of the NBT tag
     */
    public String nbtToString(CompoundTag nbt) {
        try {
            return nbt.toString();
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error converting NBT to string", e);
            return null;
        }
    }
    
    /**
     * Converts a string to a CompoundTag
     * 
     * @param nbtString The string to convert
     * @return The NBT tag, or null if error
     */
    public CompoundTag stringToNBT(String nbtString) {
        try {
            return TagParser.parseTag(nbtString);
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error converting string to NBT", e);
            return null;
        }
    }
}
