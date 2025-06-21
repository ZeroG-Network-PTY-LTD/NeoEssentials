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

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(NeoEssentials.MODID)
public class NeoEssentials {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "neoessentials";
    public static final String MOD_NAME = "NeoEssentials";
    // Directly reference a slf4j logger
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
        // Register config loading event handler
        modEventBus.addListener(this::onConfigLoad);

        // Register custom command argument types
        com.zerog.neoessentials.init.ModArgumentTypes.register(modEventBus);
        
        // Check if we're on the physical server - this mod only works on servers
        if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.DEDICATED_SERVER) {
            LOGGER.info("NeoEssentials initializing in DEDICATED SERVER environment");
        } else {
            LOGGER.info("NeoEssentials is a server-side mod. Client-side features are limited.");
        }
        
        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (NeoEssentials) to respond directly to events.
        NeoForge.EVENT_BUS.register(this);
        
        // Register the event handlers
        NeoForge.EVENT_BUS.register(com.zerog.neoessentials.events.EventHandler.class);
        NeoForge.EVENT_BUS.register(new com.zerog.neoessentials.events.PowerToolEventHandler());

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC, "neoessentials-general.toml");
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

    private void commonSetup(FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");
        LOGGER.info("Initializing NeoEssentials managers");
        
        // Initialize storage manager
        initializeStorageManager();
        
        // Initialize data manager
        dataManager = new com.zerog.neoessentials.data.DataManager(this);
        
        // Initialize managers that rely on storage
        initializeManagers();
    }

    // This method is invoked by the server starting event
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("NeoEssentials server starting");
        
        MinecraftServer server = event.getServer();
        
        // Set up commands
        CommandManager.registerCommands(server);
        
        // Start scheduler for AFK tracking and other periodic tasks
        scheduler = Executors.newScheduledThreadPool(1);
        
        // Schedule timed tasks that should run once per minute
        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (dataManager != null) {
                    dataManager.runMinutelyTasks(server);
                }
            } catch (Exception e) {
                LOGGER.error("Error in NeoEssentials scheduled task", e);
            }
        }, 60, 60, TimeUnit.SECONDS);
    }

    /**
     * Event handler for when the server is stopping.
     * Used to save all data before the server shuts down.
     * 
     * @param event The server stopping event
     */
    @SubscribeEvent
    public static void onServerStopping(net.neoforged.neoforge.event.server.ServerStoppingEvent event) {
        LOGGER.info("Server stopping, saving all NeoEssentials data");
        
        if (instance != null) {
            // Shut down scheduler
            if (instance.scheduler != null) {
                instance.scheduler.shutdown();
                try {
                    if (!instance.scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                        instance.scheduler.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    LOGGER.error("Error shutting down scheduler", e);
                    instance.scheduler.shutdownNow();
                }
                instance.scheduler = null;
            }
            
            // Save all data and close database connections
            if (instance.dataManager != null) {
                instance.dataManager.saveAll();
            }
            
            if (instance.storageManager != null && instance.storageManagerInitialized) {
                instance.storageManager.shutdown();
            }
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
     * Gets the build number from the version string.
     * Version format: MAJOR.MINOR.PATCH.BUILD
     * 
     * @return The build number, or 0 if not found
     */
    public int getBuildNumber() {
        String version = getVersion();
        
        // Split by dots and get the last part
        String[] parts = version.split("\\.");
        if (parts.length >= 4) {
            try {
                return Integer.parseInt(parts[3]);
            } catch (NumberFormatException e) {
                LOGGER.warn("Could not parse build number from version: {}", version);
            }
        }
        
        return 0;
    }
    
    /**
     * Gets the singleton instance of the mod
     * 
     * @return The mod instance
     */
    public static NeoEssentials getInstance() {
        return instance;
    }
    
    /**
     * Gets the data manager
     * 
     * @return The data manager
     */
    public com.zerog.neoessentials.data.DataManager getDataManager() {
        return dataManager;
    }
    
    private com.zerog.neoessentials.data.DataManager dataManager;
    private com.zerog.neoessentials.storage.StorageManager storageManager;
    private boolean storageManagerInitialized = false;
    
    /**
     * Initializes the storage manager based on the config
     */
    private boolean initializeStorageManager() {
        // Check if storage is already initialized
        if (storageManagerInitialized) {
            return true;
        }
        
        // Initialize storage
        storageManager = new com.zerog.neoessentials.storage.StorageManager(this);
        boolean initSuccess = storageManager.initialize();
        
        if (initSuccess) {
            LOGGER.info("Storage manager initialized successfully");
            storageManagerInitialized = true;
        } else {
            LOGGER.error("Failed to initialize storage manager");
        }
        
        return initSuccess;
    }
    
    /**
     * Initializes the managers
     */
    private void initializeManagers() {
        // Initialize managers
        if (dataManager != null) {
            dataManager.initializeManagers();
        }
    }    
}
