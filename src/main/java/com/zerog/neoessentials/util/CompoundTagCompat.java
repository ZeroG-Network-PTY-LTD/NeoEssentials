package com.zerog.neoessentials.util;

import net.minecraft.nbt.CompoundTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Cross-version-safe {@link CompoundTag#getString(String)}.
 *
 * <p>{@code getString(String)} returns a plain {@code String} on 1.21.1 (this
 * mod's compile target), but Mojang reworked several {@code CompoundTag} getters
 * around the NBT codec modernization in later versions — the old 1-arg overload
 * can throw {@link NoSuchMethodError} at runtime there (same class of cross-version
 * break as the other Compat helpers in this mod).</p>
 *
 * <p>On that failure, this reflectively tries the modern replacements that have
 * been used across that rework — a {@code getString(String, String)} default-value
 * overload, or an {@code Optional<String>}-returning {@code getString(String)} —
 * before falling back to an empty string.</p>
 */
public final class CompoundTagCompat {
    private static final Logger LOGGER = LoggerFactory.getLogger(CompoundTagCompat.class);
    private static volatile boolean loggedFallback = false;

    private CompoundTagCompat() {}

    /** Returns the string value for {@code key}, or {@code ""} if absent/unavailable. */
    public static String getString(CompoundTag tag, String key) {
        try {
            return tag.getString(key);
        } catch (NoSuchMethodError e) {
            String viaReflection = tryModernGetString(tag, key);
            if (viaReflection != null) return viaReflection;
            if (!loggedFallback) {
                loggedFallback = true;
                LOGGER.warn("CompoundTag.getString(String) is unavailable on this Minecraft version, and no " +
                    "matching replacement could be found either — falling back to empty string. ({})",
                    e.getMessage());
            }
            return "";
        }
    }

    private static String tryModernGetString(CompoundTag tag, String key) {
        // Candidate 1: getString(String key, String defaultValue)
        try {
            Method m = CompoundTag.class.getMethod("getString", String.class, String.class);
            Object result = m.invoke(tag, key, "");
            if (result instanceof String s) return s;
        } catch (Exception ignored) {
            // fall through to the next candidate
        }

        // Candidate 2: getString(String key) returning Optional<String>
        try {
            Method m = CompoundTag.class.getMethod("getString", String.class);
            Object result = m.invoke(tag, key);
            if (result instanceof Optional<?> opt) {
                Object value = opt.isPresent() ? opt.get() : "";
                if (value instanceof String s) return s;
            }
        } catch (Exception ignored) {
            // no more candidates
        }

        return null;
    }

    public static int getInt(CompoundTag tag, String key) {
        try {
            return tag.getInt(key);
        } catch (NoSuchMethodError e) {
            Number n = tryModernGetNumeric(tag, "getInt", key);
            return n != null ? n.intValue() : 0;
        }
    }

    public static float getFloat(CompoundTag tag, String key) {
        try {
            return tag.getFloat(key);
        } catch (NoSuchMethodError e) {
            Number n = tryModernGetNumeric(tag, "getFloat", key);
            return n != null ? n.floatValue() : 0f;
        }
    }

    public static double getDouble(CompoundTag tag, String key) {
        try {
            return tag.getDouble(key);
        } catch (NoSuchMethodError e) {
            Number n = tryModernGetNumeric(tag, "getDouble", key);
            return n != null ? n.doubleValue() : 0.0;
        }
    }

    public static long getLong(CompoundTag tag, String key) {
        try {
            return tag.getLong(key);
        } catch (NoSuchMethodError e) {
            Number n = tryModernGetNumeric(tag, "getLong", key);
            return n != null ? n.longValue() : 0L;
        }
    }

    public static byte getByte(CompoundTag tag, String key) {
        try {
            return tag.getByte(key);
        } catch (NoSuchMethodError e) {
            Number n = tryModernGetNumeric(tag, "getByte", key);
            return n != null ? n.byteValue() : 0;
        }
    }

    public static short getShort(CompoundTag tag, String key) {
        try {
            return tag.getShort(key);
        } catch (NoSuchMethodError e) {
            Number n = tryModernGetNumeric(tag, "getShort", key);
            return n != null ? n.shortValue() : 0;
        }
    }

    public static boolean getBoolean(CompoundTag tag, String key) {
        try {
            return tag.getBoolean(key);
        } catch (NoSuchMethodError e) {
            try {
                Method m = CompoundTag.class.getMethod("getBoolean", String.class, boolean.class);
                Object result = m.invoke(tag, key, false);
                if (result instanceof Boolean b) return b;
            } catch (Exception ignored) {}
            try {
                Method m = CompoundTag.class.getMethod("getBoolean", String.class);
                Object result = m.invoke(tag, key);
                if (result instanceof Optional<?> opt) {
                    Object value = opt.isPresent() ? opt.get() : Boolean.FALSE;
                    if (value instanceof Boolean b) return b;
                }
            } catch (Exception ignored) {}
            return false;
        }
    }

    /**
     * Reflectively tries the {@code get<Type>(String, <primitive>)} default-value overload,
     * then an {@code Optional}-returning {@code get<Type>(String)}, for any of the numeric
     * getters. Returns {@code null} if neither candidate works.
     */
    private static Number tryModernGetNumeric(CompoundTag tag, String methodName, String key) {
        for (Method m : CompoundTag.class.getMethods()) {
            if (!m.getName().equals(methodName)) continue;
            Class<?>[] params = m.getParameterTypes();
            try {
                if (params.length == 2 && params[0].equals(String.class)) {
                    Object result = m.invoke(tag, key, params[1].isPrimitive() ? primitiveZero(params[1]) : null);
                    if (result instanceof Number n) return n;
                } else if (params.length == 1 && params[0].equals(String.class)) {
                    Object result = m.invoke(tag, key);
                    if (result instanceof Optional<?> opt) {
                        Object value = opt.orElse(null);
                        if (value instanceof Number n) return n;
                    } else if (result instanceof Number n) {
                        return n;
                    }
                }
            } catch (Exception ignored) {
                // try the next matching overload, if any
            }
        }
        return null;
    }

    private static Object primitiveZero(Class<?> primitiveType) {
        if (primitiveType == int.class) return 0;
        if (primitiveType == float.class) return 0f;
        if (primitiveType == double.class) return 0.0;
        if (primitiveType == long.class) return 0L;
        if (primitiveType == byte.class) return (byte) 0;
        if (primitiveType == short.class) return (short) 0;
        return null;
    }
}
