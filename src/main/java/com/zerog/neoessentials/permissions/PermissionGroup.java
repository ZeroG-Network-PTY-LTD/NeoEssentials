package com.zerog.neoessentials.permissions;

import java.util.HashSet;
import java.util.Set;

public class PermissionGroup {
    private final String name;
    private final Set<String> permissions;
    private final Set<String> inherits;
    private String prefix = "";
    private String suffix = "";

    public PermissionGroup(String name) {
        this.name = name;
        this.permissions = new HashSet<>();
        this.inherits = new HashSet<>();
    }

    public String getName() {
        return name;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public Set<String> getInherits() {
        return inherits;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public void addPermission(String permission) {
        permissions.add(permission);
    }

    public void removePermission(String permission) {
        permissions.remove(permission);
    }

    public void addInheritance(String groupName) {
        inherits.add(groupName);
    }

    public void removeInheritance(String groupName) {
        inherits.remove(groupName);
    }
}