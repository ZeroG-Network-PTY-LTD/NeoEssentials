package com.zerog.neoessentials.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DiscordSRV Integration
 */
public class DiscordSRVIntegration {
    private static final Logger LOGGER = LoggerFactory.getLogger(DiscordSRVIntegration.class);
    private boolean available = false;
    
    public boolean initialize() {
        try {
            Class.forName("github.scarsz.discordsrv.DiscordSRV");
            available = true;
            LOGGER.info("DiscordSRV integration initialized");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    public boolean isAvailable() { return available; }
    public void shutdown() { available = false; }
}
