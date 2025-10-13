package com.zerog.neoessentials.util;

import com.zerog.neoessentials.config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Debug utility specifically for chat-related debugging
 */
public class ChatDebugUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatDebugUtil.class);
    
    /**
     * Print debug message if debug logging is enabled in config
     */
    public static void debug(String message) {
        if (ConfigManager.getInstance().isDebugLoggingEnabled()) {
            LOGGER.info("[CHAT DEBUG] {}", message);
        }
    }
    
    /**
     * Print debug message with formatted arguments if debug logging is enabled
     */
    public static void debug(String format, Object... args) {
        if (ConfigManager.getInstance().isDebugLoggingEnabled()) {
            LOGGER.info("[CHAT DEBUG] " + format, args);
        }
    }
}