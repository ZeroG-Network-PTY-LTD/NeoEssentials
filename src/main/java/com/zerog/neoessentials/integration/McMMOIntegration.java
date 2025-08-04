package com.zerog.neoessentials.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * McMMO Integration
 */
public class McMMOIntegration {
    private static final Logger LOGGER = LoggerFactory.getLogger(McMMOIntegration.class);
    private boolean available = false;
    
    public boolean initialize() {
        try {
            Class.forName("com.gmail.nossr50.mcMMO");
            available = true;
            LOGGER.info("McMMO integration initialized");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    public boolean isAvailable() { return available; }
    public void shutdown() { available = false; }
}
