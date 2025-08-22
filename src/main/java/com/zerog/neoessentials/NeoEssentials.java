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

    // Register command override listener to intercept vanilla whisper commands
    NeoForge.EVENT_BUS.register(com.zerog.neoessentials.listeners.CommandOverrideListener.class);
        
        // Register enhanced theme system event handlers (Phase 6)
    // Removed: TablistScoreboardManager does not have @SubscribeEvent methods and should not be registered on the event bus.
    // Removed: CustomBossbarManager is not an event listener
        
        // Register name tag formatting listener
        NeoForge.EVENT_BUS.register(new com.zerog.neoessentials.listeners.NameTagFormattingListener());
        LOGGER.info("Chat formatting system initialized");
        
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
            } catch (Exception e) {
                LOGGER.error("Error during NeoEssentials server starting setup", e);
            }
            // Initialize Enhanced Language System (Phase 4)  
            Path configPath = ConfigurationUnifier.getInstance().getConfigPath();
            LanguageManager.getInstance(configPath).initialize();
            LOGGER.info("Language System initialized");
            
            // Initialize all managers
            // Use simple economy manager that works with shop system
            com.zerog.neoessentials.managers.EconomyManager.getInstance();
            HomeManager.getInstance();
            WarpManager.getInstance();
            KitManager.getInstance();
            ModerationManager.getInstance();
            MessagingManager.getInstance();
            SpawnManager.getInstance();
            
            // Initialize additional critical managers
            TeleportRequestManager.getInstance();
            LOGGER.info("Teleport Request Manager initialized");
            
            com.zerog.neoessentials.storage.StorageManager.getInstance();
            LOGGER.info("Storage Manager initialized");
            
            com.zerog.neoessentials.security.SecurityManager.getInstance();
            LOGGER.info("Security Manager initialized");
            
            WebDashboardManager.getInstance();
            LOGGER.info("Web Dashboard Manager initialized");
            
            // Initialize performance monitoring managers
            com.zerog.neoessentials.performance.PerformanceManager.getInstance();
            LOGGER.info("Performance Manager initialized");

            
            com.zerog.neoessentials.performance.AsyncOperationManager.getInstance();
            LOGGER.info("Async Operation Manager initialized");
            
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
            
            // Note: TablistScoreboardManager handles all tablist functionality
            LOGGER.info("Tablist theme management ready");
            
            // Enhanced Bossbar Manager already initialized
            LOGGER.info("Enhanced Bossbar Manager ready");
            
            // GUI System removed - not needed for sign shops
            LOGGER.info("GUI system skipped - using sign-based shops only");
            
            // Configuration GUI System removed
            LOGGER.info("Config GUI system skipped - using command-based configuration");
            LOGGER.info("Configuration GUI Manager initialized");
            
            // Initialize Notification Manager
            NotificationManager notificationManager = NotificationManager.getInstance(ConfigurationUnifier.getInstance().getConfigManager().getMainConfig());
            notificationManager.notifyServerStart();
            
            // Initialize notification event listener
            NotificationEventListener.getInstance();

            // Initialize permission event listener for persistent storage
            NeoForge.EVENT_BUS.register(new com.zerog.neoessentials.listeners.PermissionEventListener());
            LOGGER.info("Permission Event Listener initialized");

            // Shop sign interactions are now handled by NeoEssentialsEventHandler
            // No need for separate ShopSignEventListener
            LOGGER.info("Shop Sign Event Handling consolidated into NeoEssentialsEventHandler");

                    // Initialize new PlaceholderManager
                    com.zerog.neoessentials.features.PlaceholderManager placeholderManager = new com.zerog.neoessentials.features.PlaceholderManager();
                    LOGGER.info("New PlaceholderManager initialized");

                    // Initialize new TabListManager
                    com.zerog.neoessentials.features.TabListManager tabListManager = new com.zerog.neoessentials.features.TabListManager();
                    LOGGER.info("New TabListManager initialized");

                    // Initialize new ScoreboardManager
                    com.zerog.neoessentials.features.ScoreboardManager scoreboardManager = new com.zerog.neoessentials.features.ScoreboardManager();
                    LOGGER.info("New ScoreboardManager initialized");

                    // Initialize new BossBarManager
                    com.zerog.neoessentials.features.BossBarManager bossBarManager = new com.zerog.neoessentials.features.BossBarManager();
                    LOGGER.info("New BossBarManager initialized");

                    // Wire managers to config and event hooks for dynamic updates
                    NeoForge.EVENT_BUS.register(new Object() {
                        @SubscribeEvent
                        public void onPlayerJoin(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
                            if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
                                String displayName = placeholderManager.parse(player, "%prefix% %player% %suffix%");
                                tabListManager.updateHeaderFooter(player, displayName);
                                tabListManager.updatePlayerEntry(player, displayName);
                                scoreboardManager.updateScoreboard(player, displayName);
                                bossBarManager.showBossBar(player, "Welcome to the server!", 1.0f, 0x00FF00);
                            }
                        }

                        @SubscribeEvent
                        public void onScoreUpdate(/* CustomScoreUpdateEvent event */) {
                            // Example: scoreboardManager.setPlayerScore(player, score);
                            // Implement your custom score update event and logic here
                        }

                        @SubscribeEvent
                        public void onBossBarEvent(/* CustomBossBarEvent event */) {
                            // Example: bossBarManager.showBossBar(player, title, progress, color);
                            // Implement your custom bossbar event and logic here
                        }
                    });
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
