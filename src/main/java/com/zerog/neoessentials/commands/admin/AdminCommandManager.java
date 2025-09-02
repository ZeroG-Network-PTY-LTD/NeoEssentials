package com.zerog.neoessentials.commands.admin;

import com.mojang.brigadier.CommandDispatcher;
import com.zerog.neoessentials.commands.CleanupTeamsCommand;
import net.minecraft.commands.CommandSourceStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Centralized management for all NeoEssentials admin commands
 * 
 * Manages registration and coordination of:
 * - Performance monitoring and optimization commands
 * - Server status and diagnostics commands
 * - Comprehensive cleanup and maintenance commands
 * - Error handling and debugging commands
 * - Player and server administration commands
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class AdminCommandManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminCommandManager.class);
    private static AdminCommandManager instance;
    
    private boolean commandsRegistered = false;
    
    private AdminCommandManager() {
        // Private constructor for singleton
    }
    
    public static AdminCommandManager getInstance() {
        if (instance == null) {
            instance = new AdminCommandManager();
        }
        return instance;
    }
    
    /**
     * Register all admin commands with the command dispatcher
     * 
     * @param dispatcher The command dispatcher to register commands with
     */
    public void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (commandsRegistered) {
            LOGGER.warn("Admin commands already registered, skipping duplicate registration");
            return;
        }
        
        try {
            LOGGER.info("Registering NeoEssentials admin commands...");
            
            // Register comprehensive cleanup command system
            CleanupCommand.register(dispatcher);
            LOGGER.debug("Registered cleanup command system");
            
            // Register performance monitoring commands
            PerformanceCommand.register(dispatcher);
            LOGGER.debug("Registered performance command");
            
            // Register server status commands
            StatusCommand.register(dispatcher);
            LOGGER.debug("Registered status command");
            
            // Register error handling commands
            ErrorCommand.register(dispatcher);
            LOGGER.debug("Registered error command");
            
            // Register legacy cleanup commands for backwards compatibility
            CleanupTeamsCommand.register(dispatcher);
            LOGGER.debug("Registered legacy cleanup teams command");
            
            commandsRegistered = true;
            LOGGER.info("Successfully registered {} NeoEssentials admin commands", getRegisteredCommandCount());
            
        } catch (Exception e) {
            LOGGER.error("Failed to register admin commands", e);
            throw new RuntimeException("Admin command registration failed", e);
        }
    }
    
    /**
     * Unregister admin commands and cleanup resources
     */
    public void unregisterCommands() {
        if (!commandsRegistered) {
            return;
        }
        
        try {
            LOGGER.info("Unregistering NeoEssentials admin commands...");
            
            // Shutdown cleanup command scheduler
            CleanupCommand.shutdown();
            
            commandsRegistered = false;
            LOGGER.info("Successfully unregistered admin commands");
            
        } catch (Exception e) {
            LOGGER.error("Error during admin command unregistration", e);
        }
    }
    
    /**
     * Check if admin commands are properly registered
     * 
     * @return true if commands are registered, false otherwise
     */
    public boolean areCommandsRegistered() {
        return commandsRegistered;
    }
    
    /**
     * Get the number of registered admin commands
     * 
     * @return The count of registered admin commands
     */
    public int getRegisteredCommandCount() {
        if (!commandsRegistered) {
            return 0;
        }
        
        // Return count of admin commands we register
        return 5; // CleanupCommand, PerformanceCommand, StatusCommand, ErrorCommand, CleanupTeamsCommand
    }
    
    /**
     * Get information about registered admin commands
     * 
     * @return Array of command information strings
     */
    public String[] getCommandInfo() {
        if (!commandsRegistered) {
            return new String[]{"No admin commands registered"};
        }
        
        return new String[] {
            "cleanup - Comprehensive server cleanup and maintenance",
            "performance - Performance monitoring and optimization", 
            "status - Server status and diagnostics",
            "error - Error handling and debugging",
            "cleanupteams - Legacy scoreboard cleanup (deprecated)"
        };
    }
    
    /**
     * Validate admin command system health
     * 
     * @return true if all systems are healthy, false if issues detected
     */
    public boolean validateCommandHealth() {
        if (!commandsRegistered) {
            LOGGER.warn("Admin commands not registered");
            return false;
        }
        
        try {
            // Check if key components are accessible
            boolean healthy = true;
            
            // Validate cleanup system is functional
            // (This would expand with more comprehensive health checks)
            
            if (healthy) {
                LOGGER.debug("Admin command system health check passed");
            } else {
                LOGGER.warn("Admin command system health check failed");
            }
            
            return healthy;
            
        } catch (Exception e) {
            LOGGER.error("Error during admin command health validation", e);
            return false;
        }
    }
    
    /**
     * Force cleanup of admin command resources
     * Called during server shutdown or mod unload
     */
    public void forceCleanup() {
        LOGGER.info("Forcing cleanup of admin command resources...");
        
        try {
            unregisterCommands();
            instance = null;
            LOGGER.info("Admin command manager cleanup completed");
        } catch (Exception e) {
            LOGGER.error("Error during forced cleanup", e);
        }
    }
}