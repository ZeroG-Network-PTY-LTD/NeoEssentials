package com.zerog.neoessentials.api.permissions;

import com.zerog.neoessentials.permissions.PermissionContext;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * Clean service API that other mods (or your own code) can use to interact with
 * NeoEssentials' permission system without depending on internal classes.
 *
 * <p>Obtain an instance via {@code NeoEssentialsAPI.getPermissionsService()}.
 *
 * <p>Example usage from another mod:
 * <pre>{@code
 * PermissionsService perms = NeoEssentialsAPI.getPermissionsService();
 * if (perms.hasPermission(player.getUUID(), "mymod.somefeature")) {
 *     // grant access
 * }
 * // Register your mod's own permission nodes so they appear in /permissions info:
 * perms.registerPermission("mymod.somefeature", "Enables the some-feature command");
 * }</pre>
 */
public interface PermissionsService {

    // ── Permission checks ─────────────────────────────────────────────────────

    /**
     * Returns {@code true} if the given player has the requested permission node,
     * going through NeoEssentials' full 5-step resolution chain (emergency → OP bypass →
     * external adapter → internal manager → vanilla-OP fallback).
     */
    boolean hasPermission(UUID playerUuid, String permissionNode);

    /**
     * Context-aware permission check.  In addition to the standard resolution chain,
     * contextual overrides (per-world, per-gamemode, time-based) and per-node conditions
     * are evaluated.
     *
     * @param context the player's current runtime context; use
     *                {@link PermissionContext#forPlayer(ServerPlayer)} to build one, or
     *                {@link PermissionContext#EMPTY} when no player context is available
     */
    boolean hasPermission(UUID playerUuid, String permissionNode, PermissionContext context);

    /**
     * Convenience overload — builds a {@link PermissionContext} from the live player
     * and delegates to {@link #hasPermission(UUID, String, PermissionContext)}.
     */
    default boolean hasPermission(ServerPlayer player, String permissionNode) {
        return hasPermission(player.getUUID(), permissionNode,
            PermissionContext.forPlayer(player));
    }

    // ── Player meta ───────────────────────────────────────────────────────────

    /** Returns the permission group the player belongs to, or the server default. */
    String getGroup(UUID playerUuid);

    /** Returns the display prefix for the player (empty string if none). */
    String getPrefix(UUID playerUuid);

    /** Returns the display suffix for the player (empty string if none). */
    String getSuffix(UUID playerUuid);

    // ── Permission node registration ─────────────────────────────────────────

    /**
     * Register a single permission node from an external mod.
     * Registered nodes appear in {@code /permissions search} and tab-completion.
     *
     * @param node        fully-qualified node (e.g. {@code "mymmod.feature.use"})
     * @param description human-readable description shown in permission listings
     */
    void registerPermission(String node, String description);

    /**
     * Bulk-register permission nodes from an external mod.
     * Keys are permission nodes; values are descriptions.
     */
    void registerPermissions(Map<String, String> permissions);

    // ── Alias management ─────────────────────────────────────────────────────

    /**
     * Register a permission alias so that legacy / short node names are silently
     * resolved to their canonical NeoEssentials equivalents.
     *
     * @param alias     the short or legacy name (e.g. {@code "essentials.fly"})
     * @param canonical the target NeoEssentials node (e.g. {@code "neoessentials.fly"})
     */
    void registerAlias(String alias, String canonical);

    /** Returns all currently registered aliases (alias → canonical). */
    Map<String, String> getAliases();

    // ── System information ────────────────────────────────────────────────────

    /** Returns {@code true} when the permission system is running in emergency (OP-only) mode. */
    boolean isEmergencyMode();

    /** Returns {@code true} when an external adapter (LuckPerms, FTB Ranks …) is active. */
    boolean isUsingExternalAdapter();

    /** Returns the names of all currently registered permission groups. */
    Collection<String> getGroupNames();

    /**
     * Returns all permission nodes registered for this player (direct, not inherited).
     * Returns an empty set if the player is not known to the internal system.
     */
    java.util.Set<String> getPlayerPermissions(UUID playerUuid);

    // ── Context helpers ───────────────────────────────────────────────────────

    /**
     * Build a {@link PermissionContext} from a live online player.
     * Convenience wrapper around {@link PermissionContext#forPlayer(ServerPlayer)}.
     */
    default PermissionContext contextFor(ServerPlayer player) {
        return PermissionContext.forPlayer(player);
    }
}

