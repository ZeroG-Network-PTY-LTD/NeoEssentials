package com.zerog.neoessentials.vault.impl;

import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.permissions.PermissionGroup;
import com.zerog.neoessentials.permissions.PermissionManager;
import com.zerog.neoessentials.permissions.PermissionStorage;
import com.zerog.neoessentials.permissions.PermissionUser;
import com.zerog.neoessentials.vault.api.VaultPermission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * NeoEssentials built-in {@link VaultPermission} implementation.
 * Delegates player permission checks to {@link PermissionAPI} and
 * group/user operations to the internal {@link PermissionManager}.
 */
public class NeoEssentialsPermission extends VaultPermission {

    private static final Logger LOGGER = LoggerFactory.getLogger(NeoEssentialsPermission.class);

    @Override public String getName()        { return "NeoEssentials Permissions"; }
    @Override public boolean isEnabled()     { return true; }
    @Override public boolean supportsWorlds(){ return false; }

    // ── Player permissions ────────────────────────────────────────────────────

    @Override
    public boolean playerHas(String world, UUID playerId, String permission) {
        try {
            return PermissionAPI.hasPermission(playerId, permission);
        } catch (Exception e) {
            LOGGER.error("VaultPermission: playerHas error: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean playerAdd(String world, UUID playerId, String permission) {
        try {
            PermissionManager pm = PermissionAPI.getManager();
            if (pm == null) return false;
            PermissionUser user = pm.getUser(playerId);
            user.addPermission(permission);
            PermissionStorage.save(pm);
            return true;
        } catch (Exception e) {
            LOGGER.error("VaultPermission: playerAdd error: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean playerRemove(String world, UUID playerId, String permission) {
        try {
            PermissionManager pm = PermissionAPI.getManager();
            if (pm == null) return false;
            PermissionUser user = pm.getUser(playerId);
            user.removePermission(permission);
            PermissionStorage.save(pm);
            return true;
        } catch (Exception e) {
            LOGGER.error("VaultPermission: playerRemove error: {}", e.getMessage());
            return false;
        }
    }

    // ── Groups ────────────────────────────────────────────────────────────────

    @Override public boolean hasGroupSupport() { return true; }

    @Override
    public boolean groupHas(String world, String group, String permission) {
        try {
            PermissionManager pm = PermissionAPI.getManager();
            if (pm == null) return false;
            PermissionGroup grp = pm.getGroup(group);
            return grp != null && grp.getPermissions().contains(permission.toLowerCase());
        } catch (Exception e) {
            LOGGER.error("VaultPermission: groupHas error: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean groupAdd(String world, String group, String permission) {
        try {
            PermissionManager pm = PermissionAPI.getManager();
            if (pm == null) return false;
            PermissionGroup grp = pm.getGroup(group);
            if (grp == null) {
                grp = new PermissionGroup(group);
                pm.addGroup(grp);
            }
            grp.addPermission(permission);
            PermissionStorage.save(pm);
            return true;
        } catch (Exception e) {
            LOGGER.error("VaultPermission: groupAdd error: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean groupRemove(String world, String group, String permission) {
        try {
            PermissionManager pm = PermissionAPI.getManager();
            if (pm == null) return false;
            PermissionGroup grp = pm.getGroup(group);
            if (grp == null) return false;
            grp.removePermission(permission);
            PermissionStorage.save(pm);
            return true;
        } catch (Exception e) {
            LOGGER.error("VaultPermission: groupRemove error: {}", e.getMessage());
            return false;
        }
    }

    /** NeoEssentials PermissionUser has a single primary group (setGroup/getGroup). */
    @Override
    public boolean playerInGroup(String world, UUID playerId, String group) {
        try {
            PermissionManager pm = PermissionAPI.getManager();
            if (pm == null) return false;
            PermissionUser user = pm.getUser(playerId);
            return group.equalsIgnoreCase(user.getGroup());
        } catch (Exception e) {
            LOGGER.error("VaultPermission: playerInGroup error: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean playerAddGroup(String world, UUID playerId, String group) {
        try {
            PermissionManager pm = PermissionAPI.getManager();
            if (pm == null) return false;
            PermissionUser user = pm.getUser(playerId);
            user.setGroup(group);               // single-group model — sets primary group
            PermissionStorage.save(pm);
            return true;
        } catch (Exception e) {
            LOGGER.error("VaultPermission: playerAddGroup error: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean playerRemoveGroup(String world, UUID playerId, String group) {
        try {
            PermissionManager pm = PermissionAPI.getManager();
            if (pm == null) return false;
            PermissionUser user = pm.getUser(playerId);
            if (group.equalsIgnoreCase(user.getGroup())) {
                user.setGroup(pm.getDefaultGroup());
                PermissionStorage.save(pm);
            }
            return true;
        } catch (Exception e) {
            LOGGER.error("VaultPermission: playerRemoveGroup error: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String[] getPlayerGroups(String world, UUID playerId) {
        try {
            PermissionManager pm = PermissionAPI.getManager();
            if (pm == null) return new String[0];
            PermissionUser user = pm.getUser(playerId);
            String g = user.getGroup();
            return g != null ? new String[]{g} : new String[0];
        } catch (Exception e) {
            LOGGER.error("VaultPermission: getPlayerGroups error: {}", e.getMessage());
            return new String[0];
        }
    }

    @Override
    public String getPrimaryGroup(String world, UUID playerId) {
        try {
            PermissionManager pm = PermissionAPI.getManager();
            if (pm == null) return null;
            return pm.getUser(playerId).getGroup();
        } catch (Exception e) {
            LOGGER.error("VaultPermission: getPrimaryGroup error: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public String[] getGroups() {
        try {
            PermissionManager pm = PermissionAPI.getManager();
            if (pm == null) return new String[0];
            return pm.getGroups().stream()
                .map(PermissionGroup::getName)
                .toArray(String[]::new);
        } catch (Exception e) {
            LOGGER.error("VaultPermission: getGroups error: {}", e.getMessage());
            return new String[0];
        }
    }
}

