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
        fallback.setProperty("neoessentials.shop.invalid", "&cInvalid shop sign!");
        fallback.setProperty("neoessentials.shop.no_permission", "&cYou don't have permission to use this shop!");
        fallback.setProperty("neoessentials.shop.out_of_stock", "&cThis shop is out of stock!");
        fallback.setProperty("neoessentials.shop.not_enough_money", "&cYou don't have enough money!");
        
        // Home system messages
        fallback.setProperty("neoessentials.home.disabled", "&cHome system is disabled!");
        fallback.setProperty("neoessentials.home.invalid_name", "&cInvalid home name: {0}");
        fallback.setProperty("neoessentials.home.max_homes_reached", "&cYou have reached the maximum number of homes ({0})!");
        fallback.setProperty("neoessentials.home.restricted_world", "&cYou cannot set homes in this world!");
        fallback.setProperty("neoessentials.home.deleted", "&aHome '{0}' deleted successfully!");
        fallback.setProperty("neoessentials.home.cooldown_active", "&cYou must wait {0} before using this command again!");
        fallback.setProperty("neoessentials.home.unsafe_location", "&cUnsafe teleport location! Teleport cancelled.");
        fallback.setProperty("neoessentials.home.list_none", "&cYou have no homes set!");
        fallback.setProperty("neoessentials.home.list_header", "&aYour homes ({0}/{1}):");
        fallback.setProperty("neoessentials.home.list_entry", "&7- {0}");
        
        // Heal command
        fallback.setProperty("neoessentials.heal.self_success", "&aYou have been healed!");
        fallback.setProperty("neoessentials.heal.other_success", "&aYou healed {0}!");
        fallback.setProperty("neoessentials.heal.success", "&aYou have been healed by {0}!");
        
        // Help command
        fallback.setProperty("neoessentials.help.entry", "&f{0} &7- {1}");
        fallback.setProperty("neoessentials.help.entry_hover", "&7Usage: {0}");
        fallback.setProperty("neoessentials.help.prev_button", "&e◀ Previous");
        fallback.setProperty("neoessentials.help.prev_hover", "&7Go to previous page");
        fallback.setProperty("neoessentials.help.next_button", "&eNext ▶");
        fallback.setProperty("neoessentials.help.next_hover", "&7Go to next page");
        fallback.setProperty("neoessentials.help.page_info", "&7Page {0} of {1}");
        
        // Item command  
        fallback.setProperty("neoessentials.command.invalid_syntax", "&cInvalid syntax: {0}");
        fallback.setProperty("neoessentials.player.not_found", "&cPlayer '{0}' not found!");
        fallback.setProperty("neoessentials.item.not_found", "&cItem '{0}' not found!");
        fallback.setProperty("neoessentials.item.invalid_name", "&cInvalid item name: {0}");
        fallback.setProperty("neoessentials.item.give", "&aGiven {0} {1}!");
        fallback.setProperty("neoessentials.item.give_other", "&aGiven {0} {1} {2}!");
        
        // Kick command
        fallback.setProperty("neoessentials.kick.player_not_found", "&cPlayer not found!");
        fallback.setProperty("neoessentials.kick.cannot_self", "&cYou cannot kick yourself!");
        fallback.setProperty("neoessentials.kick.broadcast", "&e{0} was kicked from the server");
        fallback.setProperty("neoessentials.kick.success", "&aSuccessfully kicked {0}");
        fallback.setProperty("neoessentials.kick.reason", "&7Reason: {0}");
        
        // List command
        fallback.setProperty("neoessentials.list.header", "&aOnline players ({0}/{1}):");
        fallback.setProperty("neoessentials.list.none_online", "&cNo players online");
        fallback.setProperty("neoessentials.list.admins", "&cAdmins: {0}");
        fallback.setProperty("neoessentials.list.mods", "&eModerators: {0}");
        fallback.setProperty("neoessentials.list.players", "&fPlayers: {0}");
        
        // Message command
        fallback.setProperty("neoessentials.msg.only_players", "&cOnly players can send messages!");
        fallback.setProperty("neoessentials.msg.muted", "&cYou are muted and cannot send messages!");
        fallback.setProperty("neoessentials.msg.console_from", "&7[&f{0}&7 → &eConsole&7]: {1}");
        fallback.setProperty("neoessentials.msg.console_to", "&7[&eYou&7 → &fConsole&7]: {0}");
        fallback.setProperty("neoessentials.msg.player_not_found", "&cPlayer '{0}' not found!");
        fallback.setProperty("neoessentials.msg.no_available_recipients", "&cNo available recipients!");
        fallback.setProperty("neoessentials.message.format", "&7[&f{0}&7 → &f{1}&7]: {2}");
        fallback.setProperty("neoessentials.message.reply_format", "&7[&f{0}&7 ← &f{1}&7]: {2}");
        fallback.setProperty("neoessentials.msg.log", "&7[MSG] {0} → {1}: {2}");
        
        // Mute command
        fallback.setProperty("neoessentials.mute.player_not_found", "&cPlayer not found!");
        fallback.setProperty("neoessentials.mute.cannot_self", "&cYou cannot mute yourself!");
        fallback.setProperty("neoessentials.mute.already_muted", "&c{0} is already muted!");
        fallback.setProperty("neoessentials.mute.player.temp", "&cYou have been muted for {0}. Reason: {1}");
        fallback.setProperty("neoessentials.mute.success.temp", "&aSuccessfully muted {0} for {1}. Reason: {2}");
        fallback.setProperty("neoessentials.mute.player", "&cYou have been muted. Reason: {0}");
        fallback.setProperty("neoessentials.mute.success", "&aSuccessfully muted {0}. Reason: {1}");
        fallback.setProperty("neoessentials.mute.invalid_duration", "&cInvalid duration format!");
        fallback.setProperty("neoessentials.mute.too_long", "&cMute duration is too long!");
        
        // InvSee command
        fallback.setProperty("neoessentials.invsee.inventory_title", "{0}'s Inventory");
        
        // MOTD command
        fallback.setProperty("neoessentials.motd.header", "&e=== Server Message of the Day ===");
        fallback.setProperty("neoessentials.motd.body", "&f{0}");
        fallback.setProperty("neoessentials.motd.footer", "&e==================================");
        fallback.setProperty("neoessentials.motd.admin_new", "&aNew MOTD set successfully!");
        fallback.setProperty("neoessentials.motd.admin_new_value", "&7New value: &f{0}");
        fallback.setProperty("neoessentials.motd.set.note", "&7Note: Changes will be visible on next server restart or reload");
        fallback.setProperty("neoessentials.motd.admin_log_set", "&7[MOTD] {0} changed server MOTD to: {1}");
        fallback.setProperty("neoessentials.motd.set.failed", "&cFailed to set MOTD: {0}");
        fallback.setProperty("neoessentials.motd.reload.success", "&aServer MOTD reloaded successfully!");
        fallback.setProperty("neoessentials.motd.reload.current", "&7Current MOTD: &f{0}");
        fallback.setProperty("neoessentials.motd.admin_log_reload", "&7[MOTD] {0} reloaded server MOTD");
        fallback.setProperty("neoessentials.motd.reload.failed", "&cFailed to reload MOTD: {0}");
        
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
        
        // Try to generate a reasonable fallback for common key patterns
        String fallback = generateFallbackMessage(key);
        if (fallback != null) {
            return fallback;
        }
        
        // Final fallback - only log missing keys in debug mode to reduce spam
        if (Boolean.getBoolean("neoessentials.debug.messages")) {
            LOGGER.warn("Missing language key: {} for locale: {}", key, locale);
        }
        
        // Track missing keys for admin review
        missingKeys.add(key);
        
        return "&c[Missing: " + key + "]";
    }
    
    /**
     * Generate reasonable fallback messages for common key patterns
     */
    private String generateFallbackMessage(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        
        // Handle common patterns
        if (key.contains(".feature.tag")) {
            return "Feature";
        }
        if (key.contains(".enabled")) {
            return "&aEnabled";
        }
        if (key.contains(".disabled")) {
            return "&cDisabled";
        }
        if (key.contains(".success")) {
            return "&aSuccess";
        }
        if (key.contains(".error") || key.contains(".failed")) {
            return "&cError";
        }
        if (key.contains(".permission") || key.contains(".no_permission")) {
            return "&cNo permission";
        }
        if (key.contains(".not_found")) {
            return "&cNot found";
        }
        if (key.contains(".invalid")) {
            return "&cInvalid";
        }
        if (key.contains(".reload")) {
            return "&aReloaded";
        }
        
        // Extract the last part of the key as a fallback
        String[] parts = key.split("\\.");
        if (parts.length > 0) {
            String lastPart = parts[parts.length - 1];
            // Convert snake_case to Title Case
            String[] words = lastPart.split("_");
            StringBuilder result = new StringBuilder();
            for (String word : words) {
                if (!word.isEmpty()) {
                    if (result.length() > 0) result.append(" ");
                    result.append(Character.toUpperCase(word.charAt(0)))
                          .append(word.substring(1).toLowerCase());
                }
            }
            return result.toString();
        }
        
        return null;
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
            LOGGER.info("Default language changed to: {}", language);
        } else {
            LOGGER.warn("Cannot set default language to '{}' - not available", language);
        }
    }
    
    /**
     * Check if a key exists in any language file
     */
    public boolean hasKey(String key) {
        for (Properties props : languageFiles.values()) {
            if (props.containsKey(key)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Get all missing keys that have been requested
     */
    public java.util.Set<String> getMissingKeys() {
        return new java.util.HashSet<>(missingKeys);
    }
    
    private final java.util.Set<String> missingKeys = java.util.concurrent.ConcurrentHashMap.newKeySet();    /**
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
