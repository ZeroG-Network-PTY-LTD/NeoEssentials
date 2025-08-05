package com.zerog.neoessentials;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import com.zerog.neoessentials.commands.CommandRegistry;
import com.zerog.neoessentials.config.ConfigurationUnifier;

import java.nio.file.Path;
import com.zerog.neoessentials.features.CustomBossbarManager;
import com.zerog.neoessentials.features.TablistScoreboardManager;
import com.zerog.neoessentials.listeners.NotificationEventListener;
import com.zerog.neoessentials.localization.LanguageManager;
import com.zerog.neoessentials.managers.*;
import com.zerog.neoessentials.notifications.NotificationManager;
import com.zerog.neoessentials.permissions.CustomPermissionsManager;
import com.zerog.neoessentials.storage.PlayerDataManager;

import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
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
        
        // Register enhanced theme system event handlers (Phase 6)
        NeoForge.EVENT_BUS.register(TablistScoreboardManager.getInstance());
        NeoForge.EVENT_BUS.register(CustomBossbarManager.getInstance());
        
        LOGGER.info("NeoEssentials initialized successfully!");
    }
    
    /**
     * Server starting event handler
     */
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("NeoEssentials server starting setup...");
        
        try {
            // Initialize unified configuration system (Phase 5 Fix)
            ConfigurationUnifier.getInstance().initialize();
            LOGGER.info("Unified Configuration System initialized successfully");
            
            // Initialize storage systems
            PlayerDataManager.getInstance();
            LOGGER.info("Player data manager initialized");

            // Initialize Enhanced Language System (Phase 4)  
            Path configPath = ConfigurationUnifier.getInstance().getConfigPath();
            LanguageManager.getInstance(configPath).initialize();
            LOGGER.info("Language System initialized");
            
            // Initialize all managers
            EconomyManager.getInstance();
            HomeManager.getInstance();
            WarpManager.getInstance();
            KitManager.getInstance();
            ModerationManager.getInstance();
            MessagingManager.getInstance();
            SpawnManager.getInstance();
            
            // Initialize Language Manager
            LanguageManager.getInstance().initialize();
            
            // Initialize Plugin Compatibility Manager
            PluginCompatibilityManager.getInstance().initialize();
            
            // Initialize Custom Permissions Manager
            CustomPermissionsManager.getInstance();
            LOGGER.info("Custom Permissions Manager initialized");
            
            // Initialize Placeholder System
            com.zerog.neoessentials.placeholders.PlaceholderManager.getInstance();
            LOGGER.info("Placeholder System initialized");
            
            // Initialize Custom Bossbar Manager
            CustomBossbarManager.getInstance();
            LOGGER.info("Custom Bossbar Manager initialized");
            
            // Initialize Enhanced Theme System (Phase 6)
            TablistScoreboardManager.getInstance();
            LOGGER.info("Enhanced Tablist & Scoreboard Manager initialized");
            
            // Enhanced Bossbar Manager already initialized
            LOGGER.info("Enhanced Bossbar Manager ready");
            
            // Initialize Notification Manager
            NotificationManager notificationManager = NotificationManager.getInstance(ConfigurationUnifier.getInstance().getConfigManager().getMainConfig());
            notificationManager.notifyServerStart();
            
            // Initialize notification event listener
            NotificationEventListener.getInstance();
            
            LOGGER.info("All managers initialized successfully");
            
            LOGGER.info("NeoEssentials server setup completed successfully!");
            
        } catch (Exception e) {
            LOGGER.error("Failed to setup NeoEssentials on server start", e);
        }
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
     * Get the mod logger
     */
    public static Logger getLogger() {
        return LOGGER;
    }
}
