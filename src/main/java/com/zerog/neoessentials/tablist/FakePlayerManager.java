package com.zerog.neoessentials.tablist;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.zerog.neoessentials.chat.RichTextFormatter;
import com.zerog.neoessentials.config.ConfigManager;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BungeeTabListPlus-inspired Fake Player Manager for NeoEssentials.
 *
 * <p>Fake players are decorative tab-list entries that do NOT correspond to any
 * real player. They are used for:
 * <ul>
 *   <li>Column/section separators — e.g. a blank spacer row.</li>
 *   <li>Decorative header lines inside the player list area.</li>
 *   <li>Padding to keep a fixed grid size (like BTLP's 80-slot grid).</li>
 *   <li>Randomly-named cosmetic fake players (identical to BTLP's {@code fakePlayers} list).</li>
 * </ul>
 *
 * <h2>How it works</h2>
 * <ol>
 *   <li>Each entry has a <em>stable UUID</em> derived from a deterministic hash of its
 *       slot name, so the same entry always uses the same UUID across reloads.</li>
 *   <li>On each tablist update ({@link TablistManager#updateAll}), fake entries are
 *       injected per-viewer via {@code ClientboundPlayerInfoUpdatePacket}.</li>
 *   <li>When a fake entry is removed or the system is disabled, a
 *       {@code ClientboundPlayerInfoRemovePacket} is sent to all viewers.</li>
 * </ol>
 *
 * <h2>Config (inside tablist.json → tablist)</h2>
 * <pre>{@code
 * "fakePlayers": [
 *   { "name": "§8§m────────────────", "latency": -1 },
 *   { "name": "§e§lOur Network",      "latency": 0  },
 *   { "name": "§8§m────────────────", "latency": -1 }
 * ]
 * }</pre>
 *
 * Reference: BungeeTabListPlus {@code FakePlayer}, {@code FakePlayerManagerImpl},
 * {@code MainConfig#fakePlayers}.
 */
public class FakePlayerManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(FakePlayerManager.class);

    // ── Singleton ──────────────────────────────────────────────────────────────
    private static final FakePlayerManager INSTANCE = new FakePlayerManager();
    public static FakePlayerManager getInstance() { return INSTANCE; }

    // ── Inner data class ───────────────────────────────────────────────────────
    /**
     * A single fake tab-list slot entry.
     * {@code slotId}   – internal identifier (also used to derive UUID).
     * {@code display}  – formatted display text (supports all NeoEssentials color tags).
     * {@code latency}  – ping bar ms.  Use {@code -1} for "disconnected/no bar", {@code 0} for green.
     * {@code listed}   – if true the entry appears in the visible tab list.
     */
    public record FakeEntry(String slotId, String display, int latency, boolean listed) {
        /** Deterministic UUID derived from slotId so it survives reloads. */
        public UUID uuid() {
            return UUID.nameUUIDFromBytes(("NeoEssentials|FakePlayer|" + slotId)
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        /** Short MC-safe profile name (≤ 16 chars). Prefixed with '~' like BTLP. */
        public String profileName() {
            String sanitized = slotId.replaceAll("[^A-Za-z0-9_\\-]", "_");
            String raw = "~NE_" + sanitized;
            return raw.length() > 16 ? raw.substring(0, 16) : raw;
        }
    }

    // ── State ──────────────────────────────────────────────────────────────────
    /** Ordered list of fake entries (order = tab position). */
    private final List<FakeEntry> entries = new ArrayList<>();
    /** Per-player set of UUIDs we have already injected — avoids duplicate ADD packets. */
    private final ConcurrentHashMap<UUID, Set<UUID>> injectedPerPlayer = new ConcurrentHashMap<>();

    private boolean enabled = false;

    private FakePlayerManager() {}

    // ── Config loading ─────────────────────────────────────────────────────────
    public void loadConfig() {
        entries.clear();
        try {
            JsonObject tab = getTablistSection();
            if (tab == null || !tab.has("fakePlayers")) {
                LOGGER.debug("FakePlayerManager: no fakePlayers section in config.");
                enabled = false;
                return;
            }

            var arr = tab.getAsJsonArray("fakePlayers");
            for (int i = 0; i < arr.size(); i++) {
                var el = arr.get(i);
                if (el.isJsonObject()) {
                    JsonObject obj = el.getAsJsonObject();
                    String display = obj.has("name")    ? obj.get("name").getAsString()    : "";
                    int    latency = obj.has("latency") ? obj.get("latency").getAsInt()     : 0;
                    boolean listed = !obj.has("listed") || obj.get("listed").getAsBoolean();
                    String slotId  = obj.has("id")      ? obj.get("id").getAsString()       : "slot_" + i;
                    entries.add(new FakeEntry(slotId, display, latency, listed));
                } else if (el.isJsonPrimitive()) {
                    // Simple string shorthand: just a display name
                    entries.add(new FakeEntry("slot_" + i, el.getAsString(), 0, true));
                }
            }
            enabled = !entries.isEmpty();
            LOGGER.info("FakePlayerManager: loaded {} fake entries.", entries.size());

        } catch (Exception e) {
            LOGGER.error("FakePlayerManager: failed to load config: {}", e.getMessage());
        }
    }

    // ── Per-viewer injection ───────────────────────────────────────────────────
    /**
     * Injects all fake entries into the given player's tab list view.
     * Sends {@code ADD_PLAYER} packets for entries not yet injected;
     * updates display-name for entries already present.
     */
    public void injectForPlayer(ServerPlayer viewer, MinecraftServer server) {
        if (!enabled || entries.isEmpty()) return;

        UUID viewerUUID = viewer.getUUID();
        Set<UUID> alreadyInjected = injectedPerPlayer.computeIfAbsent(viewerUUID,
            k -> Collections.synchronizedSet(new HashSet<>()));

        List<ClientboundPlayerInfoUpdatePacket.Entry> toAdd    = new ArrayList<>();
        List<ClientboundPlayerInfoUpdatePacket.Entry> toUpdate = new ArrayList<>();

        for (FakeEntry fe : entries) {
            UUID uuid = fe.uuid();
            net.minecraft.network.chat.Component display =
                RichTextFormatter.processTablistText(fe.display());

            ClientboundPlayerInfoUpdatePacket.Entry entry = new ClientboundPlayerInfoUpdatePacket.Entry(
                uuid,
                new GameProfile(uuid, fe.profileName()),
                fe.listed(),
                clampLatency(fe.latency()),
                GameType.SURVIVAL,
                display,
                null   // no chat session
            );

            if (alreadyInjected.contains(uuid)) {
                toUpdate.add(entry);
            } else {
                toAdd.add(entry);
                alreadyInjected.add(uuid);
            }
        }

        try {
            if (!toAdd.isEmpty()) {
                var addPacket = new ClientboundPlayerInfoUpdatePacket(
                    EnumSet.of(
                        ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER,
                        ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED,
                        ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY,
                        ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME
                    ),
                    toAdd
                );
                viewer.connection.send(addPacket);
            }
            if (!toUpdate.isEmpty()) {
                var updatePacket = new ClientboundPlayerInfoUpdatePacket(
                    EnumSet.of(
                        ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME,
                        ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY,
                        ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED
                    ),
                    toUpdate
                );
                viewer.connection.send(updatePacket);
            }
        } catch (Exception e) {
            LOGGER.debug("FakePlayerManager: inject error for {}: {}", viewer.getName().getString(), e.getMessage());
        }
    }

    /**
     * Removes all injected fake-player entries from a player's tab list view.
     * Called when a player disconnects or when the system is disabled.
     */
    public void removeForPlayer(ServerPlayer viewer) {
        Set<UUID> injected = injectedPerPlayer.remove(viewer.getUUID());
        if (injected == null || injected.isEmpty()) return;
        try {
            viewer.connection.send(new ClientboundPlayerInfoRemovePacket(new ArrayList<>(injected)));
        } catch (Exception e) {
            LOGGER.debug("FakePlayerManager: remove error for {}: {}", viewer.getName().getString(), e.getMessage());
        }
    }

    /**
     * Removes all injected fake entries from EVERY online player.
     * Called on reload or disable to keep the client state clean.
     */
    public void removeFromAll(MinecraftServer server) {
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            removeForPlayer(player);
        }
        injectedPerPlayer.clear();
    }

    // ── Runtime API ────────────────────────────────────────────────────────────
    /** Add a fake entry at runtime (appended to end). */
    public void addEntry(FakeEntry entry) {
        entries.add(entry);
        enabled = true;
    }

    /** Remove a fake entry by slotId. Returns true if removed. */
    public boolean removeEntry(String slotId) {
        boolean removed = entries.removeIf(e -> e.slotId().equals(slotId));
        if (removed) {
            // Notify all players still alive of the removal
            UUID uuid = new FakeEntry(slotId, "", 0, false).uuid();
            try {
                var removePacket = new ClientboundPlayerInfoRemovePacket(List.of(uuid));
                if (TablistManager.getInstance() != null) {
                    var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
                    if (server != null) {
                        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                            player.connection.send(removePacket);
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
        enabled = !entries.isEmpty();
        return removed;
    }

    public List<FakeEntry> getEntries() { return Collections.unmodifiableList(entries); }
    public boolean isEnabled() { return enabled; }
    public int getCount() { return entries.size(); }

    /** Force a clean refresh: removes all entries then re-injects them for the given server. */
    public void refreshAll(MinecraftServer server) {
        removeFromAll(server);
        if (server != null) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                injectForPlayer(player, server);
            }
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────
    private static int clampLatency(int latency) {
        if (latency < 0)   return -1; // shown as disconnected icon
        return Math.min(latency, 9999);
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
}

