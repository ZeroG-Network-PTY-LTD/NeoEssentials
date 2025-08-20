package com.zerog.neoessentials.managers;

import com.zerog.neoessentials.config.ConfigurationUnifier;
import com.zerog.neoessentials.config.SpawnConfig;
import com.zerog.neoessentials.permissions.PermissionNodes;
import com.zerog.neoessentials.storage.PlayerDataManager;
import com.zerog.neoessentials.util.LocationUtil;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Spawn management system for NeoEssentials
 * Handles spawn locations, first spawn, respawn behavior, and spawn protection
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class SpawnManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(SpawnManager.class);
    private static SpawnManager instance;
    
    private final ConfigurationUnifier configUnifier;
    private final PlayerDataManager playerDataManager;
    private final Map<UUID, Long> spawnCooldowns;
    
    private SpawnManager() {
        this.configUnifier = ConfigurationUnifier.getInstance();
        this.playerDataManager = PlayerDataManager.getInstance();
        this.spawnCooldowns = new ConcurrentHashMap<>();
    }
    
    public static SpawnManager getInstance() {
        if (instance == null) {
            instance = new SpawnManager();
        }
        return instance;
    }
    
    /**
     * Set the main spawn location
     */
    public boolean setSpawn(ServerPlayer player, LocationUtil.Location location) {
        SpawnConfig config = configUnifier.getConfigManager().getSpawnConfig();
    boolean spawnModuleEnabled = configUnifier.getConfigManager().getMainConfig().modules.spawn;
        if (!spawnModuleEnabled) {
            MessageUtil.sendMessage(player, "&cSpawn system is disabled.");
            return false;
        }
        
        // Check permission
        if (!PermissionUtil.hasPermission(player, PermissionNodes.SPAWN_SET)) {
            MessageUtil.sendMessage(player, config.messages.noPermission);
            return false;
        }
        
        // Validate location
        if (!isValidSpawnLocation(location)) {
            MessageUtil.sendMessage(player, config.messages.spawnNotFound);
            return false;
        }
        
        // Update main spawn configuration
        config.mainSpawn.world = location.world;
        config.mainSpawn.x = location.x;
        config.mainSpawn.y = location.y;
        config.mainSpawn.z = location.z;
        config.mainSpawn.yaw = location.yaw;
        config.mainSpawn.pitch = location.pitch;
        
        MessageUtil.sendMessage(player, MessageUtil.replacePlaceholders(config.messages.spawnSet,
            location.world, String.valueOf((int) location.x), String.valueOf((int) location.y), String.valueOf((int) location.z)));
        
        LOGGER.info("Spawn location set by {} at {}", player.getName().getString(), 
            formatLocation(location));
        
        return true;
    }
    
    /**
     * Set spawn at player's current location
     */
    public boolean setSpawn(ServerPlayer player) {
        LocationUtil.Location location = new LocationUtil.Location(
            player.serverLevel().dimension().location().toString(),
            player.getX(), player.getY(), player.getZ(),
            player.getYRot(), player.getXRot()
        );
        
        return setSpawn(player, location);
    }
    
    /**
     * Teleport player to spawn
     */
    public boolean teleportToSpawn(ServerPlayer player) {
        SpawnConfig config = configUnifier.getConfigManager().getSpawnConfig();
        
        if (!config.enabled) {
            MessageUtil.sendMessage(player, "&cSpawn system is disabled.");
            return false;
        }
        
        // Check permission
        if (!PermissionUtil.hasPermission(player, PermissionNodes.SPAWN)) {
            MessageUtil.sendMessage(player, config.messages.noPermission);
            return false;
        }
        
        // Get spawn location
        LocationUtil.Location spawnLocation = getSpawnLocation();
        if (spawnLocation == null) {
            MessageUtil.sendMessage(player, config.messages.spawnNotSet);
            return false;
        }
        
        // Perform teleport
        boolean success = teleportToLocation(player, spawnLocation);
        if (success) {
            MessageUtil.sendMessage(player, config.messages.teleportedToSpawn);
            LOGGER.info("Player {} teleported to spawn", player.getName().getString());
        } else {
            MessageUtil.sendMessage(player, config.messages.spawnNotFound);
        }
        
        return success;
    }
    
    /**
     * Get the spawn location for a world
     */
    public LocationUtil.Location getSpawnLocation(String world) {
        SpawnConfig config = configUnifier.getConfigManager().getSpawnConfig();
        
        // Check for world-specific spawn
        SpawnConfig.WorldSpawnConfig.WorldSpawnDefinition worldSpawn = config.getWorldSpawn(world);
        if (worldSpawn != null) {
            return new LocationUtil.Location(
                worldSpawn.worldName,
                worldSpawn.x, worldSpawn.y, worldSpawn.z,
                worldSpawn.yaw, worldSpawn.pitch
            );
        }
        
        // Use main spawn if same world
        if (config.mainSpawn.world != null && config.mainSpawn.world.equals(world)) {
            return new LocationUtil.Location(
                config.mainSpawn.world,
                config.mainSpawn.x, config.mainSpawn.y, config.mainSpawn.z,
                config.mainSpawn.yaw, config.mainSpawn.pitch
            );
        }
        
        // Fall back to world default spawn
        return getWorldDefaultSpawn(world);
    }
    
    /**
     * Get the main spawn location
     */
    public LocationUtil.Location getSpawnLocation() {
        SpawnConfig config = configUnifier.getConfigManager().getSpawnConfig();
        
        if (config.mainSpawn.world != null) {
            return new LocationUtil.Location(
                config.mainSpawn.world,
                config.mainSpawn.x, config.mainSpawn.y, config.mainSpawn.z,
                config.mainSpawn.yaw, config.mainSpawn.pitch
            );
        }
        
        // Fall back to overworld spawn
        return getWorldDefaultSpawn("minecraft:overworld");
    }
    
    /**
     * Handle first join spawn
     */
    public void handleFirstJoin(ServerPlayer player) {
        SpawnConfig config = configUnifier.getConfigManager().getSpawnConfig();
        
        if (!config.enabled || !config.setSpawnOnFirstJoin) {
            return;
        }
        
        // Check if player has joined before
        Object hasJoined = playerDataManager.getSetting(player.getUUID(), "has_joined_before");
        if (Boolean.TRUE.equals(hasJoined) || "true".equals(String.valueOf(hasJoined))) {
            return;
        }
        
        // Get spawn location
        LocationUtil.Location spawnLocation = getSpawnLocation();
        if (spawnLocation != null) {
            teleportToLocation(player, spawnLocation);
            
            if (config.newPlayer.giveWelcomeMessage && !config.messages.welcomeMessage.isEmpty()) {
                MessageUtil.sendMessage(player, MessageUtil.replacePlaceholders(config.messages.welcomeMessage,
                    player.getName().getString()));
            }
            
            LOGGER.info("Player {} teleported to spawn on first join", player.getName().getString());
        }
        
        // Mark as joined
        playerDataManager.setSetting(player.getUUID(), "has_joined_before", true);
    }
    
    /**
     * Handle respawn
     */
    public void handleRespawn(ServerPlayer player) {
        SpawnConfig config = configUnifier.getConfigManager().getSpawnConfig();
        
        if (!config.enabled || !config.setSpawnOnRespawn) {
            return;
        }
        
        // Check respawn settings
        if (config.respawn.respectBedSpawns) {
            BlockPos bedPos = player.getRespawnPosition();
            if (bedPos != null) {
                return; // Let vanilla handle bed respawn
            }
        }
        
        if (config.setSpawnOnDeath) {
            LocationUtil.Location spawnLocation = getSpawnLocation(player.serverLevel().dimension().location().toString());
            if (spawnLocation != null) {
                // Schedule teleport for next tick (respawn mechanics)
                scheduleRespawnTeleport(player, spawnLocation);
            }
        }
    }
    
    /**
     * Check if location is valid for spawn
     */
    private boolean isValidSpawnLocation(LocationUtil.Location location) {
        if (location == null) {
            return false;
        }
        
        // Check if Y coordinate is reasonable
        if (location.y < 0 || location.y > 320) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Teleport player to location
     */
    private boolean teleportToLocation(ServerPlayer player, LocationUtil.Location location) {
        try {
            // Basic teleportation - would need proper implementation
            player.teleportTo(location.x, location.y, location.z);
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to teleport player {} to spawn", player.getName().getString(), e);
            return false;
        }
    }
    
    /**
     * Get world default spawn location
     */
    private LocationUtil.Location getWorldDefaultSpawn(String worldName) {
        // This would need proper world access to get the world spawn
        // For now, return a default location
        return new LocationUtil.Location(worldName, 0, 64, 0, 0, 0);
    }
    
    /**
     * Schedule respawn teleport
     */
    private void scheduleRespawnTeleport(ServerPlayer player, LocationUtil.Location location) {
        // This would need proper scheduling system
        // For now, just log the intent
        LOGGER.debug("Scheduled respawn teleport for player {} to spawn", player.getName().getString());
    }
    
    /**
     * Format location for logging
     */
    private String formatLocation(LocationUtil.Location location) {
        return String.format("%s %.2f %.2f %.2f", 
            location.world, location.x, location.y, location.z);
    }
    
    /**
     * Check spawn protection
     */
    public boolean isInSpawnProtection(LocationUtil.Location location) {
        SpawnConfig config = configUnifier.getConfigManager().getSpawnConfig();
        
        if (!config.enabled || !config.safety.enabled) {
            return false;
        }
        
        LocationUtil.Location spawnLocation = getSpawnLocation(location.world);
        if (spawnLocation == null) {
            return false;
        }
        
        double distance = spawnLocation.distance(location);
        return distance <= config.safety.safetySearchRadius;
    }
    
    /**
     * Can player build in spawn protection
     */
    public boolean canBuildInSpawnProtection(ServerPlayer player, LocationUtil.Location location) {
        if (!isInSpawnProtection(location)) {
            return true;
        }
        
        // Check permission
        return PermissionUtil.hasPermission(player, "neoessentials.spawn.build");
    }
    
    /**
     * Clean up expired cooldowns
     */
    public void cleanup() {
        long currentTime = System.currentTimeMillis();
        
        // Clean up expired spawn cooldowns
        spawnCooldowns.entrySet().removeIf(entry -> {
            long elapsed = currentTime - entry.getValue();
            return elapsed > 24 * 60 * 60 * 1000L; // 24 hours
        });
    }
}
