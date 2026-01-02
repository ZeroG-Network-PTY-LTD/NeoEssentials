package com.zerog.neoessentials.teleportation.Warp;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.teleportation.TeleportLocation;
import com.zerog.neoessentials.teleportation.TeleportUtil;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.ResourceUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages public warp points accessible to all players
 */
public class WarpManager {
    // Cooldown for setting warps (seconds) and per-player last set timestamps
    private final Map<UUID, Long> lastWarpSetTimestamps = new ConcurrentHashMap<>();
    private int warpSetCooldown = 0;
    // --- Persistence for player warps ---
    // private static final String PLAYER_WARPS_FILE = "playerwarps.json";


    // (savePlayerWarps and loadPlayerWarps are defined only once below)
    // Player warps: UUID -> (warpName -> TeleportLocation)
    private final Map<UUID, Map<String, TeleportLocation>> playerWarps = new ConcurrentHashMap<>();
    // Player warp config
    private int maxPlayerWarps = 3;
    private boolean allowPlayerWarps = false;
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
    private boolean allowCrossDimensionWarps = true;

    private WarpManager() {
        // Load config values
        try {
            ConfigManager configManager = ConfigManager.getInstance();
            boolean safe = true;
            if (configManager != null) {
                JsonObject config = configManager.getConfig(ConfigManager.MAIN_CONFIG);
                if (config.has("teleportation")) {
                    JsonObject tp = config.getAsJsonObject("teleportation");
                    if (tp.has("warpSettings")) {
                        JsonObject warpSettings = tp.getAsJsonObject("warpSettings");
                        if (warpSettings.has("enableWarpSafety")) {
                            safe = warpSettings.get("enableWarpSafety").getAsBoolean();
                        }
                        if (warpSettings.has("allowPlayerWarps")) {
                            allowPlayerWarps = warpSettings.get("allowPlayerWarps").getAsBoolean();
                        }
                        if (warpSettings.has("maxPlayerWarps")) {
                            try {
                                maxPlayerWarps = warpSettings.get("maxPlayerWarps").getAsInt();
                            } catch (Exception ignored) {}
                        }
                        if (warpSettings.has("warpSetCooldown")) {
                            try {
                                warpSetCooldown = warpSettings.get("warpSetCooldown").getAsInt();
                            } catch (Exception ignored) {}
                        }
                        if (warpSettings.has("allowCrossDimensionWarps")) {
                            try {
                                allowCrossDimensionWarps = warpSettings.get("allowCrossDimensionWarps").getAsBoolean();
                            } catch (Exception ignored) {}
                        }
                    }
                }
            }
            this.requireSafeLocations = safe;
        } catch (Exception e) {
            LOGGER.warn("Failed to load warp config, defaulting to safe: {}", e.getMessage());
        }
        loadWarps();
        loadPlayerWarps();
    }

    // --- Player Warps API ---
    public boolean isPlayerWarpsEnabled() {
        return allowPlayerWarps;
    }

    public int getMaxPlayerWarps() {
        return maxPlayerWarps;
    }

    /**
     * Returns the maximum number of player warps allowed for a player, considering permissions.
     * If the player has the permission node neoessentials.warp.limit.<amount>, that value is used if higher than config.
     *
     * <p>Permission examples:</p>
     * <ul>
     *   <li>neoessentials.warp.limit.5 - Allows 5 player warps</li>
     *   <li>neoessentials.warp.limit.10 - Allows 10 player warps</li>
     *   <li>neoessentials.warp.limit.unlimited - Unlimited player warps</li>
     * </ul>
     *
     * @param player The player to check
     * @return Maximum number of player warps allowed (or -1 for unlimited)
     */
    public int getMaxPlayerWarpsForPlayer(ServerPlayer player) {
        // Check for unlimited permission first
        if (com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(
                player.getUUID(), "neoessentials.warp.limit.unlimited")) {
            return -1; // Unlimited
        }

        int configMax = this.maxPlayerWarps;
        int permMax = -1;

        // Check for permissions neoessentials.warp.limit.<amount> from high to low (e.g., 100 down to 1)
        for (int i = 100; i >= 1; i--) {
            String perm = "neoessentials.warp.limit." + i;
            if (com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(player.getUUID(), perm)) {
                permMax = i;
                break;
            }
        }

        // Return the higher value between config and permission
        if (permMax > configMax) {
            return permMax;
        }
        return configMax;
    }

