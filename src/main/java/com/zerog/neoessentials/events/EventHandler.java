package com.zerog.neoessentials.events;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.data.UserManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

/**
 * Handles all events for the NeoEssentials mod.
 */
<<<<<<< HEAD
public class EventHandler {    /**
     * Registers all event listeners.
     */
    public static void registerEvents() {
        NeoEssentials.LOGGER.info("Registering NeoEssentials event handlers");
        
        // Events are registered via @SubscribeEvent annotations
    }
    
    /**
     * Event handler for when a player joins the server.
     *
     * @param event The player login event
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
=======

public class EventHandler {

    /**
     * Registers all event listeners.
     */
    public void registerEvents() {
        NeoEssentials.LOGGER.info("Registering NeoEssentials event handlers");
        
        // Events are registered via @SubscribeEvent annotations
    }    /**
     * Event handler for when a player joins the server.
     *
     * @param event The player login event
     */    
    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
        Player player = event.getEntity();
        if (player instanceof ServerPlayer serverPlayer) {
            NeoEssentials.LOGGER.info("Player logged in: {}", player.getScoreboardName());
            
            // Get the user manager
            UserManager userManager = NeoEssentials.getInstance().getDataManager().getUserManager();
            
            // Load or create player data
            userManager.loadPlayerData(serverPlayer);
            
            // Track the player's username for baltop and other lookups
<<<<<<< HEAD
            userManager.trackPlayer(serverPlayer);            
            // Notify player about unread mail if they have any
            NeoEssentials.getInstance().getDataManager().getMailManager().notifyPlayer(serverPlayer);
            
            // Update tablist for joining player
            var tablistManager = NeoEssentials.getInstance().getDataManager().getTablistManager();
            if (tablistManager != null) {
                tablistManager.onPlayerJoin(serverPlayer);
            }
        }
    }
      /**
     * Event handler for when a player leaves the server.
     *
     * @param event The player logout event
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
=======
            userManager.trackPlayer(serverPlayer);
            
            // Notify player about unread mail if they have any
            NeoEssentials.getInstance().getDataManager().getMailManager().notifyPlayer(serverPlayer);
        }
    }

    /**
     * Event handler for when a player leaves the server.
     *
     * @param event The player logout event
     */    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
        Player player = event.getEntity();
        if (player instanceof ServerPlayer serverPlayer) {
            NeoEssentials.LOGGER.info("Player logged out: {}", player.getScoreboardName());
            
            // Get the user manager
<<<<<<< HEAD
            UserManager userManager = NeoEssentials.getInstance().getDataManager().getUserManager();            
            // Save player data
            userManager.savePlayerData(serverPlayer);
            
            // Update tablist for leaving player
            var tablistManager = NeoEssentials.getInstance().getDataManager().getTablistManager();
            if (tablistManager != null) {
                tablistManager.onPlayerLeave(serverPlayer);
            }
        }
    }
      /**
=======
            UserManager userManager = NeoEssentials.getInstance().getDataManager().getUserManager();
            
            // Save player data
            userManager.savePlayerData(serverPlayer);
        }
    }
    
    /**
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
     * Event handler for when the server is stopping.
     * Used to save all data before the server shuts down.
     *
     * @param event The server stopping event
<<<<<<< HEAD
     */
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
=======
     */    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
        NeoEssentials.LOGGER.info("Server stopping, saving all NeoEssentials data");
        
        // Save all data
        if (NeoEssentials.getInstance() != null && NeoEssentials.getInstance().getDataManager() != null) {
            NeoEssentials.getInstance().getDataManager().saveAll();
        }
    }
}
