package com.zerog.neoessentials.localization;

import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private final Pattern placeholderPattern = Pattern.compile("\\{([A-Z_]+)\\}");
    
    private static final String[] SUPPORTED_LANGUAGES = {
        "en_US", "es_ES", "fr_FR", "de_DE", "it_IT", "pt_BR", 
        "ru_RU", "ja_JP", "ko_KR", "zh_CN", "zh_TW", "nl_NL"
    };
    
    private LanguageManager(Path configPath) {
        this.languageDirectory = configPath.resolve("languages");
        // Also check resources directory for additional language files
        try {
            Path resourcesLangPath = Paths.get("src/main/resources/assets/neoessentials/lang");
            if (!Files.exists(resourcesLangPath)) {
                // Try relative to working directory
                resourcesLangPath = Paths.get("../src/main/resources/assets/neoessentials/lang");
            }
            if (Files.exists(resourcesLangPath)) {
                LOGGER.info("Found additional language files in resources directory: {}", resourcesLangPath);
            }
        } catch (Exception e) {
            LOGGER.warn("Could not access resources language directory: {}", e.getMessage());
        }
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
            // Create language directory if it doesn't exist
            if (!Files.exists(languageDirectory)) {
                Files.createDirectories(languageDirectory);
                LOGGER.info("Created language directory: {}", languageDirectory);
            }
            
            // Load all language files
            loadLanguageFiles();
            
            // Create default language files if they don't exist
            createDefaultLanguageFiles();
            
            LOGGER.info("Language system initialized with {} languages", languageFiles.size());
            
        } catch (IOException e) {
            LOGGER.error("Failed to initialize language system", e);
        }
    }
    
    /**
     * Load all language files from both config and resources directories
     */
    private void loadLanguageFiles() {
        try {
            // Load from config directory (runtime generated)
            if (Files.exists(languageDirectory)) {
                Files.list(languageDirectory)
                    .filter(path -> path.toString().endsWith(".properties"))
                    .forEach(this::loadLanguageFile);
            }
            
            // Load from resources directory (mod defaults)
            loadResourceLanguageFiles();
                
        } catch (IOException e) {
            LOGGER.error("Failed to load language files", e);
        }
    }
    
    /**
     * Load language files from resources directory
     */
    private void loadResourceLanguageFiles() {
        try {
            // Try multiple possible paths for resources
            Path[] possiblePaths = {
                Paths.get("src/main/resources/assets/neoessentials/lang"),
                Paths.get("../src/main/resources/assets/neoessentials/lang"),
                Paths.get("resources/assets/neoessentials/lang"),
                Paths.get("assets/neoessentials/lang")
            };
            
            for (Path resourcesLangPath : possiblePaths) {
                if (Files.exists(resourcesLangPath)) {
                    LOGGER.info("Loading language files from resources: {}", resourcesLangPath);
                    Files.list(resourcesLangPath)
                        .filter(path -> path.toString().endsWith(".properties") || path.toString().endsWith(".json"))
                        .forEach(this::loadResourceLanguageFile);
                    break;
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Could not load resource language files: {}", e.getMessage());
        }
    }
    
    /**
     * Load a language file from resources (could be .properties or .json)
     */
    private void loadResourceLanguageFile(Path languageFile) {
        String fileName = languageFile.getFileName().toString();
        String language = fileName.replaceAll("\\.(properties|json)$", "");
        
        try (InputStream input = Files.newInputStream(languageFile)) {
            Properties properties = new Properties();
            
            if (fileName.endsWith(".json")) {
                // For JSON files, we'd need to parse JSON and convert to properties
                // For now, we'll log and skip JSON files or implement simple JSON parsing
                LOGGER.debug("Found JSON language file (not yet supported): {}", fileName);
                return;
            } else {
                properties.load(new InputStreamReader(input, "UTF-8"));
            }
            
            // Only add if we don't already have this language from config
            if (!languageFiles.containsKey(language)) {
                languageFiles.put(language, properties);
                LOGGER.debug("Loaded resource language file: {}", fileName);
            } else {
                LOGGER.debug("Config language file takes precedence over resource file: {}", fileName);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load resource language file: {}", fileName, e);
        }
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
     * Create default language files with common messages
     */
    private void createDefaultLanguageFiles() {
        for (String language : SUPPORTED_LANGUAGES) {
            Path languageFile = languageDirectory.resolve(language + ".properties");
            if (!Files.exists(languageFile)) {
                createLanguageFile(language, languageFile);
            }
        }
    }
    
    /**
     * Create a language file with default messages
     */
    private void createLanguageFile(String language, Path languageFile) {
        Properties defaultMessages = getDefaultMessages(language);
        
        try (OutputStreamWriter writer = new OutputStreamWriter(
                Files.newOutputStream(languageFile), "UTF-8")) {
            
            // Write header
            writer.write("# NeoEssentials Language File - " + language + "\n");
            writer.write("# Generated on " + new Date() + "\n");
            writer.write("# Edit this file to customize messages for your server\n\n");
            
            // Write messages in categories
            writeMessagesCategory(writer, "# === GENERAL MESSAGES ===", defaultMessages, "general.");
            writeMessagesCategory(writer, "# === COMMAND MESSAGES ===", defaultMessages, "command.");
            writeMessagesCategory(writer, "# === ERROR MESSAGES ===", defaultMessages, "error.");
            writeMessagesCategory(writer, "# === SUCCESS MESSAGES ===", defaultMessages, "success.");
            writeMessagesCategory(writer, "# === NOTIFICATION MESSAGES ===", defaultMessages, "notification.");
            
            LOGGER.info("Created default language file: {}", language);
            
        } catch (IOException e) {
            LOGGER.error("Failed to create language file: {}", language, e);
        }
    }
    
    /**
     * Write a category of messages to the language file
     */
    private void writeMessagesCategory(OutputStreamWriter writer, String header, 
                                     Properties messages, String prefix) throws IOException {
        writer.write(header + "\n");
        
        messages.entrySet().stream()
            .filter(entry -> entry.getKey().toString().startsWith(prefix))
            .sorted((e1, e2) -> e1.getKey().toString().compareTo(e2.getKey().toString()))
            .forEach(entry -> {
                try {
                    writer.write(entry.getKey() + "=" + entry.getValue() + "\n");
                } catch (IOException e) {
                    LOGGER.error("Failed to write message: {}", entry.getKey(), e);
                }
            });
        
        writer.write("\n");
    }
    
    /**
     * Get default messages for a language
     */
    private Properties getDefaultMessages(String language) {
        Properties messages = new Properties();
        
        switch (language) {
            case "en_US":
                addEnglishMessages(messages);
                break;
            case "es_ES":
                addSpanishMessages(messages);
                break;
            case "fr_FR":
                addFrenchMessages(messages);
                break;
            case "de_DE":
                addGermanMessages(messages);
                break;
            default:
                addEnglishMessages(messages); // Fallback to English
        }
        
        return messages;
    }
    
    /**
     * Add English messages
     */
    private void addEnglishMessages(Properties messages) {
        // General messages
        messages.setProperty("general.prefix", "&6[NeoEssentials]&r");
        messages.setProperty("general.no_permission", "&cYou don't have permission to use this command!");
        messages.setProperty("general.player_not_found", "&cPlayer '{PLAYER}' not found!");
        messages.setProperty("general.invalid_usage", "&cInvalid usage! Use: {USAGE}");
        messages.setProperty("general.console_only", "&cThis command can only be used from console!");
        messages.setProperty("general.player_only", "&cThis command can only be used by players!");
        
        // Command messages
        messages.setProperty("command.teleport.success", "&aTeleported to {PLAYER}!");
        messages.setProperty("command.heal.success", "&aYou have been healed!");
        messages.setProperty("command.feed.success", "&aYou have been fed!");
        messages.setProperty("command.fly.enabled", "&aFlight enabled!");
        messages.setProperty("command.fly.disabled", "&cFlight disabled!");
        messages.setProperty("command.gamemode.changed", "&aGamemode changed to {GAMEMODE}!");
        
        // Error messages
        messages.setProperty("error.command_failed", "&cCommand failed: {ERROR}");
        messages.setProperty("error.database_error", "&cDatabase error occurred!");
        messages.setProperty("error.file_not_found", "&cFile not found: {FILE}");
        messages.setProperty("error.invalid_number", "&c'{VALUE}' is not a valid number!");
        
        // Success messages
        messages.setProperty("success.config_reloaded", "&aConfiguration reloaded successfully!");
        messages.setProperty("success.player_banned", "&aPlayer {PLAYER} has been banned!");
        messages.setProperty("success.player_unbanned", "&aPlayer {PLAYER} has been unbanned!");
        
        // Notification messages
        messages.setProperty("notification.player_joined", "&e{PLAYER} joined the server");
        messages.setProperty("notification.player_left", "&e{PLAYER} left the server");
        messages.setProperty("notification.server_restart", "&cServer restart in {TIME} minutes!");
    }
    
    /**
     * Add Spanish messages
     */
    private void addSpanishMessages(Properties messages) {
        messages.setProperty("general.prefix", "&6[NeoEssentials]&r");
        messages.setProperty("general.no_permission", "&c¡No tienes permisos para usar este comando!");
        messages.setProperty("general.player_not_found", "&c¡Jugador '{PLAYER}' no encontrado!");
        messages.setProperty("general.invalid_usage", "&c¡Uso inválido! Usa: {USAGE}");
        messages.setProperty("command.teleport.success", "&a¡Teletransportado a {PLAYER}!");
        messages.setProperty("command.heal.success", "&a¡Has sido curado!");
        messages.setProperty("command.feed.success", "&a¡Has sido alimentado!");
        messages.setProperty("notification.player_joined", "&e{PLAYER} se unió al servidor");
        messages.setProperty("notification.player_left", "&e{PLAYER} dejó el servidor");
    }
    
    /**
     * Add French messages
     */
    private void addFrenchMessages(Properties messages) {
        messages.setProperty("general.prefix", "&6[NeoEssentials]&r");
        messages.setProperty("general.no_permission", "&cVous n'avez pas la permission d'utiliser cette commande!");
        messages.setProperty("general.player_not_found", "&cJoueur '{PLAYER}' introuvable!");
        messages.setProperty("general.invalid_usage", "&cUtilisation invalide! Utilisez: {USAGE}");
        messages.setProperty("command.teleport.success", "&aTéléporté vers {PLAYER}!");
        messages.setProperty("command.heal.success", "&aVous avez été soigné!");
        messages.setProperty("command.feed.success", "&aVous avez été nourri!");
        messages.setProperty("notification.player_joined", "&e{PLAYER} a rejoint le serveur");
        messages.setProperty("notification.player_left", "&e{PLAYER} a quitté le serveur");
    }
    
    /**
     * Add German messages
     */
    private void addGermanMessages(Properties messages) {
        messages.setProperty("general.prefix", "&6[NeoEssentials]&r");
        messages.setProperty("general.no_permission", "&cDu hast keine Berechtigung für diesen Befehl!");
        messages.setProperty("general.player_not_found", "&cSpieler '{PLAYER}' nicht gefunden!");
        messages.setProperty("general.invalid_usage", "&cUngültige Verwendung! Verwende: {USAGE}");
        messages.setProperty("command.teleport.success", "&aZu {PLAYER} teleportiert!");
        messages.setProperty("command.heal.success", "&aDu wurdest geheilt!");
        messages.setProperty("command.feed.success", "&aDu wurdest gefüttert!");
        messages.setProperty("notification.player_joined", "&e{PLAYER} ist dem Server beigetreten");
        messages.setProperty("notification.player_left", "&e{PLAYER} hat den Server verlassen");
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
        // TODO: Implement actual client language detection
        // This could involve:
        // 1. Custom packet handling
        // 2. Player data NBT storage
        // 3. Configuration file per player
        // 4. Database lookup
        
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
        stats.put("supported_languages", Arrays.asList(SUPPORTED_LANGUAGES));
        
        // Count messages per language
        Map<String, Integer> messageCounts = new HashMap<>();
        languageFiles.forEach((lang, props) -> messageCounts.put(lang, props.size()));
        stats.put("message_counts", messageCounts);
        
        return stats;
    }
}
