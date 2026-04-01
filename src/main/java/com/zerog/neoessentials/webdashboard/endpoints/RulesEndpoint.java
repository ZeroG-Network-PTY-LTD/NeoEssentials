package com.zerog.neoessentials.webdashboard.endpoints;

import com.google.gson.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.zerog.neoessentials.util.commands.RulesCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Dashboard API endpoint for server-rules management.
 *
 * <pre>
 * GET    /api/rules             – list all rules
 * POST   /api/rules             – replace all rules   { "rules": ["...", ...] }
 * POST   /api/rules/add         – append a rule       { "rule": "..." }
 * PUT    /api/rules/{number}    – edit rule by 1-based index { "rule": "..." }
 * DELETE /api/rules/{number}    – delete rule by 1-based index
 * POST   /api/rules/reload      – reload rules from disk
 * </pre>
 */
@SuppressWarnings({"unused", "RedundantThrows", "IfCanBeSwitch"}) // Used by DashboardAPI; throws kept for interface consistency
public class RulesEndpoint implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(RulesEndpoint.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // CORS pre-flight
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Authorization, Content-Type");
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        String method = exchange.getRequestMethod().toUpperCase();
        // Strip /api/rules prefix
        String path = exchange.getRequestURI().getPath().replaceFirst("^/api/rules", "");
        if (path.isEmpty()) path = "/";

        try {
            JsonObject response;
            int statusCode = 200;

            switch (method) {
                case "GET"    -> response = handleGet(path);
                case "POST"   -> response = handlePost(path, exchange);
                case "PUT"    -> response = handlePut(path, exchange);
                case "DELETE" -> response = handleDelete(path);
                default       -> { response = error("Method not allowed"); statusCode = 405; }
            }

            if (statusCode == 200 && response.has("success") && !response.get("success").getAsBoolean()) {
                statusCode = 400;
            }
            sendResponse(exchange, statusCode, response);
        } catch (Exception e) {
            LOGGER.error("Error handling rules endpoint request to {}", exchange.getRequestURI(), e);
            sendResponse(exchange, 500, error("Internal server error: " + e.getMessage()));
        }
    }

    // ── GET ────────────────────────────────────────────────────────────────────

    @SuppressWarnings("unused") // path kept for interface consistency with other endpoints
    private JsonObject handleGet(String path) {
        List<String> rules = RulesCommand.getRules();
        JsonObject obj = new JsonObject();
        obj.addProperty("success", true);
        obj.addProperty("count", rules.size());
        JsonArray arr = new JsonArray();
        for (int i = 0; i < rules.size(); i++) {
            JsonObject ruleObj = new JsonObject();
            ruleObj.addProperty("number", i + 1);
            ruleObj.addProperty("text", rules.get(i));
            arr.add(ruleObj);
        }
        obj.add("rules", arr);
        return obj;
    }

    // ── POST ───────────────────────────────────────────────────────────────────

    private JsonObject handlePost(String path, HttpExchange exchange) throws IOException {
        // POST /api/rules/reload – reload from disk
        if (path.equals("/reload") || path.equals("/reload/")) {
            RulesCommand.reload();
            List<String> rules = RulesCommand.getRules();
            JsonObject obj = new JsonObject();
            obj.addProperty("success", true);
            obj.addProperty("message", "Rules reloaded from disk");
            obj.addProperty("count", rules.size());
            return obj;
        }

        // POST /api/rules/add – append a single rule
        if (path.equals("/add") || path.equals("/add/")) {
            JsonObject body = readBody(exchange);
            if (body == null || !body.has("rule")) {
                return error("Request body must contain 'rule'");
            }
            String ruleText = body.get("rule").getAsString().trim();
            if (ruleText.isEmpty())  return error("Rule text must not be empty");
            if (ruleText.length() > 200) return error("Rule text exceeds 200 characters");

            List<String> rules = new ArrayList<>(RulesCommand.getRules());
            rules.add(ruleText);
            RulesCommand.setRules(rules);

            JsonObject obj = new JsonObject();
            obj.addProperty("success", true);
            obj.addProperty("number", rules.size());
            obj.addProperty("message", "Rule #" + rules.size() + " added");
            return obj;
        }

        // POST /api/rules – replace all rules
        if (path.equals("/") || path.isEmpty()) {
            JsonObject body = readBody(exchange);
            if (body == null || !body.has("rules") || !body.get("rules").isJsonArray()) {
                return error("Request body must contain 'rules' (JSON array of strings)");
            }
            List<String> newRules = new ArrayList<>();
            for (JsonElement el : body.getAsJsonArray("rules")) {
                String t = el.getAsString().trim();
                if (!t.isEmpty()) newRules.add(t);
            }
            RulesCommand.setRules(newRules);

            JsonObject obj = new JsonObject();
            obj.addProperty("success", true);
            obj.addProperty("count", newRules.size());
            obj.addProperty("message", newRules.size() + " rule(s) saved");
            return obj;
        }

        return error("Unknown POST path: " + path);
    }

    // ── PUT ────────────────────────────────────────────────────────────────────

    private JsonObject handlePut(String path, HttpExchange exchange) throws IOException {
        // PUT /api/rules/{number} – edit a rule by 1-based index
        int number = parseTrailingInt(path);
        if (number < 1) return error("Path must be /api/rules/{number} where number >= 1");

        List<String> rules = new ArrayList<>(RulesCommand.getRules());
        if (number > rules.size()) {
            return error("Rule #" + number + " does not exist (there are " + rules.size() + " rules)");
        }

        JsonObject body = readBody(exchange);
        if (body == null || !body.has("rule")) return error("Request body must contain 'rule'");
        String newText = body.get("rule").getAsString().trim();
        if (newText.isEmpty())      return error("Rule text must not be empty");
        if (newText.length() > 200) return error("Rule text exceeds 200 characters");

        rules.set(number - 1, newText);
        RulesCommand.setRules(rules);

        JsonObject obj = new JsonObject();
        obj.addProperty("success", true);
        obj.addProperty("number", number);
        obj.addProperty("message", "Rule #" + number + " updated");
        return obj;
    }

    // ── DELETE ─────────────────────────────────────────────────────────────────

    private JsonObject handleDelete(String path) {
        // DELETE /api/rules/{number}
        int number = parseTrailingInt(path);
        if (number < 1) return error("Path must be /api/rules/{number} where number >= 1");

        List<String> rules = new ArrayList<>(RulesCommand.getRules());
        if (number > rules.size()) {
            return error("Rule #" + number + " does not exist (there are " + rules.size() + " rules)");
        }

        rules.remove(number - 1);
        RulesCommand.setRules(rules);

        JsonObject obj = new JsonObject();
        obj.addProperty("success", true);
        obj.addProperty("message", "Rule #" + number + " deleted (" + rules.size() + " remaining)");
        return obj;
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /** Parse a trailing integer from a path like {@code "/42"} or {@code "/42/"}. Returns -1 on failure. */
    private int parseTrailingInt(String path) {
        try {
            String s = path.replaceAll("^/|/$", ""); // strip leading/trailing slashes
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return -1;
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



