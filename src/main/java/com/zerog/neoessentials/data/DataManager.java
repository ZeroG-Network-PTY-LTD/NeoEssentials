package com.zerog.neoessentials.data;

<<<<<<< HEAD
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.ui.TablistManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
=======
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.config.StorageType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.TagParser;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)

/**
 * Main data manager class that initializes and manages all data storage components.
 */
public class DataManager {    private UserManager userManager;
    private EconomyManager economyManager;
    private HomeManager homeManager;
    private WarpManager warpManager;
    private SpawnManager spawnManager;
    private KitManager kitManager;
<<<<<<< HEAD
    private JailManager jailManager;
    private PowerToolManager powerToolManager;
    private MailManager mailManager;
    private com.zerog.neoessentials.ui.TablistManager tablistManager;
    
    private final String dataFolder = "neoessentials/";
    
    public DataManager(NeoEssentials neoEssentials) {
=======
    private DatabaseManager databaseManager;
    
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final String dataFolder = "config/neoessentials/";
    
    // Default database configuration - will be replaced with config values
    private StorageType storageType = StorageType.JSON;
    private String dbHost = "localhost";
    private int dbPort = 3306;
    private String dbName = "neoessentials";
    private String dbUser = "root";
    private String dbPassword = "";
    
    public DataManager() {
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
        // Create the data folder if it doesn't exist
        File dataFolderFile = new File(dataFolder);
        if (!dataFolderFile.exists()) {
            dataFolderFile.mkdirs();
        }
        
<<<<<<< HEAD
=======
        // Create the database manager with the configured settings
        databaseManager = new DatabaseManager(storageType, dbHost, dbPort, dbName, dbUser, dbPassword);
        
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
        // Initialize all managers
        userManager = new UserManager();
        economyManager = new EconomyManager();
        homeManager = new HomeManager();
        warpManager = new WarpManager();
        spawnManager = new SpawnManager();
<<<<<<< HEAD
        kitManager = new KitManager();        jailManager = new JailManager(dataFolderFile);
        powerToolManager = new PowerToolManager(dataFolderFile);
        mailManager = new MailManager(dataFolderFile);
          // Get the scheduler from NeoEssentials for scheduled tasks like tablist updates
        java.util.concurrent.ScheduledExecutorService scheduler = neoEssentials.getScheduler();
        tablistManager = new com.zerog.neoessentials.ui.TablistManager(scheduler);
    }
    
    /**
     * Initialize the data manager and all its components
     */
    public void initialize() {
        NeoEssentials.LOGGER.info("Initializing NeoEssentials Data Manager");            // Initialize all data managers
=======
        kitManager = new KitManager();
    }    /**
     * Initialize the data manager and all its components
     */
    public void initialize() {
        NeoEssentials.LOGGER.info("Initializing NeoEssentials Data Manager");
        
        // Initialize database manager first if we're not using JSON
        if (storageType != StorageType.JSON) {
            databaseManager.initialize();
        }
          
        // Initialize all data managers
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
        userManager.initialize();
        economyManager.initialize();
        homeManager.initialize();
        warpManager.initialize();
        spawnManager.initialize();
        kitManager.initialize();
<<<<<<< HEAD
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
        }
          if (tablistManager == null && NeoEssentials.getInstance().getConfigManager().isTablistEnabled()) {
            // Create scheduler in NeoEssentials class
            tablistManager = new TablistManager(NeoEssentials.getInstance().getScheduler());
            tablistManager.initialize();
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
=======
        
        NeoEssentials.LOGGER.info("NeoEssentials Data Manager initialized");
    }    /**
     * Save all data to disk
     */
    public void saveAll() {
        NeoEssentials.LOGGER.info("Saving all NeoEssentials data");
        
        userManager.saveAll();
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
        economyManager.saveAll();
        homeManager.saveAll();
        warpManager.saveAll();
        spawnManager.saveSpawnData();
        kitManager.saveKits();
<<<<<<< HEAD
        jailManager.saveJails();
        jailManager.saveJailedPlayers();
        powerToolManager.savePowerTools();
        mailManager.saveMail();
=======
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
    }
    
    /**
     * Close all database connections and save pending data
     */
    public void shutdown() {
        NeoEssentials.LOGGER.info("Shutting down NeoEssentials Data Manager");
        
        // Save all data first
        saveAll();
<<<<<<< HEAD
=======
        
        // Close database connection if using database
        if (storageType != StorageType.JSON && databaseManager != null) {
            databaseManager.close();
        }
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
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
    
<<<<<<< HEAD
    /**
     * Gets the warp manager instance
=======
    /**     * Gets the warp manager instance
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
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
<<<<<<< HEAD
     */
    public SpawnManager getSpawnManager() {
        return spawnManager;
    }
    
    /**
=======
     */    public SpawnManager getSpawnManager() {
        return spawnManager;
    }
      /**
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
     * Gets the kit manager instance
     * 
     * @return The kit manager
     */
    public KitManager getKitManager() {
        return kitManager;
    }
    
    /**
<<<<<<< HEAD
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
     */
    public com.zerog.neoessentials.ui.TablistManager getTablistManager() {
        return tablistManager;
=======
     * Gets the database manager instance
     * 
     * @return The database manager
     */
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    /**
     * Gets the current storage type
     * 
     * @return The storage type
     */
    public StorageType getStorageType() {
        return storageType;
    }
    
    /**
     * Set the storage type and database configuration
     * 
     * @param storageType The storage type (JSON, SQLITE, MYSQL)
     * @param dbHost Database host (for MySQL)
     * @param dbPort Database port (for MySQL)
     * @param dbName Database name
     * @param dbUser Database username (for MySQL)
     * @param dbPassword Database password (for MySQL)
     */
    public void setDatabaseConfig(StorageType storageType, String dbHost, int dbPort, String dbName, String dbUser, String dbPassword) {
        this.storageType = storageType;
        this.dbHost = dbHost;
        this.dbPort = dbPort;
        this.dbName = dbName;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
        
        // Recreate database manager with new config
        if (databaseManager != null) {
            databaseManager.close();
        }
        
        databaseManager = new DatabaseManager(storageType, dbHost, dbPort, dbName, dbUser, dbPassword);
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
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
<<<<<<< HEAD
=======
     * Loads a JSON object from a file
     * 
     * @param fileName The name of the file to load
     * @return The loaded JSON object, or null if not found or error
     */
    public JsonObject loadJsonObject(String fileName) {
        File file = new File(dataFolder + fileName);
        
        if (!file.exists()) {
            return null;
        }
        
        try (FileReader reader = new FileReader(file)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error loading JSON data from file: {}", fileName, e);
            return null;
        }
    }
    
    /**
     * Saves a JSON object to a file
     * 
     * @param fileName The name of the file to save to
     * @param jsonObject The JSON object to save
     */
    public void saveJsonObject(String fileName, JsonObject jsonObject) {
        File file = new File(dataFolder + fileName);
        
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(jsonObject, writer);
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error saving JSON data to file: {}", fileName, e);
        }
    }
    
    /**
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
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
<<<<<<< HEAD
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
=======
        }
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
    }
}
