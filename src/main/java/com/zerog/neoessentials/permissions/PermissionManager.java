
package com.zerog.neoessentials.permissions;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PermissionManager {
    private final Map<String, PermissionGroup> groups = new ConcurrentHashMap<>();
    private final Map<UUID, PermissionUser> users = new ConcurrentHashMap<>();
    private String defaultGroup = "default";

    public PermissionManager() {
    }

    /**
     * Reloads all permissions and groups from disk using PermissionStorage.
     */
    public void reload() throws Exception {
        this.groups.clear();
        this.users.clear();
        PermissionStorage.load(this);
    }

    /**
     * Set the default group name to use as a fallback.
     */
    public void setDefaultGroup(String groupName) {
        this.defaultGroup = groupName.toLowerCase();
    }

    /**
     * Get the default group name.
     */
    public String getDefaultGroup() {
        return defaultGroup;
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
        permission = permission.toLowerCase();
        PermissionUser user = getUser(uuid);
        String groupName = (user != null && user.getGroup() != null) ? user.getGroup() : defaultGroup;
        // Check user negative permissions
        if (user != null && hasNegativePermission(user.getPermissions(), permission)) return false;
        // Check group negative permissions (with inheritance)
        if (hasGroupNegativePermission(groupName, permission, new HashSet<>())) return false;
        // Check user permissions (including wildcards)
        if (user != null && hasPermissionWithWildcards(user.getPermissions(), permission)) return true;
        // Check group permissions (with inheritance and wildcards)
        return hasGroupPermission(groupName, permission, new HashSet<>());
    }

    private boolean hasNegativePermission(Set<String> perms, String permission) {
        for (String perm : perms) {
            if (perm.equals("-" + permission)) return true;
            if (perm.startsWith("-")) {
                String neg = perm.substring(1);
                if (neg.endsWith(".*")) {
                    String prefix = neg.substring(0, neg.length() - 2);
                    if (permission.startsWith(prefix + ".")) return true;
                }
            }
        }
        return false;
    }

    private boolean hasGroupNegativePermission(String groupName, String permission, Set<String> visited) {
        if (groupName == null || visited.contains(groupName.toLowerCase())) return false;
        visited.add(groupName.toLowerCase());
        PermissionGroup group = getGroup(groupName);
        if (group == null) return false;
        if (hasNegativePermission(group.getPermissions(), permission)) return true;
        for (String parent : group.getInherits()) {
            if (hasGroupNegativePermission(parent, permission, visited)) return true;
        }
        return false;
    }

    private boolean hasPermissionWithWildcards(Set<String> perms, String permission) {
        for (String perm : perms) {
            if (perm.equals(permission)) return true;
            if (perm.endsWith(".*")) {
                String prefix = perm.substring(0, perm.length() - 2);
                if (permission.startsWith(prefix + ".")) return true;
            }
        }
        return false;
    }

    private boolean hasGroupPermission(String groupName, String permission, Set<String> visited) {
        if (groupName == null || visited.contains(groupName.toLowerCase())) return false;
        visited.add(groupName.toLowerCase());
        PermissionGroup group = getGroup(groupName);
        if (group == null) return false;
        if (hasPermissionWithWildcards(group.getPermissions(), permission)) return true;
        for (String parent : group.getInherits()) {
            if (hasGroupPermission(parent, permission, visited)) return true;
        }
        return false;
    }
}
