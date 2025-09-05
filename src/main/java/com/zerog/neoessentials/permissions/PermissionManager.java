package com.zerog.neoessentials.permissions;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PermissionManager {
    private static final Map<String, Set<String>> rolePermissions = new ConcurrentHashMap<>();
    private static final Map<UUID, String> playerRoles = new ConcurrentHashMap<>();

    public static void registerRole(String role, Set<String> permissions) {
        rolePermissions.put(role, permissions);
    }

    public static void assignPlayerRole(UUID playerUUID, String role) {
        playerRoles.put(playerUUID, role);
    }

    public static boolean hasPermission(UUID playerUUID, String permission) {
        String role = playerRoles.get(playerUUID);
        if (role == null) return false;
        Set<String> perms = rolePermissions.get(role);
        if (perms == null) return false;
        // Support wildcard permissions
        if (perms.contains(permission)) return true;
        int idx = permission.lastIndexOf('.');
        while (idx > 0) {
            String wildcard = permission.substring(0, idx) + ".*";
            if (perms.contains(wildcard)) return true;
            idx = permission.lastIndexOf('.', idx - 1);
        }
        return false;
    }

    public static void loadFromConfig() {
        // Load roles and permissions from permissions.json
        try {
            java.nio.file.Path configPath = java.nio.file.Paths.get("config/permissions.json");
            String json = java.nio.file.Files.readString(configPath);
            com.google.gson.JsonObject root = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
            for (com.google.gson.JsonElement roleElem : root.getAsJsonArray("roles")) {
                com.google.gson.JsonObject roleObj = roleElem.getAsJsonObject();
                String name = roleObj.get("name").getAsString();
                Set<String> perms = new HashSet<>();
                for (com.google.gson.JsonElement permElem : roleObj.getAsJsonArray("permissions")) {
                    perms.add(permElem.getAsString());
                }
                registerRole(name, perms);
            }
        } catch (Exception e) {
            System.err.println("Failed to load permissions config: " + e.getMessage());
        }
    }

    public static String getPlayerRole(UUID playerUUID) {
        return playerRoles.get(playerUUID);
    }

    public static Set<String> getRolePermissions(String role) {
        return rolePermissions.getOrDefault(role, Collections.emptySet());
    }
}
