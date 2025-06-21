package com.zerog.neoessentials.data;

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

/**
 * Main data manager class that initializes and manages all data storage components.
 */
public class DataManager {    private UserManager userManager;
    private EconomyManager economyManager;
    private HomeManager homeManager;
    private WarpManager warpManager;
    private SpawnManager spawnManager;
    private KitManager kitManager;
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
        // Create the data folder if it doesn't exist
        File dataFolderFile = new File(dataFolder);
        if (!dataFolderFile.exists()) {
            dataFolderFile.mkdirs();
        }
        
        // Create the database manager with the configured settings
        databaseManager = new DatabaseManager(storageType, dbHost, dbPort, dbName, dbUser, dbPassword);
        
        // Initialize all managers
        userManager = new UserManager();
        economyManager = new EconomyManager();
        homeManager = new HomeManager();
        warpManager = new WarpManager();
        spawnManager = new SpawnManager();
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
        userManager.initialize();
        economyManager.initialize();
        homeManager.initialize();
        warpManager.initialize();
        spawnManager.initialize();
        kitManager.initialize();
        
        NeoEssentials.LOGGER.info("NeoEssentials Data Manager initialized");
    }    /**
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
        
        // Close database connection if using database
        if (storageType != StorageType.JSON && databaseManager != null) {
            databaseManager.close();
        }
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
    
    /**     * Gets the warp manager instance
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
     */    public SpawnManager getSpawnManager() {
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
