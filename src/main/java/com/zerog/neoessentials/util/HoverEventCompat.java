package com.zerog.neoessentials.util;

import net.minecraft.network.chat.HoverEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cross-version-safe {@link HoverEvent} construction.
 *
 * <p>{@code new HoverEvent(Action, V)} is a concrete constructor on 1.21.1
 * (this mod's compile target), but Mojang refactored {@code HoverEvent} into a
 * sealed interface with static factory methods in later versions — the old
 * constructor call throws {@link InstantiationError} at runtime there (same
 * class of cross-version break as {@link ClickEventCompat} and the tell/
 * serverLevel/addRegionTicket/playSound fixes elsewhere in this mod). Rather
 * than crash the whole command, this falls back to no hover event (plain
 * text) so the rest of the message still renders.</p>
 */
public final class HoverEventCompat {
    private static final Logger LOGGER = LoggerFactory.getLogger(HoverEventCompat.class);
    private static volatile boolean loggedFallback = false;

    private HoverEventCompat() {}

    /**
     * Returns a {@link HoverEvent} for the given action/value, or {@code null}
     * if {@code HoverEvent} can't be constructed this way on the running
     * Minecraft version. {@code Style#withHoverEvent(null)} is a safe no-op,
     * so callers can pass the result straight through without a null check.
     */
    public static <V> HoverEvent create(HoverEvent.Action<V> action, V value) {
        try {
            return new HoverEvent(action, value);
        } catch (InstantiationError | NoSuchMethodError e) {
            if (!loggedFallback) {
                loggedFallback = true;
                LOGGER.warn("HoverEvent(Action, Object) is unavailable on this Minecraft version — " +
                    "falling back to plain text (no hover event) for affected messages. ({})", e.getMessage());
            }
            return null;
        }
    }
}
