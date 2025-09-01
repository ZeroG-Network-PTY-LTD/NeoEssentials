package com.zerog.neoessentials.localization;

import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
// ...existing code...
// ...existing code...

/**
 * Comprehensive Language Manager for NeoEssentials
 * Provides complete localization support with dynamic language switching,
 * placeholder replacement, and player-specific locale detection.
 * 
 * Features:
 * - Multi-language message support
 * - Player-specific locale detection
 * - Placeholder replacement system
 * - Hot-reloading of language files
 * - Fallback language support
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class LanguageManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(LanguageManager.class);
    private static LanguageManager instance;
    
    private final Map<String, Properties> languageFiles = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerLocales = new ConcurrentHashMap<>();
    private final Path languageDirectory;
    private String defaultLanguage = "en_US";
    
    private LanguageManager(Path configPath) {
        this.languageDirectory = configPath.resolve("languages");
        initializeLanguageSystem();
    }
    
    public static synchronized LanguageManager getInstance(Path configPath) {
        if (instance == null) {
            instance = new LanguageManager(configPath);
        }
        return instance;
    }
    
    public static LanguageManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("LanguageManager not initialized! Call getInstance(Path) first.");
        }
        return instance;
    }
    
    /**
     * Initialize the language system
     */
    public void initialize() {
        initializeLanguageSystem();
    }
    
    /**
     * Initialize the language system
     */
    private void initializeLanguageSystem() {
        try {
            // Create language directory if it doesn't exist (for custom overrides only)
            if (!Files.exists(languageDirectory)) {
                Files.createDirectories(languageDirectory);
                LOGGER.info("Created language directory for custom overrides: {}", languageDirectory);
            }
            
            // Load all language files (prioritize resources, then config overrides)
            loadLanguageFiles();
            
            LOGGER.info("Language system initialized with {} languages", languageFiles.size());
            
        } catch (IOException e) {
            LOGGER.error("Failed to initialize language system", e);
        }
    }
    
    /**
     * Load all language files from both resources and config directories
     * Priority: Resources (mod defaults) first, then config overrides
     */
    private void loadLanguageFiles() {
        try {
            // First load from resources directory (mod defaults)
            loadResourceLanguageFiles();
            
            // Then load from config directory (custom overrides)
            if (Files.exists(languageDirectory)) {
                Files.list(languageDirectory)
                    .filter(path -> path.toString().endsWith(".properties"))
                    .forEach(this::loadLanguageFile);
            }
                
        } catch (IOException e) {
            LOGGER.error("Failed to load language files", e);
        }
    }
    
    /**
     * Load language files from resources directory using class loader
     */
    private void loadResourceLanguageFiles() {
        try {
            // Use class loader to access resources within the JAR
            ClassLoader classLoader = getClass().getClassLoader();
            
            // Try to load en_us.json from resources
            try (InputStream input = classLoader.getResourceAsStream("assets/neoessentials/lang/en_us.json")) {
                if (input != null) {
                    Properties properties = loadJsonLanguageFile(input);
                    if (properties != null && !properties.isEmpty()) {
                        languageFiles.put("en_US", properties);
                        LOGGER.info("Loaded en_US language from resources (converted from JSON)");
                    }
                } else {
                    LOGGER.warn("Could not find en_us.json in resources");
                }
            }
            
            // Create basic fallback if no resources found
            if (languageFiles.isEmpty()) {
                LOGGER.warn("No language files found in resources, creating basic fallback");
                Properties fallback = createBasicFallbackLanguage();
                languageFiles.put("en_US", fallback);
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to load resource language files", e);
            // Create emergency fallback
            Properties emergency = createBasicFallbackLanguage();
            languageFiles.put("en_US", emergency);
        }
    }
    
    /**
     * Load JSON language file and convert to properties
     */
    private Properties loadJsonLanguageFile(InputStream input) {
        try {
            // Read JSON content
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line);
                }
            }
            
            // Parse JSON manually (simple key-value extraction)
            Properties properties = new Properties();
            String jsonContent = content.toString().trim();
            
            if (jsonContent.startsWith("{") && jsonContent.endsWith("}")) {
                // Remove outer braces
                jsonContent = jsonContent.substring(1, jsonContent.length() - 1);
                
                // Split by commas and extract key-value pairs
                String[] entries = jsonContent.split(",(?=\\s*\")");
                for (String entry : entries) {
                    entry = entry.trim();
                    if (entry.contains(":")) {
                        String[] parts = entry.split(":", 2);
                        if (parts.length == 2) {
                            String key = parts[0].trim().replaceAll("\"", "");
                            String value = parts[1].trim().replaceAll("\"", "");
                            properties.setProperty(key, value);
                        }
                    }
                }
            }
            
            return properties;
        } catch (Exception e) {
            LOGGER.error("Failed to parse JSON language file", e);
            return null;
        }
    }
    
    /**
     * Create a basic fallback language to prevent missing messages
     */
    private Properties createBasicFallbackLanguage() {
        Properties fallback = new Properties();
        
        // Essential messages
        fallback.setProperty("general.prefix", "&6[NeoEssentials]&r");
        fallback.setProperty("general.no_permission", "&cYou don't have permission to use this command!");
        fallback.setProperty("general.player_not_found", "&cPlayer '{0}' not found!");
        fallback.setProperty("general.invalid_usage", "&cInvalid usage! Use: {0}");
        fallback.setProperty("general.console_only", "&cThis command can only be used from console!");
        fallback.setProperty("general.player_only", "&cThis command can only be used by players!");
        
        // Command messages
        fallback.setProperty("command.teleport.success", "&aTeleported to {0}!");
        fallback.setProperty("command.heal.success", "&aYou have been healed!");
        fallback.setProperty("command.feed.success", "&aYou have been fed!");
        fallback.setProperty("command.fly.enabled", "&aFlight enabled!");
        fallback.setProperty("command.fly.disabled", "&cFlight disabled!");
        
        // Shop messages
        fallback.setProperty("shop.invalid", "&cInvalid shop sign!");
        fallback.setProperty("shop.no_permission", "&cYou don't have permission to use this shop!");
        fallback.setProperty("shop.out_of_stock", "&cThis shop is out of stock!");
        fallback.setProperty("shop.not_enough_money", "&cYou don't have enough money!");
        
        LOGGER.info("Created basic fallback language with {} entries", fallback.size());
        return fallback;
    }
    
    /**
     * Load a specific language file
     */
    private void loadLanguageFile(Path languageFile) {
        String fileName = languageFile.getFileName().toString();
        String language = fileName.replace(".properties", "");
        
        try (InputStream input = Files.newInputStream(languageFile)) {
            Properties properties = new Properties();
            properties.load(new InputStreamReader(input, "UTF-8"));
            languageFiles.put(language, properties);
            LOGGER.debug("Loaded language file: {}", fileName);
        } catch (IOException e) {
            LOGGER.error("Failed to load language file: {}", fileName, e);
        }
    }
    
    /**
     * Get a localized message for a player
     */
    public String getMessage(ServerPlayer player, String key, Object... placeholders) {
        String locale = getPlayerLocale(player);
        return getMessage(locale, key, placeholders);
    }
    
    /**
     * Get a localized message for a specific locale
     */
    public String getMessage(String locale, String key, Object... placeholders) {
        String message = getRawMessage(locale, key);
        return replacePlaceholders(message, placeholders);
    }
    
    /**
     * Get a raw message without placeholder replacement
     */
    public String getRawMessage(String locale, String key) {
        Properties messages = languageFiles.get(locale);
        
        if (messages != null && messages.containsKey(key)) {
            return messages.getProperty(key);
        }
        
        // Fallback to default language
        if (!locale.equals(defaultLanguage)) {
            Properties defaultMessages = languageFiles.get(defaultLanguage);
            if (defaultMessages != null && defaultMessages.containsKey(key)) {
                return defaultMessages.getProperty(key);
            }
        }
        
        // Final fallback
        return "&c[Missing: " + key + "]";
    }
    
    /**
     * Replace placeholders in a message
     */
    private String replacePlaceholders(String message, Object... placeholders) {
        if (placeholders.length == 0) {
            return message;
        }
        
        String result = message;
        
        // Replace indexed placeholders {0}, {1}, etc.
        for (int i = 0; i < placeholders.length; i++) {
            result = result.replace("{" + i + "}", String.valueOf(placeholders[i]));
        }
        
        // Replace named placeholders if placeholders are provided as key-value pairs
        if (placeholders.length % 2 == 0) {
            for (int i = 0; i < placeholders.length; i += 2) {
                String key = String.valueOf(placeholders[i]);
                String value = String.valueOf(placeholders[i + 1]);
                result = result.replace("{" + key + "}", value);
            }
        }
        
        return result;
    }
    
    /**
     * Get the locale for a player
     */
    public String getPlayerLocale(ServerPlayer player) {
        UUID playerId = player.getUUID();
        
        // Check if we have a cached locale for this player
        String cached = playerLocales.get(playerId);
        if (cached != null) {
            return cached;
        }
        
        // Try to detect player's locale
        String detected = detectPlayerLocale(player);
        playerLocales.put(playerId, detected);
        
        return detected;
    }
    
    /**
     * Detect a player's locale based on their client settings
     */
    private String detectPlayerLocale(ServerPlayer player) {
        try {
            // Try to get the client's language setting
            // This is a placeholder - in a real implementation, you'd need to
            // hook into the client's language packet or use other detection methods
            String clientLanguage = getClientLanguage(player);
            
            if (clientLanguage != null && languageFiles.containsKey(clientLanguage)) {
                return clientLanguage;
            }
            
        } catch (Exception e) {
            LOGGER.debug("Failed to detect locale for player {}", player.getName().getString(), e);
        }
        
        return defaultLanguage;
    }
    
    /**
     * Get the client language (placeholder implementation)
     */
    private String getClientLanguage(ServerPlayer player) {
    // Actual client language detection would require querying the player's settings, custom packet handling, or mod communication.
    // For now, default to English.
    return defaultLanguage;
    }
    
    /**
     * Set a player's locale manually
     */
    public void setPlayerLocale(ServerPlayer player, String locale) {
        if (languageFiles.containsKey(locale)) {
            playerLocales.put(player.getUUID(), locale);
            LOGGER.info("Set locale for player {} to {}", player.getName().getString(), locale);
        } else {
            LOGGER.warn("Attempted to set unsupported locale '{}' for player {}", 
                       locale, player.getName().getString());
        }
    }
    
    /**
     * Reload all language files
     */
    public void reloadLanguages() {
        languageFiles.clear();
        loadLanguageFiles();
        LOGGER.info("Reloaded {} language files", languageFiles.size());
    }
    
    /**
     * Get all available languages
     */
    public Set<String> getAvailableLanguages() {
        return new HashSet<>(languageFiles.keySet());
    }
    
    /**
     * Get the default language
     */
    public String getDefaultLanguage() {
        return defaultLanguage;
    }
    
    /**
     * Set the default language
     */
    public void setDefaultLanguage(String language) {
        if (languageFiles.containsKey(language)) {
            this.defaultLanguage = language;
            LOGGER.info("Default language set to: {}", language);
        }
    }
    
    /**
     * Get a display name for a language code
     */
    public String getLanguageDisplayName(String languageCode) {
        switch (languageCode) {
            case "en_US": return "English (US)";
            case "es_ES": return "Español (España)";
            case "fr_FR": return "Français (France)";
            case "de_DE": return "Deutsch (Deutschland)";
            case "it_IT": return "Italiano (Italia)";
            case "pt_BR": return "Português (Brasil)";
            case "ru_RU": return "Русский (Россия)";
            case "ja_JP": return "日本語 (日本)";
            case "ko_KR": return "한국어 (대한민국)";
            case "zh_CN": return "中文 (中国)";
            case "zh_TW": return "中文 (台灣)";
            case "nl_NL": return "Nederlands (Nederland)";
            default: return languageCode;
        }
    }
    
    /**
     * Get statistics about the language system
     */
    public Map<String, Object> getLanguageStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("available_languages", languageFiles.size());
        stats.put("default_language", defaultLanguage);
        stats.put("player_locales", playerLocales.size());
        stats.put("loaded_languages", new ArrayList<>(languageFiles.keySet()));
        
        // Count messages per language
        Map<String, Integer> messageCounts = new HashMap<>();
        languageFiles.forEach((lang, props) -> messageCounts.put(lang, props.size()));
        stats.put("message_counts", messageCounts);
        
        return stats;
    }
}
