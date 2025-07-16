package com.zerog.neoessentials.data;

<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.ui.EnhancedTablistManager;
=======
import com.zerog.neoessentials.NeoEssentials;
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
<<<<<<< HEAD
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
=======
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d

/**
 * Main data manager class that initializes and manages all data storage components.
 */
public class DataManager {    private UserManager userManager;
<<<<<<< HEAD
    private EconomyManager economyManager; // Legacy economy manager for backward compatibility
    private com.zerog.neoessentials.economy.EconomyManager newEconomyManager; // New v1.0.2 economy system
    private HomeManager homeManager;
    private WarpManager warpManager;
    private SpawnManager spawnManager;
    private KitManager kitManager;
<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
    private JailManager jailManager;
    private PowerToolManager powerToolManager;
<<<<<<< HEAD
    private MailManager mailManager;
<<<<<<< HEAD
<<<<<<< HEAD
    private com.zerog.neoessentials.ui.TablistManager tablistManager;
=======
    private MailManager mailManager;    private EnhancedTablistManager tablistManager;
>>>>>>> 89588f4 (Add configuration management and tablist functionality)
=======
    private HomeManager homeManager;
    private WarpManager warpManager;
    private SpawnManager spawnManager;
    private KitManager kitManager;    
    private JailManager jailManager;
    private PowerToolManager powerToolManager;
    private MailManager mailManager;
    private BookmarkManager bookmarkManager;
    private PlayerSettingsManager playerSettingsManager;
    private TeleportHistoryManager teleportHistoryManager;
    // Use TABLikeTablistManager (enhanced system)
    private com.zerog.neoessentials.ui.tablist.enhanced.TABLikeTablistManager tablistManager;
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
    
    private final String dataFolder = "neoessentials/";
    
    public DataManager(NeoEssentials neoEssentials) {
<<<<<<< HEAD
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
=======
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
        // Create the data folder if it doesn't exist
        File dataFolderFile = new File(dataFolder);
        if (!dataFolderFile.exists()) {
            dataFolderFile.mkdirs();
        }
        
<<<<<<< HEAD
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
        economyManager = new EconomyManager(); // Legacy economy manager for backward compatibility
        newEconomyManager = com.zerog.neoessentials.economy.EconomyManager.getInstance(); // New v1.0.2 economy system
        homeManager = new HomeManager();
        warpManager = new WarpManager();
        spawnManager = new SpawnManager();
<<<<<<< HEAD
<<<<<<< HEAD
        kitManager = new KitManager();        jailManager = new JailManager(dataFolderFile);
        powerToolManager = new PowerToolManager(dataFolderFile);
        mailManager = new MailManager(dataFolderFile);        // Get the scheduler from NeoEssentials for scheduled tasks like tablist updates
        java.util.concurrent.ScheduledExecutorService scheduler = neoEssentials.getScheduler();
<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
        tablistManager = new com.zerog.neoessentials.ui.TablistManager(scheduler);
=======
        tablistManager = new EnhancedTablistManager(neoEssentials.getServer(), scheduler);
>>>>>>> 89588f4 (Add configuration management and tablist functionality)
=======
        tablistManager = new EnhancedTablistManager(scheduler);
>>>>>>> 30dc8b4 (feat: Refactor tablist management to improve server reference handling and placeholder processing)
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
=======
>>>>>>> 6528176 (feat: Enhance scheduler handling and improve configuration management in NeoEssentials)
        tablistManager = new com.zerog.neoessentials.ui.TablistManager(scheduler);
=======
        // Initialize all managers
        userManager = new UserManager();
        homeManager = new HomeManager();
        warpManager = new WarpManager();
        spawnManager = new SpawnManager();
        kitManager = new KitManager();        
        jailManager = new JailManager(dataFolderFile);
        powerToolManager = new PowerToolManager(dataFolderFile);
        mailManager = new MailManager(dataFolderFile);
        bookmarkManager = new BookmarkManager();
        playerSettingsManager = new PlayerSettingsManager();
        teleportHistoryManager = new TeleportHistoryManager();
        
        // Get the scheduler from NeoEssentials for scheduled tasks like tablist updates
        java.util.concurrent.ScheduledExecutorService scheduler = neoEssentials.getScheduler();
        
        // Use the TABLikeTablistManager (enhanced system) instead of TabManager
        tablistManager = new com.zerog.neoessentials.ui.tablist.enhanced.TABLikeTablistManager(scheduler);
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
    }
    
