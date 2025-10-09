package com.zerog.neoessentials.chat;

import net.minecraft.server.level.ServerPlayer;
import java.util.HashSet;
import java.util.Set;

/**
 * Manages private message toggling for players.
 */
public class MsgToggleManager {
    private static final Set<String> toggledPlayers = new HashSet<>();

    public static boolean toggleMsg(ServerPlayer player) {
        String name = player.getName().getString().toLowerCase();
        if (toggledPlayers.contains(name)) {
            toggledPlayers.remove(name);
            return true; // Now receiving messages
        } else {
            toggledPlayers.add(name);
            return false; // Now blocking messages
        }
    }

    public static boolean isMsgToggled(ServerPlayer player) {
        return toggledPlayers.contains(player.getName().getString().toLowerCase());
    }
}
