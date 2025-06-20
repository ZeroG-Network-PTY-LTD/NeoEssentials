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
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
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
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static final Supplier<ArgumentTypeInfo<StringToBooleanArgumentType, ?>> STRING_TO_BOOLEAN = COMMAND_ARGUMENT_TYPES.register(
            "string_to_boolean", 
            () -> (ArgumentTypeInfo) new StringToBooleanArgumentInfo()
    );

    /**
     * Register this class with the mod event bus to enable the registrations
     * 
     * @param eventBus The mod event bus
     */
    public static void register(IEventBus eventBus) {
        NeoEssentials.LOGGER.info("Registering custom command argument types");
        
        // Register our command argument types with the event bus
        COMMAND_ARGUMENT_TYPES.register(eventBus);
<<<<<<< HEAD
<<<<<<< HEAD
>>>>>>> 8a36c07 (feat: Add custom command argument types for string to boolean conversion)
=======

        // Also register using the direct method for compatibility 
=======
        
        // Register the common setup event for client-server synchronization
>>>>>>> 7ffa71d (feat: Enhance config management with robust error handling and lazy loading)
        eventBus.addListener(ModArgumentTypes::onCommonSetup);
    }
    
    /**
     * Common setup event to ensure proper synchronization of command arguments
     * This ensures that clients also receive the argument type information
     */
    private static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            NeoEssentials.LOGGER.info("Setting up command argument type synchronization");
              // Register the argument type class with its info class to ensure proper client-server serialization
            ArgumentTypeInfo<StringToBooleanArgumentType, ?> info = STRING_TO_BOOLEAN.get();
            ArgumentTypeInfos.registerByClass(StringToBooleanArgumentType.class, info);
        });
>>>>>>> faaaf85 (refactor: Update registration of StringToBooleanArgumentType for improved compatibility)
    }
}
