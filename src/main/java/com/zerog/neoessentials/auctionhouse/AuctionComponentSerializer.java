package com.zerog.neoessentials.auctionhouse;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Serializes and deserializes {@link DataComponentMap} (item NBT/components)
 * to/from JSON strings for SQLite storage.
 */
public class AuctionComponentSerializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuctionComponentSerializer.class);
    private static MinecraftServer server;

    /** Called once the server is available (ServerStartedEvent). */
    public static void setServer(MinecraftServer s) {
        server = s;
    }

    /** Whether {@link #setServer} has run yet — lets a caller that knows it might run before
     *  ServerStartedEvent (e.g. Kit.java's eager first load pass) skip straight to a quiet
     *  "not ready yet" path instead of calling {@link #deserialize}/{@link #serialize} only to
     *  hit the {@link IllegalStateException} below. */
    public static boolean isServerBound() {
        return server != null;
    }

    private static RegistryOps<JsonElement> ops() {
        if (server == null) throw new IllegalStateException("[AuctionHouse] Server not set on ComponentSerializer");
        return RegistryOps.create(JsonOps.INSTANCE, server.registryAccess());
    }

    public static DataComponentMap deserialize(JsonElement json) throws JsonParseException {
        return DataComponentMap.CODEC.parse(ops(), json)
                .ifError(err -> LOGGER.error("[AuctionHouse] Deserialize error: {}", err))
                .result()
                .orElseThrow(() -> new JsonParseException("Failed to deserialize DataComponentMap"));
    }

    public static JsonElement serialize(DataComponentMap components) {
        return DataComponentMap.CODEC.encodeStart(ops(), components)
                .ifError(err -> LOGGER.error("[AuctionHouse] Serialize error: {}", err))
                .result()
                .orElseThrow(() -> new RuntimeException("Failed to serialize DataComponentMap"));
    }
}

