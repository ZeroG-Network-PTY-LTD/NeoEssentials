
package com.zerog.neoessentials.economy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.zerog.neoessentials.config.EconomyConfig;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.*;
import java.math.BigDecimal;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EconomyManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(EconomyManager.class);
    private static EconomyManager instance;
    private static final String PLAYER_ECONOMY_DIR = "neoessentials/economy/";
    private static final String GLOBAL_ECONOMY_CONFIG = "config/neoessentials/economy.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private EconomyConfig config;
    private final Map<UUID, BigDecimal> playerBalances = new HashMap<>();
    private final Map<UUID, Boolean> playerPayToggle = new HashMap<>();

    private EconomyManager() {
        this.config = EconomyConfig.load(new File(GLOBAL_ECONOMY_CONFIG));
    }

    public static synchronized EconomyManager getInstance() {
        if (instance == null) {
            instance = new EconomyManager();
        }
        return instance;
    }

    public String getCurrencySymbol() {
        return config.currencySymbol;
    }

    public double getStartingBalance() {
        return config.startingBalance.doubleValue();
    }

    public BigDecimal getBalance(UUID playerId) {
        return playerBalances.getOrDefault(playerId, config.startingBalance);
    }

    public void setBalance(UUID playerId, BigDecimal balance) {
        playerBalances.put(playerId, balance);
    }

    public void addBalance(UUID playerId, BigDecimal amount) {
        playerBalances.put(playerId, getBalance(playerId).add(amount));
    }

    public void subtractBalance(UUID playerId, BigDecimal amount) {
        playerBalances.put(playerId, getBalance(playerId).subtract(amount));
    }

    public Map<UUID, BigDecimal> getAllBalances() {
        return new HashMap<>(playerBalances);
    }

    public boolean isEnabled() {
        return true; // Or add a config flag if you want to enable/disable economy
    }

    public EconomyConfig getConfig() {
        return config;
    }

    public BigDecimal getPlayerBalance(UUID playerId) {
        return getBalance(playerId);
    }

    public void setPlayerBalance(UUID playerId, double balance) {
        setBalance(playerId, BigDecimal.valueOf(balance));
    }

    public void loadPlayerEconomy(UUID playerId) {
        try {
            Path dir = Paths.get(PLAYER_ECONOMY_DIR);
            File file = dir.resolve(playerId + ".json").toFile();
            if (file.exists()) {
                try (Reader reader = new FileReader(file)) {
                    PlayerEconomyData econ = GSON.fromJson(reader, PlayerEconomyData.class);
                    if (econ != null) {
                        playerBalances.put(playerId, BigDecimal.valueOf(econ.balance));
                        // Load paytoggle state, default to true if missing
                        playerPayToggle.put(playerId, econ.acceptingPayments == null ? true : econ.acceptingPayments);
                    }
                }
            } else {
                playerBalances.put(playerId, config.startingBalance);
                playerPayToggle.put(playerId, true);
            }
        } catch (IOException e) {
                LOGGER.error("Failed to load player economy for {}", playerId, e);
        }
    }

    public void savePlayerEconomy(UUID playerId) {
        BigDecimal balance = playerBalances.get(playerId);
        if (balance == null) return;
        boolean accepting = isAcceptingPayments(playerId);
        try {
            Path dir = Paths.get(PLAYER_ECONOMY_DIR);
            if (!Files.exists(dir)) Files.createDirectories(dir);
            File file = dir.resolve(playerId + ".json").toFile();
            try (Writer writer = new FileWriter(file)) {
                GSON.toJson(new PlayerEconomyData(balance.doubleValue(), accepting), writer);
            }
        } catch (IOException e) {
                LOGGER.error("Failed to save player economy for {}", playerId, e);
        }
    }

    public void saveAllPlayerEconomy() {
        for (UUID uuid : playerBalances.keySet()) {
            savePlayerEconomy(uuid);
        }
    }

    // PayToggle: true = accepting payments, false = not accepting
    public boolean isAcceptingPayments(UUID playerId) {
        return playerPayToggle.getOrDefault(playerId, true);
    }

    public void setAcceptingPayments(UUID playerId, boolean accepting) {
        playerPayToggle.put(playerId, accepting);
    }

    public boolean toggleAcceptingPayments(UUID playerId) {
        boolean current = isAcceptingPayments(playerId);
        playerPayToggle.put(playerId, !current);
        return !current;
    }

    private static class PlayerEconomyData {
        double balance;
        Boolean acceptingPayments;
        PlayerEconomyData(double balance, boolean acceptingPayments) {
            this.balance = balance;
            this.acceptingPayments = acceptingPayments;
        }
    }
    
    /**
     * Shutdown method to clean up resources.
     */
    public void shutdown() {
        LOGGER.info("Shutting down main EconomyManager...");
        // This manager doesn't have executor services, so just save all data
        saveAllPlayerEconomy();
        LOGGER.info("Main EconomyManager shutdown complete.");
    }
}