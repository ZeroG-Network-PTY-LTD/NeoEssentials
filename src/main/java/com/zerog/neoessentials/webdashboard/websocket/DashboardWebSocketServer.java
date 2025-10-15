package com.zerog.neoessentials.webdashboard.websocket;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * WebSocket server for real-time dashboard updates
 * Handles bidirectional communication between server and dashboard clients
 */
public class DashboardWebSocketServer extends WebSocketServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(DashboardWebSocketServer.class);
    private static final Gson GSON = new GsonBuilder().create();
    
    private static DashboardWebSocketServer INSTANCE;
    
    // Track connected clients and their subscriptions
    private final Map<WebSocket, Set<String>> clientSubscriptions = new ConcurrentHashMap<>();
    private final Set<WebSocket> authenticatedClients = new CopyOnWriteArraySet<>();
    
    // Message rate limiting
    private final Map<WebSocket, Long> lastMessageTime = new ConcurrentHashMap<>();
    private static final long MESSAGE_COOLDOWN_MS = 100; // 10 messages per second max
    
    private DashboardWebSocketServer(int port) {
        super(new InetSocketAddress(port));
        LOGGER.info("WebSocket server initialized on port {}", port);
    }
    
    public static DashboardWebSocketServer getInstance(int port) {
        if (INSTANCE == null) {
            INSTANCE = new DashboardWebSocketServer(port);
        }
        return INSTANCE;
    }
    
    public static DashboardWebSocketServer getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("WebSocket server not initialized. Call getInstance(port) first.");
        }
        return INSTANCE;
    }
    
    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        LOGGER.info("New WebSocket connection from: {}", conn.getRemoteSocketAddress());
        
        // Initialize client subscription set
        clientSubscriptions.put(conn, new HashSet<>());
        
        // Send welcome message
        JsonObject welcome = new JsonObject();
        welcome.addProperty("type", "welcome");
        welcome.addProperty("message", "Connected to NeoEssentials Dashboard WebSocket");
        welcome.addProperty("serverVersion", "1.0.0");
        welcome.addProperty("timestamp", System.currentTimeMillis());
        
        sendToClient(conn, welcome);
    }
    
    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        LOGGER.info("WebSocket connection closed: {} (Code: {}, Reason: {})", 
            conn.getRemoteSocketAddress(), code, reason);
        
        // Cleanup client data
        clientSubscriptions.remove(conn);
        authenticatedClients.remove(conn);
        lastMessageTime.remove(conn);
    }
    
    @Override
    public void onMessage(WebSocket conn, String message) {
        // Rate limiting check
        long now = System.currentTimeMillis();
        Long lastTime = lastMessageTime.get(conn);
        if (lastTime != null && (now - lastTime) < MESSAGE_COOLDOWN_MS) {
            JsonObject error = new JsonObject();
            error.addProperty("type", "error");
            error.addProperty("message", "Rate limit exceeded. Please slow down.");
            sendToClient(conn, error);
            return;
        }
        lastMessageTime.put(conn, now);
        
        try {
            JsonObject msg = GSON.fromJson(message, JsonObject.class);
            String type = msg.has("type") ? msg.get("type").getAsString() : "";
            
            switch (type) {
                case "subscribe":
                    handleSubscribe(conn, msg);
                    break;
                case "unsubscribe":
                    handleUnsubscribe(conn, msg);
                    break;
                case "ping":
                    handlePing(conn);
                    break;
                case "authenticate":
                    handleAuthenticate(conn, msg);
                    break;
                default:
                    JsonObject error = new JsonObject();
                    error.addProperty("type", "error");
                    error.addProperty("message", "Unknown message type: " + type);
                    sendToClient(conn, error);
            }
        } catch (Exception e) {
            LOGGER.error("Error processing WebSocket message", e);
            JsonObject error = new JsonObject();
            error.addProperty("type", "error");
            error.addProperty("message", "Invalid message format");
            sendToClient(conn, error);
        }
    }
    
    @Override
    public void onError(WebSocket conn, Exception ex) {
        LOGGER.error("WebSocket error", ex);
        if (conn != null) {
            LOGGER.error("Error on connection: {}", conn.getRemoteSocketAddress());
        }
    }
    
    @Override
    public void onStart() {
        LOGGER.info("WebSocket server started successfully");
        setConnectionLostTimeout(30); // 30 second timeout
    }
    
    /**
     * Handle client subscription to data channels
     */
    private void handleSubscribe(WebSocket conn, JsonObject msg) {
        // Require authentication for subscriptions
        if (!authenticatedClients.contains(conn)) {
            JsonObject error = new JsonObject();
            error.addProperty("type", "auth_error");
            error.addProperty("message", "Authentication required. Please authenticate first.");
            sendToClient(conn, error);
            return;
        }
        
        if (!msg.has("channels")) {
            JsonObject error = new JsonObject();
            error.addProperty("type", "error");
            error.addProperty("message", "Missing 'channels' field in subscribe request");
            sendToClient(conn, error);
            return;
        }
        
        Set<String> subscriptions = clientSubscriptions.get(conn);
        msg.getAsJsonArray("channels").forEach(channel -> {
            String channelName = channel.getAsString();
            subscriptions.add(channelName);
            LOGGER.debug("Client {} subscribed to channel: {}", conn.getRemoteSocketAddress(), channelName);
        });
        
        JsonObject response = new JsonObject();
        response.addProperty("type", "subscribed");
        response.addProperty("message", "Successfully subscribed to channels");
        response.add("channels", msg.get("channels"));
        sendToClient(conn, response);
    }
    
    /**
     * Handle client unsubscription from data channels
     */
    private void handleUnsubscribe(WebSocket conn, JsonObject msg) {
        if (!msg.has("channels")) {
            JsonObject error = new JsonObject();
            error.addProperty("type", "error");
            error.addProperty("message", "Missing 'channels' field in unsubscribe request");
            sendToClient(conn, error);
            return;
        }
        
        Set<String> subscriptions = clientSubscriptions.get(conn);
        msg.getAsJsonArray("channels").forEach(channel -> {
            String channelName = channel.getAsString();
            subscriptions.remove(channelName);
            LOGGER.debug("Client {} unsubscribed from channel: {}", conn.getRemoteSocketAddress(), channelName);
        });
        
        JsonObject response = new JsonObject();
        response.addProperty("type", "unsubscribed");
        response.addProperty("message", "Successfully unsubscribed from channels");
        response.add("channels", msg.get("channels"));
        sendToClient(conn, response);
    }
    
    /**
     * Handle ping/pong for keep-alive
     */
    private void handlePing(WebSocket conn) {
        JsonObject pong = new JsonObject();
        pong.addProperty("type", "pong");
        pong.addProperty("timestamp", System.currentTimeMillis());
        sendToClient(conn, pong);
    }
    
    /**
     * Handle client authentication
     * Validates session token with AuthenticationManager
     */
    private void handleAuthenticate(WebSocket conn, JsonObject msg) {
        if (!msg.has("sessionId")) {
            JsonObject error = new JsonObject();
            error.addProperty("type", "auth_error");
            error.addProperty("message", "Missing sessionId in authentication request");
            sendToClient(conn, error);
            return;
        }
        
        String sessionId = msg.get("sessionId").getAsString();
        
        // Validate session with AuthenticationManager
        com.zerog.neoessentials.webdashboard.security.AuthenticationManager authManager = 
            com.zerog.neoessentials.webdashboard.security.AuthenticationManager.getInstance();
        com.zerog.neoessentials.webdashboard.security.Session session = authManager.validateSession(sessionId);
        
        if (session == null) {
            JsonObject error = new JsonObject();
            error.addProperty("type", "auth_error");
            error.addProperty("message", "Invalid or expired session");
            sendToClient(conn, error);
            return;
        }
        
        // Mark client as authenticated
        authenticatedClients.add(conn);
        
        JsonObject response = new JsonObject();
        response.addProperty("type", "authenticated");
        response.addProperty("message", "Authentication successful");
        response.addProperty("username", session.getUsername());
        response.addProperty("role", session.getRole().name());
        response.addProperty("timestamp", System.currentTimeMillis());
        sendToClient(conn, response);
        
        LOGGER.info("WebSocket client authenticated: {} ({})", session.getUsername(), conn.getRemoteSocketAddress());
    }
    
    /**
     * Broadcast data to all clients subscribed to a channel
     */
    public void broadcast(String channel, JsonObject data) {
        data.addProperty("channel", channel);
        data.addProperty("timestamp", System.currentTimeMillis());
        
        String message = GSON.toJson(data);
        
        for (Map.Entry<WebSocket, Set<String>> entry : clientSubscriptions.entrySet()) {
            if (entry.getValue().contains(channel)) {
                WebSocket client = entry.getKey();
                if (client.isOpen()) {
                    client.send(message);
                }
            }
        }
    }
    
    /**
     * Send message to specific client
     */
    public void sendToClient(WebSocket client, JsonObject data) {
        if (client != null && client.isOpen()) {
            data.addProperty("timestamp", System.currentTimeMillis());
            client.send(GSON.toJson(data));
        }
    }
    
    /**
     * Broadcast to all connected clients
     */
    public void broadcastToAll(JsonObject data) {
        data.addProperty("timestamp", System.currentTimeMillis());
        String message = GSON.toJson(data);
        
        for (WebSocket client : getConnections()) {
            if (client.isOpen()) {
                client.send(message);
            }
        }
    }
    
    /**
     * Get number of connected clients
     */
    public int getClientCount() {
        return getConnections().size();
    }
    
    /**
     * Get number of authenticated clients
     */
    public int getAuthenticatedClientCount() {
        return authenticatedClients.size();
    }
    
    /**
     * Check if client is authenticated
     */
    public boolean isAuthenticated(WebSocket client) {
        return authenticatedClients.contains(client);
    }
}
