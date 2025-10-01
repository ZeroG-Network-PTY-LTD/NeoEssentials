package com.zerog.neoessentials.chat;

import net.minecraft.server.level.ServerPlayer;
import java.util.HashSet;
import java.util.Set;

/**
 * Manages private message toggling for players.
 */
public class MsgToggleManager {
    private static final Set<String> toggledPlayers = new HashSet<>();

    public static void toggleMsg(ServerPlayer player) {
        String name = player.getName().getString().toLowerCase();
        if (toggledPlayers.contains(name)) {
            toggledPlayers.remove(name);
        } else {
            toggledPlayers.add(name);
        }
    }

    public static boolean isMsgToggled(ServerPlayer player) {
        return toggledPlayers.contains(player.getName().getString().toLowerCase());
    }
}
