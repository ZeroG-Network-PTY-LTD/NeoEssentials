package com.zerog.neoessentials;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.zerog.neoessentials.commands.CommandManager;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;

import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
<<<<<<< HEAD
=======

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
>>>>>>> 9db1c98 (feat: Implement AFK commands and functionality, including auto-AFK detection and player status management)

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * NeoEssentials - A comprehensive server-side essentials mod for Minecraft NeoForge servers
 * <p>
 * This is the main class for the NeoEssentials mod. It handles initialization, event registration,
 * and serves as the central access point for mod functionality. NeoEssentials provides essential
 * server administration and player utility features including teleportation, economy, moderation tools,
 * and more, all while maintaining true server-side compatibility with vanilla clients.
 * </p>
 * 
<<<<<<< HEAD
 * @author ZeroG
 * @version 1.0.0
<<<<<<< HEAD
<<<<<<< HEAD
 * @since 2025-06-21
=======
>>>>>>> 44b5597 (feat: Add detailed documentation for NeoEssentials commands and main class)
=======
 * @since 2025-06-21
>>>>>>> 87dd29a (Enhanced JavaDoc documentation for release)
=======
 * @author ZeroG * @version 1.0.1
 * @since 2025-06-22
>>>>>>> e614394 (fix: Update version and release date in class documentation for NeoEssentials)
 */
@Mod(NeoEssentials.MODID)
public class NeoEssentials {
    /** The mod ID used for registration and resource locations */
    public static final String MODID = "neoessentials";
    
    /** The human-readable name of the mod */
    public static final String MOD_NAME = "NeoEssentials";
    
    /** Central logger for the mod */
    public static final Logger LOGGER = LogUtils.getLogger();
    
    /** Singleton instance of the mod */
    private static NeoEssentials instance;
    
    /** The mod container for this instance */
    private ModContainer modContainer;
<<<<<<< HEAD
<<<<<<< HEAD
    
    /** Flag to track if the database configuration has been loaded */
    private boolean databaseConfigLoaded = false;
    
    /** Scheduled executor service for periodic tasks like AFK checking */
<<<<<<< HEAD
    private ScheduledExecutorService scheduler;
=======
    // Flag to track if config is loaded
=======
    
    /** Flag to track if the database configuration has been loaded */
>>>>>>> 05e6600 (feat: Enhance documentation for NeoEssentials and command management classes)
    private boolean databaseConfigLoaded = false;
<<<<<<< HEAD
>>>>>>> da6a97e (chore: Update build number to 9 and timestamp in buildnumber.properties)
=======
    
    /** Scheduled executor service for periodic tasks like AFK checking */
    private ScheduledExecutorService scheduler;
>>>>>>> 9db1c98 (feat: Implement AFK commands and functionality, including auto-AFK detection and player status management)

    /**
=======
    private ScheduledExecutorService scheduler;    /**
>>>>>>> 9f2a583 (feat: Update README and configuration files with detailed tablist animation examples and new settings)
     * Main constructor for NeoEssentials
     * <p>
     * Initializes the mod instance, registers event handlers, and sets up
     * configuration handling. This is called automatically by the Forge Mod Loader.
     * </p>
     *
     * @param modEventBus The mod-specific event bus for initialization events
     * @param modContainer The container for this mod instance
     */
    public NeoEssentials(IEventBus modEventBus, ModContainer modContainer) {
        instance = this;
        this.modContainer = modContainer;
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);
<<<<<<< HEAD
<<<<<<< HEAD
        // Register config loading event handlers
        modEventBus.addListener(this::onConfigLoad);
        modEventBus.addListener(this::onConfigReady);
<<<<<<< HEAD
=======
        // Register config loading event handler
        modEventBus.addListener(this::onConfigLoad);
>>>>>>> da6a97e (chore: Update build number to 9 and timestamp in buildnumber.properties)
=======
        // Register config loading event handlers
        modEventBus.addListener(this::onConfigLoad);
        modEventBus.addListener(this::onConfigReady);
>>>>>>> 3caece6 (feat: Register additional config loading event handler for improved config management)
=======
        
