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
 * @author ZeroG
 * @version 1.0.0
 */
@Mod(NeoEssentials.MODID)
public class NeoEssentials {
    /** The mod ID used for registration and resource locations */
    public static final String MODID = "neoessentials";
    
    /** The human-readable name of the mod */
    public static final String MOD_NAME = "NeoEssentials";
    
    /** Central logger for the mod */
    public static final Logger LOGGER = LogUtils.getLogger();
    
    private static NeoEssentials instance;
    private ModContainer modContainer;
    // Flag to track if config is loaded
    private boolean databaseConfigLoaded = false;
    
    // Scheduled executor for AFK checking
    private ScheduledExecutorService scheduler;

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public NeoEssentials(IEventBus modEventBus, ModContainer modContainer) {
        instance = this;
        this.modContainer = modContainer;
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);
        // Register config loading event handlers
        modEventBus.addListener(this::onConfigLoad);
        modEventBus.addListener(this::onConfigReady);

        // Register custom command argument types - now server-side only
        com.zerog.neoessentials.init.ModArgumentTypes.register(modEventBus);
        
        // Initialize network handler for server-side functionality in a modded environment
        com.zerog.neoessentials.network.NetworkHandler.init(modEventBus);
        
        // Check if we're on the physical server or client
        if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.DEDICATED_SERVER) {
            LOGGER.info("NeoEssentials initializing in DEDICATED SERVER environment - full server-side functionality enabled");
        } else if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) {
            LOGGER.info("NeoEssentials initializing in CLIENT environment - providing registry support for server compatibility");
            // On client, we primarily need to register things for synchronization
            // This ensures clients can connect to servers running NeoEssentials
        }
        
        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (NeoEssentials) to respond directly to events.
        NeoForge.EVENT_BUS.register(this);
        
        // Register the event handlers
        NeoForge.EVENT_BUS.register(com.zerog.neoessentials.events.EventHandler.class);
        NeoForge.EVENT_BUS.register(new com.zerog.neoessentials.events.PowerToolEventHandler());

        // Initialize our configuration system
        configManager = new com.zerog.neoessentials.config.ModConfigManager(this, modContainer);
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
        
        // Additional registrations that need to happen during common setup
        event.enqueueWork(() -> {
            LOGGER.info("Registering command argument types in common setup");
            // If needed, additional registrations could happen here
        });
        
        // Initialize storage manager
        initializeStorageManager();
        
        // Initialize data manager
        dataManager = new com.zerog.neoessentials.data.DataManager(this);
        
        // Initialize managers that rely on storage
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
    private com.zerog.neoessentials.config.ModConfigManager configManager;
    private com.zerog.neoessentials.data.DataManager dataManager;
    private com.zerog.neoessentials.commands.CommandManager commandManager;
    private com.zerog.neoessentials.storage.StorageManager storageManager;
    private boolean storageManagerInitialized = false;
    
    /**
     * Gets the config manager
     * 
     * @return The config manager
     */
    public com.zerog.neoessentials.config.ModConfigManager getConfigManager() {
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
     * Gets the storage manager
     * 
     * @return The storage manager
     */
    public com.zerog.neoessentials.storage.StorageManager getStorageManager() {
        return storageManager;
    }
    
    /**
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
        // Initialize managers that rely on data manager
        if (dataManager != null) {
            dataManager.initializeManagers();
        }
        
        // Only initialize storage manager if config is loaded, otherwise it will be initialized in onConfigLoad
        if (databaseConfigLoaded) {
            initializeStorageManager();
        } else {
            LOGGER.info("Deferring storage initialization until database config is loaded");
        }
        
        // Initialize data manager
        dataManager = new com.zerog.neoessentials.data.DataManager(this);
        dataManager.initialize();
        
        // Initialize command manager and register it with the event bus
        commandManager = new com.zerog.neoessentials.commands.CommandManager();
        NeoForge.EVENT_BUS.register(commandManager);
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
    
    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Store the server instance
        server = event.getServer();
        
        // Do something when the server starts
        LOGGER.info("NeoEssentials server-side mod activated!");
        LOGGER.info("Version: {} for Minecraft {}", getVersion(), net.minecraft.SharedConstants.getCurrentVersion().getName());
        
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
    
    public net.minecraft.server.MinecraftServer getServer() {
        return server;
    }
    
    private void registerDatabaseConfig() {
        // Database config is now registered in ModConfigManager
        LOGGER.info("Database configuration already registered through ModConfigManager");
    }
    
    /**
     * Gets the scheduler for async tasks
     * 
     * @return The scheduler
     */
    public ScheduledExecutorService getScheduler() {
        if (scheduler == null) {
            scheduler = Executors.newScheduledThreadPool(2);
        }
        return scheduler;
    }
}
