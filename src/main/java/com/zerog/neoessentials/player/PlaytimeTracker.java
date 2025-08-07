package com.zerog.neoessentials.player;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Playtime tracking system for accurate player session and total playtime monitoring
 * Handles session management and persistent storage of playtime data
 * 
 * @author ZeroG
 * @since 2.1.0
 */
public class PlaytimeTracker {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(PlaytimeTracker.class);
    private static PlaytimeTracker instance;
    
    private final Map<UUID, Long> sessionStartTimes;
    private final PlayerDataManager playerDataManager;
    private final ScheduledExecutorService scheduler;
    
    private PlaytimeTracker() {
        this.sessionStartTimes = new ConcurrentHashMap<>();
        this.playerDataManager = PlayerDataManager.getInstance();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "NeoEssentials-PlaytimeTracker");
            t.setDaemon(true);
            return t;
        });
        
        // Register for events
        NeoForge.EVENT_BUS.register(this);
        LOGGER.info("PlaytimeTracker registered for events");
        
        // Auto-save playtime every 5 minutes
        scheduler.scheduleAtFixedRate(this::saveAllPlaytime, 5, 5, TimeUnit.MINUTES);
    }
    
    public static PlaytimeTracker getInstance() {
        if (instance == null) {
            instance = new PlaytimeTracker();
        }
        return instance;
    }
    
    /**
     * Start tracking playtime for a player
     */
    public void startSession(ServerPlayer player) {
        UUID playerUUID = player.getUUID();
        long currentTime = System.currentTimeMillis();
        
        sessionStartTimes.put(playerUUID, currentTime);
        
        PlayerData playerData = playerDataManager.getPlayerData(playerUUID);
        playerData.setLastKnownName(player.getName().getString());
        playerData.startSession();
        playerDataManager.updatePlayerData(playerData);
        
        LOGGER.debug("Started playtime tracking for player: {} ({})", 
            player.getName().getString(), playerUUID);
    }
    
    /**
     * Stop tracking playtime for a player
     */
    public void endSession(ServerPlayer player) {
        UUID playerUUID = player.getUUID();
        Long sessionStart = sessionStartTimes.remove(playerUUID);
        
        if (sessionStart != null) {
            long sessionTime = System.currentTimeMillis() - sessionStart;
            
            PlayerData playerData = playerDataManager.getPlayerData(playerUUID);
            playerData.addPlaytime(sessionTime);
            playerData.endSession();
            playerDataManager.updatePlayerData(playerData);
            
            LOGGER.debug("Ended playtime tracking for player: {} ({}), session time: {}ms", 
                player.getName().getString(), playerUUID, sessionTime);
        }
    }
    
    /**
     * Get current session time for a player
     */
    public long getCurrentSessionTime(UUID playerUUID) {
        Long sessionStart = sessionStartTimes.get(playerUUID);
        if (sessionStart != null) {
            return System.currentTimeMillis() - sessionStart;
        }
        return 0L;
    }
    
    /**
     * Get current session time for a player
     */
    public long getCurrentSessionTime(ServerPlayer player) {
        return getCurrentSessionTime(player.getUUID());
    }
    
    /**
     * Get total playtime for a player
     */
    public long getTotalPlaytime(UUID playerUUID) {
        PlayerData playerData = playerDataManager.getPlayerData(playerUUID);
        long totalPlaytime = playerData.getTotalPlaytime();
        
        // Add current session time if player is online
        Long sessionStart = sessionStartTimes.get(playerUUID);
        if (sessionStart != null) {
            totalPlaytime += System.currentTimeMillis() - sessionStart;
        }
        
        return totalPlaytime;
    }
    
    /**
     * Get total playtime for a player
     */
    public long getTotalPlaytime(ServerPlayer player) {
        return getTotalPlaytime(player.getUUID());
    }
    
    /**
     * Format playtime in a readable format
     */
    public String formatPlaytime(long playtimeMs) {
        long totalSeconds = playtimeMs / 1000;
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        
        if (days > 0) {
            return String.format("%dd %dh %dm %ds", days, hours, minutes, seconds);
        } else if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }
    
    /**
     * Get formatted current session time
     */
    public String getFormattedCurrentSession(UUID playerUUID) {
        return formatPlaytime(getCurrentSessionTime(playerUUID));
    }
    
    /**
     * Get formatted total playtime
     */
    public String getFormattedTotalPlaytime(UUID playerUUID) {
        return formatPlaytime(getTotalPlaytime(playerUUID));
    }
    
    /**
     * Check if player is currently online (has active session)
     */
    public boolean isPlayerOnline(UUID playerUUID) {
        return sessionStartTimes.containsKey(playerUUID);
    }
    
    /**
     * Get playtime statistics for a player
     */
    public PlaytimeStats getPlaytimeStats(UUID playerUUID) {
        PlayerData playerData = playerDataManager.getPlayerData(playerUUID);
        long currentSession = getCurrentSessionTime(playerUUID);
        long totalPlaytime = getTotalPlaytime(playerUUID);
        
        return new PlaytimeStats(
            playerUUID,
            playerData.getLastKnownName(),
            totalPlaytime,
            currentSession,
            playerData.getFirstJoin(),
            playerData.getLastJoin(),
            playerData.getLastSeen(),
            isPlayerOnline(playerUUID)
        );
    }
    
    /**
     * Save playtime for all online players
     */
    public void saveAllPlaytime() {
        int saved = 0;
        for (Map.Entry<UUID, Long> entry : sessionStartTimes.entrySet()) {
            UUID playerUUID = entry.getKey();
            Long sessionStart = entry.getValue();
            
            if (sessionStart != null) {
                try {
                    PlayerData playerData = playerDataManager.getPlayerData(playerUUID);
                    // Don't add time here, just update the session start time in case of server issues
                    playerData.setSessionStartTime(sessionStart);
                    playerDataManager.updatePlayerData(playerData);
                    saved++;
                } catch (Exception e) {
                    LOGGER.error("Failed to save playtime for player: {}", playerUUID, e);
                }
            }
        }
        
        if (saved > 0) {
            LOGGER.debug("Saved playtime data for {} online players", saved);
        }
    }
    
    /**
     * Get the number of currently tracked sessions
     */
    public int getActiveSessionCount() {
        return sessionStartTimes.size();
    }
    
    /**
     * Shutdown the playtime tracker
     */
    public void shutdown() {
        LOGGER.info("Shutting down PlaytimeTracker...");
        
        // Save all current playtime data
        saveAllPlaytime();
        
        // Shutdown scheduler
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        sessionStartTimes.clear();
        LOGGER.info("PlaytimeTracker shutdown complete");
    }
    
    /**
     * Event handlers for automatic session management
     */
    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            startSession(player);
        }
    }
    
    @SubscribeEvent
    public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            endSession(player);
        }
    }
    
    /**
     * Playtime statistics container
     */
    public static class PlaytimeStats {
        private final UUID playerUUID;
        private final String playerName;
        private final long totalPlaytime;
        private final long currentSession;
        private final long firstJoin;
        private final long lastJoin;
        private final long lastSeen;
        private final boolean isOnline;
        
        public PlaytimeStats(UUID playerUUID, String playerName, long totalPlaytime, 
                           long currentSession, long firstJoin, long lastJoin, 
                           long lastSeen, boolean isOnline) {
            this.playerUUID = playerUUID;
            this.playerName = playerName;
            this.totalPlaytime = totalPlaytime;
            this.currentSession = currentSession;
            this.firstJoin = firstJoin;
            this.lastJoin = lastJoin;
            this.lastSeen = lastSeen;
            this.isOnline = isOnline;
        }
        
        // Getters
        public UUID getPlayerUUID() { return playerUUID; }
        public String getPlayerName() { return playerName; }
        public long getTotalPlaytime() { return totalPlaytime; }
        public long getCurrentSession() { return currentSession; }
        public long getFirstJoin() { return firstJoin; }
        public long getLastJoin() { return lastJoin; }
        public long getLastSeen() { return lastSeen; }
        public boolean isOnline() { return isOnline; }
        
        public String getFormattedTotalPlaytime() {
            return PlaytimeTracker.getInstance().formatPlaytime(totalPlaytime);
        }
        
        public String getFormattedCurrentSession() {
            return PlaytimeTracker.getInstance().formatPlaytime(currentSession);
        }
    }
    
    /**
     * Format time in milliseconds to human-readable string
     */
    public static String formatTime(long milliseconds) {
        if (milliseconds <= 0) return "0 seconds";
        
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        seconds %= 60;
        minutes %= 60;
        hours %= 24;
        
        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append(" day").append(days != 1 ? "s" : "").append(" ");
        if (hours > 0) sb.append(hours).append(" hour").append(hours != 1 ? "s" : "").append(" ");
        if (minutes > 0) sb.append(minutes).append(" minute").append(minutes != 1 ? "s" : "").append(" ");
        if (seconds > 0 || sb.length() == 0) sb.append(seconds).append(" second").append(seconds != 1 ? "s" : "");
        
        return sb.toString().trim();
    }
}
