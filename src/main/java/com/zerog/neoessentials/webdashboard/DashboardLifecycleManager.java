package com.zerog.neoessentials.webdashboard;

import com.zerog.neoessentials.webdashboard.data.DataCollector;
import com.zerog.neoessentials.webdashboard.websocket.DashboardWebSocketServer;
import com.zerog.neoessentials.util.motd.MotdManager;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import com.zerog.neoessentials.config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the lifecycle of the Dashboard API
 * Automatically starts/stops the dashboard with the server
 */
@EventBusSubscriber(modid = "neoessentials")
public class DashboardLifecycleManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(DashboardLifecycleManager.class);
    private static boolean manuallyDisabled = false;
    
    /**
     * Called when server starts - automatically start dashboard if enabled
     */
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        
        if (!ConfigManager.isWebDashboardEnabled()) {
            LOGGER.info("Dashboard is disabled in configuration");
            return;
        }
        
        if (manuallyDisabled) {
            LOGGER.info("Dashboard was manually disabled and will not auto-start");
            return;
        }

        if (!ConfigManager.isWebDashboardAutoStartEnabled()) {
            LOGGER.info("Dashboard auto-start is disabled in configuration — use /dashboard start to start it manually");
            return;
        }

        try {
            MinecraftServer server = event.getServer();

            // Initialize data collector
            DataCollector.getInstance().initialize(server);
            
            // Set server reference for API
            DashboardAPI.getInstance().setServer(server);
            
            // Start Dashboard API
            DashboardAPI.getInstance().start();

            // Start WebSocket server
            // NOTE: catches Throwable, not just Exception — a missing Java-WebSocket
            // dependency surfaces as NoClassDefFoundError (an Error), which would
            // otherwise escape this handler and crash the whole server.
            try {
                int wsPort = ConfigManager.getInstance().getWebDashboardWebSocketPort();
                DashboardWebSocketServer wsServer = DashboardWebSocketServer.getInstance(wsPort);
                wsServer.start();
                LOGGER.info("Dashboard WebSocket server started on port {}", wsPort);
            } catch (Throwable wsEx) {
                LOGGER.error("Failed to start WebSocket server: {}", wsEx.getMessage(), wsEx);
            }

            LOGGER.info("Dashboard auto-started successfully");
        } catch (Throwable e) {
            LOGGER.error("Failed to auto-start dashboard", e);
        }
    }
    
    /**
     * Called when server stops - automatically stop dashboard
     */
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        try {
            if (DashboardAPI.getInstance().isRunning()) {
                LOGGER.info("Server stopping - shutting down Dashboard...");

                // Stop Dashboard API
                long startTime = System.currentTimeMillis();
                DashboardAPI.getInstance().stop();
                long dashboardStopTime = System.currentTimeMillis() - startTime;
                LOGGER.info("Dashboard API stopped in {}ms", dashboardStopTime);

                // Stop WebSocket server
                try {
                    DashboardWebSocketServer.getInstance().stop(2000);
                    LOGGER.info("Dashboard WebSocket server stopped");
                } catch (Throwable wsEx) {
                    LOGGER.warn("Error stopping WebSocket server: {}", wsEx.getMessage());
                }

                // Shutdown data collector
                startTime = System.currentTimeMillis();
                DataCollector.getInstance().shutdown();
                long collectorStopTime = System.currentTimeMillis() - startTime;
                LOGGER.info("Data Collector stopped in {}ms", collectorStopTime);

                LOGGER.info("Dashboard shutdown complete (total: {}ms)", dashboardStopTime + collectorStopTime);
            }
            // Always shut down the MOTD rotation scheduler on server stop
            MotdManager.getInstance().shutdown();
        } catch (Throwable e) {
            LOGGER.error("Error stopping dashboard", e);
        }
    }

    /**
     * Manually start the dashboard
     */
    public static boolean startDashboard(MinecraftServer server) {
        try {
            if (DashboardAPI.getInstance().isRunning()) {
                return false; // Already running
            }
            
            // Initialize data collector if not already
            DataCollector.getInstance().initialize(server);
            
            // Set server reference for API
            DashboardAPI.getInstance().setServer(server);
            
            // Start Dashboard API
            DashboardAPI.getInstance().start();

            // Start WebSocket server
            try {
                int wsPort = ConfigManager.getInstance().getWebDashboardWebSocketPort();
                DashboardWebSocketServer.getInstance(wsPort).start();
            } catch (Throwable wsEx) {
                LOGGER.error("Failed to start WebSocket server (manual): {}", wsEx.getMessage(), wsEx);
            }

            manuallyDisabled = false;
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to start dashboard manually", e);
            return false;
        }
    }
    
    /**
     * Manually stop the dashboard
     */
    public static boolean stopDashboard() {
        try {
            if (!DashboardAPI.getInstance().isRunning()) {
                return false; // Not running
            }
            
            // Stop Dashboard API
            DashboardAPI.getInstance().stop();

            // Stop WebSocket server
            try {
                DashboardWebSocketServer.getInstance().stop(2000);
            } catch (Throwable wsEx) {
                LOGGER.warn("Error stopping WebSocket server (manual): {}", wsEx.getMessage());
            }

            // Shutdown data collector
            DataCollector.getInstance().shutdown();
            
            manuallyDisabled = true;
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to stop dashboard manually", e);
            return false;
        }
    }
    
    /**
     * Get dashboard status
     */
    public static DashboardStatus getStatus() {
        boolean running = DashboardAPI.getInstance().isRunning();
        boolean enabled = ConfigManager.isWebDashboardEnabled();
        String url = String.format("http://%s:%d", 
            DashboardAPI.getInstance().getBindAddress(),
            DashboardAPI.getInstance().getPort());
        
        return new DashboardStatus(running, enabled, manuallyDisabled, url);
    }
    
    /**
     * Dashboard status information
     */
    public static class DashboardStatus {
        public final boolean running;
        public final boolean configEnabled;
        public final boolean manuallyDisabled;
        public final String url;
        
        public DashboardStatus(boolean running, boolean configEnabled, boolean manuallyDisabled, String url) {
            this.running = running;
            this.configEnabled = configEnabled;
            this.manuallyDisabled = manuallyDisabled;
            this.url = url;
        }
    }
}
