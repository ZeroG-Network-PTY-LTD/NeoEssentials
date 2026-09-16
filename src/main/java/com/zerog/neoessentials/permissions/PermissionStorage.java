package com.zerog.neoessentials.permissions;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zerog.neoessentials.storage.DataStore;
import com.zerog.neoessentials.storage.StorageManager;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Persists permission groups and users through the pluggable {@link DataStore}
 * (JSON/YAML/SQLite/MySQL — see {@link StorageManager}), instead of the old bespoke
 * {@code permissions.json} / {@code permissions/playerdata.json} files.
 *
 * <p>Unlike most managers, this isn't append-only per-record CRUD: {@link #save} is called
 * with the FULL current state of the {@link PermissionManager} and must make the DataStore
 * collections match exactly — including deleting any group/user record that no longer exists
 * in-memory (e.g. a deleted group or a user reset to defaults), since {@link DataStore} has no
 * "replace whole collection" primitive.
 *
 * <p>{@code permissions.json} is a live source of truth, not a one-time seed: every group it
 * defines is re-applied on every {@link #load} — at server start and on every
 * {@code /permissions reload}/{@code /neoe reload} — via {@link #syncSeedGroupsFromConfig},
 * overwriting that group's permissions/inherits/prefix/suffix/priority to match the file.
 * Groups not mentioned in the file, and all user data, are left alone.
 */
public class PermissionStorage {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionStorage.class);
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();
    private static final Path FILE_PATH = com.zerog.neoessentials.util.ResourceUtil.getConfigPath("permissions.json");
    private static final Path PLAYERDATA_PATH = com.zerog.neoessentials.util.ResourceUtil.getConfigPath("permissions/playerdata.json");

    private static final String GROUP_COLLECTION = "permission_groups";
    private static final String USER_COLLECTION = "permission_users";
    private static final String META_COLLECTION = "permission_meta";

    private static DataStore store() {
        return StorageManager.getInstance().getStore();
    }

    public static void save(PermissionManager manager) throws IOException {
        // If using external permissions, do not persist internal permission data at all.
        if (com.zerog.neoessentials.permissions.PermissionSystem.isUsingExternal()) {
            return;
        }
        DataStore store = store();

        // Meta (default group)
        JsonObject meta = new JsonObject();
        meta.addProperty("defaultGroup", manager.getDefaultGroup());
        store.put(META_COLLECTION, "global", meta);

        // Groups — write current groups, then delete any stored group no longer present.
        Set<String> currentGroupNames = new HashSet<>();
        for (PermissionGroup group : manager.getGroups()) {
            currentGroupNames.add(group.getName());
            store.put(GROUP_COLLECTION, group.getName(), groupToJson(group));
        }
        for (String existingId : store.getAll(GROUP_COLLECTION).keySet()) {
            if (!currentGroupNames.contains(existingId)) {
                store.delete(GROUP_COLLECTION, existingId);
            }
        }

        // Users — same replace-by-diff approach.
        Set<String> currentUserIds = new HashSet<>();
        for (PermissionUser user : manager.getUsers()) {
            String id = user.getUuid().toString();
            currentUserIds.add(id);
            store.put(USER_COLLECTION, id, userToJson(user));
        }
        for (String existingId : store.getAll(USER_COLLECTION).keySet()) {
            if (!currentUserIds.contains(existingId)) {
                store.delete(USER_COLLECTION, existingId);
            }
        }
        NeoLog.debug(LOGGER, LogCategory.PERMISSIONS, "Saved permission state: {} group(s), {} user(s)",
                currentGroupNames.size(), currentUserIds.size());
    }

    public static void load(PermissionManager manager) throws IOException {
        // If using external permissions, do not load internal permission data.
        if (com.zerog.neoessentials.permissions.PermissionSystem.isUsingExternal()) {
            return;
        }
        DataStore store = store();
        migrateLegacyFilesIfNeeded(store);

        JsonObject meta = store.get(META_COLLECTION, "global");
        if (meta != null && meta.has("defaultGroup") && !meta.get("defaultGroup").isJsonNull()) {
            manager.setDefaultGroup(meta.get("defaultGroup").getAsString());
        }

        for (JsonObject g : store.getAll(GROUP_COLLECTION).values()) {
            manager.addGroup(groupFromJson(g));
        }

        // Re-apply permissions.json's groups on top of whatever the store had — config always
        // wins for any group defined there (permissions/inherits/prefix/suffix/priority reset
        // to exactly what's in the file). Runs on every load, not just a server's first boot,
        // so editing permissions.json and running /neoe reload (or restarting) actually takes
        // effect. Groups NOT mentioned in the file are left completely alone.
        syncSeedGroupsFromConfig(manager);

        for (JsonObject u : store.getAll(USER_COLLECTION).values()) {
            manager.addUser(userFromJson(u));
        }
        NeoLog.debug(LOGGER, LogCategory.PERMISSIONS, "Loaded permission state: {} group(s), {} user(s)",
                manager.getGroups().size(), manager.getUsers().size());

        // Persist the merged result so the store (and any dashboard/SQL view of it) reflects
        // what's actually active, not just what was there before the sync.
        try {
            save(manager);
        } catch (IOException e) {
            NeoLog.error(LOGGER, LogCategory.PERMISSIONS, "Failed to persist permission state after seed-config sync: {}", e.getMessage(), e);
        }
    }

    /**
     * Re-applies every group defined in {@code permissions.json} onto {@code manager}'s
     * in-memory group table, overwriting that group's permissions/inherits/prefix/suffix/
     * priority (and the overall default group) to match the file exactly. Groups not
     * mentioned in the file, and all user data, are left untouched.
     */
    private static void syncSeedGroupsFromConfig(PermissionManager manager) {
        if (!Files.exists(FILE_PATH)) return;
        try (Reader reader = Files.newBufferedReader(FILE_PATH)) {
            com.google.gson.stream.JsonReader jsonReader = new com.google.gson.stream.JsonReader(reader);
            jsonReader.setLenient(true);
            JsonObject root = JsonParser.parseReader(jsonReader).getAsJsonObject();

            if (root.has("defaultGroup") && !root.get("defaultGroup").isJsonNull()) {
                manager.setDefaultGroup(root.get("defaultGroup").getAsString());
            }
            if (root.has("groups")) {
                int synced = 0;
                for (JsonElement ge : root.getAsJsonArray("groups")) {
                    manager.addGroup(groupFromJson(ge.getAsJsonObject()));
                    synced++;
                }
                NeoLog.debug(LOGGER, LogCategory.PERMISSIONS, "Synced {} group(s) from permissions.json seed config", synced);
            }
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.PERMISSIONS, "Failed to sync groups from permissions.json seed config: {}", e.getMessage(), e);
        }
    }

    // ── JSON conversion ─────────────────────────────────────────────────────────

    private static JsonObject groupToJson(PermissionGroup group) {
        JsonObject g = new JsonObject();
        g.addProperty("name", group.getName());
        g.addProperty("prefix", group.getPrefix());
        g.addProperty("suffix", group.getSuffix());
        g.addProperty("priority", group.getPriority());

        JsonArray perms = new JsonArray();
        group.getPermissions().forEach(perms::add);
        g.add("permissions", perms);

        JsonArray inherits = new JsonArray();
        group.getInherits().forEach(inherits::add);
        g.add("inherits", inherits);

        long now = System.currentTimeMillis();
        JsonObject tempPerms = new JsonObject();
        group.getTempPermissions().forEach((node, expiry) -> {
            if (expiry > now) tempPerms.addProperty(node, expiry);
        });
        g.add("tempPermissions", tempPerms);

        JsonObject ctxPerms = new JsonObject();
        group.getContextualPermissions().forEach((ctxKey, nodeMap) -> {
            JsonObject nodeObj = new JsonObject();
            nodeMap.forEach(nodeObj::addProperty);
            ctxPerms.add(ctxKey, nodeObj);
        });
        g.add("contextualPermissions", ctxPerms);

        JsonObject conditions = new JsonObject();
        group.getConditions().forEach(conditions::addProperty);
        g.add("conditions", conditions);

        JsonObject meta = new JsonObject();
        group.getMetaMap().forEach(meta::addProperty);
        g.add("meta", meta);

        return g;
    }

    private static PermissionGroup groupFromJson(JsonObject g) {
        PermissionGroup group = new PermissionGroup(g.get("name").getAsString());
        if (g.has("prefix") && !g.get("prefix").isJsonNull()) group.setPrefix(g.get("prefix").getAsString());
        if (g.has("suffix") && !g.get("suffix").isJsonNull()) group.setSuffix(g.get("suffix").getAsString());
        if (g.has("priority") && !g.get("priority").isJsonNull()) group.setPriority(g.get("priority").getAsInt());

        if (g.has("permissions")) {
            for (JsonElement p : g.getAsJsonArray("permissions")) {
                if (!p.isJsonNull()) group.addPermission(p.getAsString());
            }
        }
        if (g.has("inherits")) {
            for (JsonElement inh : g.getAsJsonArray("inherits")) {
                if (!inh.isJsonNull()) group.addInheritance(inh.getAsString());
            }
        }
        if (g.has("tempPermissions") && g.get("tempPermissions").isJsonObject()) {
            long now = System.currentTimeMillis();
            for (Map.Entry<String, JsonElement> entry : g.getAsJsonObject("tempPermissions").entrySet()) {
                long expiry = entry.getValue().getAsLong();
                if (expiry > now) group.addTempPermission(entry.getKey(), expiry);
            }
        }
        if (g.has("contextualPermissions") && g.get("contextualPermissions").isJsonObject()) {
            for (Map.Entry<String, JsonElement> ctxEntry : g.getAsJsonObject("contextualPermissions").entrySet()) {
                if (ctxEntry.getValue().isJsonObject()) {
                    for (Map.Entry<String, JsonElement> ne : ctxEntry.getValue().getAsJsonObject().entrySet()) {
                        group.addContextPermission(ctxEntry.getKey(), ne.getKey(), ne.getValue().getAsBoolean());
                    }
                }
            }
        }
        if (g.has("conditions") && g.get("conditions").isJsonObject()) {
            for (Map.Entry<String, JsonElement> ce : g.getAsJsonObject("conditions").entrySet()) {
                if (!ce.getValue().isJsonNull()) group.setCondition(ce.getKey(), ce.getValue().getAsString());
            }
        }
        if (g.has("meta") && g.get("meta").isJsonObject()) {
            for (Map.Entry<String, JsonElement> me : g.getAsJsonObject("meta").entrySet()) {
                if (!me.getValue().isJsonNull()) group.setMeta(me.getKey(), me.getValue().getAsString());
            }
        }
        return group;
    }

    private static JsonObject userToJson(PermissionUser user) {
        JsonObject u = new JsonObject();
        u.addProperty("uuid", user.getUuid().toString());
        u.addProperty("group", user.getGroup());

        JsonArray perms = new JsonArray();
        user.getPermissions().forEach(perms::add);
        u.add("permissions", perms);

        if (!user.getPrefix().isEmpty()) u.addProperty("prefix", user.getPrefix());
        if (!user.getSuffix().isEmpty()) u.addProperty("suffix", user.getSuffix());

        long now = System.currentTimeMillis();
        JsonObject tempPerms = new JsonObject();
        user.getTempPermissions().forEach((node, expiry) -> {
            if (expiry > now) tempPerms.addProperty(node, expiry);
        });
        u.add("tempPermissions", tempPerms);

        JsonObject ctxPerms = new JsonObject();
        user.getContextualPermissions().forEach((ctxKey, nodeMap) -> {
            JsonObject nodeObj = new JsonObject();
            nodeMap.forEach(nodeObj::addProperty);
            ctxPerms.add(ctxKey, nodeObj);
        });
        u.add("contextualPermissions", ctxPerms);

        JsonObject conditions = new JsonObject();
        user.getConditions().forEach(conditions::addProperty);
        u.add("conditions", conditions);

        return u;
    }

    private static PermissionUser userFromJson(JsonObject u) {
        PermissionUser user = new PermissionUser(
            UUID.fromString(u.get("uuid").getAsString()),
            u.get("group").getAsString());

        if (u.has("permissions")) {
            for (JsonElement p : u.getAsJsonArray("permissions")) {
                if (!p.isJsonNull()) user.addPermission(p.getAsString());
            }
        }
        if (u.has("prefix") && !u.get("prefix").isJsonNull()) user.setPrefix(u.get("prefix").getAsString());
        if (u.has("suffix") && !u.get("suffix").isJsonNull()) user.setSuffix(u.get("suffix").getAsString());

        if (u.has("tempPermissions") && u.get("tempPermissions").isJsonObject()) {
            long now = System.currentTimeMillis();
            for (Map.Entry<String, JsonElement> entry : u.getAsJsonObject("tempPermissions").entrySet()) {
                long expiry = entry.getValue().getAsLong();
                if (expiry > now) user.addTempPermission(entry.getKey(), expiry);
            }
        }
        if (u.has("contextualPermissions") && u.get("contextualPermissions").isJsonObject()) {
            for (Map.Entry<String, JsonElement> ctxEntry : u.getAsJsonObject("contextualPermissions").entrySet()) {
                if (ctxEntry.getValue().isJsonObject()) {
                    for (Map.Entry<String, JsonElement> ne : ctxEntry.getValue().getAsJsonObject().entrySet()) {
                        user.addContextPermission(ctxEntry.getKey(), ne.getKey(), ne.getValue().getAsBoolean());
                    }
                }
            }
        }
        if (u.has("conditions") && u.get("conditions").isJsonObject()) {
            for (Map.Entry<String, JsonElement> ce : u.getAsJsonObject("conditions").entrySet()) {
                if (!ce.getValue().isJsonNull()) user.setCondition(ce.getKey(), ce.getValue().getAsString());
            }
        }
        return user;
    }

    /**
     * One-time import of a possible embedded {@code users} array from very old installs
     * (permissions.json used to allow embedding users directly) and
     * {@code permissions/playerdata.json} (users) into the active DataStore, if the user
     * collection is still empty and {@code storage.autoMigrate} is enabled. Old files are
     * left in place, untouched.
     *
     * <p>Group/defaultGroup import is NOT handled here any more — see
     * {@link #syncSeedGroupsFromConfig}, which re-applies permissions.json's groups on
     * every load (not just once), so editing that file and reloading actually takes effect.</p>
     */
    private static void migrateLegacyFilesIfNeeded(DataStore store) {
        if (store.hasAnyData(USER_COLLECTION)) {
            return;
        }
        if (!com.zerog.neoessentials.config.ConfigManager.getInstance().isStorageAutoMigrateEnabled()) return;

        int migratedUsers = 0;

        try {
            if (Files.exists(FILE_PATH)) {
                try (Reader reader = Files.newBufferedReader(FILE_PATH)) {
                    JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                    // Very old installs embedded users directly in permissions.json.
                    if (root.has("users")) {
                        for (JsonElement ue : root.getAsJsonArray("users")) {
                            JsonObject u = ue.getAsJsonObject().deepCopy();
                            store.put(USER_COLLECTION, u.get("uuid").getAsString(), u);
                            migratedUsers++;
                        }
                    }
                }
            }
            if (Files.exists(PLAYERDATA_PATH)) {
                try (Reader reader = Files.newBufferedReader(PLAYERDATA_PATH)) {
                    JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                    if (root.has("users")) {
                        for (JsonElement ue : root.getAsJsonArray("users")) {
                            JsonObject u = ue.getAsJsonObject().deepCopy();
                            store.put(USER_COLLECTION, u.get("uuid").getAsString(), u);
                            migratedUsers++;
                        }
                    }
                }
            }
        } catch (IOException e) {
            NeoLog.error(LOGGER, LogCategory.PERMISSIONS, "Failed to migrate legacy permission files: {}", e.getMessage(), e);
        }

        if (migratedUsers > 0) {
            NeoLog.info(LOGGER, LogCategory.PERMISSIONS, "PermissionStorage: migrated {} user(s) from legacy files into the '{}' storage backend.",
                migratedUsers, StorageManager.getInstance().getActiveType());
        }
    }

    // --- PATCH: Add method to check permission for a user (strict, no OP fallback) ---
    // Strict permission check using PermissionManager logic (no OP fallback)
    public static boolean hasPermission(PermissionManager manager, UUID uuid, String permission) {
        // Use the PermissionManager's hasPermission logic (which includes user, group, inheritance, wildcards)
        return manager.hasPermission(uuid, permission);
    }
}
