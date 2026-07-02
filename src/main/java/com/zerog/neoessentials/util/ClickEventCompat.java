package com.zerog.neoessentials.util;

import net.minecraft.network.chat.ClickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cross-version-safe {@link ClickEvent} construction.
 *
 * <p>{@code new ClickEvent(Action, String)} is a concrete constructor on 1.21.1
 * (this mod's compile target), but Mojang refactored {@code ClickEvent} into a
 * sealed interface with static factory methods in later versions — the old
 * constructor call throws {@link InstantiationError} at runtime there (same
 * class of cross-version break as {@code MinecraftServer#tell},
 * {@code Entity#serverLevel()}, and {@code ServerChunkCache#addRegionTicket}
 * fixed elsewhere in this mod). Rather than crash the whole command, this
 * falls back to no click event (plain text) so the rest of the message still
 * renders.</p>
 */
public final class ClickEventCompat {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClickEventCompat.class);
    private static volatile boolean loggedFallback = false;

    private ClickEventCompat() {}

    /**
     * Returns a {@link ClickEvent} for the given action/value, or {@code null}
     * if {@code ClickEvent} can't be constructed this way on the running
     * Minecraft version. {@code Style#withClickEvent(null)} is a safe no-op,
     * so callers can pass the result straight through without a null check.
     */
    public static ClickEvent create(ClickEvent.Action action, String value) {
        try {
            return new ClickEvent(action, value);
        } catch (InstantiationError | NoSuchMethodError e) {
            if (!loggedFallback) {
                loggedFallback = true;
                LOGGER.warn("ClickEvent(Action, String) is unavailable on this Minecraft version — " +
                    "falling back to plain text (no click action) for affected messages. ({})", e.getMessage());
            }
            return null;
        }
    }
}
