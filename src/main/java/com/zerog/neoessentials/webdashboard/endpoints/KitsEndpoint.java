package com.zerog.neoessentials.webdashboard.endpoints;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.zerog.neoessentials.kits.Kit;
import com.zerog.neoessentials.kits.KitManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * REST handler for kit management via the dashboard.
 *
 *   GET  /api/kits/list       – list all kits with metadata
 *   GET  /api/kits/stats      – overview stats (count, enabled, cooldown distribution)
 *   GET  /api/kits/{name}     – get single kit info
 */
public class KitsEndpoint implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(KitsEndpoint.class);

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin",  "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");

        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if (!"GET".equals(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"success\":false,\"error\":\"Method not allowed\"}");
            return;
        }

        String path = exchange.getRequestURI().getPath();

        try {
            if (path.endsWith("/list")) {
                handleList(exchange);
            } else if (path.endsWith("/stats")) {
                handleStats(exchange);
            } else {
                // /api/kits/{name}
                String kitName = path.substring(path.lastIndexOf('/') + 1);
                handleGetKit(exchange, kitName);
            }
        } catch (Exception e) {
            LOGGER.error("Error in KitsEndpoint", e);
            sendJson(exchange, 500, "{\"success\":false,\"error\":\"" + esc(e.getMessage()) + "\"}");
        }
    }

    // ── List all kits ─────────────────────────────────────────────────────────

    private void handleList(HttpExchange exchange) throws IOException {
        Collection<Kit> kits = KitManager.getInstance().getAllKits();
        StringBuilder sb = new StringBuilder("{\"success\":true,\"count\":").append(kits.size()).append(",\"kits\":[");
        boolean first = true;
        for (Kit k : kits) {
            if (!first) sb.append(",");
            first = false;
            sb.append(kitJson(k));
        }
        sb.append("]}");
        sendJson(exchange, 200, sb.toString());
    }

    // ── Stats overview ────────────────────────────────────────────────────────

    private void handleStats(HttpExchange exchange) throws IOException {
        Collection<Kit> kits = KitManager.getInstance().getAllKits();
        long total   = kits.size();
        long enabled = kits.stream().filter(Kit::isEnabled).count();
        long withPerm = kits.stream().filter(k -> k.getPermission() != null && !k.getPermission().isEmpty()).count();
        long withCooldown = kits.stream().filter(k -> k.getCooldownMillis() > 0).count();
        long withLimit = kits.stream().filter(k -> k.getMaxUses() > 0).count();

        String resp = "{\"success\":true,"
            + "\"total\":" + total + ","
            + "\"enabled\":" + enabled + ","
            + "\"withPermission\":" + withPerm + ","
            + "\"withCooldown\":" + withCooldown + ","
            + "\"withUsageLimit\":" + withLimit + "}";
        sendJson(exchange, 200, resp);
    }

    // ── Single kit ────────────────────────────────────────────────────────────

    private void handleGetKit(HttpExchange exchange, String kitName) throws IOException {
        Kit kit = KitManager.getInstance().getKit(kitName);
        if (kit == null) {
            sendJson(exchange, 404, "{\"success\":false,\"error\":\"Kit not found: " + esc(kitName) + "\"}");
            return;
        }
        sendJson(exchange, 200, "{\"success\":true,\"kit\":" + kitJson(kit) + "}");
    }

    // ── Kit JSON serialiser ───────────────────────────────────────────────────

    private String kitJson(Kit k) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"name\":\"").append(esc(k.getName())).append("\",");
        sb.append("\"displayName\":\"").append(esc(k.getDisplayName())).append("\",");
        sb.append("\"description\":\"").append(esc(k.getDescription())).append("\",");
        sb.append("\"enabled\":").append(k.isEnabled()).append(",");
        sb.append("\"permission\":").append(k.getPermission() != null ? "\"" + esc(k.getPermission()) + "\"" : "null").append(",");
        sb.append("\"cooldownMs\":").append(k.getCooldownMillis()).append(",");
        sb.append("\"cooldownDisplay\":\"").append(esc(formatCooldown(k.getCooldownMillis()))).append("\",");
        sb.append("\"maxUses\":").append(k.getMaxUses()).append(",");
        sb.append("\"itemCount\":").append(k.getItems().size());
        sb.append("}");
        return sb.toString();
    }

    private String formatCooldown(long ms) {
        if (ms <= 0) return "No cooldown";
        long seconds = TimeUnit.MILLISECONDS.toSeconds(ms);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(ms);
        long hours   = TimeUnit.MILLISECONDS.toHours(ms);
        long days    = TimeUnit.MILLISECONDS.toDays(ms);
        if (days > 0)    return days + "d " + (hours % 24) + "h";
        if (hours > 0)   return hours + "h " + (minutes % 60) + "m";
        if (minutes > 0) return minutes + "m " + (seconds % 60) + "s";
        return seconds + "s";
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private void sendJson(HttpExchange exchange, int code, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r");
    }
}

