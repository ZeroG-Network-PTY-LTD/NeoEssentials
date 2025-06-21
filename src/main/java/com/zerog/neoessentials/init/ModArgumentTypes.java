package com.zerog.neoessentials.init;

import com.zerog.neoessentials.NeoEssentials;
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
    }
}
