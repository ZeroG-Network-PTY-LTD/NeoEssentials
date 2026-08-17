package com.zerog.neoessentials.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zerog.neoessentials.storage.DataStore;
import com.zerog.neoessentials.storage.StorageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;

import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player data storage, one record per player per data type (e.g. "homes", "back_locations"),
 * backed by the pluggable {@link DataStore} (JSON/YAML/SQLite/MySQL — see {@link StorageManager}).
 *
 * <p>Each {@code dataType} maps to its own DataStore collection ({@code "playerdata_" + dataType}),
 * with the player's UUID as the record id. On first use, if that collection is empty and
 * {@code storage.autoMigrate} is enabled, any legacy per-player files under
 * {@code neoessentials/playerdata/<dataType>/<uuid>.json} (the pre-DataStore on-disk layout) are
 * imported automatically.
 *
 * <p>Kept the same public API as before this migration (load/save/flush/delete/hasData/
 * getAllPlayerIds/unload/clearAll/getStatistics) so callers like {@code HomeManager} and the
 * {@code /back} teleport manager needed no changes.
 */
public class PlayerDataStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerDataStore.class);

    private final String dataType; // e.g., "homes", "back_locations"
    private final String collection;
    private final DataStore store;

    // In-memory cache: UUID -> JsonObject
    private final Map<UUID, JsonObject> cache = new ConcurrentHashMap<>();

    // Track dirty (modified) entries that still need persisting
    private final Set<UUID> dirtyEntries = ConcurrentHashMap.newKeySet();

    /**
     * Create a new PlayerDataStore for a specific data type.
     *
     * @param dataType The type of data being stored (e.g., "homes", "back_locations")
     */
    public PlayerDataStore(String dataType) {
        this.dataType = dataType;
        this.collection = "playerdata_" + dataType;
        this.store = StorageManager.getInstance().getStore();
        migrateLegacyFilesIfNeeded();
    }

    /**
     * Load player data.
     *
     * @param playerId Player UUID
     * @return Player data as JsonObject, or new empty JsonObject if not found
     */
    public JsonObject load(UUID playerId) {
        JsonObject cached = cache.get(playerId);
        if (cached != null) {
            return cached;
        }

        JsonObject data = store.get(collection, playerId.toString());
        if (data == null) {
            data = new JsonObject();
            NeoLog.debug(LOGGER, LogCategory.GENERAL, "No {} data found for player {}, using empty data", dataType, playerId);
        } else {
            NeoLog.debug(LOGGER, LogCategory.GENERAL, "Loaded {} data for player {}", dataType, playerId);
        }

        cache.put(playerId, data);
        return data;
    }

    /**
     * Save player data (persists immediately via the active DataStore backend).
     *
     * @param playerId Player UUID
     * @param data Player data to save
     */
    public void save(UUID playerId, JsonObject data) {
        cache.put(playerId, data);
        dirtyEntries.add(playerId);
        flush(playerId);
    }

    /**
     * Flush dirty (modified) data for a specific player. Since {@link #save} already persists
     * immediately, this is mostly a no-op safety net for any dirty-but-unflushed entry.
     *
     * @param playerId Player UUID
     */
    public void flush(UUID playerId) {
        if (!dirtyEntries.contains(playerId)) {
            return; // Nothing to save
        }

        JsonObject data = cache.get(playerId);
        if (data == null) {
            return;
        }

        try {
            store.put(collection, playerId.toString(), data);
            dirtyEntries.remove(playerId);
            NeoLog.debug(LOGGER, LogCategory.GENERAL, "Saved {} data for player {}", dataType, playerId);
        } catch (Exception e) {
            LOGGER.error("Failed to save {} data for player {}: {}", dataType, playerId, e.getMessage(), e);
        }
    }

    /**
     * Flush all dirty data.
     */
    public void flushAll() {
        if (dirtyEntries.isEmpty()) {
            return;
        }

        NeoLog.info(LOGGER, LogCategory.GENERAL, "Flushing {} dirty {} entries...", dirtyEntries.size(), dataType);

        Set<UUID> toFlush = new HashSet<>(dirtyEntries);
        for (UUID playerId : toFlush) {
            flush(playerId);
        }

        NeoLog.info(LOGGER, LogCategory.GENERAL, "Flushed all {} data", dataType);
    }

    /**
     * Delete player data.
     *
     * @param playerId Player UUID
     * @return true if deleted successfully
     */
    public boolean delete(UUID playerId) {
        cache.remove(playerId);
        dirtyEntries.remove(playerId);
        boolean existed = store.delete(collection, playerId.toString());
        if (existed) {
            NeoLog.info(LOGGER, LogCategory.GENERAL, "Deleted {} data for player {}", dataType, playerId);
        }
        return true;
    }

    /**
     * Check if player has data.
     *
     * @param playerId Player UUID
     * @return true if player has a stored record
     */
    public boolean hasData(UUID playerId) {
        return cache.containsKey(playerId) || store.get(collection, playerId.toString()) != null;
    }

    /**
     * Get all player UUIDs that have data.
     *
     * @return Set of player UUIDs
     */
    public Set<UUID> getAllPlayerIds() {
        Set<UUID> playerIds = new HashSet<>();
        playerIds.addAll(cache.keySet());

        for (String id : store.getAll(collection).keySet()) {
            try {
                playerIds.add(UUID.fromString(id));
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Invalid player data id in collection {}: {}", collection, id);
            }
        }

        return playerIds;
    }

    /**
     * Unload player data from cache.
     *
     * @param playerId Player UUID
     */
    public void unload(UUID playerId) {
        flush(playerId);
        cache.remove(playerId);
        NeoLog.debug(LOGGER, LogCategory.GENERAL, "Unloaded {} data for player {} from cache", dataType, playerId);
    }

    /**
     * Clear all data (use with caution!).
     */
    public void clearAll() {
        for (String id : store.getAll(collection).keySet()) {
            store.delete(collection, id);
        }
        cache.clear();
        dirtyEntries.clear();
        LOGGER.warn("Cleared all {} data", dataType);
    }

    /**
     * Get cache size (number of loaded players).
     *
     * @return Number of players in cache
     */
    public int getCacheSize() {
        return cache.size();
    }

    /**
     * Get total number of players with data.
     *
     * @return Total player count
     */
    public int getTotalPlayers() {
        return store.getAll(collection).size();
    }

    /**
     * Get statistics about this data store.
     *
     * @return Statistics string
     */
    public String getStatistics() {
        return String.format("%s DataStore: %d players total, %d in cache, %d dirty",
            dataType, getTotalPlayers(), getCacheSize(), dirtyEntries.size());
    }

    /**
     * One-time import of legacy per-player files (the pre-DataStore on-disk layout,
     * {@code neoessentials/playerdata/<dataType>/<uuid>.json}) into the active DataStore, if
     * this collection is still empty and {@code storage.autoMigrate} is enabled.
     */
    private void migrateLegacyFilesIfNeeded() {
        if (store.hasAnyData(collection)) return;
        if (!com.zerog.neoessentials.config.ConfigManager.getInstance().isStorageAutoMigrateEnabled()) return;

        File legacyDir = new File(ResourceUtil.DATA_DIR, "playerdata/" + dataType);
        File[] files = legacyDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null || files.length == 0) return;

        int migrated = 0;
        for (File file : files) {
            String fileName = file.getName();
            String uuidStr = fileName.substring(0, fileName.length() - 5); // strip ".json"
            try {
                UUID playerId = UUID.fromString(uuidStr);
                try (FileReader reader = new FileReader(file)) {
                    JsonObject data = JsonParser.parseReader(reader).getAsJsonObject();
                    store.put(collection, playerId.toString(), data);
                    migrated++;
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to migrate legacy {} file {}: {}", dataType, fileName, e.getMessage());
            }
        }

        if (migrated > 0) {
            NeoLog.info(LOGGER, LogCategory.GENERAL, "PlayerDataStore({}): migrated {} legacy player file(s) into the '{}' storage backend.",
                dataType, migrated, StorageManager.getInstance().getActiveType());
        }
    }
}
