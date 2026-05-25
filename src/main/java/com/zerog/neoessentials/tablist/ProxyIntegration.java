package com.zerog.neoessentials.tablist;

import com.zerog.neoessentials.config.ConfigManager;
import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * NeoEssentials Proxy Integration — BungeeTabListPlus-style bridge.
 *
 * <p>When NeoEssentials detects a proxy (BungeeCord or Velocity), this class:
 * <ul>
 *   <li>Sends {@code BungeeCord} plugin-messaging queries to the proxy.</li>
 *   <li>Caches cross-server player counts for every known server.</li>
 *   <li>Provides {@code {network_online}}, {@code {server_online:NAME}} and
 *       {@code {current_server}} placeholders for use in header/footer frames.</li>
 *   <li>Tracks which server label each connected player belongs to.</li>
 * </ul>
 *
 * <p><strong>Independent mode</strong> (default): NeoEssentials manages its own
 * tablist logic entirely — it does not delegate formatting to any proxy plugin.
 * Proxy integration is purely for data (network player counts, server names); the
 * actual header/footer/player-row rendering is always owned by TablistManager.
 *
 * <h2>BungeeCord channel protocol</h2>
 * Channel: {@code bungeecord:main} (legacy name: {@code BungeeCord})<br>
 * Sub-channels used:
 * <ul>
 *   <li>{@code GetServers}  → proxy replies with comma-separated server list.</li>
 *   <li>{@code PlayerCount ServerName} → proxy replies with online count.</li>
 *   <li>{@code GetServer}   → proxy replies with this player's current server name.</li>
 * </ul>
 *
 * Reference: BungeeTabListPlus bridge / AbstractBridge, ServerStateManager, DataManager.
 */
