package com.zerog.neoessentials.player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Player data container for storing persistent player information
 * Includes preferences, playtime, achievements, and admin notes
 * 
 * @author ZeroG
 * @since 2.1.0
 */
public class PlayerData {
    
    private UUID playerUUID;
    private String lastKnownName;
    private long firstJoin;
    private long lastJoin;
    private long lastSeen;
    private long lastUpdated;
    
    // Playtime tracking
    private long totalPlaytime; // in milliseconds
    private long sessionStartTime;
    private boolean isOnline;
    
    // Player preferences
    private PlayerPreferences preferences;
    
    // Achievement tracking
    private Map<String, Long> achievements; // achievement_id -> timestamp
    private Map<String, Integer> achievementProgress; // achievement_id -> progress
    
    // Admin notes
    private Map<String, AdminNote> adminNotes; // note_id -> note
    
    // Statistics
    private Map<String, Object> statistics;
    
    // Permission system data
    private String permissionGroup;
    private Map<String, Boolean> playerPermissions;
    
    public PlayerData(UUID playerUUID) {
        this.playerUUID = playerUUID;
        this.firstJoin = System.currentTimeMillis();
        this.lastJoin = System.currentTimeMillis();
        this.lastSeen = System.currentTimeMillis();
        this.lastUpdated = System.currentTimeMillis();
        this.totalPlaytime = 0L;
        this.sessionStartTime = 0L;
        this.isOnline = false;
        
        this.preferences = new PlayerPreferences();
        this.achievements = new HashMap<>();
        this.achievementProgress = new HashMap<>();
        this.adminNotes = new HashMap<>();
        this.statistics = new HashMap<>();
        
        // Initialize permission data
        this.permissionGroup = "default"; // Default group
        this.playerPermissions = new HashMap<>();
    }
    
    // Getters and Setters
    public UUID getPlayerUUID() {
        return playerUUID;
    }
    
    public void setPlayerUUID(UUID playerUUID) {
        this.playerUUID = playerUUID;
    }
    
    public String getLastKnownName() {
        return lastKnownName;
    }
    
    public void setLastKnownName(String lastKnownName) {
        this.lastKnownName = lastKnownName;
    }
    
    public long getFirstJoin() {
        return firstJoin;
    }
    
    public void setFirstJoin(long firstJoin) {
        this.firstJoin = firstJoin;
    }
    
    public long getLastJoin() {
        return lastJoin;
    }
    
    public void setLastJoin(long lastJoin) {
        this.lastJoin = lastJoin;
    }
    
