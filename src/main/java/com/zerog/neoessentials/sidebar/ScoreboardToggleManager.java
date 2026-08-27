package com.zerog.neoessentials.sidebar;

import com.google.gson.JsonObject;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import com.zerog.neoessentials.storage.DataStore;
import com.zerog.neoessentials.storage.StorageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Persists each player's personal on/off preference for the sidebar scoreboard, the same
 * DataStore-backed pattern {@code economy/managers/PayToggleManager.java} uses for pay
 * toggles — a single collection, one JSON record per player, written immediately on change.
 */
public class ScoreboardToggleManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScoreboardToggleManager.class);
    private static final String COLLECTION = "scoreboard_toggles";

    private static class SingletonHolder {
        private static final ScoreboardToggleManager INSTANCE = new ScoreboardToggleManager();
    }

    public static ScoreboardToggleManager getInstance() { return SingletonHolder.INSTANCE; }

    private final ConcurrentHashMap<UUID, Boolean> cache = new ConcurrentHashMap<>();
    private final DataStore store = StorageManager.getInstance().getStore();

    private ScoreboardToggleManager() {
        for (Map.Entry<String, JsonObject> e : store.getAll(COLLECTION).entrySet()) {
            try {
                UUID uuid = UUID.fromString(e.getKey());
                boolean enabled = !e.getValue().has("enabled") || e.getValue().get("enabled").getAsBoolean();
                cache.put(uuid, enabled);
            } catch (Exception ex) {
                LOGGER.error("Failed to load scoreboard toggle entry {}: {}", e.getKey(), ex.getMessage());
            }
        }
    }

    /** Defaults to on (shown) when the player has never toggled it. */
    public boolean isEnabled(UUID player) {
        return cache.getOrDefault(player, true);
    }

    public void setEnabled(UUID player, boolean enabled) {
        cache.put(player, enabled);
        JsonObject obj = new JsonObject();
        obj.addProperty("enabled", enabled);
        store.put(COLLECTION, player.toString(), obj);
        NeoLog.debug(LOGGER, LogCategory.GENERAL, "ScoreboardToggleManager: set {} enabled={}", player, enabled);
    }
}
