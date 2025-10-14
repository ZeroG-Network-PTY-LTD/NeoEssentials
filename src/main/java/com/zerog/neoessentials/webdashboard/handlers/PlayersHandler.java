package com.zerog.neoessentials.webdashboard.handlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Handler for /api/players endpoint
 * Returns list of online players with name, rank, and XP
 */
public class PlayersHandler implements HttpHandler {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Add CORS headers
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        
        // Handle OPTIONS preflight
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }
        
        // Only allow GET
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendJsonResponse(exchange, 405, createErrorResponse(MessageUtil.localize("neoessentials.dashboard.api.method_not_allowed")));
            return;
        }
        
        try {
            JsonObject response = getPlayerData();
            sendJsonResponse(exchange, 200, response);
        } catch (Exception e) {
            sendJsonResponse(exchange, 500, createErrorResponse(MessageUtil.localize("neoessentials.dashboard.api.internal_error", e.getMessage())));
        }
    }
    
    /**
     * Get player data from the server
     */
    private JsonObject getPlayerData() {
        JsonObject response = new JsonObject();
        JsonArray playersArray = new JsonArray();
        
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            List<ServerPlayer> players = server.getPlayerList().getPlayers();
            
            for (ServerPlayer player : players) {
                JsonObject playerObj = new JsonObject();
                playerObj.addProperty("name", player.getName().getString());
                
                // Get rank from permissions or default to "Player"
                String rank = getPlayerRank(player);
                playerObj.addProperty("rank", rank);
                
                // Get XP level
                int xp = player.experienceLevel;
                playerObj.addProperty("xp", xp);
                
                // Additional data
                playerObj.addProperty("uuid", player.getUUID().toString());
                playerObj.addProperty("health", (int)player.getHealth());
                playerObj.addProperty("maxHealth", (int)player.getMaxHealth());
                playerObj.addProperty("dimension", player.level().dimension().location().toString());
                
                playersArray.add(playerObj);
            }
        }
        
        response.add("players", playersArray);
        response.addProperty("count", playersArray.size());
        response.addProperty("timestamp", System.currentTimeMillis());
        
        return response;
    }
    
    /**
     * Get player rank based on permissions
     * Integration ready when permission system is implemented
     */
    private String getPlayerRank(ServerPlayer player) {
        // Check if player is operator
        if (player.hasPermissions(4)) {
            return MessageUtil.localize("neoessentials.dashboard.stats.rank_admin");
        } else if (player.hasPermissions(3)) {
            return MessageUtil.localize("neoessentials.dashboard.stats.rank_moderator");
        } else if (player.hasPermissions(2)) {
            return MessageUtil.localize("neoessentials.dashboard.stats.rank_helper");
        }
        
        // Default rank
        return MessageUtil.localize("neoessentials.dashboard.stats.rank_player");
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
