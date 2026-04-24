package com.zerog.neoessentials.permissions;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class PermissionStorage {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();
    private static final Path FILE_PATH = com.zerog.neoessentials.util.ResourceUtil.getConfigPath("permissions.json");
    private static final Path PLAYERDATA_PATH = com.zerog.neoessentials.util.ResourceUtil.getConfigPath("permissions/playerdata.json");

    public static void save(PermissionManager manager) throws IOException {
        // If using external permissions, do not save or backup internal permissions.json
        if (com.zerog.neoessentials.permissions.PermissionSystem.isUsingExternal()) {
            return;
        }
        // Save groups to permissions.json (atomic operation)
        Map<String, Object> groupData = new HashMap<>();
        groupData.put("defaultGroup", manager.getDefaultGroup());
        List<Object> groups = new ArrayList<>();
        for (PermissionGroup group : manager.getGroups()) {
            Map<String, Object> g = new HashMap<>();
            g.put("name", group.getName());
            g.put("prefix", group.getPrefix());
            g.put("suffix", group.getSuffix());
            g.put("priority", group.getPriority());
            g.put("permissions", group.getPermissions());
            g.put("inherits", group.getInherits());
            // Persist only unexpired group temp permissions
            Map<String, Long> activeTemps = new HashMap<>();
            long nowG = System.currentTimeMillis();
            group.getTempPermissions().forEach((node, expiry) -> {
                if (expiry > nowG) activeTemps.put(node, expiry);
            });
            if (!activeTemps.isEmpty()) g.put("tempPermissions", activeTemps);
            // Contextual permissions
            Map<String, Map<String, Boolean>> ctxPerms = group.getContextualPermissions();
            if (!ctxPerms.isEmpty()) g.put("contextualPermissions", ctxPerms);
            // Per-node conditions
            Map<String, String> conditions = group.getConditions();
            if (!conditions.isEmpty()) g.put("conditions", conditions);
            groups.add(g);
        }
        groupData.put("groups", groups);
        Files.createDirectories(FILE_PATH.getParent());
        
        // Write to temporary file first, then atomic move
        Path tempFile = FILE_PATH.resolveSibling(FILE_PATH.getFileName() + ".tmp");
        try (Writer writer = Files.newBufferedWriter(tempFile)) {
            GSON.toJson(groupData, writer);
        }
        Files.move(tempFile, FILE_PATH, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);

        // Save users to playerdata.json (atomic operation)
        List<Object> users = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (PermissionUser user : manager.getUsers()) {
            Map<String, Object> u = new HashMap<>();
            u.put("uuid", user.getUuid().toString());
            u.put("group", user.getGroup());
            u.put("permissions", user.getPermissions());
            if (!user.getPrefix().isEmpty()) u.put("prefix", user.getPrefix());
            if (!user.getSuffix().isEmpty()) u.put("suffix", user.getSuffix());
            // Persist only unexpired temp permissions
            Map<String, Long> activeTemps = new HashMap<>();
            user.getTempPermissions().forEach((node, expiry) -> {
                if (expiry > now) activeTemps.put(node, expiry);
            });
            if (!activeTemps.isEmpty()) u.put("tempPermissions", activeTemps);
            // Contextual permissions
            Map<String, Map<String, Boolean>> ctxPerms = user.getContextualPermissions();
            if (!ctxPerms.isEmpty()) u.put("contextualPermissions", ctxPerms);
            // Per-node conditions
            Map<String, String> conds = user.getConditions();
            if (!conds.isEmpty()) u.put("conditions", conds);
            users.add(u);
        }
        Map<String, Object> userData = new HashMap<>();
        userData.put("users", users);
        Files.createDirectories(PLAYERDATA_PATH.getParent());
        
        // Write to temporary file first, then atomic move
        Path tempUserFile = PLAYERDATA_PATH.resolveSibling(PLAYERDATA_PATH.getFileName() + ".tmp");
        try (Writer writer = Files.newBufferedWriter(tempUserFile)) {
            GSON.toJson(userData, writer);
        }
        Files.move(tempUserFile, PLAYERDATA_PATH, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    public static void load(PermissionManager manager) throws IOException {
        // If using external permissions, do not load internal permissions.json
        if (com.zerog.neoessentials.permissions.PermissionSystem.isUsingExternal()) {
            return;
        }
        // Load groups from permissions.json
        if (Files.exists(FILE_PATH)) {
            try (Reader reader = Files.newBufferedReader(FILE_PATH)) {
                Map<?, ?> groupData = GSON.fromJson(reader, Map.class);
                if (groupData == null) return;
                Object defaultGroup = groupData.get("defaultGroup");
                if (defaultGroup != null) manager.setDefaultGroup(defaultGroup.toString());
                Object groups = groupData.get("groups");
                if (groups instanceof List<?>) {
                    for (Object o : (List<?>) groups) {
                        if (o instanceof Map<?, ?> g) {
                            PermissionGroup group = new PermissionGroup(g.get("name").toString());
                            group.setPrefix((String) g.get("prefix"));
                            group.setSuffix((String) g.get("suffix"));
                            // Priority (optional, defaults to 0)
                            Object priorityObj = g.get("priority");
                            if (priorityObj instanceof Number n) {
                                group.setPriority(n.intValue());
                            }
                            // Permissions
                            Object permsObj = g.get("permissions");
                            if (permsObj instanceof List<?>) {
                                for (Object perm : (List<?>) permsObj) {
                                    if (perm != null) group.addPermission(perm.toString());
                                }
                            }
                            // Inherits
                            Object inheritsObj = g.get("inherits");
                            if (inheritsObj instanceof List<?>) {
                                for (Object inh : (List<?>) inheritsObj) {
                                    if (inh != null) group.addInheritance(inh.toString());
                                }
                            }
                            // Temp permissions (skip expired)
                            Object tempPermsObj = g.get("tempPermissions");
                            if (tempPermsObj instanceof Map<?, ?> tempMap) {
                                long nowLoad = System.currentTimeMillis();
                                for (Map.Entry<?, ?> entry : tempMap.entrySet()) {
                                    if (entry.getKey() != null && entry.getValue() instanceof Number n) {
                                        long expiry = n.longValue();
                                        if (expiry > nowLoad) {
                                            group.addTempPermission(entry.getKey().toString(), expiry);
                                        }
                                    }
                                }
                            }
                            // Contextual permissions
                            Object ctxObj = g.get("contextualPermissions");
                            if (ctxObj instanceof Map<?, ?> ctxMap) {
                                for (Map.Entry<?, ?> ctxEntry : ctxMap.entrySet()) {
                                    if (ctxEntry.getValue() instanceof Map<?, ?> nodeMap) {
                                        String ctxKey = ctxEntry.getKey().toString();
                                        for (Map.Entry<?, ?> ne : nodeMap.entrySet()) {
                                            if (ne.getKey() != null && ne.getValue() instanceof Boolean b) {
                                                group.addContextPermission(ctxKey, ne.getKey().toString(), b);
                                            }
                                        }
                                    }
                                }
                            }
                            // Per-node conditions
                            Object condObj = g.get("conditions");
                            if (condObj instanceof Map<?, ?> condMap) {
                                for (Map.Entry<?, ?> ce : condMap.entrySet()) {
                                    if (ce.getKey() != null && ce.getValue() != null) {
                                        group.setCondition(ce.getKey().toString(), ce.getValue().toString());
                                    }
                                }
                            }
                            manager.addGroup(group);
                        }
                    }
                }
            }
        }

        // Load users from playerdata.json
        if (Files.exists(PLAYERDATA_PATH)) {
            try (Reader reader = Files.newBufferedReader(PLAYERDATA_PATH)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                JsonArray users = root.getAsJsonArray("users");
                for (JsonElement ue : users) {
                    JsonObject u = ue.getAsJsonObject();
                    PermissionUser user = new PermissionUser(
                        UUID.fromString(u.get("uuid").getAsString()),
                        u.get("group").getAsString());
                    for (JsonElement p : u.getAsJsonArray("permissions")) {
                        user.addPermission(p.getAsString());
                    }
                    if (u.has("prefix") && !u.get("prefix").isJsonNull())
                        user.setPrefix(u.get("prefix").getAsString());
                    if (u.has("suffix") && !u.get("suffix").isJsonNull())
                        user.setSuffix(u.get("suffix").getAsString());
                    // Load temp permissions (skip expired)
                    if (u.has("tempPermissions") && u.get("tempPermissions").isJsonObject()) {
                        long nowLoad = System.currentTimeMillis();
                        for (Map.Entry<String, com.google.gson.JsonElement> entry :
                                u.getAsJsonObject("tempPermissions").entrySet()) {
                            long expiry = entry.getValue().getAsLong();
                            if (expiry > nowLoad) {
                                user.addTempPermission(entry.getKey(), expiry);
                            }
                        }
                    }
                    // Load contextual permissions
                    if (u.has("contextualPermissions") && u.get("contextualPermissions").isJsonObject()) {
                        for (Map.Entry<String, com.google.gson.JsonElement> ctxEntry :
                                u.getAsJsonObject("contextualPermissions").entrySet()) {
                            if (ctxEntry.getValue().isJsonObject()) {
                                for (Map.Entry<String, com.google.gson.JsonElement> ne :
                                        ctxEntry.getValue().getAsJsonObject().entrySet()) {
                                    user.addContextPermission(ctxEntry.getKey(), ne.getKey(),
                                        ne.getValue().getAsBoolean());
                                }
                            }
                        }
                    }
                    // Load per-node conditions
                    if (u.has("conditions") && u.get("conditions").isJsonObject()) {
                        for (Map.Entry<String, com.google.gson.JsonElement> ce :
                                u.getAsJsonObject("conditions").entrySet()) {
                            user.setCondition(ce.getKey(), ce.getValue().getAsString());
                        }
                    }
                    manager.addUser(user);
                }
            }
        } else {
            // Migration: If playerdata.json does not exist but users are in permissions.json, migrate them
            if (Files.exists(FILE_PATH)) {
                try (Reader reader = Files.newBufferedReader(FILE_PATH)) {
                    JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                    if (root.has("users")) {
                        JsonArray users = root.getAsJsonArray("users");
                        List<Object> migratedUsers = new ArrayList<>();
                        for (JsonElement ue : users) {
                            JsonObject u = ue.getAsJsonObject();
                            PermissionUser user = new PermissionUser(
                                UUID.fromString(u.get("uuid").getAsString()),
                                u.get("group").getAsString());
                            for (JsonElement p : u.getAsJsonArray("permissions")) {
                                user.addPermission(p.getAsString());
                            }
                            if (u.has("prefix") && !u.get("prefix").isJsonNull())
                                user.setPrefix(u.get("prefix").getAsString());
                            if (u.has("suffix") && !u.get("suffix").isJsonNull())
                                user.setSuffix(u.get("suffix").getAsString());
                            manager.addUser(user);
                            Map<String, Object> userMap = new HashMap<>();
                            userMap.put("uuid", user.getUuid().toString());
                            userMap.put("group", user.getGroup());
                            userMap.put("permissions", user.getPermissions());
                            if (!user.getPrefix().isEmpty()) userMap.put("prefix", user.getPrefix());
                            if (!user.getSuffix().isEmpty()) userMap.put("suffix", user.getSuffix());
                            migratedUsers.add(userMap);
                        }
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("users", migratedUsers);
                        Files.createDirectories(PLAYERDATA_PATH.getParent());
                        try (Writer writer = Files.newBufferedWriter(PLAYERDATA_PATH)) {
                            GSON.toJson(userData, writer);
                        }
                        // Remove users from permissions.json
                        JsonObject newRoot = root.deepCopy();
                        newRoot.remove("users");
                        try (Writer writer = Files.newBufferedWriter(FILE_PATH)) {
                            GSON.toJson(newRoot, writer);
                        }
                    }
                }
            }
        }
    }

    // --- PATCH: Add method to check permission for a user (strict, no OP fallback) ---
    // Strict permission check using PermissionManager logic (no OP fallback)
    public static boolean hasPermission(PermissionManager manager, UUID uuid, String permission) {
        // Use the PermissionManager's hasPermission logic (which includes user, group, inheritance, wildcards)
        return manager.hasPermission(uuid, permission);
    }
}