
package com.zerog.neoessentials.listeners;

import com.zerog.neoessentials.placeholders.PlaceholderManager;

/**
 * Server tick listener for updating animated placeholders and performance metrics
 */
@net.neoforged.fml.common.EventBusSubscriber(modid = "neoessentials")
public class ServerTickListener {
    private static final java.util.concurrent.ScheduledExecutorService scheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
    private static boolean initialized = false;

    /**
     * Initialize the tick scheduler
     */
    public static void initialize() {
        if (!initialized) {
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    long now = System.currentTimeMillis();
                    PlaceholderManager pm = PlaceholderManager.getInstance();
                    pm.onServerTick();
                    pm.tickAnimatedPlaceholders(now);
                    
                    // Also notify TabListManager to update any live displays
                    try {
                        com.zerog.neoessentials.features.TabListManager tlm = com.zerog.neoessentials.features.TabListManager.getInstance();
                        if (tlm != null) {
                            // The TabListManager already has its own high-frequency update task
                            // so we don't need to call it here, but we ensure it's running
                        }
                    } catch (Exception tlmError) {
                        // TabListManager might not be initialized yet, ignore
                    }
                } catch (Exception e) {
                    // Silently handle errors to prevent spam
                }
            }, 0, 50, java.util.concurrent.TimeUnit.MILLISECONDS);
            initialized = true;
        }
    }

    /**
     * Server tick event handler (fallback for NeoForge events)
     */
    @net.neoforged.bus.api.SubscribeEvent
    public static void onServerTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Pre event) {
        try {
            long now = System.currentTimeMillis();
            PlaceholderManager pm = PlaceholderManager.getInstance();
            pm.onServerTick();
            pm.tickAnimatedPlaceholders(now);
        } catch (Exception e) {
            // Silently handle errors to prevent spam
        }
    }
}
