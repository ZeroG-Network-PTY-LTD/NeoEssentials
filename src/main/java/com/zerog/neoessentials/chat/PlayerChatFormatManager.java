package com.zerog.neoessentials.chat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.zerog.neoessentials.util.ResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages per-player chat format overrides.
 * Allows admins to assign a custom chat format to individual players,
 * overriding the group-level and default format for that player only.
 *
 * <p>Persistence: {@code neoessentials/chat/player_chat_formats.json}</p>
 *
 * <p>Format strings support all normal NeoEssentials placeholders, color codes,
 * and rich-text tags (gradient, rainbow, named colors).</p>
 */
public class PlayerChatFormatManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerChatFormatManager.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static volatile PlayerChatFormatManager instance;

    /** UUID → format string */
    private final ConcurrentHashMap<UUID, String> playerFormats = new ConcurrentHashMap<>();
    private File dataFile;

    private PlayerChatFormatManager() {}

    // ── Singleton ─────────────────────────────────────────────────────────────

    public static PlayerChatFormatManager getInstance() {
        if (instance == null) {
            synchronized (PlayerChatFormatManager.class) {
                if (instance == null) {
                    instance = new PlayerChatFormatManager();
                    instance.load();
                }
            }
        }
        return instance;
    }

    // ── I/O ───────────────────────────────────────────────────────────────────

    private File getDataFile() {
        if (dataFile == null) {
            dataFile = ResourceUtil.getDataFile("chat/player_chat_formats.json");
        }
        return dataFile;
    }

    /** Load from disk. Silent no-op when file doesn't exist yet. */
    public void load() {
        try {
            File file = getDataFile();
            if (!file.exists()) return;
            try (FileReader reader = new FileReader(file)) {
                JsonObject data = GSON.fromJson(reader, JsonObject.class);
                if (data != null) {
                    playerFormats.clear();
                    for (String key : data.keySet()) {
                        try {
                            UUID uuid = UUID.fromString(key);
                            playerFormats.put(uuid, data.get(key).getAsString());
                        } catch (IllegalArgumentException ignored) {
                            LOGGER.warn("Skipping malformed UUID key '{}' in player_chat_formats.json", key);
                        }
                    }
                }
            }
            LOGGER.info("Loaded {} per-player chat format override(s)", playerFormats.size());
        } catch (Exception e) {
            LOGGER.error("Failed to load player_chat_formats.json: {}", e.getMessage(), e);
        }
    }

    /** Persist current state to disk. */
    public void save() {
        try {
            File file = getDataFile();
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            JsonObject data = new JsonObject();
            playerFormats.forEach((uuid, fmt) -> data.addProperty(uuid.toString(), fmt));
            try (FileWriter writer = new FileWriter(file)) {
                GSON.toJson(data, writer);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to save player_chat_formats.json: {}", e.getMessage(), e);
        }
    }

    // ── API ───────────────────────────────────────────────────────────────────

    /**
     * Get the per-player chat format for {@code playerUUID}.
     *
     * @return the override format string, or {@code null} if none is set
     */
    public String getFormat(UUID playerUUID) {
        return playerFormats.get(playerUUID);
    }

    /**
     * Set (or replace) the per-player chat format for {@code playerUUID}.
     * Persists immediately.
     */
    public void setFormat(UUID playerUUID, String format) {
        playerFormats.put(playerUUID, format);
        save();
    }

    /**
     * Remove the per-player chat format for {@code playerUUID}.
     *
     * @return {@code true} if an override existed and was removed
     */
    public boolean clearFormat(UUID playerUUID) {
        boolean had = playerFormats.remove(playerUUID) != null;
        if (had) save();
        return had;
    }

    /** @return {@code true} if a per-player format override exists */
    public boolean hasFormat(UUID playerUUID) {
        return playerFormats.containsKey(playerUUID);
    }

    /** @return number of per-player overrides currently stored */
    public int getCount() {
        return playerFormats.size();
    }
}