        // Initialize ResourceManager to set up default configurations
        com.zerog.neoessentials.utils.ResourceManager.initialize();
>>>>>>> 9f2a583 (feat: Update README and configuration files with detailed tablist animation examples and new settings)

<<<<<<< HEAD
<<<<<<< HEAD
        // Register custom command argument types - now server-side only
        com.zerog.neoessentials.init.ModArgumentTypes.register(modEventBus);
        
        // Initialize network handler for server-side functionality in a modded environment
        com.zerog.neoessentials.network.NetworkHandler.init(modEventBus);
        
        // Check if we're on the physical server or client
=======
        // Register custom command argument types
        com.zerog.neoessentials.init.ModArgumentTypes.register(modEventBus);
        
<<<<<<< HEAD
        // Check if we're on the physical server - this mod only works on servers
>>>>>>> fddf77d (feat: Register custom command argument types during mod initialization)
        if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.DEDICATED_SERVER) {
            LOGGER.info("NeoEssentials initializing in DEDICATED SERVER environment - full server-side functionality enabled");
=======
        // Check if we're on the physical server or client
        if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.DEDICATED_SERVER) {
            LOGGER.info("NeoEssentials initializing in DEDICATED SERVER environment - full functionality enabled");
>>>>>>> 3ee6355 (feat: Update mod to support client-server synchronization and enhance compatibility)
=======
        // Register custom command argument types - now server-side only
        com.zerog.neoessentials.init.ModArgumentTypes.register(modEventBus);
        
        // Initialize network handler for server-side functionality in a modded environment
        com.zerog.neoessentials.network.NetworkHandler.init(modEventBus);
        
        // Check if we're on the physical server or client
        if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.DEDICATED_SERVER) {
            LOGGER.info("NeoEssentials initializing in DEDICATED SERVER environment - full server-side functionality enabled");
>>>>>>> 7e60483 (feat: Optimize NeoEssentials for server-only functionality and update documentation)
        } else if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) {
            LOGGER.info("NeoEssentials initializing in CLIENT environment - providing registry support for server compatibility");
            // On client, we primarily need to register things for synchronization
            // This ensures clients can connect to servers running NeoEssentials
        }
        
        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (NeoEssentials) to respond directly to events.
        NeoForge.EVENT_BUS.register(this);
        
        // Register the event handlers
<<<<<<< HEAD
<<<<<<< HEAD
        NeoForge.EVENT_BUS.register(com.zerog.neoessentials.events.EventHandler.class);
=======
        NeoForge.EVENT_BUS.register(new com.zerog.neoessentials.events.EventHandler());
>>>>>>> fc83e02 (feat: Implement PowerTool event handling for player interactions)
=======
        NeoForge.EVENT_BUS.register(com.zerog.neoessentials.events.EventHandler.class);
>>>>>>> e2153e5 (fix: Improve event registration and storage manager initialization in NeoEssentials)
        NeoForge.EVENT_BUS.register(new com.zerog.neoessentials.events.PowerToolEventHandler());

        // Initialize our configuration system
        configManager = new com.zerog.neoessentials.config.ModConfigManager(this, modContainer);
