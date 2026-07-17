package com.zerog.neoessentials.webdashboard.endpoints;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.zerog.neoessentials.webdashboard.security.ApiKeyManager;
import com.zerog.neoessentials.webdashboard.security.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * REST management for API keys (see {@link ApiKeyManager}) — the same create/list/revoke
 * capability as the in-game {@code /apikey} command, for an already-authenticated admin to
 * rotate/add keys without console access. ADMIN role required for every route (both session
 * and API-key auth qualify, so an existing key can be used to mint a replacement for itself
 * before being revoked).
 *
 *   GET    /api/apikeys            – list keys (id/label/role/enabled/lastUsedAt — never the secret)
 *   POST   /api/apikeys            – create {label, role?} -> {token} shown ONCE
 *   DELETE /api/apikeys/{id}       – revoke
 */
public class ApiKeyEndpoint implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiKeyEndpoint.class);

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");

        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        Boolean admin = (Boolean) exchange.getAttribute("auth-admin");
        if (!Boolean.TRUE.equals(admin)) {
            sendJson(exchange, 403, "{\"success\":false,\"error\":\"Admin access required\"}");
            return;
        }

        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        try {
            if ("GET".equals(method) && path.equals("/api/apikeys")) {
                handleList(exchange);
            } else if ("POST".equals(method) && path.equals("/api/apikeys")) {
                handleCreate(exchange);
            } else if ("DELETE".equals(method) && path.startsWith("/api/apikeys/")) {
                handleRevoke(exchange, path.substring("/api/apikeys/".length()));
            } else {
                sendJson(exchange, 404, "{\"success\":false,\"error\":\"Unknown endpoint\"}");
            }
        } catch (Exception e) {
            LOGGER.error("Error in ApiKeyEndpoint", e);
            sendJson(exchange, 500, "{\"success\":false,\"error\":\"Internal server error: " + escape(e.getMessage()) + "\"}");
        }
    }

    private void handleList(HttpExchange exchange) throws IOException {
        JsonArray arr = new JsonArray();
        for (ApiKeyManager.ApiKeyRecord record : ApiKeyManager.getInstance().getAllKeys()) {
            arr.add(record.toJson());
        }
        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.add("keys", arr);
        sendJson(exchange, 200, response.toString());
    }

    private void handleCreate(HttpExchange exchange) throws IOException {
        String body;
        try (InputStream is = exchange.getRequestBody()) {
            body = new String(is.readAllBytes(), StandardCharsets.UTF_8).trim();
        }

        JsonObject data = body.isEmpty() ? new JsonObject() : JsonParser.parseString(body).getAsJsonObject();
        if (!data.has("label") || data.get("label").getAsString().isBlank()) {
            sendJson(exchange, 400, "{\"success\":false,\"error\":\"Missing required field: label\"}");
            return;
        }

        String label = data.get("label").getAsString();
        User.Role role = User.Role.ADMIN;
        if (data.has("role")) {
            try {
                role = User.Role.valueOf(data.get("role").getAsString().toUpperCase());
            } catch (IllegalArgumentException e) {
                sendJson(exchange, 400, "{\"success\":false,\"error\":\"Invalid role — expected ADMIN, OPERATOR, MODERATOR, or VIEWER\"}");
                return;
            }
        }

        String token = ApiKeyManager.getInstance().createKey(label, role);

        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.addProperty("token", token);
        response.addProperty("message", "Store this token now — it will not be shown again.");
        sendJson(exchange, 201, response.toString());
    }

    private void handleRevoke(HttpExchange exchange, String id) throws IOException {
        boolean removed = ApiKeyManager.getInstance().revoke(id);
        if (removed) {
            sendJson(exchange, 200, "{\"success\":true,\"message\":\"API key revoked\"}");
        } else {
            sendJson(exchange, 404, "{\"success\":false,\"error\":\"No API key found with that id\"}");
        }
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void sendJson(HttpExchange exchange, int code, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
    }
}
