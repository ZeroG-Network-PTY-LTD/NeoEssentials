package com.zerog.neoessentials.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.utils.TeleportUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages persistent teleport history for players.
 * Handles saving and loading teleport history to/from JSON files.
 */
public class TeleportHistoryManager {
    
    private static final String HISTORY_DIR = "neoessentials/teleport_history";
    private static final String HISTORY_FILE = "player_teleport_history.json";
    private static final int MAX_HISTORY_SIZE = 25;
    
    // In-memory storage for quick access
    private final Map<UUID, Deque<TeleportLocation>> playerHistory = new ConcurrentHashMap<>();
    
    private final Gson gson;
    private final Path historyFile;
    
    public TeleportHistoryManager() {
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();
        
        // Initialize storage directory
        Path historyDir = Paths.get(HISTORY_DIR);
        try {
            Files.createDirectories(historyDir);
            this.historyFile = historyDir.resolve(HISTORY_FILE);
        } catch (IOException e) {
            NeoEssentials.LOGGER.error("Failed to create teleport history directory: {}", e.getMessage());
            throw new RuntimeException("Could not initialize teleport history storage", e);
        }
        
        loadHistory();
    }
    
    /**
     * Records a player's position before teleporting
     * 
     * @param player The player being teleported
     */
    public void recordPosition(ServerPlayer player) {
        if (player == null) return;
        
        UUID playerUuid = player.getUUID();
        ServerLevel level = player.serverLevel();
        
        // Create a new teleport location
        TeleportLocation location = new TeleportLocation(
            level.dimension().location().toString(),
            player.getX(),
            player.getY(),
            player.getZ(),
            player.getYRot(),
            player.getXRot(),
            System.currentTimeMillis()
        );
        
        // Get or create the history stack for this player
        Deque<TeleportLocation> history = playerHistory.computeIfAbsent(playerUuid, k -> new ArrayDeque<>());
        
        // Add the location to the history stack
        history.push(location);
        
        // Trim the history if it's too large
        while (history.size() > MAX_HISTORY_SIZE) {
            history.pollLast();
        }
        
        // Save to disk
        saveHistory();
    }
    
    /**
     * Teleports a player back to their previous location
     * 
     * @param player The player to teleport
     * @return True if teleport was successful, false otherwise
     */
    public boolean teleportBack(ServerPlayer player) {
        if (player == null) return false;
        
        UUID playerUuid = player.getUUID();
        Deque<TeleportLocation> history = playerHistory.get(playerUuid);
        
        // Check if the player has a history
        if (history == null || history.isEmpty()) {
            return false;
        }
        
        // Get the last location
        TeleportLocation lastLocation = history.pop();
        
        // Find the dimension
        ServerLevel targetLevel = null;
        for (ServerLevel level : player.getServer().getAllLevels()) {
            if (level.dimension().location().toString().equals(lastLocation.dimension)) {
                targetLevel = level;
                break;
            }
        }
        
        if (targetLevel == null) {
            NeoEssentials.LOGGER.error("Could not find dimension for teleport history: {}", lastLocation.dimension);
            return false;
        }
        
        // Teleport the player
        boolean success = TeleportUtil.teleport(player, targetLevel, 
            lastLocation.x, lastLocation.y, lastLocation.z, 
            lastLocation.yaw, lastLocation.pitch);
        
        if (success) {
            // Save the updated history
            saveHistory();
        }
        
        return success;
    }
    
    /**
     * Gets the teleport history for a player
     * 
     * @param playerId The player's UUID
     * @return List of teleport locations in chronological order (newest first)
     */
    public List<TeleportLocation> getPlayerHistory(UUID playerId) {
        Deque<TeleportLocation> history = playerHistory.get(playerId);
        if (history == null || history.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(history);
    }
    
    /**
     * Clears all teleport history for a player
     * 
     * @param playerId The player's UUID
     */
    public void clearPlayerHistory(UUID playerId) {
        playerHistory.remove(playerId);
        saveHistory();
    }
    
    /**
     * Saves all teleport history to disk
     */
    private void saveHistory() {
        try {
            // Convert to saveable format
            Map<String, List<TeleportLocation>> saveData = new HashMap<>();
            for (Map.Entry<UUID, Deque<TeleportLocation>> entry : playerHistory.entrySet()) {
                saveData.put(entry.getKey().toString(), new ArrayList<>(entry.getValue()));
            }
            
            try (FileWriter writer = new FileWriter(historyFile.toFile())) {
                gson.toJson(saveData, writer);
            }
        } catch (IOException e) {
            NeoEssentials.LOGGER.error("Failed to save teleport history: {}", e.getMessage());
        }
    }
    
    /**
     * Loads teleport history from disk
     */
    private void loadHistory() {
        if (!Files.exists(historyFile)) {
            return;
        }
        
        try (FileReader reader = new FileReader(historyFile.toFile())) {
            Type type = new TypeToken<Map<String, List<TeleportLocation>>>(){}.getType();
            Map<String, List<TeleportLocation>> loadData = gson.fromJson(reader, type);
            
            if (loadData != null) {
                for (Map.Entry<String, List<TeleportLocation>> entry : loadData.entrySet()) {
                    UUID playerId = UUID.fromString(entry.getKey());
                    Deque<TeleportLocation> history = new ArrayDeque<>(entry.getValue());
                    playerHistory.put(playerId, history);
                }
            }
            
            NeoEssentials.LOGGER.info("Loaded teleport history for {} players", playerHistory.size());
        } catch (IOException e) {
            NeoEssentials.LOGGER.error("Failed to load teleport history: {}", e.getMessage());
        }
    }
    
    /**
     * Saves all data when the server shuts down
     */
    public void shutdown() {
        saveHistory();
    }
    
    /**
     * Class to store teleport location data with timestamp
     */
    public static class TeleportLocation {
        private final String dimension;
        private final double x;
        private final double y;
        private final double z;
        private final float yaw;
        private final float pitch;
        private final long timestamp;
        
        public TeleportLocation(String dimension, double x, double y, double z, float yaw, float pitch, long timestamp) {
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
            this.timestamp = timestamp;
        }
        
        // Getters
        public String getDimension() { return dimension; }
        public double getX() { return x; }
        public double getY() { return y; }
        public double getZ() { return z; }
        public float getYaw() { return yaw; }
        public float getPitch() { return pitch; }
        public long getTimestamp() { return timestamp; }
    }
}
