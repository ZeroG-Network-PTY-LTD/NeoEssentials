package com.zerog.neoessentials.permissions;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PermissionUser {
    private final UUID uuid;
    private String group;
    private final Set<String> permissions;
    private String prefix;
    private String suffix;

    public PermissionUser(UUID uuid, String group) {
        this.uuid = uuid;
        this.group = group;
        this.permissions = ConcurrentHashMap.newKeySet();
        this.prefix = "";
        this.suffix = "";
    }

    public UUID getUuid() { return uuid; }
    public String getGroup() { return group; }
    public void setGroup(String group) { this.group = group; }
    public Set<String> getPermissions() { return permissions; }
    public void addPermission(String permission) { permissions.add(permission.toLowerCase()); }
    public void removePermission(String permission) { permissions.remove(permission.toLowerCase()); }

    public String getPrefix() { return prefix != null ? prefix : ""; }
    public void setPrefix(String prefix) { this.prefix = prefix != null ? prefix : ""; }
    public String getSuffix() { return suffix != null ? suffix : ""; }
    public void setSuffix(String suffix) { this.suffix = suffix != null ? suffix : ""; }
}
