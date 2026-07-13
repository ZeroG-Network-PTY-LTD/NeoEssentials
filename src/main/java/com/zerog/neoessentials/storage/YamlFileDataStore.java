package com.zerog.neoessentials.storage;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Same shape as {@link JsonFileDataStore} (one file per collection, one top-level entry
 * per record id) but written as YAML instead of JSON, for admins who prefer editing data
 * files by hand. Converts between Gson's {@link JsonElement} tree and the plain
 * {@code Map}/{@code List}/primitive tree SnakeYAML expects — there's no YAML equivalent
 * of {@code JsonObject}, so every read/write round-trips through that conversion.
 */
public class YamlFileDataStore implements DataStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(YamlFileDataStore.class);

    private final File baseDir;
    private final Yaml yaml;
    private final Map<String, Map<String, JsonObject>> cache = new ConcurrentHashMap<>();

    public YamlFileDataStore(File baseDir) {
        this.baseDir = baseDir;
        if (!baseDir.exists() && !baseDir.mkdirs()) {
            LOGGER.error("Failed to create storage directory: {}", baseDir.getAbsolutePath());
        }
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        this.yaml = new Yaml(options);
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

    @SuppressWarnings("unchecked")
    private Map<String, JsonObject> readFromDisk(String collection) {
        Map<String, JsonObject> records = new ConcurrentHashMap<>();
        File file = fileFor(collection);
        if (!file.exists()) return records;

        try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
            Object loaded = yaml.load(reader);
            if (loaded instanceof Map<?, ?> root) {
                for (Map.Entry<?, ?> entry : root.entrySet()) {
                    JsonElement converted = toJson(entry.getValue());
                    if (converted.isJsonObject()) {
                        records.put(String.valueOf(entry.getKey()), converted.getAsJsonObject());
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to read collection '{}' from {}", collection, file.getAbsolutePath(), e);
        }
        return records;
    }

    private void save(String collection, Map<String, JsonObject> records) {
        File file = fileFor(collection);
        try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
            Map<String, Object> root = new LinkedHashMap<>();
            for (Map.Entry<String, JsonObject> entry : records.entrySet()) {
                root.put(entry.getKey(), toPlainObject(entry.getValue()));
            }
            yaml.dump(root, writer);
        } catch (IOException e) {
            LOGGER.error("Failed to save collection '{}' to {}", collection, file.getAbsolutePath(), e);
        }
    }

    private File fileFor(String collection) {
        return new File(baseDir, collection + ".yml");
    }

    // ── Gson JsonElement <-> plain Map/List/primitive conversion ──────────────

    private static Object toPlainObject(JsonElement element) {
        if (element == null || element.isJsonNull()) return null;
        if (element.isJsonObject()) {
            Map<String, Object> map = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                map.put(entry.getKey(), toPlainObject(entry.getValue()));
            }
            return map;
        }
        if (element.isJsonArray()) {
            List<Object> list = new ArrayList<>();
            for (JsonElement item : element.getAsJsonArray()) {
                list.add(toPlainObject(item));
            }
            return list;
        }
        JsonPrimitive primitive = element.getAsJsonPrimitive();
        if (primitive.isBoolean()) return primitive.getAsBoolean();
        if (primitive.isNumber()) {
            // Preserve whole numbers as longs rather than doubles where possible, so
            // round-tripping a timestamp doesn't turn "1783858045479" into "1.783858045479E12".
            double asDouble = primitive.getAsDouble();
            if (asDouble == Math.rint(asDouble) && !Double.isInfinite(asDouble)) {
                return primitive.getAsLong();
            }
            return asDouble;
        }
        return primitive.getAsString();
    }

    @SuppressWarnings("unchecked")
    private static JsonElement toJson(Object value) {
        if (value == null) return com.google.gson.JsonNull.INSTANCE;
        if (value instanceof Map<?, ?> map) {
            JsonObject obj = new JsonObject();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                obj.add(String.valueOf(entry.getKey()), toJson(entry.getValue()));
            }
            return obj;
        }
        if (value instanceof List<?> list) {
            JsonArray arr = new JsonArray();
            for (Object item : list) arr.add(toJson(item));
            return arr;
        }
        if (value instanceof Boolean b) return new JsonPrimitive(b);
        if (value instanceof Number n) return new JsonPrimitive(n);
        return new JsonPrimitive(String.valueOf(value));
    }
}
