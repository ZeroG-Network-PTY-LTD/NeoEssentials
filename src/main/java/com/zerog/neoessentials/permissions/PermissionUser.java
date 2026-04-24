package com.zerog.neoessentials.permissions;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PermissionUser {
    private final UUID uuid;
    private String group;
    private final Set<String> permissions;
    private String prefix;
    private String suffix;
    /** node → expiry epoch-ms (UTC). Only entries in the future are active. */
    private final Map<String, Long> tempPermissions = new ConcurrentHashMap<>();
    /**
     * Contextual permission overrides.
     * Outer key: context key (e.g. {@code "world:overworld"}, {@code "time:day"}).
     * Inner map: permission node → explicit grant (true) or deny (false).
     */
    private final Map<String, Map<String, Boolean>> contextualPermissions = new ConcurrentHashMap<>();
    /**
     * Per-node condition expressions (node → condition string).
     * When a permission would be granted, the condition is evaluated; if it
     * fails the grant is withheld.
     */
    private final Map<String, String> conditions = new ConcurrentHashMap<>();

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

    /**
     * Set an explicit context override for a permission node.
     *
     * @param contextKey context identifier (e.g. {@code "world:overworld"}, {@code "time:day"})
     * @param node       the permission node
     * @param value      {@code true} = grant, {@code false} = deny in this context
     */
    public void addContextPermission(String contextKey, String node, boolean value) {
        contextualPermissions
            .computeIfAbsent(contextKey.toLowerCase().trim(), k -> new ConcurrentHashMap<>())
            .put(node.toLowerCase().trim(), value);
    }

    /**
     * Remove an explicit context override for a permission node.
     *
     * @return {@code true} if an entry was removed
     */
    public boolean removeContextPermission(String contextKey, String node) {
        Map<String, Boolean> ctx = contextualPermissions.get(contextKey.toLowerCase().trim());
        if (ctx == null) return false;
        boolean removed = ctx.remove(node.toLowerCase().trim()) != null;
        if (ctx.isEmpty()) contextualPermissions.remove(contextKey.toLowerCase().trim());
        return removed;
    }

    /**
     * Returns the explicit context value for the node in the given context, or
     * {@code null} if no override is registered.
     */
    public Boolean getContextPermission(String contextKey, String node) {
        Map<String, Boolean> ctx = contextualPermissions.get(contextKey.toLowerCase().trim());
        if (ctx == null) return null;
        return ctx.get(node.toLowerCase().trim());
    }

    /**
     * Unmodifiable view of all contextual overrides
     * (outer key = context, inner map = node → grant/deny).
     */
    public Map<String, Map<String, Boolean>> getContextualPermissions() {
        Map<String, Map<String, Boolean>> result = new HashMap<>();
        contextualPermissions.forEach((k, v) -> result.put(k, Collections.unmodifiableMap(v)));
        return Collections.unmodifiableMap(result);
    }

    // ── Conditions ────────────────────────────────────────────────────────────

    /**
     * Attach a condition expression to a permission node.
     * When the permission is granted, the condition must also evaluate to {@code true}.
     *
     * @param node      the permission node (e.g. {@code "neoessentials.fly"})
     * @param condition the condition expression (e.g. {@code "time:day AND gamemode:survival"})
     */
    public void setCondition(String node, String condition) {
        conditions.put(node.toLowerCase().trim(), condition.trim());
    }

    /** Remove a condition from a permission node. Returns {@code true} if one existed. */
    public boolean removeCondition(String node) {
        return conditions.remove(node.toLowerCase().trim()) != null;
    }

    /**
     * Returns the condition expression attached to the node, or {@code null} if none.
     */
    public String getCondition(String node) {
        return conditions.get(node.toLowerCase().trim());
    }

    /** Unmodifiable view of all per-node conditions (node → expression). */
    public Map<String, String> getConditions() {
        return Collections.unmodifiableMap(conditions);
    }
}
