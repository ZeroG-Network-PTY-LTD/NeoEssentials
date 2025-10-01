
package com.zerog.neoessentials.economy.managers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zerog.neoessentials.config.EconomyConfig;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

public class PayToggleManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(PayToggleManager.class);
    
    // Thread-safe singleton using Bill Pugh Singleton Pattern
    private static class SingletonHolder {
        private static final PayToggleManager INSTANCE = new PayToggleManager();
    }
    
    public static PayToggleManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private final Cache<UUID, Boolean> paytoggleCache = Caffeine.newBuilder()
        .maximumSize(10000)
        .expireAfterAccess(1, TimeUnit.HOURS)
        .recordStats() // Enable statistics for monitoring
        .build();
    private final File togglesFile = new File("neoessentials/paytoggles.json");
    private final Gson gson = new Gson();
    private final ScheduledExecutorService saveExecutor = Executors.newSingleThreadScheduledExecutor();
    private volatile boolean saveQueued = false;
    private EconomyConfig config;

    private PayToggleManager() {
        this.config = EconomyManager.getInstance().getConfig();
        loadToggles();
        saveExecutor.scheduleAtFixedRate(this::saveTogglesAtomic, 5, 5, TimeUnit.MINUTES);
    }

    private void loadToggles() {
        if (!togglesFile.getParentFile().exists()) togglesFile.getParentFile().mkdirs();
        if (!togglesFile.exists()) return;
        try (FileReader reader = new FileReader(togglesFile)) {
            Type type = new TypeToken<Map<String, Boolean>>(){}.getType();
            Map<String, Boolean> data = gson.fromJson(reader, type);
            if (data != null) {
                for (Map.Entry<String, Boolean> entry : data.entrySet()) {
                    paytoggleCache.put(UUID.fromString(entry.getKey()), entry.getValue());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load pay toggles", e);
        }
    }

    private void saveTogglesAtomic() {
        if (!togglesFile.getParentFile().exists()) togglesFile.getParentFile().mkdirs();
        try {
            File tempFile = new File(togglesFile.getAbsolutePath() + ".tmp");
            try (FileWriter writer = new FileWriter(tempFile)) {
                Map<String, Boolean> data = new ConcurrentHashMap<>();
                for (Map.Entry<UUID, Boolean> entry : paytoggleCache.asMap().entrySet()) {
                    data.put(entry.getKey().toString(), entry.getValue());
                }
                gson.toJson(data, writer);
            }
            Files.move(tempFile.toPath(), togglesFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            LOGGER.error("Failed to save pay toggles", e);
        }
    }

    private void queueAsyncSave() {
        if (saveQueued) return;
        saveQueued = true;
        saveExecutor.execute(() -> {
            try {
                saveTogglesAtomic();
            } finally {
                saveQueued = false;
            }
        });
    }

    public boolean getPayToggle(UUID player) {
        Boolean cached = paytoggleCache.getIfPresent(player);
        if (cached != null) return cached;
        // Default to config value for new players
        return config.paytoggleDefault;
    }

    public void setPayToggle(UUID player, boolean enabled) {
        paytoggleCache.put(player, enabled);
        queueAsyncSave();
    }

    public Map<UUID, Boolean> getAllToggles() {
        return new ConcurrentHashMap<>(paytoggleCache.asMap());
    }
}
