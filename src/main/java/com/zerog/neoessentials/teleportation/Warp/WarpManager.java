package com.zerog.neoessentials.teleportation.Warp;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zerog.neoessentials.teleportation.TeleportLocation;
import com.zerog.neoessentials.teleportation.TeleportUtil;
import com.zerog.neoessentials.util.ResourceUtil;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages public warp points accessible to all players
 */
public class WarpManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(WarpManager.class);
    private static final String WARPS_FILE = "warps.json";
    
    // Singleton pattern
    private static class SingletonHolder {
        private static final WarpManager INSTANCE = new WarpManager();
    }
    
    public static WarpManager getInstance() {
        return SingletonHolder.INSTANCE;
    }
    
    private final Map<String, TeleportLocation> warps = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();
    
    // Configuration
    private int teleportDelay = 0; // Instant for warps by default
    private boolean requireSafeLocations = true;
    private boolean allowOverworldOnly = false;
    private int maxWarps = 50;
    private boolean caseSensitiveNames = false;
    
    private WarpManager() {
        loadWarps();
    }
    
    /**
     * Create a new warp
     */
    public boolean createWarp(ServerPlayer creator, String warpName, TeleportLocation location) {
        // Normalize warp name
        String normalizedName = caseSensitiveNames ? warpName : warpName.toLowerCase();
        
        // Validate warp name
        if (!isValidWarpName(warpName)) {
            creator.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.warp.invalid_name", warpName));
            return false;
        }
        
        // Check if warp already exists
        if (warps.containsKey(normalizedName)) {
            creator.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.warp.already_exists", warpName));
            return false;
        }
        
        // Check warp limit
        if (warps.size() >= maxWarps) {
            creator.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.warp.limit_reached", maxWarps));
            return false;
        }
        
        // Check world restriction
        if (allowOverworldOnly && !isOverworld(location)) {
            creator.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.warp.overworld_only"));
            return false;
        }
        
        // Check if location is safe
        if (requireSafeLocations && !location.isSafe()) {
            TeleportLocation safeLocation = location.findSafeLocation();
            if (safeLocation == null) {
                creator.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.warp.unsafe_location"));
                return false;
            }
            location = safeLocation;
            creator.sendSystemMessage(MessageUtil.warning("commands.neoessentials.teleport.warp.moved_to_safety"));
        }
        
        // Create the warp
        warps.put(normalizedName, location);
        saveWarps();
        
        creator.sendSystemMessage(MessageUtil.success("commands.neoessentials.teleport.warp.created", warpName, location.getLocationString()));
        LOGGER.info("Player {} created warp '{}' at {}", creator.getName().getString(), warpName, location.getLocationString());
        
        return true;
    }
    
    /**
     * Create warp at player's current location
     */
    public boolean createWarp(ServerPlayer creator, String warpName) {
        TeleportLocation location = new TeleportLocation(creator);
        return createWarp(creator, warpName, location);
    }
    
    /**
     * Create warp at specific coordinates
     */
    public boolean createWarp(ServerPlayer creator, String warpName, ServerLevel level, BlockPos pos) {
        TeleportLocation location = new TeleportLocation(level, pos, 0.0f, 0.0f, creator.getName().getString());
        return createWarp(creator, warpName, location);
    }
    
    /**
     * Delete a warp
     */
    public boolean deleteWarp(ServerPlayer deleter, String warpName) {
        String normalizedName = caseSensitiveNames ? warpName : warpName.toLowerCase();
        
        if (!warps.containsKey(normalizedName)) {
            deleter.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.warp.not_found", warpName));
            return false;
        }
        
        warps.remove(normalizedName);
        saveWarps();
        
        deleter.sendSystemMessage(MessageUtil.success("commands.neoessentials.teleport.warp.deleted", warpName));
        LOGGER.info("Player {} deleted warp '{}'", deleter.getName().getString(), warpName);
        
        return true;
    }
    
    /**
     * Get a specific warp
     */
    public TeleportLocation getWarp(String warpName) {
        String normalizedName = caseSensitiveNames ? warpName : warpName.toLowerCase();
        return warps.get(normalizedName);
    }
    
    /**
     * Get all warp names
     */
    public List<String> getWarpNames() {
        return new ArrayList<>(warps.keySet());
    }
    
    /**
     * Get all warps
     */
    public Map<String, TeleportLocation> getAllWarps() {
        return new HashMap<>(warps);
    }
    
    /**
     * Check if warp exists
     */
    public boolean hasWarp(String warpName) {
        String normalizedName = caseSensitiveNames ? warpName : warpName.toLowerCase();
        return warps.containsKey(normalizedName);
    }
    
    /**
     * Teleport player to warp
     */
    public void teleportToWarp(ServerPlayer player, String warpName) {
        TeleportLocation warp = getWarp(warpName);
        
        if (warp == null) {
            player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.warp.not_found", warpName));
            return;
        }
        
        // Check if warp location is still safe
        if (requireSafeLocations && !warp.isSafe()) {
            TeleportLocation safeLocation = warp.findSafeLocation();
            if (safeLocation == null) {
                player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.warp.unsafe", warpName));
                return;
            }
            
            // Update warp to safe location
            String normalizedName = caseSensitiveNames ? warpName : warpName.toLowerCase();
            warps.put(normalizedName, safeLocation);
            saveWarps();
            warp = safeLocation;
            
            player.sendSystemMessage(MessageUtil.warning("commands.neoessentials.teleport.warp.moved_to_safety", warpName));
        }
        
        // Perform teleportation
        int delayTicks = teleportDelay * 20; // Convert seconds to ticks
        TeleportUtil.teleportPlayer(player, warp, delayTicks, true).thenAccept(result -> {
            if (result.isSuccess()) {
                player.sendSystemMessage(MessageUtil.success("commands.neoessentials.teleport.warp.success", warpName));
                LOGGER.info("Player {} teleported to warp '{}'", player.getName().getString(), warpName);
            } else {
                player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.warp.failed", warpName, result.getMessage()));
                LOGGER.warn("Failed to teleport player {} to warp '{}': {}", 
                          player.getName().getString(), warpName, result.getMessage());
            }
        });
    }
    
    /**
     * Get formatted list of warps for display
     */
    public String getFormattedWarpsList() {
        if (warps.isEmpty()) {
            return MessageUtil.localize("commands.neoessentials.teleport.warp.list_empty");
        }
        
        StringBuilder builder = new StringBuilder();
        // Properly format the header with arguments
        builder.append(MessageUtil.localize("commands.neoessentials.teleport.warp.list_header", warps.size(), maxWarps));
        
        List<String> sortedNames = new ArrayList<>(warps.keySet());
        Collections.sort(sortedNames);
        
        for (String warpName : sortedNames) {
            TeleportLocation location = warps.get(warpName);
            // Use the proper list entry format from the language file
            builder.append("\n").append(MessageUtil.localize("commands.neoessentials.teleport.warp.list_entry", 
                warpName, location.getLocationString(), location.getCreatedBy()));
        }
        
        return builder.toString();
    }
    
    /**
     * Get warp count
     */
    public int getWarpCount() {
        return warps.size();
    }
    
    /**
     * Check if warp name is valid
     */
    private boolean isValidWarpName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        
        // Check length
        if (name.length() > 32) {
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
     * Load warps from file
     */
    private void loadWarps() {
        try {
            File file = ResourceUtil.getConfigFile(WARPS_FILE);
            if (!file.exists()) {
                LOGGER.info("No warps file found, starting with empty warps");
                return;
            }
            
            String content = java.nio.file.Files.readString(file.toPath());
            if (content.trim().isEmpty()) {
                return;
            }
            
            JsonObject root = JsonParser.parseString(content).getAsJsonObject();
            
            // Load warps
            if (root.has("warps")) {
                JsonObject warpsJson = root.getAsJsonObject("warps");
                
                for (String warpName : warpsJson.keySet()) {
                    try {
                        JsonObject warpJson = warpsJson.getAsJsonObject(warpName);
                        TeleportLocation location = TeleportLocation.fromJson(warpJson);
                        if (location != null) {
                            warps.put(warpName, location);
                        }
                    } catch (Exception e) {
                        LOGGER.warn("Failed to load warp '{}': {}", warpName, e.getMessage());
                    }
                }
            }
            
            // Load configuration
            if (root.has("config")) {
                JsonObject config = root.getAsJsonObject("config");
                
                if (config.has("teleportDelay")) {
                    teleportDelay = config.get("teleportDelay").getAsInt();
                }
                if (config.has("requireSafeLocations")) {
                    requireSafeLocations = config.get("requireSafeLocations").getAsBoolean();
                }
                if (config.has("allowOverworldOnly")) {
                    allowOverworldOnly = config.get("allowOverworldOnly").getAsBoolean();
                }
                if (config.has("maxWarps")) {
                    maxWarps = config.get("maxWarps").getAsInt();
                }
                if (config.has("caseSensitiveNames")) {
                    caseSensitiveNames = config.get("caseSensitiveNames").getAsBoolean();
                }
            }
            
            LOGGER.info("Loaded {} warps", warps.size());
            
        } catch (Exception e) {
            LOGGER.error("Failed to load warps from file", e);
        }
    }
    
    /**
     * Save warps to file
     */
    private void saveWarps() {
        try {
            JsonObject root = new JsonObject();
            
            // Save warps
            JsonObject warpsJson = new JsonObject();
            for (Map.Entry<String, TeleportLocation> entry : warps.entrySet()) {
                warpsJson.add(entry.getKey(), entry.getValue().toJson());
            }
            root.add("warps", warpsJson);
            
            // Save configuration
            JsonObject config = new JsonObject();
            config.addProperty("teleportDelay", teleportDelay);
            config.addProperty("requireSafeLocations", requireSafeLocations);
            config.addProperty("allowOverworldOnly", allowOverworldOnly);
            config.addProperty("maxWarps", maxWarps);
            config.addProperty("caseSensitiveNames", caseSensitiveNames);
            root.add("config", config);
            
            ResourceUtil.ensureConfigDirectory();
            File file = ResourceUtil.getConfigFile(WARPS_FILE);
            java.nio.file.Files.writeString(file.toPath(), gson.toJson(root));
            
        } catch (Exception e) {
            LOGGER.error("Failed to save warps to file", e);
        }
    }
    
    // Configuration getters/setters
    public int getTeleportDelay() { return teleportDelay; }
    public void setTeleportDelay(int delay) { this.teleportDelay = Math.max(0, delay); }
    
    public boolean isRequireSafeLocations() { return requireSafeLocations; }
    public void setRequireSafeLocations(boolean require) { this.requireSafeLocations = require; }
    
    public boolean isAllowOverworldOnly() { return allowOverworldOnly; }
    public void setAllowOverworldOnly(boolean allow) { this.allowOverworldOnly = allow; }
    
    public int getMaxWarps() { return maxWarps; }
    public void setMaxWarps(int max) { this.maxWarps = Math.max(1, max); }
    
    public boolean isCaseSensitiveNames() { return caseSensitiveNames; }
    public void setCaseSensitiveNames(boolean caseSensitive) { this.caseSensitiveNames = caseSensitive; }
    
    /**
     * Clear all warps (for admin purposes)
     */
    public void clearAllWarps() {
        warps.clear();
        saveWarps();
        LOGGER.info("Cleared all warps");
    }
    
    /**
     * Get warp statistics
     */
    public String getStatistics() {
        return MessageUtil.localize("commands.neoessentials.teleport.warp.list_statistics", 
                                   warps.size(), maxWarps, (warps.size() * 100.0 / maxWarps));
    }
}