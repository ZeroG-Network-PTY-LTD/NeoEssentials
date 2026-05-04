package com.zerog.neoessentials.moderation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
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
import java.util.stream.Collectors;

/**
 * Manages player warnings with persistent JSON storage.
 */
public class WarnManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(WarnManager.class);
    private static final WarnManager INSTANCE = new WarnManager();
    public static WarnManager getInstance() { return INSTANCE; }

    private final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    /** targetId → list of warns */
    private final Map<UUID, List<WarnEntry>> warnMap = new ConcurrentHashMap<>();

    private final File warnsFile;

    private WarnManager() {
        File dir = new File(com.zerog.neoessentials.util.ResourceUtil.DATA_DIR + "moderation");
        if (!dir.exists()) dir.mkdirs();
        warnsFile = new File(dir, "warns.json");
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
        save();
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
            .collect(Collectors.toList());
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
        if (removed) save();
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
        save();
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
                String stored = entry.getValue().get(0).getTargetName();
                if (stored.equalsIgnoreCase(playerName)) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private void save() {
        try (FileWriter fw = new FileWriter(warnsFile)) {
            JsonArray root = new JsonArray();
            for (List<WarnEntry> entries : warnMap.values()) {
                for (WarnEntry e : entries) {
                    JsonObject obj = new JsonObject();
                    obj.addProperty("id",           e.getId());
                    obj.addProperty("targetId",     e.getTargetId().toString());
                    obj.addProperty("targetName",   e.getTargetName());
                    obj.addProperty("warnedById",   e.getWarnedById() != null ? e.getWarnedById().toString() : "");
                    obj.addProperty("warnedBy",     e.getWarnedBy());
                    obj.addProperty("reason",       e.getReason());
                    obj.addProperty("timestamp",    e.getTimestamp());
                    root.add(obj);
                }
            }
            gson.toJson(root, fw);
        } catch (Exception ex) {
            LOGGER.error("Failed to save warns.json: {}", ex.getMessage());
        }
    }

    private void load() {
        warnMap.clear();
        if (warnsFile == null || !warnsFile.exists()) return;
        try (FileReader fr = new FileReader(warnsFile)) {
            JsonArray root = gson.fromJson(fr, JsonArray.class);
            if (root == null) return;
            for (JsonElement el : root) {
                JsonObject obj = el.getAsJsonObject();
                String  id         = obj.get("id").getAsString();
                UUID    targetId   = UUID.fromString(obj.get("targetId").getAsString());
                String  targetName = obj.get("targetName").getAsString();
                String  wbStr      = obj.has("warnedById") ? obj.get("warnedById").getAsString() : "";
                UUID    warnedById = wbStr.isEmpty() ? null : UUID.fromString(wbStr);
                String  warnedBy   = obj.get("warnedBy").getAsString();
                String  reason     = obj.get("reason").getAsString();
                long    ts         = obj.get("timestamp").getAsLong();

                WarnEntry entry = new WarnEntry(id, targetId, targetName, warnedById, warnedBy, reason, ts);
                warnMap.computeIfAbsent(targetId, k -> new java.util.concurrent.CopyOnWriteArrayList<>())
                       .add(entry);
            }
            LOGGER.info("WarnManager: loaded {} warn record(s).", warnMap.values().stream().mapToInt(List::size).sum());
        } catch (Exception ex) {
            LOGGER.error("Failed to load warns.json: {}", ex.getMessage());
        }
    }
}





