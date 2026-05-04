package com.zerog.neoessentials.resourcepacks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * REST API handler for resource pack management.
 * Provides endpoints for upload, download, assignment, and management.
 */
public class ResourcePackHandler implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourcePackHandler.class);
    private final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        
        try {
            switch (method) {
                case "GET" -> handleGet(exchange, path);
                case "POST" -> handlePost(exchange, path);
                case "PUT" -> handlePut(exchange, path);
                case "DELETE" -> handleDelete(exchange, path);
                default -> sendErrorResponse(exchange, 405, "Method not allowed");
            }
        } catch (Exception e) {
            LOGGER.error("Error handling resource pack request", e);
            sendErrorResponse(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }
    
    private void handleGet(HttpExchange exchange, String path) throws IOException {
        String[] parts = path.split("/");
        
        // GET /api/resourcepacks - List all packs
        if (parts.length == 3) {
            handleListPacks(exchange);
        }
        // GET /api/resourcepacks/{id} - Get specific pack
        else if (parts.length == 4) {
            String packId = parts[3];
            handleGetPack(exchange, packId);
        }
        // GET /api/resourcepacks/{id}/download - Download pack file
        else if (parts.length == 5 && parts[4].equals("download")) {
            String packId = parts[3];
            handleDownloadPack(exchange, packId);
        }
        // GET /api/resourcepacks/{id}/icon - Get pack icon
        else if (parts.length == 5 && parts[4].equals("icon")) {
            String packId = parts[3];
            handleGetIcon(exchange, packId);
        }
        else {
            sendErrorResponse(exchange, 404, "Endpoint not found");
        }
    }
    
    private void handlePost(HttpExchange exchange, String path) throws IOException {
        String[] parts = path.split("/");
        
        // POST /api/resourcepacks/upload - Upload new pack
        if (parts.length == 4 && parts[3].equals("upload")) {
            handleUploadPack(exchange);
        }
        // POST /api/resourcepacks/external - Register external pack
        else if (parts.length == 4 && parts[3].equals("external")) {
            handleRegisterExternal(exchange);
        }
        // POST /api/resourcepacks/{id}/assign - Assign pack to player
        else if (parts.length == 5 && parts[4].equals("assign")) {
            String packId = parts[3];
            handleAssignPack(exchange, packId);
        }
        // POST /api/resourcepacks/{id}/unassign - Unassign pack from player
        else if (parts.length == 5 && parts[4].equals("unassign")) {
            String packId = parts[3];
            handleUnassignPack(exchange, packId);
        }
        // POST /api/resourcepacks/{id}/activate - Set as active pack
        else if (parts.length == 5 && parts[4].equals("activate")) {
            String packId = parts[3];
            handleActivatePack(exchange, packId);
        }
        else {
            sendErrorResponse(exchange, 404, "Endpoint not found");
        }
    }
    
    private void handlePut(HttpExchange exchange, String path) throws IOException {
        String[] parts = path.split("/");
        
        // PUT /api/resourcepacks/{id} - Update pack metadata
        if (parts.length == 4) {
            String packId = parts[3];
            handleUpdatePack(exchange, packId);
        }
        else {
            sendErrorResponse(exchange, 404, "Endpoint not found");
        }
    }
    
    private void handleDelete(HttpExchange exchange, String path) throws IOException {
        String[] parts = path.split("/");
        
        // DELETE /api/resourcepacks/{id} - Delete pack
        if (parts.length == 4) {
            String packId = parts[3];
            handleDeletePack(exchange, packId);
        }
        else {
            sendErrorResponse(exchange, 404, "Endpoint not found");
        }
    }
    
    private void handleListPacks(HttpExchange exchange) throws IOException {
        Collection<ResourcePack> packs = ResourcePackManager.getInstance().getAllPacks();
        
        JsonObject response = new JsonObject();
        response.addProperty("packCount", packs.size());
        
        JsonArray packsArray = new JsonArray();
        for (ResourcePack pack : packs) {
            packsArray.add(packToJson(pack, false)); // Don't include icon data in list
        }
        response.add("packs", packsArray);
        
        sendJsonResponse(exchange, 200, response);
    }
    
    private void handleGetPack(HttpExchange exchange, String packId) throws IOException {
        ResourcePack pack = ResourcePackManager.getInstance().getPack(packId);
        if (pack == null) {
            sendErrorResponse(exchange, 404, "Resource pack not found");
            return;
        }
        
        JsonObject response = packToJson(pack, false); // Don't include icon in JSON
        sendJsonResponse(exchange, 200, response);
    }
    
    private void handleUploadPack(HttpExchange exchange) throws IOException {
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.startsWith("multipart/form-data")) {
            sendErrorResponse(exchange, 400, "Content-Type must be multipart/form-data");
            return;
        }
        
        // Parse multipart form data
        String boundary = contentType.split("boundary=")[1];
        byte[] requestBody = exchange.getRequestBody().readAllBytes();
        
        Map<String, byte[]> parts = parseMultipartData(requestBody, boundary);
        
        if (!parts.containsKey("file")) {
            sendErrorResponse(exchange, 400, "No file uploaded");
            return;
        }
        
        String name = new String(parts.getOrDefault("name", "Unnamed Pack".getBytes()), StandardCharsets.UTF_8);
        byte[] fileData = parts.get("file");
        String username = getUsernameFromSession(exchange);
        
        try {
            ResourcePack pack = ResourcePackManager.getInstance().uploadPack(name, fileData, username);
            JsonObject response = packToJson(pack, false);
            sendJsonResponse(exchange, 201, response);
        } catch (Exception e) {
            LOGGER.error("Failed to upload resource pack", e);
            sendErrorResponse(exchange, 400, "Upload failed: " + e.getMessage());
        }
    }
    
    private void handleRegisterExternal(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        JsonObject data = gson.fromJson(body, JsonObject.class);
        
        if (!data.has("name") || !data.has("url") || !data.has("hash")) {
            sendErrorResponse(exchange, 400, "Missing required fields: name, url, hash");
            return;
        }
        
        String name = data.get("name").getAsString();
        String url = data.get("url").getAsString();
        String hash = data.get("hash").getAsString();
        String username = getUsernameFromSession(exchange);
        
        ResourcePack pack = ResourcePackManager.getInstance().registerExternalPack(name, url, hash, username);
        JsonObject response = packToJson(pack, false);
        sendJsonResponse(exchange, 201, response);
    }
    
    private void handleAssignPack(HttpExchange exchange, String packId) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        JsonObject data = gson.fromJson(body, JsonObject.class);
        
        if (!data.has("playerUuid")) {
            sendErrorResponse(exchange, 400, "Missing required field: playerUuid");
            return;
        }
        
        String playerUuid = data.get("playerUuid").getAsString();
        ResourcePackManager.getInstance().assignToPlayer(packId, playerUuid);
        
        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.addProperty("message", "Pack assigned to player");
        sendJsonResponse(exchange, 200, response);
    }
    
    private void handleUnassignPack(HttpExchange exchange, String packId) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        JsonObject data = gson.fromJson(body, JsonObject.class);
        
        if (!data.has("playerUuid")) {
            sendErrorResponse(exchange, 400, "Missing required field: playerUuid");
            return;
        }
        
        String playerUuid = data.get("playerUuid").getAsString();
        ResourcePackManager.getInstance().unassignFromPlayer(packId, playerUuid);
        
        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.addProperty("message", "Pack unassigned from player");
        sendJsonResponse(exchange, 200, response);
    }
    
    private void handleActivatePack(HttpExchange exchange, String packId) throws IOException {
        ResourcePackManager.getInstance().setActivePack(packId);
        
        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.addProperty("message", "Pack activated");
        sendJsonResponse(exchange, 200, response);
    }
    
    private void handleUpdatePack(HttpExchange exchange, String packId) throws IOException {
        ResourcePack pack = ResourcePackManager.getInstance().getPack(packId);
        if (pack == null) {
            sendErrorResponse(exchange, 404, "Resource pack not found");
            return;
        }
        
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        JsonObject data = gson.fromJson(body, JsonObject.class);
        
        // Update fields
        if (data.has("name")) {
            pack.setName(data.get("name").getAsString());
        }
        if (data.has("description")) {
            pack.setDescription(data.get("description").getAsString());
        }
        if (data.has("enforcementMode")) {
            String mode = data.get("enforcementMode").getAsString();
            pack.setEnforcementMode(ResourcePack.EnforcementMode.valueOf(mode));
        }
        
        JsonObject response = packToJson(pack, false);
        sendJsonResponse(exchange, 200, response);
    }
    
    private void handleDeletePack(HttpExchange exchange, String packId) throws IOException {
        boolean deleted = ResourcePackManager.getInstance().deletePack(packId);
        if (!deleted) {
            sendErrorResponse(exchange, 404, "Resource pack not found");
            return;
        }
        
        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.addProperty("message", "Pack deleted");
        sendJsonResponse(exchange, 200, response);
    }
    
    private void handleDownloadPack(HttpExchange exchange, String packId) throws IOException {
        try {
            byte[] fileData = ResourcePackManager.getInstance().getPackFileData(packId);
            if (fileData == null) {
                sendErrorResponse(exchange, 404, "Pack file not found");
                return;
            }
            
            ResourcePack pack = ResourcePackManager.getInstance().getPack(packId);
            exchange.getResponseHeaders().set("Content-Type", "application/zip");
            exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"" + pack.getFileName() + "\"");
            exchange.sendResponseHeaders(200, fileData.length);
            
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(fileData);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to download pack", e);
            sendErrorResponse(exchange, 500, "Download failed: " + e.getMessage());
        }
    }
    
    private void handleGetIcon(HttpExchange exchange, String packId) throws IOException {
        ResourcePack pack = ResourcePackManager.getInstance().getPack(packId);
        if (pack == null || pack.getIconData() == null) {
            sendErrorResponse(exchange, 404, "Icon not found");
            return;
        }
        
        byte[] iconData = pack.getIconData();
        exchange.getResponseHeaders().set("Content-Type", "image/png");
        exchange.sendResponseHeaders(200, iconData.length);
        
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(iconData);
        }
    }
    
    private JsonObject packToJson(ResourcePack pack, boolean includeIcon) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", pack.getId());
        obj.addProperty("name", pack.getName());
        obj.addProperty("description", pack.getDescription());
        obj.addProperty("fileName", pack.getFileName());
        obj.addProperty("fileHash", pack.getFileHash());
        obj.addProperty("fileSize", pack.getFileSize());
        obj.addProperty("url", pack.getUrl());
        obj.addProperty("isExternal", pack.isExternal());
        obj.addProperty("uploadedAt", pack.getUploadedAt() != null ? pack.getUploadedAt().toString() : null);
        obj.addProperty("uploadedBy", pack.getUploadedBy());
        obj.addProperty("isActive", pack.isActive());
        obj.addProperty("enforcementMode", pack.getEnforcementMode().name());
        
        // Metadata
        if (pack.getMetadata() != null) {
            JsonObject metadata = new JsonObject();
            metadata.addProperty("packFormat", pack.getMetadata().getPackFormat());
            metadata.addProperty("description", pack.getMetadata().getDescription());
            obj.add("metadata", metadata);
        }
        
        // Assignments
        JsonArray players = new JsonArray();
        pack.getAssignedPlayers().forEach(players::add);
        obj.add("assignedPlayers", players);
        
        JsonArray groups = new JsonArray();
        pack.getAssignedGroups().forEach(groups::add);
        obj.add("assignedGroups", groups);
        
        // Icon (optional, base64 encoded)
        if (includeIcon && pack.getIconData() != null) {
            obj.addProperty("iconData", Base64.getEncoder().encodeToString(pack.getIconData()));
        }
        
        return obj;
    }
    
    private Map<String, byte[]> parseMultipartData(byte[] data, String boundary) throws IOException {
        Map<String, byte[]> result = new HashMap<>();
        String boundaryStr = "--" + boundary;
        
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        BufferedReader reader = new BufferedReader(new InputStreamReader(bais, StandardCharsets.UTF_8));
        
        String line;
        String currentName = null;
        ByteArrayOutputStream currentData = null;
        boolean inData = false;
        
        while ((line = reader.readLine()) != null) {
            if (line.startsWith(boundaryStr)) {
                if (currentName != null && currentData != null) {
                    result.put(currentName, currentData.toByteArray());
                }
                currentName = null;
                currentData = null;
                inData = false;
            } else if (line.startsWith("Content-Disposition:")) {
                String[] parts = line.split("name=\"");
                if (parts.length > 1) {
                    currentName = parts[1].split("\"")[0];
                    currentData = new ByteArrayOutputStream();
                }
            } else if (line.isEmpty() && currentName != null) {
                inData = true;
            } else if (inData && currentData != null) {
                currentData.write(line.getBytes(StandardCharsets.UTF_8));
                currentData.write('\n');
            }
        }
        
        return result;
    }
    
    private String getUsernameFromSession(HttpExchange exchange) {
        // Get username from session cookie
        String cookie = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookie != null && cookie.contains("sessionId=")) {
            // Extract session ID and lookup username
            // For now, return default
            return "admin";
        }
        return "unknown";
    }
    
    private void sendJsonResponse(HttpExchange exchange, int statusCode, JsonObject data) throws IOException {
        String response = gson.toJson(data);
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
    
    private void sendErrorResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        JsonObject error = new JsonObject();
        error.addProperty("error", message);
        error.addProperty("timestamp", System.currentTimeMillis());
        sendJsonResponse(exchange, statusCode, error);
    }
}