<<<<<<< HEAD
    }
    
    /**
     * Config loading event handler. Called when configs are loaded or reloaded.
     */
    private void onConfigLoad(final ModConfigEvent.Loading event) {
        ModConfig config = event.getConfig();
        if (config.getFileName().contains("neoessentials-database.toml")) {
            LOGGER.info("Database configuration loaded");
            databaseConfigLoaded = true;
            // If already at or past common setup, initialize storage now
            if (storageManager != null && !storageManagerInitialized) {
                boolean success = initializeStorageManager();
                
                // Reload data from storage if initialization was successful and data manager is available
                if (success && dataManager != null) {
                    LOGGER.info("Storage manager initialized, reloading data");
                    dataManager.reloadFromStorage();
                }
            }
        }
    }

    // Track if configs have been initialized
    private boolean configsInitialized = false;
    private int configsLoaded = 0;
    private static final int EXPECTED_CONFIG_FILES = 7; // general, economy, homes, warps, kits, tablist, database
    
    /**
     * Config loading event handler
     */
    private void onConfigReady(final ModConfigEvent.Loading event) {
        String fileName = event.getConfig().getFileName();
        LOGGER.info("Config loaded: " + fileName);
        
        if (fileName.contains("neoessentials/")) {
            configsLoaded++;
            LOGGER.info("NeoEssentials config loaded: " + configsLoaded + " of " + EXPECTED_CONFIG_FILES);
        }
        
        // Wait until all expected config files are loaded before initializing
        if (!configsInitialized && configsLoaded >= EXPECTED_CONFIG_FILES) {
            if (configManager != null) {
                LOGGER.info("All config files loaded, initializing configs now");
                // Add a small delay to ensure configs are fully processed
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // Ignore
                }
                
                configManager.initializeConfigs();
                configsInitialized = true;
            }
        }
=======
>>>>>>> 7409b6f (feat: Add comprehensive configuration management for NeoEssentials, including database, economy, home, kit, warp, and tablist settings)
    }
    
    /**
     * Config loading event handler. Called when configs are loaded or reloaded.
     */
    private void onConfigLoad(final ModConfigEvent.Loading event) {
        ModConfig config = event.getConfig();
        if (config.getFileName().contains("neoessentials-database.toml")) {
            LOGGER.info("Database configuration loaded");
            databaseConfigLoaded = true;
            // If already at or past common setup, initialize storage now
            if (storageManager != null && !storageManagerInitialized) {
                boolean success = initializeStorageManager();
                
                // Reload data from storage if initialization was successful and data manager is available
                if (success && dataManager != null) {
                    LOGGER.info("Storage manager initialized, reloading data");
                    dataManager.reloadFromStorage();
                }
            }
        }
    }

    // Track if configs have been initialized
    private boolean configsInitialized = false;
    private int configsLoaded = 0;
    private static final int EXPECTED_CONFIG_FILES = 7; // general, economy, homes, warps, kits, tablist, database
    
    /**
     * Config loading event handler
     */
    private void onConfigReady(final ModConfigEvent.Loading event) {
        String fileName = event.getConfig().getFileName();
        LOGGER.info("Config loaded: " + fileName);
        
        if (fileName.contains("neoessentials/")) {
            configsLoaded++;
            LOGGER.info("NeoEssentials config loaded: " + configsLoaded + " of " + EXPECTED_CONFIG_FILES);
        }
        
        // Wait until all expected config files are loaded before initializing
        if (!configsInitialized && configsLoaded >= EXPECTED_CONFIG_FILES) {
            if (configManager != null) {
                LOGGER.info("All config files loaded, initializing configs now");
                // Add a small delay to ensure configs are fully processed
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // Ignore
                }
                
                configManager.initializeConfigs();
                configsInitialized = true;
            }
        }
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");
        LOGGER.info("Initializing NeoEssentials managers");
        
        // Initialize FTB Registry compatibility helpers
        com.zerog.neoessentials.compat.FTBRegistryCompat.init();
        
        // Additional registrations that need to happen during common setup
        event.enqueueWork(() -> {
            LOGGER.info("Registering command argument types in common setup");
            // If needed, additional registrations could happen here
        });
        
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> 7409b6f (feat: Add comprehensive configuration management for NeoEssentials, including database, economy, home, kit, warp, and tablist settings)
        // Initialize storage manager
        initializeStorageManager();
        
        // Initialize data manager
        dataManager = new com.zerog.neoessentials.data.DataManager(this);
        
        // Initialize managers that rely on storage
<<<<<<< HEAD
=======
        // Initialize the managers
