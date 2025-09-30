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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Singleton manager for NeoEssentials services and player data.
 */
public class NeoEssentialsManager {
    private static NeoEssentialsManager instance;

    // Player data storage
    private final Map<UUID, PlayerData> playerDataMap = new HashMap<>();
    // Service APIs
    private EconomyService economyService;

    private static final String PLAYERDATA_DIR = "config/neoessentials/playerdata/";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Private constructor for singleton pattern.
     */
    private NeoEssentialsManager() {
        // Initialize EconomyServiceImpl with persistent storage
        this.economyService = new EconomyServiceImpl(
            Paths.get("neoessentials/balances.json")
        );
    }

    /**
     * Gets the singleton instance of the manager.
     * @return NeoEssentialsManager instance
     */
    public static synchronized NeoEssentialsManager getInstance() {
        if (instance == null) {
            instance = new NeoEssentialsManager();
        }
        return instance;
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
     * Player data for homes, balances, warps, and economy toggles.
     */
    public static class PlayerData {
        public Map<String, Object> homes = new HashMap<>();
        public double balance = 0.0;
        public Map<String, Object> warps = new HashMap<>();
        // Economy toggles
        private boolean payConfirmEnabled = false;
        private boolean payAcceptEnabled = true;
        // Add more fields as needed

        public boolean isPayConfirmEnabled() { return payConfirmEnabled; }
        public void setPayConfirmEnabled(boolean enabled) { this.payConfirmEnabled = enabled; }
        public boolean isPayAcceptEnabled() { return payAcceptEnabled; }
        public void setPayAcceptEnabled(boolean enabled) { this.payAcceptEnabled = enabled; }
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
            e.printStackTrace();
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
            e.printStackTrace();
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
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}