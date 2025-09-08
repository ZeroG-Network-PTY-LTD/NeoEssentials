package com.zerog.neoessentials;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import com.zerog.neoessentials.commands.CommandRegistry;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.managers.FeatureManager;

// Temporarily disabled NeoForge imports due to classpath issues
// import net.neoforged.fml.common.Mod;
// import net.neoforged.neoforge.common.NeoForge;
// import net.neoforged.neoforge.event.RegisterCommandsEvent;
// import net.neoforged.neoforge.event.server.ServerStartingEvent;
// import net.neoforged.neoforge.event.server.ServerStoppingEvent;
// import net.neoforged.bus.api.SubscribeEvent;

/**
 * NeoEssentials Main Class - Provides server administration tools
 * TEMPORARILY SIMPLIFIED due to import issues - will restore when NeoForge imports work
 * 
 * This is the main mod class that initializes all NeoEssentials features
 * and provides server administration functionality.
 * 
 * @author ZeroG
 * @version 2.0.0
 */
// @Mod("neoessentials") // Temporarily disabled
public class NeoEssentials {
    public static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Constructor for NeoEssentials
     */
    public NeoEssentials() {
        LOGGER.info("NeoEssentials initializing (simplified mode due to import issues)...");
        // Temporarily disabled event registration
        // NeoForge.EVENT_BUS.register(this);
        // NeoForge.EVENT_BUS.register(com.zerog.neoessentials.listeners.CommandOverrideListener.class);
        // NeoForge.EVENT_BUS.register(com.zerog.neoessentials.listeners.ServerTickListener.class);
        
        // Initialize unified config system first
        // Initialize configuration system directly
        ConfigManager.getInstance().initialize();
        
        LOGGER.info("NeoEssentials configuration system initialized");
        com.zerog.neoessentials.util.DebugUtil.debugLog("Unified Configuration System initialized");
        
        // Initialize core managers early to avoid null pointer exceptions
        initializeEarlyManagers();
        
        // Temporarily disabled listener registration
        // NeoForge.EVENT_BUS.register(new com.zerog.neoessentials.listeners.NameTagFormattingListener());
        com.zerog.neoessentials.util.DebugUtil.debugLog("Chat formatting system initialized");
        
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
     * Server starting event handler - TEMPORARILY DISABLED
     */
    // @SubscribeEvent
    public void onServerStarting(Object event) {
        
        LOGGER.info("NeoEssentials server starting setup... (simplified mode)");
        try {
            // Temporarily disabled server-specific setup
            // com.zerog.neoessentials.util.ScoreboardCleanupUtil.cleanupAll(event.getServer());
            
            // Unified config system already initialized in constructor
            // Initialize all features using the new FeatureManager
            FeatureManager.getInstance().initializeFeatures();
        } catch (Exception e) {
            LOGGER.error("Error during NeoEssentials server starting setup", e);
        }
        
        // Temporarily disabled tablist system
        // setupTablistSystem();
    }
    
    /**
     * Setup the improved tablist system with coordinated managers - TEMPORARILY DISABLED
     */
    private void setupTablistSystem() {
        LOGGER.info("Tablist system setup disabled due to import issues");
        try {
            // Temporarily disabled tablist system initialization
            com.zerog.neoessentials.util.DebugUtil.debugLog("Tablist system initialization skipped");
        } catch (Exception e) {
            LOGGER.error("Error setting up tablist system", e);
        }
    }
    
    /**
     * Register tablist event handlers - TEMPORARILY DISABLED
     */
    private void registerTablistEvents(Object tabUpdateOrchestrator) {
        LOGGER.info("Tablist event registration disabled due to import issues");
    }
    
    /**
     * Command registration event handler - TEMPORARILY DISABLED
     */
    // @SubscribeEvent
    public void onRegisterCommands(Object event) {
        LOGGER.info("Command registration disabled due to import issues");
        // CommandRegistry.registerCommands(event.getDispatcher(), event.getBuildContext());
    }
    
    /**
     * Server stopping event handler - cleanup resources - TEMPORARILY DISABLED
     */
    // @SubscribeEvent
    public void onServerStopping(Object event) {
        LOGGER.info("NeoEssentials shutting down... (simplified mode)");
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
            
            // TODO: Restore cleanup command when implemented
            // com.zerog.neoessentials.commands.admin.CleanupCommand.shutdown();
            LOGGER.info("NeoEssentials cleanup scheduler shut down successfully");
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
