package com.zerog.neoessentials.utils;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Utility class for formatting and sending messages to players.
 */
public class MessageUtil {
    // Prefix for all NeoEssentials messages
    private static final String PREFIX = "§8[§6NeoEssentials§8] §r";
    
    /**
     * Formats a message with color codes and the NeoEssentials prefix
     * 
     * @param message The message to format
     * @return The formatted message as a Component
     */
    public static Component formatMessage(String message) {
        String formattedMessage = PREFIX + translateColorCodes(message);
        return Component.literal(formattedMessage);
    }
    
    /**
     * Sends a formatted message to a player
     * 
     * @param player The player to send the message to
     * @param message The message to send
     */
    public static void sendMessage(Player player, String message) {
        player.sendSystemMessage(formatMessage(message));
    }
    
    /**
     * Sends an error message to a player
     * 
     * @param player The player to send the error message to
     * @param message The error message to send
     */
    public static void sendErrorMessage(Player player, String message) {
        String errorMessage = "§c" + message;
        sendMessage(player, errorMessage);
    }
    
    /**
     * Sends a success message to a player
     * 
     * @param player The player to send the success message to
     * @param message The success message to send
     */
    public static void sendSuccessMessage(Player player, String message) {
        String successMessage = "§a" + message;
        sendMessage(player, successMessage);
    }    /**
     * Broadcasts a message to all online players
     * 
     * @param message The message to broadcast
     */
    public static void broadcastMessage(String message) {
        Component formattedMessage = formatMessage(message);
        // Get all online players from the server and send them the message
        if (com.zerog.neoessentials.NeoEssentials.getInstance() != null && 
            com.zerog.neoessentials.NeoEssentials.getInstance().getServer() != null) {
            for (ServerPlayer player : com.zerog.neoessentials.NeoEssentials.getInstance().getServer().getPlayerList().getPlayers()) {
                player.sendSystemMessage(formattedMessage);
            }
        }
    }
    
    /**
     * Sends a standard info message to a player with a Component
     * 
     * @param player The player to send the message to
     * @param component The message component
     */
    public static void sendInfo(ServerPlayer player, MutableComponent component) {
        component = Component.literal(PREFIX).append(component);
        player.sendSystemMessage(component);
    }
    
    /**
     * Sends a success message to a player with a Component
     * 
     * @param player The player to send the message to
     * @param component The message component
     */
    public static void sendSuccess(ServerPlayer player, MutableComponent component) {
        MutableComponent formatted = Component.literal(PREFIX).append(Component.literal("§a")).append(component);
        player.sendSystemMessage(formatted);
    }
    
    /**
     * Sends an error message to a player with a Component
     * 
     * @param player The player to send the message to
     * @param component The message component
     */
    public static void sendError(ServerPlayer player, MutableComponent component) {
        MutableComponent formatted = Component.literal(PREFIX).append(Component.literal("§c")).append(component);
        player.sendSystemMessage(formatted);
    }
    
    /**
     * Adds hover text to a component
     * 
     * @param component The component to add hover text to
     * @param hoverText The hover text to add
     * @return The component with hover text
     */    public static MutableComponent addHoverText(MutableComponent component, MutableComponent hoverText) {
        return component.withStyle(style -> style.withHoverEvent(new HoverEvent(
                HoverEvent.Action.SHOW_TEXT, 
                hoverText)));
    }
    
    /**
     * Makes a component clickable to run a command
     * 
     * @param component The component to make clickable
     * @param command The command to run when clicked
     * @return The clickable component
     */
    public static MutableComponent makeClickableCommand(MutableComponent component, String command) {
        return component.withStyle(style -> style.withClickEvent(new ClickEvent(
                ClickEvent.Action.RUN_COMMAND, 
                command)));
    }
    
    /**
     * Makes a component clickable to suggest a command
     * 
     * @param component The component to make clickable
     * @param command The command to suggest when clicked
     * @return The clickable component
     */
    public static MutableComponent makeSuggestCommand(MutableComponent component, String command) {
        return component.withStyle(style -> style.withClickEvent(new ClickEvent(
                ClickEvent.Action.SUGGEST_COMMAND, 
                command)));
    }
    
    /**
     * Translates standard '&' color codes to Minecraft's internal format
     * 
     * @param message The message with '&' color codes
     * @return The message with Minecraft internal color codes
     */
    private static String translateColorCodes(String message) {
        char colorChar = '&';
        char[] array = message.toCharArray();
        
        for (int i = 0; i < array.length - 1; i++) {
            if (array[i] == colorChar && "0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(array[i + 1]) > -1) {
                array[i] = '§';
                array[i + 1] = Character.toLowerCase(array[i + 1]);
            }
        }
        
        return new String(array);
    }
}
