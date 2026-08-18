package com.zerog.neoessentials.teleportation.Spawn;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import com.zerog.neoessentials.teleportation.TeleportLocation;
import com.zerog.neoessentials.teleportation.TeleportUtil;
import com.zerog.neoessentials.util.ResourceUtil;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages server spawn location with setting and teleportation functionality
 */
@SuppressWarnings("unused") // Public API class
public class SpawnManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpawnManager.class);
    private static final String SPAWN_FILE = "spawn.json";
    private static final String SPAWN_COLLECTION = "spawn";
    private static final String SPAWN_ID = "global";
    private final com.zerog.neoessentials.storage.DataStore store =
        com.zerog.neoessentials.storage.StorageManager.getInstance().getStore();

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
    private int spawnCooldownSeconds = 0;

    // Cooldown tracking: player UUID -> last spawn teleport time (ms)
    private final Map<UUID, Long> lastSpawnTimestamps = new ConcurrentHashMap<>();

    private SpawnManager() {
        loadConfig();
        migrateLegacyFilesIfNeeded();
        loadSpawn();
    }

    /**
     * Load configuration values from config file
     */
    private void loadConfig() {
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
                        if (spawnSettings.has("spawnCooldown")) {
                            try {
                                spawnCooldownSeconds = spawnSettings.get("spawnCooldown").getAsInt();
                            } catch (Exception e) {
                                NeoLog.debug(LOGGER, LogCategory.TELEPORTATION,
                                    "Failed to parse spawnSettings.spawnCooldown, using default", e);
                            }
                        }
                    }
                    // Read teleport delay (warmup) from generalSettings
                    if (tp.has("generalSettings")) {
                        JsonObject generalSettings = tp.getAsJsonObject("generalSettings");
                        if (generalSettings.has("teleportDelay")) {
                            try {
                                teleportDelay = generalSettings.get("teleportDelay").getAsInt();
                            } catch (Exception e) {
                                NeoLog.debug(LOGGER, LogCategory.TELEPORTATION,
                                    "Failed to parse generalSettings.teleportDelay, using default", e);
                            }
                        }
                    }
                }
            }
            this.requireSafeLocation = safe;
            NeoLog.info(LOGGER, LogCategory.TELEPORTATION, "[SpawnManager] Config loaded — safetyCheck={}, warmup={}s, cooldown={}s, allowNether={}, allowEnd={}",
                safe, teleportDelay, spawnCooldownSeconds, allowSetSpawnInNether, allowSetSpawnInEnd);
        } catch (Exception e) {
            LOGGER.warn("Failed to load spawn safety config, defaulting to safe: {}", e.getMessage());
        }
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
        
        // Check if location is safe (only enforce if safety is required)
        if (requireSafeLocation) {
            if (!location.isSafe()) {
                TeleportLocation safeLocation = location.findSafeLocation();
                if (safeLocation == null) {
                    setter.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.spawn.unsafe_location"));
                    return false;
                }
                location = safeLocation;
                setter.sendSystemMessage(MessageUtil.warning("commands.neoessentials.teleport.spawn.moved_to_safety"));
            }
        }
        // If safety is not required, allow spawn at unsafe locations

        // Set spawn location
        this.spawnLocation = location;
        saveSpawn();
        
        setter.sendSystemMessage(MessageUtil.success("commands.neoessentials.teleport.spawn.set", location.getLocationString()));
        if (com.zerog.neoessentials.config.ConfigManager.getInstance().isLogSpawnActionsEnabled()) {
            NeoLog.info(LOGGER, LogCategory.TELEPORTATION, "Player {} set server spawn to {}", setter.getName().getString(), location.getLocationString());
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
        NeoLog.debug(LOGGER, LogCategory.TELEPORTATION, "teleportToSpawn request: player={}", player.getName().getString());

        // Enforce spawn cooldown - atomic check (skip if player has bypass permission)
        boolean bypassCooldown = com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(player.getUUID(), "neoessentials.teleport.bypass.cooldown")
            || com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(player.getUUID(), "neoessentials.teleport.spawn.bypass.cooldown");
        if (spawnCooldownSeconds > 0 && !bypassCooldown) {
            long now = System.currentTimeMillis();
            UUID playerId = player.getUUID();
            Long lastUse = lastSpawnTimestamps.putIfAbsent(playerId, now);
            if (lastUse != null) {
                long elapsed = (now - lastUse) / 1000L;
                if (elapsed < spawnCooldownSeconds) {
                    long wait = spawnCooldownSeconds - elapsed;
                    NeoLog.debug(LOGGER, LogCategory.TELEPORTATION, "teleportToSpawn: {} blocked by cooldown, {}s remaining", player.getName().getString(), wait);
                    player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.spawn.cooldown", wait));
                    return;
                }
                lastSpawnTimestamps.put(playerId, now);
            }
        }

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
        
        // Always read safety setting at runtime from config (not the cached field) so that
        // config reloads are respected — mirroring the pattern used in HomeManager.
        boolean requireSafe = com.zerog.neoessentials.config.ConfigManager.getInstance().isSpawnSafetyEnabled();

        // Force-load the target chunk AND its 8 neighbours BEFORE any safety check.
        // isSafe() / findSafeLocation() both return false/null when the chunk is not yet
        // loaded, which previously caused a spurious fallback to world-spawn even though
        // the configured spawn location was perfectly fine.
        net.minecraft.server.level.ServerLevel spawnLevel = spawnLocation.getLevel();
        if (spawnLevel != null) {
            net.minecraft.core.BlockPos spawnBlockPos = new net.minecraft.core.BlockPos(
                (int) spawnLocation.getX(), (int) spawnLocation.getY(), (int) spawnLocation.getZ());
            NeoLog.debug(LOGGER, LogCategory.TELEPORTATION, "Pre-loading 3x3 chunk grid around ({},{}) for spawn teleport.",
                spawnBlockPos.getX() >> 4, spawnBlockPos.getZ() >> 4);
            TeleportUtil.preloadChunksForTeleport(spawnLevel, spawnBlockPos);
        }

        // Check if spawn location is still safe (chunks are now loaded, so isSafe() is accurate)
        if (requireSafe) {
            if (!spawnLocation.isSafe()) {
                TeleportLocation safeLocation = spawnLocation.findSafeLocation();
                if (safeLocation == null) {
                    player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.spawn.unsafe"));
                    NeoLog.debug(LOGGER, LogCategory.TELEPORTATION, "No safe location found near spawn; triggering world-spawn fallback.");
                    teleportToWorldSpawn(player);
                    return;
                }

                // Update spawn to safe location
                spawnLocation = safeLocation;
                saveSpawn();
                player.sendSystemMessage(MessageUtil.warning("commands.neoessentials.teleport.spawn.moved_to_safety"));
                NeoLog.debug(LOGGER, LogCategory.TELEPORTATION, "Spawn moved to safe location.");
            }
        } else {
            // Safety checks disabled — teleport directly to configured location
            NeoLog.debug(LOGGER, LogCategory.TELEPORTATION, "Spawn teleport safety is disabled. Teleporting to configured location.");
        }

        // Save current location for /back command
        com.zerog.neoessentials.teleportation.Misc.MiscTeleportManager.getInstance().saveBackLocation(player);

        // Show warmup countdown message if delay is configured and warmup messages are enabled
        // Players with warmup bypass permission teleport instantly
        boolean bypassWarmup = com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(player.getUUID(), "neoessentials.teleport.bypass.warmup")
            || com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(player.getUUID(), "neoessentials.teleport.spawn.bypass.warmup");
        int delayTicks = bypassWarmup ? 0 : teleportDelay * 20; // Convert seconds to ticks
        if (delayTicks > 0) {
            boolean showWarmup = true;
            try {
                JsonObject generalSettings = com.zerog.neoessentials.config.ConfigManager.getInstance()
                    .getConfig(com.zerog.neoessentials.config.ConfigManager.MAIN_CONFIG)
                    .getAsJsonObject("teleportation").getAsJsonObject("generalSettings");
                if (generalSettings.has("enableTeleportWarmup")) {
                    showWarmup = generalSettings.get("enableTeleportWarmup").getAsBoolean();
                }
            } catch (Exception e) {
                NeoLog.debug(LOGGER, LogCategory.TELEPORTATION,
                    "Failed to read generalSettings.enableTeleportWarmup, defaulting to shown", e);
            }
            if (showWarmup) {
                player.sendSystemMessage(MessageUtil.info("commands.neoessentials.teleport.spawn.warmup", teleportDelay));
            }
        }

        // Perform teleportation — safety already resolved above, so pass findSafe=false
        // (matching the pattern in HomeManager.teleportToHome)
        TeleportUtil.teleportPlayer(player, spawnLocation, delayTicks, false).thenAccept(result -> {
            if (result.isSuccess()) {
                player.sendSystemMessage(MessageUtil.success("commands.neoessentials.teleport.spawn.success"));
                NeoLog.debug(LOGGER, LogCategory.TELEPORTATION, "Player {} successfully teleported to spawn at {}",
                    player.getName().getString(), spawnLocation.getLocationString());
                if (com.zerog.neoessentials.config.ConfigManager.getInstance().isLogSpawnActionsEnabled()) {
                    NeoLog.info(LOGGER, LogCategory.TELEPORTATION, "Player {} teleported to spawn", player.getName().getString());
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
            MinecraftServer server = player.getServer();
            if (server == null) {
                LOGGER.error("Cannot teleport to world spawn - server is null");
                return;
            }
            ServerLevel overworld = server.overworld();
            BlockPos worldSpawn = overworld.getSharedSpawnPos();
            TeleportLocation fallbackLocation = new TeleportLocation(
                overworld, 
                worldSpawn, 
                0.0f, 
                0.0f, 
                "world"
            );
            
            TeleportUtil.teleportPlayer(player, fallbackLocation, 0, requireSafeLocation).thenAccept(result -> {
                if (result.isSuccess()) {
                    player.sendSystemMessage(MessageUtil.info("commands.neoessentials.teleport.spawn.fallback_success"));
                    if (com.zerog.neoessentials.config.ConfigManager.getInstance().isLogSpawnActionsEnabled()) {
                        NeoLog.info(LOGGER, LogCategory.TELEPORTATION, "Player {} teleported to world spawn fallback", player.getName().getString());
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
            NeoLog.info(LOGGER, LogCategory.TELEPORTATION, "Player {} cleared server spawn", clearer.getName().getString());
        }
        
        return true;
    }
    
    /**
     * Load spawn from the active {@link com.zerog.neoessentials.storage.DataStore}.
     */
    private void loadSpawn() {
        try {
            JsonObject root = store.get(SPAWN_COLLECTION, SPAWN_ID);
            if (root == null) {
                NeoLog.info(LOGGER, LogCategory.TELEPORTATION, "No spawn record found, using world spawn");
                return;
            }

            if (root.has("spawn")) {
                JsonObject spawnJson = root.getAsJsonObject("spawn");
                spawnLocation = TeleportLocation.fromJson(spawnJson);

                if (spawnLocation != null) {
                    NeoLog.info(LOGGER, LogCategory.TELEPORTATION, "Loaded spawn location: {}", spawnLocation.getLocationString());
                } else {
                    LOGGER.warn("Failed to parse spawn location from storage");
                }
            }

            // Load legacy configuration fields from the spawn record.
            // NOTE: requireSafeLocation is intentionally NOT read here; it is read at
            // runtime from config.json by isSpawnSafetyEnabled(). Reading it from
            // the spawn record would silently override any change made to
            // enableSpawnSafety in config.json after the initial save.
            if (root.has("config")) {
                JsonObject config = root.getAsJsonObject("config");
                // teleportDelay is now read from config.json (generalSettings.teleportDelay), not here
                if (config.has("allowSetSpawnInNether")) {
                    allowSetSpawnInNether = config.get("allowSetSpawnInNether").getAsBoolean();
                }
                if (config.has("allowSetSpawnInEnd")) {
                    allowSetSpawnInEnd = config.get("allowSetSpawnInEnd").getAsBoolean();
                }
            }

        } catch (Exception e) {
            LOGGER.error("Failed to load spawn from storage", e);
        }
    }

    /**
     * Save spawn to the active DataStore as a single document (collection "spawn",
     * id "global").
     */
    private void saveSpawn() {
        try {
            JsonObject root = new JsonObject();

            if (spawnLocation != null) {
                root.add("spawn", spawnLocation.toJson());
            }

            JsonObject config = new JsonObject();
            config.addProperty("teleportDelay", teleportDelay);
            config.addProperty("requireSafeLocation", requireSafeLocation);
            config.addProperty("allowSetSpawnInNether", allowSetSpawnInNether);
            config.addProperty("allowSetSpawnInEnd", allowSetSpawnInEnd);
            root.add("config", config);

            store.put(SPAWN_COLLECTION, SPAWN_ID, root);

        } catch (Exception e) {
            LOGGER.error("Failed to save spawn to storage", e);
        }
    }

    /**
     * One-time import of the legacy spawn.json file into the active DataStore, if it's
     * still empty and storage.autoMigrate is enabled.
     */
    private void migrateLegacyFilesIfNeeded() {
        if (store.hasAnyData(SPAWN_COLLECTION)) return;
        if (!com.zerog.neoessentials.config.ConfigManager.getInstance().isStorageAutoMigrateEnabled()) return;

        try {
            File file = ResourceUtil.getDataFile(SPAWN_FILE);
            if (!file.exists()) return;

            String content = java.nio.file.Files.readString(file.toPath());
            if (content.trim().isEmpty()) return;

            JsonObject root = JsonParser.parseString(content).getAsJsonObject();
            store.put(SPAWN_COLLECTION, SPAWN_ID, root);
            NeoLog.info(LOGGER, LogCategory.TELEPORTATION, "SpawnManager: migrated legacy spawn.json into the '{}' storage backend.",
                com.zerog.neoessentials.storage.StorageManager.getInstance().getActiveType());
        } catch (Exception e) {
            LOGGER.error("Failed to migrate legacy spawn.json: {}", e.getMessage());
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
        NeoLog.info(LOGGER, LogCategory.TELEPORTATION, "Reloading spawn system...");
        loadConfig();
        spawnLocation = null;
        loadSpawn();
        NeoLog.info(LOGGER, LogCategory.TELEPORTATION, "Spawn system reloaded: {}", hasSpawn() ? "Spawn loaded" : "No spawn set");
    }
}