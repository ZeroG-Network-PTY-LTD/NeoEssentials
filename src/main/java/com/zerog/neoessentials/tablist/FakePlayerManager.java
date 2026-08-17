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
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;

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
 *   { "name": "§8§m────────────────", "latency": -1 },
 *   // Mirror a real player's current skin — resolved once via the server's
 *   // profile cache + session service, then cached until the next reload.
 *   { "name": "&bNotch", "skinOwner": "Notch" },
 *   // Or supply raw base64 "textures" property data yourself (e.g. from
 *   // mineskin.org or minecraft-heads.com) for a fully custom skin/head.
 *   { "name": "&aCustom", "skinTexture": "<base64 value>", "skinSignature": "<base64 signature>" }
 * ]
 * }</pre>
 *
 * <p>{@code skinTexture}/{@code skinSignature} take priority over {@code skinOwner}
 * when both are present. Skin resolution for {@code skinOwner} happens asynchronously
 * (it may require a network round-trip), so the entry briefly shows the default
 * Steve/Alex skin until resolution completes and the tab list is refreshed.
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
    public record FakeEntry(String slotId, String display, int latency, boolean listed,
                             String skinOwner, String skinTexture, String skinSignature) {
        /** Convenience constructor for entries with no custom skin. */
        public FakeEntry(String slotId, String display, int latency, boolean listed) {
            this(slotId, display, latency, listed, null, null, null);
        }
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
    /** Resolved "textures" property per slotId, populated by {@link #resolveSkinsAsync()}. */
    private final ConcurrentHashMap<String, com.mojang.authlib.properties.Property> resolvedSkins = new ConcurrentHashMap<>();

    private boolean enabled = false;

    private FakePlayerManager() {}

    // ── Config loading ─────────────────────────────────────────────────────────
    public void loadConfig() {
        entries.clear();
        resolvedSkins.clear();
        try {
            JsonObject tab = getTablistSection();
            if (tab == null || !tab.has("fakePlayers")) {
                NeoLog.debug(LOGGER, LogCategory.GENERAL, "FakePlayerManager: no fakePlayers section in config.");
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
                    String skinOwner     = obj.has("skinOwner")     ? obj.get("skinOwner").getAsString()     : null;
                    String skinTexture   = obj.has("skinTexture")   ? obj.get("skinTexture").getAsString()   : null;
                    String skinSignature = obj.has("skinSignature") ? obj.get("skinSignature").getAsString() : null;
                    entries.add(new FakeEntry(slotId, display, latency, listed, skinOwner, skinTexture, skinSignature));
                } else if (el.isJsonPrimitive()) {
                    // Simple string shorthand: just a display name
                    entries.add(new FakeEntry("slot_" + i, el.getAsString(), 0, true));
                }
            }
            enabled = !entries.isEmpty();
            NeoLog.info(LOGGER, LogCategory.GENERAL, "FakePlayerManager: loaded {} fake entries.", entries.size());
            resolveSkinsAsync();

        } catch (Exception e) {
            LOGGER.error("FakePlayerManager: failed to load config: {}", e.getMessage());
        }
    }

    /**
     * Resolves custom skins for entries that specify {@code skinTexture}/{@code skinSignature}
     * (synchronous — no network needed) or {@code skinOwner} (async — looks up the named
     * player's current skin via the server's profile cache + session service). Resolved
     * "textures" properties are cached in {@link #resolvedSkins} and a tab-list refresh is
     * triggered once an async lookup completes.
     */
    private void resolveSkinsAsync() {
        MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        for (FakeEntry fe : new ArrayList<>(entries)) {
            if (fe.skinTexture() != null && !fe.skinTexture().isBlank()) {
                resolvedSkins.put(fe.slotId(), new com.mojang.authlib.properties.Property(
                    "textures", fe.skinTexture(), fe.skinSignature()));
                continue;
            }
            String owner = fe.skinOwner();
            if (owner == null || owner.isBlank() || server == null) continue;
            String slotId = fe.slotId();
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                try {
                    var cache = server.getProfileCache();
                    if (cache == null) return;
                    var cached = cache.get(owner);
                    if (cached.isEmpty()) {
                        LOGGER.warn("FakePlayerManager: could not resolve skinOwner '{}' for slot '{}' (unknown player name)", owner, slotId);
                        return;
                    }
                    var result = server.getSessionService().fetchProfile(cached.get().getId(), false);
                    if (result == null) return;
                    var textures = result.profile().getProperties().get("textures");
                    if (textures.isEmpty()) return;
                    resolvedSkins.put(slotId, textures.iterator().next());
                    refreshAll(server);
                } catch (Exception e) {
                    NeoLog.debug(LOGGER, LogCategory.GENERAL, "FakePlayerManager: failed to resolve skinOwner '{}': {}", owner, e.getMessage());
                }
            });
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

            GameProfile profile = new GameProfile(uuid, fe.profileName());
            com.mojang.authlib.properties.Property skin = resolvedSkins.get(fe.slotId());
            if (skin != null) {
                profile.getProperties().put("textures", skin);
            }

            ClientboundPlayerInfoUpdatePacket.Entry entry = com.zerog.neoessentials.util.TabListEntryCompat.create(
                uuid,
                profile,
                fe.listed(),
                clampLatency(fe.latency()),
                GameType.SURVIVAL,
                display,
                null   // no chat session
            );
            if (entry == null) continue; // construction failed even via the compat fallback — skip this entry

            if (alreadyInjected.contains(uuid)) {
                toUpdate.add(entry);
            } else {
                toAdd.add(entry);
                alreadyInjected.add(uuid);
            }
        }

        try {
            if (!toAdd.isEmpty()) {
                var addActions = EnumSet.of(
                    ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER,
                    ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED,
                    ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY,
                    ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME
                );
                var addPacket = buildFakePacket(addActions, toAdd);
                if (addPacket != null) viewer.connection.send(addPacket);
            }
            if (!toUpdate.isEmpty()) {
                var updateActions = EnumSet.of(
                    ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME,
                    ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY,
                    ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED
                );
                var updatePacket = buildFakePacket(updateActions, toUpdate);
                if (updatePacket != null) viewer.connection.send(updatePacket);
            }
        } catch (Exception e) {
            NeoLog.debug(LOGGER, LogCategory.GENERAL, "FakePlayerManager: inject error for {}: {}", viewer.getName().getString(), e.getMessage());
        }
    }

    /** Per-viewer set of column-grid (section header/filler) UUIDs already injected. */
    private final ConcurrentHashMap<UUID, Set<UUID>> injectedColumnPerPlayer = new ConcurrentHashMap<>();

    /**
     * Injects the BTLP-style column grid's synthetic slots (section headers + blank fillers,
     * computed by {@link TablistLayout#recomputeColumnLayout}) into the given viewer's tab list.
     * Tracked separately from the static config {@code fakePlayers} list since the set of slots
     * changes as players join/leave/change group.
     */
    public void injectColumnSlots(ServerPlayer viewer, MinecraftServer server) {
        List<TablistLayout.ColumnSlot> slots = TablistLayout.getInstance().getSyntheticSlots();
        UUID viewerUUID = viewer.getUUID();
        Set<UUID> alreadyInjected = injectedColumnPerPlayer.computeIfAbsent(viewerUUID,
            k -> Collections.synchronizedSet(new HashSet<>()));

        if (slots.isEmpty()) {
            if (!alreadyInjected.isEmpty()) removeColumnSlotsForPlayer(viewer);
            return;
        }

        // Drop any previously injected slots that no longer exist in the current layout
        // (e.g. the grid shrank after a config reload).
        Set<UUID> currentUuids = new HashSet<>();
        for (var slot : slots) currentUuids.add(slot.uuid());
        List<UUID> stale = new ArrayList<>();
        for (UUID u : alreadyInjected) if (!currentUuids.contains(u)) stale.add(u);
        if (!stale.isEmpty()) {
            try {
                viewer.connection.send(new ClientboundPlayerInfoRemovePacket(stale));
            } catch (Exception ignored) {}
            alreadyInjected.removeAll(stale);
        }

        List<ClientboundPlayerInfoUpdatePacket.Entry> toAdd    = new ArrayList<>();
        List<ClientboundPlayerInfoUpdatePacket.Entry> toUpdate = new ArrayList<>();

        for (var slot : slots) {
            UUID uuid = slot.uuid();
            net.minecraft.network.chat.Component display =
                RichTextFormatter.processTablistText(slot.display());

            ClientboundPlayerInfoUpdatePacket.Entry entry = com.zerog.neoessentials.util.TabListEntryCompat.create(
                uuid,
                new GameProfile(uuid, slot.profileName()),
                true,
                slot.header() ? 0 : -1,
                GameType.SURVIVAL,
                display,
                null
            );
            if (entry == null) continue;

            if (alreadyInjected.contains(uuid)) {
                toUpdate.add(entry);
            } else {
                toAdd.add(entry);
                alreadyInjected.add(uuid);
            }
        }

        try {
            if (!toAdd.isEmpty()) {
                var addActions = EnumSet.of(
                    ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER,
                    ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED,
                    ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY,
                    ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME
                );
                var addPacket = buildFakePacket(addActions, toAdd);
                if (addPacket != null) viewer.connection.send(addPacket);
            }
            if (!toUpdate.isEmpty()) {
                var updateActions = EnumSet.of(
                    ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME
                );
                var updatePacket = buildFakePacket(updateActions, toUpdate);
                if (updatePacket != null) viewer.connection.send(updatePacket);
            }
        } catch (Exception e) {
            NeoLog.debug(LOGGER, LogCategory.GENERAL, "FakePlayerManager: column slot inject error for {}: {}", viewer.getName().getString(), e.getMessage());
        }
    }

    /** Removes all injected column-grid entries from a player's tab list view. */
    public void removeColumnSlotsForPlayer(ServerPlayer viewer) {
        Set<UUID> injected = injectedColumnPerPlayer.remove(viewer.getUUID());
        if (injected == null || injected.isEmpty()) return;
        try {
            viewer.connection.send(new ClientboundPlayerInfoRemovePacket(new ArrayList<>(injected)));
        } catch (Exception e) {
            NeoLog.debug(LOGGER, LogCategory.GENERAL, "FakePlayerManager: column slot remove error for {}: {}", viewer.getName().getString(), e.getMessage());
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
            NeoLog.debug(LOGGER, LogCategory.GENERAL, "FakePlayerManager: remove error for {}: {}", viewer.getName().getString(), e.getMessage());
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
            removeColumnSlotsForPlayer(player);
        }
        injectedPerPlayer.clear();
        injectedColumnPerPlayer.clear();
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
    /**
     * Builds a {@link ClientboundPlayerInfoUpdatePacket} with custom fake {@link ClientboundPlayerInfoUpdatePacket.Entry}
     * objects via reflection, since there is no public constructor for arbitrary entry lists.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ClientboundPlayerInfoUpdatePacket buildFakePacket(
            EnumSet<ClientboundPlayerInfoUpdatePacket.Action> actions,
            List<ClientboundPlayerInfoUpdatePacket.Entry> customEntries) {
        try {
            // Create an empty packet (valid constructor exists for empty collection)
            ClientboundPlayerInfoUpdatePacket packet =
                new ClientboundPlayerInfoUpdatePacket(actions, Collections.emptyList());
            // Reflectively replace the internal entries list field
            for (java.lang.reflect.Field f : ClientboundPlayerInfoUpdatePacket.class.getDeclaredFields()) {
                if (List.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    f.set(packet, List.copyOf(customEntries));
                    return packet;
                }
            }
        } catch (Exception e) {
            NeoLog.debug(LOGGER, LogCategory.GENERAL, "FakePlayerManager: buildFakePacket reflection error: {}", e.getMessage());
        }
        return null;
    }

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

