package com.zerog.neoessentials.init;

import com.zerog.neoessentials.NeoEssentials;
<<<<<<< HEAD
import com.zerog.neoessentials.utils.VanillaBooleanParser;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * Command argument utilities using only vanilla command arguments.
 * This class replaces custom argument types with vanilla-compatible alternatives.
 * No custom command argument types are registered, ensuring client compatibility.
 */
public class ModArgumentTypes {

    /**
     * Register this class with the mod event bus
     * 
     * @param eventBus The mod event bus
     */
    public static void register(IEventBus eventBus) {
        NeoEssentials.LOGGER.info("Setting up vanilla-compatible command arguments");
        
        // Just set up common setup event - no custom argument types to register
        eventBus.addListener(ModArgumentTypes::onCommonSetup);
    }
    
    /**
     * Common setup for command handling
     */
    private static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // Only execute on dedicated server
            if (!net.neoforged.fml.loading.FMLEnvironment.dist.isDedicatedServer()) {
                return;
            }
            
            NeoEssentials.LOGGER.info("Initializing vanilla-compatible command arguments");
            
            // No custom command argument types to register
            // All commands will use vanilla argument types with our parsers
            
            NeoEssentials.LOGGER.info("Vanilla-compatible command system configured successfully");
        });
=======
import com.zerog.neoessentials.utils.StringToBooleanArgumentType;
import com.zerog.neoessentials.utils.StringToBooleanArgumentInfo;

import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * Handles registration of custom argument types
 */
public class ModArgumentTypes {
    // Create a deferred register for command argument types
    public static final DeferredRegister<ArgumentTypeInfo<?, ?>> COMMAND_ARGUMENT_TYPES = 
            DeferredRegister.create(Registries.COMMAND_ARGUMENT_TYPE, NeoEssentials.MODID);

    // Register our StringToBooleanArgumentType with the StringToBooleanArgumentInfo
    public static final Supplier<ArgumentTypeInfo<?, ?>> STRING_TO_BOOLEAN = COMMAND_ARGUMENT_TYPES.register(
            "string_to_boolean", 
            () -> new StringToBooleanArgumentInfo()
    );

    /**
     * Register this class with the mod event bus to enable the registrations
     * 
     * @param eventBus The mod event bus
     */
    public static void register(net.neoforged.bus.api.IEventBus eventBus) {
        NeoEssentials.LOGGER.info("Registering custom command argument types");
        COMMAND_ARGUMENT_TYPES.register(eventBus);
<<<<<<< HEAD
>>>>>>> 8a36c07 (feat: Add custom command argument types for string to boolean conversion)
=======

        // Also register using the direct method for compatibility 
        eventBus.addListener(ModArgumentTypes::onCommonSetup);
    }
    
    /**
     * Handle registration during common setup event
     * This provides a more direct way to register the argument type as a fallback
     */
    private static void onCommonSetup(net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            NeoEssentials.LOGGER.info("Registering StringToBooleanArgumentType during common setup");
            ArgumentTypeInfos.registerByClass(StringToBooleanArgumentType.class, STRING_TO_BOOLEAN.get());
        });
>>>>>>> faaaf85 (refactor: Update registration of StringToBooleanArgumentType for improved compatibility)
    }
}
