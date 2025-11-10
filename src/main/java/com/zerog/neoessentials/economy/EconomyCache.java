package com.zerog.neoessentials.economy;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class EconomyCache {
    private static final ConcurrentHashMap<UUID, Object> cache = new ConcurrentHashMap<>();

    /**
     * Get a value from the cache, loading it atomically if necessary.
     * Uses computeIfAbsent to prevent multiple threads from loading the same key.
     * @param key The UUID key
     * @param loader Function to load the value if not present
     * @return The cached or loaded value
     */
    @SuppressWarnings("unchecked")
    public static <T> T getOrLoad(UUID key, Function<UUID, T> loader) {
        return (T) cache.computeIfAbsent(key, loader);
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