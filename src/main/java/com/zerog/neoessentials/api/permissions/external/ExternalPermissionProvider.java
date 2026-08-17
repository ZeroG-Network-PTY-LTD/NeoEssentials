package com.zerog.neoessentials.api.permissions.external;

import com.zerog.neoessentials.api.permissions.PermissionRegistry;
import com.zerog.neoessentials.api.permissions.PermissionScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;

import java.util.List;
import java.util.Set;
import java.util.HashSet;


/**
 * External Permission Provider for integration with permission plugins like PermissionsEX.
 * This class provides a static API that external plugins can use to discover NeoEssentials permissions.
 */
public class ExternalPermissionProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalPermissionProvider.class);
    
    // Cache permissions to avoid repeated scanning during tab completion
    private static List<String> cachedPermissions = null;
    private static long lastCacheTime = 0;
    private static final long CACHE_DURATION_MS = 30000; // 30 seconds
    
    /**
     * Get all NeoEssentials permissions for external plugin integration.
     * This method is designed to be called by external permission plugins for tab completion.
     * 
     * @return List of all NeoEssentials permission nodes
     */
    public static List<String> getAllNeoEssentialsPermissions() {
        try {
            // Check if we have a valid cache
            long now = System.currentTimeMillis();
            if (cachedPermissions != null && (now - lastCacheTime) < CACHE_DURATION_MS) {
                return cachedPermissions;
            }
            
            // Refresh cache
            PermissionRegistry registry = PermissionRegistry.getInstance();
            PermissionScanner scanner = PermissionScanner.getInstance();
            
            // Only scan if cache is empty or very old
            if (cachedPermissions == null) {
                scanner.scanForPermissions();
            }
            
            // Combine registered and discovered permissions
            Set<String> allPermissions = new HashSet<>(registry.getAllPermissions());
            allPermissions.addAll(scanner.getDiscoveredPermissions());
            
            cachedPermissions = allPermissions.stream()
                    .sorted()
                    .toList();
                    
            lastCacheTime = now;
            
            NeoLog.debug(LOGGER, LogCategory.PERMISSIONS, "External plugin requested {} NeoEssentials permissions", cachedPermissions.size());
            return cachedPermissions;
            
        } catch (Exception e) {
            LOGGER.error("Failed to provide permissions to external plugin", e);
            return cachedPermissions != null ? cachedPermissions : List.of();
        }
    }
    
    /**
     * Get permissions starting with a specific prefix.
     * Useful for tab completion in external plugins.
     * 
     * @param prefix The prefix to filter by
     * @return List of permissions starting with the prefix
     */
    //noinspection unused
    @SuppressWarnings("unused")
    public static List<String> getPermissionsStartingWith(String prefix) {
        try {
            return getAllNeoEssentialsPermissions().stream()
                    .filter(perm -> perm.toLowerCase().startsWith(prefix.toLowerCase()))
                    .toList();
                    
        } catch (Exception e) {
            LOGGER.error("Failed to filter permissions for external plugin", e);
            return List.of();
        }
    }
    
    /**
     * Check if a permission exists in NeoEssentials.
     * 
     * @param permission The permission to check
     * @return true if the permission exists
     */
    public static boolean hasPermission(String permission) {
        try {
            return getAllNeoEssentialsPermissions().contains(permission);
        } catch (Exception e) {
            LOGGER.error("Failed to check permission for external plugin", e);
            return false;
        }
    }
    
    /**
     * Get permissions by category for external plugins.
     * 
     * @param category The category (teleport, economy, kits, chat, admin, misc)
     * @return List of permissions in that category
     */
    public static List<String> getPermissionsByCategory(String category) {
        try {
            return getAllNeoEssentialsPermissions().stream()
                    .filter(perm -> {
                        String[] parts = perm.split("\\.");
                        return parts.length >= 2 && parts[1].equalsIgnoreCase(category);
                    })
                    .toList();
                    
        } catch (Exception e) {
            LOGGER.error("Failed to get permissions by category for external plugin", e);
            return List.of();
        }
    }
    
    /**
     * Clear the permission cache and force a refresh.
     */
    public static void clearCache() {
        cachedPermissions = null;
        lastCacheTime = 0;
        NeoLog.debug(LOGGER, LogCategory.PERMISSIONS, "Permission cache cleared");
    }
    
    /**
     * Initialize the external permission provider.
     * This should be called during mod initialization.
     */
    public static void initialize() {
        NeoLog.info(LOGGER, LogCategory.PERMISSIONS, "Initializing External Permission Provider for PermissionsEX integration...");
        
        try {
            // Clear any existing cache and force fresh scan
            clearCache();
            
            // Pre-load all permissions to ensure they're cached
            List<String> permissions = getAllNeoEssentialsPermissions();
            NeoLog.info(LOGGER, LogCategory.PERMISSIONS, "External Permission Provider initialized with {} permissions available", permissions.size());
            
            // Print some key permissions for verification
            NeoLog.info(LOGGER, LogCategory.PERMISSIONS, "Sample permissions available for external plugins:");
            permissions.stream()
                    .limit(10)
                    .forEach(perm -> NeoLog.info(LOGGER, LogCategory.PERMISSIONS, "  - {}", perm));
            
            if (permissions.size() > 10) {
                NeoLog.info(LOGGER, LogCategory.PERMISSIONS, "  ... and {} more permissions", permissions.size() - 10);
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to initialize External Permission Provider", e);
        }
    }
    
    /**
     * Get all wildcard permissions that external plugins might want to use.
     * 
     * @return List of wildcard permission patterns
     */
    public static List<String> getWildcardPermissions() {
        return List.of(
            "neoessentials.*",
            "neoessentials.teleport.*",
            "neoessentials.economy.*",
            "neoessentials.kits.*",
            "neoessentials.chat.*",
            "neoessentials.admin.*",
            "neoessentials.utility.*",
            "neoessentials.teleport.admin.*",
            "neoessentials.teleport.home.*",
            "neoessentials.teleport.spawn.*",
            "neoessentials.teleport.warp.*",
            "neoessentials.teleport.request.*",
            "neoessentials.teleport.misc.*"
        );
    }
    
    /**
     * Export all permissions in a format suitable for PermissionsEX.
     * This generates a complete list that can be imported into PermissionsEX.
     * 
     * @return Formatted string for PermissionsEX import
     */
    //noinspection unused
    @SuppressWarnings("unused")
    public static String exportForPermissionsEX() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("# NeoEssentials Permissions for PermissionsEX\n");
        sb.append("# Generated automatically - copy these permissions into your PermissionsEX configuration\n");
        sb.append("# Total permissions: ").append(getAllNeoEssentialsPermissions().size()).append("\n\n");
        
        // Group permissions by category
        String[] categories = {"teleport", "economy", "kits", "chat", "utility", "admin"};
        
        for (String category : categories) {
            List<String> categoryPerms = getPermissionsByCategory(category);
            if (!categoryPerms.isEmpty()) {
                sb.append("# ").append(category.toUpperCase()).append(" PERMISSIONS (").append(categoryPerms.size()).append(")\n");
                for (String perm : categoryPerms) {
                    sb.append(perm).append("\n");
                }
                sb.append("\n");
            }
        }
        
        // Add wildcard permissions
        sb.append("# WILDCARD PERMISSIONS\n");
        for (String wildcard : getWildcardPermissions()) {
            sb.append(wildcard).append("\n");
        }
        
        return sb.toString();
    }
}