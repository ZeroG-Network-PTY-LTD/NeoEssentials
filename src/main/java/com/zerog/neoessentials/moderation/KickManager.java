package com.zerog.neoessentials.moderation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.storage.DataStore;
import com.zerog.neoessentials.storage.StorageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Records kick history. Kicks are instantaneous (no active/expiry state like a ban or
 * mute) — this is purely a persisted, queryable log matching ban-management plugins'
 * per-player punishment record, since previously a kick was fire-and-forget with at most
 * an optional unstructured log line (see {@code KickCommand}).
 *
 * <p>Persisted via {@link StorageManager} (JSON/YAML/SQLite/MySQL, per config) — one
 * record per kick in the {@code "kicks"} collection, keyed by the kick's own id. The
 * legacy {@code moderation/kicks.json} file (a single JSON array) is imported once,
 * automatically, the first time this runs against an empty store.
 */
public class KickManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(KickManager.class);
    private static final String COLLECTION = "kicks";
    private static KickManager instance;
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
    private final DataStore store;
    private final List<KickEntry> kicks = new CopyOnWriteArrayList<>();

    public static class KickEntry {
        public String id;
        public String playerName;
        public UUID playerId; // null if the target's UUID wasn't resolvable at kick time
        public String reason;
        public String kickedBy;
        public long kickTime;

        public KickEntry(String playerName, UUID playerId, String reason, String kickedBy) {
            this.id = UUID.randomUUID().toString();
            this.playerName = playerName;
            this.playerId = playerId;
            this.reason = reason;
            this.kickedBy = kickedBy;
            this.kickTime = System.currentTimeMillis();
        }
    }

    private KickManager() {
        this.store = StorageManager.getInstance().getStore();
        migrateLegacyFileIfNeeded();
        load();
    }

    public static KickManager getInstance() {
        if (instance == null) {
            instance = new KickManager();
        }
        return instance;
    }

    /** Records a kick. Call this from KickCommand right after disconnecting the player. */
    public void recordKick(String playerName, UUID playerId, String reason, String kickedBy) {
        KickEntry entry = new KickEntry(playerName, playerId, reason, kickedBy);
        kicks.add(entry);
        store.put(COLLECTION, entry.id, toJson(entry));
    }

    /** Kick history for a player by UUID, newest first. */
    public List<KickEntry> getKickHistory(UUID playerId) {
        List<KickEntry> history = new ArrayList<>();
        for (KickEntry entry : kicks) {
            if (playerId.equals(entry.playerId)) history.add(entry);
        }
        history.sort((a, b) -> Long.compare(b.kickTime, a.kickTime));
        return history;
    }

    /** Kick history for a player by name (case-insensitive) — fallback when a UUID wasn't recorded. */
    public List<KickEntry> getKickHistory(String playerName) {
        List<KickEntry> history = new ArrayList<>();
        for (KickEntry entry : kicks) {
            if (entry.playerName.equalsIgnoreCase(playerName)) history.add(entry);
        }
        history.sort((a, b) -> Long.compare(b.kickTime, a.kickTime));
        return history;
    }

    /** Every kick ever recorded, newest first — for dashboard/overview views. */
    public List<KickEntry> getAllKicks() {
        List<KickEntry> all = new ArrayList<>(kicks);
        all.sort((a, b) -> Long.compare(b.kickTime, a.kickTime));
        return all;
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private void load() {
        for (JsonObject obj : store.getAll(COLLECTION).values()) {
            kicks.add(fromJson(obj));
        }
    }

    private JsonObject toJson(KickEntry entry) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", entry.id);
        obj.addProperty("playerName", entry.playerName);
        obj.addProperty("playerId", entry.playerId != null ? entry.playerId.toString() : null);
        obj.addProperty("reason", entry.reason);
        obj.addProperty("kickedBy", entry.kickedBy);
        obj.addProperty("kickTime", entry.kickTime);
        return obj;
    }

    private KickEntry fromJson(JsonObject obj) {
        UUID playerId = obj.has("playerId") && !obj.get("playerId").isJsonNull()
            ? UUID.fromString(obj.get("playerId").getAsString()) : null;
        KickEntry entry = new KickEntry(
            obj.get("playerName").getAsString(),
            playerId,
            obj.has("reason") && !obj.get("reason").isJsonNull() ? obj.get("reason").getAsString() : null,
            obj.get("kickedBy").getAsString()
        );
        entry.id = obj.has("id") ? obj.get("id").getAsString() : UUID.randomUUID().toString();
        entry.kickTime = obj.has("kickTime") ? obj.get("kickTime").getAsLong() : System.currentTimeMillis();
        return entry;
    }

    /**
     * One-time import of the legacy {@code moderation/kicks.json} (a single JSON array)
     * into the active {@link DataStore}, if that store's "kicks" collection is still
     * empty and storage.autoMigrate is enabled. Safe to call on every boot — it's a
     * no-op once the store already has data.
     */
    private void migrateLegacyFileIfNeeded() {
        if (store.hasAnyData(COLLECTION)) return;
        if (!ConfigManager.getInstance().isStorageAutoMigrateEnabled()) return;

        File legacyFile = new File(com.zerog.neoessentials.util.ResourceUtil.DATA_DIR + "moderation", "kicks.json");
        if (!legacyFile.exists()) return;

        try (FileReader reader = new FileReader(legacyFile)) {
            JsonObject root = gson.fromJson(reader, JsonObject.class);
            if (root == null || !root.has("kicks")) return;
            int migrated = 0;
            for (JsonElement element : root.getAsJsonArray("kicks")) {
                JsonObject obj = element.getAsJsonObject();
                String id = obj.has("id") ? obj.get("id").getAsString() : UUID.randomUUID().toString();
                store.put(COLLECTION, id, obj);
                migrated++;
            }
            if (migrated > 0) {
                LOGGER.info("KickManager: migrated {} kick record(s) from legacy kicks.json into the '{}' storage backend.",
                    migrated, StorageManager.getInstance().getActiveType());
            }
        } catch (IOException e) {
            LOGGER.error("Failed to migrate legacy kicks.json", e);
        }
    }
}
