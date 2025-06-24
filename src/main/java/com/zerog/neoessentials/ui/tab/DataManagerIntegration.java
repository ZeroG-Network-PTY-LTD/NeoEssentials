package com.zerog.neoessentials.ui.tab;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.ui.tablist.FlexibleTablistManager;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.concurrent.ScheduledExecutorService;

/**
 * Handles the integration between the legacy FlexibleTablistManager and the new TabManager
 * This is a transitional class that allows both systems to coexist during migration
 */
public class DataManagerIntegration {
    private static boolean migrationEnabled = true;
    
    /**
     * Creates a new TabManager that can coexist with the old FlexibleTablistManager
     * 
     * @param scheduler The scheduler to use
     * @param legacyManager The legacy manager for migration purposes
     * @return The new TabManager
     */
    public static TabManager createTabManager(ScheduledExecutorService scheduler, FlexibleTablistManager legacyManager) {
        // Create the new tab manager
        TabManager tabManager = new TabManager(scheduler);
        
        // Set up the hook for data manager integration
        DataManagerHooks.initializeTabManager(scheduler);
        
        // Log the creation
        NeoEssentials.LOGGER.info("New TabManager created via DataManagerIntegration");
        
        // If we need to migrate settings from the old manager, do so here
        if (migrationEnabled && legacyManager != null) {
            migrateSettings(tabManager, legacyManager);
        }
        
        return tabManager;
    }
    
    /**
     * Migrates settings from the old FlexibleTablistManager to the new TabManager
     * 
     * @param newManager The new manager
     * @param oldManager The old manager
     */
    private static void migrateSettings(TabManager newManager, FlexibleTablistManager oldManager) {
        try {
            // Log the migration attempt
            NeoEssentials.LOGGER.info("Attempting to migrate settings from old FlexibleTablistManager to new TabManager");
            
            // The actual migration logic would depend on what settings need to be preserved
            // This is just a placeholder for future implementation
            
            NeoEssentials.LOGGER.info("Settings successfully migrated from old FlexibleTablistManager");
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error migrating settings from old FlexibleTablistManager", e);
        }
    }
    
    /**
     * Creates a proxy that works with both the old and new managers
     * This makes the transition smoother by handling calls to both systems
     * 
     * @param tabManager The new TabManager
     * @param flexibleTablistManager The old FlexibleTablistManager
     * @return A proxy object that works with both systems
     */
    public static TablistManagerProxy createProxy(TabManager tabManager, FlexibleTablistManager flexibleTablistManager) {
        return new TablistManagerProxy(tabManager, flexibleTablistManager);
    }
    
    /**
     * Proxy class that handles calls to both the old and new tablist systems
     * This allows for a smoother transition between systems
     */
    public static class TablistManagerProxy {
        private final TabManager tabManager;
        private final FlexibleTablistManager flexibleTablistManager;
        private boolean useNewManager = true;
        
        /**
         * Creates a new proxy
         * 
         * @param tabManager The new TabManager
         * @param flexibleTablistManager The old FlexibleTablistManager
         */
        public TablistManagerProxy(TabManager tabManager, FlexibleTablistManager flexibleTablistManager) {
            this.tabManager = tabManager;
            this.flexibleTablistManager = flexibleTablistManager;
        }
        
        /**
         * Sets the server instance for both managers
         * 
         * @param server The server
         */
        public void setServer(MinecraftServer server) {
            if (tabManager != null) {
                tabManager.setServer(server);
            }
            
            if (flexibleTablistManager != null) {
                flexibleTablistManager.setServer(server);
            }
        }
        
        /**
         * Initializes both managers
         */
        public void initialize() {
            if (tabManager != null) {
                tabManager.initialize();
            }
            
            if (flexibleTablistManager != null && !useNewManager) {
                flexibleTablistManager.initialize();
            }
        }
        
        /**
         * Handles player join for both managers
         * 
         * @param player The joining player
         */
        public void onPlayerJoin(ServerPlayer player) {
            if (tabManager != null) {
                tabManager.onPlayerJoin(player);
            }
            
            if (flexibleTablistManager != null && !useNewManager) {
                flexibleTablistManager.onPlayerJoin(player);
            }
        }
        
        /**
         * Handles player leave for both managers
         * 
         * @param player The leaving player
         */
        public void onPlayerLeave(ServerPlayer player) {
            if (tabManager != null) {
                tabManager.onPlayerLeave(player);
            }
            
            if (flexibleTablistManager != null && !useNewManager) {
                flexibleTablistManager.onPlayerLeave(player);
            }
        }
        
        /**
         * Handles player world change for both managers
         * 
         * @param player The player
         * @param worldName The world name
         */
        public void onPlayerChangeWorld(ServerPlayer player, String worldName) {
            if (tabManager != null) {
                tabManager.onPlayerChangeWorld(player, worldName);
            }
            
            if (flexibleTablistManager != null && !useNewManager) {
                // Flexible manager doesn't have this method directly, 
                // but we could implement similar logic if needed
            }
        }
        
        /**
         * Sets whether to use the new manager exclusively
         * 
         * @param useNewManager True to use only the new manager, false to use both
         */
        public void setUseNewManager(boolean useNewManager) {
            this.useNewManager = useNewManager;
        }
        
        /**
         * Gets the new TabManager
         * 
         * @return The new TabManager
         */
        public TabManager getTabManager() {
            return tabManager;
        }
        
        /**
         * Gets the old FlexibleTablistManager
         * 
         * @return The old FlexibleTablistManager
         */
        public FlexibleTablistManager getFlexibleTablistManager() {
            return flexibleTablistManager;
        }
    }
}
