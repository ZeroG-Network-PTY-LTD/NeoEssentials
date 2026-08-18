package com.zerog.neoessentials.webdashboard.endpoints;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.zerog.neoessentials.economy.managers.EconomyManager;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.NameAndId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

/**
 * Admin economy actions, keyed by player username.
 * Read-only leaderboard/stats live at {@code /api/stats/economy}.
 *
 * <pre>
 * GET  /api/economy/{username}   – current balance
 * POST /api/economy/{username}   – adjust balance {action: give|take|set, amount}
 * </pre>
 */
public class EconomyEndpoint implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(EconomyEndpoint.class);
    private final MinecraftServer server;

    public EconomyEndpoint(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Authorization, Content-Type");
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        String method = exchange.getRequestMethod().toUpperCase();
        String path = exchange.getRequestURI().getPath().replaceFirst("^/api/economy/?", "");

        NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "EconomyEndpoint request: {} target='{}'", method, path);

        try {
            if (path.isEmpty()) {
                sendJson(exchange, 400, error("Path must be /api/economy/{username}"));
                return;
            }

            UUID uuid = resolveUuid(path);
            if (uuid == null) {
                NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Economy request for unknown player '{}'", path);
                sendJson(exchange, 404, error("Player '" + path + "' not found"));
                return;
            }

            switch (method) {
                case "GET" -> handleGet(exchange, path, uuid);
                case "POST" -> handlePost(exchange, path, uuid);
                default -> sendJson(exchange, 405, error("Method not allowed"));
            }
        } catch (Exception e) {
            LOGGER.error("Error handling economy endpoint request to {}", exchange.getRequestURI(), e);
            sendJson(exchange, 500, error("Internal server error: " + e.getMessage()));
        }
    }

    private void handleGet(HttpExchange exchange, String username, UUID uuid) throws IOException {
        BigDecimal balance = EconomyManager.getInstance().getBalance(uuid);
        JsonObject obj = new JsonObject();
        obj.addProperty("success", true);
        obj.addProperty("username", username);
        obj.addProperty("uuid", uuid.toString());
        obj.addProperty("balance", balance.setScale(2, RoundingMode.HALF_UP).toPlainString());
        sendJson(exchange, 200, obj);
    }

    private void handlePost(HttpExchange exchange, String username, UUID uuid) throws IOException {
        if (!isAdmin(exchange)) {
            sendJson(exchange, 403, error("Admin permission required"));
            return;
        }

        JsonObject body = readBody(exchange);
        if (body == null || !body.has("action") || !body.has("amount")) {
            sendJson(exchange, 400, error("Body must contain 'action' (give|take|set) and 'amount'"));
            return;
        }

        String action = body.get("action").getAsString().toLowerCase();
        BigDecimal amount;
        try {
            amount = BigDecimal.valueOf(body.get("amount").getAsDouble());
        } catch (Exception e) {
            NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Economy adjustment rejected: 'amount' not numeric for player '{}'", username);
            sendJson(exchange, 400, error("'amount' must be a number"));
            return;
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            sendJson(exchange, 400, error("'amount' must not be negative"));
            return;
        }

        if (!"give".equals(action) && !"take".equals(action) && !"set".equals(action)) {
            sendJson(exchange, 400, error("'action' must be 'give', 'take', or 'set'"));
            return;
        }

        EconomyManager em = EconomyManager.getInstance();
        boolean success;
        try {
            // addBalance/subtractBalance/setBalance post cancellable economy events and mutate
            // shared balance state that main-thread listeners (scoreboards, other mods) may not
            // expect to observe off-thread. Marshal onto the main thread, same as
            // ModerationEndpoint's vanish/jail fixes.
            success = server.submit(() -> switch (action) {
                case "give" -> em.addBalance(uuid, amount);
                case "take" -> em.subtractBalance(uuid, amount);
                default -> { em.setBalance(uuid, amount); yield true; }
            }).get();
        } catch (Exception e) {
            LOGGER.error("Error applying economy adjustment on main thread for player '{}'", username, e);
            sendJson(exchange, 500, error("Internal server error: " + e.getMessage()));
            return;
        }

        if (!success) {
            NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Economy adjustment rejected for player '{}': action={} amount={}", username, action, amount);
            sendJson(exchange, 400, error("Balance adjustment rejected (insufficient funds or over limit)"));
            return;
        }

        NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Economy adjustment applied for player '{}': action={} amount={}", username, action, amount);
        JsonObject obj = new JsonObject();
        obj.addProperty("success", true);
        obj.addProperty("username", username);
        obj.addProperty("newBalance", em.getBalance(uuid).setScale(2, RoundingMode.HALF_UP).toPlainString());
        sendJson(exchange, 200, obj);
    }

    /** Resolve a path segment that may be a raw UUID or a username to a UUID. */
    private UUID resolveUuid(String usernameOrUuid) {
        try {
            return UUID.fromString(usernameOrUuid);
        } catch (IllegalArgumentException ignored) {
            // Not a UUID — fall through to username resolution
        }

        var online = server.getPlayerList().getPlayerByName(usernameOrUuid);
        if (online != null) return online.getUUID();

        var cache = server.services().nameToIdCache();
        if (cache == null) return null;
        Optional<NameAndId> profile = cache.get(usernameOrUuid);
        return profile.map(NameAndId::id).orElse(null);
    }

    private boolean isAdmin(HttpExchange exchange) {
        return Boolean.TRUE.equals(exchange.getAttribute("auth-admin"));
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

    private static JsonObject error(String message) {
        JsonObject o = new JsonObject();
        o.addProperty("success", false);
        o.addProperty("error", message);
        return o;
    }

    private void sendJson(HttpExchange exchange, int status, JsonObject body) throws IOException {
        byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