>>>>>>> fddf77d (feat: Register custom command argument types during mod initialization)
=======
>>>>>>> 7409b6f (feat: Add comprehensive configuration management for NeoEssentials, including database, economy, home, kit, warp, and tablist settings)
        initializeManagers();
    }

    /**
     * Gets the instance of the mod
     * 
     * @return The instance of the mod
     */
    public static NeoEssentials getInstance() {
        return instance;
    }
    
    // Fields for the essentials managers
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> 7409b6f (feat: Add comprehensive configuration management for NeoEssentials, including database, economy, home, kit, warp, and tablist settings)
    private com.zerog.neoessentials.config.ModConfigManager configManager;
    private com.zerog.neoessentials.data.DataManager dataManager;
    private com.zerog.neoessentials.commands.CommandManager commandManager;
    private com.zerog.neoessentials.storage.StorageManager storageManager;
    private boolean storageManagerInitialized = false;
<<<<<<< HEAD
=======
    private com.zerog.neoessentials.config.ConfigManager configManager;
    private com.zerog.neoessentials.data.DataManager dataManager;
    private com.zerog.neoessentials.commands.CommandManager commandManager;
<<<<<<< HEAD
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
    private com.zerog.neoessentials.storage.StorageManager storageManager;
>>>>>>> 73a32aa (Implement SQLite storage handler and associated factory and manager classes)
=======
>>>>>>> da6a97e (chore: Update build number to 9 and timestamp in buildnumber.properties)
    
    /**
     * Gets the config manager
     * 
     * @return The config manager
     */
