package com.zerog.neoessentials.webdashboard.endpoints;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.zerog.neoessentials.chat.MuteManager;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import com.zerog.neoessentials.moderation.*;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Public, no-login player-moderation lookup — the same "anyone can look up a punishment"
 * transparency page ban-management plugins ship, gated only by
 * {@code webDashboard.securitySettings.publicModerationLookupEnabled} (default on), not by
 * dashboard authentication. Registered WITHOUT the auth wrapper in {@link
 * com.zerog.neoessentials.webdashboard.DashboardAPI#registerEndpoints()} — still gets CORS
 * and per-IP rate limiting from that same wrapper, just skips the Bearer-token check.
 *
 * <p><b>Deliberately excludes:</b> staff notes (freeform internal commentary) and player
 * reports (contains the reporter's identity) — never exposed here. IP bans/mutes ARE exposed
 * (transparency: "who is currently banned from this server"), but only with the address
 * itself redacted (see {@link #redactIp}) — a full real IP tied to a punishment reason is
 * more than this page's transparency purpose needs, and would otherwise let anyone browsing
 * the site harvest addresses.
 *
 * <pre>
 *  GET /api/public/moderation/lookup/{name}   – bans/mutes/kicks/warns for one player, by name
 *  GET /api/public/moderation/recent          – recent active bans + mutes across all players
 *  GET /api/public/moderation/ip-bans         – active IP bans, address redacted
 *  GET /api/public/moderation/ip-mutes        – active IP mutes, address redacted
 * </pre>
 */
public class PublicModerationEndpoint implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(PublicModerationEndpoint.class);
    private static final int RECENT_LIMIT = 30;

    private final MinecraftServer server;

    public PublicModerationEndpoint(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if (!ConfigManager.getInstance().isPublicModerationLookupEnabled()) {
            sendJson(exchange, 404, json(false, "Public moderation lookup is disabled on this server"));
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "PublicModerationEndpoint request: {} {}", method, path);

        try {
            if ("GET".equals(method) && path.contains("/lookup/")) {
                handleLookup(exchange, lastSegment(path));
            } else if ("GET".equals(method) && path.endsWith("/recent")) {
                handleRecent(exchange);
            } else if ("GET".equals(method) && path.endsWith("/ip-bans")) {
                handleIpBans(exchange);
            } else if ("GET".equals(method) && path.endsWith("/ip-mutes")) {
                handleIpMutes(exchange);
            } else {
                sendJson(exchange, 404, json(false, "Unknown public moderation endpoint"));
            }
        } catch (Exception e) {
            LOGGER.error("Error in PublicModerationEndpoint", e);
            sendJson(exchange, 500, json(false, "Internal error: " + e.getMessage()));
        }
    }

    // ── Lookup ──────────────────────────────────────────────────────────────────

    private void handleLookup(HttpExchange exchange, String encodedName) throws IOException {
        String playerName = URLDecoder.decode(encodedName, StandardCharsets.UTF_8);
        if (playerName.isBlank()) {
            sendJson(exchange, 400, json(false, "Player name required"));
            return;
        }

        UUID playerId = resolvePlayerId(playerName);

        JsonArray bans = new JsonArray();
        if (playerId != null) {
            for (BanManager.BanEntry b : BanManager.getInstance().getBanHistory(playerId)) {
                bans.add(banJson(b));
            }
        }

        JsonArray mutes = new JsonArray();
        for (MuteManager.MuteEntry m : MuteManager.getMuteHistory(playerName)) {
            mutes.add(muteJson(m));
        }

        JsonArray kicks = new JsonArray();
        for (KickManager.KickEntry k : KickManager.getInstance().getKickHistory(playerName)) {
            kicks.add(kickJson(k));
        }

        JsonArray warns = new JsonArray();
        UUID warnUuid = playerId != null ? playerId : WarnManager.getInstance().findUUIDByName(playerName);
        if (warnUuid != null) {
            for (WarnEntry w : WarnManager.getInstance().getWarnings(warnUuid)) {
                warns.add(warnJson(w));
            }
        }

        JsonObject resp = new JsonObject();
        resp.addProperty("success", true);
        resp.addProperty("playerName", playerName);
        resp.addProperty("playerId", playerId != null ? playerId.toString() : null);
        resp.add("bans", bans);
        resp.add("mutes", mutes);
        resp.add("kicks", kicks);
        resp.add("warns", warns);
        sendJson(exchange, 200, resp.toString());
    }

    // ── Recent activity feed ──────────────────────────────────────────────────────

    private void handleRecent(HttpExchange exchange) throws IOException {
        List<JsonObject> combined = new ArrayList<>();

        for (BanManager.BanEntry b : BanManager.getInstance().getAllPlayerBans()) {
            JsonObject entry = banJson(b);
            entry.addProperty("type", "ban");
            entry.addProperty("sortTime", b.banTime);
            combined.add(entry);
        }
        for (String name : MuteManager.getMutedPlayers()) {
            MuteManager.MuteEntry m = MuteManager.getMuteEntry(name);
            if (m == null) continue;
            JsonObject entry = muteJson(m);
            entry.addProperty("type", "mute");
            entry.addProperty("sortTime", m.muteTime);
            combined.add(entry);
        }

        combined.sort(Comparator.comparingLong((JsonObject o) -> o.get("sortTime").getAsLong()).reversed());

        JsonArray arr = new JsonArray();
        for (int i = 0; i < Math.min(combined.size(), RECENT_LIMIT); i++) {
            JsonObject entry = combined.get(i);
            entry.remove("sortTime");
            arr.add(entry);
        }

        JsonObject resp = new JsonObject();
        resp.addProperty("success", true);
        resp.addProperty("count", arr.size());
        resp.add("recent", arr);
        sendJson(exchange, 200, resp.toString());
    }

    // ── IP bans/mutes (address redacted — see class doc comment) ──────────────────

    private void handleIpBans(HttpExchange exchange) throws IOException {
        JsonArray arr = new JsonArray();
        for (BanManager.IPBanEntry b : BanManager.getInstance().getAllIPBans()) {
            arr.add(publicIpBanJson(b));
        }
        JsonObject resp = new JsonObject();
        resp.addProperty("success", true);
        resp.addProperty("count", arr.size());
        resp.add("ipBans", arr);
        sendJson(exchange, 200, resp.toString());
    }

    private void handleIpMutes(HttpExchange exchange) throws IOException {
        JsonArray arr = new JsonArray();
        for (MuteManager.MuteEntry m : MuteManager.getAllIPMutes()) {
            arr.add(publicIpMuteJson(m));
        }
        JsonObject resp = new JsonObject();
        resp.addProperty("success", true);
        resp.addProperty("count", arr.size());
        resp.add("ipMutes", arr);
        sendJson(exchange, 200, resp.toString());
    }

    private JsonObject publicIpBanJson(BanManager.IPBanEntry b) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", b.id);
        obj.addProperty("ipAddress", redactIp(b.ipAddress));
        obj.addProperty("reason", b.reason);
        obj.addProperty("bannedBy", b.bannedBy);
        obj.addProperty("banTime", b.banTime);
        obj.addProperty("expireTime", b.expireTime);
        obj.addProperty("permanent", b.expireTime == 0);
        obj.addProperty("active", b.active);
        return obj;
    }

    private JsonObject publicIpMuteJson(MuteManager.MuteEntry m) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", m.id);
        obj.addProperty("target", redactIp(m.target));
        obj.addProperty("reason", m.reason);
        obj.addProperty("mutedBy", m.mutedBy);
        obj.addProperty("muteTime", m.muteTime);
        obj.addProperty("expireTime", m.expireTime);
        obj.addProperty("permanent", m.expireTime == 0);
        obj.addProperty("active", m.active);
        return obj;
    }

    /**
     * Masks the low-order part of an IP address for public display — enough left over to
     * show "this is roughly where the ban is from" without handing out an address someone
     * could act on. IPv4: drop the last octet ({@code 203.0.113.42 -> 203.0.113.***}). IPv6:
     * drop the last two groups the same way. Anything that isn't a recognizable IP (shouldn't
     * happen — these come straight from BanManager/MuteManager's own IP-entry stores) is
     * returned as-is rather than guessing at a redaction that might not actually hide anything.
     */
    private String redactIp(String ip) {
        if (ip == null || ip.isBlank()) return ip;
        if (ip.contains(":")) {
            String[] parts = ip.split(":");
            if (parts.length <= 2) return ip;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < parts.length - 2; i++) sb.append(parts[i]).append(':');
            sb.append("****");
            return sb.toString();
        }
        int lastDot = ip.lastIndexOf('.');
        if (lastDot < 0) return ip;
        return ip.substring(0, lastDot) + ".***";
    }

    // ── JSON shapes (mirrors ModerationEndpoint's, minus evidence/internal fields) ──

    private JsonObject banJson(BanManager.BanEntry b) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", b.id);
        obj.addProperty("playerName", b.playerName);
        obj.addProperty("playerId", b.playerId.toString());
        obj.addProperty("reason", b.reason);
        obj.addProperty("bannedBy", b.bannedBy);
        obj.addProperty("banTime", b.banTime);
        obj.addProperty("expireTime", b.expireTime);
        obj.addProperty("permanent", b.expireTime == 0);
        obj.addProperty("active", b.active);
        obj.addProperty("unbannedBy", b.unbannedBy);
        obj.addProperty("unbannedAt", b.unbannedAt);
        return obj;
    }

    private JsonObject muteJson(MuteManager.MuteEntry m) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", m.id);
        obj.addProperty("target", m.target);
        obj.addProperty("reason", m.reason);
        obj.addProperty("mutedBy", m.mutedBy);
        obj.addProperty("muteTime", m.muteTime);
        obj.addProperty("expireTime", m.expireTime);
        obj.addProperty("permanent", m.expireTime == 0);
        obj.addProperty("active", m.active);
        obj.addProperty("unmutedBy", m.unmutedBy);
        obj.addProperty("unmutedAt", m.unmutedAt);
        return obj;
    }

    private JsonObject kickJson(KickManager.KickEntry k) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", k.id);
        obj.addProperty("playerName", k.playerName);
        obj.addProperty("reason", k.reason);
        obj.addProperty("kickedBy", k.kickedBy);
        obj.addProperty("kickTime", k.kickTime);
        return obj;
    }

    private JsonObject warnJson(WarnEntry w) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", w.getId());
        obj.addProperty("targetName", w.getTargetName());
        obj.addProperty("warnedBy", w.getWarnedBy());
        obj.addProperty("reason", w.getReason());
        obj.addProperty("timestamp", w.getTimestamp());
        return obj;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────

    /** Resolves a player's UUID from the online player list, falling back to the profile cache. */
    private UUID resolvePlayerId(String playerName) {
        if (server == null || playerName == null || playerName.isBlank()) return null;
        var online = server.getPlayerList().getPlayerByName(playerName);
        if (online != null) return online.getUUID();
        try {
            var cache = server.getProfileCache();
            if (cache != null) {
                var profile = cache.get(playerName);
                if (profile.isPresent()) return profile.get().getId();
            }
        } catch (Exception e) {
            NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Failed to resolve player id for '{}'", playerName, e);
        }
        return null;
    }

    private String lastSegment(String path) {
        return path.substring(path.lastIndexOf('/') + 1);
    }

    private void sendJson(HttpExchange exchange, int code, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
    }

    private String json(boolean ok, String msg) {
        JsonObject obj = new JsonObject();
        obj.addProperty("success", ok);
        obj.addProperty(ok ? "message" : "error", msg);
        return obj.toString();
    }
}
