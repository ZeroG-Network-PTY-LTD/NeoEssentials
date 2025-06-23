package com.zerog.neoessentials.data;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.ui.tablist.FlexibleTablistManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Main data manager class that initializes and manages all data storage components.
 */
public class DataManager {    private UserManager userManager;
    private EconomyManager economyManager;
    private HomeManager homeManager;
    private WarpManager warpManager;
    private SpawnManager spawnManager;
    private KitManager kitManager;    private JailManager jailManager;
    private PowerToolManager powerToolManager;
    private MailManager mailManager;
    private FlexibleTablistManager tablistManager;
    
    private final String dataFolder = "neoessentials/";
    
    public DataManager(NeoEssentials neoEssentials) {
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
        kitManager = new KitManager();        jailManager = new JailManager(dataFolderFile);
        powerToolManager = new PowerToolManager(dataFolderFile);
        mailManager = new MailManager(dataFolderFile);        // Get the scheduler from NeoEssentials for scheduled tasks like tablist updates
        java.util.concurrent.ScheduledExecutorService scheduler = neoEssentials.getScheduler();
        tablistManager = new FlexibleTablistManager(scheduler);
    }
    
    /**
     * Initialize the data manager and all its components
     */
    public void initialize() {
        NeoEssentials.LOGGER.info("Initializing NeoEssentials Data Manager");            // Initialize all data managers
        userManager.initialize();
        economyManager.initialize();
        homeManager.initialize();
        warpManager.initialize();
        spawnManager.initialize();
        kitManager.initialize();
        // Initialize tablist after economy is initialized
        tablistManager.initialize();
        // JailManager doesn't need initialization
        
        NeoEssentials.LOGGER.info("NeoEssentials Data Manager initialized");
    }
    
    /**
     * Initialize managers that need special initialization after the data manager is created
     * This is called by NeoEssentials after the data manager is created
     */
    public void initializeManagers() {
        NeoEssentials.LOGGER.info("Initializing managers");
        
        // Create manager instances if they don't exist
        if (userManager == null) {
            userManager = new UserManager();
        }
        
        if (economyManager == null) {
            economyManager = new EconomyManager();
        }
        
        if (homeManager == null) {
            homeManager = new HomeManager();
        }
        
        if (warpManager == null) {
            warpManager = new WarpManager();
        }
        
        if (spawnManager == null) {
            spawnManager = new SpawnManager();
        }
        
        if (kitManager == null) {
            kitManager = new KitManager();
        }
          // Get data folder for managers that need it
        File dataFolderFile = new File(dataFolder);
        
        if (jailManager == null) {
            jailManager = new JailManager(dataFolderFile);
        }
        
        if (mailManager == null) {
            mailManager = new MailManager(dataFolderFile);
        }        if (tablistManager == null && NeoEssentials.getInstance().getConfigManager().isTablistEnabled()) {
            // Create scheduler in NeoEssentials class
            tablistManager = new FlexibleTablistManager(NeoEssentials.getInstance().getScheduler());
            // The server will be set later when available
            if (NeoEssentials.getInstance().getServer() != null) {
                tablistManager.setServer(NeoEssentials.getInstance().getServer());
            }
        }
    }
    
    /**
     * Load all data from storage
     */
    public void loadFromStorage() {
        NeoEssentials.LOGGER.info("Loading data from storage");
        
        // Most managers automatically load their data in their constructor
        // or have specific load methods that are already called
        
        // Simply reinitialize managers to refresh data
        initializeManagers();
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
        NeoEssentials.LOGGER.info("Saving all NeoEssentials data");        userManager.saveAll();
        economyManager.saveAll();
        homeManager.saveAll();
        warpManager.saveAll();
        spawnManager.saveSpawnData();
        kitManager.saveKits();
        jailManager.saveJails();
        jailManager.saveJailedPlayers();
        powerToolManager.savePowerTools();
        mailManager.saveMail();
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
     * Gets the jail manager instance
     * 
     * @return The jail manager
     */
    public JailManager getJailManager() {
        return jailManager;
    }
    
    /**
     * Gets the powertool manager instance
     * 
     * @return The powertool manager
     */
    public PowerToolManager getPowerToolManager() {
        return powerToolManager;
    }
    
    /**
     * Gets the mail manager instance
     * 
     * @return The mail manager
     */
    public MailManager getMailManager() {
        return mailManager;
    }
      /**
     * Gets the tablist manager.
     * 
     * @return The tablist manager
     */    public FlexibleTablistManager getTablistManager() {
        return tablistManager;
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
        }    }
    
    /**
     * Get all warps in the system
     * 
     * @return A list of all warp data objects
     */    public List<WarpData> getAllWarps() {
        // Load all warps from storage
        Map<String, WarpData> warpMap = NeoEssentials.getInstance().getStorageManager().loadWarps();
        
        // Convert to list
        List<WarpData> warpList = new ArrayList<>();
        if (warpMap != null) {
            warpList.addAll(warpMap.values());
        }
        
        return warpList;
    }
}
