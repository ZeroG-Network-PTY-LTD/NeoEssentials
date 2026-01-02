package com.zerog.neoessentials.util;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Helper class for handling command sources (players and console).
 * Provides utility methods for safely extracting player information from command sources
 * and determining if the source is a player or console.
 */
public class CommandSourceHelper {

    /**
     * Check if the command source is a player (not console or command block).
     *
     * @param source The command source
     * @return true if the source is a player
     */
    public static boolean isPlayer(CommandSourceStack source) {
        return source.getEntity() instanceof ServerPlayer;
    }

    /**
     * Check if the command source is the console.
     *
     * @param source The command source
     * @return true if the source is console
     */
    public static boolean isConsole(CommandSourceStack source) {
        return !isPlayer(source);
    }

    /**
     * Get the ServerPlayer from a command source, or null if not a player.
     *
     * @param source The command source
     * @return ServerPlayer instance or null if console
     */
    public static ServerPlayer getPlayer(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return player;
        }
        return null;
    }

    /**
     * Get the UUID of the command source player, or null if console.
     *
     * @param source The command source
     * @return UUID of the player or null if console
     */
    public static UUID getPlayerUUID(CommandSourceStack source) {
        ServerPlayer player = getPlayer(source);
        return player != null ? player.getUUID() : null;
    }

    /**
     * Get a display name for the command sender (player name or "Console").
     *
     * @param source The command source
     * @return Player name or "Console"
     */
    public static String getSenderName(CommandSourceStack source) {
        ServerPlayer player = getPlayer(source);
        return player != null ? player.getName().getString() : "Console";
    }

    /**
     * Require that the command source is a player.
     * Sends an error message if not a player.
     *
     * @param source The command source
     * @return ServerPlayer instance or null if not a player (error message sent)
     */
    public static ServerPlayer requirePlayer(CommandSourceStack source) {
        ServerPlayer player = getPlayer(source);
        if (player == null) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.error.player_only"));
        }
        return player;
    }

    /**
     * Require that the command source is a player with a custom error message.
     * Sends the custom error message if not a player.
     *
     * @param source The command source
     * @param errorMessage Custom error message translation key
     * @return ServerPlayer instance or null if not a player (error message sent)
     */
    public static ServerPlayer requirePlayer(CommandSourceStack source, String errorMessage) {
        ServerPlayer player = getPlayer(source);
        if (player == null) {
            source.sendFailure(MessageUtil.error(errorMessage));
        }
        return player;
    }
}
