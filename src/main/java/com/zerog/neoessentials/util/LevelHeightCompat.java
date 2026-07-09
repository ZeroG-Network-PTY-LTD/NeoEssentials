package com.zerog.neoessentials.util;

import net.minecraft.world.level.Level;

/**
 * World height accessors via {@code DimensionType} record fields
 * ({@code Level#getMinBuildHeight()}/{@code getMaxBuildHeight()} no longer exist as of Minecraft 26.1).
 */
public final class LevelHeightCompat {
    private LevelHeightCompat() {}

    public static int minBuildHeight(Level level) {
        return level.dimensionType().minY();
    }

    public static int maxBuildHeight(Level level) {
        return level.dimensionType().minY() + level.dimensionType().height();
    }
}
