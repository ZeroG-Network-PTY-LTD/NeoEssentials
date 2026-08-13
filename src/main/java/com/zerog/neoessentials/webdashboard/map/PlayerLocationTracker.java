package com.zerog.neoessentials.webdashboard.map;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Real-time player location tracking for map viewer
 * Tracks player positions, dimensions, health, and updates for live map display
 */
public class PlayerLocationTracker {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerLocationTracker.class);
    private static PlayerLocationTracker INSTANCE;
    
    // Player locations: UUID -> PlayerLocation
    private final Map<UUID, PlayerLocation> playerLocations = new ConcurrentHashMap<>();
    
    // Location update listeners
    private final List<LocationUpdateListener> listeners = new ArrayList<>();
    
    private PlayerLocationTracker() {
    }
    
    public static PlayerLocationTracker getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new PlayerLocationTracker();
        }
        return INSTANCE;
    }
    
    /**
     * Update player location from server tick or event
     */
    public void updatePlayerLocation(ServerPlayer player) {
        try {
            PlayerLocation location = new PlayerLocation();
            location.uuid = player.getUUID();
            location.name = player.getGameProfile().name();
            location.x = player.getX();
            location.y = player.getY();
            location.z = player.getZ();
            location.yaw = player.getYRot();
            location.pitch = player.getXRot();
            location.health = player.getHealth();
            location.maxHealth = player.getMaxHealth();
            location.dimension = player.level().dimension().identifier().toString();
            location.blockX = player.blockPosition().getX();
            location.blockY = player.blockPosition().getY();
            location.blockZ = player.blockPosition().getZ();
            location.chunkX = player.chunkPosition().x();
            location.chunkZ = player.chunkPosition().z();
            location.timestamp = System.currentTimeMillis();
            
            playerLocations.put(player.getUUID(), location);
            
            // Notify listeners of location update
            notifyListeners(location);
            
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "Error updating player location: {}", player.getGameProfile().name(), e);
        }
    }
    
    /**
     * Remove player location when they disconnect
     */
    public void removePlayer(UUID playerId) {
        playerLocations.remove(playerId);
    }
    
    /**
     * Get all current player locations
     */
    public Collection<PlayerLocation> getAllPlayerLocations() {
        return new ArrayList<>(playerLocations.values());
    }
    
    /**
     * Get player locations for specific dimension
     */
    public List<PlayerLocation> getPlayerLocationsInDimension(String dimension) {
        List<PlayerLocation> locations = new ArrayList<>();
        for (PlayerLocation loc : playerLocations.values()) {
            if (loc.dimension.equals(dimension)) {
                locations.add(loc);
            }
        }
        return locations;
    }
    
    /**
     * Get specific player location
     */
    public PlayerLocation getPlayerLocation(UUID playerId) {
        return playerLocations.get(playerId);
    }
    
    /**
     * Get all tracked dimensions
     */
    public Set<String> getTrackedDimensions() {
        Set<String> dimensions = new HashSet<>();
        for (PlayerLocation loc : playerLocations.values()) {
            dimensions.add(loc.dimension);
        }
        return dimensions;
    }
    
    /**
     * Convert player locations to JSON for API response
     */
    public JsonObject getPlayerLocationsJson() {
        JsonObject response = new JsonObject();
        response.addProperty("timestamp", System.currentTimeMillis());
        response.addProperty("playerCount", playerLocations.size());
        
        JsonArray playersArray = new JsonArray();
        for (PlayerLocation loc : playerLocations.values()) {
            JsonObject playerObj = new JsonObject();
            playerObj.addProperty("uuid", loc.uuid.toString());
            playerObj.addProperty("name", loc.name);
            playerObj.addProperty("x", loc.x);
            playerObj.addProperty("y", loc.y);
            playerObj.addProperty("z", loc.z);
            playerObj.addProperty("blockX", loc.blockX);
            playerObj.addProperty("blockY", loc.blockY);
            playerObj.addProperty("blockZ", loc.blockZ);
            playerObj.addProperty("chunkX", loc.chunkX);
            playerObj.addProperty("chunkZ", loc.chunkZ);
            playerObj.addProperty("yaw", loc.yaw);
            playerObj.addProperty("pitch", loc.pitch);
            playerObj.addProperty("health", loc.health);
            playerObj.addProperty("maxHealth", loc.maxHealth);
            playerObj.addProperty("dimension", loc.dimension);
            playerObj.addProperty("timestamp", loc.timestamp);
            playersArray.add(playerObj);
        }
        
        response.add("players", playersArray);
        return response;
    }
    
    /**
     * Get player locations JSON filtered by dimension
     */
    public JsonObject getPlayerLocationsJson(String dimension) {
        JsonObject response = new JsonObject();
        response.addProperty("timestamp", System.currentTimeMillis());
        response.addProperty("dimension", dimension);
        
        List<PlayerLocation> locations = getPlayerLocationsInDimension(dimension);
        response.addProperty("playerCount", locations.size());
        
        JsonArray playersArray = new JsonArray();
        for (PlayerLocation loc : locations) {
            JsonObject playerObj = new JsonObject();
            playerObj.addProperty("uuid", loc.uuid.toString());
            playerObj.addProperty("name", loc.name);
            playerObj.addProperty("x", loc.x);
            playerObj.addProperty("y", loc.y);
            playerObj.addProperty("z", loc.z);
            playerObj.addProperty("blockX", loc.blockX);
            playerObj.addProperty("blockY", loc.blockY);
            playerObj.addProperty("blockZ", loc.blockZ);
            playerObj.addProperty("chunkX", loc.chunkX);
            playerObj.addProperty("chunkZ", loc.chunkZ);
            playerObj.addProperty("yaw", loc.yaw);
            playerObj.addProperty("pitch", loc.pitch);
            playerObj.addProperty("health", loc.health);
            playerObj.addProperty("maxHealth", loc.maxHealth);
            playerObj.addProperty("timestamp", loc.timestamp);
            playersArray.add(playerObj);
        }
        
        response.add("players", playersArray);
        return response;
    }
    
    /**
     * Add location update listener
     */
    public void addListener(LocationUpdateListener listener) {
        listeners.add(listener);
    }
    
    /**
     * Remove location update listener
     */
    public void removeListener(LocationUpdateListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Notify all listeners of location update
     */
    private void notifyListeners(PlayerLocation location) {
        for (LocationUpdateListener listener : listeners) {
            try {
                listener.onLocationUpdate(location);
            } catch (Exception e) {
                NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "Error notifying location listener", e);
            }
        }
    }
    
    /**
     * Player location data structure
     */
    public static class PlayerLocation {
        public UUID uuid;
        public String name;
        public double x;
        public double y;
        public double z;
        public int blockX;
        public int blockY;
        public int blockZ;
        public int chunkX;
        public int chunkZ;
        public float yaw;
        public float pitch;
        public float health;
        public float maxHealth;
        public String dimension;
        public long timestamp;
    }
    
    /**
     * Location update listener interface
     */
    public interface LocationUpdateListener {
        void onLocationUpdate(PlayerLocation location);
    }
}
