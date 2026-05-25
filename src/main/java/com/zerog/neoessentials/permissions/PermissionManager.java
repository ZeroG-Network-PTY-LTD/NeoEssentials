
package com.zerog.neoessentials.permissions;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
        return hasPermission(uuid, permission, PermissionContext.EMPTY);
    }

    /**
     * Context-aware permission check.  When {@code context} is not {@link PermissionContext#EMPTY},
     * contextual overrides are evaluated (deny wins; then grant; then fall through to regular check).
     */
    public boolean hasPermission(UUID uuid, String permission, PermissionContext context) {
        permission = permission.toLowerCase();
        // Context-aware checks bypass the cache to ensure fresh evaluation
        boolean noContext = (context == null || context == PermissionContext.EMPTY);
        String cacheKey = uuid + ":" + permission;
        boolean cacheEnabled = noContext
            && com.zerog.neoessentials.config.ConfigManager.getInstance().isPermissionCacheEnabled();
        long cacheTtl = getCacheTtlMs();
        if (cacheEnabled) {
            CachedPermission cached = permissionCache.get(cacheKey);
            if (cached != null && !cached.isExpired(cacheTtl)) {
                return cached.result;
            }
        }
        boolean result = computePermission(uuid, permission, context);
        if (cacheEnabled) {
            permissionCache.put(cacheKey, new CachedPermission(result));
            if (permissionCache.size() % 100 == 0) {
                cleanExpiredCache();
            }
        }
        return result;
    }


    private boolean computePermission(UUID uuid, String permission, PermissionContext context) {
        LOGGER.debug("Computing permission '{}' for UUID {} (context={})", permission, uuid,
                context == PermissionContext.EMPTY ? "NONE" : context.worldId);
        PermissionUser user = getUser(uuid);
        String groupName = (user != null && user.getGroup() != null) ? user.getGroup() : defaultGroup;
        boolean hasCtx = (context != null && context != PermissionContext.EMPTY);

        // ── 1. Contextual denies (user) ──────────────────────────────────────
        if (hasCtx && user != null) {
            for (Map.Entry<String, java.util.Map<String, Boolean>> entry :
                    user.getContextualPermissions().entrySet()) {
                if (context.matches(entry.getKey())) {
                    Boolean ctxVal = entry.getValue().get(permission);
                    if (ctxVal == null) {
                        // wildcard check
                        ctxVal = wildcardLookup(entry.getValue(), permission);
                    }
                    if (Boolean.FALSE.equals(ctxVal)) {
                        LOGGER.debug("  -> Denied by user contextual permission ({})", entry.getKey());
                        return false;
                    }
                }
            }
        }

        // ── 2. Contextual denies (group, with inheritance) ───────────────────
        if (hasCtx && hasGroupContextualDeny(groupName, permission, context, new HashSet<>())) {
            LOGGER.debug("  -> Denied by group contextual permission");
            return false;
        }

        // ── 3. Regular negative permissions (user) ───────────────────────────
        if (user != null && hasNegativePermission(user.getPermissions(), permission)) {
            LOGGER.debug("  -> Denied by user negative permission");
            return false;
        }

        // ── 4. Regular negative permissions (group, with inheritance) ─────────
        if (hasGroupNegativePermission(groupName, permission, new HashSet<>())) {
            LOGGER.debug("  -> Denied by group negative permission");
            return false;
        }

        // ── 5. User temporary permissions ────────────────────────────────────
        if (user != null && hasTempPermissionWithWildcards(user.getTempPermissions(), permission)) {
            LOGGER.debug("  -> Granted by user temp permission");
            return applyCondition(uuid, permission, context, user.getCondition(permission));
        }

        // ── 6. Contextual grants (user) ───────────────────────────────────────
        if (hasCtx && user != null) {
            for (Map.Entry<String, java.util.Map<String, Boolean>> entry :
                    user.getContextualPermissions().entrySet()) {
                if (context.matches(entry.getKey())) {
                    Boolean ctxVal = entry.getValue().get(permission);
                    if (ctxVal == null) {
                        ctxVal = wildcardLookup(entry.getValue(), permission);
                    }
                    if (Boolean.TRUE.equals(ctxVal)) {
                        LOGGER.debug("  -> Granted by user contextual permission ({})", entry.getKey());
                        return applyCondition(uuid, permission, context, user.getCondition(permission));
                    }
                }
            }
        }

        // ── 7. Regular user permissions ───────────────────────────────────────
        if (user != null && hasPermissionWithWildcards(user.getPermissions(), permission)) {
            LOGGER.debug("  -> Granted by user permission");
            return applyCondition(uuid, permission, context, user.getCondition(permission));
        }

        // ── 8. Group permissions (context + regular, with inheritance) ────────
        boolean result = hasGroupPermission(groupName, permission, context, new HashSet<>());
        LOGGER.debug("  -> Group permission check result: {}", result);
        return result;
    }

    /**
     * Applies a condition expression to a would-be grant.  Returns {@code true}
     * if the condition passes (or there is no condition), {@code false} if it fails.
     */
    private boolean applyCondition(UUID uuid, String permission, PermissionContext context, String condExpr) {
        if (condExpr == null || condExpr.isBlank()) return true;
        try {
            net.minecraft.server.level.ServerPlayer player = null;
            if (uuid != null) {
                net.minecraft.server.MinecraftServer server =
                    net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
                if (server != null) player = server.getPlayerList().getPlayer(uuid);
            }
            return PermissionConditionManager.getInstance().evaluate(condExpr, context, player);
        } catch (Exception e) {
            LOGGER.warn("Condition evaluation failed for node '{}': {}", permission, e.getMessage());
            return true; // fail-open: if we can't evaluate, grant
        }
    }

    /**
     * Wildcard lookup: returns the first value in {@code map} whose key is a
     * wildcard pattern that covers {@code permission}, or {@code null} if none.
     */
    private static Boolean wildcardLookup(java.util.Map<String, Boolean> map, String permission) {
        for (Map.Entry<String, Boolean> e : map.entrySet()) {
            String key = e.getKey();
            if (key.endsWith(".*")) {
                String prefix = key.substring(0, key.length() - 2);
                if (permission.startsWith(prefix + ".")) return e.getValue();
            }
        }
        return null;
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
        // Check inherited groups sorted by priority (highest first)
        java.util.List<String> sorted = sortedInherits(group);
        for (String parent : sorted) {
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


    private boolean hasGroupPermission(String groupName, String permission, PermissionContext context, Set<String> visited) {
        if (groupName == null || visited.contains(groupName.toLowerCase())) return false;
        visited.add(groupName.toLowerCase());
        PermissionGroup group = getGroup(groupName);
        if (group == null) {
            LOGGER.debug("  Group '{}' not found", groupName);
            return false;
        }
        LOGGER.debug("  Checking group '{}' with {} permissions", groupName, group.getPermissions().size());

        // ── Contextual grants (this group) ────────────────────────────────────
        boolean hasCtx = (context != null && context != PermissionContext.EMPTY);
        if (hasCtx) {
            for (Map.Entry<String, java.util.Map<String, Boolean>> entry :
                    group.getContextualPermissions().entrySet()) {
                if (context.matches(entry.getKey())) {
                    Boolean ctxVal = entry.getValue().get(permission);
                    if (ctxVal == null) ctxVal = wildcardLookup(entry.getValue(), permission);
                    if (Boolean.TRUE.equals(ctxVal)) return true;
                }
            }
        }

        // ── Group temp permissions ─────────────────────────────────────────────
        if (hasTempPermissionWithWildcards(group.getTempPermissions(), permission)) {
            String cond = group.getCondition(permission);
            // conditions for groups are currently always evaluated against EMPTY UUID;
            // group UUIDs don't exist — conditions are a user-side feature for groups
            return applyCondition(null, permission, context, cond);
        }

        // ── Regular group permissions ──────────────────────────────────────────
        if (hasPermissionWithWildcards(group.getPermissions(), permission)) {
            return applyCondition(null, permission, context, group.getCondition(permission));
        }

        // ── Inherited groups ───────────────────────────────────────────────────
        java.util.List<String> sorted = sortedInherits(group);
        for (String parent : sorted) {
            LOGGER.debug("  Checking inherited group '{}'", parent);
            if (hasGroupPermission(parent, permission, context, visited)) return true;
        }
        return false;
    }

    /**
     * Returns {@code true} if any group in the hierarchy has a contextual
     * {@code false} override for the given permission + context.
     */
    private boolean hasGroupContextualDeny(String groupName, String permission,
                                           PermissionContext context, Set<String> visited) {
        if (groupName == null || visited.contains(groupName.toLowerCase())) return false;
        visited.add(groupName.toLowerCase());
        PermissionGroup group = getGroup(groupName);
        if (group == null) return false;
        for (Map.Entry<String, java.util.Map<String, Boolean>> entry :
                group.getContextualPermissions().entrySet()) {
            if (context.matches(entry.getKey())) {
                Boolean ctxVal = entry.getValue().get(permission);
                if (ctxVal == null) ctxVal = wildcardLookup(entry.getValue(), permission);
                if (Boolean.FALSE.equals(ctxVal)) return true;
            }
        }
        for (String parent : sortedInherits(group)) {
            if (hasGroupContextualDeny(parent, permission, context, visited)) return true;
        }
        return false;
    }

    /**
     * Returns the inherited group names of the given group, sorted by their priority
     * (highest priority first). Groups not found in the registry are treated as priority 0.
     */
    private java.util.List<String> sortedInherits(PermissionGroup group) {
        return group.getInherits().stream()
                .sorted((a, b) -> {
                    PermissionGroup ga = getGroup(a);
                    PermissionGroup gb = getGroup(b);
                    int pa = (ga != null) ? ga.getPriority() : 0;
                    int pb = (gb != null) ? gb.getPriority() : 0;
                    return Integer.compare(pb, pa); // descending
                })
                .toList();
    }

    // ── Temporary permission checks ───────────────────────────────────────────

    /**
     * Returns {@code true} if the node (or a covering wildcard) exists in the temp map
     * AND its expiry has not yet elapsed.
     */
    private boolean hasTempPermissionWithWildcards(Map<String, Long> temps, String permission) {
        long now = System.currentTimeMillis();
        for (Map.Entry<String, Long> e : temps.entrySet()) {
            if (e.getValue() <= now) continue; // expired
            String perm = e.getKey();
            if (perm.equals(permission)) return true;
            if (perm.endsWith(".*")) {
                String prefix = perm.substring(0, perm.length() - 2);
                if (permission.startsWith(prefix + ".")) return true;
            }
        }
        return false;
    }

    // ── Purge expired temp permissions ────────────────────────────────────────

    /**
     * Remove all expired temp permissions from every user and group.
     * Called periodically from {@link PermissionExpiryHandler} (every 30 s).
     *
     * @param server live server reference used to notify online players
     * @return total number of expired entries removed
     */
    public int purgeExpiredTempPermissions(net.minecraft.server.MinecraftServer server) {
        int total = 0;
        long now = System.currentTimeMillis();

        // ── Users ────────────────────────────────────────────────────────────
        for (PermissionUser user : users.values()) {
            Map<String, Long> snapshot = new java.util.HashMap<>(user.getTempPermissions());
            int removed = user.purgeExpiredTempPermissions();
            if (removed > 0) {
                total += removed;
                clearCache();
                if (server != null) {
                    net.minecraft.server.level.ServerPlayer online =
                        server.getPlayerList().getPlayer(user.getUuid());
                    for (Map.Entry<String, Long> e : snapshot.entrySet()) {
                        if (e.getValue() <= now) {
                            String node = e.getKey();
                            if (online != null) {
                                online.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                                    "§eYour temporary permission §f" + node + "§e has expired."));
                            }
                            String playerName = online != null
                                ? online.getGameProfile().getName()
                                : user.getUuid().toString();
                            LOGGER.info("[TempPerms] Expired user temp permission: {} -> {}", playerName, node);
                            PermissionAuditLogger.log("SYSTEM",
                                PermissionAuditLogger.USER_TEMP_PERM_EXPIRED, playerName, "node=" + node);
                        }
                    }
                }
            }
        }

        // ── Groups ───────────────────────────────────────────────────────────
        for (PermissionGroup group : groups.values()) {
            Map<String, Long> snapshot = new java.util.HashMap<>(group.getTempPermissions());
            int removed = group.purgeExpiredTempPermissions();
            if (removed > 0) {
                total += removed;
                clearCache();
                for (Map.Entry<String, Long> e : snapshot.entrySet()) {
                    if (e.getValue() <= now) {
                        LOGGER.info("[TempPerms] Expired group temp permission: {} -> {}",
                            group.getName(), e.getKey());
                        PermissionAuditLogger.log("SYSTEM",
                            PermissionAuditLogger.GROUP_TEMP_PERM_EXPIRED,
                            group.getName(), "node=" + e.getKey());
                    }
                }
            }
        }

        if (total > 0) {
            LOGGER.info("[TempPerms] Purged {} expired temporary permission(s)", total);
            try { PermissionStorage.save(this); }
            catch (Exception e) { LOGGER.warn("[TempPerms] Failed to persist after purge: {}", e.getMessage()); }
        }
        return total;
    }

    // ── Duration utilities ────────────────────────────────────────────────────

    private static final Pattern DURATION_PATTERN =
            Pattern.compile("^(?:(\\d+)d)?(?:(\\d+)h)?(?:(\\d+)m)?(?:(\\d+)s)?$",
                            Pattern.CASE_INSENSITIVE);

    /**
     * Parse a human-readable duration (e.g. {@code 1d}, {@code 12h}, {@code 30m},
     * {@code 1d12h30m}) into milliseconds.
     *
     * @throws IllegalArgumentException if blank, all-zero, or syntactically invalid
     */
    public static long parseDurationMs(String input) {
        if (input == null || input.isBlank())
            throw new IllegalArgumentException("Duration cannot be empty");
        Matcher m = DURATION_PATTERN.matcher(input.trim());
        if (!m.matches())
            throw new IllegalArgumentException(
                "Invalid duration '" + input + "'. Use combinations of: 1d 12h 30m 60s");
        long days  = m.group(1) != null ? Long.parseLong(m.group(1)) : 0;
        long hours = m.group(2) != null ? Long.parseLong(m.group(2)) : 0;
        long mins  = m.group(3) != null ? Long.parseLong(m.group(3)) : 0;
        long secs  = m.group(4) != null ? Long.parseLong(m.group(4)) : 0;
        long totalSeconds = days * 86400L + hours * 3600L + mins * 60L + secs;
        if (totalSeconds <= 0)
            throw new IllegalArgumentException("Duration must be positive");
        return totalSeconds * 1000L;
    }

    /**
     * Format a remaining-time duration (ms) into a compact human string,
     * e.g. {@code "2d 3h 15m 4s"}.
     */
    public static String formatDuration(long ms) {
        if (ms <= 0) return "expired";
        long secs  = ms / 1000;
        long days  = secs / 86400; secs %= 86400;
        long hours = secs / 3600;  secs %= 3600;
        long mins  = secs / 60;    secs %= 60;
        StringBuilder sb = new StringBuilder();
        if (days  > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (mins  > 0) sb.append(mins).append("m ");
        if (secs  > 0 || sb.length() == 0) sb.append(secs).append("s");
        return sb.toString().trim();
    }
}
