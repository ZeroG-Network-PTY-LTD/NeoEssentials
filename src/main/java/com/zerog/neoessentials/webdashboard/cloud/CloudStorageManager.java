package com.zerog.neoessentials.webdashboard.cloud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;

/**
 * Cloud storage integration manager for NeoEssentials dashboard.
 *
 * <p>Supports:
 * <ul>
 *   <li><b>Dropbox</b> — via long-lived access token (Dropbox API v2)</li>
 *   <li><b>Google Drive</b> — via OAuth2 refresh-token flow (Drive API v3)</li>
 * </ul>
 *
 * <p>Cloud credentials/tokens are stored in
 * {@code config/neoessentials/cloud_storage.json}.
 */
public class CloudStorageManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudStorageManager.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static final Path CONFIG_FILE = Path.of("config", "neoessentials", "cloud_storage.json");
    private static CloudStorageManager INSTANCE;

    // ── Dropbox constants ──────────────────────────────────────────────────────
    private static final String DBX_UPLOAD   = "https://content.dropboxapi.com/2/files/upload";
    private static final String DBX_LIST     = "https://api.dropboxapi.com/2/files/list_folder";
    private static final String DBX_DELETE   = "https://api.dropboxapi.com/2/files/delete_v2";
    private static final String DBX_QUOTA    = "https://api.dropboxapi.com/2/users/get_space_usage";

    // ── Google Drive constants ─────────────────────────────────────────────────
    private static final String GD_TOKEN     = "https://oauth2.googleapis.com/token";
    private static final String GD_UPLOAD    = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart";
    private static final String GD_LIST      = "https://www.googleapis.com/drive/v3/files";
    private static final String GD_DELETE    = "https://www.googleapis.com/drive/v3/files/";
    private static final String GD_ABOUT     = "https://www.googleapis.com/drive/v3/about?fields=storageQuota";

    // ── HTTP client (shared, lazy-init) ────────────────────────────────────────
    private final HttpClient http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build();

    // ── In-memory config ────────────────────────────────────────────────────────
    /** Dropbox long-lived access token */
    private String dropboxToken       = "";
    /** Dropbox target folder path (defaults to /NeoEssentials-Backups) */
    private String dropboxPath        = "/NeoEssentials-Backups";

    /** Google Drive OAuth2 refresh token */
    private String googleRefreshToken = "";
    /** Google OAuth2 client ID */
    private String googleClientId     = "";
    /** Google OAuth2 client secret */
    private String googleClientSecret = "";
    /** Google Drive folder ID to store files into (blank = root/My Drive) */
    private String googleFolderId     = "";

    /** Cached Google Drive access token + expiry */
    private volatile String googleAccessToken   = "";
    private volatile long   googleTokenExpiry   = 0L;

    private CloudStorageManager() {
        loadConfig();
    }

    public static synchronized CloudStorageManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new CloudStorageManager();
        }
        return INSTANCE;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Config persistence
    // ═══════════════════════════════════════════════════════════════════════════

    private void loadConfig() {
        if (!Files.exists(CONFIG_FILE)) {
            LOGGER.debug("Cloud storage config not found — using defaults");
            return;
        }
        try {
            String json = Files.readString(CONFIG_FILE, StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();

            if (root.has("dropbox")) {
                JsonObject d = root.getAsJsonObject("dropbox");
                dropboxToken = d.has("accessToken") ? d.get("accessToken").getAsString() : "";
                dropboxPath  = d.has("uploadPath")  ? d.get("uploadPath").getAsString()  : "/NeoEssentials-Backups";
            }
            if (root.has("googleDrive")) {
                JsonObject g = root.getAsJsonObject("googleDrive");
                googleRefreshToken  = g.has("refreshToken")  ? g.get("refreshToken").getAsString()  : "";
                googleClientId      = g.has("clientId")      ? g.get("clientId").getAsString()      : "";
                googleClientSecret  = g.has("clientSecret")  ? g.get("clientSecret").getAsString()  : "";
                googleFolderId      = g.has("folderId")       ? g.get("folderId").getAsString()       : "";
            }
            LOGGER.info("Cloud storage config loaded");
        } catch (Exception e) {
            LOGGER.error("Failed to load cloud storage config: {}", e.getMessage());
        }
    }

    /** Persist current credentials to disk. */
    public synchronized void saveConfig() {
        try {
            Files.createDirectories(CONFIG_FILE.getParent());
            JsonObject root = new JsonObject();

            JsonObject d = new JsonObject();
            d.addProperty("accessToken", dropboxToken);
            d.addProperty("uploadPath",  dropboxPath);
            root.add("dropbox", d);

            JsonObject g = new JsonObject();
            g.addProperty("refreshToken",  googleRefreshToken);
            g.addProperty("clientId",      googleClientId);
            g.addProperty("clientSecret",  googleClientSecret);
            g.addProperty("folderId",      googleFolderId);
            root.add("googleDrive", g);

            Files.writeString(CONFIG_FILE, GSON.toJson(root), StandardCharsets.UTF_8);
            LOGGER.info("Cloud storage config saved");
        } catch (Exception e) {
            LOGGER.error("Failed to save cloud storage config: {}", e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Config setters / getters (called by CloudStorageEndpoint)
    // ═══════════════════════════════════════════════════════════════════════════

    public void setDropboxToken(String token) { this.dropboxToken = token != null ? token.trim() : ""; }
    public void setDropboxPath(String path)   { this.dropboxPath  = path  != null ? path.trim()  : "/NeoEssentials-Backups"; }

    public void setGoogleCredentials(String refreshToken, String clientId, String clientSecret, String folderId) {
        this.googleRefreshToken  = refreshToken  != null ? refreshToken.trim()  : "";
        this.googleClientId      = clientId      != null ? clientId.trim()      : "";
        this.googleClientSecret  = clientSecret  != null ? clientSecret.trim()  : "";
        this.googleFolderId      = folderId      != null ? folderId.trim()      : "";
        this.googleAccessToken   = "";
        this.googleTokenExpiry   = 0L;
    }

    public boolean isDropboxConfigured()     { return !dropboxToken.isEmpty(); }
    public boolean isGoogleDriveConfigured() { return !googleRefreshToken.isEmpty() && !googleClientId.isEmpty() && !googleClientSecret.isEmpty(); }

    /** Masked Dropbox token for status display */
    public String getDropboxTokenMasked() {
        if (dropboxToken.length() < 8) return dropboxToken.isEmpty() ? "" : "****";
        return dropboxToken.substring(0, 4) + "…" + dropboxToken.substring(dropboxToken.length() - 4);
    }
    public String getDropboxPath()        { return dropboxPath; }
    public String getGoogleClientId()     { return googleClientId; }
    public String getGoogleFolderId()     { return googleFolderId; }
    /** Masked refresh token */
    public String getGoogleRefreshMasked() {
        if (googleRefreshToken.length() < 8) return googleRefreshToken.isEmpty() ? "" : "****";
        return googleRefreshToken.substring(0, 4) + "…" + googleRefreshToken.substring(googleRefreshToken.length() - 4);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Dropbox — upload
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Upload a local file to Dropbox.
     *
     * @param localFile the file to upload
     * @param fileName  desired name in Dropbox (without path prefix)
     * @return the Dropbox file metadata JSON object, or null on failure
     */
    public JsonObject uploadToDropbox(Path localFile, String fileName) throws Exception {
        requireDropbox();

        String destPath = dropboxPath + "/" + fileName;
        String apiArg = "{\"path\":\"" + esc(destPath) + "\",\"mode\":\"overwrite\",\"autorename\":false,\"mute\":false}";
        byte[] fileBytes = Files.readAllBytes(localFile);

        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(DBX_UPLOAD))
            .header("Authorization",    "Bearer " + dropboxToken)
            .header("Dropbox-API-Arg",  apiArg)
            .header("Content-Type",     "application/octet-stream")
            .POST(HttpRequest.BodyPublishers.ofByteArray(fileBytes))
            .timeout(Duration.ofMinutes(10))
            .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        checkStatus(resp.statusCode(), "Dropbox upload", resp.body());
        LOGGER.info("Uploaded {} to Dropbox as {}", localFile.getFileName(), destPath);
        return JsonParser.parseString(resp.body()).getAsJsonObject();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Dropbox — list
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * List files in the configured Dropbox folder.
     * @return list of file-entry JSON objects
     */
    public List<JsonObject> listDropboxFiles() throws Exception {
        requireDropbox();

        String body = "{\"path\":\"" + esc(dropboxPath) + "\",\"recursive\":false,\"include_media_info\":false}";

        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(DBX_LIST))
            .header("Authorization",  "Bearer " + dropboxToken)
            .header("Content-Type",   "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
            .timeout(Duration.ofSeconds(30))
            .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        checkStatus(resp.statusCode(), "Dropbox list", resp.body());

        JsonObject root = JsonParser.parseString(resp.body()).getAsJsonObject();
        List<JsonObject> files = new ArrayList<>();
        if (root.has("entries")) {
            root.getAsJsonArray("entries").forEach(e -> {
                JsonObject entry = e.getAsJsonObject();
                if ("file".equals(entry.has(".tag") ? entry.get(".tag").getAsString() : "")) {
                    files.add(entry);
                }
            });
        }
        return files;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Dropbox — delete
    // ═══════════════════════════════════════════════════════════════════════════

    /** Delete a file from Dropbox by its full path (e.g. /NeoEssentials-Backups/foo.zip). */
    public void deleteFromDropbox(String dropboxFilePath) throws Exception {
        requireDropbox();

        String body = "{\"path\":\"" + esc(dropboxFilePath) + "\"}";
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(DBX_DELETE))
            .header("Authorization",  "Bearer " + dropboxToken)
            .header("Content-Type",   "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
            .timeout(Duration.ofSeconds(30))
            .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        checkStatus(resp.statusCode(), "Dropbox delete", resp.body());
        LOGGER.info("Deleted from Dropbox: {}", dropboxFilePath);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Dropbox — quota / test
    // ═══════════════════════════════════════════════════════════════════════════

    /** Get Dropbox storage quota info. Returns null on failure. */
    public JsonObject getDropboxQuota() throws Exception {
        requireDropbox();
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(DBX_QUOTA))
            .header("Authorization", "Bearer " + dropboxToken)
            .header("Content-Type",  "application/json")
            .POST(HttpRequest.BodyPublishers.ofString("null", StandardCharsets.UTF_8))
            .timeout(Duration.ofSeconds(15))
            .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        checkStatus(resp.statusCode(), "Dropbox quota", resp.body());
        return JsonParser.parseString(resp.body()).getAsJsonObject();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Google Drive — OAuth2 token refresh
    // ═══════════════════════════════════════════════════════════════════════════

    /** Return a valid Google Drive access token, refreshing if needed. */
    private synchronized String getGoogleAccessToken() throws Exception {
        if (!googleAccessToken.isEmpty() && System.currentTimeMillis() < googleTokenExpiry - 30_000) {
            return googleAccessToken;
        }
        // Refresh
        String form = "client_id=" + enc(googleClientId)
            + "&client_secret=" + enc(googleClientSecret)
            + "&refresh_token=" + enc(googleRefreshToken)
            + "&grant_type=refresh_token";

        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(GD_TOKEN))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(form, StandardCharsets.UTF_8))
            .timeout(Duration.ofSeconds(20))
            .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        checkStatus(resp.statusCode(), "Google token refresh", resp.body());

        JsonObject json = JsonParser.parseString(resp.body()).getAsJsonObject();
        googleAccessToken = json.get("access_token").getAsString();
        long expiresIn    = json.has("expires_in") ? json.get("expires_in").getAsLong() : 3600L;
        googleTokenExpiry = System.currentTimeMillis() + expiresIn * 1000L;
        LOGGER.debug("Google Drive access token refreshed (expires in {}s)", expiresIn);
        return googleAccessToken;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Google Drive — upload (multipart)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Upload a local file to Google Drive.
     * @param localFile source file
     * @param fileName  desired Drive file name
     * @return Drive file metadata JSON object
     */
    public JsonObject uploadToGoogleDrive(Path localFile, String fileName) throws Exception {
        requireGoogleDrive();
        String token = getGoogleAccessToken();

        // Build multipart body
        String boundary = "NEO_BOUNDARY_" + Long.toHexString(System.currentTimeMillis());
        String metaJson = buildDriveMetadata(fileName);

        byte[] fileBytes = Files.readAllBytes(localFile);
        String mimeType  = "application/zip";

        // Assemble multipart
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String prefix = "--" + boundary + "\r\n"
            + "Content-Type: application/json; charset=UTF-8\r\n\r\n"
            + metaJson + "\r\n"
            + "--" + boundary + "\r\n"
            + "Content-Type: " + mimeType + "\r\n\r\n";
        baos.write(prefix.getBytes(StandardCharsets.UTF_8));
        baos.write(fileBytes);
        String suffix = "\r\n--" + boundary + "--";
        baos.write(suffix.getBytes(StandardCharsets.UTF_8));

        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(GD_UPLOAD))
            .header("Authorization",  "Bearer " + token)
            .header("Content-Type",   "multipart/related; boundary=" + boundary)
            .POST(HttpRequest.BodyPublishers.ofByteArray(baos.toByteArray()))
            .timeout(Duration.ofMinutes(10))
            .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        checkStatus(resp.statusCode(), "Google Drive upload", resp.body());
        LOGGER.info("Uploaded {} to Google Drive ({})", localFile.getFileName(), fileName);
        return JsonParser.parseString(resp.body()).getAsJsonObject();
    }

    private String buildDriveMetadata(String fileName) {
        JsonObject meta = new JsonObject();
        meta.addProperty("name", fileName);
        meta.addProperty("mimeType", "application/zip");
        if (!googleFolderId.isEmpty()) {
            com.google.gson.JsonArray parents = new com.google.gson.JsonArray();
            parents.add(googleFolderId);
            meta.add("parents", parents);
        }
        return meta.toString();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Google Drive — list
    // ═══════════════════════════════════════════════════════════════════════════

    /** List files in the configured Google Drive folder. */
    public List<JsonObject> listGoogleDriveFiles() throws Exception {
        requireGoogleDrive();
        String token = getGoogleAccessToken();

        String query = "mimeType='application/zip' and trashed=false";
        if (!googleFolderId.isEmpty()) {
            query = "'" + googleFolderId + "' in parents and trashed=false";
        }
        String url = GD_LIST + "?q=" + enc(query)
            + "&fields=files(id,name,size,createdTime,modifiedTime)"
            + "&orderBy=createdTime+desc"
            + "&pageSize=50";

        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Bearer " + token)
            .GET()
            .timeout(Duration.ofSeconds(20))
            .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        checkStatus(resp.statusCode(), "Google Drive list", resp.body());

        JsonObject root = JsonParser.parseString(resp.body()).getAsJsonObject();
        List<JsonObject> files = new ArrayList<>();
        if (root.has("files")) {
            root.getAsJsonArray("files").forEach(e -> files.add(e.getAsJsonObject()));
        }
        return files;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Google Drive — delete
    // ═══════════════════════════════════════════════════════════════════════════

    /** Permanently delete a file from Google Drive by its file ID. */
    public void deleteFromGoogleDrive(String fileId) throws Exception {
        requireGoogleDrive();
        String token = getGoogleAccessToken();

        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(GD_DELETE + enc(fileId)))
            .header("Authorization", "Bearer " + token)
            .DELETE()
            .timeout(Duration.ofSeconds(20))
            .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 204 && resp.statusCode() != 200) {
            checkStatus(resp.statusCode(), "Google Drive delete", resp.body());
        }
        LOGGER.info("Deleted from Google Drive: {}", fileId);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Google Drive — quota / test
    // ═══════════════════════════════════════════════════════════════════════════

    /** Get Google Drive storage quota. */
    public JsonObject getGoogleDriveQuota() throws Exception {
        requireGoogleDrive();
        String token = getGoogleAccessToken();

        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(GD_ABOUT))
            .header("Authorization", "Bearer " + token)
            .GET()
            .timeout(Duration.ofSeconds(15))
            .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        checkStatus(resp.statusCode(), "Google Drive quota", resp.body());
        return JsonParser.parseString(resp.body()).getAsJsonObject();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Helpers
    // ═══════════════════════════════════════════════════════════════════════════

    private void requireDropbox() {
        if (dropboxToken.isEmpty()) throw new IllegalStateException("Dropbox is not configured — set an access token first");
    }
    private void requireGoogleDrive() {
        if (googleRefreshToken.isEmpty() || googleClientId.isEmpty() || googleClientSecret.isEmpty()) {
            throw new IllegalStateException("Google Drive is not configured — provide clientId, clientSecret, and refreshToken");
        }
    }

    private static void checkStatus(int code, String op, String body) throws IOException {
        if (code < 200 || code >= 300) {
            throw new IOException(op + " failed (" + code + "): " + trimBody(body));
        }
    }

    private static String trimBody(String body) {
        if (body == null) return "(empty)";
        return body.length() > 300 ? body.substring(0, 300) + "…" : body;
    }

    /** URL-encode a string. */
    private static String enc(String s) {
        if (s == null) return "";
        try {
            return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }

    /** Escape double-quotes for embedding in a JSON string literal. */
    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

