package com.zerog.neoessentials.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zerog.neoessentials.util.ResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
     * Ensure all split config files are up to date, and if config.json is newer or has new keys, re-split and update split files.
     */
    public static void ensureSplitConfigsUpToDate() {
        if (!isSplittingEnabled()) {
            return;
        }
        File configFile = ResourceUtil.getConfigFile("config.json");
        ConfigSplitter splitter = new ConfigSplitter();
        if (!splitter.ensureUnifiedConfigExists(configFile)) {
            LOGGER.error("config.json is missing and could not be generated. Split config update aborted.");
            return;
        }

        LOGGER.debug("Checking split config file versions...");

        // Check if config.json is newer than any split file (by last modified time)
        boolean needsResplit = false;
        if (configFile.exists()) {
            long configJsonLastModified = configFile.lastModified();
            for (String fileName : SPLIT_CONFIG_VERSIONS.keySet()) {
                File splitFile = ResourceUtil.getConfigFile(fileName);
                if (!splitFile.exists() || configJsonLastModified > splitFile.lastModified()) {
                    needsResplit = true;
                    break;
                }
            }
        }

        if (needsResplit) {
            LOGGER.info("config.json is newer than split configs or split file missing. Re-splitting config.json into split files...");
            migrateToSplitConfigs();
            // After migration, return to avoid double update
            return;
        }

        // Normal version check/update for each split file
        for (Map.Entry<String, Integer> entry : SPLIT_CONFIG_VERSIONS.entrySet()) {
            String fileName = entry.getKey();
            int expectedVersion = entry.getValue();

            File splitFile = ResourceUtil.getConfigFile(fileName);

            if (!splitFile.exists()) {
                // Always try to extract from config.json first
                File unifiedConfig = ResourceUtil.getConfigFile("config.json");
                boolean generated = false;
                if (unifiedConfig.exists()) {
                    try (FileReader reader = new FileReader(unifiedConfig, StandardCharsets.UTF_8)) {
                        JsonObject config = GSON.fromJson(reader, JsonObject.class);
                        // Find the section name for this file
                        String sectionName = null;
                        for (Map.Entry<String, String> mapEntry : CONFIG_FILE_MAP.entrySet()) {
                            if (mapEntry.getValue().equals(fileName)) {
                                sectionName = mapEntry.getKey();
                                break;
                            }
                        }
                        if (sectionName != null && config.has(sectionName)) {
                            JsonObject section = extractSection(config, sectionName, fileName);
                            try (FileWriter writer = new FileWriter(splitFile, StandardCharsets.UTF_8)) {
                                GSON.toJson(section, writer);
                                LOGGER.info("  ✓ Generated {} from unified config.json", fileName);
                                generated = true;
                            }
                        } else {
                            LOGGER.warn("Section '{}' not found in config.json for split config {}", sectionName, fileName);
                        }
                    } catch (Exception e) {
                        LOGGER.error("Failed to generate split config {}: {}", fileName, e.getMessage());
                    }
                }
                // If still missing, do not fallback to JAR, just warn
                if (!splitFile.exists() && !generated) {
                    LOGGER.warn("Split config file {} could not be generated from config.json and will remain missing.", fileName);
                }
            } else {
                // Check version
                checkSplitConfigVersion(fileName, splitFile, expectedVersion);
            }
        }

        // --- NEW: Merge new/changed keys from config.json into split files ---
        File unifiedConfig = ResourceUtil.getConfigFile("config.json");
        if (unifiedConfig.exists()) {
            try (FileReader reader = new FileReader(unifiedConfig, StandardCharsets.UTF_8)) {
                JsonObject unified = JsonParser.parseReader(reader).getAsJsonObject();
                for (Map.Entry<String, String> entry : CONFIG_FILE_MAP.entrySet()) {
                    String sectionName = entry.getKey();
                    String fileName = entry.getValue();
                    File splitFile = ResourceUtil.getConfigFile(fileName);
                    if (!splitFile.exists()) continue;
                    if (!unified.has(sectionName)) continue;
                    // Merge section from unified config into split file
                    mergeSectionIntoSplitFile(sectionName, fileName, unified.getAsJsonObject(sectionName));
                }
            } catch (Exception e) {
                LOGGER.error("Failed to merge unified config into split files: {}", e.getMessage());
            }
        }
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

            // Replace config.json with a minimal stub file
            replaceWithStubFile(configFile);
            LOGGER.info("Replaced config.json with minimal stub file");

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
     * Check if this is a fresh server installation (no configs exist yet)
     */
    @SuppressWarnings("unused") // Called from ConfigManager
    public static boolean isFreshInstall() {
        File configFile = ResourceUtil.getConfigFile("config.json");
        File configDir = new File(ResourceUtil.CONFIG_DIR);

        // Fresh install if config directory doesn't exist or is empty
        if (!configDir.exists() || !configFile.exists()) {
            return true;
        }

        // Also check if no split configs exist
        return !isSplittingEnabled();
    }

    /**
     * Auto-split configs for fresh installations
     * This is called automatically for new servers
     */
    @SuppressWarnings("unused") // Called from ConfigManager
    public static boolean autoSplitForFreshInstall() {
        File configFile = ResourceUtil.getConfigFile("config.json");

        // If config.json doesn't exist yet, this is truly fresh
        if (!configFile.exists()) {
            LOGGER.info("========================================");
            LOGGER.info("Fresh NeoEssentials installation detected!");
            LOGGER.info("Automatically creating split configuration files...");
            LOGGER.info("========================================");

            // Create split configs directly from JAR resources
            return createSplitConfigsFromJar();
        }

        return false;
    }

    /**
     * Create split configs directly from JAR resources (for fresh installs)
     */
    private static boolean createSplitConfigsFromJar() {
        try {
            // Create each split config file from JAR
            Map<String, String> splitFiles = new LinkedHashMap<>() {{
                put("main.json", "main.json");
                put("commands.json", "commands.json");
                put("chat.json", "chat.json");
                put("security.json", "security.json");
                put("items.json", "items.json");
                put("afk.json", "afk.json");
                put("moderation.json", "moderation.json");
                put("teleportation.json", "teleportation.json");
                put("webdashboard.json", "webdashboard.json");
            }};

            int successCount = 0;
            for (Map.Entry<String, String> entry : splitFiles.entrySet()) {
                String fileName = entry.getValue();
                File targetFile = ResourceUtil.getConfigFile(fileName);

                // Try to load from JAR resources (if they exist)
                try (InputStream in = ResourceUtil.getJarConfigResource(fileName)) {
                    if (in != null) {
                        // Ensure parent directories exist
                        File parentDir = targetFile.getParentFile();
                        if (parentDir != null && !parentDir.exists()) {
                            if (!parentDir.mkdirs()) {
                                LOGGER.warn("Could not create parent directory for {}", fileName);
                            }
                        }

                        try (FileOutputStream out = new FileOutputStream(targetFile)) {
                            byte[] buffer = new byte[8192];
                            int len;
                            while ((len = in.read(buffer)) > 0) {
                                out.write(buffer, 0, len);
                            }
                        }
                        successCount++;
                        LOGGER.info("  ✓ Created {}", fileName);
                    }
                } catch (Exception e) {
                    LOGGER.debug("Could not load {} from JAR, will be created later", fileName);
                }
            }

            // Create marker file
            File configDir = new File(ResourceUtil.CONFIG_DIR);
            File marker = new File(configDir, ".split_configs");
            if (marker.createNewFile()) {
                LOGGER.info("✓ Enabled split configs mode");
            }

            LOGGER.info("========================================");
            LOGGER.info("Split configuration files created successfully! ({} files)", successCount);
            LOGGER.info("Your server is configured with easier-to-manage config files.");
            LOGGER.info("========================================");

            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to create split configs: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Check if migration is needed and prompt admin
     * Now only shows for existing servers with monolithic config
     */
    public static void checkAndPromptMigration() {
        // Don't prompt if already using split configs
        if (isSplittingEnabled()) {
            return;
        }

        File configFile = ResourceUtil.getConfigFile("config.json");

        // Only prompt if we have an existing monolithic config
        if (configFile.exists()) {
            LOGGER.info("========================================");
            LOGGER.info("NOTICE: Large config.json detected!");
            LOGGER.info("NeoEssentials now supports split configuration files for easier editing.");
            LOGGER.info("To enable, run: /neoessentials config split");
            LOGGER.info("This will split config.json into smaller, focused files.");
            LOGGER.info("========================================");

            // Set flag to notify online admins
            shouldNotifyAdmins = true;
        }
    }

    // Flag to track if we should notify admins about config splitting
    private static boolean shouldNotifyAdmins = false;

    /**
     * Check if admins should be notified about config splitting
     */
    @SuppressWarnings("unused") // Called from NeoEssentials
    public static boolean shouldNotifyAdmins() {
        return shouldNotifyAdmins;
    }

    /**
     * Mark that admins have been notified
     */
    @SuppressWarnings("unused") // Called from NeoEssentials
    public static void markAdminsNotified() {
        shouldNotifyAdmins = false;
    }

    /**
     * Replace config.json with a minimal stub file that redirects to split configs
     */
    private static void replaceWithStubFile(File configFile) throws IOException {
        JsonObject stub = new JsonObject();

        // Add version info
        stub.addProperty("_configVersion", 13);
        stub.addProperty("_configVersion_comment",
            "DO NOT MODIFY: This field is used by NeoEssentials for automatic config updates.");

        // Add informational comment
        stub.addProperty("_notice",
            "This server is using SPLIT CONFIGURATION FILES for easier management.");
        stub.addProperty("_notice_info",
            "Configuration has been split into smaller, focused files in the config/neoessentials/ directory.");

        // Create a helpful guide object
        JsonObject guide = new JsonObject();
        guide.addProperty("main.json", "Core settings: modules, logging, permissions");
        guide.addProperty("commands.json", "Command settings and toggles");
        guide.addProperty("chat.json", "Chat formatting, channels, badges, anti-spam");
        guide.addProperty("teleportation.json", "Teleport settings, homes, warps, spawn");
        guide.addProperty("moderation.json", "Ban, kick, mute, freeze, jail settings");
        guide.addProperty("webdashboard.json", "Web dashboard configuration");
        guide.addProperty("items.json", "Item management and repair settings");
        guide.addProperty("afk.json", "AFK system configuration");
        guide.addProperty("security.json", "Security and validation settings");
        guide.addProperty("kits.json", "Kit definitions (separate file)");
        stub.add("_split_config_files", guide);

        // Add restoration instructions
        stub.addProperty("_restore_instructions",
            "To restore the monolithic config.json, delete the .split_configs marker file and restore from config.json.backup");

        // Write the stub file
        try (FileWriter writer = new FileWriter(configFile, StandardCharsets.UTF_8)) {
            GSON.toJson(stub, writer);
        }
    }

    /**
     * Merge a section from the unified config into the split config file, preserving user customizations.
     * Only adds new keys or updates values that are not present in the split file.
     */
    private static void mergeSectionIntoSplitFile(String sectionName, String fileName, JsonObject unifiedSection) {
        File splitFile = ResourceUtil.getConfigFile(fileName);
        try (FileReader reader = new FileReader(splitFile, StandardCharsets.UTF_8)) {
            JsonObject splitConfig = JsonParser.parseReader(reader).getAsJsonObject();
            boolean changed = false;
            // For main.json, handle multiple sections
            if (fileName.equals("main.json")) {
                if (!splitConfig.has(sectionName)) {
                    splitConfig.add(sectionName, unifiedSection);
                    changed = true;
                } else {
                    JsonObject splitSection = splitConfig.getAsJsonObject(sectionName);
                    changed |= mergeJsonObjects(splitSection, unifiedSection);
                }
            } else {
                // Single section per file
                if (!splitConfig.has(sectionName)) {
                    splitConfig.add(sectionName, unifiedSection);
                    changed = true;
                } else {
                    JsonObject splitSection = splitConfig.getAsJsonObject(sectionName);
                    changed |= mergeJsonObjects(splitSection, unifiedSection);
                }
            }
            if (changed) {
                try (FileWriter writer = new FileWriter(splitFile, StandardCharsets.UTF_8)) {
                    GSON.toJson(splitConfig, writer);
                }
                LOGGER.info("Updated split config {} with new keys from unified config", fileName);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to merge section {} into split file {}: {}", sectionName, fileName, e.getMessage());
        }
    }

    /**
     * Recursively merge keys from source into target JsonObject. Only adds new keys or updates values that are not present in target.
     * Returns true if any changes were made.
     */
    private static boolean mergeJsonObjects(JsonObject target, JsonObject source) {
        boolean changed = false;
        for (Map.Entry<String, com.google.gson.JsonElement> entry : source.entrySet()) {
            String key = entry.getKey();
            com.google.gson.JsonElement value = entry.getValue();
            if (!target.has(key)) {
                target.add(key, value);
                changed = true;
            } else if (value.isJsonObject() && target.get(key).isJsonObject()) {
                changed |= mergeJsonObjects(target.getAsJsonObject(key), value.getAsJsonObject());
            }
        }
        return changed;
    }

    /**
     * Check if a split config file needs updating
     */
    private static void checkSplitConfigVersion(String fileName, File configFile, int expectedVersion) {
        try (FileReader reader = new FileReader(configFile, StandardCharsets.UTF_8)) {
            JsonObject config = JsonParser.parseReader(reader).getAsJsonObject();

            int currentVersion = 0;
            if (config.has("_configVersion")) {
                currentVersion = config.get("_configVersion").getAsInt();
            }

            if (currentVersion < expectedVersion) {
                LOGGER.warn("Split config {} is outdated (version {} < {}). Updating...",
                    fileName, currentVersion, expectedVersion);

                // Create backup
                String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new java.util.Date());
                String backupName = fileName.replace(".json", String.format("_v%d_backup_%s.json", currentVersion, timestamp));
                File backupFile = new File(configFile.getParentFile(), backupName);
                java.nio.file.Files.copy(configFile.toPath(), backupFile.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                LOGGER.info("Created backup: {}", backupName);

                // Update from JAR
                copyDefaultSplitConfig(fileName);
                LOGGER.info("Updated split config {} to version {}", fileName, expectedVersion);
            } else if (currentVersion > expectedVersion) {
                LOGGER.warn("Split config {} has newer version ({}) than expected ({})",
                    fileName, currentVersion, expectedVersion);
            } else {
                LOGGER.debug("Split config {} is up to date (version {})", fileName, currentVersion);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to check version for split config {}: {}", fileName, e.getMessage());
        }
    }

    /**
     * Copy a default split config from JAR
     */
    private static void copyDefaultSplitConfig(String fileName) {
        try (InputStream in = ResourceUtil.getJarConfigResource(fileName)) {
            if (in != null) {
                File targetFile = ResourceUtil.getConfigFile(fileName);

                // Ensure parent directories exist
                File parentDir = targetFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    if (!parentDir.mkdirs()) {
                        LOGGER.warn("Could not create parent directory for {}", fileName);
                    }
                }

                try (FileOutputStream out = new FileOutputStream(targetFile)) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = in.read(buffer)) > 0) {
                        out.write(buffer, 0, len);
                    }
                }
                LOGGER.debug("Copied default split config: {}", fileName);
            } else {
                LOGGER.warn("Default split config not found in JAR: {}", fileName);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to copy default split config {}: {}", fileName, e.getMessage());
        }
    }

    /**
     * Ensures config.json exists. If missing, attempts to generate from JAR default.
     * Returns true if config.json exists after this call, false otherwise.
     */
    private boolean ensureUnifiedConfigExists(File configFile) {
        if (configFile.exists()) return true;

        // Ensure parent directory exists
        File parentDir = configFile.getParentFile();
        if (!parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                LOGGER.error("Failed to create config directory: {}", parentDir.getAbsolutePath());
                return false;
            }
        }

        // Try to copy from JAR default - correct path in JAR
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("data/config/neoessentials/config.json")) {
            if (in != null) {
                Files.copy(in, configFile.toPath());
                LOGGER.info("Generated missing config.json from JAR default");
                return true;
            } else {
                LOGGER.error("Could not find default config.json in JAR (data/config/neoessentials/config.json)");
                return false;
            }
        } catch (IOException e) {
            LOGGER.error("Failed to generate config.json from JAR default", e);
            return false;
        }
    }
}
