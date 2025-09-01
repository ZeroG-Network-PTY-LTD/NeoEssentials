
package com.zerog.neoessentials.tablist;

/**
 * Schedules animation and placeholder refresh ticks, coalesces updates.
 */
public class AnimationScheduler {
    private final HeaderFooterManager headerFooterManager;
    private final com.zerog.neoessentials.placeholders.PlaceholderManager placeholderManager;
    private long lastTickTs = 0;
    private long tickIntervalMs = 100; // configurable
    public void setTickIntervalMs(long intervalMs) {
        this.tickIntervalMs = intervalMs;
        com.zerog.neoessentials.util.DebugUtil.debugLog("[TabList] Animation tick interval set to " + intervalMs + "ms");
    }

    public AnimationScheduler(HeaderFooterManager headerFooterManager, com.zerog.neoessentials.placeholders.PlaceholderManager placeholderManager) {
        this.headerFooterManager = headerFooterManager;
        this.placeholderManager = placeholderManager;
    }

    public void tick(long now) {
        if (now - lastTickTs < tickIntervalMs) return;
        lastTickTs = now;
        com.zerog.neoessentials.util.DebugUtil.debugLog("[TabList] Animation tick at " + now);
        placeholderManager.tickAnimatedPlaceholders(now);
        headerFooterManager.tick(now, placeholderManager);
    }
}
