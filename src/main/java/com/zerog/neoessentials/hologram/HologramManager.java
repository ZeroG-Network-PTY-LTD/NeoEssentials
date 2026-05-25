package com.zerog.neoessentials.hologram;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.zerog.neoessentials.util.ResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton manager for all {@link HologramData} entries.
 * Persists to {@code neoessentials/holograms.json}.
 */
public class HologramManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(HologramManager.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

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

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    public void initialize() {
        try {
            // Clear before loading so that holograms deleted from the JSON file
            // do not survive a /hologram reload (previously the map was only appended to).
            holograms.clear();
            load();
            LOGGER.info("[Hologram] Loaded {} hologram(s).", holograms.size());
        } catch (Exception e) {
            LOGGER.error("[Hologram] Failed to load holograms.json — starting fresh.", e);
        }
    }

    public void shutdown() {
        try {
            save();
            LOGGER.info("[Hologram] holograms.json saved on shutdown.");
        } catch (Exception e) {
            LOGGER.error("[Hologram] Failed to save holograms.json on shutdown.", e);
        }
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    /** Create or replace a hologram. */
    public void registerHologram(HologramData data) {
        holograms.put(data.id.toLowerCase(Locale.ROOT), data);
        trySave();
    }

    /** Remove a hologram by id. Returns removed data or null. */
    public HologramData removeHologram(String id) {
        HologramData removed = holograms.remove(id.toLowerCase(Locale.ROOT));
        if (removed != null) trySave();
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

    private void load() throws IOException {
        Path path = ResourceUtil.getDataPath("holograms.json");
        if (!Files.exists(path)) return;
        try (Reader r = Files.newBufferedReader(path)) {
            Type listType = new TypeToken<List<HologramData>>() {}.getType();
            List<HologramData> list = GSON.fromJson(r, listType);
            if (list != null) {
                for (HologramData d : list) {
                    // Init transient / collection fields that Gson leaves null when absent from JSON
                    if (d.entityUUIDs == null) d.entityUUIDs = new ArrayList<>();
                    if (d.lines == null)       d.lines       = new ArrayList<>();
                    for (HologramLine line : d.lines) {
                        if (line.frames == null) line.frames = new ArrayList<>();
                    }
                    holograms.put(d.id.toLowerCase(Locale.ROOT), d);
                }
            }
        }
    }

    private void save() throws IOException {
        Path path = ResourceUtil.getDataPath("holograms.json");
        Path tmp  = ResourceUtil.getDataPath("holograms.json.tmp");
        Files.createDirectories(path.getParent());
        try (Writer w = Files.newBufferedWriter(tmp)) {
            GSON.toJson(new ArrayList<>(holograms.values()), w);
        }
        Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
    }

    private void trySave() {
        try {
            save();
        } catch (Exception e) {
            LOGGER.error("[Hologram] Failed to save holograms.json", e);
        }
    }
}

