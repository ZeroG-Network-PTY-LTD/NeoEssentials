package com.zerog.neoessentials.webdashboard.endpoints;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.zerog.neoessentials.leaderboard.LeaderboardCache;
import com.zerog.neoessentials.leaderboard.LeaderboardManager;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Read-only dashboard API endpoint for leaderboards — deliberately thin (no write surface;
 * boards are stat sources registered in code, not admin-editable data).
 *
 * <pre>
 * GET /api/leaderboard              – list registered board ids + display names
 * GET /api/leaderboard/{board}?page=N – paginated entries for one board
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
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Authorization, Content-Type");
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, error("Method not allowed"));
            return;
        }

        String path = exchange.getRequestURI().getPath().replaceFirst("^/api/leaderboard", "");
        NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "LeaderboardEndpoint request: GET {}", path);

        try {
            JsonObject response = path.isEmpty() || path.equals("/") ? handleList() : handleBoard(exchange, path);
            int status = response.has("success") && !response.get("success").getAsBoolean() ? 400 : 200;
            sendResponse(exchange, status, response);
        } catch (Exception e) {
            LOGGER.error("Error handling leaderboard endpoint request to {}", exchange.getRequestURI(), e);
            sendResponse(exchange, 500, error("Internal server error: " + e.getMessage()));
        }
    }

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
            boards.add(b);
        }
        obj.add("boards", boards);
        return obj;
    }

    private JsonObject handleBoard(HttpExchange exchange, String path) {
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

    private static JsonObject error(String message) {
        JsonObject o = new JsonObject();
        o.addProperty("success", false);
        o.addProperty("error", message);
        return o;
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
