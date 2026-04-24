package com.zerog.neoessentials.tablist;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.chat.RichTextFormatter;
import com.zerog.neoessentials.config.ConfigManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.world.scores.PlayerTeam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Custom Player Tablist system for NeoEssentials.
 *
 * Features:
 * - Animated header/footer with frame cycling
 * - Hex colors    — {@code &#RRGGBB} inline hex color codes
 * - Gradients     — {@code <gradient:RRGGBB-RRGGBB>text</gradient>} (2+ stops)
 * - Rainbow text  — {@code <rainbow>text</rainbow>}
 * - Named colors  — {@code <red>text</red>}, {@code <gold>text</gold>}, …
 * - Format tags   — {@code <bold>}, {@code <italic>}, {@code <underline>}, …
 * - Per-group header/footer, prefix/suffix display
 * - Per-player header/footer + custom name override (nick system)
 * - Placeholder support: {player}, {online}, {max}, {ping}, {world}, {tps}, {time}, …
 * - Configurable refresh interval
 * - Vanished player hiding for non-staff
 * - AFK indicator in tablist
 *
 * References: TAB [1.7.x-1.21.x], BungeeTabListPlus, Simple TabList
 */
public class TablistManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(TablistManager.class);

    private static final TablistManager INSTANCE = new TablistManager();
    public static TablistManager getInstance() { return INSTANCE; }

    // ── Config ────────────────────────────────────────────────────────────────
    private boolean enabled = true;
    private int refreshIntervalTicks = 20; // 1 second
    private List<String> headerFrames = new ArrayList<>();
    private List<String> footerFrames = new ArrayList<>();
    private String playerFormat = "&f{prefix}&r{player}{suffix}";
    private boolean hideVanished = true;
    private boolean showAfkIndicator = true;
    private String afkSuffix = " &7[AFK]";
    /** Per-group colour overrides loaded from tablist.json groupColors section. */
    private final Map<String, String> groupColors = new LinkedHashMap<>();

    // Per-group header/footer frame overrides (group name → frame list)
    private final Map<String, List<String>> groupHeaderFrames = new LinkedHashMap<>();
    private final Map<String, List<String>> groupFooterFrames = new LinkedHashMap<>();

    // ── Runtime state ─────────────────────────────────────────────────────────
    private int headerFrame = 0;
    private int footerFrame = 0;
    private int tickCounter = 0;

    // Per-player custom tab name override (used by nick system)
    private final Map<UUID, String> customNames = new ConcurrentHashMap<>();
    // Per-player header/footer frame overrides (set via command or loaded from config)
    private final Map<UUID, List<String>> playerHeaderFrames = new ConcurrentHashMap<>();
    private final Map<UUID, List<String>> playerFooterFrames = new ConcurrentHashMap<>();

    private TablistManager() {
        headerFrames.add("<gradient:FFD700-FF8C00>&l{server_name}&r &8| &e{online}&8/&e{max} &7players");
        footerFrames.add("&7TPS: {tps} &8| &7Ping: &a{ping}ms &8| &7{world}");
    }

    // ── Initialisation ────────────────────────────────────────────────────────
    public void loadConfig() {
        try {
            JsonObject tab = null;

            // 1) Try standalone tablist.json first
            try {
                JsonObject standalone = ConfigManager.getInstance()
                    .getConfig(ConfigManager.TABLIST_CONFIG);
                if (standalone != null && standalone.has("tablist")) {
                    tab = standalone.getAsJsonObject("tablist");
                    LOGGER.debug("TablistManager: loading from tablist.json");
                }
            } catch (Exception ex) {
                LOGGER.debug("TablistManager: tablist.json not available, trying config.json fallback: {}", ex.getMessage());
            }

            // 2) Legacy fallback: "tablist" key inside config.json
            if (tab == null) {
                JsonObject cfg = ConfigManager.getInstance().getConfig(ConfigManager.MAIN_CONFIG);
                if (cfg != null && cfg.has("tablist")) {
                    tab = cfg.getAsJsonObject("tablist");
                    LOGGER.debug("TablistManager: loading from legacy tablist section in config.json");
                }
            }

            if (tab == null) {
                LOGGER.info("TablistManager: no tablist configuration found — using defaults.");
                return;
            }

            enabled              = !tab.has("enabled")           || tab.get("enabled").getAsBoolean();
            refreshIntervalTicks = tab.has("refreshInterval")    ? tab.get("refreshInterval").getAsInt() : 20;
            hideVanished         = !tab.has("hideVanished")       || tab.get("hideVanished").getAsBoolean();
            showAfkIndicator     = !tab.has("showAfkIndicator")   || tab.get("showAfkIndicator").getAsBoolean();
            afkSuffix            = tab.has("afkSuffix")           ? tab.get("afkSuffix").getAsString() : " &7[AFK]";
            playerFormat         = tab.has("playerFormat")        ? tab.get("playerFormat").getAsString() : playerFormat;

            // Per-group colour overrides
            groupColors.clear();
            if (tab.has("groupColors") && tab.get("groupColors").isJsonObject()) {
                for (var entry : tab.getAsJsonObject("groupColors").entrySet()) {
                    groupColors.put(entry.getKey(), entry.getValue().getAsString());
                }
            }

            // Global header/footer frames
            headerFrames.clear();
            if (tab.has("header")) headerFrames.addAll(loadFrames(tab.get("header")));
            footerFrames.clear();
            if (tab.has("footer")) footerFrames.addAll(loadFrames(tab.get("footer")));

            if (headerFrames.isEmpty()) headerFrames.add("&6&l{server_name}");
            if (footerFrames.isEmpty()) footerFrames.add("&7{online}&8/&7{max} online");

            // Per-group header/footer overrides
            groupHeaderFrames.clear();
            groupFooterFrames.clear();
            if (tab.has("groups") && tab.get("groups").isJsonObject()) {
                for (var entry : tab.getAsJsonObject("groups").entrySet()) {
                    String grp = entry.getKey();
                    if (!entry.getValue().isJsonObject()) continue;
                    JsonObject grpCfg = entry.getValue().getAsJsonObject();
                    if (grpCfg.has("header")) groupHeaderFrames.put(grp, loadFrames(grpCfg.get("header")));
                    if (grpCfg.has("footer")) groupFooterFrames.put(grp, loadFrames(grpCfg.get("footer")));
                }
            }

            // Per-player header/footer overrides (UUIDs as keys)
            playerHeaderFrames.clear();
            playerFooterFrames.clear();
            if (tab.has("players") && tab.get("players").isJsonObject()) {
                for (var entry : tab.getAsJsonObject("players").entrySet()) {
                    try {
                        UUID uuid = UUID.fromString(entry.getKey());
                        if (!entry.getValue().isJsonObject()) continue;
                        JsonObject pCfg = entry.getValue().getAsJsonObject();
                        if (pCfg.has("header")) playerHeaderFrames.put(uuid, loadFrames(pCfg.get("header")));
                        if (pCfg.has("footer")) playerFooterFrames.put(uuid, loadFrames(pCfg.get("footer")));
                    } catch (IllegalArgumentException ignored) {
                        LOGGER.warn("TablistManager: invalid UUID in 'players' section: {}", entry.getKey());
                    }
                }
            }

            // Reset animation counters
            headerFrame = 0;
            footerFrame = 0;
            tickCounter = 0;

            LOGGER.info("TablistManager loaded — {} header frame(s), {} footer frame(s), {} group override(s), refresh every {} ticks.",
                headerFrames.size(), footerFrames.size(), groupHeaderFrames.size(), refreshIntervalTicks);
        } catch (Exception e) {
            LOGGER.error("Failed to load tablist config: {}", e.getMessage());
        }
    }

    // ── Tick ──────────────────────────────────────────────────────────────────
    public void onTick(MinecraftServer server) {
        if (!enabled) return;
        tickCounter++;
        if (tickCounter < refreshIntervalTicks) return;
        tickCounter = 0;

        if (headerFrames.size() > 1) headerFrame = (headerFrame + 1) % headerFrames.size();
        if (footerFrames.size() > 1) footerFrame = (footerFrame + 1) % footerFrames.size();

        updateAll(server);
    }

    // ── Update ────────────────────────────────────────────────────────────────
    public void updateAll(MinecraftServer server) {
        if (!enabled || server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            updatePlayer(player, server);
        }
    }

    public void updatePlayer(ServerPlayer player, MinecraftServer server) {
        if (!enabled) return;
        try {
            // Build raw text (placeholders resolved, & codes NOT yet converted)
            String headerText = buildHeader(player, server);
            String footerText = buildFooter(player, server);
            // Convert to rich Components — RichTextFormatter handles gradients, hex, named colors, &-codes
            Component headerComp = RichTextFormatter.processTablistText(headerText);
            Component footerComp = RichTextFormatter.processTablistText(footerText);
            ClientboundTabListPacket packet = new ClientboundTabListPacket(headerComp, footerComp);
            player.connection.send(packet);
        } catch (Exception e) {
            LOGGER.debug("Failed to send tablist packet to {}: {}", player.getName().getString(), e.getMessage());
        }
        updatePlayerTeam(player, server);
    }

    // ── Scoreboard Team Prefix (player name row) ──────────────────────────────
    public void updatePlayerTeam(ServerPlayer player, MinecraftServer server) {
        if (!enabled || server == null) return;
        try {
            String prefix = getPermissionPrefix(player);
            String suffix = getPermissionSuffix(player);
            String group  = getPermissionGroup(player);

            String rawTeamName = "ne_" + group;
            String teamName = rawTeamName.length() > 16 ? rawTeamName.substring(0, 16) : rawTeamName;

            ServerScoreboard scoreboard = server.getScoreboard();

            PlayerTeam current = scoreboard.getPlayersTeam(player.getName().getString());
            if (current != null && current.getName().startsWith("ne_") && !current.getName().equals(teamName)) {
                scoreboard.removePlayerFromTeam(player.getName().getString(), current);
            }

            PlayerTeam team = scoreboard.getPlayerTeam(teamName);
            if (team == null) team = scoreboard.addPlayerTeam(teamName);

            // Use RichTextFormatter for prefix/suffix so hex/gradients work in team display
            team.setPlayerPrefix(RichTextFormatter.processTablistText(prefix));
            team.setPlayerSuffix(RichTextFormatter.processTablistText(suffix));

            scoreboard.addPlayerToTeam(player.getName().getString(), team);

        } catch (Exception e) {
            LOGGER.debug("Failed to update team for {}: {}", player.getName().getString(), e.getMessage());
        }
    }

    // ── Build header/footer (returns raw text for processTablistText) ─────────
    private String buildHeader(ServerPlayer player, MinecraftServer server) {
        String frame = getHeaderFrame(player);
        return applyPlaceholders(frame, player, server);
    }

    private String buildFooter(ServerPlayer player, MinecraftServer server) {
        String frame = getFooterFrame(player);
        return applyPlaceholders(frame, player, server);
    }

    /**
     * Frame selection priority: per-player override → per-group override → global default.
     */
    private String getHeaderFrame(ServerPlayer player) {
        // 1. Per-player
        List<String> pf = playerHeaderFrames.get(player.getUUID());
        if (pf != null && !pf.isEmpty()) return pf.get(headerFrame % pf.size());
        // 2. Per-group
        String group = getPermissionGroup(player);
        List<String> gf = groupHeaderFrames.get(group);
        if (gf != null && !gf.isEmpty()) return gf.get(headerFrame % gf.size());
        // 3. Global
        return headerFrames.get(Math.min(headerFrame, headerFrames.size() - 1));
    }

    private String getFooterFrame(ServerPlayer player) {
        // 1. Per-player
        List<String> pf = playerFooterFrames.get(player.getUUID());
        if (pf != null && !pf.isEmpty()) return pf.get(footerFrame % pf.size());
        // 2. Per-group
        String group = getPermissionGroup(player);
        List<String> gf = groupFooterFrames.get(group);
        if (gf != null && !gf.isEmpty()) return gf.get(footerFrame % gf.size());
        // 3. Global
        return footerFrames.get(Math.min(footerFrame, footerFrames.size() - 1));
    }

    // ── Placeholders ─────────────────────────────────────────────────────────
    /**
     * Resolves all {placeholder} tokens in the frame text.
     *
     * <p><strong>Note:</strong> This method intentionally does NOT convert {@code &} to
     * {@code §}.  Color processing (legacy {@code &X} codes, {@code &#RRGGBB} hex, gradient
     * tags, etc.) is deferred to {@link RichTextFormatter#processTablistText(String)} which is
     * called on the result in {@link #updatePlayer}.  This ensures that {@code &#RRGGBB} hex
     * codes survive intact and that {@code <gradient:…>} tags inside frame strings are processed
     * correctly.
     *
     * Supported placeholders:
     * {player}        — player's name
     * {displayname}   — player's display name (with nick/prefix)
     * {online}        — current online player count (excluding vanished for non-staff)
     * {max}           — max player slots
     * {ping}          — player's ping in ms
     * {world}         — current dimension path (e.g. overworld)
     * {tps}           — server TPS (formatted to 1 dp, pre-coloured with &a/&e/&c)
     * {time}          — server real-world time (HH:mm)
     * {server_name}   — server motd / name
     * {x}, {y}, {z}  — player coordinates
     * {balance}       — player balance (from EconomyManager)
     * {prefix}        — permission group prefix
     * {suffix}        — permission group suffix
     * {group}         — permission group name
     * {newline}       — line break
     * {bar}           — decorative separator (uses &8 codes left intact for processTablistText)
     */
    private String applyPlaceholders(String text, ServerPlayer player, MinecraftServer server) {
        if (text == null) return "";

        // NOTE: Do NOT do text.replace("&","§") here — color processing is deferred to
        // RichTextFormatter.processTablistText() so that &#RRGGBB and gradient tags work.

        int online = (int) server.getPlayerList().getPlayers().stream()
            .filter(p -> !isVanishedFromPlayer(p, player))
            .count();
        int max = server.getMaxPlayers();
        int ping = player.connection.latency();
        String world = player.serverLevel().dimension().location().getPath();
        String playerName = player.getName().getString();
        String displayName = getDisplayName(player);

        // TPS — use &a / &e / &c codes, processTablistText converts them
        double tps = getTps(server);
        String tpsStr = tps >= 19.0 ? "&a" + String.format("%.1f", tps)
                      : tps >= 15.0 ? "&e" + String.format("%.1f", tps)
                      : "&c" + String.format("%.1f", tps);

        String time = new java.text.SimpleDateFormat("HH:mm").format(new java.util.Date());
        String serverName = server.getMotd();

        int x = player.getBlockX(), y = player.getBlockY(), z = player.getBlockZ();

        String balance = "0";
        try {
            java.math.BigDecimal bd = com.zerog.neoessentials.economy.managers.EconomyManager.getInstance().getBalance(player.getUUID());
            balance = String.format("%.2f", bd.doubleValue());
        } catch (Exception ignored) {}

        String prefix = getPermissionPrefix(player);
        String suffix = getPermissionSuffix(player);
        String group = getPermissionGroup(player);

        // Apply per-group colour override to displayname if configured
        String groupColor = groupColors.getOrDefault(group, groupColors.getOrDefault("default", ""));
        String coloredDisplayName = groupColor.isEmpty() ? displayName : groupColor + displayName;

        return text
            .replace("{player}", playerName)
            .replace("{displayname}", coloredDisplayName)
            .replace("{online}", String.valueOf(online))
            .replace("{max}", String.valueOf(max))
            .replace("{ping}", String.valueOf(ping))
            .replace("{world}", world)
            .replace("{tps}", tpsStr)
            .replace("{time}", time)
            .replace("{server_name}", serverName)
            .replace("{x}", String.valueOf(x))
            .replace("{y}", String.valueOf(y))
            .replace("{z}", String.valueOf(z))
            .replace("{balance}", balance)
            .replace("{prefix}", prefix)
            .replace("{suffix}", suffix)
            .replace("{group}", group)
            .replace("{newline}", "\n")
            .replace("{bar}", "&8&m                              &r");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private boolean isVanishedFromPlayer(ServerPlayer target, ServerPlayer viewer) {
        if (!hideVanished) return false;
        boolean targetVanished = com.zerog.neoessentials.moderation.VanishManager.getInstance().isPlayerVanished(target.getUUID());
        if (!targetVanished) return false;
        return !PermissionAPI.hasPermission(viewer.getUUID(), "neoessentials.vanish.see");
    }

    private String getDisplayName(ServerPlayer player) {
        String custom = customNames.get(player.getUUID());
        if (custom != null && !custom.isEmpty()) return custom;
        return player.getName().getString();
    }

    private double getTps(MinecraftServer server) {
        try {
            double avgMs = server.getAverageTickTimeNanos() / 1_000_000.0;
            return Math.min(20.0, 1000.0 / Math.max(avgMs, 1.0));
        } catch (Exception e) {
            return 20.0;
        }
    }

    private String getPermissionPrefix(ServerPlayer player) {
        try {
            com.zerog.neoessentials.permissions.PermissionManager mgr =
                com.zerog.neoessentials.api.permissions.PermissionAPI.getManager();
            if (mgr == null) return "";
            com.zerog.neoessentials.permissions.PermissionUser user = mgr.getUser(player.getUUID());
            String groupName = (user != null && user.getGroup() != null)
                               ? user.getGroup() : mgr.getDefaultGroup();
            com.zerog.neoessentials.permissions.PermissionGroup grp = mgr.getGroup(groupName);
            if (grp != null && grp.getPrefix() != null && !grp.getPrefix().isEmpty()) {
                return grp.getPrefix(); // Leave & codes intact; processTablistText converts them
            }
        } catch (Exception ignored) {}
        return "";
    }

    private String getPermissionSuffix(ServerPlayer player) {
        try {
            com.zerog.neoessentials.permissions.PermissionManager mgr =
                com.zerog.neoessentials.api.permissions.PermissionAPI.getManager();
            if (mgr == null) return "";
            com.zerog.neoessentials.permissions.PermissionUser user = mgr.getUser(player.getUUID());
            String groupName = (user != null && user.getGroup() != null)
                               ? user.getGroup() : mgr.getDefaultGroup();
            com.zerog.neoessentials.permissions.PermissionGroup grp = mgr.getGroup(groupName);
            if (grp != null && grp.getSuffix() != null && !grp.getSuffix().isEmpty()) {
                return grp.getSuffix(); // Leave & codes intact
            }
        } catch (Exception ignored) {}
        return "";
    }

    private String getPermissionGroup(ServerPlayer player) {
        try {
            com.zerog.neoessentials.permissions.PermissionManager mgr =
                com.zerog.neoessentials.api.permissions.PermissionAPI.getManager();
            if (mgr == null) return "default";
            com.zerog.neoessentials.permissions.PermissionUser user = mgr.getUser(player.getUUID());
            if (user != null && user.getGroup() != null) return user.getGroup();
            return mgr.getDefaultGroup();
        } catch (Exception ignored) {}
        return "default";
    }

    /** Load a header/footer frame list from a JsonElement (array or single string). */
    private static List<String> loadFrames(JsonElement el) {
        List<String> frames = new ArrayList<>();
        if (el.isJsonArray()) {
            for (JsonElement e : el.getAsJsonArray()) frames.add(e.getAsString());
        } else {
            frames.add(el.getAsString());
        }
        return frames;
    }

    // ── Public API ────────────────────────────────────────────────────────────
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isHideVanished() { return hideVanished; }
    public int getRefreshIntervalTicks() { return refreshIntervalTicks; }
    public int getHeaderFrameCount() { return headerFrames.size(); }
    public int getFooterFrameCount() { return footerFrames.size(); }

    /** Runtime global header override (first frame replaced). Cleared on reload. */
    public void setHeaderOverride(String text) {
        headerFrames.clear();
        headerFrames.add(text);
        headerFrame = 0;
    }

    /** Runtime global footer override (first frame replaced). Cleared on reload. */
    public void setFooterOverride(String text) {
        footerFrames.clear();
        footerFrames.add(text);
        footerFrame = 0;
    }

    // ── Per-player overrides ──────────────────────────────────────────────────
    /** Set a per-player header override (runtime). Single frame. */
    public void setPlayerHeaderOverride(UUID uuid, String text) {
        if (text == null || text.isEmpty()) {
            playerHeaderFrames.remove(uuid);
        } else {
            playerHeaderFrames.put(uuid, Collections.singletonList(text));
        }
    }

    /** Set a per-player header override with multiple animated frames (runtime). */
    public void setPlayerHeaderFrames(UUID uuid, List<String> frames) {
        if (frames == null || frames.isEmpty()) playerHeaderFrames.remove(uuid);
        else playerHeaderFrames.put(uuid, new ArrayList<>(frames));
    }

    /** Set a per-player footer override (runtime). Single frame. */
    public void setPlayerFooterOverride(UUID uuid, String text) {
        if (text == null || text.isEmpty()) {
            playerFooterFrames.remove(uuid);
        } else {
            playerFooterFrames.put(uuid, Collections.singletonList(text));
        }
    }

    /** Set a per-player footer override with multiple animated frames (runtime). */
    public void setPlayerFooterFrames(UUID uuid, List<String> frames) {
        if (frames == null || frames.isEmpty()) playerFooterFrames.remove(uuid);
        else playerFooterFrames.put(uuid, new ArrayList<>(frames));
    }

    /** Clear all per-player tablist overrides (header + footer). */
    public void clearPlayerOverrides(UUID uuid) {
        playerHeaderFrames.remove(uuid);
        playerFooterFrames.remove(uuid);
    }

    // ── Per-group overrides ───────────────────────────────────────────────────
    public void setGroupHeaderOverride(String group, String text) {
        if (text == null || text.isEmpty()) groupHeaderFrames.remove(group);
        else groupHeaderFrames.put(group, Collections.singletonList(text));
    }

    public void setGroupFooterOverride(String group, String text) {
        if (text == null || text.isEmpty()) groupFooterFrames.remove(group);
        else groupFooterFrames.put(group, Collections.singletonList(text));
    }

    public void clearGroupOverrides(String group) {
        groupHeaderFrames.remove(group);
        groupFooterFrames.remove(group);
    }

    public Set<String> getGroupsWithOverrides() {
        Set<String> groups = new LinkedHashSet<>();
        groups.addAll(groupHeaderFrames.keySet());
        groups.addAll(groupFooterFrames.keySet());
        return groups;
    }

    // ── Nick system integration ───────────────────────────────────────────────
    /** Set a per-player custom tab display name (used by /nick). */
    public void setCustomName(UUID uuid, String name) {
        if (name == null || name.isEmpty()) customNames.remove(uuid);
        else customNames.put(uuid, name);
    }

    public void clearCustomName(UUID uuid) { customNames.remove(uuid); }

    public String getAfkSuffix() { return afkSuffix; }
    public boolean isShowAfkIndicator() { return showAfkIndicator; }

    /** Called when a player joins — send initial tablist update. */
    public void onPlayerJoin(ServerPlayer player, MinecraftServer server) {
        server.execute(() -> {
            updatePlayer(player, server);
            updatePlayerTeam(player, server);
        });
    }

    /** Called when a player leaves — update all remaining players' online count. */
    public void onPlayerQuit(MinecraftServer server) {
        server.execute(() -> updateAll(server));
    }
}
