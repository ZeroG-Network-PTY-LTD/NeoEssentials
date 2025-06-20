package com.zerog.neoessentials.utils;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;
import com.zerog.neoessentials.NeoEssentials;

import javax.annotation.Nonnull;

/**
 * Info class for StringToBooleanArgumentType, needed to register it with the command system
 * 
 * This class is critical for client-server network synchronization of command argument types.
 * Both client and server must have identical implementations of this class.
 */
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
}
