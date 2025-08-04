package com.zerog.neoessentials.exception;

import com.zerog.neoessentials.error.ErrorHandler;
import net.minecraft.commands.CommandSourceStack;

/**
 * Custom exception types for NeoEssentials
 * Provides structured error handling with user-friendly messages
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class NeoEssentialsExceptions {
    
    /**
     * Base exception for all NeoEssentials errors
     */
    public static abstract class NeoEssentialsException extends Exception {
        private final String userMessage;
        private final String suggestion;
        
        protected NeoEssentialsException(String message, String userMessage, String suggestion) {
            super(message);
            this.userMessage = userMessage;
            this.suggestion = suggestion;
        }
        
        protected NeoEssentialsException(String message, String userMessage, String suggestion, Throwable cause) {
            super(message, cause);
            this.userMessage = userMessage;
            this.suggestion = suggestion;
        }
        
        public String getUserMessage() { return userMessage; }
        public String getSuggestion() { return suggestion; }
        
        public abstract ErrorHandler.ErrorLevel getErrorLevel();
        public abstract ErrorHandler.ErrorCategory getErrorCategory();
    }
    
    /**
     * Command execution exceptions
     */
    public static class CommandException extends NeoEssentialsException {
        
        public CommandException(String message, String userMessage, String suggestion) {
            super(message, userMessage, suggestion);
        }
        
        public CommandException(String message, String userMessage, String suggestion, Throwable cause) {
            super(message, userMessage, suggestion, cause);
        }
        
        @Override
        public ErrorHandler.ErrorLevel getErrorLevel() { return ErrorHandler.ErrorLevel.WARNING; }
        
        @Override
        public ErrorHandler.ErrorCategory getErrorCategory() { return ErrorHandler.ErrorCategory.COMMAND; }
    }
    
    /**
     * Permission-related exceptions
     */
    public static class PermissionException extends NeoEssentialsException {
        
        public PermissionException(String message, String userMessage, String suggestion) {
            super(message, userMessage, suggestion);
        }
        
        @Override
        public ErrorHandler.ErrorLevel getErrorLevel() { return ErrorHandler.ErrorLevel.INFO; }
        
        @Override
        public ErrorHandler.ErrorCategory getErrorCategory() { return ErrorHandler.ErrorCategory.PERMISSION; }
    }
    
    /**
     * Economy system exceptions
     */
    public static class EconomyException extends NeoEssentialsException {
        
        public EconomyException(String message, String userMessage, String suggestion) {
            super(message, userMessage, suggestion);
        }
        
        public EconomyException(String message, String userMessage, String suggestion, Throwable cause) {
            super(message, userMessage, suggestion, cause);
        }
        
        @Override
        public ErrorHandler.ErrorLevel getErrorLevel() { return ErrorHandler.ErrorLevel.ERROR; }
        
        @Override
        public ErrorHandler.ErrorCategory getErrorCategory() { return ErrorHandler.ErrorCategory.ECONOMY; }
    }
    
    /**
     * Teleportation exceptions
     */
    public static class TeleportException extends NeoEssentialsException {
        
        public TeleportException(String message, String userMessage, String suggestion) {
            super(message, userMessage, suggestion);
        }
        
        @Override
        public ErrorHandler.ErrorLevel getErrorLevel() { return ErrorHandler.ErrorLevel.WARNING; }
        
        @Override
        public ErrorHandler.ErrorCategory getErrorCategory() { return ErrorHandler.ErrorCategory.TELEPORTATION; }
    }
    
    /**
     * Home management exceptions
     */
    public static class HomeException extends NeoEssentialsException {
        
        public HomeException(String message, String userMessage, String suggestion) {
            super(message, userMessage, suggestion);
        }
        
        @Override
        public ErrorHandler.ErrorLevel getErrorLevel() { return ErrorHandler.ErrorLevel.WARNING; }
        
        @Override
        public ErrorHandler.ErrorCategory getErrorCategory() { return ErrorHandler.ErrorCategory.TELEPORTATION; }
    }
    
    /**
     * Configuration exceptions
     */
    public static class ConfigException extends NeoEssentialsException {
        
        public ConfigException(String message, String userMessage, String suggestion) {
            super(message, userMessage, suggestion);
        }
        
        public ConfigException(String message, String userMessage, String suggestion, Throwable cause) {
            super(message, userMessage, suggestion, cause);
        }
        
        @Override
        public ErrorHandler.ErrorLevel getErrorLevel() { return ErrorHandler.ErrorLevel.ERROR; }
        
        @Override
        public ErrorHandler.ErrorCategory getErrorCategory() { return ErrorHandler.ErrorCategory.CONFIGURATION; }
    }
    
    /**
     * Database/Data exceptions
     */
    public static class DataException extends NeoEssentialsException {
        
        public DataException(String message, String userMessage, String suggestion) {
            super(message, userMessage, suggestion);
        }
        
        public DataException(String message, String userMessage, String suggestion, Throwable cause) {
            super(message, userMessage, suggestion, cause);
        }
        
        @Override
        public ErrorHandler.ErrorLevel getErrorLevel() { return ErrorHandler.ErrorLevel.ERROR; }
        
        @Override
        public ErrorHandler.ErrorCategory getErrorCategory() { return ErrorHandler.ErrorCategory.DATABASE; }
    }
    
    /**
     * Network/API exceptions
     */
    public static class NetworkException extends NeoEssentialsException {
        
        public NetworkException(String message, String userMessage, String suggestion) {
            super(message, userMessage, suggestion);
        }
        
        public NetworkException(String message, String userMessage, String suggestion, Throwable cause) {
            super(message, userMessage, suggestion, cause);
        }
        
        @Override
        public ErrorHandler.ErrorLevel getErrorLevel() { return ErrorHandler.ErrorLevel.WARNING; }
        
        @Override
        public ErrorHandler.ErrorCategory getErrorCategory() { return ErrorHandler.ErrorCategory.NETWORK; }
    }
    
    /**
     * Exception factory methods for common scenarios
     */
    public static class Factory {
        
        public static CommandException invalidCommand(String command, String reason) {
            return new CommandException(
                String.format("Invalid command execution: %s - %s", command, reason),
                String.format("Command failed: %s", reason),
                "Check command syntax and try again"
            );
        }
        
        public static PermissionException noPermission(String permission, String action) {
            return new PermissionException(
                String.format("Missing permission: %s for action: %s", permission, action),
                "You don't have permission to do that",
                "Ask an admin for the required permissions"
            );
        }
        
        public static EconomyException insufficientFunds(double required, double available) {
            return new EconomyException(
                String.format("Insufficient funds: required %.2f, available %.2f", required, available),
                String.format("You need $%.2f but only have $%.2f", required, available),
                "Earn more money or reduce the amount"
            );
        }
        
        public static TeleportException unsafeLocation(String reason) {
            return new TeleportException(
                String.format("Unsafe teleport location: %s", reason),
                "Cannot teleport to unsafe location",
                "Choose a different location or wait for area to be cleared"
            );
        }
        
        public static HomeException homeNotFound(String homeName) {
            return new HomeException(
                String.format("Home not found: %s", homeName),
                String.format("Home '%s' doesn't exist", homeName),
                "Use /homes to see your available homes"
            );
        }
        
        public static ConfigException configLoadError(String configName, Throwable cause) {
            return new ConfigException(
                String.format("Failed to load config: %s", configName),
                "Configuration error occurred",
                "Contact server admin - config may need to be reset",
                cause
            );
        }
        
        public static DataException saveError(String dataType, Throwable cause) {
            return new DataException(
                String.format("Failed to save %s data", dataType),
                "Failed to save your data",
                "Try again - if problem persists, contact admin",
                cause
            );
        }
        
        public static NetworkException connectionTimeout(String service) {
            return new NetworkException(
                String.format("Connection timeout to %s", service),
                "Service temporarily unavailable",
                "Please try again in a moment"
            );
        }
    }
    
    /**
     * Exception handler utility
     */
    public static class Handler {
        
        /**
         * Handle NeoEssentials exception with full error reporting
         */
        public static void handle(CommandSourceStack source, NeoEssentialsException exception) {
            ErrorHandler.handleError(
                source,
                exception.getErrorLevel(),
                exception.getErrorCategory(),
                exception.getMessage(),
                exception.getUserMessage(),
                exception.getSuggestion(),
                exception
            );
        }
        
        /**
         * Handle unknown exception with fallback error handling
         */
        public static void handleUnknown(CommandSourceStack source, Exception exception, String context) {
            ErrorHandler.handleError(
                source,
                ErrorHandler.ErrorLevel.ERROR,
                ErrorHandler.ErrorCategory.SYSTEM,
                String.format("Unknown error in %s: %s", context, exception.getMessage()),
                "An unexpected error occurred",
                "Please try again or contact an admin if the problem persists",
                exception
            );
        }
        
        /**
         * Try-catch wrapper for command execution
         */
        public static void executeWithHandling(CommandSourceStack source, String context, ThrowingRunnable operation) {
            try {
                operation.run();
            } catch (NeoEssentialsException e) {
                handle(source, e);
            } catch (Exception e) {
                handleUnknown(source, e, context);
            }
        }
        
        /**
         * Try-catch wrapper for operations that return values
         */
        public static <T> T executeWithHandling(CommandSourceStack source, String context, 
                ThrowingSupplier<T> operation, T defaultValue) {
            try {
                return operation.get();
            } catch (NeoEssentialsException e) {
                handle(source, e);
                return defaultValue;
            } catch (Exception e) {
                handleUnknown(source, e, context);
                return defaultValue;
            }
        }
        
        @FunctionalInterface
        public interface ThrowingRunnable {
            void run() throws Exception;
        }
        
        @FunctionalInterface
        public interface ThrowingSupplier<T> {
            T get() throws Exception;
        }
    }
}
