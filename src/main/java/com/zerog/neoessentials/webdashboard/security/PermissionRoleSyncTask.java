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
 * <p>Only ever operates on players who already linked a dashboard account via
 * {@code /dashboardregister} ({@link DashboardRegistrationManager#getAllRegistrations()}) —
 * never auto-creates a dashboard account for someone who hasn't opted in.
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
            for (DashboardAccountRegistration registration : DashboardRegistrationManager.getInstance().getAllRegistrations()) {
                syncPlayer(registration.getMinecraftUuid());
            }
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "Permission role-sync sweep failed", e);
        }
    }

    private static void syncPlayer(UUID minecraftUuid) {
        try {
            DashboardAccountRegistration registration = DashboardRegistrationManager.getInstance().getRegistration(minecraftUuid);
            if (registration == null) return; // Not linked to a dashboard account — nothing to sync.

            User user = AuthenticationManager.getInstance().getUserByUsername(registration.getDashboardUsername());
            if (user == null) return;

            boolean shouldBeAdmin = isInGameAdmin(minecraftUuid);
            User.Role currentRole = user.getRole();

            if (shouldBeAdmin && currentRole != User.Role.ADMIN) {
                AuthenticationManager.getInstance().updateUserRole(user.getId(), User.Role.ADMIN, true);
                NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "Role sync: granted ADMIN to dashboard user '{}' (in-game permission match)", user.getUsername());
            } else if (!shouldBeAdmin && currentRole == User.Role.ADMIN && user.isRoleSyncManaged()) {
                // Only downgrade a role this task granted itself — never a manually-set ADMIN.
                AuthenticationManager.getInstance().updateUserRole(user.getId(), User.Role.VIEWER, true);
                NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "Role sync: revoked ADMIN from dashboard user '{}' (no longer matches in-game permission)", user.getUsername());
            }
        } catch (Exception e) {
            NeoLog.warn(LOGGER, LogCategory.WEB_DASHBOARD, "Permission role-sync failed for {}: {}", minecraftUuid, e.getMessage());
        }
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
