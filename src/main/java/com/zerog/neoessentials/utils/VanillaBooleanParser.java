package com.zerog.neoessentials.utils;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.zerog.neoessentials.NeoEssentials;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Vanilla-compatible wrapper for string-to-boolean conversion
 * This class uses standard Minecraft StringArgumentType and performs conversion after parsing
 * No custom argument type is used, making it compatible with vanilla clients
 */
public class VanillaBooleanParser {

    private static final List<String> BOOL_OPTIONS = Arrays.asList("on", "off", "true", "false");
    private static final SimpleCommandExceptionType ERROR = new SimpleCommandExceptionType(
            Component.literal("Expected boolean value (on/off/true/false)")
    );

    /**
     * Get a vanilla string argument for boolean values
     * @return A standard string argument type
     */
    public static StringArgumentType argument() {
        // Use the standard string argument type
        return StringArgumentType.word();
    }
    
    /**
     * Get a suggestion provider for boolean options
     * @return A suggestion provider for on/off/true/false
     */
    public static SuggestionProvider<CommandSourceStack> booleanSuggestions() {
        return (context, builder) -> {
            return SharedSuggestionProvider.suggest(BOOL_OPTIONS, builder);
        };
    }
    
    /**
     * Parse a string into a boolean value
     * @param context The command context
     * @param name The argument name
     * @return The boolean value
     * @throws CommandSyntaxException If the value is not a valid boolean
     */
    public static boolean getBoolean(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        String value = StringArgumentType.getString(context, name).toLowerCase();
        
        if (value.equals("true") || value.equals("on")) {
            return true;
        } else if (value.equals("false") || value.equals("off")) {
            return false;
        } else {
            throw ERROR.create();
        }
    }
    
    /**
     * Try to parse a string value to boolean
     * @param value The string value
     * @return The boolean value, or null if invalid
     */
    public static Boolean tryParse(String value) {
        String lowered = value.toLowerCase();
        if (lowered.equals("true") || lowered.equals("on")) {
            return Boolean.TRUE;
        } else if (lowered.equals("false") || lowered.equals("off")) {
            return Boolean.FALSE; 
        }
        return null;
    }
}
