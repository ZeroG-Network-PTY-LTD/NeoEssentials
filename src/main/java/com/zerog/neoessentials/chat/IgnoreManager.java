package com.zerog.neoessentials.chat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe manager for player ignore lists.
 * Handles ignoring/unignoring players and message filtering.
 * Ignore lists are persisted to disk and survive server restarts.
 */
public class IgnoreManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(IgnoreManager.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    // Thread-safe storage for ignore relationships: ignorer (lowercase) → set of ignored names (lowercase)
    private static final Map<String, Set<String>> ignoreMap = new ConcurrentHashMap<>();

    private static final File IGNORE_FILE =
        com.zerog.neoessentials.util.ResourceUtil.getDataFile("chat/ignore_lists.json");

    static {
        load();
    }

    // ── Persistence ──────────────────────────────────────────────────────────

    private static void load() {
        if (!IGNORE_FILE.exists()) return;
        try (FileReader fr = new FileReader(IGNORE_FILE)) {
            JsonObject root = GSON.fromJson(fr, JsonObject.class);
            if (root == null) return;
            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                Set<String> ignored = ConcurrentHashMap.newKeySet();
                for (JsonElement el : entry.getValue().getAsJsonArray()) {
                    ignored.add(el.getAsString());
                }
                if (!ignored.isEmpty()) {
                    ignoreMap.put(entry.getKey(), ignored);
                }
            }
            LOGGER.debug("IgnoreManager: loaded ignore data for {} player(s).", ignoreMap.size());
        } catch (Exception e) {
            LOGGER.error("Failed to load ignore_lists.json: {}", e.getMessage());
        }
    }

    private static void save() {
        try {
            File parent = IGNORE_FILE.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                LOGGER.warn("IgnoreManager: failed to create parent directory: {}", parent.getAbsolutePath());
            }
            try (FileWriter fw = new FileWriter(IGNORE_FILE)) {
                JsonObject root = new JsonObject();
                for (Map.Entry<String, Set<String>> entry : ignoreMap.entrySet()) {
                    if (!entry.getValue().isEmpty()) {
                        JsonArray arr = new JsonArray();
                        entry.getValue().forEach(arr::add);
                        root.add(entry.getKey(), arr);
                    }
                }
                GSON.toJson(root, fw);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to save ignore_lists.json: {}", e.getMessage());
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public static void ignore(ServerPlayer player, String targetName) {
        String playerName = player.getName().getString().toLowerCase();
        ignoreMap.computeIfAbsent(playerName, k -> ConcurrentHashMap.newKeySet()).add(targetName.toLowerCase());
        save();
    }

    public static void unignore(ServerPlayer player, String targetName) {
        String playerName = player.getName().getString().toLowerCase();
        Set<String> ignored = ignoreMap.get(playerName);
        if (ignored != null) {
            ignored.remove(targetName.toLowerCase());
            if (ignored.isEmpty()) {
                ignoreMap.remove(playerName);
            }
            save();
        }
    }

    public static boolean isIgnoring(ServerPlayer player, ServerPlayer target) {
        String playerName = player.getName().getString().toLowerCase();
        String targetName = target.getName().getString().toLowerCase();
        Set<String> ignored = ignoreMap.get(playerName);
        return ignored != null && ignored.contains(targetName);
    }

    /**
     * Check if a player is ignoring another player by name
     */
    public static boolean isIgnoring(ServerPlayer player, String targetName) {
        String playerName = player.getName().getString().toLowerCase();
        Set<String> ignored = ignoreMap.get(playerName);
        return ignored != null && ignored.contains(targetName.toLowerCase());
    }

    /**
     * Get the ignore list for a player
     */
    @SuppressWarnings("unused")
    public static Set<String> getIgnoreList(ServerPlayer player) {
        String playerName = player.getName().getString().toLowerCase();
        Set<String> ignored = ignoreMap.get(playerName);
        return ignored != null ? Set.copyOf(ignored) : Set.of();
    }

    /**
     * Clean up transient session data when a player disconnects.
     * NOTE: Ignore lists are persisted and intentionally NOT removed here —
     * a player should still have their ignore list when they reconnect.
     */
    @SuppressWarnings("unused")
    public static void cleanupPlayer(ServerPlayer ignoredPlayer) {
        // Nothing to clean up — ignore lists survive sessions via persistence.
        // Deliberately left empty to avoid accidentally destroying player data.
    }
}
