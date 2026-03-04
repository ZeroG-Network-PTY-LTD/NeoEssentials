package com.zerog.neoessentials.vault.api;

import java.util.UUID;

/**
 * NeoEssentials Vault Chat API — NeoForge equivalent of Vault's Chat interface.
 * <p>
 * Provides a standardised chat metadata interface (group prefix/suffix, player prefix/suffix)
 * that other NeoForge mods can code against without depending on a specific implementation.
 * <p>
 * Register an implementation via {@link VaultServiceRegistry#registerChat}.
 * Retrieve the active implementation via {@link VaultServiceRegistry#getChat()}.
 */
public abstract class VaultChat {

    /** Human-readable name of this chat implementation. */
    public abstract String getName();

    /** Whether this chat implementation is currently active and usable. */
    public abstract boolean isEnabled();

    // ── Player metadata ───────────────────────────────────────────────────────

    /**
     * Get the prefix for a player.
     * @param world world name, or null for global
     * @param playerId the player UUID
     * @return the prefix string, or empty string if none
     */
    public abstract String getPlayerPrefix(String world, UUID playerId);
    public String getPlayerPrefix(UUID playerId) { return getPlayerPrefix(null, playerId); }

    /**
     * Set the prefix for a player.
     * @param world world name, or null for global
     */
    public abstract void setPlayerPrefix(String world, UUID playerId, String prefix);
    public void setPlayerPrefix(UUID playerId, String prefix) { setPlayerPrefix(null, playerId, prefix); }

    /**
     * Get the suffix for a player.
     * @return the suffix string, or empty string if none
     */
    public abstract String getPlayerSuffix(String world, UUID playerId);
    public String getPlayerSuffix(UUID playerId) { return getPlayerSuffix(null, playerId); }

    /**
     * Set the suffix for a player.
     */
    public abstract void setPlayerSuffix(String world, UUID playerId, String suffix);
    public void setPlayerSuffix(UUID playerId, String suffix) { setPlayerSuffix(null, playerId, suffix); }

    /**
     * Get a custom metadata value for a player.
     * @param world world name, or null for global
     * @param playerId the player UUID
     * @param node the metadata key
     * @return the value string, or empty string if not set
     */
    public String getPlayerInfoString(String world, UUID playerId, String node) { return ""; }
    public void setPlayerInfoString(String world, UUID playerId, String node, String value) {}

    public int getPlayerInfoInteger(String world, UUID playerId, String node, int defaultValue) { return defaultValue; }
    public void setPlayerInfoInteger(String world, UUID playerId, String node, int value) {}

    public double getPlayerInfoDouble(String world, UUID playerId, String node, double defaultValue) { return defaultValue; }
    public void setPlayerInfoDouble(String world, UUID playerId, String node, double value) {}

    public boolean getPlayerInfoBoolean(String world, UUID playerId, String node, boolean defaultValue) { return defaultValue; }
    public void setPlayerInfoBoolean(String world, UUID playerId, String node, boolean value) {}

    // ── Group metadata ────────────────────────────────────────────────────────

    /**
     * Get the prefix for a group.
     * @param world world name, or null for global
     * @param group group name
     * @return the prefix string, or empty string if none
     */
    public abstract String getGroupPrefix(String world, String group);
    public String getGroupPrefix(String group) { return getGroupPrefix(null, group); }

    /**
     * Set the prefix for a group.
     */
    public abstract void setGroupPrefix(String world, String group, String prefix);
    public void setGroupPrefix(String group, String prefix) { setGroupPrefix(null, group, prefix); }

    /**
     * Get the suffix for a group.
     * @return the suffix string, or empty string if none
     */
    public abstract String getGroupSuffix(String world, String group);
    public String getGroupSuffix(String group) { return getGroupSuffix(null, group); }

    /**
     * Set the suffix for a group.
     */
    public abstract void setGroupSuffix(String world, String group, String suffix);
    public void setGroupSuffix(String group, String suffix) { setGroupSuffix(null, group, suffix); }

    public String getGroupInfoString(String world, String group, String node) { return ""; }
    public void setGroupInfoString(String world, String group, String node, String value) {}

    public int getGroupInfoInteger(String world, String group, String node, int defaultValue) { return defaultValue; }
    public void setGroupInfoInteger(String world, String group, String node, int value) {}

    public double getGroupInfoDouble(String world, String group, String node, double defaultValue) { return defaultValue; }
    public void setGroupInfoDouble(String world, String group, String node, double value) {}

    public boolean getGroupInfoBoolean(String world, String group, String node, boolean defaultValue) { return defaultValue; }
    public void setGroupInfoBoolean(String world, String group, String node, boolean value) {}
}

