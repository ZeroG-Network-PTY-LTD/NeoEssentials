
package com.zerog.neoessentials;

// import com.zerog.neoessentials.api.TeleportService;
import com.zerog.neoessentials.api.economy.EconomyService;
import com.zerog.neoessentials.api.economy.EconomyServiceImpl;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread-safe singleton manager for NeoEssentials services and player data.
 */
public class NeoEssentialsManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(NeoEssentialsManager.class);

    // Thread-safe singleton using Bill Pugh pattern
    private static class SingletonHolder {
        private static final NeoEssentialsManager INSTANCE = new NeoEssentialsManager();
    }

    public static NeoEssentialsManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    // Thread-safe player data storage
    private final Map<UUID, PlayerData> playerDataMap = new ConcurrentHashMap<>();
    // Service APIs
    private EconomyService economyService;

    private static final String PLAYERDATA_DIR = com.zerog.neoessentials.util.ResourceUtil.CONFIG_DIR + "playerdata/";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Private constructor for singleton pattern.
     */
    private NeoEssentialsManager() {
        // Initialize EconomyServiceImpl with persistent storage
        // Note: This is for API compatibility - actual economy is handled by EconomyManager
        this.economyService = new EconomyServiceImpl(
            com.zerog.neoessentials.util.ResourceUtil.getDataPath("balances.json")
        );
    }

    /**
     * Registers a command (stub for future expansion).
     * @param command Command object
     */
    public void registerCommand(Object command) {
        // Implementation for registering commands
    }

    /**
     * Gets or creates player data for a given player.
     * @param playerId Player UUID
     * @return PlayerData instance
     */
    public PlayerData getPlayerData(UUID playerId) {
        return playerDataMap.computeIfAbsent(playerId, k -> new PlayerData());
    }



    /**
     * Sets the economy service.
     * @param service EconomyService implementation
     */
    public void setEconomyService(EconomyService service) {
        this.economyService = service;
    }

    /**
     * Gets the economy service.
     * @return EconomyService
     */
    public EconomyService getEconomyService() {
        return economyService;
    }

    /**
     * Player data for homes, warps, teleportation, and non-economy settings.
     * Note: Economy data (balance, pay toggles) is handled by EconomyManager separately.
     */
    public static class PlayerData {
        public Map<String, Object> homes = new ConcurrentHashMap<>();
        public Map<String, Object> warps = new ConcurrentHashMap<>();
        public Map<String, Object> mail = new ConcurrentHashMap<>();
        
        // Non-economy toggles and settings
        private boolean afkStatus = false;
        private boolean vanishMode = false;
        private boolean godMode = false;
        private boolean flyMode = false;
        private long lastActivity = System.currentTimeMillis();
        private String lastLocation = null;
        
        // Teleportation settings  
        private boolean tpToggle = true;
        private boolean msgToggle = true;
        
        // Social features
        private java.util.List<String> ignoreList = new java.util.ArrayList<>();
        
        // Getters and setters
        public boolean isAfkStatus() { return afkStatus; }
        public void setAfkStatus(boolean afk) { this.afkStatus = afk; }
        
        public boolean isVanishMode() { return vanishMode; }
        public void setVanishMode(boolean vanish) { this.vanishMode = vanish; }
        
        public boolean isGodMode() { return godMode; }
        public void setGodMode(boolean god) { this.godMode = god; }
        
        public boolean isFlyMode() { return flyMode; }
        public void setFlyMode(boolean fly) { this.flyMode = fly; }
        
        public long getLastActivity() { return lastActivity; }
        public void setLastActivity(long time) { this.lastActivity = time; }
        
        public String getLastLocation() { return lastLocation; }
        public void setLastLocation(String location) { this.lastLocation = location; }
        
        public boolean isTpToggle() { return tpToggle; }
        public void setTpToggle(boolean enabled) { this.tpToggle = enabled; }
        
        public boolean isMsgToggle() { return msgToggle; }
        public void setMsgToggle(boolean enabled) { this.msgToggle = enabled; }
        
        public java.util.List<String> getIgnoreList() { return ignoreList; }
        public void addToIgnoreList(String player) { ignoreList.add(player); }
        public void removeFromIgnoreList(String player) { ignoreList.remove(player); }
    }

    public void savePlayerData(UUID playerId) {
        PlayerData data = playerDataMap.get(playerId);
        if (data == null) return;
        try {
            Path dir = Paths.get(PLAYERDATA_DIR);
            if (!Files.exists(dir)) Files.createDirectories(dir);
            File file = dir.resolve(playerId + ".json").toFile();
            try (Writer writer = new FileWriter(file)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save player data for {}", playerId, e);
        }
    }

    public void loadPlayerData(UUID playerId) {
        try {
            Path dir = Paths.get(PLAYERDATA_DIR);
            File file = dir.resolve(playerId + ".json").toFile();
            if (!file.exists()) return;
            try (Reader reader = new FileReader(file)) {
                PlayerData data = GSON.fromJson(reader, PlayerData.class);
                playerDataMap.put(playerId, data);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load player data for {}", playerId, e);
        }
    }

    public void saveAllPlayerData() {
        for (UUID uuid : playerDataMap.keySet()) {
            savePlayerData(uuid);
        }
    }

    public void loadAllPlayerData() {
        try {
            Path dir = Paths.get(PLAYERDATA_DIR);
            if (!Files.exists(dir)) return;
            Files.list(dir).filter(p -> p.toString().endsWith(".json")).forEach(p -> {
                try (Reader reader = new FileReader(p.toFile())) {
                    PlayerData data = GSON.fromJson(reader, PlayerData.class);
                    String fileName = p.getFileName().toString();
                    String uuidStr = fileName.substring(0, fileName.length() - 5); // remove .json
                    UUID uuid = UUID.fromString(uuidStr);
                    playerDataMap.put(uuid, data);
                } catch (Exception e) {
                    LOGGER.error("Failed to load individual player data file", e);
                }
            });
        } catch (IOException e) {
            LOGGER.error("Failed to load all player data", e);
        }
    }
}