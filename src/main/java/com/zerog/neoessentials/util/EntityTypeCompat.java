package com.zerog.neoessentials.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;

import java.lang.reflect.Method;

/**
 * Cross-version-safe {@link EntityType#create(Level)} construction.
 *
 * <p>{@code EntityType.create(Level)} is a two-argument-free method on 1.21.1
 * (this mod's compile target), but Mojang added a required {@code EntitySpawnReason}
 * parameter in later versions (e.g. {@code EntityType.create(Level, EntitySpawnReason)}),
 * so the old call throws {@link NoSuchMethodError} at runtime there (same class of
 * cross-version break as {@link ClickEventCompat}/{@link HoverEventCompat} and the
 * tell/serverLevel/addRegionTicket/playSound/getMinBuildHeight fixes elsewhere in
 * this mod).</p>
 *
 * <p>On that failure, this reflectively looks up the modern {@code create(Level, *)}
 * overload (unavailable at compile time against 1.21.1) and invokes it with the
 * enum constant that looks most appropriate for a mod-spawned entity, rather than
 * letting the whole server crash (this call happens on the server-started event,
 * outside any per-command error boundary, so an uncaught error here takes the
 * entire server down).</p>
 */
public final class EntityTypeCompat {
    private static final Logger LOGGER = LoggerFactory.getLogger(EntityTypeCompat.class);
    private static volatile boolean loggedFallback = false;
    private static volatile boolean loggedReflectionSuccess = false;

    // Preference order for whichever enum constant the modern spawn-reason parameter expects.
    private static final String[] PREFERRED_REASON_NAMES = {
        "TRIGGERED", "COMMAND", "MOB_SUMMONED", "SPAWN_ITEM_USE", "NATURAL"
    };

    private EntityTypeCompat() {}

    /**
     * Returns a newly created entity of the given type in {@code level}, or {@code null}
     * if it can't be constructed on the running Minecraft version.
     */
    public static <T extends Entity> T create(EntityType<T> type, Level level) {
        try {
            return type.create(level);
        } catch (NoSuchMethodError e) {
            T viaReflection = tryModernCreate(type, level);
            if (viaReflection != null) {
                if (!loggedReflectionSuccess) {
                    loggedReflectionSuccess = true;
                    NeoLog.info(LOGGER, LogCategory.GENERAL, "EntityType.create(Level) is unavailable on this Minecraft version — " +
                        "using the modern create(Level, spawnReason) overload instead.");
                }
                return viaReflection;
            }
            if (!loggedFallback) {
                loggedFallback = true;
                LOGGER.warn("EntityType.create(Level) is unavailable on this Minecraft version, and no " +
                    "matching overload could be found either — entity spawning will be skipped. ({})",
                    e.getMessage());
            }
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Entity> T tryModernCreate(EntityType<T> type, Level level) {
        for (Method m : EntityType.class.getMethods()) {
            if (!m.getName().equals("create")) continue;
            Class<?>[] params = m.getParameterTypes();
            if (params.length != 2 || !params[0].isAssignableFrom(level.getClass())
                    && !params[0].equals(Level.class)) continue;
            Object reasonArg = resolveReasonArgument(params[1]);
            if (reasonArg == null) continue;
            try {
                Object result = m.invoke(type, level, reasonArg);
                if (result != null) return (T) result;
            } catch (Exception ignored) {
                // Try the next matching overload, if any.
            }
        }
        return null;
    }

    private static Object resolveReasonArgument(Class<?> paramType) {
        if (!paramType.isEnum()) return null;
        Object[] constants = paramType.getEnumConstants();
        if (constants == null || constants.length == 0) return null;

        for (String preferred : PREFERRED_REASON_NAMES) {
            for (Object constant : constants) {
                if (((Enum<?>) constant).name().equals(preferred)) {
                    return constant;
                }
            }
        }
        // No preferred name matched — fall back to the first constant rather than not
        // spawning at all; a slightly-wrong spawn reason is harmless for a decorative entity.
        return constants[0];
    }
}
