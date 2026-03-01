package com.zerog.neoessentials.chat.handlers;

import com.zerog.neoessentials.chat.AfkManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AFK activity detection with smart patterns and anti-abuse measures.
 * Tracks various player activities to determine genuine activity vs AFK farming.
 * This replaces the original simple activity handler with enhanced pattern detection
 * to prevent log spam from repetitive actions while still tracking legitimate activity.
 */
@EventBusSubscriber(modid = "neoessentials")
public class AfkActivityHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(AfkActivityHandler.class);

    // Track player activity patterns to detect AFK farming
    private static final Map<UUID, ActivityPattern> activityPatterns = new ConcurrentHashMap<>();

    // Configuration — raised so normal play never trips the filter
    private static final int REPETITIVE_ACTION_THRESHOLD = 30; // Same action 30+ times in window
    private static final long REPETITIVE_TIMEFRAME = 60000;    // Within 1 minute
    private static final int SUSPICIOUS_SCORE_THRESHOLD = 300; // Much higher bar

    /**
     * Activity pattern tracker for a player
     */
    public static class ActivityPattern {
        private final Map<String, Integer> actionCounts = new ConcurrentHashMap<>();
        private final Map<String, Long> lastActionTime = new ConcurrentHashMap<>();
        private int suspiciousScore = 0;
        private long lastActivity = System.currentTimeMillis();

        public void recordActivity(String activityType) {
            long now = System.currentTimeMillis();
            lastActivity = now;

            Long lastTime = lastActionTime.get(activityType);
            if (lastTime != null && (now - lastTime) < REPETITIVE_TIMEFRAME) {
                int count = actionCounts.getOrDefault(activityType, 0) + 1;
                actionCounts.put(activityType, count);
                if (count > REPETITIVE_ACTION_THRESHOLD) {
                    suspiciousScore += 10;
                    com.zerog.neoessentials.util.DebugLogger.log(LOGGER, "Detected repetitive {} activity: {} times", activityType, count);
                }
            } else {
                // Timeframe expired for this action type — reset its counter
                actionCounts.put(activityType, 1);
            }
            lastActionTime.put(activityType, now);

            // Decay suspicious score over time (using current time, not lastActivity)
            if (suspiciousScore > 0 && lastTime != null && (now - lastTime) > 300000) {
                suspiciousScore = Math.max(0, suspiciousScore - 5);
            }
        }

        public boolean isSuspicious() {
            return suspiciousScore > SUSPICIOUS_SCORE_THRESHOLD;
        }

        public int getSuspiciousScore() {
            return suspiciousScore;
        }

        @SuppressWarnings("unused")
        public long getLastActivity() {
            return lastActivity;
        }
    }

    /**
     * Record player activity with pattern detection
     */
    private static void recordActivity(ServerPlayer player, String activityType) {
        if (player == null) return;

        UUID uuid = player.getUUID();
        ActivityPattern pattern = activityPatterns.computeIfAbsent(uuid, k -> new ActivityPattern());
        pattern.recordActivity(activityType);

        // Only update AFK status if not suspicious
        if (!pattern.isSuspicious()) {
            AfkManager.getInstance().updateActivity(uuid);
            com.zerog.neoessentials.util.DebugLogger.log(LOGGER, "Activity tracked for {}: {} (score: {})",
                player.getName().getString(), activityType, pattern.getSuspiciousScore());
        } else {
            // This is diagnostic information - only show when debug logging is enabled
            // Otherwise it spams the logs when players are farming/building
            com.zerog.neoessentials.util.DebugLogger.log(LOGGER, "Suspicious activity pattern detected for {}: {} (score: {})",
                player.getName().getString(), activityType, pattern.getSuspiciousScore());
        }
    }

    // Event handlers with activity pattern detection

    @SubscribeEvent
    public static void onPlayerRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            recordActivity(player, "interact_block");
        }
    }

    @SubscribeEvent
    public static void onPlayerRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            recordActivity(player, "interact_item");
        }
    }

    @SubscribeEvent
    public static void onPlayerLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            recordActivity(player, "interact_attack");
        }
    }

    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            recordActivity(player, "item_toss");
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UUID uuid = player.getUUID();
            activityPatterns.remove(uuid);
            AfkManager.getInstance().onPlayerLogout(uuid);
            com.zerog.neoessentials.util.DebugLogger.log(LOGGER, "AFK tracking cleanup for: {}", player.getName().getString());
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UUID uuid = player.getUUID();
            // Reset activity pattern on login
            activityPatterns.put(uuid, new ActivityPattern());
            AfkManager.getInstance().updateActivity(uuid);
            com.zerog.neoessentials.util.DebugLogger.log(LOGGER, "AFK tracking initialized for: {}", player.getName().getString());
        }
    }

    /**
     * Get the activity pattern for a player (for debugging/admin purposes)
     */
    @SuppressWarnings("unused") // Public API method for admin/debugging
    public static ActivityPattern getActivityPattern(UUID playerUuid) {
        return activityPatterns.get(playerUuid);
    }

    /**
     * Check if a player has suspicious activity patterns
     */
    @SuppressWarnings("unused") // Public API method for admin/debugging
    public static boolean isSuspiciousActivity(UUID playerUuid) {
        ActivityPattern pattern = activityPatterns.get(playerUuid);
        return pattern != null && pattern.isSuspicious();
    }

    /**
     * Clear activity patterns (for shutdown)
     */
    @SuppressWarnings("unused") // Public API method for cleanup
    public static void clearPatterns() {
        activityPatterns.clear();
    }

    /**
     * Get current activity pattern statistics
     */
    @SuppressWarnings("unused") // Public API method for statistics
    public static Map<UUID, ActivityPattern> getActivityPatterns() {
        return new ConcurrentHashMap<>(activityPatterns);
    }
}