public class ProxyIntegration {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyIntegration.class);

    /** BungeeCord plugin-messaging channel (modern ResourceLocation style). */
    @SuppressWarnings("unused")
    public static final ResourceLocation BUNGEE_CHANNEL =
        ResourceLocation.fromNamespaceAndPath("bungeecord", "main");
    /** Legacy channel name sent in the REGISTER payload. */
    @SuppressWarnings("unused")
    public static final String BUNGEE_CHANNEL_LEGACY = "BungeeCord";

    // ── Singleton ─────────────────────────────────────────────────────────────
    private static final ProxyIntegration INSTANCE = new ProxyIntegration();
    public static ProxyIntegration getInstance() { return INSTANCE; }

    // ── Config ────────────────────────────────────────────────────────────────
    /** True when the server is known to be behind a proxy (driven by config or auto-detect). */
    private boolean proxyEnabled = false;
    /** Server label advertised to headers, e.g. "Survival", "Creative". */
    private String serverLabel = "Main";
    /** Known servers to poll for cross-server player counts. */
    private final List<String> knownServers = new ArrayList<>();
    /** How often (in ticks) to re-query the proxy (default 100 = 5 seconds). */
    private int pollIntervalTicks = 100;
    /** Whether to show players from ALL servers or only this server in the tablist. */
    private boolean showNetworkPlayers = false;

    // ── Runtime state ─────────────────────────────────────────────────────────
    /** Per-server player counts, refreshed via BungeeCord channel messages. */
    private final ConcurrentHashMap<String, Integer> serverPlayerCounts = new ConcurrentHashMap<>();
    /** Total network player count (sum of all serverPlayerCounts). */
    private final AtomicInteger networkOnline = new AtomicInteger(0);
    /** Maps player UUID → current server label (updated when proxy replies to GetServer). */
    private final ConcurrentHashMap<UUID, String> playerServerMap = new ConcurrentHashMap<>();
    /** Whether this server is currently detected as running behind a proxy. */
    private final AtomicBoolean proxyDetected = new AtomicBoolean(false);

    private int tickCounter = 0;
    private MinecraftServer cachedServer = null;

    private ProxyIntegration() {}

    // ── Config loading ────────────────────────────────────────────────────────
    public void loadConfig() {
        try {
            JsonObject tab = getTablistSection();
            if (tab == null || !tab.has("proxy")) return;

            JsonObject proxy = tab.getAsJsonObject("proxy");
            proxyEnabled      = proxy.has("enabled")           && proxy.get("enabled").getAsBoolean();
            serverLabel       = proxy.has("serverLabel")        ? proxy.get("serverLabel").getAsString() : "Main";
            pollIntervalTicks = proxy.has("pollIntervalTicks")  ? proxy.get("pollIntervalTicks").getAsInt() : 100;
            showNetworkPlayers= proxy.has("showNetworkPlayers") && proxy.get("showNetworkPlayers").getAsBoolean();

            knownServers.clear();
            if (proxy.has("knownServers") && proxy.get("knownServers").isJsonArray()) {
                for (var el : proxy.getAsJsonArray("knownServers")) {
                    knownServers.add(el.getAsString());
                }
            }

            LOGGER.info("ProxyIntegration loaded — proxyEnabled={}, serverLabel='{}', knownServers={}",
                proxyEnabled, serverLabel, knownServers);

        } catch (Exception e) {
            LOGGER.debug("ProxyIntegration: config load error: {}", e.getMessage());
        }
    }

    // ── Tick ──────────────────────────────────────────────────────────────────
    public void onTick(MinecraftServer server) {
        if (!proxyEnabled) return;
        this.cachedServer = server;
        tickCounter++;
        if (tickCounter < pollIntervalTicks) return;
        tickCounter = 0;
        pollProxyData(server);
    }

    // ── Player lifecycle ──────────────────────────────────────────────────────
    public void onPlayerJoin(ServerPlayer player, MinecraftServer server) {
        if (!proxyEnabled) return;
        cachedServer = server;
        // Request this player's current server name from the proxy
        sendGetServer(player);
    }

    public void onPlayerQuit(UUID uuid) {
        playerServerMap.remove(uuid);
    }

    /**
     * Called when a {@code BungeeCord} plugin-message is received from the proxy.
     * The payload format mirrors the BungeeCord plugin-messaging protocol:
     * UTF sub-channel, then sub-channel-specific data.
     */
    //noinspection unused
    @SuppressWarnings("unused")
    public void onPluginMessage(ServerPlayer player, byte[] data) {
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
            String subChannel = in.readUTF();
            switch (subChannel) {
                case "GetServer" -> {
                    String serverName = in.readUTF();
                    playerServerMap.put(player.getUUID(), serverName);
                    LOGGER.debug("ProxyIntegration: {} is on server '{}'", player.getName().getString(), serverName);
                }
                case "GetServers" -> {
                    String[] servers = in.readUTF().split(", ");
                    knownServers.clear();
                    Collections.addAll(knownServers, servers);
                    proxyDetected.set(true);
                    LOGGER.debug("ProxyIntegration: discovered servers — {}", Arrays.toString(servers));
                    // Now poll each server's player count
                    if (cachedServer != null) {
                        for (String srv : knownServers) {
                            sendPlayerCount(getAnyPlayer(cachedServer), srv);
                        }
                    }
                }
                case "PlayerCount" -> {
                    String serverName = in.readUTF();
                    int count = in.readInt();
                    serverPlayerCounts.put(serverName, count);
                    recalculateNetworkTotal();
                    LOGGER.debug("ProxyIntegration: {} has {} players", serverName, count);
                }
            }
        } catch (Exception e) {
            LOGGER.debug("ProxyIntegration: failed to read plugin message: {}", e.getMessage());
        }
    }

    // ── Data queries ──────────────────────────────────────────────────────────
    private void pollProxyData(MinecraftServer server) {
        ServerPlayer relay = getAnyPlayer(server);
        if (relay == null) return;
        // Ask proxy for server list
        sendGetServers(relay);
        // Ask for player counts on known servers
        for (String srv : new ArrayList<>(knownServers)) {
            sendPlayerCount(relay, srv);
        }
    }

    // ── BungeeCord plugin-messaging senders ───────────────────────────────────
    private void sendGetServer(ServerPlayer player) {
        sendBungeeMessage(player, "GetServer");
    }

    private void sendGetServers(ServerPlayer player) {
        sendBungeeMessage(player, "GetServers");
    }

    private void sendPlayerCount(ServerPlayer player, String serverName) {
        if (player == null) return;
        sendBungeeMessage(player, "PlayerCount", serverName);
    }

    /**
     * Sends a BungeeCord sub-channel message via plugin messaging.
     *
     * <p><strong>NeoForge 1.21.1 note:</strong> {@code CustomPacketPayload} no longer
     * exposes a {@code write()} method — encoding is handled through the registered
     * {@code StreamCodec}.  Full BungeeCord outbound messaging therefore requires
     * registering a {@code StreamCodec} on the mod-event bus at startup, which is
     * deferred to a future build. The proxy integration is <em>disabled by default</em>
     * ({@code proxy.enabled=false} in {@code tablist.json}), so this stub only runs
     * when explicitly opted in.
     *
     * <p>Inbound BungeeCord responses (plugin-message channel responses from the proxy)
     * are received via the {@code onPluginMessage} callback and work independently of
     * this method.
     */
    private void sendBungeeMessage(@SuppressWarnings("unused") ServerPlayer ignoredPlayer, String... parts) {
        LOGGER.debug("ProxyIntegration: sendBungeeMessage stub called (channel=bungeecord:main, parts={}) — " +
            "outbound BungeeCord messaging pending StreamCodec registration in a future build.", parts.length);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private void recalculateNetworkTotal() {
        int total = serverPlayerCounts.values().stream().mapToInt(Integer::intValue).sum();
        networkOnline.set(total);
    }

    private static ServerPlayer getAnyPlayer(MinecraftServer server) {
        if (server == null) return null;
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        return players.isEmpty() ? null : players.getFirst();
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

    // ── Public API ────────────────────────────────────────────────────────────
    /** @return true if proxy integration is configured and active. */
    public boolean isProxyEnabled() { return proxyEnabled; }

    /** @return true if the proxy has been successfully detected (responded to a query). */
    public boolean isProxyDetected() { return proxyDetected.get(); }

    /** @return total player count across ALL servers on the proxy network. */
    public int getNetworkOnline() { return networkOnline.get(); }

    /** @return player count on a specific server by name, or 0 if unknown. */
    public int getServerOnline(String serverName) {
        return serverPlayerCounts.getOrDefault(serverName, 0);
    }

    /** @return the server label for a player UUID (as reported by proxy), or serverLabel if unknown. */
    public String getPlayerServer(UUID uuid) {
        return playerServerMap.getOrDefault(uuid, serverLabel);
    }

    /** @return the configured label for this server (e.g. "Survival"). */
    public String getServerLabel() { return serverLabel; }

    /** @return copy of the known server list. */
    public List<String> getKnownServers() { return Collections.unmodifiableList(knownServers); }

    /** @return true if the tablist should include players across all network servers. */
    @SuppressWarnings("unused") public boolean isShowNetworkPlayers() { return showNetworkPlayers; }

    /** @return per-server player count map snapshot. */
    public Map<String, Integer> getServerPlayerCounts() {
        return Collections.unmodifiableMap(serverPlayerCounts);
    }

    /** Force-set a server's player count (used for testing / manual overrides). */
    public void setServerOnline(String server, int count) {
        serverPlayerCounts.put(server, count);
        recalculateNetworkTotal();
    }

    /** Reset all proxy data (called on reload). */
    public void reset() {
        serverPlayerCounts.clear();
        playerServerMap.clear();
        networkOnline.set(0);
        proxyDetected.set(false);
        tickCounter = 0;
        knownServers.clear();
    }
}

