package com.zerog.neoessentials.util;

import net.minecraft.network.chat.HoverEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * Cross-version-safe {@link HoverEvent} construction.
 *
 * <p>{@code new HoverEvent(Action, V)} is a concrete constructor on 1.21.1
 * (this mod's compile target), but Mojang refactored {@code HoverEvent} into a
 * sealed interface with static factory methods (e.g. {@code HoverEvent.showText(Component)})
 * in later versions — the old constructor call throws {@link InstantiationError} at
 * runtime there (same class of cross-version break as {@link ClickEventCompat} and the
 * tell/serverLevel/addRegionTicket/playSound fixes elsewhere in this mod).</p>
 *
 * <p>On that failure, this reflectively looks up the modern static factory method
 * (unavailable at compile time against 1.21.1) and invokes it, so hover tooltips keep
 * working on newer servers instead of silently disappearing. Only if reflection also
 * fails does this fall back to {@code null} (plain text, no hover event).</p>
 */
public final class HoverEventCompat {
    private static final Logger LOGGER = LoggerFactory.getLogger(HoverEventCompat.class);
    private static volatile boolean loggedFallback = false;
    private static volatile boolean loggedReflectionSuccess = false;

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
            HoverEvent viaFactory = tryModernFactory(action, value);
            if (viaFactory != null) {
                if (!loggedReflectionSuccess) {
                    loggedReflectionSuccess = true;
                    LOGGER.info("HoverEvent(Action, Object) is unavailable on this Minecraft version — " +
                        "using the modern static factory method instead. Hover tooltips remain functional.");
                }
                return viaFactory;
            }
            if (!loggedFallback) {
                loggedFallback = true;
                LOGGER.warn("HoverEvent(Action, Object) is unavailable on this Minecraft version, and no " +
                    "matching static factory method could be found either — falling back to plain text " +
                    "(no hover event) for affected messages. ({})", e.getMessage());
            }
            return null;
        }
    }

    private static <V> HoverEvent tryModernFactory(HoverEvent.Action<V> action, V value) {
        if (value == null) return null;
        String methodName;
        if (action == HoverEvent.Action.SHOW_TEXT) {
            methodName = "showText";
        } else if (action == HoverEvent.Action.SHOW_ITEM) {
            methodName = "showItem";
        } else if (action == HoverEvent.Action.SHOW_ENTITY) {
            methodName = "showEntity";
        } else {
            return null;
        }

        for (Method m : HoverEvent.class.getMethods()) {
            if (!m.getName().equals(methodName)) continue;
            if (m.getParameterCount() != 1) continue;
            if (!m.getParameterTypes()[0].isAssignableFrom(value.getClass())) continue;
            try {
                return (HoverEvent) m.invoke(null, value);
            } catch (Exception ignored) {
                // Try the next matching overload, if any.
            }
        }
        return null;
    }
}
