package com.zerog.neoessentials.webdashboard.map;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Event listener for tracking player locations on the map
 * Updates player positions periodically for real-time map display
 */
@EventBusSubscriber(modid = "neoessentials")
public class MapPlayerTracker {
    private static final Logger LOGGER = LoggerFactory.getLogger(MapPlayerTracker.class);
    
    // Update player locations every second (20 ticks)
    private static final int UPDATE_INTERVAL = 20;
    private static int tickCounter = 0;
    
    /**
     * Track player join events
     */
    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerLocationTracker tracker = PlayerLocationTracker.getInstance();
            tracker.updatePlayerLocation(player);
            NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Map: Player joined - {}", player.getGameProfile().getName());
        }
    }
    
    /**
     * Track player leave events
     */
    @SubscribeEvent
    public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerLocationTracker tracker = PlayerLocationTracker.getInstance();
            tracker.removePlayer(player.getUUID());
            NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Map: Player left - {}", player.getGameProfile().getName());
        }
    }
    
    /**
     * Update player locations on server tick
     */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        tickCounter++;
        
        // Update player locations every second
        if (tickCounter >= UPDATE_INTERVAL) {
            tickCounter = 0;
            
            var server = event.getServer();
            if (server != null) {
                PlayerLocationTracker tracker = PlayerLocationTracker.getInstance();
                
                // Update all online players
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    tracker.updatePlayerLocation(player);
                }
            }
        }
    }
}
