package com.zerog.neoessentials.permissions;

import java.util.UUID;

public class PermissionAPI {
    private static PermissionManager manager;

    public static void setManager(PermissionManager m) {
        manager = m;
    }

    public static boolean hasPermission(UUID uuid, String permission) {
        if (manager == null) return false;
        return manager.hasPermission(uuid, permission);
    }

    public static PermissionManager getManager() {
        return manager;
    }

    public static String getPrefix(UUID uuid) {
        if (manager == null) return "";
        PermissionUser user = manager.getUser(uuid);
        if (user == null) return "";
        PermissionGroup group = manager.getGroup(user.getGroup());
        return group != null ? group.getPrefix() : "";
    }

    public static String getSuffix(UUID uuid) {
        if (manager == null) return "";
        PermissionUser user = manager.getUser(uuid);
        if (user == null) return "";
        PermissionGroup group = manager.getGroup(user.getGroup());
        return group != null ? group.getSuffix() : "";
    }
}