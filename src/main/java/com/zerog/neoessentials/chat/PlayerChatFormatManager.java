package com.zerog.neoessentials.chat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.zerog.neoessentials.util.ResourceUtil;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.util.Map;
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

    private static final String COLLECTION = "chat_formats";
    private final com.zerog.neoessentials.storage.DataStore store =
        com.zerog.neoessentials.storage.StorageManager.getInstance().getStore();

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

    /** Load from the active {@link com.zerog.neoessentials.storage.DataStore}. */
    public void load() {
        try {
            migrateLegacyFileIfNeeded();
            playerFormats.clear();
            for (Map.Entry<String, JsonObject> entry : store.getAll(COLLECTION).entrySet()) {
                try {
                    UUID uuid = UUID.fromString(entry.getKey());
                    playerFormats.put(uuid, entry.getValue().get("format").getAsString());
                } catch (Exception e) {
                    NeoLog.warn(LOGGER, LogCategory.CHAT, "Skipping malformed entry '{}' in chat_formats storage", entry.getKey());
                }
            }
            NeoLog.info(LOGGER, LogCategory.CHAT, "Loaded {} per-player chat format override(s)", playerFormats.size());
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.CHAT, "Failed to load chat format overrides", e);
        }
    }

    /**
     * One-time import of the legacy {@code chat/player_chat_formats.json} file into the
     * active DataStore, if it's still empty and storage.autoMigrate is enabled.
     */
    private void migrateLegacyFileIfNeeded() {
        if (store.hasAnyData(COLLECTION)) return;
        if (!com.zerog.neoessentials.config.ConfigManager.getInstance().isStorageAutoMigrateEnabled()) return;
        File file = getDataFile();
        if (!file.exists()) return;

        int migrated = 0;
        try (FileReader reader = new FileReader(file)) {
            JsonObject data = GSON.fromJson(reader, JsonObject.class);
            if (data == null) return;
            for (String key : data.keySet()) {
                try {
                    UUID uuid = UUID.fromString(key);
                    JsonObject record = new JsonObject();
                    record.addProperty("format", data.get(key).getAsString());
                    store.put(COLLECTION, uuid.toString(), record);
                    migrated++;
                } catch (IllegalArgumentException e) {
                    NeoLog.warn(LOGGER, LogCategory.CHAT, "Skipping malformed UUID key '{}' in legacy player_chat_formats.json", key);
                }
            }
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.CHAT, "Failed to migrate legacy player_chat_formats.json", e);
        }

        if (migrated > 0) {
            NeoLog.info(LOGGER, LogCategory.CHAT, "PlayerChatFormatManager: migrated {} chat format override(s) from legacy player_chat_formats.json into the '{}' storage backend.",
                migrated, com.zerog.neoessentials.storage.StorageManager.getInstance().getActiveType());
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
        JsonObject record = new JsonObject();
        record.addProperty("format", format);
        store.put(COLLECTION, playerUUID.toString(), record);
        NeoLog.debug(LOGGER, LogCategory.CHAT, "Set per-player chat format override for {}: [{}]", playerUUID, format);
    }

    /**
     * Remove the per-player chat format for {@code playerUUID}.
     *
     * @return {@code true} if an override existed and was removed
     */
    public boolean clearFormat(UUID playerUUID) {
        boolean had = playerFormats.remove(playerUUID) != null;
        if (had) store.delete(COLLECTION, playerUUID.toString());
        return had;
    }

    /** @return {@code true} if a per-player format override exists */
    //noinspection unused
    @SuppressWarnings("unused")
    public boolean hasFormat(UUID playerUUID) {
        return playerFormats.containsKey(playerUUID);
    }

    /** @return number of per-player overrides currently stored */
    public int getCount() {
        return playerFormats.size();
    }
}

