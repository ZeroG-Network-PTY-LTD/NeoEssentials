package com.zerog.neoessentials.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Debug utility specifically for chat-related debugging.
 * All output goes through LOGGER.debug() — only visible when the logging
 * framework is configured to DEBUG level. No manual config gate needed.
 */
public class ChatDebugUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatDebugUtil.class);

    public static void debug(String message) {
        LOGGER.debug("[CHAT] {}", message);
    }

    public static void debug(String format, Object... args) {
        LOGGER.debug("[CHAT] " + format, args);
    }
}