    public long getLastSeen() {
        return lastSeen;
    }
    
    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }
    
    public long getLastUpdated() {
        return lastUpdated;
    }
    
    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
    
    // Playtime methods
    public long getTotalPlaytime() {
        return totalPlaytime;
    }
    
    public void setTotalPlaytime(long totalPlaytime) {
        this.totalPlaytime = totalPlaytime;
    }
    
    public void addPlaytime(long additionalTime) {
        this.totalPlaytime += additionalTime;
    }
    
    public long getSessionStartTime() {
        return sessionStartTime;
    }
    
    public void setSessionStartTime(long sessionStartTime) {
        this.sessionStartTime = sessionStartTime;
    }
    
    public boolean isOnline() {
        return isOnline;
    }
    
    public void setOnline(boolean online) {
        this.isOnline = online;
    }
    
    // Session management
    public void startSession() {
        this.sessionStartTime = System.currentTimeMillis();
        this.isOnline = true;
        this.lastJoin = this.sessionStartTime;
    }
    
    public void endSession() {
        if (sessionStartTime > 0) {
            long sessionTime = System.currentTimeMillis() - sessionStartTime;
            addPlaytime(sessionTime);
        }
        this.isOnline = false;
        this.lastSeen = System.currentTimeMillis();
        this.sessionStartTime = 0L;
    }
    
    public long getCurrentSessionTime() {
        if (isOnline && sessionStartTime > 0) {
            return System.currentTimeMillis() - sessionStartTime;
        }
        return 0L;
    }
    
    // Preferences
    public PlayerPreferences getPreferences() {
        return preferences;
    }
    
    public void setPreferences(PlayerPreferences preferences) {
        this.preferences = preferences;
    }
    
    // Achievements
    public Map<String, Long> getAchievements() {
        return achievements;
    }
    
    public void setAchievements(Map<String, Long> achievements) {
        this.achievements = achievements;
    }
    
    public boolean hasAchievement(String achievementId) {
        return achievements.containsKey(achievementId);
    }
    
    public void addAchievement(String achievementId) {
        achievements.put(achievementId, System.currentTimeMillis());
    }
    
    public void removeAchievement(String achievementId) {
        achievements.remove(achievementId);
    }
    
    // Achievement Progress
    public Map<String, Integer> getAchievementProgress() {
        return achievementProgress;
    }
    
    public void setAchievementProgress(Map<String, Integer> achievementProgress) {
        this.achievementProgress = achievementProgress;
    }
    
    public int getAchievementProgress(String achievementId) {
        return achievementProgress.getOrDefault(achievementId, 0);
    }
    
    public void setAchievementProgress(String achievementId, int progress) {
        achievementProgress.put(achievementId, progress);
    }
    
    public void incrementAchievementProgress(String achievementId, int amount) {
        int current = getAchievementProgress(achievementId);
        setAchievementProgress(achievementId, current + amount);
    }
    
    // Admin Notes
    public Map<String, AdminNote> getAdminNotes() {
        return adminNotes;
    }
    
    public void setAdminNotes(Map<String, AdminNote> adminNotes) {
        this.adminNotes = adminNotes;
    }
    
    public void addAdminNote(String noteId, AdminNote note) {
        adminNotes.put(noteId, note);
    }
    
    public void removeAdminNote(String noteId) {
        adminNotes.remove(noteId);
    }
    
    public AdminNote getAdminNote(String noteId) {
        return adminNotes.get(noteId);
    }
    
    // Statistics
    public Map<String, Object> getStatistics() {
        return statistics;
    }
    
    public void setStatistics(Map<String, Object> statistics) {
        this.statistics = statistics;
    }
    
    public Object getStatistic(String key) {
        return statistics.get(key);
    }
    
    public void setStatistic(String key, Object value) {
        statistics.put(key, value);
    }
    
    /**
     * Increment a statistic value safely
     */
    public void incrementStatistic(String key, Number amount) {
        Object current = getStatistic(key);
        if (current instanceof Number) {
            if (current instanceof Integer) {
                setStatistic(key, ((Integer) current) + amount.intValue());
            } else if (current instanceof Long) {
                setStatistic(key, ((Long) current) + amount.longValue());
            } else if (current instanceof Double) {
                setStatistic(key, ((Double) current) + amount.doubleValue());
            } else if (current instanceof Float) {
                setStatistic(key, ((Float) current) + amount.floatValue());
            }
        } else {
            setStatistic(key, amount);
        }
    }
    
    /**
     * Get statistic as integer with default value
     */
    public int getStatisticAsInt(String key, int defaultValue) {
        Object stat = getStatistic(key);
        return stat instanceof Number ? ((Number) stat).intValue() : defaultValue;
    }
    
    /**
     * Get statistic as double with default value
     */
    public double getStatisticAsDouble(String key, double defaultValue) {
        Object stat = getStatistic(key);
        return stat instanceof Number ? ((Number) stat).doubleValue() : defaultValue;
    }
    
    /**
     * Check if player has any kills
     */
    public boolean hasKills() {
        return getStatisticAsInt("player_kills", 0) > 0;
    }
    
    /**
     * Check if player has any deaths
     */
    public boolean hasDeaths() {
        return getStatisticAsInt("player_deaths", 0) > 0;
    }
    
    /**
     * Get kill/death ratio
     */
    public double getKDR() {
        int kills = getStatisticAsInt("player_kills", 0);
        int deaths = getStatisticAsInt("player_deaths", 0);
        return deaths > 0 ? (double) kills / deaths : kills;
    }
    
    /**
     * Get formatted KDR string
     */
    public String getFormattedKDR() {
        return String.format("%.2f", getKDR());
    }
    
    /**
     * Get total blocks interacted with
     */
    public int getTotalBlocksInteracted() {
        return getStatisticAsInt("blocks_broken", 0) + getStatisticAsInt("blocks_placed", 0);
    }
    
    // Utility methods
    public String getFormattedPlaytime() {
        long totalSeconds = totalPlaytime / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        
        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }
    
    public String getFormattedCurrentSession() {
        long currentSession = getCurrentSessionTime();
        if (currentSession == 0) {
            return "Not in session";
        }
        
        long totalSeconds = currentSession / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        
        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }
    
    // Permission system getters and setters
    public String getPermissionGroup() {
        return permissionGroup;
    }
    
    public void setPermissionGroup(String permissionGroup) {
        this.permissionGroup = permissionGroup;
    }
    
    public Map<String, Boolean> getPlayerPermissions() {
        return playerPermissions;
    }
    
    public void setPlayerPermissions(Map<String, Boolean> playerPermissions) {
        this.playerPermissions = playerPermissions;
    }
    
    public void addPlayerPermission(String permission, boolean value) {
        this.playerPermissions.put(permission, value);
    }
    
    public void removePlayerPermission(String permission) {
        this.playerPermissions.remove(permission);
    }
    
    @Override
    public String toString() {
        return String.format("PlayerData{uuid=%s, name=%s, totalPlaytime=%s, online=%s}", 
            playerUUID, lastKnownName, getFormattedPlaytime(), isOnline);
    }
}
