package com.zerog.neoessentials.discord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stub Discord Manager for compatibility
 * This class provides minimal functionality to maintain build compatibility
 */
public class DiscordManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(DiscordManager.class);
    private static DiscordManager instance;
    
    private DiscordManager() {
        // Private constructor
    }
    
    public static DiscordManager getInstance() {
        if (instance == null) {
            instance = new DiscordManager();
        }
        return instance;
    }
    
    public void initialize(String webhookUrl, String serverName, String avatarUrl) {
        LOGGER.info("Discord manager initialize called but Discord integration is disabled");
    }
}
