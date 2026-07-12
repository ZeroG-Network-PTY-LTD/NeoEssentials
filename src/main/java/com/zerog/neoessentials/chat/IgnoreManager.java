package com.zerog.neoessentials.chat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe manager for player ignore lists.
 * Handles ignoring/unignoring players and message filtering.
 * Ignore lists are persisted to disk and survive server restarts.
 */
public class IgnoreManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(IgnoreManager.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    // Thread-safe storage for ignore relationships: ignorer (lowercase) → set of ignored names (lowercase)
    private static final Map<String, Set<String>> ignoreMap = new ConcurrentHashMap<>();

    private static final File IGNORE_FILE =
        com.zerog.neoessentials.util.ResourceUtil.getDataFile("chat/ignore_lists.json");

    private static final String COLLECTION = "ignore_lists";
    private static final com.zerog.neoessentials.storage.DataStore store =
        com.zerog.neoessentials.storage.StorageManager.getInstance().getStore();

    static {
        load();
    }

    // ── Persistence ──────────────────────────────────────────────────────────

    private static void load() {
        migrateLegacyFileIfNeeded();
        for (Map.Entry<String, JsonObject> entry : store.getAll(COLLECTION).entrySet()) {
            try {
                JsonObject obj = entry.getValue();
                Set<String> ignored = ConcurrentHashMap.newKeySet();
                if (obj.has("ignored")) {
                    for (JsonElement el : obj.getAsJsonArray("ignored")) {
                        ignored.add(el.getAsString());
                    }
                }
                if (!ignored.isEmpty()) {
                    ignoreMap.put(entry.getKey(), ignored);
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to load ignore data for {}: {}", entry.getKey(), e.getMessage());
            }
        }
        LOGGER.debug("IgnoreManager: loaded ignore data for {} player(s).", ignoreMap.size());
    }

    /** Persists a single player's ignore set as one record — {@code id} = player name (lowercase). */
    private static void persist(String playerName, Set<String> ignored) {
        JsonObject obj = new JsonObject();
        JsonArray arr = new JsonArray();
        ignored.forEach(arr::add);
        obj.add("ignored", arr);
        store.put(COLLECTION, playerName, obj);
    }

    /**
     * One-time import of the legacy {@code chat/ignore_lists.json} file into the active
     * DataStore, if it's still empty and storage.autoMigrate is enabled.
     */
    private static void migrateLegacyFileIfNeeded() {
        if (store.hasAnyData(COLLECTION)) return;
        if (!com.zerog.neoessentials.config.ConfigManager.getInstance().isStorageAutoMigrateEnabled()) return;
        if (!IGNORE_FILE.exists()) return;

        int migrated = 0;
        try (FileReader fr = new FileReader(IGNORE_FILE)) {
            JsonObject root = GSON.fromJson(fr, JsonObject.class);
            if (root == null) return;
            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                try {
                    JsonArray arr = entry.getValue().getAsJsonArray();
                    if (arr.isEmpty()) continue;
                    JsonObject record = new JsonObject();
                    record.add("ignored", arr.deepCopy());
                    store.put(COLLECTION, entry.getKey(), record);
                    migrated++;
                } catch (Exception e) {
                    LOGGER.warn("Failed to migrate legacy ignore entry {}: {}", entry.getKey(), e.getMessage());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to migrate legacy ignore_lists.json: {}", e.getMessage());
        }

        if (migrated > 0) {
            LOGGER.info("IgnoreManager: migrated {} ignore-list record(s) from legacy ignore_lists.json into the '{}' storage backend.",
                migrated, com.zerog.neoessentials.storage.StorageManager.getInstance().getActiveType());
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public static void ignore(ServerPlayer player, String targetName) {
        String playerName = player.getName().getString().toLowerCase();
        Set<String> ignored = ignoreMap.computeIfAbsent(playerName, k -> ConcurrentHashMap.newKeySet());
        ignored.add(targetName.toLowerCase());
        persist(playerName, ignored);
    }

    public static void unignore(ServerPlayer player, String targetName) {
        String playerName = player.getName().getString().toLowerCase();
        Set<String> ignored = ignoreMap.get(playerName);
        if (ignored != null) {
            ignored.remove(targetName.toLowerCase());
            if (ignored.isEmpty()) {
                ignoreMap.remove(playerName);
                store.delete(COLLECTION, playerName);
            } else {
                persist(playerName, ignored);
            }
        }
    }

    public static boolean isIgnoring(ServerPlayer player, ServerPlayer target) {
        String playerName = player.getName().getString().toLowerCase();
        String targetName = target.getName().getString().toLowerCase();
        Set<String> ignored = ignoreMap.get(playerName);
        return ignored != null && ignored.contains(targetName);
    }

    /**
     * Check if a player is ignoring another player by name
     */
    public static boolean isIgnoring(ServerPlayer player, String targetName) {
        String playerName = player.getName().getString().toLowerCase();
        Set<String> ignored = ignoreMap.get(playerName);
        return ignored != null && ignored.contains(targetName.toLowerCase());
    }

    /**
     * Get the ignore list for a player
     */
    //noinspection unused
    @SuppressWarnings("unused")
    public static Set<String> getIgnoreList(ServerPlayer player) {
        String playerName = player.getName().getString().toLowerCase();
        Set<String> ignored = ignoreMap.get(playerName);
        return ignored != null ? Set.copyOf(ignored) : Set.of();
    }

    /**
     * Clean up transient session data when a player disconnects.
     * NOTE: Ignore lists are persisted and intentionally NOT removed here —
     * a player should still have their ignore list when they reconnect.
     */
    //noinspection unused
    @SuppressWarnings("unused")
    public static void cleanupPlayer(ServerPlayer ignoredPlayer) {
        // Nothing to clean up — ignore lists survive sessions via persistence.
        // Deliberately left empty to avoid accidentally destroying player data.
    }
}
