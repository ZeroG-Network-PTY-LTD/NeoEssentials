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

    /**
     * Returns {@code true} when the external permission system has an <em>explicit</em>
     * deny (negative) entry for the given permission node for the given player.
     *
     * <p>This is distinct from {@link #hasPermission} returning {@code false}:
     * <ul>
     *   <li>{@code false} from {@code hasPermission} could mean the permission is
     *       simply not set (undefined / inherited) in the external system.</li>
     *   <li>{@code true} from {@code isExplicitlyDenied} means an admin has
     *       intentionally negated the node.</li>
     * </ul>
     *
     * <p>NeoEssentials uses this to decide whether registry-level default
     * permissions (those registered with {@code defaultValue=true}) should still
     * be honoured: they are granted when the external system merely has no opinion
     * ({@code UNDEFINED}), but suppressed when the external system has explicitly
     * revoked the permission ({@code FALSE}).
     *
     * <p>The default implementation returns {@code false} so that adapters that
     * cannot distinguish UNDEFINED from FALSE remain source-compatible.
     *
     * @param uuid       the player's UUID
     * @param permission the permission node to query
     * @return {@code true} only if an explicit deny is present in the external system
     */
    default boolean isExplicitlyDenied(UUID uuid, String permission) {
        return false;
    }

    /**
     * Returns the rank/weight of the user's primary group in the external system, for
     * features that need to compare rank (tablist sort order, vanish see-priority, etc).
     *
     * <p>The default implementation returns {@link Integer#MIN_VALUE} as a sentinel meaning
     * "not supported / unknown" — callers must check for this rather than treating it as a
     * real weight of 0, since adapters that can't determine a weight would otherwise silently
     * flatten every player to the same rank.</p>
     *
     * @param uuid the player's UUID
     * @return the group's weight, or {@code Integer.MIN_VALUE} if unavailable
     */
    default int getGroupWeight(UUID uuid) {
        return Integer.MIN_VALUE;
    }
}
