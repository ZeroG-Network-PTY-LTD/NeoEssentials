package com.zerog.neoessentials.webdashboard.endpoints;

import com.google.gson.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import com.zerog.neoessentials.sidebar.ScoreboardBoard;
import com.zerog.neoessentials.sidebar.ScoreboardLine;
import com.zerog.neoessentials.sidebar.ScoreboardManager;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Dashboard API endpoint for the sidebar scoreboard system.
 *
 * <pre>
 * GET    /api/scoreboard                       – full overview (enabled, boards, overrides)
 * GET    /api/scoreboard/boards/{name}          – single board detail
 * PUT    /api/scoreboard/enabled                { enabled }
 * POST   /api/scoreboard/boards                 { name, priority, conditions[], title, lines[] } – create/update
 * DELETE /api/scoreboard/boards/{name}
 * PUT    /api/scoreboard/boards/{name}/line/{index}  { text }
 * </pre>
 *
 * Every write round-trips through {@link ScoreboardManager#saveConfig()} so dashboard edits
 * persist to {@code scoreboard.json} exactly like the in-game commands already do.
 */
public class ScoreboardEndpoint implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScoreboardEndpoint.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final MinecraftServer server;

    public ScoreboardEndpoint(MinecraftServer server) {
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
        String path = exchange.getRequestURI().getPath().replaceFirst("^/api/scoreboard", "");

        NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "ScoreboardEndpoint request: {} {}", method, path);

        // Same convention as MotdEndpoint/PermissionEndpoint: reads are open to any
        // authenticated caller, writes require ADMIN.
        if (!"GET".equals(method) && !Boolean.TRUE.equals(exchange.getAttribute("auth-admin"))) {
            sendResponse(exchange, 403, error("Admin access required"));
            return;
        }

        try {
            JsonObject response;
            int statusCode = 200;
            switch (method) {
                case "GET" -> response = handleGet(path);
                case "POST" -> response = handlePost(path, exchange);
                case "PUT" -> response = handlePut(path, exchange);
                case "DELETE" -> response = handleDelete(path);
                default -> { response = error("Method not allowed"); statusCode = 405; }
            }
            if (statusCode == 200 && response.has("success") && !response.get("success").getAsBoolean()) {
                statusCode = 400;
            }
            sendResponse(exchange, statusCode, response);
        } catch (Exception e) {
            LOGGER.error("Error handling scoreboard endpoint request to {}", exchange.getRequestURI(), e);
            sendResponse(exchange, 500, error("Internal server error: " + e.getMessage()));
        }
    }

    // ── GET ────────────────────────────────────────────────────────────────────
    private JsonObject handleGet(String path) {
        ScoreboardManager mgr = ScoreboardManager.getInstance();

        if (path.startsWith("/boards/")) {
            String name = path.substring("/boards/".length());
            ScoreboardBoard board = mgr.findBoard(name);
            if (board == null) return error("No board named '" + name + "'");
            JsonObject obj = new JsonObject();
            obj.addProperty("success", true);
            obj.add("board", boardJson(board));
            return obj;
        }

        JsonObject obj = new JsonObject();
        obj.addProperty("success", true);
        obj.addProperty("enabled", mgr.isEnabled());
        obj.addProperty("refreshInterval", mgr.getRefreshIntervalTicks());
        JsonArray boards = new JsonArray();
        for (String name : mgr.getBoardNames()) {
            ScoreboardBoard board = mgr.findBoard(name);
            if (board != null) boards.add(boardJson(board));
        }
        obj.add("boards", boards);
        return obj;
    }

    // ── POST ───────────────────────────────────────────────────────────────────
    private JsonObject handlePost(String path, HttpExchange exchange) throws IOException {
        if (!path.equals("/boards") && !path.equals("/boards/")) return error("Unknown POST path: " + path);

        JsonObject body = readBody(exchange);
        if (body == null || !body.has("name")) return error("Request body must contain 'name'");

        String name = body.get("name").getAsString();
        int priority = body.has("priority") ? body.get("priority").getAsInt() : 0;

        List<String> conditions = new ArrayList<>();
        if (body.has("conditions") && body.get("conditions").isJsonArray()) {
            for (JsonElement e : body.getAsJsonArray("conditions")) conditions.add(e.getAsString());
        }

        List<String> titleFrames = body.has("title") ? framesFromJson(body.get("title")) : List.of("");

        List<ScoreboardLine> lines = new ArrayList<>();
        if (body.has("lines") && body.get("lines").isJsonArray()) {
            for (JsonElement lineEl : body.getAsJsonArray("lines")) {
                if (!lineEl.isJsonObject()) continue;
                JsonObject lo = lineEl.getAsJsonObject();
                List<String> frames = lo.has("text") ? framesFromJson(lo.get("text")) : List.of("");
                String condition = lo.has("condition") && !lo.get("condition").isJsonNull()
                    ? lo.get("condition").getAsString() : null;
                lines.add(new ScoreboardLine(frames, condition));
            }
        }

        ScoreboardManager.getInstance().addOrUpdateBoard(new ScoreboardBoard(name, priority, conditions, titleFrames, lines));
        ScoreboardManager.getInstance().saveConfig();
        pushUpdate();

        JsonObject obj = new JsonObject();
        obj.addProperty("success", true);
        obj.addProperty("message", "Board '" + name + "' saved");
        return obj;
    }

    // ── PUT ────────────────────────────────────────────────────────────────────
    private JsonObject handlePut(String path, HttpExchange exchange) throws IOException {
        if (path.equals("/enabled") || path.equals("/enabled/")) {
            JsonObject body = readBody(exchange);
            if (body == null || !body.has("enabled")) return error("Request body must contain 'enabled'");
            boolean enabled = body.get("enabled").getAsBoolean();
            ScoreboardManager.getInstance().setEnabled(enabled);
            if (!enabled) ScoreboardManager.getInstance().hideAll(server);
            else pushUpdate();
            JsonObject obj = new JsonObject();
            obj.addProperty("success", true);
            obj.addProperty("enabled", enabled);
            return obj;
        }

        // PUT /boards/{name}/line/{index}
        if (path.startsWith("/boards/")) {
            String rest = path.substring("/boards/".length());
            String[] parts = rest.split("/line/");
            if (parts.length == 2) {
                String name = parts[0];
                int index;
                try {
                    index = Integer.parseInt(parts[1]);
                } catch (NumberFormatException e) {
                    return error("Invalid line index");
                }
                ScoreboardBoard board = ScoreboardManager.getInstance().findBoard(name);
                if (board == null) return error("No board named '" + name + "'");
                if (index < 0 || index >= board.getLines().size()) return error("Line index out of range");

                JsonObject body = readBody(exchange);
                if (body == null || !body.has("text")) return error("Request body must contain 'text'");
                String text = body.get("text").getAsString();
                board.getLines().set(index, new ScoreboardLine(List.of(text), board.getLines().get(index).getCondition()));
                ScoreboardManager.getInstance().saveConfig();
                pushUpdate();

                JsonObject obj = new JsonObject();
                obj.addProperty("success", true);
                obj.addProperty("message", "Line " + index + " of '" + name + "' updated");
                return obj;
            }
        }

        return error("Unknown PUT path: " + path);
    }

    // ── DELETE ─────────────────────────────────────────────────────────────────
    private JsonObject handleDelete(String path) {
        if (path.startsWith("/boards/")) {
            String name = path.substring("/boards/".length());
            if (name.isEmpty()) return error("Board name is required");
            boolean removed = ScoreboardManager.getInstance().removeBoard(name);
            if (!removed) return error("No board named '" + name + "'");
            ScoreboardManager.getInstance().saveConfig();
            pushUpdate();
            JsonObject obj = new JsonObject();
            obj.addProperty("success", true);
            obj.addProperty("message", "Board '" + name + "' deleted");
            return obj;
        }
        return error("Unknown DELETE path: " + path);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────
    private void pushUpdate() {
        if (server == null) return;
        server.execute(() -> ScoreboardManager.getInstance().updateAll(server));
    }

    private static JsonObject boardJson(ScoreboardBoard b) {
        JsonObject o = new JsonObject();
        o.addProperty("name", b.getName());
        o.addProperty("priority", b.getPriority());
        JsonArray conditions = new JsonArray();
        for (String c : b.getConditions()) conditions.add(c);
        o.add("conditions", conditions);
        o.add("title", framesJson(b.getTitleFrames()));
        JsonArray lines = new JsonArray();
        for (ScoreboardLine line : b.getLines()) {
            JsonObject lo = new JsonObject();
            lo.add("text", framesJson(line.getFrames()));
            if (line.getCondition() != null) lo.addProperty("condition", line.getCondition());
            lines.add(lo);
        }
        o.add("lines", lines);
        return o;
    }

    private static JsonElement framesJson(List<String> frames) {
        if (frames.size() == 1) return new JsonPrimitive(frames.get(0));
        JsonArray arr = new JsonArray();
        for (String f : frames) arr.add(f);
        return arr;
    }

    private static List<String> framesFromJson(JsonElement el) {
        List<String> frames = new ArrayList<>();
        if (el.isJsonArray()) {
            for (JsonElement e : el.getAsJsonArray()) frames.add(e.getAsString());
        } else {
            frames.add(el.getAsString());
        }
        return frames;
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
