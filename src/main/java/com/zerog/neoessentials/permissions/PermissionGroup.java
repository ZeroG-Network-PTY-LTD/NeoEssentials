package com.zerog.neoessentials.permissions;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a permission group with inheritance support
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class PermissionGroup {
    private final String name;
    private String prefix;
    private String suffix;
    private int priority;
    private String inheritance; // Parent group name
    private final Set<String> permissions = ConcurrentHashMap.newKeySet();
    private final Set<String> negatedPermissions = ConcurrentHashMap.newKeySet();
    
    public PermissionGroup(String name, String prefix, String suffix, int priority) {
        this.name = name;
        this.prefix = prefix;
        this.suffix = suffix;
        this.priority = priority;
    }
    
    /**
     * Add permission to this group
     */
    public void addPermission(String permission) {
        if (permission.startsWith("-")) {
            // Negated permission
            negatedPermissions.add(permission.substring(1));
        } else {
            permissions.add(permission);
        }
    }
    
    /**
     * Remove permission from this group
     */
    public void removePermission(String permission) {
        permissions.remove(permission);
        negatedPermissions.remove(permission);
    }
    
    /**
     * Check if this group has a specific permission (with inheritance)
     */
    public boolean hasPermission(String permission, Map<String, PermissionGroup> allGroups) {
        // Check negated permissions first
        if (isNegated(permission)) {
            return false;
        }
        
        // Check direct permissions
        if (hasDirectPermission(permission)) {
            return true;
        }
        
        // Check inheritance
        if (inheritance != null && allGroups.containsKey(inheritance)) {
            PermissionGroup parent = allGroups.get(inheritance);
            return parent.hasPermission(permission, allGroups);
        }
        
        return false;
    }
    
    /**
     * Check if permission is directly granted by this group
     */
    private boolean hasDirectPermission(String permission) {
        // Check exact match
        if (permissions.contains(permission)) {
            return true;
        }
        
        // Check wildcard permissions
        for (String perm : permissions) {
            if (perm.endsWith("*")) {
                String base = perm.substring(0, perm.length() - 1);
                if (permission.startsWith(base)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Check if permission is negated
     */
    private boolean isNegated(String permission) {
        // Check exact negation
        if (negatedPermissions.contains(permission)) {
            return true;
        }
        
        // Check wildcard negations
        for (String negated : negatedPermissions) {
            if (negated.endsWith("*")) {
                String base = negated.substring(0, negated.length() - 1);
                if (permission.startsWith(base)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Get all permissions for this group (including inherited)
     */
    public Set<String> getAllPermissions(Map<String, PermissionGroup> allGroups) {
        Set<String> allPermissions = new HashSet<>(permissions);
        
        // Add inherited permissions
        if (inheritance != null && allGroups.containsKey(inheritance)) {
            PermissionGroup parent = allGroups.get(inheritance);
            allPermissions.addAll(parent.getAllPermissions(allGroups));
        }
        
        // Remove negated permissions
        allPermissions.removeAll(negatedPermissions);
        
        return allPermissions;
    }
    
    /**
     * Get direct permissions only (not inherited)
     */
    public Set<String> getDirectPermissions() {
        return new HashSet<>(permissions);
    }
    
    /**
     * Get negated permissions
     */
    public Set<String> getNegatedPermissions() {
        return new HashSet<>(negatedPermissions);
    }
    
    // Getters and setters
    public String getName() {
        return name;
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
    
    public int getPriority() {
        return priority;
    }
    
    public void setPriority(int priority) {
        this.priority = priority;
    }
    
    public String getInheritance() {
        return inheritance;
    }
    
    public void setInheritance(String inheritance) {
        this.inheritance = inheritance;
    }
    
    @Override
    public String toString() {
        return "PermissionGroup{" +
                "name='" + name + '\'' +
                ", prefix='" + prefix + '\'' +
                ", suffix='" + suffix + '\'' +
                ", priority=" + priority +
                ", inheritance='" + inheritance + '\'' +
                ", permissions=" + permissions.size() +
                '}';
    }
}
