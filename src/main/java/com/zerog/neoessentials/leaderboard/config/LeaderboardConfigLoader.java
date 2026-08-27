package com.zerog.neoessentials.leaderboard.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.leaderboard.LeaderboardDefinition;
import com.zerog.neoessentials.leaderboard.LeaderboardManager;
import com.zerog.neoessentials.leaderboard.adapters.CustomStatProvider;
import com.zerog.neoessentials.leaderboard.adapters.EconomyStatProvider;
import com.zerog.neoessentials.leaderboard.adapters.VanillaStatProvider;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Reads {@code leaderboard.json} and registers each enabled board into
 * {@link LeaderboardManager}, replacing the old hardcoded-in-Java board list. A malformed or
 * unresolvable board definition logs a warning and is skipped — one bad entry can't break
 * server startup.
 */
public final class LeaderboardConfigLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(LeaderboardConfigLoader.class);

    /** Single shared instance backing every "custom" board — see {@link CustomStatProvider}. */
    private static final CustomStatProvider CUSTOM_STATS = new CustomStatProvider();
    /** Ids of boards whose type is "custom" — the only ones set/add/reset/delete may target.
     *  A board can be config-managed (economy/vanilla_stat) without being custom, so this is
     *  tracked separately from {@link LeaderboardManager#isConfigManaged}. */
    private static final Set<String> CUSTOM_BOARD_IDS = new LinkedHashSet<>();

    private LeaderboardConfigLoader() {}

    public static CustomStatProvider customStats() { return CUSTOM_STATS; }

    public static boolean isCustomBoard(String id) {
        return id != null && CUSTOM_BOARD_IDS.contains(id.toLowerCase());
    }

    public static void load() {
        LeaderboardManager manager = LeaderboardManager.getInstance();
        manager.clearConfigManagedBoards();
        CUSTOM_BOARD_IDS.clear();

        JsonObject root = ConfigManager.getInstance().getConfig(ConfigManager.LEADERBOARD_CONFIG);
        JsonObject lb = (root != null && root.has("leaderboard")) ? root.getAsJsonObject("leaderboard") : null;
        if (lb == null || !lb.has("boards") || !lb.get("boards").isJsonArray()) {
            NeoLog.info(LOGGER, LogCategory.GENERAL, "LeaderboardConfigLoader: no boards configured.");
            return;
        }

        int registered = 0;
        for (JsonElement el : lb.getAsJsonArray("boards")) {
            if (!el.isJsonObject()) continue;
            JsonObject b = el.getAsJsonObject();
            if (b.has("enabled") && !b.get("enabled").getAsBoolean()) continue;
            if (registerFromJson(manager, b)) registered++;
        }
        NeoLog.info(LOGGER, LogCategory.GENERAL, "LeaderboardConfigLoader: registered {} board(s) from leaderboard.json", registered);
    }

    private static boolean registerFromJson(LeaderboardManager manager, JsonObject b) {
        if (!b.has("id") || !b.has("type")) {
            LOGGER.warn("LeaderboardConfigLoader: board entry missing 'id' or 'type' — skipping: {}", b);
            return false;
        }
        String id = b.get("id").getAsString();
        String type = b.get("type").getAsString();
        String displayName = b.has("displayName") ? b.get("displayName").getAsString() : id;
        String format = b.has("format") ? b.get("format").getAsString() : "integer";
        boolean higherIsBetter = !b.has("higherIsBetter") || b.get("higherIsBetter").getAsBoolean();
        String exemptPermission = b.has("exemptPermission") && !b.get("exemptPermission").isJsonNull()
            ? b.get("exemptPermission").getAsString() : null;

        LeaderboardDefinition definition = new LeaderboardDefinition(id, displayName, exemptPermission, higherIsBetter);

        switch (type) {
            case "economy" -> {
                manager.registerBoard(definition, new EconomyStatProvider(), true);
                return true;
            }
            case "vanilla_stat" -> {
                if (!b.has("stat")) {
                    LOGGER.warn("LeaderboardConfigLoader: vanilla_stat board '{}' is missing 'stat' — skipping.", id);
                    return false;
                }
                String statKey = b.get("stat").getAsString();
                var provider = VanillaStatProvider.fromStatKey(statKey, "time".equals(format));
                if (provider.isEmpty()) return false; // already logged by fromStatKey
                manager.registerBoard(definition, provider.get(), true);
                return true;
            }
            case "custom" -> {
                manager.registerBoard(definition, CUSTOM_STATS.forBoard(id), true);
                CUSTOM_BOARD_IDS.add(id.toLowerCase());
                return true;
            }
            default -> {
                LOGGER.warn("LeaderboardConfigLoader: board '{}' has unknown type '{}' — skipping.", id, type);
                return false;
            }
        }
    }

    // ── Persistence (dashboard/command-driven board create/delete) ──────────
    /** Appends a new "custom" board definition to leaderboard.json and registers it live. */
    public static void addCustomBoard(String id, String displayName) {
        JsonObject root = ConfigManager.getInstance().getConfig(ConfigManager.LEADERBOARD_CONFIG);
        JsonObject lb = root.has("leaderboard") ? root.getAsJsonObject("leaderboard") : new JsonObject();
        JsonArray boards = lb.has("boards") && lb.get("boards").isJsonArray() ? lb.getAsJsonArray("boards") : new JsonArray();

        // Replace an existing entry with the same id, if any.
        JsonArray rebuilt = new JsonArray();
        for (JsonElement el : boards) {
            if (el.isJsonObject() && el.getAsJsonObject().has("id")
                    && el.getAsJsonObject().get("id").getAsString().equalsIgnoreCase(id)) {
                continue;
            }
            rebuilt.add(el);
        }
        JsonObject entry = new JsonObject();
        entry.addProperty("id", id);
        entry.addProperty("type", "custom");
        entry.addProperty("displayName", displayName);
        entry.addProperty("format", "integer");
        entry.addProperty("higherIsBetter", true);
        entry.addProperty("enabled", true);
        rebuilt.add(entry);

        lb.add("boards", rebuilt);
        root.add("leaderboard", lb);
        ConfigManager.getInstance().saveConfig(ConfigManager.LEADERBOARD_CONFIG, root);

        LeaderboardManager.getInstance().registerBoard(
            new LeaderboardDefinition(id, displayName, null, true), CUSTOM_STATS.forBoard(id), true);
        CUSTOM_BOARD_IDS.add(id.toLowerCase());
    }

    /** Removes a "custom" board from leaderboard.json and live registration. Returns false
     *  if the board doesn't exist or isn't a custom board (config-file-only types can't be
     *  deleted this way — hand-edit the file instead). */
    public static boolean deleteCustomBoard(String id) {
        if (!isCustomBoard(id)) return false;

        JsonObject root = ConfigManager.getInstance().getConfig(ConfigManager.LEADERBOARD_CONFIG);
        if (!root.has("leaderboard")) return false;
        JsonObject lb = root.getAsJsonObject("leaderboard");
        if (!lb.has("boards") || !lb.get("boards").isJsonArray()) return false;

        JsonArray rebuilt = new JsonArray();
        boolean found = false;
        for (JsonElement el : lb.getAsJsonArray("boards")) {
            if (el.isJsonObject() && el.getAsJsonObject().has("id")
                    && el.getAsJsonObject().get("id").getAsString().equalsIgnoreCase(id)
                    && "custom".equals(el.getAsJsonObject().has("type") ? el.getAsJsonObject().get("type").getAsString() : null)) {
                found = true;
                continue;
            }
            rebuilt.add(el);
        }
        if (!found) return false;

        lb.add("boards", rebuilt);
        root.add("leaderboard", lb);
        ConfigManager.getInstance().saveConfig(ConfigManager.LEADERBOARD_CONFIG, root);

        LeaderboardManager.getInstance().unregisterBoard(id);
        CUSTOM_STATS.clearBoard(id);
        CUSTOM_BOARD_IDS.remove(id.toLowerCase());
        return true;
    }
}
