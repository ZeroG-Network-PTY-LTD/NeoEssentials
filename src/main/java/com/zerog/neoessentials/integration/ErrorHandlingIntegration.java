package com.zerog.neoessentials.integration;

import com.zerog.neoessentials.error.ErrorHandler;
import com.zerog.neoessentials.exception.NeoEssentialsExceptions;
import com.zerog.neoessentials.validation.CommandValidator;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

/**
 * Integration utility for applying enhanced error handling to existing commands
 * Provides easy migration path for existing command implementations
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class ErrorHandlingIntegration {
    
    /**
     * Wrap existing command logic with comprehensive error handling
     */
    public static int executeCommand(CommandSourceStack source, String commandName, 
            ThrowingCommandFunction commandLogic) {
        
        return NeoEssentialsExceptions.Handler.executeWithHandling(
            source, 
            "command: " + commandName,
            () -> commandLogic.execute(source),
            0
        );
    }
    
    /**
     * Validate permission and execute command with error handling
     */
    public static int executeWithPermission(CommandSourceStack source, String commandName, 
            String permission, ThrowingCommandFunction commandLogic) {
        
        try {
            // Check permission first
            if (!hasPermission(source, permission)) {
                throw NeoEssentialsExceptions.Factory.noPermission(permission, commandName);
            }
            
            // Execute with error handling
            return executeCommand(source, commandName, commandLogic);
            
        } catch (NeoEssentialsExceptions.PermissionException e) {
            NeoEssentialsExceptions.Handler.handle(source, e);
            return 0;
        }
    }
    
    /**
     * Enhanced player targeting with error handling
     */
    public static ServerPlayer getPlayerSafely(CommandSourceStack source, String playerName) 
            throws NeoEssentialsExceptions.CommandException {
        
        // Validate player name format
        CommandValidator.ValidationResult validation = CommandValidator.validatePlayerName(source, playerName);
        if (!validation.isValid()) {
            throw new NeoEssentialsExceptions.CommandException(
                "Invalid player name: " + playerName,
                validation.getErrorMessage(),
                validation.getSuggestion()
            );
        }
        
        // Find player
        ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(playerName);
        if (player == null) {
            throw new NeoEssentialsExceptions.CommandException(
                "Player not found: " + playerName,
                "Player '" + playerName + "' is not online",
                "Check the player name and make sure they're online"
            );
        }
        
        return player;
    }
    
    /**
     * Safe amount parsing with validation
     */
    public static double parseAmountSafely(CommandSourceStack source, String amountStr) 
            throws NeoEssentialsExceptions.CommandException {
        
        CommandValidator.ValidationResult validation = CommandValidator.validateAmount(source, amountStr, 0.01, Double.MAX_VALUE);
        if (!validation.isValid()) {
            throw new NeoEssentialsExceptions.CommandException(
                "Invalid amount: " + amountStr,
                validation.getErrorMessage(),
                validation.getSuggestion()
            );
        }
        
        try {
            return Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            throw new NeoEssentialsExceptions.CommandException(
                "Failed to parse amount: " + amountStr,
                "Invalid number format",
                "Use a valid decimal number (e.g., 100.50)"
            );
        }
    }
    
    /**
     * Safe coordinate parsing with validation
     */
    public static double parseCoordinateSafely(CommandSourceStack source, String coordStr, String axis) 
            throws NeoEssentialsExceptions.CommandException {
        
        CommandValidator.ValidationResult validation = CommandValidator.validateCoordinate(source, coordStr, axis);
        if (!validation.isValid()) {
            throw new NeoEssentialsExceptions.CommandException(
                String.format("Invalid %s coordinate: %s", axis, coordStr),
                validation.getErrorMessage(),
                validation.getSuggestion()
            );
        }
        
        try {
            return Double.parseDouble(coordStr);
        } catch (NumberFormatException e) {
            throw new NeoEssentialsExceptions.CommandException(
                String.format("Failed to parse %s coordinate: %s", axis, coordStr),
                String.format("Invalid %s coordinate format", axis.toUpperCase()),
                "Use a valid number for coordinates"
            );
        }
    }
    
    /**
     * Economy operation wrapper with error handling
     */
    public static void performEconomyOperation(CommandSourceStack source, String operation, 
            ThrowingEconomyFunction economyLogic) throws NeoEssentialsExceptions.EconomyException {
        
        try {
            economyLogic.execute();
        } catch (Exception e) {
            if (e.getMessage().contains("insufficient")) {
                throw new NeoEssentialsExceptions.EconomyException(
                    "Economy operation failed: " + operation,
                    "Insufficient funds for this operation",
                    "Check your balance and try with a lower amount"
                );
            }
            
            throw new NeoEssentialsExceptions.EconomyException(
                "Economy operation failed: " + operation + " - " + e.getMessage(),
                "Economy operation failed",
                "Please try again or contact an admin"
            );
        }
    }
    
    /**
     * Teleportation wrapper with safety checks
     */
    public static void performTeleportation(CommandSourceStack source, String type, 
            ThrowingTeleportFunction teleportLogic) throws NeoEssentialsExceptions.TeleportException {
        
        try {
            teleportLogic.execute();
        } catch (Exception e) {
            if (e.getMessage().contains("unsafe") || e.getMessage().contains("dangerous")) {
                throw new NeoEssentialsExceptions.TeleportException(
                    "Unsafe teleportation blocked: " + type,
                    "Cannot teleport to unsafe location",
                    "Choose a different location or wait for area to be cleared"
                );
            }
            
            throw new NeoEssentialsExceptions.TeleportException(
                "Teleportation failed: " + type + " - " + e.getMessage(),
                "Teleportation failed",
                "Please try again in a moment"
            );
        }
    }
    
    /**
     * Configuration operation wrapper
     */
    public static <T> T getConfigValueSafely(String configName, String key, 
            java.util.function.Supplier<T> getter, T defaultValue) {
        
        try {
            return getter.get();
        } catch (Exception e) {
            ErrorHandler.handleConfigurationError(configName, 
                "Failed to read key: " + key, e);
            return defaultValue;
        }
    }
    
    /**
     * Simple permission check using the proper permission system
     */
    private static boolean hasPermission(CommandSourceStack source, String permission) {
        // Use the proper PermissionUtil which integrates with CustomPermissionsManager
        return com.zerog.neoessentials.util.PermissionUtil.hasPermission(source, permission);
    }
    
    /**
     * Functional interfaces for lambda expressions
     */
    @FunctionalInterface
    public interface ThrowingCommandFunction {
        int execute(CommandSourceStack source) throws Exception;
    }
    
    @FunctionalInterface
    public interface ThrowingEconomyFunction {
        void execute() throws Exception;
    }
    
    @FunctionalInterface
    public interface ThrowingTeleportFunction {
        void execute() throws Exception;
    }
    
    /**
     * Example usage methods for integration with existing commands
     */
    public static class Examples {
        
        /**
         * Example: Enhanced balance command
         */
        public static int balanceCommand(CommandSourceStack source, String playerName) {
            return executeCommand(source, "balance", (src) -> {
                ServerPlayer target = getPlayerSafely(src, playerName);
                // Get balance logic here
                src.sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                    String.format("§e%s's balance: §a$%.2f", target.getName().getString(), 1000.0)), false);
                return 1;
            });
        }
        
        /**
         * Example: Enhanced pay command
         */
        public static int payCommand(CommandSourceStack source, String targetName, String amountStr) {
            return executeWithPermission(source, "pay", "neoessentials.economy.pay", (src) -> {
                ServerPlayer target = getPlayerSafely(src, targetName);
                double amount = parseAmountSafely(src, amountStr);
                
                performEconomyOperation(src, "pay", () -> {
                    // Economy logic here
                    src.sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                        String.format("§aPaid §e$%.2f §ato §e%s", amount, target.getName().getString())), false);
                });
                
                return 1;
            });
        }
        
        /**
         * Example: Enhanced teleport command
         */
        public static int teleportCommand(CommandSourceStack source, String x, String y, String z) {
            return executeWithPermission(source, "tp", "neoessentials.teleport", (src) -> {
                double xCoord = parseCoordinateSafely(src, x, "x");
                double yCoord = parseCoordinateSafely(src, y, "y");
                double zCoord = parseCoordinateSafely(src, z, "z");
                
                performTeleportation(src, "coordinates", () -> {
                    // Teleportation logic here
                    src.sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                        String.format("§aTeleported to §e%.1f, %.1f, %.1f", xCoord, yCoord, zCoord)), false);
                });
                
                return 1;
            });
        }
    }
}
