package com.zerog.neoessentials.chat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import com.zerog.neoessentials.util.MessageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
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
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
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
    private final ScheduledExecutorService afkCheckExecutor = Executors.newSingleThreadScheduledExecutor();
    
    // Shutdown flag to prevent task submission after shutdown
    private volatile boolean isShuttingDown = false;

    // Data persistence
    private final File afkDataFile = new File(com.zerog.neoessentials.util.ResourceUtil.DATA_DIR + "afk_data.json");

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

    // Configurable excluded commands for AFK activity tracking
    private java.util.Set<String> excludedCommands = new java.util.HashSet<>(java.util.Arrays.asList(
        "afk", "list", "who", "tps", "ping", "help", "?"
    ));
    
    private AfkManager() {
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
        } else if (!afk && wasAfk) {
            data.setAfkStartTime(0);
            data.setLastActivity(System.currentTimeMillis());
            onPlayerReturnFromAfk(playerUuid);
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
            if (reason != null && !reason.trim().isEmpty()) {
                message += " (" + reason + ")";
            }
            
            // Broadcast to all players
            Component afkComponent = MessageUtil.info(message);
            server.getPlayerList().broadcastSystemMessage(afkComponent, false);
            
            LOGGER.info("Player {} went AFK{}", player.getName().getString(), 
                reason != null ? " (" + reason + ")" : "");
            
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
            
            // Broadcast to all players
            Component returnComponent = MessageUtil.info(message);
            server.getPlayerList().broadcastSystemMessage(returnComponent, false);
            
            LOGGER.info("Player {} returned from AFK", player.getName().getString());
            
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
                long afkDuration = currentTime - data.getAfkStartTime();

                // Warn player 60 seconds before kick
                if (afkDuration > (afkKickTimeoutMs - 60000) && afkDuration < afkKickTimeoutMs) {
                    long secondsUntilKick = (afkKickTimeoutMs - afkDuration) / 1000;
                    if (secondsUntilKick > 0 && secondsUntilKick <= 60) {
                        player.sendSystemMessage(Component.literal(
                            String.format("§c§lWARNING: §eYou will be kicked for AFK in %d seconds! Move to stay connected.", secondsUntilKick)
                        ));
                        LOGGER.warn("Player {} will be kicked for AFK in {} seconds", player.getName().getString(), secondsUntilKick);
                    }
                }

                // Kick if timeout exceeded
                if (afkDuration > afkKickTimeoutMs) {
                    try {
                        String kickMsg = this.afkKickMessage != null && !this.afkKickMessage.isEmpty()
                            ? this.afkKickMessage
                            : "§cKicked for being AFK too long.\n§7You were inactive for " + (afkDuration / 60000) + " minutes.";
                        player.connection.disconnect(Component.literal(kickMsg));
                        LOGGER.info("Kicked player {} for being AFK too long (AFK for {} minutes)",
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

        // AFK kick settings - DISABLED by default to prevent unexpected kicks
        // Only enable if explicitly set to true in config
        this.kickAfkPlayers = afkConfig.has("kickAfkPlayers") &&
            afkConfig.get("kickAfkPlayers").getAsBoolean();

        LOGGER.info("AFK kick feature: {}", this.kickAfkPlayers ? "ENABLED" : "DISABLED");

        // Support both 'kickTimeout' (seconds) and 'kickTimeoutMinutes' (minutes) for compatibility
        if (afkConfig.has("kickTimeout")) {
            long kickTimeoutSeconds = afkConfig.get("kickTimeout").getAsLong();
            this.afkKickTimeoutMs = kickTimeoutSeconds > 0 ? kickTimeoutSeconds * 1000 : 0;
            if (this.kickAfkPlayers && kickTimeoutSeconds > 0) {
                LOGGER.info("AFK kick timeout: {} seconds ({} minutes)", kickTimeoutSeconds, kickTimeoutSeconds / 60);
            }
        } else if (afkConfig.has("kickTimeoutMinutes")) {
            long kickTimeoutMinutes = afkConfig.get("kickTimeoutMinutes").getAsLong();
            this.afkKickTimeoutMs = kickTimeoutMinutes > 0 ? kickTimeoutMinutes * 60000 : 0;
            if (this.kickAfkPlayers && kickTimeoutMinutes > 0) {
                LOGGER.info("AFK kick timeout: {} minutes", kickTimeoutMinutes);
            }
        } else {
            // Default 30 minutes, but only matters if kickAfkPlayers is enabled
            this.afkKickTimeoutMs = 1800000;
        }

        // Warn if kick is enabled with 0 timeout (would kick immediately!)
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

        LOGGER.info("AFK configuration loaded: timeout={}min, autoAfk={}, broadcast={}, kick={}", 
            afkTimeoutMs / 60000, autoAfkEnabled, broadcastAfkMessages, kickAfkPlayers);
    }
    
    /**
     * Remove player data on logout
     */
    public void onPlayerLogout(UUID playerUuid) {
        playerData.remove(playerUuid);
        queueSaveAfkData();
    }
    
    /**
     * Load AFK data from file
     */
    private void loadAfkData() {
        if (!afkDataFile.exists()) return;
        
        try (FileReader reader = new FileReader(afkDataFile)) {
            JsonObject data = JsonParser.parseReader(reader).getAsJsonObject();
            
            for (Map.Entry<String, com.google.gson.JsonElement> entry : data.entrySet()) {
                try {
                    UUID uuid = UUID.fromString(entry.getKey());
                    JsonObject playerJson = entry.getValue().getAsJsonObject();
                    
                    PlayerAfkData playerAfkData = new PlayerAfkData();
                    playerAfkData.setLastActivity(playerJson.get("lastActivity").getAsLong());
                    // Don't restore AFK status on server restart - all players start as active
                    
                    playerData.put(uuid, playerAfkData);
                } catch (Exception e) {
                    LOGGER.warn("Failed to load AFK data for entry: {}", entry.getKey());
                }
            }
            
            LOGGER.info("Loaded AFK data for {} players", playerData.size());
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
            LOGGER.debug("Cannot queue AFK data save - executor is shutting down");
        }
    }
    
    /**
     * Save AFK data to file
     */
    private void saveAfkData() {
        try {
            File parentDir = afkDataFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    LOGGER.error("Failed to create AFK data directory: {}", parentDir.getAbsolutePath());
                    return;
                }
            }
            
            JsonObject data = new JsonObject();
            for (Map.Entry<UUID, PlayerAfkData> entry : playerData.entrySet()) {
                JsonObject playerJson = new JsonObject();
                PlayerAfkData playerData = entry.getValue();
                
                playerJson.addProperty("lastActivity", playerData.getLastActivity());
                // Don't save AFK status - it's session-based
                
                data.add(entry.getKey().toString(), playerJson);
            }
            
            File tempFile = new File(afkDataFile.getAbsolutePath() + ".tmp");
            try (FileWriter writer = new FileWriter(tempFile)) {
                GSON.toJson(data, writer);
            }
            
            Files.move(tempFile.toPath(), afkDataFile.toPath(), 
                StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                
        } catch (Exception e) {
            LOGGER.error("Failed to save AFK data", e);
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
    public boolean isAutoSave() { return autoSave; }
    public int getSaveIntervalSeconds() { return saveIntervalSeconds; }
    public java.util.Set<String> getExcludedCommands() { return excludedCommands; }
    
    /**
     * Shutdown the AFK manager
     */
    public void shutdown() {
        LOGGER.info("Shutting down AFK Manager...");
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
            LOGGER.info("AFK Manager shutdown complete");
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
                    com.zerog.neoessentials.util.DebugLogger.log(LOGGER, "AFK data auto-saved");
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
        LOGGER.info("Reloading AFK system...");
        // Note: We don't clear current AFK states as they represent live player state
        // Just reload configuration if needed
        LOGGER.info("AFK system reloaded");
    }
}
