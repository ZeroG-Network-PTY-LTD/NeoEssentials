package com.zerog.neoessentials;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.bus.api.SubscribeEvent;

/**
 * NeoEssentials - Essential server-side utilities for NeoForge
 * 
 * @author ZeroG
 * @version 2.0.0
 */
@Mod(NeoEssentials.MODID)
public class NeoEssentials {
    public static final String MODID = "neoessentials";
    public static final Logger LOGGER = LogUtils.getLogger();
    
    public NeoEssentials(IEventBus modEventBus, ModContainer modContainer) {
        // Register for setup events
        modEventBus.addListener(this::commonSetup);
        
        // Register for server events
        NeoForge.EVENT_BUS.register(this);
        
        LOGGER.info("NeoEssentials mod loading...");
    }
    
    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("NeoEssentials common setup complete");
    }
    
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("NeoEssentials server starting - ready to enhance your server experience!");
    }
}
