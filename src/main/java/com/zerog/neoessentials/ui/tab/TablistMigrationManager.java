package com.zerog.neoessentials.ui.tab;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.ui.EnhancedTablistManager;
import com.zerog.neoessentials.ui.tablist.FlexibleTablistManager;
import net.minecraft.server.MinecraftServer;

/**
 * This class manages the transition from the old tablist system to the new one
 * by ensuring that only one tablist system is active at a time.
 */
public class TablistMigrationManager {
    
    private static boolean migrationApplied = false;
    
    /**
     * Applies the tablist migration by deactivating old tablist systems
     * and ensuring only the new TabManager is active.
     * This method is idempotent (can be called multiple times without side effects).
     */
    public static void applyMigration() {
        if (migrationApplied) {
            return;
        }
        
        try {
            // Let the TabManager handle all tablist activities
            TabManager tabManager = DataManagerHooks.getTabManager();
            if (tabManager == null) {
                NeoEssentials.LOGGER.warn("TabManager not initialized, cannot apply migration");
                return;
            }
            
            // Update all references to the main TabManager to prevent duplicates
            
            // Get the server instance if available
            MinecraftServer server = NeoEssentials.getInstance().getServer();
            if (server != null) {
                DataManagerHooks.setServer(server);
                NeoEssentials.LOGGER.info("Updated server reference in TabManager");
            }
            
            // Mark migration as applied
            migrationApplied = true;
            NeoEssentials.LOGGER.info("Successfully migrated to new TabManager system");
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error applying tablist migration", e);
        }
    }
    
    /**
     * Checks if the TabManager should be the primary tablist handler
     * and deactivates other tablist systems if needed
     * 
     * @param enhancedManager EnhancedTablistManager instance to check
     * @return true if the old manager should be deactivated
     */
    public static boolean shouldDeactivate(EnhancedTablistManager enhancedManager) {
        if (enhancedManager == null) {
            return false;
        }
        
        applyMigration();
        return migrationApplied;
    }
    
    /**
     * Checks if the TabManager should be the primary tablist handler
     * and deactivates other tablist systems if needed
     * 
     * @param flexibleManager FlexibleTablistManager instance to check
     * @return true if the old manager should be deactivated
     */
    public static boolean shouldDeactivate(FlexibleTablistManager flexibleManager) {
        if (flexibleManager == null) {
            return false;
        }
        
        applyMigration();
        return migrationApplied;
    }
}
