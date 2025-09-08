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
        // Register for server events
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(com.zerog.neoessentials.listeners.CommandOverrideListener.class);
        NeoForge.EVENT_BUS.register(com.zerog.neoessentials.listeners.ServerTickListener.class);
        
        // Initialize unified config system first
        // Initialize configuration system directly
        ConfigManager.getInstance().initialize();
        
        LOGGER.info("NeoEssentials configuration system initialized");
        com.zerog.neoessentials.util.DebugUtil.debugLog("Unified Configuration System initialized");
        
        // Initialize core managers early to avoid null pointer exceptions
        initializeEarlyManagers();
        
        // Now register listeners that depend on managers
        NeoForge.EVENT_BUS.register(new com.zerog.neoessentials.listeners.NameTagFormattingListener());
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
     * Server starting event handler
     */
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        
        LOGGER.info("NeoEssentials server starting setup...");
        try {
            // Clean up any previous scoreboard/bossbar content first
            com.zerog.neoessentials.util.ScoreboardCleanupUtil.cleanupAll(event.getServer());
            
            // Unified config system already initialized in constructor
            // Initialize all features using the new FeatureManager
            FeatureManager.getInstance().initializeFeatures();
        } catch (Exception e) {
            LOGGER.error("Error during NeoEssentials server starting setup", e);
        }
        
        // Setup tablist system using coordinated managers
        setupTablistSystem();
    }
    
    /**
     * Setup the improved tablist system with coordinated managers
     */
    private void setupTablistSystem() {
        try {
            FeatureManager featureManager = FeatureManager.getInstance();
            
            // Get managers from FeatureManager
            com.zerog.neoessentials.tablist.HeaderFooterManager headerFooterManager = 
                featureManager.getManager("headerFooter", com.zerog.neoessentials.tablist.HeaderFooterManager.class);
            com.zerog.neoessentials.placeholders.PlaceholderManager placeholderManager = 
                featureManager.getManager("placeholders", com.zerog.neoessentials.placeholders.PlaceholderManager.class);
            
            if (headerFooterManager != null && placeholderManager != null) {
                // Create tablist orchestration system
                com.zerog.neoessentials.tablist.AnimationScheduler animationScheduler = 
                    new com.zerog.neoessentials.tablist.AnimationScheduler(headerFooterManager, placeholderManager);
                com.zerog.neoessentials.tablist.TabUpdateOrchestrator tabUpdateOrchestrator = 
                    new com.zerog.neoessentials.tablist.TabUpdateOrchestrator(headerFooterManager, placeholderManager, animationScheduler);
                
                // Set header/footer templates (config-driven)
                tabUpdateOrchestrator.setHeaderTemplate(new String[]{"Welcome to NeoEssentials!", "Enjoy your stay!"}, 1000);
                tabUpdateOrchestrator.setFooterTemplate(new String[]{"Online: ${server_players}", "TPS: ${server_tps}"}, 1000);
                
                // Register event listeners for tablist updates
                registerTablistEvents(tabUpdateOrchestrator);
                
                com.zerog.neoessentials.util.DebugUtil.debugLog("Enhanced tablist system initialized");
            } else {
                com.zerog.neoessentials.util.DebugUtil.warnLog("Failed to initialize tablist system - managers not available");
            }
            
            // Register additional event handlers
            NeoForge.EVENT_BUS.register(new com.zerog.neoessentials.listeners.PermissionEventListener());
            com.zerog.neoessentials.util.DebugUtil.debugLog("Permission Event Listener initialized");
            
            NeoForge.EVENT_BUS.register(new com.zerog.neoessentials.listeners.EconomyEventListener());
            com.zerog.neoessentials.util.DebugUtil.debugLog("Economy Event Listener initialized");
            
            NeoForge.EVENT_BUS.register(new com.zerog.neoessentials.shops.ShopEventHandler());
            com.zerog.neoessentials.util.DebugUtil.debugLog("Shop Event Handler initialized");
            
        } catch (Exception e) {
            LOGGER.error("Error setting up tablist system", e);
        }
    }
    
    /**
     * Register tablist event handlers
     */
    private void registerTablistEvents(com.zerog.neoessentials.tablist.TabUpdateOrchestrator tabUpdateOrchestrator) {
        NeoForge.EVENT_BUS.register(new Object() {
            @SubscribeEvent
            public void onPlayerJoin(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
                if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
                    // Clean up any residual display elements for this player
                    com.zerog.neoessentials.util.ScoreboardCleanupUtil.cleanupPlayerDisplays(player);
                    
                    // Then set up tablist
                    tabUpdateOrchestrator.onPlayerJoin(player);
                }
            }
            
            @SubscribeEvent
            public void onPlayerQuit(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
                if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
                    tabUpdateOrchestrator.onPlayerQuit(player);
                }
            }
            
            @SubscribeEvent
            public void onPermissionUpdate(com.zerog.neoessentials.features.PermissionUpdateEvent event) {
                if (event.getPlayer() instanceof net.minecraft.server.level.ServerPlayer player) {
                    tabUpdateOrchestrator.onPermissionUpdate(player);
                }
            }
        });
    }
    
    /**
     * Command registration event handler
     */
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        LOGGER.info("Registering NeoEssentials commands...");
        CommandRegistry.registerCommands(event.getDispatcher(), event.getBuildContext());
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
            
            // Shutdown cleanup command scheduler
            com.zerog.neoessentials.commands.admin.CleanupCommand.shutdown();
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
