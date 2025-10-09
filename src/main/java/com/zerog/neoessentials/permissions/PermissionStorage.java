package com.zerog.neoessentials.permissions;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import com.google.gson.*;

public class PermissionStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE_PATH = com.zerog.neoessentials.util.ResourceUtil.getConfigPath("permissions.json");
    private static final Path PLAYERDATA_PATH = com.zerog.neoessentials.util.ResourceUtil.getConfigPath("permissions/playerdata.json");

    public static void save(PermissionManager manager) throws IOException {
        // Save groups to permissions.json (atomic operation)
        Map<String, Object> groupData = new HashMap<>();
        groupData.put("defaultGroup", manager.getDefaultGroup());
        List<Object> groups = new ArrayList<>();
        for (PermissionGroup group : manager.getGroups()) {
            Map<String, Object> g = new HashMap<>();
            g.put("name", group.getName());
            g.put("prefix", group.getPrefix());
            g.put("suffix", group.getSuffix());
            g.put("permissions", group.getPermissions());
            g.put("inherits", group.getInherits());
            groups.add(g);
        }
        groupData.put("groups", groups);
        Files.createDirectories(FILE_PATH.getParent());
        
        // Write to temporary file first, then atomic move
        Path tempFile = FILE_PATH.resolveSibling(FILE_PATH.getFileName() + ".tmp");
        try (Writer writer = Files.newBufferedWriter(tempFile)) {
            GSON.toJson(groupData, writer);
        }
        Files.move(tempFile, FILE_PATH, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        // Save users to playerdata.json (atomic operation)
        List<Object> users = new ArrayList<>();
        for (PermissionUser user : manager.getUsers()) {
            Map<String, Object> u = new HashMap<>();
            u.put("uuid", user.getUuid().toString());
            u.put("group", user.getGroup());
            u.put("permissions", user.getPermissions());
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
        // Load groups from permissions.json
        if (Files.exists(FILE_PATH)) {
            try (Reader reader = Files.newBufferedReader(FILE_PATH)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                
                // Load default group setting
                if (root.has("defaultGroup")) {
                    manager.setDefaultGroup(root.get("defaultGroup").getAsString());
                }
                
                JsonArray groups = root.getAsJsonArray("groups");
                for (JsonElement ge : groups) {
                    JsonObject g = ge.getAsJsonObject();
                    PermissionGroup group = new PermissionGroup(g.get("name").getAsString());
                    if (g.has("prefix")) group.setPrefix(g.get("prefix").getAsString());
                    if (g.has("suffix")) group.setSuffix(g.get("suffix").getAsString());
                    for (JsonElement p : g.getAsJsonArray("permissions")) {
                        group.addPermission(p.getAsString());
                    }
                    for (JsonElement inh : g.getAsJsonArray("inherits")) {
                        group.addInheritance(inh.getAsString());
                    }
                    manager.addGroup(group);
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
                    PermissionUser user = new PermissionUser(UUID.fromString(u.get("uuid").getAsString()), u.get("group").getAsString());
                    for (JsonElement p : u.getAsJsonArray("permissions")) {
                        user.addPermission(p.getAsString());
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
                            PermissionUser user = new PermissionUser(UUID.fromString(u.get("uuid").getAsString()), u.get("group").getAsString());
                            for (JsonElement p : u.getAsJsonArray("permissions")) {
                                user.addPermission(p.getAsString());
                            }
                            manager.addUser(user);
                            Map<String, Object> userMap = new HashMap<>();
                            userMap.put("uuid", user.getUuid().toString());
                            userMap.put("group", user.getGroup());
                            userMap.put("permissions", user.getPermissions());
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
}