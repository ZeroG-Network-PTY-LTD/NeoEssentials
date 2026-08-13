package com.zerog.neoessentials.webdashboard.analytics;

import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks player session data for analytics
 * Monitors player join/leave times and session duration
 */
public class PlayerSessionTracker {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerSessionTracker.class);
    private static PlayerSessionTracker INSTANCE;
    
    private final Map<UUID, PlayerSession> activeSessions = new ConcurrentHashMap<>();
    private final List<PlayerSession> historicalSessions = new ArrayList<>();
    
    private PlayerSessionTracker() {}
    
    public static PlayerSessionTracker getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new PlayerSessionTracker();
        }
        return INSTANCE;
    }
    
    /**
     * Track when a player joins the server
     */
    public void trackPlayerJoin(UUID playerUuid, String playerName) {
        PlayerSession session = new PlayerSession(playerUuid, playerName, System.currentTimeMillis());
        activeSessions.put(playerUuid, session);
        NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Started tracking session for player: {} ({})", playerName, playerUuid);
    }
    
    /**
     * Track when a player leaves the server
     */
    public void trackPlayerLeave(UUID playerUuid) {
        PlayerSession session = activeSessions.remove(playerUuid);
        if (session != null) {
            session.setEndTime(System.currentTimeMillis());
            historicalSessions.add(session);
            NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Ended session for player: {} - Duration: {}ms",
                session.getPlayerName(), session.getDuration());
        }
    }
    
    /**
     * Get currently active player sessions
     */
    public Collection<PlayerSession> getActiveSessions() {
        return new ArrayList<>(activeSessions.values());
    }
    
    /**
     * Get historical player sessions
     */
    public List<PlayerSession> getHistoricalSessions() {
        return new ArrayList<>(historicalSessions);
    }
    
    /**
     * Get session for a specific player
     */
    public PlayerSession getPlayerSession(UUID playerUuid) {
        return activeSessions.get(playerUuid);
    }
    
    /**
     * Clear all historical session data
     */
    public void clearHistory() {
        historicalSessions.clear();
    }
    
    /**
     * Represents a player session
     */
    public static class PlayerSession {
        private final UUID playerUuid;
        private final String playerName;
        private final long startTime;
        private long endTime;
        
        public PlayerSession(UUID playerUuid, String playerName, long startTime) {
            this.playerUuid = playerUuid;
            this.playerName = playerName;
            this.startTime = startTime;
            this.endTime = 0;
        }
        
        public UUID getPlayerUuid() { return playerUuid; }
        public String getPlayerName() { return playerName; }
        public long getStartTime() { return startTime; }
        public long getEndTime() { return endTime; }
        public void setEndTime(long endTime) { this.endTime = endTime; }
        
        public long getDuration() {
            if (endTime == 0) {
                return System.currentTimeMillis() - startTime;
            }
            return endTime - startTime;
        }
        
        public boolean isActive() {
            return endTime == 0;
        }
    }
}
