
package com.zerog.neoessentials.permissions;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PermissionManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionManager.class);
    private final Map<String, PermissionGroup> groups = new ConcurrentHashMap<>();
    private final Map<UUID, PermissionUser> users = new ConcurrentHashMap<>();
    private String defaultGroup; // Will be loaded from config
    
    // Permission caching
    private final Map<String, CachedPermission> permissionCache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL = 300000; // 5 minutes in milliseconds
    
    private static class CachedPermission {
        final boolean result;
        final long timestamp;
        
        CachedPermission(boolean result) {
            this.result = result;
            this.timestamp = System.currentTimeMillis();
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL;
        }
    }

    public PermissionManager() {
        this.defaultGroup = com.zerog.neoessentials.config.ConfigManager.getInstance().getDefaultGroup();
    }

    /**
     * Reloads all permissions and groups from disk using PermissionStorage.
     */
    public void reload() throws Exception {
        this.groups.clear();
        this.users.clear();
        this.permissionCache.clear();
        PermissionStorage.load(this);
        LOGGER.info("Permissions reloaded, cache cleared");
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
        PermissionUser user = users.get(uuid);
        if (user == null) {
            // Auto-create user with default group
            user = new PermissionUser(uuid, defaultGroup);
            addUser(user);
            LOGGER.info("Auto-created user {} with default group '{}'", uuid, defaultGroup);
            
            // Auto-save new user (async to avoid blocking)
            try {
                PermissionStorage.save(this);
            } catch (Exception e) {
                LOGGER.error("Failed to save auto-created user {}", uuid, e);
            }
        }
        return user;
    }

    public Collection<PermissionUser> getUsers() {
        return users.values();
    }

    public boolean hasPermission(UUID uuid, String permission) {
        permission = permission.toLowerCase();
        String cacheKey = uuid + ":" + permission;
        
        // Check cache first
        CachedPermission cached = permissionCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return cached.result;
        }
        
        // Compute permission
        boolean result = computePermission(uuid, permission);
        
        // Cache the result
        permissionCache.put(cacheKey, new CachedPermission(result));
        
        // Clean expired entries periodically (every 100 checks)
        if (permissionCache.size() % 100 == 0) {
            cleanExpiredCache();
        }
        
        return result;
    }
    
    private boolean computePermission(UUID uuid, String permission) {
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
    
    /**
     * Clears expired entries from the permission cache
     */
    private void cleanExpiredCache() {
        permissionCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        LOGGER.debug("Cleaned permission cache, {} entries remaining", permissionCache.size());
    }
    
    /**
     * Clears the entire permission cache (useful after permission changes)
     */
    public void clearCache() {
        permissionCache.clear();
        LOGGER.debug("Permission cache cleared");
    }
    
    /**
     * Validates if a permission node is well-formed
     */
    public static boolean isValidPermission(String permission) {
        if (permission == null || permission.trim().isEmpty()) {
            return false;
        }
        
        // Trim and convert to lowercase for consistency
        permission = permission.trim().toLowerCase();
        
        // Handle wildcard permissions specially
        if (permission.endsWith(".*")) {
            String prefix = permission.substring(0, permission.length() - 2);
            // Validate the prefix part
            if (prefix.isEmpty() || !prefix.matches("^[a-z0-9._-]+$")) {
                return false;
            }
            // Prefix cannot start or end with dot, or have consecutive dots
            if (prefix.startsWith(".") || prefix.endsWith(".") || prefix.contains("..")) {
                return false;
            }
            return true;
        }
        
        // Handle negative permissions (starting with -)
        if (permission.startsWith("-")) {
            String actualPerm = permission.substring(1);
            return isValidPermission(actualPerm);
        }
        
        // Check for valid characters (alphanumeric, dots, underscores, hyphens)
        if (!permission.matches("^[a-z0-9._-]+$")) {
            return false;
        }
        
        // Cannot start or end with dot
        if (permission.startsWith(".") || permission.endsWith(".")) {
            return false;
        }
        
        // Cannot have consecutive dots
        if (permission.contains("..")) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Rate limiting for permission modifications
     */
    private final Map<UUID, Long> lastModification = new ConcurrentHashMap<>();
    private static final long MODIFICATION_COOLDOWN = 1000; // 1 second
    
    /**
     * Checks if a user can modify permissions (rate limiting)
     */
    public boolean canModifyPermissions(UUID executor) {
        long currentTime = System.currentTimeMillis();
        Long lastTime = lastModification.get(executor);
        
        if (lastTime != null && (currentTime - lastTime) < MODIFICATION_COOLDOWN) {
            return false;
        }
        
        lastModification.put(executor, currentTime);
        return true;
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
