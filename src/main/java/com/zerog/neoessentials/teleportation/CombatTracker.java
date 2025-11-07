package com.zerog.neoessentials.teleportation;

import net.minecraft.server.level.ServerPlayer;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks combat state for players (basic implementation).
 */
public class CombatTracker {
    // Combat timeout in milliseconds (e.g., 10 seconds)
    private static final long COMBAT_TIMEOUT_MS = 10_000L;
    private static final Map<UUID, Long> combatEndTimestamps = new ConcurrentHashMap<>();

    /**
     * Call this when a player enters combat (e.g., on attack or damage).
     */
    public static void markInCombat(ServerPlayer player) {
        combatEndTimestamps.put(player.getUUID(), System.currentTimeMillis() + COMBAT_TIMEOUT_MS);
    }

    /**
     * Returns true if the player is currently in combat.
     */
    public static boolean isInCombat(ServerPlayer player) {
        Long end = combatEndTimestamps.get(player.getUUID());
        return end != null && end > System.currentTimeMillis();
    }

    /**
     * Call this to clear combat state (e.g., on logout).
     */
    public static void clearCombat(ServerPlayer player) {
        combatEndTimestamps.remove(player.getUUID());
    }
}
