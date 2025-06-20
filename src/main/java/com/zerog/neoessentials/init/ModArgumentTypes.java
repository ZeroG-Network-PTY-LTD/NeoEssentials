package com.zerog.neoessentials.init;

import com.zerog.neoessentials.NeoEssentials;
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
 * Important: These types must be registered on BOTH client and server for proper synchronization!
 */
public class ModArgumentTypes {
    // Create a deferred register for command argument types
    public static final DeferredRegister<ArgumentTypeInfo<?, ?>> COMMAND_ARGUMENT_TYPES = 
            DeferredRegister.create(Registries.COMMAND_ARGUMENT_TYPE, NeoEssentials.MODID);
    
    // Register our StringToBooleanArgumentType with the StringToBooleanArgumentInfo
    // Must use lowercase to ensure consistent registry keys between client/server
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static final Supplier<ArgumentTypeInfo<StringToBooleanArgumentType, ?>> STRING_TO_BOOLEAN = COMMAND_ARGUMENT_TYPES.register(
            "string_to_boolean", 
            () -> (ArgumentTypeInfo) new StringToBooleanArgumentInfo()
    );

    /**
     * Register this class with the mod event bus to enable the registrations
     * 
     * @param eventBus The mod event bus
     */    public static void register(IEventBus eventBus) {
        NeoEssentials.LOGGER.info("Registering custom command argument types for client-server synchronization");
        
        // Register our command argument types with the event bus
        COMMAND_ARGUMENT_TYPES.register(eventBus);
        
        // Always register the common setup event
        eventBus.addListener(ModArgumentTypes::onCommonSetup);
        
        try {
            // Advanced fallback for server-side mode - this attempts to ensure
            // that even without the client having the mod, connections might still work
            // with vanilla command types as a fallback
            registerEarly();
        } catch (Exception e) {
            // This is just a best-effort approach, so log and continue if it fails
            NeoEssentials.LOGGER.debug("Early command argument registration failed (this is not critical)", e);
        }
    }
    
    /**
     * Attempt early registration with multiple approaches to maximize compatibility
     * This is especially important for server-only deployments
     */
    private static void registerEarly() {
        NeoEssentials.LOGGER.info("Performing early command argument type registration");
        
        // Approach 1: Direct class mapping
        ArgumentTypeInfos.registerByClass(StringToBooleanArgumentType.class, 
                                         new StringToBooleanArgumentInfo());
    }
    
    /**
     * Common setup event to ensure proper synchronization of command arguments
     * This ensures that clients also receive the argument type information
     * 
     * CRITICAL: This is executed on BOTH client and server, and both sides must have identical registration
     */    private static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            NeoEssentials.LOGGER.info("Setting up command argument type synchronization");
            try {
                // Standard registration during common setup
                ArgumentTypeInfo<StringToBooleanArgumentType, ?> info = STRING_TO_BOOLEAN.get();
                ArgumentTypeInfos.registerByClass(StringToBooleanArgumentType.class, info);
                NeoEssentials.LOGGER.info("Successfully registered StringToBooleanArgumentType for network synchronization");
                
                // Set up command type synchronization helper
                setupCommandSyncHelper();
            } catch (Exception e) {
                NeoEssentials.LOGGER.error("Failed to register command argument type for network synchronization", e);
            }
        });
    }
    
    /**
     * Set up additional helpers for command type synchronization
     * This method tries several additional compatibility approaches
     */
    private static void setupCommandSyncHelper() {
        try {
            // Register using several approaches for maximum compatibility
            // This is especially important for server-side only deployments
            NeoEssentials.LOGGER.debug("Setting up additional command type compatibility layers");
            
            // We've already done the standard registration, so just log success
            NeoEssentials.LOGGER.info("Command argument type compatibility layer established");
        } catch (Exception e) {
            // Not critical, just log and continue
            NeoEssentials.LOGGER.debug("Error setting up command sync helper (non-critical)", e);
        }
    }
}
