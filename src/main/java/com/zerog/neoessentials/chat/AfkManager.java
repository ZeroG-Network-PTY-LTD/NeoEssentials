package com.zerog.neoessentials.chat;

import net.minecraft.server.level.ServerPlayer;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe manager for AFK status of players.
 */
public class AfkManager {
    // Use newSetFromMap to create a thread-safe Set
    private static final Set<String> afkPlayers = ConcurrentHashMap.newKeySet();

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
