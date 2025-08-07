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
    private final StorageManager storageManager;
    
    private PlayerDataManager() {
        this.playerDataCache = new ConcurrentHashMap<>();
        this.storageManager = StorageManager.getInstance();
    }
    
    public static PlayerDataManager getInstance() {
        if (instance == null) {
            instance = new PlayerDataManager();
        }
        return instance;
    }
    
    /**
     * Check if player data is already loaded in cache
     */
    public boolean isPlayerDataLoaded(UUID playerUUID) {
        return playerDataCache.containsKey(playerUUID);
    }

    /**
     * Get or create player data
     */
    public PlayerData getPlayerData(UUID playerUUID) {
        return playerDataCache.computeIfAbsent(playerUUID, PlayerData::new);
    }
    
    /**
     * Save player data to disk
     */
    public void savePlayerData(UUID playerUUID) {
        PlayerData data = playerDataCache.get(playerUUID);
        if (data != null) {
            // Create a serializable map of the player data
            Map<String, Object> playerDataMap = new HashMap<>();
            playerDataMap.put("uuid", playerUUID.toString());
            playerDataMap.put("balance", data.balance.toString());
            playerDataMap.put("lastSeen", data.lastSeen);
            playerDataMap.put("nickname", data.nickname);
            playerDataMap.put("afk", data.afk);
            playerDataMap.put("vanished", data.vanished);
            playerDataMap.put("godMode", data.godMode);
            playerDataMap.put("muted", data.muted);
            playerDataMap.put("jailed", data.jailed);
            playerDataMap.put("afkTime", data.afkTime);
            playerDataMap.put("muteExpiry", data.muteExpiry);
            playerDataMap.put("jailExpiry", data.jailExpiry);
            playerDataMap.put("settings", data.settings);
            
            // Convert homes to serializable format
            Map<String, Map<String, Object>> homesData = new HashMap<>();
            for (Map.Entry<String, LocationUtil.Location> entry : data.homes.entrySet()) {
                LocationUtil.Location loc = entry.getValue();
                Map<String, Object> locationData = new HashMap<>();
                locationData.put("world", loc.world);
                locationData.put("x", loc.x);
                locationData.put("y", loc.y);
                locationData.put("z", loc.z);
                locationData.put("yaw", loc.yaw);
                locationData.put("pitch", loc.pitch);
                locationData.put("timestamp", loc.timestamp);
                homesData.put(entry.getKey(), locationData);
            }
            playerDataMap.put("homes", homesData);
            
            // Convert last location to serializable format
            if (data.lastLocation != null) {
                Map<String, Object> lastLocData = new HashMap<>();
                lastLocData.put("world", data.lastLocation.world);
                lastLocData.put("x", data.lastLocation.x);
                lastLocData.put("y", data.lastLocation.y);
                lastLocData.put("z", data.lastLocation.z);
                lastLocData.put("yaw", data.lastLocation.yaw);
                lastLocData.put("pitch", data.lastLocation.pitch);
                lastLocData.put("timestamp", data.lastLocation.timestamp);
                playerDataMap.put("lastLocation", lastLocData);
            }
            
            // Save to storage
            storageManager.saveDataAsync("players", playerUUID.toString(), playerDataMap);
            LOGGER.debug("Saved data for player {} to disk", playerUUID);
        }
    }

    /**
     * Load player data from disk
     */
    @SuppressWarnings("unchecked")
    public void loadPlayerData(UUID playerUUID) {
        storageManager.loadDataAsync("players", playerUUID.toString(), Map.class)
            .thenAccept(dataMap -> {
                if (dataMap != null) {
                    PlayerData data = getPlayerData(playerUUID);
                    
                    // Load basic data
                    if (dataMap.get("balance") != null) {
                        data.balance = new BigDecimal(dataMap.get("balance").toString());
                    }
                    if (dataMap.get("lastSeen") != null) {
                        data.lastSeen = ((Number) dataMap.get("lastSeen")).longValue();
                    }
                    if (dataMap.get("nickname") != null) {
                        data.nickname = dataMap.get("nickname").toString();
                    }
                    if (dataMap.get("afk") != null) {
                        data.afk = Boolean.parseBoolean(dataMap.get("afk").toString());
                    }
                    if (dataMap.get("vanished") != null) {
                        data.vanished = Boolean.parseBoolean(dataMap.get("vanished").toString());
                    }
                    if (dataMap.get("godMode") != null) {
                        data.godMode = Boolean.parseBoolean(dataMap.get("godMode").toString());
                    }
                    if (dataMap.get("muted") != null) {
                        data.muted = Boolean.parseBoolean(dataMap.get("muted").toString());
                    }
                    if (dataMap.get("jailed") != null) {
                        data.jailed = Boolean.parseBoolean(dataMap.get("jailed").toString());
                    }
                    if (dataMap.get("afkTime") != null) {
                        data.afkTime = ((Number) dataMap.get("afkTime")).longValue();
                    }
                    if (dataMap.get("muteExpiry") != null) {
                        data.muteExpiry = ((Number) dataMap.get("muteExpiry")).longValue();
                    }
                    if (dataMap.get("jailExpiry") != null) {
                        data.jailExpiry = ((Number) dataMap.get("jailExpiry")).longValue();
                    }
                    
                    // Load settings
                    if (dataMap.get("settings") != null) {
                        Map<String, Object> settingsData = (Map<String, Object>) dataMap.get("settings");
                        data.settings.clear();
                        data.settings.putAll(settingsData);
                    }
                    
                    // Load homes
                    if (dataMap.get("homes") != null) {
                        Map<String, Map<String, Object>> homesData = (Map<String, Map<String, Object>>) dataMap.get("homes");
                        data.homes.clear();
                        for (Map.Entry<String, Map<String, Object>> entry : homesData.entrySet()) {
                            Map<String, Object> locData = entry.getValue();
                            LocationUtil.Location location = new LocationUtil.Location(
                                locData.get("world").toString(),
                                ((Number) locData.get("x")).doubleValue(),
                                ((Number) locData.get("y")).doubleValue(),
                                ((Number) locData.get("z")).doubleValue(),
                                ((Number) locData.get("yaw")).floatValue(),
                                ((Number) locData.get("pitch")).floatValue(),
                                ((Number) locData.get("timestamp")).longValue()
                            );
                            data.homes.put(entry.getKey(), location);
                        }
                    }
                    
                    // Load last location
                    if (dataMap.get("lastLocation") != null) {
                        Map<String, Object> lastLocData = (Map<String, Object>) dataMap.get("lastLocation");
                        data.lastLocation = new LocationUtil.Location(
                            lastLocData.get("world").toString(),
                            ((Number) lastLocData.get("x")).doubleValue(),
                            ((Number) lastLocData.get("y")).doubleValue(),
                            ((Number) lastLocData.get("z")).doubleValue(),
                            ((Number) lastLocData.get("yaw")).floatValue(),
                            ((Number) lastLocData.get("pitch")).floatValue(),
                            ((Number) lastLocData.get("timestamp")).longValue()
                        );
                    }
                    
                    LOGGER.debug("Loaded data for player {} from disk", playerUUID);
                } else {
                    // No saved data - this is a new player, create default data
                    getPlayerData(playerUUID); // This creates default data
                    LOGGER.debug("Created new data for player {} (first time)", playerUUID);
                }
            })
            .exceptionally(throwable -> {
                LOGGER.error("Failed to load data for player {}", playerUUID, throwable);
                return null;
            });
    }

    /**
     * Load player data from disk synchronously (for immediate use)
     */
    @SuppressWarnings("unchecked")
    public void loadPlayerDataSync(UUID playerUUID) {
        try {
            Map<String, Object> dataMap = storageManager.loadDataAsync("players", playerUUID.toString(), Map.class).get();
            if (dataMap != null) {
                PlayerData data = getPlayerData(playerUUID);
                
                // Load basic data
                if (dataMap.get("balance") != null) {
                    data.balance = new BigDecimal(dataMap.get("balance").toString());
                }
                if (dataMap.get("lastSeen") != null) {
                    data.lastSeen = ((Number) dataMap.get("lastSeen")).longValue();
                }
                if (dataMap.get("settings") != null) {
                    Map<String, Object> settingsData = (Map<String, Object>) dataMap.get("settings");
                    data.settings.clear();
                    data.settings.putAll(settingsData);
                }
                
                LOGGER.debug("Loaded data for player {} from disk (sync)", playerUUID);
            } else {
                // No saved data - this is a new player, create default data
                getPlayerData(playerUUID); // This creates default data
                LOGGER.debug("Created new data for player {} (first time, sync)", playerUUID);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to synchronously load data for player {}", playerUUID, e);
            // Create default data on error
            getPlayerData(playerUUID);
        }
    }    /**
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
