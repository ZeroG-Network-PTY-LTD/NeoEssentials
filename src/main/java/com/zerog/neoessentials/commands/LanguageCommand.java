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
                MessageUtil.sendMessage(player, manager.getMessage(player, "neoessentials.language.info.header"));
                MessageUtil.sendMessage(player, manager.getMessage(player, "neoessentials.language.info.current", currentLocale));
                MessageUtil.sendMessage(player, manager.getMessage(player, "neoessentials.language.info.available", availableLanguages.size()));
                MessageUtil.sendMessage(player, manager.getMessage(player, "neoessentials.language.info.default", manager.getDefaultLanguage()));
                MessageUtil.sendMessage(player, manager.getMessage(player, "neoessentials.language.info.list_hint"));
            } else {
                source.sendSuccess(() -> Component.literal(
                    manager.getMessage("en_US", "neoessentials.language.info.active", manager.getAvailableLanguages().size())
                ), false);
            }
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error showing language info: " + e.getMessage(), e);
            LanguageManager manager = LanguageManager.getInstance();
            context.getSource().sendFailure(Component.literal(manager.getMessage("en_US", "neoessentials.language.error.show_info")));
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
                source.sendFailure(Component.literal(manager.getMessage("en_US", "neoessentials.language.set.only_players")));
                return 0;
            }
            Set<String> availableLanguages = manager.getAvailableLanguages();
            if (!availableLanguages.contains(language)) {
                MessageUtil.sendMessage(player, manager.getMessage(player, "neoessentials.language.set.not_available", language));
                MessageUtil.sendMessage(player, manager.getMessage(player, "neoessentials.language.set.available_list", String.join(", ", availableLanguages)));
                return 0;
            }
            manager.setPlayerLocale(player, language);
            String successMessage = manager.getMessage(player, "command.language.changed", "LANGUAGE", language);
            final String finalMessage;
            if (successMessage.contains("[Missing:")) {
                finalMessage = manager.getMessage(player, "neoessentials.language.set.success_fallback", language);
            } else {
                finalMessage = successMessage;
            }
            MessageUtil.sendMessage(player, finalMessage);
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error setting player language: " + e.getMessage(), e);
            LanguageManager manager = LanguageManager.getInstance();
            context.getSource().sendFailure(Component.literal(manager.getMessage("en_US", "neoessentials.language.error.set")));
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
            ServerPlayer player = source.getEntity() instanceof ServerPlayer p ? p : null;
            String current = player != null ? manager.getPlayerLocale(player) : "";
            if (player != null) {
                MessageUtil.sendMessage(player, manager.getMessage(player, "neoessentials.language.list.header"));
                for (String language : languages) {
                    String indicator = language.equals(current) ? "► " : "- ";
                    String languageName = getLanguageDisplayName(language);
                    MessageUtil.sendMessage(player, manager.getMessage(player, "neoessentials.language.list.entry", indicator, languageName, language));
                }
                MessageUtil.sendMessage(player, manager.getMessage(player, "neoessentials.language.list.set_hint"));
            } else {
                source.sendSuccess(() -> Component.literal(manager.getMessage("en_US", "neoessentials.language.list.header")), false);
            }
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error listing languages: " + e.getMessage(), e);
            context.getSource().sendFailure(Component.literal(LanguageManager.getInstance().getMessage("en_US", "neoessentials.language.error.list")));
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
            source.sendSuccess(() -> Component.literal(manager.getMessage("en_US", "neoessentials.language.reload.success")), false);
            source.sendSuccess(() -> Component.literal(manager.getMessage("en_US", "neoessentials.language.reload.loaded", manager.getAvailableLanguages().size())), false);
            LOGGER.info("Language files reloaded by {}", source.getTextName());
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error reloading languages: " + e.getMessage(), e);
            context.getSource().sendFailure(Component.literal(LanguageManager.getInstance().getMessage("en_US", "neoessentials.language.error.reload", e.getMessage())));
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
            ServerPlayer player = source.getEntity() instanceof ServerPlayer p ? p : null;
            if (player != null) {
                MessageUtil.sendMessage(player, manager.getMessage(player, "neoessentials.language.stats.header"));
                MessageUtil.sendMessage(player, manager.getMessage(player, "neoessentials.language.stats.available", stats.get("available_languages")));
                MessageUtil.sendMessage(player, manager.getMessage(player, "neoessentials.language.stats.default", stats.get("default_language")));
                MessageUtil.sendMessage(player, manager.getMessage(player, "neoessentials.language.stats.player_locales", stats.get("player_locales")));
                @SuppressWarnings("unchecked")
                Map<String, Integer> messageCounts = (Map<String, Integer>) stats.get("message_counts");
                MessageUtil.sendMessage(player, manager.getMessage(player, "neoessentials.language.stats.message_counts_header"));
                messageCounts.forEach((language, count) -> {
                    MessageUtil.sendMessage(player, manager.getMessage(player, "neoessentials.language.stats.message_count_entry", language, count));
                });
            } else {
                source.sendSuccess(() -> Component.literal(manager.getMessage("en_US", "neoessentials.language.stats.header")), false);
            }
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error showing language stats: " + e.getMessage(), e);
            context.getSource().sendFailure(Component.literal(LanguageManager.getInstance().getMessage("en_US", "neoessentials.language.error.stats")));
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
            
            if (source.getEntity() instanceof ServerPlayer player) {
                MessageUtil.sendMessage(player, manager.getMessage(player, "neoessentials.language.test.key", key));
                String playerLocale = manager.getPlayerLocale(player);
                String message = manager.getRawMessage(playerLocale, key);
                MessageUtil.sendMessage(player, manager.getMessage(player, "neoessentials.language.test.your_language", playerLocale, message));
                // Show in default language
                String defaultMessage = manager.getRawMessage(manager.getDefaultLanguage(), key);
                MessageUtil.sendMessage(player, manager.getMessage(player, "neoessentials.language.test.default", manager.getDefaultLanguage(), defaultMessage));
                // Show in other languages
                Set<String> languages = manager.getAvailableLanguages();
                if (languages.size() > 2) {
                    MessageUtil.sendMessage(player, manager.getMessage(player, "neoessentials.language.test.other_header"));
                    languages.stream()
                        .filter(lang -> !lang.equals(manager.getDefaultLanguage()))
                        .limit(3)
                        .forEach(lang -> {
                            String msg = manager.getRawMessage(lang, key);
                            MessageUtil.sendMessage(player, manager.getMessage(player, "neoessentials.language.test.other_entry", lang, msg));
                        });
                }
            } else {
                source.sendSuccess(() -> Component.literal(manager.getMessage("en_US", "neoessentials.language.test.key", key)), false);
            }
            
            return 1;
            
        } catch (Exception e) {
            LOGGER.error("Error testing language key: " + e.getMessage(), e);
            context.getSource().sendFailure(Component.literal(LanguageManager.getInstance().getMessage("en_US", "neoessentials.language.error.test")));
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
