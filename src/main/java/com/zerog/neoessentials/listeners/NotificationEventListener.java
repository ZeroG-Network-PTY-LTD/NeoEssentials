package com.zerog.neoessentials.listeners;

import com.zerog.neoessentials.notifications.NotificationManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Event listener for notification system integration
 * Automatically sends notifications for various server events
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class NotificationEventListener {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationEventListener.class);
    private static NotificationEventListener instance;
    
    private NotificationEventListener() {
        NeoForge.EVENT_BUS.register(this);
        LOGGER.info("Notification event listener registered");
    }
    
    public static synchronized NotificationEventListener getInstance() {
        if (instance == null) {
            instance = new NotificationEventListener();
        }
        return instance;
    }
    
    /**
     * Player join event
     */
    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            try {
                NotificationManager.getInstance().notifyPlayerJoin(player);
            } catch (Exception e) {
                LOGGER.debug("Notification manager not available for player join event", e);
            }
        }
    }
    
    /**
     * Player leave event
     */
    @SubscribeEvent
    public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            try {
                NotificationManager.getInstance().notifyPlayerLeave(player);
            } catch (Exception e) {
                LOGGER.debug("Notification manager not available for player leave event", e);
            }
        }
    }
    
    /**
     * Server shutdown event
     */
    @SubscribeEvent
    public void onServerStop(ServerStoppedEvent event) {
        try {
            NotificationManager manager = NotificationManager.getInstance();
            manager.shutdown();
        } catch (Exception e) {
            LOGGER.debug("Notification manager not available for server stop event", e);
        }
    }
}
