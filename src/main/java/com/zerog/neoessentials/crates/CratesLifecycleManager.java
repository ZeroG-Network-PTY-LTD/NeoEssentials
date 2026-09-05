package com.zerog.neoessentials.crates;

import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Loads {@code crates.json} once the server (and {@code AuctionComponentSerializer}'s server
 *  reference, needed to deserialize reward ItemStacks) is fully up.
 *
 *  <p>{@code event.getServer().execute(...)} does NOT actually defer past other
 *  {@code ServerStartedEvent} listeners — called from the server thread (which we already are,
 *  mid-dispatch) it runs the task inline in the same call stack, not on a later tick. So loading
 *  here used to race {@code NeoEssentials.onServerStarted}'s
 *  {@code AuctionHouseManager.setServer(...)} call, which binds the registry access
 *  {@code AuctionComponentSerializer} needs to deserialize a reward's saved item components —
 *  whichever {@code @EventBusSubscriber} happened to fire first. When this one fired first, every
 *  crate reward with saved components failed to deserialize
 *  ("[AuctionHouse] Server not set on ComponentSerializer"), the same ordering hazard
 *  {@code KitManager.reloadKitItemsAfterServerStart()} exists to avoid. Running at
 *  {@code EventPriority.LOW} guarantees this fires after the (default-priority) listener that
 *  sets the server reference, regardless of registration order. */
@EventBusSubscriber(modid = "neoessentials")
public class CratesLifecycleManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CratesLifecycleManager.class);

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onServerStarted(ServerStartedEvent event) {
        if (!ConfigManager.isCratesModuleEnabled()) {
            NeoLog.info(LOGGER, LogCategory.CRATES, "Crates module is disabled via config, skipping.");
            return;
        }
        try {
            CrateManager.getInstance().load();
        } catch (Throwable e) {
            NeoLog.error(LOGGER, LogCategory.CRATES, "Failed to load crates.json", e);
        }
    }
}
