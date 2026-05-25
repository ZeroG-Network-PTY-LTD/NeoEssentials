package com.zerog.neoessentials.chat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.zerog.neoessentials.util.ChatDebugUtil;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe manager for muted players.
 * Mutes are persisted to disk and survive server restarts.
 * Supports both permanent mutes and timed (expiring) mutes.
 */
public class MuteManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(MuteManager.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    /**
     * Maps lowercase player name → expiry timestamp in ms (0 = permanent).
     */
    private static final Map<String, Long> mutedPlayers = new ConcurrentHashMap<>();

    private static final File MUTE_FILE =
        com.zerog.neoessentials.util.ResourceUtil.getDataFile("moderation/mutes.json");

    static {
        load();
    }

    // ── Persistence ──────────────────────────────────────────────────────────

    private static void load() {
        if (!MUTE_FILE.exists()) return;
        try (FileReader fr = new FileReader(MUTE_FILE)) {
            JsonObject root = GSON.fromJson(fr, JsonObject.class);
            if (root == null) return;
            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                long expireTime = entry.getValue().getAsLong();
                // Skip entries that are already expired (not permanent)
                if (expireTime <= 0 || System.currentTimeMillis() < expireTime) {
                    mutedPlayers.put(entry.getKey(), expireTime);
                }
            }
            LOGGER.info("MuteManager: loaded {} active mute(s).", mutedPlayers.size());
        } catch (Exception e) {
            LOGGER.error("Failed to load mutes.json: {}", e.getMessage());
        }
    }

    private static void save() {
        try {
            File parent = MUTE_FILE.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                LOGGER.warn("MuteManager: failed to create parent directory: {}", parent.getAbsolutePath());
            }
            try (FileWriter fw = new FileWriter(MUTE_FILE)) {
                JsonObject root = new JsonObject();
                for (Map.Entry<String, Long> entry : mutedPlayers.entrySet()) {
                    root.addProperty(entry.getKey(), entry.getValue());
                }
                GSON.toJson(root, fw);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to save mutes.json: {}", e.getMessage());
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Returns a snapshot of all currently-active muted player names (lowercase).
     * Expired timed mutes are excluded.
     */
    public static Set<String> getMutedPlayers() {
        long now = System.currentTimeMillis();
        Set<String> active = new HashSet<>();
        mutedPlayers.forEach((name, expire) -> {
            if (expire == 0 || now < expire) active.add(name);
        });
        return active;
    }

    /** Permanently mute a player by name. */
    public static void mute(String targetName) {
        mute(targetName, 0L);
    }

    /**
     * Mute a player with an optional duration.
     *
     * @param targetName player name (case-insensitive)
     * @param durationMillis 0 for permanent, positive for timed mute
     */
    public static void mute(String targetName, long durationMillis) {
        long expireTime = durationMillis > 0 ? System.currentTimeMillis() + durationMillis : 0L;
        mutedPlayers.put(targetName.toLowerCase(), expireTime);
        save();
        ChatDebugUtil.debug("Muted player %s (expire=%d). Active mutes: %d", targetName, expireTime, mutedPlayers.size());
    }


    public static void unmute(String targetName) {
        mutedPlayers.remove(targetName.toLowerCase());
        save();
        ChatDebugUtil.debug("Unmuted player %s. Active mutes: %d", targetName, mutedPlayers.size());
    }

    public static boolean isMuted(ServerPlayer player) {
        return isMuted(player.getName().getString());
    }

    public static boolean isMuted(String playerName) {
        String key = playerName.toLowerCase();
        Long expireTime = mutedPlayers.get(key);
        if (expireTime == null) return false;
        if (expireTime > 0 && System.currentTimeMillis() >= expireTime) {
            // Timed mute has expired — auto-remove
            mutedPlayers.remove(key);
            save();
            return false;
        }
        return true;
    }

    /**
     * Returns the expiry timestamp (ms) for a muted player, or -1 if not muted,
     * or 0 if permanently muted.
     */
    //noinspection unused
    @SuppressWarnings("unused")
    public static long getMuteExpiry(String playerName) {
        Long expireTime = mutedPlayers.get(playerName.toLowerCase());
        if (expireTime == null) return -1L;
        if (expireTime > 0 && System.currentTimeMillis() >= expireTime) {
            mutedPlayers.remove(playerName.toLowerCase());
            save();
            return -1L;
        }
        return expireTime;
    }
}
