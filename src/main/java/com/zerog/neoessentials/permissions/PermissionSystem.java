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
            LOGGER.info("═══════════════════════════════════════════════════════════");
            LOGGER.info("Initializing NeoEssentials Permission System...");
            LOGGER.info("═══════════════════════════════════════════════════════════");

            // Check if we should use external permissions
            boolean useExternal = ConfigManager.getInstance().isExternalPermissionsEnabled();
            LOGGER.info("External permissions enabled in config: {}", useExternal);

            if (useExternal) {
                LOGGER.info("Attempting to detect external permission system...");
                ExternalPermissionAdapter externalAdapter = detectExternalPermissions();
                
                if (externalAdapter != null && externalAdapter.isAvailable()) {
                    LOGGER.info("✓ External permission system detected: {}", externalAdapter.getName());
                    LOGGER.info("✓ Using {} for all permission checks", externalAdapter.getName());
                    PermissionAPI.setExternalAdapter(externalAdapter);
                    
                    // Still create internal manager for fallback and prefix/suffix if needed
                    LOGGER.info("✓ Loading internal permission system as fallback...");
                    manager = new PermissionManager();
                    PermissionStorage.load(manager);
                    PermissionAPI.setManager(manager);
                    
                    initialized = true;
                    LOGGER.info("✓ Permission system initialized with {} (internal fallback: {} groups)",
                        externalAdapter.getName(), manager.getGroups().size());
                    LOGGER.info("═══════════════════════════════════════════════════════════");
                    return;
                }
                
                LOGGER.warn("✗ External permissions enabled but no compatible system found!");
                LOGGER.warn("✗ Falling back to internal NeoEssentials permission system");
                LOGGER.warn("✗ To use LuckPerms: Install LuckPerms mod and set useExternalPermissions: true");
            } else {
                LOGGER.info("External permissions disabled in config");
            }
            
            // Use internal permission system
            LOGGER.info("Loading internal NeoEssentials permission system...");
            manager = new PermissionManager();
            
            // Load permissions from disk
            PermissionStorage.load(manager);
            
            // Register with PermissionAPI
            PermissionAPI.setManager(manager);
            
            initialized = true;
            LOGGER.info("✓ Internal permission system initialized with {} groups",
                manager.getGroups().size());
            
            // Log loaded groups for debugging
            if (!manager.getGroups().isEmpty()) {
                LOGGER.info("Loaded permission groups:");
                for (PermissionGroup group : manager.getGroups()) {
                    LOGGER.info("  ├─ Group: '{}' ({} permissions, prefix: '{}')",
                        group.getName(), group.getPermissions().size(),
                        group.getPrefix() != null ? group.getPrefix() : "none");
                }
            } else {
                LOGGER.warn("✗ No permission groups loaded! Creating default group...");
                // Create default group if none exist
                PermissionGroup defaultGroup = new PermissionGroup("default");
                manager.addGroup(defaultGroup);
                PermissionStorage.save(manager);
            }
            
            // Log important config settings
            LOGGER.info("Permission System Configuration:");
            LOGGER.info("  ├─ Ops bypass permissions: {}",
                ConfigManager.getInstance().isOpsBypassPermissionsEnabled());
            LOGGER.info("  ├─ Permission caching: {}",
                ConfigManager.getInstance().isPermissionCacheEnabled());
            LOGGER.info("  ├─ Cache expiry: {} minutes",
                ConfigManager.getInstance().getPermissionCacheExpiryMinutes());
            LOGGER.info("  └─ Default group: {}", manager.getDefaultGroup());

            // Validate permission nodes
            LOGGER.info("");
            com.zerog.neoessentials.api.permissions.PermissionValidator.ValidationResult validation =
                com.zerog.neoessentials.api.permissions.PermissionValidator.validate(manager);

            if (validation.hasIssues()) {
                LOGGER.warn("⚠ PERMISSION VALIDATION FOUND {} ISSUES!", validation.getIssuesFound());
                LOGGER.warn("⚠ Some permissions may not work correctly!");
                LOGGER.warn("⚠ Check the validation output above for details.");
            } else {
                LOGGER.info("✓ Permission validation passed - all permissions are properly configured");
            }

            LOGGER.info("═══════════════════════════════════════════════════════════");

        } catch (Exception e) {
            LOGGER.error("═══════════════════════════════════════════════════════════");
            LOGGER.error("✗ CRITICAL: Failed to initialize permission system!");
            LOGGER.error("═══════════════════════════════════════════════════════════");
            LOGGER.error("Error details:", e);
            throw new RuntimeException("Permission system initialization failed", e);
        }
    }

    /**
     * Detect and load external permission system if available.
     */
    private static ExternalPermissionAdapter detectExternalPermissions() {
        LOGGER.info("Scanning for external permission systems...");

        // Try LuckPerms first
        try {
            Class.forName("net.luckperms.api.LuckPerms");
            LOGGER.info("  ├─ LuckPerms API class found, attempting to load adapter...");
            LuckPermsAdapter adapter = new LuckPermsAdapter();
            if (adapter.isAvailable()) {
                LOGGER.info("  └─ ✓ LuckPerms adapter loaded successfully");
                return adapter;
            } else {
                LOGGER.warn("  └─ ✗ LuckPerms detected but adapter not available");
            }
        } catch (ClassNotFoundException e) {
            LOGGER.info("  ├─ LuckPerms not found (ClassNotFoundException)");
        } catch (Exception e) {
            LOGGER.error("  └─ ✗ Failed to initialize LuckPerms adapter: {}", e.getMessage(), e);
        }
        
        // Try FTB Ranks
        try {
            Class.forName("dev.ftb.mods.ftbranks.api.FTBRanksAPI");
            LOGGER.info("  ├─ FTB Ranks API class found, attempting to load adapter...");
            FtbRanksAdapter adapter = new FtbRanksAdapter();
            if (adapter.isAvailable()) {
                LOGGER.info("  └─ ✓ FTB Ranks adapter loaded successfully");
                return adapter;
            } else {
                LOGGER.warn("  └─ ✗ FTB Ranks detected but adapter not available");
            }
        } catch (ClassNotFoundException e) {
            LOGGER.info("  ├─ FTB Ranks not found (ClassNotFoundException)");
        } catch (Exception e) {
            LOGGER.error("  └─ ✗ Failed to initialize FTB Ranks adapter: {}", e.getMessage(), e);
        }
        
        LOGGER.info("  └─ No external permission system detected");
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
