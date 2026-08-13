package com.zerog.neoessentials.webdashboard.endpoints;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.zerog.neoessentials.api.PlaceholderManager;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * REST API endpoint for the NeoEssentials Placeholder system.
 *
 * <h3>Routes (all require authentication)</h3>
 * <ul>
 *   <li>{@code GET /api/placeholders/list}   – All registered identifiers and expansion names</li>
 *   <li>{@code GET /api/placeholders/resolve?player=&lt;name&gt;&text=&lt;str&gt;} – Resolve a string server-side</li>
 *   <li>{@code GET /api/placeholders/stats}  – Registry statistics (counts, etc.)</li>
 * </ul>
 */
public class PlaceholderEndpoint implements HttpHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlaceholderEndpoint.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final MinecraftServer server;

    public PlaceholderEndpoint(MinecraftServer server) {
        this.server = server;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HttpHandler dispatch
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path   = exchange.getRequestURI().getPath();

        NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "PlaceholderEndpoint request: {} {}", method, path);

        try {
            if (!"GET".equalsIgnoreCase(method)) {
                sendJson(exchange, 405, errorBody("Method not allowed"));
                return;
            }

            if (path.equals("/api/placeholders/list")) {
                handleList(exchange);
            } else if (path.equals("/api/placeholders/resolve")) {
                handleResolve(exchange);
            } else if (path.equals("/api/placeholders/stats")) {
                handleStats(exchange);
            } else {
                sendJson(exchange, 404, errorBody("Endpoint not found. Available: /list, /resolve, /stats"));
            }
        } catch (Exception e) {
            LOGGER.error("Error in PlaceholderEndpoint [{} {}]", method, path, e);
            try {
                sendJson(exchange, 500, errorBody("Internal server error: " + sanitise(e.getMessage())));
            } catch (IOException ignored) {}
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/placeholders/list
    // ─────────────────────────────────────────────────────────────────────────

    private void handleList(HttpExchange exchange) throws IOException {
        PlaceholderManager pm = PlaceholderManager.getInstance();
        Set<String> all = new TreeSet<>(pm.getRegisteredPlaceholders());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("count", all.size());
        body.put("placeholders", all);

        sendJson(exchange, 200, body);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/placeholders/resolve?player=<name>&text=<raw_text>
    // ─────────────────────────────────────────────────────────────────────────

    private void handleResolve(HttpExchange exchange) throws IOException {
        Map<String, String> params = parseQuery(exchange.getRequestURI());

        String text       = params.get("text");
        String playerName = params.get("player");

        if (text == null || text.isBlank()) {
            sendJson(exchange, 400, errorBody("Missing query parameter: text"));
            return;
        }

        PlaceholderManager pm = PlaceholderManager.getInstance();
        ServerPlayer target = null;

        if (playerName != null && !playerName.isBlank() && server != null) {
            target = server.getPlayerList().getPlayerByName(playerName);
        }

        String resolved = pm.setPlaceholders(target, text);
        NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Resolved placeholders for player='{}'", playerName);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("player",   target != null ? target.getName().getString() : null);
        body.put("input",    text);
        body.put("resolved", resolved);

        sendJson(exchange, 200, body);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/placeholders/stats
    // ─────────────────────────────────────────────────────────────────────────

    private void handleStats(HttpExchange exchange) throws IOException {
        PlaceholderManager pm = PlaceholderManager.getInstance();
        Map<String, Object> stats = pm.getStatistics();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("stats", stats);

        sendJson(exchange, 200, body);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private Map<String, String> parseQuery(URI uri) {
        Map<String, String> result = new LinkedHashMap<>();
        String query = uri.getRawQuery();
        if (query == null || query.isBlank()) return result;
        for (String pair : query.split("&")) {
            int idx = pair.indexOf('=');
            if (idx < 0) continue;
            String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
            String val = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
            result.put(key, val);
        }
        return result;
    }

    private Map<String, Object> errorBody(String message) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("success", false);
        m.put("error", message);
        return m;
    }

    private String sanitise(String msg) {
        return msg == null ? "unknown" : msg.replace("\"", "'");
    }

    private void sendJson(HttpExchange exchange, int code, Object payload) throws IOException {
        byte[] bytes = GSON.toJson(payload).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}

