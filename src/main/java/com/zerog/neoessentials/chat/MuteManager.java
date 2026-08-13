package com.zerog.neoessentials.chat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.storage.DataStore;
import com.zerog.neoessentials.storage.StorageManager;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe manager for muted players and IP addresses.
 * Supports permanent and timed (expiring) mutes, with a reason, staff attribution,
 * per-player/IP history, and an unmute audit trail — matching the richer punishment
 * model used by {@link com.zerog.neoessentials.moderation.BanManager}.
 *
 * <p>Persisted via {@link StorageManager} — one record per mute in the {@code "mutes"}
 * (or {@code "ip_mutes"}) collection, keyed by the mute's own id, with the {@code active}
 * field distinguishing a currently-active mute from an archived (expired/reversed) one —
 * replacing the previous 4-separate-JSON-files scheme (active + history, x2 for IP).
 * The legacy files are imported once, automatically, the first time this runs against
 * an empty store.
 */
public class MuteManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(MuteManager.class);
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final String PLAYER_COLLECTION = "mutes";
    private static final String IP_COLLECTION = "ip_mutes";

    /** A single mute (or IP-mute) record. */
    public static class MuteEntry {
        public String id;
        public String target; // lowercase player name, or IP address for IP mutes
        public String reason;
        public String mutedBy;
        public long muteTime;
        public long expireTime; // 0 = permanent
        public boolean active = true;
        public String unmutedBy;
        public long unmutedAt;

        public MuteEntry(String target, String reason, String mutedBy) {
            this.id = UUID.randomUUID().toString();
            this.target = target;
            this.reason = reason;
            this.mutedBy = mutedBy;
            this.muteTime = System.currentTimeMillis();
            this.expireTime = 0;
        }

        public boolean isExpired() {
            return expireTime > 0 && System.currentTimeMillis() > expireTime;
        }
    }

    private static final DataStore STORE = StorageManager.getInstance().getStore();

    // Active mutes, keyed by lowercase player name / IP address (in-memory cache over the store)
    private static final Map<String, MuteEntry> mutedPlayers = new ConcurrentHashMap<>();
    private static final Map<String, MuteEntry> mutedIPs = new ConcurrentHashMap<>();
    // Archive of reversed/expired mutes
    private static final List<MuteEntry> muteHistory = new ArrayList<>();
    private static final List<MuteEntry> ipMuteHistory = new ArrayList<>();

    static {
        migrateLegacyFilesIfNeeded();
        load();
    }

    // ── Persistence ──────────────────────────────────────────────────────────

    private static void load() {
        loadCollection(PLAYER_COLLECTION, mutedPlayers, muteHistory);
        loadCollection(IP_COLLECTION, mutedIPs, ipMuteHistory);
        LOGGER.info("MuteManager: loaded {} active mute(s), {} active IP mute(s).", mutedPlayers.size(), mutedIPs.size());
    }

    private static void loadCollection(String collection, Map<String, MuteEntry> activeMap, List<MuteEntry> historyList) {
        for (JsonObject obj : STORE.getAll(collection).values()) {
            MuteEntry entry = entryFromJson(obj);
            if (entry.active && !entry.isExpired()) {
                activeMap.put(entry.target, entry);
            } else {
                historyList.add(entry);
            }
        }
    }

    private static MuteEntry entryFromJson(JsonObject obj) {
        MuteEntry entry = new MuteEntry(
            obj.get("target").getAsString(),
            obj.has("reason") && !obj.get("reason").isJsonNull() ? obj.get("reason").getAsString() : null,
            obj.has("mutedBy") && !obj.get("mutedBy").isJsonNull() ? obj.get("mutedBy").getAsString() : null
        );
        entry.id = obj.has("id") ? obj.get("id").getAsString() : UUID.randomUUID().toString();
        entry.muteTime = obj.has("muteTime") ? obj.get("muteTime").getAsLong() : System.currentTimeMillis();
        entry.expireTime = obj.has("expireTime") ? obj.get("expireTime").getAsLong() : 0;
        entry.active = !obj.has("active") || obj.get("active").getAsBoolean();
        entry.unmutedBy = obj.has("unmutedBy") && !obj.get("unmutedBy").isJsonNull() ? obj.get("unmutedBy").getAsString() : null;
        entry.unmutedAt = obj.has("unmutedAt") ? obj.get("unmutedAt").getAsLong() : 0;
        return entry;
    }

    private static JsonObject entryToJson(MuteEntry entry) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", entry.id);
        obj.addProperty("target", entry.target);
        obj.addProperty("reason", entry.reason);
        obj.addProperty("mutedBy", entry.mutedBy);
        obj.addProperty("muteTime", entry.muteTime);
        obj.addProperty("expireTime", entry.expireTime);
        obj.addProperty("active", entry.active);
        obj.addProperty("unmutedBy", entry.unmutedBy);
        obj.addProperty("unmutedAt", entry.unmutedAt);
        return obj;
    }

    private static void persist(String collection, MuteEntry entry) {
        STORE.put(collection, entry.id, entryToJson(entry));
    }

    private static void archiveExpired(MuteEntry entry, List<MuteEntry> history, String collection) {
        entry.active = false;
        entry.unmutedBy = null; // expired naturally, not reversed by staff
        entry.unmutedAt = entry.expireTime;
        history.add(entry);
        persist(collection, entry);
    }

    // ── Public API — players ──────────────────────────────────────────────────

    /**
     * Returns a snapshot of all currently-active muted player names (lowercase).
     * Expired timed mutes are excluded.
     */
    public static Set<String> getMutedPlayers() {
        long now = System.currentTimeMillis();
        Set<String> active = new HashSet<>();
        mutedPlayers.forEach((name, entry) -> {
            if (entry.expireTime == 0 || now < entry.expireTime) active.add(name);
        });
        return active;
    }

    /** Permanently mute a player by name, with no reason/staff attribution recorded. */
    public static void mute(String targetName) {
        mute(targetName, 0L);
    }

    /**
     * Mute a player with an optional duration, with no reason/staff attribution recorded.
     *
     * @param targetName player name (case-insensitive)
     * @param durationMillis 0 for permanent, positive for timed mute
     */
    public static void mute(String targetName, long durationMillis) {
        mute(targetName, null, null, durationMillis);
    }

    /**
     * Mute a player with a reason and staff attribution, matching ban-management plugins'
     * richer punishment record.
     *
     * @param targetName player name (case-insensitive)
     * @param reason why they were muted, or {@code null}
     * @param mutedBy staff username (or "Console"), or {@code null} if unknown
     * @param durationMillis 0 for permanent, positive for timed mute
     */
    public static void mute(String targetName, String reason, String mutedBy, long durationMillis) {
        MuteEntry entry = new MuteEntry(targetName.toLowerCase(), reason, mutedBy);
        entry.expireTime = durationMillis > 0 ? System.currentTimeMillis() + durationMillis : 0L;
        mutedPlayers.put(entry.target, entry);
        persist(PLAYER_COLLECTION, entry);
        NeoLog.debug(LOGGER, LogCategory.MODERATION, "Muted player {} (expire={}). Active mutes: {}", targetName, entry.expireTime, mutedPlayers.size());
    }

    /** Unmute a player with no reversal attribution recorded. */
    public static void unmute(String targetName) {
        unmute(targetName, null);
    }

    /** Unmute a player, archiving the reversed mute with who lifted it and when. */
    public static void unmute(String targetName, String unmutedBy) {
        MuteEntry removed = mutedPlayers.remove(targetName.toLowerCase());
        if (removed != null) {
            removed.active = false;
            removed.unmutedBy = unmutedBy;
            removed.unmutedAt = System.currentTimeMillis();
            muteHistory.add(removed);
            persist(PLAYER_COLLECTION, removed);
        }
        NeoLog.debug(LOGGER, LogCategory.MODERATION, "Unmuted player {}. Active mutes: {}", targetName, mutedPlayers.size());
    }

    public static boolean isMuted(ServerPlayer player) {
        return isMuted(player.getName().getString());
    }

    public static boolean isMuted(String playerName) {
        String key = playerName.toLowerCase();
        MuteEntry entry = mutedPlayers.get(key);
        if (entry == null) return false;
        if (entry.isExpired()) {
            mutedPlayers.remove(key);
            archiveExpired(entry, muteHistory, PLAYER_COLLECTION);
            return false;
        }
        return true;
    }

    /**
     * Returns the expiry timestamp (ms) for a muted player, or -1 if not muted,
     * or 0 if permanently muted.
     */
    @SuppressWarnings("unused")
    public static long getMuteExpiry(String playerName) {
        MuteEntry entry = mutedPlayers.get(playerName.toLowerCase());
        if (entry == null) return -1L;
        if (entry.isExpired()) {
            mutedPlayers.remove(playerName.toLowerCase());
            archiveExpired(entry, muteHistory, PLAYER_COLLECTION);
            return -1L;
        }
        return entry.expireTime;
    }

    /** The active mute entry for a player, or {@code null} if not muted (handles expiry). */
    @SuppressWarnings("unused")
    public static MuteEntry getMuteEntry(String playerName) {
        String key = playerName.toLowerCase();
        MuteEntry entry = mutedPlayers.get(key);
        if (entry == null) return null;
        if (entry.isExpired()) {
            mutedPlayers.remove(key);
            archiveExpired(entry, muteHistory, PLAYER_COLLECTION);
            return null;
        }
        return entry;
    }

    /**
     * Full mute history for a player — any currently-active mute plus every archived
     * (expired or reversed) past mute, newest first.
     */
    public static List<MuteEntry> getMuteHistory(String playerName) {
        String key = playerName.toLowerCase();
        List<MuteEntry> history = new ArrayList<>();
        MuteEntry active = mutedPlayers.get(key);
        if (active != null) history.add(active);
        for (MuteEntry archived : muteHistory) {
            if (archived.target.equals(key)) history.add(archived);
        }
        history.sort((a, b) -> Long.compare(b.muteTime, a.muteTime));
        return history;
    }

    /** Every archived (no-longer-active) mute across all players — for dashboard history views. */
    public static List<MuteEntry> getAllMuteHistory() {
        return new ArrayList<>(muteHistory);
    }

    // ── Public API — IP mutes ────────────────────────────────────────────────

    public static void muteIP(String ipAddress, String reason, String mutedBy, long durationMillis) {
        MuteEntry entry = new MuteEntry(ipAddress, reason, mutedBy);
        entry.expireTime = durationMillis > 0 ? System.currentTimeMillis() + durationMillis : 0L;
        mutedIPs.put(ipAddress, entry);
        persist(IP_COLLECTION, entry);
    }

    public static void unmuteIP(String ipAddress, String unmutedBy) {
        MuteEntry removed = mutedIPs.remove(ipAddress);
        if (removed != null) {
            removed.active = false;
            removed.unmutedBy = unmutedBy;
            removed.unmutedAt = System.currentTimeMillis();
            ipMuteHistory.add(removed);
            persist(IP_COLLECTION, removed);
        }
    }

    public static boolean isIPMuted(String ipAddress) {
        MuteEntry entry = mutedIPs.get(ipAddress);
        if (entry == null) return false;
        if (entry.isExpired()) {
            mutedIPs.remove(ipAddress);
            archiveExpired(entry, ipMuteHistory, IP_COLLECTION);
            return false;
        }
        return true;
    }

    public static List<MuteEntry> getAllIPMutes() {
        return new ArrayList<>(mutedIPs.values());
    }

    public static List<MuteEntry> getIPMuteHistory(String ipAddress) {
        List<MuteEntry> history = new ArrayList<>();
        MuteEntry active = mutedIPs.get(ipAddress);
        if (active != null) history.add(active);
        for (MuteEntry archived : ipMuteHistory) {
            if (archived.target.equals(ipAddress)) history.add(archived);
        }
        history.sort((a, b) -> Long.compare(b.muteTime, a.muteTime));
        return history;
    }

    // ── One-time legacy migration ─────────────────────────────────────────────

    /**
     * Imports the four legacy files (active + history, x2 for IP mutes) into the active
     * {@link DataStore}, if it's still empty and storage.autoMigrate is enabled. Reuses
     * {@link #entryFromJson} to parse both the old rich format and the pre-existing
     * flat {@code {"name": expiryMillis}} format transparently.
     */
    private static void migrateLegacyFilesIfNeeded() {
        if (STORE.hasAnyData(PLAYER_COLLECTION) || STORE.hasAnyData(IP_COLLECTION)) return;
        if (!ConfigManager.getInstance().isStorageAutoMigrateEnabled()) return;

        int migrated = 0;
        migrated += migrateLegacyFile("mutes.json", PLAYER_COLLECTION, true);
        migrated += migrateLegacyFile("mute_history.json", PLAYER_COLLECTION, false);
        migrated += migrateLegacyFile("ip_mutes.json", IP_COLLECTION, true);
        migrated += migrateLegacyFile("ip_mute_history.json", IP_COLLECTION, false);

        if (migrated > 0) {
            LOGGER.info("MuteManager: migrated {} mute record(s) from legacy files into the '{}' storage backend.",
                migrated, StorageManager.getInstance().getActiveType());
        }
    }

    private static int migrateLegacyFile(String filename, String collection, boolean defaultActive) {
        File file = com.zerog.neoessentials.util.ResourceUtil.getDataFile("moderation/" + filename);
        if (!file.exists()) return 0;

        int count = 0;
        try (FileReader fr = new FileReader(file)) {
            JsonObject root = GSON.fromJson(fr, JsonObject.class);
            if (root == null) return 0;

            if (root.has("mutes")) {
                for (JsonElement element : root.getAsJsonArray("mutes")) {
                    JsonObject obj = element.getAsJsonObject().deepCopy();
                    if (!obj.has("active")) obj.addProperty("active", defaultActive);
                    String id = obj.has("id") ? obj.get("id").getAsString() : UUID.randomUUID().toString();
                    STORE.put(collection, id, obj);
                    count++;
                }
            } else {
                // Legacy flat format: {"name": expiryMillis, ...} — only ever the
                // player-mutes file, never IP mutes or history (those never existed
                // in that older, bare-bones era of this class).
                for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                    MuteEntry migrated = new MuteEntry(entry.getKey(), null, null);
                    migrated.expireTime = entry.getValue().getAsLong();
                    STORE.put(collection, migrated.id, entryToJson(migrated));
                    count++;
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to migrate legacy {}: {}", filename, e.getMessage());
        }
        return count;
    }
}
