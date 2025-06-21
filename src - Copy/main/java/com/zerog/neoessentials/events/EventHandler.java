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
        Player player = event.getEntity();
        if (player instanceof ServerPlayer serverPlayer) {
            NeoEssentials.LOGGER.info("Player logged in: {}", player.getScoreboardName());
            
            // Get the user manager
            UserManager userManager = NeoEssentials.getInstance().getDataManager().getUserManager();
            
            // Load or create player data
            userManager.loadPlayerData(serverPlayer);
            
            // Track the player's username for baltop and other lookups
            userManager.trackPlayer(serverPlayer);
        }
    }

    /**
     * Event handler for when a player leaves the server.
     *
     * @param event The player logout event
     */    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        if (player instanceof ServerPlayer serverPlayer) {
            NeoEssentials.LOGGER.info("Player logged out: {}", player.getScoreboardName());
            
            // Get the user manager
            UserManager userManager = NeoEssentials.getInstance().getDataManager().getUserManager();
            
            // Save player data
            userManager.savePlayerData(serverPlayer);
        }
    }
    
    /**
     * Event handler for when the server is stopping.
     * Used to save all data before the server shuts down.
     *
     * @param event The server stopping event
     */    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        NeoEssentials.LOGGER.info("Server stopping, saving all NeoEssentials data");
        
        // Save all data
        if (NeoEssentials.getInstance() != null && NeoEssentials.getInstance().getDataManager() != null) {
            NeoEssentials.getInstance().getDataManager().saveAll();
        }
    }
}
