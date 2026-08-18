package com.zerog.neoessentials.webdashboard.endpoints;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.zerog.neoessentials.chat.MuteManager;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import com.zerog.neoessentials.moderation.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * REST handler for moderation management via the dashboard — the canonical punishment
 * model (bans, mutes, kicks, warns, notes, reports), matching ban-management plugins'
 * feature set. GET routes are readable by any logged-in dashboard account and every
 * mutating route requires admin, EXCEPT reports: filing one (POST /report) is open to
 * any logged-in account (matches the in-game /report command's permission node being
 * granted to every player by default), while every reports GET route plus reviewing one
 * is admin-only (matches /reports and /reviewreport being staff-only in-game).
 *
 * <p>Bans and mutes are now backed directly by {@link BanManager}/{@link MuteManager} —
 * the same stores the in-game {@code /ban}/{@code /mute} commands actually enforce —
 * instead of the old dashboard-only {@link ModerationManager} store, which never blocked
 * anyone from joining. See those classes' Javadoc for the full field set (ban/mute IDs,
 * active/inactive state, per-player history, unban/unmute audit trail, IP support).
 *
 * <pre>
 *  GET    /api/moderation/overview              – counts across every punishment type
 *
 *  GET    /api/moderation/bans/active            – active player bans
 *  GET    /api/moderation/bans                   – active + archived (full history)
 *  GET    /api/moderation/bans/{uuid}             – ban history for one player
 *  POST   /api/moderation/ban                    – {playerName, reason, duration(s, -1=perm)} [ADMIN]
 *  DELETE /api/moderation/ban/{uuid}              – unban a player [ADMIN]
 *  GET    /api/moderation/ipbans/active | ipbans – IP bans, active or active+history
 *  POST   /api/moderation/ipban                  – {ip, reason, duration} [ADMIN]
 *  DELETE /api/moderation/ipban/{ip}              – unban an IP [ADMIN]
 *
 *  GET    /api/moderation/mutes/active            – active mutes
 *  GET    /api/moderation/mutes                   – active + archived (full history)
 *  GET    /api/moderation/mutes/{name}            – mute history for one player
 *  POST   /api/moderation/mute                   – {targetName, reason, duration(s)} [ADMIN]
 *  DELETE /api/moderation/mute/{name}             – unmute a player [ADMIN]
 *  GET    /api/moderation/ipmutes                 – active IP mutes
 *  POST   /api/moderation/ipmute                  – {ip, reason, duration} [ADMIN]
 *  DELETE /api/moderation/ipmute/{ip}             – unmute an IP [ADMIN]
 *
 *  GET    /api/moderation/kicks                   – all recorded kicks
 *  GET    /api/moderation/kicks/{name}            – kick history for one player
 *
 *  GET    /api/moderation/warns                   – all warn entries (all players)
 *  GET    /api/moderation/warns/{name}            – warns for a specific player name
 *  DELETE /api/moderation/warn/{id}               – remove a warn (body: {targetName}) [ADMIN]
 *
 *  GET    /api/moderation/notes/{name}            – staff notes for a player
 *  POST   /api/moderation/note                    – {targetName, text} [ADMIN]
 *  DELETE /api/moderation/note/{id}                – remove a note (body: {targetName}) [ADMIN]
 *
 *  GET    /api/moderation/reports                 – pending report queue [ADMIN]
 *  GET    /api/moderation/reports/all             – every report ever filed [ADMIN]
 *  GET    /api/moderation/reports/player/{name}   – reports filed against one player [ADMIN]
 *  GET    /api/moderation/reports/{id}            – a single report [ADMIN]
 *  POST   /api/moderation/report                  – file one: {targetName, reason} (any authenticated user)
 *  POST   /api/moderation/reports/{id}/review     – {status: REVIEWED|DISMISSED, notes?} [ADMIN]
 * </pre>
 */
