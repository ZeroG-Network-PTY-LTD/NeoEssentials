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
import net.minecraft.ChatFormatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Centralized message handling system for NeoEssentials
 * Handles localization, formatting, and fallbacks consistently across all commands
 */
public class MessageUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageUtil.class);
    private static final Map<String, String> translations = new HashMap<>();
    private static boolean loaded = false;
    private static boolean debugMode = true; // Enable for debugging

    /**
     * Load translations from server directory or JAR fallback
     */
    private static void loadTranslations() {
        if (loaded) return;
        loaded = true;
        
        LOGGER.info("Loading NeoEssentials translations...");
        
        // Try to load from server directory first
        File serverLangFile = new File("neoessentials/lang/en_us.json");
        if (serverLangFile.exists()) {
            try (FileReader reader = new FileReader(serverLangFile)) {
                Gson gson = new Gson();
                Type type = new TypeToken<Map<String, String>>(){}.getType();
                Map<String, String> map = gson.fromJson(reader, type);
                if (map != null) {
                    translations.putAll(map);
                    LOGGER.info("Loaded {} translations from server directory", translations.size());
                    return;
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to load translations from server directory: {}", e.getMessage());
            }
        }
        
        // Fallback to JAR resource
        try (InputStream in = MessageUtil.class.getResourceAsStream("/data/lang/en_us.json")) {
            if (in != null) {
                try (java.util.Scanner scanner = new java.util.Scanner(in, "UTF-8").useDelimiter("\\A")) {
                    String json = scanner.hasNext() ? scanner.next() : "";
                    Gson gson = new Gson();
                    Type type = new TypeToken<Map<String, String>>(){}.getType();
                    Map<String, String> map = gson.fromJson(json, type);
                    if (map != null) {
                        translations.putAll(map);
                        LOGGER.info("Loaded {} translations from JAR resource", translations.size());
                        return;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load translations from JAR: {}", e.getMessage(), e);
        }
        
        LOGGER.warn("No translations loaded! Messages will show as keys.");
    }

    /**
     * Get a localized string with optional arguments
     */
    public static String localize(String key, Object... args) {
        loadTranslations();
        String template = translations.getOrDefault(key, key);
        
        if (debugMode && !translations.containsKey(key)) {
            LOGGER.warn("Missing translation key: {}", key);
        }
        
        try {
            return MessageFormat.format(template.replace("%s", "{0}"), args);
        } catch (Exception e) {
            LOGGER.warn("Failed to format message '{}' with args: {}", template, java.util.Arrays.toString(args));
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
        return Component.literal(localize(key, args)).withStyle(ChatFormatting.GREEN);
    }

    /**
     * Create an error message component (red text)
     */
    public static Component error(String key, Object... args) {
        return Component.literal(localize(key, args)).withStyle(ChatFormatting.RED);
    }

    /**
     * Create a warning message component (yellow text)
     */
    public static Component warning(String key, Object... args) {
        return Component.literal(localize(key, args)).withStyle(ChatFormatting.YELLOW);
    }

    /**
     * Create an info message component (aqua text)
     */
    public static Component info(String key, Object... args) {
        return Component.literal(localize(key, args)).withStyle(ChatFormatting.AQUA);
    }

    /**
     * Get debug information about loaded translations
     */
    public static String getDebugInfo() {
        loadTranslations();
        return String.format("Translations loaded: %d, Debug mode: %s", translations.size(), debugMode);
    }

    /**
     * Check if a translation key exists
     */
    public static boolean hasTranslation(String key) {
        loadTranslations();
        return translations.containsKey(key);
    }
}