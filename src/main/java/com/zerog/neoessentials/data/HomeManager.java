package com.zerog.neoessentials.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.zerog.neoessentials.NeoEssentials;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages home data for the NeoEssentials mod.
 */
public class HomeManager {
    private static final String HOME_DATA_DIR = "neoessentials/homes";
      // Map of player UUID to their homes
    // Each player has a map of home name to location
    private final Map<UUID, Map<String, HomeLocation>> playerHomes = new ConcurrentHashMap<>();
    
    private final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();
    
    /**
     * Initialize the home manager
     */
    public void initialize() {
        NeoEssentials.LOGGER.info("Initializing NeoEssentials Home Manager");
        
        // Create home data directory if it doesn't exist
        File homeDataDir = new File(HOME_DATA_DIR);
        if (!homeDataDir.exists()) {
            if (homeDataDir.mkdirs()) {
                NeoEssentials.LOGGER.info("Created home data directory: {}", homeDataDir);
            } else {
                NeoEssentials.LOGGER.error("Failed to create home data directory: {}", homeDataDir);
            }
        }
    }
    
    /**
     * Save all home data to disk
     */
    public void saveAll() {
        NeoEssentials.LOGGER.info("Saving home data");
        
        for (Map.Entry<UUID, Map<String, HomeLocation>> entry : playerHomes.entrySet()) {
            savePlayerHomes(entry.getKey());
        }
    }
    
    /**
     * Load a player's homes from disk
     * 
     * @param playerId The UUID of the player
     */
    public void loadPlayerHomes(UUID playerId) {
        try {
            File homeFile = new File(HOME_DATA_DIR, playerId.toString() + ".json");
            
            if (homeFile.exists()) {
                try (FileReader reader = new FileReader(homeFile)) {
                    Type type = new TypeToken<Map<String, HomeLocation>>() {}.getType();
                    Map<String, HomeLocation> homes = gson.fromJson(reader, type);
                    
                    if (homes != null) {
                        playerHomes.put(playerId, homes);
                    } else {
                        playerHomes.put(playerId, new HashMap<>());
                    }
                }
            } else {
                playerHomes.put(playerId, new HashMap<>());
            }
        } catch (IOException e) {
            NeoEssentials.LOGGER.error("Error loading home data for player {}", playerId, e);
            playerHomes.put(playerId, new HashMap<>());
        }
    }
    
