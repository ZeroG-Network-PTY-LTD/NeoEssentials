package com.zerog.neoessentials.permissions;

import com.zerog.neoessentials.NeoEssentials;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Central manager for permission handlers.
 * This class manages all registered permission handlers and dispatches
 * permission checks to the appropriate handler based on availability.
 */
public class PermissionHandlerManager {
    
    private final List<PermissionHandler> handlers = new ArrayList<>();
    private static PermissionHandlerManager instance;
      /**
     * Initialize the permission handler manager and register available handlers
     */    
    public PermissionHandlerManager() {
        // Register LuckPerms handler
        registerHandler(new LuckPermsPermissionHandler());
        
        // Register FTB Ranks handler
        registerHandler(new FTBRanksPermissionHandler());
        
        // Log registered handlers
        for (PermissionHandler handler : handlers) {
            if (handler.isAvailable()) {
                NeoEssentials.LOGGER.info("Registered permission handler: {}", handler.getName());
            } else {
                NeoEssentials.LOGGER.debug("Permission handler unavailable: {}", handler.getName());
            }
        }
        
        // If no handlers are available, use our built-in permission system
        if (getAvailableHandlers().isEmpty()) {
            NeoEssentials.LOGGER.info("No external permission handlers available. Using NeoEssentials built-in permission system.");
            registerHandler(new VanillaPermissionHandler());
        }
        
        // Log the final active handler
        List<PermissionHandler> activeHandlers = getAvailableHandlers();
        if (!activeHandlers.isEmpty()) {
            NeoEssentials.LOGGER.info("Active permission system: {}", activeHandlers.get(0).getName());
        } else {
            NeoEssentials.LOGGER.error("No permission handlers available. Permission checks will fail!");
        }
        
        instance = this;
    }
    
    /**
     * Register a new permission handler
     * 
     * @param handler The handler to register
     */    public void registerHandler(PermissionHandler handler) {
        if (handler != null) {
            handlers.add(handler);
            if (handler.isAvailable()) {
                NeoEssentials.LOGGER.info("Registered permission handler: {}", handler.getName());
            } else {
                NeoEssentials.LOGGER.debug("Permission handler unavailable: {} - skipping", handler.getName());
            }
        }
    }
    
    /**
     * Check if a player has a permission
     * This will try each registered handler in order until one returns true
     * 
     * @param player The player to check
     * @param permission The permission node
     * @return true if the player has permission, false otherwise
     */
    public boolean hasPermission(ServerPlayer player, String permission) {
        // Operators always have permission
        if (player.hasPermissions(2)) {
            return true;
        }
        
        // Check each handler in order
        for (PermissionHandler handler : handlers) {
            if (handler.isAvailable() && handler.hasPermission(player, permission)) {
                return true;
            }
        }
        
        // If no handler grants the permission, return false
        return false;
    }
    
    /**
     * Get all available permission handlers
     * 
     * @return List of available handlers
     */
    public List<PermissionHandler> getAvailableHandlers() {
        return handlers.stream()
            .filter(PermissionHandler::isAvailable)
            .toList();
    }
    
    /**
     * Get the singleton instance of the manager
     * 
     * @return The permission handler manager instance
     */
    public static PermissionHandlerManager getInstance() {
        if (instance == null) {
            instance = new PermissionHandlerManager();
        }
        return instance;
    }
}
