package com.zerog.neoessentials.permissions;

import com.zerog.neoessentials.api.permissions.PermissionRegistry;
import com.zerog.neoessentials.api.permissions.PermissionScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;

/**
 * Adapter for PermissionsEX integration.
 * This class helps expose NeoEssentials permissions to PermissionsEX for proper tab completion.
 */
public class PermissionsEXAdapter implements ExternalPermissionAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionsEXAdapter.class);
    private static final String PEX_PERMISSIONS_FILE = com.zerog.neoessentials.util.ResourceUtil.DATA_DIR + "pex-permissions.txt";

    public PermissionsEXAdapter() {
        // Export permissions on initialization
        exportPermissionsForPEX();
    }

    @Override
    public boolean hasPermission(UUID uuid, String permission) {
        // PermissionsEX should handle permission checking through its own system
        // This is just a fallback
        return false;
    }

    @Override
    public String getPrefix(UUID uuid) {
        // PermissionsEX handles prefixes through its own system
        return null;
    }

    @Override
    public String getSuffix(UUID uuid) {
        // PermissionsEX handles suffixes through its own system
        return null;
    }

    @Override
    public void reload() {
        LOGGER.info("Reloading PermissionsEX adapter - re-exporting permissions");
        exportPermissionsForPEX();
    }

    @Override
    public String getName() {
        return "PermissionsEX";
    }
    
    @Override
    public boolean isAvailable() {
        // PermissionsEX adapter is always "available" but just for exporting permissions
        // It doesn't actually handle permission checks
        return false;
    }
    
    /**
     * Export all NeoEssentials permissions to a file that PermissionsEX can read
     * This helps with tab completion in PermissionsEX commands
     */
    private void exportPermissionsForPEX() {
        try {
            LOGGER.info("Exporting NeoEssentials permissions for PermissionsEX tab completion...");
            
            // Get all permissions from registry and scanner
            PermissionRegistry registry = PermissionRegistry.getInstance();
            PermissionScanner scanner = PermissionScanner.getInstance();
            
            // Force a fresh scan to ensure we have the latest permissions
            scanner.scanForPermissions();
            
            Set<String> allPermissions = registry.getAllPermissions();
            Set<String> discoveredPermissions = scanner.getDiscoveredPermissions();
            
            // Create data directory if it doesn't exist
            File dataDir = new File(com.zerog.neoessentials.util.ResourceUtil.DATA_DIR);
            if (!dataDir.exists()) {
                if (!dataDir.mkdirs()) {
                    LOGGER.error("Failed to create data directory: {}", dataDir.getAbsolutePath());
                }
            }
            
            // Write permissions to file
            File permFile = new File(PEX_PERMISSIONS_FILE);
            try (FileWriter writer = new FileWriter(permFile)) {
                writer.write("# NeoEssentials Permissions Export for PermissionsEX\n");
                writer.write("# Generated automatically - DO NOT EDIT MANUALLY\n");
                writer.write("# This file helps PermissionsEX provide tab completion for NeoEssentials permissions\n");
                writer.write("# Use these permissions with /pex group <group> add <permission>\n\n");
                
                writer.write("# === REGISTERED PERMISSIONS ===\n");
                allPermissions.stream().sorted().forEach(perm -> {
                    try {
                        writer.write(perm + "\n");
                    } catch (IOException e) {
                        LOGGER.error("Error writing permission: " + perm, e);
                    }
                });
                
                writer.write("\n# === AUTO-DISCOVERED PERMISSIONS ===\n");
                discoveredPermissions.stream()
                    .filter(perm -> !allPermissions.contains(perm))
                    .sorted()
                    .forEach(perm -> {
                        try {
                            writer.write(perm + "\n");
                        } catch (IOException e) {
                            LOGGER.error("Error writing discovered permission: " + perm, e);
                        }
                    });
                
                writer.write("\n# === WILDCARD PERMISSIONS ===\n");
                writer.write("neoessentials.*\n");
                writer.write("neoessentials.admin.*\n");
                writer.write("neoessentials.economy.*\n");
                writer.write("neoessentials.teleport.*\n");
                writer.write("neoessentials.chat.*\n");
                writer.write("neoessentials.kits.*\n");
                writer.write("neoessentials.moderation.*\n");
                writer.write("neoessentials.utilities.*\n");
            }
            
            LOGGER.info("Successfully exported {} registered and {} discovered permissions to {}", 
                allPermissions.size(), discoveredPermissions.size(), PEX_PERMISSIONS_FILE);
                
            // Log instructions for server administrators
            LOGGER.info("=== PermissionsEX Integration Help ===");
            LOGGER.info("For PermissionsEX tab completion to work properly:");
            LOGGER.info("1. Install a permissions plugin that supports permission registration");
            LOGGER.info("2. Use the exported permissions file: {}", PEX_PERMISSIONS_FILE);
            LOGGER.info("3. Or manually register permissions with your permission plugin");
            LOGGER.info("4. Use '/neoessentials-permissions export pex' to regenerate this file");
            
        } catch (Exception e) {
            LOGGER.error("Failed to export permissions for PermissionsEX", e);
        }
    }
    
    /**
     * Export permissions in different formats
     */
    public void exportPermissions(String format, File outputFile) throws IOException {
        PermissionRegistry registry = PermissionRegistry.getInstance();
        PermissionScanner scanner = PermissionScanner.getInstance();
        scanner.scanForPermissions();
        
        Set<String> allPermissions = registry.getAllPermissions();
        Set<String> discoveredPermissions = scanner.getDiscoveredPermissions();
        
        try (FileWriter writer = new FileWriter(outputFile)) {
            switch (format.toLowerCase()) {
                case "pex":
                case "permissionsex":
                    exportForPermissionsEX(writer, allPermissions, discoveredPermissions);
                    break;
                case "luckperms":
                    exportForLuckPerms(writer, allPermissions, discoveredPermissions);
                    break;
                case "yaml":
                    exportAsYAML(writer, allPermissions, discoveredPermissions);
                    break;
                case "json":
                    exportAsJSON(writer, allPermissions, discoveredPermissions);
                    break;
                default:
                    exportAsText(writer, allPermissions, discoveredPermissions);
            }
        }
        
        LOGGER.info("Exported permissions in {} format to {}", format, outputFile.getAbsolutePath());
    }
    
    private void exportForPermissionsEX(FileWriter writer, Set<String> registered, Set<String> discovered) throws IOException {
        writer.write("# PermissionsEX format - use with /pex group <group> add <permission>\n");
        registered.stream().sorted().forEach(perm -> {
            try {
                writer.write(perm + "\n");
            } catch (IOException e) {
                // Handle silently
            }
        });
        discovered.stream().filter(p -> !registered.contains(p)).sorted().forEach(perm -> {
            try {
                writer.write(perm + "\n");
            } catch (IOException e) {
                // Handle silently
            }
        });
    }
    
    private void exportForLuckPerms(FileWriter writer, Set<String> registered, Set<String> discovered) throws IOException {
        writer.write("# LuckPerms format - use with /lp group <group> permission set <permission> true\n");
        registered.stream().sorted().forEach(perm -> {
            try {
                writer.write("/lp group default permission set " + perm + " false\n");
            } catch (IOException e) {
                // Handle silently
            }
        });
    }
    
    private void exportAsYAML(FileWriter writer, Set<String> registered, Set<String> discovered) throws IOException {
        writer.write("neoessentials_permissions:\n");
        writer.write("  registered:\n");
        registered.stream().sorted().forEach(perm -> {
            try {
                writer.write("    - \"" + perm + "\"\n");
            } catch (IOException e) {
                // Handle silently
            }
        });
        writer.write("  discovered:\n");
        discovered.stream().filter(p -> !registered.contains(p)).sorted().forEach(perm -> {
            try {
                writer.write("    - \"" + perm + "\"\n");
            } catch (IOException e) {
                // Handle silently
            }
        });
    }
    
    private void exportAsJSON(FileWriter writer, Set<String> registered, Set<String> discovered) throws IOException {
        writer.write("{\n");
        writer.write("  \"neoessentials_permissions\": {\n");
        writer.write("    \"registered\": [\n");
        String[] regArray = registered.stream().sorted().toArray(String[]::new);
        for (int i = 0; i < regArray.length; i++) {
            writer.write("      \"" + regArray[i] + "\"" + (i < regArray.length - 1 ? "," : "") + "\n");
        }
        writer.write("    ],\n");
        writer.write("    \"discovered\": [\n");
        String[] discArray = discovered.stream().filter(p -> !registered.contains(p)).sorted().toArray(String[]::new);
        for (int i = 0; i < discArray.length; i++) {
            writer.write("      \"" + discArray[i] + "\"" + (i < discArray.length - 1 ? "," : "") + "\n");
        }
        writer.write("    ]\n");
        writer.write("  }\n");
        writer.write("}\n");
    }
    
    private void exportAsText(FileWriter writer, Set<String> registered, Set<String> discovered) throws IOException {
        writer.write("=== NeoEssentials Permissions ===\n\n");
        writer.write("Registered Permissions:\n");
        registered.stream().sorted().forEach(perm -> {
            try {
                writer.write(perm + "\n");
            } catch (IOException e) {
                // Handle silently
            }
        });
        writer.write("\nDiscovered Permissions:\n");
        discovered.stream().filter(p -> !registered.contains(p)).sorted().forEach(perm -> {
            try {
                writer.write(perm + "\n");
            } catch (IOException e) {
                // Handle silently
            }
        });
    }
}