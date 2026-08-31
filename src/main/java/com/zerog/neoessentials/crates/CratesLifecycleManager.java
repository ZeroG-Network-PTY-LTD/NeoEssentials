package com.zerog.neoessentials.crates;

import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Loads {@code crates.json} once the server (and {@code AuctionComponentSerializer}'s server
 *  reference, needed to deserialize reward ItemStacks) is fully up. Deferred one extra tick via
 *  {@code server.execute()} so it runs after every other {@code ServerStartedEvent} listener —
 *  same ordering hazard {@code KitManager.reloadKitItemsAfterServerStart()} exists to avoid. */
@EventBusSubscriber(modid = "neoessentials")
public class CratesLifecycleManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CratesLifecycleManager.class);

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        if (!ConfigManager.isCratesModuleEnabled()) {
            NeoLog.info(LOGGER, LogCategory.CRATES, "Crates module is disabled via config, skipping.");
            return;
        }
        event.getServer().execute(() -> {
            try {
                CrateManager.getInstance().load();
            } catch (Throwable e) {
                NeoLog.error(LOGGER, LogCategory.CRATES, "Failed to load crates.json", e);
            }
        });
    }
}
