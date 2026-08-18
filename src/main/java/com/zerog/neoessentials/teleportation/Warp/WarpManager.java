package com.zerog.neoessentials.teleportation.Warp;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
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
@SuppressWarnings({"unused", "InvertedCondition"}) // Public API class
public class WarpManager {
    // Cooldown for setting warps (seconds) and per-player last set timestamps
    private final Map<UUID, Long> lastWarpSetTimestamps = new ConcurrentHashMap<>();
    private int warpSetCooldown = 0;
    // Cooldown for USING warps (seconds) and per-player last use timestamps
    private final Map<UUID, Long> lastWarpUseTimestamps = new ConcurrentHashMap<>();
    private int warpUseCooldown = 0;
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

    private static final String WARPS_COLLECTION = "warps";
    private static final String PLAYER_WARPS_COLLECTION = "player_warps";
    // Reserved record id (not a valid warp name — isValidWarpName() only allows
    // [a-zA-Z0-9_-]) used to persist the warp settings blob that used to live under
    // warps.json's "config" key.
    private static final String CONFIG_RECORD_ID = "!!config!!";
    private final com.zerog.neoessentials.storage.DataStore store =
        com.zerog.neoessentials.storage.StorageManager.getInstance().getStore();

    // Configuration
    private int teleportDelay = 0; // Instant for warps by default
    private boolean requireSafeLocations = true;
    private boolean allowOverworldOnly = false;
    private int maxWarps = 50;
    private boolean caseSensitiveNames = false;
    private boolean allowCrossDimensionWarps = true;

    private WarpManager() {
        loadConfig();
        migrateLegacyFilesIfNeeded();
        loadWarps();
        loadPlayerWarps();
    }

