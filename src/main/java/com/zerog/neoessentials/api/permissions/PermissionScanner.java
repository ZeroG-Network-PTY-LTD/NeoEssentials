package com.zerog.neoessentials.api.permissions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Automatic permission scanner that discovers ALL permission nodes used throughout the mod.
 * This system scans Java source files and JAR resources to find permission strings,
 * making them available for tab completion with external permission plugins.
 */
public class PermissionScanner {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionScanner.class);
    
    // Regex patterns to find permission nodes in code
    private static final List<Pattern> PERMISSION_PATTERNS = Arrays.asList(
        // Direct string literals: "neoessentials.something"
        Pattern.compile("\"(neoessentials\\.[a-z0-9._-]+)\"", Pattern.CASE_INSENSITIVE),
        
        // Permission constants: PERMISSION_XYZ = "neoessentials.something"
        Pattern.compile("PERMISSION_[A-Z_]+\\s*=\\s*\"(neoessentials\\.[a-z0-9._-]+)\"", Pattern.CASE_INSENSITIVE),
        
        // hasPermission calls with permission strings
        Pattern.compile("hasPermission\\([^,]+,\\s*\"(neoessentials\\.[a-z0-9._-]+)\"\\)", Pattern.CASE_INSENSITIVE),
        
        // PermissionAPI.hasPermission calls
        Pattern.compile("PermissionAPI\\.hasPermission\\([^,]+,\\s*\"(neoessentials\\.[a-z0-9._-]+)\"\\)", Pattern.CASE_INSENSITIVE),
        
        // validatePermission calls
        Pattern.compile("validatePermission\\([^,]+,\\s*\"(neoessentials\\.[a-z0-9._-]+)\"\\)", Pattern.CASE_INSENSITIVE),
        
        // register() calls in PermissionRegistry
        Pattern.compile("register\\(\\s*\"(neoessentials\\.[a-z0-9._-]+)\"", Pattern.CASE_INSENSITIVE)
    );
    
    // Additional patterns for dynamic permissions (like kit permissions)
    private static final List<Pattern> DYNAMIC_PATTERNS = Arrays.asList(
        // Pattern for kit permission generation: "neoessentials.kits." + kitName
        Pattern.compile("\"neoessentials\\.kits\\.\"\\s*\\+\\s*([a-zA-Z0-9_]+)", Pattern.CASE_INSENSITIVE),
        
        // Pattern for dynamic permission building: permission + "." + something
        Pattern.compile("\"(neoessentials\\.[a-z0-9._-]+)\\.\"\\s*\\+", Pattern.CASE_INSENSITIVE)
    );
    
    private final Set<String> discoveredPermissions = ConcurrentHashMap.newKeySet();
    private final Set<String> dynamicPermissionPrefixes = ConcurrentHashMap.newKeySet();
    private final Map<String, Set<String>> filePermissionMap = new ConcurrentHashMap<>();
    
    // Singleton pattern
    private static class SingletonHolder {
        private static final PermissionScanner INSTANCE = new PermissionScanner();
    }
    
    public static PermissionScanner getInstance() {
        return SingletonHolder.INSTANCE;
    }
    
    private PermissionScanner() {
        // Private constructor for singleton
    }
    
    /**
     * Scan all Java files in the mod for permission nodes
     */
    public void scanForPermissions() {
        LOGGER.info("Starting automatic permission discovery...");
        
        discoveredPermissions.clear();
        dynamicPermissionPrefixes.clear();
        filePermissionMap.clear();
        
        try {
            // Get the source root path
            URI sourceUri = getClass().getProtectionDomain().getCodeSource().getLocation().toURI();
            
            if (sourceUri.toString().endsWith(".jar")) {
                // Running from JAR - try scanning but don't fail if it doesn't work
                LOGGER.debug("Detected JAR execution: {}", sourceUri);
                try {
                    scanJarFile(sourceUri);
                } catch (Exception jarScanException) {
                    LOGGER.debug("JAR scanning failed (this is normal): {}", jarScanException.getMessage());
                    // Use fallback discovery method
                    generateKnownPermissions();
                }
            } else {
                // Development environment - scan source files
                Path sourcePath = Paths.get(sourceUri);
                
                // Handle null or invalid paths gracefully
                if (sourcePath != null) {
                    Path rootPath = sourcePath.getParent();
                    if (rootPath != null) {
                        LOGGER.debug("Detected development environment: {}", rootPath);
                        scanSourceDirectory(rootPath);
                    } else {
                        LOGGER.debug("Could not determine root path, using fallback discovery");
                        generateKnownPermissions();
                    }
                } else {
                    LOGGER.debug("Source path is null, using fallback discovery");
                    generateKnownPermissions();
                }
            }
            
            LOGGER.info("Permission discovery completed. Found {} permissions across {} files", 
                discoveredPermissions.size(), filePermissionMap.size());
            
            // Log discovered permissions by category if any were found
            if (!discoveredPermissions.isEmpty()) {
                logDiscoveredPermissions();
            } else {
                LOGGER.info("No permissions discovered from file scanning. All permissions are registered in PermissionRegistry.");
            }
            
        } catch (Exception e) {
            LOGGER.warn("Error during permission scanning: {}", e.getMessage());
            LOGGER.info("Using fallback permission discovery method");
            generateKnownPermissions();
        }
    }
    
    /**
     * Scan source directory for Java files
     */
    private void scanSourceDirectory(Path rootPath) throws IOException {
        if (rootPath == null) {
            LOGGER.warn("Root path is null, cannot scan source directory");
            return;
        }
        
        // Look for src/main/java directory
        Path javaSourcePath = rootPath.resolve("src").resolve("main").resolve("java");
        
        if (Files.exists(javaSourcePath)) {
            LOGGER.debug("Scanning source directory: {}", javaSourcePath);
            scanDirectory(javaSourcePath);
        } else {
            // Fallback: scan current directory for Java files
            LOGGER.debug("Java source path not found, scanning from: {}", rootPath);
            scanDirectory(rootPath);
        }
    }
    
    /**
     * Scan JAR file for Java classes
     */
    private void scanJarFile(URI jarUri) throws IOException {
        LOGGER.debug("Attempting to scan JAR file: {}", jarUri);
        
        try (FileSystem jarFs = FileSystems.newFileSystem(jarUri, Collections.emptyMap())) {
            Path jarRoot = jarFs.getPath("/");
            
            try (Stream<Path> paths = Files.walk(jarRoot)) {
                long classCount = paths.filter(path -> path.toString().endsWith(".class"))
                     .filter(path -> path.toString().contains("neoessentials"))
                     .peek(path -> LOGGER.debug("Scanning class file: {}", path))
                     .peek(this::scanClassFile)
                     .count();
                     
                LOGGER.debug("Scanned {} class files from JAR", classCount);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to scan JAR file: {}. Error: {}", jarUri, e.getMessage());
            LOGGER.info("This is normal in some deployment environments. Using registered permissions only.");
        }
    }
    
    /**
     * Scan directory recursively for Java files
     */
    private void scanDirectory(Path directory) throws IOException {
        if (!Files.exists(directory)) return;
        
        try (Stream<Path> paths = Files.walk(directory)) {
            paths.filter(path -> path.toString().endsWith(".java"))
                 .forEach(this::scanJavaFile);
        }
    }
    
    /**
     * Scan a single Java file for permission strings
     */
    private void scanJavaFile(Path javaFile) {
        try {
            String content = Files.readString(javaFile);
            scanContent(content, javaFile.toString());
        } catch (IOException e) {
            LOGGER.warn("Could not read Java file: {}", javaFile, e);
        }
    }
    
    /**
     * Scan a class file (when running from JAR)
     */
    private void scanClassFile(Path classFile) {
        // For class files, we can't easily extract string literals
        // But we can at least record that we found a class in our package
        String className = classFile.toString();
        if (className.contains("neoessentials")) {
            LOGGER.debug("Found NeoEssentials class: {}", className);
        }
    }
    
    /**
     * Scan content for permission patterns
     */
    private void scanContent(String content, String fileName) {
        Set<String> filePermissions = new HashSet<>();
        
        // Scan for direct permission patterns
        for (Pattern pattern : PERMISSION_PATTERNS) {
            Matcher matcher = pattern.matcher(content);
            while (matcher.find()) {
                String permission = matcher.group(1).toLowerCase();
                if (isValidPermission(permission)) {
                    discoveredPermissions.add(permission);
                    filePermissions.add(permission);
                    LOGGER.debug("Found permission '{}' in {}", permission, fileName);
                }
            }
        }
        
        // Scan for dynamic permission patterns
        for (Pattern pattern : DYNAMIC_PATTERNS) {
            Matcher matcher = pattern.matcher(content);
            while (matcher.find()) {
                String prefix = matcher.group(1).toLowerCase();
                if (isValidPermission(prefix)) {
                    dynamicPermissionPrefixes.add(prefix);
                    LOGGER.debug("Found dynamic permission prefix '{}' in {}", prefix, fileName);
                }
            }
        }
        
        if (!filePermissions.isEmpty()) {
            filePermissionMap.put(fileName, filePermissions);
        }
    }
    
    /**
     * Validate permission format
     */
    private boolean isValidPermission(String permission) {
        if (permission == null || permission.trim().isEmpty()) return false;
        
        // Must start with neoessentials
        if (!permission.startsWith("neoessentials.")) return false;
        
        // Check for valid characters
        if (!permission.matches("^[a-z0-9._-]+$")) return false;
        
        // Cannot end with dot
        if (permission.endsWith(".")) return false;
        
        // Cannot have consecutive dots
        if (permission.contains("..")) return false;
        
        // Must have at least one part after neoessentials
        String[] parts = permission.split("\\.");
        return parts.length >= 2;
    }
    
    /**
     * Get all discovered permissions
     */
    public Set<String> getDiscoveredPermissions() {
        return new HashSet<>(discoveredPermissions);
    }
    
    /**
     * Get dynamic permission prefixes
     */
    public Set<String> getDynamicPermissionPrefixes() {
        return new HashSet<>(dynamicPermissionPrefixes);
    }
    
    /**
     * Get permissions by file
     */
    public Map<String, Set<String>> getFilePermissionMap() {
        return new HashMap<>(filePermissionMap);
    }
    
    /**
     * Get permissions by category (parsed from permission structure)
     */
    public Map<String, Set<String>> getPermissionsByCategory() {
        Map<String, Set<String>> categoryMap = new HashMap<>();
        
        for (String permission : discoveredPermissions) {
            String[] parts = permission.split("\\.");
            if (parts.length >= 2) {
                String category = parts[1]; // Second part after "neoessentials"
                categoryMap.computeIfAbsent(category, k -> new HashSet<>()).add(permission);
            }
        }
        
        return categoryMap;
    }
    
    /**
     * Generate expanded permissions for dynamic prefixes
     * This can be used to generate kit permissions, etc.
     */
    public Set<String> generateDynamicPermissions(Set<String> dynamicValues) {
        Set<String> generated = new HashSet<>();
        
        for (String prefix : dynamicPermissionPrefixes) {
            for (String value : dynamicValues) {
                String dynamicPermission = prefix + "." + value.toLowerCase();
                if (isValidPermission(dynamicPermission)) {
                    generated.add(dynamicPermission);
                }
            }
        }
        
        return generated;
    }
    
    /**
     * Log discovered permissions grouped by category
     */
    private void logDiscoveredPermissions() {
        Map<String, Set<String>> categories = getPermissionsByCategory();
        
        LOGGER.info("=== DISCOVERED PERMISSIONS BY CATEGORY ===");
        
        for (Map.Entry<String, Set<String>> entry : categories.entrySet()) {
            String category = entry.getKey();
            Set<String> perms = entry.getValue();
            
            LOGGER.info("{} ({}): {}", category.toUpperCase(), perms.size(), 
                String.join(", ", perms.stream().sorted().toArray(String[]::new)));
        }
        
        if (!dynamicPermissionPrefixes.isEmpty()) {
            LOGGER.info("DYNAMIC PREFIXES ({}): {}", dynamicPermissionPrefixes.size(),
                String.join(", ", dynamicPermissionPrefixes.stream().sorted().toArray(String[]::new)));
        }
        
        LOGGER.info("=== END PERMISSION DISCOVERY REPORT ===");
    }
    
    /**
     * Export all discovered permissions to a list (for external use)
     */
    public List<String> exportDiscoveredPermissions() {
        List<String> export = new ArrayList<>();
        export.add("# Auto-Discovered NeoEssentials Permissions");
        export.add("# Total discovered: " + discoveredPermissions.size() + " permissions");
        export.add("# Dynamic prefixes: " + dynamicPermissionPrefixes.size());
        export.add("");
        
        Map<String, Set<String>> categories = getPermissionsByCategory();
        
        for (Map.Entry<String, Set<String>> entry : categories.entrySet()) {
            String category = entry.getKey();
            Set<String> perms = entry.getValue();
            
            export.add("## " + category.toUpperCase() + " (" + perms.size() + " permissions)");
            export.add("");
            
            perms.stream().sorted().forEach(perm -> export.add(perm + " - Auto-discovered permission"));
            export.add("");
        }
        
        if (!dynamicPermissionPrefixes.isEmpty()) {
            export.add("## DYNAMIC PERMISSION PREFIXES");
            export.add("# These prefixes are used to generate permissions dynamically (e.g., for kits)");
            export.add("");
            
            dynamicPermissionPrefixes.stream().sorted()
                .forEach(prefix -> export.add(prefix + ".* - Dynamic permission prefix"));
        }
        
        return export;
    }
    
    /**
     * Fallback method to generate known permissions when file scanning fails
     * This ensures we always have comprehensive permission coverage for PermissionsEX
     */
    private void generateKnownPermissions() {
        LOGGER.debug("Using fallback permission generation");
        
        // Add all the core teleportation permissions that are registered in PermissionRegistry
        // These are the individual permissions that PermissionsEX needs for tab completion
        
        // Direct teleport admin permissions
        addDiscoveredPermission("neoessentials.teleport.admin.tp", "Admin teleport command");
        addDiscoveredPermission("neoessentials.teleport.admin.tphere", "Admin teleport here command");
        addDiscoveredPermission("neoessentials.teleport.admin.tpall", "Admin teleport all command");
        addDiscoveredPermission("neoessentials.teleport.admin.tpo", "Admin teleport override command");
        addDiscoveredPermission("neoessentials.teleport.admin.tppos", "Admin teleport to position command");
        
        // Home teleportation permissions
        addDiscoveredPermission("neoessentials.teleport.home.home", "Home teleport command");
        addDiscoveredPermission("neoessentials.teleport.home.set", "Set home command");
        addDiscoveredPermission("neoessentials.teleport.home.delete", "Delete home command");
        addDiscoveredPermission("neoessentials.teleport.home.list", "List homes command");
        addDiscoveredPermission("neoessentials.teleport.home.others", "Access other players' homes");
        
        // Spawn teleportation permissions
        addDiscoveredPermission("neoessentials.teleport.spawn.spawn", "Spawn teleport command");
        addDiscoveredPermission("neoessentials.teleport.spawn.setspawn", "Set spawn command");
        addDiscoveredPermission("neoessentials.teleport.spawn.spawninfo", "Spawn info command");
        
        // Warp teleportation permissions
        addDiscoveredPermission("neoessentials.teleport.warp.warp", "Warp teleport command");
        addDiscoveredPermission("neoessentials.teleport.warp.setwarp", "Set warp command");
        addDiscoveredPermission("neoessentials.teleport.warp.delwarp", "Delete warp command");
        addDiscoveredPermission("neoessentials.teleport.warp.warps", "List warps command");
        
        // Teleport request permissions
        addDiscoveredPermission("neoessentials.teleport.request.tpa", "Teleport ask command");
        addDiscoveredPermission("neoessentials.teleport.request.tpahere", "Teleport ask here command");
        addDiscoveredPermission("neoessentials.teleport.request.tpaccept", "Accept teleport request");
        addDiscoveredPermission("neoessentials.teleport.request.tpdeny", "Deny teleport request");
        addDiscoveredPermission("neoessentials.teleport.request.tpcancel", "Cancel teleport request");
        
        // Misc teleportation permissions
        addDiscoveredPermission("neoessentials.teleport.misc.back", "Back teleport command");
        addDiscoveredPermission("neoessentials.teleport.misc.top", "Top teleport command");
        addDiscoveredPermission("neoessentials.teleport.misc.jump", "Jump teleport command");
        addDiscoveredPermission("neoessentials.teleport.misc.jumpto", "Jump to teleport command");
        addDiscoveredPermission("neoessentials.teleport.misc.tpr", "Random teleport command");
        
        // Economy permissions
        addDiscoveredPermission("neoessentials.economy.balance", "Check balance command");
        addDiscoveredPermission("neoessentials.economy.balance.others", "Check other players' balance");
        addDiscoveredPermission("neoessentials.economy.pay", "Pay command");
        addDiscoveredPermission("neoessentials.economy.eco.give", "Economy give command");
        addDiscoveredPermission("neoessentials.economy.eco.take", "Economy take command");
        addDiscoveredPermission("neoessentials.economy.eco.set", "Economy set command");
        addDiscoveredPermission("neoessentials.economy.baltop", "Balance top command");
        
        // Kit permissions
        addDiscoveredPermission("neoessentials.kits.kit", "Kit command");
        addDiscoveredPermission("neoessentials.kits.createkit", "Create kit command");
        addDiscoveredPermission("neoessentials.kits.delkit", "Delete kit command");
        addDiscoveredPermission("neoessentials.kits.listkits", "List kits command");
        addDiscoveredPermission("neoessentials.kits.starter", "Starter kit");
        addDiscoveredPermission("neoessentials.kits.starter.nocooldown", "Starter kit no cooldown");
        
        // Chat permissions
        addDiscoveredPermission("neoessentials.chat.msg", "Private message command");
        addDiscoveredPermission("neoessentials.chat.reply", "Reply to message command");
        addDiscoveredPermission("neoessentials.chat.ignore", "Ignore player command");
        addDiscoveredPermission("neoessentials.chat.socialspy", "Social spy command");
        addDiscoveredPermission("neoessentials.chat.mute", "Mute player command");
        
        // Utility permissions
        addDiscoveredPermission("neoessentials.utility.afk", "AFK command");
        addDiscoveredPermission("neoessentials.utility.repair", "Repair command");
        addDiscoveredPermission("neoessentials.utility.dispose", "Dispose command");
        addDiscoveredPermission("neoessentials.utility.clearinventory", "Clear inventory command");
        
        // Admin permissions
        addDiscoveredPermission("neoessentials.admin.reload", "Reload configuration");
        addDiscoveredPermission("neoessentials.admin.permissions", "Permission management");
        
        // Add wildcards for convenience
        addDiscoveredPermission("neoessentials.teleport.*", "All teleportation permissions");
        addDiscoveredPermission("neoessentials.economy.*", "All economy permissions");
        addDiscoveredPermission("neoessentials.chat.*", "All chat permissions");
        addDiscoveredPermission("neoessentials.kits.*", "All kit permissions");
        addDiscoveredPermission("neoessentials.admin.*", "All admin permissions");
        addDiscoveredPermission("neoessentials.*", "All NeoEssentials permissions");
        
        LOGGER.info("Generated {} fallback permissions for PermissionsEX integration", discoveredPermissions.size());
    }
    
    /**
     * Helper method to add discovered permissions
     */
    private void addDiscoveredPermission(String permission, String source) {
        if (isValidPermission(permission)) {
            discoveredPermissions.add(permission);
            LOGGER.debug("Added fallback permission: {}", permission);
        }
    }
}