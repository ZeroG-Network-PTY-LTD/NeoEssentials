package com.zerog.neoessentials.features.discord;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.config.DiscordConfig;

/**
 * Discord integration manager for NeoEssentials
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class DiscordManager {
    
    private final DiscordConfig config;
    
    public DiscordManager(DiscordConfig config) {
        this.config = config;
        
        NeoEssentials.LOGGER.info("Discord manager initialized");
    }
}
