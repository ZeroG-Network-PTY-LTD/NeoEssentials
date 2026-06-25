package com.zerog.neoessentials.i18n;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.zerog.neoessentials.util.MessageUtil;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enhanced language file manager that supports custom user-created language files
 * and automatic template generation for easy translation.
 */
public class CustomLanguageManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomLanguageManager.class);
    private static CustomLanguageManager INSTANCE;
    // Use only the correct path under the mod's data directory
    @SuppressWarnings("unused") // kept for external tools that may reference this constant
    private static final String LANG_DIR = "neoessentials/languages/custom/";
    private static final String LANG_FILE = "en_us.json";
    private final Path customLangDir;
    private final Path templatesDir;
    private final Path overridesFile;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    // Track missing translation keys for template generation
    private final Set<String> missingKeys = ConcurrentHashMap.newKeySet();
    private final Map<String, Map<String, String>> customTranslations = new ConcurrentHashMap<>();
    private final Map<String, LanguageFileInfo> languageFiles = new ConcurrentHashMap<>();
    /** Admin overrides — take priority over all language files and the JAR en_us.json. */
    private final Map<String, String> overrides = new ConcurrentHashMap<>();

    private CustomLanguageManager() {
        // Always resolve relative to the server root, not 'run/'
        this.customLangDir = resolveModDataPath("languages", "custom");
        this.templatesDir = resolveModDataPath("languages", "templates");
        this.overridesFile = resolveModDataPath("languages", "overrides.json");
        LOGGER.info("[LANG] Custom language directory set to: {}", this.customLangDir.toAbsolutePath());
        LOGGER.info("[LANG] Template directory set to: {}", this.templatesDir.toAbsolutePath());
        LOGGER.info("[LANG] Overrides file set to: {}", this.overridesFile.toAbsolutePath());
    }

    public static CustomLanguageManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new CustomLanguageManager();
        }
        return INSTANCE;
    }

    /**
     * Initialize custom language system
     */
    public void initialize() {
        try {
            LOGGER.info("Custom language directory resolved to: {}", customLangDir.toAbsolutePath());
            LOGGER.info("Template directory resolved to: {}", templatesDir.toAbsolutePath());

            // Create directories
            Files.createDirectories(customLangDir);
            Files.createDirectories(templatesDir);

            // Ensure language file exists (copy from JAR if missing or empty/invalid)
            Path langFile = customLangDir.resolve(LANG_FILE);
            boolean needsCopy = false;
            if (!Files.exists(langFile)) {
                needsCopy = true;
            } else {
                // Check if file is empty or invalid JSON
                try (Reader reader = Files.newBufferedReader(langFile, StandardCharsets.UTF_8)) {
                    Type type = new TypeToken<Map<String, String>>(){}.getType();
                    Map<String, String> test = gson.fromJson(reader, type);
                    if (test == null || test.isEmpty()) {
                        needsCopy = true;
                    }
                } catch (Exception e) {
                    needsCopy = true;
                }
            }
            if (needsCopy) {
                try (InputStream in = findLangResource(LANG_FILE)) {
                    if (in != null) {
                        Files.copy(in, langFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        LOGGER.info("Copied language file from JAR: {}", langFile.toAbsolutePath());
                    } else {
                        LOGGER.error("Failed to copy language file: Resource not found for {}!", LANG_FILE);
                    }
                } catch (Exception e) {
                    LOGGER.error("Exception while copying language file from JAR: {}", e.getMessage(), e);
                }
            } else {
                // Merge missing keys from JAR version
                Map<String, String> jarLang = loadBaseTranslations();
                Map<String, String> fileLang;
                try (Reader reader = Files.newBufferedReader(langFile, StandardCharsets.UTF_8)) {
                    Type type = new TypeToken<Map<String, String>>(){}.getType();
                    fileLang = gson.fromJson(reader, type);
                }
                boolean updated = false;
                if (fileLang != null && jarLang != null) {
                    for (Map.Entry<String, String> entry : jarLang.entrySet()) {
                        if (!fileLang.containsKey(entry.getKey())) {
                            fileLang.put(entry.getKey(), entry.getValue());
                            updated = true;
                        }
                    }
                    if (updated) {
                        try (Writer writer = Files.newBufferedWriter(langFile, StandardCharsets.UTF_8)) {
                            gson.toJson(fileLang, writer);
                            LOGGER.info("Merged missing keys from JAR into language file: {}", langFile.toAbsolutePath());
                        }
                    }
                }
            }
            // After copy/merge attempt, check if file exists
            if (!Files.exists(langFile)) {
                LOGGER.error("Critical: Language file {} still does not exist after copy attempt! Translations will not be loaded from disk.", langFile.toAbsolutePath());
            }

            // Deploy all other bundled language files from the JAR (fr_fr, de_de, es_es, etc.)
            deployBundledLanguageFiles();

            // Scan for custom language files (will load the file)
            scanCustomLanguageFiles();

            // Load admin overrides
            loadOverrides();

            // Generate templates for common languages if they don't exist
            generateTemplatesIfNeeded();

            LOGGER.info("Custom Language Manager initialized");
            LOGGER.info("  Custom languages found: {}", languageFiles.keySet());
            LOGGER.info("  Custom language directory: {}", customLangDir.toAbsolutePath());
            LOGGER.info("  Template directory: {}", templatesDir.toAbsolutePath());
        } catch (Exception e) {
            LOGGER.error("Failed to initialize custom language manager", e);
        }
    }

    /**
     * Scan for custom language files in the custom directory
     */
    private void scanCustomLanguageFiles() {
        try {
            if (!Files.exists(customLangDir)) {
                return;
            }
            try (var stream = Files.list(customLangDir)) {
                stream.filter(path -> path.toString().endsWith(".json"))
                      .forEach(this::loadCustomLanguageFile);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to scan custom language files", e);
        }
    }

    /**
     * Load a custom language file
     */
    private void loadCustomLanguageFile(Path filePath) {
        try {
            String fileName = filePath.getFileName().toString();
            String langCode = fileName.replace(".json", "");

            String content = Files.readString(filePath, StandardCharsets.UTF_8);
            Type type = new TypeToken<Map<String, String>>(){}.getType();
            Map<String, String> translations = gson.fromJson(content, type);

            if (translations != null && !translations.isEmpty()) {
                customTranslations.put(langCode, translations);

                // Extract metadata from the file
                String nativeName = translations.getOrDefault("_nativeName", langCode);
                String englishName = translations.getOrDefault("_englishName", langCode);
                String author = translations.getOrDefault("_author", "Unknown");
                String version = translations.getOrDefault("_version", "1.0");

                languageFiles.put(langCode, new LanguageFileInfo(
                    langCode, nativeName, englishName, author, version, filePath
                ));

                LOGGER.info("Loaded custom language: {} ({}) - {} keys",
                    langCode, nativeName, translations.size());
            }

        } catch (Exception e) {
            LOGGER.error("Failed to load custom language file: {}", filePath, e);
        }
    }

    /**
     * Get translation from custom language file, with fallback to MessageUtil.
     * Admin overrides take priority over everything.
     */
    @SuppressWarnings("unused") // public API — called by TranslationHandler and external integrations
    public String getTranslation(String key, String languageCode) {
        // Admin overrides take top priority
        if (overrides.containsKey(key)) {
            return overrides.get(key);
        }

        // Try custom translation first
        Map<String, String> customLang = customTranslations.get(languageCode);
        if (customLang != null && customLang.containsKey(key)) {
            return customLang.get(key);
        }

        // Fall back to default MessageUtil
        if (MessageUtil.hasTranslation(key)) {
            return MessageUtil.localize(key);
        }

        // Track missing key for template generation
        missingKeys.add(key);

        // Return key as fallback
        return key;
    }

    /**
     * Check if a custom language is available
     */
    @SuppressWarnings("unused") // public API
    public boolean hasCustomLanguage(String languageCode) {
        return customTranslations.containsKey(languageCode);
    }

    /**
     * Get list of all available custom languages
     */
    public List<LanguageFileInfo> getCustomLanguages() {
        return new ArrayList<>(languageFiles.values());
    }

    /**
     * Generate language template files for easy translation
     */
    private void generateTemplatesIfNeeded() {
        String[] languagesToGenerate = {
            "es_es", "fr_fr", "de_de", "it_it", "pt_br",
            "ru_ru", "ja_jp", "ko_kr", "zh_cn", "nl_nl"
        };

        for (String langCode : languagesToGenerate) {
            Path templatePath = templatesDir.resolve(langCode + "_template.json");
            if (!Files.exists(templatePath)) {
                generateTemplate(langCode, templatePath);
            }
        }
    }

    /**
     * Generate a translation template file
     */
    public void generateTemplate(String languageCode, Path outputPath) {
        try {
            // Get all keys from the English (base) file
            Map<String, String> baseTranslations = loadBaseTranslations();

            // Create template map with metadata
            Map<String, String> template = new LinkedHashMap<>();
            template.put("_comment", "NeoEssentials Custom Language File");
            template.put("_nativeName", "Language Name (in native language)");
            template.put("_englishName", "Language Name (in English)");
            template.put("_languageCode", languageCode);
            template.put("_author", "Your Name");
            template.put("_version", "1.0");
            template.put("_minModVersion", "1.0.2.4");
            template.put("_lastUpdated", new Date().toString());
            template.put("_rtl", "false");
            template.put("", "");
            template.put("_instructions", "To translate: Replace the English text on the right side of each line with your language. Keep the {0}, {1} placeholders exactly as they are.");
            template.put("__example", "For example: \"commands.example\": \"Your translation here with {0} placeholder\"");
            template.put("___", "");

            // Add all translation keys with English values as placeholders
            for (Map.Entry<String, String> entry : baseTranslations.entrySet()) {
                String key = entry.getKey();
                if (!key.startsWith("_")) {  // Skip metadata keys
                    template.put(key, "[TRANSLATE] " + entry.getValue());
                }
            }

            // Write template file
            Files.createDirectories(outputPath.getParent());
            try (Writer writer = new OutputStreamWriter(
                    new FileOutputStream(outputPath.toFile()), StandardCharsets.UTF_8)) {
                gson.toJson(template, writer);
            }

            LOGGER.info("Generated language template: {} ({} keys)", outputPath, template.size());

        } catch (Exception e) {
            LOGGER.error("Failed to generate language template for {}", languageCode, e);
        }
    }

    /**
     * Export current missing keys to a template file
     */
    public void exportMissingKeys(Path outputPath) {
        try {
            Map<String, String> missingTemplate = new LinkedHashMap<>();
            missingTemplate.put("_comment", "Missing Translation Keys - Add these to your language file");
            missingTemplate.put("_generated", new Date().toString());
            missingTemplate.put("_count", String.valueOf(missingKeys.size()));
            missingTemplate.put("", "");

            for (String key : missingKeys) {
                missingTemplate.put(key, "[TRANSLATE] " + key);
            }

            try (Writer writer = new OutputStreamWriter(
                    new FileOutputStream(outputPath.toFile()), StandardCharsets.UTF_8)) {
                gson.toJson(missingTemplate, writer);
            }

            LOGGER.info("Exported {} missing keys to {}", missingKeys.size(), outputPath);

        } catch (Exception e) {
            LOGGER.error("Failed to export missing keys", e);
        }
    }

    /**
     * Load base translations from en_us.json
     */
    private Map<String, String> loadBaseTranslations() {
        try {
            InputStream is = getClass().getClassLoader()
                .getResourceAsStream("data/lang/en_us.json");

            if (is != null) {
                try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    Type type = new TypeToken<Map<String, String>>(){}.getType();
                    return gson.fromJson(reader, type);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load base translations", e);
        }

        return new HashMap<>();
    }

    /**
     * Reload all custom language files.
     *
     * <p>Also re-runs {@link #deployBundledLanguageFiles()} so that any new keys
     * added to bundled JAR translations (fr_fr, de_de, etc.) since the last server
     * start are merged into the on-disk files before they are re-read into memory.
     * User edits are preserved — only missing keys are added.
     */
    public void reload() {
        customTranslations.clear();
        languageFiles.clear();
        overrides.clear();
        // Re-merge new JAR keys into all bundled language files before re-scanning disk
        deployBundledLanguageFiles();
        scanCustomLanguageFiles();
        loadOverrides();
        LOGGER.info("Reloaded custom languages: {}", languageFiles.keySet());
    }

    /**
     * Get statistics about translations
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("customLanguagesLoaded", customTranslations.size());
        stats.put("languageCodes", new ArrayList<>(customTranslations.keySet()));
        stats.put("missingKeysTracked", missingKeys.size());
        stats.put("overrideCount", overrides.size());
        stats.put("customLanguageDirectory", customLangDir.toAbsolutePath().toString());
        stats.put("templateDirectory", templatesDir.toAbsolutePath().toString());

        return stats;
    }

    /**
     * Clear missing keys tracker
     */
    public void clearMissingKeys() {
        missingKeys.clear();
    }

    /**
     * Get all missing keys
     */
    public Set<String> getMissingKeys() {
        return new HashSet<>(missingKeys);
    }

    /**
     * Language file information class
     */
    public static class LanguageFileInfo {
        private final String languageCode;
        private final String nativeName;
        private final String englishName;
        private final String author;
        private final String version;
        private final Path filePath;

        public LanguageFileInfo(String languageCode, String nativeName, String englishName,
                                String author, String version, Path filePath) {
            this.languageCode = languageCode;
            this.nativeName = nativeName;
            this.englishName = englishName;
            this.author = author;
            this.version = version;
            this.filePath = filePath;
        }

        public String getLanguageCode() { return languageCode; }
        public String getNativeName() { return nativeName; }
        public String getEnglishName() { return englishName; }
        public String getAuthor() { return author; }
        public String getVersion() { return version; }
        @SuppressWarnings("unused") // public API
        public Path getFilePath() { return filePath; }
    }

    /**
     * Deploy all bundled language files from the JAR to the custom language directory.
     * - If the file does not exist on disk → copy from JAR (first-run)
     * - If the file already exists → merge only NEW keys from the JAR, preserving user edits
     * Skips en_us.json (handled separately above).
     */
    private void deployBundledLanguageFiles() {
        // All language codes bundled in the JAR (excluding en_us which is handled separately)
        String[] bundledLangs = {"fr_fr", "de_de", "es_es", "pt_br", "zh_cn", "nl_nl", "pl_pl", "ru_ru"};
        int deployed = 0;
        int merged = 0;
        for (String langCode : bundledLangs) {
            String fileName = langCode + ".json";
            Path target = customLangDir.resolve(fileName);
            try {
                try (InputStream in = findLangResource(fileName)) {
                    if (in == null) {
                        LOGGER.debug("No bundled lang file found in JAR for: {}", langCode);
                        continue;
                    }
                    if (!Files.exists(target)) {
                        // First run — copy from JAR
                        Files.copy(in, target);
                        LOGGER.info("Deployed bundled language file: {}", fileName);
                        deployed++;
                    } else {
                        // Already on disk — merge NEW keys only, don't overwrite user edits
                        Type type = new TypeToken<Map<String, String>>(){}.getType();
                        Map<String, String> jarLang;
                        try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                            jarLang = gson.fromJson(reader, type);
                        }
                        Map<String, String> diskLang;
                        try (Reader reader = Files.newBufferedReader(target, StandardCharsets.UTF_8)) {
                            diskLang = gson.fromJson(reader, type);
                        }
                        if (jarLang != null && diskLang != null) {
                            boolean updated = false;
                            for (Map.Entry<String, String> entry : jarLang.entrySet()) {
                                if (!diskLang.containsKey(entry.getKey())) {
                                    diskLang.put(entry.getKey(), entry.getValue());
                                    updated = true;
                                }
                            }
                            if (updated) {
                                try (Writer writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
                                    gson.toJson(diskLang, writer);
                                }
                                LOGGER.info("Merged new keys from JAR into {}", fileName);
                                merged++;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Failed to deploy/merge bundled language file {}: {}", fileName, e.getMessage());
            }
        }
        if (deployed > 0 || merged > 0) {
            LOGGER.info("Language deployment complete: {} deployed, {} merged", deployed, merged);
        }
    }

    // =========================================================================
    // Admin Override Support
    // =========================================================================

    /**
     * Load admin overrides from overrides.json.
     * Overrides take priority over all language files.
     */
    private void loadOverrides() {
        if (!Files.exists(overridesFile)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(overridesFile, StandardCharsets.UTF_8)) {
            Type type = new TypeToken<Map<String, String>>(){}.getType();
            Map<String, String> loaded = gson.fromJson(reader, type);
            if (loaded != null) {
                overrides.putAll(loaded);
                LOGGER.info("Loaded {} admin translation override(s) from {}", loaded.size(), overridesFile);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load translation overrides from {}: {}", overridesFile, e.getMessage(), e);
        }
    }

    /**
     * Save admin overrides to overrides.json.
     */
    private void saveOverrides() {
        try {
            Files.createDirectories(overridesFile.getParent());
            try (Writer writer = Files.newBufferedWriter(overridesFile, StandardCharsets.UTF_8)) {
                gson.toJson(overrides, writer);
            }
            LOGGER.info("Saved {} admin translation override(s) to {}", overrides.size(), overridesFile);
        } catch (Exception e) {
            LOGGER.error("Failed to save translation overrides: {}", e.getMessage(), e);
        }
    }

    /**
     * Set an admin override for a translation key.
     */
    public void setOverride(String key, String value) {
        overrides.put(key, value);
        saveOverrides();
    }

    /**
     * Remove an admin override.
     * @return true if it existed and was removed
     */
    public boolean removeOverride(String key) {
        boolean existed = overrides.containsKey(key);
        overrides.remove(key);
        if (existed) saveOverrides();
        return existed;
    }

    /**
     * Get all current admin overrides.
     */
    public Map<String, String> getOverrides() {
        return new LinkedHashMap<>(overrides);
    }

    /**
     * Get a specific override value, or null if not set.
     */
    public String getOverride(String key) {
        return overrides.get(key);
    }

    /**
     * Clear all admin overrides.
     */
    public void clearOverrides() {
        overrides.clear();
        saveOverrides();
    }

    // =========================================================================
    // Validation & Regeneration
    // =========================================================================

    /**
     * Validate a language file against the base English translations.
     * Returns a report with coverage stats and lists of missing/extra keys.
     */
    public ValidationReport validateLanguage(String languageCode) {
        Map<String, String> base = loadBaseTranslations();
        Set<String> baseKeys = new LinkedHashSet<>();
        for (String k : base.keySet()) {
            if (!k.startsWith("_")) baseKeys.add(k);
        }

        Map<String, String> target = customTranslations.get(languageCode);
        if (target == null) {
            // Try loading from disk directly
            Path filePath = customLangDir.resolve(languageCode + ".json");
            if (Files.exists(filePath)) {
                try (Reader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
                    Type type = new TypeToken<Map<String, String>>(){}.getType();
                    target = gson.fromJson(reader, type);
                } catch (Exception e) {
                    return new ValidationReport(languageCode, 0, 0, 0,
                        Collections.emptyList(), Collections.emptyList(),
                        "Failed to read file: " + e.getMessage());
                }
            } else {
                return new ValidationReport(languageCode, 0, 0, 0,
                    Collections.emptyList(), Collections.emptyList(),
                    "Language file not found: " + filePath.toAbsolutePath());
            }
        }

        final Map<String, String> targetFinal = target;
        Set<String> targetKeys = new LinkedHashSet<>();
        for (String k : targetFinal.keySet()) {
            if (!k.startsWith("_")) targetKeys.add(k);
        }

        List<String> missingFromTarget = new ArrayList<>();
        for (String k : baseKeys) {
            if (!targetKeys.contains(k)) missingFromTarget.add(k);
        }

        List<String> extraInTarget = new ArrayList<>();
        for (String k : targetKeys) {
            if (!baseKeys.contains(k)) extraInTarget.add(k);
        }

        int total = baseKeys.size();
        int present = total - missingFromTarget.size();
        int coverage = total > 0 ? (int) ((present * 100.0) / total) : 100;

        return new ValidationReport(languageCode, total, present, coverage,
            missingFromTarget, extraInTarget, null);
    }

    /**
     * Regenerate a language file from the JAR, merging existing user translations.
     * The current file is backed up to &lt;lang&gt;.json.bak before overwriting.
     * @return the number of new keys added from the JAR
     */
    public int regenerate(String languageCode) throws IOException {
        String fileName = languageCode + ".json";
        Path target = customLangDir.resolve(fileName);
        Path backup = customLangDir.resolve(languageCode + ".json.bak");

        // Read existing disk file (may have user edits)
        Map<String, String> existing = new LinkedHashMap<>();
        if (Files.exists(target)) {
            try (Reader reader = Files.newBufferedReader(target, StandardCharsets.UTF_8)) {
                Type type = new TypeToken<Map<String, String>>(){}.getType();
                Map<String, String> loaded = gson.fromJson(reader, type);
                if (loaded != null) existing.putAll(loaded);
            }
            // Backup current file
            Files.copy(target, backup, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            LOGGER.info("Backed up {} to {}", target, backup);
        }

        // Load fresh JAR version
        Map<String, String> jarVersion;
        try (InputStream in = findLangResource(fileName)) {
            if (in == null) throw new IOException("JAR resource not found for language: " + languageCode);
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                Type type = new TypeToken<Map<String, String>>(){}.getType();
                jarVersion = gson.fromJson(reader, type);
            }
        }
        if (jarVersion == null) throw new IOException("Empty/invalid JAR language file for: " + languageCode);

        // Merge: JAR keys + existing user values (user wins on conflict)
        Map<String, String> merged = new LinkedHashMap<>(jarVersion);
        int newKeys = 0;
        for (Map.Entry<String, String> e : jarVersion.entrySet()) {
            if (!existing.containsKey(e.getKey())) {
                newKeys++;
            } else {
                merged.put(e.getKey(), existing.get(e.getKey())); // keep user value
            }
        }

        Files.createDirectories(target.getParent());
        try (Writer writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
            gson.toJson(merged, writer);
        }
        LOGGER.info("Regenerated {}: {} total keys, {} new from JAR, backup at {}", fileName, merged.size(), newKeys, backup);

        // Reload translations for this language
        loadCustomLanguageFile(target);

        return newKeys;
    }

    /**
     * Validation report from comparing a language file against the English base.
     */
    public static class ValidationReport {
        private final String languageCode;
        private final int totalKeys;
        private final int presentKeys;
        private final int coveragePercent;
        private final List<String> missingKeys;
        private final List<String> extraKeys;
        private final String errorMessage;

        public ValidationReport(String languageCode, int totalKeys, int presentKeys, int coveragePercent,
                                List<String> missingKeys, List<String> extraKeys, String errorMessage) {
            this.languageCode = languageCode;
            this.totalKeys = totalKeys;
            this.presentKeys = presentKeys;
            this.coveragePercent = coveragePercent;
            this.missingKeys = missingKeys;
            this.extraKeys = extraKeys;
            this.errorMessage = errorMessage;
        }

        @SuppressWarnings("unused") // public API
        public String getLanguageCode() { return languageCode; }
        public int getTotalKeys() { return totalKeys; }
        public int getPresentKeys() { return presentKeys; }
        public int getCoveragePercent() { return coveragePercent; }
        public List<String> getMissingKeys() { return missingKeys; }
        @SuppressWarnings("unused") // public API
        public List<String> getExtraKeys() { return extraKeys; }
        public boolean hasError() { return errorMessage != null; }
        public String getErrorMessage() { return errorMessage; }
        public int getMissingCount() { return missingKeys.size(); }
        public int getExtraCount() { return extraKeys.size(); }
    }

    private InputStream findLangResource(String filename) {
        // Try class.getResourceAsStream with and without leading slash
        String[] paths = {"/data/lang/" + filename, "data/lang/" + filename};
        for (String path : paths) {
            InputStream in = getClass().getResourceAsStream(path);
            if (in != null) {
                LOGGER.debug("Found language resource at: {}", path);
                return in;
            }
        }
        // Try classloader as fallback
        for (String path : paths) {
            String normalised = path.startsWith("/") ? path.substring(1) : path;
            InputStream in = getClass().getClassLoader().getResourceAsStream(normalised);
            if (in != null) {
                LOGGER.debug("Found language resource via classloader at: {}", normalised);
                return in;
            }
        }
        // Truly not found — log once at WARN level (not ERROR, and no expensive JAR listing)
        LOGGER.warn("Language resource not found in JAR for: {} (tried paths: /data/lang/{}, data/lang/{})",
            filename, filename, filename);
        return null;
    }

    /**
     * Resolve a path relative to the mod's data directory (server root/neoessentials/)
     */
    private static Path resolveModDataPath(String... parts) {
        // Always resolve to the server root directory, not the working directory
        Path serverRoot = FMLPaths.GAMEDIR.get();
        Path neoEssentialsDir = serverRoot.resolve("neoessentials");
        for (String part : parts) {
            neoEssentialsDir = neoEssentialsDir.resolve(part);
        }
        // Log the resolved path for debugging
        LOGGER.info("[LANG] Resolved mod data path: {}", neoEssentialsDir.toAbsolutePath());
        return neoEssentialsDir;
    }
}
