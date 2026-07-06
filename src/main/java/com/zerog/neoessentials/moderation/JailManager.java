package com.zerog.neoessentials.moderation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player jail system with persistent storage
 */
public class JailManager {
    private static boolean jailSystemEnabledCache = true;
    private static final Logger LOGGER = LoggerFactory.getLogger(JailManager.class);
    private static JailManager instance;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String JAIL_COLLECTION = "jails";
    private static final String JAIL_LOCATION_COLLECTION = "jail_locations";
    private final com.zerog.neoessentials.storage.DataStore store;
    // Track number of times each player has been jailed
    private final Map<UUID, Integer> jailCounts = new ConcurrentHashMap<>();
    
    // In-memory cache for quick lookups
    private final Map<UUID, JailEntry> jailedPlayers = new ConcurrentHashMap<>();
    private final Map<String, JailLocation> jailLocations = new ConcurrentHashMap<>();
    
    public static class JailEntry {
        public String playerName;
        public UUID playerId;
        public String reason;
        public String jailedBy;
        public long jailTime;
        public long expireAt;   // 0 = indefinite (Essentials: checkJailTimeout)
        public String jailName;
        public BlockPos originalLocation;
        public String originalDimension;
        
        public JailEntry(String playerName, UUID playerId, String reason, String jailedBy, String jailName) {
            this.playerName = playerName;
            this.playerId = playerId;
            this.reason = reason;
            this.jailedBy = jailedBy;
            this.jailName = jailName;
            this.jailTime = System.currentTimeMillis();
            this.expireAt = 0L;
        }

        /** Returns true if this is a timed jail that has now expired. */
        public boolean isExpired() {
            return expireAt > 0 && System.currentTimeMillis() >= expireAt;
        }

        /** Formatted remaining time string, or "indefinite". */
        public String getFormattedRemaining() {
            if (expireAt <= 0) return "indefinite";
            long remaining = expireAt - System.currentTimeMillis();
            if (remaining <= 0) return "expired";
            return formatDuration(remaining);
        }
        
        public String getFormattedJailTime() {
            return formatTime(jailTime);
        }
    }
    
    public static class JailLocation {
        public String name;
        public BlockPos position;
        public String dimension;
        public String createdBy;
        public long createdTime;
        
        public JailLocation(String name, BlockPos position, String dimension, String createdBy) {
            this.name = name;
            this.position = position;
            this.dimension = dimension;
            this.createdBy = createdBy;
            this.createdTime = System.currentTimeMillis();
        }
        
        public String getFormattedCreatedTime() {
            return formatTime(createdTime);
        }
    }
    
    private JailManager() {
        // Check config for jail system enabled
        jailSystemEnabledCache = com.zerog.neoessentials.config.ConfigManager.isJailSystemEnabled();
        if (!jailSystemEnabledCache) {
            LOGGER.info("Jail system is disabled via config. All jail features will be inactive.");
        }

        this.store = com.zerog.neoessentials.storage.StorageManager.getInstance().getStore();
        migrateLegacyFilesIfNeeded();
        loadData();
    }

    public static boolean isJailSystemEnabled() {
        return jailSystemEnabledCache;
    }
    
    public static JailManager getInstance() {
        if (instance == null) {
            instance = new JailManager();
        }
        return instance;
    }

    /**
     * Best-effort direct notice to whoever issued the /jail(for) that just triggered an
     * auto-ban — previously this only reached a LOGGER.info line (gated behind
     * isLogJailActionsEnabled), completely invisible to the admin who ran the command unless
     * they went looking in the server log afterward. No-ops silently if {@code jailedBy}
     * isn't a currently-online player (e.g. "Console", or the admin has since logged off) —
     * the log line is still the fallback of record for those cases.
     */
    private void notifyJailer(String jailedBy, net.minecraft.network.chat.Component message) {
        if (jailedBy == null) return;
        MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        ServerPlayer jailer = server.getPlayerList().getPlayerByName(jailedBy);
        if (jailer != null) {
            jailer.sendSystemMessage(message);
        }
    }

    /**
     * Jail a player indefinitely (no expiry).
     */
    public boolean jailPlayer(String playerName, UUID playerId, String reason, String jailedBy, String jailName) {
        return jailPlayer(playerName, playerId, reason, jailedBy, jailName, 0L);
    }

