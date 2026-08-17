
package com.zerog.neoessentials.economy.managers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import com.zerog.neoessentials.storage.DataStore;
import com.zerog.neoessentials.storage.StorageManager;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.io.File;
import java.io.FileReader;
import java.util.Map;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

public class PayToggleManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(PayToggleManager.class);
    private static final String COLLECTION = "pay_toggles";

    // Thread-safe singleton using Bill Pugh Singleton Pattern
    private static class SingletonHolder {
        private static final PayToggleManager INSTANCE = new PayToggleManager();
    }

    public static PayToggleManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private final ConcurrentHashMap<UUID, Boolean> paytoggleCache = new ConcurrentHashMap<>();
    private final DataStore store = StorageManager.getInstance().getStore();
    // Legacy file — kept only so migrateLegacyFilesIfNeeded() can import pre-DataStore data.
    private final File legacyTogglesFile = com.zerog.neoessentials.util.ResourceUtil.getDataFile("paytoggles.json");
    private final Gson gson = new Gson();
    // Use daemon thread to prevent blocking JVM shutdown
    private final ScheduledExecutorService saveExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "PayToggle-Save");
        t.setDaemon(true);
        return t;
    });
    @SuppressWarnings("unused") // Reserved for future direct config access
    private final ConfigManager configManager = ConfigManager.getInstance();

    private PayToggleManager() {
        // Don't initialize config in constructor to avoid circular dependency
        // Will be lazy-loaded when needed
        migrateLegacyFilesIfNeeded();
        loadToggles();
    }

    private void loadToggles() {
        for (Map.Entry<String, JsonObject> e : store.getAll(COLLECTION).entrySet()) {
            try {
                UUID uuid = UUID.fromString(e.getKey());
                boolean enabled = e.getValue().has("enabled") && e.getValue().get("enabled").getAsBoolean();
                paytoggleCache.put(uuid, enabled);
            } catch (Exception ex) {
                LOGGER.error("Failed to load pay toggle entry {}: {}", e.getKey(), ex.getMessage());
            }
        }
    }

    public boolean getPayToggle(UUID player) {
        return paytoggleCache.computeIfAbsent(player,
            uuid -> com.zerog.neoessentials.config.ConfigManager.getPayToggleDefault());
    }

    public void setPayToggle(UUID player, boolean enabled) {
        paytoggleCache.put(player, enabled);
        JsonObject obj = new JsonObject();
        obj.addProperty("enabled", enabled);
        store.put(COLLECTION, player.toString(), obj);
        NeoLog.debug(LOGGER, LogCategory.ECONOMY, "setPayToggle: player={} enabled={}", player, enabled);
    }

    @SuppressWarnings("unused") // Public API method
    public Map<UUID, Boolean> getAllToggles() {
        return new ConcurrentHashMap<>(paytoggleCache);
    }

    /**
     * Shutdown the PayToggleManager and clean up resources.
     * Each toggle is persisted immediately on change, so there's nothing to flush here —
     * just stop the (currently idle) executor service cleanly.
     */
    public void shutdown() {
        try {
            saveExecutor.shutdown();
            try {
                if (!saveExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    LOGGER.warn("PayToggleManager executor did not terminate gracefully, forcing shutdown...");
                    saveExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                LOGGER.warn("Interrupted while waiting for PayToggleManager executor shutdown");
                saveExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }

            NeoLog.info(LOGGER, LogCategory.ECONOMY, "PayToggleManager shutdown complete.");
        } catch (Exception e) {
            LOGGER.error("Error during PayToggleManager shutdown", e);
        }
    }

    /**
     * One-time import of the legacy paytoggles.json file into the active DataStore, if it's
     * still empty and storage.autoMigrate is enabled.
     */
    private void migrateLegacyFilesIfNeeded() {
        if (store.hasAnyData(COLLECTION)) return;
        if (!ConfigManager.getInstance().isStorageAutoMigrateEnabled()) return;
        if (!legacyTogglesFile.exists()) return;

        int migrated = 0;
        try (FileReader reader = new FileReader(legacyTogglesFile)) {
            Type type = new TypeToken<Map<String, Boolean>>(){}.getType();
            Map<String, Boolean> data = gson.fromJson(reader, type);
            if (data != null) {
                for (Map.Entry<String, Boolean> entry : data.entrySet()) {
                    try {
                        JsonObject obj = new JsonObject();
                        obj.addProperty("enabled", entry.getValue());
                        store.put(COLLECTION, UUID.fromString(entry.getKey()).toString(), obj);
                        migrated++;
                    } catch (Exception ignored) {
                        NeoLog.debug(LOGGER, LogCategory.ECONOMY,
                            "migrateLegacyFilesIfNeeded: skipping malformed legacy pay-toggle entry for key=" + entry.getKey(), ignored);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to migrate legacy paytoggles.json: {}", e.getMessage());
        }

        if (migrated > 0) {
            NeoLog.info(LOGGER, LogCategory.ECONOMY, "PayToggleManager: migrated {} pay-toggle record(s) from legacy files into the '{}' storage backend.",
                migrated, StorageManager.getInstance().getActiveType());
        }
    }
}
