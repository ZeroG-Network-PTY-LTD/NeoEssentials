
package com.zerog.neoessentials.economy.managers;
import com.zerog.neoessentials.util.DebugUtil;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.storage.DataStore;
import com.zerog.neoessentials.storage.StorageManager;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.io.File;
import java.io.FileReader;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

public class TransactionHistoryManager {
    private static TransactionHistoryManager instance;
    public static TransactionHistoryManager getInstance() {
        if (instance == null) instance = new TransactionHistoryManager();
        return instance;
    }

    private static final String COLLECTION = "transaction_history";

    private static int historyLimit() {
        return ConfigManager.getTransactionHistoryLimit();
    }
    private final Map<UUID, Deque<String>> historyMap = new ConcurrentHashMap<>();
    private final DataStore store = StorageManager.getInstance().getStore();
    // Legacy file — kept only so migrateLegacyFilesIfNeeded() can import pre-DataStore data.
    private final File legacyHistoryFile = com.zerog.neoessentials.util.ResourceUtil.getDataFile("transaction_history.json");
    private final Gson gson = new Gson();
    // Use daemon thread to prevent blocking JVM shutdown
    private final ScheduledExecutorService saveExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "TransactionHistory-Save");
        t.setDaemon(true);
        return t;
    });

    private TransactionHistoryManager() {
        migrateLegacyFilesIfNeeded();
        loadHistory();
    }

    private void loadHistory() {
        for (Map.Entry<String, JsonObject> e : store.getAll(COLLECTION).entrySet()) {
            try {
                UUID uuid = UUID.fromString(e.getKey());
                Deque<String> deque = new ArrayDeque<>();
                if (e.getValue().has("entries")) {
                    for (JsonElement el : e.getValue().getAsJsonArray("entries")) {
                        deque.addLast(el.getAsString());
                    }
                }
                historyMap.put(uuid, deque);
            } catch (Exception ex) {
                DebugUtil.debugStackTrace(ex);
            }
        }
    }

    /** Persist a single player's capped transaction log. */
    private void persistHistory(UUID player, Deque<String> deque) {
        JsonObject obj = new JsonObject();
        JsonArray arr = new JsonArray();
        for (String entry : deque) arr.add(entry);
        obj.add("entries", arr);
        store.put(COLLECTION, player.toString(), obj);
    }

    public void addTransaction(UUID player, String entry) {
        // Use compute() for atomic read-modify-write — prevents race conditions
        historyMap.compute(player, (uuid, deque) -> {
            if (deque == null) deque = new ArrayDeque<>();
            if (deque.size() >= historyLimit()) deque.removeFirst();
            deque.addLast(entry);
            return deque;
        });
        Deque<String> snapshot = new ArrayDeque<>(historyMap.get(player));
        saveExecutor.execute(() -> persistHistory(player, snapshot));
    }

    public List<String> getHistory(UUID player) {
        return new ArrayList<>(historyMap.getOrDefault(player, new ArrayDeque<>()));
    }

    /**
     * Shutdown the TransactionHistoryManager and clean up resources.
     * Each transaction is persisted immediately on add, so there's nothing to flush here
     * beyond letting any in-flight async writes finish.
     */
    public void shutdown() {
        try {
            saveExecutor.shutdown();
            try {
                if (!saveExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    DebugUtil.debug("TransactionHistoryManager executor did not terminate gracefully, forcing shutdown...");
                    saveExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                DebugUtil.debug("Interrupted while waiting for TransactionHistoryManager executor shutdown");
                saveExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }

            DebugUtil.debug("TransactionHistoryManager shutdown complete.");
        } catch (Exception e) {
            DebugUtil.debugStackTrace(e);
        }
    }

    /**
     * One-time import of the legacy transaction_history.json file into the active DataStore,
     * if it's still empty and storage.autoMigrate is enabled. The cap (historyLimit()) is
     * applied on load anyway via addTransaction()'s own logic, but legacy entries are already
     * capped from when they were written, so they're copied as-is.
     */
    private void migrateLegacyFilesIfNeeded() {
        if (store.hasAnyData(COLLECTION)) return;
        if (!ConfigManager.getInstance().isStorageAutoMigrateEnabled()) return;
        if (!legacyHistoryFile.exists()) return;

        int migrated = 0;
        try (FileReader reader = new FileReader(legacyHistoryFile)) {
            Type type = new TypeToken<Map<String, List<String>>>(){}.getType();
            Map<String, List<String>> data = gson.fromJson(reader, type);
            if (data != null) {
                for (Map.Entry<String, List<String>> entry : data.entrySet()) {
                    try {
                        JsonObject obj = new JsonObject();
                        JsonArray arr = new JsonArray();
                        for (String s : entry.getValue()) arr.add(s);
                        obj.add("entries", arr);
                        store.put(COLLECTION, UUID.fromString(entry.getKey()).toString(), obj);
                        migrated++;
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            DebugUtil.debugStackTrace(e);
        }

        if (migrated > 0) {
            DebugUtil.debug("TransactionHistoryManager: migrated " + migrated
                + " record(s) from legacy files into the '" + StorageManager.getInstance().getActiveType() + "' storage backend.");
        }
    }
}
