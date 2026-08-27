package com.zerog.neoessentials.leaderboard.adapters;

import com.google.gson.JsonObject;
import com.zerog.neoessentials.leaderboard.StatProvider;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import com.zerog.neoessentials.storage.DataStore;
import com.zerog.neoessentials.storage.StorageManager;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Backs "custom" leaderboard boards — point totals nothing in Minecraft tracks (event
 * scores, minigame points), settable by admins ({@code /leaderboard admin set|add|reset})
 * or by other mods via {@link com.zerog.neoessentials.leaderboard.LeaderboardAPI}.
 *
 * One shared {@code DataStore} collection for every custom board (not one collection per
 * board), records keyed {@code "<boardId>:<uuid>"} — same single-collection,
 * write-immediately pattern as {@code economy/managers/PayToggleManager.java}. A single
 * instance of this class is reused across every {@code type: "custom"} board.
 */
public class CustomStatProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomStatProvider.class);
    private static final String COLLECTION = "leaderboard_custom";

    private final DataStore store = StorageManager.getInstance().getStore();
    private final ConcurrentHashMap<String, Long> cache = new ConcurrentHashMap<>();

    public CustomStatProvider() {
        for (Map.Entry<String, JsonObject> e : store.getAll(COLLECTION).entrySet()) {
            try {
                long value = e.getValue().has("value") ? e.getValue().get("value").getAsLong() : 0L;
                cache.put(e.getKey(), value);
            } catch (Exception ex) {
                LOGGER.error("Failed to load custom leaderboard entry {}: {}", e.getKey(), ex.getMessage());
            }
        }
    }

    private static String key(String boardId, UUID uuid) {
        return boardId.toLowerCase() + ":" + uuid;
    }

    public long get(String boardId, UUID uuid) {
        return cache.getOrDefault(key(boardId, uuid), 0L);
    }

    public void set(String boardId, UUID uuid, long value) {
        String k = key(boardId, uuid);
        cache.put(k, value);
        JsonObject obj = new JsonObject();
        obj.addProperty("value", value);
        store.put(COLLECTION, k, obj);
        NeoLog.debug(LOGGER, LogCategory.GENERAL, "CustomStatProvider: set {} = {}", k, value);
    }

    public void add(String boardId, UUID uuid, long delta) {
        set(boardId, uuid, get(boardId, uuid) + delta);
    }

    public void reset(String boardId, UUID uuid) {
        set(boardId, uuid, 0L);
    }

    /** Removes every entry for a deleted custom board. */
    public void clearBoard(String boardId) {
        String prefix = boardId.toLowerCase() + ":";
        cache.keySet().removeIf(k -> {
            boolean match = k.startsWith(prefix);
            if (match) store.delete(COLLECTION, k);
            return match;
        });
    }

    private Map<UUID, Number> getAllValuesForBoard(String boardId) {
        Map<UUID, Number> out = new LinkedHashMap<>();
        String prefix = boardId.toLowerCase() + ":";
        for (Map.Entry<String, Long> e : cache.entrySet()) {
            if (!e.getKey().startsWith(prefix)) continue;
            try {
                out.put(UUID.fromString(e.getKey().substring(prefix.length())), e.getValue());
            } catch (IllegalArgumentException ignored) {
                NeoLog.debug(LOGGER, LogCategory.GENERAL, "CustomStatProvider: skipping malformed key {}", e.getKey());
            }
        }
        return out;
    }

    /** A {@link StatProvider} view of this shared provider scoped to one board id — this,
     *  not {@code CustomStatProvider} itself, is what gets registered per board, since one
     *  instance backs every custom board. */
    public StatProvider forBoard(String boardId) {
        return new StatProvider() {
            @Override
            public Map<UUID, Number> getAllValues(MinecraftServer server) {
                return getAllValuesForBoard(boardId);
            }
            @Override
            public String formatValue(Number value) {
                return String.valueOf(value.longValue());
            }
        };
    }
}
