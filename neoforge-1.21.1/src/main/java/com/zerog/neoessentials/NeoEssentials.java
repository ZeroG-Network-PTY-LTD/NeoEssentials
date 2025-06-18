<<<<<<< HEAD
public class NeoEssentials {
    
=======
package com.zerog.neoessentials;

import com.zerog.neoessentials.adapter.NeoForge121AdapterFactory;
import com.zerog.neoessentials.common.adapter.AdapterFactory;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

/**
 * Main mod class for NeoEssentials NeoForge 1.21.1 version
 */
@Mod(NeoEssentials.MODID)
public class NeoEssentials {
    public static final String MODID = "neoessentials";
    public static final String MOD_NAME = "NeoEssentials";
    public static final Logger LOGGER = LogUtils.getLogger();
    
    private static NeoEssentials instance;
    private final AdapterFactory adapterFactory;
    private boolean initialized = false;
    
    /**
     * Constructor for the mod - called by Forge
     * @param modEventBus The event bus for mod-lifecycle events
     */
    public NeoEssentials(IEventBus modEventBus) {
        instance = this;
        
        LOGGER.info("NeoEssentials initializing for Minecraft 1.21.1 (NeoForge)");
        
        // Get the adapter factory for this version
        adapterFactory = NeoForge121AdapterFactory.getInstance();
        
        // Register ourselves for mod events
        modEventBus.addListener(this::setup);
        
        // Register ourselves for server events
        NeoForge.EVENT_BUS.register(this);
    }
    
    /**
     * Setup event handler - called during mod initialization
     * @param event The setup event
     */
    private void setup(final FMLCommonSetupEvent event) {
        LOGGER.info("NeoEssentials common setup");
        
        // Initialize adapters
        adapterFactory.getPermissionAdapter().initialize();
        adapterFactory.getCommandAdapter().initialize();
        
        // Mark as initialized
        initialized = true;
    }
    
    /**
     * Server starting event handler
     * @param event The server starting event
     */
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("NeoEssentials server starting");
        
        // Register all commands
        adapterFactory.getCommandAdapter().registerAllCommands();
    }
    
    /**
     * Server stopping event handler
     * @param event The server stopping event
     */
    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        LOGGER.info("NeoEssentials server stopping");
        
        // Perform cleanup
    }
    
    /**
     * Get the mod instance
     * @return The mod instance
     */
    public static NeoEssentials getInstance() {
        return instance;
    }
    
    /**
     * Get the adapter factory for this version
     * @return The adapter factory
     */
    public AdapterFactory getAdapterFactory() {
        return adapterFactory;
    }
    
    /**
     * Check if the mod is initialized
     * @return true if initialized, false otherwise
     */
    public boolean isInitialized() {
        return initialized;
    }
>>>>>>> 81f44ad (feat: Enhance README with multi-version support details)
}
