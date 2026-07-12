package com.zerog.neoessentials.moderation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.storage.DataStore;
import com.zerog.neoessentials.storage.StorageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player warnings. Persisted via {@link StorageManager} — one record per warn
 * in the {@code "warns"} collection, keyed by the warn's own id. The legacy
 * {@code moderation/warns.json} file (a single JSON array) is imported once,
 * automatically, the first time this runs against an empty store.
 */
public class WarnManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(WarnManager.class);
    private static final String COLLECTION = "warns";
    private static final WarnManager INSTANCE = new WarnManager();
    public static WarnManager getInstance() { return INSTANCE; }

    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
    private final DataStore store;

    /** targetId → list of warns */
    private final Map<UUID, List<WarnEntry>> warnMap = new ConcurrentHashMap<>();

    private WarnManager() {
        this.store = StorageManager.getInstance().getStore();
        migrateLegacyFileIfNeeded();
        load();
    }

    /**
     * Add a warning for a player. Logs to console.
     */
    public WarnEntry addWarn(UUID targetId, String targetName,
                             UUID warnedById, String warnedBy, String reason) {
        WarnEntry entry = new WarnEntry(targetId, targetName, warnedById, warnedBy, reason);
        warnMap.computeIfAbsent(targetId, k -> new java.util.concurrent.CopyOnWriteArrayList<>())
               .add(entry);
        store.put(COLLECTION, entry.getId(), toJson(entry));
        // Always log to console so it appears in server logs
        LOGGER.info("[Warn] {} warned {} — Reason: {} ({})",
            warnedBy, targetName, reason, entry.getFormattedTime());
        return entry;
    }

    /**
     * Get all warnings for a player, sorted newest first.
     */
    public List<WarnEntry> getWarnings(UUID targetId) {
        List<WarnEntry> list = warnMap.getOrDefault(targetId, new ArrayList<>());
        return list.stream()
            .sorted((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()))
            .toList();
    }

    /**
     * Count warnings for a player.
     */
    public int getWarnCount(UUID targetId) {
        return warnMap.getOrDefault(targetId, new ArrayList<>()).size();
    }

    /**
     * Remove a specific warn by its ID.
     *
     * @return true if found and removed
     */
    public boolean removeWarn(UUID targetId, String warnId) {
        List<WarnEntry> list = warnMap.get(targetId);
        if (list == null) return false;
        boolean removed = list.removeIf(w -> w.getId().equals(warnId));
        if (removed) store.delete(COLLECTION, warnId);
        return removed;
    }

    /**
     * Clear all warnings for a player.
     *
     * @return number of warnings removed
     */
    public int clearWarnings(UUID targetId) {
        List<WarnEntry> removed = warnMap.remove(targetId);
        if (removed == null || removed.isEmpty()) return 0;
        for (WarnEntry entry : removed) {
            store.delete(COLLECTION, entry.getId());
        }
        return removed.size();
    }

    /**
     * Get all warn lists (all players), for dashboard use.
     */
    public Collection<List<WarnEntry>> getAllWarnings() {
        return warnMap.values();
    }

    /**
     * Look up the UUID of a player by their stored name (case-insensitive).
     * Used to resolve offline players via warn history.
     */
    public UUID findUUIDByName(String playerName) {
        for (Map.Entry<UUID, List<WarnEntry>> entry : warnMap.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                String stored = entry.getValue().getFirst().getTargetName();
                if (stored.equalsIgnoreCase(playerName)) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private JsonObject toJson(WarnEntry e) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id",           e.getId());
        obj.addProperty("targetId",     e.getTargetId().toString());
        obj.addProperty("targetName",   e.getTargetName());
        obj.addProperty("warnedById",   e.getWarnedById() != null ? e.getWarnedById().toString() : "");
        obj.addProperty("warnedBy",     e.getWarnedBy());
        obj.addProperty("reason",       e.getReason());
        obj.addProperty("timestamp",    e.getTimestamp());
        return obj;
    }

    private WarnEntry fromJson(JsonObject obj) {
        String  id         = obj.get("id").getAsString();
        UUID    targetId   = UUID.fromString(obj.get("targetId").getAsString());
        String  targetName = obj.get("targetName").getAsString();
        String  wbStr      = obj.has("warnedById") ? obj.get("warnedById").getAsString() : "";
        UUID    warnedById = wbStr.isEmpty() ? null : UUID.fromString(wbStr);
        String  warnedBy   = obj.get("warnedBy").getAsString();
        String  reason     = obj.get("reason").getAsString();
        long    ts         = obj.get("timestamp").getAsLong();
        return new WarnEntry(id, targetId, targetName, warnedById, warnedBy, reason, ts);
    }

    private void load() {
        warnMap.clear();
        int count = 0;
        for (JsonObject obj : store.getAll(COLLECTION).values()) {
            WarnEntry entry = fromJson(obj);
            warnMap.computeIfAbsent(entry.getTargetId(), k -> new java.util.concurrent.CopyOnWriteArrayList<>())
                   .add(entry);
            count++;
        }
        LOGGER.info("WarnManager: loaded {} warn record(s).", count);
    }

    /**
     * One-time import of the legacy {@code moderation/warns.json} (a single JSON array)
     * into the active {@link DataStore}, if that store's "warns" collection is still
     * empty and storage.autoMigrate is enabled.
     */
    private void migrateLegacyFileIfNeeded() {
        if (store.hasAnyData(COLLECTION)) return;
        if (!ConfigManager.getInstance().isStorageAutoMigrateEnabled()) return;

        File legacyFile = new File(com.zerog.neoessentials.util.ResourceUtil.DATA_DIR + "moderation", "warns.json");
        if (!legacyFile.exists()) return;

        try (FileReader fr = new FileReader(legacyFile)) {
            JsonArray root = gson.fromJson(fr, JsonArray.class);
            if (root == null) return;
            int migrated = 0;
            for (JsonElement el : root) {
                JsonObject obj = el.getAsJsonObject();
                String id = obj.get("id").getAsString();
                store.put(COLLECTION, id, obj);
                migrated++;
            }
            if (migrated > 0) {
                LOGGER.info("WarnManager: migrated {} warn(s) from legacy warns.json into the '{}' storage backend.",
                    migrated, StorageManager.getInstance().getActiveType());
            }
        } catch (Exception ex) {
            LOGGER.error("Failed to migrate legacy warns.json: {}", ex.getMessage());
        }
    }
}
