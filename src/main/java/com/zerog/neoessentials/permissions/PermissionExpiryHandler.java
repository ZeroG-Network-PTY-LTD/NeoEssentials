package com.zerog.neoessentials.permissions;

import com.zerog.neoessentials.api.permissions.PermissionAPI;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server-tick driven handler that periodically purges expired temporary permissions
 * from all users and groups, notifies affected online players, and writes the
 * expiry to the audit log.
 *
 * <p>Runs every 30 seconds (600 ticks at 20 TPS).
 */
@EventBusSubscriber(modid = "neoessentials")
public class PermissionExpiryHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionExpiryHandler.class);
    /** Check interval: 600 ticks = 30 seconds at 20 TPS. */
    private static final int CHECK_INTERVAL_TICKS = 600;
    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (++tickCounter < CHECK_INTERVAL_TICKS) return;
        tickCounter = 0;

        PermissionManager manager = PermissionAPI.getManager();
        if (manager == null) return;

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        try {
            manager.purgeExpiredTempPermissions(server);
        } catch (Exception e) {
            LOGGER.warn("[TempPerms] Error during expiry purge: {}", e.getMessage());
        }
    }
}

