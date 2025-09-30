package com.zerog.neoessentials.permissions;

import java.util.UUID;

/**
 * Stub implementation for FTB Ranks integration.
 * Replace with real FTB Ranks API calls if available.
 */
public class FtbRanksAdapter implements ExternalPermissionAdapter {
    @Override
    public boolean hasPermission(UUID uuid, String permission) {
        // TODO: Integrate with FTB Ranks API
        return false;
    }

    @Override
    public String getPrefix(UUID uuid) {
        // TODO: Integrate with FTB Ranks API
        return null;
    }

    @Override
    public String getSuffix(UUID uuid) {
        // TODO: Integrate with FTB Ranks API
        return null;
    }

    @Override
    public void reload() {
        // TODO: Integrate with FTB Ranks API
    }

    @Override
    public String getName() {
        return "FTB Ranks";
    }
}
