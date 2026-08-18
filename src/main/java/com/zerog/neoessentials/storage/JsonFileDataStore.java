package com.zerog.neoessentials.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default backend — one JSON file per collection, at
 * {@code neoessentials/store/<collection>.json}, shaped as {@code {"id1": {...}, "id2": {...}}}.
 *
 * <p>This is a new, consistent format shared by every collection — it is <b>not</b> a
 * drop-in reader for each manager's old bespoke file shape (some were flat maps, some
 * were {@code {"bans": [...]}} arrays, etc.). Existing data is brought in via the
 * one-time migration in {@code StorageManager}, not by this class understanding old formats.
 */
public class JsonFileDataStore implements DataStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonFileDataStore.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final File baseDir;
    // In-memory cache per collection, so repeated get()/getAll() calls don't re-read
    // the file from disk every time — same pattern the existing managers already use.
    private final Map<String, Map<String, JsonObject>> cache = new ConcurrentHashMap<>();

    public JsonFileDataStore(File baseDir) {
        this.baseDir = baseDir;
        if (!baseDir.exists() && !baseDir.mkdirs()) {
            LOGGER.error("Failed to create storage directory: {}", baseDir.getAbsolutePath());
        }
    }

    @Override
    public void put(String collection, String id, JsonObject data) {
        Map<String, JsonObject> records = loadCollection(collection);
        records.put(id, data);
        save(collection, records);
    }

    @Override
    public JsonObject get(String collection, String id) {
        return loadCollection(collection).get(id);
    }

    @Override
    public boolean delete(String collection, String id) {
        Map<String, JsonObject> records = loadCollection(collection);
        boolean removed = records.remove(id) != null;
        if (removed) save(collection, records);
        return removed;
    }

    @Override
    public Map<String, JsonObject> getAll(String collection) {
        return new LinkedHashMap<>(loadCollection(collection));
    }

    @Override
    public boolean hasAnyData(String collection) {
        return !loadCollection(collection).isEmpty();
    }

    @Override
    public void close() {
        // Nothing to release — every write is already flushed to disk immediately.
    }

    private Map<String, JsonObject> loadCollection(String collection) {
        return cache.computeIfAbsent(collection, this::readFromDisk);
    }

    private Map<String, JsonObject> readFromDisk(String collection) {
        Map<String, JsonObject> records = new ConcurrentHashMap<>();
        File file = fileFor(collection);
        if (!file.exists()) return records;

        try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root != null) {
                for (String key : root.keySet()) {
                    records.put(key, root.getAsJsonObject(key));
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to read collection '{}' from {}", collection, file.getAbsolutePath(), e);
        } catch (com.google.gson.JsonParseException e) {
            // A truncated/corrupt file (e.g. the process died mid-save before the atomic
            // rename in save() below existed, or the file was hand-edited into invalid JSON)
            // throws here, not IOException. Rename it aside instead of silently treating the
            // collection as empty — returning empty would make the very next put() overwrite
            // it with just the one new record, permanently destroying whatever was still
            // intact in the rest of the file.
            File corrupted = new File(file.getParentFile(), file.getName() + ".corrupt-" + System.currentTimeMillis());
            LOGGER.error("Collection '{}' at {} is not valid JSON — treating as empty and preserving the " +
                "corrupted file at {} for manual recovery.", collection, file.getAbsolutePath(), corrupted.getName(), e);
            try {
                Files.copy(file.toPath(), corrupted.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException copyEx) {
                LOGGER.error("Could not preserve corrupted collection file {}: {}", file.getAbsolutePath(), copyEx.getMessage());
            }
        }
        return records;
    }

    private void save(String collection, Map<String, JsonObject> records) {
        File file = fileFor(collection);
        // Write to a temp file first, then atomically replace the target — writing directly
        // into the target truncates it immediately, so a crash mid-write (process kill, OOM,
        // power loss) between the truncation and the write completing left the collection
        // empty or half-written, and readFromDisk() had no way to tell that apart from a
        // genuinely empty collection.
        File tmp = new File(file.getParentFile(), file.getName() + ".tmp-" + System.currentTimeMillis());
        try {
            JsonObject root = new JsonObject();
            for (Map.Entry<String, JsonObject> entry : records.entrySet()) {
                root.add(entry.getKey(), entry.getValue());
            }
            try (var writer = Files.newBufferedWriter(tmp.toPath(), StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
            Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            LOGGER.error("Failed to save collection '{}' to {}", collection, file.getAbsolutePath(), e);
            try { Files.deleteIfExists(tmp.toPath()); } catch (IOException ignored) { /* best effort cleanup */ }
        }
    }

    private File fileFor(String collection) {
        return new File(baseDir, collection + ".json");
    }
}
