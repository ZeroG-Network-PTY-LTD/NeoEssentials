package com.zerog.neoessentials.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Efficient player data storage system that stores each player's data in separate files.
 *
 * <p>Instead of storing all player data in one massive file, this system uses:</p>
 * <pre>
 * config/neoessentials/playerdata/
 * ├── {uuid1}.json  (Player 1's data)
 * ├── {uuid2}.json  (Player 2's data)
 * └── {uuid3}.json  (Player 3's data)
 * </pre>
 *
 * <p>Benefits:</p>
 * <ul>
 *   <li>Fast I/O - only load/save data for specific players</li>
 *   <li>Scalable - works with thousands of players</li>
 *   <li>Easy to find - one file per player</li>
 *   <li>Corruption resistant - one player's corrupt data doesn't affect others</li>
 *   <li>Memory efficient - can unload inactive player data</li>
 * </ul>
 *
 * <p>Thread-safe with atomic file operations.</p>
 *
 * @author NeoEssentials
 * @version 1.0.0
 */
public class PlayerDataStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerDataStore.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final String dataType; // e.g., "homes", "warps", "economy"
    private final File dataDirectory;

    // In-memory cache: UUID -> JsonObject
    private final Map<UUID, JsonObject> cache = new ConcurrentHashMap<>();

    // Track dirty (modified) entries that need saving
    private final Set<UUID> dirtyEntries = ConcurrentHashMap.newKeySet();

    /**
     * Create a new PlayerDataStore for a specific data type.
     *
     * @param dataType The type of data being stored (e.g., "homes", "economy")
     */
    public PlayerDataStore(String dataType) {
        this.dataType = dataType;
        this.dataDirectory = new File(ResourceUtil.CONFIG_DIR, "playerdata/" + dataType);

        // Ensure directory exists
        if (!dataDirectory.exists()) {
            if (dataDirectory.mkdirs()) {
                LOGGER.info("Created playerdata directory for {}: {}", dataType, dataDirectory.getPath());
            }
        }
    }

    /**
     * Load player data from disk.
     *
     * @param playerId Player UUID
     * @return Player data as JsonObject, or new empty JsonObject if not found
     */
    public JsonObject load(UUID playerId) {
        // Check cache first
        if (cache.containsKey(playerId)) {
            return cache.get(playerId);
        }

        // Load from disk
        File playerFile = getPlayerFile(playerId);
        JsonObject data;

        if (playerFile.exists()) {
            try (FileReader reader = new FileReader(playerFile)) {
                data = JsonParser.parseReader(reader).getAsJsonObject();
                LOGGER.debug("Loaded {} data for player {}", dataType, playerId);
            } catch (Exception e) {
                LOGGER.error("Failed to load {} data for player {}: {}", dataType, playerId, e.getMessage(), e);
                data = new JsonObject();
            }
        } else {
            data = new JsonObject();
            LOGGER.debug("No {} data found for player {}, using empty data", dataType, playerId);
        }

        // Cache it
        cache.put(playerId, data);
        return data;
    }

    /**
     * Save player data to disk (atomic operation).
     *
     * @param playerId Player UUID
     * @param data Player data to save
     */
    public void save(UUID playerId, JsonObject data) {
        cache.put(playerId, data);
        dirtyEntries.add(playerId);

        // Immediate save
        flush(playerId);
    }

    /**
     * Flush dirty (modified) data to disk for a specific player.
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

        File playerFile = getPlayerFile(playerId);
        File tempFile = new File(playerFile.getAbsolutePath() + ".tmp");

        try {
            // Write to temp file
            try (FileWriter writer = new FileWriter(tempFile)) {
                GSON.toJson(data, writer);
            }

            // Atomic move to actual file
            Files.move(tempFile.toPath(), playerFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE);

            dirtyEntries.remove(playerId);
            LOGGER.debug("Saved {} data for player {}", dataType, playerId);

        } catch (Exception e) {
            LOGGER.error("Failed to save {} data for player {}: {}", dataType, playerId, e.getMessage(), e);

            // Clean up temp file if it exists
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    /**
     * Flush all dirty data to disk.
     */
    public void flushAll() {
        if (dirtyEntries.isEmpty()) {
            return;
        }

        LOGGER.info("Flushing {} dirty {} entries...", dirtyEntries.size(), dataType);

        // Create a copy to avoid ConcurrentModificationException
        Set<UUID> toFlush = new HashSet<>(dirtyEntries);

        for (UUID playerId : toFlush) {
            flush(playerId);
        }

        LOGGER.info("Flushed all {} data", dataType);
    }

    /**
     * Delete player data from disk and cache.
     *
     * @param playerId Player UUID
     * @return true if deleted successfully
     */
    public boolean delete(UUID playerId) {
        cache.remove(playerId);
        dirtyEntries.remove(playerId);

        File playerFile = getPlayerFile(playerId);
        if (playerFile.exists()) {
            boolean deleted = playerFile.delete();
            if (deleted) {
                LOGGER.info("Deleted {} data for player {}", dataType, playerId);
            }
            return deleted;
        }

        return true;
    }

    /**
     * Check if player has data.
     *
     * @param playerId Player UUID
     * @return true if player has data file
     */
    public boolean hasData(UUID playerId) {
        return getPlayerFile(playerId).exists() || cache.containsKey(playerId);
    }

    /**
     * Get all player UUIDs that have data.
     *
     * @return Set of player UUIDs
     */
    public Set<UUID> getAllPlayerIds() {
        Set<UUID> playerIds = new HashSet<>();

        // Add from cache
        playerIds.addAll(cache.keySet());

        // Add from disk
        File[] files = dataDirectory.listFiles((dir, name) -> name.endsWith(".json"));
        if (files != null) {
            for (File file : files) {
                try {
                    String fileName = file.getName();
                    String uuidStr = fileName.substring(0, fileName.length() - 5); // Remove .json
                    UUID uuid = UUID.fromString(uuidStr);
                    playerIds.add(uuid);
                } catch (Exception e) {
                    LOGGER.warn("Invalid player data file: {}", file.getName());
                }
            }
        }

        return playerIds;
    }

    /**
     * Get player file path.
     *
     * @param playerId Player UUID
     * @return Player data file
     */
    private File getPlayerFile(UUID playerId) {
        return new File(dataDirectory, playerId.toString() + ".json");
    }

    /**
     * Unload player data from cache (keeps on disk).
     * Useful for memory management with many players.
     *
     * @param playerId Player UUID
     */
    public void unload(UUID playerId) {
        // Flush before unloading
        flush(playerId);

        cache.remove(playerId);
        LOGGER.debug("Unloaded {} data for player {} from cache", dataType, playerId);
    }

    /**
     * Clear all data (use with caution!).
     */
    public void clearAll() {
        cache.clear();
        dirtyEntries.clear();

        // Delete all files
        File[] files = dataDirectory.listFiles((dir, name) -> name.endsWith(".json"));
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }

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
     * Get total number of players with data (on disk).
     *
     * @return Total player count
     */
    public int getTotalPlayers() {
        File[] files = dataDirectory.listFiles((dir, name) -> name.endsWith(".json"));
        return files != null ? files.length : 0;
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
}

