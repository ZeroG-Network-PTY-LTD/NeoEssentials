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
public class AfkManager {
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
    
    // Data persistence
    private final File afkDataFile = new File("config/neoessentials/afk_data.json");
    
    // AFK configuration (will be loaded from config)
    private long afkTimeoutMs = 300000; // 5 minutes default
    private boolean autoAfkEnabled = true;
    private boolean broadcastAfkMessages = true;
    private boolean kickAfkPlayers = false;
    private long afkKickTimeoutMs = 1800000; // 30 minutes default
    private String afkMessage = "{player} is now AFK";
    private String returnMessage = "{player} is no longer AFK";
    
    // Additional configuration fields
    private boolean ignoreAfkInSleep = true;
    private boolean enableTablistIndicator = true;
    private String tablistAfkPrefix = "[AFK] ";
    private String tablistAfkSuffix = "";
    
    private AfkManager() {
        loadAfkData();
        startAfkCheckTask();
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
        if (!broadcastAfkMessages) return;
        
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
            
            // Check if AFK player should be kicked
            if (kickAfkPlayers && data.isAfk() && (currentTime - data.getAfkStartTime()) > afkKickTimeoutMs) {
                try {
                    player.connection.disconnect(Component.literal("Kicked for being AFK too long"));
                    LOGGER.info("Kicked player {} for being AFK too long", player.getName().getString());
                } catch (Exception e) {
                    LOGGER.error("Error kicking AFK player", e);
                }
            }
        }
    }
    
    /**
     * Load AFK configuration from config manager
     */
    public void loadConfiguration(JsonObject afkConfig) {
        if (afkConfig == null) return;
        
        this.afkTimeoutMs = afkConfig.has("timeoutMinutes") ? 
            afkConfig.get("timeoutMinutes").getAsLong() * 60000 : 300000;
        this.autoAfkEnabled = !afkConfig.has("autoAfkEnabled") || 
            afkConfig.get("autoAfkEnabled").getAsBoolean();
        this.broadcastAfkMessages = !afkConfig.has("broadcastMessages") || 
            afkConfig.get("broadcastMessages").getAsBoolean();
        this.kickAfkPlayers = afkConfig.has("kickAfkPlayers") && 
            afkConfig.get("kickAfkPlayers").getAsBoolean();
        this.afkKickTimeoutMs = afkConfig.has("kickTimeoutMinutes") ? 
            afkConfig.get("kickTimeoutMinutes").getAsLong() * 60000 : 1800000;
        this.afkMessage = afkConfig.has("afkMessage") ? 
            afkConfig.get("afkMessage").getAsString() : "{player} is now AFK";
        this.returnMessage = afkConfig.has("returnMessage") ? 
            afkConfig.get("returnMessage").getAsString() : "{player} is no longer AFK";
            
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
        afkCheckExecutor.execute(this::saveAfkData);
    }
    
    /**
     * Save AFK data to file
     */
    private void saveAfkData() {
        try {
            if (!afkDataFile.getParentFile().exists()) {
                afkDataFile.getParentFile().mkdirs();
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
    
    /**
     * Shutdown the AFK manager
     */
    public void shutdown() {
        try {
            saveAfkData();
            afkCheckExecutor.shutdown();
            if (!afkCheckExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                afkCheckExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            afkCheckExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
