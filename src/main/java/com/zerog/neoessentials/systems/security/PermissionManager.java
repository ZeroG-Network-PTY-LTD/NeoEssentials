package com.zerog.neoessentials.systems.security;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Permission management system
 */
public class PermissionManager {
    private final Map<String, Set<String>> userPermissions = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> rolePermissions = new ConcurrentHashMap<>();
    private final Map<String, String> userRoles = new ConcurrentHashMap<>();
    
    public void grantPermission(String userId, String permission) {
        userPermissions.computeIfAbsent(userId, k -> new HashSet<>()).add(permission);
    }
    
    public void revokePermission(String userId, String permission) {
        Set<String> permissions = userPermissions.get(userId);
        if (permissions != null) {
            permissions.remove(permission);
        }
    }
    
    public boolean hasPermission(String userId, String permission) {
        // Check direct user permissions
        Set<String> userPerms = userPermissions.get(userId);
        if (userPerms != null && userPerms.contains(permission)) {
            return true;
        }
        
        // Check role-based permissions
        String userRole = userRoles.get(userId);
        if (userRole != null) {
            Set<String> rolePerms = rolePermissions.get(userRole);
            return rolePerms != null && rolePerms.contains(permission);
        }
        
        return false;
    }
    
    public void assignRole(String userId, String role) {
        userRoles.put(userId, role);
    }
    
    public void addRolePermission(String role, String permission) {
        rolePermissions.computeIfAbsent(role, k -> new HashSet<>()).add(permission);
    }
    
    public Set<String> getUserPermissions(String userId) {
        return new HashSet<>(userPermissions.getOrDefault(userId, new HashSet<>()));
    }
    
    /**
     * Initialize the permission manager
     */
    public void initialize() {
        // Set up default roles and permissions
        addRolePermission("admin", "essentials.*");
        addRolePermission("moderator", "essentials.kick");
        addRolePermission("moderator", "essentials.ban");
        addRolePermission("user", "essentials.home");
        addRolePermission("user", "essentials.spawn");
    }
}
