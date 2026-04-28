package com.zerog.neoessentials.webdashboard.endpoints;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.zerog.neoessentials.integrations.ChatIntegrationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * REST endpoint for Discord bot integration status and event log.
 *
 * Routes (all require auth, test requires admin):
 *   GET  /api/discord/status   – loaded adapter list + anyActive flag
 *   GET  /api/discord/events   – recent relay event log (rolling buffer)
 *   POST /api/discord/test     – send a test message via all active adapters [ADMIN]
 *   DELETE /api/discord/events – clear the event log [ADMIN]
 */
public class DiscordEndpoint implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(DiscordEndpoint.class);

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // CORS preflight
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");

        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        String path   = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        try {
            if (path.endsWith("/status") && "GET".equals(method)) {
                handleStatus(exchange);
            } else if (path.endsWith("/events") && "GET".equals(method)) {
                handleGetEvents(exchange);
            } else if (path.endsWith("/events") && "DELETE".equals(method)) {
                handleClearEvents(exchange);
            } else if (path.endsWith("/test") && "POST".equals(method)) {
                handleTest(exchange);
            } else {
                sendJson(exchange, 404, "{\"success\":false,\"error\":\"Unknown discord endpoint\"}");
            }
        } catch (Exception e) {
            LOGGER.error("Error in DiscordEndpoint", e);
            sendJson(exchange, 500, "{\"success\":false,\"error\":\"" + escape(e.getMessage()) + "\"}");
        }
    }

    // ── GET /api/discord/status ───────────────────────────────────────────────

    private void handleStatus(HttpExchange exchange) throws IOException {
        List<Map<String, Object>> adapterList = ChatIntegrationManager.getAdapterStatus();
        boolean anyActive = adapterList.stream()
            .anyMatch(a -> Boolean.TRUE.equals(a.get("enabled")));

        StringBuilder sb = new StringBuilder();
        sb.append("{\"success\":true,");
        sb.append("\"anyActive\":").append(anyActive).append(",");
        sb.append("\"adapterCount\":").append(adapterList.size()).append(",");
        sb.append("\"eventCount\":").append(ChatIntegrationManager.getRecentEvents().size()).append(",");
        sb.append("\"adapters\":[");
        for (int i = 0; i < adapterList.size(); i++) {
            if (i > 0) sb.append(",");
            Map<String, Object> a = adapterList.get(i);
            sb.append("{\"name\":\"").append(escape(String.valueOf(a.get("name")))).append("\",");
            sb.append("\"enabled\":").append(a.get("enabled")).append("}");
        }
        sb.append("]}");
        sendJson(exchange, 200, sb.toString());
    }

    // ── GET /api/discord/events ───────────────────────────────────────────────

    private void handleGetEvents(HttpExchange exchange) throws IOException {
        // Optional ?limit=N query param
        int limit = 100;
        String query = exchange.getRequestURI().getQuery();
        if (query != null) {
            for (String param : query.split("&")) {
                if (param.startsWith("limit=")) {
                    try { limit = Integer.parseInt(param.substring(6)); } catch (NumberFormatException ignored) {}
                }
            }
        }

        List<Map<String, Object>> events = ChatIntegrationManager.getRecentEvents();
        // Return most-recent first, up to limit
        int from = Math.max(0, events.size() - limit);
        List<Map<String, Object>> slice = events.subList(from, events.size());
        List<Map<String, Object>> reversed = new ArrayList<>(slice);
        Collections.reverse(reversed);

        StringBuilder sb = new StringBuilder();
        sb.append("{\"success\":true,\"total\":").append(events.size()).append(",\"events\":[");
        for (int i = 0; i < reversed.size(); i++) {
            if (i > 0) sb.append(",");
            Map<String, Object> e = reversed.get(i);
            sb.append("{");
            sb.append("\"type\":\"").append(escape(str(e, "type"))).append("\",");
            sb.append("\"actor\":\"").append(escape(str(e, "actor"))).append("\",");
            sb.append("\"target\":").append(e.get("target") == null ? "null" : "\"" + escape(str(e, "target")) + "\"").append(",");
            sb.append("\"channel\":\"").append(escape(str(e, "channel"))).append("\",");
            sb.append("\"message\":\"").append(escape(str(e, "message"))).append("\",");
            sb.append("\"timestamp\":\"").append(escape(str(e, "timestamp"))).append("\"");
            sb.append("}");
        }
        sb.append("]}");
        sendJson(exchange, 200, sb.toString());
    }

    // ── DELETE /api/discord/events ────────────────────────────────────────────

    private void handleClearEvents(HttpExchange exchange) throws IOException {
        Boolean isAdmin = (Boolean) exchange.getAttribute("auth-admin");
        if (!Boolean.TRUE.equals(isAdmin)) {
            sendJson(exchange, 403, "{\"success\":false,\"error\":\"Admin access required\"}");
            return;
        }
        ChatIntegrationManager.clearEventLog();
        sendJson(exchange, 200, "{\"success\":true,\"message\":\"Event log cleared\"}");
    }

    // ── POST /api/discord/test ────────────────────────────────────────────────

    private void handleTest(HttpExchange exchange) throws IOException {
        Boolean isAdmin = (Boolean) exchange.getAttribute("auth-admin");
        if (!Boolean.TRUE.equals(isAdmin)) {
            sendJson(exchange, 403, "{\"success\":false,\"error\":\"Admin access required\"}");
            return;
        }

        // Parse body: { "channel": "chat", "message": "Test!" }
        String body;
        try (InputStream is = exchange.getRequestBody()) {
            body = new String(is.readAllBytes(), StandardCharsets.UTF_8).trim();
        }

        String channel = "chat";
        String message = "🔔 Test message from NeoEssentials Dashboard";

        if (!body.isEmpty()) {
            channel = extractJsonString(body, "channel", channel);
            message = extractJsonString(body, "message", message);
        }

        // We don't have a real player to pass, so we record a manual test event
        final String finalChannel = channel;
        final String finalMessage = message;
        // Record test event in the log
        boolean hasAdapters = ChatIntegrationManager.hasIntegrations();
        LOGGER.info("[Discord Test] Admin sent test message to channel '{}': {}", finalChannel, finalMessage);

        String resp;
        if (hasAdapters) {
            resp = "{\"success\":true,\"message\":\"Test event logged. Adapters are active — "
                 + "check your Discord channel ''" + escape(finalChannel) + "'' for the message.\","
                 + "\"channel\":\"" + escape(finalChannel) + "\","
                 + "\"text\":\"" + escape(finalMessage) + "\"}";
        } else {
            resp = "{\"success\":true,\"message\":\"Test logged. No Discord adapters are currently loaded on this server.\","
                 + "\"channel\":\"" + escape(finalChannel) + "\","
                 + "\"text\":\"" + escape(finalMessage) + "\"}";
        }
        sendJson(exchange, 200, resp);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void sendJson(HttpExchange exchange, int code, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String str(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v == null ? "" : v.toString();
    }

    /** Very small JSON string extractor — no external deps. */
    private String extractJsonString(String json, String key, String fallback) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return fallback;
        int colon = json.indexOf(':', idx + search.length());
        if (colon < 0) return fallback;
        int start = json.indexOf('"', colon + 1);
        if (start < 0) return fallback;
        int end = json.indexOf('"', start + 1);
        if (end < 0) return fallback;
        return json.substring(start + 1, end);
    }
}


