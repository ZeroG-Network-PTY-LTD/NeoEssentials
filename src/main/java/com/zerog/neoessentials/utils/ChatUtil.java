package com.zerog.neoessentials.utils;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Utility class for chat-related functionality.
 */
public class ChatUtil {

    /**
     * Send a success message to the command source.
     * 
     * @param source The command source to send the message to
     * @param message The message to send
     */
    public static void sendSuccess(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal(message), false);
    }

    /**
     * Send an error message to the command source.
     * 
     * @param source The command source to send the message to
     * @param message The message to send
     */
    public static void sendError(CommandSourceStack source, String message) {
        source.sendFailure(Component.literal(message));
    }
    
    /**
     * Send a message to a player.
     * 
     * @param player The player to send the message to
     * @param message The message to send
     */
    public static void sendMessage(ServerPlayer player, String message) {
        player.sendSystemMessage(Component.literal(message));
    }

    /**
     * Send a message to a command source.
     * 
     * @param source The command source to send the message to
     * @param message The message to send
     */
    public static void sendMessage(CommandSourceStack source, String message) {
        source.sendSystemMessage(Component.literal(message));
    }

    /**
     * Send a message to a player with a specific color.
     * 
     * @param player The player to send the message to
     * @param message The message to send
     * @param color The color in hexadecimal format (e.g. 0xFF0000 for red)
     */
    public static void sendColoredMessage(ServerPlayer player, String message, int color) {
        player.sendSystemMessage(Component.literal(message).withColor(color));
    }
}
