package com.zerog.neoessentials.permissions;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents player-specific permissions and temporary permissions
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class PlayerPermissions {
    private final Set<String> permissions = ConcurrentHashMap.newKeySet();
    private final Set<String> negatedPermissions = ConcurrentHashMap.newKeySet();
    private final Map<String, Long> temporaryPermissions = new ConcurrentHashMap<>();
    
    /**
     * Add permission to player
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
     * Remove permission from player
     */
    public void removePermission(String permission) {
        permissions.remove(permission);
        negatedPermissions.remove(permission);
        temporaryPermissions.remove(permission);
    }
    
    /**
     * Add temporary permission with expiration time
     */
    public void addTemporaryPermission(String permission, long expirationTime) {
        temporaryPermissions.put(permission, expirationTime);
    }
    
    /**
     * Check if player has a specific permission
     * Returns null if no explicit permission set (defer to group)
     */
    public Boolean hasPermission(String permission) {
        long currentTime = System.currentTimeMillis();
        
        // Check negated permissions first
        if (isNegated(permission)) {
            return false;
        }
        
        // Check temporary permissions
        Long expiration = temporaryPermissions.get(permission);
        if (expiration != null) {
            if (currentTime < expiration) {
                return true; // Valid temporary permission
            } else {
                // Expired temporary permission
                temporaryPermissions.remove(permission);
            }
        }
        
        // Check permanent permissions
        if (hasDirectPermission(permission)) {
            return true;
        }
        
        // No explicit permission set - defer to group
        return null;
    }
    
    /**
     * Check if permission is directly granted
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
     * Get all permissions (permanent and temporary)
     */
    public Set<String> getPermissions() {
        Set<String> allPermissions = new HashSet<>(permissions);
        
        // Add valid temporary permissions
        long currentTime = System.currentTimeMillis();
        for (Map.Entry<String, Long> entry : temporaryPermissions.entrySet()) {
            if (currentTime < entry.getValue()) {
                allPermissions.add(entry.getKey());
            }
        }
        
        return allPermissions;
    }
    
    /**
     * Get permanent permissions only
     */
    public Set<String> getPermanentPermissions() {
        return new HashSet<>(permissions);
    }
    
    /**
     * Get temporary permissions with expiration times
     */
    public Map<String, Long> getTemporaryPermissions() {
        return new HashMap<>(temporaryPermissions);
    }
    
    /**
     * Get valid temporary permissions (not expired)
     */
    public Map<String, Long> getValidTemporaryPermissions() {
        long currentTime = System.currentTimeMillis();
        Map<String, Long> validTempPerms = new HashMap<>();
        
        for (Map.Entry<String, Long> entry : temporaryPermissions.entrySet()) {
            if (currentTime < entry.getValue()) {
                validTempPerms.put(entry.getKey(), entry.getValue());
            }
        }
        
        return validTempPerms;
    }
    
    /**
     * Get negated permissions
     */
    public Set<String> getNegatedPermissions() {
        return new HashSet<>(negatedPermissions);
    }
    
    /**
     * Clean up expired temporary permissions
     * Returns number of permissions cleaned up
     */
    public int cleanupExpired(long currentTime) {
        int cleaned = 0;
        Iterator<Map.Entry<String, Long>> iterator = temporaryPermissions.entrySet().iterator();
        
        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            if (currentTime >= entry.getValue()) {
                iterator.remove();
                cleaned++;
            }
        }
        
        return cleaned;
    }
    
    /**
     * Check if player has any custom permissions
     */
    public boolean hasCustomPermissions() {
        return !permissions.isEmpty() || !negatedPermissions.isEmpty() || !temporaryPermissions.isEmpty();
    }
    
    /**
     * Clear all permissions
     */
    public void clearAll() {
        permissions.clear();
        negatedPermissions.clear();
        temporaryPermissions.clear();
    }
    
    @Override
    public String toString() {
        return "PlayerPermissions{" +
                "permissions=" + permissions.size() +
                ", negatedPermissions=" + negatedPermissions.size() +
                ", temporaryPermissions=" + temporaryPermissions.size() +
                '}';
    }
}
