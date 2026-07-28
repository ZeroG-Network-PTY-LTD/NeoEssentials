package com.zerog.neoessentials.config;

import com.google.gson.*;
import com.zerog.neoessentials.util.ResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * Handles splitting monolithic config.json into smaller, module-specific files
 * and merging them back for ConfigManager access.
 *
 * <p>File layout when split configs are enabled:
 * <pre>
 *   main.json         — modules, logging, permissions, kits, economy
 *   commands.json     — commands (enable/disable toggles)
 *   chat.json         — chat formatting, channels, anti-spam, badges
 *   teleportation.json— home, warp, spawn, tpa settings
 *   moderation.json   — ban, jail, freeze, kick, vanish settings
 *   items.json        — item spawn settings
 *   afk.json          — AFK system settings
 *   security.json     — security and validation settings
 *   tablist.json      — tablist display settings
 *   templates/discord_embed.json — discordEmbedTemplate (Discord chat-embed styling)
 * </pre>
 *
 * <p>{@code templates/discord_embed.json} is the one split file that lives in a subdirectory
 * rather than directly under {@code config/neoessentials/} — {@link #writeJsonFile} creates
 * that subdirectory on demand, since {@link #ensureConfigDir()} only guarantees the top-level
 * config directory exists.
 *
 * <p>Splitting is activated by the presence of a {@code .split_configs} marker file
 * in the {@code config/neoessentials/} directory.
 */
public class ConfigSplitter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigSplitter.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    // ── Section → File mapping (kept for backwards-compat / external callers) ──
    @SuppressWarnings("unused")
    public static final Map<String, String> CONFIG_FILE_MAP = new LinkedHashMap<>() {{
        put("modules",       "main.json");
        put("logging",       "main.json");
        put("permissions",   "main.json");
        put("kits",          "main.json");   // kits SETTINGS; kits.json = kit definitions (JsonArray)
        put("economy",       "main.json");   // economy config settings
        put("localization",  "main.json");
        put("security",      "security.json");
        put("commands",      "commands.json");
        put("items",         "items.json");
        put("afk",           "afk.json");
        put("teleportation", "teleportation.json");
        put("moderation",    "moderation.json");
        put("chat",          "chat.json");
        put("tablist",       "tablist.json");
        put("discordEmbedTemplate", "templates/discord_embed.json");
    }};

    /**
     * File → list of config sections it must contain.
     * This is the authoritative mapping used for generation and validation.
     */
    public static final Map<String, List<String>> FILE_SECTIONS_MAP = new LinkedHashMap<>() {{
        put("main.json",          List.of("modules", "logging", "permissions", "kits", "economy", "localization"));
        put("commands.json",      Collections.singletonList("commands"));
        put("chat.json",          Collections.singletonList("chat"));
        put("teleportation.json", Collections.singletonList("teleportation"));
        put("moderation.json",    Collections.singletonList("moderation"));
        put("items.json",         Collections.singletonList("items"));
        put("afk.json",           Collections.singletonList("afk"));
        put("security.json",      Collections.singletonList("security"));
        put("tablist.json",       Collections.singletonList("tablist"));
        put("templates/discord_embed.json", Collections.singletonList("discordEmbedTemplate"));
    }};

    // Version for each split config file
    private static final Map<String, Integer> SPLIT_CONFIG_VERSIONS = new HashMap<>() {{
        put("main.json",          1);
        put("commands.json",      1);
        put("chat.json",          1);
        put("teleportation.json", 1);
        put("moderation.json",    1);
        put("items.json",         1);
        put("afk.json",           2);  // v2  — invulnerableWhenAfk option
        put("security.json",      1);
        put("tablist.json",       1);
        put("templates/discord_embed.json", 1);
    }};

    /**
     * Current monolithic config version — must stay in sync with the JAR's config.json
     * {@code _configVersion} field and {@code ConfigManager.EXPECTED_CONFIG_VERSIONS}.
     */
    private static final int CURRENT_MAIN_VERSION = 23;

    // ── Marker ────────────────────────────────────────────────────────────────

    /** @return {@code true} if the {@code .split_configs} marker exists on disk. */
    public static boolean isSplittingEnabled() {
        return new File(ResourceUtil.CONFIG_DIR, ".split_configs").exists();
    }

    // ── Startup ensure ────────────────────────────────────────────────────────

    /**
     * Called on startup when split configs are active.
     * Ensures every expected split file exists and contains all required sections.
     * Missing or outdated files are regenerated/updated from the JAR default config.
     */
    public static void ensureSplitConfigsUpToDate() {
        if (!isSplittingEnabled()) return;

        LOGGER.debug("Checking split config files…");

        // Load the on-disk config.json (may be a stub) as a possible source for sections.
        // Primary fallback is always the JAR's config.json.
        JsonObject jarConfig   = loadJarConfig();
        JsonObject diskConfig  = loadDiskConfig();   // may be stub / empty

        // Process one FILE at a time — never process individual sections.
        // This prevents main.json being overwritten multiple times with only one section.
        for (Map.Entry<String, List<String>> entry : FILE_SECTIONS_MAP.entrySet()) {
            String fileName       = entry.getKey();
            List<String> sections = entry.getValue();
            File splitFile        = ResourceUtil.getConfigFile(fileName);

            if (!splitFile.exists()) {
                LOGGER.warn("Split config '{}' is missing. Attempting to regenerate…", fileName);
                boolean created = generateSplitFile(splitFile, fileName, sections, diskConfig, jarConfig);
                if (!created) {
                    LOGGER.error(
                        "╔══════════════════════════════════════════════════════════╗");
                    LOGGER.error(
                        "║  MISSING SPLIT CONFIG: {}",  fileName);
                    LOGGER.error(
                        "║  This file should contain: {}",  String.join(", ", sections));
                    LOGGER.error(
                        "║  Run: /neoe config repair   to regenerate all missing files.");
                    LOGGER.error(
                        "╚══════════════════════════════════════════════════════════╝");
                }
            } else {
                // File exists — check version and merge new keys
                checkSplitConfigVersion(fileName, splitFile, SPLIT_CONFIG_VERSIONS.getOrDefault(fileName, 1),
                                        jarConfig);
                // Also merge any sections that may be missing (e.g. economy added later)
                repairMissingSectionsInFile(splitFile, fileName, sections, diskConfig, jarConfig);
            }
        }

        LOGGER.debug("Split config check complete.");
    }

    // ── Migration ─────────────────────────────────────────────────────────────

    /**
     * Migrate from monolithic {@code config.json} to split configs.
     * Creates one split file per entry in {@link #FILE_SECTIONS_MAP}.
     *
     * @return {@code true} on success
     */
    public static boolean migrateToSplitConfigs() {
        try {
            File configFile = ResourceUtil.getConfigFile("config.json");
            JsonObject source;
            if (configFile.exists()) {
                source = readJsonFile(configFile);
                if (source == null) {
                    LOGGER.error("config.json is not valid JSON — cannot migrate.");
                    return false;
                }
            } else {
                // Fall back to the JAR default
                source = loadJarConfig();
                if (source == null) {
                    LOGGER.error("config.json not found and JAR default unavailable — cannot migrate.");
                    return false;
                }
                LOGGER.warn("config.json not found on disk. Migrating from JAR default instead.");
            }

            LOGGER.info("════════════════════════════════════════");
            LOGGER.info("Migrating to split configuration files…");
            LOGGER.info("════════════════════════════════════════");

            // Backup original
            if (configFile.exists()) {
                File backup = new File(configFile.getParentFile(), "config.json.backup");
                Files.copy(configFile.toPath(), backup.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                LOGGER.info("Created backup: config.json.backup");
            }

            ensureConfigDir();
            int created = 0;
            for (Map.Entry<String, List<String>> entry : FILE_SECTIONS_MAP.entrySet()) {
                String fileName       = entry.getKey();
                List<String> sections = entry.getValue();
                File targetFile       = ResourceUtil.getConfigFile(fileName);

                // Skip if already exists (avoid overwriting user edits during partial migration)
                if (targetFile.exists()) {
                    LOGGER.debug("  Skipping {} — already exists", fileName);
                    continue;
                }

                JsonObject fileContent = buildFileContent(fileName, sections, source);
                if (fileContent.entrySet().stream().allMatch(e -> e.getKey().startsWith("_"))) {
                    LOGGER.warn("  No sections found for {} — skipping", fileName);
                    continue;
                }
                writeJsonFile(targetFile, fileContent);
                LOGGER.info("  ✓ Created {}", fileName);
                created++;
            }

            // Create marker
            File marker = new File(ResourceUtil.CONFIG_DIR, ".split_configs");
            if (!marker.exists() && !marker.createNewFile()) {
                LOGGER.warn("Could not create .split_configs marker file — split mode may not persist.");
            }

            // Replace config.json with stub
            replaceWithStubFile(configFile);

            // Clear the ConfigManager cache so the newly created split files are picked
            // up on next access instead of the now-stale monolithic config.json entry.
            ConfigManager.getInstance().clearCache();

            LOGGER.info("════════════════════════════════════════");
            LOGGER.info("Migration complete! {} file(s) created.", created);
            LOGGER.info("Original config backed up to: config.json.backup");
            LOGGER.info("════════════════════════════════════════");
            return true;

        } catch (Exception e) {
            LOGGER.error("Failed to migrate to split configs: {}", e.getMessage(), e);
            return false;
        }
    }

    // ── Fresh install ─────────────────────────────────────────────────────────

    /** @return {@code true} if no config.json and no split configs exist. */
    @SuppressWarnings("unused")
    public static boolean isFreshInstall() {
        return !ResourceUtil.getConfigFile("config.json").exists() && !isSplittingEnabled();
    }

    /**
     * For fresh installs: create all split files from the JAR's default config.json.
     * Activates split configs mode automatically.
     *
     * @return {@code true} on success
     */
    @SuppressWarnings("unused")
    public static boolean autoSplitForFreshInstall() {
        if (ResourceUtil.getConfigFile("config.json").exists()) return false; // not a fresh install

        LOGGER.info("════════════════════════════════════════");
        LOGGER.info("Fresh NeoEssentials install detected.");
        LOGGER.info("Creating split configuration files…");
        LOGGER.info("════════════════════════════════════════");

        return createSplitConfigsFromJar();
    }

    /**
     * Create all split config files from the JAR's bundled {@code config.json}.
     * This is the correct path for fresh installs — never reads a non-existent on-disk file.
     */
    private static boolean createSplitConfigsFromJar() {
        try {
            JsonObject jarConfig = loadJarConfig();
            if (jarConfig == null) {
                LOGGER.error("Cannot find config.json inside the mod JAR — cannot create split configs.");
                return false;
            }

            ensureConfigDir();
            int created = 0;
            for (Map.Entry<String, List<String>> entry : FILE_SECTIONS_MAP.entrySet()) {
                String fileName       = entry.getKey();
                List<String> sections = entry.getValue();
                File targetFile       = ResourceUtil.getConfigFile(fileName);

                if (targetFile.exists()) continue;

                JsonObject fileContent = buildFileContent(fileName, sections, jarConfig);
                writeJsonFile(targetFile, fileContent);
                LOGGER.info("  ✓ Created {}", fileName);
                created++;
            }

            // Create marker
            File marker = new File(ResourceUtil.CONFIG_DIR, ".split_configs");
            if (!marker.exists() && !marker.createNewFile()) {
                LOGGER.warn("Could not create .split_configs marker file — split mode may not persist.");
            }

            LOGGER.info("════════════════════════════════════════");
            LOGGER.info("Split configs created ({} files). Configuration is ready.", created);
            LOGGER.info("════════════════════════════════════════");
            return created > 0;

        } catch (Exception e) {
            LOGGER.error("Failed to create split configs from JAR: {}", e.getMessage(), e);
            return false;
        }
    }

    // ── Validation ────────────────────────────────────────────────────────────

    /**
     * Validate all split config files.
     * Returns a list of human-readable problem strings.
     * An empty list means everything is OK.
     */
    public static List<String> validateSplitConfigs() {
        List<String> problems = new ArrayList<>();

        if (!isSplittingEnabled()) {
            problems.add("Split configs are NOT enabled (.split_configs marker missing). " +
                         "Run /neoe config split to migrate.");
            return problems;
        }

        for (Map.Entry<String, List<String>> entry : FILE_SECTIONS_MAP.entrySet()) {
            String fileName       = entry.getKey();
            List<String> sections = entry.getValue();
            File f                = ResourceUtil.getConfigFile(fileName);

            if (!f.exists()) {
                problems.add("MISSING FILE: " + fileName +
                             "  →  should contain: " + String.join(", ", sections) +
                             "  →  fix with: /neoe config repair");
                continue;
            }

            JsonObject obj = readJsonFile(f);
            if (obj == null) {
                problems.add("PARSE ERROR: " + fileName + " is not valid JSON — " +
                             "restore from backup or run /neoe config repair");
                continue;
            }

            for (String section : sections) {
                if (!obj.has(section)) {
                    problems.add("MISSING SECTION '" + section + "' in " + fileName +
                                 "  →  fix with: /neoe config repair");
                }
            }
        }

        return problems;
    }

    /**
     * Attempt to repair split configs:
     * - Regenerate any missing files from the JAR default
     * - Add any missing sections to existing files
     *
     * @return number of files created or repaired (0 = nothing needed fixing)
     */
    public static int repairSplitConfigs() {
        if (!isSplittingEnabled()) {
            LOGGER.warn("Split configs are not enabled — nothing to repair.");
            return 0;
        }

        JsonObject jarConfig  = loadJarConfig();
        JsonObject diskConfig = loadDiskConfig();
        if (jarConfig == null) {
            LOGGER.error("Cannot load JAR config.json — repair aborted.");
            return 0;
        }

        int repaired = 0;
        for (Map.Entry<String, List<String>> entry : FILE_SECTIONS_MAP.entrySet()) {
            String fileName       = entry.getKey();
            List<String> sections = entry.getValue();
            File splitFile        = ResourceUtil.getConfigFile(fileName);

            if (!splitFile.exists()) {
                if (generateSplitFile(splitFile, fileName, sections, diskConfig, jarConfig)) {
                    LOGGER.info("  ✓ Repaired (created) {}", fileName);
                    repaired++;
                }
            } else {
                // Check for missing sections
                boolean changed = repairMissingSectionsInFile(splitFile, fileName, sections, diskConfig, jarConfig);
                if (changed) {
                    LOGGER.info("  ✓ Repaired (added missing sections to) {}", fileName);
                    repaired++;
                }
            }
        }

        return repaired;
    }

    // ── Merge for ConfigManager ───────────────────────────────────────────────

    /**
     * Merge all split config files into one virtual JsonObject for ConfigManager.
     */
    public static JsonObject mergeSplitConfigs() {
        JsonObject merged = new JsonObject();
        merged.addProperty("_configVersion", CURRENT_MAIN_VERSION);
        merged.addProperty("_configVersion_comment",
            "NOTE: This is a virtual merged view. Edit individual split config files instead.");

        for (Map.Entry<String, List<String>> entry : FILE_SECTIONS_MAP.entrySet()) {
            String fileName       = entry.getKey();
            List<String> sections = entry.getValue();
            File configFile       = ResourceUtil.getConfigFile(fileName);

            if (!configFile.exists()) {
                LOGGER.warn("Split config '{}' not found during merge — some settings may use defaults.", fileName);
                continue;
            }

            JsonObject fileObj = readJsonFile(configFile);
            if (fileObj == null) {
                LOGGER.error("Failed to parse split config '{}' — skipping.", fileName);
                continue;
            }

            for (String section : sections) {
                if (fileObj.has(section) && !merged.has(section)) {
                    // Only merge JsonObject sections (never let a JsonArray sneak in as a section)
                    if (fileObj.get(section).isJsonObject()) {
                        merged.add(section, fileObj.get(section));
                    } else if (!section.equals("kits")) {
                        // Allow non-object for non-kits sections (shouldn't happen normally)
                        merged.add(section, fileObj.get(section));
                    }
                }
            }
        }

        return merged;
    }

    /**
     * Persists a merged/virtual config view (as produced by {@link #mergeSplitConfigs()},
     * then possibly mutated by a caller) back out to the individual split files, one section
     * at a time. Used by {@link ConfigManager#saveConfig} when split configs are active, since
     * writes must land in {@code main.json}/{@code chat.json}/etc. rather than a nonexistent
     * monolithic {@code config.json}.
     */
    public static void saveMergedConfigToSplitFiles(JsonObject merged) {
        for (Map.Entry<String, List<String>> entry : FILE_SECTIONS_MAP.entrySet()) {
            String fileName = entry.getKey();
            List<String> sections = entry.getValue();
            File configFile = ResourceUtil.getConfigFile(fileName);

            JsonObject fileObj = configFile.exists() ? readJsonFile(configFile) : null;
            if (fileObj == null) fileObj = new JsonObject();

            boolean changed = false;
            for (String section : sections) {
                if (merged.has(section)) {
                    fileObj.add(section, merged.get(section));
                    changed = true;
                }
            }
            if (!changed) continue;

            try {
                writeJsonFile(configFile, fileObj);
            } catch (IOException e) {
                LOGGER.error("Failed to write split config file '{}': {}", fileName, e.getMessage());
            }
        }
    }

    // ── Startup prompt ────────────────────────────────────────────────────────

    private static boolean shouldNotifyAdmins = false;

    public static void checkAndPromptMigration() {
        if (isSplittingEnabled()) return;

        File configFile = ResourceUtil.getConfigFile("config.json");
        if (configFile.exists()) {
            LOGGER.info("════════════════════════════════════════════════════════");
            LOGGER.info("NOTICE: Monolithic config.json detected.");
            LOGGER.info("NeoEssentials supports split configuration files for easier management.");
            LOGGER.info("Run:  /neoe config split  to migrate to split files.");
            LOGGER.info("Run:  /neoe config status  to see the current config state.");
            LOGGER.info("════════════════════════════════════════════════════════");
            shouldNotifyAdmins = true;
        }
    }

    @SuppressWarnings("unused")
    public static boolean shouldNotifyAdmins()  { return shouldNotifyAdmins; }
    @SuppressWarnings("unused")
    public static void markAdminsNotified()     { shouldNotifyAdmins = false; }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Build a single split file's JsonObject containing the given sections from {@code source}.
     * Sections missing from {@code source} are silently omitted (not all installs have all sections).
     */
    private static JsonObject buildFileContent(String fileName, List<String> sections, JsonObject source) {
        JsonObject result = new JsonObject();
        Integer version = SPLIT_CONFIG_VERSIONS.get(fileName);
        if (version != null) {
            result.addProperty("_configVersion", version);
            // Note: _configVersion_comment is intentionally omitted — it is treated as a
            // legacy key by the upgrade system and would be stripped on next version bump.
        }
        for (String section : sections) {
            if (source != null && source.has(section)) {
                result.add(section, source.get(section));
            }
        }
        return result;
    }

    /**
     * Generate a missing split file. Tries disk config first, then JAR default.
     * @return {@code true} if the file was created successfully.
     */
    private static boolean generateSplitFile(File targetFile, String fileName, List<String> sections,
                                             JsonObject diskConfig, JsonObject jarConfig) {
        // Try to get sections from disk config first, fall back to JAR
        JsonObject source = (diskConfig != null && !diskConfig.entrySet().isEmpty()) ? diskConfig : jarConfig;
        if (source == null) {
            LOGGER.error("No config source available to generate '{}'", fileName);
            return false;
        }

        JsonObject content = buildFileContent(fileName, sections, source);
        // Check we got at least one real section
        boolean hasSections = content.entrySet().stream().anyMatch(e -> !e.getKey().startsWith("_"));
        if (!hasSections) {
            LOGGER.warn("No sections available for '{}' from source config — file not created.", fileName);
            return false;
        }

        try {
            ensureConfigDir();
            writeJsonFile(targetFile, content);
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to write '{}': {}", fileName, e.getMessage());
            return false;
        }
    }

    /**
     * Check if existing sections in a split file are missing and add them from source configs.
     * @return {@code true} if any changes were made.
     */
    private static boolean repairMissingSectionsInFile(File splitFile, String fileName, List<String> sections,
                                                       JsonObject diskConfig, JsonObject jarConfig) {
        JsonObject onDisk = readJsonFile(splitFile);
        if (onDisk == null) return false;

        boolean changed = false;
        JsonObject source = (diskConfig != null && !diskConfig.entrySet().isEmpty()) ? diskConfig : jarConfig;

        for (String section : sections) {
            if (!onDisk.has(section) && source != null && source.has(section)) {
                onDisk.add(section, source.get(section));
                LOGGER.info("  Added missing section '{}' to {}", section, fileName);
                changed = true;
            }
        }

        if (changed) {
            try {
                writeJsonFile(splitFile, onDisk);
            } catch (Exception e) {
                LOGGER.error("Failed to write repaired '{}': {}", fileName, e.getMessage());
                return false;
            }
        }
        return changed;
    }

    /**
     * Check / upgrade a split config file's version.
     * Only adds NEW keys from the JAR template — never overwrites user values.
     * Also strips leftover legacy {@code _comment} / {@code _doc_*} keys on upgrade.
     */
    private static void checkSplitConfigVersion(String fileName, File configFile,
                                                int expectedVersion, JsonObject jarConfig) {
        JsonObject onDisk = readJsonFile(configFile);
        if (onDisk == null) return;

        int current = onDisk.has("_configVersion") ? onDisk.get("_configVersion").getAsInt() : 0;
        if (current >= expectedVersion) {
            LOGGER.debug("Split config {} is up to date (v{})", fileName, current);
            return;
        }

        LOGGER.warn("Split config '{}' is outdated (v{} < v{}). Merging new keys…",
                    fileName, current, expectedVersion);

        // Backup
        try {
            String ts = new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new java.util.Date());
            String backupName = fileName.replace(".json",
                    String.format("_v%d_backup_%s.json", current, ts));
            Files.copy(configFile.toPath(), new File(configFile.getParentFile(), backupName).toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            LOGGER.warn("Could not create backup for {}: {}", fileName, e.getMessage());
        }

        // Get the JAR sections for this file
        List<String> sections = FILE_SECTIONS_MAP.getOrDefault(fileName, Collections.emptyList());
        for (String section : sections) {
            if (jarConfig != null && jarConfig.has(section)) {
                if (onDisk.has(section) && onDisk.get(section).isJsonObject() &&
                        jarConfig.get(section).isJsonObject()) {
                    mergeJsonObjects(onDisk.getAsJsonObject(section),
                                     jarConfig.getAsJsonObject(section));
                } else if (!onDisk.has(section)) {
                    onDisk.add(section, jarConfig.get(section));
                }
            }
        }

        // Strip legacy _comment / _doc_* / _step* keys left over from older file formats
        stripLegacyCommentKeys(onDisk);

        onDisk.addProperty("_configVersion", expectedVersion);
        try {
            writeJsonFile(configFile, onDisk);
            LOGGER.info("Updated split config '{}' to v{}", fileName, expectedVersion);
        } catch (Exception e) {
            LOGGER.error("Failed to write updated '{}': {}", fileName, e.getMessage());
        }
    }

    /**
     * Recursively remove legacy "comment" keys from a {@link JsonObject}.
     * Removes keys that end with {@code _comment}, end with {@code -description},
     * or start with {@code _} but are NOT {@code _configVersion}.
     *
     * @return {@code true} if at least one key was removed
     */
    private static boolean stripLegacyCommentKeys(JsonObject obj) {
        boolean changed = false;
        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            String key = entry.getKey();
            if (isLegacyCommentKey(key)) {
                toRemove.add(key);
            } else if (entry.getValue().isJsonObject()) {
                changed |= stripLegacyCommentKeys(entry.getValue().getAsJsonObject());
            }
        }
        for (String key : toRemove) {
            obj.remove(key);
            changed = true;
            LOGGER.debug("  - Removed legacy comment key '{}' from split config", key);
        }
        return changed;
    }

    private static boolean isLegacyCommentKey(String key) {
        if ("_configVersion".equals(key)) return false; // always preserve version field
        return key.endsWith("_comment") || key.endsWith("-description") || key.startsWith("_");
    }

    /**
     * Merge {@code source} keys into {@code target} — only adds MISSING keys, never overwrites.
     * @return {@code true} if any keys were added.
     */
    private static boolean mergeJsonObjects(JsonObject target, JsonObject source) {
        boolean changed = false;
        for (Map.Entry<String, JsonElement> entry : source.entrySet()) {
            String key = entry.getKey();
            JsonElement val = entry.getValue();
            if (!target.has(key)) {
                target.add(key, val);
                changed = true;
            } else if (val.isJsonObject() && target.get(key).isJsonObject()) {
                changed |= mergeJsonObjects(target.getAsJsonObject(key), val.getAsJsonObject());
            }
        }
        return changed;
    }


    // ── Stub file ─────────────────────────────────────────────────────────────

    private static void replaceWithStubFile(File configFile) throws IOException {
        JsonObject stub = new JsonObject();
        stub.addProperty("_configVersion", CURRENT_MAIN_VERSION);
        stub.addProperty("_configVersion_comment",
            "DO NOT MODIFY: Used by NeoEssentials for automatic config updates.");
        stub.addProperty("_notice",
            "This server is using SPLIT CONFIGURATION FILES.");
        stub.addProperty("_notice_info",
            "Edit the smaller files in config/neoessentials/ instead of this file.");

        JsonObject guide = new JsonObject();
        for (Map.Entry<String, List<String>> e : FILE_SECTIONS_MAP.entrySet()) {
            guide.addProperty(e.getKey(), String.join(", ", e.getValue()));
        }
        stub.add("_split_config_files", guide);
        stub.addProperty("_restore_instructions",
            "To restore: delete .split_configs marker and restore from config.json.backup");

        writeJsonFile(configFile, stub);
    }

    // ── I/O utilities ─────────────────────────────────────────────────────────

    /**
     * Parse a JSON reader in lenient mode so that {@code //} and {@code /* *\/} comments
     * (present in config.json since v22) are accepted without errors.
     */
    private static JsonObject parseLenient(java.io.Reader reader) {
        com.google.gson.stream.JsonReader jr = new com.google.gson.stream.JsonReader(reader);
        jr.setLenient(true);
        return JsonParser.parseReader(jr).getAsJsonObject();
    }

    /** Load the bundled {@code config.json} from the mod JAR. Returns {@code null} on failure. */
    private static JsonObject loadJarConfig() {
        try (InputStream in = ResourceUtil.getJarConfigResource("config.json")) {
            if (in == null) {
                LOGGER.error("config.json not found in JAR at {}", ResourceUtil.JAR_CONFIG_PATH);
                return null;
            }
            // Lenient reader required: config.json uses // comment syntax since v22.
            return parseLenient(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (Exception e) {
            LOGGER.error("Failed to parse JAR config.json: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Load the on-disk {@code config.json}.
     * Returns an empty JsonObject (not {@code null}) if the file is missing or is a stub.
     */
    private static JsonObject loadDiskConfig() {
        File f = ResourceUtil.getConfigFile("config.json");
        if (!f.exists()) return new JsonObject();
        JsonObject obj = readJsonFile(f);
        if (obj == null) return new JsonObject();
        // If it's a stub (has _notice but no real sections) return empty so we fall back to JAR
        boolean hasSections = FILE_SECTIONS_MAP.values().stream()
            .flatMap(Collection::stream)
            .anyMatch(obj::has);
        return hasSections ? obj : new JsonObject();
    }

    /**
     * Read a JSON file in lenient mode.
     * Lenient mode is used for consistency with the JAR config reader and to handle
     * any edge-case comments the user may have added. Returns {@code null} on parse/IO error.
     */
    private static JsonObject readJsonFile(File f) {
        try (FileReader reader = new FileReader(f, StandardCharsets.UTF_8)) {
            return parseLenient(reader);
        } catch (Exception e) {
            LOGGER.error("Failed to read/parse '{}': {}", f.getName(), e.getMessage());
            return null;
        }
    }

    /** Write a JsonObject to a file (pretty-printed). Creates the file's parent directory
     * first — most split files sit directly in {@link ResourceUtil#CONFIG_DIR}, already
     * covered by {@link #ensureConfigDir()}, but {@code templates/discord_embed.json} needs
     * its own {@code templates/} subdirectory created on demand. */
    private static void writeJsonFile(File f, JsonObject obj) throws IOException {
        ensureConfigDir();
        File parent = f.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            LOGGER.warn("Could not create directory: {}", parent.getAbsolutePath());
        }
        try (FileWriter w = new FileWriter(f, StandardCharsets.UTF_8)) {
            GSON.toJson(obj, w);
        }
    }

    private static void ensureConfigDir() {
        File dir = new File(ResourceUtil.CONFIG_DIR);
        if (!dir.exists() && !dir.mkdirs()) {
            LOGGER.warn("Could not create config directory: {}", dir.getAbsolutePath());
        }
    }

}
