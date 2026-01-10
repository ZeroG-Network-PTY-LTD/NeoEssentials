package com.zerog.neoessentials.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zerog.neoessentials.util.ResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Handles splitting large config.json into smaller, manageable files.
 * Provides backward compatibility by merging split configs into one view.
 */
public class ConfigSplitter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigSplitter.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Map of config section names to their file names
    private static final Map<String, String> CONFIG_FILE_MAP = new LinkedHashMap<>() {{
        put("modules", "modules.json");
        put("logging", "main.json");
        put("permissions", "main.json");
        put("security", "security.json");
        put("commands", "commands.json");
        put("webDashboard", "webdashboard.json");
        put("items", "items.json");
        put("afk", "afk.json");
        put("kits", "kits.json");  // Already separate
        put("teleportation", "teleportation.json");  // Already separate
        put("moderation", "moderation.json");
        put("chat", "chat.json");
    }};

    // Version for each split config file
    private static final Map<String, Integer> SPLIT_CONFIG_VERSIONS = new HashMap<>() {{
        put("main.json", 1);
        put("commands.json", 1);
        put("chat.json", 1);
        put("teleportation.json", 1);
        put("moderation.json", 1);
        put("webdashboard.json", 1);
        put("items.json", 1);
        put("afk.json", 1);
        put("security.json", 1);
        put("modules.json", 1);
    }};

    /**
     * Check if config splitting is enabled
     */
    public static boolean isSplittingEnabled() {
        File configDir = new File(ResourceUtil.CONFIG_DIR);
        File marker = new File(configDir, ".split_configs");
        return marker.exists();
    }

    /**
     * Migrate from monolithic config.json to split configs
     */
    public static boolean migrateToSplitConfigs() {
        try {
            File configFile = ResourceUtil.getConfigFile("config.json");
            if (!configFile.exists()) {
                LOGGER.warn("config.json not found, cannot migrate to split configs");
                return false;
            }

            LOGGER.info("========================================");
            LOGGER.info("Migrating to split configuration files...");
            LOGGER.info("========================================");

            // Read the monolithic config
            JsonObject config;
            try (FileReader reader = new FileReader(configFile, StandardCharsets.UTF_8)) {
                config = JsonParser.parseReader(reader).getAsJsonObject();
            }

            // Create backup of original config
            File backup = new File(configFile.getParentFile(), "config.json.backup");
            java.nio.file.Files.copy(configFile.toPath(), backup.toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            LOGGER.info("Created backup: config.json.backup");

            // Extract each section into its own file
            int filesCreated = 0;
            for (Map.Entry<String, String> entry : CONFIG_FILE_MAP.entrySet()) {
                String sectionName = entry.getKey();
                String fileName = entry.getValue();

                if (config.has(sectionName)) {
                    JsonObject section = extractSection(config, sectionName, fileName);
                    File targetFile = ResourceUtil.getConfigFile(fileName);

                    // Don't overwrite existing split configs
                    if (!targetFile.exists() || sectionName.equals("modules") || sectionName.equals("logging") || sectionName.equals("permissions")) {
                        try (FileWriter writer = new FileWriter(targetFile, StandardCharsets.UTF_8)) {
                            GSON.toJson(section, writer);
                            filesCreated++;
                            LOGGER.info("  ✓ Created {}", fileName);
                        }
                    }
                }
            }

            // Create marker file to indicate split configs are active
            File configDir = new File(ResourceUtil.CONFIG_DIR);
            File marker = new File(configDir, ".split_configs");
            if (marker.createNewFile()) {
                LOGGER.info("Created split configs marker file");
            }

            LOGGER.info("========================================");
            LOGGER.info("Migration complete! Created {} config files", filesCreated);
            LOGGER.info("Original config backed up to: config.json.backup");
            LOGGER.info("You can now edit smaller, focused config files!");
            LOGGER.info("========================================");

            return true;

        } catch (Exception e) {
            LOGGER.error("Failed to migrate to split configs: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Extract a section from the main config and add version info
     */
    private static JsonObject extractSection(JsonObject mainConfig, String sectionName, String targetFile) {
        JsonObject result = new JsonObject();

        // Add version info
        Integer version = SPLIT_CONFIG_VERSIONS.get(targetFile);
        if (version != null) {
            result.addProperty("_configVersion", version);
            result.addProperty("_configVersion_comment",
                "DO NOT MODIFY: This field is used by NeoEssentials for automatic config updates.");
        }

        // Handle special case: main.json contains multiple sections
        if (targetFile.equals("main.json")) {
            if (mainConfig.has("modules")) {
                result.add("modules", mainConfig.get("modules"));
            }
            if (mainConfig.has("logging")) {
                result.add("logging", mainConfig.get("logging"));
            }
            if (mainConfig.has("permissions")) {
                result.add("permissions", mainConfig.get("permissions"));
            }
        } else {
            // Single section per file
            if (mainConfig.has(sectionName)) {
                result.add(sectionName, mainConfig.get(sectionName));
            }
        }

        return result;
    }

    /**
     * Merge split configs back into a single view for backward compatibility
     */
    public static JsonObject mergeSplitConfigs() {
        JsonObject merged = new JsonObject();

        // Add overall version
        merged.addProperty("_configVersion", 13);
        merged.addProperty("_configVersion_comment",
            "NOTE: This is a virtual merged view. Edit individual config files instead.");

        // Load and merge each split config
        for (Map.Entry<String, String> entry : CONFIG_FILE_MAP.entrySet()) {
            String sectionName = entry.getKey();
            String fileName = entry.getValue();

            File configFile = ResourceUtil.getConfigFile(fileName);
            if (configFile.exists()) {
                try (FileReader reader = new FileReader(configFile, StandardCharsets.UTF_8)) {
                    JsonObject fileConfig = JsonParser.parseReader(reader).getAsJsonObject();

                    // Handle main.json which contains multiple sections
                    if (fileName.equals("main.json")) {
                        if (fileConfig.has("modules")) {
                            merged.add("modules", fileConfig.get("modules"));
                        }
                        if (fileConfig.has("logging")) {
                            merged.add("logging", fileConfig.get("logging"));
                        }
                        if (fileConfig.has("permissions")) {
                            merged.add("permissions", fileConfig.get("permissions"));
                        }
                    } else {
                        // Single section
                        if (fileConfig.has(sectionName)) {
                            merged.add(sectionName, fileConfig.get(sectionName));
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to load split config {}: {}", fileName, e.getMessage());
                }
            }
        }

        return merged;
    }

    /**
     * Check if migration is needed and prompt admin
     */
    public static void checkAndPromptMigration() {
        if (!isSplittingEnabled()) {
            File configFile = ResourceUtil.getConfigFile("config.json");
            if (configFile.exists()) {
                LOGGER.info("========================================");
                LOGGER.info("NOTICE: Large config.json detected!");
                LOGGER.info("NeoEssentials now supports split configuration files for easier editing.");
                LOGGER.info("To enable, run: /neoessentials config split");
                LOGGER.info("This will split config.json into smaller, focused files.");
                LOGGER.info("========================================");
            }
        }
    }
}
