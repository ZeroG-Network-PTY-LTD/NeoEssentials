package com.zerog.neoessentials.discord;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Discord event listener for NeoEssentials
 * Handles server and player events for Discord integration
 * 
 * @author ZeroG
 * @since 2.0.0
 */
@EventBusSubscriber(modid = "neoessentials")
public class DiscordEventListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(DiscordEventListener.class);
    
    /**
     * Handle player join events
     */
    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            try {
                DiscordManager.getInstance().notifyPlayerJoin(player);
            } catch (Exception e) {
                LOGGER.error("Failed to send Discord join notification for player: " + player.getDisplayName().getString(), e);
            }
        }
    }
    
    /**
     * Handle player leave events
     */
    @SubscribeEvent
    public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            try {
                DiscordManager.getInstance().notifyPlayerLeave(player);
            } catch (Exception e) {
                LOGGER.error("Failed to send Discord leave notification for player: " + player.getDisplayName().getString(), e);
            }
        }
    }
    
    /**
     * Handle server start events
     */
    @SubscribeEvent
    public void onServerStart(ServerStartedEvent event) {
        try {
            DiscordManager.getInstance().notifyServerStart();
            LOGGER.info("Sent Discord server start notification");
        } catch (Exception e) {
            LOGGER.error("Failed to send Discord server start notification", e);
        }
    }
    
    /**
     * Handle server stop events
     */
    @SubscribeEvent
    public void onServerStop(ServerStoppedEvent event) {
        try {
            DiscordManager.getInstance().notifyServerStop();
            LOGGER.info("Sent Discord server stop notification");
        } catch (Exception e) {
            LOGGER.error("Failed to send Discord server stop notification", e);
        }
    }
}
