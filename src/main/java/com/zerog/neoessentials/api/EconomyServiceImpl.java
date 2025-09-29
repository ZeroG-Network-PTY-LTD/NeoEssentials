package com.zerog.neoessentials.api;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.io.*;
import java.nio.file.*;
import java.util.Optional;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

/**
 * Implementation of the EconomyService interface.
 * Handles player balances and persistent storage.
 */
public class EconomyServiceImpl implements EconomyService {
    private final Map<UUID, Double> balances = new ConcurrentHashMap<>();
    private final Path dataFile;
    private final Gson gson = new Gson();

    /**
     * Constructs the EconomyServiceImpl with the given data file path.
     * @param dataFile Path to the balances JSON file.
     */
    public EconomyServiceImpl(Path dataFile) {
        this.dataFile = dataFile;
        loadBalances();
    }

    /**
     * Gets the balance for a player.
     * @param playerId Player UUID
     * @return Player's balance, or 0.0 if not found
     */
    @Override
    public double getBalance(UUID playerId) {
        return balances.getOrDefault(playerId, 0.0);
    }

    /**
     * Deposits an amount to a player's balance.
     * @param playerId Player UUID
     * @param amount Amount to deposit (must be positive)
     * @return true if successful, false otherwise
     */
    @Override
    public boolean deposit(UUID playerId, double amount) {
        if (amount <= 0) return false;
        balances.merge(playerId, amount, Double::sum);
        saveBalances();
        return true;
    }

    /**
     * Withdraws an amount from a player's balance.
     * @param playerId Player UUID
     * @param amount Amount to withdraw (must be positive and less than or equal to balance)
     * @return true if successful, false otherwise
     */
    @Override
    public boolean withdraw(UUID playerId, double amount) {
        if (amount <= 0) return false;
        return updateBalanceIfSufficient(playerId, amount);
    }

    /**
     * Sets the balance for a player. This can be used for admin operations.
     * @param playerId Player UUID
     * @param amount New balance amount (must be non-negative)
     * @return true if successful, false otherwise
     */
    @Override
    public boolean setBalance(UUID playerId, double amount) {
        if (amount < 0) return false;
        balances.put(playerId, amount);
        saveBalances();
        return true;
    }

    /**
     * Resets the balance for a player to zero. This can be used for admin operations.
     * @param playerId Player UUID
     * @return true if successful, false otherwise
     */
    @Override
    public boolean resetBalance(UUID playerId) {
        balances.put(playerId, 0.0);
        saveBalances();
        return true;
    }

    /**
     * Loads balances from the JSON file.
     */
    private void loadBalances() {
        if (!Files.exists(dataFile)) return;
        try (Reader reader = Files.newBufferedReader(dataFile)) {
            java.lang.reflect.Type type = new TypeToken<Map<String, Double>>(){}.getType();
            Map<String, Double> raw = gson.fromJson(reader, type);
            if (raw != null) {
                for (Map.Entry<String, Double> entry : raw.entrySet()) {
                    balances.put(UUID.fromString(entry.getKey()), entry.getValue());
                }
            }
        } catch (IOException e) {
            System.err.println("[NeoEssentials] " + java.time.LocalDate.now() + " - " +
                "Error loading balances: " + e.getMessage());
        }
    }

    /**
     * Saves balances to the JSON file.
     */
    private void saveBalances() {
        Map<String, Double> raw = new ConcurrentHashMap<>();
        for (Map.Entry<UUID, Double> entry : balances.entrySet()) {
            raw.put(entry.getKey().toString(), entry.getValue());
        }
        try {
            Files.createDirectories(dataFile.getParent());
            try (Writer writer = Files.newBufferedWriter(dataFile)) {
                gson.toJson(raw, writer);
            }
        } catch (IOException e) {
            System.err.println("[NeoEssentials] " + java.time.LocalDate.now() + " - " +
                "Error saving balances: " + e.getMessage());
        }
    }

    /**
     * Atomically updates the balance if the player has enough funds.
     * @param playerId Player UUID
     * @param amount Amount to withdraw
     * @return true if successful, false otherwise
     */
    private boolean updateBalanceIfSufficient(UUID playerId, double amount) {
        synchronized (balances) {
            double current = getBalance(playerId);
            if (current < amount) return false;
            balances.put(playerId, current - amount);
            saveBalances();
            return true;
        }
    }

    /**
     * Gets the balance as an Optional for null-safety.
     * @param playerId Player UUID
     * @return Optional containing the balance, or empty if not found
     */
    public Optional<Double> getBalanceOptional(UUID playerId) {
        return Optional.ofNullable(balances.get(playerId));
    }
}