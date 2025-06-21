package com.zerog.neoessentials.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.zerog.neoessentials.NeoEssentials;
import net.minecraft.server.level.ServerPlayer;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages user data for the NeoEssentials mod.
 */
public class UserManager {
    private static final String USER_DATA_DIR = "neoessentials/users";
    
    // Map of UUID to user data
    private final Map<UUID, JsonObject> userDataMap = new ConcurrentHashMap<>();
    
    private final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();
    
    public UserManager() {
    }
    
    /**
     * Initialize the user manager
     */
    public void initialize() {
        NeoEssentials.LOGGER.info("Initializing NeoEssentials User Manager");
        
        try {
            // Create user data directory if it doesn't exist
            File userDataDir = new File(USER_DATA_DIR);
            if (!userDataDir.exists()) {
                if (userDataDir.mkdirs()) {
                    NeoEssentials.LOGGER.info("Created user data directory: {}", userDataDir);
                } else {
                    NeoEssentials.LOGGER.error("Failed to create user data directory: {}", userDataDir);
                }
            }
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error initializing user manager", e);
        }
    }
    
    /**
     * Save all user data to disk
     */
    public void saveAll() {
        NeoEssentials.LOGGER.info("Saving all user data");
        
        for (Map.Entry<UUID, JsonObject> entry : userDataMap.entrySet()) {
            saveUserData(entry.getKey(), entry.getValue());
        }
    }
    
    /**
     * Load a player's data from disk or create new data if none exists
     * 
     * @param player The player to load data for
     */
    public void loadPlayerData(ServerPlayer player) {
        UUID playerId = player.getUUID();
        
        try {
            File userDataFile = getUserDataFile(playerId);
            JsonObject userData;
            
            if (userDataFile.exists()) {
                // Load existing user data
                try (FileReader reader = new FileReader(userDataFile)) {
                    userData = gson.fromJson(reader, JsonObject.class);
                    if (userData == null) {
                        userData = createDefaultUserData(player);
                    }
                }
            } else {
                // Create default user data
                userData = createDefaultUserData(player);
                saveUserData(playerId, userData);
            }
            
            // Store in memory
            userDataMap.put(playerId, userData);
            
        } catch (IOException e) {
            NeoEssentials.LOGGER.error("Error loading user data for player {}", player.getScoreboardName(), e);
            // Create default data in case of error
            JsonObject userData = createDefaultUserData(player);
            userDataMap.put(playerId, userData);
        }
    }
    
    /**
     * Save a player's data to disk
     * 
     * @param player The player to save data for
     */
    public void savePlayerData(ServerPlayer player) {
        UUID playerId = player.getUUID();
        
        if (userDataMap.containsKey(playerId)) {
            JsonObject userData = userDataMap.get(playerId);
            saveUserData(playerId, userData);
        }
    }
    
