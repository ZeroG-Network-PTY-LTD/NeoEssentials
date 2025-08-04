package com.zerog.neoessentials.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WorldGuard Integration
 */
public class WorldGuardIntegration {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldGuardIntegration.class);
    private boolean available = false;
    
    public boolean initialize() {
        try {
            Class.forName("com.sk89q.worldguard.WorldGuard");
            available = true;
            LOGGER.info("WorldGuard integration initialized");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    public boolean isAvailable() { return available; }
    public void shutdown() { available = false; }
}
