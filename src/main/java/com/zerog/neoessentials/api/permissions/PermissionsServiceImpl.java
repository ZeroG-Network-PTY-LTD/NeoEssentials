package com.zerog.neoessentials.api.permissions;

import com.zerog.neoessentials.permissions.PermissionAliasManager;
import com.zerog.neoessentials.permissions.PermissionContext;
import com.zerog.neoessentials.permissions.PermissionGroup;
import com.zerog.neoessentials.permissions.PermissionUser;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;

import java.util.*;

/**
 * Default implementation of {@link PermissionsService} that delegates to the
 * NeoEssentials internal {@link PermissionAPI} and supporting managers.
 *
 * <p>Obtained via {@code NeoEssentialsAPI.getPermissionsService()}.
 */
public class PermissionsServiceImpl implements PermissionsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionsServiceImpl.class);
    private static final PermissionsServiceImpl INSTANCE = new PermissionsServiceImpl();

    private PermissionsServiceImpl() {}

    public static PermissionsServiceImpl getInstance() {
        return INSTANCE;
    }

    // ── Permission checks ─────────────────────────────────────────────────────

    @Override
    public boolean hasPermission(UUID playerUuid, String permissionNode) {
        return PermissionAPI.hasPermission(playerUuid, permissionNode);
    }

    @Override
    public boolean hasPermission(UUID playerUuid, String permissionNode, PermissionContext context) {
        return PermissionAPI.hasPermission(playerUuid, permissionNode, context);
    }

    @Override
    public boolean hasPermission(ServerPlayer player, String permissionNode) {
        return PermissionAPI.hasPermission(player.getUUID(), permissionNode,
            PermissionContext.forPlayer(player));
    }

    // ── Player meta ───────────────────────────────────────────────────────────

    @Override
    public String getGroup(UUID playerUuid) {
        try {
            var mgr = PermissionAPI.getManager();
            if (mgr == null) return "";
            PermissionUser user = mgr.getUser(playerUuid);
            return user != null ? user.getGroup() : mgr.getDefaultGroup();
        } catch (Exception e) {
            NeoLog.debug(LOGGER, LogCategory.PERMISSIONS, "getGroup failed for {}: {}", playerUuid, e.getMessage());
            return "";
        }
    }

    @Override
    public String getPrefix(UUID playerUuid) {
        return PermissionAPI.getPrefix(playerUuid);
    }

    @Override
    public String getSuffix(UUID playerUuid) {
        return PermissionAPI.getSuffix(playerUuid);
    }

    // ── Permission node registration ─────────────────────────────────────────

    @Override
    public void registerPermission(String node, String description) {
        if (node == null || node.isBlank()) return;
        try {
            PermissionRegistry.getInstance().register(
                node.toLowerCase().trim(),
                description != null ? description : "",
                PermissionRegistry.PermissionCategory.MISC,
                false);
            NeoLog.debug(LOGGER, LogCategory.PERMISSIONS, "External permission registered: {}", node);
        } catch (Exception e) {
            LOGGER.warn("Failed to register external permission '{}': {}", node, e.getMessage());
        }
    }

    @Override
    public void registerPermissions(java.util.Map<String, String> permissions) {
        if (permissions == null) return;
        permissions.forEach(this::registerPermission);
    }

    // ── Alias management ─────────────────────────────────────────────────────

    @Override
    public void registerAlias(String alias, String canonical) {
        PermissionAliasManager.getInstance().addAlias(alias, canonical);
        NeoLog.debug(LOGGER, LogCategory.PERMISSIONS, "Permission alias registered: {} -> {}", alias, canonical);
    }

    @Override
    public Map<String, String> getAliases() {
        return PermissionAliasManager.getInstance().getAll();
    }

    // ── System information ────────────────────────────────────────────────────

    @Override
    public boolean isEmergencyMode() {
        return PermissionAPI.isEmergencyMode();
    }

    @Override
    public boolean isUsingExternalAdapter() {
        return PermissionAPI.isUsingExternal();
    }

    @Override
    public Collection<String> getGroupNames() {
        try {
            var mgr = PermissionAPI.getManager();
            if (mgr == null) return Collections.emptyList();
            return mgr.getGroups().stream()
                .map(PermissionGroup::getName)
                .sorted()
                .toList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Override
    public Set<String> getPlayerPermissions(UUID playerUuid) {
        try {
            var mgr = PermissionAPI.getManager();
            if (mgr == null) return Collections.emptySet();
            PermissionUser user = mgr.getUser(playerUuid);
            return user != null
                ? Collections.unmodifiableSet(user.getPermissions())
                : Collections.emptySet();
        } catch (Exception e) {
            return Collections.emptySet();
        }
    }

    // ── Context helpers ───────────────────────────────────────────────────────

    @Override
    public PermissionContext contextFor(ServerPlayer player) {
        return PermissionContext.forPlayer(player);
    }
}

