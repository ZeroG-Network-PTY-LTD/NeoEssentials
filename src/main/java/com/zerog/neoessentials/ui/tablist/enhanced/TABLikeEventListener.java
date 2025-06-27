package com.zerog.neoessentials.ui.tablist.enhanced;

import com.zerog.neoessentials.NeoEssentials;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

/**
 * Event listener for the TABLikeTablistManager system
 * Handles all NeoForge events related to enhanced tablist functionality
 */
public class TABLikeEventListener {
    
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
        
        try {
            // Get TABLikeTablistManager from data manager
            var dataManager = NeoEssentials.getInstance().getDataManager();
            if (dataManager != null && dataManager.getTablistManager() != null) {
                dataManager.getTablistManager().onPlayerJoin(player);
            }
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error handling player login for TABLikeTablistManager", e);
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
        
        try {
            // Get TABLikeTablistManager from data manager
            var dataManager = NeoEssentials.getInstance().getDataManager();
            if (dataManager != null && dataManager.getTablistManager() != null) {
                dataManager.getTablistManager().onPlayerLeave(player);
            }
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error handling player logout for TABLikeTablistManager", e);
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
        
        try {
            // Get TABLikeTablistManager from data manager
            var dataManager = NeoEssentials.getInstance().getDataManager();
            if (dataManager != null && dataManager.getTablistManager() != null) {
                // Treat dimension change as a rejoin for tablist purposes
                dataManager.getTablistManager().onPlayerJoin(player);
            }
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error handling player dimension change for TABLikeTablistManager", e);
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
        
        try {
            // Get TABLikeTablistManager from data manager
            var dataManager = NeoEssentials.getInstance().getDataManager();
            if (dataManager != null && dataManager.getTablistManager() != null) {
                // Treat respawn as a rejoin for tablist purposes
                dataManager.getTablistManager().onPlayerJoin(player);
            }
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error handling player respawn for TABLikeTablistManager", e);
        }
    }
    
    /**
     * Called when the server has started and is ready to accept players
     * 
     * @param event The server started event
     */
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        try {
            // Get TABLikeTablistManager from data manager
            var dataManager = NeoEssentials.getInstance().getDataManager();
            if (dataManager != null && dataManager.getTablistManager() != null && event.getServer() != null) {
                // Update the server reference
                dataManager.getTablistManager().setServer(event.getServer());
                
                // Initialize the TABLikeTablistManager
                dataManager.getTablistManager().initialize();
                
                NeoEssentials.LOGGER.info("TABLikeTablistManager fully initialized on server start");
            }
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error initializing TABLikeTablistManager on server start", e);
        }
    }
    
    /**
     * Called when the server is stopping
     * 
     * @param event The server stopping event
     */
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        try {
            // Get TABLikeTablistManager from data manager
            var dataManager = NeoEssentials.getInstance().getDataManager();
            if (dataManager != null && dataManager.getTablistManager() != null) {
                // Shutdown the tablist manager cleanly
                dataManager.getTablistManager().shutdown();
                
                NeoEssentials.LOGGER.info("TABLikeTablistManager shutdown on server stop");
            }
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error shutting down TABLikeTablistManager on server stop", e);
        }
    }
}
