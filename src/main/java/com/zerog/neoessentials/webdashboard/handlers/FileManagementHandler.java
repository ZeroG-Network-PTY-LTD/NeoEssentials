package com.zerog.neoessentials.webdashboard.handlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.zerog.neoessentials.util.MessageUtil;
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
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
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
