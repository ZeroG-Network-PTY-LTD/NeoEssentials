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
<<<<<<< HEAD
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
=======
public class EventHandler {
>>>>>>> b6e2875 (fix: Update build number to 31 and modify event handler methods for proper registration)

    /**
=======
public class EventHandler {    /**
>>>>>>> 5283f26 (fix: Update build number to 35 and change event handler methods to static for proper registration)
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
<<<<<<< HEAD
<<<<<<< HEAD
     */    
    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
     */      @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
>>>>>>> 2ae5184 (fix: Change event handler methods to static for proper registration)
=======
     */
    @SubscribeEvent
<<<<<<< HEAD
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
>>>>>>> b6e2875 (fix: Update build number to 31 and modify event handler methods for proper registration)
=======
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
>>>>>>> 5283f26 (fix: Update build number to 35 and change event handler methods to static for proper registration)
        Player player = event.getEntity();
        if (player instanceof ServerPlayer serverPlayer) {
            NeoEssentials.LOGGER.info("Player logged in: {}", player.getScoreboardName());
            
            // Get the user manager
            UserManager userManager = NeoEssentials.getInstance().getDataManager().getUserManager();
            
            // Load or create player data
            userManager.loadPlayerData(serverPlayer);
            
            // Track the player's username for baltop and other lookups
<<<<<<< HEAD
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
            
=======
            userManager.trackPlayer(serverPlayer);            
>>>>>>> b9b302b (feat: Enhance tablist functionality with player-specific headers and footers; update DataManager and EventHandler for tablist integration)
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
<<<<<<< HEAD
<<<<<<< HEAD
     */    
    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
     */      @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
>>>>>>> 3f70d9c (fix: Update build number to 27 and correct event handler method visibility)
=======
     */
    @SubscribeEvent
<<<<<<< HEAD
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
>>>>>>> b6e2875 (fix: Update build number to 31 and modify event handler methods for proper registration)
=======
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
>>>>>>> 5283f26 (fix: Update build number to 35 and change event handler methods to static for proper registration)
        Player player = event.getEntity();
        if (player instanceof ServerPlayer serverPlayer) {
            NeoEssentials.LOGGER.info("Player logged out: {}", player.getScoreboardName());
            
            // Get the user manager
<<<<<<< HEAD
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
            
=======
            UserManager userManager = NeoEssentials.getInstance().getDataManager().getUserManager();            
>>>>>>> b9b302b (feat: Enhance tablist functionality with player-specific headers and footers; update DataManager and EventHandler for tablist integration)
            // Save player data
            userManager.savePlayerData(serverPlayer);
            
            // Update tablist for leaving player
            var tablistManager = NeoEssentials.getInstance().getDataManager().getTablistManager();
            if (tablistManager != null) {
                tablistManager.onPlayerLeave(serverPlayer);
            }
        }
    }
<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
    
    /**
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
      /**
>>>>>>> e2153e5 (fix: Improve event registration and storage manager initialization in NeoEssentials)
=======
    
    /**
>>>>>>> b6e2875 (fix: Update build number to 31 and modify event handler methods for proper registration)
=======
      /**
>>>>>>> 5283f26 (fix: Update build number to 35 and change event handler methods to static for proper registration)
     * Event handler for when the server is stopping.
     * Used to save all data before the server shuts down.
     *
     * @param event The server stopping event
<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
     */
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
=======
     */    @SubscribeEvent
=======
     */    
    @SubscribeEvent
>>>>>>> e2153e5 (fix: Improve event registration and storage manager initialization in NeoEssentials)
    public void onServerStopping(ServerStoppingEvent event) {
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
     */      @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
>>>>>>> 2ae5184 (fix: Change event handler methods to static for proper registration)
=======
     */
    @SubscribeEvent
<<<<<<< HEAD
    public void onServerStopping(ServerStoppingEvent event) {
>>>>>>> b6e2875 (fix: Update build number to 31 and modify event handler methods for proper registration)
=======
    public static void onServerStopping(ServerStoppingEvent event) {
>>>>>>> 5283f26 (fix: Update build number to 35 and change event handler methods to static for proper registration)
        NeoEssentials.LOGGER.info("Server stopping, saving all NeoEssentials data");
        
        // Save all data
        if (NeoEssentials.getInstance() != null && NeoEssentials.getInstance().getDataManager() != null) {
            NeoEssentials.getInstance().getDataManager().saveAll();
        }
    }
}
