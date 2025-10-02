package com.zerog.neoessentials.economy;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class EconomyCache {
    private static final ConcurrentHashMap<UUID, Object> cache = new ConcurrentHashMap<>();

    /**
     * Get a value from the cache, loading it if necessary.
     * @param key The UUID key
     * @param loader Function to load the value if not present
     * @return The cached or loaded value
     */
    public static <T> T getOrLoad(UUID key, Function<UUID, T> loader) {
        Object value = cache.get(key);
        if (value == null) {
            value = loader.apply(key);
            if (value != null) {
                cache.put(key, value);
            }
        }
        @SuppressWarnings("unchecked")
        T result = (T) value;
        return result;
    }

    /**
     * Invalidate a cache entry.
     */
    public static void invalidate(UUID key) {
        cache.remove(key);
    }

    /**
     * Clear the entire cache.
     */
    public static void clear() {
        cache.clear();
    }
}