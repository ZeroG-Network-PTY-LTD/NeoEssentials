package com.zerog.neoessentials.permissions;

import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server-tick driven handler that promotes online players from one permission group to
 * another once their vanilla /playtime statistic crosses a configured threshold
 * (config.json -> permissionsAutoPromote, default: guest -> member after 24h).
 *
 * <p>Only affects the internal permission system — a no-op while an external adapter
 * (LuckPerms etc.) is active, since NeoEssentials doesn't own group assignment there.</p>
 *
 * <p>Runs every 5 minutes (6000 ticks at 20 TPS); only online players are checked, since a
 * disconnected player's playtime can't change.</p>
 */
@EventBusSubscriber(modid = "neoessentials")
public class AutoPromoteManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutoPromoteManager.class);
    private static final int CHECK_INTERVAL_TICKS = 6000;
    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (++tickCounter < CHECK_INTERVAL_TICKS) return;
        tickCounter = 0;

        if (!ConfigManager.isPermissionsAutoPromoteEnabled()) return;

        PermissionManager manager = PermissionAPI.getManager();
        if (manager == null) return;

        String fromGroup = ConfigManager.getAutoPromoteFromGroup();
        String toGroup = ConfigManager.getAutoPromoteToGroup();
        double requiredHours = ConfigManager.getAutoPromoteRequiredPlaytimeHours();

        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        boolean promotedAny = false;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            try {
                if (tryPromote(manager, player, fromGroup, toGroup, requiredHours)) {
                    promotedAny = true;
                }
            } catch (Exception e) {
                NeoLog.error(LOGGER, LogCategory.PERMISSIONS,
                    "[AutoPromote] Error checking player {}: {}", player.getName().getString(), e.getMessage(), e);
            }
        }

        if (promotedAny) {
            try {
                PermissionStorage.save(manager);
            } catch (Exception e) {
                NeoLog.error(LOGGER, LogCategory.PERMISSIONS, "[AutoPromote] Failed to save promotion(s): {}", e.getMessage(), e);
            }
        }
    }

    private static boolean tryPromote(PermissionManager manager, ServerPlayer player, String fromGroup, String toGroup, double requiredHours) {
        PermissionUser user = manager.getUser(player.getUUID());
        if (user.getGroup() == null || !user.getGroup().equalsIgnoreCase(fromGroup)) {
            return false;
        }

        int playTimeTicks = player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME));
        double hoursPlayed = playTimeTicks / 20.0 / 3600.0;
        if (hoursPlayed < requiredHours) {
            return false;
        }

        user.setGroup(toGroup);
        manager.clearCache();
        player.sendSystemMessage(MessageUtil.success("commands.neoessentials.permissions.auto_promoted",
            String.format("%.0f", requiredHours), toGroup));
        NeoLog.info(LOGGER, LogCategory.PERMISSIONS, "[AutoPromote] Promoted {} from '{}' to '{}' after {} hours played",
            player.getName().getString(), fromGroup, toGroup, String.format("%.1f", hoursPlayed));
        return true;
    }
}
