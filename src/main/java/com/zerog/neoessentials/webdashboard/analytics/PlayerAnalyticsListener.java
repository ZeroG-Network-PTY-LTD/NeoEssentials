package com.zerog.neoessentials.webdashboard.analytics;

import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Event listener for tracking player analytics
 * Listens to player join/leave events and updates session tracker
 */
@EventBusSubscriber(modid = "neoessentials")
public class PlayerAnalyticsListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerAnalyticsListener.class);
    
    /**
     * Track player join events
     */
    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        try {
            var player = event.getEntity();
            PlayerSessionTracker tracker = PlayerSessionTracker.getInstance();
            tracker.trackPlayerJoin(player.getUUID(), player.getGameProfile().name());
            
            NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Analytics: Player joined - {} ({})",
                player.getGameProfile().name(), player.getUUID());
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "Error tracking player join event", e);
        }
    }
    
    /**
     * Track player leave events
     */
    @SubscribeEvent
    public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        try {
            var player = event.getEntity();
            PlayerSessionTracker tracker = PlayerSessionTracker.getInstance();
            tracker.trackPlayerLeave(player.getUUID());
            
            NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Analytics: Player left - {} ({})",
                player.getGameProfile().name(), player.getUUID());
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "Error tracking player leave event", e);
        }
    }
}
