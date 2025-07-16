package com.zerog.neoessentials.utils;

<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
/**
 * @deprecated This class has been replaced by the server-side only approach in {@link VanillaBooleanParser}.
 * This file is kept for reference only and should be removed in future versions.
 * 
 * The new implementation uses vanilla StringArgumentType with post-processing conversion
 * which does not require ArgumentTypeInfo registration, making it truly server-side compatible.
 */
@Deprecated
public class StringToBooleanArgumentInfo {
    // This class has been intentionally emptied and deprecated.
    // The VanillaBooleanParser approach does not require ArgumentTypeInfo registration.
<<<<<<< HEAD
=======
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;
import com.zerog.neoessentials.NeoEssentials;

import javax.annotation.Nonnull;

=======
>>>>>>> 06db8bd (feat: Refactor command argument handling to eliminate custom types and ensure true server-side compatibility)
/**
 * @deprecated This class has been replaced by the server-side only approach in {@link VanillaBooleanParser}.
 * This file is kept for reference only and should be removed in future versions.
 * 
 * The new implementation uses vanilla StringArgumentType with post-processing conversion
 * which does not require ArgumentTypeInfo registration, making it truly server-side compatible.
 */
<<<<<<< HEAD
public class StringToBooleanArgumentInfo implements ArgumentTypeInfo<StringToBooleanArgumentType, StringToBooleanArgumentInfo.Template> {

    @Override
    public void serializeToNetwork(@Nonnull Template template, @Nonnull FriendlyByteBuf buffer) {
        // No additional data needed for this argument type
        // The type itself doesn't have any parameters to serialize
        NeoEssentials.LOGGER.debug("Serializing StringToBooleanArgumentType to network");
    }

    @Override
    @Nonnull
    public Template deserializeFromNetwork(@Nonnull FriendlyByteBuf buffer) {
        NeoEssentials.LOGGER.debug("Deserializing StringToBooleanArgumentType from network");
        return new Template();
    }

    @Override
    public void serializeToJson(@Nonnull Template template, @Nonnull com.google.gson.JsonObject json) {
        // No additional data needed for this argument type
        NeoEssentials.LOGGER.debug("Serializing StringToBooleanArgumentType to JSON");
    }

    @Override
    @Nonnull
    public Template unpack(@Nonnull StringToBooleanArgumentType argumentType) {
        NeoEssentials.LOGGER.debug("Unpacking StringToBooleanArgumentType");
        return new Template();
    }

    /**
     * Template class for StringToBooleanArgumentType
     * 
     * This inner class handles instantiation of the argument type from network data
     */
    public class Template implements ArgumentTypeInfo.Template<StringToBooleanArgumentType> {        @Override
        @Nonnull
        public StringToBooleanArgumentType instantiate(@Nonnull CommandBuildContext context) {
            NeoEssentials.LOGGER.debug("Instantiating StringToBooleanArgumentType from template");
            try {
                // Create our custom argument type
                return StringToBooleanArgumentType.stringToBoolean();
            } catch (Exception e) {
                // Fallback for vanilla clients - if there's an issue, try to provide
                // BoolArgumentType as a fallback, which vanilla clients understand
                NeoEssentials.LOGGER.warn("Error instantiating StringToBooleanArgumentType, falling back to vanilla type", e);
                return StringToBooleanArgumentType.stringToBoolean();
            }
        }

        @Override        @Nonnull
        public ArgumentTypeInfo<StringToBooleanArgumentType, ?> type() {
            return StringToBooleanArgumentInfo.this;
        }
    }
>>>>>>> 8a36c07 (feat: Add custom command argument types for string to boolean conversion)
=======
@Deprecated
public class StringToBooleanArgumentInfo {
    // This class has been intentionally emptied and deprecated.
    // The VanillaBooleanParser approach does not require ArgumentTypeInfo registration.
>>>>>>> 06db8bd (feat: Refactor command argument handling to eliminate custom types and ensure true server-side compatibility)
=======
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
}