<<<<<<< HEAD
<<<<<<< HEAD
    public com.zerog.neoessentials.config.ModConfigManager getConfigManager() {
=======
    public com.zerog.neoessentials.config.ConfigManager getConfigManager() {
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
    public com.zerog.neoessentials.config.ModConfigManager getConfigManager() {
>>>>>>> 7409b6f (feat: Add comprehensive configuration management for NeoEssentials, including database, economy, home, kit, warp, and tablist settings)
        return configManager;
    }
    
    /**
     * Gets the data manager
     * 
     * @return The data manager
     */
    public com.zerog.neoessentials.data.DataManager getDataManager() {
        return dataManager;
    }
    
    /**
     * Gets the command manager
     * 
     * @return The command manager
     */
    public com.zerog.neoessentials.commands.CommandManager getCommandManager() {
        return commandManager;
    }
    
    /**
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> 73a32aa (Implement SQLite storage handler and associated factory and manager classes)
     * Gets the storage manager
     * 
     * @return The storage manager
     */
    public com.zerog.neoessentials.storage.StorageManager getStorageManager() {
        return storageManager;
    }
    
    /**
<<<<<<< HEAD
=======
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
>>>>>>> 73a32aa (Implement SQLite storage handler and associated factory and manager classes)
     * Gets the mod container
     * 
     * @return The mod container
     */
    public ModContainer getModContainer() {
        return modContainer;
    }
      /**
     * Initialize the managers
     */
    public void initializeManagers() {
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> 7409b6f (feat: Add comprehensive configuration management for NeoEssentials, including database, economy, home, kit, warp, and tablist settings)
        // Initialize managers that rely on data manager
        if (dataManager != null) {
            dataManager.initializeManagers();
        }
<<<<<<< HEAD
        
        // Only initialize storage manager if config is loaded, otherwise it will be initialized in onConfigLoad
        if (databaseConfigLoaded) {
            initializeStorageManager();
        } else {
            LOGGER.info("Deferring storage initialization until database config is loaded");
        }
        
        // Initialize data manager
        dataManager = new com.zerog.neoessentials.data.DataManager(this);
=======
        // Initialize config manager first
        configManager = new com.zerog.neoessentials.config.ConfigManager();
        configManager.initialize();
        
        // Register database config with FML (separate file from main config)
        registerDatabaseConfig();
        
        // Just create the storage manager instance without trying to access config values yet
        // Create a default fallback config for the storage manager
        com.zerog.neoessentials.config.DatabaseConfig fallbackConfig = new com.zerog.neoessentials.config.DatabaseConfig();
        fallbackConfig.initialize(); // Initialize to ensure defaults are set
        storageManager = new com.zerog.neoessentials.storage.StorageManager(fallbackConfig);
=======
>>>>>>> 7409b6f (feat: Add comprehensive configuration management for NeoEssentials, including database, economy, home, kit, warp, and tablist settings)
        
        // Only initialize storage manager if config is loaded, otherwise it will be initialized in onConfigLoad
        if (databaseConfigLoaded) {
            initializeStorageManager();
        } else {
            LOGGER.info("Deferring storage initialization until database config is loaded");
        }
        
        // Initialize data manager
<<<<<<< HEAD
        dataManager = new com.zerog.neoessentials.data.DataManager();
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
        dataManager = new com.zerog.neoessentials.data.DataManager(this);
>>>>>>> ecf8e9a (feat: Refactor DataManager initialization and loading process for improved data handling)
        dataManager.initialize();
        
        // Initialize permission handler manager
        initializePermissionHandlers();
        
        // Initialize command manager and register it with the event bus
        commandManager = new com.zerog.neoessentials.commands.CommandManager();
        NeoForge.EVENT_BUS.register(commandManager);
    }
    
<<<<<<< HEAD
<<<<<<< HEAD
    /**
     * Initialize the permission handlers
     */
    private void initializePermissionHandlers() {
        try {
            LOGGER.info("Initializing permission handlers");
            // This will register all available permission handlers (LuckPerms, FTB Ranks, etc.)
            com.zerog.neoessentials.permissions.PermissionHandlerManager.getInstance();
        } catch (Exception e) {
            LOGGER.error("Error initializing permission handlers", e);
        }
    }
    
    /**
     * Initialize the storage manager (called after database config is loaded)
     * 
     * @return True if initialization was successful, false otherwise
     */
    private boolean initializeStorageManager() {
        if (storageManager != null && !storageManagerInitialized) {
            LOGGER.info("Initializing storage manager now that database config is loaded");
            boolean success = storageManager.initialize();
            
            if (success) {
                LOGGER.info("Storage manager successfully initialized");
                storageManagerInitialized = true;
                return true;
            } else {
                LOGGER.error("Failed to initialize storage manager");
                // Use a fallback JSON storage handler if the configured one fails
                LOGGER.info("Attempting to use fallback JSON storage as a backup");
                
                // Create a fallback config with JSON storage
                com.zerog.neoessentials.config.DatabaseConfig fallbackConfig = new com.zerog.neoessentials.config.DatabaseConfig();
                fallbackConfig.storageType.set(com.zerog.neoessentials.config.StorageType.JSON);
                
                // Replace the storage manager
                storageManager = new com.zerog.neoessentials.storage.StorageManager(fallbackConfig);
                boolean fallbackSuccess = storageManager.initialize();
                
                if (fallbackSuccess) {
                    LOGGER.info("Fallback JSON storage manager initialized successfully");
                    storageManagerInitialized = true;
                    return true;
                } else {
                    LOGGER.error("Failed to initialize even the fallback storage manager");
                    return false;
                }
            }
        }
        
        return storageManagerInitialized;
    }
    
    /**
     * Server starting event handler
     * <p>
     * This method is called when the server starts up. It initializes server-specific
     * features including the AFK checker and stores the server instance for later use.
     * This is the main initialization point for server-side functionality.
     * </p>
     *
     * @param event The server starting event
<<<<<<< HEAD
     */
<<<<<<< HEAD
=======
=======
    /**
     * Initialize the storage manager (called after database config is loaded)
     * 
     * @return True if initialization was successful, false otherwise
     */
    private boolean initializeStorageManager() {
        if (storageManager != null && !storageManagerInitialized) {
            LOGGER.info("Initializing storage manager now that database config is loaded");
            boolean success = storageManager.initialize();
            
            if (success) {
                LOGGER.info("Storage manager successfully initialized");
                storageManagerInitialized = true;
                return true;
            } else {
                LOGGER.error("Failed to initialize storage manager");
                // Use a fallback JSON storage handler if the configured one fails
                LOGGER.info("Attempting to use fallback JSON storage as a backup");
                
                // Create a fallback config with JSON storage
                com.zerog.neoessentials.config.DatabaseConfig fallbackConfig = new com.zerog.neoessentials.config.DatabaseConfig();
                fallbackConfig.storageType.set(com.zerog.neoessentials.config.StorageType.JSON);
                
                // Replace the storage manager
                storageManager = new com.zerog.neoessentials.storage.StorageManager(fallbackConfig);
                boolean fallbackSuccess = storageManager.initialize();
                
                if (fallbackSuccess) {
                    LOGGER.info("Fallback JSON storage manager initialized successfully");
                    storageManagerInitialized = true;
                    return true;
                } else {
                    LOGGER.error("Failed to initialize even the fallback storage manager");
                    return false;
                }
            }
        }
        
        return storageManagerInitialized;
    }
    
>>>>>>> da6a97e (chore: Update build number to 9 and timestamp in buildnumber.properties)
    // You can use SubscribeEvent and let the Event Bus discover methods to call
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
>>>>>>> 05e6600 (feat: Enhance documentation for NeoEssentials and command management classes)
    @SubscribeEvent
=======
     */    @SubscribeEvent
>>>>>>> 30dc8b4 (feat: Refactor tablist management to improve server reference handling and placeholder processing)
    public void onServerStarting(ServerStartingEvent event) {
        // Store the server instance
        server = event.getServer();
        
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> 05e6600 (feat: Enhance documentation for NeoEssentials and command management classes)
        // Log mod activation
        LOGGER.info("NeoEssentials server-side mod activated!");
        LOGGER.info("Version: {} for Minecraft {}", getVersion(), net.minecraft.SharedConstants.getCurrentVersion().getName());
        
        // Update the server reference in EnhancedTablistManager if it exists
        if (dataManager != null && dataManager.getTablistManager() != null) {
            LOGGER.info("Updating server reference in EnhancedTablistManager");
            dataManager.getTablistManager().setServer(server);
        } else {
            LOGGER.info("TablistManager not yet initialized, server reference will be set during initialization");
        }
        
        // Initialize the AFK checker task
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (commandManager != null && commandManager.getAfkCommands() != null) {
                    commandManager.getAfkCommands().checkForInactivePlayers();
                }
            } catch (Exception e) {
                LOGGER.error("Error in AFK checker task", e);
            }
        }, 60, 60, TimeUnit.SECONDS); // Check every minute
