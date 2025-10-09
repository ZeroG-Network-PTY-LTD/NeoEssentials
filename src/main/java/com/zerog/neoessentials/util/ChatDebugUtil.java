package com.zerog.neoessentials.util;

import com.zerog.neoessentials.config.ConfigManager;

/**
 * Debug utility specifically for chat-related debugging
 */
public class ChatDebugUtil {
    
    /**
     * Print debug message if debug logging is enabled in config
     */
    public static void debug(String message) {
        if (ConfigManager.getInstance().isDebugLoggingEnabled()) {
            System.out.println("DEBUG: " + message);
        }
    }
    
    /**
     * Print debug message with formatted arguments if debug logging is enabled
     */
    public static void debug(String format, Object... args) {
        if (ConfigManager.getInstance().isDebugLoggingEnabled()) {
            System.out.println("DEBUG: " + String.format(format, args));
        }
    }
}