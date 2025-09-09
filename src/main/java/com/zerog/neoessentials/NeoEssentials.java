package com.zerog.neoessentials;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import com.zerog.neoessentials.commands.CommandRegistry;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.managers.FeatureManager;

import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
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
        
        // Register event handlers
        NeoForge.EVENT_BUS.register(this);
        
        // Initialize unified config system first
        ConfigManager.getInstance().initialize();
        
        LOGGER.info("NeoEssentials configuration system initialized");
        com.zerog.neoessentials.util.DebugUtil.debugLog("Unified Configuration System initialized");
        
        // Initialize core managers early to avoid null pointer exceptions
        initializeEarlyManagers();
        
        // Initialize server tick listener for animated placeholders
        com.zerog.neoessentials.listeners.ServerTickListener.initialize();
        com.zerog.neoessentials.util.DebugUtil.debugLog("Server tick listener initialized");
        
        LOGGER.info("NeoEssentials initialized successfully!");
    }
    
    /**
     * Initialize early managers that other components depend on
     */
    private void initializeEarlyManagers() {
        try {
            // Initialize TabListManager early to prevent null pointer exceptions
            new com.zerog.neoessentials.features.TabListManager();
            com.zerog.neoessentials.util.DebugUtil.debugLog("TabListManager initialized early");
            
            // Initialize other critical managers
            com.zerog.neoessentials.features.NameFormatManager.getInstance();
            com.zerog.neoessentials.util.DebugUtil.debugLog("NameFormatManager initialized early");
            
            com.zerog.neoessentials.placeholders.PlaceholderManager.getInstance();
            com.zerog.neoessentials.util.DebugUtil.debugLog("PlaceholderManager initialized early");
            
        } catch (Exception e) {
            LOGGER.error("Error initializing early managers", e);
        }
    }
    
    /**
     * Server starting event handler
     */
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("NeoEssentials server starting setup...");
        try {
            // Initialize all features using the new FeatureManager
            FeatureManager.getInstance().initializeFeatures();
            LOGGER.info("FeatureManager initialized successfully");
            
            // Setup tablist system
            setupTablistSystem();
            LOGGER.info("Tablist system initialized successfully");
            
        } catch (Exception e) {
            LOGGER.error("Error during NeoEssentials server starting setup", e);
        }
    }
    
    /**
     * Setup the improved tablist system with coordinated managers
     */
    private void setupTablistSystem() {
        LOGGER.info("Setting up tablist system...");
        try {
            // Initialize tablist components with correct constructors
            com.zerog.neoessentials.tablist.HeaderFooterManager headerFooterManager = 
                new com.zerog.neoessentials.tablist.HeaderFooterManager();
            
            com.zerog.neoessentials.tablist.AnimationScheduler animationScheduler = 
                new com.zerog.neoessentials.tablist.AnimationScheduler(
                    headerFooterManager,
                    com.zerog.neoessentials.placeholders.PlaceholderManager.getInstance());
            
            com.zerog.neoessentials.tablist.TabUpdateOrchestrator tabUpdateOrchestrator = 
                new com.zerog.neoessentials.tablist.TabUpdateOrchestrator(
                    headerFooterManager, 
                    com.zerog.neoessentials.placeholders.PlaceholderManager.getInstance(), 
                    animationScheduler);
            
            // Register tablist event handlers
            registerTablistEvents(tabUpdateOrchestrator);
            
            com.zerog.neoessentials.util.DebugUtil.debugLog("Tablist system components initialized successfully");
        } catch (Exception e) {
            LOGGER.error("Error setting up tablist system", e);
        }
    }
    
    /**
     * Register tablist event handlers
     */
    private void registerTablistEvents(com.zerog.neoessentials.tablist.TabUpdateOrchestrator tabUpdateOrchestrator) {
        LOGGER.info("Registering tablist event handlers...");
        try {
            // Register tablist-related event listeners with NeoForge
            NeoForge.EVENT_BUS.register(new com.zerog.neoessentials.listeners.TablistEventListener(tabUpdateOrchestrator));
            
            com.zerog.neoessentials.util.DebugUtil.debugLog("Tablist event handlers registered successfully");
        } catch (Exception e) {
            LOGGER.error("Error registering tablist event handlers", e);
        }
    }
    
    /**
     * Command registration event handler
     */
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        LOGGER.info("Starting command registration...");
        try {
            CommandRegistry.registerCommands(event.getDispatcher(), event.getBuildContext());
            LOGGER.info("All NeoEssentials commands registered successfully!");
        } catch (Exception e) {
            LOGGER.error("Error registering NeoEssentials commands", e);
        }
    }
    
    /**
     * Server stopping event handler - cleanup resources
     */
    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        LOGGER.info("NeoEssentials shutting down...");
        try {
            // Save shop data before shutdown - CRITICAL FIX
            com.zerog.neoessentials.economy.shops.ShopManager shopManager = 
                com.zerog.neoessentials.economy.shops.ShopManager.getInstance();
            if (shopManager != null) {
                LOGGER.info("Saving shop data before shutdown...");
                shopManager.shutdown(); // This calls saveShopsToStorage()
                LOGGER.info("Shop data saved successfully");
            }
            
            // Save any other critical data
            com.zerog.neoessentials.storage.StorageManager storageManager = 
                com.zerog.neoessentials.storage.StorageManager.getInstance();
            if (storageManager != null) {
                storageManager.shutdown();
                LOGGER.info("Storage manager shut down successfully");
            }
            
            LOGGER.info("NeoEssentials shutdown completed successfully");
        } catch (Exception e) {
            LOGGER.error("Error during NeoEssentials shutdown", e);
        }
    }
    
    /**
     * Get the mod logger
     */
    public static Logger getLogger() {
        return LOGGER;
    }
}
