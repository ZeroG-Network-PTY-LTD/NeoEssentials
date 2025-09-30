package com.zerog.neoessentials.permissions;

import java.util.UUID;

/**
 * Stub implementation for LuckPerms integration.
 * Replace with real LuckPerms API calls if available.
 */
public class LuckPermsAdapter implements ExternalPermissionAdapter {
    @Override
    public boolean hasPermission(UUID uuid, String permission) {
        // TODO: Integrate with LuckPerms API
        return false;
    }

    @Override
    public String getPrefix(UUID uuid) {
        // TODO: Integrate with LuckPerms API
        return null;
    }

    @Override
    public String getSuffix(UUID uuid) {
        // TODO: Integrate with LuckPerms API
        return null;
    }

    @Override
    public void reload() {
        // TODO: Integrate with LuckPerms API
    }

    @Override
    public String getName() {
        return "LuckPerms";
    }
}
