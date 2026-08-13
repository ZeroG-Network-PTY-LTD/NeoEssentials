package com.zerog.neoessentials.webdashboard.api.endpoints;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import net.minecraft.server.MinecraftServer;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Handles all admin-related API endpoints for dashboard
 * Provides server control functionality: restart, stop, reload, etc.
 *
 * Endpoints:
 * - POST /api/admin/restart - Restart the server
 * - POST /api/admin/stop - Stop the server
 * - POST /api/admin/reload - Reload all configs
 * - GET /api/admin/status - Get admin capabilities status
 */
public class AdminEndpoint implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminEndpoint.class);
    private final MinecraftServer server;

    public AdminEndpoint(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public void handle(HttpExchange exchange) {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "AdminEndpoint handling request: {} {}", method, path);

        try {
            // Every action here can halt/restart the server, reload config, or broadcast as
            // the server — all of it is admin-only. This endpoint previously had NO role
            // check at all (only DashboardAPI.withAuth ran, which merely verifies a valid
            // session exists, not its role), so any authenticated dashboard account —
            // including a self-registered default VIEWER via /dashboardregister — could stop
            // or restart the server. /status is read-only and left open to any session.
            if (!path.equals("/api/admin/status")) {
                requireAdmin(exchange);
            }
            if (path.equals("/api/admin/status") && "GET".equals(method)) {
                handleGetStatus(exchange);
            } else if (path.equals("/api/admin/restart") && "POST".equals(method)) {
                handleRestart(exchange);
            } else if (path.equals("/api/admin/stop") && "POST".equals(method)) {
                handleStop(exchange);
            } else if (path.equals("/api/admin/reload") && "POST".equals(method)) {
                handleReload(exchange);
            } else if (path.equals("/api/admin/save") && "POST".equals(method)) {
                handleSaveAll(exchange);
            } else if (path.equals("/api/admin/broadcast") && "POST".equals(method)) {
                handleBroadcast(exchange);
            } else {
                sendResponse(exchange, 404, "{\"error\":\"Endpoint not found\"}");
            }
        } catch (SecurityException e) {
            try {
                sendResponse(exchange, 403, "{\"success\":false,\"error\":\"Admin access required\"}");
            } catch (IOException ex) {
                NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "Failed to send error response", ex);
            }
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "Error handling admin request: {} {}", method, path, e);
            try {
                String errorResponse = String.format("{\"success\":false,\"error\":\"Internal error: %s\"}",
                    e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
                sendResponse(exchange, 500, errorResponse);
            } catch (IOException ex) {
                NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "Failed to send error response", ex);
            }
        }
    }

    private void requireAdmin(HttpExchange exchange) {
        if (!Boolean.TRUE.equals(exchange.getAttribute("auth-admin"))) {
            throw new SecurityException("Admin required");
        }
    }

    /**
     * GET /api/admin/status - Get admin capabilities status
     */
    private void handleGetStatus(HttpExchange exchange) throws IOException {
        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.addProperty("serverRunning", !server.isStopped());
        response.addProperty("canRestart", true);
        response.addProperty("canStop", true);
        response.addProperty("canReload", true);
        response.addProperty("canSave", true);

        sendResponse(exchange, 200, response.toString());
    }

    /**
     * POST /api/admin/restart - Restart the server
     */
    private void handleRestart(HttpExchange exchange) throws IOException {
        NeoLog.warn(LOGGER, LogCategory.WEB_DASHBOARD, "Server restart requested via dashboard");

        // Execute restart on server thread
        CompletableFuture<JsonObject> future = CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject response = new JsonObject();

                // Broadcast message to all players
                server.getPlayerList().getPlayers().forEach(player -> {
                    player.sendSystemMessage(MessageUtil.component("commands.neoessentials.admin.server_restarting"));
                });

                NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "Broadcasting restart message and scheduling restart in 5 seconds");

                // Schedule restart in 5 seconds
                new Thread(() -> {
                    try {
                        Thread.sleep(5000);

                        // Save all worlds
                        NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "Saving all worlds before restart...");
                        server.saveAllChunks(true, true, true);

                        // Stop server
                        NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "Stopping server for restart...");
                        server.halt(false);

                        // Note: Actual restart depends on how the server is launched
                        // Most server wrappers detect shutdown and restart automatically

                    } catch (InterruptedException e) {
                        NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "Restart interrupted", e);
                        Thread.currentThread().interrupt();
                    }
                }, "Dashboard-Restart").start();

                response.addProperty("success", true);
                response.addProperty("message", "Server restart initiated. Restarting in 5 seconds...");
                return response;

            } catch (Exception e) {
                NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "Error initiating restart", e);
                JsonObject error = new JsonObject();
                error.addProperty("success", false);
                error.addProperty("error", "Failed to restart: " + e.getMessage());
                return error;
            }
        }, server);

        try {
            JsonObject response = future.get(2, TimeUnit.SECONDS);
            sendResponse(exchange, 200, response.toString());
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "Timeout or error getting restart response", e);
            sendResponse(exchange, 500, "{\"success\":false,\"error\":\"Restart request timeout\"}");
        }
    }

    /**
     * POST /api/admin/stop - Stop the server
     */
    private void handleStop(HttpExchange exchange) throws IOException {
        NeoLog.warn(LOGGER, LogCategory.WEB_DASHBOARD, "Server stop requested via dashboard");

        // Execute stop on server thread
        CompletableFuture<JsonObject> future = CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject response = new JsonObject();

                // Broadcast message to all players
                server.getPlayerList().getPlayers().forEach(player -> {
                    player.sendSystemMessage(MessageUtil.component("commands.neoessentials.admin.server_shutting_down"));
                });

                NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "Broadcasting shutdown message and scheduling stop in 5 seconds");

                // Schedule stop in 5 seconds
                new Thread(() -> {
                    try {
                        Thread.sleep(5000);

                        // Save all worlds
                        NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "Saving all worlds before shutdown...");
                        server.saveAllChunks(true, true, true);

                        // Stop server
                        NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "Stopping server...");
                        server.halt(false);

                    } catch (InterruptedException e) {
                        NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "Stop interrupted", e);
                        Thread.currentThread().interrupt();
                    }
                }, "Dashboard-Stop").start();

                response.addProperty("success", true);
                response.addProperty("message", "Server shutdown initiated. Stopping in 5 seconds...");
                return response;

            } catch (Exception e) {
                NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "Error initiating stop", e);
                JsonObject error = new JsonObject();
                error.addProperty("success", false);
                error.addProperty("error", "Failed to stop: " + e.getMessage());
                return error;
            }
        }, server);

        try {
            JsonObject response = future.get(2, TimeUnit.SECONDS);
            sendResponse(exchange, 200, response.toString());
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "Timeout or error getting stop response", e);
            sendResponse(exchange, 500, "{\"success\":false,\"error\":\"Stop request timeout\"}");
        }
    }

    /**
     * POST /api/admin/reload - Reload all configs
     */
    private void handleReload(HttpExchange exchange) throws IOException {
        NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "Config reload requested via dashboard");

        // Execute reload on server thread
        CompletableFuture<JsonObject> future = CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject response = new JsonObject();
                int successCount = 0;
                int totalCount = 0;
                StringBuilder details = new StringBuilder();

                // Reload all configuration files
                totalCount++;
                try {
                    ConfigManager.loadAll();
                    NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "✓ Configuration files reloaded");
                    details.append("Configuration files: OK\n");
                    successCount++;
                } catch (Exception e) {
                    NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "✗ Failed to reload configuration files: {}", e.getMessage(), e);
                    details.append("Configuration files: FAILED (").append(e.getMessage()).append(")\n");
                }

                // Reload translations
                totalCount++;
                try {
                    com.zerog.neoessentials.util.MessageUtil.reloadTranslations();
                    NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "✓ Translations reloaded");
                    details.append("Translations: OK\n");
                    successCount++;
                } catch (Exception e) {
                    NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "✗ Failed to reload translations: {}", e.getMessage(), e);
                    details.append("Translations: FAILED (").append(e.getMessage()).append(")\n");
                }

                // Reload permissions
                totalCount++;
                try {
                    com.zerog.neoessentials.api.permissions.PermissionAPI.reload();
                    NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "✓ Permission system reloaded");
                    details.append("Permissions: OK\n");
                    successCount++;
                } catch (Exception e) {
                    NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "✗ Failed to reload permissions: {}", e.getMessage(), e);
                    details.append("Permissions: FAILED (").append(e.getMessage()).append(")\n");
                }

                // Build response
                boolean allSuccess = successCount == totalCount;
                response.addProperty("success", allSuccess);
                response.addProperty("successCount", successCount);
                response.addProperty("totalCount", totalCount);
                response.addProperty("message", String.format("Reload completed: %d/%d systems reloaded successfully", successCount, totalCount));
                response.addProperty("details", details.toString());

                NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "Dashboard reload completed: {}/{} systems", successCount, totalCount);
                return response;

            } catch (Exception e) {
                NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "Error executing reload", e);
                JsonObject error = new JsonObject();
                error.addProperty("success", false);
                error.addProperty("error", "Failed to reload: " + e.getMessage());
                return error;
            }
        }, server);

        try {
            JsonObject response = future.get(30, TimeUnit.SECONDS);
            sendResponse(exchange, 200, response.toString());
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "Timeout or error getting reload response", e);
            sendResponse(exchange, 500, "{\"success\":false,\"error\":\"Reload request timeout\"}");
        }
    }

    /**
     * POST /api/admin/broadcast - Send a message to all online players from the dashboard
     * Body: {"message": "Server maintenance in 10 minutes!"}
     */
    private void handleBroadcast(HttpExchange exchange) throws IOException {
        String bodyJson;
        try {
            bodyJson = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Could not read broadcast request body: {}", e.getMessage());
            sendResponse(exchange, 400, "{\"success\":false,\"error\":\"Could not read request body\"}");
            return;
        }

        String message;
        try {
            com.google.gson.JsonObject body = com.google.gson.JsonParser.parseString(bodyJson).getAsJsonObject();
            if (!body.has("message") || body.get("message").getAsString().isBlank()) {
                sendResponse(exchange, 400, "{\"success\":false,\"error\":\"'message' field is required\"}");
                return;
            }
            message = body.get("message").getAsString();
        } catch (Exception e) {
            NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Invalid broadcast request body: {}", e.getMessage());
            sendResponse(exchange, 400, "{\"success\":false,\"error\":\"Invalid JSON body\"}");
            return;
        }

        String senderName = "Dashboard";
        Object attr = exchange.getAttribute("auth-username");
        if (attr instanceof String s && !s.isBlank()) senderName = s;

        final String finalSender  = senderName;
        final String finalMessage = "§6[Dashboard §e" + senderName + "§6]§f " + message;

        CompletableFuture<JsonObject> future = CompletableFuture.supplyAsync(() -> {
            JsonObject resp = new JsonObject();
            try {
                int count = server.getPlayerList().getPlayers().size();
                server.getPlayerList().getPlayers().forEach(p ->
                    p.sendSystemMessage(net.minecraft.network.chat.Component.literal(finalMessage)));
                NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "[Dashboard Broadcast] {}: {}", finalSender, finalMessage);
                resp.addProperty("success", true);
                resp.addProperty("recipients", count);
                resp.addProperty("message", "Message sent to " + count + " player(s)");
            } catch (Exception e) {
                NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "Error sending dashboard broadcast", e);
                resp.addProperty("success", false);
                resp.addProperty("error", e.getMessage());
            }
            return resp;
        }, server);

        try {
            JsonObject result = future.get(8, TimeUnit.SECONDS);
            int code = result.has("success") && result.get("success").getAsBoolean() ? 200 : 500;
            sendResponse(exchange, code, result.toString());
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "Timeout or error getting broadcast response", e);
            sendResponse(exchange, 500, "{\"success\":false,\"error\":\"Timeout\"}");
        }
    }

    /**
     * POST /api/admin/save - Save all worlds
     */
    private void handleSaveAll(HttpExchange exchange) throws IOException {
        NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "Save all requested via dashboard");

        // Execute save on server thread
        CompletableFuture<JsonObject> future = CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject response = new JsonObject();

                // Save all chunks
                boolean saved = server.saveAllChunks(true, true, true);

                if (saved) {
                    response.addProperty("success", true);
                    response.addProperty("message", "All worlds saved successfully");
                    NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "Dashboard save-all completed successfully");
                } else {
                    response.addProperty("success", false);
                    response.addProperty("error", "Save operation returned false");
                    NeoLog.warn(LOGGER, LogCategory.WEB_DASHBOARD, "Dashboard save-all returned false");
                }

                return response;

            } catch (Exception e) {
                NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "Error executing save-all", e);
                JsonObject error = new JsonObject();
                error.addProperty("success", false);
                error.addProperty("error", "Failed to save: " + e.getMessage());
                return error;
            }
        }, server);

        try {
            JsonObject response = future.get(30, TimeUnit.SECONDS);
            sendResponse(exchange, 200, response.toString());
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "Timeout or error getting save response", e);
            sendResponse(exchange, 500, "{\"success\":false,\"error\":\"Save request timeout\"}");
        }
    }

    /**
     * Send JSON response
     */
    private void sendResponse(HttpExchange exchange, int statusCode, String jsonResponse) throws IOException {
        byte[] bytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}

