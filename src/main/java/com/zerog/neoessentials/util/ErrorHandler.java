package com.zerog.neoessentials.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import com.zerog.neoessentials.localization.LanguageManager;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.util.MessageUtil;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Centralized error handling utility for NeoEssentials
 * Provides consistent error logging, user feedback, and error tracking
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class ErrorHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ErrorHandler.class);
    
    // Error tracking for debugging and monitoring
    private static final ConcurrentHashMap<String, AtomicLong> errorCounts = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Long> lastErrorTime = new ConcurrentHashMap<>();
    
    // Error categories
    public enum ErrorCategory {
        COMMAND_EXECUTION("Command Execution"),
        DATA_STORAGE("Data Storage"),
        PLAYER_MANAGEMENT("Player Management"),
        ECONOMY("Economy System"),
        TELEPORTATION("Teleportation"),
        PERMISSIONS("Permissions"),
        CONFIGURATION("Configuration"),
        PLACEHOLDER("Placeholder System"),
        LANGUAGE("Language System"),
        NETWORK("Network Operations"),
        VALIDATION("Input Validation"),
        INITIALIZATION("System Initialization"),
        FILE_IO("File Operations"),
        DATABASE("Database Operations"),
        API_INTEGRATION("API Integration"),
        UNKNOWN("Unknown");
        
        private final String displayName;
        
        ErrorCategory(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    // Error severity levels
    public enum ErrorSeverity {
        LOW("Low", "Minor issue that doesn't affect functionality"),
        MEDIUM("Medium", "Issue that may affect some functionality"),
        HIGH("High", "Serious issue that affects core functionality"),
        CRITICAL("Critical", "Critical error that may cause system instability");
        
        private final String level;
        private final String description;
        
        ErrorSeverity(String level, String description) {
            this.level = level;
            this.description = description;
        }
        
        public String getLevel() {
            return level;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * Handle an exception with context and user feedback
     */
    public static void handleError(ErrorCategory category, ErrorSeverity severity, 
                                 String operation, Exception exception, ServerPlayer player) {
        String errorKey = category.name() + ":" + operation;
        
        // Track error occurrence
        errorCounts.computeIfAbsent(errorKey, k -> new AtomicLong(0)).incrementAndGet();
        lastErrorTime.put(errorKey, System.currentTimeMillis());
        
        // Log error with context
        String logMessage = String.format("[%s][%s] Error in %s: %s", 
            category.getDisplayName(), severity.getLevel(), operation, exception.getMessage());
        
        switch (severity) {
            case LOW -> LOGGER.debug(logMessage, exception);
            case MEDIUM -> LOGGER.warn(logMessage, exception);
            case HIGH, CRITICAL -> LOGGER.error(logMessage, exception);
        }
        
        // Notify player if applicable
        if (player != null && severity != ErrorSeverity.LOW) {
            notifyPlayer(player, category, operation, severity);
        }
        
        // Additional handling for critical errors
        if (severity == ErrorSeverity.CRITICAL) {
            handleCriticalError(category, operation, exception);
        }
    }
    
    /**
     * Handle an exception with context (no player notification)
     */
    public static void handleError(ErrorCategory category, ErrorSeverity severity, 
                                 String operation, Exception exception) {
        handleError(category, severity, operation, exception, null);
    }
    
    /**
     * Handle a validation error with user-friendly feedback
     */
    public static void handleValidationError(String operation, String details, ServerPlayer player) {
        LOGGER.debug("Validation error in {}: {}", operation, details);
        
        if (player != null) {
            try {
                String message = LanguageManager.getInstance()
                    .getMessage(player, "neoessentials.error.validation", operation, details);
                MessageUtil.sendMessage(player, message);
            } catch (Exception e) {
                // Fallback message if language system fails
                player.sendSystemMessage(MessageUtil.translatable(player, "neoessentials.error.validation", details));
            }
        }
    }
    
    /**
     * Handle a permission error
     */
    public static void handlePermissionError(String operation, ServerPlayer player) {
        LOGGER.debug("Permission denied for player {} in operation: {}", 
            player != null ? player.getName().getString() : "unknown", operation);
        
        if (player != null) {
            try {
                String message = LanguageManager.getInstance()
                    .getMessage(player, "neoessentials.error.permission", operation);
                MessageUtil.sendMessage(player, message);
            } catch (Exception e) {
                // Fallback message
                player.sendSystemMessage(MessageUtil.translatable(player, "neoessentials.error.no_permission"));
            }
        }
    }
    
    /**
     * Safe execution wrapper that handles exceptions
     */
    public static <T> T safeExecute(ErrorCategory category, String operation, 
                                   Supplier<T> supplier, T defaultValue) {
        try {
            return supplier.get();
        } catch (Exception e) {
            handleError(category, ErrorSeverity.MEDIUM, operation, e);
            return defaultValue;
        }
    }
    
    /**
     * Safe execution wrapper for void operations
     */
    public static void safeExecute(ErrorCategory category, String operation, Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            handleError(category, ErrorSeverity.MEDIUM, operation, e);
        }
    }
    
    /**
     * Safe execution with player context
     */
    public static <T> T safeExecute(ErrorCategory category, String operation, 
                                   Supplier<T> supplier, T defaultValue, ServerPlayer player) {
        try {
            return supplier.get();
        } catch (Exception e) {
            handleError(category, ErrorSeverity.MEDIUM, operation, e, player);
            return defaultValue;
        }
    }
    
    /**
     * Notify player about an error
     */
    private static void notifyPlayer(ServerPlayer player, ErrorCategory category, 
                                   String operation, ErrorSeverity severity) {
        try {
            String messageKey = switch (severity) {
                case MEDIUM -> "neoessentials.error.operation.medium";
                case HIGH -> "neoessentials.error.operation.high";
                case CRITICAL -> "neoessentials.error.operation.critical";
                default -> "neoessentials.error.operation.general";
            };
            
            String message = LanguageManager.getInstance()
                .getMessage(player, messageKey, operation, category.getDisplayName());
            MessageUtil.sendMessage(player, message);
            
        } catch (Exception e) {
            // Ultimate fallback - direct component message
            String fallbackMessage = switch (severity) {
                case MEDIUM -> "§eWarning: An issue occurred with " + operation;
                case HIGH -> "§cError: A problem occurred with " + operation;
                case CRITICAL -> "§4Critical Error: " + operation + " failed";
                default -> "§cAn error occurred";
            };
            player.sendSystemMessage(MessageUtil.translatable(player, "neoessentials.error.generic", fallbackMessage));
        }
    }
    
    /**
     * Handle critical errors that might affect system stability
     */
    private static void handleCriticalError(ErrorCategory category, String operation, Exception exception) {
        // Log detailed error information
        LOGGER.error("CRITICAL ERROR DETECTED");
        LOGGER.error("Category: {}", category.getDisplayName());
        LOGGER.error("Operation: {}", operation);
        LOGGER.error("Exception: {}", exception.getMessage());
        LOGGER.error("Stack trace:", exception);
        
        // Check if this is a repeated critical error
        String errorKey = "CRITICAL:" + category.name() + ":" + operation;
        long count = errorCounts.computeIfAbsent(errorKey, k -> new AtomicLong(0)).incrementAndGet();
        
        if (count > 5) {
            LOGGER.error("REPEATED CRITICAL ERROR: {} has occurred {} times", errorKey, count);
            // Could implement additional measures here like disabling certain features
        }
    }
    
    /**
     * Get error statistics for debugging
     */
    public static String getErrorStatistics() {
        StringBuilder stats = new StringBuilder();
        stats.append("=== Error Statistics ===\n");
        
        errorCounts.entrySet().stream()
            .sorted((a, b) -> Long.compare(b.getValue().get(), a.getValue().get()))
            .limit(10)
            .forEach(entry -> {
                long lastTime = lastErrorTime.getOrDefault(entry.getKey(), 0L);
                stats.append(String.format("%s: %d errors (last: %s ago)\n", 
                    entry.getKey(), 
                    entry.getValue().get(),
                    formatTimeDifference(System.currentTimeMillis() - lastTime)));
            });
        
        return stats.toString();
    }
    
    /**
     * Clear error statistics
     */
    public static void clearErrorStatistics() {
        errorCounts.clear();
        lastErrorTime.clear();
        LOGGER.info("Error statistics cleared");
    }
    
    /**
     * Check if detailed error logging is enabled
     */
    public static boolean isDetailedLoggingEnabled() {
        try {
            var config = ConfigManager.getInstance().getMainConfig();
            return config.debugMode;
        } catch (Exception e) {
            return false; // Default to false if config is unavailable
        }
    }
    
    /**
     * Format time difference for human reading
     */
    private static String formatTimeDifference(long milliseconds) {
        if (milliseconds < 1000) return milliseconds + "ms";
        if (milliseconds < 60000) return (milliseconds / 1000) + "s";
        if (milliseconds < 3600000) return (milliseconds / 60000) + "m";
        return (milliseconds / 3600000) + "h";
    }
}
