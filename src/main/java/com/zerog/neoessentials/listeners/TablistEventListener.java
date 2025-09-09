package com.zerog.neoessentials.listeners;

import com.zerog.neoessentials.tablist.TabUpdateOrchestrator;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Event listener for tablist-related events
 * Handles player join/leave for tablist updates
 */
public class TablistEventListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(TablistEventListener.class);
    
    private final TabUpdateOrchestrator tabUpdateOrchestrator;
    
    public TablistEventListener(TabUpdateOrchestrator tabUpdateOrchestrator) {
        this.tabUpdateOrchestrator = tabUpdateOrchestrator;
    }
    
    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            try {
                tabUpdateOrchestrator.onPlayerJoin(player);
                com.zerog.neoessentials.util.DebugUtil.debugLog("Tablist updated for joining player: " + player.getName().getString());
            } catch (Exception e) {
                LOGGER.error("Error updating tablist for joining player: " + player.getName().getString(), e);
            }
        }
    }
    
    @SubscribeEvent
    public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            try {
                tabUpdateOrchestrator.onPlayerQuit(player);
                com.zerog.neoessentials.util.DebugUtil.debugLog("Tablist updated for leaving player: " + player.getName().getString());
            } catch (Exception e) {
                LOGGER.error("Error updating tablist for leaving player: " + player.getName().getString(), e);
            }
        }
    }
    
    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        try {
            // Clean up tablist resources when server stops
            com.zerog.neoessentials.util.DebugUtil.debugLog("Tablist system shutting down with server");
        } catch (Exception e) {
            LOGGER.error("Error during tablist system shutdown", e);
        }
    }
}
