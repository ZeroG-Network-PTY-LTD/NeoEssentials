package com.zerog.neoessentials.teleportation;

import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks combat state for players.
 * Combat is tracked when a player attacks or takes damage from another entity.
 * This does NOT trigger on environmental damage (fall, hunger, etc.) or non-combat activities.
 */
public class CombatTracker {
    private static final Logger LOGGER = LoggerFactory.getLogger(CombatTracker.class);

    // Combat timeout in milliseconds (5 seconds - reduced from 10 for better UX)
    private static final long COMBAT_TIMEOUT_MS = 5_000L;
    private static final Map<UUID, Long> combatEndTimestamps = new ConcurrentHashMap<>();

    /**
     * Mark a player as in combat. Combat status expires after COMBAT_TIMEOUT_MS.
     */
    public static void markInCombat(ServerPlayer player) {
        long endTime = System.currentTimeMillis() + COMBAT_TIMEOUT_MS;
        combatEndTimestamps.put(player.getUUID(), endTime);
        LOGGER.debug("Player {} marked in combat until {}", player.getName().getString(), endTime);
    }

    /**
     * Check if a player is currently in combat.
     * @return true if the player is in combat, false otherwise
     */
    public static boolean isInCombat(ServerPlayer player) {
        Long end = combatEndTimestamps.get(player.getUUID());
        boolean inCombat = end != null && end > System.currentTimeMillis();

        // Clean up expired entries
        if (end != null && !inCombat) {
            combatEndTimestamps.remove(player.getUUID());
        }

        return inCombat;
    }

    /**
     * Get remaining combat time in seconds for a player.
     * @return seconds remaining in combat, or 0 if not in combat
     */
    public static int getRemainingCombatTime(ServerPlayer player) {
        Long end = combatEndTimestamps.get(player.getUUID());
        if (end == null) return 0;

        long remaining = end - System.currentTimeMillis();
        if (remaining <= 0) {
            combatEndTimestamps.remove(player.getUUID());
            return 0;
        }

        return (int) Math.ceil(remaining / 1000.0);
    }

    /**
     * Clear combat status for a player (e.g., on logout).
     */
    public static void clearCombat(ServerPlayer player) {
        combatEndTimestamps.remove(player.getUUID());
        LOGGER.debug("Cleared combat status for player {}", player.getName().getString());
    }

    /**
     * Clear all combat data (e.g., on server shutdown).
     */
    public static void clearAll() {
        combatEndTimestamps.clear();
    }
}
