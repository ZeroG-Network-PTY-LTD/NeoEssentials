package com.zerog.neoessentials.webdashboard.endpoints;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import com.zerog.neoessentials.webdashboard.backup.BackupManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * REST endpoint for server backup/restore via the dashboard.
 *
 * <pre>
 *   GET    /api/backup/status              — storage stats + available targets
 *   GET    /api/backup/list                — list all snapshots (newest first)
 *   GET    /api/backup/download?name=...   — download a snapshot ZIP
 *   POST   /api/backup/create              — {"name":"...", "targets":["configs","neodata",...]}
 *   POST   /api/backup/restore             — {"name":"..."}
 *   DELETE /api/backup/delete?name=...     — delete a snapshot
 * </pre>
 *
 * <p>All write operations are admin-only (checked via {@code auth-admin} attribute set by the
 * auth middleware in {@link com.zerog.neoessentials.webdashboard.DashboardAPI}).
 */
public class BackupEndpoint implements HttpHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackupEndpoint.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path   = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");

        NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "BackupEndpoint request: {} {}", method, path);

        try {
            if (path.endsWith("/status") && "GET".equals(method)) {
                handleStatus(exchange);
            } else if (path.endsWith("/list") && "GET".equals(method)) {
                handleList(exchange);
            } else if (path.contains("/download") && "GET".equals(method)) {
                requireAdmin(exchange);
                handleDownload(exchange);
            } else if (path.endsWith("/create") && "POST".equals(method)) {
                requireAdmin(exchange);
                handleCreate(exchange);
            } else if (path.endsWith("/restore") && "POST".equals(method)) {
                requireAdmin(exchange);
                handleRestore(exchange);
            } else if (path.endsWith("/delete") && "DELETE".equals(method)) {
                requireAdmin(exchange);
                handleDelete(exchange);
            } else {
                respond(exchange, 404, "{\"error\":\"Not found\"}");
            }
        } catch (SecurityException e) {
            NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "BackupEndpoint denied non-admin request: {} {}", method, path);
            respond(exchange, 403, "{\"error\":\"Admin access required\"}");
        } catch (IllegalArgumentException e) {
            NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "BackupEndpoint bad request: {} {}: {}", method, path, e.getMessage());
            respond(exchange, 400, json("error", e.getMessage()));
        } catch (IOException e) {
            LOGGER.error("[BackupEndpoint] Error handling {} {}: {}", method, path, e.getMessage(), e);
            respond(exchange, 500, json("error", e.getMessage() != null ? e.getMessage() : "Internal error"));
        }
    }

    // ── GET /api/backup/status ────────────────────────────────────────────────

    private void handleStatus(HttpExchange exchange) throws IOException {
        JsonObject status = BackupManager.getInstance().getStatus();
        respond(exchange, 200, GSON.toJson(status));
    }

    // ── GET /api/backup/list ──────────────────────────────────────────────────

    private void handleList(HttpExchange exchange) throws IOException {
        respond(exchange, 200, GSON.toJson(BackupManager.getInstance().listSnapshots()));
    }

    // ── GET /api/backup/download?name=... ────────────────────────────────────

    private void handleDownload(HttpExchange exchange) throws IOException {
        String name = queryParam(exchange, "name");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Missing 'name' parameter");

        // Auth tokens can also be passed as a query param for browser download links
        // (withAuth middleware already ran for header tokens; this is the fallback)
        Path zip = BackupManager.getInstance().getSnapshotPath(name);
        if (!Files.exists(zip)) {
            respond(exchange, 404, json("error", "Snapshot '" + name + "' not found"));
            return;
        }

        byte[] bytes = Files.readAllBytes(zip);
        exchange.getResponseHeaders().set("Content-Type",        "application/zip");
        exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"" + zip.getFileName() + "\"");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
    }

    // ── POST /api/backup/create ───────────────────────────────────────────────

    private void handleCreate(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        JsonObject req = com.zerog.neoessentials.webdashboard.util.RequestBodyUtil.parseJsonObject(body);

        String name = req.has("name") ? req.get("name").getAsString().trim() : "backup-" + System.currentTimeMillis();

        List<String> targets = new ArrayList<>();
        if (req.has("targets") && req.get("targets").isJsonArray()) {
            req.getAsJsonArray("targets").forEach(e -> targets.add(e.getAsString()));
        }

        LOGGER.info("[BackupEndpoint] Creating snapshot '{}' with targets: {}", name, targets);
        JsonObject result = BackupManager.getInstance().createSnapshot(name, targets);
        result.addProperty("success", true);
        respond(exchange, 200, GSON.toJson(result));
    }

    // ── POST /api/backup/restore ──────────────────────────────────────────────

    private void handleRestore(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        JsonObject req = com.zerog.neoessentials.webdashboard.util.RequestBodyUtil.parseJsonObject(body);

        String name = req.has("name") ? req.get("name").getAsString().trim() : null;
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Missing 'name' field");

        LOGGER.info("[BackupEndpoint] Restoring snapshot '{}'", name);
        JsonObject result = BackupManager.getInstance().restoreSnapshot(name);
        respond(exchange, 200, GSON.toJson(result));
    }

    // ── DELETE /api/backup/delete?name=... ───────────────────────────────────

    private void handleDelete(HttpExchange exchange) throws IOException {
        String name = queryParam(exchange, "name");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Missing 'name' parameter");

        boolean deleted = BackupManager.getInstance().deleteSnapshot(name);
        NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Snapshot delete '{}' deleted={}", name, deleted);
        JsonObject result = new JsonObject();
        result.addProperty("success", deleted);
        result.addProperty("name",    name);
        respond(exchange, deleted ? 200 : 404, GSON.toJson(result));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void requireAdmin(HttpExchange exchange) {
        Object adminAttr = exchange.getAttribute("auth-admin");
        if (!(Boolean.TRUE.equals(adminAttr))) throw new SecurityException("Admin required");
    }

    private static String queryParam(HttpExchange exchange, String key) {
        String query = exchange.getRequestURI().getQuery();
        if (query == null) return null;
        for (String part : query.split("&")) {
            if (part.startsWith(key + "=")) return part.substring(key.length() + 1);
        }
        return null;
    }

    private static String json(String key, String value) {
        return "{\"" + key + "\":\"" + value.replace("\"", "'") + "\"}";
    }

    private static void respond(HttpExchange exchange, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
    }
}


