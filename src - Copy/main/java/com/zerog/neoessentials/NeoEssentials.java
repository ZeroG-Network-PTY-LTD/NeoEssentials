package com.zerog.neoessentials;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;

import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

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

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public NeoEssentials(IEventBus modEventBus, ModContainer modContainer) {
        instance = this;
        this.modContainer = modContainer;
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

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
        NeoForge.EVENT_BUS.register(new com.zerog.neoessentials.events.EventHandler());

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC, "neoessentials-general.toml");
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");
        LOGGER.info("Initializing NeoEssentials managers");
        
        // Initialize the managers
        initializeManagers();

        if (Config.LOG_DIRT_BLOCK.getAsBoolean()) {
            LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));
        }

        LOGGER.info("{}{}", Config.MAGIC_NUMBER_INTRODUCTION.get(), Config.MAGIC_NUMBER.getAsInt());

        Config.ITEM_STRINGS.get().forEach((item) -> LOGGER.info("ITEM >> {}", item));
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
    private com.zerog.neoessentials.config.ConfigManager configManager;
    private com.zerog.neoessentials.data.DataManager dataManager;
    private com.zerog.neoessentials.commands.CommandManager commandManager;
    
    /**
     * Gets the config manager
     * 
     * @return The config manager
     */
    public com.zerog.neoessentials.config.ConfigManager getConfigManager() {
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
        // Initialize config manager first
        configManager = new com.zerog.neoessentials.config.ConfigManager();
        configManager.initialize();
        
        // Register database config with FML (separate file from main config)
        registerDatabaseConfig();
        
        // Initialize data manager
        dataManager = new com.zerog.neoessentials.data.DataManager();
        dataManager.initialize();
        
        // Initialize command manager and register it with the event bus
        commandManager = new com.zerog.neoessentials.commands.CommandManager();
        NeoForge.EVENT_BUS.register(commandManager);
    }
    
    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Store the server instance
        server = event.getServer();
        
        // Do something when the server starts
        LOGGER.info("NeoEssentials server-side mod activated!");
        LOGGER.info("Version: {} for Minecraft {}", getVersion(), net.minecraft.SharedConstants.getCurrentVersion().getName());
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
        
        // Save all data and close database connections
        if (dataManager != null) {
            dataManager.shutdown();
        }
    }
    
    // Server instance obtained from the ServerStartingEvent
    private net.minecraft.server.MinecraftServer server;
    
    /**
     * Gets the server instance
     * 
     * @return The server instance
     */
    public net.minecraft.server.MinecraftServer getServer() {
        return server;
    }
    
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
    private void registerDatabaseConfig() {
        if (configManager != null && configManager.getDatabaseConfig() != null) {
            // Register the database config with a different filename to avoid conflict
            modContainer.registerConfig(
                net.neoforged.fml.config.ModConfig.Type.COMMON,
                configManager.getDatabaseConfig().getSpec(),
                "neoessentials-database.toml"
            );
        }
    }
}
