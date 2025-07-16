package com.zerog.neoessentials.init;

import com.zerog.neoessentials.NeoEssentials;
<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
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
<<<<<<< HEAD
=======
import com.zerog.neoessentials.utils.StringToBooleanArgumentType;
import com.zerog.neoessentials.utils.StringToBooleanArgumentInfo;
=======
import com.zerog.neoessentials.utils.VanillaBooleanParser;
>>>>>>> c8bd7e4 (feat: Replace custom string-to-boolean argument type with vanilla-compatible implementation and update command handling)

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
<<<<<<< HEAD
     */    public static void register(IEventBus eventBus) {
        NeoEssentials.LOGGER.info("Setting up server-side command argument types in modded environment");
        
<<<<<<< HEAD
        // Register our command argument types with the event bus
        COMMAND_ARGUMENT_TYPES.register(eventBus);
<<<<<<< HEAD
<<<<<<< HEAD
>>>>>>> 8a36c07 (feat: Add custom command argument types for string to boolean conversion)
=======

        // Also register using the direct method for compatibility 
=======
        
<<<<<<< HEAD
<<<<<<< HEAD
        // Register the common setup event for client-server synchronization
>>>>>>> 7ffa71d (feat: Enhance config management with robust error handling and lazy loading)
=======
        // Register with normal priority since we don't have other setup that depends on this
>>>>>>> 7199bed (feat: Enhance client-server synchronization for command argument types and add compatibility guide)
=======
        // Always register the common setup event
>>>>>>> fcd7175 (feat: Optimize server-side command argument registration and enhance client compatibility)
        eventBus.addListener(ModArgumentTypes::onCommonSetup);
        
        try {
            // Advanced fallback for server-side mode - this attempts to ensure
            // that even without the client having the mod, connections might still work
            // with vanilla command types as a fallback
            registerEarly();
        } catch (Exception e) {
            // This is just a best-effort approach, so log and continue if it fails
            NeoEssentials.LOGGER.debug("Early command argument registration failed (this is not critical)", e);
=======
        // Only register on the server side
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isDedicatedServer()) {
            NeoEssentials.LOGGER.info("Registering server-side command argument types");
            
            // Register DeferredRegister on server side only
            COMMAND_ARGUMENT_TYPES.register(eventBus);
            
            // Set up common setup for server
            eventBus.addListener(ModArgumentTypes::onCommonSetup);
            
            try {
                // Direct registration for server-side only
                registerServerSide();
            } catch (Exception e) {
                NeoEssentials.LOGGER.warn("Server-side command registration encountered an issue", e);
            }
        } else {
            NeoEssentials.LOGGER.info("Skipping command argument registration on client side");
>>>>>>> b7ad675 (feat: Implement server-side network handler for NeoEssentials mod)
        }
    }
      /**
     * Server-side registration of command argument types
     * This is only called on dedicated servers
=======
>>>>>>> c8bd7e4 (feat: Replace custom string-to-boolean argument type with vanilla-compatible implementation and update command handling)
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
>>>>>>> faaaf85 (refactor: Update registration of StringToBooleanArgumentType for improved compatibility)
=======
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
    }
}
