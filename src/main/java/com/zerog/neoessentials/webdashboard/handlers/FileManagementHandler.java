package com.zerog.neoessentials.webdashboard.handlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.zerog.neoessentials.webdashboard.cloud.CloudProviderManager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Handler for /api/files endpoint
 * Provides file browsing, reading, writing, and management capabilities
 */
public class FileManagementHandler implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileManagementHandler.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    // Allowed directories for file operations (security)
    private static final List<Path> ALLOWED_PATHS = Arrays.asList(
        Paths.get("config"),
        Paths.get("logs"),
        Paths.get("neoessentials"),
        Paths.get("world")
    );
    
    // Allowed file extensions for editing
    private static final Set<String> EDITABLE_EXTENSIONS = new HashSet<>(Arrays.asList(
        ".json", ".txt", ".properties", ".yml", ".yaml", ".toml", ".conf", ".cfg", ".log"
    ));
    
    // Maximum file size for reading/editing (10 MB)
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Add CORS headers
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        
        // Handle OPTIONS preflight
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }
        
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        
        try {
            switch (method) {
                case "GET":
                    if (path.endsWith("/browse")) {
                        handleBrowse(exchange);
                    } else if (path.endsWith("/read")) {
                        handleRead(exchange);
                    } else if (path.endsWith("/download")) {
                        handleDownload(exchange);
                    } else if (path.endsWith("/listBackups")) {
                        handleListBackups(exchange);
                    } else if (path.endsWith("/cloudProviders")) {
                        handleCloudProviders(exchange);
                    } else if (path.endsWith("/server/statistics")) {
                        handleServerStatistics(exchange);
                    } else if (path.endsWith("/player/statistics")) {
                        handlePlayerStatistics(exchange);
                    } else if (path.endsWith("/user/activityLog")) {
                        handleUserActivityLog(exchange);
                    } else {
                        sendJsonResponse(exchange, 400, createErrorResponse("Invalid GET endpoint"));
                    }
                    break;
                case "POST":
                    if (path.endsWith("/write")) {
                        handleWrite(exchange);
                    } else if (path.endsWith("/create")) {
                        handleCreate(exchange);
                    } else if (path.endsWith("/upload")) {
                        handleUpload(exchange);
                    } else if (path.endsWith("/restore")) {
                        handleRestore(exchange);
                    } else if (path.endsWith("/cloudBackup")) {
                        handleCloudBackup(exchange);
                    } else if (path.endsWith("/cloudRestore")) {
                        handleCloudRestore(exchange);
                    } else if (path.endsWith("/cloudLink")) {
                        handleCloudLink(exchange);
                    } else if (path.endsWith("/cloudUnlink")) {
                        handleCloudUnlink(exchange);
                    } else {
                        sendJsonResponse(exchange, 400, createErrorResponse("Invalid POST endpoint"));
                    }
                    break;
                case "DELETE":
                    if (path.endsWith("/delete")) {
                        handleDelete(exchange);
                    } else {
                        sendJsonResponse(exchange, 400, createErrorResponse("Invalid DELETE endpoint"));
                    }
                    break;
                default:
                    sendJsonResponse(exchange, 405, createErrorResponse("Method not allowed"));
            }
        } catch (SecurityException e) {
            LOGGER.warn("Security violation attempt: {}", e.getMessage());
            sendJsonResponse(exchange, 403, createErrorResponse("Access denied"));
        } catch (Exception e) {
            LOGGER.error("Error handling file management request", e);
            sendJsonResponse(exchange, 500, createErrorResponse("Internal server error: " + e.getMessage()));
        }
    }
    
    /**
     * Browse directory contents
     * GET /api/files/browse?path=config
     */
    private void handleBrowse(HttpExchange exchange) throws IOException {
        Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
        String pathParam = params.getOrDefault("path", "");
        
        Path targetPath = resolvePath(pathParam);
        validatePath(targetPath);
        
        if (!Files.exists(targetPath)) {
            sendJsonResponse(exchange, 404, createErrorResponse("Path not found"));
            return;
        }
        
        if (!Files.isDirectory(targetPath)) {
            sendJsonResponse(exchange, 400, createErrorResponse("Path is not a directory"));
            return;
        }
        
        JsonObject response = new JsonObject();
        response.addProperty("path", pathParam);
        response.addProperty("absolutePath", targetPath.toAbsolutePath().toString());
        
        JsonArray items = new JsonArray();
        try (Stream<Path> paths = Files.list(targetPath)) {
            paths.sorted().forEach(path -> {
                try {
                    JsonObject item = new JsonObject();
                    item.addProperty("name", path.getFileName().toString());
                    item.addProperty("type", Files.isDirectory(path) ? "directory" : "file");
                    
                    BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
                    item.addProperty("size", attrs.size());
                    item.addProperty("modified", attrs.lastModifiedTime().toMillis());
                    item.addProperty("created", attrs.creationTime().toMillis());
                    
                    if (!Files.isDirectory(path)) {
                        String fileName = path.getFileName().toString();
                        String extension = getFileExtension(fileName);
                        item.addProperty("extension", extension);
                        item.addProperty("editable", EDITABLE_EXTENSIONS.contains(extension.toLowerCase()));
                    }
                    
                    items.add(item);
                } catch (IOException e) {
                    LOGGER.warn("Error reading file attributes: {}", path, e);
                }
            });
        }
        
        response.add("items", items);
        sendJsonResponse(exchange, 200, response);
    }
    
    /**
     * Read file contents
     * GET /api/files/read?path=config/main.json
     */
    private void handleRead(HttpExchange exchange) throws IOException {
        Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
        String pathParam = params.getOrDefault("path", "");
        
        Path targetPath = resolvePath(pathParam);
        validatePath(targetPath);
        
        if (!Files.exists(targetPath)) {
            sendJsonResponse(exchange, 404, createErrorResponse("File not found"));
            return;
        }
        
        if (Files.isDirectory(targetPath)) {
            sendJsonResponse(exchange, 400, createErrorResponse("Path is a directory"));
            return;
        }
        
        long fileSize = Files.size(targetPath);
        if (fileSize > MAX_FILE_SIZE) {
            sendJsonResponse(exchange, 400, createErrorResponse("File too large to read (max 10 MB)"));
            return;
        }
        
        String content = Files.readString(targetPath, StandardCharsets.UTF_8);
        
        JsonObject response = new JsonObject();
        response.addProperty("path", pathParam);
        response.addProperty("content", content);
        response.addProperty("size", fileSize);
        response.addProperty("extension", getFileExtension(targetPath.getFileName().toString()));
        
        sendJsonResponse(exchange, 200, response);
    }
    
    /**
     * Download file
     * GET /api/files/download?path=logs/latest.log
     */
    private void handleDownload(HttpExchange exchange) throws IOException {
        Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
        String pathParam = params.getOrDefault("path", "");
        
        Path targetPath = resolvePath(pathParam);
        validatePath(targetPath);
        
        if (!Files.exists(targetPath) || Files.isDirectory(targetPath)) {
            sendJsonResponse(exchange, 404, createErrorResponse("File not found"));
            return;
        }
        
        byte[] fileBytes = Files.readAllBytes(targetPath);
        
        exchange.getResponseHeaders().set("Content-Type", "application/octet-stream");
        exchange.getResponseHeaders().set("Content-Disposition", 
            "attachment; filename=\"" + targetPath.getFileName().toString() + "\"");
        exchange.sendResponseHeaders(200, fileBytes.length);
        
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(fileBytes);
        }
    }
    
    /**
     * Write/update file contents
     * POST /api/files/write
     * Body: {"path": "config/main.json", "content": "..."}
     */
    private void handleWrite(HttpExchange exchange) throws IOException {
        String requestBody = readRequestBody(exchange);
        JsonObject request = GSON.fromJson(requestBody, JsonObject.class);
        
        if (!request.has("path") || !request.has("content")) {
            sendJsonResponse(exchange, 400, createErrorResponse("Missing 'path' or 'content' field"));
            return;
        }
        
        String pathParam = request.get("path").getAsString();
        String content = request.get("content").getAsString();
        
        Path targetPath = resolvePath(pathParam);
        validatePath(targetPath);
        
        if (!Files.exists(targetPath)) {
            sendJsonResponse(exchange, 404, createErrorResponse("File not found"));
            return;
        }
        
        // Create backup before writing
        Path backupPath = createBackup(targetPath);
        
        try {
            Files.writeString(targetPath, content, StandardCharsets.UTF_8);
            
            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("message", "File written successfully");
            response.addProperty("path", pathParam);
            response.addProperty("backup", backupPath.toString());
            
            sendJsonResponse(exchange, 200, response);
        } catch (IOException e) {
            LOGGER.error("Error writing file", e);
            sendJsonResponse(exchange, 500, createErrorResponse("Failed to write file: " + e.getMessage()));
        }
    }
    
    /**
     * Create new file or directory
     * POST /api/files/create
     * Body: {"path": "config/newfile.json", "type": "file", "content": "..."}
     */
    private void handleCreate(HttpExchange exchange) throws IOException {
        String requestBody = readRequestBody(exchange);
        JsonObject request = GSON.fromJson(requestBody, JsonObject.class);
        
        if (!request.has("path") || !request.has("type")) {
            sendJsonResponse(exchange, 400, createErrorResponse("Missing 'path' or 'type' field"));
            return;
        }
        
        String pathParam = request.get("path").getAsString();
        String type = request.get("type").getAsString();
        
        Path targetPath = resolvePath(pathParam);
        validatePath(targetPath);
        
        if (Files.exists(targetPath)) {
            sendJsonResponse(exchange, 409, createErrorResponse("Path already exists"));
            return;
        }
        
        if ("directory".equals(type)) {
            Files.createDirectories(targetPath);
        } else if ("file".equals(type)) {
            // Ensure parent directory exists
            if (targetPath.getParent() != null) {
                Files.createDirectories(targetPath.getParent());
            }
            
            String content = request.has("content") ? request.get("content").getAsString() : "";
            Files.writeString(targetPath, content, StandardCharsets.UTF_8);
        } else {
            sendJsonResponse(exchange, 400, createErrorResponse("Invalid type (must be 'file' or 'directory')"));
            return;
        }
        
        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.addProperty("message", type + " created successfully");
        response.addProperty("path", pathParam);
        
        sendJsonResponse(exchange, 201, response);
    }
    
    /**
     * Upload file
     * POST /api/files/upload
     * Body: multipart/form-data with file and path
     */
    private void handleUpload(HttpExchange exchange) throws IOException {
        // Note: Full multipart implementation would require additional library
        // For now, accept base64 encoded content in JSON
        String requestBody = readRequestBody(exchange);
        JsonObject request = GSON.fromJson(requestBody, JsonObject.class);
        
        if (!request.has("path") || !request.has("content")) {
            sendJsonResponse(exchange, 400, createErrorResponse("Missing 'path' or 'content' field"));
            return;
        }
        
        String pathParam = request.get("path").getAsString();
        String base64Content = request.get("content").getAsString();
        
        Path targetPath = resolvePath(pathParam);
        validatePath(targetPath);
        
        // Ensure parent directory exists
        if (targetPath.getParent() != null) {
            Files.createDirectories(targetPath.getParent());
        }
        
        byte[] decodedContent = Base64.getDecoder().decode(base64Content);
        Files.write(targetPath, decodedContent);
        
        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.addProperty("message", "File uploaded successfully");
        response.addProperty("path", pathParam);
        response.addProperty("size", decodedContent.length);
        
        sendJsonResponse(exchange, 201, response);
    }
    
    /**
     * Delete file or directory
     * DELETE /api/files/delete?path=config/temp.json
     */
    private void handleDelete(HttpExchange exchange) throws IOException {
        Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
        String pathParam = params.getOrDefault("path", "");
        
        Path targetPath = resolvePath(pathParam);
        validatePath(targetPath);
        
        if (!Files.exists(targetPath)) {
            sendJsonResponse(exchange, 404, createErrorResponse("Path not found"));
            return;
        }
        
        // Create backup before deleting
        Path backupPath = createBackup(targetPath);
        
        if (Files.isDirectory(targetPath)) {
            // Delete directory recursively
            deleteDirectory(targetPath);
        } else {
            Files.delete(targetPath);
        }
        
        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.addProperty("message", "Path deleted successfully");
        response.addProperty("path", pathParam);
        response.addProperty("backup", backupPath.toString());
        
        sendJsonResponse(exchange, 200, response);
    }
    
    /**
     * List backups for a given file
     * GET /api/files/listBackups?path=config/main.json
     */
    private void handleListBackups(HttpExchange exchange) throws IOException {
        Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
        String pathParam = params.getOrDefault("path", "");
        Path targetPath = resolvePath(pathParam);
        validatePath(targetPath);
        String fileName = targetPath.getFileName().toString();
        Path backupDir = Paths.get("neoessentials", "backups", "files");
        Files.createDirectories(backupDir);
        JsonArray backups = new JsonArray();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(backupDir, fileName + ".*.backup")) {
            for (Path backup : stream) {
                JsonObject obj = new JsonObject();
                obj.addProperty("name", backup.getFileName().toString());
                obj.addProperty("path", backup.toString());
                obj.addProperty("modified", Files.getLastModifiedTime(backup).toMillis());
                obj.addProperty("size", Files.size(backup));
                backups.add(obj);
            }
        }
        JsonObject response = new JsonObject();
        response.add("backups", backups);
        sendJsonResponse(exchange, 200, response);
    }

    /**
     * Restore a file from backup
     * POST /api/files/restore
     * Body: {"targetPath": "config/main.json", "backupPath": "neoessentials/backups/files/main.json.1234567890.backup"}
     */
    private void handleRestore(HttpExchange exchange) throws IOException {
        String requestBody = readRequestBody(exchange);
        JsonObject request = GSON.fromJson(requestBody, JsonObject.class);
        if (!request.has("targetPath") || !request.has("backupPath")) {
            sendJsonResponse(exchange, 400, createErrorResponse("Missing 'targetPath' or 'backupPath' field"));
            return;
        }
        Path targetPath = resolvePath(request.get("targetPath").getAsString());
        Path backupPath = Paths.get(request.get("backupPath").getAsString());
        validatePath(targetPath);
        // Only allow restore from backup directory
        Path backupDir = Paths.get("neoessentials", "backups", "files").toAbsolutePath();
        if (!backupPath.toAbsolutePath().startsWith(backupDir)) {
            sendJsonResponse(exchange, 403, createErrorResponse("Invalid backup path"));
            return;
        }
        // Ensure parent directory exists
        if (targetPath.getParent() != null) {
            Files.createDirectories(targetPath.getParent());
        }
        Files.copy(backupPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.addProperty("message", "File restored from backup");
        response.addProperty("targetPath", targetPath.toString());
        response.addProperty("backupPath", backupPath.toString());
        sendJsonResponse(exchange, 200, response);
    }

    /**
     * List available cloud providers and their status
     * GET /api/files/cloudProviders
     */
    private void handleCloudProviders(HttpExchange exchange) throws IOException {
        CloudProviderManager cloudManager = CloudProviderManager.getInstance();

        JsonArray providers = new JsonArray();

        // Google Drive
        JsonObject google = new JsonObject();
        google.addProperty("name", "Google Drive");
        google.addProperty("id", "google_drive");
        google.addProperty("linked", cloudManager.isProviderLinked("google_drive"));
        google.addProperty("description", "Store backups on Google Drive");
        google.addProperty("icon", "☁️");
        providers.add(google);

        // Dropbox
        JsonObject dropbox = new JsonObject();
        dropbox.addProperty("name", "Dropbox");
        dropbox.addProperty("id", "dropbox");
        dropbox.addProperty("linked", cloudManager.isProviderLinked("dropbox"));
        dropbox.addProperty("description", "Store backups on Dropbox");
        dropbox.addProperty("icon", "📦");
        providers.add(dropbox);

        // OneDrive (bonus provider)
        JsonObject onedrive = new JsonObject();
        onedrive.addProperty("name", "OneDrive");
        onedrive.addProperty("id", "onedrive");
        onedrive.addProperty("linked", cloudManager.isProviderLinked("onedrive"));
        onedrive.addProperty("description", "Store backups on Microsoft OneDrive");
        onedrive.addProperty("icon", "☁️");
        providers.add(onedrive);

        JsonObject response = new JsonObject();
        response.add("providers", providers);
        response.addProperty("stub", true);
        response.addProperty("message", "Cloud provider integration not fully implemented yet. OAuth linking is ready but file sync requires additional implementation.");

        sendJsonResponse(exchange, 200, response);
    }

    /**
     * Initiate backup of a file/folder to a selected cloud provider (stub)
     * POST /api/files/cloudBackup
     * Body: {"path": "config/main.json", "provider": "Google Drive"}
     */
    private void handleCloudBackup(HttpExchange exchange) throws IOException {
        JsonObject response = new JsonObject();
        response.addProperty("success", false);
        response.addProperty("stub", true);
        response.addProperty("message", "Cloud backup not implemented yet. This will upload the file/folder to the selected provider in the future.");
        sendJsonResponse(exchange, 501, response);
    }

    /**
     * Restore a file/folder from a selected cloud provider (stub)
     * POST /api/files/cloudRestore
     * Body: {"path": "config/main.json", "provider": "Google Drive", "cloudPath": "..."}
     */
    private void handleCloudRestore(HttpExchange exchange) throws IOException {
        JsonObject response = new JsonObject();
        response.addProperty("success", false);
        response.addProperty("stub", true);
        response.addProperty("message", "Cloud restore not implemented yet. This will download the file/folder from the selected provider in the future.");
        sendJsonResponse(exchange, 501, response);
    }

    /**
     * Link a cloud provider (OAuth authentication)
     * POST /api/files/cloudLink
     * Body: {"provider": "google_drive", "accessToken": "...", "refreshToken": "...", "expiresIn": 3600}
     */
    private void handleCloudLink(HttpExchange exchange) throws IOException {
        String requestBody = readRequestBody(exchange);
        JsonObject request = GSON.fromJson(requestBody, JsonObject.class);

        if (!request.has("provider") || !request.has("accessToken")) {
            sendJsonResponse(exchange, 400, createErrorResponse("Missing 'provider' or 'accessToken' field"));
            return;
        }

        String provider = request.get("provider").getAsString();
        String accessToken = request.get("accessToken").getAsString();
        String refreshToken = request.has("refreshToken") ? request.get("refreshToken").getAsString() : null;
        long expiresIn = request.has("expiresIn") ? request.get("expiresIn").getAsLong() : 3600;

        CloudProviderManager cloudManager = CloudProviderManager.getInstance();
        cloudManager.storeToken(provider, accessToken, refreshToken, expiresIn);

        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.addProperty("message", "Cloud provider linked successfully");
        response.addProperty("provider", provider);
        response.addProperty("linked", true);

        sendJsonResponse(exchange, 200, response);
    }

    /**
     * Unlink a cloud provider (remove OAuth token)
     * POST /api/files/cloudUnlink
     * Body: {"provider": "google_drive"}
     */
    private void handleCloudUnlink(HttpExchange exchange) throws IOException {
        String requestBody = readRequestBody(exchange);
        JsonObject request = GSON.fromJson(requestBody, JsonObject.class);

        if (!request.has("provider")) {
            sendJsonResponse(exchange, 400, createErrorResponse("Missing 'provider' field"));
            return;
        }

        String provider = request.get("provider").getAsString();

        CloudProviderManager cloudManager = CloudProviderManager.getInstance();
        cloudManager.removeToken(provider);

        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.addProperty("message", "Cloud provider unlinked successfully");
        response.addProperty("provider", provider);
        response.addProperty("linked", false);

        sendJsonResponse(exchange, 200, response);
    }

    /**
     * Stub: Get server-wide statistics (uptime, TPS, RAM, CPU, etc.)
     * GET /api/files/server/statistics
     */
    private void handleServerStatistics(HttpExchange exchange) throws IOException {
        JsonObject response = new JsonObject();
        response.addProperty("stub", true);
        response.addProperty("message", "Server statistics endpoint not implemented yet.");
        // Example fields for future implementation
        response.addProperty("uptime", 0);
        response.addProperty("tps", 20.0);
        response.addProperty("ramUsedMB", 0);
        response.addProperty("ramTotalMB", 0);
        response.addProperty("cpuUsage", 0.0);
        sendJsonResponse(exchange, 200, response);
    }

    /**
     * Stub: Get player statistics (online time, messages sent, economy, etc.)
     * GET /api/files/player/statistics
     */
    private void handlePlayerStatistics(HttpExchange exchange) throws IOException {
        JsonObject response = new JsonObject();
        response.addProperty("stub", true);
        response.addProperty("message", "Player statistics endpoint not implemented yet.");
        // Example fields for future implementation
        response.addProperty("onlineTime", 0);
        response.addProperty("messagesSent", 0);
        response.addProperty("balance", 0);
        sendJsonResponse(exchange, 200, response);
    }

    /**
     * Stub: Get user activity log for user management improvements
     * GET /api/files/user/activityLog
     */
    private void handleUserActivityLog(HttpExchange exchange) throws IOException {
        JsonObject response = new JsonObject();
        response.addProperty("stub", true);
        response.addProperty("message", "User activity log endpoint not implemented yet.");
        // Example: response.add("log", new JsonArray());
        sendJsonResponse(exchange, 200, response);
    }

    /**
     * Resolve and normalize path
     */
    private Path resolvePath(String pathParam) {
        if (pathParam.isEmpty()) {
            return Paths.get(".");
        }
        return Paths.get(pathParam).normalize();
    }
    
    /**
     * Validate path is within allowed directories
     */
    private void validatePath(Path path) throws SecurityException {
        Path normalized = path.normalize().toAbsolutePath();
        
        boolean allowed = ALLOWED_PATHS.stream()
            .anyMatch(allowedPath -> {
                try {
                    Path allowedNormalized = allowedPath.normalize().toAbsolutePath();
                    return normalized.startsWith(allowedNormalized);
                } catch (Exception e) {
                    return false;
                }
            });
        
        if (!allowed) {
            throw new SecurityException("Access to path denied: " + path);
        }
    }
    
    /**
     * Create backup of file before modification
     */
    private Path createBackup(Path file) throws IOException {
        Path backupDir = Paths.get("neoessentials", "backups", "files");
        Files.createDirectories(backupDir);
        
        String timestamp = String.valueOf(System.currentTimeMillis());
        String fileName = file.getFileName().toString();
        Path backupPath = backupDir.resolve(fileName + "." + timestamp + ".backup");
        
        if (Files.isDirectory(file)) {
            // For directories, create a zip backup
            // For simplicity, just return the backup directory path
            return backupDir;
        } else {
            Files.copy(file, backupPath);
        }
        
        return backupPath;
    }
    
    /**
     * Delete directory recursively
     */
    private void deleteDirectory(Path directory) throws IOException {
        Files.walkFileTree(directory, new SimpleFileVisitor<>() {
            @NotNull
            @Override
            public FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
            @NotNull
            @Override
            public FileVisitResult postVisitDirectory(@NotNull Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
    
    /**
     * Get file extension
     */
    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot);
        }
        return "";
    }
    
    /**
     * Parse query parameters
     */
    private Map<String, String> parseQueryParams(String query) {
        if (query == null || query.isEmpty()) {
            return Collections.emptyMap();
        }
        
        return Arrays.stream(query.split("&"))
            .map(param -> param.split("=", 2))
            .filter(parts -> parts.length == 2)
            .collect(Collectors.toMap(
                parts -> URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                parts -> URLDecoder.decode(parts[1], StandardCharsets.UTF_8)
            ));
    }
    
    /**
     * Read request body
     */
    private String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }
    
    /**
     * Send JSON response
     */
    private void sendJsonResponse(HttpExchange exchange, int statusCode, JsonObject json) throws IOException {
        byte[] response = GSON.toJson(json).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }
    
    /**
     * Create error response JSON
     */
    private JsonObject createErrorResponse(String message) {
        JsonObject error = new JsonObject();
        error.addProperty("error", message);
        error.addProperty("timestamp", System.currentTimeMillis());
        return error;
    }
}