=======
        // Do something when the server starts
        LOGGER.info("NeoEssentials server-side mod activated!");
        LOGGER.info("Version: {} for Minecraft {}", getVersion(), net.minecraft.SharedConstants.getCurrentVersion().getName());
<<<<<<< HEAD
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
        
        // Initialize the AFK checker task
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (commandManager != null && commandManager.getAfkCommands() != null) {
                    commandManager.getAfkCommands().checkForInactivePlayers();
                }
            } catch (Exception e) {
                LOGGER.error("Error in AFK checker task", e);
            }
        }, 60, 60, TimeUnit.SECONDS); // Check every minute
>>>>>>> 9db1c98 (feat: Implement AFK commands and functionality, including auto-AFK detection and player status management)
    }
    
    /**
     * Event handler for when the server is stopping.
     * Used to save all data before the server shuts down.
     * 
     * @param event The server stopping event
     */
    @SubscribeEvent
    public void onServerStopping(net.neoforged.neoforge.event.server.ServerStoppingEvent event) {
        LOGGER.info("Server stopping, saving all NeoEssentials data");
        
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> 9db1c98 (feat: Implement AFK commands and functionality, including auto-AFK detection and player status management)
        // Shut down scheduler
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                LOGGER.error("Error shutting down scheduler", e);
                scheduler.shutdownNow();
            }
            scheduler = null;
        }
        
        // Save all data and close database connections
        if (dataManager != null) {
            dataManager.saveAll();
        }
        
        if (storageManager != null && storageManagerInitialized) {
            storageManager.shutdown();
        }
    }
    
    /**
     * Gets the version of the mod from the mods.toml file
     * 
     * @return The version of the mod
     */
    public String getVersion() {
        return modContainer.getModInfo().getVersion().toString();
    }
    
    /**
     * Gets the Minecraft server instance
     * 
     * @return The server instance
     */
    private net.minecraft.server.MinecraftServer server;
    
