package com.zerog.neoessentials.sidebar;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.zerog.neoessentials.api.PlaceholderAPI;
import com.zerog.neoessentials.chat.RichTextFormatter;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import com.zerog.neoessentials.tablist.AnimationManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.BlankFormat;
import net.minecraft.network.protocol.game.ClientboundResetScorePacket;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NeoEssentials sidebar scoreboard system.
 *
 * <p>Unlike {@link com.zerog.neoessentials.tablist.TablistManager}'s use of the server's
 * real, shared {@link net.minecraft.server.ServerScoreboard} for nametag/tablist-row
 * prefixes (safe there because a player's own row looks the same to every viewer), a
 * sidebar board is inherently per-VIEWER: two players can simultaneously see different
 * boards (different world/permission conditions) or the same board with different
 * placeholder values (e.g. their own balance). The real {@code ServerScoreboard} broadcasts
 * every objective/team/score change to all connected players, so it can't represent two
 * different simultaneous states. Instead, every packet here is built against a private,
 * never-registered {@link Scoreboard} instance purely to serialize {@link Objective}/
 * {@link PlayerTeam} objects, and sent directly to one player's connection — the same
 * technique {@code TablistManager} already uses for {@code ClientboundTabListPacket}, just
 * extended to the objective/team/score packets a sidebar needs.
 */
public class ScoreboardManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScoreboardManager.class);
    private static final ScoreboardManager INSTANCE = new ScoreboardManager();
    public static ScoreboardManager getInstance() { return INSTANCE; }

    private static final int MAX_LINES = 15;
    private static final String OBJECTIVE_NAME = "ne_sidebar";
    /** One invisible, unique fake score-holder string per line slot (§0 .. §e). */
    private static final String[] SLOT_ENTRIES = new String[MAX_LINES];
    static {
        String hex = "0123456789abcde";
        for (int i = 0; i < MAX_LINES; i++) SLOT_ENTRIES[i] = "§" + hex.charAt(i);
    }

    /** Throwaway, never-registered scoreboard — exists only so Objective/PlayerTeam objects
     *  can be constructed for packet serialization without touching real server state. */
    private final Scoreboard packetScoreboard = new Scoreboard();

    // ── Config ────────────────────────────────────────────────────────────────
    private boolean enabled = true;
    private int refreshIntervalTicks = 20;
    private int joinDelayTicks = 40;
    private List<ScoreboardBoard> boards = new ArrayList<>();

    private static class Override {
        String title;
        final Map<Integer, String> lines = new LinkedHashMap<>();
    }
    private final Map<String, Override> groupOverrides = new ConcurrentHashMap<>();
    private final Map<UUID, Override> playerOverrides = new ConcurrentHashMap<>();

    // ── Runtime state ─────────────────────────────────────────────────────────
    private int tickCounter = 0;
    private int animFrame = 0;
    private final Map<UUID, Integer> joinTick = new ConcurrentHashMap<>();
    private int globalTick = 0;

    // Per-viewer dirty cache / active-slot bookkeeping
    private final Map<UUID, String> lastTitle = new ConcurrentHashMap<>();
    private final Map<UUID, String[]> lastLines = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> active = new ConcurrentHashMap<>();

    private ScoreboardManager() {}

    // ── Config loading ───────────────────────────────────────────────────────
    public void loadConfig() {
        // Scoreboard lines/titles can reference {animation:NAME} tokens, but animations.json
        // is a shared resource also used by tablist/holograms/chat — TablistManager.loadConfig()
        // already refreshes it, but /scoreboard reload (and the scoreboard-only block in
        // /neoe reload) called ONLY this method, never touching AnimationManager at all. So
        // editing animations.json and reloading via the scoreboard command specifically left
        // the in-memory animation frames stale until a /tablist reload also happened to run.
        AnimationManager.getInstance().loadConfig();
        try {
            JsonObject root = ConfigManager.getInstance().getConfig(ConfigManager.SCOREBOARD_CONFIG);
            JsonObject sb = (root != null && root.has("scoreboard")) ? root.getAsJsonObject("scoreboard") : null;
            if (sb == null) {
                NeoLog.info(LOGGER, LogCategory.GENERAL, "ScoreboardManager: no scoreboard configuration found — using defaults.");
                return;
            }

            enabled = !sb.has("enabled") || sb.get("enabled").getAsBoolean();
            refreshIntervalTicks = sb.has("refreshInterval") ? sb.get("refreshInterval").getAsInt() : 20;
            joinDelayTicks = sb.has("joinDelayTicks") ? sb.get("joinDelayTicks").getAsInt() : 40;

            List<ScoreboardBoard> loaded = new ArrayList<>();
            if (sb.has("boards") && sb.get("boards").isJsonArray()) {
                for (JsonElement el : sb.getAsJsonArray("boards")) {
                    if (!el.isJsonObject()) continue;
                    JsonObject b = el.getAsJsonObject();
                    String name = b.has("name") ? b.get("name").getAsString() : ("board" + loaded.size());
                    int priority = b.has("priority") ? b.get("priority").getAsInt() : 0;
                    List<String> conditions = readStringList(b.get("conditions"));
                    List<String> titleFrames = b.has("title") ? loadFrames(b.get("title")) : List.of("");
                    List<ScoreboardLine> lines = new ArrayList<>();
                    if (b.has("lines") && b.get("lines").isJsonArray()) {
                        for (JsonElement lineEl : b.getAsJsonArray("lines")) {
                            if (lines.size() >= MAX_LINES) {
                                LOGGER.warn("ScoreboardManager: board '{}' has more than {} lines — truncating.", name, MAX_LINES);
                                break;
                            }
                            if (!lineEl.isJsonObject()) continue;
                            JsonObject lo = lineEl.getAsJsonObject();
                            List<String> frames = lo.has("text") ? loadFrames(lo.get("text")) : List.of("");
                            String condition = lo.has("condition") && !lo.get("condition").isJsonNull()
                                ? lo.get("condition").getAsString() : null;
                            lines.add(new ScoreboardLine(frames, condition));
                        }
                    }
                    int refreshMultiplier = b.has("refreshMultiplier") ? b.get("refreshMultiplier").getAsInt() : 1;
                    loaded.add(new ScoreboardBoard(name, priority, conditions, titleFrames, lines, refreshMultiplier));
                }
            }
            // Highest priority first; a no-condition board should be given priority 0 so it
            // naturally sorts last and acts as the fallback.
            loaded.sort((a, c) -> Integer.compare(c.getPriority(), a.getPriority()));
            boards = loaded;

            groupOverrides.clear();
            if (sb.has("groups") && sb.get("groups").isJsonObject()) {
                for (var entry : sb.getAsJsonObject("groups").entrySet()) {
                    groupOverrides.put(entry.getKey(), readOverride(entry.getValue()));
                }
            }

            playerOverrides.clear();
            if (sb.has("players") && sb.get("players").isJsonObject()) {
                for (var entry : sb.getAsJsonObject("players").entrySet()) {
                    try {
                        UUID uuid = UUID.fromString(entry.getKey());
                        playerOverrides.put(uuid, readOverride(entry.getValue()));
                    } catch (IllegalArgumentException ignored) {
                        LOGGER.warn("ScoreboardManager: invalid UUID in 'players' section: {}", entry.getKey());
                    }
                }
            }

            // Force a fresh send of CONTENT for everyone after reload — same reasoning as
            // TablistManager.loadConfig() clearing its dirty cache. Deliberately does NOT
            // clear `active`: that map tracks whether a player's client already has the
            // "ne_sidebar" objective registered, which reload doesn't change. Clearing it too
            // used to make the very next update think every currently-displayed player needed
            // a fresh METHOD_ADD — but the client already has that objective from before, and
            // vanilla's ClientPacketListener.handleAddObjective throws (disconnecting the
            // player with "Network Protocol Error") when told to add one that already exists.
            // Leaving `active` alone makes the next update correctly take the METHOD_CHANGE +
            // per-slot diff path instead, refreshing content without re-adding anything.
            lastTitle.clear();
            lastLines.clear();
            tickCounter = 0;
            animFrame = 0;

            NeoLog.info(LOGGER, LogCategory.GENERAL,
                "ScoreboardManager loaded — {} board(s), refresh every {} ticks, join delay {} ticks.",
                boards.size(), refreshIntervalTicks, joinDelayTicks);
        } catch (Exception e) {
            LOGGER.error("Failed to load scoreboard config: {}", e.getMessage());
        }
    }

    private static Override readOverride(JsonElement el) {
        Override ov = new Override();
        if (!el.isJsonObject()) return ov;
        JsonObject o = el.getAsJsonObject();
        if (o.has("titleOverride")) ov.title = o.get("titleOverride").getAsString();
        if (o.has("lineOverrides") && o.get("lineOverrides").isJsonObject()) {
            for (var e : o.getAsJsonObject("lineOverrides").entrySet()) {
                try {
                    ov.lines.put(Integer.parseInt(e.getKey()), e.getValue().getAsString());
                } catch (NumberFormatException ignored) {
                    LOGGER.warn("ScoreboardManager: lineOverrides key '{}' is not a line index", e.getKey());
                }
            }
        }
        return ov;
    }

    private static List<String> readStringList(JsonElement el) {
        List<String> out = new ArrayList<>();
        if (el == null || el.isJsonNull()) return out;
        if (el.isJsonArray()) {
            for (JsonElement e : el.getAsJsonArray()) out.add(e.getAsString());
        } else {
            out.add(el.getAsString());
        }
        return out;
    }

    private static List<String> loadFrames(JsonElement el) {
        List<String> frames = new ArrayList<>();
        if (el.isJsonArray()) {
            for (JsonElement e : el.getAsJsonArray()) frames.add(e.getAsString());
        } else {
            frames.add(el.getAsString());
        }
        return frames;
    }

    // ── Tick ──────────────────────────────────────────────────────────────────
    public void onTick(MinecraftServer server) {
        globalTick++;
        if (!enabled) return;
        tickCounter++;
        if (tickCounter < refreshIntervalTicks) return;
        tickCounter = 0;
        animFrame++;
        for (ScoreboardBoard board : boards) board.tickOwnFrame();
        updateAll(server);
    }

    public void updateAll(MinecraftServer server) {
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            updatePlayer(player, server);
        }
    }

    // ── Board resolution ─────────────────────────────────────────────────────
    public ScoreboardBoard resolveBoardForPlayer(ServerPlayer player) {
        for (ScoreboardBoard board : boards) {
            if (ConditionEvaluator.evaluateAll(board.getConditions(), player)) return board;
        }
        return null;
    }

    private Override overrideFor(ServerPlayer player) {
        Override result = new Override();
        String group = getPermissionGroup(player);
        Override groupOv = groupOverrides.get(group);
        if (groupOv != null) {
            result.title = groupOv.title;
            result.lines.putAll(groupOv.lines);
        }
        Override playerOv = playerOverrides.get(player.getUUID());
        if (playerOv != null) {
            if (playerOv.title != null) result.title = playerOv.title;
            result.lines.putAll(playerOv.lines);
        }
        return result;
    }

    // ── Update one player ────────────────────────────────────────────────────
    public void updatePlayer(ServerPlayer player, MinecraftServer server) {
        UUID uuid = player.getUUID();
        boolean shouldShow = enabled
            && ScoreboardToggleManager.getInstance().isEnabled(uuid)
            && globalTick - joinTick.getOrDefault(uuid, globalTick) >= joinDelayTicks;

        if (!shouldShow) {
            hidePlayer(player);
            return;
        }

        ScoreboardBoard board = resolveBoardForPlayer(player);
        if (board == null) {
            hidePlayer(player);
            return;
        }

        try {
            Override ov = overrideFor(player);

            // The board's own frame index (advanced once per global refresh cycle, but at
            // 1/refreshMultiplier the rate — see ScoreboardBoard.tickOwnFrame()), not the
            // manager's shared `animFrame`, so a board configured with refreshMultiplier > 1
            // actually cycles its title/line frames slower than the rest.
            int frame = board.getOwnAnimFrame();
            String rawTitle = ov.title != null ? ov.title : board.currentTitleFrame(frame);
            String title = resolveText(rawTitle, player, server);

            List<String> visibleLines = new ArrayList<>();
            List<ScoreboardLine> boardLines = board.getLines();
            for (int i = 0; i < boardLines.size() && visibleLines.size() < MAX_LINES; i++) {
                ScoreboardLine line = boardLines.get(i);
                if (!ConditionEvaluator.evaluateAll(
                        line.getCondition() != null ? List.of(line.getCondition()) : List.of(), player)) {
                    continue;
                }
                String raw = ov.lines.containsKey(i) ? ov.lines.get(i) : line.currentFrame(frame);
                visibleLines.add(resolveText(raw, player, server));
            }

            sendIfChanged(player, title, visibleLines);
        } catch (Throwable e) {
            NeoLog.debug(LOGGER, LogCategory.GENERAL, "Failed to update scoreboard for {}: {}", player.getName().getString(), e.getMessage());
        }
    }

    // ── Packet send (dirty-checked) ──────────────────────────────────────────
    private void sendIfChanged(ServerPlayer player, String title, List<String> lines) {
        UUID uuid = player.getUUID();
        String[] linesArr = lines.toArray(new String[0]);

        boolean wasActive = active.getOrDefault(uuid, false);
        boolean titleChanged = !title.equals(lastTitle.get(uuid));
        boolean linesChanged = !Arrays.equals(linesArr, lastLines.get(uuid));

        if (wasActive && !titleChanged && !linesChanged) return; // nothing changed — no packets

        Component titleComp = RichTextFormatter.processTablistText(title);
        Objective objective = new Objective(packetScoreboard, OBJECTIVE_NAME, ObjectiveCriteria.DUMMY,
            titleComp, ObjectiveCriteria.RenderType.INTEGER, false, BlankFormat.INSTANCE);

        if (!wasActive) {
            player.connection.send(new ClientboundSetObjectivePacket(objective, ClientboundSetObjectivePacket.METHOD_ADD));
            player.connection.send(new ClientboundSetDisplayObjectivePacket(DisplaySlot.SIDEBAR, objective));
        } else if (titleChanged) {
            player.connection.send(new ClientboundSetObjectivePacket(objective, ClientboundSetObjectivePacket.METHOD_CHANGE));
        }

        String[] previousLines = lastLines.getOrDefault(uuid, new String[0]);
        for (int slot = 0; slot < MAX_LINES; slot++) {
            boolean wasVisible = slot < previousLines.length;
            boolean isVisible = slot < linesArr.length;
            String entry = SLOT_ENTRIES[slot];

            if (!isVisible) {
                if (wasVisible) {
                    player.connection.send(new ClientboundResetScorePacket(entry, OBJECTIVE_NAME));
                    PlayerTeam staleTeam = new PlayerTeam(packetScoreboard, "ne_sb" + slot);
                    player.connection.send(ClientboundSetPlayerTeamPacket.createRemovePacket(staleTeam));
                }
                continue;
            }
            if (wasVisible && linesArr[slot].equals(previousLines[slot])) continue; // unchanged slot

            PlayerTeam team = new PlayerTeam(packetScoreboard, "ne_sb" + slot);
            team.getPlayers().add(entry);
            team.setPlayerPrefix(RichTextFormatter.processTablistText(linesArr[slot]));
            player.connection.send(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(team, true));
            player.connection.send(new ClientboundSetScorePacket(entry, OBJECTIVE_NAME, MAX_LINES - slot,
                java.util.Optional.empty(), java.util.Optional.of(BlankFormat.INSTANCE)));
        }

        active.put(uuid, true);
        lastTitle.put(uuid, title);
        lastLines.put(uuid, linesArr);
    }

    private void hidePlayer(ServerPlayer player) {
        UUID uuid = player.getUUID();
        if (!active.getOrDefault(uuid, false)) return;
        try {
            player.connection.send(new ClientboundSetDisplayObjectivePacket(DisplaySlot.SIDEBAR, null));
            String[] previousLines = lastLines.getOrDefault(uuid, new String[0]);
            for (int slot = 0; slot < previousLines.length; slot++) {
                player.connection.send(new ClientboundResetScorePacket(SLOT_ENTRIES[slot], OBJECTIVE_NAME));
                PlayerTeam staleTeam = new PlayerTeam(packetScoreboard, "ne_sb" + slot);
                player.connection.send(ClientboundSetPlayerTeamPacket.createRemovePacket(staleTeam));
            }
            // Also remove the objective itself — otherwise the client still thinks "ne_sidebar"
            // exists (just undisplayed), and a later re-enable's METHOD_ADD would target an
            // objective name the client already has registered.
            Objective staleObjective = new Objective(packetScoreboard, OBJECTIVE_NAME, ObjectiveCriteria.DUMMY,
                Component.empty(), ObjectiveCriteria.RenderType.INTEGER, false, BlankFormat.INSTANCE);
            player.connection.send(new ClientboundSetObjectivePacket(staleObjective, ClientboundSetObjectivePacket.METHOD_REMOVE));
        } catch (Throwable e) {
            NeoLog.debug(LOGGER, LogCategory.GENERAL, "Failed to hide scoreboard for {}: {}", player.getName().getString(), e.getMessage());
        }
        active.put(uuid, false);
        lastTitle.remove(uuid);
        lastLines.remove(uuid);
    }

    // ── Placeholders ──────────────────────────────────────────────────────────
    /** Shorthand tokens (matching tablist's convention), then AnimationManager, then the
     *  full PlaceholderAPI registry for everything else (including {@code {neoessentials_*}}). */
    private String resolveText(String text, ServerPlayer player, MinecraftServer server) {
        if (text == null || text.isEmpty()) return "";

        int online = server.getPlayerList().getPlayers().size();
        int max = server.getMaxPlayers();
        String world = com.zerog.neoessentials.util.LevelCompat.of(player).dimension().location().getPath();
        String balance = "0";
        try {
            java.math.BigDecimal bd = com.zerog.neoessentials.economy.managers.EconomyManager.getInstance().getBalance(player.getUUID());
            balance = String.format("%.2f", bd.doubleValue());
        } catch (Exception e) {
            NeoLog.debug(LOGGER, LogCategory.GENERAL, "Failed to resolve balance for {} scoreboard placeholder", player.getName().getString(), e);
        }
        String group = getPermissionGroup(player);
        String prefix = text.contains("{prefix}") ? safePrefix(player) : "";
        String suffix = text.contains("{suffix}") ? safeSuffix(player) : "";

        String result = text
            .replace("{player}", player.getName().getString())
            .replace("{displayname}", player.getName().getString())
            .replace("{online}", String.valueOf(online))
            .replace("{max}", String.valueOf(max))
            .replace("{ping}", String.valueOf(player.connection.latency()))
            .replace("{world}", world)
            .replace("{balance}", balance)
            .replace("{prefix}", prefix)
            .replace("{suffix}", suffix)
            .replace("{group}", group)
            .replace("{x}", String.valueOf(player.getBlockX()))
            .replace("{y}", String.valueOf(player.getBlockY()))
            .replace("{z}", String.valueOf(player.getBlockZ()))
            .replace("{server_name}", com.zerog.neoessentials.config.ConfigManager.getServerName())
            .replace("{server_motd}", com.zerog.neoessentials.util.motd.MotdManager.getInstance().getEffectiveMotd(server))
            .replace("{time}", new java.text.SimpleDateFormat("HH:mm").format(new java.util.Date()));

        result = AnimationManager.getInstance().resolveAnimations(result);

        try {
            result = PlaceholderAPI.setPlaceholders(player, result);
        } catch (Exception e) {
            NeoLog.debug(LOGGER, LogCategory.GENERAL, "Failed to resolve remaining placeholders for {} scoreboard line", player.getName().getString(), e);
        }
        return result;
    }

    private String getPermissionGroup(ServerPlayer player) {
        try {
            String group = com.zerog.neoessentials.api.permissions.PermissionAPI.getPrimaryGroup(player.getUUID());
            if (group != null) return group;
        } catch (Exception e) {
            NeoLog.debug(LOGGER, LogCategory.GENERAL, "Failed to resolve permission group for {}, defaulting to 'default'", player.getName().getString(), e);
        }
        return "default";
    }

    private String safePrefix(ServerPlayer player) {
        try {
            String p = com.zerog.neoessentials.api.permissions.PermissionAPI.getPrefix(player.getUUID());
            return p != null ? p : "";
        } catch (Exception e) { return ""; }
    }

    private String safeSuffix(ServerPlayer player) {
        try {
            String s = com.zerog.neoessentials.api.permissions.PermissionAPI.getSuffix(player.getUUID());
            return s != null ? s : "";
        } catch (Exception e) { return ""; }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    public void onPlayerJoin(ServerPlayer player, MinecraftServer server) {
        joinTick.put(player.getUUID(), globalTick);
    }

    public void onPlayerQuit(ServerPlayer player) {
        UUID uuid = player.getUUID();
        joinTick.remove(uuid);
        active.remove(uuid);
        lastTitle.remove(uuid);
        lastLines.remove(uuid);
    }

    /** Force-hide every currently-shown viewer's board (used by {@code /scoreboard disable}). */
    public void hideAll(MinecraftServer server) {
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) hidePlayer(player);
    }

    // ── Public API ────────────────────────────────────────────────────────────
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public int getRefreshIntervalTicks() { return refreshIntervalTicks; }
    public int getBoardCount() { return boards.size(); }

    public List<String> getBoardNames() {
        List<String> names = new ArrayList<>();
        for (ScoreboardBoard b : boards) names.add(b.getName());
        return names;
    }

    public ScoreboardBoard findBoard(String name) {
        for (ScoreboardBoard b : boards) if (b.getName().equalsIgnoreCase(name)) return b;
        return null;
    }

    public void setGroupTitleOverride(String group, String text) {
        groupOverrides.computeIfAbsent(group, g -> new Override()).title = (text == null || text.isEmpty()) ? null : text;
    }

    public void setGroupLineOverride(String group, int index, String text) {
        Override ov = groupOverrides.computeIfAbsent(group, g -> new Override());
        if (text == null || text.isEmpty()) ov.lines.remove(index); else ov.lines.put(index, text);
    }

    public void clearGroupOverrides(String group) { groupOverrides.remove(group); }

    public void setPlayerTitleOverride(UUID uuid, String text) {
        playerOverrides.computeIfAbsent(uuid, u -> new Override()).title = (text == null || text.isEmpty()) ? null : text;
    }

    public void setPlayerLineOverride(UUID uuid, int index, String text) {
        Override ov = playerOverrides.computeIfAbsent(uuid, u -> new Override());
        if (text == null || text.isEmpty()) ov.lines.remove(index); else ov.lines.put(index, text);
    }

    public void clearPlayerOverrides(UUID uuid) { playerOverrides.remove(uuid); }

    // ── Board CRUD (webdashboard) ────────────────────────────────────────────
    /** Insert a new board, or replace the existing one with the same name, then re-sort. */
    public void addOrUpdateBoard(ScoreboardBoard board) {
        boards.removeIf(b -> b.getName().equalsIgnoreCase(board.getName()));
        boards.add(board);
        boards.sort((a, c) -> Integer.compare(c.getPriority(), a.getPriority()));
    }

    public boolean removeBoard(String name) {
        return boards.removeIf(b -> b.getName().equalsIgnoreCase(name));
    }

    // ── Config serialization (webdashboard round-trip) ───────────────────────
    /** Serializes current in-memory state back into scoreboard.json's shape. */
    public JsonObject toConfigJson() {
        JsonObject root = new JsonObject();
        root.addProperty("_configVersion", 1);
        JsonObject sb = new JsonObject();
        sb.addProperty("enabled", enabled);
        sb.addProperty("refreshInterval", refreshIntervalTicks);
        sb.addProperty("joinDelayTicks", joinDelayTicks);

        com.google.gson.JsonArray boardsArr = new com.google.gson.JsonArray();
        for (ScoreboardBoard b : boards) boardsArr.add(boardToJson(b));
        sb.add("boards", boardsArr);

        JsonObject groupsObj = new JsonObject();
        groupOverrides.forEach((group, ov) -> groupsObj.add(group, overrideToJson(ov)));
        sb.add("groups", groupsObj);

        JsonObject playersObj = new JsonObject();
        playerOverrides.forEach((uuid, ov) -> playersObj.add(uuid.toString(), overrideToJson(ov)));
        sb.add("players", playersObj);

        root.add("scoreboard", sb);
        return root;
    }

    private static JsonObject boardToJson(ScoreboardBoard b) {
        JsonObject o = new JsonObject();
        o.addProperty("name", b.getName());
        o.addProperty("priority", b.getPriority());
        com.google.gson.JsonArray conditions = new com.google.gson.JsonArray();
        for (String c : b.getConditions()) conditions.add(c);
        o.add("conditions", conditions);
        o.add("title", framesToJson(b.getTitleFrames()));
        com.google.gson.JsonArray lines = new com.google.gson.JsonArray();
        for (ScoreboardLine line : b.getLines()) {
            JsonObject lo = new JsonObject();
            lo.add("text", framesToJson(line.getFrames()));
            if (line.getCondition() != null) lo.addProperty("condition", line.getCondition());
            lines.add(lo);
        }
        o.add("lines", lines);
        return o;
    }

    private static com.google.gson.JsonElement framesToJson(List<String> frames) {
        if (frames.size() == 1) return new com.google.gson.JsonPrimitive(frames.get(0));
        com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
        for (String f : frames) arr.add(f);
        return arr;
    }

    private static JsonObject overrideToJson(Override ov) {
        JsonObject o = new JsonObject();
        if (ov.title != null) o.addProperty("titleOverride", ov.title);
        JsonObject lines = new JsonObject();
        ov.lines.forEach((idx, text) -> lines.addProperty(String.valueOf(idx), text));
        o.add("lineOverrides", lines);
        return o;
    }

    /** Persists the current in-memory state to scoreboard.json (webdashboard writes). */
    public void saveConfig() {
        ConfigManager.getInstance().saveConfig(ConfigManager.SCOREBOARD_CONFIG, toConfigJson());
    }
}
