
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
        // Validate input parameters
        if (uuid == null) {
            LOGGER.warn("PermissionAPI.hasPermission: UUID is null");
            return false;
        }
        if (permission == null || permission.trim().isEmpty()) {
            LOGGER.warn("PermissionAPI.hasPermission: Permission string is null or empty");
            return false;
        }
        
        LOGGER.debug("═══ PERMISSION CHECK ═══");
        LOGGER.debug("Player UUID: {}", uuid);
        LOGGER.debug("Permission: {}", permission);
        LOGGER.debug("External adapter: {}", (externalAdapter != null ? externalAdapter.getName() : "NONE"));

        // Always check OP bypass first when enabled — this acts as a safety fallback
        // so server operators are never locked out even if external permissions fail.
        if (com.zerog.neoessentials.config.ConfigManager.getInstance().isOpsBypassPermissionsEnabled()) {
            if (isPlayerOpped(uuid)) {
                LOGGER.debug("Player is OP - bypassing permission check (global fallback)");
                LOGGER.debug("Result: TRUE (op bypass)");
                LOGGER.debug("═══════════════════════");
                return true;
            }
        }

        // If using external permissions (LuckPerms, FTB Ranks), try the external system first.
        // If the external adapter is unhealthy (repeated runtime failures) or throws an
        // exception, we fall through to the internal manager and, as a last resort, OP bypass.
        if (externalAdapter != null) {
            LOGGER.debug("Using external permission system: {}", externalAdapter.getName());
            boolean externalAvailable = externalAdapter.isAvailable() && externalAdapter.isHealthy();

            if (externalAvailable) {
                try {
                    boolean hasExternalPerm = externalAdapter.hasPermission(uuid, permission);
                    LOGGER.debug("External system returned: {}", hasExternalPerm);
                    LOGGER.debug("═══════════════════════");
                    return hasExternalPerm;
                } catch (Exception ex) {
                    LOGGER.warn("External permission adapter '{}' threw during hasPermission('{}') — falling back: {}",
                            externalAdapter.getName(), permission, ex.getMessage());
                    // fall through to internal manager
                }
            } else {
                LOGGER.warn("External permission adapter '{}' is UNHEALTHY (failures: {}) — using internal fallback",
                        externalAdapter.getName(), externalAdapter.getConsecutiveFailures());
                // fall through to internal manager
            }

            // ── Internal-manager fallback ─────────────────────────────────────────
            if (manager != null) {
                LOGGER.debug("Using internal permission system (external adapter fallback)");
                boolean hasInternalPerm = manager.hasPermission(uuid, permission);
                LOGGER.debug("Internal fallback returned: {}", hasInternalPerm);
                LOGGER.debug("═══════════════════════");
                return hasInternalPerm;
            }

            // ── Emergency OP fallback — last resort to prevent lockout ────────────
            LOGGER.warn("No internal manager available — checking OP status as emergency fallback for '{}'", permission);
            boolean opFallback = isPlayerOpped(uuid);
            LOGGER.debug("Emergency OP fallback: {}", opFallback);
            LOGGER.debug("═══════════════════════");
            return opFallback;
        }
        
        LOGGER.debug("Using INTERNAL permission system");
        if (manager == null) {
            LOGGER.warn("PermissionAPI.hasPermission: PermissionManager is null - returning false");
            LOGGER.debug("Result: FALSE (no manager)");
            LOGGER.debug("═══════════════════════");
            return false;
        }

        boolean hasInternalPerm = manager.hasPermission(uuid, permission);
        LOGGER.debug("Internal system returned: {}", hasInternalPerm);
        LOGGER.debug("Result: {}", hasInternalPerm);
        LOGGER.debug("═══════════════════════");
        return hasInternalPerm;
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
                    return player.hasPermissions(2); // Op level 2 or higher
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