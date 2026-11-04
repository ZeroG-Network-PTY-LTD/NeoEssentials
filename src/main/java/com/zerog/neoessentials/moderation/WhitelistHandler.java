package com.zerog.neoessentials.moderation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * REST API handler for whitelist management.
 *
 * Used to live combined with ban management as ModerationHandler, dead/unregistered code
 * with no auth check at all. Its ban routes fully duplicated what the already-registered
 * ModerationEndpoint does against the same ModerationManager backing store - rather than
 * resurrect a second, divergence-prone copy of that, this keeps only what ModerationEndpoint
 * never covered (whitelist) and mounts it at its own path instead of overlapping
 * ModerationEndpoint's /api/moderation.
 */
public class WhitelistHandler implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(WhitelistHandler.class);
    private final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Whitelist CRUD against the real ModerationManager backing store - gate it the same
        // way every other admin-only dashboard endpoint does, checked here rather than only
        // trusting the withAuth(...) wrapper at registration time.
        if (!isAdmin(exchange)) {
            sendErrorResponse(exchange, 403, "Admin permission required");
            return;
        }

        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        try {
            switch (method) {
                case "GET" -> handleGet(exchange, path);
                case "POST" -> handlePost(exchange, path);
                case "DELETE" -> handleDelete(exchange, path);
                default -> sendErrorResponse(exchange, 405, "Method not allowed");
            }
        } catch (Exception e) {
            LOGGER.error("Error handling whitelist request", e);
            sendErrorResponse(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }

    private void handleGet(HttpExchange exchange, String path) throws IOException {
        String[] parts = path.split("/");

        // GET /api/whitelist - List whitelist
        if (parts.length == 3) {
            handleListWhitelist(exchange);
        }
        // GET /api/whitelist/status - Get whitelist status
        else if (parts.length == 4 && parts[3].equals("status")) {
            handleGetWhitelistStatus(exchange);
        }
        else {
            sendErrorResponse(exchange, 404, "Endpoint not found");
        }
    }

    private void handlePost(HttpExchange exchange, String path) throws IOException {
        String[] parts = path.split("/");

        // POST /api/whitelist - Add whitelist entry
        if (parts.length == 3) {
            handleAddWhitelist(exchange);
        }
        // POST /api/whitelist/import - Bulk import whitelist
        else if (parts.length == 4 && parts[3].equals("import")) {
            handleImportWhitelist(exchange);
        }
        // POST /api/whitelist/toggle - Enable/disable whitelist
        else if (parts.length == 4 && parts[3].equals("toggle")) {
            handleToggleWhitelist(exchange);
        }
        else {
            sendErrorResponse(exchange, 404, "Endpoint not found");
        }
    }

    private void handleDelete(HttpExchange exchange, String path) throws IOException {
        String[] parts = path.split("/");

        // DELETE /api/whitelist/{id} - Remove whitelist entry
        if (parts.length == 4) {
            String entryId = parts[3];
            handleRemoveWhitelist(exchange, entryId);
        }
        else {
            sendErrorResponse(exchange, 404, "Endpoint not found");
        }
    }

    private void handleListWhitelist(HttpExchange exchange) throws IOException {
        Collection<WhitelistEntry> entries = ModerationManager.getInstance().getAllWhitelist();

        JsonObject response = new JsonObject();
        response.addProperty("entryCount", entries.size());
        response.addProperty("whitelistEnabled", ModerationManager.getInstance().isWhitelistEnabled());

        JsonArray entriesArray = new JsonArray();
        for (WhitelistEntry entry : entries) {
            entriesArray.add(whitelistToJson(entry));
        }
        response.add("entries", entriesArray);

        sendJsonResponse(exchange, 200, response);
    }

    private void handleGetWhitelistStatus(HttpExchange exchange) throws IOException {
        JsonObject response = new JsonObject();
        response.addProperty("enabled", ModerationManager.getInstance().isWhitelistEnabled());
        response.addProperty("entryCount", ModerationManager.getInstance().getAllWhitelist().size());
        sendJsonResponse(exchange, 200, response);
    }

    private void handleAddWhitelist(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        JsonObject data = gson.fromJson(body, JsonObject.class);

        if (!data.has("type") || !data.has("target")) {
            sendErrorResponse(exchange, 400, "Missing required fields: type, target");
            return;
        }

        WhitelistEntry.WhitelistType type = WhitelistEntry.WhitelistType.valueOf(data.get("type").getAsString());
        String target = data.get("target").getAsString();
        String playerName = data.has("playerName") ? data.get("playerName").getAsString() : null;
        String notes = data.has("notes") ? data.get("notes").getAsString() : null;
        String addedBy = getUsername(exchange);

        WhitelistEntry entry = ModerationManager.getInstance().addWhitelist(type, target, playerName, addedBy, notes);

        JsonObject response = whitelistToJson(entry);
        sendJsonResponse(exchange, 201, response);
    }

    private void handleRemoveWhitelist(HttpExchange exchange, String entryId) throws IOException {
        boolean removed = ModerationManager.getInstance().removeWhitelist(entryId);
        if (!removed) {
            sendErrorResponse(exchange, 404, "Whitelist entry not found");
            return;
        }

        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.addProperty("message", "Whitelist entry removed");
        sendJsonResponse(exchange, 200, response);
    }

    private void handleImportWhitelist(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        JsonObject data = gson.fromJson(body, JsonObject.class);

        if (!data.has("entries")) {
            sendErrorResponse(exchange, 400, "Missing required field: entries");
            return;
        }

        List<WhitelistEntry> entries = new ArrayList<>();
        JsonArray entriesArray = data.getAsJsonArray("entries");
        for (int i = 0; i < entriesArray.size(); i++) {
            JsonObject entryObj = entriesArray.get(i).getAsJsonObject();
            WhitelistEntry entry = new WhitelistEntry();
            entry.setType(WhitelistEntry.WhitelistType.valueOf(entryObj.get("type").getAsString()));
            entry.setTarget(entryObj.get("target").getAsString());
            if (entryObj.has("playerName")) {
                entry.setPlayerName(entryObj.get("playerName").getAsString());
            }
            if (entryObj.has("notes")) {
                entry.setNotes(entryObj.get("notes").getAsString());
            }
            entries.add(entry);
        }

        String importedBy = getUsername(exchange);
        int imported = ModerationManager.getInstance().importWhitelist(entries, importedBy);

        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.addProperty("imported", imported);
        sendJsonResponse(exchange, 200, response);
    }

    private void handleToggleWhitelist(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        JsonObject data = gson.fromJson(body, JsonObject.class);

        if (!data.has("enabled")) {
            sendErrorResponse(exchange, 400, "Missing required field: enabled");
            return;
        }

        boolean enabled = data.get("enabled").getAsBoolean();
        ModerationManager.getInstance().setWhitelistEnabled(enabled);

        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.addProperty("enabled", enabled);
        sendJsonResponse(exchange, 200, response);
    }

    private JsonObject whitelistToJson(WhitelistEntry entry) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", entry.getId());
        obj.addProperty("type", entry.getType().name());
        obj.addProperty("target", entry.getTarget());
        obj.addProperty("playerName", entry.getPlayerName());
        obj.addProperty("addedBy", entry.getAddedBy());
        obj.addProperty("addedAt", entry.getAddedAt() != null ? entry.getAddedAt().toString() : null);
        obj.addProperty("notes", entry.getNotes());
        return obj;
    }

    /**
     * The old ModerationHandler's getUsernameFromSession() never actually read the real
     * session - it just returned the literal string "admin" whenever any sessionId cookie
     * existed, so every whitelist audit field (addedBy, etc.) would have recorded the wrong
     * name no matter who was logged in. auth-username is the real one, set by the dashboard's
     * auth middleware (withAuth(...)) - see DashboardAPI and every other endpoint's isAdmin().
     */
    private String getUsername(HttpExchange exchange) {
        Object username = exchange.getAttribute("auth-username");
        return username != null ? username.toString() : "unknown";
    }

    /** Set by the dashboard's auth middleware (withAuth(...)) when registered - see DashboardAPI. */
    private boolean isAdmin(HttpExchange exchange) {
        return Boolean.TRUE.equals(exchange.getAttribute("auth-admin"));
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, JsonObject data) throws IOException {
        String response = gson.toJson(data);
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, bytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void sendErrorResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        JsonObject error = new JsonObject();
        error.addProperty("error", message);
        error.addProperty("timestamp", System.currentTimeMillis());
        sendJsonResponse(exchange, statusCode, error);
    }
}
