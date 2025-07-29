package com.zerog.neoessentials;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.bus.api.SubscribeEvent;

/**
 * NeoEssentials Main Class - Provides server administration tools
 * 
 * This is the main mod class that initializes all NeoEssentials features
 * and provides server administration functionality.
 * 
 * @author ZeroG
 * @version 2.0.0
 */
public class NeoEssentials {
    public static final Logger LOGGER = LogUtils.getLogger();
    
    /**
     * Constructor for NeoEssentials
     */
    public NeoEssentials() {
        LOGGER.info("NeoEssentials initializing...");
        
        // Register for server events
        NeoForge.EVENT_BUS.register(this);
        
        LOGGER.info("NeoEssentials initialized successfully!");
    }
    
    /**
     * Server starting event handler
     */
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("NeoEssentials server starting setup...");
        
        try {
            // TODO: Initialize managers and configuration
            LOGGER.info("NeoEssentials server setup completed successfully!");
            
        } catch (Exception e) {
            LOGGER.error("Failed to setup NeoEssentials on server start", e);
        }
    }
    
    /**
     * Get the mod logger
     */
    public static Logger getLogger() {
        return LOGGER;
    }
}
