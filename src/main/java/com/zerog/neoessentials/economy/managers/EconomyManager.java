
package com.zerog.neoessentials.economy.managers;

import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.config.EconomyConfig;
import com.zerog.neoessentials.economy.EconomyTransactionLogger;
import com.zerog.neoessentials.api.event.EconomyDepositEvent;
import com.zerog.neoessentials.api.event.EconomyWithdrawEvent;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import com.zerog.neoessentials.storage.DataStore;
import com.zerog.neoessentials.storage.StorageManager;
import net.neoforged.neoforge.common.NeoForge;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.io.File;
import java.io.FileReader;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EconomyManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(EconomyManager.class);

    private static final String COLLECTION = "economy_balances";

    // Thread-safe singleton using Bill Pugh Singleton Pattern
    private static class SingletonHolder {
        private static final EconomyManager INSTANCE = new EconomyManager();
    }

    public static EconomyManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    // Use ConcurrentHashMap for balances — initialized at declaration so it is NEVER null,
    // even when the economy module is disabled and the constructor returns early.
    private ConcurrentHashMap<UUID, BigDecimal> balancesCache = new ConcurrentHashMap<>();
    private final DataStore store = StorageManager.getInstance().getStore();
    // Legacy files — kept only so migrateLegacyFilesIfNeeded() can import pre-DataStore data.
    // No longer written to.
    private final File legacyBalancesFile = com.zerog.neoessentials.util.ResourceUtil.getDataFile("balances.json");
    private final File legacyActivityFile = com.zerog.neoessentials.util.ResourceUtil.getDataFile("balances_activity.json");
    private final Gson gson = new Gson();
    // Use daemon thread to prevent blocking JVM shutdown
    private final ScheduledExecutorService saveExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "EconomyManager-Save");
        t.setDaemon(true);
        return t;
    });
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);
    /**
     * True only after the constructor has completed a full initialization
     * (i.e. the economy module is enabled and balances have been loaded).
     * Guards save/metrics methods so they are no-ops when the economy is disabled.
     */
    private volatile boolean initialized = false;

    // Track last activity (epoch millis) for each account
    private final ConcurrentHashMap<UUID, Long> lastActivityMap = new ConcurrentHashMap<>();

    private EconomyManager() {
        // Check global config for module enable
        if (!ConfigManager.isEconomyEnabled()) {
            // Economy is globally disabled, do not load balances or settings.
            // balancesCache is still a valid (empty) map thanks to the field initializer —
            // no NPE will occur if shutdown() is called before initialization.
            return;
        }
        // Configuration is loaded automatically by ConfigManager
        balancesCache = new ConcurrentHashMap<>();
        migrateLegacyFilesIfNeeded();
        loadBalances();

        // Schedule periodic cache statistics logging every 30 minutes
        saveExecutor.scheduleAtFixedRate(this::logCacheMetrics, 30, 30, TimeUnit.MINUTES);
        // Schedule inactive account cleanup every hour (no time window)
        saveExecutor.scheduleAtFixedRate(this::cleanupInactiveAccounts, 1, 1, TimeUnit.HOURS);
        initialized = true;
    }

    // ── Load / persist ───────────────────────────────────────────────────────

    private void loadBalances() {
        for (Map.Entry<String, JsonObject> e : store.getAll(COLLECTION).entrySet()) {
            try {
                UUID uuid = UUID.fromString(e.getKey());
                JsonObject obj = e.getValue();
                if (obj.has("balance")) {
                    balancesCache.put(uuid, new BigDecimal(obj.get("balance").getAsString()));
                }
                if (obj.has("lastActivity")) {
                    lastActivityMap.put(uuid, obj.get("lastActivity").getAsLong());
                }
            } catch (Exception ex) {
                LOGGER.error("Failed to load economy record {}: {}", e.getKey(), ex.getMessage());
            }
        }
    }

    /** Persist a single player's balance + last-activity record. */
    private void persistPlayer(UUID player) {
        JsonObject obj = new JsonObject();
        BigDecimal bal = balancesCache.get(player);
        obj.addProperty("balance", bal != null ? bal.toPlainString() : BigDecimal.ZERO.toPlainString());
        Long activity = lastActivityMap.get(player);
        obj.addProperty("lastActivity", activity != null ? activity : System.currentTimeMillis());
        store.put(COLLECTION, player.toString(), obj);
        NeoLog.debug(LOGGER, LogCategory.ECONOMY, "persistPlayer: committed balance={} for player={}",
            bal != null ? bal.toPlainString() : "0", player);
    }

    private void queueAsyncSave(UUID player) {
        if (!initialized) return; // economy disabled — nothing was loaded, nothing to save
        saveExecutor.execute(() -> persistPlayer(player));
    }

    private void cleanupInactiveAccounts() {
        if (!ConfigManager.isCleanupInactiveAccountsEnabled()) return;
        long now = System.currentTimeMillis();
        long thresholdMillis = ConfigManager.getInactiveAccountCleanupDays() * 24L * 60L * 60L * 1000L;
        Set<UUID> toRemove = new HashSet<>();
        for (UUID uuid : balancesCache.keySet()) {
            Long lastActive = lastActivityMap.get(uuid);
            // BUG FIX: Only remove if we know when the account was last active AND it exceeds
            // the inactive threshold. Previously the condition was (null || expired) which
            // would wipe ALL accounts the first time the activity data is missing/empty.
            if (lastActive != null && (now - lastActive) >= thresholdMillis) {
                toRemove.add(uuid);
            }
        }
        for (UUID uuid : toRemove) {
            balancesCache.remove(uuid);
            lastActivityMap.remove(uuid);
            store.delete(COLLECTION, uuid.toString());
        }
    }

    public BigDecimal getBalance(UUID player) {
        return balancesCache.computeIfAbsent(player,
            uuid -> BigDecimal.valueOf(ConfigManager.getEconomyStartingBalance()));
    }

    public void setBalance(UUID player, BigDecimal amount) {
        if (shuttingDown.get()) {
            LOGGER.warn("Attempted to modify balance during shutdown for player {}", player);
            return;
        }
        if (!ConfigManager.allowNegativeBalances() && amount.compareTo(BigDecimal.ZERO) < 0) amount = BigDecimal.ZERO;
        BigDecimal maxBalance = BigDecimal.valueOf(ConfigManager.getMaxBalance());
        if (amount.compareTo(maxBalance) > 0) amount = maxBalance;

        NeoLog.debug(LOGGER, LogCategory.ECONOMY, "setBalance: player={}, requestedAmount={}", player, amount);

        BigDecimal finalAmount = amount;
        BigDecimal oldAmount = balancesCache.put(player, finalAmount);
        lastActivityMap.put(player, System.currentTimeMillis());
        queueAsyncSave(player);

        NeoLog.debug(LOGGER, LogCategory.ECONOMY, "setBalance: player={} balance changed {} -> {}, queued for persist",
            player, oldAmount != null ? oldAmount.toPlainString() : "new account", finalAmount.toPlainString());

        // Log transaction
        EconomyTransactionLogger.log("SET", player.toString(), "SERVER", finalAmount.toPlainString(),
            "Set balance (was: " + (oldAmount != null ? oldAmount.toPlainString() : "new account") + ")");
    }

    public boolean addBalance(UUID player, BigDecimal amount) {
        if (shuttingDown.get()) {
            LOGGER.warn("Attempted to modify balance during shutdown for player {}", player);
            return false;
        }

        NeoLog.debug(LOGGER, LogCategory.ECONOMY, "addBalance: player={}, amount={}", player, amount);

        // Fire cancellable deposit event BEFORE modifying balance
        EconomyDepositEvent depositEvent = new EconomyDepositEvent(player, amount);
        NeoForge.EVENT_BUS.post(depositEvent);
        if (depositEvent.isCanceled()) {
            NeoLog.debug(LOGGER, LogCategory.ECONOMY, "addBalance: EconomyDepositEvent cancelled for player {}", player);
            return false;
        }

        BigDecimal maxBalance = BigDecimal.valueOf(ConfigManager.getMaxBalance());
        boolean allowNegative = ConfigManager.allowNegativeBalances();

        // Atomic read-modify-write using compute()
        BigDecimal[] result = new BigDecimal[1];
        balancesCache.compute(player, (uuid, current) -> {
            if (current == null) {
                current = BigDecimal.valueOf(ConfigManager.getEconomyStartingBalance());
            }

            BigDecimal newAmount = current.add(amount);

            // Validate constraints
            if (!allowNegative && newAmount.compareTo(BigDecimal.ZERO) < 0) {
                result[0] = null; // Signal failure
                return current; // Don't modify
            }

            if (newAmount.compareTo(maxBalance) > 0) {
                newAmount = maxBalance;
            }

            result[0] = newAmount; // Signal success
            return newAmount;
        });

        if (result[0] == null) {
            NeoLog.debug(LOGGER, LogCategory.ECONOMY, "addBalance: player={} rejected — negative balances not allowed, amount={}",
                player, amount);
            return false; // Operation failed validation
        }

        lastActivityMap.put(player, System.currentTimeMillis());
        queueAsyncSave(player);

        NeoLog.debug(LOGGER, LogCategory.ECONOMY, "addBalance: player={} new balance={}, queued for persist", player, result[0]);

        // Log transaction
        EconomyTransactionLogger.log("ADD", "SERVER", player.toString(), amount.toPlainString(), "Add to balance");
        return true;
    }

    public boolean subtractBalance(UUID player, BigDecimal amount) {
        if (shuttingDown.get()) {
            LOGGER.warn("Attempted to modify balance during shutdown for player {}", player);
            return false;
        }

        NeoLog.debug(LOGGER, LogCategory.ECONOMY, "subtractBalance: player={}, amount={}", player, amount);

        // Fire cancellable withdraw event BEFORE modifying balance
        EconomyWithdrawEvent withdrawEvent = new EconomyWithdrawEvent(player, amount);
        NeoForge.EVENT_BUS.post(withdrawEvent);
        if (withdrawEvent.isCanceled()) {
            NeoLog.debug(LOGGER, LogCategory.ECONOMY, "subtractBalance: EconomyWithdrawEvent cancelled for player {}", player);
            return false;
        }

        boolean allowNegative = ConfigManager.allowNegativeBalances();

        // Atomic read-modify-write using compute()
        BigDecimal[] result = new BigDecimal[1];
        balancesCache.compute(player, (uuid, current) -> {
            if (current == null) {
                current = BigDecimal.valueOf(ConfigManager.getEconomyStartingBalance());
            }

            BigDecimal newAmount = current.subtract(amount);

            // Validate constraints
            if (!allowNegative && newAmount.compareTo(BigDecimal.ZERO) < 0) {
                result[0] = null; // Signal failure
                return current; // Don't modify
            }

            result[0] = newAmount; // Signal success
            return newAmount;
        });

        if (result[0] == null) {
            NeoLog.debug(LOGGER, LogCategory.ECONOMY, "subtractBalance: player={} rejected — insufficient funds for amount={}",
                player, amount);
            return false; // Insufficient funds
        }

        lastActivityMap.put(player, System.currentTimeMillis());
        queueAsyncSave(player);

        NeoLog.debug(LOGGER, LogCategory.ECONOMY, "subtractBalance: player={} new balance={}, queued for persist", player, result[0]);

        // Log transaction
        EconomyTransactionLogger.log("SUBTRACT", player.toString(), "SERVER", amount.toPlainString(), "Subtract from balance");
        return true;
    }

    public Map<UUID, BigDecimal> getAllBalances() {
        return new ConcurrentHashMap<>(balancesCache);
    }

    /**
     * Remove a player's account completely (deletes their balance entry).
     * @return true if the account existed and was removed, false if it didn't exist.
     */
    public boolean removeAccount(UUID player) {
        BigDecimal removed = balancesCache.remove(player);
        lastActivityMap.remove(player);
        if (removed != null) {
            store.delete(COLLECTION, player.toString());
            EconomyTransactionLogger.log("DELETE", player.toString(), "SERVER", "0", "Account deleted");
            NeoLog.debug(LOGGER, LogCategory.ECONOMY, "removeAccount: deleted account for player={} (was balance={})",
                player, removed.toPlainString());
            return true;
        }
        NeoLog.debug(LOGGER, LogCategory.ECONOMY, "removeAccount: no account found for player={}", player);
        return false;
    }

    /**
     * @deprecated Use ConfigManager.getInstance() directly for config access
     */
    @Deprecated
    public EconomyConfig getConfig() {
        return new EconomyConfig(); // Return a fresh instance that delegates to ConfigManager
    }

    public boolean isEnabled() {
        return ConfigManager.isEconomyEnabled();
    }

    public String getCurrencySymbol() {
        return ConfigManager.getCurrencySymbol();
    }

    // Vault compatibility stub removed; use EconomyService API instead

    /**
     * Manually optimize the balances cache by cleaning up expired or low-activity entries.
     * This can be called after large batch operations or periodically for memory efficiency.
     */
    @SuppressWarnings("unused") // Public API method
    public void optimizeCache() {
        // ConcurrentHashMap doesn't need explicit cleanup
        // Account cleanup is handled by cleanupInactiveAccounts
    }

    /**
     * Returns cache statistics for monitoring and tuning.
     */
    @SuppressWarnings("unused") // Public API method
    public String getCacheStats() {
        return "EconomyManager Cache Size: " + balancesCache.size();
    }

    /**
     * Logs cache metrics for monitoring and debugging.
     */
    private void logCacheMetrics() {
        if (!initialized) return;
        NeoLog.info(LOGGER, LogCategory.ECONOMY, "EconomyManager Cache Metrics - Size: {}", balancesCache.size());
    }

    /**
     * Shuts down the economy manager and its executor services properly.
     */
    public void shutdown() {
        NeoLog.info(LOGGER, LogCategory.ECONOMY, "Shutting down EconomyManager...");

        // Set shutdown flag to prevent new operations
        shuttingDown.set(true);

        if (!initialized) {
            // Economy was never enabled/initialized — nothing to save; just stop the executor.
            NeoLog.info(LOGGER, LogCategory.ECONOMY, "EconomyManager was not initialized (economy disabled) — skipping save.");
            saveExecutor.shutdownNow();
            return;
        }

        // Shutdown executor service — lets any already-queued persistPlayer() writes finish
        // (each mutation persists its own record immediately/async, so there's no bulk
        // save-everything step needed here anymore).
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

        NeoLog.info(LOGGER, LogCategory.ECONOMY, "EconomyManager shutdown complete.");
    }

    // ── Legacy migration ────────────────────────────────────────────────────

    /**
     * One-time import of the legacy balances.json/balances_activity.json files into the
     * active DataStore, if it's still empty and storage.autoMigrate is enabled. Both files
     * are merged into a single record per player in COLLECTION (balance + lastActivity).
     */
    private void migrateLegacyFilesIfNeeded() {
        if (store.hasAnyData(COLLECTION)) return;
        if (!ConfigManager.getInstance().isStorageAutoMigrateEnabled()) return;

        Map<UUID, BigDecimal> legacyBalances = new HashMap<>();
        Map<UUID, Long> legacyActivity = new HashMap<>();

        if (legacyBalancesFile.exists()) {
            try (FileReader reader = new FileReader(legacyBalancesFile)) {
                Type type = new TypeToken<Map<String, Object>>(){}.getType();
                Map<String, Object> data = gson.fromJson(reader, type);
                if (data != null) {
                    for (Map.Entry<String, Object> entry : data.entrySet()) {
                        if (entry.getKey().startsWith("_")) continue; // skip metadata fields
                        try {
                            legacyBalances.put(UUID.fromString(entry.getKey()), new BigDecimal(entry.getValue().toString()));
                        } catch (Exception ignored) {
                            NeoLog.debug(LOGGER, LogCategory.ECONOMY,
                                "migrateLegacyFilesIfNeeded: skipping malformed legacy balance entry for key=" + entry.getKey(), ignored);
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Failed to migrate legacy balances.json: {}", e.getMessage());
            }
        }

        if (legacyActivityFile.exists()) {
            try (FileReader reader = new FileReader(legacyActivityFile)) {
                Type type = new TypeToken<Map<String, Object>>(){}.getType();
                Map<String, Object> data = gson.fromJson(reader, type);
                if (data != null) {
                    for (Map.Entry<String, Object> entry : data.entrySet()) {
                        if (entry.getKey().startsWith("_")) continue; // skip metadata fields
                        try {
                            legacyActivity.put(UUID.fromString(entry.getKey()), ((Number) entry.getValue()).longValue());
                        } catch (Exception ignored) {
                            NeoLog.debug(LOGGER, LogCategory.ECONOMY,
                                "migrateLegacyFilesIfNeeded: skipping malformed legacy activity entry for key=" + entry.getKey(), ignored);
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Failed to migrate legacy balances_activity.json: {}", e.getMessage());
            }
        }

        if (legacyBalances.isEmpty() && legacyActivity.isEmpty()) return;

        Set<UUID> allIds = new HashSet<>();
        allIds.addAll(legacyBalances.keySet());
        allIds.addAll(legacyActivity.keySet());

        int migrated = 0;
        for (UUID uuid : allIds) {
            JsonObject obj = new JsonObject();
            BigDecimal bal = legacyBalances.get(uuid);
            obj.addProperty("balance", bal != null ? bal.toPlainString() : BigDecimal.ZERO.toPlainString());
            Long act = legacyActivity.get(uuid);
            obj.addProperty("lastActivity", act != null ? act : System.currentTimeMillis());
            store.put(COLLECTION, uuid.toString(), obj);
            migrated++;
        }

        if (migrated > 0) {
            NeoLog.info(LOGGER, LogCategory.ECONOMY, "EconomyManager: migrated {} balance record(s) from legacy files into the '{}' storage backend.",
                migrated, StorageManager.getInstance().getActiveType());
        }
    }
}
