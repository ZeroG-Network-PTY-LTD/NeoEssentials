package com.zerog.neoessentials.chat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import com.zerog.neoessentials.util.MessageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;

import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.io.File;
import java.io.FileReader;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Centralized, thread-safe manager for AFK status and activity tracking.
 * This is the single source of truth for all AFK-related functionality.
 */
@SuppressWarnings({"unused", "FieldCanBeLocal", "FieldMayBeFinal"}) // Public API class with configuration fields
public class AfkManager {
    // Configurable auto-save for AFK data
    private boolean autoSave = true;
    private int saveIntervalSeconds = 60;
    private static final Logger LOGGER = LoggerFactory.getLogger(AfkManager.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    
    // Thread-safe singleton using Bill Pugh pattern
    private static class SingletonHelper {
        private static final AfkManager INSTANCE = new AfkManager();
    }
    
    public static AfkManager getInstance() {
        return SingletonHelper.INSTANCE;
    }
    
    // Player AFK data storage
    private final Map<UUID, PlayerAfkData> playerData = new ConcurrentHashMap<>();
    
    // Scheduled executor for automatic AFK detection
    // Use daemon thread to prevent blocking JVM shutdown
    private final ScheduledExecutorService afkCheckExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "AfkManager-Check");
        t.setDaemon(true); // CRITICAL: Set as daemon to allow JVM shutdown
        return t;
    });

    // Shutdown flag to prevent task submission after shutdown
    private volatile boolean isShuttingDown = false;

    // Data persistence
    private final File afkDataFile = new File(com.zerog.neoessentials.util.ResourceUtil.DATA_DIR + "afk_data.json");
    private static final String COLLECTION = "afk_data";
    private final com.zerog.neoessentials.storage.DataStore store =
        com.zerog.neoessentials.storage.StorageManager.getInstance().getStore();

    // AFK configuration (will be loaded from config)
    private long afkTimeoutMs = 300000; // 5 minutes default
    private boolean autoAfkEnabled = true;
    private boolean broadcastAfkMessages = true;
    private boolean broadcastReturnMessages = true;
    private boolean kickAfkPlayers = false;
    private long afkKickTimeoutMs = 1800000; // 30 minutes default
    private String afkMessage = "{player} is now AFK";
    private String returnMessage = "{player} is no longer AFK";
    private String afkKickMessage = null;
    
    // Additional configuration fields
    private boolean ignoreAfkInSleep = true;
    private boolean enableTablistIndicator = true;
    private String tablistAfkPrefix = "[AFK] ";
    private String tablistAfkSuffix = "";
    private boolean enableActivityTracking = true;
    private boolean trackMovement = true;
    private boolean trackChat = true;
    private boolean trackCommands = true;
    private boolean trackInteractions = true;

    // Configurable rotation threshold for AFK detection
    private double rotationThreshold = 5.0;

    // Configurable movement distance threshold for AFK detection
    private double movementThreshold = 0.1;

    // Whether AFK players are invulnerable to all damage
    private boolean invulnerableWhenAfk = false;

    // Configurable excluded commands for AFK activity tracking
    private java.util.Set<String> excludedCommands = new java.util.HashSet<>(java.util.List.of(
        "afk", "list", "who", "tps", "ping", "help", "?"
    ));
    
    private AfkManager() {
    migrateLegacyFileIfNeeded();
    loadAfkData();
    startAfkCheckTask();
    startAutoSaveTask();
    }
    
    /**
     * Player AFK data container
     */
    public static class PlayerAfkData {
        private boolean isAfk = false;
        private long lastActivity = System.currentTimeMillis();
        private String afkReason = null;
        private long afkStartTime = 0;
        
        // Getters and setters
        public boolean isAfk() { return isAfk; }
        public void setAfk(boolean afk) { this.isAfk = afk; }
        public long getLastActivity() { return lastActivity; }
        public void setLastActivity(long time) { this.lastActivity = time; }
        public String getAfkReason() { return afkReason; }
        public void setAfkReason(String reason) { this.afkReason = reason; }
        public long getAfkStartTime() { return afkStartTime; }
        public void setAfkStartTime(long time) { this.afkStartTime = time; }
    }
    
    /**
     * Update player activity timestamp
     */
    public void updateActivity(UUID playerUuid) {
        PlayerAfkData data = playerData.computeIfAbsent(playerUuid, k -> new PlayerAfkData());
        data.setLastActivity(System.currentTimeMillis());
        
        // If player was AFK, mark them as returned
        if (data.isAfk()) {
            setAfkStatus(playerUuid, false, null);
        }
    }
    
    /**
     * Check if player is AFK
     */
    public boolean isAfk(UUID playerUuid) {
        PlayerAfkData data = playerData.get(playerUuid);
        return data != null && data.isAfk();
    }
    
    /**
     * Check if player is AFK (convenience method for ServerPlayer)
     */
    public boolean isAfk(ServerPlayer player) {
        return isAfk(player.getUUID());
    }
    
    /**
     * Toggle AFK status for player
     */
    public void toggleAfk(ServerPlayer player, String reason) {
        UUID uuid = player.getUUID();
        PlayerAfkData data = playerData.computeIfAbsent(uuid, k -> new PlayerAfkData());
        setAfkStatus(uuid, !data.isAfk(), reason);
    }
    
    /**
     * Set AFK status for player
     */
    public void setAfkStatus(UUID playerUuid, boolean afk, String reason) {
        PlayerAfkData data = playerData.computeIfAbsent(playerUuid, k -> new PlayerAfkData());
        boolean wasAfk = data.isAfk();

        data.setAfk(afk);
        data.setAfkReason(reason);

        if (afk && !wasAfk) {
            data.setAfkStartTime(System.currentTimeMillis());
            onPlayerGoAfk(playerUuid, reason);
            // Notify Discord integrations
            try {
                net.minecraft.server.MinecraftServer srv = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
                if (srv != null) {
                    ServerPlayer p = srv.getPlayerList().getPlayer(playerUuid);
                    if (p != null) com.zerog.neoessentials.integrations.ChatIntegrationManager.broadcastAfkEvent(p, true, reason);
                }
            } catch (Exception ignored) {}
        } else if (!afk && wasAfk) {
            data.setAfkStartTime(0);
            data.setLastActivity(System.currentTimeMillis());
            onPlayerReturnFromAfk(playerUuid);
            // Notify Discord integrations
            try {
                net.minecraft.server.MinecraftServer srv = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
                if (srv != null) {
                    ServerPlayer p = srv.getPlayerList().getPlayer(playerUuid);
                    if (p != null) com.zerog.neoessentials.integrations.ChatIntegrationManager.broadcastAfkEvent(p, false, null);
                }
            } catch (Exception ignored) {}
        }

        queueSaveAfkData();
    }
    
    /**
     * Get AFK reason for player
     */
    public String getAfkReason(UUID playerUuid) {
        PlayerAfkData data = playerData.get(playerUuid);
        return data != null ? data.getAfkReason() : null;
    }
    
    /**
     * Get time since player went AFK (in milliseconds)
     */
    public long getAfkDuration(UUID playerUuid) {
        PlayerAfkData data = playerData.get(playerUuid);
        if (data == null || !data.isAfk()) return 0;
        return System.currentTimeMillis() - data.getAfkStartTime();
    }
    
    /**
     * Called when player goes AFK
     */
    private void onPlayerGoAfk(UUID playerUuid, String reason) {
        if (!broadcastAfkMessages) return;
        
        try {
            // Get player reference
            net.minecraft.server.MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server == null) return;
            
            ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
            if (player == null) return;
            
            // Create AFK message
            String message = afkMessage.replace("{player}", player.getName().getString());
            if (reason != null && !reason.trim().isEmpty() && !reason.equals("Inactive")) {
                message += " (" + reason + ")";
            }
            
            // Broadcast to all players and log to console
            Component afkComponent = Component.literal("§e" + message);
            server.getPlayerList().broadcastSystemMessage(afkComponent, false);
            server.sendSystemMessage(afkComponent);

            // Send personal confirmation only for auto-AFK (manual toggle sends its own feedback via the command)
            if ("Inactive".equals(reason)) {
                player.sendSystemMessage(MessageUtil.component("commands.neoessentials.afk.auto_afk_notice"));
            }

            NeoLog.info(LOGGER, LogCategory.CHAT, "Player {} went AFK{}", player.getName().getString(), 
                reason != null && !reason.equals("Inactive") ? " (" + reason + ")" : "");

            // Update tablist display
            com.zerog.neoessentials.chat.handlers.AfkTablistHandler.onPlayerAfk(player);
                
        } catch (Exception e) {
            LOGGER.error("Error broadcasting AFK message", e);
        }
    }
    
    /**
     * Called when player returns from AFK
     */
    private void onPlayerReturnFromAfk(UUID playerUuid) {
        if (!broadcastReturnMessages) return;
        
        try {
            // Get player reference
            net.minecraft.server.MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server == null) return;
            
            ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
            if (player == null) return;
            
            // Create return message
            String message = returnMessage.replace("{player}", player.getName().getString());
            
            // Broadcast to all players and log to console
            Component returnComponent = Component.literal("§e" + message);
            server.getPlayerList().broadcastSystemMessage(returnComponent, false);
            server.sendSystemMessage(returnComponent);

            NeoLog.info(LOGGER, LogCategory.CHAT, "Player {} returned from AFK", player.getName().getString());
            
            // Update tablist display
            com.zerog.neoessentials.chat.handlers.AfkTablistHandler.onPlayerReturnFromAfk(player);
            
        } catch (Exception e) {
            LOGGER.error("Error broadcasting return message", e);
        }
    }
    
    /**
     * Start the automatic AFK detection task
     */
    private void startAfkCheckTask() {
        if (!autoAfkEnabled) return;
        
        afkCheckExecutor.scheduleAtFixedRate(() -> {
            try {
                checkForAfkPlayers();
            } catch (Exception e) {
                LOGGER.error("Error in AFK check task", e);
            }
        }, 30, 30, TimeUnit.SECONDS); // Check every 30 seconds
    }
    
    /**
     * Check all players for AFK timeout
     */
    private void checkForAfkPlayers() {
        net.minecraft.server.MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        
        long currentTime = System.currentTimeMillis();
        
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID uuid = player.getUUID();
            PlayerAfkData data = playerData.get(uuid);

            if (data == null) continue;

            // Check if player should be marked AFK due to inactivity
            if (!data.isAfk() && (currentTime - data.getLastActivity()) > afkTimeoutMs) {
                setAfkStatus(uuid, true, "Inactive");
            }

            // Check if AFK player should be kicked (only if kick timeout is > 0 and feature is enabled)
            if (kickAfkPlayers && afkKickTimeoutMs > 0 && data.isAfk()) {
                // Skip players with the kick-exempt permission
                if (com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(uuid, "neoessentials.afk.exempt")) {
                    continue;
                }

                long afkDuration = currentTime - data.getAfkStartTime();

                // Warn player 60 seconds before kick
                if (afkDuration > (afkKickTimeoutMs - 60000) && afkDuration < afkKickTimeoutMs) {
                    long secondsUntilKick = (afkKickTimeoutMs - afkDuration) / 1000;
                    if (secondsUntilKick > 0 && secondsUntilKick <= 60) {
                        player.sendSystemMessage(MessageUtil.component(
                            "commands.neoessentials.afk.kick_warning", secondsUntilKick
                        ));
                        LOGGER.warn("Player {} will be kicked for AFK in {} seconds", player.getName().getString(), secondsUntilKick);
                    }
                }

                // Kick if timeout exceeded
                if (afkDuration > afkKickTimeoutMs) {
                    try {
                        String kickMsg = this.afkKickMessage != null && !this.afkKickMessage.isEmpty()
                            ? this.afkKickMessage
                            : MessageUtil.localize("commands.neoessentials.afk.kick_message", afkDuration / 60000);
                        player.connection.disconnect(Component.literal(kickMsg));
                        NeoLog.info(LOGGER, LogCategory.CHAT, "Kicked player {} for being AFK too long (AFK for {} minutes)",
                            player.getName().getString(), afkDuration / 60000);
                    } catch (Exception e) {
                        LOGGER.error("Error kicking AFK player {}", player.getName().getString(), e);
                    }
                }
            }
        }
    }
    
    /**
     * Load AFK configuration from config manager
     */
    public void loadConfiguration(JsonObject afkConfig) {
        // Support autoSave from config
        if (afkConfig.has("autoSave")) {
            this.autoSave = afkConfig.get("autoSave").getAsBoolean();
        } else {
            this.autoSave = true;
        }

        // Support saveInterval from config
        if (afkConfig.has("saveInterval")) {
            try {
                this.saveIntervalSeconds = Math.max(10, afkConfig.get("saveInterval").getAsInt());
            } catch (Exception e) {
                this.saveIntervalSeconds = 60;
                LOGGER.warn("Invalid value for saveInterval in config, using default 60s");
            }
        } else {
            this.saveIntervalSeconds = 60;
        }
        
        // Support both 'timeout' (seconds) and 'timeoutMinutes' (minutes) for compatibility
        if (afkConfig.has("timeout")) {
            long timeoutSeconds = afkConfig.get("timeout").getAsLong();
            this.afkTimeoutMs = timeoutSeconds > 0 ? timeoutSeconds * 1000L : 300000L;
        } else if (afkConfig.has("timeoutMinutes")) {
            long timeoutMinutes = afkConfig.get("timeoutMinutes").getAsLong();
            this.afkTimeoutMs = timeoutMinutes > 0 ? timeoutMinutes * 60000 : 300000;
        } else {
            this.afkTimeoutMs = 300000;
        }
        this.autoAfkEnabled = !afkConfig.has("autoAfkEnabled") || 
            afkConfig.get("autoAfkEnabled").getAsBoolean();
        // Use 'broadcastOnAfk' if present, then 'enableafkBroadcasts', then fallback to 'broadcastMessages' for compatibility
        if (afkConfig.has("broadcastOnAfk")) {
            this.broadcastAfkMessages = afkConfig.get("broadcastOnAfk").getAsBoolean();
        } else if (afkConfig.has("enableafkBroadcasts")) {
            this.broadcastAfkMessages = afkConfig.get("enableafkBroadcasts").getAsBoolean();
        } else {
            this.broadcastAfkMessages = !afkConfig.has("broadcastMessages") || 
                afkConfig.get("broadcastMessages").getAsBoolean();
        }
        // Support 'broadcastOnReturn' for return-from-AFK messages
        if (afkConfig.has("broadcastOnReturn")) {
            this.broadcastReturnMessages = afkConfig.get("broadcastOnReturn").getAsBoolean();
        } else {
            this.broadcastReturnMessages = this.broadcastAfkMessages;
        }

        // AFK kick settings: parse timeout FIRST, then derive kickAfkPlayers from it.
        // This ensures that users only need to set kickTimeout > 0 to enable AFK kicks,
        // matching the documented behaviour: kickTimeout = 0 means disabled.
        // Support both 'kickTimeout' (seconds) and 'kickTimeoutMinutes' (minutes) for compatibility
        if (afkConfig.has("kickTimeout")) {
            long kickTimeoutSeconds = afkConfig.get("kickTimeout").getAsLong();
            this.afkKickTimeoutMs = kickTimeoutSeconds > 0 ? kickTimeoutSeconds * 1000L : 0;
            if (kickTimeoutSeconds > 0) {
                NeoLog.info(LOGGER, LogCategory.CHAT, "AFK kick timeout: {} seconds ({} minutes)", kickTimeoutSeconds, kickTimeoutSeconds / 60);
            }
        } else if (afkConfig.has("kickTimeoutMinutes")) {
            long kickTimeoutMinutes = afkConfig.get("kickTimeoutMinutes").getAsLong();
            this.afkKickTimeoutMs = kickTimeoutMinutes > 0 ? kickTimeoutMinutes * 60000L : 0;
            if (kickTimeoutMinutes > 0) {
                NeoLog.info(LOGGER, LogCategory.CHAT, "AFK kick timeout: {} minutes", kickTimeoutMinutes);
            }
        } else {
            // No timeout key present; default to 0 (disabled)
            this.afkKickTimeoutMs = 0;
        }

        // Determine kickAfkPlayers:
        // - If explicitly set in config, honour that value (allows force-disabling even with timeout > 0)
        // - Otherwise auto-derive: kick is enabled whenever kickTimeout > 0
        if (afkConfig.has("kickAfkPlayers")) {
            this.kickAfkPlayers = afkConfig.get("kickAfkPlayers").getAsBoolean();
        } else {
            this.kickAfkPlayers = this.afkKickTimeoutMs > 0;
        }

        NeoLog.info(LOGGER, LogCategory.CHAT, "AFK kick feature: {}", this.kickAfkPlayers ? "ENABLED" : "DISABLED");

        // Warn if kick is enabled with 0 timeout (would kick immediately!) – guard against explicit misconfiguration
        if (this.kickAfkPlayers && this.afkKickTimeoutMs == 0) {
            LOGGER.error("AFK kick is ENABLED but timeout is 0! This would kick players immediately. Setting to 30 minutes default.");
            this.afkKickTimeoutMs = 1800000; // Force 30 minutes minimum
        }
        this.afkMessage = afkConfig.has("afkMessage") ? 
            afkConfig.get("afkMessage").getAsString() : "{player} is now AFK";
        this.returnMessage = afkConfig.has("returnMessage") ? 
            afkConfig.get("returnMessage").getAsString() : "{player} is no longer AFK";
        this.afkKickMessage = afkConfig.has("afkkickMessage") ? 
            afkConfig.get("afkkickMessage").getAsString() : null;

        // Support ignoreAfkInSleep from config
        if (afkConfig.has("ignoreAfkInSleep")) {
            this.ignoreAfkInSleep = afkConfig.get("ignoreAfkInSleep").getAsBoolean();
        }

        // Support enableActivityTracking from config
        if (afkConfig.has("enableActivityTracking")) {
            this.enableActivityTracking = afkConfig.get("enableActivityTracking").getAsBoolean();
        }

        // Support trackMovement from config
        if (afkConfig.has("trackMovement")) {
            this.trackMovement = afkConfig.get("trackMovement").getAsBoolean();
        }

        // Support trackChat from config
        if (afkConfig.has("trackChat")) {
            this.trackChat = afkConfig.get("trackChat").getAsBoolean();
        }

        // Support trackCommands from config
        if (afkConfig.has("trackCommands")) {
            this.trackCommands = afkConfig.get("trackCommands").getAsBoolean();
        }

        // Support trackInteractions from config
        if (afkConfig.has("trackInteractions")) {
            this.trackInteractions = afkConfig.get("trackInteractions").getAsBoolean();
        }

        // Support rotationThreshold from config
        if (afkConfig.has("rotationThreshold")) {
            try {
                this.rotationThreshold = afkConfig.get("rotationThreshold").getAsDouble();
            } catch (Exception e) {
                this.rotationThreshold = 5.0;
                LOGGER.warn("Invalid value for rotationThreshold in config, using default 5.0");
            }
        } else {
            this.rotationThreshold = 5.0;
        }

        // Support movementThreshold from config
        if (afkConfig.has("movementThreshold")) {
            try {
                this.movementThreshold = afkConfig.get("movementThreshold").getAsDouble();
            } catch (Exception e) {
                this.movementThreshold = 0.1;
                LOGGER.warn("Invalid value for movementThreshold in config, using default 0.1");
            }
        } else {
            this.movementThreshold = 0.1;
        }

        // Support excludedCommands from config
        if (afkConfig.has("excludedCommands") && afkConfig.get("excludedCommands").isJsonArray()) {
            try {
                java.util.Set<String> newSet = new java.util.HashSet<>();
                for (var el : afkConfig.get("excludedCommands").getAsJsonArray()) {
                    newSet.add(el.getAsString().toLowerCase());
                }
                if (!newSet.isEmpty()) {
                    this.excludedCommands = newSet;
                }
            } catch (Exception e) {
                LOGGER.warn("Invalid value for excludedCommands in config, using default list");
            }
        }

        // Support invulnerableWhenAfk from config
        if (afkConfig.has("invulnerableWhenAfk")) {
            this.invulnerableWhenAfk = afkConfig.get("invulnerableWhenAfk").getAsBoolean();
        } else {
            this.invulnerableWhenAfk = false;
        }

        NeoLog.info(LOGGER, LogCategory.CHAT, "AFK configuration loaded: timeout={}min, autoAfk={}, broadcast={}, kick={}, invulnerable={}",
            afkTimeoutMs / 60000, autoAfkEnabled, broadcastAfkMessages, kickAfkPlayers, invulnerableWhenAfk);
    }
    
    /**
     * Remove player data on logout
     */
    public void onPlayerLogout(UUID playerUuid) {
        playerData.remove(playerUuid);
        store.delete(COLLECTION, playerUuid.toString());
    }

    /**
     * Load AFK data from the active {@link com.zerog.neoessentials.storage.DataStore}.
     * Only {@code lastActivity} is persisted — AFK status/reason/start-time are
     * intentionally session-based and are never restored on server restart (matches the
     * previous file-based behavior).
     */
    private void loadAfkData() {
        try {
            for (Map.Entry<String, JsonObject> entry : store.getAll(COLLECTION).entrySet()) {
                try {
                    UUID uuid = UUID.fromString(entry.getKey());
                    JsonObject playerJson = entry.getValue();

                    PlayerAfkData playerAfkData = new PlayerAfkData();
                    playerAfkData.setLastActivity(playerJson.get("lastActivity").getAsLong());
                    // Don't restore AFK status on server restart - all players start as active

                    playerData.put(uuid, playerAfkData);
                } catch (Exception e) {
                    LOGGER.warn("Failed to load AFK data for entry: {}", entry.getKey());
                }
            }

            NeoLog.info(LOGGER, LogCategory.CHAT, "Loaded AFK data for {} players", playerData.size());
        } catch (Exception e) {
            LOGGER.error("Failed to load AFK data", e);
        }
    }

    /**
     * Queue save of AFK data (async)
     */
    private void queueSaveAfkData() {
        // Don't queue tasks if we're shutting down
        if (isShuttingDown || afkCheckExecutor.isShutdown()) {
            return;
        }

        try {
            afkCheckExecutor.execute(this::saveAfkData);
        } catch (java.util.concurrent.RejectedExecutionException e) {
            // Executor is shutting down, just log and skip
            NeoLog.debug(LOGGER, LogCategory.CHAT, "Cannot queue AFK data save - executor is shutting down");
        }
    }

    /**
     * Persist current AFK data (just {@code lastActivity} per player) to the active
     * {@link com.zerog.neoessentials.storage.DataStore}, one record per tracked player.
     */
    private void saveAfkData() {
        try {
            for (Map.Entry<UUID, PlayerAfkData> entry : playerData.entrySet()) {
                JsonObject playerJson = new JsonObject();
                // Don't save AFK status - it's session-based
                playerJson.addProperty("lastActivity", entry.getValue().getLastActivity());
                store.put(COLLECTION, entry.getKey().toString(), playerJson);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to save AFK data", e);
        }
    }

    /**
     * One-time import of the legacy {@code afk_data.json} file into the active DataStore,
     * if it's still empty and storage.autoMigrate is enabled.
     */
    private void migrateLegacyFileIfNeeded() {
        if (store.hasAnyData(COLLECTION)) return;
        if (!com.zerog.neoessentials.config.ConfigManager.getInstance().isStorageAutoMigrateEnabled()) return;
        if (!afkDataFile.exists()) return;

        int migrated = 0;
        try (FileReader reader = new FileReader(afkDataFile)) {
            JsonObject data = JsonParser.parseReader(reader).getAsJsonObject();
            if (data == null) return;
            for (Map.Entry<String, com.google.gson.JsonElement> entry : data.entrySet()) {
                try {
                    UUID uuid = UUID.fromString(entry.getKey());
                    JsonObject playerJson = entry.getValue().getAsJsonObject();
                    JsonObject record = new JsonObject();
                    record.addProperty("lastActivity", playerJson.get("lastActivity").getAsLong());
                    store.put(COLLECTION, uuid.toString(), record);
                    migrated++;
                } catch (Exception e) {
                    LOGGER.warn("Failed to migrate legacy AFK data for entry: {}", entry.getKey());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to migrate legacy afk_data.json", e);
        }

        if (migrated > 0) {
            NeoLog.info(LOGGER, LogCategory.CHAT, "AfkManager: migrated {} AFK record(s) from legacy afk_data.json into the '{}' storage backend.",
                migrated, com.zerog.neoessentials.storage.StorageManager.getInstance().getActiveType());
        }
    }
    
    // Configuration getter methods for external access
    public boolean isIgnoreAfkInSleep() { return ignoreAfkInSleep; }
    public boolean isEnableTablistIndicator() { return enableTablistIndicator; }
    public String getTablistAfkPrefix() { return tablistAfkPrefix; }
    public String getTablistAfkSuffix() { return tablistAfkSuffix; }
    public boolean isEnableActivityTracking() { return enableActivityTracking; }
    public boolean isTrackMovement() { return trackMovement; }
    public boolean isTrackChat() { return trackChat; }
    public boolean isTrackCommands() { return trackCommands; }
    public boolean isTrackInteractions() { return trackInteractions; }
    public double getRotationThreshold() { return rotationThreshold; }
    public double getMovementThreshold() { return movementThreshold; }
    public boolean isAutoSave() { return autoSave; }
    public int getSaveIntervalSeconds() { return saveIntervalSeconds; }
    public java.util.Set<String> getExcludedCommands() { return excludedCommands; }
    public boolean isInvulnerableWhenAfk() { return invulnerableWhenAfk; }
    
    /**
     * Shutdown the AFK manager
     */
    public void shutdown() {
        NeoLog.info(LOGGER, LogCategory.CHAT, "Shutting down AFK Manager...");
        // Set shutdown flag first to prevent new task submissions
        isShuttingDown = true;

        try {
            // Save data synchronously before shutting down
            saveAfkData();

            // Shutdown executor
            afkCheckExecutor.shutdown();
            if (!afkCheckExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                LOGGER.warn("AFK Manager executor did not terminate gracefully, forcing shutdown...");
                afkCheckExecutor.shutdownNow();
            }
            NeoLog.info(LOGGER, LogCategory.CHAT, "AFK Manager shutdown complete");
        } catch (InterruptedException e) {
            LOGGER.warn("Interrupted while waiting for AFK Manager executor shutdown");
            afkCheckExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            LOGGER.error("Error during AFK Manager shutdown", e);
        }
    }

    /**
     * Start periodic auto-save task for AFK data
     */
    private void startAutoSaveTask() {
        afkCheckExecutor.scheduleAtFixedRate(() -> {
            if (autoSave) {
                try {
                    saveAfkData();
                    com.zerog.neoessentials.logging.NeoLog.debug(LOGGER, com.zerog.neoessentials.logging.LogCategory.CHAT, "AFK data auto-saved");
                } catch (Exception e) {
                    LOGGER.error("Error during AFK data auto-save", e);
                }
            }
        }, saveIntervalSeconds, saveIntervalSeconds, TimeUnit.SECONDS);
    }

    /**
     * Reload AFK data from disk (does not clear current AFK states)
     */
    public void reload() {
        NeoLog.info(LOGGER, LogCategory.CHAT, "Reloading AFK system...");
        // Note: We don't clear current AFK states as they represent live player state
        // Just reload configuration if needed
        NeoLog.info(LOGGER, LogCategory.CHAT, "AFK system reloaded");
    }
}
