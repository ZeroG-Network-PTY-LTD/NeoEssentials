package com.zerog.neoessentials;

import com.zerog.neoessentials.adapter.Forge1201AdapterFactory;
import com.zerog.neoessentials.common.adapter.AdapterFactory;
import com.zerog.neoessentials.common.adapter.ICommandAdapter;
import com.zerog.neoessentials.common.adapter.IPermissionAdapter;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * Main mod class for Forge 1.20.1 version
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
        
        LOGGER.info("NeoEssentials initializing for Minecraft 1.20.1 (Forge)");
        
        // Initialize the adapter factory
        adapterFactory = Forge1201AdapterFactory.getInstance();
        
        // Register event handlers
        modEventBus.addListener(this::setup);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarting);
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