    /**
     * Load configuration values from config file
     */
    private void loadConfig() {
        try {
            ConfigManager configManager = ConfigManager.getInstance();
            if (configManager != null) {
                JsonObject config = configManager.getConfig(ConfigManager.MAIN_CONFIG);
                if (config.has("teleportation")) {
                    JsonObject tp = config.getAsJsonObject("teleportation");

                    // Load warp settings
                    if (tp.has("warpSettings")) {
                        JsonObject warpSettings = tp.getAsJsonObject("warpSettings");

                        if (warpSettings.has("enableWarpSafety")) {
                            requireSafeLocations = warpSettings.get("enableWarpSafety").getAsBoolean();
                        }
                        if (warpSettings.has("allowPlayerWarps")) {
                            allowPlayerWarps = warpSettings.get("allowPlayerWarps").getAsBoolean();
                        }
                        if (warpSettings.has("maxPlayerWarps")) {
                            try {
                                maxPlayerWarps = warpSettings.get("maxPlayerWarps").getAsInt();
                            } catch (Exception e) {
                                NeoLog.debug(LOGGER, LogCategory.TELEPORTATION,
                                    "Failed to parse warpSettings.maxPlayerWarps, using default", e);
                            }
                        }
                        if (warpSettings.has("warpSetCooldown")) {
                            try {
                                warpSetCooldown = warpSettings.get("warpSetCooldown").getAsInt();
                            } catch (Exception e) {
                                NeoLog.debug(LOGGER, LogCategory.TELEPORTATION,
                                    "Failed to parse warpSettings.warpSetCooldown, using default", e);
                            }
                        }
                        if (warpSettings.has("warpCooldown")) {
                            try {
                                warpUseCooldown = warpSettings.get("warpCooldown").getAsInt();
                            } catch (Exception e) {
                                NeoLog.debug(LOGGER, LogCategory.TELEPORTATION,
                                    "Failed to parse warpSettings.warpCooldown, using default", e);
                            }
                        }
                        if (warpSettings.has("allowCrossDimensionWarps")) {
                            try {
                                allowCrossDimensionWarps = warpSettings.get("allowCrossDimensionWarps").getAsBoolean();
                            } catch (Exception e) {
                                NeoLog.debug(LOGGER, LogCategory.TELEPORTATION,
                                    "Failed to parse warpSettings.allowCrossDimensionWarps, using default", e);
                            }
                        }
                    }

                    // Load general teleportation settings that apply to warps
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
            NeoLog.info(LOGGER, LogCategory.TELEPORTATION, "[WarpManager] Config loaded — safetyCheck={}, maxWarps={}, allowPlayerWarps={}, maxPlayerWarps={}, warmup={}s, useCooldown={}s, setCooldown={}s, crossDimension={}",
                requireSafeLocations, maxWarps, allowPlayerWarps, maxPlayerWarps, teleportDelay, warpUseCooldown, warpSetCooldown, allowCrossDimensionWarps);
        } catch (Exception e) {
            LOGGER.warn("Failed to load warp config, using defaults: {}", e.getMessage());
        }
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
        // Return the higher value between permission-based and config-based limits
        return Math.max(permMax, configMax);
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
        
        // Check warp-set cooldown (read-only) — only actually consumed once the warp genuinely
        // gets created (see below); an invalid name, a name collision, or the player-warp limit
        // no longer costs the cooldown for a /setpwarp that never took effect.
        UUID playerId = player.getUUID();
        if (warpSetCooldown > 0) {
            Long lastSet = lastWarpSetTimestamps.get(playerId);
            if (lastSet != null) {
                long elapsed = System.currentTimeMillis() - lastSet;
                if (elapsed < warpSetCooldown * 1000L) {
                    long secondsLeft = (warpSetCooldown - (elapsed / 1000));
                    player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.warp.set_cooldown", secondsLeft));
                    return false;
                }
            }
        }

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

        // Warp genuinely created — now commit the cooldown.
        if (warpSetCooldown > 0) {
            lastWarpSetTimestamps.put(playerId, System.currentTimeMillis());
        }

        savePlayerWarps();
        player.sendSystemMessage(MessageUtil.success("commands.neoessentials.teleport.warp.playerwarp_created", warpName, location.getLocationString()));
        NeoLog.info(LOGGER, LogCategory.TELEPORTATION, "Player {} created player warp '{}' at {}", player.getName().getString(), warpName, location.getLocationString());
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
        NeoLog.info(LOGGER, LogCategory.TELEPORTATION, "Player {} deleted player warp '{}'", player.getName().getString(), warpName);
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
     * All players' warps, keyed by owner UUID. For dashboard/REST use — unlike
     * {@link #teleportToPlayerWarp} / {@link #listPlayerWarps}, this doesn't require an
     * online {@link ServerPlayer} caller.
     */
    public Map<UUID, Map<String, TeleportLocation>> getAllPlayerWarps() {
        return Collections.unmodifiableMap(playerWarps);
    }

    /**
     * One player's warps by raw UUID, without requiring them to be online. For dashboard/REST use.
     */
    public Map<String, TeleportLocation> getPlayerWarpsRaw(UUID playerId) {
        Map<String, TeleportLocation> warps = playerWarps.get(playerId);
        return warps == null ? Collections.emptyMap() : Collections.unmodifiableMap(warps);
    }

    /**
     * Admin/dashboard deletion of another player's warp by raw UUID — no {@link ServerPlayer}
     * required, so no in-game messaging (unlike {@link #deletePlayerWarp}).
     */
    public boolean deletePlayerWarpByAdmin(UUID playerId, String warpName) {
        Map<String, TeleportLocation> warps = playerWarps.get(playerId);
        if (warps == null) return false;

        String normalizedName = caseSensitiveNames ? warpName : warpName.toLowerCase();
        TeleportLocation removed = warps.remove(normalizedName);
        if (removed == null) return false;

        savePlayerWarps();
        NeoLog.info(LOGGER, LogCategory.TELEPORTATION, "Admin deleted player warp '{}' for {}", warpName, playerId);
        return true;
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
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean isAdmin(ServerPlayer player) {
        // Replace with your real admin/permission check
        return com.zerog.neoessentials.util.PermissionLevelCompat.hasPermission(player, 4); // Permission level 4 = admin/op
    }

    // --- Persistence for player warps ---
    // NOTE: this used to be hard-coded to the literal relative path "run/playerwarps.json"
    // (a pre-existing bug — it was never using ResourceUtil's data dir like every other
    // manager). It now persists through the DataStore under PLAYER_WARPS_COLLECTION; the
    // old "run/playerwarps.json" path is only ever read once more, during legacy migration.

    private void savePlayerWarps() {
        for (Map.Entry<UUID, Map<String, TeleportLocation>> entry : playerWarps.entrySet()) {
            store.put(PLAYER_WARPS_COLLECTION, entry.getKey().toString(), playerWarpsToJson(entry.getValue()));
        }
    }

    private void loadPlayerWarps() {
        playerWarps.clear();
        for (Map.Entry<String, JsonObject> entry : store.getAll(PLAYER_WARPS_COLLECTION).entrySet()) {
            try {
                UUID playerId = UUID.fromString(entry.getKey());
                playerWarps.put(playerId, playerWarpsFromJson(entry.getValue()));
            } catch (Exception e) {
                LOGGER.warn("Failed to load player warps for '{}': {}", entry.getKey(), e.getMessage());
            }
        }
    }

    /** Serializes one player's named warps into {@code {"warps": {name -> location}}}. */
    private JsonObject playerWarpsToJson(Map<String, TeleportLocation> warpsForPlayer) {
        JsonObject root = new JsonObject();
        JsonObject warpsJson = new JsonObject();
        for (Map.Entry<String, TeleportLocation> entry : warpsForPlayer.entrySet()) {
            warpsJson.add(entry.getKey(), entry.getValue().toJson());
        }
        root.add("warps", warpsJson);
        return root;
    }

    private Map<String, TeleportLocation> playerWarpsFromJson(JsonObject root) {
        Map<String, TeleportLocation> result = new ConcurrentHashMap<>();
        if (root != null && root.has("warps")) {
            JsonObject warpsJson = root.getAsJsonObject("warps");
            for (String warpName : warpsJson.keySet()) {
                try {
                    TeleportLocation location = TeleportLocation.fromJson(warpsJson.getAsJsonObject(warpName));
                    if (location != null) {
                        result.put(warpName, location);
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to load player warp '{}': {}", warpName, e.getMessage());
                }
            }
        }
        return result;
    }

    /**
     * One-time import of the legacy warps.json / playerwarps.json files into the active
     * DataStore, if it's still empty and storage.autoMigrate is enabled. The legacy
     * per-player file was (buggily) written to the hard-coded relative path
     * "run/playerwarps.json" instead of ResourceUtil's data dir — that path is only
     * consulted here, once, to import any pre-existing data.
     */
    private void migrateLegacyFilesIfNeeded() {
        if (store.hasAnyData(WARPS_COLLECTION) || store.hasAnyData(PLAYER_WARPS_COLLECTION)) return;
        if (!ConfigManager.getInstance().isStorageAutoMigrateEnabled()) return;

        int migratedWarps = migrateLegacyWarpsFile();
        int migratedPlayerWarps = migrateLegacyPlayerWarpsFile();

        if (migratedWarps > 0 || migratedPlayerWarps > 0) {
            NeoLog.info(LOGGER, LogCategory.TELEPORTATION, "WarpManager: migrated {} warp(s) and {} player warp owner(s) from legacy files into the '{}' storage backend.",
                migratedWarps, migratedPlayerWarps, com.zerog.neoessentials.storage.StorageManager.getInstance().getActiveType());
        }
    }

    private int migrateLegacyWarpsFile() {
        try {
            File file = ResourceUtil.getDataFile(WARPS_FILE);
            if (!file.exists()) return 0;
            String content = java.nio.file.Files.readString(file.toPath());
            if (content.trim().isEmpty()) return 0;
            JsonObject root = JsonParser.parseString(content).getAsJsonObject();
            int count = 0;
            if (root.has("warps")) {
                JsonObject warpsJson = root.getAsJsonObject("warps");
                for (String warpName : warpsJson.keySet()) {
                    store.put(WARPS_COLLECTION, warpName, warpsJson.getAsJsonObject(warpName));
                    count++;
                }
            }
            if (root.has("config")) {
                store.put(WARPS_COLLECTION, CONFIG_RECORD_ID, root.getAsJsonObject("config"));
            }
            return count;
        } catch (Exception e) {
            LOGGER.error("Failed to migrate legacy warps.json: {}", e.getMessage());
            return 0;
        }
    }

    private int migrateLegacyPlayerWarpsFile() {
        // Legacy hard-coded path (bug being fixed): a File relative to the working
        // directory, NOT ResourceUtil's data dir. Consulted only here for one-time import.
        try {
            java.nio.file.Path path = java.nio.file.Path.of("run/playerwarps.json");
            if (!java.nio.file.Files.exists(path)) return 0;
            String json = java.nio.file.Files.readString(path);
            java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<Map<String, Map<String, TeleportLocation>>>(){}.getType();
            Map<String, Map<String, TeleportLocation>> loaded = new com.google.gson.Gson().fromJson(json, type);
            if (loaded == null) return 0;
            int count = 0;
            for (Map.Entry<String, Map<String, TeleportLocation>> entry : loaded.entrySet()) {
                store.put(PLAYER_WARPS_COLLECTION, entry.getKey(), playerWarpsToJson(entry.getValue()));
                count++;
            }
            return count;
        } catch (Exception e) {
            LOGGER.error("Failed to migrate legacy run/playerwarps.json: {}", e.getMessage());
            return 0;
        }
    }
    
    /**
     * Create a new warp
     */
    public boolean createWarp(ServerPlayer creator, String warpName, TeleportLocation location) {
        // Check warp-set cooldown (read-only) — only actually consumed once the warp genuinely
        // gets created (see below). Same fix as setHome/PayCommand/TeleportRequestManager's
        // cooldown bugs earlier this session: consuming it here unconditionally meant an
        // invalid name, the warp limit, a restricted world, an unreachable safe spot, or a
        // name collision all still cost the player a full cooldown for a /setwarp that never
        // took effect.
        UUID playerId = creator.getUUID();
        if (warpSetCooldown > 0) {
            Long lastSet = lastWarpSetTimestamps.get(playerId);
            if (lastSet != null) {
                long elapsed = System.currentTimeMillis() - lastSet;
                if (elapsed < warpSetCooldown * 1000L) {
                    long secondsLeft = (warpSetCooldown - (elapsed / 1000));
                    creator.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.warp.set_cooldown", secondsLeft));
                    return false;
                }
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
        
        // Check if location is safe - read from config dynamically
        boolean requireSafe = true; // Default to true for safety
        try {
            JsonObject config = ConfigManager.getInstance().getConfig(ConfigManager.MAIN_CONFIG);
            if (config.has("teleportation")) {
                JsonObject tp = config.getAsJsonObject("teleportation");
                if (tp.has("warpSettings")) {
                    JsonObject warpSettings = tp.getAsJsonObject("warpSettings");
                    if (warpSettings.has("enableWarpSafety")) {
                        requireSafe = warpSettings.get("enableWarpSafety").getAsBoolean();
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to read warp safety config, defaulting to enabled: {}", e.getMessage());
        }

        if (requireSafe && !location.isSafe()) {
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

        // Warp genuinely created — now commit the cooldown.
        if (warpSetCooldown > 0) {
            lastWarpSetTimestamps.put(playerId, System.currentTimeMillis());
        }

        saveWarps();
        
        creator.sendSystemMessage(MessageUtil.success("commands.neoessentials.teleport.warp.created", warpName, location.getLocationString()));
        NeoLog.info(LOGGER, LogCategory.TELEPORTATION, "Player {} created warp '{}' at {}", creator.getName().getString(), warpName, location.getLocationString());
        
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
     * Create a warp — admin/console variant that doesn't require a ServerPlayer
     * (used by the web dashboard, where there's no in-game player to message).
     * Applies the same validation as {@link #createWarp(ServerPlayer, String, TeleportLocation)}
     * (name format, warp limit, cross-dimension/overworld-only restrictions, safety check)
     * but reports failures via the returned reason instead of chat messages.
     *
     * @return {@code null} on success, or a short reason string on failure.
     */
    public String createWarpByAdmin(String warpName, TeleportLocation location, String createdBy) {
        if (!isValidWarpName(warpName)) {
            return "invalid_name";
        }

        if (warps.size() >= maxWarps) {
            return "limit_reached";
        }

        if (!allowCrossDimensionWarps && !isOverworld(location)) {
            return "cross_dimension_disabled";
        }

        if (allowOverworldOnly && !isOverworld(location)) {
            return "overworld_only";
        }

        boolean requireSafe = true;
        try {
            JsonObject config = ConfigManager.getInstance().getConfig(ConfigManager.MAIN_CONFIG);
            if (config.has("teleportation")) {
                JsonObject tp = config.getAsJsonObject("teleportation");
                if (tp.has("warpSettings")) {
                    JsonObject warpSettings = tp.getAsJsonObject("warpSettings");
                    if (warpSettings.has("enableWarpSafety")) {
                        requireSafe = warpSettings.get("enableWarpSafety").getAsBoolean();
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to read warp safety config, defaulting to enabled: {}", e.getMessage());
        }

        if (requireSafe && !location.isSafe()) {
            TeleportLocation safeLocation = location.findSafeLocation();
            if (safeLocation == null) {
                return "unsafe_location";
            }
            location = safeLocation;
        }

        String normalizedName = caseSensitiveNames ? warpName : warpName.toLowerCase();
        TeleportLocation existing = warps.putIfAbsent(normalizedName, location);
        if (existing != null) {
            return "already_exists";
        }

        saveWarps();
        if (ConfigManager.getInstance().isLogWarpActionsEnabled()) {
            NeoLog.info(LOGGER, LogCategory.TELEPORTATION, "Warp '{}' created by {} at {}", warpName, createdBy, location.getLocationString());
        }
        return null;
    }

    /**
     * Delete a warp by name — admin/console variant that doesn't require a ServerPlayer.
     * Essentials: Warps.removeWarp(name)
     */
    public boolean deleteWarpByAdmin(String warpName, String deletedBy) {
        String normalizedName = caseSensitiveNames ? warpName : warpName.toLowerCase();
        TeleportLocation removed = warps.remove(normalizedName);
        if (removed == null) return false;
        saveWarps();
        if (ConfigManager.getInstance().isLogWarpActionsEnabled()) {
            NeoLog.info(LOGGER, LogCategory.TELEPORTATION, "Warp '{}' deleted by {}", warpName, deletedBy);
        }
        return true;
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
            NeoLog.info(LOGGER, LogCategory.TELEPORTATION, "Player {} deleted warp '{}'", player.getName().getString(), warpName);
        }
        return true;
    }
    
    /**
     * Teleport player to warp
     */
    public void teleportToWarp(ServerPlayer player, String warpName) {
        TeleportLocation warp = getWarp(warpName);
        NeoLog.debug(LOGGER, LogCategory.TELEPORTATION, "teleportToWarp request: player={} warpName={}",
            player.getName().getString(), warpName);

        if (warp == null) {
            NeoLog.debug(LOGGER, LogCategory.TELEPORTATION, "teleportToWarp: warp '{}' not found", warpName);
            player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.warp.not_found", warpName));
            return;
        }

        // Check warp-use cooldown (read-only) — only actually consumed once the teleport
        // genuinely proceeds (see below). Checked before maxTeleportDistance so a too-far
        // warp attempt (blocked below) doesn't cost the cooldown for a teleport that never
        // happened, but the actual timestamp update is deferred past both checks.
        boolean bypassCooldown = com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(player.getUUID(), "neoessentials.teleport.bypass.cooldown")
            || com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(player.getUUID(), "neoessentials.teleport.warp.bypass.cooldown");
        UUID playerId = player.getUUID();
        if (warpUseCooldown > 0 && !bypassCooldown) {
            Long lastUse = lastWarpUseTimestamps.get(playerId);
            if (lastUse != null) {
                long elapsed = (System.currentTimeMillis() - lastUse) / 1000L;
                if (elapsed < warpUseCooldown) {
                    long wait = warpUseCooldown - elapsed;
                    NeoLog.debug(LOGGER, LogCategory.TELEPORTATION, "teleportToWarp: player {} blocked by use cooldown, {}s remaining", player.getName().getString(), wait);
                    player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.warp.cooldown", wait));
                    return;
                }
            }
        }

        // Enforce maxTeleportDistance if set in config
        int maxDistance = com.zerog.neoessentials.config.ConfigManager.getInstance().getMaxTeleportDistance();
        if (maxDistance > 0) {
            com.zerog.neoessentials.teleportation.TeleportLocation fromLoc = new com.zerog.neoessentials.teleportation.TeleportLocation(player);
            if (fromLoc.getWorldName().equals(warp.getWorldName())) {
                double dist = fromLoc.distanceTo(warp);
                if (dist > maxDistance) {
                    NeoLog.debug(LOGGER, LogCategory.TELEPORTATION, "teleportToWarp: player {} blocked, distance {} exceeds max {}", player.getName().getString(), dist, maxDistance);
                    player.sendSystemMessage(com.zerog.neoessentials.util.MessageUtil.error("commands.neoessentials.teleport.warp.distance_exceeded", maxDistance));
                    return;
                }
            }
        }

        // Past the checks that reject the teleport outright — commit the cooldown now (the
        // remaining unsafe-location-with-no-fallback path below is a rare edge case, same
        // tradeoff already made for the equivalent case in HomeManager.teleportToHome()).
        if (warpUseCooldown > 0 && !bypassCooldown) {
            lastWarpUseTimestamps.put(playerId, System.currentTimeMillis());
        }

        // Check if warp location is still safe - read from config dynamically
        boolean requireSafe = true; // Default to true for safety
        try {
            JsonObject config = ConfigManager.getInstance().getConfig(ConfigManager.MAIN_CONFIG);
            if (config.has("teleportation")) {
                JsonObject tp = config.getAsJsonObject("teleportation");
                if (tp.has("warpSettings")) {
                    JsonObject warpSettings = tp.getAsJsonObject("warpSettings");
                    if (warpSettings.has("enableWarpSafety")) {
                        requireSafe = warpSettings.get("enableWarpSafety").getAsBoolean();
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to read warp safety config, defaulting to enabled: {}", e.getMessage());
        }

        if (requireSafe && !warp.isSafe()) {
            TeleportLocation safeLocation = warp.findSafeLocation();
            if (safeLocation == null) {
                NeoLog.debug(LOGGER, LogCategory.TELEPORTATION, "teleportToWarp: warp '{}' is unsafe and no safe location found, teleport blocked", warpName);
                player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.warp.unsafe", warpName));
                return;
            }

            // Update warp to safe location
            String normalizedName = caseSensitiveNames ? warpName : warpName.toLowerCase();
            warps.put(normalizedName, safeLocation);
            saveWarps();
            warp = safeLocation;

            NeoLog.debug(LOGGER, LogCategory.TELEPORTATION, "teleportToWarp: warp '{}' moved to safe location {}", warpName, safeLocation.getLocationString());
            player.sendSystemMessage(MessageUtil.warning("commands.neoessentials.teleport.warp.moved_to_safety", warpName));
        }
        
        // Save current location for /back command
        com.zerog.neoessentials.teleportation.Misc.MiscTeleportManager.getInstance().saveBackLocation(player);

        // Show warmup countdown message if delay is configured and warmup messages are enabled
        // Players with warmup bypass permission teleport instantly
        boolean bypassWarmup = com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(player.getUUID(), "neoessentials.teleport.bypass.warmup")
            || com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(player.getUUID(), "neoessentials.teleport.warp.bypass.warmup");
        int delayTicks = bypassWarmup ? 0 : teleportDelay * 20;
        if (delayTicks > 0) {
            boolean showWarmup = true;
            try {
                JsonObject generalSettings = ConfigManager.getInstance()
                    .getConfig(ConfigManager.MAIN_CONFIG)
                    .getAsJsonObject("teleportation").getAsJsonObject("generalSettings");
                if (generalSettings.has("enableTeleportWarmup")) {
                    showWarmup = generalSettings.get("enableTeleportWarmup").getAsBoolean();
                }
            } catch (Exception e) {
                NeoLog.debug(LOGGER, LogCategory.TELEPORTATION,
                    "Failed to read generalSettings.enableTeleportWarmup, defaulting to shown", e);
            }
            if (showWarmup) {
                player.sendSystemMessage(MessageUtil.info("commands.neoessentials.teleport.warp.warmup", warpName, teleportDelay));
            }
        }

        // Perform teleportation — safety already resolved above, so pass findSafe=false
        TeleportLocation finalWarp = warp;
        TeleportUtil.teleportPlayer(player, finalWarp, delayTicks, false).thenAccept(result -> {
            if (result.isSuccess()) {
                player.sendSystemMessage(MessageUtil.success("commands.neoessentials.teleport.warp.success", warpName));
                NeoLog.debug(LOGGER, LogCategory.TELEPORTATION, "Player {} successfully teleported to warp '{}' at {}",
                    player.getName().getString(), warpName, finalWarp.getLocationString());
                NeoLog.info(LOGGER, LogCategory.TELEPORTATION, "Player {} teleported to warp '{}'", player.getName().getString(), warpName);
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
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
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
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean isOverworld(TeleportLocation location) {
        return location.getWorldName().contains("overworld");
    }
    
    /**
     * Load warps from the active {@link com.zerog.neoessentials.storage.DataStore}.
     */
    private void loadWarps() {
        try {
            for (Map.Entry<String, JsonObject> entry : store.getAll(WARPS_COLLECTION).entrySet()) {
                if (entry.getKey().equals(CONFIG_RECORD_ID)) {
                    JsonObject config = entry.getValue();
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
                    continue;
                }
                try {
                    TeleportLocation location = TeleportLocation.fromJson(entry.getValue());
                    if (location != null) {
                        warps.put(entry.getKey(), location);
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to load warp '{}': {}", entry.getKey(), e.getMessage());
                }
            }

            NeoLog.info(LOGGER, LogCategory.TELEPORTATION, "Loaded {} warps", warps.size());

        } catch (Exception e) {
            LOGGER.error("Failed to load warps from storage", e);
        }
    }

    /**
     * Save warps (and the warp settings blob, under {@link #CONFIG_RECORD_ID}) to the
     * active DataStore. Also prunes any store records for warps that no longer exist in
     * memory (e.g. after a delete), matching the old full-file-rewrite semantics.
     */
    private void saveWarps() {
        try {
            for (Map.Entry<String, TeleportLocation> entry : warps.entrySet()) {
                store.put(WARPS_COLLECTION, entry.getKey(), entry.getValue().toJson());
            }
            for (String existingId : store.getAll(WARPS_COLLECTION).keySet()) {
                if (!existingId.equals(CONFIG_RECORD_ID) && !warps.containsKey(existingId)) {
                    store.delete(WARPS_COLLECTION, existingId);
                }
            }

            JsonObject config = new JsonObject();
            config.addProperty("teleportDelay", teleportDelay);
            config.addProperty("requireSafeLocations", requireSafeLocations);
            config.addProperty("allowOverworldOnly", allowOverworldOnly);
            config.addProperty("maxWarps", maxWarps);
            config.addProperty("caseSensitiveNames", caseSensitiveNames);
            store.put(WARPS_COLLECTION, CONFIG_RECORD_ID, config);

        } catch (Exception e) {
            LOGGER.error("Failed to save warps to storage", e);
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
        NeoLog.info(LOGGER, LogCategory.TELEPORTATION, "Cleared all warps");
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
        NeoLog.info(LOGGER, LogCategory.TELEPORTATION, "Reloading warp system...");
        loadConfig();
        warps.clear();
        playerWarps.clear();
        loadWarps();
        loadPlayerWarps();
        NeoLog.info(LOGGER, LogCategory.TELEPORTATION, "Warp system reloaded: {} warps, {} player warps loaded", warps.size(),
            playerWarps.values().stream().mapToInt(Map::size).sum());
    }
}