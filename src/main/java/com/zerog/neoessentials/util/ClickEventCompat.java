package com.zerog.neoessentials.util;

import net.minecraft.network.chat.ClickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.net.URI;

/**
 * Cross-version-safe {@link ClickEvent} construction.
 *
 * <p>{@code new ClickEvent(Action, String)} is a concrete constructor on 1.21.1
 * (this mod's compile target), but Mojang refactored {@code ClickEvent} into a
 * sealed interface with static factory methods (e.g. {@code ClickEvent.runCommand(String)})
 * in later versions — the old constructor call throws {@link InstantiationError} at
 * runtime there (same class of cross-version break as {@code MinecraftServer#tell},
 * {@code Entity#serverLevel()}, and {@code ServerChunkCache#addRegionTicket}
 * fixed elsewhere in this mod).</p>
 *
 * <p>On that failure, this reflectively looks up the modern per-action static
 * factory method (unavailable at compile time against 1.21.1) and invokes it, so
 * click actions such as the helpop "[Reply]" button keep working on newer servers
 * instead of silently degrading to plain, non-clickable text. Only if reflection
 * also fails does this fall back to {@code null} (plain text).</p>
 */
public final class ClickEventCompat {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClickEventCompat.class);
    private static volatile boolean loggedFallback = false;
    private static volatile boolean loggedReflectionSuccess = false;

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
            ClickEvent viaFactory = tryModernFactory(action, value);
            if (viaFactory != null) {
                if (!loggedReflectionSuccess) {
                    loggedReflectionSuccess = true;
                    LOGGER.info("ClickEvent(Action, String) is unavailable on this Minecraft version — " +
                        "using the modern static factory method instead. Click actions remain functional.");
                }
                return viaFactory;
            }
            if (!loggedFallback) {
                loggedFallback = true;
                LOGGER.warn("ClickEvent(Action, String) is unavailable on this Minecraft version, and no " +
                    "matching static factory method could be found either — falling back to plain text " +
                    "(no click action) for affected messages. ({})", e.getMessage());
            }
            return null;
        }
    }

    private static ClickEvent tryModernFactory(ClickEvent.Action action, String value) {
        String[] candidateNames = switch (action.getSerializedName()) {
            case "run_command" -> new String[]{"runCommand"};
            case "suggest_command" -> new String[]{"suggestCommand"};
            case "open_url" -> new String[]{"openUrl"};
            case "copy_to_clipboard" -> new String[]{"copyToClipboard"};
            case "change_page" -> new String[]{"changePage"};
            case "open_file" -> new String[]{"openFile"};
            default -> new String[0];
        };

        for (String name : candidateNames) {
            ClickEvent result = tryStringFactory(name, value);
            if (result != null) return result;
            if ("open_url".equals(action.getSerializedName())) {
                result = tryUriFactory(name, value);
                if (result != null) return result;
            }
            if ("change_page".equals(action.getSerializedName())) {
                result = tryIntFactory(name, value);
                if (result != null) return result;
            }
        }
        return null;
    }

    private static ClickEvent tryStringFactory(String methodName, String value) {
        try {
            Method m = ClickEvent.class.getMethod(methodName, String.class);
            Object result = m.invoke(null, value);
            return (ClickEvent) result;
        } catch (Exception e) {
            return null;
        }
    }

    private static ClickEvent tryUriFactory(String methodName, String value) {
        try {
            Method m = ClickEvent.class.getMethod(methodName, URI.class);
            Object result = m.invoke(null, URI.create(value));
            return (ClickEvent) result;
        } catch (Exception e) {
            return null;
        }
    }

    private static ClickEvent tryIntFactory(String methodName, String value) {
        try {
            Method m = ClickEvent.class.getMethod(methodName, int.class);
            Object result = m.invoke(null, Integer.parseInt(value));
            return (ClickEvent) result;
        } catch (Exception e) {
            return null;
        }
    }
}
