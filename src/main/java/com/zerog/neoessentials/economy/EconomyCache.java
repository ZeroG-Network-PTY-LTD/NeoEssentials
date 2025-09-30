package com.zerog.neoessentials.economy;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class EconomyCache {
    private static final Cache<UUID, Object> cache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build();

    /**
     * Get a value from the cache, loading it if necessary.
     * @param key The UUID key
     * @param loader Function to load the value if not present
     * @return The cached or loaded value
     */
    public static <T> T getOrLoad(UUID key, Function<UUID, T> loader) {
        Object value = cache.getIfPresent(key);
        if (value == null) {
            value = loader.apply(key);
            if (value != null) {
                cache.put(key, value);
            }
        }
        return (T) value;
    }

    /**
     * Invalidate a cache entry.
     */
    public static void invalidate(UUID key) {
        cache.invalidate(key);
    }

    /**
     * Clear the entire cache.
     */
    public static void clear() {
        cache.invalidateAll();
    }
}