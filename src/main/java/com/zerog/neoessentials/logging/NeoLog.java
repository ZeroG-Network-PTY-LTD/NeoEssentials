package com.zerog.neoessentials.logging;

import com.zerog.neoessentials.config.ConfigManager;
import org.slf4j.Logger;

/**
 * Central, per-category logging gate. Replaces the old DebugLogger/DebugUtil/ChatDebugUtil
 * trio with a single utility wired to {@code logging.categories.<category>.{normal,debug}}
 * in config.json.
 *
 * <p>{@link #info} / {@link #debug} are gated per {@link LogCategory} and can be disabled by
 * a server admin. {@link #warn} / {@link #error} are never gated — a category toggle can only
 * silence routine chatter, never a real warning or error.</p>
 *
 * <p>Debug-level messages already flow into {@code logs/debug.log} via the platform's default
 * Log4j2 setup; normal-level messages show in the console and {@code logs/latest.log}.</p>
 */
public final class NeoLog {

    private NeoLog() {
    }

    public static void info(Logger logger, LogCategory category, String format, Object... args) {
        if (isNormalEnabled(category)) {
            logger.info(format, args);
        }
    }

    public static void debug(Logger logger, LogCategory category, String format, Object... args) {
        if (isDebugEnabled(category)) {
            logger.debug(format, args);
        }
    }

    public static void debug(Logger logger, LogCategory category, String message, Throwable throwable) {
        if (isDebugEnabled(category)) {
            logger.debug(message, throwable);
        }
    }

    public static void warn(Logger logger, LogCategory category, String format, Object... args) {
        logger.warn(format, args);
    }

    public static void error(Logger logger, LogCategory category, String message, Throwable throwable) {
        logger.error(message, throwable);
    }

    public static void error(Logger logger, LogCategory category, String format, Object... args) {
        logger.error(format, args);
    }

    public static boolean isDebugEnabled(LogCategory category) {
        try {
            return ConfigManager.getInstance().isCategoryDebugEnabled(category);
        } catch (Exception e) {
            // Config not available yet (e.g. very early startup) — default to quiet.
            return false;
        }
    }

    public static boolean isNormalEnabled(LogCategory category) {
        try {
            return ConfigManager.getInstance().isCategoryNormalEnabled(category);
        } catch (Exception e) {
            return true;
        }
    }
}
