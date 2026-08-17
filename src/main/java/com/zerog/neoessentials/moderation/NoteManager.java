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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;

/**
 * Manages staff notes on players. Mirrors {@link WarnManager}'s shape (per-player list,
 * UUID-keyed, offline-name fallback). Persisted via {@link StorageManager} — one record
 * per note in the {@code "notes"} collection, keyed by the note's own id. The legacy
 * {@code moderation/notes.json} file (a single JSON array) is imported once, automatically,
 * the first time this runs against an empty store.
 */
public class NoteManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(NoteManager.class);
    private static final String COLLECTION = "notes";
    private static final NoteManager INSTANCE = new NoteManager();
    public static NoteManager getInstance() { return INSTANCE; }

    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
    private final DataStore store;

    /** targetId → list of notes */
    private final Map<UUID, List<NoteEntry>> noteMap = new ConcurrentHashMap<>();

    private NoteManager() {
        this.store = StorageManager.getInstance().getStore();
        migrateLegacyFileIfNeeded();
        load();
    }

    /** Add a note to a player's record. */
    public NoteEntry addNote(UUID targetId, String targetName, UUID authorId, String authorName, String text) {
        NoteEntry entry = new NoteEntry(targetId, targetName, authorId, authorName, text);
        noteMap.computeIfAbsent(targetId, k -> new CopyOnWriteArrayList<>()).add(entry);
        store.put(COLLECTION, entry.getId(), toJson(entry));
        NeoLog.info(LOGGER, LogCategory.MODERATION, "[Note] {} added a note on {}: {}", authorName, targetName, text);
        return entry;
    }

    /** All notes for a player, sorted newest first. */
    public List<NoteEntry> getNotes(UUID targetId) {
        List<NoteEntry> list = noteMap.getOrDefault(targetId, new ArrayList<>());
        return list.stream()
            .sorted((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()))
            .toList();
    }

    /**
     * Remove a specific note by its ID.
     * @return true if found and removed
     */
    public boolean removeNote(UUID targetId, String noteId) {
        List<NoteEntry> list = noteMap.get(targetId);
        if (list == null) return false;
        boolean removed = list.removeIf(n -> n.getId().equals(noteId));
        if (removed) store.delete(COLLECTION, noteId);
        return removed;
    }

    /** All note lists (all players), for dashboard use. */
    public Collection<List<NoteEntry>> getAllNotes() {
        return noteMap.values();
    }

    /** Look up the UUID of a player by their stored name (case-insensitive), for offline lookups. */
    public UUID findUUIDByName(String playerName) {
        for (Map.Entry<UUID, List<NoteEntry>> entry : noteMap.entrySet()) {
            if (!entry.getValue().isEmpty() && entry.getValue().getFirst().getTargetName().equalsIgnoreCase(playerName)) {
                return entry.getKey();
            }
        }
        return null;
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private JsonObject toJson(NoteEntry e) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", e.getId());
        obj.addProperty("targetId", e.getTargetId().toString());
        obj.addProperty("targetName", e.getTargetName());
        obj.addProperty("authorId", e.getAuthorId() != null ? e.getAuthorId().toString() : "");
        obj.addProperty("authorName", e.getAuthorName());
        obj.addProperty("text", e.getText());
        obj.addProperty("timestamp", e.getTimestamp());
        return obj;
    }

    private NoteEntry fromJson(JsonObject obj) {
        String id = obj.get("id").getAsString();
        UUID targetId = UUID.fromString(obj.get("targetId").getAsString());
        String targetName = obj.get("targetName").getAsString();
        String authorIdStr = obj.has("authorId") ? obj.get("authorId").getAsString() : "";
        UUID authorId = authorIdStr.isEmpty() ? null : UUID.fromString(authorIdStr);
        String authorName = obj.get("authorName").getAsString();
        String text = obj.get("text").getAsString();
        long ts = obj.get("timestamp").getAsLong();
        return new NoteEntry(id, targetId, targetName, authorId, authorName, text, ts);
    }

    private void load() {
        noteMap.clear();
        int count = 0;
        for (JsonObject obj : store.getAll(COLLECTION).values()) {
            NoteEntry entry = fromJson(obj);
            noteMap.computeIfAbsent(entry.getTargetId(), k -> new CopyOnWriteArrayList<>()).add(entry);
            count++;
        }
        NeoLog.info(LOGGER, LogCategory.MODERATION, "NoteManager: loaded {} note(s).", count);
    }

    /**
     * One-time import of the legacy {@code moderation/notes.json} (a single JSON array)
     * into the active {@link DataStore}, if that store's "notes" collection is still
     * empty and storage.autoMigrate is enabled.
     */
    private void migrateLegacyFileIfNeeded() {
        if (store.hasAnyData(COLLECTION)) return;
        if (!ConfigManager.getInstance().isStorageAutoMigrateEnabled()) return;

        File legacyFile = new File(com.zerog.neoessentials.util.ResourceUtil.DATA_DIR + "moderation", "notes.json");
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
                NeoLog.info(LOGGER, LogCategory.MODERATION, "NoteManager: migrated {} note(s) from legacy notes.json into the '{}' storage backend.",
                    migrated, StorageManager.getInstance().getActiveType());
            }
        } catch (Exception ex) {
            LOGGER.error("Failed to migrate legacy notes.json: {}", ex.getMessage());
        }
    }
}
