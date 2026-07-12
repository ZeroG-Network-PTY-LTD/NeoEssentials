package com.zerog.neoessentials.chat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.zerog.neoessentials.util.ChatDebugUtil;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe manager for muted players and IP addresses.
 * Mutes are persisted to disk and survive server restarts.
 * Supports permanent and timed (expiring) mutes, with a reason, staff attribution,
 * per-player/IP history, and an unmute audit trail — matching the richer punishment
 * model used by {@link com.zerog.neoessentials.moderation.BanManager}.
 */
public class MuteManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(MuteManager.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

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

    // Active mutes, keyed by lowercase player name / IP address
    private static final Map<String, MuteEntry> mutedPlayers = new ConcurrentHashMap<>();
    private static final Map<String, MuteEntry> mutedIPs = new ConcurrentHashMap<>();
    // Archive of reversed/expired mutes — mirrors BanManager's history approach so a
    // player's/IP's full mute record (including who unmuted them and when) survives
    // past the mute itself being lifted, instead of being erased on unmute.
    private static final List<MuteEntry> muteHistory = new ArrayList<>();
    private static final List<MuteEntry> ipMuteHistory = new ArrayList<>();

    private static final File MUTE_FILE =
        com.zerog.neoessentials.util.ResourceUtil.getDataFile("moderation/mutes.json");
    private static final File IP_MUTE_FILE =
        com.zerog.neoessentials.util.ResourceUtil.getDataFile("moderation/ip_mutes.json");
    private static final File MUTE_HISTORY_FILE =
        com.zerog.neoessentials.util.ResourceUtil.getDataFile("moderation/mute_history.json");
    private static final File IP_MUTE_HISTORY_FILE =
        com.zerog.neoessentials.util.ResourceUtil.getDataFile("moderation/ip_mute_history.json");

    static {
        load();
    }

    // ── Persistence ──────────────────────────────────────────────────────────

    private static void load() {
        loadMutes(MUTE_FILE, mutedPlayers, true);
        loadMutes(IP_MUTE_FILE, mutedIPs, false);
        loadHistory(MUTE_HISTORY_FILE, muteHistory);
        loadHistory(IP_MUTE_HISTORY_FILE, ipMuteHistory);
        LOGGER.info("MuteManager: loaded {} active mute(s), {} active IP mute(s).", mutedPlayers.size(), mutedIPs.size());
    }

    /**
     * Loads either the current rich format ({@code {"mutes":[{...}]}}) or the legacy flat
     * format ({@code {"name": expiryMillis}} written before this class tracked reason/staff/
     * history) — old files are transparently migrated into MuteEntry objects with a null
     * reason/mutedBy on next load, rather than losing the still-valid expiry data.
     */
    private static void loadMutes(File file, Map<String, MuteEntry> target, boolean isPlayerMap) {
        if (!file.exists()) return;
        try (FileReader fr = new FileReader(file)) {
            JsonObject root = GSON.fromJson(fr, JsonObject.class);
            if (root == null) return;

            if (root.has("mutes")) {
                for (JsonElement element : root.getAsJsonArray("mutes")) {
                    JsonObject obj = element.getAsJsonObject();
                    MuteEntry entry = entryFromJson(obj);
                    if (!entry.isExpired()) {
                        target.put(entry.target, entry);
                    }
                }
                return;
            }

            // Legacy flat format: {"name": expiryMillis, ...}
            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                long expireTime = entry.getValue().getAsLong();
                if (expireTime <= 0 || System.currentTimeMillis() < expireTime) {
                    MuteEntry migrated = new MuteEntry(entry.getKey(), null, null);
                    migrated.expireTime = expireTime;
                    target.put(entry.getKey(), migrated);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load {}: {}", file.getName(), e.getMessage());
        }
    }

    private static void loadHistory(File file, List<MuteEntry> target) {
        if (!file.exists()) return;
        try (FileReader fr = new FileReader(file)) {
            JsonObject root = GSON.fromJson(fr, JsonObject.class);
            if (root == null || !root.has("mutes")) return;
            for (JsonElement element : root.getAsJsonArray("mutes")) {
                target.add(entryFromJson(element.getAsJsonObject()));
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load {}: {}", file.getName(), e.getMessage());
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

    private static void saveMutes(File file, Map<String, MuteEntry> source) {
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                LOGGER.warn("MuteManager: failed to create parent directory: {}", parent.getAbsolutePath());
            }
            try (FileWriter fw = new FileWriter(file)) {
                JsonObject root = new JsonObject();
                JsonArray arr = new JsonArray();
                for (MuteEntry entry : source.values()) {
                    arr.add(entryToJson(entry));
                }
                root.add("mutes", arr);
                GSON.toJson(root, fw);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to save {}: {}", file.getName(), e.getMessage());
        }
    }

    private static void saveHistory(File file, List<MuteEntry> source) {
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                LOGGER.warn("MuteManager: failed to create parent directory: {}", parent.getAbsolutePath());
            }
            try (FileWriter fw = new FileWriter(file)) {
                JsonObject root = new JsonObject();
                JsonArray arr = new JsonArray();
                for (MuteEntry entry : source) {
                    arr.add(entryToJson(entry));
                }
                root.add("mutes", arr);
                GSON.toJson(root, fw);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to save {}: {}", file.getName(), e.getMessage());
        }
    }

    private static void archiveExpired(MuteEntry entry, List<MuteEntry> history, File historyFile) {
        entry.active = false;
        entry.unmutedBy = null; // expired naturally, not reversed by staff
        entry.unmutedAt = entry.expireTime;
        history.add(entry);
        saveHistory(historyFile, history);
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
        saveMutes(MUTE_FILE, mutedPlayers);
        ChatDebugUtil.debug("Muted player %s (expire=%d). Active mutes: %d", targetName, entry.expireTime, mutedPlayers.size());
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
            saveHistory(MUTE_HISTORY_FILE, muteHistory);
            saveMutes(MUTE_FILE, mutedPlayers);
        }
        ChatDebugUtil.debug("Unmuted player %s. Active mutes: %d", targetName, mutedPlayers.size());
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
            archiveExpired(entry, muteHistory, MUTE_HISTORY_FILE);
            saveMutes(MUTE_FILE, mutedPlayers);
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
            archiveExpired(entry, muteHistory, MUTE_HISTORY_FILE);
            saveMutes(MUTE_FILE, mutedPlayers);
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
            archiveExpired(entry, muteHistory, MUTE_HISTORY_FILE);
            saveMutes(MUTE_FILE, mutedPlayers);
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
        saveMutes(IP_MUTE_FILE, mutedIPs);
    }

    public static void unmuteIP(String ipAddress, String unmutedBy) {
        MuteEntry removed = mutedIPs.remove(ipAddress);
        if (removed != null) {
            removed.active = false;
            removed.unmutedBy = unmutedBy;
            removed.unmutedAt = System.currentTimeMillis();
            ipMuteHistory.add(removed);
            saveHistory(IP_MUTE_HISTORY_FILE, ipMuteHistory);
            saveMutes(IP_MUTE_FILE, mutedIPs);
        }
    }

    public static boolean isIPMuted(String ipAddress) {
        MuteEntry entry = mutedIPs.get(ipAddress);
        if (entry == null) return false;
        if (entry.isExpired()) {
            mutedIPs.remove(ipAddress);
            archiveExpired(entry, ipMuteHistory, IP_MUTE_HISTORY_FILE);
            saveMutes(IP_MUTE_FILE, mutedIPs);
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
}
