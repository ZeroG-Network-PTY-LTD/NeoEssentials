package com.zerog.neoessentials.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Accessor for a player's {@link ServerLevel}, via {@code Entity#level()}
 * ({@code ServerPlayer#serverLevel()} no longer exists as of Minecraft 26.1).
 */
public final class LevelCompat {
    private LevelCompat() {}

    public static ServerLevel of(ServerPlayer player) {
        return (ServerLevel) player.level();
    }
}