public class ModerationEndpoint implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModerationEndpoint.class);
    private final MinecraftServer server;

    public ModerationEndpoint(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin",  "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");

        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        String path   = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "ModerationEndpoint request: {} {}", method, path);

        try {
            if ("GET".equals(method) && path.endsWith("/overview")) {
                handleOverview(exchange);

            } else if ("GET".equals(method) && path.endsWith("/bans/active")) {
                handlePlayerBans(exchange, true);
            } else if ("GET".equals(method) && path.endsWith("/bans")) {
                handlePlayerBans(exchange, false);
            } else if ("GET".equals(method) && path.contains("/bans/")) {
                handlePlayerBanHistory(exchange, lastSegment(path));
            } else if ("POST".equals(method) && path.endsWith("/ban")) {
                requireAdmin(exchange);
                handleCreateBan(exchange);
            } else if ("DELETE".equals(method) && path.contains("/ban/")) {
                requireAdmin(exchange);
                handleRemoveBan(exchange, lastSegment(path));

            } else if ("GET".equals(method) && path.endsWith("/ipbans/active")) {
                handleIPBans(exchange, true);
            } else if ("GET".equals(method) && path.endsWith("/ipbans")) {
                handleIPBans(exchange, false);
            } else if ("POST".equals(method) && path.endsWith("/ipban")) {
                requireAdmin(exchange);
                handleCreateIPBan(exchange);
            } else if ("DELETE".equals(method) && path.contains("/ipban/")) {
                requireAdmin(exchange);
                handleRemoveIPBan(exchange, lastSegment(path));

            } else if ("GET".equals(method) && path.endsWith("/mutes/active")) {
                handleMutes(exchange, true);
            } else if ("GET".equals(method) && path.endsWith("/mutes")) {
                handleMutes(exchange, false);
            } else if ("GET".equals(method) && path.contains("/mutes/")) {
                handleMuteHistory(exchange, lastSegment(path));
            } else if ("POST".equals(method) && path.endsWith("/mute")) {
                requireAdmin(exchange);
                handleCreateMute(exchange);
            } else if ("DELETE".equals(method) && path.contains("/mute/")) {
                requireAdmin(exchange);
                handleRemoveMute(exchange, lastSegment(path));

            } else if ("GET".equals(method) && path.endsWith("/ipmutes")) {
                handleIPMutes(exchange);
            } else if ("POST".equals(method) && path.endsWith("/ipmute")) {
                requireAdmin(exchange);
                handleCreateIPMute(exchange);
            } else if ("DELETE".equals(method) && path.contains("/ipmute/")) {
                requireAdmin(exchange);
                handleRemoveIPMute(exchange, lastSegment(path));

            } else if ("GET".equals(method) && path.contains("/kicks/")) {
                handleKickHistory(exchange, lastSegment(path));
            } else if ("GET".equals(method) && path.endsWith("/kicks")) {
                handleAllKicks(exchange);

            } else if ("GET".equals(method) && path.contains("/warns/")) {
                handleWarnsForPlayer(exchange, lastSegment(path));
            } else if ("GET".equals(method) && path.endsWith("/warns")) {
                handleAllWarns(exchange);
            } else if ("DELETE".equals(method) && path.contains("/warn/")) {
                requireAdmin(exchange);
                handleRemoveWarn(exchange, lastSegment(path));

            } else if ("GET".equals(method) && path.contains("/notes/")) {
                handleNotesForPlayer(exchange, lastSegment(path));
            } else if ("POST".equals(method) && path.endsWith("/note")) {
                requireAdmin(exchange);
                handleCreateNote(exchange);
            } else if ("DELETE".equals(method) && path.contains("/note/")) {
                requireAdmin(exchange);
                handleRemoveNote(exchange, lastSegment(path));

            } else if ("POST".equals(method) && path.endsWith("/report")) {
                // Filing a report is intentionally NOT admin-gated — matches the in-game
                // /report command, whose permission node is granted to every player by
                // default. Only reviewing/dismissing the queue is staff-only (below).
                handleCreateReport(exchange);
            } else if ("POST".equals(method) && path.contains("/reports/") && path.endsWith("/review")) {
                requireAdmin(exchange);
                String id = path.substring(path.indexOf("/reports/") + "/reports/".length(), path.lastIndexOf("/review"));
                handleReviewReport(exchange, id);
            } else if ("GET".equals(method) && path.endsWith("/reports/all")) {
                requireAdmin(exchange);
                handleAllReports(exchange);
            } else if ("GET".equals(method) && path.contains("/reports/player/")) {
                requireAdmin(exchange);
                handleReportsForPlayer(exchange, lastSegment(path));
            } else if ("GET".equals(method) && path.contains("/reports/")) {
                requireAdmin(exchange);
                handleReportById(exchange, lastSegment(path));
            } else if ("GET".equals(method) && path.endsWith("/reports")) {
                requireAdmin(exchange);
                handlePendingReports(exchange);

            } else if ("GET".equals(method) && path.endsWith("/freeze/list")) {
                handleFreezeList(exchange);
            } else if ("GET".equals(method) && path.contains("/freeze/")) {
                handleFreezeStatus(exchange, lastSegment(path));
            } else if ("POST".equals(method) && path.endsWith("/freeze")) {
                requireAdmin(exchange);
                handleCreateFreeze(exchange);
            } else if ("DELETE".equals(method) && path.contains("/freeze/")) {
                requireAdmin(exchange);
                handleRemoveFreeze(exchange, lastSegment(path));

            } else if ("GET".equals(method) && path.endsWith("/vanish/list")) {
                handleVanishList(exchange);
            } else if ("GET".equals(method) && path.contains("/vanish/")) {
                handleVanishStatus(exchange, lastSegment(path));
            } else if ("POST".equals(method) && path.endsWith("/vanish")) {
                requireAdmin(exchange);
                handleCreateVanish(exchange);
            } else if ("DELETE".equals(method) && path.contains("/vanish/")) {
                requireAdmin(exchange);
                handleRemoveVanish(exchange, lastSegment(path));

            } else if ("GET".equals(method) && path.endsWith("/jails")) {
                handleJailLocations(exchange);
            } else if ("GET".equals(method) && path.contains("/jail/")) {
                handleJailStatus(exchange, lastSegment(path));
            } else if ("POST".equals(method) && path.endsWith("/jail")) {
                requireAdmin(exchange);
                handleCreateJail(exchange);
            } else if ("DELETE".equals(method) && path.contains("/jail/")) {
                requireAdmin(exchange);
                handleRemoveJail(exchange, lastSegment(path));

            // Jail LOCATIONS — defining the cell itself (name, dimension, shape, coordinates),
            // as opposed to /jail above which sends a PLAYER to an already-defined one. Lets
            // the dashboard create a jail cell by typed-in coordinates without an admin needing
            // to physically stand there in-game first (still fully supported too, via
            // /setjail and the jail wand — see JailCommand/JailWandHandler).
            } else if ("GET".equals(method) && path.endsWith("/jail-locations")) {
                requireAdmin(exchange);
                handleJailLocationsDetailed(exchange);
            } else if ("POST".equals(method) && path.endsWith("/jail-location")) {
                requireAdmin(exchange);
                handleCreateJailLocation(exchange);
            } else if ("DELETE".equals(method) && path.contains("/jail-location/")) {
                requireAdmin(exchange);
                handleRemoveJailLocation(exchange, lastSegment(path));

            } else {
                sendJson(exchange, 404, "{\"success\":false,\"error\":\"Unknown moderation endpoint\"}");
            }
        } catch (SecurityException ignored) {
            // already sent 403
        } catch (Exception e) {
            LOGGER.error("Error in ModerationEndpoint", e);
            sendJson(exchange, 500, json(false, "Internal error: " + e.getMessage()));
        }
    }

    // ── Overview ────────────────────────────────────────────────────────────────

    private void handleOverview(HttpExchange exchange) throws IOException {
        int activeBans   = BanManager.getInstance().getAllPlayerBans().size();
        int activeIPBans = BanManager.getInstance().getAllIPBans().size();
        int activeMutes  = MuteManager.getMutedPlayers().size();
        int totalKicks   = KickManager.getInstance().getAllKicks().size();
        long totalWarns  = WarnManager.getInstance().getAllWarnings().stream().mapToLong(List::size).sum();
        long totalNotes  = NoteManager.getInstance().getAllNotes().stream().mapToLong(List::size).sum();
        int pendingReports = ReportManager.getInstance().getPendingReports().size();
        int totalReports = ReportManager.getInstance().getAllReports().size();
        int jailedCount = 0;
        try {
            jailedCount = JailManager.getInstance().getAllJailedPlayers().size();
        } catch (Exception e) {
            NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Failed to fetch jailed player count for overview", e);
        }

        JsonObject resp = new JsonObject();
        resp.addProperty("success", true);
        resp.addProperty("activeBans", activeBans);
        resp.addProperty("activeIPBans", activeIPBans);
        resp.addProperty("mutedCount", activeMutes);
        resp.addProperty("totalKicks", totalKicks);
        resp.addProperty("totalWarns", totalWarns);
        resp.addProperty("totalNotes", totalNotes);
        resp.addProperty("pendingReports", pendingReports);
        resp.addProperty("totalReports", totalReports);
        resp.addProperty("jailedCount", jailedCount);
        sendJson(exchange, 200, resp.toString());
    }

    // ── Player bans ────────────────────────────────────────────────────────────

    private void handlePlayerBans(HttpExchange exchange, boolean activeOnly) throws IOException {
        List<BanManager.BanEntry> bans;
        if (activeOnly) {
            bans = BanManager.getInstance().getAllPlayerBans();
        } else {
            bans = new ArrayList<>(BanManager.getInstance().getAllPlayerBans());
            bans.addAll(BanManager.getInstance().getAllBanHistory());
        }
        JsonArray arr = new JsonArray();
        for (BanManager.BanEntry b : bans) arr.add(banJson(b));
        JsonObject resp = new JsonObject();
        resp.addProperty("success", true);
        resp.addProperty("count", bans.size());
        resp.add("bans", arr);
        sendJson(exchange, 200, resp.toString());
    }

    private void handlePlayerBanHistory(HttpExchange exchange, String uuidStr) throws IOException {
        UUID playerId;
        try { playerId = UUID.fromString(uuidStr); }
        catch (Exception e) { sendJson(exchange, 400, json(false, "Invalid UUID: " + uuidStr)); return; }

        List<BanManager.BanEntry> history = BanManager.getInstance().getBanHistory(playerId);
        JsonArray arr = new JsonArray();
        for (BanManager.BanEntry b : history) arr.add(banJson(b));
        JsonObject resp = new JsonObject();
        resp.addProperty("success", true);
        resp.addProperty("count", history.size());
        resp.add("bans", arr);
        sendJson(exchange, 200, resp.toString());
    }

    private void handleCreateBan(HttpExchange exchange) throws Exception {
        JsonObject body = readBody(exchange);
        String playerName = body.has("playerName") ? body.get("playerName").getAsString() : "";
        String reason = body.has("reason") ? body.get("reason").getAsString() : "No reason provided";
        long durationSeconds = body.has("duration") && !body.get("duration").isJsonNull() ? body.get("duration").getAsLong() : -1L;
        String bannedBy = executorName(exchange);

        UUID playerId = resolvePlayerId(playerName);
        if (playerId == null) {
            sendJson(exchange, 404, json(false, "Player not found: " + playerName));
            return;
        }

        boolean success = durationSeconds > 0
            ? BanManager.getInstance().tempBanPlayer(playerName, playerId, reason, bannedBy, durationSeconds * 1000L)
            : BanManager.getInstance().banPlayer(playerName, playerId, reason, bannedBy);

        if (!success) {
            sendJson(exchange, 409, json(false, "Player is already banned, or bans are disabled in config"));
            return;
        }
        NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Ban created for player '{}' by '{}'", playerName, bannedBy);
        sendJson(exchange, 200, "{\"success\":true,\"message\":\"Ban created\",\"playerId\":\"" + esc(playerId.toString()) + "\"}");
    }

    private void handleRemoveBan(HttpExchange exchange, String uuidStr) throws IOException {
        UUID playerId;
        try { playerId = UUID.fromString(uuidStr); }
        catch (Exception e) { sendJson(exchange, 400, json(false, "Invalid UUID: " + uuidStr)); return; }

        boolean removed = BanManager.getInstance().unbanPlayer(playerId, executorName(exchange));
        NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Unban request for playerId={} removed={}", playerId, removed);
        sendJson(exchange, 200, json(removed, removed ? "Ban removed" : "Ban not found: " + uuidStr));
    }

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
        obj.addProperty("evidence", b.evidence);
        obj.addProperty("active", b.active);
        obj.addProperty("unbannedBy", b.unbannedBy);
        obj.addProperty("unbannedAt", b.unbannedAt);
        return obj;
    }

    // ── IP bans ────────────────────────────────────────────────────────────────

    private void handleIPBans(HttpExchange exchange, boolean activeOnly) throws IOException {
        List<BanManager.IPBanEntry> bans;
        if (activeOnly) {
            bans = BanManager.getInstance().getAllIPBans();
        } else {
            bans = new ArrayList<>(BanManager.getInstance().getAllIPBans());
            bans.addAll(BanManager.getInstance().getAllIPBanHistory());
        }
        JsonArray arr = new JsonArray();
        for (BanManager.IPBanEntry b : bans) arr.add(ipBanJson(b));
        JsonObject resp = new JsonObject();
        resp.addProperty("success", true);
        resp.addProperty("count", bans.size());
        resp.add("ipBans", arr);
        sendJson(exchange, 200, resp.toString());
    }

    private void handleCreateIPBan(HttpExchange exchange) throws Exception {
        JsonObject body = readBody(exchange);
        String ip = body.has("ip") ? body.get("ip").getAsString() : "";
        String reason = body.has("reason") ? body.get("reason").getAsString() : "No reason provided";
        long durationSeconds = body.has("duration") && !body.get("duration").isJsonNull() ? body.get("duration").getAsLong() : -1L;
        String bannedBy = executorName(exchange);

        boolean success = durationSeconds > 0
            ? BanManager.getInstance().tempBanIP(ip, reason, bannedBy, durationSeconds * 1000L)
            : BanManager.getInstance().banIP(ip, reason, bannedBy);

        if (!success) {
            sendJson(exchange, 409, json(false, "IP is already banned, or IP bans are disabled in config"));
            return;
        }
        NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "IP ban created by '{}'", bannedBy);
        sendJson(exchange, 200, json(true, "IP ban created"));
    }

    private void handleRemoveIPBan(HttpExchange exchange, String ip) throws IOException {
        boolean removed = BanManager.getInstance().unbanIP(URLDecoder.decode(ip, StandardCharsets.UTF_8), executorName(exchange));
        NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "IP unban request removed={}", removed);
        sendJson(exchange, 200, json(removed, removed ? "IP ban removed" : "IP ban not found: " + ip));
    }

    private JsonObject ipBanJson(BanManager.IPBanEntry b) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", b.id);
        obj.addProperty("ipAddress", b.ipAddress);
        obj.addProperty("reason", b.reason);
        obj.addProperty("bannedBy", b.bannedBy);
        obj.addProperty("banTime", b.banTime);
        obj.addProperty("expireTime", b.expireTime);
        obj.addProperty("permanent", b.expireTime == 0);
        obj.addProperty("evidence", b.evidence);
        obj.addProperty("active", b.active);
        obj.addProperty("unbannedBy", b.unbannedBy);
        obj.addProperty("unbannedAt", b.unbannedAt);
        return obj;
    }

    // ── Mutes ─────────────────────────────────────────────────────────────────

    private void handleMutes(HttpExchange exchange, boolean activeOnly) throws IOException {
        List<MuteManager.MuteEntry> mutes;
        if (activeOnly) {
            mutes = new ArrayList<>();
            for (String name : MuteManager.getMutedPlayers()) {
                MuteManager.MuteEntry entry = MuteManager.getMuteEntry(name);
                if (entry != null) mutes.add(entry);
            }
        } else {
            mutes = MuteManager.getAllMuteHistory();
            for (String name : MuteManager.getMutedPlayers()) {
                MuteManager.MuteEntry entry = MuteManager.getMuteEntry(name);
                if (entry != null) mutes.add(entry);
            }
        }
        JsonArray arr = new JsonArray();
        for (MuteManager.MuteEntry m : mutes) arr.add(muteJson(m));
        JsonObject resp = new JsonObject();
        resp.addProperty("success", true);
        resp.addProperty("count", mutes.size());
        resp.add("mutes", arr);
        sendJson(exchange, 200, resp.toString());
    }

    private void handleMuteHistory(HttpExchange exchange, String playerName) throws IOException {
        List<MuteManager.MuteEntry> history = MuteManager.getMuteHistory(playerName);
        JsonArray arr = new JsonArray();
        for (MuteManager.MuteEntry m : history) arr.add(muteJson(m));
        JsonObject resp = new JsonObject();
        resp.addProperty("success", true);
        resp.addProperty("count", history.size());
        resp.add("mutes", arr);
        sendJson(exchange, 200, resp.toString());
    }

    private void handleCreateMute(HttpExchange exchange) throws Exception {
        JsonObject body = readBody(exchange);
        if (!body.has("targetName")) {
            sendJson(exchange, 400, json(false, "Body must contain 'targetName'"));
            return;
        }
        String targetName = body.get("targetName").getAsString();
        String reason = body.has("reason") && !body.get("reason").isJsonNull() ? body.get("reason").getAsString() : null;
        long durationSeconds = body.has("duration") && !body.get("duration").isJsonNull() ? body.get("duration").getAsLong() : 0L;
        MuteManager.mute(targetName, reason, executorName(exchange), durationSeconds * 1000L);
        NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Mute created for player '{}'", targetName);
        sendJson(exchange, 200, json(true, targetName + " muted"));
    }

    private void handleRemoveMute(HttpExchange exchange, String targetName) throws IOException {
        MuteManager.unmute(targetName, executorName(exchange));
        NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Unmute request for player '{}'", targetName);
        sendJson(exchange, 200, json(true, targetName + " unmuted"));
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

    // ── IP mutes ──────────────────────────────────────────────────────────────

    private void handleIPMutes(HttpExchange exchange) throws IOException {
        List<MuteManager.MuteEntry> mutes = MuteManager.getAllIPMutes();
        JsonArray arr = new JsonArray();
        for (MuteManager.MuteEntry m : mutes) arr.add(muteJson(m));
        JsonObject resp = new JsonObject();
        resp.addProperty("success", true);
        resp.addProperty("count", mutes.size());
        resp.add("ipMutes", arr);
        sendJson(exchange, 200, resp.toString());
    }

    private void handleCreateIPMute(HttpExchange exchange) throws Exception {
        JsonObject body = readBody(exchange);
        String ip = body.has("ip") ? body.get("ip").getAsString() : "";
        String reason = body.has("reason") && !body.get("reason").isJsonNull() ? body.get("reason").getAsString() : null;
        long durationSeconds = body.has("duration") && !body.get("duration").isJsonNull() ? body.get("duration").getAsLong() : 0L;
        MuteManager.muteIP(ip, reason, executorName(exchange), durationSeconds * 1000L);
        NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "IP mute created");
        sendJson(exchange, 200, json(true, ip + " muted"));
    }

    private void handleRemoveIPMute(HttpExchange exchange, String ip) throws IOException {
        MuteManager.unmuteIP(URLDecoder.decode(ip, StandardCharsets.UTF_8), executorName(exchange));
        NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "IP unmute request");
        sendJson(exchange, 200, json(true, ip + " unmuted"));
    }

    // ── Kicks ─────────────────────────────────────────────────────────────────

    private void handleAllKicks(HttpExchange exchange) throws IOException {
        List<KickManager.KickEntry> kicks = KickManager.getInstance().getAllKicks();
        JsonArray arr = new JsonArray();
        for (KickManager.KickEntry k : kicks) arr.add(kickJson(k));
        JsonObject resp = new JsonObject();
        resp.addProperty("success", true);
        resp.addProperty("count", kicks.size());
        resp.add("kicks", arr);
        sendJson(exchange, 200, resp.toString());
    }

    private void handleKickHistory(HttpExchange exchange, String playerName) throws IOException {
        List<KickManager.KickEntry> kicks = KickManager.getInstance().getKickHistory(playerName);
        JsonArray arr = new JsonArray();
        for (KickManager.KickEntry k : kicks) arr.add(kickJson(k));
        JsonObject resp = new JsonObject();
        resp.addProperty("success", true);
        resp.addProperty("count", kicks.size());
        resp.add("kicks", arr);
        sendJson(exchange, 200, resp.toString());
    }

    private JsonObject kickJson(KickManager.KickEntry k) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", k.id);
        obj.addProperty("playerName", k.playerName);
        obj.addProperty("playerId", k.playerId != null ? k.playerId.toString() : null);
        obj.addProperty("reason", k.reason);
        obj.addProperty("kickedBy", k.kickedBy);
        obj.addProperty("kickTime", k.kickTime);
        return obj;
    }

    // ── Warns ─────────────────────────────────────────────────────────────────

    private void handleAllWarns(HttpExchange exchange) throws IOException {
        WarnManager wm = WarnManager.getInstance();
        List<WarnEntry> all = new ArrayList<>();
        wm.getAllWarnings().forEach(all::addAll);
        all.sort(Comparator.comparingLong(WarnEntry::getTimestamp).reversed());
        sendJson(exchange, 200, warnsJson(all));
    }

    private void handleWarnsForPlayer(HttpExchange exchange, String playerName) throws IOException {
        WarnManager wm = WarnManager.getInstance();
        UUID uuid = wm.findUUIDByName(playerName);
        List<WarnEntry> warns = uuid != null ? wm.getWarnings(uuid) : Collections.emptyList();
        sendJson(exchange, 200, warnsJson(warns));
    }

    private String warnsJson(List<WarnEntry> warns) {
        JsonArray arr = new JsonArray();
        for (WarnEntry w : warns) {
            JsonObject obj = new JsonObject();
            obj.addProperty("id", w.getId());
            obj.addProperty("targetId", w.getTargetId() != null ? w.getTargetId().toString() : null);
            obj.addProperty("targetName", w.getTargetName());
            obj.addProperty("warnedById", w.getWarnedById() != null ? w.getWarnedById().toString() : null);
            obj.addProperty("warnedBy", w.getWarnedBy());
            obj.addProperty("reason", w.getReason());
            obj.addProperty("timestamp", w.getTimestamp());
            arr.add(obj);
        }
        JsonObject resp = new JsonObject();
        resp.addProperty("success", true);
        resp.addProperty("count", warns.size());
        resp.add("warns", arr);
        return resp.toString();
    }

    private void handleRemoveWarn(HttpExchange exchange, String warnId) throws Exception {
        JsonObject body = readBody(exchange);
        String targetName = body.has("targetName") ? body.get("targetName").getAsString() : "";
        WarnManager wm = WarnManager.getInstance();
        UUID uuid = wm.findUUIDByName(targetName);
        if (uuid == null) { sendJson(exchange, 404, json(false, "Player not found: " + targetName)); return; }
        boolean removed = wm.removeWarn(uuid, warnId);
        NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Warn removal for player '{}' warnId={} removed={}", targetName, warnId, removed);
        sendJson(exchange, 200, json(removed, removed ? "Warning removed" : "Warning not found"));
    }

    // ── Notes ─────────────────────────────────────────────────────────────────

    private void handleNotesForPlayer(HttpExchange exchange, String playerName) throws IOException {
        UUID uuid = NoteManager.getInstance().findUUIDByName(playerName);
        List<NoteEntry> notes = uuid != null ? NoteManager.getInstance().getNotes(uuid) : Collections.emptyList();
        JsonArray arr = new JsonArray();
        for (NoteEntry n : notes) {
            JsonObject obj = new JsonObject();
            obj.addProperty("id", n.getId());
            obj.addProperty("targetName", n.getTargetName());
            obj.addProperty("authorId", n.getAuthorId() != null ? n.getAuthorId().toString() : null);
            obj.addProperty("authorName", n.getAuthorName());
            obj.addProperty("text", n.getText());
            obj.addProperty("timestamp", n.getTimestamp());
            arr.add(obj);
        }
        JsonObject resp = new JsonObject();
        resp.addProperty("success", true);
        resp.addProperty("count", notes.size());
        resp.add("notes", arr);
        sendJson(exchange, 200, resp.toString());
    }

    private void handleCreateNote(HttpExchange exchange) throws Exception {
        JsonObject body = readBody(exchange);
        String targetName = body.has("targetName") ? body.get("targetName").getAsString() : "";
        String text = body.has("text") ? body.get("text").getAsString() : "";
        if (targetName.isBlank() || text.isBlank()) {
            sendJson(exchange, 400, json(false, "Body must contain 'targetName' and 'text'"));
            return;
        }

        UUID targetId = resolvePlayerId(targetName);
        if (targetId == null) {
            sendJson(exchange, 404, json(false, "Player not found: " + targetName));
            return;
        }

        String authorName = executorName(exchange);
        NoteEntry entry = NoteManager.getInstance().addNote(targetId, targetName, null, authorName, text);
        NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Note added for player '{}'", targetName);
        sendJson(exchange, 200, "{\"success\":true,\"message\":\"Note added\",\"noteId\":\"" + esc(entry.getId()) + "\"}");
    }

    private void handleRemoveNote(HttpExchange exchange, String noteId) throws Exception {
        JsonObject body = readBody(exchange);
        String targetName = body.has("targetName") ? body.get("targetName").getAsString() : "";
        UUID uuid = NoteManager.getInstance().findUUIDByName(targetName);
        if (uuid == null) { sendJson(exchange, 404, json(false, "Player not found: " + targetName)); return; }
        boolean removed = NoteManager.getInstance().removeNote(uuid, noteId);
        NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Note removal for player '{}' noteId={} removed={}", targetName, noteId, removed);
        sendJson(exchange, 200, json(removed, removed ? "Note removed" : "Note not found"));
    }

    // ── Reports ───────────────────────────────────────────────────────────────

    /**
     * Files a report on the dashboard's behalf — the same underlying store the in-game
     * {@code /report} command writes to, just without a real ServerPlayer as the reporter
     * (reporterId is left null; reporterName is whoever's authenticated on the dashboard,
     * same attribution every other mutating dashboard action already uses).
     */
    private void handleCreateReport(HttpExchange exchange) throws Exception {
        JsonObject body = readBody(exchange);
        String targetName = body.has("targetName") ? body.get("targetName").getAsString() : "";
        String reason = body.has("reason") && !body.get("reason").isJsonNull() ? body.get("reason").getAsString() : "";
        if (targetName.isBlank() || reason.isBlank()) {
            sendJson(exchange, 400, json(false, "Body must contain 'targetName' and 'reason'"));
            return;
        }
        UUID targetId = resolvePlayerId(targetName);
        if (targetId == null) {
            sendJson(exchange, 404, json(false, "Player not found: " + targetName));
            return;
        }
        String reporterName = executorName(exchange);
        ReportEntry entry = ReportManager.getInstance().addReport(null, reporterName, targetId, targetName, reason);
        NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Report filed against '{}' by '{}'", targetName, reporterName);
        JsonObject resp = new JsonObject();
        resp.addProperty("success", true);
        resp.addProperty("message", "Report filed");
        resp.add("report", reportJson(entry));
        sendJson(exchange, 200, resp.toString());
    }

    private void handlePendingReports(HttpExchange exchange) throws IOException {
        sendJson(exchange, 200, reportsJson(ReportManager.getInstance().getPendingReports()));
    }

    private void handleAllReports(HttpExchange exchange) throws IOException {
        sendJson(exchange, 200, reportsJson(ReportManager.getInstance().getAllReports()));
    }

    /** Every report ever filed against a given player, newest logic handled client-side — used by
     *  the player-lookup panel's staff-only reports section. Admin-gated (see route dispatch). */
    private void handleReportsForPlayer(HttpExchange exchange, String playerName) throws IOException {
        UUID targetId = resolvePlayerId(playerName);
        List<ReportEntry> reports = targetId != null
            ? ReportManager.getInstance().getReportsAgainst(targetId) : Collections.emptyList();
        sendJson(exchange, 200, reportsJson(reports));
    }

    private void handleReportById(HttpExchange exchange, String id) throws IOException {
        ReportEntry report = ReportManager.getInstance().findById(id);
        if (report == null) { sendJson(exchange, 404, json(false, "Report not found: " + id)); return; }
        JsonObject resp = new JsonObject();
        resp.addProperty("success", true);
        resp.add("report", reportJson(report));
        sendJson(exchange, 200, resp.toString());
    }

    private void handleReviewReport(HttpExchange exchange, String id) throws Exception {
        JsonObject body = readBody(exchange);
        String statusStr = body.has("status") ? body.get("status").getAsString() : "REVIEWED";
        String notes = body.has("notes") && !body.get("notes").isJsonNull() ? body.get("notes").getAsString() : null;

        ReportEntry.Status status;
        try { status = ReportEntry.Status.valueOf(statusStr.toUpperCase()); }
        catch (Exception e) { sendJson(exchange, 400, json(false, "Invalid status: " + statusStr)); return; }

        String reviewerName = executorName(exchange);
        boolean success = ReportManager.getInstance().reviewReport(id, status, null, reviewerName, notes);
        NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Report {} reviewed by '{}' status={} success={}", id, reviewerName, status, success);
        sendJson(exchange, 200, json(success, success ? "Report updated" : "Report not found: " + id));
    }

    private String reportsJson(List<ReportEntry> reports) {
        JsonArray arr = new JsonArray();
        for (ReportEntry r : reports) arr.add(reportJson(r));
        JsonObject resp = new JsonObject();
        resp.addProperty("success", true);
        resp.addProperty("count", reports.size());
        resp.add("reports", arr);
        return resp.toString();
    }

    private JsonObject reportJson(ReportEntry r) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", r.getId());
        obj.addProperty("reporterId", r.getReporterId() != null ? r.getReporterId().toString() : null);
        obj.addProperty("reporterName", r.getReporterName());
        obj.addProperty("targetId", r.getTargetId() != null ? r.getTargetId().toString() : null);
        obj.addProperty("targetName", r.getTargetName());
        obj.addProperty("reason", r.getReason());
        obj.addProperty("timestamp", r.getTimestamp());
        obj.addProperty("status", r.getStatus().name());
        obj.addProperty("reviewedBy", r.getReviewedBy());
        obj.addProperty("reviewedAt", r.getReviewedAt());
        obj.addProperty("reviewNotes", r.getReviewNotes());
        return obj;
    }

    // ── Freeze ────────────────────────────────────────────────────────────────────

    private void handleFreezeList(HttpExchange exchange) throws IOException {
        JsonArray arr = new JsonArray();
        for (FreezeManager.FreezeEntry f : FreezeManager.getInstance().getAllFrozenPlayers()) arr.add(freezeJson(f));
        JsonObject resp = new JsonObject();
        resp.addProperty("success", true);
        resp.add("frozen", arr);
        sendJson(exchange, 200, resp.toString());
    }

    private void handleFreezeStatus(HttpExchange exchange, String playerName) throws IOException {
        UUID uuid = resolvePlayerId(playerName);
        JsonObject resp = new JsonObject();
        resp.addProperty("success", true);
        FreezeManager.FreezeEntry entry = uuid != null ? FreezeManager.getInstance().getFreezeEntry(uuid) : null;
        resp.addProperty("frozen", entry != null);
        if (entry != null) resp.add("entry", freezeJson(entry));
        sendJson(exchange, 200, resp.toString());
    }

    private void handleCreateFreeze(HttpExchange exchange) throws Exception {
        JsonObject body = readBody(exchange);
        String targetName = body.has("targetName") ? body.get("targetName").getAsString() : "";
        String reason = body.has("reason") && !body.get("reason").isJsonNull() ? body.get("reason").getAsString() : "Frozen by staff";
        UUID targetId = resolvePlayerId(targetName);
        if (targetId == null) {
            sendJson(exchange, 404, json(false, "Player not found: " + targetName));
            return;
        }
        boolean success = FreezeManager.getInstance().freezePlayer(targetName, targetId, reason, executorName(exchange));
        NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Freeze request for player '{}' success={}", targetName, success);
        sendJson(exchange, success ? 200 : 409, json(success, success ? targetName + " frozen" : "Player is already frozen"));
    }

    private void handleRemoveFreeze(HttpExchange exchange, String playerName) throws IOException {
        UUID uuid = resolvePlayerId(playerName);
        if (uuid == null) { sendJson(exchange, 404, json(false, "Player not found: " + playerName)); return; }
        boolean removed = FreezeManager.getInstance().unfreezePlayer(uuid);
        NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Unfreeze request for player '{}' removed={}", playerName, removed);
        sendJson(exchange, 200, json(removed, removed ? playerName + " unfrozen" : "Player was not frozen"));
    }

    private JsonObject freezeJson(FreezeManager.FreezeEntry f) {
        JsonObject obj = new JsonObject();
        obj.addProperty("playerName", f.playerName);
        obj.addProperty("playerId", f.playerId.toString());
        obj.addProperty("reason", f.reason);
        obj.addProperty("frozenBy", f.frozenBy);
        obj.addProperty("freezeTime", f.freezeTime);
        return obj;
    }

    // ── Vanish ────────────────────────────────────────────────────────────────────

    private void handleVanishList(HttpExchange exchange) throws IOException {
        JsonArray arr = new JsonArray();
        MinecraftServer srv = this.server;
        if (srv != null) {
            for (UUID uuid : VanishManager.getInstance().getVanishedPlayers()) {
                var player = srv.getPlayerList().getPlayer(uuid);
                if (player != null) arr.add(player.getName().getString());
            }
        }
        JsonObject resp = new JsonObject();
        resp.addProperty("success", true);
        resp.add("vanished", arr);
        sendJson(exchange, 200, resp.toString());
    }

    private void handleVanishStatus(HttpExchange exchange, String playerName) throws IOException {
        UUID uuid = resolvePlayerId(playerName);
        JsonObject resp = new JsonObject();
        resp.addProperty("success", true);
        resp.addProperty("vanished", uuid != null && VanishManager.getInstance().isPlayerVanished(uuid));
        sendJson(exchange, 200, resp.toString());
    }

    private void handleCreateVanish(HttpExchange exchange) throws Exception {
        JsonObject body = readBody(exchange);
        String targetName = body.has("targetName") ? body.get("targetName").getAsString() : "";
        MinecraftServer srv = this.server;
        var target = srv != null ? srv.getPlayerList().getPlayerByName(targetName) : null;
        if (target == null) {
            sendJson(exchange, 404, json(false, "Player '" + targetName + "' is not online"));
            return;
        }
        boolean success = VanishManager.getInstance().vanishPlayer(target.getUUID(), targetName, executorName(exchange), false);
        NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Vanish request for player '{}' success={}", targetName, success);
        sendJson(exchange, success ? 200 : 409, json(success, success ? targetName + " vanished" : "Player is already vanished"));
    }

    private void handleRemoveVanish(HttpExchange exchange, String playerName) throws IOException {
        UUID uuid = resolvePlayerId(playerName);
        if (uuid == null) { sendJson(exchange, 404, json(false, "Player not found: " + playerName)); return; }
        boolean removed = VanishManager.getInstance().unvanishPlayer(uuid);
        NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Unvanish request for player '{}' removed={}", playerName, removed);
        sendJson(exchange, 200, json(removed, removed ? playerName + " unvanished" : "Player was not vanished"));
    }

    // ── Jail ──────────────────────────────────────────────────────────────────────

    private void handleJailLocations(HttpExchange exchange) throws IOException {
        JsonArray arr = new JsonArray();
        for (JailManager.JailLocation loc : JailManager.getInstance().getAllJailLocations()) arr.add(loc.name);
        JsonObject resp = new JsonObject();
        resp.addProperty("success", true);
        resp.add("jails", arr);
        sendJson(exchange, 200, resp.toString());
    }

    private void handleJailStatus(HttpExchange exchange, String playerName) throws IOException {
        UUID uuid = resolvePlayerId(playerName);
        JsonObject resp = new JsonObject();
        resp.addProperty("success", true);
        JailManager.JailEntry entry = uuid != null ? JailManager.getInstance().getJailEntry(uuid) : null;
        resp.addProperty("jailed", entry != null);
        if (entry != null) resp.add("entry", jailEntryJson(entry));
        sendJson(exchange, 200, resp.toString());
    }

    private void handleCreateJail(HttpExchange exchange) throws Exception {
        JsonObject body = readBody(exchange);
        String targetName = body.has("targetName") ? body.get("targetName").getAsString() : "";
        String jailName = body.has("jailName") ? body.get("jailName").getAsString() : "";
        String reason = body.has("reason") && !body.get("reason").isJsonNull() ? body.get("reason").getAsString() : "Jailed by staff";
        long durationSeconds = body.has("duration") && !body.get("duration").isJsonNull() ? body.get("duration").getAsLong() : 0L;

        JailManager jailManager = JailManager.getInstance();
        if (jailManager.getJailLocation(jailName) == null) {
            sendJson(exchange, 404, json(false, "Jail location not found: " + jailName));
            return;
        }
        UUID targetId = resolvePlayerId(targetName);
        if (targetId == null) {
            sendJson(exchange, 404, json(false, "Player not found: " + targetName));
            return;
        }
        boolean success = jailManager.jailPlayer(targetName, targetId, reason, executorName(exchange), jailName, durationSeconds * 1000L);
        NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Jail request for player '{}' jail='{}' success={}", targetName, jailName, success);
        sendJson(exchange, success ? 200 : 409, json(success, success ? targetName + " jailed" : "Player is already jailed"));
    }

    private void handleRemoveJail(HttpExchange exchange, String playerName) throws IOException {
        UUID uuid = resolvePlayerId(playerName);
        if (uuid == null) { sendJson(exchange, 404, json(false, "Player not found: " + playerName)); return; }
        boolean removed = JailManager.getInstance().unjailPlayer(uuid);
        NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Unjail request for player '{}' removed={}", playerName, removed);
        sendJson(exchange, 200, json(removed, removed ? playerName + " unjailed" : "Player was not jailed"));
    }

    private void handleJailLocationsDetailed(HttpExchange exchange) throws IOException {
        JsonArray arr = new JsonArray();
        for (JailManager.JailLocation loc : JailManager.getInstance().getAllJailLocations()) arr.add(jailLocationJson(loc));
        JsonObject resp = new JsonObject();
        resp.addProperty("success", true);
        resp.add("jailLocations", arr);
        sendJson(exchange, 200, resp.toString());
    }

    /**
     * Creates (or overwrites, by name) a jail cell from typed-in coordinates — {@code sphere}:
     * {name, dimension, shape:"SPHERE", position:{x,y,z}, radius?} or {@code cuboid}:
     * {name, dimension, shape:"CUBOID", corner1:{x,y,z}, corner2:{x,y,z}}.
     */
    private void handleCreateJailLocation(HttpExchange exchange) throws Exception {
        JsonObject body = readBody(exchange);
        String name = body.has("name") && !body.get("name").isJsonNull() ? body.get("name").getAsString() : "";
        if (name.isBlank()) {
            sendJson(exchange, 400, json(false, "Body must contain 'name'"));
            return;
        }
        String dimension = body.has("dimension") && !body.get("dimension").isJsonNull()
            ? body.get("dimension").getAsString() : "minecraft:overworld";
        String shape = body.has("shape") && !body.get("shape").isJsonNull() ? body.get("shape").getAsString() : "SPHERE";
        String createdBy = executorName(exchange);
        JailManager jailManager = JailManager.getInstance();

        if ("CUBOID".equalsIgnoreCase(shape)) {
            if (!body.has("corner1") || !body.has("corner2")) {
                sendJson(exchange, 400, json(false, "Cuboid jail requires 'corner1' and 'corner2'"));
                return;
            }
            BlockPos corner1 = blockPosFromJson(body.getAsJsonObject("corner1"));
            BlockPos corner2 = blockPosFromJson(body.getAsJsonObject("corner2"));
            jailManager.setJailLocationCuboid(name, corner1, corner2, dimension, createdBy);
        } else {
            if (!body.has("position")) {
                sendJson(exchange, 400, json(false, "Sphere jail requires 'position'"));
                return;
            }
            BlockPos center = blockPosFromJson(body.getAsJsonObject("position"));
            double radius = body.has("radius") && !body.get("radius").isJsonNull()
                ? body.get("radius").getAsDouble()
                : com.zerog.neoessentials.config.ConfigManager.getDefaultJailSphereRadius();
            jailManager.setJailLocationSphere(name, center, radius, dimension, createdBy);
        }
        NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Jail location '{}' created via dashboard by '{}'", name, createdBy);
        sendJson(exchange, 200, json(true, "Jail location '" + name + "' created"));
    }

    private void handleRemoveJailLocation(HttpExchange exchange, String name) throws IOException {
        boolean removed = JailManager.getInstance().removeJailLocation(URLDecoder.decode(name, StandardCharsets.UTF_8));
        NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Jail location '{}' remove request removed={}", name, removed);
        sendJson(exchange, 200, json(removed, removed ? "Jail location removed" : "Jail location not found: " + name));
    }

    private BlockPos blockPosFromJson(JsonObject obj) {
        return new BlockPos(obj.get("x").getAsInt(), obj.get("y").getAsInt(), obj.get("z").getAsInt());
    }

    private JsonObject jailLocationJson(JailManager.JailLocation loc) {
        JsonObject obj = new JsonObject();
        obj.addProperty("name", loc.name);
        obj.addProperty("dimension", loc.dimension);
        obj.addProperty("shape", loc.shape.name());
        obj.addProperty("createdBy", loc.createdBy);
        obj.addProperty("createdTime", loc.createdTime);

        JsonObject pos = new JsonObject();
        pos.addProperty("x", loc.position.getX());
        pos.addProperty("y", loc.position.getY());
        pos.addProperty("z", loc.position.getZ());
        obj.add("position", pos);

        if (loc.shape == JailManager.JailShape.CUBOID && loc.corner1 != null && loc.corner2 != null) {
            JsonObject c1 = new JsonObject();
            c1.addProperty("x", loc.corner1.getX());
            c1.addProperty("y", loc.corner1.getY());
            c1.addProperty("z", loc.corner1.getZ());
            obj.add("corner1", c1);
            JsonObject c2 = new JsonObject();
            c2.addProperty("x", loc.corner2.getX());
            c2.addProperty("y", loc.corner2.getY());
            c2.addProperty("z", loc.corner2.getZ());
            obj.add("corner2", c2);
        } else {
            obj.addProperty("radius", loc.radius);
        }
        return obj;
    }

    private JsonObject jailEntryJson(JailManager.JailEntry j) {
        JsonObject obj = new JsonObject();
        obj.addProperty("playerName", j.playerName);
        obj.addProperty("playerId", j.playerId.toString());
        obj.addProperty("reason", j.reason);
        obj.addProperty("jailedBy", j.jailedBy);
        obj.addProperty("jailTime", j.jailTime);
        obj.addProperty("expireAt", j.expireAt);
        obj.addProperty("jailName", j.jailName);
        return obj;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────

    /** Resolves a player's UUID from the online player list, falling back to the profile cache. */
    private UUID resolvePlayerId(String playerName) {
        if (server == null || playerName == null || playerName.isBlank()) return null;
        var online = server.getPlayerList().getPlayerByName(playerName);
        if (online != null) return online.getUUID();
        try {
            var cache = server.services().nameToIdCache();
            if (cache != null) {
                var profile = cache.get(playerName);
                if (profile.isPresent()) return profile.get().id();
            }
        } catch (Exception e) {
            NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Failed to resolve player id for '{}'", playerName, e);
        }
        return null;
    }

    private String executorName(HttpExchange exchange) {
        String username = (String) exchange.getAttribute("auth-username");
        return username != null ? username : "dashboard";
    }

    private String lastSegment(String path) {
        return path.substring(path.lastIndexOf('/') + 1);
    }

    private void requireAdmin(HttpExchange exchange) throws IOException {
        Boolean admin = (Boolean) exchange.getAttribute("auth-admin");
        if (!Boolean.TRUE.equals(admin)) {
            sendJson(exchange, 403, "{\"success\":false,\"error\":\"Admin access required\"}");
            throw new SecurityException("Not admin");
        }
    }

    private JsonObject readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            String s = new String(is.readAllBytes(), StandardCharsets.UTF_8).trim();
            if (s.isEmpty()) return new JsonObject();
            var parsed = JsonParser.parseString(s);
            return parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();
        }
    }

    private void sendJson(HttpExchange exchange, int code, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
    }

    private String json(boolean ok, String msg) {
        return "{\"success\":" + ok + ",\"message\":\"" + esc(msg) + "\"}";
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r");
    }
}
