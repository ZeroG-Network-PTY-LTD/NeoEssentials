package com.zerog.neoessentials.tablist;

import com.google.gson.JsonObject;
import com.zerog.neoessentials.config.ConfigManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * BungeeTabListPlus-inspired tablist layout and player-sorting manager.
 *
 * <p>In BungeeTabListPlus, the tab overlay shows players in a configurable grid
 * (up to 4 columns × 20 rows = 80 slots). Players are sorted into sections by
 * server or permission group, and each section has its own header/footer labels.
 *
 * <p>This class brings that same concept to NeoEssentials on NeoForge:
 * <ul>
 *   <li><strong>Columns</strong> — 1–4 visual columns; affects client-side display ordering.</li>
 *   <li><strong>Sorting</strong> — players are ordered by group weight (higher weight = top)
 *       then alphabetically within the same weight, matching BTLP's default sort.</li>
 *   <li><strong>Group sections</strong> — optionally bucket players by permission group
 *       with a labelled separator row between buckets.</li>
 *   <li><strong>PlayersByServer</strong> — when proxy mode is active, players can be
 *       further bucketed by the server they are on (mirrors BTLP's
 *       {@code PlayersByServerComponent}).</li>
 *   <li><strong>Exclude servers</strong> — servers whose players should NOT appear in this
 *       server's tablist (BTLP's {@code excludeServers} / {@code hiddenServers}).</li>
 * </ul>
 *
 * <h2>Config (inside tablist.json → tablist → layout)</h2>
 * <pre>{@code
 * "layout": {
 *   "columns": 4,
 *   "sortByGroupWeight": true,
 *   "groupSections": true,
 *   "playersByServer": false,
 *   "excludeServers": [],
 *   "hiddenServers": [],
 *   "maxSlotsPerColumn": 20
 * }
 * }</pre>
 *
 * Reference: BungeeTabListPlus {@code PlayersByServerComponentTemplate},
 * {@code PlayersByServerComponentView}, {@code MainConfig#excludeServers},
 * {@code MainConfig#hiddenServers}, {@code ContextAwareOrdering}.
 */
public class TablistLayout {

    private static final Logger LOGGER = LoggerFactory.getLogger(TablistLayout.class);

    // ── Singleton ──────────────────────────────────────────────────────────────
    private static final TablistLayout INSTANCE = new TablistLayout();
    public static TablistLayout getInstance() { return INSTANCE; }

    // ── Config ─────────────────────────────────────────────────────────────────
    /** Number of visual columns in the tab list (1–4). */
    private int columns = 1;
    /** Sort players by descending permission-group weight before alphabetical. */
    private boolean sortByGroupWeight = true;
    /** Insert separator rows between player groups (requires fakePlayers to be configured). */
    private boolean groupSections = false;
    /** Group players by their proxy server (requires proxy integration). */
    private boolean playersByServer = false;
    /** Maximum slots per column (BTLP default: 20; full 80-slot grid = 4 × 20). */
    private int maxSlotsPerColumn = 20;
    /** Server names whose players are excluded from this tab entirely (BTLP: excludeServers). */
    private final Set<String> excludeServers = new LinkedHashSet<>();
    /** Server names whose players are hidden from the list but the server header may still show. */
    private final Set<String> hiddenServers = new LinkedHashSet<>();

    private TablistLayout() {}

    // ── Config loading ─────────────────────────────────────────────────────────
    public void loadConfig() {
        try {
            JsonObject tab = getTablistSection();
            if (tab == null || !tab.has("layout")) return;

            JsonObject layout = tab.getAsJsonObject("layout");
            columns            = layout.has("columns")           ? Math.max(1, Math.min(4, layout.get("columns").getAsInt()))     : 1;
            sortByGroupWeight  = !layout.has("sortByGroupWeight") || layout.get("sortByGroupWeight").getAsBoolean();
            groupSections      = layout.has("groupSections")      && layout.get("groupSections").getAsBoolean();
            playersByServer    = layout.has("playersByServer")    && layout.get("playersByServer").getAsBoolean();
            maxSlotsPerColumn  = layout.has("maxSlotsPerColumn")  ? Math.max(1, layout.get("maxSlotsPerColumn").getAsInt()) : 20;

            excludeServers.clear();
            if (layout.has("excludeServers") && layout.get("excludeServers").isJsonArray()) {
                for (var el : layout.getAsJsonArray("excludeServers")) excludeServers.add(el.getAsString());
            }
            hiddenServers.clear();
            if (layout.has("hiddenServers") && layout.get("hiddenServers").isJsonArray()) {
                for (var el : layout.getAsJsonArray("hiddenServers")) hiddenServers.add(el.getAsString());
            }

            LOGGER.info("TablistLayout loaded — columns={}, sortByWeight={}, groupSections={}, playersByServer={}",
                columns, sortByGroupWeight, groupSections, playersByServer);

        } catch (Exception e) {
            LOGGER.debug("TablistLayout: config load error: {}", e.getMessage());
        }
    }

    // ── Player ordering ────────────────────────────────────────────────────────
    /**
     * Returns the online players sorted according to the configured sort strategy.
     *
     * <p>Sort order (BTLP-compatible):
     * <ol>
     *   <li>Descending group weight (higher weight = shown first — admins before members).</li>
     *   <li>Ascending alphabetical name within the same weight tier.</li>
     * </ol>
     */
    public List<ServerPlayer> sortedPlayers(MinecraftServer server) {
        List<ServerPlayer> players = new ArrayList<>(server.getPlayerList().getPlayers());

        if (sortByGroupWeight) {
            players.sort(Comparator
                .<ServerPlayer, Integer>comparing(p -> -getGroupWeight(p))
                .thenComparing(p -> p.getName().getString().toLowerCase(Locale.ROOT)));
        } else {
            players.sort(Comparator.comparing(p -> p.getName().getString().toLowerCase(Locale.ROOT)));
        }
        return players;
    }

    /**
     * Groups the sorted player list into sections by permission group.
     * Returns a map of groupName → players (insertion-ordered by group weight descending).
     */
    public LinkedHashMap<String, List<ServerPlayer>> groupedByPermGroup(MinecraftServer server) {
        List<ServerPlayer> sorted = sortedPlayers(server);
        LinkedHashMap<String, List<ServerPlayer>> result = new LinkedHashMap<>();
        for (ServerPlayer player : sorted) {
            String group = TablistManager.getInstance() != null
                ? getGroup(player)
                : "default";
            result.computeIfAbsent(group, k -> new ArrayList<>()).add(player);
        }
        return result;
    }

    /**
     * Groups sorted players by proxy server name.
     * Only meaningful when proxy integration is active.
     */
    public LinkedHashMap<String, List<ServerPlayer>> groupedByServer(MinecraftServer server) {
        List<ServerPlayer> sorted = sortedPlayers(server);
        LinkedHashMap<String, List<ServerPlayer>> result = new LinkedHashMap<>();
        for (ServerPlayer player : sorted) {
            String srv = ProxyIntegration.getInstance().getPlayerServer(player.getUUID());
            result.computeIfAbsent(srv, k -> new ArrayList<>()).add(player);
        }
        return result;
    }

    /**
     * Returns true if the given player should be hidden from viewing based on the
     * configured {@code hiddenServers} set (proxy mode only).
     */
    public boolean isHiddenByServer(ServerPlayer player) {
        if (!playersByServer || hiddenServers.isEmpty()) return false;
        String srv = ProxyIntegration.getInstance().getPlayerServer(player.getUUID());
        return hiddenServers.contains(srv);
    }

    /**
     * Returns true if the given player should be completely excluded (not even shown
     * as a server block header) — mirrors BTLP's {@code excludeServers}.
     */
    public boolean isExcludedServer(String serverName) {
        return excludeServers.contains(serverName);
    }

    /**
     * Apply scoreboard-team ordering to reflect the sorted player list.
     * Teams are named {@code neL_<padded_weight>_<group>} so the client sorts them
     * in the correct visual order without NeoEssentials needing to intercept packets.
     *
     * <p>Weight padding: 10000 minus actual weight ensures lower-indexed teams appear first.
     * This mirrors the BTLP approach of using a prefix sort key.
     */
    public void applySortingTeams(MinecraftServer server) {
        if (!sortByGroupWeight) return;
        net.minecraft.server.ServerScoreboard scoreboard;
        try {
            scoreboard = server.getScoreboard();
        } catch (Throwable e) {
            LOGGER.debug("TablistLayout: applySortingTeams error: {}", e.getMessage());
            return;
        }
        // Re-sort all online players into weight-ordered teams. Each player is wrapped in
        // its own try/catch (catching Errors too, e.g. a version-drifted Scoreboard/PlayerTeam
        // API) so one player's failure can't abort sorting for everyone else this tick.
        for (ServerPlayer player : sortedPlayers(server)) {
            try {
                int weight = getGroupWeight(player);
                String group = getGroup(player);
                // Team name encodes sort key: "neL_" + zero-padded inverse weight + "_" + group (max 16 chars)
                int sortKey = 9999 - Math.min(weight, 9999); // lower key = shown first
                String rawTeamName = String.format("neL_%04d_%s", sortKey, group);
                String teamName = rawTeamName.length() > 16 ? rawTeamName.substring(0, 16) : rawTeamName;

                net.minecraft.world.scores.PlayerTeam team = scoreboard.getPlayerTeam(teamName);
                if (team == null) team = scoreboard.addPlayerTeam(teamName);

                String playerName = player.getName().getString();
                net.minecraft.world.scores.PlayerTeam current = scoreboard.getPlayersTeam(playerName);
                // Only move if not already in the correct team
                if (current == null || !current.getName().equals(teamName)) {
                    if (current != null) scoreboard.removePlayerFromTeam(playerName, current);
                    scoreboard.addPlayerToTeam(playerName, team);
                }
            } catch (Throwable e) {
                LOGGER.debug("TablistLayout: applySortingTeams error for {}: {}",
                    player.getName().getString(), e.getMessage());
            }
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────
    private int getGroupWeight(ServerPlayer player) {
        try {
            var mgr = com.zerog.neoessentials.api.permissions.PermissionAPI.getManager();
            if (mgr == null) return 0;
            var user = mgr.getUser(player.getUUID());
            String groupName = (user != null && user.getGroup() != null) ? user.getGroup() : mgr.getDefaultGroup();
            var grp = mgr.getGroup(groupName);
            if (grp != null) {
                // Use priority (NeoEssentials PermissionGroup uses priority, not weight)
                try { return grp.getPriority(); } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private String getGroup(ServerPlayer player) {
        try {
            var mgr = com.zerog.neoessentials.api.permissions.PermissionAPI.getManager();
            if (mgr == null) return "default";
            var user = mgr.getUser(player.getUUID());
            if (user != null && user.getGroup() != null) return user.getGroup();
            return mgr.getDefaultGroup();
        } catch (Exception ignored) {}
        return "default";
    }

    private static JsonObject getTablistSection() {
        try {
            JsonObject standalone = ConfigManager.getInstance()
                .getConfig(ConfigManager.TABLIST_CONFIG);
            if (standalone != null && standalone.has("tablist")) {
                return standalone.getAsJsonObject("tablist");
            }
        } catch (Exception ignored) {}
        try {
            JsonObject cfg = ConfigManager.getInstance().getConfig(ConfigManager.MAIN_CONFIG);
            if (cfg != null && cfg.has("tablist")) return cfg.getAsJsonObject("tablist");
        } catch (Exception ignored) {}
        return null;
    }

    // ── Public accessors ───────────────────────────────────────────────────────
    public int getColumns() { return columns; }
    public boolean isSortByGroupWeight() { return sortByGroupWeight; }
    public boolean isGroupSections() { return groupSections; }
    public boolean isPlayersByServer() { return playersByServer; }
    public int getMaxSlotsPerColumn() { return maxSlotsPerColumn; }
    public Set<String> getExcludeServers() { return Collections.unmodifiableSet(excludeServers); }
    public Set<String> getHiddenServers() { return Collections.unmodifiableSet(hiddenServers); }
    /** Total available slots (columns × maxSlotsPerColumn). */
    public int getTotalSlots() { return columns * maxSlotsPerColumn; }
}

