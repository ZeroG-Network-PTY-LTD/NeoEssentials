package com.zerog.neoessentials.chat;

import net.minecraft.server.level.ServerPlayer;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import com.zerog.neoessentials.util.ChatDebugUtil;

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
        ChatDebugUtil.debug("Muted player %s. Muted players now: %s", targetName, mutedPlayers);
    }

    public static void unmute(ServerPlayer sender, String targetName) {
        mutedPlayers.remove(targetName.toLowerCase());
        ChatDebugUtil.debug("Unmuted player %s. Muted players now: %s", targetName, mutedPlayers);
    }

    public static boolean isMuted(ServerPlayer player) {
        boolean result = mutedPlayers.contains(player.getName().getString().toLowerCase());
        // Add debug logging to help diagnose the issue
        ChatDebugUtil.debug("Checking if %s is muted: %s (mutedPlayers contains: %s)", player.getName().getString(), result, mutedPlayers);
        return result;
    }
}
