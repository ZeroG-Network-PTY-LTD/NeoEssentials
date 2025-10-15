package com.zerog.neoessentials.webdashboard.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class for monitoring Simple Discord Link bot status.
 * Uses polling-based approach to check BotController state without requiring compile-time dependency.
 * <p>
 * This class tracks:
 * - Discord bot connection status (ready/not ready)
 * - Player verification events (via verifiedaccounts.json file monitoring)
 * </p>
 * <p>
 * Note: This uses a simple polling approach instead of events to avoid annotation and reflection complications.
 * The SDLinkDataReader already handles file monitoring for verification changes.
 * </p>
 */
public class SDLinkEventListener {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(SDLinkEventListener.class);
    private static boolean botReady = false;
    private static boolean initialCheckDone = false;
    
    /**
     * Check if the Discord bot is ready using BotController reflection.
     * This method caches the result to avoid excessive reflection calls.
     * 
     * @return true if bot is connected and ready, false otherwise
     */
    public static boolean isBotReady() {
        // Do initial check on first call
        if (!initialCheckDone) {
            checkBotStatus();
            initialCheckDone = true;
        }
        return botReady;
    }
    
    /**
     * Force a re-check of the bot status.
     * Call this periodically or when you suspect the status has changed.
     */
    public static void checkBotStatus() {
        try {
            Class<?> botControllerClass = Class.forName("com.hypherionmc.sdlink.core.discord.BotController");
            Object botController = botControllerClass.getField("INSTANCE").get(null);
            java.lang.reflect.Method isBotReadyMethod = botControllerClass.getMethod("isBotReady");
            Boolean ready = (Boolean) isBotReadyMethod.invoke(botController);
            
            boolean wasReady = botReady;
            botReady = (ready != null && ready);
            
            // Log status change
            if (!wasReady && botReady) {
                LOGGER.info("===========================================");
                LOGGER.info("Simple Discord Link bot is now connected!");
                LOGGER.info("Discord integration is ready for dashboard authentication");
                LOGGER.info("===========================================");
            } else if (wasReady && !botReady) {
                LOGGER.warn("Simple Discord Link bot connection lost!");
            }
            
        } catch (ClassNotFoundException e) {
            LOGGER.debug("SDLink BotController not found - mod may not be installed");
            botReady = false;
        } catch (Exception e) {
            LOGGER.debug("Could not check SDLink bot status: {}", e.getMessage());
            botReady = false;
        }
    }
    
    /**
     * Reset the bot ready state (useful for testing or reloads).
     * Normally you shouldn't need to call this manually.
     */
    public static void resetBotReadyState() {
        botReady = false;
        initialCheckDone = false;
        LOGGER.debug("Bot ready state has been reset");
    }
}
