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
public class ResourceManager {    private static final List<String> CONFIG_FILES = Arrays.asList(
        "general.toml",
        "economy.toml",
        "homes.toml",
        "warps.toml",
        "kits.toml",
        "tablist.toml",
        "database.toml",
        "animations.toml"
    );

    /**
     * Initializes the resource manager and copies default configs
     */
    public static void initialize() {
        copyDefaultConfigs();
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

            // Copy each default config file
            for (String configFile : CONFIG_FILES) {
                copyDefaultConfig(configDir, configFile);
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
     * @throws IOException If an I/O error occurs
     */
    private static void copyDefaultConfig(Path configDir, String fileName) throws IOException {
        Path configPath = configDir.resolve(fileName);
        
        // Check if file already exists
        if (Files.exists(configPath)) {
            NeoEssentials.LOGGER.debug("Config file already exists: {}", configPath);
            return;
        }

        // Copy file from resources
        try (InputStream in = ResourceManager.class.getResourceAsStream("/default_configs/" + fileName)) {
            if (in != null) {
                Files.copy(in, configPath, StandardCopyOption.REPLACE_EXISTING);
                NeoEssentials.LOGGER.info("Copied default config: {}", fileName);
            } else {
                NeoEssentials.LOGGER.warn("Default config not found in resources: {}", fileName);
            }
        }
    }

    /**
     * Updates a specific default example in a config file
     * 
     * @param configDir The config directory
     * @param fileName The config file name
     * @param exampleSectionName The name of the example section to update
     * @throws IOException If an I/O error occurs
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

            // Read the default example from resources
            String exampleContent = readResourceFile("/default_configs/" + fileName);
            
            if (exampleContent == null) {
                NeoEssentials.LOGGER.warn("Default example not found for: {}", fileName);
                return;
            }

            // Extract the example section
            int exampleStart = exampleContent.indexOf("# " + exampleSectionName + " EXAMPLE");
            int exampleEnd = exampleContent.indexOf("# END " + exampleSectionName + " EXAMPLE");
            
            if (exampleStart == -1 || exampleEnd == -1) {
                NeoEssentials.LOGGER.warn("Example section '{}' not found in default config", exampleSectionName);
                return;
            }

            String example = exampleContent.substring(exampleStart, exampleEnd + ("# END " + exampleSectionName + " EXAMPLE").length());
            
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
     * Force extracts the tablist.toml file, creating a backup of the existing one if it exists.
     * This is useful when troubleshooting animation or placeholder issues.
     * 
     * @return True if the extraction was successful, false otherwise
     */
    public static boolean forceExtractTablistConfig() {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve("neoessentials");
        String fileName = "tablist.toml";
        Path configPath = configDir.resolve(fileName);
        
        try {
            // Create config directory if it doesn't exist
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
                NeoEssentials.LOGGER.info("Created config directory: {}", configDir);
            }
            
            // If file exists, make a backup
            if (Files.exists(configPath)) {
                Path backupPath = configDir.resolve(fileName + ".backup-" + System.currentTimeMillis() + ".toml");
                Files.copy(configPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
                NeoEssentials.LOGGER.info("Created backup of existing tablist.toml at: {}", backupPath);
            }
            
            // Extract the resource
            String resourceContent = readResourceFile("/default_configs/" + fileName);
            if (resourceContent == null) {
                NeoEssentials.LOGGER.error("Could not find default tablist.toml in resources");
                return false;
            }
            
            // Write the content
            Files.write(configPath, resourceContent.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            NeoEssentials.LOGGER.info("Successfully extracted fresh tablist.toml configuration to: {}", configPath);
            
            return true;
        } catch (IOException e) {
            NeoEssentials.LOGGER.error("Failed to extract tablist.toml", e);
            return false;
        }
    }
}
