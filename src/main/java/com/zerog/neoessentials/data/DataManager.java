package com.zerog.neoessentials.data;

<<<<<<< HEAD
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
=======
>>>>>>> 73a32aa (Implement SQLite storage handler and associated factory and manager classes)
import com.zerog.neoessentials.NeoEssentials;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;

import java.io.File;
<<<<<<< HEAD
<<<<<<< HEAD
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
>>>>>>> 73a32aa (Implement SQLite storage handler and associated factory and manager classes)
=======
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
>>>>>>> aa6024a (feat: Implement Admin Panel and Menu System)

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
<<<<<<< HEAD
<<<<<<< HEAD
    private JailManager jailManager;
    private PowerToolManager powerToolManager;
    private MailManager mailManager;
<<<<<<< HEAD
<<<<<<< HEAD
    private com.zerog.neoessentials.ui.TablistManager tablistManager;
    
    private final String dataFolder = "neoessentials/";
    
    public DataManager(NeoEssentials neoEssentials) {
<<<<<<< HEAD
=======
    private DatabaseManager databaseManager;
=======
>>>>>>> 73a32aa (Implement SQLite storage handler and associated factory and manager classes)
=======
    private JailManager jailManager;
    private PowerToolManager powerToolManager;
>>>>>>> 2b0efb3 (Implement powertool and jail management systems)
=======
>>>>>>> 907bd69 (feat: Add MailManager and MailCommands for player mail functionality)
=======
    private com.zerog.neoessentials.ui.TablistManager tablistManager;
>>>>>>> b9b302b (feat: Enhance tablist functionality with player-specific headers and footers; update DataManager and EventHandler for tablist integration)
    
    private final String dataFolder = "neoessentials/";
    
    public DataManager() {
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
>>>>>>> 7409b6f (feat: Add comprehensive configuration management for NeoEssentials, including database, economy, home, kit, warp, and tablist settings)
        // Create the data folder if it doesn't exist
        File dataFolderFile = new File(dataFolder);
        if (!dataFolderFile.exists()) {
            dataFolderFile.mkdirs();
        }
        
<<<<<<< HEAD
<<<<<<< HEAD
=======
        // Create the database manager with the configured settings
        databaseManager = new DatabaseManager(storageType, dbHost, dbPort, dbName, dbUser, dbPassword);
        
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
>>>>>>> 73a32aa (Implement SQLite storage handler and associated factory and manager classes)
        // Initialize all managers
        userManager = new UserManager();
        economyManager = new EconomyManager();
        homeManager = new HomeManager();
        warpManager = new WarpManager();
        spawnManager = new SpawnManager();
<<<<<<< HEAD
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
        jailManager = new JailManager(dataFolderFile);
=======
        kitManager = new KitManager();        jailManager = new JailManager(dataFolderFile);
>>>>>>> b9b302b (feat: Enhance tablist functionality with player-specific headers and footers; update DataManager and EventHandler for tablist integration)
        powerToolManager = new PowerToolManager(dataFolderFile);
        mailManager = new MailManager(dataFolderFile);
        
        // Create executor service for scheduled tasks like tablist updates
        java.util.concurrent.ScheduledExecutorService scheduler = 
            java.util.concurrent.Executors.newScheduledThreadPool(1);
        tablistManager = new com.zerog.neoessentials.ui.TablistManager(scheduler);
    }
    
    /**
     * Initialize the data manager and all its components
     */
    public void initialize() {
<<<<<<< HEAD
        NeoEssentials.LOGGER.info("Initializing NeoEssentials Data Manager");
<<<<<<< HEAD
          
        // Initialize all data managers
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
            // Initialize all data managers
>>>>>>> 2b0efb3 (Implement powertool and jail management systems)
=======
        NeoEssentials.LOGGER.info("Initializing NeoEssentials Data Manager");            // Initialize all data managers
>>>>>>> b9b302b (feat: Enhance tablist functionality with player-specific headers and footers; update DataManager and EventHandler for tablist integration)
        userManager.initialize();
        economyManager.initialize();
        homeManager.initialize();
        warpManager.initialize();
        spawnManager.initialize();
        kitManager.initialize();
<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> b9b302b (feat: Enhance tablist functionality with player-specific headers and footers; update DataManager and EventHandler for tablist integration)
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
<<<<<<< HEAD
<<<<<<< HEAD
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
=======
        // Initialize any managers that need special initialization
        NeoEssentials.LOGGER.info("Initializing data managers");
        
        // Load data for all managers that need it
        loadFromStorage();
>>>>>>> 2ac7252 (feat: Enhance DataManager with initialization and data loading methods for improved manager setup)
=======
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
>>>>>>> ecf8e9a (feat: Refactor DataManager initialization and loading process for improved data handling)
    }
    
    /**
     * Load all data from storage
     */
    public void loadFromStorage() {
        NeoEssentials.LOGGER.info("Loading data from storage");
        
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> ecf8e9a (feat: Refactor DataManager initialization and loading process for improved data handling)
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
=======
        // JailManager doesn't need initialization
>>>>>>> 2b0efb3 (Implement powertool and jail management systems)
        
        NeoEssentials.LOGGER.info("NeoEssentials Data Manager initialized");
=======
        // Load data for each manager
        economyManager.loadData();
        homeManager.loadData();
        warpManager.loadData();
        spawnManager.loadData();
        kitManager.loadData();
        jailManager.loadData();
        mailManager.loadData();
>>>>>>> 2ac7252 (feat: Enhance DataManager with initialization and data loading methods for improved manager setup)
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
<<<<<<< HEAD
        NeoEssentials.LOGGER.info("Saving all NeoEssentials data");
        
        userManager.saveAll();
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
        NeoEssentials.LOGGER.info("Saving all NeoEssentials data");        userManager.saveAll();
>>>>>>> 2b0efb3 (Implement powertool and jail management systems)
        economyManager.saveAll();
        homeManager.saveAll();
        warpManager.saveAll();
        spawnManager.saveSpawnData();
        kitManager.saveKits();
<<<<<<< HEAD
<<<<<<< HEAD
        jailManager.saveJails();
        jailManager.saveJailedPlayers();
        powerToolManager.savePowerTools();
        mailManager.saveMail();
<<<<<<< HEAD
=======
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
        jailManager.saveJails();
        jailManager.saveJailedPlayers();
        powerToolManager.savePowerTools();
>>>>>>> 2b0efb3 (Implement powertool and jail management systems)
=======
>>>>>>> 907bd69 (feat: Add MailManager and MailCommands for player mail functionality)
    }
    
    /**
     * Close all database connections and save pending data
     */
    public void shutdown() {
        NeoEssentials.LOGGER.info("Shutting down NeoEssentials Data Manager");
        
        // Save all data first
        saveAll();
<<<<<<< HEAD
<<<<<<< HEAD
=======
        
        // Close database connection if using database
        if (storageType != StorageType.JSON && databaseManager != null) {
            databaseManager.close();
        }
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
>>>>>>> 73a32aa (Implement SQLite storage handler and associated factory and manager classes)
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
<<<<<<< HEAD
    /**
     * Gets the warp manager instance
=======
    /**     * Gets the warp manager instance
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
    /**
     * Gets the warp manager instance
>>>>>>> 73a32aa (Implement SQLite storage handler and associated factory and manager classes)
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
=======
     */
    public SpawnManager getSpawnManager() {
        return spawnManager;
    }
    
    /**
>>>>>>> 73a32aa (Implement SQLite storage handler and associated factory and manager classes)
     * Gets the kit manager instance
     * 
     * @return The kit manager
     */
    public KitManager getKitManager() {
        return kitManager;
    }
    
    /**
<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> 2b0efb3 (Implement powertool and jail management systems)
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
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> 907bd69 (feat: Add MailManager and MailCommands for player mail functionality)
     * Gets the mail manager instance
     * 
     * @return The mail manager
     */
    public MailManager getMailManager() {
        return mailManager;
    }
    
    /**
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> b9b302b (feat: Enhance tablist functionality with player-specific headers and footers; update DataManager and EventHandler for tablist integration)
     * Gets the tablist manager.
     * 
     * @return The tablist manager
     */
    public com.zerog.neoessentials.ui.TablistManager getTablistManager() {
        return tablistManager;
<<<<<<< HEAD
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
=======
>>>>>>> 73a32aa (Implement SQLite storage handler and associated factory and manager classes)
=======
>>>>>>> 2b0efb3 (Implement powertool and jail management systems)
=======
>>>>>>> 907bd69 (feat: Add MailManager and MailCommands for player mail functionality)
=======
    }
    
    /**
>>>>>>> b9b302b (feat: Enhance tablist functionality with player-specific headers and footers; update DataManager and EventHandler for tablist integration)
     * Get the data directory
     * 
     * @return The data directory path
     */
    public String getDataDirectory() {
        return dataFolder;
    }
    
    /**
<<<<<<< HEAD
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
=======
>>>>>>> 73a32aa (Implement SQLite storage handler and associated factory and manager classes)
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
<<<<<<< HEAD
=======
>>>>>>> aa6024a (feat: Implement Admin Panel and Menu System)
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
<<<<<<< HEAD
=======
        }
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
>>>>>>> aa6024a (feat: Implement Admin Panel and Menu System)
    }
}
