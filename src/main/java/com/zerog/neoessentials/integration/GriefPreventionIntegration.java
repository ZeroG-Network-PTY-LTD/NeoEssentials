package com.zerog.neoessentials.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GriefPrevention Integration
 */
public class GriefPreventionIntegration {
    private static final Logger LOGGER = LoggerFactory.getLogger(GriefPreventionIntegration.class);
    private boolean available = false;
    
    public boolean initialize() {
        try {
            Class.forName("me.ryanhamshire.GriefPrevention.GriefPrevention");
            available = true;
            LOGGER.info("GriefPrevention integration initialized");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    public boolean isAvailable() { return available; }
    public void shutdown() { available = false; }
}