    public boolean createPlayerWarp(ServerPlayer player, String warpName, TeleportLocation location) {
        if (!allowPlayerWarps) {
            player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.warp.playerwarps_disabled"));
            return false;
        }
        if (!allowCrossDimensionWarps && !isOverworld(location)) {
            player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.warp.cross_dimension_disabled"));
            return false;
        }
        
        // Enforce warp set cooldown per player (atomic check)
        if (warpSetCooldown > 0) {
            long now = System.currentTimeMillis();
            UUID playerId = player.getUUID();
            Long lastSet = lastWarpSetTimestamps.putIfAbsent(playerId, now);
            if (lastSet != null && (now - lastSet < warpSetCooldown * 1000L)) {
                long secondsLeft = (warpSetCooldown - ((now - lastSet) / 1000));
                player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.warp.set_cooldown", secondsLeft));
                return false;
            }
            // Update timestamp atomically if cooldown passed
            if (lastSet != null) {
                lastWarpSetTimestamps.put(playerId, now);
            }
        }
        
        UUID playerId = player.getUUID();
        String normalizedName = caseSensitiveNames ? warpName : warpName.toLowerCase();
        
        if (!isValidWarpName(warpName)) {
            player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.warp.invalid_name", warpName));
            return false;
        }
        
        // Atomic warp creation with limit check using compute()
        Map<String, TeleportLocation> warps = playerWarps.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
        
        // Check if warp already exists and enforce limit atomically
        if (warps.containsKey(normalizedName)) {
            player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.warp.already_exists", warpName));
            return false;
        }
        
        // Get max warps for this player (considers permissions)
        int maxWarpsForPlayer = getMaxPlayerWarpsForPlayer(player);

        // Atomic limit check and insertion (-1 means unlimited)
        if (maxWarpsForPlayer >= 0 && warps.size() >= maxWarpsForPlayer) {
            player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.warp.playerwarps_limit", maxWarpsForPlayer));
            return false;
        }
        
        // Use putIfAbsent to prevent race condition on duplicate names
        TeleportLocation existing = warps.putIfAbsent(normalizedName, location);
        if (existing != null) {
            player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.warp.already_exists", warpName));
            return false;
        }
        
        savePlayerWarps();
        player.sendSystemMessage(MessageUtil.success("commands.neoessentials.teleport.warp.playerwarp_created", warpName, location.getLocationString()));
        LOGGER.info("Player {} created player warp '{}' at {}", player.getName().getString(), warpName, location.getLocationString());
        return true;
    }

    public boolean deletePlayerWarp(ServerPlayer player, String warpName) {
        UUID playerId = player.getUUID();
        Map<String, TeleportLocation> warps = playerWarps.get(playerId);
        if (warps == null) {
            player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.warp.not_found", warpName));
            return false;
        }
        
        String normalizedName = caseSensitiveNames ? warpName : warpName.toLowerCase();
        
        // Atomic removal - remove() returns null if key doesn't exist
        TeleportLocation removed = warps.remove(normalizedName);
        if (removed == null) {
            player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.warp.not_found", warpName));
            return false;
        }
        
        savePlayerWarps();
        player.sendSystemMessage(MessageUtil.success("commands.neoessentials.teleport.warp.playerwarp_deleted", warpName));
        LOGGER.info("Player {} deleted player warp '{}'", player.getName().getString(), warpName);
        return true;
    }