    /**
     * Jail a player with an optional timed duration (millis). 0 = indefinite.
     * Ported from Essentials: checkJailTimeout pattern.
     */
    public boolean jailPlayer(String playerName, UUID playerId, String reason, String jailedBy, String jailName, long durationMillis) {
        // Enforce config: check maxJailReason
        int maxReason = com.zerog.neoessentials.config.ConfigManager.getInstance().getMaxJailReasonLength();
        if (reason != null && reason.length() > maxReason) {
            LOGGER.warn("Jail reason too long ({} > {}). Cannot jail player {}.", reason.length(), maxReason, playerName);
            return false;
        }
        // Check if already jailed atomically using putIfAbsent
        if (jailedPlayers.putIfAbsent(playerId, null) != null) {
            // Already jailed
            return false;
        }

        JailLocation jailLoc = jailLocations.get(jailName);
        if (jailLoc == null) {
            jailedPlayers.remove(playerId, null); // Clean up
            return false; // Jail doesn't exist
        }

        // Track jail count ATOMICALLY using compute
        int jailCount = jailCounts.compute(playerId, (id, count) -> {
            return (count == null ? 0 : count) + 1;
        });

        // Check thresholds
        int tempBanThreshold = com.zerog.neoessentials.config.ConfigManager.getMaxJailsBeforeTempBan();
        int permBanThreshold = com.zerog.neoessentials.config.ConfigManager.getMaxJailsBeforePermBan();
        int tempBanDuration = com.zerog.neoessentials.config.ConfigManager.getTempBanDurationMinutes();

        if (jailCount >= permBanThreshold) {
            // Issue permanent ban
            jailedPlayers.remove(playerId, null); // Clean up
            BanManager banManager = BanManager.getInstance();
            banManager.banPlayer(playerName, playerId, "Exceeded maximum jailings (permanent ban)", "System");
            jailCounts.put(playerId, 0); // Reset count
            if (com.zerog.neoessentials.config.ConfigManager.getInstance().isLogJailActionsEnabled()) {
                LOGGER.info("Player {} ({}) permanently banned after {} jailings.", playerName, playerId, jailCount);
            }
            notifyJailer(jailedBy, MessageUtil.error("commands.neoessentials.jail.auto_permban_notice",
                playerName, jailCount));
            return false;
        } else if (jailCount >= tempBanThreshold) {
            // Issue temp ban
            jailedPlayers.remove(playerId, null); // Clean up
            BanManager banManager = BanManager.getInstance();
            banManager.tempBanPlayer(playerName, playerId, "Exceeded maximum jailings (temporary ban)", "System", tempBanDuration * 60 * 1000L);
            if (com.zerog.neoessentials.config.ConfigManager.getInstance().isLogJailActionsEnabled()) {
                LOGGER.info("Player {} ({}) temp-banned for {} minutes after {} jailings.", playerName, playerId, tempBanDuration, jailCount);
            }
            notifyJailer(jailedBy, MessageUtil.error("commands.neoessentials.jail.auto_tempban_notice",
                playerName, jailCount, tempBanDuration));
            return false;
        }

        // Create jail entry
        JailEntry jail = new JailEntry(playerName, playerId, reason, jailedBy, jailName);
        if (durationMillis > 0) {
            jail.expireAt = System.currentTimeMillis() + durationMillis;
        }

        // Store original location
        MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null) {
                jail.originalLocation = player.blockPosition();
                jail.originalDimension = player.level().dimension().identifier().toString();

                // Replace the null placeholder with actual jail entry
                jailedPlayers.put(playerId, jail);
                saveJailedPlayers();

                // Teleport to jail
                teleportToJail(player, jailLoc);

                String message = MessageUtil.localize("neoessentials.moderation.jailed_message", reason, jailedBy);
                player.sendSystemMessage(MessageUtil.warning(message));

        if (com.zerog.neoessentials.config.ConfigManager.getInstance().isLogJailActionsEnabled()) {
            LOGGER.info("Player {} ({}) jailed by {} in {} for: {}", 
                playerName, playerId, jailedBy, jailName, reason);
        }
                return true;
            }
        }

        // Player offline - still record the jail
        jailedPlayers.put(playerId, jail);
        saveJailedPlayers();

    if (com.zerog.neoessentials.config.ConfigManager.getInstance().isLogJailActionsEnabled()) {
        LOGGER.info("Player {} ({}) jailed while offline by {} in {} for: {}", 
            playerName, playerId, jailedBy, jailName, reason);
    }
        return true;
    }
    
    /**
     * Unjail a player
     */
    public boolean unjailPlayer(UUID playerId) {
        JailEntry jail = jailedPlayers.remove(playerId);
        if (jail != null) {
            saveJailedPlayers();
            
            // Teleport back to original location if online
            MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                ServerPlayer player = server.getPlayerList().getPlayer(playerId);
                if (player != null) {
                    if (jail.originalLocation != null && jail.originalDimension != null) {
                        teleportToOriginalLocation(player, jail);
                    }
                    
                    String message = MessageUtil.localize("neoessentials.moderation.unjailed_message");
                    player.sendSystemMessage(MessageUtil.success(message));
                }
            }
            
            if (com.zerog.neoessentials.config.ConfigManager.getInstance().isLogJailActionsEnabled()) {
                LOGGER.info("Player {} ({}) unjailed", jail.playerName, playerId);
            }
            return true;
        }
        return false;
    }
    
    /**
     * Set a jail location
     */
    public boolean setJailLocation(String jailName, BlockPos position, String dimension, String createdBy) {
        JailLocation jail = new JailLocation(jailName, position, dimension, createdBy);
        jailLocations.put(jailName, jail);
        saveJailLocations();
        
        LOGGER.info("Jail location '{}' set at {} in {} by {}", jailName, position, dimension, createdBy);
        return true;
    }
    
    /**
     * Remove a jail location
     */
    public boolean removeJailLocation(String jailName) {
        JailLocation removed = jailLocations.remove(jailName);
        if (removed != null) {
            saveJailLocations();
            LOGGER.info("Jail location '{}' removed", jailName);
            return true;
        }
        return false;
    }
    
    /**
     * Check if a player is jailed
     */
    public boolean isPlayerJailed(UUID playerId) {
        return jailedPlayers.containsKey(playerId);
    }
    
    /**
     * Get jail entry for a player
     */
    public JailEntry getJailEntry(UUID playerId) {
        return jailedPlayers.get(playerId);
    }
    
    /**
     * Get jail location by name
     */
    public JailLocation getJailLocation(String jailName) {
        return jailLocations.get(jailName);
    }
    
    /**
     * Get all jailed players
     */
    public List<JailEntry> getAllJailedPlayers() {
        return new ArrayList<>(jailedPlayers.values());
    }
    
    /**
     * Get all jail locations
     */
    public List<JailLocation> getAllJailLocations() {
        return new ArrayList<>(jailLocations.values());
    }
    
    /**
     * Check if player can move (not jailed or within jail bounds)
     */
    public boolean canPlayerMove(ServerPlayer player, BlockPos newPos) {
        UUID playerId = player.getUUID();
        if (!isPlayerJailed(playerId)) {
            return true; // Not jailed, can move freely
        }
        
        JailEntry jail = getJailEntry(playerId);
        if (jail == null) {
            return true;
        }
        
        JailLocation jailLoc = getJailLocation(jail.jailName);
        if (jailLoc == null) {
            return true; // Jail doesn't exist anymore
        }

        // Dimension check: the old distSqr-only check compared raw block coordinates with no
        // regard for which dimension the player is actually in. A jailed player who reached a
        // different dimension (e.g. via an unblocked teleport command) but happened to be near
        // the jail's raw XYZ in THAT dimension was incorrectly treated as "in bounds" and never
        // redirected back — the cell only really exists in jailLoc.dimension.
        if (jailLoc.dimension != null && !jailLoc.dimension.isEmpty()) {
            String currentDimension = player.level().dimension().location().toString();
            if (!jailLoc.dimension.equals(currentDimension)) {
                return false;
            }
        }

        // Check if within jail bounds (simple distance check)
        double distance = newPos.distSqr(jailLoc.position);
        return distance <= 100; // 10 block radius squared
    }
    
    /**
     * Handle player join - teleport to jail if jailed
     */
    public void onPlayerJoin(ServerPlayer player) {
        UUID playerId = player.getUUID();
        if (!isPlayerJailed(playerId)) {
            return;
        }

        JailEntry jail = getJailEntry(playerId);
        if (jail == null) {
            return;
        }

        JailLocation jailLoc = getJailLocation(jail.jailName);
        if (jailLoc == null) {
            // Jail doesn't exist anymore, unjail player
            unjailPlayer(playerId);
            return;
        }

        boolean teleportOnLogin = com.zerog.neoessentials.config.ConfigManager.getInstance().isJailTeleportOnLoginEnabled();
        if (teleportOnLogin) {
            teleportToJail(player, jailLoc);
            String message = MessageUtil.localize("neoessentials.moderation.jail_reminder", jail.reason);
            player.sendSystemMessage(MessageUtil.warning(message));
        }
    }
    
    /**
     * Check if a player's timed jail has expired and release them if so.
     * Called on player join (Essentials: user.checkJailTimeout(currentTime)) and periodically.
     *
     * @return true if the player was released due to expiry
     */
    public boolean checkJailTimeout(UUID playerId) {
        JailEntry jail = jailedPlayers.get(playerId);
        if (jail == null) return false;
        if (!jail.isExpired()) return false;

        LOGGER.info("Timed jail expired for player {} ({}). Auto-releasing.", jail.playerName, playerId);
        unjailPlayer(playerId);
        return true;
    }

    /**
     * Format a duration in milliseconds into a human-readable string (e.g. "2h 30m 15s").
     */
    public static String formatDuration(long millis) {
        if (millis <= 0) return "0s";
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours   = minutes / 60;
        long days    = hours / 24;
        seconds %= 60; minutes %= 60; hours %= 24;
        StringBuilder sb = new StringBuilder();
        if (days > 0)    sb.append(days).append("d ");
        if (hours > 0)   sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0 || sb.length() == 0) sb.append(seconds).append("s");
        return sb.toString().trim();
    }

    /**
     * Teleport player to jail
     */
    private void teleportToJail(ServerPlayer player, JailLocation jailLoc) {
        try {
            MinecraftServer server = player.level().getServer();
            if (server == null) return;
            
            // Get the dimension from jail location or default to overworld
            ResourceKey<Level> dimensionKey = Level.OVERWORLD; // Default
            if (jailLoc.dimension != null && !jailLoc.dimension.isEmpty()) {
                try {
                    Identifier dimensionId = Identifier.tryParse(jailLoc.dimension);
                    if (dimensionId != null) {
                        dimensionKey = ResourceKey.create(Registries.DIMENSION, dimensionId);
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to parse jail dimension '{}', defaulting to overworld", jailLoc.dimension);
                }
            }
            ServerLevel dimension = server.getLevel(dimensionKey);
            
            if (dimension != null) {
                player.teleportTo(
                dimension,
                jailLoc.position.getX() + 0.5,
                jailLoc.position.getY() + 1,
                jailLoc.position.getZ() + 0.5,
                java.util.Set.of(),
                player.getYRot(),
                player.getXRot(),
                true
            );
            }
        } catch (Exception e) {
            LOGGER.error("Failed to teleport player {} to jail {}", player.getName().getString(), jailLoc.name, e);
        }
    }
    
    /**
     * Teleport player back to original location
     */
    private void teleportToOriginalLocation(ServerPlayer player, JailEntry jail) {
        try {
            MinecraftServer server = player.level().getServer();
            if (server == null || jail.originalLocation == null) return;
            
            // Get the dimension from original location or default to overworld
            ResourceKey<Level> dimensionKey = Level.OVERWORLD; // Default
            if (jail.originalDimension != null && !jail.originalDimension.isEmpty()) {
                try {
                    Identifier dimensionId = Identifier.tryParse(jail.originalDimension);
                    if (dimensionId != null) {
                        dimensionKey = ResourceKey.create(Registries.DIMENSION, dimensionId);
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to parse original dimension '{}', defaulting to overworld", jail.originalDimension);
                }
            }
            ServerLevel dimension = server.getLevel(dimensionKey);
            
            if (dimension != null) {
                player.teleportTo(
                dimension,
                jail.originalLocation.getX() + 0.5,
                jail.originalLocation.getY() + 1,
                jail.originalLocation.getZ() + 0.5,
                java.util.Set.of(),
                player.getYRot(),
                player.getXRot(),
                true
            );
            }
        } catch (Exception e) {
            LOGGER.error("Failed to teleport player {} back to original location", player.getName().getString(), e);
        }
    }
    
    /**
     * Format timestamp to readable string
     */
    private static String formatTime(long timestamp) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
    
    /**
     * Load data from files
     */
    private void loadData() {
        loadJailedPlayers();
        loadJailLocations();
    }

    private void loadJailedPlayers() {
        for (JsonObject jailObj : store.getAll(JAIL_COLLECTION).values()) {
            JailEntry jail = new JailEntry(
                jailObj.get("playerName").getAsString(),
                UUID.fromString(jailObj.get("playerId").getAsString()),
                jailObj.get("reason").getAsString(),
                jailObj.get("jailedBy").getAsString(),
                jailObj.get("jailName").getAsString()
            );
            jail.jailTime = jailObj.get("jailTime").getAsLong();
            jail.expireAt = jailObj.has("expireAt") ? jailObj.get("expireAt").getAsLong() : 0L;

            if (jailObj.has("originalLocation")) {
                JsonObject locObj = jailObj.getAsJsonObject("originalLocation");
                jail.originalLocation = new BlockPos(
                    locObj.get("x").getAsInt(),
                    locObj.get("y").getAsInt(),
                    locObj.get("z").getAsInt()
                );
            }

            if (jailObj.has("originalDimension") && !jailObj.get("originalDimension").isJsonNull()) {
                jail.originalDimension = jailObj.get("originalDimension").getAsString();
            }

            jailedPlayers.put(jail.playerId, jail);
        }
    }

    private void loadJailLocations() {
        for (JsonObject jailObj : store.getAll(JAIL_LOCATION_COLLECTION).values()) {
            JsonObject posObj = jailObj.getAsJsonObject("position");
            BlockPos position = new BlockPos(
                posObj.get("x").getAsInt(),
                posObj.get("y").getAsInt(),
                posObj.get("z").getAsInt()
            );

            JailLocation jail = new JailLocation(
                jailObj.get("name").getAsString(),
                position,
                jailObj.get("dimension").getAsString(),
                jailObj.get("createdBy").getAsString()
            );
            jail.createdTime = jailObj.get("createdTime").getAsLong();

            jailLocations.put(jail.name, jail);
        }
    }

    private JsonObject jailedPlayerToJson(JailEntry jail) {
        JsonObject jailObj = new JsonObject();
        jailObj.addProperty("playerName", jail.playerName);
        jailObj.addProperty("playerId", jail.playerId.toString());
        jailObj.addProperty("reason", jail.reason);
        jailObj.addProperty("jailedBy", jail.jailedBy);
        jailObj.addProperty("jailName", jail.jailName);
        jailObj.addProperty("jailTime", jail.jailTime);
        jailObj.addProperty("expireAt", jail.expireAt);

        if (jail.originalLocation != null) {
            JsonObject locObj = new JsonObject();
            locObj.addProperty("x", jail.originalLocation.getX());
            locObj.addProperty("y", jail.originalLocation.getY());
            locObj.addProperty("z", jail.originalLocation.getZ());
            jailObj.add("originalLocation", locObj);
        }

        if (jail.originalDimension != null) {
            jailObj.addProperty("originalDimension", jail.originalDimension);
        }
        return jailObj;
    }

    private JsonObject jailLocationToJson(JailLocation jail) {
        JsonObject jailObj = new JsonObject();
        jailObj.addProperty("name", jail.name);
        jailObj.addProperty("dimension", jail.dimension);
        jailObj.addProperty("createdBy", jail.createdBy);
        jailObj.addProperty("createdTime", jail.createdTime);

        JsonObject posObj = new JsonObject();
        posObj.addProperty("x", jail.position.getX());
        posObj.addProperty("y", jail.position.getY());
        posObj.addProperty("z", jail.position.getZ());
        jailObj.add("position", posObj);

        return jailObj;
    }

    /**
     * Persist the full set of currently jailed players to the active DataStore
     * (collection {@link #JAIL_COLLECTION}, id = player UUID string). Since jailed
     * players are a small, bounded set, each call rewrites every active jail entry —
     * simplest way to also implicitly delete entries for players removed from the
     * in-memory map (e.g. via unjailPlayer()) since the last save.
     */
    private void saveJailedPlayers() {
        for (JailEntry jail : jailedPlayers.values()) {
            store.put(JAIL_COLLECTION, jail.playerId.toString(), jailedPlayerToJson(jail));
        }
        for (JsonObject existing : store.getAll(JAIL_COLLECTION).values()) {
            String id = existing.get("playerId").getAsString();
            if (!jailedPlayers.containsKey(UUID.fromString(id))) {
                store.delete(JAIL_COLLECTION, id);
            }
        }
    }

    /**
     * Persist the full set of jail locations to the active DataStore
     * (collection {@link #JAIL_LOCATION_COLLECTION}, id = jail name). See
     * {@link #saveJailedPlayers()} for why this rewrites the whole set each call.
     */
    private void saveJailLocations() {
        for (JailLocation jail : jailLocations.values()) {
            store.put(JAIL_LOCATION_COLLECTION, jail.name, jailLocationToJson(jail));
        }
        for (String existingName : store.getAll(JAIL_LOCATION_COLLECTION).keySet()) {
            if (!jailLocations.containsKey(existingName)) {
                store.delete(JAIL_LOCATION_COLLECTION, existingName);
            }
        }
    }

    /**
     * One-time import of the legacy jailed_players.json / jail_locations.json files into
     * the active DataStore, if it's still empty and storage.autoMigrate is enabled.
     */
    private void migrateLegacyFilesIfNeeded() {
        if (store.hasAnyData(JAIL_COLLECTION) || store.hasAnyData(JAIL_LOCATION_COLLECTION)) return;
        if (!com.zerog.neoessentials.config.ConfigManager.getInstance().isStorageAutoMigrateEnabled()) return;

        int migrated = 0;
        migrated += migrateLegacyJailedPlayersFile();
        migrated += migrateLegacyJailLocationsFile();

        if (migrated > 0) {
            LOGGER.info("JailManager: migrated {} record(s) from legacy files into the '{}' storage backend.",
                migrated, com.zerog.neoessentials.storage.StorageManager.getInstance().getActiveType());
        }
    }

    private int migrateLegacyJailedPlayersFile() {
        File file = new File(com.zerog.neoessentials.util.ResourceUtil.DATA_DIR + "moderation", "jailed_players.json");
        if (!file.exists()) return 0;

        int count = 0;
        try (FileReader reader = new FileReader(file)) {
            JsonObject root = gson.fromJson(reader, JsonObject.class);
            if (root == null || !root.has("jailed")) return 0;
            for (JsonElement element : root.getAsJsonArray("jailed")) {
                JsonObject obj = element.getAsJsonObject().deepCopy();
                String id = obj.get("playerId").getAsString();
                store.put(JAIL_COLLECTION, id, obj);
                count++;
            }
        } catch (IOException e) {
            LOGGER.error("Failed to migrate legacy jailed_players.json: {}", e.getMessage());
        }
        return count;
    }

    private int migrateLegacyJailLocationsFile() {
        File file = new File(com.zerog.neoessentials.util.ResourceUtil.DATA_DIR + "moderation", "jail_locations.json");
        if (!file.exists()) return 0;

        int count = 0;
        try (FileReader reader = new FileReader(file)) {
            JsonObject root = gson.fromJson(reader, JsonObject.class);
            if (root == null || !root.has("jails")) return 0;
            for (JsonElement element : root.getAsJsonArray("jails")) {
                JsonObject obj = element.getAsJsonObject().deepCopy();
                String id = obj.get("name").getAsString();
                store.put(JAIL_LOCATION_COLLECTION, id, obj);
                count++;
            }
        } catch (IOException e) {
            LOGGER.error("Failed to migrate legacy jail_locations.json: {}", e.getMessage());
        }
        return count;
    }

    /**
     * Reload jail data from the active DataStore.
     */
    public void reload() {
        LOGGER.info("Reloading jail system...");
        jailedPlayers.clear();
        jailLocations.clear();
        jailCounts.clear();
        loadJailedPlayers();
        loadJailLocations();
        LOGGER.info("Jail system reloaded: {} jailed players, {} jail locations",
            jailedPlayers.size(), jailLocations.size());
    }
}