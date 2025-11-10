
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
    private long getCacheTtlMs() {
        int minutes = com.zerog.neoessentials.config.ConfigManager.getInstance().getPermissionCacheExpiryMinutes();
        return minutes * 60_000L;
    }
    
    private static class CachedPermission {
        final boolean result;
        final long timestamp;

        CachedPermission(boolean result) {
            this.result = result;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired(long ttlMs) {
            return System.currentTimeMillis() - timestamp > ttlMs;
        }
    }

    public PermissionManager() {
    // Try to load from permissions.json (via PermissionStorage), fallback to config.json
    String configDefault = com.zerog.neoessentials.config.ConfigManager.getInstance().getDefaultGroup();
    this.defaultGroup = (configDefault != null && !configDefault.trim().isEmpty()) ? configDefault.trim() : "default";
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
        if (groupName != null && !groupName.trim().isEmpty()) {
            this.defaultGroup = groupName.trim();
        }
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
            
            // Schedule async save to avoid blocking (don't save synchronously on every getUser call)
            // The save will happen on server shutdown or manual /pex reload
        }
        return user;
    }

    public Collection<PermissionUser> getUsers() {
        return users.values();
    }

    public boolean hasPermission(UUID uuid, String permission) {
        permission = permission.toLowerCase();
        String cacheKey = uuid + ":" + permission;
        boolean cacheEnabled = com.zerog.neoessentials.config.ConfigManager.getInstance().isPermissionCacheEnabled();
        long cacheTtl = getCacheTtlMs();
        if (cacheEnabled) {
            // Check cache first
            CachedPermission cached = permissionCache.get(cacheKey);
            if (cached != null && !cached.isExpired(cacheTtl)) {
                return cached.result;
            }
        }
        // Compute permission
        boolean result = computePermission(uuid, permission);
        if (cacheEnabled) {
            // Cache the result
            permissionCache.put(cacheKey, new CachedPermission(result));
            // Clean expired entries periodically (every 100 checks)
            if (permissionCache.size() % 100 == 0) {
                cleanExpiredCache();
            }
        }
        return result;
    }
    
    private boolean computePermission(UUID uuid, String permission) {
        LOGGER.debug("Computing permission '{}' for UUID {}", permission, uuid);
        PermissionUser user = getUser(uuid);
        String groupName = (user != null && user.getGroup() != null) ? user.getGroup() : defaultGroup;
        LOGGER.debug("  User group: {}", groupName);
        
        // Check user negative permissions
        if (user != null && hasNegativePermission(user.getPermissions(), permission)) {
            LOGGER.debug("  -> Denied by user negative permission");
            return false;
        }
        
        // Check group negative permissions (with inheritance)
        if (hasGroupNegativePermission(groupName, permission, new HashSet<>())) {
            LOGGER.debug("  -> Denied by group negative permission");
            return false;
        }
        
        // Check user permissions (including wildcards)
        if (user != null && hasPermissionWithWildcards(user.getPermissions(), permission)) {
            LOGGER.debug("  -> Granted by user permission");
            return true;
        }
        
        // Check group permissions (with inheritance and wildcards)
        boolean result = hasGroupPermission(groupName, permission, new HashSet<>());
        LOGGER.debug("  -> Group permission check result: {}", result);
        return result;
    }
    
    /**
     * Clears expired entries from the permission cache
     */
    private void cleanExpiredCache() {
    long cacheTtl = getCacheTtlMs();
    permissionCache.entrySet().removeIf(entry -> entry.getValue().isExpired(cacheTtl));
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
            LOGGER.debug("Checking perm '{}' against permission '{}'", perm, permission);
            if (perm.equals(permission)) {
                LOGGER.debug("  -> Exact match!");
                return true;
            }
            if (perm.endsWith(".*")) {
                String prefix = perm.substring(0, perm.length() - 2);
                LOGGER.debug("  -> Wildcard check: does '{}' start with '{}'?", permission, prefix + ".");
                if (permission.startsWith(prefix + ".")) {
                    LOGGER.debug("  -> Wildcard match!");
                    return true;
                }
            }
        }
        LOGGER.debug("No match found for permission '{}'", permission);
        return false;
    }

    private boolean hasGroupPermission(String groupName, String permission, Set<String> visited) {
        if (groupName == null || visited.contains(groupName.toLowerCase())) return false;
        visited.add(groupName.toLowerCase());
        PermissionGroup group = getGroup(groupName);
        if (group == null) {
            LOGGER.debug("  Group '{}' not found", groupName);
            return false;
        }
        LOGGER.debug("  Checking group '{}' with {} permissions", groupName, group.getPermissions().size());
        if (hasPermissionWithWildcards(group.getPermissions(), permission)) return true;
        for (String parent : group.getInherits()) {
            LOGGER.debug("  Checking inherited group '{}'", parent);
            if (hasGroupPermission(parent, permission, visited)) return true;
        }
        return false;
    }
}
