package com.zerog.neoessentials.api.economy;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.io.*;
import java.nio.file.*;
import java.util.Optional;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import net.neoforged.neoforge.common.NeoForge;
import com.zerog.neoessentials.api.event.EconomyDepositEvent;
import com.zerog.neoessentials.api.event.EconomyWithdrawEvent;

/**
 * Implementation of the EconomyService interface.
 * Handles player balances and persistent storage.
 */
public class EconomyServiceImpl implements EconomyService {
    private final Map<UUID, Double> balances = new ConcurrentHashMap<>();
    private final Path dataFile;
    private final Gson gson = new Gson();

    public EconomyServiceImpl(Path dataFile) {
        this.dataFile = dataFile;
        loadBalances();
    }

    @Override
    public double getBalance(UUID playerId) {
        return balances.getOrDefault(playerId, 0.0);
    }

    @Override
    public boolean deposit(UUID playerId, double amount) {
        if (amount <= 0) return false;
        balances.merge(playerId, amount, Double::sum);
        saveBalances();
        NeoForge.EVENT_BUS.post(new EconomyDepositEvent(playerId, amount));
        return true;
    }

    @Override
    public boolean withdraw(UUID playerId, double amount) {
        if (amount <= 0) return false;
        boolean result = updateBalanceIfSufficient(playerId, amount);
        if (result) {
            NeoForge.EVENT_BUS.post(new EconomyWithdrawEvent(playerId, amount));
        }
        return result;
    }

    @Override
    public boolean setBalance(UUID playerId, double amount) {
        if (amount < 0) return false;
        balances.put(playerId, amount);
        saveBalances();
        return true;
    }

    @Override
    public boolean resetBalance(UUID playerId) {
        balances.put(playerId, 0.0);
        saveBalances();
        return true;
    }

    @Override
    public boolean hasAccount(UUID playerId) {
        return balances.containsKey(playerId);
    }

    @Override
    public boolean createAccount(UUID playerId) {
        if (balances.containsKey(playerId)) return false;
        balances.put(playerId, 0.0);
        saveBalances();
        return true;
    }

    @Override
    public boolean deleteAccount(UUID playerId) {
        if (!balances.containsKey(playerId)) return false;
        balances.remove(playerId);
        saveBalances();
        return true;
    }

    @Override
    public String format(double amount) {
        return String.format("%s%.2f", getCurrencySymbol(), amount);
    }

    @Override
    public String getCurrencySymbol() {
        return "$";
    }

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

    private boolean updateBalanceIfSufficient(UUID playerId, double amount) {
        synchronized (balances) {
            double current = getBalance(playerId);
            if (current < amount) return false;
            balances.put(playerId, current - amount);
            saveBalances();
            return true;
        }
    }

    public Optional<Double> getBalanceOptional(UUID playerId) {
        return Optional.ofNullable(balances.get(playerId));
    }
}
