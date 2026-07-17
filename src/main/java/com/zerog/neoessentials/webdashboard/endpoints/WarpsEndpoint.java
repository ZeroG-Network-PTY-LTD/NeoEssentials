package com.zerog.neoessentials.webdashboard.endpoints;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.zerog.neoessentials.teleportation.TeleportLocation;
import com.zerog.neoessentials.teleportation.Warp.WarpManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Dashboard API endpoint for public warp management.
 *
 * <pre>
 * GET    /api/warps          – list all warps (name + location)
 * GET    /api/warps/{name}   – get a single warp's location
 * POST   /api/warps          – create a warp  { "name": "...", "world": "...", "x", "y", "z", "yaw"?, "pitch"? }
 * DELETE /api/warps/{name}   – delete a warp
 * </pre>
 */
@SuppressWarnings("unused") // Used by DashboardAPI
public class WarpsEndpoint implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(WarpsEndpoint.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Authorization, Content-Type");
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        String method = exchange.getRequestMethod().toUpperCase();
        String path = exchange.getRequestURI().getPath().replaceFirst("^/api/warps", "");
        if (path.isEmpty()) path = "/";

        // Creating/deleting warps requires ADMIN, matching every other config-writing endpoint
        // group. Reads stay open to any authenticated caller.
        if (!"GET".equals(method) && !Boolean.TRUE.equals(exchange.getAttribute("auth-admin"))) {
            sendResponse(exchange, 403, error("Admin access required"));
            return;
        }

        try {
            JsonObject response;
            int statusCode = 200;

            switch (method) {
                case "GET"    -> response = handleGet(path);
                case "POST"   -> response = handlePost(exchange);
                case "DELETE" -> response = handleDelete(path);
                default       -> { response = error("Method not allowed"); statusCode = 405; }
            }

            if (statusCode == 200 && response.has("success") && !response.get("success").getAsBoolean()) {
                statusCode = 400;
            }
            sendResponse(exchange, statusCode, response);
        } catch (Exception e) {
            LOGGER.error("Error handling warps endpoint request to {}", exchange.getRequestURI(), e);
            sendResponse(exchange, 500, error("Internal server error: " + e.getMessage()));
        }
    }

    // ── GET ────────────────────────────────────────────────────────────────────

    private JsonObject handleGet(String path) {
        WarpManager manager = WarpManager.getInstance();
        String name = path.replaceAll("^/|/$", "");

        if (name.isEmpty()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("success", true);
            JsonArray arr = new JsonArray();
            for (String warpName : manager.getWarpNames()) {
                TeleportLocation loc = manager.getWarp(warpName);
                if (loc == null) continue;
                JsonObject w = loc.toJson();
                w.addProperty("name", warpName);
                arr.add(w);
            }
            obj.add("warps", arr);
            obj.addProperty("count", arr.size());
            return obj;
        }

        TeleportLocation loc = manager.getWarp(name);
        if (loc == null) return error("Warp '" + name + "' not found");

        JsonObject obj = loc.toJson();
        obj.addProperty("success", true);
        obj.addProperty("name", name);
        return obj;
    }

    // ── POST ───────────────────────────────────────────────────────────────────

    private JsonObject handlePost(HttpExchange exchange) throws IOException {
        JsonObject body = readBody(exchange);
        if (body == null || !body.has("name") || !body.has("world")
                || !body.has("x") || !body.has("y") || !body.has("z")) {
            return error("Request body must contain 'name', 'world', 'x', 'y', 'z'");
        }

        String name = body.get("name").getAsString().trim();
        if (name.isEmpty()) return error("Warp name must not be empty");

        String world = body.get("world").getAsString();
        double x = body.get("x").getAsDouble();
        double y = body.get("y").getAsDouble();
        double z = body.get("z").getAsDouble();
        float yaw = body.has("yaw") ? body.get("yaw").getAsFloat() : 0.0f;
        float pitch = body.has("pitch") ? body.get("pitch").getAsFloat() : 0.0f;

        TeleportLocation location = new TeleportLocation(world, x, y, z, yaw, pitch, "Dashboard");
        String failureReason = WarpManager.getInstance().createWarpByAdmin(name, location, "Dashboard");
        if (failureReason != null) {
            return error("Failed to create warp: " + failureReason);
        }

        JsonObject obj = new JsonObject();
        obj.addProperty("success", true);
        obj.addProperty("message", "Warp '" + name + "' created");
        return obj;
    }

    // ── DELETE ─────────────────────────────────────────────────────────────────

    private JsonObject handleDelete(String path) {
        String name = path.replaceAll("^/|/$", "");
        if (name.isEmpty()) return error("Path must be /api/warps/{name}");

        boolean removed = WarpManager.getInstance().deleteWarpByAdmin(name, "Dashboard");
        if (!removed) return error("Warp '" + name + "' not found");

        JsonObject obj = new JsonObject();
        obj.addProperty("success", true);
        obj.addProperty("message", "Warp '" + name + "' deleted");
        return obj;
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

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
