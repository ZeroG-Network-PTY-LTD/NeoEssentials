
package com.zerog.neoessentials.error;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.commands.CommandSourceStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.zerog.neoessentials.localization.LanguageManager;

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
        sendLocalizedError(source, "error.command.validation", commandName, issue);
        logError(ErrorLevel.WARNING, ErrorCategory.COMMAND,
            String.format("Command validation failed for /%s: %s", commandName, issue), null);
    }
    
    /**
     * Handle permission errors with helpful suggestions
     */
    public static void handlePermissionError(CommandSourceStack source, String requiredPermission) {
        sendLocalizedError(source, "error.permission.denied", requiredPermission);
        logError(ErrorLevel.INFO, ErrorCategory.PERMISSION,
            String.format("Permission denied for %s: %s", getPlayerName(source), requiredPermission), null);
    }
    
    /**
     * Handle economy-related errors
     */
    public static void handleEconomyError(CommandSourceStack source, String operation, String details) {
        sendLocalizedError(source, "error.economy", operation, details);
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
        sendLocalizedError(source, "error.teleportation", type, reason);
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
        sendLocalizedError(source, "error.validation", field, value, expected);
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
            LOGGER.warn("Failed to send error message to user: {}", message, e);
        }
    }

    private static void sendLocalizedError(CommandSourceStack source, String key, Object... args) {
        try {
            ServerPlayer player = null;
            if (source.getEntity() instanceof ServerPlayer p) player = p;
            String msg = LanguageManager.getInstance().getMessage(player, key, args);
            source.sendFailure(Component.literal(msg));
        } catch (Exception e) {
            LOGGER.warn("Failed to send localized error message to user: {}", key, e);
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
        LOGGER.info("Admin notification ({}): {} - {}", level.name(), title, message);
        try {
            // Get all online players with admin permissions
            net.minecraft.server.MinecraftServer server = getActiveMinecraftServer();
            if (server != null) {
                for (net.minecraft.server.level.ServerPlayer player : server.getPlayerList().getPlayers()) {
                    // Replace with your admin permission node or OP level check
                    if (player.hasPermissions(3)) { // 3 = OP, adjust as needed
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            String.format("[ADMIN] %s: %s", title, message)));
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to notify admins: {}", e.getMessage(), e);
        }
    }

    /**
     * Helper to get the active MinecraftServer instance
     */
    private static net.minecraft.server.MinecraftServer getActiveMinecraftServer() {
        try {
            return ServerLifecycleHooks.getCurrentServer();
        } catch (Exception e) {
            return null;
        }
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
                sendLocalizedError(source, "error.command.recovery", commandName);
                LOGGER.info("Attempting recovery for command {} after error: {}", commandName, error.getMessage());
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
            notifyAdmins(ErrorLevel.CRITICAL, "System Shutdown",
                LanguageManager.getInstance().getMessage((String) null, "error.system.shutdown", reason));
        }
    }
}
