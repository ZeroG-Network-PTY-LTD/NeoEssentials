package com.zerog.neoessentials;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import com.zerog.neoessentials.commands.CommandRegistry;
import com.zerog.neoessentials.config.ConfigurationUnifier;

import java.nio.file.Path;
import com.zerog.neoessentials.features.CustomBossbarManager;
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
        NeoForge.EVENT_BUS.register(com.zerog.neoessentials.listeners.CommandOverrideListener.class);
        NeoForge.EVENT_BUS.register(new com.zerog.neoessentials.listeners.NameTagFormattingListener());
        LOGGER.info("Chat formatting system initialized");
        // Initialize unified config system (only once)
        ConfigurationUnifier.getInstance().initialize();
        LOGGER.info("Unified Configuration System initialized");
        LOGGER.info("NeoEssentials initialized successfully!");
    }
    
    /**
     * Server starting event handler
     */
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        
        LOGGER.info("NeoEssentials server starting setup...");
        try {
            // Unified config system already initialized in constructor
            // Initialize storage systems
            PlayerDataManager.getInstance();
            LOGGER.info("Player data manager initialized");
        } catch (Exception e) {
            LOGGER.error("Error during NeoEssentials server starting setup", e);
        }
        // Initialize Enhanced Language System (Phase 4)
        Path configPath = ConfigurationUnifier.getInstance().getConfigPath();
        LanguageManager.getInstance(configPath).initialize();
        LOGGER.info("Language System initialized");
        // Initialize all managers using unified config
        com.zerog.neoessentials.managers.EconomyManager.getInstance();
        HomeManager.getInstance();
        WarpManager.getInstance();
        KitManager.getInstance();
        ModerationManager.getInstance();
        MessagingManager.getInstance();
        SpawnManager.getInstance();
        TeleportRequestManager.getInstance();
        LOGGER.info("Teleport Request Manager initialized");
        com.zerog.neoessentials.storage.StorageManager.getInstance();
        LOGGER.info("Storage Manager initialized");
        com.zerog.neoessentials.security.SecurityManager.getInstance();
        LOGGER.info("Security Manager initialized");
        WebDashboardManager.getInstance();
        LOGGER.info("Web Dashboard Manager initialized");
        com.zerog.neoessentials.performance.PerformanceManager.getInstance();
        LOGGER.info("Performance Manager initialized");
        com.zerog.neoessentials.performance.AsyncOperationManager.getInstance();
        LOGGER.info("Async Operation Manager initialized");
        LanguageManager.getInstance().initialize();
        PluginCompatibilityManager.getInstance().initialize();
        CustomPermissionsManager.getInstance();
        LOGGER.info("Custom Permissions Manager initialized");
        com.zerog.neoessentials.placeholders.PlaceholderManager.getInstance();
        LOGGER.info("Placeholder System initialized");
        CustomBossbarManager.getInstance();
        LOGGER.info("Custom Bossbar Manager initialized");
        LOGGER.info("Enhanced Bossbar Manager ready");
        LOGGER.info("GUI system skipped - using sign-based shops only");
        LOGGER.info("Config GUI system skipped - using command-based configuration");
        LOGGER.info("Configuration GUI Manager initialized");
        NotificationManager notificationManager = NotificationManager.getInstance(ConfigurationUnifier.getInstance().getConfigManager().getMainConfig());
        notificationManager.notifyServerStart();
        NotificationEventListener.getInstance();
        NeoForge.EVENT_BUS.register(new com.zerog.neoessentials.listeners.PermissionEventListener());
        LOGGER.info("Permission Event Listener initialized");
        LOGGER.info("Shop Sign Event Handling consolidated into NeoEssentialsEventHandler");
    // Register shop/signshop event handler
    NeoForge.EVENT_BUS.register(new com.zerog.neoessentials.shops.ShopEventHandler());
        // Initialize new PlaceholderManager, TabListManager, ScoreboardManager, BossBarManager
        com.zerog.neoessentials.features.PlaceholderManager placeholderManager = new com.zerog.neoessentials.features.PlaceholderManager();
        com.zerog.neoessentials.features.TabListManager tabListManager = new com.zerog.neoessentials.features.TabListManager();
        com.zerog.neoessentials.features.ScoreboardManager scoreboardManager = new com.zerog.neoessentials.features.ScoreboardManager();
        com.zerog.neoessentials.features.BossBarManager bossBarManager = new com.zerog.neoessentials.features.BossBarManager();
        // Register UI event handler for tablist, scoreboard, bossbar
        NeoForge.EVENT_BUS.register(new com.zerog.neoessentials.features.UIEventHandler(
            tabListManager,
            scoreboardManager,
            bossBarManager,
            placeholderManager
        ));
    }
    // ...existing code...
    
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
