package com.zerog.neoessentials.vault.api;

import java.util.UUID;

/**
 * NeoEssentials Vault Permission API — NeoForge equivalent of Vault's Permission interface.
 * <p>
 * Provides a standardised permission interface that other NeoForge mods can code against.
 * Register an implementation via {@link VaultServiceRegistry#registerPermission}.
 * Retrieve the active implementation via {@link VaultServiceRegistry#getPermission()}.
 */
public abstract class VaultPermission {

    /** Human-readable name of this permission implementation. */
    public abstract String getName();

    /** Whether this implementation is currently active and usable. */
    public abstract boolean isEnabled();

    /** Whether this implementation supports per-world permissions. */
    public abstract boolean supportsWorlds();

    // ── Player permissions ────────────────────────────────────────────────────

    public abstract boolean playerHas(String world, UUID playerId, String permission);
    public boolean playerHas(UUID playerId, String permission) { return playerHas(null, playerId, permission); }

    public abstract boolean playerAdd(String world, UUID playerId, String permission);
    public boolean playerAdd(UUID playerId, String permission) { return playerAdd(null, playerId, permission); }

    public abstract boolean playerRemove(String world, UUID playerId, String permission);
    public boolean playerRemove(UUID playerId, String permission) { return playerRemove(null, playerId, permission); }

    // ── Groups ────────────────────────────────────────────────────────────────

    public abstract boolean hasGroupSupport();

    public abstract boolean groupHas(String world, String group, String permission);
    public abstract boolean groupAdd(String world, String group, String permission);
    public abstract boolean groupRemove(String world, String group, String permission);

    public abstract boolean playerInGroup(String world, UUID playerId, String group);
    public boolean playerInGroup(UUID playerId, String group) { return playerInGroup(null, playerId, group); }

    public abstract boolean playerAddGroup(String world, UUID playerId, String group);
    public abstract boolean playerRemoveGroup(String world, UUID playerId, String group);

    public abstract String[] getPlayerGroups(String world, UUID playerId);
    public String[] getPlayerGroups(UUID playerId) { return getPlayerGroups(null, playerId); }

    public abstract String getPrimaryGroup(String world, UUID playerId);
    public String getPrimaryGroup(UUID playerId) { return getPrimaryGroup(null, playerId); }

    public abstract String[] getGroups();
}

