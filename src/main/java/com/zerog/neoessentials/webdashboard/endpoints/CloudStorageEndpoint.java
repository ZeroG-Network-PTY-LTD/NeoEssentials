package com.zerog.neoessentials.webdashboard.endpoints;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.zerog.neoessentials.webdashboard.backup.BackupManager;
import com.zerog.neoessentials.webdashboard.cloud.CloudStorageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

/**
 * REST handler for cloud storage operations.
 *
 * Routes (all require authentication, mutating ops require admin):
 *   GET    /api/cloud/status               – provider configuration & quota status
 *   GET    /api/cloud/config               – return current (masked) config
 *   POST   /api/cloud/config/dropbox       – save Dropbox access-token & path [ADMIN]
 *   POST   /api/cloud/config/google        – save Google Drive OAuth2 credentials [ADMIN]
 *   POST   /api/cloud/test/dropbox         – test Dropbox connection [ADMIN]
 *   POST   /api/cloud/test/google          – test Google Drive connection [ADMIN]
 *   GET    /api/cloud/files/dropbox        – list Dropbox files
 *   GET    /api/cloud/files/google         – list Google Drive files
 *   POST   /api/cloud/upload/dropbox/{id}  – upload backup to Dropbox [ADMIN]
 *   POST   /api/cloud/upload/google/{id}   – upload backup to Google Drive [ADMIN]
 *   DELETE /api/cloud/files/dropbox/{path} – delete Dropbox file [ADMIN]
 *   DELETE /api/cloud/files/google/{id}    – delete Google Drive file [ADMIN]
 */
