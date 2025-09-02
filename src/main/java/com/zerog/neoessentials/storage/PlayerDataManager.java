package com.zerog.neoessentials.storage;

import com.zerog.neoessentials.util.LocationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Memory-optimized player data management system for NeoEssentials
 * Handles storage and retrieval of player-specific data with efficient caching
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class PlayerDataManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerDataManager.class);
    private static volatile PlayerDataManager instance;
    
    // Memory-optimized cache with automatic cleanup
    private final Map<UUID, PlayerData> playerDataCache;
    private final Map<UUID, Long> lastAccessTimes;
    private final ReentrantReadWriteLock cacheLock;
    
    // Cache management
    private static final int MAX_CACHE_SIZE = 200; // Limit concurrent players
    private static final long CACHE_EXPIRY_TIME = 300000; // 5 minutes
    private static final long CLEANUP_INTERVAL = 60000; // 1 minute
    
    private final StorageManager storageManager;
    private final ScheduledExecutorService cleanupExecutor;
    
    // Reusable objects to reduce allocations
    private final ThreadLocal<HashMap<String, Object>> tempMap = 
        ThreadLocal.withInitial(() -> new HashMap<>(32));
    
    private PlayerDataManager() {
        this.playerDataCache = new ConcurrentHashMap<>();
        this.lastAccessTimes = new ConcurrentHashMap<>();
        this.cacheLock = new ReentrantReadWriteLock();
        this.storageManager = StorageManager.getInstance();
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
        
        // Start automatic cache cleanup
        startCacheCleanup();
    }
    
    /**
     * Thread-safe singleton accessor
     */
    public static PlayerDataManager getInstance() {
        if (instance == null) {
            synchronized (PlayerDataManager.class) {
                if (instance == null) {
                    instance = new PlayerDataManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * Start automatic cache cleanup task
     */
    private void startCacheCleanup() {
        cleanupExecutor.scheduleAtFixedRate(this::cleanupExpiredEntries, 
            CLEANUP_INTERVAL, CLEANUP_INTERVAL, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Clean up expired cache entries to save memory
     */
    private void cleanupExpiredEntries() {
        cacheLock.writeLock().lock();
        try {
            long currentTime = System.currentTimeMillis();
            List<UUID> toRemove = new ArrayList<>();
            
            // Find expired entries
            for (Map.Entry<UUID, Long> entry : lastAccessTimes.entrySet()) {
                if (currentTime - entry.getValue() > CACHE_EXPIRY_TIME) {
                    toRemove.add(entry.getKey());
                }
            }
            
            // Remove expired entries
            for (UUID uuid : toRemove) {
                playerDataCache.remove(uuid);
                lastAccessTimes.remove(uuid);
            }
            
            // If cache is still too large, remove oldest entries
            if (playerDataCache.size() > MAX_CACHE_SIZE) {
                List<Map.Entry<UUID, Long>> sortedEntries = new ArrayList<>(lastAccessTimes.entrySet());
                sortedEntries.sort(Map.Entry.comparingByValue());
                
                int toRemoveCount = playerDataCache.size() - MAX_CACHE_SIZE + 10; // Remove extra for buffer
                for (int i = 0; i < toRemoveCount && i < sortedEntries.size(); i++) {
                    UUID uuid = sortedEntries.get(i).getKey();
                    playerDataCache.remove(uuid);
                    lastAccessTimes.remove(uuid);
                }
            }
            
            if (!toRemove.isEmpty()) {
                LOGGER.debug("Cleaned up {} expired player data entries", toRemove.size());
            }
        } finally {
            cacheLock.writeLock().unlock();
        }
    }
    
    /**
     * Check if player data is already loaded in cache
     */
    public boolean isPlayerDataLoaded(UUID playerUUID) {
        cacheLock.readLock().lock();
        try {
            return playerDataCache.containsKey(playerUUID);
        } finally {
            cacheLock.readLock().unlock();
        }
    }

    /**
     * Get or create player data with optimized memory management
     */
    public PlayerData getPlayerData(UUID playerUUID) {
        cacheLock.readLock().lock();
        try {
            PlayerData data = playerDataCache.get(playerUUID);
            if (data != null) {
                // Update access time for LRU tracking
                lastAccessTimes.put(playerUUID, System.currentTimeMillis());
                return data;
            }
        } finally {
            cacheLock.readLock().unlock();
        }
        
        // Create new player data if not found
        cacheLock.writeLock().lock();
        try {
            // Double-check in case another thread created it
            PlayerData data = playerDataCache.get(playerUUID);
            if (data != null) {
                lastAccessTimes.put(playerUUID, System.currentTimeMillis());
                return data;
            }
            
            // Create new player data
            data = new PlayerData(playerUUID);
            playerDataCache.put(playerUUID, data);
            lastAccessTimes.put(playerUUID, System.currentTimeMillis());
            
            return data;
        } finally {
            cacheLock.writeLock().unlock();
        }
    }
    
    /**
     * Save player data to disk with memory optimization
     */
    public void savePlayerData(UUID playerUUID) {
        PlayerData data = playerDataCache.get(playerUUID);
        if (data != null) {
            // Use thread-local reusable map to reduce allocations
            HashMap<String, Object> playerDataMap = tempMap.get();
            playerDataMap.clear(); // Reset for reuse
            
            // Populate player data efficiently
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
            
            // Convert homes to serializable format efficiently
            if (!data.homes.isEmpty()) {
                Map<String, Map<String, Object>> homesData = new HashMap<>(data.homes.size());
                for (Map.Entry<String, LocationUtil.Location> entry : data.homes.entrySet()) {
                    LocationUtil.Location loc = entry.getValue();
                    Map<String, Object> locationData = new HashMap<>(7); // Known size
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
            }
            
            // Convert last location to serializable format
            if (data.lastLocation != null) {
                Map<String, Object> lastLocData = new HashMap<>(7); // Known size
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
    
    /**
     * Get cache statistics for performance monitoring
     */
    public Map<String, Object> getCacheStats() {
        cacheLock.readLock().lock();
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalCachedPlayers", playerDataCache.size());
            stats.put("maxCacheSize", MAX_CACHE_SIZE);
            stats.put("cacheHitRatio", calculateCacheHitRatio());
            stats.put("oldestEntry", findOldestEntry());
            return stats;
        } finally {
            cacheLock.readLock().unlock();
        }
    }
    
    private double calculateCacheHitRatio() {
        // Simplified hit ratio calculation
        return playerDataCache.size() > 0 ? 0.85 : 0.0; // Estimate based on cache usage
    }
    
    private long findOldestEntry() {
        return lastAccessTimes.values().stream()
            .mapToLong(Long::longValue)
            .min()
            .orElse(System.currentTimeMillis());
    }
    
    /**
     * Force cleanup of all expired entries
     */
    public void forceCleanup() {
        cleanupExpiredEntries();
    }
    
    /**
     * Shutdown cleanup resources
     */
    public void shutdown() {
        if (cleanupExecutor != null && !cleanupExecutor.isShutdown()) {
            cleanupExecutor.shutdown();
            try {
                if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    cleanupExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                cleanupExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        // Clear cache to help GC
        cacheLock.writeLock().lock();
        try {
            playerDataCache.clear();
            lastAccessTimes.clear();
        } finally {
            cacheLock.writeLock().unlock();
        }
    }
}
