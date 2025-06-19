package com.zerog.neoessentials.init;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.utils.StringToBooleanArgumentType;
import com.zerog.neoessentials.utils.StringToBooleanArgumentInfo;

import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * Handles registration of custom argument types
 */
public class ModArgumentTypes {
    // Create a deferred register for command argument types
    public static final DeferredRegister<ArgumentTypeInfo<?, ?>> COMMAND_ARGUMENT_TYPES = 
            DeferredRegister.create(Registries.COMMAND_ARGUMENT_TYPE, NeoEssentials.MODID);    // Register our StringToBooleanArgumentType with the StringToBooleanArgumentInfo
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static final Supplier<ArgumentTypeInfo<StringToBooleanArgumentType, ?>> STRING_TO_BOOLEAN = COMMAND_ARGUMENT_TYPES.register(
            "StringToBoolean", 
            () -> (ArgumentTypeInfo) new StringToBooleanArgumentInfo()
    );

    /**
     * Register this class with the mod event bus to enable the registrations
     * 
     * @param eventBus The mod event bus
     */
    public static void register(net.neoforged.bus.api.IEventBus eventBus) {
        NeoEssentials.LOGGER.info("Registering custom command argument types");
        COMMAND_ARGUMENT_TYPES.register(eventBus);

        // Also register using the direct method for compatibility 
        eventBus.addListener(ModArgumentTypes::onCommonSetup);
    }
    
    /**
     * Handle registration during common setup event
     * This provides a more direct way to register the argument type as a fallback
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void onCommonSetup(net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            NeoEssentials.LOGGER.info("Registering StringToBooleanArgumentType during common setup");
            ArgumentTypeInfos.registerByClass((Class) StringToBooleanArgumentType.class, new StringToBooleanArgumentInfo());
        });
    }
}
