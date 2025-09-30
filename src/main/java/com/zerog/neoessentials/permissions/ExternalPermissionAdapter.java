package com.zerog.neoessentials.permissions;

import java.util.UUID;

/**
 * Interface for external permission adapters (LuckPerms, FTB Ranks, etc).
 */
public interface ExternalPermissionAdapter {
    /**
     * Check if the user has the given permission.
     * @param uuid The UUID of the user.
     * @param permission The permission node.
     * @return true if the user has the permission, false otherwise.
     */
    boolean hasPermission(UUID uuid, String permission);

    /**
     * Get the prefix for the user (if supported).
     * @param uuid The UUID of the user.
     * @return The prefix string, or null if not supported.
     */
    String getPrefix(UUID uuid);

    /**
     * Get the suffix for the user (if supported).
     * @param uuid The UUID of the user.
     * @return The suffix string, or null if not supported.
     */
    String getSuffix(UUID uuid);

    /**
     * Reload the external permission data (if supported).
     */
    void reload();

    /**
     * @return The name of the external system (e.g., "LuckPerms").
     */
    String getName();
}
