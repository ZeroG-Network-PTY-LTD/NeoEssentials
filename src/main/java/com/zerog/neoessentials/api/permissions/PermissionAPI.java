
    package com.zerog.neoessentials.api.permissions;

import java.util.UUID;
import java.util.logging.Logger;

import com.zerog.neoessentials.permissions.ExternalPermissionAdapter;
import com.zerog.neoessentials.permissions.PermissionGroup;
import com.zerog.neoessentials.permissions.PermissionManager;
import com.zerog.neoessentials.permissions.PermissionUser;

public class PermissionAPI {
    private static PermissionManager manager;
    private static ExternalPermissionAdapter externalAdapter = null;
    private static final Logger LOGGER = Logger.getLogger("NeoEssentials-Permissions");

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
        if (externalAdapter != null) {
            return externalAdapter.hasPermission(uuid, permission);
        }
        if (manager == null) return false;
        return manager.hasPermission(uuid, permission);
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
            LOGGER.warning("PermissionAPI.getPrefix: PermissionManager is null");
            return "";
        }
        PermissionUser user = manager.getUser(uuid);
        if (user == null) {
            LOGGER.warning("PermissionAPI.getPrefix: No PermissionUser found for UUID " + uuid);
        }
        String groupName = (user != null && user.getGroup() != null) ? user.getGroup() : manager.getDefaultGroup();
        PermissionGroup group = manager.getGroup(groupName);
        if (group == null) {
            LOGGER.warning("PermissionAPI.getPrefix: No PermissionGroup found for group '" + groupName + "'");
        }
        return group != null ? group.getPrefix() : "";
    }

    public static String getSuffix(UUID uuid) {
        if (externalAdapter != null) {
            String suffix = externalAdapter.getSuffix(uuid);
            if (suffix != null) return suffix;
        }
        if (manager == null) {
            LOGGER.warning("PermissionAPI.getSuffix: PermissionManager is null");
            return "";
        }
        PermissionUser user = manager.getUser(uuid);
        if (user == null) {
            LOGGER.warning("PermissionAPI.getSuffix: No PermissionUser found for UUID " + uuid);
        }
        String groupName = (user != null && user.getGroup() != null) ? user.getGroup() : manager.getDefaultGroup();
        PermissionGroup group = manager.getGroup(groupName);
        if (group == null) {
            LOGGER.warning("PermissionAPI.getSuffix: No PermissionGroup found for group '" + groupName + "'");
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