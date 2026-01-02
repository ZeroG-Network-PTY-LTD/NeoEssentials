package com.zerog.neoessentials.teleportation.Spawn;

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

/**
 * Manages server spawn location with setting and teleportation functionality
 */
public class SpawnManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpawnManager.class);
    private static final String SPAWN_FILE = "spawn.json";
    
    // Singleton pattern
    private static class SingletonHolder {
        private static final SpawnManager INSTANCE = new SpawnManager();
    }
    
    public static SpawnManager getInstance() {
        return SingletonHolder.INSTANCE;
    }
    
    private TeleportLocation spawnLocation;
    
    // Configuration
    private int teleportDelay = 0; // Instant for spawn by default
    private boolean requireSafeLocation = true;
    private boolean allowSetSpawnInNether = false;
    private boolean allowSetSpawnInEnd = false;

    private SpawnManager() {
        // Load config values
        try {
            com.zerog.neoessentials.config.ConfigManager configManager = com.zerog.neoessentials.config.ConfigManager.getInstance();
            boolean safe = true;
            if (configManager != null) {
                JsonObject config = configManager.getConfig(com.zerog.neoessentials.config.ConfigManager.MAIN_CONFIG);
                if (config.has("teleportation")) {
                    JsonObject tp = config.getAsJsonObject("teleportation");
                    if (tp.has("spawnSettings")) {
                        JsonObject spawnSettings = tp.getAsJsonObject("spawnSettings");
                        if (spawnSettings.has("enableSpawnSafety")) {
                            safe = spawnSettings.get("enableSpawnSafety").getAsBoolean();
                        }
                    }
                }
            }
            this.requireSafeLocation = safe;
        } catch (Exception e) {
            LOGGER.warn("Failed to load spawn safety config, defaulting to safe: {}", e.getMessage());
        }
        loadSpawn();
    }
    
    /**
     * Set the server spawn location
     */
    public boolean setSpawn(ServerPlayer setter, TeleportLocation location) {
        if (location == null) {
            setter.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.spawn.invalid_location"));
            return false;
        }
        
        // Check world restrictions
        String worldName = location.getWorldName();
        if (!allowSetSpawnInNether && worldName.contains("nether")) {
            setter.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.spawn.no_nether"));
            return false;
        }
        
        if (!allowSetSpawnInEnd && worldName.contains("end")) {
            setter.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.spawn.no_end"));
            return false;
        }
        
        // Check if location is safe
        if (requireSafeLocation && !location.isSafe()) {
            TeleportLocation safeLocation = location.findSafeLocation();
            if (safeLocation == null) {
                setter.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.spawn.unsafe_location"));
                return false;
            }
            location = safeLocation;
            setter.sendSystemMessage(MessageUtil.warning("commands.neoessentials.teleport.spawn.moved_to_safety"));
        }
        
        // Set spawn location
        this.spawnLocation = location;
        saveSpawn();
        
        setter.sendSystemMessage(MessageUtil.success("commands.neoessentials.teleport.spawn.set", location.getLocationString()));
        if (com.zerog.neoessentials.config.ConfigManager.getInstance().isLogSpawnActionsEnabled()) {
            LOGGER.info("Player {} set server spawn to {}", setter.getName().getString(), location.getLocationString());
        }
        
        return true;
    }
    
    /**
     * Set spawn at player's current location
     */
    public boolean setSpawn(ServerPlayer setter) {
        TeleportLocation location = new TeleportLocation(setter);
        return setSpawn(setter, location);
    }
    
    /**
     * Set spawn at specific coordinates
     */
    public boolean setSpawn(ServerPlayer setter, ServerLevel level, BlockPos pos) {
        TeleportLocation location = new TeleportLocation(level, pos, 0.0f, 0.0f, setter.getName().getString());
        return setSpawn(setter, location);
    }
    
    /**
     * Get the current spawn location
     */
    public TeleportLocation getSpawn() {
        return spawnLocation;
    }
    
    /**
     * Check if spawn is set
     */
    public boolean hasSpawn() {
        return spawnLocation != null;
    }
    
    /**
     * Teleport player to spawn
     */
    public void teleportToSpawn(ServerPlayer player) {
        if (spawnLocation == null) {
            // Fallback to world spawn
            teleportToWorldSpawn(player);
            return;
        }

        // Enforce maxTeleportDistance if set in config
        int maxDistance = com.zerog.neoessentials.config.ConfigManager.getInstance().getMaxTeleportDistance();
        if (maxDistance > 0 && spawnLocation != null) {
            com.zerog.neoessentials.teleportation.TeleportLocation fromLoc = new com.zerog.neoessentials.teleportation.TeleportLocation(player);
            if (fromLoc.getWorldName().equals(spawnLocation.getWorldName())) {
                double dist = fromLoc.distanceTo(spawnLocation);
                if (dist > maxDistance) {
                    player.sendSystemMessage(com.zerog.neoessentials.util.MessageUtil.error("commands.neoessentials.teleport.spawn.distance_exceeded", maxDistance));
                    return;
                }
            }
        }
        
        // Check if spawn location is still safe
        if (requireSafeLocation && !spawnLocation.isSafe()) {
            TeleportLocation safeLocation = spawnLocation.findSafeLocation();
            if (safeLocation == null) {
                player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.spawn.unsafe"));
                teleportToWorldSpawn(player);
                return;
            }
            
            // Update spawn to safe location
            spawnLocation = safeLocation;
            saveSpawn();
            player.sendSystemMessage(MessageUtil.warning("commands.neoessentials.teleport.spawn.moved_to_safety"));
        }
        
        // Perform teleportation
        int delayTicks = teleportDelay * 20; // Convert seconds to ticks
        TeleportUtil.teleportPlayer(player, spawnLocation, delayTicks, requireSafeLocation).thenAccept(result -> {
            if (result.isSuccess()) {
                player.sendSystemMessage(MessageUtil.success("commands.neoessentials.teleport.spawn.success"));
                if (com.zerog.neoessentials.config.ConfigManager.getInstance().isLogSpawnActionsEnabled()) {
                    LOGGER.info("Player {} teleported to spawn", player.getName().getString());
                }
            } else {
                player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.spawn.failed", result.getMessage()));
                LOGGER.warn("Failed to teleport player {} to spawn: {}", player.getName().getString(), result.getMessage());
                // Fallback to world spawn
                teleportToWorldSpawn(player);
            }
        });
    }
    
    /**
     * Teleport to vanilla world spawn as fallback
     */
    private void teleportToWorldSpawn(ServerPlayer player) {
        try {
            ServerLevel overworld = player.getServer().overworld();
            BlockPos worldSpawn = overworld.getSharedSpawnPos();
            TeleportLocation fallbackLocation = new TeleportLocation(
                overworld, 
                worldSpawn, 
                0.0f, 
                0.0f, 
                "world"
            );
            
            TeleportUtil.teleportPlayer(player, fallbackLocation, 0, true).thenAccept(result -> {
                if (result.isSuccess()) {
                    player.sendSystemMessage(MessageUtil.info("commands.neoessentials.teleport.spawn.fallback_success"));
                    if (com.zerog.neoessentials.config.ConfigManager.getInstance().isLogSpawnActionsEnabled()) {
                        LOGGER.info("Player {} teleported to world spawn fallback", player.getName().getString());
                    }
                } else {
                    player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.spawn.fallback_failed", result.getMessage()));
                    LOGGER.error("Failed to teleport player {} to world spawn fallback: {}", 
                               player.getName().getString(), result.getMessage());
                }
            });
        } catch (Exception e) {
            player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.spawn.critical_failure"));
            LOGGER.error("Critical failure in spawn teleportation for player {}: {}", 
                        player.getName().getString(), e.getMessage(), e);
        }
    }
    
    /**
     * Get spawn information string
     */
    public String getSpawnInfo() {
        if (spawnLocation == null) {
            return MessageUtil.localize("commands.neoessentials.teleport.spawn.info_not_set");
        }
        
        return MessageUtil.localize("commands.neoessentials.teleport.spawn.info", 
                                  spawnLocation.getLocationString(),
                                  spawnLocation.getCreatedBy());
    }
    
    /**
     * Clear spawn (reset to world spawn)
     */
    public boolean clearSpawn(ServerPlayer clearer) {
        spawnLocation = null;
        saveSpawn();
        
        clearer.sendSystemMessage(MessageUtil.success("commands.neoessentials.teleport.spawn.cleared"));
        if (com.zerog.neoessentials.config.ConfigManager.getInstance().isLogSpawnActionsEnabled()) {
            LOGGER.info("Player {} cleared server spawn", clearer.getName().getString());
        }
        
        return true;
    }
    
    /**
     * Load spawn from file
     */
    private void loadSpawn() {
        try {
            File file = ResourceUtil.getConfigFile(SPAWN_FILE);
            if (!file.exists()) {
                LOGGER.info("No spawn file found, using world spawn");
                return;
            }
            
            String content = java.nio.file.Files.readString(file.toPath());
            if (content.trim().isEmpty()) {
                return;
            }
            
            JsonObject root = JsonParser.parseString(content).getAsJsonObject();
            
            if (root.has("spawn")) {
                JsonObject spawnJson = root.getAsJsonObject("spawn");
                spawnLocation = TeleportLocation.fromJson(spawnJson);
                
                if (spawnLocation != null) {
                    LOGGER.info("Loaded spawn location: {}", spawnLocation.getLocationString());
                } else {
                    LOGGER.warn("Failed to parse spawn location from file");
                }
            }
            
            // Load configuration
            if (root.has("config")) {
                JsonObject config = root.getAsJsonObject("config");
                
                if (config.has("teleportDelay")) {
                    teleportDelay = config.get("teleportDelay").getAsInt();
                }
                if (config.has("requireSafeLocation")) {
                    requireSafeLocation = config.get("requireSafeLocation").getAsBoolean();
                }
                if (config.has("allowSetSpawnInNether")) {
                    allowSetSpawnInNether = config.get("allowSetSpawnInNether").getAsBoolean();
                }
                if (config.has("allowSetSpawnInEnd")) {
                    allowSetSpawnInEnd = config.get("allowSetSpawnInEnd").getAsBoolean();
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to load spawn from file", e);
        }
    }
    
    /**
     * Save spawn to file
     */
    private void saveSpawn() {
        try {
            JsonObject root = new JsonObject();
            
            // Save spawn location
            if (spawnLocation != null) {
                root.add("spawn", spawnLocation.toJson());
            }
            
            // Save configuration
            JsonObject config = new JsonObject();
            config.addProperty("teleportDelay", teleportDelay);
            config.addProperty("requireSafeLocation", requireSafeLocation);
            config.addProperty("allowSetSpawnInNether", allowSetSpawnInNether);
            config.addProperty("allowSetSpawnInEnd", allowSetSpawnInEnd);
            root.add("config", config);
            
            ResourceUtil.ensureConfigDirectory();
            File file = ResourceUtil.getConfigFile(SPAWN_FILE);
            java.nio.file.Files.writeString(file.toPath(), 
                new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(root));
            
        } catch (Exception e) {
            LOGGER.error("Failed to save spawn to file", e);
        }
    }
    
    // Configuration getters/setters
    public int getTeleportDelay() { return teleportDelay; }
    public void setTeleportDelay(int delay) { this.teleportDelay = Math.max(0, delay); }
    
    public boolean isRequireSafeLocation() { return requireSafeLocation; }
    public void setRequireSafeLocation(boolean require) { this.requireSafeLocation = require; }
    
    public boolean isAllowSetSpawnInNether() { return allowSetSpawnInNether; }
    public void setAllowSetSpawnInNether(boolean allow) { this.allowSetSpawnInNether = allow; }
    
    public boolean isAllowSetSpawnInEnd() { return allowSetSpawnInEnd; }
    public void setAllowSetSpawnInEnd(boolean allow) { this.allowSetSpawnInEnd = allow; }
    
    /**
     * Get spawn statistics
     */
    public String getStatistics() {
        return String.format("Spawn Statistics: %s, Safe location required: %s, Teleport delay: %ds", 
                           hasSpawn() ? "Set at " + spawnLocation.getLocationString() : "Not set",
                           requireSafeLocation,
                           teleportDelay);
    }

    /**
     * Reload spawn data from disk
     */
    public void reload() {
        LOGGER.info("Reloading spawn system...");
        spawnLocation = null;
        loadSpawn();
        LOGGER.info("Spawn system reloaded: {}", hasSpawn() ? "Spawn loaded" : "No spawn set");
    }
}