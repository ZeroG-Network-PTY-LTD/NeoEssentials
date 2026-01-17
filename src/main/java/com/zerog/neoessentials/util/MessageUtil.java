package com.zerog.neoessentials.util;

import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.text.MessageFormat;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
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
        LOGGER.info("[NeoEssentials] Debug mode set to: {} (from config)", debugMode);
    }
    
    // Language version tracking - increment when translations change
    private static final String LANG_VERSION_KEY = "_langVersion";
    private static final int CURRENT_LANG_VERSION = 8;

    /**
     * Load translations from server directory, updating from JAR if needed
     */
    private static void loadTranslations() {
        if (loaded) return;
        loaded = true;
        
        LOGGER.info("=== LOADING NEOESSENTIALS TRANSLATIONS ===");
        
        File serverLangFile = ResourceUtil.getLanguageFile("en_us");
        LOGGER.info("Server language file path: {}", serverLangFile.getAbsolutePath());
        
        // Load JAR translations first to compare/update
        Map<String, String> jarTranslations = loadJarTranslations();
        if (jarTranslations == null || jarTranslations.isEmpty()) {
            LOGGER.error("Failed to load JAR translations - cannot proceed");
            return;
        }
        
        LOGGER.info("JAR contains {} translation keys", jarTranslations.size());
        
        // Check if server file needs updating
        boolean needsUpdate = false;
        if (!serverLangFile.exists()) {
            LOGGER.info("Server language file doesn't exist, will create from JAR");
            needsUpdate = true;
        } else {
            // Load existing server file and compare
            Map<String, String> serverTranslations = loadServerTranslations(serverLangFile);
            if (serverTranslations == null) {
                LOGGER.info("Failed to load server language file, will recreate from JAR");
                needsUpdate = true;
            } else {
                // Check version first
                int serverVersion = getLanguageVersion(serverTranslations);
                int jarVersion = getLanguageVersion(jarTranslations);
                
                if (serverVersion < jarVersion) {
                    LOGGER.info("Language version outdated: server={}, JAR={} - updating", serverVersion, jarVersion);
                    needsUpdate = true;
                } else if (serverTranslations.size() != jarTranslations.size()) {
                    LOGGER.info("Key count mismatch: server={}, JAR={} - updating", serverTranslations.size(), jarTranslations.size());
                    needsUpdate = true;
                } else {
                    LOGGER.info("Server language file is up to date (version={}, {} keys)", serverVersion, serverTranslations.size());
                }
            }
        }
        
        // Update server file if needed
        if (needsUpdate) {
            updateServerLanguageFile(serverLangFile, jarTranslations);
        }
        
        // Load from server file (now guaranteed to be up to date)
        Map<String, String> finalTranslations = loadServerTranslations(serverLangFile);
        if (finalTranslations != null) {
            translations.putAll(finalTranslations);
            LOGGER.info("Successfully loaded {} translations from server directory", translations.size());
        } else {
            // Final fallback - use JAR translations directly
            translations.putAll(jarTranslations);
            LOGGER.warn("Using JAR translations directly ({} keys)", translations.size());
        }
        
        LOGGER.info("=== TRANSLATION LOADING COMPLETE ===");
        LOGGER.info("Total translations loaded: {}", translations.size());
        LOGGER.info("Sample translation keys: {}", translations.keySet().stream().limit(5).toArray());
        LOGGER.info("Home set message key exists: {}", translations.containsKey("commands.neoessentials.teleport.home.set"));
        if (translations.containsKey("commands.neoessentials.teleport.home.set")) {
            LOGGER.info("Home set message template: '{}'", translations.get("commands.neoessentials.teleport.home.set"));
        }
    }
    
    /**
     * Load translations from JAR resource
     */
    private static Map<String, String> loadJarTranslations() {
        try (InputStream in = ResourceUtil.getJarLanguageResource("en_us")) {
            if (in != null) {
                try (java.util.Scanner scanner = new java.util.Scanner(in, "UTF-8").useDelimiter("\\A")) {
                    String json = scanner.hasNext() ? scanner.next() : "";
                    Gson gson = new Gson();
                    Type type = new TypeToken<Map<String, String>>(){}.getType();
                    return gson.fromJson(json, type);
                }
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
            // Ensure parent directory exists
            serverFile.getParentFile().mkdirs();
            
            // Create a copy with version key added
            Map<String, String> translationsWithVersion = new HashMap<>(jarTranslations);
            translationsWithVersion.put(LANG_VERSION_KEY, String.valueOf(CURRENT_LANG_VERSION));
            
            // Write translations to server file
            try (java.io.FileWriter writer = new java.io.FileWriter(serverFile)) {
                Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
                gson.toJson(translationsWithVersion, writer);
                LOGGER.info("Updated server language file with {} keys (version {})", translationsWithVersion.size(), CURRENT_LANG_VERSION);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to update server language file: {}", e.getMessage(), e);
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
}