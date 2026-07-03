package com.zerog.neoessentials.util;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.RemoteChatSession;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.world.level.GameType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.UUID;

/**
 * Cross-version-safe {@link ClientboundPlayerInfoUpdatePacket.Entry} construction.
 *
 * <p>The 7-argument canonical constructor
 * {@code Entry(UUID, GameProfile, boolean, int, GameType, Component, RemoteChatSession.Data)}
 * is what exists on 1.21.1 (this mod's compile target), but Mojang has been known to add
 * fields to this record in later versions (e.g. a tab-list ordering field) — the old
 * constructor call can throw {@link NoSuchMethodError} at runtime there (same class of
 * cross-version break as the other Compat helpers in this mod).</p>
 *
 * <p>On that failure, this reflectively finds a constructor whose first 7 parameter types
 * match our known ones (Mojang extends records by appending fields, not reordering them),
 * fills any additional trailing parameters with a zero/false/null default, and invokes it —
 * rather than letting the whole tablist tick loop crash for every player, every tick.</p>
 */
public final class TabListEntryCompat {
    private static final Logger LOGGER = LoggerFactory.getLogger(TabListEntryCompat.class);
    private static volatile boolean loggedFallback = false;
    private static volatile boolean loggedReflectionSuccess = false;

    private TabListEntryCompat() {}

    public static ClientboundPlayerInfoUpdatePacket.Entry create(
            UUID uuid, GameProfile profile, boolean listed, int latency,
            GameType gameMode, Component displayName, RemoteChatSession.Data chatSession) {
        try {
            return new ClientboundPlayerInfoUpdatePacket.Entry(
                uuid, profile, listed, latency, gameMode, displayName, chatSession);
        } catch (NoSuchMethodError | InstantiationError e) {
            ClientboundPlayerInfoUpdatePacket.Entry viaReflection =
                tryModernConstructor(uuid, profile, listed, latency, gameMode, displayName, chatSession);
            if (viaReflection != null) {
                if (!loggedReflectionSuccess) {
                    loggedReflectionSuccess = true;
                    LOGGER.info("ClientboundPlayerInfoUpdatePacket.Entry's 7-arg constructor is unavailable on " +
                        "this Minecraft version — using the modern constructor instead. Fake tablist entries remain functional.");
                }
                return viaReflection;
            }
            if (!loggedFallback) {
                loggedFallback = true;
                LOGGER.warn("ClientboundPlayerInfoUpdatePacket.Entry's 7-arg constructor is unavailable on this " +
                    "Minecraft version, and no matching constructor could be found either — skipping this fake " +
                    "tablist entry. ({})", e.getMessage());
            }
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static ClientboundPlayerInfoUpdatePacket.Entry tryModernConstructor(
            UUID uuid, GameProfile profile, boolean listed, int latency,
            GameType gameMode, Component displayName, RemoteChatSession.Data chatSession) {
        Class<?>[] known = {UUID.class, GameProfile.class, boolean.class, int.class,
            GameType.class, Component.class, RemoteChatSession.Data.class};
        Object[] knownArgs = {uuid, profile, listed, latency, gameMode, displayName, chatSession};

        for (Constructor<?> ctor : ClientboundPlayerInfoUpdatePacket.Entry.class.getDeclaredConstructors()) {
            Class<?>[] params = ctor.getParameterTypes();
            if (params.length < known.length) continue;

            boolean prefixMatches = true;
            for (int i = 0; i < known.length; i++) {
                if (!params[i].equals(known[i])) { prefixMatches = false; break; }
            }
            if (!prefixMatches) continue;

            Object[] args = new Object[params.length];
            System.arraycopy(knownArgs, 0, args, 0, known.length);
            for (int i = known.length; i < params.length; i++) {
                args[i] = defaultFor(params[i]);
            }

            try {
                ctor.setAccessible(true);
                return (ClientboundPlayerInfoUpdatePacket.Entry) ctor.newInstance(args);
            } catch (Exception ignored) {
                // try the next matching constructor, if any
            }
        }
        return null;
    }

    private static Object defaultFor(Class<?> type) {
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == boolean.class) return false;
        if (type == float.class) return 0f;
        if (type == double.class) return 0.0;
        return null;
    }
}
