package com.zerog.neoessentials;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import com.zerog.neoessentials.commands.EssentialsCommandManager;
import com.zerog.neoessentials.config.EssentialsConfig;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.bus.api.SubscribeEvent;

/**
 * NeoEssentials Compatibility Layer - Provides EssentialsX-style commands
 * 
 * This class acts as a compatibility layer for the main NeoEssentials mod,
 * providing the familiar EssentialsX command interface while leveraging
 * the core services from the main mod.
 * 
 * @author ZeroG
 * @version 2.0.0
 */
public class NeoEssentialsCompat {
    public static final Logger LOGGER = LogUtils.getLogger();
    
    private EssentialsCommandManager commandManager;
    
    /**
     * Initialize the compatibility layer
     * This is called by the main NeoEssentials mod during its setup phase
     */
    public void initialize() {
        LOGGER.info("Initializing NeoEssentials compatibility layer...");
        
        // Initialize command manager with empty config for now
        // This will be properly initialized when the main mod provides services
        commandManager = new EssentialsCommandManager(new EssentialsConfig());
        
        // Register for server events
        NeoForge.EVENT_BUS.register(this);
        
        LOGGER.info("NeoEssentials compatibility layer initialized");
    }
    
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Try to connect to the main NeoEssentials mod services
        try {
            // Access the static services directly
            if (commandManager != null) {
                commandManager.initializeServices(
                    com.neoessentials.NeoEssentials.getHomeService(),
                    com.neoessentials.NeoEssentials.getLanguageManager()
                );
                LOGGER.info("Successfully connected to main NeoEssentials services");
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to connect to main NeoEssentials services - compatibility layer disabled: {}", e.getMessage());
        }
        
        LOGGER.info("NeoEssentials compatibility layer ready!");
    }
    
    /**
     * Get the command manager instance
     */
    public EssentialsCommandManager getCommandManager() {
        return commandManager;
    }
}
