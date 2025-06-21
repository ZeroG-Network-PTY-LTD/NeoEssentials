package com.zerog.neoessentials.network;

import com.zerog.neoessentials.NeoEssentials;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * Handles network functionality for NeoEssentials.
 * This class supports server-only deployment in a modded environment.
 */
public class NetworkHandler {
      // Identifier for our mod resources
    public static final String MOD_ID = NeoEssentials.MODID;
    
    private static NetworkHandler instance;
    
    /**
     * Initialize the network handler
     * @param modEventBus The mod event bus
     */
    public static void init(IEventBus modEventBus) {
        if (instance == null) {
            instance = new NetworkHandler();
        }
        
        // Register setup event
        modEventBus.addListener(NetworkHandler::onCommonSetup);
        
        NeoEssentials.LOGGER.info("Registered NetworkHandler for server-side functionality");
    }
    
    /**
     * Common setup to handle any required network functionality
     */
    private static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            NeoEssentials.LOGGER.info("Setting up server-only modded environment support");
            
            // Register any optional server-side functionality here
            // No specific packets needed for server-side only
        });
    }
}
