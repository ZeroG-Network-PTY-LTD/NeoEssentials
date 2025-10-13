package com.zerog.neoessentials.teleportation;

import com.mojang.brigadier.CommandDispatcher;
import com.zerog.neoessentials.commands.teleportation.HomeCommands;
import com.zerog.neoessentials.commands.teleportation.SpawnCommands;
import com.zerog.neoessentials.commands.teleportation.WarpCommands;
import com.zerog.neoessentials.teleportation.DirectTeleport.DirectTeleportCommands;
import com.zerog.neoessentials.teleportation.Misc.MiscTeleportCommands;
import com.zerog.neoessentials.teleportation.TeleportRequests.TeleportRequestCommands;
import net.minecraft.commands.CommandSourceStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central registry for all teleportation commands and systems
 */
public class TeleportationRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(TeleportationRegistry.class);
    
    /**
     * Register all teleportation commands
     */
    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        LOGGER.info("Registering teleportation commands...");
        
        try {
            // Register Home system commands
            HomeCommands.register(dispatcher);
            
            // Register Spawn system commands
            SpawnCommands.register(dispatcher);
            
            // Register Warp system commands
            WarpCommands.register(dispatcher);
            
            // Register TeleportRequest system commands
            TeleportRequestCommands.register(dispatcher);
            
            // Register DirectTeleport system commands (admin only)
            DirectTeleportCommands.register(dispatcher);
            
            // Register Misc teleport commands
            MiscTeleportCommands.register(dispatcher);
            
            LOGGER.info("Successfully registered all teleportation commands!");
            
        } catch (Exception e) {
            LOGGER.error("Failed to register teleportation commands", e);
            throw new RuntimeException("Teleportation system initialization failed", e);
        }
    }
    
    /**
     * Initialize all teleportation managers
     */
    public static void initializeManagers() {
        LOGGER.info("Initializing teleportation managers...");
        
        try {
            // Initialize managers (they will be created as singletons when first accessed)
            // This ensures they are ready to handle events
            
            LOGGER.info("Successfully initialized all teleportation managers!");
            
        } catch (Exception e) {
            LOGGER.error("Failed to initialize teleportation managers", e);
            throw new RuntimeException("Teleportation manager initialization failed", e);
        }
    }
    
    /**
     * Shutdown all teleportation systems
     */
    public static void shutdown() {
        LOGGER.info("Shutting down teleportation systems...");
        
        try {
            // Add shutdown logic for managers that need cleanup
            
            LOGGER.info("Successfully shut down all teleportation systems!");
            
        } catch (Exception e) {
            LOGGER.error("Error during teleportation system shutdown", e);
        }
    }
}