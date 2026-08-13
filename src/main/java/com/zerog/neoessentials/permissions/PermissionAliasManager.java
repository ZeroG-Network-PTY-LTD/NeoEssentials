package com.zerog.neoessentials.permissions;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages permission node aliases, allowing short/legacy names to be mapped to
 * their canonical NeoEssentials equivalents.
 *
 * <p>Persisted via the active {@link com.zerog.neoessentials.storage.DataStore} in the
 * {@code permission_aliases} collection — one record per alias, keyed by the alias
 * itself, with a {@code target} field holding the canonical node. Legacy installs are
 * migrated in from {@code config/neoessentials/permission_aliases.json} the first time
 * the collection is empty (see {@link #migrateLegacyFileIfNeeded()}).
 *
 * <p>Aliases are resolved <em>transparently</em> inside
 * {@link com.zerog.neoessentials.api.permissions.PermissionAPI#hasPermission} before
 * the permission check, so external mods and in-game commands that use legacy node
 * names will automatically receive the correct result.
 */
public class PermissionAliasManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionAliasManager.class);
    private static final String COLLECTION = "permission_aliases";

    private final com.zerog.neoessentials.storage.DataStore store;

    /** alias (lower-case) → canonical node (lower-case). */
    private final Map<String, String> aliases = new ConcurrentHashMap<>();

    // ── Singleton ────────────────────────────────────────────────────────────

    private static volatile PermissionAliasManager INSTANCE;

    private PermissionAliasManager() {
        this.store = com.zerog.neoessentials.storage.StorageManager.getInstance().getStore();
        migrateLegacyFileIfNeeded();
        load();
    }

    public static PermissionAliasManager getInstance() {
        if (INSTANCE == null) {
            synchronized (PermissionAliasManager.class) {
                if (INSTANCE == null) INSTANCE = new PermissionAliasManager();
            }
        }
        return INSTANCE;
    }

    // ── Load / Save ──────────────────────────────────────────────────────────

    /**
     * Load aliases from the active {@link com.zerog.neoessentials.storage.DataStore}.
     * An empty collection is silently ignored (no aliases active).
     */
    public void load() {
        aliases.clear();
        for (JsonObject obj : store.getAll(COLLECTION).values()) {
            if (!obj.has("target") || !obj.has("alias")) continue;
            String alias     = obj.get("alias").getAsString().toLowerCase().trim();
            String canonical = obj.get("target").getAsString().toLowerCase().trim();
            if (!alias.isEmpty() && !canonical.isEmpty()) {
                aliases.put(alias, canonical);
            }
        }
        NeoLog.info(LOGGER, LogCategory.PERMISSIONS, "Loaded {} permission alias(es) from storage.", aliases.size());
    }

    /**
     * Persist the current alias map to the active DataStore. One record per alias,
     * so this rewrites every alias — cheap since the alias table is expected to stay
     * small (short/legacy node mappings, not per-player data).
     */
    public void save() throws IOException {
        for (Map.Entry<String, String> e : aliases.entrySet()) {
            store.put(COLLECTION, e.getKey(), toJson(e.getKey(), e.getValue()));
        }
    }

    private JsonObject toJson(String alias, String canonical) {
        JsonObject obj = new JsonObject();
        obj.addProperty("alias", alias);
        obj.addProperty("target", canonical);
        return obj;
    }

    // ── Alias resolution ─────────────────────────────────────────────────────

    /**
     * Resolve a permission node through the alias table.
     * Returns the canonical node if an alias is found, otherwise returns {@code node} unchanged.
     *
     * @param node the incoming permission node (may be an alias or canonical)
     * @return the canonical permission node
     */
    public String resolve(String node) {
        if (node == null) return null;
        String lower = node.toLowerCase().trim();
        String canonical = aliases.get(lower);
        if (canonical != null) {
            NeoLog.debug(LOGGER, LogCategory.PERMISSIONS, "Resolved permission alias '{}' -> '{}'", lower, canonical);
            return canonical;
        }
        return lower;
    }

    // ── CRUD ─────────────────────────────────────────────────────────────────

    /**
     * Register a new alias.
     *
     * @param alias     short/legacy node name
     * @param canonical the canonical NeoEssentials node it maps to
     */
    public void addAlias(String alias, String canonical) {
        String key = alias.toLowerCase().trim();
        String target = canonical.toLowerCase().trim();
        aliases.put(key, target);
        store.put(COLLECTION, key, toJson(key, target));
    }

    /**
     * Remove an alias.
     *
     * @param alias the alias to remove
     * @return {@code true} if it existed
     */
    public boolean removeAlias(String alias) {
        String key = alias.toLowerCase().trim();
        boolean removed = aliases.remove(key) != null;
        if (removed) store.delete(COLLECTION, key);
        return removed;
    }

    /** Returns an unmodifiable snapshot of all current aliases (alias → canonical). */
    public Map<String, String> getAll() {
        return Collections.unmodifiableMap(aliases);
    }

    /** Returns {@code true} if the given alias key is registered. */
    public boolean hasAlias(String alias) {
        return aliases.containsKey(alias.toLowerCase().trim());
    }

    // ── Legacy migration ─────────────────────────────────────────────────────

    /**
     * One-time import of the legacy {@code permission_aliases.json} config file into the
     * active DataStore, if it's still empty and storage.autoMigrate is enabled.
     */
    private void migrateLegacyFileIfNeeded() {
        if (store.hasAnyData(COLLECTION)) return;
        if (!com.zerog.neoessentials.config.ConfigManager.getInstance().isStorageAutoMigrateEnabled()) return;

        File file = com.zerog.neoessentials.util.ResourceUtil.getConfigFile("permission_aliases.json");
        if (!file.exists()) return;

        int migrated = 0;
        try (FileReader reader = new FileReader(file)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            for (Map.Entry<String, com.google.gson.JsonElement> entry : root.entrySet()) {
                String alias     = entry.getKey().toLowerCase().trim();
                String canonical = entry.getValue().getAsString().toLowerCase().trim();
                if (alias.isEmpty() || canonical.isEmpty()) continue;
                store.put(COLLECTION, alias, toJson(alias, canonical));
                migrated++;
            }
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.PERMISSIONS, "Failed to migrate legacy permission_aliases.json: {}", e.getMessage(), e);
            return;
        }

        if (migrated > 0) {
            NeoLog.info(LOGGER, LogCategory.PERMISSIONS, "PermissionAliasManager: migrated {} permission alias(es) from legacy file into the '{}' storage backend.",
                migrated, com.zerog.neoessentials.storage.StorageManager.getInstance().getActiveType());
        }
    }
}
