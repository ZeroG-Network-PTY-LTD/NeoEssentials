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
    private static boolean initialized = false;
    private static PermissionManager manager;
    private static boolean usingExternal = false;

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
                    // Detect specific permission plugin (LuckPerms, PermissionsEx, GroupManager, etc.)
                    String detectedPlugin = null;
                    try {
                        // LuckPerms
                        Class.forName("net.luckperms.api.LuckPerms");
                        detectedPlugin = "LuckPerms";
                    } catch (ClassNotFoundException ignored) {}
                    try {
                        // FTB Ranks
                        Class.forName("dev.ftb.mods.ftbranks.api.FTBRanksAPI");
                        detectedPlugin = "FTB Ranks";
                    } catch (ClassNotFoundException ignored) {}
                    try {
                        // Bukkit/Arclight
                        Class.forName("org.bukkit.Bukkit");
                        detectedPlugin = "Bukkit/Arclight";
                    } catch (ClassNotFoundException ignored) {}
                    if (detectedPlugin != null) {
                        LOGGER.info("✓ Detected permission plugin: {}", detectedPlugin);
                    }
                    LOGGER.info("✓ Using {} for ALL permission checks, prefixes, and suffixes", externalAdapter.getName());
                    PermissionAPI.setExternalAdapter(externalAdapter);
                    usingExternal = true;
                    // Internal permission system is NOT loaded or used
                    LOGGER.warn("  ⚠ Internal permissions.json will be IGNORED for all permission checks");
                    LOGGER.warn("  ⚠ All permissions/groups MUST be managed in {}", externalAdapter.getName());
                    // Do not load, create, or backup internal permissions.json
                    manager = null;
                    initialized = true;
                    LOGGER.info("✓ Permission system initialized with {} (internal groups loaded but NOT USED)", externalAdapter.getName());
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

            usingExternal = false;
            initialized = true;
            LOGGER.info("✓ Internal permission system initialized with {} groups",
                manager.getGroups().size());

            // Emit compatibility report even in internal mode (shows which perm mods are absent)
            AdapterCompatibilityChecker.generateReport(null);

            // Log loaded groups for debugging
            if (!manager.getGroups().isEmpty()) {
                LOGGER.info("Loaded permission groups:");
                for (PermissionGroup group : manager.getGroups()) {
                    LOGGER.info("  ├─ Group: '{}' ({} permissions, prefix: '{}')", group.getName(), group.getPermissions().size(), group.getPrefix());
                }
            }
            LOGGER.info("Permission System Configuration:");
            LOGGER.info("  ├─ Ops bypass permissions: {}", ConfigManager.getInstance().isOpsBypassPermissionsEnabled());
            LOGGER.info("  ├─ Permission caching: {}", ConfigManager.getInstance().isPermissionCacheEnabled());
            LOGGER.info("  ├─ Cache expiry: {} minutes", ConfigManager.getInstance().getPermissionCacheExpiryMinutes());
            LOGGER.info("  └─ Default group: {}", manager.getDefaultGroup());
            LOGGER.info("");
            // Validate permission nodes
            com.zerog.neoessentials.api.permissions.PermissionValidator.ValidationResult validation =
                com.zerog.neoessentials.api.permissions.PermissionValidator.validate(manager);
            if (validation.hasIssues()) {
                LOGGER.warn("⚠ PERMISSION VALIDATION FOUND {} ISSUES!", validation.getIssuesFound());
                LOGGER.warn("⚠ Some permissions may not work correctly!");
                LOGGER.warn("⚠ Check the validation output above for details.");
                for (String warning : validation.getWarnings()) {
                    LOGGER.warn(warning);
                }
                for (String suggestion : validation.getSuggestions()) {
                    LOGGER.warn(suggestion);
                }
            } else {
                LOGGER.info("✓ Permission validation passed - all permissions are properly configured");
            }
            LOGGER.info("═══════════════════════════════════════════════════════════");
        } catch (Exception e) {
            LOGGER.error("Failed to initialize permission system", e);
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

        // Try Arclight (Bukkit-compatible hybrid server)
        try {
            Class.forName("io.izzel.arclight.api.Arclight");
            LOGGER.info("  ├─ Arclight API class found, attempting to load Bukkit-compatible adapter...");
            BukkitSpongeAdapter adapter = new BukkitSpongeAdapter();
            if (adapter.isAvailable()) {
                LOGGER.info("  └─ ✓ Arclight adapter loaded successfully (Bukkit-compatible)");
                return adapter;
            } else {
                LOGGER.warn("  └─ ✗ Arclight detected but adapter not available");
            }
        } catch (ClassNotFoundException e) {
            LOGGER.info("  ├─ Arclight not found (ClassNotFoundException)");
        } catch (Exception e) {
            LOGGER.error("  └─ ✗ Failed to initialize Arclight adapter: {}", e.getMessage(), e);
        }

        // Try Bukkit/Sponge/Mohist
        try {
            Class.forName("org.bukkit.Bukkit");
            LOGGER.info("  ├─ Bukkit API class found (may be Mohist/Arclight), attempting to load adapter...");
            BukkitSpongeAdapter adapter = new BukkitSpongeAdapter();
            if (adapter.isAvailable()) {
                LOGGER.info("  └─ ✓ Bukkit/Mohist/Arclight adapter loaded successfully");
                return adapter;
            } else {
                LOGGER.warn("  └─ ✗ Bukkit/Mohist/Arclight detected but adapter not available");
            }
        } catch (ClassNotFoundException e) {
            LOGGER.info("  ├─ Bukkit/Mohist/Arclight not found (ClassNotFoundException)");
        } catch (Exception e) {
            LOGGER.error("  └─ ✗ Failed to initialize Bukkit/Mohist/Arclight adapter: {}", e.getMessage(), e);
        }
        // Try Sponge
        try {
            Class.forName("org.spongepowered.api.Sponge");
            LOGGER.info("  ├─ Sponge API class found, attempting to load adapter...");
            BukkitSpongeAdapter adapter = new BukkitSpongeAdapter();
            if (adapter.isAvailable()) {
                LOGGER.info("  └─ ✓ Sponge adapter loaded successfully");
                return adapter;
            } else {
                LOGGER.warn("  └─ ✗ Sponge detected but adapter not available");
            }
        } catch (ClassNotFoundException e) {
            LOGGER.info("  ├─ Sponge not found (ClassNotFoundException)");
        } catch (Exception e) {
            LOGGER.error("  └─ ✗ Failed to initialize Sponge adapter: {}", e.getMessage(), e);
        }

        // Try Mohist (Bukkit-compatible hybrid server)
        try {
            Class.forName("com.mohistmc.api.MohistAPI");
            LOGGER.info("  ├─ Mohist API class found, attempting to load Bukkit-compatible adapter...");
            BukkitSpongeAdapter adapter = new BukkitSpongeAdapter();
            if (adapter.isAvailable()) {
                LOGGER.info("  └─ ✓ Mohist adapter loaded successfully (Bukkit-compatible)");
                return adapter;
            } else {
                LOGGER.warn("  └─ ✗ Mohist detected but adapter not available");
            }
        } catch (ClassNotFoundException e) {
            LOGGER.info("  ├─ Mohist not found (ClassNotFoundException)");
        } catch (Exception e) {
            LOGGER.error("  └─ ✗ Failed to initialize Mohist adapter: {}", e.getMessage(), e);
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
            if (isUsingExternal()) {
                PermissionAPI.reload();
                LOGGER.info("External permission system reloaded");
                return;
            }
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
     * Check if the server is using an external permission system.
     */
    public static boolean isUsingExternal() {
        return usingExternal;
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
                try {
                    PermissionStorage.save(manager);
                } catch (Exception e) {
                    LOGGER.error("Failed to save permissions during shutdown", e);
                }
            }
            LOGGER.info("Permission system shutdown complete");
        } catch (Exception e) {
            LOGGER.error("Failed to shutdown permission system", e);
        }
    }
}