=======
        // Save all data and close database connections
        if (dataManager != null) {
            dataManager.saveAll();
        }
        
        if (storageManager != null && storageManagerInitialized) {
            storageManager.shutdown();
        }
    }
    
    /**
     * Gets the version of the mod from the mods.toml file
     * 
     * @return The version of the mod
     */
    public String getVersion() {
        return modContainer.getModInfo().getVersion().toString();
    }
    
    /**
     * Gets the Minecraft server instance
     * 
     * @return The server instance
     */
<<<<<<< HEAD
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
    private net.minecraft.server.MinecraftServer server;
    
>>>>>>> da6a97e (chore: Update build number to 9 and timestamp in buildnumber.properties)
    public net.minecraft.server.MinecraftServer getServer() {
        return server;
    }
    
<<<<<<< HEAD
<<<<<<< HEAD
    private void registerDatabaseConfig() {
        // Database config is now registered in ModConfigManager
        LOGGER.info("Database configuration already registered through ModConfigManager");
    }
    
    /**
<<<<<<< HEAD
<<<<<<< HEAD
     * Gets the scheduler for async tasks
     * 
     * @return The scheduler
     */
    public ScheduledExecutorService getScheduler() {
        if (scheduler == null) {
            scheduler = Executors.newScheduledThreadPool(2);
        }
        return scheduler;
=======
    /**
     * Gets the mod version from the ModContainer
     * 
     * @return The mod version string
     */
    public String getVersion() {
        return modContainer != null ? modContainer.getModInfo().getVersion().toString() : "unknown";
    }
    
    /**
     * Register the database config with FML
     */
=======
>>>>>>> da6a97e (chore: Update build number to 9 and timestamp in buildnumber.properties)
    private void registerDatabaseConfig() {
<<<<<<< HEAD
        if (configManager != null && configManager.getDatabaseConfig() != null) {
            // Register the database config with a different filename to avoid conflict
            modContainer.registerConfig(
                net.neoforged.fml.config.ModConfig.Type.COMMON,
                configManager.getDatabaseConfig().getSpec(),
                "neoessentials-database.toml"
            );
        }
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
        // Database config is now registered in ModConfigManager
        LOGGER.info("Database configuration already registered through ModConfigManager");
>>>>>>> 2ac7252 (feat: Enhance DataManager with initialization and data loading methods for improved manager setup)
=======
     * Gets the scheduler
=======
     * Gets the scheduler for async tasks
     * 
>>>>>>> 6528176 (feat: Enhance scheduler handling and improve configuration management in NeoEssentials)
     * @return The scheduler
     */
    public ScheduledExecutorService getScheduler() {
        if (scheduler == null) {
            scheduler = Executors.newScheduledThreadPool(2);
        }
        return scheduler;
>>>>>>> 2c0e119 (feat: Add compatibility layer for legacy config structure and enhance DataManager with scheduler integration)
    }
    
    /**
     * Check if debug mode is enabled in the configuration
     * 
     * @return true if debug mode is enabled, false otherwise
     */
    public static boolean isDebugMode() {
        try {
            if (instance != null && instance.configManager != null) {
                return com.zerog.neoessentials.config.GeneralConfig.DEBUG_MODE.get();
            }
        } catch (Exception e) {
            LOGGER.debug("Error checking debug mode status", e);
        }
        return false;
    }
}
