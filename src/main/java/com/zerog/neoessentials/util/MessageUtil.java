package com.zerog.neoessentials.util;

import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
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
    private static final int CURRENT_LANG_VERSION = 18;

    /**
     * Returns the configured server language code, e.g. "fr_fr".
     * Safe to call before config is fully loaded (falls back to "en_us").
     */
    private static String getConfiguredLanguage() {
        try {
            return com.zerog.neoessentials.config.ConfigManager.getServerLanguage();
        } catch (Exception ignored) {}
        return "en_us";
    }

    private static void loadTranslations() {
        if (loaded) return;
        loaded = true;

        LOGGER.debug("=== LOADING NEOESSENTIALS TRANSLATIONS ===");

        String langCode = getConfiguredLanguage();
        LOGGER.info("NeoEssentials: loading language '{}'", langCode);

        File customLangDir = getNeoEssentialsLangCustomDir();
        if (!customLangDir.exists()) {
            boolean dirCreated = customLangDir.mkdirs();
            if (!dirCreated) {
                LOGGER.error("Failed to create custom language directory: {}", customLangDir.getAbsolutePath());
            } else {
                LOGGER.debug("Created custom language directory: {}", customLangDir.getAbsolutePath());
            }
        }
        File serverLangFile = new File(customLangDir, langCode + ".json");
        LOGGER.debug("Server language file path: {}", serverLangFile.getAbsolutePath());

        boolean preserveCustom = false;
        try {
            preserveCustom = com.zerog.neoessentials.config.ConfigManager.isPreserveCustomTranslationsEnabled();
        } catch (Exception ignored) {}

        Map<String, String> finalTranslations;
        if (serverLangFile.exists() && serverLangFile.length() > 0) {
            finalTranslations = loadServerTranslations(serverLangFile);
            if (finalTranslations != null) {
                // Version check — merge any new JAR keys without overwriting user edits
                int deployedVersion = 0;
                try {
                    deployedVersion = Integer.parseInt(
                        finalTranslations.getOrDefault(LANG_VERSION_KEY, "0"));
                } catch (NumberFormatException ignored) {}

                if (preserveCustom) {
                    LOGGER.info("NeoEssentials: localization.preserveCustomTranslations is enabled — " +
                        "skipping merge/auto-fix of '{}'.", serverLangFile.getName());
                } else if (deployedVersion < CURRENT_LANG_VERSION) {
                    LOGGER.info("NeoEssentials: lang file is v{} (current v{}) — merging new keys...",
                        deployedVersion, CURRENT_LANG_VERSION);
                    // Build merge source: configured language + en_us fallback for missing keys
                    Map<String, String> mergeSource = buildJarTranslationsWithFallback(langCode);
                    if (mergeSource != null) {
                        int added = 0;
                        int updated = 0;
                        java.util.regex.Pattern legacyNamedPattern =
                            java.util.regex.Pattern.compile("\\{[A-Z][A-Z0-9_]+}");
                        for (Map.Entry<String, String> e : mergeSource.entrySet()) {
                            if (!finalTranslations.containsKey(e.getKey())) {
                                finalTranslations.put(e.getKey(), e.getValue());
                                added++;
                            } else {
                                // Update keys where server still has legacy {HOME}/{NAME} style
                                // but the JAR now uses positional {0}/{1} style
                                String serverVal = finalTranslations.get(e.getKey());
                                if (serverVal != null
                                        && legacyNamedPattern.matcher(serverVal).find()
                                        && e.getValue().contains("{0}")) {
                                    finalTranslations.put(e.getKey(), e.getValue());
                                    updated++;
                                }
                            }
                        }
                        finalTranslations.put(LANG_VERSION_KEY, String.valueOf(CURRENT_LANG_VERSION));
                        try (java.io.FileWriter fw = new java.io.FileWriter(serverLangFile)) {
                            new com.google.gson.GsonBuilder().setPrettyPrinting()
                                .disableHtmlEscaping().create().toJson(finalTranslations, fw);
                        } catch (Exception ex) {
                            LOGGER.warn("NeoEssentials: could not save merged lang file: {}", ex.getMessage());
                        }
                        LOGGER.info("NeoEssentials: merged {} new + {} updated translation keys (total: {})",
                            added, updated, finalTranslations.size());
                    }
                }
                translations.putAll(finalTranslations);
                LOGGER.info("NeoEssentials: loaded {} translations (language: {})", translations.size(), langCode);
            } else {
                LOGGER.error("Failed to load custom language file, will attempt to update from JAR");
            }
        }
        // If file missing or unreadable, deploy from JAR
        if (translations.isEmpty()) {
            Map<String, String> jarTranslations = buildJarTranslationsWithFallback(langCode);
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
            LOGGER.debug("JAR contains {} translation keys for '{}'", jarTranslations.size(), langCode);
            try {
                updateServerLanguageFile(serverLangFile, jarTranslations);
                if (serverLangFile.exists()) {
                    LOGGER.debug("Language file successfully created: {}", serverLangFile.getAbsolutePath());
                    finalTranslations = loadServerTranslations(serverLangFile);
                    if (finalTranslations != null) {
                        translations.putAll(finalTranslations);
                        LOGGER.info("NeoEssentials: loaded {} translations (updated from JAR, language: {})", translations.size(), langCode);
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
     * Load translations from JAR resource for the given language code.
     * Returns null if the resource is not found.
     */
    private static Map<String, String> loadJarTranslations(String langCode) {
        try (InputStream in = ResourceUtil.getJarLangResource(langCode + ".json")) {
            if (in != null) {
                try (java.util.Scanner scanner = new java.util.Scanner(in, java.nio.charset.StandardCharsets.UTF_8).useDelimiter("\\A")) {
                    String json = scanner.hasNext() ? scanner.next() : "";
                    Gson gson = new Gson();
                    Type type = new TypeToken<Map<String, String>>(){}.getType();
                    return gson.fromJson(json, type);
                }
            } else {
                LOGGER.debug("JAR language resource '{}' not found.", langCode + ".json");
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load JAR translations for '{}': {}", langCode, e.getMessage(), e);
        }
        return null;
    }

    /**
     * Load translations from JAR resource (en_us fallback).
     */
    //noinspection unused
    @SuppressWarnings("unused") // convenience overload kept for external callers
    private static Map<String, String> loadJarTranslations() {
        Map<String, String> result = loadJarTranslations("en_us");
        if (result == null) {
            LOGGER.error("JAR language resource 'en_us.json' not found.");
        }
        return result;
    }

    /**
     * Build a merged translation map for the given language code.
     * Loads the JAR's <langCode>.json and fills any missing keys from en_us.json.
     * If langCode == "en_us" or the language file is not bundled, returns en_us directly.
     */
    private static Map<String, String> buildJarTranslationsWithFallback(String langCode) {
        if (langCode == null || langCode.equals("en_us")) {
            return loadJarTranslations("en_us");
        }
        Map<String, String> base = loadJarTranslations("en_us");
        Map<String, String> lang = loadJarTranslations(langCode);
        if (lang == null || lang.isEmpty()) {
            LOGGER.warn("NeoEssentials: no bundled JAR file for language '{}', falling back to en_us.", langCode);
            return base;
        }
        // Start from en_us base so every key is covered, then overlay the target language
        Map<String, String> merged = new HashMap<>();
        if (base != null) merged.putAll(base);
        merged.putAll(lang); // target language takes priority
        LOGGER.info("NeoEssentials: built '{}' translations ({} keys, {} from en_us fallback)",
            langCode, merged.size(), base != null ? Math.max(0, merged.size() - lang.size()) : 0);
        return merged;
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
                Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
                gson.toJson(translationsWithVersion, writer);
                LOGGER.debug("Updated server language file with {} keys (version {})", translationsWithVersion.size(), CURRENT_LANG_VERSION);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to update server language file: {} ({}): {}", serverFile.getAbsolutePath(), serverFile.getParentFile(), e.getMessage(), e);
        }
    }

    /**
     * Pattern matching named (non-positional) {@code {TOKEN}} placeholders — used to detect
     * unresolved tokens after {@link #resolveTemplate} runs in debug mode.
     */
    private static final java.util.regex.Pattern NAMED_PLACEHOLDER_PATTERN =
            java.util.regex.Pattern.compile("\\{([^0-9'{}\\s][^}]*)}");

    /**
     * Resolve a message template with extra named variables and PlaceholderAPI.
     *
     * <p>Resolution order:
     * <ol>
     *   <li>Apply {@code extraVars} via case-insensitive token replacement so both
     *       {@code {MESSAGE}} and {@code {message}} work.</li>
     *   <li>Run {@link com.zerog.neoessentials.api.PlaceholderAPI#setPlaceholders} for any
     *       remaining {@code {neoessentials_*}} and external tokens.</li>
     *   <li>In debug mode, log any {@code {TOKEN}} tokens that are still present after
     *       resolution to help diagnose template misconfigurations.</li>
     * </ol>
     *
     * @param player    Player context for PlaceholderAPI; may be {@code null} for server-level messages
     * @param template  Raw template string (may contain {@code &} color codes)
     * @param extraVars Named variable overrides (key without braces → value); may be {@code null}
     * @return Resolved string — color codes still use {@code &} prefix for subsequent
     *         processing by {@link #coloredText(String)}
     */
    public static String resolveTemplate(
            @javax.annotation.Nullable net.minecraft.server.level.ServerPlayer player,
            String template,
            @javax.annotation.Nullable java.util.Map<String, String> extraVars) {
        if (template == null || template.isEmpty()) return template == null ? "" : template;

        String result = template;

        // ── Step 1: apply extra named vars (case-insensitive) ───────────────────
        if (extraVars != null && !extraVars.isEmpty()) {
            for (java.util.Map.Entry<String, String> entry : extraVars.entrySet()) {
                String val = entry.getValue() != null ? entry.getValue() : "";
                result = result.replaceAll(
                    "(?i)\\{" + java.util.regex.Pattern.quote(entry.getKey()) + "}",
                    java.util.regex.Matcher.quoteReplacement(val));
            }
        }

        // ── Step 2: PlaceholderAPI for remaining {neoessentials_*} etc. ─────────
        result = com.zerog.neoessentials.api.PlaceholderAPI.setPlaceholders(player, result);

        // ── Step 3: debug — log any {TOKEN} tokens still unresolved ─────────────
        if (debugMode) {
            java.util.regex.Matcher m = NAMED_PLACEHOLDER_PATTERN.matcher(result);
            java.util.List<String> unresolved = new java.util.ArrayList<>();
            while (m.find()) unresolved.add(m.group(0));
            if (!unresolved.isEmpty()) {
                LOGGER.warn("[NeoEssentials] Unresolved placeholders in template '{}': {}",
                    template.length() > 80 ? template.substring(0, 77) + "..." : template,
                    unresolved);
            }
        }

        return result;
    }

    /**
     * Apply positional arguments to a template string.
     *
     * <p>Substitution rules (in order):
     * <ol>
     *   <li>{@code ''} → literal {@code '} (MessageFormat-style escaped single-quote, kept for
     *       backward-compat with existing translation files).</li>
     *   <li>{@code %s} → first argument (legacy shorthand).</li>
     *   <li>{@code {0}}, {@code {1}}, … → corresponding element of {@code args}.</li>
     * </ol>
     * Named tokens such as {@code {HOME}}, {@code {MESSAGE}}, or
     * {@code {neoessentials_displayname}} are left untouched for later resolution by
     * {@link com.zerog.neoessentials.api.PlaceholderAPI} or
     * {@link #resolveTemplate}.
     * </p>
     */
    private static String applyArgs(String template, Object... args) {
        if (template == null) return "";
        // MessageFormat-style double-single-quote escape: '' → '
        String result = template.replace("''", "'");
        // Legacy %s → first arg
        if (args != null && args.length > 0) {
            result = result.replace("%s", args[0] != null ? args[0].toString() : "");
        }
        // Positional {0}, {1}, {2}, … substitution
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                result = result.replace("{" + i + "}", args[i] != null ? args[i].toString() : "");
            }
        }
        return result;
    }

    /**
     * Get a localized string with optional arguments.
     * Falls back to a human-readable form of the key if the key is not found.
     *
     * <p>Named placeholders (e.g. {@code {neoessentials_displayname}}, {@code {MESSAGE}}) are
     * preserved verbatim so callers can resolve them via
     * {@link com.zerog.neoessentials.api.PlaceholderAPI} after this call.
     * Positional placeholders {@code {0}}, {@code {1}}, … are replaced by the supplied
     * {@code args}.  Legacy {@code %s} tokens are treated as {@code {0}}.</p>
     */
    public static String localize(String key, Object... args) {
        loadTranslations();
        String template = translations.get(key);

        if (template == null) {
            if (debugMode) {
                LOGGER.warn("Missing translation key: {} (total keys loaded: {})", key, translations.size());
            }
            // Generate human-readable fallback from the key name instead of showing the raw key
            template = humanizeKey(key);
        }

        try {
            String result = applyArgs(template, args);
            if (debugMode) {
                LOGGER.info("localize success - Key: {}, Template: '{}', Args: {}, Result: '{}'",
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
     * Get a localized string with an explicit English fallback text.
     * Use this when you know what the English text should be in case the key is missing.
     *
     * <p>Named placeholders are preserved verbatim (see {@link #localize(String, Object...)}).</p>
     */
    public static String localize(String key, String fallback, Object... args) {
        loadTranslations();
        String template = translations.getOrDefault(key, fallback);

        if (debugMode && !translations.containsKey(key)) {
            LOGGER.warn("Missing translation key: {} — using provided fallback: '{}'", key, fallback);
        }

        try {
            return applyArgs(template, args);
        } catch (Exception e) {
            LOGGER.error("Failed to format message with fallback - Key: {}, Template: '{}', Error: {}",
                key, template, e.getMessage(), e);
            return template;
        }
    }

    /**
     * Convert a dotted translation key into a human-readable English string.
     * E.g. "commands.neoessentials.home.not_found" → "Home not found"
     */
    private static String humanizeKey(String key) {
        if (key == null || key.isEmpty()) return "";
        // Strip common prefixes
        String stripped = key;
        if (stripped.startsWith("commands.neoessentials.")) {
            stripped = stripped.substring("commands.neoessentials.".length());
        } else if (stripped.startsWith("neoessentials.")) {
            stripped = stripped.substring("neoessentials.".length());
        }
        // Replace dots and underscores with spaces, capitalize first letter
        String readable = stripped.replace('.', ' ').replace('_', ' ');
        if (!readable.isEmpty()) {
            readable = Character.toUpperCase(readable.charAt(0)) + readable.substring(1);
        }
        return readable;
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
    //noinspection unused
    @SuppressWarnings("unused")
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
     * Merge any new JAR keys into the deployed server language file without overwriting
     * existing user edits. Called after a config version bump to ensure all translation
     * keys added in a new mod build are present on disk.
     *
     * <p>Strategy:
     * <ul>
     *   <li>Keys present in the JAR but missing from the server file → added.</li>
     *   <li>Keys already on disk → left unchanged (user edits are preserved).</li>
     *   <li>File missing entirely → written fresh from the JAR.</li>
     * </ul>
     */
    public static void ensureLanguageFileUpToDate() {
        String langCode = getConfiguredLanguage();
        File serverLangFile = new File(getNeoEssentialsLangCustomDir(), langCode + ".json");
        Map<String, String> jarTranslations = buildJarTranslationsWithFallback(langCode);
        if (jarTranslations == null) {
            LOGGER.error("JAR translations are null, cannot update language file.");
            return;
        }

        Map<String, String> serverTranslations = loadServerTranslations(serverLangFile);
        if (serverTranslations == null) {
            // File missing — write from JAR (no user edits to preserve)
            updateServerLanguageFile(serverLangFile, jarTranslations);
            LOGGER.info("Language file created from JAR (was missing).");
        } else {
            // Merge: only add keys that are absent from the server file
            int added = 0;
            for (Map.Entry<String, String> entry : jarTranslations.entrySet()) {
                if (!serverTranslations.containsKey(entry.getKey())) {
                    serverTranslations.put(entry.getKey(), entry.getValue());
                    added++;
                }
            }
            if (added > 0) {
                // Bump the version so loadTranslations() doesn't re-merge on the same boot
                serverTranslations.put(LANG_VERSION_KEY, String.valueOf(CURRENT_LANG_VERSION));
                try (java.io.FileWriter fw = new java.io.FileWriter(serverLangFile)) {
                    new com.google.gson.GsonBuilder().setPrettyPrinting()
                        .disableHtmlEscaping().create().toJson(serverTranslations, fw);
                    LOGGER.info("Language file merged: {} new key(s) added (user edits preserved).", added);
                } catch (Exception ex) {
                    LOGGER.warn("Could not save merged language file: {}", ex.getMessage());
                }
            } else {
                LOGGER.debug("Language file is already up to date, no merge needed.");
            }
        }

        // Invalidate the in-memory cache so the next call to localize() picks up the updated file
        translations.clear();
        loaded = false;
        loadTranslations();
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
        String langCode = getConfiguredLanguage();
        File configRoot = getNeoEssentialsConfigRoot();
        File langDir = new File(configRoot, "neoessentials/languages/custom");
        File langFile = new File(langDir, langCode + ".json");
        logInfo("[Lang] Working directory: " + System.getProperty("user.dir"));
        logInfo("[Lang] Active language: " + langCode);
        logInfo("[Lang] Resolved language file path: " + langFile.getAbsolutePath());
        if (!langFile.exists() || langFile.length() == 0) {
            logInfo("Custom language file not found or empty: " + langFile.getAbsolutePath());
            try (InputStream in = ResourceUtil.getJarLangResource(langCode + ".json")) {
                InputStream source = in;
                if (source == null) {
                    logInfo("Language '" + langCode + "' not bundled, falling back to en_us");
                    source = ResourceUtil.getJarLangResource("en_us.json");
                }
                if (source == null) {
                    logError("Default language resource not found in JAR: data/lang/en_us.json");
                    return;
                }
                Files.createDirectories(langFile.getParentFile().toPath());
                Files.copy(source, langFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
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
    //noinspection unused
    @SuppressWarnings("unused")
    public static Component clickableSuggestion(String text, String command, String hoverText) {
        return ChatComponentUtil.createClickableSuggestion(text, command, hoverText);
    }
    
    /**
     * Create formatted balance display with interaction
     */
    //noinspection unused
    @SuppressWarnings("unused")
    public static Component balanceComponent(String playerName, double balance, String currency) {
        return ChatComponentUtil.createBalanceComponent(playerName, balance, currency);
    }
    
    /**
     * Create formatted player name with interaction
     */
    //noinspection unused
    @SuppressWarnings("unused")
    public static Component playerComponent(String playerName) {
        return ChatComponentUtil.createPlayerComponent(playerName);
    }
    
    /**
     * Create formatted permission with copy functionality
     */
    //noinspection unused
    @SuppressWarnings("unused")
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
    //noinspection unused
    @SuppressWarnings("unused") // public API
    public static Component separator(int length, char character, net.minecraft.ChatFormatting color) {
        return ChatComponentUtil.createSeparator(length, character, color);
    }
    
    /**
     * Create a progress bar
     */
    //noinspection unused
    @SuppressWarnings("unused")
    public static Component progressBar(double current, double max, int width) {
        return ChatComponentUtil.createProgressBar(current, max, width);
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
     * Utility to get the NeoEssentials custom language directory (matches CustomLanguageManager).
     * <p>Also removes the legacy 'lang' directory if it exists in the server root.
     */
    private static File getNeoEssentialsLangCustomDir() {
        // Use FMLPaths.GAMEDIR if available, else fallback to user.dir
        File langDir;
        try {
            // Try to use FMLPaths if available (Forge/NeoForge)
            Class<?> fmlPathsClass = Class.forName("net.neoforged.fml.loading.FMLPaths");
            // FMLPaths.GAMEDIR is an enum constant; we access it reflectively as a field
            //noinspection JavaReflectionMemberAccess
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
        if (!dir.delete()) {
            LOGGER.warn("MessageUtil: failed to delete: {}", dir.getAbsolutePath());
        }
    }

    /**
     * Loads a language file from the custom language directory in the NeoEssentials data folder.
     *
     * @param languageCode The language code (e.g., "en_us").
     * @return The loaded language map, or null if not found.
     */
    public static Map<String, String> loadCustomLanguageFile(String languageCode) {
        // Always use the NeoEssentials data directory for custom languages
        File customLangFile = ResourceUtil.getDataFile("languages/custom/" + languageCode + ".json");
        if (!customLangFile.exists()) {
            return null;
        }
        try (FileReader reader = new FileReader(customLangFile)) {
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, String>>() {}.getType();
            return gson.fromJson(reader, type);
        } catch (Exception e) {
            LOGGER.error("Failed to load custom language file '{}': {}", languageCode, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Loads all available custom language files from the NeoEssentials data directory.
     */
    //noinspection unused
    @SuppressWarnings("unused")
    public static Map<String, Map<String, String>> loadAllCustomLanguages() {
        Map<String, Map<String, String>> languages = new HashMap<>();
        File langDir = ResourceUtil.getDataFile("languages/custom");
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
