package com.zerog.neoessentials.webdashboard.events;

import com.zerog.neoessentials.webdashboard.WebDashboardServer;
import com.zerog.neoessentials.webdashboard.websocket.DataStreamManager;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Event handler for WebSocket data streaming integration
 * Listens to Minecraft events and broadcasts to WebSocket clients
 */
@EventBusSubscriber(modid = "neoessentials")
public class WebSocketEventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketEventHandler.class);
    
    // TPS calculation
    private static long lastTickTime = 0;
    private static long lastTickMillis = 0;
    private static double currentTps = 20.0;
    private static int tickCounter = 0;
    
    /**
     * Track server ticks for TPS calculation and performance monitoring
     */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server == null) return;
        
        tickCounter++;
        long currentMillis = System.currentTimeMillis();
        
        // Calculate TPS every 20 ticks (1 second)
        if (tickCounter >= 20) {
            if (lastTickMillis > 0) {
                long elapsedMillis = currentMillis - lastTickMillis;
                if (elapsedMillis > 0) {
                    currentTps = (20.0 * 1000.0) / elapsedMillis;
                }
            }
            lastTickMillis = currentMillis;
            tickCounter = 0;
            
            // Update data stream manager
            try {
                WebDashboardServer dashboardServer = WebDashboardServer.getInstance();
                if (dashboardServer != null && dashboardServer.isRunning()) {
                    DataStreamManager streamManager = dashboardServer.getDataStreamManager();
                    if (streamManager != null) {
                        streamManager.updateTps(currentTps, lastTickTime);
                    }
                }
            } catch (Exception e) {
                // Dashboard might not be initialized yet, ignore
            }
        }
        
        // Calculate tick time in milliseconds (average time per tick)
        if (lastTickMillis > 0) {
            long elapsedMillis = currentMillis - lastTickMillis;
            lastTickTime = elapsedMillis; // Average time for 20 ticks
        }
    }
    
    /**
     * Broadcast player join events to WebSocket clients
     */
    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        try {
            WebDashboardServer dashboardServer = WebDashboardServer.getInstance();
            if (dashboardServer != null && dashboardServer.isRunning()) {
                DataStreamManager streamManager = dashboardServer.getDataStreamManager();
                if (streamManager != null) {
                    String playerName = event.getEntity().getName().getString();
                    String uuid = event.getEntity().getUUID().toString();
                    streamManager.broadcastPlayerJoin(playerName, uuid);
                    
                    LOGGER.debug("Broadcast player join: {}", playerName);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error broadcasting player join event", e);
        }
    }
    
    /**
     * Broadcast player leave events to WebSocket clients
     */
    @SubscribeEvent
    public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        try {
            WebDashboardServer dashboardServer = WebDashboardServer.getInstance();
            if (dashboardServer != null && dashboardServer.isRunning()) {
                DataStreamManager streamManager = dashboardServer.getDataStreamManager();
                if (streamManager != null) {
                    String playerName = event.getEntity().getName().getString();
                    String uuid = event.getEntity().getUUID().toString();
                    streamManager.broadcastPlayerLeave(playerName, uuid);
                    
                    LOGGER.debug("Broadcast player leave: {}", playerName);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error broadcasting player leave event", e);
        }
    }
    
    /**
     * Broadcast chat messages to WebSocket clients
     */
    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        try {
            WebDashboardServer dashboardServer = WebDashboardServer.getInstance();
            if (dashboardServer != null && dashboardServer.isRunning()) {
                DataStreamManager streamManager = dashboardServer.getDataStreamManager();
                if (streamManager != null) {
                    String playerName = event.getPlayer().getName().getString();
                    String message = event.getMessage().getString();
                    long timestamp = System.currentTimeMillis();
                    
                    streamManager.broadcastChatMessage(playerName, message, timestamp);
                    
                    LOGGER.debug("Broadcast chat message from {}: {}", playerName, message);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error broadcasting chat message", e);
        }
    }
    
    /**
     * Cleanup when server stops
     */
    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        try {
            WebDashboardServer dashboardServer = WebDashboardServer.getInstance();
            if (dashboardServer != null && dashboardServer.isRunning()) {
                // Dashboard will auto-stop with server lifecycle hooks
                LOGGER.info("Server stopped, WebSocket connections will be closed");
            }
        } catch (Exception e) {
            LOGGER.debug("Dashboard not initialized or already stopped");
        }
    }
    
    /**
     * Get current TPS (for external access)
     */
    public static double getCurrentTps() {
        return Math.min(currentTps, 20.0);
    }
    
    /**
     * Get last tick time (for external access)
     */
    public static long getLastTickTime() {
        return lastTickTime;
    }
}
