package com.zerog.neoessentials.commands.language;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.zerog.neoessentials.localization.EnhancedLanguageManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Enhanced Language Command for NeoEssentials
 * Phase 4: Complete Language System Management
 * 
 * Commands:
 * - /language - Shows current language information
 * - /language set <language> - Set your language
 * - /language list - List all available languages
 * - /language reload - Reload language files (admin only)
 * - /language stats - Show language system statistics (admin only)
 * - /language test <key> - Test a language key (admin only)
 * 
 * @author ZeroG
 * @since 2.0.0 (Phase 4 Enhanced)
 */
public class LanguageCommand {
    
    /**
     * Register the enhanced language command
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("language")
            .requires(source -> source.hasPermission(2) || 
                     source.isPlayer() && source.getPlayer() != null)
            .executes(LanguageCommand::showLanguageInfo)
            .then(Commands.literal("set")
                .then(Commands.argument("language", StringArgumentType.word())
                    .suggests(LanguageCommand::suggestLanguages)
                    .executes(LanguageCommand::setLanguage)))
            .then(Commands.literal("list")
                .executes(LanguageCommand::listLanguages))
            .then(Commands.literal("reload")
                .requires(source -> source.hasPermission(3))
                .executes(LanguageCommand::reloadLanguages))
            .then(Commands.literal("stats")
                .requires(source -> source.hasPermission(3))
                .executes(LanguageCommand::showStats))
            .then(Commands.literal("test")
                .requires(source -> source.hasPermission(3))
                .then(Commands.argument("key", StringArgumentType.greedyString())
                    .executes(LanguageCommand::testLanguageKey))));
    }
    
    /**
     * Show current language information
     */
    private static int showLanguageInfo(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        if (!source.isPlayer()) {
            source.sendFailure(Component.literal("This command can only be used by players!")
                .withStyle(ChatFormatting.RED));
            return 0;
        }
        
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by players!")
                .withStyle(ChatFormatting.RED));
            return 0;
        }
        
        EnhancedLanguageManager languageManager = EnhancedLanguageManager.getInstance();
        
        try {
            String currentLanguage = languageManager.getPlayerLocale(player);
            String displayName = languageManager.getLanguageDisplayName(currentLanguage);
            int availableCount = languageManager.getAvailableLanguages().size();
            
            // Send current language info
            player.sendSystemMessage(Component.literal("=== Language Information ===")
                .withStyle(ChatFormatting.GOLD));
            
            player.sendSystemMessage(Component.literal("Current Language: ")
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(displayName + " (" + currentLanguage + ")")
                    .withStyle(ChatFormatting.WHITE)));
            
            player.sendSystemMessage(Component.literal("Available Languages: ")
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(String.valueOf(availableCount))
                    .withStyle(ChatFormatting.WHITE)));
            
            player.sendSystemMessage(Component.literal("Use '/language list' to see all languages")
                .withStyle(ChatFormatting.GRAY));
            
            player.sendSystemMessage(Component.literal("Use '/language set <language>' to change your language")
                .withStyle(ChatFormatting.GRAY));
            
            return Command.SINGLE_SUCCESS;
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error retrieving language information: " + e.getMessage())
                .withStyle(ChatFormatting.RED));
            return 0;
        }
    }
    
    /**
     * Set player's language
     */
    private static int setLanguage(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        if (!source.isPlayer()) {
            source.sendFailure(Component.literal("This command can only be used by players!")
                .withStyle(ChatFormatting.RED));
            return 0;
        }
        
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by players!")
                .withStyle(ChatFormatting.RED));
            return 0;
        }
        
        String language = StringArgumentType.getString(context, "language");
        EnhancedLanguageManager languageManager = EnhancedLanguageManager.getInstance();
        
        try {
            // Check if language is available
            if (!languageManager.getAvailableLanguages().contains(language)) {
                player.sendSystemMessage(Component.literal("Language '" + language + "' is not available!")
                    .withStyle(ChatFormatting.RED));
                
                player.sendSystemMessage(Component.literal("Use '/language list' to see available languages")
                    .withStyle(ChatFormatting.GRAY));
                return 0;
            }
            
            // Set the language
            languageManager.setPlayerLocale(player, language);
            String displayName = languageManager.getLanguageDisplayName(language);
            
            // Send confirmation in the new language
            String message = languageManager.getMessage(player, "language.changed", 
                "LANGUAGE", displayName);
            
            player.sendSystemMessage(Component.literal(message)
                .withStyle(ChatFormatting.GREEN));
            
            return Command.SINGLE_SUCCESS;
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error setting language: " + e.getMessage())
                .withStyle(ChatFormatting.RED));
            return 0;
        }
    }
    
    /**
     * List all available languages
     */
    private static int listLanguages(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        if (!source.isPlayer()) {
            source.sendFailure(Component.literal("This command can only be used by players!")
                .withStyle(ChatFormatting.RED));
            return 0;
        }
        
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by players!")
                .withStyle(ChatFormatting.RED));
            return 0;
        }
        
        EnhancedLanguageManager languageManager = EnhancedLanguageManager.getInstance();
        
        try {
            player.sendSystemMessage(Component.literal("=== Available Languages ===")
                .withStyle(ChatFormatting.GOLD));
            
            String currentLanguage = languageManager.getPlayerLocale(player);
            
            for (String language : languageManager.getAvailableLanguages()) {
                String displayName = languageManager.getLanguageDisplayName(language);
                
                MutableComponent languageComponent = Component.literal("• ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(displayName + " (" + language + ")")
                        .withStyle(language.equals(currentLanguage) ? 
                                 ChatFormatting.GREEN : ChatFormatting.WHITE));
                
                if (language.equals(currentLanguage)) {
                    languageComponent = languageComponent.append(Component.literal(" ← Current")
                        .withStyle(ChatFormatting.GOLD));
                }
                
                player.sendSystemMessage(languageComponent);
            }
            
            player.sendSystemMessage(Component.literal("Use '/language set <language>' to change your language")
                .withStyle(ChatFormatting.GRAY));
            
            return Command.SINGLE_SUCCESS;
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error listing languages: " + e.getMessage())
                .withStyle(ChatFormatting.RED));
            return 0;
        }
    }
    
    /**
     * Reload language files (admin only)
     */
    private static int reloadLanguages(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        try {
            EnhancedLanguageManager languageManager = EnhancedLanguageManager.getInstance();
            languageManager.reloadLanguages();
            
            source.sendSuccess(() -> Component.literal("Language files reloaded successfully!")
                .withStyle(ChatFormatting.GREEN), true);
            
            source.sendSuccess(() -> Component.literal("Loaded " + 
                languageManager.getAvailableLanguages().size() + " language files")
                .withStyle(ChatFormatting.GRAY), false);
            
            return Command.SINGLE_SUCCESS;
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error reloading languages: " + e.getMessage())
                .withStyle(ChatFormatting.RED));
            return 0;
        }
    }
    
    /**
     * Show language system statistics (admin only)
     */
    private static int showStats(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        try {
            EnhancedLanguageManager languageManager = EnhancedLanguageManager.getInstance();
            Map<String, Object> stats = languageManager.getLanguageStats();
            
            source.sendSuccess(() -> Component.literal("=== Language System Statistics ===")
                .withStyle(ChatFormatting.GOLD), false);
            
            source.sendSuccess(() -> Component.literal("Available Languages: " + 
                stats.get("available_languages"))
                .withStyle(ChatFormatting.YELLOW), false);
            
            source.sendSuccess(() -> Component.literal("Default Language: " + 
                stats.get("default_language"))
                .withStyle(ChatFormatting.YELLOW), false);
            
            source.sendSuccess(() -> Component.literal("Players with Custom Locales: " + 
                stats.get("player_locales"))
                .withStyle(ChatFormatting.YELLOW), false);
            
            source.sendSuccess(() -> Component.literal("Auto-reload Enabled: " + 
                stats.get("auto_reload_enabled"))
                .withStyle(ChatFormatting.YELLOW), false);
            
            // Show message counts per language
            @SuppressWarnings("unchecked")
            Map<String, Integer> messageCounts = (Map<String, Integer>) stats.get("message_counts");
            
            source.sendSuccess(() -> Component.literal("Message Counts per Language:")
                .withStyle(ChatFormatting.AQUA), false);
            
            for (Map.Entry<String, Integer> entry : messageCounts.entrySet()) {
                String language = entry.getKey();
                Integer count = entry.getValue();
                String displayName = languageManager.getLanguageDisplayName(language);
                
                source.sendSuccess(() -> Component.literal("  " + displayName + " (" + 
                    language + "): " + count + " messages")
                    .withStyle(ChatFormatting.WHITE), false);
            }
            
            return Command.SINGLE_SUCCESS;
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error getting language stats: " + e.getMessage())
                .withStyle(ChatFormatting.RED));
            return 0;
        }
    }
    
    /**
     * Test a language key (admin only)
     */
    private static int testLanguageKey(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String key = StringArgumentType.getString(context, "key");
        
        try {
            EnhancedLanguageManager languageManager = EnhancedLanguageManager.getInstance();
            
            if (source.isPlayer()) {
                ServerPlayer player = source.getPlayer();
                if (player != null) {
                    String message = languageManager.getMessage(player, key);
                    
                    player.sendSystemMessage(Component.literal("Testing key: " + key)
                        .withStyle(ChatFormatting.GOLD));
                    player.sendSystemMessage(Component.literal("Result: " + message)
                        .withStyle(ChatFormatting.WHITE));
                }
            } else {
                String message = languageManager.getMessage(languageManager.getDefaultLanguage(), key);
                
                source.sendSuccess(() -> Component.literal("Testing key: " + key)
                    .withStyle(ChatFormatting.GOLD), false);
                source.sendSuccess(() -> Component.literal("Result: " + message)
                    .withStyle(ChatFormatting.WHITE), false);
            }
            
            return Command.SINGLE_SUCCESS;
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error testing language key: " + e.getMessage())
                .withStyle(ChatFormatting.RED));
            return 0;
        }
    }
    
    /**
     * Suggest available languages for tab completion
     */
    private static CompletableFuture<Suggestions> suggestLanguages(
            CommandContext<CommandSourceStack> context, 
            SuggestionsBuilder builder) {
        
        try {
            EnhancedLanguageManager languageManager = EnhancedLanguageManager.getInstance();
            
            for (String language : languageManager.getAvailableLanguages()) {
                builder.suggest(language);
            }
            
        } catch (Exception e) {
            // If there's an error, don't crash - just provide no suggestions
        }
        
        return builder.buildFuture();
    }
}
