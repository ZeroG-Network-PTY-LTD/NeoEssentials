package com.zerog.neoessentials.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cross-version-safe accessor for an entity's {@link ServerLevel}.
 *
 * <p>{@code Entity#serverLevel()} is a convenience accessor whose erased method
 * signature has proven fragile across Minecraft versions — the same class of
 * problem that previously broke {@code MinecraftServer#tell(TickTask)} (see
 * {@link com.zerog.neoessentials.scheduler.DelayedTaskScheduler}). This helper
 * tries the direct call first and falls back to the more fundamental
 * {@code Entity#level()} accessor (present since long before {@code serverLevel()}
 * was introduced) if the direct call is missing at runtime.</p>
 */
public final class LevelCompat {
    private static final Logger LOGGER = LoggerFactory.getLogger(LevelCompat.class);
    private static volatile boolean loggedFallback = false;

    private LevelCompat() {}

    /**
     * Returns the {@link ServerLevel} the given player is in, falling back to
     * {@code (ServerLevel) player.level()} if {@code player.serverLevel()} is
     * unavailable at runtime on this Minecraft version.
     */
    public static ServerLevel of(ServerPlayer player) {
        try {
            return player.serverLevel();
        } catch (NoSuchMethodError e) {
            if (!loggedFallback) {
                loggedFallback = true;
                LOGGER.warn("ServerPlayer#serverLevel() is unavailable on this Minecraft version — " +
                    "falling back to Entity#level(). ({})", e.getMessage());
            }
            return (ServerLevel) player.level();
        }
    }
}
