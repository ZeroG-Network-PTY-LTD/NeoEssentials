package com.zerog.neoessentials.webdashboard.endpoints;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.zerog.neoessentials.leaderboard.LeaderboardCache;
import com.zerog.neoessentials.leaderboard.LeaderboardManager;
import com.zerog.neoessentials.leaderboard.config.LeaderboardConfigLoader;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

/**
 * Dashboard API endpoint for leaderboards. Reads are open to any authenticated caller;
 * writes (custom-board create/delete/value-set) require ADMIN, same convention as
 * {@code ScoreboardEndpoint}/{@code MotdEndpoint}. Config-sourced boards (economy/
 * vanilla_stat) aren't writable here — hand-edit {@code leaderboard.json} for those.
 *
 * <pre>
 * GET    /api/leaderboard                  – list registered board ids + display names
 * GET    /api/leaderboard/{board}?page=N   – paginated entries for one board
 * POST   /api/leaderboard/boards           { id, displayName } – create a custom board
 * PUT    /api/leaderboard/boards/{id}/value { player, value }  – set a custom board's value
 * DELETE /api/leaderboard/boards/{id}      – delete a custom board
 * </pre>
 */
public class LeaderboardEndpoint implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(LeaderboardEndpoint.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final int PAGE_SIZE = 10;

    private final MinecraftServer server;

    public LeaderboardEndpoint(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Authorization, Content-Type");
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        String method = exchange.getRequestMethod().toUpperCase();
        String path = exchange.getRequestURI().getPath().replaceFirst("^/api/leaderboard", "");
        NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "LeaderboardEndpoint request: {} {}", method, path);

        if (!"GET".equals(method) && !Boolean.TRUE.equals(exchange.getAttribute("auth-admin"))) {
            sendResponse(exchange, 403, error("Admin access required"));
            return;
        }

        try {
            JsonObject response;
            int statusCode = 200;
            switch (method) {
                case "GET" -> response = path.isEmpty() || path.equals("/") ? handleList() : handleGetBoard(exchange, path);
                case "POST" -> response = handlePost(path, exchange);
                case "PUT" -> response = handlePut(path, exchange);
                case "DELETE" -> response = handleDelete(path);
                default -> { response = error("Method not allowed"); statusCode = 405; }
            }
            if (statusCode == 200 && response.has("success") && !response.get("success").getAsBoolean()) statusCode = 400;
            sendResponse(exchange, statusCode, response);
        } catch (Exception e) {
            LOGGER.error("Error handling leaderboard endpoint request to {}", exchange.getRequestURI(), e);
            sendResponse(exchange, 500, error("Internal server error: " + e.getMessage()));
        }
    }

    // ── GET ────────────────────────────────────────────────────────────────────
    private JsonObject handleList() {
        JsonObject obj = new JsonObject();
        obj.addProperty("success", true);
        JsonArray boards = new JsonArray();
        for (String id : LeaderboardManager.getInstance().getRegisteredBoardIds()) {
            LeaderboardCache cache = LeaderboardManager.getInstance().getBoard(id);
            if (cache == null) continue;
            JsonObject b = new JsonObject();
            b.addProperty("id", id);
            b.addProperty("displayName", cache.getDefinition().displayName());
            b.addProperty("custom", LeaderboardConfigLoader.isCustomBoard(id));
            boards.add(b);
        }
        obj.add("boards", boards);
        return obj;
    }

    private JsonObject handleGetBoard(HttpExchange exchange, String path) {
        String boardId = path.startsWith("/") ? path.substring(1) : path;
        LeaderboardCache cache = LeaderboardManager.getInstance().getBoard(boardId);
        if (cache == null) return error("No leaderboard board named '" + boardId + "'");

        int page = 1;
        String query = exchange.getRequestURI().getQuery();
        if (query != null) {
            for (String param : query.split("&")) {
                String[] kv = param.split("=", 2);
                if (kv.length == 2 && kv[0].equals("page")) {
                    try { page = Math.max(1, Integer.parseInt(kv[1])); } catch (NumberFormatException ignored) {}
                }
            }
        }

        JsonObject obj = new JsonObject();
        obj.addProperty("success", true);
        obj.addProperty("board", boardId);
        obj.addProperty("displayName", cache.getDefinition().displayName());
        obj.addProperty("page", page);
        obj.addProperty("totalPages", cache.getTotalPages(PAGE_SIZE));

        JsonArray entries = new JsonArray();
        int rank = (page - 1) * PAGE_SIZE + 1;
        for (var entry : cache.getPage(server, page, PAGE_SIZE)) {
            JsonObject e = new JsonObject();
            e.addProperty("rank", rank++);
            e.addProperty("name", entry.name());
            e.addProperty("value", cache.getProvider().formatValue(entry.value()));
            entries.add(e);
        }
        obj.add("entries", entries);
        return obj;
    }

    // ── POST /boards — create a custom board ─────────────────────────────────
    private JsonObject handlePost(String path, HttpExchange exchange) throws IOException {
        if (!path.equals("/boards") && !path.equals("/boards/")) return error("Unknown POST path: " + path);
        JsonObject body = readBody(exchange);
        if (body == null || !body.has("id") || !body.has("displayName")) {
            return error("Request body must contain 'id' and 'displayName'");
        }
        String id = body.get("id").getAsString();
        if (LeaderboardManager.getInstance().getBoard(id) != null) {
            return error("A board named '" + id + "' already exists");
        }
        String displayName = body.get("displayName").getAsString();
        LeaderboardConfigLoader.addCustomBoard(id, displayName);

        JsonObject obj = new JsonObject();
        obj.addProperty("success", true);
        obj.addProperty("message", "Custom board '" + id + "' created");
        return obj;
    }

    // ── PUT /boards/{id}/value — set a custom board's value for one player ──
    private JsonObject handlePut(String path, HttpExchange exchange) throws IOException {
        if (!path.startsWith("/boards/") || !path.endsWith("/value")) return error("Unknown PUT path: " + path);
        String id = path.substring("/boards/".length(), path.length() - "/value".length());
        if (!LeaderboardConfigLoader.isCustomBoard(id)) {
            return error("No custom board named '" + id + "' (only custom boards can have values set this way)");
        }

        JsonObject body = readBody(exchange);
        if (body == null || !body.has("player") || !body.has("value")) {
            return error("Request body must contain 'player' and 'value'");
        }
        Optional<UUID> uuid = resolveUuid(body.get("player").getAsString());
        if (uuid.isEmpty()) return error("Player '" + body.get("player").getAsString() + "' not found");

        long value = body.get("value").getAsLong();
        LeaderboardConfigLoader.customStats().set(id, uuid.get(), value);
        LeaderboardManager.getInstance().getBoard(id).invalidate();

        JsonObject obj = new JsonObject();
        obj.addProperty("success", true);
        obj.addProperty("message", "Set '" + id + "' = " + value);
        return obj;
    }

    // ── DELETE /boards/{id} ───────────────────────────────────────────────────
    private JsonObject handleDelete(String path) {
        if (!path.startsWith("/boards/")) return error("Unknown DELETE path: " + path);
        String id = path.substring("/boards/".length());
        if (!LeaderboardConfigLoader.deleteCustomBoard(id)) {
            return error("No custom board named '" + id + "' (only custom boards can be deleted this way)");
        }
        JsonObject obj = new JsonObject();
        obj.addProperty("success", true);
        obj.addProperty("message", "Board '" + id + "' deleted");
        return obj;
    }

    // ── Helpers ────────────────────────────────────────────────────────────────
    private Optional<UUID> resolveUuid(String name) {
        var online = server.getPlayerList().getPlayerByName(name);
        if (online != null) return Optional.of(online.getUUID());
        try {
            return server.services().nameToIdCache().get(name).map(p -> p.id());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static JsonObject error(String message) {
        JsonObject o = new JsonObject();
        o.addProperty("success", false);
        o.addProperty("error", message);
        return o;
    }

    private JsonObject readBody(HttpExchange exchange) {
        try (InputStream is = exchange.getRequestBody()) {
            byte[] bytes = is.readAllBytes();
            if (bytes.length == 0) return null;
            return JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception e) {
            LOGGER.warn("Failed to parse request body: {}", e.getMessage());
            return null;
        }
    }

    private void sendResponse(HttpExchange exchange, int status, JsonObject body) throws IOException {
        byte[] bytes = GSON.toJson(body).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
