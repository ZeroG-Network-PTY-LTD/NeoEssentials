package com.zerog.neoessentials.webdashboard.endpoints;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.zerog.neoessentials.moderation.*;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

/**
 * REST handler for moderation management via the dashboard.
 *
 * Routes (mutating ops require admin):
 *  GET  /api/moderation/overview        – counts: bans, warnings, muted, jailed
 *  GET  /api/moderation/bans            – list all bans
 *  GET  /api/moderation/bans/active     – active bans only
 *  POST /api/moderation/ban             – create a ban {target, playerName, reason, type, duration}
 *  DELETE /api/moderation/ban/{id}      – remove (pardon) a ban
 *  GET  /api/moderation/warns           – all warn entries (all players)
 *  GET  /api/moderation/warns/{name}    – warns for a specific player name
 *  DELETE /api/moderation/warn/{id}     – remove a specific warn (body: {targetName})
 *  GET  /api/moderation/mutes           – list currently muted player names
 *  POST /api/moderation/mute            – mute a player {targetName, duration?} (seconds; omit = indefinite)
 *  DELETE /api/moderation/mute/{name}   – unmute a player
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

        try {
            if ("GET".equals(method) && path.endsWith("/overview")) {
                handleOverview(exchange);
            } else if ("GET".equals(method) && path.endsWith("/bans/active")) {
                handleBans(exchange, true);
            } else if ("GET".equals(method) && path.endsWith("/bans")) {
                handleBans(exchange, false);
            } else if ("POST".equals(method) && path.endsWith("/ban")) {
                requireAdmin(exchange);
                handleCreateBan(exchange);
            } else if ("DELETE".equals(method) && path.contains("/ban/")) {
                requireAdmin(exchange);
                String banId = path.substring(path.lastIndexOf('/') + 1);
                handleRemoveBan(exchange, banId);
            } else if ("GET".equals(method) && path.contains("/warns/")) {
                String playerName = path.substring(path.lastIndexOf('/') + 1);
                handleWarnsForPlayer(exchange, playerName);
            } else if ("GET".equals(method) && path.endsWith("/warns")) {
                handleAllWarns(exchange);
            } else if ("DELETE".equals(method) && path.contains("/warn/")) {
                requireAdmin(exchange);
                String warnId = path.substring(path.lastIndexOf('/') + 1);
                handleRemoveWarn(exchange, warnId);
            } else if ("GET".equals(method) && path.endsWith("/mutes")) {
                handleMutes(exchange);
            } else if ("POST".equals(method) && path.endsWith("/mute")) {
                requireAdmin(exchange);
                handleCreateMute(exchange);
            } else if ("DELETE".equals(method) && path.contains("/mute/")) {
                requireAdmin(exchange);
                String targetName = path.substring(path.lastIndexOf('/') + 1);
                handleRemoveMute(exchange, targetName);
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
        ModerationManager mm = getModerationManager();
        WarnManager wm = WarnManager.getInstance();

        long activeBans = mm == null ? 0L :
            mm.getAllBans().stream().filter(b -> b != null && b.isActive()).count();
        long totalBans  = mm == null ? 0L : mm.getAllBans().size();
        long totalWarns = wm.getAllWarnings().stream().mapToLong(java.util.List::size).sum();
        int mutedCount  = com.zerog.neoessentials.chat.MuteManager.getMutedPlayers().size();
        int jailedCount = 0;
        try {
            jailedCount = JailManager.getInstance().getAllJailedPlayers().size();
        } catch (Exception ignored) {}

        String resp = "{\"success\":true,"
            + "\"activeBans\":" + activeBans + ","
            + "\"totalBans\":" + totalBans + ","
            + "\"totalWarns\":" + totalWarns + ","
            + "\"mutedCount\":" + mutedCount + ","
            + "\"jailedCount\":" + jailedCount + "}";
        sendJson(exchange, 200, resp);
    }

    // ── Bans ─────────────────────────────────────────────────────────────────────

    private void handleBans(HttpExchange exchange, boolean activeOnly) throws IOException {
        ModerationManager mm = getModerationManager();
        Collection<BanEntry> bans = mm == null ? Collections.emptyList() :
            (activeOnly ? mm.getActiveBans() : mm.getAllBans());

        StringBuilder sb = new StringBuilder("{\"success\":true,\"count\":").append(bans.size()).append(",\"bans\":[");
        boolean first = true;
        for (BanEntry b : bans) {
            if (b == null) continue;
            if (!first) sb.append(",");
            first = false;
            sb.append("{");
            sb.append("\"id\":\"").append(esc(b.getId())).append("\",");
            sb.append("\"type\":\"").append(b.getType() != null ? b.getType().name() : "UUID").append("\",");
            sb.append("\"target\":\"").append(esc(b.getTarget())).append("\",");
            sb.append("\"playerName\":\"").append(esc(b.getPlayerName())).append("\",");
            sb.append("\"reason\":\"").append(esc(b.getReason())).append("\",");
            sb.append("\"bannedBy\":\"").append(esc(b.getBannedBy())).append("\",");
            sb.append("\"bannedAt\":\"").append(b.getBannedAt() != null ? b.getBannedAt().toString() : "").append("\",");
            sb.append("\"expiresAt\":").append(b.getExpiresAt() != null ? "\"" + b.getExpiresAt().toString() + "\"" : "null").append(",");
            sb.append("\"permanent\":").append(b.isPermanent()).append(",");
            sb.append("\"active\":").append(b.isActive());
            sb.append("}");
        }
        sb.append("]}");
        sendJson(exchange, 200, sb.toString());
    }

    // ── Create ban ────────────────────────────────────────────────────────────────

    private void handleCreateBan(HttpExchange exchange) throws Exception {
        JsonObject body = readBody(exchange);
        ModerationManager mm = getModerationManager();
        if (mm == null || server == null) {
            sendJson(exchange, 503, json(false, "Moderation system not available"));
            return;
        }

        String target     = body.has("target")     ? body.get("target").getAsString()     : "";
        String playerName = body.has("playerName") ? body.get("playerName").getAsString() : target;
        String reason     = body.has("reason")     ? body.get("reason").getAsString()     : "No reason provided";
        String typeStr    = body.has("type")       ? body.get("type").getAsString()       : "UUID";
        long   duration   = body.has("duration")   ? body.get("duration").getAsLong()     : -1L; // seconds, -1=permanent

        BanEntry.BanType banType;
        try { banType = BanEntry.BanType.valueOf(typeStr.toUpperCase()); }
        catch (Exception e) { banType = BanEntry.BanType.UUID; }

        Instant expires = duration > 0 ? Instant.now().plusSeconds(duration) : null;
        String executor = (String) exchange.getAttribute("auth-username");
        BanEntry ban = mm.addBan(banType, target, playerName, reason, null, expires, executor != null ? executor : "dashboard");

        sendJson(exchange, 200, "{\"success\":true,\"message\":\"Ban created\",\"banId\":\"" + esc(ban.getId()) + "\"}");
    }

    // ── Remove ban ────────────────────────────────────────────────────────────────

    private void handleRemoveBan(HttpExchange exchange, String banId) throws IOException {
        ModerationManager mm = getModerationManager();
        if (mm == null) { sendJson(exchange, 503, json(false, "Moderation unavailable")); return; }
        boolean removed = mm.removeBan(banId);
        sendJson(exchange, 200, json(removed, removed ? "Ban removed" : "Ban not found: " + banId));
    }

    // ── Warns ─────────────────────────────────────────────────────────────────────

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
        StringBuilder sb = new StringBuilder("{\"success\":true,\"count\":").append(warns.size()).append(",\"warns\":[");
        boolean first = true;
        for (WarnEntry w : warns) {
            if (!first) sb.append(",");
            first = false;
            sb.append("{");
            sb.append("\"id\":\"").append(esc(w.getId())).append("\",");
            sb.append("\"targetName\":\"").append(esc(w.getTargetName())).append("\",");
            sb.append("\"warnedBy\":\"").append(esc(w.getWarnedBy())).append("\",");
            sb.append("\"reason\":\"").append(esc(w.getReason())).append("\",");
            sb.append("\"timestamp\":").append(w.getTimestamp());
            sb.append("}");
        }
        sb.append("]}");
        return sb.toString();
    }

    // ── Remove warn ────────────────────────────────────────────────────────────────

    private void handleRemoveWarn(HttpExchange exchange, String warnId) throws Exception {
        JsonObject body = readBody(exchange);
        String targetName = body.has("targetName") ? body.get("targetName").getAsString() : "";
        WarnManager wm = WarnManager.getInstance();
        UUID uuid = wm.findUUIDByName(targetName);
        if (uuid == null) { sendJson(exchange, 404, json(false, "Player not found: " + targetName)); return; }
        boolean removed = wm.removeWarn(uuid, warnId);
        sendJson(exchange, 200, json(removed, removed ? "Warning removed" : "Warning not found"));
    }

    // ── Mutes ─────────────────────────────────────────────────────────────────────

    private void handleMutes(HttpExchange exchange) throws IOException {
        Set<String> muted = com.zerog.neoessentials.chat.MuteManager.getMutedPlayers();
        StringBuilder sb = new StringBuilder("{\"success\":true,\"count\":").append(muted.size()).append(",\"muted\":[");
        boolean first = true;
        for (String name : muted) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(esc(name)).append("\"");
        }
        sb.append("]}");
        sendJson(exchange, 200, sb.toString());
    }

    private void handleCreateMute(HttpExchange exchange) throws Exception {
        JsonObject body = readBody(exchange);
        if (!body.has("targetName")) {
            sendJson(exchange, 400, json(false, "Body must contain 'targetName'"));
            return;
        }
        String targetName = body.get("targetName").getAsString();
        if (body.has("duration")) {
            long durationSeconds = body.get("duration").getAsLong();
            com.zerog.neoessentials.chat.MuteManager.mute(targetName, durationSeconds * 1000L);
        } else {
            com.zerog.neoessentials.chat.MuteManager.mute(targetName);
        }
        sendJson(exchange, 200, json(true, targetName + " muted"));
    }

    private void handleRemoveMute(HttpExchange exchange, String targetName) throws IOException {
        com.zerog.neoessentials.chat.MuteManager.unmute(targetName);
        sendJson(exchange, 200, json(true, targetName + " unmuted"));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────

    private ModerationManager getModerationManager() {
        try {
            ModerationManager mm = ModerationManager.getInstance();
            if (server != null) mm.setServer(server);
            return mm;
        } catch (Exception e) {
            LOGGER.warn("ModerationManager unavailable: {}", e.getMessage());
            return null;
        }
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
            return s.isEmpty() ? new JsonObject() : JsonParser.parseString(s).getAsJsonObject();
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



