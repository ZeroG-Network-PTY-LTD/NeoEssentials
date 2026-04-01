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
     * Validate permission format.
     * Accepts fully-qualified nodes and wildcard suffixes (e.g. {@code neoessentials.spawner.*}).
     */
    private boolean isValidPermission(String permission) {
        if (permission == null || permission.trim().isEmpty()) return false;

        // Allow ".*" wildcard suffix
        if (permission.endsWith(".*")) {
            String prefix = permission.substring(0, permission.length() - 2);
            return prefix.startsWith("neoessentials")
                && prefix.matches("^[a-z0-9._-]+$")
                && !prefix.startsWith(".")
                && !prefix.endsWith(".")
                && !prefix.contains("..");
        }

        // Must start with neoessentials.
        if (!permission.startsWith("neoessentials.")) return false;

        // Valid characters only
        if (!permission.matches("^[a-z0-9._-]+$")) return false;

        // Cannot end with dot or have consecutive dots
        if (permission.endsWith(".") || permission.contains("..")) return false;

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
     * Fallback method to load permissions from permissions_nodes.txt resource file
     * This ensures we always have comprehensive permission coverage for PermissionsEX
     */
    private void generateKnownPermissions() {
        LOGGER.debug("Loading permissions from permissions_nodes.txt resource file");
        
        try {
            // Try to load from classpath resource
            var inputStream = getClass().getClassLoader().getResourceAsStream("data/config/permissions_nodes.txt");
            
            if (inputStream == null) {
                LOGGER.warn("Could not find permissions_nodes.txt in resources, using hardcoded fallback");
                loadHardcodedFallback();
                return;
            }
            
            // Read all lines from the resource file
            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(inputStream))) {
                int loadedCount = 0;
                String line;
                
                while ((line = reader.readLine()) != null) {
                    // Trim whitespace
                    line = line.trim();
                    
                    // Skip empty lines and comments
                    if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) {
                        continue;
                    }
                    
                    // Extract permission node (before the dash if present)
                    String permission;
                    int dashIndex = line.indexOf(" -");
                    if (dashIndex > 0) {
                        permission = line.substring(0, dashIndex).trim();
                    } else {
                        permission = line;
                    }
                    
                    // Validate and add permission
                    if (isValidPermission(permission)) {
                        discoveredPermissions.add(permission);
                        loadedCount++;
                        LOGGER.debug("Loaded permission from file: {}", permission);
                    } else {
                        LOGGER.debug("Skipping invalid permission line: {}", line);
                    }
                }
                
                LOGGER.info("Loaded {} permissions from permissions_nodes.txt for PermissionsEX integration", loadedCount);
                
            } catch (IOException e) {
                LOGGER.error("Error reading permissions_nodes.txt: {}", e.getMessage());
                loadHardcodedFallback();
            }
            
        } catch (Exception e) {
            LOGGER.error("Unexpected error loading permissions from file: {}", e.getMessage());
            loadHardcodedFallback();
        }
    }
    
    /**
     * Hardcoded fallback if resource file cannot be loaded
     */
    private void loadHardcodedFallback() {
        LOGGER.debug("Using hardcoded permission fallback");
        
        // Add basic wildcard permissions as last resort
        addDiscoveredPermission("neoessentials.*", "All NeoEssentials permissions");
        addDiscoveredPermission("neoessentials.teleport.*", "All teleportation permissions");
        addDiscoveredPermission("neoessentials.teleport.admin.*", "All admin teleport permissions");
        addDiscoveredPermission("neoessentials.teleport.home.*", "All home permissions");
        addDiscoveredPermission("neoessentials.teleport.spawn.*", "All spawn permissions");
        addDiscoveredPermission("neoessentials.teleport.warp.*", "All warp permissions");
        addDiscoveredPermission("neoessentials.teleport.request.*", "All teleport request permissions");
        addDiscoveredPermission("neoessentials.teleport.misc.*", "All misc teleport permissions");
        addDiscoveredPermission("neoessentials.economy.*", "All economy permissions");
        addDiscoveredPermission("neoessentials.chat.*", "All chat permissions");
        addDiscoveredPermission("neoessentials.kits.*", "All kit permissions");
        addDiscoveredPermission("neoessentials.admin.*", "All admin permissions");
        addDiscoveredPermission("neoessentials.utility.*", "All utility permissions");
        
        LOGGER.warn("Loaded {} hardcoded fallback permissions (permissions_nodes.txt not available)", 
                    discoveredPermissions.size());
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