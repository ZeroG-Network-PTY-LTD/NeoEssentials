package com.zerog.neoessentials.webdashboard.endpoints;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import com.zerog.neoessentials.teleportation.TeleportLocation;
import com.zerog.neoessentials.teleportation.Warp.WarpManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.UserNameToIdResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Dashboard API endpoint for public and player warp management.
 *
 * <pre>
 * GET    /api/warps                        – list all server warps (name + location)
 * GET    /api/warps/{name}                 – get a single server warp's location
 * POST   /api/warps                        – create a server warp  { "name": "...", "world": "...", "x", "y", "z", "yaw"?, "pitch"? }
 * DELETE /api/warps/{name}                 – delete a server warp
 * GET    /api/warps/players                – list every player's warps (admin only)
 * GET    /api/warps/players/{uuid}         – one player's warps (admin only)
 * DELETE /api/warps/players/{uuid}/{name}  – delete one player's warp (admin only)
 * </pre>
 */
@SuppressWarnings("unused") // Used by DashboardAPI
public class WarpsEndpoint implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(WarpsEndpoint.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final MinecraftServer server;

    public WarpsEndpoint(MinecraftServer server) {
        this.server = server;
    }

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

        NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "WarpsEndpoint request: {} {}", method, path);

        boolean isAdmin = Boolean.TRUE.equals(exchange.getAttribute("auth-admin"));
        boolean isPlayerWarpsPath = path.equals("/players") || path.startsWith("/players/");

        // Creating/deleting server warps requires ADMIN, matching every other config-writing
        // endpoint group; server-warp reads stay open to any authenticated caller since server
        // warps are meant to be publicly discoverable (that's the point of /warps in-game).
        //
        // Player warps are personal, not public, so — unlike server warps — EVERY player-warp
        // route requires admin, including GET: listing every player's private warp locations
        // needs the same gate as deleting them.
        if (isPlayerWarpsPath) {
            if (!isAdmin) {
                sendResponse(exchange, 403, error("Admin access required"));
                return;
            }
        } else if (!"GET".equals(method) && !isAdmin) {
            sendResponse(exchange, 403, error("Admin access required"));
            return;
        }

        try {
            JsonObject response;
            int statusCode = 200;

            if (isPlayerWarpsPath) {
                switch (method) {
                    // GET resolves player names via server.getProfileCache()/getPlayerList(),
                    // which (like every other endpoint touching MinecraftServer state — see
                    // ServerEndpoint's docblock) must run on the main server thread, not this
                    // HTTP worker thread. Off-thread access can silently hang under real
                    // concurrent player activity (no exception, no response ever sent) rather
                    // than throwing — a 10s timeout here turns that into a clean error instead
                    // of an indefinitely stuck connection.
                    case "GET" -> {
                        String finalPath = path;
                        try {
                            response = CompletableFuture.supplyAsync(() -> handlePlayerWarpsGet(finalPath), server)
                                .get(10, TimeUnit.SECONDS);
                        } catch (TimeoutException e) {
                            response = error("Timed out resolving player warps — server may be overloaded");
                        } catch (ExecutionException e) {
                            throw (e.getCause() instanceof Exception ex) ? ex : e;
                        }
                    }
                    case "DELETE" -> response = handlePlayerWarpsDelete(path);
                    default       -> { response = error("Method not allowed"); statusCode = 405; }
                }
                if (statusCode == 200 && response.has("success") && !response.get("success").getAsBoolean()) {
                    statusCode = 400;
                }
                sendResponse(exchange, statusCode, response);
                return;
            }

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

    // ── Player warps (GET) ──────────────────────────────────────────────────────

    private JsonObject handlePlayerWarpsGet(String path) {
        WarpManager manager = WarpManager.getInstance();
        // path is "/players", "/players/", or "/players/{uuid}"
        String remainder = path.replaceFirst("^/players/?", "").replaceAll("^/|/$", "");

        if (remainder.isEmpty()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("success", true);
            JsonArray playersArr = new JsonArray();
            int totalWarps = 0;
            for (Map.Entry<UUID, Map<String, TeleportLocation>> entry : manager.getAllPlayerWarps().entrySet()) {
                Map<String, TeleportLocation> warps = entry.getValue();
                if (warps.isEmpty()) continue;
                playersArr.add(playerWarpGroupJson(entry.getKey(), warps));
                totalWarps += warps.size();
            }
            obj.add("players", playersArr);
            obj.addProperty("totalPlayers", playersArr.size());
            obj.addProperty("totalWarps", totalWarps);
            return obj;
        }

        UUID uuid;
        try {
            uuid = UUID.fromString(remainder);
        } catch (IllegalArgumentException e) {
            return error("Invalid UUID: " + remainder);
        }

        Map<String, TeleportLocation> warps = manager.getPlayerWarpsRaw(uuid);
        if (warps.isEmpty()) return error("No warps found for player " + uuid);

        JsonObject obj = playerWarpGroupJson(uuid, warps);
        obj.addProperty("success", true);
        return obj;
    }

    private JsonObject playerWarpGroupJson(UUID uuid, Map<String, TeleportLocation> warps) {
        JsonObject obj = new JsonObject();
        obj.addProperty("uuid", uuid.toString());
        obj.addProperty("name", resolveUsername(uuid));
        JsonArray warpsArr = new JsonArray();
        for (Map.Entry<String, TeleportLocation> w : warps.entrySet()) {
            JsonObject wj = w.getValue().toJson();
            wj.addProperty("name", w.getKey());
            warpsArr.add(wj);
        }
        obj.add("warps", warpsArr);
        obj.addProperty("warpCount", warpsArr.size());
        return obj;
    }

    // ── Player warps (DELETE) ───────────────────────────────────────────────────

    private JsonObject handlePlayerWarpsDelete(String path) {
        // path is "/players/{uuid}/{name}"
        String remainder = path.replaceFirst("^/players/?", "").replaceAll("^/|/$", "");
        int slash = remainder.indexOf('/');
        if (slash < 0) return error("Path must be /api/warps/players/{uuid}/{name}");

        String uuidStr = remainder.substring(0, slash);
        String warpName = remainder.substring(slash + 1);
        if (warpName.isEmpty()) return error("Path must be /api/warps/players/{uuid}/{name}");

        UUID uuid;
        try {
            uuid = UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            return error("Invalid UUID: " + uuidStr);
        }

        boolean removed = WarpManager.getInstance().deletePlayerWarpByAdmin(uuid, warpName);
        if (!removed) return error("Warp '" + warpName + "' not found for player " + uuid);

        JsonObject obj = new JsonObject();
        obj.addProperty("success", true);
        obj.addProperty("message", "Warp '" + warpName + "' deleted for player " + uuid);
        return obj;
    }

    private String resolveUsername(UUID uuid) {
        if (server != null) {
            try {
                UserNameToIdResolver cache = server.services().nameToIdCache();
                if (cache != null) {
                    var opt = cache.get(uuid);
                    if (opt.isPresent() && opt.get().name() != null) return opt.get().name();
                }
            } catch (Exception e) {
                NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Failed to resolve username for uuid={}", uuid, e);
            }
            var player = server.getPlayerList().getPlayer(uuid);
            if (player != null) return player.getName().getString();
        }
        return uuid.toString().substring(0, 8) + "…";
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
            NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Warp creation failed for '{}': {}", name, failureReason);
            return error("Failed to create warp: " + failureReason);
        }
        NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Warp created: '{}'", name);

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
        NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Warp deleted: '{}'", name);

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
