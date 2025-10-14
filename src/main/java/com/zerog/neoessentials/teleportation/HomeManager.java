package com.zerog.neoessentials.teleportation;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zerog.neoessentials.util.ResourceUtil;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player home locations with creation, deletion, listing, and teleportation
 */
public class HomeManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(HomeManager.class);
    private static final String HOMES_FILE = "homes.json";
    
    // Singleton pattern
    private static class SingletonHolder {
        private static final HomeManager INSTANCE = new HomeManager();
    }
    
    public static HomeManager getInstance() {
        return SingletonHolder.INSTANCE;
    }
    
    private final Map<UUID, Map<String, TeleportLocation>> playerHomes = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();
    
    // Configuration
    private int maxHomesPerPlayer = 5;
    private boolean allowOverworldOnly = false;
    private boolean requireSafeLocations = true;
    private int teleportDelay = 3; // seconds
    
    private HomeManager() {
        loadHomes();
    }
    
    /**
     * Set a home for a player
     */
    public boolean setHome(ServerPlayer player, String homeName) {
        return setHome(player, homeName, null);
    }
    
    /**
     * Set a home for a player at a specific location
     */
    public boolean setHome(ServerPlayer player, String homeName, TeleportLocation customLocation) {
        UUID playerId = player.getUUID();
        
        // Validate home name
        if (!isValidHomeName(homeName)) {
            player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.home.invalid_name", homeName));
            return false;
        }
        
        // Check if player has reached home limit
        Map<String, TeleportLocation> homes = playerHomes.computeIfAbsent(playerId, k -> new HashMap<>());
        if (!homes.containsKey(homeName) && homes.size() >= maxHomesPerPlayer) {
            player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.home.limit_reached", maxHomesPerPlayer));
            return false;
        }
        
        // Create location
        TeleportLocation location = customLocation != null ? customLocation : new TeleportLocation(player);
        
        // Check world restriction
        if (allowOverworldOnly && !isOverworld(location)) {
            player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.home.overworld_only"));
            return false;
        }
        
        // Check if location is safe
        if (requireSafeLocations && !location.isSafe()) {
            TeleportLocation safeLocation = location.findSafeLocation();
            if (safeLocation == null) {
                player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.home.unsafe_location"));
                return false;
            }
            location = safeLocation;
        }
        
        // Set the home
        boolean isNew = !homes.containsKey(homeName);
        homes.put(homeName, location);
        
        // Save to file
        saveHomes();
        
        if (isNew) {
            player.sendSystemMessage(MessageUtil.success("commands.neoessentials.teleport.home.set", homeName, location.getLocationString()));
        } else {
            player.sendSystemMessage(MessageUtil.success("commands.neoessentials.teleport.home.updated", homeName, location.getLocationString()));
        }
        
        LOGGER.info("Player {} {} home '{}' at {}", 
                   player.getName().getString(), 
                   isNew ? "set" : "updated", 
                   homeName, 
                   location.getLocationString());
        
        return true;
    }
    
    /**
     * Delete a home for a player
     */
    public boolean deleteHome(ServerPlayer player, String homeName) {
        UUID playerId = player.getUUID();
        Map<String, TeleportLocation> homes = playerHomes.get(playerId);
        
        if (homes == null || !homes.containsKey(homeName)) {
            player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.home.not_found", homeName));
            return false;
        }
        
        homes.remove(homeName);
        if (homes.isEmpty()) {
            playerHomes.remove(playerId);
        }
        
        saveHomes();
        
        player.sendSystemMessage(MessageUtil.success("commands.neoessentials.teleport.home.deleted", homeName));
        LOGGER.info("Player {} deleted home '{}'", player.getName().getString(), homeName);
        
        return true;
    }
    
    /**
     * Get a specific home for a player
     */
    public TeleportLocation getHome(ServerPlayer player, String homeName) {
        UUID playerId = player.getUUID();
        Map<String, TeleportLocation> homes = playerHomes.get(playerId);
        
        if (homes == null) {
            return null;
        }
        
        return homes.get(homeName);
    }
    
    /**
     * Get all homes for a player
     */
    public Map<String, TeleportLocation> getPlayerHomes(ServerPlayer player) {
        UUID playerId = player.getUUID();
        Map<String, TeleportLocation> homes = playerHomes.get(playerId);
        return homes != null ? new HashMap<>(homes) : new HashMap<>();
    }
    
    /**
     * Get list of home names for a player
     */
    public List<String> getHomeNames(ServerPlayer player) {
        Map<String, TeleportLocation> homes = getPlayerHomes(player);
        return new ArrayList<>(homes.keySet());
    }
    
    /**
     * Teleport player to their home
     */
    public void teleportToHome(ServerPlayer player, String homeName) {
        TeleportLocation home = getHome(player, homeName);
        
        if (home == null) {
            player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.home.not_found", homeName));
            return;
        }
        
        // Check if home location is still safe
        if (requireSafeLocations && !home.isSafe()) {
            TeleportLocation safeLocation = home.findSafeLocation();
            if (safeLocation == null) {
                player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.home.unsafe", homeName));
                return;
            }
            
            // Update home to safe location
            UUID playerId = player.getUUID();
            playerHomes.get(playerId).put(homeName, safeLocation);
            saveHomes();
            home = safeLocation;
            
            player.sendSystemMessage(MessageUtil.warning("commands.neoessentials.teleport.home.moved_to_safety", homeName));
        }
        
        // Perform teleportation
        int delayTicks = teleportDelay * 20; // Convert seconds to ticks
        TeleportUtil.teleportPlayer(player, home, delayTicks, true).thenAccept(result -> {
            if (result.isSuccess()) {
                player.sendSystemMessage(MessageUtil.success("commands.neoessentials.teleport.home.success", homeName));
                LOGGER.info("Player {} teleported to home '{}'", player.getName().getString(), homeName);
            } else {
                player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.home.failed", homeName, result.getMessage()));
                LOGGER.warn("Failed to teleport player {} to home '{}': {}", player.getName().getString(), homeName, result.getMessage());
            }
        });
    }
    
    /**
     * Teleport to default home (first home or "home")
     */
    public void teleportToDefaultHome(ServerPlayer player) {
        Map<String, TeleportLocation> homes = getPlayerHomes(player);
        
        if (homes.isEmpty()) {
            player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.home.none_set"));
            return;
        }
        
        // Try "home" first, then first alphabetically
        String homeName = homes.containsKey("home") ? "home" : homes.keySet().iterator().next();
        teleportToHome(player, homeName);
    }
    
    /**
     * Get formatted list of homes for display
     */
    public String getFormattedHomesList(ServerPlayer player) {
        Map<String, TeleportLocation> homes = getPlayerHomes(player);
        
        if (homes.isEmpty()) {
            return MessageUtil.localize("commands.neoessentials.teleport.home.list_empty");
        }
        
        StringBuilder builder = new StringBuilder();
        builder.append(MessageUtil.localize("commands.neoessentials.teleport.home.list_header", homes.size(), maxHomesPerPlayer));
        
        List<String> sortedNames = new ArrayList<>(homes.keySet());
        Collections.sort(sortedNames);
        
        for (String homeName : sortedNames) {
            TeleportLocation location = homes.get(homeName);
            builder.append("\n  §e").append(homeName).append("§r: ")
                   .append(location.getLocationString());
        }
        
        return builder.toString();
    }
    
    /**
     * Check if player has any homes
     */
    public boolean hasHomes(ServerPlayer player) {
        UUID playerId = player.getUUID();
        Map<String, TeleportLocation> homes = playerHomes.get(playerId);
        return homes != null && !homes.isEmpty();
    }
    
    /**
     * Get home count for player
     */
    public int getHomeCount(ServerPlayer player) {
        Map<String, TeleportLocation> homes = getPlayerHomes(player);
        return homes.size();
    }
    
    /**
     * Check if home name is valid
     */
    private boolean isValidHomeName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        
        // Check length
        if (name.length() > 20) {
            return false;
        }
        
        // Check characters (alphanumeric, underscore, dash)
        return name.matches("^[a-zA-Z0-9_-]+$");
    }
    
    /**
     * Check if location is in overworld
     */
    private boolean isOverworld(TeleportLocation location) {
        return location.getWorldName().contains("overworld");
    }
    
    /**
     * Load homes from file
     */
    private void loadHomes() {
        try {
            File file = ResourceUtil.getConfigFile(HOMES_FILE);
            if (!file.exists()) {
                LOGGER.info("No homes file found, starting with empty homes");
                return;
            }
            
            String content = java.nio.file.Files.readString(file.toPath());
            if (content.trim().isEmpty()) {
                return;
            }
            
            JsonObject root = JsonParser.parseString(content).getAsJsonObject();
            
            for (String playerId : root.keySet()) {
                try {
                    UUID uuid = UUID.fromString(playerId);
                    JsonObject playerHomesJson = root.getAsJsonObject(playerId);
                    Map<String, TeleportLocation> homes = new HashMap<>();
                    
                    for (String homeName : playerHomesJson.keySet()) {
                        JsonObject homeJson = playerHomesJson.getAsJsonObject(homeName);
                        TeleportLocation location = TeleportLocation.fromJson(homeJson);
                        if (location != null) {
                            homes.put(homeName, location);
                        }
                    }
                    
                    if (!homes.isEmpty()) {
                        playerHomes.put(uuid, homes);
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to load homes for player {}: {}", playerId, e.getMessage());
                }
            }
            
            LOGGER.info("Loaded homes for {} players", playerHomes.size());
            
        } catch (Exception e) {
            LOGGER.error("Failed to load homes from file", e);
        }
    }
    
    /**
     * Save homes to file (atomic operation)
     */
    private void saveHomes() {
        try {
            ResourceUtil.ensureConfigDirectory();
            File file = ResourceUtil.getConfigFile(HOMES_FILE);
            
            // Write to temp file first
            File tempFile = new File(file.getAbsolutePath() + ".tmp");
            
            JsonObject root = new JsonObject();
            
            for (Map.Entry<UUID, Map<String, TeleportLocation>> playerEntry : playerHomes.entrySet()) {
                JsonObject playerHomesJson = new JsonObject();
                
                for (Map.Entry<String, TeleportLocation> homeEntry : playerEntry.getValue().entrySet()) {
                    playerHomesJson.add(homeEntry.getKey(), homeEntry.getValue().toJson());
                }
                
                root.add(playerEntry.getKey().toString(), playerHomesJson);
            }
            
            // Write to temp file
            try (java.io.FileWriter writer = new java.io.FileWriter(tempFile)) {
                gson.toJson(root, writer);
            }
            
            // Atomically move temp file to actual file
            java.nio.file.Files.move(tempFile.toPath(), file.toPath(), 
                java.nio.file.StandardCopyOption.REPLACE_EXISTING, 
                java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            
            LOGGER.debug("Successfully saved homes for {} players", playerHomes.size());
            
        } catch (Exception e) {
            LOGGER.error("Failed to save homes to file", e);
        }
    }
    
    // Configuration getters/setters
    public int getMaxHomesPerPlayer() { return maxHomesPerPlayer; }
    public void setMaxHomesPerPlayer(int max) { this.maxHomesPerPlayer = Math.max(1, max); }
    
    public boolean isAllowOverworldOnly() { return allowOverworldOnly; }
    public void setAllowOverworldOnly(boolean allow) { this.allowOverworldOnly = allow; }
    
    public boolean isRequireSafeLocations() { return requireSafeLocations; }
    public void setRequireSafeLocations(boolean require) { this.requireSafeLocations = require; }
    
    public int getTeleportDelay() { return teleportDelay; }
    public void setTeleportDelay(int delay) { this.teleportDelay = Math.max(0, delay); }
    
    /**
     * Clear all homes (for testing/admin purposes)
     */
    public void clearAllHomes() {
        playerHomes.clear();
        saveHomes();
        LOGGER.info("Cleared all player homes");
    }
    
    /**
     * Get total number of homes across all players
     */
    public int getTotalHomesCount() {
        return playerHomes.values().stream().mapToInt(Map::size).sum();
    }
    
    /**
     * Get homes statistics
     */
    public String getStatistics() {
        int totalPlayers = playerHomes.size();
        int totalHomes = getTotalHomesCount();
        double avgHomesPerPlayer = totalPlayers > 0 ? (double) totalHomes / totalPlayers : 0;
        
        return String.format("Homes Statistics: %d players, %d total homes, %.1f avg homes per player", 
                           totalPlayers, totalHomes, avgHomesPerPlayer);
    }
}