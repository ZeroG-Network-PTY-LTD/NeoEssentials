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
    
    /**
     * Check if this adapter is properly loaded and available for use.
     * @return true if the external system is available, false otherwise.
     */
    boolean isAvailable();

    // ── New default methods — source-compatible with all existing implementations ──

    /**
     * Returns the detected version string of the underlying mod/API,
     * or {@code "unknown"} if it could not be determined.
     */
    default String getVersion() {
        return "unknown";
    }

    /**
     * Returns {@code false} when the adapter has encountered enough consecutive
     * runtime failures that it should no longer be considered reliable.
     * NeoEssentials uses this as a signal to activate the internal-system fallback.
     */
    default boolean isHealthy() {
        return true;
    }

    /**
     * Returns the number of consecutive permission-check failures recorded
     * since the last successful check.  Zero means the adapter is operating normally.
     */
    default int getConsecutiveFailures() {
        return 0;
    }
}
