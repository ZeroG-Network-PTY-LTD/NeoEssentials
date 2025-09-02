package com.zerog.neoessentials.commands.language;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.zerog.neoessentials.localization.LanguageManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Language Command for NeoEssentials
 * Allows players to change their language preferences and view available languages.
 * 
 * Commands:
 * - /language - Show current language and available options
 * - /language set <language> - Set the player's language
 * - /language list - Show all available languages
 * - /language info - Show language system information
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class LanguageCommand {
    
    /**
     * Register the language command
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("language")
            .executes(LanguageCommand::showCurrentLanguage)
            .then(Commands.literal("set")
                .then(Commands.argument("language", StringArgumentType.string())
                    .suggests(LanguageCommand::suggestLanguages)
                    .executes(LanguageCommand::setLanguage)))
            .then(Commands.literal("list")
                .executes(LanguageCommand::listLanguages))
            .then(Commands.literal("info")
                .executes(LanguageCommand::showLanguageInfo))
        );
    }
    
    /**
     * Show current language information
     */
    private static int showCurrentLanguage(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by players!")
                .withStyle(ChatFormatting.RED));
            return 0;
        }
        
        LanguageManager languageManager = LanguageManager.getInstance();
        
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
                    .withStyle(ChatFormatting.GREEN)));
            
            player.sendSystemMessage(Component.literal("Available Languages: ")
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(String.valueOf(availableCount))
                    .withStyle(ChatFormatting.WHITE)));
            
            player.sendSystemMessage(Component.literal("Use '/language set <language>' to change your language")
                .withStyle(ChatFormatting.GRAY));
            
            player.sendSystemMessage(Component.literal("Use '/language list' to see all available languages")
                .withStyle(ChatFormatting.GRAY));
            
            return Command.SINGLE_SUCCESS;
            
        } catch (Exception e) {
            player.sendSystemMessage(Component.literal("Error retrieving language information: " + e.getMessage())
                .withStyle(ChatFormatting.RED));
            return 0;
        }
    }
    
    /**
     * Set player's language
     */
    private static int setLanguage(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by players!")
                .withStyle(ChatFormatting.RED));
            return 0;
        }
        
        String language = StringArgumentType.getString(context, "language");
        LanguageManager languageManager = LanguageManager.getInstance();
        
        try {
            // Check if the language is available
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
            String message = languageManager.getMessage(player, "neoessentials.language.changed", 
                "LANGUAGE", displayName);
            
            player.sendSystemMessage(Component.literal(message)
                .withStyle(ChatFormatting.GREEN));
            
            return Command.SINGLE_SUCCESS;
            
        } catch (Exception e) {
            player.sendSystemMessage(Component.literal("Error setting language: " + e.getMessage())
                .withStyle(ChatFormatting.RED));
            return 0;
        }
    }
    
    /**
     * List all available languages
     */
    private static int listLanguages(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by players!")
                .withStyle(ChatFormatting.RED));
            return 0;
        }
        
        LanguageManager languageManager = LanguageManager.getInstance();
        
        try {
            player.sendSystemMessage(Component.literal("=== Available Languages ===")
                .withStyle(ChatFormatting.GOLD));
            
            String currentLanguage = languageManager.getPlayerLocale(player);
            
            for (String language : languageManager.getAvailableLanguages()) {
                String displayName = languageManager.getLanguageDisplayName(language);
                
                MutableComponent languageComponent = Component.literal("* ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(displayName + " (" + language + ")")
                        .withStyle(language.equals(currentLanguage) ? 
                                 ChatFormatting.GREEN : ChatFormatting.WHITE));
                
                if (language.equals(currentLanguage)) {
                    languageComponent = languageComponent.append(Component.literal(" <- Current")
                        .withStyle(ChatFormatting.GOLD));
                }
                
                player.sendSystemMessage(languageComponent);
            }
            
            player.sendSystemMessage(Component.literal("Use '/language set <language>' to change your language")
                .withStyle(ChatFormatting.GRAY));
            
            return Command.SINGLE_SUCCESS;
            
        } catch (Exception e) {
            player.sendSystemMessage(Component.literal("Error listing languages: " + e.getMessage())
                .withStyle(ChatFormatting.RED));
            return 0;
        }
    }
    
    /**
     * Show detailed language system information
     */
    private static int showLanguageInfo(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by players!")
                .withStyle(ChatFormatting.RED));
            return 0;
        }
        
        LanguageManager languageManager = LanguageManager.getInstance();
        
        try {
            Map<String, Object> stats = languageManager.getLanguageStats();
            
            player.sendSystemMessage(Component.literal("=== Language System Information ===")
                .withStyle(ChatFormatting.GOLD));
            
            player.sendSystemMessage(Component.literal("Available Languages: ")
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(stats.get("available_languages").toString())
                    .withStyle(ChatFormatting.WHITE)));
            
            player.sendSystemMessage(Component.literal("Default Language: ")
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(stats.get("default_language").toString())
                    .withStyle(ChatFormatting.WHITE)));
            
            player.sendSystemMessage(Component.literal("Players with Custom Locales: ")
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(stats.get("player_locales").toString())
                    .withStyle(ChatFormatting.WHITE)));
            
            return Command.SINGLE_SUCCESS;
            
        } catch (Exception e) {
            player.sendSystemMessage(Component.literal("Error retrieving language info: " + e.getMessage())
                .withStyle(ChatFormatting.RED));
            return 0;
        }
    }
    
    /**
     * Provide language suggestions for tab completion
     */
    private static CompletableFuture<Suggestions> suggestLanguages(CommandContext<CommandSourceStack> context, 
                                                                  SuggestionsBuilder builder) {
        try {
            LanguageManager languageManager = LanguageManager.getInstance();
            
            for (String language : languageManager.getAvailableLanguages()) {
                if (language.toLowerCase().startsWith(builder.getRemaining().toLowerCase())) {
                    builder.suggest(language);
                }
            }
            
        } catch (Exception e) {
            // If there's an error, just return empty suggestions
        }
        
        return builder.buildFuture();
    }
}
