package com.zerog.neoessentials.vault.impl;

import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.permissions.PermissionGroup;
import com.zerog.neoessentials.permissions.PermissionManager;
import com.zerog.neoessentials.permissions.PermissionStorage;
import com.zerog.neoessentials.permissions.PermissionUser;
import com.zerog.neoessentials.vault.api.VaultChat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * NeoEssentials built-in {@link VaultChat} implementation.
 * Player prefix/suffix is stored on {@link PermissionUser} metadata,
 * and group prefix/suffix is stored on {@link PermissionGroup} directly.
 */
public class NeoEssentialsChat extends VaultChat {

    private static final Logger LOGGER = LoggerFactory.getLogger(NeoEssentialsChat.class);

    @Override public String getName()    { return "NeoEssentials Chat"; }
    @Override public boolean isEnabled() { return true; }

    // ── Player prefix/suffix ──────────────────────────────────────────────────

    @Override
    public String getPlayerPrefix(String world, UUID playerId) {
        try {
            PermissionManager pm = PermissionAPI.getManager();
            if (pm == null) return "";
            PermissionUser user = pm.getUser(playerId);
            // Fall back to the player's primary group prefix if no per-user prefix is set
            String userPrefix = user.getPrefix();
            if (userPrefix != null && !userPrefix.isEmpty()) return userPrefix;
            return getGroupPrefixForPlayer(pm, user);
        } catch (Exception e) {
            LOGGER.debug("VaultChat: getPlayerPrefix error: {}", e.getMessage());
            return "";
        }
    }

    @Override
    public void setPlayerPrefix(String world, UUID playerId, String prefix) {
        try {
            PermissionManager pm = PermissionAPI.getManager();
            if (pm == null) return;
            PermissionUser user = pm.getUser(playerId);
            user.setPrefix(prefix);
            PermissionStorage.save(pm);
        } catch (Exception e) {
            LOGGER.error("VaultChat: setPlayerPrefix error: {}", e.getMessage());
        }
    }

    @Override
    public String getPlayerSuffix(String world, UUID playerId) {
        try {
            PermissionManager pm = PermissionAPI.getManager();
            if (pm == null) return "";
            PermissionUser user = pm.getUser(playerId);
            String userSuffix = user.getSuffix();
            if (userSuffix != null && !userSuffix.isEmpty()) return userSuffix;
            return getGroupSuffixForPlayer(pm, user);
        } catch (Exception e) {
            LOGGER.debug("VaultChat: getPlayerSuffix error: {}", e.getMessage());
            return "";
        }
    }

    @Override
    public void setPlayerSuffix(String world, UUID playerId, String suffix) {
        try {
            PermissionManager pm = PermissionAPI.getManager();
            if (pm == null) return;
            PermissionUser user = pm.getUser(playerId);
            user.setSuffix(suffix);
            PermissionStorage.save(pm);
        } catch (Exception e) {
            LOGGER.error("VaultChat: setPlayerSuffix error: {}", e.getMessage());
        }
    }

    // ── Group prefix/suffix ───────────────────────────────────────────────────

    @Override
    public String getGroupPrefix(String world, String group) {
        try {
            PermissionManager pm = PermissionAPI.getManager();
            if (pm == null) return "";
            PermissionGroup grp = pm.getGroup(group);
            return grp != null && grp.getPrefix() != null ? grp.getPrefix() : "";
        } catch (Exception e) {
            LOGGER.debug("VaultChat: getGroupPrefix error: {}", e.getMessage());
            return "";
        }
    }

    @Override
    public void setGroupPrefix(String world, String group, String prefix) {
        try {
            PermissionManager pm = PermissionAPI.getManager();
            if (pm == null) return;
            PermissionGroup grp = pm.getGroup(group);
            if (grp == null) {
                grp = new PermissionGroup(group);
                pm.addGroup(grp);
            }
            grp.setPrefix(prefix);
            PermissionStorage.save(pm);
        } catch (Exception e) {
            LOGGER.error("VaultChat: setGroupPrefix error: {}", e.getMessage());
        }
    }

    @Override
    public String getGroupSuffix(String world, String group) {
        try {
            PermissionManager pm = PermissionAPI.getManager();
            if (pm == null) return "";
            PermissionGroup grp = pm.getGroup(group);
            return grp != null && grp.getSuffix() != null ? grp.getSuffix() : "";
        } catch (Exception e) {
            LOGGER.debug("VaultChat: getGroupSuffix error: {}", e.getMessage());
            return "";
        }
    }

    @Override
    public void setGroupSuffix(String world, String group, String suffix) {
        try {
            PermissionManager pm = PermissionAPI.getManager();
            if (pm == null) return;
            PermissionGroup grp = pm.getGroup(group);
            if (grp == null) {
                grp = new PermissionGroup(group);
                pm.addGroup(grp);
            }
            grp.setSuffix(suffix);
            PermissionStorage.save(pm);
        } catch (Exception e) {
            LOGGER.error("VaultChat: setGroupSuffix error: {}", e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String getGroupPrefixForPlayer(PermissionManager pm, PermissionUser user) {
        String groupName = user.getGroup();
        if (groupName == null) return "";
        PermissionGroup grp = pm.getGroup(groupName);
        return (grp != null && grp.getPrefix() != null) ? grp.getPrefix() : "";
    }

    private String getGroupSuffixForPlayer(PermissionManager pm, PermissionUser user) {
        String groupName = user.getGroup();
        if (groupName == null) return "";
        PermissionGroup grp = pm.getGroup(groupName);
        return (grp != null && grp.getSuffix() != null) ? grp.getSuffix() : "";
    }
}