    public TeleportLocation getPlayerWarp(ServerPlayer player, String warpName) {
        UUID playerId = player.getUUID();
        Map<String, TeleportLocation> warps = playerWarps.get(playerId);
        if (warps == null) return null;
        String normalizedName = caseSensitiveNames ? warpName : warpName.toLowerCase();
        return warps.get(normalizedName);
    }

    public List<String> getPlayerWarpNames(ServerPlayer player) {
        UUID playerId = player.getUUID();
        Map<String, TeleportLocation> warps = playerWarps.get(playerId);
        if (warps == null) return Collections.emptyList();
        return new ArrayList<>(warps.keySet());
    }


    /**
     * Teleport to another player's warp (admin only)
     */
    public boolean teleportToPlayerWarp(ServerPlayer admin, UUID targetPlayerId, String warpName) {
        if (!isAdmin(admin)) {
            admin.sendSystemMessage(MessageUtil.error("You do not have permission to access other players' warps."));
            return false;
        }
        Map<String, TeleportLocation> warps = playerWarps.get(targetPlayerId);
        if (warps == null) {
            admin.sendSystemMessage(MessageUtil.error("Target player has no warps."));
            return false;
        }
        TeleportLocation location = warps.get(caseSensitiveNames ? warpName : warpName.toLowerCase());
        if (location == null) {
            admin.sendSystemMessage(MessageUtil.error("Warp not found for target player."));
            return false;
        }
            TeleportUtil.teleportPlayer(admin, location);
        admin.sendSystemMessage(MessageUtil.success("Teleported to target player's warp."));
        return true;
    }

    /**
     * List another player's warps (admin only)
     */
    public List<String> listPlayerWarps(UUID targetPlayerId, ServerPlayer admin) {
        if (!isAdmin(admin)) {
            admin.sendSystemMessage(MessageUtil.error("You do not have permission to list other players' warps."));
            return List.of();
        }
        Map<String, TeleportLocation> warps = playerWarps.get(targetPlayerId);
        if (warps == null) return List.of();
        return new ArrayList<>(warps.keySet());
    }

    /**
     * Check if a player is an admin (placeholder, replace with real permission check)
     */
    private boolean isAdmin(ServerPlayer player) {
        // Replace with your real admin/permission check
        return player.hasPermissions(4); // Permission level 4 = admin/op
    }

    // --- Persistence for player warps ---
    private static final String PLAYER_WARPS_FILE = "run/playerwarps.json";

    private void savePlayerWarps() {
        try {
            Map<String, Map<String, TeleportLocation>> serializable = new HashMap<>();
            for (Map.Entry<UUID, Map<String, TeleportLocation>> entry : playerWarps.entrySet()) {
                serializable.put(entry.getKey().toString(), entry.getValue());
            }
            String json = new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(serializable);
            java.nio.file.Files.writeString(java.nio.file.Path.of(PLAYER_WARPS_FILE), json);
        } catch (Exception e) {
            System.err.println("[WarpManager] Failed to save player warps: " + e);
        }
    }

    private void loadPlayerWarps() {
        try {
            java.nio.file.Path path = java.nio.file.Path.of(PLAYER_WARPS_FILE);
            if (!java.nio.file.Files.exists(path)) return;
            String json = java.nio.file.Files.readString(path);
            java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<Map<String, Map<String, TeleportLocation>>>(){}.getType();
            Map<String, Map<String, TeleportLocation>> loaded = new com.google.gson.Gson().fromJson(json, type);
            playerWarps.clear();
            for (Map.Entry<String, Map<String, TeleportLocation>> entry : loaded.entrySet()) {
                playerWarps.put(UUID.fromString(entry.getKey()), entry.getValue());
            }
        } catch (Exception e) {
            System.err.println("[WarpManager] Failed to load player warps: " + e);
        }
    }
    
