package com.zerog.neoessentials.ui.tab;

import java.util.concurrent.ScheduledExecutorService;

import com.zerog.neoessentials.NeoEssentials;
import net.minecraft.server.MinecraftServer;

/**
 * Provides integration hooks between the new TabManager and the existing DataManager
 */
public class DataManagerHooks {
    private static TabManager tabManager;
    
    /**
     * Initializes the TabManager for use with the DataManager
     * 
     * @param scheduler The scheduler to use
     * @return The created TabManager instance
     */
    public static TabManager initializeTabManager(ScheduledExecutorService scheduler) {
        if (tabManager == null) {
            tabManager = new TabManager(scheduler);
            NeoEssentials.LOGGER.info("New TabManager created via DataManagerHooks");
        }
        return tabManager;
    }
    
    /**
     * Sets the server instance for the TabManager
     * 
     * @param server The server instance
     */
    public static void setServer(MinecraftServer server) {
        if (tabManager != null) {
            tabManager.setServer(server);
            NeoEssentials.LOGGER.info("Server reference updated in TabManager via DataManagerHooks");
        }
    }
    
    /**
     * Gets the current TabManager instance
     * 
     * @return The TabManager instance, or null if not initialized
     */
    public static TabManager getTabManager() {
        return tabManager;
    }
    
    /**
     * Initialize the TabManager if it exists
     */
    public static void initializeIfExists() {
        if (tabManager != null) {
            tabManager.initialize();
            NeoEssentials.LOGGER.info("TabManager initialized via DataManagerHooks");
        }
    }
    
    /**
     * Safely updates player data when a player joins
     * 
     * @param player The player who joined
     */
    public static void onPlayerJoin(net.minecraft.server.level.ServerPlayer player) {
        if (tabManager != null) {
            tabManager.onPlayerJoin(player);
        }
    }
    
    /**
     * Safely updates player data when a player leaves
     * 
     * @param player The player who left
     */
    public static void onPlayerLeave(net.minecraft.server.level.ServerPlayer player) {
        if (tabManager != null) {
            tabManager.onPlayerLeave(player);
        }
    }
    
    /**
     * Safely updates player data when a player changes worlds
     * 
     * @param player The player who changed worlds
     * @param worldName The name of the new world
     */
    public static void onPlayerChangeWorld(net.minecraft.server.level.ServerPlayer player, String worldName) {
        if (tabManager != null) {
            tabManager.onPlayerChangeWorld(player, worldName);
        }
    }
    
    /**
     * Ensures the TabManager is initialized. If not, creates and initializes it.
     * 
     * @return True if TabManager is initialized, false otherwise
     */
    public static boolean ensureTabManagerInitialized() {
        if (tabManager == null) {
            ScheduledExecutorService scheduler = NeoEssentials.getInstance().getScheduler();
            if (scheduler != null) {
                tabManager = new TabManager(scheduler);
                NeoEssentials.LOGGER.info("TabManager created via ensureTabManagerInitialized");
                
                // Set server if available
                if (NeoEssentials.getInstance().getServer() != null) {
                    tabManager.setServer(NeoEssentials.getInstance().getServer());
                }
                
                // Initialize the TabManager
                tabManager.initialize();
                return true;
            }
            return false;
        }
        
        return tabManager.isInitialized();
    }
    
    /**
     * Reload templates from disk
     * 
     * @return True if reload was successful, false otherwise
     */
    public static boolean reloadTemplates() {
        if (ensureTabManagerInitialized()) {
            return tabManager.reloadTemplates();
        }
        return false;
    }
}
