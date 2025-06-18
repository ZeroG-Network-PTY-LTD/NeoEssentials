package com.zerog.neoessentials;

import com.zerog.neoessentials.adapter.NeoForge1201AdapterFactory;
import com.zerog.neoessentials.common.adapter.AdapterFactory;
import com.zerog.neoessentials.common.adapter.ICommandAdapter;
import com.zerog.neoessentials.common.adapter.IPermissionAdapter;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

/**
 * Main mod class for NeoForge 1.20.1 version
 */
@Mod(NeoEssentials.MODID)
public class NeoEssentials {
    public static final String MODID = "neoessentials";
    public static final String MOD_NAME = "NeoEssentials";
    public static final Logger LOGGER = LogUtils.getLogger();
    
    private static NeoEssentials instance;
    private final AdapterFactory adapterFactory;
    
    public NeoEssentials(IEventBus modEventBus) {
        instance = this;
        
        LOGGER.info("NeoEssentials initializing for Minecraft 1.20.1 (NeoForge)");
        
        // Initialize the adapter factory
        adapterFactory = NeoForge1201AdapterFactory.getInstance();
        
        // Register event handlers
        modEventBus.addListener(this::setup);
        NeoForge.EVENT_BUS.addListener(this::onServerStarting);
    }
    
    private void setup(final FMLCommonSetupEvent event) {
        LOGGER.info("NeoEssentials common setup");
        
        // Initialize permission system
        IPermissionAdapter permissionAdapter = adapterFactory.getPermissionAdapter();
        permissionAdapter.initialize();
        
        // Initialize command system
        ICommandAdapter commandAdapter = adapterFactory.getCommandAdapter();
        commandAdapter.initialize();
    }
    
    private void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("NeoEssentials server starting");
        
        // Register commands
        ICommandAdapter commandAdapter = adapterFactory.getCommandAdapter();
        commandAdapter.registerAllCommands();
    }
    
    public static NeoEssentials getInstance() {
        return instance;
    }
    
    public AdapterFactory getAdapterFactory() {
        return adapterFactory;
    }
}