    /**
     * Create a new warp
     */
    public boolean createWarp(ServerPlayer creator, String warpName, TeleportLocation location) {
        // Enforce warp set cooldown per player (atomic check)
        if (warpSetCooldown > 0) {
            long now = System.currentTimeMillis();
            UUID playerId = creator.getUUID();
            Long lastSet = lastWarpSetTimestamps.putIfAbsent(playerId, now);
            if (lastSet != null && (now - lastSet < warpSetCooldown * 1000L)) {
                long secondsLeft = (warpSetCooldown - ((now - lastSet) / 1000));
                creator.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.warp.set_cooldown", secondsLeft));
                return false;
            }
            // Update timestamp atomically if cooldown passed
            if (lastSet != null) {
                lastWarpSetTimestamps.put(playerId, now);
            }
        }
        
        // Enforce cross-dimension restriction
        if (!allowCrossDimensionWarps && !isOverworld(location)) {
            creator.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.warp.cross_dimension_disabled"));
            return false;
        }
        
        // Normalize warp name
        String normalizedName = caseSensitiveNames ? warpName : warpName.toLowerCase();
        
        // Validate warp name
        if (!isValidWarpName(warpName)) {
            creator.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.warp.invalid_name", warpName));
            return false;
        }
        
        // Check warp limit before attempting creation
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
        
        // Atomic warp creation using putIfAbsent to prevent duplicate names
        TeleportLocation existing = warps.putIfAbsent(normalizedName, location);
        if (existing != null) {
            creator.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.warp.already_exists", warpName));
            return false;
        }
        
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
     * Check if warp exists
     */
    public boolean hasWarp(String warpName) {
        String normalizedName = caseSensitiveNames ? warpName : warpName.toLowerCase();
        return warps.containsKey(normalizedName);
    }
    
    /**
     * Get warp by name
     */
    public TeleportLocation getWarp(String warpName) {
        String normalizedName = caseSensitiveNames ? warpName : warpName.toLowerCase();
        return warps.get(normalizedName);
    }
    
    /**
     * Get list of all warp names
     */
    public List<String> getWarpNames() {
        return new ArrayList<>(warps.keySet());
    }
    
    /**
     * Delete a warp (admin/staff command)
     */
    public boolean deleteWarp(ServerPlayer player, String warpName) {
        String normalizedName = caseSensitiveNames ? warpName : warpName.toLowerCase();
        
        // Atomic removal using remove() which returns null if key doesn't exist
        TeleportLocation removed = warps.remove(normalizedName);
        if (removed == null) {
            return false;
        }
        
        saveWarps();
        
        if (com.zerog.neoessentials.config.ConfigManager.getInstance().isLogWarpActionsEnabled()) {
            LOGGER.info("Player {} deleted warp '{}'", player.getName().getString(), warpName);
        }
        return true;
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

        // Enforce maxTeleportDistance if set in config
        int maxDistance = com.zerog.neoessentials.config.ConfigManager.getInstance().getMaxTeleportDistance();
        if (maxDistance > 0 && warp != null) {
            com.zerog.neoessentials.teleportation.TeleportLocation fromLoc = new com.zerog.neoessentials.teleportation.TeleportLocation(player);
            if (fromLoc.getWorldName().equals(warp.getWorldName())) {
                double dist = fromLoc.distanceTo(warp);
                if (dist > maxDistance) {
                    player.sendSystemMessage(com.zerog.neoessentials.util.MessageUtil.error("commands.neoessentials.teleport.warp.distance_exceeded", maxDistance));
                    return;
                }
            }
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

    /**
     * Reload warp data from disk
     */
    public void reload() {
        LOGGER.info("Reloading warp system...");
        warps.clear();
        playerWarps.clear();
        loadWarps();
        loadPlayerWarps();
        LOGGER.info("Warp system reloaded: {} warps, {} player warps loaded", warps.size(),
            playerWarps.values().stream().mapToInt(Map::size).sum());
    }
}