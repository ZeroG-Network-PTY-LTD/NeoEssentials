package com.zerog.neoessentials.chat;

import net.minecraft.server.level.ServerPlayer;
import java.util.HashSet;
import java.util.Set;

/**
 * Manages muted players and provides mute/unmute logic.
 */
public class MuteManager {
    /**
     * Returns a set of all muted player names (lowercase).
     */
    public static Set<String> getMutedPlayers() {
        return new HashSet<>(mutedPlayers);
    }
    private static final Set<String> mutedPlayers = new HashSet<>();

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
