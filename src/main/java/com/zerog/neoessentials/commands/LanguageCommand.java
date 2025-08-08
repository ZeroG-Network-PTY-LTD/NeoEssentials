package com.zerog.neoessentials.commands;

import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.zerog.neoessentials.localization.LanguageManager;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

/**
 * Language management commands for NeoEssentials
 * Provides commands for language switching, management, and testing
 * 
 * Commands:
 * - /language - Main language command
 * - /language set <language> - Set player's language
 * - /language list - List available languages
 * - /language reload - Reload language files
 * - /language info - Show language system information
 * - /language test <key> - Test a language key
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class LanguageCommand {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(LanguageCommand.class);
    
    private static final SuggestionProvider<CommandSourceStack> LANGUAGE_SUGGESTIONS = 
        (context, builder) -> {
            LanguageManager manager = LanguageManager.getInstance();
            Set<String> languages = manager.getAvailableLanguages();
            return SharedSuggestionProvider.suggest(languages, builder);
        };
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("language")
                .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.MODERATION_BASIC))
                .executes(LanguageCommand::showLanguageInfo)
                .then(Commands.literal("set")
                    .then(Commands.argument("language", StringArgumentType.string())
                        .suggests(LANGUAGE_SUGGESTIONS)
                        .executes(LanguageCommand::setPlayerLanguage)))
                .then(Commands.literal("list")
                    .executes(LanguageCommand::listLanguages))
                .then(Commands.literal("reload")
                    .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.ADMIN_BASIC))
                    .executes(LanguageCommand::reloadLanguages))
                .then(Commands.literal("info")
                    .executes(LanguageCommand::showLanguageStats))
                .then(Commands.literal("test")
                    .then(Commands.argument("key", StringArgumentType.string())
                        .executes(LanguageCommand::testLanguageKey)))
        );
        
        // Alias commands
        dispatcher.register(
            Commands.literal("lang")
                .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.MODERATION_BASIC))
                .redirect(dispatcher.getRoot().getChild("language"))
        );
        
        LOGGER.info("Language commands registered");
    }
    
    /**
     * Show basic language information
     */
    private static int showLanguageInfo(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            LanguageManager manager = LanguageManager.getInstance();
            
            if (source.getEntity() instanceof ServerPlayer player) {
                String currentLocale = manager.getPlayerLocale(player);
                Set<String> availableLanguages = manager.getAvailableLanguages();
                
                source.sendSuccess(() -> Component.literal(
                    MessageUtil.translateColorCodes("&6&l=== Language System ===")
                ), false);
                
                source.sendSuccess(() -> Component.literal(
                    MessageUtil.translateColorCodes("&eCurrent Language: &f" + currentLocale)
                ), false);
                
                source.sendSuccess(() -> Component.literal(
                    MessageUtil.translateColorCodes("&eAvailable Languages: &f" + availableLanguages.size())
                ), false);
                
                source.sendSuccess(() -> Component.literal(
                    MessageUtil.translateColorCodes("&eDefault Language: &f" + manager.getDefaultLanguage())
                ), false);
                
                source.sendSuccess(() -> Component.literal(
                    MessageUtil.translateColorCodes("&7Use '/language list' to see all languages")
                ), false);
                
            } else {
                source.sendSuccess(() -> Component.literal(
                    MessageUtil.translateColorCodes("&6Language system is active with " + 
                                                  manager.getAvailableLanguages().size() + " languages")
                ), false);
            }
            
            return 1;
            
        } catch (Exception e) {
            LOGGER.error("Error showing language info: " + e.getMessage(), e);
            context.getSource().sendFailure(Component.literal("Error showing language information"));
            return 0;
        }
    }
    
    /**
     * Set a player's language
     */
    private static int setPlayerLanguage(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            String language = StringArgumentType.getString(context, "language");
            LanguageManager manager = LanguageManager.getInstance();
            
            if (!(source.getEntity() instanceof ServerPlayer player)) {
                source.sendFailure(Component.literal("This command can only be used by players"));
                return 0;
            }
            
            Set<String> availableLanguages = manager.getAvailableLanguages();
            if (!availableLanguages.contains(language)) {
                source.sendFailure(Component.literal(
                    MessageUtil.translateColorCodes("&cLanguage '" + language + "' not available!")
                ));
                source.sendSuccess(() -> Component.literal(
                    MessageUtil.translateColorCodes("&7Available: " + String.join(", ", availableLanguages))
                ), false);
                return 0;
            }
            
            manager.setPlayerLocale(player, language);
            
            // Send success message in the new language
            String successMessage = manager.getMessage(player, "command.language.changed", 
                                                     "LANGUAGE", language);
            
            // Fallback if message key doesn't exist
            final String finalMessage;
            if (successMessage.contains("[Missing:")) {
                finalMessage = "&aLanguage changed to " + language + "!";
            } else {
                finalMessage = successMessage;
            }
            
            source.sendSuccess(() -> Component.literal(
                MessageUtil.translateColorCodes(finalMessage)
            ), false);
            
            return 1;
            
        } catch (Exception e) {
            LOGGER.error("Error setting player language: " + e.getMessage(), e);
            context.getSource().sendFailure(Component.literal("Error setting language"));
            return 0;
        }
    }
    
    /**
     * List all available languages
     */
    private static int listLanguages(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            LanguageManager manager = LanguageManager.getInstance();
            Set<String> languages = manager.getAvailableLanguages();
            
            source.sendSuccess(() -> Component.literal(
                MessageUtil.translateColorCodes("&6&l=== Available Languages ===")
            ), false);
            
            String current = "";
            if (source.getEntity() instanceof ServerPlayer player) {
                current = manager.getPlayerLocale(player);
            }
            
            for (String language : languages) {
                String indicator = language.equals(current) ? "&a► " : "&7- ";
                String languageName = getLanguageDisplayName(language);
                
                source.sendSuccess(() -> Component.literal(
                    MessageUtil.translateColorCodes(indicator + languageName + " &8(" + language + ")")
                ), false);
            }
            
            source.sendSuccess(() -> Component.literal(
                MessageUtil.translateColorCodes("&7Use '/language set <language>' to change your language")
            ), false);
            
            return 1;
            
        } catch (Exception e) {
            LOGGER.error("Error listing languages: " + e.getMessage(), e);
            context.getSource().sendFailure(Component.literal("Error listing languages"));
            return 0;
        }
    }
    
    /**
     * Reload language files
     */
    private static int reloadLanguages(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            LanguageManager manager = LanguageManager.getInstance();
            
            manager.reloadLanguages();
            
            source.sendSuccess(() -> Component.literal(
                MessageUtil.translateColorCodes("&aLanguage files reloaded successfully!")
            ), false);
            
            source.sendSuccess(() -> Component.literal(
                MessageUtil.translateColorCodes("&7Loaded " + manager.getAvailableLanguages().size() + " languages")
            ), false);
            
            LOGGER.info("Language files reloaded by {}", source.getTextName());
            return 1;
            
        } catch (Exception e) {
            LOGGER.error("Error reloading languages: " + e.getMessage(), e);
            context.getSource().sendFailure(Component.literal("Error reloading languages: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Show detailed language statistics
     */
    private static int showLanguageStats(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            LanguageManager manager = LanguageManager.getInstance();
            Map<String, Object> stats = manager.getLanguageStats();
            
            source.sendSuccess(() -> Component.literal(
                MessageUtil.translateColorCodes("&6&l=== Language System Statistics ===")
            ), false);
            
            source.sendSuccess(() -> Component.literal(
                MessageUtil.translateColorCodes("&eAvailable Languages: &f" + stats.get("available_languages"))
            ), false);
            
            source.sendSuccess(() -> Component.literal(
                MessageUtil.translateColorCodes("&eDefault Language: &f" + stats.get("default_language"))
            ), false);
            
            source.sendSuccess(() -> Component.literal(
                MessageUtil.translateColorCodes("&ePlayers with Custom Locales: &f" + stats.get("player_locales"))
            ), false);
            
            @SuppressWarnings("unchecked")
            Map<String, Integer> messageCounts = (Map<String, Integer>) stats.get("message_counts");
            
            source.sendSuccess(() -> Component.literal(
                MessageUtil.translateColorCodes("&eMessage Counts per Language:")
            ), false);
            
            messageCounts.forEach((language, count) -> {
                source.sendSuccess(() -> Component.literal(
                    MessageUtil.translateColorCodes("&7- " + language + ": &f" + count + " messages")
                ), false);
            });
            
            return 1;
            
        } catch (Exception e) {
            LOGGER.error("Error showing language stats: " + e.getMessage(), e);
            context.getSource().sendFailure(Component.literal("Error showing language statistics"));
            return 0;
        }
    }
    
    /**
     * Test a language key
     */
    private static int testLanguageKey(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            String key = StringArgumentType.getString(context, "key");
            LanguageManager manager = LanguageManager.getInstance();
            
            source.sendSuccess(() -> Component.literal(
                MessageUtil.translateColorCodes("&6Testing language key: &f" + key)
            ), false);
            
            if (source.getEntity() instanceof ServerPlayer player) {
                String playerLocale = manager.getPlayerLocale(player);
                String message = manager.getRawMessage(playerLocale, key);
                
                source.sendSuccess(() -> Component.literal(
                    MessageUtil.translateColorCodes("&eYour language (" + playerLocale + "): &f" + message)
                ), false);
            }
            
            // Show in default language
            String defaultMessage = manager.getRawMessage(manager.getDefaultLanguage(), key);
            source.sendSuccess(() -> Component.literal(
                MessageUtil.translateColorCodes("&eDefault (" + manager.getDefaultLanguage() + "): &f" + defaultMessage)
            ), false);
            
            // Show in other languages
            Set<String> languages = manager.getAvailableLanguages();
            if (languages.size() > 2) {
                source.sendSuccess(() -> Component.literal(
                    MessageUtil.translateColorCodes("&eOther languages:")
                ), false);
                
                languages.stream()
                    .filter(lang -> !lang.equals(manager.getDefaultLanguage()))
                    .limit(3)
                    .forEach(lang -> {
                        String msg = manager.getRawMessage(lang, key);
                        source.sendSuccess(() -> Component.literal(
                            MessageUtil.translateColorCodes("&7- " + lang + ": &f" + msg)
                        ), false);
                    });
            }
            
            return 1;
            
        } catch (Exception e) {
            LOGGER.error("Error testing language key: " + e.getMessage(), e);
            context.getSource().sendFailure(Component.literal("Error testing language key"));
            return 0;
        }
    }
    
    /**
     * Get a human-readable display name for a language code
     */
    private static String getLanguageDisplayName(String languageCode) {
        return switch (languageCode) {
            case "en_US" -> "English (US)";
            case "es_ES" -> "Español (España)";
            case "fr_FR" -> "Français (France)";
            case "de_DE" -> "Deutsch (Deutschland)";
            case "it_IT" -> "Italiano (Italia)";
            case "pt_BR" -> "Português (Brasil)";
            case "ru_RU" -> "Русский (Россия)";
            case "ja_JP" -> "日本語 (日本)";
            case "ko_KR" -> "한국어 (대한민국)";
            case "zh_CN" -> "中文 (简体)";
            case "zh_TW" -> "中文 (繁體)";
            case "nl_NL" -> "Nederlands (Nederland)";
            default -> languageCode;
        };
    }
}
