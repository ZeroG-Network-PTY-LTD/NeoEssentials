package com.zerog.neoessentials.webdashboard.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks Discord bot connection state for SDLink integration
 */
public class SDLinkEventListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(SDLinkEventListener.class);
    private static boolean botReady = false;
    
    /**
     * Check if the Discord bot is ready
     */
    public static boolean isBotReady() {
        // Try to check SDLink's BotController if available
        try {
            Class<?> botControllerClass = Class.forName("com.hypherionmc.sdlink.core.discord.BotController");
            Object botController = botControllerClass.getField("INSTANCE").get(null);
            java.lang.reflect.Method isBotReadyMethod = botControllerClass.getMethod("isBotReady");
            Boolean ready = (Boolean) isBotReadyMethod.invoke(botController);
            botReady = ready != null && ready;
        } catch (Exception e) {
            // SDLink not available or bot not ready
            LOGGER.debug("SDLink BotController not available: {}", e.getMessage());
            botReady = false;
        }
        
        return botReady;
    }
    
    /**
     * Set bot ready state (called by integration code)
     */
    public static void setBotReady(boolean ready) {
        botReady = ready;
    }
}
