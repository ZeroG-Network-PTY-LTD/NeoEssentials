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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Records kick history. Kicks are instantaneous (no active/expiry state like a ban or
 * mute) — this is purely a persisted, queryable log matching ban-management plugins'
 * per-player punishment record, since previously a kick was fire-and-forget with at most
 * an optional unstructured log line (see {@code KickCommand}).
 */
public class KickManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(KickManager.class);
    private static KickManager instance;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final File kickFile;
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
        File moderationDir = new File(com.zerog.neoessentials.util.ResourceUtil.DATA_DIR + "moderation");
        if (!moderationDir.exists() && !moderationDir.mkdirs()) {
            LOGGER.error("Failed to create moderation directory: {}", moderationDir.getAbsolutePath());
        }
        this.kickFile = new File(moderationDir, "kicks.json");
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
        kicks.add(new KickEntry(playerName, playerId, reason, kickedBy));
        save();
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

    private void load() {
        if (!kickFile.exists()) return;
        try (FileReader reader = new FileReader(kickFile)) {
            JsonObject root = gson.fromJson(reader, JsonObject.class);
            if (root == null || !root.has("kicks")) return;
            for (JsonElement element : root.getAsJsonArray("kicks")) {
                JsonObject obj = element.getAsJsonObject();
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
                kicks.add(entry);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load kicks.json", e);
        }
    }

    private void save() {
        try (FileWriter writer = new FileWriter(kickFile)) {
            JsonObject root = new JsonObject();
            JsonArray arr = new JsonArray();
            for (KickEntry entry : kicks) {
                JsonObject obj = new JsonObject();
                obj.addProperty("id", entry.id);
                obj.addProperty("playerName", entry.playerName);
                obj.addProperty("playerId", entry.playerId != null ? entry.playerId.toString() : null);
                obj.addProperty("reason", entry.reason);
                obj.addProperty("kickedBy", entry.kickedBy);
                obj.addProperty("kickTime", entry.kickTime);
                arr.add(obj);
            }
            root.add("kicks", arr);
            gson.toJson(root, writer);
        } catch (IOException e) {
            LOGGER.error("Failed to save kicks.json", e);
        }
    }
}
