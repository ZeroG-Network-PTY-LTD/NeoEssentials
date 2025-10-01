package com.zerog.neoessentials.chat;

import net.minecraft.server.level.ServerPlayer;
import java.util.HashSet;
import java.util.Set;

/**
 * Manages AFK status for players.
 */
public class AfkManager {
    private static final Set<String> afkPlayers = new HashSet<>();

    public static void toggleAfk(ServerPlayer player) {
        String name = player.getName().getString().toLowerCase();
        if (afkPlayers.contains(name)) {
            afkPlayers.remove(name);
        } else {
            afkPlayers.add(name);
        }
    }

    public static boolean isAfk(ServerPlayer player) {
        return afkPlayers.contains(player.getName().getString().toLowerCase());
    }
}
