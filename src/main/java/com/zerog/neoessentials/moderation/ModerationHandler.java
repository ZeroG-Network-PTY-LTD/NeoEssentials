package com.zerog.neoessentials.moderation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import com.zerog.neoessentials.util.InputValidator;

/**
 * REST API handler for moderation (bans and whitelist).
 */
public class ModerationHandler implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModerationHandler.class);
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
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
            LOGGER.error("Error handling moderation request", e);
            sendErrorResponse(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }
    
    private void handleGet(HttpExchange exchange, String path) throws IOException {
        String[] parts = path.split("/");
        
        // GET /api/moderation/bans - List all bans
        if (parts.length == 4 && parts[3].equals("bans")) {
            handleListBans(exchange);
        }
        // GET /api/moderation/bans/active - List active bans
        else if (parts.length == 5 && parts[3].equals("bans") && parts[4].equals("active")) {
            handleListActiveBans(exchange);
        }
        // GET /api/moderation/bans/{id} - Get specific ban
        else if (parts.length == 5 && parts[3].equals("bans")) {
            String banId = parts[4];
            handleGetBan(exchange, banId);
        }
        // GET /api/moderation/bans/history/{target} - Get ban history
        else if (parts.length == 6 && parts[3].equals("bans") && parts[4].equals("history")) {
            String target = parts[5];
            handleGetBanHistory(exchange, target);
        }
        // GET /api/moderation/whitelist - List whitelist
        else if (parts.length == 4 && parts[3].equals("whitelist")) {
            handleListWhitelist(exchange);
        }
        // GET /api/moderation/whitelist/status - Get whitelist status
        else if (parts.length == 5 && parts[3].equals("whitelist") && parts[4].equals("status")) {
            handleGetWhitelistStatus(exchange);
        }
        else {
            sendErrorResponse(exchange, 404, "Endpoint not found");
        }
    }
    
    private void handlePost(HttpExchange exchange, String path) throws IOException {
        String[] parts = path.split("/");
        
        // POST /api/moderation/bans - Add new ban
        if (parts.length == 4 && parts[3].equals("bans")) {
            handleAddBan(exchange);
        }
        // POST /api/moderation/bans/check - Check if player/IP is banned
        else if (parts.length == 5 && parts[3].equals("bans") && parts[4].equals("check")) {
            handleCheckBan(exchange);
        }
        // POST /api/moderation/bans/{id}/appeal - Submit appeal
        else if (parts.length == 6 && parts[3].equals("bans") && parts[5].equals("appeal")) {
            String banId = parts[4];
            handleSubmitAppeal(exchange, banId);
        }
        // POST /api/moderation/bans/{id}/review - Review appeal
        else if (parts.length == 6 && parts[3].equals("bans") && parts[5].equals("review")) {
            String banId = parts[4];
            handleReviewAppeal(exchange, banId);
        }
        // POST /api/moderation/whitelist - Add whitelist entry
        else if (parts.length == 4 && parts[3].equals("whitelist")) {
            handleAddWhitelist(exchange);
        }
        // POST /api/moderation/whitelist/import - Bulk import whitelist
        else if (parts.length == 5 && parts[3].equals("whitelist") && parts[4].equals("import")) {
            handleImportWhitelist(exchange);
        }
        // POST /api/moderation/whitelist/toggle - Enable/disable whitelist
        else if (parts.length == 5 && parts[3].equals("whitelist") && parts[4].equals("toggle")) {
            handleToggleWhitelist(exchange);
        }
        else {
            sendErrorResponse(exchange, 404, "Endpoint not found");
        }
    }
    
    private void handlePut(HttpExchange exchange, String path) throws IOException {
        sendErrorResponse(exchange, 404, "Endpoint not found");
    }
    
    private void handleDelete(HttpExchange exchange, String path) throws IOException {
        String[] parts = path.split("/");
        
        // DELETE /api/moderation/bans/{id} - Remove ban
        if (parts.length == 5 && parts[3].equals("bans")) {
            String banId = parts[4];
            handleRemoveBan(exchange, banId);
        }
        // DELETE /api/moderation/whitelist/{id} - Remove whitelist entry
        else if (parts.length == 5 && parts[3].equals("whitelist")) {
            String entryId = parts[4];
            handleRemoveWhitelist(exchange, entryId);
        }
        else {
            sendErrorResponse(exchange, 404, "Endpoint not found");
        }
    }
    
    // ===== BAN HANDLERS =====
    
    private void handleListBans(HttpExchange exchange) throws IOException {
        Collection<BanEntry> bans = ModerationManager.getInstance().getAllBans();
        
        JsonObject response = new JsonObject();
        response.addProperty("banCount", bans.size());
        
        JsonArray bansArray = new JsonArray();
        for (BanEntry ban : bans) {
            bansArray.add(banToJson(ban));
        }
        response.add("bans", bansArray);
        
        sendJsonResponse(exchange, 200, response);
    }
    
    private void handleListActiveBans(HttpExchange exchange) throws IOException {
        Collection<BanEntry> bans = ModerationManager.getInstance().getActiveBans();
        
        JsonObject response = new JsonObject();
        response.addProperty("banCount", bans.size());
        
        JsonArray bansArray = new JsonArray();
        for (BanEntry ban : bans) {
            bansArray.add(banToJson(ban));
        }
        response.add("bans", bansArray);
        
        sendJsonResponse(exchange, 200, response);
    }
    
    private void handleGetBan(HttpExchange exchange, String banId) throws IOException {
        BanEntry ban = ModerationManager.getInstance().getBan(banId);
        if (ban == null) {
            sendErrorResponse(exchange, 404, "Ban not found");
            return;
        }
        
        JsonObject response = banToJson(ban);
        sendJsonResponse(exchange, 200, response);
    }
    
    private void handleGetBanHistory(HttpExchange exchange, String target) throws IOException {
        List<BanEntry> history = ModerationManager.getInstance().getBanHistory(target);
        
        JsonObject response = new JsonObject();
        response.addProperty("target", target);
        response.addProperty("banCount", history.size());
        
        JsonArray historyArray = new JsonArray();
        for (BanEntry ban : history) {
            historyArray.add(banToJson(ban));
        }
        response.add("history", historyArray);
        
        sendJsonResponse(exchange, 200, response);
    }
    // Use InputValidator.ValidationResult directly
    
    private void handleAddBan(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        JsonObject data = gson.fromJson(body, JsonObject.class);
        
        if (!data.has("type") || !data.has("target") || !data.has("reason")) {
            sendErrorResponse(exchange, 400, "Missing required fields: type, target, reason");
            return;
        }
        
        BanEntry.BanType type = BanEntry.BanType.valueOf(data.get("type").getAsString());
        String target = data.get("target").getAsString();
        String playerName = data.has("playerName") ? data.get("playerName").getAsString() : null;
        String reason = data.get("reason").getAsString();
        String evidence = data.has("evidence") ? data.get("evidence").getAsString() : null;
        Instant expiresAt = data.has("expiresAt") ? Instant.parse(data.get("expiresAt").getAsString()) : null;
        String bannedBy = getUsernameFromSession(exchange);

        // Validate reason length and content
            InputValidator.ValidationResult reasonResult = InputValidator.validateReason(reason);
        if (!reasonResult.isValid()) {
            sendErrorResponse(exchange, 400, "Invalid reason: " + reasonResult.getErrorMessage());
            return;
        }
    reason = (String) reasonResult.getValue();

        BanEntry ban = ModerationManager.getInstance().addBan(type, target, playerName, reason, evidence, expiresAt, bannedBy);

        JsonObject response = banToJson(ban);
        sendJsonResponse(exchange, 201, response);
    }
    
    private void handleRemoveBan(HttpExchange exchange, String banId) throws IOException {
        boolean removed = ModerationManager.getInstance().removeBan(banId);
        if (!removed) {
            sendErrorResponse(exchange, 404, "Ban not found");
            return;
        }
        
        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.addProperty("message", "Ban removed");
        sendJsonResponse(exchange, 200, response);
    }
    
    private void handleCheckBan(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        JsonObject data = gson.fromJson(body, JsonObject.class);
        
        String uuid = data.has("uuid") ? data.get("uuid").getAsString() : null;
        String ip = data.has("ip") ? data.get("ip").getAsString() : null;
        
        BanEntry ban = ModerationManager.getInstance().checkBan(uuid, ip);
        
        JsonObject response = new JsonObject();
        response.addProperty("isBanned", ban != null);
        if (ban != null) {
            response.add("ban", banToJson(ban));
        }
        
        sendJsonResponse(exchange, 200, response);
    }
    
    private void handleSubmitAppeal(HttpExchange exchange, String banId) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        JsonObject data = gson.fromJson(body, JsonObject.class);
        
        if (!data.has("appealText")) {
            sendErrorResponse(exchange, 400, "Missing required field: appealText");
            return;
        }
        
        String appealText = data.get("appealText").getAsString();
        boolean success = ModerationManager.getInstance().submitAppeal(banId, appealText);
        
        if (!success) {
            sendErrorResponse(exchange, 404, "Ban not found or already inactive");
            return;
        }
        
        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.addProperty("message", "Appeal submitted");
        sendJsonResponse(exchange, 200, response);
    }
    
    private void handleReviewAppeal(HttpExchange exchange, String banId) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        JsonObject data = gson.fromJson(body, JsonObject.class);
        
        if (!data.has("status") || !data.has("reviewNotes")) {
            sendErrorResponse(exchange, 400, "Missing required fields: status, reviewNotes");
            return;
        }
        
        BanEntry.BanAppeal.AppealStatus status = BanEntry.BanAppeal.AppealStatus.valueOf(data.get("status").getAsString());
        String reviewNotes = data.get("reviewNotes").getAsString();
        String reviewedBy = getUsernameFromSession(exchange);
        
        boolean success = ModerationManager.getInstance().reviewAppeal(banId, status, reviewedBy, reviewNotes);
        
        if (!success) {
            sendErrorResponse(exchange, 404, "Ban not found or no appeal submitted");
            return;
        }
        
        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.addProperty("message", "Appeal reviewed");
        sendJsonResponse(exchange, 200, response);
    }
    
    // ===== WHITELIST HANDLERS =====
    
    private void handleListWhitelist(HttpExchange exchange) throws IOException {
        Collection<WhitelistEntry> entries = ModerationManager.getInstance().getAllWhitelist();
        
        JsonObject response = new JsonObject();
        response.addProperty("entryCount", entries.size());
        response.addProperty("whitelistEnabled", ModerationManager.getInstance().isWhitelistEnabled());
        
        JsonArray entriesArray = new JsonArray();
        for (WhitelistEntry entry : entries) {
            entriesArray.add(whitelistToJson(entry));
        }
        response.add("entries", entriesArray);
        
        sendJsonResponse(exchange, 200, response);
    }
    
    private void handleGetWhitelistStatus(HttpExchange exchange) throws IOException {
        JsonObject response = new JsonObject();
        response.addProperty("enabled", ModerationManager.getInstance().isWhitelistEnabled());
        response.addProperty("entryCount", ModerationManager.getInstance().getAllWhitelist().size());
        sendJsonResponse(exchange, 200, response);
    }
    
    private void handleAddWhitelist(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        JsonObject data = gson.fromJson(body, JsonObject.class);
        
        if (!data.has("type") || !data.has("target")) {
            sendErrorResponse(exchange, 400, "Missing required fields: type, target");
            return;
        }
        
        WhitelistEntry.WhitelistType type = WhitelistEntry.WhitelistType.valueOf(data.get("type").getAsString());
        String target = data.get("target").getAsString();
        String playerName = data.has("playerName") ? data.get("playerName").getAsString() : null;
        String notes = data.has("notes") ? data.get("notes").getAsString() : null;
        String addedBy = getUsernameFromSession(exchange);
        
        WhitelistEntry entry = ModerationManager.getInstance().addWhitelist(type, target, playerName, addedBy, notes);
        
        JsonObject response = whitelistToJson(entry);
        sendJsonResponse(exchange, 201, response);
    }
    
    private void handleRemoveWhitelist(HttpExchange exchange, String entryId) throws IOException {
        boolean removed = ModerationManager.getInstance().removeWhitelist(entryId);
        if (!removed) {
            sendErrorResponse(exchange, 404, "Whitelist entry not found");
            return;
        }
        
        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.addProperty("message", "Whitelist entry removed");
        sendJsonResponse(exchange, 200, response);
    }
    
    private void handleImportWhitelist(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        JsonObject data = gson.fromJson(body, JsonObject.class);
        
        if (!data.has("entries")) {
            sendErrorResponse(exchange, 400, "Missing required field: entries");
            return;
        }
        
        List<WhitelistEntry> entries = new ArrayList<>();
        JsonArray entriesArray = data.getAsJsonArray("entries");
        for (int i = 0; i < entriesArray.size(); i++) {
            JsonObject entryObj = entriesArray.get(i).getAsJsonObject();
            WhitelistEntry entry = new WhitelistEntry();
            entry.setType(WhitelistEntry.WhitelistType.valueOf(entryObj.get("type").getAsString()));
            entry.setTarget(entryObj.get("target").getAsString());
            if (entryObj.has("playerName")) {
                entry.setPlayerName(entryObj.get("playerName").getAsString());
            }
            if (entryObj.has("notes")) {
                entry.setNotes(entryObj.get("notes").getAsString());
            }
            entries.add(entry);
        }
        
        String importedBy = getUsernameFromSession(exchange);
        int imported = ModerationManager.getInstance().importWhitelist(entries, importedBy);
        
        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.addProperty("imported", imported);
        sendJsonResponse(exchange, 200, response);
    }
    
    private void handleToggleWhitelist(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        JsonObject data = gson.fromJson(body, JsonObject.class);
        
        if (!data.has("enabled")) {
            sendErrorResponse(exchange, 400, "Missing required field: enabled");
            return;
        }
        
        boolean enabled = data.get("enabled").getAsBoolean();
        ModerationManager.getInstance().setWhitelistEnabled(enabled);
        
        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.addProperty("enabled", enabled);
        sendJsonResponse(exchange, 200, response);
    }
    
    // ===== UTILITY METHODS =====
    
    private JsonObject banToJson(BanEntry ban) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", ban.getId());
        obj.addProperty("type", ban.getType().name());
        obj.addProperty("target", ban.getTarget());
        obj.addProperty("playerName", ban.getPlayerName());
        obj.addProperty("reason", ban.getReason());
        obj.addProperty("evidence", ban.getEvidence());
        obj.addProperty("bannedAt", ban.getBannedAt() != null ? ban.getBannedAt().toString() : null);
        obj.addProperty("expiresAt", ban.getExpiresAt() != null ? ban.getExpiresAt().toString() : null);
        obj.addProperty("bannedBy", ban.getBannedBy());
        obj.addProperty("isActive", ban.isActive());
        obj.addProperty("isPermanent", ban.isPermanent());
        obj.addProperty("isExpired", ban.isExpired());
        
        if (ban.hasAppeal()) {
            JsonObject appealObj = new JsonObject();
            BanEntry.BanAppeal appeal = ban.getAppeal();
            appealObj.addProperty("appealText", appeal.getAppealText());
            appealObj.addProperty("appealedAt", appeal.getAppealedAt() != null ? appeal.getAppealedAt().toString() : null);
            appealObj.addProperty("status", appeal.getStatus().name());
            appealObj.addProperty("reviewedBy", appeal.getReviewedBy());
            appealObj.addProperty("reviewedAt", appeal.getReviewedAt() != null ? appeal.getReviewedAt().toString() : null);
            appealObj.addProperty("reviewNotes", appeal.getReviewNotes());
            obj.add("appeal", appealObj);
        }
        
        return obj;
    }
    
    private JsonObject whitelistToJson(WhitelistEntry entry) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", entry.getId());
        obj.addProperty("type", entry.getType().name());
        obj.addProperty("target", entry.getTarget());
        obj.addProperty("playerName", entry.getPlayerName());
        obj.addProperty("addedBy", entry.getAddedBy());
        obj.addProperty("addedAt", entry.getAddedAt() != null ? entry.getAddedAt().toString() : null);
        obj.addProperty("notes", entry.getNotes());
        return obj;
    }
    
    private String getUsernameFromSession(HttpExchange exchange) {
        String cookie = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookie != null && cookie.contains("sessionId=")) {
            return "admin";
        }
        return "system";
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
