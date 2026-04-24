package com.zerog.neoessentials.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.text.MessageFormat;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Centralized message handling system for NeoEssentials
 * Handles localization, formatting, and fallbacks consistently across all commands
 */
public class MessageUtil {
    /**
     * Returns whether debug mode is enabled (for use throughout the mod)
     */
    public static boolean isDebugMode() {
        return debugMode;
    }
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageUtil.class);
    private static final Map<String, String> translations = new HashMap<>();
    private static boolean loaded = false;
    private static boolean debugMode = false; // Default to false, will sync with config
    /**
     * Sync debugMode with config value (modules.debugMode)
     */
    public static void syncDebugModeFromConfig() {
        debugMode = com.zerog.neoessentials.config.ConfigManager.isDebugModeEnabled();
        LOGGER.debug("Debug mode set to: {} (from config)", debugMode);
    }
    
    // Language version tracking - increment when translations change
    private static final String LANG_VERSION_KEY = "_langVersion";
    private static final int CURRENT_LANG_VERSION = 12;

    /**
     * Load translations from server directory, updating from JAR if needed.
     * If the deployed file exists but is at an older version, any missing keys
     * are merged in from the JAR without overwriting user edits.
     */
    private static void loadTranslations() {
        if (loaded) return;
        loaded = true;

        LOGGER.debug("=== LOADING NEOESSENTIALS TRANSLATIONS ===");

        File customLangDir = getNeoEssentialsLangCustomDir();
        if (!customLangDir.exists()) {
            boolean dirCreated = customLangDir.mkdirs();
            if (!dirCreated) {
                LOGGER.error("Failed to create custom language directory: {}", customLangDir.getAbsolutePath());
            } else {
                LOGGER.debug("Created custom language directory: {}", customLangDir.getAbsolutePath());
            }
        }
        File serverLangFile = new File(customLangDir, "en_us.json");
        LOGGER.debug("Server language file path: {}", serverLangFile.getAbsolutePath());

        Map<String, String> finalTranslations = null;
        if (serverLangFile.exists() && serverLangFile.length() > 0) {
            finalTranslations = loadServerTranslations(serverLangFile);
            if (finalTranslations != null) {
                // Version check — merge any new JAR keys without overwriting user edits
                int deployedVersion = 0;
                try {
                    deployedVersion = Integer.parseInt(
                        finalTranslations.getOrDefault(LANG_VERSION_KEY, "0"));
                } catch (NumberFormatException ignored) {}

                if (deployedVersion < CURRENT_LANG_VERSION) {
                    LOGGER.info("NeoEssentials: lang file is v{} (current v{}) — merging new keys...",
                        deployedVersion, CURRENT_LANG_VERSION);
                    Map<String, String> jarTranslations = loadJarTranslations();
                    if (jarTranslations != null) {
                        int added = 0;
                        for (Map.Entry<String, String> e : jarTranslations.entrySet()) {
                            if (!finalTranslations.containsKey(e.getKey())) {
                                finalTranslations.put(e.getKey(), e.getValue());
                                added++;
                            }
                        }
                        finalTranslations.put(LANG_VERSION_KEY, String.valueOf(CURRENT_LANG_VERSION));
                        try (java.io.FileWriter fw = new java.io.FileWriter(serverLangFile)) {
                            new com.google.gson.GsonBuilder().setPrettyPrinting()
                                .create().toJson(finalTranslations, fw);
                        } catch (Exception ex) {
                            LOGGER.warn("NeoEssentials: could not save merged lang file: {}", ex.getMessage());
                        }
                        LOGGER.info("NeoEssentials: merged {} new translation keys (total: {})",
                            added, finalTranslations.size());
                    }
                }
                translations.putAll(finalTranslations);
                LOGGER.info("NeoEssentials: loaded {} translations", translations.size());
            } else {
                LOGGER.error("Failed to load custom language file, will attempt to update from JAR");
            }
        }
        // If file missing or unreadable, deploy from JAR
        if (translations.isEmpty()) {
            Map<String, String> jarTranslations = loadJarTranslations();
            if (jarTranslations == null || jarTranslations.isEmpty()) {
                LOGGER.error("Failed to load JAR translations - cannot proceed");
                try (InputStream testIn = ResourceUtil.getJarLangResource("en_us.json")) {
                    if (testIn == null) {
                        LOGGER.error("JAR resource 'en_us.json' is missing or not found in /data/lang/");
                    } else {
                        LOGGER.debug("JAR resource 'en_us.json' is present but failed to load as translations.");
                    }
                } catch (Exception e) {
                    LOGGER.error("Exception when testing JAR resource existence: {}", e.getMessage(), e);
                }
                return;
            }
            LOGGER.debug("JAR contains {} translation keys", jarTranslations.size());
            try {
                updateServerLanguageFile(serverLangFile, jarTranslations);
                if (serverLangFile.exists()) {
                    LOGGER.debug("Language file successfully created: {}", serverLangFile.getAbsolutePath());
                    finalTranslations = loadServerTranslations(serverLangFile);
                    if (finalTranslations != null) {
                        translations.putAll(finalTranslations);
                        LOGGER.info("NeoEssentials: loaded {} translations (updated from JAR)", translations.size());
                    } else {
                        LOGGER.error("Failed to load custom language file after update, using JAR translations directly");
                        translations.putAll(jarTranslations);
                    }
                } else {
                    LOGGER.error("Language file was not created: {}", serverLangFile.getAbsolutePath());
                    translations.putAll(jarTranslations);
                }
            } catch (Exception e) {
                LOGGER.error("Exception during language file update: {}", e.getMessage(), e);
                translations.putAll(jarTranslations);
            }
        }
        LOGGER.debug("Translation loading complete. Total keys: {}", translations.size());
        if (serverLangFile.length() == 0) {
            LOGGER.error("Server language file is empty after creation! Check file permissions and JAR resource.");
        }
    }
    
    /**
     * Load translations from JAR resource
     */
    private static Map<String, String> loadJarTranslations() {
        try (InputStream in = ResourceUtil.getJarLangResource("en_us.json")) {
            if (in != null) {
                try (java.util.Scanner scanner = new java.util.Scanner(in, java.nio.charset.StandardCharsets.UTF_8).useDelimiter("\\A")) {
                    String json = scanner.hasNext() ? scanner.next() : "";
                    Gson gson = new Gson();
                    Type type = new TypeToken<Map<String, String>>(){}.getType();
                    return gson.fromJson(json, type);
                }
            } else {
                LOGGER.error("JAR language resource 'en_us.json' not found.");
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load JAR translations: {}", e.getMessage(), e);
        }
        return null;
    }
    
    /**
     * Load translations from server file
     */
    private static Map<String, String> loadServerTranslations(File serverFile) {
        if (!serverFile.exists()) return null;
        
        try (FileReader reader = new FileReader(serverFile)) {
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, String>>(){}.getType();
            return gson.fromJson(reader, type);
        } catch (Exception e) {
            LOGGER.warn("Failed to load server translations from {}: {}", serverFile.getAbsolutePath(), e.getMessage());
            return null;
        }
    }
    
    /**
     * Update server language file with JAR translations
     */
    private static void updateServerLanguageFile(File serverFile, Map<String, String> jarTranslations) {
        try {
            File parentDir = serverFile.getParentFile();
            if (!parentDir.exists()) {
                boolean dirCreated = parentDir.mkdirs();
                if (!dirCreated) {
                    LOGGER.error("Failed to create language directory: {}", parentDir.getAbsolutePath());
                } else {
                    LOGGER.debug("Created language directory: {}", parentDir.getAbsolutePath());
                }
            }
            Map<String, String> translationsWithVersion = new HashMap<>(jarTranslations);
            translationsWithVersion.put(LANG_VERSION_KEY, String.valueOf(CURRENT_LANG_VERSION));
            try (java.io.FileWriter writer = new java.io.FileWriter(serverFile)) {
                Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
                gson.toJson(translationsWithVersion, writer);
                LOGGER.debug("Updated server language file with {} keys (version {})", translationsWithVersion.size(), CURRENT_LANG_VERSION);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to update server language file: {} ({}): {}", serverFile.getAbsolutePath(), serverFile.getParentFile(), e.getMessage(), e);
        }
    }

    /**
     * Get a localized string with optional arguments
     */
    public static String localize(String key, Object... args) {
        loadTranslations();
        String template = translations.getOrDefault(key, key);
        
        if (debugMode && !translations.containsKey(key)) {
            LOGGER.warn("Missing translation key: {} (total keys loaded: {})", key, translations.size());
        }
        
        try {
            String result = MessageFormat.format(template.replace("%s", "{0}"), args);
            if (debugMode) {
                LOGGER.info("MessageFormat success - Key: {}, Template: '{}', Args: {}, Result: '{}'", 
                    key, template, java.util.Arrays.toString(args), result);
            }
            return result;
        } catch (Exception e) {
            LOGGER.error("Failed to format message - Key: {}, Template: '{}', Args: {}, Error: {}", 
                key, template, java.util.Arrays.toString(args), e.getMessage(), e);
            return template;
        }
    }

    /**
     * Create a Component from a localized message (standard approach)
     */
    public static Component component(String key, Object... args) {
        String message = localize(key, args);
        if (debugMode) {
            LOGGER.debug("Component created - Key: {}, Message: '{}'", key, message);
        }
        return Component.literal(message);
    }

    /**
     * Create a success message component (green text)
     */
    public static Component success(String key, Object... args) {
        return Component.literal(localize(key, args)).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x00FF00)));
    }

    /**
     * Create an error message component (red text)
     */
    public static Component error(String key, Object... args) {
        return Component.literal(localize(key, args)).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFF0000)));
    }

    /**
     * Create a warning message component (yellow text)
     */
    public static Component warning(String key, Object... args) {
        return Component.literal(localize(key, args)).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFFF00)));
    }

    /**
     * Create an info message component (aqua text)
     */
    public static Component info(String key, Object... args) {
        return Component.literal(localize(key, args)).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x00FFFF)));
    }

    /**
     * Get debug information about loaded translations
     */
    public static String getDebugInfo() {
    loadTranslations();
    syncDebugModeFromConfig();
    return String.format("Translations loaded: %d, Debug mode: %s", translations.size(), debugMode);
    }
    
    /**
     * Debug method to check if a specific key exists
     */
    public static void debugKey(String key) {
        loadTranslations();
        LOGGER.info("Debug key '{}': exists={}, value='{}'", key, translations.containsKey(key), translations.get(key));
        LOGGER.info("Total translations loaded: {}, Sample keys: {}", translations.size(), 
            translations.keySet().stream().limit(3).toArray());
    }

    /**
     * Check if a translation key exists
     */
    public static boolean hasTranslation(String key) {
        loadTranslations();
        return translations.containsKey(key);
    }
    
    /**
     * Force reload translations (for debugging/testing)
     */
    public static void reloadTranslations() {
        loaded = false;
        translations.clear();
        loadTranslations();
        LOGGER.info("Forced translation reload completed, {} keys loaded", translations.size());
    }
    
    /**
     * Force update/merge the language file if config version is updated.
     * Ensures all keys from the JAR are present in the server language file.
     */
    public static void ensureLanguageFileUpToDate() {
        File serverLangFile = ResourceUtil.getLanguageFile("en_us");
        Map<String, String> jarTranslations = loadJarTranslations();
        Map<String, String> serverTranslations = loadServerTranslations(serverLangFile);
        boolean needsUpdate = false;
        if (jarTranslations == null) {
            LOGGER.error("JAR translations are null, cannot update language file.");
            return;
        }
        if (serverTranslations == null) {
            needsUpdate = true;
        } else {
            // Check for missing keys
            for (String key : jarTranslations.keySet()) {
                if (!serverTranslations.containsKey(key)) {
                    needsUpdate = true;
                    break;
                }
            }
        }
        if (needsUpdate) {
            updateServerLanguageFile(serverLangFile, jarTranslations);
            translations.clear();
            loaded = false;
            loadTranslations();
            LOGGER.info("Language file updated/merged due to config version update.");
        }
    }

    /**
     * Utility to get the NeoEssentials config root directory (handles IDE/run/production cases)
     */
    private static File getNeoEssentialsConfigRoot() {
        // Try to use the same logic as config file location
        String configDir = System.getProperty("neoessentials.config.dir");
        if (configDir != null && !configDir.isEmpty()) {
            return new File(configDir);
        }
        // Fallback: use user.dir (should be project root or server root)
        return new File(System.getProperty("user.dir"));
    }

    /**
     * Ensures the custom language file exists and is loaded from the correct directory.
     * If missing, generates it from the JAR resource and logs all steps.
     */
    public static void ensureCustomLanguageFile() {
        File configRoot = getNeoEssentialsConfigRoot();
        File langDir = new File(configRoot, "neoessentials/languages/custom");
        File langFile = new File(langDir, "en_us.json");
        logInfo("[Lang] Working directory: " + System.getProperty("user.dir"));
        logInfo("[Lang] Resolved language file path: " + langFile.getAbsolutePath());
        if (!langFile.exists() || langFile.length() == 0) {
            logInfo("Custom language file not found or empty: " + langFile.getAbsolutePath());
            try (InputStream in = ResourceUtil.getJarLangResource("en_us.json")) {
                if (in == null) {
                    logError("Default language resource not found in JAR: data/lang/en_us.json");
                    return;
                }
                Files.createDirectories(langFile.getParentFile().toPath());
                Files.copy(in, langFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                logInfo("Generated custom language file from JAR resource: " + langFile.getAbsolutePath());
            } catch (Exception e) {
                logError("Failed to generate custom language file: " + e.getMessage());
            }
        } else {
            logInfo("Custom language file exists: " + langFile.getAbsolutePath());
        }
    }

    private static void logInfo(String msg) {
        System.out.println("[NeoEssentials-Lang] INFO: " + msg);
    }
    private static void logError(String msg) {
        System.err.println("[NeoEssentials-Lang] ERROR: " + msg);
    }

    // === Enhanced Chat Components ===
    
    /**
     * Create a clickable command component with enhanced formatting
     */
    public static Component clickableCommand(String text, String command, String hoverText) {
        return ChatComponentUtil.createClickableCommand(text, command, hoverText);
    }
    
    /**
     * Create a clickable suggestion component
     */
    public static Component clickableSuggestion(String text, String command, String hoverText) {
        return ChatComponentUtil.createClickableSuggestion(text, command, hoverText);
    }
    
    /**
     * Create formatted balance display with interaction
     */
    public static Component balanceComponent(String playerName, double balance, String currency) {
        return ChatComponentUtil.createBalanceComponent(playerName, balance, currency);
    }
    
    /**
     * Create formatted player name with interaction
     */
    public static Component playerComponent(String playerName) {
        return ChatComponentUtil.createPlayerComponent(playerName);
    }
    
    /**
     * Create formatted permission with copy functionality
     */
    public static Component permissionComponent(String permission) {
        return ChatComponentUtil.createPermissionComponent(permission);
    }
    
    /**
     * Parse color codes in text and return colored component
     */
    public static Component coloredText(String text) {
        if (!com.zerog.neoessentials.config.ConfigManager.isColorCodesEnabled()) {
            // Strip all color codes, including hex (#RRGGBB)
            if (text == null) return Component.empty();
            // Remove § and & color codes
            String noCodes = text.replaceAll("[§&][0-9a-fk-or]", "");
            // Remove hex color codes (#RRGGBB)
            noCodes = noCodes.replaceAll("#[0-9a-fA-F]{6}", "");
            return Component.literal(noCodes);
        }
        return ChatComponentUtil.parseColorCodes(text);
    }
    
    /**
     * Create a separator line
     */
    public static Component separator(int length, char character, net.minecraft.ChatFormatting color) {
        return ChatComponentUtil.createSeparator(length, character, color);
    }
    
    /**
     * Create a progress bar
     */
    public static Component progressBar(double current, double max, int width) {
        return ChatComponentUtil.createProgressBar(current, max, width);
    }
    
    /**
     * Get the version of a language file from its translations map
     */
    private static int getLanguageVersion(Map<String, String> translations) {
        if (translations == null || !translations.containsKey(LANG_VERSION_KEY)) {
            return 0; // Default version for files without version key
        }
        try {
            return Integer.parseInt(translations.get(LANG_VERSION_KEY));
        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid language version format, defaulting to 0");
            return 0;
        }
    }

    /**
     * Create a clickable confirmation message for home actions
     */
    public static MutableComponent homeConfirmComponent(String homeName, String action, String commandConfirm, String commandDeny) {
        MutableComponent confirm = Component.literal("[Confirm]")
            .withStyle(style -> style.withColor(TextColor.fromRgb(0x4CAF50)))
            .withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, commandConfirm)))
            .withStyle(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to confirm " + action + " of home '" + homeName + "'"))));
        MutableComponent deny = Component.literal("[Deny]")
            .withStyle(style -> style.withColor(TextColor.fromRgb(0xF44336)))
            .withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, commandDeny)))
            .withStyle(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to cancel " + action + " of home '" + homeName + "'"))));
        return Component.literal("")
            .append(Component.literal("Are you sure you want to "+action+" home '").withStyle(style -> style.withColor(TextColor.fromRgb(0xFFD600))))
            .append(Component.literal(homeName).withStyle(style -> style.withColor(TextColor.fromRgb(0xFF9800))))
            .append(Component.literal("'? "))
            .append(confirm)
            .append(Component.literal(" "))
            .append(deny);
    }

    /**
     * Utility to get the NeoEssentials custom language directory (matches CustomLanguageManager)
     *
     * This version also removes the legacy 'lang' directory if it exists in the server root.
     */
    private static File getNeoEssentialsLangCustomDir() {
        // Use FMLPaths.GAMEDIR if available, else fallback to user.dir
        File langDir;
        try {
            // Try to use FMLPaths if available (Forge/NeoForge)
            Class<?> fmlPathsClass = Class.forName("net.neoforged.fml.loading.FMLPaths");
            java.lang.reflect.Method gamedirMethod = fmlPathsClass.getMethod("GAMEDIR");
            Object gamedirPath = gamedirMethod.invoke(null);
            java.nio.file.Path serverRoot = (java.nio.file.Path) gamedirPath.getClass().getMethod("get").invoke(gamedirPath);
            langDir = serverRoot.resolve("neoessentials").resolve("languages").resolve("custom").toFile();
            // Remove legacy 'lang' directory if it exists
            File legacyLangDir = serverRoot.resolve("neoessentials").resolve("lang").toFile();
            if (legacyLangDir.exists() && legacyLangDir.isDirectory()) {
                deleteDirectoryRecursively(legacyLangDir);
                LOGGER.info("Removed legacy language directory: {}", legacyLangDir.getAbsolutePath());
            }
        } catch (Exception e) {
            // Fallback: use user.dir
            File fallbackRoot = new File(System.getProperty("user.dir"), "neoessentials");
            langDir = new File(fallbackRoot, "languages/custom");
            // Remove legacy 'lang' directory if it exists
            File legacyLangDir = new File(fallbackRoot, "lang");
            if (legacyLangDir.exists() && legacyLangDir.isDirectory()) {
                deleteDirectoryRecursively(legacyLangDir);
                LOGGER.info("Removed legacy language directory: {}", legacyLangDir.getAbsolutePath());
            }
        }
        return langDir;
    }

    /**
     * Recursively delete a directory and all its contents.
     */
    private static void deleteDirectoryRecursively(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectoryRecursively(file);
                }
            }
        }
        dir.delete();
    }

    /**
     * Loads a language file from the custom language directory in the NeoEssentials data folder.
     *
     * @param languageCode The language code (e.g., "en_us").
     * @return The loaded language map, or null if not found.
     */
    public static Map<String, String> loadCustomLanguageFile(String languageCode) {
        // Always use the NeoEssentials data directory for custom languages
        File customLangFile = new File("neoessentials/languages/custom/" + languageCode + ".json");
        if (!customLangFile.exists()) {
            return null;
        }
        try (FileReader reader = new FileReader(customLangFile)) {
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, String>>() {}.getType();
            return gson.fromJson(reader, type);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Loads all available custom language files from the NeoEssentials data directory.
     */
    public static Map<String, Map<String, String>> loadAllCustomLanguages() {
        Map<String, Map<String, String>> languages = new HashMap<>();
        File langDir = new File("neoessentials/languages/custom");
        if (langDir.exists() && langDir.isDirectory()) {
            File[] files = langDir.listFiles((dir, name) -> name.endsWith(".json"));
            if (files != null) {
                for (File file : files) {
                    String langCode = file.getName().replace(".json", "");
                    Map<String, String> langMap = loadCustomLanguageFile(langCode);
                    if (langMap != null) {
                        languages.put(langCode, langMap);
                    }
                }
            }
        }
        return languages;
    }
}
