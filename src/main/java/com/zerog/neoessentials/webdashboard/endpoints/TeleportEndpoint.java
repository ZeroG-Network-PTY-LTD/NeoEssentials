package com.zerog.neoessentials.webdashboard.endpoints;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.zerog.neoessentials.config.ConfigManager;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * REST API endpoint for reading and updating teleportation settings via the dashboard.
 *
 * Endpoints:
 * - GET  /api/teleport/settings — Return all teleport settings (delays, cooldowns, safety flags).
 * - PUT  /api/teleport/settings — Update teleport settings in config.json and reload managers.
 */
public class TeleportEndpoint implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(TeleportEndpoint.class);
    private final MinecraftServer server;

    public TeleportEndpoint(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        try {
            if (path.equals("/api/teleport/settings")) {
                if ("GET".equals(method)) {
                    handleGetSettings(exchange);
                } else if ("PUT".equals(method) || "POST".equals(method)) {
                    // Mutates server-wide teleport config (cooldowns, safety checks, etc.) —
                    // had no role check at all previously (only DashboardAPI.withAuth ran,
                    // which merely verifies a valid session, not its role), so any
                    // authenticated session could disable safety checks / cooldowns server-wide.
                    if (!Boolean.TRUE.equals(exchange.getAttribute("auth-admin"))) {
                        sendResponse(exchange, 403, "{\"success\":false,\"error\":\"Admin access required\"}");
                        return;
                    }
                    handlePutSettings(exchange);
                } else {
                    sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                }
            } else {
                sendResponse(exchange, 404, "{\"error\":\"Endpoint not found\"}");
            }
        } catch (Exception e) {
            LOGGER.error("Error handling teleport settings request: {} {}", method, path, e);
            try {
                String err = String.format("{\"success\":false,\"error\":\"%s\"}",
                        e.getMessage() != null ? e.getMessage().replace("\"", "'") : e.getClass().getSimpleName());
                sendResponse(exchange, 500, err);
            } catch (IOException ex) {
                LOGGER.error("Failed to send error response", ex);
            }
        }
    }

    // ── GET /api/teleport/settings ───────────────────────────────────────────

    private void handleGetSettings(HttpExchange exchange) throws IOException {
        JsonObject response = new JsonObject();
        response.addProperty("success", true);

        try {
            ConfigManager cfg = ConfigManager.getInstance();
            JsonObject config = cfg.getConfig(ConfigManager.MAIN_CONFIG);

            // generalSettings
            JsonObject general = new JsonObject();
            int teleportDelay = 3;
            boolean enableWarmup = true;
            boolean cancelOnMove = true;
            boolean cancelOnDamage = true;
            int maxDistance = 0;

            if (config.has("teleportation")) {
                JsonObject tp = config.getAsJsonObject("teleportation");
                if (tp.has("generalSettings")) {
                    JsonObject gs = tp.getAsJsonObject("generalSettings");
                    if (gs.has("teleportDelay")) teleportDelay = gs.get("teleportDelay").getAsInt();
                    if (gs.has("enableTeleportWarmup")) enableWarmup = gs.get("enableTeleportWarmup").getAsBoolean();
                    if (gs.has("cancelTeleportOnMove")) cancelOnMove = gs.get("cancelTeleportOnMove").getAsBoolean();
                    if (gs.has("cancelTeleportOnDamage")) cancelOnDamage = gs.get("cancelTeleportOnDamage").getAsBoolean();
                    if (gs.has("maxTeleportDistance")) maxDistance = gs.get("maxTeleportDistance").getAsInt();
                }
            }
            general.addProperty("teleportDelay", teleportDelay);
            general.addProperty("enableTeleportWarmup", enableWarmup);
            general.addProperty("cancelTeleportOnMove", cancelOnMove);
            general.addProperty("cancelTeleportOnDamage", cancelOnDamage);
            general.addProperty("maxTeleportDistance", maxDistance);
            response.add("generalSettings", general);

            // homeSettings
            JsonObject home = new JsonObject();
            int homeSetCooldown = 0, homeTpCooldown = 0, homeDelCooldown = 0, maxHomes = 5;
            boolean homeSafety = true, crossDimension = true;
            if (config.has("teleportation")) {
                JsonObject tp = config.getAsJsonObject("teleportation");
                if (tp.has("homeSettings")) {
                    JsonObject hs = tp.getAsJsonObject("homeSettings");
                    if (hs.has("homeSetCooldown")) homeSetCooldown = hs.get("homeSetCooldown").getAsInt();
                    if (hs.has("homeTeleportCooldown")) homeTpCooldown = hs.get("homeTeleportCooldown").getAsInt();
                    if (hs.has("homeDeleteCooldown")) homeDelCooldown = hs.get("homeDeleteCooldown").getAsInt();
                    if (hs.has("maxHomes")) maxHomes = hs.get("maxHomes").getAsInt();
                    if (hs.has("enableHomeTeleportSafety")) homeSafety = hs.get("enableHomeTeleportSafety").getAsBoolean();
                    else if (hs.has("enableHomeSafety")) homeSafety = hs.get("enableHomeSafety").getAsBoolean();
                    if (hs.has("allowCrossDimensionHomes")) crossDimension = hs.get("allowCrossDimensionHomes").getAsBoolean();
                }
            }
            home.addProperty("homeSetCooldown", homeSetCooldown);
            home.addProperty("homeTeleportCooldown", homeTpCooldown);
            home.addProperty("homeDeleteCooldown", homeDelCooldown);
            home.addProperty("maxHomes", maxHomes);
            home.addProperty("enableHomeSafety", homeSafety);
            home.addProperty("allowCrossDimensionHomes", crossDimension);
            response.add("homeSettings", home);

            // warpSettings
            JsonObject warp = new JsonObject();
            int warpSetCooldown = 0, warpUseCooldown = 0, maxWarps = 50;
            boolean warpSafety = true;
            if (config.has("teleportation")) {
                JsonObject tp = config.getAsJsonObject("teleportation");
                if (tp.has("warpSettings")) {
                    JsonObject ws = tp.getAsJsonObject("warpSettings");
                    if (ws.has("warpSetCooldown")) warpSetCooldown = ws.get("warpSetCooldown").getAsInt();
                    if (ws.has("warpCooldown")) warpUseCooldown = ws.get("warpCooldown").getAsInt();
                    if (ws.has("maxWarps")) maxWarps = ws.get("maxWarps").getAsInt();
                    if (ws.has("enableWarpSafety")) warpSafety = ws.get("enableWarpSafety").getAsBoolean();
                }
            }
            warp.addProperty("warpSetCooldown", warpSetCooldown);
            warp.addProperty("warpCooldown", warpUseCooldown);
            warp.addProperty("maxWarps", maxWarps);
            warp.addProperty("enableWarpSafety", warpSafety);
            response.add("warpSettings", warp);

            // spawnSettings
            JsonObject spawn = new JsonObject();
            int spawnCooldown = 0;
            boolean spawnSafety = true;
            if (config.has("teleportation")) {
                JsonObject tp = config.getAsJsonObject("teleportation");
                if (tp.has("spawnSettings")) {
                    JsonObject ss = tp.getAsJsonObject("spawnSettings");
                    if (ss.has("spawnCooldown")) spawnCooldown = ss.get("spawnCooldown").getAsInt();
                    if (ss.has("enableSpawnSafety")) spawnSafety = ss.get("enableSpawnSafety").getAsBoolean();
                }
            }
            spawn.addProperty("spawnCooldown", spawnCooldown);
            spawn.addProperty("enableSpawnSafety", spawnSafety);
            response.add("spawnSettings", spawn);

            // backSettings / miscSettings
            JsonObject back = new JsonObject();
            int backCooldown = 0, backDelay = cfg.getBackTeleportDelay();
            boolean deathBack = cfg.isDeathBackEnabled(), teleportBack = cfg.isTeleportBackEnabled();
            if (config.has("teleportation")) {
                JsonObject tp = config.getAsJsonObject("teleportation");
                if (tp.has("miscSettings")) {
                    JsonObject ms = tp.getAsJsonObject("miscSettings");
                    if (ms.has("backCooldown")) backCooldown = ms.get("backCooldown").getAsInt();
                }
            }
            back.addProperty("backCooldown", backCooldown);
            back.addProperty("teleportDelay", backDelay);
            back.addProperty("enableDeathBack", deathBack);
            back.addProperty("enableTeleportBack", teleportBack);
            response.add("backSettings", back);

        } catch (Exception e) {
            LOGGER.error("Error reading teleport settings", e);
            response.addProperty("success", false);
            response.addProperty("error", "Failed to read teleport settings: " + e.getMessage());
        }

        sendResponse(exchange, 200, response.toString());
    }

    // ── PUT /api/teleport/settings ───────────────────────────────────────────

    private void handlePutSettings(HttpExchange exchange) throws IOException {
        // Read request body
        String body;
        try (InputStream is = exchange.getRequestBody()) {
            body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
        if (body == null || body.isBlank()) {
            sendResponse(exchange, 400, "{\"success\":false,\"error\":\"Empty request body\"}");
            return;
        }

        JsonObject updates;
        try {
            updates = JsonParser.parseString(body).getAsJsonObject();
        } catch (Exception e) {
            sendResponse(exchange, 400, "{\"success\":false,\"error\":\"Invalid JSON: " + e.getMessage() + "\"}");
            return;
        }

        try {
            ConfigManager cfg = ConfigManager.getInstance();
            JsonObject config = cfg.getConfig(ConfigManager.MAIN_CONFIG);

            // Ensure teleportation section exists
            if (!config.has("teleportation")) {
                config.add("teleportation", new JsonObject());
            }
            JsonObject tp = config.getAsJsonObject("teleportation");

            // ── generalSettings ──────────────────────────────────────────────
            if (updates.has("generalSettings")) {
                if (!tp.has("generalSettings")) tp.add("generalSettings", new JsonObject());
                JsonObject gs = tp.getAsJsonObject("generalSettings");
                JsonObject gsIn = updates.getAsJsonObject("generalSettings");
                applyIntIfPresent(gsIn, gs, "teleportDelay", 0, 300);
                applyBoolIfPresent(gsIn, gs, "enableTeleportWarmup");
                applyBoolIfPresent(gsIn, gs, "cancelTeleportOnMove");
                applyBoolIfPresent(gsIn, gs, "cancelTeleportOnDamage");
                applyIntIfPresent(gsIn, gs, "maxTeleportDistance", 0, Integer.MAX_VALUE);
            }

            // ── homeSettings ─────────────────────────────────────────────────
            if (updates.has("homeSettings")) {
                if (!tp.has("homeSettings")) tp.add("homeSettings", new JsonObject());
                JsonObject hs = tp.getAsJsonObject("homeSettings");
                JsonObject hsIn = updates.getAsJsonObject("homeSettings");
                applyIntIfPresent(hsIn, hs, "homeSetCooldown", 0, 86400);
                applyIntIfPresent(hsIn, hs, "homeTeleportCooldown", 0, 86400);
                applyIntIfPresent(hsIn, hs, "homeDeleteCooldown", 0, 86400);
                applyIntIfPresent(hsIn, hs, "maxHomes", 1, 1000);
                applyBoolIfPresent(hsIn, hs, "enableHomeSafety");
                applyBoolIfPresent(hsIn, hs, "enableHomeTeleportSafety");
                applyBoolIfPresent(hsIn, hs, "allowCrossDimensionHomes");
            }

            // ── warpSettings ─────────────────────────────────────────────────
            if (updates.has("warpSettings")) {
                if (!tp.has("warpSettings")) tp.add("warpSettings", new JsonObject());
                JsonObject ws = tp.getAsJsonObject("warpSettings");
                JsonObject wsIn = updates.getAsJsonObject("warpSettings");
                applyIntIfPresent(wsIn, ws, "warpSetCooldown", 0, 86400);
                applyIntIfPresent(wsIn, ws, "warpCooldown", 0, 86400);
                applyIntIfPresent(wsIn, ws, "maxWarps", 1, 10000);
                applyBoolIfPresent(wsIn, ws, "enableWarpSafety");
            }

            // ── spawnSettings ────────────────────────────────────────────────
            if (updates.has("spawnSettings")) {
                if (!tp.has("spawnSettings")) tp.add("spawnSettings", new JsonObject());
                JsonObject ss = tp.getAsJsonObject("spawnSettings");
                JsonObject ssIn = updates.getAsJsonObject("spawnSettings");
                applyIntIfPresent(ssIn, ss, "spawnCooldown", 0, 86400);
                applyBoolIfPresent(ssIn, ss, "enableSpawnSafety");
            }

            // ── miscSettings (back) ──────────────────────────────────────────
            if (updates.has("backSettings")) {
                if (!tp.has("miscSettings")) tp.add("miscSettings", new JsonObject());
                JsonObject ms = tp.getAsJsonObject("miscSettings");
                JsonObject bsIn = updates.getAsJsonObject("backSettings");
                applyIntIfPresent(bsIn, ms, "backCooldown", 0, 86400);
                applyBoolIfPresent(bsIn, ms, "enableDeathBack");
                applyBoolIfPresent(bsIn, ms, "enableTeleportBack");
                // backSettings teleportDelay goes into backSettings sub-object
                if (!tp.has("backSettings")) tp.add("backSettings", new JsonObject());
                JsonObject backSettingsCfg = tp.getAsJsonObject("backSettings");
                applyIntIfPresent(bsIn, backSettingsCfg, "teleportDelay", 0, 300);
            }

            // Save updated config
            cfg.saveConfig(ConfigManager.MAIN_CONFIG, config);
            LOGGER.info("Teleport settings updated via dashboard");

            // Reload all teleport managers so changes take effect immediately
            reloadTeleportManagers();

            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("message", "Teleport settings saved and managers reloaded.");
            sendResponse(exchange, 200, response.toString());

        } catch (Exception e) {
            LOGGER.error("Error updating teleport settings", e);
            sendResponse(exchange, 500, "{\"success\":false,\"error\":\"Failed to save settings: " + e.getMessage() + "\"}");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void applyIntIfPresent(JsonObject source, JsonObject target, String key, int min, int max) {
        if (source.has(key)) {
            try {
                int val = source.get(key).getAsInt();
                val = Math.max(min, Math.min(max, val));
                target.addProperty(key, val);
            } catch (Exception ignored) {}
        }
    }

    private void applyBoolIfPresent(JsonObject source, JsonObject target, String key) {
        if (source.has(key)) {
            try {
                target.addProperty(key, source.get(key).getAsBoolean());
            } catch (Exception ignored) {}
        }
    }

    private void reloadTeleportManagers() {
        try {
            com.zerog.neoessentials.teleportation.HomeManager.getInstance().reload();
        } catch (Exception e) {
            LOGGER.warn("HomeManager reload failed: {}", e.getMessage());
        }
        try {
            com.zerog.neoessentials.teleportation.Warp.WarpManager.getInstance().reload();
        } catch (Exception e) {
            LOGGER.warn("WarpManager reload failed: {}", e.getMessage());
        }
        try {
            com.zerog.neoessentials.teleportation.Spawn.SpawnManager.getInstance().reload();
        } catch (Exception e) {
            LOGGER.warn("SpawnManager reload failed: {}", e.getMessage());
        }
        try {
            com.zerog.neoessentials.teleportation.Misc.MiscTeleportManager.getInstance().reload();
        } catch (Exception e) {
            LOGGER.warn("MiscTeleportManager reload failed: {}", e.getMessage());
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}

