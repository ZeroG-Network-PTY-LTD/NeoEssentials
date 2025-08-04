package com.zerog.neoessentials.error;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.commands.CommandSourceStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enhanced error handling system for NeoEssentials
 * Provides user-friendly error messages and admin notifications
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class ErrorHandler {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ErrorHandler.class);
    
    /**
     * Error severity levels
     */
    public enum ErrorLevel {
        INFO("§a[INFO]", "ℹ️"),
        WARNING("§e[WARNING]", "⚠️"), 
        ERROR("§c[ERROR]", "❌"),
        CRITICAL("§4[CRITICAL]", "🚨");
        
        private final String colorCode;
        private final String emoji;
        
        ErrorLevel(String colorCode, String emoji) {
            this.colorCode = colorCode;
            this.emoji = emoji;
        }
        
        public String getColorCode() { return colorCode; }
        public String getEmoji() { return emoji; }
    }
    
    /**
     * Error categories for better organization
     */
    public enum ErrorCategory {
        COMMAND("Command"),
        ECONOMY("Economy"),
        TELEPORTATION("Teleportation"),
        PERMISSION("Permission"),
        CONFIGURATION("Configuration"),
        DATABASE("Database"),
        NETWORK("Network"),
        VALIDATION("Validation"),
        SYSTEM("System");
        
        private final String displayName;
        
        ErrorCategory(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() { return displayName; }
    }
    
    /**
     * Handle command validation errors with user-friendly messages
     */
    public static void handleCommandValidationError(CommandSourceStack source, String commandName, String issue) {
        String message = String.format("§c❌ Command Error: §7%s\n§7Command: §f/%s\n§7Issue: §e%s", 
            "Invalid command usage", commandName, issue);
        
        sendUserFriendlyError(source, message);
        logError(ErrorLevel.WARNING, ErrorCategory.COMMAND, 
            String.format("Command validation failed for /%s: %s", commandName, issue), null);
    }
    
    /**
     * Handle permission errors with helpful suggestions
     */
    public static void handlePermissionError(CommandSourceStack source, String requiredPermission) {
        String message = String.format("§c🔒 Permission Denied\n§7You need permission: §e%s\n§7Contact an administrator for access.", 
            requiredPermission);
        
        sendUserFriendlyError(source, message);
        logError(ErrorLevel.INFO, ErrorCategory.PERMISSION,
            String.format("Permission denied for %s: %s", getPlayerName(source), requiredPermission), null);
    }
    
    /**
     * Handle economy-related errors
     */
    public static void handleEconomyError(CommandSourceStack source, String operation, String details) {
        String message = String.format("§c💰 Economy Error\n§7Operation: §e%s\n§7Details: §f%s", 
            operation, details);
        
        sendUserFriendlyError(source, message);
        logError(ErrorLevel.ERROR, ErrorCategory.ECONOMY,
            String.format("Economy error for %s - %s: %s", getPlayerName(source), operation, details), null);
    }
    
    /**
     * Comprehensive error handling method for exceptions
     */
    public static void handleError(CommandSourceStack source, ErrorLevel level, ErrorCategory category, 
            String internalMessage, String userMessage, String suggestion, Exception exception) {
        
        // Log the error with full details
        logError(level, category, internalMessage, exception);
        
        // Send user-friendly message to player
        if (source != null) {
            String formattedMessage = String.format("%s %s\n§7%s", 
                level.getEmoji(), userMessage, suggestion != null ? suggestion : "");
            sendUserFriendlyError(source, formattedMessage);
        }
        
        // Notify admins for serious errors
        if (level == ErrorLevel.CRITICAL || level == ErrorLevel.ERROR) {
            notifyAdmins(level, category.getDisplayName() + " Error", 
                String.format("§c%s Error in %s:\n§7Message: §f%s\n§7Player: §e%s", 
                    level.getEmoji(), category.getDisplayName(), userMessage, 
                    source != null ? getPlayerName(source) : "Console"));
        }
    }

    /**
     * Handle teleportation errors with helpful context
     */
    public static void handleTeleportationError(CommandSourceStack source, String type, String reason) {
        String message = String.format("§c🌀 Teleportation Failed\n§7Type: §e%s\n§7Reason: §f%s\n§7Try again in a moment.", 
            type, reason);
        
        sendUserFriendlyError(source, message);
        logError(ErrorLevel.WARNING, ErrorCategory.TELEPORTATION,
            String.format("Teleportation failed for %s (%s): %s", getPlayerName(source), type, reason), null);
    }
    
    /**
     * Handle configuration errors
     */
    public static void handleConfigurationError(String configName, String issue, Exception exception) {
        String message = String.format("Configuration error in %s: %s", configName, issue);
        
        logError(ErrorLevel.ERROR, ErrorCategory.CONFIGURATION, message, exception);
        notifyAdmins(ErrorLevel.ERROR, "Configuration Error", 
            String.format("§c⚙️ Config issue detected:\n§7File: §e%s\n§7Issue: §f%s", configName, issue));
    }
    
    /**
     * Handle database errors
     */
    public static void handleDatabaseError(String operation, Exception exception) {
        String message = String.format("Database error during %s: %s", operation, exception.getMessage());
        
        logError(ErrorLevel.CRITICAL, ErrorCategory.DATABASE, message, exception);
        notifyAdmins(ErrorLevel.CRITICAL, "Database Error", 
            String.format("§4💾 Critical database error:\n§7Operation: §e%s\n§7Error: §f%s", operation, exception.getMessage()));
    }
    
    /**
     * Handle validation errors with helpful guidance
     */
    public static void handleValidationError(CommandSourceStack source, String field, String value, String expected) {
        String message = String.format("§c✋ Invalid Input\n§7Field: §e%s\n§7Your Input: §f%s\n§7Expected: §a%s", 
            field, value, expected);
        
        sendUserFriendlyError(source, message);
        logError(ErrorLevel.INFO, ErrorCategory.VALIDATION,
            String.format("Validation error for %s - %s: '%s' (expected: %s)", 
                getPlayerName(source), field, value, expected), null);
    }
    
    /**
     * Handle system errors that need admin attention
     */
    public static void handleSystemError(String component, String operation, Exception exception) {
        String message = String.format("System error in %s during %s: %s", component, operation, exception.getMessage());
        
        logError(ErrorLevel.CRITICAL, ErrorCategory.SYSTEM, message, exception);
        notifyAdmins(ErrorLevel.CRITICAL, "System Error", 
            String.format("§4🔧 System error detected:\n§7Component: §e%s\n§7Operation: §f%s\n§7Error: §c%s", 
                component, operation, exception.getMessage()));
    }
    
    /**
     * Send user-friendly error message to command source
     */
    private static void sendUserFriendlyError(CommandSourceStack source, String message) {
        try {
            source.sendFailure(Component.literal(message));
        } catch (Exception e) {
            // Fallback to logging if sending fails
            LOGGER.warn("Failed to send error message to user: {}", message, e);
        }
    }
    
    /**
     * Log error with appropriate level and category
     */
    private static void logError(ErrorLevel level, ErrorCategory category, String message, Exception exception) {
        String formattedMessage = String.format("[%s] [%s] %s", level.name(), category.getDisplayName(), message);
        
        switch (level) {
            case INFO:
                if (exception != null) {
                    LOGGER.info(formattedMessage, exception);
                } else {
                    LOGGER.info(formattedMessage);
                }
                break;
            case WARNING:
                if (exception != null) {
                    LOGGER.warn(formattedMessage, exception);
                } else {
                    LOGGER.warn(formattedMessage);
                }
                break;
            case ERROR:
            case CRITICAL:
                if (exception != null) {
                    LOGGER.error(formattedMessage, exception);
                } else {
                    LOGGER.error(formattedMessage);
                }
                break;
        }
    }
    
    /**
     * Notify online administrators about critical issues
     */
    private static void notifyAdmins(ErrorLevel level, String title, String message) {
        // Implementation would notify all online admins
        // This is a placeholder for the notification system
        LOGGER.info("Admin notification ({}): {} - {}", level.name(), title, message);
        
        // TODO: Implement actual admin notification system
        // - Get all online players with admin permissions
        // - Send them the formatted error message
        // - Consider rate limiting to prevent spam
    }
    
    /**
     * Get player name from command source, with fallback
     */
    private static String getPlayerName(CommandSourceStack source) {
        try {
            if (source.getEntity() instanceof ServerPlayer player) {
                return player.getName().getString();
            } else {
                return source.getTextName();
            }
        } catch (Exception e) {
            return "Unknown";
        }
    }
    
    /**
     * Recovery helper for graceful error handling
     */
    public static class ErrorRecovery {
        
        /**
         * Attempt to recover from a command error
         */
        public static boolean attemptCommandRecovery(CommandSourceStack source, String commandName, Exception error) {
            try {
                // Log the recovery attempt
                LOGGER.info("Attempting recovery for command {} after error: {}", commandName, error.getMessage());
                
                // Send helpful recovery message to user
                String recoveryMessage = String.format(
                    "§e⚠️ Command encountered an issue but we're handling it gracefully.\n" +
                    "§7Command: §f/%s\n" +
                    "§7You can try again in a moment or contact an administrator if the issue persists.",
                    commandName
                );
                
                source.sendFailure(Component.literal(recoveryMessage));
                return true;
                
            } catch (Exception recoveryError) {
                LOGGER.error("Failed to recover from command error", recoveryError);
                return false;
            }
        }
        
        /**
         * Graceful shutdown helper for critical errors
         */
        public static void gracefulShutdown(String reason, Exception cause) {
            LOGGER.error("Initiating graceful shutdown due to critical error: {}", reason, cause);
            
            // Notify all online players
            notifyAdmins(ErrorLevel.CRITICAL, "System Shutdown", 
                String.format("§4🚨 Critical error detected - Graceful shutdown initiated:\n§7Reason: §f%s", reason));
            
            // Save critical data before shutdown
            // TODO: Implement data saving logic if needed
        }
    }
}
