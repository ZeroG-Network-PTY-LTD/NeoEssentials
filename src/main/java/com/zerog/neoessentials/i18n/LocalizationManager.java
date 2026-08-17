package com.zerog.neoessentials.i18n;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.zerog.neoessentials.util.MessageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages localization and internationalization for the NeoEssentials dashboard.
 * Hybrid approach: Uses MessageUtil's language file infrastructure but adds
 * dashboard-specific features (RTL support, locale formatting, etc.)
 */
public class LocalizationManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(LocalizationManager.class);
    private static LocalizationManager INSTANCE;
    
    private final Path langDirectory;
    private final Map<String, Map<String, String>> dashboardTranslations = new ConcurrentHashMap<>();
    private final Map<String, LanguageInfo> availableLanguages = new LinkedHashMap<>();
    private final String defaultLanguage = "en_us";
    private final Gson gson = new Gson();
    
    // RTL languages
    private static final Set<String> RTL_LANGUAGES = Set.of(
        "ar", "ar_sa", "he", "he_il", "fa", "fa_ir", "ur", "ur_pk"
    );
    
    private LocalizationManager() {
        this.langDirectory = Paths.get("neoessentials", "webdashboard", "lang");
        initialize();
    }
    
    public static LocalizationManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new LocalizationManager();
        }
        return INSTANCE;
    }
    
    /**
     * Initialize localization system
     * Uses hybrid approach: MessageUtil for base translations + dashboard-specific additions
     */
    public void initialize() {
        try {
            // Ensure language directory exists
            if (!Files.exists(langDirectory)) {
                Files.createDirectories(langDirectory);
                NeoLog.info(LOGGER, LogCategory.GENERAL, "Created language directory: {}", langDirectory.toAbsolutePath());
            }
            
            // Register built-in languages
            registerBuiltInLanguages();
            
            // Load dashboard-specific translations (extends MessageUtil's base translations)
            loadDashboardTranslations();
            
            // Create default dashboard translations if they don't exist
            if (!dashboardTranslations.containsKey(defaultLanguage)) {
                createDefaultDashboardTranslations();
            }
            
            NeoLog.info(LOGGER, LogCategory.GENERAL, "Dashboard localization initialized with {} languages", availableLanguages.size());
            NeoLog.info(LOGGER, LogCategory.GENERAL, "Dashboard-specific translations loaded for: {}", dashboardTranslations.keySet());
            
        } catch (Exception e) {
            LOGGER.error("Failed to initialize localization", e);
        }
    }
    
    /**
     * Register built-in language information
     */
    private void registerBuiltInLanguages() {
        // English (US)
        availableLanguages.put("en_us", new LanguageInfo(
            "en_us", "English (US)", "English", "US", false
        ));
        
        // Spanish (Spain)
        availableLanguages.put("es_es", new LanguageInfo(
            "es_es", "Español (España)", "Spanish", "ES", false
        ));
        
        // French (France)
        availableLanguages.put("fr_fr", new LanguageInfo(
            "fr_fr", "Français (France)", "French", "FR", false
        ));
        
        // German (Germany)
        availableLanguages.put("de_de", new LanguageInfo(
            "de_de", "Deutsch (Deutschland)", "German", "DE", false
        ));
        
        // Chinese (Simplified)
        availableLanguages.put("zh_cn", new LanguageInfo(
            "zh_cn", "简体中文", "Chinese (Simplified)", "CN", false
        ));
        
        // Japanese
        availableLanguages.put("ja_jp", new LanguageInfo(
            "ja_jp", "日本語", "Japanese", "JP", false
        ));
        
        // Korean
        availableLanguages.put("ko_kr", new LanguageInfo(
            "ko_kr", "한국어", "Korean", "KR", false
        ));
        
        // Portuguese (Brazil)
        availableLanguages.put("pt_br", new LanguageInfo(
            "pt_br", "Português (Brasil)", "Portuguese (Brazil)", "BR", false
        ));
        
        // Russian
        availableLanguages.put("ru_ru", new LanguageInfo(
            "ru_ru", "Русский", "Russian", "RU", false
        ));
        
        // Arabic (Saudi Arabia) - RTL
        availableLanguages.put("ar_sa", new LanguageInfo(
            "ar_sa", "العربية (السعودية)", "Arabic (Saudi Arabia)", "SA", true
        ));
        
        // Hebrew (Israel) - RTL
        availableLanguages.put("he_il", new LanguageInfo(
            "he_il", "עברית (ישראל)", "Hebrew (Israel)", "IL", true
        ));
    }
    
    /**
     * Load dashboard-specific translation files
     * These extend MessageUtil's base translations with dashboard UI strings
     */
    private void loadDashboardTranslations() {
        try {
            if (!Files.exists(langDirectory)) {
                return;
            }
            
            Files.list(langDirectory)
                .filter(path -> path.toString().endsWith("_dashboard.json"))
                .forEach(this::loadDashboardTranslationFile);
                
        } catch (IOException e) {
            LOGGER.error("Failed to load dashboard translation files", e);
        }
    }
    
    /**
     * Load a single dashboard translation file
     */
    private void loadDashboardTranslationFile(Path filePath) {
        try {
            String fileName = filePath.getFileName().toString();
            // Extract language code from filename (e.g., "en_us_dashboard.json" -> "en_us")
            String langCode = fileName.replace("_dashboard.json", "");
            
            String content = Files.readString(filePath, StandardCharsets.UTF_8);
            Type type = new TypeToken<Map<String, String>>(){}.getType();
            Map<String, String> langTranslations = gson.fromJson(content, type);
            
            dashboardTranslations.put(langCode, langTranslations);
            NeoLog.debug(LOGGER, LogCategory.GENERAL, "Loaded dashboard translations: {} ({} keys)", langCode, langTranslations.size());
            
        } catch (Exception e) {
            LOGGER.error("Failed to load dashboard translation file: {}", filePath, e);
        }
    }
    
    /**
     * Create default English dashboard translation file
     * Dashboard-specific UI strings that complement MessageUtil's in-game translations
     */
    private void createDefaultDashboardTranslations() {
        Map<String, String> defaultTranslations = new LinkedHashMap<>();
        
        // Dashboard UI
        defaultTranslations.put("dashboard.title", "NeoEssentials Dashboard");
        defaultTranslations.put("dashboard.welcome", "Welcome to NeoEssentials");
        defaultTranslations.put("dashboard.logout", "Logout");
        defaultTranslations.put("dashboard.language", "Language");
        
        // Navigation
        defaultTranslations.put("nav.overview", "Overview");
        defaultTranslations.put("nav.players", "Players");
        defaultTranslations.put("nav.server", "Server");
        defaultTranslations.put("nav.console", "Console");
        defaultTranslations.put("nav.logs", "Logs");
        defaultTranslations.put("nav.config", "Configuration");
        defaultTranslations.put("nav.files", "Files");
        defaultTranslations.put("nav.database", "Database");
        defaultTranslations.put("nav.moderation", "Moderation");
        defaultTranslations.put("nav.analytics", "Analytics");
        defaultTranslations.put("nav.settings", "Settings");
        
        // Common actions
        defaultTranslations.put("action.save", "Save");
        defaultTranslations.put("action.cancel", "Cancel");
        defaultTranslations.put("action.delete", "Delete");
        defaultTranslations.put("action.edit", "Edit");
        defaultTranslations.put("action.create", "Create");
        defaultTranslations.put("action.refresh", "Refresh");
        defaultTranslations.put("action.search", "Search");
        defaultTranslations.put("action.filter", "Filter");
        defaultTranslations.put("action.export", "Export");
        defaultTranslations.put("action.import", "Import");
        defaultTranslations.put("action.download", "Download");
        defaultTranslations.put("action.upload", "Upload");
        
        // Player management
        defaultTranslations.put("players.online", "Online Players");
        defaultTranslations.put("players.total", "Total Players");
        defaultTranslations.put("players.kick", "Kick Player");
        defaultTranslations.put("players.ban", "Ban Player");
        defaultTranslations.put("players.unban", "Unban Player");
        defaultTranslations.put("players.mute", "Mute Player");
        defaultTranslations.put("players.unmute", "Unmute Player");
        defaultTranslations.put("players.whitelist", "Whitelist");
        defaultTranslations.put("players.inventory", "Inventory");
        defaultTranslations.put("players.statistics", "Statistics");
        
        // Server info
        defaultTranslations.put("server.status", "Server Status");
        defaultTranslations.put("server.uptime", "Uptime");
        defaultTranslations.put("server.tps", "TPS");
        defaultTranslations.put("server.memory", "Memory");
        defaultTranslations.put("server.cpu", "CPU");
        defaultTranslations.put("server.disk", "Disk Space");
        defaultTranslations.put("server.version", "Version");
        defaultTranslations.put("server.worlds", "Worlds");
        
        // Database browser
        defaultTranslations.put("database.list", "Databases");
        defaultTranslations.put("database.tables", "Tables");
        defaultTranslations.put("database.query", "Query");
        defaultTranslations.put("database.export", "Export");
        defaultTranslations.put("database.rows", "Rows");
        defaultTranslations.put("database.schema", "Schema");
        
        // Log viewer
        defaultTranslations.put("logs.viewer", "Log Viewer");
        defaultTranslations.put("logs.level", "Log Level");
        defaultTranslations.put("logs.search", "Search Logs");
        defaultTranslations.put("logs.download", "Download Log");
        defaultTranslations.put("logs.tail", "Tail Logs");
        
        // Time and date
        defaultTranslations.put("time.seconds", "seconds");
        defaultTranslations.put("time.minutes", "minutes");
        defaultTranslations.put("time.hours", "hours");
        defaultTranslations.put("time.days", "days");
        defaultTranslations.put("time.weeks", "weeks");
        defaultTranslations.put("time.months", "months");
        defaultTranslations.put("time.years", "years");
        defaultTranslations.put("time.ago", "{0} ago");
        defaultTranslations.put("time.in", "in {0}");
        
        // Messages
        defaultTranslations.put("message.success", "Operation completed successfully");
        defaultTranslations.put("message.error", "An error occurred");
        defaultTranslations.put("message.loading", "Loading...");
        defaultTranslations.put("message.confirm", "Are you sure?");
        defaultTranslations.put("message.no_data", "No data available");
        defaultTranslations.put("message.saved", "Changes saved successfully");
        
        // Pagination
        defaultTranslations.put("pagination.page", "Page");
        defaultTranslations.put("pagination.of", "of");
        defaultTranslations.put("pagination.showing", "Showing {0} to {1} of {2} entries");
        defaultTranslations.put("pagination.per_page", "Per page");
        
        dashboardTranslations.put(defaultLanguage, defaultTranslations);
        
        // Save to file with _dashboard suffix to distinguish from MessageUtil's lang files
        try {
            Path langFile = langDirectory.resolve(defaultLanguage + "_dashboard.json");
            
            Files.writeString(langFile, gson.toJson(defaultTranslations), StandardCharsets.UTF_8);
            NeoLog.info(LOGGER, LogCategory.GENERAL, "Created default dashboard translation file: {}", langFile);
            
        } catch (IOException e) {
            LOGGER.error("Failed to create default dashboard translation file", e);
        }
    }
    
    /**
     * Get translation for key in specified language
     * Hybrid approach: Checks dashboard translations first, then falls back to MessageUtil
     */
    public String translate(String key, String language) {
        return translate(key, language, key);
    }
    
    /**
     * Get translation with fallback
     * Priority: 1) Dashboard translations (language), 2) Dashboard translations (default),
     *           3) MessageUtil (in-game), 4) Fallback string
     */
    public String translate(String key, String language, String fallback) {
        // Try dashboard translations for requested language
        Map<String, String> dashboardMap = dashboardTranslations.get(language);
        if (dashboardMap != null && dashboardMap.containsKey(key)) {
            return dashboardMap.get(key);
        }
        
        // Try dashboard translations for default language
        if (!language.equals(defaultLanguage)) {
            dashboardMap = dashboardTranslations.get(defaultLanguage);
            if (dashboardMap != null && dashboardMap.containsKey(key)) {
                return dashboardMap.get(key);
            }
        }
        
        // Try MessageUtil (for in-game message keys that might be reused in dashboard)
        if (MessageUtil.hasTranslation(key)) {
            return MessageUtil.localize(key);
        }
        
        // Return fallback
        return fallback;
    }
    
    /**
     * Get translation with placeholders
     */
    public String translate(String key, String language, Object... args) {
        String translation = translate(key, language);
        
        // Replace placeholders {0}, {1}, etc.
        for (int i = 0; i < args.length; i++) {
            translation = translation.replace("{" + i + "}", String.valueOf(args[i]));
        }
        
        return translation;
    }
    
    /**
     * Get all translations for a language (dashboard-specific only)
     * For complete translations including in-game messages, use both this and MessageUtil
     */
    public Map<String, String> getTranslations(String language) {
        Map<String, String> dashboardMap = dashboardTranslations.get(language);
        if (dashboardMap != null) {
            return new HashMap<>(dashboardMap);
        }
        
        // Return default language if requested language not found
        return new HashMap<>(dashboardTranslations.getOrDefault(defaultLanguage, new HashMap<>()));
    }
    
    /**
     * Get combined translations (dashboard + in-game from MessageUtil)
     */
    public Map<String, String> getAllTranslations(String language) {
        Map<String, String> combined = new HashMap<>();
        
        // Start with dashboard translations
        Map<String, String> dashboardMap = dashboardTranslations.get(language);
        if (dashboardMap != null) {
            combined.putAll(dashboardMap);
        }
        
        // Note: MessageUtil translations are loaded on-demand, not bulk exported
        // Dashboard translations take priority over in-game translations
        
        return combined;
    }
    
    /**
     * Get available languages
     */
    public List<LanguageInfo> getAvailableLanguages() {
        return new ArrayList<>(availableLanguages.values());
    }
    
    /**
     * Check if language is supported
     */
    public boolean isLanguageSupported(String language) {
        return availableLanguages.containsKey(language);
    }
    
    /**
     * Check if language is RTL
     */
    public boolean isRTL(String language) {
        LanguageInfo info = availableLanguages.get(language);
        if (info != null) {
            return info.isRtl();
        }
        return RTL_LANGUAGES.contains(language);
    }
    
    /**
     * Format date/time according to locale
     */
    public String formatDateTime(Instant instant, String language, FormatStyle style) {
        Locale locale = getLocaleFromLanguage(language);
        DateTimeFormatter formatter = DateTimeFormatter
            .ofLocalizedDateTime(style)
            .withLocale(locale)
            .withZone(ZoneId.systemDefault());
        return formatter.format(instant);
    }
    
    /**
     * Format date according to locale
     */
    public String formatDate(Instant instant, String language, FormatStyle style) {
        Locale locale = getLocaleFromLanguage(language);
        DateTimeFormatter formatter = DateTimeFormatter
            .ofLocalizedDate(style)
            .withLocale(locale)
            .withZone(ZoneId.systemDefault());
        return formatter.format(instant);
    }
    
    /**
     * Format time according to locale
     */
    public String formatTime(Instant instant, String language, FormatStyle style) {
        Locale locale = getLocaleFromLanguage(language);
        DateTimeFormatter formatter = DateTimeFormatter
            .ofLocalizedTime(style)
            .withLocale(locale)
            .withZone(ZoneId.systemDefault());
        return formatter.format(instant);
    }
    
    /**
     * Format number according to locale
     */
    public String formatNumber(Number number, String language) {
        Locale locale = getLocaleFromLanguage(language);
        NumberFormat formatter = NumberFormat.getInstance(locale);
        return formatter.format(number);
    }
    
    /**
     * Format currency according to locale
     */
    public String formatCurrency(Number amount, String language) {
        Locale locale = getLocaleFromLanguage(language);
        NumberFormat formatter = NumberFormat.getCurrencyInstance(locale);
        return formatter.format(amount);
    }
    
    /**
     * Format percentage according to locale
     */
    public String formatPercent(Number value, String language) {
        Locale locale = getLocaleFromLanguage(language);
        NumberFormat formatter = NumberFormat.getPercentInstance(locale);
        return formatter.format(value);
    }
    
    /**
     * Convert language code to Locale
     */
    private Locale getLocaleFromLanguage(String language) {
        String[] parts = language.split("_");
        if (parts.length == 2) {
            return new Locale.Builder().setLanguage(parts[0]).setRegion(parts[1].toUpperCase()).build();
        } else if (parts.length == 1) {
            return new Locale.Builder().setLanguage(parts[0]).build();
        }
        return Locale.US;
    }
    
    /**
     * Reload all dashboard translation files
     */
    public void reload() {
        dashboardTranslations.clear();
        loadDashboardTranslations();
        
        // Also reload MessageUtil's translations
        MessageUtil.reloadTranslations();
        
        NeoLog.info(LOGGER, LogCategory.GENERAL, "Reloaded dashboard translation files and MessageUtil translations");
    }
    
    /**
     * Language information class
     */
    public static class LanguageInfo {
        private final String code;
        private final String nativeName;
        private final String englishName;
        private final String countryCode;
        private final boolean rtl;
        
        public LanguageInfo(String code, String nativeName, String englishName, 
                          String countryCode, boolean rtl) {
            this.code = code;
            this.nativeName = nativeName;
            this.englishName = englishName;
            this.countryCode = countryCode;
            this.rtl = rtl;
        }
        
        public String getCode() { return code; }
        public String getNativeName() { return nativeName; }
        public String getEnglishName() { return englishName; }
        public String getCountryCode() { return countryCode; }
        public boolean isRtl() { return rtl; }
    }
}
