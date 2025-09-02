package com.zerog.neoessentials.commands.admin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import com.zerog.neoessentials.util.ErrorHandler;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;

/**
 * Administrative command for managing error handling and diagnostics
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class ErrorCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("error")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.ADMIN_FULL))
            
            // Show error statistics
            .then(Commands.literal("stats")
                .executes(ErrorCommand::executeStats))
            
            // Clear error statistics
            .then(Commands.literal("clear")
                .executes(ErrorCommand::executeClear))
            
            // Test error handling
            .then(Commands.literal("test")
                .executes(ErrorCommand::executeTest))
            
            // Toggle detailed error logging
            .then(Commands.literal("debug")
                .executes(ErrorCommand::executeDebugToggle))
        );
    }
    
    private static int executeStats(CommandContext<CommandSourceStack> context) {
        return ErrorHandler.safeExecute(
            ErrorHandler.ErrorCategory.COMMAND_EXECUTION,
            "Error Stats Command",
            () -> {
                var player = context.getSource().getPlayer();
                if (player != null) {
                    String stats = ErrorHandler.getErrorStatistics();
                    MessageUtil.sendMessage(player, "§6=== Error Statistics ===§r");
                    for (String line : stats.split("\n")) {
                        if (!line.trim().isEmpty()) {
                            MessageUtil.sendMessage(player, "§7" + line + "§r");
                        }
                    }
                    
                    boolean debugEnabled = ErrorHandler.isDetailedLoggingEnabled();
                    MessageUtil.sendMessage(player, "");
                    MessageUtil.sendMessage(player, "§7Detailed Error Logging: " + 
                        (debugEnabled ? "§aEnabled§r" : "§cDisabled§r"));
                }
                return 1;
            },
            0,
            context.getSource().getPlayer()
        );
    }
    
    private static int executeClear(CommandContext<CommandSourceStack> context) {
        return ErrorHandler.safeExecute(
            ErrorHandler.ErrorCategory.COMMAND_EXECUTION,
            "Error Clear Command",
            () -> {
                ErrorHandler.clearErrorStatistics();
                var player = context.getSource().getPlayer();
                if (player != null) {
                    MessageUtil.sendMessage(player, "§aError statistics cleared successfully.§r");
                }
                return 1;
            },
            0,
            context.getSource().getPlayer()
        );
    }
    
    private static int executeTest(CommandContext<CommandSourceStack> context) {
        return ErrorHandler.safeExecute(
            ErrorHandler.ErrorCategory.COMMAND_EXECUTION,
            "Error Test Command",
            () -> {
                var player = context.getSource().getPlayer();
                
                // Test different error severities
                ErrorHandler.handleError(
                    ErrorHandler.ErrorCategory.COMMAND_EXECUTION,
                    ErrorHandler.ErrorSeverity.LOW,
                    "Test Low Severity",
                    new RuntimeException("Test low severity error"),
                    player
                );
                
                ErrorHandler.handleError(
                    ErrorHandler.ErrorCategory.VALIDATION,
                    ErrorHandler.ErrorSeverity.MEDIUM,
                    "Test Medium Severity",
                    new IllegalArgumentException("Test medium severity error"),
                    player
                );
                
                ErrorHandler.handleValidationError("Test Validation", "Invalid test parameter", player);
                ErrorHandler.handlePermissionError("Test Permission", player);
                
                if (player != null) {
                    MessageUtil.sendMessage(player, "§aError handling test completed. Check logs and statistics.§r");
                }
                return 1;
            },
            0,
            context.getSource().getPlayer()
        );
    }
    
    private static int executeDebugToggle(CommandContext<CommandSourceStack> context) {
        return ErrorHandler.safeExecute(
            ErrorHandler.ErrorCategory.COMMAND_EXECUTION,
            "Error Debug Toggle Command",
            () -> {
                var player = context.getSource().getPlayer();
                boolean currentState = ErrorHandler.isDetailedLoggingEnabled();
                
                if (player != null) {
                    MessageUtil.sendMessage(player, "§7Current detailed error logging state: " + 
                        (currentState ? "§aEnabled§r" : "§cDisabled§r"));
                    MessageUtil.sendMessage(player, "§7To change this setting, modify the debugMode in your main config.§r");
                    MessageUtil.sendMessage(player, "§7Current errors are being " + 
                        (currentState ? "logged with full details" : "logged with basic information") + ".§r");
                }
                return 1;
            },
            0,
            context.getSource().getPlayer()
        );
    }
}
