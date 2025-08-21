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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class NickManager {
    // Config options
    public boolean enabled = true;
    public String defaultFormat = "<nick>";
    public boolean allowUnsafeCharacters = false;

    public void loadConfig(boolean enabled, String defaultFormat, boolean allowUnsafeCharacters) {
        this.enabled = enabled;
        this.defaultFormat = defaultFormat;
        this.allowUnsafeCharacters = allowUnsafeCharacters;
    }
    private static final Logger LOGGER = LoggerFactory.getLogger(NickManager.class);
    private static NickManager instance;
    private final Map<UUID, String> nicknames = new HashMap<>();
    private final Gson gson = new Gson();
    private Path savePath;

    public static NickManager get() {
        if (instance == null) instance = new NickManager();
        return instance;
    }

    public void load(Path path) {
        this.savePath = path;
        if (!Files.exists(path)) return;
        try {
            String json = Files.readString(path);
            Type type = new TypeToken<Map<String, String>>(){}.getType();
            Map<String, String> raw = gson.fromJson(json, type);
            nicknames.clear();
            if (raw != null) {
                raw.forEach((k, v) -> nicknames.put(UUID.fromString(k), v));
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load nicks.json", e);
        }
    }

    public void save() {
        if (savePath == null) return;
        try {
            Map<String, String> raw = new HashMap<>();
            nicknames.forEach((k, v) -> raw.put(k.toString(), v));
            String json = gson.toJson(raw);
            Files.writeString(savePath, json);
        } catch (IOException e) {
            LOGGER.error("Failed to save nicks.json", e);
        }
    }

    public void setNick(UUID uuid, String nickname) {
        nicknames.put(uuid, nickname);
        save();
    }

    public void clearNick(UUID uuid) {
        nicknames.remove(uuid);
        save();
    }

    public Optional<String> getNick(UUID uuid) {
        return Optional.ofNullable(nicknames.get(uuid));
    }

    public Map<UUID, String> getAllNicks() {
        return new HashMap<>(nicknames);
    }
}
