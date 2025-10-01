package com.zerog.neoessentials.permissions;

import java.util.UUID;
import net.neoforged.fml.ModList;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;

/**
 * Stub implementation for LuckPerms integration.
 * Replace with real LuckPerms API calls if available.
 */
public class LuckPermsAdapter implements ExternalPermissionAdapter {
    private final boolean luckPermsLoaded;
    private LuckPerms luckPermsApi;

    public LuckPermsAdapter() {
        this.luckPermsLoaded = ModList.get().isLoaded("luckperms");
        if (luckPermsLoaded) {
            try {
                this.luckPermsApi = LuckPermsProvider.get();
            } catch (Exception e) {
                this.luckPermsApi = null;
            }
        }
    }

    @Override
    public boolean hasPermission(UUID uuid, String permission) {
        if (luckPermsLoaded && luckPermsApi != null) {
            User user = luckPermsApi.getUserManager().getUser(uuid);
            if (user != null) {
                return user.getCachedData().getPermissionData(QueryOptions.defaultContextualOptions()).checkPermission(permission).asBoolean();
            }
            return false;
        }
        return ConfigPermissionUtil.hasPermission(uuid, permission);
    }

    @Override
    public String getPrefix(UUID uuid) {
        if (luckPermsLoaded && luckPermsApi != null) {
            User user = luckPermsApi.getUserManager().getUser(uuid);
            if (user != null) {
                return user.getCachedData().getMetaData(QueryOptions.defaultContextualOptions()).getPrefix();
            }
            return null;
        }
        return ConfigPermissionUtil.getPrefix(uuid);
    }

    @Override
    public String getSuffix(UUID uuid) {
        if (luckPermsLoaded && luckPermsApi != null) {
            User user = luckPermsApi.getUserManager().getUser(uuid);
            if (user != null) {
                return user.getCachedData().getMetaData(QueryOptions.defaultContextualOptions()).getSuffix();
            }
            return null;
        }
        return ConfigPermissionUtil.getSuffix(uuid);
    }

    @Override
    public void reload() {
        if (!luckPermsLoaded) {
            ConfigPermissionUtil.reload();
        }
    }

    @Override
    public String getName() {
        return "LuckPerms";
    }
}