    /**
     * Save user data to disk
     * 
     * @param playerId The UUID of the player
     * @param userData The user data to save
     */
    private void saveUserData(UUID playerId, JsonObject userData) {
        try {
            File userDataFile = getUserDataFile(playerId);
            
            // Create parent directories if they don't exist
            File parentDir = userDataFile.getParentFile();
            if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
                NeoEssentials.LOGGER.error("Failed to create directory for user data: {}", parentDir);
                return;
            }
            
            // Write to file
            try (FileWriter writer = new FileWriter(userDataFile)) {
                gson.toJson(userData, writer);
            }
            
        } catch (IOException e) {
            NeoEssentials.LOGGER.error("Error saving user data for player {}", playerId, e);
        }
    }
    
    /**
     * Get a player's user data
     * 
     * @param playerId The UUID of the player
     * @return The player's user data, or null if not found
     */
    public JsonObject getUserData(UUID playerId) {
        return userDataMap.get(playerId);
    }
    
    /**
     * Get a player's user data
     * 
     * @param player The player
     * @return The player's user data, or null if not found
     */
    public JsonObject getUserData(ServerPlayer player) {
        return getUserData(player.getUUID());
    }
    
    /**
     * Set a value in a player's user data
     * 
     * @param playerId The UUID of the player
     * @param key The key to set
     * @param value The value to set
     */
    public void setUserData(UUID playerId, String key, String value) {
        JsonObject userData = userDataMap.get(playerId);
        if (userData != null) {
            userData.addProperty(key, value);
        }
    }
    
    /**
     * Set a numeric value in a player's user data
     * 
     * @param playerId The UUID of the player
     * @param key The key to set
     * @param value The value to set
     */
    public void setUserData(UUID playerId, String key, Number value) {
        JsonObject userData = userDataMap.get(playerId);
        if (userData != null) {
            userData.addProperty(key, value);
        }
    }
    
    /**
     * Set a boolean value in a player's user data
     * 
     * @param playerId The UUID of the player
     * @param key The key to set
     * @param value The value to set
     */
    public void setUserData(UUID playerId, String key, boolean value) {
        JsonObject userData = userDataMap.get(playerId);
        if (userData != null) {
            userData.addProperty(key, value);
        }
    }
    
    /**
     * Get a string value from a player's user data
     * 
     * @param playerId The UUID of the player
     * @param key The key to get
     * @param defaultValue The default value to return if the key doesn't exist
     * @return The value, or the default value if not found
     */
    public String getUserDataString(UUID playerId, String key, String defaultValue) {
        JsonObject userData = userDataMap.get(playerId);
        if (userData != null && userData.has(key)) {
            return userData.get(key).getAsString();
        }
        return defaultValue;
    }
    
    /**
     * Get a numeric value from a player's user data
     * 
     * @param playerId The UUID of the player
     * @param key The key to get
     * @param defaultValue The default value to return if the key doesn't exist
     * @return The value, or the default value if not found
     */
    public double getUserDataNumber(UUID playerId, String key, double defaultValue) {
        JsonObject userData = userDataMap.get(playerId);
        if (userData != null && userData.has(key)) {
            return userData.get(key).getAsDouble();
        }
        return defaultValue;
    }
    
    /**
     * Get a boolean value from a player's user data
     * 
     * @param playerId The UUID of the player
     * @param key The key to get
     * @param defaultValue The default value to return if the key doesn't exist
     * @return The value, or the default value if not found
     */
    public boolean getUserDataBoolean(UUID playerId, String key, boolean defaultValue) {
        JsonObject userData = userDataMap.get(playerId);
        if (userData != null && userData.has(key)) {
            return userData.get(key).getAsBoolean();
        }
        return defaultValue;
    }
    
    /**
     * Create default user data for a player
     * 
     * @param player The player to create data for
     * @return The default user data
     */
    private JsonObject createDefaultUserData(ServerPlayer player) {
        JsonObject userData = new JsonObject();
        
        // Basic player info
        userData.addProperty("name", player.getScoreboardName());
        userData.addProperty("uuid", player.getStringUUID());
        userData.addProperty("firstJoin", System.currentTimeMillis());
        userData.addProperty("lastSeen", System.currentTimeMillis());
        
        // Other default values
        userData.addProperty("balance", NeoEssentials.getInstance().getConfigManager().getConfig().getStartingBalance());
        
        return userData;
    }
      /**
     * Get the user data file for a player
     * 
     * @param playerId The UUID of the player
     * @return The user data file
     */
    private File getUserDataFile(UUID playerId) {
        return new File(USER_DATA_DIR, playerId + ".json");
    }
    
    /**
     * Get the username associated with a UUID
     * If the player is not currently cached, attempts to read from their data file
     * 
     * @param playerId The UUID of the player
     * @return The player's username, or null if not found
     */
    public String getUsername(UUID playerId) {
        // First check the cache
        if (userDataMap.containsKey(playerId)) {
            JsonObject userData = userDataMap.get(playerId);
            if (userData.has("name")) {
                return userData.get("name").getAsString();
            }
        }
        
        // Not in cache, try to read from file
        try {
            File userDataFile = getUserDataFile(playerId);
            if (userDataFile.exists()) {
                try (FileReader reader = new FileReader(userDataFile)) {
                    JsonObject userData = gson.fromJson(reader, JsonObject.class);
                    if (userData != null && userData.has("name")) {
                        return userData.get("name").getAsString();
                    }
                }
            }
        } catch (IOException e) {
            NeoEssentials.LOGGER.warn("Error reading username from file for UUID: {}", playerId, e);
        }
        
        // Still not found
        return null;
    }
    
    /**
     * Keep track of username to UUID mappings for quick lookups
     * This should be called whenever a player joins
     * 
     * @param player The player to track
     */
    public void trackPlayer(ServerPlayer player) {
        UUID playerId = player.getUUID();
        JsonObject userData = getUserData(playerId);
        
        if (userData != null) {
            userData.addProperty("name", player.getScoreboardName());
            userData.addProperty("lastSeen", System.currentTimeMillis());
        }
    }
    
    /**
     * Gets a player's UUID from their name by scanning all user data files
     * 
     * @param playerName The name of the player to find
     * @return The UUID of the player, or null if not found
     */
    public UUID getPlayerUUID(String playerName) {
        // First try to find the player in the currently loaded data
        for (Map.Entry<UUID, JsonObject> entry : userDataMap.entrySet()) {
            JsonObject userData = entry.getValue();
            if (userData.has("name") && userData.get("name").getAsString().equalsIgnoreCase(playerName)) {
                return entry.getKey();
            }
        }
        
        // If not found in memory, search through the data files
        File userDataDir = new File(USER_DATA_DIR);
        if (!userDataDir.exists() || !userDataDir.isDirectory()) {
            return null;
        }
        
        File[] userFiles = userDataDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (userFiles == null || userFiles.length == 0) {
            return null;
        }
        
        for (File userFile : userFiles) {
            try (FileReader reader = new FileReader(userFile)) {
                JsonObject userData = gson.fromJson(reader, JsonObject.class);
                if (userData != null && userData.has("name") && 
                        userData.get("name").getAsString().equalsIgnoreCase(playerName)) {
                    String fileName = userFile.getName();
                    String uuidStr = fileName.substring(0, fileName.length() - 5); // Remove .json extension
                    return UUID.fromString(uuidStr);
                }
            } catch (Exception e) {
                NeoEssentials.LOGGER.error("Error reading user data file while looking for player name: {}", playerName, e);
            }
        }
        
        return null;
    }
}
