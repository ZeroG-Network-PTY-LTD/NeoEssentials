package com.zerog.neoessentials.teleportation;

import com.google.gson.JsonObject;
import com.zerog.neoessentials.util.PlayerDataStore;
import com.zerog.neoessentials.util.PlayerDataMigration;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player home locations with creation, deletion, listing, and teleportation.
 *
 * <p>Now uses per-player data storage for better performance and scalability:</p>
 * <pre>
 * config/neoessentials/playerdata/homes/
 * ├── {uuid1}.json  (Player 1's homes)
 * ├── {uuid2}.json  (Player 2's homes)
 * └── {uuid3}.json  (Player 3's homes)
 * </pre>
 */
public class HomeManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(HomeManager.class);
    private static final String HOMES_FILE = "homes.json"; // Legacy file (for migration)

    // Singleton pattern
    private static class SingletonHolder {
        private static final HomeManager INSTANCE = new HomeManager();
    }

    public static HomeManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    // NEW: Per-player data storage
    private final PlayerDataStore playerDataStore;

    // In-memory cache for quick lookups (UUID -> homes map)
    private final Map<UUID, Map<String, TeleportLocation>> playerHomes = new ConcurrentHashMap<>();

    // Configuration
    private int maxHomesPerPlayer = 5;
    private int homeSetCooldownSeconds = 0;
    private int homeTeleportCooldownSeconds = 0;
    private int homeDeleteCooldownSeconds = 0;

    // Cooldown tracking: player UUID -> last setHome time (ms)
    private final Map<UUID, Long> lastHomeSetTimestamps = new ConcurrentHashMap<>();
    // Cooldown tracking: player UUID -> last home teleport time (ms)
    private final Map<UUID, Long> lastHomeTeleportTimestamps = new ConcurrentHashMap<>();
    // Cooldown tracking: player UUID -> last home delete time (ms)
    private final Map<UUID, Long> lastHomeDeleteTimestamps = new ConcurrentHashMap<>();

    /**
     * Returns the maximum number of homes allowed for a player, considering permissions.
     * If the player has the permission node neoessentials.home.<amount>, that value is used if higher than config.
     */
    public int getMaxHomesForPlayer(ServerPlayer player) {
        int configMax = this.maxHomesPerPlayer;
        int permMax = -1;
        // Check for permissions neoessentials.home.<amount> from high to low (e.g., 100 down to 1)
        for (int i = 100; i >= 1; i--) {
            String perm = "neoessentials.home." + i;
            if (com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(player.getUUID(), perm)) {
                permMax = i;
                break;
            }
        }
        // Return the higher value between permission-based and config-based limits
        return Math.max(permMax, configMax);
    }
    private boolean allowOverworldOnly = false;
    private boolean allowCrossDimensionHomes = true;
    private boolean requireSafeLocations = true;
    private int teleportDelay = 3; // seconds

    private HomeManager() {
        // Initialize per-player data store
        this.playerDataStore = new PlayerDataStore("homes");

        // Perform migration from old homes.json if needed
        if (PlayerDataMigration.needsMigration(HOMES_FILE)) {
            LOGGER.info("Migrating homes from old storage format...");
            PlayerDataMigration.migrateToPlayerData(HOMES_FILE, "homes");
        }

        loadConfig();
    }

    /**
     * Load configuration values from config file
     */
    private void loadConfig() {
        try {
            com.zerog.neoessentials.config.ConfigManager configManager = com.zerog.neoessentials.config.ConfigManager.getInstance();
            boolean safe = true;
            int maxHomes = 5;
            int setCooldown = 0;
            int tpCooldown = 0;
            int delCooldown = 0;
            if (configManager != null) {
                JsonObject config = configManager.getConfig(com.zerog.neoessentials.config.ConfigManager.MAIN_CONFIG);
                if (config.has("teleportation")) {
                    JsonObject tp = config.getAsJsonObject("teleportation");
                    if (tp.has("homeSettings")) {
                        JsonObject homeSettings = tp.getAsJsonObject("homeSettings");
                        if (homeSettings.has("enableHomeTeleportSafety")) {
                            safe = homeSettings.get("enableHomeTeleportSafety").getAsBoolean();
                        }
                        if (homeSettings.has("maxHomes")) {
                            try {
                                maxHomes = homeSettings.get("maxHomes").getAsInt();
                            } catch (Exception ignored) {}
                        }
                        if (homeSettings.has("allowCrossDimensionHomes")) {
                            try {
                                allowCrossDimensionHomes = homeSettings.get("allowCrossDimensionHomes").getAsBoolean();
                            } catch (Exception ignored) {}
                        }
                        if (homeSettings.has("homeSetCooldown")) {
                            try {
                                setCooldown = homeSettings.get("homeSetCooldown").getAsInt();
                            } catch (Exception ignored) {}
                        }
                        if (homeSettings.has("homeTeleportCooldown")) {
                            try {
                                tpCooldown = homeSettings.get("homeTeleportCooldown").getAsInt();
                            } catch (Exception ignored) {}
                        }
                        if (homeSettings.has("homeDeleteCooldown")) {
                            try {
                                delCooldown = homeSettings.get("homeDeleteCooldown").getAsInt();
                            } catch (Exception ignored) {}
                        }
                    }
                }
            }
            setRequireSafeLocations(safe);
            setMaxHomesPerPlayer(maxHomes);
            setHomeSetCooldownSeconds(setCooldown);
            setHomeTeleportCooldownSeconds(tpCooldown);
            setHomeDeleteCooldownSeconds(delCooldown);
        } catch (Exception e) {
            LOGGER.warn("Failed to load home config, using defaults: {}", e.getMessage());
        }
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

        // Enforce set home cooldown - atomic check
        if (homeSetCooldownSeconds > 0) {
            long now = System.currentTimeMillis();
            // Use putIfAbsent to atomically check and update cooldown
            Long lastSet = lastHomeSetTimestamps.putIfAbsent(playerId, now);
            if (lastSet != null) {
                long elapsed = (now - lastSet) / 1000L;
                if (elapsed < homeSetCooldownSeconds) {
                    long wait = homeSetCooldownSeconds - elapsed;
                    player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.home.cooldown", wait));
                    return false;
                }
                // Update timestamp atomically
                lastHomeSetTimestamps.put(playerId, now);
            }
        }

        // Validate home name
        if (!isValidHomeName(homeName)) {
            player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.home.invalid_name", homeName));
            return false;
        }

        // Create location
        TeleportLocation location = customLocation != null ? customLocation : new TeleportLocation(player);

        // Check world restriction
        if (!allowCrossDimensionHomes && !isOverworld(location)) {
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

        // ATOMIC: Set the home using computeIfAbsent + compute for atomic limit check
        int allowedHomes = getMaxHomesForPlayer(player);
        final TeleportLocation finalLocation = location;
        
        // Use compute to atomically check limit and add home
        boolean[] result = new boolean[2]; // [0] = success, [1] = isNew
        playerHomes.compute(playerId, (id, homes) -> {
            if (homes == null) {
                homes = new ConcurrentHashMap<>();
            }
            
            // Check limit atomically
            boolean isNew = !homes.containsKey(homeName);
            if (isNew && homes.size() >= allowedHomes) {
                // Limit exceeded - result[0] stays false
                return homes;
            }
            
            // Set the home
            homes.put(homeName, finalLocation);
            result[0] = true; // Success
            result[1] = isNew; // Track if new
            return homes;
        });
        
        if (!result[0]) {
            player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.home.limit_reached", allowedHomes));
            return false;
        }
        
        boolean isNew = result[1];

        // Save to file (per-player storage)
        savePlayerHomes(playerId);

        if (isNew) {
            player.sendSystemMessage(MessageUtil.success("commands.neoessentials.teleport.home.set", homeName, location.getLocationString()));
        } else {
            player.sendSystemMessage(MessageUtil.success("commands.neoessentials.teleport.home.updated", homeName, location.getLocationString()));
        }

        // Log home set/update if enabled in config
        if (com.zerog.neoessentials.config.ConfigManager.getInstance().isLogHomeActionsEnabled()) {
            LOGGER.info("Player {} {} home '{}' at {}", 
                player.getName().getString(), 
                isNew ? "set" : "updated", 
                homeName, 
                location.getLocationString());
        }

        return true;
    }
    
    /**
     * Delete a home for a player
     */
    public boolean deleteHome(ServerPlayer player, String homeName) {
        UUID playerId = player.getUUID();

        // Enforce delete home cooldown - atomic check
        if (homeDeleteCooldownSeconds > 0) {
            long now = System.currentTimeMillis();
            // Use putIfAbsent to atomically check and update cooldown
            Long lastDelete = lastHomeDeleteTimestamps.putIfAbsent(playerId, now);
            if (lastDelete != null) {
                long elapsed = (now - lastDelete) / 1000L;
                if (elapsed < homeDeleteCooldownSeconds) {
                    long wait = homeDeleteCooldownSeconds - elapsed;
                    player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.home.delete_cooldown", wait));
                    return false;
                }
                // Update timestamp atomically
                lastHomeDeleteTimestamps.put(playerId, now);
            }
        }

        // ATOMIC: Delete home using compute
        boolean[] deleted = {false};
        playerHomes.computeIfPresent(playerId, (id, homes) -> {
            if (homes.remove(homeName) != null) {
                deleted[0] = true;
                // Return null if empty to remove entry
                return homes.isEmpty() ? null : homes;
            }
            return homes;
        });

        if (!deleted[0]) {
            player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.home.not_found", homeName));
            return false;
        }

        // Save to file (per-player storage)
        savePlayerHomes(playerId);

        player.sendSystemMessage(MessageUtil.success("commands.neoessentials.teleport.home.deleted", homeName));
        // Log home delete if enabled in config
        if (com.zerog.neoessentials.config.ConfigManager.getInstance().isLogHomeActionsEnabled()) {
            LOGGER.info("Player {} deleted home '{}'", player.getName().getString(), homeName);
        }

        return true;
    }
    public int getHomeDeleteCooldownSeconds() { return homeDeleteCooldownSeconds; }
    public void setHomeDeleteCooldownSeconds(int seconds) { this.homeDeleteCooldownSeconds = Math.max(0, seconds); }
    
    /**
     * Get or load homes for a player (lazy loading from PlayerDataStore)
     */
    private Map<String, TeleportLocation> getOrLoadPlayerHomes(UUID playerId) {
        return playerHomes.computeIfAbsent(playerId, this::loadPlayerHomes);
    }

    /**
     * Get a specific home for a player
     */
    public TeleportLocation getHome(ServerPlayer player, String homeName) {
        UUID playerId = player.getUUID();
        Map<String, TeleportLocation> homes = getOrLoadPlayerHomes(playerId);
        return homes.get(homeName);
    }
    
    /**
     * Get all homes for a player
     */
    public Map<String, TeleportLocation> getPlayerHomes(ServerPlayer player) {
        UUID playerId = player.getUUID();
        Map<String, TeleportLocation> homes = getOrLoadPlayerHomes(playerId);
        return new HashMap<>(homes); // Return copy for thread safety
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
        UUID playerId = player.getUUID();

        // Enforce teleport cooldown - atomic check
        if (homeTeleportCooldownSeconds > 0) {
            long now = System.currentTimeMillis();
            // Use putIfAbsent to atomically check and update cooldown
            Long lastTp = lastHomeTeleportTimestamps.putIfAbsent(playerId, now);
            if (lastTp != null) {
                long elapsed = (now - lastTp) / 1000L;
                if (elapsed < homeTeleportCooldownSeconds) {
                    long wait = homeTeleportCooldownSeconds - elapsed;
                    player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.home.teleport_cooldown", wait));
                    return;
                }
                // Update timestamp atomically
                lastHomeTeleportTimestamps.put(playerId, now);
            }
        }

        // Enforce maxTeleportDistance if set in config
        int maxDistance = com.zerog.neoessentials.config.ConfigManager.getInstance().getMaxTeleportDistance();
        if (maxDistance > 0 && home != null) {
            TeleportLocation fromLoc = new TeleportLocation(player);
            if (fromLoc.getWorldName().equals(home.getWorldName())) {
                double dist = fromLoc.distanceTo(home);
                if (dist > maxDistance) {
                    player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.home.distance_exceeded", maxDistance));
                    return;
                }
            }
        }

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

            // Update home to safe location atomically
            playerHomes.computeIfPresent(playerId, (id, homes) -> {
                homes.put(homeName, safeLocation);
                return homes;
            });

            // Save to file (per-player storage)
            savePlayerHomes(playerId);
            home = safeLocation;

            player.sendSystemMessage(MessageUtil.warning("commands.neoessentials.teleport.home.moved_to_safety", homeName));
        }

        // Perform teleportation
        int delayTicks = teleportDelay * 20; // Convert seconds to ticks
        TeleportUtil.teleportPlayer(player, home, delayTicks, true).thenAccept(result -> {
            if (result.isSuccess()) {
                player.sendSystemMessage(MessageUtil.success("commands.neoessentials.teleport.home.success", homeName));
                // Log home teleport if enabled in config
                if (com.zerog.neoessentials.config.ConfigManager.getInstance().isLogHomeActionsEnabled()) {
                    LOGGER.info("Player {} teleported to home '{}'", player.getName().getString(), homeName);
                }
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
    int allowedHomes = getMaxHomesForPlayer(player);
    builder.append(MessageUtil.localize("commands.neoessentials.teleport.home.list_header", homes.size(), allowedHomes));
        
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
        Map<String, TeleportLocation> homes = getOrLoadPlayerHomes(playerId);
        return !homes.isEmpty();
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
     * Load homes from file (legacy - loads all players for compatibility)
     * New code should use loadPlayerHomes(UUID) instead
     */
    private void loadHomes() {
        // This method is now only called during initialization
        // Individual player homes are loaded on-demand via getHomes()
        LOGGER.debug("Home loading is now on-demand per player");
    }

    /**
     * Load a specific player's homes from their data file
     */
    private Map<String, TeleportLocation> loadPlayerHomes(UUID playerId) {
        try {
            JsonObject data = playerDataStore.load(playerId);
            Map<String, TeleportLocation> homes = new HashMap<>();

            if (data.keySet().isEmpty()) {
                LOGGER.debug("No homes found for player {}", playerId);
                return homes;
            }

            for (String homeName : data.keySet()) {
                try {
                    JsonObject homeJson = data.getAsJsonObject(homeName);
                    TeleportLocation location = TeleportLocation.fromJson(homeJson);
                    if (location != null) {
                        homes.put(homeName, location);
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to load home '{}' for player {}: {}",
                        homeName, playerId, e.getMessage());
                }
            }
            
            LOGGER.debug("Loaded {} homes for player {}", homes.size(), playerId);
            return homes;

        } catch (Exception e) {
            LOGGER.error("Failed to load homes for player {}: {}", playerId, e.getMessage(), e);
            return new HashMap<>();
        }
    }
    
    /**
     * Save homes to file (atomic operation)
     * Legacy method - kept for compatibility but delegates to per-player save
     */
    private void saveHomes() {
        // Save all loaded players' homes
        for (UUID playerId : playerHomes.keySet()) {
            savePlayerHomes(playerId);
        }
    }

    /**
     * Save a specific player's homes to their data file
     */
    private void savePlayerHomes(UUID playerId) {
        try {
            Map<String, TeleportLocation> homes = playerHomes.get(playerId);
            if (homes == null || homes.isEmpty()) {
                // No homes to save, but ensure file is created (empty data)
                playerDataStore.save(playerId, new JsonObject());
                return;
            }

            JsonObject data = new JsonObject();
            for (Map.Entry<String, TeleportLocation> entry : homes.entrySet()) {
                data.add(entry.getKey(), entry.getValue().toJson());
            }
            
            playerDataStore.save(playerId, data);
            LOGGER.debug("Saved {} homes for player {}", homes.size(), playerId);

        } catch (Exception e) {
            LOGGER.error("Failed to save homes for player {}: {}", playerId, e.getMessage(), e);
        }
    }
    

    // Configuration getters/setters
    public int getMaxHomesPerPlayer() { return maxHomesPerPlayer; }
    public void setMaxHomesPerPlayer(int max) { this.maxHomesPerPlayer = Math.max(1, max); }

    public boolean isAllowOverworldOnly() { return allowOverworldOnly; }
    public void setAllowOverworldOnly(boolean allow) { this.allowOverworldOnly = allow; }

    public boolean isAllowCrossDimensionHomes() { return allowCrossDimensionHomes; }
    public void setAllowCrossDimensionHomes(boolean allow) { this.allowCrossDimensionHomes = allow; }

    public boolean isRequireSafeLocations() { return requireSafeLocations; }
    public void setRequireSafeLocations(boolean require) { this.requireSafeLocations = require; }

    public int getTeleportDelay() { return teleportDelay; }
    public void setTeleportDelay(int delay) { this.teleportDelay = Math.max(0, delay); }

    public int getHomeSetCooldownSeconds() { return homeSetCooldownSeconds; }
    public void setHomeSetCooldownSeconds(int seconds) { this.homeSetCooldownSeconds = Math.max(0, seconds); }

    public int getHomeTeleportCooldownSeconds() { return homeTeleportCooldownSeconds; }
    public void setHomeTeleportCooldownSeconds(int seconds) { this.homeTeleportCooldownSeconds = Math.max(0, seconds); }
    
    /**
     * Clear all homes (for testing/admin purposes)
     */
    public void clearAllHomes() {
        playerHomes.clear();
        playerDataStore.clearAll();
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

    /**
     * Reload home data from disk
     */
    public void reload() {
        LOGGER.info("Reloading home system...");

        // Reload config values
        loadConfig();

        // Flush any pending saves before clearing cache
        playerDataStore.flushAll();

        // Clear cache - homes will be loaded on-demand from PlayerDataStore
        playerHomes.clear();

        LOGGER.info("Home system reloaded - {} players in storage, homes will load on-demand",
            playerDataStore.getTotalPlayers());
    }
}