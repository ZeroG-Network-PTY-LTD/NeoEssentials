
package com.zerog.neoessentials.economy.managers;
import com.zerog.neoessentials.util.DebugUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

public class TransactionHistoryManager {
    private static TransactionHistoryManager instance;
    public static TransactionHistoryManager getInstance() {
        if (instance == null) instance = new TransactionHistoryManager();
        return instance;
    }

    private static final int HISTORY_LIMIT = 20; // Configurable if needed
    private final Map<UUID, Deque<String>> historyMap = new ConcurrentHashMap<>();
    private final File historyFile = com.zerog.neoessentials.util.ResourceUtil.getDataFile("transaction_history.json");
    private final Gson gson = new Gson();
    private final ScheduledExecutorService saveExecutor = Executors.newSingleThreadScheduledExecutor();
    private volatile boolean saveQueued = false;

    private TransactionHistoryManager() {
        loadHistory();
        saveExecutor.scheduleAtFixedRate(this::saveHistoryAtomic, 5, 5, TimeUnit.MINUTES);
    }

    private void loadHistory() {
        if (!historyFile.getParentFile().exists()) historyFile.getParentFile().mkdirs();
        if (!historyFile.exists()) return;
        try (FileReader reader = new FileReader(historyFile)) {
            Type type = new TypeToken<Map<String, List<String>>>(){}.getType();
            Map<String, List<String>> data = gson.fromJson(reader, type);
            if (data != null) {
                for (Map.Entry<String, List<String>> entry : data.entrySet()) {
                    Deque<String> deque = new ArrayDeque<>(entry.getValue());
                    historyMap.put(UUID.fromString(entry.getKey()), deque);
                }
            }
        } catch (Exception e) {
            DebugUtil.debugStackTrace(e);
        }
    }

    private void saveHistoryAtomic() {
        if (!historyFile.getParentFile().exists()) historyFile.getParentFile().mkdirs();
        try {
            File tempFile = new File(historyFile.getAbsolutePath() + ".tmp");
            try (FileWriter writer = new FileWriter(tempFile)) {
                Map<String, List<String>> data = new HashMap<>();
                for (Map.Entry<UUID, Deque<String>> entry : historyMap.entrySet()) {
                    data.put(entry.getKey().toString(), new ArrayList<>(entry.getValue()));
                }
                gson.toJson(data, writer);
            }
            Files.move(tempFile.toPath(), historyFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            DebugUtil.debugStackTrace(e);
        }
    }

    private void queueAsyncSave() {
        if (saveQueued) return;
        saveQueued = true;
        saveExecutor.execute(() -> {
            try {
                saveHistoryAtomic();
            } finally {
                saveQueued = false;
            }
        });
    }

    public void addTransaction(UUID player, String entry) {
        historyMap.computeIfAbsent(player, k -> new ArrayDeque<>());
        Deque<String> deque = historyMap.get(player);
        if (deque.size() >= HISTORY_LIMIT) deque.removeFirst();
        deque.addLast(entry);
        queueAsyncSave();
    }

    public List<String> getHistory(UUID player) {
        return new ArrayList<>(historyMap.getOrDefault(player, new ArrayDeque<>()));
    }

    /**
     * Shutdown the TransactionHistoryManager and clean up resources.
     * Saves any pending data and terminates the executor service.
     */
    public void shutdown() {
        try {
            // Save any pending data immediately
            saveHistoryAtomic();
            
            // Shutdown executor service
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
}
