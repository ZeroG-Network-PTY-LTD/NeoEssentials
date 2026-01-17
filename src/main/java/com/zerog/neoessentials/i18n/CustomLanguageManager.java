package com.zerog.neoessentials.i18n;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.zerog.neoessentials.util.MessageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enhanced language file manager that supports custom user-created language files
 * and automatic template generation for easy translation.
 */
public class CustomLanguageManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomLanguageManager.class);
    private static CustomLanguageManager INSTANCE;

    private final Path customLangDir;
    private final Path templatesDir;
    private final Map<String, Map<String, String>> customTranslations = new ConcurrentHashMap<>();
    private final Map<String, LanguageFileInfo> languageFiles = new ConcurrentHashMap<>();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    // Track missing translation keys for template generation
    private final Set<String> missingKeys = ConcurrentHashMap.newKeySet();

    private CustomLanguageManager() {
        this.customLangDir = Paths.get("neoessentials", "languages", "custom");
        this.templatesDir = Paths.get("neoessentials", "languages", "templates");
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
            // Create directories
            Files.createDirectories(customLangDir);
            Files.createDirectories(templatesDir);

            // Scan for custom language files
            scanCustomLanguageFiles();

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

            Files.list(customLangDir)
                .filter(path -> path.toString().endsWith(".json"))
                .forEach(this::loadCustomLanguageFile);

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
     * Get translation from custom language file, with fallback to MessageUtil
     */
    public String getTranslation(String key, String languageCode) {
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
     * Reload all custom language files
     */
    public void reload() {
        customTranslations.clear();
        languageFiles.clear();
        scanCustomLanguageFiles();
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
        public Path getFilePath() { return filePath; }
    }
}

