package com.zerog.neoessentials.utils;

<<<<<<< HEAD
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
=======
import com.mojang.brigadier.arguments.ArgumentType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;

import javax.annotation.Nonnull;

/**
 * Info class for StringToBooleanArgumentType, needed to register it with the command system
 */
public class StringToBooleanArgumentInfo implements ArgumentTypeInfo<StringToBooleanArgumentType, StringToBooleanArgumentInfo.Template> {

    @Override
    public void serializeToNetwork(@Nonnull Template template, @Nonnull FriendlyByteBuf buffer) {
        // No additional data needed for this argument type
    }

    @Override
    @Nonnull
    public Template deserializeFromNetwork(@Nonnull FriendlyByteBuf buffer) {
        return new Template();
    }

    @Override
    public void serializeToJson(@Nonnull Template template, @Nonnull com.google.gson.JsonObject json) {
        // No additional data needed for this argument type
    }

    @Override
    @Nonnull
    public Template unpack(@Nonnull StringToBooleanArgumentType argumentType) {
        return new Template();
    }

    /**
     * Template class for StringToBooleanArgumentType
     */
    public class Template implements ArgumentTypeInfo.Template<StringToBooleanArgumentType> {
        @Override
        @Nonnull
        public StringToBooleanArgumentType instantiate(@Nonnull CommandBuildContext context) {
            return StringToBooleanArgumentType.stringToBoolean();
        }

        @Override
        @Nonnull
        public ArgumentTypeInfo<StringToBooleanArgumentType, ?> type() {
            return StringToBooleanArgumentInfo.this;
        }
    }
>>>>>>> 8a36c07 (feat: Add custom command argument types for string to boolean conversion)
}
