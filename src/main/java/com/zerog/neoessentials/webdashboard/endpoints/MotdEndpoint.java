package com.zerog.neoessentials.webdashboard.endpoints;

import com.google.gson.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.zerog.neoessentials.util.motd.MotdManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Dashboard API endpoint for MOTD management.
 *
 * <pre>
 * GET    /api/motd              – list all profiles + rotation settings
 * GET    /api/motd/active       – get active profile
 * POST   /api/motd/profiles     – create / update a profile  { name, motd [, author] }
 * DELETE /api/motd/profiles/{name} – delete a profile
 * PUT    /api/motd/active       – switch active profile       { name }
 * PUT    /api/motd/rotation     – update rotation settings    { enabled, intervalMinutes }
 * POST   /api/motd/rotation/next – rotate immediately
 * POST   /api/motd/broadcast    – broadcast active MOTD to all online players
 * </pre>
 */
public class MotdEndpoint implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(MotdEndpoint.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final MinecraftServer server;

    public MotdEndpoint(MinecraftServer server) {
        this.server = server;
    }

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
        // Strip /api/motd prefix
        String path = exchange.getRequestURI().getPath().replaceFirst("^/api/motd", "");

        try {
            JsonObject response;
            int statusCode = 200;

            switch (method) {
                case "GET":
                    response = handleGet(path);
                    break;
                case "POST":
                    response = handlePost(path, exchange);
                    break;
                case "PUT":
                    response = handlePut(path, exchange);
                    break;
                case "DELETE":
                    response = handleDelete(path);
                    break;
                default:
                    response = error("Method not allowed");
                    statusCode = 405;
            }

            // Derive status from response "success" field if not already set
            if (statusCode == 200 && response.has("success") && !response.get("success").getAsBoolean()) {
                statusCode = 400;
            }

            sendResponse(exchange, statusCode, response);
        } catch (Exception e) {
            LOGGER.error("Error handling MOTD endpoint request to {}", exchange.getRequestURI(), e);
            sendResponse(exchange, 500, error("Internal server error: " + e.getMessage()));
        }
    }

    // ── GET ────────────────────────────────────────────────────────────────────

    private JsonObject handleGet(String path) {
        MotdManager mgr = MotdManager.getInstance();

        if (path.equals("/active") || path.equals("/active/")) {
            return activeProfileJson(mgr);
        }

        // Default: full overview
        JsonObject obj = new JsonObject();
        obj.addProperty("success", true);
        obj.addProperty("activeProfile", mgr.getActiveProfileName());

        // Rotation settings
        JsonObject rot = new JsonObject();
        rot.addProperty("enabled", mgr.isRotationEnabled());
        rot.addProperty("intervalMinutes", mgr.getRotationIntervalMinutes());
        obj.add("rotation", rot);

        // All profiles
        JsonObject profiles = new JsonObject();
        for (Map.Entry<String, MotdManager.MotdProfile> entry : mgr.getProfiles().entrySet()) {
            profiles.add(entry.getKey(), profileToJson(entry.getValue()));
        }
        obj.add("profiles", profiles);

        return obj;
    }

    private JsonObject activeProfileJson(MotdManager mgr) {
        MotdManager.MotdProfile p = mgr.getActiveProfile();
        if (p == null) return error("No active profile found");
        JsonObject obj = new JsonObject();
        obj.addProperty("success", true);
        obj.addProperty("activeProfile", mgr.getActiveProfileName());
        obj.add("profile", profileToJson(p));
        return obj;
    }

    // ── POST ───────────────────────────────────────────────────────────────────

    private JsonObject handlePost(String path, HttpExchange exchange) throws IOException {
        MotdManager mgr = MotdManager.getInstance();

        if (path.equals("/broadcast") || path.equals("/broadcast/")) {
            return doBroadcast();
        }

        if (path.equals("/rotation/next") || path.equals("/rotation/next/")) {
            mgr.rotateNext();
            JsonObject obj = new JsonObject();
            obj.addProperty("success", true);
            obj.addProperty("activeProfile", mgr.getActiveProfileName());
            obj.addProperty("message", "Rotated to profile '" + mgr.getActiveProfileName() + "'");
            return obj;
        }

        if (path.equals("/profiles") || path.equals("/profiles/")) {
            JsonObject body = readBody(exchange);
            if (body == null || !body.has("name") || !body.has("motd")) {
                return error("Request body must contain 'name' and 'motd'");
            }
            String name   = body.get("name").getAsString();
            String motd   = body.get("motd").getAsString();
            String author = body.has("author") ? body.get("author").getAsString() : "Dashboard";

            String saveError = mgr.setProfile(name, motd, author);
            if (saveError != null) return error(saveError);

            JsonObject obj = new JsonObject();
            obj.addProperty("success", true);
            obj.addProperty("message", "Profile '" + name.toLowerCase() + "' saved");
            obj.add("profile", profileToJson(mgr.getProfiles().get(name.toLowerCase())));
            return obj;
        }

        return error("Unknown POST path: " + path);
    }

    // ── PUT ────────────────────────────────────────────────────────────────────

    private JsonObject handlePut(String path, HttpExchange exchange) throws IOException {
        MotdManager mgr = MotdManager.getInstance();

        if (path.equals("/active") || path.equals("/active/")) {
            JsonObject body = readBody(exchange);
            if (body == null || !body.has("name")) {
                return error("Request body must contain 'name'");
            }
            String switchError = mgr.switchProfile(body.get("name").getAsString());
            if (switchError != null) return error(switchError);

            JsonObject obj = new JsonObject();
            obj.addProperty("success", true);
            obj.addProperty("activeProfile", mgr.getActiveProfileName());
            obj.addProperty("message", "Active profile switched to '" + mgr.getActiveProfileName() + "'");
            return obj;
        }

        if (path.equals("/rotation") || path.equals("/rotation/")) {
            JsonObject body = readBody(exchange);
            if (body == null || !body.has("enabled")) {
                return error("Request body must contain 'enabled'");
            }
            boolean enabled  = body.get("enabled").getAsBoolean();
            int intervalMins = body.has("intervalMinutes") ? body.get("intervalMinutes").getAsInt() : 60;

            String rotError = mgr.setRotation(enabled, intervalMins);
            if (rotError != null) return error(rotError);

            JsonObject obj = new JsonObject();
            obj.addProperty("success", true);
            obj.addProperty("rotationEnabled", enabled);
            obj.addProperty("intervalMinutes", intervalMins);
            obj.addProperty("message", enabled
                    ? "Rotation enabled every " + intervalMins + " minutes"
                    : "Rotation disabled");
            return obj;
        }

        return error("Unknown PUT path: " + path);
    }

    // ── DELETE ─────────────────────────────────────────────────────────────────

    private JsonObject handleDelete(String path) {
        // DELETE /api/motd/profiles/{name}
        if (path.startsWith("/profiles/")) {
            String name = path.substring("/profiles/".length());
            if (name.isEmpty()) return error("Profile name is required");

            String deleteError = MotdManager.getInstance().deleteProfile(name);
            if (deleteError != null) return error(deleteError);

            JsonObject obj = new JsonObject();
            obj.addProperty("success", true);
            obj.addProperty("message", "Profile '" + name.toLowerCase() + "' deleted");
            return obj;
        }
        return error("Unknown DELETE path: " + path);
    }

    // ── Broadcast ──────────────────────────────────────────────────────────────

    private JsonObject doBroadcast() {
        MotdManager mgr = MotdManager.getInstance();
        if (!mgr.hasMotd()) return error("No MOTD is set on the active profile");

        if (server == null) return error("Server is not available");

        MotdManager.MotdProfile p = mgr.getActiveProfile();
        net.minecraft.network.chat.Component motdComp =
                net.minecraft.network.chat.Component.literal(p.motd.replace("&", "§"));

        int sent = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§6[MOTD]"));
            player.sendSystemMessage(motdComp);
            sent++;
        }

        JsonObject obj = new JsonObject();
        obj.addProperty("success", true);
        obj.addProperty("playersNotified", sent);
        obj.addProperty("message", "MOTD broadcast to " + sent + " players");
        return obj;
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private static JsonObject profileToJson(MotdManager.MotdProfile p) {
        JsonObject o = new JsonObject();
        o.addProperty("name",      p.name);
        o.addProperty("motd",      p.motd);
        o.addProperty("author",    p.author);
        o.addProperty("timestamp", p.timestamp);
        return o;
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

