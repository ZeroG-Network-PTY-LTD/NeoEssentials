
package com.zerog.neoessentials.economy.managers;

import com.zerog.neoessentials.config.EconomyConfig;
import com.zerog.neoessentials.economy.EconomyTransactionLogger;
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

import java.nio.file.StandardCopyOption;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EconomyManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(EconomyManager.class);
    
    // Thread-safe singleton using Bill Pugh Singleton Pattern
    private static class SingletonHolder {
        private static final EconomyManager INSTANCE = new EconomyManager();
    }
    
    public static EconomyManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    // Use Caffeine cache for balances
    private Cache<UUID, BigDecimal> balancesCache;
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
                    balancesCache.put(UUID.fromString(entry.getKey()), new BigDecimal(entry.getValue()));
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load balances from file", e);
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
                for (Map.Entry<UUID, BigDecimal> entry : balancesCache.asMap().entrySet()) {
                    data.put(entry.getKey().toString(), entry.getValue().toPlainString());
                }
                gson.toJson(data, writer);
            }
            // Atomically move temp file to balancesFile
            Files.move(tempFile.toPath(), balancesFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            LOGGER.error("Failed to save balances to file", e);
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
        for (UUID uuid : balancesCache.asMap().keySet()) {
            Long lastActive = lastActivityMap.get(uuid);
            if (lastActive == null || (now - lastActive) >= thresholdMillis) {
                balancesCache.invalidate(uuid);
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
            LOGGER.error("Failed to load last activity data", e);
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
            LOGGER.error("Failed to save last activity data", e);
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
        // Initialize Caffeine cache with config values and statistics
        balancesCache = Caffeine.newBuilder()
            .maximumSize(config.cacheMaximumSize)
            .expireAfterAccess(config.cacheExpireAfterAccessMinutes, TimeUnit.MINUTES)
            .recordStats() // Enable statistics for monitoring
            .removalListener((uuid, balance, cause) -> 
                LOGGER.debug("Cache evicted: {} -> {} (cause: {})", uuid, balance, cause))
            .build();
        loadBalances();
        loadLastActivity();
        // Schedule periodic batch save every 5 minutes
        saveExecutor.scheduleAtFixedRate(this::saveBalancesAtomic, 5, 5, TimeUnit.MINUTES);
        saveExecutor.scheduleAtFixedRate(this::saveLastActivityAtomic, 5, 5, TimeUnit.MINUTES);
        
        // Schedule periodic cache statistics logging every 30 minutes
        saveExecutor.scheduleAtFixedRate(this::logCacheMetrics, 30, 30, TimeUnit.MINUTES);
        // Schedule inactive account cleanup every hour (no time window)
        saveExecutor.scheduleAtFixedRate(this::cleanupInactiveAccounts, 1, 1, TimeUnit.HOURS);
    }

    public BigDecimal getBalance(UUID player) {
        BigDecimal cached = balancesCache.getIfPresent(player);
        if (cached != null) return cached;
        return config.startingBalance;
    }

    public void setBalance(UUID player, BigDecimal amount) {
        if (!config.allowNegativeBalances && amount.compareTo(BigDecimal.ZERO) < 0) amount = BigDecimal.ZERO;
        if (amount.compareTo(config.maxBalance) > 0) amount = config.maxBalance;
        BigDecimal oldAmount = getBalance(player);
        balancesCache.put(player, amount);
        lastActivityMap.put(player, System.currentTimeMillis());
        queueAsyncSave();
        queueAsyncSaveActivity();
        // Log transaction
        EconomyTransactionLogger.log("SET", player.toString(), "SERVER", amount.toPlainString(), "Set balance (was: " + oldAmount.toPlainString() + ")");
    }

    public boolean addBalance(UUID player, BigDecimal amount) {
        BigDecimal current = getBalance(player);
        BigDecimal newAmount = current.add(amount);
        if (!config.allowNegativeBalances && newAmount.compareTo(BigDecimal.ZERO) < 0) return false;
        if (newAmount.compareTo(config.maxBalance) > 0) newAmount = config.maxBalance;
        balancesCache.put(player, newAmount);
        lastActivityMap.put(player, System.currentTimeMillis());
        queueAsyncSave();
        queueAsyncSaveActivity();
        // Log transaction
        EconomyTransactionLogger.log("ADD", "SERVER", player.toString(), amount.toPlainString(), "Add to balance");
        return true;
    }

    public boolean subtractBalance(UUID player, BigDecimal amount) {
        BigDecimal current = getBalance(player);
        BigDecimal newAmount = current.subtract(amount);
        if (!config.allowNegativeBalances && newAmount.compareTo(BigDecimal.ZERO) < 0) return false;
        balancesCache.put(player, newAmount);
        lastActivityMap.put(player, System.currentTimeMillis());
        queueAsyncSave();
        queueAsyncSaveActivity();
        // Log transaction
        EconomyTransactionLogger.log("SUBTRACT", player.toString(), "SERVER", amount.toPlainString(), "Subtract from balance");
        return true;
    }

    public Map<UUID, BigDecimal> getAllBalances() {
        return new ConcurrentHashMap<>(balancesCache.asMap());
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

    // Vault compatibility stub removed; use EconomyService API instead

    /**
     * Manually optimize the balances cache by cleaning up expired or low-activity entries.
     * This can be called after large batch operations or periodically for memory efficiency.
     */
    public void optimizeCache() {
        // Remove entries that are expired according to Caffeine's policy
        balancesCache.cleanUp();
        // Optionally, remove accounts with no activity for a long time (already handled by cleanupInactiveAccounts)
    }

    /**
     * Returns cache statistics for monitoring and tuning.
     */
    public String getCacheStats() {
        return balancesCache.stats().toString();
    }
    
    /**
     * Logs cache metrics for monitoring and debugging.
     */
    private void logCacheMetrics() {
        var stats = balancesCache.stats();
        LOGGER.info("EconomyManager Cache Metrics - Hit Rate: {:.2f}%, Evictions: {}, Size: {}", 
                   stats.hitRate() * 100, 
                   stats.evictionCount(), 
                   balancesCache.estimatedSize());
    }
    
    /**
     * Shuts down the economy manager and its executor services properly.
     */
    public void shutdown() {
        LOGGER.info("Shutting down EconomyManager...");
        
        // Save any pending data
        saveBalancesAtomic();
        saveLastActivityAtomic();
        
        // Shutdown executor service
        saveExecutor.shutdown();
        try {
            if (!saveExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                LOGGER.warn("EconomyManager executor did not terminate gracefully, forcing shutdown...");
                saveExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            LOGGER.warn("Interrupted while waiting for EconomyManager executor shutdown");
            saveExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        LOGGER.info("EconomyManager shutdown complete.");
    }
}