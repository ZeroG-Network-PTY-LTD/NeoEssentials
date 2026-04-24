package com.zerog.neoessentials.permissions;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages permission node aliases, allowing short/legacy names to be mapped to
 * their canonical NeoEssentials equivalents.
 *
 * <p>Configuration file: {@code config/neoessentials/permission_aliases.json}
 *
 * <p>Example content:
 * <pre>
 * {
 *   "essentials.fly"          : "neoessentials.fly",
 *   "essentials.warp"         : "neoessentials.teleport.warp",
 *   "efly"                    : "neoessentials.fly"
 * }
 * </pre>
 *
 * <p>Aliases are resolved <em>transparently</em> inside
 * {@link com.zerog.neoessentials.api.permissions.PermissionAPI#hasPermission} before
 * the permission check, so external mods and in-game commands that use legacy node
 * names will automatically receive the correct result.
 */
public class PermissionAliasManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionAliasManager.class);
    private static final Gson   GSON   = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Path   FILE   =
            com.zerog.neoessentials.util.ResourceUtil.getConfigPath("permission_aliases.json");

    /** alias (lower-case) → canonical node (lower-case). */
    private final Map<String, String> aliases = new ConcurrentHashMap<>();

    // ── Singleton ────────────────────────────────────────────────────────────

    private static volatile PermissionAliasManager INSTANCE;

    private PermissionAliasManager() {}

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
     * Load aliases from disk.  Missing file is silently ignored (no aliases active).
     */
    public void load() {
        aliases.clear();
        if (!Files.exists(FILE)) {
            LOGGER.debug("permission_aliases.json not found — no aliases active.");
            return;
        }
        try (Reader r = Files.newBufferedReader(FILE)) {
            JsonObject obj = JsonParser.parseReader(r).getAsJsonObject();
            for (Map.Entry<String, com.google.gson.JsonElement> entry : obj.entrySet()) {
                String alias     = entry.getKey().toLowerCase().trim();
                String canonical = entry.getValue().getAsString().toLowerCase().trim();
                if (!alias.isEmpty() && !canonical.isEmpty()) {
                    aliases.put(alias, canonical);
                }
            }
            LOGGER.info("Loaded {} permission alias(es) from permission_aliases.json", aliases.size());
        } catch (Exception e) {
            LOGGER.warn("Failed to load permission_aliases.json: {}", e.getMessage());
        }
    }

    /**
     * Persist the current alias map to disk atomically.
     */
    public void save() throws IOException {
        JsonObject obj = new JsonObject();
        aliases.entrySet().stream()
               .sorted(Map.Entry.comparingByKey())
               .forEach(e -> obj.addProperty(e.getKey(), e.getValue()));
        Files.createDirectories(FILE.getParent());
        Path tmp = FILE.resolveSibling(FILE.getFileName() + ".tmp");
        try (Writer w = Files.newBufferedWriter(tmp)) {
            GSON.toJson(obj, w);
        }
        Files.move(tmp, FILE,
            java.nio.file.StandardCopyOption.REPLACE_EXISTING,
            java.nio.file.StandardCopyOption.ATOMIC_MOVE);
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
        return aliases.getOrDefault(lower, lower);
    }

    // ── CRUD ─────────────────────────────────────────────────────────────────

    /**
     * Register a new alias.
     *
     * @param alias     short/legacy node name
     * @param canonical the canonical NeoEssentials node it maps to
     */
    public void addAlias(String alias, String canonical) {
        aliases.put(alias.toLowerCase().trim(), canonical.toLowerCase().trim());
    }

    /**
     * Remove an alias.
     *
     * @param alias the alias to remove
     * @return {@code true} if it existed
     */
    public boolean removeAlias(String alias) {
        return aliases.remove(alias.toLowerCase().trim()) != null;
    }

    /** Returns an unmodifiable snapshot of all current aliases (alias → canonical). */
    public Map<String, String> getAll() {
        return Collections.unmodifiableMap(aliases);
    }

    /** Returns {@code true} if the given alias key is registered. */
    public boolean hasAlias(String alias) {
        return aliases.containsKey(alias.toLowerCase().trim());
    }
}

