package com.zerog.neoessentials.ui.tab;

import com.zerog.neoessentials.NeoEssentials;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

/**
 * Event listener for the TabManager system
 * Handles all NeoForge events related to tablist functionality
 */
public class TabEventListener {
    /**
     * Called when a player logs in
     * 
     * @param event The player login event
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        
        // Handle player join event
        try {
            // Get TabManager from hooks
            TabManager tabManager = DataManagerHooks.getTabManager();
            if (tabManager != null) {
                tabManager.onPlayerJoin(player);
            }
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error handling player login for TabManager", e);
        }
    }
    
    /**
     * Called when a player logs out
     * 
     * @param event The player logout event
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        
        // Handle player leave event
        try {
            // Get TabManager from hooks
            TabManager tabManager = DataManagerHooks.getTabManager();
            if (tabManager != null) {
                tabManager.onPlayerLeave(player);
            }
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error handling player logout for TabManager", e);
        }
    }
    
    /**
     * Called when a player changes dimensions
     * 
     * @param event The dimension change event
     */
    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        
        // Get world name from dimension
        String worldName = event.getTo().location().toString();
        
        // Handle player world change event
        try {
            // Get TabManager from hooks
            TabManager tabManager = DataManagerHooks.getTabManager();
            if (tabManager != null) {
                tabManager.onPlayerChangeWorld(player, worldName);
            }
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error handling dimension change for TabManager", e);
        }
    }
    
    /**
     * Called when a player respawns
     * 
     * @param event The player respawn event
     */
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        
        // Get world name
        Level level = player.level();
        String worldName = level.dimension().location().toString();
        
        // Handle player respawn (similar to world change)
        try {
            // Get TabManager from hooks
            TabManager tabManager = DataManagerHooks.getTabManager();
            if (tabManager != null) {
                // Update the player's data
                tabManager.onPlayerChangeWorld(player, worldName);
            }
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error handling player respawn for TabManager", e);
        }
    }
    
    /**
     * Called when the server has started and is ready to accept players
     * 
     * @param event The server started event
     */
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        // Initialize the TabManager once the server is fully started
        try {
            // Get TabManager from hooks
            TabManager tabManager = DataManagerHooks.getTabManager();
            if (tabManager != null && event.getServer() != null) {
                // Update the server reference
                tabManager.setServer(event.getServer());
                
                // Initialize the TabManager
                tabManager.initialize();
                
                NeoEssentials.LOGGER.info("TabManager fully initialized on server start");
            }
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error initializing TabManager on server start", e);
        }
    }
    
    /**
     * Called when the server is stopping
     * 
     * @param event The server stopping event
     */
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        // Clean up TabManager resources if needed
        NeoEssentials.LOGGER.info("TabManager shutting down with server");
    }
}
