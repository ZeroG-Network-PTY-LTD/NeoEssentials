package com.zerog.neoessentials.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * 26.1 port note: {@code Level.getDayTime()}/{@code setDayTime(long)} were removed in favor
 * of an entirely new data-driven day/night system — {@code WorldClock}s (registry entries
 * with their own rate, pause state, and named "time markers" like sunrise/noon), managed
 * server-side by {@code MinecraftServer.clockManager()} ({@code ServerClockManager}).
 *
 * <p>This wraps the dimension's <em>default</em> clock (the one that drives the normal
 * day/night cycle and sky rendering) so the rest of the codebase can keep treating world
 * time as a single tick counter, matching the old {@code getDayTime()}/{@code setDayTime()}
 * behavior — none of NeoEssentials' time-related features (display, {@code /time}-style
 * admin commands) need the new multi-clock/timeline machinery.</p>
 */
public final class WorldClockCompat {

    private WorldClockCompat() {}

    /** Equivalent to the old {@code Level.getDayTime()} — total ticks on the dimension's default clock. */
    public static long getTime(Level level) {
        return level.getDefaultClockTime();
    }

    /** Equivalent to the old {@code Level.setDayTime(long)}. No-op if the dimension has no default clock. */
    public static void setTime(ServerLevel level, long ticks) {
        level.dimensionType().defaultClock()
            .ifPresent(clock -> level.clockManager().setTotalTicks(clock, ticks));
    }
}
