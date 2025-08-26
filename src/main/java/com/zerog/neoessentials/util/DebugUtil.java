package com.zerog.neoessentials.util;

import com.zerog.neoessentials.config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DebugUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(DebugUtil.class);

    public static void debugLog(String message) {
        boolean debugEnabled = false;
        try {
            debugEnabled = ConfigManager.getInstance().getMainConfig().debugMode;
        } catch (Exception e) {
            // fallback: do not log debug if config is missing
        }
        if (debugEnabled) {
            LOGGER.info("[DEBUG] " + message);
        }
    }

    public static void infoLog(String message) {
        boolean debugEnabled = false;
        try {
            debugEnabled = ConfigManager.getInstance().getMainConfig().debugMode;
        } catch (Exception e) {}
        if (debugEnabled) {
            LOGGER.info("[INFO] " + message);
        }
    }

    public static void warnLog(String message) {
        LOGGER.warn("[WARN] " + message);
    }

    public static void errorLog(String message, Throwable t) {
        LOGGER.error("[ERROR] " + message, t);
    }

    public static void errorLog(String message) {
        LOGGER.error("[ERROR] " + message);
    }
}
