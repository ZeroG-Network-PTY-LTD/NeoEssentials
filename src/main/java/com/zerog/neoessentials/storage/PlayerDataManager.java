package com.zerog.neoessentials.storage;

import com.zerog.neoessentials.util.LocationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Player data management system for NeoEssentials
 * Handles storage and retrieval of player-specific data
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class PlayerDataManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerDataManager.class);
    private static PlayerDataManager instance;
    
    private final Map<UUID, PlayerData> playerDataCache;
    
    private PlayerDataManager() {
        this.playerDataCache = new ConcurrentHashMap<>();
    }
    
    public static PlayerDataManager getInstance() {
        if (instance == null) {
            instance = new PlayerDataManager();
        }
        return instance;
    }
    
    /**
     * Get or create player data
     */
    public PlayerData getPlayerData(UUID playerUUID) {
        return playerDataCache.computeIfAbsent(playerUUID, PlayerData::new);
    }
    
    /**
     * Save player data (placeholder - would save to file/database)
     */
    public void savePlayerData(UUID playerUUID) {
        PlayerData data = playerDataCache.get(playerUUID);
        if (data != null) {
            // Placeholder for actual saving logic
            LOGGER.debug("Saved data for player {}", playerUUID);
        }
    }
    
    /**
     * Load player data (placeholder - would load from file/database)
     */
    public void loadPlayerData(UUID playerUUID) {
        // Placeholder for actual loading logic
        getPlayerData(playerUUID); // This will create if not exists
        LOGGER.debug("Loaded data for player {}", playerUUID);
    }
    
    /**
     * Remove player data from cache
     */
    public void unloadPlayerData(UUID playerUUID) {
        savePlayerData(playerUUID); // Save before unloading
        playerDataCache.remove(playerUUID);
    }
    
    // Home-related methods
    public void setHome(UUID playerUUID, String homeName, LocationUtil.Location location) {
        PlayerData data = getPlayerData(playerUUID);
        data.homes.put(homeName.toLowerCase(), location);
        savePlayerData(playerUUID);
    }
    
    public LocationUtil.Location getHome(UUID playerUUID, String homeName) {
        PlayerData data = getPlayerData(playerUUID);
        return data.homes.get(homeName.toLowerCase());
    }
    
    public void deleteHome(UUID playerUUID, String homeName) {
        PlayerData data = getPlayerData(playerUUID);
        data.homes.remove(homeName.toLowerCase());
        savePlayerData(playerUUID);
    }
    
    public boolean hasHome(UUID playerUUID, String homeName) {
        PlayerData data = getPlayerData(playerUUID);
        return data.homes.containsKey(homeName.toLowerCase());
    }
    
    public List<String> getHomeNames(UUID playerUUID) {
        PlayerData data = getPlayerData(playerUUID);
        return new ArrayList<>(data.homes.keySet());
    }
    
    public int getHomeCount(UUID playerUUID) {
        PlayerData data = getPlayerData(playerUUID);
        return data.homes.size();
    }
    
    // Economy-related methods
    public BigDecimal getBalance(UUID playerUUID) {
        PlayerData data = getPlayerData(playerUUID);
        return data.balance;
    }
    
    public void setBalance(UUID playerUUID, BigDecimal balance) {
        PlayerData data = getPlayerData(playerUUID);
        data.balance = balance;
        savePlayerData(playerUUID);
    }
    
    // Mail-related methods
    public void addMail(UUID playerUUID, Mail mail) {
        PlayerData data = getPlayerData(playerUUID);
        data.mail.add(mail);
        savePlayerData(playerUUID);
    }
    
    public List<Mail> getMail(UUID playerUUID) {
        PlayerData data = getPlayerData(playerUUID);
        return new ArrayList<>(data.mail);
    }
    
    public void deleteMail(UUID playerUUID, int mailIndex) {
        PlayerData data = getPlayerData(playerUUID);
        if (mailIndex >= 0 && mailIndex < data.mail.size()) {
            data.mail.remove(mailIndex);
            savePlayerData(playerUUID);
        }
    }
    
    public void clearMail(UUID playerUUID) {
        PlayerData data = getPlayerData(playerUUID);
        data.mail.clear();
        savePlayerData(playerUUID);
    }
    
    // Settings-related methods
    public void setSetting(UUID playerUUID, String key, Object value) {
        PlayerData data = getPlayerData(playerUUID);
        data.settings.put(key, value);
        savePlayerData(playerUUID);
    }
    
    public Object getSetting(UUID playerUUID, String key) {
        PlayerData data = getPlayerData(playerUUID);
        return data.settings.get(key);
    }
    
    public boolean getSettingBoolean(UUID playerUUID, String key, boolean defaultValue) {
        Object value = getSetting(playerUUID, key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }
    
    public String getSettingString(UUID playerUUID, String key, String defaultValue) {
        Object value = getSetting(playerUUID, key);
        if (value instanceof String) {
            return (String) value;
        }
        return defaultValue;
    }
    
    // Last location tracking
    public void setLastLocation(UUID playerUUID, String world, double x, double y, double z, float yaw, float pitch) {
        PlayerData data = getPlayerData(playerUUID);
        data.lastLocation = new LocationUtil.Location(world, x, y, z, yaw, pitch, System.currentTimeMillis());
        savePlayerData(playerUUID);
    }
    
    public LocationUtil.Location getLastLocation(UUID playerUUID) {
        PlayerData data = getPlayerData(playerUUID);
        return data.lastLocation;
    }
    
    /**
     * Player data container class
     */
    public static class PlayerData {
        public final UUID playerUUID;
        public final Map<String, LocationUtil.Location> homes;
        public final List<Mail> mail;
        public final Map<String, Object> settings;
        public BigDecimal balance;
        public LocationUtil.Location lastLocation;
        public long lastSeen;
        public String nickname;
        public boolean afk;
        public boolean vanished;
        public boolean godMode;
        public boolean muted;
        public boolean jailed;
        public long afkTime;
        public long muteExpiry;
        public long jailExpiry;
        
        public PlayerData(UUID playerUUID) {
            this.playerUUID = playerUUID;
            this.homes = new ConcurrentHashMap<>();
            this.mail = Collections.synchronizedList(new ArrayList<>());
            this.settings = new ConcurrentHashMap<>();
            this.balance = BigDecimal.ZERO;
            this.lastSeen = System.currentTimeMillis();
            this.afk = false;
            this.vanished = false;
            this.godMode = false;
            this.muted = false;
            this.jailed = false;
            this.afkTime = 0;
            this.muteExpiry = 0;
            this.jailExpiry = 0;
        }
    }
    
    /**
     * Mail message class
     */
    public static class Mail {
        public final UUID sender;
        public final String senderName;
        public final String message;
        public final long timestamp;
        public boolean read;
        
        public Mail(UUID sender, String senderName, String message) {
            this.sender = sender;
            this.senderName = senderName;
            this.message = message;
            this.timestamp = System.currentTimeMillis();
            this.read = false;
        }
    }
}
