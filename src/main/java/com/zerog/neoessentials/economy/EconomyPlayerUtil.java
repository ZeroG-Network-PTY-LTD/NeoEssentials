package com.zerog.neoessentials.economy;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import com.mojang.authlib.GameProfile;
import java.util.Optional;
import java.util.UUID;

public class EconomyPlayerUtil {
    // Lookup online player by name
    public static Optional<ServerPlayer> getOnlinePlayer(MinecraftServer server, String name) {
        return server.getPlayerList().getPlayers().stream()
            .filter(p -> p.getGameProfile().name().equalsIgnoreCase(name))
            .findFirst();
    }

    // Lookup UUID by name (online or offline)
    public static Optional<UUID> getUUIDByName(MinecraftServer server, String name) {
        // Try online first
        Optional<ServerPlayer> online = getOnlinePlayer(server, name);
        if (online.isPresent()) return Optional.of(online.get().getUUID());
        // Try offline (GameProfile cache)
        net.minecraft.server.players.NameAndId profile = server.services().nameToIdCache().get(name).orElse(null);
        if (profile != null) return Optional.of(profile.id());
        return Optional.empty();
    }
}
