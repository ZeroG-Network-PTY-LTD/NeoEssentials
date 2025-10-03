package com.zerog.neoessentials.chat;

import net.minecraft.server.level.ServerPlayer;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe manager for muted players.
 */
public class MuteManager {
    // Use thread-safe Set
    private static final Set<String> mutedPlayers = ConcurrentHashMap.newKeySet();

    /**
     * Returns a snapshot of all muted player names (lowercase).
     */
    public static Set<String> getMutedPlayers() {
        return new HashSet<>(mutedPlayers);
    }

    public static void mute(ServerPlayer sender, String targetName) {
        mutedPlayers.add(targetName.toLowerCase());
    }

    public static void unmute(ServerPlayer sender, String targetName) {
        mutedPlayers.remove(targetName.toLowerCase());
    }

    public static boolean isMuted(ServerPlayer player) {
        return mutedPlayers.contains(player.getName().getString().toLowerCase());
    }
}
