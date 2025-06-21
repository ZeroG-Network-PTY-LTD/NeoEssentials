package com.zerog.neoessentials.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zerog.neoessentials.NeoEssentials;
import net.minecraft.server.level.ServerPlayer;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the economy system for the NeoEssentials mod.
 */
public class EconomyManager {
    private static final String ECONOMY_DATA_FILE = "neoessentials/economy.json";
    
    // Map of player UUID to balance
    private final Map<UUID, Double> balances = new ConcurrentHashMap<>();
    
    private final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();
      /**
     * Initialize the economy manager
     */
    public void initialize() {
        NeoEssentials.LOGGER.info("Initializing NeoEssentials Economy Manager");
        
        // Load existing economy data
        loadEconomyData();
    }
    
    /**
     * Save all economy data to disk
     */
    public void saveAll() {
        NeoEssentials.LOGGER.info("Saving economy data");
        
        try {
            File dataFolder = new File("neoessentials");
            if (!dataFolder.exists() && !dataFolder.mkdirs()) {
                NeoEssentials.LOGGER.error("Failed to create neoessentials directory");
                return;
            }
            
            File dataFile = new File(ECONOMY_DATA_FILE);
            if (!dataFile.exists()) {
                if (!dataFile.createNewFile()) {
                    NeoEssentials.LOGGER.error("Failed to create economy data file");
                    return;
                }
            }
            
            try (FileWriter writer = new FileWriter(dataFile)) {
                gson.toJson(balances, writer);
            }
            
            NeoEssentials.LOGGER.info("Economy data saved successfully");
        } catch (IOException e) {
            NeoEssentials.LOGGER.error("Error saving economy data", e);
        }
    }
    
    /**
     * Load economy data from disk
     */
    @SuppressWarnings("unchecked")
    private void loadEconomyData() {
        File dataFile = new File(ECONOMY_DATA_FILE);
        
        if (!dataFile.exists()) {
            NeoEssentials.LOGGER.info("No existing economy data found, starting fresh");
            return;
        }
        
        try (FileReader reader = new FileReader(dataFile)) {
            Map<String, Double> rawData = gson.fromJson(reader, Map.class);
            
            if (rawData != null) {
                // Convert the string keys back to UUIDs
                rawData.forEach((key, value) -> {
                    try {
                        UUID uuid = UUID.fromString(key);
                        balances.put(uuid, value);
                    } catch (IllegalArgumentException e) {
                        NeoEssentials.LOGGER.error("Invalid UUID in economy data: " + key);
                    }
                });
                
                NeoEssentials.LOGGER.info("Loaded " + balances.size() + " player economy records");
            }
        } catch (IOException e) {
            NeoEssentials.LOGGER.error("Error loading economy data", e);
        }
    }
    
    /**
     * Get the balance of a player
     * 
     * @param playerId The UUID of the player
     * @return The player's balance
     */
    public double getBalance(UUID playerId) {
        return balances.getOrDefault(playerId, 0.0);
    }
    
    /**
     * Set the balance of a player
     * 
     * @param playerId The UUID of the player
     * @param amount The new balance amount
     */
    public void setBalance(UUID playerId, double amount) {
        if (amount < 0) {
            amount = 0;
        }
        balances.put(playerId, amount);
    }
    
    /**
     * Add to a player's balance
     * 
     * @param playerId The UUID of the player
     * @param amount The amount to add
     * @return The new balance
     */
    public double addBalance(UUID playerId, double amount) {
        if (amount <= 0) {
            return getBalance(playerId);
        }
        
        double currentBalance = getBalance(playerId);
        double newBalance = currentBalance + amount;
        balances.put(playerId, newBalance);
        return newBalance;
    }
    
    /**
     * Remove from a player's balance
     * 
     * @param playerId The UUID of the player
     * @param amount The amount to remove
     * @return True if the player had enough money, false otherwise
     */
    public boolean removeBalance(UUID playerId, double amount) {
        if (amount <= 0) {
            return true;
        }
        
        double currentBalance = getBalance(playerId);
        if (currentBalance < amount) {
            return false;
        }
        
        double newBalance = currentBalance - amount;
        balances.put(playerId, newBalance);
        return true;
    }
    
    /**
     * Transfer money from one player to another
     * 
     * @param fromId The UUID of the player sending money
     * @param toId The UUID of the player receiving money
     * @param amount The amount to transfer
     * @return True if the transfer was successful, false otherwise
     */    public boolean transfer(UUID fromId, UUID toId, double amount) {
        if (amount <= 0) {
            return false;
        }
        
        // Check if from player has enough money
        if (!removeBalance(fromId, amount)) {
            return false;
        }
        
        // Add money to the target player
        addBalance(toId, amount);
        return true;
    }
      /**
     * Gets a list of top balances for the baltop command
     * 
     * @param limit The maximum number of entries to return
     * @return A map of UUIDs to balances, sorted by balance descending
     */
    public Map<UUID, Double> getTopBalances(int limit) {
        return balances.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .limit(limit)
                .collect(java.util.stream.Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (e1, e2) -> e1,
                    java.util.LinkedHashMap::new
                ));
    }
    
    /**
     * Get all balances
     * 
     * @return Map of player UUIDs to their balances
     */
    public Map<UUID, Double> getAllBalances() {
        return new ConcurrentHashMap<>(balances);
    }
    
    /**
     * Get a player's name from their UUID
     * 
     * @param playerId The UUID of the player
     * @return The player's name, or null if not found
     */
    public String getPlayerName(UUID playerId) {
        // Try to find online player first
        ServerPlayer player = NeoEssentials.getInstance().getServer().getPlayerList().getPlayer(playerId);
        if (player != null) {
            return player.getScoreboardName();
        }
        
        // Try to get from offline player data
        return NeoEssentials.getInstance().getServer().getProfileCache()
            .get(playerId)
            .map(profile -> profile.getName())
            .orElse("Unknown Player");
    }
}
