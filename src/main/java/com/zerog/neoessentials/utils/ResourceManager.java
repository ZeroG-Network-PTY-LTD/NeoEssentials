package com.zerog.neoessentials.utils;

import com.zerog.neoessentials.NeoEssentials;
import net.neoforged.fml.loading.FMLPaths;

import java.io.*;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;

/**
 * Manages resources for NeoEssentials, including copying default configs.
 */
public class ResourceManager {
    // TOML config files that go to config/neoessentials/ (disabled - using YAML instead)
    private static final List<String> CONFIG_FILES = Arrays.asList(
        // "general.toml",
        // "homes.toml", 
        // "warps.toml",
        // "kits.toml",
        // "database.toml"
    );
    
    // YAML config files that go to config/neoessentials/
    private static final List<String> YAML_CONFIG_FILES = Arrays.asList(
        "economy.yml"
    );
    
    // Files to be placed in the main neoessentials directory (outside of config)
    private static final List<String> NEOESSENTIALS_FILES = Arrays.asList(
        "tablist.yml",
        "animations.yml",
        "README.md"
    );

    /**
     * Initializes the resource manager and copies default configs
     */
    public static void initialize() {
        copyDefaultConfigs();
        copyNeoEssentialsFiles();
    }

    /**
     * Copies default configuration files from the mod's resources to the config directory
     * if they don't already exist.
     */
    public static void copyDefaultConfigs() {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve("neoessentials");
        
        try {
            // Create config directory if it doesn't exist
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
                NeoEssentials.LOGGER.info("Created config directory: {}", configDir);
            }

            // Copy TOML config files from /default_configs/
            for (String configFile : CONFIG_FILES) {
                copyDefaultConfig(configDir, configFile, "/default_configs/");
            }
            
            // Copy YAML config files from /default-config/
            for (String configFile : YAML_CONFIG_FILES) {
                copyDefaultConfig(configDir, configFile, "/default-config/");
            }
            
            NeoEssentials.LOGGER.info("Default configuration files processed");
        } catch (IOException e) {
            NeoEssentials.LOGGER.error("Failed to copy default config files", e);
        }
    }
    
    /**
     * Copies a specific config file from resources if it doesn't exist
     * 
     * @param configDir The config directory
     * @param fileName The config file name
     * @param resourcePathPrefix The resource path prefix (e.g., "/default-config/")
     * @throws IOException If an I/O error occurs
     */
    private static void copyDefaultConfig(Path configDir, String fileName, String resourcePathPrefix) throws IOException {
        Path configPath = configDir.resolve(fileName);
        
        // Check if file already exists
        if (Files.exists(configPath)) {
            NeoEssentials.LOGGER.debug("Config file already exists: {}", configPath);
            return;
        }

        // Copy file from resources
        String resourcePath = resourcePathPrefix + fileName;
        NeoEssentials.LOGGER.debug("Attempting to load resource: {}", resourcePath);
        
        try (InputStream in = ResourceManager.class.getResourceAsStream(resourcePath)) {
            if (in != null) {
                Files.copy(in, configPath, StandardCopyOption.REPLACE_EXISTING);
                NeoEssentials.LOGGER.info("Copied default config: {} from {}", fileName, resourcePath);
            } else {
                NeoEssentials.LOGGER.warn("Default config not found in resources: {} (path: {})", fileName, resourcePath);
                // Try to debug the issue by listing available resources
                try {
                    java.net.URL resourceUrl = ResourceManager.class.getResource(resourcePathPrefix);
                    if (resourceUrl != null) {
                        NeoEssentials.LOGGER.debug("{} directory found at: {}", resourcePathPrefix, resourceUrl);
                    } else {
                        NeoEssentials.LOGGER.warn("{} directory not found in resources", resourcePathPrefix);
                    }
                } catch (Exception e) {
                    NeoEssentials.LOGGER.warn("Error checking resource directory {}: {}", resourcePathPrefix, e.getMessage());
                }
            }
        }
    }

    /**
     * Updates a specific default example in a config file
     * 
     * @param fileName The config file name
     * @param exampleSectionName The name of the example section to update
     */
    public static void updateConfigExamples(String fileName, String exampleSectionName) {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve("neoessentials");
        Path configPath = configDir.resolve(fileName);

        if (!Files.exists(configPath)) {
            NeoEssentials.LOGGER.warn("Cannot update examples - file doesn't exist: {}", fileName);
            return;
        }

        try {
            // Read the config file content
            String content = new String(Files.readAllBytes(configPath));
            
            // Create a backup of the original file
            Path backupPath = configDir.resolve(fileName + ".bak");
            Files.copy(configPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
            
            // Read the example from resources
            String resourcePath = "/default-config/" + fileName;
            String example = readResourceFile(resourcePath);
            
            if (example == null) {
                NeoEssentials.LOGGER.warn("Cannot read example from resources: {}", resourcePath);
                return;
            }
            
            // Extract the specific example section
            int exampleStart = example.indexOf("# " + exampleSectionName + " EXAMPLE");
            int exampleEnd = example.indexOf("# END " + exampleSectionName + " EXAMPLE");
            
            if (exampleStart == -1 || exampleEnd == -1) {
                NeoEssentials.LOGGER.warn("Example section '{}' not found in resource file", exampleSectionName);
                return;
            }
            
            example = example.substring(exampleStart, exampleEnd + ("# END " + exampleSectionName + " EXAMPLE").length());
            
            // Update the example in the user's config file
            int userExampleStart = content.indexOf("# " + exampleSectionName + " EXAMPLE");
            int userExampleEnd = content.indexOf("# END " + exampleSectionName + " EXAMPLE");
            
            if (userExampleStart == -1 || userExampleEnd == -1) {
                // Example doesn't exist in user's config, append it
                Files.write(configPath, (content + "\n\n" + example).getBytes());
                NeoEssentials.LOGGER.info("Added example section '{}' to '{}'", exampleSectionName, fileName);
            } else {
                // Example exists, update it
                String updatedContent = content.substring(0, userExampleStart) + 
                                        example + 
                                        content.substring(userExampleEnd + ("# END " + exampleSectionName + " EXAMPLE").length());
                Files.write(configPath, updatedContent.getBytes());
                NeoEssentials.LOGGER.info("Updated example section '{}' in '{}'", exampleSectionName, fileName);
            }
        } catch (IOException e) {
            NeoEssentials.LOGGER.error("Failed to update example in {}: {}", fileName, e.getMessage());
        }
    }

    /**
     * Reads a resource file's contents as a string
     * 
     * @param resourcePath The path to the resource
     * @return The file contents, or null if not found
     */
    private static String readResourceFile(String resourcePath) {
        try (InputStream in = ResourceManager.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                return null;
            }
            
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            return result.toString("UTF-8");
        } catch (IOException e) {
            NeoEssentials.LOGGER.error("Failed to read resource file: {}", resourcePath, e);
            return null;
        }
    }

    /**
     * Force extracts the tablist.yml file, creating a backup of the existing one if it exists.
     * This is useful when troubleshooting animation or placeholder issues.
     * 
     * @return True if the extraction was successful, false otherwise
     */
    public static boolean forceExtractTablistConfig() {
        Path neoEssentialsDir = Paths.get("neoessentials");
        Path tablistPath = neoEssentialsDir.resolve("tablist.yml");
        
        try {
            // Create backup if file exists
            if (Files.exists(tablistPath)) {
                Path backupPath = neoEssentialsDir.resolve("tablist.yml.backup");
                Files.copy(tablistPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
                NeoEssentials.LOGGER.info("Created backup of existing tablist.yml");
            }
            
            // Force extract from resources
            try (InputStream in = ResourceManager.class.getResourceAsStream("/default-neoessentials/tablist.yml")) {
                if (in != null) {
                    Files.copy(in, tablistPath, StandardCopyOption.REPLACE_EXISTING);
                    NeoEssentials.LOGGER.info("Force extracted tablist.yml from resources");
                    return true;
                } else {
                    NeoEssentials.LOGGER.error("tablist.yml not found in resources");
                    return false;
                }
            }
        } catch (IOException e) {
            NeoEssentials.LOGGER.error("Failed to force extract tablist.yml", e);
            return false;
        }
    }

    /**
     * Force extracts the animations.yml file, creating a backup of the existing one if it exists.
     * This is useful when troubleshooting animation issues.
     * 
     * @return True if the extraction was successful, false otherwise
     */
    public static boolean forceExtractAnimationsConfig() {
        Path neoEssentialsDir = Paths.get("neoessentials");
        Path animationsPath = neoEssentialsDir.resolve("animations.yml");
        
        try {
            // Create backup if file exists
            if (Files.exists(animationsPath)) {
                Path backupPath = neoEssentialsDir.resolve("animations.yml.backup");
                Files.copy(animationsPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
                NeoEssentials.LOGGER.info("Created backup of existing animations.yml");
            }
            
            // Force extract from resources
            try (InputStream in = ResourceManager.class.getResourceAsStream("/default-neoessentials/animations.yml")) {
                if (in != null) {
                    Files.copy(in, animationsPath, StandardCopyOption.REPLACE_EXISTING);
                    NeoEssentials.LOGGER.info("Force extracted animations.yml from resources");
                    return true;
                } else {
                    NeoEssentials.LOGGER.error("animations.yml not found in resources");
                    return false;
                }
            }
        } catch (IOException e) {
            NeoEssentials.LOGGER.error("Failed to force extract animations.yml", e);
            return false;
        }
    }

    /**
     * Copies files to the main neoessentials directory (outside of config)
     */
    public static void copyNeoEssentialsFiles() {
        Path neoEssentialsDir = Paths.get("neoessentials");
        
        try {
            // Create neoessentials directory if it doesn't exist
            if (!Files.exists(neoEssentialsDir)) {
                Files.createDirectories(neoEssentialsDir);
                NeoEssentials.LOGGER.info("Created neoessentials directory: {}", neoEssentialsDir);
            }

            // Copy each default file
            for (String fileName : NEOESSENTIALS_FILES) {
                copyNeoEssentialsFile(neoEssentialsDir, fileName);
            }
            
            NeoEssentials.LOGGER.info("Default neoessentials files processed");
        } catch (IOException e) {
            NeoEssentials.LOGGER.error("Failed to copy default neoessentials files", e);
        }
    }
    
    /**
     * Copies a specific file from resources to the neoessentials directory if it doesn't exist
     * 
     * @param neoEssentialsDir The neoessentials directory
     * @param fileName The file name
     * @throws IOException If an I/O error occurs
     */
    private static void copyNeoEssentialsFile(Path neoEssentialsDir, String fileName) throws IOException {
        Path filePath = neoEssentialsDir.resolve(fileName);
        
        // Check if file already exists
        if (Files.exists(filePath)) {
            NeoEssentials.LOGGER.debug("File already exists: {}", filePath);
            return;
        }

        // Copy file from resources
        try (InputStream in = ResourceManager.class.getResourceAsStream("/default-neoessentials/" + fileName)) {
            if (in != null) {
                Files.copy(in, filePath, StandardCopyOption.REPLACE_EXISTING);
                NeoEssentials.LOGGER.info("Copied default file to neoessentials directory: {}", fileName);
            } else {
                NeoEssentials.LOGGER.warn("Default file not found in resources: {}", fileName);
            }
        }
    }
}
