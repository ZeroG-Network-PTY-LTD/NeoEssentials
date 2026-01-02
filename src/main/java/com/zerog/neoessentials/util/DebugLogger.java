package com.zerog.neoessentials.util;

import com.zerog.neoessentials.config.ConfigManager;
import org.slf4j.Logger;

/**
 * Centralized debug logging utility that respects the enableDebugLogging configuration.
 * 
 * <p>This utility ensures that all debug logging in NeoEssentials is controlled by the
 * {@code logging.enableDebugLogging} configuration option in config.json.</p>
 * 
 * <p>Usage:</p>
 * <pre>{@code
 * // Instead of:
 * LOGGER.debug("Debug message: {}", value);
 * 
 * // Use:
 * DebugLogger.log(LOGGER, "Debug message: {}", value);
 * }</pre>
 * 
 * <p>Configuration in config.json:</p>
 * <pre>{@code
 * {
 *   "logging": {
 *     "enableDebugLogging": false
 *   }
 * }
 * }</pre>
 * 
 * @since 1.0.2.2-HotFix
 */
public class DebugLogger {
    
    /**
     * Log a debug message if debug logging is enabled in configuration.
     * 
     * @param logger The logger instance to use
     * @param message The debug message
     */
    public static void log(Logger logger, String message) {
        if (isDebugEnabled()) {
            logger.debug(message);
        }
    }
    
    /**
     * Log a formatted debug message if debug logging is enabled in configuration.
     * 
     * @param logger The logger instance to use
     * @param format The message format string
     * @param arg The argument for the format string
     */
    public static void log(Logger logger, String format, Object arg) {
        if (isDebugEnabled()) {
            logger.debug(format, arg);
        }
    }
    
    /**
     * Log a formatted debug message with two arguments if debug logging is enabled.
     * 
     * @param logger The logger instance to use
     * @param format The message format string
     * @param arg1 The first argument
     * @param arg2 The second argument
     */
    public static void log(Logger logger, String format, Object arg1, Object arg2) {
        if (isDebugEnabled()) {
            logger.debug(format, arg1, arg2);
        }
    }
    
    /**
     * Log a formatted debug message with three arguments if debug logging is enabled.
     * 
     * @param logger The logger instance to use
     * @param format The message format string
     * @param arg1 The first argument
     * @param arg2 The second argument
     * @param arg3 The third argument
     */
    public static void log(Logger logger, String format, Object arg1, Object arg2, Object arg3) {
        if (isDebugEnabled()) {
            logger.debug(format, arg1, arg2, arg3);
        }
    }
    
    /**
     * Log a formatted debug message with variable arguments if debug logging is enabled.
     * 
     * @param logger The logger instance to use
     * @param format The message format string
     * @param arguments The arguments for the format string
     */
    public static void log(Logger logger, String format, Object... arguments) {
        if (isDebugEnabled()) {
            logger.debug(format, arguments);
        }
    }
    
    /**
     * Log a debug message with an exception if debug logging is enabled.
     * 
     * @param logger The logger instance to use
     * @param message The debug message
     * @param throwable The exception to log
     */
    public static void log(Logger logger, String message, Throwable throwable) {
        if (isDebugEnabled()) {
            logger.debug(message, throwable);
        }
    }
    
    /**
     * Check if debug logging is currently enabled.
     * 
     * @return true if debug logging is enabled in configuration
     */
    public static boolean isDebugEnabled() {
        try {
            return ConfigManager.getInstance().isDebugLoggingEnabled();
        } catch (Exception e) {
            // If config is not available (e.g., during early initialization),
            // default to disabled for safety
            return false;
        }
    }
    
    /**
     * Log a debug message with explicit enabled check.
     * Useful when you want to avoid string construction if debug is disabled.
     * 
     * <pre>{@code
     * if (DebugLogger.isDebugEnabled()) {
     *     DebugLogger.logUnchecked(LOGGER, "Expensive operation: " + expensiveMethod());
     * }
     * }</pre>
     * 
     * @param logger The logger instance to use
     * @param message The debug message
     */
    public static void logUnchecked(Logger logger, String message) {
        logger.debug(message);
    }
    
    /**
     * Log a formatted debug message with explicit enabled check.
     * 
     * @param logger The logger instance to use
     * @param format The message format string
     * @param arguments The arguments for the format string
     */
    public static void logUnchecked(Logger logger, String format, Object... arguments) {
        logger.debug(format, arguments);
    }
}

