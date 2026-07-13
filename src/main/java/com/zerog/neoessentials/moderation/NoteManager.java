package com.zerog.neoessentials.moderation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages staff notes on players with persistent JSON storage. Mirrors
 * {@link WarnManager}'s shape (per-player list, UUID-keyed, offline-name fallback).
 */
public class NoteManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(NoteManager.class);
    private static final NoteManager INSTANCE = new NoteManager();
    public static NoteManager getInstance() { return INSTANCE; }

    private final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    /** targetId → list of notes */
    private final Map<UUID, List<NoteEntry>> noteMap = new ConcurrentHashMap<>();

    private final File notesFile;

    private NoteManager() {
        File dir = new File(com.zerog.neoessentials.util.ResourceUtil.DATA_DIR + "moderation");
        if (!dir.exists()) dir.mkdirs();
        notesFile = new File(dir, "notes.json");
        load();
    }

    /** Add a note to a player's record. */
    public NoteEntry addNote(UUID targetId, String targetName, UUID authorId, String authorName, String text) {
        NoteEntry entry = new NoteEntry(targetId, targetName, authorId, authorName, text);
        noteMap.computeIfAbsent(targetId, k -> new CopyOnWriteArrayList<>()).add(entry);
        save();
        LOGGER.info("[Note] {} added a note on {}: {}", authorName, targetName, text);
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
        if (removed) save();
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

    private void save() {
        try (FileWriter fw = new FileWriter(notesFile)) {
            JsonArray root = new JsonArray();
            for (List<NoteEntry> entries : noteMap.values()) {
                for (NoteEntry e : entries) {
                    JsonObject obj = new JsonObject();
                    obj.addProperty("id", e.getId());
                    obj.addProperty("targetId", e.getTargetId().toString());
                    obj.addProperty("targetName", e.getTargetName());
                    obj.addProperty("authorId", e.getAuthorId() != null ? e.getAuthorId().toString() : "");
                    obj.addProperty("authorName", e.getAuthorName());
                    obj.addProperty("text", e.getText());
                    obj.addProperty("timestamp", e.getTimestamp());
                    root.add(obj);
                }
            }
            gson.toJson(root, fw);
        } catch (Exception ex) {
            LOGGER.error("Failed to save notes.json: {}", ex.getMessage());
        }
    }

    private void load() {
        noteMap.clear();
        if (notesFile == null || !notesFile.exists()) return;
        try (FileReader fr = new FileReader(notesFile)) {
            JsonArray root = gson.fromJson(fr, JsonArray.class);
            if (root == null) return;
            for (JsonElement el : root) {
                JsonObject obj = el.getAsJsonObject();
                String id = obj.get("id").getAsString();
                UUID targetId = UUID.fromString(obj.get("targetId").getAsString());
                String targetName = obj.get("targetName").getAsString();
                String authorIdStr = obj.has("authorId") ? obj.get("authorId").getAsString() : "";
                UUID authorId = authorIdStr.isEmpty() ? null : UUID.fromString(authorIdStr);
                String authorName = obj.get("authorName").getAsString();
                String text = obj.get("text").getAsString();
                long ts = obj.get("timestamp").getAsLong();

                NoteEntry entry = new NoteEntry(id, targetId, targetName, authorId, authorName, text, ts);
                noteMap.computeIfAbsent(targetId, k -> new CopyOnWriteArrayList<>()).add(entry);
            }
            LOGGER.info("NoteManager: loaded {} note(s).", noteMap.values().stream().mapToInt(List::size).sum());
        } catch (Exception ex) {
            LOGGER.error("Failed to load notes.json: {}", ex.getMessage());
        }
    }
}
