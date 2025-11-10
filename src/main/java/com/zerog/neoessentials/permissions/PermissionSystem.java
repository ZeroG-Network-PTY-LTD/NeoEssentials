package com.zerog.neoessentials.permissions;

import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central initialization and management for the permission system.
 */
public class PermissionSystem {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionSystem.class);
    private static PermissionManager manager;
    private static boolean initialized = false;

    /**
     * Initialize the permission system on server start.
     * This MUST be called before any permission checks.
     */
    public static void initialize() {
        if (initialized) {
            LOGGER.warn("Permission system already initialized, skipping");
            return;
        }

        try {
            LOGGER.info("Initializing NeoEssentials permission system...");
            
            // Check if we should use external permissions
                boolean useExternal = ConfigManager.getInstance().isExternalPermissionsEnabled();
            
            if (useExternal) {
                LOGGER.info("Attempting to detect external permission system...");
                ExternalPermissionAdapter externalAdapter = detectExternalPermissions();
                
                if (externalAdapter != null) {
                    LOGGER.info("Using external permission system: {}", externalAdapter.getName());
                    PermissionAPI.setExternalAdapter(externalAdapter);
                    
                    // Still create internal manager for fallback
                    manager = new PermissionManager();
                    PermissionStorage.load(manager);
                    PermissionAPI.setManager(manager);
                    
                    initialized = true;
                    return;
                }
                
                LOGGER.warn("External permissions enabled but no compatible system found, using internal system");
            }
            
            // Use internal permission system
            LOGGER.info("Using internal NeoEssentials permission system");
            manager = new PermissionManager();
            
            // Load permissions from disk
            PermissionStorage.load(manager);
            
            // Register with PermissionAPI
            PermissionAPI.setManager(manager);
            
            initialized = true;
            LOGGER.info("Permission system initialized successfully with {} groups", 
                manager.getGroups().size());
            
            // Log loaded groups for debugging
            for (PermissionGroup group : manager.getGroups()) {
                LOGGER.info("  Loaded group '{}' with {} permissions and prefix '{}'", 
                    group.getName(), group.getPermissions().size(), group.getPrefix());
            }
            
            // Log important config settings
            LOGGER.info("  Ops bypass permissions: {}", 
                ConfigManager.getInstance().isOpsBypassPermissionsEnabled());
            LOGGER.info("  Permission caching: {}", 
                ConfigManager.getInstance().isPermissionCacheEnabled());
            LOGGER.info("  Cache expiry: {} minutes", 
                ConfigManager.getInstance().getPermissionCacheExpiryMinutes());
            
        } catch (Exception e) {
            LOGGER.error("CRITICAL: Failed to initialize permission system!", e);
            throw new RuntimeException("Permission system initialization failed", e);
        }
    }

    /**
     * Detect and load external permission system if available.
     */
    private static ExternalPermissionAdapter detectExternalPermissions() {
        // Try LuckPerms first
        try {
            Class.forName("net.luckperms.api.LuckPerms");
            LOGGER.info("LuckPerms detected, attempting to load adapter...");
            LuckPermsAdapter adapter = new LuckPermsAdapter();
            if (adapter.isAvailable()) {
                return adapter;
            }
        } catch (ClassNotFoundException e) {
            LOGGER.debug("LuckPerms not found");
        } catch (Exception e) {
            LOGGER.error("Failed to initialize LuckPerms adapter", e);
        }
        
        // Try FTB Ranks
        try {
            Class.forName("dev.ftb.mods.ftbranks.api.FTBRanksAPI");
            LOGGER.info("FTB Ranks detected, attempting to load adapter...");
            FtbRanksAdapter adapter = new FtbRanksAdapter();
            if (adapter.isAvailable()) {
                return adapter;
            }
        } catch (ClassNotFoundException e) {
            LOGGER.debug("FTB Ranks not found");
        } catch (Exception e) {
            LOGGER.error("Failed to initialize FTB Ranks adapter", e);
        }
        
        return null;
    }

    /**
     * Reload the permission system from disk.
     */
    public static void reload() {
        if (!initialized) {
            LOGGER.warn("Cannot reload: permission system not initialized");
            initialize();
            return;
        }

        try {
            LOGGER.info("Reloading permission system...");
            
            // If using external, try to reload it
            if (PermissionAPI.isUsingExternal()) {
                PermissionAPI.reload();
            }
            
            // Always reload internal manager (used as fallback)
            if (manager != null) {
                manager.reload();
            }
            
            LOGGER.info("Permission system reloaded successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to reload permission system", e);
            throw new RuntimeException("Permission reload failed", e);
        }
    }

    /**
     * Get the permission manager instance.
     */
    public static PermissionManager getManager() {
        if (!initialized) {
            LOGGER.error("Permission system not initialized! Call initialize() first!");
            throw new IllegalStateException("Permission system not initialized");
        }
        return manager;
    }

    /**
     * Check if the permission system is initialized.
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * Shutdown the permission system (save data).
     */
    public static void shutdown() {
        if (!initialized) {
            return;
        }

        try {
            LOGGER.info("Shutting down permission system...");
            
            // Save internal manager data
            if (manager != null) {
                PermissionStorage.save(manager);
            }
            
            LOGGER.info("Permission system shutdown complete");
        } catch (Exception e) {
            LOGGER.error("Failed to save permissions during shutdown", e);
        }
    }
}
