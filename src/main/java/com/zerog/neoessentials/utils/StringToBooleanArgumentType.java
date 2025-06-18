package com.zerog.neoessentials.utils;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Argument type for boolean values using strings "on"/"off" or "true"/"false"
 */
public class StringToBooleanArgumentType implements ArgumentType<Boolean> {

    private static final List<String> EXAMPLES = Arrays.asList("on", "off", "true", "false");
    private static final SimpleCommandExceptionType ERROR = new SimpleCommandExceptionType(Component.literal("Expected boolean value (on/off/true/false)"));

    private StringToBooleanArgumentType() {
    }

    /**
     * Get a new StringToBooleanArgumentType instance
     * 
     * @return A new StringToBooleanArgumentType
     */
    public static StringToBooleanArgumentType stringToBoolean() {
        return new StringToBooleanArgumentType();
    }

    /**
     * Get the boolean value from the given context and name
     * 
     * @param context The command context
     * @param name The argument name
     * @return The boolean value
     */
    public static boolean getBoolean(final CommandContext<?> context, final String name) {
        return context.getArgument(name, Boolean.class);
    }

    @Override
    public Boolean parse(final StringReader reader) throws CommandSyntaxException {
        final String value = reader.readUnquotedString().toLowerCase();
        
        if (value.equals("true") || value.equals("on")) {
            return true;
        } else if (value.equals("false") || value.equals("off")) {
            return false;
        } else {
            throw ERROR.create();
        }
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
        for (String option : Arrays.asList("on", "off", "true", "false")) {
            if (option.startsWith(builder.getRemaining().toLowerCase())) {
                builder.suggest(option);
            }
        }
        return builder.buildFuture();
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
