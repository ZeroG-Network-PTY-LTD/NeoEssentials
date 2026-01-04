package com.zerog.neoessentials.webdashboard;

import com.zerog.neoessentials.webdashboard.data.DataCollector;
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
        
        try {
            MinecraftServer server = event.getServer();
            
            // Initialize data collector
            DataCollector.getInstance().initialize(server);
            
            // Set server reference for API
            DashboardAPI.getInstance().setServer(server);
            
            // Start Dashboard API
            DashboardAPI.getInstance().start();
            
            LOGGER.info("Dashboard auto-started successfully");
        } catch (Exception e) {
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

                // Shutdown data collector
                startTime = System.currentTimeMillis();
                DataCollector.getInstance().shutdown();
                long collectorStopTime = System.currentTimeMillis() - startTime;
                LOGGER.info("Data Collector stopped in {}ms", collectorStopTime);

                LOGGER.info("Dashboard shutdown complete (total: {}ms)", dashboardStopTime + collectorStopTime);
            }
        } catch (Exception e) {
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
