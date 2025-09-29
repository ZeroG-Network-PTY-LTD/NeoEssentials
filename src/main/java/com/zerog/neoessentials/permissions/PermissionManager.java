package com.zerog.neoessentials.permissions;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PermissionManager {
    private final Map<String, PermissionGroup> groups = new ConcurrentHashMap<>();
    private final Map<UUID, PermissionUser> users = new ConcurrentHashMap<>();

    public PermissionManager() {
    }

    public void addGroup(PermissionGroup group) {
        groups.put(group.getName().toLowerCase(), group);
    }

    public PermissionGroup getGroup(String name) {
        return groups.get(name.toLowerCase());
    }

    public Collection<PermissionGroup> getGroups() {
        return groups.values();
    }

    public void addUser(PermissionUser user) {
        users.put(user.getUuid(), user);
    }

    public PermissionUser getUser(UUID uuid) {
        return users.get(uuid);
    }

    public Collection<PermissionUser> getUsers() {
        return users.values();
    }

    public boolean hasPermission(UUID uuid, String permission) {
        PermissionUser user = getUser(uuid);
        if (user == null) return false;
        // Check user permissions
        if (user.getPermissions().contains(permission)) return true;
        // Check group permissions (with inheritance)
        return hasGroupPermission(user.getGroup(), permission, new HashSet<>());
    }

    private boolean hasGroupPermission(String groupName, String permission, Set<String> visited) {
        if (groupName == null || visited.contains(groupName.toLowerCase())) return false;
        visited.add(groupName.toLowerCase());
        PermissionGroup group = getGroup(groupName);
        if (group == null) return false;
        if (group.getPermissions().contains(permission)) return true;
        for (String parent : group.getInherits()) {
            if (hasGroupPermission(parent, permission, visited)) return true;
        }
        return false;
    }
}
