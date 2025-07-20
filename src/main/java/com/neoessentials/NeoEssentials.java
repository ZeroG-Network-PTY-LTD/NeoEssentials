package com.neoessentials;

import com.neoessentials.api.home.HomeService;
import com.neoessentials.commands.teleportation.HomeCommand;
import com.neoessentials.config.EssentialsConfig;
import com.neoessentials.language.LanguageManager;
import com.neoessentials.network.NetworkHandler;
import com.neoessentials.util.ServerSideUtil;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.bus.api.SubscribeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * NeoEssentials - Essential server-side utilities for NeoForge
 * This mod is designed to work ONLY on the server side and does not require client installation.
 * 
 * @author ZeroG
 * @version 2.0.0
 */
@Mod(NeoEssentials.MODID)
public class NeoEssentials {
    public static final String MODID = "neoessentials";
    public static final Logger LOGGER = LoggerFactory.getLogger(NeoEssentials.class);
    
    // Core services
    private EssentialsConfig config;
    private LanguageManager languageManager;
    private HomeService homeService;
    
    // Command handlers
    private HomeCommand homeCommand;
    
    public NeoEssentials(IEventBus modEventBus, ModContainer modContainer) {
        // Only initialize on the server side
        if (FMLEnvironment.dist.isClient()) {
            LOGGER.warn("NeoEssentials is a server-side only mod and should not be installed on clients!");
            return;
        }
        
        // Register for server setup events only
        modEventBus.addListener(this::onServerSetup);
        
        // Register for server events
        NeoForge.EVENT_BUS.register(this);
        
        LOGGER.info("NeoEssentials mod loading - Server-side utilities initializing...");
    }
    
    /**
     * Server setup - Initialize all services and configurations
     */
    private void onServerSetup(final FMLDedicatedServerSetupEvent event) {
        LOGGER.info("NeoEssentials server setup starting...");
        
        try {
            // Initialize configuration
            config = new EssentialsConfig();
            LOGGER.info("Configuration system initialized");
            
            // Initialize language manager
            languageManager = new LanguageManager();
            LOGGER.info("Language system initialized with {} supported locales", 
                       config.language.supportedLocales.length);
            
            // Initialize services
            Path dataDir = Paths.get("config", MODID);
            homeService = new HomeService(dataDir, config, languageManager);
            LOGGER.info("Home service initialized");
            
            // Initialize commands
            homeCommand = new HomeCommand(homeService, languageManager);
            LOGGER.info("Command system initialized");
            
            LOGGER.info("NeoEssentials server setup complete - All systems ready!");
            
        } catch (Exception e) {
            LOGGER.error("Failed to initialize NeoEssentials", e);
        }
    }
    
    /**
     * Register commands when server starts
     */
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        if (homeCommand != null) {
            homeCommand.register(event.getDispatcher());
            LOGGER.info("Home commands registered: /home, /sethome, /delhome, /homes");
        }
    }
    
    /**
     * Server starting event
     */
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("NeoEssentials server starting - Essential utilities are now active!");
        LOGGER.info("Available features: Home System, Multi-Language Support");
        
        // Log configuration status
        if (config != null) {
            LOGGER.info("Max homes per player: {}", config.getTeleportConfig().getMaxHomes());
            LOGGER.info("Default language: {}", config.language.defaultLocale);
            LOGGER.info("Modules enabled: Economy={}, Chat={}, Protection={}", 
                       config.modules.economy, config.modules.chat, config.modules.protect);
        }
    }
    
    /**
     * Server stopping event - Clean up and save data
     */
    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        LOGGER.info("NeoEssentials shutting down - Saving all data...");
        
        // Save all cached data
        if (homeService != null) {
            try {
                homeService.saveAll().join(); // Wait for save to complete
                LOGGER.info("All home data saved successfully");
            } catch (Exception e) {
                LOGGER.error("Failed to save home data during shutdown", e);
            }
        }
        
        LOGGER.info("NeoEssentials shutdown complete");
    }
    
    // Getters for services (for future expansion)
    public static EssentialsConfig getConfig() {
        return getInstance().config;
    }
    
    public static LanguageManager getLanguageManager() {
        return getInstance().languageManager;
    }
    
    public static HomeService getHomeService() {
        return getInstance().homeService;
    }
    
    private static NeoEssentials instance;
    
    private static NeoEssentials getInstance() {
        return instance;
    }
    
    // Set instance for service access
    {
        instance = this;
    }
}
