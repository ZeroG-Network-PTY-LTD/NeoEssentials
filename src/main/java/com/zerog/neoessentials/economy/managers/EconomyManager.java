package com.zerog.neoessentials.economy.managers;

import com.zerog.neoessentials.economy.EconomyConfig;
import com.zerog.neoessentials.config.GlobalConfig;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class EconomyManager {
    // Singleton instance
    private static EconomyManager instance;
    public static EconomyManager getInstance() {
        if (instance == null) instance = new EconomyManager();
        return instance;
    }

    // Replace balances map with Caffeine cache
    // private final Cache<UUID, BigDecimal> balancesCache = Caffeine.newBuilder()
    //     .maximumSize(10000) // TODO: make configurable
    //     .expireAfterAccess(1, TimeUnit.HOURS) // TODO: make configurable
    //     .build();
    // Use a simple ConcurrentHashMap instead
    private final ConcurrentHashMap<UUID, BigDecimal> balancesMap = new ConcurrentHashMap<>();
    private EconomyConfig config;
    // Store balances in root/neoessentials/balances.json
    private final File balancesFile = new File("neoessentials/balances.json");
    private final Gson gson = new Gson();
    private final ScheduledExecutorService saveExecutor = Executors.newSingleThreadScheduledExecutor();
    private volatile boolean saveQueued = false;

    // Track last activity (epoch millis) for each account
    private final ConcurrentHashMap<UUID, Long> lastActivityMap = new ConcurrentHashMap<>();
    private final File lastActivityFile = new File("neoessentials/balances_activity.json");

    private void loadBalances() {
        if (!balancesFile.getParentFile().exists()) {
            balancesFile.getParentFile().mkdirs();
        }
        if (!balancesFile.exists()) return;
        try (FileReader reader = new FileReader(balancesFile)) {
            Type type = new TypeToken<Map<String, String>>(){}.getType();
            Map<String, String> data = gson.fromJson(reader, type);
            if (data != null) {
                for (Map.Entry<String, String> entry : data.entrySet()) {
                    balancesMap.put(UUID.fromString(entry.getKey()), new BigDecimal(entry.getValue()));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveBalancesAtomic() {
        if (!balancesFile.getParentFile().exists()) {
            balancesFile.getParentFile().mkdirs();
        }
        try {
            // Write to temp file first
            File tempFile = new File(balancesFile.getAbsolutePath() + ".tmp");
            try (FileWriter writer = new FileWriter(tempFile)) {
                Map<String, String> data = new ConcurrentHashMap<>();
                for (Map.Entry<UUID, BigDecimal> entry : balancesMap.entrySet()) {
                    data.put(entry.getKey().toString(), entry.getValue().toPlainString());
                }
                gson.toJson(data, writer);
            }
            // Atomically move temp file to balancesFile
            Files.move(tempFile.toPath(), balancesFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void queueAsyncSave() {
        if (saveQueued) return;
        saveQueued = true;
        saveExecutor.execute(() -> {
            try {
                saveBalancesAtomic();
            } finally {
                saveQueued = false;
            }
        });
    }

    private void cleanupInactiveAccounts() {
        if (config == null || !config.cleanupInactiveAccounts) return;
        long now = System.currentTimeMillis();
        long thresholdMillis = config.inactiveAccountCleanupDays * 24L * 60L * 60L * 1000L;
        for (UUID uuid : balancesMap.keySet()) {
            Long lastActive = lastActivityMap.get(uuid);
            if (lastActive == null || (now - lastActive) >= thresholdMillis) {
                balancesMap.remove(uuid);
                lastActivityMap.remove(uuid);
            }
        }
        queueAsyncSave();
        queueAsyncSaveActivity();
    }

    private void loadLastActivity() {
        if (!lastActivityFile.getParentFile().exists()) {
            lastActivityFile.getParentFile().mkdirs();
        }
        if (!lastActivityFile.exists()) return;
        try (FileReader reader = new FileReader(lastActivityFile)) {
            Type type = new TypeToken<Map<String, Long>>(){}.getType();
            Map<String, Long> data = gson.fromJson(reader, type);
            if (data != null) {
                for (Map.Entry<String, Long> entry : data.entrySet()) {
                    lastActivityMap.put(UUID.fromString(entry.getKey()), entry.getValue());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveLastActivityAtomic() {
        if (!lastActivityFile.getParentFile().exists()) {
            lastActivityFile.getParentFile().mkdirs();
        }
        try {
            File tempFile = new File(lastActivityFile.getAbsolutePath() + ".tmp");
            try (FileWriter writer = new FileWriter(tempFile)) {
                Map<String, Long> data = new ConcurrentHashMap<>();
                for (Map.Entry<UUID, Long> entry : lastActivityMap.entrySet()) {
                    data.put(entry.getKey().toString(), entry.getValue());
                }
                gson.toJson(data, writer);
            }
            Files.move(tempFile.toPath(), lastActivityFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void queueAsyncSaveActivity() {
        saveExecutor.execute(this::saveLastActivityAtomic);
    }

    private EconomyManager() {
        // Check global config for module enable
        if (!GlobalConfig.isEconomyEnabled()) {
            // Economy is globally disabled, do not load balances or settings
            this.config = new EconomyConfig(); // Only for defaults, not for enable/disable
            return;
        }
        // Load config on startup
        File configFile = new File("config/neoessentials/economy.json");
        this.config = EconomyConfig.load(configFile);
        loadBalances();
        loadLastActivity();
        // Schedule periodic batch save every 5 minutes
        saveExecutor.scheduleAtFixedRate(this::saveBalancesAtomic, 5, 5, TimeUnit.MINUTES);
        saveExecutor.scheduleAtFixedRate(this::saveLastActivityAtomic, 5, 5, TimeUnit.MINUTES);
        // Schedule inactive account cleanup every hour (no time window)
        saveExecutor.scheduleAtFixedRate(this::cleanupInactiveAccounts, 1, 1, TimeUnit.HOURS);
    }

    public BigDecimal getBalance(UUID player) {
        BigDecimal cached = balancesMap.get(player);
        if (cached != null) return cached;
        return config.startingBalance;
    }

    public void setBalance(UUID player, BigDecimal amount) {
        if (!config.allowNegativeBalances && amount.compareTo(BigDecimal.ZERO) < 0) amount = BigDecimal.ZERO;
        if (amount.compareTo(config.maxBalance) > 0) amount = config.maxBalance;
        balancesMap.put(player, amount);
        lastActivityMap.put(player, System.currentTimeMillis());
        queueAsyncSave();
        queueAsyncSaveActivity();
    }

    public boolean addBalance(UUID player, BigDecimal amount) {
        BigDecimal current = getBalance(player);
        BigDecimal newAmount = current.add(amount);
        if (!config.allowNegativeBalances && newAmount.compareTo(BigDecimal.ZERO) < 0) return false;
        if (newAmount.compareTo(config.maxBalance) > 0) newAmount = config.maxBalance;
        balancesMap.put(player, newAmount);
        lastActivityMap.put(player, System.currentTimeMillis());
        queueAsyncSave();
        queueAsyncSaveActivity();
        return true;
    }

    public boolean subtractBalance(UUID player, BigDecimal amount) {
        BigDecimal current = getBalance(player);
        BigDecimal newAmount = current.subtract(amount);
        if (!config.allowNegativeBalances && newAmount.compareTo(BigDecimal.ZERO) < 0) return false;
        balancesMap.put(player, newAmount);
        lastActivityMap.put(player, System.currentTimeMillis());
        queueAsyncSave();
        queueAsyncSaveActivity();
        return true;
    }

    public Map<UUID, BigDecimal> getAllBalances() {
        return new ConcurrentHashMap<>(balancesMap);
    }

    public EconomyConfig getConfig() {
        return config;
    }

    public boolean isEnabled() {
        // Only check the global config now
        return GlobalConfig.isEconomyEnabled();
    }

    public String getCurrencySymbol() {
        if (config != null && config.currencySymbol != null) {
            return config.currencySymbol;
        }
        return "$"; // Default fallback
    }

    // TODO: Implement transaction logging, cache optimization, Vault compatibility
}