    /**
     * Initialize the data manager and all its components
     */
    public void initialize() {
<<<<<<< HEAD
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
        economyManager.initialize(); // Legacy economy system
        // newEconomyManager is already initialized via singleton getInstance()
=======
        NeoEssentials.LOGGER.info("Initializing NeoEssentials Data Manager");            // Initialize all data managers
        userManager.initialize();
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
        homeManager.initialize();
        warpManager.initialize();
        spawnManager.initialize();
        kitManager.initialize();
<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> b9b302b (feat: Enhance tablist functionality with player-specific headers and footers; update DataManager and EventHandler for tablist integration)
        // Initialize tablist after economy is initialized
=======
        // Initialize tablist manager
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
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
<<<<<<< HEAD
=======
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
        NeoEssentials.LOGGER.info("Initializing managers");
        
        // Create manager instances if they don't exist
        if (userManager == null) {
            userManager = new UserManager();
        }
        
<<<<<<< HEAD
        if (economyManager == null) {
            economyManager = new EconomyManager();
        }
        
        if (newEconomyManager == null) {
            newEconomyManager = com.zerog.neoessentials.economy.EconomyManager.getInstance();
        }
        
=======
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
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
<<<<<<< HEAD
            // Create scheduler in NeoEssentials class
            tablistManager = new EnhancedTablistManager(NeoEssentials.getInstance().getScheduler());
=======
            // Create TABLikeTablistManager (enhanced system)
            tablistManager = new com.zerog.neoessentials.ui.tablist.enhanced.TABLikeTablistManager(NeoEssentials.getInstance().getScheduler());
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
            // The server will be set later when available
            if (NeoEssentials.getInstance().getServer() != null) {
                tablistManager.setServer(NeoEssentials.getInstance().getServer());
            }
        }
<<<<<<< HEAD
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
=======
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
    }
    
    /**
     * Load all data from storage
     */
    public void loadFromStorage() {
        NeoEssentials.LOGGER.info("Loading data from storage");
        
<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> ecf8e9a (feat: Refactor DataManager initialization and loading process for improved data handling)
=======
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
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
<<<<<<< HEAD
            // economyManager.reloadEconomyData();
=======
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
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
<<<<<<< HEAD
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
=======
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
        homeManager.saveAll();
        warpManager.saveAll();
        spawnManager.saveSpawnData();
        kitManager.saveKits();
<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
        jailManager.saveJails();
        jailManager.saveJailedPlayers();
        powerToolManager.savePowerTools();
        mailManager.saveMail();
<<<<<<< HEAD
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
=======
        
        // Save new persistent data managers
        if (teleportHistoryManager != null) {
            teleportHistoryManager.shutdown(); // This calls saveHistory internally
        }
        if (playerSettingsManager != null) {
            playerSettingsManager.shutdown(); // This calls saveSettings internally
        }
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
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
<<<<<<< HEAD
=======
        
        // Close database connection if using database
        if (storageType != StorageType.JSON && databaseManager != null) {
            databaseManager.close();
        }
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
>>>>>>> 73a32aa (Implement SQLite storage handler and associated factory and manager classes)
=======
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
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
<<<<<<< HEAD
     * Gets the economy manager instance
     * 
     * @return The economy manager
     */
    public EconomyManager getEconomyManager() {
        return economyManager;
    }
    
    /**
     * Gets the new v1.0.2 economy manager instance
     * 
     * @return The new economy manager
     */
    public com.zerog.neoessentials.economy.EconomyManager getNewEconomyManager() {
        return newEconomyManager;
    }
    
    /**
=======
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
     * Gets the home manager instance
     * 
     * @return The home manager
     */
    public HomeManager getHomeManager() {
        return homeManager;
    }
    
<<<<<<< HEAD
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
=======
    /**
     * Gets the warp manager instance
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
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
<<<<<<< HEAD
=======
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
     */
    public SpawnManager getSpawnManager() {
        return spawnManager;
    }
    
    /**
<<<<<<< HEAD
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
=======
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
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
<<<<<<< HEAD
=======
>>>>>>> 2b0efb3 (Implement powertool and jail management systems)
=======
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
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
<<<<<<< HEAD
=======
>>>>>>> 907bd69 (feat: Add MailManager and MailCommands for player mail functionality)
=======
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
     * Gets the mail manager instance
     * 
     * @return The mail manager
     */
    public MailManager getMailManager() {
        return mailManager;
    }
<<<<<<< HEAD
    
    /**
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> b9b302b (feat: Enhance tablist functionality with player-specific headers and footers; update DataManager and EventHandler for tablist integration)
     * Gets the tablist manager.
     * 
     * @return The tablist manager
     */    public EnhancedTablistManager getTablistManager() {
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
=======

    /**
     * Get the bookmark manager
     * 
     * @return The bookmark manager
     */
    public BookmarkManager getBookmarkManager() {
        return bookmarkManager;
    }
    
    /**
     * Get the player settings manager
     * 
     * @return The player settings manager
     */
    public PlayerSettingsManager getPlayerSettingsManager() {
        return playerSettingsManager;
    }
    
    /**
     * Get the teleport history manager
     * 
     * @return The teleport history manager
     */
    public TeleportHistoryManager getTeleportHistoryManager() {
        return teleportHistoryManager;
    }

    /**
     * Gets the enhanced tablist manager.
     * 
     * @return The TABLikeTablistManager
     */
    public com.zerog.neoessentials.ui.tablist.enhanced.TABLikeTablistManager getTablistManager() {
        return tablistManager;
    }
    
    /**
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
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
=======
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
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
<<<<<<< HEAD
=======
>>>>>>> aa6024a (feat: Implement Admin Panel and Menu System)
=======
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
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
<<<<<<< HEAD
=======
        }
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
>>>>>>> aa6024a (feat: Implement Admin Panel and Menu System)
=======
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
    }
}
