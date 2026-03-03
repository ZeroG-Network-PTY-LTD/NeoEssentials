package com.zerog.neoessentials.tablist;

import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.config.ConfigManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Custom Player Tablist system for NeoEssentials.
 *
 * Features:
 * - Animated header/footer with frame cycling
 * - Per-group prefix/suffix display on player rows (via GameProfile display name)
 * - Placeholder support: {player}, {online}, {max}, {ping}, {world}, {tps}, {time}
 * - Configurable refresh interval
 * - Per-player custom name override (e.g. nick system integration)
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
    private String playerFormat = "§f{prefix}§r{player}{suffix}";
    private boolean showPing = true;
    private boolean hideVanished = true;
    private boolean showAfkIndicator = true;
    private String afkSuffix = " §7[AFK]";

    // ── Runtime state ─────────────────────────────────────────────────────────
    private int headerFrame = 0;
    private int footerFrame = 0;
    private int tickCounter = 0;

    // Per-player custom tab name override (used by nick system)
    private final Map<UUID, String> customNames = new ConcurrentHashMap<>();

    private TablistManager() {
        // Default header/footer if nothing configured
        headerFrames.add("§6§l{server_name} §8| §e{online}§8/§e{max} §7players");
        footerFrames.add("§7TPS: §a{tps} §8| §7Ping: §a{ping}ms §8| §7{world}");
    }

    // ── Initialisation ────────────────────────────────────────────────────────
    public void loadConfig() {
        try {
            com.google.gson.JsonObject cfg = ConfigManager.getInstance().getConfig(ConfigManager.MAIN_CONFIG);
            if (!cfg.has("tablist")) {
                LOGGER.info("No tablist section in config — using defaults.");
                return;
            }
            com.google.gson.JsonObject tab = cfg.getAsJsonObject("tablist");

            enabled = tab.has("enabled") && tab.get("enabled").getAsBoolean();
            refreshIntervalTicks = tab.has("refreshInterval") ? tab.get("refreshInterval").getAsInt() : 20;
            showPing = !tab.has("showPing") || tab.get("showPing").getAsBoolean();
            hideVanished = !tab.has("hideVanished") || tab.get("hideVanished").getAsBoolean();
            showAfkIndicator = !tab.has("showAfkIndicator") || tab.get("showAfkIndicator").getAsBoolean();
            afkSuffix = tab.has("afkSuffix") ? tab.get("afkSuffix").getAsString() : " §7[AFK]";
            playerFormat = tab.has("playerFormat") ? tab.get("playerFormat").getAsString() : playerFormat;

            headerFrames.clear();
            if (tab.has("header")) {
                var h = tab.get("header");
                if (h.isJsonArray()) {
                    for (var el : h.getAsJsonArray()) headerFrames.add(el.getAsString());
                } else {
                    headerFrames.add(h.getAsString());
                }
            }

            footerFrames.clear();
            if (tab.has("footer")) {
                var f = tab.get("footer");
                if (f.isJsonArray()) {
                    for (var el : f.getAsJsonArray()) footerFrames.add(el.getAsString());
                } else {
                    footerFrames.add(f.getAsString());
                }
            }

            if (headerFrames.isEmpty()) headerFrames.add("§6§l{server_name}");
            if (footerFrames.isEmpty()) footerFrames.add("§7{online}§8/§7{max} online");

            LOGGER.info("TablistManager loaded — {} header frame(s), {} footer frame(s), refresh every {} ticks.",
                headerFrames.size(), footerFrames.size(), refreshIntervalTicks);
        } catch (Exception e) {
            LOGGER.error("Failed to load tablist config: {}", e.getMessage());
        }
    }

    // ── Tick ──────────────────────────────────────────────────────────────────
    /**
     * Called every server tick from TablistEventHandler.onServerTick().
     * Only does work every refreshIntervalTicks ticks.
     */
    public void onTick(MinecraftServer server) {
        if (!enabled) return;
        tickCounter++;
        if (tickCounter < refreshIntervalTicks) return;
        tickCounter = 0;

        // Advance animation frames
        if (headerFrames.size() > 1) headerFrame = (headerFrame + 1) % headerFrames.size();
        if (footerFrames.size() > 1) footerFrame = (footerFrame + 1) % footerFrames.size();

        updateAll(server);
    }

    // ── Update ────────────────────────────────────────────────────────────────
    /** Send updated header/footer packet to all online players. */
    public void updateAll(MinecraftServer server) {
        if (!enabled || server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            updatePlayer(player, server);
        }
    }

    /** Send header/footer to a single player. */
    public void updatePlayer(ServerPlayer player, MinecraftServer server) {
        if (!enabled) return;
        try {
            String header = buildHeader(player, server);
            String footer = buildFooter(player, server);
            ClientboundTabListPacket packet = new ClientboundTabListPacket(
                Component.literal(header),
                Component.literal(footer)
            );
            player.connection.send(packet);
        } catch (Exception e) {
            LOGGER.debug("Failed to send tablist packet to {}: {}", player.getName().getString(), e.getMessage());
        }
    }

    // ── Build header/footer ───────────────────────────────────────────────────
    private String buildHeader(ServerPlayer player, MinecraftServer server) {
        String frame = headerFrames.get(Math.min(headerFrame, headerFrames.size() - 1));
        return applyPlaceholders(frame, player, server);
    }

    private String buildFooter(ServerPlayer player, MinecraftServer server) {
        String frame = footerFrames.get(Math.min(footerFrame, footerFrames.size() - 1));
        return applyPlaceholders(frame, player, server);
    }

    // ── Placeholders ─────────────────────────────────────────────────────────
    /**
     * Supported placeholders:
     * {player}        — player's name
     * {displayname}   — player's display name (with nick/prefix)
     * {online}        — current online player count (excluding vanished for non-staff)
     * {max}           — max player slots
     * {ping}          — player's ping in ms
     * {world}         — current dimension path (e.g. overworld)
     * {tps}           — server TPS (formatted to 1 dp)
     * {time}          — server real-world time (HH:mm)
     * {server_name}   — server motd / name
     * {x}, {y}, {z}   — player coordinates
     * {balance}       — player balance (from EconomyManager)
     * {prefix}        — permission group prefix
     * {suffix}        — permission group suffix
     * {group}         — permission group name
     * {newline}       — line break
     * {bar}           — decorative separator
     * &<code>         — colour codes converted to §
     */
    private String applyPlaceholders(String text, ServerPlayer player, MinecraftServer server) {
        if (text == null) return "";

        // Convert & colour codes
        text = text.replace("&", "§");

        int online = (int) server.getPlayerList().getPlayers().stream()
            .filter(p -> !isVanishedFromPlayer(p, player))
            .count();
        int max = server.getMaxPlayers();
        int ping = player.latency;
        String world = player.serverLevel().dimension().location().getPath();
        String playerName = player.getName().getString();
        String displayName = getDisplayName(player);

        // TPS
        double tps = getTps(server);
        String tpsStr = tps >= 19.0 ? "§a" + String.format("%.1f", tps)
                      : tps >= 15.0 ? "§e" + String.format("%.1f", tps)
                      : "§c" + String.format("%.1f", tps);

        // Real time
        String time = new java.text.SimpleDateFormat("HH:mm").format(new java.util.Date());

        // Server name from MOTD
        String serverName = server.getMotd();

        // Position
        int x = player.getBlockX(), y = player.getBlockY(), z = player.getBlockZ();

        // Economy balance
        String balance = "0";
        try {
            double bal = com.zerog.neoessentials.economy.EconomyManager.getInstance().getBalance(player.getUUID());
            balance = String.format("%.2f", bal);
        } catch (Exception ignored) {}

        // Permission group prefix/suffix/name
        String prefix = getPermissionPrefix(player);
        String suffix = getPermissionSuffix(player);
        String group = getPermissionGroup(player);

        text = text
            .replace("{player}", playerName)
            .replace("{displayname}", displayName)
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
            .replace("{bar}", "§8§m                              §r");

        return text;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private boolean isVanishedFromPlayer(ServerPlayer target, ServerPlayer viewer) {
        if (!hideVanished) return false;
        boolean targetVanished = com.zerog.neoessentials.moderation.commands.VanishCommand.isVanished(target.getUUID());
        if (!targetVanished) return false;
        // Staff with seevanished perm can still see them
        return !PermissionAPI.hasPermission(viewer.getUUID(), "neoessentials.vanish.see");
    }

    private String getDisplayName(ServerPlayer player) {
        // Check custom name override (nick system)
        String custom = customNames.get(player.getUUID());
        if (custom != null && !custom.isEmpty()) return custom;
        return player.getName().getString();
    }

    private double getTps(MinecraftServer server) {
        try {
            // NeoForge 1.21.1: getAverageTickTimeNanos() → convert to TPS
            double avgMs = server.getAverageTickTimeNanos() / 1_000_000.0;
            return Math.min(20.0, 1000.0 / Math.max(avgMs, 1.0));
        } catch (Exception e) {
            return 20.0;
        }
    }

    private String getPermissionPrefix(ServerPlayer player) {
        try {
            // Access internal permission manager through PermissionAPI
            var pmField = com.zerog.neoessentials.api.permissions.PermissionAPI.class.getDeclaredField("manager");
            pmField.setAccessible(true);
            var mgr = (com.zerog.neoessentials.permissions.PermissionManager) pmField.get(null);
            if (mgr != null) {
                var user = mgr.getUser(player.getUUID());
                if (user != null) {
                    String groupName = user.getGroup();
                    var group = mgr.getGroup(groupName);
                    if (group != null && group.getPrefix() != null && !group.getPrefix().isEmpty()) {
                        return group.getPrefix().replace("&", "§");
                    }
                }
            }
        } catch (Exception ignored) {}
        return "";
    }

    private String getPermissionSuffix(ServerPlayer player) {
        try {
            var pmField = com.zerog.neoessentials.api.permissions.PermissionAPI.class.getDeclaredField("manager");
            pmField.setAccessible(true);
            var mgr = (com.zerog.neoessentials.permissions.PermissionManager) pmField.get(null);
            if (mgr != null) {
                var user = mgr.getUser(player.getUUID());
                if (user != null) {
                    String groupName = user.getGroup();
                    var group = mgr.getGroup(groupName);
                    if (group != null && group.getSuffix() != null && !group.getSuffix().isEmpty()) {
                        return group.getSuffix().replace("&", "§");
                    }
                }
            }
        } catch (Exception ignored) {}
        return "";
    }

    private String getPermissionGroup(ServerPlayer player) {
        try {
            var pmField = com.zerog.neoessentials.api.permissions.PermissionAPI.class.getDeclaredField("manager");
            pmField.setAccessible(true);
            var mgr = (com.zerog.neoessentials.permissions.PermissionManager) pmField.get(null);
            if (mgr != null) {
                var user = mgr.getUser(player.getUUID());
                if (user != null) return user.getGroup();
            }
        } catch (Exception ignored) {}
        return "default";
    }

    // ── Public API ────────────────────────────────────────────────────────────
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    /** Runtime header override (first frame replaced). Cleared on reload. */
    public void setHeaderOverride(String text) {
        headerFrames.clear();
        headerFrames.add(text);
        headerFrame = 0;
    }

    /** Runtime footer override (first frame replaced). Cleared on reload. */
    public void setFooterOverride(String text) {
        footerFrames.clear();
        footerFrames.add(text);
        footerFrame = 0;
    }

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
        // Small delay to ensure the client is fully ready
        server.execute(() -> updatePlayer(player, server));
    }

    /** Called when a player leaves — update all remaining players' online count. */
    public void onPlayerQuit(MinecraftServer server) {
        server.execute(() -> updateAll(server));
    }
}



