package com.zerog.neoessentials.util;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.RemoteChatSession;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.world.level.GameType;

import java.util.UUID;

/**
 * Constructs a {@link ClientboundPlayerInfoUpdatePacket.Entry}.
 *
 * <p>As of Minecraft 26.1, the record gained {@code showHat}/{@code listOrder} fields
 * between {@code displayName} and {@code chatSession}. This defaults them to
 * {@code true}/{@code 0} (hat shown, default tab-list ordering) for fake tablist entries.</p>
 */
public final class TabListEntryCompat {
    private TabListEntryCompat() {}

    public static ClientboundPlayerInfoUpdatePacket.Entry create(
            UUID uuid, GameProfile profile, boolean listed, int latency,
            GameType gameMode, Component displayName, RemoteChatSession.Data chatSession) {
        return new ClientboundPlayerInfoUpdatePacket.Entry(
            uuid, profile, listed, latency, gameMode, displayName, true, 0, chatSession);
    }
}
