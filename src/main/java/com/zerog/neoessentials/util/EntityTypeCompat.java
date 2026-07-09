package com.zerog.neoessentials.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Constructs a mod-spawned entity via {@link EntityType#create(Level, EntitySpawnReason)},
 * always using {@link EntitySpawnReason#COMMAND} since every call site here is
 * command- or feature-triggered rather than a natural/world-gen spawn.
 */
public final class EntityTypeCompat {
    private EntityTypeCompat() {}

    public static <T extends Entity> T create(EntityType<T> type, Level level) {
        return type.create(level, EntitySpawnReason.COMMAND);
    }
}
