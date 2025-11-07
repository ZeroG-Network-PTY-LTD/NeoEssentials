
package com.zerog.neoessentials.economy.managers;

import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.config.EconomyConfig;
import com.zerog.neoessentials.economy.EconomyTransactionLogger;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EconomyManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(EconomyManager.class);
    
    // Data version tracking - increment when JSON structure changes
    private static final String DATA_VERSION_KEY = "_dataVersion";
    private static final int CURRENT_DATA_VERSION = 1;
    
    // Thread-safe singleton using Bill Pugh Singleton Pattern
    private static class SingletonHolder {
        private static final EconomyManager INSTANCE = new EconomyManager();
    }
    
    public static EconomyManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    // Use ConcurrentHashMap for balances
    private ConcurrentHashMap<UUID, BigDecimal> balancesCache;
    // Store balances in root/neoessentials/balances.json
    private final File balancesFile = com.zerog.neoessentials.util.ResourceUtil.getDataFile("balances.json");
    private final Gson gson = new Gson();
    private final ScheduledExecutorService saveExecutor = Executors.newSingleThreadScheduledExecutor();
    private volatile boolean saveQueued = false;

    // Track last activity (epoch millis) for each account
    private final ConcurrentHashMap<UUID, Long> lastActivityMap = new ConcurrentHashMap<>();
    private final File lastActivityFile = new File("neoessentials/balances_activity.json");
    
    // Track current file versions to avoid unnecessary backups
    private volatile int currentBalancesVersion = CURRENT_DATA_VERSION;
    private volatile int currentActivityVersion = CURRENT_DATA_VERSION;

    private void loadBalances() {
        if (!balancesFile.getParentFile().exists()) {
            balancesFile.getParentFile().mkdirs();
        }
        if (!balancesFile.exists()) return;
        try (FileReader reader = new FileReader(balancesFile)) {
            Type type = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> data = gson.fromJson(reader, type);
            if (data != null) {
                // Read version if present
                if (data.containsKey(DATA_VERSION_KEY)) {
                    Object versionObj = data.get(DATA_VERSION_KEY);
                    if (versionObj instanceof Number) {
                        currentBalancesVersion = ((Number) versionObj).intValue();
                    }
                    data.remove(DATA_VERSION_KEY); // Don't process version as balance
                }
                
                // Load balances
                for (Map.Entry<String, Object> entry : data.entrySet()) {
                    if (!entry.getKey().startsWith("_")) { // Skip metadata fields
                        balancesCache.put(UUID.fromString(entry.getKey()), new BigDecimal(entry.getValue().toString()));
                    }
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
            // Check if version has changed - if so, create backup before overwriting
            if (balancesFile.exists() && shouldCreateBackup(balancesFile, currentBalancesVersion)) {
                LOGGER.info("Economy data version mismatch detected, creating backup...");
                createBackupFile(balancesFile);
            }
            
            // Write to temp file first
            File tempFile = new File(balancesFile.getAbsolutePath() + ".tmp");
            try (FileWriter writer = new FileWriter(tempFile)) {
                Map<String, Object> data = new ConcurrentHashMap<>();
                
                // Add version marker
                data.put(DATA_VERSION_KEY, CURRENT_DATA_VERSION);
                
                // Add balances
                for (Map.Entry<UUID, BigDecimal> entry : balancesCache.entrySet()) {
                    data.put(entry.getKey().toString(), entry.getValue().toPlainString());
                }
                gson.toJson(data, writer);
            }
            
            // Atomically move temp file to balancesFile
            Files.move(tempFile.toPath(), balancesFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            
            // Update current version tracker
            currentBalancesVersion = CURRENT_DATA_VERSION;
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
        if (!ConfigManager.isCleanupInactiveAccountsEnabled()) return;
        long now = System.currentTimeMillis();
        long thresholdMillis = ConfigManager.getInactiveAccountCleanupDays() * 24L * 60L * 60L * 1000L;
        for (UUID uuid : balancesCache.keySet()) {
            Long lastActive = lastActivityMap.get(uuid);
            if (lastActive == null || (now - lastActive) >= thresholdMillis) {
                balancesCache.remove(uuid);
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
            Type type = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> data = gson.fromJson(reader, type);
            if (data != null) {
                // Read version if present
                if (data.containsKey(DATA_VERSION_KEY)) {
                    Object versionObj = data.get(DATA_VERSION_KEY);
                    if (versionObj instanceof Number) {
                        currentActivityVersion = ((Number) versionObj).intValue();
                    }
                    data.remove(DATA_VERSION_KEY); // Don't process version as activity
                }
                
                // Load activity data
                for (Map.Entry<String, Object> entry : data.entrySet()) {
                    if (!entry.getKey().startsWith("_")) { // Skip metadata fields
                        lastActivityMap.put(UUID.fromString(entry.getKey()), ((Number) entry.getValue()).longValue());
                    }
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
            // Check if version has changed - if so, create backup before overwriting
            if (lastActivityFile.exists() && shouldCreateBackup(lastActivityFile, currentActivityVersion)) {
                LOGGER.info("Activity data version mismatch detected, creating backup...");
                createBackupFile(lastActivityFile);
            }
            
            File tempFile = new File(lastActivityFile.getAbsolutePath() + ".tmp");
            try (FileWriter writer = new FileWriter(tempFile)) {
                Map<String, Object> data = new ConcurrentHashMap<>();
                
                // Add version marker
                data.put(DATA_VERSION_KEY, CURRENT_DATA_VERSION);
                
                // Add activity data
                for (Map.Entry<UUID, Long> entry : lastActivityMap.entrySet()) {
                    data.put(entry.getKey().toString(), entry.getValue());
                }
                gson.toJson(data, writer);
            }
            
            Files.move(tempFile.toPath(), lastActivityFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            
            // Update current version tracker
            currentActivityVersion = CURRENT_DATA_VERSION;
        } catch (IOException e) {
            LOGGER.error("Failed to save last activity data", e);
        }
    }

    private void queueAsyncSaveActivity() {
        saveExecutor.execute(this::saveLastActivityAtomic);
    }

    private EconomyManager() {
        // Check global config for module enable
        if (!ConfigManager.isEconomyEnabled()) {
            // Economy is globally disabled, do not load balances or settings
            return;
        }
        // Configuration is loaded automatically by ConfigManager
        // Initialize ConcurrentHashMap for balances
        balancesCache = new ConcurrentHashMap<>();
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
        BigDecimal cached = balancesCache.get(player);
        if (cached != null) return cached;
        return BigDecimal.valueOf(ConfigManager.getEconomyStartingBalance());
    }

    public void setBalance(UUID player, BigDecimal amount) {
        if (!ConfigManager.allowNegativeBalances() && amount.compareTo(BigDecimal.ZERO) < 0) amount = BigDecimal.ZERO;
        BigDecimal maxBalance = BigDecimal.valueOf(ConfigManager.getMaxBalance());
        if (maxBalance != null && amount.compareTo(maxBalance) > 0) amount = maxBalance;
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
        if (!ConfigManager.allowNegativeBalances() && newAmount.compareTo(BigDecimal.ZERO) < 0) return false;
        BigDecimal maxBalance = BigDecimal.valueOf(ConfigManager.getMaxBalance());
        if (maxBalance != null && newAmount.compareTo(maxBalance) > 0) newAmount = maxBalance;
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
        if (!ConfigManager.allowNegativeBalances() && newAmount.compareTo(BigDecimal.ZERO) < 0) return false;
        balancesCache.put(player, newAmount);
        lastActivityMap.put(player, System.currentTimeMillis());
        queueAsyncSave();
        queueAsyncSaveActivity();
        // Log transaction
        EconomyTransactionLogger.log("SUBTRACT", player.toString(), "SERVER", amount.toPlainString(), "Subtract from balance");
        return true;
    }

    public Map<UUID, BigDecimal> getAllBalances() {
        return new ConcurrentHashMap<>(balancesCache);
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
    public void optimizeCache() {
        // ConcurrentHashMap doesn't need explicit cleanup
        // Account cleanup is handled by cleanupInactiveAccounts
    }

    /**
     * Returns cache statistics for monitoring and tuning.
     */
    public String getCacheStats() {
        return "EconomyManager Cache Size: " + balancesCache.size();
    }
    
    /**
     * Logs cache metrics for monitoring and debugging.
     */
    private void logCacheMetrics() {
        LOGGER.info("EconomyManager Cache Metrics - Size: {}", balancesCache.size());
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
    
    /**
     * Check if a backup should be created by comparing file version with current version.
     * Only creates backup if file exists and version differs (similar to ConfigManager behavior).
     */
    private boolean shouldCreateBackup(File file, int currentVersion) {
        if (!file.exists()) {
            return false; // No file to backup
        }
        
        try (FileReader reader = new FileReader(file)) {
            Type type = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> data = gson.fromJson(reader, type);
            
            if (data != null && data.containsKey(DATA_VERSION_KEY)) {
                Object versionObj = data.get(DATA_VERSION_KEY);
                if (versionObj instanceof Number) {
                    int fileVersion = ((Number) versionObj).intValue();
                    // Only backup if version differs
                    return fileVersion != CURRENT_DATA_VERSION;
                }
            }
            
            // No version field means old format - create backup
            return true;
            
        } catch (Exception e) {
            LOGGER.warn("Error checking version for {}: {}", file.getName(), e.getMessage());
            return false; // Don't backup on error
        }
    }
    
    /**
     * Create backup file with incremental numbering (.bak1, .bak2, etc.)
     * Same behavior as ConfigManager for consistency.
     */
    private void createBackupFile(File originalFile) {
        try {
            File parent = originalFile.getParentFile();
            String baseName = originalFile.getName();
            
            // Find next available backup number
            File backupFile = null;
            for (int i = 1; i <= 999; i++) {
                File candidate = new File(parent, baseName + ".bak" + i);
                if (!candidate.exists()) {
                    backupFile = candidate;
                    break;
                }
            }
            
            // Fallback if we somehow have 999 backups
            if (backupFile == null) {
                backupFile = new File(parent, baseName + ".bak999");
            }
            
            // Copy existing file to backup
            Files.copy(originalFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            LOGGER.info("✓ Created backup: {} -> {} (version mismatch detected)", 
                originalFile.getName(), backupFile.getName());
            
        } catch (IOException e) {
            LOGGER.error("Failed to create backup for {}: {}", originalFile.getName(), e.getMessage());
        }
    }
}