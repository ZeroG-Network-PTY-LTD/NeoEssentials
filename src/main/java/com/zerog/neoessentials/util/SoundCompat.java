package com.zerog.neoessentials.util;

import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/**
 * 26.1 port note: {@code Player.playNotifySound(SoundEvent, SoundSource, float, float)} —
 * which played a sound for just one player (a client-only UI/notification sound, not
 * broadcast to nearby players via the world) — was removed. Its old behavior was just a
 * thin wrapper sending a {@link ClientboundSoundPacket} directly to that player's
 * connection at their current position; this reproduces it directly.
 */
public final class SoundCompat {

    private SoundCompat() {}

    public static void playNotifySound(ServerPlayer player, Holder<SoundEvent> sound, SoundSource source, float volume, float pitch) {
        player.connection.send(new ClientboundSoundPacket(
            sound, source, player.getX(), player.getY(), player.getZ(), volume, pitch, player.level().getRandom().nextLong()));
    }
}
