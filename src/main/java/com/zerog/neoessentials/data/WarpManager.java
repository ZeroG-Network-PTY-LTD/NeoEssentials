package com.zerog.neoessentials.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.zerog.neoessentials.NeoEssentials;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages warp data for the NeoEssentials mod.
 */
public class WarpManager {
    private static final String WARP_DATA_FILE = "neoessentials/warps.json";
    
    // Map of warp names to locations
    private final Map<String, WarpLocation> warps = new HashMap<>();
    
    private final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();
    
    /**
     * Initialize the warp manager
     */
    public void initialize() {
        NeoEssentials.LOGGER.info("Initializing NeoEssentials Warp Manager");
        
        loadWarps();
    }
    
    /**
     * Save all warp data to disk
     */
    public void saveAll() {
        NeoEssentials.LOGGER.info("Saving warp data");
        
        saveWarps();
    }
    
    /**
     * Load warps from disk
     */
    private void loadWarps() {
        try {
            File warpFile = new File(WARP_DATA_FILE);
            
            if (warpFile.exists()) {
                try (FileReader reader = new FileReader(warpFile)) {
                    Type type = new TypeToken<Map<String, WarpLocation>>() {}.getType();
                    Map<String, WarpLocation> loadedWarps = gson.fromJson(reader, type);
                    
                    if (loadedWarps != null) {
                        warps.putAll(loadedWarps);
                    }
                }
            }
        } catch (IOException e) {
            NeoEssentials.LOGGER.error("Error loading warp data", e);
        }
    }
    
    /**
     * Save warps to disk
     */
    private void saveWarps() {
        try {
            File warpFile = new File(WARP_DATA_FILE);
            
            // Create parent directories if they don't exist
            File parentDir = warpFile.getParentFile();
            if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
                NeoEssentials.LOGGER.error("Failed to create directory for warp data: {}", parentDir);
                return;
            }
            
            try (FileWriter writer = new FileWriter(warpFile)) {
                gson.toJson(warps, writer);
            }
        } catch (IOException e) {
            NeoEssentials.LOGGER.error("Error saving warp data", e);
        }
    }
      /**
     * Sets a warp at the player's current location
     * 
     * @param player The player setting the warp
     * @param warpName The name of the warp
     * @return True if the warp was set successfully, false otherwise
     */
    public boolean setWarp(ServerPlayer player, String warpName) {
        if (player == null || warpName == null || warpName.isEmpty()) {
            return false;
        }
        
        // Create a new warp location from the player's current position
        ResourceKey<Level> dimension = player.level().dimension();
        WarpLocation location = new WarpLocation(
            dimension.location().toString(),
            player.getX(),
            player.getY(),
            player.getZ(),
            player.getYRot(),
            player.getXRot()
        );
        
        // Add or update the warp
        warps.put(warpName.toLowerCase(), location);
        
        // Save the warps to disk
        saveWarps();
        
        return true;
    }
    
    /**
     * Deletes a warp
     * 
     * @param warpName The name of the warp to delete
     * @return True if the warp was deleted successfully, false otherwise
     */
    public boolean deleteWarp(String warpName) {
        if (warpName == null || warpName.isEmpty()) {
            return false;
        }
        
        // Try to remove the warp
        WarpLocation removed = warps.remove(warpName.toLowerCase());
        
        // If the warp was removed, save the warps to disk
        if (removed != null) {
            saveWarps();
            return true;
        }
        
        return false;
    }
    
    /**
     * Gets the location of a warp
     * 
     * @param warpName The name of the warp
     * @return The warp location, or null if the warp doesn't exist
     */
    public WarpLocation getWarp(String warpName) {
        if (warpName == null || warpName.isEmpty()) {
            return null;
        }
        
        return warps.get(warpName.toLowerCase());
    }
    
    /**
     * Gets all warps
     * 
     * @return A map of warp names to locations
     */
    public Map<String, WarpLocation> getWarps() {
        return new HashMap<>(warps);
    }
    
    /**
     * Class to store warp location data
     */
    public static class WarpLocation {
        private String dimension;
        private double x;
        private double y;
        private double z;
        private float yaw;
        private float pitch;
        
        public WarpLocation(String dimension, double x, double y, double z, float yaw, float pitch) {
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
    }
}
