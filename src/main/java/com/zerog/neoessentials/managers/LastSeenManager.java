package com.zerog.neoessentials.managers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class LastSeenManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(LastSeenManager.class);
    private static LastSeenManager instance;
    private final Map<UUID, Long> lastSeen = new HashMap<>();
    private final Gson gson = new Gson();
    private Path savePath;

    public static LastSeenManager getInstance() {
        if (instance == null) instance = new LastSeenManager();
        return instance;
    }

    public void load(Path path) {
        this.savePath = path;
        if (!Files.exists(path)) return;
        try {
            String json = Files.readString(path);
            Type type = new TypeToken<Map<String, Long>>(){}.getType();
            Map<String, Long> raw = gson.fromJson(json, type);
            lastSeen.clear();
            if (raw != null) {
                raw.forEach((k, v) -> lastSeen.put(UUID.fromString(k), v));
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load seen.json", e);
        }
    }

    public void save() {
        if (savePath == null) return;
        try {
            Map<String, Long> raw = new HashMap<>();
            lastSeen.forEach((k, v) -> raw.put(k.toString(), v));
            String json = gson.toJson(raw);
            Files.writeString(savePath, json);
        } catch (IOException e) {
            LOGGER.error("Failed to save seen.json", e);
        }
    }

    public void setLastSeen(UUID uuid, long timestamp) {
        lastSeen.put(uuid, timestamp);
        save();
    }

    public Optional<Long> getLastSeen(UUID uuid) {
        return Optional.ofNullable(lastSeen.get(uuid));
    }

    public void updateOnLogout(ServerPlayer player) {
        setLastSeen(player.getUUID(), Instant.now().toEpochMilli());
    }

    public void updateOnLogin(ServerPlayer player) {
        setLastSeen(player.getUUID(), Instant.now().toEpochMilli());
    }
}
