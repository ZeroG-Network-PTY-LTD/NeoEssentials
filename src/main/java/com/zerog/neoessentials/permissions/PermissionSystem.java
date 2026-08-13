package com.zerog.neoessentials.permissions;

import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
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
            NeoLog.warn(LOGGER, LogCategory.PERMISSIONS,"Permission system already initialized, skipping");
            return;
        }

        try {
            NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"═══════════════════════════════════════════════════════════");
            NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"Initializing NeoEssentials Permission System...");
            NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"═══════════════════════════════════════════════════════════");

            // Check if we should use external permissions
            boolean useExternal = ConfigManager.getInstance().isExternalPermissionsEnabled();
            NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"External permissions enabled in config: {}", useExternal);

            if (useExternal) {
                NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"Attempting to detect external permission system...");
                ExternalPermissionAdapter externalAdapter = detectExternalPermissions();

                if (externalAdapter != null && externalAdapter.isAvailable()) {
                    NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"✓ External permission system detected: {}", externalAdapter.getName());
                    // Detect specific permission plugin (LuckPerms, PermissionsEx, GroupManager, etc.)
                    String detectedPlugin = null;
                    try {
                        // LuckPerms
                        Class.forName("net.luckperms.api.LuckPerms");
                        detectedPlugin = "LuckPerms";
                    } catch (ClassNotFoundException ignored) {
                        NeoLog.debug(LOGGER, LogCategory.PERMISSIONS, "LuckPerms class not present on classpath");
                    }
                    try {
                        // FTB Ranks
                        Class.forName("dev.ftb.mods.ftbranks.api.FTBRanksAPI");
                        detectedPlugin = "FTB Ranks";
                    } catch (ClassNotFoundException ignored) {
                        NeoLog.debug(LOGGER, LogCategory.PERMISSIONS, "FTB Ranks class not present on classpath");
                    }
                    try {
                        // Bukkit/Arclight
                        Class.forName("org.bukkit.Bukkit");
                        detectedPlugin = "Bukkit/Arclight";
                    } catch (ClassNotFoundException ignored) {
                        NeoLog.debug(LOGGER, LogCategory.PERMISSIONS, "Bukkit/Arclight class not present on classpath");
                    }
                    if (detectedPlugin != null) {
                        NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"✓ Detected permission plugin: {}", detectedPlugin);
                    }
                    NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"✓ Using {} for ALL permission checks, prefixes, and suffixes", externalAdapter.getName());
                    NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"  ├─ Vanilla OP fallback: {}", ConfigManager.getInstance().isVanillaOpFallbackEnabled());
                    NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"  └─ OP bypass (pre-check): {}", ConfigManager.getInstance().isOpsBypassPermissionsEnabled());
                    PermissionAPI.setExternalAdapter(externalAdapter);
                    usingExternal = true;
                    // Internal permission system is NOT loaded or used
                    NeoLog.warn(LOGGER, LogCategory.PERMISSIONS,"  ⚠ Internal permissions.json will be IGNORED for all permission checks");
                    NeoLog.warn(LOGGER, LogCategory.PERMISSIONS,"  ⚠ All permissions/groups MUST be managed in {}", externalAdapter.getName());
                    // Do not load, create, or backup internal permissions.json
                    manager = null;
                    initialized = true;
                    NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"✓ Permission system initialized with {} (internal groups loaded but NOT USED)", externalAdapter.getName());
                    NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"═══════════════════════════════════════════════════════════");
                    return;
                }

                NeoLog.warn(LOGGER, LogCategory.PERMISSIONS,"✗ External permissions enabled but no compatible system found!");
                NeoLog.warn(LOGGER, LogCategory.PERMISSIONS,"✗ Falling back to internal NeoEssentials permission system");
                NeoLog.warn(LOGGER, LogCategory.PERMISSIONS,"✗ To use LuckPerms: Install LuckPerms mod and set useExternalPermissions: true");
            } else {
                NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"External permissions disabled in config");
            }

            // Use internal permission system
            NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"Loading internal NeoEssentials permission system...");
            manager = new PermissionManager();

            // Load permissions from disk
            PermissionStorage.load(manager);

            // Register with PermissionAPI
            PermissionAPI.setManager(manager);

            usingExternal = false;
            initialized = true;
            NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"✓ Internal permission system initialized with {} groups",
                manager.getGroups().size());

            // Emit compatibility report even in internal mode (shows which perm mods are absent)
            AdapterCompatibilityChecker.generateReport(null);

            // Log loaded groups for debugging
            if (!manager.getGroups().isEmpty()) {
                NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"Loaded permission groups:");
                for (PermissionGroup group : manager.getGroups()) {
                    NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"  ├─ Group: '{}' ({} permissions, prefix: '{}')", group.getName(), group.getPermissions().size(), group.getPrefix());
                }
            }
            NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"Permission System Configuration:");
            NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"  ├─ Ops bypass permissions: {}", ConfigManager.getInstance().isOpsBypassPermissionsEnabled());
            NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"  ├─ Vanilla OP fallback:    {}", ConfigManager.getInstance().isVanillaOpFallbackEnabled());
            NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"  ├─ Permission caching:     {}", ConfigManager.getInstance().isPermissionCacheEnabled());
            NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"  ├─ Cache expiry:           {} minutes", ConfigManager.getInstance().getPermissionCacheExpiryMinutes());
            NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"  └─ Default group:          {}", manager.getDefaultGroup());
            NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"");
            // Validate permission nodes
            com.zerog.neoessentials.api.permissions.PermissionValidator.ValidationResult validation =
                com.zerog.neoessentials.api.permissions.PermissionValidator.validate(manager);
            if (validation.hasIssues()) {
                NeoLog.warn(LOGGER, LogCategory.PERMISSIONS,"⚠ PERMISSION VALIDATION FOUND {} ISSUES!", validation.getIssuesFound());
                NeoLog.warn(LOGGER, LogCategory.PERMISSIONS,"⚠ Some NeoEssentials permissions may not work correctly!");
                NeoLog.warn(LOGGER, LogCategory.PERMISSIONS,"⚠ Check the validation output above for details.");
                NeoLog.warn(LOGGER, LogCategory.PERMISSIONS,"⚠ Note: {} external mod permission(s) were skipped (they are valid).",
                        validation.getExternalSkipped());
                for (String warning : validation.getWarnings()) {
                    NeoLog.warn(LOGGER, LogCategory.PERMISSIONS,warning);
                }
                for (String suggestion : validation.getSuggestions()) {
                    NeoLog.warn(LOGGER, LogCategory.PERMISSIONS,suggestion);
                }
            } else {
                NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"✓ Permission validation passed - all NeoEssentials permissions are properly configured");
                if (validation.getExternalSkipped() > 0) {
                    NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"  └─ {} external mod permission(s) accepted without validation (e.g. worldedit.*, ftbranks.*, etc.)",
                            validation.getExternalSkipped());
                }
            }
            NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"═══════════════════════════════════════════════════════════");
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.PERMISSIONS,"╔══════════════════════════════════════════════════════════════╗");
            NeoLog.error(LOGGER, LogCategory.PERMISSIONS,"║  PERMISSION SYSTEM FAILED TO INITIALIZE                      ║");
            NeoLog.error(LOGGER, LogCategory.PERMISSIONS,"║  Error: {}  ║",
                    padRight(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName(), 54));
            NeoLog.error(LOGGER, LogCategory.PERMISSIONS,"║  Activating EMERGENCY MODE — OPs will have all permissions.  ║");
            NeoLog.error(LOGGER, LogCategory.PERMISSIONS,"║  Fix the config/permission issue, then run: /neoe reload      ║");
            NeoLog.error(LOGGER, LogCategory.PERMISSIONS,"╚══════════════════════════════════════════════════════════════╝");
            NeoLog.error(LOGGER, LogCategory.PERMISSIONS,"Full stack trace:", e);
            com.zerog.neoessentials.api.permissions.PermissionAPI.setEmergencyMode(true);
            com.zerog.neoessentials.util.SupportLinks.markProblemDetected();
            com.zerog.neoessentials.util.SupportLinks.logConsole(LOGGER, true);
            initialized = true; // Mark initialised so reload can recover later
        }
    }

    /**
     * Detect and load external permission system if available.
     */
    private static ExternalPermissionAdapter detectExternalPermissions() {
        NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"Scanning for external permission systems...");

        // Try LuckPerms first
        try {
            Class.forName("net.luckperms.api.LuckPerms");
            NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"  ├─ LuckPerms API class found, attempting to load adapter...");
            LuckPermsAdapter adapter = new LuckPermsAdapter();
            if (adapter.isAvailable()) {
                NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"  └─ ✓ LuckPerms adapter loaded successfully");
                return adapter;
            } else {
                NeoLog.warn(LOGGER, LogCategory.PERMISSIONS,"  └─ ✗ LuckPerms detected but adapter not available");
            }
        } catch (ClassNotFoundException e) {
            NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"  ├─ LuckPerms not found (ClassNotFoundException)");
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.PERMISSIONS,"  └─ ✗ Failed to initialize LuckPerms adapter: {}", e.getMessage(), e);
        }
        
        // Try FTB Ranks
        try {
            Class.forName("dev.ftb.mods.ftbranks.api.FTBRanksAPI");
            NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"  ├─ FTB Ranks API class found, attempting to load adapter...");
            FtbRanksAdapter adapter = new FtbRanksAdapter();
            if (adapter.isAvailable()) {
                NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"  └─ ✓ FTB Ranks adapter loaded successfully");
                return adapter;
            } else {
                NeoLog.warn(LOGGER, LogCategory.PERMISSIONS,"  └─ ✗ FTB Ranks detected but adapter not available");
            }
        } catch (ClassNotFoundException e) {
            NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"  ├─ FTB Ranks not found (ClassNotFoundException)");
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.PERMISSIONS,"  └─ ✗ Failed to initialize FTB Ranks adapter: {}", e.getMessage(), e);
        }

        // Try Arclight (Bukkit-compatible hybrid server)
        try {
            Class.forName("io.izzel.arclight.api.Arclight");
            NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"  ├─ Arclight API class found, attempting to load Bukkit-compatible adapter...");
            BukkitSpongeAdapter adapter = new BukkitSpongeAdapter();
            if (adapter.isAvailable()) {
                NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"  └─ ✓ Arclight adapter loaded successfully (Bukkit-compatible)");
                return adapter;
            } else {
                NeoLog.warn(LOGGER, LogCategory.PERMISSIONS,"  └─ ✗ Arclight detected but adapter not available");
            }
        } catch (ClassNotFoundException e) {
            NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"  ├─ Arclight not found (ClassNotFoundException)");
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.PERMISSIONS,"  └─ ✗ Failed to initialize Arclight adapter: {}", e.getMessage(), e);
        }

        // Try Bukkit/Sponge/Mohist
        try {
            Class.forName("org.bukkit.Bukkit");
            NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"  ├─ Bukkit API class found (may be Mohist/Arclight), attempting to load adapter...");
            BukkitSpongeAdapter adapter = new BukkitSpongeAdapter();
            if (adapter.isAvailable()) {
                NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"  └─ ✓ Bukkit/Mohist/Arclight adapter loaded successfully");
                return adapter;
            } else {
                NeoLog.warn(LOGGER, LogCategory.PERMISSIONS,"  └─ ✗ Bukkit/Mohist/Arclight detected but adapter not available");
            }
        } catch (ClassNotFoundException e) {
            NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"  ├─ Bukkit/Mohist/Arclight not found (ClassNotFoundException)");
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.PERMISSIONS,"  └─ ✗ Failed to initialize Bukkit/Mohist/Arclight adapter: {}", e.getMessage(), e);
        }
        // Try Sponge
        try {
            Class.forName("org.spongepowered.api.Sponge");
            NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"  ├─ Sponge API class found, attempting to load adapter...");
            BukkitSpongeAdapter adapter = new BukkitSpongeAdapter();
            if (adapter.isAvailable()) {
                NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"  └─ ✓ Sponge adapter loaded successfully");
                return adapter;
            } else {
                NeoLog.warn(LOGGER, LogCategory.PERMISSIONS,"  └─ ✗ Sponge detected but adapter not available");
            }
        } catch (ClassNotFoundException e) {
            NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"  ├─ Sponge not found (ClassNotFoundException)");
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.PERMISSIONS,"  └─ ✗ Failed to initialize Sponge adapter: {}", e.getMessage(), e);
        }

        // Try Mohist (Bukkit-compatible hybrid server)
        try {
            Class.forName("com.mohistmc.api.MohistAPI");
            NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"  ├─ Mohist API class found, attempting to load Bukkit-compatible adapter...");
            BukkitSpongeAdapter adapter = new BukkitSpongeAdapter();
            if (adapter.isAvailable()) {
                NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"  └─ ✓ Mohist adapter loaded successfully (Bukkit-compatible)");
                return adapter;
            } else {
                NeoLog.warn(LOGGER, LogCategory.PERMISSIONS,"  └─ ✗ Mohist detected but adapter not available");
            }
        } catch (ClassNotFoundException e) {
            NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"  ├─ Mohist not found (ClassNotFoundException)");
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.PERMISSIONS,"  └─ ✗ Failed to initialize Mohist adapter: {}", e.getMessage(), e);
        }

        NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"  └─ No external permission system detected");
        return null;
    }

    /**
     * Reload the permission system from disk.
     */
    public static void reload() {
        if (!initialized) {
            NeoLog.warn(LOGGER, LogCategory.PERMISSIONS,"Cannot reload: permission system not initialized");
            initialize();
            return;
        }

        // If emergency mode is active, a full re-initialisation is needed to recover.
        if (com.zerog.neoessentials.api.permissions.PermissionAPI.isEmergencyMode()) {
            NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"Emergency mode active — running full permission system re-initialisation...");
            initialized = false;
            manager = null;
            usingExternal = false;
            initialize();
            return;
        }

        try {
            NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"Reloading permission system...");
            if (isUsingExternal()) {
                PermissionAPI.reload();
                NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"External permission system reloaded");
                return;
            }
            if (manager != null) {
                manager.reload();
            }
            // Deactivate emergency mode on a successful reload (edge case: mode set externally)
            com.zerog.neoessentials.api.permissions.PermissionAPI.setEmergencyMode(false);
            NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"Permission system reloaded successfully");
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.PERMISSIONS,"Failed to reload permission system", e);
            throw new RuntimeException("Permission reload failed", e);
        }
    }

    /**
     * Get the permission manager instance.
     */
    public static PermissionManager getManager() {
        if (!initialized) {
            NeoLog.error(LOGGER, LogCategory.PERMISSIONS,"Permission system not initialized! Call initialize() first!");
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
     * Returns {@code true} if the permission system is running in emergency
     * (OP-only) mode due to an initialisation failure.
     */
    @SuppressWarnings("unused") // public API — may be queried by dashboard or commands
    public static boolean isEmergencyMode() {
        return com.zerog.neoessentials.api.permissions.PermissionAPI.isEmergencyMode();
    }

    /**
     * Shutdown the permission system (save data).
     */
    public static void shutdown() {
        if (!initialized) {
            return;
        }

        try {
            NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"Shutting down permission system...");
            // Save internal manager data
            if (manager != null) {
                try {
                    PermissionStorage.save(manager);
                } catch (Exception e) {
                    NeoLog.error(LOGGER, LogCategory.PERMISSIONS,"Failed to save permissions during shutdown", e);
                }
            }
            // Unsubscribe external adapter listeners (e.g. LuckPerms event bus)
            // so no permission-sync callbacks fire after the server starts stopping.
            var externalAdapter = com.zerog.neoessentials.api.permissions.PermissionAPI.getExternalAdapter();
            if (externalAdapter instanceof LuckPermsAdapter luckPermsAdapter) {
                try {
                    luckPermsAdapter.shutdown();
                } catch (Exception e) {
                    NeoLog.error(LOGGER, LogCategory.PERMISSIONS,"Failed to shut down LuckPerms adapter", e);
                }
            }
            NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"Permission system shutdown complete");
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.PERMISSIONS,"Failed to shutdown permission system", e);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    @SuppressWarnings("SameParameterValue")
    private static String padRight(String s, int width) {
        if (s == null) s = "";
        if (s.length() >= width) return s.substring(0, width);
        return s + " ".repeat(width - s.length());
    }
}

