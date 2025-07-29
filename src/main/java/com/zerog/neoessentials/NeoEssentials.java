package com.zerog.neoessentials;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.managers.*;
import com.zerog.neoessentials.storage.PlayerDataManager;

import net.neoforged.fml.common.Mod;
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
@Mod("neoessentials")
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
            // Initialize configuration manager
            ConfigManager.getInstance();
            LOGGER.info("Configuration loaded successfully");
            
            // Initialize storage systems
            PlayerDataManager.getInstance();
            LOGGER.info("Player data manager initialized");
            
            // Initialize all managers
            EconomyManager.getInstance();
            HomeManager.getInstance();
            WarpManager.getInstance();
            KitManager.getInstance();
            ModerationManager.getInstance();
            MessagingManager.getInstance();
            SpawnManager.getInstance();
            LOGGER.info("All managers initialized successfully");
            
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