    /**
     * Save a player's homes to disk
     * 
     * @param playerId The UUID of the player
     */
    private void savePlayerHomes(UUID playerId) {
        try {
            File homeFile = new File(HOME_DATA_DIR, playerId.toString() + ".json");
            
            // Create parent directories if they don't exist
            File parentDir = homeFile.getParentFile();
            if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
                NeoEssentials.LOGGER.error("Failed to create directory for home data: {}", parentDir);
                return;
            }
            
            Map<String, HomeLocation> homes = playerHomes.get(playerId);
            
            if (homes != null && !homes.isEmpty()) {
                try (FileWriter writer = new FileWriter(homeFile)) {
                    gson.toJson(homes, writer);
                }
            } else if (homeFile.exists()) {
                homeFile.delete();
            }
        } catch (IOException e) {
            NeoEssentials.LOGGER.error("Error saving home data for player {}", playerId, e);
        }
    }
      /**
     * Sets a home for a player
     * 
     * @param player The player
     * @param homeName The name of the home
     * @return True if the home was set successfully, false otherwise
     */
    public boolean setHome(ServerPlayer player, String homeName) {
        UUID playerId = player.getUUID();
        
        // Check if player has homes map, if not load it
        if (!playerHomes.containsKey(playerId)) {
            loadPlayerHomes(playerId);
        }
        
        // Check if player has reached their home limit
        if (!homeName.equalsIgnoreCase("home") && hasReachedHomeLimit(playerId)) {
            return false;
        }
        
        // Create the home location
        HomeLocation homeLocation = new HomeLocation(
            player.level().dimension().location().toString(),
            player.getX(),
            player.getY(),
            player.getZ(),
            player.getYRot(),
            player.getXRot()
        );
        
        // Add the home to the player's homes
        Map<String, HomeLocation> homes = playerHomes.get(playerId);
        homes.put(homeName.toLowerCase(), homeLocation);
        
        // Save the player's homes
        savePlayerHomes(playerId);
        
        return true;
    }
    
    /**
     * Checks if a player has reached their home limit
     * 
     * @param playerId The UUID of the player
     * @return True if the player has reached their limit, false otherwise
     */
    private boolean hasReachedHomeLimit(UUID playerId) {
        Map<String, HomeLocation> homes = playerHomes.get(playerId);
        if (homes == null) {
            return false;
        }
        
        // TODO: Get max homes from config or permissions
        int maxHomes = 3; // Default max homes
        return homes.size() >= maxHomes;
    }
    
    /**
     * Deletes a home for a player
     * 
     * @param playerId The UUID of the player
     * @param homeName The name of the home
     * @return True if the home was deleted successfully, false otherwise
     */
    public boolean deleteHome(UUID playerId, String homeName) {
        if (!playerHomes.containsKey(playerId)) {
            loadPlayerHomes(playerId);
        }
        
        Map<String, HomeLocation> homes = playerHomes.get(playerId);
        if (homes == null || !homes.containsKey(homeName.toLowerCase())) {
            return false;
        }
        
        homes.remove(homeName.toLowerCase());
        savePlayerHomes(playerId);
        
        return true;
    }
    
    /**
     * Gets the location of a player's home
     * 
     * @param playerId The UUID of the player
     * @param homeName The name of the home
     * @return The home location, or null if the home doesn't exist
     */
    public HomeLocation getHome(UUID playerId, String homeName) {
        if (!playerHomes.containsKey(playerId)) {
            loadPlayerHomes(playerId);
        }
        
        Map<String, HomeLocation> homes = playerHomes.get(playerId);
        if (homes == null) {
            return null;
        }
        
        return homes.get(homeName.toLowerCase());
    }
    
    /**
     * Gets all homes for a player
     * 
     * @param playerId The UUID of the player
     * @return A map of home names to locations
     */
    public Map<String, HomeLocation> getHomes(UUID playerId) {
        if (!playerHomes.containsKey(playerId)) {
            loadPlayerHomes(playerId);
        }
        
        return playerHomes.getOrDefault(playerId, new HashMap<>());
    }
    
    /**
     * Class to store home location data
     */
    public static class HomeLocation {
        private String dimension;
        private double x;
        private double y;
        private double z;
        private float yaw;
        private float pitch;
        
        public HomeLocation(String dimension, double x, double y, double z, float yaw, float pitch) {
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
        }
        
        public String getDimension() {
            return dimension;
        }
        
        public double getX() {
            return x;
        }
        
        public double getY() {
            return y;
        }
        
        public double getZ() {
            return z;
        }
        
        public float getYaw() {
            return yaw;
        }
        
        public float getPitch() {
            return pitch;
        }
        
        /**
         * Gets the server level for this location
         * 
         * @param server The Minecraft server
         * @return The server level, or null if the dimension doesn't exist
         */
        public ServerLevel getLevel(MinecraftServer server) {
            try {
                ResourceLocation dimLocation = ResourceLocation.tryParse(dimension);
                if (dimLocation == null) {
                    return server.overworld(); // Default to overworld
                }
                
                ResourceKey<Level> levelKey = ResourceKey.create(Registries.DIMENSION, dimLocation);
                return server.getLevel(levelKey);
            } catch (Exception e) {
                // Return the overworld as fallback
                return server.overworld();
            }
        }
        
        /**
         * Gets the BlockPos for this location
         * 
         * @return The BlockPos
         */
        public BlockPos getBlockPos() {
            return BlockPos.containing(x, y, z);
        }
    }
}
