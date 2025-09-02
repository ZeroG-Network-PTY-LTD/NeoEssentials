package com.zerog.neoessentials.commands.essentials;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.integration.ErrorHandlingIntegration;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Function;

/**
 * Utility class for standardizing essential command behavior
 * Provides common functionality for permission checking, error handling, and messaging
 * 
 * @author NeoEssentials Team
 * @since 1.0.0
 */
public class EssentialCommandHelper {
    
    /**
     * Execute a command with proper permission checking and error handling
     * 
     * @param context The command context
     * @param permission The required permission
     * @param operation The operation to execute
     * @return Command result (1 for success, 0 for failure)
     */
    public static int executeWithPermission(CommandContext<CommandSourceStack> context, 
                                          String permission, 
                                          Function<ServerPlayer, Integer> operation) {
        return ErrorHandlingIntegration.executeWithPermission(
            context.getSource(),
            "essential_command",
            permission,
            (source) -> {
                try {
                    ServerPlayer player = source.getPlayerOrException();
                    return operation.apply(player);
                } catch (CommandSyntaxException e) {
                    source.sendFailure(Component.literal("§cThis command can only be used by players."));
                    return 0;
                }
            }
        );
    }
    
    /**
     * Execute a command targeting another player with permission checks
     * 
     * @param context The command context
     * @param permission The required permission
     * @param target The target player
     * @param operation The operation to execute
     * @return Command result (1 for success, 0 for failure)
     */
    public static int executeOnTarget(CommandContext<CommandSourceStack> context,
                                    String permission,
                                    ServerPlayer target,
                                    Function<ServerPlayer, Integer> operation) {
        return ErrorHandlingIntegration.executeWithPermission(
            context.getSource(),
            "essential_command_target",
            permission,
            (source) -> operation.apply(target)
        );
    }
    
    /**
     * Send a success message to the command source
     * 
     * @param source The command source
     * @param messageKey The translation key
     * @param args Optional message arguments
     */
    public static void sendSuccess(CommandSourceStack source, String messageKey, Object... args) {
        source.sendSuccess(() -> Component.translatable(messageKey, args), false);
    }
    
    /**
     * Send a success message with broadcast to operators
     * 
     * @param source The command source
     * @param messageKey The translation key
     * @param args Optional message arguments
     */
    public static void sendSuccessWithBroadcast(CommandSourceStack source, String messageKey, Object... args) {
        source.sendSuccess(() -> Component.translatable(messageKey, args), true);
    }
    
    /**
     * Send a failure message to the command source
     * 
     * @param source The command source
     * @param messageKey The translation key
     * @param args Optional message arguments
     */
    public static void sendFailure(CommandSourceStack source, String messageKey, Object... args) {
        source.sendFailure(Component.translatable(messageKey, args));
    }
    
    /**
     * Send a translated message to a player
     * 
     * @param player The target player
     * @param messageKey The translation key
     * @param args Optional message arguments
     */
    public static void sendPlayerMessage(ServerPlayer player, String messageKey, Object... args) {
        MessageUtil.sendTranslatedMessage(player, messageKey, args);
    }
    
    /**
     * Check if a player has permission with proper error handling
     * 
     * @param source The command source
     * @param permission The permission to check
     * @return True if player has permission
     */
    public static boolean hasPermission(CommandSourceStack source, String permission) {
        return PermissionUtil.hasPermissionOrOp(source, permission);
    }
    
    /**
     * Get a player from the command source with error handling
     * 
     * @param source The command source
     * @return The player, or null if not a player
     */
    public static ServerPlayer getPlayer(CommandSourceStack source) {
        try {
            return source.getPlayerOrException();
        } catch (CommandSyntaxException e) {
            return null;
        }
    }
}
