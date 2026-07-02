package com.zerog.neoessentials.util;

import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Compatibility wrapper for {@link Level#getMinBuildHeight()} / {@link Level#getMaxBuildHeight()}.
 *
 * <p>Mojang has renamed these {@code LevelHeightAccessor} methods between Minecraft
 * versions (e.g. 1.21.1 vs 1.21.8+), so code compiled against this project's target
 * version can throw {@link NoSuchMethodError} at runtime on a newer server. Falls back
 * to {@code level.dimensionType().minY()} / {@code minY() + height()}, which are stable
 * {@link net.minecraft.world.level.dimension.DimensionType} record accessors rather than
 * the churn-prone {@code LevelHeightAccessor} default methods.</p>
 */
public final class LevelHeightCompat {
    private static final Logger LOGGER = LoggerFactory.getLogger(LevelHeightCompat.class);
    private static volatile boolean loggedFallback = false;

    private LevelHeightCompat() {}

    public static int minBuildHeight(Level level) {
        try {
            return level.getMinBuildHeight();
        } catch (NoSuchMethodError e) {
            logFallbackOnce(e);
            return level.dimensionType().minY();
        }
    }

    public static int maxBuildHeight(Level level) {
        try {
            return level.getMaxBuildHeight();
        } catch (NoSuchMethodError e) {
            logFallbackOnce(e);
            return level.dimensionType().minY() + level.dimensionType().height();
        }
    }

    private static void logFallbackOnce(NoSuchMethodError e) {
        if (!loggedFallback) {
            loggedFallback = true;
            LOGGER.warn("Level.getMinBuildHeight()/getMaxBuildHeight() are unavailable on this " +
                "Minecraft version — falling back to dimensionType() bounds. ({})", e.getMessage());
        }
    }
}