public class CloudStorageEndpoint implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudStorageEndpoint.class);

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin",  "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");

        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        String path   = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        try {
            if ("GET".equals(method) && path.endsWith("/status")) {
                handleStatus(exchange);
            } else if ("GET".equals(method) && path.endsWith("/config")) {
                handleGetConfig(exchange);
            } else if ("POST".equals(method) && path.endsWith("/config/dropbox")) {
                requireAdmin(exchange);
                handleConfigDropbox(exchange);
            } else if ("POST".equals(method) && path.endsWith("/config/google")) {
                requireAdmin(exchange);
                handleConfigGoogle(exchange);
            } else if ("POST".equals(method) && path.endsWith("/test/dropbox")) {
                requireAdmin(exchange);
                handleTestDropbox(exchange);
            } else if ("POST".equals(method) && path.endsWith("/test/google")) {
                requireAdmin(exchange);
                handleTestGoogle(exchange);
            } else if ("GET".equals(method) && path.endsWith("/files/dropbox")) {
                handleListDropbox(exchange);
            } else if ("GET".equals(method) && path.endsWith("/files/google")) {
                handleListGoogle(exchange);
            } else if ("POST".equals(method) && path.contains("/upload/dropbox/")) {
                requireAdmin(exchange);
                handleUploadDropbox(exchange, path);
            } else if ("POST".equals(method) && path.contains("/upload/google/")) {
                requireAdmin(exchange);
                handleUploadGoogle(exchange, path);
            } else if ("DELETE".equals(method) && path.contains("/files/dropbox/")) {
                requireAdmin(exchange);
                handleDeleteDropbox(exchange, path);
            } else if ("DELETE".equals(method) && path.contains("/files/google/")) {
                requireAdmin(exchange);
                handleDeleteGoogle(exchange, path);
            } else {
                sendJson(exchange, 404, "{\"success\":false,\"error\":\"Unknown cloud endpoint\"}");
            }
        } catch (IllegalStateException e) {
            // Provider not configured
            sendJson(exchange, 400, json(false, e.getMessage()));
        } catch (IOException e) {
            // HTTP call or disk error
            sendJson(exchange, 502, json(false, "Cloud provider error: " + e.getMessage()));
        } catch (Exception e) {
            LOGGER.error("Unexpected error in CloudStorageEndpoint", e);
            sendJson(exchange, 500, json(false, "Internal error: " + e.getMessage()));
        }
    }

    // ── Status ─────────────────────────────────────────────────────────────────

    private void handleStatus(HttpExchange exchange) throws IOException {
        CloudStorageManager csm = CloudStorageManager.getInstance();
        StringBuilder sb = new StringBuilder("{\"success\":true,\"providers\":{");

        // Dropbox
        sb.append("\"dropbox\":{");
        sb.append("\"configured\":").append(csm.isDropboxConfigured()).append(",");
        sb.append("\"uploadPath\":\"").append(esc(csm.getDropboxPath())).append("\",");
        sb.append("\"tokenMasked\":\"").append(esc(csm.getDropboxTokenMasked())).append("\"");
        if (csm.isDropboxConfigured()) {
            try {
                JsonObject quota = csm.getDropboxQuota();
                if (quota != null && quota.has("allocation")) {
                    JsonObject alloc = quota.getAsJsonObject("allocation");
                    long allocated = alloc.has("allocated") ? alloc.get("allocated").getAsLong() : 0L;
                    long used      = quota.has("used") ? quota.get("used").getAsLong() : 0L;
                    sb.append(",\"quotaUsedMB\":").append(used / 1048576L);
                    sb.append(",\"quotaTotalMB\":").append(allocated / 1048576L);
                    sb.append(",\"connected\":true");
                }
            } catch (Exception e) {
                sb.append(",\"connected\":false,\"error\":\"").append(esc(e.getMessage())).append("\"");
            }
        }
        sb.append("},");

        // Google Drive
        sb.append("\"googleDrive\":{");
        sb.append("\"configured\":").append(csm.isGoogleDriveConfigured()).append(",");
        sb.append("\"folderId\":\"").append(esc(csm.getGoogleFolderId())).append("\",");
        sb.append("\"clientId\":\"").append(esc(csm.getGoogleClientId())).append("\"");
        if (csm.isGoogleDriveConfigured()) {
            try {
                JsonObject quota = csm.getGoogleDriveQuota();
                if (quota != null && quota.has("storageQuota")) {
                    JsonObject q = quota.getAsJsonObject("storageQuota");
                    long limit  = q.has("limit")   ? q.get("limit").getAsLong()  : 0L;
                    long usage  = q.has("usage")   ? q.get("usage").getAsLong()  : 0L;
                    sb.append(",\"quotaUsedMB\":").append(usage / 1048576L);
                    sb.append(",\"quotaTotalMB\":").append(limit / 1048576L);
                    sb.append(",\"connected\":true");
                }
            } catch (Exception e) {
                sb.append(",\"connected\":false,\"error\":\"").append(esc(e.getMessage())).append("\"");
            }
        }
        sb.append("}");

        sb.append("}}");
        sendJson(exchange, 200, sb.toString());
    }

    // ── Config endpoints ───────────────────────────────────────────────────────

    private void handleGetConfig(HttpExchange exchange) throws IOException {
        CloudStorageManager csm = CloudStorageManager.getInstance();
        String body = "{\"success\":true,"
            + "\"dropbox\":{\"configured\":" + csm.isDropboxConfigured() + ","
            + "\"tokenMasked\":\"" + esc(csm.getDropboxTokenMasked()) + "\","
            + "\"uploadPath\":\"" + esc(csm.getDropboxPath()) + "\"},"
            + "\"googleDrive\":{\"configured\":" + csm.isGoogleDriveConfigured() + ","
            + "\"clientId\":\"" + esc(csm.getGoogleClientId()) + "\","
            + "\"folderId\":\"" + esc(csm.getGoogleFolderId()) + "\","
            + "\"refreshTokenMasked\":\"" + esc(csm.getGoogleRefreshMasked()) + "\"}"
            + "}";
        sendJson(exchange, 200, body);
    }

    private void handleConfigDropbox(HttpExchange exchange) throws Exception {
        JsonObject body = readBody(exchange);
        CloudStorageManager csm = CloudStorageManager.getInstance();
        if (body.has("accessToken")) csm.setDropboxToken(body.get("accessToken").getAsString());
        if (body.has("uploadPath"))  csm.setDropboxPath(body.get("uploadPath").getAsString());
        csm.saveConfig();
        sendJson(exchange, 200, json(true, "Dropbox configuration saved"));
    }

    private void handleConfigGoogle(HttpExchange exchange) throws Exception {
        JsonObject body = readBody(exchange);
        CloudStorageManager csm = CloudStorageManager.getInstance();
        csm.setGoogleCredentials(
            body.has("refreshToken")  ? body.get("refreshToken").getAsString()  : "",
            body.has("clientId")      ? body.get("clientId").getAsString()      : "",
            body.has("clientSecret")  ? body.get("clientSecret").getAsString()  : "",
            body.has("folderId")      ? body.get("folderId").getAsString()       : ""
        );
        csm.saveConfig();
        sendJson(exchange, 200, json(true, "Google Drive configuration saved"));
    }

    // ── Test connections ───────────────────────────────────────────────────────

    private void handleTestDropbox(HttpExchange exchange) throws Exception {
        JsonObject quota = CloudStorageManager.getInstance().getDropboxQuota();
        long used  = quota.has("used") ? quota.get("used").getAsLong() : 0L;
        sendJson(exchange, 200, "{\"success\":true,\"message\":\"Dropbox connection successful\","
            + "\"usedMB\":" + (used / 1048576L) + "}");
    }

    private void handleTestGoogle(HttpExchange exchange) throws Exception {
        JsonObject quota = CloudStorageManager.getInstance().getGoogleDriveQuota();
        String resp = "{\"success\":true,\"message\":\"Google Drive connection successful\","
            + "\"quota\":" + quota.toString() + "}";
        sendJson(exchange, 200, resp);
    }

    // ── List files ─────────────────────────────────────────────────────────────

    private void handleListDropbox(HttpExchange exchange) throws Exception {
        List<JsonObject> files = CloudStorageManager.getInstance().listDropboxFiles();
        StringBuilder sb = new StringBuilder("{\"success\":true,\"count\":").append(files.size()).append(",\"files\":[");
        for (int i = 0; i < files.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(files.get(i).toString());
        }
        sb.append("]}");
        sendJson(exchange, 200, sb.toString());
    }

    private void handleListGoogle(HttpExchange exchange) throws Exception {
        List<JsonObject> files = CloudStorageManager.getInstance().listGoogleDriveFiles();
        StringBuilder sb = new StringBuilder("{\"success\":true,\"count\":").append(files.size()).append(",\"files\":[");
        for (int i = 0; i < files.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(files.get(i).toString());
        }
        sb.append("]}");
        sendJson(exchange, 200, sb.toString());
    }

    // ── Upload ─────────────────────────────────────────────────────────────────

    private void handleUploadDropbox(HttpExchange exchange, String path) throws Exception {
        String backupId = lastSegment(path, "/upload/dropbox/");
        Path zipFile   = resolveBackupFile(backupId);
        String fileName = backupId + ".zip";

        JsonObject meta = CloudStorageManager.getInstance().uploadToDropbox(zipFile, fileName);
        sendJson(exchange, 200, "{\"success\":true,\"message\":\"Uploaded to Dropbox successfully\","
            + "\"file\":" + meta.toString() + "}");
    }

    private void handleUploadGoogle(HttpExchange exchange, String path) throws Exception {
        String backupId = lastSegment(path, "/upload/google/");
        Path zipFile   = resolveBackupFile(backupId);
        String fileName = backupId + ".zip";

        JsonObject meta = CloudStorageManager.getInstance().uploadToGoogleDrive(zipFile, fileName);
        sendJson(exchange, 200, "{\"success\":true,\"message\":\"Uploaded to Google Drive successfully\","
            + "\"file\":" + meta.toString() + "}");
    }

    // ── Delete ─────────────────────────────────────────────────────────────────

    private void handleDeleteDropbox(HttpExchange exchange, String path) throws Exception {
        // path format: /api/cloud/files/dropbox/NeoEssentials-Backups%2Ffile.zip
        String filePath = java.net.URLDecoder.decode(
            path.substring(path.indexOf("/files/dropbox/") + "/files/dropbox/".length()),
            StandardCharsets.UTF_8);
        CloudStorageManager.getInstance().deleteFromDropbox(filePath);
        sendJson(exchange, 200, json(true, "Deleted from Dropbox: " + filePath));
    }

    private void handleDeleteGoogle(HttpExchange exchange, String path) throws Exception {
        String fileId = path.substring(path.lastIndexOf('/') + 1);
        CloudStorageManager.getInstance().deleteFromGoogleDrive(fileId);
        sendJson(exchange, 200, json(true, "Deleted from Google Drive: " + fileId));
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private void requireAdmin(HttpExchange exchange) throws IOException {
        Boolean admin = (Boolean) exchange.getAttribute("auth-admin");
        if (!Boolean.TRUE.equals(admin)) {
            sendJson(exchange, 403, json(false, "Admin access required"));
            throw new SecurityException("Not admin");
        }
    }

    private Path resolveBackupFile(String backupId) throws IOException {
        // Look for backupId.zip in the backup directory
        Path file = BackupManager.BACKUP_DIR.resolve(sanitize(backupId) + ".zip");
        if (!java.nio.file.Files.exists(file)) {
            throw new FileNotFoundException("Backup not found: " + backupId);
        }
        return file;
    }

    private static String sanitize(String s) {
        return s == null ? "" : s.replaceAll("[^a-zA-Z0-9_\\-.]", "_");
    }

    private static String lastSegment(String path, String prefix) {
        int idx = path.indexOf(prefix);
        return idx >= 0 ? path.substring(idx + prefix.length()) : "";
    }

    private JsonObject readBody(HttpExchange exchange) throws IOException {
        return com.zerog.neoessentials.webdashboard.util.RequestBodyUtil.readJsonObject(exchange);
    }

    private void sendJson(HttpExchange exchange, int code, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
    }

    private String json(boolean ok, String msg) {
        return "{\"success\":" + ok + ",\"message\":\"" + esc(msg) + "\"}";
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r");
    }
}

