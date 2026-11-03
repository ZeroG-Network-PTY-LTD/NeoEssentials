package com.zerog.neoessentials.webdashboard.security;

import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Keeps a linked player's dashboard role ({@link User.Role}) in sync with their actual in-game
 * permission/group status — opt-in via {@code webDashboard.roleSync.*}
 * ({@link ConfigManager#isRoleSyncEnabled()}). Grants {@code ADMIN} when a linked player has the
 * configured permission node or belongs to the configured group; revokes it again once they no
 * longer do, unless the current role was set manually (see {@link User#isRoleSyncManaged()}) —
 * this task only ever adjusts a role it granted itself, never an admin's deliberate override.
 *
 * <p>Only ever operates on players who already have a dashboard account with a linked Minecraft
 * UUID ({@link User#getMcUuid()}) — never auto-creates a dashboard account for someone who
 * hasn't opted in. Originally this only covered accounts registered via {@code /dashboardregister}
 * ({@link DashboardRegistrationManager}), missing any account that got its {@code mcUuid} some
 * other way (login-time auto-registration, or the Settings page's "link Minecraft account" flow
 * on an account that was never run through {@code /dashboardregister} at all) — those never had
 * their role synced, ever. Scoped to {@link AuthenticationManager#getAllUsers()} directly now,
 * which covers every real account regardless of how it came to have an {@code mcUuid}.
 *
 * <p>Runs a periodic sweep (self-heals any permission change made outside this server, e.g. a
 * raw LuckPerms edit) plus an immediate check on player join, since {@link PermissionAPI} has no
 * change-event hooks to react to directly.
 */
@EventBusSubscriber(modid = "neoessentials")
public class PermissionRoleSyncTask {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionRoleSyncTask.class);

    private static ScheduledExecutorService scheduler;

    private PermissionRoleSyncTask() {}

    /** Starts the periodic sweep. No-op if role sync is disabled in config or already running. */
    public static synchronized void start() {
        if (!ConfigManager.isRoleSyncEnabled()) return;
        if (scheduler != null && !scheduler.isShutdown()) return;

        int intervalSeconds = ConfigManager.getRoleSyncIntervalSeconds();
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "NeoEssentials-RoleSync");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(PermissionRoleSyncTask::syncAll, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
        NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "Permission-driven dashboard role sync started (every {}s)", intervalSeconds);
    }

    public static synchronized void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!ConfigManager.isRoleSyncEnabled()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        UUID uuid = player.getUUID();
        // Shared bounded pool, not a fresh Thread per join — see DelayedTaskExecutor.
        com.zerog.neoessentials.util.DelayedTaskExecutor.schedule(() -> syncPlayer(uuid), 0);
    }

    private static void syncAll() {
        try {
            for (User user : AuthenticationManager.getInstance().getAllUsers()) {
                String mcUuid = user.getMcUuid();
                if (mcUuid == null || mcUuid.isBlank()) continue;
                try {
                    syncPlayer(UUID.fromString(mcUuid));
                } catch (IllegalArgumentException malformed) {
                    NeoLog.warn(LOGGER, LogCategory.WEB_DASHBOARD, "Skipping role sync for '{}': malformed mcUuid '{}'", user.getUsername(), mcUuid);
                }
            }
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "Permission role-sync sweep failed", e);
        }
    }

    private static void syncPlayer(UUID minecraftUuid) {
        try {
            User user = AuthenticationManager.getInstance().getUserByMcUuid(minecraftUuid.toString());
            if (user == null) return; // Not linked to a dashboard account — nothing to sync.

            User.Role currentRole = user.getRole();

            if (isInGameAdmin(minecraftUuid)) {
                if (currentRole != User.Role.ADMIN) {
                    AuthenticationManager.getInstance().updateUserRole(user.getId(), User.Role.ADMIN, true);
                    NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "Role sync: granted ADMIN to dashboard user '{}' (in-game permission match)", user.getUsername());
                }
                return;
            }

            // Not admin-matched — check webDashboard.groupPermissions next, same unconditional-
            // grant-on-match philosophy as the ADMIN branch above (a real in-game group match is
            // always reflected, regardless of what the account's role happened to be before).
            java.util.Set<String> matchedTabPermissions = matchGroupPermissions(minecraftUuid);
            if (matchedTabPermissions != null) {
                boolean upToDate = currentRole == User.Role.CUSTOM && user.getPermissions().equals(matchedTabPermissions);
                if (!upToDate) {
                    AuthenticationManager.getInstance().updateUserRoleAndPermissions(user.getId(), matchedTabPermissions, true);
                    NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "Role sync: set CUSTOM tab permissions for dashboard user '{}' ({} tab grant(s), in-game group match)", user.getUsername(), matchedTabPermissions.size());
                }
                return;
            }

            // No admin match, no group match — only revert a role this task granted itself,
            // never a manually-set ADMIN/CUSTOM.
            if ((currentRole == User.Role.ADMIN || currentRole == User.Role.CUSTOM) && user.isRoleSyncManaged()) {
                AuthenticationManager.getInstance().updateUserRole(user.getId(), User.Role.VIEWER, true);
                NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "Role sync: reverted dashboard user '{}' to VIEWER (no longer matches any configured in-game group)", user.getUsername());
            }
        } catch (Exception e) {
            NeoLog.warn(LOGGER, LogCategory.WEB_DASHBOARD, "Permission role-sync failed for {}: {}", minecraftUuid, e.getMessage());
        }
    }

    /** @return the tab-permission strings for the first {@code webDashboard.groupPermissions}
     *          entry whose group matches this player's in-game primary group, or null if none
     *          match (including when no mappings are configured at all). */
    private static java.util.Set<String> matchGroupPermissions(UUID uuid) {
        String primaryGroup = PermissionAPI.getPrimaryGroup(uuid);
        if (primaryGroup == null || primaryGroup.isBlank()) return null;

        for (GroupPermissionMapping mapping : ConfigManager.getGroupPermissionMappings()) {
            if (primaryGroup.equalsIgnoreCase(mapping.getGroup())) {
                java.util.Set<String> perms = new java.util.HashSet<>();
                for (GroupPermissionMapping.TabGrant grant : mapping.getTabs()) {
                    perms.add(grant.toPermissionString());
                }
                return perms;
            }
        }
        return null;
    }

    private static boolean isInGameAdmin(UUID uuid) {
        String adminPermission = ConfigManager.getRoleSyncAdminPermission();
        if (adminPermission != null && !adminPermission.isBlank() && PermissionAPI.hasPermission(uuid, adminPermission)) {
            return true;
        }

        String adminGroup = ConfigManager.getRoleSyncAdminGroup();
        if (adminGroup != null && !adminGroup.isBlank()) {
            String primaryGroup = PermissionAPI.getPrimaryGroup(uuid);
            if (adminGroup.equalsIgnoreCase(primaryGroup)) return true;
        }

        return false;
    }
}
