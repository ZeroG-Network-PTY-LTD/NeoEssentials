package com.zerog.neoessentials.hologram;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.zerog.neoessentials.storage.DataStore;
import com.zerog.neoessentials.util.ResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton manager for all {@link HologramData} entries.
 * Persists to the {@code "holograms"} collection of the active
 * {@link com.zerog.neoessentials.storage.DataStore} backend (JSON/YAML/SQLite/MySQL),
 * one record per hologram id.
 */
public class HologramManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(HologramManager.class);
    // Only used to parse the legacy holograms.json file during one-time migration.
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String COLLECTION = "holograms";

    // ── Singleton ─────────────────────────────────────────────────────────────

    private static HologramManager instance;
    public static HologramManager getInstance() {
        if (instance == null) instance = new HologramManager();
        return instance;
    }
    private HologramManager() {}

    // ── State ─────────────────────────────────────────────────────────────────

    /** hologram id → data */
    private final ConcurrentHashMap<String, HologramData> holograms = new ConcurrentHashMap<>();
    private final DataStore store = com.zerog.neoessentials.storage.StorageManager.getInstance().getStore();

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    public void initialize() {
        try {
            // Clear before loading so that holograms deleted from storage
            // do not survive a /hologram reload (previously the map was only appended to).
            holograms.clear();
            migrateLegacyFilesIfNeeded();
            load();
            LOGGER.info("[Hologram] Loaded {} hologram(s).", holograms.size());
        } catch (Exception e) {
            LOGGER.error("[Hologram] Failed to load holograms — starting fresh.", e);
        }
    }

    public void shutdown() {
        // Every mutation already persists immediately via the DataStore, so there's
        // nothing left to flush here beyond letting the backend close cleanly.
        LOGGER.info("[Hologram] Holograms are persisted; nothing further to flush on shutdown.");
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    /** Create or replace a hologram. */
    public void registerHologram(HologramData data) {
        String id = data.id.toLowerCase(Locale.ROOT);
        holograms.put(id, data);
        trySave(id, data);
    }

    /** Remove a hologram by id. Returns removed data or null. */
    public HologramData removeHologram(String id) {
        String key = id.toLowerCase(Locale.ROOT);
        HologramData removed = holograms.remove(key);
        if (removed != null) {
            try {
                store.delete(COLLECTION, key);
            } catch (Exception e) {
                LOGGER.error("[Hologram] Failed to delete hologram '{}' from storage", key, e);
            }
        }
        return removed;
    }

    public HologramData getHologram(String id) {
        return holograms.get(id.toLowerCase(Locale.ROOT));
    }

    public boolean exists(String id) {
        return holograms.containsKey(id.toLowerCase(Locale.ROOT));
    }

    public Collection<HologramData> getAllHolograms() {
        return Collections.unmodifiableCollection(holograms.values());
    }

    /** Returns all holograms whose {@code world} matches {@code dimensionKey}. */
    public List<HologramData> getHologramsForWorld(String dimensionKey) {
        List<HologramData> list = new ArrayList<>();
        for (HologramData d : holograms.values()) {
            if (dimensionKey.equals(d.world)) list.add(d);
        }
        return list;
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private void load() {
        for (JsonObject obj : store.getAll(COLLECTION).values()) {
            HologramData d = fromJson(obj);
            // Init transient / collection fields that JSON leaves null when absent.
            if (d.entityUUIDs == null) d.entityUUIDs = new ArrayList<>();
            if (d.lines == null)       d.lines       = new ArrayList<>();
            for (HologramLine line : d.lines) {
                if (line.frames == null) line.frames = new ArrayList<>();
            }
            holograms.put(d.id.toLowerCase(Locale.ROOT), d);
        }
    }

    private void trySave(String id, HologramData data) {
        try {
            store.put(COLLECTION, id, toJson(data));
        } catch (Exception e) {
            LOGGER.error("[Hologram] Failed to save hologram '{}'", id, e);
        }
    }

    // ── Serialization ─────────────────────────────────────────────────────────

    private JsonObject toJson(HologramData d) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", d.id);
        obj.addProperty("world", d.world);
        obj.addProperty("x", d.x);
        obj.addProperty("y", d.y);
        obj.addProperty("z", d.z);

        JsonArray lines = new JsonArray();
        for (HologramLine line : d.lines) {
            JsonObject lineObj = new JsonObject();
            lineObj.addProperty("lineId", line.lineId);
            lineObj.addProperty("text", line.text);
            JsonArray frames = new JsonArray();
            for (String frame : line.frames) frames.add(frame);
            lineObj.add("frames", frames);
            lineObj.addProperty("animFrameIntervalTicks", line.animFrameIntervalTicks);
            lines.add(lineObj);
        }
        obj.add("lines", lines);

        obj.addProperty("refreshInterval", d.refreshInterval);
        obj.addProperty("visible", d.visible);
        obj.addProperty("interactive", d.interactive);

        obj.addProperty("scale", d.scale);
        obj.addProperty("lineSpacing", d.lineSpacing);
        obj.addProperty("textShadow", d.textShadow);
        obj.addProperty("textOpacity", d.textOpacity);
        obj.addProperty("backgroundColorArgb", d.backgroundColorArgb);
        obj.addProperty("textAlign", d.textAlign);
        obj.addProperty("seeThrough", d.seeThrough);
        obj.addProperty("lineWidth", d.lineWidth);
        obj.addProperty("viewRange", d.viewRange);

        obj.addProperty("billboardMode", d.billboardMode);
        obj.addProperty("spinEnabled", d.spinEnabled);
        obj.addProperty("spinSpeedDegrees", d.spinSpeedDegrees);
        obj.addProperty("spinAxis", d.spinAxis);
        obj.addProperty("spinTrackPlayer", d.spinTrackPlayer);

        obj.addProperty("hoverEnabled", d.hoverEnabled);
        obj.addProperty("hoverAmplitude", d.hoverAmplitude);
        obj.addProperty("hoverSpeedDegrees", d.hoverSpeedDegrees);

        return obj;
    }

    private HologramData fromJson(JsonObject obj) {
        HologramData d = new HologramData();
        d.id = obj.has("id") ? obj.get("id").getAsString() : "";
        d.world = obj.has("world") ? obj.get("world").getAsString() : "minecraft:overworld";
        d.x = obj.has("x") ? obj.get("x").getAsDouble() : 0;
        d.y = obj.has("y") ? obj.get("y").getAsDouble() : 64;
        d.z = obj.has("z") ? obj.get("z").getAsDouble() : 0;

        d.lines = new ArrayList<>();
        if (obj.has("lines")) {
            for (var el : obj.getAsJsonArray("lines")) {
                JsonObject lineObj = el.getAsJsonObject();
                HologramLine line = new HologramLine();
                line.lineId = lineObj.has("lineId") ? lineObj.get("lineId").getAsString() : UUID.randomUUID().toString();
                line.text = lineObj.has("text") ? lineObj.get("text").getAsString() : "";
                line.frames = new ArrayList<>();
                if (lineObj.has("frames")) {
                    for (var frame : lineObj.getAsJsonArray("frames")) {
                        line.frames.add(frame.getAsString());
                    }
                }
                line.animFrameIntervalTicks = lineObj.has("animFrameIntervalTicks") ? lineObj.get("animFrameIntervalTicks").getAsInt() : 0;
                d.lines.add(line);
            }
        }

        d.refreshInterval = obj.has("refreshInterval") ? obj.get("refreshInterval").getAsInt() : 5;
        d.visible = !obj.has("visible") || obj.get("visible").getAsBoolean();
        d.interactive = obj.has("interactive") && obj.get("interactive").getAsBoolean();

        d.scale = obj.has("scale") ? obj.get("scale").getAsFloat() : 1.0f;
        d.lineSpacing = obj.has("lineSpacing") ? obj.get("lineSpacing").getAsFloat() : 0.3f;
        d.textShadow = obj.has("textShadow") && obj.get("textShadow").getAsBoolean();
        d.textOpacity = obj.has("textOpacity") ? obj.get("textOpacity").getAsInt() : 255;
        d.backgroundColorArgb = obj.has("backgroundColorArgb") ? obj.get("backgroundColorArgb").getAsInt() : 0x00000000;
        d.textAlign = obj.has("textAlign") ? obj.get("textAlign").getAsInt() : 0;
        d.seeThrough = obj.has("seeThrough") && obj.get("seeThrough").getAsBoolean();
        d.lineWidth = obj.has("lineWidth") ? obj.get("lineWidth").getAsInt() : 200;
        d.viewRange = obj.has("viewRange") ? obj.get("viewRange").getAsFloat() : 1.0f;

        d.billboardMode = obj.has("billboardMode") ? obj.get("billboardMode").getAsInt() : 3;
        d.spinEnabled = obj.has("spinEnabled") && obj.get("spinEnabled").getAsBoolean();
        d.spinSpeedDegrees = obj.has("spinSpeedDegrees") ? obj.get("spinSpeedDegrees").getAsFloat() : 3.0f;
        d.spinAxis = obj.has("spinAxis") ? obj.get("spinAxis").getAsString() : "Y";
        d.spinTrackPlayer = !obj.has("spinTrackPlayer") || obj.get("spinTrackPlayer").getAsBoolean();

        d.hoverEnabled = obj.has("hoverEnabled") && obj.get("hoverEnabled").getAsBoolean();
        d.hoverAmplitude = obj.has("hoverAmplitude") ? obj.get("hoverAmplitude").getAsFloat() : 0.08f;
        d.hoverSpeedDegrees = obj.has("hoverSpeedDegrees") ? obj.get("hoverSpeedDegrees").getAsFloat() : 1.5f;

        return d;
    }

    // ── Legacy migration ──────────────────────────────────────────────────────

    /**
     * One-time import of the legacy {@code neoessentials/holograms.json} file (a plain
     * JSON array of {@link HologramData}) into the active DataStore, if it's still
     * empty and storage.autoMigrate is enabled.
     */
    private void migrateLegacyFilesIfNeeded() {
        if (store.hasAnyData(COLLECTION)) return;
        if (!com.zerog.neoessentials.config.ConfigManager.getInstance().isStorageAutoMigrateEnabled()) return;

        File file = new File(ResourceUtil.DATA_DIR, "holograms.json");
        if (!file.exists()) return;

        int migrated = 0;
        try (FileReader reader = new FileReader(file)) {
            Type listType = new TypeToken<List<HologramData>>() {}.getType();
            List<HologramData> list = GSON.fromJson(reader, listType);
            if (list != null) {
                for (HologramData d : list) {
                    if (d.id == null || d.id.isEmpty()) continue;
                    String id = d.id.toLowerCase(Locale.ROOT);
                    store.put(COLLECTION, id, toJson(d));
                    migrated++;
                }
            }
        } catch (IOException e) {
            LOGGER.error("[Hologram] Failed to migrate legacy holograms.json: {}", e.getMessage());
        }

        if (migrated > 0) {
            LOGGER.info("HologramManager: migrated {} record(s) from legacy holograms.json into the '{}' storage backend.",
                migrated, com.zerog.neoessentials.storage.StorageManager.getInstance().getActiveType());
        }
    }
}
