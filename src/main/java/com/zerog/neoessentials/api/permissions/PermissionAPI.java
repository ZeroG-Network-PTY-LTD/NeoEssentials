
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
        // First check if player is opped (opped players have all permissions)
        if (uuid != null && isPlayerOpped(uuid)) {
            return true;
        }
        
        if (externalAdapter != null) {
            return externalAdapter.hasPermission(uuid, permission);
        }
        if (manager == null) return false;
        return manager.hasPermission(uuid, permission);
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
                com.mojang.authlib.GameProfile profile = server.getProfileCache().get(uuid).orElse(null);
                if (profile != null) {
                    return server.getPlayerList().isOp(profile);
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
        if (externalAdapter != null) {
            String prefix = externalAdapter.getPrefix(uuid);
            if (prefix != null) return prefix;
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
        PermissionGroup group = manager.getGroup(groupName);
        if (group == null) {
            LOGGER.warn("PermissionAPI.getPrefix: No PermissionGroup found for group '" + groupName + "'");
        }
        return group != null ? group.getPrefix() : "";
    }

    public static String getSuffix(UUID uuid) {
        if (externalAdapter != null) {
            String suffix = externalAdapter.getSuffix(uuid);
            if (suffix != null) return suffix;
        }
        if (manager == null) {
            LOGGER.warn("PermissionAPI.getSuffix: PermissionManager is null");
            return "";
        }
        PermissionUser user = manager.getUser(uuid);
        if (user == null) {
            LOGGER.warn("PermissionAPI.getSuffix: No PermissionUser found for UUID " + uuid);
        }
        String groupName = (user != null && user.getGroup() != null) ? user.getGroup() : manager.getDefaultGroup();
        PermissionGroup group = manager.getGroup(groupName);
        if (group == null) {
            LOGGER.warn("PermissionAPI.getSuffix: No PermissionGroup found for group '" + groupName + "'");
        }
        return group != null ? group.getSuffix() : "";
    }

    /**
     * Reloads all permissions and groups from disk at runtime.
     */
    public static void reload() throws Exception {
        if (externalAdapter != null) {
            externalAdapter.reload();
        } else if (manager != null) {
            manager.reload();
        }
    }
}