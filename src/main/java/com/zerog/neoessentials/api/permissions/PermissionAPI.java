
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

        // If using external permissions (LuckPerms, FTB Ranks), ONLY use external system
        // Do NOT check ops bypass - let the external system handle that
        if (externalAdapter != null) {
            LOGGER.debug("Using external permission system: {}", externalAdapter.getName());
            boolean hasExternalPerm = externalAdapter.hasPermission(uuid, permission);
            LOGGER.debug("External system returned: {}", hasExternalPerm);
            LOGGER.debug("═══════════════════════");
            return hasExternalPerm;
        }
        
        LOGGER.debug("Using INTERNAL permission system");

        // Only use internal system if NO external adapter is configured
        // Check ops bypass first (only for internal system)
        if (com.zerog.neoessentials.config.ConfigManager.getInstance().isOpsBypassPermissionsEnabled()) {
            if (isPlayerOpped(uuid)) {
                LOGGER.debug("Player is OP - bypassing permission check");
                LOGGER.debug("Result: TRUE (op bypass)");
                LOGGER.debug("═══════════════════════");
                return true;
            }
        }
        
        // Finally check internal permission manager
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
        boolean debugEnabled = com.zerog.neoessentials.config.ConfigManager.getInstance().isDebugLoggingEnabled();

        if (debugEnabled) {
            LOGGER.info(">>> PermissionAPI.getPrefix() called for UUID: {}", uuid);
        }

        // Validate input parameters
        if (uuid == null) {
            LOGGER.warn("PermissionAPI.getPrefix: UUID is null");
            return "";
        }
        
        if (debugEnabled) {
            LOGGER.info(">>> Using external adapter: {}", (externalAdapter != null ? externalAdapter.getName() : "NONE"));
        }

        // If external adapter is set, ONLY use it - do NOT fall back to internal
        if (externalAdapter != null) {
            if (debugEnabled) {
                LOGGER.info(">>> Querying external adapter for prefix...");
            }
            String prefix = externalAdapter.getPrefix(uuid);
            if (debugEnabled) {
                LOGGER.info(">>> External adapter returned: [{}]", prefix);
            }
            // Return what external system says, even if null/empty
            // Do NOT fall back to internal when external is enabled
            return prefix != null ? prefix : "";
        }

        // Only use internal system if NO external adapter is configured
        if (debugEnabled) {
            LOGGER.info(">>> Using internal permission system (no external adapter)");
        }

        if (manager == null) {
            LOGGER.warn("PermissionAPI.getPrefix: PermissionManager is null");
            return "";
        }
        PermissionUser user = manager.getUser(uuid);
        if (user == null) {
            LOGGER.warn("PermissionAPI.getPrefix: No PermissionUser found for UUID " + uuid);
        }
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
        if (debugEnabled) {
            LOGGER.info(">>> Internal system prefix: [{}]", prefix);
        }
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