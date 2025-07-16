package com.zerog.neoessentials.utils;

<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
/**
 * @deprecated This class has been replaced by {@link VanillaBooleanParser}.
 * This file is kept for reference only and should be removed in future versions.
 * 
 * VanillaBooleanParser uses vanilla StringArgumentType with post-processing conversion
 * which does not require client-side registration, making it truly server-side compatible.
 */
@Deprecated
public class StringToBooleanArgumentType {
    // This class has been intentionally emptied and deprecated.
    // Use VanillaBooleanParser instead which uses vanilla StringArgumentType.
<<<<<<< HEAD
=======
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

=======
>>>>>>> 06db8bd (feat: Refactor command argument handling to eliminate custom types and ensure true server-side compatibility)
/**
 * @deprecated This class has been replaced by {@link VanillaBooleanParser}.
 * This file is kept for reference only and should be removed in future versions.
 * 
 * VanillaBooleanParser uses vanilla StringArgumentType with post-processing conversion
 * which does not require client-side registration, making it truly server-side compatible.
 */
<<<<<<< HEAD
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
>>>>>>> a0123aa (refactor: Enhance message command handling and introduce StringToBooleanArgumentType for improved command argument parsing)
=======
@Deprecated
public class StringToBooleanArgumentType {
    // This class has been intentionally emptied and deprecated.
    // Use VanillaBooleanParser instead which uses vanilla StringArgumentType.
>>>>>>> 06db8bd (feat: Refactor command argument handling to eliminate custom types and ensure true server-side compatibility)
=======
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
}
