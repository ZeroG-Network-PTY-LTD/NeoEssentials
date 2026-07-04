
    package com.zerog.neoessentials.api.permissions;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zerog.neoessentials.permissions.ExternalPermissionAdapter;
import com.zerog.neoessentials.permissions.PermissionGroup;
import com.zerog.neoessentials.permissions.PermissionManager;
import com.zerog.neoessentials.permissions.PermissionUser;

public class PermissionAPI {
    private static PermissionManager manager;
    private static ExternalPermissionAdapter externalAdapter = null;
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionAPI.class);

    /**
     * When {@code true} the permission system failed to initialise at startup.
     * All permission checks immediately fall back to vanilla OP status so that
     * server operators are never locked out of administrative commands.
     */
    private static volatile boolean emergencyMode = false;

    // ── Emergency mode ────────────────────────────────────────────────────────

    /**
     * Activates or deactivates emergency mode.  Called by {@link
     * com.zerog.neoessentials.permissions.PermissionSystem} when initialisation
     * fails so that OP players retain access instead of crashing the server.
     */
    @SuppressWarnings("unused") // called from PermissionSystem
    public static void setEmergencyMode(boolean active) {
        if (active != emergencyMode) {
            emergencyMode = active;
            if (active) {
                LOGGER.warn("╔══════════════════════════════════════════════════════════════╗");
                LOGGER.warn("║  PERMISSION SYSTEM — EMERGENCY MODE ACTIVE                   ║");
                LOGGER.warn("║  The permission system failed to initialise correctly.        ║");
                LOGGER.warn("║  ALL permission checks will be answered by vanilla OP status. ║");
                LOGGER.warn("║  Run /neoe reload once the config issue has been resolved.    ║");
                LOGGER.warn("╚══════════════════════════════════════════════════════════════╝");
            } else {
                LOGGER.info("Permission system emergency mode deactivated — normal checks resumed.");
            }
        }
    }

    /** Returns {@code true} when the system is running in emergency (OP-only) mode. */
    @SuppressWarnings("unused") // called from PermissionSystem
    public static boolean isEmergencyMode() {
        return emergencyMode;
    }

    /**
     * Set the built-in permission manager (default system).
     */
    public static void setManager(PermissionManager m) {
        manager = m;
    }

    /**
     * Set an external permission adapter (e.g., LuckPerms, FTB Ranks).
     * If set, all permission checks will be delegated to this adapter.
     */
    public static void setExternalAdapter(ExternalPermissionAdapter adapter) {
        externalAdapter = adapter;
        LOGGER.info("External permission adapter set: " + (adapter != null ? adapter.getName() : "none"));
    }

    /**
     * Returns the current external permission adapter, or null if using built-in.
     */
    @SuppressWarnings("unused") // Public API method
    public static ExternalPermissionAdapter getExternalAdapter() {
        return externalAdapter;
    }

    /**
     * Returns true if using an external permission system.
     */
    public static boolean isUsingExternal() {
        return externalAdapter != null;
    }

    public static boolean hasPermission(UUID uuid, String permission) {
        return hasPermission(uuid, permission, com.zerog.neoessentials.permissions.PermissionContext.EMPTY);
    }

    /**
     * Context-aware permission check.  The permission node is first passed through the
     * alias resolver ({@link com.zerog.neoessentials.permissions.PermissionAliasManager})
     * before the 5-step resolution chain is executed.
     *
     * @param uuid       the player's UUID (must not be null)
     * @param permission the permission node to check
     * @param context    the player's runtime context; pass
     *                   {@link com.zerog.neoessentials.permissions.PermissionContext#EMPTY}
     *                   when no context is available
     */
    public static boolean hasPermission(UUID uuid, String permission,
                                        com.zerog.neoessentials.permissions.PermissionContext context) {
        // Validate input parameters
        if (uuid == null) {
            LOGGER.warn("PermissionAPI.hasPermission: UUID is null");
            return false;
        }
        if (permission == null || permission.trim().isEmpty()) {
            LOGGER.warn("PermissionAPI.hasPermission: Permission string is null or empty");
            return false;
        }

        // ── Alias resolution ──────────────────────────────────────────────────
        // Map legacy / short alias nodes to their canonical NeoEssentials equivalents
        // before running any other check.
        try {
            permission = com.zerog.neoessentials.permissions.PermissionAliasManager
                .getInstance().resolve(permission);
        } catch (Exception e) {
            LOGGER.debug("Alias resolution failed for '{}': {}", permission, e.getMessage());
        }

        LOGGER.debug("═══ PERMISSION CHECK ═══");
        LOGGER.debug("Player UUID: {}", uuid);
        LOGGER.debug("Permission: {}", permission);
        LOGGER.debug("External adapter: {}", (externalAdapter != null ? externalAdapter.getName() : "NONE"));
        if (context != null && context != com.zerog.neoessentials.permissions.PermissionContext.EMPTY) {
            LOGGER.debug("Context: world={} time={} gamemode={}", context.worldId, context.dayTime, context.gamemode);
        }

        // ── Emergency mode — permission system failed to start ────────────────
        // Grant access immediately by OP status so admins can fix the issue.
        if (emergencyMode) {
            boolean isOp = isPlayerOpped(uuid);
            LOGGER.warn("EMERGENCY MODE active — {} for '{}' (player is OP: {})",
                    isOp ? "GRANTED" : "DENIED", permission, isOp);
            return isOp;
        }

        // ── Fast-path: OP bypass (runs BEFORE any permission system) ──────────
        // opsBypassPermissions: true means OPs skip all checks entirely.
        // Different from vanillaOpFallback (which runs AFTER all checks).
        if (com.zerog.neoessentials.config.ConfigManager.getInstance().isOpsBypassPermissionsEnabled()) {
            if (isPlayerOpped(uuid)) {
                LOGGER.debug("Player is OP - bypassing permission check (opsBypassPermissions)");
                LOGGER.debug("Result: TRUE (op bypass)");
                LOGGER.debug("═══════════════════════");
                return true;
            }
        }

        // ── External permission adapter path ──────────────────────────────────
        // Try external first. If unhealthy or throwing, fall through to internal
        // and then to the registry-default / vanilla-OP fallbacks.
        if (externalAdapter != null) {
            LOGGER.debug("Using external permission system: {}", externalAdapter.getName());
            boolean externalAvailable = externalAdapter.isAvailable() && externalAdapter.isHealthy();

            // explicitDeny caches the result of isExplicitlyDenied() so that we avoid
            // calling queryTristate a second time inside checkRegistryDefault.
            // null  = not yet determined (adapter unavailable or threw)
            // true  = adapter confirmed an intentional revocation (Tristate.FALSE)
            // false = adapter said UNDEFINED (no opinion) or TRUE
            Boolean explicitDeny = null;

            if (externalAvailable) {
                try {
                    boolean hasExternalPerm = externalAdapter.hasPermission(uuid, permission);
                    LOGGER.debug("External system returned: {}", hasExternalPerm);
                    if (hasExternalPerm) {
                        LOGGER.debug("Result: TRUE (external)");
                        LOGGER.debug("═══════════════════════");
                        return true;
                    }
                    // Not explicitly granted — check once whether it is explicitly denied.
                    // Caching the result here avoids a second queryTristate call inside
                    // checkRegistryDefault, which would double-count consecutive failures.
                    try {
                        explicitDeny = externalAdapter.isExplicitlyDenied(uuid, permission);
                    } catch (Exception ex2) {
                        LOGGER.debug("isExplicitlyDenied threw for '{}' — treating as not denied: {}",
                                permission, ex2.getMessage());
                        explicitDeny = false;
                    }
                    LOGGER.debug("External '{}': no explicit grant; explicitDeny={}", permission, explicitDeny);
                } catch (Exception ex) {
                    LOGGER.warn("External permission adapter '{}' threw during hasPermission('{}') — falling back: {}",
                            externalAdapter.getName(), permission, ex.getMessage());
                    // fall through to internal then registry-default / vanilla-OP fallback
                }
            } else {
                LOGGER.warn("External permission adapter '{}' is UNHEALTHY (failures: {}) — using internal/registry fallback",
                        externalAdapter.getName(), externalAdapter.getConsecutiveFailures());
            }

            // ── Internal-manager fallback (external failed or denied) ─────────
            if (manager != null) {
                LOGGER.debug("Using internal permission system (external adapter fallback)");
                boolean hasInternalPerm = manager.hasPermission(uuid, permission, context);
                LOGGER.debug("Internal fallback returned: {}", hasInternalPerm);
                if (hasInternalPerm) {
                    LOGGER.debug("Result: TRUE (internal fallback)");
                    LOGGER.debug("═══════════════════════");
                    return true;
                }
                // Internal also said "no" — try registry defaults before vanilla-OP fallback.
            }

            // ── Registry-default fallback ─────────────────────────────────────
            // Apply NeoEssentials documented defaults unconditionally so that users
            // are never locked out simply because LuckPerms became temporarily
            // unhealthy or has no explicit node for a permission.
            //
            // • When the adapter is healthy we already know whether it explicitly denied
            //   the node (cached in explicitDeny above) — pass that result directly to
            //   avoid a second LuckPerms queryTristate call and the double-failure-count
            //   bug that went with it.
            //
            // • When the adapter is unhealthy / unavailable (explicitDeny == null) we
            //   cannot distinguish "denied" from "unknown", so we conservatively treat
            //   the node as not explicitly denied and still honour the registry default.
            //   This is the safer choice: grant defaults rather than lock everyone out.
            boolean explicitlyDenied = Boolean.TRUE.equals(explicitDeny); // false when null (unknown) or false
            if (!explicitlyDenied) {
                boolean registryDefault = checkRegistryDefaultNoAdapterCall(permission);
                if (registryDefault) {
                    LOGGER.debug("Result: TRUE (registry default — external had no opinion or was unavailable)");
                    LOGGER.debug("═══════════════════════");
                    return true;
                }
            } else {
                LOGGER.debug("Registry default suppressed: external adapter explicitly denied '{}'", permission);
            }

            // ── Vanilla OP fallback (last resort after external+internal both failed/denied) ──
            return checkVanillaOpFallback(uuid, permission, "external+internal");
        }

        // ── Pure-internal path (no external adapter configured) ───────────────
        LOGGER.debug("Using INTERNAL permission system");
        if (manager == null) {
            LOGGER.warn("PermissionAPI.hasPermission: PermissionManager is null");
            // No manager at all — fall straight to vanilla-OP fallback
            return checkVanillaOpFallback(uuid, permission, "no-manager");
        }

        boolean hasInternalPerm = manager.hasPermission(uuid, permission, context);
        LOGGER.debug("Internal system returned: {}", hasInternalPerm);
        if (hasInternalPerm) {
            LOGGER.debug("Result: TRUE (internal)");
            LOGGER.debug("═══════════════════════");
            return true;
        }

        // ── Registry-default fallback (internal-only path) ────────────────────
        // No external adapter is active; check whether the permission has
        // defaultValue=true in the registry and grant it if so.
        boolean registryDefault = checkRegistryDefaultNoAdapterCall(permission);
        if (registryDefault) {
            LOGGER.debug("Result: TRUE (registry default — internal had no entry)");
            LOGGER.debug("═══════════════════════");
            return true;
        }

        // Internal said "no" — vanilla-OP fallback is the last resort.
        return checkVanillaOpFallback(uuid, permission, "internal");
    }

    /**
     * Registry-default fallback — <em>without</em> calling back into the external adapter.
     *
     * <p>Used by both:
     * <ul>
     *   <li>The external-adapter path in {@link #hasPermission} where the
     *       explicit-deny status is already known (cached as {@code explicitDeny}) so
     *       a second {@code queryTristate} call is unnecessary and would double-count
     *       consecutive failures, potentially flipping the adapter to "unhealthy" faster.</li>
     *   <li>The pure-internal path where no external adapter is configured at all.</li>
     * </ul>
     *
     * <p>The caller is responsible for checking explicit-deny <em>before</em>
     * calling this method and skipping it when an explicit deny is confirmed.
     *
     * @param permission the permission node to check
     * @return {@code true} when the permission is registered with {@code defaultValue=true}
     */
    private static boolean checkRegistryDefaultNoAdapterCall(String permission) {
        try {
            PermissionRegistry registry = PermissionRegistry.getInstance();
            PermissionRegistry.PermissionInfo info = registry.getPermissionInfo(permission);
            if (info == null || !info.getDefaultValue()) {
                return false;
            }
            LOGGER.debug("Registry default applies for '{}' (defaultValue=true, explicit-deny already confirmed as false)", permission);
            return true;
        } catch (Exception e) {
            LOGGER.debug("Error checking registry default (no-adapter path) for '{}': {}", permission, e.getMessage());
            return false;
        }
    }

    /**
     * Vanilla-OP last-resort fallback.
     *
     * <p>Fires after <em>all</em> permission systems have been consulted and
     * none granted the requested node.  If {@code vanillaOpFallback} is enabled
     * in config and the player holds vanilla OP status, permission is granted and
     * a {@code DEBUG} message is logged (first occurrence logged at {@code WARN}
     * to alert admins that the fallback is in use).
     *
     * @param source short label for log messages, e.g. {@code "internal"} or
     *               {@code "external+internal"}
     */
    private static boolean checkVanillaOpFallback(UUID uuid, String permission, String source) {
        if (com.zerog.neoessentials.config.ConfigManager.getInstance().isVanillaOpFallbackEnabled()) {
            if (isPlayerOpped(uuid)) {
                LOGGER.debug("Vanilla OP fallback (after {}): granting '{}' to OP {}", source, permission, uuid);
                LOGGER.debug("Result: TRUE (vanillaOpFallback)");
                LOGGER.debug("═══════════════════════");
                return true;
            }
        }
        LOGGER.debug("Result: FALSE ({} denied, no OP fallback triggered)", source);
        LOGGER.debug("═══════════════════════");
        return false;
    }


    /**
     * Checks if a player is opped by their UUID.
     */
    private static boolean isPlayerOpped(UUID uuid) {
        try {
            net.minecraft.server.MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                // Try to get the player directly and check their permission level
                net.minecraft.server.level.ServerPlayer player = server.getPlayerList().getPlayer(uuid);
                if (player != null) {
                    return com.zerog.neoessentials.util.PermissionLevelCompat.hasPermission(player, 2); // Op level 2 or higher
                }
                
                // If player is offline, check the ops file
                var profileCache = server.getProfileCache();
                if (profileCache != null) {
                    com.mojang.authlib.GameProfile profile = profileCache.get(uuid).orElse(null);
                    if (profile != null) {
                        return server.getPlayerList().isOp(profile);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Could not check op status for UUID {}: {}", uuid, e.getMessage());
        }
        return false;
    }

    public static PermissionManager getManager() {
        return manager;
    }

    public static String getPrefix(UUID uuid) {
        // Validate input parameters
        if (uuid == null) {
            LOGGER.warn("PermissionAPI.getPrefix: UUID is null");
            return "";
        }

        LOGGER.debug(">>> PermissionAPI.getPrefix() called for UUID: {}", uuid);
        LOGGER.debug(">>> Using external adapter: {}", (externalAdapter != null ? externalAdapter.getName() : "NONE"));

        // If external adapter is set, ONLY use it - do NOT fall back to internal
        if (externalAdapter != null) {
            LOGGER.debug(">>> Querying external adapter for prefix...");
            String prefix = externalAdapter.getPrefix(uuid);
            LOGGER.debug(">>> External adapter returned: [{}]", prefix);
            return prefix != null ? prefix : "";
        }

        // Only use internal system if NO external adapter is configured
        LOGGER.debug(">>> Using internal permission system (no external adapter)");

        if (manager == null) {
            LOGGER.warn("PermissionAPI.getPrefix: PermissionManager is null");
            return "";
        }
        PermissionUser user = manager.getUser(uuid);
        String groupName = (user != null && user.getGroup() != null) ? user.getGroup() : manager.getDefaultGroup();
        if (groupName == null) {
            LOGGER.warn("PermissionAPI.getPrefix: Default group name is null");
            return "";
        }
        PermissionGroup group = manager.getGroup(groupName);
        if (group == null) {
            LOGGER.warn("PermissionAPI.getPrefix: No PermissionGroup found for group '" + groupName + "'");
            return "";
        }
        String prefix = group.getPrefix();
        LOGGER.debug(">>> Internal system prefix: [{}]", prefix);
        return prefix != null ? prefix : "";
    }

    public static String getSuffix(UUID uuid) {
        // Validate input parameters
        if (uuid == null) {
            LOGGER.warn("PermissionAPI.getSuffix: UUID is null");
            return "";
        }
        
        // If external adapter is set, ONLY use it - do NOT fall back to internal
        if (externalAdapter != null) {
            String suffix = externalAdapter.getSuffix(uuid);
            // Return what external system says, even if null/empty
            // Do NOT fall back to internal when external is enabled
            return suffix != null ? suffix : "";
        }

        // Only use internal system if NO external adapter is configured
        if (manager == null) {
            LOGGER.warn("PermissionAPI.getSuffix: PermissionManager is null");
            return "";
        }
        PermissionUser user = manager.getUser(uuid);
        if (user == null) {
            LOGGER.warn("PermissionAPI.getSuffix: No PermissionUser found for UUID " + uuid);
        }
        String groupName = (user != null && user.getGroup() != null) ? user.getGroup() : manager.getDefaultGroup();
        if (groupName == null) {
            LOGGER.warn("PermissionAPI.getSuffix: Default group name is null");
            return "";
        }
        PermissionGroup group = manager.getGroup(groupName);
        if (group == null) {
            LOGGER.warn("PermissionAPI.getSuffix: No PermissionGroup found for group '" + groupName + "'");
            return "";
        }
        String suffix = group.getSuffix();
        return suffix != null ? suffix : "";
    }

    /**
     * Reloads all permissions and groups from disk at runtime.
     */
    public static void reload() throws Exception {
        if (externalAdapter != null) {
            externalAdapter.reload();
        } else if (manager != null) {
            manager.reload();
        } else {
            LOGGER.warn("PermissionAPI.reload: Both externalAdapter and manager are null - nothing to reload");
            throw new IllegalStateException("Permission system not initialized - cannot reload");
        }
    }
}