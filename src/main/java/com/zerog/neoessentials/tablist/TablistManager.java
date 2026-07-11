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
 * NeoEssentials Tablist System — BungeeTabListPlus-inspired rewrite.
 *
 * <h2>Feature parity with BungeeTabListPlus</h2>
 * <ul>
 *   <li>Animated header/footer with frame cycling</li>
 *   <li>Extended placeholder set (network, proxy, server, health, XP, session, AFK)</li>
 *   <li>Fake player decorative entries — {@link FakePlayerManager}</li>
 *   <li>Proxy integration — {@link ProxyIntegration} (BungeeCord/Velocity channel)</li>
 *   <li>BTLP-style group-sorted player list — {@link TablistLayout}</li>
 *   <li>PlayersByServer grouping — {@link TablistLayout#isPlayersByServer()}</li>
 *   <li>Independent mode: NeoEssentials owns the tab; no proxy plugin needed</li>
 *   <li>Hex colors, gradients, rainbow, named colors via {@link RichTextFormatter}</li>
 *   <li>Per-group header/footer, prefix/suffix display</li>
 *   <li>Per-player header/footer + custom name override (nick system)</li>
 *   <li>Vanished player hiding for non-staff</li>
 *   <li>AFK indicator in tablist</li>
 * </ul>
 *
 * <h2>Placeholder reference</h2>
 * <pre>
     * Standard : {player} {displayname} {online} {max} {ping} {world} {tps} {time}
     *            {server_name} {x} {y} {z} {balance} {prefix} {suffix} {group}
     * BTLP-style: {network_online} {server_online:NAME} {current_server} {server_label}
     *             {rank_weight} {session_minutes} {session_hours}
     *             {level} {health} {max_health} {afk}
     * Decoration: {newline} {bar}
     * Animations: {animation:NAME}  — replaced with current frame from animations.json
 * </pre>
 *
 * References: TAB [1.7.x-1.21.x], BungeeTabListPlus, Simple TabList
 */
public class TablistManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(TablistManager.class);

    private static final TablistManager INSTANCE = new TablistManager();
    public static TablistManager getInstance() { return INSTANCE; }

    // ── Config ────────────────────────────────────────────────────────────────
    private boolean enabled = true;
    /**
     * Independent mode — when true, NeoEssentials is the sole owner of the tablist.
     * No proxy plugin should be managing this server's tab simultaneously.
     * Proxy integration ({@link ProxyIntegration}) is still used for data only.
     */
    private boolean independentMode = true;
    private int refreshIntervalTicks = 20;
    private final List<String> headerFrames = new ArrayList<>();
    private final List<String> footerFrames = new ArrayList<>();
    private String playerFormat = "&f{prefix}&r{player}{suffix}";
    /**
     * Text segments derived from {@link #playerFormat}, split around its three tokens.
     * Vanilla's scoreboard-team prefix/suffix mechanism always renders as a fixed
     * {@code prefix + <actual player name> + suffix} — there's no way to reorder
     * {player} relative to {prefix}/{suffix}, or to insert text between the player's
     * name and the surrounding prefix/suffix except by putting it INSIDE the prefix
     * (trailing) or suffix (leading) string sent to the client. These four segments
     * are exactly that: whatever text sits before {prefix}, between {prefix} and
     * {player}, between {player} and {suffix}, and after {suffix} in the template —
     * e.g. a plain space in "{prefix} {player} {suffix}" ends up appended to the
     * prefix and prepended to the suffix, which is the only way to actually separate
     * them visually. Recomputed once whenever playerFormat is (re)loaded, not per-tick.
     */
    private String formatLead = "";
    private String formatBetweenPrefixPlayer = "";
    private String formatBetweenPlayerSuffix = "";
    private String formatTrail = "";
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
    // Player session start times (for {session_minutes} / {session_hours})
    private final Map<UUID, Long> sessionStartTimes = new ConcurrentHashMap<>();

    // ── Team-update dirty cache ───────────────────────────────────────────────
    // Avoids sending redundant scoreboard team packets on every tick, which causes
    // the prefix/suffix to flicker when refreshInterval is very low (e.g. 1 tick).
    private final Map<UUID, String> lastTeamName   = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastTeamPrefix = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastTeamSuffix = new ConcurrentHashMap<>();

    private TablistManager() {
        headerFrames.add("<gradient:FFD700-FF8C00>&l{server_name}&r &8| &e{online}&8/&e{max} &7players");
        footerFrames.add("&7TPS: {tps} &8| &7Ping: &a{ping}ms &8| &7{world}");
        parsePlayerFormat();
    }

    // ── Initialisation ────────────────────────────────────────────────────────
    /** Load all tablist config, including proxy, fake-players, and layout sub-sections. */
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
            independentMode      = !tab.has("independentMode")    || tab.get("independentMode").getAsBoolean();
            refreshIntervalTicks = tab.has("refreshInterval")    ? tab.get("refreshInterval").getAsInt() : 20;
            hideVanished         = !tab.has("hideVanished")       || tab.get("hideVanished").getAsBoolean();
            showAfkIndicator     = !tab.has("showAfkIndicator")   || tab.get("showAfkIndicator").getAsBoolean();
            afkSuffix            = tab.has("afkSuffix")           ? tab.get("afkSuffix").getAsString() : " &7[AFK]";
            playerFormat         = tab.has("playerFormat")        ? tab.get("playerFormat").getAsString() : playerFormat;
            parsePlayerFormat();

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

            // Clear dirty cache so all players get a fresh team update after reload
            lastTeamName.clear();
            lastTeamPrefix.clear();
            lastTeamSuffix.clear();

            // Delegate to sub-system configs
            ProxyIntegration.getInstance().loadConfig();
            FakePlayerManager.getInstance().loadConfig();
            TablistLayout.getInstance().loadConfig();
            AnimationManager.getInstance().loadConfig();

            LOGGER.info("TablistManager loaded — {} header frame(s), {} footer frame(s), {} group override(s), " +
                "refresh every {} ticks. independentMode={}, proxyEnabled={}, animations={}.",
                headerFrames.size(), footerFrames.size(), groupHeaderFrames.size(), refreshIntervalTicks,
                independentMode, ProxyIntegration.getInstance().isProxyEnabled(),
                AnimationManager.getInstance().getAnimationCount());
        } catch (Exception e) {
            LOGGER.error("Failed to load tablist config: {}", e.getMessage());
        }
    }

    // ── Tick ──────────────────────────────────────────────────────────────────
    public void onTick(MinecraftServer server) {
        // Tick animations unconditionally, even when the tablist system itself is disabled —
        // {animation:name} tokens are also usable in chat/permission-prefix contexts now
        // (via RichTextFormatter.processTablistText), so frame timing must keep advancing
        // regardless of `tablist.enabled`.
        AnimationManager.getInstance().tick(System.currentTimeMillis());

        if (!enabled) return;
        tickCounter++;
        if (tickCounter < refreshIntervalTicks) return;
        tickCounter = 0;

        // Advance using the LARGEST frame count across the global list AND every
        // per-group/per-player override — not just headerFrames.size(). Otherwise a
        // server with a single (unanimated) default header but a multi-frame per-group
        // header (e.g. VIP) would never advance headerFrame at all, since the old check
        // only looked at the global list's size — the per-group frames would be stuck on
        // index 0 forever even though getHeaderFrame() correctly indexes into them.
        int maxHeaderFrames = maxFrameCount(headerFrames, groupHeaderFrames, playerHeaderFrames);
        int maxFooterFrames = maxFrameCount(footerFrames, groupFooterFrames, playerFooterFrames);
        if (maxHeaderFrames > 1) headerFrame = (headerFrame + 1) % maxHeaderFrames;
        if (maxFooterFrames > 1) footerFrame = (footerFrame + 1) % maxFooterFrames;

        // Tick proxy integration (polls proxy data at its own configured rate)
        ProxyIntegration.getInstance().onTick(server);

        updateAll(server);
    }

    /** Largest frame-list size across the global list and every per-group/per-player override. */
    private static int maxFrameCount(List<String> global, Map<String, List<String>> byGroup, Map<UUID, List<String>> byPlayer) {
        int max = global.size();
        for (List<String> frames : byGroup.values()) max = Math.max(max, frames.size());
        for (List<String> frames : byPlayer.values()) max = Math.max(max, frames.size());
        return max;
    }

    // ── Update ────────────────────────────────────────────────────────────────
    public void updateAll(MinecraftServer server) {
        if (!enabled || server == null) return;
        // Apply BTLP-style group weight sorting teams
        TablistLayout.getInstance().applySortingTeams(server);
        // Recompute the BTLP-style column grid (section headers + fillers) once per cycle —
        // scoreboard teams are global state, so this must not run per-viewer.
        TablistLayout.getInstance().recomputeColumnLayout(server);
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
        } catch (Throwable e) {
            // Catches Errors too (e.g. a version-drifted vanilla API) so one player's
            // failure can't abort the header/footer send for every other player this tick.
            LOGGER.debug("Failed to send tablist packet to {}: {}", player.getName().getString(), e.getMessage());
        }
        try {
            updatePlayerTeam(player, server);
        } catch (Throwable e) {
            LOGGER.debug("Failed to update tablist team for {}: {}", player.getName().getString(), e.getMessage());
        }
        // Inject fake-player decorative entries (BTLP-style fakePlayers)
        if (FakePlayerManager.getInstance().isEnabled()) {
            try {
                FakePlayerManager.getInstance().injectForPlayer(player, server);
            } catch (Throwable e) {
                LOGGER.debug("Failed to inject fake tablist entries for {}: {}", player.getName().getString(), e.getMessage());
            }
        }
        // Inject the BTLP-style column grid's section headers + blank fillers, if enabled
        if (TablistLayout.getInstance().isGroupSections()) {
            try {
                FakePlayerManager.getInstance().injectColumnSlots(player, server);
            } catch (Throwable e) {
                LOGGER.debug("Failed to inject column layout entries for {}: {}", player.getName().getString(), e.getMessage());
            }
        }
    }

    /**
     * Splits {@link #playerFormat} around its {@code {prefix}}/{@code {player}}/
     * {@code {suffix}} tokens into the four literal segments that surround them,
     * for use by {@link #updatePlayerTeam}. See {@link #formatLead} for why this
     * is the only way {@code playerFormat} can actually affect rendering.
     */
    private void parsePlayerFormat() {
        String fmt = playerFormat;
        final String tokPrefix = "{prefix}", tokPlayer = "{player}", tokSuffix = "{suffix}";
        int playerIdx = fmt.indexOf(tokPlayer);
        if (playerIdx < 0) {
            // No {player} token — nothing sane to derive; leave every segment empty
            // rather than guess, matching the previous (template-ignored) behavior.
            formatLead = formatBetweenPrefixPlayer = formatBetweenPlayerSuffix = formatTrail = "";
            return;
        }
        int prefixIdx = fmt.indexOf(tokPrefix);
        int suffixIdx = fmt.indexOf(tokSuffix);

        formatLead = (prefixIdx > 0) ? fmt.substring(0, prefixIdx) : "";

        if (prefixIdx >= 0 && prefixIdx + tokPrefix.length() <= playerIdx) {
            formatBetweenPrefixPlayer = fmt.substring(prefixIdx + tokPrefix.length(), playerIdx);
        } else {
            formatBetweenPrefixPlayer = "";
        }

        int afterPlayer = playerIdx + tokPlayer.length();
        if (suffixIdx >= 0 && afterPlayer <= suffixIdx) {
            formatBetweenPlayerSuffix = fmt.substring(afterPlayer, suffixIdx);
            formatTrail = fmt.substring(suffixIdx + tokSuffix.length());
        } else {
            formatBetweenPlayerSuffix = (afterPlayer <= fmt.length()) ? fmt.substring(afterPlayer) : "";
            formatTrail = "";
        }
    }

    // ── Scoreboard Team Prefix (player name row) ──────────────────────────────
    public void updatePlayerTeam(ServerPlayer player, MinecraftServer server) {
        if (!enabled || server == null) return;
        try {
            String prefix = getPermissionPrefix(player);
            String suffix = getPermissionSuffix(player);
            String group  = getPermissionGroup(player);

            // Append AFK suffix to the team suffix when AFK
            String effectiveSuffix = suffix;
            if (showAfkIndicator && isAfk(player)) {
                effectiveSuffix = suffix + afkSuffix;
            }

            // BTLP-style: encode group weight (or, with groupSections on, the exact column-grid
            // slot) into the team name for client-side sort order.
            String rawTeamName;
            String columnKey = TablistLayout.getInstance().getColumnTeamKey(player.getUUID());
            if (columnKey != null) {
                rawTeamName = columnKey;
            } else if (TablistLayout.getInstance().isSortByGroupWeight()) {
                int weight = getGroupWeight(player);
                int sortKey = 9999 - Math.min(weight, 9999);
                rawTeamName = String.format("ne_%04d_%s", sortKey, group);
            } else {
                rawTeamName = "ne_" + group;
            }
            String teamName = rawTeamName.length() > 16 ? rawTeamName.substring(0, 16) : rawTeamName;

            UUID uuid = player.getUUID();

            // ── Dirty check: skip all scoreboard packets if nothing changed ────────
            // This is the core fix for prefix flickering at low refreshInterval values.
            // setPlayerPrefix/setPlayerSuffix and addPlayerToTeam all broadcast packets
            // to every connected client; doing that 20×/sec causes visible flicker.
            String cachedTeam   = lastTeamName.get(uuid);
            String cachedPrefix = lastTeamPrefix.get(uuid);
            String cachedSuffix = lastTeamSuffix.get(uuid);

            boolean teamChanged   = !teamName.equals(cachedTeam);
            boolean prefixChanged = !prefix.equals(cachedPrefix);
            boolean suffixChanged = !effectiveSuffix.equals(cachedSuffix);

            if (!teamChanged && !prefixChanged && !suffixChanged) {
                return; // Nothing to update — no packet needed
            }

            // Update the cache with new values
            lastTeamName.put(uuid, teamName);
            lastTeamPrefix.put(uuid, prefix);
            lastTeamSuffix.put(uuid, effectiveSuffix);

            ServerScoreboard scoreboard = server.getScoreboard();

            // If the player moved to a different team, remove from the old one first
            if (teamChanged) {
                PlayerTeam current = scoreboard.getPlayersTeam(player.getName().getString());
                if (current != null && current.getName().startsWith("ne_") && !current.getName().equals(teamName)) {
                    scoreboard.removePlayerFromTeam(player.getName().getString(), current);
                }
            }

            PlayerTeam team = scoreboard.getPlayerTeam(teamName);
            if (team == null) team = scoreboard.addPlayerTeam(teamName);

            // Only push prefix/suffix packets when they have actually changed.
            // playerFormat's literal text around {prefix}/{player}/{suffix} is folded
            // into the prefix/suffix strings themselves here — see parsePlayerFormat().
            if (prefixChanged || teamChanged) {
                team.setPlayerPrefix(RichTextFormatter.processTablistText(formatLead + prefix + formatBetweenPrefixPlayer));
            }
            if (suffixChanged || teamChanged) {
                team.setPlayerSuffix(RichTextFormatter.processTablistText(formatBetweenPlayerSuffix + effectiveSuffix + formatTrail));
            }

            // Only re-add to team when the team itself changed (avoid redundant add packets)
            if (teamChanged) {
                scoreboard.addPlayerToTeam(player.getName().getString(), team);
            }

        } catch (Throwable e) {
            // Catches Errors too — Scoreboard/PlayerTeam are exactly the kind of vanilla
            // API Mojang has reworked across 1.21.x versions elsewhere in this mod.
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
     * {@code §}.  Color processing is deferred to {@link RichTextFormatter#processTablistText(String)}.
     *
     * <p><strong>BTLP-equivalent placeholders:</strong>
     * <ul>
     *   <li>{@code {network_online}}   — total players on the proxy network</li>
     *   <li>{@code {server_online:X}}  — players on proxy server X</li>
     *   <li>{@code {current_server}}   — proxy server name this player is on</li>
     *   <li>{@code {server_label}}     — this server's configured display label</li>
     *   <li>{@code {rank_weight}}      — numeric group weight</li>
     *   <li>{@code {session_minutes}}  — minutes in current session</li>
     *   <li>{@code {session_hours}}    — hours in current session</li>
     *   <li>{@code {level}}            — XP level</li>
     *   <li>{@code {health}}           — current HP</li>
     *   <li>{@code {max_health}}       — max HP</li>
     *   <li>{@code {afk}}              — AFK label (blank when not AFK)</li>
     * </ul>
     */
    @SuppressWarnings("resource") // ServerLevel is not AutoCloseable; IntelliJ false positive
    private String applyPlaceholders(String text, ServerPlayer player, MinecraftServer server) {
        if (text == null) return "";

        // ── Basic counts ──────────────────────────────────────────────────────
        int online = (int) server.getPlayerList().getPlayers().stream()
            .filter(p -> !isVanishedFromPlayer(p, player))
            .count();
        int max = server.getMaxPlayers();
        int ping = player.connection.latency();
        String world = com.zerog.neoessentials.util.LevelCompat.of(player).dimension().identifier().getPath();
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

        // ── Economy ───────────────────────────────────────────────────────────
        String balance = "0";
        try {
            java.math.BigDecimal bd = com.zerog.neoessentials.economy.managers.EconomyManager.getInstance().getBalance(player.getUUID());
            balance = String.format("%.2f", bd.doubleValue());
        } catch (Exception ignored) {}

        // ── Permission / group ────────────────────────────────────────────────
        String prefix = getPermissionPrefix(player);
        String suffix = getPermissionSuffix(player);
        String group  = getPermissionGroup(player);
        int rankWeight = getGroupWeight(player);

        // Apply per-group colour override to displayname if configured
        String groupColor = groupColors.getOrDefault(group, groupColors.getOrDefault("default", ""));
        String coloredDisplayName = groupColor.isEmpty() ? displayName : groupColor + displayName;

        // ── Proxy / network data (BTLP-style) ─────────────────────────────────
        ProxyIntegration proxy = ProxyIntegration.getInstance();
        int networkOnline = proxy.isProxyEnabled() ? proxy.getNetworkOnline() : online;
        String currentServer = proxy.isProxyEnabled()
            ? proxy.getPlayerServer(player.getUUID()) : proxy.getServerLabel();
        String serverLabel = proxy.getServerLabel();

        // ── Session duration ──────────────────────────────────────────────────
        long sessionMs = System.currentTimeMillis()
            - sessionStartTimes.getOrDefault(player.getUUID(), System.currentTimeMillis());
        long sessionMinutes = sessionMs / 60_000;
        long sessionHours   = sessionMinutes / 60;

        // ── Health / XP ───────────────────────────────────────────────────────
        int level     = player.experienceLevel;
        int health    = (int) player.getHealth();
        int maxHealth = (int) player.getMaxHealth();

        // ── AFK indicator ─────────────────────────────────────────────────────
        String afkStr = (showAfkIndicator && isAfk(player)) ? afkSuffix : "";

        String result = text
            // Standard
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
            .replace("{rank_weight}", String.valueOf(rankWeight))
            // BTLP-style proxy / network
            .replace("{network_online}", String.valueOf(networkOnline))
            .replace("{current_server}", currentServer)
            .replace("{server_label}", serverLabel)
            // Session
            .replace("{session_minutes}", String.valueOf(sessionMinutes % 60))
            .replace("{session_hours}", String.valueOf(sessionHours))
            // Player stats
            .replace("{level}", String.valueOf(level))
            .replace("{health}", String.valueOf(health))
            .replace("{max_health}", String.valueOf(maxHealth))
            // AFK
            .replace("{afk}", afkStr)
            // Decoration
            .replace("{newline}", "\n")
            .replace("{bar}", "&8&m                              &r");

        // Resolve dynamic {server_online:ServerName} tokens
        result = resolveServerOnlinePlaceholders(result, proxy);

        // Resolve {animation:NAME} tokens — expands to the current animation frame
        result = AnimationManager.getInstance().resolveAnimations(result);

        // Finally, pass any remaining {placeholder} tokens through the full PlaceholderAPI
        // so that {neoessentials_*}, {luckperms_*}, {ftbranks_*} and any custom
        // registered expansions are resolved too.
        try {
            result = com.zerog.neoessentials.api.PlaceholderAPI.setPlaceholders(player, result);
        } catch (Exception ignored) {}

        return result;
    }

    /**
     * Resolves {@code {server_online:ServerName}} tokens.
     * Example: {@code "Lobby: {server_online:Lobby}"} → {@code "Lobby: 5"}.
     */
    private static String resolveServerOnlinePlaceholders(String text, ProxyIntegration proxy) {
        if (!text.contains("{server_online:")) return text;
        StringBuilder sb = new StringBuilder(text);
        int start;
        while ((start = sb.indexOf("{server_online:")) >= 0) {
            int end = sb.indexOf("}", start);
            if (end < 0) break;
            String srvName = sb.substring(start + "{server_online:".length(), end);
            sb.replace(start, end + 1, String.valueOf(proxy.getServerOnline(srvName)));
        }
        return sb.toString();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private boolean isVanishedFromPlayer(ServerPlayer target, ServerPlayer viewer) {
        if (!hideVanished) return false;
        boolean targetVanished = com.zerog.neoessentials.moderation.VanishManager.getInstance().isPlayerVanished(target.getUUID());
        if (!targetVanished) return false;
        return !PermissionAPI.hasPermission(viewer.getUUID(), "neoessentials.vanish.see");
    }

    /**
     * Vanish-aware online count from {@code viewer}'s perspective — the same computation
     * {@code {online}} uses in tablist headers. Public so other systems (e.g. chat's
     * short-form placeholder support) can reuse it.
     */
    public int countOnlineExcludingVanish(MinecraftServer server, ServerPlayer viewer) {
        return (int) server.getPlayerList().getPlayers().stream()
            .filter(p -> !isVanishedFromPlayer(p, viewer))
            .count();
    }

    /** Total minutes elapsed in {@code uuid}'s current session (not capped to 0-59; pair with {@link #getSessionHours}). */
    public long getSessionMinutes(UUID uuid) {
        long sessionMs = System.currentTimeMillis()
            - sessionStartTimes.getOrDefault(uuid, System.currentTimeMillis());
        return sessionMs / 60_000;
    }

    /** Full hours elapsed in {@code uuid}'s current session. */
    public long getSessionHours(UUID uuid) {
        return getSessionMinutes(uuid) / 60;
    }

    private boolean isAfk(ServerPlayer player) {
        try {
            return com.zerog.neoessentials.chat.AfkManager.getInstance().isAfk(player.getUUID());
        } catch (Exception ignored) {}
        return false;
    }

    private String getDisplayName(ServerPlayer player) {
        // Priority: NickCommand nickname → internal customNames override → real name
        try {
            String nick = com.zerog.neoessentials.util.commands.NickCommand.getNickname(player.getUUID());
            if (nick != null && !nick.isEmpty()) return nick.replace("&", "§");
        } catch (Exception ignored) {}
        String custom = customNames.get(player.getUUID());
        if (custom != null && !custom.isEmpty()) return custom;
        return player.getName().getString();
    }

    /** Public so other systems (e.g. chat's short-form placeholder support) can reuse it. */
    public double getTps(MinecraftServer server) {
        try {
            double avgMs = server.getAverageTickTimeNanos() / 1_000_000.0;
            return Math.min(20.0, 1000.0 / Math.max(avgMs, 1.0));
        } catch (Throwable e) {
            // Catches Errors too — MinecraftServer's tick-time accessor has had naming/shape
            // changes across versions before (getAverageTickTime() vs getAverageTickTimeNanos()).
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

    /** Public so other systems (e.g. chat's short-form placeholder support) can reuse it. */
    public int getGroupWeight(ServerPlayer player) {
        try {
            com.zerog.neoessentials.permissions.PermissionManager mgr =
                com.zerog.neoessentials.api.permissions.PermissionAPI.getManager();
            if (mgr == null) return 0;
            com.zerog.neoessentials.permissions.PermissionUser user = mgr.getUser(player.getUUID());
            String groupName = (user != null && user.getGroup() != null)
                ? user.getGroup() : mgr.getDefaultGroup();
            com.zerog.neoessentials.permissions.PermissionGroup grp = mgr.getGroup(groupName);
            if (grp != null) {
                try { return grp.getPriority(); } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        return 0;
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
    public boolean isIndependentMode() { return independentMode; }
    public void setIndependentMode(boolean independentMode) { this.independentMode = independentMode; }

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
    @SuppressWarnings("unused")
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
    @SuppressWarnings("unused")
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
    @SuppressWarnings("unused")
    public void setCustomName(UUID uuid, String name) {
        if (name == null || name.isEmpty()) customNames.remove(uuid);
        else customNames.put(uuid, name);
    }

    public void clearCustomName(UUID uuid) { customNames.remove(uuid); }

    @SuppressWarnings("unused")
    public String getAfkSuffix() { return afkSuffix; }
    @SuppressWarnings("unused")
    public boolean isShowAfkIndicator() { return showAfkIndicator; }

    /** Called when a player joins — record session start and send initial tablist update. */
    public void onPlayerJoin(ServerPlayer player, MinecraftServer server) {
        sessionStartTimes.put(player.getUUID(), System.currentTimeMillis());
        ProxyIntegration.getInstance().onPlayerJoin(player, server);
        server.execute(() -> {
            updatePlayer(player, server);
            updatePlayerTeam(player, server);
        });
    }

    /** Called when a player leaves — clean up and update all remaining players. */
    public void onPlayerQuit(ServerPlayer player, MinecraftServer server) {
        UUID uuid = player.getUUID();
        sessionStartTimes.remove(uuid);
        // Clear dirty-check cache so the next login starts fresh
        lastTeamName.remove(uuid);
        lastTeamPrefix.remove(uuid);
        lastTeamSuffix.remove(uuid);
        ProxyIntegration.getInstance().onPlayerQuit(uuid);
        FakePlayerManager.getInstance().removeForPlayer(player);
        FakePlayerManager.getInstance().removeColumnSlotsForPlayer(player);
        server.execute(() -> updateAll(server));
    }

    /** @deprecated Use {@link #onPlayerQuit(ServerPlayer, MinecraftServer)} */
    @Deprecated
    public void onPlayerQuit(MinecraftServer server) {
        server.execute(() -> updateAll(server));
    }
}
