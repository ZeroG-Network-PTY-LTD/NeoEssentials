package com.zerog.neoessentials.permissions;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PermissionGroup {
    private String name;
    private final Set<String> permissions;
    private final Set<String> inherits;
    private String prefix = "";
    private String suffix = "";
    /** Higher priority groups are checked first in inheritance resolution. Default 0. */
    private int priority = 0;
    /** node → expiry epoch-ms (UTC). Only entries in the future are active. */
    private final Map<String, Long> tempPermissions = new ConcurrentHashMap<>();
    /**
     * Contextual permission overrides.
     * Outer key: context identifier. Inner map: node → explicit grant/deny.
     */
    private final Map<String, Map<String, Boolean>> contextualPermissions = new ConcurrentHashMap<>();
    /**
     * Per-node condition expressions (node → condition string).
     */
    private final Map<String, String> conditions = new ConcurrentHashMap<>();

    public PermissionGroup(String name) {
        this.name = name;
        this.permissions = ConcurrentHashMap.newKeySet();
        this.inherits = ConcurrentHashMap.newKeySet();
    }

    public String getName() { return name; }
    /** Package-private — only {@link PermissionManager#renameGroup} should call this, so the
     *  map key (lowercased name) and every cross-reference stay in sync with it. */
    void setName(String name) { this.name = name; }
    public Set<String> getPermissions() { return permissions; }
    public Set<String> getInherits() { return inherits; }
    public String getPrefix() { return prefix; }
    public void setPrefix(String prefix) { this.prefix = prefix; }
    public String getSuffix() { return suffix; }
    public void setSuffix(String suffix) { this.suffix = suffix; }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public void addPermission(String permission) { permissions.add(permission.toLowerCase()); }
    public void removePermission(String permission) { permissions.remove(permission.toLowerCase()); }
    public void addInheritance(String groupName) { inherits.add(groupName); }
    public void removeInheritance(String groupName) { inherits.remove(groupName); }

    // ── Temporary permissions ─────────────────────────────────────────────

    /** Grant a temporary permission that expires at {@code expiryMs} (epoch-ms UTC). */
    public void addTempPermission(String node, long expiryMs) {
        tempPermissions.put(node.toLowerCase(), expiryMs);
    }

    /** Remove a temporary permission (does not affect regular permissions). */
    public void removeTempPermission(String node) {
        tempPermissions.remove(node.toLowerCase());
    }

    /** Unmodifiable snapshot of the raw temp-permissions map (node → expiry ms). */
    public Map<String, Long> getTempPermissions() {
        return Collections.unmodifiableMap(tempPermissions);
    }

    /**
     * Remove all expired temp permissions.
     * @return number of entries removed
     */
    public int purgeExpiredTempPermissions() {
        long now = System.currentTimeMillis();
        int[] removed = {0};
        tempPermissions.entrySet().removeIf(e -> {
            if (e.getValue() <= now) { removed[0]++; return true; }
            return false;
        });
        return removed[0];
    }

    /**
     * Returns {@code true} if the node exists as an unexpired temp permission.
     * Does NOT check regular {@link #permissions}.
     */
    public boolean hasActiveTempPermission(String node) {
        Long expiry = tempPermissions.get(node.toLowerCase());
        return expiry != null && expiry > System.currentTimeMillis();
    }

    // ── Contextual permissions ────────────────────────────────────────────────

    public void addContextPermission(String contextKey, String node, boolean value) {
        contextualPermissions
            .computeIfAbsent(contextKey.toLowerCase().trim(), k -> new ConcurrentHashMap<>())
            .put(node.toLowerCase().trim(), value);
    }

    public boolean removeContextPermission(String contextKey, String node) {
        Map<String, Boolean> ctx = contextualPermissions.get(contextKey.toLowerCase().trim());
        if (ctx == null) return false;
        boolean removed = ctx.remove(node.toLowerCase().trim()) != null;
        if (ctx.isEmpty()) contextualPermissions.remove(contextKey.toLowerCase().trim());
        return removed;
    }

    public Boolean getContextPermission(String contextKey, String node) {
        Map<String, Boolean> ctx = contextualPermissions.get(contextKey.toLowerCase().trim());
        if (ctx == null) return null;
        return ctx.get(node.toLowerCase().trim());
    }

    public Map<String, Map<String, Boolean>> getContextualPermissions() {
        Map<String, Map<String, Boolean>> result = new HashMap<>();
        contextualPermissions.forEach((k, v) -> result.put(k, Collections.unmodifiableMap(v)));
        return Collections.unmodifiableMap(result);
    }

    // ── Conditions ────────────────────────────────────────────────────────────

    public void setCondition(String node, String condition) {
        conditions.put(node.toLowerCase().trim(), condition.trim());
    }

    public boolean removeCondition(String node) {
        return conditions.remove(node.toLowerCase().trim()) != null;
    }

    public String getCondition(String node) {
        return conditions.get(node.toLowerCase().trim());
    }

    public Map<String, String> getConditions() {
        return Collections.unmodifiableMap(conditions);
    }
}