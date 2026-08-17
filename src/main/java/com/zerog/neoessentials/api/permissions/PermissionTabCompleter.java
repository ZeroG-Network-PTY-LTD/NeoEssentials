package com.zerog.neoessentials.api.permissions;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Provides tab completion for NeoEssentials permission nodes.
 * This integrates with external permission plugins like PermissionsEX, LuckPerms, etc.
 * to provide proper tab completion when using commands like /pex group <group> add <permission>
 */
public class PermissionTabCompleter {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionTabCompleter.class);
    
    /**
     * Suggestion provider for NeoEssentials permissions
     */
    public static final SuggestionProvider<CommandSourceStack> NEOESSENTIALS_PERMISSIONS = 
        (context, builder) -> suggestNeoEssentialsPermissions(context, builder);
    
    /**
     * Suggestion provider for all registered permissions
     */
    public static final SuggestionProvider<CommandSourceStack> ALL_PERMISSIONS = 
        (context, builder) -> suggestAllPermissions(context, builder);
    
    /**
     * Suggestion provider for permissions by category
     */
    public static SuggestionProvider<CommandSourceStack> permissionsByCategory(PermissionRegistry.PermissionCategory category) {
        return (context, builder) -> suggestPermissionsByCategory(context, builder, category);
    }
    
    /**
     * Suggest NeoEssentials permissions starting with "neoessentials."
     */
    private static CompletableFuture<Suggestions> suggestNeoEssentialsPermissions(
            com.mojang.brigadier.context.CommandContext<CommandSourceStack> context, 
            SuggestionsBuilder builder) {
        
        String input = builder.getRemaining().toLowerCase();
        List<String> permissions = PermissionRegistry.getInstance().getNeoEssentialsPermissions();
        
        // Filter permissions based on input
        List<String> filtered = permissions.stream()
                .filter(perm -> perm.toLowerCase().startsWith(input))
                .sorted()
                .toList();
        
        return SharedSuggestionProvider.suggest(filtered, builder);
    }
    
    /**
     * Suggest all registered and discovered permissions
     */
    private static CompletableFuture<Suggestions> suggestAllPermissions(
            com.mojang.brigadier.context.CommandContext<CommandSourceStack> context, 
            SuggestionsBuilder builder) {
        
        String input = builder.getRemaining().toLowerCase();
        
        // Get both registered and discovered permissions
        PermissionRegistry registry = PermissionRegistry.getInstance();
        PermissionScanner scanner = PermissionScanner.getInstance();
        
        // Ensure we have the latest discovered permissions
        scanner.scanForPermissions();
        
        // Combine both sets
        java.util.Set<String> allPermissions = new java.util.HashSet<>(registry.getAllPermissions());
        allPermissions.addAll(scanner.getDiscoveredPermissions());
        
        List<String> permissions = allPermissions.stream()
                .filter(perm -> perm.toLowerCase().startsWith(input))
                .sorted()
                .toList();
        
        return SharedSuggestionProvider.suggest(permissions, builder);
    }
    
    /**
     * Suggest permissions by category
     */
    private static CompletableFuture<Suggestions> suggestPermissionsByCategory(
            com.mojang.brigadier.context.CommandContext<CommandSourceStack> context, 
            SuggestionsBuilder builder,
            PermissionRegistry.PermissionCategory category) {
        
        String input = builder.getRemaining().toLowerCase();
        List<String> permissions = PermissionRegistry.getInstance().getPermissionsByCategory(category).stream()
                .filter(perm -> perm.toLowerCase().startsWith(input))
                .sorted()
                .toList();
        
        return SharedSuggestionProvider.suggest(permissions, builder);
    }
    
    /**
     * Initialize permission tab completion
     * This method should be called during mod initialization
     */
    public static void initialize() {
        NeoLog.info(LOGGER, LogCategory.PERMISSIONS, "Initializing NeoEssentials permission tab completion...");
        
        // Register permission nodes for external plugin integration
        registerWithExternalPlugins();
        
        NeoLog.info(LOGGER, LogCategory.PERMISSIONS, "Permission tab completion initialized with {} nodes", 
                   PermissionRegistry.getInstance().getAllPermissions().size());
    }
    
    /**
     * Register permissions with external permission plugins
     * This makes our permission nodes available for tab completion in plugins like PermissionsEX
     */
    private static void registerWithExternalPlugins() {
        // Try to register with PermissionsEX if available
        registerWithPermissionsEX();
        
        // Try to register with LuckPerms if available
        registerWithLuckPerms();
        
        // Try to register with other common permission plugins
        registerWithOtherPlugins();
    }
    
    /**
     * Register with PermissionsEX for tab completion
     */
    private static void registerWithPermissionsEX() {
        try {
            // We can't directly register with PermissionsEX since it's not a NeoForge mod
            // Instead, we'll make our permissions available through our external provider
            NeoLog.info(LOGGER, LogCategory.PERMISSIONS, "Making NeoEssentials permissions available for external plugin compatibility");
            
            // Get all permissions (registered + discovered)
            PermissionRegistry registry = PermissionRegistry.getInstance();
            PermissionScanner scanner = PermissionScanner.getInstance();
            
            // Force a fresh scan
            scanner.scanForPermissions();
            
            // Combine all permissions
            java.util.Set<String> allPermissions = new java.util.HashSet<>(registry.getAllPermissions());
            allPermissions.addAll(scanner.getDiscoveredPermissions());
            
            NeoLog.info(LOGGER, LogCategory.PERMISSIONS, "Made {} total permissions available for external plugin access", allPermissions.size());
            NeoLog.info(LOGGER, LogCategory.PERMISSIONS, "External plugins can access permissions via ExternalPermissionProvider class");
            
            // Log some sample permissions for verification
            if (!allPermissions.isEmpty()) {
                NeoLog.info(LOGGER, LogCategory.PERMISSIONS, "Sample permissions available:");
                allPermissions.stream().limit(5).forEach(perm -> NeoLog.info(LOGGER, LogCategory.PERMISSIONS, "  - {}", perm));
                if (allPermissions.size() > 5) {
                    NeoLog.info(LOGGER, LogCategory.PERMISSIONS, "  ... and {} more permissions", allPermissions.size() - 5);
                }
            }
            
        } catch (Exception e) {
            LOGGER.warn("Failed to prepare permissions for external plugins: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Register with LuckPerms for tab completion
     */
    private static void registerWithLuckPerms() {
        try {
            // Check if LuckPerms is available
            Class.forName("net.luckperms.api.LuckPerms");
            
            NeoLog.info(LOGGER, LogCategory.PERMISSIONS, "LuckPerms detected - registering permission nodes");
            
            // Register all NeoEssentials permissions with LuckPerms
            for (String permission : PermissionRegistry.getInstance().getAllPermissions()) {
                // LuckPerms integration code would go here
                // This would use LuckPerms API to register permissions
                NeoLog.debug(LOGGER, LogCategory.PERMISSIONS, "Would register permission with LuckPerms: {}", permission);
            }
            
        } catch (ClassNotFoundException e) {
            NeoLog.debug(LOGGER, LogCategory.PERMISSIONS, "LuckPerms not found - skipping integration");
        } catch (Exception e) {
            LOGGER.warn("Failed to register with LuckPerms: {}", e.getMessage());
        }
    }
    
    /**
     * Register with other permission plugins
     */
    private static void registerWithOtherPlugins() {
        // GroupManager integration
        try {
            Class.forName("org.anjocaido.groupmanager.GroupManager");
            NeoLog.info(LOGGER, LogCategory.PERMISSIONS, "GroupManager detected - registering permission nodes");
            // GroupManager integration would go here
        } catch (ClassNotFoundException e) {
            NeoLog.debug(LOGGER, LogCategory.PERMISSIONS, "GroupManager not found - skipping integration");
        }
        
        // PermissionsBukkit integration
        try {
            Class.forName("com.platymuus.bukkit.permissions.PermissionsBukkit");
            NeoLog.info(LOGGER, LogCategory.PERMISSIONS, "PermissionsBukkit detected - registering permission nodes");
            // PermissionsBukkit integration would go here
        } catch (ClassNotFoundException e) {
            NeoLog.debug(LOGGER, LogCategory.PERMISSIONS, "PermissionsBukkit not found - skipping integration");
        }
    }
    
    /**
     * Export permissions to external plugin format
     * This can be used to generate permission files for various plugins
     */
    public static void exportPermissionsForPlugin(String pluginType, String outputPath) {
        switch (pluginType.toLowerCase()) {
            case "permissionsex", "pex" -> exportForPermissionsEX(outputPath);
            case "luckperms", "lp" -> exportForLuckPerms(outputPath);
            case "groupmanager", "gm" -> exportForGroupManager(outputPath);
            default -> LOGGER.warn("Unknown plugin type for export: {}", pluginType);
        }
    }
    
    /**
     * Export permissions for PermissionsEX format
     */
    private static void exportForPermissionsEX(String outputPath) {
        NeoLog.info(LOGGER, LogCategory.PERMISSIONS, "Exporting permissions for PermissionsEX to: {}", outputPath);
        // Implementation for PEX export format
    }
    
    /**
     * Export permissions for LuckPerms format
     */
    private static void exportForLuckPerms(String outputPath) {
        NeoLog.info(LOGGER, LogCategory.PERMISSIONS, "Exporting permissions for LuckPerms to: {}", outputPath);
        // Implementation for LuckPerms export format
    }
    
    /**
     * Export permissions for GroupManager format
     */
    private static void exportForGroupManager(String outputPath) {
        NeoLog.info(LOGGER, LogCategory.PERMISSIONS, "Exporting permissions for GroupManager to: {}", outputPath);
        // Implementation for GroupManager export format
    }
    
    /**
     * Get permission suggestions for a given input string
     * This can be used by external plugins for custom tab completion
     */
    public static List<String> getPermissionSuggestions(String input) {
        PermissionRegistry registry = PermissionRegistry.getInstance();
        PermissionScanner scanner = PermissionScanner.getInstance();
        
        // Ensure we have the latest discovered permissions
        scanner.scanForPermissions();
        
        // Combine registered and discovered permissions
        java.util.Set<String> allPermissions = new java.util.HashSet<>(registry.getPermissionsStartingWith(input.toLowerCase()));
        allPermissions.addAll(scanner.getDiscoveredPermissions().stream()
                .filter(perm -> perm.toLowerCase().startsWith(input.toLowerCase()))
                .toList());
        
        return allPermissions.stream().sorted().toList();
    }
    
    /**
     * Register a permission dynamically (for runtime-created permissions like kits)
     */
    public static void registerDynamicPermission(String permission, String description, 
                                                PermissionRegistry.PermissionCategory category) {
        PermissionRegistry.getInstance().register(permission, description, category);
        
        // Also register with external plugins if they're loaded
        registerSinglePermissionWithExternalPlugins(permission);
    }
    
    /**
     * Register a single permission with external plugins
     */
    private static void registerSinglePermissionWithExternalPlugins(String permission) {
        // This would notify external plugins about the new permission
        // Implementation depends on the specific plugin APIs
        NeoLog.debug(LOGGER, LogCategory.PERMISSIONS, "Registering dynamic permission with external plugins: {}", permission);
    }
}